# Issue #591: FEAT-C36-4: a paper on the autograding contract and the course experience is actually submitted to a peer-reviewed venue — with the tool-paper fallback recorded in writing if the course data does not arrive
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue asks a solo-maintainer OSS project to write and submit an academic paper to a named peer-reviewed education venue (SIGCSE/WCAE/ASEE-class), with a "tool paper" fallback if a contingent external course pilot (#509) doesn't materialize. The submission-not-acceptance framing (AC-3) and the fallback discipline (AC-5) are sound instincts, but the acceptance criteria leave the actual quality bar for "citable evidence" ungated, hide a multi-issue dependency chain behind a small effort estimate, and never address the human-subjects/FERPA exposure that a real "course experience" section implies.

## Findings, most severe first

**1. AC-1/AC-3 do not gate venue legitimacy — the issue is satisfiable by submitting to a throwaway venue.**
The Outcome section says "A SIGCSE/WCAE/ASEE-class paper," but that constraint lives only in prose. AC-1 ("A tracked note names the target venue, its submission deadline, the page/format limits, and the fallback venue") and AC-3 ("A submission receipt or confirmation is recorded... acceptance is explicitly not an acceptance criterion — submission is") impose no bar on what counts as a qualifying venue. Since acceptance is explicitly excluded and no minimum venue standard (indexed, peer-reviewed, non-predatory, in the stated SIGCSE/WCAE/ASEE class) is written into an AC, the criteria pass identically whether the paper goes to SIGCSE or to a pay-to-publish workshop with no real review. That defeats the issue's own stated Outcome ("JLS becomes citable... something with a DOI-track to point at") while every AC is green.
*Recommendation:* Move the venue-class constraint from prose into AC-1 itself (e.g., "venue must appear in [named list] or be justified in writing against stated criteria"), and require the submission receipt in AC-3 to show the venue's review process (not just a confirmation email).

**2. No human-subjects/FERPA handling for the course-experience section, despite AC-4 demanding a reviewer-runnable reproducibility appendix.**
If #509's WashU CSE 260M pilot materializes, the paper's "course experience" section will draw on real student data (grades, submissions, outcomes). AC-4 requires "whatever the paper claims empirically ships as a committed appendix (fixtures, vectors, commands, expected output) that a reviewer runs without contacting the authors" — i.e., public, in-tree, reproducible artifacts. Nowhere in this issue, #509, or #520 is there an IRB/ethics-approval step, a consent process, or an anonymization requirement for student data. Publishing real student performance data in a public git repo to satisfy AC-4 would be a FERPA violation and a research-ethics failure; quietly aggregating/anonymizing it would violate AC-4's "reviewer runs it, byte for byte" reproducibility bar. The issue has no answer for this tension.
*Recommendation:* Add an explicit AC or boundary note: course data used in the paper must be IRB-approved (or its absence justified), and the reproducibility appendix for any course-experience claim must ship only aggregated/de-identified data with a stated aggregation method — not raw student records.

**3. The effort estimate ("1.5-3 mw") only covers this issue and hides a 3-5+ maintainer-week prerequisite chain that hasn't started.**
`ordering_after` names "#520 PF-2" (issue #589, white paper, 1-2 mw, currently unstarted) and "#520 PF-1" (issue #588, comparison notes, 2-3 mw, currently unstarted) as the source of the paper's technical core and related-work section. Per the Boundary notes, "the white paper is the technical core the submission is built from" — so #591 cannot meaningfully start until #589 and #588 are substantially done. Neither has any recorded progress (both are 0-comment/1-comment tracking issues at the time of review). The real lead time to a submittable draft is #588 + #589 + #591 ≈ 4.5-8 maintainer-weeks, not the 1.5-3 mw this issue's `band_mw` implies to anyone scoping it in isolation.
*Recommendation:* State the transitive critical-path estimate (not just this issue's slice) next to `band_mw`, or explicitly flag in the issue that `band_mw` excludes prerequisite work.

**4. AC-2's "reviewed by someone outside the author" has no evidence bar, unlike its sibling issue.**
Contrast with #589 AC-3 ("Every guarantee names its enforcement... a skeptical reader can verify rather than trust") — that issue requires a checkable artifact for every claim. Here, AC-2 only requires "A complete draft exists and has been reviewed by someone outside the author." No reviewer-identity record, no review notes, no evidence of what changed as a result, no qualification bar (a friend skimming it for five minutes satisfies the letter of the AC as written). For a solo-maintainer project (README: "a single-maintainer signing key," CONTRIBUTING/SECURITY imply one primary maintainer) this criterion is exactly the kind that gets rubber-stamped under time pressure near a submission deadline.
*Recommendation:* Require a recorded reviewer name/affiliation and a dated review artifact (even a short comment thread) attached to the tracking issue, mirroring the enforcement discipline #589 already uses.

**5. Fixed external CFP deadlines are not reconciled with the internal dependency chain.**
SIGCSE, WCAE, and ASEE each run on fixed annual submission windows outside the maintainer's control. AC-1 asks for "a venue and deadline... chosen and written down," but given finding #3's real lead time, there is a concrete risk that by the time #588/#589 land, the chosen venue's deadline has already passed — and the issue's only stated recourse is "the fallback venue if the first deadline slips" (AC-1), which addresses course-data non-materialization (AC-5) but not a missed submission window entirely. There's no re-planning step if both the primary and fallback deadlines are blown by upstream slippage.
*Recommendation:* AC-1 should require the chosen deadline to be checked against the realistic dependency-chain completion date at filing time, not just recorded once.

**6. AC-5's "in time" trigger for the fallback is undefined, making the fallback decision gameable in either direction.**
"If #509's course does not materialize in time, a dated note in this issue states that the paper proceeds as a tool paper" — but "in time" is never pinned to a concrete date (e.g., relative to the AC-1 deadline or a stated drop-dead date). This lets the fallback be declared prematurely (skipping the stated preferred path — "Co-authorship with the #509 pilot is the strongest version and should be pursued first") or postponed indefinitely (never declaring the fallback and stalling AC-3 while waiting on an external course that has no committed timeline of its own — #509 is itself an "adoption target," not a scheduled event).
*Recommendation:* Tie "in time" to a specific date derived from AC-1's chosen deadline minus a stated drafting buffer, e.g., "if #509 data has not arrived by (deadline − N weeks), the fallback triggers automatically."

**7. Minor: label/deliverable mismatch.** Labels are `enhancement, area:batch, area:test, tier:feature` for what the Boundary notes call a "Non-code deliverable." Same pattern already present on sibling #589 — consistent with house style but still likely to misroute a code-focused contributor who filters by `area:batch`/`area:test`.

## What's solid

- AC-3's explicit "acceptance is not an acceptance criterion — submission is" (echoing CAP-36 AC-4) is honest scoping — it doesn't ask the maintainer to control something outside their power.
- The dedup adjudication in the issue's own comment against #589 (own-outcome asymmetry: "A paper submitted on deadline with no standalone instructor-facing document fails #589 while satisfying this issue") is a correct and well-reasoned non-merge call.
- The Boundary notes correctly forbid new benchmarking under this issue and defer measurement to #512/#560, keeping scope from creeping into re-running performance numbers.
- AC-5's intent (record the fallback rather than silently substitute) is the right instinct for research integrity; it just needs the trigger condition tightened (finding #6).
