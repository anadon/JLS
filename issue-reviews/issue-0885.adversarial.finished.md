# Issue #885: TASK-C880-3: the separation is reported as two distributions and their overlap, and KC-25-1 gets its written answer on #506
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

#885 is the third of three tasks under #880 (FEAT-C25-0), itself the sole filed
work under the deferred capstone #506 (CAP-25, schematic similarity detection).
#883 builds a 30-submission synthetic corpus with 3 planted copied pairs; #884
scores all 435 pairs deterministically; #885 (this issue) reports the two score
distributions, states the overlap as a number, and posts a binary verdict on
#506 against kill criterion KC-25-1. Nothing in the tree implements any of
this yet — `test/fixtures/similarity-corpus/` does not exist, and #356 (the
canonical-form lineage #884's erasure layer is required to sit "on", not
beside) has zero code (`git grep` for `SemanticCheck`, `sref`/`sprobe`, or a
`validate()` method on `Circuit.java` all return nothing, per #356's own §1).

## Findings, most severe first

**1. AC-3's pre-registration requirement is unenforceable by construction — and, worse, contradicts #884's own acceptance criteria.**
AC-3 requires "what counts as separation is written down **before** the scores
are looked at, and committed in the same change as #884's scoring code or
earlier." But #884's acceptance criteria (which is the actual checklist its
implementer will work from) contain no such obligation — in fact #884's AC-6
says the opposite: *"no threshold is chosen, computed, or hinted at. Not in
code, not in a constant, not in a comment."* Whichever quantile or overlap
rule #885 needs pre-registered ("relevant upper quantiles", AC-1) is exactly
the kind of separation criterion #884 is explicitly forbidden from touching.
So either (a) #884's implementer, with no AC obligating them to do so,
happens to also write the pre-registration file in the same commit — pure
luck — or (b) #885's implementer writes it retroactively once #884's scores
already exist, which is precisely the "choosing the criterion after seeing
the numbers" failure mode AC-3 exists to prevent, and which no test, CI
check, or git hook in this repository (or proposed by this issue) can
detect. There is no mechanism — commit-order review is not scriptable against
"did the author privately look at the numbers first." This criterion is
untestable/gameable as written.
*Recommendation:* Either fold a literal, numeric pre-registration criterion
into #884's own AC list (so it's owned by the task with the code change), or
have #885 add an explicit AC requiring a *dedicated* commit, authored and
merged before #884's scoring command is first run at the target commit, with
the commit hash cross-referenced in the final report — something a reviewer
can actually check.

**2. No contingency is defined for the case where #884 cannot produce scores at all.**
#884's own Boundary says: "If the shared form cannot carry erasure, this task
stops and reports that, and CAP-25 re-sequences behind #356 rather than
forking" — i.e., #884 has a defined "discharge without scores" outcome given
#356's current state (no canonical form exists yet in the tree — confirmed
above). #885 is `blocked_by: [884]` and its whole AC-1/AC-4/AC-5 apparatus
presumes two score distributions exist to report on. Nothing in #885 states
what #885's own AC-5 verdict should say if #884 discharges with "canonical
form not ready" instead of a score table — is that a third, undeclared
outcome on #506, contradicting AC-5's "no third outcome, no hedge"? Given
that #356 is currently not built to the point of carrying erasure (its own
three prerequisite tasks TASK-0005/0031/0032 are, per #356's status table,
not yet filed as of this issue's creation), this is not a hypothetical edge
case — it is the more likely near-term outcome of the chain, and #885 is
silent on it.
*Recommendation:* Add an explicit AC or Boundary clause: if #884 discharges
without a score table, #885 discharges by posting that fact to #506 (a third,
named, legitimate outcome distinct from the two AC-5 permits), rather than
leaving the executor to invent behavior.

**3. "The relevant upper quantiles" (AC-1) is not a specific, falsifiable metric.**
AC-1 requires "maximum and the relevant upper quantiles of the independent
set" and "whether the intervals intersect" — but never names which quantile
(90th? 95th? 99th? multiple?) is the one that decides AC-5's binary verdict.
With only 3 planted-pair data points and 432 independent pairs, the choice of
quantile is exactly the kind of researcher-degree-of-freedom that determines
whether the report reads "no threshold separates these" or "separation
achieved" — and AC-3's pre-registration (see finding 1) is supposed to pin
this down but the issue never specifies what the pre-registered document
must actually contain. A pre-registration that says "we'll use the relevant
quantile" pre-registers nothing.
*Recommendation:* Name the exact statistic in the issue itself (e.g., "the
independent set's 100th percentile [max] against the planted set's minimum")
so AC-3's pre-registration has a concrete target to commit.

**4. Internal contradiction risk between #885 AC-3 ("committed... or earlier") and #883/#884's actual scope.**
#883 (the corpus task) and #884 (the scoring task) both have their own closed
AC lists with no mention of a separation/threshold pre-registration document.
If AC-3's file needs to land "in the same change as #884's scoring code **or
earlier**," the only realistic "earlier" candidate is #883 — but #883's
Boundary explicitly says "No scoring, no fingerprinting, no canonicalization
here. Those are #884" and says nothing about pre-registering a separation
rule either. So there is currently no task in the four-issue chain
(#880/#883/#884/#885) whose AC list actually obligates anyone to write the
pre-registration document AC-3 requires to exist. It's a requirement that
belongs to no one.
*Recommendation:* see finding 1's recommendation — assign ownership
explicitly to one of the three prerequisite tasks, or to #885 itself with an
added blocking sub-step that happens *before* #884 is allowed to run.

**5. Feasibility: band_mw "0.5" looks tight against what AC-4's contingency clause actually demands.**
AC-4 says if the null side is degenerate, the report must state "the smallest
scale at which the measurement *would* be meaningful (#880 KC-25-0-1)" — that
is a power/sample-size argument, not a one-line observation, and doing it
honestly (rather than as a hand-waved guess) is a nontrivial piece of
statistical reasoning layered on top of writing two distributions, computing
an overlap, checking AC-3's pre-registration was actually satisfied, and
posting a two-outcome GitHub comment. 0.5 mw (roughly two days) is plausible
only if the "smallest meaningful scale" clause is never triggered; the issue
doesn't flag that its own band assumes the non-degenerate branch.
*Recommendation:* Either widen the band or scope the degenerate-case writeup
to something bounded (e.g., "one sentence citing a standard rule of thumb,
not a derived number") so the AC and the cost estimate agree.

**6. Chain feasibility: the task presumes #356 will be "ready enough" on a timeline this issue does not control.**
#885 sits at the end of a chain (`#506 -> #880 -> #883 -> #884 -> #885`) whose
middle link (#884) is `ordering_after: [356]`, and #356 is a large, actively
contested feature (9-13 mw band, its own band-vs-task-sum discrepancy flagged
as an open question, blocked by #319 and #334, both open) whose three
prerequisite tasks are unfiled as of this issue's own creation date
(2026-08-08). #885's existence as a "0.5 mw, nearly done" task is only true
in the branch where #356 is far enough along — which is not the state of the
repository today. The issue doesn't surface this dependency risk itself
(it appears only in #884 and #880's text); a reader of #885 alone would not
know the entire chain is currently stalled on an unrelated, much larger
feature.
*Recommendation:* Either note the #356 readiness risk directly in #885 (even
though it's inherited), or accept that #885 cannot be picked up in isolation
and should carry a visible "blocked on #356 readiness" flag rather than
reading as shovel-ready.

## What's solid

- AC-2 (declaring "no threshold separates these" as a legible pass, not a
  failure to rerun) is a genuinely good discipline against p-hacking via
  corpus growth, and is reinforced consistently across #880/#883/#884/#885.
- AC-4's insistence that n=3 cannot support a confidence claim, and its
  explicit naming of "silently growing the corpus to rescue the result" as
  the antipattern, correctly pre-empts the most likely form of quiet scope
  creep in this chain.
- AC-6 (no verdict vocabulary, fixtures not people) is consistent with the
  parent capstone's KC-25-3 and is adopted early rather than retrofitted —
  a reasonable engineering choice given #506's own explicit ethics section.
- The `related` links (#506, #300, #883) are all verified open and
  consistent with the machine block; no stale references found.
