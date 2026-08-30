# JLS Roadmap — From Teaching Simulator to Open-Hardware On-Ramp

*July 2026. A sequencing document: the end goal, the disciplined path to it,
and how the existing open GitHub issues map onto that path. It consolidates
what is today scattered across tracking issues (#33, #59, #96, #163, #188)
into one ordered picture. It is strategy, not a contract — the normative
specs remain [`batch-interface.md`](batch-interface.md),
[`file-format.md`](file-format.md), and
[`simulation-semantics.md`](simulation-semantics.md). Companion analyses it
builds on: [`hdl-support-research.md`](hdl-support-research.md) (the verified
HDL staged path) and
[`eda-landscape-competitive-analysis.md`](eda-landscape-competitive-analysis.md)
(where JLS sits in the wider tooling world and why "competitor of a foundry"
is a category error). Issue numbers are current as of 2026-07; items marked
**(propose)** are not yet filed.*

---

## Preface — the end goal

**JLS becomes the unambiguous best *free* tool for learning digital logic,
and the friendliest *on-ramp* from a hand-drawn circuit to logic running on
real, open hardware — while staying a self-contained, reproducible, single
artifact a student can trust and a lab can deploy.**

Concretely, at the end of this road a student can:

1. **Draw** a circuit — from gates up to a working CPU — in an editor that is
   approachable, keyboard-operable, theme-aware, and inviting on first run.
2. **Simulate and test** it interactively or headlessly, with an in-editor
   test panel and a stable autograder-grade CLI, watching any internal signal
   in a waveform.
3. **Bring HDL in and push it out**: import synthesizable Verilog as an
   editable schematic, and export structural Verilog/VHDL plus a
   pin-constraint file for a named cheap board.
4. **Reach real hardware** by handing that export to the open FPGA flow
   (Yosys → nextpnr → iCE40/ECP5) and watching their drawing blink an LED —
   with JLS as the friendly front door, not a reimplementation of Vivado.
5. Optionally **collaborate** on the same circuit in real time, and trust that
   the binary they run is exactly the audited, reproducible source.

Equally important is what the end goal **excludes**, deliberately and on the
record (see [`eda-landscape-competitive-analysis.md`](eda-landscape-competitive-analysis.md) §8):

- JLS is **not** an FPGA implementation suite (no place-and-route, no
  bitstream generation of its own — it *feeds* nextpnr/Vivado/Quartus).
- JLS is **not** an ASIC design or sign-off flow (no timing/power, no
  Liberty/SDC/SDF, no LEF/DEF/GDSII) and has no relationship to a silicon
  foundry.
- **SPICE (analog) simulation and PCB layout are different domains, not
  extensions of a gate-level digital simulator.** They enter the roadmap only
  as optional *bridges* to best-of-breed external tools (ngspice, KiCad),
  never as homegrown engines, and only after the enabling architecture and
  real demand exist (Phase 5, gated).

The North Star, in one line: **best-in-class for learning and for the
first mile toward hardware — explicitly not best-in-class for tape-out.**

---

## The constraints every phase is designed around

These are settled decisions (see `ARCHITECTURE.md` "Recorded decisions" and
[`library-survey-2026-07.md`](library-survey-2026-07.md) ground rules); the
roadmap obeys them rather than fighting them.

1. **The self-contained jar is the product.** Students run `java -jar
   jls.jar` on lab machines. Every runtime dependency is paid for in jar size
   and supply-chain surface. Small and few beats featureful.
2. **GPLv3-compatible runtime code only**, actively maintained. (This already
   rules out linking ELK in-process — [`hdl-support-research.md`](hdl-support-research.md) §3.)
3. **Delegate, don't reimplement.** The proven educational pattern (Digital,
   Logisim-Evolution, DigitalJS) is to shell out to Yosys/GHDL/Icarus/nextpnr,
   not to rebuild them. JLS already does this in CI. Every "bigger" ambition
   below is an *integration*, not an engine.
4. **Contracts are pinned by tests.** The CLI table, save format, load-error
   taxonomy, and batch stdout are compatibility contracts; expansion must
   preserve them exactly.
5. **Single-maintainer bandwidth is the binding constraint.** The ordering
   below front-loads high-leverage, low-surface work and gates anything that
   would require a second maintainer or a second delivery model.

---

## The path

Phases are ordered by dependency, not calendar. Phase 0 is in flight; Phases
1–2 are the heart of the end goal; Phase 3 is the architectural gate that
unlocks everything larger; Phases 4–5 are reach and frontier, partly
parallelizable once Phase 3 lands.

### Phase 0 — Correctness and foundation *(in flight)*

An exporter and an importer both walk the same `Circuit`/element graph a
loader builds, so correctness precedes expansion. This is the
[`hdl-support-research.md`](hdl-support-research.md) §6 "Stage 0" principle.

| Issue | Work | Why it's a prerequisite |
| --- | --- | --- |
| **#33** *(tracking)* | Post-audit correctness/hardening → v5.0.0 | The umbrella; most items below are its children |
| **#198** | Loader silently mis-loads malformed wire nets | Import/round-trip integrity — the graph an exporter trusts |
| **#77** | Extract a headless `jls.core` (simulator base imports no Swing) | Clean core is the substrate for batch, import, and any future engine bridge |
| **#93 / #94 / #95 / #96** *(#96 tracking)* | Modern-Java program: null-safety, value semantics, sealed dispatch | Makes the element graph safe to extend and refactor |
| **#91 / #162 / #159** | UI test harness, CI display substrate, coverage-ratchet | You cannot safely grow the editor without these tests in place |

### Phase 1 — Win the educational category

The bounded, on-mission gap to the strongest peers (Digital,
Logisim-Evolution). Delivering just this makes JLS the best free digital-logic
teaching tool.

**1a. HDL export maturation + board-aware export** — under **#59** *(tracking)*.
Structural Verilog-2005 export exists today; add the VHDL printer on the same
graph walk, and **(propose) a "board-aware export" issue**: emit a
pin-constraint file (XDC/QSF/PCF) for a small set of named cheap boards, so
the exported netlist is one command from a bitstream. CI-validate emitted HDL
with `iverilog`/`ghdl` (the Digital pattern). *Highest-leverage single item
in the whole roadmap — it is what makes the on-ramp exciting.*

**1b. In-editor test panel** — **(propose)**, built on the existing batch `-t`
engine and the #91 harness. Digital's built-in test-case component is its
standout UX; JLS already does the work headlessly, so this is a GUI front-end,
not new semantics.

**1c. Datapath and element richness** — the elements that make real
architecture labs first-class:

| Issue | Work |
| --- | --- |
| **#201** | Multi-port register file + sign/zero-extend-from-field elements |
| **#199** | Memory: optional synchronous (clock-edge) write mode |
| **#200** | Batch/headless observability: watch arbitrary internal nets in stdout + VCD |
| **#202** | RV32I CPU worked example: integration golden + HDL-export differential oracle |

(These also harden the existing `riscv/` RV32I CPU into a first-class,
tested showcase — a credibility asset no peer markets.)

**1d. A compelling design experience** — the ergonomics that make "compelling
relative to commercial offerings" true where JLS can win it (approachability,
not feature count):

| Issue | Work |
| --- | --- |
| **#76** | Visual ergonomics: dark mode, color-vision-safe semantics, HiDPI, system integration |
| **#73** | First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability |
| **#75** | Keyboard operability and accessibility (focus, menu accelerators) |
| **#86** | KeyPad dismissal bug (Esc/click-outside) |

### Phase 2 — Become the on-ramp / hub

Turn the one-way export street into a two-way bridge, and connect the far end
to real hardware and the open verification toolchain. This is where the
"friendly front door to a professional open flow" identity is realized.

| Issue | Work |
| --- | --- |
| **#61** | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists |
| **#62** | HDL Stage 2 companion: heuristic layered auto-layout for imported netlists (ELK is EPL-only → hand-rolled or out-of-process, per research §7.2) |
| **#63** | HDL Stage 3: black-box HDL component, external GHDL/Icarus co-simulation |

Plus two **(propose)** items that complete the pipeline:

- **Bitstream handoff (documented + scripted):** a `docs/` recipe and helper
  script driving Yosys → nextpnr → iCE40/ECP5 → `openFPGALoader`, consuming
  the Phase-1a board constraints. This is the "blink a real LED" payoff, kept
  as an external-tool orchestration (no bitstream code in JLS).
- **Waveform/verification interop:** confirm standard `.vcd` write (an open
  uncertainty flag), document the GTKWave/Surfer handoff, and sketch a
  cocotb-style autograde bridge over the batch interface.

### Phase 3 — The extension architecture *(the gate to everything larger)*

Nothing in Phases 4–5 should ship as baggage in every student's jar. This
phase builds the seam that makes larger capabilities *opt-in modules* rather
than mission-diluting weight — the `ServiceLoader`-based registry recorded as
the direction when the old plugin loader was removed (#80).

| Issue | Work |
| --- | --- |
| **#78** | Element descriptor and registry: self-describing elements, compiler-enforced authoring contract (collapses the ~16-step "add an element" list in `ARCHITECTURE.md`) |
| **#84** | Decompose `SimpleEditor` (4,119 lines, 9-state machine) into testable units |
| **#77** | (from Phase 0) the headless core the registry plugs into |

Until this phase lands, **SPICE and PCB integration stay out of scope** — not
because they're impossible, but because without the registry they would have
to be wired into the monolith, violating constraint #1.

### Phase 4 — Reach *(parallelizable after Phase 3; partly in flight)*

Differentiators that widen adoption without changing JLS's identity.

- **Real-time collaboration** — **#163** *(tracking)*, built in order:
  **#167** operation layer (also the foundation for robust undo and
  scripting) → **#168** P2P/identity/transport → **#169** shared session v1 →
  **#170** collaboration security → **#171** CRDT replication. A genuine
  differentiator versus every desktop peer; the operation layer (#167) pays
  for itself even if collaboration slips.
- **Trust and distribution** *(largely in flight)* — **#82** jpackage
  installers + file association, **#134** Authenticode signing, **#101**
  Wayland GUI CI rig, and the reproducibility program **#188** *(tracking)*
  with **#184/#185/#190/#191**. This is the "a lab can deploy, a student can
  trust" pillar of the end goal.
- **Web/WASM reach** — **(propose, strategic open question)**. CircuitVerse's
  zero-install browser reach is the single biggest structural advantage any
  peer holds over a Java desktop tool. No issue exists; whether to pursue it
  (and how, given the Swing codebase) is the largest unresolved strategic bet
  and deserves its own research spike before any commitment.

### Phase 5 — The gated frontier: SPICE and PCB, integration-only

The ambition the strategy discussion raised, placed where it honestly belongs:
last, gated, and never as a homegrown engine.

**Preconditions — all three must hold before any work starts:**

1. The extension architecture (Phase 3, #78) has shipped.
2. There is demonstrated, repeated user demand (instructors/courses asking).
3. Maintainer bandwidth exists — realistically, a second maintainer or a
   scoped companion effort, because these are separate products' worth of
   surface.

**If and only if those hold:**

- **SPICE / mixed-signal:** emit a SPICE netlist from a schematic and drive
  **ngspice** (BSD, embeddable) as an external process at the analog/digital
  boundary; render results in the existing waveform view. A *format +
  subprocess* problem, never a homegrown nonlinear solver (JLS's engine is
  discrete-event 2-state+HiZ — essentially zero code reuse with a SPICE
  matrix solver).
- **PCB:** JLS is the wrong source of truth for a board (it draws logic, not
  footprinted components). The realistic bridge is **netlist export
  (EDIF/KiCad netlist) → KiCad**, which owns layout, DRC, and Gerber. JLS
  never grows a board model.

Both would be recorded as scope decisions with explicit revisit triggers, in
the repo's established style — so the boundary stays deliberate rather than
eroding by drift.

---

## Sequencing at a glance

```
Phase 0  Correctness & foundation      #33 #198 #77 #93-96 #91/#162/#159
   │
Phase 1  Win the category              #59(export+board*) test-panel* #199-202 #73 #75 #76 #86
   │        └── on-mission, bounded; delivering this alone wins the peer race
Phase 2  Become the on-ramp/hub        #61 #62 #63  bitstream* vcd/cocotb*
   │        └── draw → simulate → import/export → real hardware
Phase 3  Extension architecture        #78 #84 (+#77)      ← GATE for anything larger
   │
Phase 4  Reach (parallel)              collab #163/#167-171   trust #82/#134/#188/#101   web*
   │
Phase 5  Frontier (gated)              SPICE via ngspice*   PCB via KiCad netlist*
            └── preconditions: Phase 3 shipped + demand + bandwidth
```
*`*` = not yet filed as an issue (the **(propose)** items above).*

---

## Decision triggers (what reopens a settled boundary)

- **Pursue web/WASM reach** if classroom-adoption data shows install friction
  is the dominant barrier — run a research spike first (Phase 4).
- **Open Phase 5 (SPICE/PCB)** only when all three preconditions above hold;
  until then the answer to "can JLS do SPICE/PCB?" is "it bridges to the tools
  that do, once the extension seam exists."
- **Re-scope the end goal itself** if a second maintainer joins and the
  project chooses to become a broader electronics-education suite — that is a
  different, larger identity than this roadmap targets, and should be adopted
  explicitly, not by accretion.

The through-line: every phase makes JLS better at *what it is* before it
considers becoming something larger, and every "larger" step is an
integration that respects the self-contained, reproducible, single-artifact
identity that makes JLS trustworthy in the first place.
