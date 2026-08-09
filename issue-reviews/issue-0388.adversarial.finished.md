# Issue #388: TASK-0050 (RESIDUAL): the readability rubric becomes a gate over real imports, hierarchy instances get placed, and an import can actually be started and undone
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is open, well-evidenced against the code (its Observations O1–O7 check out against the current `src/jls/hdl/layout/` and `src/jls/hdl/imp/NetlistImporter.java` on `master`), and the underlying complaint — the rubric is code but never asserted against a real import, and no CLI/GUI import entry point exists — is real and reproducible (verified independently below). But the issue's own dependency graph is stale and self-contradictory in a way that could let an executor start work that its own text says must not start yet, its scope is large enough that "task" is the wrong tier, and two of its "Blocks execution" open questions are still genuinely unresolved.

## Findings, most severe first

### 1. `blocked_by: [320]` names a closed issue, and neither of the two real blockers is wired into the machine-readable dependency block

The Status & Dependencies YAML reads:

```yaml
blocked_by:
  - 320                # FEAT-020 — the layout problem is defined by what the mapper realizes.
```

`#320` is **closed, `state_reason: duplicate`**, absorbed into `#61` on 2026-08-04. The issue's own comment thread (comment #5227454359, posted the same day as the issue's `updated_at`) acknowledges the parent-pointer rot for `part_of_feature` but does **not** touch `blocked_by`; it only narrates, in prose, that the real gating relationship is "now expressible as an edge between two open parents (#61 -> #62)" — i.e. `#448` (TASK-0047, the first producer of `feedback = true` edges on `LayoutGraph.connect`, re-homed to `#61`). Separately, the body's own §1 and §6 name a *second*, undeclared blocker: TASK-0048 ("hierarchy realization"), which turned out to be filed as **`#449`** (verified open) — and the body's mermaid diagram explicitly omits that edge with a note "a link pass adds it," which never happened in this issue's own machine block.

Verified against `master` (`5311625`): `NetlistImporter.mapCell`'s switch still has exactly five arms (`$not`, `$and`, `$or`, `$xor`, `$mux`) — identical to the evidence commit — so both real prerequisites (`#448`/TASK-0047 sequential realization, `#449`/TASK-0048 hierarchy realization) are still unlanded. The *substance* of "this is blocked" is currently true. The *pointer* is wrong. An executor or automation that resolves `blocked_by` by issue state (closed ⇒ satisfied) will conclude #388 is unblocked and start it — which the issue's own O6/O7 sections say produces the worst outcome: P7 (the first assertion over a real feedback back-edge) and the hierarchy-placement half get implemented against fixtures that still cannot express what they're supposed to test, because #448/#449 haven't landed.

**Recommendation:** before work starts, edit `blocked_by` to `[448, 449]` (or file the link-pass edges as the body promises) and drop `320`. Do not rely on the prose corrections in comments to override a stale machine block.

### 2. `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is not an ancestor of `master`

Verified directly: `git merge-base --is-ancestor 2d0ca9d HEAD` fails. This is a known tracker-wide problem recorded in `#493` ("The evidence_commit every filed issue declares… is on a branch that will be deleted"). The good news, also verified: none of #388's cited files (`LayoutMetrics.java`, `NetlistImporter.java`, `ImportSummary.java`, `HeuristicLayeredLayouter.java`) are among `#493`'s seven "sharp edge" files whose content diverges from `master`, and #388 does not appear in `#493`'s list of 43 affected issues. Spot-checked: `LayoutMetrics.java:26-35` (the five constants) and `:414`/`:450` (`rubricFailures`), and `NetlistImporter.java:104` (the unconditional layout call) are byte-identical to what the issue quotes. So the *content* of O1/O2/O5/O7 holds today — but the commit label itself is a 404 waiting to happen once the branch is deleted, and the Definition of Done explicitly requires "every cited evidence document and permalink resolves on the default branch at close." This is a live, self-inflicted failure condition for the issue's own DoD checklist, not a hypothetical.

**Recommendation:** re-pin `evidence_commit` to `828822672fc3a8e2cb6da25192472079f04c29dd` (or later) per #493's guidance before this issue is picked up, since the DoD item will otherwise fail trivially.

### 3. Open Question 2 ("what is core scale, concretely, and which core?") is unresolved and is stated by the issue itself as blocking execution of P9/IC-3

```
2. What is "core scale", concretely, and which core? … Blocks execution of P9.
```

No core has been named anywhere in this issue, in `#62`, or in `#290` at the time of this review. P9 ("the five rubric numbers at core scale, recorded") and the corresponding Definition-of-Done bullet ("core-scale rubric numbers are recorded… with the fixture's element and net counts") are consequently unactionable as written — an executor has no way to know when this item is "done" versus merely postponed. This is the same defect the parent `#62` calls "the feature's honest unknown" (H2) but here it's worse: the issue puts a checklist item in its own Definition of Done that depends on a decision the issue explicitly says isn't made yet, with no owner or deadline named for making it.

**Recommendation:** name the core (or explicitly descope core-scale measurement from this task into a follow-up) before this issue is considered ready to pick up — not "rides along."

### 4. Scope is feature-sized, filed as a single `tier: task`

The Method section (§8) has 12 top-level checklist items spanning: a new parameterized rubric-corpus test, three hand-drawn showcase golden `.jls` files plus one large synthetic fixture (all requiring `#320`'s — now `#61`'s — sequential-cell work to even be importable), a hierarchy-placement integration with an entirely separate unfiled task (`#449`), a derived (non-hand-listed) vocabulary-totality test, a new feedback-edge invariant test, a new CLI flag plus flag-collision resolution, a new GUI menu entry through `SimpleEditor`'s already-flagged 305-line dispatcher (`#84`), single-undo grouping, a core-scale measurement campaign whose scale target is undecided (Finding 3), and a human trace trial. The Definition of Done has 24 checkboxes. This is comparable in shape to `#62` itself (a `tier: feature`) or to `#320`/its successor (also `tier: feature`), not to a one-week task like `#449`. Filing it as a task risks a PR that either quietly narrows scope without a recorded REPLAN, or balloons past what one reviewable change should contain.

**Recommendation:** either split along the "three independent halves" the issue itself identifies in §"Unfiled sibling" (vocabulary totality / core-scale measurement / editor ergonomics are said to be independent and concurrently workable) into separate filed tasks, or re-tier this as a feature with its own child tasks, mirroring how `#62` and `#320` are structured.

### 5. `#290` ownership question is answered off-issue, contradicting this issue's own resolution protocol

§12 says: "**Ownership must be resolved before either is funded**… decided in one recorded comment on #290. **Blocks execution absolutely.**" The actual resolution — "`#62 = #290 + #388`" (not a duplicate; disjoint slices) — was posted as a comment on **`#62`** on 2026-08-04, never mirrored onto `#290` as this issue's own text demands. Substantively the ambiguity is resolved (verified by reading `#62`'s comment thread), so this is not a live blocker in practice, but it means Open Question 1's literal instruction ("decided… on #290") was not followed, and anyone auditing `#290` alone (as the issue tells a picker-up to do) will not find the resolution there.

**Recommendation:** mirror the resolution onto `#290`, or update Open Question 1 to point at the `#62` comment instead of demanding one on `#290`.

### 6. P2's acceptance criterion ("run `jls -import`; observe an import") is underspecified enough to be gameable

P2 only requires that *some* flag starting import succeeds; Open Question 4 leaves the actual flag name undecided ("What is the import flag actually called?"), and the recommended default is "rely on longest-match... pin it with a CliFlagTableTest case." `test/jls/CliFlagTableTest.java` already exists (5 tests) and would need a new case, which is fine — but nothing in P2/§8/§14 pins the *chosen name* itself, so a reviewer cannot tell from the acceptance criteria alone whether `-import`, `-importJson`, or something else was chosen, or whether it collides with a future flag the way `-import` collides with `-i` today. Given the issue's own warning ("A wrong flag name is a permanent CLI commitment... whatever is chosen ships forever"), the criterion should require the specific chosen name to be recorded and reviewed, not merely that *an* import flag works.

**Recommendation:** add an explicit "flag name proposal reviewed and recorded before implementation" step, not just longest-match compatibility.

## What's solid

- O1–O5, O7 (the rubric constants, the single `overlapCount`-only assertion in `HeuristicLayeredLayouterTest.java:108`, the four-fixture all-combinational corpus, the `-import`→`-i mport` mis-parse, and the unconditional `layout()` call in `NetlistImporter.java:104`) are all independently reproducible against the current tree — this is a well-grounded, non-speculative bug report, not a wishlist.
- The explicit non-goals ("Do not rewrite the layouter," "Nothing in this task links ELK," "no `.jls` format change") are concrete and testable, and correctly inherited from `#62`'s licensing/engine-neutrality invariants.
- H1–H4 are genuinely falsifiable and the falsification responses (tune vs. record-a-decision vs. re-home) are well-differentiated — this avoids the common trap of a hypothesis with no exit condition.
- P10 ("`git diff --stat` on `HeuristicLayeredLayouter.java` traces only to named measured failures") is a good anti-scope-creep control that's hard to game by accident.
