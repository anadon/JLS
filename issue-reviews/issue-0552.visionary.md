# Issue #552: FEAT-C27-5: the first three circuits teach themselves — stepped build-along lessons a newcomer completes from on-screen prompts alone
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of the feature vocabulary, #552 exists to move one number: the 2/5 "on-ramp:
learning" score in #510 §1, the only dimension where JLS sits near the category floor while
being top-tier on semantics rigor and category-best on grading. The mechanism named in #510
§2 is the ten-minute bounce — a switcher leaves before discovering the parts of JLS that are
genuinely superior. So the real goal is not "three lessons exist." It is: **a stranger's first
ten minutes produce a circuit they drew and understand, with no reading longer than a
caption.** Everything below judges the issue against that, not against its AC list.

The goal is right and it is the cheapest lever on the whole board. I endorse the outcome. I
do not endorse the artifact the issue proposes to build, or its cost basis.

## The seam this issue cuts along is the wrong one

#552 treats a lesson as **prose plus a presentation layer** — that is what AC-4 is defending
(content authored separately from in-tool tooling, so docs pages can carry it if the band
blows). That framing is defensive rather than generative: it buys survivability and nothing
else. It also puts the feature at odds with the recorded direction in ARCHITECTURE.md ("Help
delivery: in-jar now, hosted docs are the planned future… when it happens, the in-app viewer
shrinks to context-sensitive basics"). A new in-tool HTML lesson surface grows exactly the
thing the architecture says will shrink, and it inherits the `JEditorPane` HTML 3.2 ceiling
that `jls.Help` and `jls.Tutorial` already live under (`src/jls/Tutorial.java`:141,
`editorPane.setPage`). "In-tool or docs, whichever fits the band" is offered as a coin flip;
the project has already recorded which side it lands on.

**The seam that generates instead of defends is the circuit file itself.** JLS already has,
shipped and tested:

- a line-oriented plain-text save format with a `FORMAT 1` header, and a supported way to
  produce it (`-savetext`, `docs/file-format.md`, `FileAbstractor`);
- `CircuitSnapshot` (`src/jls/edit/CircuitSnapshot.java`) — the editor already represents a
  circuit-in-progress as save-format text, and undo is literally a sequence of such states;
- headless load + simulate + a documented `-t` vector grammar (`docs/batch-interface.md`);
- resolution-independent per-circuit rendering (`-i out.svg`), which #511 PF-4 already relies on.

### Alternative framing: a lesson is a sequence of circuit files, not a sequence of paragraphs

Author each lesson as `resources/lessons/<id>/step-00.jls … step-NN.jls` (plain-text saves)
plus one caption line per step in a small manifest. Nothing else. Then:

- **AC-4 is satisfied by construction, not by discipline.** The content is data — .jls plus
  caption strings. A docs page is a render of it. An in-tool pane is a render of it. There is
  no way to accidentally fuse content to presentation, so KC-27-2 stops being a clause someone
  has to remember at task-breakdown time.
- **The step illustrations come free.** `-i step-03.svg` renders every step. Today's tutorial
  ships hand-made JPEGs (`src/jls/tutorial/halfadder.jpg`, `counter.jpg`, …) that will silently
  drift from the tool; generated renders cannot. This is the same pipeline PF-4's gallery needs,
  built once.
- **The lessons become CI-testable in the shape the repo already uses.** Every step loads
  through the ordinary reader (`AllElementsRoundTripTest` shape), the final step simulates
  against `-t` vectors (`BatchSimulationGoldenTest` shape). A lesson that has rotted — an
  element renamed, a dialog default changed — fails the build instead of failing a newcomer.
  That is a per-commit guarantee the issue's AC-2 cannot give, because AC-2's only oracle is a
  human protocol run occasionally.
- **The #548/#552 boundary dissolves.** The dedup comment spends a paragraph insisting a
  one-line "try this next" belongs to #548 and a stepped sequence belongs to #552, and predicts
  the two "will read as redundant during implementation." They read as redundant because they
  are one artifact seen twice: **an example circuit is a lesson's last step; the suggested
  exercise is the lesson's epilogue.** Author the step sequence, and #548's curated example for
  those three circuits is `step-NN.jls` with the caption already written. One corpus, two menus.
- **CAP-33 (#517) PF-4 wants exactly this packaging convention** ("kit = labs + vectors +
  schedule + rubric… so a third party can author one"). A lesson-as-step-corpus is that
  convention at its smallest instance. Building it here is not growth into CAP-33's scope; it
  is CAP-33 inheriting a format instead of inventing one, which is the difference between the
  two capstones sharing content (as both issues say they should) and sharing only prose.

The honest cost of this framing: a real "check my work" affordance needs a *structural*
comparison of learner circuit vs. step golden, and JLS has no such thing —
`CircuitSnapshot.sameAs` is byte equality over deflated save text (`CircuitSnapshot.java`:118),
far too strict when the learner placed the OR gate forty pixels left. Do not put a structural
comparator in this feature's band. Ship the step corpus and captions first; the comparator is a
follow-on whose value is not confined here anyway (importer loss-naming in #513, structural
rubrics in CAP-06/CAP-33 all want it).

## Second reframing: this is a refactor, not an authoring project

The issue and its dedup comment both assume the content is new: "#552 authors **new stepped
build-along lesson content**… a different artifact." Check the tree. `src/jls/tutorial/`
already contains build-alongs for **A + ~B** (place an OR gate, place a NOT gate, touch the
outputs, watch them turn green), **a 4-bit counter**, **a full adder built from half-adder
subcircuits**, and **sign extension** — Poplawski's own prose, GPL-clean, with figures. That is
the SimCast arc, already written, already covering more than three circuits. What it is not is
*stepped*: tutorial1.html is ~10 kB of continuous prose, and every step is a paragraph, not a
caption.

So the work here is: chop existing prose into caption-length steps, save the intermediate
circuit at each step, delete the drifting JPEGs in favor of generated renders. That is
plausibly 1–1.5 mw of editing plus corpus capture — not the 3–4 mw band #552 claims, and the
band matters, because KC-27-2 exists precisely to protect against this feature overrunning. A
feature whose cost estimate assumes from-scratch authorship will get planned, staffed and
scheduled as if the existing tutorial were not there, and will then produce a *second* body of
lesson content that must be kept consistent with the first — which is the outcome the
"supersedes for the first-run path but is not removed" clause guarantees. Two live lesson
corpora describing the same four circuits is a maintenance liability, not a boundary.

## Where I am disregarding the stated acceptance criteria

- **AC-1's unit of success ("three lessons for the first three examples") is wrong.** The unit
  is the on-ramp arc: one combinational gate circuit → one sequential circuit → one hierarchical
  circuit. The existing tutorial already picks those three well, and picking them again to match
  whichever circuits happen to sort first in #548's menu is arbitrary. Bind the lessons to the
  arc, and let #548's ordering follow the lessons rather than the reverse.
- **AC-2's verification should not rest solely on a human protocol.** Keep the scripted
  fresh-user run as the outcome measure, but add the mechanized invariant the step corpus makes
  possible: every step loads, the final step simulates green, in CI. Otherwise "completable from
  on-screen prompts alone" is true on the day it is written and unfalsifiable thereafter.
- **AC-5's "no new chrome" is achievable more cheaply than the issue implies.** `SimpleEditor`
  already has an in-view guidance channel — the status `JLabel` at `SimpleEditor.java`:141,
  cyan when idle, yellow during a gesture, and tutorial1.html already teaches learners to read
  it. A lesson step prompt riding that existing label plus a Next/Back control in the (already
  shipping) `Tutorial` dialog adds zero widgets to the default editing view. That is the whole
  in-tool surface worth building; anything more is the presentation layer AC-4 is trying to
  make disposable.

## Stretch idea, explicitly out of band

Because the undo stack is already a sequence of save-format snapshots, an instructor could in
principle *record* a lesson by drawing the circuit once, with the editor emitting the step
corpus and prompting for a caption per step. That would make CAP-33 PF-4's "a third party can
author a kit" nearly free and is the kind of thing #510 §2 means by JLS's under-leveraged
elegance. It is not this feature's work and should not be smuggled in — but the step-corpus
format is what keeps that door open, and a prose-plus-viewer lesson closes it.

## Verdict

**endorse-with-reframing.** The outcome is right, well-evidenced, and cheap relative to what it
gates. Reframe the artifact from "lesson prose + a presentation layer" to "a versioned sequence
of circuit files with caption-length steps," fold the three lessons' final steps into #548's
example set rather than defending a boundary between them, treat the existing
`src/jls/tutorial/` content as the source to refactor rather than legacy to supersede, and let
the recorded help-delivery decision — not the band — settle in-tool vs. docs.
