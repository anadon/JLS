# Is KiCad + ngspice a superset of JLS?

*A scope-and-migration survey, August 2026. Companion to
[`docs/standards-landscape.md`](standards-landscape.md) (what exists in
the field), [`docs/grand-architecture.md`](grand-architecture.md) (what
JLS should become), and
[`docs/capability-roadmap/`](capability-roadmap/) (what is worth
building).*

## 0. The question, and the short answer

The question: **KiCad is far more actively developed and far more
widely used than JLS. Is KiCad plus ngspice strictly a superset of what
JLS does — such that a migration path from JLS to KiCad would lose
nothing? And if not, what exactly would have to be built into KiCad for
it to absorb the JLS userbase?**

**The short answer: no, and not close — but the gap is not where the
question implies.** KiCad + ngspice is enormously larger than JLS in
*capability*, and simultaneously does not contain JLS in *any* of the
four dimensions that make JLS the thing its users use:

1. **The simulation value domain.** JLS simulates *n*-bit bus values as
   single first-class signals. ngspice's event-driven engine has no bus
   value at all — a digital node is one scalar, and even XSPICE's
   bracketed vector ports are a wiring convenience with no addressable
   value (§3.1).
2. **The element vocabulary.** JLS's 34 registered element types (§1.2)
   include a graphical state-machine editor, an editable truth table, a
   memory with an initial-contents editor, and a register file. XSPICE
   has textual analogues for two of these and no analogue for the rest,
   and none of them are reachable from KiCad's UI (§3.2, §4).
3. **The headless grading surface.** `kicad-cli` has no simulation verb
   at all **[verified: KiCad 9/master CLI docs list only `fp`, `jobset`,
   `pcb`, `sch`, `sym`, `version`, plus `api-server`]**. JLS's
   `-b -t`/VCD/exit-code contract is the interface every autograder in
   its userbase is built on, and it is a documented stability promise
   (`docs/batch-interface.md`).
4. **The FPGA/HDL bridge.** JLS emits structural Verilog-2005 and
   VHDL-93, imports Yosys JSON netlists, and emits iCE40 PCF pin
   constraints. KiCad has **no** HDL export; the community answer is
   third-party netlist converters **[verified via search: KiCadVerilog,
   plus long-standing unimplemented wishlist requests]**.

Conversely, the direction in which KiCad+ngspice *is* a strict superset
is genuinely enormous (§2), and it is worth naming honestly before the
gap list, because it is the reason the question is being asked.

The strategic conclusion (§7), stated up front so the detail can be
skipped: **KiCad is the wrong migration target for JLS.** It is a
different tool class solving a different problem — netlist-to-board with
analog verification — and every one of the four gaps above would have to
be built essentially from scratch inside a project whose maintainers
have no reason to want them. The tools in JLS's *own* class
(Digital, Logisim-evolution, DigitalJS) already have three of the four,
and JLS's existing Verilog emitter is already a better migration path
than anything KiCad could offer.

---

## 1. What JLS actually is, in migration-relevant terms

Grounded in the tree at HEAD, not in reputation.

### 1.1 The simulation model

From [`docs/simulation-semantics.md`](simulation-semantics.md), which is
normative:

| Property | JLS |
|---|---|
| Time | dimensionless non-negative 64-bit integer; no physical unit (`Simulator.now`) |
| Engine | pure discrete-event; time advances only by dequeuing events |
| Value | `BitSet` — an *n*-bit unsigned value carried by one signal |
| States | two-state (0/1) plus whole-signal HiZ, represented as a null reference |
| Unknown/X | **does not exist anywhere in the simulator** |
| Wires | ideal: zero delay, one value per net |
| Delay | lives entirely in elements; per-instance, user-editable; transport (not inertial) |
| Determinism | total — event order is a pure function of circuit content (canonical stable-id seed order, issue #181) |
| Multi-driver | first active driver in net order wins, deterministically, warned once |
| Termination | stopped / time limit / queue drained — three named outcomes |

Two things about this table matter more than they look:

- **"No X state" is a pedagogical feature, not a missing feature.** A
  first-year student wiring a NAND latch in JLS gets a settled answer.
  The same student in a 12-state simulator gets `U` propagating through
  the whole schematic and no idea why. (JLS's own roadmap does consider
  a four-state migration — `capability-roadmap/keystone-b-migration.md`
  prices it at 17–22 maintainer-weeks — but as a *deliberate, staged*
  expansion with the existing goldens held byte-identical, not as an
  inherited default.)
- **Dimensionless integer time with no convergence** means there is no
  timestep, no tolerance, no `.option reltol`, no failure-to-converge
  dialog, and no netlist that has to be electrically sensible. This is
  the entire reason a lab of 200 first-years can be told "draw it and
  press Start."

### 1.2 The element vocabulary

`src/jls/elem/ElementRegistry.java` registers **34 element types**:

```
Adder, AndGate, Binder, Clock, Constant, Decoder, DelayGate, Display,
Extend, FieldExtend, InputPin, JumpEnd, JumpStart, Memory, Mux,
NandGate, NorGate, NotGate, OrGate, OutputPin, Pause, Register,
RegisterFile, ShiftRegister, SigGen, Splitter, StateMachine, Stop,
SubCircuit, TestGen, Text, TriState, TruthTable, WireEnd, XorGate
```

Sorted by what a migration would have to reproduce:

- **Trivially portable** (a KiCad symbol plus an XSPICE primitive
  exists): `AndGate`, `OrGate`, `NandGate`, `NorGate`, `NotGate`,
  `XorGate`, `TriState`, `Clock`, `Constant`.
- **Portable only by per-bit expansion**: `Adder`, `Mux`, `Decoder`,
  `Register`, `ShiftRegister`, `Extend`, `FieldExtend`, `Splitter`,
  `Binder` — every one of which is *parameterized by bit width* in JLS
  and would become *n* instances plus wiring in a scalar-node netlist.
- **No analogue, textual or otherwise, reachable from KiCad**:
  `StateMachine` (graphical state/transition editor with Moore
  outputs), `TruthTable` (user-editable table as a placeable element),
  `Memory` (RAM/ROM with an initial-contents editor and RLE-encoded
  persistence, plus the synchronous-write mode of issue #199),
  `RegisterFile`, `Display`, `SigGen` (in-drawing stimulus),
  `Stop`/`Pause` (in-drawing simulation control), `JumpStart`/`JumpEnd`,
  `TestGen`, `SubCircuit` (any drawn circuit is directly instantiable
  as a component — no library, no symbol authoring, no footprint).

The last bullet is the migration's real cost center. It is also where
the pedagogy lives: a course that teaches finite state machines uses
`StateMachine`; a course that teaches memory hierarchies uses `Memory`;
a course that teaches datapaths uses `RegisterFile` + `Adder` +
`Mux` at parameterized widths.

### 1.3 The headless surface

`docs/batch-interface.md` is explicitly a **stability contract**: "any
change to them requires a CHANGELOG entry and either a major version
bump or a compatibility flag." It specifies:

- the `-t` test-vector grammar (`name initial {for d v | until t v} end`);
- the watched-element stdout report and its one-line outcome
  (`Simulation Stopped` / `Simulation Time Limit` / …);
- the process contract: exit 0/1/2, `jls: error: …` on stderr, stdout
  carrying *only* results so it can be diffed;
- a byte-deterministic IEEE 1364 §18 VCD profile.

Plus `-i` image export (PNG/JPEG/SVG), `-export out.v` Verilog export,
`-savetext` plain-text re-save, and a multi-arch container
(`ghcr.io/anadon/jls`, including `linux/riscv64`) whose whole purpose is
autograding in CI. `examples/autograde/autograde.py` is the worked
bridge.

### 1.4 The rest of the product

- **HDL/FPGA path**: `src/jls/hdl/` — `VerilogEmitter` (Verilog-2005
  structural), `VhdlEmitter` (VHDL-93 with `std_logic_1164` and
  `numeric_std`), `yosys/` + `imp/` (Yosys JSON netlist import),
  `board/PcfEmitter` (iCE40 pin constraints), `scan/` (Verilog/VHDL
  header scanners), `layout/` (schematic auto-layout for imports).
- **Deployment**: one self-contained jar, no install, any JDK 25+,
  including riscv64; plus native installers, an AppImage, and a Nix
  flake. Offline in-jar help and tutorial, version-locked to the binary.
- **Provenance and supply chain**: reproducible jar + CycloneDX BOM,
  signed build provenance, cosign-signed images, `.buildinfo`
  rebuild recipe.
- **Collaboration**: `src/jls/collab/{crdt,net,op,session}` — pure-P2P
  simultaneous editing, no server (issue #163 and its stack).
- **The corpus**: `.jls` files from ~two decades of coursework, plus a
  fork lineage (4.6–4.10) whose files JLS still loads. `riscv/` builds a
  working RV32I CPU *through the element model*, differentially fuzzed
  against a reference emulator.

~82k lines of Java, ~24k of it in `jls.elem`.

---

## 2. Where KiCad + ngspice genuinely is a superset

This half of the ledger is not close either, in the other direction. If
a JLS user's goal is anything on this list, they should already be using
KiCad and the question of migration is moot:

| Capability | Status vs JLS |
|---|---|
| Analog simulation (transient, AC, DC sweep, op, noise, distortion, PZ, sensitivity, Monte Carlo) | JLS has *nothing*; ngspice is a full SPICE |
| Real device models (BSIM, diodes, BJTs, magnetics, transmission lines, vendor `.lib`) | absent from JLS by design |
| Mixed-signal (`adc_bridge`/`dac_bridge` between analog and event domains) | JLS has no analog domain to bridge to |
| Electrical realism: real time in seconds, real V/I, temperature, tolerances | JLS time is dimensionless |
| PCB layout, routing, DRC, 3-D view, Gerber/ODB++/IPC-2581 output | different tool class entirely |
| Symbol/footprint libraries at industrial scale | JLS has 34 primitives |
| ERC, BOM, netlist export in many formats | JLS has none of these |
| Schematic file format with per-instance UUIDs, s-expression text | comparable to JLS's `sid`s; KiCad's is better established |
| Python API + out-of-process IPC API | JLS has neither (`capability-roadmap/lf-07` calls this "table stakes; JLS is one verb into it") |
| Internationalization, platform installers, an ecosystem, a real dev team | JLS is single-maintainer |
| Waveform plotting, cursors, measurements, `.meas` | JLS has a trace window and VCD export |

`docs/capability-roadmap/README.md` and `sweep-06-physical-boundary.md`
already record the boundary from the JLS side, in the same terms:
IPC-2581, Gerber, ODB++ and friends are "KiCad's domain, and KiCad is
excellent at it."

So the honest framing is not "which is bigger." It is: **the two
programs overlap in a narrow band — drawing a schematic of logic and
seeing what it does — and inside that band each has capabilities the
other lacks entirely.**

---

## 3. The gaps: what KiCad + ngspice cannot do that JLS does

Ordered by how hard they are to close.

### 3.1 No bus-valued signal (the structural blocker)

This is the single most important finding, because everything in §1.2's
"portable only by per-bit expansion" row depends on it.

- ngspice's event-driven layer has a **12-state digital node type**
  (three logic states × four strengths) **[verified via
  ngspice.sourceforge.io/xspice.html summary]**. A node is a *scalar*.
- XSPICE code models can declare **vector ports**, written in the
  netlist with bracket notation (`a1 [in1 in2] out d_and`). But this is
  a *port wiring* construct, not a value type: per the ngspice
  community, "the only groupings of digital nodes that exist in ngspice
  are the vector ports that some XSPICE devices have, and **there is no
  syntax to access those during or after simulation**" **[verified via
  KiCad forum / ngspice discussion search snippets — worth re-verifying
  against the current manual before acting on it]**.
- KiCad schematics *do* have bus notation, but the SPICE netlist it
  exports resolves buses to individual nets.

Consequences for a migration:

- A JLS 32-bit `Register` becomes 32 `d_dff` instances plus 32 nets.
  The `riscv/` CPU — 32-bit datapath, register file, memories — becomes
  a netlist with thousands of scalar nodes and no way to *look at* a
  32-bit value as a number.
- JLS's `Splitter`/`Binder`, whose entire job is bit-range extraction
  and composition on bus values, have no meaning in a scalar-node
  world: they become wiring, and the width-checking they provide at
  *edit* time (a real source of early student error detection) is gone.
- The trace window's "show this value in base 2/10/16" and the VCD
  profile's multi-bit vector variables have no source. VCD from
  `eprvcd` **[verified: ngspice `eprvcd` dumps event nodes to VCD]**
  would be one 1-bit variable per node.

There is no configuration or library that closes this. It is a property
of the ngspice event kernel's node type.

### 3.2 No structural elements above the gate

XSPICE's digital library covers gates, latches and flip-flops
(`d_and`, `d_nand`, `d_or`, `d_nor`, `d_xor`, `d_xnor`, `d_inverter`,
`d_buffer`, `d_tristate`, `d_dff`, `d_jkff`, `d_tff`, `d_srff`,
`d_dlatch`, `d_srlatch`), plus `d_source` (stimulus from a file),
`d_state` (a finite state machine from a state-table file), `d_ram`,
`d_fdiv`, and pull-ups/pull-downs **[model names from the XSPICE
documentation; the count "over 40 primitives" is verified, the exact
per-model list should be re-checked against the manual for the ngspice
version being targeted]**.

Matching that against §1.2:

| JLS element | Nearest ngspice thing | Honest verdict |
|---|---|---|
| `StateMachine` | `d_state` | Same *capability*, no shared *workflow*. `d_state` reads a text state table from an external file. JLS has a graphical state/transition editor with Moore outputs, edge-select, busy semantics, and a warn-once unmatched-edge report. The migration is "your FSM diagram becomes a text file you edit by hand." |
| `Memory` | `d_ram` | Same capability. No initial-contents editor, no RLE-persisted contents in the schematic file, no synchronous-write mode selector (#199), no tri-state data output tied into a bus. |
| `TruthTable` | none | `d_state` can be abused into a combinational table. There is no placeable, editable truth-table element. |
| `RegisterFile` | none | Build from `d_dff` + decoders, per bit, per word. |
| `Adder`, `Mux`, `Decoder`, `ShiftRegister` | none | Per-bit gate-level construction, or a custom C code model. |
| `SigGen` | `d_source` | External file vs. an in-drawing element with a dialog. |
| `Stop` / `Pause` | none | ngspice has no notion of the *circuit* stopping the run. |
| `Display` | none | No in-canvas numeric value readout. |
| `SubCircuit` | hierarchical sheets / `.subckt` | KiCad does this well — this row is a genuine match. |
| `Extend` / `FieldExtend` | none | Bus-valued; see §3.1. |

### 3.3 The models exist but are not reachable from KiCad

Even where ngspice has the primitive, KiCad does not surface it as a
usable digital-logic workflow:

- **No digital symbol library bound to the digital models.** KiCad ships
  simulation symbols oriented at analog. Digital work means hand-written
  `.model` cards and manually attached model text.
- **Symbol/model pin mismatch is a known, live problem.** The canonical
  example: the ngspice 7400 NAND model has 3 pins (2 in, 1 out) while
  the KiCad 7400 symbol has 14 (four gates plus power and ground)
  **[verified via KiCad forum and frdmtoplay.com write-up]**. Every
  multi-gate part needs manual reconciliation.
- **74xx models are `U` devices** (the PSpice/MicroCap digital device
  convention ngspice adopted) with a small-but-growing 74HCxx set. That
  is a *parts-level* library — the right abstraction for someone
  building a board out of real chips, and the wrong one for someone
  learning what a mux is.
- **No interactive stimulus.** ngspice runs an analysis and plots it.
  JLS's simulator window has Start / Step (by a user-set amount) /
  Animate / End, with values displayed live on the schematic at probes
  and watched elements. Stepping through a state machine and watching
  the drawing update is a core teaching interaction with no KiCad
  equivalent.
- **12 states, four strengths, `U` initial values.** Correct for
  hardware, hostile to a first-week lab (§1.1).

### 3.4 No headless simulation surface

`kicad-cli` subcommands are `fp`, `jobset`, `pcb`, `sch`, `sym`,
`version`, plus a headless `api-server` for the IPC API **[verified
against the KiCad 9 and master CLI documentation]**. **There is no
simulate verb.** The documented community workflow is: export a netlist
with `kicad-cli sch netlist`, edit it with a script, and invoke
`ngspice` yourself.

That is not fatal — `ngspice -b` exists and `eprvcd` writes VCD — but it
means the *migration target for JLS's grading interface is ngspice, not
KiCad*, and everything specific to JLS's contract has to be rebuilt in
the glue:

- the `-t` test-vector grammar (JLS: five keywords; ngspice: a
  `d_source` table file with a different timing model);
- the watched-element report format (JLS: a documented stdout grammar
  autograders parse; ngspice: `print`/`wrdata`/raw files);
- the outcome line and its three named reasons;
- the exit-code contract (0/1/2 with diagnostics on stderr);
- HiZ rendering as `HiZ`/`z` and the two-state-plus-HiZ VCD profile
  (ngspice will emit 12-state-derived values including `x`);
- byte-determinism of the VCD, which JLS golden-tests.

Every autograder in the JLS userbase is written against those. A
migration that does not reproduce them is a rewrite of every course's
grading infrastructure, not a file conversion.

### 3.5 No HDL export, no FPGA bridge

KiCad has no Verilog or VHDL emitter; the answers are third-party
(KiCadVerilog converts an exported netlist) and long-standing wishlist
items **[verified via search: no native HDL export found in KiCad 9
documentation; community tools and unimplemented requests only]**.

JLS ships `-export out.v` (Verilog-2005 structural, CI-validated by
compiling the output with Icarus Verilog), a VHDL-93 emitter with
`std_logic_1164`/`numeric_std`, Yosys JSON netlist *import* with a
cell→element mapping and auto-layout, and PCF pin-constraint emission
for iCE40 — i.e. a staged drawn-circuit → bitstream path (#59–#63,
#213/#215).

This one is worth stating sharply because it inverts the premise of the
question: **JLS's own Verilog emitter is already a better migration path
out of JLS than KiCad is.** A `.jls` file exported to structural Verilog
lands in the entire open HDL ecosystem — Icarus, Verilator, Yosys,
nextpnr, GHDL, GTKWave/Surfer — which is far more actively maintained
than KiCad's simulator and is where the circuit actually wants to go.

### 3.6 The rest of the ledger

- **Determinism as a contract.** JLS pins event ordering to circuit
  content and golden-tests byte-identical VCD output. ngspice's analog
  side has convergence and timestep behavior that varies across
  versions and platforms; even pure-event runs are not covered by a
  reproducibility promise. Autograding depends on this.
- **Deployment.** One jar, no install, any JDK 25+, riscv64 included,
  offline help. KiCad is a large native install with library packages —
  a real obstacle on locked-down lab machines and a non-starter where
  JLS is currently run from a shared jar.
- **Per-instance, user-editable propagation delay in abstract units**,
  with a "reset all delays" global. Teaching hazards and races by
  dialing a gate's delay is a JLS lab exercise; in ngspice delay is a
  model-card parameter in seconds.
- **In-drawing simulation control** (`Stop`, `Pause`) — the circuit
  halts the run.
- **Collaborative editing** (P2P, no server). KiCad has none.
- **The `.jls` corpus and its fork lineage**, plus a published,
  normative simulation-semantics spec that third parties (forks,
  autograders) build against.

---

## 4. What "extending KiCad to take over the JLS userbase" would require

Ranked by dependency, not by effort. Items 1 and 2 are prerequisites
for everything else.

1. **A bus-valued digital signal.** Either (a) extend the ngspice event
   kernel with an *n*-bit vector node type — a change to a mature
   upstream simulator that nobody currently wants — or (b) do not use
   ngspice for this at all and give KiCad a second, digital-only
   simulation engine. §3.1 makes this unavoidable; §5 argues (b).
2. **A two-state-plus-HiZ mode** (or a defensible mapping from 12
   states down to something a first-year can read), with the
   `U`-propagation problem solved by construction rather than by
   telling students to ignore it.
3. **A digital primitive library at the right abstraction** —
   width-parameterized adder, mux, decoder, register, register file,
   shifter, splitter/binder, extend — with widths checked at edit time,
   plus the symbol/model pin-reconciliation problem of §3.3 solved
   generically rather than per part.
4. **Graphical editors for the structural elements**: a state
   diagram editor, a truth-table editor, a memory-contents editor.
   Three nontrivial UI subsystems, each of which is a course's primary
   teaching surface.
5. **An interactive simulator UI**: run / step-by-*n* / animate / stop,
   with live values rendered on the schematic at probes and watched
   elements, and multi-bit values shown in base 2/10/16.
6. **A headless grading CLI** — a `kicad-cli sch simulate` verb, or a
   documented ngspice glue layer — implementing a test-vector input
   format, a diffable stdout report, a stable exit-code contract, and a
   deterministic VCD profile; then *published as a stability contract*,
   because autograders are the consumers.
7. **In-drawing simulation control and stimulus elements**
   (`Stop`, `Pause`, `SigGen`).
8. **Structural HDL export** (Verilog/VHDL) and Yosys JSON import, to
   match what JLS already ships.
9. **A `.jls` importer.** The geometry/net/hierarchy half is
   mechanical — `.jls` is a documented line-oriented text format
   (`docs/file-format.md`) and `.kicad_sch` is s-expression text with
   per-instance UUIDs. The semantic half is items 1–4: without them
   there is nothing to import the elements *into*.
10. **A deployment story for locked-down lab machines**, and offline
    help pitched at first-years rather than at PCB designers.

Items 1–5 are, collectively, "write a digital logic simulator and its
editors inside KiCad." Item 6 is a second product surface. Item 9 —
the thing that sounds like the whole job — is the last and easiest step,
and is worthless without 1–4.

---

## 5. If someone did want to build it, the least-bad shape

For completeness, since the question is framed as extending KiCad rather
than replacing JLS. Three candidate architectures for item 1:

- **(a) Per-bit expansion onto XSPICE primitives.** Cheapest to start,
  and wrong: it destroys the bus abstraction at the exact point where
  the pedagogy lives (§3.1), produces unreadable waveforms, and scales
  badly — the `riscv/` CPU becomes thousands of nodes.
- **(b) A native digital engine inside KiCad, ngspice used only for
  analog.** Correct, and the biggest ask: it means KiCad gains a second
  simulation kernel, with its own value domain, its own event queue and
  its own determinism contract. This is effectively "port JLS's
  `jls.core` into KiCad," which is what JLS's own architecture is
  already converging on (`grand-architecture.md` §3: an enforced
  headless kernel that the GUI merely consumes) — and it would be a
  C++ reimplementation of it.
- **(c) Elaborate the schematic to structural Verilog and simulate with
  an external tool** (Icarus/Verilator), surfaced through KiCad's IPC
  API. Best cost/benefit by a wide margin: it reuses maintained
  simulators, gets four-state and real bus values for free, gives VCD
  natively, and needs no ngspice change. It also drops interactive
  stepping unless the external simulator is driven incrementally, and it
  puts a synthesis-shaped toolchain in front of first-years.

The relevant observation: **JLS already ships (c)'s first half.** The
work that would make KiCad a plausible JLS successor is largely work
JLS has already done and KiCad has not started — which is a strong
signal about direction of flow.

---

## 6. Where a migration path *should* point

If the motivation is "JLS is one maintainer and KiCad is a project with
a future," the honest options are not KiCad:

- **Structural Verilog export → the open HDL ecosystem.** Already
  shipped (`-export out.v`), already CI-validated against Icarus. This
  is the durable exit: it preserves the *circuit*, and lands it in
  tooling with orders of magnitude more maintenance than either JLS or
  KiCad's simulator. It does not preserve the teaching UI, and does not
  pretend to.
- **Digital / Logisim-evolution / DigitalJS** — the *same tool class*.
  Each has bus-valued signals, structural elements above the gate,
  hierarchical subcircuits, and (for Digital and Logisim-evolution)
  test-vector grading. `grand-architecture.md` §7 already surveys them.
  A `.jls` → Logisim-evolution or `.jls` → Digital converter would
  preserve far more of what a JLS user has than a KiCad converter, at a
  fraction of the cost, because the target model actually matches.
- **KiCad as a *downstream*, not a successor.** The one genuinely
  sensible KiCad integration is the direction §3.5 already points and
  `capability-roadmap/README.md` already names: JLS emits a netlist;
  IPC-D-356A/netlist export toward a board is "a printer" away. Circuit
  in JLS → schematic/board in KiCad is a plausible *hand-off*, and
  keeps each tool inside its own class.

---

## 7. The determination

**KiCad + ngspice is not a superset of JLS, strictly or loosely.** It is
a superset of JLS's *electrical* modeling (which is empty) and of its
*physical* output (also empty), and a strict subset of JLS's digital
teaching model: no bus-valued signals, no structural elements above the
gate, no graphical FSM/truth-table/memory editors, no interactive
stepping with on-schematic values, no headless grading contract, no HDL
export. The overlap is the narrow band of "draw logic, see what it
does," and inside that band the two tools disagree about the value
domain at the kernel level.

For the stated purpose — a migration path that hands the JLS userbase to
a more actively maintained tool — KiCad would have to acquire a second
simulation engine, four editor subsystems, and a grading CLI, none of
which serve KiCad's own users. The same userbase is already served by
tools in JLS's class, and the circuits themselves already have a
maintained exit through JLS's Verilog emitter.

The recommendation: **do not build a JLS→KiCad migration path.** Keep
KiCad as a downstream hand-off for the board-level tail of the workflow,
treat the HDL emitters as the durable exit for the circuits, and — if a
same-class migration is wanted for the *userbase* — evaluate
Logisim-evolution and Digital as the converter targets, where the model
match is real.

---

## 8. Verification notes

Claims about JLS are anchored in the tree at HEAD and in its normative
docs (`simulation-semantics.md`, `batch-interface.md`, `file-format.md`,
`ElementRegistry.java`, `src/jls/hdl/`).

Claims about KiCad and ngspice were checked against public documentation
in August 2026 and are marked inline. Specifically:

- **Verified**: `kicad-cli` has no simulation subcommand (KiCad 9 and
  master CLI docs); ngspice's XSPICE layer provides event-driven
  simulation with a 12-state digital node type and 40+ code-model
  primitives; `eprvcd` dumps event nodes to VCD; KiCad has no native
  HDL export (community converters and open wishlist requests only);
  the 7400 symbol/model pin-count mismatch is a documented practical
  problem; ngspice 74xx digital modeling uses PSpice-style `U` devices
  with a growing 74HCxx set.
- **Verified from secondary sources, worth re-checking against the
  ngspice manual before acting**: that XSPICE vector ports are wiring
  only and carry no simulation-time addressable value; the exact
  membership of the digital code-model list in §3.2.
- **Not independently verified**: KiCad 10's simulator changes beyond
  what the KiCad 9 documentation covers. Nothing in §7 turns on them —
  the gaps in §3 are architectural, not release-specific — but a
  revision of this document should re-check §3.3 and §3.4 against the
  current release.

Sources consulted:
[KiCad CLI documentation](https://docs.kicad.org/master/en/cli/cli.html),
[KiCad SPICE simulation overview](https://www.kicad.org/discover/spice/),
[ngspice XSPICE](https://ngspice.sourceforge.io/xspice.html),
[ngspice XSPICE usage](https://ngspice.sourceforge.io/xspiceusage.html),
[ngspice as Eeschema backend](https://ngspice.sourceforge.io/ngspice-eeschema.html),
[ngspice manual, `eprvcd`](https://nmg.gitlab.io/ngspice-manual/interactiveinterpreter/commands/eprvcd_dumpeventnodesinvcdformat.html),
[XSPICE code model overview](https://deepwiki.com/imr/ngspice/5.2-xspice-code-models),
[multi-gate model/symbol mismatch](https://www.frdmtoplay.com/simulating-multi-gate-devices-in-kicad-with-ngspice/),
[KiCadVerilog](https://github.com/galacticstudios/KiCadVerilog),
[KiCad forum: running ngspice from kicad-cli](https://forum.kicad.info/t/execute-ngspice-by-kicad-cli/44358),
[KiCad forum: logic-gate style simulation in KiCad](https://forum.kicad.info/t/can-i-do-logic-gate-style-simulation-in-kicad/17234).
