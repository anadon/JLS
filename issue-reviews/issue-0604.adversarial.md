# Issue #604: TASK-C332-4: a cut that crosses a combinational cycle is refused by name at partition time instead of simulated to a different answer
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Context

#604 is TASK-C332-4, the fourth of five child tasks under #332 (FEAT-055, "a
circuit exists as parts that load independently"). It is the falsification
guard for #332's equivalence harness (TASK-C332-5, #606): without a refusal
on cross-partition combinational cycles, byte-identity between the
partitioned and single-file forms could pass vacuously. I read #332, #333,
#601, #602, and the existing #606 review to check #604 against its declared
dependency chain, and grepped the tree for the infrastructure it claims to
reuse.

## Findings, most severe first

**1. [High] The central boundary note is false against the tree: no existing combinational-dependency graph exists for this task to "read".**
The issue states: "The combinational dependency graph this reads is the
existing one; this task does not introduce a second cycle analysis." I
searched the whole source tree (`grep -rn "combinational\|oscillat" src/`)
and the only structural hit is `src/jls/hdl/layout/LayoutGraph.java:17-21`,
which marks a register-output edge as a `feedback` back-edge purely so the
HDL-import **schematic layouter** (issue #61/#62) can route it against the
left-to-right flow direction — it is a rendering aid over an already-
imported netlist, not a correctness-oriented dependency graph over the
live circuit, and it has no notion of "spans two or more parts." Nothing in
`src/jls/sim/` builds or walks a combinational dependency graph either
(`grep -i cycle src/jls/sim/Simulator.java` returns only javadoc about time
limits); `docs/simulation-semantics.md` documents no cycle-detection
semantics at all. Today a combinational cycle is bounded only by the
simulator's wall-clock time limit (issue #25), not detected. This directly
contradicts the visionary review of #332's own grep ("`grep -rn
"combinational\|oscillat" src/` returns nothing relevant: JLS has **no**
combinational-cycle analysis"). **Recommendation:** strike the "this task
does not introduce a second cycle analysis" claim, or replace it with an
honest statement that this task builds the *first* one — which changes the
task's actual scope and cost.

**2. [High] AC-1 through AC-4 describe behavior of a partitioner and a cut-declaration syntax that are specified nowhere, including in this issue.**
"Declaring a cut" (AC-1), "partitioning stops" (Outcome), and "the parts
involved" (AC-1) all presume an author-facing mechanism for declaring a
cut and a representation of "parts" to check membership against — but
`git grep -rliE "PartitionSet|BoundaryDescription|streamingElaborat" --
src/ test/` returns zero files at HEAD (confirmed independently, matching
#332's own review). #604 never cites TASK-C332-1 (#600, the part-file
artifact format) as a dependency at all — its `ordering_after` names only
TASK-C332-3 (#602). Without a pinned artifact form to check cuts against,
"the elements and the parts involved" in AC-1's diagnostic has no concrete
referent yet, and an implementer could satisfy the letter of AC-1 against
any ad hoc, test-only partition representation invented for the occasion.
This is the same interface-invocation gap the sibling review of #606
(TASK-C332-5) already found in that task for the same reason.
**Recommendation:** add #600 to `ordering_after` explicitly (it is only
reachable today by chasing #602's own `ordering_after` two hops away), and
state what "declaring a cut" means operationally before this task is
picked up.

**3. [Medium] band_mw "2" looks underpriced once findings 1 and 2 are accounted for.**
Building a from-scratch graph cycle-detector (e.g., an SCC pass such as
Tarjan's over the combinational subgraph) that additionally must partition-
tag every node, distinguish intra-part cycles (AC-3, must NOT refuse) from
cross-part cycles (AC-1, must refuse), and produce a diagnostic naming both
elements and parts, is a materially larger unit of work than "2 mw" once
there is no existing graph to build it on top of. The estimate is
consistent with the issue's own (mistaken) premise that it is reusing
existing infrastructure; it should be re-derived once finding 1 is
resolved.

**4. [Medium] AC-4 is a process claim, not a testable behavior, and has no described enforcement mechanism.**
"The refusal is armed and its test is green **before** TASK-C332-5's
equivalence harness is trusted, and that ordering is recorded" describes a
sequencing fact about two separate PRs/tasks, not an assertion about #604's
own code. Nothing in #604 says how "the ordering is recorded" is verified
rather than merely asserted — no shared marker, no CI gate, no explicit
precondition class is named. As written, a comment on #332 or #606 stating
"TASK-C332-4 landed first" satisfies AC-4's text without anything actually
preventing #606's harness from being trusted first. This is the identical
gameability pattern the sibling #606 review flagged in that issue's AC-3
("the harness records that dependency ... unspecified and gameable").
**Recommendation:** either drop AC-4 from #604 (it is arguably #606's and
#332's obligation, not this task's, since #606's own AC-3 already restates
it) or specify a concrete enforcement mechanism.

**5. [Low] The prerequisite is cited only by task-id, never by issue number, inside #604's own body.**
`ordering_after: ["TASK-C332-3 (...)"]` never resolves to "#602" anywhere in
the issue text. An implementer working from #604 alone has no way to find
the actual GitHub issue without searching #332 or the tracker — a small but
real friction point, and the same traceability gap noted in the #332 and
#606 reviews for this same task family.

**6. [Low] Inherits the family-wide tracking staleness, nothing #604-specific to add.**
Like #600–#606, #604 was filed as a child of #332 roughly eight hours after
#332's last comment, and #332's `planned_tasks`/Decomposition table still
says "not filed, no id" for all five scopes with no `REPLAN:` posted (see
the #332 review, finding 1). This doesn't invalidate #604's own content but
means #332's tracked state cannot currently be used to discover #604 exists.

## What's solid

- The scope boundary — "refusal is a property of the partitioner, not of
  the transport layer... #332 §2 rejects pushing it into FEAT-056 (#333)
  explicitly" — is verified accurate: #332 §2 rejection-3 states exactly
  this, and #333's own "out of scope" list names FEAT-055 as owning
  partitioning, not itself. Correctly drawn and consistent across issues.
- AC-3 ("a cycle wholly inside one part is not refused; the check is on
  cycles that span, not on cycles") is a precise, testable boundary
  condition that correctly preserves today's behavior for ordinary
  single-file designs with feedback loops.
- AC-2 ("no simulation runs on a refused partitioning") is clear and
  directly checkable by a unit test asserting the run path is never
  entered.
- The task's stated role as #332's falsification guard, and the ordering
  requirement relative to TASK-C332-5, is consistent with #332's own
  Completion Criteria checklist item and with #606's own AC-3 — the two
  sibling issues agree with each other and with the parent on this point.

## Verdict rationale

`needs-rework`. The task's boundary claim that it reuses an existing
combinational dependency graph is false against the current tree — nothing
in `src/` performs correctness-oriented cycle detection, and the one
"combinational cycle" hit that exists (`LayoutGraph.java`) is a schematic-
layout aid for an unrelated HDL-import feature. That false premise
understates both scope and cost, and combined with the missing direct
dependency on TASK-C332-1's artifact form (#600) and the unenforceable
process criterion in AC-4, the issue is not yet buildable or testable as
specified. Fix by correcting the reuse claim, citing #600 directly, and
either dropping or concretizing AC-4.
