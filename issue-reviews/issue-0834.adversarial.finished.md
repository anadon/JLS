# Issue #834: TASK-C333-3: a partition advances its clock only as far as its peers' committed time and lookahead allow, so no committed simulation time is ever un-committed
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#834 is the barrier-advance-rule child of FEAT-056 (#333, distributed
partitioned simulation). It is open, well-scoped in isolation, and the
conservative-synchronization inequality it states (`t ≤ min_j(T_j + L_j)`)
is a textbook-correct formulation. The problems are (1) two of its five
acceptance criteria cannot presently be executed against the real system
because the machinery they depend on does not exist and is not disclosed
as a dependency in this issue, and (2) two of the criteria as literally
worded can be satisfied without the capability actually working.

## Findings, most severe first

### 1. [High] AC-3 depends on infrastructure that does not exist and is not on this issue's declared dependency list

AC-3 requires: *"a two-partition run over the loopback produces watched
output byte-identical to the same design run whole."* That requires an
actual partitioned circuit (parts + boundary description) plus boundary-event
transport. Neither exists in the tree:

```
$ git grep -rliE "PartitionSet|BoundaryDescription|streamingElaborat" -- src/ test/
(no output)
$ find src/jls test -iname "*partition*"
(no output)
```

FEAT-055 (#332, "a circuit exists as parts that load independently") is the
feature that would create this, and its own five child scopes are listed as
*"not filed, no id"* with an explicit warning that its cost band is
*"unvalidated by decomposition."* TASK-C333-2 (#832, the frame/drain path)
that #834's own `ordering_after` names as a prerequisite is itself still
open and, per its own AC-5, deliberately ships **no** advance rule or
barrier — i.e. even the thing #834 says it depends on hasn't landed. #834
is therefore not "ready" work; AC-3 cannot be verified until at least two
unlanded, partially unscoped features (#332 and #832) land first.

**Recommendation:** either (a) mark #834 blocked on #832 (and transitively
#332) explicitly rather than only naming #832 in a prose `ordering_after`
note, or (b) rewrite AC-3 to use a synthetic two-"partition" test double
(matching AC-1's apparent scope) and move the real two-partition/loopback
byte-identity claim to TASK-C333-5 (#838), which is the invariance-suite
task the parent feature already assigns that responsibility to.

### 2. [High] AC-3's dependency on FEAT-014 is known to the parent feature but omitted from this issue's own scope notes

Feature #333 says explicitly: *"FEAT-014 before criterion 1's 'watched
output' is well-defined. A watched element inside one partition needs a
name that does not depend on which partition it landed in; proceeding
without it means the watch expression changes with the cut."* #834's
"Boundary notes" section names exactly one out-of-scope dependency
(TASK-C333-4, lookahead value/refusal) and is silent on FEAT-014 (#318).
A contributor who scopes their work from #834 alone — which is the whole
point of splitting a feature into tasks — has no way to learn that AC-3 is
unstateable without #318, and could spend the 4-6 mw band on this task and
then discover AC-3 is blocked by something the issue never mentioned.

**Recommendation:** add FEAT-014 (#318) to the boundary notes alongside
TASK-C333-4, the same way #333 does at the feature level.

### 3. [Medium-High] AC-1 and AC-3 test different things, and only the infeasible one (AC-3) forces real integration

AC-1 ("a test holds one peer back and asserts the other stalls at the
bound") is phrased generically enough to be satisfiable with two mock
clock/lookahead objects that never touch `jls.sim.Simulator`
(`src/jls/sim/Simulator.java` currently has no partition or committed-time
concept — confirmed by reading the class: single `now` field, single
`eventQueue`, single-threaded event loop). AC-3 is the only criterion that
would force the barrier rule to actually gate two real simulator instances,
and per Finding 1 it cannot be run today. That leaves a real path where
this task "lands" against AC-1/AC-2/AC-4/AC-5 with a self-contained barrier
class that is never wired into `Simulator`, and AC-3 is quietly waived or
deferred — exactly the outcome the issue's own acceptance criteria are
supposed to prevent.

**Recommendation:** AC-1 should specify that "peer" means an actual
`Simulator`-integrated commit-time/lookahead pair (or explicitly declare it
a unit-level test on the barrier class alone, with the integration claim
moved to AC-3/TASK-C333-5 and AC-3 correspondingly rewritten per Finding 1).

### 4. [Medium] AC-1 asserts safety only, not liveness — a permanently-stalled partition passes

*"a test holds one peer back and asserts the other stalls at the bound
rather than overshooting it"* — this only forbids overshoot. An
implementation that never advances any partition's committed time past 0
(e.g. a broken or deadlocked barrier) trivially satisfies "does not
overshoot" while providing none of the claimed capability.

**Recommendation:** AC-1's test should also assert the free partition
*reaches* `min_j(T_j + L_j)` once that bound becomes known — not merely
that it never exceeds it.

### 5. [Medium] AC-2's grep-based criterion is a lexical proxy, not a behavioral one

*"`git grep -inE "cancel|withdraw|rollback" -- src/jls/sim/` still returns
nothing at the landing commit"* — confirmed currently true (`exit 1`, no
matches) — but this checks for three specific English tokens, not for the
absence of rollback-shaped code. A future implementer under pressure to fix
an over-advanced clock could add a method named `restoreCheckpoint`,
`rewindTo`, `resetToBarrier`, or `undoAdvance` and this AC would still pass
while the "no committed time is ever un-committed" invariant it exists to
protect is silently broken. This weakness is inherited from the parent
feature (#333 uses the identical grep as its own criterion 3 evidence) so
it isn't unique to #834, but #834 is where the actual advance-rule code
lands, making it the task where the gap matters most.

**Recommendation:** supplement the grep with a structural/architecture test
(e.g., no method assigns a partition's committed-time field to a value less
than its current value — comparable in spirit to
`ArchitectureRulesTest.socketEndpointsAreConfinedToCollabNet`) rather than
relying solely on vocabulary absence.

### 6. [Medium] No reconciliation with the recorded single-strategy decision in ARCHITECTURE.md (#221)

`ARCHITECTURE.md` records: *"the `jls.sim.Simulator` event-queue interpreter
remains JLS's **only** simulation execution strategy... Revisit trigger: a
concrete CPU-scale design... that is unusably slow interactively... a
levelized compiled pass as a second strategy"* — i.e. the one sanctioned
"second strategy" is a compiled/levelized evaluation pass, not a
multi-process conservative-synchronization scheme. #333/#834 add a
materially different execution shape (independent per-partition clocks,
cross-process barrier exchange) without citing or distinguishing it from
#221. #333 §3 does say *"each partition keeps the existing single
simulation thread"*, which is plausibly how this stays compatible (it's
orchestration around N ordinary interpreters, not a new per-event
evaluation strategy) — but neither #333 nor #834 states that argument
explicitly, so a later auditor comparing the two "recorded decisions" would
have to reconstruct the reconciliation themselves.

**Recommendation:** add one sentence to #834 or #333 explicitly
distinguishing "N ordinary interpreters plus cross-process barrier
orchestration" from "a second simulation execution strategy" under #221's
terms, so the two recorded decisions don't read as contradictory in the
issue tracker.

### 7. [Low-Medium] The barrier-vs-null-message decision is asserted as settled with no visible resolution trail

#834's Outcome states the barrier-synchronous shape is *"chosen over
null-message"* as settled fact. But #333 §"Open Questions & Decisions
Needed" #1 still lists *"Which conservative algorithm? ...Recommended
default: barrier-synchronous... **Blocks filing the protocol child**"* with
no comment/`REPLAN:` on #333 recording that the recommended default was
adopted. #834 was filed ~8 hours after #333's last update, so the decision
was plausibly made off-issue, but the issue trail itself doesn't show it.

**Recommendation:** add a short `REPLAN:`-style comment on #333 recording
that Open Question 1 resolved to barrier-synchronous, so #834's Outcome
isn't the only place the decision is visible.

### 8. [Low] AC-4's "observable point" is underspecified

*"the barrier is a named, observable point at which every partition's
committed time is equal, exposed as a testable property here"* — no
concrete surface is named (a method, an event, a log line?), unlike sibling
task #832's AC-3 which is concretely testable ("a test asserts no foreign
thread posts into a simulator"). As written, an internal test-only hook
visible to nothing but this task's own test suite would satisfy the letter
of AC-4 while providing nothing TASK-C333-6 (#839, checkpoint coherence)
could actually consume.

**Recommendation:** name the concrete observable, e.g. a per-partition
`committedTime()`/`atBarrier()` accessor, so #839 has something real to
depend on rather than "assumed."

## What's solid (no rework needed)

- The core inequality (`t ≤ min_{j≠i}(T_j + L_j)`) is the standard,
  correctly-stated conservative/CMB-family barrier bound.
- The boundary-note split between "consumes a lookahead it is given" (this
  task) and "the lookahead value and low-lookahead refusal" (TASK-C333-4,
  #836) is clean and matches TASK-C333-4's own scope as filed.
- AC-2's grep is currently empirically true against the repo at the time of
  this review.
- The dependency on TASK-C333-2 (#832) for "the frame and the drain path"
  is correctly identified as a prerequisite, even though (per Finding 1) it
  understates how much else is missing above it.
