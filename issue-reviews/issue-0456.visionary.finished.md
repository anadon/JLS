# Issue #456: TASK-0075: resuming from a checkpoint reproduces the next one byte for byte, and a deliberately dropped field fails the gate
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is really for

Stripped of apparatus, the issue asserts one thing worth asserting: **JLS should own a
machine-checked definition of "the same run."** Everything else — six fixtures, a
committed null, a javadoc coverage fraction — is a delivery vehicle for that. The claim
is correct and it is load-bearing far beyond checkpointing. `ARCHITECTURE.md`'s #221
decision already binds any future compiled evaluation strategy to "agree bit-for-bit
with the #202 RV32I integration golden run as a differential oracle"; #265/#111 want the
same relation across platforms; #421 wants it across a fidelity boundary;
`docs/capability-roadmap/lf-03-causal-debug.md` wants it to make time travel honest. Four
trajectories are each re-deriving "same run" privately. This issue could give them one
definition — and as written, it gives them a checkpoint-specific test class instead.

That gap is the review.

## 1. The dependency arrow is inverted against its own parent, and that is not bookkeeping

`blocked_by: [426]` reverses the one ordering edge the parent feature calls its central
claim. #363 §6: *"TASK-0075 is written first and is not on the critical path — it is a
test that fails until TASK-0074 lands, and writing it first is the whole point,"* with
`T75 --> T74` drawn in its mermaid. #426 says it twice in its own voice (§11: *"should be
written **before** this task"*; §13 lists the gate as out of scope *"which should be
written first"*). #456 then makes itself downstream and its Method step 2 is "confirm
#426 has landed."

This is not a scheduling nit. Test-first here buys exactly one thing: **the gate gets to
name the seam.** Written first, this task's deliverable is not a test file, it is the
shape of `checkpoint()` / `restore()` / `runUntil()` — `lf-03-causal-debug.md` already
specifies those three methods on `Simulator` and notes `runEventLoop` need not change.
Written second, the codec's author picks the seam and the test conforms to it, which is
the blind-sharing the issue's own §12 says the two-task split exists to prevent. The
split preserves *authorship* independence and then throws away *design* independence.

**Concrete alternative:** make this task's first deliverable a compiling, failing SPI —
`jls.sim.SimCheckpoint`, `Simulator.checkpoint()`, `Simulator.restore(SimCheckpoint)`,
`Simulator.runUntil(long)` — plus the property harness written against it. P1 then
genuinely FAILS at `2d0ca9d` (it does not compile, then does not pass), which is what P1
claims. `blocked_by` becomes empty; #426 becomes `blocked_by: [456]`. §7.4's "N/A — no
production class or method is added" is the line I am explicitly disregarding: adding the
interface is the point.

## 2. The null is hand-rolled mutation testing, and JLS already adopted the real thing

P3 commits a codec variant that omits `Register.currentC`. §11 admits it proves nothing
about any other field. Open Question 3 defers the general case ("drop each field in turn")
to a follow-up issue. §7.12 records a standing obligation to keep the null compiling in
step with the codec.

All of that is one already-adopted tool. `docs/mutation-testing-trial-2026-07.md` §6 is
an **Adopt** verdict; `pom.xml:752-812` carries the `pitest` profile with
`targetClasses` including **`jls.sim.*`**, a weekly `mutation.yml` run, and an
`80` `mutationThreshold` climb-ratchet that "may only ever move UP." A committed null is
one hand-written mutant in a repo that generates 905 of them weekly over the exact
package the codec lands in.

**Concrete alternative:** delete P3's committed null. Instead (a) put the codec's package
in the `pitest` `targetClasses` list and let the ratchet carry it, and (b) if bytecode
mutators prove too coarse for "field omitted from output" — an honest risk, they mutate
operators not serialization intent — drive a `@ParameterizedTest` off the **field map
#426 P5 already commits**, dropping each mapped field in turn via one codec-internal
test hook. Either way the payoff is the same and it is large: the coverage statement
becomes **computed and re-computed weekly**, not frozen in a javadoc (P6) that decays
exactly like the reviewed field list this task exists to replace. §11's "the null tests
exactly one field" threat disappears, §7.12's keep-in-step obligation disappears, and
Open Question 3's follow-up issue never needs filing.

## 3. Six committed `.jls` fixtures pull against the project's own established practice

P5 wants six hand-built `.jls` files under `test/fixtures/`, each *demonstrated* to reach
its in-flight state, each carrying a maintenance obligation across format epochs (§7.7,
§11) that nothing in the suite detects when it breaks.

The project stopped doing this. `test/jls/ElementSimulationGoldenTest.java` builds its
circuits as **circuit text in code** (`simulate(String circuitText)`, `CircuitTextBuilder`)
and pins vocabulary coverage with a reflective ratchet: `COVERED` (33 element classes) and
`EXEMPT` (each entry carrying a written reason), with a test that fails when a simulating
element is in neither. `test/fixtures/` holds four files, and three are *legacy container*
artifacts — a 4.6-fork save, a headless canary, the RV32I run — i.e. fixtures that exist
because a byte-exact historical container is the thing under test. In-flight simulator
state is not that.

**Concrete alternative:** drop the six new `.jls` files. Run the round-trip property over
the *existing* golden corpus, parameterized by the same reflective element sweep. A
format epoch cannot neuter a circuit that is built from a string at test time, so P5's
"demonstrated to reach" reduces to an assertion on the built circuit, §7.7's regeneration
obligation vanishes, and the `.gitattributes` `-text` framing becomes irrelevant. Better,
coverage stops being a hand-maintained list of six and becomes the same registry-totality
property #363 §5 criterion 4 already demands and that no child currently asserts — this
task can assert it for free.

## 4. "State that exists only between events" is a mis-frame that inflates the whole task

O4 concludes each of the six state classes is *"state that exists only **between** events. A
checkpoint at an event boundary never sees any of them, which is why the fixture set is the
substance of this task."* That premise is wrong, and if it were right #426 and #363 would be
undeliverable — both take the checkpoint at a quiescent instant *between* events, on
`Simulator.beforeEvent()` (`Simulator.java:252`, called at `:220`).

Every class listed is a *window*, not an instant. `Register.toBeValue` is set on the clock
edge and cleared when the `NewValue` event arrives one propagation delay later — it is
non-null at every event boundary in that window. A `Memory` write in flight *is a queue
member*, visible at every boundary until it retires. `StateMachine.busy`,
`WireNet.conflictReported`, `Clock` phase, `TestGen` cursor: all windows. Reaching them is a
question of **checkpoint cadence relative to propagation delay**, not of exotic circuit
authoring. Open Question 1 half-sees this ("the coarsest cadence that still places a
checkpoint inside each in-flight window") and then keeps the fixture apparatus anyway.

Consequence: checkpoint at **every** event boundary over a deliberately short run and every
window is hit by construction. Stage 6's quadratic-cost worry is bounded by choosing a short
`L`, not by coarsening `Δ` — and short-and-dense is strictly better coverage per second than
long-and-sparse, which is the opposite of what H4's fallback recommends.

## 5. The obligation nobody assigned: the codec must be canonical

The gate rests on byte comparison, and §7.2 insists the test treat the format as opaque —
"this test must not reach inside the format." That is right, and it silently imports a
requirement neither #426 nor #456 makes anyone's deliverable: **`ser` must be canonical**,
a function of state alone. `Simulator.dupCheck` is a `HashSet`; element traversal order is
whatever the circuit's element list gives. `lf-03-causal-debug.md` derived this explicitly
— *"Sorted everywhere, so two checkpoints of identical state are byte-identical"* — and
#363 invariant 4 gestures at it ("nothing may depend on hash order"), but the gate's own
predictions never assert it. Without it, P1 produces false failures the executor will debug
as codec bugs, which is precisely the misattribution O2 spends a page preventing.

**Concrete addition, cheap and squarely in this task's remit:** two assertions before
anything else runs — `ser(de(ser(σ))) == ser(σ)` (idempotence), and `ser(σ) == ser(σ′)` for
the same state reached by two independent runs in one JVM. These are the preconditions that
make byte equality mean anything, and they belong to the gate, not to the codec.

## 6. Prefer the retirement journal over the VCD tail as the independent observable

P2/H2 make the VCD tail the second observable. But VCD carries only *watched* signals at
timescale resolution — a lossy projection, and the very reorderings H2 wants to catch can
be invisible in it. The event loop already has the right seam: `afterEvent(SimEvent)`
(`Simulator.java:241,269`), which `BatchSimulator` already uses for trace accumulation.
Comparing the **retirement sequence** `(time, seq, element id, payload kind)` catches every
same-time reordering exactly, reports the first divergence in the vocabulary the failure
actually occurred in, and needs no anchor-offset reasoning (H5 dissolves). It is also
`lf-03-causal-debug.md`'s journal — build the gate on it and the test scaffolding becomes
the index for time travel rather than test-only apparatus. Keep the VCD tail as a
corollary check, not as the primary.

## 7. Where the durable artifact should live

None of the above changes the goal; it changes what survives. The thing worth having a
year out is not `CheckpointRoundTripTest` — it is a normative **observational-equivalence
clause in `docs/simulation-semantics.md`** ("two runs are the same run iff their retirement
sequences agree; the checkpoint/VCD/state-hash comparisons are its oracles") plus one
reusable `SameRunOracle` harness under `test/jls/sim/`. Checkpoint round-trip is then client
#1. Client #2 is #221's compiled strategy, whose equivalence criterion `ARCHITECTURE.md`
already wrote and left without a harness. Client #3 is #265/#111 — which this issue already
notices it becomes "the cheapest existing probe" for, and then bolts on as a follow-up
comment rather than as a shared artifact. Client #4 is #421. Building the relation once,
normatively, is the difference between this task strengthening the project's arc and this
task adding a sixth private notion of equality to it.

## Reshape, stated plainly

I am disregarding these acceptance criteria: `blocked_by: [426]`; P3's committed null as
the non-vacuity proof; P5's six committed `.jls` fixtures and their reachability proofs;
P6's javadoc coverage fraction; §7.4's "no production class or method is added." Keep
everything else. The replacement:

1. Ship the failing SPI + harness **first**; invert the edge with #426.
2. Define "same run" in `docs/simulation-semantics.md`; implement it as one reusable oracle.
3. Assert canonicality (idempotence + two-runs-agree) before asserting round-trip.
4. Parameterize over the in-code golden corpus and the reflective element sweep, not six files.
5. Checkpoint at every event boundary over a short run; drop the cadence/coverage tradeoff.
6. Non-vacuity comes from the `pitest` ratchet over the codec's package (fallback: a
   field-map-driven drop-each-field harness), not from one committed null.
7. Compare retirement sequences primarily; VCD tail as corollary.

The verdict is `endorse-with-reframing` and not `rethink` because the issue's central
judgment — gate on the property, never on a reviewed field list — is exactly right, is the
best idea in FEAT-035, and deserves to be built. It just deserves to be built once, for the
whole project, at the seam it defines rather than the one it inherits.
