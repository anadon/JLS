# Issue #332: FEAT-055: a circuit exists as parts that load independently — the single-file ceiling stops being the limit on how large a design can be
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## What holds up

The code citations are unusually precise and I verified every one of them against the current
tree, not just the frozen `evidence_commit`:

- `src/jls/FileAbstractor.java:65` is exactly `static final long MAX_CIRCUIT_TEXT_BYTES = 64L << 20;`
- `:145` is `if (overLimit) {`, `:151-154` is the refusal message, `:307` is the zip-entry
  `contents.length > MAX_CIRCUIT_TEXT_BYTES` check — all match.
- `src/jls/Circuit.java`'s `finishLoad` runs exactly `1300-1422`; `:1345` is
  `LinkedList<WireEnd> ends = new LinkedList<WireEnd>();`, `:1354-1355` is the
  "partition ends into wire nets" `while` loop, `:1369` is `ends.remove(vend);` — the claimed
  O(n) `LinkedList.remove` inside the visited-set walk is real.
- `git grep -rliE "PartitionSet|BoundaryDescription|streamingElaborat" -- src/ test/` still
  returns nothing at HEAD, not only at the pinned commit — the absence claim hasn't gone stale.

The DAG-closure argument is also correct as far as I could check it: #333's `blocked_by` includes
332 and its `blocks` is genuinely `[]`, so the claimed closure (`{#333}`, no path back to #332)
holds today. #319's `blocks` list includes 332, mirroring #332's `blocked_by` entry for 319. The
scope split against #333 (single-process artifact vs. multi-process transport) is real: #332's own
I1/I3 are single-process measurements and #333's IC-1/IC-2 are unstateable without #332's output,
which is the practical test the dedup comment names. The "out of scope" boundaries (automatic
partitioning, cross-partition transport, campaign execution, flat representation, quadratic-load
fix) are each independently filed elsewhere and not silently absorbed.

## Findings, most severe first

**1. The issue is already stale about its own children, and nothing in it says so.**
All five `planned_tasks` scopes this issue lists as "not filed, no id" have in fact been filed:
TASK-C332-1 (#600, part-file set/boundary description), TASK-C332-2 (#601, streaming
elaboration), TASK-C332-3 (#602, boundary net identity), TASK-C332-4 (#604, uncuttable-construct
refusal), TASK-C332-5 (#606, equivalence harness) — all created 2026-08-04T15:08–15:1x, roughly
eight hours after #332's most recent comment (2026-08-04T07:22:34Z), and #332 carries exactly one
comment total (the #332-vs-#333 dedup note), which does not mention any of them. #332's body still
says, in the Decomposition table and in the "no TASK id names any of the five scopes" prose, that
none are filed — and its own Completion Criteria requires "[ ] Each of the five unnumbered scopes
… resolved to a filed issue number by REPLAN:, or descoped", unchecked, with no REPLAN comment
anywhere. #600 has `part_of_feature: 332` in its own machine block, so the link is one-directional
today: the child knows its parent, the parent's tracked state doesn't know the children exist. A
contributor reading #332 in isolation would think five scopes still need filing and would either
duplicate #600-#606 or miss that #600 already carries the concrete AC-1..AC-5 acceptance criteria
for the artifact-form scope. **Recommendation:** before any work starts, post the REPLAN comment
#332's own protocol demands, point `planned_tasks` at #600/#601/#602/#604/#606, and check off that
DoD item — this is a five-minute fix that the issue's own rules already require.

**2. The peak-memory bound leaves the constant unconstrained, which makes I1 gameable.**
§3's formula is `M(load(D)) ≤ max_i M(D_i) + M(B) + c "for a small constant c"`, and I1 says this
is "measured rather than asserted" — good instinct, but the criterion never bounds `c` numerically
or relates it to `max_i M(D_i)`. An implementation that, say, always retains a fixed-size buffer
sized to the largest design class ever seen, or that never releases a prior part's fully-parsed
element list before starting the next (only the *raw* text buffer is freed, not the intermediate
object graph), could still satisfy "bounded by the largest part plus a constant" for any single
test design, because the constant absorbs the leak — it just wouldn't scale, which is the entire
point of the feature and is exactly what the measurement is supposed to catch. **Recommendation:**
pin `c` to something checkable, e.g. `c = o(max_i M(D_i))` as design count N grows, or require the
I1 test to run N ∈ {2, 8, 32} parts of comparable per-part size and assert peak memory stays flat
across N — a single two-part measurement cannot distinguish "bounded" from "a big fixed constant".

**3. #332's own I3 depends on an assumption #332 never states, and only #333 owns checking it.**
The dedup comment says explicitly: "`docs/parity-contract.md:469-477` leaves cross-platform run
determinism **unverified**, and … every byte-identity claim in #333 — and, transitively, #332's
I3 — rests on it." #333's Completion Criteria contains a line item for running that experiment or
restating its guarantees as single-platform; #332's Completion Criteria contains no equivalent
item, and I3 in #332's own table ("Byte-identical output", pinned by "The equivalence harness")
says nothing about platform scope. If #332 closes on the strength of I3 passing on one CI platform
before #333 (or #265) runs the cross-platform experiment, "byte-identical" is unqualified and
could be silently false on a second platform — the exact failure mode #333 was careful to guard
against for itself. **Recommendation:** either add the same platform caveat to I3/§3's equivalence
transformation, or make #332's close explicitly conditional on the same experiment #333 already
depends on, so the guarantee isn't accidentally stronger-sounding in this issue than in its sibling.

**4. The aggregate on-disk budget is left as "checked and reported, not enforced" — a DoS gap the issue itself half-notices.**
Open Question 2 states the per-part cap (`MAX_CIRCUIT_TEXT_BYTES`, unchanged) is settled but the
*aggregate* budget across all N parts of one design "is undecided," with a "recommended default"
of "checked and reported rather than enforced silently." Global invariant 7 says the hostile-input
cap "keeps applying per part, and a part set is not a way to smuggle past it in aggregate without a
declared budget" — but a budget that is reported rather than enforced is not a budget in the sense
issue #38's threat model (cited by README's hostile-input framing and by #319) uses elsewhere: a
design of 10,000 parts at 64 MiB each (a legal per-part size) is 640 GB aggregate, "reported" after
the fact does nothing to stop the load attempt that already happened. **Recommendation:** either
this issue commits to an enforced aggregate cap (even a generous one) before I6 is called done, or
it explicitly defers the enforcement decision to #312 with a named follow-up issue — right now it
recommends the weaker option without flagging that "reported, not enforced" reopens the same
hostile-input class #38 exists to close.

**5. `evidence_commit` is pinned to a branch that issue #493 says will be deleted.**
#332 declares `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`, which #493 (filed
2026-08-03, i.e. after #332) identifies as "a merge commit that exists only on a working branch
which will not be merged and will be deleted" — once gone, the commit-pinned permalinks in #332
404. The good news: #332 doesn't cite any of the seven code files #493 flags as actively
divergent from `master`, and it isn't in any of #493's three "affected issue" lists, so its
`Circuit.java`/`FileAbstractor.java` line citations verified clean against HEAD in this review.
But the label itself is still stale, and #493 explicitly instructs re-pinning citations to
`828822672fc3a8e2cb6da25192472079f04c29dd` rather than leaving the dead commit in place. Low
severity given the content survives, but it's a second, independent piece of evidence (alongside
finding 1) that #332 has not been kept in sync with tracker activity that postdates it.

## Solid, one line each

- The scope boundary against #333 (partitioning vs. transport) is well-drawn and mutually
  consistent on both issues — verified, not just asserted.
- The refusal-before-equivalence-harness sequencing (criterion 5 before criterion 4/I3) is
  correctly justified as the falsification guard, and is echoed consistently in #333's own
  boundary note.
- The four `blocked_by` issues (#319, #336, #353, #370) all exist, are open, and their one-line
  descriptions in #332's §6 match their actual titles/scope.
- Invariant 1/2 (single-file behavior and save output untouched) is a reasonable, testable
  backstop against scope creep into the existing load path.
