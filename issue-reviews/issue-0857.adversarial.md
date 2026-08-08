# Issue #857: TASK-C580-3: the winget submission-and-review cycle gets its cost written down, in the same ledger every channel reports to
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Third of three TASK-C580 tasks under FEAT-C34-2 (#580, winget), itself a
planned feature of CAP-34 (#518). #857 creates a per-channel maintenance
cost ledger (release/channel/task/time-spent/notes), seeds it with one
real winget release cycle's cost, states KC-34-1's 0.5 mw threshold in
the ledger header, and asserts the format is meant to be reused by the
Flathub (#579) and Homebrew (#581) sibling tasks.

## Findings, most severe first

**1. AC-3's coordination claim is contradicted by the two issues it names.**
AC-3: "The ledger format is documented so the Flathub (#579) and Homebrew
(#581) tasks record into the same file rather than inventing their own."
I fetched both. #579's AC-5: "The per-release review/update cost is
recorded; if it exceeds 0.5 mw per cycle, KC-34-1 applies..." — no
mention of #857, a shared file, or a required format. #581's AC-5: "The
per-release cask update-and-review cost is recorded against KC-34-1's
0.5 mw threshold." — same silence. Neither issue lists `#857` in
`ordering_after` (both are `ordering_after: []`), so both are free to
close, ledger and all, before #857 exists — which is exactly the
"inventing their own" outcome AC-3 claims to prevent. AC-3 is a
one-directional promise: #857 declares itself authoritative, but nothing
in the two consuming issues acknowledges or depends on that authority.
**Recommendation:** add `#857` to `ordering_after` on #579 and #581 (or
retrofit it after the fact), and replace "documented" with a
machine-checked conformance test — this repo already has the pattern
(`SaveTagsTest`, `FileFormatSpecTest`, `ExtensionPointCatalogTest` per
ARCHITECTURE.md) for exactly this "one canonical registry, checked from
both directions" problem; a plain doc note is not that.

**2. The claimed task hierarchy has no GitHub-native backing, so nothing
enforces the ordering #857 depends on.** I called `get_sub_issues` on
#580 (the parent feature) — empty — and `get_parent` on #857 — `null`.
The `task_id`/`part_of_feature: 580`/`ordering_after` fields are free-text
YAML convention only, not GitHub sub-issue links. #857's own
`ordering_after` says a cycle "must have happened to be costed" (i.e.
#856/TASK-C580-2's AC-3 end-to-end verification), which is the right
call in principle, but nothing stops an implementer from picking up #857
before #856 lands — at which point AC-2 ("at least one real winget
release cycle is recorded") is literally unsatisfiable, and the task can
only be closed by fabricating or mis-scoping the entry that AC-5 exists
specifically to forbid. **Recommendation:** file the sub-issue links so
GitHub's own dependency graph reflects `ordering_after`, or at minimum
have CI/PR review reject closing #857 without a linked #856 close.

**3. AC-2's "at least one real cycle" is a gameable, thin evidence bar
for what AC-4 asks it to support.** AC-4 requires stating what happens
when a channel exceeds the KC-34-1 threshold; the arithmetic behind that
policy call rests on n=1 winget-pkgs review cycle. `winget-pkgs` review
turnaround is known to vary enormously — automated validation for a
compliant manifest from an established publisher versus multi-day/week
human review for a first-time submission or a flagged manifest. AC-2
does not require the recorded cycle to be a first submission (the
riskiest, most representative case for a brand-new package) versus a
routine version bump, and does not ask whether the one sample is
labeled as such. A single fast cycle satisfies AC-2 to the letter while
masking exactly the tail risk KC-34-1 exists to catch.
**Recommendation:** require AC-2's recorded cycle to be explicitly the
*first* (initial-submission) winget-pkgs review, since that is the one
data point actually informative for a keep/drop decision, and note that
a single sample is not a stable estimate of a recurring per-release cost.

**4. `band_mw: "0.25"` conflates labor cost with wall-clock wait on a
process the maintainer doesn't control.** The outcome text and AC-2 both
require an external, volunteer-run review cycle at `microsoft/winget-pkgs`
to actually complete before this task can close — a dependency #856
(TASK-C580-2) itself doesn't bound either. 0.25 mw plausibly covers the
labor of writing the ledger file and one row into it; it does not
describe the elapsed time before that row can exist, which is on
someone else's calendar. The issue doesn't distinguish "effort" from
"elapsed" anywhere, so the estimate reads as a quarter-maintainer-week
task when the true schedule risk is open-ended. **Recommendation:** state
the band_mw as labor-only and note the external-review wait as an
unbounded schedule dependency, consistent with how #518 itself treats
KC-34-1 as a cost signal rather than a fixed calendar promise.

**5. The ledger schema is underspecified for an artifact three separate
tasks must agree on.** AC-1 lists columns ("release, channel, task, time
spent, notes") but gives no file path, no serialization (CSV vs.
Markdown table vs. YAML), and no unit for "time spent" — presumably `mw`
given that's the unit used everywhere else in this issue family
(`band_mw`, "0.5 mw per cycle"), but AC-1 never says so. Given finding 1
(no enforced reuse), three independently-written tasks each guessing at
units and format is a real risk, not a hypothetical one.
**Recommendation:** AC-1 should pin the file path and state the unit for
"time spent" explicitly (mw), not leave it implied by convention.

**6. Solid, briefly.** AC-5 ("An estimated entry is marked as an
estimate; the ledger never presents a guess as a measurement") is
concrete and falsifiable — a good anti-pattern guard, and one this
corpus doesn't always bother to include. The `ordering_after`
dependency on TASK-C580-2 is directionally correct in intent — costing a
cycle that hasn't happened yet would be incoherent — even though finding
2 shows it isn't actually enforced.

## Bottom line

The ledger itself is a reasonable, buildable artifact, and AC-5 is a
genuinely good guardrail. But the issue's central coordination claim
(AC-3 — that #579 and #581 will record into this same file) is not
backed by either of those issues' own text or dependency graph, the
one-sample evidence bar in AC-2 is too thin for the kill-criterion
judgment AC-4 hangs on it, and the labor-vs-wait-time conflation in
`band_mw` understates the real schedule risk. None of this is
fatal — it's a needs-rework, not a should-not-proceed — but #857 should
not be started as written without first closing the loop with #579 and
#581 on the shared-file expectation.
