# Issue #536: FEAT-C24-1: a schematic exports as print-styled SVG and PDF — print symbols, no screen chrome, byte-identical across platforms
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue is

FEAT-C24-1 (PF-1 of capstone #505/CAP-24) wants a dedicated print `Theme`
(`src/jls/Theme.java`) rendered to camera-ready SVG and PDF, distinct from
the shipped screen-styled export (#154), with byte-identical output across
Linux/macOS/Windows achieved by owning text metrics through a bundled font
with no OS fallback. Grounded against `ARCHITECTURE.md`, `Theme.java`,
`CircuitRenderer.java`, `SvgExportTest.java`, `JLSStart.java`, and the cited
issues #505, #508, #154, #540 (fetched in full).

## Findings, most severe first

**1. (High) The title's "byte-identical across platforms" claim is broader
than anything the stated acceptance criteria actually test — a classic
gameable gap.** The title promises byte-identity for "print-styled SVG and
PDF" together. But AC-2 only binds SVG: *"SVG output is byte-identical
across the three CI platforms."* AC-3 only requires PDF be produced
*directly* by the renderer rather than by conversion — it says nothing
about PDF being byte-identical anywhere, or on any platform. A PDF backend
that embeds a wall-clock `CreationDate`/`ModDate`, a random document ID, or
platform-specific font-hinting metadata (all default behavior in every PDF
writer this reviewer is aware of, and never addressed by AC-3) would satisfy
AC-1–AC-5 exactly as worded while the headline claim — "figure can live in a
course repo under version control and be diffed in review" for *both*
formats — is false for the PDF half. **Recommendation:** add an explicit
PDF byte-identity criterion (or an explicit, named carve-out with rationale,
mirroring how #505's own Kill Criterion KC-24-1 already anticipates a
REPLAN if determinism fails) rather than leaving PDF's determinism
unstated.

**2. (High) Direct PDF rendering has no library, no evaluation, and is
likely underpriced.** `pom.xml` carries zero PDF-related dependencies
(checked in full: xz, org.jfree.svg, flatlaf, jspecify, test-scope only).
The sibling evaluation issue #154 (closed, fetched in full) explicitly
scoped PDF *out*: *"PDF export (OpenPDF) | LGPL/MPL | Adjacent, not a
substitute: OS print-to-PDF already covers PDF; note only."* No PDF backend
has ever been evaluated in this codebase. "Direct, not SVG-to-PDF
conversion" (AC-3) means either hand-writing a `Graphics2D`-shaped PDF
backend (vector paths, font-subset embedding, deterministic object IDs — a
project comparable in scope to what JFreeSVG already does for SVG, i.e. not
small) or pulling in a new, unvetted dependency with its own license
diligence to do. The inherited cost band (`band_mw: 4-6`, "text metrics is
most of it," carried verbatim from #505's PF-1 line) treats the PDF backend
as the *residual* after text metrics are solved, not as comparable work.
Nothing in #536 revisits that allocation now that a concrete "direct, no
conversion" requirement is on the table. **Recommendation:** name a
candidate PDF approach (library or hand-rolled) and re-derive the cost band
before funding, or explicitly invoke AC-3's own escape valve ("a REPLAN on
#505 names the residual") now rather than after the fact.

**3. (High) The declared ordering creates an unacknowledged chicken-and-egg
loop with #540.** `ordering_after: [FEAT-C24-5]` correctly matches #505's
own dependency graph (`PF5 --> PF1`, i.e. #540 before #536) — internally
consistent as filing metadata. But the sibling adversarial review of #540
(`issue-reviews/issue-0540.adversarial.md`, finding 2) establishes that
#540's own AC-1 — *"a palette-sweep circuit... exports with zero
missing-print-symbol warnings"* — requires a print-aware export pipeline
that **is #536's job to build**, and does not exist until #536 lands. So
the graph says #540 must land before #536, while #540's own stated
acceptance criterion cannot be verified until #536's exporter exists.
Neither issue's body names this loop or proposes which one actually goes
first in practice (e.g., a headless registry stub in #540 with rendering
deferred to #536). **Recommendation:** #536 should state explicitly what it
assumes #540 has already delivered at the point #536 starts (a symbol data
type with no renderer yet?), since #540's own text can't be taken at face
value here.

**4. (Medium) Misattributed citation.** The "Boundary and reference notes"
section states: *"#508 prices this feature's 2–3 mw demo slice as the
risk-retiring first step of CAP-24."* I fetched #508 in full: it contains no
"2–3 mw" figure and no demo-slice discussion anywhere — it only places CAP-24
in a "Keep-strategic (cheap slice now, rest gated)" disposition table with no
mw estimate attached. The "Demo slice, 2–3 mw... PF-1 print-styled SVG for
the gate/wire/pin subset... the text-metrics risk (KC-24-1) is retired or
exposed here" language is verbatim from **#505's own Cost section**, not
#508. This is a wrong citation, not a paraphrase — anyone spot-checking #508
for that number will not find it. **Recommendation:** correct the
attribution to #505.

**5. (Medium) The referenced fixture does not exist.** AC-1 says *"Exporting
the hazard-demo circuit produces an SVG and a PDF..."* I grepped the entire
repository (`examples/`, `test/`, `docs/`, `.jls` files anywhere) for
"hazard" and found nothing — no such circuit file exists. #505's own Outcome
walkthrough uses the same name, so this isn't #536-specific, but #536 is the
first issue in the chain that actually needs the file to exist to satisfy
AC-1, and nothing in either issue scopes *creating* it. **Recommendation:**
either name where this fixture will come from (new `examples/` addition, with
its own small cost line) or point AC-1 at an existing circuit the repo
already has.

**6. (Medium) No CLI/invocation surface is specified, and the obvious
candidate is already taken by a different semantic.** `JLSStart.java`'s `-i`
flag (help text at line 765, extension validation at line 1038) accepts only
`.png`/`.jpg`/`.jpeg`/`.svg` and — per #505's own Background section —
produces the *screen*-styled export. #536 carries `area:batch`, implying CLI
reachability, but never states whether the print export is a new flag, an
`-i`-adjacent option, or GUI-menu-only, nor how two different SVG-producing
paths (screen vs. print) are told apart at the command line if both would
naturally want a `.svg` output extension. Nothing in the AC section exercises
`JLSStart` at all — contrast with #505's own AC-2, which is scoped to
outputs, not invocation, but still assumes *some* CLI story exists for CI to
call. **Recommendation:** name the flag/command surface explicitly; this is
implementation-blocking, not a nice-to-have.

**7. (Medium) The determinism mechanism has no stated policy for glyphs
missing from the bundled font.** "Owning text metrics via a bundled
deterministic font path — no OS font fallback in the render" addresses *which*
font supplies metrics, but not what happens when circuit text (a label, a pin
name, a user-entered string) contains a character the bundled font doesn't
cover. AWT's default behavior is silent substitution from an installed
physical font when a glyph is missing — precisely the non-determinism this
criterion exists to kill, and it would defeat the "no OS font fallback"
guarantee for exactly the inputs that trigger it. This isn't hypothetical:
`SvgExportTest.java`'s own class comment (lines 22–24) already documents this
exact failure mode for the *existing* export path: *"text layout coordinates
depend on the JDK's font metrics, which differ across machines... Deliberately
no full-document golden."* AC-2 doesn't say what a missing glyph should do
(reject the export? substitute a `.notdef` box deterministically? require an
ASCII-only content policy for print exports?). **Recommendation:** add an
explicit glyph-coverage policy to AC-2, and a test case exercising a
non-ASCII label.

**8. (Low) Font provenance/license is unstated.** Embedding a physical font's
bytes in the jar (the only way to guarantee "no OS font fallback") is a new
bundled asset the README's provenance chain (SHA256SUMS, `bom.json`,
build-provenance attestation) would need to account for, and it needs a
license compatible with GPLv3-or-later. The issue names no candidate font.
`CONTRIBUTING.md`/README reference `fonts-dejavu-core` only as a *system*
package for headless AWT fallback in CI, not as a jar-bundled asset — so it
cannot be assumed as the intended answer without saying so.

**9. (Low) AC-5 is unfalsifiable as written.** *"No measurable startup or
per-edit cost added to the interactive editor (KC-24-4)"* names no test, no
measurement method, and no threshold — contrast every other AC in this issue,
each of which cites a concrete test class. I found no startup or per-edit
benchmark harness anywhere under `test/` to run this against. As written,
"measurable" is whoever's stopwatch is being used that day.

**10. (Low) AC-4 conflates two different seams under one name.** `Theme.java`
(read in full) is a plain `record` of `Color` fields with a two-entry
`List<Theme>` name lookup — it carries no notion of element shapes or "print
symbols" at all. The registry-keyed *symbol* vocabulary that CAP-24 risk 2
actually worries about forking is #540's (PF-5's) responsibility, not
`Theme.java`'s. AC-4's "the print theme extends the existing `Theme`
registry-keyed seam — no parallel symbol vocabulary is minted" reads as one
claim but is really two: extending `Theme` for colors/chrome is real and
feasible; not forking the symbol vocabulary depends on a registry (#540's)
that doesn't exist yet. Worth separating so an implementer doesn't assume
`Theme.java` alone is where symbol shapes live.

## What holds up

- The `ordering_after: [FEAT-C24-5]` edge matches #505's own mermaid
  dependency graph (`PF5 --> PF1`) exactly — no drift from the parent
  capstone's plan, even though the target itself has problems (finding 3).
- AC-3 honestly frames the direct-vs-conversion PDF choice as "CAP-24 Open
  Question 3 recommended default" with an explicit REPLAN escape valve,
  rather than presenting an open question as already settled — good process
  discipline, consistent with #505's own re-planning protocol.
- The scope boundary against #154 (screen-styled export, shipped) is
  correctly stated and independently confirmed by #505's own Background
  section and the maintainer's boundary comment on this very issue
  (comment 5175923251): different theme, different artifact, no replacement
  claimed.
- The KC-24-1 stop-clause is carried over faithfully, word-for-word, from
  #505 — no silent weakening of the kill criterion at the feature level.

## Verdict rationale

`needs-rework`: the feature's core idea (a print theme through the existing
`Theme` seam, direct PDF, deterministic text) is coherent and consistent with
its parent capstone. But the acceptance criteria let a materially weaker
result pass as complete — PDF determinism is claimed in the title and never
tested (finding 1) — the PDF-direct requirement has no evaluated technical
path and is likely underpriced (finding 2), the stated build order has an
unacknowledged circular dependency with #540 (finding 3), a citation is
factually wrong (finding 4), and two implementation-blocking gaps (missing
fixture, missing CLI surface — findings 5–6) mean a competent implementer
cannot start without first making unstated decisions the issue should have
made. None of this is a rejection of the feature; it is not yet buildable or
verifiable as specified.
