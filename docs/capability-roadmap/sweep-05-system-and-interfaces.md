## System-level, ISA, and interface standards

*Capability sweep over survey entries #1–#24 (Tier 1: system, architecture,
interfaces) and #129–#139 (Tier 9: test/DFT, the drawable subset). Written
under the capability-expansion frame: the question is not "can JLS do this
cheaply" but "what would JLS have to become, and what else does that buy."*

The survey marked 20 of the 24 Tier 1 entries OTHER, 2 ADJACENT, 1 HAVE
(#5, RV32I), and 3 COULD (#4 IP-XACT, #6 privileged ISA, #11 Wishbone, #22
I²C/SPI — four, actually; the survey's own §2 prose then narrows even that
to "#4 is the only realistic addition"). Tier 9 got one COULD (#129) and a
subsequent full costing that ended in *defer*
(`docs/standards-adoption/11-costed-rejections.md:618-820`).

That verdict is an artifact of the preservation filter. Re-run under
capability expansion, Tier 1 is not a wasteland — it is the **densest
cluster of shared-blocker standards in the entire 304-entry survey**. Nine
Tier 1 entries (#4, #10, #11, #12, #13, #14, #17, #22, and half of #6) plus
four Tier 9 entries (#129, #134, #135, #84 by adjacency) are blocked by
exactly **four** model changes, three of which are individually small. No
other tier has that ratio.

The existence proof the frame asks for is real and measurable. The RV32I CPU
fixture `test/fixtures/riscv-sum1to10.jls` is **1038 saved elements — 810
`WireEnd`, 228 real elements** (43 `Mux`, 43 `Constant`, 34 `Splitter`, 34
`AndGate`, 32 `Register`, 9 `Binder`, 8 `NotGate`, 5 `Extend`, 5 `XorGate`,
4 `Adder`, 3 `Memory`, 3 `OrGate`, 3 `ShiftRegister`, 1 `Decoder`, 1
`InputPin`). A whole processor in 228 elements, because JLS elements are
word-wide. Scale is *not* the barrier to architecture-scale designs in JLS.
Two other things are, and this sweep names them.

Two measurements taken during this sweep, both load-bearing below:

- **Zero `SubCircuit` elements in the CPU.** `riscv/build_cpu.py` emits one
  flat netlist. There is no ALU block, no register-file block, no reuse.
  That is not laziness — see change **D**.
- **Throughput: ~1,070 dynamic instructions/second.** A 7-instruction loop
  program compiled by `riscv/make_cpu.py` to 36,865 dynamic steps ran in
  **34.4 s wall** under `java -jar target/jls-5.0.5-SNAPSHOT.jar -b -d
  73730000 -t big.clk.txt big.jls` (correct result, `x1 = 0x047FE800`). This
  is the number that decides whether "runs an OS kernel" is a sentence JLS
  can ever say.

---

### The blocked standards

| # | Standard | What blocks JLS today (code) | Change that unblocks |
|---|---|---|---|
| **#22** | I²C (NXP) — multi-master, wired-AND | **No drive strengths and no per-bit HiZ.** `WireNet.propagate` resolves a multi-driver net by taking *the first active driver in wire-end insertion order* and warning once (`src/jls/elem/WireNet.java:454-484`). An undriven net is `null`, and every consumer coerces `null` to all-zeros (`AndGate.computeOutput`, `src/jls/elem/AndGate.java:70-71`; 27 such coercion sites in `src/jls/elem/`). So an idle I²C bus with a pull-up reads **0** at every receiver, and a 0-vs-1 arbitration reads **whichever driver was drawn first**. Both are wrong. | **B** (strength-resolved value domain) |
| **#22** | SPI (de facto) — shared MISO, multi-slave | Same as above for the wire-OR / HiZ MISO; plus **no bidirectional port**: `Pin` is `sealed … permits InputPin, OutputPin` (`src/jls/elem/Pin.java:20`), `Put` is `sealed … permits Input, Output` (`src/jls/elem/Put.java:17`), and `SubCircuit` keeps only `inmap : Input→InputPin` and `outmap : OutputPin→Output` (`src/jls/elem/SubCircuit.java:33,35`). A slave subcircuit cannot present one pin that both drives and senses. | **A** + **B** |
| **#11** | Wishbone B4 | Nothing in the *simulation* blocks it — B4 is fully synchronous and MUX-interconnected, no tri-state required. What blocks it is **hierarchy and bundling**: a B4 slave interface is 9–11 signals; JLS has no grouped port, so each connection is 9–11 separate wires drawn by hand across a boundary; and `SubCircuit.save` writes *the entire nested circuit inline* (`src/jls/elem/SubCircuit.java:282-288`), with `Circuit.load` constructing a fresh `Circuit` per instance (`src/jls/Circuit.java:1021, setImported`) — so there is no reusable "Wishbone slave" component, only copies. Also blocked: `SEL_O` byte-select is meaningless because `Memory` has one monolithic `WE` (`src/jls/elem/Memory.java:188`). | **D** (+ **C** for `SEL_O`) |
| **#10** | AMBA APB (the simplest ARM interface) | Same as #11 — APB is synchronous, no tri-state. `PSTRB` needs byte lanes (**C**). What additionally blocks it is that **APB is defined by a protocol, and JLS cannot state or check a protocol**: the `-t` vector grammar is strictly open-loop (`docs/batch-interface.md` §2.2: `signal ::= name initial { ("for" duration \| "until" time) value } "end"` — no `wait`), so a testbench cannot hold `PSEL` until the slave asserts `PREADY`. | **D** + **E** |
| **#10** | AMBA AHB/AXI/AXI-Lite | All of the above, plus outstanding-transaction bookkeeping. AXI-Lite is reachable after **D**+**E**; full AXI is buildable but the schematic abstraction genuinely strains (5 channels × ready/valid × ID reordering). Honest boundary: **AXI-Lite in, full AXI out.** | **D** + **E** |
| **#12/#13/#14** | TileLink, Avalon, OCP | Identical shape to #11/#10. Each is a named signal bundle plus a handshake protocol. They come free with **D** and a library `.jls`, not with code. | **D** |
| **#4** | IEEE 1685 IP-XACT | Already fully costed in `docs/standards-adoption/08-ipxact-export.md` and gated on demand. Its blockers list (that doc, items 2/3/4) is exactly changes **A**, **C**, **D**: no reuse identity, no `inout`, no bus interfaces. Its own demand-gate (b) reads: *"If a real bus abstraction enters JLS (a Wishbone-style teaching bus element, landscape #11) … IP-XACT acquires content worth emitting"* (`08-ipxact-export.md:599`). **D fires that gate.** | **D** (+ **A**, **C**) |
| **#6** | RISC-V Privileged ISA | Four separate blockers. (i) `Register` has exactly two inputs, `D` and `C` (`src/jls/elem/Register.java:230-231`) — no enable, no reset, no set; trap entry needs a resettable, redirectable PC. (ii) `Memory` is single-port with one `WE` and no per-address write mask (`src/jls/elem/Memory.java:184-200`) — a CSR file is 4096 addresses with WARL/WLRL/read-only/hardwired-zero fields, which is a masked write, not a memory write. (iii) There is **no priority encoder element** — `Decoder` exists (`src/jls/elem/Decoder.java:183-206`), its inverse does not; interrupt prioritisation is a priority encoder. (iv) **Throughput**: measured 1,070 instr/s ⇒ a 10⁸-instruction xv6-class boot is ~26 hours. | **C** + **F** |
| **#5** | RV32I (HAVE, but incomplete) | `riscv/README.md` "Scope note (sub-word memory)": `lb/lh/lbu/lhu/sb/sh` are not implemented in hardware because the 32-bit `Memory` has no byte lanes. Also `HdlExporter.EXPORTED` (`src/jls/hdl/HdlExporter.java:418-424`) omits `Memory`, `ShiftRegister` and `SubCircuit` — **the RV32I CPU cannot be exported to Verilog or VHDL at all.** | **C** (+ exporter coverage) |
| **#65/#259** | riscv-arch-test / RISCOF | `docs/standards-adoption/05-riscv-compliance.md` already establishes that every test in `rv32i_m/privilege` is unreachable "because the `riscv/` CPU has no CSRs, no traps, no privilege modes." That is #6's blocker restated. | **C** + **F** |
| **#129** | IEEE 1149.1 JTAG + BSDL | The 16-state TAP controller is drawable *today* (`StateMachine`, though §8.2's busy-state semantics make a purpose-built element cleaner — `11-costed-rejections.md:678-682`). What is **not** drawable is a bidirectional boundary-scan cell, because there is no bidirectional pin (**A**), and the mandatory HIGHZ instruction plus TDO chaining want real 3-state resolution (**B**). BSDL emission additionally needs a device-pin concept — partially present as `jls.hdl.board.Board`/`PinBindings` — and `HdlModel.Direction` is a two-valued enum `{INPUT, OUTPUT}` (`src/jls/hdl/HdlModel.java:28-33`), so no emitter can say `inout`. | **A** + **B** + **G** |
| **#134** | IEEE 1500 embedded core test wrapper | Not evaluated by the survey (marked OTHER). It is **boundary scan around a subcircuit** — the same cells, the same WIR/WBR registers, applied at the hierarchy boundary JLS already has. Blocked by **A** and by the absence of subcircuit reuse identity. | **A** + **D** + **G** |
| **#135** | IEEE 1687 IJTAG | Also marked OTHER. A SIB network is muxes and flip-flops — entirely drawable. It is #129 plus one element. The survey's dismissal is preservation-filtered. | **G** (after **A**) |
| **#84** | SVF / XSVF (Tier 5, adjacent) | An SVF playback vector is the `-t` grammar plus **E**'s `wait`. Once #129 exists, this is a printer. | **E** + **G** |
| **#8** | RISC-V Debug Specification | Debug transport *is* a JTAG DTM (#129) driving a Debug Module that is memory-mapped on a system bus (#11/#10) and pokes CSRs (#6). It is the intersection of everything above — and it is the single most vivid demonstration that Tier 1 is one connected component, not 24 independent rows. | **A**+**B**+**C**+**D**+**F**+**G** |
| **#17** | JEDEC JESD79/209/235 (marked ADJACENT) | Not a fair OTHER: a *teaching-scale* SDRAM model (bank/row/column state machine, `tRCD`/`tCAS` as element delays, bidirectional `DQ`) is drawable after **A**+**B**+**C**. It would not be a JEDEC-conformant model, and JLS should never claim that — but "why does a row activate cost 15 ns" is a first-year lesson JLS currently cannot teach at all. | **A**+**B**+**C** |
| **#2** | UML 2.5.1 state machines | The survey's ADJACENT is right, and `StateMachine`/`State`/`SMUtil` are the "informal cousin" it names. No change needed; listed for completeness. | — |

---

### The changes, and what each unlocks

#### A. Bidirectional ports — a third `Pin` kind and an inout `Put`

**Technically.** Open the two sealed hierarchies. `Pin` at
`src/jls/elem/Pin.java:20` becomes `permits InputPin, OutputPin, BidirPin`;
`Put` at `src/jls/elem/Put.java:17` gains a third permitted subtype (or,
cheaper and probably better, `BidirPin` owns a co-located `Input`/`Output`
pair sharing one connection point, so `Put`'s hierarchy is untouched and
`WireNet`'s driver scan still sees an `Output`). `SubCircuit` gains a third
map alongside `inmap`/`outmap` (`src/jls/elem/SubCircuit.java:33,35`) and
`SubCircuit.init`'s tri-state propagation must mark a bidir put as both a
driver and a receiver. `HdlModel.Direction` (`src/jls/hdl/HdlModel.java:28-33`)
gains `INOUT`; both emitters learn to print it (`VerilogEmitter`,
`VhdlEmitter` — VHDL `inout std_logic` is trivial, and
`docs/standards-adoption/07-waveform-formats.md:118-121` records that today
`inout` appears in the tree *only* inside `HdlNames`' reserved-word lists).
One new `SaveTags` row; `FORMAT` header stays at 1 (purely additive).

**Standards unlocked.** #22 (I²C/SPI slave modules), #129 (bidirectional
BC_7 boundary-scan cells — the majority of real cells), #134 (IEEE 1500
wrappers), #4 (`ipxact:direction` gains `inout`, closing blocker 3 of
`08-ipxact-export.md`), #17 (DDR `DQ`), #31/#25 (Verilog/VHDL export
fidelity — JLS currently cannot round-trip a design it imports if that
design has an inout port), #75 (Yosys import of the same).

**Pedagogically.** A student can draw *a bus* — one wire that a memory
module and a CPU module both drive and both read — with the memory as a
reusable subcircuit. Today the only way to build a shared bus with hierarchy
is to manually split every bidirectional line into three pins (`data_out`,
`data_oe`, `data_in`), which is the exact synthesis-idiom transformation
that professional tools *hide* and that first-years find bewildering. The
lesson "a pin can be an input and an output at different times" is currently
untellable.

**What JLS papers over.** The CPU fixture has **zero `SubCircuit`
elements**: `riscv/build_cpu.py` emits 228 elements flat. Part of that is
generator convenience, but part is that once you want a bus between blocks,
hierarchy stops helping. `docs/standards-adoption/08-ipxact-export.md:52-56`
already records the gap verbatim: *"There is no bidirectional port … Tri-state
is a property of the net, not the port … and has no faithful component-port
encoding — it is simply lost."*

**Size: 4–6 maintainer-weeks.** One element class, two sealed-hierarchy
edits, one `SubCircuit` map, one enum value, two emitter arms, the
sixteen-place element checklist in `ARCHITECTURE.md` (partly collapsed by
`ElementRegistry`, #78).

---

#### B. Drive strength and a resolution function — open-drain, pull-ups, wired-AND

**Technically.** Replace the multi-driver rule at
`src/jls/elem/WireNet.java:454-484` — currently *"the first active driver in
net order … the user is told once"* — with a genuine resolution over a
value domain that carries strength. Minimum viable domain, per bit:
`{Z, weak0, weak1, strong0, strong1, X}`. That requires two sub-changes that
the normative document currently forbids:

1. **Per-bit HiZ.** Today HiZ is whole-signal `null`
   (`docs/simulation-semantics.md` §2: *"HiZ is all-or-nothing per signal…
   There is no per-bit HiZ"*, and `docs/batch-interface.md` §4.3 records the
   consequence: VCD *"mixed vectors like `b1z0` cannot"* be emitted). A
   `BitSet` value gains a companion mask, or the value becomes a small
   record of (levels, driven-mask, strength-mask).
2. **A resolution function**, IEEE-1164-shaped, applied per bit across all
   active drivers.

New elements: `PullUp` / `PullDown` (weak constant drivers) and either an
`OpenDrain` buffer or a `drive` attribute on `TriState`. The hot-loop cost
is contained by keeping a fast path: nets with ≤1 potential driver and no
weak elements resolve exactly as today.

**Standards unlocked.** #22 I²C — *this is the whole standard*; wired-AND
arbitration and clock stretching are the two lessons I²C exists to teach and
neither is representable today. #22 SPI multi-slave with pull-ups. #129 —
HIGHZ instruction, TDO chains, compliance-enable pins. #26 IEEE 1164 — the
frame's own example: `VhdlEmitter.java:469-495,575-578` writes `when others`
arms to satisfy *"VHDL's full-coverage rule over std_logic's nine values"*
while the simulator has two states plus a whole-signal Z. The emitter already
pretends to a value model the simulator does not have; **B** is the change
that stops it being a pretence. #67 EVCD — the survey calls it COULD, but
EVCD's *entire content* is strength and direction, so today JLS has nothing
to write into it; **B** is a precondition, not an option. #75 Yosys import —
`docs/hdl-support-research.md:467-468` names *"x/z bits (JLS BitSet is
2-state)"* as one of five "loudly-rejectable gaps". #17 (bus-hold, ODT).

**Pedagogically.** This is the item the frame's brief names by example, and
the brief understates it. Today a bus conflict resolves to *"the first driver
in net order wins"* plus a warning — a rule about **file order** presented to
a student as a rule about **hardware**. After **B**: two strong drivers
fighting produce X, X propagates, and the trace window shows the student
where the contention started and how far it spread. Separately, an
open-drain 0 against a pull-up 1 produces 0 — which is *why* I²C has no bus
contention, *why* it can have multiple masters, and *why* the SDA line
arbitrates for free. That is a genuinely famous piece of engineering design
and JLS cannot currently show it. Also unlocked: "your input is floating"
becomes a visible X rather than a silent 0 — the single most common
first-year wiring mistake, currently invisible because 27 sites in
`src/jls/elem/` coerce `null` to zero.

**What JLS papers over.** Three places, all in normative text: the
first-in-net-order rule and its one-shot warning
(`WireNet.java:475-480`), the `when others` VHDL arms
(`VhdlEmitter.java:469`), and `docs/batch-interface.md` §4.3's explicit
statement that mixed vectors "cannot" be emitted. The golden test that pins
the current model is named for the invariant this change breaks:
`test/jls/VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`.

**Size: 8–12 maintainer-weeks.** This is the expensive one and the correct
one. It touches the value domain (`src/jls/BitSetUtils.java`), the hot loop
(`WireNet.propagate`), 25 `react` implementations, 8 `computeOutput`
implementations in the `Gate` family, the VCD writer, the batch stdout
`HiZ` rendering, and two normative documents. It also needs a recorded
decision, because conflict resolution changes observably for existing files
(today: first driver wins; after: X) — the migration is a `FORMAT` bump with
FORMAT-1 files defaulting to the legacy resolution, or a global preference,
and the choice must be written down before code is written.

*Cross-reference: this change is the shared core of the value-domain sweep.
Costed here only as it applies to Tier 1/Tier 9; the 9-value IEEE 1164
target is that sweep's call, not this one's.*

---

#### C. Sequential-element port expansion — enable, reset, byte lanes, second port

**Technically.** `Register` (`src/jls/elem/Register.java:230-265`) gains
optional `EN` and `RST` inputs (with a `reset` attribute selecting
sync/async and a reset value, defaulting off so every existing file is
byte-identical). `Memory` (`src/jls/elem/Memory.java:184-200`) gains (i) a
per-byte-lane write-strobe input `WE[n]` replacing the single-bit `WE` when
a `lanes` attribute is set, and (ii) an optional second read port
(address₂/output₂) — the register-file shape. Optionally a `mask` attribute
for masked writes, which is what a CSR file is.

**Standards unlocked.** #6 RISC-V privileged (reset+enable on the PC is trap
entry; masked writes are CSRs). #5 RV32I completion — `lb/lh/lbu/lhu/sb/sh`,
which `riscv/README.md`'s scope note names as the deliberate omission. #11
Wishbone `SEL_O`, #10 AMBA `PSTRB`/`WSTRB` — byte strobes are a *mandatory*
field of both, so without lanes neither protocol can be built correctly even
after **D**. #65/#259 RISCOF's `rv32i_m/privilege` bucket. #75 Yosys — the
`$adff` family is named in `docs/hdl-support-research.md:465-467` as a
"genuine, loudly-rejectable gap" precisely because *"JLS Register has no
reset pin"*.

**Pedagogically.** Reset is the first thing a real design has and the last
thing a JLS student meets. Right now the only way to teach "power-on state"
is the `init` attribute, which is not a signal and cannot be driven — so
"press the reset button and watch the machine restart" is not a lab JLS can
run. Byte lanes make the endianness lesson concrete.

**What JLS papers over — the sharpest evidence in this sweep.**
`riscv/build_cpu.py:239-252` builds the 31-register file with, per register,
one `AndGate` and one 2-input `Mux` whose input0 is the register's own `Q`:

```
c.connect(reg.p("Q"), dmux.p("input0"))     # hold
c.connect(write_data, dmux.p("input1"))     # load
```

That is **62 elements of the CPU's 228 — 27% of the entire processor —
existing solely to synthesise a write-enable that the `Register` element
does not have.** Second site: `build_cpu.py:404-406` gates data-memory `WE`
with `NOT clk` because level-sensitive writes glitch (
`docs/simulation-semantics.md` §8.4 documents the hazard and issue #199
added a `sync` mode; the fixture still carries the gate). Third site:
`riscv/README.md`'s sub-word scope note.

**Size: 3–5 maintainer-weeks.** Two element classes, additive attributes,
two `MemoryModelTest`/`RegisterModelTest` extensions, `HdlModel`
`RegisterStatement` arms for the new pins, §7/§8 rows in
`docs/simulation-semantics.md`. Cheapest change in this sweep with the
highest ratio of unlocked standards to weeks.

---

#### D. Interfaces — a named, typed, role-flipped signal bundle, plus subcircuit reuse identity

**Technically, two halves that must ship together.**

*Half 1 — the bundle.* A first-class `Interface` declaration: an ordered
list of `(signal name, width, direction-relative-to-master)`, saved once in
the circuit and instantiated on a subcircuit boundary as a **single pin**,
drawn as one thick wire, connected in one gesture, with a master/slave role
flip at the instance. This is simultaneously SystemVerilog's
`interface`/`modport`, VHDL's record ports, IP-XACT's
`busInterface`/`abstractionDefinition`, and the Wishbone/APB signal set.
Crucially it belongs to the **cold plane** of `docs/grand-architecture.md`
§6: a bundle *elaborates* to N ordinary `WireNet`s at load time, so
`Simulator` and `WireNet.propagate` are untouched and the hot loop pays
nothing. Editor, file format and `SubCircuit`'s port model absorb the whole
change.

*Half 2 — reuse identity.* `SubCircuit.save` writes the entire nested
circuit inline (`src/jls/elem/SubCircuit.java:282-288`) and `Circuit.load`
constructs a fresh `Circuit` per instance (`src/jls/Circuit.java:1021`), so
two instances of "the same" block are two independent copies with no way to
tell they are the same. An SoC is *n* peripherals on one bus; without
identity, editing the UART means editing it *n* times. This needs a
component table in the save format (a VLNV-shaped `(name, version)` key is
enough) and a by-reference instantiation path.

**Standards unlocked — this is the program.** #11 Wishbone B4, #10 AMBA
APB and AXI-Lite, #12 TileLink, #13 Avalon, #14 OCP — **every on-chip
interconnect entry in Tier 1 is the same object**, and after **D** each one
is a shipped `.jls` library file plus a documentation page, not code. #4
IP-XACT — `docs/standards-adoption/08-ipxact-export.md` lists "no reuse
identity" and "no bus interfaces" as blockers 2 and 4 and sets its own
demand gate at *"if a real bus abstraction enters JLS"*; **D** fires that
gate and reduces IP-XACT to an XML printer over an object that now exists.
#33 SystemVerilog `interface` export. #38 SystemRDL (register maps are
bus-interface-addressed by definition). #134 IEEE 1500 (a wrapper is an
interface on a reusable core). Indirectly #8 (RISC-V Debug's Debug Module is
a bus peripheral).

**Pedagogically — this is where "educational logic simulator" becomes
"system design teaching tool", and the answer to the brief's question is
yes, it is real.** After **D**, a student in one lab period draws: a CPU, a
ROM, a RAM, a UART and a timer, each a reusable component, each attached to
one Wishbone bus with one wire each, with an address decoder in between —
and then writes a program that talks to the UART through a memory-mapped
register. That is the entire content of a computer-organisation course's
second half, and it is currently impossible: the same drawing today is
~60 hand-drawn wire segments per peripheral and is unreadable on a screen.

**Where the schematic abstraction *does* break down** — the honest half of
the answer. Bundles solve the *wiring*. They do not solve: (a) protocol
*semantics* — a student can wire APB correctly and still violate it, which
is change **E**; (b) address decoding, which stays hand-drawn comparators
(fine, that's the lesson); (c) burst/out-of-order protocols — AXI's five
channels with ID reordering produce a schematic no human reads, so
**AXI-Lite is the ceiling** and full AXI should be declined on the
"technically incoherent for a schematic-first tool" ground; (d) a full SoC
at 1,070 instr/s runs software too slowly to be interesting, which is **F**.

**What JLS papers over.** The flat 228-element CPU with zero subcircuits is
the tell. `HdlExporter.EXPORTED` (`src/jls/hdl/HdlExporter.java:418-424`)
excluding `SubCircuit` entirely is the second: JLS's own HDL export cannot
cross a hierarchy boundary, so hierarchy is already a second-class citizen
in the toolchain.

**Size: 6–10 maintainer-weeks.** Split roughly 3–4 for the bundle
(editor gesture, rendering, `SubCircuit` port model, save records,
elaboration at load) and 3–6 for reuse identity (component table, a
`FORMAT` bump, migration of inline subcircuits on load, and an honest
answer to "what happens when two files disagree about version 1 of a
component"). No simulator change. Ship the Wishbone and APB library `.jls`
files with it — they are a day each once the mechanism exists.

---

#### E. Closed-loop stimulus and a protocol-checker element

**Technically.** Two additions. (1) The `-t` grammar
(`docs/batch-interface.md` §2.2, parsed by `SigSim.initSim`,
`src/jls/elem/SigSim.java`) gains a reactive step — `wait <pin> <value>
[timeout t]` — turning an open-loop waveform into a testbench. Note the
grammar's other current limit, also worth lifting: *"input pins inside
subcircuits are not reachable"* (§2.2). (2) A first-class `Checker` element
implementing `jls.sim.Reacts`, with a small temporal vocabulary evaluated in
the event loop — `stable(sig, n)`, `req → ack within n`, `onehot(bus)`,
`never(cond)` — that reports through the existing `TellUser` channel and can
call `Simulator.stop`.

**Standards unlocked.** #11/#10 — a Wishbone or APB *conformance* lab, where
the student's slave is driven by a supplied master and a supplied checker
tells them exactly which rule they broke. #22 — I²C start/stop conditions
and setup/hold are temporal properties, unstatable today. #129 — TAP
controller state-sequence conformance. #65 RISCOF (its harness is
signature-compare, but mid-run trap checking needs this). #84 SVF playback.
And it moves #49 SVA and #48 UVM from OTHER to *a defensible teaching
subset exists* — not the standards themselves, but the concepts.

**Pedagogically.** Today the only verification a JLS student gets is
"compare the final answer." `riscv/verify.py:compare` and
`riscv/fuzz_diff.py` both do exactly that — final register file and data
memory, nothing during the run — because nothing *can* observe during the
run. After **E**, a student learns the actual professional skill: state the
property, let the tool find the violation, read the time stamp.

**What JLS papers over.** `riscv/verify.py` compares only final
architectural state (see `compare()`, and its `problems` list built purely
from `dump_regs()`/`dump_dmem_words()`), and `riscv/README.md` describes the
scheme candidly: *"the clock is an input pin driven by a `-t` test vector
that supplies exactly as many rising edges as the reference emulator took to
finish, then holds."* The number of cycles is precomputed by the emulator
because the hardware cannot signal that it is done.

**Size: 4–6 maintainer-weeks.** ~1 for the vector grammar (plus a
`docs/batch-interface.md` §2 revision, which is a stability contract and
needs the additive-only discipline), 3–5 for the checker element and its
vocabulary.

---

#### F. Architecture-scale execution — the CSR/trap element set, and the second simulator strategy

**Technically, two independent pieces.**

*Piece 1 — the missing elements.* A `PriorityEncoder` (JLS has `Decoder` and
no inverse — interrupt prioritisation, and also `clz`/`ctz`, need it), and
either a `CsrFile` element or `Memory`'s masked-write attribute from **C**
(WARL/WLRL/read-only/hardwired-zero fields are per-address write masks). Plus
**C**'s resettable register for trap-driven PC redirection.

*Piece 2 — throughput.* `ARCHITECTURE.md:341-368` records the decision that
the discrete-event interpreter is the **sole** strategy, and names its own
revisit trigger: *"a concrete CPU-scale design on the `riscv/` trajectory
(#200/#201/#202) that is unusably slow interactively."* **This sweep supplies
that trigger with a number.** 36,865 dynamic instructions in 34.4 s = ~1,070
instr/s. A privileged core running a timer-interrupt demo (10⁵ instructions)
is 90 s — acceptable. An xv6-class kernel boot (10⁸) is ~26 hours — not. The
levelized/compiled second strategy is options 2/3 of #221 and
`grand-architecture.md` §6 already specifies where it lives (inside `core`,
behind the same boundary) and what binds it: *"it must be observably
identical to the event model … and it must agree bit-for-bit with the #202
RV32I integration golden run as a differential oracle."*

**Standards unlocked.** #6 privileged ISA. #65/#259 the `rv32i_m/privilege`
RISCOF bucket. #9 SBI (a real M-mode firmware needs traps). #8 debug spec
(with **G**). #7 profiles becomes *statable* rather than implementable —
still a checklist, still OTHER.

**Pedagogically — the largest single jump available anywhere in the survey.**
The difference between "a datapath that computes" and "a machine that takes
a timer interrupt, saves `mepc` and `mcause`, vectors to a handler, and
returns with `mret`" is the difference between a computer-organisation
course and an operating-systems course. A student who has *drawn* the trap
path understands context switching in a way no textbook diagram delivers.
Second: privilege modes drawn as hardware make "why can't user code write
this register" a wire, not a rule.

**What JLS papers over.** `docs/standards-adoption/05-riscv-compliance.md`
states the consequence flatly — every privilege test is "unconditionally out
of reach", the trademark route is "closed and should not be attempted". The
recorded no-second-strategy decision papers over the same thing from the
other side, and honestly so: it explicitly waits for this measurement.

**Size.** Elements (piece 1): **4–6 maintainer-weeks**. The drawn privileged
core itself: **6–10 weeks**, and it is a `riscv/` Python project, not a
`src/` project — `riscv/build_cpu.py` (477 lines) roughly doubles, and
`riscv/riscv_ref.py` (975 lines) already has the reference semantics to
differentially test against. The levelized strategy (piece 2):
**10–16 maintainer-weeks**, and it should not start until the privileged
core exists to be slow.

---

#### G. Boundary scan as a shipped library plus a BSDL emitter

**Technically.** Depends on **A** (bidirectional cells) and **B** (HIGHZ,
TDO chaining). Then: a `TapController` element (purpose-built rather than
drawn from `StateMachine`, whose busy-state semantics at
`docs/simulation-semantics.md` §8.2 drop edges faster than `propDelay`) and
a `BoundaryScanCell` element (BC_1/BC_2 for unidirectional, BC_7 for
bidirectional); plus `src/jls/hdl/bsdl/BsdlEmitter.java` contributed at the
existing `hdl.exporter` extension point (`docs/extension-points.md`),
walking the `HdlModel` port list and a new scan-chain traversal to emit
`PIN_MAP_STRING`, `INSTRUCTION_OPCODE`, `BOUNDARY_LENGTH`,
`BOUNDARY_REGISTER`. BSDL is a VHDL subset and `VhdlEmitter` + `HdlNames`
already do the identifier legalisation.

**This has already been costed and deferred** in
`docs/standards-adoption/11-costed-rejections.md:618-820` at 15–25
maintainer-days, with a two-condition gate. Re-examined under the capability
frame, **one half of that deferral survives and one half does not.**

- *Does not survive:* "15–25 md" and "defer until a DFT course asks." Size
  is not grounds, and demand-gating a capability is the preservation filter
  by another name. The drawn TAP is a lab that no competing teaching
  simulator offers, and that document itself concedes option (b) "ships the
  drawn TAP elements (real teaching value, simulates today)".
- *Does survive, and should be kept verbatim:* the honesty rule. IEEE
  1149.1-2013 is paywalled; the doc's rule is *"if the maintainer has not
  read IEEE 1149.1, the docs must say 'JLS emits BSDL-shaped files accepted
  by <named parsers>', never 'JLS conforms to IEEE 1149.1'."* That is not a
  cost objection, it is a conformance-claim objection, and it is right.

**Recommendation: split the item.** Build the drawable half unconditionally
(it is the pedagogy). Build `BsdlEmitter` too, and label its output
"BSDL-shaped, accepted by *named parser*" until someone has read the
normative text. Do not let a paywall block a circuit.

**Standards unlocked.** #129, #134 (IEEE 1500 is the same cells at a
subcircuit boundary), #135 (IJTAG SIB networks are muxes and flip-flops),
#84 SVF (with **E**), and it makes #85 (IEEE 1532) visible rather than
imaginary.

**Pedagogically.** "Your chip has 40 pins, one of them is shorted to its
neighbour, find it without touching a probe to the board" — the student
shifts an EXTEST pattern through the boundary register and reads it back.
That lab is offered by no free teaching tool. It is also the only lab in
this entire sweep that teaches *manufacturing* rather than design, which is
a hole in every digital-logic curriculum.

**Size: 3–4 maintainer-weeks** for two elements and the emitter, **after**
**A** lands. (Consistent with the 15–25 md already costed; that estimate
assumed unidirectional cells only.)

---

### Ripple effects

**Normative documents.** `docs/simulation-semantics.md` is the big one and
**B** rewrites three of its sections: §2 (value domain — "two states plus
HiZ" and "no per-bit HiZ" both become false), §6.1 (`WireNet.propagate`),
§9 (tri-state and multi-driver resolution, wholesale). **C** adds rows to §7
(the delay table) and §8.1/§8.4. **A** adds a §6.1 paragraph on bidirectional
puts. `docs/batch-interface.md` §2.2 (vector grammar, **E**) and §4.3
(per-bit HiZ / `b1z0`, **B**) both change, and it is a published stability
contract — additive-only discipline applies, and §4.3's "cannot" becomes a
"MAY". `docs/file-format.md` gains element-tag rows for every new element
(the documented additive path) and, for **D**'s component table, a
`FORMAT_VERSION` bump — the first real one since #79.
`docs/hdl-support-research.md` §7.2's gap list shrinks by two entries
(`$adff` family, x/z bits). `docs/standards-landscape.md` §2, §10, §13.1 and
§14's relevance tally all need revision. `ARCHITECTURE.md`'s recorded
decision on simulation execution strategy needs its revisit trigger marked
**fired**, with the measured number.

**File format.** New `SaveTags` rows: `BidirPin`, `PullUp`, `PullDown`,
`OpenDrain` (or a `TriState` attribute), `PriorityEncoder`, `TapController`,
`BoundaryScanCell`, `Checker`, plus `Interface` declaration and instance
records. `Register` and `Memory` gain optional attributes (absent ⇒ current
behaviour byte-identically, the pattern issue #199 already established for
`int sync 1`). Only **D** forces a version bump.

**Element `react()` methods.** There are **25** `public void react`
implementations in `src/jls/` (`LogicElement.java:533` is the throwing base;
24 real ones across `Adder`, `Binder`, `Clock`, `Constant`, `Decoder`,
`Display`, `Extend`, `Gate`, `InputPin`, `JumpEnd`, `JumpStart`, `Memory`,
`Mux`, `OutputPin`, `Pause`, `Register`, `ShiftRegister`, `SigSim`,
`Splitter`, `StateMachine`, `Stop`, `SubCircuit`, `TriState`, `TruthTable`).
Change **B** touches every one that coerces HiZ to zero — **27 such
`== null → new BitSet()` sites** in `src/jls/elem/`, plus the **8**
`computeOutput` implementations in the `Gate` family. Changes **A**, **C**,
**D**, **E**, **G** touch between 1 and 4 each.

**Simulation hot loop.** Only **B** reaches it. `WireNet.propagate`
(`src/jls/elem/WireNet.java:443-531`) is the hottest method in the program;
per-bit strength resolution turns an O(drivers) equality scan into
O(drivers × bits). Mitigation is a guard: nets with ≤1 potential driver and
no weak element take the current path unchanged. **D** deliberately stays in
the cold plane (bundles elaborate at load), per `grand-architecture.md` §6.
**F**'s piece 2 replaces the loop with a second strategy, bound by the
bit-for-bit equivalence criterion `ARCHITECTURE.md:360-368` already states.

**GUI.** New palette entries in `SimpleEditor.makeElements`, toolbar icons
in `src/jls/edit/images/`, and help pages under `resources/help/elements/**`
with `Map.jhm` + `JLSHelpTOC.xml` rows (`HelpTopicsTest` fails until they
exist) — for each of ~8 new elements. **B** additionally needs X/weak
rendering on wires and in the trace window (a colour and a glyph; today a
wire is driven or HiZ). **D** needs the largest GUI work in the set: bundle
rendering as a single thick wire, an expand/collapse gesture, and a
connect-by-bundle mouse mode in the `SimpleEditor` state machine.

**Existing saved circuits.** All load unchanged for **A**, **C**, **E**,
**G** (additive tags and attributes). **B** is the exception and needs a
recorded decision: a FORMAT-1 circuit with two conflicting strong drivers
currently resolves to the first in net order; under a resolution function it
resolves to X. Either FORMAT 1 keeps legacy resolution or the change is
announced. **D**'s bump migrates inline subcircuits to component references
on load, which is a one-way transformation and needs a round-trip test.

**Existing tests.** Totality tests fire immediately on every new element:
`ElementRegistryTest`, `SaveTagsTest`, `FileFormatSpecTest`,
`ElementConstructorContractTest`, `AllElementsRoundTripTest`,
`HelpTopicsTest`, `ElementDrawSmokeTest`, `PinFaceContractTest`.
`SealedHierarchyTest` fires on the `Pin`/`Put` `permits` edits (**A**).
`ExtensionPointCatalogTest` fires on the BSDL exporter (**G**).
`MemoryModelTest` / `RegisterModelTest` extend for **C**.
`SimulationSemanticsRegressionTest` and `SequentialGoldenTest` need new cases
throughout. **B** rewrites `VcdExportGoldenTest` — including
`vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`, whose *name* asserts the
invariant being removed — plus `WireValueChannelTest` and
`BatchSimulationGoldenTest`. `RiscvCpuGoldenTest` is the cross-cutting
oracle: `ARCHITECTURE.md:363-366` makes bit-for-bit agreement with it
binding on any execution-strategy change, and it should be made binding on
**B** and **C** as well. `HdlPolicyTest` / `VhdlEmitterPolicyTest` /
`VerilogExportGoldenTest` fire on `HdlModel.Direction`'s new value (**A**).

---

### What genuinely stays out, and why

Judged only on the frame's three legitimate grounds.

- **#1 SysML v2, #3 ISO/IEC/IEEE 42010, #23 SDL (Z.100), #24 AUTOSAR** —
  *different tool class.* These describe requirements, architecture
  viewpoints, reactive-system specification and automotive software
  architecture. None has a drawable gate-level realisation; a schematic
  editor that grew a requirements-traceability model would be a second
  program wearing the first one's UI.
- **#15 UCIe, #16 Bow** — *different tool class.* The value of a die-to-die
  PHY is lane training, equalisation and ps-scale sideband timing, all
  analog/SerDes. The link-layer FSM is drawable and worth nothing on its own.
- **#18 PCIe, #19 USB, #20 Ethernet, #21 MIPI** — *partly out, and the
  survey's blanket OTHER is slightly too broad.* The PHYs (CDR, equalisation,
  D-PHY signalling) are analog and correctly out. But 8b/10b encoding, USB
  NRZI + bit-stuffing, and Ethernet's CRC-32 are pure combinational/sequential
  logic, drawable today, and are excellent labs. Recommendation: keep the
  *standards* OTHER — JLS should never claim USB or Ethernet conformance —
  and ship the sub-blocks as example circuits without a conformance claim.
- **#7 RISC-V Profiles** — *not implementable by nature.* A profile is a
  mandated bundle of other specs; there is nothing to build. It becomes a
  checklist once #5 and #6 exist.
- **#130 IEEE 1149.4, #131 IEEE 1149.6** — *technically incoherent for a
  digital schematic simulator.* Mixed-signal and AC-coupled test require an
  analog value domain (currents, capacitive edges). Even change **B**'s
  strength model does not reach them, and it should not try.
- **#133 IEEE 1149.10, #136 IEEE 1838** — *different tool class.*
  High-speed parallel test access and 3D die-stack test presuppose a
  package/stack model JLS has no vocabulary for and no reason to acquire.
- **#137 IEEE 1450 STIL, #138 WGL, #139 ATE native formats** — *different
  tool class.* These describe **tester programs**: pin electronics, timing
  sets, format definitions, per-pin drive/compare windows. That is
  test-program development, a distinct discipline with distinct tools. The
  one honest adjacency is that a boundary-scan vector emitted after **G**
  could be printed as SVF (#84), which is the JTAG-specific, tool-agnostic
  format — do that instead.
- **Full AMBA AXI/ACE/CHI (part of #10)** — *technically incoherent for a
  schematic-first tool.* Five independent channels with ID-based reordering
  and coherency states produce a drawing no student can read; the abstraction
  genuinely breaks. **AXI-Lite is the ceiling** and it is a real one.
- **RVI "RISC-V Compatible" trademark listing (#259)** — not a technical
  exclusion, but already correctly settled in
  `docs/standards-adoption/05-riscv-compliance.md`: the trademark route is
  closed. Passing `riscv-arch-test` buckets and *saying exactly that* is the
  real work (#65) and change **F** is its precondition.

Everything else in #1–#24 and #129–#139 that this sweep did not list above
is reachable by A–G.

---

### Sources

**Repo, read and verified at HEAD:**

- `/home/user/JLS/docs/standards-landscape.md:127-176` — Tier 1 table (#1–#24)
  and its "Where JLS sits" verdict; `:432-459` — Tier 9 table (#129–#153) and
  the #129 note; `:740-762` — §13.1 ranking; `:791-816` — §14 tally.
- `/home/user/JLS/docs/simulation-semantics.md:43-70` (§2, value domain),
  `:150-215` (§6, propagation and delay discipline), `:255-283` (§7, delay
  table), `:365-400` (§8.4, memory write hazard), `:401-430` (§9, tri-state
  and multi-driver resolution).
- `/home/user/JLS/docs/batch-interface.md:56-129` (§2, `-t` grammar),
  `:274-305` (§4.3, VCD value section and the per-bit-HiZ limitation).
- `/home/user/JLS/src/jls/elem/WireNet.java:443-531` — `propagate`;
  `:454-484` — the first-driver-in-net-order resolution and the one-shot
  bus-conflict warning; `:404-407` — `value`/`conflictReported` fields.
- `/home/user/JLS/src/jls/elem/Pin.java:18-20` — `sealed … permits InputPin,
  OutputPin`; `/home/user/JLS/src/jls/elem/Put.java:17` — `permits Input,
  Output`.
- `/home/user/JLS/src/jls/elem/SubCircuit.java:33,35` — `inmap`/`outmap`;
  `:282-288` — `save` writes the nested circuit inline.
  `/home/user/JLS/src/jls/Circuit.java:1021` — `setImported`, a fresh
  `Circuit` per instance.
- `/home/user/JLS/src/jls/elem/Register.java:230-265` — `D` and `C` only,
  `Q`/`notQ` out, all four orientations.
- `/home/user/JLS/src/jls/elem/Memory.java:184-200` — `address`, `input`,
  `WE`, `OE`, `CS`, optional `clock`, one `output`; single port, one
  monolithic write enable.
- `/home/user/JLS/src/jls/elem/Decoder.java:183-206` — decoder ports (no
  inverse element exists anywhere in `src/jls/elem/`).
- `/home/user/JLS/src/jls/elem/AndGate.java:64-75` — `computeOutput`, the
  `null → new BitSet()` coercion at `:70-71`.
- `/home/user/JLS/src/jls/elem/TriState.java:454-529` — `initSim`/`react`,
  the whole-signal HiZ discipline.
- `/home/user/JLS/src/jls/hdl/HdlModel.java:26-45` — `Direction {INPUT,
  OUTPUT}` and the `Port` record.
- `/home/user/JLS/src/jls/hdl/HdlExporter.java:418-433` — `EXPORTED` /
  `SKIPPED` / `TOPOLOGY` sets; `SubCircuit`, `Memory` and `ShiftRegister`
  are in none of them.
- `/home/user/JLS/src/jls/hdl/VhdlEmitter.java:465-495` — the `select`
  emitter and the `when others` arm documented at `:469-471` as satisfying
  "VHDL's full-coverage rule over std_logic's nine values"; `:575-578`,
  `:658`, `:690` — three more `when others` sites; `:67` —
  `use ieee.std_logic_1164.all`.
- `/home/user/JLS/riscv/README.md` — architecture narrative, the
  "Scope note (sub-word memory)" section, and the closing claim that nothing
  in `riscv/` modifies JLS.
- `/home/user/JLS/riscv/build_cpu.py:228-256` — the register file: per
  register one `AndGate` + one hold/load `Mux` (`:248-249` are the two
  `connect` calls quoted above); `:404-406` — data-memory `WE` gated with
  `NOT clk`.
- `/home/user/JLS/riscv/verify.py` — `compare()`, final-state-only
  comparison; `gen_clock()`, the precomputed cycle count.
- `/home/user/JLS/riscv/make_cpu.py`, `/home/user/JLS/riscv/riscv_ref.py`
  (975 lines, the golden oracle), `/home/user/JLS/riscv/jlsbuild.py`.
- `/home/user/JLS/ARCHITECTURE.md:115-146` — "Adding an element today (the
  honest list)"; `:341-368` — the recorded no-second-strategy decision, its
  revisit trigger, and the equivalence criterion.
- `/home/user/JLS/docs/grand-architecture.md:46-66` (§2 trajectories),
  `:237-313` (§5 module graph), `:314-343` (§6 cold/hot planes and the
  Verilator note).
- `/home/user/JLS/docs/hdl-support-research.md:453-476` (§7.2) — the
  "loudly-rejectable gaps": async-reset FFs, clocked/multi-port memories,
  `$mul/$div/$mod/$pow`, set/reset latches, x/z bits.
- `/home/user/JLS/docs/extension-points.md` — the `hdl.exporter` seam a BSDL
  emitter contributes to.
- `/home/user/JLS/docs/standards-adoption/05-riscv-compliance.md:1-80` —
  RISCOF mechanics, the privilege-bucket exclusion, the closed trademark
  route.
- `/home/user/JLS/docs/standards-adoption/08-ipxact-export.md:40-70` —
  blockers 2 (no reuse identity), 3 (no bidirectional port), 4 (no bus
  interfaces), 7 (`buildModel` rejects subcircuits); `:590-605` — the
  demand gate, including "a Wishbone-style teaching bus element".
- `/home/user/JLS/docs/standards-adoption/11-costed-rejections.md:618-820` —
  the full #129 costing, the drawn-vs-generated recommendation, the paywall
  discussion and the conformance-claim rule, the 15–25 md estimate and the
  defer verdict.
- `/home/user/JLS/docs/standards-adoption/07-waveform-formats.md:118-121`,
  `:619` — `inout` appears in the tree only inside reserved-word lists; the
  emitters never emit it.
- Tests referenced: `test/jls/RiscvCpuGoldenTest.java` (fixture path, 34
  steps, `HALF = 1000`), `test/jls/VcdExportGoldenTest.java:244`
  (`vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`),
  `test/jls/SimulationSemanticsRegressionTest.java`,
  `test/jls/elem/MemoryModelTest.java`, `test/jls/elem/RegisterModelTest.java`,
  `test/jls/elem/SealedHierarchyTest.java`,
  `test/jls/ExtensionPointCatalogTest.java`, `test/jls/HelpTopicsTest.java`,
  `test/jls/AllElementsRoundTripTest.java`.

**Measurements taken during this sweep (reproducible):**

- Element census of `test/fixtures/riscv-sum1to10.jls`: 1038 `ELEMENT`
  records (810 `WireEnd` + 228 logic elements, breakdown in the preamble),
  9360 lines of save-format text. Method: XZ-decompress and count
  `^ELEMENT (\w+)`. Zero `SubCircuit` records.
- Throughput: a 7-instruction loop assembled by `riscv/make_cpu.py`
  (`lui x3, 3` bound) → 36,865 dynamic steps; `java -jar
  target/jls-5.0.5-SNAPSHOT.jar -b -d 73730000 -t big.clk.txt big.jls`
  completed in **34.365 s** wall (30.1 s user) with the correct result
  `x1 = 0x047FE800`, on this container. ⇒ ~1,070 instr/s, ~2.1 M
  simulation-time-units/s. Single sample, no JIT warmup control — treat as
  order-of-magnitude, which is all the argument needs.
- Counts by grep over `src/jls/`: 25 `public void react` implementations;
  27 `== null` → `new BitSet()` coercion sites in `src/jls/elem/`; 8
  `protected BitSet computeOutput` implementations.

**External documents — none fetched during this sweep.** All standards
content (Wishbone B4 signal sets, AMBA APB/AXI-Lite channel structure, I²C
wired-AND arbitration, IEEE 1149.1 TAP architecture and BSDL attribute
names, IEEE 1500/1687 structure, RISC-V privileged CSR/trap model) is stated
from general knowledge and **is unverified against primary texts**. IEEE
1149.1-2013 is paywalled and has not been read by this pass — the
conformance-claim rule quoted from `11-costed-rejections.md:711-715` applies
to every statement here about #129. Sizes in maintainer-weeks are estimates
by analogy to the shipped work the repo records (issue #199's synchronous
memory, #78's element registry, #213's board export), not measurements.
