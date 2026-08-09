# Issue #828: TASK-C571-1: the two developers this repo bounced in 2026 get told what actually happened to their fixes, and get asked back by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

CAP-30 (#514) bets that JLS's growth path runs through people, not features, and #510 §5
names the only reachable pool. #828 is the cheapest piece of that bet and the only piece
where the people are known by name. The end is right and I endorse it: two humans brought
work to this repo in 2026, both were closed unmerged, and neither has been told the truth.
That is a debt, and it should be paid whether or not anyone comes back.

But the issue models the debt as a *communications* failure — say what happened, apologize,
invite them back — and prices it at 0.25 mw of comment-writing. Reading the actual threads,
it is a *throughput and ownership* failure with a specific, structural, still-live cause.
Inviting these two back into an unchanged system is not a low-risk act. It is a bounce with
a paper trail.

## Ground truth, because the issue's own summary is wrong in three places

**(a) Half of AC-1 already shipped, three weeks before this issue was filed.** PR #5 carries
a comment dated 2026-07-17 that does exactly what AC-1 and AC-3 ask: the root cause restated
in the contributor's own terms, each of the four changes traced to where it landed (#34 /
`590f124`, #15/#81, #174 with co-author credit), the Makefile honestly described as
superseded by Maven+Nix, and an apology for the delay. It is a model of the genre. #828 was
filed 2026-08-04 and does not know it exists. The real remaining work is two comments, not
three, and one of them has a template already written inside this repo's own history.

**(b) The two cases are not the same failure and must not get the same comment.**

- **PR #4/#5 — AmityWilder (Feb 2026).** This was not a silent bounce. The opening move was
  "I'll need proof that this submission isn't AI," which she answered gracefully; the
  maintainer then engaged *substantively* for two weeks — counting sort, segment stacks,
  thread kill logic — and she pushed back with a genuinely good engineering argument about
  whether O(n log n)+O(n) beats O(n²) at student circuit sizes. It stalled on 2026-02-24
  with "I can get back to this late in the week," and then five months of silence, and then
  "Closing as fixed in another branch." The failure here is *abandonment mid-collaboration*,
  and it is the more painful of the two precisely because the collaboration was real.
- **PR #187 — Dodothereal (Jul 2026).** Zero comments. Zero reviews. Closed four days after
  opening, in silence. And the timeline is worse than silence: the maintainer filed #182 at
  01:37 on 2026-07-18; #187 arrived at 10:51 the same day fixing it; #182 was closed as
  completed at 15:35 — the maintainer's own pipeline landed the fix four hours after the
  contributor's PR was sitting open. The fix in `CircuitRenderer.addToBook:259-262` is the
  same idea the PR proposed (`getElementsInStableOrder()` instead of `HashSet` order),
  reached independently. The contributor was *raced*, then ignored.

**(c) "Merged by other means" is false for PR #4 and AC-3 will collide with it.** #571's AC-1
instructs "state the merged-by-other-means outcome" — it pre-judges the answer. Nothing of
AmityWilder's diff is in the tree. #3 closed 2026-07-08 via a spatial index
(`circuit.elementsNear(...)`, visible at `src/jls/edit/SimpleEditor.java:4506`), which is the
maintainer's own design, not her bounding-box filter and loop hoist. The honest sentence is
"the problem you found is fixed; your implementation is not the one that landed, and here is
why" — which is a *different and harder* comment to write than #5's. #828's AC-3 is right and
its parent's AC-1 is wrong; when they conflict, AC-3 wins.

## Reframing 1 — the apology is not the deliverable; the rule change is

The reason #187 was raced is not rudeness. It is that this repo's issue supply is consumed by
an agentic pipeline that files scientific-method issues and closes them within hours (#182:
filed 01:37, closed 15:35, same day). A human PR cannot win that race, and there is no
protocol that says it shouldn't have to.

This is not only #828's problem — it is a live contradiction with the capstone it serves.
#568 must hold ≥10 good-first-issues open *continuously for a quarter* (CAP-30 AC-2), and
#514 AC-4 wants three external PRs merged with median first-response <48h. A pipeline that
drains curated issues faster than strangers can claim them makes both criteria structurally
unreachable, no matter how good the templates are.

**Concrete alternative:** before the comments go out, adopt and publish a claim protocol —
an open PR or a self-assignment against an issue freezes the automated pipeline on that
issue; the agent may review, not race. Then the comment to Dodothereal can say something
that is actually worth reading: *"you were racing our automation and we did not tell you the
race existed; that rule is now written down, here it is."* That converts an apology into
evidence. Without it, the invitation asks two people to re-enter the exact system that
produced the bounce.

I would put this rule ahead of #567 and #568 in CAP-30's ordering. Templates make filing
possible; the claim protocol makes *finishing* possible, and finishing is what AC-4 grades.

## Reframing 2 — the AI-contribution policy is a prerequisite, not a nicety

The asymmetry across these three threads is stark and entirely undocumented:

- PR #4 was met with a demand to prove it was not AI-written.
- PR #187 volunteered a careful AI-assistance disclosure ("@Dodothereal is accountable for
  it… will close this PR immediately if this kind of contribution is unwelcome") and was
  closed without a word — which reads, from the outside, as the answer being *yes, unwelcome*.
- The closing comment on #5, the boundary note on #571, and the ordering correction on #571
  are all signed "Generated by Claude Code."

`CONTRIBUTING.md` (139 lines) says nothing about any of this; a repo-wide grep finds no AI
policy anywhere. Whatever the policy is — disclosure required, agent PRs welcome, agent PRs
declined — it must be written before an invitation goes out to a contributor who was told to
prove her humanity and a contributor whose disclosed agent work was closed in silence. An
invitation issued into that vacuum is not a low-cost act of goodwill; it is an unanswered
question sent to the two people best positioned to notice it. This belongs in #828's
prerequisites (or as its own sibling task under #571), and it appears nowhere in CAP-30.

## Reframing 3 — make the credit durable, and the comment becomes a pointer

The #5 comment is strong because it points at `#174`, where credit actually landed as a
commit trailer. A comment is a GitHub artifact; a trailer, a CHANGELOG line, or a CREDITS
entry is part of the project. For #187, the honest and cheap move is to attribute the #182
fix in `CHANGELOG.md:261` and/or amend credit, then let the comment point at it. For #4,
where no code landed, the durable form is different but available: her O(n³)→O(n²)
observation and her quadtree/segment-sort analysis are genuinely good technical content that
belongs in #3's or #17's record as prior art, cited by name. Redefining the unit of work from
"a comment" to "a durable attribution the comment links to" costs nearly the same and
survives the thread.

## Reframing 4 — the highest-leverage name in these threads is not in the plan at all

PR #4's thread contains @bsiever announcing an active teaching fork
(`github.com/bsiever/JLS`) with his own updates, still used in classes — and AmityWilder
mentions she is *already doing bug fixes on that fork*, at the suggestion of two named
faculty (Dr. Kurmas, Dr. Lalejini). That is a live, warm, adjacent maintainer plus a student
pipeline plus a fork carrying real divergence, all discovered inside the very thread this
task is about. CAP-30's recruitment half spends its budget cold-inviting Digital's rejected
PR authors (#571 AC-2), gated behind a 12–20 mw editor refactor. The bsiever fork is warmer,
ungated, in the same educational niche, and appears in no capstone, feature, or task in this
tree. If one 0.25 mw outreach action deserves to exist, it is arguably *that* one — and it
belongs in this task's scope, because it is the same thread, the same week, the same people.

## What I would keep exactly as written

AC-5. "No response is required for this task to be complete — the obligation discharged here
is ours, and their silence is not a failure condition." That is the most mature sentence in
the whole CAP-30 corpus, and it is what makes this task honest rather than transactional.
Keep it verbatim, and resist any later pass that tries to convert it into an engagement
metric.

## Am I disregarding the stated acceptance criteria?

Partly, and explicitly:

- **AC-1/AC-3 stand**, but the inherited "merged-by-other-means" framing from #571 AC-1 must
  be dropped for PR #4; the accurate outcome there is "problem fixed, implementation not
  used." Where the parent and AC-3 conflict, AC-3 governs.
- **AC-2 I would gate**, not on #567/#568 (the round-2 correction on #571 rightly frees AC-1
  from the #316 gate), but on the two things that actually caused the bounces: a published
  AI-contribution policy, and a claim/right-of-first-refusal rule against the automated
  pipeline. Both are cheap. Neither exists. Inviting first and writing them later is the one
  sequencing that can make this task net-negative.
- **AC-4 (link from #571) is bookkeeping** and should not be what makes this pass.

## Verdict

**endorse-with-reframing.** The obligation is real, the cost is near zero, and the instinct
that this is the highest-leverage row in the programme is correct. But the task as filed
treats a systemic failure as a wording problem, prices three comments when one is already
written, describes two structurally different failures identically, and would send an
invitation into an undocumented AI-policy vacuum created by the same pipeline that raced one
of the invitees. Reframe the deliverable as *rule change + durable credit + honest note*,
scope it to PR #4 and PR #187 (PR #5 is done), and add the bsiever fork to the outreach list
while the thread is open.
