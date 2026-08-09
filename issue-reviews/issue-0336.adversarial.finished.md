# Issue #336: FEAT-004: exactly one net partition in JLS, and a synthesized net name that survives an unrelated edit
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what's being attacked

FEAT-004 is a two-task feature: (TASK-0007/#468) extract the net-connectivity
walk into a new `jls.netlist` package, and (TASK-0008/#373) make synthesized
net/register/probe names a function of `Element.getStableId()` instead of the
save-order `getID()`. The underlying complaint is real and independently
verifiable: `Circuit.java:1344-1368`, `Util.java:170`, and
`AddWire.java:180` all contain a near-identical `WireEnd vend = visit.remove();`
BFS body; `HdlExporter.java:346-353` synthesizes `net_<getID()>` off the
file-local reassigned-on-every-save index
(`Element.java:21`: `/** The file-local reference index, reassigned on every
save. */`); and `Wire.attachProbe` (`Wire.java:462-466`) assigns
`probeName = name` with no call to `Util.isValidName` (`Util.java:219`). None
of that is invented — it checks out against current HEAD (5b05d67), not just
against the issue's cited (and now-deleted) evidence commit.

## Findings, most severe first

**1. IC-1's "exactly one implementation" promise is already contradicted by its own child task's escape hatch.**
§5 IC-1 states the feature's proof obligation as an architecture test that
"fails if a second connected-component walk over wire ends appears outside
the package." But TASK-0007 (#468), which #336 spawned to do exactly this
extraction, explicitly reserves the right to leave two of the five copies
(`Util.partition`, `AddWire.apply`) unconverted: "If unifying [Util.partition
and AddWire] would change behaviour, do not unify — file it" (§10, H4
falsification). If that fallback triggers, IC-1's ArchUnit rule and #336's
own "exactly one implementation" claim in §1 item 1 become unsatisfiable by
construction, and nothing in #336 says what happens then. *Recommendation:*
#336 should either name the H4-refuted case as an acceptable IC-1 deviation
in advance, or state plainly that TASK-0007 is not permitted to close via
H4's fallback without a `REPLAN:` on #336 itself.

**2. The duplication is undercounted by the feature's own scope statement, verified against source.**
§1 and the Background section describe "two partition passes" (the
`Circuit.finishLoad` walk and `HdlExporter`'s union-find). Grepping current
HEAD confirms the walk body appears **three** times verbatim
(`Circuit.java:1368`, `Util.java:170`, `AddWire.java:180`), plus the
exporter's union-find, plus a fourth independent implementation in
`test/jls/ui/CircuitAssert.java` (`reaches`, line 125) that the exporter's
own javadoc names as doing "the same aliasing." That's five, not two — a
fact #336's own spawned child #468 discovered and documents in detail (O1).
§1's Capability Statement ("Both of today's partition sites consume it")
therefore describes a narrower fix than the problem the issue itself later
proves exists. If TASK-0007 only converts the two sites #336 names, three
duplicate implementations survive the feature's close, undermining the
"exactly one answer" framing in the Abstract. *Recommendation:* reconcile
§1's scope language with #468's O1 finding before work starts, not as a
`REPLAN:` afterward.

**3. The issue's own stated filing gate was violated, by its own admission.**
§ Completion Criteria requires "Open Questions 1 and 2 are answered on this
issue before TASK-0008 is filed" (digest function, stability epoch). Neither
question has an answering comment on #336 — the three comments present are
an evidence-repin notice and two deduplication boundary notes, none of which
resolve OQ1/OQ2. Yet TASK-0008 exists and is open as #373, filed 2026-08-03,
one day after #336 itself. #373 acknowledges the violation directly: "Filing
proceeded anyway: the deliverable this task is measured on is the freeze and
its documentation... Withholding the issue until the decision exists would
be the circularity D10 forbids." That is a post-hoc justification for
skipping a gate #336 itself wrote as a hard precondition, not a resolution
of it. A reviewer relying on #336's Completion Criteria as the source of
truth for "is this ready to execute" would be misled. *Recommendation:*
either strike the OQ1/OQ2-before-filing gate from #336's Completion Criteria
(if D10 genuinely supersedes it) or answer OQ1/OQ2 on #336 now and record it.

**4. IC-6, the test that the package boundary is "not decorative," is explicitly not automated — and Open Question 4 ignores that TASK-0007's own design already answers it.**
IC-6 ("delete `HdlExporter` from the build and compile `jls.netlist` plus
its other consumer") is "recorded as a manual procedure in the close-out
comment rather than a permanent job" — i.e., there is no standing CI check
that a future change can't silently reintroduce a `jls.hdl` dependency into
`jls.netlist`. Separately, Open Question 4 treats "which second consumer
discharges IC-6" as an open decision between retargeting `PcfEmitter` or
writing a new command — but TASK-0007's own expected-callers list already
includes `Circuit.finishLoad`, `Util.partition`, and `AddWire.apply`, all of
which are non-emitter, in-tree consumers outside `jls.hdl` by construction.
Either the load path doesn't "count" for IC-6 for a reason nowhere stated,
or Open Question 4 is asking a question the feature's own first task already
answers. This is a gameable acceptance criterion either way: a manual,
one-time compile check plus an ambiguous notion of what counts as "the
second consumer" is weak evidence for a structural claim ("the package
boundary is not decorative") that #336 sells as a durable property.
*Recommendation:* make IC-6 (or an equivalent) a permanent ArchUnit rule —
`jls.netlist` importing nothing from `jls.hdl` is a one-line test — and
clarify in Open Question 4 whether the load-path consumers satisfy it.

**5. Cost estimate looks materially understated against the child tasks' own revealed scope.**
§ Cost bands this at "2-3 maintainer-weeks," reconciled as TASK-0007 (1.5 wk)
+ TASK-0008 (1.5 wk) = 3.0 mw. But #468 alone requires: converting five call
sites (not two), explicitly preserving three different, previously-drifted
fold behaviors (O3: `Util.partition` already disagrees with
`Circuit.finishLoad` on iteration order and tri-state folding — "the two
copies are not interchangeable"), auditing whether `CircuitAssert` folds in,
adding a new ArchUnit rule, and registering the new package with two
different ratchet tests. #373 alone requires reviewing 54 regenerated golden
files by hand ("the largest single cost of the task," per its own §9),
designing and freezing a digest function with a privacy tradeoff (raw stable
id leaks a per-install replica string), validating probe names at five call
sites with four distinct failure behaviors, and extending two normative docs.
Both tasks individually look like 1.5-2.5 week efforts on their own stated
scope, which puts the real total closer to 3-5 mw than 2-3. The Cost
section's "reconciliation" step (checking that the row sum doesn't exceed
the band) validates internal arithmetic, not whether the estimate reflects
the work the tasks themselves describe. *Recommendation:* re-band after
reading #468 and #373 in full, not just their own headline week figures.

**6. Feasibility: the feature is fully gated on an unstarted, unfiled prerequisite.**
`blocked_by: [315]`, and #315 (FEAT-001, verified open) itself has two
"not filed" planned tasks (TASK-0001, TASK-0002) with no child issues yet.
So #336 cannot start until a feature that hasn't started itself lands. This
is disclosed honestly in #336's machine block, but it means "sound-with-concerns"
rather than "ready to execute" — a reader who only sees the two-cut
decomposition, the mermaid diagram, and the cost estimate could reasonably
miss that the real critical path includes an entire unstarted upstream
feature. Not a defect in the issue's honesty, but a real scheduling risk
worth surfacing explicitly rather than leaving it to be inferred from a YAML
array.

**7. Process overhead is large relative to the underlying code change, for a project explicitly scoped as single-maintainer.**
ARCHITECTURE.md states plainly: "JLS is a single-maintainer pedagogy tool."
This issue and its two task children run to roughly 15,000 words combined,
carry LaTeX transformation semantics, DAG-acyclicity proofs, and three
follow-up adjudication comments cross-referencing #523, #468, #373, #315 to
resolve boundary/staleness questions the framework itself introduced. The
actual code change — collapse ~3-5 duplicate BFS/union-find implementations
into one package, and swap a name-synthesis key from `getID()` to
`getStableId()` — is a legitimate but comparatively contained refactor. For
a solo-maintainer project, the planning apparatus's own maintenance cost
(keeping the machine block, roster table, and mermaid graph "in agreement
with reality," per the Completion Criteria) is itself already generating
defects, as finding #3's comment thread on #336 shows ("roster is stale
against the tracker"). This is a standing risk, not a one-time issue: the
more issues this framework spawns, the more of these self-correction
comments will be needed to keep the graph honest.

## What's solid (no rework needed)

- The core technical diagnosis is real and directly verifiable in the
  checked-out repo: name instability under unrelated edits, unvalidated
  probe names reaching the VCD `$var` writer, and duplicated connectivity
  logic all exist exactly as described.
- The decision to split "move" (TASK-0007) from "rename" (TASK-0008) so the
  extraction's correctness proof (byte-identical goldens) isn't muddied by a
  simultaneous rename is well-reasoned and appropriately defended in §2.
- Global Invariants (§4) are the right list for this kind of change: no
  format bump, byte-identical saves, byte-identical goldens except in name
  positions, headless leaf package.
- User-supplied names never being rewritten (§4 item 6) is a correct and
  necessary guardrail against over-scoping the naming freeze.
- The privacy concern flagged in Open Question 1 (raw stable id leaking a
  per-install replica string into student-readable artifacts) is a genuine,
  well-spotted risk, correctly deferred to a digest rather than shipped raw.

## Verdict rationale

`sound-with-concerns`: the underlying technical problem is real, well
evidenced against the actual codebase, and the decomposition is reasonable.
But the issue's own numbers don't fully hold together — IC-1's "exactly one
implementation" commitment conflicts with its child task's own stated
escape hatch, the duplication count in scope is smaller than what the
feature's own audit found, a completion-criteria gate was already bypassed
by the time TASK-0008 was filed, and the cost estimate looks optimistic set
against the tasks' own described scope. None of these individually block
the work from proceeding — #315 blocks that regardless — but they should be
reconciled before TASK-0007/#468 and TASK-0008/#373 are picked up, or a
`REPLAN:` risks being needed almost immediately after execution starts.
