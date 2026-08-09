# Issue #75: Keyboard operability and accessibility: accelerators, focus model, shared Actions, and keyboard-only construction landed — residual: File>Close accelerator, GUI HDL-export entry, assistive-tech pass
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its residual, #75 makes one claim about what JLS should become: **every
capability of the editor is a named operation, invocable from any surface, and no
surface is privileged.** That is the same claim `jls.collab.op` makes for mutations
(`docs/operation-layer.md`: one closed vocabulary, one `OpSink` entry point), the same
claim `ElementRegistry` makes for element metadata (#78), and the same claim
`JLSStart.FLAGS` (`src/jls/JLSStart.java:759`) makes for the CLI. #75 is the GUI-input
instance of the project's strongest structural pattern, and `EditOp` +
`editAction(op)` is the right abstraction — `EditActionMatrixTest` proving object
identity across canvas binding, popup, and menu bar is exactly the invariant that
makes it real.

The residual as filed (a Cmd+W accelerator, one File-menu item, a human VoiceOver
session) is correct and cheap, and I endorse closing it. But it is bookkeeping. The
issue's stated Definition of Done would let #75 close while three things that are
*within its own capability statement* remain unbuilt or unbuildable. Those are worth
more than the checkboxes, and I would rather see them filed than see the checkboxes
completed cleanly.

## Reframing 1 — the keymap should be data, and the shadowing question should be a test

Open Question 1 (mask+W: Watch or Close?) is the third instance of one bug class, not a
new decision. The first was mask+S view-value shadowing File>Save
(`MenuAcceleratorPolicy.viewValueStroke` javadoc, `src/jls/MenuAcceleratorPolicy.java:271-287`).
The second was mask+W wire-start shadowing File>Close (comment 2026-07-19, prototype
built then withheld). The third is the residual. Every one is the same failure:
a `WHEN_FOCUSED` canvas binding silently outranks a `WHEN_IN_FOCUSED_WINDOW` menu
accelerator, so the *advertised* stroke and the *effective* stroke disagree. The issue
itself names this ("I4 … is exactly the lesson of the 2026-07-19 hold") and then
resolves it by owner adjudication — again.

This is mechanically decidable and the codebase is one small step from deciding it.
Today the binding table exists twice: `EditOp.accelerator(osName)`'s switch
(`src/jls/edit/EditOp.java:110-153`) and eighteen literal `register(...)` call sites
inside `SimpleEditor.initEditActions` (`src/jls/edit/SimpleEditor.java:1650-2011`,
e.g. `KeyStroke.getKeyStroke(KeyEvent.VK_P, menuMask)` restated for PROBE). They agree
only because a *display* test (`EditActionMatrixTest`, needs Xvfb) says so.

Concrete proposal: one declarative table — `record Binding(EditOp op, Scope scope,
KeyStroke stroke, boolean advertised)` beside `EditOp`, Swing-free and headless like
every other policy class in this project — plus `register()` taking its stroke from
`op.accelerator(osName)` instead of restating it. Then add the invariant nobody has
written: **no advertised window-scoped accelerator may be shadowed by a component-scoped
binding**, as a pure headless test over the table. Consequences:

- Open Question 1 stops being a maintainer call and becomes a data edit whose
  correctness the test asserts. (Option (a) is still right; the point is that nothing
  should have needed a year of adjudication to reach it.)
- `hotkeys.html` can be *generated* from the table rather than pinned against it by
  `HotkeysHelpAccuracyTest` — the drift class disappears instead of being detected.
- #570's rebinding UI (AC-3) becomes an overlay on a table that already exists, with
  the conflict checker it would otherwise have to invent; the dedup comment's warning
  ("a rebinding surface that bypasses `MenuAcceleratorPolicy` would silently break help
  accuracy") becomes structurally impossible rather than a note for a future funder.
- #549's ratchet gets a machine-readable subject; #594's palette search gets a command
  list to search over for free.

## Reframing 2 — #288 is a symptom; the fix is a GUI/CLI capability parity rule

HDL export is CLI-only not because someone forgot a menu item, but because the CLI's
capability list is a table (`FLAGS`) and the GUI's is hand-written Swing —
`exportImage()` got a File-menu item at `JLSStart.java:1525`, `-export` did not, and
nothing in the build notices. `-vcd` and `-savetext` sit in the same grey zone. #59's
future VHDL emitter will repeat the drift exactly.

Rather than land #288 as one item, land it as the first row of a completeness rule: an
`Export`/headless-capability catalog with a test asserting every user-facing headless
capability has both a CLI flag and a GUI entry (or a recorded, justified exemption) —
the same shape as `HelpTopicsTest`'s palette-coverage test and
`ExtensionPointCatalogTest`'s two-way catalog cross-check, both of which this project
already relies on. The issue's capability statement (f) — "everything a GUI user needs
… is reachable without the CLI" — is a *predicate over a set*, and predicates over sets
should be tests, not lists of remembered items.

## Reframing 3 — a grid caret is pointer emulation; a schematic wants a graph cursor

This is the one place I would disregard the acceptance criteria as written. I2 is
satisfied — `keyboardBuildsTheTwoGateCircuit` is genuine, and the focus-faithful driver
described in `docs/keyboard-a11y-verification.md` is the most rigorous piece of test
engineering in this repository. But arrow-nudge over a pixel grid is a mouse simulator:
it scales linearly with distance, and on anything the size of `riscv/` a keyboard user
is pressing arrow keys hundreds of times. The claim "the editor no longer requires a
pointing device" is true for two gates and false for a CPU.

The circuit is a *graph*, and the native keyboard model is graph-relative: next/previous
element in reading order, follow this wire to its far end, cycle the ports of the
selected element, jump to the driver of this net, jump to the next unconnected input.
That model — call it `CircuitCursor`, model-side, headless, testable without a display —
is simultaneously:

- the thing that makes keyboard construction usable past the tutorial;
- the substrate #544 needs for canvas announcements (the accessibility playbook already
  reaches this conclusion independently: `docs/standards-adoption/03-accessibility-conformance.md:420-428`
  proposes `AccessibleRelation.CONTROLLER_FOR`/`CONTROLLED_BY` for netlist topology and
  "a screen-reader navigation model (next/previous element, follow connection) layered
  on the existing #75 caret");
- the natural keyboard target for #594's placement, since "place next to the selected
  element's output" beats "nudge to coordinates".

#75 declared the canvas scene model out of scope in July, correctly, on cost grounds.
But *navigation* is not the scene model — it is the cheap half, it is pure model code,
and it is what would make the expensive half worth building later. I would file it here
rather than let it be reinvented inside #544.

**Related, and damning for a keyboard feature:** the caret #75 ships is drawn in
`JLSInfo.Palette.selectionColor`, which in `Theme.DEFAULT` is (240,240,240) — the grid
color — on white, ≈1.14:1 against WCAG 1.4.11's 3:1 floor
(`docs/standards-adoption/03-accessibility-conformance.md:186-198`, with the numbers).
The keyboard focus indicator of the keyboard-operability feature is invisible. It is
formally #76/#381's color surface, but "keyboard operability" that a low-vision user
cannot follow is not the outcome this issue promises. It belongs in the Completion
Criteria, delegated or not.

## Reframing 4 — stop gating closure on hardware nobody has

I3 (VoiceOver + Orca + macOS conventions, human + hardware) has been outstanding since
July and has no owner, no schedule, and — in a single-maintainer project — no realistic
date. A criterion that cannot be discharged is not a criterion; it is an issue that
never closes. Split it:

- **Machine-checkable now, headless:** walk the whole booted component tree and assert
  every focusable component has a non-blank accessible name/role (widening invariant 4
  from `AbstractButton` to everything — this is #549's ask and it does not need Orca).
- **Machine-checkable now, no Mac required:** `apple.laf.useScreenMenuBar` appears
  nowhere in `src/` (grepped). On macOS, JLS therefore draws its menu bar *inside the
  window* — the single most visible macOS convention violation the manual pass was
  meant to find, detectable today as a startup-policy assertion in the same shape as
  `LookAndFeelPolicyTest`.
- **Keep manual, don't gate:** the actual VoiceOver/Orca listening pass. The
  accessibility playbook already costs the AT-SPI CI route and recommends against it
  (`03-accessibility-conformance.md:548-563`) — so accept that, and make the human pass
  a `WAIVED:`-with-successor rather than a permanent hold on a feature that is otherwise
  done.

## Alignment, duplication, and one structural tension

#75 pulls *with* the project's arc almost everywhere: one vocabulary per concern, pure
injected-`os.name` policy classes, headless-testable decisions, evidence documents that
name their own falsification. `docs/keyboard-a11y-verification.md`'s "a green test proved
nothing about a real keystroke" section is a model the rest of the UI work should copy.

The one place it pulls against the arc is housing. The shared `Action` layer — the piece
#75 advertises to #73, #84, and #91 as a provided interface — lives as an `EnumMap` field
and ~390 lines of anonymous `AbstractAction`s inside `SimpleEditor.EditWindow`, an inner
class of a 5,852-line file that #316/#84 exists to decompose and that #594 treats as a
*hard gate* ("nothing here lands in the god class"). A public interface embedded in the
class everyone is trying to dissolve will be re-cut anyway. Extracting `EditCommands` as
a standalone per-editor registry — the same move `MenuAcceleratorPolicy` and
`KeyboardConstructionPolicy` already made — is both #75's cleanest close-out and the
most obvious first slice of #84, and it is what makes Reframing 1's table implementable
without touching the editor.

## Bottom line

Endorse the residual: close #288, take option (a) on mask+W, waive the AT pass with a
successor. But #75's real legacy is the command-vocabulary pattern, and it is currently
half-built and locked in a god class. If one thing is filed out of this review, file the
declarative keymap table with its shadowing test — it retires an open question, deletes a
duplicate table, generates the help page, and hands #549, #570, and #594 their substrate.
If two, add the graph cursor, and stop calling grid-nudge keyboard construction.
