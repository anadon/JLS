# The EDA Landscape and JLS's Place In It — Competitive Analysis and Compatibility Matrix

*July 2026. A survey of circuit-design, simulation, PCB-layout, FPGA, and
silicon tooling — free and commercial — built to answer a specific
stakeholder question: could JLS "compete with the likes of Xilinx, Intel,
Texas Instruments, and TSMC"? Produced with a multi-agent web-research
harness (three parallel search angles: educational logic simulators;
schematic/SPICE/PCB tools; HDL/synthesis/FPGA/ASIC/foundry flows). Facts are
current as of 2026-07 and cited; version numbers, prices, and release states
are moving targets. Prices for enterprise EDA tools are quote-only and are
given as order-of-magnitude class ranges, flagged where they come from
resellers/analysts rather than vendor list pages. This document is
orientation and strategy input, not a normative contract like
[`batch-interface.md`](batch-interface.md) or [`file-format.md`](file-format.md).*

*Companion: [`hdl-support-research.md`](hdl-support-research.md) already
covers the HDL export/import feasibility in verified depth; its verdicts are
summarized here (the "on-ramp" thesis), not re-litigated.*

---

## 0. The one-paragraph answer

JLS is an **educational, schematic-first, gate-level digital logic
simulator**. Its only standards-based points of contact with the industrial
stack are **VCD waveform export** and **structural Verilog-2005 export**.
Within its actual category — teaching digital logic by drawing circuits — JLS
is a credible peer of Logisim-Evolution, hneemann's *Digital*, CircuitVerse,
and Deeds, and it holds genuine advantages (a stable batch/autograde CLI,
VCD + structural-Verilog export, a real RV32I CPU expressible in its stock
elements). It **does not compete with**, and is not trying to be, AMD/Xilinx
Vivado or Intel/Altera Quartus (FPGA implementation suites), the
Cadence/Synopsys/Siemens "big three" (full ASIC design and sign-off), TI's
role (a chip vendor shipping SPICE models and free front-ends), or **TSMC** —
which is not a software company at all but the world's largest silicon
**foundry**. Asking whether a logic simulator competes with a foundry is a
category error, spelled out in §2. The correct and achievable framing is
**"on-ramp to,"** not **"competitor of,"** the professional flow — and JLS's
existing Verilog/VCD exports already make it a real on-ramp.

---

## 1. The four things "circuit design tool" can mean

The stakeholder's four named companies sit in four *different industries*,
which is the root of the confusion. Untangling them is the whole analysis.

| Layer | What it is | Named example | JLS relation |
| --- | --- | --- | --- |
| **Design capture + logic** | Draw/describe a design; simulate its logic | (this is JLS's layer, at the *educational* end) | **JLS lives here**, at gate level, for teaching |
| **Implementation (FPGA)** | Synthesize → place-and-route → bitstream for a programmable chip | **AMD/Xilinx** (Vivado), **Intel/Altera** (Quartus) | JLS can *feed* these via Verilog export; does not replace them |
| **Implementation (ASIC) + sign-off** | RTL → GDSII, timing/power/DRC/LVS sign-off for custom silicon | Cadence, Synopsys, Siemens EDA | Entirely outside JLS's scope |
| **Manufacturing (foundry)** | Physically fabricate wafers from a GDSII + PDK | **TSMC**, Intel Foundry, GlobalFoundries, Samsung | Not software at all — see §2 |

**Texas Instruments** is a fifth thing again: a **semiconductor vendor** that
gives away simulation front-ends (TINA-TI, PSpice for TI, WEBENCH) loaded
with models of *its own parts*, as sales enablement — not an EDA-platform
business (§6C).

The rest of this document surveys each layer, then returns in §7–8 to the
honest competitive verdict and what JLS could realistically do to strengthen
its position.

---

## 2. Why a tool does not "compete with TSMC" (the category error, made precise)

This is the crux of the stakeholder question, so it gets its own section.

Chip creation separates cleanly into **three distinct businesses**:

1. **EDA tools** — the *software* that designs a chip (Cadence, Synopsys,
   Siemens EDA; or, open-source, Yosys + OpenROAD + Magic + KLayout).
2. **The PDK (Process Design Kit)** — the *data* a foundry hands designers so
   the tools can target one specific manufacturing process: design rules
   (DRC), device SPICE models, standard-cell timing/power (**Liberty `.lib`**),
   physical abstracts (**LEF/DEF**), and layer maps. Real leading-edge PDKs
   (e.g. TSMC's) are **NDA-protected trade secrets** tightly bound to the
   commercial tools.
   ([open-pdks](https://github.com/fossi-foundation/open-pdks))
3. **The foundry** — the *physical fab* that turns a finished layout (GDSII)
   plus a PDK into silicon wafers: **TSMC**, Intel Foundry, GlobalFoundries,
   Samsung Foundry, UMC, SMIC.

TSMC is a **pure-play foundry**: a contract manufacturer that fabricates
silicon designed by *other* companies (Apple, NVIDIA, AMD…). It sells
**manufacturing capacity**, not software. In 2025 the pure-play foundry
market hit ~$165B with **TSMC at ~70% overall and >90% at the leading edge
(3nm/2nm)**.
([Counterpoint, 2025](https://counterpointresearch.com/en/insights/post-insight-research-briefs-blogs-global-pureplay-semiconductor-foundry-revenues-to-grow-17-yoy-in-2025-driven-by-ai-highperformance-computing-chips))

So a logic simulator relates to TSMC the way **a spell-checker relates to a
paper mill**, or **MS Paint relates to a printing press**: adjacent in a
mental picture of "making a document/chip," but in different industries with
no product overlap. JLS lives *upstream of even EDA layer 1* — it is a
front-end teaching and verification aid. There is no version of JLS that
competes with a foundry, and that is not a deficiency to fix; it is a
category boundary.

The **open-silicon movement** (§6E) is the one place this boundary softens —
foundry-backed *open* PDKs (SkyWater SKY130, GF180MCU, IHP SG13G2) plus the
open tool flow (OpenLane/OpenROAD) let hobbyists and universities tape out
real chips. Even there, JLS is not a participant: that flow starts from RTL
Verilog and needs synthesis, place-and-route, DRC/LVS, and a PDK — none of
which JLS has or is trying to have.

---

## 3. JLS's actual category: educational digital logic simulators

These are JLS's real peers — the tools it is meaningfully compared against.
All are gate-level/schematic-first teaching simulators. The interop columns
use JLS's two portable outputs: **structural Verilog-2005** and **VCD**.

| Tool | Vendor / status | License / cost | Platform | HDL export | VCD / waveform | Autograde / batch | Verilog interop w/ JLS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| **JLS** (this project) | Poplawski / anadon fork; active | **GPLv3, free** | Java desktop; headless batch; jar/container | **Structural Verilog-2005** (roadmap: VHDL) | **VCD export** | **Stable CLI `-t`/`-b`, VCD, image, container** | — (source) |
| **Logisim-Evolution** | community org; active, v4.x (2025) | GPLv3, free | Java desktop | **VHDL + Verilog** via per-component generators | Chronogram viewer; standard `.vcd` export **[unconfirmed]** | Test-vector CLI (`--test-vector`, `--tty`, `--substitute`) | **Yes** — both emit structural Verilog |
| **Digital** (hneemann) | H. Neemann; active | GPLv3, free | Java desktop | **VHDL + Verilog** (as an FPGA deployment vehicle) | Waveform/measurement export; `.vcd` specifically **[unconfirmed]** | **Built-in test-case component** (best-in-class) | **Yes** — structural Verilog |
| **CircuitVerse** | circuitverse.org; very active | Open source (MIT sim), free hosted | **Browser** | **Verilog** in/out (2025 multi-file import, testbenches) | In-browser waveforms; `.vcd` file **[unconfirmed]** | Web-hosted assignment suite | **Yes** — Verilog |
| **Deeds** | Univ. Genova; maintained | Freeware (closed) | Windows (Wine elsewhere) | **VHDL only** | none noted | FSM + microcomputer teaching flow | Weak — VHDL, not Verilog |
| **Falstad / CircuitJS** | Falstad / Sharp; active | GPLv2, free | Browser | none | animated only, no VCD | none | **None** |
| **HADES** | Univ. Hamburg; dormant/applet-era | free (academic) | Java (applet legacy) | none confirmed | own waveform viewer | none | **None** |
| **Logisim** (original) | C. Burch; **discontinued 2014** | GPL, free | Java desktop | none | none | none | **None** |
| **LogicWorks 5** | DesignWorks; legacy commercial | **Commercial** (textbook bundles) | Win/macOS | VHDL sim (closed) | — | — | Weak/none |
| **NI Multisim** | NI/Emerson; sold | **Commercial**; student ed. | Windows | VHDL for Digilent FPGA | SPICE/analog focus | — | Weak — VHDL |
| **Proteus VSM** | Labcenter; active | **Commercial**, perpetual | Windows | none (MCU firmware co-sim) | mixed-mode SPICE | — | **None** (different domain) |

**Reading of this table.** JLS's *differentiators* inside the category are
its **stable, documented batch/autograde interface** (a compatibility
contract — [`batch-interface.md`](batch-interface.md)) and its
**VCD + structural-Verilog** exports feeding the open HDL toolchain. Its
*catch-up items* versus the strongest peers (Digital, Logisim-Evolution) are
the well-known ones from [`hdl-support-research.md`](hdl-support-research.md)
§4: **board-aware HDL export** (pin-constraint files for named cheap dev
boards) and an **in-editor test panel**. Note that several "waveform" tools
render signals but do **not** necessarily write standard `.vcd` files — that
was the survey's most-flagged uncertainty and is worth confirming before any
"interoperates with X" claim ships.

---

## 4. Adjacent world A: schematic capture + analog/SPICE simulation

This is a *different domain* from JLS (analog/continuous, not gate-level
digital), included so the boundary is explicit. The universal interchange
here is the **SPICE netlist**; the one format that overlaps a digital logic
simulator is **VCD** (ngspice and most mixed-signal SPICE tools can emit VCD
for digital nodes).

| Tool | Vendor / status | License / cost | Platform | Role | Digital overlap |
| --- | --- | --- | --- | --- | --- |
| **ngspice** | community; active (v44/45) | mostly BSD, free | Win/mac/Linux | The open **SPICE engine** (embedded by KiCad, Qucs-S, Xschem) | Can emit **VCD** for digital nodes |
| **KiCad — Eeschema** | KiCad/CERN; very active (9.0, 2025) | GPLv3, free | Win/mac/Linux | Schematic + integrated ngspice + full PCB | SPICE; no gate-level digital |
| **LTspice** | Analog Devices; active (26.0, Jan 2026) | Free (proprietary) | Win/mac (Wine) | Best-known free SPICE (SMPS/power) | none |
| **Qucs-S** | ra3xdh; active (24.4, 2024) | GPL, free | Win/mac/Linux | Schematic front-end → ngspice/Xyce/SPICE OPUS | via backends |
| **Xschem** | S. Schippers; active | GPL, free | Linux/mac/Win | IC-grade schematic → SPICE; the **SKY130 analog front-end** | Verilog/VHDL netlist out |
| **gEDA → Lepton-EDA** | **gEDA dead 2025**; Lepton active | GPL, free | Linux/mac | Schematic + broad netlist backends (EDIF/Verilog/VHDL/SPICE) | Verilog/EDIF export |
| **TINA-TI** | DesignSoft/TI; maintained | Free (TI-restricted) | Windows | TI-model SPICE front-end | none |
| **PSpice / PSpice for TI** | Cadence (+TI); active | Commercial; **TI ed. free, no node limit** | Windows | Reference analog SPICE; "PSpice models" are an industry standard | mixed-signal |
| **Micro-Cap 12** | Spectrum; **freeware since 2019, frozen** | Free (unsupported) | Windows | Full analog/digital/mixed, now free | real mixed-mode |
| **SIMetrix/SIMPLIS** | SIMetrix; active | Commercial (quote) | Win/Linux | SPICE + piecewise-linear for SMPS/control loops | — |
| **Cadence Virtuoso / Spectre** | Cadence; flagship | Commercial (enterprise, quote) | Linux | **Custom analog IC** design + sign-off SPICE | Verilog-AMS |
| **Synopsys HSPICE / PrimeSim** | Synopsys; flagship | Commercial (enterprise, quote) | Linux | Golden-reference SPICE for IC sign-off | Verilog-A/AMS |

---

## 5. Adjacent world B: PCB layout

Also a different domain (physical board design, not logic). The manufacturing
format hierarchy is **Gerber RS-274X + Excellon** (universal baseline) →
**IPC-2581 / ODB++** (intelligent complete data; ODB++ stewarded by Siemens,
IPC-2581 championed by Cadence + the IPC consortium). JLS produces **none** of
these — it has no PCB concept at all.

| Tool | Vendor / status | License / cost | Platform | Manufacturing out |
| --- | --- | --- | --- | --- |
| **KiCad — Pcbnew** | KiCad/CERN; very active | GPLv3, free | Win/mac/Linux | Gerber, Excellon, **IPC-2581, ODB++ (v9)**, STEP |
| **EasyEDA** | JLCPCB/LCSC; active | **Free** (fab-monetized) | Browser + desktop | Gerber, BOM, pick-and-place; direct-to-JLCPCB |
| **LibrePCB** | project; very active (2.0, Jan 2026) | GPLv3, free | Win/mac/Linux | Gerber, Excellon, IPC-2581, ODB++, STEP |
| **Horizon EDA** | L. Kramer; active | GPLv3, free | Linux/Win | Gerber, Excellon, ODB++, STEP |
| **Fritzing** | community; revived (1.0.5, 2025) | Open source (~$8 build funding) | Win/mac/Linux | Gerber, SVG, BOM (breadboard-first, education) |
| **DipTrace** | Novarm; active | **Perpetual $75–$995**; free 300-pin/2-layer | Windows (Wine) | Gerber, Excellon, ODB++, IPC-2581 |
| **Autodesk Eagle** | Autodesk; **EOL 7 June 2026** | Subscription (ending) | Win/mac/Linux | **Migrate off** → Fusion/KiCad/Altium |
| **Fusion Electronics** | Autodesk; active | Subscription; free hobby/edu tier | Win/mac | Gerber, IPC-2581 (Eagle's successor) |
| **Altium Designer** | Altium (Renesas); active | **~$4.5k–7.5k/seat/yr**; "Develop" ~$1,990/yr **[reseller figures]** | Windows | Gerber, ODB++, IPC-2581, IPC-356, STEP |
| **OrCAD X** | Cadence; active (24.1) | Commercial (quote) | Windows | Gerber, IPC-2581, ODB++ (+ PSpice) |
| **Allegro X** | Cadence; flagship | Commercial (enterprise, ~$20k+ class) | Win/Linux | Gerber, IPC-2581, ODB++ (high-speed/SI) |
| **PADS / Xpedition** | Siemens EDA; active | Commercial (quote) | Win/(Linux) | Gerber, **ODB++**, IPC-2581 (SI/PI, PLM ties) |
| **Proteus** | Labcenter; active | Commercial, perpetual (~$6.5k full **[single-source]**) | Windows | Gerber, ODB++ (+ MCU firmware co-sim) |

---

## 6. The industrial digital stack (the "Xilinx/Intel" layers, plus ASIC and foundry)

### 6A. HDL simulation & synthesis — where JLS's Verilog/VCD actually land

| Tool | Role | License | Key formats | JLS touch point |
| --- | --- | --- | --- | --- |
| **Icarus Verilog** | event-driven Verilog sim | GPLv2 | in: Verilog; out: **VCD**, FST | **Consumes JLS's structural Verilog directly** |
| **Verilator** | compiled 2-state (System)Verilog sim; fastest FOSS | LGPLv3/Artistic | in: (S)Verilog; out: C++/SystemC, **VCD**, FST | Runs JLS's Verilog |
| **GHDL** | VHDL simulator (+ Yosys front-end) | GPLv2 | in: VHDL; out: **VCD**, GHW | future VHDL-export target |
| **GTKWave / Surfer** | waveform viewers | GPL / MPL-EUPL | in: **VCD**, FST, GHW | **Open JLS's VCD directly** |
| **cocotb** | Python testbench framework driving a sim | BSD | drives Icarus/Verilator/GHDL/… | could drive JLS-exported Verilog under Icarus |
| **Yosys** | the FOSS **RTL synthesis** framework | ISC | in: Verilog; out: EDIF/BLIF/**JSON**/netlist; reads Liberty `.lib` | JLS's roadmap import path (§7); JLS output is *downstream* of synthesis |
| **Questa/ModelSim** | industry-standard SV/VHDL/UVM sim | Siemens, commercial | Verilog/VHDL/SV, SDF, WLF | JLS Verilog would load, but this is pro verification |
| **VCS** / **Xcelium** | sign-off simulators | Synopsys / Cadence, commercial | + SDF, FSDB/SHM | not a JLS target |
| **Design/Fusion Compiler**, **Genus** | ASIC synthesis | Synopsys / Cadence, commercial | Verilog, **Liberty**, SDC | JLS produces neither Liberty nor SDC |

**The key insight for the matrix:** JLS emits a *structural* (already
gate-level) netlist — that is the *output* of synthesis, not its input. So
JLS sits **conceptually downstream** of Yosys/Design Compiler/Genus, not in
competition with them. Its VCD and structural Verilog are genuine,
standards-based bridges into the open toolchain (Icarus, Verilator, GTKWave,
Surfer) — this is the one area of real, useful interoperability, and it
already works today.

Market context: Synopsys (~31%), Cadence (~30%), and Siemens EDA (~13%)
together hold **>90%** of a ~$16.7B (2024) → ~$18.3B (2025) EDA market.
([ChipXpert](https://chipxpert.in/cadence-vs-synopsys-vs-siemens-eda/))

### 6B. FPGA toolchains — the "Xilinx" and "Intel" layer

These take RTL → **synthesis → place-and-route → bitstream** for a specific
programmable device. JLS **cannot** target any real FPGA (no P&R, no
bitstream); it can only *feed* these tools a Verilog netlist, exactly as
Digital and Logisim-Evolution do.

- **AMD/Xilinx — Vivado** (current **2025.2**; 7-series/UltraScale/Versal).
  The old free "WebPACK" is now **Vivado ML Standard Edition** (permanently
  free, limited device subset); Enterprise is paid. **2026.1 (June 2026)
  moves to a new tiered model** ("Vivado BASIC" free subscription + paid
  tiers), with the free tier reportedly **pulled from Linux** — verify at
  publication.
  ([AMD licensing](https://www.amd.com/en/products/software/adaptive-socs-and-fpgas/vivado/vivado-licensing-options.html),
  [Slashdot, May 2026](https://hardware.slashdot.org/story/26/05/23/1917255/amd-xilinx-is-excluding-linux-from-the-free-tier-for-its-fpga-dev-tool))
  Formats: Verilog/VHDL/SV in; EDIF; **XDC** constraints; `.bit` bitstream;
  SDF out. Legacy **ISE** (14.7, 2013) lingers for Spartan-6/CoolRunner.
- **Intel/Altera — Quartus Prime.** *Ownership change:* Intel sold **51% of
  Altera to Silver Lake**, closed **Sep 2025** — so this is now really
  **Altera**, "largest pure-play FPGA" again.
  ([CNBC](https://www.cnbc.com/2025/04/14/intel-to-sell-51percent-stake-in-altera-to-silver-lake.html))
  Editions: **Pro** (Agilex/Stratix 10, paid), **Standard** (paid),
  **Lite** (free; MAX 10, Cyclone). Formats: Verilog/VHDL in; **QSF**
  constraints; `.sof/.pof`; SDC; SDF.
- **Lattice — Diamond / Radiant.** Free license covers many popular parts
  (MachXO/O2, ECP5 without SERDES); subscription for the rest.
- **Open-source FPGA flow:** **Yosys** (synth) + **nextpnr** (P&R) +
  **Project IceStorm** (iCE40) / **Project Trellis** (ECP5) — the first fully
  open flows, producing real loadable bitstreams for *smaller* FPGAs.
  **F4PGA** (ex-SymbiFlow; 7-series, momentum reportedly slowed — verify),
  **OpenFPGA** (custom fabrics), **apio** (turnkey wrapper), **Amaranth**
  (Python HDL driving both open and vendor tools). These trail Vivado/Quartus
  badly on large modern devices, timing closure, and IP — but they are the
  natural downstream for a teaching tool's Verilog on a cheap iCE40/ECP5
  board. ([nextpnr](https://github.com/YosysHQ/nextpnr))

### 6C. Texas Instruments — a chip vendor, not an EDA vendor

TI supplies **free simulation front-ends and SPICE models** to sell its
silicon, rather than selling EDA software:

- **PSpice for TI** — free full-featured Cadence PSpice edition; **no node
  limit**; 5,700+ TI models (only limit: 3 simultaneous measurements with
  non-TI models). TI's current recommended simulator.
  ([TI](https://www.ti.com/tool/PSPICE-FOR-TI))
- **TINA-TI** — free TI edition of DesignSoft's TINA (older offering).
- **WEBENCH** — browser design/calculation tools for TI power/system parts.

So TI belongs in the matrix as a **model/IP + free-simulation provider**
(alongside analog vendors), *distinct from* EDA-platform vendors. JLS has no
overlap with TI's business, and TI ships nothing that competes with JLS.

### 6D. ASIC / open-silicon tools (the layer below FPGAs)

RTL → **GDSII** with timing/DRC/LVS sign-off. Entirely outside JLS's scope,
listed for completeness of the mental model.

| Tool | Role | License | Formats |
| --- | --- | --- | --- |
| **OpenLane / OpenLane 2** | automated **RTL→GDSII** flow orchestrator | Apache-2.0 | Verilog, LEF/DEF, Liberty, SDC, **GDSII** |
| **OpenROAD** | the open RTL-to-GDS engine (floorplan→route→sign-off) | BSD-3 | LEF/DEF, Liberty, SDC, GDSII |
| **Magic** | classic VLSI layout + DRC + extraction | permissive | GDSII, LEF/DEF, SPICE, MAG |
| **KLayout** | high-performance layout viewer/editor + DRC/LVS | GPLv3 | **GDSII, OASIS**, DXF, LEF/DEF |
| **Netgen** | LVS netlist comparison | permissive | SPICE netlists |
| Cadence / Synopsys / Siemens | commercial sign-off (Innovus, ICC2, Calibre…) | commercial | the tools that actually tape out advanced nodes |

### 6E. Open PDKs and the MPW-shuttle model (where the boundary softens)

- **Open PDKs:** **SkyWater SKY130** (130nm, the first foundry-backed open
  PDK), **GlobalFoundries GF180MCU** (180nm), **IHP SG13G2** (130nm SiGe
  BiCMOS, RF/mmWave) — all Apache-2.0, now under CHIPS Alliance.
  ([open-pdks](https://github.com/fossi-foundation/open-pdks),
  [IHP-Open-PDK](https://github.com/IHP-GmbH/IHP-Open-PDK))
- **The shuttle model** pools many small designs onto one multi-project wafer
  to share mask/fab cost. **eFabless (chipIgnite / Tiny Tapeout host) shut
  down in 2025**, stranding hundreds of designs; recovery is real but
  fragmented across **ChipFoundry.io**, **wafer.space**, a Cadence-run SKY130
  shuttle, and **IHP** shuttles (a European open-shuttle path; IHP taped out
  in May 2025).
  ([Tom's Hardware](https://www.tomshardware.com/tech-industry/semiconductors/efabless-shuts-down-fate-of-tiny-tapeout-chip-production-projects-unclear),
  [Zero-to-ASIC 2025](https://www.zerotoasiccourse.com/post/year_update_2025/))

Even here JLS is not a participant: the flow begins at RTL Verilog and needs
synthesis + P&R + DRC/LVS + a PDK. This is the frontier a *future* on-ramp
could point students toward — not a place JLS competes.

---

## 7. The interchange-format matrix — what JLS speaks

The blunt summary of every table above, from JLS's point of view. This is the
honest scorecard of interoperability.

| Format | What it's for | JLS today | Who consumes/produces it |
| --- | --- | --- | --- |
| **VCD** | digital waveform dump | **Exports** ✓ | GTKWave, Surfer, every HDL sim, ngspice (digital nodes) |
| **Structural Verilog-2005** | gate-level netlist | **Exports** ✓ | Icarus, Verilator, Yosys, Vivado/Quartus (as netlist) |
| **VHDL** | HDL netlist | roadmap (§ below) | GHDL, Vivado, Quartus, Deeds/Multisim emit it |
| **PNG/JPEG/SVG** | circuit image | **Exports** ✓ | slides, lab reports, docs |
| Yosys **JSON netlist** | synthesized netlist | roadmap *import* only | Yosys ↔ yosys2digitaljs pattern |
| EDIF / BLIF | netlist interchange | ✗ | synthesis/FPGA tools |
| Liberty `.lib` / SDC / SDF | timing/power/constraints | ✗ (not applicable) | synthesis, STA, sign-off |
| LEF/DEF / **GDSII** / OASIS | physical layout | ✗ (not applicable) | ASIC P&R, layout, foundry |
| Gerber / Excellon / IPC-2581 / ODB++ | PCB manufacturing | ✗ (not applicable) | PCB tools, board houses |
| SPICE netlist / Verilog-A(MS) | analog | ✗ (different domain) | SPICE tools, analog IC |
| FPGA bitstream (`.bit/.sof/.jed`) | device programming | ✗ | Vivado/Quartus/nextpnr |
| PDK | process data | ✗ (category boundary) | foundry ↔ EDA tools |

**Two green rows, and they matter.** VCD and structural Verilog are exactly
the entry-level lingua franca of the open digital toolchain. Everything below
them is either a *different domain* (analog, PCB), a *later stage* JLS
deliberately doesn't do (synthesis, timing, layout, bitstream), or a
*category boundary* (PDK/foundry). This picture is not a to-do list — most of
those ✗ rows should *stay* ✗, because filling them would turn a focused
teaching tool into a bad clone of Vivado. The green rows are where the
leverage is.

---

## 8. Honest competitive verdict

**Where JLS legitimately competes (and can win):** the **educational
gate-level logic simulator** category — against Logisim-Evolution, Digital,
CircuitVerse, and Deeds. Here JLS is a real contender with a distinct profile:
a **stable, documented batch/autograde CLI** (a compatibility contract most
peers lack), **VCD + structural-Verilog** export into the open toolchain,
reproducible byte-for-byte jar/BOM builds, and a demonstrated ability to
express **a real RV32I CPU in stock elements** (`riscv/`). Its known gaps
versus the strongest peers are bounded and already on the roadmap:
**board-aware HDL export** and an **in-editor test panel**
([`hdl-support-research.md`](hdl-support-research.md) §4).

**Where JLS does not compete — and shouldn't try to:**

- **AMD/Xilinx Vivado, Intel/Altera Quartus** — FPGA *implementation* suites
  (synthesis + P&R + bitstream). JLS can *feed* them Verilog; it cannot and
  should not replace them. The realistic ambition is to make JLS's Verilog
  land cleanly on a **cheap open-flow board (iCE40/ECP5 via Yosys+nextpnr)**,
  the way Digital and Logisim-Evolution already do.
- **Cadence / Synopsys / Siemens EDA** — full ASIC design and sign-off, a
  >$16B market they hold >90% of. Different universe.
- **Texas Instruments** — a chip vendor giving away SPICE front-ends; no
  product overlap in either direction.
- **TSMC** — a **foundry**, not a software company (§2). No tool competes
  with a fab.

**The correct framing is "on-ramp," not "competitor."** JLS's genuine
strategic value is that a student draws a circuit, simulates it, and then —
via VCD and structural Verilog — sees that same design run in Icarus, open in
GTKWave/Surfer, and (with board-aware export) blink an LED on a real $30 FPGA.
That path from "drawing on a screen" to "logic on real hardware" is
pedagogically powerful and **entirely achievable** without JLS pretending to
be an FPGA suite, an ASIC flow, or a foundry.

---

## 9. Recommended positioning and next steps

Grounded in JLS's existing roadmap ([`hdl-support-research.md`](hdl-support-research.md) §6)
and this survey:

1. **Adopt the "deployment vehicle, not HDL tutorial" framing explicitly**
   (hneemann's honest line for Digital). Ship structural Verilog + VHDL
   export validated in CI against `iverilog`/`ghdl` — already the recorded
   Stage-1 plan.
2. **Board-aware export** is the single highest-leverage catch-up item: emit a
   pin-constraint file (XDC/QSF/PCF) for a small set of cheap named boards, so
   the exported Verilog is one `nextpnr`/vendor command away from a
   bitstream. This is what makes the on-ramp *exciting* ("your drawing is now
   on real hardware") and what both leading peers already do.
3. **Prefer the open flow** (Yosys → nextpnr → iCE40/ECP5) as the documented
   downstream, avoiding a hard dependency on a commercial suite whose free
   tier is itself in flux (Vivado's 2026 changes).
4. **In-editor test panel** — an HDL-independent UX win that closes the gap to
   Digital's built-in test-case component; JLS's batch `-t` engine already
   does the work headlessly.
5. **Document the interop truthfully** (the §7 matrix): "JLS speaks VCD and
   structural Verilog-2005; it is an on-ramp to the open FPGA toolchain, not a
   replacement for Vivado/Quartus, and has no relationship to ASIC sign-off or
   silicon foundries." Truthful scope is a feature for an educational tool —
   it sets correct student expectations and avoids the category error this
   document exists to prevent.
6. **Leave the ✗ rows in §7 as non-goals.** No PDK, no GDSII, no PCB, no
   bitstream generation, no analog SPICE. Each would dilute the teaching
   mission and put JLS into a market it cannot serve.

---

## Sources

Educational simulators: [Logisim-Evolution releases](https://github.com/logisim-evolution/logisim-evolution/releases),
[hneemann/Digital](https://github.com/hneemann/Digital),
[CircuitVerse GSoC 2025](https://blog.circuitverse.org/posts/vivek_kumar_gsoc2025_finalreport/),
[Deeds](https://www.digitalelectronicsdeeds.com/deeds.html),
[Carl Burch Logisim](https://cburch.com/logisim/),
[sharpie7/circuitjs1](https://github.com/sharpie7/circuitjs1),
[DigitalJS paper](https://dl.acm.org/doi/10.1145/3375258.3375272).

Schematic/SPICE/PCB: [KiCad 9.0](https://www.kicad.org/blog/2025/02/Version-9.0.0-Released/),
[LTspice](https://www.analog.com/en/resources/design-tools-and-calculators/ltspice-simulator.html),
[Altium Develop pricing](https://www.altium.com/develop/pricing),
[Autodesk Eagle EOL](https://www.autodesk.com/products/fusion-360/blog/future-of-autodesk-eagle-fusion-360-electronics/),
[PSpice for TI](https://www.ti.com/tool/PSPICE-FOR-TI),
[EasyEDA pricing](https://easyeda.com/page/pricing),
[Qucs-S](https://ra3xdh.github.io/),
[Lepton-EDA](https://github.com/lepton-eda/lepton-eda),
[DipTrace](https://diptrace.com/buy/online-store/),
[LibrePCB 2.0](https://github.com/LibrePCB/LibrePCB/releases),
[Micro-Cap 12](https://spectrum-soft.com/),
[Proteus pricing](https://www.labcenter.com/pricing/),
[Cadence OrCAD X / Allegro X 24.1](https://community.cadence.com/cadence_blogs_8/b/pcb/posts/cadence-orcad-x-and-allegro-x-24-1-is-now-available).

HDL/FPGA/ASIC/foundry: [Verilator releases](https://github.com/verilator/verilator/releases),
[Yosys commercial/Tabby](https://yosyshq.net/yosys/commercial.html),
[cocotb simulator support](https://docs.cocotb.org/en/stable/simulator_support.html),
[Vivado licensing (AMD)](https://www.amd.com/en/products/software/adaptive-socs-and-fpgas/vivado/vivado-licensing-options.html),
[Vivado 2026 free-tier change (Slashdot)](https://hardware.slashdot.org/story/26/05/23/1917255/amd-xilinx-is-excluding-linux-from-the-free-tier-for-its-fpga-dev-tool),
[Quartus editions (Intel)](https://www.intel.com/content/dam/www/central-libraries/us/en/documents/quartus-prime-compare-editions-guide.pdf),
[Intel sells Altera (CNBC)](https://www.cnbc.com/2025/04/14/intel-to-sell-51percent-stake-in-altera-to-silver-lake.html),
[nextpnr](https://github.com/YosysHQ/nextpnr),
[F4PGA docs](https://f4pga.readthedocs.io/en/latest/),
[OpenLane](https://github.com/The-OpenROAD-Project/openlane),
[open-pdks](https://github.com/fossi-foundation/open-pdks),
[IHP-Open-PDK](https://github.com/IHP-GmbH/IHP-Open-PDK),
[eFabless shutdown (Tom's Hardware)](https://www.tomshardware.com/tech-industry/semiconductors/efabless-shuts-down-fate-of-tiny-tapeout-chip-production-projects-unclear),
[Zero-to-ASIC 2025](https://www.zerotoasiccourse.com/post/year_update_2025/),
[foundry revenue 2025 (Counterpoint)](https://counterpointresearch.com/en/insights/post-insight-research-briefs-blogs-global-pureplay-semiconductor-foundry-revenues-to-grow-17-yoy-in-2025-driven-by-ai-highperformance-computing-chips),
[EDA market share (ChipXpert)](https://chipxpert.in/cadence-vs-synopsys-vs-siemens-eda/).

*Uncertainty flags carried from the research pass: standard `.vcd` file export
(vs. in-app waveform rendering) in Digital / Logisim-Evolution / CircuitVerse
is unconfirmed; enterprise EDA seat pricing is quote-only (class estimates);
Vivado's 2026.1 free-tier/Linux policy and F4PGA 7-series momentum are
evolving — verify before citing as settled.*
