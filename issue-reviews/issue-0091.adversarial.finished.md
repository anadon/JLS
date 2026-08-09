# Issue #91: Automated UI test harness (P5 residual): retire display-suite retry masking and produce the 20-run zero-flake record
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what the issue actually asks

By the time of this review the issue has been narrowed (2026-08-02 restructuring comment) to a single residual scope: (a) delete `rerunFailingTestsCount=2` from the `display-tests` surefire execution in `pom.xml` (or record an amended decision keeping it), and (b) produce a 20-consecutive-run zero-flake record for the `@Tag("display")` suite under Xvfb. All the line citations I checked (`pom.xml:262`, `:271`, `:293`) match the current checked-out tree exactly (verified against HEAD `5311625`, 2026-08-09), so the technical grounding is accurate as of today, not just at the pinned `29afb26`. That is a genuine strength — most issues rot faster than this.

## Findings, most severe first

### 1. (High) The acceptance bar is gameable via the very silent-skip defect the issue's own sibling issue (#162) documents

Predictions § P-2 states the win condition as: "20 consecutive runs of the display suite on the Linux Xvfb substrate … complete with zero test failures and zero reruns." § Data Collection & Analysis: "20 consecutive runs, zero failures, zero reruns ⇒ H1 holds."

But #162's own Observation 2 (same repo, same commit) documents that the CI build step currently silently downgrades when xvfb is unavailable:
```
if command -v xvfb-run >/dev/null; then
  xvfb-run -a mvn -B verify -Djls.test.headless=false
else
  mvn -B verify
fi
```
`mvn -B verify` alone excludes `excludedGroups=display` (`pom.xml:271`), so on any run where the best-effort `apt-get install … xvfb` line (ci.yml:73) hiccups, the entire display suite is skipped, not run — and reports zero failures and zero reruns. Nothing in #91's Method, Data Collection, or Completion Criteria requires confirming that display tests actually *executed* (non-zero "tests run") on each of the 20 runs — contrast with #162's own P3, which explicitly demands "surefire reports show `DialogConstructionSmokeTest` executed, not skipped." #91 collects "tests run/failures/errors/skipped" (§9) but never states a judgment rule that skipped-everything must fail the count. As written, an implementer (or an unlucky run of flaky apt mirrors) can produce a technically-compliant "20-run zero-flake record" that is actually a 20-run all-skipped record, satisfying P-2 and the Completion Criteria while proving nothing about flakiness.

**Recommendation:** add an explicit judgment clause to §9/§10: a run counts toward the 20 only if the display suite's surefire report shows tests-run > 0 (or cite #162's P3 check directly as a precondition), and make that clause a Completion Criteria checkbox.

### 2. (High) Body's authoritative dependency block contradicts the issue's own latest comment, and neither correction was applied

§ Status & Dependencies states, and explicitly asserts its own authority:
> `blocked_by: []  # P5 is actionable now — the Xvfb lane and the display suite both exist`
> `blocks: []  # 162 SHOULD consume the 20-run record … but that sequencing is advisory, not a hard edge`
> "(All edges dotted = reference/sequencing advice only; the machine block above, with no blocking edges, is authoritative.)"

But the most recent comment (2026-08-08T18:19:24Z, same timestamp as the issue's `updated_at`) reverses this: "The consequence is an ordering edge, and it runs #162 -> #91 … Record `blocked_by: [162]` here." I fetched #162's current body and confirmed it was never actually updated either — #162 still says `blocked_by: []` and its own Completion Criteria still says "`blocked_by` is empty — nothing to land first." So the "correction" exists only as prose in a comment; the machine-readable block that the issue itself tells readers to trust as authoritative is stale and contradicts the thread's own most recent conclusion. A contributor who follows the documented pickup protocol (§8 first bullet: "Re-verify § Observations at pickup") will not catch this, because the check is scoped to § Observations, not § Status & Dependencies. This also collides with §10's escalation rule ("Two consecutive quarantine cycles … escalate to #162: the substrate, not the tests, is the suspect") — the same #162 that the latest comment says must land *first*. The issue currently gives three mutually inconsistent postures toward #162 (parallel-safe, hard blocker, and downstream escalation target) with no reconciliation.

**Recommendation:** before starting work, either edit the body's machine block to `blocked_by: [162]` (and `part_of_feature: 317`, per the same uncommitted correction) or post a comment explicitly reaffirming `blocked_by: []` and retracting the ordering-edge claim. Don't leave both live.

### 3. (Medium) The 20-run methodology explicitly permits exactly the sampling weakness the issue's own Threats to Validity section warns against

§6 Materials & Apparatus: "a repeat-run driver (a trivial shell loop or a temporary CI workflow-dispatch matrix; 20 sequential lane runs) — nothing else is missing." §7.6 accepts "CI run link **or local log digest**" as the record's evidence. Yet §11 Threats to Validity says: "20 sequential runs on one runner class under-sample the loaded-runner nondeterminism the pom comment describes; prefer runs spread across CI's normal scheduling rather than one back-to-back local batch" — and the pom.xml comment itself (L285-292) attributes the flake source specifically to "loaded CI runners." Nothing in the Method or Completion Criteria requires the runs be spread over time/CI scheduling; a local `for i in {1..20}; do xvfb-run -a mvn test …; done` on an idle laptop in twenty minutes satisfies the letter of P-2 while testing none of the load conditions the whole exercise exists to characterize.

**Recommendation:** promote the Threats-to-Validity preference to a hard requirement: the 20 runs must be actual CI executions (not local loops), and ideally not all triggered back-to-back by one dispatch job.

### 4. (Medium) No requirement to distinguish "flaky" from "genuinely broken" before quarantining, which can silently defeat the issue's stated purpose

The Impact section's whole justification for this task is: "A retry-masked suite can silently degrade into 'passes on the third try,' which hides real regressions and erodes trust in the net." But the prescribed response to *any* failure encountered during the 20-run campaign (§8, §10 P-3) is uniform: "quarantine the test per the recorded rule, file the flake issue, restart the counter" — with no step asking whether the failure reproduces deterministically (i.e., is actually a regression, not noise) before it gets excluded from the required set. If a real regression in `SimpleEditor` (an actively-refactored file per #84/#167/#262/#273) surfaces during one of the 20 runs, the letter of this issue's own method quarantines it away — precisely inverting the stated goal of "not hiding real regressions." The issue never asks for a local reproduction/bisection step to classify a failure as flaky vs. real before quarantine.

**Recommendation:** add a step: before quarantining a failure encountered during the 20-run campaign, attempt local reproduction (e.g., re-run the specific test N times outside CI); only quarantine if it does not reproduce deterministically, and if it does reproduce, treat it as a regression bug, not a flake.

### 5. (Low) Feasibility/cost is underestimated given the file's churn rate

The display suite sits on `SimpleEditor`/editor internals that the issue's own § Background documents as under near-continuous multi-agent refactor (#84, #167, #262, #273 all cited as having "ridden through" the suite in recent cycles, each landing within days of the last). A 20-*consecutive*-clean-run bar on a fast-moving target, where any unrelated legitimate PR that touches editor internals can reset the counter (per the quarantine-and-restart rule), may take substantially longer than the issue implies by listing it as a single checklist item alongside a one-line pom.xml edit. This isn't fatal, but the issue gives no time-boxing or check-in cadence, so "stuck restarting the counter" has no defined off-ramp beyond the two-cycles-escalate-to-#162 rule flagged in Finding 2 as itself unreliable right now.

**Recommendation:** state an expected wall-clock window (e.g., "expect 1-3 weeks of master cadence") so a stalled counter is recognizable rather than silently abandoned.

## What's solid

- The evidence citations (commit, file, line) are accurate against the live tree today, not just at the pinned commit — verified independently for `pom.xml:262/271/293` and `ci.yml:69-86`.
- The #91/#162 boundary is well-reasoned and explicitly justified (different bar: zero-reruns vs. at-most-one-failure; different artifact location) — the 2026-08-04 "not a duplicate" comment is a genuinely good piece of scope hygiene, modulo Finding 2's unresolved edge.
- The falsification criteria (H1/H2) and the "quarantine, don't mask" default are the right instinct, undermined only by the gap in Finding 4.
- Scope is narrow and config-only (no production code, no new tests required) — low blast radius if executed as literally specified, aside from the gameability risk above.

## Verdict

**sound-with-concerns.** The core deliverable (remove retry masking, produce an honest 20-run record) is a reasonable, well-evidenced task, but the acceptance criteria as written can be satisfied without proving the thing they claim to prove (Finding 1), the issue's own dependency metadata is self-contradictory as of the most recent comment (Finding 2), and the quarantine procedure can mask exactly the regressions the task exists to protect against (Finding 4). Fix the blocked_by inconsistency and add the "tests actually ran" + "reproduce before quarantining" checks before starting the 20-run clock.
