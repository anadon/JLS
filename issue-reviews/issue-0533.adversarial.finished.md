# Issue #533: FEAT-C23-4: single-event stepping animates each propagation wavefront along the wires — per-gate delay stops being a number in a dialog and becomes time a student watches pass
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

PF-4 of CAP-23 (#504): add a stepping mode to the interactive simulator that
advances "exactly one event" or "one wavefront" (all events at the current
timestamp), animates the affected wires, and shows simulated time advancing.
Four acceptance criteria; a boundary-note comment distinguishes it from #535
(rewind-by-replay).

## Findings, most severe first

**1. The issue never acknowledges that a time-quantum stepping mode already
ships, under the same "Step"/"Animate" vocabulary, and doesn't say how the
new mode relates to it.** `src/jls/edit/InteractiveSimulator.java` already
has `step`/`animate`/`pause`/`resume` buttons (lines 55-64), a user-editable
"Step:" amount field (`stepAmount`, default `1`, line 35), and a `stepEnd`
target (line 37) that `beforeEvent()` (line 736-810) uses to pause the sim
thread once the next event's time exceeds `stepEnd`. With the default step
amount of 1 time unit, repeatedly pressing Step already fires "all events
due by the next tick" and the clock display (`showClock`, `Time: N`) already
updates per step — i.e., the existing feature is already close to a
time-quantum version of "one wavefront," and "Animate" already does
`"repeat step every second"` (tooltip, line 136), which is the same
observable behavior ("watch propagation happen over time") the issue
describes as new. The issue's Outcome text ("Instead of running to
completion, the student advances one scheduled event... at a time") reads
as if no stepping exists today. It needs to say explicitly: is this a new
third mode alongside Step/Animate, a redefinition of what "Step" means
(event-count instead of time-quantum — a breaking UI/behavior change for
existing users and any doc/help page describing "Step: <n>"), or an
additional radio choice? None of AC-1 through AC-4 mentions the existing
buttons, the `stepAmount`/`stepEnd` fields, or `resources/help/**`
(`docs/*` step-button help topic), which would need updating either way.
*Recommendation:* Before filing this as buildable work, add a section
reconciling old vs. new stepping semantics, and decide whether "Step" is
being repurposed (test/help/UI churn) or a new button is added (two
overlapping controls for the same pedagogical purpose, a UX smell CAP-23's
own K9-style parsimony argument would flag elsewhere in the roadmap).

**2. AC-3's fixture — "the shipped hazard-demo circuit" — does not exist in
the repository.** `grep -rli hazard /home/user/JLS` (case-insensitive, repo
root, examples, test, resources, docs) turns up zero circuit files and zero
source mentions of a hazard/glitch demo; `find /home/user/JLS -iname
'*hazard*'` returns nothing. The word appears only in this issue's title,
in #504's Outcome Statement step 1 ("Open the shipped hazard-demo circuit"),
and in `docs/capability-roadmap/lf-03-causal-debug.md` / `AMENDMENT.md` as
prose about a "wavefront retreat" metaphor — never as a delivered artifact.
Both #504 and #533 refer to "the shipped hazard-demo circuit" in the present
tense, as if it already exists and only needs to be pointed a stepping
control at. It doesn't. This is either an undisclosed prerequisite (someone
has to design and check in a `.jls` static-hazard circuit plus its expected
golden event sequence before AC-3 is checkable) or a scope gap the two
issues silently assume the other one covers — and #504's `requires_features:
[]` / `blocked_by: []` machine block does not list any such fixture-creation
task. *Recommendation:* either file the hazard-demo-circuit fixture as an
explicit prerequisite (with its own golden), or rewrite AC-3 to build the
fixture in-scope and say so.

**3. Acceptance criteria are unusually under-specified for testability
compared to sibling issues, and AC-2 is not falsifiable as written.**
Contrast with the boundary-note comment on this very issue, which for the
sibling #535 cites a concrete test name (`RewindEqualsReplayTest`) and a
byte-identity gate. #533's four ACs name **no test class at all** — "a
stepping mode advances..." (AC-1), "each step animates... the current
simulated time is displayed" (AC-2), "reproduces the same final state...
pinned by existing goldens" (AC-3, but see finding 2 — the pin doesn't
exist), "byte-identical... with the feature present" (AC-4). AC-2 in
particular — "animates the affected wire(s) and highlights the
consuming/producing elements" — describes a visual/animation effect with no
measurable success condition; "animates" and "highlights" are not asserted
against anything. `ARCHITECTURE.md` "Test layout" is explicit that the
UI-verification harness's Layer 2 (Swing harness under Xvfb) and Layer 3
(render-to-image) are **"reserved"** — not yet built — and Layer 1 is
"headless model assertions" only. So today there is no test infrastructure
in this repo capable of checking that a wire visually animates or a gate
visually highlights; AC-2 as worded could "pass" by any developer's
subjective screenshot-eyeballing, or by a model-level flag being set that
no test reads. *Recommendation:* AC-2 needs to be rewritten as a headless,
Layer-1-checkable claim (e.g., "the stepping controller exposes the set of
wires/elements touched by the just-fired event/wavefront, and a test
asserts that set against a known circuit") or explicitly deferred to when
Layer 2/3 exist, per ARCHITECTURE.md's own phasing.

**4. AC-1's two granularities ("exactly one event" vs. "one wavefront...all
events at the current timestamp") are not shown to be compatible with the
single existing pause mechanism, and the difference is load-bearing for
correctness, not just UX.** `Simulator.runEventLoop` (lines 215-243) polls
one `SimEvent` per iteration and calls `beforeEvent()` before every poll;
`InteractiveSimulator.beforeEvent()` only knows how to pause at a `stepEnd`
*time*, not at an event count or "next distinct timestamp." Implementing
"exactly one event" stepping requires pausing mid-timestamp (after firing
event N but before N+1, even when both share `now`), which the current
`stepEnd`-based mechanism (compares `when > stepEnd`, line ~780) cannot do —
two same-time events would both fire in one step under the existing
comparison. So AC-1's "exactly one event" mode is a materially different
pause primitive from the "one wavefront" mode and from the existing `Step`
button, and the issue does not say which of the two is the default, whether
both are simultaneously required, or how a UI offers the choice. Given
finding 1, this compounds: there could end up being three different
step granularities (time-quantum "Step" the tool already has, "one event,"
"one wavefront") with no stated relationship.

**5. AC-4 is close to vacuous as insurance and mostly restates an existing
invariant rather than testing the new feature.** "Batch/headless behavior
and recorded outputs are byte-identical with the feature present" is
already guaranteed by `ARCHITECTURE.md`'s standing constraint that core
`jls.sim.Simulator`/`BatchSimulator` stay AWT/Swing-free
(`HeadlessCoreRatchetTest`, issue #77) and by the existing batch goldens
(`BatchSimulationGoldenTest`, `VcdExportGoldenTest`). As long as the new
stepping/animation logic is implemented entirely in
`jls.edit.InteractiveSimulator` (the only place it *can* live under the
recorded architecture), AC-4 passes automatically and proves nothing about
whether the feature itself works — it is a regression fence, not a feature
gate, and shouldn't be counted as one of only four criteria for the whole
feature. *Recommendation:* keep it as a ratchet (it is cheap and correct to
keep) but don't let it stand in for a real interactive-mode test.

**6. Scope-creep vector flagged but not closed: "interactive-engine
batching hygiene."** The Boundary/reference note says CAP-23 §3 risk 4
"may surface as a prerequisite during the demo slice; if so, it is filed
separately, not absorbed here." That's the right instinct, but as written
it is a blank check: nothing in #533 states what happens to *this* issue's
acceptance criteria if that prerequisite does surface mid-implementation
(paused? re-scoped? AC-3's determinism claim silently weakened?). CAP-23 §3
risk 4 itself says determinism "must hold under the interactive engine, not
just batch" — which is precisely AC-3's claim — so the risk that could
block AC-3 is not hypothetical, it's the exact thing AC-3 asserts.
*Recommendation:* AC-3 should say explicitly what happens to this issue if
the batching-hygiene prerequisite is discovered: block, or descope AC-3 to
"batch-equivalent" and file determinism separately.

## What's solid

- The determinism concern in AC-3 (same-time event ordering under stepping)
  is a real risk, correctly identified: `SimEvent.compareTo` uses a global
  `seq` tie-breaker, and `SimEventDedupTest`/`SimEventContractTest` exist to
  pin exactly this kind of ordering property — the right kind of test to
  extend for a stepping-specific case, once the fixture from finding 2
  exists.
- Keeping stepping interactive-only and out of the headless/batch surface
  (AC-4's intent) matches the repo's actual, enforced architecture boundary
  (`HeadlessCoreRatchetTest`, issue #77) rather than fighting it.
- `ordering_after: []` (no filed dependency) is accurate: nothing in
  `docs/capability-roadmap` or #504's roster makes PF-4 depend on PF-1
  (chronogram) or any other planned feature, and the CAP-23 mermaid graph
  agrees (`PF4 --> CAP23` only).
- The boundary-note comment against #535 is a genuinely useful piece of
  scoping work — it correctly separates "step forward with animation" from
  "jump to arbitrary earlier T by replay," two features that read as
  duplicates on a shallow pass but aren't.

## Verdict rationale

Two of the four acceptance criteria rest on things that don't currently
exist or aren't currently checkable (a hazard-demo fixture, and a
render-level animation assertion under a test harness the project's own
architecture doc marks "reserved"), and the issue is silent about a
same-named feature (`Step`/`Animate`) that already exists in the file it
would modify. None of this is fatal — the underlying idea (event-granular
stepping for pedagogy) is sound and clearly serves CAP-23 — but it should
not proceed to implementation without: (a) an explicit reconciliation with
the existing Step/Animate controls, (b) a decision on the hazard-demo
fixture, and (c) rewritten, test-anchored acceptance criteria for AC-1/AC-2
comparable in rigor to what the issue's own boundary-note comment demanded
of #535.
