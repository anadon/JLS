# Issue #396: TASK-0093: the breadboard is checked against the schematic per discrepancy, and the placed arrangement can drive the simulation
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-0093 is a well-specified consistency-check and physical-binding task
riding on top of a dependency chain that is, as of today, largely fictional:
two of its three real prerequisites are open issues with no landed code, and
the third has no issue number at all. The document is honest about that
("blocks execution") but several of its own internal contracts do not survive
contact with either the current codebase or its own machine-readable
dependency block. Findings below are ranked by severity.

## Findings

**1. (High) The dependency chain this issue names cannot be completed today, and one hop is still unfiled.**
`blocked_by: [394, 387]` — both open (`#394` TASK-0086, `#387` TASK-0058),
neither landed; `grep -rn "PackPlan\|NetPartition" src/ test/` returns zero
hits, confirming neither's public type exists in the tree yet. The third
prerequisite, TASK-0092 (the breadboard canvas), has *no issue number* — the
issue says "being filed concurrently and a link pass adds its number." Its
owning feature #329 (FEAT-043), last updated the same day as this review,
still lists TASK-0092 as "not filed," itself gated behind TASK-0036 and
TASK-0105, also "not filed," both marked "Blocks filing children" in #329's
own Open Questions. Method step 1 of #396 — "Confirm #394, #387 and
TASK-0092 have landed" — is therefore unexecutable: two named issues are
open and the third doesn't exist as a filed issue to check the status of.
Recommendation: do not treat this issue as pickable; either withdraw it
until #394/#387/TASK-0092 land, or add an explicit "blocked, re-check before
pickup" label loop the tracker can act on.

**2. (High) `blocked_by` omits a real, load-bearing prerequisite: #468 (TASK-0007).**
§6 Materials and §7.4/H1 make the physical partition's identity with the
schematic `NetPartition` type the crux of the whole design ("That sameness
is the whole point... Refuted if the physical derivation cannot be expressed
as that type"). That type is defined by TASK-0007, which is filed as **#468**
and is itself open, unlanded, with `blocked_by: []` (so it could move
independently). #396's YAML `blocked_by: [394, 387]` never names it, so the
issue's own machine-readable dependency graph is inconsistent with its prose
engineering requirements — a reader (or an automated dependency walker)
trusting the YAML block would start work believing only two prerequisites
gate this task, not three. Recommendation: add `468` to `blocked_by` and
regenerate the mermaid graph in the same edit.

**3. (Medium) P8's exit-status choice conflicts with the project's own documented contract.**
§7.11/P8: "Exit status is 2 when any `ERROR`-severity discrepancy exists."
`docs/batch-interface.md` §1 codifies (and `JLSStart.usageError`,
`src/jls/JLSStart.java:1142-1147`, enforces): exit 0 = success, **1 =
runtime/content failure**, **2 = usage error**. A discrepancy in a circuit's
physical-vs-schematic wiring is a content-level finding, not a
command-line/usage problem — the sibling task #394 (TASK-0086) gets this
right, using exit 1 for "any element is unrealized." #396 as written would
make `-breadboard-check` the one batch mode whose content-failure exit code
collides with the meaning every other flag gives exit 2, and no rationale is
offered for the deviation. Recommendation: change P8/§7.11 to exit 1 (or
explicitly argue for and document the deviation in
`docs/batch-interface.md`, not just in this issue).

**4. (Medium) The rule that "justifies the whole task" is optional, and the Definition of Done cannot tell the difference.**
The abstract states plainly: "The rule that justifies the whole task,
`C6_CONTENTION`, is the finding the schematic *cannot* produce." §7.11 and
the Open Questions then make C6 conditional on #387 having landed, and the
totality test P2 is explicitly designed to accept an enum constant "marked
unimplemented" instead of a working fixture. Every checkbox in §14
(Completion Criteria) — including "P2 landed first; every rule constant has
a committed fixture that trips it, **or is explicitly marked
unimplemented**" and "Whether C6 shipped or was descoped is stated plainly
in the PR" — is satisfiable by shipping C1-C5 only and writing one sentence
in the PR. Given #387 is currently open (finding 1), this is not a remote
edge case: it is close to the default outcome. An executor can close every
box in this 500-line issue while delivering none of the capability that
motivated filing it. Recommendation: either block this issue on #387
landing (move it to `blocked_by`'s hard-gate list rather than a
"recommended default" escape hatch), or retitle/reframe the abstract so the
C1-C5 subset is honestly described as the deliverable when C6 is descoped.

**5. (Medium) The scope-justifying maintainer ruling (D9) is cited from a document that will not exist on the branch that merges.**
"Maintainer ruling D9 is why this work is in scope at all rather than
refused on audience fit," sourced to `docs/plan/evidence/BRIEF.md` §13. Per
issue #493 (filed against this exact evidence-pinning problem), `docs/plan/**`
— 195 files including `BRIEF.md` — is **absent from `master` entirely** and
"cannot be re-pinned at all... treat the quoted text in the issue body as
the only surviving copy." Confirmed locally: `ls docs/plan` fails on this
checkout. The load-bearing justification for this task's audience fit is
therefore unverifiable by anyone except by trusting this issue's own
paraphrase of a document that no longer exists anywhere reachable. This is
a process risk more than a code risk, but it means a future reviewer cannot
independently confirm the "K9/D9 progressive disclosure" argument the issue
leans on to avoid the audience-fit objection — they can only take the
issue's word for it. Recommendation: land D9's operative text as a permanent
recorded-decision issue (the pattern #98/#165/#221 already use) instead of
leaving it inside a doomed planning tree.

**6. (Low) Gameable severity table has no test tying rule severity to the ERROR/WARN split it depends on.**
Open Question 4 assigns severities ("C1, C2, C5, C6 are ERROR; C3, C4 are
WARN") as a "recommended default... Blocks execution," but nothing in §8
Method or §14 Completion Criteria requires a test pinning that mapping the
way P2 pins the rule-to-fixture mapping. A future edit could silently
reclassify a rule's severity (e.g. downgrade C1_SPLIT_NET to WARN) without
breaking any stated acceptance test, changing the exit-2 (or, per finding 3,
exit-1) behavior for graders relying on it. Recommendation: add a
`RuleSeverityTest` alongside `ConsistencyCheckTest.everyConsistencyRuleHasAFixtureThatTripsIt`.

## What's solid

- **O1/O2's central motivating bug is real and well-demonstrated.** The
  file-order-dependent bus-conflict resolution (`WireNet.java` lines ~22-24
  insertion-order `LinkedHashSet`s and the `propagate` comment at ~450-453)
  matches the current tree, and the "swap two WireEnd blocks, get a
  different answer" experiment is exactly the kind of observation this
  format is supposed to produce.
- **H4 / P7 (`SCHEMATIC` stays the default, whole golden corpus byte-identical)** is
  the right conservative default and is testable as stated.
- **The C1/C2 split-net/merged-net formalization (§7.10) as the two
  directions of φ failing to be a bijection is a clean, checkable
  definition** — no complaint.
- **The explicit "never emit C6 speculatively" rule** is the correct
  response to O2/O4's finding that there is no strength-based resolution to
  back a contention claim yet — the problem is only that the Definition of
  Done doesn't enforce it strongly enough (finding 4).

## Verdict rationale

`needs-rework`, not `should-not-proceed`: the technical design (net
partition as shared type, per-discrepancy report, explicit SCHEMATIC
default) is sound and worth keeping. But the issue is not pickable as
written — its own prerequisite list is incomplete (finding 2) and partly
unfileable (finding 1), its exit-status contract conflicts with the rest of
the CLI (finding 3), and its headline feature can be silently dropped while
every stated acceptance gate still passes green (finding 4). These need
fixing in the issue text and machine block before an executor starts, not
after.
