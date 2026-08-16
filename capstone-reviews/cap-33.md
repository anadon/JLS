**CAP-33 (#517) — readiness-to-undertake review.** Tree walked: #517 → FEAT #575 (TASKs #744, #746, #748, #751, #752), FEAT #576 (#755, #757, #759), FEAT #577 (#761, #763, #765), FEAT #578 (#767, #769, #772); cross-referenced issues #300/#369/#466 (CAP-06 verdict slice), #502, #509, #548, #552, #311, #513; code claims verified against the master checkout.

**Verdict: not-ready.** The corpus is unusually honest — most conflicts are flagged in the issues themselves — but three of them are flagged *as requiring a maintainer decision before execution*, and one planned feature (PF-0) has no issue at all. An orchestrator starting implementation today would hit a self-declared "do not execute as written" on the first content task. The repair is issue-editing work, not engineering work: an estimated half-day of decisions closes every blocking item below.

---

## 1. Decomposition

**PF-0 is unfiled, and its scope is double-described.** #517's own machine block says `planned_features: [PF-0 unfiled, ...]`, the band note admits PF-0 is "still unfiled and not yet estimated," and the demo slice ("PF-0 + PF-1(b), ~1-2 mw") is anchored on it. A repo-wide search finds no issue claiming CAP-33 PF-0. Meanwhile #578's tasks #767 (kit layout/manifest schema) and #769 (`jls kit verify` validator) deliver substantially the same artifacts — but #578 orders after #575/#576/#577, i.e. the format+validator arrives *last*, while PF-0 was designed to arrive *first* as the capstone's cheapest proof. Either file PF-0 (and reconcile its ownership against #767/#769), or amend #517 to assign PF-0's scope to #767/#769 and retire the early demo slice. As filed, the capstone's stated entry point does not exist.

**The #575 one-lab rescope vs. the #744/#746/#748/#751 eight-lab pack is an open, self-declared conflict.** #575 AC-1 (rewritten 2026-08-12) scopes the feature to exactly one demonstration lab and disclaims "broader, course-scale lab authorship." Its four open children still compose to an 8+ lab pack: #744 ships two labs, #746 three more, #748 a three-lab datapath spine, #751 reviews "every shipped lab." #575's boundary note says "the maintainer needs to decide whether those four tasks still stand as filed, are rescoped... or are superseded"; #748 says "this task's scope needs to be revisited by the maintainer rather than executed as written." The FEAT's task tier does not compose to the FEAT's own AC; nobody can start #744 without contradicting one side of this. This is the single largest not-ready item.

**#757 is declared stale by its own parent.** #576's estimate-scope note: #757 "predates this issue's identity-contract framing and needs re-cutting against the ACs above before its own band can be treated as current." The re-cut has not happened (see §2 for the concrete contradiction it left behind).

**Otherwise the decomposition is clean.** All four filed PFs map 1:1 to open FEATs with native sub-issue links; every TASK carries `part_of_feature`; boundary/ownership splits against #466 (#757's "byte-identity proves consumption"), #509 (#765's "outreach is #509's action, not a gate here"), #502, #548, and #552 are explicit and non-overlapping. No required edge points at a closed issue as a prerequisite (#58, #79, #130 are closed but cited as conventions, correctly).

## 2. Acceptance-criteria composition

**Degraded-mode grading is specified twice, incompatibly.** #576 AC-2's degraded cohort report is a three-status vocabulary — exactly `ran` / `failed to run` / `output differs from reference` per student, "using today's stdout-diff oracle." #757 AC-3's degraded mode mandates the verdict field read exactly `"no verdict channel available (exit 0/1/2 only)"` — "never PASS or FAIL, and never synthesized by diffing... output," and "never a second comparator." #759's result vocabulary then cites "the AC-3 degraded-mode three-status reading," conflating the two. Each issue can pass its own ACs while FEAT-C33-2 ships two contradictory degraded reports. This is precisely the reconciliation #576 already says #757 needs; it must land before #757/#759 are startable.

**Kit-content license is contradicted at the first task to execute.** #744 AC-2 pins **CC-BY-SA-4.0** for all lab content, "decided now so it is not re-decided per lab," and claims this "satisfies #575 AC-6's requirement." But #575 AC-6, #752 AC-2, and #772 AC-1 ratify **CC-BY-4.0 for prose + CC0-1.0 for circuits/vectors**, and #772 records CC-BY-SA-4.0 as *considered and set aside* (share-alike leaking into instructors' private courses is the stated reason). #744 — the first content task on the critical path — would embed the superseded license in the first shipped manifests. One-line edit to #744 AC-2 fixes it.

**A stale cross-note in #751, in the benign direction.** #751's boundary claims #517's KC-33-2 "gates on time-budget overrun (more than 50%)... has not been updated to match." #517's current KC-33-2 text already gates on author-questions/unaided-completion, matching #751 — the needed edit evidently landed; #751's note is now the stale half. Cosmetic.

**Upward composition otherwise holds.** #517 AC-1 (validator + one recipe-derived kit) resolves to #744/#769 + #575 AC-2; AC-2 (workflow walk) to #576 AC-4/#759; AC-3 (compatibility oracle, held-not-claimed second course) to #577/#761/#763 with the fallback mirrored verbatim in #578 AC-3 and #765 AC-1; AC-4 (license/provenance) to #752/#772. Anti-vacuity clauses (non-empty worked example, no empty CI matrix, no zero-entry index, census ratchet) are consistently present at the task tier — every "child passes while capstone fails" hole I probed except the two above is closed.

## 3. Dependency chains

**The real chain is longer than the machine blocks and correctly documented in #578:** #578 → {#575, #576, #577} → #466/#369 (CAP-06 verdict slice, open). #575 AC-5 and KC-33-1 correctly block the "autogrades out of the box" claim on #466; #576 splits per-criterion (AC-2/AC-5 on #466, rest unblocked); #744/#763 correctly disclaim #300 edges by grading over today's documented `-t`/VCD contract. #466 is open but in-repo and planned — a funded prerequisite, not a blocker.

**A nominal cycle exists at issue-closure granularity: #575 → #751 → #578 → #575.** #578 `ordering_after: [575, 576, 577]`; #575's child #751 `ordering_after: [..., 578]` (review-record slot lives in #578 AC-1's manifest). The corpus breaks this deliberately at AC granularity (#575 AC-6 explicitly refuses to block on #578; #767 AC-4 keys on #575 AC-1's *lab existing*, not #575 closing; #751 keys on #578 AC-1's *slot existing*). Any orchestrator that schedules on whole-issue closure from the machine-block edges will deadlock; scheduling must consume the AC-level carve-outs. Worth stating in whichever tool reads `ordering_after`.

**One genuinely external, unfunded prerequisite — correctly fenced off the critical path.** #765 AC-1 cannot start until Dr. Siever publishes a kit from his own repository (and until #578 ships the reference-vs-vendor manifest fields). No agreement exists; #509 owns the outreach as an action, not a gate. #517 AC-3's 8-week hold-not-claim fallback and #578 AC-3's mirrored "held — not silently satisfied" clause mean PF-0..2 ship regardless. This makes the capstone *not-blocked* in the verdict sense, but the "one real hosted course" headline should be expected to stay open past the rest of the tree.

**Unresourced human dependency, named but unassigned:** the non-author reviewer required by KC-33-2 / #751 / #744 AC-3 / #578 AC-4/AC-7 (a human, explicitly not a second AI walkthrough). #575's band is "provisional pending that resourcing decision." Not a start-blocker, but a close-blocker for every content task; no candidate is named anywhere in the tree.

## 4. Staleness and evidence

**One cited evidence doc does not resolve on master.** #517's ground-truth section, #746, and #757 all cite `docs/capability-roadmap/lf-04-formal-and-grading.md`. That file exists only on the unmerged branch `j99xga-work` (`origin/claude/github-issue-review-agents-j99xga`); `docs/capability-roadmap/` is absent from master. Merge the roadmap docs or retarget the citations before implementation treats them as in-tree ground truth.

**Verified and holding:** `docs/batch-interface.md` §1 defines exactly three exit statuses (0/1/2), none meaning "ran fine, answer wrong" — the capstone's core motivation is accurate. `JLSStart.FLAGS` at ~line 759, `Util.isValidName` at 219-234, `FileAbstractor.openCircuit`, `LoadError.Category`'s seven values (matching #759's vocabulary exactly), `Circuit.FORMAT_VERSION` at line 102, `CellValidator`'s teachable-reject limits (async reset, set/reset, wide arithmetic, clocked memory), `NetlistImporter`, `examples/autograde/autograde.py` + `AutogradeBridgeExampleTest`, `test/fixtures/legacy-4.1/README.md`, `resources/help/**` + `HelpTopicsTest`, the five `riscv/` scripts #748 builds on, and CONTRIBUTING's GPLv3 license section (~lines 136-139) all resolve as cited. `resources/samples/`, `test/conformance/`, `examples/coursedelivery/`, and `kits/` do not exist yet — all are declared target paths, correctly future-tense. Minor: `FORMAT_VERSION` is now 2; #767's `format: 1` example is illustrative but dated.

**Cost bands:** #517's 8-13 mw explicitly excludes unfiled PF-0 ("the true total is higher") — honest but incomplete until PF-0 is filed or reassigned. #575's 1-3 is provisional on reviewer resourcing; #576's 2-3 explicitly excludes #757's un-recut band. The bands are internally consistent with scope *as currently written*, which is the problem: #744-#748's bands price the 8-lab pack #575 no longer asks for.

---

## What must happen before start (all issue-edit work)

1. **Decide the #575 scope conflict** (one demonstration lab vs. the #744/#746/#748/#751 pack) and rescope or close the four tasks accordingly — the corpus itself defers this to the maintainer.
2. **File PF-0 or amend #517** to assign PF-0's format+validator scope to #767/#769 and restate (or drop) the demo slice; re-estimate the band either way.
3. **Re-cut #757** against #576's identity-contract/degraded-mode ACs (and fix #759's "three-status" cross-reference) so FEAT-C33-2 has one degraded vocabulary, not two.
4. **Fix #744 AC-2's license** to the ratified CC-BY-4.0/CC0-1.0 split (#772 AC-1).
5. Minor, non-blocking: merge or retarget the `lf-04` roadmap citation; correct #751's stale KC-33-2 note; ensure the orchestrator consumes AC-level ordering carve-outs so the #575/#751/#578 edge set doesn't deadlock issue-granularity scheduling.

Independently startable today, unaffected by the above: #755 (submission layout), #761 (CSE 260M provenance manifest), then #763 (conformance ratchet).
