# Issue #344: FEAT-028: a port and a net know what alphabet they speak, and the editor refuses a ternary-to-binary connection for the same reason it already refuses 4-bit-to-8-bit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is open, well-organized on the page, and its code citations against
the *current* checkout (`WireNet.java`, `SimpleEditor.java`, `Constant.java`,
`docs/simulation-semantics.md`) check out almost exactly. But the issue's own
comment thread has, over the course of a single day (2026-08-08), rewritten
the data model twice, has not propagated either rewrite into the body's
YAML block, §1 criteria, §3 arithmetic, or Completion-Criteria checklist, and
depends on an upstream feature (#322) that itself flags the exact design
question (plane count) this issue's own scope-bounding arithmetic assumes is
settled. As filed, an implementer who reads only the body would build a
model the maintainer has explicitly discarded twice.

## Findings, most severe first

**1. The issue body is self-contradicted by its own comment thread, and none of the load-bearing sections were updated to match — CRITICAL.**
The body's §1/§3/Completion-Criteria describe radix as a property of *both*
`Put` and `WireNet` ("carry radix on puts and nets," TASK-0059), validated by
all-attached-puts-agreeing, bounded at radix ≤ 5 by `P(r)=⌈log₂(r+3)⌉` planes,
with radix ≥ 6 refused "with the arithmetic reason stated" (criterion 6,
IC-6). Comment 2 (`REPLAN`, 2026-08-08T18:55) retracts exactly that bound:
*"The radix-6 kernel refusal and the `ceil(log2(r+3))` scope bound are
removed. Arbitrary bounded N is a hard requirement,"* and replaces "radix"
with a signed interval `[lo,hi]` model where balanced ternary is native.
Comment 3 (`CORRECTION`, 2026-08-08T19:05, quoting the maintainer verbatim)
then supersedes comment 2 itself: *"WireNet gains no `lo()`/`hi()`... only
`Put` gains the accessors"* — the domain lives on ports, never on nets, which
directly contradicts the body's TASK-0059 contract and comment 2's "net
holds the interval" model in the same breath. None of this reaches the
issue's own YAML `planned_tasks`/`blocked_by` block, the roster table in §2,
the mermaid diagram, or the Completion-Criteria checklist — all of which
still read as if the original radix-2-to-5, net-validated model is current.
**Recommendation: before any child is filed, edit the issue body itself
(not just append another comment) so §1, §3, IC-6, and the DoD checklist
reflect the port-only-domain model from comment 3, and delete or explicitly
supersede the now-false arithmetic in §3.**

**2. IC-6 is now an acceptance criterion that cannot be honestly satisfied — gameable / self-defeating — HIGH.**
IC-6 ("radix ≥ 6 is refused with a message containing `⌈log₂9⌉=4` exceeds
three planes") is still listed, unedited, in §5 and implicitly required by
the DoD's "every prediction in §5 verified." Comment 2 explicitly retracts
the premise IC-6 depends on ("Cliffs become element-level, refused per
element... never [refused as a hard kernel bound]"). An implementer has two
bad options: (a) literally satisfy IC-6 as written, reintroducing a scope
bound the maintainer discarded, or (b) follow the REPLAN and silently fail a
still-open checkbox. Either way the checklist can be gamed — checked off by
implementing stale scope, or left honestly unchecked forever. **Recommendation:
IC-6 needs an explicit successor criterion (element-level refusal, stated
where) before it is left in the DoD list.**

**3. Hard-blocked on an upstream feature that has not resolved the exact question this issue's math depends on — HIGH.**
`blocked_by: [322]` is real (verified: #322 is open). #322's own Open
Questions §1 states: *"Two planes or three?... These disagree, and the
disagreement is load-bearing: #295 (CAP-03) requires the three-plane
record... Blocks TASK-0056's filing — it decides the type's shape."* #344's
entire §3 arithmetic (`P(3)=P(4)=P(5)=3`, "fourth-plane cliff is at radix 6")
is derived assuming the three-plane resolution wins a question #322 says is
still open. #322 also lists zero landed or filed tasks at the time of
fetch — this is the third consecutive layer (#232 → #322 → #344) of an
unstarted, internally contested dependency chain. Nothing in #344 flags that
its own quantitative scope bound rests on an unresolved sibling decision.

**4. Evidence commit is unreachable in this repository — MEDIUM.**
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not exist in
this repo's history (`git cat-file -e` fails). Neither does
`3a81a4a7d6a0f108ec201e632732d308cc02b3fc`, cited as where
`docs/plan/evidence/mvl-determination.md` landed. Neither `docs/plan/`, that
file, nor `Add3Swar.java` exist anywhere in the tree at HEAD (`5b05d67`).
The specific performance numbers underwriting the Stage-2 cost band and the
kernel's design rationale (9.79 ns/op vs 178.32 ns/op, 200,000-vector corpus)
are therefore not independently checkable from this checkout — they must be
taken on faith. This is partly mitigated: every *code-location* citation I
could check against HEAD (WireNet.java:139/232/280, SimpleEditor.java
:4015/:4142/:4247/:4358, Constant.java:36, `ElementRegistry` = 35 types,
`docs/simulation-semantics.md`'s "no unknown/X state" language) matches
almost exactly, so the pattern is "unlanded document with unverifiable
numbers," not "fabricated code citations."

**5. Open Questions 2 and 3 are declared blocking but resolution is soft — MEDIUM.**
§ "Open Questions" states Q2 "Blocks filing TASK-0056" and Q3 "Blocks filing
TASK-0059," and the DoD requires "Open Questions 1–3 are answered on this
issue before the first child is filed." Comment 1 records that TASK-0056's
successors (#878/#879) were filed today, but no comment on #344 records an
explicit decision resolving Q2/Q3 — only that "recommended defaults stand"
per an external proposal document (PR #887, itself marked "not normative").
If a "recommended default" mentioned once, off-issue, counts as answering a
declared blocking question, the DoD's blocking language is decorative rather
than enforced. **Recommendation: post the Q2/Q3 resolutions as an explicit
decision on #344 itself, not by reference to a non-normative PR.**

**6. Minor call-site count drift — LOW.**
The issue's "78 call sites" claim (92 `getBits()` occurrences minus 14
declarations, `src/*.java`) is close but not exact against current HEAD: a
recursive grep of `src/**/*.java` finds 89 occurrences / 14 declarations / 75
call sites. Not a defect by itself — code moves — but nothing in the issue
commits to re-deriving this count at the commit TASK-0059 actually starts
from; Open Question 3's "architecture test that fails on a new unaudited
site" (recommended default, not committed scope) is the right fix and should
be promoted from option to requirement given the count already drifted once.

## What's solid (brief)

- The width-vs-radix distinction in §3 — "width folds by maximum, radix must
  not" — is precise, correctly cites the real `Math.max` idiom at
  `WireNet.java:232`/`:280` and `net.bits = put.getBits()` at `:139`, and
  identifies a genuine, specific failure mode (silent radix promotion)
  worth guarding against regardless of which data-model revision wins.
- The four "Bits don't match" editor sites are identified with exact,
  verified line numbers (`SimpleEditor.java:4015,4142,4247,4358`) — real and
  precise.
- The non-goals list (no drawable N-ary elements, no HDL export, no
  device-tier/energy semantics) is a clean, legible scope fence, independent
  of the data-model churn above.

## Verdict rationale

`needs-rework`: not because the underlying idea (radix/alphabet validation
parallel to width validation) is unsound, but because the issue as currently
written is unsafe to hand to an implementer today — its own comment thread
has quietly replaced the model twice without touching the body, leaving a
completion checklist (IC-6 in particular) that cannot be honestly satisfied,
and it rests on an upstream issue (#322) that has not resolved the exact
question its scope arithmetic assumes is closed. The body needs an editorial
pass reconciling it with comment 3 before any child task is filed.
