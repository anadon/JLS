# Issue #654: TASK-C565-3: the synthesized netlist is placed and routed legibly through the #62 layouter lineage, and lands as an ordinary editable circuit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

The outcome is right and I do not dispute it: a synthesized circuit should arrive
*drawn*, and what lands should be an ordinary circuit with no synthesized-ness
about it. That is the difference between CAP-31 (#515) closing Digital's analysis
loop and CAP-31 shipping a table-to-blob converter.

But the issue names the wrong reusable asset. "Layout is reused, not rebuilt" is
the boundary note, and #62 is the named dependency — yet layout is the *smallest
and already-solved* part of the problem. `SchematicLayouter` is a 32-line
interface; `HeuristicLayeredLayouter` is on master and a two-level SoP netlist is
the easiest input a layered layouter will ever see (it is literally three layers,
acyclic, no feedback edges). Reusing that is a one-line construction site.

What actually stands between "netlist" and "drawn ordinary circuit" is two things,
and #654 owns neither and cites neither:

1. **Element geometry knowledge** — a `LayoutGraph.Node` needs a box size and a
   per-port pixel offset for every element instantiated. Today that table is
   hand-written per cell kind inside the importer
   (`/home/user/JLS/src/jls/hdl/imp/NetlistImporter.java`, roughly lines 543–640:
   `new LayoutGraph.Port("input0", INPUT, 0, GRID)`, `GRID * 4` wide gates, and so
   on).
2. **Realization** — turning a `LayoutResult` into `ELEMENT` blocks and WireEnd
   chains with one shared attached end per driven port. That is
   `Builder.emit` / `emitWires` / `portEnd`, same file, lines ~796–905 — a
   **private method on a private inner class returning a `String`**. Nothing
   outside `jls.hdl.imp` can call it.

So a worker taking #654 at its word will reimplement both, which is precisely the
duplication its own boundary note forbids — just in the half of the pipeline the
note does not cover.

And there is already a *third* implementation, out of tree:
`/home/user/JLS/riscv/jlsbuild.py` is a full Python circuit emitter with its own
port model and a marching-grid placer (`_pos()` walks x/y from 60). That is the
"generated netlist opens as an unusable overlap pile" #62 complains about, and it
exists because there was no callable Java realizer. Counting #654, JLS would then
have three netlist-to-drawn-circuit builders sharing one layouter.

## Where the work belongs

#62 already has the right task in its `planned_tasks`, unfiled:

> "Layout entry point for programmatically generated netlists (generator
> consumers: #202 CPU, #73 sample circuits) behind the same SchematicLayouter
> seam"

and its Open Question 2 recommends it live in #62. #654 is that task, arriving
under a different parent with a narrower name and a 1-mw band. **Redirect: file
#62's entry-point task now, build the shared realizer there, and reduce #654 to
its first consumer plus the synthesis-specific fixtures.** Concretely I would make
#654's body read "synthesis calls the #62 generated-netlist entry point; here are
the two-level fixtures and the golden bytes", and let AC-1/AC-2/AC-4 be inherited
rather than restated.

That redirect also buys #62 something it currently lacks: IC7 ("second
consumer/engine substitutes behind `SchematicLayouter` with no caller changes
beyond construction") is marked "Design intent only" with no witness. A synthesis
consumer *is* the witness. Name #654 as IC7's evidence and one open criterion on
the feature closes as a side effect of CAP-31 work.

## Concrete alternative designs

**A. The seam to cut is the realizer, not the layouter.** Promote the importer's
private builder to a public, consumer-neutral component — `DrawnCircuitBuilder`
(in `jls.hdl.layout`, or better a neutral `jls.gen` since synthesis is not HDL):

    add(elementType, attributes, ports) / connect(net, src, srcPort, dst, dstPort)
    build() -> save text (and/or a live Circuit)

with the `SchematicLayouter` call *inside* it. `NetlistImporter` becomes a caller
that loses ~150 lines; synthesis becomes a caller that never writes a WireEnd;
#202 and #73 become callers that stop inventing geometry. One realizer, four
consumers, one place where "indistinguishable from hand-drawn" is true or false.

**B. Derive port geometry from the elements themselves.** `Put` already exposes
`getXr()` / `getYr()` — element-relative port offsets — and `Element` exposes
`getAllPuts()` / `getPut(String)`. A ten-line adapter turns an initialized
`Element` into a `LayoutGraph.Node` with exact size and ports. That deletes the
importer's hardcoded table, prevents synthesis from growing a second one that
drifts from it, and makes #24's element-geometry baselines propagate to every
generator for free. Without this, AC-2's "no elements overlap" is proven against a
*paper* geometry model that may not match what the canvas draws — the assertion
passes and the picture is still wrong.

**C. Land it through paste, not through a new insertion path.** `SimpleEditor`
already pastes a whole `Circuit` into the current one (`paste(Circuit from)`,
~line 4976), with duplicate-name and orphan-wire-end checks, and
`jls.collab.op.AddElements` / `AddWire` exist for the op-layer route. If
synthesis's landing gesture *is* paste, then AC-3 ("no lock, no marker, no
behaviour difference on save/load/undo") stops being a thing to test and becomes
structurally impossible to violate — the code path is the same one hand-drawn
copy/paste uses, so a synthesized circuit cannot differ from a hand-drawn one.
That also delivers #62's IC8 ("one undoable, pre-selected unit") for both
consumers at once, instead of leaving it as an unfiled ergonomics task for import
and an unstated assumption here.

**D. Move determinism into the seam.** AC-4 (deterministic layout, reproducible
saved bytes) is a property of the layouter and realizer, not of synthesis. It is
mostly already true by construction — `LayoutGraph` keeps nodes in a
`LinkedHashMap` and edges in insertion order, `emit` walks `elems`/`edgeOrder`,
ids are assigned by iteration order — but nothing asserts it. Assert it once in
`test/jls/hdl/layout/` (same graph twice, identical result; and same graph built
in a different insertion order, documented outcome) and every consumer inherits
it. Restating it per consumer invites three separate near-miss tests.

## Two things to strike outright

- **AC-1's fallback clause.** "with a documented fallback placement if it is not
  yet landed" describes a world that no longer exists: `HeuristicLayeredLayouter`
  is on master and `NetlistImporter` line ~104 already calls it. A fallback placer
  written for a contingency that has already resolved is a second placement
  algorithm nobody will ever delete. Disregard it.
- **`ordering_after: [..., 62]`.** This orders a 1-mw task behind an entire open
  *feature* whose critical path is #290's rubric run over a ~150-element import
  fixture, human trace trials, and an ergonomics task — none of which affect
  whether a three-layer SoP graph can be laid out. Depend on the landed *seam*,
  not on the feature's close-out. As written, CAP-31's synthesis direction is
  blocked on HDL-import corpus fixtures, which is a real schedule coupling
  invented by a citation.

## What I am explicitly disregarding, and why

I am setting aside AC-1, AC-2 and AC-4 as this issue's acceptance criteria. Not
because they are wrong — they are the right properties — but because they are
properties of a shared component that does not exist yet, and asserting them
inside a consumer is what causes the component never to be built. Assert them
where they live (the realizer + layout invariants); leave #654 asserting the one
thing only it can: that a synthesized two-level circuit for a given table produces
the golden bytes, and that the drawn result reads left to right.

`LayoutInvariants.check` already covers AC-2 in full (on-grid, on-canvas, exact
port anchoring, orthogonal nonzero segments, non-overlapping bodies) and
`LayoutMetrics` covers legibility. AC-2 as phrased ("asserted mechanically rather
than by eye") will otherwise be honoured by someone writing a local overlap
checker, which is the same duplication one level down.

## Alignment verdict

The direction strengthens the project's arc: JLS's whole trajectory — batch mode
as a stability contract, HDL export/import, VCD, the RV32I generator, sample
circuits — keeps producing circuits by machine, and every one of them needs to
land as a circuit a student can read and edit. That is a *platform capability*,
not a CAP-31 detail. Filed as written, #654 spends 1 mw building a private third
copy of it. Filed as #62's generated-netlist entry point with #654 as first
consumer, the same effort retires the importer's private builder, gives #202 and
#73 a real placer, closes #62's IC7, and makes CAP-31's synthesis direction a
thin, honest task.
