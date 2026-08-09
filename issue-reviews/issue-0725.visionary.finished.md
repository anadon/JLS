# Issue #725: TASK-C540-2: every registered element type gets an ANSI/IEEE-91 distinctive print symbol, and the palette sweep exports with zero warnings
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this task is really for

#725 is the authoring half of #540's two-task split: #723 builds an
element-type-to-print-symbol mapping with a red totality ratchet, and #725
fills it with ANSI/IEEE-91 distinctive shapes so the ratchet goes green. The
end it serves is CAP-24 (#505) step 5: *the camera-ready export must cover the
whole palette and keep covering it as the palette grows*.

I am disregarding acceptance criteria 1 and 4 as written, and re-aiming 2, 3
and 5. The reason is not that they are wrong in spirit but that the thing
#725 proposes to author **already exists in this tree**, and the thing the
project actually lacks — an auditable statement of what its symbols *are* —
is only glanced at by AC-2's "recorded with a reason."

## Ground truth at HEAD

- **The distinctive shapes ship, as headless data.**
  `/home/user/JLS/src/jls/elem/AndGate.java:44-54` builds the ANSI/IEEE-91
  distinctive AND — flat back, two sides, a 180-degree arc — through
  `GateOutline.builder()`. `OrGate`, `NandGate`, `NorGate`, `NotGate`,
  `XorGate` are the same, and `/home/user/JLS/src/jls/elem/GateOutline.java`'s
  javadoc states the intent explicitly: "express an element's drawn geometry
  as a headless description the model side owns." AC-3 asks for "symbol
  geometry is data" and AC-2 asks for "distinctive shapes where the standard
  defines one." For the six gate leaves — the only elements ANSI/IEEE-91
  actually defines a distinctive shape for — both are satisfied by doing
  nothing. `/home/user/JLS/docs/standards-adoption/01-iec-ieee-symbols.md:13-15`
  says the same in the project's own words: the current gates are "not
  *non*-conformant so much as *unclaimed*."
- **Everything else is already a labelled rectangle.** That document's scope
  table (lines 60-89) walks all 35 registry entries and finds `MuxRenderer`'s
  trapezoid to be the only non-rectangular non-gate. So "author a print symbol
  for every registered element type" is, concretely, *relabel one trapezoid
  and write 30 rows of prose.*
- **Totality over drawing is already enforced twice.**
  `/home/user/JLS/test/jls/edit/PaletteContractTest.java:48-66` asserts set
  *equality* between `Palette.entries()` and `ElementRegistry.all()` minus a
  named exemption set, and
  `/home/user/JLS/test/jls/ElementDrawSmokeTest.java:123-159` sweeps every
  drawable class reflectively through both export canvases and fails when the
  fixture falls behind. AC-1's "zero missing-print-symbol warnings" buys a
  third copy of a guarantee the tree has, and buys it with a runtime
  diagnostic whose only correct value is zero.

## The seam is cut on the wrong axis

#725 inherits from #540 the idea that "print symbol" is a kind of symbol.
Ask what differs between a printed AND gate and an on-screen one and the
answer is entirely *colour, line weight and chrome* — grid, selection fill,
watch fill, value overlays. Those are `Theme` roles
(`/home/user/JLS/src/jls/Theme.java:47-83`), and `Theme.apply()` already
rewrites the statics every renderer reads. The shape does not change at all.

The tree's real second axis is the one
`docs/standards-adoption/01-iec-ieee-symbols.md:113-116` designs:
`jls.core.SymbolSet { DISTINCTIVE, RECTANGULAR }`, a global render mode with
a `-symbols` flag, one conformance matrix, and `GateOutline.Label` as the
data carrier. That is a *vocabulary* axis, orthogonal to `Theme`. #725's
print-symbol set is a third axis parallel to neither — and CAP-24 risk 2
("two symbol vocabularies must not fork") is the risk #725 would realize
from inside CAP-24. AC-5's promise that the data will be "usable by #43/#44
without duplication" is the tell: a task authored *before* the design it
promises not to duplicate is the standard way to produce the duplication.

## The redirect: publish the vocabulary, don't re-author it

**File the `SymbolSet` work from `docs/standards-adoption/01-iec-ieee-symbols.md`
as its own issue, and make CAP-24 its first consumer rather than its
predecessor.** The dependency inverts: #725 stops being "author symbols for
the exporter" and becomes "the exporter reads the vocabulary the standards
item owns."

That flip is now affordable in a way that document did not anticipate. Its
own abort test — "**No user has asked and no course is waiting**... This item
is the better *engineering* and the worse *bet* until an instructor asks"
(lines 600-609) — was written when nothing in the tree consumed a symbol
vocabulary. CAP-24, CAP-19 and the accessibility capstone's tactile SVG now
all want the same seam; #505's own cost note says PF-5 "is co-funded by the
IEC/IEEE symbol-conformance item." The demand signal the standards item was
waiting for is the capstone tree itself. Filing it as the owner, once, is
strictly cheaper than #725 authoring a private set and a later item unifying
or forking it.

**And the deliverable becomes an artifact rather than a table.** The thing
worth producing here is `docs/symbol-conformance.md` — one row per element,
outline, qualifying symbol, the clause that specifies it or an explicit
"no clause, JLS-local", and the test that pins it — plus
`SymbolConformanceMatrixTest` parsing it two ways against `ElementRegistry`,
in the exact style
`/home/user/JLS/test/jls/ExtensionPointCatalogTest.java` already uses against
`docs/extension-points.md`. AC-2's "recorded with a reason" is a weaker,
untested echo of that document; AC-5 becomes structurally true instead of
aspirational, because there is only one artifact to be usable.

## A second, out-of-the-box framing: the symbol sheet is a JLS output

CAP-24's whole thesis is that figures should be *exported from the tool*, not
drawn beside it. Apply that to CAP-24's own symbol set. Instead of committing
hand-made goldens (AC-4), make the golden the **reference sheet the tool
generates**: `docs/symbols-reference.svg`, produced by JLS from a palette-sweep
fixture as a scripted refresh, checked in, and diffed in CI. One artifact then
serves four purposes — the totality check (a missing symbol is a visibly empty
cell), the reviewable diff AC-4 wants, the figure in `docs/`, and a one-page
handout an instructor can give students. That last one matters most: JLS is a
pedagogy tool, and a symbol sheet is a *teaching artifact*, not an internal
map. AC-1 as written serves a test; this serves the audience #505 names in its
own "Intended Audience" section.

## AC-4 specifically: text goldens, not visual ones

"Committed visual goldens" collides with a discipline this project states in
three places (`test/jls/ui/package-info.java` layer 3, `CliImageExportTest`'s
javadoc, and the standards doc's golden-file section): no pixel goldens, they
are brittle across JDK font rendering. But the tree already demonstrates the
better answer — `/home/user/JLS/test/resources/orientation-geometry.txt` pins
rects and put coordinates as one line of text per element per orientation, and
it diffs perfectly in review. `GateOutline` is coordinate data; flattening the
whole sweep to `test/resources/symbol-geometry.txt` gives AC-4's "a symbol
change is a reviewable diff" with **no font dependency, no platform
dependency, and no ordering dependency on #707's deterministic-text work**.
The generated SVG sheet above is then documentation, not the test oracle.

## Two totalities are being conflated

"Every registered element type" (35 tags, including `Wire`, `WireEnd`,
`TestGen`, `Cross`, `HLine`) is the wrong universe for a symbol vocabulary and
the right universe for a *renderer* check. Keep them separate: totality over
`ElementRenderers` (equality against `ElementRegistry.all()` minus named
exemptions — the genuinely missing test, since
`/home/user/JLS/src/jls/edit/ElementRenderers.java:53-58` silently falls back
to drawing only a highlight), and vocabulary coverage over the drawable
palette, expressed in the conformance matrix. #725 asks one mechanism to be
both, which is why AC-1 forces an implementer to invent a symbol for `WireEnd`.

## What to keep, verbatim

The falsification discipline #723 carries and #725 depends on — a scratch
element must be shown red before any green run counts — is the best thing in
this lineage and survives every reframing above unchanged. So does AC-3's
instinct that geometry must be data consumed through one mapping; it is right,
and the mapping it should name is `SymbolSet`/`GateOutline`, not a
CAP-24-private table.

## Concrete re-derivation

Replace #725 with: *"Publish `docs/symbol-conformance.md` describing the
symbol vocabulary JLS already draws — customary ANSI/IEEE-91 distinctive
shapes for the gate leaves, labelled rectangles elsewhere, each row citing a
clause or an explicit JLS-local reason — pinned two ways by
`SymbolConformanceMatrixTest` against `ElementRegistry`, with
`test/resources/symbol-geometry.txt` as the coordinate golden and a
tool-generated `docs/symbols-reference.svg` as the published sheet. Make no
standards-conformance claim until the primary documents are read; the
`SymbolSet` render mode and any IEC theme are filed under the standards
lineage, with CAP-24 as consumer."* That is roughly a day of honest work
instead of 1-1.5 mw of re-authoring, it removes the `PF5 --> PF1` edge from
#505's critical path, and it leaves CAP-24 free to start on the only part of
it that can actually fail — PF-1's cross-platform text metrics.
