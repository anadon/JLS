# Issue #582: FEAT-C34-4: adoption is answered with release-asset download counts collected on a schedule into a tracked file, and stars stop being the number anyone quotes
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

PF-4 of CAP-34 (#518): a committed script scrapes per-asset download counts
from the public GitHub releases API on a schedule, appends them idempotently
to a tracked data file, and a one-line policy doc names this the adoption
KPI (executing #508's recommendation to retire stars). Five ACs, scoped
explicitly to not depend on the other CAP-34 channels landing first.

## Findings, most severe first

**1. AC-3's "four consecutive scheduled runs" is unverifiable at merge time and the schedule cadence is never stated — the criterion is gameable in both directions.**
Quoted: "AC-3: At least four consecutive scheduled runs are recorded
in-tree (CAP-34 AC-3)." Nothing in the issue says how often "the schedule"
fires. A daily cron satisfies this in 4 days with almost no signal (asset
download counts barely move day-to-day for a project with ~3 GitHub stars
per #508); an hourly cron satisfies it in an afternoon and produces a
column of four nearly-identical rows that "prove" nothing about adoption
trend while technically closing the AC; a monthly cron defers verifiable
completion by 4+ months, which conflicts with the issue's own framing of
this as a lean, standalone 0.5-1 mw slice. Compare the sibling workflow
`.github/workflows/repro-installers.yml`, which is explicit about its cron
("06:17 UTC on the 3rd of each month") and about *why* that cadence was
picked (off the top-of-hour spike, monthly is enough to catch toolchain
drift) — this issue sets no equivalent anchor. Recommendation: pin the
cadence in the issue itself (weekly is the natural fit for a low-traffic
repo) and reword AC-3 to require the runs span a minimum wall-clock window
(e.g. "≥4 weekly runs over ≥3 weeks"), not just a bare count, so an
implementer cannot satisfy the letter by picking an absurd interval.

**2. The ACs never require the new workflow to justify or scope its `contents: write` grant, breaking with this repo's established least-privilege discipline.**
Every existing workflow in `.github/workflows/` sets `permissions: contents:
read` at the top level and elevates only per-job, with an inline comment
naming the reason (`ci.yml:` `# least-privilege token: the build only reads
the repo (issue #68)`; `release.yml:45-48`: `contents: write` commented `#
create the release and upload assets`). A scheduled script that commits
results to a tracked file needs the same elevation, but nothing in #582's
ACs asks for the job-scoped `permissions:` block or the justifying comment
that every other write-capable job in this repo carries. An unattended,
low-scrutiny cron job with repo-write access is a meaningfully different
risk profile from a human-triggered release job — a bug or a compromised
dependency in the download-count script becomes a path to an arbitrary
commit on `master`. Recommendation: add an AC requiring the new job to
declare `permissions: contents: write` scoped to only the commit/push step
(top-level stays `contents: read`), with an inline comment matching the
repo's existing convention, and to state whether it pushes directly to
`master` or opens a PR (relevant if branch protection ever requires
review — SECURITY.md/CONTRIBUTING.md were checked and neither documents
current branch-protection rules, so this is presently unconstrained but
should be decided, not left implicit).

**3. Unbudgeted CI cost: every commit this workflow makes will re-trigger the full CI matrix.**
`ci.yml` triggers `on: push: branches: ["master"]` with no path filter —
confirmed by reading the file; there is no `paths-ignore` anywhere in the
workflow set. A scheduled commit that only appends a row to a data file
will therefore also fire the full Linux build (JDK 25 + 26 legs, SpotBugs,
HDL toolchain install, JaCoCo, Windows/macOS/Wayland lanes per the other
workflows triggered off push) — real compute cost for a one-line data
change, and noise in the Actions history that makes genuine CI failures
harder to spot. This cost is not mentioned anywhere in the issue and is not
folded into the stated 0.5-1 mw band. Recommendation: either have the
workflow skip CI on its own commits (`[skip ci]` in the commit message, or
scope `ci.yml`'s trigger with a `paths-ignore` on the data file), or
explicitly accept the cost in the issue text so it isn't discovered as a
surprise during implementation review.

**4. AC-2's idempotency contract is underspecified for the case that actually matters: a same-day rerun after a partial or failed prior run.**
Quoted: "Results append to a tracked data file in a stable, diffable
format; re-running the script for an already-recorded date is idempotent
rather than duplicating rows." Download counts are monotonically
non-decreasing over time, so "idempotent" has two very different possible
implementations: (a) skip if a row for today's date already exists — this
satisfies the letter of AC-2 but permanently freezes whatever data a
first, possibly-incomplete run captured, even if a corrected rerun happens
minutes later; or (b) overwrite the existing row for today's key — this is
the behaviorally correct choice but is arguably not "idempotent" in the
strict sense the AC's wording suggests, and a literal-minded implementation
could pick (a) and still pass every stated check. The issue never states
the row key (date alone? date+release+asset?) or which of these two
behaviors is required. Recommendation: specify the key explicitly (e.g.
`(collection_date, release_tag, asset_name)`) and state that reruns for an
existing key overwrite rather than skip, then rename the criterion
"no duplicate rows per key" to avoid the ambiguous "idempotent" framing.

**5. Minor self-contradictory phrasing in AC-1 could send an implementer down an unnecessary secret-management path.**
Quoted: "A committed script collects per-release, per-asset download counts
from the public API with no authentication beyond the workflow token."
"No authentication ... beyond the workflow token" reads as "no auth, except
auth." GitHub's public releases endpoint already returns `download_count`
unauthenticated for a public repo, so no token is strictly required even
in CI — `GITHUB_TOKEN` is only useful there as a rate-limit bump (60/hr
unauthenticated vs. 5000/hr authenticated), and AC-1 separately requires
the script be "runnable locally by a contributor," which should mean zero
auth, full stop. As worded, an implementer could reasonably conclude a PAT
must be minted and stored as a secret. Recommendation: reword to "no
authentication required; the workflow token, where present, only raises
the CI rate limit."

**6. The metric's own trustworthiness ceiling is never flagged, which sits in tension with the boundary comment's promise to #590.**
GitHub's `download_count` is known to undercount downloads served through
CDN caching or mirrored/pre-fetched by package managers, and is not
adjusted for bot/CI traffic. That's an inherent property of the chosen
metric, not a defect in this issue's design, but the issue's own boundary
comment (2026-08-04, by anadon) stakes a claim that #582 is "the correct
source" for numbers #590 will quote in public announcements, where #590's
AC-4/AC-5 require every claim to be falsifiable and checked at posting
time. A KPI with an unstated undercounting ceiling, first quoted in a
public "flare moment" post, is exactly the kind of over-claim #590 is
designed to forbid. Recommendation: fold a one-line caveat into the same
policy doc AC-4 already requires ("counts reflect GitHub's reported
`download_count`, which may undercount CDN-cached or mirrored downloads").

## What's solid

- The boundary claims against #338 (FEAT-010, installer-matrix hardening)
  and #443 (TASK-0027, arming that matrix) hold up under direct reading of
  both issues: they govern how installer artifacts are built and gated,
  never how downloads are counted. No overlap found, contradicting nothing.
- AC-5 ("an API failure or rate-limit fails the scheduled run visibly
  rather than appending an empty or zeroed row") is a well-specified,
  hard-to-game criterion that directly forecloses the most tempting way to
  fake a healthy-looking series, and is easy to test in isolation
  (mock the API call to error, assert no row is appended and the job
  exits non-zero).
- The "does not depend on FEAT-C34-1/2/3" scoping is honest and
  consistent with #518's own text; there's no double-count between this
  issue and CAP-34's overall AC-3 (which this issue's AC-3 is explicitly
  meant to discharge on its own).
- The pattern itself — a scheduled cron workflow with a job-scoped write
  grant — is not novel risk territory for this repo: `repro-installers.yml`
  (monthly cron) and `release.yml`'s `contents: write` jobs are direct,
  working precedent, so feasibility of the mechanism is not in question,
  only whether #582's ACs hold implementers to the same discipline those
  precedents demonstrate.
