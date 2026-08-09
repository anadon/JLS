# Issue #641: TASK-C563-1: a selected combinational region enumerates as a truth table golden-tested against exhaustive simulation
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#641 is a well-scoped task (enumerate a combinational cone into a truth table) sitting inside a
carefully cross-referenced task mesh (#563 → #641/#642/#644/#646, shared extractor #872, consumers
#564/#565). The issue's own comment thread already caught and partially fixed its worst defect (a
dangling `ordering_after: [306]`). That self-correction is good news, but it also demonstrates the
process is fragile — the first correcting comment itself cited a dead commit and needed a second,
same-day comment to fix *that*. Several problems remain live in the issue as it stands today.

## Findings, most severe first

**1. AC-4's fix lives only in a comment, and the issue body still states the original unfalsifiable
criterion.** Body text, verbatim: "AC-4: Multi-bit inputs and outputs are handled with a stated
convention (bit expansion vs. refusal), and whichever is chosen is tested." The 2026-08-08T17:21 comment
itself calls this "unfalsifiable as written: it accepts either answer and so cannot fail," then resolves
it in prose (bit-expansion, MSB-first, matching `TruthTable.makeRowCode` at
`src/jls/elem/TruthTable.java:1064-1074`, confirmed by reading — the loop does put `inputNames[0]` at
the highest bit position). That's a sound decision, but it was never folded back into the AC-4 text
itself. Anyone who reads the issue body (a snapshot, an export, a triager skimming instead of reading
both comments) inherits the original gameable AC. **Recommendation:** edit AC-4's body text to state
the decision directly; don't leave a load-bearing spec decision as comment archaeology.

**2. AC-1's golden-testing story risks circularity.** AC-1: "golden-tested against exhaustive
simulation of the original circuit." The feature's own mechanism for building the table *is*
enumerate-inputs-and-simulate (there is no other way named anywhere in #641/#872/#563 to get an output
column). If the "golden" oracle is produced by running the same enumerate-and-simulate procedure a
second time, a shared bug (bit-order swap, off-by-one on the frontier, wrong don't-care handling) passes
both the implementation and its own oracle. The 17:21 comment confirms `jls.sim.BatchSimulator` exists
and runs headless but does not address independence of the oracle from the code under test.
**Recommendation:** require at least the seed fixtures (the "drawn 4-input combinational circuit") to
carry a hand-computed or structurally-independent expected table (e.g., derived from the gates' known
Boolean functions), not solely a second simulator run through the same harness.

**3. The AC-4 decision unilaterally amends #642 without #642 being updated.** The comment states: "the
input count the bound in #642 is applied to is therefore the expanded bit count, not the signal count,
and #642's arithmetic message must say so." I fetched #642 independently: its body and its only comment
say nothing about bit expansion — AC-1 there just says "the row-count arithmetic" with no mention of
signal vs. bit counting. A requirement was written into #641's comment thread and aimed at a sibling
issue that has not acknowledged it. If #642 is implemented from its own text, the two tasks will
disagree about what N means the moment any bus-width input appears. **Recommendation:** mirror the
bit-expansion decision into #642 itself (the #872 pattern — "STATUS comment on #563 and #306" — models
how cross-issue decisions should be propagated; this one wasn't).

**4. AC-1's own fixture is under-specified given AC-4's resolution.** "A drawn 4-input combinational
circuit" is ambiguous between 4 single-bit inputs (16 rows) and 4 possibly-multi-bit signals (up to
2^(4×width) rows after AC-4's bit expansion) — and neither #641 nor #642 states a concrete bound number
anywhere fetched (#642's boundary note defers the number to "a stated decision recorded with this task"
but #642's body never states one). AC-1 can't be checked for boundedness in isolation from a bound that
doesn't yet exist in writing.

**5. AC-2's "documented" has no named home.** "Enumeration order and column order are deterministic and
documented" — documented where? `docs/simulation-semantics.md`, `docs/batch-interface.md`, a doc
comment, and a help page are all real, distinct documentation tiers in this repo (see
`ARCHITECTURE.md`'s "Documentation" list and the normative-doc convention). As written, a one-line
Javadoc comment satisfies the AC as fully as a normative spec entry would, which is exactly the kind of
gap a minimal implementation can walk through. Name the document.

**6. Process observation, not unique to #641 but visible here:** the ordering fix's first attempt cited
commit `07a0bea` as grounding, which a follow-up comment retracted as "not on master... exists only on
`claude/jls-project-review-505pnf`, the branch #493 says is being deleted," replacing it with
`8288226`. I verified independently: `828822672fc3a8e2cb6da25192472079f04c29dd` **is** an ancestor of
`origin/master` (confirmed via `git merge-base --is-ancestor`), so the correction is right — but the
fact the first pass cited an about-to-be-deleted branch commit as "master" is a hygiene failure worth
naming: other unverified commit/line citations in this task family deserve the same skepticism rather
than being taken on faith.

**7. Minor inconsistency:** the issue's own "Boundary notes" name downstream tasks only by short id
("TASK-C563-3", "TASK-C563-4") with no issue numbers, while the corrected boundary note is careful to
give numbers (#872, #306). Cross-check via #872's body: these are very likely #644 and #646, but #641
itself never says so — low cost to fix, worth doing for anyone navigating from this issue alone.

## What's solid (no rework needed)

- The corrected `ordering_after` (→ #872's cone extractor, not the whole #306 capstone) is right and
  independently verifiable: I confirmed #306's own machine block lists `related: [...]` with no mention
  of 563/641/872, so the original edge really did point at nothing, and #872 is real and does declare
  `blocks: [641, 642, 655]`.
- AC-2 (determinism) and AC-3 (one table, multiple columns, not one table per output) are both
  concretely falsifiable as written and match the shared-frontier model #872 establishes.
- The shared-component boundary discipline ("must not reimplement" the extractor, gaps filed against
  #872/#306) is the right call and consistent with the rest of the CAP-31 family (#563, #565 state the
  identical boundary).

## Verdict rationale

`needs-rework`: the issue's most dangerous defect (dangling prerequisite) is already fixed by its own
comment thread, but that fix path is demonstrably fragile (self-correcting SHA), the fix for AC-4 was
never merged into the criterion text it corrects, AC-1's golden-testing criterion has a real circularity
risk, and the AC-4 decision creates an un-mirrored obligation on #642. None of these block starting
#872 (the actual current blocker), but #641 should not be picked up for implementation as currently
worded without folding the comment-thread decisions into the body and closing the #642 propagation gap.
