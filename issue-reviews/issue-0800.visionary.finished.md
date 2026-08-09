# Issue #800: TASK-C587-2: element pages assert their ports and parameters against the registry descriptor, and hotkey accuracy generalizes past the one table it covers today
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

CAP-35 (#519) says the documentation *system* must make drift impossible; FEAT-C35-4
(#587) narrows that to "documentation that contradicts the program fails CI". This
task takes two thirds of that: element pages must not lie about ports/parameters, and
accelerator claims must not lie anywhere in the tree. Both goals are right and worth
having. The *route* the issue picks for the first one rests on two things that do not
exist in this repository, and it aims at a representation that its own sibling task is
about to delete.

## The premise of AC-1 does not hold

"The registry descriptor" is `jls.elem.ElementType`
(`/home/user/JLS/src/jls/elem/ElementType.java`): tag, alias set, element class,
factory. Four fields. Its class comment records the #78 decision explicitly — it
"carries only what loading, saving, and headless tooling need. GUI concerns — palette
icon, category, help topic, creation dialog — belong to a separate GUI-side palette
entry and never appear here." There is no port list and no parameter list in the
descriptor, and putting them there is the one thing #78 decided against. AC-1 as
written can only be satisfied by breaking a recorded architectural decision or by
quietly redefining "registry descriptor" to mean something else.

The other half of the premise fails harder. The element pages contain no port lists
and no parameter lists to be stale. `resources/help/elements/components/adder.html` is
fourteen lines of prose that never names `A`, `B`, `Cin`, `S` or `Cout` — the ports
`Adder.init` actually creates (`src/jls/elem/Adder.java:103-107`). Grepping the whole
of `resources/help/elements/**` for port names (`Cin`, `WE`, `OE`, `CS`, "port")
returns nothing but false positives. So AC-3's first planted defect — "an element page
with a stale port list" — has nothing to plant into. The task must first *author*
port tables into ~20 pages, then assert them. That is a content project dressed as a
test project, and it is priced at 0.5–1 mw.

Where pages do document parameters, they document *dialog labels*:
`resources/help/elements/memory/memory.html` names **Bits/Word**, **Capacity
(words)**, **Synchronous write (clock-edge)**. The code-side names are `bits`, `cap`,
`sync` (`Element.savedAttributes`, #23). Binding those two sides requires a
hand-maintained label→attribute table across 35 element types — precisely the kind of
hand table that this same issue's AC-2 exists to escape for hotkeys
(`DOCUMENTED_OPS` in `test/jls/HotkeysHelpAccuracyTest.java:55`). AC-1 would retire one
17-row hand table by building a several-hundred-row one.

And `savedAttributes()` is the wrong truth anyway: it is the save-format field list
(`id`, `x`, `y`, `width`, `height`, `fixed`, `trpos`, `sid`, then the element's own).
No help page should document `trpos`. Ports are worse than merely absent from the
descriptor — they are *configuration-dependent*: `Memory.init`
(`src/jls/elem/Memory.java:184-200`) creates `input`/`WE` only for writable memories
and `clock` only when synchronous. A single frozen "documented port list" per element
is false for Memory, Register, the gates (input count), Binder and Splitter. Freezing
one is not drift-proofing; it is installing a new lie with a test guarding it.

## The reframing: generate the element reference, do not assert it

The ports and parameters of an element are already fully determined by code that runs
headlessly. `ElementType.create(circuit)` yields an instance; `getInputs()` /
`getOutputs()` yield named, width-carrying puts. Instead of writing an HTML-prose
parser that checks a hand-authored table against that, **emit the table from it**.

Concretely: a small doc-generation step (a `jls.doc.ElementReference` walker, or a
Maven goal in the #584 pipeline) instantiates every `ElementRegistry.all()` type at
defaults — and, for the configuration-dependent ones, at each documented configuration
— enumerates ports as `(name, direction, width expression)` and parameters as
`(dialog label, code name, default, range)`, and writes one fragment per element into
the docs source. The in-jar page and the hosted page then both *contain* the generated
block. The residual test is a fixed-point check — regenerate, diff, fail if different —
which is the same shape as `DeterministicSaveTest` and the reproducible-jar ratchet
this project already runs. Nothing is asserted because nothing can disagree.

This is better on four counts, not one:

1. **It fixes a real user gap.** JLS's element pages document no port names today. A
   student wiring a Memory has to guess `CS`/`OE`/`WE` from the drawing. The
   assert-route produces a build gate over content it must invent anyway; the
   generate-route produces the content as the primary deliverable and the gate for
   free.
2. **It handles the conditional ports honestly**, because it renders them from the
   same `init` that creates them, per configuration, instead of freezing one variant.
3. **It shrinks ARCHITECTURE.md's 16-step "adding an element" checklist** (steps 14–15
   at `ARCHITECTURE.md:133-142`), rather than adding a 17th "and update the port table
   in the help page".
4. **It has a second consumer already in tree.** The HDL export/import work
   (`src/jls/hdl/**`, #33/#59/#61) and the batch/autograder audience both need
   per-element port names; `HdlModel.Port` is the same triple. One generated reference
   serves docs, HDL cell-map documentation, and `docs/file-format.md` §7's neighbour
   table (which `FileFormatSpecTest` already keeps honest — the pattern to extend is
   *that* one, not the prose pages).

The one piece of new metadata this needs is the parameter side, and it is worth having
for its own sake: a small `ElementParameter(codeName, dialogLabel, kind, default,
range)` list per element class, consumed by the creation dialog *and* the doc
generator *and* cross-checked against `savedAttributes()`. Today the label lives in
hand-coded dialog Swing, the name lives in an `Attribute`, and the prose lives in HTML
— three copies. Making parameters data collapses them to one and gives the GUI a
generated-dialog path later. That is the architectural seam worth cutting along; a
regex over `<td>` cells is not.

## AC-2 survives, and gets simpler than proposed

The accelerator half is sound and cheap. Outside `hotkeys.html`, the entire content
tree mentions exactly five accelerators — `undoredo.html` (CTRL-z, CTRL-y, shift-CMD-z,
CMD-y), `paste.html` (ctrl-V), `cutcopydel.html` (ctrl-X, ctrl-C) — in three different
casings. So do not extend `DOCUMENTED_OPS`; **invert it**. Scan every page for
accelerator-shaped tokens `(ctrl|control|alt|shift|meta|cmd|command)-<key>`, fold them
through the existing `canonicalize`/`GLYPHS` machinery, and require each to be a stroke
that some `EditOp` actually binds on that platform — a set-membership check with no
hand table at all, so a *new* page mentioning a *new* key is covered on the day it is
written. Keep the existing per-row exact check for the hotkeys table on top. The mac
side is already solved: `canonicalKeyName` is glyph-free by construction (#265), so
running the same assertion with the mac accelerator mask costs a parameter, not a
design.

## Sequencing pulls against the project's arc

#584 (FEAT-C35-1) converts `resources/help/**` into a *build output* of a
Markdown/AsciiDoc source, and #587 AC-4 requires these ratchets to run against the
generated targets. This task is ordered only after TASK-C587-1, and #801 then retrofits
"run against generated targets". So the plan is: write an HTML-prose parser against
hand-written pages, have #584 replace those pages with generated HTML, then re-point
the parser. Building the ratchet against a representation scheduled for deletion is
work done twice. Order this task after the C584 pipeline lands, and the element
reference becomes a generator stage inside that pipeline instead of a parser bolted to
its output.

## Verdict and what I am disregarding

**rethink.** The goal — element documentation cannot lie about ports and parameters —
is right and I keep it. I am explicitly disregarding **AC-1** and the first planted
defect of **AC-3**: they specify asserting a hand-authored port table against a
descriptor that has no ports, in pages that have no tables, for elements whose ports
are configuration-dependent. Replace them with: (a) `ElementParameter` as data,
consumed by dialogs and docs; (b) a generated per-element ports/parameters block in
the #584 source tree; (c) a regeneration fixed-point test, with the planted defect
being a hand-edited generated block. **AC-2** I endorse, implemented as a
table-free scanner rather than an extended `DOCUMENTED_OPS`; **AC-4** (do not regress
`HelpTopicsTest` / `HotkeysHelpAccuracyTest`) I endorse unchanged.
