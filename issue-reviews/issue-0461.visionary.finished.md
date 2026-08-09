# Issue #461: TASK-0090: a drawn circuit leaves JLS as a self-contained gEDA/Lepton schematic — every symbol embedded, so the file references nothing on disk
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the record grammar away and the claim is: *a drawing made in JLS should be able to
leave JLS as a thing another EDA tool can open, edit and act on.* That is a good claim and
it belongs in the project's arc — `docs/capability-roadmap/sweep-06-physical-boundary.md`
argues at length that "being a legitimate front end to somebody else's physical flow" is
not another tool class, and points at `src/jls/hdl/board/PcfEmitter.java` as proof JLS
already does the FPGA analogue.

But the artifact this task actually ships is not that. Read §7.4 and §7.10 literally: each
component's embedded symbol is `B` (one box sized `getWidth()` × `getHeight()`) plus one
`P` per put. An AND gate emits as a rectangle. A `Memory` emits as a rectangle. Every
element in the file emits as a rectangle with pins. Meanwhile §4 H4 names the *failure*
mode of KiCad's importer as "rectangular fallback symbols with correct nets" — which is
byte-for-byte the same outcome as success. The task cannot distinguish its win condition
from its documented degradation, and its Research Question's phrase "correct symbol
geometry" is true only in the sense that the bounding box is the right size.

That is the crux. Everything below follows from it.

## The seam this task should have cut along, and it already half-exists

`src/jls/elem/GateOutline.java` is a headless, AWT-free symbol-art description —
`Kind{LINE, ARC, CUBIC, ELLIPSE}`, coordinate arrays, subpath connect/close flags — living
in `jls.elem`, which is inside `ArchitectureRulesTest`'s headless package list. It was
created by #77's model/render split precisely so gate artwork stops being AWT code:
`src/jls/edit/GateRenderer.java` rebuilds a `GeneralPath` from `gate.outline()` on every
draw, and `src/jls/edit/CircuitRenderer.java:314-323` pushes that same artwork through
JFreeSVG to produce a deterministic vector document (`SvgExportTest#exportingTwiceIsByte-
Identical`). JLS therefore already emits *real* symbol geometry, headlessly, byte-
deterministically, today — for gates.

The gEDA symbol vocabulary is `L` (line), `A` (arc), `V` (circle), `H` (path, beziers
included), `B` (box), `T` (text). `GateOutline.Kind` maps onto it almost one-to-one.

So the high-value move is not a new `jls.pcb` package that re-derives boxes from
`getWidth()`/`getHeight()`. It is **generalizing `GateOutline` into an `ElementOutline`
across the element registry**, finishing the #77 split, and making the schematic emitter a
~200-line transcription of that IR into gEDA records. That single change:

- makes "every symbol embedded" *worth* embedding — an AND gate looks like an AND gate in
  lepton-schematic, and the H4 experiment becomes decidable by looking at the screen;
- retires the last renderers that keep drawing logic inside `jls.edit`, which is the
  standing direction recorded on `ElementRenderer` itself;
- pays into `docs/standards-adoption/01-iec-ieee-symbols.md` (IEC/IEEE symbol shapes need
  a data description of artwork, not a `Graphics` call sequence), into #76's ~126
  hardcoded color call sites (an outline IR has no colors), and into sweep-06's
  observation that JLS "already *is* a 2D geometry viewer … with a per-element renderer
  seam" on the road to LEF/DEF/GDS *reading*;
- gives the print path, the SVG path, the Swing path and the PCB path one source of truth
  for what an element looks like, instead of two-and-a-half.

I am explicitly disregarding this issue's §7.4 acceptance criterion that `Sym(e)` be a box
plus pins, and §5 P3's structural test as the definition of success. P3 asserts the
embedded block *exists*; it should assert the embedded block *is the element's artwork*.
A test that a rectangle is present is a test that the emitter ran.

## A third export flag, when the seam for emitters is already catalogued

The CLI at `src/jls/JLSStart.java:759-788` has `-export` (HDL, dispatching on `.v`/`.vhd`/
`.vhdl`) and `-i` (images, dispatching on `.png`/`.jpg`/`.svg`). This task adds `-netlist`
dispatching on `.net`/`.sch`. That is three flags, each a private extension-to-emitter
table, and O3 presents the duplication as "the pattern to mirror".

`docs/extension-points.md` is the project's recorded answer to exactly this: `hdl.exporter`
(`jls.hdl.HdlExtensionPoints.EXPORTER`, contract `HdlEmitter`) is a typed, catalogued seam
with `ExtensionPointCatalogTest` pinning it in both directions, and the catalog's own rule
reads: *"Pending seams are named here first … so nobody invents a parallel mechanism in the
meantime."* #461 never mentions the catalog. Concrete alternative: widen the seam to an
`app.exporter` / `export.emitter` point keyed by output extension, land `-export out.sch`
and `-export out.net` through it, and leave `-i`/`-netlist` as at most aliases. Three
private dispatch tables is the moment to fix this; four is where it calcifies.

## The pin numbering is derived from a `HashSet`

§7.5 numbers pins "sequential `1..N` in `getAllPuts()` order", and §4 H3 calls determinism
"free". `src/jls/elem/LogicElement.java:328-333` returns `new HashSet<Put>(inputs)` plus
outputs, and the Javadoc on the accessor immediately below it says so out loud: *"Needed by
consumers that must pair puts with per-position semantics, such as the HDL exporter (issue
#60); `getAllPuts()` loses the order."* `getInputList()` exists because this exact mistake
was already made and fixed once. P6's two-JVM check would likely catch it; §11's own
warning — "a mis-ordered component record parses, imports, and yields the wrong board" —
is what it would be catching.

The deeper point is not the bug. It is that four separate consumers (HDL export, this
schematic emitter, TASK-0089's netlist, FEAT-041's packing and FEAT-040's pin maps) all
need *the same* stable, ordered, named terminal identity per element, and each is inventing
its own. That primitive — a model-owned port ordinal, on the element, tested for
totality against `ElementRegistry` — is the thing worth building here. It is smaller than
this task and unblocks more of #366 than this task does.

## The sequencing is inverted against its own parent

#366 says the schematic-vs-netlist route decision "**Blocks funding TASK-0089 and
TASK-0090**", and that the embedded-symbol premise "has never been run" and blocks that
decision. #461 was filed with both unresolved, moves the afternoon experiment into its own
§8 checklist, and its `blocked_by` is empty while naming a hard prerequisite (TASK-0007)
that does not exist as an issue. Filing detail ahead of the decision that governs the work
tends to make the decision by default.

And the destination deserves one more look. gEDA `.sch` was frozen ~2007, gEDA/gaf is dead,
lepton-eda is the one live editor, and the KiCad path is a single importer branch nobody
has executed. KiCad's own `.kicad_sch` is S-expression, documented, actively maintained,
and supports embedded `lib_symbols` natively — the very property this task pays a dead
format to get. The issue forecloses that comparison in §10 ("Do **not** silently switch").
Fine as a guard against drift mid-task; wrong as a scoping input. The afternoon experiment
should hand-write *both* ten-line files and open both, and let the route follow the result.

## What the project's own roadmap says the binding constraint is

`sweep-06` opens with the RV32I flagship failing to export at all — `Memory`, `SubCircuit`,
`ShiftRegister` rejected by `HdlExporter` — and concludes the whole physical descent is
gated on "A. Total export coverage + hierarchy". #461's P8 celebrates that *its* path draws
all four rejected types because "the HDL export buckets do not apply". That leaves JLS with
two contradictory answers to "what may leave this program", diverging permanently, with the
easier answer (a picture) shipping first and the load-bearing one (a netlist a synthesizer
accepts) still blocked. Given that a JLS `Memory 1024×8` or a 32-bit `Adder` has no
package, no footprint and no device until #349/FEAT-041 land, the emitted `.sch` reaches
KiCad's *Assign Footprints* dialog with nothing assignable in it. Its honest value today
is "an editable drawing", and `-i out.svg` already ships that.

## What I would keep, unchanged

Projecting the `Circuit` graph rather than `HdlModel` (H2/O5) is right and the reasoning is
correct. Self-containment as a structural invariant, the named `SchematicTransform` with
declared constants, temp-file-and-rename, the `jls.pcb`-joins-the-layering-rule-in-the-same-
commit discipline, and the gated external-oracle idiom are all good and should survive any
re-cut. The exact-rational `k = 100/12` question (OQ2) is a real question and well posed.

## Recommended re-cut

1. An afternoon: hand-write and open **two** ten-line files — gEDA with an embedded symbol,
   and `.kicad_sch` with an inline `lib_symbols` entry. Record both. This answers #366 OQ1
   and OQ2 together and costs the same day.
2. A small task: **stable ordered terminal identity** on `Element`, total over
   `ElementRegistry`, consumed by the HDL exporter first (deleting its private ordering
   workaround) and by every downstream emitter after.
3. A medium task: **`ElementOutline`** — generalize `GateOutline` across the registry,
   move the remaining `jls.edit` renderers onto it, and pin it with the existing SVG golden
   so the refactor is provably byte-neutral.
4. Then this emitter, as a transcription of (3) plus (2) into whichever format (1) selected,
   contributed through a single catalogued exporter seam rather than a third CLI flag.

That ordering ships the same student-visible capability, and leaves behind three pieces the
rest of #366, #349, #365, #62 and the symbol-standards work all need — instead of one
package of rectangles.
