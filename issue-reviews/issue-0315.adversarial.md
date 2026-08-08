# Issue #315: FEAT-001 (RESIDUAL): a thirty-sixth element type cannot ship until every registry-keyed table has a row for it
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

Two unfiled tasks: (TASK-0001) a committed inventory `docs/registry-keyed-tables.md` of every
"registry-, orientation-, edit-op- and save-tag-keyed table" plus totality tests for the ones that
lack one, and (TASK-0002) a reusable abstract JUnit base other tables extend. Grounding checked
against the current checkout: `src/jls/elem/ElementRegistry.java` exists with exactly 35
`new ElementType(...)` entries (`:38-77`, confirmed by `grep -c`), and the six test files the issue
cites (`ElementRegistryTest`, `JlsModulesBootTest`, `PaletteContractTest`,
`CapabilityInterfaceTest`, `PinFaceContractTest`, `HdlPolicyTest`) all do read `ElementRegistry.all()`
in the tree today. `PaletteContractTest.java:44-45`'s three-tag exemption set
(`SubCircuit`, `WireEnd`, `TestGen`) and its failure message are quoted accurately. Neither
`docs/registry-keyed-tables.md` nor any `RegistryTotalityTestBase` exists yet, matching the issue's
own "not filed" / "ABSENT" claims — the factual grounding is solid.

## Findings, most severe first

**1. (Correctness/internal contradiction) The scope silently expands past what the stated integration criterion can verify.**
The title and §1 frame the feature as "registry-keyed table" totality, and the primary falsification
method (I1) is "add a synthetic thirty-sixth `ElementType`... at least one totality test fails per
inventoried table." But §2/TASK-0001's actual scope text is "every registry-, orientation-,
edit-op- and save-tag-keyed table" — three additional key domains. `src/jls/core/Orientation.java`
is a fixed four-value enum (`UP/DOWN/LEFT/RIGHT`) that can never grow a 36th member the way
`ElementType` does, and `src/jls/edit/EditOp.java` is likewise a hand-authored, closed enum of
editor operations, unrelated to element count. Adding a synthetic 36th `ElementType` (I1's whole
mechanism) cannot exercise the totality of any orientation- or edit-op-keyed table — there is
nothing in that mutation for such a table to react to. So I1, the feature's single headline
integration criterion, verifies at most one of the four key domains the feature claims to cover.
A reviewer running exactly the I1 recipe at close-out would see it pass while orientation- and
edit-op-keyed tables (if any exist and are non-total) go completely unchecked.
Recommendation: split the "registry-keyed" claim (I1-testable) from the orientation/edit-op/save-tag
claims, each of which needs its own falsification recipe stated in §5, or drop the latter three from
scope and let them be filed as siblings.

**2. (Gameable acceptance criterion) I3, the only check on inventory completeness, has no defined method.**
The issue itself names the failure mode it's most worried about: "an incomplete inventory that still
looks green" (§2, rationale for not merging TASK-0001 with fix work). The only integration criterion
meant to catch exactly that is I3: "Diff the committed inventory against a mechanical sweep of `src/`
... The two agree, or every divergence is an explicitly recorded exemption." No pattern, tool,
grep expression, or AST-based heuristic is specified for what "mechanical sweep" means, and the same
person/agent who writes the inventory (TASK-0001) is implicitly the one who also builds the sweep
that is supposed to audit it. A sweep whose classification rule is "whatever the inventory author
decided counts" will trivially agree with the inventory by construction, and I3 is marked "does not
exist" with no spec to hold it to. Recommendation: pin down the sweep's detection heuristic (e.g. a
concrete grep/AST pattern for "collection literal indexed by an `ElementType`/tag/orientation/op
constant") in the Completion Criteria before TASK-0001 starts, ideally written by someone other than
the inventory's author, so I3 isn't self-certifying.

**3. (Stale machine block vs. own comments — internal contradiction) `blocks:` lists a closed issue, and the correction was never folded into the body.**
The issue body's machine block still reads `blocks: [314, 334, 336, 352, 358]`. Verified live via the
GitHub API: **#352 is `closed`, `state_reason: duplicate`** (closed 2026-08-04, merged into #170).
A same-day comment (2026-08-08, `id 5227261049`) already catches this and posts the correction
(`blocks: [314, 334, 336, 358]`), plus narrows the #314 and #334 edges — but per the issue's own DoD
("Machine block, roster table, and mermaid graph agree with reality at close") the body is supposed
to be the source of truth, and it was not edited. Anyone who reads only the body (as the machine block
is designed to be machine-read) gets a false blocking dependency on a closed issue. This is not
hypothetical staleness; it happened during the life of this very issue.
Recommendation: edit the machine block and mermaid graph in place per the comment's correction rather
than leaving the fix only in comment history — the issue's own rule A requires this at close, but
nothing stops it rotting further before then.

**4. (Feasibility/scope) Two unrelated "hygiene items" are folded into TASK-0001 with zero specification.**
§2: "Two hygiene items are folded into TASK-0001 rather than given their own ids, because they are the
same shape of hole: rejecting incompatible batch flag combinations at parse time, and the enumeration
discipline for edit-op kinds." Neither has a file/line anchor, an acceptance criterion, or a place in
§5's integration-criteria table — contrast with every other claim in this issue, which is unusually
disciplined about anchoring to `file:line`. "Same shape of hole" is asserted, not demonstrated: CLI
batch-flag parse-time validation is not obviously a "registry-keyed table" question at all. This is
scope creep riding in on a rationale that isn't argued.
Recommendation: either give each hygiene item its own one-line contract and evidence anchor (as every
other claim in this doc has), or strip them from TASK-0001 entirely and file separately.

**5. (Underspecified, blocks integration) Open Question 2 leaves the audit's cost unbounded.**
"The band prices the audit, not the fixes it finds... N fixed by the maintainer at TASK-0001's
inventory review... Blocks integration, not filing." This is honestly flagged, but note the
consequence: nothing in this issue prevents TASK-0001 from starting, running 1.5 weeks, and only then
discovering (at "inventory review") that the audit surfaced 40 missing rows across a dozen tables —
at which point the maintainer decides after the fact whether that's "small" (absorbed) or "large"
(re-planned as a HANDOFF). The absence of an a priori N means a contributor could pick this up, do the
full audit, and still not know if the ticket is "done" without a maintainer round-trip mid-task.
Recommendation: pick a provisional N now (e.g. "≤5 missing rows total absorbed here; more triggers an
automatic HANDOFF") even if it's later revised — cheap insurance against a stalled task.

**6. (Cost/band self-contradiction, already flagged by the issue, worth restating) Open Question 1: task-row sum (2.1 wk) exceeds the stated band (1-2 mw) and is left unreconciled by design** ("no row is adjusted to make the band true"). This is transparently self-reported, which is good practice, but it means the issue ships with a known-wrong top-line estimate that a planner skimming only the `tier`/band fields (not the Open Questions) would take at face value.

**7. (Provenance fragility, contextual) The evidentiary chain has already needed three rounds of correction.**
Comment history shows: evidence_commit `2d0ca9d` lives only on a to-be-deleted branch (comment 1);
a follow-up re-pin to "master `07a0bea`" (comment 4) turns out to *also* not be on master (comment 6,
same day); the final correction points to `8288226`. The issue's own citation discipline (exact
`file:line` anchors, `git show <hash>:<path>` commands) is a real strength, but the base commit it's
all anchored to has drifted three times in six days without the body's `evidence_commit:` field ever
being updated — it still reads `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`. A contributor who tries to
reproduce a citation by checking out `evidence_commit` literally, rather than reading all six comments
first, will fail. Not fatal, but the issue is a case study in why re-pinning by comment rather than by
body edit degrades exactly the falsifiability the format is designed to buy.

## What's solid (no action needed)

- The equality-not-containment formalization (§3: `C(T) = K \ X(T)`, with `missing`/`stale` both
  named) is the right assertion shape and is well-argued against the weaker containment alternative.
- The three rejected alternatives in §2 (one task, inventory-without-rule, annotation processor) are
  genuine alternatives with real reasons for rejection, not straw men.
- Global invariant 6 (base class's totality test method stays `final`) is a concrete, checkable
  guard against the "subclass quietly weakens the check" failure mode that gives totality tests
  their value in the first place.
- The `related`/`blocks` reasoning that *is* still accurate (edges to #336 and #358, kept because
  `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry` genuinely does not exist on master) checks
  out against the live repo state.

## Verdict rationale

The core mechanism (inventory + reusable base class + equality assertion) is well-reasoned and the
factual grounding against the live tree holds up. It drops from "sound" to "sound-with-concerns"
because of two things a team should fix before work starts: the scope silently spans four key domains
while the one integration criterion given only exercises one of them (#1), and the one criterion meant
to guard against a green-but-incomplete inventory (I3) has no defined procedure (#2) — exactly the
failure mode the issue itself is worried about. The stale `blocks:` list (#3) and the folded-in
hygiene items (#4) are lower-severity but should be cleaned up in the same pass.
