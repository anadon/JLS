# Issue #824: TASK-C568-3: the shelf refills itself — a written restocking rule plus a scheduled check that says so out loud when the count drops below ten
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the task down and it serves exactly one line of CAP-30 (#514): AC-2, "the
good-first-issue label holds ≥10 open items continuously for a quarter." That
AC is itself a proxy. The capstone's actual outcome is "an outside developer
finds JLS, picks a labeled first issue, ... and merges within a week," measured
by AC-4 (three external PRs merged, median first response <48h). Label
inventory is the input; merged outside PRs are the output. #824 proposes to
build monitoring infrastructure for the input while the output has never once
been observed — this repo bounced both of its 2026 external PR authors.

That is not fatal. Inputs are worth instrumenting. But it determines how much
machinery the task deserves and where the machinery should sit, and on both
counts the issue picks the wrong seam.

## Grounding

- `repo:anadon/JLS is:open label:"good first issue"` → **0 issues**, against
  **605 open issues** total. The shelf does not exist yet; #823 builds it.
- `src/jls/edit/SimpleEditor.java` is **5,852 lines** at HEAD — unchanged from
  the number CAP-30 quotes. Capstone AC-5 does not pass while it stands, and
  the survey names it as the single biggest repellent.
- `.github/ISSUE_TEMPLATE/` holds `capstone.md`, `feature.md`,
  `scientific_task.md` — three planning-corpus templates, no human bug report
  (that is #567, unlanded).
- Existing scheduled workflows: `mutation.yml` (weekly, **fails red** on a
  ratchet breach), `scorecard.yml` (weekly), `ci.yml` nightly cron. All three
  signal by turning a run red or publishing to an external service. **None**
  writes to the issue tracker.

## The reframing: measure rot, not count

The shelf will not drain the way this issue imagines. With 605 open issues and
no external PR throughput, item count is the quantity *least* likely to move.
The quantity most likely to move is **accuracy**. #823 requires each curated
item to name its entry-point files and the tests that judge it. Those pointers
rot as the tree moves — and the tree is about to move hard: #316 decomposes
`SimpleEditor`, #223 freezes the extension API, the capability-roadmap sweeps
touch `jls.sim` and `jls.elem` broadly. A shelf curated in August 2026 can read
"15" every week for a year while every entry-point path on it has gone stale.
The count check would stay silent the entire time. **Silence would mean the
shelf is stocked with lies, and AC-2 explicitly promises the opposite.**

So: the check that earns its keep is not `count < 10`. It is:

1. every item on the curated list is still open and still labelled;
2. every file path the item's curation names still exists at HEAD;
3. every test the item names as its acceptance signal still exists at HEAD;
4. and, derived last and cheaply, the count.

(1)-(3) are the same cost to implement as (4) and catch the failure that will
actually happen. AC-5's "relabelling stale issues doesn't count" is prose
trying to prevent by exhortation what (1)-(3) prevent structurally.

## The seam: a committed manifest, not a bot that opens issues

This project already has a settled idiom for "an invariant that must not
erode," and it is used six times over: the JaCoCo floors in `pom.xml`, the PIT
thresholds in the `pitest` profile, `NullMarkedRatchetTest`,
`SealedHierarchyTest`, `HeadlessCoreRatchetTest`, and the `docs/` record
convention (`docs/mutation-testing-trial-2026-07.md`,
`docs/reproducibility.md`) where a committed file is named as the source of
truth. #824 proposes a seventh, novel mechanism — a scheduled job that opens or
updates a tracker issue — for the weakest invariant in the set.

The alternative that dissolves most of the task: **make the shelf a committed
file.** `docs/good-first-issues.md` lists the curated items with their entry
points and acceptance signals; #823's curation pass writes it; the scheduled
job validates it against HEAD and the API and turns red on a breach, exactly
like `mutation.yml`. Consequences:

- **AC-3 becomes free.** "Was the count ≥10 for a quarter?" is `git log -p --
  docs/good-first-issues.md`. No invented time series.
- **AC-3's stated mechanism is a trap worth naming.** If the count is recorded
  as an Actions artifact, default retention is 90 days — one day short of a
  quarter. A recording medium that expires just before the question is asked
  cannot answer it. Committed files do not expire.
- **AC-2's tracker notice becomes unnecessary, and it was actively harmful.**
  CAP-30's own diagnosis is that "the tracker's spec-prose reads as an internal
  monologue" to outsiders. Adding a bot-maintained meta-issue about label
  inventory to a 605-issue tracker makes the repellent worse, in public, in
  service of a metric about attracting the people it repels. A red scheduled
  run is invisible to a browsing outsider and unmissable to the maintainer.
  That is the right visibility profile.
- **The rule gets one home.** AC-1 offers "CONTRIBUTING or a tracker
  meta-issue." Given CAP-30's read on the tracker, CONTRIBUTING — next to the
  coverage-ratchet climb convention it structurally resembles — is the only
  defensible choice. I would drop the "or."

I am explicitly disregarding AC-2 and AC-3 as written. AC-1 and AC-5 stand
as-is; they are the cheap, correct half of this task.

## Apply the project's own hard-won lesson to AC-4

AC-4 asks that an API failure fail loudly. Correct, but this repo has learned
something sharper, recorded in CONTRIBUTING: *"a floor that has never been seen
to fail should be assumed vacuous"* — earned from the `include`-pattern bug
where slash-form package names silently matched nothing, and from the #233
zero-headroom incident. The same rule binds here with more force, because this
check's normal state is silence. **The task should require one observed firing:
a `workflow_dispatch` run with the threshold temporarily raised above the
actual count, linked from #568, proving the alarm can go off.** Without that,
the check is a floor nobody has seen fail, and the project already knows what
those are worth. This is a stronger AC-4 than the one filed, drawn from the
repo's own convention rather than from general principle.

## Consolidation the issue does not see

Sibling #571 (FEAT-C30-6) AC-4 wants "a rolling record (tracker query or doc)
[showing] actual first-response times," feeding capstone AC-4. That is the same
machinery class: a scheduled measurement of one funnel metric, recorded
durably enough to answer a quarterly question. Building two separate one-off
mechanisms for one capstone's two metrics is how a project accumulates
unmaintained cron jobs. One `funnel-health.yml`, writing both the shelf state
and first-response times into one committed record, is strictly simpler and
makes the capstone's health readable in one place. If #824 lands its own
bespoke workflow first, #571 inherits an awkward choice between duplicating it
and retrofitting it.

## Sequencing, and a kill hook

CAP-30 carries KC-30-2: two quarters post-PF-1..4 with zero external PRs
falsifies the outreach premise and retires active recruitment. An automated
restocking alarm built now could plausibly outlive the program it monitors,
firing into a repo that has stopped caring. Two consequences:

- The **written rule (AC-1, AC-5) should land immediately** — it is nearly
  free, it is what #568 AC-2 literally requires, and it is what a curious
  outsider can actually read.
- The **automation should be gated on evidence of drain**: the first time the
  shelf loses an item to a merged external PR, or the first quarterly manual
  check that finds it below 10. Until then a manual quarterly look costs
  minutes and a scheduled job costs a maintained workflow file that #317's
  timeout lint will also police (per the boundary note on #568).

And the honest ordering point the issue avoids: this is 0.25–0.5 mw of
inventory monitoring in a capstone whose AC-5 is blocked by a 5,852-line god
class that still sits at exactly its surveyed size. Nothing on the shelf
matters if the first file a Digital-calibre reviewer opens loses the
code-inspection duel. #824 should not be allowed to feel like progress on
CAP-30 while #316 sits still.

## Verdict

**endorse-with-reframing.** The written rule is right and should ship now, in
CONTRIBUTING only. The automation half should be rebuilt on a different seam:
a committed shelf manifest validated for freshness (not merely counted),
signalling by a red scheduled run (not a tracker issue), with one observed
firing to prove it is not vacuous, ideally merged with #571's response-time
record into a single funnel-health job, and held until the shelf has been seen
to drain at least once.
