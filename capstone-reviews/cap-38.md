**Verdict: ready-with-gaps** — the two load-bearing feature slices (#597, #598) are startable today with verified code anchors and a real, acyclic dependency graph; the gaps are concentrated in the hardware-free-CI pillar (AC-3 has no filed owner anywhere), the unresolved #599 framing conflict, and one AC (AC-5) whose named evidentiary artifact does not exist in the repository.

Tree walked: #522 → #597 (#632, #634, #636), #598 (#638, #640, #643), #599 (#645, #647); ordering/predecessor referents #75 (#288), #264 (#416, #386), #358 (#292, #336), #873 (#291), #492, #359, #586, #302, #510. All 25 issues fetched; every load-bearing code claim checked against master (`c5cee1b`, 2026-08-05).

## 1. Decomposition

**Sound where it is filed.** All three FEATs and all eight TASKs are open, natively linked, and non-overlapping in their stated seams (#597 owns the GUI surface and the `board`/`boardpin` attribute model; #598 owns the headless diagnostic engine and CLI rendering; #599 owns the Basys-3 verdict). Cross-feature task edges (#636→#598, #647→#597/#645) serialize correctly and mirror the feature-level boundary notes.

**Gap — the capstone's fourth pillar is unfiled.** The planned-features table's "Hardware-free CI truth" row says "not separately filed," and the situation underneath is worse than the row implies:

- #264's "Required CI lane" (real yosys/nextpnr-ice40/icepack via nix devShell, checksummed `.bin`) is still a `planned (unfiled)` row in #264's own decomposition table. AC-1 and AC-3 both gate on it.
- #359's committed-behavioral-corpus task — the two-layer shape the capstone's differential-oracle lane explicitly reuses — is marked "Not filed" in #359's own `planned_tasks` ("the registry's task id space is closed at TASK-0112").
- The two slivers the capstone itself names (per-push lane trigger scoped to this flow; once-per-release manual programming checklist) remain unowned.

Net effect: **no filed issue owns AC-3's differential-oracle CI lane** (circuit → VerilogEmitter → yosys → `NetlistImporter` → `BatchSimulator` round trip). The ingredients verified as present on master (`src/jls/hdl/imp/NetlistImporter.java`, `src/jls/hdl/yosys/`, `BatchSimulator`), so this is filing work, not research — but every filed child could close and AC-3 would still have nobody to do it.

**Gap — double-owned/differently-named shared validator.** #634 (TASK-C597-2) expects a shared `PinPlan.check` and attributes "Board pin capability + a shared PinPlan.check" to **#264** via its `ordering_after` comment; #598/#638 own the identical capability as `BoardPreflight.check` with #638 supplying the direction-data migration. Same engine, two names, two claimed owners; #634's ordering does not list #598 at all. Reconcile before #634 starts (cheap: one REPLAN naming #598's `BoardPreflight`/`Diagnostic` shape as the authority, which #598's redirect note already claims).

**Predecessors are real and open** — #75, #264, #358 (chain: #336 → #358, requires #292), #873 (requires #291, blocked_by #292). The CAP's referent hygiene is good (it correctly names #75 not #288, and #598 correctly repointed its rejection-reason source from closed-`not_planned` #59 to open #492). One mirror gap: **neither #358 (`serves_capstones: [298, 302, 304, 310]`) nor #873 (`[302, 304]`) records #522**, so if those capstones descope, CAP-38's hard predecessors lose their only recorded demand edges — worth a one-line REPLAN on each.

## 2. Acceptance-criteria composition

- **AC-1** (SubCircuit/Memory demo circuit) composes only through predecessors #358/#291 — correct as an ordering claim, but note #597's own AC-4 pins bind-time *refusal* of SubCircuit/Memory as intended behavior. If #597's refusal test hardcodes those two classes rather than reading the live disposition set (#492's table), it will contradict AC-1 the day #358/#291 land. No child owns the post-predecessor "GUI flow works on a hierarchical/Memory design" verification; only the capstone AC catches it.
- **AC-2**'s second sentence — "at least one diagnostic asserted against the real installed toolchain, not only the hermetic stub-PATH pattern" — is owned by **no child**. #598's AC-1–AC-9 are all satisfiable with stubs (its AC-7 is about where detection code lives, not what tests run against). This leg currently falls through to #264's unfiled CI-lane task. All three #598 children pass ⇒ AC-2 still fails.
- **AC-3** — unowned entirely (see §1).
- **AC-4** vs **#599** — direct, mutually acknowledged contradiction. The capstone says "no separate Basys-3 decision document is required"; #599's decomposition (#645 decide → #647 land) makes a written verdict document mandatory, its Completion Criteria require resolving Open Question 3 "before this feature closes," and its own recommendation is REPLAN **before #645 starts substantive work**. Also #647's `ordering_after: [264, 416, 597, 645]` bakes in serialization behind the ECP5 board and the GUI picker that the capstone's ~1 mw table-row framing would mostly dissolve. This is a genuine start-blocking open question *for the #599 slice only*.
- **AC-5** — see staleness below; its named evidentiary owner does not exist.

#598's internal AC composition is the strongest in the tree (cross-class aggregation, never-silent-skip, structure-not-wording assertions, honest per-class current-state table — the no-direction-data claim about `Board` verified true on master: `record Board(String name, String fpga, Format format, Map<String,String> pins)`, and `ToolLocator` confirmed test-scope-only).

## 3. Dependency chains

**Acyclic — verified by walking every machine block.** Longest chain: #336 → #358(#292) → #291(#873) and #264/#288 → #632 → #634 → #636; #638 → #640 → #643; #645 → #647. Cross-feature edges (#636 after #598; #647 after #597) create a 598 → 597-close → 599-close partial order with no back-edges. #599's own cycle-walk section is accurate.

**No edge points at a closed issue.** All 25 walked issues are open except #59 (closed `not_planned` 2026-08-03), which appears only in `related` arrays and in #599's prose ("Consumes: #59's exported Verilog/VHDL" / "#59's exported Verilog/VHDL — untouched") — stale referent, cosmetic since the exporter itself is live on master, but #599 should cite the code or #492's disposition layer, not a not-planned issue, at its next touch.

**External prerequisites:** the open toolchain in CI is unfiled work, not an unfunded external dependency; physical board programming is explicitly waived (#264's WAIVED row) and excluded from the ACs. Nothing here justifies `blocked`.

## 4. Staleness and gaps

1. **AC-5's evidentiary artifact does not exist.** The capstone twice calls `docs/fpga-board-checklist.md` "#264's existing, standing spot-check process" / "already defined by #264." Verified: 404 at `master`, zero code-search hits, never added on any fetched ref. #264's own body also refers to it as extant. What actually exists is `docs/icestick-bitstream-handoff.md` (PR #267). Either the checklist is unlanded #264 work that both bodies mis-tense, or the reference should be repointed — as written, AC-5 names a fictional owner.
2. **ShiftRegister gap already closed.** The capstone's parenthetical ("Sweep-06's measurement also names a ShiftRegister-specific export gap; no issue currently owns it") and sweep-06's "fails on four Memories and three ShiftRegisters" are stale: master's `HdlExporter.EXPORTED` includes `ShiftRegister.class` (barrel-shift rendering, `HdlExporter.java:709-744`). The flagship now trips only on Memory-family elements — the predecessor logic (#358/#873) is unchanged, but the flagged-for-successors gap is already resolved.
3. **Cost band contradicted by its own children.** Capstone `band_mw: "3-5"` beyond predecessors; the children sum to well above it: #597 (3–5) + #598 (3–5) + #599's tasks (1–2) ≈ **7–12 mw**, before the unfiled AC-3 lane work. Either the feature bands are padded or the capstone band is understated by ~2×; as filed, the arithmetic does not close.
4. **#599's `evidence_commit` (`646d5ae`) is not on master** — it is a checkpoint commit on the transient review branch `claude/github-issue-review-agents-j99xga` ("to be swept in final cleanup"). Its citations happen to hold on real master, but the anchor will dangle when that branch is deleted. #264's `29afb26` and #873's `e44b0535` resolve fine.
5. **Local-clone note (not an issue defect):** the reviewed checkout at `/home/anadon/Documents/code/JLS` is behind origin (6744b56, 2026-07-28 vs c5cee1b, 2026-08-05); all claims above were verified against GitHub master, not the stale checkout.
6. **The "cheapest move" (release + docs page + screenshot) remains unfiled**, exactly as the capstone self-reports. Since half the competitive loss is a shipping loss, leaving the zero-cost item unowned while 3–5+ mw of feature work is fully decomposed is the most upside-down part of the current filing state.

## Verdict

**ready-with-gaps.** Start now on #632/#638 (both only need landed #264 substrate and open #288/#492 coordination); the graph is real, acyclic, and free of closed-issue edges, and the code anchors verify. Before or alongside first implementation PRs, close these named gaps:

1. File the AC-3 owner(s): #264's required real-toolchain CI lane + the differential-oracle round-trip lane (+ the two slivers), or add them to an existing issue's `requires_tasks`.
2. REPLAN #599 vs the capstone's AC-4 table-row framing before #645 starts — both documents already agree this is unresolved.
3. Repoint or land `docs/fpga-board-checklist.md` so AC-5 names a real artifact.
4. Reconcile #634's `PinPlan.check`/#264 attribution with #598's `BoardPreflight.check`/#638 ownership.
5. Give #522's AC-2 real-toolchain leg a named owner (naturally the same issue as gap 1).
6. Fix the band arithmetic (capstone 3–5 vs children ≈7–12) and drop the stale ShiftRegister flag; mirror #522 into #358's/#873's `serves_capstones` at their next touch.
