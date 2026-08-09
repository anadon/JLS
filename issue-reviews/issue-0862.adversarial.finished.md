# Issue #862: TASK-C582-2: the collector runs on a schedule and four consecutive runs land in-tree — and a rate-limited run fails loudly instead of writing a zero
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

The second of three sibling tasks under FEAT-C34-4 (#582): #861 builds the
collector script and data file, #862 (this issue) gives it a schedule, a
commit path, and a fail-loud contract, #863 writes the policy prose.
`ordering_after: TASK-C582-1` correctly sequences it after #861.

## Findings, most severe first

**1. The task's own lineage appears to violate the governance rule its
justifying review just stated.** #508 ("Product & direction review, August
2026"), created 2026-08-03T23:27Z, states under "Process findings (act on
these)": *"Planning ratchet: no new tier:feature/tier:task until two
capstones close."* CAP-34 (#518) was filed 2026-08-04T02:48Z — barely
three hours later — citing "maintainer directive 2026-08-04: file
capstones for all noted gaps." FEAT-C34-4 (#582, `tier:feature`) followed
at 06:40Z the same day, and TASK-C582-1/2/3 (#861/**#862**/#863,
`tier:task`) at 15:37Z. None of the 26 capstones #508 enumerates had
closed in that window (CAP-34 itself, and #582's parent chain, are still
open as of today). #862 is a `tier:task` filed downstream of a `tier:feature`
that appears to be exactly the class of filing #508 says should not
happen yet. The capstone's `filed_by` note reads like an attempt at an
exception ("non-code capstone," "lean filing"), but nothing in #508, #518,
or #862 states that exception or reconciles it with the ratchet's plain
wording. Recommend: before implementation starts, get an explicit written
call on whether CAP-34/#582/#862 are exempt from the ratchet (and why),
or hold this task until two capstones actually close — otherwise the
project is filing new task-tier work while telling itself, in writing,
not to.

**2. AC-4's "four consecutive scheduled runs" sets no minimum interval —
gameable as written.** AC-4: "At least four consecutive scheduled runs
are recorded in-tree ... satisfying CAP-34 AC-3." Neither #862 nor #582
nor CAP-34 (#518) states a cadence (daily? weekly?). A literal
implementer can set `cron: "*/5 * * * *"` and accumulate four "consecutive
scheduled runs" inside twenty minutes — satisfying the letter of AC-4
while defeating the outcome paragraph's own stated purpose ("the series
accumulates ... without anyone running anything" as a real adoption
trend). It also directly undercuts AC-2/AC-3's rate-limit concern: an
unauthenticated-feeling, sub-hour cadence is exactly what would turn
occasional rate-limiting into a routine failure mode. Recommend: state
the intended cadence (daily is the obvious choice, matching `ci.yml`'s
nightly cron pattern at `.github/workflows/ci.yml:12-13`) explicitly in
AC-1 or AC-4, so "four consecutive runs" means four days of real signal,
not four button presses.

**3. "Empty response" is bundled with failure without being defined,
and collides with a legitimate zero.** AC-2: "An API failure, rate limit,
or malformed response fails the run visibly and writes nothing." The
outcome text adds: "a row of zeros is a lie that will later be read as a
collapse in adoption." But a brand-new release's assets legitimately
report `download_count: 0` from the GitHub API on day one — a valid,
non-empty, well-formed response whose counts happen to be zero. Nothing
in AC-2 distinguishes "the API returned nothing / an empty body" (a real
failure) from "the API returned valid JSON in which every count is zero"
(a real data point that must NOT be discarded, or the collector can never
record a new release's first day). As worded, an implementer who treats
"all-zero counts" as "empty response" would silently drop legitimate rows
forever for slow-selling releases — the opposite of the honesty AC-2 is
trying to protect. Recommend: replace "empty response" with a precise
failure signature (non-2xx status, unparseable JSON, missing expected
fields) and explicitly say all-zero counts on a well-formed response are
data, not failure.

**4. AC-3's test coverage stops at the script; nothing verifies the
workflow YAML actually wires failure to "no commit."** AC-3: "A test
drives the failure path against a simulated error/rate-limit response and
asserts the file is unchanged." That is testable at the collector-script
level (per #861's AC-5, which already promises "unit coverage... without
hitting the network") but AC-2's real guarantee — that a failed *run*
(the GitHub Actions job, per AC-1) commits nothing — depends on the
workflow's step ordering and exit-code propagation, not the script's
internal logic. A `continue-on-error: true` on the collector step, a
commit step that runs unconditionally (no `if: steps.collect.outcome ==
'success'` guard), or a script that logs an error but exits 0 would all
let AC-3's unit test pass while AC-2's actual promise breaks in
production, and none of #862's ACs would catch it. Recommend: AC-3 (or a
new AC) should also assert, at the workflow level, that a failed collector
step short-circuits the commit step — e.g., an integration check in CI
that stubs a failing collector invocation and asserts `git status` is
clean afterward, not just a script-level unit test.

**5. The "four runs" evidence is claimed by three different tracked
entities, and propagating it is a manual, unenforced step.** CAP-34 AC-3,
#582 AC-3, and #862 AC-4 all point at the identical real-world fact (four
consecutive scheduled runs recorded in-tree). #862 is the task that
actually produces it, but AC-4 also requires "the run links noted on
#582" — a manual comment with no test or ratchet enforcing it happens.
Given #508's own assessment of this project ("bus factor 1," maintainer
capacity already tapering), and that closing #862 requires waiting real
calendar days after the PR merges (see Finding 2) before AC-4 can even be
checked, this is a concrete way for the task to sit "merged but not
actually done" indefinitely, with #582 and CAP-34 never learning it
happened. Recommend: track the four-runs evidence and the cross-link in
one place (e.g., a checklist in #582 that #862's PR description points
at), not as free-text "noted on #582."

**6. AC-1's push-to-branch pattern has no precedent in this repo's
existing workflows.** AC-1: "using only the workflow token" implies the
scheduled job commits directly with the default `GITHUB_TOKEN`. A scan of
every workflow (`ci.yml`, `release.yml`, `codeql.yml`, `mutation.yml`,
`repro-installers.yml`, `scorecard.yml`) turns up two other `contents:
write` grants: `ci.yml`'s `dependency-submission` job (uses the
dependency-graph *API*, no `git push`) and `release.yml`'s installer job
(uploads release *assets*, no `git push` to a branch either). Nothing in
this repository currently has a scheduled job pushing a commit straight
to a branch. That may be entirely fine, but the issue doesn't address how
this interacts with any branch protection on `master` (not visible from
the checked-out tree) or whether the commit should instead go through a
bot-authored PR — a meaningfully different, and more auditable, design
than a direct push. Recommend: state explicitly whether AC-1 means a
direct push or a bot-opened PR, and confirm against the live branch
protection settings before implementation.

## What's solid

- The fail-loud contract (AC-2) paired with a fixture-driven test (AC-3)
  is a good, specific, testable requirement, consistent with #582's own
  AC-5 and the sibling #861/#863 philosophy — "a gap is honest, a zero is
  a lie" is a real, well-chosen design principle for this kind of metric.
- `ordering_after: TASK-C582-1` is correctly reasoned: there is no data
  file to commit to until #861 lands.
- Scope is small (`band_mw: 0.25-0.5`) and confined to a workflow file
  plus a test — no product code, no GUI, no simulation-engine surface.
- No secrets, no new authentication surface: the public releases API plus
  the ambient `GITHUB_TOKEN` is a minimal, low-risk footprint.

## Recommendation

Get an explicit ruling on Finding 1 (ratchet exemption) before work
starts — that is a process question, not an engineering one, and it sits
above everything else here. Independently of that, tighten AC-1
(cadence, push-vs-PR), AC-2 (define "empty," carve out legitimate
all-zero rows), and AC-3 (extend to the workflow's failure-to-no-commit
wiring, not just the script) before an implementer picks this up.
