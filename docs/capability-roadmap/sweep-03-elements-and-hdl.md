## Element model gaps and HDL interchange fidelity

*Sweep 03. Survey entries #25–#41 (HDLs) and #74–#86 (netlist/synthesis
interchange), plus #4 (IP-XACT) and #38 (SystemRDL), read against
`docs/hdl-support-research.md` §7.2 under the capability-expansion frame:
the question is not "can JLS do this cheaply" but "what would JLS have to
become, and what else does that same change buy".*

The headline: **JLS's HDL interchange is not limited by its HDL code. It is
limited by its element vocabulary and its value domain.** Both emitters
(`src/jls/hdl/VerilogEmitter.java`, `src/jls/hdl/VhdlEmitter.java`) are
competent, golden-tested, and validated against real `iverilog`/`ghdl`
(`test/jls/hdl/IverilogCompileTest.java`, `GhdlCompileTest.java`). The
importer (`src/jls/hdl/imp/NetlistImporter.java`) is a working Yosys-JSON
consumer with a real gatekeeper. What blocks every remaining standard in
these two tiers is that the element set has no reset, no clocked memory, no
arithmetic beyond `Adder`, no width conversion, and no hierarchy instance —
and that the value domain has two states where every standard in the tier
assumes at least four.

Two facts that frame everything below:

- **JLS cannot HDL-export its own flagship design.** `HdlExporter.EXPORTED`
  (`src/jls/hdl/HdlExporter.java:418-424`) omits `SubCircuit`, `Memory`, and
  `ShiftRegister`; anything not in that set is an offender and the whole
  export is rejected (`HdlExporter.java:187-193`), pinned by
  `test/jls/hdl/HdlPolicyTest.java:67-100`
  (`memoryIsRejectedByName`, `subCircuitIsRejectedCleanly`,
  `rejectionListsEveryOffenderInOneMessage`). The `riscv/` RV32I CPU is built
  from `Adder`, `Mux`, `Register`, `Memory`, `ShiftRegister`, `Splitter`/
  `Binder`, `Decoder` and gates (`riscv/README.md:10-12`). It therefore
  cannot be exported to Verilog or VHDL at all. The "FPGA on-ramp" the
  research report calls the flagship feature stops at gate toys.
- **The importer's reject list is a mirror of the element set.**
  `src/jls/hdl/yosys/CellValidator.java:75-103` holds four hand-written
  student-facing apology messages — async reset, set/reset, wide arithmetic,
  clocked/multi-port memory. Each one is a sentence explaining that the
  student's perfectly ordinary Verilog must be rewritten because JLS lacks an
  element. That file is the most honest gap catalogue in the tree.

---

### The blocked standards

| # | Standard | What blocks JLS today (code) | Change that unblocks |
|---|---|---|---|
| **#26** | IEEE 1164 `std_logic_1164` (9-value) | Marked HAVE, but the value domain is 2-state + whole-signal HiZ (`docs/simulation-semantics.md` §2; `BitSetUtils`, null-for-HiZ). `VhdlEmitter.java:466-472` documents that its `when others` arm exists to satisfy "VHDL's full-coverage rule over std_logic's nine values" — the emitter already writes to a value model the simulator does not have. `VhdlEmitter.java:101` prints "this design drives '0'/'1'/'Z', never 'X'". | **C1** multi-value domain |
| **#31 / #76** | Verilog-2005 / structural gate-level netlist (export) | `VerilogEmitter.java:14,71-72` asserts "0/1/z only … never x". Export rejects `SubCircuit`, `Memory`, `ShiftRegister` (`HdlExporter.java:418-424`), so no hierarchical or memory-bearing design leaves JLS. `HdlModel` has no instance statement kind at all (`src/jls/hdl/HdlModel.java:197-762` — Gate/Replicate/Constant/TriState/Adder/Register/BitMap/Select/PriorityCase/StateMachine, and nothing else). | **C5** hierarchy + instance IR; **C3**; **C1** |
| **#25 / #27** | IEEE 1076 VHDL, 1076.3 `numeric_std` | Same as #31; additionally the adder is the only arithmetic construct emitted (`VhdlEmitter.java:350-375` builds a `bits+1` helper `unsigned`), so `numeric_std` conformance covers exactly one operator. | **C4** arithmetic family; **C5** |
| **#33** | IEEE 1800 SystemVerilog (accept as far as Yosys does) | Yosys accepts far more than `CellValidator.SUPPORTED` (`CellValidator.java:54-65` — 19 cell types). Every SV design using `always_ff @(posedge clk or negedge rst_n)`, a memory, or `*` hits a teachable reject. | **C2**, **C3**, **C4** |
| **#34 / #36** | SystemC / Accellera synthesizable subset (emit only) | No structural-instance IR to print from (`HdlModel` again). A SystemC printer is a third backend over the same walk, but the walk cannot represent a module instance. | **C5** |
| **#38** | Accellera SystemRDL 2.0 | Nothing in the element model represents an addressable register block: `Memory` (`src/jls/elem/Memory.java:184-202`) is one address port, one data port, `WE`/`OE`/`CS`, no field structure, no access policy (rw/ro/w1c), no reset value per field. `Register` (`Register.java:224-232`) has `D`/`C`/`Q`/`notQ` and nothing else. | **C8** register-block element; **C2** (per-field reset); **C7** |
| **#39 / #40** | Chisel/FIRRTL, CIRCT/MLIR hw dialects | Reachable only through firtool/Yosys → JSON, i.e. gated on the same importer gaps. A direct `hw`/`comb`/`seq` printer is gated on the instance IR. | **C5** (+ nothing new) |
| **#4** | IEEE 1685 IP-XACT | The survey calls this "structurally free" because a subcircuit is already a typed-port component (`standards-landscape.md:147-149`). It is not free: `SubCircuit` ports carry name/direction/width but no bus-interface role, no clock/reset semantics, no memory map, and the exporter cannot instantiate a subcircuit, so the `component` would describe something JLS cannot emit structurally. | **C7** port metadata; **C5** |
| **#74** | EDIF 2 0 0 / 4 0 0 | EDIF is fundamentally a hierarchical cell/instance/net language. With no instance statement in `HdlModel` and `SubCircuit` in the reject bucket, an EDIF writer would emit exactly one flat cell. The survey dismissed EDIF as "conformance theater" (`standards-landscape.md:275-277`); under this frame it is a *diagnostic* — it fails for the same reason Verilog export of the RISC-V CPU fails. | **C5** |
| **#75** | Yosys JSON netlist (import) | Marked HAVE, and it is real, but the realized mapper is *four gate types plus `$mux` plus constants* (`NetlistImporter.java:234-259`). `$add`, `$dff`, `$dlatch`, `$tribuf`, the reductions, `$bmux` and hierarchy are validated-then-refused (`NetlistImporter.java:251-257`). Any bit slice, concatenation or width mismatch is refused (`:317`, `:352`). Multi-module netlists are refused (`:156-159`). Async reset, set/reset, `$mul/$div/$mod/$pow`, and any memory with a write port or a clocked read port are refused by `CellValidator` (`:75-103`, `:233-246`). | **C2**, **C3**, **C4**, **C5**, **C6**, plus finishing the mapper |
| **#77** | BLIF | Same instance/hierarchy blocker as EDIF, plus BLIF's `.latch` has an init-value and a control (clock) field JLS's `Register` maps to only for the reset-free case. | **C5**, **C2** |
| **#82** | XDC / QSF / LPF constraints | Genuinely just printers over the existing port walk (`jls.hdl.board`, `PcfEmitter.java`) — **not blocked by the element model**. Roadmapped as #213 follow-ups. Listed here only to keep the boundary honest. | none needed |
| **#83** | JEDEC JESD3-C fuse map | Needs a two-level sum-of-products view of a circuit (PAL/GAL personality). JLS has `TruthTable` (`src/jls/elem/TruthTable.java`) and gates, but no minimizer and no device model. Not element-model-blocked; blocked on a small synthesis step (or delegation to an external `galette`/`galasm`). | small, independent |
| **#86** | Vendor bitstreams | Delegated to nextpnr/openFPGALoader by design (#215). Correctly out. | — |

---

### The changes, and what each unlocks

#### C1 — A multi-value logic domain (X, and per-bit Z)

**Technically.** Replace the `java.util.BitSet`-valued signal with a
value type carrying two bits per position (0/1/X/Z), or the full IEEE 1164
nine-value set (`U X 0 1 Z W L H -`). `docs/simulation-semantics.md` §2 is
explicit that "there is no unknown/X state anywhere in the simulator" and
that "HiZ is all-or-nothing per signal: a value is either fully driven or
fully HiZ", represented as a **null reference** rather than a value. The
change is: a real `Value` type with a resolution function; `WireNet.propagate`
(`src/jls/elem/WireNet.java:443-529`) resolves by the 1164 resolution table
instead of "first active driver in net order wins"; every element's `react`
computes over the new type instead of treating null as zero.

**Standards unlocked.**
- **#26 IEEE 1164** for real, not as a `when others` fig leaf.
- **#25/#31 export honesty**: emitted VHDL/Verilog stops carrying the
  disclaimer comments at `VhdlEmitter.java:101` and `VerilogEmitter.java:71-72`.
- **#75 import**: the last item on the §7.2 gap list ("x/z bits (JLS BitSet is
  2-state); coerce and document") closes. Yosys `x` in constant vectors,
  `$dff` with no init, and don't-care outputs from `opt_dontcare` become
  representable instead of coerced.
- **#66 VCD** (already HAVE) gains the `x` value it currently can never emit;
  **#67/#68** (EVCD/FST) become reachable rather than pointless.
- **#33** SystemVerilog four-state semantics stop being a lie in the
  import direction.

**Pedagogically.** This is the single biggest teaching change in the sweep.
Today:
- A **bus conflict** resolves to "the first active driver in net order wins"
  plus one warning (`WireNet.java:476-482`, `docs/simulation-semantics.md` §9).
  That teaches students something false about hardware. Real contention is X
  (or smoke). After C1, a conflict *propagates* X downstream and the student
  watches their whole datapath go red — which is what actually happens.
- An **uninitialized register** reads as 0. Students therefore never learn why
  reset exists; the `riscv/` PC "resets" by having initial value 0
  (`riscv/README.md:98-99`), which is a simulator convenience, not hardware.
  After C1 (with C2) a register powers up X and the design genuinely needs a
  reset sequence to work — the lesson lands by itself.
- **Metastability / setup-hold** teaching becomes expressible at all (X on a
  timing violation), which is currently impossible.
- **Don't-care** in truth tables and Karnaugh maps has an actual runtime
  representation instead of being lowered to 0 (`HdlExporter.java:711-716`
  documents "an output don't-care is lowered to 0").

**Currently papered over.** `VhdlEmitter.java:466-472` (the `when others` arm
that "satisfies VHDL's full-coverage rule over std_logic's nine values" while
the simulator has three); `VerilogEmitter.java:14,71-72`; the null-as-zero
convention in every `react` (`docs/simulation-semantics.md` §2 lists
`Gate.computeOutput`, `Register.react`, `Adder.react` explicitly — and
`Register.react` at `src/jls/elem/Register.java:757-765` does exactly
`if (inVal == null) inVal = new BitSet();`); the first-driver-wins warning;
`CellValidator`'s silent absence of any x/z handling.

**Size: 10–16 maintainer-weeks.** This is the largest item in the sweep and
the only one that touches the hot loop. It is 24 element `react()` methods,
`BitSetUtils`, `WireNet`, the VCD writer, the batch stdout formatter, the
trace window, the save format for `Constant` values, and every golden test.
It is also the change with the widest blast radius, and the only one that
changes what JLS *teaches* rather than what it can *represent*.

**Staging that de-risks it:** introduce the value type behind
`BitSetUtils`-shaped accessors first (mechanical, no semantic change), then
flip resolution and initialization in one specified, documented step with
`docs/simulation-semantics.md` §2/§9 rewritten in the same commit — the
ARCHITECTURE.md rule for simulation changes ("a specified, documented change
to `docs/simulation-semantics.md` first, never a silent behavioral
difference", `ARCHITECTURE.md:363-368`) applies verbatim.

---

#### C2 — Register control pins: async clear/preset, sync reset, clock enable, load

**Technically.** `Register` today has exactly four puts —
`D`, `C`, `Q`, `notQ` (`src/jls/elem/Register.java:224-232`, repeated per
orientation at `:234-262`) — and three types (`Latch`, `PosFF`, `NegFF`,
`:33-39`). The change: optional pins, each gated by a saved boolean
attribute, in the `Memory.sync` style already established by issue #199
(`Memory.java:193-197`: "synchronous-write clock … appended last so the
pre-#199 input indices are unchanged"). Add `CLR` (async, active-high or
-low), `PRE`, `EN` (clock enable), `LD`, and a per-instance reset value
distinct from the initial value. `Register.react`'s three-way switch
(`:747-800`) grows an async arm that fires on the reset pin's edge
independently of `C`.

**Standards unlocked.**
- **#75 import**: the entire `$adff` family — `$adff`, `$adffe`, `$aldff`,
  `$aldffe`, `$adlatch` — currently rejected at `CellValidator.java:132-136`,
  and the `$sr`/`$dffsr`/`$dffsre`/`$dlatchsr` set at `:137-139`. Those are
  ten of the fifteen entries in `buildTeachable`. `dffunmap` already handles
  sync-reset and clock-enable exactly, so C2 specifically buys the *async*
  half plus a native (non-`$mux`-expanded) clock enable.
- **#33/#31**: `always_ff @(posedge clk or negedge rst_n)` — the single most
  common sequential idiom in every textbook and every real design — imports
  instead of being refused with a rewrite instruction.
- **#77 BLIF**: `.latch` carries a control signal and an init value; a
  reset-bearing register is closer to a faithful `.latch`.
- **#76/#25/#31 export**: emitted registers gain a reset process, which is
  what every FPGA synthesis tool wants to see and what makes exported HDL
  actually usable on a board after power-up.

**Pedagogically.**
- **Realistic CPU reset sequences become drawable.** Today the `riscv/` PC
  "resets" via its initial-value attribute; there is no reset *signal*, so
  students cannot draw a reset line, cannot see the machine held in reset,
  cannot single-step out of reset. With C2 the reset line is a wire like any
  other and the CPU becomes a faithful teaching artifact rather than one that
  relies on a save-file attribute.
- **The classic SR latch, D latch with clear, JK flip-flop with preset/clear**
  — the standard first-year sequential-logic parade — become first-class
  elements instead of gate constructions.
- **Clock enable vs. gated clock** becomes teachable as the *design choice it
  is*: today the `riscv/` register file gives each of 31 registers "a hold/load
  `Mux` on its `D` input so unselected registers keep their value"
  (`riscv/README.md:113-116`) — 31 muxes that exist only because `Register`
  has no enable. That is the papering-over, drawn 31 times.

**Currently papered over.** `CellValidator.ASYNC_RESET_MESSAGE`
(`:75-81`) and `SET_RESET_MESSAGE` (`:83-87`) — two paragraphs of prose
apologising for a missing pin; the `riscv/` per-register hold Mux; the
"reset value 0" initial-value convention in `riscv/README.md:98-99`.

**File-format story.** Pure additive attributes, written only when non-default,
exactly the `Memory.sync` precedent (`docs/file-format.md:307`, `:471-479`).
Old circuits are byte-identical on re-save. Old readers loading a new file
ignore the unknown names and load a register *without* its reset — which
changes simulation behavior, so this is squarely the "silent-drop caveat"
class (`docs/file-format.md:459-479`) and, per that section's own advice
("Writers SHOULD prefer a version bump over an 'ignorable' attribute whenever
dropping the attribute would change simulation behavior"), it should carry a
`FORMAT 3` bump. That also resolves the open question #199 left behind.

**Size: 3–5 maintainer-weeks.** Element + `RegisterDialog.java` (454 lines) +
`RegisterRenderer.java` (346 lines) + save attributes + `HdlExporter`
register template + `HdlModel.RegisterStatement.Kind`
(`HdlModel.java:396-400`) + both emitters + `CellValidator` +
`NetlistImporter` mapper + goldens.

---

#### C3 — Memory: clocked ports, multiple ports, separate read/write ports

**Technically.** `Memory` (`src/jls/elem/Memory.java`) is one address input,
one data input, `WE`/`OE`/`CS`, one tri-state data output, optionally one
write clock (`:184-202`). Reads are asynchronous and level-gated; writes are
level-sensitive unless `sync` is on (`:1369-1383`). The change: a port list
instead of a fixed pin set — *N* read ports and *M* write ports, each
independently clocked or combinational, each with its own address/data/enable,
plus a declared read-during-write policy and per-port byte/bit write masks.

**Standards unlocked.**
- **#75 import**: `CellValidator.checkMemory` (`:233-246`) accepts exactly
  `WR_PORTS == 0 && RD_PORTS == 1 && RD_CLK_ENABLE == 0` — an async-read ROM.
  Every RAM, every register file, every FIFO, every inferred block RAM in
  every real design is rejected with `MEMORY_MESSAGE` (`:97-103`). This single
  change moves `$mem_v2` from "one shape works" to "the common shapes work",
  and lets the pipeline drop `memory -nomap`/`memory_map` workarounds.
- **#76/#25/#31 export**: `Memory` leaves the reject bucket
  (`HdlExporter.java:84`), so *any* design with storage becomes exportable —
  which is most designs, and all interesting ones.
- **#38 SystemRDL** partially: a multi-port addressable block is the substrate
  a register-map element sits on (see C8).
- **#82/#86 (via delegation)**: a clocked two-port memory is what Yosys/nextpnr
  infer into FPGA block RAM. Today a JLS design's memory could not even reach
  them.

**Pedagogically.**
- **A register file becomes a register file.** Today `riscv/` builds 31
  discrete 32-bit `Register`s plus a `Decoder`, per-register AND gates for
  write enables, per-register hold `Mux`es, and two 32-way read `Mux`es
  (`riscv/README.md:113-116`). That is a beautiful gate-level lesson *once*;
  after that it is noise obscuring the datapath. With a 2-read-1-write clocked
  memory, students can draw the textbook register file — and can still draw
  the gate-level one to compare. Both lessons become available; today only one
  is.
- **Read-during-write** and **write-first vs. read-first** become teachable —
  these are the questions FPGA labs actually stumble on.
- **Synchronous vs. asynchronous memory timing** stops requiring the hack in
  `riscv/README.md:124-126`: the data memory's "write-enable is gated so a
  store commits only in the clock-low phase, after the datapath has settled,
  preventing spurious writes to transient addresses". That gate exists purely
  because the memory model is level-sensitive; it is a workaround a student
  must be told to copy without understanding.
- **Caches, FIFOs, dual-port frame buffers, and pipelined memory** become
  drawable at all.

**Currently papered over.** `CellValidator.MEMORY_MESSAGE`; the #199 `sync`
attribute (a half-step already taken, and the fact it was taken is evidence
the model is thin); the `riscv/` clock-phase write gate; the 31-register
register file.

**File-format story.** The port list is new structure, not a new item kind —
representable as repeated `pair`/`String` items in the existing grammar
(`docs/file-format.md:122`), so no new item kind and no forced bump on
grammar grounds. But a port-list-bearing Memory loaded by an old reader would
silently become a single-port memory, which is a behavior change → `FORMAT`
bump, same reasoning as C2.

**Size: 4–7 maintainer-weeks.** The element (1547 lines today, the largest in
`jls.elem`), `MemoryDialog`/`MemoryContentsDialog`/`MemoryRenderer`/`MemTrace`,
the `MemoryRead`/`MemoryWrite` payload records (`src/jls/sim/SimEvent.java:65-79`
— they carry a bare address, no port index), `CellValidator.checkMemory`,
the importer's memory mapper (does not exist yet), and both emitters.

---

#### C4 — An arithmetic element family: multiplier, divider, comparator, subtractor, counter

**Technically.** `Adder` (`src/jls/elem/Adder.java`) is the entire arithmetic
vocabulary: `A`, `B`, `Cin` → `S`, `Cout` (`:103-107`), with a ripple-carry
delay model of `bits × 30` (`:261`). Everything else is built from it or from
gates. The change: new `LogicElement` subclasses — `Multiplier` (signed/
unsigned, optionally with a latency parameter), `Divider` (quotient +
remainder, floor/trunc selectable), `Comparator` (`<`, `≤`, `=`, signed and
unsigned, one element with output flags), `Subtractor`/`Negate`, and a
`Counter` (up/down, load, enable, terminal count) which is the sequential
member of the family and pairs with C2.

**Standards unlocked.**
- **#75 import**: `$mul`, `$div`, `$divfloor`, `$mod`, `$modfloor`, `$pow` —
  the `ARITHMETIC_MESSAGE` reject list at `CellValidator.java:140-143`. Also
  lets `$sub`, `$eq`, `$ne`, `$lt`/`$le`/`$gt`/`$ge` map to a *single element*
  instead of the gate meshes `test/resources/hdl/jls_map.v` currently rewrites
  them into (`$eq` → `$xor` + `$reduce_or` + `$not`; `$ne` → `$xor` +
  `$reduce_or`). Those techmap rules are literally JLS compensating for
  missing elements — and the compensation produces an imported schematic that
  is a pile of anonymous gates where the source said `a == b`.
- **#25/#27 VHDL `numeric_std`**: conformance currently covers `+` and nothing
  else. Multiplication and comparison operators become emittable.
- **#31/#76**: exported Verilog gains `*`, `/`, `%`, and relational operators,
  which is what makes exported HDL synthesizable into DSP blocks.

**Pedagogically.**
- **The array multiplier vs. the `*` operator** is one of the best lessons in
  a digital-logic course: draw the shift-and-add structure, then replace it
  with a single element, then compare area/delay. Today only the first half is
  possible, and `CellValidator.ARITHMETIC_MESSAGE` (`:89-95`) literally
  instructs students to "build the operation structurally (for example,
  shift-and-add multiplication)" as a *workaround*, not as a lesson.
- **ALU design** stops requiring the `riscv/` trick of deriving `slt`/`sltu`
  "from the subtractor's sign and carry" (`riscv/README.md:117-121`) — clever,
  but a comparator element makes the intent readable.
- **Timing/area tradeoffs become measurable**: the `Adder`'s `bits × 30`
  ripple model (`Adder.java:261`) is JLS's only structural timing statement.
  A carry-lookahead vs. ripple comparison, or a combinational vs. pipelined
  multiplier comparison, needs at least two arithmetic elements with different
  delay models to be a comparison at all.
- **`ShiftRegister` is a misnomer that should be fixed here too**: despite the
  name it is a stateless barrel shifter (`src/jls/elem/ShiftRegister.java:20-34`,
  `docs/file-format.md:316`). A genuine serial shift register (SISO/SIPO/PISO,
  with load and enable) is the classic sequential lab exercise and JLS does not
  have one. It is a C2+C4 element.

**Currently papered over.** `test/resources/hdl/jls_map.v` in its entirety —
three techmap rules whose only purpose is to lower operators JLS cannot
represent into gate meshes it can; `CellValidator.ARITHMETIC_MESSAGE`;
`riscv/`'s subtractor-derived comparisons and XOR-plus-`Cin` subtract.

**Size: 4–6 maintainer-weeks for the family** (~1.5 weeks for the first
element including the plumbing pattern, ~0.75 each after). Each is a new
`ElementRegistry` entry (`src/jls/elem/ElementRegistry.java:37-70`), a
`Palette` entry (`src/jls/edit/Palette.java`), a renderer, a dialog, an
`HdlModel` statement kind, two emitter arms, an importer arm, and a
`CellValidator` promotion.

---

#### C5 — Hierarchy in interchange: a `SubCircuit` instance statement in the netlist IR

**Technically.** `HdlModel` (`src/jls/hdl/HdlModel.java`) has ten statement
kinds and none of them is "instantiate a module". `HdlExporter` walks one
circuit and produces one module; `SubCircuit` is in the reject bucket
(`HdlExporter.java:84`, `:418-424`). The change: an `InstanceStatement`
(module name, instance name, port-to-net bindings), a multi-module `HdlModel`
(one module per distinct subcircuit *type*, emitted once, instantiated N
times), and the corresponding import direction — `NetlistImporter` currently
refuses any netlist with more than one module (`:156-159`) and any non-`$`
cell (`:228-232`).

**Standards unlocked.**
- **#74 EDIF** becomes writable as EDIF rather than as a flattened
  impersonation of it. This is the entry the survey wrote off as "conformance
  theater" (`standards-landscape.md:275-277`); the honest reading is that EDIF
  is a *test* JLS currently fails for a structural reason, and passing it means
  JLS has a real hierarchical netlist IR.
- **#77 BLIF** (`.subckt`), **#76** structural Verilog, **#25/#31** hierarchical
  export.
- **#34** SystemC structural emission becomes a third printer over the same IR
  (modules + `sc_signal` wiring is exactly instance + net binding).
- **#4 IP-XACT**: a `component` describing a subcircuit is only meaningful if
  the tool can also *emit the structure* the component describes.
- **#75 import**: hierarchy import (Yosys without `flatten`), so an imported
  design keeps the module structure the author wrote instead of becoming one
  enormous flat sheet — which for anything above toy size is the difference
  between a usable import and an unusable one.
- **#39/#40** FIRRTL/CIRCT printers, if ever wanted, ride the same IR.

**Pedagogically.** Hierarchical design is *the* structural lesson JLS teaches
well — subcircuits are a first-class element and students use them from week
one. Today that lesson cannot leave the tool: a student's beautifully
decomposed 4-bit ALU built from 1-bit slices exports as nothing at all. After
C5, hierarchy survives the round trip to an FPGA, and "your module boundary is
also a synthesis boundary" becomes a teachable fact rather than an assertion.

**Currently papered over.** `HdlPolicyTest.subCircuitIsRejectedCleanly`
(`test/jls/hdl/HdlPolicyTest.java:81-85`) — the rejection is *tested*, i.e.
the limitation is pinned as intended behavior; `NetlistImporter.java:156-159`
telling users to run Yosys `flatten`; `NetlistImporter.java:228-232` telling
them the same thing again per-cell.

**Size: 3–4 maintainer-weeks** for export + IR; **+2** for the import
direction; **+2** for an EDIF writer; **+1** for BLIF. No file-format change
at all — `SubCircuit` already saves fine; this is purely an interchange-layer
change, which makes it the cheapest high-value item in the sweep.

---

#### C6 — Width conversion and bit-level connectivity

**Technically.** `Extend` (`src/jls/elem/Extend.java:91-100`) has a **1-bit**
input replicated to N bits. There is no N→M sign-extend or zero-extend
element; `riscv/` builds RV32I immediate sign-extension from `Binder`s plus
`Extend` on bit 31 (`riscv/README.md:109-112`). On the import side, any width
mismatch is refused (`NetlistImporter.java:313-320`, `:343-353`), and bit
slices/concatenations "that would need a Splitter/Binder mesh" are refused
wholesale (`NetlistImporter.java:41-46`). The change: (a) a proper
`WidthAdapter` element (N→M, sign or zero extend, truncate) and (b) the
importer's Splitter/Binder mesh synthesis so that arbitrary Yosys bit vectors
— which are *always* bit-level — become real connections.

**Standards unlocked.** **#75** — this is arguably the largest single
unlock for import volume, because Yosys' JSON connection model is bit-level by
construction (`src/jls/hdl/yosys/YosysNetlist.java:18`) and real designs slice
and concatenate constantly. **#31/#76** export gains `$sext`/`$pad`-shaped
constructs. **#33** ditto.

**Pedagogically.** Sign extension is a core RV32I lesson (`riscv/` has to
hand-build it five times, once per immediate format). A width-adapter element
makes "why does `lb` sign-extend and `lbu` not" a one-element difference on
the schematic instead of a Binder rewiring exercise.

**Currently papered over.** Four separate "not built in this increment"
messages in `NetlistImporter` (`:317`, `:352`, `:255-257`, plus the class
Javadoc at `:41-46`); the `riscv/` Binder+Extend immediate generator.

**Size: 1–2 weeks for the element; 2–3 weeks for the importer's bit-level
mesh synthesis.** The mesh synthesis is the single highest-leverage
importer task remaining and does not depend on any other change here.

---

#### C7 — Port metadata on subcircuits: roles, buses, clock/reset semantics

**Technically.** A `SubCircuit`'s interface today is a set of `InputPin`/
`OutputPin` elements carrying name, direction, and width. The change: an
optional per-port role annotation (clock / reset / data / address / valid /
ready), optional grouping of ports into a named *bus interface* with a
declared protocol, and an optional documentation string. Stored as additive
attributes on the pins.

**Standards unlocked.** **#4 IP-XACT** (`busInterface`, `abstractionType`,
port maps — this is exactly the payload the survey claimed was already
present, and this is what is actually missing); **#11 Wishbone B4** and
**#22 I²C/SPI** as *declared* interfaces rather than incidental wire bundles;
**#74 EDIF** properties; **#38 SystemRDL** address-map attachment (C8's
prerequisite).

**Pedagogically.** Interface-based design — "this block is an AXI-Lite slave",
"this port is the reset" — is how real design is taught after gates. JLS
today cannot say a wire *is* a clock; `HdlExporter` infers a clock only by
the presence of a `Clock` element (`HdlExporter.java:271,485`). Making
port roles explicit also lets the exporter emit correct synthesis attributes
and lets the board-binding flow (`jls.hdl.board`) auto-bind clocks and resets
instead of requiring every pin by hand
(`docs/hdl-support-research.md:520-540`).

**Currently papered over.** `HdlExporter.java:485-487` treating `Clock` and
`InputPin` identically ("ports drive their nets directly") and the board pin
file requiring the user to bind `clk` manually like any other port
(`docs/hdl-support-research.md:527-536`).

**Size: 3–4 weeks** for the metadata + IP-XACT writer; the writer alone is
~1 week once the metadata exists.

---

#### C8 — An addressable register-block element (the SystemRDL angle)

**Technically.** A new element: a memory-mapped register block with a declared
address offset, a set of registers, and per-register *fields* with width,
offset, access policy (`rw`, `ro`, `wo`, `w1c`, `rclr`), reset value, and
optional hardware-side ports. Its bus side is an address/data/enable port
group (C7); its hardware side is a set of field-width outputs and inputs.

**Standards unlocked.** **#38 Accellera SystemRDL 2.0** in both directions —
import an `.rdl` file into a drawable register block, and export a drawn one
back to `.rdl` (and, from there, to the C headers and documentation the
SystemRDL ecosystem generates). Also **#4 IP-XACT** memory maps, which are
the same data in XML. Also feeds **#11 Wishbone** peripherals.

**Pedagogically.** Memory-mapped I/O is the bridge between "digital logic"
and "computer architecture", and it is currently undrawable in JLS except as
a hand-built address decoder plus discrete registers. A register-block element
makes "write 1 to bit 3 of 0x40 to clear the interrupt" something a student
can *draw*, wire to a `riscv/` CPU, and then read back in software. That is a
whole lab that does not exist today.

**Currently papered over.** Nothing — this is a genuine absence rather than a
workaround, which is why the survey marked it COULD and moved on. It is worth
doing because it is the natural next teaching layer above the CPU that
`riscv/` already proves JLS can host.

**Size: 4–6 weeks** (element + dialog + SystemRDL reader/writer). Depends on
C3 (multi-port addressable storage) and C7 (bus interface metadata).

---

#### C9 — Round-trippability: what would have to be true

`docs/hdl-support-research.md:100` records that round-trip is "not observed in
any surveyed tool", that "generated HDL and hand-written HDL are stylistically
disjoint", and — normatively — **"do not promise this"**. That verdict is
correct *for arbitrary HDL*. It is not correct for the case JLS uniquely
controls: **JLS-exported HDL re-imported into JLS.** JLS owns both the emitter
and the importer, which no other tool in the survey does.

For that closed loop, here is exactly what would have to be true:

1. **A declared closed subset.** A named set of JLS elements whose HDL
   rendering, pushed through the fixed Yosys pipeline
   (`test/jls/hdl/imp/ImportPipelineTest.java:109-112`), returns cells that
   map back to the *same* elements. Today: gates and `$mux` close; `Register`
   would close once `$dff` is realized in the mapper; `Adder` closes once
   `$add` is realized; `Memory`, `SubCircuit`, `TruthTable`, `StateMachine`
   do not close at all.
2. **Provenance attributes surviving synthesis.** The emitter tags each
   construct with the originating element's stable id and type — Verilog
   `(* jls_id = "..." , jls_type = "TruthTable" *)`, VHDL attribute
   declarations. Yosys carries cell attributes into `write_json`
   (`YosysNetlist` already parses an `attributes` map — `NetlistImporter.java:141`
   reads the `top` attribute this way), but `opt`/`opt_clean` will drop
   attributes on cells it merges. So the pipeline needs a preservation mode:
   `(* keep *)` on tagged cells, or `hierarchy -keep` with each non-primitive
   element emitted as its own module.
3. **Behavioral elements emitted as black boxes.** `TruthTable` and
   `StateMachine` currently lower to `casez`/`case` templates
   (`HdlExporter.java:710-760`, `HdlModel.PriorityCaseStatement`,
   `StateMachineStatement`). Synthesized, they become anonymous gate meshes
   that can never be recovered. Round-trip mode must emit them as separate
   modules marked `(* blackbox *)`/`keep_hierarchy`, so the importer sees one
   named cell and restores the original element.
4. **Geometry as a sidecar.** Layout is the thing HDL has no place for. JLS
   should emit an `x/y/orientation` sidecar keyed by the same stable ids, so
   re-import restores the drawing instead of re-running
   `HeuristicLayeredLayouter` (`src/jls/hdl/layout/`). No HDL-first tool can
   do this because it has no layout to preserve; JLS does.
5. **A round-trip CI property.** `export → yosys → import → save`, asserted
   equal to the original save text modulo element ids. This is a
   golden-testable invariant and its absence is why nobody claims round-trip.
   With the four items above, it becomes a *test*, not a promise.

**What is still honestly impossible:** round-tripping *hand-written* HDL
through JLS and back to recognizable hand-written HDL. That should stay
un-promised, exactly as the research report says. The correct claim to make is
narrower and defensible: **"JLS-exported HDL re-imports to the circuit it came
from, and CI proves it."** No tool in the survey makes that claim either.

**Size: 6–10 weeks on top of C1/C2/C5**, most of it in the preservation
pipeline and the attribute plumbing.

---

### Ripple effects

**Normative documents.**
- `docs/simulation-semantics.md` — **§2 (value domain), §7 (per-element
  delays), §8 (sequential semantics), §9 (tri-state and multi-driver
  resolution)** all change under C1 and C2. §2's "there is no unknown/X state
  anywhere in the simulator" and §9's "first active driver in net order wins"
  are the two sentences that must be rewritten. `ARCHITECTURE.md:363-368`
  binds any such change to be documented *first*.
- `docs/batch-interface.md` §3.4/§4.3 — the stdout `HiZ` rendering and the
  VCD `z` mapping gain an `x`; the batch interface is a stability contract, so
  this is a versioned change to a published de facto standard of JLS's own.
- `docs/file-format.md` — §5's per-type attribute table (`:297-325`) gains rows
  for every new attribute and element; §9's version history gains **FORMAT 3**
  (C2 and C3 both change simulation behavior when their attributes are dropped,
  which §9 itself says should prefer a bump); the open question about #199's
  `sync` attribute (`:471-479`) gets resolved in the same bump.
- `docs/hdl-support-research.md` §7.2 — the gap list becomes a changelog.
- `docs/standards-landscape.md` — #26 moves from HAVE to genuinely HAVE; #74,
  #77, #4, #38 move from COULD to ROADMAP; §13.3's "EDIF — conformance to a
  dead format" and §13.1's "IP-XACT … structurally free" both need correcting.

**Simulation kernel and elements.**
- **24 concrete `react(long now, Simulator, SimEvent.Payload)` implementations**
  (plus the base in `LogicElement.java`): `Adder`, `Binder`, `Clock`,
  `Constant`, `Decoder`, `Display`, `Extend`, `Gate`, `InputPin`, `JumpEnd`,
  `JumpStart`, `Memory`, `Mux`, `OutputPin`, `Pause`, `Register`,
  `ShiftRegister`, `SigSim`, `Splitter`, `StateMachine`, `Stop`, `SubCircuit`,
  `TriState`, `TruthTable`. C1 touches every one (each currently does
  `if (x == null) x = new BitSet()`); C2/C3/C4 touch two and add ~6 new ones.
- `SimEvent.Payload` is a **sealed interface with seven records**
  (`src/jls/sim/SimEvent.java:23-90`) and every `react` switches exhaustively
  with no default arm — by design, "adding a payload kind is a compile error at
  every consumer until it is handled" (`:16-21`). Multi-port memory needs
  `MemoryRead`/`MemoryWrite` to carry a port index; async reset may want its own
  payload. Each addition is a deliberate 24-site compile break — a feature, but
  budget for it.
- The **hot loop** (`Simulator.runEventLoop`) is unchanged in shape by all of
  C2–C8. Only C1 touches per-value cost, and `ARCHITECTURE.md:340-357` records
  the standing decision that the event interpreter is the *sole* strategy with
  a revisit trigger of "a concrete CPU-scale design … unusably slow
  interactively". A 4-state value doubles per-bit storage and adds a resolution
  step; if that trips the trigger, the recorded answer is a levelized pass
  inside `core`, not a change to the value model.

**Registries, GUI, and the palette.** Every new element needs: an
`ElementRegistry` line (`src/jls/elem/ElementRegistry.java:37-70`, 33 entries
today — and `jls.ElementRegistryTest` enforces *totality*, so forgetting the
line is a build failure); a `Palette` entry (`src/jls/edit/Palette.java`); a
renderer and a dialog under `src/jls/edit/` (the existing per-element pairs —
`RegisterDialog`/`RegisterRenderer`, `MemoryDialog`/`MemoryRenderer` — are
the pattern, ~350–450 lines each); registration in
`BuiltinElementRenderers`/`ElementDialogs`; and `SaveTags` stability.

**Existing saved circuits.** All C2–C8 changes are additive attributes or new
element types. Existing `.jls` files load unchanged and re-save byte-identically
(the new attributes are written only when non-default, the `Memory.sync`
precedent). New element *types* fail loudly in old readers with "no element
type named X", which `docs/file-format.md:433-435` explicitly classifies as
detectable rather than a misparse. The one genuine compatibility cost is C1:
a circuit that relied on undriven-reads-as-zero will now see X. That is a
behavior change to *every* existing circuit and is the reason C1 needs the
`FORMAT` bump and a documented migration note.

**Existing tests that must move.** `test/jls/ElementSimulationGoldenTest.java`
(17 tests), `SequentialGoldenTest.java` (11), `BatchSimulationGoldenTest.java`,
`VcdExportGoldenTest.java` (its
`vcdIsStructurallyWellFormedAndTwoStatePlusHiZ` pins C1 out by name),
`SimulationSemanticsRegressionTest.java` (its
`multiDriverConflictResolvesDeterministicallyAndWarnsOnce` pins the
first-driver-wins rule), `AllElementsRoundTripTest.java` (save/load fixed-point
and copy-completeness for every type — every new attribute is covered here
automatically, which is exactly why it exists), `ElementRegistryTest.java`,
`GenerativeRoundTripFuzzTest.java`, `ElementDrawSmokeTest.java`,
`RiscvCpuGoldenTest.java`, and on the HDL side `HdlPolicyTest.java` (14 tests,
three of which assert the rejections C3/C5 remove),
`VerilogExportGoldenTest.java` (28), `VhdlExportGoldenTest.java` (29),
`VhdlEmitterPolicyTest.java` (6), `yosys/CellValidatorTest.java`,
`imp/NetlistImporterTest.java`, `imp/ImportPipelineTest.java`.

**The `riscv/` tooling.** `riscv/jlsbuild.py` has "one factory per element,
exact save-format and put names" and `riscv/test_primitives.py` "validates
every element emitter against the real simulator" (`riscv/README.md:131-139`).
Every element change lands there too — and every one of C2/C3/C4 lets
`build_cpu.py` delete a workaround.

**The techmap library.** `test/resources/hdl/jls_map.v` shrinks as C4 lands
(the `$eq`/`$ne` rules become unnecessary) and the pipeline in
`ImportPipelineTest.java:109-112` sheds passes as C2 lands (`dffunmap` stops
being mandatory).

---

### What genuinely stays out, and why

- **#28 VHDL-AMS, #35 SystemC AMS, #37 Verilog-AMS, #42 Verilog-A.**
  Continuous-time analog. Supporting them means being a SPICE-class solver,
  which is a different tool class, not a bigger version of this one.
- **#36 Accellera SystemC Synthesizable Subset (as *import*).** Consuming it
  means building the front half of a high-level-synthesis tool — C++ parsing,
  scheduling, binding. Different tool class. (*Emitting* structural SystemC is
  in, and is one printer over C5's IR.)
- **#29 IEEE 1076.4 VITAL.** Authoring back-annotated cell timing models for a
  characterized standard-cell library, consumed by ASIC sign-off simulators.
  Requires a PDK and a library JLS neither has nor could have.
- **#79 VPR architecture XML, #80 FASM, #84 SVF/XSVF, #85 IEEE 1532, #86 vendor
  bitstreams.** Place-and-route configuration and device programming. JLS emits
  a netlist and constraints; nextpnr, openFPGALoader and vendor tools consume
  them. This is the delegation stance the project already holds and it is
  correct — not because these are hard, but because they belong to those tools.
- **#30 IEEE 1076.6 and #32 IEEE 1364.1 / IEC-IEEE 62142.** Withdrawn
  synthesis-subset standards with no successor conformance surface; the subset
  is now defined by whatever the synthesizer accepts, which for JLS means
  Yosys. There is nothing left to conform *to*.

Everything else in #25–#41 and #74–#86 is in. In particular, three items the
original survey declined should be reinstated: **#74 EDIF** (declined as
"conformance theater" — it is actually a structural diagnostic JLS fails),
**#77 BLIF** (same blocker, cheaper), and **#83 JESD3-C** (declined implicitly
as obsolete — it is a delightful, genuinely small teaching artifact and the
technology being obsolete is not one of the three grounds when the *format*
still has live tooling).

---

### Sources

**Repository (all paths absolute-from-root, verified at HEAD).**

- `src/jls/elem/Register.java` — pins `D`/`C`/`Q`/`notQ` at `:224-232` (and
  `:234-262` per orientation); three types at `:33-39`; saved attributes at
  `:268-380`; `initSim` at `:715-740`; `react` at `:747-820` (null-as-zero at
  `:757-765`).
- `src/jls/elem/Memory.java` — RAM/ROM enum `:35-39`; pins `:184-202`
  (including the #199 `clock` "appended last so the pre-#199 input indices are
  unchanged"); tri-state output `:200-202`; `react` `:1335-1481` (sync-write
  gate at `:1369-1383`).
- `src/jls/elem/ShiftRegister.java` — the misnamed combinational barrel
  shifter, `:20-34`; `react` `:614-682`.
- `src/jls/elem/Adder.java` — pins `:103-107`; ripple delay `bits × 30` at
  `:261`; `react` `:381-434`.
- `src/jls/elem/Extend.java:91-100` — 1-bit input only.
- `src/jls/elem/Mux.java:141-168` — `ceil(log2(n))` select, i.e. an exact
  `$bmux`.
- `src/jls/elem/Decoder.java:183-199`.
- `src/jls/elem/WireNet.java:443-529` — multi-driver resolution; the
  "first active driver in net order wins" warning at `:476-482`.
- `src/jls/elem/ElementRegistry.java:37-70` — 33 registered types; totality
  enforced by `jls.ElementRegistryTest` (`:16-21` Javadoc).
- `src/jls/sim/SimEvent.java:23-90` — sealed `Payload` with seven records;
  the exhaustive-switch rationale at `:16-21`.
- `src/jls/hdl/HdlExporter.java` — element policy Javadoc `:64-99`
  (reject bucket named at `:83-85`); `EXPORTED` `:418-424`; `SKIPPED` `:427-429`;
  `TOPOLOGY` `:432-433`; rejection `:187-193`; `Clock`-as-`InputPin` `:485-487`;
  don't-care lowered to 0 `:711-716`.
- `src/jls/hdl/HdlModel.java:197-762` — ten statement kinds, no instance kind.
- `src/jls/hdl/VhdlEmitter.java:466-472` — the `when others` arm "which also
  satisfies VHDL's full-coverage rule over std_logic's nine values"; `:101`
  the "'0'/'1'/'Z', never 'X'" header; `:350-375` the adder helper.
- `src/jls/hdl/VerilogEmitter.java:14`, `:71-72` — "0/1/z only … never x".
- `src/jls/hdl/yosys/CellValidator.java` — `SUPPORTED` `:54-65`;
  `ASYNC_RESET_MESSAGE` `:75-81`; `SET_RESET_MESSAGE` `:83-87`;
  `ARITHMETIC_MESSAGE` `:89-95`; `MEMORY_MESSAGE` `:97-103`; `buildTeachable`
  `:129-146`; `checkMemory` `:233-246`.
- `src/jls/hdl/imp/NetlistImporter.java` — scope Javadoc `:34-47`; multi-module
  refusal `:156-159`; hierarchy-cell refusal `:228-232`; realized cell switch
  `:234-259`; width-mismatch refusals `:313-320`, `:343-353`.
- `src/jls/hdl/layout/HeuristicLayeredLayouter.java`, `src/jls/hdl/board/`,
  `src/jls/hdl/scan/` — the shipped auxiliary paths.
- `test/resources/hdl/jls_map.v` — three techmap rules (`$xnor`, `$ne`, `$eq`)
  that exist solely to lower operators JLS has no element for.
- `test/jls/hdl/imp/ImportPipelineTest.java:105-126` — the exact Yosys pass
  pipeline.
- `test/jls/hdl/HdlPolicyTest.java:67-100` — the export rejections, pinned.
- `test/jls/` — `ElementSimulationGoldenTest.java` (17 `@Test`),
  `SequentialGoldenTest.java` (11), `VerilogExportGoldenTest.java` (28),
  `VhdlExportGoldenTest.java` (29), `HdlPolicyTest.java` (14),
  `AllElementsRoundTripTest.java:152-165`, `VcdExportGoldenTest.java`,
  `SimulationSemanticsRegressionTest.java`, `RiscvCpuGoldenTest.java`.
- `src/jls/edit/` — `RegisterDialog.java` (454 lines),
  `RegisterRenderer.java` (346), `MemoryDialog.java`,
  `MemoryContentsDialog.java`, `MemoryRenderer.java`, `MemTrace.java`,
  `Palette.java:156-165`.
- `riscv/README.md` — element inventory `:10-12`; PC reset-by-initial-value
  `:98-99`; ROM-controlled decode `:104-108`; Binder/Extend immediate generator
  `:109-112`; 31-register file with per-register hold Mux `:113-116`;
  subtractor-derived `slt`/`sltu` `:117-121`; clock-phase write gate `:124-126`;
  tooling `:131-139`.
- `docs/simulation-semantics.md` §2 (`:42-67`), §7 (`:264-289`), §8
  (`:291-408`), §9 (`:409-446`).
- `docs/file-format.md` §5 (`:200-235`), per-type table (`:297-325`), §9
  evolution policy (`:420-490`), the `Memory.sync` open question (`:471-479`).
- `docs/hdl-support-research.md` — round-trip verdict `:100`; §6 Stage 2 gap
  list `:290-330`; §7.2 `:520-545`; board export `:505-560`.
- `docs/standards-landscape.md` — HAVE table `:70-110`; Tier 2 `:161-177`;
  Tier 5 `:259-277`; §13.1 ranking `:722-742`; §13.3 `:772-778`.
- `ARCHITECTURE.md:340-368` — the single-simulation-strategy decision and the
  binding equivalence criterion for any change to the simulation model.
- `docs/grand-architecture.md:46-66` (the three trajectories), `:314-343`
  (cold plane / hot plane).

**External, unverified in this sweep** (taken from
`docs/hdl-support-research.md`, which marks its own verification status):
Yosys cell library and pass semantics (`simlib.v`, `simcells.v`, `dffunmap`,
`memory_map`, `write_json`); the IEEE 1164 resolution table; EDIF 2 0 0/4 0 0
structure; BLIF `.subckt`/`.latch` syntax; Accellera SystemRDL 2.0 field
semantics; IEEE 1685-2022 `busInterface` schema; JESD3-C fuse-map layout.
None of these were fetched during this sweep; every claim about them here is
structural and should be checked against the primary document before
implementation.
