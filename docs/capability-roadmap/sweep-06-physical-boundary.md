## The physical-implementation boundary: where does it actually lie?

*Sweep 06. Survey entries #100–#153 (physical implementation, mask/lithography,
test/package/board) and #288–#304 (foundry certification). The survey marked
every one of these OTHER and declared the judgment "correct and permanent"
(`docs/standards-landscape.md:330-337`). Re-derived here under the capability
frame.*

---

### The finding, before the tables

The survey's Tier 7 preamble says the boundary lies at physical implementation:
"the nearest JLS ever comes to this tier is by handing a netlist to Yosys"
(`docs/standards-landscape.md:330-337`), and Tier 8 adds that "lithography mask
design is not reachable by an incremental extension of a schematic editor"
(`:366-373`).

Both statements are true. Neither is the boundary, because **JLS cannot hand a
netlist to Yosys for any design that matters.** Measured, not assumed:

```
$ java -jar target/jls-5.0.5-SNAPSHOT.jar -export cpu.v riscv/build/addi.jls
jls: error: circuit "addi" contains elements HDL export does not support yet:
  Memory "imem" at (300,60); Memory "ctrl" at (1620,60);
  ShiftRegister at (2460,780); ShiftRegister at (2580,780);
  ShiftRegister at (2700,780); Memory "dmem" at (1740,900)
```

That is the repository's own flagship design — the RV32I CPU that
`docs/grand-architecture.md:52-55` names as trajectory #1, that
`test/jls/RiscvCpuGoldenTest.java` pins, that `riscv/fuzz_diff.py`
differentially fuzzes against a reference emulator. 1038 elements, one flat
circuit. It cannot reach step one of the open flow.

So the honest map is:

| Tier | Survey's claim | What the tree says |
|---|---|---|
| 5 (netlist out, #74–#86) | HAVE / ADJACENT | **Partial.** `HdlExporter` covers 21 of 33 element types (`src/jls/hdl/HdlExporter.java:418-424`); Memory, SubCircuit and ShiftRegister are hard rejects (`:83-86`, `:190`). No hierarchy. |
| 6 (timing/libraries, #87–#99) | "Nothing in this tier is reachable" | Correct *today*, and the reason is a missing cell abstraction, not a missing parser. |
| 7 (physical, #100–#113) | "Every row is OTHER, and that is the correct and permanent answer" | **Wrong for three rows and half-wrong for four more.** Producing physical data is a different tool class; *reading and displaying* it is not; *targeting* it is not. |
| 8 (mask/fab, #114–#128) | OTHER | **Correct, on ground (a) and (b) of the frame.** Restated with a better reason below. |
| 9 (test/board, #129–#153) | OTHER except #129 | **Wrong for #137/#138** (STIL/WGL are vector-interchange printers of the same class as the VCD writer JLS already ships) and **understated for #129/#134/#135**. |
| §12.i (foundry, #288–#304) | OTHER; #304 ADJACENT | **Correct for #288–#303**, on a ground the frame does not list and should: these certify a *commercial relationship*, not a capability, so no model change unlocks them. **#304 is factually stale** — Efabless shut down in March 2025. |

The real boundary is a line drawn *through* Tier 7, not before it, and it
separates three things the survey's tier scheme fuses:

1. **Computing physical data** — placement, routing, extraction, DRC/LVS deck
   execution, OPC, mask fracturing. Genuinely another tool class, because each
   is an optimisation or numerical-imaging engine over a geometric-and-process
   model that has no logical content. Declining is correct.
2. **Reading and displaying physical data** — GDSII, OASIS, DEF, CIF, LEF, and
   layer-property files. Not another tool class at all: a 2D geometry viewer.
   JLS already *is* a 2D geometry viewer with a viewport, a spatial index
   proven correct in Agda (`proofs/SpatialIndexCorrectness.agda`), a per-element
   renderer seam (`src/jls/edit/ElementRenderer.java`), and vector export
   (`src/jls/edit/CircuitRenderer.java:314-323`).
3. **Being a legitimate front end to somebody else's physical flow** — emitting
   what a shuttle or an RTL-to-GDSII flow requires. Not another tool class
   either, and provably so: JLS *already does the FPGA analogue* of exactly this
   at `src/jls/hdl/board/PcfEmitter.java`, one tier down, shipped in #213.

---

### The blocked standards

| # | Standard | What blocks JLS today (code) | Change that unblocks |
|---|---|---|---|
| — | *(prerequisite)* the whole descent | `HdlExporter.java:83-86` rejects `SubCircuit`, `Memory`, and anything not in `EXPORTED` (`:418-424`, which omits `ShiftRegister`); the throw at `:190` names every offender. `HdlPolicyTest.memoryIsRejectedByName()` and `.subCircuitIsRejectedCleanly()` *pin the limitation as intended behaviour*. | **A. Total export coverage + hierarchy** |
| 110 | OpenROAD / OpenLane / **LibreLane** flow conventions | Nothing to feed it. Beyond A: `VerilogEmitter` renders register init as a declaration initializer — `reg [3:0] count = 4'h0;` (`test/resources/hdl/counter.v:19`) — which FPGA synthesis honours and **ASIC synthesis silently discards**. `Register` has no reset attribute at all (`src/jls/elem/Register.java:274-386`: name, bits, init, orient, delay, type, watch). | **A + B (reset / power-on-state model)** |
| 109 | SKY130 / GF180MCU / IHP SG13G2 open PDKs | JLS has no concept of a technology cell. `ElementRegistry.ALL` is 33 Java classes (`src/jls/elem/ElementRegistry.java:37-73`); a cell cannot be data. | **D (technology-cell layer)** |
| 100 | **LEF** — abstract cell views | Same. Also `HdlModel.Direction` has only `INPUT`/`OUTPUT` (`src/jls/hdl/HdlModel.java:28-33`) so a cell with an inout pin is inexpressible. | **D + C (inout / strength)** |
| 101 | **DEF** — placement & floorplan | No coordinate space for it: `Geometry.CIRCUITSIZE = 1000` px, `Geometry.SPACING = 12` px, integer only (`src/jls/core/Geometry.java:17-20`); `Viewport` clamps zoom to [0.25, 4.0] (`src/jls/edit/Viewport.java:58,64`). A 1 mm² die at 5 nm resolution is 200000 units across. | **E (layout view)** |
| 102 | **GDSII Stream** | No binary-geometry reader, no layer model, no non-schematic canvas mode. | **E** |
| 103 | SEMI P39 **OASIS** | Same, plus modal-state/CBLOCK decoding. | **E** (+3–4 wk over GDSII) |
| 104 | **CIF** | Same; trivially cheap once E exists (ASCII, ~8 record kinds). | **E** |
| 108 | KLayout `.lyp` layer properties / Magic `.tech` | No layer/style model. *Executing* a DRC deck stays out. | **E** (read-only) |
| 87 | **Liberty** `.lib` | Delay is one scalar integer per element with fixed per-class defaults (`docs/simulation-semantics.md:283-300`: AND 10, NAND 5, Mux 25, Register 50, Memory 100, Adder 30×bits). No pin capacitance, no load, no fanout, no area, no arcs. | **D** (subset read: area, pin caps, cell function, one delay figure) |
| 89 | IEEE 1497 **SDF** | `docs/standards-adoption/11-costed-rejections.md:110-124` names the real blocker and it is not the parser: SDF keys on `CELLTYPE`+`INSTANCE`, and technology mapping destroys the 1:1 drawing↔instance correspondence. | **D** — a drawn cell *is* the instance, so the correspondence survives by construction. This is the change 11-costed-rejections named as absent (`:810`). |
| 111 | SPICE / LVS netlist conventions | A structural `.subckt` printer over `HdlExporter.buildModel` is the same class of work as `VerilogEmitter`; it is meaningless without cells to name. | **D** (then a ~1 wk printer) |
| 112 | **IBIS** / 113 Touchstone | No continuous-time solver, and none should be added. But IBIS's *digital shadow* — drive strength, pull-up/pull-down, open-drain — is blocked by the resolution rule: "the first active driver in net order wins" (`docs/simulation-semantics.md:422-443`, `WireNet.propagate`). | **C** unlocks the digital half; the analog half stays out. |
| 105 | Si2 OpenAccess | Membership-licensed C++ database API, not a format. | none — stays out |
| 106 | iPDK / OPDK | TCL/PyCell *parameterized layout generators*; requires a layout engine. | none — stays out |
| 107 | Calibre SVRF / Pegasus / ICV | Proprietary rule languages executed by a geometry engine. | none — stays out |
| 129 | IEEE **1149.1** JTAG + BSDL | The TAP is a 16-state FSM and JLS has `StateMachine`; the blocker is the *boundary cell*, which is a bidirectional pin with an enable — inexpressible (`HdlModel.Direction`, no `InOutPin` class in `ElementRegistry`). BSDL itself is a small printer. | **C**, then a ~2 wk BSDL emitter |
| 134 | IEEE **1500** core-test wrapper | Same shape as #129, one level in. | **C + A** (hierarchy) |
| 135 | IEEE **1687** IJTAG | A network of #129s; needs hierarchy. | **C + A** |
| 137 | IEEE **1450 STIL** | *Nothing structural blocks this.* JLS has a test-vector engine (`docs/batch-interface.md` §2, `src/jls/elem/SigSim.java`, `TestGen`), accumulated samples (`BatchSimulator.getTraceSamples`), and a byte-deterministic waveform writer already conformed to IEEE 1364 §18 (#66, HAVE). STIL is the same class of artifact. The survey shelved it next to ATE *hardware* and inherited that row's OTHER. | **nothing** — a printer, 2–3 wk. Mis-classified, not blocked. |
| 138 | **WGL** | Same as #137. | **nothing** — 1–2 wk on the #137 walk |
| 304 | Efabless / ChipIgnite shuttle acceptance | **Row is stale.** Efabless shut down operations in March 2025, taking chipIgnite with it; the surviving open-shuttle path is Tiny Tapeout on SKY130 (ChipFoundry / Swiss Chips), and OpenLane 2 was renamed **LibreLane** in early 2026 (`librelane/librelane`). The live target has a *fixed* top-level signature — `tt_um_*(ui_in[7:0], uo_out[7:0], uio_in[7:0], uio_out[7:0], uio_oe[7:0], ena, clk, rst_n)` — which is structurally the same problem `PinBindings` already solves for PCF. | **F (shuttle target)**, on top of A+B+C |
| 288–303 | TSMC OIP / Samsung SAFE / Intel Foundry / PDK certification | Not a capability question. These bind a named tool version to a named PDK version under NDA, renewably. There is no model change that unlocks them. | none — stays out, on a *contractual* ground |
| 114–128 | mask data prep, lithography, fab automation | See "What genuinely stays out". | none — stays out |
| 130–133, 136, 139–153 | mixed-signal/AC scan, ATE programs, PCB, packaging, qualification | See "What genuinely stays out". | none — stays out |

---

### The changes, and what each unlocks

#### A. Total export coverage: hierarchy, memory, and the missing elements

**Technically.** Three separable pieces:

- *Hierarchy.* `HdlExporter.buildModel` walks one `Circuit` into one flat
  `HdlModel`. It becomes a walk producing one module per distinct subcircuit
  plus instantiation statements, with a name-collision policy and a
  depth/recursion guard. `HdlModel` gains an `Instance` statement kind next to
  the existing `Port`/`Net`/`Operand` records. `SubCircuit` moves from the
  reject set to `EXPORTED`.
- *Memory.* `Memory` becomes an inferrable RAM/ROM: a `reg [w-1:0] m [0:d-1]`
  array plus the read/write templates matching its two modes (level-sensitive
  default and the #199 synchronous-write mode, `docs/simulation-semantics.md`
  §8.4), plus `$readmemh`/VHDL initializer emission for pre-loaded contents.
  The tri-state output (`§9`) needs C to be expressed honestly; without C it
  emits `en ? m[a] : {w{1'bz}}` as `TriState` already does.
- *ShiftRegister.* A one-line barrel-shift expression per mode; it is the
  cheapest of the three and is currently rejected only because nobody added it
  to the set after #122 shipped.

**Unlocks (standards).** Nothing *directly* — and that is the point. It unlocks
#110, #109, #86, #82, #89, #100, #101, #111, #304 and the whole of §12.i's
reachable edge **transitively**, because every one of them consumes a netlist
JLS cannot currently produce. It also unblocks the already-roadmapped #82
(XDC/QSF) and #215 (bitstream handoff), whose value is presently capped at
whatever a student can draw without a subcircuit or a memory.

**Unlocks (pedagogy).** "Export your CPU" — currently impossible. More sharply:
JLS teaches hierarchical design (`SubCircuit` is a first-class element with its
own dialog, renderer and disabled-banner UI test) and then silently refuses to
carry that hierarchy across the tool's own most important boundary. A student
who structures their design *well* is punished by the exporter; one who draws a
1000-element flat mess is rewarded. That is a teaching inversion, and it is in
the tree right now.

**What is being papered over.** Two artifacts:
`test/jls/hdl/HdlPolicyTest.java:63-74` (`memoryIsRejectedByName`) and `:77-…`
(`subCircuitIsRejectedCleanly`) are *tests that assert the limitation is
deliberate*. `docs/hdl-support-research.md:452-460` describes the same gaps from
the *import* side and calls them "genuine, loudly-rejectable gaps". The
loudness is real; the rejection has now outlived its rationale.

**Size:** 5–8 maintainer-weeks. Hierarchy 3–4 (name policy, recursion guard,
per-module net naming, 29 Verilog + 29 VHDL goldens re-baselined plus new ones);
Memory 2–3 (two modes, two languages, initialization); ShiftRegister 0.5.

---

#### B. A reset model, and the end of the power-on-value fiction

**Technically.** `Register` gains reset attributes — presence, polarity,
synchronous/asynchronous, reset value — and a reset input `Put`. `Register.react`
gains a reset branch ahead of the edge branch. `initSim` keeps driving `init` at
time 0 for backward compatibility, but the *emitters* stop rendering `init` as a
declaration initializer when a reset exists and render a reset branch instead.
`StateMachine` gets the same (it already has clock-edge machinery,
`docs/simulation-semantics.md` §8.2), and `Memory`'s sync mode gets a reset for
its control state. On the import side, `$adff`/`$sdff`/`$adffe` stop being
rejected by `CellValidator` (`src/jls/hdl/yosys/CellValidator.java:54-64` lists
only `$dff`/`$dlatch`).

**Unlocks (standards).** #110 and #109 *properly* — this is the difference
between Verilog that synthesizes and Verilog that fabricates. #304 (the shuttle
wrapper's `rst_n` is mandatory and the design must come up in a defined state).
#129/#134/#135 (a TAP controller is defined by its reset behaviour). And it
closes the largest single named gap in `docs/hdl-support-research.md:452-455`
("async-reset FFs (`$adff` family — JLS Register has no reset pin").

**Unlocks (pedagogy).** Reset discipline is a first-year topic that JLS
currently cannot teach, and worse, *actively mis-teaches*: a JLS register comes
up holding its `init` value with no reset in sight, forever if no clock ever
ticks (`SequentialGoldenTest.registerInitialValueAppearsBeforeAnyClockEdge`
pins exactly this). Students learn that flip-flops have factory-set contents.
On an FPGA that happens to be true (the bitstream sets them). On silicon it is
false, and a design that relies on it boots into garbage. New labs: reset
sequencing, asynchronous-assert/synchronous-de-assert, reset-domain reasoning,
"why does this counter work on the iCEstick and not in the ASIC flow".

**What is being papered over.** `test/resources/hdl/counter.v:19` — `reg [3:0]
count = 4'h0;` — is a construct whose meaning *depends on the target*, emitted
unconditionally, with no warning anywhere. The generated-file header says "JLS
simulates two states plus HiZ: this module drives 0/1/z, never x" and says
nothing about the initializer. This is the single most dangerous paper-over in
the tree with respect to silicon: the exported design simulates one way and
fabricates another, and JLS's own goldens bless it.

**Size:** 2–3 maintainer-weeks. The element work is small; the cost is the
16-step element-authoring ritual (`ARCHITECTURE.md:120-137`) applied to a
changed element, a `FORMAT_VERSION` bump from 2 (`src/jls/Circuit.java:102`),
`AllElementsRoundTripTest`/`AttributePersistenceTest`/`RegisterModelTest`
updates, and a `docs/simulation-semantics.md` §8.1 rewrite.

---

#### C. Bidirectional pins and a driver-strength resolution model

**Technically.** Two layers, and the first is cheap while the second is the
value:

- *Layer 1 (structural).* `HdlModel.Direction` gains `INOUT`
  (`src/jls/hdl/HdlModel.java:28-33`); an `InOutPin` element joins
  `ElementRegistry`; the exporter emits `inout` ports; the scanners' existing
  `ScannedPort.Direction.INOUT` (`src/jls/hdl/scan/ScannedPort.java:18`) finally
  has a counterpart to map onto.
- *Layer 2 (semantic).* `WireNet.propagate` stops resolving by net order and
  starts resolving by a strength lattice — at minimum
  {HiZ, weak-0/weak-1 (pull), strong-0/strong-1 (drive), conflict}. The
  `Output` gains a strength; a `PullUp`/`PullDown` element becomes drawable; a
  genuine conflict resolves to a distinct value rather than to a winner.

**Unlocks (standards).** #129/#134/#135 (a boundary-scan cell *is* a
bidirectional pin with an enable — this is the actual blocker behind the
survey's own "COULD (teaching value)" mark on #129). #100/#101 (pad-ring and
LEF pin models are directional; an inout pin has no LEF `DIRECTION` today).
#304 (the shuttle wrapper's `uio_in`/`uio_out`/`uio_oe` triple). #112's digital
half (IBIS is a *drive-strength* model; JLS has no strength axis at all). And
#26 (IEEE 1164) genuinely rather than nominally — see below.

**Unlocks (pedagogy).** This is the frame's own named example, and it is worse
than the frame states. Today a bus conflict resolves to "the first active driver
in net order wins" where net order is "the order the wire ends were added to the
net … a breadth-first walk of the net from its first wire end in file order"
(`docs/simulation-semantics.md:429-443`). A student who draws two conflicting
drivers gets a *plausible, deterministic, wrong* answer plus a warning — and the
answer depends on the order they drew the wires. Real hardware gets a contention
current and an indeterminate level. With strengths: conflicts are visibly
different from valid values; open-drain and wired-AND become drawable (which is
how I²C and every interrupt line works); pull-ups become a thing students place
rather than a thing they are told about. New labs: I²C/1-Wire bus arbitration,
open-drain interrupt trees, tri-state bus with real contention, boundary scan.

**What is being papered over.** `src/jls/hdl/VhdlEmitter.java:469-495` — the
`when others` arm the frame cites — is one of three. The others are `:658` and
`:690`, and `:575-578` documents the pattern explicitly: "`when others`
(std_logic's non-binary values) also holds" and "with a `when others` arm
zeroing them (unreachable codes)". The emitter satisfies IEEE 1164's
nine-value full-coverage rule three separate times over a simulator with two
values plus a null reference. `ScannedPort.Direction.INOUT` is the second
paper-over: JLS can *parse* a bidirectional port declaration and has nowhere to
put it. The third is `TriState.react`'s `"off"` string sentinel
(`docs/simulation-semantics.md:79-82`) — a strength state smuggled in as an
event payload because the value domain has no room for it.

**Size:** 6–10 maintainer-weeks, and it is the item with the widest blast
radius (see Ripple effects). Layer 1 alone is ~2. Layer 2 is the value-domain
change and it is normative-document work as much as code.

---

#### D. A technology-cell layer: drawable cells with LEF-style abstract views

**Technically.** The largest and most consequential item. Four parts:

1. *Cells as data, not classes.* `ElementType` (`src/jls/elem/ElementType.java`)
   is already a descriptor + factory, and `docs/grand-architecture.md:281-292`
   already calls it "the seed of the plugin mechanism". Extend it so a cell can
   be defined by a record — name, pin list (name, direction, width), logic
   function, timing figure, area — loaded from a library file rather than
   compiled in. A `CellInstance` element renders as a box with named pins and
   `react`s by evaluating its function.
2. *A Liberty subset reader.* Not the timing engine — the *inventory*: cell
   names, pin directions, `function`, `area`, `capacitance`, and one delay
   figure. `sky130_fd_sc_hd` is ~440 cells; the subset that matters for teaching
   is ~40.
3. *A LEF reader for the abstract view.* `MACRO` size, `PIN`/`PORT`/`LAYER`
   rectangles, `OBS` blockages. This is the "LEF-style abstract views" the sweep
   brief asks about, and it is ~600 lines of a well-documented ASCII format.
4. *Importer acceptance of technology cells.* `NetlistImporter.mapCell`
   currently rejects any cell type not starting with `$`
   (`src/jls/hdl/imp/NetlistImporter.java:227-232`) — which is precisely every
   real standard cell. With a loaded library that branch becomes a lookup.

**Unlocks (standards).** #100 (LEF, read), #87 (Liberty, subset read), #109
(open PDKs as a data source — SKY130/GF180/SG13G2 all ship both), #110 (JLS can
now both emit *and re-import* a technology-mapped netlist, which is the actual
content of "flow conformance"), #111 (a cell-instance netlist is the logical
half of an LVS netlist; the printer is then ~1 week), #92 (ALF, partially),
and — the interesting one — **#89 SDF, which `docs/standards-adoption/11-costed-rejections.md:110-124`
correctly rejected on the grounds that "a JLS drawing has no cells; it has AND
gates and Registers", and whose own stated re-open condition (`:810`) is "JLS
acquires a technology-cell library *and* a name-stable synthesis path".** D is
literally that condition. The rejection was right and D is what changes it.

**Unlocks (pedagogy).** The largest single new teaching surface in this sweep,
and the one that has no substitute. Today every JLS gate has a fixed integer
delay from a table (`docs/simulation-semantics.md:283-300`) and **no area, no
input capacitance, no drive strength, no fanout limit, no load dependence**. A
student can wire one NOT gate to two hundred inputs and JLS will simulate it
happily at delay 5. With cells: fanout matters, buffering matters, drive
strength selection (`_1` vs `_4` vs `_8`) matters, area matters, and the
`X1 vs X4` tradeoff becomes a thing you can *measure in the tool*. New labs —
"map your adder to sky130 and count the cells"; "your critical path is nine
cells, here is each one's delay and load"; "add a buffer tree and watch the
delay drop"; "here is the same function in `and2_1` and `nand2_1 + inv_1`, which
is smaller?" That is a whole second-course unit that JLS cannot currently
express a single sentence of.

**What is being papered over.** The §7 delay table *is a cell library with the
technology removed*. AND=10, NAND=5, NOT=5 encodes a real fact (NAND is cheaper
than AND because AND is NAND+INV) in a form that cannot be questioned,
parameterised, or checked against anything. `Adder`'s "30 × bits (ripple-carry
model, recomputed from width)" is a hand-rolled per-element area/delay model —
one element got a technology model and the other thirty-two did not. And the
1:1 correspondence problem that killed SDF is *self-inflicted*: it exists only
because JLS's primitives are not cells. Draw in cells and it evaporates.

**Size:** 10–16 maintainer-weeks for the readable-library core (data-defined
cells 4–6; Liberty subset 2–3; LEF 2–3; importer 2; library packaging and the
"which cells ship / which are fetched" decision 1–2). The drawable-cell-layout
half is separable and belongs with E.

---

#### E. A layout view: GDSII / OASIS / DEF / CIF import and display, read-only

**Technically.** A second canvas mode behind the existing renderer seam:

- A GDSII stream reader — `BOUNDARY`, `PATH`, `BOX`, `SREF`, `AREF`, `TEXT`,
  `BGNSTR`/`ENDSTR`, layer/datatype, UNITS. It is a 1978 binary record format
  with ~20 record types that matter; the reader is bounded and testable, and
  JLS's hostile-input discipline (`UntrustedFileHardeningTest`,
  `ContainerMutationFuzzTest`, the `LoadError` taxonomy) applies unchanged.
- A layer-style model (colour, fill, visibility) read from a KLayout `.lyp`
  (#108) or an equivalent table.
- A coordinate-space change: `jls.core.Geometry` is integer pixels on a 12-px
  grid with `CIRCUITSIZE = 1000` (`src/jls/core/Geometry.java:17-20`), and
  `Viewport` clamps zoom to [0.25, 4.0] (`src/jls/edit/Viewport.java:58,64`).
  Layout wants nanometre integers over a 10⁶-unit extent and a 10⁵ zoom range.
  This is a *separate* coordinate space for a *separate* view, not a change to
  the schematic model — which is what makes it tractable.
- A spatial index sized for it. `jls.SpatialIndex` is a uniform grid for editor
  hit-testing; a layout view at classroom scale (one tile, ~10⁵ polygons) is
  within reach, a full SoC is not, and saying so in the docs is the honest
  posture.

**Unlocks (standards).** #102 (GDSII), #103 (OASIS, +3–4 wk — variable-length
integers, modal state, CBLOCK/zlib), #101 (DEF, which is placement over the
same canvas), #104 (CIF, nearly free once E exists), #108 (layer properties,
read).

**Unlocks (pedagogy).** "Show students the layout their gates became" — yes,
this is coherent, and the sweep brief is right to ask. But the *reason* is not
that JLS should compete with KLayout, which is free, excellent, scriptable and
better at this in every respect. The reason is **cross-probing**: with D, JLS
knows which cell instance corresponds to which drawn element, and no external
viewer can ever know that. Click a NAND you drew; the two `sky130_fd_sc_hd__nand2_1`
instances light up in the layout. Click a polygon; the schematic element
highlights. That single interaction is the entire pedagogical payload, it is
impossible in any other tool, and it is worthless without D. **E without D is a
worse KLayout. E with D is a thing that does not exist.**

**What is being papered over.** Nothing — this is a genuine absence rather than
a fiction. Worth stating plainly, because most items in this sweep are the
other kind.

**Size:** 8–12 maintainer-weeks for GDSII + layers + the second canvas
(reader 3–4; layer model and styling 1–2; canvas/viewport/index 3–4; cross-probe
wiring 1–2). OASIS +3–4. DEF +2. CIF +0.5.

---

#### F. A shuttle target: the board mechanism pointed at silicon

**Technically.** Generalize `jls.hdl.board` from `(name, fpga, format, pin map)`
(`src/jls/hdl/board/Board.java:26-27`) to a target descriptor that can also
carry a **wrapper template**. A `tinytapeout` target emits three artifacts from
the same `HdlExporter.buildModel` port walk `PcfEmitter` already uses:

1. a `tt_um_<name>` wrapper module with the fixed signature
   `(ui_in[7:0], uo_out[7:0], uio_in[7:0], uio_out[7:0], uio_oe[7:0], ena, clk, rst_n)`,
   binding the user's ports into it;
2. an `info.yaml` (top module, source files, pin documentation);
3. a LibreLane/OpenLane config stanza.

`PinBindings` and the all-or-nothing binding discipline (#213 P3 — "an
unbindable port set fails the whole export with every problem in one
`jls: error:` line", `docs/hdl-support-research.md:534-541`) transfer verbatim;
only the emitted artifact changes. `Board.Format` gains a constant beside `PCF`
exactly as its javadoc already anticipates for XDC/QSF
(`src/jls/hdl/board/Board.java:30-38`).

**Unlocks (standards).** #304 (as the *surviving* open-shuttle acceptance path;
the survey's row names an organization that no longer exists), #110/LibreLane
flow conventions, #109 (SKY130 as a named target with a named PDK version),
#86/#215 by analogy (the artifact handoff is the same shape as the iCE40
bitstream recipe already documented at `docs/hdl-support-research.md:549-556`).

**Unlocks (pedagogy).** "The class taped out a chip", which is not a small
thing. But the sober payload is the *budget*: 8 dedicated inputs, 8 dedicated
outputs, 8 bidirectionals, one clock, one reset, one tile of area. Every one of
those is a constraint students must design against, and constraints are where
digital design is actually taught. A course can run "fit your design in a tile"
as a term project with a real deadline and a real artifact.

**What is being papered over.** `docs/standards-adoption/06-fpga-constraint-formats.md:578-582`
already states the rule that governs this item: "a constraint file with no
documented path from `.jls` to a programmed board is half a feature". The same
rule applied to silicon is why F must not ship before A and B — a wrapper around
a design whose flops have no reset is a wrapper around a chip that does not
work.

**Size:** 2–4 maintainer-weeks *given* A, B and C. Effectively infinite without
them. This is the cheapest item in the sweep and the one with the strictest
prerequisites.

---

#### G. Electrical rule checking (the honest, small answer to "does JLS want DRC?")

**Technically.** A checking pass over the element graph — not over geometry:
undriven inputs, multiply-driven non-tri-state nets, combinational loops,
width mismatches, fanout beyond a cell's limit (with D), clock nets driving
data pins, registers with no reset in a design targeting an ASIC flow (with B).
It reports through the existing `LoadError`-shaped structured-diagnostic
discipline and the `TellUser` boundary.

**Unlocks (standards).** None directly, and it should not be sold as DRC or LVS
conformance — those are geometry and device-level respectively, and both stay
out. What it unlocks is the *reliability* of A/B/F: a design handed to an
external flow either passes JLS's own checks or the student is told why before
Yosys tells them in a worse way.

**Unlocks (pedagogy).** The one thing JLS most conspicuously lacks against
Digital and Logisim-evolution. Also: it is where the strength model (C) pays
off a second time, because "two strong drivers on one net" becomes a *checkable
static property* rather than a runtime warning fired once per run.

**Size:** 3–5 maintainer-weeks.

---

### Ripple effects

**Normative documents.**
- `docs/simulation-semantics.md` is **normative** and C rewrites its §2 (value
  domain, "There is no unknown/X state anywhere in the simulator"), §6.1 (wire
  ideality and the read-latest rule), §9 (tri-state and multi-driver
  resolution), §10 (bundle HiZ rules), and its §12 validation table. B rewrites
  §5 (initialization) and §8.1–8.2. D rewrites §7 (the delay table becomes a
  default, not a fact).
- `docs/batch-interface.md` is a **stability contract** and C touches §3.4
  (stdout `HiZ` rendering) and §4.3 (VCD `z`, "HiZ is all-or-nothing per
  signal"). A new strength or X value means a new stdout token and a new VCD
  character, which is a compatibility event for every autograder downstream —
  `examples/autograde` and `test/jls/AutogradeBridgeExampleTest.java` are the
  in-tree consumers.
- `docs/file-format.md` for the FORMAT bump; `docs/hdl-support-research.md` §7.2
  (its gap catalogue is A+B+C's specification, and should be rewritten as a
  changelog once they land); `docs/standards-landscape.md` §8/§9/§10/§12.i and
  §13.3 ("SDF consumption — the first step onto a timing-engine slope" is
  re-derived by D, and #304 is factually stale); `docs/extension-points.md` (D
  and E each publish a new seam beside `hdl.exporter`);
  `ARCHITECTURE.md`'s "Simulation execution strategy" recorded decision, whose
  binding equivalence criterion explicitly names "the two-states-plus-HiZ value
  domain and multi-driver/tri-state resolution (§2, §9)" — C changes the
  criterion itself, which must be a specified edit, never a silent one.

**File format.** `Circuit.FORMAT_VERSION` is 2 (`src/jls/Circuit.java:102`).
B, C and D each add persisted attributes; D adds a new savable element type,
which means a `SaveTags` row, an `ElementRegistry` row, and an
`AllElementsRoundTripTest` fixture (`ARCHITECTURE.md:120-137`, all sixteen
steps). Version-0 and version-1 files must keep loading unchanged —
`FormatHeaderTest` and `CircuitRoundTripTest` enforce this and are the right
enforcement.

**Element `react()` methods: 25.** `grep -rn "public void react" src/jls/`
returns exactly 25, in `Adder`, `Binder`, `Clock`, `Constant`, `Decoder`,
`Display`, `Extend`, `Gate`, `InputPin`, `JumpEnd`, `JumpStart`,
`LogicElement`, `Memory`, `Mux`, `OutputPin`, `Pause`, `Register`,
`ShiftRegister`, `SigSim`, `Splitter`, `StateMachine`, `Stop`, `SubCircuit`,
`TriState`, `TruthTable`. Change A touches 3 (Memory, SubCircuit,
ShiftRegister — export only, not `react`). Change B touches 3 (`Register`,
`StateMachine`, `Memory`). **Change C touches all 25**, because
`docs/simulation-semantics.md:60-66` records that "nearly every element's
`react` treats a null (HiZ) input as zero before computing" — every one of those
sites is a decision that a strength/X model must revisit deliberately. That
count is the honest measure of C's cost and the reason it is 6–10 weeks and not
3.

**The simulation hot loop.** `Simulator.runEventLoop` is untouched by A, B, D,
E, F, G. C touches `WireNet.propagate`, which is on the hot path: resolution
goes from "scan attached outputs, take the first non-null" to a lattice join
over all drivers. That is more work per net event, and
`docs/grand-architecture.md:346-360` is explicit that the hot plane must stay
inside `core` with no indirection — so the lattice must be a primitive
operation (a small int, a table lookup), not a polymorphic call. The `riscv/`
CPU-scale designs are the workload that would notice.

**The GUI.** D adds a palette category and a renderer for cell instances
(`src/jls/edit/ElementRenderers.java`, `Palette.java`, plus a `.gif` and a help
topic per the ritual). E adds a whole second view mode — `Viewport`'s
`MIN_SCALE`/`MAX_SCALE` (`src/jls/edit/Viewport.java:58,64`) and
`Geometry.CIRCUITSIZE` (`src/jls/core/Geometry.java:17`) are schematic
constants and must not be widened for layout; the layout view needs its own.
C adds strength rendering on wires (a colour or line-weight axis), which
collides with the ~126 hardcoded chrome/canvas colour call sites already
audited in `docs/flatlaf-evaluation-2026-07.md` and tracked as #76's follow-up.

**Existing saved circuits.** 19 `.jls` files in the tree —
`riscv/build/*.jls` (18), `riscv/gui/cpu.jls`, `test/fixtures/*.jls`. All must
load byte-compatibly after every FORMAT bump. `riscv/build/addi.jls` alone is
1038 elements (43 `Constant`, 43 `Mux`, 34 `AndGate`, 34 `Splitter`, 32
`Register`, 3 `Memory`, 3 `ShiftRegister`, 810 `WireEnd`) and is the natural
acceptance test for A: it should export, synthesize, and — with B — hold its
`RiscvCpuGoldenTest` behaviour through the round trip.

**Existing tests.** 29 Verilog + 29 VHDL goldens under `test/resources/hdl/`
plus `test/resources/hdl/board/blinky_icestick.pcf` re-baseline under A and B.
Tests that **assert the current limitation and must be inverted**:
`HdlPolicyTest.memoryIsRejectedByName`, `HdlPolicyTest.subCircuitIsRejectedCleanly`,
`VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ` (pins the
two-state domain), and the `SimulationSemanticsRegressionTest` cases
`multiDriverConflictResolvesDeterministicallyAndWarnsOnce` and
`registerInitialValueAppearsBeforeAnyClockEdge`. Tests that gain new
obligations: `ElementRegistryTest` (totality), `SaveTagsTest`,
`FileFormatSpecTest`, `HelpTopicsTest` (palette completeness),
`ElementConstructorContractTest`, `HeadlessCoreRatchetTest` (every new reader —
GDSII, LEF, Liberty — must be AWT-free), `ExtensionPointCatalogTest` (new
seams). CI gains lanes: LibreLane/OpenLane and KLayout would follow the existing
`jls.hdl.ToolLocator` + `Assumptions.assumeTrue` skip-if-absent pattern that
`GhdlCompileTest` and `IverilogCompileTest` already establish.

---

### What genuinely stays out, and why

Each of these fails one of the frame's three legitimate grounds. Stated with
the reason, not the size.

- **#114–#121 mask data preparation and lithography** (OASIS.MASK, MEBES, JEOL/
  Nuflare/IMS writer formats, curvilinear extensions, MRC, OPC/RET/ILT, SEMI
  P-series). *Different tool class.* OPC/ILT is a numerical inverse-imaging
  problem over an optical model; mask fracturing is shot-decomposition for a
  specific writer's beam. Neither has logical content — the input is polygons
  and a process model, the output is machine instructions. No change to a
  schematic editor's model produces or consumes them meaningfully. (Note: the
  *format* OASIS.MASK is a P39 dialect and E's reader would technically parse
  much of it; being able to read the bytes is not being able to do the work,
  and shipping a viewer for data a teaching tool can never produce would be
  theatre.)
- **#122–#127 fab equipment and factory automation** (SECS-II/GEM/HSMS, E87/
  E90/E94/E84, Interface A, E142/G85 wafer maps, M1 wafer specs, S2/S8 safety).
  *Different tool class, and a different industry* — these are manufacturing
  execution and equipment messaging. Nothing about them touches design data.
- **#128 IRDS.** Not a standard; a roadmap. Nothing to conform to.
- **#105 Si2 OpenAccess.** A membership-licensed C++ database API, not an
  interchange format; there is no artifact to read or write.
- **#106 iPDK / OPDK.** Parameterized *layout generators* in TCL/PyCell. Running
  them requires a layout engine and a device model; that is ground (a).
- **#107 Calibre SVRF / Pegasus / ICV.** Proprietary rule languages whose
  execution is a geometry engine over billions of polygons. No open grammar
  exists (the survey says so and is right).
- **#113 Touchstone**, and the analog half of **#112 IBIS**. S-parameters and
  I/V/t buffer curves require a continuous-time solver. Adding one is building
  SPICE, which is ground (a). The digital shadow of IBIS is C and is in scope;
  the analog is not.
- **#130 (1149.4 mixed-signal), #131 (1149.6 AC-coupled), #132 (1149.7 cJTAG),
  #133 (1149.10).** Each is defined by *electrical* behaviour — analog test
  buses, AC coupling detection, pin-count-reduced electrical signalling,
  high-speed serial. The digital protocol is the small part. Ground (a).
- **#136 IEEE 1838 (3D-IC test access).** The access network is drawable, but it
  presumes die stacking and a package model JLS has no reason to acquire.
  Declined on relevance, not on tool class — and it comes nearly free if
  #129/#134/#135 are ever built, so this is a "later, cheaply" rather than a
  "never".
- **#139 ATE native formats** (Advantest, Teradyne). Vendor tester programs,
  proprietary, no public grammar. (This is *not* an argument against #137/#138 —
  STIL and WGL are the open interchange layer above these, and they are in
  scope.)
- **#140–#146 PCB** (IPC-2581, IPC-D-356A, IPC-7351, IPC-2221/2222, IPC-A-610/
  J-STD-001, Gerber X2/X3, ODB++). *Different tool class* — this is KiCad's
  domain and KiCad is excellent at it. The one near-miss worth naming honestly:
  **IPC-D-356A is a netlist format**, and JLS has a netlist, so the emitter
  would be a printer. It stays out because a bare-board test netlist without a
  board layout has no consumer — the artifact is only meaningful attached to
  the fabrication data JLS will never produce.
- **#147–#151, #153 packaging and qualification** (JEDEC outlines, JESD47/
  JEP122, AEC-Q100/Q101, JS-001/JS-002/JESD78, MIL-STD-883/MIL-PRF-38535,
  IEC 60747). These are *physical test regimes applied to manufactured parts*.
  There is no design-tool artifact. Ground (a).
- **#152 ECSS-Q-ST-60-02C** is mis-shelved by the survey: it is a *development
  process* standard for ASIC/FPGA, not a physical test, and it belongs beside
  the §12.a tool-qualification family that `docs/standards-adoption/04-tool-qualification-and-scope.md`
  already adjudicated. It stays out for that section's reasons (audited
  multi-year process, commercial support commitment), not this one's.
- **#288–#303 foundry and EDA-ecosystem certification** (TSMC OIP ITC/ITF/RF,
  TSMC9000, Samsung SAFE-QEDA, Intel Foundry per-node qualification, PDK
  certification). The survey's reasoning survives the capability frame intact
  and deserves to be restated in the frame's own terms: **these are not
  capability gates, they are contract gates.** They certify a named tool version
  against a named PDK version under NDA, renewably, with a commercial support
  obligation. There is no change to JLS's value domain, element model, timing
  model, kernel, file format or UI that moves any of them one inch. That is a
  qualitatively different kind of "out" from everything else in this list, and
  the frame's three grounds do not have a slot for it — they should gain one.
- **#304** does *not* stay out, and its current text is wrong on the facts:
  Efabless shut down in March 2025 and chipIgnite went with it. The row should
  be rewritten around Tiny Tapeout on SKY130 (via ChipFoundry / Swiss Chips)
  and LibreLane (the early-2026 rename of OpenLane 2, moved to
  `librelane/librelane`), and re-marked from ADJACENT to a real target — see
  change F.

---

### Sources

**Repository, all read at HEAD.**

- `src/jls/hdl/HdlExporter.java:83-86` (reject policy: SubCircuit, Memory),
  `:190` (the throw), `:418-424` (`EXPORTED`, 21 classes, no ShiftRegister),
  `:427-429` (`SKIPPED`), `:432-433` (`TOPOLOGY`)
- `src/jls/hdl/HdlModel.java:28-33` (`Direction` = INPUT|OUTPUT only)
- `src/jls/hdl/VhdlEmitter.java:469-495`, `:575-578`, `:658`, `:690` (four
  `when others` full-coverage arms over IEEE 1164's nine values)
- `src/jls/hdl/scan/ScannedPort.java:12-20` (`Direction.INOUT` with no model
  counterpart)
- `src/jls/hdl/imp/NetlistImporter.java:227-232` (non-`$` cell types rejected —
  i.e. every technology cell), `:233-258` (mapped cells: `$not $and $or $xor
  $mux` only)
- `src/jls/hdl/yosys/CellValidator.java:54-64` (`SUPPORTED`: `$dff`/`$dlatch`,
  no `$adff`), `:97` (memory message)
- `src/jls/hdl/board/Board.java:26-27` (the `(name, fpga, format, pins)`
  record), `:30-38` (`Format` enum, PCF only, XDC/QSF anticipated)
- `src/jls/hdl/board/Boards.java:34` (`ICESTICK`, the only board)
- `src/jls/hdl/HdlExtensionPoints.java` (the `hdl.exporter` seam)
- `src/jls/elem/Register.java:274-386` (attributes: name, bits, init, orient,
  delay, type, watch — no reset)
- `src/jls/elem/ElementRegistry.java:37-73` (33 compiled-in types),
  `src/jls/elem/ElementType.java` (descriptor + factory)
- `src/jls/elem/WireNet.java:20-31, 404-407` (insertion-ordered driver sets,
  net value, conflict flag)
- `src/jls/core/Geometry.java:17-20` (`CIRCUITSIZE` 1000, `SPACING` 12)
- `src/jls/edit/Viewport.java:58, 64` (zoom clamped to [0.25, 4.0])
- `src/jls/edit/CircuitRenderer.java:99-100, 297-323` (canvas rect, SVG export
  through JFreeSVG)
- `src/jls/SpatialIndex.java:15-35` (uniform grid; correctness proved in
  `proofs/SpatialIndexCorrectness.agda`)
- `src/jls/Circuit.java:102` (`FORMAT_VERSION = 2`)
- `src/jls/JLSStart.java:759-788` (`FLAGS`: `-export`, `-board`, `-pins`)
- `test/resources/hdl/counter.v:19` (`reg [3:0] count = 4'h0;` — the FPGA-only
  power-on value), `test/resources/hdl/tristate.v`, `mux.v`, `register_pff.v`
- `test/jls/hdl/HdlPolicyTest.java:63-74` (`memoryIsRejectedByName`), `:77+`
  (`subCircuitIsRejectedCleanly`) — tests that pin the limitation
- `test/jls/VcdExportGoldenTest.java` (`vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`)
- `test/jls/SimulationSemanticsRegressionTest.java`
  (`multiDriverConflictResolvesDeterministicallyAndWarnsOnce`)
- `test/jls/SequentialGoldenTest.java`
  (`registerInitialValueAppearsBeforeAnyClockEdge`)
- Counts: 25 `public void react` methods across 25 files; 29 `.v` + 29 `.vhdl`
  goldens under `test/resources/hdl/`; 19 `.jls` files in-tree;
  `riscv/build/addi.jls` = 1038 elements.

**Documents.**

- `docs/simulation-semantics.md` (**normative**) §2:46-66 (two states + HiZ, no
  X), §3:79-82 (the `"off"` sentinel), §6.1:185-205 (ideal wires, read-latest),
  §7:283-300 (the delay table), §8.1:305-335 (register init), §8.4:392-404
  (memory write modes), §9:406-449 (tri-state and multi-driver resolution)
- `docs/batch-interface.md` §2, §3.1, §3.4, §4.2, §4.3 (a stability contract)
- `docs/hdl-support-research.md` §7.2:435-467 (the gap catalogue: `$adff`,
  clocked memories, `$mul/$div/$mod/$pow`, latches, x/z), §7.5:501-556 (board
  export as shipped; the iCE40 handoff recipe)
- `docs/standards-landscape.md` §7:282-306, §8:310-337, §9:339-373,
  §10:374-410, §12.i:525-…, §13.3
- `docs/standards-adoption/11-costed-rejections.md:78-124` (why SDF fails
  today), `:810` (its own re-open condition — which change D satisfies)
- `docs/standards-adoption/06-fpga-constraint-formats.md:20-62` (the testable
  proposition for constraint emission), `:560-600` (the "half a feature" rule)
- `docs/grand-architecture.md` §5:281-292 (registry as plugin seed),
  §6:346-370 (cold plane / hot plane)
- `ARCHITECTURE.md:120-137` (the sixteen-step element ritual), and the recorded
  decision "Simulation execution strategy" (its equivalence criterion names
  §2/§9 explicitly)
- `docs/flatlaf-evaluation-2026-07.md` (~126 hardcoded colour call sites, #76)

**External.**

- Tiny Tapeout Verilog submission template, module header verified by fetch:
  `https://raw.githubusercontent.com/TinyTapeout/tt08-verilog-template/main/src/project.v`
  — `tt_um_example(ui_in[7:0], uo_out[7:0], uio_in[7:0], uio_out[7:0],
  uio_oe[7:0], ena, clk, rst_n)`. Top-module name must begin `tt_um_`;
  `TinyTapeout/ttsky-*-template` repos are the current SKY130/ChipFoundry
  shuttle templates.
- Efabless ceased operations March 2025 (CEO statement: "unable to complete our
  latest funding round… shut down operations until further notice"), taking
  chipIgnite with it; Tiny Tapeout continued via Swiss Chips.
  eeNews Europe, Tom's Hardware, Hackster.io coverage, March 2025.
  **Consequence for `docs/standards-landscape.md` #304: the row names a defunct
  organization and should be rewritten.**
- OpenLane 2 renamed **LibreLane** in early 2026; repository moved
  `efabless/openlane2` → `librelane/librelane`, first LibreLane-branded release
  3.0.0, backwards compatible. FOSSi Foundation announcement, August 2025.
- SKY130 standard-cell count (`sky130_fd_sc_hd` ≈ 440 cells) — **unverified in
  this pass**; order of magnitude only, and the sizing for change D depends on
  the *teaching subset* (~40), not the full library.
- GDSII record-type count and OASIS's additional complexity (variable-length
  integers, modal state, CBLOCK/zlib) — from format knowledge, **not verified
  against SEMI P39 in this pass**; SEMI standards are paywalled. The GDSII
  reader sizing should be re-derived against an open implementation
  (KLayout's `dbGDS2Reader`, `gdstk`, `python-gdsii`) before being committed to.
- IEEE 1450 (STIL) and WGL grammar sizes — **unverified**; the claim made here
  is only that they are *vector-interchange* artifacts of the same class as VCD,
  which is a classification claim, not a cost estimate.
- No EDA tooling was available in this environment (`yosys`, `nextpnr-ice40`,
  `iverilog`, `ghdl`, `klayout`, `magic`, `openroad` all absent from `PATH`), so
  every end-to-end flow claim in this document is traced from the code and the
  formats, not executed. The one thing that *was* executed is the export failure
  quoted at the top, against `target/jls-5.0.5-SNAPSHOT.jar`.
