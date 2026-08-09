# Issue #679: TASK-C350-4: the same campaign at one worker and at N workers produces a byte-identical aggregate — scheduling is not observable in the result
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C350-4 is one of six tasks (`part_of_feature: 350`) decomposing FEAT-057
(#350, campaign execution and aggregation). Its scope — fold the campaign's
job list in description order, look up each job's result, and assert the
resulting aggregate is byte-identical regardless of worker count or dispatch
order — is mathematically well-posed (#350 §3's commutativity statement,
$A(J;\sigma,w)=A(J;\sigma',w')$, is exactly what criterion 1 tests). But the
issue smuggles in a decision-making obligation the task cannot discharge, omits
a dependency its own parent issue asserts in a same-day comment, and its
acceptance criteria mix testable and untestable claims in ways that let a
compliant-looking PR miss the point.

## Findings, most severe first

### 1. [Critical] Acceptance criterion 4 assigns this task a maintainer-level decision that #350's own newest comment says is escalated, not delegable

Criterion 4: *"the ownership question of #350 Open Question 1 is settled in
writing — this format, or the grading harness's, with the other side
consuming it."*

#350's own comment thread (`issuecomment-5227057942`, posted **2026-08-08**,
the same day as this review) states plainly:

> "Open Question 1 is now the live blocker, and it is a maintainer decision,
> not a dedup one... This is escalated rather than decided. The dedup pass
> does not have standing to reassign ownership of a format between two
> features whose outcomes differ; §7's rule ... requires a maintainer to say
> which is shipping first."

It further identifies the competing claimant as a *large, already-filed*
roster — the CAP-21 grading-harness cluster (#524/#525/#526/#528/#530/#531,
with #697 and #724 doing the identical "dispatch many jobs, order-independent
aggregate" work) — not a hypothetical. Whoever implements TASK-C350-4
(band `1-1.5` maintainer-weeks) cannot unilaterally settle which of two
sibling feature clusters owns a shared report format; that is precisely the
authority the parent's own dedup pass says it lacks. As written, criterion 4
is either impossible to close without a maintainer ruling that lands outside
this task's band and scope, or it will be checked off by unilaterally
declaring #350 the owner — silently overriding the escalation and risking the
exact duplicate-implementation failure mode §7 of #350 was written to
prevent.

**Recommendation:** Strike criterion 4 from this task, or reduce it to
"blocked on: maintainer ruling on #350 Open Question 1 (tracked separately)"
so the task cannot be marked done by a unilateral format decision. The
ownership ruling belongs on #350 or a dedicated decision issue, not buried as
a checkbox on a 1-1.5 mw aggregation task.

### 2. [High] The issue omits a dependency its own parent explicitly asserts

The same 2026-08-08 comment on #350 states: *"`blocked_by: [354, 363]` means
neither #676 nor anything downstream of it can be called done before the
long-run ergonomics (#354) and checkpointing (#363) land."* TASK-C350-4
(#679) is three hops downstream of #676 (local runner) in the DAG
(#676 → #677 → #679). Yet #679's own `ordering_after` list is
`["TASK-C350-3", "TASK-C350-5"]` (#677, #681) only — #354 and #363 are never
mentioned anywhere in this issue's body. An implementer working from #679
alone, without cross-referencing #350's comment stream, will not know this
task cannot be "called done" until #354/#363 land, and will schedule/estimate
against a false picture of readiness.

**Recommendation:** Add `#354`, `#363` (or a note pointing at the #350
comment) to this issue's ordering/blocking metadata directly, rather than
leaving it recoverable only by reading a sibling issue's comment thread.

### 3. [High] Two of four acceptance criteria are not independently testable — they invite a compliant-looking implementation that misses the point

- Criterion 2: *"no code path appends in completion order."* This is a
  claim about the *absence* of a code path, which a test suite cannot
  exhaustively prove — it can be reviewed, but "no code path" is not an
  automatable pass/fail the way criterion 1 (byte-diff two runs) is.
- Criterion 3: *"No aggregate field carries a worker id, thread name,
  dispatch index or wall-clock timestamp."* Testable only up to whatever
  fields the reviewer thinks to enumerate; a field that leaks ordering
  indirectly (e.g., a hash of completion-order-dependent intermediate state,
  or floating-point summation order sensitivity if any numeric fold is later
  added) would not trip an "am I one of these four literal field types"
  check.

Both read as design intentions restated as checkboxes, not verification
procedures. The issue gives exactly one real, mechanically checkable test
(criterion 1); the rest are reviewer judgment calls dressed as acceptance
criteria.

**Recommendation:** Either specify a mechanical check (e.g., static analysis
grep for `completion`/`onFinish`/thread-name fields near the aggregator, or a
property test that runs at 1 and N workers many times and asserts field-level
equality, not just top-level byte equality) or relabel these as "design
constraints verified by code review," distinct from criterion 1's automated
oracle.

### 4. [Medium] Criterion 1 crosses the stated Boundary and duplicates #677's own acceptance criteria, creating ownership ambiguity when the test fails

This issue's Boundary line says: *"Aggregation only. Failure rows come from
TASK-C350-5; the multi-host worker source is TASK-C350-6."* Yet criterion 1
requires diffing not just the aggregate but *"byte-identical per-job
artifacts"* across 1-worker and N-worker runs. Per-job artifact naming and
byte-stability is explicitly TASK-C350-3's (#677) scope — #677's own
acceptance criteria already require "Running the same campaign twice produces
byte-identical per-job artifacts" and that no artifact path varies between
1-worker and N-worker runs. If the shared 1-vs-N integration test fails
because a per-job artifact diverged (an #677 defect), it is unclear whether
that blocks #679's closure, since #679's own Boundary disclaims artifact
storage. This is exactly the kind of boundary ambiguity #350 §7 warns about
at the feature level, reproduced here one layer down between two sibling
tasks.

**Recommendation:** Either narrow criterion 1 to the aggregate bytes only
(per-job artifact identity is #677's criterion to own and close), or make the
shared integration test explicitly co-owned by both issues with a stated
tie-breaking rule for whose defect a failure counts as.

### 5. [Medium] "the shuffle seed is committed" has no requirement that the shuffle be nontrivial or that N-worker execution be genuinely concurrent

Nothing in the criterion requires the committed seed to produce a
permutation materially different from job-description order, nor that the
N-worker run actually exercises concurrent/out-of-order completion (as
opposed to, say, N worker threads that happen to still finish in submission
order on a small fixture). A implementer under schedule pressure could commit
a seed that shuffles trivially (or a fixture small enough that shuffling
rarely changes completion order) and have criterion 1 pass while the
underlying claim — "scheduling is not observable" — remains unexercised in
the one test meant to prove it.

**Recommendation:** Require the committed fixture/seed combination to be
shown (in the PR or a code comment) to actually produce at least one
completion order that differs from job-description order across the 1- and
N-worker runs, e.g. via a logged/asserted completion sequence in the test
itself (not asserted in the aggregate, which is the point, but confirmed once
in a debug/test-only capacity).

### 6. [Medium] Ordering dependency on failure accounting (#681) is asserted but never exercised by this issue's own acceptance criteria

`ordering_after` includes `TASK-C350-5` (#681, failure accounting), and
#350's formalization requires the fold's index set to include failed jobs
(`r(j) ∈ {ok(a(j)), failed(...)}`, denominator = job count per #350 invariant
2). But none of #679's four checkboxes test a campcampaign containing a
failed job — that scenario is pushed entirely to #681's own criteria
("Run a campaign in which a known job fails... $m-1$ successes" is #350's
integration criterion 2, owned by #681 per its Boundary). It is therefore
possible for an implementation to satisfy all four of #679's checkboxes
using a fold that only handles the `ok()` variant of the result type,
deferring (or silently mishandling) `failed()` results, and still pass every
test #679 names — the exact "tempting implementation" failure mode #350 and
#681 both warn about, just shifted one task to the left.

**Recommendation:** Add a criterion here (or an explicit cross-reference to
one on #681) that the aggregation fold is exercised end-to-end against a
mixed ok/failed job set at both 1 and N workers as part of *this* task's
closure, not left solely to #681.

### What is solid

- The core mathematical framing — aggregate as a fold over the description's
  job order with lookup rather than a completion-ordered stream — is precise,
  matches #350 §3's formalization exactly, and is a genuinely good
  architectural constraint to pin down before implementation starts.
- Criterion 1 (the 1-vs-N byte-identity diff with a committed seed) is a
  real, automatable, high-value oracle — the strongest single item in the
  issue.
- The Boundary section correctly declines to re-litigate failure-row content
  or multi-host dispatch, keeping this task's *nominal* scope narrow even
  where (per finding 6) the narrowing isn't fully honored by the criteria.

## Feasibility note

`git grep -c campaign -- src/` at HEAD (`3b6d6ec`) returns no hits: no
campaign, job-description, or aggregation concept exists in the source tree
today. This task sits three levels deep in an unimplemented six-task ladder
(#674 → #676 → #677 → #679), all itself provisionally gated on #354/#363 per
finding 2. The stated `band_mw: 1-1.5` is plausible for the aggregation logic
in isolation, but not for the aggregation logic *plus* settling a
cross-feature format-ownership question (finding 1) that the parent issue's
own maintainer explicitly declined to settle unilaterally the same day this
issue was last touched.
