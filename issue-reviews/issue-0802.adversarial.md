# Issue #802: TASK-C592-1: the editor ergonomics parity catalog is published — one row per behaviour, cited to its originating complaint, graded HAVE/GAP/REFUSE
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task is a documentation-only deliverable (a scored parity catalog under `docs/`) gating funding for later editor-ergonomics fixes. The no-editor-code boundary is clean and the HAVE/GAP/REFUSE-with-reason discipline is a real floor. But checked against its own parent feature (#592) and capstone (#521), #802 quietly drops two of the parent's five acceptance criteria, asks a grader to classify rows against a target architecture (#316) that is itself unresolved and self-contradictory, and leans on external-tracker citations that are neither verifiable from any material in this repo nor protected against collision with this repo's own issue numbers.

## Findings, most severe first

### 1. [HIGH] #802 silently drops two of parent-feature #592's five acceptance criteria, with no sibling task to pick them up

#592 (FEAT-C37-1, `part_of_feature` target of this task) states five ACs for the catalog: AC-1 (rows cited+graded), AC-2 (REFUSE reasons), **AC-3** ("Rows carry a funding score, and PF-5's stop-loss rule (KC-37-2, per-item 1.5x) is expressed as a column"), AC-4 (bsiever #18/#4 named, satisfying CAP-37 AC-3's precondition), and **AC-5** ("A timed baseline for the CAP-37 AC-4 task (build a 4-bit counter from scratch) is recorded... so the 'after is not slower' claim has a before"). #802's own acceptance criteria list only covers what map to #592's AC-1, AC-2 and AC-4 — there is no funding-score/stop-loss column, and no timed-baseline requirement anywhere in #802. `get_sub_issues` on #592 returns an empty list, so no sibling task exists to carry these two. Yet #802's Outcome text claims this task itself is "the gate the fix features are funded against." If #802 closes as literally written, PF-5's per-item 1.5× stop-loss (KC-37-2) has no column to be read against and CAP-37 AC-4's "not slower" claim has no recorded "before" — both silently orphaned.
**Recommendation:** either add #592 AC-3 and AC-5 as explicit acceptance criteria here, or state plainly (and file) which other task carries them, before this is scheduled as "the gate."

### 2. [HIGH] AC-4's "blocked on #316" classification asks for a stable verdict against an architecture that is itself unresolved and self-contradictory

AC-4 requires flagging rows whose "only plausible implementation is a `SimpleEditor` edit" as blocked on #316 rather than scoring them ready. But #316 (fetched directly) records its own extracted-state contract as unresolved: §3 specifies a GoF per-state class design, while a 2026-08-08 comment reports the absorbed duplicate task (#441) instead specifies "a *public* class ... and no per-state `enter`/`exit` objects" — a contract deviation #316 itself says "requires a `REPLAN:` here" that, as of the same day, has not been posted (confirmed independently in the earlier review of #316 in this same tracker, `issue-reviews/issue-0316.adversarial.md` Finding 1). Deciding today whether a given ergonomic fix's "only plausible implementation" is inside `SimpleEditor` or in a decomposed collaborator requires knowing the shape of collaborators that do not yet exist and whose design is disputed. Rows graded "ready" now under one candidate design could become "blocked on #316" (or vice versa) once #316 picks a shape — and #802 gives no re-score trigger or revisit mechanism for that.
**Recommendation:** either make AC-4's classification explicitly provisional (with a named re-score trigger tied to #316's REPLAN landing), or defer AC-4 until #316 resolves its own design contradiction.

### 3. [MEDIUM] Citation requirement (AC-1) is gameable: two named sources have no issue numbers at all, and the four numbers given elsewhere are not corroborated by any material this repo can check

AC-1 demands citations to "Digital #882/#1308/#1129, Logisim-Evolution #88/#1234, Issie width-inference messages, LogicCircuit bus ergonomics." "Issie width-inference messages" and "LogicCircuit bus ergonomics" carry no issue numbers — there is nothing to check a citation against for those two rows short of trusting whatever text the catalog author writes. The four bare numbers for Digital/Logisim-Evolution are likewise never corroborated in this repo's own prior research: #510 (the "niche survey," fetched directly, explicitly the source corpus per #592's `ordering_after`) cites Digital #151, #1477, #84, #882, #1464, #1470, #1412 and Logisim-Evolution #786, #1546, #598, #1871, #2454 — but never #1308, #1129, #88, or #1234. Nothing in #802 requires the executor to actually open and confirm the cited external issue before using it as "the citation," so AC-1 as literally written can be satisfied by a plausible-looking but unverified or stale reference.
**Recommendation:** require each citation to carry a full `owner/repo#number` and a one-line quoted excerpt of the actual complaint, checked at authoring time, not just a bare number.

### 4. [MEDIUM] Bare external issue numbers collide with this repo's own numbering, and nothing in the AC guards against the confusion

The acceptance criteria cite "Digital #882" and "Logisim-Evolution #88" with no repo qualifier. This repo already has its own #882 (`TASK-C592-1`-style task about circuit time-base declarations — nothing to do with UI ergonomics) and its own #88 (a closed PR, "Fix the data-loss chain: save targeting, changed-flag ordering, checkpoint resurrection" — also unrelated), confirmed by direct fetch. A catalog row, a cross-reference in another issue, or a future automated link-checker reading "#882"/"#88" inside this tracker's own `docs/` will resolve them to the wrong issue by GitHub's default same-repo linking behavior unless the qualified form is used everywhere.
**Recommendation:** mandate the fully-qualified form (`hneemann/Digital#882`, not `#882`) in every row, and say so explicitly in the AC text — the current wording (matching #521's own evidence block) never does.

### 5. [MEDIUM] "One row per ergonomic behaviour" sets no granularity floor, so the row-count and grading-discipline requirements can be satisfied coarsely

AC-1/AC-2 require every row graded and REFUSE rows reasoned, but nothing requires a minimum row count or a 1:1 mapping between rows and the specific sub-behaviours the downstream fix issues (e.g. #804's compound-selection AC set) need scored individually. At the stated band (0.5-1 mw, half a week to a week), an author under time pressure can satisfy every letter of AC-1/AC-2 with a handful of coarse rows ("selection ergonomics: GAP, see bsiever #18") while omitting the fine-grained sub-behaviours a downstream fix issue actually needs individually scored to fund correctly.
**Recommendation:** name the minimum row set explicitly (e.g., one row per behaviour bullet already enumerated in #521's PF-2..5 descriptions) rather than leaving granularity to the author's judgment.

### 6. [LOW] Declared feature/dependency links are prose only, and one hard predecessor is dropped without comment

`part_of_feature: 592` in #802's YAML header is not backed by a GitHub sub-issue relationship — `get_parent` on #802 and `get_sub_issues` on #592 both return empty/null. #802's `ordering_after: []` also silently drops #592's own two stated hard predecessors ("#316 FEAT-008/#84" and "#510"). Treating #316 as a per-row flag rather than a hard wait is defensible given AC-4's design, but the issue never states that reasoning — it just declares an empty list where the parent declares two entries, continuing the same informal-bookkeeping pattern already flagged for this issue cluster in this tracker's #316 review (Finding 6: "the cross-issue bookkeeping scheme is still accumulating defects during the planning phase").
**Recommendation:** state explicitly in #802 why #316/#510 are not carried into its own `ordering_after`, rather than leaving the omission to be inferred.

### 7. [LOW] Cost estimate undercuts the parent feature's own estimate for the same deliverable, unreconciled

#802's `band_mw: 0.5-1` is below #592's and #521's own PF-1 estimate of "1–1.5 mw" for what #802's Outcome text presents as the same complete catalog deliverable — no note reconciles the two figures, unlike #316's issue, which explicitly flags and discusses an analogous band/task-sum mismatch.
**Recommendation:** either justify the lower estimate (e.g., because AC-3/AC-5 are out of scope here — see Finding 1) or align the numbers.

## What's solid

- AC-2's "not scored is not an allowed grade" is a genuinely hard-to-game floor: it forecloses the cheapest failure mode (silently skipping an awkward row) even though row granularity itself is not floored (Finding 5).
- AC-5 ("No editor code changes in this task") is a clean, binary, mechanically-checkable boundary — a `git diff` scoped outside `src/` settles it.
- The underlying discipline — grounding "the editor feels dated" in a cited, gradeable catalog before funding fixes — is sound project management and matches the stated purpose of gating PF-2..5 on evidence rather than opinion.

## Bottom line

The task's shape and boundary (docs-only, HAVE/GAP/REFUSE, no "not scored") are well-formed. But as written it does not fully discharge the deliverable it claims to be ("the gate the fix features are funded against"): it drops two of its own parent's five acceptance criteria with no successor task named, asks for a classification (blocked-on-#316) against an architecture that is itself unresolved, and rests its central verifiability mechanism (citations) on external references that are neither locally corroborated nor protected from colliding with this repo's own issue numbers.
