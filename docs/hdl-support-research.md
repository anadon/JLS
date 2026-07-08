# HDL Support for JLS: VHDL, Verilog/SystemVerilog, and SystemC — Research Report

*July 2026. Prepared for the JLS maintainer as input to the roadmap (issue #33,
Phase 6). Produced with a multi-agent research harness: 5 search angles, 22
sources fetched, 93 candidate claims extracted, 25 subjected to 3-vote
adversarial verification — 23 confirmed, 2 refuted. Facts below marked
**[verified]** survived that process with unanimous or majority votes and are
cited; sections marked **[unverified survey]** did not survive or were not
independently verified and are orientation only. All claims current as of
2026-07-08; supported-board lists, parser coverage, and release states are
moving targets.*

## Executive summary

HDL "support" is three different problems with very different feasibility for
an educational Java schematic simulator:

1. **Export (schematic → HDL)** is the proven, achievable baseline.
   hneemann's *Digital* — the closest comparable tool to JLS (educational,
   Java Swing, schematic-first) — ships exactly this ("A circuit can be
   exported to VHDL or Verilog") and treats it as a **deployment vehicle for
   FPGAs, not a way to teach the HDL**. **[verified]**
2. **Import (HDL → editable schematic)** is realistic only for the
   *synthesizable subset*, and the working architecture in the wild does it
   without writing an HDL parser at all: an external synthesizer (Yosys)
   emits a JSON netlist which the tool maps onto its own elements (the
   yosys2digitaljs → DigitalJS pipeline). **[verified]**
3. **Simulation of full HDL semantics in-house is a trap.** Peer-reviewed
   work (Lööw, OOPSLA 2025) finds the IEEE Verilog simulation semantics
   inconsistent both with itself and with practice; every serious educational
   tool surveyed delegates HDL simulation to mature external simulators
   (GHDL, Icarus Verilog) rather than reimplementing an event scheduler.
   **[verified]**
4. **SystemC is a C++ class library consumed by high-level-synthesis tools**,
   not a netlist format; meaningful *import* is out of scope for JLS. At most,
   JLS could *emit* a structural SystemC netlist, which is just a C++-flavored
   variant of export. **[verified]**

**Recommended staged path** (§6): Verilog + VHDL netlist export first,
validated in CI against external `iverilog`/`ghdl`; then synthesizable-Verilog
import via external Yosys JSON netlists; then (optional) HDL-defined black-box
components co-simulated via GHDL/Icarus; SystemC limited to structural C++
emission or skipped. All license constraints check out for a GPLv3 project.

---

## 1. What the three formats are, and what "support" would mean

### The languages

- **Verilog / SystemVerilog** (IEEE 1364 / IEEE 1800) is, in practice, two
  languages sharing syntax: *synthesizable Verilog*, the subset describing
  hardware structure and behavior, and the full simulation/testbench
  language. IEEE 1364.1-2002 formally defined the RTL-synthesis subset
  ("defines the subset of IEEE 1364 … suitable for RTL synthesis and defines
  the semantics of that subset for the synthesis domain"); it is now
  withdrawn (superseded by IEC/IEEE 62142-2005), so the exact subset boundary
  today is tool-defined. **[verified]** ([IEEE 1364.1](https://standards.ieee.org/standard/1364_1-2002.html),
  [Lööw 2025](https://arxiv.org/pdf/2502.19348))
- Full Verilog **simulation semantics is a poor reimplementation target**:
  Lööw (OOPSLA 2025, peer-reviewed) reports that the standard's simulation
  semantics "has thus far eluded definitive mathematical formalisation" and
  that "the Verilog standard is inconsistent both with Verilog practice and
  itself" (corroborated independently by the well-known IEEE 1364 scheduling
  nondeterminism literature). This is one author's peer-reviewed judgment,
  but it matches why no surveyed educational tool wrote its own Verilog
  simulator. **[verified, attributed]**
- **VHDL** (IEEE 1076, current revision 2019) has the same
  synthesizable-subset-vs-simulation-language split (the analogous synthesis
  subset standard is IEEE 1076.6, also withdrawn). Parser infrastructure for
  VHDL is notably weaker on the JVM than for Verilog (§4).
- **SystemC** (IEEE 1666) is not an HDL file format at all but a **C++ class
  library**; a "SystemC design" is a C++ program. Accellera's *SystemC
  Synthesizable Subset* v1.4.7 (approved March 2016, still the current
  release) "defines the syntactic elements in C++ and SystemC that are
  appropriate for use in SystemC models intended as input for High Level
  Synthesis (HLS) tools." Supporting SystemC *import* therefore means
  building the front half of an HLS tool — categorically out of scope for
  JLS. **[verified]** ([Accellera subset](https://www.accellera.org/images/downloads/standards/systemc/SystemC_Synthesis_Subset_1_4_7.pdf);
  note: the PDF itself was proxy-blocked during research; scope wording was
  verified via Accellera working-group pages and search-index text.)

### The four possible meanings of "support"

| Mode | What it is | Feasibility for JLS |
| --- | --- | --- |
| **Export** | Generate HDL from a drawn circuit | Proven in a direct comparable (Digital); structural netlist emission over JLS's element graph |
| **Import** | Parse HDL into an editable schematic | Realistic for the synthesizable subset only, via an external synthesizer + netlist intermediate |
| **Co-simulation** | HDL-defined components inside a JLS schematic, simulated externally | Proven in Digital (GHDL/Icarus subprocesses); real but significant plumbing |
| **Round-trip** | Edit the same design alternately as HDL and schematic | Not observed in any surveyed tool; generated HDL and hand-written HDL are stylistically disjoint; **do not promise this** |

---

## 2. How comparable programs handle these formats

### Digital (hneemann) — the reference point **[verified]**

*Digital* is ~98.6% Java, Swing-based, explicitly "designed for educational
purposes" — the closest thing to a more-developed JLS. Its choices:

- **Export to VHDL and Verilog** is a shipped, advertised feature (Verilog
  added after maintainer issue [#52](https://github.com/hneemann/Digital/issues/52),
  Sept 2017; VHDL predates it).
- **Design philosophy** (hneemann, in #52): "At the moment I use the VHDL
  export only as a vehicle to run a circuit created with Digital on an FPGA,
  without explaining the VHDL code itself further… an HDL export is not
  suitable to teach the HDL itself. This is because the generated code uses
  only a small fraction of the HDL." He analogizes exported HDL to a JEDEC
  file nobody reads. **JLS should adopt the same honest framing.**
- **No in-house HDL parser or simulator.** Schematic components can be
  *described in* VHDL or Verilog, and those components are simulated by
  external open-source simulators — GHDL for VHDL, Icarus Verilog for
  Verilog ("The open source VHDL simulator ghdl needs to be installed to
  simulate a VHDL defined component…").
- **Generated HDL is validated against real toolchains, in tests**: emitted
  VHDL tested with Xilinx Vivado and GHDL, emitted Verilog with Icarus, via
  automated tests (`VHDLSimulatorTest.java` / `VerilogSimulatorTest.java`)
  that skip when the external tool is absent. This CI pattern transfers
  directly to JLS's Maven/Actions setup.
- **The export is coupled to a student-facing FPGA flow**: when a supported
  board is configured, Digital also generates a pin-assignment constraints
  file (manual v0.31 documents BASYS3 and Mimas/Mimas V2; README also lists
  TinyFPGA BX) and the manual walks students through Vivado project →
  bitstream → programming. Plain HDL export works standalone without a board.

Two candidate claims about Digital were **refuted** in verification and must
not be treated as fact: that its export is "one-click/single-file" and that
its manual documents no HDL import (1–2 vote — the nuance around board
configuration and the characterization were not sustained).

### DigitalJS / yosys2digitaljs — the import architecture **[verified]**

[DigitalJS](https://github.com/tilk/digitaljs) (Marek Materzok, University of
Wrocław) is a browser-based teaching simulator whose Verilog import is exactly:
external **Yosys** synthesizes user Verilog → **JSON netlist** →
[yosys2digitaljs](https://github.com/tilk/yosys2digitaljs) (BSD-2-Clause,
actively maintained — 165 commits, latest release 0.10.3 on 2026-02-24)
converts the JSON into the simulator's own element graph. No in-house parser,
no in-house HDL semantics; the synthesizer defines the accepted subset. This
is a working, maintained proof of the exact pipeline JLS would need for
import.

### Other free and proprietary tools **[unverified survey]**

The harness's claims about the tools below did **not** survive adversarial
verification (mostly for lack of independently fetchable primary sources
within the run), so research question 2 is only partially answered with
verified material. The following is orientation from general knowledge —
treat as leads to confirm, not facts:

- **Logisim-evolution** (Java, the most widely used Logisim fork): has an
  FPGA-synthesis flow that generates VHDL/Verilog for supported boards —
  export-direction, like Digital. How its HDL generation and board database
  are implemented, and its test-vector/autograder features, is the top open
  question left by this research (§7).
- **DEEDS** (Genoa): exports VHDL for FPGA use; schematic-first teaching tool.
- **TkGate**: its own Verilog-*like* save format; not standard-Verilog
  interoperable.
- **Icarus Verilog, Verilator, GHDL, nvc**: not schematic tools at all —
  they are the external simulators/compilers that schematic tools delegate
  to (Verilator compiles synthesizable SystemVerilog to C++; GHDL also has a
  Yosys plugin enabling VHDL through the Yosys netlist path).
- **Proprietary (ModelSim/Questa, Vivado, Quartus, Active-HDL/Riviera, VCS,
  Xcelium)**: HDL-first simulators/toolchains where the schematic view, if
  any, is a *derived* visualization (RTL/elaborated schematic viewers), not
  an editable source of truth. Their block-design editors (e.g. Vivado IP
  Integrator) wire pre-verified IP rather than gate-level schematics. The
  relevant lesson: in industry tools the HDL is the design; schematics are
  views — the opposite of JLS's model, which is why the educational niche
  (schematic as source of truth, HDL as export) remains distinct.

---

## 3. Java-accessible infrastructure and licensing **[verified]**

| Component | What it is | License | GPLv3 fit | Caveats |
| --- | --- | --- | --- | --- |
| [antlr/grammars-v4 `verilog`](https://github.com/antlr/grammars-v4/tree/master/verilog) | ANTLR4 grammar, IEEE 1364-2005 Verilog | MIT (grammar), BSD-3 (ANTLR runtime) | ✔ compatible | **Syntax only**: parse tree + separate pre-parser for compiler directives; elaboration/semantics are yours to build. Java is a first-class CI-tested target. |
| antlr/grammars-v4 `systemverilog` | ANTLR4 grammar, IEEE 1800-2017 | MIT | ✔ | Same syntax-only caveat. |
| [hdlConvertor](https://github.com/Nic30/hdlConvertor) | C++/ANTLR parser → universal AST, Python bindings | MIT | ✔ license-wise | **No Java API** — usable only via subprocess/JNI. VHDL-2008 + earlier (no PSL/tool_directive; June 2025 commits still fixing VHDL-93-era constructs). Its claimed *full* SystemVerilog 1800-2017 coverage was **refuted (0-3)** — do not rely on it for SystemVerilog. |
| [vMAGIC](https://sourceforge.net/projects/vmagic/) | The only native-Java VHDL library found: VHDL'93 parser + object model + writer | LGPLv3 | ✔ (one-way, linkable) | **Abandoned**: Alpha status, last update 2013-05-21, VHDL-93 only, no maintained fork found. At most a starting point for VHDL *generation*. |
| Yosys | External synthesizer; `read_verilog` → `write_json` netlist | ISC | ✔ (subprocess — no linking question at all) | Defines the accepted synthesizable subset for you; VHDL possible via the GHDL Yosys plugin. |
| GHDL, Icarus Verilog | External simulators for co-simulation and CI validation | GPLv2+ | ✔ as subprocesses | The Digital-proven pattern. |

Bottom line: for **Verilog**, JLS can have a native-Java parser today (ANTLR,
MIT) if it ever wants one — but the recommended import path doesn't need it.
For **VHDL**, there is *no* good maintained Java parsing option; VHDL import
would route through Yosys+GHDL-plugin the same way as Verilog. External
subprocess integration sidesteps license-linking questions entirely.

---

## 4. Where JLS stands against its contemporaries

What JLS already has that matches the class: subcircuits, test-vector-driven
batch simulation (`-t` test files, `TestGen`), signal-trace window, printing,
image export, state machines and truth tables as first-class elements (the
latter two are *ahead* of some contemporaries).

What the more-developed contemporaries signal students/instructors now expect
(verified for Digital; Logisim-evolution unverified but consistent):

1. **HDL export as an FPGA on-ramp** — the flagship gap this report addresses.
2. **Board-aware export** (constraints/pin files for named cheap dev boards)
   — the part that makes #1 pedagogically exciting ("your drawing is now on
   real hardware").
3. **External-toolchain honesty** — none of these tools pretend to be Vivado;
   they generate input for it and document the handoff.
4. **Test-vector UX** — Digital has an in-editor test-case component;
   JLS's batch `-t` machinery is functionally similar but CLI-only. An
   in-editor test panel is a smaller, HDL-independent catch-up item.
5. **Autograder friendliness** (stable CLI, machine-readable results) — JLS's
   batch mode is already close; worth documenting as a use case.

## 5. What to promise per language

- **Verilog**: Export (structural netlist, Verilog-2005 for maximum tool
  acceptance) and, later, synthesizable-subset import via Yosys JSON.
  SystemVerilog: accept as *input* only insofar as Yosys accepts it; never
  promise full-language anything.
- **VHDL**: Export second (same element-graph walk, different printer —
  Digital ships both, and FPGA courses split roughly evenly between the two
  languages). Import only via the Yosys/GHDL-plugin route, and only if
  Verilog import proves its value first.
- **SystemC**: Do not promise import or co-simulation. If demand
  materializes, a structural SystemC netlist emitter (modules +
  `sc_signal` wiring) is a low-cost third printer on the same export
  walk — but survey actual demand first; in education SystemC appears in
  architecture/HLS courses, not gate-level logic courses.

---

## 6. Recommended staged path

**Stage 0 — prerequisites (already tracked).** The #33 program's correctness
fixes precede this work; in particular the save-path and loader fixes (#34,
#41, #55) touch the same `Circuit`/element-graph code an exporter walks, and
issue #56's test-corpus work provides the harness HDL export validation will
reuse.

**Stage 1 — Verilog + VHDL export (issue-sized: one milestone).**
Walk the element graph (elements, wire nets, pins already carry names and
bit-widths); emit one structural module per circuit, one per subcircuit type;
map each JLS element to a primitive instantiation or a small behavioral
template (gates → expressions; Register/Clock → clocked `always`/process
blocks; Memory → array + init from the existing contents model; TriState →
conditional assigns; StateMachine/TruthTable → generated case blocks).
Follow Digital's framing (deployment vehicle, not HDL tutorial) and its CI
pattern: golden-file tests always; `iverilog`/`ghdl` compile-and-simulate
tests that skip when the tool is absent. Risks: name legalization (JLS
allows names HDL doesn't), multi-driver nets/tri-state semantics, and
Memory/StateMachine templates dominating the effort.

**Stage 2 — synthesizable-Verilog import via external Yosys (separate
milestone, decide after Stage 1 ships).**
`yosys -p "read_verilog …; prep; write_json"` → parse JSON (one small,
well-documented format) → map Yosys cells to JLS elements. The key open
technical question is the **cell-mapping cost**: JLS's element set (gates,
mux, decoder, adder, register, memory) covers the common cells (`$and`,
`$mux`, `$dff`, `$mem`, …) but Yosys emits a wider family (`$adffe`,
`$sdffce`, width-parameterized cells); either run a `techmap` pass to a
restricted cell library first (recommended) or grow JLS's element set.
Auto-placement of the imported netlist is a real sub-project (DigitalJS uses
an auto-layouter; JLS would need one). Yosys is an external user-installed
dependency — document it, detect it, degrade gracefully.

**Stage 3 (optional) — HDL black-box components co-simulated externally.**
Digital-style: an "HDL component" element whose ports come from parsing the
module header (the one place the ANTLR grammar earns its keep) and whose
behavior runs in a GHDL/Icarus subprocess stepped in lockstep with JLS's
event loop. Highest plumbing cost, biggest didactic payoff (mixed
schematic/HDL labs). Decide only after Stages 1–2 see classroom use.

**Stage 4 — SystemC:** structural C++ netlist printer on the Stage 1 walk,
*if and only if* user demand appears. Otherwise explicitly out of scope.

**Effort honesty:** no external effort estimates survived verification; the
ordering above is this report's synthesis. Stage 1 is bounded and
low-risk (a printer over an existing graph + CI); Stage 2 adds an external
dependency, a mapping layer, and a layout problem; Stage 3 adds process
lifecycle and simulation-synchronization complexity. Size each stage against
the actual codebase before committing (see open questions).

## 7. Open questions for follow-up

1. **Logisim-evolution's actual HDL/FPGA implementation** (direction, subset,
   parser vs. toolchain, board database, test vectors, autograders) — the
   biggest verified-coverage gap in this report; worth a focused code-level
   read of its repo before Stage 1 design freezes.
2. **Yosys cell ↔ JLS element mapping cost** — can JLS's element model
   represent a `techmap`-restricted Yosys cell library without lossy
   transformation, and what does auto-placement require?
3. **ANTLR Verilog grammar completeness on real student code** (preprocessor,
   elaboration) and whether any maintained Java semantic layer exists on top.
4. **A maintained GPLv3-compatible VHDL-2008 path usable from Java** —
   subprocess wrapper around hdlConvertor or GHDL's libghdl, a revived
   vMAGIC fork, or the Yosys+GHDL-plugin route as the only VHDL door.

## 8. Refuted during verification (for the record)

- "Digital's export is one-click/single-file and its manual documents no HDL
  import" — 1–2 vote; characterization not sustained (board-configuration
  nuance).
- "hdlConvertor provides full IEEE 1800-2017 SystemVerilog coverage" — 0–3;
  do not present hdlConvertor as a SystemVerilog solution.

## Sources (primary, verified against)

- https://github.com/hneemann/Digital (README, feature list)
- https://github.com/hneemann/Digital/issues/52 (Verilog export; maintainer's design philosophy)
- Digital manual (v0.31-era mirror): https://wcours.gel.ulaval.ca/2021/a/GIF1002/default/6travaux/Doc_English2019.pdf (FPGA flow, external-simulator validation)
- https://github.com/tilk/digitaljs and https://github.com/tilk/yosys2digitaljs (Yosys-JSON import architecture)
- https://standards.ieee.org/standard/1364_1-2002.html (Verilog RTL-synthesis subset, withdrawn)
- Lööw, *A Correct-by-Authorship Verilog Semantics*, OOPSLA 2025: https://arxiv.org/pdf/2502.19348 / https://dl.acm.org/doi/10.1145/3720484 (simulation-semantics inconsistency)
- https://www.accellera.org/images/downloads/standards/systemc/SystemC_Synthesis_Subset_1_4_7.pdf (+ Accellera working-group pages) (SystemC synthesizable subset = HLS input)
- https://github.com/antlr/grammars-v4/tree/master/verilog (MIT Verilog-2005 / SystemVerilog-2017 grammars, Java target)
- https://github.com/Nic30/hdlConvertor (VHDL parser capabilities and limits)
- https://sourceforge.net/projects/vmagic/ + Pohl et al., Int. J. Reconfigurable Computing 2009, doi:10.1155/2009/205149 (vMAGIC)
- https://ghdl.github.io/ghdl/using/Synthesis.html (GHDL synthesis/Yosys plugin)
