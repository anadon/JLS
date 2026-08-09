# Issue #575: FEAT-C33-1: an instructor teaching from the standard text finds a lab already written for each chapter — starter circuit, exercise prose and grading vectors, ready to assign
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The scope boundary against #548 (curated examples), #552 (lesson content), and #502
(platform plumbing) is precise and each is stated once and honored — good hygiene, and
the pass-1 adjudication comment on this issue confirms #575 vs #578 is producer/spec,
not a duplicate outcome. But the issue's own five acceptance criteria contain two
internal contradictions (one severe enough to make the headline "≥8 labs ship" false
under the issue's own kill mechanism) and three under-specified criteria that let the
stated verification pass while the real "chapter-by-chapter, teachable, clean-provenance
pack" outcome does not.

## Findings, most severe first

### 1. (High) AC-4's kill mechanism can silently violate AC-1's floor, with no backfill named
AC-1: *"At least 8 labs ship, spanning combinational → sequential → FSM → small
datapath."* AC-4: *"per KC-33-2, a lab failing two consecutive such reviews is pulled
from the pack rather than padding the count."* Read together: start at exactly 8 (the
stated floor), fail two labs on review, and the pack now ships 6 — which is no longer
"at least 8." Nothing in #575 says whether a pulled lab must be replaced before the
issue can close, what the effective floor is after pulls, or whether authors must
over-produce (e.g. author 10 to guarantee 8 survive). KC-33-2 is imported by reference
from #517 and explicitly prioritizes quality over count ("rather than padding the
count"), which is the right instinct, but it is never reconciled against AC-1's own
number here. As written, an implementer can satisfy AC-4 (pull the failing labs) while
failing AC-1 (fall under 8) and there's no criterion that catches the conflict.
**Recommendation:** state a target authored count with headroom (e.g. "author 10-12,
ship whichever ≥8 survive two-consecutive-pass review") or make explicit that AC-1's
"≥8" is re-checked after any KC-33-2 pull and gates closure until restored.

### 2. (High) AC-5's license dependency on #578 is real but omitted from the machine-readable `ordering_after` block
AC-5: *"Content provenance is clean and auditable... under the kit content license (see
FEAT-C33-4)."* FEAT-C33-4 is #578, and #578's own AC-4 is where that license actually
gets named ("Kit content carries a stated open license distinct from the code
license... the authoring doc tells third-party authors how to state theirs") — it does
not exist yet in #575. Yet #575's `ordering_after` YAML block lists only `#300` and
`#511 CAP-27 PF-5 / #552`; #578 appears nowhere in it, unlike the other two real
dependencies which are captured there. Worse, #578's own `ordering_after` names
"FEAT-C33-1 (the worked instance)" — i.e. #578 expects #575's pack to exist first so it
can serve as #578's worked instance, while #575's AC-5 expects #578's license decision
to exist first so the pack can ship "under" it. That is a genuine ordering cycle
between the two issues' own acceptance criteria, present in both bodies, caught by
neither's `ordering_after` block.
**Recommendation:** either add #578 to #575's `ordering_after` and accept that AC-5 is
not closeable until #578's licensing AC lands, or break the cycle explicitly (e.g. #575
ships under a maintainer-chosen provisional license, #578 ratifies/relicenses later) —
but the cycle needs to be named, not left implicit across two issue bodies.

### 3. (Medium) The #300 dependency has no degraded-mode fallback, unlike the sibling PF-2 workflow issue
`ordering_after: ["#300 CAP-06 verdict slice (the grading engine the vectors run on)"]`
— stated as an unconditional prerequisite. But #300 (CAP-06) is itself open and
unlanded, and its own kill/fallback discipline in the parent capstone (#517's
KC-33-1) explicitly exists only for PF-2 ("if CAP-06's verdict slice slips, PF-2 ships
against today's three-exit-status contract rather than waiting"). #575's own AC-2
("a CI lane grades the reference solution green and a planted-defect variant red") is
achievable today on the existing three-exit-status `-t` contract (`docs/batch-interface.md`
§1) exactly the way `examples/autograde/autograde.py` already does it, crudely — so the
hard block on #300 is not actually forced by AC-2's text, only asserted by the ordering
note. If #300 slips, #575 has no recorded fallback the way its sibling does.
**Recommendation:** either justify why AC-2 specifically needs #300's richer verdict
machinery (counterexamples, xUnit) rather than today's exit-code contract, or give #575
the same degraded-mode escape hatch KC-33-1 gives PF-2.

### 4. (Medium) AC-3's "declares the chapter" has no enforcement mechanism at the time #575 is expected to close
AC-3: *"Each lab declares the chapter it maps to and a stated student time budget."*
The only mechanism anywhere in this issue cluster that could check a structured
declaration — the validator — belongs to #578 (FEAT-C33-4 AC-2), and #578's own
`ordering_after` places it *after* #575. So at the moment #575 can be marked done,
nothing has validated that "declares the chapter" means a structured, checkable field
rather than a sentence buried in free-text exercise prose. AC-3 as written is satisfied
by prose alone, which is functionally ungameable-proof only by inspection, not by any
test named in this issue.
**Recommendation:** either add a minimal machine-checkable convention directly to #575
(e.g. a one-line `CHAPTER:`/`TIME_BUDGET:` header each lab file must carry, tested by a
lightweight assertion in this issue's own CI lane) or explicitly defer AC-3's
enforceability to #578 and mark it "declared, not yet validated" in #575's own
completion criteria.

### 5. (Medium) "Auditable" provenance (AC-5) names no audit process, auditor, or artifact
AC-5 asserts *"Content provenance is clean and auditable — original prose and circuits
only... All content is original; no DEEDS assets, text or figures are copied."* No
audit trail is specified: not a per-lab attestation, not a diff-against-DEEDS check
(impossible anyway — DEEDS's `.pbs`/course materials are exactly the closed asset this
whole capstone says JLS cannot access), not even a reviewer sign-off distinct from
AC-4's completion-time-budget reviewer. As written, "auditable" is satisfied by the
author's own unverified assertion in a commit message, which is not what "auditable"
normally implies.
**Recommendation:** name what the audit artifact actually is — e.g. an explicit
per-lab provenance statement file, or extend AC-4's non-author review to also attest to
originality, since that reviewer is already in the loop.

### 6. (Medium) Copyright/derivative-work exposure of textbook-chapter alignment is unassessed
The Outcome is built explicitly around mirroring "the Donzellini Springer digital-design
text" chapter-by-chapter (corroborated by #510's survey: *"Textbook-mapped lab pack
(Donzellini chapters — closed format, port the course not the files)"*). The issue
disclaims copying DEEDS *assets*, but a commercial Springer textbook's chapter
organization and pedagogical sequencing is itself a structured expression some
publishers treat as protectable, and nothing here records that Donzellini or Springer
was consulted, unlike the parallel Dr. Siever conversation tracked for #509/#577. This
is the one issue in the CAP-33 cluster where the risk actually materializes (the labs
get written here), yet no AC addresses it.
**Recommendation:** add a criterion confirming the mapping uses only chapter titles/
topic ordering (widely treated as unprotectable organization of ideas) and no
substantive text, figures, problem sets, or exercise wording from the source text, and
record whether outreach to the publisher/author was considered necessary.

### 7. (Low) AC-1's "spanning" wording is looser than the Outcome's "chapter-by-chapter" promise
The Outcome promises "a chapter-by-chapter lab pack." AC-1 requires only "≥8 labs...
spanning combinational → sequential → FSM → small datapath" — a category-coverage
requirement, not a chapter-count requirement. If the Donzellini text has materially more
than 8 chapters (typical intro digital-design texts run 10-15), a pack that ships
exactly 8 labs, one thin sliver per category, technically satisfies AC-1 while leaving
most of the actual textbook chapters — the thing an instructor is promised — unmapped.
**Recommendation:** either state the actual chapter count of the reference text and
require mapping to a stated fraction of it, or drop "chapter-by-chapter" from the
Outcome language if partial-category coverage is the real intended target.

### 8. (Low) No author or reviewer capacity named for content that needs subject-matter expertise, not engineering time
`band_mw: "3-5"` covers writing eight-plus labs of exercise prose and pedagogically
sound circuits mapped to a specific external text, plus the AC-4 non-author completion
review pool. Neither role is named anywhere in #575, and this is a single-maintainer
project per `ARCHITECTURE.md`'s recorded i18n-scope decision — the same capacity gap
already flagged at the capstone level (#517 review finding 6) recurs here undiminished,
since #575 is where the actual authorship happens.
**Recommendation:** name who authors and who serves as the non-author reviewer pool (or
flag the band as provisional pending a resourcing decision) before treating 3-5 mw as
real.

## What's solid

- The scope boundary against #548 (curated Examples menu) and #552 (lesson tooling) is
  stated once, cleanly, and doesn't overlap either sibling's own boundary language.
- AC-2's CI shape (reference solution green, planted-defect variant red) is concretely
  testable and resistant to the "three-literal-stdout-lines" gaming that #300's own
  Background section calls out in `examples/autograde/autograde.py`.
- The pass-1 adjudication comment's producer/specification split against #578 is
  argued from both bodies' own text and holds up on inspection — no merge is the right
  call.
- The claim that DEEDS's format is closed and only the course (not the files) can port
  is independently corroborated by #510's teardown, not invented for this issue.

