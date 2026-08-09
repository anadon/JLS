# Issue #350: FEAT-057: many independent runs dispatch across cores or hosts and aggregate into one report whose contents do not depend on how many workers ran
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is a well-structured, evidence-grounded planning document (part of a large
formal "capstone/feature/task" roster) for a campaign runner. Its code claims check
out — `src/jls/sim/BatchSimulator.java` is confirmed 574 lines with the quoted headless
banner at lines 13-15, and `git grep -ic "campaign" -- src/` does exit 1 (no output) as
claimed, so "no campaign concept exists today" is accurate. The design itself (order
independence, injective naming checked at read time, failure accounting that doesn't
shrink the denominator, no-transport-first) is sound in the abstract. The problems are
structural: the issue is currently blocked at its very first task by an unresolved
cross-issue governance question, its dependency graph contains a stated contradiction
about how hard `blocked_by` actually is, its cost is admittedly not an estimate at all,
and its central "byte-identical aggregate" claim is verified by a single seeded test
standing in for a universal quantifier.

## Findings (most severe first)

**1. The critical path is blocked at step zero by an unresolved ownership question, with no owner or deadline.**
§7 states: *"Two independent implementations of 'run many things and collect the
output' is the named failure mode... Whichever of the two ships first owns the job
description and the aggregation format, and the other consumes it... **no resolution is
not**."* Open Question 1 records this as still open and "**Blocks filing the
description scope**." The 2026-08-08 comment (today) confirms it is still unresolved,
now sharpened by a second overlapping claimant (#369/CAP-21's #697, #724), and states
plainly: *"#674 should not land a description format... it is the artifact being
duplicated."* Yet #674 (`TASK-C350-1`, filed 2026-08-04) is explicitly the ordering
root — its own yaml says `ordering_after: []   # first cut; everything else in #350 is
defined over this format` — and its acceptance criteria only require the ownership
question to be answered "before the format is called stable," not before work starts.
That is a live inconsistency between the task's own bar and the maintainer comment's
stronger bar, and neither issue assigns a decision-maker or a deadline. As written, this
feature cannot make real progress without an off-issue maintainer decision that has now
been open six days with two comments both declining to make it.
*Recommendation:* add a concrete decision deadline or an explicit interim rule (e.g.
"first PR touching either format wins, adjudicated by X") rather than leaving "whichever
ships first" as a race condition between two independently-staffed issue threads.

**2. `blocked_by: [354, 363]` is a hard dependency in the machine block but a soft one in prose — and neither blocker has a single filed child yet.**
Global invariant 3 / the DAG treat `blocked_by` as literal ordering ("A must land before
B"). But §7 Open Question 5 says the opposite for #363 (checkpointing): *"restart in the
first landing, with the description carrying a field that later selects resume...
Rides along, but it changes what the first landing can promise."* That is describing
#350 shipping a first landing **without** #363 landing first — directly contradicting
`blocked_by: [363]`. Compounding this, #354 (FEAT-006, long-run ergonomics) lists all
four of its own tasks as "not yet filed," and #363 (FEAT-035, checkpointing) lists all
four of *its* tasks as "Not filed" too — both are themselves unstarted multi-week
features. So the declared hard blockers are two entirely unstarted features, while the
issue's own prose plans to route around one of them anyway.
*Recommendation:* either loosen `blocked_by` to reflect what's actually required for a
first landing (per Open Question 5's own recommended default), or drop the "rides along"
language and commit to the hard block as written.

**3. The cost estimate is admittedly not an estimate — it's a plug figure.**
Quoting the issue directly: *"Sum of this feature's own task rows: 0 wk — there are none
[at time of filing]... the band is part of #312's own marginal arithmetic for its four
new features — 12-20+10-16+10-18+6-8 = 38-62 mw, which is that capstone's marginal band
exactly. So the figure is internally consistent with the capstone that requires it, and
inconsistent with nothing, because there is nothing to be inconsistent with."* This is
circular by the issue's own admission: 6-8 mw was chosen so a parent total adds up, not
derived from the six child tasks it later spawned (#674, #676, #677, #679, #681, #683).
Contrast with sibling issue #354, which reconciles band vs. task-row sum explicitly
("Band 3-5 mw. Task sum 4 wk... The band and the sum agree"), and #363, which flags the
gap when it doesn't agree. #350 does neither — no reconciliation against the now-filed
children's own bands (e.g. #674 alone claims 1-1.5 mw) has been done anywhere in the
thread.
*Recommendation:* once all six `TASK-C350-*` children carry bands, sum them and either
reconcile against 6-8 mw or file the same kind of gap disclosure #363 did — don't leave
an unfalsifiable cost figure as the last word.

**4. The central correctness claim is a universally-quantified statement verified by one seeded test.**
§3 states the requirement as $A(J;\sigma,w) = A(J;\sigma',w') \; \forall \sigma,\sigma'
\in \Sigma,\, \forall w,w' \ge 1$ — a claim over all schedules and all worker counts. §5
criterion 1's evidence plan is: *"Run the same campaign at 1 worker and at N workers with
a deliberately shuffled dispatch order... New test at close-out; the shuffle seed is
committed."* A single committed seed at one (worker-count, shuffle) pair is exactly the
kind of test a completion-order bug (e.g. a `HashMap`/`HashSet`-iteration-dependent
aggregation path, or a thread-pool result that only misorders under specific core
counts) can pass while the general property still fails in the field — this is the
gameable-acceptance-criteria pattern: the letter of criterion 1 is satisfiable without
the theorem it's supposed to stand in for.
*Recommendation:* require randomized/property-based coverage (multiple seeds, multiple
worker counts including 1 vs. a prime N) in CI, not one committed fixture, or explicitly
document the residual risk the single-seed test leaves uncovered.

**5. Integration criterion 5 (resource bounding) is asserted as testable while its mechanism is an explicitly open, unresolved design question.**
§5 criterion 5: *"A campaign description that bounds worker count or memory is honoured,
and exceeding the bound is reported rather than causing the host to thrash."* But Open
Question 3 says: *"How does a campaign bound resource exhaustion at high worker counts?
Options: (a) an explicit worker cap and per-job memory bound in the description,
recommended; (b) rely on the host's scheduler... **Blocks integration criterion 5.**"*
The issue lists this as a real, checkable prediction in §5 while simultaneously
admitting in Open Questions that the mechanism it would check is undecided. A criterion
whose precondition is an open design question isn't yet a criterion.
*Recommendation:* move criterion 5 out of §5 until Open Question 3 is resolved, or
resolve the question in the same pass that files the collection/dispatch tasks.

**6. Ad hoc task-id namespace invented without a recorded governance decision.**
The 2026-08-08 comment introduces `TASK-C350-*` ids for the six newly-filed children,
noting the registry's own task space is "closed at TASK-0112." Using a new C-prefixed
namespace to route around a closed id space is a reasonable-sounding workaround, but
nowhere in #350, #354, or #363 is there a recorded decision about what governs this new
namespace (collision rules against a future official numbering, who else may mint
`TASK-C*` ids, whether other features are doing the same). This is exactly the kind of
undocumented process decision the issue's own rigor (REPLAN comments, mirrored edges,
"no resolution is not") would flag if applied to itself.
*Recommendation:* one line in the registry or in this issue's body naming the `TASK-C*`
convention as a deliberate, documented scheme — not something a future reader has to
infer from a single comment.

**7. Scope overlap with #300/#369 is acknowledged but left as a live seam, not a boundary.**
§ "Intended Audience" claims *"a whole submission set **is** a campaign. The
dispatch-and-aggregate shape is identical"* for #300, while §1's Out of Scope disclaims
owning "the grading harness's rubric, verdicts and counterexamples... the overlap must
be resolved by sharing, not duplication" and the first comment documents that #369
already independently builds "run a directory of submissions, emit per-item results plus
one aggregate" with its own determinism claim (under submission reordering, not worker
count). Two features are both claiming the same worked example (grading a course's
submissions) as their motivating audience while explicitly not having decided which one
owns the shared vocabulary (this is finding #1 restated at the scope level, not just the
task level) — worth calling out separately because it means the *audience* section, not
just the implementation, is currently double-counted across #312 and whatever capstone
#369 serves.
*Recommendation:* once Open Question 1 resolves, prune the losing side's "Intended
Audience" language so the same audience isn't claimed as justification twice.

## What's solid (no action needed)

- Evidence commit claims are accurate: `BatchSimulator.java` line count (574) and the
  headless-construction banner (`:13-15`) both verified against the checked-out tree.
- The "no distributed transport required" in-scope item (§1.5) and Global Invariant 3
  are mutually consistent, not contradictory — a genuine positive given how much of the
  rest of the graph has this kind of tension elsewhere.
- Failure-accounting design (denominator = job count, not success count) directly
  targets the "tempting implementation" failure mode by name and is a good, specific
  anti-gaming criterion in its own right.
- The `blocked_by`/`blocks` mirroring convention (each edge written on both issues) is
  followed correctly for #350↔#354 and #350↔#363 as far as the fetched issues show.
