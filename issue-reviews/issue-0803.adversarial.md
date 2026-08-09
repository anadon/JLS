# Issue #803: TASK-C592-2: each catalog row carries a funding score, a named acceptance vehicle and a stop-loss column — and the timed counter task gets its "before"
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue actually is

TASK-C592-2 (#803) is the second half of FEAT-C37-1 (#592): TASK-C592-1 (#802,
`ordering_after` dependency, confirmed open) publishes the bare ergonomics
parity catalog (rows, citations, HAVE/GAP/REFUSE grades); #803 adds a funding
score, a stop-loss column, an acceptance-vehicle name per row, and a timed
4-bit-counter baseline that CAP-37 AC-4 (#521) needs as its "before." The
dependency chain (#803 → #802 → #592 → #521) is internally consistent and the
sequencing is sound. The problems are in what #803 asks for, not whether it
should exist.

## Findings, most severe first

**1. AC-2 names a closed issue as one of only two valid acceptance vehicles.**
AC-2 requires every row to name "#91 harness or #441 headless interaction
machine." I fetched #441: it is **closed, `state_reason: duplicate`**, closed
2026-08-08 — the day before this review and four days after #803 was filed
(2026-08-04). Its own closing comment says plainly: *"Superseded by #84
(feature/task deduplication)... Lower number wins, so #84 survives and this
one closes into it."* Anyone executing #803 today who names "#441" per the
letter of AC-2 is citing a dead, redirected issue number; the real target is
#84. This is a live stale-reference bug, not a hypothetical one — recommend
fixing AC-2 to say "#91 harness or #84's headless interaction-machine
extraction" before work starts, and re-deriving the citation at pickup (the
sibling tasks in this same generation, e.g. #441 itself, already carry a rule
for re-verifying citations at execution time — #803 should inherit it
explicitly).

**2. AC-1 silently universalizes a stop-loss rule that its own source scopes to PF-5 only.**
AC-1 says "the per-item 1.5x stop-loss is expressed as a column" for
**every** row, citing KC-37-2. But KC-37-2, quoted verbatim from #521:
*"PF-5 items fund strictly in catalog-score order with a per-item 1.5×
stop-loss."* It is a PF-5-only kill criterion (the "small-parity long tail"
feature #596). #592's own AC-3, which #803 is supposed to satisfy, is more
careful — *"PF-5's stop-loss rule (KC-37-2, per-item 1.5x) is expressed as a
column"* — but #803's paraphrase drops the "PF-5's" qualifier and applies it
to "every row," including PF-2 (compound selection, #593), PF-3
(findability, #594) and PF-4 (message quality, #595) rows that KC-37-2 never
mentions. A 1.5x stop-loss multiplier has no defined meaning for those rows
(1.5x of what baseline estimate?). Executed literally, this either produces a
column full of meaningless values for 3/4 of the catalog or forces the
executor to silently narrow the criterion back to PF-5 — in which case the
issue text is simply wrong and should say so.

**3. A funding-score requirement (AC-1) collides with the maintainer's own recorded finding that at least one row cannot yet be scored.**
The #592 comment thread (dedup pass 2, 2026-08-04) found that the "wire
coloring" row (Digital #1308) "has two readings with different owners" (state-
encoding, owned by #542/#76, vs. user-assignable per-wire colour, which is
new `.jls`-format scope no issue owns) and concluded: *"Neither reading can
be scored from the row as currently described... Until then #596's AC-1...
correctly blocks it."* #803's AC-1 requires "every row" to carry a funding
score with no carve-out for rows the maintainer has already flagged as
unscorable pending disambiguation. Either #803 silently depends on that
disambiguation happening first (undeclared dependency — it appears nowhere
in #803's `ordering_after`), or "every row" quietly becomes "every row except
the ones we can't," which AC-2 of #802 explicitly disallows ("'not scored' is
not an allowed grade"). This is a real blocker the issue doesn't acknowledge.

**4. A recorded design fix from the parent feature's own review is missing from #803's acceptance criteria.**
The #592 dedup-pass-1 comment recommends, as the fix for #596's forever-bucket
risk: *"every row in this catalog carries the owning feature number (#593...
#594... #595... #596... #570), so 'the long tail' is defined by subtraction
rather than by judgement at funding time... Without that column, #596's
bucket can re-own #593/#594/#595 scope by simply scoring a row for it."* #803
is exactly the task that adds columns to the catalog (funding score,
stop-loss, acceptance vehicle) — the natural place to also land the
owning-feature column the maintainer already asked for — but it is absent
from #803's four acceptance criteria, and it isn't in #802's five either. If
#803 ships as written, the catalog gets a funding score and a stop-loss
column but still lacks the one column the maintainer's own analysis says is
required to prevent #596 from silently re-owning PF-2..4 scope. Recommend
folding the owning-feature column into AC-1 explicitly.

**5. AC-3's timed baseline has no protocol against the confound it exists to rule out.**
AC-3 asks for "a timed baseline for building a 4-bit counter from scratch...
with the procedure, the operator and the conditions stated so it can be
re-run comparably." This baseline is what CAP-37 AC-4 ("the after is not
slower") will be judged against — i.e., it gates a load-bearing claim of the
whole #521 capstone. As specified it has: no requirement that the same
build be timed by more than one operator or trial (n=1 is legal under this
text); no counterbalancing or blinding discipline against practice effects
(if the same operator later re-times the "after" build having already built
the identical circuit once, they benefit from familiarity independent of any
editor change); and no numeric tolerance for "not slower" (one second slower
technically fails AC-4 unless a margin is defined here, and nothing says
who defines it). "Stated so it can be re-run comparably" describes
reproducibility of the *setup*, not validity of the *comparison* — a
literal reading of AC-3 is satisfiable by a single unblinded self-timed run
with no safeguards, which would let CAP-37 AC-4 pass or fail on noise. Add:
number of trials, whether the same operator does both timings, and how
"not slower" is quantified.

**6. AC-4's "recorded as a gate on #521" is unverifiable prose, not a checkable artifact.**
AC-4 says "the ordering is recorded as a gate on #521." #521's gate on PF-2..5
already exists at the #592 level (`gates: ["#521 PF-2", ..., "#521 PF-5"]` in
#592's YAML front matter) — so it's unclear whether AC-4 wants (a) a sentence
in the catalog doc that says this in prose, (b) an edit to issue #521 itself
adding a field, or (c) nothing beyond what #592 already declares. As written
this criterion can be satisfied by one throwaway sentence anywhere in the
catalog file that mentions "#521," which would technically close the AC
without making the gate machine-checkable or even prominent. Recommend
specifying where in the catalog document this statement must live (e.g. a
named header/section) if it's meant to be more than decorative.

## What is solid

- The `ordering_after: [TASK-C592-1]` dependency on #802 is real, verified
  open, and correctly sequenced (schema-only catalog first, scoring columns
  second).
- Splitting "catalog exists" (#802) from "catalog is fundable" (#803) is a
  reasonable decomposition that keeps each task in the stated 0.5-1 mw band
  more plausible than one combined task would be.
- AC-1's core idea — funding score and stop-loss as a machine-readable
  column rather than prose guidance — is a genuine improvement or at least a
  clearer gate than what #592 originally specified, and is directly
  traceable to CAP-37 AC-3/KC-37-2 (once the PF-5 scoping in finding 2 is
  fixed).
