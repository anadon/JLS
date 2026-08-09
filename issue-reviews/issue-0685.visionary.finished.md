# Issue #685: TASK-C529-2: clicking a wire lands on its chronogram lane, creating the lane if the signal was never displayed — and the round trip is a scripted offline test
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Two things are bundled here and they are not the same size. The small one is a click
handler: canvas wire → chronogram lane. The large one is AC-2 — *"if the signal was
never displayed, the lane is created rather than the click being ignored."* That
sentence is the whole of causal debugging in disguise: it asserts that the student's
attention, not a decision made before the run started, determines what they can see.
JLS today decides everything up front (`InteractiveSimulator.findTraces`, `:973`, runs
once inside `runSim` after `traces.clear()` at `:614`), and the roadmap's own study
names this as the central defect — *"the history is wiped on every run… there is no way
to go back to time T without re-running from 0"* (`docs/capability-roadmap/lf-03-causal-debug.md`,
workaround 2).

So I read #685 as the first place in the tree where the project actually tries to break
the up-front-decision model. That claim is right and worth backing. Two of its four
acceptance criteria, as literally written, do not deliver it, and I am disregarding them
below and saying why.

## Objection 1 (load-bearing): the lane AC-2 creates is empty, and an empty lane is worse than no lane

`Trace` accumulates only forward. `Trace.addValue` (`src/jls/edit/Trace.java:180`) is
called from `findTraces` once with the value at run start (`:984`, `:1002`) and
thereafter only from `InteractiveSimulator.afterEvent` (`:885`, `:893`) as events retire.
A `Trace` constructed at click time at t=4200 has exactly one sample: whatever the wire
reads right now. The student clicked that wire *because they want to know what it did
before the bug* — and AC-2 hands them a flat line starting at the moment of the click,
visually indistinguishable from "this signal never changed." That is not a neutral
partial win; it is a diagnostic tool that lies, in exactly the scenario (`hazard-demo`)
the capstone is built around.

AC-2 as written can be satisfied by code that does this. I am disregarding it.

**Reframe A — separate the capture set from the display set.** The concept JLS is
missing is not "lane creation"; it is that *captured* and *displayed* are currently one
word. Split them: the simulator captures a bounded per-signal history for a capture set;
the chronogram displays a subset. Then AC-2's gesture is a display-set edit — free,
instant, undoable, and the lane arrives *with its past*. `Trace` already has the
retention machinery for this (`MAX_RETAINED_CHANGES = 100_000`, `:32`, issue #121) and
the storage is already per-signal; what is missing is only the decision to populate it
for signals nobody has opened a lane for.

**Reframe B (the one I would actually build) — journal the stimulus, not the signals.**
JLS's simulation is a deterministic function of (circuit, stimulus). The stimulus surface
is tiny: `Clock` and `SigSim`/`TestGen` are pure functions of time (`Clock.initSim`
`:384`, `react` `:404` reposts itself), and the only genuinely interactive input is a
student editing an `InputPin` mid-run. Journal *that* — a list of (time, ElementId,
value), kilobytes for any classroom run — and any signal's full history is recoverable
at any moment by silently re-running to `now` with that signal captured. Clicking a wire
that was never captured then produces a *complete* lane, not a stub. The same journal is
the missing substrate for lf-03's time-travel, for #532's cause chain, and — see below —
for the walkthrough test this very issue is trying to write. This is the reframing that
makes AC-2's hard case disappear instead of shipping it half-done.

## Objection 2: the mutation this task proposes already exists, fully plumbed

"Make this signal displayed" is a shipped, named, first-class gesture. `EditOp.PROBE`
(`SimpleEditor.java:1313`), Ctrl-P (`:1433`), the option-menu entry
(`OptionMenuPolicy.java:201`), executed via `doProbe` (`:5222`) which submits
`AttachProbe`/`RemoveProbe` collab ops (`src/jls/collab/op/AttachProbe.java`). Those ops
are undoable, replicated to collaborating peers, persisted into the `.jls` file
(`WireEnd.java:612`), rendered on the canvas (`WireRenderer.java:94`), and consumed by
both simulators (`InteractiveSimulator:979`, `BatchSimulator:256`).

If AC-2 introduces a second path to "this wire now has a lane" that does not go through
`AttachProbe`, JLS acquires two notions of a displayed signal that disagree on undo, on
save, on collab, and on whether the canvas draws the probe glyph. That is a seam cut in
the wrong place. **The click should *be* the op.** One consequence is a genuine
improvement the issue never asks for: `doProbe` currently blocks on a modal name prompt
(`TellUser.prompt(null, "Name?")`, `:5236`) — which is flatly incompatible with AC-2's
"creates the lane *in place*." Default the name from the net (`WireEnd.getFullName()`,
already how the lane label is built at `:980`), make renaming an in-lane edit, and the
modal disappears from the fast path for everyone, not just cross-probers.

## Objection 3: AC-4 asks for one resolution path where there are honestly two relations

AC-4 demands "a single resolution path, not two that can disagree." But the two
directions resolve *different relations*:

- #684 (waveform → canvas): event → **the element that drove this value**. That is
  provenance, and it is information the tree actively discards —
  `WireNet.propagate` computes the winning driver and drops it (`WireNet.java:464-484`,
  quoted in lf-03), and `SimEvent.PinChanged` is a zero-field record that is 82.3% of all
  events fired.
- #685 (canvas → waveform): net → **the lane that displays it**. Static, structural, and
  already a `HashMap` lookup away (`traceMap`, `wireMap`, `InteractiveSimulator:94-96`).

Forcing these through one path can only go badly: either the cheap structural lookup gets
routed through provenance machinery that costs per event, or provenance degrades into
"whatever lane matches," which is precisely the by-name matching the feature title
condemns. Note also that TASK-C527-1 (#678) AC-4 defines its seam over *event* identity
and value only — it does not, by construction, carry the signal identity #685 needs.

**Reframe C — share the identity *type*, not the path.** Define one closed `SignalRef`
(`ElementId` for a watched element, canonical net key for a probed net, `(ElementId,
port)` for a pin) and let each direction have its own resolver returning it. Then "cannot
disagree" is enforced by the type system rather than by an unenforceable prose
requirement, and #684 AC-3's "element deleted between capture and click" case has a place
to live.

## Objection 4: this gesture makes the running simulation slower, by design

`afterEvent` (`:891`) loops over **every** entry in `wireMap` on **every** retired event,
calling `addValue` unconditionally. Per-event cost is O(probed signals) today. AC-2 hands
the student a one-click way to grow that set while the simulation runs — the more
diligently they diagnose, the slower their circuit gets. #684's AC-4 pins
`ChronogramClosedCostTest` (chronogram *closed*); nothing anywhere covers chronogram-open
cost, and this is the task that makes it user-driven and unbounded. Reframe A/B fix this
too: a pull-based lane over a journal costs nothing per event to open.

## Reframe D: the walkthrough deserves to be a project asset, not a test fixture

AC-3's `HazardDiagnosisWalkthroughTest` is the most valuable thing in this issue and it
is filed as a regression test. Consider what it is: a scripted, deterministic, offline
sequence of *diagnostic gestures* over a shipped circuit. That is the same artifact as
(a) the tutorial JLS ships today as static HTML and JPEGs — `src/jls/tutorial/tutorial*.html`,
including `AornotBprobe.jpg`, a *screenshot of a probe*; (b) an instructor's lecture demo;
(c) a bug report a student can send. Express the round trip as an AWT-free navigation/
session model with the panels as thin views, and the test lands in Layer 1 of the #91
harness (headless, no Xvfb, per `test/jls/ui/package-info.java`) *and* becomes replayable
in the app. Two further consequences worth stating: the hazard-demo circuit should live in
`examples/` (which today contains only `autograde/autograde.py` — nothing hazard-shaped
exists in the tree), and a stimulus journal (Reframe B) *is* the recording format for such
a script.

## Reframe E: this is the second consumer of a focus concept, so cut the seam here

Point-to-point wiring between two panels will be built at least three more times: #532's
cause-chain row → canvas, CAP-23 AC-4's viewer sync, and collab peer-presence (whose ops
already address elements by `ElementId`). Worse, the anti-pattern is already shipping:
`JumpStartRenderer.java:45-56` cross-highlights jump partners **by matching names**. A
small `focus(SignalRef|ElementId, Origin)` publisher — declared as an `ExtensionPoint`
per the recorded seam discipline in ARCHITECTURE.md — costs less than the third
point-to-point link and retires that renderer's string matching as a side effect.

## Verdict

**endorse-with-reframing.** The direction is right and it is the right moment: this is
where JLS stops deciding before the run what the student is allowed to see. Keep AC-1 and
AC-3. Disregard AC-2 as written (an empty on-demand lane is a misleading feature, not a
partial one) and replace it with: the click submits `AttachProbe` through the existing op
vocabulary, and the created lane shows history from t=0 — via a capture/display split or,
better, a stimulus journal with deterministic re-run. Disregard AC-4's "single resolution
path" and replace it with a shared `SignalRef` identity type plus per-direction resolvers.
Add an explicit chronogram-*open* cost criterion, since this task is the one that makes
that cost a user gesture.
