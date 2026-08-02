# 09 — THE FORMAT ADOPTION PLAN

*The maintainer's question, answered as a path and a cost.*

**Anchored to HEAD `b299d63`.** Every JLS claim below carries `file:line` and was
re-run as a command this session; the verification log is §12. Inputs: `BRIEF.md`
§§11–13 (binding, including **D8** — the "orchestrate, never reimplement" stance
is revoked — and **D9** — one CS→ECE→EE trajectory), the six family surveys
(`fmt-spice.md`, `fmt-kicad-geda.md`, `fmt-hdl-netlist.md`,
`fmt-waveform-verif.md`, `fmt-mrcs-and-accessible.md`, `fmt-data-model-delta.md`),
`docs/standards-adoption/**` (11 numbered docs + README + OPEN-QUESTIONS),
`docs/standards-landscape.md` (304 entries), `docs/capability-roadmap/README.md`
+ `AMENDMENT.md` (P1–P13), `sweep-03`, `sweep-06`, `docs/hdl-support-research.md`,
`docs/vcd-interop.md`, `docs/file-format.md`, and the shipped code under
`src/jls/hdl/**`.

**This is not new scope.** `docs/standards-landscape.md` already surveyed 304
standards and `docs/standards-adoption/` already priced eleven. This document is
that same programme *pursued harder, sorted on one axis, and put in an order*.

---

## 1. THE THESIS

**Adopt the formats; let other people's tools be the views.** There are three
ways to reach a capability, not two: *build an engine* (own the computation),
*orchestrate a subprocess* (shell out at runtime), or **speak the format** —
write or read an open text file that is a pure function of the model. Mode 3 is
categorically cheaper than the other two and the maintainer's list is made
almost entirely of it: it needs **no engine** (the consumer computes), **no
runtime dependency and no toolchain install** (the artifact is a file; the
external tool is a *test-scope* dependency and a docs recipe — `src/` contains
**zero** `ProcessBuilder` while `test/` contains **fifteen**, all behind one
skip-when-absent `ToolLocator`), **no determinism break** (a deterministic text
printer is byte-identical by construction, unlike any external floating-point
solver), **no `jls.edit` code** (nothing here touches the ~37%-of-bundle,
~26%-covered editor package), and **no draw on the coverage commons** (every
emitter is a fresh, easily-100%-covered leaf package that *raises* the JaCoCo
package aggregate rather than spending it). The reframe this unlocks is that
**a view does not have to be a JLS canvas**: KiCad becomes the PCB view,
GTKWave and Surfer the waveform views, OpenSTA the power view, netlistsvg the
schematic renderer, DigitalJS a shareable browser simulator, Yosys the
technology mapper, and iverilog/Verilator/GHDL differential oracles — and
**this is already how the subsystem works in shipped code**: one `HdlModel`
walk feeds three renderers in three unrelated syntaxes, because
`PcfEmitter.emit` iterates `model.ports()` at `PcfEmitter.java:73,155,190`, the
*same* port list `VerilogEmitter` walks at `:93,110` and `VhdlEmitter` at
`:123,1007`. The projection thesis is not a proposal. It ships. What has never
been written is the *rest of the projections*.

---

## 2. THE SORTING PRINCIPLE, AND THE THREE TIERS IT PRODUCES

> **Format adoption is CHEAP where the format is a projection of data JLS
> already has, and EXPENSIVE where it requires NEW PRIMARY DATA.**

Applied format-by-format across all six families, the axis produces three bins,
not two, and the middle one carries the plan:

| Tier | Definition | Examples | Verdict shape |
|---|---|---|---|
| **A** | **No new primary data.** The datum is in the model or in the `.jls` file today and is merely unprojected, underived, or undecided. Cost is code, policy and test-pinning | hierarchy, Yosys JSON, gEDA symbols, switching activity, net names, export metadata | **cheap-now** |
| **B** | **New primary data, but BOUNDED.** One scalar per circuit, four strings, an enum member, an element pin, or a binding the *user* supplies externally. It stops arriving | `INOUT`, timescale, VLNV, reset, port roles, package pins, retire event | **needs-data-first**, 0.5–5 wk each |
| **C** | **Permanent obligation or another tool class.** A library curated forever, a physical quantity JLS's semantics cannot honestly carry, or a geometry/solver engine | device models, footprint libraries, layout geometry, energy, breadboard artwork | **refuse** or **needs-engine** |

The line between B and C is **not size — it is whether the data stops
arriving.** A timescale scalar is written once. A footprint library is never
finished. That is why the shipped `-pins` precedent (`PinBindings.java`, 98
lines: external, user-supplied, all-or-nothing validated) is the correct
mechanism for every Tier-B binding: it converts *"JLS must acquire and maintain
data"* into *"the user supplies data JLS validates."*

**One correction to the brief's own framing, verified.** The briefing predicted
that "hierarchical instance identity" is new primary data. **It is not.**
`SubCircuit.save` writes the entire nested circuit inline
(`src/jls/elem/SubCircuit.java:284-288`, verified — `getSubCircuit().save(output)`),
so every fact hierarchy needs is already in the file and already in the
`Circuit` graph. What is missing is an **IR shape**, an **export-policy
decision**, and a **naming policy**. Hierarchy is Tier A.

---

## 3. THE LEVERAGE TABLE

Every missing datum, ranked by **formats unlocked per maintainer-week**, with
the capability-roadmap program that owns it. "Sufficient" = the format ships
once this lands (given a printer of the stated cost). "Necessary" = the format
cannot ship without it but needs more besides. Costs use the repo's own
calibration (~200–250 lines of shipped-and-tested code per maintainer-week at
the 93.0/92.0/84.5 JaCoCo package aggregate + 80/82 PIT bar, ~1:1 test-to-source),
calibrated against `PcfEmitter` (199 L, 1 golden), `VerilogEmitter` (752 L, 34
goldens) and `HdlExporter` (1,364 L).

| # | Datum | Tier | Weeks | Sufficient for | Necessary for | Suff/wk | Owner program | Started at HEAD? |
|---:|---|:---:|---:|---:|---:|---:|---|---|
| **1** | **RTLIL-shaped netlist → WRITE Yosys JSON** | A | **3–4** | **5** — netlistsvg, DigitalJS, Yosys→EDIF, Yosys→BLIF, Yosys→SPICE | 0 | **1.25–1.67** | **none — unscheduled** | n/a |
| **2** | **Hierarchical instance structure** (uniquified) | A | **4–6** | **3** — hier-Verilog, hier-VHDL, SubCircuit export | **6** — EDIF, BLIF, KiCad `.net`, IP-XACT, Yosys hier-import, SPICE `.subckt` | 0.50–0.75 | **P3** (26–38 wk) | **no** |
| **3** | **Derived symbol geometry + refdes + sequential pin numbers** | A | **0** (enables a 5–8 wk emitter) | **1** — gEDA `.sch`, which back-doors **KiCad 10 + lepton-schematic + lepton-netlist's 30+ backends** | 1 — `.kicad_sch` | — | **none needed** | n/a |
| **4** | **`INOUT` in `HdlModel.Direction`** (IR half only) | B | **1–2** | 0 | **6** — IP-XACT, KiCad `.net`, gEDA bidir, EVCD, BSDL/#129, TT `uio_oe` | 0 (**3.0–6.0 nec/wk — highest in the table**) | **P3** (IR half); P2 owns `BidirPin`; P1 owns honest readback | **no** |
| **5** | **Net-name stability** (`stableId` digest, not save index) | A | **0.5–1** | 0 | **6** — SAIF, SDF, EDIF properties, VCD-vs-external comparison, LEC, name-stable HDL diffs | 0 (**6.0–12.0 nec/wk**) | **effectively unowned**; P3-adjacent | **no** |
| **6** | **Nominal real-time scalar** (one declared unit per circuit) | B | **0.5–1** | 0 | **4** — FST honesty, VCD honesty, SDF, RVVI `TIME` | 0 (4.0–8.0 nec/wk) | **P4** (23–35 wk) | **no** |
| **7** | **Switching activity** (toggle counts T0/T1/TZ/TC) | A | **2–3** | **1** — SAIF → **OpenSTA becomes the power view, with real joules** | 0 | 0.33–0.50 | **P5** (33–50 wk) | **no** |
| **8** | **Subcircuit type identity** (canonical structural digest) | A | **1.5–2** | **1** — deduplicated hier-HDL | **2** — IP-XACT blocker 2, D7's circuit-library format | 0.50–0.67 | **P3** | **no** |
| **9** | **Reset semantics on `Register`** | B | **3–5** | 0 | **4** — FPGA/ASIC-honest HDL, TT `rst_n`, any ASIC netlist, BSDL | 0 (0.8–1.3 nec/wk) | **P2** (20–30 wk); reset *architecture* P13 | **no** |
| **10** | **Package pin numbers + footprint + part value** (mechanism) | B | **2–4** (mechanism; data is the user's) | **1** — KiCad `.net` | **4** — gEDA `footprint=`, IPC-D-356A, Fritzing, BSDL | 0.25–0.50 | **NOBODY** — P6 owns *silicon* cells, not *packages* (§6.1) | n/a |
| **11** | **Port roles** (clock/reset/data/address/valid/ready) | B | **2–3** | 0 | **3** — IP-XACT bus interfaces, SDC/XDC `create_clock`, P13 domain inference | 0 (1.0–1.5) | **P3** | **no** |
| **12** | **Positional pin order per element type** | B | **~1** (≈2 d if `getAllPuts()` is already stable) | 0 | **3** — SPICE `X` calls, KiCad `.net`, EDIF cell binding | 0 (3.0) | **NOBODY** (§6.3). **The only silent-when-wrong item in the study** | n/a |
| **13** | **VLNV** (vendor/library/name/version — four strings) | B | **~1.5** | 0 | **2** — IP-XACT, **and D7's "circuit libraries are DATA" format** | 0 (1.3) | **P3** + D7 | **no** |
| **14** | **Retire event** (one record per retired instruction) | B | **2–4** (+1–2 emitter) | **1** — RVVI-TEXT v0.5 | **1** — this study's own parity contract | 0.17–0.33 | **NOBODY.** There is no P14/P15/P16 (§6.2) | n/a |
| **15** | **Export metadata surfaced** (`renames()`/`warnings()` as data) | A | **~1** | **1** — a machine-readable export report for autograders/CI | 0 | 1.0 | **none — new, tiny, unowned** | n/a |
| **16** | **Design hierarchy in the waveform** (nested `$scope`) | A | **2–4 d** | 1 — a VCD *profile* change | 0 | ~2.0 | none | n/a |
| **17** | **Technology cells as data** (Liberty subset) | C | **10–16** | 0 | **5** — EDIF, SDF, LEF, SPICE `X`-calls, layout — **all also reachable via Yosys** | 0 (0.31–0.50) | **P6** (20–32 wk) | **no** |
| **18** | **Layout geometry** (polygons, layer stack, second coordinate space) | C | **8–12** | **4** — GDSII/OASIS/CIF/DEF read-only | 0 | 0.33–0.50 | **P6**, gated on #17 | **no** |
| **19** | **Drive strength** / **X in the value domain** | C | ≫ (inside P1's 28–36) | **1** — EVCD | 1 — IBIS digital half | ~0 | **P1** | **no** |
| — | Device models, footprint libraries, gate-to-package packing, power/ground, breadboard artwork, energy | C | — | — | — | — | **NOBODY, correctly** | — |

### 3.1 The brief's central prediction, tested

> *"HdlModel has ten statement kinds and NONE instantiates a module … This one
> gap reportedly blocks EDIF, BLIF, KiCad netlist AND subcircuit export
> simultaneously."*

**Verified at HEAD, and the prediction is FALSE as stated — 1 of 4 sufficient.**
(Two corrections in passing: `HdlModel.StatementVisitor` declares **ELEVEN**
`visit` methods, not ten — `HdlModel.java:143-201`, counted; `ShiftStatement`
post-dates `sweep-03`. `README.md:314-318` and `BRIEF` both say ten. The
substantive claim — none instantiates a module — is **confirmed**, as is the
single `moduleName` field at `:891` and the single flat statement list.)

| Claimed | Hierarchy sufficient? | What it *additionally* needs |
|---|:---:|---|
| **SubCircuit export** (the user-visible feature) | **YES, completely** | nothing |
| **EDIF** | **no** | a 6–10 wk bit-level lowering pass **+** a target cell library (datum #17) |
| **BLIF** | **no** | the same 6–10 wk lowering pass |
| **KiCad `.net`** | **no** | footprint + package pins + part value (datum #10) — and without them `pcbnew` refuses every component |

**But the claim also omits what hierarchy *is* sufficient for, and that list is
better**: hierarchical **Verilog-2005** and hierarchical **VHDL-93**, in two
emitters that already ship, are golden-pinned, and are CI-cross-checked against
`iverilog` and `ghdl`. Today a student's 4-bit ALU decomposed into 1-bit slices
**throws** on export (`HdlExporter.java:191-197`). That is the largest single
pedagogical loss at HEAD and hierarchy closes it outright.

**Corrected claim, and it is stronger than the original:**

> Hierarchy is **SUFFICIENT** for three immediately shippable outputs,
> **near-sufficient** for three more at 2–3 wk each (VCD `$scope`, SystemC,
> FIRRTL), and **NECESSARY** for six further formats — and it requires **no new
> primary data at all.**

### 3.2 The split nobody had proposed: uniquified vs deduplicated

`fmt-hdl-netlist.md` costs hierarchy at 6–8 wk and attributes the whole delta
over `sweep-03`'s 3–4 to the **type-identity** problem: `SubCircuit` is a
per-instance deep copy (sharing factor exactly 1.00×), so two instances of "the
same" block may have legitimately diverged and keying modules on the nested
circuit's *name* would silently mis-bind.

**That problem only exists if you deduplicate.** A hierarchy in which every
instance becomes its own uniquely-named module — `top`, `top_u3_alu`,
`top_u3_alu_u1_slice` — is legal Verilog, legal VHDL, and is accepted by
`iverilog`, `ghdl`, Verilator and Yosys. It needs no digest, no collision
policy, no normative note.

| Sub-item | Weeks | In the uniquified increment? |
|---|---:|:---:|
| `InstanceStatement` + multi-module `HdlModel` | 1.5–2 | **yes** |
| Subcircuit type identity (digest + collision naming + doc) | 1.5–2 | **NO** |
| Recursive walk: cycle detection, port→net binding, reject propagation | 1.5–2 | **yes** |
| Emitter work in both printers | 1–1.5 | **yes** |
| Goldens, fixtures, `iverilog`/`ghdl` cross-check, coverage/PIT | 1.5–2 | **yes** (fewer fixtures) |
| **Uniquified total** | **4–6** | |
| **+ deduplication** | **+1.5–2 → 6–8** | |

**Ship uniquified first** (Wave 2), add the digest later (Wave 5). Honest cost of
the split: emitting the deduplicated form later regenerates the hierarchy
goldens a second time — which is *why* net-name stability (datum #5) must land
**before either**. Honest caveat: uniquified export forfeits the "one module
reused N times" lesson that is arguably the point of teaching hierarchy. That is
a maintainer call, not a cost question (§10, Q1).

### 3.3 The item that is an ENGINE masquerading as a datum

**Bit-level lowering** — word→gate decomposition of `AdderStatement`,
`SelectStatement`, `PriorityCaseStatement`, `ShiftStatement` — is listed by the
corpus as a data gap for EDIF and BLIF. It is not data. It is **technology
mapping**: 6–10 maintainer-weeks *before a line of syntax is written*, and it
duplicates software this repo already depends on in tests.

`docs/standards-adoption/11-costed-rejections.md:269-290` says an EDIF emitter
"walks the same `HdlModel`" and is "genuinely small." **That is wrong on a
technical fact.** EDIF's `NETLIST` view is an instance-of-cell model; there is no
`assign {c,s} = a + b + cin` in it. BLIF's `.names` is a single-output two-level
SOP node and `.latch` is a single-bit flip-flop; a 32-bit adder's truth table is
2^65 rows. **The alternative that works is named** (D10 obligation 6): emit
Yosys JSON (datum #1, 3–4 wk) or emit the Verilog that already ships, and run
`yosys -p 'read_verilog design.v; synth_xilinx; write_edif design.edf'`. That is
real technology mapping, from ISC-licensed software already in `ToolLocator`'s
path, at **0.25–0.5 wk of documentation**.

**This row is why datum #1 outranks every datum in the table.**

---

## 4. THE FORMAT TABLE

Every format across all six families. **Direction** is emit/read. **Verdict** is
one of *ships-already / cheap-now / needs-data-first / needs-engine / refuse*.
Costs are maintainer-weeks at the project's coverage bar.

### 4.1 SHIPS ALREADY — the projection reframe is already load-bearing

| Format | Dir | New data | Cost | External tool that becomes the view | Licence / spec status |
|---|:---:|---|---:|---|---|
| **Verilog-2005** structural/dataflow, flat | emit | none | **0** | `iverilog` (compile oracle, in CI), Verilator, **Yosys → nextpnr → a real iCE40 board today** | IEEE 1364-2005 **paywalled**; iverilog GPL-2.0-or-later active; Verilator LGPL-3.0-only OR Artistic-2.0 active (5.042, Nov 2025) — both **GPL-3 compatible, absorbable** |
| **VHDL-93** structural, flat | emit | none | **0** | GHDL (`GhdlCompileTest.java:34`) | IEEE 1076 **paywalled**; GHDL GPL-2.0-or-later, active, **absorbable** |
| **PCF** iCE40 pin constraints | emit | none | **0** | nextpnr + icepack → bitstream | de-facto; nextpnr/icestorm **ISC**, absorbable (icestorm in maintenance mode) |
| **VCD** IEEE 1364-2001 §18 | emit | none | **0** | **GTKWave, Surfer, vaporview** — already the waveform view, verified by running the shipped fixture | IEEE **paywalled**, superseded but universally cited. No conformance program anywhere in this space; JLS's four-leg package (normative profile + spec-derived checker + byte goldens + a §6 stability promise) is stronger than most tools in the class publish |
| **EDIF** via `yosys write_edif` | emit | none | **0.5 wk docs** | Vivado, Quartus — with **real technology mapping** | Yosys **ISC**, active, absorbable. EDIF itself: ANSI/EIA-548 paywalled, committee dissolved |
| **BLIF** via `yosys write_blif` | emit | none | **0.25 wk docs** | ABC (MIT, active), VPR/VTR (MIT, active) | BLIF de-facto, ~1992 Berkeley, unmaintained as a spec; living description is the VTR docs |
| **SPICE** via `yosys write_spice` | emit | none | **0.25 wk docs** | ngspice, netgen LVS — and after `abc -liberty`, with **correct positional pin order**, because Yosys read the Liberty file | ngspice mostly 3-clause BSD, active. **No SPICE standard exists — only a Berkeley SPICE3f5 lineage** |
| **Yosys JSON** import (partial) | read | none | shipped; increments below | Yosys front end for Verilog/SystemVerilog | Yosys ISC. `CellValidator` accepts **19** cells (`:58-68`); `NetlistImporter` realizes **5** (`:233-259`) |
| **MRCS** output formats | emit | none | **0** | *nothing new* — its two outputs are Verilog (already emitted) and HSPICE (refused). See §5 | MRCS GPL-3.0, Unity/C#, **dormant since 2023-10-26** |

### 4.2 CHEAP-NOW — Tier A, no new primary data

| Format | Dir | Cost | External tool that becomes the view | Licence / spec status | Note |
|---|:---:|---:|---|---|---|
| **Export-policy totality + explicit REJECTED bucket** | — | **1 d** | — | internal; mirrors commit `970db41`'s registry→SaveTags totality test | **Prerequisite to every emitter below.** `FieldExtend`/`RegisterFile` drift is a live defect (§6.5) |
| **Probe-name validation + `$var` checker regex fix** | emit | **1–2 d** | GTKWave/Surfer/wellen | internal + IEEE 1364 §18 | Measured defect: `Wire.attachProbe:462-468` never calls `Util.isValidName:219-234`; a probe named `my probe.name` emits a malformed `$var` line |
| **Net-name stability** (`stableId` digest) | emit | **0.5–1** | every downstream annotation format | internal | `Element.stableId` exists since #165 and has **zero uses in `src/jls/hdl/`** (verified). Must precede any new goldens |
| **Machine-readable export report** | emit | **~1** | autograders, CI lanes | JLS-defined | The cheapest new "format" in the study |
| **Yosys JSON — WRITE** | emit | **3–4** | **netlistsvg** (MIT) as schematic renderer; **DigitalJS** via `yosys2digitaljs` (BSD-2) as a *browser simulator shareable by URL*; Yosys's own EDIF/BLIF/SPICE backends | Yosys ISC active; machine-readable schema at netlistsvg `lib/yosys.schema.json5` (MIT) — better documentation than most paywalled standards here | **Best formats/week in the plan.** `JsonValue.java` (580 L) is parse-only; a writer is ~120 L. 8 of 11 statement kinds map directly; `BitMapStatement` is **free** (bit routing lives in the connection array) |
| **Hierarchical Verilog + VHDL + SubCircuit export** (uniquified) | emit | **4–6** | `iverilog`, `ghdl`, Verilator, Yosys | IEEE 1364-2005 / 1076 | **Completes a user-visible feature in two already-shipped, golden-pinned, CI-cross-checked emitters** |
| **gEDA / Lepton EDA `.sch`** with **embedded** symbols | emit | **4–6** (+1–2 net-partition extraction, +0.6–1 acceptance test, +1–2 hierarchy) | **KiCad 10** (File → Import → Non-KiCad Schematic), **lepton-schematic** as an editing view, **lepton-netlist's 30+ netlist backends** | gEDA `.sch` frozen ~2007, line-oriented ASCII. Original gEDA/gaf **dead**; **lepton-eda GPL-2.0-or-later, active** (master committed 2026-06-15; last *tag* 1.9.18-20220529 — CI must pin a distro package, not a tag). KiCad **GPL-3.0-or-later**, active, 10.0.0 released 2026-03-19 | Symbols **embedded** via `[ … ]`, so **no library is curated**. `sch_io_geda.cpp:4774` dispatches `case '['`; `:3752-3762` installs the embedded symbol as the `LIB_SYMBOL`. **Falsify this in one afternoon before scoping** (§10, Q3) |
| **SAIF** switching activity | emit | **2–3** | **OpenSTA becomes the power view** — `read_saif` + `read_liberty` + `report_power` yields **real joules from a real cell library**, so JLS never invents a number it cannot know | Synopsys-originated de-facto, no open IEEE document — but **OpenSTA `power/SaifParse.yy` + `SaifLex.ll` are GPL-3.0-or-later**, the same licence as JLS: specification-by-implementation, freely absorbable | **The only O(nets) format in the study** — its size does *not* grow with run length. A 1.16e8-cycle boot yields ~80 KB, against VCD's 12.7 GB. Exactly **one** field (`TX`) is structurally zero, not two: `TZ` is real because JLS has HiZ |
| **Verilog testbench emitter** (from the `-t` grammar) | emit | **2–3** | **iverilog / Verilator / GHDL as differential oracles** | `docs/batch-interface.md` §2 is a **stability promise** (§6); the `-t` grammar at `:79-118` maps mechanically onto a Verilog `initial` block | This is the missing piece of "Verilator as an oracle" — not Verilator, which is already installable in CI |
| **VCD — READ** + first-divergence comparator | read | **2–4** | completes the oracle loop with **no engine and no subprocess** — two finished files are diffed | same as emit; `examples/autograde/autograde.py` already contains a dependency-free VCD parser | Costed by **no** prior phase; no roadmap slot exists |
| **Nested `$scope`/`$upscope`** | emit | **2–4 d** + a §6 contract bump | GTKWave/Surfer render JLS's **real** hierarchy instead of a viewer's `.`-splitting convention | IEEE 1364 §18 | The code is trivial; **the governance cost is the real price** (§10, Q2) |
| **Streaming `toVcd`** | emit | **1–2** | — | — | Removes a **measured** hard wall: `BatchSimulator.toVcd` materialises the whole dump as one `String` (`:384-476`, copied again at `:368`); a real 95.5 MB run **OOMs at `-Xmx768m`** and reports *"Not enough memory to simulate circuit"* — a writer failure misattributed to the simulator. **Prerequisite to every waveform item** |
| **FST** (GTKWave Fast Signal Trace) | emit | **3–6** | GTKWave, Surfer/wellen | **`fstapi.c` is `SPDX-License-Identifier: MIT`** (Tony Bybell 2009–2023, verified verbatim) — **transliterable outright under D8**. No IEEE/RFC spec and no change process; two maintained BSD-3 clean-room readers exist | **Measured**: FST-zlib is 10.3× smaller than VCD and **5.0× faster / 2.2× less memory** for hierarchy+1-signal+200-point-query access. **Pack type 0 (zlib) is the SMALLEST of the three** and emits no LZ4/FastLZ block, so `java.util.zip.Deflater` is the only codec needed — **no new dependency, no BOM line, no supply-chain surface** |
| **`-vcd out.vcd.gz`** | emit | **2–3 d** | GTKWave only | container choice, no spec | **Measured: Surfer/wellen REFUSES `.vcd.gz`.** Strictly dominated by FST on both size and access. Ship only with accurate docs, or not at all |
| **XDC / QSF / LPF** pin constraints | emit | **1–1.5 each** | Vivado, Quartus, Diamond | vendor de-facto, public docs; already specified in `docs/standards-adoption/06` | `PcfEmitter` siblings over the identical `model.ports()` walk — the projection reframe replicated three more times |
| **`rvfi_*` net names + RVVI-TEXT as the parity serialization** | — | **1–2 d (a decision)** | — | riscv-formal **ISC**, active; RVVI-TEXT v0.5 EBNF-specified, **licence unverified** | **Free now, and it must be decided before the parity emitter is written** or a private vocabulary gets invented |
| **Importer bit-level mesh synthesis** | read | **2–3** | Yosys front end | internal; Yosys JSON connections are always bit vectors | **Highest-leverage import task.** Today any slice or concat is refused wholesale (`NetlistImporter.java:41-46`). Gates four of the eight remaining mapper increments |
| **`FieldExtend` export** | emit | **1–1.5** | — | internal; undocumented drift from #201/#220 | One `assign` with a sign-extend/zero-pad concat; needs no new statement kind if `ReplicateStatement` generalizes |
| **`Memory` + `RegisterFile` export** (`MemoryStatement`) | emit | **2.5–3 combined** | BRAM designs on the shipped iCE40 path | standard `reg [W-1:0] mem [0:D-1]` + `$readmemh` | D1 consequence: a large memory image is bulk binary → **`$readmemh` sidecar**, which matches D1's content-kind table exactly |
| **Yosys mapper increments** (`$add`, `$dff`, `$dlatch`, `$tribuf`, `$bmux`, reductions, `$mem`/`$mem_v2`) | read | **5–7 total**, in ~1 wk slices | Yosys front end | Yosys ISC | The gap has **moved from validation to realization**. Import pipeline is deliberately **word-level-preserving** (no `flatten`, no `abc`, `memory -nomap`) |
| **Yosys hierarchy import → `SubCircuit`** | read | **2–3** | — | — | Today `selectModule` refuses any multi-module netlist (`:156-159`) even though `CellValidator` accepts hierarchy instances |
| **SystemC** structural | emit | **~3** (after hierarchy) | any SystemC install | Accellera SystemC **Apache-2.0**, active, absorbable | A third printer over the same IR. No consumer in CI without an install |
| **FIRRTL** | emit | **2–3** (after hierarchy) | CIRCT | firrtl-spec **Apache-2.0**, active; CIRCT Apache-2.0 with LLVM exception | A fourth printer over the same IR. **No stated demand** — build on request |
| **Logisim-Evolution `.circ`** | **read** | **12–18** (+1–2 test-vector CSV, +2–3 coordinate-preserving layout) | — (this direction moves *users*, not files) | Logisim-Evolution **GPL-3.0** (per-file headers say "GNU GPLv3" with **no "or later"** — see the hazard in §8), **active**: v4.1.0 released 2026-02-15, 7.2k stars | **The only item in the plan that moves USERS.** No `.circ` converter to *any* format exists anywhere (only open requests logisim-evolution#1616, #64) |
| **Digital (hneemann) `.dig`** | read | **6–9** (after `.circ`) | — | GPL-3.0 (same "or later" ambiguity), Java, active | Reuses the `.circ` scaffolding wholesale. `.dig` references subcircuits **by filename**, which maps onto `SubCircuit` more directly than `.circ` does |
| **SPICE `.subckt` PORT STUB** (interface only, no body) | emit | **~0.5** | a hand-off boundary for a student writing the analog implementation in ngspice themselves | de-facto | **Must be labelled a stub, not "SPICE support."** Genuinely teachable for D9's CS→ECE→EE span |
| **SPICE `.subckt` structural netlist** (self-consistent) | emit | **~1** (after hierarchy) | netgen-style LVS | de-facto, no spec, no conformance suite | **Cheap-now-after-hierarchy but DOMINATED** — `yosys write_spice` on JLS's shipped Verilog delivers it today with zero new code. Build only if a named consumer appears (§10, Q6) |

### 4.3 NEEDS-DATA-FIRST — Tier B, bounded new primary data

| Format | Dir | New primary data | Cost | External tool that becomes the view | Licence / spec status |
|---|:---:|---|---:|---|---|
| **`-parts` package/footprint binding file** | emit | footprint name + package pin map per element — **supplied by the USER in an external file**, never stored on an element or in the `.jls` | **2–4** (mechanism) | turns the gEDA `.sch` into a board-capable artifact | JLS-internal; the precedent ships and is tested (`PinBindings.java`, 98 L; `PcfEmitter`'s all-or-nothing validation `:66-119`) |
| **`INOUT` in the IR** | both | a third `Direction` member + a bidirectional port form | **1–2** | IP-XACT, KiCad, gEDA bidir, Yosys `inout` port import (today rejected at `NetlistImporter.java:181-187`), TT `uio_*` | internal. Note `ScannedPort.Direction` **already has INOUT** (`:14-20`) while `HdlModel.Direction` does not — the scan side already models what the export side cannot express |
| **Nominal time-unit scalar** | emit | **one declared scalar per circuit**, honestly labelled "tool-compatibility only" | **0.5–1** | FST, VCD, later SDF/STIL/Liberty | The precedent **ships**: `$timescale 1 ns` is hard-coded at `BatchSimulator.java:423` and defended at `docs/vcd-interop.md:70-71`. **Decide it ONCE in `HdlModel`, not four times** |
| **Reset on `Register`** | emit | an element input pin — `Register.init` adds exactly two inputs, D and C; the only reset-shaped field is `initialValue`, emitted as a declaration-time `reg x = <init>` | **3–5** | (a) **Tiny Tapeout / any ASIC target** — an ASIC flip-flop has no power-on initial value; (b) moves the entire `$adff`/`$adffe`/`$aldff`/`$adlatch` family off `CellValidator`'s reject list — **the most common idiom students write** | internal. **Revisit the 2026-07-17 decision at `CellValidator.java:140-143` explicitly** rather than assume it overtaken (§10, Q8) |
| **Port roles** | emit | per-port enum | **2–3** | XDC/SDC `create_clock`, IP-XACT bus interfaces | internal. `HdlExporter.java:485-491` treats `Clock` and `InputPin` identically — **JLS cannot currently say a wire is a clock** |
| **Positional pin order** | emit | a frozen, declared, test-pinned terminal order per element type | **~1** (≈2 d if already stable) | SPICE `X` calls, KiCad `.net`, EDIF cell binding | internal. **The only silent failure mode in the study**: a mis-ordered `X` line parses, simulates, and produces garbage. Independent confirmation that this is real: **KiCad ships a manual "Pin Assignment" dialog** in its Simulation Model Editor precisely because symbol order rarely matches `.subckt` order |
| **VLNV metadata section** | emit | four strings, as an OPTIONAL D3 section | **~1.5** | IP-XACT — **and D7's circuit-library format**, which needs exactly a versioning-and-provenance schema | IEEE 1685 `identifier.xsd`; Accellera XSDs **Apache-2.0**, absorbable |
| **Tiny Tapeout `tt_um_*` wrapper** | emit | a port binding file (cheap) + the `Register` reset (the real one) | **1.5–2** (wrapper) on top of hierarchy + reset | **a student's drawn circuit on a real SKY130 die** — the clearest demonstration of D9's CS→ECE→EE trajectory | **ACTIVE**: SKY 26b launched 25 Apr 2026, ChipFoundry shuttle CI2605. Template `TinyTapeout/ttsky-verilog-template` **Apache-2.0**. Good news: **INOUT is not on the critical path** — a first submission sets `uio_oe = 8'h00` |
| **IP-XACT (IEEE 1685-2022)** component + design | emit | VLNV, `INOUT`, bus interfaces, memory maps, parameterization | **3–4** on top of hierarchy + VLNV | Kactus2 as an **external validator** | **Re-assessed at HEAD: doc 08's seven blockers go to FIVE.** Blocker 7 (`buildModel` can't handle `SubCircuit`) is removed by hierarchy; blocker 2 (no reuse identity) is solved as a **side effect** of the type digest. The two that clear were the two that were *structural*; the five that survive are all new primary data. **Correct classification is needs-data-first, NOT refuse** |
| **RVVI-TEXT v0.5** retire trace | emit | a **retire event** — one record per retired instruction | **2–4** (event) + **1–2** (emitter) | Spike / Sail / `rvls` become the retire oracle | Imperas-originated, adopted by OpenHW/Core-V, MIPS, SiLabs. Line-oriented, **EBNF-specified (ISO/IEC 14977)**, with committed examples. **LICENCE UNVERIFIED** — formats are not copyrightable so emitting from the published EBNF is safe; absorbing RVVI *source* is not. `CYCLE` and `TIME` are **OPTIONAL elements JLS simply never emits**, so the parity contract's "no field for cycles or simulated time" survives serialization **enforced by omission** |
| **KiCad `.net`** | emit | footprint (hard gate), package pins, part value, designator scheme, positional pin order | **1.5–2** (emitter) + **2–4** (binding mechanism) | `pcbnew` — **but only with footprints** | KiCad GPL-3.0-or-later, active. **STRICTLY DOMINATED by the gEDA `.sch` path** (§5) |
| **EVCD** (`$dumpports`) | emit | per-driver drive strength (P1) + a bidirectional pin element (P2) | **~1** *after* P1's strength stage (P1 is 28–36) and P2's `BidirPin` (3–5) | Questa, VCS, Verdi/nWave — **proprietary, thin consumer base for the D9 audience**. Measured: **Surfer/wellen does NOT read EVCD** | **Sequenced, not closed.** Doc 07's two revisit triggers are exactly P1-S3 and P2, both real programs. The **honesty** argument for not building it now is the right one and it survives: constant strength digits would produce a file that *looks* like it carries drive-strength data and does not |
| **GDSII / OASIS / CIF / DEF, Liberty, LEF, SDF** | read | technology cells as data (10–16 wk) + layout geometry (8–12 wk) | **18–28** | KLayout-class read-only views | KLayout GPL-2.0-or-later active; Magic BSD-style active; Electric VLSI **GPL-3.0-or-later** (the only tool in the study with a licence identical to JLS's). **Dominated for formats** — every consumer is also reachable via Yosys, and KLayout is free and excellent. **The one honest justification is CROSS-PROBING**, which no external viewer can ever do because only JLS knows which cell instance is which drawn element |

### 4.4 NEEDS-ENGINE / REFUSE

| Format | Dir | Verdict | The reason — never precedent |
|---|:---:|---|---|
| **EDIF — direct emit** | emit | **needs-engine → refuse** | Not a printer. Instance-of-cell model requiring §3.3's 6–10 wk lowering pass **plus** a target cell library. **Second, independent problem: no third-party consumer can run in CI** (Vivado/Quartus are registration-walled and multi-gigabyte), so an EDIF claim cannot meet the repo's own four-part self-assertion standard. **Named alternative: `yosys write_edif`, 0.5 wk of docs.** (The EDIF 2 0 0 primary document was **not fetched** this pass; `edifLevel`/`keywordLevel` semantics remain unverified, as `11-costed-rejections.md:259-261` already flags. Moot if the Yosys recipe is adopted) |
| **BLIF — direct emit** | emit | **needs-engine → refuse** | Same lowering pass. `.names` is single-output and bit-level **by construction**. **Named alternative: `yosys write_blif`, 0.25 wk** |
| **BLIF / EDIF — read** | read | **refuse** | **This answers the brief's question directly: NO, they are not cheaper read targets than Yosys JSON, and the reason is the format's VALUE LEVEL, not parser cost.** BLIF's grammar is genuinely tiny. But BLIF is bit-level and JLS is word-level: importing it lands on BRIEF §4's bad rows — *gate-mapped import, memories preserved* = 9,500 elements / 3,965 ev-per-instr / **18.4 h**, versus *word-mapped Yosys import* = 2,300 / 1,120 / **4.3 h**. A ~4× element and ~4.3× wall-clock penalty **for free**, by choosing the wrong input format. **Spend the same weeks on the importer bit-mesh instead** |
| **XSPICE digital primitives** (`d_and`, `d_dff`, `d_state`, `d_lut`, …) | emit | **needs-engine → refuse** | **The strongest form of the maintainer's argument, and never evaluated by the corpus — so it gets an explicit reasoned refusal rather than an omission.** XSPICE requires no device models, no PDK and no cells (`ngspice/src/xspice` is **PUBLIC DOMAIN**), which is exactly why it looked decisive. It fails structurally: **XSPICE digital nodes are ONE BIT and XSPICE has NO arithmetic primitive at all** — no adder, no multi-bit mux, no ALU. JLS's `Adder` is one element with one `react()` and ripple survives only as `propDelay = bits*30` (`Adder.java:259-262`); there is no gate-level lowering pass in `src/`. Of the 22 EXPORTED types, five have no XSPICE realisation — Adder, Mux, Decoder (+ Memory, RegisterFile) — **and they are the datapath. A JLS CPU emitted to XSPICE is a control path with a hole where the ALU was.** Worth recording that the *rest* maps pleasantly: `StateMachine`→`d_state`, `TruthTable`→`d_lut`, `TriState`→`d_tristate`, `Register`→`d_dff`×N, and Splitter/Binder/Extend become **free** because nodes are 1-bit |
| **SPICE device-level analog** (`.model`, R/L/C/D/Q/M cards, `.tran`/`.ac`/`.dc`/`.noise`) | emit | **refuse — semantic, not cost** | **A JLS circuit has no electrical content.** Value domain is 2-state + HiZ, no X; time is a dimensionless 64-bit integer. JLS models **neither energy nor real time**, which forbids every `.model` and every analysis card — `.tran`/`.ac`/`.dc`/`.noise`/`.pz`/`.disto`/`.tf`/`.sens`/`.four` have **zero preimage; JLS has no notion of an "analysis" at all.** Every floating-point occurrence in `jls.elem`/`jls.sim` is screen geometry. Building it is another tool class: MNA assembler, sparse LU with pivoting, Newton-Raphson with damping/gmin/source stepping, LTE timestep control, a device library with analytic derivatives |
| **SPICE netlist — read** | read | **refuse** | Dominated by the Yosys JSON path, which does the same thing losslessly with typed cells and parameters. And **the REALISER is the bottleneck, not the parser**: a SPICE front end just adds a second way to reach a mapper that covers five cell types and cannot emit a flip-flop |
| **SPICE `.subckt` HEADER scan** into `ScannedModule`/`ScannedPort` | read | **refuse — on a TRUTHFULNESS ground** | ~2–3 days of work reusing `src/jls/hdl/scan/`, and it is still wrong. `ScannedPort` has exactly three fields (`:24-30`): name, direction {IN\|OUT\|INOUT}, bits "always at least 1". **A `.subckt` line supplies only the name.** SPICE nodes are undirected and every SPICE node is one node, so the scanner would fill **two of three fields with fiction** — direction guessed, bits always 1 (a 32-bit bus imports as 32 unrelated ports with no grouping). Not shippable under the project's own conform-to-a-named-clause discipline |
| **Qucs / Qucs-S `.sch`** | emit | **refuse — semantic** | Every meaningful Qucs quantity is a physical unit — volts, amperes, ohms, seconds — and JLS has none of them. Also not headless-friendly: a version mismatch raises a **modal warn-and-ask dialog** (`schematic_file.cpp:1193-1218`). Original Qucs is dead; Qucs-S is the living fork and is a **front end, not a simulator** |
| **STIL (IEEE 1450) / WGL** | emit | **refuse for now — and ADJUDICATE** | Needs tester timing: drive and strobe **edge placements in real time units**, plus pin groups. The `-t` grammar has no edge-placement concept. The D9 audience has no ATE. WGL is proprietary with no published spec. **⚠ This is the ONE place in the study where two passes disagree about whether a format is blocked by DATA**: `sweep-06:137` says nothing structural blocks it and prices it as a 2–3 wk printer over the existing test-vector engine; `fmt-waveform-verif.md` §4.8 gates it behind P10's pattern model. **One of them is wrong. Adjudicate before either is scheduled** |
| **GHW** | emit | **refuse — with the alternative named** | `VhdlEmitter` already ships. **Emit VHDL and let GHDL write the GHW itself.** That is the projection reframe working correctly. Emitting GHW directly would assert nine-value semantics JLS does not have — the EVCD error in a different costume |
| **LXT / LXT2 / VZT / VPD / WLF / FSDB** | emit | **refuse** | GTKWave's own docs: *"It is planned to remove support for the following formats in GTKWave 4: LXT, LXT2, VZT, IDX, AET2, VPD, WLF, FSDB."* Surfer/wellen never read them. VPD/WLF/FSDB are proprietary with no open spec. **This is a legitimate objection under D10 because it is a statement about the external ecosystem's trajectory, not about JLS's current state** |
| **RVVI-API / RVVI-TRACE** | both | **refuse — take the format, refuse the transport** | This is exactly the live co-simulation `docs/vcd-interop.md:19-24` rejects under #63. It requires in-process foreign code, cutting against the single offline jar and against determinism. **Named alternative: RVVI-TEXT** — file-based, offline, diffable, same payload |
| **KiCad `.kicad_sch`** | emit | **refuse — strictly dominated** | 8–12 wk versus 4–6 for gEDA `.sch` reaching the **same destination and more**, plus a documented forward-**in**compatible format that becomes a maintenance event at every KiCad major release. Bus factor 1 |
| **Footprint / symbol LIBRARIES; gate-to-package packing** | emit | **refuse** | Unbounded permanent curation at bus factor 1, against a free incumbent (KiCad's libraries: thousands of parts, CC-BY-SA-4.0 **with an explicit waiver leaving generated designs unencumbered**). Packing additionally needs a bin-packing algorithm over pin compatibility, plus power/ground pins JLS does not model at all. **REFUSE THE LIBRARY, SHIP THE EMITTER — they are separable precisely because the destination tool has the library.** This is the only thing that ever made the KiCad verdict "a new tool" |
| **Fritzing `.fzz` / `.fzp`** | emit | **refuse** | A Fritzing part is **four hand-authored SVG views plus a connector table**. No function of a JLS circuit produces it. D9 puts the breadboard view in scope; it does not make it cheap. (Also: Fritzing's *code* is GPL-3.0 but its part **artwork is CC-BY-SA** — a separate and stricter permission) |
| **IPC-D-356A** | emit | **refuse — and the roadmap's stated reason is wrong** | `README.md:1079-1082` and `sweep-06:570-576` decline it because *"a bare-board test netlist without a board layout has no consumer."* **The right reason is that it CANNOT BE EMITTED**: its mandatory 80-column fixed fields carry XY location, drill/pad geometry and layer, from a board layout JLS will never have. Right verdict, wrong justification — **and the wrong justification is re-openable the day a consumer appears** |
| **CircuitVerse `.cv`** | read | **refuse — a TIMING objection, not a demand gate** | A GSoC 2026 project *"Structured Format for Saved Circuit Data"* is **actively restructuring the format right now**. Building a reader against a format being rewritten is the one genuinely bad time to build a reader. **Revisit trigger: the GSoC 2026 output lands and is documented** |
| **MRCS** as a code or format target | both | **refuse on practice, not licence** | GPL-3.0 so licence-compatible, but it is a **Unity/C# application** (the `UnityEngine` dependency is its spine, not a shell) and **dormant since 2023-10-26** (2 y 9 m). Absorbing it imports an unmaintained codebase into a bus-factor-1 project. Its two output formats resolve to *"emit Verilog"* (shipped) and HSPICE (refused). **What IS absorbable is the ALGORITHM from the open-access thesis** — a paper-reading exercise, worthwhile only if JLS ever wants MVL *synthesis*, which `07-mvl-determination.md` §5 routes around via lowering |

---

## 5. THE SEQUENCED PLAN

Waves are ordered by the sorting principle: projections of existing data first,
bounded new data second, permanent obligations never. **Every wave is
independently shippable and every item inside a wave is an issue-sized unit.**

**One calendar caveat, stated up front and honestly.** Under **D6**, defect
fixes land immediately and *everything else in this programme sequences behind
#77* (the core extraction). Nothing in this plan is *technically* gated on #77 —
no item reads data #77 creates — but its **calendar start** is. The only Wave 0
items exempt are the two that are defect fixes on their own terms (W0.1, W0.2).

### WAVE 0 — ships this month. Zero new data, zero risk.

| Item | Cost | Depends on | Unlocks |
|---|---:|---|---|
| **W0.1** Export-policy totality test over `ElementRegistry.ALL`; promote the implicit fall-through to an explicit **REJECTED** bucket with teaching messages | **1 d** | — (**lands under D6 now**) | Closes the measured `FieldExtend`/`RegisterFile` drift. **A standing guard, and a prerequisite to every emitter in the plan** — every new emitter inherits `buildModel`'s policy |
| **W0.2** Validate probe names (`Wire.attachProbe` → `Util.isValidName`); fix the `$var` checker regex (`\w+` vs the dotted names §4.2 documents) | **1–2 d** | — (**lands under D6 now**) | A measured correctness defect on a documented stability surface, about to become a parity-contract surface |
| **W0.3** Net-name stability: short deterministic `stableId` digest instead of the save-index `getID()` | **0.5–1 wk** | — | Name-stable HDL diffs (**D2**). **Must land before any new goldens are generated** or hierarchy goldens are regenerated twice. Keep user names for ports/named registers/jump aliases; digest only genuinely anonymous nets |
| **W0.4** Machine-readable export report from `HdlModel.renames()` / `warnings()` | **~1 wk** | W0.1 | Autograders and CI lanes. The cheapest new "format" in the study |
| **W0.5** Document the `yosys write_edif` / `write_blif` / `write_spice` routes in `docs/hdl-support-research.md` | **0.5 wk** | — | **Converts three costed rejections into a documented, shipped capability.** Turns *"JLS does not support EDIF/BLIF/SPICE"* into *"JLS supports them through the Yosys step it already documents"* |
| **W0.6** Decide and record: `rvfi_*` net names verbatim + **RVVI-TEXT v0.5** as the parity trace serialization, in `docs/parity-contract.md` §3.1/§5.1 | **1–2 d** | — | **Free now; prevents inventing a private trace format later.** Also fixes the "stringly-typed seam" charge against `parity-contract.md:442` — RVFI *is* a naming convention on nets, industry-wide |
| **W0.7** Corpus corrections (docs only, fold into the above): "eleven statement kinds", not ten; `sweep-06:82`'s gate reason → "cells to name **and order**"; `sweep-01:50`'s SAIF classification; `07-waveform-formats.md`'s `fstapi.c` licence → **MIT**; annotate `standards-landscape.md:252` (#69) with the GTKWave-4 removal notice; a line-number refresh pass over `docs/capability-roadmap/` | **~0** | — | Stops five stale claims propagating into issues |
| **W0.8** One FPGA constraint emitter (**XDC** or **LPF**) | **1–1.5 wk** | — | A `PcfEmitter` sibling over the identical `model.ports()` walk; already specified in `docs/standards-adoption/06` |

**Wave 0 total: 3.5–5.5 maintainer-weeks. Pick seven of the eight for a strict
month.** Gated on: nothing (W0.1, W0.2); #77 (the rest, per D6).

### WAVE 1 — the projection engine. Zero new data.

| Item | Cost | Depends on | Unlocks |
|---|---:|---|---|
| **W1.1** Stream `toVcd`; fix the misattributed OOM diagnostic | **1–2 wk** | — | Removes a measured ~10×-of-output-size heap wall. **Prerequisite to every waveform item** |
| **W1.2** Extract the net partition out of `HdlExporter` into a shared `jls.netlist` package (union-find + jump aliasing + `Group` naming) | **1–2 wk** | W0.1 | **The enabling refactor for every netlist-shaped emitter.** Pure motion, proven by 34+29+1 unchanged goldens. **Without it JLS acquires two net partitioners that can disagree**, and `PcfEmitter`'s "can never disagree about the interface" invariant stops being a property of the design and becomes a coincidence |
| **W1.3** **Yosys JSON writer** (`JsonValue` write path ~120 L + an `HdlEmitter` at the published `HdlExtensionPoints.EXPORTER` seam) | **3–4 wk** | W1.2 | **netlistsvg** (schematic renderer), **DigitalJS** (browser simulator, shareable by URL — a capability JLS has no other route to), and Yosys's own `write_edif`/`write_blif`/`write_spice`. **The best item in the plan** |
| **W1.4** Nested `$scope`/`$upscope` in VCD | **2–4 d** + a §6 contract decision | W1.1 | GTKWave/Surfer render JLS's real hierarchy |
| **W1.5** Importer bit-level mesh synthesis (Splitter/Binder from arbitrary Yosys bit vectors) | **2–3 wk** | — | **The single highest-leverage import task.** Gates four of the eight remaining mapper increments. Real designs slice constantly; today any slice is refused wholesale |
| **W1.6** Fix `-export` dispatch for a third emitter: the extension whitelist at `JLSStart.java:1088-1090` **and** the binary ternary at `:382-385` must both learn the new suffix | **1–2 d** | W1.3 | Prevents the fourth emitter shipping behind a two-way switch |

**Wave 1 total: 8–12 maintainer-weeks.** Gated on: #77.

### WAVE 2 — hierarchy. The pivot. Zero new data.

| Item | Cost | Depends on | Unlocks |
|---|---:|---|---|
| **W2.1** `InstanceStatement` + multi-module `HdlModel` + recursive walk (cycle detection, port→net binding, reject propagation) + both printers + goldens + `iverilog`/`ghdl` cross-check — **UNIQUIFIED** (one module per instance, no type identity) | **4–6 wk** | **W0.3 (hard)**, W0.1 | **Hierarchical Verilog-2005, hierarchical VHDL-93, and SubCircuit export** — completing a user-visible feature in two already-shipped, golden-pinned, CI-cross-checked emitters. Today a decomposed 4-bit ALU **throws** |
| **W2.2** `FieldExtend` export | **1–1.5 wk** | W0.1 | Removes one of the two untested reject-bucket types |
| **W2.3** `MemoryStatement`: `Memory` + `RegisterFile` export, with a `$readmemh` sidecar for the init image (D1's content-kind table) | **2.5–3 wk** | W0.1 | BRAM designs on the shipped iCE40 path; removes the other two fall-through rejects |

**Wave 2 total: 7.5–10.5 maintainer-weeks.** Gated on: W0.3 (a real
dependency — regenerating hierarchy goldens twice is the cost of skipping it).
Roadmap ownership: **W2.1 is a P3 slice.** Do not start P3.

### WAVE 3 — the differential oracle. Zero new data, no subprocess in `src/`.

| Item | Cost | Depends on | Unlocks |
|---|---:|---|---|
| **W3.1** Verilog testbench emitter from the `-t` stimulus grammar | **2–3 wk** | W0.3 | The loop: `jls -b -t -vcd → jls.vcd`; `jls -export design.v -tb tb.v → iverilog → hdl.vcd`; compare at settling points |
| **W3.2** VCD **reader** + signal-aligned comparator with first-divergence reporting | **2–4 wk** | W1.4 (naming/scope interacts) | Turns **Verilator, iverilog and GHDL into differential oracles for a drawn circuit** |
| **W3.3** Verilator as a second, fast lane (`lint_off CASEINCOMPLETE` for the `TruthTable` hold template; `lint_off LATCH` for the `Register` LATCH template; scope slice 1 to non-tri-state circuits) | **1–1.5 wk** | W3.1 | **29× speed over iverilog on identical RTL.** Verilator is the *speed* upgrade, not the correctness enabler — build against iverilog first |

**Wave 3 total: 5–8.5 maintainer-weeks.** Gated on: #77.

Two honest notes. (a) Emitted Verilog is delay-free while JLS carries per-element
propagation delays, so **comparison must be at settling points, not per-delta** —
this is not a workaround; BRIEF §6's parity contract already places all timing in
the PERMITTED-to-differ column with industrial precedent. (b) This is **batch**:
two finished VCDs are diffed. It therefore does **not** conflict with
`docs/vcd-interop.md:19-24`'s rejection of live co-simulation under #63 — but the
adjacency is close enough to be mistaken for the rejected thing, so **confirm the
reading with the maintainer explicitly** (§10, Q5).

### WAVE 4 — the outward views. Zero new data.

| Item | Cost | Depends on | Unlocks |
|---|---:|---|---|
| **W4.1** gEDA / Lepton `.sch` emitter, flat, self-contained: `C`/`N`/`P`/`T`/`{}`/`[…]` records; symbols synthesised from `Element` geometry and **embedded**; `netname=` from `Group.name`; deterministic bytes pinned by a golden. **Element policy: render everything** — `Memory`, `RegisterFile`, `FieldExtend`, `SubCircuit` all draw fine and only fail to *synthesise* | **4–6 wk** | **W1.2 (hard)**; **W4.0 falsification** (§10, Q3) | **KiCad 10 becomes the PCB view; lepton-schematic an editing view; lepton-netlist's 30+ backends downstream** |
| **W4.2** `lepton-netlist` acceptance test (`ToolLocator.findOnPath` + `assumeTrue`, mirroring `GhdlCompileTest.java:34-35`) | **3–5 d** | W4.1 | Converts a shape golden into *"the format's reference reader accepted it"* — evidence the shipped PCF golden still lacks |
| **W4.3** gEDA hierarchy via `source=` (needs a multi-file output shape; `JLSStart` writes exactly one file today) | **1–2 wk** | W4.1 | **Hierarchy without `HdlModel`'s instance statement** — a schematic projects the `Circuit` graph, not the IR. Interchange and hierarchy are **not one programme** here |
| **W4.4** SAIF emitter + **a frozen, normative net-naming convention** + an OpenSTA interop test | **2–3 wk** | **W0.3 (hard)** | **OpenSTA becomes the power view with real joules from a real Liberty library.** Plus a schematic **heat map** in the GUI for almost nothing once the counters exist — the one deliverable no waveform viewer can produce |
| **W4.5** FST writer, pack type 0 (zlib), transliterated from **MIT** `fstapi.c` with attribution; four block types only; an in-tree reader for self-consistency goldens; a skip-when-absent `fst2vcd` interop arm | **3–6 wk** | **W1.1 (hard)** | Makes a boot-length debug trace **openable at all**. Must be phrased as **interop against named tool versions**, never conformance |

**Wave 4 total: 11–17.5 maintainer-weeks.** Gated on: W1.2, W1.1, W0.3.
Roadmap ownership: **W4.4 is a P5 slice** (activity moved to P5 per
`AMENDMENT.md`). Do not start P5.

**Risk note on W4.1, stated plainly.** KiCad's gEDA importer is **four months
old** (present in 10.0.0/10.0.5, absent from 9.0 and 8.0) and its
embedded-symbol path is in the code but **not in the user manual**
(`eeschema_importing.adoc:148-151` documents only external `.sym` with a
rectangular fallback). **Falsify with a hand-written ten-line `.sch` before
scoping.** If embedding does not work in practice the emitter must ship `.sym`
sidecars — the "no library curation" claim weakens but does not collapse, since
JLS would *generate* those files, not curate them. Validating against
`lepton-netlist` as well (W4.2) is what keeps the artifact from depending on a
single four-month-old importer.

### WAVE 5 — depth on the paths now shipping. Zero new data.

| Item | Cost | Depends on | Unlocks |
|---|---:|---|---|
| **W5.1** Subcircuit type identity: canonical structural digest + collision-naming policy recorded in `model.renames()` → **deduplicated** hierarchy | **1.5–2 wk** | W2.1 | File size, the "one module reused N times" lesson, and **IP-XACT blocker 2 as a side effect** |
| **W5.2** Yosys mapper increments, in ~1 wk slices: `$add`, `$dff`, `$dlatch`, `$tribuf`, `$bmux`, reductions, `$mem`/`$mem_v2` | **5–7 wk** | W1.5 | Closes the validation→realization gap: `CellValidator` accepts 19, `NetlistImporter` realizes 5. **Note `$dff` currently has a committed test *asserting* rejection — it flips** |
| **W5.3** Yosys hierarchy import → `SubCircuit` | **2–3 wk** | W1.5, W2.1 | Imported designs keep the module structure the author wrote instead of becoming one enormous flat sheet |
| **W5.4** Remaining two constraint emitters (QSF, LPF or XDC) | **2–3 wk** | — | Vivado / Quartus / Diamond flows |

**Wave 5 total: 10.5–15 maintainer-weeks.** Gated on: Waves 1–2.

### WAVE 6 — the first genuinely-new data. Tier B, bounded. All are existing-program SLICES.

| Item | Cost | Owner | Unlocks |
|---|---:|---|---|
| **W6.1** `INOUT` in `HdlModel.Direction` + a bidirectional port form (**IR half only**) | **1–2 wk** | **P3 slice** | Necessary for six targets. Highest necessary-leverage in the plan. `ScannedPort` already has INOUT; only the export side lacks it |
| **W6.2** Nominal time-unit scalar, **decided once in `HdlModel`** | **0.5–1 wk** | **P4 slice** | FST/VCD honesty, and later SDF/STIL/Liberty — deciding it once is cheaper than four times |
| **W6.3** Freeze and test-pin **positional pin order** per element type | **~1 wk** (≈2 d if `getAllPuts()` is already stable — measure first) | **unowned → standalone** | Must land **before** any cell-bound emitter. The only silent-when-wrong item |
| **W6.4** **Reset on `Register`** (async/sync, polarity, reset value distinct from init) + format section + `RegisterStatement.reset` + both emitters + goldens + `CellValidator` propagation | **3–5 wk** | **P2 slice** | (a) ASIC-honest HDL; (b) the whole `$adff` family off the reject list; (c) Tiny Tapeout `rst_n`. **Revisit `CellValidator.java:140-143` explicitly** |
| **W6.5** VLNV as an OPTIONAL D3 section | **~1.5 wk** | **P3 slice + D7** | IP-XACT **and** D7's circuit-library versioning/provenance schema |
| **W6.6** Port roles | **2–3 wk** | **P3 slice** | Clock-aware constraints (XDC `create_clock`), IP-XACT bus interfaces, P13 domain inference. Fixes `HdlExporter.java:485-491` |

**Wave 6 total: 9–14.5 maintainer-weeks**, drawn from P2/P3/P4 which total
69–103 weeks. **You do not need the programs; you need these six slices.** This
is the pattern the repo already follows: #199, #201, #213, #167 and #220 all
landed as 1–5 week slices ahead of their programs, and **every program in this
plan's ownership column is unstarted at HEAD** (verified by absence-grep for
`LogicValue`, `Multiplier`, `InOutPin`, `InstanceStatement`, `DelayModel`,
`Liberty`, `jls.core.elab`, `int phase`, `class Assert`, `SAIF` — all zero).

### WAVE 7 — the demonstration.

| Item | Cost | Depends on | Unlocks |
|---|---:|---|---|
| **W7.1** Tiny Tapeout `tt_um_*` wrapper emitter + a port binding file + a submission recipe | **1.5–2 wk** | W2.1, W6.4 | **A student's drawn circuit on a real SKY130 die.** The clearest possible demonstration of D9's CS→ECE→EE trajectory. The wrapper is a pure projection over `model.ports()` plus a binding file — structurally identical to `PcfEmitter`/`PinBindings` |

**Wave 7 total: 1.5–2 maintainer-weeks on top of Waves 2 and 6.**

### WAVE 8 — migration. The only item that moves USERS.

| Item | Cost | Depends on | Unlocks |
|---|---:|---|---|
| **W8.0** Promote `NetlistImporter.Builder` from `private static final class` (`:410`) to a shared class | **~1 wk** | — | Any second importer |
| **W8.0b** Measure: run the accept/reject table over a corpus of public `.circ` files | **~2 d** | — | **Could reorder the mapper increments. Nobody has done this and the 12–18 wk figure must not be mistaken for a measurement** |
| **W8.1** Logisim-Evolution `.circ` importer, increment 1 (structure-correct, JLS-generated layout) — ~3,960 new lines | **12–18 wk** | W8.0, the licence answer (§8) | **Migrates an entire existing user base of course material into JLS.** No `.circ` converter to *any* format exists |
| **W8.2** Logisim test-vector CSV reader | **1–2 wk** | W8.1 | W8.1 migrates the **circuit**; this migrates the **assignment's grading**. Together they migrate a **course** |
| **W8.3** Coordinate-preserving layout | **2–3 wk** | W8.1 | The imported circuit looks like the author drew it |
| **W8.4** Digital `.dig` importer | **6–9 wk** | W8.1's scaffolding | A second migration corpus |

**Wave 8 total: 16–24 wk (W8.0–W8.3), +6–9 for `.dig`.**

Four hard risks, all real: (1) **connectivity is purely geometric** — the
importer must replicate Logisim's per-component port geometry exactly or
circuits import silently disconnected (`AbstractGate.getInputOffset` contains
rules like `if (inputs == 4 && size >= 60) dy -= 10;`). (2) **A name-collision
trap**: Logisim's `ShiftRegister` is **sequential**; JLS's `ShiftRegister` is a
**combinational barrel shift** (`HdlExporter.java:78-80`, "despite its name it
holds no state, issue #122"). Mapping by name is a silent correctness disaster
and must be a **loud reject**. (3) **XXE**: this would be the **first XML parse
in `src/`** (grep-verified: no `javax.xml`, `org.w3c.dom`, `DocumentBuilder` or
`XMLStreamReader` anywhere). A `.circ` is untrusted input and #38's premise is
that a circuit file cannot touch your machine — `FEATURE_SECURE_PROCESSING` +
`disallow-doctype-decl` + null entity resolvers, **with a test per vector**, is
mandatory. (4) **The licence question** (§8) is blocking for absorbing the port
geometry, though not for a re-derived importer at higher cost and higher defect
risk.

### WAVE 9 — conditional and demand-gated. Do not schedule; record the trigger.

| Item | Cost | Trigger |
|---|---:|---|
| `-parts` package/footprint binding mechanism | **2–4 wk** | A user shows up with a real design and a real board |
| Retire event + RVVI-TEXT emitter | **3–6 wk** | **Needs an owner and a program number first** (§6.2). The format decision is already free at W0.6; only the emitter waits |
| IP-XACT component + design | **3–4 wk** on top of W6.1/W6.5/W6.6 | A named consumer, or the D7 library format arriving and wanting VLNV anyway |
| SystemC / FIRRTL printers | **3 / 2–3 wk** | A request. Both ride the same IR; neither has a CI consumer |
| SPICE `.subckt` structural emitter | **~1 wk** | **A NAMED consumer.** Without one it buys a shorter command line than `yosys write_spice`. #212's demand-gate discipline applies to third-party asks and this is one |
| SPICE `.subckt` port stub | **~0.5 wk** | Teaching demand for a CS→ECE hand-off boundary |
| `BidirPin` element | **3–5 wk** | P2 |
| EVCD | **~1 wk** | P1's strength stage **and** P2's `BidirPin` |
| KiCad `.net` | **1.5–2 wk** + `-parts` | A user names a non-KiCad tool that reads KiCad netlists and cannot read a gEDA schematic |
| `.vcd.gz` | **2–3 d** | Only with accurate documentation ("GTKWave, not Surfer") |
| CircuitVerse `.cv` | — | The GSoC 2026 format output lands and is documented |

---

## 6. UNOWNED WORK — assign it now or it gets discovered mid-emitter

### 6.1 Package/pin binding data — 5 formats, no owner, mis-attributed
Both sibling surveys route footprints and package pin numbers to **P6**. **P6
owns SILICON cells** (Liberty, LEF, `CellInstance`); **nothing owns PACKAGES.**
Required by KiCad `.net` (a hard gate), gEDA `footprint=`, IPC-D-356A, Fritzing,
and — unnoticed by `sweep-06` — **BSDL (#129)**, since a boundary-scan
description is literally a package-pin-to-cell map. The mechanism is already
designed and shipped in miniature (`PinBindings`, 98 lines). **Assign `-parts`
as a STANDALONE item, per the #213 precedent. Say so explicitly, or it drifts
into P6 and inherits P6's 20–32 weeks of gating.**

### 6.2 The retire event — the study's own deliverable, unowned
`fmt-waveform-verif.md` attributes it to "L6/P16". **Verified: there is no P14,
P15 or P16.** `AMENDMENT.md`'s program list ends at P13 and its Honest-totals
table has fourteen rows, none a parity or retirement programme. RVVI-TEXT and
every parity claim this study makes depend on it. **Without an owner it will be
scheduled by whoever writes the parity emitter, at which point 2–4 weeks of
event work lands inside a "1–2 wk emitter" line.**

### 6.3 Positional pin order — 3 formats, no owner, **silent** when wrong
`getAllPuts()` order is incidental at HEAD; nothing freezes or tests it. A wrong
order produces a **parseable deck that simulates the wrong circuit**. **Measure
first** (~1 day): if the order is already stable, W6.3 is a test and a doc note
rather than a change.

### 6.4 A frozen net-naming CONVENTION — 6 formats, no owner
W0.3 gives name *stability*. Nothing makes the resulting names a **normative
contract** that SAIF, SDF, EDIF properties, LEC and external-simulator VCD
comparison can key on. **Fold the normative note into W0.3 or it is discovered
six formats later.**

### 6.5 Export-policy totality — a standing obligation, not a one-off
Measured at HEAD: `EXPORTED` = 22 (`HdlExporter.java:422-430`), `SKIPPED` = 6
(`:432-434`), `TOPOLOGY` = 4 (`:437-438`, including `Wire`, which is not a
registry entry) — 22 + 6 + 4 − 1 = **31 of the 35-type `ElementRegistry.ALL`**.
`FieldExtend`, `Memory`, `RegisterFile`, `SubCircuit` fall to the `offenders`
throw at `:191-197`. `Memory` and `SubCircuit` are deliberate and test-pinned
(`HdlPolicyTest:63,:77`); **`FieldExtend` and `RegisterFile` appear nowhere in
`test/jls/hdl/`** — undocumented drift landing in an error path whose message
implies deliberate non-support. Because a new element type costs **zero format
version**, **this hole recurs every time the element set grows, and every new
emitter multiplies the cost of leaving it non-total.**

### 6.6 Per-view geometry — required by ZERO external formats
The briefing lists it as a candidate datum. **Checked across all six families:
not one external format requires it.** gEDA `.sch` and `.kicad_sch` want *a*
geometry and JLS has exactly one. Per-view geometry belongs to JLS's own
multi-view programme and, per D9, to the collaboration stack — one design
problem (one artifact, N views, M editors) with the `CircuitOp` grammar as the
shared seam. **It is real unowned work; it is not format work. No format item
may be sequenced behind it.**

---

## 7. WHAT THIS DOES NOT SOLVE, AND WHAT IS REFUSED

**This plan does not make JLS an analog simulator, a synthesis tool, a
place-and-route tool, or a PCB tool.** It makes JLS *legible* to the tools that
are. Specifically it does not solve:

- **Any physical quantity.** JLS models neither energy nor real time; its value
  domain is 2-state + HiZ with no X; time is a dimensionless 64-bit integer and
  an `Adder`'s delay is `bits*30`. No format in this plan asserts a volt, a
  second, or a joule. SAIF works precisely *because* its payload — toggle counts
  and duty-cycle ratios — is unit-free, and OpenSTA supplies the joules from a
  Liberty library JLS never has to own.
- **Technology mapping.** Nothing here lowers word-level to gate-level. Yosys
  does that, and does it better than JLS ever would.
- **The board.** No footprint library, no package model, no power/ground concept,
  no layout geometry. The gEDA path hands a schematic to KiCad and lets the
  student assign footprints from KiCad's own thousands-strong free library —
  exactly as KiCad's own documentation instructs.
- **Live co-simulation.** Every oracle in this plan is **batch**: two finished
  files, diffed offline. That is deliberate, and it is what keeps #63 intact.

**Refused, with the reason — never the precedent** (D10 obligations 1 and 6):

| Refused | The reason |
|---|---|
| **Device-level analog SPICE, and Qucs** | **Semantic, not cost.** SPICE's leaves are *devices* and JLS has *no devices*. Every analysis card has zero preimage — JLS has no notion of an "analysis". This is the one row where six prior sweeps and all six of this study's families converge |
| **XSPICE digital primitives** | **Structural, not effort.** XSPICE nodes are single-bit and XSPICE has no arithmetic primitive at all; JLS's elements are words with no lowering pass. The datapath — Adder, Mux, Decoder — has no realisation. This is the strongest form of the maintainer's argument and it is the one that genuinely fails |
| **Direct EDIF and direct BLIF emit** | Not printers. A 6–10 wk synthesis pass duplicating Yosys, plus (for EDIF) a cell library, plus (for EDIF) **no CI-installable consumer at all**. **Alternative named: the Yosys backends, 0.75 wk of docs total** |
| **BLIF/EDIF as READ targets** | Not cheaper than Yosys JSON. Bit-level input costs ~4× the elements and ~4.3× the wall clock. **Alternative named: spend those weeks on the importer bit-mesh** |
| **SPICE netlist read; SPICE `.subckt` header scan** | The first is dominated by Yosys JSON and the realiser is the bottleneck anyway. The second is refused on a **truthfulness** ground: SPICE ports carry no direction and no width, so a scanner would fill two of `ScannedPort`'s three fields with fiction |
| **Footprint/symbol libraries; gate-to-package packing; breadboard artwork** | Unbounded permanent curation at bus factor 1, against free incumbents. **Refuse the library, ship the emitter** |
| **IPC-D-356A** | Cannot be emitted at all: its mandatory fixed columns carry layout geometry |
| **`.kicad_sch`** | Strictly dominated by gEDA `.sch`: same destination, 2× the work, forward-incompatible format |
| **STIL / WGL** | Needs edge placements in real time units. **⚠ And the STIL contradiction must be adjudicated before either verdict is trusted** |
| **GHW; LXT/LXT2/VZT/VPD/WLF/FSDB; RVVI-API** | Alternatives named for each: emit VHDL and let GHDL write the GHW; GTKWave 4 removes the LXT family; take RVVI-**TEXT** and refuse the transport |
| **MRCS as a code or format target** | Licence-compatible (GPL-3.0), **practically not absorbable**: Unity/C#, dormant 2 y 9 m. Its outputs resolve to "emit Verilog" (shipped) and HSPICE (refused) |
| **CircuitVerse `.cv`** | A **timing** objection, not a demand gate: the format is being rewritten right now |

**One item on the maintainer's own list is genuinely not tractable, and it should
be said plainly.** Of *MRCS, HSPICE, NGSPICE, Verilog, Qucs, gEDA, KiCad,
OrCAD EE PSpice*: Verilog **ships**; gEDA is the **best** item in the list and
is 5–8 weeks; KiCad is reached **through** gEDA at zero extra cost; ngspice is
reached **today** through `yosys write_spice`. But **the SPICE family as a
target for JLS to speak natively — ngspice, HSPICE, PSpice, Qucs — does not
work**, and not for want of effort: a SPICE deck's leaves are devices and a JLS
element is a logic function. That is a structural mismatch, not a budget. The
one SPICE leaf-set that is not devices (XSPICE digital) is single-bit against
JLS's words. HSPICE and PSpice are additionally proprietary — though note
carefully that this makes them unusable as *sources of code*, not as *consumers
of a file JLS writes*, and the licence question for the whole open SPICE
ecosystem is **closed and favourable** (§8).

**"MRCS" is answered; one list item could not be resolved.** MRCS is identified
(MixedRadixCircuitSynthesis) and determined above. But `fmt-kicad-geda.md` could
not identify "MRCS" as an *EDA* format or tool in the schematic/PCB sense — if
the maintainer meant something else by it there, **put it back to the maintainer
rather than guess.**

---

## 8. LICENCE AND ABSORBABLE-CODE REGISTER

**The headline, and it is favourable:** under D8, JLS (GPL-3.0-or-later) may
absorb code from **every** reference implementation this plan touches. There is
no GPL-incompatible obstacle anywhere on the maintainer's list.

| Source | Licence | Maintenance | Absorbable? | Value |
|---|---|---|:---:|---|
| **Yosys** | **ISC** | active | **YES** | `backends/json/json.cc` is the normative shape for W1.3. **But the correct use is as an external CONSUMER, not a port** |
| **`fstapi.c`** (GTKWave/libfst) | **MIT** — `SPDX-License-Identifier: MIT`, © 2009–2023 Tony Bybell, **verified verbatim** | GTKWave active (nightly 21 Jul 2026) | **YES — transliterate outright with attribution** | W4.5. **Corrects `docs/standards-adoption/07`, which calls this licence "unverified and load-bearing"** |
| **OpenSTA** `power/SaifParse.yy`, `SaifLex.ll` | **GPL-3.0-or-later** — identical family to JLS | active | **YES** | The SAIF grammar without buying any paywalled document |
| **lepton-eda** `liblepton/src/{net,component,pin,text}_object.c` | **GPL-2.0-or-later** | active (distro-packaged) | **YES** | The normative reference for byte-exact gEDA record emission. **Absorb as a reference, not linked code — Mode 3 needs no runtime dependency** |
| **KiCad** `sch_io_geda.cpp` (4,883 L), `kicad_netlist_reader.cpp` | **GPL-3.0-or-later** — identical to JLS | active (10.0.5, 2026-07-21) | **YES** | The authoritative statement of which gEDA constructs survive a round trip into KiCad, and the only real definition of a minimum valid KiCad netlist |
| **iverilog**, **GHDL** | GPL-2.0-**or-later** | active | **YES** (the "or later" is what makes it so) | Already the CI consumers |
| **Verilator** | LGPL-3.0-only **OR** Artistic-2.0 | active | **YES** (either) | Levelization technique for P8; nothing needed for *this* plan |
| **nextpnr / icestorm** | **ISC** | active / maintenance | **YES** | Already in the shipped iCE40 path |
| **VTR/VPR**, **ABC**, **netlistsvg** | **MIT** | active | **YES** | Living BLIF description; the Yosys JSON schema as a test fixture |
| **yosys2digitaljs / DigitalJS** | **BSD-2-Clause** | active (0.10.3, 2026-02-24) | **YES** | Consumers of W1.3, not code to port |
| **Accellera** IP-XACT XSDs, **SystemC**, **FIRRTL spec**, **CIRCT**, **Tiny Tapeout templates** | **Apache-2.0** (CIRCT: + LLVM exception) | active | **YES** | Schemas, fixtures, the fixed `tt_um_*` interface |
| **Qucs** `qucs-core/src/converter` (`parse_spice.ypp` 972 L) | **GPL-2.0-or-later** | upstream dormant | **YES**, but **rejected on COST** | A declarative Bison grammar is the best available inventory of the SPICE3f5 surface. Only relevant if a SPICE reader is ever wanted — §4.4 says it is not |
| **ngspice** front end (~15,300 L C) | **Modified BSD** (+ MIT/PD/LGPL/MPL fragments — **all one-way-compatible into GPL-3**, including MPL-2.0 via §3.3) | active (45.2, 2025-09-06) | **YES**, but **DO NOT — rejected on COST, not licence** | It is not a parser in the portable sense: an imperative list-mangling pipeline over global state whose entire value is accumulated dialect quirks. Porting is a rewrite with a reference, and the 93.0/92.0/84.5 + 80/82 gate would apply to all 15k lines' worth of Java. **This is the correct application of D8's replacement axis: "no" on cost, recorded as a cost judgment rather than as policy** |
| **ngspice `src/xspice`** | **PUBLIC DOMAIN** | active | YES | Recorded so the option is not lost if XSPICE emit is ever revisited |
| **Xyce / XDM** (Sandia) | **GPL-3.0-or-later** | active | **YES** | XDM's value is **documentary**: the authoritative catalogue of PSpice/HSPICE/Spectre→SPICE3 differences encoded as data. Its mere existence — a national laboratory funding a compiler with a six-edition manual — is the sharpest available quantification of SPICE dialect divergence |
| **KLayout / Magic / Electric VLSI** | GPL-2.0-or-later / BSD-style / **GPL-3.0-or-later** | all active | YES | Gated on the cell-and-layout data layer, not on licence |
| **Surfer** | **EUPL-1.2** | active | **YES** (EUPL-1.2 is explicitly GPL-compatible) | Recorded because a prior assumption that Surfer is GPL is wrong; the practical conclusion is unchanged |

### ⚠ Four hazards

1. **Kactus2** (`kactus2/kactus2dev`): distributed under **GPL-2 WITH a
   commercial dual-licence option** — a business model coherent only under
   **GPL-2.0-ONLY**, which is **GPL-INCOMPATIBLE with GPL-3 and must NOT be
   ported into JLS.** `docs/standards-adoption/08-ipxact-export.md:117` cites it
   as "GPL-2.0" without flagging this. **Read its `COPYING` before any
   absorption.** Usable as an external IP-XACT *validator* regardless.
2. **Logisim-Evolution and Digital** carry bare "GNU GPLv3" notices with **no
   "or later"**. Absorbing GPL-3.0-**only** code into GPL-3.0-**or-later** JLS is
   legally compatible but **silently costs JLS its own "or later"**. Three
   options: accept and record it in `REUSE.toml`/`README`; **ask upstream**
   (the wording is ambiguous enough that the answer could be yes); or re-derive
   at higher cost. **Re-deriving is RECOMMENDED AGAINST** — "clean-room"
   reimplementation from GPL source is a *worse* legal position than simply
   complying with the GPL, and it is far more error-prone in exactly the
   `if (inputs == 4 && size >= 60)` cases that matter.
3. **RVVI** (`riscv-verification/RVVI`): **no licence file detected.** Formats
   are not copyrightable, so implementing RVVI-TEXT from the published EBNF is
   not a licensing question; **absorbing any RVVI source is.**
4. **KiCad's symbol/footprint LIBRARIES** are **CC-BY-SA-4.0** — a different and
   stricter permission from KiCad's GPL-3 code, though with an explicit waiver
   leaving *designs and generated files* unencumbered. **Fritzing's part artwork
   is likewise CC-BY-SA while its code is GPL-3.** Do not conflate them.

---

## 9. THE HONEST TOTAL

### 9.1 The sum

| Block | Weeks | New primary data? |
|---|---:|---|
| **Wave 0** — ships this month | **3.5–5.5** | none |
| **Wave 1** — the projection engine | **8–12** | none |
| **Wave 2** — hierarchy | **7.5–10.5** | none |
| **Wave 3** — the differential oracle | **5–8.5** | none |
| **Wave 4** — the outward views (incl. FST) | **11–17.5** | none |
| **Wave 5** — depth on shipped paths | **10.5–15** | none |
| **Subtotal, ZERO new primary data** | **45.5–69** | **none** |
| **Wave 6** — bounded new data (P2/P3/P4 slices) | **9–14.5** | bounded |
| **Wave 7** — Tiny Tapeout demonstration | **1.5–2** | bounded |
| **THE CORE PROGRAMME** | **56–85.5** | |
| **Wave 8** — Logisim/Digital migration | **16–24** (+6–9 for `.dig`) | none |
| **Wave 9** — conditional / demand-gated tail | **17–26** | mixed |
| **EVERYTHING, if every option is taken** | **89–135.5** | |

**Recommended commitment: the core programme, 56–85.5 maintainer-weeks — thirteen
to twenty maintainer-months.**

### 9.2 Against the committed roadmap

The brief cites 281–410. **`AMENDMENT.md:979` says 288–424** (66–98
maintainer-months); the brief's figure is stale by one revision and I use the
committed number.

| Measure | Weeks | % of 288–424 |
|---|---:|---:|
| Wave 0 alone | 3.5–5.5 | **~1%** |
| Waves 0–2 (the answer to the maintainer's question) | 19–28 | **5–10%** |
| Zero-new-data total (Waves 0–5) | 45.5–69 | **11–24%** |
| The core programme (Waves 0–7) | 56–85.5 | **13–30%** |
| With migration (Waves 0–8) | 72–109.5 | **17–38%** |

**But 16–23.5 of those weeks are NOT additive.** They are format-relevant
*slices* of programmes the roadmap already commits to and already counts:
hierarchy IR + type identity (P3, 5.5–8), `INOUT` (P3, 1–2), port roles (P3,
2–3), VLNV (P3, ~1.5), reset (P2, 3–5), timescale (P4, 0.5–1), activity/SAIF
(P5, 2–3). Adopting this plan **re-orders** those weeks; it does not add them.

**Net new spend, honestly: ~35–53 maintainer-weeks** — eight to twelve
maintainer-months at bus factor 1.

### 9.3 What it displaces

**It does not fit alongside everything, and pretending otherwise would be
dishonest.** At bus factor 1, ~35–53 net-new maintainer-weeks is most of a year.
Three specific consequences:

1. **It displaces the start of P8 (the compiled engine, 24–35 wk) by roughly a
   year.** That is the direct competitor: P8 is the other single item that would
   consume a comparable block, and #221's revisit trigger is now quantitatively
   met. **This is the real trade and the maintainer should make it consciously.**
   Note the asymmetry: P8 buys speed on the structural tier (a measured 2.26×
   semantics-preserving, 4.9× full stack, moving boot from ~1.7 h to ~20–45 min
   and keystroke echo from 1.5 s to 0.30 s/char) and buys **zero formats**; this
   plan buys the entire projection surface and **zero speed**.
2. **It displaces P9 (causal debug, 19–27) and P10 (fault simulation, 12–18)
   entirely for the period** — with one credit: this plan delivers the VCD
   reader, first-divergence comparator and differential oracle that P9 would
   otherwise have to build, and SAIF's toggle counters are P5/P10-adjacent
   infrastructure.
3. **It does NOT displace #77.** Nothing here is technically gated on the core
   extraction. Under D6 the calendar is, but no dependency is — which means if
   #77 slips, Waves 0.1 and 0.2 still land and the rest queues without rework.

**Three ways to make it fit, all honest:**

- **The minimum honest answer (19–28 wk, one to two quarters):** Waves 0–2. This
  delivers Yosys JSON → netlistsvg + DigitalJS + Yosys's EDIF/BLIF/SPICE
  backends, hierarchical Verilog and VHDL, SubCircuit export, name-stable output,
  a closed policy hole, and three documented Yosys recipes. **It answers the
  maintainer's question in full at 5–10% of the committed roadmap.**
- **The demonstration cut (add Waves 6–7, +10.5–16.5 wk):** ends with a
  student's drawn circuit on a real SKY130 die.
- **The migration cut (add Wave 8, +16–24 wk):** ends with an existing
  Logisim course migrating into JLS. This is the only block that moves **users**
  rather than **files**, and it is also the only block whose estimate is not
  calibrated against anything in-tree — treat 12–18 as a band, not a number, and
  **run the 2-day corpus measurement first**.

**Wave 8 should not be committed on cost estimate alone.** It is the largest
single item in the plan and the least measured.

---

## 10. RE-OPENED QUESTIONS AND MAINTAINER DECISIONS

### 10.1 THE N-ARY REOPENING — the one the brief asked for by name

**`07-mvl-determination.md` §5 records, verbatim: *"An external oracle exists.
It is manual — a human authors the design twice — so it is weaker than the #202
differential golden, and I state that limit rather than overclaim."***

**What changed: format adoption supplies a SINGLE-SOURCE, AUTOMATED oracle that
the determination believed structurally unavailable.** The determination's
oracle was *MRCS* — a second human-authored design in a dormant Unity/C#
project, compared by hand. That is dual authorship, not a differential oracle.
The loop this plan builds is different in kind:

> a JLS N-ary circuit → `RadixBridge` BET lowering → **JLS's own shipped
> `VerilogEmitter`** → the **testbench emitter** (W3.1) driven by the **same
> `-t` vectors** → `iverilog` (already in CI) → VCD → the **first-divergence
> comparator** (W3.2) against JLS's own VCD from the same run.

**One source, one stimulus, machine-compared, in CI, with no second human
author and no MRCS.** Cost: **zero incremental** — W3.1 and W3.2 are built for
binary circuits and the BET lowering makes the ternary case fall out, because
BET-lowered ternary *is* ordinary binary Verilog over a documented encoding.

**Three consequences to record:**

1. **`07-mvl-determination.md` §5's oracle sentence should be corrected**, one
   sentence: lead with Icarus-on-BET-Verilog (single-source, differential,
   automated) and **demote MRCS to an optional dormant dual-authorship
   cross-check**. This is `fmt-mrcs-and-accessible.md` §1.3's recommendation and
   I concur.
2. **The verdict does not change — it strengthens.** The determination's verdict
   is *"Do it. Radix 2, 3 and 4, natively"* (`:19-21`), and it is itself a D10
   re-derivation replacing a prior "no". **The brief mis-states this**: the
   "BET circuit library for ~1–2 weeks" position it attributes to the
   determination appears at `:872-875` inside **§10 THE HONEST CASE AGAINST** —
   an argument the file records and **rules against**. Downstream phases must
   not inherit the brief's version.
3. **The oracle strength argument was doing real work in that determination.**
   Angles 3 and 4 argued *"an N-ary circuit can never leave JLS; HDL export
   refuses permanently"*, and the response leaned on the manual MRCS oracle.
   That response is now unnecessary — the oracle is automated, in-tree, and
   costs nothing beyond work already justified for binary circuits.

### 10.2 Other reopenings, each with what changed

| Prior determination | What it said | What changed | New status |
|---|---|---|---|
| **`docs/standards-adoption/07`** — EVCD | a "closed negative" | Its own two revisit triggers are **exactly P1's strength stage and P2's `BidirPin`**, both real, scheduled programmes | **Sequenced, not closed.** Restate as needs-data-first under D10. The *honesty* argument survives and is the right one |
| **`docs/standards-adoption/07`** — FST | deferred; licence "unverified and load-bearing"; zlib-only acceptance "the load-bearing unknown"; gzip on `-vcd` was "the recommendation" | **Licence verified MIT.** **Zlib-only measured: valid, and the SMALLEST of three pack types.** **Surfer measured to REFUSE `.vcd.gz`** | Defer verdict **survives on cost only**; its stated reason is wrong. gzip demoted to a GTKWave-only convenience |
| **`docs/standards-adoption/11`** — EDIF | an emitter "walks the same `HdlModel`" and is "genuinely small" | **Wrong on a technical fact**: EDIF's NETLIST view is instance-of-cell | Reason replaced. **Verdict unchanged, and now correctly grounded** |
| **`docs/standards-adoption/08`** — IP-XACT | seven blockers | **Two clear**: hierarchy removes blocker 7; the type digest solves blocker 2 as a side effect. **The two that clear were the two that were structural** | **Blockers 7→5, all five now new primary data. Reclassify from refuse to needs-data-first** |
| **`sweep-01:50`** — SAIF | "V1 — blocked on the value domain; two of the four fields are permanently zero" | **Exactly ONE (`TX`) is.** `TZ` is real because JLS has HiZ and `WireNet.propagate` resolves it; T0/T1/TC are all real | **`sweep-02:123`'s "the cheapest real item" rating is correct.** SAIF moves up |
| **`sweep-06:82`** — SPICE gate | gated on P6-D (technology cells) | The blocker is **hierarchy**, not cells; and the gate reason should read **"cells to name AND ORDER"**, because positional pin binding is harder and **silent when wrong** | **Move the gate from P6 to P3; strengthen the stated reason** |
| **The prior KiCad-netlist verdict** (D10 flagged it: *"looks front-end, is a new tool"* — an argument about completeness dressed as one about tractability) | KiCad is a new tool | **The wrong artifact was tested.** The netlist is mechanically dead-ended (`board_netlist_updater.cpp:151-160` refuses footprint-less components; KiCad has **sixteen** schematic importers and **none is a netlist reader**; CvPcb merged into eeschema so Assign-Footprints operates on a *schematic*). But **KiCad 10 ships a gEDA importer** and gEDA **embeds symbols** | **The verdict flips for the schematic path.** gEDA `.sch` is 5–8 wk, needs no library, and reaches KiCad *and* lepton *and* 30+ netlist backends. The `.net` refusal survives, now on the correct mechanical grounds |
| **`#68`** — FST | deferred | Only the *reason* changes (MIT, not unspecified reverse-engineering) | Defer survives on cost |
| **`#221`** — the discrete-event interpreter as sole strategy | revisit trigger: "a CPU-scale design unusably slow interactively" | Trigger quantitatively met — **but this plan is orthogonal.** It buys zero speed and P8 buys zero formats | **Not reopened here. Named explicitly as the displacement trade (§9.3)** |
| **`#63` / `docs/vcd-interop.md:19-24`** — live co-simulation rejected | graders must not depend on interacting with a running simulation | This plan's oracle is **batch** — two finished files diffed offline | **Not a conflict — but confirm the reading explicitly**, because the adjacency is close enough to be mistaken for the rejected thing |
| **`CellValidator.java:140-143`** (2026-07-17) | the `$adff` family stays on the teachable-reject list "until corpus evidence justifies a `Register` pin" | Tiny Tapeout is an **argument**, not corpus evidence | **Revisit explicitly rather than assume overtaken.** W6.4 depends on the answer |

### 10.3 Decisions only the maintainer can make

1. **Uniquified or deduplicated hierarchy in v1?** Uniquified is 4–6 wk and
   forfeits the "one module reused N times" lesson — arguably the point of
   teaching hierarchy. I recommend shipping uniquified with the emitted header
   stating plainly that instances are uniquified, then adding the digest. **This
   is a pedagogical call, not a cost one.**
2. **Is nested `$scope` a `batch-interface.md` §6 contract break worth taking
   now?** It moves bytes for every existing VCD consumer. Major bump,
   compatibility flag, or leave the flat-dotted form frozen forever. **Decide
   before the streaming rewrite (W1.1) touches the same code, or the emitter is
   rewritten twice.**
3. **Falsify the gEDA embedded-symbol claim before scoping W4.1** — one
   afternoon, a hand-written ten-line `.sch` with one `[ … ]` block, opened in
   KiCad 10. The path is in the code but not in the manual.
4. **Does W1.3 (Yosys JSON write) create a round-trip obligation?** JLS would
   both write and read the format; the reader realizes 5 of 19 accepted cells,
   so the round trip fails loudly on almost anything. **Publish an explicit
   no-round-trip claim, or fund W5.2 alongside.**
5. **Confirm that a batch differential oracle does not conflict with #63.**
6. **Is there a NAMED consumer for a JLS-emitted structural SPICE deck?** If an
   instructor wants netgen-style LVS comparison, the ~1-week emitter becomes
   worth building. Without one it buys a shorter command line than
   `yosys write_spice`.
7. **Assign owners to the four unowned items** (§6.1–6.4), especially the retire
   event, which needs a program number.
8. **Revisit `CellValidator.java:140-143` explicitly** before W6.4.
9. **Ask Logisim-Evolution and Digital upstream whether "GPLv3" means "or
   later."** Blocking for the port-geometry absorption route only.
10. **Adjudicate the STIL contradiction** (`sweep-06:137` vs
    `fmt-waveform-verif.md` §4.8). One of them is wrong.
11. **Is a pre-1.0 external format (RVVI-TEXT v0.5) an acceptable contract
    surface** for a document that promises bit-identity? My recommendation is
    yes, profiled and version-pinned via the format's own `VERSION` element.
12. **Is `xz` on `-vcd` worth one sentence of a recorded decision?** Measured
    13.9× — better than FST-zlib — and `org.tukaani:xz` is already shaded in.
    No viewer reads it, so it would be JLS-only. Probably no; worth recording
    rather than leaving silent.
13. **`-vcdfrom` / `-vcdto` windowing appears in NO roadmap document** and may
    beat every container change on a long run — the dominant lever in the boot
    arithmetic is signal-set and time-window restriction, not compression.
    **Worth costing (guess: 3–5 d) before W4.5 is committed.**
14. **Cross-platform run determinism is still unasserted**
    (`parity-contract.md` §5.1). Every byte-exact claim in this plan inherits
    that gap. The experiment is cheap and the CI jobs exist: run one circuit on
    all three platforms and diff the VCDs.

---

## 11. UNVERIFIED AND SECONDARY CLAIMS — read before citing

Stated so nothing here is mistaken for measurement:

- **Egress refusals.** Several hosts returned 403 at this session's proxy:
  `qucs-s-help.readthedocs.io`, `spiceopus.si`, `xyce.sandia.gov`,
  `deepwiki.com`, `blog.timhutt.co.uk`, and the GitHub contents API. Facts
  sourced from search-result extracts — the Xyce "not 100% netlist compatible"
  quote, the ngspice XSPICE 12-state node model, the XSPICE code-model list, and
  the KiCad Pin Assignment dialog — should be re-verified before being cited as
  settled.
- **The EDIF 2 0 0 primary document was not fetched.** `edifLevel` /
  `keywordLevel` semantics remain unverified, as
  `11-costed-rejections.md:259-261` already flags. Moot under the Yosys recipe.
- **lepton-eda's "over 30 netlist formats" is the project's own README claim.**
  Count it from a checkout before quoting the number in a project document.
- **Wave 8's 12–18 week band is an analogy, not a measurement** — line-count
  scaling against the in-tree Yosys import path. The corpus measurement (W8.0b)
  has not been done.
- **All cost figures are analogies** at ~200–250 lines of shipped-and-tested code
  per maintainer-week, and inherit the roadmap's own caveat.

---

## 12. HEAD VERIFICATION LOG

Re-run this session at `b299d63`:

| Claim | Command / read | Result |
|---|---|---|
| Element registry size | `ElementRegistry.java:39-77` | **35** types |
| Export policy buckets | `HdlExporter.java:422-438` | EXPORTED **22**, SKIPPED **6**, TOPOLOGY **4** (incl. `Wire`, unregistered) |
| Policy totality | 22+6+4−1 = **31 of 35** | Unclassified: `FieldExtend`, `Memory`, `RegisterFile`, `SubCircuit` |
| `HdlModel` statement kinds | `HdlModel.java:143-201`, `grep -c "void visit"` | **11**, not ten. None instantiates a module |
| Single-module IR | `HdlModel.java:891` | one `moduleName`; one flat statement list |
| `Direction` | `HdlModel.java:27-33` | `{INPUT, OUTPUT}` — **no INOUT** |
| `Register` reset | `grep -n reset src/jls/elem/Register.java` | Only `resetPropDelay` (`:508-511`) and doc comments. **No reset attribute, no reset input** |
| Yosys cell acceptance | `CellValidator.java:58-68` | **19** cell types |
| Yosys cell realization | `NetlistImporter.java:234-258` | **5** (`$not`,`$and`,`$or`,`$xor`,`$mux`); default arm apologises verbatim |
| Hierarchy import | `NetlistImporter.java:228-232` | "flatten the design in Yosys and re-import" |
| Three renderings, one port set | `PcfEmitter.java:73,155,190`; `VerilogEmitter.java:93,110`; `VhdlEmitter.java:123,1007` | All walk `model.ports()`. **The projection thesis is shipped** |
| Subcircuit inline save | `SubCircuit.java:283-287` | `getSubCircuit().save(output)` |
| `stableId` unused by export | `grep -rn stableId src/jls/hdl/ \| wc -l` | **0** |
| No subprocess in shipped code | `grep -rl ProcessBuilder src/ \| wc -l` | **0** (test/: **15**) |
| Nominal time precedent | `BatchSimulator.java:423` | `out.append("$timescale 1 ns $end\n")` — hard-coded |
| Probe names unvalidated | `Wire.java:462-468` vs `Util.java:219-234` | `attachProbe` accepts any non-empty string; `isValidName` never called |
| `-export` dispatch | `JLSStart.java:1088-1090` (whitelist) and `:382-385` (ternary) | **Correction to `fmt-hdl-netlist.md`**: a typo *is* caught at parse time (`.v`/`.vhd`/`.vhdl` only). The real issue is that **a third emitter must extend BOTH sites** |
| Extension seam is open | `HdlExtensionPoints.java:23-26` | `ExtensionPoint<HdlEmitter> EXPORTER`, published, many-cardinality — **no new `docs/extension-points.md` row is needed for a fourth emitter** |
| Shipped file sizes (cost calibration) | `wc -l` | `PinBindings` 98, `PcfEmitter` 199, `VerilogEmitter` 752, `VhdlEmitter` 1149, `HdlExporter` 1364, `YosysNetlist` 953, `JsonValue` 580 |
| Committed roadmap total | `AMENDMENT.md:979` | **288–424 weeks** (the brief's 281–410 is stale) |
| Program start status | absence-grep for `LogicValue`, `Multiplier`, `Divider`, `InOutPin`, `InstanceStatement`, `DelayModel`, `Liberty`, `CellInstance`, `jls.core.elab`, `int phase`, `class Assert`, `SAIF` | **all zero — every program in the ownership column is UNSTARTED** |

---

## 13. THE ANSWER TO THE MAINTAINER, IN ONE PAGE

**You are right, and the code already proves it.** Mode 3 — speak the format —
needs no engine, no subprocess, no runtime dependency, no toolchain install, no
determinism break, no `jls.edit` change and no draw on the coverage commons. One
`HdlModel` walk already feeds three renderers in three unrelated syntaxes. A
fourth is one class at a seam that is already published.

**You are right about tractability, and more so than this study expected.** The
biggest supposed blocker — hierarchy — turns out to need **zero new primary
data**: `SubCircuit.save` already writes the nested circuit inline. And the
single best item in the plan is one nobody had proposed: **write Yosys JSON, 3–4
weeks**, which makes netlistsvg, DigitalJS and Yosys's own EDIF, BLIF and SPICE
backends reachable at once and **dissolves three costed rejections without JLS
owning any of them.**

**The list, item by item.** *Verilog* ships. *gEDA* is the best thing on the list
and is 5–8 weeks — and because KiCad 10 imports gEDA and gEDA embeds its symbols,
*KiCad* comes free with it and **no library is curated**. *ngspice* is reachable
**today** through `yosys write_spice`, at documentation cost. *MRCS* resolves to
"emit Verilog", which JLS does. *HSPICE*, *PSpice* and *Qucs* are the exception,
and the reason is structural rather than budgetary: **a SPICE deck's leaves are
devices and a JLS element is a logic function.** Every other format on your list
has leaves that are logic functions, which is exactly why the rest works.

**What it costs.** The answer to your question in full — Waves 0 through 2 — is
**19–28 maintainer-weeks, 5–10% of the committed roadmap, all of it zero-new-data.**
The whole realistic projection surface is **56–85.5 weeks**, of which **16–23.5
are already committed inside P2/P3/P4/P5 and merely re-ordered**, so the net new
spend is **35–53 weeks**. **That is most of a year at bus factor 1, and it
displaces the start of P8, the compiled engine.** P8 buys speed and no formats;
this buys the formats and no speed. That is the trade, and it is yours to make.

**What ships this month, needing nothing:** a total export policy, validated
probe names, name-stable HDL output, a machine-readable export report, three
documented Yosys recipes that convert costed rejections into shipped
capability, the parity trace-format decision made for free, and one FPGA
constraint emitter. **Three and a half to five and a half weeks, and it starts
Monday.**
