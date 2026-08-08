# Issue #571: FEAT-C30-6: the two developers this repo bounced hear back, Digital's rejected authors get invitations by name, and a sub-48-hour first-response record is published and kept
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its acceptance criteria, #571 makes one claim about what JLS should become: **a project that
answers.** Everything else in CAP-30 (#514) is about arrival — templates (#567), a shelf of first issues
(#568), a codebase that survives inspection (#316). This is the only feature about what happens *after*
someone shows up. That claim is correct, it is the cheapest item in the entire programme, and #508 §3
ranks half of it as the highest-leverage move on the board. The goal is endorsed without reservation.

The instrument is another matter. I am disregarding AC-3 and AC-4 as written, and rescoping AC-2, because
the evidence sitting in this repository says the project's failure with external developers was **not
silence**, and a first-response latency objective therefore measures the one thing that was never broken.

## The evidence, re-read

I pulled the three PRs the issue is built on:

| PR | Author | Opened → closed | Comments | Outcome |
|---|---|---|---|---|
| #4 Collision Performance | AmityWilder (human) | 2026-02-08 → 2026-07-17 | **17** | closed unmerged |
| #5 Fix saving to opened files | AmityWilder (human) | 2026-02-10 → 2026-07-17 | 2 | closed unmerged |
| #187 Make addToBook deterministic | Dodothereal | 2026-07-18 → **2026-07-22** | — | closed unmerged |

Two facts follow that neither #571 nor #514 nor #508 states:

1. **"Two developers" is one developer.** PR #187's body says outright: *"This contribution was produced
   by an autonomous AI coding agent (Claude Code) that @Dodothereal operates."* That is a legitimate
   contribution, but it is not a member of the human developer community CAP-30 is built to recruit, and
   a 4-day close is not the wound the issue is dressing. The actual bounced human is AmityWilder, twice,
   after **five months and seventeen comments** on #4.
2. **The pathology is disposition, not latency.** #187 got a decision in four days. #4 got sustained
   engagement and then a close. Publishing a `<48h first-response` objective (AC-3) and tracking it
   (AC-4) would have flagged nothing about either case. Meanwhile the fixes landed: `jls/SpatialIndex.java`
   exists (the #4 idea), and CHANGELOG.md:261 records the #182/#187 determinism fix. **Neither external
   author is named anywhere in CHANGELOG.md, CONTRIBUTING.md, or the PR template.** The repo has no
   attribution convention at all.

So the honest reading of "merged by other means" is: *a stranger diagnosed a real defect, the maintainer
re-implemented it to the house standard, and the stranger's name left the record.* That is a repeatable
mechanism, and it will repeat on the next contributor whether or not a template exists. AC-1 apologises
for it once. Nothing in CAP-30 stops it happening again.

## Reframing 1 — commit to disposition, not to response time (replaces AC-3/AC-4)

A `<48h` first-response SLO on a bus-factor-1 project is a liability that is trivially satisfiable by a
GitHub Action posting "thanks, we'll look" — Goodhart bait with the maintainer's reputation as collateral,
and the ordering-correction comment already flags that keeping it is a standing cost against a
`0.5-1 + ongoing` band. Replace it with a commitment that would actually have changed the #4 outcome:

> **No external PR is closed unmerged.** It is either landed (possibly after maintainer follow-up commits
> on the same branch), or it is closed with a written reason *and* the reason names which of the repo's
> published bars it missed. If the change's idea is later implemented by other means, the originating PR
> and author are named in the CHANGELOG entry and in a `Co-authored-by:`/`Reported-by:` trailer.

Track **time-to-decision and disposition mix** (landed / landed-after-help / declined-with-reason), not
first-response median. That is a record an outsider can read to predict what will happen to *their* PR,
which is the only thing the metric is for. It also costs nothing per-PR at this volume — one to three
external PRs a year — and it degrades gracefully when the maintainer is busy, which a 48-hour clock does not.

## Reframing 2 — the merge path is the repellent, not the on-ramp

CONTRIBUTING publishes a genuinely formidable contract: warnings-as-errors, SpotBugs High, CodeQL,
JaCoCo bundle *and* per-package ratchets, PIT mutation/test-strength ratchets, NullAway + JSpecify with a
never-unmark ratchet, `#94` value-semantics rules, `#95` sealed-dispatch rules, and a house style
("tabs, `// end of X method` trailers"). This discipline is the project's category-leading asset — #508
says so — and it is simultaneously the reason a drive-by 137-line perf patch cannot land as authored. The
gap CAP-30 never names: **arriving is not the hard part; clearing the ratchets is.** #567/#568/#316 all
reduce arrival friction. Zero features reduce landing friction.

The concrete out-of-the-box move, worth more than the whole outreach half: publish a **finishing policy**.
An outsider's PR carrying a correct diagnosis and an imperfect patch is accepted onto a maintainer-pushed
branch, the maintainer adds the regression test / `@Nullable` annotations / floor raise, and it merges with
the contributor as author. One line in CONTRIBUTING, one worked example, and the "merged by other means"
failure mode structurally cannot recur. It also converts the ratchets from a wall into a service.

## Reframing 3 — AC-2 is aimed at the wrong pool

AC-2 proposes named invitations to Digital's rejected-PR authors. Three problems, in ascending severity:

- **Adverse selection.** These are, by construction, people whose contributions a *less* strict maintainer
  declined. Inviting them into the strictest tracker in the niche, before Reframing 2's finishing policy
  exists, reproduces the #4 outcome at scale — and the gate on #567/#568/#316 does not help, because none
  of those three touch the merge bar.
- **Channel risk.** Unsolicited by-name @-mentions to 26 people about a competitor's rejection of their
  work is one screenshot away from reading as poaching, and it pulls directly against KC-30-1's own
  "on their own merit, not marketing" principle.
- **A warmer pool already exists and is already filed.** #509 (WashU CSE 260M / bsiever fork), #517/#577
  (course kits, CSE 260M corpus), and the MTU/GVSU lineage are people who *already run this software*.
  #508 ranks contacting Bill Siever above everything else on the board. A course maintainer who ships JLS
  to students has durable motivation to fix it; a stranger rejected from Digital has none.

Concretely: retarget AC-2's named-invitation list at the course/fork pipeline, and serve the Digital pool
with an **artifact instead of a message** — a public "coming from Digital" page (feature mapping, file
import path, and a table of *their* long-open asks that are filed here: #289 dark mode, live subcircuit
dive, keybindings via #570/#521). Discoverable, non-intrusive, doubles as positioning, and it recruits
while the maintainer sleeps. Keep by-name outreach for the two or three cases where someone publicly asked
for a thing JLS now has.

## What survives untouched

- **AC-1 — do it this week.** The ordering correction comment is right that it is ungated, and it is right
  for a reason the comment understates: AmityWilder engaged 17 comments deep and got nothing back but a
  close. One comment on #4/#5 naming `SpatialIndex.java` as their idea shipped, one on #187, plus a
  CHANGELOG credit line for both. Cost ≈ 0. This is the whole issue's value, and it does not need the rest.
- **AC-5's kill criterion.** Correctly shaped, and the boundary comment is right that retirement-with-
  evidence is a legitimate close. Under Reframing 1 it should also be able to fire on the *disposition*
  record, not only on a zero-PR count.
- **The "partly non-code feature" framing.** This is the right call and the project should do more of it.

## Verdict

**endorse-with-reframing.** The claim — JLS should become a project that answers — is one of the truest
things in the tracker, and AC-1 is the single best-value action filed anywhere in CAP-30. But the issue
diagnoses its own evidence backwards: it treats non-response as the failure when the record shows
five months of response ending in a close, and it aims recruitment at a cold, adversely-selected pool
while a warm one sits filed at #509/#517/#577. Keep AC-1 and AC-5; replace the latency SLO with a
disposition commitment and an attribution convention; move the finishing-policy gap into CAP-30 as the
feature it is missing; retarget AC-2 and let a landing page do the Digital-facing work.

What would change my mind: evidence that the 2026 PRs died from maintainer silence rather than from the
merge bar. The 17-comment thread on #4 is the reason I do not believe that.
