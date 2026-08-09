# Issue #366: FEAT-042: a drawn circuit leaves JLS as a schematic and netlist a real PCB tool opens without hand editing, and a named-rule gate says whether the board can actually be built
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a well-structured feature issue with real precedent (`PcfEmitter.java`), clean scope
boundaries, and honestly-flagged weak points. But the issue's own governance apparatus — the
machine block, the Open Questions, and the Completion Criteria — has already drifted out of
sync with events that happened in its own comment thread and in the issues it cites as
authoritative. None of these are hypothetical; each is independently verified below against
the live tracker and the repository.

## Findings, most severe first

**1. The decision authority this issue names for its central open question no longer exists.**
Open Question 1 states the schematic-vs-netlist route decision "belongs to CAP-13 (#307)…
and it must be made **before either task is funded**," and Completion Criteria item 2 requires
"Open Question 1 resolved by CAP-13 (#307)." But #307 was closed 2026-08-03 as a duplicate of
#298 ("Closing as duplicate of #298 per the August 2026 product review... this capstone is
structurally a variant of #298"). #366's own second comment, dated 2026-08-04 — *after* #307
closed — still treats #307 as live and never mentions the closure. Nobody has redirected Open
Question 1 or the DoD bullet to #298. As written today, the issue's completion criteria point
at a dead end.
*Recommendation:* edit Open Question 1 and the DoD bullet to name #298, or strike the #307
reference and record where the route decision now actually lives.

**2. The funding gate Open Question 1 sets was silently bypassed.** The same Open Question says
resolving it "**Blocks funding TASK-0089 and TASK-0090**." Yet TASK-0089 is already filed in
full (issue #460, complete with hypotheses, predictions, and a 20+ item DoD) — funded and
scoped despite the stated blocker never being lifted on #366. Worse, #460 itself asserts the
route question as settled fact: "TASK-0090 (the schematic path) is ranked ahead of this one in
`fmt-kicad-geda.md` §7... precisely because of the footprint trap," treating a sequencing
decision as already made by a document, while #366 still lists it as an unresolved
Open Question. The two issues disagree about whether the question is open.
*Recommendation:* either strike the "blocks funding" language as inoperative, or explain in a
REPLAN comment why TASK-0089 was funded anyway.

**3. The machine block contradicts this issue's own comment thread.** The YAML block still
carries `requires_tasks: []` and `planned_tasks` listing all four tasks as "not yet filed" —
including "TASK-0089 (not yet filed)" and "TASK-0091 (not yet filed)." But the issue's second
comment (2026-08-04, `#issuecomment-5181360229`) records: "`#366 = #460 + #461 + #480`" —
i.e., three of the four tasks are already filed. Completion Criteria item 1 requires
`planned_tasks` to be "empty (each resolved to a filed issue or descoped)," and the issue's own
Re-planning Protocol requires "Update § 2's roster and `planned_tasks` in the same edit as the
`REPLAN:` comment" whenever a child splits/files — that edit never happened. A reader or
scheduler consulting only the machine block (the thing meant to be machine-readable) gets
stale status five days after the correction was recorded elsewhere on the same issue.
*Recommendation:* sync the machine block and § 2 table to the filed numbers (#460, #461, #480,
and #400 for the shared TASK-0085) in the same edit that acknowledges this review.

**4. The dedup comment's own accounting drops a task without reconciling the parent.**
The "#366 = #460 + #461 + #480" equation covers only TASK-0089/0090/0091 and omits TASK-0085
(filed as #400, "shared with FEAT-040"). That omission is defensible on its own (TASK-0085's
primary home may be #349), but nothing on #366 says so — the decomposition table in § 2 still
lists TASK-0085 as one of *this* issue's four children, unreconciled with the three-task dedup
comment. A reader has two different, uncorrelated counts of how many tasks close this feature.
*Recommendation:* state explicitly whether TASK-0085/#400 counts toward #366's own closure or
is discharged purely via #349.

**5. A load-bearing evidence citation has been deleted from the repository.** § 6 and Cost quote
`docs/plan/evidence/format-adoption-plan.md` at length — including the entire gEDA/KiCad
row that justifies the chosen artifact format and the cost band — and state it "landed in
`3a81a4a7d6a0f108ec201e632732d308cc02b3fc`." Verified: that commit exists, but a later commit,
`742da74` ("docs: remove the planning corpus now that it is encoded in issues"), deletes the
entire `docs/plan/evidence/` tree. `docs/plan/evidence/format-adoption-plan.md` does not exist
at HEAD or on `origin/master`. Completion Criteria explicitly requires "Every cited evidence
document and permalink resolves on the default branch at close" — as things stand this is
false, and since the corpus was *deliberately* removed as redundant with the issues, it may
never become true again unless someone restores this one file for #366's sake.
*Recommendation:* either restore the cited evidence file (or the specific quoted rows) before
close, or replace the citation with the load-bearing sentences transcribed directly into #366
so the claim doesn't depend on a file the maintainer already chose to delete.

**6. Criteria 1 and 2 are a one-time manual check with no drift detection, and the issue admits
it.** § 5 states plainly: "Recorded manual procedure; no automation exists. This is the
weakest check in the plan unless the tool is armed in CI." The DoD only requires the manual
open/import be performed once, "at a named commit," against KiCad 10.0 sources as read (not
run) at filing time. KiCad and lepton-eda are both active projects; a later importer change
(e.g. to embedded-symbol handling, which the issue itself calls "an unfalsified premise… never
been run") would silently invalidate the "verified" claim with nothing in CI to catch it. This
is exactly the "verification could pass while the real goal fails" pattern: the artifact could
regress against a newer KiCad release and the test suite would stay green.
*Recommendation:* pin the tested KiCad/lepton-eda version in the DoD text (the issue leaves
this to Open Question 4, but that question's own "blocks nothing" framing undersells the risk),
and file the CI-armed successor now rather than leaving it purely aspirational.

**7. Disposition totality (criterion 5 / invariant 4) is satisfiable by a table that places
nothing.** The test only asserts every element type has *some* disposition — placement or
`CANNOT_PLACE(reason)` — with no floor on how many types must actually be placeable. An
implementation could mark every element `CANNOT_PLACE` and pass this criterion while producing
a gate/emitter pair that can never emit a non-trivial board, defeating § 1's entire capability
statement while every named test is green.
*Recommendation:* add an integration criterion asserting a minimum realistic fixture (e.g. the
existing four-NAND fixture referenced in #460) actually places successfully end to end.

**8. "The gate gates" (criterion 6) needs only one rule to be true.** § 5's criterion 6 requires
"a fixture that violates a named rule is refused" — singular. Nothing in § 4 or § 5 lower-bounds
the *set* of rules the gate must check (footprint presence is explicitly load-bearing per the
cited KiCad behavior in #460, but nothing else is named as mandatory here). A gate that checks
one rule and rubber-stamps everything else satisfies the letter of criteria 5, 6, and 7 while
leaving the stated purpose — "whether the design as drawn can actually be fabricated" — largely
unanswered.
*Recommendation:* enumerate a minimum required rule set in § 1 or § 5 rather than leaving "a
named rule" implicitly singular.

## What holds up

- The core precedent claim is real: `src/jls/hdl/board/PcfEmitter.java` and
  `test/resources/hdl/board/blinky_icestick.pcf` exist exactly as cited, and the "no physical
  vocabulary in shipped source" grep (`footprint|refdes|pinout`) still returns zero hits at
  current HEAD, not just at the pinned evidence commit.
- Scope boundaries are clean and correctly deflect into sibling features (place/route and
  gerbers, the net partition, part-library curation, packing/refdes, the breadboard canvas, SI
  constraints) without obvious overlap.
- The `blocked_by: [336, 349, 365]` dependency edges are accurate and correctly mirrored on all
  three target issues (verified directly).
- Global invariant 3 ("an emitter returns the complete artifact or nothing") and invariant 1
  ("no fourth copy of the net partition") are the right invariants for this class of bug and
  are stated as testable properties, not aspirations.

## Verdict rationale

The technical design is sound, but the issue's self-governance has already failed once in a
verifiable, checkable way — a closed decision authority still cited as live, a funding gate
bypassed without record, and a stale machine block contradicting the issue's own comments —
plus a load-bearing citation that the maintainer has since deleted from the repository. These
are exactly the failure modes this project's own tracker discipline (REPLAN comments, roster
sync, evidence-permalink checks) was built to catch, and they were not caught here. Recommend
a REPLAN pass reconciling the machine block, Open Question 1, and the evidence citations before
any child task proceeds further.
