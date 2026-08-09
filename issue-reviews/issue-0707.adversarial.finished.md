# Issue #707: TASK-C536-1: a print theme and a bundled font make figure rendering deterministic — no screen chrome, no OS font fallback, one Theme seam
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue is

TASK-C536-1 is the substrate task under FEAT-C24-1 (#536, part of capstone
#505/CAP-24): a print variant of `src/jls/Theme.java` plus a bundled,
JLS-owned font so text metrics stop depending on the host's installed
fonts, before #709 (TASK-C536-2, SVG writer) and #711 (TASK-C536-3, PDF
writer) build on top of it. `ordering_after: [TASK-C540-1]` (#723, the
print-symbol registry mechanism). Grounded against `ARCHITECTURE.md`,
`src/jls/Theme.java`, `src/jls/core/TextMetrics.java`,
`src/jls/edit/SwingTextMetrics.java`, `src/jls/edit/CircuitRenderer.java`,
and the full text of #536, #505, #723, #711, plus the prior adversarial
reviews of #536 and #709 already on disk in this repo
(`issue-reviews/issue-0536.adversarial.md`,
`issue-reviews/issue-0709.adversarial.md`), which this review cross-checks
rather than duplicates.

## Findings, most severe first

**1. (High) AC-5 asserts against a test artifact that does not exist in this repository.**
AC-5 reads: "No measurable startup or per-edit cost is added to the
interactive editor (KC-24-4), asserted against **the existing startup
ratchet**." I grepped all of `test/` for any startup/per-edit performance
harness (`nanoTime`, `currentTimeMillis`, `Duration`, `benchmark`,
`threshold`, and by name) and found none — no `StartupRatchetTest` or
equivalent. The parent feature's own adversarial review already flagged
the same criterion (verbatim from KC-24-4) as unfalsifiable at the feature
level (`issue-reviews/issue-0536.adversarial.md` finding 9: "I found no
startup or per-edit benchmark harness anywhere under `test/` to run this
against"). #707 makes it worse by wording the AC as if such a ratchet
**already exists** ("the existing startup ratchet") rather than
acknowledging it must be built. An implementer following the text literally
will go looking for a fixture that isn't there. **Recommendation:** either
name the ratchet as new work with its own budget line (it is not free —
choosing a measurement methodology for Swing startup/edit latency is itself
nontrivial), or point at a concrete existing test this task is meant to
extend.

**2. (High) AC-1 conflates the color seam (`Theme.java`) with a symbol vocabulary that lives elsewhere, inheriting an unresolved conflation from #536.**
AC-1: "A print theme exists as a variant of the existing `Theme`
registry-keyed seam; no parallel symbol or colour vocabulary is minted."
`Theme.java` (read in full) is a `record` of ten `Color` fields plus a
two-entry `List<Theme>` lookup (`DEFAULT`, `CLASSIC`) — it has no notion of
element shapes/symbols and no font field. The registry-keyed *symbol*
mapping this AC also worries about forking is TASK-C540-1's (#723)
responsibility, not `Theme.java`'s — #723's own Outcome text describes "a
registry-keyed element-type-to-print-symbol mapping" as a wholly separate
artifact. The #536 adversarial review already caught this exact conflation
at the feature level (finding 10: "AC-4's... reads as one claim but is
really two"); #707 is the task where an implementer would actually act on
the wording, and it repeats the ambiguity rather than resolving it.
Additionally, `Theme.java` has no field to carry a bundled font either, so
"a variant of the existing seam" understates what AC-3 needs: either the
`Theme` record grows a font field (a schema change, unstated) or the
bundled font is wired in through some other, unnamed mechanism.
**Recommendation:** split AC-1 into "extend `Theme.java`'s color/chrome
fields" (this task's actual scope) and "do not fork #723's symbol
registry" (a dependency constraint, not something this task builds), and
state explicitly how/whether `Theme` itself gains a font-carrying field.

**3. (High) AC-2's "no screen chrome" premise targets a code path that doesn't draw chrome today, leaving it unclear what is actually being built or tested.**
AC-2 wants "no screen chrome — no selection marks, no hint strip, no grid,
no watch decoration — asserted on a fixture that has all of them on
screen." I read `CircuitRenderer.java` in full (392 lines, the existing
GUI-side render/print/image-export path behind #154's `-i` flag) and it
contains **no** selection/highlight/watch/grid drawing code at all —
`JLSInfo.Palette` chrome fields (`selectionColor`, `watchColor`,
`gridColor`) are referenced only from `SimpleEditor.java` and per-element
renderers reached from the *interactive* canvas, not from the batch/export
renderer. Per `ARCHITECTURE.md`'s threading model, "batch mode never
leaves the main thread and never touches Swing." So either (a) the export
path already draws no chrome, in which case AC-2's "asserted on a fixture
that has all of them on screen" test is circularly satisfied without new
work, or (b) the intent is for the new print-figure renderer to reuse
`SimpleEditor`'s interactive paint path (the one place chrome is actually
drawn) and suppress it there — which pulls Swing/AWT interactive-canvas
code into what #536/#711 elsewhere describe as a "reachable headlessly"
export pipeline. The issue does not say which, and the two readings differ
substantially in what gets built. **Recommendation:** name the concrete
render entry point AC-2's fixture goes through, and reconcile "no screen
chrome" with the fact that the existing headless export path never had any
to suppress.

**4. (Medium) AC-2's fixture is unnamed and unbudgeted — a smaller version of a gap #536 already has for its own "hazard-demo circuit."**
Unlike #536 (which at least names a fixture, "the hazard-demo circuit,"
independently confirmed by both the #536 and #709 adversarial reviews to
not exist anywhere in the repo), #707's AC-2 gives no name at all — just "a
fixture that has all of them on screen." Authoring a circuit that
simultaneously exercises selection marks, the hint strip, the grid, *and*
watch decoration, then wiring it into a test fixture, is real work with no
line item in the 2-2.5 mw band. **Recommendation:** name the fixture (new
or existing) explicitly, matching whatever #536/#709 eventually settle on
so the two efforts don't duplicate fixture-authoring cost.

**5. (Medium) AC-3's "never consults a host font" is a negative claim over an unbounded call graph, and the codebase already contains a font-measurement seam this task must either replace or explicitly leave alone.**
`src/jls/edit/SwingTextMetrics.java` (read in full) is the existing
`TextMetrics` implementation used for element layout; its `forFont` method
takes a font *name* string and calls `g.getFontMetrics(new Font(name,
style, size))` — i.e., host-font-dependent by construction, and
`src/jls/core/TextMetrics.java`'s interface doc states this **must** keep
returning "the same integer pixel values the backing `FontMetrics` would,"
because element geometry (and therefore saved-file bytes) is pinned to it.
AC-3 needs the *new* figure-render path to never take this route, but
nothing in the issue says whether that means a parallel `TextMetrics`
implementation is introduced (leaving `SwingTextMetrics` untouched for
editor geometry) or whether `SwingTextMetrics` itself is touched — the
latter would risk violating the pinned parity contract
(`TextMetricsParityTest`, cited in `SwingTextMetrics`'s class doc) that
keeps saved-file byte-stability intact. A test that only checks "the print
render path's own classes contain no `Font` construction" would pass while
silently reusing `SwingTextMetrics` under a different name — gameable
without a comment on which classes it is scoped to check.
**Recommendation:** state explicitly that AC-3's implementation is a new,
separate `TextMetrics`/`TextMetrics.Provider` and that
`SwingTextMetrics`/editor geometry are out of scope and unchanged.

**6. (Medium) Font provenance and license are unstated — inherited unresolved from #536.**
Bundling font bytes into the jar (the only way to guarantee "no OS font
fallback") is a new shipped asset the README's provenance chain
(`SHA256SUMS`, `bom.json`, build-provenance attestation) has to account
for, and it needs a license compatible with `GPL-3.0-or-later`. #536's
adversarial review flagged this gap already (finding 8); #707 is the task
that actually introduces the font, and it still names no candidate,
license, or how it enters `bom.json`.

**7. (Low) No CI/multi-OS harness is named for AC-4, and it is unclear whether this is the first task to exercise one.**
AC-4 requires "the same fixture measured on Linux, macOS and Windows CI
produces identical glyph advances." KC-24-1 (cited in #536 and #505) gates
funding on exactly this kind of cross-platform determinism check, but
#707 doesn't say whether it is building that three-OS harness for the
first time or reusing one CAP-24 already has elsewhere in the chain
(`FigureDeterminismTest` is named in #536/#711, not here). Worth stating
explicitly so the cost isn't silently duplicated or silently assumed away.

**8. (Low) The stated purpose — "isolated here so the SVG and PDF writers that follow inherit determinism" — is not itself an acceptance criterion.**
The Outcome text promises a reusable seam for #709/#711, but none of the
five ACs require documenting or exposing that seam's contract (an API,
not just passing tests). A task can satisfy AC-1–5 literally with a
private, ad hoc implementation that #709/#711 then cannot actually build
on without re-deriving the interface — silently pushing integration cost
downstream.

## What holds up

- `ordering_after: [TASK-C540-1]` (#723) is consistent with the
  feature-level graph: #536 (FEAT-C24-1) orders after #540 (FEAT-C24-5,
  #723's parent feature), and #707 correctly narrows that to the specific
  task (the symbol-registry mechanism) it actually needs before starting.
- The architectural instinct — isolate theme + deterministic text metrics
  once, before two downstream writers (#709, #711) each need it — is
  sound and matches `ARCHITECTURE.md`'s existing pattern of shared seams
  (`Theme`, `TextMetrics`) rather than per-consumer duplication.
- The `band_mw: 2-2.5` estimate is at least plausible as a slice of
  #536's own `4-6` total ("text metrics is most of it"), unlike some
  siblings in this chain whose costs were shown to be misattributed.

## Verdict rationale

`needs-rework`: the task's placement in the dependency chain is correct
and its founding idea — one shared `Theme`+font substrate before the SVG
and PDF writers — is architecturally sound. But AC-5 asserts an "existing"
test artifact that does not exist anywhere in the repository (finding 1),
AC-1 repeats a color/symbol-vocabulary conflation the sibling feature
review already caught and that this task, being the one meant to actually
touch `Theme.java`, was positioned to resolve (finding 2), and AC-2's
chrome-suppression premise targets a render path (`CircuitRenderer`) that
demonstrably draws no chrome today, leaving genuinely ambiguous what gets
built or tested (finding 3). Two more criteria (AC-2's fixture, AC-3's
font-seam boundary) are underspecified in ways that let a materially
weaker implementation pass. None of this indicts the underlying
substrate-first idea; it indicts the issue's readiness to be picked up and
built exactly as specified.
