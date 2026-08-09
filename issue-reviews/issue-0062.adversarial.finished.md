# Issue #62: HDL Stage 2 companion: schematic auto-layout for imported netlists (heuristic layered layout; ELK only out-of-process)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The layout seam and heuristic engine are genuinely landed and match the
issue's evidence citations (verified against current source, not just
the pinned `29afb26`). But the issue itself has become a 13-comment,
multi-thousand-word merge target for at least three other issues
(#342, #290, #388) that keeps re-describing its own scope, carries a
documented process violation (an explicit "do NOT hand-roll a Sugiyama
layouter" directive that was overridden days later with no recorded
justification beyond "supersedes"), and leaves a source-level
contradiction unfixed for a month despite flagging it three separate
times. Several of the acceptance criteria that would gate closure are
either undefined (IC-3's "core scale"), self-graded (IC6), or
unenforced by any code artifact (IC9). Findings below, most severe
first.

## Findings

**1. (Critical) Source-level contradiction flagged three times, never fixed.**
`src/jls/hdl/layout/package-info.java` lines 17–21 state: *"the layout
engine is an out-of-process ELK Layered runner... A hand-rolled
Sugiyama layouter is explicitly out of scope."* But
`HeuristicLayeredLayouter.java` (553 lines, verified present) is a
textbook Sugiyama pipeline (layering → barycenter ordering → grid
coordinates → routing, per its own javadoc lines 21–36) and is called
unconditionally at `NetlistImporter.java:104`. The issue body itself
flags this exact mismatch in the 2026-08-02 REPLAN comment, again in
the #342-absorption comment ("Open Question 3... bookkeeping a cold
maintainer will trip over"), and again in the Pass-2 comment ("not up
for re-argument; only Open Question 3's contradiction... is"). As of
today the source is still unfixed — `package-info.java` was last
touched 2026-08-08 per file listing but the contradictory sentence is
still there verbatim. A three-times-acknowledged, never-applied fix is
a process failure, not an open question. **Recommend:** treat this as
a one-line blocking fix, not a rolling "Open Question."

**2. (High) The engine decision is a documented reversal presented as continuity.**
Comment `#5005445088` (2026-07-17) resolves: *"use the out-of-process
ELK runner now... **Do NOT hand-roll a Sugiyama layouter.**"* One to
three days later, PR #196 shipped exactly that (confirmed: comment
`#5027065529`, 2026-07-20, "a heuristic layouter... landed"). The
issue's own "Decision history" section calls this a "SUPERSEDED
(REPLAN 2026-08-02)" rather than a violated directive, and gives no
evidence of *why* the reversal happened beyond "what actually landed
is the opposite." Adversarially: whoever built #196 either did not
read or chose to ignore the standing 07-17 resolution, and the paper
trail only rationalizes it after the fact. This matters because the
quantified rubric (IC1–IC5) was adjudicated on 2026-07-08/07-17 for
"the proven ELK Layered" algorithm, not for the still-unvalidated
heuristic that now has to clear the same bar (#290 has not run yet).
**Recommend:** the rubric thresholds should be explicitly re-affirmed
(or revised) for the heuristic engine specifically, not inherited
silently from an ELK-authored adjudication.

**3. (High) Scope creep via issue absorption is now unresolvable from the body alone.**
Three later comments graft substantial new scope onto this issue
without ever consolidating it into the body: the #342 absorption
(~1,700 words, adds IC-1 through IC-6, a second cost estimate table,
new invariants, new open questions), the #290/#388 "boundary note"
(`#5181372218`), and the #388 "roster correction" (`#5227463791`,
2026-08-08 — the newest comment on the issue). The result is two
independently-numbered acceptance-criteria schemes (IC1–IC10 in the
body, IC-1–IC-6 in a comment) that the issue admits overlap — e.g. the
absorption comment's own Completion-Criteria addition says "#290's
overlap has an explicit disposition... IC-3 measures at core scale;
resolve as one piece of work, not two" — i.e. it ships with a *known*
unresolved duplication between IC10 and IC-3. A reviewer who reads
only the body (as the template intends) will materially undercount
the acceptance surface. **Recommend:** the body's Completion Criteria
and IC table must be rewritten to a single numbering before this issue
can be treated as reviewable; comment-only scope additions should not
be load-bearing for closure.

**4. (Medium) IC-3's fixture ("core scale") is explicitly undefined, making it ungameable-proof in the wrong direction.**
The migrated Open Question 1 states verbatim: *"What is 'core scale',
concretely, and which core?... Blocks integration of IC-3."* Yet IC-3
("the rubric at core scale, recorded") is listed as a required
criterion in the migrated Completion Criteria. An acceptance criterion
whose input fixture is unnamed can be satisfied by whoever runs it
picking a convenient "core" after the fact — the opposite of the
issue's stated goal of "CI-regressable quality thresholds instead of
aesthetic judgment calls." **Recommend:** name the specific core (or
formally descope IC-3 from #62's closure) before any close-out
attempt.

**5. (Medium) IC6 is self-graded and structurally unauditable.**
"Human trace trial: three named signals traced end-to-end in < 1 min
per showcase circuit | pass/fail, recorded in PR (screenshot review)."
No named independent reviewer, no rubric for "traced," and the
artifact is a screenshot in a PR description — nothing CI can check.
The codebase otherwise has a strong headless/ratchet-test culture
(`HeadlessCoreRatchetTest`, `NotificationRatchetTest`, etc. — see
ARCHITECTURE.md's Error-reporting and Threading sections), making IC6
the one criterion in this issue where "we looked" stands in for a
verifiable gate. This directly contradicts the issue's own "Intended
Audience & Impact" claim of giving contributors "CI-regressable
quality thresholds instead of aesthetic judgment calls."

**6. (Medium) IC9's timing bound has no enforcement artifact.**
Verified: `grep -rn "nanoTime\|currentTimeMillis" src/jls/hdl/layout/`
returns no matches. Unlike IC1–IC5 (public constants in
`LayoutMetrics.java` lines 26–34, confirmed present), IC9's "under 1s"
bound exists only as prose, to be "asserted in the #290 corpus metrics
test" — a test that doesn't exist yet. No hardware/JVM baseline (cold
vs. warm JIT, single run vs. median-of-N) is specified anywhere, so
whoever writes #290's test can trivially pick a lenient or
favorable-hardware measurement and pass. **Recommend:** pin a
`LayoutMetrics`-style public constant for the bound now, alongside a
stated measurement protocol, rather than leaving it to the test
author's discretion.

**7. (Medium) The feedback/back-edge code path this issue's rubric exempts has never run against real data — acknowledged but not gated.**
Verified independently: `NetlistImporter.java:788` hardcodes
`graph.connect(..., false)` — the sole call site always passes
`feedback = false`. The importer also outright rejects sequential
cells (`test/resources/hdl/import/reject_dff.json`,
`reject_adff.json`), so no design with real feedback can reach the
layouter today. The newest comment on the issue (`#5227463791`,
2026-08-08) confirms this independently and states landing #388
before #448/#61 "means its two most interesting code paths ship
unexercised" — but this ordering constraint lives in a comment, not in
`blocked_by`/`requires_tasks`, and isn't reflected in the Completion
Criteria checklist. It is easy to miss at close-out.

**8. (Low) The machine-readable `requires_tasks: [290]` block is stale relative to the prose's "#62 = #290 + #388" framing.**
Comment `#5181372218` states "#62 = #290 + #388" as a load-bearing
equation for what completes this feature, but the body's YAML block
still lists `requires_tasks: [290]` only, and `blocked_by: []`. The
issue's own Definition of Done ("Machine block, roster table, and
mermaid graph agree with reality at close") is itself an admission
this agreement doesn't hold *now*. A DAG-walk claim ("no path returns
here, so the graph is a DAG") made in the body is only as good as the
block it's computed over, and that block is already known-incomplete
per later comments.

**9. Solid, no issue: the GPL/ELK licensing analysis.** The
EPL-2.0-vs-GPLv3 in-process-linking concern is well-reasoned, cited
against real evidence (`docs/hdl-support-research.md`), and
independently corroborated by `ARCHITECTURE.md`'s own "Plugin trust
boundary" recorded-decision section, which names #62/ELK explicitly as
already sitting on the out-of-process boundary. No adversarial finding
here.

**10. Solid, no issue: the landed-evidence citations mostly check out.**
`SchematicLayouter.java`, `HeuristicLayeredLayouter.java` (including
the `GRID = Geometry.SPACING` constant), `LayoutMetrics.java`'s
threshold constants, and the `NetlistImporter.java:104` call site all
match the issue's claims against current source, and the "31 unit
tests" count is accurate (verified: 6+6+8+7+4 = 31 `@Test` methods
across the five test classes).

## Recommendation

Do not close this issue on the current body/comment stack as-is.
Before further work: (a) fix the package-info.java contradiction
(finding 1) — it is a five-minute source edit, not a planning
question; (b) consolidate the IC1–IC10 / IC-1–IC-6 numbering into one
scheme in the body itself, since comment-only scope is not reliably
reviewable; (c) name the IC-3 core fixture or formally drop it from
this issue's closure; (d) turn IC9 into an enforceable constant before
#290 is written, not after.
