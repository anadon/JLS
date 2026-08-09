# Issue #538: FEAT-C24-3: selected signals over a chosen window become a WaveJSON timing figure — interactively and from a VCD in headless batch
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue actually is

FEAT-C24-3, one of six planned features (PF-1..PF-6) decomposing capstone
CAP-24 (#505, "every figure in a lab handout … exported camera-ready"). It
covers PF-3: WaveJSON export of a signal/window selection, interactively in
the GUI and headlessly from a VCD. I fetched #505, #216 (closed, cited as
the handoff it closes the gap on), #405 (cited as reference-only), #369
(cited for the WaveJSON-as-expectation-input boundary), and #537 (the
sibling FEAT-C24-2 for cross-check). The `feat_id`, `serves_capstones`,
`band_mw` (2-3, matches #505's PF-3 row exactly), and the two boundary notes
all check out against #505's text — this issue is unusually well-grounded on
the paperwork level. The problems are in what it asks an implementer to
build and how it proposes to verify that they built it.

## Findings, most severe first

**1. (High) AC-1's fixture — "the hazard-demo run" — does not exist anywhere
in the tree, and no task in this cluster owns creating it.** `find
/home/user/JLS -iname "*hazard*"` and a grep across `test/fixtures/` return
nothing; the three committed fixtures are `riscv-sum1to10.jls`,
`fork-4.6-shiftregister.jls`, and `headless-canary-gate.jls`. #505 itself
uses "the hazard-demo circuit" as if pre-existing (its §1 Outcome
Statement), and sibling issues #539/#541/#875 in this fleet's own review set
independently flagged the identical gap. AC-1 as written cannot be executed
against a real fixture today. Recommendation: either point AC-1 at
`fork-4.6-shiftregister.jls` (or another committed fixture) with the word
"hazard-demo" dropped, or state explicitly which upstream issue in this
cluster is responsible for landing that `.jls` file and its expected
glitch/hazard behavior before this feature can close.

**2. (High) AC-1 names no test, no golden, and no comparison procedure —
"matching the run" is unfalsifiable as written, and the GUI-testing
infrastructure this AC would need does not exist yet.** Contrast with AC-2
(`VcdToWaveJsonGoldenTest`, checked-in golden SVG) and AC-3
(`FigureDeterminismTest`, byte-identical across platforms): both name a
concrete oracle. AC-1 says only "produces WaveJSON with clock grouping and
bus lanes matching the run" — no test class, no golden file, no definition
of what "matching" means (structural equality of the WaveJSON against a
fixture? visual comparison of a rendered figure? neither is stated).
ARCHITECTURE.md's own test-layout section is explicit that the
infrastructure this would need is not there: *"Layer 1 (present) is
headless model assertions; layers 2 (Swing harness under Xvfb) and 3
(render-to-image) are reserved."* An implementer can satisfy AC-1's letter
with a Layer-1 headless unit test that never drives the actual interactive
signal/window-selection UI, which defeats the criterion's own stated point
(GUI path, interactively). Recommendation: either name the test class and
the comparison it performs (structural WaveJSON equality against a fixture
is realistic even at Layer 1 if the export logic is invoked directly rather
than through mouse events), or state plainly that AC-1 is provisionally
Layer-1-only until #91's Layer 2/3 harness lands, and cite that dependency.

**3. (Medium-High) The GUI and headless paths pull from two data sources
with different retention semantics, and the issue never says they must
share one code path — the exact failure mode #369 explicitly calls out and
guards against for its own CLI/GUI pair.** The GUI's trace data structure,
`Trace` (`src/jls/edit/Trace.java:26-32`), caps scrollback at
`MAX_RETAINED_CHANGES = 100_000` per signal — documented in-code as *"the
display-side bound (distinct from #20's simulation state bounds)"*. The
headless path's data (`BatchSimulator.eventTrace`/`probeTrace`, see #405 O7)
retains the full run with no such cap. So "selected signals over a chosen
window" is, today, two different underlying stores with two different
truncation behaviors: a window that fits in the VCD-derived headless path
can silently truncate or misrepresent in the interactive path once a run
exceeds 100,000 changes on a watched signal. #369 hit this identical shape
of problem for its CLI/GUI pair and made it a named global invariant ("One
runner behind both front ends... The CLI and the panel never compute a
verdict independently") specifically because two independently-written
paths is "the worst possible failure for a grading surface." #538 states no
equivalent invariant for its two WaveJSON emitters, and AC-3's determinism
test only covers the headless VCD path across platforms — it says nothing
about GUI-vs-headless output agreement on the same run. Recommendation: add
an invariant that both paths route through one WaveJSON-emission function
taking a common in-memory representation (e.g., both adapt to the same
`TraceSample`-shaped input), and add a criterion that GUI export and
headless VCD export of the *same* run produce identical WaveJSON.

**4. (Medium) The headless CLI's own interface is unspecified where the
project's other CLI additions are rigorously spec'd.** "A headless
VCD-to-WaveJSON CLI mode converts batch-produced VCDs (signal list + window
arguments)" names no flag, no grammar for the signal list, and no format for
the window arguments. Compare the rigor this project applies elsewhere:
`docs/batch-interface.md` gives the `-t` grammar as a four-production BNF,
`JLSStart.java`'s flag table is "the authoritative flag table" cited in
ARCHITECTURE.md, and the CLI contract (issue #42) fixes exit codes and the
`jls: error: ...` diagnostic format precisely. AC-2's test can still be
written without this ("the CLI over a batch-mode VCD," generic), so an
implementer could invent any interface and pass AC-2 — including one that
doesn't compose cleanly with the existing `-vcd`/`-i`/`-export` flag
namespace, or that violates the exit-code convention on a malformed signal
list. Recommendation: name the flag (or at least commit to the signal-list
and window-argument grammar) before implementation starts, the same way
`docs/batch-interface.md` §2.2 pins `-t`.

**5. (Medium) Byte-identical determinism (AC-3) is asserted for "WaveJSON
output" without saying which of the two producers it covers, or what
JSON-serialization discipline guarantees platform-stable bytes.** JSON
determinism hazards are real and specific in this codebase's own history:
locale-sensitive number formatting and unordered map iteration are exactly
the class of bug the VCD writer's own javadoc calls out avoiding ("no
$date/$version headers... deterministic by construction," #405 O5). Nothing
in #538 states the equivalent discipline for WaveJSON — key order, numeric
formatting for any float-valued fields (delay/period), or which of GUI vs.
headless output AC-3 checks. #874's adversarial review flagged the sibling
ambiguity for SVG ("Criterion 3's SVG is ambiguous between two different
SVG files"); the same shape of gap exists here between GUI-WaveJSON and
CLI-WaveJSON. Recommendation: AC-3 should name the emitter under test
explicitly and state the serialization discipline (e.g., "keys in signal
declaration order, no floating-point fields, or a fixed-precision format").

**6. (Low) `pom.xml` carries no JSON library today** (`grep -i
jackson\|gson\|json` finds only the CycloneDX SBOM path). Not fatal — a
schema as small as WaveJSON's is reasonably hand-serialized in a single
class the way the VCD writer already hand-serializes its own text format —
but the issue doesn't rule out an implementer reaching for a JSON dependency
that would (a) be new supply-chain surface for a single-maintainer project
whose README makes a point of the self-contained-jar model, and (b) need
its own reproducibility argument alongside the font-metrics one #711 already
carries for SVG/PDF. Worth one sentence ruling this in or out explicitly,
the way KC-24-3 already rules out a Node/network dependency for the
*renderer* half of this same feature.

## What holds up

- The `serves_capstones`/`band_mw`/`ordering_after` machine block is
  internally consistent with #505's PF-3 row — unlike some siblings in this
  cluster, no cost or scheduling contradiction was found.
- The WaveJSON-as-test-expectation boundary against #369 is accurately
  quoted and matches #505 §2 verbatim ("recorded in Open Questions with a
  named home").
- Citing #405 as "reference-only upstream" is correct: #405 only changes
  *how* the VCD text is produced/streamed, not its byte profile, so a
  VCD-parsing consumer built against today's format is unaffected by that
  task landing first or later.
- KC-24-3 (WaveDrom renderer pin stays dev/CI-only, rendered-SVG golden
  dropped before jar/CI gains a Node dependency) is carried over from #505
  correctly and is a concrete, checkable escape hatch if the pin proves
  costly — this is the one place the issue pre-commits to a fallback rather
  than leaving a silent gap.
