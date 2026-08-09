# Issue #600: TASK-C332-1: a design is expressible as N part files plus one boundary description that names the cut nets the author declared
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task is well-scoped on paper (explicit non-goals, a clean AC-per-invariant
structure, correct cross-references to #332's own invariants), but several of
its acceptance criteria depend on mechanisms that are not yet decided by the
very issues this task orders itself after, and two real hazards in the
project's own threat model (multi-file write atomicity, aggregate resource
exhaustion) are left completely unaddressed by the AC list.

## Findings, most severe first

**1. (High) AC-4 presupposes a shape for #319 that #319 has not decided yet.**
AC-4 requires the part-set container be "section-framed and carr[y]
must-understand semantics from #319." But #319's own body lists Open Question
1 as unresolved: "Frame inside the text grammar, or a multi-member
container?... Must be answered in TASK-0033, not discovered. **Blocks filing
children**." #319's `planned_tasks` are all "not filed." So #600 bakes in an
assumption — that a "part file" can simply be wrapped in a section frame —
before the frame's own shape (single-grammar vs. multi-member container) is
settled. If #319 lands option (b), it is not obvious AC-4 as worded still
makes sense (is each part file itself section-framed, or is the *part set*
one section-framed container spanning files?). Recommendation: either state
AC-4 generically enough to survive either #319 outcome, or explicitly gate
#600's start on #319's Open Question 1 being answered, not just on #319
"landing" in some eventual sense.

**2. (High) AC-2's "naming" of cut nets has no defined naming scheme, and the two candidates are both blocked elsewhere.** AC-2: "The boundary description names exactly the cut nets... as an equality in both directions, asserted over a generated design." Naming nets in a way that is stable and testable is exactly the deliverable #336 has not shipped: #336's Open Question 1 ("The digest function, and whether the raw stable id may appear in an artifact... **Blocks filing TASK-0008**") is unresolved, and #336's own `planned_tasks` are unfiled. #600 never says whether the boundary description keys cut nets on `Element.getStableId()` (already exists, #165) directly, on pin/(part, pin) address pairs, or on #336's not-yet-frozen synthesized name. Without picking one, "asserted over a generated design" is not a testable requirement — the test author has to invent the naming scheme #336 is chartered to invent. Recommendation: pin the naming basis explicitly in the AC text (e.g., "keyed on `Element.getStableId()` pairs, independent of #336's synthesized display names") so the criterion is actually checkable without #336 landing first.

**3. (High) Multi-file write atomicity is unaddressed, contradicting the project's own documented crash-safety discipline.** ARCHITECTURE.md states the existing save path is atomic specifically so "a crash mid-write never destroys the previous save" (`ARCHITECTURE.md:74-77`), and `FileAbstractor.java:34-38`'s class doc makes the same claim ("via a temp file and atomic rename, so a crash mid-write can never leave a truncated file where a complete one used to be"). AC-1 requires writing "N part files plus one boundary description," but none of AC-1 through AC-5 says what a reader sees if the process is killed after writing part 3 of 5 but before the rest — there is no generation id, cross-file checksum, or two-phase-commit discipline mentioned anywhere in the issue. A partial write today leaves one truncated file that the existing single-file atomic-rename path prevents; a partial *part-set* write under this design leaves N+1 files in a state where some are stale and some are current, silently loadable as a self-consistent-looking but wrong design. Recommendation: add an AC (or an explicit `WAIVED:`-style deferral naming the sibling task that owns it) covering detectable/atomic multi-file commit.

**4. (Medium) Unbounded aggregate resource use is left open with no stopgap, in a codebase whose stated threat model is exactly this.** SECURITY.md: "circuit files (`.jls`/`.jls~`) are routinely shared between students and instructors and are treated as untrusted input — parser crashes, resource exhaustion, or code execution reachable from a hostile circuit file are all in scope." AC-5 correctly scopes the 64 MiB cap to per-part only, and the Boundary notes correctly attribute the aggregate on-disk budget to #332's open question 2. But AC-1's round-trip check ("reading that set back yields the same element and net content") necessarily materializes the parts to compare them, and this task explicitly disclaims streaming elaboration (the sibling task that would bound peak memory). As specified, nothing stops a hostile design from declaring hundreds of parts each just under the 64 MiB per-part cap, forcing tens of gigabytes to be resident during exactly the round-trip check AC-1 mandates — and nothing caps the part *count* N at all, which is also a filesystem-level exhaustion vector (huge numbers of tiny part files) independent of the byte cap. Recommendation: either bring a minimal sanity cap on N and/or aggregate bytes into this task's scope, or add an explicit `WAIVED:` note naming the follow-up issue, per the project's own convention (#332 §7) for not silently absorbing a known gap.

**5. (Medium) AC-1 lacks the rigor AC-2 explicitly demands, despite being the more foundational claim.** AC-2 requires the cut-net-naming property be "asserted over a generated design, not by spot check" — a deliberate, well-chosen bar. AC-1, the round-trip equivalence claim itself, carries no such requirement and could be satisfied by a single hand-built two-part fixture. That would leave untested: nets spanning three or more parts, multiple simultaneous cuts through the same element, and uneven pin distributions across parts — precisely the cases a generative test would catch and a hand-picked fixture would not. Recommendation: apply the same "generated design, not spot check" standard to AC-1.

**6. (Medium) The task's declared prerequisite list is narrower than the parent feature's, with no stated reason.** #332's machine block declares `blocked_by: [319, 336, 353, 370]`, and its Sequencing section states all four gate the feature. #600 declares `ordering_after: [319, 336]` only, dropping #353 (elimination of the quadratic/materializing load-time fixup) and #370 (flat compact element representation). This narrowing may well be correct — #600 explicitly disclaims capacity and streaming-elaboration concerns as sibling scope — but the issue never says so, and #319's own Re-planning Protocol calls a dropped ordering edge without a matching `REPLAN:` a "half-edge... the defect this Link pass exists to prevent." A task quietly omitting two of its parent's four blocking dependencies should say why in one sentence, not leave it to be inferred.

**7. (Low) AC-3's "byte-identical to today" is a sharper implementation constraint than it reads.** Read literally, AC-3 requires that routing an uncut design through the *new* partition-aware save path (N=1, no cuts) must reproduce the exact legacy single-file bytes — not merely "similar" or "round-trippable" output. That is a real, easy-to-violate constraint (e.g., an accidentally-emitted empty section marker, or the container-detection logic taking a different code path than before) and is worth calling out explicitly to implementers, since the wording could otherwise be read as "still one file, semantically equivalent" rather than "bit-for-bit identical."

**8. (Low) The `band_mw: "2-3"` estimate outruns its own parent's arithmetic.** #332's Open Question 3 states the five child scopes (of which this is one) are "unpriced" and that the cost band is not yet a rollup of task rows: "there is no row sum to reconcile it against... price each scope when it is filed, then re-derive the band from the sum." #600's "2-3" is therefore the first data point toward a reconciliation #332 has not yet performed — not a contradiction, but the estimate should not be read as validated against the parent feature's own cost model.

## What's solid

- The non-goals ("Boundary notes") are explicit and correctly hand off streaming elaboration, net identity across a cut, uncuttable-construct refusal, and the equivalence harness to named sibling tasks under #332 — this keeps the task testable without pretending to solve partition quality.
- AC-3's cross-reference to #332 invariant 2 (no boundary description leaking into an uncut design) is accurate and consistent with the parent issue's text.
- AC-5's citation of `MAX_CIRCUIT_TEXT_BYTES` and its file/line (`src/jls/FileAbstractor.java:65`) is correct as verified against the source, and correctly defers only the *aggregate* budget question to #332 open question 2 rather than silently dropping it.
