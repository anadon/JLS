# Issue #467: TASK-0110: concurrent edits converge to identical bytes, undo is per-user and never silently wrong, and no peer can name a type the allowlist excludes
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The two headline factual claims in this issue check out against the live tree: `ElementVocabulary.ALLOWED` (`src/jls/collab/op/ElementVocabulary.java:39-46`) really does hold 34 tokens, `ElementRegistry.ALL` (`src/jls/elem/ElementRegistry.java:38-73`) really does hold 35, and the difference really is exactly `TestGen`. The stale-inverse hazard (O3) is also accurately quoted from `src/jls/collab/op/package-info.java:13`. But the issue has two disqualifying planning defects that need resolving before anyone starts: it silently overlaps already-filed, already-open sibling work (#279, #280), and its stated `blocked_by` chain bottoms out in prerequisites that are themselves blocked by issues that do not exist yet. Both are the kind of thing that either duplicates effort or stalls the task indefinitely, and neither is flagged in the issue body as a risk — only a maintenance comment half-notices the first one, and nothing notices the second.

## Findings, most severe first

### 1. [CRITICAL] Undisclosed scope duplication with #279 and #280, #171's own filed decomposition of this exact work

Issue #171 (which #467 claims to close) states its own decomposition explicitly: *"Decomposition: causal substrate (landed) → merge rules + P1 (#279) → anti-entropy + P2 (#280) → compaction, undo, gossip/token retirement, RGA, and the P4 interactive legs (planned slices)."* #279 is titled "per-kind confluent CRDT merge rules + P1 in-process convergence suite" and its Method section proposes building exactly the "merge metadata + rules per the research doc §3 table: add-wins/delete-wins element bookkeeping keyed by `ElementId`; per-attribute LWW register... OR-set wires" plus an "in-process N-replica harness with delivery-order permutation." #280 is titled "anti-entropy resync + partition/heal convergence (P2)" and proposes clock-exchange, missing-op transfer, and a bounded op log.

That is line-for-line the same deliverable #467 proposes to build under different names (`ElementSet`, `AttributeRegister`, `WireSet`, anti-entropy, compaction) and under different blocking issues (#415/TASK-0032, #435/TASK-0109, rather than #279/#280). #467's `related` list is `[171, 170, 163, 167, 78, 352, 348, 433, 436, 356]` — **#279 and #280 are not cited anywhere in the body**, only #171 itself. The only place this collision is acknowledged is a maintenance comment (`issuecomment-5181206771`), which flags it and then punts: *"this task now straddles #170 and #171... and #171's slices (#279, #280) overlap that ground. A later pass may prefer to split this task along that line rather than leave it double-homed."* That comment is from four days before this review and the split never happened.

**Failure mode if executed as written:** two independently-implemented `jls.collab.crdt` merge-rule cores land under different issue numbers, either as a duplicate PR that has to be thrown away, or — worse — as two designs that both pass their own tests but diverge on the harder edge cases (LWW tiebreak details, OR-set semantics), because neither #467 nor #279 references the other's design decisions.

**Recommendation:** before any code is written, either (a) close #467 as a duplicate/superset of #279+#280 and fold TASK-0037/TASK-0032/TASK-0109's additional scope (headless op application, deny-list hardening, per-user undo) into #171's roster as new slices alongside #279/#280, or (b) explicitly REPLAN #171 to retire #279/#280 in favor of #467/#415/#435/#382 and record why. Leaving both live is the actual defect, not a preference between them.

### 2. [CRITICAL] The stated `blocked_by` chain is not close to landable — it bottoms out in unfiled issues

#467 is `blocked_by: [415, 435]`, and its own Method step 1 says: *"Confirm #415 (the merge-rule table) and #435 (the two-replica harness) have landed. Do not write rules before the table."* Neither has landed (both fetched as `state: open`). Worse, both are themselves blocked by prerequisites that don't exist as filed issues yet:

- #415 (TASK-0032, the merge-rule table) lists `blocked_by: []` in its machine block but its own body says: *"blocked_by: [] # TASK-0005 (reference elements by stable id, FEAT-003) and TASK-0031 (semantic validation of a merged file, FEAT-012) are both genuine prerequisites and neither is filed yet."* Its Method step 2 says explicitly: *"Confirm TASK-0005 (stable-id references) and TASK-0031 (`Circuit.validate()`) have landed."*
- #435 (TASK-0109, the two-replica harness) lists `blocked_by: [382]` and #382 (TASK-0037, headless op application) is open and unlanded — verified directly against the current tree: `src/jls/collab/op/CircuitOp.java:51` still reads `void apply(Circuit circuit, Graphics g) throws OpRejected;`, exactly the pre-fix state #382 describes. #435's body also names a second prerequisite, TASK-0108 (session foundation), and says outright: *"is not yet filed... without it there is no envelope framing, no peer identity to stamp a `VectorClock` with."*

So the dependency graph under #467, at minimum, is: #467 → {#415 → {TASK-0005 (unfiled), TASK-0031 (unfiled)}, #435 → {#382 (open, unlanded), TASK-0108 (unfiled)}}. Four of the five nodes beneath #467 are either unlanded or don't exist as issues. This is not "blocked, wait a bit" — it's a task sitting three-to-four issues deep behind work that hasn't been scoped yet.

**Recommendation:** the Status & Dependencies block should say so plainly (e.g. "transitively blocked by 3 unfiled issues; do not pick up before TASK-0005, TASK-0031 and TASK-0108 are filed and landed"), rather than presenting `blocked_by: [415, 435]` as though landing those two is sufficient.

### 3. [HIGH] Stale `part_of_feature` pointing at a closed-as-duplicate issue, silently left uncorrected in the body

The body's machine block declares `part_of_feature: 352`. #352 (FEAT-052) is `state: closed`, `state_reason: duplicate`, closed 2026-08-04 — before this review and before the second correction comment. Two separate maintenance comments on #467 itself (2026-08-04 and 2026-08-08) both say the field must be re-read as `170`, with the second stating: *"Read this in place of the body's... Corrected field: `part_of_feature: 170`."* Per that same comment's own rule, *"No body or title of any issue was edited by this pass"* — so the body still says 352 and will keep saying so for any reader who does not scroll to the bottom comment. A contributor picking this up from the issue body alone, or via any tooling that reads only the YAML block, will misattribute landing reports to a closed duplicate feature rather than #170.

**Recommendation:** edit the body's machine block directly rather than relying on a comment thread to override it — the whole point of the machine-readable block is that it's the thing tooling reads.

### 4. [MEDIUM] "This closes #171 and #170" overreaches what #467's own scope covers

#171's Definition of Done requires, beyond convergence/undo/hardening: `requires_tasks: [279, 280]` landed (see Finding 1), plus unfiled planned slices — *"OpSink gossip integration + Stage 1 token retirement,"* and *"P4 interactive suite + Stage 2 pilot legs"* (a human-run two/three-machine pilot session, #163 item P) — none of which #467's Method addresses. #467's own §13 Conclusion lists "session membership and the join lifecycle (#433, #348)... the offline three-way merger (#356)... undoing another peer's op" as out of scope, which is consistent, but it does not similarly disclaim gossip integration or the pilot legs, and still asserts closure of #171 in the Abstract and §13. Closing #171 on #467's landing would leave #171's own Definition of Done partially unmet.

**Recommendation:** either narrow the closes-#171 claim to "advances #171, does not close it," or explicitly fold gossip/token-retirement and the pilot-leg requirement into #467's own scope and Definition of Done.

### 5. [MEDIUM] P6 (`everyTableRowHasATest()`) is only as strong as a table that doesn't exist yet, and can't be checked now

P6/H1's anti-vacuity guard cross-checks reflectively against "the committed table" — i.e., #415's merge-rule table, which per Finding 2 hasn't landed and is blocked by two unfiled issues. If #415 lands a narrower table than what #467 assumes (four convergent types cover every row — H1's falsifiable claim), #467's P6 will trivially pass against whatever table exists at that time, without anyone re-validating H1's coverage claim against the originally-envisioned scope. The falsification path for H1 ("a row whose concurrent pair does not converge... would mean a fifth type is needed and the table's coverage was wrong") is sound in principle but its soundness is entirely deferred to an artifact this issue cannot inspect today.

**Recommendation:** no action needed before #415 lands, but flag this issue as needing a fresh read of #415's actual table before implementation starts, not just a landed/not-landed check.

### 6. [LOW-MEDIUM] "Report, not silently skip" (P10) is underspecified as a UI-visible contract

O3/Stage 4/P10 correctly diagnose that the recorded inverse (`CircuitOp.invert()` against the pre-apply circuit, `src/jls/collab/op/package-info.java:13`) goes stale under concurrency, and correctly insist that a failed undo must be *reported* rather than silently skipped, with the strong framing "a silently skipped undo is the worst outcome because the user believes it happened." But §7.6's data contract only says *"Undo failure reports — a real output, asserted by P10"* without specifying the delivery mechanism (return value the editor must render, a dialog, a status-bar message, a log line). Since this is a GUI editor (`SimpleEditor`) and the whole point is that the *user* must not be misled, an implementer could satisfy P10's literal test (assert some report object exists) while shipping a report that never reaches the user's screen — e.g. a value silently swallowed by a caller that isn't yet wired to a UI surface, which is plausible here since Stage 4 explicitly says the undo stack and its UI wiring aren't specified beyond "in memory only for this task."

**Recommendation:** the Interface & Data Contract should name the UI surface (or explicitly defer it and say so), not just the data shape of the report.

### 7. [LOW] Compaction frontier (H5/P8) depends on "the roster," whose ownership sits in unfiled/adjacent issues

Stage 3's correctness argument — taking the acknowledged frontier as the pointwise minimum over the *roster's* clocks rather than the *transport's* connected set — is sound and the failure mode (irreversible history loss) is well-argued. But the roster/reachability tracking this depends on (`src/jls/collab/session/Roster.java`, `ReachabilityTracker.java`) is owned by #168/#169/#433, and #433 (TASK-0108) is explicitly unfiled per #435's own body. #467 doesn't call this out as a feasibility risk the way it does for #415/#435 in `blocked_by`, even though compaction (P8, "the one irreversible operation") is arguably the highest-stakes prediction in the whole issue.

**Recommendation:** add #433/TASK-0108 (or its landed equivalent) as an explicit dependency for the compaction slice specifically, not just an implicit assumption about roster shape.

## What's solid

- **O2 (registry-vs-allowlist gap = exactly `TestGen`, 34 vs 35) is accurately measured** against the live tree and is a genuine, well-scoped security finding — the "registry minus a tested, non-empty deny list" remedy is the right shape for the hazard described.
- **O3 (stale-inverse hazard) is accurately quoted and the commuting-square argument in Stage 4 is correct**: a recorded inverse computed against the pre-apply state is provably wrong once a concurrent remote op touches the same element.
- **The three-outcome convergence framing (converged-same / converged-different / unconverged) that #467 inherits from #435 is a good design** — collapsing "converged, different bytes" into "timeout" would indeed destroy the ability to diagnose merge-rule bugs, and P6's test is a reasonable guard against that regression.
- **The dependency-downward-only architecture constraint (O5/P14) is real and already enforced** by `ArchitectureRulesTest.replicationStackDependsDownwardOnly` (`test/jls/ArchitectureRulesTest.java:226-238`), verified to exist with matching content.
- **The four security ratchets cited (O4/P13)** exist at the cited line numbers in `test/jls/CollabSecurityRatchetTest.java` (48, 68, 87, 110) and are a reasonable invariant to require staying green.
