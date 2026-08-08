# Issue #544: FEAT-C26-3: a blind student builds and simulates a two-gate circuit by keyboard, hearing each element, each connection and each signal-state change as it happens
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a disciplined planning issue with a genuinely good instinct — it names its own blow-out risk and puts a feasibility spike in front of the expensive band rather than committing 5-8 mw up front. But the spike gate and the acceptance criteria contradict each other about what's mandatory, the CI rig the issue names as "the automatable screen reader" substrate has no screen-reader capability in it today, and a dependency the issue's own text asserts (static per-element accessible children from #355/#380) is missing from the ordering fields that would actually enforce it.

## Findings, most severe first

### 1. (High) The acceptance criteria and the feasibility-spike gate contradict each other on whether live announcements are mandatory

The spike gate lays out three branches, one of which is explicitly "live announcements unreachable through Swing → re-scope to navigation + on-demand state query, record the exception... REPLAN #507." But acceptance-criteria bullet 1 states flatly: *"A scripted Orca session builds and simulates a two-gate circuit by keyboard alone, with spoken state changes asserted, in the Wayland CI rig (CAP-26 AC-2 `OrcaLabSessionTest`)."* "With spoken state changes asserted" is unconditional — it does not branch on the spike outcome. Bullet 3 then reopens the branch: *"Live signal-state changes... are announced — or the reduced announcement set is recorded as a named VPAT exception... never papered over."* So bullet 1 says the live-speech test must pass; bullet 3 says it's acceptable for it not to, provided the gap is recorded. A reviewer checking "did AC pass" against bullet 1 alone would reject a legitimately re-scoped (per the spike's own escape hatch) delivery; a reviewer checking bullet 3 alone would accept it. The issue never says which bullet governs when they diverge.

**Recommendation:** rewrite bullet 1 to explicitly carry the same branch as bullet 3 (e.g., "...with spoken state changes asserted, or navigation-only if the spike found live announcements unreachable"), or delete the redundant unconditional claim in bullet 1 and let bullet 3 be the sole source of truth.

### 2. (High) The named CI substrate for the central test has no screen-reader capability, and this gap is invisible in the issue's own scope/cost

The issue asserts "Orca in the #101 Wayland CI rig is the automatable screen reader" and requires `OrcaLabSessionTest` to run there. But #101's body — the full feature spec for that rig — describes exactly two capabilities: booting the GUI under JBR's `WLToolkit` on headless sway, and screenshotting/pixel-diffing it (`scripts/wayland-rig.sh`, `HelloSwingControl`, `PIXEL_DIFF_MIN`). It contains zero mentions of Orca, AT-SPI, `atk`, or any screen-reader/accessibility-bridge tooling anywhere in its abstract, capability statement, interface contract, or completion criteria. A repo-wide check confirms this is not just an omission in the issue text: `.github/workflows/ci.yml` has no `Orca`/`AT-SPI`/`atk` reference at all, and `src/jls/elem/` has zero `AccessibleContext`/`Accessible` overrides on any element class today (the only `Accessible*` hits in `src/` are `TextDialog`, `SigGenDialog`, `SimpleEditor`, `MemoryDialog`, `ElementFormDialog`, `KeyPad` — all chrome/dialog work from #75, none of it circuit-element scaffolding). On Linux, Java accessibility reaching Orca additionally requires a native ATK bridge (`java-atk-wrapper` or equivalent) installed and enabled in the CI image — an infrastructure dependency this issue never names, costs, or assigns an owner for. The "feasibility spike" is the right instinct for exactly this uncertainty, but the issue frames the spike as proving out *Swing's* announcement capability, not as first building screen-reader-capable CI infrastructure that doesn't exist yet — a materially larger and differently-owned piece of work than "a thin slice proving Swing can deliver a live signal-state announcement."

**Recommendation:** split the spike into (a) stand up Orca + an AT-SPI query/assert harness in the #101 rig (infrastructure, arguably belongs to #101 or a new task under it) and (b) prove Swing can drive a live announcement through that harness (the feature-specific question). Cost and gate them separately; right now the whole 5-8 mw band's go/no-go rests on a spike whose scope silently includes CI infrastructure the issue doesn't mention building.

### 3. (Medium) A dependency the issue's own prose asserts is not encoded in any ordering/blocking field

The boundary notes say: *"Extends — does not re-own — FEAT-011's residual #355... and its task #380 (elements as named accessible children). Those cover static reporting; this feature owns traversal, connection announcements, and live simulation speech."* Element-graph *traversal* and *connection announcements* are not meaningfully buildable without the static per-element `AccessibleContext` children #380 is scoped to add — you can't traverse or announce a graph of accessible nodes that don't exist yet. Yet the only entry in `ordering_after` is `[FEAT-C26-6]` (#549, the operability ratchet). #355/#380 are open (per this fleet's own #355 review, "#380/#381 filed and open" as of today) and appear only in a prose "boundary note," not in any field that would stop someone from starting #544 before the static scaffolding it depends on exists.

**Recommendation:** add #380 (or #355, if #380 hasn't been individually confirmed sufficient) to `ordering_after`, matching the treatment already given to #549.

### 4. (Medium) "Connection context" is underspecified and gameable the same way #355's totality criterion is

AC bullet 2 requires every element and connection to report "a spoken name, role, and connection context." There is no definition of what "connection context" must contain — net name, source/sink element identity, pin index, signal direction? A build that announces a generic string like "connected" for every wire, or "Input 1 of AndGate1" without saying *what it connects to*, satisfies the literal bullet while leaving a blind user unable to tell which two elements a wire actually joins — the same failure mode this fleet's #355 review already flagged for that issue's role/name totality criterion (mapping every element to a generic name/role passes a non-null check without conveying anything). Since #544 explicitly builds on #355's model, it inherits the same specification gap one level up, and doesn't add the missing precision itself.

**Recommendation:** name the minimum content of "connection context" explicitly (e.g., "announces the connected element's name and pin/net identity, not merely that a connection exists") and assert it against a golden table in `OrcaLabSessionTest`, not just non-emptiness.

### 5. (Medium) No debounce/rate-limit requirement for live announcements during simulation — a literal pass could still be unusable

AC bullet 3 requires live signal-state changes to be announced during simulation. JLS simulations commonly include a `Clock` element toggling every tick; a naive implementation firing one accessible-name/value-change event per state transition would flood Orca with announcements during any clocked run — satisfying "signal-state changes are announced" literally while making the feature unusable for exactly the population it's built for. Nothing in the acceptance criteria, the spike gate, or the boundary notes addresses throttling, coalescing, or scoping announcements to elements currently focused/selected versus the whole circuit.

**Recommendation:** add an explicit criterion bounding announcement volume/rate (e.g., only the focused/traversed element's state is spoken live, or changes are coalesced per simulation step), verified by the same Orca session test rather than left to implementer judgment.

### 6. (Low) Spike itself carries no cost estimate or owner, despite gating a 5-8 mw band

CAP-26's cost table gives PF-3 as "5-8 mw (Swing accessible-tree work plus live announcements; the band's risk)" — but neither #507 nor #544 gives the feasibility spike its own mw estimate, timebox, or named owner. A funding gate with an uncosted, unowned precondition risks sitting indefinitely (nobody is on the hook to run it) or being run cheaply and superficially to unblock the "real" work it's supposed to be gating.

**Recommendation:** cost and assign the spike explicitly (even a small fixed band, e.g. "0.5-1 mw, timeboxed"), and record its outcome as a REPLAN comment here per the issue's own re-planning convention used elsewhere in the CAP-26 cluster.

## What's solid

- The three-way spike-gate structure (fund / re-scope / stop) is a genuinely good risk-management shape or would be if the AC didn't contradict it (see #1).
- The NVDA-as-documentation-only stance is honest and consistently drawn: it's stated as manual/non-automated in both this issue and #507, with no place claiming otherwise.
- The #75/#355 boundary-of-scope reasoning (comment 1) is careful and specifically argued against rule 3(a)/(c)/(d), not just asserted — chrome vs. circuit, static vs. dynamic are real, checkable distinctions.
- `ordering_after: [FEAT-C26-6]` (#549) is a real and correctly-identified prerequisite (#549 also names itself as "the substrate FEAT-C26-3's screen-reader work stands on" — the edge is mirrored, not asserted one-sided).

## Verdict rationale

The core idea is sound and the spike-first funding structure is the right shape for a genuinely risky band. But finding 1 means the stated verification could pass or fail depending on which acceptance bullet a reviewer reads, finding 2 means the named CI substrate for that verification doesn't exist yet and its build-out isn't scoped or costed anywhere, and finding 3 means the issue can be started out of order relative to a dependency its own text calls load-bearing. These are fixable by editing the issue body (tighten the AC, split and cost the spike, add the ordering edge) rather than by re-deriving the whole plan — but they need to happen before any spike work is scheduled, since the spike's scope and pass/fail bar are exactly what's underspecified. **needs-rework.**
