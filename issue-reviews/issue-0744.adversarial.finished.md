# Issue #744: TASK-C575-1: the lab format is defined and the first combinational labs ship — starter circuit, exercise prose, grading vectors, and a CI lane that grades the reference green and a planted defect red
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core idea — settle a lab format by building two real labs, gated on a reference-green/defect-red CI check — is sound and appropriately scoped as a first task. But the issue leans on two sibling issues (#552, #300) in ways that don't hold up against those issues' own text, it silently drops the one acceptance criterion (#575 AC-4, the non-author time-budget review) that would keep the "stated student time budget" honest, and its central grading acceptance criterion is worded loosely enough to be satisfied by a check materially weaker than the one #300 exists to fix.

## Findings, most severe first

### 1. (High) The claim that "the build-along lesson shape is consumed from #552, not redefined" contradicts #552's own recorded boundary

#744's Boundary section states: "The build-along lesson shape is consumed from #552, not redefined." But #552 (FEAT-C27-5) is explicitly about *stepped, hand-holding, draw-this/wire-that/run-it* tutorials for the three circuits in #548's curated Examples menu — content designed to be "completable by following on-screen prompts only, with no external reading" (#552 AC-2). #552's own boundary note (added by the maintainer, 2026-08-04) says in so many words:

> "CAP-33 (#517) keeps textbook-mapped lab packs, instructor materials and the assignment workflow — this feature must not grow into them."

#575 (#744's parent feature, CAP-33's content-authoring slice) is exactly that "textbook-mapped lab pack." So the issue that #744 says it will draw its lesson shape from has, on the record, disclaimed being a source for #744's kind of content. A build-along tutorial that walks a student through the exact wiring of the answer is also in tension with a *graded* exercise: #744's labs need students to build something independently and be scored against grading vectors, which is a different pedagogical contract than "watch here, wire that" spoon-feeding. Nothing in #744 explains which sliver of #552's "shape" (step granularity? UI chrome? prose style?) is actually meant to transfer, or how a build-along walkthrough coexists with a submission that must be gradeable as right-or-wrong.

**Recommendation:** either drop the #552 reference and describe the lab's own exercise-prose shape from scratch, or specify concretely which elements of #552 are reused (e.g., "one step ≤ one caption" prose granularity) while explicitly excluding the parts of #552 (the guided answer-reveal) that would defeat grading validity.

### 2. (High) The task silently drops #575's AC-4 (the only mechanism that keeps "stated student time budget" honest), and #744 never says so

#575 AC-3 ("Each lab declares the chapter it maps to and a stated student time budget") is carried into #744 verbatim as its own AC-3. But #575 AC-4 — "A non-author completes each lab within its stated time budget; per KC-33-2, a lab failing two consecutive such reviews is pulled from the pack rather than padding the count" — is the *only* thing in the parent feature that verifies the time budget is real rather than invented. #744 does not mention AC-4 at all, does not defer it explicitly to a later task, and does not carry any weaker substitute. As written, #744's own acceptance criteria (lab layout, CI red/green, chapter+budget declaration, provenance) are fully satisfiable by an author who writes "15 minutes" on a lab that actually takes three hours — nothing in #744 checks it, and there's no forward reference telling a reviewer which future TASK-C575-N will.

This is exactly the kind of quiet criterion-drop the fleet should catch: #575's own comment thread (2026-08-04) explicitly names AC-4's "pull rather than pad" rule as load-bearing for the whole pack's credibility, yet the first task that ships actual labs doesn't inherit it or flag its absence.

**Recommendation:** add an explicit line noting AC-4 (time-budget review) is out of scope for this task and naming which future TASK-C575-N owns it, so the gap is a decision rather than a silent drop.

### 3. (Medium) AC-2's grading bar is worded to accept exactly the kind of weak check #300/CAP-06 exists to replace

AC-2: "A CI lane loads, simulates and autogrades each lab: the reference solution is green and a planted-defect variant is red, with the failing test named." This requires exactly *one* planted-defect variant per lab to be caught. #300 (CAP-06), which #744's own Boundary section cites as the grading engine, exists specifically because `examples/autograde/autograde.py` currently greps three literal stdout lines for one input vector — "a submission wrong on every other vector and right on that one passes" (#300's own Abstract). #744's AC-2 has the identical shape: pass one reference, fail one specific defect, done. A lab could ship with grading vectors that happen to distinguish the reference from the one planted bug chosen by the same author, while covering almost none of the input space a real (wrong) student submission would explore. Nothing in AC-2 asks for defect diversity, vector coverage, or an independent (non-author-chosen) defect class.

**Recommendation:** require at least two or three planted-defect variants spanning distinct fault classes (e.g., wrong gate, swapped input order, off-by-one timing) per lab, or explicitly state that deeper coverage is deferred to #300 landing and name the follow-up.

### 4. (Medium) `ordering_after: [300, 552]` sits awkwardly against the Boundary section's claim that #300 is out-of-scope plumbing

The task's own YAML front matter declares `ordering_after: [300, 552]`, i.e., #744 should not start (or at least not ship) until both land. But the Boundary section says "the grading engine is #300 CAP-06" — language that reads as "not our concern here." If #744's CI lane can be built entirely on the *existing*, already-documented `-t`/exit-status contract (`docs/batch-interface.md` §1, already normative and implemented today), then the `ordering_after: [300]` dependency is unnecessary and blocks a shippable task on an unrelated, still-open capstone (#300 is open, unassigned to any landed PR, per `mcp__github__issue_read`). If #744 genuinely needs #300's not-yet-built verdict/counterexample machinery, that contradicts the Boundary section's "plumbing, not our concern" framing, and #744 should say what part of #300 it needs. Either way the issue doesn't reconcile the two statements, and as filed a contributor could reasonably start work today (the underlying `-t` mechanism already exists) in violation of the stated ordering, or stall indefinitely waiting on an open capstone the Boundary text says isn't this task's problem.

**Recommendation:** state explicitly whether #744's CI lane targets the current `-t`/exit-status contract (in which case drop #300 from `ordering_after`) or #300's future verdict artifacts (in which case explain why that's compatible with calling #300 "plumbing" out of scope).

### 5. (Low) "A lab directory layout is specified in tree" names no path, format, or owner document

AC-1 requires a directory layout to be "specified in tree," but the issue gives no proposed location (`examples/labs/`? a new `labs/` at repo root? under the not-yet-existing `resources/samples/` that #548/#73 are separately building?), no file-naming scheme, and no pointer to where the spec itself should live (a new `docs/*.md`, following the project's normative-doc convention, or just directory structure implied by two example labs). Two contributors could satisfy the literal AC by shipping two differently-shaped lab directories with no written spec at all ("the layout two labs happen to conform to" is not the same as "a layout specified in tree"). This also risks colliding with #548's `resources/samples/` convention, which is being defined concurrently and explicitly reserves that path for the curated Examples menu, not graded labs — the two efforts should either share or explicitly diverge from that namespace, and #744 doesn't say which.

**Recommendation:** name the target path and require a short normative doc (mirroring `docs/batch-interface.md`'s style) rather than relying on two examples to imply a spec, and cross-reference #548's `resources/samples/` decision to avoid an accidental namespace collision.

### 6. (Low) "provenance statement... is recorded" (AC-5) has no content requirement or independent check

AC-5 requires original content and "the provenance statement for these labs is recorded," but doesn't say what the statement must contain (author, date, explicit confirmation no DEEDS/Donzellini text or figures were consulted vs. merely not copied verbatim?) or who reviews it. Since the same person authoring the lab also self-attests its provenance, the criterion is satisfiable by a single unverified sentence. Given the parent feature (#575) explicitly frames this pack as targeting DEEDS/Donzellini-syllabus instructors, a closer paraphrase risk (structure, exercise sequencing, problem framing) exists even where prose is "original," and AC-5 as worded doesn't guard against that milder form of derivation.

**Recommendation:** specify the provenance statement's minimum content (what was consulted, what wasn't, chapter-mapping rationale) and consider requiring it to be reviewed by someone other than the lab's author, consistent with the review discipline #575 AC-4 establishes elsewhere in the same feature.

## What's solid

- Scoping two labs (not the full eight of #575 AC-1) to "settle the shape" first is a reasonable incremental slice, and is explicitly framed as such.
- The Boundary section's exclusions (platform delivery is #502, browsable Examples menu is #548) are accurate against those issues' own text and don't conflict.
- Piggybacking on the existing, already-documented `-t` test-vector grammar (`docs/batch-interface.md` §2) rather than inventing a new grading mechanism is the right technical instinct.

## Note

No repository artifacts for labs, a lab-directory convention, or `DEEDS`/`Donzellini` content exist yet (`resources/samples/` does not exist; `examples/` contains only `examples/autograde/`), so this task is starting from a genuinely empty slate — which raises the stakes on findings #1 and #5 (there's no existing convention to fall back on if the #552 borrowing or the directory layout turn out to be wrong).
