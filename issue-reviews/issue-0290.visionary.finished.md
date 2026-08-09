# Issue #290: HDL layout goldens: commit the three hand-drawn showcase references (ALU slice, counter, small FSM) and run the corpus metrics rubric against the heuristic layouter
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of ceremony, #290 exists to answer one question: *does the layouter we
already shipped produce schematics a student can read, fast enough, at the size
circuits actually come in?* That question deserves an answer, and the seam-first
design that makes it answerable (`SchematicLayouter` / `LayoutInvariants` /
`LayoutMetrics` landed before any engine) is the best-executed architectural
decision in the HDL tree. Nothing below argues against getting the verdict.

But the issue bundles two different goals under one task and lets the harder one
set the cost: (a) an **absolute launch gate** — does the engine clear fixed
thresholds — and (b) a **CI regression signal** — did today's change make layouts
worse. Only (a) needs hand-drawn references and a bespoke fixture; (b) is cheaper
and more useful, and the issue's own stated impact ("a CI-regressable pass/fail
signal instead of eyeballing screenshots") is mostly (b).

## 1. The headline prediction is answerable now, for free, and I think it is false

`HeuristicLayeredLayouter` routes with **one dedicated vertical lane per net** in
the gap before each target layer (`src/jls/hdl/layout/HeuristicLayeredLayouter.java`,
`assignLanes`/`laneCounts`/`columnPositions`; javadoc: "Every net gets its own
vertical lane ... so different nets never share a channel column"). Channel width
therefore grows with net count, not with circuit depth, and every long-span edge
becomes a dogleg out to its private column and back. Two rubric metrics are
directly exposed to that: `wireLengthRatio` (threshold 2.0×) and `boundingBoxArea`
(the 4× compactness check). The class's own javadoc concedes it: "good at
adjacent-layer classroom scale and degrading gracefully (extra wire, some
crossings) on long multi-layer spans."

H1 says the landed engine passes everything *including* a ~150-element import.
I would bet against that at multi-layer scale, and the evidence is in the source,
not in a fixture. The issue's plan spends its most expensive work — sourcing and
adjudicating three hand-drawn MTU circuits, building a Yosys datapath fixture — to
discover an outcome a two-hour experiment can surface first.

**Reframing, ordering by information gain:** before any golden or fixture work,
write a throwaway scaling probe that synthesizes `LayoutGraph`s programmatically
(the unit tests already build nodes/ports by hand — `test/jls/hdl/layout/HeuristicLayeredLayouterTest.java`
`buffer()`/`gate()` are the seed), for N = 10, 50, 150, 500 in a few shapes
(ripple chain, fan-in tree, register-feedback pipeline), and print metrics plus
time versus N. That probe needs no Yosys, no MTU sourcing, no importer, and no
adjudication. If the ratios blow up at N≈150 — as I expect — #62's escalation
fires weeks earlier and the golden work is never spent on a refuted engine.

## 2. The compactness golden, as specified, measures the wrong thing

IC5 compares the bbox of *an imported Yosys netlist* against the bbox of *a
hand-drawn JLS circuit of nominally the same function*. Those are different
element populations: #61's conversion inserts Splitter/Binder meshes and
Constants that no human draws, and Yosys bit-blasting decides granularity. A 4×
ratio between two different graphs is dominated by conversion granularity, not by
the layouter's compactness. The number would be recorded, defended, and
uninformative.

**Alternative that makes the problem disappear — the golden as a differential
oracle on the same graph:** take a hand-drawn `.jls`, strip its geometry, rebuild
the identical `LayoutGraph` from its elements/puts/nets, re-lay it out, and compare
the layouter's bbox (and crossings, and wire length) against *the human's own
geometry for that exact circuit*. Same nodes, same ports, same nets — the ratio now
means precisely what IC5 wants it to mean. It also:

- deletes the MTU sourcing question, the provenance/licensing question (a project
  that refuses to link EPL-2.0 ELK in-process should not casually vendor
  textbook-derived circuit files for one scalar), and Open Question 1 entirely;
- turns **every** `.jls` anyone has — repo fixtures, instructor circuits, future
  student submissions — into a layout benchmark, instead of three frozen files;
- requires one new piece: a `Circuit` → `LayoutGraph` extractor. That piece is not
  overhead. It is #62's own planned "layout entry point for programmatically
  generated netlists," and it is the natural spine of the obvious future editor
  command ("Tidy this circuit"), which is the feature students would actually ask
  for. Building it here cuts along a better seam than committing three files does.

I am explicitly disregarding the "commit three hand-drawn showcase goldens" DoD
item as written: it buys one reference-relative scalar at the price of a permanent
third-party fixture dependency, and the same scalar is available better without it.

## 3. A better compactness metric needs no reference at all

`LayoutMetrics` already computes `boundingBoxArea` but leaves the threshold to the
caller "because it is reference-relative." It does not have to be. Add a
self-relative density: bbox area over (sum of node areas + a routing allowance
derived from the existing `manhattanLowerBound`). That is scale-free, computable on
every circuit in the corpus with no golden, comparable across circuits, and
CI-regressable. The human references then shrink to what they are genuinely good
for — a **one-time calibration study** ("what density do humans achieve?") recorded
on #62 to set the constant — rather than a permanent test-tree dependency.

## 4. The target-scale fixture already exists in this repository

Observation 5 is right that the import corpus tops out at 3 cells. But the repo
already carries `test/fixtures/riscv-sum1to10.jls`: 1038 `ELEMENT` records,
~228 non-`WireEnd` elements — past #62's ~150 bound — generated by
`riscv/jlsbuild.py`, whose placer is a raster sweep explicitly commented "a
spread-out grid position, purely cosmetic" (`_pos`, x += 120, wrap at 3000) with
every WireEnd emitted at (0,0). That file *is* the "unusable overlap pile" #62's
Impact section names, and #202 is the consumer #62 says it serves.

Building a fresh ~150-element Yosys datapath to prove target scale, while a
larger, real, in-repo, directly-motivating circuit sits unlaid-out, is toil aimed
at the less interesting target. With the §2 extractor in hand, the RV32I CPU
becomes the IC10 case for free — and a passing run there is a far stronger claim
than any synthesized adder slice. (Keep one Yosys-path circuit in the run so the
importer seam stays covered; it does not have to be the large one.)

## 5. Two gates that will not survive contact with how this project runs

- **IC6, the human trace trial** ("three named signals, under a minute each,"
  screenshots in the PR). This repo is worked by LLM agents against machine-checked
  DoDs; an unauditable human ritual on the per-PR path either gets rubber-stamped or
  blocks. #62's rule — "if a circuit passes the numbers but fails IC6, the rubric
  gains a metric, not an exception" — is exactly right and should stay; run the trial
  **once**, as a validity study of the rubric, recorded on #62. Do not make it a
  standing per-task checkbox.
- **The 1 s wall-clock assertion.** A time threshold on shared CI is a flake
  generator, and the interesting quantity is the *shape* of time versus N, not one
  point under one bound. Record the curve from the §1 probe; if an assertion is
  wanted, make it generous, best-of-3, and framed as a blowup detector.

Related, and worth pinning while someone is in this code: `LayoutMetrics.measure`
is O(S²) in wire segments (the nested crossing loop and `countOverlaps`, plus an
S×N body-intersection pass). At classroom scale that is free; at the RV32I scale
of §4 — especially with per-net lanes multiplying segments — **the measurement, not
the layout, becomes the bottleneck**. If metrics are ever to gate #202-scale work,
that wants a sweep line. Cheap to note now, expensive to discover as a "timeout" later.

## 6. Where this pulls against the project's arc

The rubric's absolute thresholds (`MAX_CROSSINGS_PER_NET = 0.5`,
`MAX_CROSSINGS_ON_ONE_NET = 4`) are written scale-free but are only defensible at
one scale — which is why IC2 had to grow the parenthetical "bounds apply at target
scale, ≤ ~150 elements." That qualifier is the rubric telling you it wants to be
scale-relative. Freezing a 150-element ceiling into the test infrastructure right
as the repo's own trajectory (`riscv/`, #202, and #221's recorded revisit trigger
about CPU-scale designs) points at thousands is the one place this task pulls
against the larger arc. Express the thresholds per-scale, or at least record
metrics at two scales, so the day the ceiling moves the tests bend instead of break.

One more, for #62 rather than #290: its escalation protocol says "do not grow the
layouter — file the ELK runner." That rule was written to stop aesthetic tinkering,
and it is a good rule for tinkering. Per-net lanes is not tinkering — it is a
missing channel-routing model, a structural choice, and shared lanes with net
merging is a well-understood, bounded fix that is far cheaper than standing up an
out-of-process ELK subprocess. If #290 refutes on wire length or area, the REPLAN
should distinguish "tune constants" (forbidden, correctly) from "the router has no
channel model" (a legitimate third option the protocol currently forecloses).

## Recommended shape, in order

1. Scaling probe on synthetic `LayoutGraph`s; record metrics + time versus N on #62.
   Decide the engine's fate here, before fixture spend.
2. `Circuit` → `LayoutGraph` extractor (also #62's planned generated-netlist entry
   point). Re-layout `test/fixtures/riscv-sum1to10.jls` — IC10, no new fixture.
3. Corpus metrics test over the existing #61 fixtures, thresholds asserted, plus the
   tightening sanity check the DoD already asks for. This part of #290 is right as written.
4. Self-relative density metric replacing the reference-relative compactness check;
   human references become a one-time calibration on #62, not committed goldens.
5. IC6 once, as a rubric-validity study; timing recorded as a curve.

What would change my mind: if the §1 probe shows the heuristic clearing every
threshold at N=500, then the engine is stronger than its own javadoc claims, the
scale worry evaporates, and #290 as written becomes a reasonable — if still
golden-heavy — way to bank the verdict.
