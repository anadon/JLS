# Issue #695: TASK-C533-2: each step animates the propagation wavefront along the wires, highlighting the producing and consuming elements as its delay is spent
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

The parent feature (#533) and the capstone (#504) both want one thing: JLS
computes a timed causal trajectory and shows the student only its endpoints.
#693 delivers the control (advance by one event / one wavefront) and the time
readout. **#695 is where the whole pedagogical claim actually lands** — "per-gate
delay becomes time a student watches pass." Everything else in PF-4 is
plumbing; this task is the lesson.

That end is right and worth funding. The route in the acceptance criteria is
not: as written, AC-1 draws delay in the one place the model has none, and AC-3
removes the only property that would make the delay legible at all. I am
explicitly disregarding AC-1 and AC-3.

## AC-3 contradicts the outcome sentence, and that is the important finding

> **AC-3.** Animation duration is a view setting independent of simulated time.

Read that against the outcome: *"the elapsed portion of the gate's delay is
visible as it is consumed."* If every step's animation takes the same wall-clock
duration, a gate with `propDelay` 1 and a gate with `propDelay` 50 animate
identically. The student watches fifty steps of equal length and learns
precisely nothing about relative delay — which is the entire deliverable. Worse,
AC-2's simultaneity claim degrades to a coincidence of the animator rather than a
property of the circuit.

The correct view setting is **a rate, not a duration**: milliseconds of wall
clock per unit of simulated time (a time-dilation slider — 200 ms/unit for a
lecture, 5 ms/unit for a long run). Under a rate, everything the issue wants
falls out for free and *truthfully*: a 50-delay gate takes fifty times as long as
a 1-delay gate; every event at timestamp T lands at the same wall-clock instant,
so AC-2's simultaneity is simultaneity rather than a rendering convention; and
"changing it changes no simulated value" is still trivially satisfied, because
the rate only maps an existing axis onto the wall clock.

This single substitution is the highest-value change available in this issue.

## AC-1's "from producer to consumer" is not a path that exists

`docs/simulation-semantics.md` §6.1 is normative: *"**Wires are ideal**:
propagation across a net takes zero simulation time … all delay in JLS lives in
elements, none in wiring."* The parent's review already objects that wire-crawl
teaches inverted physics. At task altitude there is a second, purely mechanical
objection the feature issue could hide but this one cannot: **there is no
producer→consumer route to animate.**

- A propagation event targets a `WireNet` (`src/jls/elem/WireNet.java`), which is
  a `LinkedHashSet` of `Wire` segments and `WireEnd`s — a graph, not a path. No
  code anywhere computes a route from the driving `Output` to a given `Input`;
  it would have to be invented, and for a fan-out-of-8 net there are eight
  routes to sweep concurrently.
- `JumpStart`/`JumpEnd` put geometrically disconnected pieces on one net by
  design. Any wavefront sweep must teleport across the canvas mid-animation, at
  which point the visual is no longer describing motion.
- `Splitter`, `Binder`, `InputPin`/`OutputPin`, `SubCircuit` and `Constant` are
  zero-delay (§6.2). A single "step" therefore cascades through a chain of them
  within one timestamp — and a `SubCircuit` hop lands the consumer in a
  *different editor window* that may not be open. AC-1's "highlights both ends"
  has no defined meaning there.

So AC-1 asks for a graph traversal that does not exist, to animate transit time
the spec says is zero, along a geometry that is sometimes discontinuous, ending
at consumers that are sometimes off-screen.

One factual correction to the sibling review of #533, since it changes what has
to be built: **on-canvas value rendering already ships.**
`src/jls/edit/WireRenderer.java` colours every wire from `w.getValue()`
(`nonZeroColor` / `wireZeroColor` / `wireOffColor`) and carries the same state in
a second, colour-blind-safe channel via `strokeFor` (thick / thin / dashed,
issue #76), and the canvas is repainted at each step end
(`InteractiveSimulator.beforeEvent` → `edRef.repaint()`). The missing vocabulary
is not "wires show values" — it is **"an element shows work in flight."**

## The reframe: give the canvas a view clock, and animation stops being a feature

Do not build an animator. Introduce one scalar — `viewNow`, a position in
*simulated* time, possibly fractional — and make the canvas a function of it.
Then:

- **Rendering a moving `viewNow` is animation.** No frame-by-frame choreography,
  no per-step script, no wire-path solver.
- **A pending `SimEvent` at time `t` targeting element `E` with delay `d` draws
  as a fill inside `E`'s body, at fraction `(viewNow - (t - d)) / d`.** That is
  the delay being spent, drawn where §6.2 puts it. When `viewNow` reaches `t`,
  the fill completes and the net flips instantly — which is exactly §6.1.
- **AC-2 is free.** Every event at timestamp T completes its fill at the same
  `viewNow`. Simultaneity is visible because it is real.
- **A static-1 hazard shows both reconvergent paths filling at once, with
  unequal remaining time, in the same frame** — CAP-23 §1 step 2's insight,
  delivered here without PF-3's event-graph retention.
- **#693's two controls become predicates on the same scalar:** step-one-event =
  advance `viewNow` to the next event time; step-one-wavefront = advance to the
  next distinct timestamp; the existing "Animate" button = advance continuously
  at the rate. That retires the current `Animate` idiom (a `java.util.Timer`
  pressing Step once a second, `InteractiveSimulator.java:392–429`) rather than
  adding a fourth temporal control beside it.
- **#535's rewind is the same control run backwards.** The dedup comment on #533
  is right that the *mechanisms* differ (replay vs. step) and I am not disputing
  the no-merge call — but both are "render the canvas at time T", and shipping
  two independent notions of displayed time is the mistake CAP-23 §3 risk 1
  forbids one abstraction lower ("never two cause-chain models").

### Where the seam goes, concretely

1. `jls.sim` publishes an **immutable, AWT-free in-flight snapshot** — for each
   pending event: target element id, scheduled time, originating time. It is
   built only when a consumer is attached, so the base hook stays the no-op it
   is today and PF-1's KC-23-1 tap-cost ratchet is paid once, not per feature.
   `HeadlessCoreRatchetTest` keeps this honest by construction.
2. `jls.edit` adds an **overlay pass** — a third layer in
   `CircuitRenderer.draw`, after `wires` and `parts` (`CircuitRenderer.java:143`)
   — that draws the fills and the producing/consuming highlights. **It must not
   go inside the per-element renderers**: `ElementRenderers.draw` is the same
   path `exportImage` (PNG/JPEG/SVG) and `print` use, and
   `SvgExportTest.exportingTwiceIsByteIdentical` plus the image-export goldens
   would start capturing animation state. A separate pass, skipped when no
   snapshot is attached, makes AC-4 structurally true instead of asserted.
3. The frame clock is a `javax.swing.Timer` **on the EDT**, reading the published
   snapshot. Under no circumstances should the sim thread sleep for the duration
   of a step: AC-1's "for the duration of the step" reads as exactly that, and
   coupling wall time into the Runner thread re-opens the `volatile`/`pauseSem`
   class of bug that issue #49's H7/H8 findings and `EdtViolationDetector` exist
   to prevent. **Simulate, publish, then play back.** One direction of data flow,
   no new shared control flags.

### Testability, which the criteria as written do not have

`test/jls/ui/package-info.java` records that layer 3 exists but is deliberately
*"semantic checks … never brittle pixel goldens"* — so a frame-by-frame
animation golden is out of bounds by policy, not just by cost. AC-1 and AC-2 are
currently unfalsifiable. Under the view-clock design they become layer-1 headless
assertions on the snapshot: at `viewNow = T`, the in-flight set is exactly
`{...}` with fractions `{...}`; a wavefront advance yields entries whose
completion times are all equal; with no consumer attached the snapshot is never
constructed. Name it `InFlightSnapshotGoldenTest`, sitting beside the existing
`SimulationSemanticsRegressionTest` — same fixtures, same oracle discipline.

## Rewritten acceptance criteria

1. Pending events render on their target elements as a delay fill positioned by
   `viewNow`; nets change value instantaneously when their event fires.
2. Advancing one wavefront completes every fill at that timestamp in the same
   frame; asserted on the snapshot, not by eye.
3. The view setting is **wall-clock milliseconds per simulated time unit**; a
   gate with ten times the delay visibly takes ten times as long. No simulated
   value or recorded output depends on it.
4. The overlay is a separate render pass fed by an attachable snapshot; with
   nothing attached, no snapshot is built and no overlay code runs — pinned for
   batch, image export, SVG export, and print.

## Bottom line

Keep the outcome; it is the most vivid thing CAP-23 contains. Discard the
mechanism. Wire-crawl animation cannot be drawn (no route), should not be drawn
(§6.1), and a fixed per-step duration defeats the lesson it exists to teach.
A view clock plus an in-flight overlay is smaller, truer, testable headlessly,
shared with PF-1 and #535, and it makes "animation" a consequence of rendering a
moving clock rather than a fourth temporal mode bolted onto a control path that
has already had concurrency bugs. Sequencing note, inherited from the parent:
AC-1 of #533 leans on a "shipped hazard-demo circuit" that does not exist —
`examples/` contains only `autograde/`, and this task's demo value depends on it.
