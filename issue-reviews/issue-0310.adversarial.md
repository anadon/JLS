# Issue #310: CAP-15: every drawable design leaves for the four open HDL toolchains, is checked against them, and comes back as a hierarchy
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## 1. [Critical] Central code citations are false on the default branch, and the body was never corrected
The Abstract, "Intended Audience & Impact", §1 Outcome Statement, and Background sections all
build their "why this matters now" framing on `src/jls/hdl/HdlExporter.java:465-468` — quoted
verbatim as "subcircuits cannot be exported yet: the HDL model has no module-instantiation
statement…" — and on a four-bucket `REJECTED` map at `:429-477`. I checked this directly against
the repo at HEAD: `HdlExporter.java` has exactly **three** policy buckets (`EXPORTED` at line 422,
`SKIPPED` at 431, `TOPOLOGY` at 436); `grep -n "REJECTED\|subcircuits cannot be exported"` returns
**zero** hits in the whole file. This is not a stale-line-number problem — the code doesn't exist
on `master` at all. It's independently confirmed by the maintainer's own issue **#493** ("The
evidence_commit every filed issue declares (2d0ca9d) is on a branch that will be deleted"), which
lists **#310 by number** in its "Wrong about master — quotes or relies on branch-only code" table,
and states plainly: "master has no `REJECTED` bucket at all… an unclassified element is refused
with no reason attached." The refusal-with-reasons machinery this issue treats as *already shipped
background* ("Recorded so it is not funded twice") is actually **issue #492's proposed, unlanded
scope** — and #492 is not in #310's `requires_features`. #310 received one comment
(`2026-08-03T20:33:02Z`) acknowledging exactly these two anchors are branch-only, but the issue
**body itself was never edited** — it still asserts the refusal as present fact throughout, and the
Completion Criteria's own box ("Every cited evidence document and permalink resolves on the default
branch at close") is *already failing today*, not merely a future risk.
**Recommendation:** before this issue is worked, edit the body (Abstract, Impact, §1, Background) to
either (a) cite #492 as the source of the refusal-with-reasons behavior and drop the "already
shipped" framing for it, or (b) fold #492 into `requires_features` if the totality/reason-map work
is in fact needed for AC-5.

## 2. [Critical] The planning-corpus evidence this issue leans on doesn't exist on the default branch
The "Document cross-check" section claims `docs/plan/capstones/CAP-15-hdl-toolchain-parity.md`
(cited by line range 61-73, 82) shows "no document-versus-issue disagreement," and the D13 REPLAN
comment quotes a maintainer ruling from `docs/plan/evidence/BRIEF.md §14`. I verified: `docs/plan`
does not exist anywhere in this repository — not on `master`, not in any local ref, not in the
full `git log --all --diff-filter=A` history. Issue #493 confirms this at scale: all 192
`docs/plan/**` files are "absent from `master` entirely… unrecoverable by re-reading. These files
never existed on `master`." Every sufficiency/cost claim in #310 that says "the corpus agrees"
(including the OQ-8 adjudication comment's per-feature cross-check against "corpus row… under
`docs/plan/features/`") is unauditable by any contributor working from the checked-out tree — the
only surviving copy of that reasoning is the prose in the issue/comments, which is exactly the
thing being cross-checked. This weakens confidence in the 67-101 mw cost band and the sufficiency
argument, neither of which can be independently verified against their cited source.
**Recommendation:** either commit the planning corpus to `master` (even as a snapshot) or stop
citing it as verifiable evidence; state plainly that these figures are asserted, not checkable.

## 3. [High] Scope/process overhead the maintainer already pushed back on, without a real correction
The D13 REPLAN comment quotes the maintainer verbatim: *"I don't think that these require such
fuss. Just make something that works."* Yet the very next comment (`5174128895`, one day later) is
a full re-verification pass re-confirming all ten roster rows and re-checking every ordering edge —
producing no change, only more ceremony. Across the six comments, the volume of DAG-walk
bookkeeping, edge-withdrawal tables, and kill-criteria renumbering trivia (`KC-15-n` vs `K1`-`K5`)
substantially exceeds the volume of content describing what to actually build. This is the same
class of "fuss" the maintainer flagged, recurring after the ruling that was supposed to stop it.
**Recommendation:** apply D13 to #310 itself — stop producing verification passes that change
nothing, and let the required features (already filed and separately reviewable) carry the
detail.

## 4. [High] Feasibility: a large capstone stacked on nine unfinished features, for a single-maintainer project
The required-set cost sums to 67-101 "mw" (unit undefined in-issue), ~40% of it (28-36 mw) in
FEAT-026 alone — a four-state value-domain redesign, not an incremental change. ARCHITECTURE.md's
own recorded decisions repeatedly describe JLS as a "single-maintainer pedagogy tool" (see the
i18n non-goal rationale). #310 is one of 19 filed capstones drawing on a pool of 57 features; none
of the cost, scheduling, or sequencing discussion addresses whether this is achievable on any
realistic timeline for one maintainer, or whether it's aspirational backlog that will accumulate
REPLAN churn indefinitely (as ARCHITECTURE.md's #78 element-registry note already predicts for
every new element: "sixteen places" to touch, before any of this capstone's tooling is added).
**Recommendation:** the issue should state an intended timeframe or explicitly disclaim one,
rather than leaving feasibility as an unaddressed background assumption.

## 5. [Medium] AC-1's falsification bar is real but shallow
Credit where due: AC-1 requires the differential oracle be shown **red** against a deliberately
mis-emitted design before any pass counts — a genuinely strong anti-vacuity requirement most of
this template's other criteria lack (§3 risk 4 admits FEAT-019/#321's and FEAT-020/#320's
*individual* criteria are gameable by golden files the target tool never touches). But AC-1 doesn't
constrain *how many or what kind* of induced bugs the red-transcript must cover, and Open Question
5 defers the definition of "settling point" — the exact axis §3 risk 3 warns can make "a
settling-point comparator with a badly chosen settling definition agree with everything" — to
implementation time with no worked example of a divergence class the comparator must catch (e.g.
wrong reset polarity is given as *an* example, but nothing requires covering X/Z propagation bugs,
which is the harder case FEAT-026 exists for).
**Recommendation:** name at least one X/Z-class and one timing-class mis-emission in the
falsification requirement, not just polarity.

## 6. [Medium] AC-5's totality check can pass while real elements are misclassified
"Add a synthetic 36th element type in a test fixture; run the build. Observe: the build fails until
the type is placed in a bucket." This proves the totality *mechanism* fires on an unclassified
type, not that the 35 *real* element types are correctly bucketed today or will be correctly
bucketed as new ones land — a build-breaks-on-missing-entry check is satisfiable by a registry-size
assertion alone, with no semantic verification that a given element landed in the *right* one of
EXPORTED/SKIPPED/TOPOLOGY/REJECTED. This is the same gameable shape §3 risk 4 already calls out for
other criteria, just not applied to AC-5 itself.

## 7. [Medium] The FEAT-numbering scheme has already produced one real ordering error
The filed dependency graph originally drew `FEAT-019 → FEAT-020` when the real prerequisite (per
#320's own `blocked_by`) is FEAT-021, because "the two adjacent FEAT numbers map to non-adjacent
issue numbers in opposite order" (#321 vs #339). This is disclosed and was caught — but only by
manual title-by-title cross-checking against a large lookup table, a process with no structural
safeguard against a second silent transposition. The issue's own repeated warning ("position in the
range is not evidence") is itself evidence the numbering scheme is a standing hazard, not a
one-time slip.

## 8. [Low, positive] Everything outside the HdlExporter.java/docs-plan cluster checks out
Verified directly against the current tree: `HdlModel.java`'s `Direction` enum at lines 30/32
(issue cites 28-33), `HdlModel.java:891` `moduleName`, `Adder.java:261`'s `propDelay = bits *
defaultPropDelay`, `CellValidator.java`'s 19-entry `SUPPORTED` set at line 58, `NetlistImporter
.java`'s exactly 5 realized cell types (`$not/$and/$or/$xor/$mux`) falling through to `default` at
line 250, the `ProcessBuilder` counts (0 in `src/`, 15 in `test/`), `ElementRegistry`'s 35 element
types, `HeuristicLayeredLayouter.java`'s 553 lines, and all six named totality test files. This is
unusually careful grounding for a template-generated issue — worth stating so the two failures
above read as a specific, fixable defect rather than a wholesale trust problem.

## 9. [Low, disclosed] The #59 supersession is real and the one loose end is surfaced honestly
Confirmed #59 is closed (`state_reason: not_planned`), matching the comment thread's claim, and the
mirror obligation (Open Question 1) was executed on #59's side. #310's own comment thread discloses,
rather than hides, that #291 (Memory export) lost its tier-level owner when #59 closed and has not
been re-homed — a legitimate small gap, but transparently flagged as open rather than silently
dropped.

## Bottom line
The required-feature decomposition, sequencing, and acceptance-criteria design are largely
coherent, and the issue is unusually diligent about disclosing its own gaps (§3, Open Questions,
the D13 and #493 comment threads). But the issue's own substantiating evidence — the code anchor
that motivates the whole capstone and the planning-corpus citations used to justify its cost and
sufficiency — is demonstrably false or unverifiable against the branch this work would actually
land on, and that defect is already known to the maintainer (via #493) yet uncorrected in the body
weeks later. A contributor picking up #310 today would be working from a spec with a broken
foundation until the body is edited.
