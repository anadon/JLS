# Issue #693: TASK-C533-1: the interactive simulator advances by exactly one event, or one wavefront, on demand — with simulated time shown and the event order unchanged
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Read past the ACs and #693 is not a GUI feature at all. It is the first request in
the tree for a **bounded run** — "advance the engine by a named quantum and stop" —
and it arrives dressed as two toolbar buttons because its parent (#533 / CAP-23
PF-4) is a pedagogy story. The pedagogy is the weakest part of the claim; the
primitive is the strongest, and the primitive has three other customers already
filed or documented:

- **#535 / PF-6** rewind-by-replay is `runUntil(T)` and nothing else.
- **`docs/capability-roadmap/lf-03-causal-debug.md`** (P7-C) states it outright:
  `runUntil` is "`InteractiveSimulator`'s `stepEnd` logic (`:774-805`) generalized
  and pushed down into `Simulator`, **which is where it always belonged**."
- The existing time-amount **Step** button (`InteractiveSimulator.java:56,323-360`)
  is already a bounded run, implemented badly (below).

So the right question is not "should stepping exist" — it should — but "is #693
cutting at the seam that gives the project four features for one?" As written it
is not: it cuts inside `jls.edit.InteractiveSimulator`, adds a third control flag
to a class ARCHITECTURE.md already flags as delicate (EDT/sim-thread `volatile`
discipline, #49 H7/H8), and declares itself interactive-only, which is exactly the
one shape of this work that can never become a JLS artifact.

## Finding 1 — "one event" is an engine artifact, not a unit of student meaning

`docs/capability-roadmap/keystone-c-performance.md`'s census (quoted in lf-03) is
decisive: of 2,331,793 events fired on the flagship run, **1,919,891 (82.3%) are
zero-field `PinChanged`** and only 378,129 are `NewValue`. `PinChanged` is "some
input moved, re-read"; it changes no displayed value, and under §3's duplicate
suppression it is already coalesced to one per element per timestamp. Four presses
of *Step One Event* out of five therefore change nothing on the canvas and, when
the event sits at the current timestamp, do not move the clock either. A control
that does nothing four times out of five reads as broken, and no AC in #693
forbids that outcome — AC-1 asks only that the step advance "exactly that much."

The student's unit is not an event. It is **an arrival**: a wire changed, because
that gate finished, after that many time units. That is `NetChange` — lf-03's
journal record, chosen at net-change granularity for precisely this reason (~380 K
records instead of 2.3 M on the same run). Reframe the default step unit as
*next observable change* (advance until some net or watched value differs), keep
raw-event stepping as an expert mode, and add the one thing that makes any of it
teach: **a caption saying what the step just did** ("`g7` (NAND, delay 5) drove
`c2` → 1 at t=41175"). Without the caption, #695's animation is doing all the
teaching and #693 is a clock label.

## Finding 2 — the seam belongs in `jls.sim.Simulator`, and it makes AC-2/AC-3 free

Today's stepper fakes simulated time. `beforeEvent` peeks the head, and when it is
past the boundary sets **`now = stepEnd`** (`InteractiveSimulator.java:783`) — a
value no event occupies — and `runSim`'s epilogue then adds `now += 10L *
scaleFactor` (`:666`) for cosmetic room. #693's actual deliverable is *the time
readout*, and the mechanism it will inherit is the one that invents times.
`docs/simulation-semantics.md` §1 is normative: "time advances only by dequeuing
events."

Put three methods on `Simulator` instead, next to `runEventLoop` (`:215-243`),
which is not edited:

```java
boolean stepEvent();              // poll one event, react, return false if none
boolean stepTimestamp();          // drain the current instant (see Finding 4)
void    runUntil(long target);    // the existing loop, bounded
```

Then AC-2 and AC-3 stop being tests and become **theorems**: stepping is the same
loop, entered the same way, differing only in where it returns. Sequence equality
cannot fail because there is no second code path to diverge. The current design
has to *assert* the property because it genuinely could break it. Prefer the
design where the property is structural — that is the same instinct behind
`HeadlessCoreRatchetTest` and `CircuitSnapshot.sameAs` byte-equality, both already
in this tree.

Secondary win: `stepEnd`, `stepAmount`, and the animate `TimerTask`
(`:372-455`) all collapse onto `runUntil`. The feature should be **net-negative**
in `InteractiveSimulator` control state, not additive.

## Finding 3 — "interactive-only" pulls against the project's own leapfrog axis

AC-4 reads as pure defence: don't perturb batch. But the ACs above it silently
demand machinery that does not exist. There is **no event-sequence recorder in the
tree**. `BatchSimulator.eventTrace` (`:24-25,140-180`) records *deduplicated value
samples for watched elements* — not events, not order, not the elements that did
not change. To assert "the stepped sequence equals the free-run sequence element
for element" you must first build a sequence recorder in `jls.sim`, and #693's
1–1.5 mw band does not contain it.

That unbudgeted recorder is lf-03's journal in embryo. Build it deliberately:
a debug-only, off-by-default `afterEvent` tap behind a single early return (the
pattern is `BatchSimulator.afterEvent:144-145`) emitting
`(time, seq, elementStableId, payloadKind)`, plus `jls -b --step-trace` printing
it deterministically to stdout. Then:

- AC-2/AC-3 become a **text diff of two files**, runnable headlessly in
  `test/jls/`, where the golden suite already lives — instead of a GUI harness in
  `test/jls/ui/`, whose layers 2 and 3 are documented as *reserved, not built*.
  As written, #693's central test has no home.
- AC-4 becomes assertable the cheap way: same tap, same bytes, feature on and off.
- The artifact is on lf-03's named leapfrog axis — "a headless, deterministic
  causal artefact… the axis where JLS could be *unambiguously ahead*" — and inside
  `docs/batch-interface.md`'s existing stability contract.

Interactive-only is the right *scope for the UI*. It is the wrong scope for the
*definition*. Define the step unit in the headless core; let the GUI be its first
client and the grader its second.

## Finding 4 — "one wavefront" is undefined, and one reading can hang the GUI

"Every event at the current timestamp" has two inequivalent meanings, and #693
picks neither. Draining timestamp T **posts new events at T**: `WireNet.propagate`
posts `PinChanged` at `now`, and §7's table plus §6.2 ("an arbitrarily deep chain
of wiring elements adds zero time") make splitters, binders, pins, jumps and
subcircuit boundaries zero-delay. So:

- *Delta-cycle reading* — drain the events queued at entry — gives a jagged,
  partial step through wiring plumbing, which is the opposite of the intent.
- *Closure reading* — settle the instant — is what "wavefront" means to a student,
  but it is an unbounded loop in a zero-delay cycle, and JLS has **no
  combinational-loop detection at all** (roadmap README:416). A free run at least
  stays interruptible through the `volatile stopping` flag; a wavefront drain that
  ignores it is a new way to freeze the GUI with no Stop.

Pick the closure, cap the iterations, honour `stopping` inside the drain, and
surface "still settling after N passes" rather than spinning. Write the choice
into `docs/simulation-semantics.md` — under #221's own process clause, semantics
changes are documented before code, and "what a wavefront is" is a semantics
statement, not a UI detail.

## Finding 5 — the missing asset nobody owns

AC-2 pins behaviour against "the shipped hazard-demo circuit… against the existing
goldens." The repository contains four `.jls` files
(`test/fixtures/riscv-sum1to10.jls`, `fork-4.6-shiftregister.jls`,
`headless-canary-gate.jls`, `riscv/gui/cpu.jls`); none is a hazard demo, and no
issue in the repo owns creating one. CAP-23 §1 step 1 opens with it, and #695
depends on it too. The demo circuit is load-bearing for an entire capstone and is
currently a phantom. Also note "the existing goldens" pin *outputs*, not event
order — there is no event-order golden to extend.

## The alternative framing I would actually take

**If only one of PF-3 and PF-4 ships, ship PF-3.** #693's stated purpose — "per-
gate delay stops being a number in a dialog" — is delivered *better* by the
cause-chain tree (CAP-23 PF-3 / lf-03 (a)) than by stepping. The tree shows the
delays accumulated along both reconvergent paths, statically, permanently,
inspectable at leisure, diffable in a grader, and it answers the question the
student actually has ("why is this wrong") rather than the one the tool can
currently answer ("what happens next"). Stepping shows one delay at a time and
only while you are holding the button; hneemann's Digital already does forward
gate-stepping well, so PF-4 is parity work, while the schematic-native causal tree
is the thing nobody has.

So I am **disregarding AC-4's "interactive-only" framing and AC-1's two-buttons
framing**, and would rewrite the task as:

1. `Simulator.stepEvent` / `stepTimestamp` / `runUntil` in `jls.sim`, headless,
   with `runEventLoop` untouched.
2. A single **Step** control in the GUI with a unit selector (event / instant /
   time amount), replacing today's Step and re-implementing Animate on top —
   fewer controls, not more.
3. An off-by-default event-sequence tap plus `-b --step-trace`, and AC-2/AC-3
   restated as a byte-diff against a free-run trace on `headless-canary-gate.jls`
   and one new hazard fixture, which this task creates and #695 reuses.
4. A "what just happened" caption naming the element, its delay, and the value —
   the smallest thing that makes the mode teach before #695's animation lands.
5. One paragraph in `docs/simulation-semantics.md` defining the wavefront quantum.

That is the same 1–1.5 mw of visible surface, aimed at a seam that also completes
#535, unblocks P7-C, and deletes more `InteractiveSimulator` state than it adds.
