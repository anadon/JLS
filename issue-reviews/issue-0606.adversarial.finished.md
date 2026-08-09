# Issue #606: TASK-C332-5: where a design fits both ways, the partitioned form and the single-file form produce byte-identical simulation output
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Context

#606 is TASK-C332-5, the last of five child tasks #332 (FEAT-055,
"a circuit exists as parts that load independently") lists as
`planned_tasks`. I read #332, #600 (part-file set), #601 (streaming
load), #602 (boundary net identity), and #604 (uncuttable refusal) in
full to check #606 against its declared dependency chain, and confirmed
against the tree that none of the partitioned-form machinery exists yet:
`git grep -rliE "PartitionSet|BoundaryDescription|streamingElaborat" --
src/ test/` returns nothing at HEAD, and `WireNet.java` has no name
field of any kind (`ends`, `wires`, `bits`, `hasinput`, `triState` only).
So #606 is entirely prospective — it can only be evaluated as a spec for
work that presumes #600/#601/#602/#604 land first.

## Findings, most severe first

### 1. [High] AC-2's "the signal it belongs to" presupposes #602, which #606 never cites

AC-2 requires the harness to fail "naming the first differing byte and
**the signal it belongs to**." Mapping a byte in simulation output back
to a signal name that means the same thing on both sides of a cut is
*exactly* the deliverable of TASK-C332-3 (#602, "a cut net names the
same signal on both sides ... that name does not depend on which
partition it landed in"). #606's `ordering_after` names only
`TASK-C332-2` (#601, streaming load) and `TASK-C332-4` (#604, refusal)
— #602 appears nowhere in #606's machine block or Boundary notes, and is
reachable only by chasing #604's own `ordering_after` (which does cite
`TASK-C332-3`) two hops away. An implementer who reads #606 in isolation
can miss that AC-2's "signal" clause has no meaning until #602's naming
scheme exists, and could satisfy the letter of AC-2 with any ad hoc
label (e.g. a raw part-local wire index) that "names" the byte without
it being the stable cross-partition identity #332 §3 actually requires.
**Recommendation:** add #602 to `ordering_after` explicitly, and state
that AC-2's "signal" name is #602's stable identity, not a
harness-invented label.

### 2. [High] The interface under test, `sim({D_i}, B)`, is math notation with no cited invocation surface

AC-1/AC-2 test "`sim({D_i}, B)`" against "`sim(D_flat)`", copying #332
§3's transformation formula verbatim. But #332 explicitly scopes the
*format* work to #600 ("this task owns the artifact form only"), and
#600's own ACs are about round-trip *content* equality (parts read back
yield the same elements/nets as the flat form) — nothing in #600, #601,
or #606 specifies how a partitioned design is actually *simulated*: is
it `jls -b` given N file operands, a new flag, or a Java-API-only path
with no CLI surface at all? `docs/batch-interface.md` — the normative
stability contract for what "simulation output" means (stdout format,
exit codes) — has no mention of multi-file/part-set input. Boundary
notes say only "this harness runs both forms in-process," which settles
process topology but not invocation. Without a pinned invocation
contract, AC-1 is satisfiable by a harness that drives the partitioned
form through an internal test-only entry point that no real user (or the
documented batch interface) can reach — passing the letter of the AC
while never validating what "the partitioned form" means to anyone
outside the test. **Recommendation:** either #606 or #600 must pin the
actual invocation surface (CLI flag, file-list argument grammar, or
explicit statement that no CLI surface is in scope yet) before this
harness can be written against something real.

### 3. [Medium] AC-3's "the harness records that dependency in its own output" is unspecified and gameable

AC-3 requires TASK-C332-4's refusal test to be green "before this
harness's results are read as evidence" and that "the harness records
that dependency in its own output." Nothing says *how* that recording is
verified rather than merely asserted — a harness could print a line
claiming the precondition holds without ever invoking or querying #604's
test results (there's no described mechanism: a build-report check, a
shared marker file, a Maven module-ordering guarantee). As written, a
harness that hardcodes `System.out.println("TASK-C332-4: assumed
green")` satisfies AC-3's text. **Recommendation:** specify the check
concretely — e.g., the harness fails closed if it cannot confirm (via a
recorded test-run artifact or an explicit precondition class) that
#604's refusal test executed and passed in the same CI run.

### 4. [Medium] AC-1's "at least one fixture" sets an easily-gamed floor for a claim this consequential

"Byte-identical" is the correctness backstop for the entire feature (per
#332 §2 rejection-4: "the harness runs on designs where both forms fit,
which is precisely the regime where the two can be compared at all").
AC-1 only requires one fixture whose cut crosses nets. A single minimal
two-gate fixture satisfies this literally while exercising none of the
simulation-semantics surface area that a streaming/partitioned load path
could plausibly perturb — sequential elements and edge-triggering,
multi-driver/tri-state resolution on a cut net, or propagation-delay
ordering across the boundary (all normative concerns of
`docs/simulation-semantics.md`, cited by ARCHITECTURE.md as the binding
equivalence criterion for any future second simulation strategy). AC-1
as worded lets the harness go green on trivial combinational fixtures
while a register or tri-state bus crossing a cut silently diverges.
**Recommendation:** require fixtures covering at least one sequential
element and one multi-driver/tri-state net on the cut boundary, not just
"at least one" cut-crossing fixture of any kind.

### 5. [Low-Medium] AC-4's REPLAN mechanism has already lapsed once in this exact feature

AC-4 requires divergences to be "filed as `REPLAN:` on #332." #332's own
Definition of Done includes "[ ] Each of the five unnumbered scopes ...
resolved to a filed issue number by `REPLAN:`, or descoped" — unchecked,
with #332 carrying exactly one comment (the #332-vs-#333 dedup note)
despite all five children (#600-#606, including this one) having been
filed roughly eight hours after that comment. #332's own protocol for
"post a REPLAN when reality changes" has not been followed for the fact
that its children now exist; the same protocol is what AC-4 stakes a
correctness contract on for future divergences. This doesn't block #606
mechanically, but it's evidence the discipline AC-4 assumes has already
gone unexercised once inside this feature.

### 6. [Low] The children's summed band_mw is left unreconciled against #332's own explicit statement that no such sum should be trusted yet

#332 Open Question 3 says the corpus band (10-16 mw) "is **not** a
rollup of task rows, so there is no row sum to reconcile it against,"
and that scopes are "unpriced" until filed. All five are now filed with
bands (#600: 2-3, #601: 3-4, #602: 2-3, #604: 2, #606: 2). The sum
(~11-14 mw) happens to sit inside 10-16, but #332 has not been updated
to state the reconciliation despite the data now existing — a minor
instance of the same staleness as finding 5.

## What's solid

- AC-3's ordering (refusal armed before the harness's results are
  trusted) is exactly mirrored by #604's own AC-4 ("armed and its test
  is green before TASK-C332-5's equivalence harness is trusted") — the
  two issues agree with each other and with #332 §2/§6, verified by
  reading both directly rather than taking either's word for it.
- AC-4's REPLAN framing is a correct, near-verbatim restatement of #332
  §7's own re-planning-protocol bullet ("Two representations of one
  design is a second format surface to keep in agreement. Any divergence
  found between them is a `REPLAN:` trigger here, not a note in whichever
  feature found it.") — faithfully derived, not invented.
- The Boundary notes' scope split against #333/FEAT-056 (cross-host
  transport and the sync barrier are explicitly out of scope; this
  harness runs in-process) matches #332's own explicit "explicitly out
  of scope" list and is consistent with the parallel review of #332.
- Requiring the fixture's cut to "actually cross nets" rather than merely
  exist is the correct falsification instinct — it is #604's own stated
  rationale for the refusal-before-harness ordering, applied here too.

## Verdict rationale

The task's place in the dependency graph and its ordering relative to
#604 are correct and internally consistent with #332 and its siblings.
`needs-rework` because its two central acceptance criteria are not
actually testable against anything concrete yet: AC-2's "the signal it
belongs to" borrows an identity concept from #602 that #606 never cites
as a dependency, and AC-1/AC-2's `sim({D_i}, B)` has no defined
invocation surface anywhere in the four issues that are supposed to
produce one. Both let an implementation satisfy the letter of the ACs
with a placeholder (an invented net label, a test-only entry point) that
has no relationship to the real partitioned-form interface #332
describes. Fix by pinning the missing dependency and the invocation
contract before this harness is written.
