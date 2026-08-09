# Issue #824: TASK-C568-3: the shelf refills itself — a written restocking rule plus a scheduled check that says so out loud when the count drops below ten
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What's being asked

Third task in the `part_of_feature: 568` chain (`ordering_after: TASK-C568-2`
= #823, "the initial fifteen"): write a restocking rule for the
`good first issue` label (threshold below 10, target back to ≥15, an owner)
in CONTRIBUTING or a tracker meta-issue (AC-1), back it with a scheduled
workflow that raises a visible signal when the count is below threshold
(AC-2), make the count auditable after the fact so "≥10 for a quarter" is
answerable (AC-3), fail loudly on API error rather than recording a false
zero (AC-4), and state that relabelling stale/oversized issues doesn't
count as restocking (AC-5). Live-verified: `label:"good first issue"
is:open"` returns 0 results today, and `grep -i "good first issue"
CONTRIBUTING.md` returns nothing — the rule this task would write does not
exist yet in any form, matching the gap the sibling review of #568 already
flagged.

## Findings, most severe first

**1. [High] AC-2 and AC-3 are in tension: a "signal only when below threshold" design cannot support "≥10 for a quarter, answerable after the fact."**
AC-2's own words: "raises a visible signal ... when the count is below the
threshold — silence means the shelf is stocked, not that the check is
broken." Under that design, a run that finds the count fine produces no
record at all — nothing is logged, committed, or commented on a good week.
AC-3 then asks for a record that makes "≥10 for a quarter" answerable
after the fact. But if the only artifacts are alarm-on-breach signals, a
reviewer three months later cannot distinguish "the count stayed ≥10 the
whole quarter" from "the workflow silently stopped running in week 2 and
never got the chance to alarm" — exactly the ambiguity AC-2's "silence
means stocked, not broken" line asserts away without any mechanism to back
the assertion. AC-4 only guards against an *API* failure inside a run that
did execute; it says nothing about a run that never fired. **Recommendation:**
require the workflow to record the count on *every* run, not just
breaches (e.g. append a line to a checked-in log, or an always-updated
tracker-issue comment/table), so "answerable after the fact" doesn't rest
on inferring meaning from absence of evidence.

**2. [High] AC-4 covers a failed API call but not a workflow that never runs — a well-known, concrete platform hazard for exactly this project's traffic pattern.**
GitHub disables `schedule`-triggered workflows automatically after 60 days
with no repository activity on the default branch, resuming only on the
next push. `ARCHITECTURE.md` (i18n section) and `README.md`'s Contributing
section both describe JLS as a single-maintainer tool; a maintainer pause
of two months is not a fringe scenario here, and it is precisely the
period a "shelf" going unwatched matters most. AC-2's "silence means
stocked" promise is false in exactly this case — the check goes dark, not
the shelf. Neither AC-2 nor AC-4 mentions this failure mode, and nothing
in the acceptance criteria would catch it (a dead cron produces the same
outward silence as a healthy one). **Recommendation:** add an explicit
"last successful run" signal readable without relying on the workflow
itself firing — e.g. a badge or dated line in CONTRIBUTING/README updated
each run, so a stale timestamp is itself the alarm.

**3. [Medium] "Who is responsible" (AC-1) is close to a no-op for a declared single-maintainer project.**
`ARCHITECTURE.md`'s i18n section states the rationale for other process
decisions on the premise that JLS "is a single-maintainer pedagogy tool" —
there is exactly one person (`@anadon`) who could plausibly be named. AC-1
can be satisfied by writing that one name down, which adds no operational
information: if that person is unavailable, naming them in the rule
doesn't change the outcome. This isn't wrong to ask for, but as an
acceptance criterion it's trivially game-able into a formality.
**Recommendation:** either drop the "who" clause or make it substantive —
e.g., require the rule to state what happens if the responsible party is
unavailable when the threshold trips (does the scheduled issue just sit
open, unresolved, until someone returns?).

**4. [Medium] AC-2's "visible signal" doesn't specify idempotency, leaving room for either issue-spam or a signal nobody re-reads.**
The text allows "an issue comment or a dedicated tracker issue" without
saying whether repeated breaches should update one persistent artifact or
create a new one each time. A naive implementation that opens a fresh
issue on every weekly run while the count sits at 8 produces a growing
pile of duplicate low-signal issues (itself degrading the tracker the way
AC-5 is trying to prevent from happening to the label); a naive
implementation that comments once and never again is easy to lose in
scrollback. The repo's own prior art (`mutation.yml`, `scorecard.yml`) is
schedule + `workflow_dispatch`, least-privilege `permissions: contents:
read` — neither existing workflow opens or updates issues, so there's no
in-repo pattern to imitate for the find-or-update behavior this task
actually needs. **Recommendation:** specify a find-or-create/update
pattern against one persistent tracker issue, and note the `issues:
write` permission the token will need (undiscussed in the issue, and a
step up from every existing scheduled workflow's read-only footprint).

**5. [Low] Sequencing risk: the rule's "target ≥15" describes a state the shelf has never been in.**
`ordering_after` correctly lists #823 (currently open, unresolved), and
today's live count is 0, not merely "below ten." Nothing in #824 blocks
itself from landing before #823 does, or notes what the restocking rule
should say in the interim (a rule that says "restock back to ≥15" while
the shelf has never held 15 reads oddly to a reader who checks). Not
fatal — `ordering_after` is directional guidance in this tracker's
convention, not enforced — but worth an explicit note in the written rule
that day-one state is "below threshold by construction" rather than a
regression.

## What's solid

- The overall shape — write the rule, then verify it mechanically rather
  than trust it — directly answers the gap the sibling #568 review
  flagged (a written-only rule with no enforcement); this task existing at
  all is the right move.
- AC-5 (relabelling stale/oversized issues doesn't count as restocking) is
  a real and correctly-identified gaming vector, and stating it in the
  rule is a cheap, effective guardrail regardless of the mechanism gaps
  above.
- AC-1's threshold/target numbers (below 10 / back to ≥15) are concrete
  and match capstone #514's and feature #568's own AC-2 language, so there
  is no numeric drift between this task and its parents.

## Verdict rationale

The core idea is sound and the numeric bar is consistent with its
parents, but the acceptance criteria as written can be satisfied — a rule
gets written, a workflow gets merged, it runs green for a while — without
actually producing the thing capstone #514 needs: a provable "≥10 for a
quarter" record that survives silent workflow death. That's a spec gap,
not a reason to shelve the task; needs-rework, not should-not-proceed.
