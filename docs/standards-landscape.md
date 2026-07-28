# The standards and certification landscape of digital logic design

*A scope-and-orientation survey, July 2026. Companion to
[`docs/grand-architecture.md`](grand-architecture.md) (what JLS should
become) and [`docs/hdl-support-research.md`](hdl-support-research.md)
(how the HDL half of it works).*

## 0. What this document is

The question this answers: **across the whole logic-design stack — from
system-level specification down to the data a mask writer consumes —
what standards and certification regimes exist, which of them does JLS
already conform to, which could it plausibly take on, and which belong
permanently to other software?**

Three definitions, because the question mixes two different things:

- A **standard** is a published technical specification you conform to.
  Conformance is usually self-asserted (nobody audits your VCD writer).
- A **certification / conformity regime** is a process by which a third
  party (or a regulator) asserts that a product, a *tool*, an
  organization, or a person meets a bar. This is where the word
  "certification" literally applies to an EDA tool: **tool
  qualification** under ISO 26262, IEC 61508, DO-330 and friends.
- A **de facto standard** has no standards body but is what everything
  interoperates on anyway (Liberty, SDC, GDSII, Yosys JSON). These are
  included and marked, because ignoring them would misrepresent the
  field: the physical-design half of the stack runs mostly on de facto
  formats and vendor-proprietary decks.

**Entries are numbered globally, 1..N**, so the "sum" the question asks
for is well defined: see §14 for the tally. The count is of *named
entries in this document*, not a claim that the universe of standards is
exactly this large — sub-parts, national adoptions (IEC/ISO
re-publications of IEEE documents), and every vendor deck format could
inflate it arbitrarily.

**Relevance column** — every entry carries one of:

| Mark | Meaning |
|---|---|
| **HAVE** | JLS conforms to it today; evidence cited in §1 |
| **ROADMAP** | already funded by a tracked issue / research doc |
| **COULD** | plausible for JLS; §12 ranks these by cost and value |
| **ADJACENT** | JLS could interoperate with it but should never implement it |
| **OTHER** | belongs to other software; named so the boundary is explicit |

**A correction recorded in place (revision 2).** The first revision built
§12 by asking *which certification regimes could apply to JLS* rather
than *which regimes exist in this space* — which silently dropped the
question's own "or can be taken on by other software" clause, and with
it the entire family of **foundry certification programs** (TSMC OIP,
Samsung SAFE, Intel Foundry) that is how EDA tools are actually
certified in this industry. That family is now §12.i (#288–#304). The
same framing error let §13 rank a general open-source badge above ISA
compliance; §13 is now split along the two axes it was conflating.

**Currency caveat.** Revision years are given where they are load-bearing
and are current to roughly mid-2026; IEEE, SEMI, JEDEC, and IPC revise
continuously and several entries below are marked *(verify revision)*.
Withdrawn-but-still-used standards are marked as such, because in this
field withdrawal rarely means disuse.

---

## 1. What JLS conforms to today (audited)

Everything here is grounded in the tree, not aspiration:

| Standard (ID) | Evidence |
|---|---|
| IEEE 1364-2001 §18 **VCD** (#66) | `docs/batch-interface.md:212`, `BatchSimulator.toVcd`; byte-deterministic, golden-tested |
| **Verilog-2005** structural export (#31) | `src/jls/hdl/VerilogEmitter.java:9`; IEEE 1364-2005 Annex B reserved-word list at `src/jls/hdl/HdlNames.java:30` |
| **VHDL-93/2002** structural export (#25) | `src/jls/hdl/VhdlEmitter.java:15` |
| IEEE **1164** `std_logic_1164` (#26) | `VhdlEmitter.java:67`, plus the `when others` full-coverage handling of the 9-value type at `:470` |
| IEEE **1076.3** `numeric_std` (#27) | `VhdlEmitter.java:68,363` |
| **Yosys JSON netlist** import (#75) | `src/jls/hdl/yosys/`, `src/jls/hdl/imp/` |
| **PCF** pin-constraint emission (iCE40/icestorm) (#81) | `src/jls/hdl/board/PcfEmitter.java`; `docs/hdl-support-research.md:505` |
| **RISC-V RV32I** unprivileged ISA (#5) | `riscv/` — a real circuit, differentially fuzzed against a reference emulator |
| **PNG / JPEG / SVG** export (#158, #159, #160) | batch `-i` image export |
| **ZIP**, **XZ / LZMA2**, plain-text containers (#161, #162) | `.jls` save formats; `docs/file-format.md` |
| **Unicode / UTF-8** (#154, #155) | save format and element names |
| **POSIX**-shaped CLI exit contract (#166) | `docs/batch-interface.md:33-48` — 0/1/2, `jls: error:` on stderr |
| **RFC 2119** keywords in normative docs (#168) | `docs/batch-interface.md`, `docs/file-format.md` |
| **SemVer 2.0.0** (#169) | release tags, `CHANGELOG.md` |
| **CycloneDX** SBOM (#188) | `pom.xml:588` |
| **SLSA**-style build provenance + **in-toto** attestations (#189, #190) | `gh attestation verify`, README |
| **Sigstore / cosign** keyless signing (#191) | container images, README |
| **Reproducible Builds** / fixed-epoch archives (#186) | `pom.xml:44`, `docs/reproducibility.md` |
| **OpenSSF Scorecard** (#198) | README badge |
| **Authenticode** signing (#181) | Windows MSI via SignPath |
| **OCI** image + distribution specs (#176) | `ghcr.io/anadon/jls`, multi-arch incl. `linux/riscv64` |
| **Debian Policy** / **RPM** / **AppImage** / **MSI** packaging (#177–#180) | `scripts/build-installer.sh`, `resources/packaging/` |
| **freedesktop** Desktop Entry + shared-mime-info (#172, #173) | `resources/packaging/resource-dir-linux/JLS.desktop`, `application/x-jls-circuit` |
| **Wayland** and **X11** display protocols (#184, #185) | `ToolkitPolicy`, `scripts/wayland-rig.sh`, the supported-desktop matrix in README |
| **Java Accessibility API** conformance work (#213) | `docs/keyboard-a11y-verification.md`, `docs/component-naming.md` |
| **AES-256-GCM, SHA-256, HMAC-SHA256, HKDF, X25519, Ed25519**, TLS-1.3-shaped handshake (#199–#206) | `src/jls/collab/net/Crypto.java`, `Handshake.java` |
| **GPL-3.0** license identity (SPDX identifiers #171 are not yet applied per-file) | `LICENSE`, `pop_GPLv3.pdf` |

That is **51 numbered entries marked HAVE below, in a ~69k-line
educational logic simulator** — which is itself the headline finding: the software
half of the stack (files, CLI, packaging, supply chain, crypto,
accessibility) already outnumbers the EDA half, and will keep doing so.

JLS also *publishes* three de facto standards of its own, which is worth
naming because downstream tools (autograders, forks) depend on them:
the `.jls` file format (`docs/file-format.md`), the batch/grading
interface (`docs/batch-interface.md`), and the simulation semantics
(`docs/simulation-semantics.md`).

---

## 2. Tier 1 — System, architecture, and interface specification

The "highest level design space": what the chip is supposed to be,
before anyone writes RTL.

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 1 | SysML v2 (and v1.6) | OMG | System modelling, requirements↔design traceability | OTHER |
| 2 | UML 2.5.1 / ISO-IEC 19505 | OMG / ISO | Structural + state-machine modelling; JLS's state-machine element is an informal cousin | ADJACENT |
| 3 | ISO/IEC/IEEE 42010 | ISO/IEC/IEEE | Architecture description practice | OTHER |
| 4 | IEEE 1685 **IP-XACT** (2022) | IEEE | XML metadata for IP: ports, memory maps, bus interfaces, generators | COULD |
| 5 | **RISC-V Unprivileged ISA** (ratified) | RISC-V International | Instruction semantics — RV32I is what `riscv/` implements | HAVE |
| 6 | RISC-V Privileged ISA | RVI | CSRs, traps, modes | COULD |
| 7 | RISC-V Profiles (RVA23/RVB23) | RVI | Mandated feature bundles | OTHER |
| 8 | RISC-V Debug Specification | RVI | Debug transport, triggers | OTHER |
| 9 | RISC-V SBI / platform specs | RVI | Firmware/OS interface | OTHER |
| 10 | ARM **AMBA** (AXI/ACE/AHB/APB/CHI) | Arm | On-chip interconnect protocols | ADJACENT |
| 11 | **Wishbone** B4 | OpenCores | Open on-chip bus — the realistic one for teaching | COULD |
| 12 | **TileLink** | CHIPS Alliance / SiFive | Open coherent interconnect | OTHER |
| 13 | Avalon | Intel | FPGA-oriented on-chip bus | OTHER |
| 14 | Open Core Protocol (OCP) | Accellera | Core-to-bus socket | OTHER |
| 15 | **UCIe** (1.1/2.0) | UCIe Consortium | Die-to-die chiplet interconnect | OTHER |
| 16 | Bunch of Wires (BoW) | OCP ODSA | Open chiplet PHY | OTHER |
| 17 | JEDEC memory: JESD79 (DDR), JESD209 (LPDDR), JESD235 (HBM) families | JEDEC | Memory device/interface behaviour | ADJACENT |
| 18 | PCIe Base Specification | PCI-SIG | Link protocol | OTHER |
| 19 | USB specifications | USB-IF | Link protocol | OTHER |
| 20 | IEEE 802.3 Ethernet | IEEE | MAC/PHY | OTHER |
| 21 | MIPI (D-PHY, CSI-2, DSI) | MIPI Alliance | Mobile interfaces | OTHER |
| 22 | I²C spec (NXP) / SPI (de facto) | NXP / — | Simple serial buses — realistic teaching targets | COULD |
| 23 | SDL (ITU-T Z.100) | ITU-T | Specification & description language for reactive systems | OTHER |
| 24 | AUTOSAR (Classic / Adaptive) | AUTOSAR | Automotive software architecture | OTHER |

**Where JLS sits:** it owns exactly one entry here (#5), and that
by accident of being a good enough simulator to host a real CPU. #4
(IP-XACT) is the only realistic addition: a subcircuit already *is* a
component with typed, named, bit-width-carrying ports, which is
precisely IP-XACT's `component`/`busInterface` payload.

---

## 3. Tier 2 — Design entry: languages and schematic notation

**The tier JLS actually lives in.** Note especially #43–#45: the
schematic-symbol standards are the ones a drawing tool can conform to and
nearly all of them silently do not.

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 25 | IEEE **1076 VHDL** (2019) | IEEE | VHDL language | HAVE (93/2002 subset emitted) |
| 26 | IEEE **1164** `std_logic_1164` | IEEE | 9-value logic type — the reference multi-value model | HAVE |
| 27 | IEEE **1076.3** `numeric_std` (+ fixed/float pkgs) | IEEE | Arithmetic on `std_logic_vector` | HAVE |
| 28 | IEEE 1076.1 **VHDL-AMS** | IEEE | Mixed-signal VHDL | OTHER |
| 29 | IEEE 1076.4 **VITAL** | IEEE | ASIC library timing modelling in VHDL | OTHER |
| 30 | IEEE 1076.6 RTL synthesis subset | IEEE | *Withdrawn*; subset now tool-defined | ADJACENT |
| 31 | IEEE **1364 Verilog** (2005) | IEEE | *Superseded by 1800*, still the max-compatibility export target | HAVE |
| 32 | IEEE 1364.1 / IEC-IEEE **62142** | IEEE/IEC | Verilog RTL synthesis subset; 1364.1 withdrawn | ADJACENT |
| 33 | IEEE **1800 SystemVerilog** (2023) | IEEE | Design + assertions + testbench + DPI | ADJACENT (accept only as far as Yosys does) |
| 34 | IEEE **1666 SystemC** (2023) | IEEE / Accellera | C++ class library, TLM-2.0 | ADJACENT |
| 35 | IEEE 1666.1 SystemC AMS | IEEE | Analog/mixed-signal extensions | OTHER |
| 36 | Accellera SystemC **Synthesizable Subset 1.4.7** | Accellera | HLS input subset | OTHER |
| 37 | Accellera **Verilog-AMS** LRM | Accellera | Mixed-signal Verilog | OTHER |
| 38 | Accellera **SystemRDL** 2.0 | Accellera | Register-map description → RTL/docs/headers | COULD |
| 39 | **Chisel / FIRRTL** spec | CHIPS Alliance | Scala-embedded HDL + its IR | ADJACENT |
| 40 | **CIRCT / MLIR** HW dialects | LLVM | Emerging compiler IR for hardware | OTHER |
| 41 | De facto HDL DSLs: SpinalHDL, Amaranth, Bluespec, Migen | — | Alternative design entry | OTHER |
| 42 | **Verilog-A** | Accellera | Analog behavioural modelling | OTHER |
| 43 | **IEEE 91 / 91a-1991** — graphic symbols for logic functions | IEEE/ANSI | Distinctive-shape *and* rectangular symbols, dependency notation (qualifying symbols, `&`, `≥1`, `Σ`, control blocks) | **COULD** |
| 44 | **IEC 60617-12** — graphical symbols, binary logic elements | IEC | The international rectangular-symbol set | COULD |
| 45 | IEEE 315 / ANSI Y32.2 | IEEE/ANSI | Graphic symbols for electrical & electronics diagrams | COULD |
| 46 | **IEC 61131-3** (LD/FBD/ST) | IEC | PLC logic languages — the *other* schematic logic domain | OTHER |
| 47 | WaveDrom / WaveJSON | — (de facto) | Timing-diagram notation | COULD |

**The finding worth acting on:** JLS draws distinctive-shape gates by
hand with no reference to #43/#44. A "rectangular (IEC) symbols" render
mode is a small, self-contained, *genuinely standards-conformant*
feature with real pedagogical value — European curricula teach IEC
symbols, and JLS today can only draw the ANSI ones. Nothing else in this
document is that cheap for that much conformance.

There is deliberately **no standard** for truth tables, Karnaugh maps,
ASM charts, or state-transition diagrams as drawn in teaching tools.
JLS's state-machine and truth-table elements are therefore in an
un-standardized space — which is fine, and worth saying out loud so
nobody goes looking.

---

## 4. Tier 3 — Verification, assertions, and coverage

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 48 | IEEE **1800.2 UVM** | IEEE / Accellera | Universal Verification Methodology class library | OTHER |
| 49 | SystemVerilog **Assertions** (within 1800) | IEEE | Temporal assertion language | OTHER |
| 50 | IEEE **1850 PSL** | IEEE | Property Specification Language (VHDL/Verilog-neutral) | OTHER |
| 51 | IEEE **1647** `e` | IEEE | Specman verification language | OTHER |
| 52 | Accellera **PSS** (Portable Test & Stimulus) | Accellera | Retargetable test intent | OTHER |
| 53 | Accellera **UCIS** | Accellera | Unified coverage database interchange | OTHER |
| 54 | Accellera **SCE-MI** | Accellera | Co-emulation modelling interface | OTHER |
| 55 | **OSVVM** | — (de facto) | VHDL verification methodology | OTHER |
| 56 | **UVVM** | — (de facto) | VHDL verification methodology | OTHER |
| 57 | **VUnit** | — (de facto) | VHDL/SV unit-test framework | OTHER |
| 58 | **cocotb** | — (de facto) | Python co-simulation testbenches | ADJACENT |
| 59 | **VPI / PLI** (1364/1800 Annexes) | IEEE | C simulator API | OTHER |
| 60 | **VHPI** (1076) | IEEE | VHDL C API | OTHER |
| 61 | **DPI-C** (1800) | IEEE | SystemVerilog direct programming interface | OTHER |
| 62 | IEEE **1735** | IEEE | IP encryption & rights management (v2 has known weaknesses) | OTHER |
| 63 | **SMT-LIB 2.6** | SMT-LIB initiative | Solver input language — the formal back end | ADJACENT |
| 64 | **AIGER** / **BTOR2** | — (de facto) | And-inverter graph / word-level model-checking formats | ADJACENT |
| 65 | `riscv-arch-test` / **RISCOF** | RVI | ISA compliance test framework | COULD (for `riscv/`) |

**JLS's position:** it has a verification story (`-t` test vectors,
differential fuzzing in `riscv/fuzz_diff.py`, golden files) that
conforms to *none* of these and shouldn't. The one exception is #65:
`riscv/verify.py` already does differential testing against a reference
emulator, which is what RISCOF formalizes. Running the official
`riscv-arch-test` suite against the JLS-drawn CPU would be a genuinely
novel claim for a teaching tool — and it is the only path to the
certification in §11f.

---

## 5. Tier 4 — Simulation interop and waveform formats

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 66 | **VCD** — IEEE 1364-2001 §18 | IEEE | Value Change Dump | **HAVE** |
| 67 | **Extended VCD (EVCD)** | IEEE 1364 | Adds strength/direction | COULD |
| 68 | **FST** | GTKWave (de facto) | Compressed waveform format; the modern default | COULD |
| 69 | LXT / LXT2 / VZT | GTKWave (de facto) | Legacy compressed waveforms | OTHER |
| 70 | **FSDB** | Synopsys (proprietary) | Verdi waveform database | OTHER |
| 71 | **WLF** | Siemens EDA (proprietary) | Questa waveform database | OTHER |
| 72 | **SAIF** | Synopsys-originated (de facto) | Switching-activity interchange for power analysis | OTHER |
| 73 | **UCDB / coverage DBs** | vendor | Coverage persistence (see UCIS #53) | OTHER |

JLS emits #66 deterministically and documents it as a stability
contract — which is more rigor than most tools in its class. #68 (FST)
is the obvious next step for large traces and is a self-contained
writer, but VCD is universally readable and gzip-friendly, so the
value is modest.

---

## 6. Tier 5 — Synthesis output, netlists, and IP interchange

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 74 | **EDIF** 2 0 0 / 3 0 0 / 4 0 0 (ANSI/EIA-548, EIA-618, EIA-682; IEC 61690) | ANSI/EIA/IEC | The one *formally standardized* netlist interchange format | COULD |
| 75 | **Yosys JSON netlist** | YosysHQ (de facto) | The working import intermediate | **HAVE** (import) |
| 76 | Structural gate-level Verilog netlist | — (de facto) | The universal handoff | HAVE (export) |
| 77 | **BLIF** | Berkeley (de facto) | Logic-interchange format, ABC/VPR | COULD |
| 78 | **EBLIF** | F4PGA/VTR (de facto) | Extended BLIF with carry chains, attributes | OTHER |
| 79 | VPR architecture XML | VTR (de facto) | FPGA architecture description | OTHER |
| 80 | **FASM** (FPGA assembly) | CHIPS Alliance | Textual pre-bitstream representation | OTHER |
| 81 | **PCF** pin constraints | icestorm/nextpnr (de facto) | iCE40 pin assignment | **HAVE** |
| 82 | **XDC** / **QSF** / **LPF** constraints | AMD / Intel / Lattice (de facto) | Vendor pin & timing constraints | ROADMAP (#213 follow-ups) |
| 83 | **JEDEC JESD3-C** fuse-map file | JEDEC | The classic PLD/GAL programming file | COULD |
| 84 | **SVF / XSVF** | Asset InterTech / Xilinx (de facto) | Serial vector format for JTAG programming | OTHER |
| 85 | IEEE **1532** | IEEE | In-system configuration of programmable devices | OTHER |
| 86 | Vendor bitstream formats (+ Project X-Ray / IceStorm / Trellis documentation) | vendor / reverse-engineered | The final FPGA artifact | ADJACENT (#215 delegates) |

The settled stance from `hdl-support-research.md` holds throughout this
tier: **JLS emits and consumes; Yosys/nextpnr/vendor tools do the work.**
#74 (EDIF) deserves one honest note — it is the only *standard* in the
tier, and it is largely dead in practice; adopting it would be
conformance theater. #83 is the opposite: obsolete technology, but a
genuinely delightful teaching artifact (draw logic → burn a GAL22V10).

---

## 7. Tier 6 — Timing, power, libraries, and constraints

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 87 | **Liberty** (`.lib`) | Synopsys (open-published de facto) | Cell timing/power/noise characterization | OTHER |
| 88 | Liberty **CCS / ECSM** | Synopsys / Cadence | Current-source timing models | OTHER |
| 89 | IEEE **1497 SDF** | IEEE | Standard Delay Format — back-annotated delays | COULD |
| 90 | IEEE **1481** (DPCS) incl. **SPEF** | IEEE | Standard Parasitic Exchange Format | OTHER |
| 91 | DSPF / RSPF | Synopsys (de facto) | Detailed/reduced parasitics | OTHER |
| 92 | IEEE **1603 ALF** | IEEE | Advanced Library Format | OTHER |
| 93 | **SDC** | Synopsys (de facto) | Design constraints — the universal timing-intent language | ADJACENT |
| 94 | IEEE **1801 UPF** | IEEE | Power intent: domains, isolation, retention *(verify revision)* | OTHER |
| 95 | Si2 **CPF** | Si2 | Common Power Format (legacy alternative to UPF) | OTHER |
| 96 | IEEE **2416** | IEEE | System power modelling | OTHER |
| 97 | ITF / ICT technology files | vendor (proprietary) | Interconnect/parasitic technology description | OTHER |
| 98 | Si2 **OpenDFM** | Si2 | DFM rule interchange | OTHER |
| 99 | IEEE **1801.1** / power-model extensions *(verify)* | IEEE | UPF application profiles | OTHER |

Nothing in this tier is reachable from a two-state gate simulator with
unit delays, and that is a boundary, not a gap. The single interesting
entry is #89 (SDF): JLS *does* have a delay model
(`docs/simulation-semantics.md`), and consuming SDF would let a drawn
circuit be simulated with realistic cell delays. It is also exactly the
kind of feature that would tempt JLS into re-implementing a timing
engine — so it stays a "COULD" with a warning attached.

---

## 8. Tier 7 — Physical implementation and layout data

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 100 | **LEF** (Library Exchange Format) | Cadence/Si2 (published de facto) | Abstract cell views: pins, blockages, layers | OTHER |
| 101 | **DEF** (Design Exchange Format) | Cadence/Si2 | Placement, routing, floorplan | OTHER |
| 102 | **GDSII Stream Format** | Calma (de facto; never formally standardized) | *The* layout interchange format, ~1978–present | OTHER |
| 103 | **SEMI P39 — OASIS** | SEMI | The standardized GDSII successor | OTHER |
| 104 | **CIF** (Caltech Intermediate Form) | — (legacy de facto) | Early layout format, still in Magic | OTHER |
| 105 | Si2 **OpenAccess** | Si2 | Standard design-database API | OTHER |
| 106 | Si2 **iPDK / OPDK** | Si2 | Interoperable PDK (TCL/PyCell parameterized cells) | OTHER |
| 107 | Foundry DRC/LVS decks: Calibre **SVRF**, Cadence **Pegasus/PVS**, Synopsys **ICV** | vendor (proprietary) | Rule languages — no open standard exists | OTHER |
| 108 | **KLayout DRC DSL**, **Magic `.tech`** | open (de facto) | Open-source rule decks | OTHER |
| 109 | Open PDKs: **SKY130**, **GF180MCU**, **IHP SG13G2** (via `open_pdks`) | foundry/Efabless/Google/IHP | Openly licensed process kits | OTHER |
| 110 | **OpenROAD / OpenLane 2 / ORFS** flow conventions | OpenROAD project (de facto) | RTL→GDSII flow structure | OTHER |
| 111 | **LVS netlist / SPICE netlist** conventions | Berkeley SPICE3 lineage (de facto) | Device-level netlists | OTHER |
| 112 | **IBIS** (ANSI/EIA-656) + **IBIS-AMI** | IBIS Open Forum | I/O buffer behavioural models | OTHER |
| 113 | **Touchstone** (`.sNp`) | IBIS Open Forum | S-parameter interchange | OTHER |

Every row is **OTHER**, and that is the correct and permanent answer.
The nearest JLS ever comes to this tier is by handing a netlist to
Yosys, whose downstream (OpenROAD, KLayout, a foundry) owns all of it.
Naming the tier matters anyway: it is the honest answer to "how far down
could this go," and it is where any "JLS to silicon" fantasy must stop
and delegate.

---

## 9. Tier 8 — Mask data preparation, lithography, and fab

The bottom of the question's range. Included in full because the
boundary is only meaningful if you can see what lies past it.

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 114 | **SEMI P44 — OASIS.MASK** | SEMI | Mask/reticle pattern data as consumed by writers | OTHER |
| 115 | **MEBES** | Etec/Applied (de facto legacy) | The long-dominant mask-writer format | OTHER |
| 116 | JEOL (JBX/VSB) and Nuflare EBM writer formats | vendor (proprietary) | E-beam shot data | OTHER |
| 117 | Multi-beam writer formats (IMS) | vendor (proprietary) | Multi-beam mask writing | OTHER |
| 118 | Curvilinear mask data extensions (OASIS.MASK / MULTIGON work) | SEMI | Curvilinear ILT shapes | OTHER |
| 119 | **MRC** (mask rule check) decks | vendor (proprietary) | Manufacturability of the mask itself | OTHER |
| 120 | **OPC / RET / ILT** recipes (Calibre, Proteus, Tachyon) | vendor (proprietary) | Resolution enhancement; no open standard | OTHER |
| 121 | SEMI photomask **P-series** (e.g. P37 EUV substrates, plus blank/substrate specs) *(verify individual numbers)* | SEMI | Mask blanks, substrates, defect specs | OTHER |
| 122 | SEMI **E5 (SECS-II)**, **E30 (GEM)**, **E37 (HSMS)** | SEMI | Fab equipment communication | OTHER |
| 123 | SEMI **E87 / E90 / E94 / E84** | SEMI | Carrier, substrate, job, handoff management | OTHER |
| 124 | SEMI **E164** and "Interface A" (E120/E125/E132/E134) | SEMI | Equipment data acquisition / EDA (the *other* EDA) | OTHER |
| 125 | SEMI **E142** / **G85** wafer maps | SEMI | Die-level result mapping | OTHER |
| 126 | SEMI **M1** and wafer specifications | SEMI | Silicon wafer geometry/quality | OTHER |
| 127 | SEMI **S2 / S8** | SEMI | Equipment safety and ergonomics | OTHER |
| 128 | **IRDS** (successor to ITRS) | IEEE | Not a standard — the roadmap the tier plans against | OTHER |

Two observations worth keeping. First, the field *inverts* as you
descend: the top is IEEE-standardized and openly published, the bottom
is SEMI-standardized plus vendor-proprietary, and the middle
(Liberty/SDC/GDSII) runs on de facto formats owned by companies.
Second, **"lithography mask design" is not reachable by an incremental
extension of a schematic editor** — the intervening tiers (synthesis,
place & route, extraction, DRC/LVS, OPC) each represent a distinct
discipline with a distinct tool class. The correct architecture for the
whole range is not one program; it is what the field already built,
namely a chain of programs exchanging the formats above.

---

## 10. Tier 9 — Test, DFT, packaging, board, and reliability

| # | Standard | Body | Governs | Rel. |
|---|---|---|---|---|
| 129 | IEEE **1149.1** JTAG + **BSDL** | IEEE | Boundary scan; BSDL describes the chain | COULD (teaching value) |
| 130 | IEEE 1149.4 | IEEE | Mixed-signal boundary scan | OTHER |
| 131 | IEEE 1149.6 | IEEE | AC-coupled net testing | OTHER |
| 132 | IEEE 1149.7 (cJTAG) | IEEE | Reduced-pin debug/test | OTHER |
| 133 | IEEE 1149.10 | IEEE | High-speed test access | OTHER |
| 134 | IEEE **1500** | IEEE | Embedded core test wrapper | OTHER |
| 135 | IEEE **1687** (IJTAG) + 1687.1/1687.2 | IEEE | Instrument access networks | OTHER |
| 136 | IEEE **1838** | IEEE | 3D-IC / die-stack test access | OTHER |
| 137 | IEEE **1450 STIL** (+1450.1, **1450.6 CTL**) | IEEE | Test pattern & core-test description | OTHER |
| 138 | **WGL** | TSSI/Synopsys (de facto) | Waveform generation language for ATE | OTHER |
| 139 | ATE native formats (Advantest, Teradyne) | vendor | Tester programs | OTHER |
| 140 | **IPC-2581** (DPMX) | IPC | Open PCB design data exchange | OTHER |
| 141 | **IPC-D-356A** | IPC | Netlist for bare-board test | OTHER |
| 142 | **IPC-7351** | IPC | Land-pattern (footprint) geometry | OTHER |
| 143 | **IPC-2221 / 2222** | IPC | Generic PCB design | OTHER |
| 144 | **IPC-A-610** / **J-STD-001** | IPC | Assembly acceptability and soldering | OTHER |
| 145 | **Gerber X2/X3** (RS-274X) | Ucamco | PCB fabrication data | OTHER |
| 146 | **ODB++** | Siemens EDA (published) | PCB manufacturing data model | OTHER |
| 147 | JEDEC package outlines (JC-11, MO-/MS- series) | JEDEC | Mechanical package definitions | OTHER |
| 148 | JEDEC **JESD47**, **JEP122** | JEDEC | Qualification and failure mechanisms | OTHER |
| 149 | **AEC-Q100 / Q101** | AEC | Automotive IC/discrete qualification | OTHER |
| 150 | ANSI/ESDA/JEDEC **JS-001** (HBM), **JS-002** (CDM), **JESD78** (latch-up) | ESDA/JEDEC | ESD and latch-up robustness | OTHER |
| 151 | **MIL-STD-883**, **MIL-PRF-38535** (QML) | US DoD | Military-grade test methods and qualification | OTHER |
| 152 | **ECSS-Q-ST-60-02C** | ECSS/ESA | ASIC/FPGA development for space | OTHER |
| 153 | IEC 60747 series | IEC | Semiconductor device standards | OTHER |

#129 is the single teaching-relevant entry: a boundary-scan register
chain is a *drawable circuit*, and BSDL is a small declarative format.
An "add boundary scan to your design" lab is within JLS's reach; nothing
else in this tier is.

---

## 11. Tier 10 — The standards the tool itself is built on

This is where JLS's actual conformance surface lives, and it is the
largest tier. Splitting it out prevents the common error of treating
"standards in the logic design space" as EDA-only: for a distributed,
installable, network-capable, accessible teaching tool, these are the
standards that generate real obligations.

### 11.1 File formats, encoding, and data

| # | Standard | Body | Rel. |
|---|---|---|---|
| 154 | **ISO/IEC 10646** / Unicode | ISO/Unicode | HAVE |
| 155 | **RFC 3629** UTF-8 | IETF | HAVE |
| 156 | **RFC 8259** / ECMA-404 JSON | IETF/Ecma | HAVE (Yosys netlist reader) |
| 157 | **W3C XML 1.0** | W3C | ADJACENT (IP-XACT, board files) |
| 158 | **ISO/IEC 15948** PNG (W3C PNG 3rd ed.) | ISO/W3C | HAVE |
| 159 | **ISO/IEC 10918** JPEG | ISO/IEC | HAVE |
| 160 | **W3C SVG 1.1 / SVG 2** | W3C | HAVE |
| 161 | **ZIP** (PKWARE APPNOTE; ISO/IEC 21320-1 profile) | PKWARE/ISO | HAVE (legacy `.jls`) |
| 162 | **XZ / LZMA2** file format | Tukaani | HAVE (current `.jls`) |
| 163 | **ISO 8601-1** date/time | ISO | HAVE |
| 164 | **IEEE 754-2019** | IEEE | ADJACENT |
| 165 | **IEC 60027-2** binary prefixes (KiB/MiB) | IEC | COULD (memory sizing UI) |

### 11.2 CLI, documentation, and release conventions

| # | Standard | Body | Rel. |
|---|---|---|---|
| 166 | **IEEE 1003.1 POSIX** (Issue 8, 2024) — utility syntax, exit status | IEEE/Open Group | HAVE |
| 167 | GNU Coding Standards (CLI/`--help` conventions) | GNU | HAVE |
| 168 | **RFC 2119 / RFC 8174** requirement keywords | IETF | HAVE |
| 169 | **SemVer 2.0.0** | — | HAVE |
| 170 | Keep a Changelog | — | HAVE |
| 171 | **SPDX license identifiers** / REUSE | Linux Foundation | COULD (REUSE headers) |

### 11.3 Desktop, packaging, and distribution

| # | Standard | Body | Rel. |
|---|---|---|---|
| 172 | freedesktop **Desktop Entry Specification** | freedesktop.org | HAVE |
| 173 | freedesktop **shared-mime-info** | freedesktop.org | HAVE (`application/x-jls-circuit`) |
| 174 | **XDG Base Directory Specification** | freedesktop.org | COULD (prefs location) |
| 175 | **AppStream** metainfo | freedesktop.org | COULD (needed for Flathub/software centres) |
| 176 | **OCI** Image / Distribution / Runtime specs | OCI | HAVE |
| 177 | **Debian Policy Manual** | Debian | HAVE |
| 178 | RPM / Fedora packaging guidelines | Fedora | HAVE |
| 179 | **AppImage** specification | AppImage project | HAVE |
| 180 | Windows Installer (MSI) | Microsoft | HAVE |
| 181 | **Authenticode** code signing | Microsoft | HAVE (via SignPath) |
| 182 | Apple codesign / notarization | Apple | Declined by recorded decision (#128, #135) |
| 183 | **Nix flakes** | NixOS (de facto) | HAVE |
| 184 | **Wayland** protocol + `wlroots`/xdg-shell | freedesktop.org | HAVE (`WLToolkit` path) |
| 185 | **X11 / ICCCM / EWMH** | X.Org/freedesktop | HAVE (XToolkit path) |

### 11.4 Supply chain, provenance, and security process

| # | Standard | Body | Rel. |
|---|---|---|---|
| 186 | **Reproducible Builds** / `SOURCE_DATE_EPOCH` | Reproducible Builds | HAVE (jar + BOM) |
| 187 | **SPDX** — ISO/IEC 5962:2021 | ISO/IEC | COULD (second SBOM format) |
| 188 | **CycloneDX** — ECMA-424 | Ecma | HAVE |
| 189 | **SLSA** v1.x build levels | OpenSSF | HAVE (provenance attestations) |
| 190 | **in-toto** attestation framework | in-toto/CNCF | HAVE |
| 191 | **Sigstore** / cosign keyless signing | OpenSSF | HAVE |
| 192 | **NIST SP 800-218 SSDF** | NIST | COULD (self-attestation) |
| 193 | **NIST SP 800-161** supply-chain risk | NIST | OTHER |
| 194 | NTIA **SBOM minimum elements** | NTIA/CISA | HAVE (satisfied by #188) |
| 195 | **CVE** / **CVSS v4.0** / **CWE** | MITRE/FIRST | ADJACENT |
| 196 | **OSV** vulnerability schema | OpenSSF | ADJACENT |
| 197 | **ISO/IEC 29147** (disclosure) / **30111** (handling) | ISO/IEC | COULD (`SECURITY.md` already close) |
| 198 | **OpenSSF Scorecard** | OpenSSF | HAVE |

### 11.5 Cryptography (the collaboration stack, #163/#168)

| # | Standard | Body | Rel. |
|---|---|---|---|
| 199 | **FIPS 197** AES | NIST | HAVE |
| 200 | **NIST SP 800-38D** GCM | NIST | HAVE |
| 201 | **FIPS 180-4** SHA-2 | NIST | HAVE |
| 202 | **FIPS 198-1** HMAC | NIST | HAVE |
| 203 | **RFC 5869** / SP 800-56C HKDF | IETF/NIST | HAVE |
| 204 | **RFC 7748** X25519 | IETF | HAVE |
| 205 | **RFC 8032** Ed25519 (+ FIPS 186-5) | IETF/NIST | HAVE |
| 206 | **RFC 8446** TLS 1.3 (handshake shape, nonce construction) | IETF | HAVE (pattern, not protocol) |
| 207 | **Noise Protocol Framework** | — (de facto) | ADJACENT (the design's other reference point) |
| 208 | **RFC 9000** QUIC / **RFC 8445** ICE / **RFC 5389** STUN | IETF | COULD (P2P NAT traversal, #168 follow-on) |

### 11.6 Accessibility and human factors

| # | Standard | Body | Rel. |
|---|---|---|---|
| 209 | **WCAG 2.2** (and ISO/IEC 40500 for 2.0) | W3C/ISO | ADJACENT (a desktop app is not a web page, but it is the benchmark everyone cites) |
| 210 | **EN 301 549** | ETSI/CEN/CENELEC | **COULD** — EU public-sector procurement |
| 211 | **Section 508** (US Rehabilitation Act) | US Access Board | **COULD** — US public-university procurement |
| 212 | ADA Title II web/app rule (2024) | US DOJ | ADJACENT — drives #211 for public universities |
| 213 | **Java Accessibility API** | Oracle/OpenJDK | HAVE |
| 214 | **AT-SPI2** / **IAccessible2** / **UIA** bridges | freedesktop / Microsoft | COULD (verification only) |
| 215 | **VPAT / ACR** (Accessibility Conformance Report) | ITI | COULD — see §12 |

### 11.7 Software engineering process and quality

| # | Standard | Body | Rel. |
|---|---|---|---|
| 216 | **ISO/IEC 25010** product quality model | ISO/IEC | ADJACENT |
| 217 | **ISO/IEC/IEEE 12207** software lifecycle | ISO/IEC/IEEE | OTHER |
| 218 | **ISO/IEC/IEEE 15288** system lifecycle | ISO/IEC/IEEE | OTHER |
| 219 | **ISO/IEC/IEEE 29119** software testing | ISO/IEC/IEEE | OTHER |
| 220 | **ISO/IEC 5055** automated source code quality measures | ISO/IEC | OTHER |
| 221 | **Java Language / VM Specifications**, JEP process, JPMS (JEP 261) | Oracle/OpenJDK | HAVE |
| 222 | **1EdTech (IMS) LTI 1.3** / **QTI** / **Caliper** | 1EdTech | COULD — autograder↔LMS integration |

---

## 12. Certification and conformity regimes

Standards you conform to; these you are *assessed against*. This section
answers the "certifications" half of the question directly.

### 12.a Tool qualification — the one that applies to EDA tools themselves

If a design tool's output ends up in a safety-critical product, the
*tool* must be qualified. This is the only place a program like JLS
could itself be "certified."

| # | Regime | Domain | Mechanism |
|---|---|---|---|
| 223 | **ISO 26262-8 §11** software tool qualification (TI/TD → **TCL1–3**) | Automotive | Tool confidence level from impact × detection; qualification by validation, process, or dev-process compliance |
| 224 | **ISO 26262** ASIL A–D (incl. part 5, hardware; part 11, semiconductors) | Automotive | Product-level functional safety |
| 225 | **IEC 61508-3 §7.4.4** offline support tool classes **T1/T2/T3** | Industrial | Class-based tool justification |
| 226 | **IEC 61508** SIL 1–4 | Industrial | Product-level functional safety |
| 227 | **DO-330** tool qualification (**TQL-1..5**) | Airborne | Applied with DO-178C (software) and DO-254 (hardware) |
| 228 | **DO-254** Design Assurance Levels A–E | Airborne electronic hardware | The FPGA/ASIC certification path |
| 229 | **DO-178C** (+ DO-331/332/333 supplements) | Airborne software | — |
| 230 | **EN 50128 / EN 50657** tool classes T1/T2/T3; **EN 50129** | Rail | — |
| 231 | **IEC 62304** (+ ISO 13485, IEC 60601) | Medical devices | Tool validation under a QMS |
| 232 | **IEC 61513 / IEC 60880** | Nuclear I&C | — |
| 233 | **ISO 25119 / EN ISO 13849 / IEC 62061** | Agricultural & machinery safety | — |
| 234 | **MIL-STD-882E** | US defense system safety | — |
| 235 | **IEEE 2851** functional-safety data interchange *(verify revision)* | Cross-domain | Interoperable safety artifacts |
| 236 | TÜV SÜD / TÜV Rheinland / exida **tool certificates** | The commercial instrument | How #223/#225/#227 are actually evidenced |

**The honest verdict for JLS:** #223/#225/#227 are *reachable in
principle* — a deterministic, reproducible, golden-tested batch tool
with a documented interface is unusually well-positioned on the
evidence side — and *wrong in practice*. Qualification costs a
multi-year audited process, a maintained safety manual, and a
commercial support commitment. A single-maintainer GPLv3 teaching tool
should state plainly, in writing, that it is **not qualified for
safety-critical use**, which is a cheaper and more useful contribution
than pursuing the certificate. That statement is currently absent from
`SECURITY.md` and `README.md` and is a genuine documentation gap.

### 12.b Organizational / quality management

| # | Regime | Scope |
|---|---|---|
| 237 | **ISO 9001** | Quality management system |
| 238 | **IATF 16949** | Automotive QMS |
| 239 | **ISO/IEC 27001** | Information security management |
| 240 | **ISO/IEC 20000-1** | IT service management |
| 241 | **Automotive SPICE** (ISO/IEC 33061) | Process capability assessment |
| 242 | **CMMI** | Process maturity |
| 243 | **OpenChain — ISO/IEC 5230** | Open-source license compliance program |
| 244 | **OpenChain Security Assurance — ISO/IEC 18974** | Open-source security assurance program |

### 12.c Product security certification

| # | Regime | Scope |
|---|---|---|
| 245 | **Common Criteria — ISO/IEC 15408** (+ **18045** methodology, EAL1–7) | Security evaluation of IT products, incl. hardware |
| 246 | **SOG-IS** / CC recognition arrangements | Mutual recognition |
| 247 | **FIPS 140-3** (CMVP) | Cryptographic module validation |
| 248 | **NIST CAVP** | Cryptographic algorithm validation |
| 249 | **SESIP** | IoT platform security evaluation |
| 250 | **PSA Certified** | Arm-ecosystem device security |
| 251 | **GlobalPlatform** / **EMVCo** | Secure element & payment certification |
| 252 | **IEC 62443** | Industrial automation security (product + process) |
| 253 | **EU Cyber Resilience Act (CRA)** conformity | Regulatory; obligations phase in 2026–2027, with an open-source "steward" category |
| 254 | **ETSI EN 303 645** / UK PSTI | Consumer IoT security baseline |
| 255 | **US EO 14028 / CISA secure-software self-attestation** | Federal procurement |

**Live relevance to JLS:** #253 is the only regulatory regime that could
plausibly reach a project like this. The CRA's open-source provisions
turn on whether the software is monetized and who "stewards" it; a
free, unmonetized, single-maintainer educational project is at the
lightest end, but the *evidence* it already produces (SBOM, provenance,
security policy, coordinated disclosure) is exactly the CRA's expected
artifact set. Worth a short recorded stance rather than a project.

### 12.d Accessibility conformance

| # | Regime | Scope |
|---|---|---|
| 256 | **VPAT → ACR** (Accessibility Conformance Report) | The standard vendor self-assessment artifact; what universities ask for |
| 257 | **EN 301 549 declaration** | EU public procurement |
| 258 | **Section 508 conformance** | US federal & most public-university procurement |

This is the certification family with the **highest real-world
probability of being demanded of JLS**, because its users are
universities and universities procure. `docs/keyboard-a11y-verification.md`
is already most of the evidence base for a VPAT.

### 12.e Design-conformance programs (what a *design* gets certified for)

| # | Program | Scope |
|---|---|---|
| 259 | **RISC-V compatibility / certification** (RVI) + `riscv-arch-test`/RISCOF | ISA conformance and trademark use |
| 260 | **Arm SystemReady** / Architecture Compliance Kits | Arm ecosystem conformance |
| 261 | **USB-IF certification** | USB logo program |
| 262 | **PCI-SIG compliance program** | PCIe interoperability |
| 263 | **Bluetooth SIG qualification** | Bluetooth listing |
| 264 | **Wi-Fi Alliance certification** | WLAN interoperability |
| 265 | **HDMI / DisplayPort (VESA) compliance** | Display interfaces |
| 266 | **Ethernet interoperability testing (UNH-IOL)** | Ethernet |
| 267 | **JEDEC compliance / memory validation** | Memory devices |
| 268 | **OpenPOWER / OpenCAPI compliance** | OpenPOWER ecosystem |

### 12.f Manufacturing, sourcing, and trust

| # | Regime | Scope |
|---|---|---|
| 269 | **DMEA Trusted Foundry accreditation** | US defense-trusted fabrication |
| 270 | **ISO/IEC 20243 (O-TTPS)** | Trusted technology provider / anti-tamper supply chain |
| 271 | **SAE AS6171 / AS5553** | Counterfeit electronic part detection & avoidance |
| 272 | **ITAR / EAR** export control | Regulatory constraint on design data |
| 273 | **ISO 14001 / ISO 45001** | Environmental / occupational H&S at fabs |
| 274 | **RoHS / REACH / WEEE** | Substance and disposal compliance |

### 12.g Education and curriculum

| # | Regime | Scope |
|---|---|---|
| 275 | **ABET** EAC/CAC accreditation criteria | Accredits the *programs* that use JLS; drives lab-outcome requirements |
| 276 | **ACM/IEEE-CS CS2023** | Computer science curricular guidelines (architecture & organization knowledge area) |
| 277 | **ACM/IEEE-CS CE2016** | Computer engineering curriculum — digital design is a core area |
| 278 | **IEEE/ACM Computing Curricula** series | Umbrella curricular framework |

### 12.h Personnel certifications in the space

| # | Certification | Holder |
|---|---|---|
| 279 | **IPC CID / CID+** | PCB designers |
| 280 | **IPC-A-610 CIS/CIT** | Assembly inspectors/trainers |
| 281 | **ASQ CQE / CRE** | Quality & reliability engineers |
| 282 | **ISTQB** | Test engineers |
| 283 | **CISSP / CSSLP** | Security professionals |
| 284 | **NCEES PE (Electrical & Computer)** | Licensed professional engineers |
| 285 | Vendor certifications (AMD/Xilinx, Intel/Altera, Cadence, Synopsys, Siemens EDA) | Tool specialists |
| 286 | **Linux Foundation / OpenSSF** certifications | Open-source practitioners |
| 287 | **OpenSSF Best Practices Badge** (passing/silver/gold) | *Projects*, not people — the one badge JLS could earn this month |

### 12.i Foundry and EDA-ecosystem certification — how EDA tools actually get certified

**This is the certification regime that governs the logic-design industry
itself**, and the first revision of this document omitted it entirely —
an error of framing (see §0's note). When a design tool is described as
"certified" in this field, it almost never means ISO or IEEE; it means a
*foundry* has certified that tool, at a named version, against a named
process node and PDK version. The programs are the real gatekeepers of
the EDA market.

| # | Program | Body | What is certified | Rel. |
|---|---|---|---|---|
| 288 | **TSMC OIP — EDA Alliance: Individual Tool Certification (ITC)** | TSMC | A single tool at a specific version against a specific process node/PDK | OTHER |
| 289 | **TSMC OIP — Integrated Tool Flow (ITF)** | TSMC | A multi-tool flow working together on a node | OTHER |
| 290 | **TSMC OIP — Reference Flow (RF)** | TSMC | A complete, published, node-specific design flow | OTHER |
| 291 | **TSMC OIP — IP Alliance** | TSMC | Silicon-verified, foundry-specific IP catalogue membership | OTHER |
| 292 | **TSMC9000** IP quality program | TSMC | IP quality: documentation, shuttle silicon validation, assessment results published on TSMC-Online. TSMC's own derivative of ISO 9000 | OTHER |
| 293 | **TSMC OIP — Cloud Alliance** | TSMC | Certified cloud environments running RTL-to-GDSII and custom flows (AWS, Azure, Cadence, Synopsys as inaugural members) | OTHER |
| 294 | **TSMC OIP — Design Center Alliance (DCA)** | TSMC | Design-service providers | OTHER |
| 295 | **TSMC OIP — Value Chain Alliance (VCA)** | TSMC | Independent design-service companies taking designs to production | OTHER |
| 296 | **TSMC OIP — 3DFabric Alliance** | TSMC | Advanced-packaging ecosystem enablement | OTHER |
| 297 | **Samsung SAFE™** (Samsung Advanced Foundry Ecosystem) | Samsung Foundry | The umbrella program: PDKs, design methodologies, IP | OTHER |
| 298 | **Samsung SAFE-QEDA** (Qualified EDA) | Samsung Foundry | EDA tools *and full flows* certified per process node (e.g. a complete flow certified for 4LPP) | OTHER |
| 299 | **Samsung SAFE Cloud Alliance** / SAFE IP | Samsung Foundry | Certified cloud design environments; qualified IP | OTHER |
| 300 | **Intel Foundry EDA/IP alliance** — per-node tool qualification & IP readiness | Intel Foundry | Tool qualification disclosed per node family (e.g. Intel 18A) | OTHER |
| 301 | **Intel Foundry Accelerator / Chiplet Alliance** | Intel Foundry | Chiplet and packaging ecosystem enablement | OTHER |
| 302 | GlobalFoundries, UMC, SMIC, Rapidus ecosystem/partner programs *(specifics unverified in this pass)* | respective foundries | Equivalent per-foundry tool and IP enablement | OTHER |
| 303 | **PDK certification and versioning** per node | foundries | The artifact every program above hinges on; a tool is certified against a *PDK version*, not a process in the abstract | OTHER |
| 304 | **Efabless / ChipIgnite shuttle acceptance**, `open_pdks`, OpenROAD flow conformance | Efabless / Google / open community | The open-silicon analogue: acceptance criteria for a shuttle tapeout rather than a commercial certification | ADJACENT |

**Why every row is OTHER, and why that is not a dodge.** These programs
are structurally closed to a project like JLS, for reasons that have
nothing to do with quality: they require an NDA, PDK access under that
NDA, a named tool version submitted for assessment against a named node,
and *re-certification on every node and PDK revision* — a permanent
commercial obligation, not a one-time badge. They are also the sharpest
possible illustration of the tier boundary this document draws: the
certification that matters in this industry attaches to tools that
consume a PDK, and JLS neither has nor could have one. #304 is the only
row a GPLv3 project can reach, and it is reached by *emitting a netlist
into somebody else's open flow* — exactly the delegation stance the rest
of this document defends.

---

## 13. What JLS should plausibly take on — ranked

Filtered from everything above by: does it serve students or the
maintainer, does it fit the single-jar/single-maintainer constraint, and
is it a real conformance claim rather than theater?

**Two axes, kept apart.** The first revision of this section ranked
everything on one list and let *cheap and achievable* outrank *relevant
to logic design* — which put a general open-source badge second, above
ISA compliance. They are now separated: §13.1 is conformance in the
logic-design space, §13.2 is conformance the project owes as a piece of
distributed software. Both are real; only the first answers the question
this document was written to answer.

### 13.1 Logic-design conformance

1. **IEEE 91/91a + IEC 60617-12 symbol conformance (#43, #44).** A
   rectangular/IEC symbol render mode alongside the current
   distinctive-shape gates. Self-contained, GUI-only, real pedagogical
   value (European curricula), and the only entry in this whole document
   where JLS would go from "draws something gate-shaped" to "conforms to
   the symbol standard."
2. **`riscv-arch-test` / RISCOF against the `riscv/` CPU (#65, #259).**
   Turns "we built a CPU in a logic simulator" into "we built a CPU that
   passes the official ISA compliance suite." Extends existing
   differential-testing machinery rather than inventing anything, and it
   is the only entry in this document that would let JLS make a
   *conformance claim about a design*, not about itself.
3. **XDC/QSF/LPF constraint emitters (#82).** Already roadmapped as
   #213 follow-ups; each is a small printer over the existing port walk.
4. **EVCD or FST waveform output (#67, #68).** Only if trace size or
   strength/direction information becomes a real complaint.
5. **IP-XACT export for subcircuits (#4).** Speculative but structurally
   free: a JLS subcircuit already carries exactly IP-XACT's payload.
   Demand-gated, like #212.

### 13.2 Institutional and project conformance — real, but not logic design

These generate genuine obligations for a tool distributed to
universities, and several are cheaper than anything in §13.1. They are
listed **separately and second** because cheapness is not relevance:
they belong to Tier 10 and §12, not to the logic-design stack, and the
first revision of this document wrongly let low cost promote them up a
single merged ranking.

1. **VPAT/ACR + Section 508 / EN 301 549 statement (#256–#258).** The
   evidence exists (`docs/keyboard-a11y-verification.md`); the artifact
   does not. This is the certification most likely to be *asked for* by
   an actual institutional user.
2. **A written "not qualified for safety-critical use" scope statement
   (§12.a).** Zero cost, removes a real liability ambiguity, and is the
   correct answer to #223/#225/#227 rather than pursuing them.
3. **OpenSSF Best Practices Badge (#287).** Days of work; the project
   already satisfies most criteria (documented contribution process,
   security policy, reproducible builds, static analysis, test suite).
   The most cheaply earned certification available — and, to be explicit
   about its standing, a *general open-source* badge with no bearing on
   logic design, which is why it sits here rather than in §13.1.
4. **A CRA stance paragraph (#253).** Recorded decision, not a project.
5. **SPDX SBOM alongside CycloneDX (#187), REUSE headers (#171),
   AppStream metainfo (#175), XDG base directories (#174).** Small,
   independent housekeeping items with clear consumers.

### 13.3 Deliberately not recommended

SDF consumption (#89) — the first step onto a timing-engine slope; EDIF
(#74) — conformance to a dead format; anything in Tiers 6–9 beyond #129;
any form of safety-tool qualification (§12.a); and every foundry program
in §12.i, which is closed to this project by NDA and PDK access rather
than by any judgment about merit.

---

## 14. The tally

| Section | Entries | IDs |
|---|---|---|
| Tier 1 — system & interface | 24 | 1–24 |
| Tier 2 — languages & schematic notation | 23 | 25–47 |
| Tier 3 — verification & coverage | 18 | 48–65 |
| Tier 4 — waveform & sim interop | 8 | 66–73 |
| Tier 5 — netlist & synthesis interchange | 13 | 74–86 |
| Tier 6 — timing, power, libraries | 13 | 87–99 |
| Tier 7 — physical implementation | 14 | 100–113 |
| Tier 8 — mask, lithography, fab | 15 | 114–128 |
| Tier 9 — test, packaging, board, reliability | 25 | 129–153 |
| Tier 10 — the tool's own software standards | 69 | 154–222 |
| §12.a–h — certification & conformity regimes | 65 | 223–287 |
| §12.i — foundry & EDA-ecosystem certification | 17 | 288–304 |
| **Total named entries** | **304** | |

By relevance to JLS:

| Mark | Count | Reading |
|---|---|---|
| **HAVE** | 51 | Already conformed to — dominated by Tier 10 |
| **ROADMAP** | 1 | #82, vendor constraint formats |
| **COULD** | 30 | §13 ranks the worthwhile subset |
| **ADJACENT** | 22 | Interoperate, never implement |
| **OTHER** | 134 | Owned by other tool classes |
| *(declined)* | 1 | #182, Apple notarization — recorded decision #128/#135 |
| *(unmarked)* | 65 | §12.a–h certification regimes, which take a different reading |

Counts are of rows in the tables above and are mechanically checkable
(`grep -E '^\| [0-9]+ \|' docs/standards-landscape.md`); they sum to 304.

Three conclusions the numbers make hard to miss:

1. **JLS already conforms to more standards than most of its peers, and
   almost none of them are EDA standards.** Files, CLI, packaging,
   supply chain, crypto, and accessibility account for the large
   majority of the 51 HAVE entries. This is what shipping a real
   cross-platform installable program costs, and it is invisible from
   inside the EDA framing of the question.
2. **The EDA standards JLS holds are precisely the interchange formats
   at its own boundary** — VCD out, Verilog/VHDL out, Yosys JSON in, PCF
   out. That is the correct footprint for a tool whose settled stance is
   *orchestrate external tools, never reimplement their semantics*, and
   it is exactly the shape `docs/hdl-support-research.md` argued for
   independently.
3. **The descent from "highest level design space" to "lithography mask
   design" is not one program's ladder.** It is roughly six tool classes
   (entry → verification → synthesis → P&R → verification/extraction →
   mask data prep), each with its own standards culture, and the
   handoffs between them are exactly the formats catalogued above. The
   most useful thing a schematic-first educational simulator can do
   about the bottom of that stack is emit a clean netlist and get out of
   the way — which JLS does.
4. **"Certified" in this industry usually means a foundry said so.**
   Of the 82 certification entries, the 17 in §12.i are the ones that
   actually gate participation in commercial logic design — and they
   certify a *tool version against a PDK version*, renewably, under NDA.
   Every general-purpose regime in §12.a–h (safety, security, QMS,
   accessibility, supply chain) is real but peripheral to how an EDA
   tool earns its standing. A survey that omits §12.i answers the
   standards question while missing the certification one.

---

## 15. Sources and how to verify

Standards numbers and scopes here come from the bodies' own catalogues
(IEEE SA, Accellera, SEMI, JEDEC, IPC, Si2, OMG, W3C, IETF, NIST, ISO)
and, for the entries JLS already implements, from this repository's own
code and docs as cited in §1.

§12.i was added in revision 2 from the foundry programs' own material
and industry reporting: TSMC's OIP pages
(<https://www.tsmc.com/english/dedicatedFoundry/oip>, and the EDA, IP,
Cloud, Design Center, Value Chain and 3DFabric alliance pages beneath
it — the ITC/ITF/RF certification vocabulary and the TSMC9000 IP
quality program), Samsung Foundry's SAFE™ and SAFE-QEDA pages
(<https://semiconductor.samsung.com/foundry/safe/>), and Intel Foundry's
per-node EDA/IP qualification announcements. The `tsmc.com` and
`semiwiki.com` pages returned HTTP 403 to automated fetching during this
pass; their content was taken from search-index text and secondary
reporting, so the ITC/ITF/RF and alliance-membership details should be
confirmed against the primary pages in a browser before being relied on.
Row #302 (GlobalFoundries, UMC, SMIC, Rapidus) was not researched
beyond confirming such programs exist. Entries marked *(verify revision)* were
not re-checked against the publishing body in this pass. Anything
load-bearing for a decision — particularly the §12.a tool-qualification
clauses and the #253 CRA obligations — should be read in the primary
document before it is relied on; this survey is a map, not a legal or
compliance opinion.
