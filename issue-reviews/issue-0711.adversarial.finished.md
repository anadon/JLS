# Issue #711: TASK-C536-3: PDF comes out of the same deterministic renderer, not out of an SVG converter — and both outputs are byte-identical on all three platforms
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being attacked

TASK-C536-3 is the third task of FEAT-C24-1 (#536), under capstone CAP-24
(#505). Its `ordering_after: [TASK-C536-2]` correctly points at #709 (the
actual TASK-C536-2), and #709 in turn correctly follows #707
(TASK-C536-1) — unlike the sibling #712, whose `ordering_after` was found
to skip a real dependency, this issue's declared chain is internally
consistent. The core claim under attack is narrower: whether AC-1–AC-4 as
written can actually verify "PDF from the same renderer, not a converter,"
and whether the cost band is realistic for what that requires.

## Findings, most severe first

**1. (High) AC-2 attributes a PDF byte-identity requirement to "CAP-24 AC-2," but CAP-24's actual AC-2 does not mention PDF.**
#711 AC-2 reads: "`FigureDeterminismTest` (CAP-24 AC-2 share): SVG and PDF
bytes for the fixture corpus are identical across the three CI platforms."
I read #505 (CAP-24) in full. Its AC-2 states: "SVG, TikZ and WaveJSON
outputs are byte-identical across the three CI platforms" — no PDF. The
parent feature #536's own AC-2 also names only SVG ("SVG output is
byte-identical across the three CI platforms"); #536 keeps PDF directness
as a *separate*, softer AC-3 with an explicit REPLAN escape hatch ("or a
REPLAN on #505 names the residual"). #711 is the only issue in this chain
that promotes PDF to a hard byte-identity requirement under the
`FigureDeterminismTest` name, while citing a capstone AC that doesn't
support it. An implementer who goes to CAP-24 to check what
"`FigureDeterminismTest`... share" means will find no PDF clause there —
either #711 is inventing scope beyond what was ever funded/planned, or
CAP-24 §4 AC-2 is stale and should have been amended when this task was
filed. Either way the citation is wrong as written.
*Recommendation:* either strike "and PDF" from AC-2's byte-identity claim
(keep PDF directness under AC-1/AC-3's REPLAN-guarded language only), or
open the REPLAN on #505 that actually adds PDF to CAP-24 AC-2 before
claiming the share.

**2. (High) AC-1's verification mechanism ("a test asserts no SVG-to-PDF conversion step exists in the path") is not something a test can observe, and is gameable either way.**
"No conversion step exists" is a claim about internal call structure, not
externally observable behavior. As stated, this could be "satisfied" by:
(a) a reflection/call-graph assertion that the PDF path's top-level method
never calls the SVG path's top-level method — trivially true even if both
independently serialize the same intermediate `Graphics2D` calls through a
shared-but-renamed helper that is, functionally, a converter; or (b) no
test at all, with the REPLAN escape hatch used preemptively to avoid
writing anything. Compare AC-2, which at least names a test class
(`FigureDeterminismTest`); AC-1 names no class and no concrete check. The
outcome paragraph's real claim — "produced directly by the same renderer
that produces the SVG" — is about code *sharing*, not code *absence*, but
the AC is worded as an absence check, which is the wrong shape of test to
prove the actual requirement (that both writers draw through one shared
paint path, the same relationship `CircuitRenderer`/`SVGGraphics2D`
already has for screen-styled export at
`src/jls/edit/CircuitRenderer.java:311-324`).
*Recommendation:* reword AC-1 to assert the positive claim directly — e.g.
"the PDF and SVG writers both drive `CircuitRenderer`'s existing paint
path through a `Graphics2D` implementation; no code path serializes SVG
text before producing PDF bytes" — and name the test.

**3. (High) The cost band and AC-4's dependency ban are in tension with what "direct PDF renderer" actually requires, and the issue doesn't reconcile them.**
The existing screen-styled SVG export (`CircuitRenderer.exportImage`,
`src/jls/edit/CircuitRenderer.java:314-324`) gets SVG for free by handing
the *existing* paint code a third-party `Graphics2D` implementation:
`org.jfree.svg.SVGGraphics2D` (`pom.xml:70-73`, JFreeSVG, ~50 KB, GPL-
compatible, zero transitive deps — the pom's own comment calls that out).
There is no equivalent PDF `Graphics2D` bridge anywhere in this repo or
its dependency tree (`grep -rn pdf pom.xml` finds nothing). Two paths
exist to close that gap, and the issue picks neither explicitly:
(a) hand-write a PDF-generating `Graphics2D` — object streams, xref
table, deterministic (non-timestamped) `/CreationDate` handling, and font
glyph embedding sufficient to reproduce TASK-C536-1's bundled-font
metrics — which is a real file-format implementation project, not a
1.5–2 mw task; or (b) pull in a PDF-writing library (PDFBox, iText, or
JFreeSVG's own commercial sibling OrsonPDF), which risks violating AC-4
("no external PDF toolchain dependency") depending on how "toolchain" is
read, and for iText/OrsonPDF risks a license that is not obviously
GPLv3-shippable the way JFreeSVG's pom-documented license is. The issue
asserts the outcome ("produced directly... no converter dependency enters
the jar") without picking (a) or (b), and neither the cost band nor AC-4
is precise enough to stop an implementer from quietly choosing (b) with a
library whose license was never checked.
*Recommendation:* name the intended implementation approach (write-from-
scratch vs. named library) in the issue, and if a library, record its
license compatibility with GPL-3.0-or-later explicitly, the way the pom.xml
comments already do for JFreeSVG and FlatLaf.

**4. (Medium) AC-3's kill-criterion citation (KC-24-1) is reused past the point it was meant to gate, and the "before further funding" language is already moot.**
AC-3 says a byte-identity failure "before further funding" triggers a stop
via KC-24-1. But KC-24-1 (#505) is explicitly scoped to "the demo slice" —
"a 2-3 mw demo slice... the text-metrics risk (KC-24-1) is retired or
exposed here" — and its purpose was to gate whether PF-2..PF-6 get funded
*at all*. Those downstream features are no longer hypothetical: PF-2 is
already filed and has open task issues (#537/#712/#714), i.e. funding
already happened without KC-24-1 having been retired by a landed,
CI-verified demo slice (none of #707/#709/#711 has merged). Invoking
KC-24-1 here as if it still gates "further funding" describes a decision
point the rest of the tracker has already passed. This makes the kill
criterion's enforcement look aspirational rather than binding — a real
inconsistency in the surrounding plan, not something #711 caused, but
something it perpetuates by citing the gate as live.
*Recommendation:* either note explicitly that KC-24-1 has already been
bypassed by PF-2's early filing (and decide whether that's acceptable), or
scope AC-3's stop-the-world language down to "re-plan PDF byte-identity
specifically on #505," which is what #711 can actually control.

**5. (Medium) "The hazard-demo circuit" is referenced as a given fixture, but no task in the chain visibly owns creating it.**
A full-tree search (`grep -ri hazard`, excluding `issue-reviews/`) finds no
`.jls` file, test resource, or fixture-builder anywhere in the repository
— only prose in #505, #709, #711, #712, #714, #874 treating it as shared,
pre-existing infrastructure. #711's own AC-1 and AC-2 both key off "the
hazard-demo circuit" / "the fixture corpus." This was already flagged
against #712; it is equally live here, and #711 is one task later in the
chain than #712 checked, so the risk of reaching this task with still no
fixture is not smaller.
*Recommendation:* before #711 starts, confirm #707 or #709 actually
commits the hazard-demo `.jls` file, or add that as an explicit blocking
prerequisite here.

**6. (Low) "The fixture corpus" in AC-2 is a scope expansion beyond AC-1's single named fixture, introduced without definition.**
AC-1 speaks of "the hazard-demo circuit" (singular, named). AC-2 shifts to
"the fixture corpus" (plural, undefined) for the byte-identity test. If
this is meant to include the palette-sweep fixture from #709 AC-4, that
should be stated; as written, "corpus" could be satisfied by testing only
the hazard-demo circuit under a different label, or expanded arbitrarily
by an implementer with no objective bound either way.
*Recommendation:* name the fixture corpus explicitly (e.g. "the
hazard-demo circuit and the palette-sweep fixture from #709").

## What's solid

- The `ordering_after` chain (#711 → #709 → #707) is verified correct,
  unlike the sibling #712's broken chain — a real point in this issue's
  favor.
- AC-4 (no external PDF toolchain dependency in the shipped jar) correctly
  identifies the right hazard in principle, even though finding 3 shows it
  is underspecified against the realistic implementation paths.
- The "three CI platforms" byte-identity claim is grounded in reality:
  `.github/workflows/ci.yml` already runs `ubuntu-latest`,
  `windows-latest`, and `macos-latest` lanes, so the determinism test has
  somewhere real to run.
- Choosing "direct renderer, not SVG-to-PDF conversion" as the default
  (per CAP-24 Open Question 3) is a defensible engineering call: it avoids
  a second source of platform-dependent drift (a converter's own font/
  rasterization behavior) on top of the renderer's.
