# Issue #491: ElementId.parse never advances the creation counter, so the second run of one install saves a circuit JLS then refuses to open
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what I verified

The root-cause diagnosis is real and reproducible against the current checkout
(HEAD `5311625`, not the issue's pinned `828822672`), not just against a
commit that has since moved on:

- `src/jls/elem/ElementId.java:59-60` — `NEXT_COUNTER` is a static
  `AtomicLong`, zero at every JVM start. Confirmed.
- `src/jls/elem/ElementId.java:210-213` (`mintFresh`) increments it;
  `src/jls/elem/ElementId.java:245-269` (`parse`) never touches it — the
  method's last effect before `return new ElementId(replica, counter)` is
  the negative-counter check at `:264-267`. Confirmed; no reference to
  `NEXT_COUNTER` anywhere in `parse`.
- `src/jls/elem/Element.java:24` mints in the field initializer
  (`private ElementId stableId = ElementId.mintFresh();`), and the `sid`
  attribute setter at `:293-296` overwrites it via `ElementId.parse` without
  ever feeding the parsed counter back into `NEXT_COUNTER`. Confirmed.
- `src/jls/Circuit.java:1310-1320` (the uniqueness refusal) and
  `:1322-1332` (the legacy-path `do/while` that *does* skip used ids) match
  the issue's quotes almost line-for-line. Confirmed.
- The regression test the issue proposes
  (`mintingAfterALoadSkipsIdsTheLoadedFileAlreadyDeclares`) does not yet
  exist in `test/jls/elem/ElementIdReplicaTest.java` — consistent with the
  issue's claim that this path is uncovered.
- H2's claim that "the collaboration op path consumes ids parsed on the
  same route" is not hand-waving: `src/jls/collab/op/CircuitOpReader.java:212`
  and `src/jls/collab/op/NetBlocks.java:410` both call `ElementId.parse`
  directly on peer-supplied text, and `AddElements.java`'s `validate()`
  loads blocks through `ElementBlocks.load`, which goes through the same
  `Element` construction/`sid`-setter path as a file load. The guard's
  necessity argument is grounded in real, already-built code, not a
  hypothetical future feature, even though #163 itself is filed as open.

The diff in §8 applies cleanly at today's line context (verified byte-for-byte
against `ElementId.java:264-268`), and the from-scratch determinism argument
(P4: `parse` never runs unless a file was loaded, so untouched circuits never
touch `NEXT_COUNTER`) is logically sound given the code as read.

## Findings, most severe first

**1. (Moderate) The issue treats a self-acknowledged incomplete fix as "the fix," and only an unresolved Open Question gates the missing piece.**
§7.11 states plainly: "Hostile `sid` with a huge counter under our own
replica... is a real residual hole and an executor must close it," and
Open Question 3 leaves the resolution — clamp vs. reject at
`Long.MAX_VALUE` — as "Needs a maintainer." But the code block the issue
literally headers "### The fix — `src/jls/elem/ElementId.java`" is the
three-line diff *without* that guard, and it is presented as copy-pasteable,
verified-to-compile, monotone-and-atomic. An engineer working from the
diff rather than reading all the way through §7.11 and the checklist item
in §8 ("This step is **not** optional") ships exactly the vulnerability the
issue exists to close: a crafted `sid "ourreplica:9223372036854775807"`
overflows `k + 1` to `Long.MIN_VALUE`, `Math.max` leaves the counter
unmoved, and the next mint can still collide with a low id the same file
declares — the identical user-visible failure (`Circuit.finishLoad`
refusing a JLS-written file) that this issue is filed to eliminate.
Recommendation: fold the overflow guard into the diff shown in §8 rather
than deferring it to a maintainer decision; if clamp-vs-reject genuinely
needs a maintainer call, ship the safe default (reject, already
recommended) in the patch and let a maintainer relax it later — don't ship
open.

**2. (Moderate) The issue's own metadata is self-contradicting on parentage, and the contradiction is unresolved at review time.**
The issue body's YAML block states `part_of_feature: none`, and Completion
Criteria explicitly relies on that: "`part_of_feature` is `none`, so no
feature `STATUS:` comment is owed." But four automated triage passes are
recorded in the comment thread, disagreeing with each other: comment 1
proposes #334 as parent without committing it; comment 2 (STAGE 3 CLOSE)
reaffirms "no parent feature"; comment 3 (STAGE-3 PARENTAGE RECORD)
reaffirms "no parent feature" again; comment 4 (same day as this review,
2026-08-08) reverses all three and asserts `part_of_feature: 356` as a
"CHAIN-INTEGRITY CORRECTION," while admitting "this is a judgement, not a
declaration in the body" and that #334 is an equally plausible alternative
the maintainer might prefer instead. The issue body was never edited to
reflect any of this. An executor who trusts the body's YAML (as the
Completion Criteria checklist instructs) will tick "no feature STATUS:
comment is owed" while the most recent comment says the opposite is now
true. Recommendation: resolve the parentage before pickup — either edit
the body's `part_of_feature` field or explicitly waive comment 4 — rather
than leaving two contradictory sources of truth live in the same issue.

**3. (Minor, security-adjacent) The counter-advance guard trusts an unauthenticated string match against attacker-reachable input, and the fix widens what that trust can do without naming it.**
`replica.equals(processReplica)` is the entire authorization check before
`parse` moves `NEXT_COUNTER`. That string is not secret — every element
this install has ever saved into a shared or transmitted file carries it
verbatim (docs/file-format.md's own framing, and this issue's §7.3, calls
`sid` "hostile input"). Confirmed reachable from two real paths: a crafted
`.jls` file, and a collaboration peer via `CircuitOpReader.parseId` /
`NetBlocks.parseSid` (both real code, not speculative — see above).
Before this fix, spoofing our replica in a parsed id only affected the
identity assigned to one element. After this fix, the same spoof can also
walk our *global* minting counter forward — which is exactly how finding 1's
overflow hole is triggered, but the general point (our own replica string,
once observed by anyone, becomes a lever on our future id space) is broader
than the single overflow case and isn't named anywhere in §7 or §11 as a
widening of what a spoofed replica can do. It doesn't need to block this
fix (the blast radius stays "wasted counter space," not memory/security
corruption, once finding 1 is closed), but it should be written down given
how carefully §7.3/§7.11 already treat `sid` as hostile input elsewhere in
the same document.

**4. (Minor) Internal arithmetic inconsistency in the O4 narrative.**
"loading one element burns counter 0, so mints go 1, 2, 3, 4, **5**: the
sixth element the user draws is the collision" — the five values listed
(1,2,3,4,5) are the mints for the *first through fifth* added elements
(counter 0 was burned by the one loaded element), so the collision is on
the fifth add, not the sixth, by the passage's own numbers. The formula in
§7.10 and the repro transcript in O4 are internally consistent with each
other (and with what I'd derive independently); only this one prose
sentence is off by one. Doesn't affect the diagnosis or the fix, but is
worth fixing given the document stakes its credibility on formal rigor
("RULE 3," predictions, falsification criteria) — an off-by-one in the
walkthrough undermines that stance and risks an executor mis-sizing a
fixture from the prose instead of the formula.

**5. (Minor) Unverifiable "filed separately" siblings.**
§12 references "Two sibling defects from the same rescue... filed
separately" (the frozen save-tag table/`docs/file-format.md` §7 gap, and
the HDL export policy gap) without issue numbers, so they cannot be
cross-checked from within this issue. Given how precisely everything else
in the document cites line numbers and issue numbers, this is a
traceability gap. Recommendation: add the actual issue numbers in a
follow-up comment before this issue is picked up, so "they share no root
cause with this one" (the claim made) is checkable rather than asserted.

## What's solid (one line each)

- The root cause (H1) is correctly and precisely located — no path other
  than `parse` needs auditing for this specific defect; the field-initializer
  mint in `Element.java:24` plus the setter overwrite in `:293-296` is a
  complete causal chain, confirmed by reading both files.
- H2's replica-scoping (advance only for `processReplica`) is justified
  against real collaboration code, not a hypothetical, and its "necessity"
  half (P3) is testable as stated.
- H3's monotonicity argument (`Math.max`) is correct and matches the
  existing `pinForTesting` idiom at `ElementId.java:170-181`, so the "no
  new discipline invented" claim holds.
- The compatibility story (old files still load, from-scratch saves stay
  byte-identical because `parse` never runs before any load) is logically
  sound given the code as read.
- Scope is honestly bounded: repairing already-broken files is explicitly
  out of scope (Open Question 1) rather than silently expanded into this
  task, which is the right call for a task-tier issue.

## Bottom line

The bug is real, the localization is correct, and the proposed patch is
minimal and well-argued for the case it covers. But the issue ships a code
block labeled "the fix" that its own §7.11 says is not the fix, and leaves
that gap behind an unresolved "Needs a maintainer" open question rather
than closing it — for a data-loss defect whose whole premise is untrusted
`sid` input. Combine that with the live contradiction between the issue
body's `part_of_feature: none` and the latest comment's assertion that this
is now wrong, and an executor who works strictly from the document as
written can complete every checked box while still shipping an incomplete
fix and skipping a process step the comment thread says is owed. Fix the
diff to include the overflow guard and settle the parentage before this is
picked up; the underlying diagnosis does not need rework.
