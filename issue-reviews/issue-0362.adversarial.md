# Issue #362: FEAT-030: every retired event costs measurably less and the entire golden corpus stays byte-identical — a structural boot fits a nightly lane without any semantic change
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a feature-tier planning issue (not an implementation issue) that bundles a calendar
queue, a zero-delay closure, a value-representation migration, and a measurement/ratchet gate
behind a hard byte-identity kill criterion (K3). The code-level claims in § 1 verify cleanly
against HEAD (`828822672fc3a8e2cb6da25192472079f04c29dd`): `PriorityQueue`/`HashSet dupCheck`
at `src/jls/sim/Simulator.java:25,27`, the add-then-poll dedup dance at `:167-169` and
`:224-225`, no `CalendarQueue`/`LogicVector`/perf gate anywhere in `src/` or `test/` — all
confirmed. The scope boundary (§1), invariants (§4), and K3 are internally coherent and well
argued. But the issue's *evidentiary basis* — the numbers everything else is computed from —
is broken, and it was already caught by the maintainer's own same-day REPLAN comment. That
comment is thorough but does not fully close the gap it opens, and it introduces at least one
new, unresolved risk of its own (a live mechanism fork with money already spent on the wrong
side). Treating the issue as ready to execute today is not warranted.

## Findings, most severe first

### 1. [HIGH] Every quantitative claim in the issue cites documents and commits that do not exist on this branch
The issue's own "Basis for the band" section names `docs/plan/evidence/capstone-plan.md` and
`docs/machine-calibration.md` at commits `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and
`3a81a4a7d6a0f108ec201e632732d308cc02b3fc`. I verified directly:
```
$ git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7   # MISSING
$ git cat-file -e 3a81a4a7d6a0f108ec201e632732d308cc02b3fc   # MISSING
$ ls docs/plan                                                # No such file or directory
$ ls docs/machine-calibration.md                              # No such file or directory
```
Every number the issue derives its headline claim from — **318 ns/event, 47.7% queue+dedup,
the 2.26x stack factor, the 1.66-1.72h→44-46min conversion, the 12-20mw band** — traces back
to sources that are not reachable from this repository. The Completion Criteria's first bullet
("Every cited evidence document and permalink resolves on the default branch at close") is
already failing at *file* time, not just at close.
**This is not a new finding of mine** — the maintainer's own 2026-08-08 comment (`REPLAN`)
catches exactly this and re-homes most of the figures to
`docs/capability-roadmap/keystone-c-performance.md`, which I confirmed does exist and does
carry matching figures (318 ns/event at line 136, 47.7% at lines 52/193/598, 8,090 cycles/s at
line 138). Good — but two figures have **no surviving home at all**, per the same comment:
`capstone-plan.md`'s S24 band row (12-20 mw, the 2.26x factor) and
`docs/machine-calibration.md:516`'s "MODELED, never measured" claim that is TASK-0023's
(now #379's) entire premise.
**Recommendation:** do not treat the 2.26x figure, the 12-20mw band, or the 44-46min headline
as load-bearing until they are re-derived from a document on master. The REPLAN comment already
says this; it should be elevated into the issue body (not left in a comment) before any child
task cites it, since children copy these numbers forward (e.g. #379's premise).

### 2. [HIGH] The declared critical path is fiction — no task actually encodes the blocking edges
§ 6 states as "necessity": TASK-0023 → TASK-0026 → TASK-0063 → TASK-0064, and separately claims
TASK-0056 can run concurrently with TASK-0063. The REPLAN comment checked the four now-filed
issues (#379, #442, #476, #393) and found the edges exist only in this issue's prose — #393
literally says the link pass promised in its own body never happened ("blocked_by: []" with a
body claiming a future link pass), and #476 lacks the #442 (byte-identity gate) dependency that
§6 calls "necessity under K3." A scheduler or an agent picking up tasks from their own machine
blocks — which is the entire point of this repo's structured-issue system — would be free to
land #476/#393/#879 *before* the gate that is supposed to be able to reject them, directly
inverting the safety argument §6 itself makes ("Landing a speedup and then building the gate
that would have rejected it inverts the entire safety argument").
**Recommendation:** the missing `blocked_by`/`blocks` edges on #379/#442/#476/#393/#878/#879
need to be mirrored before any of those tasks starts work, not deferred to "the link pass" as
an indefinite future action.

### 3. [HIGH] TASK-0056, the value-migration leg this feature depends on, does not exist — it was closed as a duplicate the same day this review runs
The issue's own decomposition table lists TASK-0056 as "planned" work this feature requires,
shared with FEAT-026/FEAT-028. Per the REPLAN comment, #475 (TASK-0056) was closed 2026-08-08 as
a duplicate of #232, and #232's replacement children (#878, #879) were filed the same day by
the same review that flags this. This means: at the moment this issue is read, one of its five
named tasks resolves to a closed, superseded issue, and the reader must already know to
substitute #878→#879 mentally. The issue body itself has not been updated to reflect this — the
`planned_tasks` YAML block still says "TASK-0056 (not yet filed)."
**Recommendation:** update the `planned_tasks` machine block itself, not just a comment,
before treating this feature's task roster as current. A machine block that requires reading a
comment thread to interpret correctly is not machine-readable in the sense the DAG/roster
tooling implies.

### 4. [MEDIUM] Genuine, unresolved mechanism fork between this issue's calendar-queue design and a competing in-tree design — with capital already committed on both sides
`docs/capability-roadmap/keystone-c-performance.md` §7.2 (on master) proposes a *different*
mechanism for the same 47.7% cost center: per-element pending-event slots that must be
sequenced *after* the value-type migration, because its coalescing rule compares values. This
issue's TASK-0063/#476 proposes a calendar queue that is explicitly value-independent and can
run in parallel with the value migration. These are not two descriptions of the same plan —
they're mutually exclusive engineering choices with opposite sequencing consequences, and #476
(the filed calendar-queue task) does not even cite the keystone-c alternative in its Open
Questions. Meanwhile #370 (FEAT-054, the sibling issue this one names as the "funding it twice"
failure mode) already has six filed children, two of which (#846, #848) collide directly with
this issue's value migration and queue-index assumptions.
**Recommendation:** this is a design decision, not a task-sequencing detail, and it should
block #476 from starting implementation, not just ride along as an "open question." As filed,
an agent could correctly execute #476 to the letter and still produce work keystone-c's own
roadmap says must be sequenced differently — which is exactly the kind of acceptance-criteria
gaming the adversarial lens is meant to catch: passing #476's local criteria does not establish
that the *right* mechanism was chosen.

### 5. [MEDIUM] The "no partial credit" kill criterion (K3) is asserted but not made falsifiable at the feature level
§4 invariant 1 and K3 say the entire golden corpus must be byte-identical or "this feature stops
at the failing change... reverted, not documented." But nothing in this issue's own Integration
Criteria (§5) specifies *how* a partial landing is distinguished from a full one at the moment
of failure — e.g., if TASK-0063 (calendar queue) lands and passes the corpus, but TASK-0064
(zero-delay closure) fails K3 six weeks later, is TASK-0063 retroactively suspect too, given §3
states the total-order invariant is only "re-run here after TASK-0064 lands, because the
closure changes what gets posted"? The issue anticipates this in §7 Re-planning Protocol
("TASK-0064 depends on TASK-0063 and is re-planned with it") but doesn't specify what state the
already-merged TASK-0063 code should be left in while TASK-0064 is re-planned — shipped-but-
unexercised code sitting on `master` for an unbounded time is a real interim-state risk this
issue doesn't budget for.
**Recommendation:** state explicitly whether an individually-passing child stays merged while a
sibling is being re-planned, or gets reverted alongside it.

### 6. [LOW] Criterion 4 ("total order preserved... over generated input") is gameable as worded
§5.4 says a property test asserts polled order matches priority-queue order "for the same
input," re-run after TASK-0064 "because the closure changes what gets posted." But the issue
never specifies the generator's distribution (event time range, tie density, seq collision
handling) or a minimum trial count / shrinking requirement. A property test with a narrow or
low-cardinality generator (e.g. distinct times only, no same-bucket collisions) would pass while
missing exactly the swap-adjacent-same-time-bucket bug a calendar queue is most likely to
introduce. The math proof in §3 ("insertion order within a time bucket is ascending s") is sound
*if* the implementation actually appends within a bucket — but that's precisely the invariant
the property test needs to stress with many same-time events, and the issue doesn't say it will.
**Recommendation:** the eventual TASK-0063/#476 acceptance criteria (not necessarily this
feature issue) should specify generator density for same-(t) collisions explicitly, since that's
the only case where the calendar-queue and priority-queue orderings could plausibly diverge.

### 7. [LOW] Scope-boundary self-contradiction risk between "no semantic change" and "zero-delay closure"
§1 lists "collapse events that model no elapsed time" as part of the semantics-preserving stack,
and §4 invariant 3 says "per-element propagation delays are observably unchanged." Collapsing a
chain of zero-delay events into a straight-line sweep is very plausibly semantics-preserving for
*final* values, but changes the event *count* and *ordering granularity* mid-chain — which
directly touches invariant 2 ("total event ordering is preserved exactly... asserted over
generated streams") for any observer that watches events at zero-delay granularity (e.g. a
probe on an intermediate net in a purely-combinational zero-delay chain, per `probeSample` in
`Simulator.java:285-287`). The issue's own §5.2 requires events-per-clock-cycle to stay an exact
equality — but if the zero-delay set removes events that would otherwise land in the queue, the
event *count* for a design containing zero-delay elements necessarily changes, which appears to
directly conflict with "unchanged, as an equality" unless "events" is implicitly scoped to
exclude the collapsed ones. The issue never states this scoping.
**Recommendation:** §5.2's equality claim should explicitly state whether zero-delay-collapsed
events count toward "events per clock cycle," since as worded the criterion and the closure
mechanism appear to be in tension.

## What's solid (one line each)
- The code-level "absent at evidence commit" claims in §1 all verify against current HEAD via direct grep/`git cat-file` checks — accurate, falsifiable, well-cited.
- The scope boundary correctly excludes the compiled/cycle-based strategy reopening #221, correctly separates FEAT-054's capacity work from this feature's throughput work, and correctly refuses to let FEAT-030 gate on all of FEAT-026 rather than just the shared task.
- §4's invariants and K3's "no partial credit, revert not document" stance is exactly the right level of strictness for a stack that touches the simulation kernel.
- The Related Issues section is honest about overlap with #232 rather than silently duplicating it.
- The maintainer already ran essentially this same adversarial exercise same-day (the REPLAN comment) and caught findings 1-3 independently — a genuinely good sign for how this tracker self-corrects, even though the correction lives in a comment rather than the issue body yet.

## Bottom line
The architectural reasoning is sound and the scope boundary is disciplined, but the issue's
numeric foundation is currently unsourced on this branch, its declared critical path is not
enforced by the machine blocks of the tasks it spawned, one of its five named tasks is already
stale, and a live, uncited mechanism fork exists between this issue's calendar-queue approach
and a competing in-tree roadmap document. None of these are fatal to the *idea* of FEAT-030, but
none of them should be considered resolved by the existence of a REPLAN comment — the issue body
itself (machine block, § Cost citations, § 6 sequencing) needs to be updated to match before a
child task's completion can be trusted to mean what this issue claims it means.
