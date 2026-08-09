# Issue #712: TASK-C537-1: a drawn circuit exports as CircuiTikZ source that compiles standalone with no manual edits
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being attacked

TASK-C537-1 is the first of two tasks implementing PF-2 (CircuiTikZ export) of
capstone CAP-24 (#505), under feature #537 (FEAT-C24-2). It is small by design
(`band_mw: 1-1.5`) and explicitly scoped to "the hazard-demo circuit" only,
with the approximation table for non-native elements deferred to its sibling
TASK-C537-2 (#714) — I verified that split is real and #714 does own the
approximation table correctly, so that part of the plan is sound.

## Findings, most severe first

**1. (High) The declared `ordering_after` is narrower than the capstone's own dependency graph, and AC-4 leans on infrastructure that may not exist yet.**
`#712`'s yaml says `ordering_after: [TASK-C536-1]` — i.e. only the *first*
of TASK-C536-1/-2/-3 (#707/#709/#711). But capstone #505's own mermaid graph
states `PF1 --> PF2` (all of PF-1 gates PF-2, not just its first task), and
`FigureDeterminismTest` — which AC-4 asks #712 to "share" ("Generated TikZ
is byte-identical across the three CI platforms (`FigureDeterminismTest`
share, CAP-24 AC-2)") — is introduced by TASK-C536-3 (#711: "Alongside it,
`FigureDeterminismTest` closes the determinism claim"), not by TASK-C536-1
(#707), whose own acceptance criteria cover only theme colors/chrome
suppression and bundled-font text metrics. If #712 is picked up once its
stated single prerequisite (#707) lands, there is no guarantee the test
class it's supposed to extend exists yet. Either the `ordering_after` list
is missing #711, or AC-4's "share" language is wrong.
*Recommendation:* add `TASK-C536-3` (#711) to `ordering_after`, or restate
AC-4 to say #712 introduces its own determinism test to be merged with
#711's later.

**2. (High) AC-2 attributes a "print-geometry" seam to TASK-C536-1 that TASK-C536-1 does not create.**
AC-2 reads: "Element placement, orientation and wire routing in the emitted
source correspond to the drawn geometry through the print-geometry
decisions of TASK-C536-1, not through a second geometry model." I read
TASK-C536-1 (#707) in full: its five acceptance criteria are about a print
`Theme` variant (colors, chrome suppression) and a bundled font for
deterministic text metrics — nothing about element placement, orientation,
or wire-routing geometry. I also read `src/jls/Theme.java`: it is a
colors-only record (`touch`, `highlight`, `nonZero`, `wireOff`, `grid`,
`background`, …) with zero geometry or layout fields. Circuit-element
geometry (position, rotation/flip, wire endpoints) already lives in
`jls.elem`/`Circuit` and predates this whole capstone. So "the print-geometry
decisions of TASK-C536-1" names something that does not exist in the cited
task. This isn't cosmetic: if an implementer takes AC-2 literally, they
either (a) wrongly assume geometry determinism was already solved upstream
and skip verifying it, or (b) have to invent a "print-geometry" concept
inside #712's own 1-1.5 mw budget — which directly contradicts the same
sentence's "not through a second geometry model."
*Recommendation:* either point AC-2 at the actual geometry source (the
existing `Circuit`/`Element` model) and drop the false attribution to
TASK-C536-1, or file the missing geometry-seam decision as its own
prerequisite task before #712 starts.

**3. (Medium) AC-1 has no fidelity check and is gameable as written.**
"Exporting the hazard-demo circuit yields CircuiTikZ source that compiles
standalone... with no manual edits" is satisfied by *any* compiling TikZ,
including a degenerate one — an empty `tikzpicture`, or every element
rendered as an unlabeled generic box, would pass AC-1 literally. Compare
this to the sibling SVG task, TASK-C536-2 (#709), whose AC-1 requires "its
committed golden is checked in" — an actual visual-regression artifact.
AC-2 is presumably meant to carry the fidelity requirement, but it names no
verification mechanism at all (no golden, no named test), unlike AC-1
(implicit "does it compile" check) and AC-4 (named `FigureDeterminismTest`).
As written, there is no test that would fail if the exporter emitted
geometrically wrong-but-compiling TikZ.
*Recommendation:* give AC-2 a concrete check — e.g. a golden `.tex`/rendered
comparison, or an assertion over emitted coordinates against the circuit's
element positions — the same discipline #709 already applies to SVG.

**4. (Medium) "The hazard-demo circuit" does not exist anywhere in the repository, and no task in the visible chain owns creating it.**
A full-tree search (`grep -ri hazard`, excluding `issue-reviews/`) finds no
`.jls` fixture, no test resource, nothing — only prose references to "the
hazard-demo circuit" in #505, #709, #711, #712, and #874, all treating it as
shared, pre-existing infrastructure. TASK-C536-1 (#707), #712's sole
declared prerequisite, doesn't create it either (its ACs are theme/font
only). Whichever task actually authors this fixture first is implicitly
load-bearing for at least four other issues, yet no issue claims that
responsibility explicitly.
*Recommendation:* name the task that creates and commits the hazard-demo
`.jls` fixture, and have #712 (and its siblings) declare an explicit
dependency on it.

**5. (Medium) Byte-identity (AC-4) is achievable for TikZ text, but the issue names no guard against a well-known Java pitfall: locale-dependent number formatting.**
Unlike SVG/PDF (where the capstone rightly calls font-metrics "the known
hard part"), plain TikZ source is just numbers and macro calls, so
byte-identity should be comparatively easy *if* coordinate formatting is
locale-pinned. `String.format`/`Double.toString`-adjacent calls without an
explicit `Locale.ROOT` can silently swap `.`/`,` decimal separators or
grouping depending on JVM default locale — a genuine cross-platform CI risk
distinct from the font problem, and AC-4 as worded gives no criterion that
would catch it before a real CI run does.
*Recommendation:* AC-4 (or its implementation notes) should explicitly
require locale-independent numeric formatting for all emitted coordinates.

**6. (Low) AC-3 ("the exporter is reachable headlessly") names no CLI surface.**
Every existing export mode is documented precisely — `-i`, `-export`,
`-vcd`, `-p`/`-v`/`-r` are all named, flag-tabled, and covered by
`docs/batch-interface.md`'s "documented stability contract" per
ARCHITECTURE.md. AC-3 could be satisfied by an internal Java method called
only from a test, never exposed as a flag a "courseware author" (the
outcome paragraph's named beneficiary) could actually run.
*Recommendation:* name the intended CLI flag (e.g. `-tikz`) and require a
`docs/batch-interface.md`/README update, mirroring how `-i`/`-export`/`-vcd`
are documented.

## What's solid

- AC-5 (LaTeX/circuitikz as CI/dev-time only, no jar dependency) is concrete
  and correctly closes a real licensing/dependency hazard — `circuitikz`
  and `pgf/tikz` are LPPL-licensed LaTeX packages that must not enter the
  shipped, GPLv3 jar's runtime classpath.
- Reusing the existing `Theme` seam rather than minting a parallel one is
  consistent with the project's recorded extension-point discipline
  (ARCHITECTURE.md "Extension points: the typed seam catalog," #223).
- Deferring the approximation table to TASK-C537-2 (#714) is a real,
  verified, sensible split, not scope creep — #714's own acceptance
  criteria correctly own that half of FEAT-C24-2/PF-2.
- The task is appropriately small and single-purpose relative to the
  capstone/feature/task hierarchy the repo uses elsewhere.
