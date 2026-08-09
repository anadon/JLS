# Issue #829: TASK-C571-2: a sub-48-hour first response and a one-week merge decision are published as objectives, and the actual times are visible enough to be held to
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

One prospective contributor, hovering over the Fork button, asking: *if I spend an evening on
this, will anything happen?* Everything in #829 exists to answer that question **before** they
spend the evening. The published objective is the promise; the rolling record is the reason to
believe it. The goal is right and it is cheap, and I endorse it.

The instrument is wrong for this project at this scale, and wrong in a way that is specific
enough to fix. I am **disregarding AC-2 and AC-4 as written** — the median-computing rolling
record — and reshaping AC-3 and AC-5 around a different artifact. Three arguments, then the
alternative design.

## 1. The record, as specified, will mostly publish "no data" — an emptiness advertisement

The population is known: two external PR authors in all of 2026 (#4/#5, #187), and #571's own
visionary review establishes that one of those was itself an agent operated by a third party.
Call it one to three external items a year. AC-4 asks for a median over a trailing quarter;
AC-5 correctly forbids reporting an empty quarter as a satisfied objective. Compose the two and
you get the steady state: **a generated page that says "no data" most of the time**, published
at the exact URL CONTRIBUTING points a hesitant stranger at. The metric is honest and the
signal it sends is "nobody contributes here."

A median is the wrong summary statistic when n is 0, 1 or 2 — not because it is inaccurate, but
because at that n the *individual cases are more informative than any summary of them*. The
reframing is to drop the aggregate entirely and publish the **enumeration**:

> `docs/external-contributions.md` — an all-time, append-only ledger. One row per external
> issue or PR ever opened: who, what, opened, first maintainer response, decision, where the
> work actually landed (commit/CHANGELOG line), and current status.

At n=3 that table *is* the record. It never degrades to "no data" — it says "three people
showed up; here is precisely what happened to each of them," which is what the hovering
contributor wants and what a median can never tell them. AC-4's trailing-quarter median stays
computable from it by anyone at grading time (the capstone bar survives), but nothing has to be
maintained to keep it computable, and no automation is built before there is data to justify
it. Build the pipeline when the ledger passes ~10 rows, not before.

Bonus: the ledger's "where the work landed" column is the *only* artifact in CAP-30 that
structurally prevents the #4 failure — a stranger's idea shipping under the maintainer's name —
from recurring. A latency dashboard cannot; a disposition ledger must, because the column is
blank until it is filled.

## 2. This repo does not build reports. It builds checks that fail.

Read the codebase as a whole and one idiom dominates every contract: **write the rule down,
then pin it with something that goes red when the document stops being true.** `HeadlessCoreRatchetTest`
pins the no-AWT-in-`jls.sim` rule; `NotificationRatchetTest` pins the "only `TellUser` makes
dialogs" rule; `NullMarkedRatchetTest` pins the never-unmark ratchet; `ExtensionPointCatalogTest`
cross-checks `docs/extension-points.md` in both directions; `HelpTopicsTest` fails when a
palette entry lacks a help topic. The rigs follow the same shape — `scripts/wayland-rig.sh`
paired with `wayland-rig-selftest.sh`, same for macOS, X11 and the icestick handoff. And
`.github/workflows/mutation.yml` is the precedent that matters most here: a weekly
`cron: "0 5 * * 0"` lane, never a required PR check, where CONTRIBUTING states flatly that "a
red weekly run is a regression to fix, not noise."

#829 proposes the one thing this repository never builds: a **passive report**, generated and
read by nobody, that grades the maintainer after the failure has already happened. It is a
lagging indicator on a project whose entire engineering culture is leading indicators.

**Reframe the SLO from a scoreboard into a watchdog.** Same cost band, strictly more useful:

- `.github/workflows/response-watch.yml`, daily cron. Query open issues/PRs by external
  authors; for each with no maintainer response, compute age.
- At ≥24h (half the objective), it acts: updates one standing "unanswered external
  contributions" issue, which is what actually pages a bus-factor-1 maintainer who missed the
  notification email.
- Its state file *is* the ledger of §1 — AC-2 satisfied as a by-product of the mechanism rather
  than as its own deliverable.
- Per repo idiom, it ships with `scripts/response-watch-selftest.sh` driving the unmodified
  logic against fixture data (an item with a bot-only reply, an item with a label-only event,
  an item closed silently, an empty period) and asserting the classification. Without that, the
  "what counts as a first response" method in AC-3 is prose nobody can verify — exactly the
  gap the rig selftests exist to close everywhere else in this repo.

The difference is causal, not cosmetic: a scoreboard tells you afterwards that you missed 48
hours; a watchdog makes you not miss it. The capstone's AC-4 wants the *outcome* (median <48h),
and a control loop produces outcomes that a report only describes.

## 3. Self-published metrics pull against this project's own credibility idiom

Every external-facing trust claim JLS makes is built to be verified **without trusting the
maintainer**: SHA256SUMS, SLSA build-provenance attestations, keyless cosign on the container,
byte-reproducible jar and BOM with a published `.buildinfo` and an independent-rebuild recipe
(`docs/reproducibility.md`), an OpenSSF Scorecard badge in the README's first line. That is the
project's signature: *don't take my word for it, recompute it.*

A maintainer-generated responsiveness metric, on a denominator the maintainer defines, is the
one governance claim that would arrive in the opposite register — a self-graded scorecard. The
elegant fix is to apply the repo's own reproducibility principle to governance: **the record
generator must be runnable by a stranger against public GitHub data and produce the same
table.** No tokens beyond a public read, no private state, deterministic given a cutoff date.
Say so in the method document. That single property makes the number credible in a way no
amount of "we promise" prose can, and it is a natural fit for a project that already ships a
recipe for rebuilding its own jar bit-for-bit.

## 4. The definitional hole is a differentiator, not an embarrassment

AC-3 says a first response is "a substantive human comment, not a bot or a label." On this
repository that test is undecidable as stated: the maintainer's own comments on #571 carry
`_Generated by [Claude Code](https://claude.ai/code)_` while being authored by `anadon` with
`author_association: OWNER`, and this review fleet is itself an instance of the same pattern.
The adversarial pass treats this as a gaming vector to be defended against. I read it the
other way.

**The reason JLS can credibly offer a sub-48-hour objective, when a human-only bus-factor-1
project cannot, is that its maintenance is agent-augmented.** Hiding that behind a "human
comment" definition makes the promise both less keepable and less honest. Declare it instead:

> A *first accountable response* is a substantive reply that engages the specific content of
> the contribution, posted by the maintainer or by an agent acting under the maintainer's
> supervision and labelled as such. Agent-authored replies carry the generator trailer. The
> maintainer is answerable for every one of them as if typed by hand.

That is keepable through vacations, gives the record a **mechanical** classifier (trailer
present or absent) instead of a subjective "substantive" judgment, and turns the most
suspicious fact about this tracker into a stated reason the promise is realistic. It also
forces the honest disclosure that a stranger deserves anyway.

## 5. And the promise itself can be replaced by a fact

The deepest reframing: a 48-hour clock is a *target you can miss*. A stranger's real question
is not "what is your SLO" but "are you around right now." Publish a maintainer-status line in
CONTRIBUTING — `active` / `slow` / `paused`, with a last-updated date — beside the ledger. A
line that says "active; typical first answer within a day — if this says *paused*, expect
weeks" is more decision-useful than any median, is never false, degrades gracefully instead of
breaking, and costs one edit when life happens. The objectives of AC-1 then read as the
calibration of that status rather than as a debt that accrues silently while the maintainer is
on holiday.

## What survives untouched

- **AC-1.** Publish the objectives, with the bus-factor-1 honesty clause. Ungated, ~an hour,
  and it is what makes any record worth accruing. Add one line cross-referencing SECURITY.md's
  already-published two-week acknowledgement window so the two numbers do not read as a
  contradiction.
- **AC-3's insistence that the method be written down.** Keep it verbatim as an obligation;
  §2 and §4 only change what the method says and add the selftest that makes it verifiable.
- **AC-5's "no data is never a satisfied objective."** Correct instinct. Under the ledger it
  becomes structurally unnecessary — an empty table is self-evidently empty — which is the
  strongest form of satisfying a criterion.

## Verdict

**endorse-with-reframing.** The claim — that a project which wants strangers must publish what
it owes them and be auditable against it — is true and this is the cheapest place in CAP-30 to
act on it. But the artifact is mis-shaped for a project with one to three external
contributions a year and an engineering culture built entirely on failing checks and
third-party-verifiable claims. Publish the objectives (AC-1), keep an all-time disposition
ledger instead of a trailing-quarter median, implement the SLO as a daily watchdog with a
fixture selftest rather than a report, make the generator reproducible by a stranger, define
"first accountable response" so it admits the supervised agent that makes the promise keepable,
and add a maintainer-status line so the promise never has to be quietly broken. Defer the
metrics pipeline until the ledger has enough rows to be worth automating.
