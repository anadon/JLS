# Issue #480: TASK-0091: "can this design be built as a board?" gets a named-rule answer per finding, with the gaps JLS cannot close reported rather than hidden
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core design — eight named rules over four already-produced inputs, a
report that explains rather than throws, `M4` demoted to WARNING because
JLS has no power model — is well reasoned and grounded in real code
(`PcfEmitter.java`, `docs/simulation-semantics.md`, `HdlModel.Direction`
all verified present and matching the quoted text at their cited lines).
But the issue has real, checkable defects: an incomplete dependency
graph that omits real transitive blockers, a data-contract attribution
conflict, and an exit-code contract that collides with sibling tasks'
contracts for the exact same underlying condition. These are fixable
without redesigning the gate, but they should be fixed before pickup.

## Findings, most severe first

### 1. [HIGH] Exit-code contract collision with sibling tasks re-reporting the same condition

§7.10 stage 3 maps the gate's own findings to exit codes: `ERROR → 2`,
everything else → `0`. `docs/batch-interface.md`'s table (verified in
the repo) defines status `2` as **"usage error"** with a one-line
diagnostic, the same class as "unknown option" or "missing argument."
The gate hijacks that class for "your design fails a manufacturability
rule," which is not a usage error by any reading of the contract it
claims to reuse.

Worse, `M6_FANOUT_EXCEEDED` explicitly **re-reports** `LoadingReport`
from #430 (TASK-0088) rather than recomputing it ("`M6` re-reports it;
re-implementing it here would give JLS two loading checks that can
disagree"). But #430's own exit mapping (§7.10 of #430, verified) is:

> `exit = 1` if `∃N: V(N) = OVER_FANOUT ∨ contended ∨ undriven`; `0` otherwise

and #394 (TASK-0086, sibling under the same feature #365) independently
specifies: *"Exit status is 1 ... when any element is unrealized."*
So the identical underlying defect — a net over its driver's fan-out
capacity — exits **1** when discovered via `-pack`'s `loading.txt` and
exits **2** when the same finding is re-reported as `M6` via
`-manufacturability`. A CI script that branches on exit code per
`docs/batch-interface.md`'s documented meaning will treat one as
"runtime failure, my design is broken" and the other as "usage error,
check my command line" — for the same defect in the same tree.

**Recommendation.** Either use exit `1` (the "runtime failure" class,
consistent with #394 and #430) for ERROR findings, or, if `2` is kept,
amend `docs/batch-interface.md`'s table itself to redefine what `2`
means for this flag and say explicitly why the gate departs from its
own siblings' precedent.

### 2. [HIGH] The blocked_by list omits real transitive prerequisites

`blocked_by` names #400 (TASK-0085), #394 (TASK-0086), #430
(TASK-0088). But #430's own `blocked_by` text (verified by fetching
#430) says: *"TASK-0087 (width decomposition and the cascade rule,
owned by FEAT-041) is the third prerequisite and is not yet filed."*
That task has since been filed as **#427**, and #427 is itself
`blocked_by` #400 **and** TASK-0007, which has since been filed as
**#468** (still open). Neither #427 nor #468 appears anywhere in
#480's `blocked_by`; #427 appears only in `related`, described merely
as "the mechanism M8 names as the resolution" for a WARNING/ERROR
message string — not as a hard prerequisite, even though #480's own
`M8_WORD_LEVEL_NO_PACKAGE` predicate is defined in terms of "no cascade
decomposition," a concept #427 introduces and #480 does not.

`M8` and, as argued in finding 3, the very shape of `PhysicalNetlist`
that `M1`/`M3`/`M7` range over, are downstream of #427 and #468/#373
(TASK-0008, stable net naming — also unlisted). The Completion
Criteria's own checklist item — *"Every `blocked_by` entry ... has
landed, or the dependency was waived"* — cannot catch this gap, because
the entries that would need to be checked are not in the list.

**Recommendation.** Add #427 (and, transitively, #468/#373 if they
gate #427's landing) to `blocked_by`, or explicitly waive them with a
stated reason per the project's own rule 10.

### 3. [MEDIUM] Data-contract attribution conflict on `PhysicalNetlist`

§7.3 states: *"`PhysicalNetlist` — the net partition with terminals,
from the emitter path. Structure authoritatively defined by [#460]'s
emitter work."* But #427 (TASK-0087), fetched and read in full, states
its own deliverable is *"a `PhysicalNet` sum type over `SchematicNet`
and `SyntheticNet`, [in] the netlist IR"* — i.e., #427 is the task that
actually defines the type `M1`, `M3`, `M7` and `M8` all read from
(`N ∈ PhysicalNetlist` in §7.10's math). #460 (the KiCad emitter) is a
sibling **consumer** of that IR, not its definer, per #427's Related
Work table ("#329 and #366 [read: the gate under #366] both index
slice order and both read `SyntheticNet`"). Attributing the schema to
the wrong issue compounds finding 2: a reader who checks only #460
(which is *not* in `blocked_by` either, correctly, since it's a
sibling, not a prerequisite) will conclude the input type is already
specified by a landed-adjacent task, when its actual definer (#427) is
unlanded and unlisted as a blocker.

**Recommendation.** Correct §7.3's citation to #427, and confirm
whether `M7`'s tri-state analysis is meant to run over synthetic
(post-decomposition) carry nets as well as schematic ones — the issue
is silent on this, and #427 §7.6 explicitly declares synthetic nets
carry power/ground-adjacent semantics that `M7`'s predicate doesn't
mention.

### 4. [MEDIUM] Evidentiary base sits outside the project's default branch

`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is **not
an ancestor of `origin/master`** (verified: `git merge-base
--is-ancestor 2d0ca9d... origin/master` fails); it exists only on
`origin/claude/jls-virtual-hardware-linux-njsoma`. `M1`'s ERROR
severity rests on a specific external claim — KiCad 10.0's
`board_netlist_updater.cpp` refusing components with an empty
footprint — sourced from `docs/plan/evidence/format-adoption-plan.md`,
which was committed only on that same orphan branch (`3a81a4a...`) and
**later deleted from it** (`742da74`, "remove the planning corpus now
that it is encoded in issues" — verified via `git log --all`). That
document does not exist anywhere in `origin/master`'s history. The
code-level claims (O1–O6) still hold against the actual current
`master` tip — I independently re-verified `PcfEmitter.java`,
`HdlModel.java`, `docs/simulation-semantics.md`, and
`CliFlagTableTest.java` against the checked-out tree, which is
content-identical to `origin/master` for the cited paths — but the
issue's own Completion Criteria demand *"every cited permalink resolves
on the default branch at close — no branch-path links, no deleted
docs,"* a bar the issue's own evidence base does not currently clear
for the KiCad-refusal claim specifically.

**Recommendation.** Re-anchor `evidence_commit` to a commit reachable
from `origin/master`, or inline the KiCad-refusal citation (version,
file, and the relevant snippet) directly into this issue so it survives
independent of the orphan branch's fate.

### 5. [LOW, self-disclosed] Filed with a load-bearing decision still marked "blocks execution"

Open Question 1 — whether `M4` is reported once per package or once
per unconnected pin — is explicitly marked *"Blocks execution"* of
`M4`'s predicate shape, and the Completion Criteria repeat this
("Open Question 1 resolved before `M4`'s predicate is written"). The
issue is honest about this, but it means the issue as filed ships with
an unresolved design decision on its only genuinely novel-judgment rule
(the other seven are largely mechanical translations of existing
schema). Filing before that ratification round-trips is consistent
with this project's stated process (see #400's identical pattern), so
this is a process note rather than a defect, but a reviewer picking
this up should not assume "filed" means "ready to implement `M4`."

### 6. [LOW] "Three" boundary rules asserted, only two ever named

The abstract states *"three of them exist specifically to say where
JLS stops,"* then the only sentence that names specific rules in that
role says *"`M4` and `M8` exist so that where JLS stops is visible."*
The third is presumably `M7` (also unresolvable until an external
dependency lands — see below), but the document never says so
explicitly, leaving "three" uncorroborated by its own elaboration.

Relatedly: `M7`'s Threat T5 and Open Question 2 both assume `#474`
(TASK-0049, adding `INOUT` to `HdlModel.Direction`) will eventually
land and let `M7` be "re-examined, not left as a permanent ERROR."
As of this review (2026-08-09), **#474 is closed with
`state_reason: duplicate`** (closed 2026-08-08, the day before this
review and five days after #480 was filed) — verified by fetching
#474. It is not merged; it was superseded by an unnamed duplicate.
`M7`'s stated escape path is therefore currently dangling: #480 has no
fallback if the duplicate that closed #474 never lands, or lands with
different scope.

**Recommendation.** Either name the third boundary rule explicitly, or
drop "three" to "two, with `M7` conditionally a third pending #474's
successor" and identify that successor issue.

### 7. [LOW] `P3`'s totality check is fixture-content-blind by the issue's own admission

`everyGateRuleHasAFixtureThatTripsIt()` only asserts each `GateRule`
constant appears in *some* finding from the committed fixture set.
Threat T4 concedes this directly: *"a badly built fixture could satisfy
[it] trivially,"* and defers the real check to a human reading the
PR's "rule-to-fixture table." That's a reasonable mitigation for a
one-time PR review, but it is not a regression guard — a fixture that
trips `M3` for an unrelated reason (e.g., a malformed footprint that
also happens to have too few pins) would keep `P3` green forever after
merge, silently drifting from "the fixture exercises the rule for the
right structural reason" without any test noticing.

## What's solid

- `M4`'s WARNING severity and its rationale (JLS has no power/energy
  model, verified against `docs/simulation-semantics.md` §2 and
  `Adder.resetPropDelay`'s dimensionless delay) is well-argued and
  matches the actual tree.
- The "report, don't throw" contract (H2) and its distinction from
  `PcfEmitter`'s all-or-nothing refusal (verified: `PcfEmitter.java`'s
  javadoc matches the quoted text exactly) is a coherent, precedented
  design choice, not an invention for this issue.
- `CliFlagTableTest`'s two drift tests (verified present at the cited
  method names) genuinely do make a new flag safe to add without
  hand-maintaining usage text — O6's claim holds.
- Explicit DRC/LVS exclusion (Threat T1) and the "no new primary data"
  discipline (H1) are stated clearly enough to be checkable at review
  time, which is the right shape for a scope boundary.

## Note

The dependency-graph and evidence-provenance issues (findings 2–4)
affect whether this specific issue is safe to *pick up* today; they do
not indicate the rule design itself is wrong.
