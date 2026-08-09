# Issue #327: FEAT-037: a drawn register has an honest reset that exports as a reset, and every clock-domain crossing in a design is reported
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two different ends, welded together. End A: **reset stops being a save-file
attribute and becomes something a machine actually comes out of** — which is
what a shuttle wrapper, an imported reset-driven core, and a drawn CPU all need.
End B: **a clock stops being an anonymous wire** — which is what STA, ERC, SDC
ingestion, and any statement about a two-clock design need. The issue's own
pinned comment concedes the weld: `#327 = #478 + #398`, "different capabilities
that happen to share the sequential-element surface."

I endorse both ends without reservation. The project has already written both
down, in far more depth than this issue does, at
`docs/capability-roadmap/lf-08-clocks-and-cdc.md` (C1–C7) and in P2's register
control pins (`docs/capability-roadmap/README.md:221-225`). My objection is that
this issue re-derives that work in a shape that contradicts it on four
load-bearing decisions, and it never cites lf-08 once.

## 1. Reset as an attribute is the failure the issue says it exists to prevent

§2 rejects "model reset as a special initial-value mode" — correctly, "that is
the confusion the feature exists to end." But §1 criterion 1 and §3's *Modifies*
list deliver exactly that shape: "two saved attributes appended to
`Register.OWN_ATTRIBUTES`" plus behaviour in `react`/`initSim`. No `Input` is
added. No dialog, palette, geometry, `rotate`/`flip`, or help page is touched —
none of `ARCHITECTURE.md`'s port-changing checklist appears. The transformation
in §3 has an input `r`, so a pin must exist; the interface contract that would
own it does not.

That gap matters more than a missing line item. lf-08 C5's teaching value is
"**reset becomes a wire and therefore becomes checkable**": reset-tree
completeness, reset released synchronously per domain, *a reset crossing domains
is itself a CDC crossing*. None of that is reachable from an enum on the
element. And the roadmap already knows where the pin belongs: P2's
`CLR`/`PRE`/`EN`/`LD` family, "each gated by a saved boolean, in the
`Memory.sync` style issue #199 established" — the exact precedent #327 cites for
its own attribute. **Reframing:** ship reset as the first member of the control-pin
family, not as a bespoke two-attribute mode enum. The same edit that adds `CLR`
lifts `CellValidator.SET_RESET_MESSAGE` and `ASYNC_RESET_MESSAGE`
(`src/jls/hdl/yosys/CellValidator.java:75-88`), and the same port-list machinery
later carries `EN` — which is 27% of the RISC-V CPU's elements today
(`riscv/build_cpu.py:239-252`, an `AndGate` + `Mux` per register-file entry).
A reset-only mode enum buys one capstone and leaves that machinery unbuilt.

## 2. The ordering is inverted, and the issue's own invariant says so

§6 asserts "TASK-0077 before TASK-0078 is necessity, not convention." lf-08 C1
derives the opposite and gives the better reason: `Clocked` is *the enabling
refactor* — "everything else needs to ask 'which pin is the clock' and today has
to know each element's port order" — and "C1 should precede P2's register
control pins … doing that *after* the `Clocked` interface exists is one edit,
doing it before is two."

The codebase settles it. Today the clock is `inputs.get(1)` by construction
order (`Register.java:230-231`, repeated per orientation) and the exporter reads
it that way: `HdlModel.Operand clock = operand(ins.get(1), nets, groups)`
(`src/jls/hdl/HdlExporter.java:578`). Adding a reset pin *changes that port
list*. §4 invariant 3 — "`Clocked` returns the named pin, looked up by name,
never by index. An index-based lookup breaks the moment a port list changes and
fails silently" — is a description of the hazard TASK-0077 creates and
TASK-0078 removes. Doing them in the stated order means shipping the port-list
change into three hand-written index-and-field edge detectors
(`Register.currentC:698`, `StateMachine.oldClock:730-752`, `Memory.lastClock:996`)
and then rewriting them. **Reframing:** `Clocked` first, then pins.

## 3. Declared domains throw away the one thing JLS can do that RTL tools cannot

Open Question 4's recommended default is domains "on the design, referenced by
clocked elements." lf-08's competitive section identifies the single
architectural leapfrog in this whole area, and it is the opposite decision:

> "in a schematic, the clock roots are *in the drawing* — a `Clock` element is
> unambiguously a clock — so inference is exact and the constraint file is
> optional. No RTL tool can do that, because RTL has no `Clock` element."

A declaration surface re-imports the incumbent failure mode the roadmap names in
the same breath — *a wrong constraint file produces a clean report* — and it
does so in a tool whose users are students, who will get the declaration wrong
and receive a confidently clean report. It also manufactures the issue's own
worry ("a new thing to get wrong in the file format, in the editor and in the IR
at once"), and it drags in Open Question 2's epoch dependency for data that
never needed to be saved.

**Reframing:** infer, then allow override (lf-08 C2). Roots = `Clock` element
outputs, top-level pins carrying a clock port role, anything a constraint file
names. Backward slice from each `Clocked.clockPin()` through the clock cone,
classifying gated / generated / muxed / undriven. Domain identity is *the root*,
not a declared label — which is SDC's model, so the vocabulary transfers to
Vivado and PrimeTime unchanged and #82/#93/#94's `create_clock` /
`create_generated_clock` / `set_clock_groups` finally have somewhere to land.
Under this framing §1 criterion 4 becomes "domains are computed and reported",
§3's durable-state row shrinks to nothing, and OQ2 and OQ4 both dissolve. I am
explicitly disregarding OQ4's recommended default and criterion 4 as written.

Two smaller consequences of inference-first: lf-08 makes **the clock list the
first artifact**, before any violation ("a CDC tool whose domain model is wrong
produces confidently wrong violations, and this is the single most common way
the capability fails"). #327 has no clock-list criterion at all — it goes
straight to crossings. And §3's crossing set is defined net-locally with
`δ(e_d) ≠ δ(e_s) ≠ ⊥`; since δ is propagated only from clocked elements, any
crossing with a gate between the two flops — the common topology — has ⊥ on the
intervening net and is silent. lf-08 C3 defines it over the *data cone* of a
`Clocked` sink, which is the correct seam.

## 4. The check's claim is one the simulator will contradict

This is the deepest misalignment, and lf-08 already priced this exact scenario.
In JLS today setup and hold are zero (`docs/simulation-semantics.md` §6.1;
same-time races resolve by `SimEvent`'s global sequence counter), so — lf-08's
words — "**a two-flop synchronizer in JLS is two flip-flops that cost 100 time
units of latency and change nothing else.** JLS does not merely fail to teach
why the second flop exists; it demonstrates that it does not." A machine-readable
report that flags a crossing which the student then simulates a thousand times
without a single failure teaches that the tool is wrong.

lf-08's disposition of the static-only version is explicit: "If the metastability
half is refused or indefinitely deferred … this is not a program — it is two ERC
rules and a clock-domain map inside P5, worth about 3–4 weeks, and it should be
filed there instead of standing alone." #327 *is* the deferred version and does
not record that finding. Either route is honest; picking neither is not:
**(a)** file the crossing check as ERC rules on the ERC channel, sized at 3–4
weeks, and let this issue be the reset feature it is named after; or **(b)** pull
in the one cheap dynamic piece that makes the report true — seeded, independent-
per-bit resolution at a violated capture. That is the "run 100 seeds, count
failures" experiment lf-08 calls "the single most transferable lesson in the
area," and it is also the only thing that makes the false-positive number in I6
worth measuring.

## 5. The report wants to be a diagnostics channel, not a bespoke JSON

§2 rejects the dialog for the right reason (consumers are CI legs and graders)
and then over-corrects into batch-only. lf-08's strongest competitive claim is
the other surface: "**JLS's user drew the picture, and there is exactly one
picture**" — the crossing marked on the wire the student drew, with a one-click
waiver persisted in the `.jls`.

Both fall out of one thing JLS does not have and repeatedly needs: a structured
diagnostic record. `LoadError` is the only structured taxonomy in the tree;
everything else is a string (`HdlModel.addWarning`, `CellValidator`'s messages,
the exporter's refusals). **Reframing:** make TASK-0078's deliverable a
`jls.diag` record — rule id, severity, anchors, message, waiver key — with two
renderers (batch JSON, canvas overlay). ERC, the exporter's refusals, and this
issue's own §4 invariant 5 ("refuse by name") are all the same shape, and the
budget then leaves a channel behind rather than one report format.

This also answers Open Question 3 in a way the issue does not notice. Per-crossing
suppression requires a key that survives an unrelated edit — and #336, already
this issue's `blocked_by`, is precisely the feature that makes net names a
function of `Element.getStableId()` instead of save order. Key waivers off that,
or every waiver evaporates the next time someone inserts a gate.

## 6. The bundle imposes a false gate

`blocks: [328]` means Tiny Tapeout's shuttle handoff waits on a CDC analysis it
does not need. Meanwhile lf-08 records the structural half as "**parallel-safe,
gated on nothing**." The feature boundary is the wrong seam: it cuts across the
project's two live arcs (element vocabulary; clock/timing architecture) instead
of along either. Split into (i) register control pins, reset first — unblocks
#328, #291/#292's reject-list neighbours, and the importer's two apology
messages; (ii) `Clocked` + inferred domains + clock list + crossing diagnostics —
gated on #336 for net identity, and on nothing else.

## What I would keep unchanged

Criterion 3 (byte-identical pre-feature simulation), invariant 3 (pin by name),
invariant 5 (refuse, never silently drop a reset), I6's "zero findings on a
single-clock teaching circuit," I7's permutation test, and the refusal to infer
polarity from the target. Those five are exactly right and are the parts of this
issue lf-08 would not improve on.
