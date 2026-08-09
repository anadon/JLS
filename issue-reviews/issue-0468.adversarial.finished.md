# Issue #468: TASK-0007: exactly one net-partition walk exists as a callable pass, instead of five copies nothing outside their own files can call
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is an unusually well-instrumented issue: every quoted line, grep result, and
test-method name I independently re-checked against the working tree matched
verbatim (see "Verification" below). That empirical rigor is real and raises the
floor a lot. The concerns below are not "the issue made things up" — they are
places where the issue's own contract (§7.4, H4) is inconsistent with code it
elsewhere quotes, and one place where it is out of sync with its own declared
parent issue (#336). None of these should block filing, but an executor who
doesn't read past the Abstract will hit avoidable rework.

## Verification performed

- `git grep -n "WireEnd vend = visit.remove();" -- src test` on current HEAD
  returns exactly the three hits O1 claims (`Circuit.java:1368`, `Util.java:170`,
  `AddWire.java:180`).
- `docs/plan/` does not exist in this checkout, and the pinned commit
  `3a81a4a7d6a0f108ec201e632732d308cc02b3fc` is not a reachable git object here
  (`fatal: bad object`) — see Finding 3.
- The evidence-pin comment's line-number remapping (`HdlExporter.java` L93-97,
  L209-215, L216-217, L232-233, L259-375, L1087-1088, L1102-1103, L1103-1142)
  matches current HEAD exactly; `git diff --stat` between the cited `master`
  commit `8288226` and HEAD touches none of the six files this issue cites.
- Every test method cited (`partitionRebuildsWireNets`,
  `twoNetsBridgedByJumpsBecomeOneVerilogNet`, `twoLoadInstancesSaveByteIdentically`,
  `stateHashIsContentDetermined`, `multiDriverConflictResolvesDeterministicallyAndWarnsOnce`)
  exists at the named file/class. `src/jls/netlist/` and `test/jls/netlist/` do
  not yet exist, confirming O6.

## Findings, most severe first

### 1. (High) H4's "expected caller" AddWire.apply cannot consume the documented fold as specified — this isn't a risk, it's already falsifiable by inspection

§7.4 lists `jls.hdl.HdlExporter.buildModel`, `Circuit.finishLoad`, `Util.partition`
**and `jls.collab.op.AddWire.apply`** as expected callers of `NetPartition.of`, and
§7.10 Stage 2 formalizes the tri-state fold as a pure function of the wire end
itself: `τ(C) = ⋁ isLoadTriState(e)`.

But `AddWire.apply` does not use `WireEnd.isLoadTriState()` for its fold — it
builds fresh `WireEnd`s (`AddWire.java:122-129`, only `x`/`y`/`width`/`height`/`sid`
are set via `setValue`) and folds tri-state from a locally-built map instead:

```java
// AddWire.java:184
if (Objects.requireNonNull(specOf.get(vend)).triState()) {
    net.loadTriState();
}
```

`WireEnd.isLoadTriState()` (`WireEnd.java:647-650`) reads a field (`loadTriState`)
that is only ever set by the file-load `setValue("tristate", …)` path
(`WireEnd.java:634-636`) or the public `setTriState()` setter — neither of which
`AddWire.apply` calls on its freshly-constructed ends. So `isLoadTriState()` on
every end `AddWire` builds is always `false`, and the fold formalized in §7.10
literally cannot reproduce what `AddWire.apply` does today.

This means H4 ("`Util.partition` and `AddWire`'s inline net build are instances of
the same computation ... the difference is in the *input order* and in *which
folds run*") is wrong about `AddWire` specifically: the difference isn't which
folds run, it's that the fold's *data source* differs (external caller-supplied
map vs. the wire end's own state) — something the `EnumSet<Fold>` shape in §7.4
has no slot for.

The issue already has the right escape hatch for this in §10: "H4 refuted if
`Util.partition` or `AddWire` cannot consume the pass without a behaviour
change... do not change their behaviour, leave the other two on their current
bodies with a comment naming this issue, and file the residual." **Recommendation:**
pre-empt the rediscovery cost — strike `AddWire.apply` from §7.4's "expected
callers" list now (or mark it "expected to be refuted, see §10"), since the
falsification is verifiable today without writing a line of code.

### 2. (High) Unreconciled scope/status mismatch with the declared parent, #336

Two concrete inconsistencies with #336 (FEAT-004, `part_of_feature: 336` in this
issue's own YAML), as read on 2026-08-08:

- **Filing status.** #336's `planned_tasks` still reads `"TASK-0007 (unfiled) —
  ... ABSENT at 2d0ca9d"` and its own mermaid graph labels the node
  `T0007["TASK-0007 (unfiled)<br/>..."]`. #336 was last updated
  2026-08-08T16:37:54Z — after #468 (this issue, filed 2026-08-03) existed — yet
  nowhere in #336's body, `related`, or `planned_tasks` does it cite #468 by
  number. A scheduler or agent reading #336 alone has no way to discover that
  TASK-0007 has in fact been filed as #468.
- **Scope.** #336 §1 Capability Statement item 1 commits only to "**Both** of
  today's partition sites consume it" (its Background section names exactly two:
  the `Circuit.java:1345` load walk and the `HdlExporter.java:1161` union-find).
  #468 has independently widened that to five sites (adding `Util.partition`,
  `AddWire.apply`, and a `CircuitAssert` audit) via its own re-derivation (O1).
  That's a legitimate, well-evidenced correction — but #336's own re-planning
  protocol requires "any change to the shape of the partition value or the
  naming function reconciles §3 ... before the child closes; a deviation left
  unrecorded makes §3 stale." Nothing in #468 posts that reconciliation to #336
  now, and the DoD (§14) only requires a `STATUS:` comment on #336 at *landing*,
  not before starting — so #336 stays factually wrong (both filing status and
  scope) for the entire duration of the work.

**Recommendation:** post a comment on #336 now updating `planned_tasks` to point
at #468 and widening item 1's "both" language to match the five-site scope, or
explicitly note the discrepancy is deliberate and will be reconciled at close
(which the DoD already promises, but only #468 says so — #336 doesn't know to
expect it).

### 3. (Medium) Primary-source citations point to a git object that doesn't exist anywhere in this repository

The Background section leans on `docs/plan/tasks/TASK-0007-extract-net-partition-walk.md`
and `docs/plan/evidence/BRIEF.md` §14 (cited for the claim "There are **two**
partition passes at HEAD, not one," which O1 then says is an undercount),
both said to have "landed in `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`." That
commit is not a reachable object in this repository
(`git cat-file -t 3a81a4a... → fatal: bad object`), and `docs/plan/` does not
exist anywhere in the working tree or history I can inspect. This isn't merely
stale-line-numbers (the kind #493's pin-notice comment already fixed elsewhere in
this issue) — it's a citation to a document an executor in *this* repository
cannot open by any means available here. The issue's own O1-O6 re-derive the
load-bearing claims independently at a real, checkable commit, so the
*conclusion* doesn't depend on the phantom document — but the "prior work"
narrative that motivates the task, and the specific "two, not one" claim being
corrected, is unauditable. **Recommendation:** either drop the corpus citation or
replace it with a link to wherever that planning corpus actually lives (a
different repo/branch), so a reader can tell the difference between "stale
pointer" and "document that was never merged here."

### 4. (Medium) Gameable completion criterion: Open Question 1's recommended follow-up filing is not gated by the Definition of Done

Open Question 1 says the recommended default for `Util.partition`'s
hash-ordered input (O3) is "(a) preserve it verbatim, note it in the javadoc,
and **file the divergence as its own issue**." But the ~20-item Completion
Criteria checklist (§14) never requires that filing — it only requires the
Open Questions be "resolved (or explicitly deferred), none left blocking."
An executor can tick every DoD box, ship the extraction, and never file the
follow-up, at which point the same non-canonical-order smell that O3 already
flags as "a live defect surface, not a stylistic one" is now baked into a
shared, more-consumed `jls.netlist` primitive with no tracking issue pointing
at it — worse than the status quo, where at least the divergence is only two
files apart. **Recommendation:** add an explicit DoD line: "if Open Question 1
resolves to (a), the follow-up issue exists and is linked here before close."

### 5. (Low) Abstract oversells "a move, not a rewrite" against the issue's own hedged Hypothesis

The Abstract states flatly: "It is a **move, not a rewrite**: the goldens are
the proof that nothing changed." But H1 explicitly treats "is the walk
separable from its mutation" as an open, falsifiable question, and O2's quoted
code shows `Circuit.finishLoad`'s loop interleaves the walk and the mutation in
one pass (`visited.add(vend); net.add(vend); vend.setNet(net); if
(vend.isLoadTriState()) {...}`, immediately after the dequeue). Splitting an
interleaved single-pass BFS into a pure partition pass plus a separate
caller-side mutation pass (§7.10 Stage 4) is a real restructuring of the
algorithm's control flow, even where the net effect on state is provably
identical. The body of the issue is honest about this risk (H1's falsification
clause); the Abstract's one-line framing is not. Cosmetic, but it's the kind of
line a reviewer skims and an implementer takes as license to skip the
red-tests-first discipline §8 otherwise insists on.

### 6. (Low) Feasibility/blast-radius note, not a defect

The task modifies `Circuit.finishLoad`'s mutation path directly — the routine
every existing `.jls` file loads through, and the one multi-driver resolution
order depends on. That's a large blast radius for a "task"-tier ticket in a
single-maintainer project, even though the extensive P3-P7/H1-H4 falsification
scaffolding is a genuinely proportionate mitigation. Worth the maintainer's
explicit go-ahead on timing (e.g., not landed the same week as unrelated
load-path changes), not a reason to rework the issue.

## What's solid (no action needed)

- O1's "five copies" count is independently reproducible exactly as stated, and
  the BFS-body grep is a good invariant to pin as an ArchUnit rule (P2/P6).
- The determinism argument (O3, quoting `Circuit.java:76-79`'s
  `LinkedHashSet loadedElements` comment against `Util.partition`'s
  `circ.getElements()`) is real and well-evidenced; H2's ordering clause
  correctly identifies the risk and DeterministicSaveTest is the right pin.
  Open Question 1 correctly declines to silently "fix" it in-flight rather than
  unify the two copies' behavior.
- Explicit non-goals (§13: no complexity fix, no naming change, no semantics
  change) are consistent with #376 and #373's own scope statements — I checked
  both issues and found no contradiction there.
- The `IllegalStateException("attached put has no wire end")` error-preservation
  requirement (§7.11) is pinned to a real, quoted line in `HdlExporter.java`.
- Failure-mode table (§7.11) correctly matches today's `while (!ends.isEmpty())`
  fall-through behavior for the empty-input case.
