# 08 — DETERMINATION: the multi-view / SPICE extension

**Re-run under D10.** This document replaces the prior version, whose verdict was
reached by the forbidden reasoning D10 names (tracker `total_count: 0`, demand
gates aimed at the maintainer's own roadmap, "looks front-end, is a new tool"
presented as tractability when it was completeness). The survey evidence from all
eight angles is kept. The verdict is re-derived as **a path and a cost for every
view**.

Every HEAD claim below was verified in the working tree at `/home/user/JLS`,
commit `b299d63`, this session. Where a roadmap document is stale, the stale text
is cited and corrected. External claims carry URLs; where a page returned 403
through the agent proxy that is stated at the citation.

---

## 0. THE VERDICT, ONCE

**The maintainer's reframe is the right organizing principle and it is cheaper
than either of the two things this study had been comparing.** Adopt the open
formats and let other tools be the views: KiCad is the PCB view, KLayout is the
layout view, GTKWave/Surfer are the waveform view, ngspice is the analog view,
any text editor is the HDL view. That route costs a *printer* per view — one to
four weeks each — instead of a canvas (6–10 weeks each plus a 7–11 week
first-view tax) or a subprocess (a permanent tool-version matrix against a single
offline jar). It also draws **nothing** from the `jls.edit` coverage commons,
which is the binding constraint on this whole program and the one hard arithmetic
limit in the study.

Against that principle, the six named views sort cleanly:

| View | Status | Route | Marginal cost |
|---|---|---|---|
| **Drag-and-drop gate placement** | **SHIPPED.** This is JLS | — | 0 (+1 wk for per-view palettes) |
| **FPGA integration** | **SHIPPED and never validated** | printer + existing script | **~1 wk** to validate; 5–8 wk (P3) for the real gate |
| **Text/HDL** | **SHIPPED one-way, no GUI surface at all** | format; the editor is the view | 1–2 wk defects, +2–3 wk read-only pane |
| **PCB** | Not shipped | **emit a KiCad netlist; KiCad is the view** | **5–8 wk** — the maintainer's correction is right and this study had it wrong |
| **Lithography / masks** | Not shipped | **link the CI-produced GDS; KLayout is the view** | **0 wk** for the view; 11.5–18 wk for the silicon path that ends in a chip |
| **Breadboard** | Not shipped | **no format route exists** — this is the one genuine canvas | 4–6 wk for the useful half without a canvas; +15–24 wk with one |

**Four of the six are printers or already exist. One is free. One is a canvas.**
That is a materially different answer from "six tool classes for one maintainer,"
and it is the answer the maintainer's own reframe produces.

Three things are genuinely refused with arithmetic, and each names what would
work instead: a bundled footprint/symbol **geometry** library (KiCad's ~15k
curated footprints, free, versus one maintainer owning every part shipped,
forever — emit the footprint *name string* instead, ship zero geometry); a GDSII
reader (re-derived at 12–19 weeks against KLayout, which is free and better —
link the CI-produced GDS instead); and automatic connect-module insertion
(Verilog-AMS's headline feature silently inserts a module the student never drew,
which K9 forbids more absolutely than any cost argument — draw the Dac and Adc by
hand instead).

**The one hard limit.** The `jls.edit` coverage commons caps the number of
canvases at two or three, not six. Measured bundle headroom before the JaCoCo
floor trips is ~2,897 uncovered LINE and ~1,475 uncovered BRANCH units *total,
for all future untested code* (bundle floors `pom.xml:355-372` at
0.545/0.535/0.505); a minimum canvas is ~850–1,350 executable lines and is nearly
all branches. `jls.edit` is 23,910 lines and is **deliberately unfloored**
(`pom.xml:409-411`) until #91/#84 make editor code testable, and the
raise-with-your-PR ratchet in the same comment forbids lowering a floor to make
room. **#84 is therefore not a nice-to-have adjacent to this program; it is a
precondition for the canvas half of it** — and `SimpleEditor` is **5,852 lines at
HEAD** against the 4,119 in its own issue title, i.e. it grew 42% while the issue
sat open.

---

## 1. THE PATH — six views, with costs, order, and what each displaces

The question asked of each view, in this order:

1. Is it already shipped? (Then validate it, do not rebuild it.)
2. Is there an **open format** an existing free tool already renders? (Then emit
   the format. This is the maintainer's reframe and it is usually 10x cheaper.)
3. If not, is the data **authored** — does it have no preimage in the design
   model? (Then it needs a canvas, and canvases are what the coverage commons
   caps.)

### 1.1 Drag-and-drop gate placement — SHIPPED. Move on.

This is JLS. `src/jls/edit/Palette.java` defines 32 entries in 8 groups;
`SimpleEditor` runs the mouse machine. Listing it in the ask tells us the list was
assembled from a genre catalogue rather than from the tree, which is worth saying
once and then dropping.

**One real item hides here, and it is the cheapest concrete deliverable in the
whole study.** D9 restates K9 as *progressive disclosure* — "views are
default-hidden and opt-in; palettes are per-view, not one growing global palette."
**A green test at HEAD forbids that.**
`test/jls/edit/PaletteContractTest.java:47-65`,
`paletteIsTotalOverTheElementRegistry`, asserts the palette has *exactly one entry
per registered element type* outside `Set.of("SubCircuit","WireEnd","TestGen")`
(`:44-45`). So a breadboard part or an analog bridge element that is a registered
`ElementType` — which it must be, or it cannot be saved or loaded — **lands on the
first-year's toolbar, enforced by a passing test**. And if it is not registered,
it cannot round-trip.

**Build:** give `PaletteEntry` and the contract test a view dimension; totality
becomes "exactly one entry in exactly one view's palette." **~1 maintainer-week.**
Hard prerequisite for the *first* view of any kind and invisible in every roadmap
document. **Displaces: nothing.**

### 1.2 Text / HDL — SHIPPED one-way; the format IS the view; two live defects

**Shipped:** `HdlExporter` + `VerilogEmitter`/`VhdlEmitter`, CLI `-export`.

**Verified at HEAD: there is no HDL surface in the GUI at all.**
`grep -rln "HdlExporter\|VerilogEmitter\|PcfEmitter" src/jls/edit/ src/jls/ui/`
returns nothing; the only call sites are `src/jls/JLSStart.java:418-425`. A
capability that ships and cannot be reached from the editor can never accumulate
usage evidence. **Adding the export menu item costs hours** and is the only
demand experiment in this entire study that is nearly free — see §6.

**The format route needs no work at all.** Verilog is the interchange; every
editor, Yosys, Icarus, GHDL and Verilator are already the view. Nothing to build.

**Two live defects, both of which every other projection also rides:**

**T0a — the export policy is not total over the registry, and two elements
shipped in #201 fall through the hole.** Verified this session by reading the
policy sets at `src/jls/hdl/HdlExporter.java:421-437` against
`src/jls/elem/ElementRegistry.java:38-76`:

- `ElementRegistry.ALL` = **35** types.
- `EXPORTED` = **22** classes (`:422-428`), and it **does** include
  `ShiftRegister` — correcting `README.md:33` and `sweep-06:40`, both of which say
  it is omitted, and correcting `README.md:36`'s "omits SubCircuit, Memory and
  ShiftRegister."
- `SKIPPED` = 6 (`:431-433`); `TOPOLOGY` = 4 (`:436-437`), of which `Wire` is not
  a registry type.
- Registry types covered: 22 + 6 + 3 = **31**. Uncovered: **`FieldExtend`,
  `Memory`, `RegisterFile`, `SubCircuit`** — all four hit the offender throw at
  `:191-197`, which calls them *unrecognized*.
- `Memory` and `SubCircuit` are deliberate and pinned
  (`HdlPolicyTest.java:63,77`). **`RegisterFile` and `FieldExtend` are not.**
  `grep -rn 'RegisterFile\|FieldExtend' src/jls/hdl/ test/jls/hdl/` returns
  nothing.

`RegisterFile` is the element BRIEF §13 records as *deleting* the parity machine's
register-file design choice (98 elements → 1; 18.00 → 6.94 events/cycle). **The
element that made a CPU drawable is the element that cannot leave the tool, and
no test says either way.** Fix: a totality test over `ElementRegistry.ALL`
asserting every type lands in exactly one bucket, in the exact idiom of `970db41`'s
registry→`SaveTags` test one commit ago, plus a bucket decision for the two.
**1 day.**

**T0b — the generated HDL is not a pure function of the circuit's logic.**
`HdlExporter.orderedElements` sorts by `Element::getID` (`:1292-1293`) and names
nets `net_<getID()>` (`:346`) / `net_u<id>` (`:374`). `Element.java:20-21`:
*"The file-local reference index, reassigned on every save."* Angle 7 measured the
committed goldens: of 642 non-blank lines across the 32 `test/resources/hdl/*.v`
files, **139 (21.7%)** carry either a dense-id-derived identifier (104, 16.2%) or
literal element pixel coordinates in a comment (35, 5.5%). So dragging one gate
rewrites the Verilog and inserting one element renames every net. The fix uses
mechanisms that already ship: `Circuit.canonicalOrder()` (sorted by stable id) and
`getStableId()` (#165/#166). **This is D2 applied to the projection D2 forgot,**
it is worth doing with no view at all, and it is the precondition for P3's
round-trip CI property being *comparable*. **1–2 wk including re-baselining 32+
goldens.**

**T1 — a default-hidden, opt-in, READ-ONLY generated-HDL pane with bidirectional
cross-probing** (click a statement → select the element; select an element →
scroll to the statement), debounced, regenerated off the EDT. **2–3 wk after T0.**
Frame it with hneemann's own already-in-tree line that HDL export is a vehicle for
an FPGA, not a way to teach the HDL (`docs/hdl-support-research.md:118-122`).

**Editable HDL pane: refused, with the alternative named.** Not on precedent — on
two arithmetic facts. (a) **Zero of six surveyed tools does it**, including two
commercial flagships: Vivado's block design generates a one-way RTL wrapper and
its elaborated schematic is an analysis view; Quartus BDF→HDL reverses only to a
symbol; Simulink/Embedded Coder offers bidirectional *traceability* and says so;
LabVIEW has no text form; Logisim-Evolution is export-only (HDL import is
[logisim-evolution#64](https://github.com/logisim-evolution/logisim-evolution/issues/64),
still open); Digital exports plus GHDL/Icarus black boxes. The **one** true
bidirectional graphical/textual editor found is Modelica/OMEdit, and it achieves
it by **putting the diagram geometry inside the source language** —
`annotation(Placement(transformation(origin=…, extent=…, rotation=…)))` on
components and `connect(…) annotation(Line(points={{-80,30},…}))` on connections
([Modelica specification ch.18](https://specification.modelica.org/)). Verilog
cannot carry that and never will. (b) The **lens** argument: `get:Model→View`
takes one argument, `put:(View,Model)→Model` takes two, and the second is the
*complement* — layout, identity, orientation, probes, `Memory` contents,
hierarchy — which HDL has nowhere to store. **What works instead:** the text view
that IS bidirectional is the **`.jls` file**. It already carries `x`, `y`,
`orient`, `sid` and every attribute, and since #166 the serialized form is a pure
function of circuit content (`docs/file-format.md` §8) — `get = Circuit.save`,
`put = Circuit.load`, the Modelica arrangement, already shipped. Under D1 (plain
text default, already tested), D2 (ref-by-sid) and D3 (per-section versioning), a
validated editor over the `.jls` text whose edits diff into the 11 shipped
stable-id-addressed `CircuitOp` kinds is the honest answer to "text programming."
**It is not a substitute for `jls.api`/P12** — a text view serves a human reading
tens of elements; `jls.api` serves a program generating the ~580-element machine
— and the parity determination already excluded both a new DSL
(`03-determination.md:869`) and HDL-as-source-of-truth (`:871-874`).

**Order:** T0a (1 d) → T0b (1–2 wk) → menu item (hours) → T1 (2–3 wk).
**Displaces:** T0a/T0b displace nothing and land under D6. T1 displaces roughly
half a P5 slice.

### 1.3 FPGA — SHIPPED, and nobody has ever flashed a JLS design

**The strongest evidence for the projection reframe anywhere in the tree, and it
was found rather than argued.** `src/jls/hdl/board/` is 581 lines across 5 files.
`PcfEmitter.java:14-22` states the invariant in its own javadoc — it walks
*"exactly the port set `HdlExporter#buildModel` produced — the same ports the
Verilog/VHDL emitters render, so the constraint file and the HDL can never
disagree about the module's interface"* — and `:73` implements it as
`for (HdlModel.Port port : model.ports())`. **One model, three renderings, in
shipped code.** A fourth projection (LPF, XDC, a Tiny Tapeout wrapper) is a
printer over an existing walk.

**And the stack is unvalidated end to end.**
`docs/icestick-bitstream-handoff.md:115-119` is a table of five `_TBD_` cells;
`grep -rn "nextpnr|icepack|iceprog|openFPGALoader" test/` returns one line and it
is a comment inside the golden PCF itself; the self-test's own note (`:108-111`)
says it *"proves the control flow, not that the exact yosys / nextpnr-ice40 /
icepack arguments synthesize a real design."*

**Path, in order, and the first four are about one week total:**

| # | Item | Cost |
|---|---|---|
| F0 | Arm `yosys` + `nextpnr-ice40` in CI against the shipped `blinky_icestick.pcf` golden, via the existing `ToolLocator` + `Assumptions.assumeTrue` idiom (`test/jls/hdl/GhdlCompileTest.java:35`). Linux apt install and the SHA-256-pinned Windows oss-cad-suite (`ci.yml:159-170`, which ships nextpnr) already exist. `docs/standards-adoption/06-fpga-constraint-formats.md:583-586` names this as slice (0) and it has not been done. | **2–4 days** |
| F1 | Reject `-board` with a non-Verilog `-export` target. Verified at HEAD: `JLSStart.java:908-916` checks that board/pins pair and that `-export` is present, **not the output language**, while the documented flow feeds Verilog to yosys (`scripts/icestick-handoff.sh:145`). A live footgun, one condition. | **0.5 day** |
| F2 | The export policy-totality test (T0a above — shared) | **1 day** |
| F3 | One real iCEstick flash, filling the all-`_TBD_` version record | **1 day + ~$30** |
| — | **THEN the actual gate: P3's export-coverage slice** (hierarchy + `Memory` + `RegisterFile`). "Put your CPU on a board" is blocked by this and by nothing in the constraint layer. Depends on nothing. | **5–8 wk (already funded, P3)** |
| F4 | Generalize `PcfEmitter` → `ConstraintEmitter` + shared `PinBinder`; PCF bytes unchanged | 1–2 wk |
| F5 | LPF + one ECP5 board — the first board with room for a CPU, and the only second format with machine-checkable CI evidence (prjtrellis, ISC) | 1–2 wk |
| F6 | `Board` frequency field + one clock-constraint line. `Board` is `record Board(String name, String fpga, Format format, Map<String,String> pins)` (`Board.java:26`) with no frequency, yet `docs/standards-adoption/06-fpga-constraint-formats.md:35-37` lists a board-clock period constraint under "Explicitly claimed." This retires a false claim as well as adding a feature. | 3–5 days |

**Doing F4–F6 before P3 adds constraint formats for designs that still cannot
export.** Say that plainly in the issue.

**Board reality, to tell an instructor before they plan a lab.** nextpnr is stable
for iCE40, ECP5, Nexus, Gowin, NG-Ultra and GateMate; Xilinx 7-series (Project
X-Ray) is **experimental** and Intel MAX 10 has no open path
([nextpnr README](https://raw.githubusercontent.com/YosysHQ/nextpnr/master/README.md)).
The two most common US teaching boards — Basys 3 (Artix-7) and DE10-Lite
(MAX 10) — are exactly the boards the open flow does not serve. And
`Boards.ALL = List.of(ICESTICK)` (`Boards.java:81`) is an iCE40-HX1K at 1,280
logic cells; a ~580-element machine will not fit even after P3. **If a second
board lands, add it for capacity (HX8K breakout, 7,680 cells, same PCF format, no
new emitter) before variety (ECP5, which needs the F4 refactor).**

**Refuse, with alternatives named:** FPGA place & route (nextpnr, ISC, ~10 years,
free to invoke — and JLS reimplementing it buys nothing teachable); bitstream
packing (the reverse-engineered per-device database *is* the work and is not
reproducible by one maintainer); timing closure / sign-off STA (JLS's timing model
is one scalar `propDelay` per element, so any Fmax it reported would be fiction
relative to the fabric — let nextpnr report it; this is **not** an argument
against P4, which is about JLS's own simulation). **Do not** port
`scripts/icestick-handoff.sh` into `src/` over `ProcessBuilder`: it would be JLS's
first subprocess ever (verified: `grep -rn "ProcessBuilder|Runtime.getRuntime().exec" src/`
returns **0** at HEAD) and it converts 157 lines of shell into a permanent
cross-platform tool-version surface.

**Displaces:** F0–F3 displace nothing. P3 is already funded.

### 1.4 PCB — the maintainer's correction is right, and this study had it wrong

**Re-derived from scratch, as instructed, distinguishing a netlist export from a
bundled library.**

The prior verdict — "a KiCad netlist needs footprints, refdes, values and package
pin numbers, which are primary data JLS does not have, therefore it is a new tool
one P6-cell-layer wide (20–32 wk plus permanent curation)" — **fused two things
that cost an order of magnitude apart, exactly as the maintainer said.** Taking
them apart:

**(a) The KiCad netlist itself.** The component record is
`(comp (ref P1) (value DB25FEMELLE) (footprint DB25FC) (libsource (lib conn) (part DB25)) (sheetpath …) (tstamp …))`
plus a package pin number per pin. Decomposed against what JLS has:

| Datum | Where it comes from | Cost |
|---|---|---|
| **Nets** | Already derived. Union-find over the ref-wire graph; measured 297 nets from 810 wire ends in the flagship `test/fixtures/riscv-sum1to10.jls`, independently reproducing keystone-c's "297 nets." The algorithm already ships twice — `Circuit.finishLoad:1345-1394` (BFS) and `HdlExporter`'s own `UnionFind` at `:1103-1177` | **free** (but pick ONE derivation — see §3) |
| **Reference designator** | Assignment policy keyed on `stableId` so `U3` does not become `U7` on the next edit. #165/#166 shipped the key | days |
| **Package pin number** | A **package table**: `74HC00` → 14 pins, section A = pins 1/2/3, VCC 14, GND 7. ~24–40 rows of **DATA**, per D7 ("circuit libraries are DATA, not plugins") | 2–3 wk incl. loader |
| **Packing** (4 NANDs → one 74HC00, leftovers tied off) | Deterministic first-fit over a fixed section count per package. **This is not the bin-packing-plus-obstacle-routing that the breadboard angle correctly refused** | days |
| **Footprint** | **One more column in the same table**, a *string* — `Package_DIP:DIP-14_W7.62mm` — that **pcbnew resolves from its own installed footprint library table**. JLS ships **zero geometry** | ~0 |
| **Value, libsource, sheetpath, tstamp** | Constants or already present | ~0 |

The footprint field being a resolvable *name* is the crux, and it is what the
prior verdict missed. KiCad's own documentation describes the field as populated
when a symbol's footprint field is set, and pcbnew's error for a component without
one is "No footprint defined for component" — i.e. the field is a string in the
grammar whose *absence* is what strands the import, not its presence
([KiCad netlist parser reference](https://docs.kicad.org/doxygen/classKICAD__NETLIST__PARSER.html);
[CvPcb documentation](https://docs.kicad.org/5.1/en/cvpcb/cvpcb.html)). **Both
pages returned HTTP 403 to automated fetch through the agent proxy**, so these are
cited from search-result summaries and must be re-verified by hand before landing
in an in-tree document.

**So: emit the name, ship none of the geometry. The curation obligation is one
column in a ~30-row table of through-hole 74-series parts, not a footprint
library.**

**(b) A bundled symbol/footprint GEOMETRY library.** This is the permanent
curation the prior verdict was actually describing, and refusing it is correct.
KiCad ships on the order of 15,000 curated footprints maintained by a team; every
one JLS shipped it would own forever at bus factor 1, and every KiCad library
revision would be a maintenance event. **Cost if built: P6-cell-layer scale
(20–32 wk) plus unbounded curation. Refuse.** Alternative: (a) above.

**(c) A PCB layout canvas, placement, routing, DRC, gerber.** Refuse — see §7.
KiCad is the view.

**The remaining objection is real and is a workflow cost, not a tractability
cost.** In KiCad 6+ the schematic and board editors exchange data internally
("Update PCB from Schematic") and a netlist file "is not necessary as part of the
normal workflow"
([KiCad forum thread](https://forum.kicad.info/t/seeking-description-of-kicad-native-netlist-format/46739));
netlist import into pcbnew survives as an interop path. A student importing a JLS
netlist therefore has a **board with no schematic**: no eeschema ERC, no BOM, no
cross-probing, no back-annotation, and two sources of truth. That is a limitation
to *state in the emitter's own generated header* — the `VhdlEmitter` header
precedent already exists — not a reason to refuse. Concretely, the contract is:
**JLS is the schematic of record; the netlist is for board layout only;
back-annotation is explicitly not supported.** For the designs a first-year team
will actually board — one or two 74-series packages, some LEDs and a header —
this works, and it is the difference between "your drawing is a real board" and
"draw it a second time in KiCad."

**Build, in order:**

| # | Item | Cost | Shared with |
|---|---|---|---|
| B1 | Package/pinout table as DATA (~24–40 74-series parts) + loader | 2–3 wk | breadboard, P6-D cell layer |
| B2 | Gate→section→package packing + refdes assignment keyed on `stableId`, delivered as a **headless batch report**: `jls -pack design.jls` → *"your design needs 3× 74HC00, 2× 74HC08; here is the gate→package→pin assignment; U1 has one unused section"* | 2–3 wk | breadboard (this IS the breadboard's payload) |
| B3 | KiCad netlist printer over B1+B2 | 1–2 wk | — |
| — | **Total** | **5–8 wk** | |

B2 has **zero GUI code and therefore zero draw on the coverage commons**, and it
delivers the strongest single pedagogical item on the whole list: *a logical gate
is not a chip; one drawn NAND is a quarter of a package that costs the same as all
four*. That is the direct ancestor of area and utilisation in the FPGA and ASIC
flows, and it is why the package table is the shared substrate for three views
rather than a PCB-only tax.

**Also correct, while in the file:** `README.md:1079-1082` and
`sweep-06:570-576` decline IPC-D-356A because *"a bare-board test netlist without
a board layout has no consumer."* IPC-D-356 is 80-column fixed-field ASCII whose
per-feature records (`317` through-hole, `327` SMD) carry XY location, drill/pad
geometry and layer in **mandatory** columns
([pcb-rnd reference](http://repo.hu/projects/pcb-rnd-aux/pool/ipc-d-356/Body.html)).
It is not a printer over JLS's netlist; it is a printer over a layout JLS will
never have. **Right verdict, wrong reason — and the weak reason invites a re-open
the day a consumer appears.** Change it to "cannot be emitted."

**Displaces:** 5–8 weeks — roughly one P5 slice, or half of P13.

### 1.5 Lithography / masks — the view is free; the silicon path is the work

**The format route settles this outright, and the settling fact was not in the
roadmap corpus.** The 2026 student path to real silicon **never asks the student
for layout**. Tiny Tapeout's current template (`TinyTapeout/ttsky-verilog-template`
— note `sweep-06:678-683` cites the obsolete `tt08-verilog-template`) takes
Verilog in `src/`, `info.yaml`, a testbench and `docs/info.md`, and *"uses
LibreLane to automatically build ASIC files through GitHub Actions."* The
submitter never produces GDS. LibreLane 3.0 shipped 2026-03-25 under the FOSSi
Foundation; SKY130 is Apache-2.0.

**Therefore the layout "view" costs one paragraph and a link:** the student opens
the CI-produced GDS in KLayout. **Zero maintainer-weeks.** That is essentially all
of the teaching payload, and it is available today for any design that can export.

**What is actually on the path, and it is not one polygon:**

| Item | Cost |
|---|---|
| Export-policy fix (`RegisterFile`, `FieldExtend`) — T0a again | 0.5–1 wk |
| P3 hierarchy + `Memory` export | 5–8 wk (funded) |
| P2 reset model. Verified: `Register` has no reset attribute and no reset input; `test/resources/hdl/counter.v:20-21` still emits `reg [3:0] count = 4'h0;`, which FPGA synthesis honours and **ASIC synthesis discards**, while `tt_um_*` makes `rst_n` mandatory | 2–3 wk (funded) |
| Inout layer 1: `HdlModel.Direction` is `{INPUT, OUTPUT}` (verified, `HdlModel.java:28-33`) and there is no `InOutPin` in the registry, so `uio_oe` has nothing to bind to | ~2 wk |
| The `tinytapeout` board target (roadmap change F): a `tt_um_*` wrapper, `info.yaml` (yaml_version 6, tiles 1×1…8×2), a LibreLane config — structurally the same problem `PinBindings` already solves for PCF | 2–4 wk |
| **Total** | **11.5–18 wk, and it ends with a chip** |

**The GDSII reader is refused with arithmetic, and the arithmetic corrects the
roadmap.** `README.md`'s load-bearing premise for change E — *"Reading and
displaying it is a 2D geometry viewer, which JLS already is"* — is **false at
HEAD**. JLS is a *schematic* geometry engine and shares nothing with layout
geometry: `Bounds` is an axis-aligned **integer** rectangle;
`Geometry.CIRCUITSIZE=1000`, `SPACING=12` (verified, `src/jls/core/Geometry.java:17-19`);
`Orientation` is a four-value enum; `SpatialIndex` is a uniform grid with a
compile-time cell of `4 × SPACING`; `Viewport` clamps zoom to [0.25, 4.0]; and
`grep -rn "Polygon|Path2D|Area(" src/` returns 4 matches, two of them `JTextArea`
false positives and two decorative `java.awt.Polygon`s in renderers — **there is
no polygon type in the model at all.** GDSII needs arbitrary polygons, PATH with
widths and end types, SREF/AREF with reflection and arbitrary angle and
magnification, a COLROW array lattice, a layer+datatype keyspace, 8-byte
**excess-64** floats (not IEEE 754), and signed-32-bit database units over a 10^6
extent. Re-derived with the hierarchical shape database included: **12–19
maintainer-weeks**, not the roadmap's 8–12. Against KLayout, which is free,
GPL-2-or-later, and better.

**OASIS should be struck from the roadmap's increment list, not costed at +3–4
weeks.** SEMI P39 is paywalled at $286 member / $380 non-member
(store-us.semi.org), and the alternative is porting KLayout's `dbOASISReader.cc` +
header = **3,945 lines** of modal-state C++ (measured by `curl` + `wc -l` against
`raw.githubusercontent.com/KLayout/klayout/master`, all HTTP 200), 1.84× the GDS2
reader proper. Legal under D8 — GPL-2-or-later absorbs into GPL-3-or-later — but
not an increment.

**And the differentiator the roadmap sells does not survive the flow.**
`README.md:666-669`: *"E without D is a worse KLayout. E with D is a thing that
does not exist."* That holds only if JLS did the technology mapping and the
mapping is what got fabricated. On the Tiny Tapeout path, LibreLane runs Yosys and
OpenROAD, which constant-propagate, re-map, replicate for drive and rename. One
drawn `NandGate` becomes 0..n cells with names JLS never emitted. **The
interesting problem is name stability across a boundary JLS does not control — an
annotation-import problem (Yosys JSON + OpenROAD ODB/DEF keyed against the shipped
stable ids), not a canvas problem.** That is a different and far cheaper project,
and it is the only place in this view where #165/#166 pays off.

**If "an open space" literally meant a scratch canvas for images and annotation,**
that is an `Image` element, a sibling of `Text`: **1–2 wk**, cheap and good, but
it is not on the silicon path and must not be counted as progress toward it.

**Course-calendar caveat, stated because no in-tree document does:** TTSKY26c
opened 2026-05-26, closes 2026-09-07, estimated delivery **2027-05-12** — roughly
9–12 months from close to chips in hand. Students submitting in an autumn term
graduate before the package arrives. Design the lab around the submission artifact
and the CI-produced GDS, not the physical chip. (tinytapeout.com and
app.tinytapeout.com return **HTTP 403** to automated fetch; these dates, the
€70/tile figure and the delivery estimate come from search-result snapshots and
must be re-verified by hand before landing in-tree.)

**Displaces:** nothing — the recommendation for the *view* is zero weeks.

### 1.6 Breadboard — the one genuine canvas, and the one with a semantic problem

**No format route exists, and that is the finding.** Fritzing is the closest thing
and it stores geometry **per view per instance** (`<breadboardView><geometry x y z/></breadboardView>`,
`<schematicView>…`, `<pcbView>…`) and, decisively, **connectivity per view too**,
reconciled by ratsnest air-wires rather than derived. Emitting a `.fz` from JLS
gives you a pile of parts at the origin — technically a deliverable, substantively
thin. **So this view's payload really is authored data, and authored data really
does need a canvas.**

**Split it, and the cheap half is the valuable half.** The pedagogy is *"a logical
gate is not a chip"* — packing, package sections, refdes, unused sections. That is
**B1 + B2 from §1.4, already costed at 4–6 wk, with no canvas, no per-view
geometry, no `CircuitOp` extension, no palette change and no `jls.edit` code.**
Build it once and both the PCB and the breadboard get their payload. Fritzing's
own history supports scoping it this way: it shipped a breadboard view from 2009
with **zero** simulation, and the simulator that arrived in 1.0.0 (June 2023) is
ngspice-backed, DC-only, and restricted to a curated SIM parts bin. **Thirteen
years of adoption with no simulation establishes that the breadboard view's value
is layout and documentation.**

**The canvas half has a semantic problem that is a sequencing dependency, not a
refusal.** JLS gets the canonical first-year breadboard bug **backwards**: a
floating 74-series TTL input reads HIGH in reality (74HC/HCT float indeterminate
and must be tied), while `docs/simulation-semantics.md:60-67` says *"Nearly every
element's `react` treats a null (HiZ) input as zero before computing"* and `:47-49`
says *"There is no unknown/X state anywhere in the simulator."* So a breadboard
view shows an unwired NAND input as solid LOW and teaches the opposite of the
truth on the very first bug. The same applies to contention (`WireNet` resolves
"first active driver in net order wins" — which depends on the order the student
drew the wires), fanout (`README.md:653-655`: one NOT gate to two hundred inputs
simulates happily), decoupling and mixed voltage.

**This is P1, which is already the roadmap keystone at 28–36 weeks and is already
funded.** So the honest statement is: **the breadboard canvas should not ship
before P1**, or it must carry an on-screen honesty statement in the
`VhdlEmitter`-generated-header idiom saying it checks topology only (wrong pin,
wrong section, wrong tie-point column) and cannot model floating inputs,
contention, fanout or decoupling. That is a defensible product; it just has to say
so.

**Cost with a canvas:** first-view tax 7–11 wk (palette 1, `VIEW` sections 3–4,
plus the `#84`/`#91` precondition) + canvas 6–10 wk + schematic↔breadboard
consistency checker 2–3 wk = **15–24 wk on top of the 4–6 wk report**, and the
consistency check ("does your breadboard match your schematic?") is a
net-equivalence comparison between two views of one model, which is P5-ERC-shaped
and gradeable headless — genuinely good, and genuinely a program.

**One unresolved implementation question that could move the estimate by weeks:**
a breadboard strip is a net with **no wire in it**, and `WireNet` is
`LinkedHashSet<Wire>` + `LinkedHashSet<WireEnd>` whose insertion order is
load-bearing for deterministic multi-driver resolution (#98 S1, pinned by
`SimulationSemanticsRegressionTest.multiDriverConflictResolvesDeterministicallyAndWarnsOnce`).
A wire-less net either fabricates synthetic `Wire`s (safe, ugly) or changes a
class whose ordering is pinned.

**Also worth putting in front of the maintainer:** the tree already contains a
cheaper, more honest and more motivating answer to *"make my drawing physical"* —
`docs/standards-adoption/11-costed-rejections.md:498-511` argues for burning an
ATF22V10 GAL for ~$2 and putting it in a **real** breadboard, calling it *"the
strongest of the four by a wide margin… the only item that closes the loop from
drawing to physical hardware without an FPGA toolchain, a vendor account, or a
gigabyte download."* A simulated breadboard is a picture of the reward; the GAL
path *is* the reward. The genuine constituency for a simulated one is students
with no lab access or parts budget, who are squarely in scope under D9.

**Displaces:** the report half displaces nothing extra (shared with PCB). The
canvas half displaces a whole program — call it P5 or P13.

### 1.7 The seventh thing: SPICE / analog. See §4.

---

## 2. THE CLASSIFICATION TABLE

**The trichotomy needs five classes, not three, and the two new ones are where the
money goes.** All four surveying angles that touched it converged on this
independently — Angle 2 found batch-vs-live front ends an order of magnitude
apart; Angle 3 found AUTHORED data that the classifier calls cheap and is not;
Angle 5 derived the primary-data test; Angle 6 found derived artifacts that
annotate rather than project.

**Per D8 this is a COST classifier, not a refusal classifier.** A COMPUTE
classification is a price tag, not a veto.

| Class | Definition | Typical cost | Cheap iff |
|---|---|---|---|
| **COMPUTE** | JLS runs an optimisation or numerical engine over a model with no logical content | tool-class; 20 wk – unbounded | never |
| **DISPLAY** | Read a foreign file and draw it | 3 wk – 19 wk | **the geometry vocabulary already exists**; JLS's is schematic, not layout, so this is usually *not* cheap |
| **PRINT** (batch front end) | Emit a file; the process ends. `PcfEmitter`'s class | 0.5 – 4 wk | **every fact is already in the model or a pure function of it**; if it needs new PRIMARY data, price the data separately |
| **DRIVE** (live front end) | A stateful, time-synchronised, bidirectional conversation with a *running* engine | ~10× PRINT: 20 – 35 wk | never cheap; needs rollback or a forward-only contract |
| **AUTHOR** | The user types in data with no preimage in the model | 6 – 10 wk canvas + 7 – 11 wk first-view tax | never; this is the class the naive classifier calls cheap because "there is no engine" |

**The reusable test, in one line, and it subsumes the trichotomy:**

> A view is a **PRINT** iff every fact it needs is already in the design model or
> is a pure function of it. A view that needs new **primary** data is one of
> AUTHOR (the user supplies it), DISPLAY (a foreign file supplies it) or COMPUTE
> (an engine must derive it) — *however small its printer looks*.

### 2.1 Every capability discussed

| Capability | Class | Reason |
|---|---|---|
| Drag-and-drop gate placement | AUTHOR — **shipped** | JLS is this view |
| Per-view palettes (`PaletteEntry` view dimension) | PRINT-adjacent, ~1 wk | A green test (`PaletteContractTest:47-65`) currently forbids D9's progressive disclosure; the fix is a totality-predicate change |
| Verilog/VHDL export | PRINT — **shipped** | `HdlExporter.buildModel` → emitter; total function of the model |
| Export-policy totality over `ElementRegistry` | PRINT hygiene, 1 d | Verified hole at HEAD: 31 of 35 registry types bucketed; `RegisterFile`/`FieldExtend` unpinned |
| Read-only generated-HDL pane + cross-probe | DISPLAY, 2–3 wk | The text is derived; the pane draws what the printer already produces |
| **Editable** HDL pane mutating the schematic | AUTHOR + inverse elaboration | `put` needs a complement Verilog cannot store; zero of six surveyed tools does it |
| `.jls` text editor → `CircuitOp` diffs | AUTHOR — but the complement is **empty**, because `.jls` carries geometry + identity | The Modelica arrangement, already shipped in the format. The genuinely bidirectional text view |
| PCF / LPF / XDC constraint emit | PRINT — **PCF shipped** | `PcfEmitter.java:73` walks `model.ports()`; a second format is a printer over an existing walk |
| Arming nextpnr in CI | PRINT validation, 2–4 d | Existing `ToolLocator` + `assumeTrue` idiom; no new capability |
| FPGA place & route / bitstream packing | COMPUTE | Timing-driven analytic placement over a per-device DB; the reverse-engineered bitstream database *is* the work |
| FPGA sign-off STA | COMPUTE | One scalar `propDelay` per element; any Fmax would be fiction relative to the fabric |
| Tiny Tapeout `tt_um_*` wrapper + `info.yaml` | PRINT, 2–4 wk | Fixed top-level signature; structurally the `PinBindings` problem |
| EDIF / BLIF / hierarchical Verilog / "export your CPU" | PRINT, blocked on **one** change | `HdlModel.StatementVisitor` has **11** visit methods (verified) and none instantiates a module |
| Package/pinout table as data (~24–40 parts) | PRIMARY DATA (D7: data, not plugin), 2–3 wk | The shared substrate for breadboard + PCB + P6-D |
| Gate→package packing + refdes, as a batch report | PRINT over the table, 2–3 wk | Deterministic first-fit; not the optimiser |
| **KiCad netlist export** | **PRINT, 1–2 wk over the table** | Nets are derived; refdes is a policy over `stableId`; pin numbers and footprint *name* come from the table. **KiCad resolves footprint geometry from its own library** |
| Bundled symbol/footprint **geometry** library | PRIMARY DATA, **unbounded curation** | ~15k KiCad footprints maintained by a team vs. one maintainer owning every part shipped, forever |
| PCB placement / autorouting / DRC / gerber | COMPUTE | Geometry optimiser over a rule deck; KiCad is excellent and free |
| IPC-D-356A emit | **cannot be emitted** — printer over a layout | Records `317`/`327` carry XY, drill/pad geometry and layer in mandatory columns |
| Breadboard **packing report** | PRINT, 2–3 wk (shared) | No geometry, no canvas, no `jls.edit` code |
| Breadboard **canvas** | **AUTHOR**, 15–24 wk | Placement has no preimage in any design model; forces per-view geometry, ops, undo, merge, a file section |
| Schematic↔breadboard consistency check | PRINT/analysis, 2–3 wk | Net equivalence between two views of one model; P5-ERC shaped |
| GDSII read-and-display | DISPLAY, **12–19 wk** | The "already a 2D geometry viewer" premise is false: no polygon type in the model, integer-only schematic coordinates |
| OASIS read | DISPLAY, **strike** | $380 paywalled SEMI P39, or a measured 3,945-line C++ port |
| Hand-drawn layout | AUTHOR, unbounded | Electric VLSI already is exactly this: Java, GPL-3-or-later, single jar, 9.08 in April 2025 |
| Running a standard-cell flow locally | COMPUTE/DRIVE | LibreLane exists; Tiny Tapeout runs it in CI for free; local orchestration breaks the single offline jar |
| Geometric DRC / LVS / OPC / mask fracturing | COMPUTE | Numerical inverse imaging and rule-deck evaluation over billions of polygons |
| Cell-name annotation import (Yosys JSON / OpenROAD ODB) | PRINT-inverse (annotate), cheap-ish | The real cross-probe problem; keyed on #165/#166; not a canvas |
| ERC over the element graph (roadmap change G) | PRINT/analysis, 3–5 wk | The real content of "does JLS want DRC" |
| `Image`/annotation element ("an open space") | AUTHOR, 1–2 wk | A sibling of `Text`; cheap and good, but not silicon |
| SPICE `.subckt` structural printer | PRINT, ~1 wk, gated on P6-D | Meaningless without cells to name; a deck naming `AndGate$7` is theatre |
| Analog waveform **display** (rawfile / real-typed VCD reader) | DISPLAY, 3–5 wk | No engine, no determinism problem |
| Drawable `Dac` / `Adc` bridge elements | AUTHOR (elements), 3–4 wk, **zero format version** | Useful with no engine attached; the `Adc` with `vLow>vHigh` is a Schmitt trigger by parameter order |
| Automatic connect-module insertion (Verilog-AMS) | COMPUTE at elaboration + **K9 violation** | Inserts a module the student never drew |
| JLS↔ngspice live co-simulation | **DRIVE**, 23–34 wk marginal | Two event queues in lock-step across a process boundary; forward-only contract required |
| In-process `libngspice` | DRIVE + native binary | Non-reentrant (global state, one instance/process); 5 platform `.so`s destroy the single jar |
| A pure-Java teaching SPICE solver | COMPUTE, **30–45 wk** — see §4 | Seven subsystems, zero reuse of the shipped loop; but **bit-reproducible** (JEP 306) |
| Vendor/foundry certification (#288–#303) | **contract gate, not a capability gate** | No model change moves them one inch |

---

## 3. THE SHARED-MODEL DETERMINATION

### 3.1 Does the reframe hold? Partly — and precisely.

**It holds for four of the six views and fails for two, and the two it fails for
are the two that cost the most.** State it that way rather than as a yes or a no,
because both the yes and the no mislead.

The correct formulation replaces "projection" with three relations:

**(a) PROJECTIONS — a pure function of the model. The reframe HOLDS, in shipped
code.** HDL text, PCF/LPF/XDC, EDIF/BLIF, the SPICE `.subckt`, the `tt_um_*`
wrapper, and the **nets half of a KiCad netlist**. `PcfEmitter.java:14-22`
literally states the invariant and `:73` implements it as a walk over the same
`model.ports()` the Verilog and VHDL emitters use. **One model, three renderings,
already shipped.** And they nearly all unblock on the *same single change* — an
instance statement in `HdlModel`, whose `StatementVisitor` has 11 visit methods at
HEAD (verified, `HdlModel.java:143-199`) and not one of which instantiates a
module.

**(b) BINDINGS — a *relation* between the model and an external table.** Pin
bindings, package/footprint assignment, cell mapping, an analog deck reference.
The reframe holds **with an amendment**: a binding is not a projection and must
not live as fields on elements. The shipped design already got this right —
`PinBindings` is a separate `-pins` file precisely because it is a relation
(`PinBindings.java:26`, `JLSStart.java:393-406`) — and the generalisation is
**external, per-target binding files keyed by identity, never new element
attributes.** What bindings need from the model is a **key** and a **place to
persist**, and today they have neither: `PinBindings` binds by **port name**, so
renaming a pin silently changes what the binding means.

**(c) AUTHORED VIEWS — primary data with no preimage. The reframe FAILS.**
Breadboard placement, PCB placement, layout polygons. Fritzing is the
counterexample the maintainer named: it stores geometry *and connectivity* per
view per instance and reconciles views by **ratsnest air-wires**, not by
derivation — seventeen years of running that experiment, with a forum full of
"PCB view not congruent with breadboard view" as the result. Nothing in P3, P7 or
#165/#166 gives you a second `(x,y)`: verified at HEAD, `Element` has exactly one
pair (`src/jls/elem/Element.java:27-30`), with one `setXY`, one `move`, one
`savePosition`/`restorePosition`. **Per-view geometry is a fourth workstream,
unscoped anywhere in the 288–424-week roadmap.**

**The corollary that matters for scoping.** "Six views" is **not one program with
six outputs**. It is one program with four cheap printers (which unblock together
on one change), one free link (KLayout), and one canvas. Sizing it as a single
thing is the failure mode.

### 3.2 What the ONE artifact must carry beyond today's `Circuit`

The one artifact is **not `Circuit` and not `HdlModel`** — `HdlModel` is a
derived, *flat* IR produced by a one-way walk that does not flatten hierarchy, it
*rejects* it (`HdlExporter.java:191-197`). The one artifact is **the identity
spine plus an elaborated hierarchical netlist**. Here is exactly what it must
carry, with an owner:

| # | Requirement | State at HEAD | Owner | Cost |
|---|---|---|---|---|
| 1 | **Stable per-element identity** surviving save/load/replication | **SHIPPED** — `Element.java:24` (`stableId = ElementId.mintFresh()`, #165) + canonical save order (#166) | — | 0 |
| 2 | **Reference by `sid`, not by dense file index** | ABSENT. `Element.java:20-21`: the id is *"reassigned on every save."* The sibling study measured one inserted element rewriting 2,651 of 10,744 lines in 234 hunks | M0 / D2 | **2–3 wk**, pays for itself in diffs alone (295× fewer churned lines) |
| 3 | **Definition/instance split** — one stored definition, N instances | ABSENT and the single biggest obstacle. `SubCircuit.save:282-288` writes the whole nested circuit **inline per instance** (verified); measured sharing factor **1.00×** | **P7 / lf-01** | inside 25–36 wk |
| 4 | **Instance-path identity** `instanceSid("/"instanceSid)*"/"elementSid` | ABSENT, and **this is the only item with a deadline.** Today a flat `sid` is design-unique only because there is *no* sharing (`Element.java:298-300`: a copy mints a fresh id). The moment P7/P3 introduce sharing, it stops being unique. KiCad hit exactly this and rewrote its format around `SCH_SHEET_PATH` UUID chaining | **specify INSIDE P7/P3** | **2–3 wk now; 6–10 wk plus a format break afterwards** |
| 5 | **Parameters on the instance** | ABSENT. `SubCircuit`'s entire state is (subCircuit, name, inmap, outmap, orientation) | P7 | inside 25–36 wk |
| 6 | **An instance statement in the netlist IR** + per-definition module emission | ABSENT. 11 statement kinds, none instantiates a module (verified) | P3 | inside 26–38 wk. **Unblocks EDIF, BLIF, hierarchical Verilog and "export your CPU" simultaneously** |
| 7 | **Net identity + ONE agreed net derivation** | ABSENT, and **two shipped subsystems already disagree.** `Wire.save` is literally `// do nothing` (verified, `Wire.java:123-126`); nets are rebuilt by BFS at `Circuit.java:1345-1394`; `HdlExporter` runs its **own** `UnionFind` (`:1103-1177`) that additionally unions same-named `JumpStart`/`JumpEnd`. A jump pair is **one signal to the exporter and two nets to the simulator** | **nobody** | **4–6 wk** — and four other programs need it |
| 8 | **Typed ports incl. INOUT, plus port roles** (clock/reset/data/valid/ready) | `Direction` is `{INPUT, OUTPUT}` (verified); `HdlExporter:485-487` treats `Clock` and `InputPin` identically, so JLS cannot say a wire *is* a clock | P2 (BidirPin) + P3 | ~2 wk for inout layer 1 |
| 9 | **Per-view geometry in an independently-versioned `VIEW` section, PRESERVE-VERBATIM on re-save** | ABSENT, and the naive place to put it is a **data-loss path**: `Element.setValue` (`Element.java:344-351`) silently ignores unknown attribute names, so an older JLS opening and saving a multi-view file **destroys the layout**. Harmless for `sid` (`file-format.md:400-402` says so); destructive for authored geometry | **nobody** — delivers D3 as a side effect | **3–4 wk** |
| 10 | **Part/package binding as DATA, persisted, keyed by path** | The only shipped physical binding is `PinBindings`: a `Map<String,String>` (`:26`) parsed from the CLI, **name-keyed and not persisted in the `.jls` at all** | **nobody** (D7 says data) | **2–3 wk table + 2–3 wk binding** |
| 11 | **View-qualified ops in the `CircuitOp` grammar** | ABSENT and the collision is concrete. `CircuitOp` is sealed over **11** ops (verified) and `MoveElements(List<ElementId> ids, int dx, int dy)` has **no view qualifier**; `AddElements` carries serialized element blocks, which by design cannot carry `VIEW`-section data. `apply()` takes `java.awt.Graphics` | **D9: extend ONCE, for multi-view and collab together** | 1–2 wk of design + the grammar change |
| 12 | **A per-circuit physical timescale** | ABSENT. Time is a dimensionless `long` (`Simulator.java:36`) | P4 | 2–3 wk — needed only for the analog binding, SDF, Liberty, STA and the VCD honesty fix |

**The negative requirement, and it is a finding, not a caveat.** The one artifact
must **never** carry analog device models, PDK data, mask polygons or footprint
geometry. Representing those *is* the COMPUTE half. **The model buys the
BOUNDARY, not the view** — which correctly predicts what stays JLS's problem
(port list, identity, binding persistence, refusal rules) and what never does
(netlist internals, models, solver, PDK, polygons).

### 3.3 The scheduling decision with a deadline

**Items 2 and 4 are near-free today and expensive after P7/P3 write code.** They
cost ~4–6 weeks now, together, against ~8–13 weeks plus a format break later.
Every cross-probe, per-view geometry record, SDF `INSTANCE` key, package binding
and LibreLane cell name is keyed on them. `AMENDMENT.md:805-810` already rules
that the site/slot index — *"P9's journal site index IS P8's levelization slot
table IS P6's cross-probe map IS P4's critical-path overlay key. One table, four
payoffs. Must be designed BEFORE either P8 or P9 writes code"* — **the multi-view
key has identical shape and makes it one table, five payoffs.** That
recommendation costs a design document, not code, and it is the cheapest item in
this determination.

### 3.4 The D9 synthesis, made concrete

Multi-view and collaboration are one program because they share **one seam**: the
op vocabulary. Two editors moving the same element in two different views is a
concurrent edit to **two different fields**, and it merges cleanly **only if the
fields are separate in the op grammar**. Extend `CircuitOp` once, deliberately,
before #169/#171 — or it will be extended twice, incompatibly.

One open architectural question falls out and should be answered while the
grammar is open: `jls.collab.op` is **not** in
`HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` and is permitted to import AWT,
which it does (`CircuitOp.apply(Circuit, Graphics)`, because rotate and flip
re-derive size from font metrics). If the op layer is the seam for both multi-view
and collaboration, an AWT-coupled op layer is a latent problem independent of this
ask.

---

## 4. SPICE

### 4.1 What is genuinely subsumable — three things, all digital

**S1. The structural `.subckt` printer** over `HdlExporter.buildModel`
(`src/jls/hdl/HdlExporter.java:170`). **~1 wk, KEPT GATED on P6-D.**
`sweep-06-physical-boundary.md:82` is right that it is meaningless without cells
to name, and a deck naming `AndGate$7` would be exactly the theatre
`README.md` §6(a) warns about for OASIS.MASK. No new decision is required; this is
already correctly scoped in-tree at the right size in the right place.

**S2. The SPICE *workflow idiom*, which is the genuinely valuable subsumption and
needs no solver.** `.step` and `.alter` teach *"run one design across a parameter
space and compare."* JLS's native answer is **P7 parameterization plus P5's
multi-run batch mode** — and with P4 it produces the N-sweep critical-path curve
that is the only clean answer to "why does anyone build a carry-lookahead adder."
Today `riscv/fuzz_diff.py` pays a JVM start per program; that is the gap.

**S3. The digital shadow of the analog value model — P1, already the keystone.**
Drive strength, open-drain `(strong, highz)`, pull-up as a constant-1 output at
`(highz, pull)`, and a real net-resolution fold. `sweep-06:83` scopes this
precisely for IBIS: *"the digital shadow — drive strength, pull-up/pull-down,
open-drain… the analog half stays out."* **This delivers the open-drain I²C
wired-AND lab that `README.md:54` records as currently not merely hard but
IMPOSSIBLE, and it is what a first-year actually means when they say "analog."**
P1 is priced at 28–36 wk and is projected to make the event loop **15–25% faster**
(`README.md:788`), so this is not a consolation prize.

### 4.2 What is another tool class — priced, per D10, not refused by precedent

**A SPICE engine is seven new subsystems with zero reuse of the shipped
3.14 M events/s loop:** an MNA assembler; sparse LU with pivoting;
Newton-Raphson with damping, limiting, gmin and source stepping; adaptive
timestep under LTE control; a device-model library with analytic derivatives; an
AC/noise path; and — the one that bites — a **floating-point continuous-time value
domain**. The measurement that makes this concrete:
`grep -rn "double \|float " src/jls/elem/*.java src/jls/sim/*.java` returns exactly
**14** hits at HEAD and **every one is screen geometry** (`GateOutline` drawing
primitives, `SMUtil` label angle, `State` mouse hit-test, `TraceGeometry` label
pitch). Not one is a value, a time, a delay, a current or a voltage. Time is
`long now` on an integer `PriorityQueue`; delay is `int propDelay`; value is a
`@Nullable BitSet`. **JLS has no numerical substrate of any kind** — which is a
stronger and more decisive fact than "it is 2-state."

**Priced honestly, per D8's own verdict** (*"teaching-grade SPICE: plausibly
reimplement… scope the models, not the solver"*): a DC-operating-point plus
transient engine over linear R/L/C/V/I, a diode and MOSFET level 1, with no AC
and no noise, is **30–45 maintainer-weeks** including an analog element
vocabulary and a real-valued waveform viewer — estimated by analogy to P1 (28–36)
and P8 (24–35), the two shipped-comparable programs of similar shape, and stated
as an analogy, not a measurement.

**And one important correction that cuts FOR reimplementation.** Every prior
document in this study, including this one's predecessor, refused a JLS solver on
determinism grounds — "external float solvers are not reproducible across
platforms." That is true of *orchestration* and **false of a pure-Java
reimplementation**: JLS targets `maven.compiler.release 25` (verified,
`pom.xml:43`), and since JEP 306 in Java 17 **all** floating-point arithmetic is
always-strict IEEE 754. A pure-Java MNA + Newton-Raphson + trapezoidal transient
solver is **bit-reproducible across every platform JLS runs on**, which
orchestrating ngspice is not. **This is the single strongest piece of evidence for
D8's "reimplement" axis found in the whole study**, and the determinism objection
against a JLS solver should be struck.

**So the refusal of a SPICE engine is not on determinism and not on principle. It
is on arithmetic and displacement:** 30–45 weeks is the fourteenth program against
a committed 288–424, it displaces roughly all of P1 or all of P7, and P1's digital
shadow (S3) delivers more of the pedagogy the ask is reaching for, per week, than
the solver does. If the maintainer wants it anyway, the number above is the price
and the sequence is: after P1, after P7, and with a scoped model set frozen in
writing before line one.

**Refuse permanently, with alternatives:** in-process/linked `libngspice` — not on
licence grounds (KiCad embeds BSD ngspice into GPL-3 fine) but because ngspice's
shared library is **non-reentrant** by its own developers' statement (global
variables, one instance per process, *"your best bet currently is to rely on
`fork()`/`exit()` and inter-process communication"*), and a native per-platform
`.so` for linux-x64/arm64, macos-x64/arm64 and win-x64 destroys the single offline
jar. A convergence abort in-process is a native crash in the same address space as
a student's unsaved circuit. **Alternative: pure Java (above), or file handoff.**
Also refuse "a GUI over ngspice/Xyce" positioning — that is **Qucs-S**, which
already exists and is explicitly *not a simulator by itself*; JLS would be
duplicating a shipped product.

**One licence correction worth recording:** "ngspice is BSD so linking is free" is
a false premise. Verified by reading `COPYING` at
`raw.githubusercontent.com/imr/ngspice/master`: the tree is **mixed** — Modified
BSD by default, but LGPLv2 (KLU, numparam, tclspice), Public Domain (XSPICE,
ndev), **GPLv2+** (the XSPICE table component), MPL-2.0 (OSDI), MIT (sparse). A
built ngspice therefore contains GPLv2+ code and the combined work is effectively
GPL. Fine for GPL-3.0-or-later JLS, but the reasoning must be corrected.

### 4.3 The mixed-signal boundary design, if it is worth doing

**The architecture is settled by elimination, not by preference.** The
industry-standard tight-lockstep contract requires **rollback**: ngspice's
`src/spicelib/analysis/dctran.c:723-733` literally runs time backwards
(`ckt->CKTtime -= ckt->CKTdelta; ckt->CKTdelta = ckt->CKTdelta/8;`) and then calls
`EVTbackup` to splice every event, node value and message posted after the
retracted time back onto free lists. JLS cannot do any of that: `Simulator.post()`
can suppress an exact duplicate but cannot **withdraw**; there is no `cancel`
anywhere in `src/jls/sim/`; element `react` mutates element-private state with no
journal; and BRIEF §7 grades *"no simulation-state serialization at all"* as fatal
— there is not even a checkpoint to roll back **to**.

**Therefore JLS must be the side that only moves forward.** JLS owns `now`,
schedules its `Dac` outputs **ahead** as a (time, value) trajectory, and accepts a
bounded lateness — the Xyce / cocotbext-ams contract, not the XSPICE one. Rollback
is eliminated by giving the analog engine enough future that it never needs to
ask. The outbound half is a mechanism JLS already ships one abstraction away:
Xyce's `updateTimeVoltagePairs` takes a **vector** of (time, voltage) pairs built
up on the digital side since the last update (`N_CIR_Xyce.h:236-245`), and JLS's
batch test-vector grammar is exactly that shape (`docs/batch-interface.md:104-112`:
*"for d v posts v at previous event time + d … until t v posts v at absolute
time t"*).

**R1, the most important refusal in this section: NO automatic connect-module
insertion.** Verilog-AMS's headline feature — disciplines typing every net, a
hierarchical discipline-resolution fixpoint, and the elaborator **inserting** an
A2D/D2A module wherever a continuous net meets a discrete one — is invisible,
elaboration-time behaviour change driven by a rule the student never sees. That is
a direct violation of *what you draw is what runs*, which K9 forbids more
absolutely than any cost argument. **Draw the `Dac` and `Adc` by hand.** This also
deletes the entire discipline-lattice infrastructure from the cost, which is most
of why the estimate is 25–37 weeks rather than ~60.

**R2, and it costs the most interesting labs:** no X, therefore no dead band. JLS
is 2-state + HiZ, pinned by
`VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`. ngspice's
`adc_bridge` returns UNKNOWN in the band; JLS's `Adc` must hold its previous value
and **fail loudly** with time/pin/voltage if a sample lands within a declared
guard band. **The scope this excludes is exactly comparator-trip-point and
metastability — the most interesting analog-boundary physics. Say so up front.**
If P1 lands, the guard band becomes a real X and those labs reopen; that is the
single largest scope expansion available anywhere in this program.

**R3–R7:** no timing-parity claim (digital observables may be up to one horizon
late); no analog content in the `.jls` — the deck is a **sidecar with a path and a
hash** in an optional versioned section (D1's split, D3's versioning); no
interactive-GUI co-simulation in a first slice; no conformance claim to
Verilog-AMS or VHDL-AMS (JLS would implement the pattern, not the standard); no
in-process engine.

**Feasibility is governed by boundary event density, not design size — which makes
the scope boundary characterizable in advance.** JLS retires an event in 318 ns
warm (keystone-c:126,136). Against an **assumed** 50 µs pipe round trip plus SPICE
re-entry (labelled: **not measured**, and measuring it is prerequisite #2):

| Digital events per synced boundary event | Slowdown |
|---:|---:|
| 10,000 | 1.02× |
| 1,000 | 1.16× |
| 100 | 2.6× |
| 10 | 16.7× |
| 1 | 158× |

A PWM-into-RC-filter or R-2R-DAC lab sits at the top and is essentially free; a
sigma-delta modulator sampling at the digital clock is at the bottom and is not
viable. Separately, the commitment horizon is **exact and free** for
register-driven `Dac`s (H = the next clock edge, which JLS knows from its `Clock`
elements) and degrades only for combinational drive.

### 4.4 The determinism problem and its containment

**The problem, stated from the algorithm and explicitly not measured.** SPICE uses
adaptive **order** and adaptive **timestep** under truncation-error control
(`dctran.c:790` `CKTtrunc`, `:816-826` order raising), so a last-ULP difference can
change whether a timestep is **accepted** — changing the timepoint *set* and hence
the row count. **The rawfile can be structurally, not merely numerically,
different across platforms.** JLS's corpus has no tolerance concept anywhere
(`VcdExportGoldenTest.clockedRegisterVcdMatchesGoldenByteForByte` compares a whole
VCD byte for byte; `file-format.md` §8 makes serialization a pure function of
content).

**The containment is the A2D itself.** A quantizer turns a non-reproducible float
stream into a reproducible bit stream, provided nothing lands near a threshold —
converting a flaky test into a deterministic **failure**. Concretely:

1. **Never golden an analog voltage.**
2. **Golden the `Adc` output bit stream byte-identically**, exactly like every
   other JLS artifact.
3. **Hard-fail the run** with time/pin/voltage if any sample lands within a
   declared guard band.
4. **Record engine + version as provenance, not as a gate.**
5. **Run the lane under `Assumptions.assumeTrue`**, non-required, exactly as
   `IverilogCompileTest.java:34` and `YosysGroundTruthTest.java:44` already do.

Byte-comparing the rawfile and numeric tolerance are both rejected: the first
assumes the timepoint set is stable (the thing in doubt), the second imports a
tolerance concept the tree has successfully avoided for its entire life.

**And the containment is unnecessary if JLS owns the solver.** Per §4.2, a
pure-Java solver under JEP 306 always-strict FP makes the timepoint set a
deterministic function of the deck. **The determinism argument is an argument
against ORCHESTRATION specifically, and an argument FOR reimplementation.** That
should be recorded, because it inverts what four prior documents assumed.

**Highest-leverage unknown, and it should be experiment #1:** is the same ngspice
version bit-reproducible across platforms on one deck? One afternoon — same
version, same deck, two platforms, diff the rawfiles. If byte-identical, the whole
determinism section gets much cheaper; if the accepted-timepoint *sets* differ,
the guard-band design is the only path.

### 4.5 Is analog/digital synchronization the SAME problem as the fidelity toggle?

**The MECHANISM is the same and should be built once. The CONTRACT inverts, and
the inversion is the entire engineering problem. One seam serves both; one
contract cannot.**

**Same.** Both are a **subcircuit-shaped boundary with port-value handoff at
declared sampling instants, selected by a per-instance attribute on `SubCircuit`
naming one member of a closed, core-internal, sealed implementation set.** That is
verbatim the fidelity toggle's design (`mech-fidelity-toggle.md` §0), and it is
where an `AnalogDeck` implementation would plug in. `SubCircuit.react` posting to
each `InputPin` is the entire inbound boundary (`SubCircuit.java:621-636`);
`SubCircuit.send` → `Output.propagate` is the entire outbound boundary (`:646-652`).
**Build the implementation-selection seam once, for the toggle, and the analog
binding is one more `ImplementationKind` plus a sidecar deck reference.** That is a
real and non-trivial saving and it is the correct answer to the maintainer's
question.

**Inverts, and this must be written down as weaker rather than smuggled in.**

| | Fidelity toggle | Mixed-signal |
|---|---|---|
| Value domain across the boundary | **shared** (2-state + HiZ both sides) | **disjoint** — zero `double`s in JLS's value domain |
| Time domain | **shared** (integer `now`) | **disjoint** — dimensionless `long` vs. double seconds; needs P4 |
| Both sides deterministic | **yes** | **no**, unless JLS owns the solver |
| Strongest available contract | **bit-identical committed state** at every sampling instant | bit-identical **only after quantization** and **only under a margin assumption** |
| Rollback | **not needed** — one event loop, one direction | **needed by the standard architecture**; eliminated only by making JLS forward-only |

BRIEF §6's *"all timing PERMITTED to differ"* extends cleanly to mixed-signal.
*"Bit-identical committed state"* does **not** — it is available only downstream of
the quantizer and only when no sample lands in the guard band. **That is a
strictly weaker contract than anything currently in the tree.**

### 4.6 The SPICE recommendation

**Recommended slice: 8–12 maintainer-weeks, and it needs no subprocess, no
protocol and no determinism containment.**

1. P4's physical-time-units slice (2–3 wk) — a hard prerequisite, since JLS time
   is a dimensionless `long` and SPICE time is double seconds. Shared with SDF,
   Liberty, STA and the VCD honesty fix.
2. Analog waveform **display**: a SPICE rawfile / real-typed-VCD reader plus a
   real-valued `Trace` row (3–5 wk). Emit real-typed VCD so GTKWave and Surfer
   keep working.
3. Two drawable bridge elements, `Dac` and `Adc` (3–4 wk, **zero format
   version**), ported from ngspice's **public-domain** `adc_bridge` and
   `dac_bridge` code models. Both are useful with no engine attached, and the
   `Adc`'s `vLow > vHigh` case turns the dead band into hysteresis — so one
   element is both a comparator and a Schmitt trigger by parameter order.

Only if a course asks: the Tier-C co-simulation protocol, **23–34 wk marginal /
25–37 wk with P4**, as Family B (forward-only, schedule-ahead, per-window horizon),
subprocess-only, orchestration living beside `scripts/icestick-handoff.sh` rather
than as JLS's first `ProcessBuilder` in `src/`.

**One decision blocks the protocol regardless of cost:** `docs/vcd-interop.md:19-24`
**rejects live co-simulation** under #63 — *"Graders must not depend on interacting
with a running simulation"* — while `docs/grand-architecture.md:432-434` lists
"subprocess co-sim" on the sanctioned side. That is an in-repo contradiction, not
a probe error, and it must be reconciled by the maintainer before the protocol is
scheduled. **If #63 stands, mixed-signal is closed by decision, not by cost — and
that is a cleaner and more honest closure than any estimate.**

---

## 5. SEQUENCING UNDER BUS FACTOR 1

The committed roadmap is **288–424 maintainer-weeks** (`AMENDMENT.md:956-980`) —
5.5 to 8 maintainer-years for one maintainer, and `README.md:1211-1218` already
says of itself: *"This is therefore not a plan to finish. It is a spine along
which to choose."* Everything below is on top of that. The ordering is by
**evidence-per-week and by deadline**, not by enthusiasm.

### Tier 0 — defects and validation. ~2 weeks total. Land under D6. Displace nothing.

| | Item | Cost |
|---|---|---|
| 0a | **Export-policy totality test over `ElementRegistry`** + a bucket decision for `RegisterFile` and `FieldExtend` | 1 d |
| 0b | Reject `-board` with a non-Verilog `-export` target (`JLSStart.java:908-916`) | 0.5 d |
| 0c | **Arm yosys + nextpnr-ice40 in CI** against the shipped `blinky_icestick.pcf` golden | 2–4 d |
| 0d | One real iCEstick flash; fill the all-`_TBD_` version record | 1 d + ~$30 |
| 0e | Stable-id net names + canonical statement order in the HDL projection; drop pixel coordinates from emitted comments (D2 applied to the projection D2 forgot) | 1–2 wk |
| 0f | Documentation corrections: `ARCHITECTURE.md:314-317` and `grand-architecture.md:432-434` from *subprocess* to **file handoff** (0 `ProcessBuilder` in `src/` at HEAD, verified); IPC-D-356A from *"no consumer"* to **"cannot be emitted"**; `README.md:33/36/52` and `sweep-06:40` stale counts (`ShiftRegister` **is** exported; 11 statement kinds, not ten; 35 element types, not 33) | 1 d |
| 0g | Add the **HDL export menu item** — the only near-free demand experiment in the study | hours |

### Tier 1 — the model items with a deadline, inside programs already funded.

| | Item | Cost | Why now |
|---|---|---|---|
| 1a | **M0 reference-by-`sid`** | 2–3 wk | Everything keyed is keyed on ids that must not renumber |
| 1b | **M2 instance-path identity specified INSIDE P7/P3** | **2–3 wk now vs. 6–10 wk + a format break later** | **The only item with a deadline** |
| 1c | The `CircuitOp` view-qualifier decision, before #169/#171 | 1–2 wk | Extend once or extend twice incompatibly |
| 1d | **The site/slot index designed as ONE table** — `AMENDMENT.md:805-810` already requires it before P8/P9 write code; multi-view adds a fifth payoff | a design document | Cheapest item here |
| 1e | Net identity + **one** agreed net derivation | 4–6 wk | Two shipped subsystems disagree today; four programs need the answer |

### Tier 2 — the first-view tax. ~7–11 wk, paid once by whichever view goes first.

| | Item | Cost |
|---|---|---|
| 2a | View-aware palette contract (unblocks D9's progressive disclosure, currently forbidden by a green test) | ~1 wk |
| 2b | D3 per-section versioning + `VIEW` sections with **preserve-verbatim** | 3–4 wk |
| 2c | **#84 `SimpleEditor` decomposition + #91 UI harness** | not costed here; **a precondition, not an adjacency** |

**2c is the hard arithmetic limit.** `SimpleEditor` is 5,852 lines at HEAD against
4,119 in its own issue title. `jls.edit` is 23,910 lines, is deliberately
unfloored, and the raise-with-your-PR ratchet forbids lowering a floor to make
room. Bundle headroom is ~2,897 uncovered LINE and ~1,475 uncovered BRANCH units
**total, for all future untested code**; a minimum canvas is ~850–1,350 executable
lines and nearly all branches. **Two, maybe three canvases fit. Six do not.**
(Provenance caveat, stated because it should be: the JaCoCo figures come from a
`target/jacoco.exec` of unverified cleanliness reading ~0.6 pt above documented
headless figures — `pom.xml:411-415` warns the agent appends across unclean runs —
so these are **upper bounds** on headroom, which is the conservative direction.
A `mvn clean verify` should pin the exact budget before anyone plans against it.)

### Tier 3 — the shared physical substrate. 5–8 wk. Serves PCB, breadboard, and P6-D.

B1 package table (2–3) + B2 packing report (2–3) + B3 KiCad netlist printer (1–2).
**Zero GUI code, zero draw on the coverage commons, and it delivers the strongest
pedagogical item on the list.**

### Tier 4 — projections that unblock together on P3's instance statement.

EDIF, BLIF, hierarchical Verilog, "export your CPU", the `tt_um_*` wrapper, and
(after P6-D) the SPICE `.subckt` — all printers over one existing walk.

### Tier 5 — canvases, and the analog protocol. Only against a named course.

Breadboard canvas 6–10 + consistency checker 2–3, **after P1** or with an on-screen
honesty statement. Tier-C analog co-simulation 23–34 wk marginal. PCB layout
canvas: never — KiCad. Layout canvas: never — KLayout.

### What does NOT fit — stated as arithmetic

1. **Six canvases.** ~2,897 LINE units of total bundle headroom against ~850–1,350
   per canvas. **2–3 maximum**, and only after #84/#91. This is the one hard
   ceiling in the study.
2. **A SPICE solver (30–45) alongside P1 (28–36), P7 (25–36) and P3 (26–38).**
   That is 109–155 weeks — 2–3 maintainer-years — for four things, and the solver
   is the one of the four that delivers the least pedagogy per week.
3. **Live analog co-simulation (23–34 wk marginal) ahead of P1 (28–36 wk).** P1
   delivers the open-drain I²C lab `README.md:54` records as impossible; the
   protocol delivers a boundary whose most interesting labs (comparator trip
   point, metastability) are exactly what R2 refuses until P1 lands anyway.
4. **The GDSII reader (12–19 wk re-derived) at all**, while KLayout is free and
   the Tiny Tapeout path (11.5–18 wk) ends with a chip instead of a picture of
   one. **OASIS: struck.**
5. **A bundled footprint/symbol geometry library.** ~15k KiCad footprints
   maintained by a team; one maintainer owns forever whatever ships.
6. **The full 288–424-week roadmap.** It was never a plan to finish, and adding
   ~50–70 weeks of view work does not change that — it changes which stopping
   points leave a shippable tree.

---

## 6. DEMAND GATES — in the #212 idiom, and correctly scoped

**D10 §3 is binding: demand gates apply to third-party asks, never to the
maintainer's roadmap.** So none of the following *permits* a view — the maintainer
has already ruled these users in scope (D9). What follows is **evidence that would
move a view to the front of the queue**, which is the legitimate residue of the
#212 idiom.

| View | What moves it to the front |
|---|---|
| **Breadboard canvas** | A named course whose students have **no lab access or no parts budget** — D9's constituency, and the one the GAL path (`11-costed-rejections.md:498-511`, ~$2 ATF22V10, <$100 of class hardware) does **not** serve. (arXiv 2206.07146, *"Learning Hands-On Electronics from Home: A Simulator for Fritzing"*, is the best available evidence on that constituency; it returned **HTTP 403** through the agent proxy and should be fetched by other means.) |
| **Second FPGA board** | Two independent triggers: `docs/standards-adoption/06-fpga-constraint-formats.md`'s own recorded gate (*"a course or a user naming a board they own"*), **and** capacity — a ~580-element machine will not fit an HX1K's 1,280 cells even after P3, so an HX8K breakout is the capacity answer at zero emitter cost |
| **KiCad netlist** | A student team that has drawn a design and asks *"how do I make this real"* — and note the honest triage: for one or two packages the breadboard/GAL answer is cheaper and closes the loop faster |
| **Live mixed-signal** | A named lab needing an ADC/DAC/RC boundary that **cannot** use Falstad or Tinkercad — both free, in-browser, no install, and both already do analog-alongside-digital today |
| **GDSII viewer** | **Three conditions together**: a named instructor and course; P6-D landed so JLS does its own technology mapping; and a **CI-proven name-stable path** from a drawn element to a polygon in a named flow version. Any one alone is not enough — that is what makes the roadmap's cross-probe differentiator real rather than asserted |
| **Editable HDL pane** | Does not open on demand. It opens only if a target language carries diagram geometry (the Modelica arrangement). Verilog will not |

**The one demand experiment worth running, and it costs hours:** **there is no HDL
export menu item in the GUI** (verified: zero references to `HdlExporter`,
`VerilogEmitter` or `PcfEmitter` anywhere in `src/jls/edit/` or `src/jls/ui/`; the
only call site is `JLSStart.java:418-425`). JLS ships a capability nobody can
reach from the editor, so no usage evidence for it can exist. Add the menu item
and watch. This is the correct way to get demand data — build the cheap surface,
not gate on a signal the tool makes impossible to produce, which is precisely the
antipattern D10 names.

---

## 7. PERMANENT REFUSALS — grand-architecture §9 idiom

Each carries an arithmetic or a stated-goal conflict, an alternative, and a
revisit trigger. Per D8, none of these is refused on policy.

**R-1. Geometric DRC, LVS, OPC, RET, ILT, mask data prep and fracturing.**
*Rationale:* numerical inverse-imaging and rule-deck evaluation over billions of
polygons; no logical content; the geometry vocabulary does not exist in the model
(zero polygon types at HEAD). *Alternative:* **ERC over the element graph**
(roadmap change G, 3–5 wk) is the real content of "does JLS want DRC," and the
schematic↔breadboard consistency check is the real content of "does JLS want LVS."
*Revisit:* never for OPC/RET/ILT/fracturing. DRC only under a PDK relationship,
which is a contract gate, not a capability gate.

**R-2. PCB placement, autorouting, DRC and gerber generation.** *Rationale:* a
geometry optimiser over a design-rule deck — no shared data model, no shared UI,
no shared expertise. *Alternative:* **emit the netlist; KiCad is the view**
(§1.4). *Revisit:* never.

**R-3. A bundled symbol/footprint GEOMETRY library.** *Arithmetic:* KiCad
maintains on the order of 15,000 footprints with a team; JLS would own every one
it shipped forever at bus factor 1, and each KiCad library revision is a
maintenance event. *Alternative:* emit the footprint **name string**
(`Package_DIP:DIP-14_W7.62mm`) and let pcbnew resolve it from its own installed
library — **zero geometry shipped**, one column in a ~30-row table. *Revisit:* if
a course standardises on ≤20 parts **and** accepts JLS as the maintainer of that
list.

**R-4. Automatic connect-module insertion (Verilog-AMS discipline resolution).**
*Rationale:* it changes the elaborated circuit by inserting a module the student
never drew — a direct conflict with a goal the maintainer stated, and K9 forbids
it more absolutely than any cost argument. *Alternative:* hand-drawn `Dac`/`Adc`
elements, which are independently the most teachable objects in the whole
proposal. *Revisit:* **never.** This one is on principle, and the principle is the
maintainer's.

**R-5. In-process / linked `libngspice`, or any native per-platform binary in the
jar.** *Arithmetic:* ngspice's shared library is non-reentrant (global state, one
instance per process, per its own developers); a native `.so` means five platform
binaries and destroys the single self-contained offline jar (D4, load-bearing); a
convergence abort is a native crash in the same address space as a student's
unsaved circuit. *Alternative:* pure-Java reimplementation (deterministic under
JEP 306, §4.2) or file handoff. *Revisit:* never for in-process native.

**R-6. Vendor and foundry certification (#288–#303), and any conformance claim to
Verilog-AMS, VHDL-AMS, USB, Ethernet or PCIe.** *Rationale:* these bind a named
tool version to a named PDK version under NDA, renewably — **there is no change to
JLS's value domain, element model, timing model, kernel, file format or UI that
moves any of them one inch.** *Alternative:* ship the sub-blocks (8b/10b, NRZI +
bit stuffing, CRC-32) as example circuits with no conformance claim. *Revisit:*
only a commercial relationship.

**R-7. An editable HDL pane whose edits mutate the schematic.** *Arithmetic:*
`put:(View,Model)→Model` needs a complement — layout, identity, orientation,
probes, `Memory` contents, hierarchy — that Verilog has nowhere to store; and zero
of six surveyed tools, including Vivado and Simulink, does it. *Alternative:* the
`.jls` text editor under D1/D2/D3, diffing into the 11 shipped stable-id-addressed
`CircuitOp` kinds. *Revisit:* a target language that carries diagram geometry
inside the source, as Modelica does.

**R-8. Live co-simulation with any external running engine, while #63 stands.**
*Rationale:* this is a **decision** conflict, not a cost one —
`docs/vcd-interop.md:19-24` rejects it, `grand-architecture.md:432-434` sanctions
it, and the contradiction is in-repo and must be resolved by the maintainer, not
by a cost estimate. *Alternative:* file handoff (Tier D display + `Dac`/`Adc`
elements), which needs no protocol at all. *Revisit:* the maintainer reopening
#63, **and** an event-injection path existing (BRIEF §7 grades its absence fatal:
every `post()` site is in `jls.elem`, and `post()` is unsynchronized over a
single-thread `PriorityQueue`).

---

## 8. THE ONE THING TO DO FIRST

**Add a policy-totality test over `ElementRegistry` to `HdlExporter`, and decide
the bucket for `RegisterFile` and `FieldExtend`. One day.**

Six reasons, in order:

1. **It is a live regression at HEAD, verified this session.**
   `ElementRegistry.ALL` is 35 types; `EXPORTED` (22) + `SKIPPED` (6) +
   `TOPOLOGY` (4, of which `Wire` is not a registry type) covers 31.
   `FieldExtend`, `Memory`, `RegisterFile` and `SubCircuit` fall through to the
   offender throw at `HdlExporter.java:191-197`, which calls them
   **unrecognized**. `Memory` and `SubCircuit` are deliberate and pinned
   (`HdlPolicyTest.java:63,77`). **`RegisterFile` and `FieldExtend` are neither
   deliberate nor pinned** — `grep -rn 'RegisterFile\|FieldExtend' src/jls/hdl/ test/jls/hdl/`
   returns nothing.
2. **It broke the flagship.** `RegisterFile` is the element BRIEF §13 records as
   *deleting* the parity machine's register-file design choice — 98 elements down
   to 1, 18.00 events/cycle down to 6.94. Adding it made the CPU cheaper to
   simulate and simultaneously made every circuit using it silently
   un-exportable. **The natural way to draw a CPU is now the way that cannot
   leave the tool.**
3. **Every projection view in the ask rides this one table.** HDL text, FPGA
   constraints, EDIF/BLIF, the SPICE `.subckt`, the KiCad netlist, the `tt_um_*`
   wrapper — all of them project through `HdlExporter`'s policy buckets. A hole
   here is a hole in six views at once. **It is the multi-view program's smallest
   possible instance of its own thesis, and fixing it is the cheapest available
   test of the reframe: if one artifact really is what all views project from,
   then one day's work should fix six views. It does.**
4. **The idiom is one commit away.** `970db41` added exactly this totality test
   for the registry→`SaveTags` frozen tag table. The pattern, the failure-message
   style and the reviewer's expectation all exist.
5. **It costs nothing anyone else needs.** Zero format version (BRIEF §13: a new
   element type costs none), zero draw on the coverage commons, zero displacement,
   and it lands immediately under D6 without waiting on #77.
6. **It is the correct kind of first move for a bus-factor-1 project holding a
   288–424-week roadmap:** it converts an unnoticed regression into a permanent
   invariant, so the next in-tree element — and BRIEF §13 measures that tax at
   ~65 lines across 12 files — cannot silently narrow the export surface again.
   The 13th file is a test.

**And in the same week, because they are days and they displace nothing:** the
`-board` language guard (0.5 d), arming nextpnr in CI against the shipped golden
(2–4 d, the highest evidence-per-hour item in the study), one real iCEstick flash
(1 d + ~$30), and the documentation corrections in Tier 0f. That is one week
that turns a shipped-but-unverified FPGA claim into a verified one and closes a
hole under six views — before a single new canvas exists.

---

## 9. OPEN QUESTIONS THE MAINTAINER MUST ANSWER

1. **Is #63 reopened?** `docs/vcd-interop.md:19-24` rejects live co-simulation;
   `grand-architecture.md:432-434` sanctions subprocess co-sim. This contradiction
   is in-repo and blocks the analog protocol regardless of its cost.
2. **Does the maintainer have or can they buy an iCEstick (~$30)?** The single
   highest-credibility item in this determination is blocked on hardware, not on
   engineering.
3. **"Text programming" — an HDL pane, or an editor over the `.jls` text?** The
   whole §1.2 recommendation forks on this and the ask does not disambiguate.
4. **Is M2 (instance paths) amended into P7/P3's design in place, or does it get
   its own issue?** The 2–3 wk vs. 6–10 wk asymmetry makes this the
   highest-leverage scheduling decision available today.
5. **Is a `JumpStart`/`JumpEnd` pair one net or two?** Undecided at HEAD;
   `Circuit.finishLoad` and `HdlExporter`'s `UnionFind` already disagree. Any
   physical view forces the answer and the answer is user-visible.
6. **Legacy fixtures carry zero `sid`s.** Do they get re-saved (changing every
   golden's bytes), or are view bindings simply refused on legacy-id circuits?
7. **What is the true headless bundle coverage?** All the canvas-count arithmetic
   is stated as an upper bound on headroom because the `jacoco.exec` provenance is
   unverified. A `mvn clean verify` pins it.
8. **Should `jls.collab.op` join `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES`?**
   It imports AWT today. If the op layer is the seam for both multi-view and
   collaboration, that is a latent problem independent of this ask.
