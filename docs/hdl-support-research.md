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

*Follow-up, same month: the report's open questions 1–3 were finalized by a
second, targeted research pass (Logisim-evolution source read; Yosys cell
library, pass, and yosys2digitaljs source read plus JLS element-model
analysis; ANTLR grammar issue-tracker and Maven-ecosystem survey). The
resolved answers are in §7, and §§2–3 and the staged path in §6 are updated
where the answers changed them. Implementation issues for the staged path
are filed as sub-issues of #59.*

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

### Logisim-evolution — the other Java reference point **[verified follow-up]**

Resolved by a source-level read of `github.com/logisim-evolution/logisim-evolution`
(`main` branch, 2026-07-08); every claim below is backed by fetched source
files (see §7.1 for the detailed evidence trail):

- **Direction: export both VHDL and Verilog; no general HDL import.** The
  output language is a global preference defaulting to VHDL
  (`AppPreferences.java`, `hdlType`); the `HdlGeneratorFactory` interface
  declares both languages.
- **Implementation: its own HDL generator framework, one Java generator
  class per library component** (`com.cburch.logisim.fpga.hdlgenerator` +
  e.g. `MultiplexerHdlGeneratorFactory`, `AbstractGateHdlGenerator`) — the
  opposite build-vs-delegate choice from the Yosys-netlist pattern, and a
  maintenance burden JLS should note: every new component needs a matching
  generator.
- **FPGA flow drives four external toolchains via generated scripts**:
  Quartus, Xilinx ISE, Vivado, and an open-source flow (GHDL→Yosys→
  nextpnr-ecp5→ecppack→openFPGALoader — currently Lattice ECP5 only).
  The download pipeline is DRC → fits-board check → interactive pin-mapping
  GUI (click on a photo of the board) → HDL generation → vendor script →
  tool invocation.
- **Board database: 29 built-in XML board files** (BASYS3, DE0/DE10, Zybo,
  Alchitry Au, …), each with FPGA part, pin assignments, and an embedded
  board photo; users can add up to 20 external board files and there is a
  full board-editor GUI.
- **Its embedded-HDL component is VHDL-only and needs a commercial
  simulator**: the `VhdlEntity` component parses the entity header for
  ports, but in-simulator behavior requires **Questa/ModelSim** over a
  TCP/Tcl socket bridge; without it the component outputs UNKNOWN. An open
  issue (#84) has sought GHDL co-simulation for years; a fork did it but was
  never merged. This validates the report's Stage 3 shape (external
  co-simulation) while warning against a commercial-tool dependency —
  Digital's GHDL/Icarus choice is the better model.
- **Test vectors and autograder machinery are strong**: a documented
  test-vector file format (don't-cares, high-Z, sequential mode), a GUI
  runner, and a headless CLI (`--test-vector` with exit codes, `--tty`
  with csv/table outputs, `--substitute` for grading against reference
  implementations, `--test-fpga … HDLONLY` for scripted HDL generation).
  This is the concrete catch-up bar for JLS's batch mode (§4).
- **Pedagogical framing is thin**: the FPGA/HDL flow is presented
  operationally (generated files + board-mapping GUI), with no first-party
  "how your schematic maps to HDL" material — consistent with Digital's
  "deployment vehicle" philosophy arrived at independently.

### Other free and proprietary tools **[unverified survey]**

The harness's claims about the remaining tools did **not** survive
adversarial verification (mostly for lack of independently fetchable primary
sources within the run). The following is orientation from general
knowledge — treat as leads to confirm, not facts:

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

**Follow-up findings that sharpen this table** (§7.3 for full evidence):

- The ANTLR grammars are weaker in practice than their existence suggests.
  Their preprocessor grammars only *recognize* backtick directives on a side
  token channel — macros are **never expanded**, includes never resolved
  (grammars-v4 is action-free by policy), so `input [\`WIDTH-1:0] x` parses
  wrongly unless the caller preprocesses first. The Verilog-2005
  lexer+pre-parser combination has an open, unfixed "produces an empty tree"
  CI-coverage bug (grammars-v4 #3348); the SystemVerilog grammar reportedly
  rejects non-ANSI module headers (#3082), behaves differently across
  targets (#2401), and is ~23× slower than the Verilog grammar on the
  repo's own performance table (independent reports: 131 s / 1.3 GB for a
  1.3 MB design). Test corpora are smoke-test sized (9 real files for
  Verilog; ~120 spec snippets for SV). Neither grammar is published to
  Maven Central.
- **No maintained Java semantic layer exists.** Maven Central's only
  Verilog artifact is a tree-sitter CST binding (no semantics); Verible and
  slang are C++ with Python bindings only; the closest JVM prior art
  (xprova/netlist-graph, Java+ANTLR, MIT) is dormant since ~2018.
  vMAGIC is additionally **not on Maven Central** (repo1 returns 404).
- **For the black-box use case (parse a module/entity header for ports),
  the pragmatic ranking is therefore inverted**: a small hand-written
  header scanner (comments, a minimal `` `define/`ifdef `` subset, ANSI and
  non-ANSI port styles — a few hundred dependency-free lines) beats
  carrying the ANTLR runtime plus an under-tested grammar *plus* a macro
  expander you'd still have to write. For robust real-world files, external
  Yosys already emits exactly the needed data (`write_json`: port
  directions, exact bit offsets/widths, evaluated parameter defaults);
  Verilator `-E`/`--json-only` is an alternative; Icarus has no JSON
  output. VHDL has no preprocessor, so an `entity … end` header scanner is
  even simpler; GHDL's `--file-to-xml` exists but its format is
  version-unstable.

**Auto-layout licensing (needed for import, §6 Stage 2):** ELK (Eclipse
Layout Kernel) is the proven algorithm family for schematic layout — pure
Java, on Maven Central, and DigitalJS ships on elkjs (`layered` +
orthogonal routing). But ELK is plain **EPL-2.0 with no GPL secondary-license
designation** (Exhibit A unfilled, NOTICE declares none), which the FSF
classifies as GPL-incompatible — **GPLv3 JLS cannot link ELK in-process and
distribute the result**. Clean options: run ELK out-of-process (separate
JVM, JSON over pipes — mere aggregation), or hand-roll a heuristic layered
layout. JGraphX (BSD-3, has a Sugiyama layout) is archived/EOL since 2020;
JUNG has no layered layout; yFiles is proprietary.

Bottom line: for **Verilog**, the recommended import path needs no Java
parser at all (Yosys does the parsing), and the black-box feature needs
only a header scanner — the ANTLR grammars are a last resort, not a
foundation. For **VHDL**, there is *no* good maintained Java parsing
option; VHDL routes through Yosys+GHDL-plugin (import) or a trivial
entity-header scanner (black-box). External subprocess integration
sidesteps license-linking questions entirely — which matters twice over
now that ELK's license rules out in-process linking too.

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
The mapping-cost question (§7.2) resolved **favorably**: with the pipeline

```
read_verilog design.v
setattr -mod -unset top; hierarchy -auto-top
proc; opt_clean
memory -nomap; wreduce -memx; opt    # keep $mem_v2 for the ROM case
dffunmap                             # clock-enable + sync-reset → $dff + $mux (exact)
pmuxtree                             # $pmux → $mux trees
techmap -map jls_map.v               # ~6-10 rules: $sub,$neg,$xnor,$eq/$ne,
                                     # comparators, variable shifts, $demux
opt_clean; write_json out.json
```

the surviving cell set is ~15 types, every one with a direct or ≤4-element
JLS realization — JLS's N-input gates, true-parity XOR, latch-capable
Register with initial values, tri-state nets, Mux (which is exactly
`$bmux`), Adder with carry in/out, and Splitter/Binder line up unusually
well with Yosys's model. Because the importer writes JLS's *text save
format* directly (the loader accepts plain text; wires attach by
element-id + port-name references, not geometry), no GUI automation is
needed and connectivity is correct independent of layout quality.

The importer must **reject loudly and precisely** (residual named cells
make this easy) the genuine gaps: **async-reset flip-flops** (`$adff`
family — the idiomatic `always @(posedge clk or posedge rst)` pattern;
either teach sync reset, which `dffunmap` handles exactly, or grow Register
with an async-clear pin as a contained element change), **clocked or
multi-port memories** (JLS Memory is an async single-port SRAM/ROM; only
the async-read ROM maps faithfully — offer `memory_map` for small RAMs),
**wide arithmetic** (`$mul/$div/$mod/$pow`), latches with set/reset, and
x/z semantics (JLS is 2-state; coerce and document). Signedness is compiled
away in the techmap rules.

**Auto-placement is the larger half and its own workstream**: layer 1,
the mechanical netlist-to-JLS-graph conversion (bit-vector connections →
Splitter/Binder mesh, Constants, WireEnd chains on the 12-px grid) is the
bulk of the importer; layer 2, placement proper, should start as a
hand-rolled heuristic layered layout (longest-path layering, barycenter
ordering, greedy orthogonal routing — adequate at classroom scale) behind
an interface, because the proven layouter (ELK Layered, which DigitalJS
uses) is EPL-2.0-only and **cannot be linked into GPLv3 JLS**; an
out-of-process ELK runner can slot in later. Treat layout quality as a
ratchet, not a launch gate. Yosys is an external user-installed
dependency — document it, detect it, degrade gracefully.

**Stage 3 (optional) — HDL black-box components co-simulated externally.**
Digital-style: an "HDL component" element whose ports come from a **small
hand-written module/entity header scanner** (comments + minimal
`` `define/`ifdef `` + ANSI and non-ANSI port styles; a few hundred
dependency-free lines — §7.3 found this strictly better than the ANTLR
route, whose grammars never expand macros and carry open correctness/
performance bugs), with external Yosys `write_json` as the robust fallback
for gnarly files. Behavior runs in a GHDL/Icarus subprocess stepped in
lockstep with JLS's event loop — Digital's choice; Logisim-evolution's
Questa/ModelSim socket bridge shows why a *commercial* simulator dependency
is the mistake to avoid (its own issue tracker has sought GHDL for years).
Highest plumbing cost, biggest didactic payoff (mixed schematic/HDL labs).
Decide only after Stages 1–2 see classroom use.

**Stage 4 — SystemC:** structural C++ netlist printer on the Stage 1 walk,
*if and only if* user demand appears. Otherwise explicitly out of scope.

**Effort honesty:** no external effort estimates survived verification; the
ordering above is this report's synthesis. Stage 1 is bounded and
low-risk (a printer over an existing graph + CI); Stage 2 adds an external
dependency, a mapping layer, and a layout problem; Stage 3 adds process
lifecycle and simulation-synchronization complexity. Size each stage against
the actual codebase before committing (see open questions).

## 7. Open questions — resolved (July 2026 follow-up)

Questions 1–3 were finalized by a targeted second research pass; question 4
was largely answered in passing.

### 7.1 Logisim-evolution's HDL/FPGA implementation — RESOLVED

Source-level read of the repo (`main`, 2026-07-08); summary in §2. Key
facts, each verified against fetched source files: export-only for VHDL
**and** Verilog via its own per-component Java generator framework
(`com.cburch.logisim.fpga.hdlgenerator`; output language is a global
preference, `AppPreferences.java` `hdlType`, default VHDL); no general HDL
import; the FPGA download flow scripts four external toolchains
(`VendorSoftware.java`: Quartus `quartus_sh/…`, Xilinx ISE `xst/…`, Vivado,
and open-source `ghdl`→`yosys -m ghdl`→`nextpnr-ecp5`→`ecppack`→
`openFPGALoader`, ECP5-only per issue #84); 29 built-in XML board files
with embedded photos, user-extensible (20 external slots) plus a board
editor GUI; the `VhdlEntity` embedded-HDL component parses entity headers
in Java (`VhdlParser.java`) but co-simulates only via **Questa/ModelSim**
over a Tcl/TCP socket (`VhdlSimulatorTclBinder.java`) — outputs UNKNOWN
without it, GHDL support requested for years (#84) but never merged;
test-vector engine with documented format plus headless CLI
(`--test-vector` exit codes, `--tty` csv/table, `--substitute`,
`--test-fpga … HDLONLY`). Consequences adopted in §6: per-component
generator frameworks are viable but costly (Digital's approach remains the
model for JLS); avoid commercial-simulator co-simulation dependencies; the
CLI/test-vector feature set is the §4 catch-up bar. Evidence URLs: repo
paths cited above under
`github.com/logisim-evolution/logisim-evolution/…` (README,
`src/main/java/com/cburch/logisim/{fpga,vhdl,prefs}/…`,
`src/main/resources/resources/logisim/boards/`, `docs/test_vector.md`,
discussions #859, issue #84).

### 7.2 Yosys cell ↔ JLS element mapping cost — RESOLVED, favorable

Full analysis from JLS source (element capabilities at file:line level) plus
Yosys cell-library/pass sources (`techlibs/common/simlib.v`, `simcells.v`,
`passes/techmap/{techmap,dffunmap,dfflegalize,pmuxtree,simplemap}.cc`,
`passes/memory/memory_map.cc`, `backends/json/json.cc`) and the
yosys2digitaljs pipeline (`src/core.ts`). Verdict: **tractable and small.**
`dffunmap` eliminates clock-enable/sync-reset FF variants *exactly* at word
level; `pmuxtree` eliminates `$pmux`; one custom `jls_map.v` (~6–10 rules)
covers `$sub/$neg/$xnor/$eq/$ne`/comparators/variable-shifts/`$demux`; the
surviving ~15 cell types all map directly or in ≤4 JLS elements (details
and the full mapping table are reflected in §6 Stage 2). Genuine,
loudly-rejectable gaps: async-reset FFs (`$adff` family — JLS Register has
no reset pin; sync-reset teaching idiom works exactly), clocked/multi-port
memories (JLS Memory is async single-port with tri-state output; async-read
ROM maps faithfully), `$mul/$div/$mod/$pow`, set/reset latches, and x/z
bits (JLS BitSet is 2-state). The importer generates JLS's plain-text save
format directly (loader accepts it; wires attach by id+port-name refs), so
correctness is layout-independent. Auto-placement: ELK Layered is the
proven algorithm (DigitalJS ships elkjs) but ELK is EPL-2.0 **without** the
GPL secondary-license designation — per the FSF, GPL-incompatible for
linking; start with a hand-rolled heuristic layered layout behind an
interface, optionally an out-of-process ELK runner later.

### 7.3 ANTLR Verilog grammars on real code — RESOLVED, negative

The grammars-v4 Verilog/SystemVerilog grammars *recognize* preprocessor
directives on a side token channel but **never expand macros or resolve
includes** (action-free by policy) — `input [\`WIDTH-1:0] x` fails without
external preprocessing; the Verilog lexer+pre-parser combination has an
open "empty tree on any input" bug (#3348); the SV grammar is reported
ANSI-header-only (#3082), target-inconsistent (#2401), and ~23× slower
than the Verilog grammar (repo performance table; independent report:
131 s/1.3 GB for 1.3 MB of SV). Test corpora: 9 real Verilog files, ~120 SV
spec snippets; not on Maven Central. **No maintained Java semantic layer
exists** (Central's only Verilog artifact is a tree-sitter CST binding;
Verible/slang are C++/Python-facing; nearest JVM prior art dormant since
~2018). Recommendation adopted in §6 Stage 3: hand-written header scanner
as the built-in path; external Yosys `write_json` (directions, exact bit
widths, evaluated parameter defaults) as the robust path; ANTLR demoted to
last resort.

### 7.4 VHDL-from-Java path — largely answered in passing

vMAGIC is dead *and* absent from Maven Central (repo1 404s); GHDL's
`--file-to-xml` can dump an analyzed AST for entity/port extraction via
subprocess but its format is version-unstable and huge. Since VHDL has no
preprocessor, a hand-written `entity … end` header scanner covers the
black-box case, and Yosys+GHDL-plugin remains the only credible VHDL
*import* door. No further research needed unless Stage 3 VHDL support is
prioritized.

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

Added by the July 2026 follow-up pass (§7):

- Logisim-evolution sources: https://github.com/logisim-evolution/logisim-evolution — README; `src/main/java/com/cburch/logisim/fpga/hdlgenerator/HdlGeneratorFactory.java`; `fpga/download/Download.java`; `fpga/settings/VendorSoftware.java`; `prefs/AppPreferences.java`, `prefs/FpgaBoards.java`; `vhdl/base/VhdlEntity.java`, `vhdl/sim/VhdlSimulatorTclBinder.java`; `src/main/resources/resources/logisim/boards/` (29 board XMLs, e.g. BASYS3.xml); `docs/test_vector.md`; `gui/start/Startup.java`; issue #84, discussion #859
- Yosys cell library & passes: https://github.com/YosysHQ/yosys — `techlibs/common/simlib.v`, `techlibs/common/simcells.v`, `docs/source/cell/*.rst`, `passes/techmap/{techmap,simplemap,dffunmap,dfflegalize,pmuxtree}.cc`, `passes/memory/memory_map.cc`, `backends/json/json.cc` (readthedocs renderings were proxy-blocked; the generating sources were read instead)
- yosys2digitaljs pipeline: https://github.com/tilk/yosys2digitaljs (`src/core.ts` `prepare_yosys_script`, `src/index.ts`); DigitalJS layout: https://github.com/tilk/digitaljs (`package.json` elkjs ^0.11.0, `src/elkjs.mjs`)
- ELK licensing: https://github.com/eclipse-elk/elk (LICENSE.md, NOTICE.md, `Layered.melk`); https://repo1.maven.org/maven2/org/eclipse/elk/; https://www.gnu.org/licenses/license-list.html#EPL2; https://www.gnu.org/licenses/gpl-faq.html#MereAggregation; alternatives https://github.com/jgraph/jgraphx (archived), https://github.com/jrtom/jung
- ANTLR grammar practicalities: https://github.com/antlr/grammars-v4 issues #3348, #3082, #2401, #2363, #1438, #3632; performance table (repo `performance.html`); https://groups.google.com/g/antlr-discussion/c/ynvgmjjsZTw; https://central.sonatype.com/artifact/io.github.bonede/tree-sitter-verilog; https://github.com/chipsalliance/verible; https://github.com/MikePopoloski/slang; https://github.com/xprova/netlist-graph; Verilator `--json-only`/`-E`: https://github.com/verilator/verilator/blob/master/docs/guide/exe_verilator.rst; Icarus targets: https://steveicarus.github.io/iverilog/usage/command_line_flags.html; GHDL `--file-to-xml`: https://github.com/ghdl/ghdl/blob/master/doc/using/CommandReference.rst
