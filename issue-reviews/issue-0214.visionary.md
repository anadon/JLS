# Issue #214: In-editor test panel: a GUI front-end over the batch `-t` test-vector engine (Digital-parity, HDL-independent)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the scaffolding and the goal is one sentence: **a student should be able to
find out, inside the editor, whether the circuit they drew is right.** That goal is
squarely on the project's arc — `docs/grand-architecture.md` §1 names "two co-equal
front ends" and an audience of "first-year students and instructors," and §7 item 4
names in-editor test panels as mainstream. Nothing here pulls against the trajectory.

The *design* is another matter. The issue proposes to reach that goal by adding a
Swing panel that opens an external vector file and renders a table of verdicts. I
think that is the wrong seam, and I'd re-cut it before anyone writes the panel.

## The framing in the body is still false, and it matters

The Abstract says the capability is "CLI-only" and that this is "a small,
HDL-independent catch-up item because the simulation machinery already exists."
It does not exist. `docs/batch-interface.md` §2.2's grammar has four productions,
none naming an output; `SigSim.initSim` (`/home/user/JLS/src/jls/elem/SigSim.java`)
posts stimulus and compares nothing; the exit table has 0/1/2 and no "wrong answer."
The adversarial comment of 2026-08-08 established this and struck §8 step 1. But the
body itself — the thing a picker-upper reads first — still opens with the false
claim, and §7.4 still says the runner's signature is "fixed in §8 step 1." An issue
whose Abstract contradicts its own newest comment will be executed from the Abstract.

## Alternative A (primary): the test case is an *element*, not a panel over a file

The exemplar this issue cites — Digital's test-case component, named as
`TestCaseElement` in `docs/grand-architecture.md` §7 — **is a component placed in the
circuit and saved in the circuit file.** #214 copies the feature name and discards
the design. JLS already has every seam needed to copy the design instead:

- `jls.elem.SigGen` (`/home/user/JLS/src/jls/elem/SigGen.java`, 199 lines) is
  already an in-circuit, saved, declaratively-persisted element holding *exactly the
  `-t` stimulus grammar* as a `String signals` field.
- `jls.edit.SigGenDialog` (`/home/user/JLS/src/jls/edit/SigGenDialog.java`) is
  already an in-editor `JTextArea` editor over that grammar. **The "in-panel vector
  editor" that §13 defers to future scope has shipped since 4.1 — for stimulus.**
- `Palette` already has a `Group.TEST` toolbar group containing `SigGen`
  (`/home/user/JLS/src/jls/edit/Palette.java:179`).
- The #78 registry has landed: `jls.elem.ElementRegistry`, `jls.elem.ElementType`,
  `ElementDialogs.register` / `ElementRenderers.register`
  (`/home/user/JLS/src/jls/edit/BuiltinElementRenderers.java:42-45`). ARCHITECTURE.md's
  "honest ~16-step list" is stale; a `TestCase` element modelled on `SigGen` is a
  class, a dialog, three registration lines, a `Palette` row, a `SaveTags` row, a
  help topic, and a round-trip fixture.

What falls out for free if expectations live in a `TestCase` element rather than a file:

1. **Persistence, undo, and crash recovery, at zero cost.** `CircuitSnapshot`
   deflates the save text, so undo of a test edit is undo of a circuit edit.
2. **The pairing problem disappears.** Today a student must keep `circuit.jls` and
   `vectors.txt` together and pass both. An instructor shipping a lab ships one file
   with the tests already in it. This is the single biggest usability win available
   here and the panel-over-a-file design forfeits it.
3. **The batch path needs no new flag to run the embedded test** — the element is in
   the circuit, so `jls -b circuit.jls` elaborates it like any other element. The
   `-t` grammar stays literally untouched, satisfying #214's own hard criterion by
   construction rather than by the careful separate-file argument #466 §11 has to make.
4. **#466's most surprising behaviour evaporates.** O5/P5 exist because
   `BatchSimulator.addTestGen` (`/home/user/JLS/src/jls/sim/BatchSimulator.java:190-212`)
   *deletes the top-level `SigGen`s* and substitutes a `TestGen`, so the panel is not
   running the circuit on screen. With the test as an element, C' = C and there is
   no notice to write, no P5 to test, and no student to mislead.
5. **It closes a live CLI/GUI divergence that P2 will otherwise trip over.**
   `BatchSimulator.addTestGen` removes `SigGen`s; `InteractiveSimulator.runSim`
   (`/home/user/JLS/src/jls/edit/InteractiveSimulator.java:590-594`) constructs a
   `TestGen` and **does not** remove them — `grep -n SigGen
   src/jls/edit/InteractiveSimulator.java` is empty. The two front ends already
   simulate different circuits from the same inputs. A shared *runner* does not fix
   that; a shared *stimulus representation* does.

Honest costs: a new savable element is a `FORMAT` change, so a circuit with a
`TestCase` will not open in older JLS (the `NEWER_FORMAT` path exists and is the
right refusal). And an in-file test is authored by whoever holds the file — which is
precisely why it does **not** replace #466.

## Where both designs belong (they are not competitors)

- **In-file `TestCase` element** — the student's self-check and the instructor's
  shipped lab. Authority and artifact travel together; that is the point.
- **External expectations + `-check` + xUnit (#466, #757)** — cohort grading, where
  the authority must live *outside* the submission because the student can edit
  anything inside it. A grader that reads the student's own expectations grades
  nothing.

Seen this way, #214 as written is the awkward middle: an external file (grading's
trust model) driven from the editor (the student's context), serving neither well.

## Alternative B: if a panel is built, build the dock, not the panel

`TellUser` is documented in ARCHITECTURE.md as "the only place allowed to create
message dialogs" — so the editor's entire reporting vocabulary is *modal dialogs*.
A non-modal, scrollable, keyboard-navigable results dock is a missing organ, and
test verdicts are only its first tenant. The others already exist or are funded:
structured `LoadError`s (category + location + hint), simulation aborts
(oscillation, time limit), HDL export diagnostics from the #61/#63 subprocess tools,
collab notices. Contribute content through the typed seam catalog
(`docs/extension-points.md`, `jls.edit.GuiExtensionPoints`) — a `gui.results-panel`
point, so `TestPanel` becomes a *contribution* rather than another few hundred lines
welded to the ~4k-line `SimpleEditor` that #84 is trying to decompose. As specified,
#214 adds mass exactly where #84 is removing it.

## Alternative C: locality is the only thing the GUI actually adds

The issue never asks what the panel is *for* beyond "not dropping to a terminal." A
table of pass/fail rendered in Swing is strictly worse than `jls -b -check | less`.
What a GUI can do that a terminal cannot is **put the failure on the drawing**:
click a failing expectation → select and highlight the offending `OutputPin` on the
canvas (`jls.SpatialIndex` already does hit-testing), and position the existing
`InteractiveSimulator` trace window at that simulated time with that signal visible.
That is Digital-parity in spirit rather than in feature-name, and it is the reason
to spend Swing effort at all. It is absent from §5, §7.6 and §14.

## Trajectory: does this still deserve to be an issue?

After the 2026-08-08 split, #214 owns a widget and nothing else: the runner is
#466's, the report is #466's, the cohort command is #757's, the lab is #575/#576's,
and §8 step 1 is struck. The comments keep it alive on lower-number-wins
bookkeeping, not on design grounds. Either it grows into the front-end story
(element + dock + locality) and earns its number, or it is a checkbox inside #466.
The status quo — a placeholder that will be implemented literally — reliably yields
the verdict table nobody uses.

## Disregarded acceptance criteria, and why

I am explicitly setting aside §14's "the panel loads a user-selected vector file"
shape and §13's deferral of the vector editor. The vector editor exists
(`SigGenDialog`); what is missing is *expectations* and a *verdict*. Keep §14's
`-t`-grammar freeze, the no-verdict-in-`jls.edit` rule, the record-level parity
assertion, and the located non-fatal error (P4). Drop P5 — under Alternative A the
substitution it warns about no longer happens.

## Concretely, what I would do

1. Rewrite the Abstract to state the true starting point (there is no verdict).
2. Let #466 land `Expectations` + `TestVectorRunner` unchanged — grading needs the
   external file and the exit status.
3. File `TestCase` as an element (grammar = `-t` productions + expectation clauses,
   parsed by the *same* `TestVectorRunner`), palette `Group.TEST`, dialog cloned
   from `SigGenDialog`. This is #214's real deliverable.
4. Add the results dock behind a typed extension point, coordinated with #84.
5. Wire failure → canvas selection → trace-window scrub. That is the feature.
