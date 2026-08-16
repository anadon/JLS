**Capstone:** CAP-14 (#309) — JLS's own analog answer and real ngspice's agree to a declared 3.16e-3 envelope
**Verdict: not-ready** — the filed tree cannot deliver the promised outcome even if every filed child closes green: the nonlinear engine (FEAT-046b) that Tier B, AC-3's Newton ratchet, and KC-14-4 all depend on is planned-but-unfiled, and the capstone's own AC-7 precondition (a recorded analog-acquisition decision) does not exist on `master`. The cost/roster story is additionally contradicted by CAP-12's current machine block.

All code-level evidence claims in the issue body were re-verified against `master` and **hold exactly** (see §4). The problems are in the issue graph, not the evidence.

---

## 1. Decomposition

**All seven `requires_features` rows are filed and open** — #351, #331, #367, #368, #336, #317, #315 — and the native-child placement is correct (#351 is #309's only native child; #351's `serves_capstones` is `[309]` alone, all other rows are shared spine parented elsewhere). Every `requires_tasks` entry of every required feature is open: #368→#434; #336→#468, #373; #315→#372, #375. No required edge points at a closed issue.

**Blocking gap — FEAT-046b is unfiled, and the capstone's outcome runs through it.** The machine block asserts `planned_features: [] # all seven required rows are filed; none pending`. One level down this is false in substance:

- #351 (FEAT-046) has been re-scoped to **v1: linear LTI only** — TASK-0097 (#463) is "dense-LU … R/L/C/V/I only, fixed declared timestep, **no Newton loop**"; TASK-0100 (#397) is re-scoped to "v1's own closed-form fixtures … rather than the full nonlinear fixture set."
- #331 (FEAT-049) still promises the full outcome at feature level ("the circuit they drew converges"), but both of its filed children are gated on an engine that has no issue number: TASK-0103 (#465) is "blocked on **FEAT-046b's Newton/Jacobian types (not filed)**" and TASK-0104 (#464) is "**DEFERRED TO FEAT-046b** — no Newton loop exists to harden under FEAT-046 v1." TASK-0105 (palette registration) is also not filed.

Consequence: every filed task under #351 and #331 can close and CAP-14's claims 2 and 3 hold only for two-to-three linear fixtures. The "device-shaped — diodes, op-amps, transistors — not two linear circuits" corpus (§2), Tier B convergence torture, AC-3's Newton-iteration equalities, and KC-14-4's 10%-of-Tier-B threshold all have **no filed owner**. FEAT-046b must be filed (and added to this capstone's `requires_features` or `planned_features`) before orchestration, or the capstone's outcome statement must be re-scoped to linear-only.

**Mirror drift with CAP-12 (#305) — the funding premise is refuted by the sibling's filed roster.** CAP-14's Status comment and Cost section say "CAP-12 funds four of this set's five analog features first" and compute the 10-16 mw marginal band on "FEAT-046/047/048/049 (44.5-68 mw) are shared with #305, which funds them first." #305's current machine block says the opposite: "**FEAT-046 (#351 …) is NOT required by this capstone's default design** … is CAP-14's (#309) own deliverable if AC-6 below decides a general solver is warranted," and its #331 row is "**NARROWED** … no matrix, no Newton loop, no convergence hardening." #351's `serves_capstones: [309]` confirms it. This is exactly the one-sided roster change §5's re-planning protocol forbids ("a one-sided change is the drift the tier model warns about") — and it fired against this issue without this issue being updated.

**Minor double-ownership.** The Cost section books "the offline comparator" as capstone-owned integration with no feature row, but TASK-0100 (#397, child of #351) now owns exactly that comparator, envelope, and detectors for the linear fixtures. Not harmful, but the Cost text and #397 should agree on who owns the comparator when the corpus grows past v1.

## 2. Acceptance criteria — do children compose upward?

- **AC-1 (deck accepted, byte-identical golden):** inputs exist — #336 (stable net names), #367 (physical seconds for `.tran`), #315 (emitter totality), #402 (SPICE-suffix parser in `jls.core`). The deck emitter itself is capstone-owned with no filed task; acceptable under the `requires_tasks_exception: []` convention but it is unowned work at orchestration time.
- **AC-2 (3.16e-3 envelope, offline, every push):** #397 implements precisely this for the linear fixtures, including committed provenance — good downward push. The **20-consecutive-run promotion rule per device family** has no owner beyond v1, because device families themselves are gated on unfiled FEAT-046b.
- **AC-3 (statistics ratchet as equalities):** as written ("Newton iterations … equalities"), **cannot be satisfied by any filed child** — v1 has no Newton loop. #397's "detectors that catch a regression staying inside it" is the v1 analogue; the AC as specified needs FEAT-046b.
- **AC-4 (anti-cheat lower bound ~1e-13):** owned by #397 + #481's determinism work. Composes.
- **AC-5 (cross-platform byte-identical, required check):** split across #481 (StrictMath allow-list, pivot ratchet, digest — filed, open) and #317's promotion leg. The promotion leg now runs #317 → #406 (TASK-0017, open) plus #111/#265 direct scope — see §3 for the deepened chain. The AC's own caveat is honest: the hard determinism problem (transcendentals, adaptive step) only appears with FEAT-046b, which again is unfiled — so the part of AC-5 "the whole claim rests on" for nonlinear devices is unowned.
- **AC-6 (mixed-signal crossing rule):** #368/#434 filed, open, correctly framed as definition-plus-self-consistency. Composes.
- **AC-7 (analog-acquisition decision):** **unmet and unowned.** `ARCHITECTURE.md` on `master` contains no analog/SPICE capability decision (the only grep hit is the unrelated word "analogue"), no capability-roadmap document exists, and no issue owns producing the decision. By the capstone's own words: "Until it exists, this AC is unmet regardless of code state" and "FEAT-046 and FEAT-049 should not begin execution under this capstone's authority alone." That gates 38.5-59 of the 50.5-79 mw standalone cost.
- **AC-8 (envelope inputs committed and re-measured):** self-declared provisional; no filed task owns the re-measurement. Tolerable as close-out work, but the 3.16e-3 number that #397 already hard-codes in its title inherits the provisional status.

Composite risk: every filed child can pass while the capstone fails — specifically claims 2/3 over the device-shaped corpus, AC-3 as written, and AC-7. That is the definition of an AC gap.

## 3. Dependency chains

- **Acyclic: confirmed.** Walking every required feature's current machine block: #351 `blocked_by []`; #331 `[351]`; #368 `[331,351]`; #336 `[315]`; #315 `[]`; #367 `[]`; #317 `[353,354,363]`. Union closure {315, 331, 351, 353, 354, 363} — no capstone, no #309, no cycle. No feature declares `blocked_by` naming a capstone.
- **Stale DAG-state text:** the issue asserts #317's closure is `{353, 354}`. #317's actual `blocked_by` is now `[353, 354, 363]` — **#363 (FEAT-035, checkpoint/resume by deterministic replay, open)** was added. AC-5's required-check status therefore sits behind three unrelated performance/infrastructure features (#353, #354, #363), deeper than the issue's plan states.
- **Stale task pointers inside #317:** its planned_tasks cede the four literal ci.yml promotion edits to "#265/#111's own filed slices (#661-664 Windows, #667-670 macOS)" — **all eight are closed as duplicates**. The work is re-owned by #406 (open, #317's own child) and #111/#265 direct scope (both open), so the chain is intact, but the text points at dead issues.
- **No closed/redirected issue in `requires_features` or any `requires_tasks`.** The unfunded prerequisite on the critical path is not an external tool — it is the unfiled FEAT-046b (§1) and the unmade AC-7 decision (§2).

## 4. Staleness / evidence

**Evidence claims: all verified on `master`.** `evidence_commit` 8288226 resolves and is an ancestor of remote `master` (6 behind head). Re-run at head: `grep -rli spice src/` → 0; `grep -rliE "analog|nodal|newton|adc\b|dac\b" src/` → 0; `ProcessBuilder` src/ → 0, test/ → 15; `src/jls/sim/Simulator.java:36` `protected long now = 0;`; `docs/simulation-semantics.md:26` dimensionless integer; `src/jls/JLSStart.java:780-781` HDL-only export; `src/jls/elem/Element.java:18` sealed permits; no `src/jls/netlist/`; `ci.yml:156`/`:263` `continue-on-error: true`, `:41` JDK-matrix advisory; emitter line counts 752/1149/199 exact; `docs/parity-contract.md` absent exactly as the evidence note says.

Stale references, none individually blocking:

- **#303 (CAP-11) closed 2026-08-03 as duplicate** (merged into #308, whose title is now "no analog solver required"), yet AC-7, Proposed disposition #4, §Cost and Related Issues still cite #303 as a live capstone sharing the analog-acquisition precondition. The set of capstones sharing that precondition has shrunk to effectively #305 and #309 — which strengthens, not weakens, the case that AC-7's decision must be made deliberately.
- **#307 (CAP-13) closed 2026-08-03 as duplicate**, yet §3, Related Issues, and the marginal-cost caveat ("if #307 funds it [FEAT-004] first, this capstone's marginal band over-states by up to 3 mw") treat it as live. #336's own `serves_capstones` already removed 307.
- **Cost bands contradicted by scope** (the decisive one, detailed in §1): the 10-16 mw marginal band depends on CAP-12 funding 44.5-68 mw of shared spine first; CAP-12's filed roster no longer funds #351 at all and funds only a narrowed #331. Until CAP-12's AC-6 spike verdicts (or the rosters are re-mirrored), CAP-14's realistic marginal cost is close to its standalone band.
- **#367's shape changed** from "physical time base + nominal real-time scalar" to "ratify 1 ns, then fix the quantum at 1 ps tree-wide" — still serves #309 and still yields seconds for `.tran`; description drift only.
- **Open questions that block start:** OQ-1 (ngspice license/attribution plan) is self-declared "a blocking precondition, not a can-ride-along note" and no filed issue owns it; OQ-4 (dialect target) "blocks execution of FEAT-049 (#331)"; OQ-9 blocks #331's TASK-0103 scope — #465 is filed with the diode-only scope, consistent with OQ-9's default.

## What must happen before this capstone is orchestration-ready

1. **Record the AC-7 decision** (analog acquisition, `ARCHITECTURE.md` or capability-roadmap amendment) — or explicitly re-scope CAP-14 to the linear-only v1 slice that #463/#481/#397/#402 already deliver.
2. **File FEAT-046b** (nonlinear Newton/Jacobian engine) and add it to this capstone's roster; re-derive §2 sufficiency/minimality and the mermaid graph for the eight-row set (per this issue's own §5 protocol).
3. **Re-mirror with CAP-12 (#305):** either restore #351/full-#331 to CAP-12's roster or rewrite CAP-14's Status comment, §Cost basis and marginal band to reflect that CAP-14 now funds the general solver alone.
4. Housekeeping: update the DAG-state closure for #317's `blocked_by [353,354,363]`; re-point #303→#308 and drop the #307-funds-FEAT-004 caveat; give OQ-1's license/attribution plan an owner.

Items 1-3 are decomposition/AC work on the issue corpus itself — hence **not-ready** rather than blocked: the gate is fixable inside the tracker, and the linear v1 slice (#463 → #481/#397/#402, plus #336/#315/#367 spine) is well-formed and could be started under CAP-12/its own authority while the capstone-level fixes land.
