# Issue #788: TASK-C570-2: a subcircuit instance can be opened mid-simulation and its internal signals watched live, then navigated back out, without stopping the run
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stated goal: close the gap Digital's #84 has held open for nine years. That is the
marketing frame, and #570's own comment already concedes the marketing belongs to
PF-6, not here. Strip it away and the actual want is: **a student should be able to
see where in the hierarchy a wrong value comes from, while the circuit is running.**
That is a debugging-locality problem, not a "dive" problem. JLS's existing answer is
the trace window — `findTraces` already recurses through `SubCircuit` (`InteractiveSimulator.java:989`)
and `SubCircuit.setWatched` already propagates into nested instances, so internal
signals of a nested instance are *already* observable live, named `alu.adder0.carry`
via `LogicElement.getFullName()` (`src/jls/elem/LogicElement.java:511`). What is
missing is not the information; it is the **spatial** presentation of it — seeing the
value on the schematic where the wire is, instead of on a waveform row.

Saying that out loud changes what should be built.

## What the codebase already gives you for free

Three of the five acceptance criteria are already satisfied by JLS's data model, and
the issue does not seem to know it:

- **AC-3 (per-instance, not per-definition) is structurally free.** JLS has no shared
  subcircuit definitions. Each `SubCircuit` element owns its own `Circuit` object:
  `copy()` deep-copies the whole inner circuit (`SubCircuit.java:332-384`), and the
  save format nests a full `CIRCUIT` record per instance (`SubCircuit.save`, and
  `Circuit.java:1015-1024` on load). Two instances are two disjoint element graphs
  with two sets of values. "A design with N instances shows N distinct live views" is
  not an achievement here; it is unavoidable. This is exactly the hard part of
  Digital's #84, and JLS does not have it.
- **AC-4/AC-5 (no kernel cost, byte-identical batch) are free under the right design
  and expensive under the wrong one.** The current live-display model is *poll on
  repaint* — the sim thread never notifies elements; `beforeEvent`/the epilogue just
  call `edRef.repaint()` on the EDT (`InteractiveSimulator.java:695, 798`). A dive
  that keeps polling adds literally nothing to the hot plane. A dive implemented as
  per-element observer callbacks in `react()`/`propagate()` would violate both AC-4
  and §6 of `docs/grand-architecture.md` ("the inner loop lives entirely inside core
  with zero indirection"). The ACs assert the outcome without naming the design that
  makes it true, which is the one thing worth writing down.
- **Half the navigation already exists.** `doModify` on a selected `SubCircuit`
  (`SimpleEditor.java:5158-5196`) already opens the instance's circuit in its own
  tab. And `enabled` is set back to `true` whenever the run is *paused*
  (`InteractiveSimulator.java:763`), so today a student can already pause, dive, and
  look — they just see a frozen picture, because the simulator only ever repaints
  `Editors.of(circ)` for the top circuit.

So the genuine delta is roughly: **fan the repaint out to every open view, and let
the dive gesture through while the run is not paused.** That is a fraction of the
2-3 mW band this task carries.

## Reframing 1 (design): a read-only view, not an Editor tab

The one thing that must not be done is the obvious one — reuse `doModify` mid-run.
That path opens a full `Editor`: an editing state machine, undo stacks, an `OpSink`
into the collab vocabulary, a checkpoint writer, and a `disableForSubcircuit` banner
telling the user to close the tab to resume editing (`SimpleEditor.java:720-729`).
Handing a student a live *mutation* surface onto a circuit whose elements the sim
thread is concurrently writing is precisely what `enableEditor(false)` exists to
prevent. It also breaks the simulator's own bookkeeping: the run epilogue re-enables
exactly one editor, `Editors.of(circ)`, so a second editor registered mid-run is left
in whatever state it happened to be in.

Cut a different seam. `CircuitRenderer` (`src/jls/edit/CircuitRenderer.java`) is
already a standalone, model-only paint path with draw culling, extracted under #77 so
`Circuit` could stop importing AWT. Build **`CircuitView`** — a `JComponent` that owns
a `Circuit` and a `CircuitRenderer`, paints, and does nothing else: no `State` enum,
no undo, no `OpSink`, no `Editors` registration, no checkpointing. Dive is then a
navigation stack inside one view panel with a breadcrumb (`top / alu / adder0`), and
"navigate back out" is a pop. Under that design:

- AC-2's "event schedule unperturbed" is true *by construction* — the view has no
  reference to the `Simulator` and no method that mutates anything.
- AC-5 is true by construction — `BatchSimulator` never constructs a view, and the
  `HeadlessCoreRatchetTest` boundary is untouched.
- The mid-run editing hazard disappears instead of being managed.
- You get an asset the project needs anyway: a paint-only circuit surface is what the
  tutorial (`jls/tutorial`), the help pages, image export, and a future collab
  "spectate / follow-me" mode all want. Today the only way to show a circuit on
  screen is to instantiate an editor.

I would state that as the acceptance criterion the issue is missing: **a test asserts
the dive view exposes no mutation entry point** (no `OpSink`, not registered in
`Editors`, no listeners installed). That is a ratchet in the same family as
`HeadlessCoreRatchetTest` and `NotificationRatchetTest`, and it is the criterion that
actually protects AC-2 and AC-5 rather than sampling for them.

## Reframing 2 (architecture): the real primitive is the scope path

The bigger claim, and where I would spend the ambition this task has budget for.

JLS has hierarchical instance addressing today, but only as an accident:
`getFullName()` walks the `Circuit.subElement` back-pointer to build a dotted string.
Every consumer then flattens it. VCD export is the tell — `BatchSimulator.java:424-436`
emits a single `$scope module <top>`, dumps every signal into it, and closes with one
`$upscope`, with the hierarchy surviving only as dots inside `$var` names. That is a
lossy encoding of the exact structure the VCD standard has `$scope`/`$upscope` for,
and `docs/vcd-interop.md` sells GTKWave/Surfer interop as a headline batch feature —
where a real scope tree is the difference between a browsable design and a flat list
of a thousand rows.

So: promote the path to a first-class `ScopePath` in the core — an ordered list of
`SubCircuit` stable ids from the root, with a canonical text form. Then this task's
GUI dive is one consumer of it, and three other subsystems collect the payoff:

1. **VCD export** emits nested `$scope module <instance>` blocks, which is both more
   correct and strictly cheaper to consume downstream.
2. **Probes and the `-t` batch grammar** get an unambiguous way to name an internal
   signal of a specific instance.
3. **HDL export** (`jls.hdl`) needs instance paths for the same reason.
4. **Collab** (#163 stack) gets a natural payload for "where I am looking".

There is also a durability argument. The back-pointer walk is only sound *because*
JLS has no shared definitions. If the element registry (#78) or Yosys netlist import
(`jls.hdl.imp`) ever pushes JLS toward shared definitions — and a teaching tool where
editing one adder does not fix its six copies is a real pedagogical liability — then
`getFullName()`, `setImported`, and any dive built on `Editors.of(subcircuit)` all
break at once. A dive addressed by explicit path from the root survives that change;
a dive addressed by "the Circuit object I found hanging off this element" does not.
This costs almost nothing to do now and is very hard to retrofit.

## Where this sits in the project's arc

It pulls with the arc, on two counts. It is a natural continuation of #77's
model/view inversion (`Editors`, `CircuitRenderer`, `ElementValueDisplays` are all
that same move), and it is genuine unowned scope — #570's dedup comment confirms no
other issue claims it. It also serves the `riscv/` trajectory (§2 of
`docs/grand-architecture.md`) more than it serves the Digital comparison: an RV32I
datapath is exactly the design where a student cannot tell which instance of a
nested block is producing garbage, and where a flat waveform list is unusable.

The one place it pulls against the arc is the framing itself. TASK-C570-1 exists to
force a JLS-merit justification precisely because this feature entered the backlog as
competitor parity. The strongest such justification is not "Digital's #84 is nine
years old"; it is "hierarchical instance addressing is missing from a simulator that
exports VCD, exports HDL, and is being pointed at CPUs." Write that one.

## Disregarding an acceptance criterion

I am explicitly setting aside **AC-3 as a unit of work**. Its two-instance fixture is
worth keeping as a cheap regression pin, but budgeting for it as though per-instance
state must be *achieved* misreads the codebase and will produce effort spent proving
something the deep-copy model already guarantees. Spend that budget on the
`ScopePath` primitive and on the no-mutation-surface ratchet instead.

## Summary

Endorse the outcome; reframe the route. Do not open an `Editor` mid-run — build a
paint-only `CircuitView` over the existing `CircuitRenderer`, fan the simulator's
repaint out over open views, and let the dive gesture through while running. Then
spend the remaining ambition on making the instance path a real core type rather than
a string assembled from a parent pointer, so this lands as a capability four
subsystems share rather than one GUI affordance answering a competitor's bug tracker.
