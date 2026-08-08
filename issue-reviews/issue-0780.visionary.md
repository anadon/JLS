# Issue #780: TASK-C552-3: lessons are entered from the welcome pane and the Examples menu and render on-screen, and lesson 1 is completed from prompts alone
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-27 (#511) is the cheapest capstone on the board and gates every adoption-facing
one: a switcher bounces in the first ten minutes. #780 is the last mile of PF-5 —
the surface that turns #777's lesson content into something a newcomer follows
without reading anything else. The *outcome* is right and I endorse it.

The design it proposes is not. As written, #780 builds a fourth stepped-text
surface into a jar that already has three: `jls.Help` (TOC tree + `JEditorPane`
over `resources/help/**`), `jls.Tutorial` (186 lines: ordered pages, Previous/Next,
"Page x of y: title", non-modal so the canvas stays live — pinned by
`TutorialNavigationTest`), and, per #550 AC-2, a welcome pane whose Tutorial button
already opens the second one. AC-1 of this issue — "renders its steps on screen
with forward and back navigation, driven entirely by the lesson data format and
holding no lesson text of its own" — is a description of `jls.Tutorial` with its
`PAGES`/`TITLES` arrays swapped for a data source. Two of the five acceptance
criteria are then spent on not colliding with the widget it duplicates (AC-5) and
on deciding whether to build it at all (AC-4).

That is the shape of a task that has found the wrong seam. The reframing below cuts
along a different one, and most of #780's stated criteria fall out of it for free.

## The seam nobody in this cluster has noticed: the op vocabulary

A build-along lesson *is* a sequence of editor gestures: draw this, wire that, set
that bit width, probe here, run it. JLS shipped a closed, validated, invertible,
serializable vocabulary of exactly those gestures under #167 —
`jls.collab.op.CircuitOp` with `AddElements`, `AddWire`, `MoveElements`,
`SetElementConfig`, `AttachProbe`, `ToggleWatched`, `RotateElement`, `FlipElement`
and their inverses (`docs/operation-layer.md`). Every editor gesture already flows
through one `OpSink` (`src/jls/edit/SimpleEditor.java:5547`), and there is a *typed
extension point* whose whole contract is observing every submit:
`collab.op-observer` (`src/jls/collab/op/OpExtensionPoints.java:25`, catalogued in
`docs/extension-points.md`).

So the concrete alternative: **a lesson step carries prose plus the op(s) that
accomplish it.** `{ prompt: "Drop an AND gate on the canvas", ops: [AddElements …] }`.
Nothing else changes about #777's format discipline — the prose stays free of
presentation markup and widget references, and a docs renderer prints prompts and
ignores ops. What the ops buy is everything this cluster currently hand-waves:

1. **Steps become checkable.** Register a lesson runner on `collab.op-observer`,
   match the learner's submitted ops against the step's, and Next lights up when
   they have actually done it — or says "not yet, the output is still unconnected".
   This is the only thing an in-tool panel can do that a docs page cannot, and it is
   the only honest justification for building one. A panel that merely displays text
   beside the canvas is a strictly worse `jls.Tutorial`: same interaction, more code.
2. **Lessons become authorable by recording.** Build the circuit once in the editor;
   the op stream is the lesson skeleton; the author writes captions over it. That
   collapses the authoring cost of #777/#779 rather than adding to it.
3. **TASK-C552-2 AC-4 becomes mechanically true.** #779 requires "a test asserts
   each authored lesson's final step produces a circuit that loads and simulates."
   Under a prose-only format there is no mechanism — you maintain a separate target
   circuit and hope it corresponds. Under an op-carrying format the steps *are*
   executable: replay them headlessly into a `Circuit`, run `BatchSimulator`, diff
   against the shipped example from #548. A lesson that has drifted from its circuit
   fails the build. That is the same class of guarantee as `HelpTopicsTest` and the
   `AllElementsRoundTripTest` fixtures, and it is the project's house style.
4. **KC-27-2 stops being a fork and becomes a property.** AC-4 asks for a recorded
   decision on whether the in-tool variant survives its band. Replay a lesson
   headlessly, snapshot each step through the shipped `-i out.svg` export, and the
   docs page is a *build product* of the same source — prompts plus a per-step
   picture, always in sync. The in-tool-versus-docs decision the issue wants
   recorded no longer has to be made: you get both from one authoring act, and
   #551's gallery and #586's "no image can outlive the UI it claims to show" get
   their step imagery from the same rig.

Honest gap: simulation control ("run it", "watch this value") is not in the op
vocabulary — `ToggleWatched` and `AttachProbe` are, but start/step/pause are
`InteractiveSimulator` state. The final step of each lesson therefore needs one
small non-op predicate (sim ran; watched value settled). Say so in the design
rather than discovering it; do not widen `CircuitOp` to absorb sim control, which
would pollute the collab replication vocabulary with non-mutations.

## On the renderer: generalize `Tutorial`, do not add a sibling

`jls.Tutorial` is already a non-modal, resizable, ordered, prev/next viewer with a
position label, opened at an arbitrary index, reachable from Help and from #550's
welcome pane, and covered by two tests. Make its page source an interface, give it a
lesson-backed implementation, and AC-1 and AC-2 are satisfied by a diff measured in
tens of lines instead of a new panel. AC-5 ("Help→Tutorial superseded but not
removed") then dissolves entirely: the four legacy pages are just another sequence
in the same viewer, and the #73 IC2 promise the #552 comment is protecting ("the
panel's Open tutorial opens the same refreshed tutorial the README points to") stays
literally true instead of becoming a boundary that has to be policed. This also
respects the recorded arc in ARCHITECTURE.md — "in-jar now, hosted docs are the
planned future, the in-app viewer shrinks to context-sensitive basics." Adding a
third in-jar text surface pulls against a direction that says the surfaces should be
converging, not multiplying.

## I am disregarding AC-3 as the verification instrument

AC-3 makes this task's completion depend on "the capstone's scripted fresh-user
protocol (CAP-27 AC-5)". That protocol does not exist and has no owner: the
capstone's own 2026-08-08 comment flags the n=5 screen-recorded trial as an *open
question*, currently stranded between #73 and #381, and the 2026-08-04 coverage pass
records AC-2's fresh-user protocol as `GAP-NOTED — owned by no single feature`. A
1–1.5 mw task cannot be gated on an unowned research instrument; as written #780 can
be simultaneously implemented and unclosable.

The op-checked runner supplies a better instrument, and supplies it as a by-product:
an opt-in, local, anonymous completion trace — steps attempted, steps completed
without a wrong move, wall-clock time to the final step. That turns CAP-27's
headline claim ("under ten minutes, prompts alone") from an unfalsifiable assertion
into a number the tool itself produces, on every machine that runs a lesson, instead
of five recruited humans once per release. Keep a human trial if the maintainer
wants qualitative signal — but it should not be the gate on this task, and it
belongs at capstone close-out, not here.

## If the reframing is unaffordable

If the op-checked runner does not fit the band, the right answer is **not** the
uncheckable panel this task describes — it is to exercise KC-27-2 immediately and
ship the three lessons as docs pages reachable from the welcome pane and the
Examples menu, closing #780 as "presentation deferred". The uncheckable panel is the
one outcome worse than either alternative: it costs GUI code, a new test surface, and
a fourth text viewer, in exchange for a reading experience the existing Tutorial
dialog already delivers.

## Summary of the reframing

- Lesson steps carry prose **and** ops from the shipped `jls.collab.op` vocabulary.
- The runner registers on the existing `collab.op-observer` seam; Next advances on
  observed completion, not on a button press alone.
- The renderer is a generalization of `jls.Tutorial`, not a new panel; AC-5 dissolves.
- Docs pages and per-step imagery are build products of headless replay, so KC-27-2
  becomes a property rather than a decision.
- Verification is the recorded completion trace; the human protocol moves to
  capstone close-out where CAP-27 already admits it lives.
