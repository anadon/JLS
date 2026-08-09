# Issue #62: HDL Stage 2 companion: schematic auto-layout for imported netlists (heuristic layered layout; ELK only out-of-process)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the ten integration criteria, the two rosters, the absorbed #342 scope and the
ELK licensing archaeology, and one sentence remains: **a circuit JLS did not draw by
hand should still be readable by a student.** That is a genuine and well-aimed goal for
this project — JLS's whole reason to exist is that a human looks at a picture of a
circuit and understands it. Everything else in the issue is machinery around that.

The machinery is where the misalignment is. The issue frames readability as *a quality
property of the HDL import pipeline*, owned by `jls.hdl`, proven by a batch metric
rubric over netlist fixtures. I think that is the wrong seam, and that most of the
issue's residual weight — IC7, IC8, the "generated-netlist entry point", the ELK
escalation, half of #388 — is downstream of the framing rather than of the problem.

## What is actually true on master

- `src/jls/hdl/layout/` is eight files, 1794 lines. `HeuristicLayeredLayouter` (553
  lines) ships and `NetlistImporter.java:104` calls it unconditionally. This is not
  greenfield; the issue says so and is right to shout it.
- The layout package imports **nothing from JLS except `jls.core.Geometry`** (verified:
  `grep -n "import " src/jls/hdl/layout/*.java` yields JDK collections plus that one
  line). `LayoutGraph.Node` is "an opaque rectangle with ports". The solver is already
  domain-general — it merely lives inside the HDL import subtree.
- The importer never builds a live `Circuit`: `builder.emit(layout)` returns save-format
  **text** (`ImportResult`). So layout is currently a text-emission concern.
- `jls.elem.Element` already has `setXY(int,int)` and `move(int,int)`; `SimpleEditor`
  already snapshots before every mutating gesture; `CircuitSnapshot` is save-text.
- The other named consumers are not Java. `riscv/jlsbuild.py` emits `.jls` text from
  Python; `riscv/` has no `SubCircuit` usage at all — the generated CPU is flat.

## Reframing 1 (the main one): layout is an editor verb, not an import stage

Make the primary user-facing form of this capability a command in the editor —
**Edit ▸ Auto-arrange selection** — that runs the existing `SchematicLayouter` over the
selected elements (or the whole circuit) and writes the placements back through
`Element.setXY`.

Consider what that collapses:

- **IC8 disappears.** "Import arrives as one undoable, pre-selected unit" stops being a
  criterion needing a GUI import entry point and an undo harness. Arrange-selection runs
  inside the ordinary gesture/snapshot path, so undo is exactly save/load semantics,
  already pinned by existing round-trip tests (ARCHITECTURE.md, "The save/load
  pipeline"). One planned task deleted, not built.
- **IC7 disappears as *design intent* and becomes a fact.** "A second consumer
  substitutes behind `SchematicLayouter`" is proven by the editor adapter existing at
  all — a `Circuit`-subset → `LayoutGraph` builder — rather than by a hypothetical ELK
  runner or a hypothetical generator hook.
- **The "generated-netlist layout entry point" planned task disappears.** A generated
  circuit is just a circuit you opened. Select all, arrange.
- **It serves users the issue never counted.** Every JLS student who has ever dragged a
  circuit into a tangle gets the button. The capability stops being HDL-shaped and
  becomes an editor capability that HDL import happens to use — which matches how the
  code is already written.

The adapter is small precisely because `LayoutGraph` knows nothing about `jls.elem`: a
`Node` per element from its bounds, a `Port` per `Put` from its offset, an `Edge` per
`WireEnd` pair. The one honest cost is deciding what to do with wires the layouter
re-routes versus wires the user drew; a first cut can re-route only wires wholly inside
the selection.

## Reframing 2: the second entry point is a CLI flag, not a Java API

Open Question 2 asks where the generated-netlist entry point lives — here or in #202.
Neither. `riscv/jlsbuild.py` is Python; it cannot call `SchematicLayouter` and never
will. The seam that actually serves it is one more flag next to the existing
`-savetext` in `JLSStart.FLAGS` (`src/jls/JLSStart.java:759`):

```
jls -layout out.jls in.jls
```

Load, arrange, save — headless, in the existing container image, usable by the RV32I
generator (#202), sample circuits (#73), truth-table synthesis (#654), and by anyone
scripting JLS. It reuses the same adapter as Reframing 1, costs a flag row and a case in
the dispatch, and is a *documented CLI contract* — a stability surface this project
already knows how to maintain (`docs/batch-interface.md`, `CliFlagTableTest`).

## Reframing 3: the golden should be the picture, not five scalars

`LayoutMetrics` computes five numbers that are proxies for "readable". The issue is
admirably honest that they are proxies (IC6's trace trial; "the rubric gains a metric,
not an exception"). But JLS **already renders circuits headlessly to PNG and SVG**
(`jls -i out.svg`, README "Command-line options"; `jls/edit/CircuitRenderer`). The
strongest available regression artifact for a layout engine is therefore the rendered
schematic itself, committed as a golden and reviewed as an image diff in the PR.

Layout is already required to be deterministic (an invariant #342 migrated in). So:

- commit `test/resources/hdl/layout-goldens/*.svg` rendered from the imported fixtures;
- a test re-renders and byte-compares — any layouter change becomes a *visible* diff a
  human judges in seconds, which is the actual acceptance question;
- keep `LayoutMetrics` as a **reported** ratchet (Open Question 2 of the migrated set
  already recommends "hard invariants required, rubric reported"), not as the gate.

This collapses IC5 (bbox ratio versus hand-drawn), most of IC6 (trace trial: look at the
committed picture), and removes the awkward dependency on sourcing three authentic MTU
hand-drawn circuits before anything can be measured. #290 shrinks to: render the corpus,
commit the images, add the timing assertion.

## Reframing 4: scale is a hierarchy problem, and the core-scale criterion is wrong

IC10 (~150 elements) and the migrated IC-3 ("the rubric at core scale, a published
core") both assume the target is a readable *flat* page. No layered layouter produces a
readable flat RV32I, and none should be asked to. ARCHITECTURE.md's #221 decision
already records that classroom scale is this project's workload. JLS's answer to size is
`SubCircuit` (654 lines, shipping) — and the generated CPU currently uses none of it.

So the leverage for #202 is not a better layouter; it is **`jlsbuild.py` emitting nested
subcircuits** (regfile, ALU, decoder, control), each of which is 20–40 elements and
laid out trivially by the engine that already ships. The same is true of hierarchy
import (#449/#61): once a hierarchical netlist becomes nested subcircuits, every
placement problem is back at fixture scale by construction.

I would keep IC10 as a **robustness** criterion (the layouter must not blow up or take a
minute on 150 elements) and **drop IC-3 as a quality criterion** — measuring readability
thresholds on a flat published core measures the wrong artifact and, per the issue's own
D10 protocol, would generate recorded threshold-miss decisions about a picture no one
should ever open.

## Reframing 5: retire ELK, and change what "escalation" means

The ELK thread has consumed a review, two adjudications, a reversal, a supersession, and
a contradiction still sitting in `package-info.java:17-25` ("A hand-rolled Sugiyama
layouter is explicitly out of scope") against the 553-line hand-rolled Sugiyama layouter
in the same package. The heuristic shipped and works. Meanwhile an out-of-process ELK
runner means bundling an EPL-2.0 jar and spawning a second JVM inside a product whose
entire distribution story is "one self-contained jar a student double-clicks" (README,
"Running JLS from the jar"). That pulls against the project's arc.

The out-of-the-box point: **in an interactive editor, the escalation path for "not
readable enough" is the human, not a bigger batch algorithm.** If the heuristic places
something awkwardly, the student drags it — and if Auto-arrange is a selection-scoped
editor verb (Reframing 1), they can re-arrange just the part that is wrong. Interactive,
incremental, partial re-layout is strictly more valuable to a teaching tool than an
extra 5% on a crossings metric, and it is unreachable from the current
import-pipeline-only framing.

Concretely: delete the contingent ELK task, keep `SchematicLayouter` engine-neutral
(cheap, already true), and fix the `package-info.java` contradiction in the same commit
as anything else touching the package.

## One structural nit with real consequences

`jls.hdl.layout` depends only on `jls.core.Geometry` and is described in its own javadoc
as engine-neutral and semantics-free. It belongs at `jls.layout` (or `jls.core.layout`),
beside the geometry types it uses. Leaving a domain-general geometry solver inside the
HDL import subtree is exactly what makes "who owns the generated-netlist entry point?"
look like a hard question. Move it and the question evaporates.

## What I would keep unchanged

The seam-first cut was correct and has already paid for itself through one full engine
reversal. `LayoutInvariants` as a hard, reject-wholesale contract is right. Determinism
as an invariant is right and is what makes Reframing 3 possible. IC9 (timing) and the
vocabulary-totality criterion (IC-1, enumeration derived from the mapper's dispatch
rather than hand-listed) are both good criteria I would keep verbatim.

## Acceptance criteria I am explicitly disregarding, and why

- **IC5, IC6** — subsumed by rendered-SVG goldens, which test the real question directly.
- **IC7, IC8** — not criteria at all once layout is an editor verb; both become true by
  construction rather than by separate planned tasks.
- **IC-3 (core-scale rubric)** — measures readability of an artifact that should never
  exist flat. Replace with "the generator emits subcircuits".
- **The contingent ELK runner** — retire it; the escalation path is interactive
  re-layout.

## Alignment verdict

The goal strengthens the project's arc; the framing weakens it by making a general
editor capability into an HDL-pipeline detail, and by carrying a subprocess dependency
that fights the single-jar deployment model. Endorse the work, cut it along the editor
seam instead of the importer seam, and let the picture — not five scalars — be the
oracle.

A closing observation offered in the same spirit: this issue now carries two conflicting
IC numbering schemes (IC1–IC10 and IC-1–IC-6), two task rosters, an absorbed feature, a
"union of #290 and #388" boundary note, and thirteen comments of ledger. It has become
unreadable in precisely the way the schematics it exists to fix are unreadable. The
right move at pickup is the same one recommended above for circuits: re-lay it out —
one body, one criteria list, one roster — before adding anything else to it.
