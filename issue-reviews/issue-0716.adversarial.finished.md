# Issue #716: TASK-C538-1: selected signals over a chosen window export as WaveJSON, with clock grouping and bus lanes that match the run they came from
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue actually is

TASK-C538-1, the GUI half of FEAT-C24-3 (#538), which is PF-3 of capstone
CAP-24 (#505, "every figure in a lab handout … exported camera-ready"). Its
sibling #718 (TASK-C538-2, `ordering_after: [TASK-C538-1]`) is the headless
VCD-to-WaveJSON CLI half. I fetched #505, #538, and #718, and read
`ARCHITECTURE.md`, `src/jls/edit/Trace.java`, `src/jls/elem/Clock.java`,
`test/jls/edit/TraceWindowingTest.java`, and `test/jls/ui/package-info.java`.
The `part_of_feature`/`band_mw`/`ordering_after` block and the AC-4 citation
("CAP-24 AC-2") check out against #505 §4 verbatim — `FigureDeterminismTest`
really is AC-2 there, spanning PF-1+PF-2+PF-3. The problems are in what the
four acceptance criteria ask an implementer to build and verify.

## Findings, most severe first

**1. (High) AC-3's premise — that the chronogram/trace surface already has
selection UI to attach export to — is false; the actual widget has no
selection code at all.** `src/jls/edit/Trace.java` implements
`MouseListener`/`MouseMotionListener`, but `mouseDragged` (line 613) and
`mouseReleased` (line 599) are empty no-op overrides, and `mousePressed`
(lines 513-565) does exactly one thing when clicked in the name area: pop up
a four-item "Move Trace Up/Down/Top/Bottom" reorder menu. There is no hit for
`select`/`Select` anywhere in the file. "Signal selection and window
selection are made in the UI over the chronogram/trace surface" therefore
isn't a UI-wiring task over an existing selection model — it requires
inventing drag-to-select-a-time-range and multi-signal-selection interaction
from zero on a component that currently has neither. The issue frames this as
one bullet among four AC's, disguising it as the smallest item when it is
plausibly the largest undesigned piece of the task. Recommendation: scope
this explicitly as new interaction design (mouse-down/drag/up state machine,
visual selection affordance, keyboard alternative for accessibility) and size
it accordingly, or name the existing mechanism it's supposed to reuse if one
exists elsewhere in the editor that this review missed.

**2. (High) AC-1's fixture — "the hazard-demo run" — does not exist, and
"asserted against a committed golden" names no test class.** `test/fixtures/`
holds exactly three `.jls` files (`riscv-sum1to10.jls`,
`fork-4.6-shiftregister.jls`, `headless-canary-gate.jls`); none is
hazard-related, and no `.jls` or `.vcd` fixture with "hazard" in its name
exists anywhere in the tree (confirmed by grep). #505's own §1 Outcome
Statement uses "the hazard-demo circuit" as if pre-existing, so this gap
originates upstream and both #538's and #718's adversarial reviews flagged it
independently for their own AC's — it is not new here, but #716 inherits it
without resolving it, and unlike AC-4 (which names `FigureDeterminismTest`),
AC-1 names no test class at all, only "a committed golden." An implementer
could satisfy the letter of AC-1 with any ad hoc unit test asserting a
hand-picked golden against a hand-picked small circuit, none of it tied to
"the hazard-demo run" the criterion actually names. Recommendation: point
AC-1 at an existing fixture (e.g. `fork-4.6-shiftregister.jls`) or name which
issue lands the hazard fixture first as a hard dependency, and name the test
class the way AC-4 does.

**3. (Medium-High) AC-1 (exact transition fidelity) and AC-2 (clock-lane
abstraction) can conflict, and the issue states no tiebreaker.** WaveDrom
clock lanes (`p`/`n`/`P`/`N`/`l`/`h` wave characters) encode an idealized,
uniformly-periodic signal, not an arbitrary sequence of timestamped
transitions — that's the whole point of special-casing them instead of N
bit-row samples. `Clock` (`src/jls/elem/Clock.java:76-78`) is
user-configurable per-instance (`cycleTime`, `oneTime`) and, per
`ARCHITECTURE.md`'s threading-model section, runs under interactive
step/pause/animate control, so a real run's clock signal is not guaranteed to
align to period boundaries at an arbitrary selected window's edges (pause
mid-cycle, a window that starts off-phase, or — nothing in the issue rules
this out — a `Clock` reconfigured mid-run). AC-1 requires the exported
WaveJSON's "transitions match the run's transitions in that window" exactly;
AC-2 requires that same signal be emitted as an idealized periodic clock
lane. The issue never states which representation wins when a real run's
clock signal isn't perfectly periodic across the selected window, so a
conforming implementation could pick either interpretation and both AC-1 and
AC-2's goldens would only ever exercise the case where they happen to agree.
Recommendation: state the resolution rule explicitly (e.g., "clock-lane
rendering applies only when the source is a `Clock` element and its output is
periodic for the full window at its configured cycle/one time; otherwise fall
back to a bit lane") and add a golden case that forces the disagreement path.

**4. (Medium) AC-2's "clock signal" and "multi-bit signal" have no stated
classification rule.** `Trace.getElement()` (`src/jls/edit/Trace.java:151`)
does expose the source `Element`, so `instanceof Clock` is a plausible
detection path for the clock case, and multi-bit traces already exist as a
data shape (`TraceWindowingTest`'s `windowedRepaintMatchesFullRepaintForAMultiBitTrace`)
for the bus case — so this is a real gap, not a fabricated one, but it's
still unstated. Does *any* single-bit signal the user toggles at a fixed
period get clock-lane treatment, or only actual `Clock` elements? The
distinction matters for AC-1's byte-for-byte golden and for AC-4's
determinism claim, and it isn't a small implementation detail — it decides
what the golden file actually contains. Recommendation: state the
classification rule (element type, not waveform-shape heuristic, is the
obviously simpler and more deterministic choice) as part of AC-2.

**5. (Medium) The issue's own vocabulary collides with an existing, different
meaning of "window" in this codebase, inviting a false-positive pass.**
`TraceWindowingTest` and the code it tests (`Trace.java` lines documented at
109-111, 159-160, 177-178, 210-212) implement "windowed trace drawing" — a
paint-clip optimization ("purely a cost optimization, never a rendering
change," per the test's own doc comment) — entirely unrelated to a
user-selected export time range. AC-3's "the exported window boundaries
equal the selected ones exactly" is easy to misread, or to accidentally
satisfy in a unit test, against the *existing* windowing machinery (which
already has an exact-match invariant tested) rather than against a genuinely
new user-selection feature. Recommendation: use a distinct term (e.g.
"export range") in the AC text to avoid the collision, or explicitly
disambiguate from the existing paint-window concept.

**6. (Low) AC-4's "CI platforms" plural is grounded but the byte-identity
target for WaveJSON's serialization discipline is unstated — same gap the
#538 review already flagged for the parent feature, inherited here
unchanged.** The three-platform CI matrix (`ubuntu-latest`, `windows-latest`,
`macos-latest` in `.github/workflows/ci.yml`) is real, so "three CI
platforms" isn't invented. But nothing in #716 states key ordering, numeric
formatting, or line-ending discipline for the WaveJSON text this AC needs to
be byte-identical — and `pom.xml` carries no JSON library today, so an
implementer must also decide whether to hand-serialize (consistent with how
the VCD writer already hand-serializes) or add a dependency, which the issue
doesn't rule on either way. Recommendation: name the serialization discipline
in this issue rather than leaving it solely to #718 or the implementer's
judgment.

## What holds up

- The `part_of_feature`/`band_mw`/`ordering_after` machine block is
  internally consistent with #505 and #718; no scheduling contradiction.
- AC-4's "CAP-24 AC-2" citation is a verbatim, correct match to #505 §4
  (`FigureDeterminismTest` really is AC-2 there) — no drift, unlike some
  siblings in this review fleet.
- The underlying data model is genuinely capable of what AC-2 asks: multi-bit
  traces and source-element access both already exist, so bus/clock
  discrimination is an unspecified rule, not an infeasible one.
- Scoping the GUI half and the headless CLI half (#718) as separate,
  explicitly-ordered tasks is a reasonable decomposition of #538, and #716
  correctly leaves VCD production (#405) untouched by not mentioning it.

## Verdict rationale

Two High findings — a UI selection mechanism the issue treats as a given but
that doesn't exist on the target widget, and a fixture the criterion names
but that isn't in the tree with no owning task — mean an implementer cannot
start AC-1 or AC-3 today without making load-bearing design decisions the
issue should have pinned. The Medium-High clock/transition-fidelity conflict
is a real correctness trap that would let a conforming implementation pass
its own golden while being wrong on any real run whose clock isn't
window-aligned. The parts that hold up (paperwork consistency, correct
upstream citation, a genuinely feasible data model) don't offset these gaps.
