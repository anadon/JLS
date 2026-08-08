# Issue #571: FEAT-C30-6: the two developers this repo bounced hear back, Digital's rejected authors get invitations by name, and a sub-48-hour first-response record is published and kept
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

A "partly non-code" feature under capstone CAP-30 (#514): comment on the two
bounced 2026 external PRs (#4/#5, #187) with a merged-by-other-means
explanation and an invitation back (AC-1); build a named-invitation list of
Digital's rejected-PR authors, gated on #567/#568/#316 (AC-2); publish
<48h/<1wk response SLOs in CONTRIBUTING (AC-3); keep a rolling record of
actual response times (AC-4); and a kill criterion if two quarters pass
post-PF-1..4 with zero external PRs (AC-5).

## Findings, most severe first

**1. [Critical] The issue body and its own "ordering correction" comment
disagree, and only the comment is right.** The machine block at the top of
the issue still reads:

```yaml
ordering_after: ["FEAT-C30-1 #567", "FEAT-C30-2 #568", "#316 (FEAT-008 — the PF-3 prerequisite, owned by CAP-00)"]
```

The 2026-08-08 comment (`issuecomment-5227450446`) says this is wrong — only
AC-2 and AC-5 actually need the #567/#568/#316 gate; AC-1, AC-3, and AC-4 are
"ready now" — and posts a `ordering_after_by_criterion` table as the
correction. But the comment explicitly states it changes nothing in the
acceptance criteria or the machine block ("No acceptance criterion is struck,
reworded or moved") and the body was never edited. Anyone reading only the
issue body (which is how `ARCHITECTURE.md`'s own conventions and #316's
`REPLAN:` discipline assume issues are consumed) still sees the whole feature
gated behind a 12-20 mw editor refactor. This is exactly the kind of
half-applied correction #316 itself warns about ("a half-edge is the defect
this Link pass exists to prevent") — except here it's a half-*correction*,
not a half-edge. **Recommendation:** edit the machine block itself (or add a
`REPLAN:`-style block) rather than leaving the fix live only in a comment
thread three levels deep.

**2. [High] AC-1's factual premise is stale, uneven, and the issue doesn't
account for it.** I read all comments on #4, #5, and #187 directly:

- **#5** already has a substantive 2026-07-17 closing comment from anadon
  naming the exact commits that superseded it (#34/`590f124`, #15/#81, #174)
  and thanking the author — this is most of what AC-1 asks for, just missing
  an explicit "come back" line.
- **#4** has only a terse one-liner, `"Closing as fixed in another branch."`
  (`issuecomment-5005660223`), after an extensive, personally invested
  15-comment exchange in which the maintainer initially demanded proof the
  contribution wasn't AI-generated, the student (AmityWilder) explained
  juggling five classes and a professor's involvement, and a third party
  (bsiever) vouched that the work "wasn't for naught." A bare "fixed
  elsewhere" line, with no thanks and no acknowledgment of that history, is a
  worse outcome than #5's, not a comparable one — yet the issue treats both
  identically.
- **#187** has **zero comments**, despite being closed 2026-07-22.

AC-1 as written ("Comments on the bounced 2026 PRs state the merged-by-other-
means outcome and extend an explicit invitation back") can be judged
"already satisfied" for #5 by a shallow check, is clearly unsatisfied for #4
and #187, and none of the three currently contains the "explicit invitation
back" AC-1 actually demands. The issue should have surveyed prior state
before filing so the acceptance criterion is checkable against what's
missing, not restated as if nothing had happened. **Recommendation:** add to
AC-1 an explicit per-PR checklist reflecting current state (#5: add the
missing invitation line; #4: replace or supplement the terse comment; #187:
write one from scratch) instead of one criterion applied uniformly to three
different situations.

**3. [High] AC-1/AC-2 conflate a human contributor with an AI-agent
operator, and this contradicts the capstone that spawned this issue.**
Capstone #514's "Why this is a capstone" section states plainly: "both
external humans who brought PRs in 2026 bounced unmerged." But PR #187's own
body is an explicit AI-assistance disclosure: *"This contribution was
produced by an autonomous AI coding agent (Claude Code) that @Dodothereal
operates and monitors."* That is not a second human contributor bouncing —
it's an agent-operator disclosure, a materially different category the
survey's own framing (#510 §5, "the one reachable dev community... Digital's
rejected-contributor pool") never mentions. #571 inherits #514's "two
developers"/"two bounced... authors" framing verbatim without correcting it.
An "invite them back to the human community" outreach comment addressed to
an AI-agent operator reads differently than one addressed to a student, and
the issue gives no guidance on how (or whether) to adjust the message.
**Recommendation:** either correct the "two developers" framing here and in
#514, or explicitly decide (and state) that agent-operated contributions are
in scope for the same outreach and say why.

**4. [High] AC-2's "invitation by name" has no specified channel, and
directly contacting a competitor's rejected-PR authors is a real hazard the
issue never addresses.** The named list is sourced from #510 §5's teardown
of Digital's tracker (26 open PRs, #1464/#1470 named). Nothing in AC-2, in
#514, or in #510 specifies *how* the invitation is delivered — a public
comment on Digital's own issue tracker, a direct message, an email found via
their GitHub profile? Cold-soliciting a named list of another project's
spurned contributors, especially by commenting on *their* tracker, risks
reading as poaching to Digital's maintainer and to the contributors
themselves, and cuts against the "welcoming" positioning the survey argues
for. KC-30-1 (in #514) guards against PF-5 features shipping "only to
poach," but that kill criterion is scoped to feature work, not to this
direct-contact act. **Recommendation:** AC-2 should specify the invitation
channel and a norm (e.g., only via channels the recipient already made
public/reachable, never via comments on Digital's own tracker) before any
invitation goes out.

**5. [Medium] AC-4's "rolling record" is underspecified to the point of
being gameable.** "First response" is never defined — a bot-generated label
or triage acknowledgment would satisfy a naive median-time measurement
without any human engagement, so the published <48h objective could be met
on paper while the real goal (a contributor gets substantively answered)
fails. The capstone's AC-4 ("median first-response <48h... over a rolling
quarter") has the same gap. **Recommendation:** define "first response" as
the first *substantive, human-authored* comment from a maintainer, and
exclude bot/label-only actions explicitly in the tracker doc AC-4 produces.

**6. [Medium] The published SLO can outlive any near-term exit.** Per the
correction comment, AC-3 (publish <48h/<1wk objectives) can start "now," but
AC-5's kill clock only starts counting "two quarters post-PF-1..4," and PF-3
(#316) is a 12-20 mw refactor whose six component tasks are all "not filed."
On a bus-factor-1 project (the issue's own notes call this "a standing
obligation on a bus-factor-1 project"), that means the maintainer could be
holding a public <48h response commitment for a year or more with no
reachable exit ramp, since the exit is defined relative to a dependency that
may not land soon. **Recommendation:** give AC-5 (or a new criterion) a
calendar-anchored fallback exit (e.g., "or 12 months after AC-3 publishes,
whichever is sooner") independent of PF-1..4 landing.

**7. [Low, verification gap] The "merged by other means" claim for #187 is
asserted, not cited.** Unlike #5 (which anadon's prior comment ties to named
commits), nothing in #571, #514, or #510 cites the specific commit that
superseded #187's `addToBook` determinism fix. I found circumstantial
support in the repo — `test/jls/PrintPageOrderTest.java` references issue
#182 and describes `addToBook`'s prior non-determinism — but no commit
message directly matching "sort by stable ID" tied to #182/#187. Before
posting a "your fix landed elsewhere" comment to a real contributor, this
needs a commit citation the way #5's did; asserting it without one risks
telling someone something false on a public record.

## What's solid

- The boundary-note comment (`issuecomment-5176175892`) correctly
  distinguishes this issue from #567 (removing the on-ramp barrier vs.
  actually answering people) and flags the CONTRIBUTING collision-by-section
  hazard between the two — good, concrete deduplication work.
- AC-5 as a genuine kill criterion, not a disguised deliverable, is the right
  shape, and the notes section says so explicitly.
- The cost band (0.5-1 mw + ongoing) is honestly scoped for the one-time
  actions; only the "ongoing" SLO-keeping part is open-ended, and the issue
  says so.

## Verdict rationale

The core idea — close the loop with real people who already engaged this
repo — is sound and cheap. But the issue as currently written is not safe to
execute as-is: its acceptance criteria don't reflect the actual state of the
threads they target (finding 2), inherit a factual error about who #187's
author is (finding 3), leave a stale contradictory gate in the machine block
that a literal reader would still honor (finding 1), and are silent on a
real solicitation hazard for AC-2 (finding 4). These need correction before
anyone posts on a real contributor's behalf. Hence **needs-rework**, not
`sound-with-concerns` — the defects are in what would actually get typed
into public comment threads, not just process hygiene.
