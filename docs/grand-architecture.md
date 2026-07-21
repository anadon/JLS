# The grand architecture JLS should have

A forward-looking companion to [`ARCHITECTURE.md`](../ARCHITECTURE.md).
Where `ARCHITECTURE.md` maps HEAD — where code lives *today* — this
document answers a different question: **given how JLS is actually used,
what it could become, and every currently-open issue, what is the single
most correct target architecture to converge on?** It is a determination,
not a work order: it names the shape, the keystone, the layer boundaries,
the sequence, and — just as importantly — what is deliberately excluded.

The central finding up front: **the open issues are not a backlog, they
are a latent architecture.** All 46 open issues map cleanly onto six
layers with one enabling triad at the bottom. That they map so cleanly is
the strongest evidence that the target below is the right one — it was
already being built toward, one issue at a time, without a picture of the
whole. This document draws the picture.

## 1. What JLS is (the load-bearing facts)

Any target has to hold these fixed, because they are the actual product:

- **A single self-contained jar** is the deployment model students and
  labs rely on. Help works offline and version-locked to the binary
  (recorded decision, `ARCHITECTURE.md`); installers bundle a runtime so
  no JDK is required. The architecture may not assume a network, a
  server, or an install step.
- **Two co-equal front ends, not one.** The GUI editor (Swing) *and* the
  headless batch/grading surface (`-b -t`, VCD, image/Verilog export,
  the `ghcr.io/anadon/jls` container) are both first-class. Autograders
  and CI consume the headless surface; the batch interface is already a
  documented stability contract (`docs/batch-interface.md`).
- **~69k lines of Swing-dominated Java**, single-maintainer, audience of
  first-year students and their instructors. This was assessed
  explicitly against a Kotlin/Scala rewrite and the answer was **no**
  (#96): the correct move is Java-25-baseline + Kotlin's transferable
  practices as *compiler-enforced* properties, incrementally.
- **A schematic-first model with typed pins, bit-widths, wire nets,
  subcircuits, and a discrete-event simulator.** That element graph is
  the asset everything else walks — the exporter's input, the registry's
  subject, the replication unit, the autograder's oracle.

## 2. What JLS could be (the latent product)

The open issues, read together, describe a tool considerably larger than
"draw gates and watch them blink." Three trajectories are already funded
by real issues and real prototypes:

- **A serious datapath / CPU teaching tool.** The `riscv/` tree already
  builds a working RV32I CPU strictly through the model, with a
  reference simulator and differential fuzzing; #201 (multi-port register
  file, sign/zero-extend), #199 (synchronous memory), #202 (RV32I worked
  example as an integration golden) push JLS from gate toys to full
  single-cycle/pipelined processors.
- **An FPGA-deployment bridge.** #59/#61/#62/#63 stage HDL export →
  Yosys-netlist import → black-box co-simulation; #213/#215 carry the
  drawn circuit all the way to a bitstream on a named dev board. The
  settled stance (research-backed) is *orchestrate external tools; never
  reimplement HDL semantics in JLS* — Verilog's own semantics are
  self-inconsistent (Lööw, OOPSLA 2025).
- **A collaborative editor.** #163 and its stack (#167→#168→#169→#170→
  #171) make circuits editable simultaneously, pure-P2P, no server — lab
  pairs and instructor demos as the dominant case.

None of these is speculative fan-out. Each has a research doc, a tracking
issue, and (for the first two) working code in-tree. The architecture's
job is to let all three land *without a rewrite between them* — which
they can, because they share a spine.

## 3. The keystone: one headless kernel, everything else a consumer

The most correct architecture has exactly one non-negotiable move, and
everything else follows from it:

> **Extract an enforced-headless core — the circuit model, the
> simulator, persistence, the element registry, and the operation
> vocabulary — that imports no `java.awt`, no `javax.swing`, and no
> `jls.edit`. Make the GUI one consumer of that core, not its center.**

This is #77's thesis, and it is load-bearing for nearly every future
above. Today there is *no layering*: the abstract `Simulator` imports
Swing and `jls.edit`; `Circuit` holds an `Editor` back-reference and its
`draw` takes a `SimpleEditor`; batch mode is headless only because a
runtime flag is set, not because any layer is structurally GUI-free; and
`JLSInfo` is a ~640-reference public-static hub wiring it all together.

Three extractions, taken together, are the **enabling triad**. Almost
every high-value issue depends on at least one:

| Triad member | Issue | Unlocks |
|---|---|---|
| **Headless core** (model+sim+persistence, zero AWT/Swing) | #77 | Embedding (autograders, services), clean testing, any future UI, the container surface being headless *by construction* not by flag |
| **Element registry** (self-describing `ElementType` descriptors, capability interfaces, one `Orientation`) | #78 | HDL import's cell→element table, palette generation, plugin providers (#212), collapsing the ~16-point element-authoring ritual to one |
| **Operation layer** (`CircuitOp` vocabulary + single `OpSink`) | #167 | CRDT replication (#171), precise undo, per-peer attribution, targeted revert — and it *is* the hardened network surface (#170) |

The registry and the operation layer are already emerging in-tree
(`Gate.Kind`, the `#23` attribute registry, `src/jls/collab/op/`). The
core boundary is the one still to be drawn, and it is the highest-leverage
single change in the whole tracker.

## 4. The target: six layers

```
┌───────────────────────────────────────────────────────────────────────┐
│  DISTRIBUTION & SUPPLY CHAIN  (cross-cutting, not a runtime layer)      │
│  reproducible jar + BOM · attested/signed installers · container       │
│  #82 #134 #184 #185 #188 #190 #191        gate: #159 coverage ratchet   │
└───────────────────────────────────────────────────────────────────────┘
        ▲ packages and ships every layer below; depends on none of them

┌─────────────────────────────┐   ┌─────────────────────────────────────┐
│  L5  GUI  (jls.edit/jls.ui) │   │  L4  HEADLESS SERVICES & EXPORT     │
│  the ONLY AWT/Swing layer   │   │  batch · VCD · image · HDL · boards │
│  #84 decompose SimpleEditor │   │  #200 net observability             │
│  #75 keyboard/a11y  #76 theme│   │  #59/#61/#62/#63 HDL export/import  │
│  #86 dialogs  #73 onboarding │   │      /co-sim                        │
│  #207 #208 #210 identity/a11y│   │  #213 board constraints  #215 bits  │
│  #214 in-editor test panel   │   │  #216 waveform/autograde interop    │
│  #91/#162 UI harness  #101 WL│   │  #202 RV32I golden  #212 plugins    │
└──────────────┬──────────────┘   └──────────────────┬──────────────────┘
               │  both are consumers of the core       │
               │                                        │
        ┌──────┴────────────────────────────────────────┴──────┐
        │  L3  COLLABORATION  (jls.collab.*, no Swing)          │
        │  #163 tracking · #168 P2P transport+SAS · #169 shared │
        │  session v1 · #170 security hardening · #171 CRDT     │
        │  built ENTIRELY on the L0 operation vocabulary        │
        └───────────────────────────┬───────────────────────────┘
                                     │ replicates CircuitOps
┌────────────────────────────────────┴──────────────────────────────────┐
│  L0  jls.core — THE HEADLESS KERNEL   (zero java.awt / javax.swing /   │
│                                        jls.edit; the keystone, #77)    │
│                                                                        │
│   model         Circuit · WireNet · typed pins/bit-widths · subcircuits│
│   elements      ElementRegistry + self-describing ElementType (#78)    │
│                 new datapath elements: reg file, extend (#201);        │
│                 synchronous memory (#199); loader hardening (#198)     │
│   simulation    discrete-event Simulator + BatchSimulator (headless)   │
│   persistence   FORMAT-versioned save/load, canonical serialization    │
│   operations    CircuitOp vocabulary + OpSink, stable ids (#167)       │
│                                                                        │
│   language properties enforced across the whole kernel by the build:   │
│     null-safety (JSpecify+NullAway #93) · records/value semantics      │
│     (#94) · sealed hierarchy + exhaustive switch (#95)   — program #96 │
└────────────────────────────────────────────────────────────────────────┘
```

**L0 — `jls.core`, the headless kernel (#77).** Model, registry,
simulation, persistence, operations. Imports nothing GUI. This is the
embeddable unit an autograder or service links against, and the substrate
the container image already wants to *be* rather than merely pretend to be
via `-Djava.awt.headless`. `JLSInfo` dissolves into it as typed seams
(load result #58, sim handle #49, version record #36, theme/prefs #76) —
not one mutable hub. The modern-Java program (#96: #93/#94/#95) is not a
seventh layer; it is the **quality of L0's construction** — nullness,
value semantics, and exhaustive dispatch as compiler obligations, landed
package-by-package as the core is carved out.

**L3 — `jls.collab` (#163 stack).** Sits directly on L0's operation
vocabulary and canonical serialization — *not* on the GUI. Its lower
sub-layers (op → session → crdt) touch no Swing; only the presence
overlays and peer panel reach up into L5. Pure-P2P, CRDT for the document,
Raft-style discipline only for the peer roster, security structural (a
closed data-only op vocabulary is the entire network surface). It is a
consumer of L0, peer to the GUI, and is the reason the operation layer is
worth extracting even before collaboration ships.

**L4 — headless services & export.** Batch grading, VCD, image export,
and the HDL/board/bitstream pipeline. This layer is where JLS's
*potential* concentrates, and it is realizable precisely because it needs
only L0: the HDL exporter walks the element graph; the importer needs the
registry (#78) as its cell→element table; the plugin provider API (#212)
contributes `ElementType`s to that same registry via `ServiceLoader`. All
external-tool integration is **subprocess orchestration** (Yosys, GHDL,
Icarus, nextpnr) — no in-process HDL semantics, no linking questions.

**L5 — `jls.edit` / `jls.ui`, the GUI.** The *only* layer permitted to
import AWT/Swing. Everything here is a consumer of L0. This is where the
large ergonomics debt lives (#84 decompose the 4k-line `SimpleEditor`;
#75 keyboard/a11y; #76 color-vision-safe semantics, HiDPI, dark mode,
persistent prefs; #86 dialog behavior; #210 stable component identity for
automation and assistive tech; #73 onboarding) and where the in-editor
test panel (#214) puts a GUI face on L0's existing batch `-t` engine. The
UI test harness (#91/#162) and the Wayland rig (#101) verify this layer
without becoming a dependency of the others.

**Distribution & supply chain (cross-cutting).** Reproducible jar+BOM
(done), attested and signed installers (#82/#134), the per-format
byte-reproducibility program (#188/#190/#191), reproducible-build
conformance (#184/#185), and the coverage ratchet (#159) as the gate.
This wraps and ships every layer and depends on none of them at runtime —
correctly modeled as a band around the diagram, not a box inside it.

## 5. The dependency spine (what to build, in order)

The layers imply a strict-enough order that the sequencing is nearly
forced:

```
#77 headless core ──┬── enables ── L4 embedding/HDL/autograde (#59, #200, #216, #202)
                    │
#78 registry ───────┼── enables ── HDL import cell table (#61), plugins (#212),
                    │              palette generation, #214 panel authoring
                    │
#167 operation layer┴── enables ── #168 → #169 → #170 → #171  (collab stack)
        │
        └─ built on #165 stable ids + #166 canonical save (the convergence oracle)

#96 (#93/#94/#95) ── runs THROUGH the core extraction, not after it
#84 (decompose SimpleEditor) ── the L5 counterpart to #77; do it as the
        operation layer lands, since #167 is the first real slice out of it
```

Read one way: **the triad (#77, #78, #167) is the critical path.** Ship
those and the three big futures — autograding/embedding, HDL/FPGA, and
collaboration — stop being rewrites and become additive consumers.
Everything in L4 and L3 is gated on the triad; everything in L5 is
parallelizable against it because the GUI is already a de-facto consumer,
it just imports the core through leaky seams today.

## 6. What the architecture deliberately excludes

"Most correct" is as much about firm boundaries as about layers. These are
settled non-goals; re-proposing them is re-litigating a decision, and the
architecture is stronger for naming them:

- **No central server, ever, for collaboration** (#163). Pure-P2P is a
  constraint, not a stage-one simplification.
- **No in-house HDL simulation or parsing beyond a header scanner** (#59,
  #63). External synthesizer + JSON netlist for import; subprocess
  co-simulation for black boxes. SystemC import is out of scope (it is a
  C++ program / HLS input).
- **No language rewrite** (#96). Java 25 + Kotlin's practices as Java
  incarnations. Kotlin and Scala were assessed and declined with reasons.
- **No internationalization** until a concrete course requests it
  (recorded, `ARCHITECTURE.md`). Inline English is a decision.
- **No hosted-docs dependency in the binary.** In-jar HTML 3.2 help stays
  the offline, version-locked student manual; hosted docs are additive,
  not a replacement (recorded).
- **No plugin execution surface ahead of demand** (#212, gated). The
  registry is closed-by-construction today; opening it via `ServiceLoader`
  is a *priced-on-demand* decision, and any such API must resolve the
  trust/sandbox stance before it ships — the network op vocabulary (#170)
  is closed and data-only for exactly this reason.
- **Installers are not assumed reproducible; they are assumed attested.**
  Byte-reproducibility is pursued per format where reachable and bounded
  honestly where not (#188). The jar+BOM are the reproducible artifacts.

## 7. Why this is the *most correct* shape, not merely *a* shape

- **It falls out of the evidence.** Every open issue lands in exactly one
  layer with no leftovers and no forcing. A backlog that partitions this
  cleanly is describing a structure that already exists in intent.
- **It has a single keystone with maximal leverage.** The headless-core
  extraction (#77) is on the critical path of embedding, HDL, and
  collaboration simultaneously. There is no competing single change with
  that reach, which is the signature of a correct primary boundary.
- **It preserves both front ends as co-equal.** Making the core headless
  by *construction* is what lets the batch/autograde surface be a
  first-class product rather than a GUI afterthought — matching how JLS is
  actually used in courses and CI.
- **It admits the three futures additively.** Datapath/CPU teaching, FPGA
  deployment, and collaboration each attach to L0 (and, for collab, the
  operation layer) without disturbing the others or the GUI — the test of
  a layering that will still be correct after the next three programs
  land.
- **Its boundaries are enforced, not aspirational.** The pattern already
  in the tree — `HeadlessCoreRatchetTest`, `NotificationRatchetTest`, the
  `-Werror`/SpotBugs gate, the coverage ratchet (#159) — means each layer
  boundary can be a compiler or CI obligation, so the architecture is one
  a single maintainer can *hold* over time, which is the only kind worth
  choosing here.

## 8. One-paragraph statement of the determination

JLS should converge on a six-layer architecture whose foundation is an
enforced-headless kernel (`jls.core`: model, self-describing element
registry, discrete-event simulation, versioned persistence, and an
invertible/serializable operation vocabulary), constructed with
null-safety, value semantics, and sealed exhaustive dispatch as
compiler-enforced properties. Above it sit three peer consumers — a
pure-P2P collaboration layer built on the operation vocabulary, a
headless services-and-export layer that orchestrates external HDL/FPGA
toolchains, and the Swing GUI as the sole AWT-importing layer — all
packaged and shipped by a cross-cutting reproducible-and-attested
distribution band. The critical path is the enabling triad #77 (core),
#78 (registry), and #167 (operations); ship it and JLS's three
trajectories — CPU-scale teaching, FPGA deployment, and collaborative
editing — become additive rather than rewrites.
