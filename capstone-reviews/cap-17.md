**Capstone:** CAP-17 (#312) — a design too large for any one machine elaborates, simulates and reports as one design across a cluster
**Verdict: BLOCKED** — by the capstone's own recorded entry trigger, which has not fired. The body itself directs "no work should be scheduled against it"; this review confirms that disposition is sound and the tree beneath it is coherent enough to hold in deferral, with a short list of bookkeeping defects to fix at the next corpus touch.

### 1. Decomposition

**The nine required features are all filed, open, and correctly cross-linked.** `requires_features: [318, 332, 333, 335, 336, 354, 362, 363, 370]` — all nine are open, and every one declares `serves_capstones` including 312, exactly as the body claims. The deliberate exclusions (FEAT-005 #353 moved to `blocked_by`-only; FEAT-057 #350 removed) are argued in the body and match #508's disposition ("Defer … CAP-17 #312 (split: campaign axis → CAP-06/09; fund FEAT-005 now)"). #353 is open with `blocked_by: []` and `serves_capstones: [296, 301, 295, 300, 306]` — correctly no longer naming 312.

**Defect — the FEAT-057 split is executed on one side only.** #350 is still a **native sub-issue of #312** and its machine block still declares `serves_capstones: [312]` — the only capstone it names — while CAP-17 no longer requires it. Neither promised new home has adopted it: CAP-06 (#300) explicitly declines ("FEAT-057 is **not** added to `requires_features` … no such REPLAN exists yet") and CAP-09 (#306) lists it only under `related` as beneficial-not-required. The campaign axis (6 open TASKs, #674–#683) is currently owned by no capstone's required set while hierarchically parented under a deferred capstone. Not CAP-17's outcome at risk, but the re-home REPLAN on #350 needs to actually happen, and the sub-issue parent link moved.

**Task-level decomposition beneath the required rows is incomplete — tolerable in deferral, not implementation-ready:**
- #332 (FEAT-055) has `requires_tasks: []` while five open native TASKs (#600–#606) sit under it; its own body says the filed cycle-refusal task #604 "is scoped against the old artifact form" and lists five `planned_tasks` **not yet filed**, including the two it calls unblocked-and-startable.
- #370 (FEAT-054) has `requires_tasks: []` with six open native TASKs, three of which (#846, #848, #851) self-describe as "superseded … carries no independent scope … recommended for closure" yet remain open.
- #333 (FEAT-056) requires five of its six native TASKs but excludes #836 (whose scope its rewrite moved into #332's *unfiled* cut-legality item), and carries an unfiled planned task (partition-independent same-time event ordering) that it says #834 "cannot honestly claim criterion 1" without.

### 2. Acceptance criteria

**Upward composition is unusually strong.** AC-2 is explicitly engineered to close the each-child-passes-while-the-capstone-fails hole: neither FEAT-055 nor FEAT-056 may close on single-process criteria alone — a joint partitioned-load → distributed-simulate → compare smoke at N=2 gates both, and the falsification test must be shown red first with the transcript recorded. AC-1/AC-3 exercise the integrated path; AC-4 forces every number through FEAT-009's (currently nonexistent — grep verified 0 files) measurement gate; AC-5 pins the pedagogy floor to a measurable (`entry(Group.` count = 32, verified at master); AC-6/AC-7 cover suspend/resume and the disk budget with named kill criteria (K17-5).

**One reconciliation gap:** CAP-17's final AC demands FEAT-056 file "the numeric lookahead threshold that triggers refusal," but the mechanism has since migrated: #332's planned (unfiled) zero-lookahead cut-legality task "declares no numeric threshold, only presence or absence of a Timed driver," and #836 derives lookahead per cut. Three artifacts now describe the refusal contract differently; reconcile ownership and the threshold's definition before resume.

### 3. Dependency chains

Verified against each required feature's actual machine block. The graph is **acyclic** (checked: 336←315; 335,354←353; 370←322,335; 332←319,336,353,370; 333←318,332,363; 362←335; 363←∅; 318←468). But **three of CAP-17's six `blocked_by` edges are stale**, per its own re-derivation rule:

- **#348 (FEAT-051 RESIDUAL) is CLOSED as duplicate** (2026-08-04, superseded by #169, which is open), and #333's `blocked_by` is `[318, 332, 363]` — it no longer names 348. This also voids §3 risk 4's claim that transport reuse "is enforced by an ordering edge (#333 `blocked_by` names #348)": that enforcement now exists **nowhere** — neither #333 nor this capstone references #169. The one structural guard against a second networking stack is gone.
- **#337 → #318 no longer exists:** #318's `blocked_by` is now `[468]` (open TASK-0007), with the note that it "no longer names 318/319/340."
- **#319 → #318 and #319 → #363 no longer exist:** #363's `blocked_by` is `[]`. (#319 → #332 still holds, so #319 stays in the list — but for one edge, not three.)
- Mermaid edge **F030 → F054 is stale:** #370's `blocked_by` is `[322, 335]`, and #362's own block records the mirror edge as deliberately "moved to related."

Valid edges confirmed: 315→336, 322→370, 353→{335, 354, 332}, 319→332. No external unfunded prerequisite sits on a critical path *other than the entry trigger itself*, which is the point of the deferral.

### 4. Staleness / gaps

- **The evidence commit repeats the exact defect the body flags.** `evidence_commit: 333523a` is annotated "Current default-branch HEAD" — it is not. GitHub compare shows master (`c5cee1b`) **behind 333523a by 15 commits**; the commit (2026-08-09) sits on an unmerged branch. This is the same "corpus bleed" (#493/#508) the body cites as the reason 2d0ca9d was distrusted. Mitigating: I re-verified every load-bearing code claim at actual master `c5cee1b` and all hold — `Circuit.java:1345` (`LinkedList<WireEnd> ends`) and `:1369` (`ends.remove(vend)`), `FileAbstractor.java:65` (`MAX_CIRCUIT_TEXT_BYTES = 64L << 20`), Palette count 32, `Transport.java:38`, `Element.java:24` stable id, `Memory`'s `Map<Integer,BitSet>`, MeasurementGate/CalibrationFixture grep = 0, `SimpleEditor.java` = 5,852 lines. The figures are right; the provenance label is wrong.
- Three of the nine required rows (#318, #333, #354) still carry `evidence_commit: 2d0ca9d` — the unreachable commit — while #370/#362 re-derived and #336/#363 moved to 333523a (itself unmerged). Evidence provenance across the tree is inconsistent; the body's own Completion Criteria already demand full re-derivation before funding.
- `blocks: []` is self-flagged as needing a re-scan against the grown corpus (FEAT-058/059/060, #523) — honest, still open.
- Cost bands (~75–126 mw required, ~32–54 mw marginal) are declared "provisional and unaudited," consistent with #508's finding on capstone band arithmetic; acceptable only because nothing is being funded.
- The entry trigger's factual basis still holds at master: largest fixture 1,038 elements, `ARCHITECTURE.md` line 341 records #221's sole-strategy decision with its revisit trigger unfired.

### Verdict: BLOCKED

The external prerequisite is the capstone's own entry trigger — a named user with a drawn design near 10^6 elements, or the #221 revisit trigger firing on a real `riscv/`-trajectory design — and neither exists. This is a deliberate, well-reasoned deferral, not a decomposition failure; the correct action is to schedule nothing, exactly as the body says. **Fix at next corpus touch (none changes the verdict):** (1) execute #350's re-home REPLAN and move its sub-issue parent off #312; (2) re-derive `blocked_by` — drop the dead #348 edge (closed duplicate → #169), re-point or re-justify the transport-reuse guard, drop 337, narrow 319's rationale; (3) correct the 333523a "default-branch HEAD" annotation; (4) close the three self-declared-empty TASKs under #370 and reconcile the lookahead-refusal ownership between #332, #333/#836, and this body's AC.
