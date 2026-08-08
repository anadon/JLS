# Issue #862: TASK-C582-2: the collector runs on a schedule and four consecutive runs land in-tree — and a rate-limited run fails loudly instead of writing a zero
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the ceremony and #862 is asking for one thing the project does not have: **a
measurement that survives time without a human.** #582's collector (#861) can already
answer "how many downloads today"; the GitHub releases API answers that on demand, for
free, forever. What the API cannot do is answer "how many last March" — cumulative
per-asset counters carry no history and no retention. So the series genuinely must be
captured as it happens, and capture without a schedule is capture that stops the first
week someone is busy. That justification is sound and I want to say so plainly before
proposing changes: this is not vanity instrumentation, it is the only way the data can
exist at all.

The failure semantics — a gap is honest, a zero is a lie — are also exactly right, and
are the kind of judgement this repository already makes elsewhere (the README's
insistence on stating precisely what a checksum proves and what an attestation proves,
`SECURITY.md`'s refusal of a signing key that would add rotation risk without adding a
guarantee). #862 belongs to that lineage.

Where I would cut differently is *where the seam goes*, *who enforces the honesty*, and
*what the number is for*.

## Reframing 1: this repository has never let a scheduled job write to the tree — and has three other series it is already losing

Every scheduled measurement JLS runs today reports **out of tree**:

- `scorecard.yml` (weekly cron) → SARIF to the Security tab, results to the OpenSSF API.
- `mutation.yml` (weekly cron, `permissions: contents: read`) → run artifacts.
- `repro-installers.yml` (monthly cron, `contents: read`) → run artifacts.

The only jobs with `contents: write` are `release.yml` (publishing assets) and
`ci.yml`'s `dependency-submission`, which *submits to an API* rather than pushing a
commit. **#862 would be the first workflow in this repository's history to commit to the
tree.** That is a real convention change and deserves to be argued, not assumed by an
AC-1 one-liner.

And the argument is available, because the existing convention is quietly failing:
`docs/mutation-testing-trial-2026-07.md` hand-transcribes mutation scores into prose
tables (39 %, then 40 %, then an 82.98/82.98/83.09 % triplicate run) precisely because
the weekly `mutation.yml` cron throws its history away. The Scorecard badge shows today's
score and forgets last quarter's. This is the *same pathology* #582 names — "the series
accumulates in the repository rather than in someone's memory" — appearing three times
in a repo that has not noticed the pattern.

**Concrete alternative:** don't build a downloads-only cron-and-commit. Build the
**metrics ledger** once — a single append-only directory (`metrics/`), one documented
TSV/CSV schema per series, one scheduled workflow, one commit convention, one failure
contract — and make release-asset downloads its *first* collector. The expensive,
reusable half of #862 is exactly the generic half: the schedule, the commit path, the
provenance convention, the write-nothing-on-failure rule. The metric-specific half (parse
one JSON array) is the cheap part and already lives in #861. Adding
`metrics/scorecard.tsv` and `metrics/mutation.tsv` afterwards then costs a few lines each
instead of a rediscovery of this whole design. Same band (0.25–0.5 mw), strictly larger
arc.

## Reframing 2: the commit target is a load-bearing choice the issue does not make

AC-1 says "commits the updated data file". Committing to `master` has consequences #862
never weighs, and they are checkable in this tree:

- `ci.yml` triggers on every `push` to `master` with no `paths` filter, running ~13 jobs
  including Windows and macOS runners, three GUI boot rigs, the reproducibility gate and
  the installer-reproducibility gate. A daily metrics commit fires that entire matrix
  daily, for a one-line data change.
- `ci.yml`'s concurrency group is `ci-${{ github.ref }}` with `cancel-in-progress: true`.
  A bot push to `master` can **cancel an in-flight master build** — including the
  reproducibility gate — and vice versa. That is a genuine interaction defect that would
  ship silently and be diagnosed months later.
- Bot commits interleave into `git log` and into `git blame`/bisect ranges for
  contributors, in a repo whose history is otherwise entirely human.

Two clean routes out, either acceptable, both better than the unstated default:

1. **`paths-ignore: ['metrics/**']` on `ci.yml`'s push trigger**, plus the commit
   convention of AC-5. Minimal, keeps the data literally in-tree.
2. **An orphan `metrics` branch** written by the workflow, never merged. The data is
   still in this repository (AC-4's "in-tree" is satisfied by any honest reading, and
   CAP-34 AC-3's "recorded in-tree" likewise), CI never fires, `master`'s history stays
   human, and the write permission is scoped to a branch that no build ever consumes. The
   cost is one `git fetch origin metrics` for a reader — trivially documented in #863's
   policy doc.

I mildly prefer (2) for the permission-scoping alone: a workflow that can push to
`master` is a bigger supply-chain surface than one that can push to a leaf branch, and
this project cares about that (the OpenSSF badge is the second line of the README).

## Reframing 3: move the honesty from a test into the data path

AC-2 and AC-3 ask for a *behaviour* — "fails visibly and writes nothing" — plus a test
that drives a simulated 403/rate-limit and asserts the file is unchanged. That is
defensive verification of a property that should be structurally impossible to violate.

The elegant version: the collector **never mutates the tracked file in place**. It
computes the complete new file content in memory from a fully-parsed response set, writes
it to a temp file, and `rename(2)`s over the target as its last act. A parse failure, a
non-200, a short asset list, or a missing `download_count` key aborts before the rename.
Under that design there is no code path that can produce a zeroed or partial row, because
zeroes are never constructed — a row exists only as the transcription of a number that
was actually read. The workflow then reduces to: run collector (`set -euo pipefail`),
`git diff --quiet metrics/ || commit`. No workflow-level failure test is needed, because
the workflow has no failure logic to test.

That pushes AC-2/AC-3 down into #861, where the fixture-based unit harness (its AC-5)
already lives, and leaves #862 as what it should be: schedule, commit path, provenance
convention. I would **explicitly retire AC-3 as written** — a test that exercises a
workflow's failure path is testing YAML, and this repo's own experience with
`wayland-rig-selftest.sh` shows the maintainer already knows the difference between
testing a rig's control flow and testing the thing it drives.

One thing #862 *should* add and does not: **an alert on the gap.** Failing loudly is only
loud if someone hears it. A scheduled workflow that has been red for six weeks is
indistinguishable from one nobody looks at, and the honest-gap policy then produces an
honest but empty series. Either the ledger's reader tolerates gaps explicitly (documented
in #863) or the failure needs a notification path.

## Reframing 4: the highest-value use of this number is not "adoption"

CAP-34 frames downloads as the adoption KPI replacing stars, and #863 will write that
policy. Fine. But the *per-asset* granularity #861 produces enables something more
valuable than an adoption headline, and nothing in the tree currently names it:

JLS ships an unusually wide artifact matrix — deb and rpm and AppImage in amd64 and
arm64, MSI in x86_64 and aarch64, an aarch64 dmg, a jar, a three-arch container, a flake.
CI pays for that matrix on every push and every release. **Per-asset download counts are
the only evidence that could ever tell the maintainer which legs of that matrix are dead
weight** — whether the aarch64 MSI has ever been fetched by a human, whether the rpm
justifies its share of `installer-reproducibility`. That is a decision with real cost
attached, at bus factor 1, and it is exactly the kind of arithmetic KC-34-1 already
demands for channels. I would say so in the data file's companion note: this series
exists to answer *which artifacts to keep building*, and adoption-headline duty is the
secondary use.

Bind a metric to a decision or it becomes decoration. #863 AC-5 asks what would make the
metric wrong; nothing in the tree asks what the metric is allowed to change.

## One factual correction for the sibling issue

#582 AC-4 and #863 treat "are Flathub/winget/Homebrew counters in or out of the number"
as an open policy choice. For two of the three it largely is not: **winget manifests and
Homebrew casks fetch the GitHub release asset directly by URL**, so those installs
already land in the release-asset counter and are not separable from a direct download by
anything the API exposes. Only Flathub rebuilds and hosts its own artifact and therefore
has a genuinely disjoint counter. The policy sentence should say that — "winget and
Homebrew installs are already inside this number and cannot be split out; Flathub's are
not, and are reported separately if at all" — rather than presenting a three-way choice
that reality has already made.

## Verdict

**endorse-with-reframing.** The outcome — an unattended, gap-honest series — is right and
aligned with this project's evidence culture. Keep AC-1, AC-4, AC-5. Change the route:
make it the project's metrics ledger rather than a downloads-only cron; decide the commit
target deliberately (`paths-ignore` or an orphan `metrics` branch) because the default
collides with `ci.yml`'s trigger and its cancelling concurrency group; move AC-2's
guarantee into #861's write path as a build-then-rename invariant and drop AC-3's
workflow-level failure test in favour of a fixture unit test there; add a notification so
a loud failure is actually heard; and record, next to the data, which decision the series
is allowed to make.
