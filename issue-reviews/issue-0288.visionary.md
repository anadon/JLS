# Issue #288: GUI HDL export: a File-menu Export entry over the existing HdlExporter, keyboard-reachable and harness-drivable
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is a menu item. The actual end it serves is the
README's "deployment bridge": a student draws a circuit and gets it toward an
FPGA (`README.md:135`, `docs/grand-architecture.md` §5 "hdl.export/import/cosim",
§2 trajectory 2). Today that journey has a discontinuity at the very first
step — the drawn circuit is in the editor, and the only way out is to retype
its path in a terminal. Closing that discontinuity is unambiguously right and
belongs to #75's constituency. I endorse the outcome.

I do not endorse the design, and I am explicitly disregarding one Definition
of Done line: **"No changes inside `jls.hdl` (H1)."** That constraint, plus
§7.5's instruction to write "a private action-listener/helper in `JLSStart`
mirroring the Export Image item's structure," forces the new surface to
re-implement decisions that already exist, in the one class the architecture
most wants thinned. The issue then proposes a byte-identity diff test (P2) to
police the duplication it just created. A differential test between two
implementations is a drift *alarm*, not drift *prevention*.

## The precedent the issue cites proves the opposite point

§1 offers "Export Image" as the model. Look at what image export actually
does: the CLI path calls `CircuitRenderer.of(circ).exportImage(outFile)`
(`src/jls/JLSStart.java:355`) and the GUI path calls the identical method
(`src/jls/JLSStart.java:3035`). One implementation, two thin adapters. That is
why `CliImageExportTest` can exist without a byte-diff harness — byte-identity
is structural.

HDL export has no such object. Everything above `HdlExporter.export` lives
inline in `JLSStart`'s static main branch (`src/jls/JLSStart.java:373-475`):
emitter selection by filename suffix, board/pin-bindings loading (#213),
warning emission, temp-file-and-atomic-rename write, constraint-file naming.
A GUI item that "calls the same `HdlExporter.export` path the CLI uses"
inherits none of that and must re-decide at least the suffix→emitter rule and
the guarded write. So the image precedent's real lesson is at the *service*
layer, and the issue borrows it only at the *menu* layer.

Copying that item's structure verbatim also clones two defects worth not
propagating: `Util.isValidName` rejecting any filename with a hyphen or dot in
the stem, and `chooser.getCurrentDirectory() + "/" + fileName` hardcoding the
separator (`src/jls/JLSStart.java:3022-3030`).

## Reframing A — export is a use case, not a menu handler

Extract the export use case as an exit-free, headless object; make both
surfaces adapters over it:

```
HdlExportRequest(Circuit, Path target, Optional<Board>, Optional<PinBindings>)
   -> HdlExportOutcome(filesWritten, warnings, rejection)
```

It owns suffix→emitter selection, the model walk, the constraint text, and the
temp-and-rename write; it returns outcomes instead of calling `System.exit`.
`JLSStart`'s `-export` branch becomes ~15 lines that map the outcome onto the
#42 CLI contract (`jls: error:` on stderr, exit 1). The menu item becomes ~20
lines that map the same outcome onto `TellUser`. P2 stops being a diff harness
and becomes a one-line assertion that both call sites construct the same
request. §7.11's "same guarded pattern the CLI uses" stops being an instruction
a reviewer must verify by eye.

This is not more work than the issue as written — it is the same work
rearranged, and it *removes* ~80 lines from `JLSStart`. It also gives #213's
board/pin-constraint UI (named as future work in §13) and #215's bitstream
handoff somewhere to land other than a third copy.

The H1 constraint should be re-cut along the right seam: **no change to
emitters, `HdlModel`, or emitted bytes** (that is #59's, correctly fenced);
**additive extraction of the export use case is in scope**, and its home is
`jls.hdl` per the home-package rule in `docs/extension-points.md`. §10's
"stop and re-scope with #59" is the right guard against semantic drift and the
wrong guard against structural refactoring; as written it cannot tell the two
apart.

## Reframing B — one "Export…" item over a target registry, not N items

`hdl.exporter` is already a typed, `many`-cardinality extension point holding
both emitters (`src/jls/hdl/HdlExtensionPoints.java:25`,
`docs/extension-points.md:32`), and `HdlEmitter` already exposes
`fileExtension()` (`src/jls/hdl/HdlEmitter.java:27`). Yet no surface enumerates
it: the CLI picks the emitter with a hardcoded `endsWith(".v") ? Verilog :
VHDL` ternary (`src/jls/JLSStart.java:383-385`), and the issue's chooser would
hardcode `.v`/`.vhd` filters a second time.

Make the export targets a list both surfaces read. Then the File menu gets one
**"Export…"** item whose `JFileChooser` file-type filters *are* the target set
— JPEG image, Verilog, VHDL, and later PCF/bitstream. That is flat (it honours
the ownership comment's ruling 3: no new submenu), it uses the issue's own
recommended extension-filter mechanism, it unifies rather than multiplies
Export items, and a third emitter costs zero menu surfaces. The alternative
trajectory — one bespoke item per target — has the File menu growing an
"Export Verilog", "Export VHDL", "Export Constraints", "Export Bitstream" row
each time a roadmap issue lands, and #213/#215 are already queued.

If unifying with image export is judged out of scope for a task-tier issue,
the fallback is still "Export HDL…" with filters *derived from the registered
emitters* rather than a literal `.v`/`.vhd` pair.

## Reframing C — the menu surface should join the shared Action layer

`jls.edit.EditOp` exists precisely to abolish "one anonymous listener per
menu item": *"Adding a menu-bar item would have made a third copy. Instead each
editor builds exactly one Action per EditOp … so the three surfaces are no
longer separate copies"* (`src/jls/edit/EditOp.java:14-22`). That is #75's own
signature achievement — #288's parent feature. §7.5 then proposes exactly the
copy pattern `EditOp` was built to end, in a 3069-line class. Note also that
`EditOp` already carries `CLOSE`, a File-menu-scoped operation, so the enum is
not intrinsically canvas-only.

A `FileOp` sibling (or an extension of the same table) turning `fileMenu()`
into a table of label / mnemonic / accelerator / component name / help row
would give the whole File menu what #288 asks for one item: harness handles,
accessible names, and `HotkeysHelpAccuracyTest` coverage *by construction*.

This matters concretely because `docs/component-naming.md` has **no menu-bar
section at all** — it covers palette buttons, the mirror menu, and dialogs.
#288's Completion Criteria require "a stable component name per
`docs/component-naming.md`" for a widget class the document does not cover, so
executing it as written means silently inventing a scheme (`menu.file.exporthdl`?)
in a PR whose subject is HDL. Write the menu-bar naming scheme once, for the
menu, rather than N-1 times ad hoc.

## What I would keep unchanged

- The ownership ruling in the 2026-08-08 comment is sound and my reframing is
  compatible with it: #288 still owns the action; #386/#758 still discharge by
  `STATUS:` comment.
- Rejection semantics (P4): list offenders, write nothing. Correct, and it is
  the use-case object's contract, not the menu's.
- EDT-synchronous, no speculative `SwingWorker` (§7.9). Right call.
- The pin-test/display-test split for the chooser (§11). Right, and Reframing A
  shrinks what the display test must cover to "the menu item invokes the
  request builder."

## Risk of the reframing

Scope. The minimum viable version of this review is Reframing A alone: it is
cost-neutral, deletes code from `JLSStart`, and makes P2 trivially true. B is a
small increment on top (read `fileExtension()` instead of hardcoding). C is
genuinely a separate slice and should be split out to #75 if it does not fit —
but the component-naming gap it exposes has to be resolved somewhere before
this item can claim conformance.
