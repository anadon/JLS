# Issue #702: TASK-C535-1: dragging the cursor back to T shows the circuit at T — by deterministic re-simulation, with honest replay progress and no checkpoint code path
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not "re-simulation". The end is: **the schematic stops being stuck at the
end of the run.** CAP-23 (#504) §1 step 4 wants a student to drag the chronogram
cursor onto a glitch and see the canvas as it was at that instant, so the waveform
and the drawing agree about the past. #702 answers "how do we obtain state at T?"
with one mechanism, then spends three of its four acceptance criteria defending that
mechanism (a no-serialization ratchet, a progress bar, a kill criterion). That is a
lot of apparatus in service of an implementation choice the outcome never required.

## The reframing: the canvas is a lane of the recording, not a re-run

The project has already recorded the principle this task needs, in the very document
#702 cites to justify its exclusion. #498 §7.2: *"An interactive session is a
recording device; the recording, not the session, is the contract."* And #498 §8
exclusion 2 forbids a specific thing — *"a permanent per-element serialization tax
across the whole element library in service of re-running a boot"* — not the act of
retaining values.

JLS already **is** the recording device, today, in the code #702 will build on:

- `src/jls/edit/InteractiveSimulator.java:879` (`afterEvent`) appends every watched
  element's value and every probed wire's value, with its time, into `Trace`.
- `src/jls/edit/Trace.java:180` (`addValue`) stores `(BitSet value, long when)`
  changes, deduplicated, retained to `MAX_RETAINED_CHANGES = 100_000` (`Trace.java:32`).
- `src/jls/edit/Trace.java:445` (`firstChangeAtOrBefore`) is already a binary search
  for *the value a signal held at time T*.

So "the value of this net at T" is an O(log n) lookup that ships at HEAD. TASK-C527-1's
tap generalizes exactly this stream. The alternative design is therefore:

> **Rewind is a rendering operation, not a simulation operation.** With time-travel
> armed, the tap records every net's transitions (mode-gated, so `ChronogramClosedCostTest`
> is untouched). Dragging the cursor to T sets a *display time* on the canvas; every
> wire, probe and status readout paints `firstChangeAtOrBefore(T)` instead of "live".
> The engine is never rewound, never re-run, never touched.

This is not the checkpoint path and it is not exclusion 2's tax: no element serializes
itself, no `.jls` section is added, nothing persists past the session. It is the
chronogram's own data, painted on the schematic. Memory and register *interiors* — the
one thing net history does not carry — are reconstructible from the `SimEvent.MemoryWrite`
/`NewValue` payloads already flowing through the tap (initial image plus writes ≤ T),
which is O(writes), targeted, and again not a per-element snapshot.

Cost of the reframing: scrubbing is smooth at 60fps instead of O(T) per drag; there
is no progress bar to build, nothing to cancel, and nothing to restore on cancel.

## What the reframing dissolves, and what it keeps

- **AC-3 disappears entirely.** No long operation, no cancel, no "return the view to
  its pre-rewind time".
- **AC-1 changes shape and gets *better*.** "Displayed state at T equals a fresh run
  truncated at T" is, under the recording design, a statement about engine determinism
  and tap fidelity — which deserves to be a standalone invariant test over the whole
  engine, not a test of a GUI gesture. Keep the test; move it off the rewind path.
- **AC-2's intent survives; its letter should be dropped** (see below).
- **AC-4 (KC-23-3) stops being a project-stopping risk**, because the displayed past
  is the past that actually happened, byte for byte, by construction.
- **Honest boundary, kept:** retention is bounded (100k changes today). Beyond the
  retained window the cursor cannot seek. That is the honest limit to state in the
  UI — and it is where the issue's replay belongs, as the *fallback*, not the primary.
  Hybrid: seek inside the window, offer replay outside it.

I am explicitly disregarding AC-1's binding of the outcome to re-simulation and AC-3's
progress/cancel apparatus. The outcome (step 4 of #504 §1) is unchanged and, I claim,
better served.

## Three concrete facts that pull against replay-in-place

1. **The Circuit being replayed is the Circuit being drawn.** Element state lives in
   the element objects the canvas paints; there is no second copy. Replaying to T
   means running the live circuit forward from 0 (`Simulator.initSimulation`,
   `src/jls/sim/Simulator.java`, clears queues and re-`initSim`s every element), so
   the canvas visibly passes through every intermediate state, and the pre-rewind
   time is only recoverable by *a second full replay*. AC-3's cancel is O(T) twice.
2. **The clean way to avoid that is the one AC-2 forbids by name.** A shadow replay
   on a copy of the circuit is obtained through `CircuitSnapshot`
   (`src/jls/edit/CircuitSnapshot.java`) — deflated save-format text through the
   ordinary load path, the same mechanism undo uses. That *is* "a state-serialization
   entry point". AC-2 conflates "don't build FEAT-035's per-element checkpoint tax"
   with "never call save/load", and in doing so forces the destructive design.
3. **The most likely cause of KC-23-3 is a one-line bug that lives inside the
   dependency #702 disowns.** `SimEvent.sequence` is `static`
   (`src/jls/sim/SimEvent.java:86`) and is the same-time tie-break in `compareTo`
   (`:134`). A shadow or concurrent replay interleaves sequence numbers with any other
   live `Simulator` in the JVM (a second editor window, the paused live run), so
   same-time event order can differ between the original run and its replay — the
   exact byte-inequality AC-1 asserts against. The fix ("`SimEvent`'s static sequence
   counter made per-`Simulator`") is written down already: it is inside FEAT-035
   (#363) TASK-0074, the feature #702 declares "deliberately not a dependency."
   The exclusion boundary is drawn in the wrong place — it excludes the cheap
   correctness prerequisite along with the expensive machinery. Pull the per-simulator
   sequence counter out of #363 as a standalone hygiene task and depend on it.

## If replay is kept anyway, it is much smaller than 2–2.5 mw

The seam already exists: `InteractiveSimulator.runSim(true)` runs the shared event
loop with UI updates suppressed and — crucially — **does not clear the trace panel**
(`runSim` guards the trace reset with `if (!isQuiet())`,
`src/jls/edit/InteractiveSimulator.java:612`), while `afterEvent` records nothing when
quiet. So "replay truncated at T" is `setTimeLimit(T); runSim(true); repaint()` — the
loop's own `now <= maxTime` condition does the truncation. The genuine work in #702 is
therefore not the replay; it is the progress/cancel UI (AC-3) and the ratchet test
(AC-2), i.e. the cost is concentrated in the two criteria the reframing deletes.

## Does this strengthen the project's arc?

The outcome does; the mechanism pulls against it in one specific way. The project's
discipline is **one artifact, one writer**: TASK-C534-3 (#700) AC-2 asserts no second
trace writer exists; the batch/interactive VCD must be byte-identical (#504 AC-3);
#498 §7.2 makes the recording the contract. Rewind-by-replay introduces a *second,
independent source of truth for the same displayed fact*: the chronogram says the net
was 1 at T because it recorded that; the canvas says the net was 0 at T because a
re-run computed that. AC-1 exists precisely to police that divergence — a test whose
necessity is evidence of the duplication. Making the canvas read the recording removes
the divergence rather than testing for it, and it makes the schematic literally an
instrument of the recording device #498 says JLS is.

Two smaller alignment notes. (a) #508's product review recommends cutting PF-6's
viewer-sync half; the rewind half reviewed here is the survivor, so it deserves the
cheaper, more elegant construction rather than the one whose cost is defended by
kill criteria. (b) A canvas that renders a *time* rather than "live" is the same
primitive PF-4 wavefront stepping needs, and the same primitive an eventual
"scrub through a graded VCD without re-running it" workflow would need. Replay gives
none of that reuse; a display-time canvas gives all of it.

## Recommended shape

1. Re-file the outcome as: *the canvas gains a display time; at cursor time T it
   paints values from the tap's retained history*.
2. Keep determinism as a standalone invariant test of the engine (batch vs interactive,
   truncated runs), not as a property of a GUI gesture.
3. Depend on a small "per-`Simulator` sequence counter" task carved out of #363
   TASK-0074 — the only piece of that feature this work actually needs.
4. Keep replay as the documented fallback beyond the retention window, implemented as
   `setTimeLimit(T); runSim(true)`, with the progress UI scoped to that path only.
5. Replace AC-2's "no state-serialization entry point" with the honest ratchet:
   *no per-element checkpoint API is added, and no simulation state is written to disk*.
