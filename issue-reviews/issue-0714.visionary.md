# Issue #714: TASK-C537-2: every element CircuiTikZ cannot draw natively has a named row in an in-tree approximation table, and a sample document proves the export in CI
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Two goods are bundled here under one task. The first is **honesty**: a figure exported
from JLS must not silently claim more fidelity than it has, because CAP-24 (#505) exists
precisely to kill "the figure-vs-behavior mismatch every instructor has shipped at least
once." The second is a **ratchet**: the export must break the build, not the user, when it
rots — the same instinct that produced `ElementRegistryTest`'s totality check,
`CliFlagTableTest`, `FileFormatSpecTest`, and `HeadlessCoreRatchetTest`.

Both goods are right and both are squarely on the project's arc. What I do not accept is
the mechanism: a hand-authored prose **approximation table** whose rows are invented
substitute renderings for Memory, StateMachine and TruthTable. That mechanism cuts along
the wrong seam, forks a vocabulary the capstone explicitly says must not fork (#505 risk 3
vs risk 2), and — worst — it is self-undermining. The Outcome says the goal is to avoid
"something that looks like a schematic and is not one," but a hand-drawn TikZ box labeled
`Memory` inside an otherwise-real schematic is exactly that thing, now blessed by a
footnote.

## Reframe 1: TikZ can draw anything. The premise of AC-1 is false.

The issue conflates two claims: *CircuiTikZ ships no `\node[memory]` macro* (true) and
*CircuiTikZ cannot draw a memory* (false — TikZ draws arbitrary paths; that is all it
does). The distinction matters because JLS is already mid-flight on the program that makes
the second claim decisively false:

- `src/jls/elem/GateOutline.java` expresses a gate's drawn symbol as pure coordinate data
  (`LINE`/`ARC`/`CUBIC`/`ELLIPSE` segments in a local pixel frame, no AWT), and its own
  javadoc names this "the pattern later waves continue across the rest of `jls.elem`."
- `src/jls/edit/GateRenderer.java` is the *translator* from that data to `GeneralPath` —
  and nothing more. A `TikzGateEmitter` is the same function with a different sink.
- `src/jls/core/` already owns `Geometry`, `SegmentGeometry`, `Bounds`, `Orientation` and
  `TextMetrics` — the last being the determinism-critical piece that KC-24-1 turns on.
- `Wire`, `Extend`, `State` and the six gate leaves already carry outline/segment data.

So the correct route to PF-2 is not "map elements onto circuitikz macros, invent
substitutes for the rest." It is: **emit TikZ from the same headless geometry the print
theme (PF-1/TASK-C536-1) renders**, and treat CircuiTikZ's native macros as an *opt-in
prettification* for the handful of shapes where an author benefits from document-coherent
styling (gates, wires, ground, tri-state). Under that framing a Memory is drawn exactly as
the print theme draws it — as real `\draw` commands, in-document, editable, deterministic —
and there is no approximation at all in the *shape* dimension.

This is not a detour from the sibling task; it is TASK-C537-1's own AC-2 taken seriously
("element placement, orientation and wire routing … through the print-geometry decisions of
TASK-C536-1, not through a second geometry model"). #714 as written reintroduces the second
model by the back door: an invented substitute rendering is, definitionally, geometry that
exists nowhere else.

What survives after this reframe is a genuinely short list of **semantic** residuals — a
Memory's initial contents are not shown, an FSM's transition guards are truncated, a
TruthTable's rows past N are elided, a SubCircuit is drawn as its boundary and not expanded.
Those are worth naming. They are perhaps five to eight rows, not "every registered element
type not rendered natively."

## Reframe 2: the table must be generated code, and it must not be TikZ-specific

AC-2 wants the table registry-keyed so a new element fails the build. If the table is
markdown, that requires a bespoke drift test — a third artifact to maintain, in the
`FileFormatSpecTest` idiom. JLS already knows the better move: `JLSStart`'s flag table is
the single authoritative source and `usage()` is *generated* from it, with `CliFlagTableTest`
existing to fail if anyone reverts to a hand-maintained list on either side. Do the same
here. The table is a sealed type per `ElementType`:

```
sealed interface Rendering { Native(macro) | Vector(symbolKey) | Residual(what, why) | Refused(reason) }
```

Totality then falls out of the type system plus one `ElementRegistry.all()` sweep; the
published markdown is a build product, not a document. AC-2 becomes free rather than bespoke.

Then go one seam wider. JLS does not have one exporter facing this question — it has four
and a fifth coming:

| Target | Where "can this element be expressed?" lives today |
|---|---|
| Verilog | an `instanceof` chain in `HdlExporter` (lines ~489–712), not registry-keyed |
| VHDL | same walk, shared model |
| print SVG/PDF | PF-5's proposed print-symbol totality ratchet |
| CircuiTikZ | this issue |
| browser export (CAP-19) | unfiled; #505 risk 2 warns it will fork a third vocabulary |

Five per-target tables is five decay surfaces. **One element × target support matrix**, one
totality test, one generated `docs/element-support-matrix.md`, is one. It subsumes PF-5's
ratchet outright, retro-fixes the HDL exporter's un-keyed `instanceof` chain (which today
decays exactly the way #78 was filed to stop), and makes CAP-19 cheap instead of dangerous.
That is the artifact I would fund; #714's table is one column of it.

## Reframe 3: honesty belongs in the artifact and the exit code, not in a doc

JLS already has a house answer for "the target cannot express this element," and it is not a
prose table. It is `HdlExportException` naming every offender in one message
(`HdlPolicyTest#rejectionListsEveryOffenderInOneMessage`, `#memoryIsRejectedByName`), plus
`HdlExporter.Result.warnings()` for the skip-with-a-warning cases
(`#displayIsSkippedWithAWarning`). A courseware author who exports a circuit with an elided
FSM guard should learn it from stderr and from the bundle manifest at export time — not from
a markdown file in a repo they do not have checked out. A doc row that no user reads is
honesty theater; a warning line and a residual note emitted *into the `.tex` as a comment
and into the bundle manifest* is honesty.

And for anything with no faithful rendering at all, the boldest and simplest option the
issue never considers: **refuse, and point at PF-1**. A vector PDF renders a Memory
perfectly. "CircuiTikZ export is defined over the natively-expressible and
vector-translatable set; circuits containing X export as print PDF instead" is a smaller,
more defensible product than a menagerie of invented boxes.

## Acceptance criteria I am disregarding, and why

- **AC-1 (a row for every non-native element) — disregarded.** Its cardinality is an artifact
  of the false premise in Reframe 1. Under geometry reuse the row set collapses to semantic
  residuals; under Reframe 2 the rows are generated, so "lands in-tree" is the wrong verb.
- **AC-2 (registry-keyed, new element fails the build) — kept, widened.** Keyed on
  `ElementRegistry.all()` × target, not on TikZ alone.
- **AC-3 (sample document builds in CI) — kept and strengthened.** Make the fixture the
  **palette-sweep circuit** #505 §1 step 5 already requires (one instance of every registered
  type; `AllElementsRoundTripTest` and `FileFormatSpecTest` both already build such a fixture,
  and note the "hazard-demo circuit" #537/#712 name does not exist under `examples/`). One
  document over that fixture proves compile-cleanliness, exercises every matrix cell, and
  makes the ratchet observable in the same run — collapsing four criteria into one test.
- **AC-4 (recorded red run before any pass counts) — kept unconditionally.** This is the best
  line in the issue and the project's strongest habit; it should apply to the matrix sweep.

## Trajectory

Worth stating plainly even from this lens: #508 puts CAP-24 in *keep-strategic — cheap slice
now, rest gated*, and CAP-24's own KC-24-1 forbids funding PF-2..PF-6 until the demo slice
records byte-identical SVG across two platforms. #714 is PF-2's second half, filed the day
after that review, under a stated ratchet of "no new tier:feature/tier:task until two
capstones close." Nothing here is worth building before the text-metrics determinism question
is answered — and if it is answered, most of this issue's cost disappears into the geometry
seam that answering it creates.

## What I would file instead

**Outcome.** One generated element-support matrix, keyed by `ElementRegistry`, with a cell
per export target; every cell is Native, Vector, Residual(named loss) or Refused(reason).
The CircuiTikZ emitter translates the headless print geometry, uses CircuiTikZ macros where
they exist, emits residual notes as `.tex` comments and as export warnings, and refuses by
name what it cannot draw.

**ACs.** (1) The matrix is generated from code; a scratch element with no cell fails the
build, red run recorded first. (2) Palette-sweep export compiles standalone in CI on all
three platforms, byte-identically. (3) Every Residual cell's loss appears in the emitted
`.tex` and in the export warnings, asserted by test. (4) The HDL exporter's expressibility
verdicts are read from the matrix, deleting the parallel `instanceof` policy — proof the seam
is real and not a fourth vocabulary.
