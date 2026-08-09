# Issue #788: TASK-C570-2: a subcircuit instance can be opened mid-simulation and its internal signals watched live, then navigated back out, without stopping the run
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The outcome is coherent and the per-instance state claim (AC-3) is actually
already true of the data model for free — but the issue is silent about the
one mechanism in the tree that most directly determines feasibility: opening
a subcircuit in its own tab (`SimpleEditor.doModify`, issue #86) and running
a simulation (`InteractiveSimulator`) both gate through the *same* all-or-
nothing `enabled` flag, and today a running simulation turns that flag off,
which makes the very interaction this task wants (double-click a subcircuit
to dive in) unreachable. AC-2's "identical event sequence" test also assumes
instrumentation that does not exist yet. Neither is mentioned as scope or
cost, which for a band_mw 2-3 task materially understates the work.

## Findings, most severe first

**1. [HIGH] The dive interaction is currently wired through the same `enabled` gate that a running simulation turns off — the issue doesn't acknowledge this, so it isn't clear whether "dive" reuses or replaces that path.**
`src/jls/edit/SimpleEditor.java:120-126` documents `enabled` as "False while
a subcircuit is being edited. Volatile: written from the sim thread
(`enableEditor` around a run)" — one boolean serves two unrelated purposes
already. `InteractiveSimulator.java:638,770` calls `ed.enableEditor(false)`
at run start (`// turn off listeners`) and `:670,764` re-enables it at
stop/pause. Meanwhile the double-click-to-open-subcircuit-tab action lives in
`doModify` (`SimpleEditor.java:5153-5195`), and essentially every mouse/key
handler that could reach it is guarded by `if (!enabled) return;` (26 call
sites, e.g. lines 1383, 1660-1966, 2255-2726, 3038-3739, 5361). So as the code
stands today, starting a run disables the exact gesture (double-click a
`SubCircuit` element) that AC-1 needs to still work mid-run. The issue never
says whether "dive" is a new, narrower-scoped affordance carved out of that
gate (view-only navigation while structural editing stays blocked) or a
special case of the existing edit-tab flow — and `disableForSubcircuit`
(`SimpleEditor.java:720-729`) currently disables the *parent* editor whenever
a subcircuit tab is open, which is the opposite of what "navigate back up...
without the simulation stopping" implies should happen to the parent's live
view. This is a real architectural fork the issue should have picked before
scoring band_mw 2-3, not left to the implementer to discover.
Recommendation: state explicitly whether "dive" is a new read-only view
distinct from the existing (edit-capable) subcircuit-tab mechanism, and if it
reuses tabs, specify how `enabled`/`disableForSubcircuit` get split into
"structural edits blocked" vs. "navigation/observation allowed."

**2. [HIGH] AC-2's "event sequence is identical" test has no instrumentation to build on, and as worded it's gameable.**
`Simulator.java:25` — `protected PriorityQueue<SimEvent> eventQueue` — is
consumed destructively as the sim runs; nothing in `jls.sim` currently
records a log of processed events for later comparison (no `getEventLog`,
`eventSequence`, or similar found anywhere in `src/jls/sim`). Building that
capture is new scope the issue doesn't mention. Worse, "the event sequence
is identical" is unspecified: identical event *count*, identical
`(time, target, payload)* ordering, or identical wall-clock/thread
interleaving? A dive that (correctly) does nothing to the event queue could
still pass a loose version of this check (e.g. comparing final signal states
only) while a subtly wrong implementation that reorders same-timestamp
events non-deterministically could also pass it, since `SimEvent`'s
`compareTo` and tie-breaking rules aren't cited as the oracle.
Recommendation: name the exact comparator/log format the test asserts on
(tie-break rule included) and confirm whether new `Simulator` API surface is
being added — if so, that's an ARCHITECTURE.md-relevant boundary change to
the class the docs call "headless by construction" and gate with
`HeadlessCoreRatchetTest".

**3. [MEDIUM] AC-4's "no measurable cost" and "matches baseline" have no tolerance, no benchmark harness, and no existing precedent in the repo.**
A search of `test/` for throughput/benchmark-style tests (`*Throughput*`,
`*Benchmark*`) returns nothing. "Matches baseline" without a defined metric,
sample size, or acceptable variance is unfalsifiable as written — any run-to
-run noise can be waved away as "still matches" or weaponized as "regression"
depending on who's arguing. This also quietly asks for new perf-test
infrastructure (a JMH-style or wall-clock harness) that isn't listed as part
of the task's cost.
Recommendation: pin a concrete method (e.g. batch-mode wall time on a named
golden circuit, N runs, median with a stated % tolerance) before this is
implemented, not after.

**4. [MEDIUM] The EDT-invocation discipline the live view needs isn't mentioned, and it directly interacts with AC-4.**
ARCHITECTURE.md's threading section is explicit: "UI work initiated from the
sim thread is routed through `SwingUtilities.invokeLater`... the clock
display is additionally rate-limited there" (a lesson from issue #49,
finding H8/M15). A dived view that pushes every internal element/wire update
to Swing live, per event, is exactly the kind of sim-thread-to-EDT traffic
that previously required rate-limiting to avoid perf and correctness
problems. The issue's AC-4 promises zero cost *when unused*, which is the
easy case; it says nothing about what bounds the cost *when the dive is
open*, which is the case this feature exists for and where the EDT-flooding
risk actually lives. Recommendation: state an update-rate policy for the
dived view (e.g. same rate-limiting pattern as the clock) as part of this
task's design, not left implicit.

**5. [MEDIUM] AC-5 ("batch and headless... byte-identical") is true by vacuous construction and adds no verification value as stated.**
Batch/headless mode has no GUI and therefore no subcircuit dive affordance
at all — `Simulator`/`BatchSimulator` are documented as importing "no AWT,
Swing, or `jls.edit`" (ARCHITECTURE.md, module layout). A feature that is
by definition unreachable in batch mode cannot make batch output anything
other than byte-identical, with or without correct implementation. As
written this criterion can't fail, which means it can't be relied on to
catch a regression either — it reads as a checkbox copied from a template
rather than a criterion tailored to this task. Recommendation: either drop
it, or replace it with something that could actually fail, e.g. "the shared
`Simulator`/element `react`/`initSim` code paths touched by this task are
unchanged for batch consumers, asserted by running the existing
`BatchSimulationGoldenTest`/`VcdExportGoldenTest` suite unmodified."

**6. [LOW] "Subcircuit instance" implies a definition/instance relationship JLS's model doesn't actually have — worth flagging so UI copy and tests don't imply the wrong mental model.**
`SubCircuit.copy()` (`src/jls/elem/SubCircuit.java:331-384`) deep-copies the
imported circuit element-by-element into a fresh `Circuit` per placement, and
`SimpleEditor.finishImport` (`SimpleEditor.java:679-697`) does the same on
initial import — there is no shared "definition" object multiple
`SubCircuit` elements point at; each one owns an independent structural copy.
That's actually good news for AC-3 (N placements already carry N independent
sets of `Output`/register/wire state, so "distinct live views" falls out of
the existing model almost for free — this is the one place the task is
*cheaper* than its framing suggests). But "instance," used four times in the
outcome/AC text, invites the reader to assume edits to one instance's
definition propagate to siblings (the conventional schematic-tool meaning),
which is false here. Recommendation: no code risk, but scope the fixture and
any UI label ("instance 1 of Foo" vs. "a copy of Foo") to match the actual
copy-not-reference semantics so testers don't write an assertion for
propagation behavior JLS was never going to have.

**7. [LOW] Ordering dependency on #787 is sound but currently blocking: the D10 justification #788 is ordered after is itself still an open, unstarted task.**
`ordering_after: [TASK-C570-1]` in #788's frontmatter correctly points at
#787, and #787 (fetched: state `open`, 0 comments) is the "write the
justification before implementation" gate for exactly this sub-feature. That
sequencing is the right shape and not a defect — flagging only so a reviewer
doesn't start #788 work before #787's justification actually exists; the
issue itself gives no signal of #787's current (unstarted) status.

## What's solid

- The parent-context citation (#570 AC-2, #787's D10 gate, Digital's #84) is
  internally consistent — #788 is a faithful narrowing of #570's AC-2 into
  one task, and its ordering_after correctly names the prerequisite task
  rather than skipping it.
- AC-1's "internal element and wire states updating live" is scoped to
  observation, not editing, which — if made explicit per finding #1 — is the
  right cut given the sim-thread/EDT and event-queue mutation hazards a
  concurrent-editing scope would raise.
- band_mw and labels (`area:gui`, `area:sim`, `tier:task`) are directionally
  correct: this genuinely touches both areas, unlike a pure-GUI issue that
  mislabels itself as touching `area:sim`.
