# Issue #602: TASK-C332-3: a cut net names the same signal on both sides, and that name does not depend on which partition it landed in
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#602 is a leaf task under #332 (FEAT-055, partitioned model / streaming
elaboration) requiring that boundary-net names agree on both sides of a cut
and derive from #336's (FEAT-004) stable net naming. The issue is open, has
no comments, and is well-anchored to its parent's invariant 4 and §3 formula
— but it depends on two artifacts that do not exist in the codebase yet, and
at least one of its acceptance criteria cannot be discharged with the
information its own prerequisite task (#600) has committed to producing.

## Findings, by severity

### 1. AC-3's naming dependency is on an unshipped feature, and the codebase still contradicts it (blocking)

AC-3 says: *"Net names are derived from #336's stable synthesized naming; no
naming scheme local to the partitioner is introduced."* But #336 itself is
still open, and its own body lists both children as `(unfiled)`:
`TASK-0007 (unfiled)` and `TASK-0008 (unfiled)` — the task that actually
freezes the naming function. Two of #336's Open Questions ("the digest
function" and "the stability promise's epoch") are recorded as **"Blocks
filing TASK-0008"**, i.e. #336 cannot even be split into an implementable
task yet.

The codebase confirms the naming function #602 is told to reuse does not
exist: `src/jls/hdl/HdlExporter.java` still synthesizes names from the
file-local, save-order-reassigned index, not from stable identity —
`group.name = names.synth("net_" + el.getID()` (line 346), `"reg_" +
el.getID()` (line 581), same pattern at lines 667, 695, 812, 945, 1026,
1255. `git grep -c stableId -- src/jls/hdl/` returns nothing. `#336`'s own
evidence block makes the same finding at the pinned commit.

`ordering_after: [..., 336]` in #602's YAML acknowledges the dependency
exists, but "ordering_after" is advisory sequencing, not a hard gate the
way `blocked_by` is used at the feature tier. As scoped, an agent could
pick up #602 today, find no stable-naming function to call, and either
stall or — worse — implement a local synthesized-naming shim "for now,"
which directly violates AC-3's second clause and is exactly the failure
#332 §7 calls a `REPLAN:` trigger.

**Recommendation:** convert the #336 dependency to a hard precondition
(explicitly: #602 cannot start implementation, only design, until TASK-0008
lands), or descope AC-3 to define an interim, clearly-labeled naming
contract with a recorded migration path once #336 ships.

### 2. AC-4 assumes an artifact contract that #600 (its own prerequisite) does not commit to

AC-4 requires: *"A boundary description whose two sides disagree on a name
is refused at load with both names and both parts in the diagnostic."* This
presupposes the boundary description carries **two independently-derived
per-part names** for each cut net, so a mismatch is representable at all.

But #600 (TASK-C332-1), which owns the artifact's schema, only commits to
naming *which* nets are cut: *"AC-2: The boundary description names exactly
the cut nets — those whose pin set intersects more than one part — as an
equality in both directions."* That is a set-membership guarantee, not a
per-side-redundant-naming guarantee. Nothing in #600's five ACs promises
the boundary description stores two names to compare.

This interacts with a real computability problem in #336's own naming
formula: `name(n) = "net_" + δ(min_⪯{sid(e) : e ∈ drv(n)})` — the minimum
stable id over *all* driving elements of the net. For a net whose drivers
span two parts (e.g. a tri-stated bus with one driver in each part), the
side that doesn't hold the globally-minimal-id driver cannot compute that
minimum from local information alone under the streaming, one-part-resident
load model (#332 §3, "Ephemeral: the resident part during elaboration, and
the index of boundary nets" — nothing entitles a part to the whole design).
Either the boundary description itself must carry enough of the driver-id
information for both sides to agree without seeing each other, or the
"disagreement" AC-4 checks for can never legitimately arise, which would
make AC-4 a permanently-vacuous check dressed up as a safety net — the same
failure mode #332 explicitly calls out for its own criterion 5 ("the
falsification guard... without it, byte-identity assertions can pass
vacuously").

**Recommendation:** before #602 starts, get an explicit commitment on #600
(or here) for exactly what per-net, per-part information the boundary
description carries, and confirm AC-4's disagreement path is reachable by a
concrete, realistic scenario (e.g. a hand-edited or tool-generated boundary
description with a stale name after an unrelated edit) — not only by
malformed input.

### 3. AC-1/AC-2's "generated design" has no owner and no existing infrastructure

Both AC-1 and AC-2 (and #332's own I2, and #600's AC-2) key their test
strategy on "a generated design with a known cut" / "over a generated
design, not by spot check." No such circuit-generation test utility exists
anywhere in the repository: `grep -rl "generated design\|GeneratedDesign\|
RandomCircuit\|RandomDesign"` across `test/` and `src/` returns nothing,
and there is no `test/**/*Random*`/`*Generator*` fixture. Every sibling
task under #332 (#600, #601, #604, #606, per the pattern) will presumably
need the same generator. No task's boundary notes name it as owned scope,
so #602 risks either building a one-off generator (duplicated four more
times by its siblings) or silently blocking on infrastructure nobody was
assigned to build.

**Recommendation:** #332 (or #600, as the artifact-form owner) should name
an owning task for a shared "generated design with declared cuts" test
fixture before any of TASK-C332-{2,3,4,5} start.

### 4. AC-2 is under-operationalized and gameable

AC-2: *"Re-partitioning the same design along a different cut leaves every
surviving net's name unchanged (#332 invariant 4)."* "A different cut" is
undefined — a trivially near-identical repartition (e.g. moving one
unrelated internal net between parts, touching no boundary net) would
satisfy the letter of AC-2 without exercising the property that actually
matters: names surviving a partition-membership change for nets that
*were* boundary nets, or that transition between boundary and internal.
Contrast with #336's own IC-3, which is concretely operationalized: *"export
a fixture; insert an element that drives nothing... zero pre-existing net
names moved."* #602 should hold itself to the same concreteness.

**Recommendation:** state AC-2 as: re-partition such that at least one
previously-internal net becomes a boundary net and at least one
previously-boundary net becomes internal; assert every surviving net's name
is unchanged across both partitionings.

### 5. Machine-block hygiene: `ordering_after` mixes a descriptive string with a bare integer

`ordering_after: ["TASK-C332-1 (the cut is what identity has to survive)",
336]`. Its sibling #600, filed the same day by the same author, uses pure
integers: `ordering_after: [319, 336]`. #332 and #336 both rely on
`blocked_by`/`blocks` being bare-integer arrays specifically so the "Link
phase" DAG walks recorded in their own comments/bodies can be verified
mechanically. As written, #602's edge to its actual prerequisite (#600) is
not resolvable by that tooling — a script walking `ordering_after` for a
cycle check or a readiness gate will not find issue 600 here.

**Recommendation:** normalize to `ordering_after: [600, 336]`.

### 6. Minor: no evidence anchor for the task's own premise

Unlike #332 and #336, which ground every claim with an `ABSENT at
<commit>` citation and exact file/line evidence, #602's body asserts the
problem ("a watched element is unnameable across a re-partition") without
citing any current code location or verified-absent grep. This is a minor
rigor gap relative to the corpus's own standard, not a correctness defect.

## What's solid

- The outcome statement is accurate to its source: #332's invariant 4 text
  ("A net's name does not depend on which partition it landed in") and the
  §3 boundary-net-identity formula are quoted correctly, not paraphrased
  into something the parent doesn't actually say.
- AC-3's ban on a partitioner-local naming scheme is the right governance
  guard given #336 exists specifically to prevent a second, disagreeing
  netlist partition/naming implementation.
- The "sibling of TASK-C332-2, independent of it" boundary note matches
  #332 §6's own sequencing claim almost verbatim — no drift there.
- AC-1's "asserted as an equality over the whole cut set, not sampled" is a
  good, specific anti-gaming clause — it's the sibling clauses (2 and 4)
  that fall short of the same bar.
