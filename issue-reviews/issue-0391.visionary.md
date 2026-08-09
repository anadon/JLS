# Issue #391: TASK-0057: multi-driver resolution stops depending on the order the wires were drawn — a commutative, associative, idempotent per-bit fold over a cached driver list
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## The claim, and why it is the right claim

Stripped of apparatus, #391 asserts one thing: **a net's value must be a function of its
driver set, not of its driver list.** That is correct, it is central, and it is the seam
the whole tri-state half of JLS should have been cut along from the start. `WireNet.propagate`
today is *resolve-and-distribute* fused into one method over a mutable graph object
(`src/jls/elem/WireNet.java:443-529`); every downstream ambition — strength (#387), the
breadboard contention check, Verilog parity (#310/#304), a grader that can score a bus —
needs *resolve* to exist separately as a pure total function. Extracting it is the single
highest-leverage structural move available in `jls.elem` right now. On the goal, endorse
without reservation.

The route, though, is three or four times larger than the goal requires, is blocked behind
a dependency that may never ship, duplicates the operator #387 is separately specifying,
and introduces one concrete stale-state bug. Below: what the issue is really for, the
smaller thing that achieves it, and the criteria I am explicitly disregarding.

## The observation the issue never makes about its own payoff

If at most one driver is active, first-active-driver-wins is *already* order-independent —
there is exactly one non-`null` value to pick, and every permutation picks it. So the entire
user-visible payoff of this task lives in the **conflict** case, and nowhere else. #387's O1
is the honest demonstration: two drivers at 5 and 10, swap two `ELEMENT WireEnd` blocks,
answer flips 0x5 → 0xA. Nothing else moves.

That reframes the task. "No code path selects a driver by position" is not a behavioural
requirement; it is a *proof technique* for a requirement whose entire observable content is
"a bus conflict has one canonical answer." H2 ("two-state mode reproduces today byte-for-byte")
is therefore trivially true on every configuration except the one this task exists to change —
which is why P5's negative control is self-contradictory: it keeps
`multiDriverConflictResolvesDeterministicallyAndWarnsOnce` green, and that test pins precisely
the rule being retired. Behind a mode gate that keeps a positional scan alive, this task ships
its defect and its fix simultaneously.

## Reframing 1 (the big one): unblock the fold from #881 by splitting at the lowering boundary

O3 says the fold is *inexpressible* over `@Nullable BitSet` and therefore must wait for the
value type. The first half is true; the conclusion does not follow. What resolution needs is a
per-bit driven/undriven distinction **at the resolution site**, not a migrated value channel
across the simulator. That is a `record` of two `BitSet`s and a width — and it happens to be
the IEEE 1364 `s_vpi_vecval` aval/bval pair the issue's own Open Question 1 lists as option (b):

    0 = (a=0,b=0)   1 = (a=1,b=0)   Z = (a=0,b=1)   X = (a=1,b=1)

Lift each attached `Output`'s `@Nullable BitSet` into that pair at the top of `propagate`
(`null` → all-Z; non-null → fully driven), fold, and **lower back** to `@Nullable BitSet` on
the way out. Lowering is where X currently has nowhere to go — so lower X *canonically*
(all-Z → `null`; X bits → 0, or → the ones-plane, decided once and documented) and keep the
warning. That delivers P1, P2, P3 and P7 **today**, with:

- no dependency on #881 (and therefore none on #232's unfiled allocation profile, whose H1
  can descope #881 entirely — the issue comment says so plainly, and this issue's execution
  is currently hostage to a measurement nobody has taken);
- no `TriStateOff` migration;
- no mode gate this task does not own (Open Question 3, marked *blocks execution*, deferred
  to an owner that may not exist);
- and, crucially, **no rework later**: when the real value type lands, only the lowering
  function is deleted. The operator, the algebra, and the exhaustive permutation tests
  survive verbatim. `Resolution` is written against the four-state alphabet from day one; it
  simply has a narrow window on the world until #322/#881 widen it.

The observability of X in trace/VCD/stdout is #322's, and the issue already says so (§7.6).
Then let it be #322's *entirely* — including the value type. This issue should own the
operator and nothing else.

Choosing three planes with a reserved `U` (the recommended default carried from #322/#295)
is speculative generality bought at the price of this issue's independence. If the stated
destination is comparison against a real toolchain and structural Verilog export
(`src/jls/hdl/VerilogEmitter.java`, whose header already tells the reader "JLS simulates two
states plus HiZ"), then the alphabet, the resolution table, and later the strength levels
should be taken from IEEE 1364 by citation rather than invented and then proven locally.
That settles Open Questions 1, 2 and 4 by reference, and gives the VCD `x`/`z` glyphs, the
emitter, and the simulator one shared vocabulary instead of three.

## Reframing 2: there is one operator here, not two — #391 and #387 are writing it twice

#387 §7.10 already specifies the general fold:

    s* = max strength;  V* = values at s*;  Z if s*=HIGHZ, v if |V*|=1, X otherwise

#391's operator (Z identity, idempotent, 0⊕1 = X, X absorbing) is exactly that formula
restricted to one populated strength level. #391 says #387 "extends" it; in practice the
operand type changes from a value to a (strength, value) pair, and #387's DoD demands the
permutation test be "extended, not replaced" — which is a rewrite of every test signature
this issue is about to write.

Concrete alternative: define `resolve` **once**, over `(Strength, LogicVector)` pairs, with
`enum Strength { HIGHZ, WEAK, PULL, STRONG, SUPPLY }` fully declared and only `HIGHZ`/`STRONG`
reachable until #387. Cost: one enum and one field. Benefit: #387 collapses from "widen the
operator and its proofs" to "populate two enum members and ship two elements", and there is
never a moment when two resolution semantics exist. The algebraic laws hold identically —
max over a total order is commutative, associative and idempotent, and the tie rule is too.

## Reframing 3: delete the cached driver list — it is the only part of this design that can be wrong

§7.5/H3 add `WireNet.drivers`, accumulated in `makeNet` (`:132-147`) and `recheck` (`:277-288`).
This buys nothing: `propagate` already walks `ends` once for resolution and once for
distribution, so folding in place costs zero extra traversals and H3 becomes vacuously true.

It also introduces a defect. `WireNet.absorb` (`:251-261`) merges another net's ends and wires
into `this` and **never calls `recheck()`**, and neither do its two callers
(`src/jls/edit/SimpleEditor.java:4092` and `:4210`, both on wire-merge paths). A driver list
populated only in `makeNet`/`recheck` is silently stale the moment a student joins two wires
in the editor — the exact class of "the drawing and the simulation disagree" bug this task
exists to abolish. The issue's own §7.9 hazard (a longer-lived net object invalidating the
`conflictReported` latch's assumptions) is a second symptom of the same cause. Both disappear
if the fold reads `ends` directly. Derived state that can go stale is what a pure function is
*for*.

## What this leaves, and what I am disregarding

The task that survives is roughly one file and one test class:

1. `jls.core.Resolution` — `record LogicVector(BitSet ones, BitSet unknownOrZ, int width)` plus
   `resolve(Collection<Driver>, int width)`, pure, total, with the strength dimension declared.
   (`jls.core`'s charter — "types shared by the circuit model, the simulation engine, and
   persistence that carry no GUI dependency", `src/jls/core/package-info.java` — fits this
   exactly; the package is not geometry-only by intent.)
2. `WireNet.propagate` lifts, folds over `ends`, lowers, warns on X-from-disagreement.
3. `jls.sim.ResolutionFoldTest` — exhaustive laws over the alphabet, exhaustive permutations
   at n=3 and n=4.
4. `docs/simulation-semantics.md` §9 rewritten in the same diff, §12's row re-pointed, and
   `SimulationSemanticsRegressionTest.multiDriverConflictResolvesDeterministicallyAndWarnsOnce`
   rewritten to assert the canonical answer instead of the positional one — per #221, which
   this reframing honours rather than weakens.

**Explicitly disregarded acceptance criteria, and why:**

- *"`TriStateOff` is a deprecated alias for all-`Z` across all 18 files"* (O4, §7.12, DoD).
  Out. `TriStateOff` is an event payload meaning "stop driving"; per-bit Z at the *net* does
  not require its removal. Nothing in the Research Question, H1–H3, or P1–P7 needs it. The
  issue itself says this is "where the two weeks go" — it is nine tenths of the schedule
  buying none of the stated outcome. It belongs to #322's migration, where the sealed-arm
  epoch discipline it describes is genuinely load-bearing. (For the record, the surface is 19
  files at the current default-branch head, not 18.)
- *"the mode gate"* (Open Question 3, §7.12's `Splitter`/`Binder` gating). Out. A gate whose
  owner is contingent on #232's H1 is a dependency on vapor, and a gate that preserves a
  positional scan preserves the defect. `Splitter`/`Binder`'s all-or-nothing HiZ cases are
  untouched by this reframing anyway — they read *inputs*, not resolved nets, so they need no
  gate to stay put.
- *"the cached driver list"* (§7.5, H3, and the Method step that adds it). Out, per above.
- *"Confirm TASK-0056/#881 has landed"* (§6, §8 step 1, DoD link-pass line). Out — that is the
  point of the split.

## The uncomfortable question worth asking out loud

§2 of the semantics doc records that "nearly every element's `react` treats a null (HiZ) input
as zero before computing." Until #322 makes X *propagate*, an X produced by this fold is
invisible one gate downstream — it coerces to 0 exactly like HiZ does. So the four-state
resolution result buys, for a student today, a changed value at watch points and nothing more.
That is still worth having (it is what makes the answer order-independent), but it is worth
noticing that the pedagogically strongest version of this feature may not be a value at all:
JLS is a teaching tool, and simultaneous contention on a bus is an *error in the drawing*, not
a number. A located, editor-visible diagnostic — "these two tri-state drivers are both enabled
at t=10 on this net", with the net highlighted — teaches more than an X that vanishes into the
next AND gate. I would not replace the fold with it; contention detection needs the set-valued
resolution anyway. But if the fold lands and the diagnostic does not, the observable outcome
for the intended audience is a warning string that changes wording. Consider filing the
diagnostic as the deliverable that makes this work *felt*, rather than leaving it implicit in
#387's C6_CONTENTION consumer.

## Verdict

**endorse-with-reframing.** The claim — resolution is a function of the driver set — is right
and should land. The route should be: one operator with strength declared from birth, written
against a local IEEE-1364-shaped two-plane vector, folded in place over `ends` with no cache,
lowered canonically at the boundary, with the `TriStateOff` migration, the mode gate and the
#881 dependency cut away. That version is a day's work, unblocks #387, cannot go stale, and
leaves #322 strictly less to do rather than more.
