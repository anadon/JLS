# Issue #546: FEAT-C26-4: one export emits what disability services can emboss — a part-to-whole prose narrative and a tactile-lint-clean SVG sized for swell paper
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and the goal is one sentence: *a blind student
can study a JLS circuit away from the machine, and their disability-services
office can produce the tactile half without arguing with the tool.* That goal is
right, it is category-defining (CAP-26 §Impact is not overstating: no schematic
simulator ships this), and it is squarely on JLS's arc — the headless batch
surface is already a documented stability contract
(`docs/batch-interface.md`), export-as-artifact is already how JLS serves
autograders and lab reports, and the whole export family is already
extension-point-shaped (`docs/extension-points.md` line 32: `hdl.exporter`,
cardinality *many*).

What is wrong is not the goal but where the issue cuts. As written it is *one
feature that builds two new pipelines*: a prose generator and a tactile
renderer, each private to the accessible bundle, each validated by its own
checker. Both halves are the third or fourth instance of something JLS already
has, and the seams they should have used are the interesting part of this issue.

## Reframing 1: the narrative is not an exporter — it is the first sink of a description model JLS needs four times

Count the places JLS already turns a circuit into words:

- `Element.showInfo` / `showCurrentValue` — 29 element classes carry per-element
  human text for the status line and probes (ARCHITECTURE.md "Adding an element
  today", item 9).
- `test/jls/ui/CircuitAssert.describe(Element)` (line 231) — the test harness's
  own element-naming vocabulary.
- #355 / #380 (FEAT-011 residual) — accessible names for elements as named
  accessible children.
- #544 (FEAT-C26-3) — spoken element, connection and live-state announcements.
- This issue — a part-to-whole prose narrative.

Five vocabularies, five ways to say "2-input AND gate named `carry`, output wired
to the sum register's D input". #544's boundary note splits itself from #355 by
*channel* (static reporting vs. traversal vs. live speech) and this issue splits
itself from #544 by *delivery* ("the artifact half" vs. "the interactive half").
Both splits are along the wrong axis. The mechanism — turning graph facts into a
sentence — is identical; only the sink differs.

Concrete alternative: file the seam, not the artifact. A headless, AWT-free
`jls.describe` package producing a `CircuitDescription` (element facts,
connection facts, hierarchy) plus a small sentence vocabulary **keyed off the
shipped element registry** — `ElementType` (`src/jls/elem/ElementType.java`)
today carries only `tag`, `aliases`, `elementClass`, `factory`; it needs a
human-name/role field, and that one field serves all five consumers. Sinks:
prose emitter (this issue), speech strings (#544), `AccessibleContext` names
(#355/#380), status line, test harness. Then:

- **Totality is one ratchet, not three.** #542's registry-keyed totality test,
  #544's "every element reports a spoken name", and this issue's narrative
  coverage collapse into one "every `ElementType` has a description" build gate.
- **The i18n constraint stops being a promise.** CAP-26 §3.5 and this issue's
  AC-5 only claim the *format* does not preclude translation. A registry-keyed
  string table makes that structural instead of asserted — and note
  ARCHITECTURE.md's recorded i18n decision names its own revisit trigger (b):
  "the element-registry work (#78) centralizing element metadata to the point
  that string externalization becomes cheap as a side effect." #78 has shipped
  (`ElementRegistry`, `ElementType`). A prose generator is the largest new
  English-string surface JLS would grow; putting those strings anywhere but the
  registry is the version of this feature that ages badly.
- **Determinism is free**, because the model is headless and ordered — AC-4's
  `AccessibleExportDeterminismTest` becomes a property of the seam rather than a
  per-exporter check.

## Reframing 2: the structural walk already exists, in `jls.hdl`

`HdlExporter.buildModel` (`src/jls/hdl/HdlExporter.java`, ~1364 lines) already
does the graph analysis a narrative needs and nobody has flagged it: `WireNet`
partitioning, unioning nets bridged by same-named JumpStart/JumpEnd pairs, and
deterministic net naming chosen **"most-user-visible first"** — input-pin name,
else jump name, else `<register>_q`, else synthesized `net_<id>`. That naming
rule is exactly what prose wants ("the net named *carry-in*", not "net 47").
`HdlModel` is explicitly "a language-neutral structural model … it knows nothing
about Verilog or VHDL" and `HdlEmitter` explicitly demands "same model, same
bytes". A narrative emitter is very nearly a third `HdlEmitter`.

Two things stop it being literally that, and both are informative:

1. `HdlExporter` **rejects** `SubCircuit` and `Memory`, and warns-and-skips
   `Display`, `SigGen`, `Pause`, `Stop`, `Text`, `TestGen`. A narrative must
   describe all of them — a `Text` annotation is often the most important thing
   on a student's schematic.
2. **"Part-to-whole" ordering *is* the subcircuit hierarchy**, and `HdlModel` is
   flat by construction. The issue's central ordering claim depends on exactly
   the structure the existing model discards.

So: either the description model is the superset and `HdlModel` becomes a
projection of it, or the narrative rides `HdlModel` after it grows hierarchy
(which HDL export wants anyway). Either is better than a third walk of
`Circuit`. What must not happen is this feature quietly re-deriving jump
aliasing and net naming inside an accessible exporter — that is a silent
divergence between what the Verilog says and what the narrative says about the
same file, and it will be found by a student, not by CI.

## Reframing 3: the tactile SVG is a units problem, not a lint problem

The dedup comment on this issue already says the tactile SVG should be a
*profile* over the one shipped exporter rather than a fork, and #536 AC-4 says
the print theme "extends the existing `Theme` registry-keyed seam — no parallel
symbol vocabulary is minted". Both are right and neither goes far enough,
because `Theme` is a **color** seam and tactile compliance is **geometry**:

- `CircuitRenderer.export` (line 314ff) builds `SVGGraphics2D(rect.width,
  rect.height)` in editor pixels. There is no physical unit anywhere.
- `WireRenderer.strokeFor` hardcodes `1.0f` / `3.0f` / a `{4,3}` dash — pixels,
  in code, not in a profile.
- Element geometry is integer-quantized on a 12px grid (`Geometry.SPACING = 12`,
  `GridTransform`'s exact integer transforms).

That last fact is the lever the issue never picks up. Because *everything* is
grid-quantized, the BANA separation rules reduce to solving for **one scalar**:
millimetres per grid unit, chosen so one grid step ≥ the guideline's minimum
edge separation. Pick it and the spacing rules hold *by construction* for every
circuit JLS can draw; the lint shrinks to the handful of things that are not on
the grid (text, labels, wire runs that share a grid lane) plus page fit. That is
the reframing that makes most of the problem disappear: **the deliverable is a
device-independent render profile — `{palette, stroke widths in mm, symbol
vocabulary, min separation, page size}` — and "tactile" is a preset of it.**
Screen, print (#536), and tactile become three profiles of one exporter, and the
physical-unit work is *also* what #536 AC-3 needs for a real PDF (points,
camera-ready size) and what #542 needs for thickness/dash/glyph encoding. One
seam funds four features; today it is unowned and specified three times.

## Reframing 4: make the profile data, and Open Question 3 stops blocking

The issue encodes "BANA, cited by edition" *in the lint's rules*, because CAP-26
Open Question 3 is recorded as blocking PF-4's filing. Both the block and the
hardcoding are avoidable. Disability-services offices do not share one output
device — swell/capsule paper, ViewPlus/Tiger embossers at ~20 DPI dot pitch, and
3D print all have different minima, and an office with a working setup has
*numbers*, not a guideline citation. Ship the constraints as a data file (min
line width, min separation, page size, symbol substitutions) with the
BANA-derived values as the shipped default, the edition cited **in the data
file**, and `--tactile-profile <file>` for an office with its own numbers. Then
the guideline choice is a default value rather than a fork in the code, Open
Question 3 stops gating the filing, and a REPLAN that changes guidelines edits
one file instead of a lint.

## Reframing 5 — where I disregard a stated criterion

**AC-4 as applied to the SVG, and the ordering it implies.** Byte-identical SVG
across platforms is #536's *hardest* problem: its AC-2 concedes it needs a
bundled deterministic font path because OS font fallback moves text metrics.
Inheriting that here would sequence the tactile export behind font work nobody
has funded.

But a tactile diagram should carry almost no rendered text: standard
tactile-graphics practice is a **numbered key**, not inline labels — braille
inline on a schematic is unreadable at these densities anyway. Make the tactile
profile **glyph-free by construction** and its determinism is free, no font work,
no cross-platform metric risk. So I would drop the shared-text-metrics
dependency and state the criterion as: the tactile SVG contains no text
elements; the narrative carries the key.

That change also fixes the bundle's real gap. AC-1 only requires that one
command emit both files. Nothing in the issue or in CAP-26 AC-3 requires the two
artifacts to **refer to each other** — yet the entire use is a student with one
hand on the swell paper and a screen reader on the prose. The criterion worth
having is: every element in the tactile SVG carries a key number; the narrative
names the same numbers; a test asserts the bijection. That is what makes it a
bundle rather than two files in a directory, and it is the one thing here that
cannot be recovered later by refactoring.

## Alignment notes

- **Batch-surface stability.** A new output on the documented batch contract
  becomes something JLS must keep stable. The narrative needs a version marker
  the way saves have `FORMAT 1` (`Circuit.readFormatHeader`); neither this issue
  nor #507 says so.
- **CLI shape.** Prefer `-i out.svg` plus a profile selector over minting a
  private renderer entry point; the bundle command then orchestrates two
  existing exporters instead of owning two new ones.
- **Honesty rule.** CAP-26 §3.1 is right and this issue inherits it: a passing
  lint proves guideline compliance, not readability. Ship it as
  "guideline-compliant, machine-verified", and treat the first real embossing by
  a disability-services office as the acceptance event that matters.

## Verdict

**endorse-with-reframing.** The outcome is the strongest thing in CAP-26 and
should be built. Build it as: (1) one registry-keyed description model with the
narrative as its first sink and #544/#355 as the next two; (2) one render
profile with physical units, tactile as a preset alongside screen and print;
(3) a data-file guideline profile so the BANA question stops blocking; (4) a
glyph-free tactile SVG cross-referenced to the narrative by a numbered key. As
written, the issue funds a private prose walker and a private tactile renderer
whose seams three other issues also need — the same feature, one layer lower,
costs about the same and pays for #536, #540, #542 and #544 on the way past.
