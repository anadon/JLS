# Issue #649: TASK-C564-2: above the stated bound the minimizer refuses with the prime-implicant growth as arithmetic — never a heuristic answer, never a hang
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The design intent — exact-or-refuse, no silent heuristic degradation, refusal enforced before the
exponential work starts — is sound and matches the kill-criterion it cites (CAP-31 KC-31-1, #515).
The problems are all in what the issue fails to actually specify or reconcile, not in the goal.
None of these are fixed by writing code; they need the issue text corrected first.

## Findings, most severe first

**1. (High) The boundary note claims the bound's numeric value is "recorded here," but no number
appears anywhere in the issue.** Quoting the issue in full: *"The bound's numeric value is recorded
here and must be reconciled with #563's extraction bound and #565's table-entry bound."* There is no
digit anywhere in the body. AC-1 ("above it the tool refuses") and AC-2 ("a table at bound+1 refuses
within a fixed time") both require a concrete N (or minterm count) to even be written as a test. As
filed, this issue cannot be implemented or closed without an edit nobody has made — the sentence
promises an artifact the issue doesn't contain.
*Recommendation:* add the actual number and its derivation (e.g., worst-case prime-implicant count
at N inputs, chosen against acceptable wall-clock/memory) before this task is picked up.

**2. (High) Circular, unresolved ownership conflict with #642 over who records the number.** #642
(TASK-C563-2, the enumeration-bound sibling) carries near-identical language: *"The bound's numeric
value is a stated decision recorded with this task; #564 and #565 must use the same number or state
why theirs differs."* So #642 and #649 each currently assert that *it* is the canonical source of the
number — and neither contains one. Worse, the fleet's own review comment on #642
(2026-08-08T16:18:03Z) claims *"#642 and #652 carry no matching sentence"* to #649's reconciliation
clause — but #642's live body text, quoted above, does carry a matching sentence. The tracker's own
account of this asymmetry is itself stale or wrong. There is no working single source of truth for
the bound anywhere in the issue family today.
*Recommendation:* pick one task (#642, since enumeration is upstream of minimization) as the sole
owner of the numeric decision; strip the duplicate "recorded here" claim from #649 and make #649
reference #642's number (or its own independently-derived, differently-named one — see #3).

**3. (High) #649's own reconciliation instruction is contradicted by analysis already sitting on a
sibling issue.** The 2026-08-08 comment on #642 concludes: *"Exact minimization is exponential in
prime implicants, which is a function of the table's content, not only its width: two tables of
identical size can sit on opposite sides of #649's wall... one number can be a floor for all three but
cannot be the correct wall for any two."* That is a direct rebuttal of #649's boundary note, which
instructs reconciling this task's bound with #563's (input-count/2^N) and #565's (table-entry) bounds
as if they should converge. #649's text has not been updated to reflect that its own family has since
determined a shared number is architecturally wrong for this specific task. Executing #649 as
literally written risks forcing minimization's genuinely different complexity wall to match
enumeration's, either making the bound too permissive (accepting inputs that blow up in PI count) or
too conservative (refusing content-simple tables the extractor would happily enumerate).
*Recommendation:* rewrite the boundary note to state minimization's bound is independently derived
(as the #642 comment argues it must be) and drop "must be reconciled" in favor of "must be no more
permissive than #563's / #565's, and the difference documented."

**4. (Medium) AC-1/AC-4 assume GUI and batch surfaces that #649's own `ordering_after` does not
require to exist, and the GUI side never links back.** AC-1 requires the bound to be "stated wherever
minimization can be invoked," and AC-4 requires the refusal to be "identical whether reached from the
GUI or from the batch flag." But #649 declares `ordering_after: ["TASK-C564-1 (the minimizer this
bounds)"]` only — no dependency on the GUI display task (#650, TASK-C564-3) or the batch-flag task
(#651, TASK-C564-4). Checking those two: #651 correctly lists #649 as a prerequisite, so the batch
half of AC-4 is sound. #650 does not — its `ordering_after` is `["TASK-C564-1","TASK-C563-3"]`, with
no edge to #649, even though #650's own AC-4 says *"TASK-C564-2's refusal appears in this surface with
its arithmetic."* Nothing in the graph forces #650 to actually wire in #649's refusal, so AC-1's "wherever
minimization can be invoked" could be satisfied by a single internal API call site at the time #649
lands (since #650 hasn't necessarily consumed it yet), making the criterion pass in a technical sense
while the student-facing GUI never shows the bound until #650 catches up, if it ever does.
*Recommendation:* add #649 to #650's `ordering_after`, or make #649's AC-1 explicit that it applies
only to whatever invocation surfaces already exist at execution time.

**5. (Medium) "the prime-implicant growth as arithmetic" names a concept but never specifies what is
actually displayed, and the enforcement timing is at risk of contradicting itself.** Compare sibling
#642's refusal, which is fully specified: *"2^N rows for this selection, N inputs, this many rows."*
#649 gives no analogous formula (e.g., a worst-case prime-implicant bound such as O(3^N/N)) for what
text a student would actually see. That gap also creates a feasibility trap: the Outcome text says the
bound is "enforce[d]... before work begins," but if an implementer reads AC-1's "naming the
prime-implicant growth arithmetic" as requiring the *actual* PI count (only knowable by running most
of Quine–McCluskey), the enforcement can no longer happen before the exponential work starts — the two
requirements only coexist if the shown number is a closed-form worst-case formula, and the issue never
says so.
*Recommendation:* state explicitly that the displayed quantity is a worst-case formula over N (or
minterm count), computable in closed form, not a measured PI count.

**6. (Medium) "input/minterm bound" is ambiguous about which quantity is actually checked.** Sibling
#642's bound is purely a function of input count N (2^N row growth). Minimization's real cost driver
is the number and bit-pattern structure of ON/DC minterms, not N alone — two N-input tables can sit on
opposite sides of the wall depending on content (this is exactly what the #642 comment argues, see
Finding 3). The issue's own phrase "input/minterm bound" reads as if these are interchangeable, without
saying whether the gate is on N, on minterm count M, or on both, leaving the actual trigger condition
for refusal undefined.
*Recommendation:* pick one gating quantity (or an explicit combination) and state it plainly.

## What's solid

- AC-2 ("a table at bound+1 refuses within a fixed time; no run is started and abandoned") is
  well-formed, cheaply testable, and mirrors #642 AC-2's pattern for the sibling enumeration bound.
- AC-3 ("every returned expression is minimal or absent, with no third state") is a sound, cheaply
  testable structural/type-contract invariant, correctly scoped away from proving actual minimality
  (that burden sits on #648 AC-1 and #651 AC-1/AC-2's differential goldens, not here).
- The kill-criterion citation (CAP-31 KC-31-1, #515: "no heuristic minimizer... gets built in v1") is
  accurately quoted and the Outcome text ("a heuristic answer presented as minimal is worse than no
  answer") is a faithful restatement, not an overclaim — unlike the miscitation pattern found in #659.
- `ordering_after` correctly names TASK-C564-1 (#648) as the only true prerequisite for the
  minimizer-core half of this task.

## Verdict rationale

The core policy (exact-or-refuse, enforced before exponential work, no third state) is worth building
and is internally consistent. But the issue promises a numeric bound it does not contain, conflicts
with a sibling issue over who owns that number, and that sibling's own later analysis already
concludes the reconciliation #649 asks for is the wrong approach — plus a real ordering gap (#650) that
lets AC-1/AC-4 be satisfied without the GUI ever actually showing the refusal. These are
issue-text defects, not implementation difficulty, and should be fixed before anyone starts on this
task: **needs-rework**.
