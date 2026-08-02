# THE DETERMINATION — The Witnessed Boundary

*Target architecture for parity of software running on virtual logic and virtual
hardware in JLS. Synthesized from five proposals and four judgments over
`BRIEF.md`. Base proposal: **Two Elements and a Port** (minimal intervention),
reshaped on one axis and grafted from all four rivals. Every load-bearing repo
claim below was re-verified at HEAD this session; where the BRIEF is contradicted,
the contradiction is stated with evidence, as `BRIEF.md` §0 requires.*

---

## 0. Three corrections to the evidence base, before anything else

The panel argued for five documents on top of a factual ground that is stale in
three places. All three are verified at HEAD and all three change the answer.

**0.1 `docs/capability-roadmap/` exists — 19 documents — and no proposal cited
it.** It contains a recorded determination covering most of what the panel
re-derived from scratch:

- `keystone-c-performance.md` **measured this loop**: 318 ns/event, **8,090
  simulated CPU cycles/s warm** on `riscv/build/k2000.jls`, **4.9% of in-loop
  samples are `react()` bodies**, 47.7% is `PriorityQueue` + `dupCheck`, 37.6% is
  `BitSet`. (8,090 × 389 ev/cycle = 3.1M ev/s — so it and `BRIEF` §2 agree on
  events/cycle and differ only on measurement scope.)
- The same document **measured a levelized pass** over the RV32I CPU's real shape
  (522 evaluation slots, 4-state planes, real width mix, real opcode mix):
  **4.32 ns/node with plane arrays, 22.01 ns/node with `BitSet[]`**, and an
  activity-gated curve down to 0.68 µs/pass at 10% live. **The panel treated
  levelized per-node cost as its single largest unmeasured constant and guessed
  15/40/100 ns. It is measured, in-tree, at 4.32.**
- `lf-02-compiled-evaluation.md` already specifies the second engine, its two
  modes, its refusal policy, and — §2.7, verbatim — *"keeping the two engines
  identical: specify this, do not hope."*
- `lf-07-api-and-platform.md` already specifies the missing programmatic API
  (`jls.api`, five nouns, every mutation verb a shipped `CircuitOp`) and names
  `riscv/jlsbuild.py` as the workaround that proves the gap. Four proposals
  independently reinvented this as `CircuitBuilder` / `CircuitForge` /
  `CircuitElaborator` / `jls.mach.dsl`.

**Consequence: this program invents no engine, no IR, and no construction API. It
is a consumer of recorded programs.** Where a proposal's layer duplicates a
roadmap program, the roadmap program wins and this document contributes the first
real consumer for it.

**0.2 `RegisterFile` and `FieldExtend` are shipped.** `LogicElement`'s permits
list names 24 types, `ElementRegistry` has 35 rows, and
`src/jls/elem/RegisterFile.java` is a first-class multi-read-port,
clock-synchronous-write element. Three consequences: the "2 mirrored
`Memory(32×32)` = 9 elements" lever that four proposals treat as their clever move
is **obsolete** (it is one element, with true multi-port semantics and no
coherence hazard); every element census and every ev/instr figure in the panel is
stale in the favourable direction; and most importantly — **the precedent the
panel spent five documents arguing about is already set in-tree.** When a datapath
needed a ~95-element structure, the maintainer added a first-class element. The
question is not *may JLS have behavioural macro-elements*; it is *what rule did
#201 implicitly establish, and where is it written down*. It is not written down.
L4 writes it down and applies it retroactively.

**0.3 `riscv/` contains the parity harness, not just the builder.**
`riscv/riscv_ref.py` is a 975-line RV32I reference emulator; `riscv/fuzz_diff.py`
runs random programs on both the reference and the drawn CPU and requires
identical final architectural state; `riscv/verify.py` runs 11 directed programs.
`lf-02` §2.7 already cites all three as the existing oracle stack. Every proposal
read D5's "the *approach* may survive" as meaning programmatic construction only.
**The approach that matters is the differential harness, and it works today.** Its
design is salvaged in M1; its files are deleted. Two further casualties nobody
priced: `riscv/bench_kernel.py` + `riscv/build/k2000.jls` are the anchor for every
number in `keystone-c`, and `test/jls/RiscvCpuGoldenTest.java` +
`test/fixtures/riscv-sum1to10.jls` are **tracked** (falsifying `BRIEF` §7's
"gitignored, never run by CI") with their regeneration path inside `riscv/`.
Re-homing both is a precondition of deletion, not a follow-up.

---

## 1. The restatement

The maintainer asked for:

> *"parity of software running on virtual logic and virtual hardware … I want
> simulated hardware to be able to run and boot into a terminal only Linux
> distribution that can be interacted with live."*

**Restated in the form the evidence supports — this is a sharpening, not a
retreat, and one clause of it is a plain refusal:**

> JLS gains a **fidelity boundary**: any subcircuit may be bound at elaboration
> to a structural implementation (its drawn contents, simulated by the one event
> loop) or to a behavioural implementation of the same boundary, and the two are
> held equal by a differential harness that fails the build. A RISC-V SoC drawn
> in JLS, with its CPU subcircuit bound **behavioural**, boots a terminal-only
> Linux to a login prompt in **about three and a half minutes**, and a human
> types at it in a GUI console pane at **roughly 0.15–0.6 s per echoed
> character** and **tens of seconds per shell command**. The *same circuit*, with
> the *same* CPU subcircuit bound **structural** — the drawn machine, ~600
> ordinary JLS elements — boots the *same* image headless, and the two runs are
> proved to have produced the same retirement trace on every interrupt-free
> program short enough to check both ways, and the same guest output byte stream
> on the boot itself. At any point the behavioural run can hand the next ten
> thousand cycles to the drawn logic, with waveforms, for about a second of wall
> clock.

**What is refused, plainly.** *"Interacted with live"* is not achievable on the
structural tier and never will be inside this event model. `BRIEF` §5 is
categorical and I do not contest it: a tty echo path needs a budget of 2–23 events
per simulated cycle and the drawn machine spends 386. No stacking of
semantics-preserving constant factors closes 17–190×. **Nobody may promise a live
console on drawn logic.** What replaces it is two things, both real: the fidelity
boundary makes the interactive tier *the same circuit* rather than a different
product, and bounded handover windows make the drawn logic *inspectable from the
exact state your keystroke produced* at roughly one second per ten thousand
cycles.

**A second, quieter refusal.** *"Live"* on the behavioural tier means a 1970s
timesharing terminal, not a native shell. The echo is fast; the command is not.
`ls` is a fork + exec + readdir + write — 5×10⁶–2×10⁷ instructions — and answers
in **26–104 s**. Two of the five proposals computed echo latency and stopped; the
number a user experiences is command latency, and it is the headline metric here.

**A correction to the structural boot time, which every proposal inherited
wrong.** `BRIEF` §4's 1.9 h back-solves to α = 0.16, *below the floor of the
0.18/0.40/0.56 band `BRIEF` §10 prints two pages later*. At `BRIEF` §2's measured
1.8 events per active element per cycle, with L = 600 and CPI 2.9,
ev/instr = 1.8 · α · 600 · 2.9 = 564 / 1,253 / 1,754, and at R = 2.3M ev/s the
headless structural boot is **2.7 h / 6.1 h / 8.5 h**, honest band **2.4–9.7 h**.
Until M1 measures α, the structural boot is *"a few hours to most of a day"*, not
1.9 h. Nothing in this architecture depends on which; the CI plan does, and is
sized for the pessimistic end.

---

## 2. The layer stack

*In the idiom of `docs/grand-architecture.md`. Each layer names its purpose, its
new mechanisms, the files it touches, and what it depends on. The keystone is L4.*

### L0 — The measurement gate (ships no product code)

**Purpose.** Every wall-clock number in every proposal divides by a constant
nobody measured. Three spikes, days each, settle all of them, and one of them is
already half-built in-tree.

**New mechanisms.** None. Three throwaway experiments and one document.
- **(a) α, CPI, k.** `BRIEF` §10's experiment: the shipped single-cycle demo
  converted to a 2-cycle unified-memory machine (~10 elements: merged imem/dmem,
  an IR `Register`, a fetch-vs-data address `Mux`, a PC-hold `Mux`, a 2-state
  sequencer), event-counted with an internal `Clock`. Measures the 3.1× dominant
  uncertainty.
- **(b) events per retired instruction for a behavioural binding.** A ~200-line
  accumulator machine behind a fidelity boundary, wired to a real `Memory` on a
  real bus, event-counted. The modelled 12 is cross-checked only by 1.8 ev/active
  element × ~7 active elements ≈ 12.6; it has never been measured because nothing
  like it exists.
- **(c) levelized ns/node at CPU scale.** Re-run `keystone-c`'s `Levelized.java`
  at ~1,400 slots (a 600-element machine's shape) instead of 522, with the
  activity variants. `keystone-c` measured 4.32 ns/node at 522; the question is
  cache behaviour at 2.7× the working set.
- **(d) adjudication:** the event polled past `maxTime` is removed from `dupCheck`
  *before* the limit check and then discarded (`Simulator.java:224-234`). A
  curiosity today; silent corruption under any future state capture. Adjudicate
  and file now.

**Files.** `docs/machine-calibration.md` (new, normative-evidence: the 228-element
census, 389 ev/cycle, R ~ L^-0.12, α/CPI/k, ev/instr, with method — so `riscv/`'s
evidence outlives its files); a committed benchmark fixture replacing
`riscv/build/k2000.jls` as `keystone-c`'s anchor; re-homed
`test/fixtures/riscv-sum1to10.jls` regeneration path; the salvaged *design* of
`riscv_ref.py`/`fuzz_diff.py` written into `docs/parity-contract.md` §0 before
deletion.

**Depends on.** Nothing. **Blocks.** Everything — see §9.

---

### L1 — Engine constant factors (recorded work; this program is its first consumer)

**Purpose.** Buy 2–5× on every event with **zero semantic change and no recorded
decision reopened**, before spending a day on architecture. This is
`keystone-c`'s stage 1 and `lf-02`'s Mode T; it is not new work and must not be
re-specified here.

**New mechanisms.** Per `keystone-c` §7–8 and `lf-02` §2: a time-bucketed calendar
queue with an intrusive queued flag replacing `PriorityQueue` + `HashSet`
`dupCheck`, preserving `(time, seq)` total order exactly; the width-carrying,
immutable, plane-encoded value replacing defensive `BitSet.clone()`; levelized
zero-delay closure (`lf-02` Mode T — **not** user-visible, **not** a flag, and
**not** a second strategy: it collapses the 82.3% of events that model no elapsed
time, leaving every per-element propagation delay intact); and the two O(n²) bugs
(`SigSim.initSim` string concatenation, `Circuit.finishLoad`).

**Acceptance gate, which is the whole point.** **Byte-identical VCD and stdout
across the entire existing golden corpus.** If any golden moves, L1 is wrong.
Locked in afterwards by `SimulationBudgetRatchetTest` (grafted from structural-
first): events-per-clock-cycle as a **hard equality** on committed fixtures plus
ns/event within a band, so performance regression is a build failure rather than
a discipline.

**Files.** `src/jls/sim/Simulator.java`, `SimEvent.java`, `src/jls/elem/WireNet.java`,
`src/jls/BitSetUtils.java`, `src/jls/Circuit.java`, `src/jls/elem/SigSim.java`, the
value path across ~24 `react` bodies; `test/jls/sim/SimulationBudgetRatchetTest.java`.

**Depends on.** L0(d). **Note.** `keystone-c` projects **25–40 kcycles/s** on the
RV32I CPU after stage 1, from 8,090. Every wall-clock number in §4 is given both
before and after L1.

---

### L2 — Capacity and the long-lived batch run

**Purpose.** Make a multi-billion-event, multi-megabyte-guest, human-attended run
*expressible*. Removes ceilings; makes nothing faster.

**New mechanisms.** `-d 0` unbounded (`JLSInfo.defaultTimeLimit = 100000000` is
~400× short of a boot); `Memory`'s `DENSE_CAPACITY_LIMIT = 1<<22` — **exactly**
16 MiB at 32-bit words, zero headroom against Linux's 12–16 MiB — replaced by a
byte budget; copy-on-write init image (`initSim` currently does `initMem.copy()`,
doubling heap); **a raw-binary memory image section** — today `Memory.initOK`
constructs a `new Scanner` *per line* and `initSim` reads UTF-8 hex text into a
`char[]` then copies to a `String`, so a 4.19M-word image costs 20–85 s and ~64 MB
transient, and is a 33 MB `.jls` at 15.87 B/word; a long-lived batch mode
(`BatchSimulator.pause()` is literally identical to `stop()` today); progress
heartbeat and clean SIGINT.

**The binary image is not a sidecar.** Per maintainer decision **D3**, it is an
**optional, independently versioned section with must-understand semantics** — an
old reader skips it and opens the circuit structurally with a clean diagnostic.
Four of five proposals invented a private sidecar format specifically to avoid
touching `docs/file-format.md`; D3 names this exact case as the reason to grow
optional sections instead. D1's arithmetic is the forcing constraint: a 16 MiB
image is ~66 MB of text and alone exceeds `MAX_CIRCUIT_TEXT_BYTES` = 64 MiB.

**Files.** `src/jls/JLSInfo.java`, `src/jls/sim/Simulator.java`,
`src/jls/sim/BatchSimulator.java`, `src/jls/elem/Memory.java`,
`src/jls/JLSStart.java`, `docs/file-format.md` (+1 optional section),
`docs/batch-interface.md` (additive, CHANGELOG per its own §6).

**Depends on.** L1 (order only, not correctness).

---

### L3 — The host boundary: one door, granted at invocation

**Purpose.** Give a running circuit — *either* tier — a byte exchange with a human
or a script, without a transport, without a foreign thread posting events, and
without destroying determinism. Verified: zero `System.in` in all of `src/`.

**New mechanisms.**
- `jls.elem.Console` — **the only new element in this architecture.** The measured
  minimum 16550: three byte addresses (THR write, RBR read, LSR read returning
  `0x60 | data_ready`), polled, irq = 0, no PLIC.
- `jls.io.HostBytePort` — a **sealed** interface permitting `NullPort`,
  `StdioPort`, `FilePort`, `PipePort` (the in-memory test double), `PanelPort`.
  Sealing is structural, not etiquette: #212's external-provider gate cannot reach
  a sealed contract, so a loaded `.jls` can never acquire host I/O.
- **No cross-thread `post()`.** The `Console` owns a lock-free MPSC byte ring; the
  host thread offers, the simulation thread drains inside `Console.react`. The RX
  side **self-schedules its next poll event exactly as `Clock` self-schedules its
  next transition** (`docs/simulation-semantics.md` §8.3 already specifies that
  idiom normatively). `BRIEF` §7's "no event-injection path — fatal" therefore
  needs **zero lines in `jls.sim`**: the gap exists only for a design that pushes.
- **Record / replay.** A `ConsoleTranscript` of `(stamp, byte)` where the stamp is
  **retirement index or simulated time, never wall clock**. This is the shipped
  #167 `CircuitOp`/`OpSink` vocabulary generalized from *edits to the drawing* to
  *interactions with the machine* (grafted from THE BENCH), which is why it is a
  use of an existing mechanism rather than a new subsystem. It extends
  `Simulator.initSimulation`'s #181 invariant from *"every simulated value is a
  pure function of circuit content"* to *"…of circuit content **and the
  transcript**"* rather than quietly breaking it at the first keystroke.
- **The session-boundary rule, enforced by a test** (grafted from THE BENCH): no
  `java.io`, no host handle, no extension lookup on any path reachable from
  `Reacts.react()`; the port fills and drains the ring only at declared
  boundaries. `SessionBoundaryRatchetTest`, alongside `HeadlessCoreRatchetTest`.
  This turns `grand-architecture` §6's hot/cold prose into a build failure.
- **Grant, never ambient.** Host I/O requires `-console`/`-console-script` or an
  explicit GUI action, and the grant is named on the run's outcome line.

**Files.** New `src/jls/elem/Console.java` (+ permits, `ElementRegistry` row,
`Palette` row, help page, `Map.jhm` topic, `JLSHelpTOC.xml` entry, round-trip
fixture, icon — the ritual, counted, once); new AWT-free `src/jls/io/*`;
`src/jls/sim/Simulator.java` (+`hostPort()`, ~10 lines);
`src/jls/sim/BatchSimulator.java`; `src/jls/JLSStart.java`;
`docs/extension-points.md` (+1 row); `docs/reproducibility.md`.

**Depends on.** L2 (long-lived batch). **Independently valuable:** a drawn FSM can
print. It is the most conspicuous element JLS lacks against Logisim-Evolution and
Digital, which both ship TTY components.

---

### L4 — The fidelity boundary — **THE KEYSTONE**

**Purpose.** Make parity a property of a **boundary**, which is what makes it
testable — the maintainer's own adopted framing (`BRIEF` §11 **D4**), which no
proposal built. This layer replaces "add a `Cpu` element to the sealed permits
list", which four of five proposals did, with something strictly better.

**The mechanism.** A per-instance saved attribute on `SubCircuit` naming one of a
**closed, core-internal, sealed set** of implementations of *that instance's
definition*. Structural is the default and is what happens today. A behavioural or
compiled binding is **one element with one `react()`**, structurally
indistinguishable from `Adder` (lumped `30 × bits`), `Memory` (lumped
`accessTime`), `TruthTable`, `StateMachine` or the newly-shipped `RegisterFile`.

**Why this is strictly better than a new `Cpu` element — five reasons.**
1. **The structural referent is in the same file.** Behavioral-first's legitimacy
   condition 2 ("realizable AND realized AND checked") is discharged *by
   construction* instead of by a promissory note drawn on a layer at position 7 of
   8. The skeptic judge's fatal finding against behavioral-first — *"for the
   entire plausible lifetime of the effort the shipped artifact is an emulator
   plus a document saying emulators are illegitimate"* — cannot arise, because the
   binding is meaningless without a definition to bind to.
2. **No new sealed permit, no palette entry, no help page, no icon, no
   `-Werror` switch-exhaustiveness ripple.** `SubCircuit` already exists in
   `permits`. One new element total (L3's `Console`), not two to nine.
3. **Handover is free.** Toggling the binding at a declared instant maps only *one
   boundary's* architectural state; `Memory`, `Console` and the bus are the same
   objects in the same run. THE BENCH's best idea arrives **without** the general
   simulation-state serialization that its own judgment identified as its
   unverified keystone.
4. **It is the maintainer's recorded direction of travel**, and it lands
   `lf-02` §2.6's switch-over policy as a real mechanism rather than a plan.
5. **It is provable at student scale on day one** — an ALU subcircuit, drawn vs
   compiled — with zero RISC-V and zero Linux. See M3.

**The observation function** (this is the contract, and it must be written before
any binding exists): across a fidelity boundary the observable is the **settled
output word per sampling instant, indexed and not timestamped**. Sampling instants
are quiescence points or edges of a declared sync net. What is quotiented out is
**combinational transport delay strictly inside the boundary and nothing else** —
sequential elements keep their own delays and their §8 edge semantics, the value
domain is unchanged, and tri-state resolution at the boundary still happens in
`WireNet.propagate`, outside the toggled element.

**Enforcement, not policy.**
- `jls.sim.equiv.BoundaryHarness` — runs the same stimulus through both bindings
  and compares the observation function. **Exhaustive under 16 input bits; a
  seeded 10⁶-vector sample plus declared corner vectors (width edges
  1/31/32/33/63/64/65, HiZ, undriven) above.** (Grafted from THE ELABORATION
  SPINE's `MacroContractionHarness` and `PrimitiveParityHarness` — its best idea,
  re-homed onto JLS's actual hierarchy instead of a new IR.)
- **A deliberately-failing null test.** A knowingly-wrong binding that the harness
  *must* reject. An unfalsifiable parity harness is worse than none; see kill
  criterion K4.
- **A reflective guard** that no binding touches the event queue.
- **Declared refusals, by name** (`lf-02` §2.6): a `DelayGate` used as a delay
  line; a `TriState` whose behaviour depends on turn-off relative to turn-on; a
  level-sensitive `Memory` write (§8.4's glitch hazard is *a timing phenomenon*
  and quietly deleting it would teach that #199's bug does not exist); more than
  one incommensurable `Clock`; a block that does not settle. **Refusal, never
  silent degradation.**
- `docs/abstraction-levels.md`, **normative** (grafted from behavioral-first, and
  reframed per §0.2): the legitimacy test for behavioural abstraction, written as
  the **retroactive articulation of a rule the repo already follows**, applied
  element-by-element to `Adder`, `Memory`, `TruthTable`, `StateMachine`,
  `ShiftRegister`, `RegisterFile` and `FieldExtend` **first**, and only then to a
  CPU binding. Plus an abstraction banner on the outcome line and in the VCD
  header whenever any non-structural binding is active, and `--allow-fidelity SET`
  (default: everything allowed) so an instructor can require a lab to be drawn.

**Files.** `src/jls/elem/SubCircuit.java` (+1 saved attribute, +1 dispatch);
`docs/file-format.md` (+1 attribute — unknown attribute *names* are already
silently ignored, so old readers are unaffected); new `src/jls/sim/equiv/*`; new
`docs/abstraction-levels.md`; `docs/extension-points.md` (+1 row).

**Depends on.** L1 (the compiled binding wants plane arrays). **Blocks.** L5–L9.

---

### L5 — `jls.mach`: the machine model, in a pure leaf package

**Purpose.** Hold every line of architectural logic where it can carry the full
bar. **Forced by a verified constraint** (behavioral-first's best reasoning): there
is no `module-info.java` anywhere in `src/`, so in the unnamed module a permitted
subclass of a sealed type must sit in the same package as its parent — and
`pom.xml` sets `jls.elem`'s JaCoCo PACKAGE rule at 0.730/0.700/0.585 against
`jls.sim`'s 0.930/0.920/0.845. Putting ~3,000 lines of ISA logic behind an element
would hide it behind the weaker bar. Therefore the logic must not live in an
element — which is the split you would want anyway, because it makes the reference
runner the parity harness needs fall out for free.

**New mechanisms.** `ArchState` (record-shaped, `int`/`long`, no `BitSet`);
`MemoryView` with two indistinguishable implementations — `ArrayMemoryView`
(reference runner) and `BusMemoryView` (the fidelity binding) — which is what
makes the wired core and the reference runner *provably the same code*;
`rv32.Decode` (data-only table); `rv32.Rv32Model.step()` as a pure function;
`mach.dev.Uart16550Model`; `mach.dev.ClintModel` with `mtime` driven by
**simulated** time (`BRIEF` §3: running 100× slower raised instructions-to-login by
only 8% *provided this holds*). Zero AWT, zero `jls.sim`, zero `jls.elem`,
unit-testable with no simulator and no circuit. Added to
`HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` **as born-clean, in the same
commit that creates it** — that literal `Set.of()` is the only thing that polices a
package, and four proposals asserted AWT-freedom for packages the ratchet does not
look at.

**Files.** New `src/jls/mach/**`, `test/jls/mach/**`; `pom.xml` (+PACKAGE rule at
0.930/0.920/0.845, +PIT 80/82); `test/jls/HeadlessCoreRatchetTest.java`.

**Depends on.** L4 (it binds through the boundary), L0(b) (its cost model).

---

### L6 — The parity contract and its harness

**Purpose.** Make "the two tiers agree" a build failure rather than a claim, and
ship the machinery as a **student feature before any Linux work**.

**New mechanisms.** `jls.parity.RetireRecord` — the RVFI field list verbatim, as a
**Java record with no field for cycles, pipeline, or cache state**, so `BRIEF`
§6's *permitted to differ* set is enforced **negatively by the type** and
over-constraining parity becomes a compile error (behavioral-first's single best
idea, grafted). `RetireTrace` folded into the **existing, already-overridden**
`BatchSimulator.probeSample` hook, which is already fed from `WireNet.propagate`
and already descends subcircuits — an existing observation channel, not a parallel
one. `jls.parity.Differ` with first-divergence reporting, both records side by
side, and the differing fields named. `--rvfi FILE` and `--diff-against`.
`docs/parity-contract.md` (normative) and `test/parity/exclusions.txt`, ratcheted.

**The shipped student feature, before Linux:** *"JLS names the exact instruction
where your drawn CPU first disagreed with the reference, and prints both records."*
That is a better datapath assignment than anything in this software class, it falls
straight out of the trace machinery, and it justifies the program on its own.

**Files.** New `src/jls/parity/*`; `src/jls/sim/BatchSimulator.java` (~30 lines);
`src/jls/JLSStart.java`; `docs/batch-interface.md` (+§, additive, CHANGELOG);
new `docs/parity-contract.md`.

**Depends on.** L5, L4, L3.

---

### L7 — Virtual hardware and the front ends

**Purpose.** The moment the maintainer's sentence becomes true.

**New mechanisms.** `machines/soc-rv32.jls` — a drawn SoC of ~10 top-level boxes:
a `Cpu` **subcircuit** (definition initially a stub, binding behavioural),
`Memory`, `Console`, a CLINT subcircuit, address decode, `Clock`, watched pins. A
student opens it and sees a computer. GUI console pane bound to a `Console`
element's `PanelPort` through `InteractiveSimulator`'s existing Runner-thread/EDT
seam — per-keystroke `KeyListener` on the EDT into the ring, output batched at
~30 Hz via `invokeLater`, **never per signal** (`grand-architecture` §6's own
rule). **Two terminal modes**, grafted from structural-first: *block mode* is the
default and is the maintainer's own IBM citation answering the maintainer's own
latency problem — type the line locally at keyboard speed with local echo, submit
on Enter, ^C/^D as immediate sideband keys, pay one command latency per command;
*character mode* is honest pass-through for shell line editing. Block mode is
**presentation only** — the guest sees the same bytes at the same simulated times,
the transcript is identical, parity is unaffected. Headless `-console stdio` is
line-buffered by default and says so (the JVM cannot set raw mode without JNI);
`-console stdio,raw` shells out to `stty raw -echo` and degrades gracefully.
The free multipliers are taken: declare a 1 MHz clock, pin `lpj=`.

**Files.** `src/jls/edit/` (console panel — the only AWT in this architecture);
`src/jls/JLSStart.java`; `machines/`; new `docs/virtual-hardware.md`.

**Depends on.** L5, L6, L3.

---

### L8 — Virtual logic: the drawn machine, brought up boundary by boundary

**Purpose.** The legitimacy witness, the differential counterparty, and the
genuine first — no drawn logic simulator has ever booted an OS.

**The construction question, answered against the maintainer's own record.**
Minimal-intervention refuses a generator and hand-draws 600 elements; three judges
called that its weakest point, and `lf-07` is the maintainer's recorded
determination that the missing programmatic API is a real gap with
`riscv/jlsbuild.py` cited as the workaround that proves it. **Resolution: build no
bespoke forge.** The drawn machine is authored in the editor where structure is
pedagogically load-bearing (datapath, control, CSR block) and generated through
**`lf-07`'s `jls.api` `Edit` verbs — every one of which is a shipped
`jls.collab.op.CircuitOp`** — where it is mechanical repetition (bus fan-out,
decode tables, the CSR file). That contributes to a recorded program instead of
forking one, gets validation, atomic rejection, exact inverses and undo for free,
and **cannot construct a circuit the editor could not.** It is D5's "approach
reborn as a first-class in-tree tested mechanism", correctly routed.

**Bring-up is boundary by boundary, and this is the whole method.** Each of ALU,
RegFile, Decode, LSU, CSR, CLINT is a subcircuit with a fidelity boundary and a
behavioural binding from `jls.mach`. Each is checked by L4's harness at its own
boundary the day it is drawn. Then the CPU boundary itself. **At every commit,
whatever is drawn is checked.** This is HDL-single-source's homomorphic
contraction realized on JLS's actual hierarchy, and it removes the failure mode
every judge bet on — a year of work ending at the kernel decompressor with nothing
verified.

**Design levers, corrected for HEAD.** `RegisterFile` is one element (not 9 mirrored
`Memory`s, not 98 discretes). `Multiplier`/`Divider` as behavioural bindings of drawn
definitions rather than a 194-element combinational array — legitimate under L4 by
construction, and excludable per assignment via `--allow-fidelity`. Element count and
ev/instr are re-derived from L0, not inherited.

**Files.** `machines/cpu-rv32/**` (the definitions), `src/jls/api/**` (contributing
to `lf-07`), a nightly CI workflow with explicit `timeout-minutes`.

**Depends on.** L4, L6, L0(a).

---

### L9 — Mode C, the cycle engine (GATED; the only layer that touches #221)

**Purpose.** The last 3–20× for the structural tier — the difference between "run
it overnight" and "run it over lunch". Deliberately last, deliberately optional.

**This is `lf-02`'s Mode C, already specified in the recorded roadmap**, with a
measured basis (4.32 ns/node) the panel did not have. Opt-in per run, never the
default, behind a static classifier that **refuses** circuits where the weakening
is observable and falls back with a one-line notice, not an error — so a
first-year student drawing an adder never sees it. It requires a **named second
conformance level** in `docs/simulation-semantics.md` and an argued reopening of
#221; see §6.

**Depends on.** L1 exhausted and measured, L0(c), L6 as its equivalence oracle.

---

### Cross-cutting — the governance band (ships no runtime code)

New JaCoCo PACKAGE rules at 0.930/0.920/0.845 plus PIT 80/82 for `jls.mach`,
`jls.parity`, `jls.io`, `jls.sim.equiv`, `jls.api`; every new package prefix added
to `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` in the commit that creates it;
`SimulationBudgetRatchetTest`; `SessionBoundaryRatchetTest`; the exclusion-set
ratchet; **a determinism ratchet asserting no golden or VCD fixture may be
produced in live console mode** (the invariant all five proposals stated as
discipline and none enforced); `ExtensionPointCatalogTest` cross-checking the two
new rows in both directions; extension-point rows written **before** their
constants exist.

---

## 3. The parity contract, made concrete

### 3.1 Observation points — four, all of which already have a home

| # | Point | Mechanism | Granularity |
|---|---|---|---|
| 1 | **Fidelity boundary** | `jls.sim.equiv.BoundaryHarness` on a `SubCircuit`'s pins | settled output word per sampling instant, **indexed, not timestamped** |
| 2 | **Retirement** | six reserved-name nets sampled through the **existing** `BatchSimulator.probeSample` | one `RetireRecord` per retired instruction, monotonic, no gaps |
| 3 | **Guest output** | the `Console` element — **literally the same ~180 lines of Java in both tiers** | byte stream, in order |
| 4 | **Sync points** | `ArchState` digest through the binding's declared state map | per declared quantum and at exit |

### 3.2 The comparison alphabet

`jls.parity.RetireRecord`, the RVFI field list verbatim:
`{order, pc_before, pc_after, insn_word, rd_index, rd_value, mem_addr,
mem_rmask, mem_wmask, mem_wdata, privilege, trap}` — **and nothing else.** It is a
`record` with **no field for cycles, simulated time, pipeline state, or cache
state**, so `BRIEF` §6's *permitted to differ* set is unrepresentable rather than
merely documented. Trace files are untimestamped canonical text; the comparison is
`diff`.

### 3.3 MUST be identical / PERMITTED to differ

**Identical** (`BRIEF` §6, adopted verbatim): the ordered per-retirement record
sequence; full architectural state at each sync point minus `E`; the guest output
byte stream in order; trap occurrence and cause attributed to the causing
instruction; retired-instruction count between sync points.

**Permitted to differ**, each with industrial precedent: all timing (cycles,
simulated time, CPI, interrupt latency — six SystemC abstraction levels produced a
bit-identical memory image across a 2.8× spread in reported simulated time); all
microarchitectural state (gem5 *panics* rather than checkpoint classic caches and
explicitly flushes TLBs at handover); event ordering finer than the sync quantum.

### 3.4 The exclusion set `E`

`test/parity/exclusions.txt`, one line per excluded field **with a reason**,
initially `{mcycle, minstret, mtime, mtimecmp}`. **Ratcheted**: a test asserts its
exact content, so it may shrink freely and cannot grow without a diff a reviewer
sees plus a CHANGELOG entry. **`E` is printed in every parity report**, so an
exclusion is never silent policy hidden in comparison code.

### 3.5 The timer/CPI problem — named and solved, because three proposals fumbled it

Excluding `mtime` from the alphabet **does not exclude its effect**. A behavioural
binding retiring ~1 instruction per cycle and a drawn datapath at 2.9 both drive
CLINT `mtime` from simulated time, so the timer interrupt lands **between different
instructions**, injecting trap records at different retirement indices and changing
the subsequent pc stream. At the 1 MHz declared clock with HZ = 100 a tick lands
every ~10⁴ cycles — inside any handover window and inside any Linux-scale diff.

**Resolution, in two parts, adopted as normative:**

1. **The retirement-indexed trace diff is used only on interrupt-free, deterministic
   programs** — riscv-tests, directed programs, the fuzz corpus. There it is exact
   and cheap and it is where instruction-level bugs actually live.
2. **For interrupt-bearing and Linux-scale runs, the console byte stream is the
   parity clock** (minimal-intervention's recorded decision, grafted — the cheapest
   correct answer on the panel). The binding comparison is the guest output byte
   stream plus sync-point digests **computed by the guest** (`sha256sum` in the
   initramfs, arriving over the console), which needs no memory-introspection API,
   no checkpoint, and no new mechanism. The same expect-style script drives a
   3.5-minute behavioural boot and a multi-hour structural boot **because it is
   keyed on output bytes, not on time** — tier-independent by construction across a
   several-hundred-fold timing difference.

The rejected alternative is named: a hand-maintained per-instruction cycle-budget
table coupling the two models. It is the only *mechanical* fix on the panel and it
was honestly offered, but under bus factor 1 "real bug or stale budget entry?" is
exactly the ambiguity that erodes trust in a differential suite.

### 3.6 How it is checked by a test rather than by belief

| Tier | When | Content | Cost |
|---|---|---|---|
| **T-null** | every push | the deliberately-wrong fidelity binding the harness **must** reject | ms |
| **T0** | every push | `BoundaryHarness` on every fidelity binding (exhaustive ≤16 bits, else 10⁶ seeded + corners) | seconds |
| **T1** | every push | riscv-tests through both bindings, `--rvfi` files byte-identical; plus the salvaged `fuzz_diff` randomized differential | ~25 s structural + ~0.5 s behavioural, inside the 141 s gate |
| **T2** | nightly, `timeout-minutes` set | behavioural boot to prompt under `-console-script`, console byte stream diffed against a frozen golden | ~3.5 min |
| **T3** | release cadence, manual | structural boot under the **same script**, byte stream diffed against T2's | 2.4–9.7 h |

**T3 is not in CI and cannot be** (`BRIEF` §7: no lane can host a multi-hour run;
141 s required gate, 6-hour hosted ceiling). It is recorded in CHANGELOG with the
commit SHA. This is an accepted cost, stated in §7, not papered over — and note
that at central α a full structural boot **does not fit a hosted run at all**,
which invalidates the nightly and weekly structural lanes two proposals planned.

---

## 4. The live-interaction answer, with arithmetic

**Constants.** R = 2.3×10⁶ ev/s central (band 2.0–2.6×10⁶; `keystone-c` measures
3.1×10⁶ at 318 ns/event, carried as the optimistic edge). N = 4.0×10⁷ instructions
to a shell (**measured**, mini-rv32ima, RV32IMA nommu + busybox). L_echo = 10⁴–10⁵
instructions per echoed character. L_cmd = 10⁶–2×10⁷ instructions per busybox
fork+exec command. CPI 2.9 structural, ~1 behavioural.

### 4.1 Behavioural tier — the tier you type at

E_H ≈ 12 events/retired instruction (**modelled**; cross-checked by 1.8 ev/active
element × ~7 active elements ≈ 12.6; **measured by L0(b) before anything is built
on it**). Itemized: core step react (1) + address net → RAM/Console/decode (3) +
chip-select re-assert (3) + posted `MemoryRead` completing (1) + rdata back (1) +
~35% data access (~1) + store completion (~0.3) + next step post (1) = 11.3.

| Quantity | Today | After L1 (2–3×) |
|---|---|---|
| instructions/s | 2.3e6/12 = **191,700** (band 100k–325k) | 380k–575k |
| Boot to prompt | 4.0e7/191,700 = **209 s ≈ 3.5 min** (band 2.0–6.7 min) | **70–105 s** |
| Echo per character | 52–522 ms + poll granularity 52 ms + batching ≤33 ms = **140–610 ms** | **50–300 ms** |
| **`ls` (5e6–2e7 instr)** | **26–104 s** | **9–52 s** |
| Against `BRIEF` §5's budget | **12 ev/cycle vs 2–23 required — inside, with ~2× headroom** | — |

**This is the only claim in the entire panel that survives recomputation with
headroom.** Three proposals converged on it independently.

### 4.2 Structural tier — honestly

ev/instr = 1.8 · α · 600 · 2.9 = **564 / 1,253 / 1,754** at α = 0.18/0.40/0.56.

| Quantity | Today (central α) | After L1 (3–5×) | After L9 Mode C |
|---|---|---|---|
| instructions/s | **1,836** (band 1,311–4,080) | 5,500–9,200 | 7,200–49,000 |
| Headless boot | **6.1 h** (band 2.4–9.7 h) | 1.2–2.0 h | **14 min – 1.5 h** |
| Echo per character | **2.5–54 s** | 1.1–18 s | 0.2–14 s |
| `ls` | **4 min – 3 h** | 2–60 min | 20 s – 46 min |

**No arrangement of these numbers is a live console, and none is promised.** Mode
C's range is derived from `keystone-c`'s **measured** 4.32 ns/node over ~1,380
slots at 2 passes/cycle, with the two conservative halvings that document applies
itself (model→implementation, cache behaviour on a larger design) as the lower
bound and its activity-gated curve as the upper. Note the corroboration the panel
produced and discarded: structural-first's independent estimate (168 active
elements × 40 ns = 148,810 cycles/s) and HDL-single-source's (200 nodes × 32 ns =
150,000 cycles/s) agree with each other to 1% **and** with the activity-gated
measurement here. Three independent routes, one of them measured in-tree.

### 4.3 What replaces "live" on the structural tier

**Bounded handover windows**, grafted from THE BENCH and made cheap by L4. Toggle
the CPU boundary from behavioural to structural at a declared instant; only that
boundary's `ArchState` is mapped; `Memory`, `Console` and the bus are the same
objects in the same run; **no general checkpointing is required.**

10⁴ cycles × 386 ev/cycle / 2.3e6 = **1.7 s of wall clock today, 0.35–0.85 s after
L1, ~0.1 s under Mode C.** One second buys ten thousand real cycles of real drawn
logic with waveforms, starting from the exact architectural state your keystroke
produced. That is gem5's `--restore-with-cpu`, Simics hybrid mode, and ARM's
FVP-then-RTL flow — the industry's answer, not a consolation prize.

**Declared blind spot** (grafted from HDL-single-source, which alone stated it):
the structural side resumes with an empty event queue, so its first cycles are a
settling transient with no counterpart on the behavioural side. The handover
instant is **declared a sync point** and comparison resumes at the next
retirement. A bug whose only symptom lives in that transient is invisible to this
contract. gem5 has the same hole and flushes TLBs rather than pretend otherwise.

### 4.4 The sentence a user can say afterwards

*"I opened `soc-rv32.jls`, pressed Run, and about three and a half minutes later a
login prompt appeared. I typed `uname -a`; it came back in about half a minute. I
then set the CPU subcircuit's fidelity to structural, pressed Run, and went to
lunch; when I came back the same kernel had booted on drawn logic, and JLS had
proved that the two runs emitted the same bytes. When it disagreed on a test
program, it told me the instruction number."*

---

## 5. The gap list, ranked, each mapped to its closing layer

| # | Gap (all verified at HEAD) | Severity | Closed by |
|---|---|---|---|
| 1 | **No host I/O, no device concept.** Zero `System.in` in `src/`; two read-only text sites at `initSim` | fatal | **L3** |
| 2 | **Parity has no observation point.** No boundary concept, no fidelity toggle, no equivalence harness | fatal | **L4** |
| 3 | **No machine model.** No ISA model, no reference runner in-tree (`riscv_ref.py` is Python and is being deleted) | fatal | **L5** + L0 salvage |
| 4 | **No in-tree parity harness.** `fuzz_diff.py`/`verify.py` are the oracle stack `lf-02` §2.7 already relies on, and they are Python in a deleted directory | fatal | **L6** + L0 salvage |
| 5 | **~48% of loop time is queue/dedup, ~62% of allocation is `BitSet` churn, 82.3% of events model no elapsed time** | major, no decision needed | **L1** (`keystone-c` stage 1 / `lf-02` Mode T) |
| 6 | **Time limit 1e8 vs ~4e10 needed; `pause()` == `stop()`; one-shot batch** | major | **L2** |
| 7 | **`DENSE_CAPACITY_LIMIT` = 2²² is exactly 16 MiB, zero headroom against Linux's 12–16 MiB; `initSim` doubles heap** | major | **L2** |
| 8 | **Kernel image unshippable**: `Scanner`-per-line hex text, 33 MB as `.jls`, ~66 MB text alone exceeds the 64 MiB cap (D1) | major | **L2**, as a D3 optional section |
| 9 | **No programmatic construction API**; all six generative paths emit save text and re-parse it | major | **L8** via `lf-07` `jls.api` — *not* a new forge |
| 10 | **Structural tier is 17–190× outside the interactive budget** | major | **L9**, gated; partially **L1** |
| 11 | **No event-injection path** (`BRIEF` calls it fatal) | **not a gap** | dissolved in **L3**: a polled 16550 pulls; `Clock`'s self-scheduling idiom needs zero engine change |
| 12 | **`Element` is sealed** (`BRIEF` calls it fatal) | **not a gap** | dissolved in **L4**: no new permit is needed at all; and HEAD has already added two first-party permits |
| 13 | **No simulation-state serialization** | major | **deliberately not closed** — §7, with a re-entry trigger; boundary handover (L4) supplies what parity actually needs |
| 14 | **Golden oracle is 34 cycles, 4 assertions, RV32I-only** | major | **L6** (T0–T3 replaces it) |
| 15 | **Subcircuits are per-instance deep copies, sharing factor exactly 1.00×** | major | **not closed**; `lf-01`'s program owns it. L8 is sized assuming no sharing |
| 16 | **No CI lane can host a multi-hour run** | major | **not closed** — accepted cost, §7 |
| 17 | **Element census stale; no `Multiplier`/`Divider`** | moderate | **L8**, re-derived from L0; `RegisterFile` already lands the biggest lever |

---

## 6. Recorded decisions

### 6.1 #221 — the discrete-event interpreter is the sole simulation strategy: **NOT REOPENED by L0–L8**

One event loop, one `runEventLoop`, one queue, one post/react discipline,
per-element propagation delays intact. A fidelity binding is **one element with one
`react()` and one lumped propagation delay at its pins** — structurally
indistinguishable from `Adder` (`30 × bits`), `Memory` (`accessTime`),
`TruthTable`, `StateMachine` and `RegisterFile`, all of which already compute
arbitrary functions in one `react()` with no internal delays. JLS has shipped
lumped-delay behavioural abstraction of drawn logic since day one; L4 makes that
existing mixed abstraction **selectable at a boundary** and, for the first time,
**machine-checked**. L1's Mode T is likewise not a second strategy: it collapses
the zero-delay closure that `docs/simulation-semantics.md` §3's read-latest+FIFO
rule already defines topologically, and its acceptance gate is byte-identical VCD.

**But the recorded revisit trigger is now quantitatively met and instrumented for
the first time** (1,836 instr/s structural, 2.5–54 s per character, against a
≥10⁵ cycles/s need). Honesty and the recorded process therefore require **filing
the follow-up implementation issue that ARCHITECTURE.md says "deliberately does
not exist yet"**, with the measured constants attached, and **immediately
deferring it** — because ~48% of per-event cost is removable with no semantic
change and that budget must be spent and re-measured first. That is the exact
sequence #221's own text prescribes.

**L9 is the only layer that reopens it, and it reopens it as a weakening, argued
and documented — not as a reinterpretation.** The ritual, adopted verbatim from
The Drawn Machine, which had the best process on the panel:
1. File the follow-up issue **first**; the recorded process requires the issue
   before the work.
2. Present the trigger evidence as **measured on the real L8 machine**, not
   extrapolated from a demo that is being deleted, **including** the L1 result
   showing semantics-preserving constant-factor work was exhausted first.
3. Propose a **named second conformance level** in `docs/simulation-semantics.md`
   — *cycle-settled equivalence*: identical values at every clock-domain settling
   point and identical retirement traces, with **no** guarantee about intra-cycle
   observation order or per-element propagation delay. State plainly that this
   gives up #221's binding criterion by construction.
4. Bind the new strategy to **two** oracles: L6's differ and the #202 RV32I
   golden.
5. Ship it opt-in behind a static classifier that **refuses** circuits where the
   weakening is observable and falls back with a one-line notice, not an error.

**And record the counter-argument in the same issue:** the abstraction gap between
tiers is 42× in *events* (500 vs 12 ev/instr) but only 3–20× in *work* once
levelization deletes the per-event tax the structural machine pays 500 times and
the behavioural one pays 12 times. Levelization is worth 3–20×, not 42×, and it
costs the most expensive governance token in the repository. It may still be worth
it; the issue must say so with numbers.

### 6.2 `docs/vcd-interop.md:19-24` / #63 — live co-simulation: **REOPENED, NARROWLY**

The sentence fuses two claims. **Still rejected, unamended:** an external transport
driving JLS's event loop — cocotb, VPI, DPI, a socket, a subprocess, a stepping
protocol. Nothing here proposes any of it: there is no second simulator, no
transport, and no external semantic authority; `Console` is `Memory`'s init-file
category made bidirectional and in-process.

**Amended:** the grading sentence, replaced with —

> *Graders must not depend on interactive input. The supported grading surfaces
> are batch artifacts: exit status, stdout report, VCD, the retirement trace, and
> a recorded console transcript. **An interactive session is a recording device;
> the recording, not the session, is the contract.** Replay of a transcript is
> deterministic, threadless, byte-reproducible and CI-runnable.*

Today "don't interact" is enforced by there being no way to. Afterwards it is
enforced by a replay test — which is stronger, not weaker.

**Process:** a decision issue against #63/#216 quoting both sentences; **edit** the
vcd-interop.md section rather than leave the contradiction standing; CHANGELOG
entry; ARCHITECTURE.md decision block. `examples/autograde/` and
`AutogradeBridgeExampleTest` are untouched.

### 6.3 #223 — extension-point catalog: **HONORED**, two rows, written before their constants

| Seam | Point id | Contract | Home | Cardinality | Lifecycle | Status |
|---|---|---|---|---|---|---|
| Fidelity binding | `elem.fidelity-binding` | `jls.sim.equiv.FidelityBinding` | `jls.sim.equiv` | many | resolve at elaboration, before `initSim` | typed with L4 |
| Device host port | `elem.host-port` | `jls.io.HostBytePort` (**sealed**) | `jls.io` | one active per device instance | invocation-time grant, before `initSim` | typed with L3 |

Both pinned by `ExtensionPointCatalogTest` in both directions. **Deliberately not
added:** no `sim.execution-strategy` seam (§6 and #221 both say a second strategy
is core-internal with zero plugin indirection); no `mach.*` seam (the machine model
is compiled-in and closed); no new `elem.*` seam for `Console`, which is a
contribution to the existing `elem.element-provider` row. Fewer seams is better
governance. The pending `hdl.importer` row is **not** filled — this program touches
no HDL.

### 6.4 Respected without amendment

- **`grand-architecture` §6 hot/cold plane** — extended with a *testable* rule
  (`SessionBoundaryRatchetTest`) rather than merely obeyed. The one hot-side seam
  is polled on a declared ~10⁴-cycle interval and output is batched at ~30 Hz.
- **`grand-architecture` §9 exclusions** — nothing re-proposed. No central store,
  no in-house HDL simulation or parsing, no language rewrite, no server, no plugin
  execution surface ahead of demand.
- **#222 plugin trust boundary** — *extended*, not reopened: host I/O is a new
  authority class and `Stop.react` calling `sim.stop()` is not precedent for it.
  A loaded `.jls` never acquires host I/O; the contract is **sealed** so #212's
  external-provider gate structurally cannot reach it; grants are invocation-time
  and named on the outcome line. `UntrustedFileHardeningTest` already treats `.jls`
  as untrusted input and ambient host I/O would regress that. Recorded as an
  extension of #222's threat model, in #222's shared vocabulary.
- **#181 stable-order determinism** — *strengthened*: the invariant becomes "a pure
  function of circuit content **and the transcript**", enforced by a replay test
  and by the live-mode golden ratchet.
- **#165 stable ids / #166 canonical save / #167 op layer** — consumed, not
  changed. The transcript is the op vocabulary generalized.
- **#77 headless core** — every new package prefix is added to
  `CORE_PACKAGE_PREFIXES` in the commit that creates it. Only the console panel
  lives in `jls.edit`.
- **`docs/batch-interface.md`** — additive flags only, each with a CHANGELOG entry
  per its own §6 rule. No existing flag, stdout line, exit code or VCD field
  changes; the abstraction banner appears only when a non-structural binding is
  active, so no existing circuit's output changes by one byte.
- **D1/D2/D3** — the binary image, and any future checkpoint, are **optional,
  independently versioned sections with must-understand semantics**, not private
  sidecar formats. Structural content stays uncompressed, canonical and diffable.
- **D4** — adopted as the keystone.
- **D5** — honored: `riscv/` deleted; its *evidence* transcribed to
  `docs/machine-calibration.md`; its *construction approach* routed to `lf-07`'s
  `jls.api`; its *differential harness design* reborn as L6. Its two tracked
  dependents (`RiscvCpuGoldenTest` + fixture, `keystone-c`'s benchmark anchor)
  re-homed **before** deletion.

---

## 7. What is deliberately excluded

*In the idiom of `grand-architecture` §9. Each exclusion states its price and, where
one exists, its re-entry trigger — the discipline grafted from Two Elements and a
Port's §8, which is the best bus-factor-1 governance artifact on the panel.*

1. **A live console on drawn logic.** Not slow — impossible inside this event
   model. 17–190× outside the measured budget. **No re-entry trigger.** Anyone who
   wants a native-feel shell wants QEMU and should use QEMU.
2. **General simulation-state checkpoint/restore.** A permanent per-element
   serialization tax on the whole element library, ~6 weeks minimum, in service of
   re-running a boot. Boundary handover (L4) supplies what parity actually needs.
   **Re-entry trigger:** three consecutive structural expeditions failing past the
   one-hour mark, **or** a CI plan that would re-boot the behavioural tier more
   than once per lane. When it comes it arrives as a D3 optional section, not a
   fourth sidecar format.
3. **A machine-description IR or DSL.** A new language in a bus-factor-1 project.
   The circuit is the description; `lf-07`'s `Edit` verbs are the construction API.
4. **HDL as the source of truth.** RTLIL's word-level cell set and JLS's element
   set do not span each other; the decisive levers are *idioms*, not cell maps; and
   a flagship whose source of truth requires an out-of-jar Yosys rots. `lf-02`
   §9's exclusion stands.
5. **DMI / temporal-decoupling fast-forward.** At a large quantum the drawn bus
   shows a sampled fiction and the only mitigation is a warning banner — social,
   not structural. Cut entirely; no arithmetic here depends on it.
6. **A `Cpu` element in the sealed permits list.** L4 makes it unnecessary. Exactly
   one new element ships: `Console`.
7. **Any second execution strategy in L0–L8.** L9 only, gated, argued, opt-in.
8. **Committed guest images, committed multi-MB checkpoints, Git LFS.** The jar
   stays self-contained and offline; only the user's payload comes from outside,
   via a pinned, documented, checksummed build recipe.
9. **A server, a network dependency, an install step, a plugin execution surface
   ahead of demand.**
10. **Per-class coverage exemptions.** None requested, none available.
11. **A CI lane for the structural boot.** No lane can host it (141 s gate, 6-hour
    ceiling, and at central α the boot exceeds the ceiling outright). T3 is a
    release-cadence expedition recorded in CHANGELOG with its SHA. **Accepted
    cost, stated plainly:** the headline result is verified by a human, in a
    project whose constraints demand test-enforced checks. Mitigated, not solved,
    by T0/T1 checking both bindings on every push.

---

## 8. The dependency spine, and the first three merges

```
L0 measurement gate ─┬─> L1 engine constants ─┬─> L2 capacity ──> L3 host boundary ─┐
                     │                        │                                     │
                     └────────────────────────┴──> L4 FIDELITY BOUNDARY <───────────┘
                                                        │
                                        ┌───────────────┼────────────────┐
                                        v               v                v
                                    L5 jls.mach ──> L6 parity ──> L7 virtual hardware
                                                        │
                                                        └──────> L8 virtual logic
                                                                      │
                                                                      v
                                                              L9 Mode C (gated)
```

**The first three merges, in order.**

**M1 — the measurement gate and the salvage (~3 weeks, no product code).**
The 2-cycle unified-memory machine measuring α/CPI/k; the 200-line behavioural
accumulator behind a boundary measuring ev/instruction; `keystone-c`'s
`Levelized.java` re-run at ~1,400 slots; the `now > maxTime` adjudication;
`docs/machine-calibration.md`; `riscv/`'s two tracked dependents re-homed and its
differential-harness design written into `docs/parity-contract.md` §0; **then**
`riscv/` deleted. Nothing in this program is costed until M1 lands, because every
wall-clock number divides by one of its outputs. *Four proposals scheduled this
experiment after committing to an architecture; one re-homed it inside an 8–12
week refactor. It is days of work and it is commit #1.*

**M2 — `Console`, `HostBytePort`, transcript, and the vcd-interop amendment
(~3 weeks).** A drawn FSM prints. A GUI session records and replays in batch
byte-identically, which finally joins JLS's two front ends. Immediately useful to
a first-year lab; contains no RISC-V, no Linux, and no parity machinery. Ships the
only recorded-decision amendment in the program, early, where it can be argued on
its own merits rather than under schedule pressure.

**M3 — the fidelity boundary, its harness, its null test, and
`docs/abstraction-levels.md` (~5 weeks).** Demonstrated on an **ALU subcircuit**:
drawn versus a compiled binding, held equal by `BoundaryHarness`, with the
deliberately-wrong binding failing in CI. Zero RISC-V. This is the keystone, it is
provable at student scale, and it lands the maintainer's own D4 as a mechanism.
*If the program stops here, JLS has a console, deterministic replay, a normative
abstraction-level policy applied retroactively to seven shipped elements, and a
machine-checked fidelity toggle — all independently valuable and none of it
wreckage.*

Thereafter: **M4** `jls.mach` + riscv-tests (~4 months, the dominant cost, driven
by PIT 80/82 on CSR/trap corners); **M5** retire trace + `--diff-against` shipped
as a student feature (~3 weeks); **M6** the behavioural SoC boots Linux with a GUI
console — **the sentence becomes true** (~3 weeks); **M7** the drawn machine
brought up boundary by boundary (~3–5 months); **M8** the structural headless boot
and T3 (~2 weeks + wall clock); **M9** Mode C, gated.

**Honest total: ~6–8 months to the maintainer's sentence, 14–20 months to the
structural boot**, at a single maintainer's realistic cadence, with L1 landing
somewhere in M1–M4 as its own track because it is independently valuable to every
student today and needs no architecture at all.

---

## 9. Kill criteria

*Numeric, measured at named milestones, each with a stated consequence. If one
fires, the consequence is taken — not argued with.*

**K1 — behavioural events per retired instruction (M1, spike b).** Modelled 12.
- **> 25:** boot > 7 min, echo > 1 s, `ls` > 3.5 min. The word "live" is retired
  from all documentation; the claim becomes "responsive-ish, one command per
  minute". Program continues.
- **> 46** (2× the 23 ev/cycle budget ceiling): **stop the live-console claim
  entirely.** Ship headless + transcript + replay. L7's GUI console panel is cut.

**K2 — α (M1, spike a).** Band 0.18/0.40/0.56.
- **α ≥ 0.56 and L1 delivers < 2.0×:** structural boot ≥ 9.7 h. It fits no CI lane
  under any arrangement; the structural deliverable is restated as a
  release-cadence expedition and the nightly structural lane is never built.
- **α such that the boot exceeds 12 h:** cut the full structural boot claim. The
  structural deliverable becomes riscv-tests parity + bounded handover windows,
  and "no drawn logic simulator has booted an OS" stays true of JLS too. Say so.

**K3 — L1's acceptance gate.** If byte-identical VCD and stdout cannot be achieved
across the **entire** existing golden corpus, **L1 stops at the failing change.**
No semantic change is permitted to buy speed. There is no partial credit here.

**K4 — the null test (M3).** If the deliberately-wrong fidelity binding is **not**
rejected by `BoundaryHarness`, **L4 stops.** An unfalsifiable parity harness is
worse than no harness, because it converts an unchecked claim into a checked-looking
one. Nothing downstream of L4 may merge until the null test fails on demand.

**K5 — `jls.mach` under the coverage bar (M4).** Budget 4 months.
- **> 8 months without reaching 93.0/92.0/84.5 as a PACKAGE aggregate plus PIT
  80/82:** cut the model to RV32I + Zicsr + M-mode only, **abandon the Linux
  target**, and promote the in-jar M-mode self-checking payload to *the* parity
  workload. The architecture survives this intact; the flagship demo does not.

**K6 — drawn-machine authoring (M7).** **More than three ground-up revisions, or
more than 16 weeks of authoring**, and further hand-drawing stops: `lf-07`'s
`jls.api` generation is escalated ahead of it, with an issue.

**K7 — Mode C's gate (before L9 is scheduled).** `keystone-c` measured 4.32 ns/node
at 522 slots.
- **A real implementation at ~1,400 slots measuring worse than 15 ns/node:** Mode C
  yields < 3× over L1. **Do not build it. #221 is not reopened.** The structural
  tier's speed is whatever L1 made it, and that is the end of the matter.

**K8 — the guest image (continuous).** If the pinned kernel + initramfs cannot be
rebuilt from its documented recipe **by the maintainer alone in under 2 hours** at
any point — RV32 nommu removal, toolchain drift, `CONFIG_NONPORTABLE` withdrawal —
the Linux target is demoted below the in-jar M-mode payload and the demo is
restated. A published removal proposal targets "the beginning of 2027", **inside
this program's window**; the kernel version is pinned as a documented artifact from
day one.

**K9 — the pedagogy floor (continuous, and it outranks everything above).** GUI
startup time, per-edit cost (58 ms @10k elements, 552 ms @100k today) and
palette size are ratcheted. **Any regression to the first-year student drawing an
adder stops the responsible layer**, regardless of what it costs the flagship. The
pedagogy audience is the product; the Linux boot is a demonstration.
