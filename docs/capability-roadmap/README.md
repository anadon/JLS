# The capability roadmap

*Replaces §13 of `docs/standards-landscape.md`. Written from the six capability
sweeps (`sweep-01`…`sweep-06`) and the three keystone studies (`keystone-a-design`,
`keystone-b-migration`, `keystone-c-performance`) in this directory. Every claim
about JLS is anchored to a path in the tree at HEAD; claims about external
standards inherit the sweeps' verification status and are marked where the
sweeps marked them.*

---

## 1. The reframe

`docs/standards-landscape.md` §13 ranks 304 standards by **fit with JLS's current
scope**. This document ranks the same 304 by **capability gained**. That is not a
methodological refinement. It reverses the sign on the survey's most-used
argument.

The survey's §13.3 declines SDF (#89) because it is "the first step onto a
timing-engine slope." Under this frame the timing engine is not a slope — it is
the deliverable, and SDF is the diagnostic that found it. The survey declines
EDIF (#74) as "conformance to a dead format." EDIF is a structural test that JLS
fails for the same reason `HdlExporter` cannot export the `riscv/` CPU
(`src/jls/hdl/HdlExporter.java:418-424` omits `SubCircuit`, `Memory` and
`ShiftRegister`; the rejection is *pinned as intended behaviour* by
`test/jls/hdl/HdlPolicyTest.java`'s `memoryIsRejectedByName` and
`subCircuitIsRejectedCleanly`). The survey's §4 says of the whole verification
tier that JLS "has a verification story that conforms to *none* of these and
shouldn't" — a preservation verdict wearing scope clothing, since sweep 04 shows
the assertion-and-coverage program edits **three** of the 25 `react`
implementations under `src/jls/elem/`, not all 25.

**Three things the frame does to the earlier conclusions.**

**Items that move up.** These were declined, deferred, or filed OTHER for reasons
the capability frame does not accept:

| Entry | Survey verdict | Why it moves |
|---|---|---|
| **#89 SDF** | §13.3 "deliberately not recommended" | `docs/standards-adoption/11-costed-rejections.md:810` states its own re-open condition — "JLS acquires a technology-cell library *and* a name-stable synthesis path." Program **P6** is literally that condition. Its C4 objection ("requires a value domain that can express 'violated'", `:66`) is **P1**. |
| **#74 EDIF, #77 BLIF** | §13.3 "conformance to a dead format" | Both fail on one missing thing: `src/jls/hdl/HdlModel.java` has ten statement kinds and none of them instantiates a module. Passing EDIF means JLS has a hierarchical netlist IR — which it needs anyway. |
| **#67 EVCD** | §13.1 rank 4, "only if … strength/direction becomes a real complaint" | `docs/standards-adoption/07-waveform-formats.md:150-151` names its own revisit triggers verbatim: "JLS gains a drive-strength value domain **or** a bidirectional pin element." **P1-S3** and **P2** are those two triggers. The recorded "do NOT do EVCD, ever" (`:572`) is correctly scoped to *"under the current simulation semantics"* — the condition, not the format, was the objection. |
| **#22 I²C** | Tier 1 COULD | Not "a bus JLS could draw." I²C is *definitionally* open-drain wired-AND with pull-ups, and its multi-master arbitration **is** wired-AND. `grep -rni "open.drain\|wired.and\|pull.up" src/` returns only `src/jls/hdl/HdlNames.java:51`'s Verilog reserved-word list. The lab is not hard today; it is impossible. |
| **#49 SVA, #50 PSL, #53 UCIS, #63 SMT-LIB, #64 AIGER/BTOR2** | OTHER / ADJACENT | Collapsed into one "OTHER" because each *individually* implied a model change. Sweep 04 prices the whole tier at 3 of 25 `react` edits; the kernel seams it needs (`Simulator.afterEvent`, `:269`; `probeSample`, `:285`) already exist and are already used by the VCD exporter. |
| **#129 JTAG/BSDL** | costed at 15–25 md, then **deferred** pending "a DFT course asking" | Demand-gating a capability is the preservation filter renamed. What survives from that costing, verbatim and worth keeping, is the *honesty rule*: never say "conforms to IEEE 1149.1" without having read the paywalled text. That is a conformance-claim objection, not a cost objection. |
| **#137 STIL, #138 WGL** | OTHER, by shelf-adjacency to ATE hardware | Vector-interchange printers of the same class as the VCD writer JLS already ships (#66, HAVE). Mis-classified, not blocked. |
| **#72 SAIF** | Tier 4 OTHER | JLS already computes every datum in a SAIF file — `WireNet.propagate` (`src/jls/elem/WireNet.java:443`) sees every net transition, `Output.propagate` (`src/jls/elem/Output.java:139-145`) already does source-side change detection — and throws all of it away. No value-domain change, no timing change, no units. The cheapest real item in six sweeps. |
| **Tier 7 as a whole (#100–#111)** | "Every row is OTHER, and that is the correct and permanent answer" | Wrong for three rows and half-wrong for four more. *Computing* physical data is another tool class. *Reading and displaying* it is a 2D geometry viewer, which JLS already is. *Being a front end to somebody else's flow* is what `src/jls/hdl/board/PcfEmitter.java` already does one tier down. |
| **#5 RV32I, #26 IEEE 1164, #31 Verilog, #66 VCD, #75 Yosys** | **HAVE** | See below — these move *sideways*, not up. |

**Items that move down — or rather, whose HAVE marks turn out to be narrower than
the mark suggests.** These are not demotions of work done; they are corrections to
what the mark claims:

- **#26 IEEE 1164 (HAVE).** The survey cites `VhdlEmitter.java:470` as the
  evidence for the mark. That line is the `when others` arm whose own doc comment
  says it exists to "satisf[y] VHDL's full-coverage rule over std_logic's nine
  values" — written by a simulator with two states plus a null reference. There
  are three such sites (`:467-471`, `:575-580`, plus `(others => 'Z')` at `:345`)
  and a generated-header disclaimer at `:100-101` that says the quiet part out
  loud: *"JLS simulates two states plus HiZ: this design drives '0'/'1'/'Z',
  never 'X'."* The mark is for **emitting syntactically valid 1164**, not for
  modelling it. Split it: HAVE(syntax) / GAP(semantics).
- **#66 VCD (HAVE).** Conformant as a strict subset — and the subset is pinned as
  a contract by a test whose *name* asserts the invariant the capability program
  removes: `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`.
- **#31 Verilog / #76 netlist export (HAVE).** Structural export is real. The
  language's logic model — four states, an eight-level strength lattice,
  `wand`/`wor`/`tri0`/`tri1`, `pullup`/`pulldown`, the switch-level primitives —
  is unreachable.
- **#75 Yosys import (HAVE).** Real, and the realized mapper is four gate types
  plus `$mux` plus constants (`src/jls/hdl/imp/NetlistImporter.java:234-259`).
  `src/jls/hdl/yosys/CellValidator.java:75-103` holds four hand-written apology
  paragraphs — async reset, set/reset, wide arithmetic, clocked/multi-port memory
  — each explaining that a student's ordinary Verilog must be rewritten because
  JLS lacks an element. That file is the most honest gap catalogue in the tree.
- **#5 RV32I (HAVE).** Incomplete (`riscv/README.md`'s sub-word scope note:
  `lb/lh/lbu/lhu/sb/sh` are unimplemented because 32-bit `Memory` has no byte
  lanes) *and* un-exportable.
- **#4 IP-XACT ("structurally free", §13.1 rank 5).** Not free.
  `docs/standards-adoption/08-ipxact-export.md` lists its own blockers 2, 3 and 4
  as reuse identity, `inout`, and bus interfaces. None exists.
- **#43/#44 symbol conformance (§13.1 rank 1).** Still worth doing and still near
  the front. But `docs/standards-adoption/01-iec-ieee-symbols.md:44-46` carves
  the output qualifying symbols (open-collector, open-emitter, three-state) and
  the bidirectional signal-flow arrows *out* of the conformance claim, on the
  stated ground that "JLS has no elements that need them." It ships with a
  documented hole that only **P1**+**P2** close.

**The rejections that were pure artifacts of the old filter**: #89, #74, #77,
#67, #49, #50, #53, #63, #64, #72, #22, #83, #129's deferral, #134, #135, #137,
#138, and the Tier 7 blanket. Seventeen entries, none of which fails any of the
three legitimate grounds.

**One factual correction the sweeps turned up independently of the frame.**
Survey row **#304** names Efabless / ChipIgnite. Efabless ceased operations in
March 2025 and chipIgnite went with it; OpenLane 2 was renamed **LibreLane** in
early 2026 (`librelane/librelane`). The row names a defunct organization and must
be rewritten around Tiny Tapeout on SKY130 (via ChipFoundry / Swiss Chips) — see
§6.

---

## 2. The capability programs

Every model change the six sweeps found, clustered into six programs plus a short
list of standalone items. Each program is defined so that no change appears
twice: bidirectional ports appear in three sweeps, drive strength in four, and
register reset in four, and they are assigned here to exactly one owner. **The
totals in §7 are de-duplicated; the sweeps' own totals are not, and summing them
double-counts by roughly a third.**

---

### P1 — The value and resolution program

**What it changes.** Replace `@Nullable java.util.BitSet` as the currency of the
value channel with an immutable, width-carrying, plane-encoded value type in
`jls.core`; give drivers strength; give nets a real resolution function.

Concretely, per keystone A §5 and keystone C §5 (which measured the alternatives):

- `sealed interface LogicValue permits Word, Wide` — `record Word(int width, long
  a, long b, long u)` for width ≤ 64, sparse-plane `Wide` above. Five kernel
  states `0 1 X Z U`; `null` is banished from `Put.currentValue`
  (`src/jls/elem/Put.java:385`), `Input.setValue/getValue`
  (`src/jls/elem/Input.java:59,72`), `Output.propagate`
  (`src/jls/elem/Output.java:136`), `WireNet.value`
  (`src/jls/elem/WireNet.java:404-405`) and `SimEvent.TriStateOff`
  (`src/jls/sim/SimEvent.java:47`).
- **Strength lives on the driver, not in the signal**: two Verilog levels
  (`strength0`, `strength1`) on `Output`, defaulting to `(strong, strong)` =
  today. Open-drain is `(strong, highz)`; a pull-up is a constant-1 `Output` at
  `(highz, pull)`. A `driverKind` enum survives only as a *dialog preset over the
  pair*, not as a kernel concept.
- **A real fold replaces `WireNet.propagate:454-485`'s "first active driver in net
  order."** Driver and sink arrays cached at elaboration in `makeNet`/`recheck`
  (`:97-165`, `:272-302`); three fast paths (single strong driver; all-strong
  tri-state; general strength) so the common cases are *cheaper than today's
  scan*, which re-walks a `LinkedHashSet` with three virtual calls and an
  `instanceof` per end on every propagate.
- **Unknown-aware element semantics**, decided one element at a time behind a
  named, greppable `zeroFill()` lever: proper three-valued gate tables, X on an
  unknown mux selector, no latch on an unknown clock, X out of an unknown memory
  address.
- **Uninitialized start-up (`U`) and the end of the free reset.**
  `LogicElement.initInputs` (`:476-481`) stops zeroing every input at every
  depth; `Register.init` becomes optional; unwritten `Memory` words read X
  (`Memory.java:1455` today returns a fresh zero).
- **The IEEE 1164 nine-value view as a total projection** of `(kernel state,
  resolved strength)`, plus a separate small `Bits4` *specification* type carrying
  `-` for `TruthTable` cells (stored as `2` at `src/jls/elem/TruthTable.java:79`
  and then destroyed by `react`'s "don't care becomes false", `:1447-1449`) and
  for `-t` expectations.
- **The kernel hygiene that pays for it**, from keystone C: `SigSim`'s quadratic
  `String +=` vector build (`src/jls/elem/SigSim.java:64,67,71,74` — 0.57 s of a
  1.31 s run at 6004 cycles) and its post-everything-at-t=0 scheduling; interning
  the zero-field `SimEvent.PinChanged` record (1.92 M allocations per run); the
  per-`Simulator` sequence counter replacing the mutable static at
  `SimEvent.java:87`.

**Standards unlocked (28):** #22, #25, #26, #27, #30, #31, #32, #33, #43, #44,
#45, #47, #49, #50, #58, #64, #65/#259, #66, #67, #68, #72, #75, #76, #82, #83,
#89, #112 (digital half), #129.

**Pedagogical capabilities unlocked.**
- **Honest bus contention.** Today two enabled drivers produce a deterministic
  winner — *"the first active driver in net order"*, where net order is a
  breadth-first walk from the first wire end in file order
  (`docs/simulation-semantics.md` §9) — plus a one-shot `TellUser.warn`
  (`WireNet.java:472-483`). A student learns that bus conflicts have an answer,
  and that the answer depends on the order they drew the wires. After P1 the net
  goes X, the X propagates, and the datapath visibly goes red.
- **A floating input becomes visible.** 27 sites across 17 element classes
  rewrite "I am not being driven" to "I am zero." The most common first-year
  wiring mistake is currently invisible.
- **Don't-know vs don't-care** — two things students conflate for years, and
  which JLS currently *causes* them to conflate by collapsing both to 0.
  Karnaugh-map don't-cares become end-to-end real: mark `-`, watch the
  synthesizer exploit it, see the smaller gate count.
- **The open-drain I²C lab, complete**: wired-AND arbitration (the master that
  writes 1 and reads 0 has lost), ACK/NACK (the slave *pulls down*), clock
  stretching, and *why the bus needs a resistor*. Self-contained, industrial, and
  impossible today.
- **Reset discipline, at all.** An unreset design shows X on its state outputs
  forever, and the fix — draw a reset tree, hold it, release it synchronously —
  becomes a lab with visible pass/fail. Today `Register.initSim` supplies a reset
  the design does not have, pinned by
  `SequentialGoldenTest.registerInitialValueAppearsBeforeAnyClockEdge`.
- **X-pessimism, three-valued gate tables, and reading somebody else's waveform.**
  Students who move to GHDL/Icarus/Vivado meet `x` in hour one and have never
  seen it.

**Where JLS papers over it today — six structural workarounds, each *deleted* by
the change:** `TraceSample`'s marker-bit hack (`src/jls/sim/TraceSample.java:6-17`,
"the extra top bit set to mark a HiZ value"), reconstructed independently at five
sites; the `TriStateOff`/`NewValue` payload fork, written out at five `react`
bodies; the bus-conflict dialog standing in for a value; `BitSetUtils.toDisplay`
returning the literal string `"HiZ"` (`src/jls/BitSetUtils.java:237-245`);
`ImportSummary.coercedX` (`src/jls/hdl/imp/ImportSummary.java:28,59,97-100`), a
counter whose only job is to report information the importer destroyed after
Yosys parsed it faithfully; and `VhdlEmitter`'s three-site nine-value disclaimer.

**Size: 28–36 maintainer-weeks (6.5–8.5 maintainer-months).** Keystone B prices
the four-state core with a dual-mode discipline that keeps the tree green at
every commit at **17–22 weeks** (a big-bang branch with a long red period is
12–14; the extra ~6 weeks buys "no existing lab changes behaviour in the release
that ships it"). On top: strength + driver kinds + drawable pulls 6–9; `U` and
reset semantics 2–4; the 1164 projection and `Bits4` 2–3; kernel hygiene 1–2.

**Dependencies.** None on other programs. Lands *inside* `jls.core` — see §5.

---

### P2 — The element-vocabulary program

**What it changes.** The element set. `src/jls/elem/ElementRegistry.java` has 33
registered types and the gaps are exactly what `CellValidator`'s four apology
messages describe.

- **`Register` control pins.** Today exactly `D`, `C`, `Q`, `notQ`
  (`src/jls/elem/Register.java:224-232`, repeated per orientation). Add optional
  `CLR` (async, polarity-selectable), `PRE`, `EN` (clock enable), `LD`, and a
  reset value distinct from the initial value — each gated by a saved boolean, in
  the `Memory.sync` style issue #199 established.
- **`Memory` port model.** Today one address, one data, `WE`/`OE`/`CS`, one
  tri-state output, optionally one write clock
  (`src/jls/elem/Memory.java:184-202`). Replace with a port list: *N* read and
  *M* write ports, each independently clocked or combinational, with per-byte-lane
  write strobes, a masked-write mode, and a declared read-during-write policy.
- **An arithmetic family.** `Adder` is the entire arithmetic vocabulary. Add
  `Multiplier`, `Divider`, `Comparator`, `Subtractor`/`Negate`, `Counter`, and a
  *genuine* serial shift register — because `ShiftRegister` is a misnomer for a
  stateless barrel shifter (`src/jls/elem/ShiftRegister.java:20-34`).
- **Width conversion and bit-level connectivity.** `Extend` has a **1-bit** input
  replicated to N (`src/jls/elem/Extend.java:91-100`). Add an N→M
  `WidthAdapter` (sign/zero extend, truncate), and — the single
  highest-leverage importer task remaining — Splitter/Binder mesh synthesis, so
  Yosys's inherently bit-level connections stop being refused
  (`NetlistImporter.java:313-320`, `:343-353`).
- **A bidirectional pin.** `Pin` is `sealed … permits InputPin, OutputPin`
  (`src/jls/elem/Pin.java:20`); `HdlModel.Direction` is `{INPUT, OUTPUT}`
  (`src/jls/hdl/HdlModel.java:28-33`); `ScannedPort.Direction.INOUT`
  (`src/jls/hdl/scan/ScannedPort.java:18`) can *parse* a bidirectional port and
  has nowhere to put it. Add `BidirPin` and a third `SubCircuit` map beside
  `inmap`/`outmap` (`src/jls/elem/SubCircuit.java:33,35`).
- **A priority encoder** (JLS has `Decoder` and no inverse) and the CSR/trap
  element set — masked-write addressable state — that RISC-V privileged mode
  needs.

**Standards unlocked (18):** #4, #5, #6, #10, #11, #17, #31, #33, #38, #65/#259,
#75 (the whole `CellValidator` reject list — `$adff`/`$adffe`/`$aldff`/`$adlatch`,
`$sr`/`$dffsr`/`$dffsre`, `$mul`/`$div`/`$mod`/`$pow`, `$mem_v2` with write
ports), #76, #77, #109, #110, #129, #134, #304.

**Pedagogical capabilities unlocked.**
- **A reset line becomes a wire.** Today the `riscv/` PC "resets" by having
  initial value 0 (`riscv/README.md:98-99`) — a save-file attribute. Students
  cannot draw a reset line, hold the machine in reset, or single-step out of it.
- **The register file becomes a register file.** `riscv/build_cpu.py:239-252`
  gives each of 31 registers an `AndGate` and a hold/load `Mux` on its `D` input.
  That is **62 of the CPU's 228 elements — 27% of the entire processor —
  existing solely to synthesise a write-enable the `Register` element does not
  have.** With a 2-read-1-write clocked memory a student can draw the textbook
  register file *and* still draw the gate-level one to compare; today only one
  lesson is available.
- **The array multiplier vs. the `*` operator.** `CellValidator.ARITHMETIC_MESSAGE`
  (`:89-95`) currently instructs students to "build the operation structurally
  (for example, shift-and-add multiplication)" as a **workaround**. It is one of
  the best lessons in the course, and JLS offers only the first half of it.
- **`always_ff @(posedge clk or negedge rst_n)` imports** instead of being
  refused with a rewrite instruction — the single most common sequential idiom in
  every textbook.
- **SR latch, D latch with clear, JK with preset/clear** — the standard
  first-year sequential parade — become first-class rather than gate
  constructions. Read-during-write and write-first-vs-read-first become
  teachable. Endianness becomes concrete via byte lanes.
- **A shared bus across a module boundary.** Today hierarchy stops helping the
  moment a bus is involved: the `riscv/` CPU fixture has **zero `SubCircuit`
  elements** in 228.

**Size: 22–32 maintainer-weeks (5–7.5 maintainer-months).** Register pins 3–5;
memory ports 4–7 (`Memory.java` alone holds 51 of the tree's 417 `BitSet`
references and is 1547 lines — schedule it alone); arithmetic family 4–6 (~1.5
for the first element including the plumbing pattern, ~0.75 each after); width
adapter 1–2 plus importer mesh synthesis 2–3; bidirectional pin 4–6 (the element
is small; the cost is the subcircuit boundary, `Circuit.finishLoad`, the
`-pins`/board path, and the two sealed-hierarchy edits that fire
`SealedHierarchyTest`); priority encoder and CSR set 4–6.

**Dependencies.** The additive-attribute half (register pins, memory ports,
arithmetic) is **independent of P1** and can run fully in parallel. The
bidirectional pin needs P1's resolution to read back meaningfully, and the CSR/
trap set needs P2's own register work first.

---

### P3 — The interchange and hierarchy program

**What it changes.** JLS's netlist IR and its notion of a reusable component.

- **An instance statement.** `HdlModel` has ten statement kinds and none of them
  instantiates a module. Add `InstanceStatement` (module name, instance name,
  port bindings) and a multi-module `HdlModel` — one module per distinct
  subcircuit type, emitted once, instantiated N times. Same for the import
  direction: `NetlistImporter` refuses any netlist with more than one module
  (`:156-159`) and tells the user to run Yosys `flatten`.
- **Total export coverage.** `SubCircuit`, `Memory` and `ShiftRegister` leave the
  reject bucket. `Memory` becomes an inferrable RAM/ROM (`reg [w-1:0] m [0:d-1]`
  plus `$readmemh`/VHDL initializers, in both its level-sensitive and #199
  synchronous modes); `ShiftRegister` is a one-line barrel-shift expression per
  mode and is rejected only because nobody added it to `EXPORTED` after #122
  shipped.
- **Interface bundles and reuse identity.** A first-class `Interface`
  declaration — an ordered `(name, width, direction-relative-to-master)` list,
  instantiated on a subcircuit boundary as a *single pin*, drawn as one thick
  wire, connected in one gesture, with a master/slave role flip. And a component
  table in the save format, because `SubCircuit.save` writes the entire nested
  circuit **inline** (`src/jls/elem/SubCircuit.java:282-288`) and `Circuit.load`
  constructs a fresh `Circuit` per instance (`src/jls/Circuit.java:1021`) — two
  instances of "the same" block are two independent copies with no way to tell
  they are the same. Bundles elaborate to N ordinary `WireNet`s at load, so the
  hot loop pays nothing.
- **Port metadata**: per-port roles (clock / reset / data / address / valid /
  ready) and bus-interface grouping. Today `HdlExporter.java:485-487` treats
  `Clock` and `InputPin` identically; JLS cannot say a wire *is* a clock.
- **An addressable register-block element** with per-field width, offset, access
  policy (`rw`/`ro`/`w1c`/`rclr`) and reset value — the SystemRDL substrate.
- **Round-trippability, narrowly and defensibly.** `docs/hdl-support-research.md:100`
  says "do not promise this," and is right *for arbitrary HDL*. It is not right
  for the case JLS uniquely controls: JLS owns both the emitter and the importer,
  which no other surveyed tool does. A declared closed subset, provenance
  attributes (`(* jls_id … , jls_type … *)`) surviving a `keep`-preserving Yosys
  pipeline, behavioural elements emitted as black boxes, geometry as a sidecar
  keyed by stable id, and a CI property — `export → yosys → import → save` equal
  to the original modulo element ids. The claim becomes **"JLS-exported HDL
  re-imports to the circuit it came from, and CI proves it."** No tool in the
  survey makes that claim.
- The printers that then fall out: EDIF, BLIF, structural SystemC, SPICE
  `.subckt`, IP-XACT, SystemRDL.

**Standards unlocked (21):** #4, #10, #11, #12, #13, #14, #25, #31, #34, #38,
#39, #40, #74, #75, #76, #77, #109, #110, #111, #134, #304.

**Pedagogical capabilities unlocked.**
- **"Export your CPU."** Currently impossible. Worse: JLS teaches hierarchical
  design as a first-class idea and then silently refuses to carry that hierarchy
  across the tool's own most important boundary. A student who structures a
  design *well* is punished by the exporter; one who draws a 1000-element flat
  mess is rewarded. **That is a teaching inversion sitting in the tree right
  now**, pinned by two passing tests.
- **A system on a bus, in one lab period.** A CPU, a ROM, a RAM, a UART and a
  timer, each a reusable component, each attached to one Wishbone bus with one
  wire, an address decoder between — then a program that talks to the UART
  through a memory-mapped register. That is the second half of a
  computer-organisation course. The same drawing today is ~60 hand-drawn wire
  segments per peripheral and is unreadable on a screen.
- **Memory-mapped I/O as something you draw.** "Write 1 to bit 3 of 0x40 to clear
  the interrupt", wired to the `riscv/` CPU and read back in software. A whole
  lab that does not exist.
- **"Your module boundary is also a synthesis boundary"** becomes a demonstrable
  fact rather than an assertion.

**Size: 26–38 maintainer-weeks (6–9 maintainer-months).** Export coverage +
hierarchy 5–8 (hierarchy 3–4 with a name policy, recursion guard and 29+29
re-baselined goldens; `Memory` 2–3; `ShiftRegister` 0.5); interface bundles 3–4
plus reuse identity 3–6 (the component table, a FORMAT bump, one-way migration of
inline subcircuits on load, and an honest answer to "what happens when two files
disagree about version 1 of a component"); port metadata + IP-XACT writer 3–4;
register-block element + SystemRDL 4–6; round-trip pipeline 6–10; EDIF/BLIF/SPICE
printers ~3.

**Dependencies.** The export-coverage slice depends on **nothing** and is the
earliest large user-visible payload in the whole roadmap. Reuse identity and
bundles are independent of P1 and P2. `Memory` export gains fidelity from P2's
port model and honesty from P1's per-bit Z, but does not wait on either.

---

### P4 — The timing and analysis program

**What it changes.** JLS's timing model is **one scalar `int propDelay` per
element**, applied as pure transport delay uniformly across every input→output
arc, both edges, one value (`src/jls/elem/Gate.java:708` and the identical line
in nine other `react` bodies). Wires are ideal. **Nothing anywhere records when a
signal last changed** — `src/jls/elem/Input.java` is a bare value cell.

- **Structured delay.** A `DelayModel` keyed by arc = (input pin, output pin),
  each arc carrying rise/fall (and turn-on/turn-off for `TriState`, which already
  distinguishes them via `TriStateOff` vs `NewValue`), each delay a min:typ:max
  triple with a run-wide corner selector, optionally `COND`-predicated. The
  degenerate case — one arc, rise = fall, min = typ = max — **is exactly today's
  scalar**, so every existing `.jls` simulates byte-identically.
- **Physical time units.** A per-circuit timescale. `long now` stays an integer
  count of precision units; the timescale is the interpretation.
- **Inertial delay and glitch detection.** A per-element delay type
  (transport | inertial | inertial-with-reject-width), needing
  `Simulator.cancel`, which does not exist today (`post` at
  `Simulator.java:165-170` can suppress an exact duplicate but cannot withdraw).
  Separately and independently: a **glitch detector** that records a transient
  whenever an element schedules a new output while a different value is in flight
  — a condition `Gate.java:706` already computes. Plus sub-pixel transient
  rendering, because `Trace.paintComponent`'s `int rlen = (int)Math.round(len)`
  currently rounds a short pulse to zero pixels and it vanishes.
- **Timing checks.** `tSetup`, `tHold`, `tRecovery`, `tRemoval`,
  `tMinPulseWidth` on sequential elements; a `long lastChange` on `Input`,
  written from the single call site `WireNet.propagate:499`; a check in
  `Register.react`'s `PosFF` arm at the moment it detects the edge. **A violation
  can be *reported* without being *modelled*, and reporting is 90% of the
  teaching value** — so this does not have to wait for P1, though driving X on
  violation is the correct eventual answer and does.
- **A static timing analyser** in a new headless `jls.timing`: a timing DAG over
  `Circuit.getElementsInStableOrder()` and the `WireNet` structure, cut at
  sequential boundaries; forward-longest-path arrival, backward required, slack;
  worst negative slack, fmax, ranked critical paths — and combinational-loop
  detection, which falls out free and which JLS today does not have at all.
- **A constraint object model** (SDC/XDC/QSF/LPF ingestion) feeding it.
- **Per-sink interconnect delay**, opt-in, computed from fanout — which the net
  already knows.
- **Switching activity and power**: per-net toggle counters behind the same gate
  the trace machinery already uses, a SAIF writer, and energy = toggles ×
  energy-per-transition.

**Standards unlocked (11):** #31 (`specify`/`$setup`/`$hold`), #66 (made
honest), #72, #76 (delay-annotated emission — which makes a JLS↔Icarus
*behavioural* differential test possible for the first time; today
`test/jls/hdl/IverilogCompileTest.java:32` only compiles), #82 (timing half),
#87, #89 (all four claims), #92, #93, #94, #99.

**Pedagogical capabilities unlocked.**
- **The critical path, highlighted on the schematic.** The student's ripple-carry
  adder lights up red along the carry chain; they draw a carry-lookahead adder and
  watch the red path shorten. *That is the comparison the whole adder unit of a
  first-year course exists to teach*, and today JLS can only assert it verbally.
- **fmax in MHz, and slack with a number attached**, before the student ever
  meets an FPGA toolchain that reports them in an unfamiliar dialect. Pipelining
  taught by measurement: insert a register, watch the path halve and latency rise.
- **Static and dynamic hazards become visible, named and countable.** Hazard
  analysis is core Karnaugh-map content whose entire motivation is a glitch the
  student cannot currently see. Add the consensus term, watch the glitch count go
  to zero — instead of adding a term that does nothing to the truth table and
  taking the instructor's word for why.
- **Setup and hold become phenomena rather than definitions.** Today
  `Register.react` samples D at the edge with zero regard for how recently D
  moved: a student can violate every setup constraint in the universe and JLS
  pronounces the design correct. *That teaches that flip-flops are magic.* The
  clock-skew demo — insert a delay gate in one clock leg, watch a hold violation
  appear — becomes a five-minute exercise.
- **Fanout has consequences.** Today a net driving one input and a net driving
  twenty are timing-identical. That is not a simplification, it is a falsehood,
  and it is the reason buffer trees exist.
- **Power as a design axis at all** — currently absent. And glitches cost energy,
  which is the strongest possible motivation for hazard elimination.

**Where JLS papers over it today.** `Adder.resetPropDelay()` sets
`propDelay = bits * defaultPropDelay` (`src/jls/elem/Adder.java:261`) — a
hard-coded structural timing model of an element whose structure is not
simulated: it asserts "I am a ripple-carry chain with this depth-dependent delay"
while behaving as one lumped black box, so the very phenomenon the number models
is unobservable. `Timed.usesAccessTime()` exists **only to change a dialog
label**, because a memory access time and a gate propagation delay are the same
`int`. `Memory` saves its scalar under a different attribute name (`int time`)
than every other element (`int delay`) — two names for one concept never
modelled. `docs/simulation-semantics.md` §7's delay table is a **library
datasheet published as a normative simulator document**. And `docs/batch-interface.md`
§4.2 admits that JLS already writes `$timescale 1 ns $end` into every VCD —
byte-pinned by `VcdExportGoldenTest.clockedRegisterVcdMatchesGoldenByteForByte` —
while "JLS time units are abstract." GTKWave reads that file and confidently
labels the axis in nanoseconds. Same shape of gap as `VhdlEmitter`'s nine values.

**Size: 26–38 maintainer-weeks (6–9 maintainer-months).** Structured delay 6–9;
time units 2–3; inertial + glitch 3–4 (**the detector half alone is ~1.5 weeks
and is the highest teaching-value-per-week item in six sweeps** — no file-format
change, no value domain, no arcs); timing checks 4–6; STA 5–8; constraints 2–3;
interconnect 3–5 (the risky one — it changes same-time race resolution, so it
must be opt-in with default zero net delay, and `RiscvCpuGoldenTest` is the
canary); activity + SAIF 2–3.

**Dependencies.** Structured delay, units, glitch detection, STA and activity
depend on **nothing** and run fully in parallel with P1. Only the
violation-drives-X half of timing checks needs P1. Liberty as a *delay source*
needs P6's cell layer; an STA over today's uniform scalars is still useful and
still finds the carry chain.

---

### P5 — The verification program

**What it changes.** There is no object in the tree representing "a property that
must hold" and none representing "something that was exercised." Both absences
are currently paid for in Python, in one-shot `TellUser.warn` dialogs, and in
hard-coded expected strings.

- **Timestamp closure.** `Simulator.runEventLoop` (`:215`) advances `now` per
  event with no notion of "this timestamp is finished," and because
  `WireNet.propagate` overwrites inputs eagerly a signal can hold several values
  *within* one timestamp. Peek the queue head (O(1) on a `PriorityQueue`); when
  its time exceeds `now`, fire an `afterTimestamp(long)` hook. That hook is the
  **sampling region**, and it is observation-only — it posts no events and
  mutates no state, which is a property to be *tested*, by every golden staying
  byte-identical.
- **A drawable `Assert` element** in the existing `Group.TEST` palette group,
  with `check`/`enable`/`clock` ports and `name`/`severity`/`mode`/`message`
  attributes. The archetype already exists: `Stop` (`src/jls/elem/Stop.java:157`)
  and `Pause` (`:177`) are exactly this shape — a 1-bit-in, zero-out element whose
  whole job is a side effect when a condition holds. An `Assert` is `Stop` with a
  name, a severity, and a report instead of a halt.
- **A minimal temporal layer**: `stable`, `rose`/`fell`, `a |-> b`, `a |=> b`,
  `a ##N b`, `a |-> ##[1:N] b`, `always`/`never`, bounded `$past` — a per-property
  bounded NFA plus a ring buffer, no solver, no unbounded state. Two authoring
  forms: drawn (the Boolean part from ordinary gates, the temporal part a dialog
  choice — for first-years) and written (a one-line grammar attribute, specified
  and frozen like the `-t` grammar — for instructors, so properties are diffable
  and distributable in an assignment repo).
- **A coverage model**, four measures each grounded in an existing structure: net
  toggle coverage per bit, `StateMachine` **transition** coverage (strictly
  dominates state coverage at the same cost — `State.getNextState()` already
  returns the match), `TruthTable` **row** coverage (`react` already computes
  `matchingRow`; this is the schematic analogue of branch coverage, and it
  surfaces the silent hole at `:1430-1434` where an unmatched row holds its
  outputs and tells nobody), and select/value-bin coverage. Plus HiZ coverage and
  conflict counts, turning today's one-shot warnings into numbers.
- **Reactive and constrained-random stimulus.** Every stimulus today is a
  schedule fixed before time 0: `SigSim.initSim` parses the whole `-t` file and
  posts every event up front. Add a `Stimulus` SPI that may post during the run,
  a seeded per-pin domain declaration, coverage-driven feedback, and a multi-run
  batch mode (`riscv/fuzz_diff.py` currently pays a JVM start per program).
  **`-t` stays byte-compatible forever** — the generator gets a new flag and a
  new file.
- **In-tool differential/equivalence checking**, and **formula export** — SMT-LIB,
  AIGER, BTOR2 as printers over the model `HdlExporter.buildModel` already
  constructs, with ABC/`btormc`/SymbiYosys doing the solving, matching the
  recorded delegation stance. Plus the piece worth naming separately: **the
  counterexample loop** — render a solver's falsifying assignment back as a `-t`
  file, an interface that already exists and is already a frozen contract, so the
  student replays the counterexample in the GUI and watches the property fail.
- **The report channel and exit-status contract.** `docs/batch-interface.md` §1
  defines exactly three exit statuses and no way to say "the run completed and a
  property failed." A new flag gates every new observable; xUnit XML as the
  primary artifact because that is what CI and every LMS autograder already
  ingests. **This must be designed first**, because it is a change to a promise.
- **Electrical rule checking** (sweep 06 G) rides here: undriven inputs,
  multiply-driven non-tri-state nets, combinational loops, width mismatches,
  fanout beyond a cell's limit, clock nets on data pins, registers with no reset
  in an ASIC-targeted design.

**Standards unlocked (12):** #48 (the *shape*, not the library), #49, #50, #52
(capability), #53, #57 (artifact shape), #58 (capability, natively), #63, #64,
#65 (materially cheaper), #84, #89 (C4 partially, on new grounds — a violation
gets somewhere to go that is *not* an X value).

**Pedagogical capabilities unlocked.**
- **Failures surface where and when the intent was violated**, marked on the
  drawing the student is looking at — instead of as a wrong number in a `Display`,
  200 time units and three subcircuits downstream of the cause. "These two
  tri-state drivers must never both be enabled." "This FSM must never be in `RUN`
  while `reset` is high." "This handshake must never see `ack` without `req`."
- **A specification is a thing you write down, separate from the
  implementation** — the single most transferable idea in the field, taught
  early and correctly.
- **"Passing tests is not the same as being tested."** A student can pass a lab
  today with a circuit whose entire carry path is dead, and neither they nor the
  instructor can tell. Coverage makes it gradeable: "exercise every row of your
  truth table"; "drive the FSM through every *transition*, not just every state"
  — a distinction students consistently get wrong and which is nearly impossible
  to teach without a tool that measures both.
- **Exhaustive grading.** "Build a 4-bit adder out of gates" becomes gradeable as
  *"is it equivalent to the reference for all 2⁹ inputs"* rather than *"does it
  match on the 20 vectors I wrote."* A categorical change in what a grade means.
- **Bounded proofs with a drawn counterexample** — strictly more instructive than
  a pass/fail diff, because the failure is *constructed for* the student rather
  than stumbled into.

**Where JLS papers over it today.** `examples/autograde/autograde.py` hard-codes
`EXPECTED_STDOUT_LINES` — the grading criterion is *literal bytes of a report
format*, i.e. an assertion expressed outside the circuit against a text encoding.
`riscv/verify.py:compare` builds a `problems` list from register and memory
diffs: a scoreboard, in Python, because the model has no place for one.
`docs/standards-adoption/05-riscv-compliance.md` step 3 builds a halt condition
from a magic-address comparator feeding `Stop` — an assertion assembled out of
gates. `riscv/verify.py:gen_clock` generates *exactly as many rising edges as the
reference emulator took*, because there is no way to say "run until `done`"; the
same document measures that scaffolding at 38.2 s against 4.1 s free-running.
And `TruthTable.react` on an unmatched row silently holds its outputs and warns
nobody at all.

**Size: 19–28 maintainer-weeks (4.5–6.5 maintainer-months).** Report channel 1
(small, but it is a promise); timestamp closure 2–3 (mostly normative-document
work and proving non-disturbance); `Assert` element 2–3 (the GUI half is the
larger share); temporal layer 3–4; coverage 2–3 plus 1 for UCIS; stimulus 2–3
plus 1 for multi-run; equivalence checking 1–2; formula export 3–4 plus 2–3 for
BTOR2 and bounded sequential unrolling; ERC 3–5.

**The useful floor is 5–7 weeks**: report channel + timestamp closure + `Assert`
+ toggle/row/transition coverage. That alone converts autograding from
output-matching to property-and-coverage grading.

**Dependencies. None.** This program needs **nothing** from P1 — X is not
required for the useful subset — and it edits **three** of the 25 `react`
implementations (`TruthTable.react`, `State.getNextState`, `WireNet.propagate`),
plus its own new elements. It is the one program that can run start-to-finish in
parallel with everything else.

---

### P6 — The silicon on-ramp

**What it changes.** JLS's primitives are 33 compiled-in Java classes. A cell
cannot be data.

- **Cells as data.** Extend `ElementType` — already a descriptor + factory that
  `docs/grand-architecture.md:281-292` calls "the seed of the plugin mechanism" —
  so a cell can be a record (name, pin list, logic function, timing figure, area,
  capacitance) loaded from a library file. A `CellInstance` element renders as a
  box with named pins and `react`s by evaluating its function. `NetlistImporter`
  today rejects any cell type not starting with `$`
  (`src/jls/hdl/imp/NetlistImporter.java:227-232`) — i.e. every real standard
  cell; with a loaded library that branch becomes a lookup.
- **A Liberty subset reader** — the inventory, not the timing engine: cell names,
  pin directions, `function`, `area`, `capacitance`, one delay figure. The
  teaching subset of `sky130_fd_sc_hd` is ~40 cells of ~440.
- **A LEF reader** for the abstract view (`MACRO` size, `PIN`/`PORT`/`LAYER`
  rectangles, `OBS` blockages).
- **A shuttle target.** Generalize `jls.hdl.board` from `(name, fpga, format, pin
  map)` (`src/jls/hdl/board/Board.java:26-27`) to a target descriptor that can
  carry a **wrapper template**. A `tinytapeout` target emits, from the same
  `HdlExporter.buildModel` port walk `PcfEmitter` already uses: a `tt_um_<name>`
  wrapper with the fixed signature, an `info.yaml`, and a LibreLane config
  stanza. `PinBindings` and the all-or-nothing binding discipline (#213 P3)
  transfer verbatim; `Board.Format`'s own javadoc already anticipates new
  constants.
- **A read-only layout view** behind the existing renderer seam: a GDSII stream
  reader, a layer-style model, and — critically — a *separate* coordinate space,
  because `Geometry.CIRCUITSIZE = 1000` on a 12-px grid and `Viewport` clamps
  zoom to [0.25, 4.0] are schematic constants that must not be widened. Then DEF,
  CIF, OASIS as increments.

**Standards unlocked (12):** #87, #89 (its C2 objection — "SDF keys on `CELLTYPE`
+ `INSTANCE`; a JLS drawing has no cells" — dissolves, because a drawn cell *is*
the instance), #100, #101, #102, #103, #104, #108, #109, #110, #111, #304.

**Pedagogical capabilities unlocked.**
- **Fanout, drive strength, area and load become measurable.** Today every gate
  has a fixed integer delay from `docs/simulation-semantics.md` §7 and **no area,
  no input capacitance, no drive strength, no fanout limit, no load dependence**.
  A student can wire one NOT gate to two hundred inputs and JLS simulates it
  happily at delay 5. New labs: "map your adder to sky130 and count the cells";
  "your critical path is nine cells, here is each one's delay and load"; "add a
  buffer tree and watch the delay drop"; "`and2_1` versus `nand2_1 + inv_1` —
  which is smaller?"
- **Datasheet literacy.** "Open the 74HC00 datasheet, find t_PD, put it in the
  library, re-run." A first-year student meeting a real datasheet with a real
  consequence, instead of characterisation data compiled into `Gate.Kind`.
- **Cross-probing — the payload that makes the layout view worth building.**
  With cells as data, JLS knows which cell instance corresponds to which drawn
  element, and **no external viewer can ever know that**. Click a NAND you drew;
  its two `sky130_fd_sc_hd__nand2_1` instances light up in the layout. Click a
  polygon; the schematic element highlights. *E without D is a worse KLayout. E
  with D is a thing that does not exist.*
- **"The class taped out a chip"** — and, more soberly, the *budget*: 8 dedicated
  inputs, 8 outputs, 8 bidirectionals, one clock, one reset, one tile. Every one
  is a constraint students must design against, and constraints are where digital
  design is actually taught. "Fit your design in a tile" becomes a term project
  with a real deadline and a real artifact.

**Where JLS papers over it today.** The §7 delay table *is a cell library with
the technology removed*: AND=10, NAND=5, NOT=5 encodes a real fact (NAND is
cheaper than AND because AND is NAND+INV) in a form that cannot be questioned,
parameterised, or checked against anything. And `test/resources/hdl/counter.v:19`
— `reg [3:0] count = 4'h0;` — is a construct whose meaning **depends on the
target**, emitted unconditionally with no warning: FPGA synthesis honours it,
ASIC synthesis silently discards it. That is the single most dangerous paper-over
in the tree with respect to silicon, and JLS's own goldens bless it.

**Size: 20–32 maintainer-weeks (4.5–7.5 maintainer-months).** Cell layer 10–16
(data-defined cells 4–6; Liberty subset 2–3; LEF 2–3; importer acceptance 2;
library packaging and the "which cells ship / which are fetched" decision 1–2);
shuttle target 2–4; layout view 8–12 (reader 3–4; layer model 1–2; second canvas
and index 3–4; cross-probe wiring 1–2). OASIS +3–4, DEF +2, CIF +0.5 — all
optional and low priority.

**Dependencies. The most gated program in the roadmap.** The shuttle target is
"2–4 weeks *given* A, B and C — effectively infinite without them." It needs P3's
export coverage (or the only tapeoutable designs are gate toys), P2's reset model
(or the wrapper wraps a chip that boots into garbage), and P2's bidirectional pin
(for `uio_oe`). The layout view is worthless without the cell layer.

---

### Standalone items (not programs — each unlocks one thing, or unlocks nothing but is small and good)

| Item | Unlocks | Size | Gate |
|---|---|---|---|
| **IEEE 91/91a + IEC 60617-12 symbol render mode** | #43, #44 | 3–4 wk | GUI-only now; the excluded output qualifiers and bidirectional arrows complete only after P1-S3 + P2 |
| **Boundary scan: `TapController` + `BoundaryScanCell` + BSDL emitter** | #129, #134, #135, #84 | 3–4 wk | after P1 (strength/HIGHZ) + P2 (bidir cells). Keep the conformance-claim honesty rule verbatim |
| **STIL / WGL vector printers** | #137, #138 | 3–5 wk | none — printers over `BatchSimulator.getTraceSamples` |
| **JESD3-C fuse map (burn a GAL22V10)** | #83 | 2–3 wk | a small two-level minimizer or delegation to `galette`; P1's driver kinds make the macrocell honest |
| **XDC / QSF / LPF pin-half emitters** | #82 | ~1 wk each | already roadmapped as #213 follow-ups; not blocked by anything |
| **Interactive-engine batching** | nothing | 1–2 wk | `InteractiveSimulator.afterEvent:879-896` runs per-event with a `BitSet` clone per probe, violating `grand-architecture.md` §6's "batched, rate-limited, never per-signal" rule. Independent hygiene |
| **Full cycle-based simulation strategy** | nothing directly; unblocks OS-scale `riscv/` work | 10–16 wk | after P1 + the zero-delay levelization; needs a *declared alternative strategy* section in `docs/simulation-semantics.md`, because it cannot reproduce transport delay |

---

## 3. The keystone and the spine

### The keystone

**A signal has a width, an immutable identity, and plane-encoded multi-value
state; a driver has strength; a net has a resolution function.**

Not "replace `BitSet`." That framing is what makes the change sound like a tax.
Three properties arrive together, and only one of them is about multi-value
logic — the multi-value alphabet is nearly free once the other two land. It is
the *reason* to do the work, not the *cost* of it.

**The evidence, from three directions.**

**Reach (keystone A).** Sixteen survey entries directly blocked, twenty-four
counting dependents — more than any other single change in six sweeps. Every
other sweep routes its top item back through it: sweep 03 calls it "the single
biggest teaching change"; sweep 05 says of I²C "this is the whole standard";
sweep 06 finds it gating LEF's inout pins, IBIS's digital shadow, and JTAG
boundary cells; sweep 02 finds SDF's `TIMINGCHECK` and EVCD both terminating on
it. And the frame's own named example is worse than stated: `VhdlEmitter` asserts
a value model the simulator does not have in **three** separate places plus a
generated-header disclaimer.

**Tractability (keystone B).** The migration is finite and mechanically
enumerable: **33 value-computing methods** (25 `react` + 8 `computeOutput`), of
which **22 are mechanical**, 8 need a documented decision, and only **4** are
genuinely ambiguous; **27 HiZ-as-zero coercion sites** across 17 element classes;
5 marker reconstructions; 5 payload forks; 4 duplicated formatters; 4
file-format value carriers; 3 `batch-interface.md` sections; **171 exposed
`@Test` methods across 23 files**, of which ~120 survive unchanged in two-state
mode and **21 assert the old behaviour by name**. Not one number is unbounded.
And the change is **net line-count-negative in at least six places** — the
whole-signal HiZ marker is reconstructed four times, the `TriStateOff` fork is
written out five times, and `Splitter`/`Binder` each carry an all-or-nothing
special case that per-bit Z deletes outright. A per-bit value type is not only
more expressive than what is there; it is *smaller*.

The organising trick that makes it affordable: **do not delete the 27 coercions
— rename them.** `if (v == null) v = new BitSet()` becomes `v.zeroFill()`, a
named, greppable, javadoc'd method with identical behaviour. The tree stays
green, every golden stays byte-identical, and the remaining work is a finite
worklist whose progress is measurable as `grep -c zeroFill` falling. That converts
a 24-file flag-day into a sequence of reviewable commits, and it is the single
most important mechanic in the design.

**Cost (keystone C) — and this is the finding that inverts the usual objection.**
Measured on the tree at HEAD with a 6004-cycle RV32I workload:

- The event loop spends **37.6% on `BitSet` value-container overhead, 47.7% on
  event-queue bookkeeping, and 4.9% on the actual digital logic.** JLS spends
  roughly twenty times as long deciding where an event goes in a priority queue,
  and copying `BitSet`s defensively, as it spends computing what a gate outputs.
- The recommended representation is **10.85 ns/op against today's 21.11 ns at
  32 bits**, four states and a dormant `U` plane included. The saving is not
  cleverness: it is deleting the defensive clones that mutability required. A net
  with *n* sinks goes from *n+1* allocations per change to **zero**.
- `BitSetUtils.SumCarry` + `ToLong` for one 32-bit add costs **294.49 ns**
  against **0.60 ns** for a `long` add and a mask — ~490×, a direct consequence
  of a value that carries neither a width nor a word view. `BitSet.size()` is the
  backing-array capacity, so a 32-bit ripple add executes 64 iterations of a
  four-branch chain. `Adder.react` fired 108,025 times in the benchmark run;
  `Mux.react`, which calls `BitSetUtils.ToInt` for its selector, fired 875,291.
- **The levelized compiled pass needs the same representation.** 522 evaluation
  slots cost **2.26 µs with plane arrays and 11.49 µs with `BitSet[]`** — 5.1×.
  The value-domain change is not a tax the compiled pass must absorb; it is where
  **80% of the compiled pass's speedup comes from.** Building them sequentially
  means writing the four-state truth tables twice, against two data layouts, six
  months apart, and discovering the disagreement through the #202 differential
  oracle.

**Projected effect of the migration: the event loop gets 15–25% *faster*.** That
is a testable prediction, and it should be the acceptance criterion of the stage
that lands the type — stated as an expected improvement, not a tolerated
regression.

**One correction the measurement forces on sweep 01.** Sweep 01 recommends two
parallel `BitSet`s as the representation, on the grounds that it is the smallest
diff. Measured, it is **~81 ns/op — 4× slower than today at every width**, the
worst of five options, because it inherits mutability (the ~20 clone sites become
~40), inherits unboundedness, and needs five intermediate allocations per
four-state AND. **If the value program is going to fail on performance, that is
how it fails.** Take the sealed width-split record (`Word` for ≤64 bits, sparse
nullable-plane `Wide` above); reject the `BitSet` pair; keep `byte[]`-of-codes
for the *specification* type only, where `-` must exist and nothing is on a loop.

### Two rivals, named honestly

**The rival by urgency: P3's export coverage.** JLS cannot HDL-export its own
flagship design. Run at HEAD:

```
$ java -jar target/jls-5.0.5-SNAPSHOT.jar -export cpu.v riscv/build/addi.jls
jls: error: circuit "addi" contains elements HDL export does not support yet:
  Memory "imem" …; Memory "ctrl" …; ShiftRegister …; Memory "dmem" …
```

That is a bigger *immediate embarrassment* than anything in the value domain, and
it is blocked by **nothing** in the value domain — it shares no code. The two
should run in parallel, not in series. **The value domain is the keystone by
reach; export coverage is the keystone by urgency**, and it goes first in the
sequencing for exactly that reason.

**The rival by cost: the event queue.** On the measured profile the queue and its
dedup `HashSet` are 47.7% of the loop against the value type's 37.6%. If the
question were purely "what is the cheapest way to make JLS faster," the answer
would be the queue. It is *not* the keystone by the capability frame's criteria —
no standard is blocked on it, no pedagogy depends on it, it unlocks nothing — but
it must be scheduled alongside, because a value migration that leaves the queue
untouched will be reported as "we did all that work and got 18%." (An A/B with
dedup removed produced **identical final architectural state, 11% more events
fired, and 9–17% *less* time**. Dedup is specified behaviour with its own contract
test, so it must be *reimplemented* as per-element pending-event coalescing, not
deleted.)

### The spine

Drawn the way `docs/grand-architecture.md` §8 draws its own.

```
#77 headless core ──── grand-architecture's keystone. jls.core exists but holds
   │                   only geometry; P1's type lands inside it. See §5.
   │
P1-S0  kernel hygiene ─── SigSim StringBuilder + lazy vector streaming; intern
   │                      PinChanged; cache WireNet driver/sink arrays;
   │                      per-Simulator sequence counter.
   │                      INDEPENDENT OF EVERYTHING. Ships alone. Measurable.
   │
P1-S1  LogicValue lands ── X and U never produced; 27 coercions become
   │                       zeroFill(); every golden byte-identical.
   │                       Gate: RiscvCpuGoldenTest identical AND loop 15-25% faster.
   │
   ├─ P1-S2  X produced ──── resolution function replaces first-driver-wins;
   │     │                   importer stops coercing. Behind --value-model.
   │     │                   *** the pedagogically decisive stage ***
   │     │
   │     ├─ P1-S3  strength, driver kinds, PullUp/PullDown ── I2C, EVCD, #43/#44 qualifiers
   │     │
   │     └─ P1-S4  U + reset semantics ── P1-S5  IEEE 1164 projection + Bits4
   │
   └─ (P1-S1 also enables) zero-delay levelization ── the plane arrays, hoisted
                                                      out of the value objects

P2 element vocabulary ─┬─ register pins / memory ports / arithmetic  ── PARALLEL with P1
                       └─ bidirectional pin ── after P1-S2/S3 ── CSR/trap set ── after P2's registers

P3 interchange ────────┬─ export coverage (Memory, SubCircuit, ShiftRegister) ── PARALLEL, FIRST
                       ├─ instance IR / hierarchy ── EDIF, BLIF, SystemC, SPICE printers
                       ├─ interface bundles + reuse identity ── PARALLEL with P1
                       └─ round-trip CI property ── after hierarchy + P2

P4 timing ─────────────┬─ structured delay / units / glitch detector / STA / activity ── PARALLEL with P1
                       └─ timing checks driving X ── after P1-S2

P5 verification ───────── ALL OF IT PARALLEL. Needs nothing from P1. Edits 3 of 25 reacts.

P6 silicon on-ramp ────── after P2 (reset, bidir) + P3 (export coverage, hierarchy) + P4's library
```

**What can proceed in parallel — stated plainly, because it is the difference
between a 3-year plan and a 6-year one.** P5 in its entirety. P3's export
coverage, hierarchy and interface work. P4's delay model, time units, glitch
detector, STA and activity accounting. P2's additive-attribute half. Every one of
those is independent of P1, and together they are roughly 60 maintainer-weeks of
work that does not wait on the keystone.

**What is genuinely gated:** the bidirectional pin, timing checks that drive X,
the strength/pull work, `U`/reset semantics, the 1164 projection, boundary scan,
and all of P6.

---

## 4. Sequencing

Each stage is independently shippable and leaves the tree green. Stage numbers
are dependency-ordered, not calendar-ordered; the parallel lanes are marked.

| Stage | Content | Weeks | User-visible payload |
|---|---|---|---|
| **0** | **Kernel hygiene.** `SigSim` `StringBuilder` + lazy vector streaming; intern `SimEvent.PinChanged`; cache `WireNet` driver/sink arrays at `makeNet`/`recheck`; per-`Simulator` sequence counter. | 2–3 | **Yes.** Setup 0.57 s → ~0.01 s on a 6004-cycle run; loop ~15% faster from the shallower heap. Batch grading gets measurably quicker on day one. |
| **1** | **Total HDL export coverage.** `Memory`, `ShiftRegister`, `SubCircuit` leave the reject bucket; `HdlPolicyTest.memoryIsRejectedByName` and `.subCircuitIsRejectedCleanly` are inverted. | 5–8 | **Yes — the first capability.** *"Export your CPU."* The flagship design becomes exportable, which also unblocks #82, #213 and #215 from being capped at gate toys. |
| **2** *(lane B)* | **The verification floor.** Report channel + exit-status contract; timestamp closure; `Assert` element; toggle / truth-table-row / FSM-transition coverage. | 5–7 | **Yes.** Assertions marked red on the drawing; a coverage report; xUnit XML that CI and every LMS autograder already ingests. |
| **3** *(lane C)* | **`Register` control pins** + FORMAT 3. Async clear/preset, enable, load, reset value. | 3–5 | **Yes.** A reset line becomes a wire. Deletes 62 of the `riscv/` CPU's 228 elements. Unlocks the `$adff` family in the importer. |
| **4** *(lane D)* | **The glitch detector + sub-pixel transient rendering**, and physical time units. | 3–5 | **Yes.** Hazards become visible, named and countable; the consensus-term lesson lands; the VCD `$timescale` stops being a fiction. |
| **5** | **`LogicValue` lands.** Dual-mode; X and U never produced; all 27 coercions become `zeroFill()`; every golden byte-identical. | 12–16 | **No — deliberately.** *This is the only multi-month stage with no user-visible payoff, which is exactly why it is fifth.* Its acceptance criteria are byte-identity on `RiscvCpuGoldenTest` and a 15–25% faster event loop. |
| **6** | **X becomes producible.** Resolution function replaces first-driver-wins; importer stops coercing; X renders red on wires and traces, `x` in VCD, `X` on stdout. Behind `--value-model=four-state` for one release. | 4–6 | **Yes, and it is the big one.** Bus conflicts propagate; floating inputs become visible; three-valued gate tables become demonstrable and assignable. |
| **7** | **Strength, driver kinds, `PullUp`/`PullDown`, net kinds.** | 6–9 | **Yes.** The complete open-drain I²C lab. EVCD. The IEEE 91/60617 output qualifiers the conformance claim currently carves out. |
| **8** | **`U` + reset semantics**, and the IEEE 1164 projection + `Bits4`. | 4–7 | **Yes.** Reset-discipline labs; uninitialized memory reads X; GHDL differential testing becomes meaningful because the two simulators finally share a value domain. |
| **9** *(lane C)* | **`Memory` port model + arithmetic family + width adapter + importer mesh synthesis.** | 11–18 | **Yes.** Register files, FIFOs, caches, dual-port frame buffers; the array-multiplier lesson gets its second half; the `CellValidator` apology messages get deleted one by one. |
| **10** *(lane D)* | **Structured delay + STA + critical-path overlay + timing checks.** | 15–23 | **Yes.** The ripple-vs-lookahead carry chain lights up red and then shortens. fmax in MHz. Setup/hold become observable. |
| **11** | **Hierarchy IR + interface bundles + reuse identity + round-trip CI property.** | 15–24 | **Yes.** A five-peripheral bus system in one lab period; EDIF/BLIF/IP-XACT/SystemRDL; "JLS-exported HDL re-imports to the circuit it came from, and CI proves it." |
| **12** | **Cell layer + shuttle target**, then (optional) the layout view with cross-probing. | 12–20 (+8–12) | **Yes.** "Map your adder to sky130 and count the cells." "Fit your design in a tile." |

**The earliest stage that delivers user-visible value is Stage 0** — two to three
weeks, no new capability, but a batch run that finishes noticeably sooner, which
is the thing a single maintainer can feel immediately. **The earliest stage that
delivers a new capability is Stage 1**, five to eight weeks: exporting the
`riscv/` CPU.

**The scheduling rule that matters.** Stage 5 is twelve to sixteen weeks with
nothing to show. It is placed fifth, behind four stages that each ship something,
because a multi-month program with no intermediate payoff does not survive contact
with a single maintainer's calendar. Stages 1–4 buy roughly four months of
visible progress before the keystone's silent stretch begins — and Stage 5's own
`zeroFill()` mechanic then breaks *it* into reviewable commits with a
`grep -c` progress metric.

**The one behavioural change to existing saved circuits, deliberately not flagged
away:** at Stage 6, a circuit that today silently resolves a bus conflict to
"first driver in net order" will show X. That is the correction the whole program
exists to make. CHANGELOG headline, not a compatibility flag.

---

## 5. Relationship to the existing architecture

`docs/grand-architecture.md` names **#77, the headless core, as ITS keystone**,
and organizes everything around a module system. Does this roadmap conflict with
that, depend on it, or run orthogonal to it?

**Answer: orthogonal, with exactly one hard ordering constraint, one required
amendment, and three new extension-point seams. The maintainer does not choose
between two roadmaps.**

They answer different questions. `grand-architecture` answers *what shape JLS
should have*; this document answers *what JLS should be able to do*. #77 is the
keystone of the **structure**; the value domain is the keystone of the
**capability**. They intersect at one point.

**The one hard ordering constraint.** `LogicValue` belongs in `jls.core` —
AWT-free by construction, covered for free by `HeadlessCoreRatchetTest` and by
`ArchitectureRulesTest.coreDependsOnNoGuiClasses`. But `src/jls/core/` today
holds only geometry (`Bounds`, `Geometry`, `GridPoint`, `GridSize`,
`Orientation`, `SegmentGeometry`, `TextMetrics`); the model/sim/persistence
extraction has not happened. So P1's Stage 5 means performing the biggest type
migration in the program **across a boundary that is not yet drawn**.
Recommendation, and both keystone A §8 and sweep 01 reach it independently:
**sequence #77 before P1's Stage 5, or accept that P1's element pass doubles as
part of the extraction.** Nothing else in this roadmap cares.

**The module system is not aspirational — it is shipped.** `src/jls/module/`
contains `ModuleManifest`, `ModuleResolver`, `ModuleRuntime`, `ExtensionPoint`,
`ExtensionRegistry`, `Activation` and `JlsModule`;
`docs/extension-points.md` is the normative catalog; and
`ExtensionPointCatalogTest` cross-checks constants against the table **in both
directions**, so a row without a constant or a constant without a row is a build
failure. Every program here delivers *through* that mechanism rather than around
it:

- **P3's and P6's printers** (EDIF, BLIF, structural SystemC, SPICE `.subckt`,
  IP-XACT, SystemRDL, BSDL, STIL/WGL, the shuttle wrapper) are
  `hdl.exporter` contributions. That seam is **typed now** and already carries
  `PcfEmitter`. No new mechanism.
- **P6's readers** (Liberty, LEF, GDSII) and P3's hierarchical Yosys import want
  `hdl.importer`, which is **pending** with "cell-map/layout contract to be
  defined" and owning issues #61/#62. This roadmap gives that seam its first
  non-Yosys consumer, which is what should force its contract to be written.
- **P2's new elements** are `elem.element-provider` rows plus
  `gui.palette-contributor` rows — the mechanism `ElementRegistry` (#78) already
  is, with `ElementRegistryTest`'s totality check turning every omission into a
  build failure. That is the good kind of cost, and it collapses
  `ARCHITECTURE.md`'s honest sixteen-step element ritual.
- **P5 needs three seams that do not exist**: `sim.checker`,
  `sim.coverage-collector`, `app.report-writer`. Per the catalog's own rule —
  *"pending seams are named here first, so nobody invents a parallel mechanism"*
  — these get rows and owning issues in `docs/extension-points.md` **before** any
  code.

**The hot-plane rule (§6) is satisfied, and is being defended against a threat
that does not exist.** `docs/grand-architecture.md` §6 requires the discrete-event
loop to run inside `core` with zero plugin indirection, no capability lookup, no
cross-module call per event. P1 keeps `Simulator.runEventLoop` untouched and
`LogicValue` inside `core`. P5's coverage collector is a **concrete core type**
that the extension point *registers* — it never dispatches inside the loop, and
collection is gated exactly as tracing already is
(`BatchSimulator.afterEvent:144` opens with a single early return). Meanwhile
keystone C measures the loop as 37.6% value-container overhead + 47.7% queue
bookkeeping + 4.9% digital logic. §6 is protecting a loop that is already almost
entirely overhead. **The value-domain program does not endanger the hot plane; it
is the first serious attention the hot plane has had.**

**The one required amendment.** `ARCHITECTURE.md`'s recorded decision
"Simulation execution strategy: discrete-event interpreter is the sole strategy"
(#221, recorded 2026-07-26) carries a *binding* equivalence criterion that names
by reference "the **two-states-plus-HiZ value domain** and multi-driver/tri-state
resolution (§2, §9)". **P1 changes the criterion itself.** It must be re-anchored
to the new §2/§9 *before* P1 lands — and the clause states its own process:
*"Any divergence is a specified, documented change to `docs/simulation-semantics.md`
first, never a silent behavioral difference."* That process is already written
down; it just has to be followed.

Two further amendments to the same decision, both supplied by measurement rather
than argument:

- Its **revisit trigger** — "a concrete CPU-scale design on the `riscv/`
  trajectory that is unusably slow interactively" — is not a testable condition
  and nobody will ever agree it has been met. Keystone C measured **8,090
  simulated CPU cycles/s** in the warm event loop and **~1,070 dynamic
  instructions/s end-to-end from the CLI**. Restate the trigger quantitatively,
  e.g. *"below 10 kcycles/s on the #202 golden's CPU."*
- Its **differential oracle** (`RiscvCpuGoldenTest` plus `riscv/verify.py`'s
  fuzzing against a reference emulator) is exactly the right gate for P1's
  Stage 5, and should be made explicitly binding on P1 and P2 as well as on any
  future execution strategy.

**Verdict.** No conflict. One ordering constraint (#77 before P1's Stage 5).
One amendment (re-anchor #221's equivalence criterion, and quantify its trigger).
Three new catalogued seams. The two roadmaps compose: shipping #77 and the module
mechanism makes every program here an additive module rather than a rewrite —
which is precisely the claim `grand-architecture` §10 makes for itself.

---

## 6. What still stays out

Re-derived under the capability frame, not inherited. The frame allows three
grounds — different tool class, technically incoherent for a schematic-first
logic simulator, obsolete with no successor — and the sweeps found the frame
needs a **fourth**, stated at the end.

**(a) Different tool class.**

- **Continuous-time and analog.** #28 VHDL-AMS, #35 SystemC-AMS, #37
  Verilog-AMS, #42 Verilog-A, #88 Liberty CCS/ECSM (nonlinear I(V,t) driver
  waveforms), #113 Touchstone, the analog half of #112 IBIS, #130 IEEE 1149.4,
  #131 IEEE 1149.6, #132 IEEE 1149.7, #133 IEEE 1149.10. Supporting these means
  being a SPICE-class solver — a different tool, not a deeper digital model. The
  *digital shadow* of IBIS (drive strength, pull-up/pull-down, open-drain) is P1
  and is in scope.
- **Producing physical data.** #90 SPEF, #91 DSPF/RSPF, #97 ITF/ICT, #106
  iPDK/OPDK (parameterized layout generators requiring a layout engine), #107
  Calibre SVRF / Pegasus / ICV (proprietary rule languages executed by a geometry
  engine over billions of polygons), #98 OpenDFM, and all of #114–#128 (mask data
  prep, lithography, OPC/RET/ILT, fab equipment messaging, factory automation).
  OPC is a numerical inverse-imaging problem over an optical model; mask
  fracturing is shot decomposition for a specific writer's beam. Neither has
  logical content. Note honestly that OASIS.MASK is a P39 dialect that P6's
  reader would technically parse — *being able to read the bytes is not being
  able to do the work*, and shipping a viewer for data a teaching tool can never
  produce would be theatre.
- **#105 Si2 OpenAccess** — a membership-licensed C++ database API, not a format.
  There is no artifact to read or write.
- **#96 IEEE 2416** — system-level power modelling above the gate, consumed by
  architectural exploration tools.
- **#29 IEEE 1076.4 VITAL** — authoring back-annotated cell timing models for a
  characterized standard-cell library, consumed by ASIC sign-off simulators.
  Requires a PDK JLS neither has nor could have.
- **#36 Accellera SystemC synthesizable subset, as *import*** — consuming it means
  building the front half of an HLS tool: C++ parsing, scheduling, binding.
  (*Emitting* structural SystemC is in, and is one printer over P3's IR.)
- **Testbench programming languages and simulator C APIs.** #48 UVM as a class
  library, #51 IEEE 1647 `e`, #52 PSS as a language, #54 SCE-MI (transaction
  transport to a hardware emulator), #59 VPI/PLI, #60 VHPI, #61 DPI-C, and
  #55/#56/#57 as libraries. Their *capabilities* are delivered natively by P5;
  adopting their syntax at a 5% implementation would be conformance theater and
  would teach a dialect students could not use anywhere else. Their design ideas
  *are* taken: OSVVM's bin/holes model informs P5's coverage, VUnit's xUnit
  artifact shape is adopted as P5's report format.
- **#62 IEEE 1735** — IP encryption and rights management for commercial IP
  delivery. Different tool class, and v2's published cryptographic weaknesses
  make it something a teaching tool should not model.
- **#139 ATE native formats** (Advantest, Teradyne) — vendor tester programs, no
  public grammar. This is *not* an argument against #137/#138, which are the open
  interchange layer above them and are in scope.
- **#140–#146 PCB** (IPC-2581, IPC-D-356A, IPC-7351, IPC-2221/2222, Gerber,
  ODB++) — KiCad's domain, and KiCad is excellent at it. The honest near-miss:
  **IPC-D-356A is a netlist format** and JLS has a netlist, so the emitter would
  be a printer. It still stays out because a bare-board test netlist without a
  board layout has no consumer.
- **#147–#151, #153** packaging and qualification (JEDEC outlines, JESD47,
  AEC-Q100, JS-001, MIL-STD-883, IEC 60747) — physical test regimes applied to
  manufactured parts. No design-tool artifact exists.
- **#1 SysML v2, #3 ISO/IEC/IEEE 42010, #23 SDL, #24 AUTOSAR** — requirements,
  architecture viewpoints, reactive-system specification, automotive software
  architecture. A schematic editor that grew a requirements-traceability model
  would be a second program wearing the first one's UI.
- **#15 UCIe, #16 Bow** — die-to-die PHYs; the value is lane training,
  equalisation and ps-scale sideband timing. The link-layer FSM is drawable and
  worth nothing alone.
- **#18 PCIe, #19 USB, #20 Ethernet, #21 MIPI** — *partly out.* The PHYs are
  analog and correctly out. But 8b/10b encoding, USB NRZI + bit stuffing, and
  Ethernet CRC-32 are pure combinational/sequential logic, drawable today, and
  excellent labs. **Keep the standards OTHER — JLS should never claim USB or
  Ethernet conformance — and ship the sub-blocks as example circuits with no
  conformance claim.**

**(b) Technically incoherent for a schematic-first logic simulator.**

- **Full AMBA AXI / ACE / CHI** (part of #10). Five independent channels with
  ready/valid per channel and ID-based reordering produce a drawing no student
  can read; the abstraction genuinely breaks. **AXI-Lite is the ceiling, and it
  is a real one.**
- **Unbounded liveness** in P5's temporal layer (`s_eventually` with no bound),
  SERE repetition operators, and solver-backed constraint languages. The bounded
  subset is the whole teaching value; the unbounded part needs a model checker in
  the loop.

**(c) Obsolete with a successor.**

- **#95 Si2 CPF** — explicitly the legacy alternative to #94 IEEE 1801 UPF.
  Supporting both is duplicated work for one intent.
- **#69 LXT / LXT2 / VZT** — GTKWave's own superseded formats, with FST as the
  maintained successor from the same author.
- **#30 IEEE 1076.6, #32 IEEE 1364.1 / IEC-IEEE 62142** — withdrawn synthesis
  subsets with no successor conformance surface. There is nothing left to conform
  *to*; the subset is now whatever the synthesizer accepts, which for JLS means
  Yosys. (The *capability* they exist to enable — the synthesis don't-care — is
  P1's, and is very much in.)
- **#7 RISC-V Profiles** — a mandated bundle of other specs. Nothing to build; it
  becomes a checklist once #5 and #6 exist.
- **#128 IRDS** — a roadmap, not a standard.
- **#70 FSDB, #71 WLF, #73 UCDB** — proprietary binary databases with no published
  format and no independent reader. Strictly these are *unimplementable* rather
  than obsolete, which makes them another tool class's problem. #53 UCIS is the
  standardized cousin and is in scope.

**(d) The fourth ground the frame needs: contract gates, not capability gates.**

**#288–#303** (TSMC OIP ITC/ITF/RF and TSMC9000, Samsung SAFE / SAFE-QEDA, Intel
Foundry per-node qualification, PDK certification) bind a *named tool version* to
a *named PDK version* under NDA, renewably, with a commercial support obligation.
**There is no change to JLS's value domain, element model, timing model, kernel,
file format or UI that moves any of them one inch.** That is qualitatively
different from "different tool class," and the frame's three grounds have no slot
for it. They should gain one. The same ground covers **#259** (the RVI
"RISC-V Compatible" trademark listing — architecturally closed *and*
contractually closed) and **#182** (Apple notarization, already a recorded
decision).

**#136 IEEE 1838 (3D-IC test access)** is a deliberate *"later, cheaply"* rather
than a "never": the access network is drawable and comes nearly free if
#129/#134/#135 land; it presumes a die-stack model JLS has no current reason to
acquire. **#152 ECSS-Q-ST-60-02C** is mis-shelved by the survey — it is a
development-*process* standard, not a physical test, and belongs beside the §12.a
family `docs/standards-adoption/04-tool-qualification-and-scope.md` already
adjudicated.

### The open-silicon tapeout question (sweep 06, #304) — verdict

**In. Real. Cheap. And strictly gated.**

The survey's row is **factually stale**: Efabless ceased operations in March 2025
and chipIgnite went with it; OpenLane 2 was renamed **LibreLane** in early 2026
(repository moved `efabless/openlane2` → `librelane/librelane`). The surviving
open-shuttle path is **Tiny Tapeout on SKY130** via ChipFoundry / Swiss Chips.
The row should be rewritten and re-marked from ADJACENT to a real target.

The evidence that it is reachable: the shuttle's top-level signature is *fixed* —
`tt_um_*(ui_in[7:0], uo_out[7:0], uio_in[7:0], uio_out[7:0], uio_oe[7:0], ena,
clk, rst_n)`, verified by fetch in sweep 06 against the Tiny Tapeout Verilog
template — and that is structurally the same problem `src/jls/hdl/board/PinBindings`
already solves for PCF. `Board.Format`'s own javadoc anticipates new constants
beside `PCF`. The emitter is **2–4 maintainer-weeks**.

The evidence that it must be gated, and this is the part the enthusiasm has to
survive: `docs/standards-adoption/06-fpga-constraint-formats.md:578-582` already
states the governing rule — *"a constraint file with no documented path from
`.jls` to a programmed board is half a feature."* Applied to silicon:

1. **Without P3's export coverage**, the only designs that can be taped out are
   gate toys — the flagship CPU does not export at all.
2. **Without P2's reset model**, the wrapper wraps a chip that does not work.
   `test/resources/hdl/counter.v:19` emits `reg [3:0] count = 4'h0;`
   unconditionally, with no warning anywhere. FPGA synthesis honours it; ASIC
   synthesis silently discards it. The exported design simulates one way and
   fabricates another, and JLS's own goldens bless it. That is the single most
   dangerous paper-over in the tree with respect to silicon.
3. **Without P2's bidirectional pin**, `uio_oe` has nothing to bind to.

So: build it, label it accurately, and **ship it last** — after A, B and C. And
apply the honesty rule inherited verbatim from the #129 costing: say *"JLS emits
LibreLane/Tiny-Tapeout-shaped artifacts accepted by [named tool version]"*, never
*"JLS conforms to the shuttle flow."*

---

## 7. Honest totals

De-duplicated. Each change is counted once, under the program that owns it. The
six sweeps' own totals sum to roughly a third more than this, because
bidirectional ports appear in three sweeps, drive strength in four, and register
reset in four.

| Program | Maintainer-weeks | Maintainer-months | Entries unlocked |
|---|---:|---:|---:|
| **P1** value and resolution | 28–36 | 6.5–8.5 | 28 |
| **P2** element vocabulary | 22–32 | 5.0–7.5 | 18 |
| **P3** interchange and hierarchy | 26–38 | 6.0–9.0 | 21 |
| **P4** timing and analysis | 26–38 | 6.0–9.0 | 11 |
| **P5** verification | 19–28 | 4.5–6.5 | 12 |
| **P6** silicon on-ramp | 20–32 | 4.5–7.5 | 12 |
| Standalone items | 10–16 | 2.5–3.5 | 9 |
| **Total** | **151–220** | **35–51** | **~85 distinct entries** |

**Thirty-five to fifty-one maintainer-months. Three to four and a quarter
maintainer-years.**

That number is neither deflated nor padded. At a sustained half-time it is six to
nine calendar years; at one day a week it is not finishable. **This is therefore
not a plan to finish. It is a spine along which to choose**, and the staging in
§4 is built so that stopping after any stage leaves a shippable tree with
something new in it. Stopping after Stage 4 — roughly four months of elapsed
work, all of it visible — already delivers a CPU that exports, assertions and
coverage in the tool, a reset line that is a wire, and glitches you can see.
Stopping after Stage 7 delivers the two changes that most alter what JLS
*teaches*: honest bus conflicts and the open-drain bus.

Three caveats on the estimates, stated because a roadmap that hides them is
worse than useless. First, **P1's figure buys a green tree at every commit**: a
big-bang branch with a long red period lands the four-state core in 12–14 weeks
instead of 17–22, and the six-week difference is the dual-mode discipline. For a
project with blocking CI ratchets on three axes (JaCoCo package floors — `jls.sim`
at 92% line coverage is the most likely cause of a red build during the
migration; the PIT mutation threshold, which scopes `jls.BitSetUtils` and will
need a re-baseline; and ArchUnit), the dual-mode route is almost certainly
cheaper in wall-clock time despite being more weeks of work. Second, **the sizes
are estimates by analogy to shipped work the repo records** (#199's synchronous
memory, #78's element registry, #213's board export), not measurements — the only
measured numbers in this document are keystone C's benchmarks and sweep 05's
throughput figure. Third, **roughly 60 of the 151–220 weeks are parallel-safe**
(all of P5, P3's export and hierarchy work, P4's delay/units/glitch/STA/activity,
P2's additive-attribute half), so the *dependency-critical* path is materially
shorter than the total.

### What JLS becomes if the whole thing lands

A student opens JLS and draws a gate; an unconnected input shows `X` instead of
lying that it is zero, and when they wire two drivers to one bus the bus goes red
and the red spreads, because the tool now models contention rather than picking
whichever wire was drawn first. They add a pull-up and an open-drain buffer and
build a two-master I²C bus that arbitrates by itself. They draw a ripple-carry
adder and the critical path lights up along the carry chain with a number in
nanoseconds beside it; they draw a carry-lookahead adder and watch the number
fall, and watch a static hazard on the old one glitch three times and cost energy.
They write `after start, done within 8 cycles` on an assertion element, run a
thousand random stimuli, and get a coverage report saying which truth-table row
their tests never touched — and a solver-generated counterexample loaded back as
a test vector they can single-step. They wrap a CPU, a RAM, a UART and a timer
into reusable components with a Wishbone interface each, connect them with four
wires, and write a program that talks to the UART through a memory-mapped
register. They export the whole thing — hierarchy, memories, resets and all — to
Verilog that re-imports to the circuit it came from with CI proving it, push it
through Yosys onto an iCE40, and then map it to sky130, count the cells, click a
NAND on the schematic and watch its polygons light up in the layout view. The
tool that does all of that is still one jar, still offline, still gradeable from
a shell script — and it stops being a logic simulator that hides hardware behind
convenient fictions, and becomes one that shows students what hardware actually
does.
