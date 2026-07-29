# Machine calibration: what a CPU-scale JLS circuit costs

**Status: normative evidence.** This document is not a plan and it authorizes
nothing. It records **measurements** — engine throughput, per-element event
cost, the boot-cost model and its inputs, and the guest-side facts a
Linux-capable machine has to satisfy — so that they survive the deletion of
the directory most of them were taken on. Where a number is an estimate it
says so, with its method. Where two sources disagree, both are printed and
the disagreement is named rather than averaged away.

Every claim about the tree carries a `file` / method anchor and was verified
at HEAD. Every performance number carries its workload, its date and its
hardware. Numbers that are *derived* carry the arithmetic that derives them,
so a future reader can re-derive them against a different constant instead of
having to trust this one.

Sibling documents:

- [`simulation-semantics.md`](simulation-semantics.md) is normative for what
  a simulation *means*. Nothing here changes it; section 3.4 records two
  shipped elements it does not yet describe.
- [`parity-contract.md`](parity-contract.md) is normative for what it means
  for two implementations of one boundary to agree. This document supplies
  the cost model that decides which comparisons are affordable; it does not
  define the comparison.
- [`capability-roadmap/keystone-c-performance.md`](capability-roadmap/keystone-c-performance.md)
  is the primary source for section 2. This document is its durable
  distillation plus the corrections found while re-deriving it.

---

## 1. Why this document exists

### 1.1 The evidence is about to lose its home

`riscv/` is remnant work and is being removed. It is also the **only
CPU-scale JLS circuit that has ever been measured**: a single-cycle RV32I
datapath, 225 logic elements, that runs real programs and passes a
differential oracle. Every throughput number in
[`capability-roadmap/keystone-c-performance.md`](capability-roadmap/keystone-c-performance.md)
was taken on a circuit built by `riscv/build_cpu.py`, and
`ARCHITECTURE.md`'s recorded revisit trigger for issue #221 names "a concrete
CPU-scale design on the `riscv/` trajectory" — a trajectory whose directory
is being deleted.

Deleting the files is the right call and this document does not contest it.
But the measurements are not remnant work, and if they are not written down
outside `riscv/` they are gone: the anchor circuit is untracked, the
generator that produces it is inside the doomed directory, and the numbers
appear nowhere else in the tree with their method attached.

**After deletion, this document is the record.** It is written to stand alone.

### 1.2 What must be re-homed *before* the deletion commit

These are preconditions, not follow-ups. Each is tracked (or, in one case,
conspicuously not), and each has a distinct job.

| Asset | Tracked? | What it is for | What must happen first |
|---|---|---|---|
| `test/jls/RiscvCpuGoldenTest.java` | **yes** (`git ls-files`) | The RV32I integration golden. `ARCHITECTURE.md`'s #221 equivalence criterion binds any future evaluation strategy to agreeing with it bit-for-bit, so it is the oracle that makes #221 enforceable | Its javadoc cites `riscv/examples/sum1to10.s` and `riscv/README.md` as the regeneration path. Both are deleted. Re-home the regeneration recipe (the `.s` source and its assembly/clock-vector procedure) into `test/fixtures/` or this document, **and fix the javadoc** — the references are `{@code}` spans, not `{@link}`, so the `-Werror` doclint gate will not catch the rot |
| `test/fixtures/riscv-sum1to10.jls` | **yes** — `.gitignore` ignores `*.jls` but explicitly exempts `test/fixtures/**/*.jls` | The golden's circuit | Nothing to move; but it becomes unregenerable when its source leaves |
| `riscv/build/k2000.jls` | **NO** — `riscv/.gitignore` line 1 is `build/` | The performance anchor for every number in section 2 | It cannot be "re-homed" because it was never tracked. It must be **regenerated and committed** as a fixture before `bench_kernel.py` is deleted, or section 2 becomes permanently unreproducible |
| `riscv/bench_kernel.py` | yes | Generates the sized benchmark circuits (`k500`/`k1000`/`k2000`) and their `-t` clock vectors | Its *output* must be committed (row above). Its *method* is recorded in section 7 |
| `riscv/riscv_ref.py`, `riscv/fuzz_diff.py`, `riscv/verify.py` | yes | The differential harness: a 975-line RV32I reference emulator, a randomized differential runner that requires identical final architectural state, and 11 directed programs | **The design survives; the code does not.** The design is what [`parity-contract.md`](parity-contract.md) formalizes. Transcribe the limitation with it: `riscv_ref.py` was written by the same author as the design under test, so it is a self-consistency oracle, not an independent one |
| `riscv/gui/cpu.jls` | **yes** — one of only four tracked `.jls` files in the repository | Used as a real-world fixture for measured save-format churn | Decide explicitly whether it is kept as a fixture or lost. It is not replaceable by a synthetic circuit for that purpose |

Two further notes on the deletion:

- `riscv/README.md` records that `lb`/`lh`/`lbu`/`lhu`/`sb`/`sh` are
  unimplemented **because 32-bit `Memory` has no byte lanes**
  ([`capability-roadmap/README.md:88-90`](capability-roadmap/README.md)).
  That is a hard blocker for any Linux-capable machine — the minimum SoC's
  UART is three *byte* addresses on a 32-bit bus (section 5.3) — and it is
  currently recorded only inside the directory being deleted. It is restated
  in section 6 so it survives.
- `ARCHITECTURE.md`'s #221 revisit trigger will, after deletion, name a
  directory that does not exist. Restating it quantitatively against the
  constants in section 2 is the cheap fix; `keystone-c` proposes "below 10
  kcycles/s on the #202 golden's CPU."

### 1.3 What this document deliberately does not do

It does not price any future work, recommend an architecture, or claim that
any of the machines in section 4's table can be built. It records what things
cost when measured and what remains unmeasured.

---

## 2. Measured engine constants

### 2.1 The workload

All of section 2 is one workload unless stated otherwise.

| Property | Value |
|---|---|
| Circuit | `riscv/build/k2000.jls` — a single-cycle RV32I CPU running a sum-1..N loop, built by `riscv/build_cpu.py` |
| Census (`jls.sim.Census`) | **225 logic elements, 297 wire nets**, flat (no subcircuits), every width ≤ 32 bits; 1,551 total runtime elements including 810 `WireEnd` and 513 `Wire` |
| Element mix | `Constant` 43, `Mux` 43, `AndGate` 34, `Splitter` 34, `Register` 32, `Binder` 9, `NotGate` 8, `Extend` 5, `XorGate` 5, `Adder` 4, `Memory` 3, `OrGate` 3, `ShiftRegister` 3, `Decoder` 1, `InputPin` 1 |
| Run length | **6,004 clocked cycles**, driven by a `-t` stimulus vector (193 KB) |
| Oracle state | `riscv/verify.py` passed 11/11 on the same jar before any measurement |
| Date | 2026-07 |
| Hardware | Linux 6.18.5 x86-64 container, default heap, `jls-5.0.5-SNAPSHOT.jar` |
| Method | In-process harnesses in the `jls.sim` package (to reach the protected loop), JIT-warm, best of 8 reps |

**Absolute nanoseconds are machine-specific. Ratios are robust.** Anyone
re-measuring on other hardware should expect the shares in section 2.3 to
hold and the ns figures to move.

### 2.2 The four raw quantities

Everything else in this section is arithmetic over these:

```
events fired            2,331,793      (jls.sim.KernelProbe)
runEventLoop            0.742 s        (jls.sim.KernelProbe2, best of 8)
initSimulation          0.568 s        (same)
clocked cycles          6,004
```

Derived, with the division shown:

| Quantity | Value | Derivation |
|---|---|---|
| Events per second, **warm event loop** | **3.14 × 10⁶ /s** | 2,331,793 / 0.742 |
| Nanoseconds per event, warm loop | **318 ns** | 0.742 / 2,331,793 |
| Events per second, **including `initSimulation`** | **1.78 × 10⁶ /s** | 2,331,793 / (0.568 + 0.742) |
| Events per clocked cycle | **388.4** | 2,331,793 / 6,004 — see the caveat in 2.5 |
| Simulated cycles per second, warm loop | **8,092 /s** | 6,004 / 0.742 |
| Simulated cycles per second, incl. init | **≈ 4,600 /s** | recorded by `keystone-c` |

**Always say which scope a throughput figure uses.** The two differ by 1.76×
on this workload and confusing them has produced wrong conclusions twice in
this study's own history.

A separate, older set of six probes over six *different* circuits at mixed
measurement scopes gives a band of **2.0–2.6 × 10⁶ events/s**. That band and
the 3.14 × 10⁶ figure are not the same measurement and neither supersedes the
other: the band is cross-circuit, the 3.14 figure is one circuit warm-loop
only. It is **not** true that the band is simply "the including-init figure" —
on this workload that figure is 1.78 × 10⁶, below the band's floor.

> **Do not quote** `keystone-c-performance.md:140-141`'s "≈1,100–1,450
> cycles/s end-to-end from the CLI." Its own stated inputs (4.15 s at 3,004
> cycles, 5.63 s at 6,004) give 724 and 1,066 cycles/s. The range is not
> reproducible from the numbers printed beside it. A separate six-probe
> end-to-end CLI figure of 914–2,900 cycles/s comes from different probes and
> is unaffected.

### 2.3 Where the 318 ns goes

JFR execution sampling (`stackdepth=512` — without it ~30% of samples are
truncated and mis-attributed), 12 reps, 973 samples attributed inside the
event loop:

| Share | Subsystem | ns of 318 |
|---:|---|---:|
| 37.6% | `BitSet` value work — `clone`, `and`/`or`/`set`, `equals`/`ArraysSupport.mismatch`, `hashCode`, `trimToSize`, `recalculateWordsInUse`, `BitSetUtils.ToInt`/`ToLong` | 119.6 |
| 17.1% | `eventQueue.poll()` — `siftDownComparable` + `SimEvent.compareTo` | 54.4 |
| 15.3% | `post()` → `dupCheck.add` — `HashSet.add` → `HashMap.putVal`, dragging `SimEvent.hashCode` → `BitSet.hashCode` | 48.7 |
| 10.1% | `dupCheck.remove` (`src/jls/sim/Simulator.java:225`) | 32.1 |
| 5.2% | `eventQueue.add` — `siftUpComparable` | 16.5 |
| **4.9%** | **element `react()` bodies' own code** | **15.6** |
| 1.5% | loop scaffolding | 4.8 |

Grouped: **value representation 37.6%, event-queue bookkeeping 47.7% (151.8
ns of 318), actual logic 4.9%.**

The structural fact worth carrying forward: JLS spends roughly twenty times
as long deciding where an event goes in a priority queue, whether it is a
duplicate, and copying `BitSet`s defensively, as it spends computing what a
gate outputs. The engine at HEAD is `PriorityQueue` plus a `HashSet`
`dupCheck` (`src/jls/sim/Simulator.java:25,27`).

**Allocation**, same run (JFR `ObjectAllocationSample`): `byte[]` dominates —
that is `SigSim`'s quadratic string concatenation during stimulus setup, an
ordinary bug and not a property of the engine — then `long[]` +
`java.util.BitSet` (592 samples combined, the value clones), `jls.sim.SimEvent`
(252), `HashMap$Node` (164), `SimEvent$PinChanged` (87), and
`LinkedHashMap$LinkedKeyIterator` (83). 82 young GCs across 12 reps.

Two allocations are pure waste visible from the class name alone:
`SimEvent.PinChanged` is a **zero-field record** allocated fresh for every
sink of every propagate (1.92 M times in this run), and every `BitSet` clone
on this workload carries at most 32 bits in a heap object.

### 2.4 Queue depth, and what circuit size actually costs

Isolated `PriorityQueue` microbenchmark, poll+add at fixed depth:

| Depth | poll+add | + `HashSet` dedup |
|---:|---:|---:|
| 64 | 82.32 ns | 116.64 ns |
| 512 | 116.89 ns | 173.75 ns |
| 4,096 | 140.93 ns | 200.75 ns |
| 12,000 | 141.91 ns | 212.30 ns |

Queue depth costs about **60 ns/event between a 64-deep and a 12,000-deep
heap — a 1.7× range across a 190× depth range**. It is log-shaped and heavily
damped.

An earlier fit over circuit size reported **R ≈ R₀ · (L/228)^−0.12**. Treat
that exponent as *the shape of the table above*, not as an independent law,
and do not extrapolate it:

- The measured max queue depth on this workload was **12,093 events**, and it
  has nothing to do with the circuit. `SigSim.initSim` posts *every* stimulus
  transition during elaboration (`src/jls/elem/SigSim.java:129` and `:193-197`,
  both inside `initSim`), so the entire `-t` vector sits in the priority queue
  for the whole run.
- Therefore most of the measured depth tax was **stimulus, not design**. Once
  a vector is streamed rather than pre-posted, throughput becomes *more* flat
  in circuit size, not less.

**The honest statement: engine throughput is flat in circuit size, with a
bounded ~1.7× worst-case queue-depth tax that is currently paid for harness
reasons.** The practical consequence is unchanged and is the important part —
**element count enters boot time once, through events per instruction, not
twice.**

### 2.5 Events per clocked cycle — the number to be most careful with

388.4 events/cycle is a clean division of two measured quantities on this
workload. It is also **the least transferable number in this document**, for
one specific reason.

`k2000.jls` is driven by a `-t` stimulus vector — a `SigSim`/TestGen clock.
A separate pair of measurements on **element-for-element identical circuits**
found **245.5 events/cycle under TestGen drive and 121.5 under an internal
`Clock` element**, a 2.02× discrepancy that has never been explained. A third
figure of 243.1 for the self-clocked case appears in the same source document
as the 121.5, and the two contradict each other. Across all three, the spread
against 388.4 is about **3.2×**.

This matters because a boot cannot use TestGen drive at all: `SigSim.initSim`
pre-posts every transition, so a multi-billion-cycle stimulus vector would
have to fit in the event queue before the first event fires. **A boot must be
driven by an internal `Clock`** — which is precisely the regime in which
events/cycle was measured *lowest* and least reliably.

**No boot wall-clock figure may be quoted without naming the events-per-cycle
constant it used and the clocking regime that constant was measured in.**
Settling this is the cheapest high-value experiment available (section 6.2).

### 2.6 The levelized model (not an engine; a measurement of one)

`keystone-c` also modelled a levelized evaluation pass over this CPU's real
shape — **522 evaluation slots** (225 logic elements + 297 nets), the real
width mix, a topologically ordered DAG, a realistic opcode mix, full 3-plane
4-state arithmetic including the X-poison rule for `ADD`:

```
522 nodes, plane arrays (long[] a, b, u) : 2.26 µs/pass  → 4.32 ns/node
522 nodes, BitSet[]                      : 11.49 µs/pass → 22.01 ns/node
```

**5.1×.** Levelizing while keeping `BitSet` as the signal type throws away
four fifths of the win, and the reason is structural: a levelized pass wants
state in flat arrays indexed by node id, touched by straight-line `long`
operations. `BitSet` cannot be that.

Activity-gated variants of the same pass:

| Live nodes | µs/pass |
|---:|---:|
| 522 (100%) | 1.62 |
| 252 (50%) | 1.33 |
| 123 (25%) | 0.89 |
| 46 (10%) | 0.68 |

**Three rules for anyone quoting a ns/node figure.** They exist because two
agents in this study's own history got this wrong, one by 4.6×:

1. **State the node count.** 522 nodes = 225 logic elements + 297 nets, i.e.
   **2.32 nodes per logic element**. A ~580-element machine is ~1,346 nodes,
   not 580.
2. **State the pass count.** A levelized design needs **two passes per cycle**
   (one per clock edge; a levelized DAG converges in one pass by
   construction). 2 × 1,346 × 4.32 ns = 11.63 µs/cycle = **~86,000 cycles/s**.
3. **State which per-node cost, because the source prints two.** 4.32 ns/node
   is the 2.26 µs/pass figure; the activity table's 100% row is 1.62 µs/pass
   = **3.10 ns/node** for the same 522 nodes, and the source never reconciles
   them. At 3.10 the ceiling above would be ~119,800 cycles/s.

**~86,000 cycles/s is an underated ceiling, not a target.** `keystone-c`
derates it twice on its own account — halve for model-versus-implementation,
halve again for cache behavior on a design several times larger — reaching
**22,000–40,000 cycles/s**. Quote the derated range; quote the ceiling only
as a ceiling.

---

## 3. The element-cost table

**This is the most reusable measurement in this study.** It tells a future
machine-builder what a design choice costs in the currency the engine
actually charges in: events.

### 3.1 Method

Four fixtures, identical stimulus harness in all four: a `Clock`, a 5-bit
address counter (`Register` + `Adder` + `Constant`), a 32-bit data counter,
one 1-bit `Constant`. 1,000 clock cycles at a 100-unit period. Events counted
by a `BatchSimulator` subclass overriding `afterEvent` — the same observation
seam `BatchSimulator` already uses (`src/jls/sim/BatchSimulator.java:140`,
overriding the no-op at `src/jls/sim/Simulator.java:269`), so the counter
needs no change to `jls.sim`. Element census counts non-`Wire`,
non-`WireEnd` elements. Date 2026-07, same hardware as section 2.

Each row's Δ columns are against the `rf_none` harness-only baseline. **The Δ
is the measurement; the absolute is context.**

### 3.2 Register file, three ways

| Fixture | Logic elems | ev/cycle | Δ elems | **Δ ev/cycle** |
|---|---:|---:|---:|---:|
| harness only | 10 | 14.01 | — | — |
| **1 native `RegisterFile(32×32, 2R1W)`** + 2 `OutputPin` | 13 | 20.95 | +3 | **+6.94** |
| 2 mirrored `Memory(32×32, sync)` + zero-`Constant` + 2 `OutputPin` | 15 | 32.01 | +5 | **+18.00** |
| 31 `Register` + 31 hold-`Mux` + 31 `AndGate` + `Decoder` + `Splitter` + 2× 32:1 `Mux` + 2 zero-`Constant` + 2 `OutputPin` | 111 | 128.54 | +101 | **+114.53** |

**A 2.6× event spread and a 34× element spread for one architectural
component.** The native `RegisterFile` (shipped in #201,
`src/jls/elem/RegisterFile.java`) has independent read and write address
ports, so the one-address-port constraint that used to force the mirrored
design is gone. Any older document describing "9 mirrored `Memory` elements
versus a 98-element flip-flop farm" as a live design choice is describing a
choice that no longer exists.

### 3.3 The cause, and the general rule it teaches

The mirrored-`Memory` row costs 2.6× the native row because of one line of
behavior: **`Memory.react` posts a self-event at `now + accessTime` on *every*
inbound `PinChanged`** — a `MemoryWrite` on the gated write path
(`src/jls/elem/Memory.java:1384-1386`), then unconditionally either a
`MemoryRead` (`:1393-1397`) or a `TriStateOff` (`:1400-1403`). Every path
through the `PinChanged` case emits at least one event. Two mirrored memories
with six inputs each therefore pay roughly two events per input change, twice
over.

Note also that `Memory` reads are **asynchronous**: an address change posts a
read at `now + accessTime` with **no clock consulted**. This is why a fetch is
one cycle and a load is one cycle, and it is the reason the CPI estimate in
section 4 is ~2.9 rather than the ~6.6 an earlier synchronous-read assumption
produced.

**The rule for a machine-builder: count an element's cost in *posted events
per input change*, not in gates.** An element that self-posts on every
inbound change is expensive at any size; an element that suppresses unchanged
outputs is cheap at any size.

### 3.4 Event cost is flat in depth and width

| Fixture | ev/cycle |
|---|---:|
| `RegisterFile` 32 words × 32 bits | 20.95 |
| `RegisterFile` 1,024 × 64 | 21.01 |
| `RegisterFile` 65,536 × 32 | 21.01 |

A 64Ki-word register file costs the same events as a 32-word one. **Event
cost is a function of port count only.** This is the word-level property of
JLS taken one step further than it is usually stated: a 32-bit `Adder`, a
32-input `Mux`, a `Register` and a 256×32 `Memory` are each one element with
one `react()`, and *size within an element is free at the event level*. It is
not free in heap.

### 3.5 Three constraints on these two elements that a machine-builder must know

Verified at HEAD, and each is a live gap rather than a design intent:

1. **Neither `RegisterFile` nor `FieldExtend` is HDL-exportable.**
   `HdlExporter.EXPORTED` (`src/jls/hdl/HdlExporter.java:422-428`) lists
   exactly 22 element classes and neither appears. A machine built around the
   cheap native `RegisterFile` **cannot be exported to Verilog at HEAD.**
   Either exclude these elements from any HDL round-trip claim, or price
   adding them.
2. **Neither appears in `simulation-semantics.md`'s normative delay table**
   (its section 7) *or* in its zero-delay set (its section 6.2). A reader
   cannot learn their delay from the normative spec at all.
3. **Both save an editable `propDelay` that has no simulated effect.**
   `RegisterFile.java:72` and `FieldExtend.java:64` declare the field; both
   propagate at `now` (`RegisterFile.java:553-560`, `FieldExtend.java:473-478`).
   This matters beyond tidiness: `ARCHITECTURE.md`'s #221 equivalence
   criterion binds a future strategy to "observably identical per-element
   propagation delays," and two shipped elements now have a *declared* delay
   differing from their *observed* delay of zero.

---

## 4. The boot-cost model

### 4.1 The formula

```
T = N_instr × ev_instr / R

ev_instr = k × α × L × CPI
```

| Symbol | Meaning | Value | Measured or estimated |
|---|---|---|---|
| `N_instr` | Retired instructions to a shell prompt | **4.0 × 10⁷** | **MEASURED** — section 5.1 |
| `R` | Events per second, warm event loop | **3.14 × 10⁶ /s** | **MEASURED** — section 2.2, one circuit, one machine |
| `k` | Events per active logic element per cycle | **1.07 or 1.8 — unreconciled, 1.68× apart** | measured twice, differently; see 4.2 |
| `α` | Per-cycle active fraction of a multi-cycle machine | 0.18 / 0.40 / 0.56 | **never measured** — no multi-cycle JLS machine exists to measure it on |
| `L` | Functional logic elements (non-`Wire`, non-`WireEnd`) | ~580 central, band 400–870 (nommu); ~750 (Sv32) | estimated by census extrapolation; see 4.3 |
| `CPI` | Cycles per retired instruction | ~2.9 structural, ~1 behavioral | estimated |

The second form (`T = N_instr × ev_instr / R`) is the more robust one,
because it avoids splitting `α` from `CPI`, and it is the form the table in
4.4 uses.

### 4.2 The two values of `k`, and why you must pick one out loud

`k` was measured twice and the two results differ by 1.68×:

- **1.07** — from 243.1 ev/cycle ÷ 228 elements at unit duty.
- **1.8** — carried as "~1.8 events per active logic element per cycle" from
  the earlier probe set.

Both values circulate. **Neither the boot table below nor any prior document
declares which one it used**, and the choice moves every wall-clock number.
Worse, the two are entangled with the events/cycle discrepancy of section 2.5
— they are the same measurement seen from two sides.

Sweeping *both* uncertainties honestly, at L = 580, CPI 2.9, R = 3.1426 × 10⁶:

```
ev_instr = k · α · L · CPI  ranges over  324 … 1,695
boot     = 4.0e7 × ev_instr / R  ranges over  1.15 h … 6.0 h
```

**That is a 5.2× band, not a ±2% one.** Any table that prints a boot time to
three significant figures is printing precision the inputs do not have.

Note the consistency check that fails: the shipped 468 ev/instr figure (4.3
below), taken at k = 1.8, implies **α = 0.155 — below the floor of α's own
0.18–0.56 band**. Either `k` is 1.07 there, or `α` is outside its band, or
468 is wrong. This is unresolved and it is the single most important open
question in this document.

### 4.3 Element count and events per instruction

The element census is built from a raw tally of 460 elements for a nommu
machine, adjusted at HEAD and scaled by a glue factor:

```
460  raw baseline (older mirrored-Memory register file = 9 elements, inside it)
 -8  register file: 9 elements → 1 native RegisterFile
 -4  immediate generator: 4 Extend elements removed by FieldExtend
----
448  raw baseline at HEAD
```

| Corner | Elements | Arithmetic | Status |
|---|---:|---|---|
| central, nommu | **~580** | 448 × 1.30 | glue factor **estimated** |
| low | ~400 | 448 × 0.90 | estimated |
| high | ~870 | (448 + 150 combinational multiplier) × 1.45 | estimated |
| Sv32 + S-mode + OpenSBI | ~750 | 760 − 8 − 4 | estimated |

Events per instruction, re-derived from the section 3 measurements against a
baseline calibrated with the old mirrored register file:

```
Δ per cycle  = 18.00 (measured mirrored) − 6.94 (measured native) = 11.06
Δ per instr  = 11.06 × CPI 2.9                                    = 32.1
ev_instr     = 500 − 32                                           = 468
```

Against the older, more optimistic 12 ev/cycle convention for the mirrored
form the saving is (12 − 6.94) × 2.9 = 14.7, giving **485**.

> **The band 468–485 is not an uncertainty band on `ev_instr`.** It is a band
> over *which register-file convention the inherited 500 baseline used*. The
> real uncertainty is upstream, in `k` and `α`, and it is 5.2× wide (4.2).
> Do not present 468–485 as the model's error bars.

### 4.4 The table

`T = 4.0 × 10⁷ × ev_instr / 3.1426 × 10⁶`, except where noted. **Every row
inherits the 1.2–6 h honest band of section 4.2**; the point values are what
the central inputs give, not what the model knows.

| Machine | Logic elems | CPI | ev/instr | Wall clock | Basis |
|---|---:|---:|---:|---|---|
| Behavioral macro-element ("virtual hardware") | ~10 | 1 | **12** | **~2.5 min** | ev/instr is **MODELLED, never measured** — see below |
| Authored word-level, native `RegisterFile`, iterative M | ~580 | 2.9 | 468–485 | **~1.7 h**; honest band **1.2–6 h** | ev/instr derived; `k`/`α` unsettled |
| + behavioral Multiplier/Divider (hybrid) | ~560 | 2.4 | 403 | ~1.4 h | estimated |
| Authored, combinational multiplier | ~740 | 2.4 | 393 | ~1.4 h | estimated |
| Low corner | ~400 | 2.9 | 268 | ~0.95 h | estimated |
| High corner | ~870 | 2.9 | 888 | ~3.1 h | estimated |
| Sv32 + S-mode + OpenSBI | ~750 | 3.9 | 807 | **~4 h** | **estimated**, and it assumes N_instr = 5.6 × 10⁷ — a **×1.4 instruction inflation** for page-table build and SBI round-trips that was never measured, plus an estimated CPI 3.9 |
| Word-mapped Yosys import | ~2,300 | 1.3 | 1,120 | ~4.0 h | estimated |
| Gate-mapped import, memories preserved | ~9,500 | 1.3 | 3,965 | ~14 h | estimated |
| Gate-mapped, memories **flattened** | ~200,000 | 1.3 | 83,460 | ~12 days | estimated |

The 31-flip-flop register-file row that appears in older tables is **struck**:
at 114.53 ev/cycle it is strictly dominated by the native `RegisterFile` on
both axes.

**The behavioral row's 12 ev/instr is modelled, not measured.** Its only
cross-check is `1.8 ev/active element × ~7 active elements ≈ 12.6`, which
reuses the disputed `k`. Nothing should be costed against it until it is
measured (section 6.4).

**The gate-mapped rows exist to kill a myth, not to propose a path.** The
figure of ~170,000 cells that circulates for a Linux-capable RV32 core is an
artifact of a Yosys flag: plain `synth` flattens 72 Kib of I$/D$/register-file
SRAM into tens of thousands of `$_DFFE_PP_` and `$_MUX_` cells. The same
design with memories preserved is ~20,000 cells, and JLS models each of those
SRAMs as a single `Memory` element. **88% of the scary number is memory that
JLS does not spend elements on.**

### 4.5 What optimization work is worth, and what it is not

Two multipliers circulate and they are not interchangeable.

**The semantics-preserving stack: 2.26×.** Composed from three recorded stage
midpoints — `SigSim`/queue-depth work (−15%), value representation (−20%),
levelized zero-delay closure (−35%): 0.85 × 0.80 × 0.65 = 0.442 → **2.262×**.

> Label it honestly: *a conservative multiplicative composition of per-stage
> midpoints.* The stages attack **overlapping cost pools** — the levelized
> stage deletes 82.3% of all events, which removes 82.3% of the queue cost
> the first stage shrank and of the value cost the second halved. The
> composition is not derived; it lands conservatively, which is why it is
> usable.

**The full stack: 2.7–4.9×, a range.** Composing all four stage midpoints
gives 0.85 × 0.80 × 0.85 × 0.65 = 0.376 → **2.66×**. The 4.9× figure is
40,000 / 8,090 — the *top* of the independently stated 25–40 kcycles/s
projection. The primary source says outright: *use 2.7–4.9×*.

> **Never quote 4.9× alone as "the full stack."** Doing so turns a range into
> a promise. The honest nommu boot row after the full stack is **20–38
> minutes**, not "20–21 minutes." (It still fits a `timeout-minutes: 60` CI
> job at the pessimistic end, which is the decision the number was being used
> for.)

**Engine work does not accrue only to the structural tier.** The behavioral
row's 2.5 minutes is computed as 4.0 × 10⁷ × 12 / 3.1426 × 10⁶ — it divides
by the *same* constant the stack multiplies. Applying the stack to its own
formula gives ~1.1 min at 2.26× and ~0.9 min at 2.7×. The defense sometimes
offered ("the behavioral tier's cost sits in the 4.9% `react()`-bodies
bucket") is incompatible with the figure it defends: of the 12 events per
instruction, 11 are wiring, bus and memory events with ordinary react bodies,
so most of the behavioral tier's cost is the same bookkeeping the stack
removes. **Either 318 ns/event is right and the behavioral row moves, or it
is wrong and 2.5 min is not derivable.** The correction is in the favorable
direction and the tier ratio (~39×) is unchanged.

### 4.6 Live-console arithmetic

The chain, at the 468 ev/instr arm and CPI 2.9:

```
468 / 2.9              = 161.4 events per cycle
3.1426e6 / 161.4       = 19,473 simulated cycles/s
19,473 / 2.9           = 6,715 retired instructions/s
1e4 instructions       / 6,715 = 1.49 s per echoed character
```

At the 485 arm the same chain gives 18,791 cycles/s and 1.54 s/char.

| Stage | Cycles/s | s/char **at a 10⁴-instruction echo path** |
|---|---|---|
| Today | **18,800–19,500** | **1.5** |
| After the semantics-preserving stack (2.26×) | ~44,000 | ~0.66 |
| After the full stack (2.7–4.9×) | **52,600–95,400** | **0.30–0.55** |
| Mode C (unbuilt), derated | 22,000–40,000 | 0.73–1.3 |
| Mode C, underated ceiling — **not a target** | ~86,000 | 0.34 |

**Every s/char figure above assumes a 10⁴-instruction tty echo path, and that
number is not measured.** The available estimate is a *band*, 10⁴–10⁵
instructions, and the sensitivity is exactly 10×. It flips the verdict: at
10⁴ the console is laggy-but-usable after the semantics-preserving stack
(1.5 / 0.66 / 0.30 s); at 10⁵ it is unusable at every point in the plan
(14.9 / 6.6 / 3.0 s). **No s/char figure may ship without its echo-path
assumption in the same sentence.**

Against a requirement of 10⁵–10⁶ cycles/s for human-perceptible latency, the
10⁵ floor is missed by **1.05–5.1×** on today's basis, or **1.9–5.1×** against
the honest 2.7× full stack. This is a decision about where to spend effort,
not a physical limit — but it is also not a small gap, and no work breakdown
exists anywhere behind any estimate of what closing it would cost. Any figure
in maintainer-weeks for that work is currently unsourced.

**A Mode C caution, since it is the expensive option.** Break-even for a
compiled cycle engine against a 2.26×-optimized interpreter (44,010 cycles/s)
is:

```
1 / 44,010 s = 22.7 µs/cycle;  22.7 µs / 2 passes / 1,346 nodes = 8.45 ns/node
```

**The kill threshold is ~8.5 ns/node, not 15.** At 15 ns/node a compiled
engine would run at ~24,800 cycles/s — *slower* than the optimized
interpreter. The measured 4.32 ns/node clears the real threshold by only
about 2×.

---

## 5. Guest-side facts

All of section 5 was measured on instrumented emulators, not on JLS. It
constrains what a JLS machine must contain and how long it must run; it does
not depend on JLS at all, which is why it is durable.

**Method.** `cnlohr/mini-rv32ima` (https://github.com/cnlohr/mini-rv32ima) and
its prebuilt images (https://github.com/cnlohr/mini-rv32ima-images), built and
patched in-session so that every character written to the UART is stamped
with the retired-instruction count taken from the interpreter's own
`{cycleh, cyclel}` CSR pair. Date 2026-07. **These are direct measurements,
not estimates.**

### 5.1 Instructions to a shell prompt

Linux 6.5.12, RV32IMA nommu, 16 MiB RAM, busybox initramfs:

| Milestone | Retired instructions |
|---|---:|
| kernel entry → console alive | 3.6 × 10⁵ |
| → end of kernel init (`Run /init`) | 2.93 × 10⁷ |
| → userspace banner (`Welcome to mini-rv32ima Linux`) | 3.99 × 10⁷ |
| → shell after `root` login | 4.18 × 10⁷ |

**≈ 4.0 × 10⁷ instructions to an interactive shell.** Linux 6.1.14 at 64 MiB
is 5.68 × 10⁷ to the login prompt. Independent corroboration:
`raspiduino/arv32-opt` (mini-rv32ima on an ATmega328P) reports 8 h 18 min to
shell at ~1,500 instructions/s = 4.5 × 10⁷ — agreeing within 15%.

**The order of magnitude that decides feasibility is 10⁷, not 10⁸ and not
10⁹.** Estimates in the 10⁸–10⁹ range come from Sv32/MMU configurations or
from delay-loop-dominated boots (5.4).

For the Sv32 hedge: kernel init alone under RV32IMASU + Sv32 MMU + OpenSBI is
**2.8 × 10⁷** measured, with OpenSBI v0.9 firmware costing **3.2 × 10⁶** from
reset to S-mode entry. Instructions to a *shell* under Sv32 were **not
measured**; the ×1.4 inflation used in section 4.4 is an estimate.

### 5.2 Minimum RAM

Same kernel image, sweeping the memory size:

| RAM | Result |
|---|---|
| 64 MiB | boots, shell |
| **16 MiB** | boots, shell, root login (`Memory: 13668K/16380K available`) |
| **12 MiB** | boots, shell (6.1.14 image; login prompt at 5.99 × 10⁷) |
| 8 MiB | kernel boots, `/init` runs, then dies: `binfmt_flat: Unable to allocate RAM for process text/data, errno -12` |
| 6 MiB | panic: `No working init found` |
| 4 MiB | panic: `System is deadlocked on memory` |

**12 MiB is the practical floor for a shell; 16 MiB is the number to design
to.** This matches real silicon: the KianV uLinux SoC (TinyTapeout, sky130)
ships 16 MiB of external PSRAM.

**The collision with JLS, exact.** `Memory.DENSE_CAPACITY_LIMIT = 1 << 22`
(`src/jls/elem/Memory.java:1224`), and the dense store is chosen only when
`bits <= 64 && capacity <= DENSE_CAPACITY_LIMIT` (`:1234`). At 32-bit words
that is 4,194,304 words = **exactly 16 MiB, with zero headroom.** One word
past it and the store falls back to a sparse map. Note the gate is on **word
count, not bytes** — which is why replacing it with a byte budget is the
correctly-shaped fix rather than raising the constant.

**And the storage collision.** A saved `Memory` init is `"<hexaddr> <hexvalue>"`
pairs, with an RLE form emitted instead when it is shorter
(`src/jls/elem/Memory.java:455-466`), measured at **15.87 bytes per
32-bit word** (16,638,595 B for 1,048,576 words, on a generated probe). Then:

| Image | Words | `.jls` text | Against `MAX_CIRCUIT_TEXT_BYTES` = 64 MiB = 67,108,864 B |
|---|---:|---:|---|
| 2.38 MiB kernel (the real 6.5.12 image) | 624,000 | ~9.9 MB | 15% |
| 8 MiB kernel | 2,097,152 | ~33.3 MB | 50% |
| **16 MiB RAM image** | 4,194,304 | **66.6 MB** | **99.2% — under the cap, leaving ~0.5 MB for the entire circuit** |

`MAX_CIRCUIT_TEXT_BYTES` (`src/jls/FileAbstractor.java:65`) is measured
against **decompressed** text, so container choice does not move this. The
16 MiB row does *not* exceed the cap — earlier statements that it did
confused decimal MB with binary MiB — but 0.5 MB is not enough for a circuit,
and `Memory.initSim` copies the init image (`initMem.copy()`), doubling
transient heap.

### 5.3 The minimum SoC

Measured from an emulator that actually boots mainline Linux, not assumed.

The device tree that boots Linux 6.1.14 nommu is **1,536 bytes** compiled
(1,447 for the MMU variant). Its entire `soc` node is **one UART, one CLINT,
one syscon**. There is **no PLIC node**; Linux prints `riscv-intc: 32 local
interrupts mapped` and proceeds. The only interrupt controller is the
per-hart CSR-level `riscv,cpu-intc`, which is not a peripheral at all — it is
`mip`/`mie`.

**UART — three byte addresses, no interrupt line.** This is the most
surprising measured result in the study:

| Address | Direction | Behavior |
|---|---|---|
| `0x10000000` | write | THR — emit the byte |
| `0x10000000` | read | RBR — return the next input byte if one is available |
| `0x10000005` | read | LSR — return `0x60 \| data_ready` |

Everything else in the 0x100-byte window is write-ignored and reads as zero —
IER, IIR, FCR, LCR, MCR, MSR, SCR — and the 8250 autodetector still concludes
`ttyS0 at MMIO 0x10000000 (irq = 0, base_baud = 1048576) is a XR16850`.

Two consequences that a JLS UART must respect:

- **`irq = 0` is load-bearing.** `drivers/tty/serial/8250/8250_core.c` falls
  back to a **kernel timer** when a port has no hardware interrupt. That is
  what removes the PLIC — and it is also why the CLINT timer becomes
  load-bearing for *interactivity*, not just for scheduling.
- **LSR must never read `0xff`.** The 8250 driver treats an all-ones LSR as a
  missing port and disables it. The model returns `0x60 | data_ready`, so at
  least one bit in the byte must always be low.

**CLINT — two registers.**

| Address | Direction | Meaning |
|---|---|---|
| `0x1100bff8` / `0x1100bffc` | read | `mtime` low/high, a 64-bit free-running counter |
| `0x11004000` / `0x11004004` | write | `mtimecmp` low/high |

`msip` (software interrupt / IPI) is **not implemented at all** and Linux
boots, because `CONFIG_SMP` is off and IPIs only exist for multi-hart
systems. The whole timer interrupt is: when `{timerh,timerl} >
{timermatchh,timermatchl}`, set `mip |= 1<<7`.

The timer interrupt **cannot** be avoided: `arch/riscv/Kconfig` force-selects
`CLINT_TIMER` for M-mode, and the console depends on it via the polled-UART
path above. Budget a 64-bit up-counter, a 64-bit comparator, a 64-bit compare
register, and one interrupt line.

**A cost line that JLS cannot shortcut:** userspace is **bFLT, not ELF**
(`CONFIG_BINFMT_FLAT=y`, uClibc). Building the rootfs requires a
uClibc + elf2flt riscv32 nommu toolchain — a multi-hour, network-heavy
buildroot job.

### 5.4 The two free levers, and why they work

Linux burns cycles in `calibrate_delay()` and in every driver's `udelay()`,
and those cycles are proportional to the **declared** clock frequency while
computing nothing. Two levers follow, both free:

1. **Declare a slow clock.** The device tree's `timebase-frequency` in the
   measured configuration is `0xf4240` = **1 MHz**. Declaring 1 MHz instead
   of 50 MHz shrinks every `udelay` by 50× at zero architectural cost.
2. **Pin `lpj=` on the kernel command line.** This supplies `loops_per_jiffy`
   directly and skips `calibrate_delay()` entirely. It must be pinned for
   parity work anyway: a self-measured `loops_per_jiffy` is the guest's
   measurement of *its own speed*, so it could legally differ between two
   implementations that are otherwise identical.

**The measurement that proves the levers work.** Same image, same everything,
only the emulated speed changed:

| Emulated MIPS | `Run /init` | Login prompt |
|---:|---:|---:|
| 100 | 27,666,432 | 56,849,791 |
| 1 | 38,046,720 | 61,233,095 |

**A 100× slower machine costs only ~8% more instructions to reach the
prompt** (37% more to reach `/init`, from calibration and timeout loops).
**The boot is compute-bound, not timer-bound. There is no timeout cliff.**

**This holds under exactly one condition, and it is a hard requirement on any
JLS bring-up: CLINT `mtime` must be driven by *simulated* time, never host
wall clock.** The counter-example was observed in the same session — with
`mtime` driven by host wall clock and a human taking 8 seconds to type `root`,
the instruction counter ran to 1.5 × 10¹⁰, all of it idle spin.

These two levers are worth more than any engine optimization in this
document, and they cost nothing.

### 5.5 One published risk worth pinning against

RV32 nommu requires `CONFIG_NONPORTABLE=y`, and a 2024 upstream patch
proposed removing RV32 nommu support "by the beginning of 2027." This
document does not verify the current status of that proposal — it is an
external fact that goes stale. The mitigation is cheap and permanent:
**pin the kernel version, the `.config`, the initramfs and the device tree as
checksummed artifacts with a documented rebuild recipe.** Once pinned,
upstream removal becomes documentation rot rather than an existential risk.

---

## 6. What is still unmeasured and load-bearing

Each entry names the cheapest experiment that settles it. They are ordered by
how much of section 4 they move.

### 6.1 α — the per-cycle active fraction

**Spread 3.1× (0.18 / 0.40 / 0.56), never measured, and it is the dominant
input to every structural boot number.** No multi-cycle JLS machine exists to
measure it on. Section 4.2's consistency failure — the shipped 468 ev/instr
implying α = 0.155, below the band's own floor — is unresolved.

**Experiment.** Convert the single-cycle demo into a 2-cycle unified-memory
machine: merge instruction and data memory into one `Memory`, add an IR
`Register`, a fetch-versus-data address `Mux`, a PC-hold `Mux`, and a 2-state
sequencer — about ten new elements. Drive it with an **internal `Clock`** and
count events. One experiment measures α, CPI and the calibration constant
simultaneously. Days.

### 6.2 The events-per-cycle constant and its clocking regime

**121.5 (internal `Clock`) versus 245.5 (TestGen) on element-for-element
identical circuits, versus 388.4 on the anchor workload — a 3.2× spread on
exactly the axis a boot is forced onto** (section 2.5). A third figure, 243.1,
contradicts the 121.5 inside its own source document. `k` inherits this
disagreement as its 1.07-versus-1.8 split (section 4.2).

**Experiment.** Re-run the existing event-counting harness with **per-callback
attribution** under both drives, on the same circuit. Hours, not days, and it
moves every number in section 4 by up to 2×. **Do this before the α
experiment**, since α is measured through the same counter.

### 6.3 The tty echo path length

**10⁴ versus 10⁵ instructions — a 10× band that decides usable versus
unusable** (section 4.6). Nobody has measured it.

**Experiment.** On the same instrumented `mini-rv32ima` that produced
N = 4.0 × 10⁷, count retired instructions between a `getchar` on the UART and
the echoed byte appearing at the THR. One afternoon.

### 6.4 Behavioral events per retired instruction

**The modelled 12** (section 4.4). Its only cross-check reuses the disputed
`k`. Nothing on the behavioral tier — boot time, echo latency, the entire
interactive claim — should be costed until it exists.

**Experiment.** A ~200-line behavioral accumulator machine wired to a real
`Memory` on a real bus, event-counted with the same harness. Days.

### 6.5 `InteractiveSimulator`'s per-event cost

**Every cycles/s and s/char figure in this document is measured on
`BatchSimulator`.** A live console runs on `InteractiveSimulator`, which does
substantially more per event — a trace-map lookup, a `BitSet` clone, and a
walk proportional to the probe count. **The live-console numbers in section
4.6 are therefore upper bounds of unknown tightness.**

**Experiment.** Run the same 6,004-cycle fixture under `InteractiveSimulator`
with the same event counter. One afternoon.

### 6.6 The two levelized per-node costs

**4.32 ns/node and 3.10 ns/node for the same 522-node pass**, never
reconciled (section 2.6). The 1.39× difference propagates into every compiled-
engine figure. Separately, the measurement was taken at 522 slots; a
~580-element machine is ~1,346 nodes and the question is cache behavior at
2.6× the working set.

**Experiment.** Re-run the levelized model at ~1,400 slots with the activity
variants, and instrument both pass timings in the same run so the two figures
come from one measurement.

### 6.7 Cross-platform and cross-JDK determinism

**Nothing in the tree asserts that a simulation is bit-identical across a JDK
or OS change**, and iteration order in any `HashMap`-backed path would break
it silently. `docs/reproducibility.md` covers *build* reproducibility — the
jar and the BOM — and says nothing about runs. Every parity claim assumes an
answer nobody has.

**Experiment.** Run one circuit on the three CI platforms that already exist
as jobs and diff the VCDs. Days. If determinism does not hold, that finding
outranks most of this document.

### 6.8 Memory byte lanes

Not a measurement gap but a capability gap, recorded here because it lives in
the directory being deleted: `lb`/`lh`/`lbu`/`lhu`/`sb`/`sh` are
unimplemented **because 32-bit `Memory` has no byte lanes**
([`capability-roadmap/README.md:88-90`](capability-roadmap/README.md)). The
minimum SoC's UART is three *byte* addresses on a 32-bit bus (5.3). **Without
sub-word access there is no UART driver and no Linux.**

### 6.9 An adjudication that should not wait

An event polled past `maxTime` is **removed from `dupCheck` before the limit
check**, then discarded without reacting or re-queueing
(`src/jls/sim/Simulator.java:224-233`: `poll()` → `dupCheck.remove(event)` →
`now = event.getTime()` → `if (now > maxTime) { now = maxTime; break; }`).
Today it is a curiosity. Under any future state capture or resume it is
silent corruption, because the dedup eviction happens first and the resumed
run retains no record the event existed. `JLSInfo.defaultTimeLimit =
100000000` (`src/jls/JLSInfo.java:69`) is roughly 400× short of a boot, so any
long run meets this path.

---

## 7. How to re-measure

Written so that a future maintainer can reproduce sections 2 and 3 after the
code has moved. It assumes nothing about `riscv/` still existing.

### 7.1 What you need

1. **A CPU-scale workload circuit** and a matching clock/stimulus vector.
   Historically this was `riscv/build/k2000.jls` + `k2000_clk.txt` (6,004
   cycles, time limit 12,008,000), generated by `riscv/bench_kernel.py`.
   **That file was never tracked** (`riscv/.gitignore` line 1 is `build/`).
   If it was regenerated and committed as a fixture before the deletion, use
   that fixture and record its path here. If it was not, section 2 is no
   longer reproducible and any successor circuit must be re-characterized
   from scratch, with its census printed alongside the numbers — because
   **events/cycle is a property of the circuit, not of the engine.**
2. **A built jar**: `mvn -q package -DskipTests`.
3. **Harnesses in the `jls.sim` package**, compiled against the jar. They
   must be in that package to reach the protected event loop.

### 7.2 The three harnesses, and what each is for

| Harness | Measures | Shape |
|---|---|---|
| Event counter | events fired, by payload and by callback; posted; dup-suppressed; max queue depth | Subclass `BatchSimulator`, override `afterEvent` (the no-op is `src/jls/sim/Simulator.java:269`, already overridden at `src/jls/sim/BatchSimulator.java:140`). Zero changes to `jls.sim` are required |
| Phase timer | `initSimulation` versus `runEventLoop` separately, with **no** per-event instrumentation | A second, uninstrumented runner. Keeping it separate is the whole point: the event counter perturbs the loop it measures |
| Census | logic elements, wire nets, max width, subcircuit count, per-type histogram | Walks the loaded `Circuit`; counts non-`Wire`, non-`WireEnd` elements |

**Report both phase timings, always.** The warm-loop and including-init
figures differ by 1.76× and the study's worst errors came from conflating
them.

### 7.3 Procedure

```
1. Warm the JIT. Best of 8 reps minimum; discard the first two.
2. Run the phase timer.      → initSimulation s, runEventLoop s
3. Run the event counter.    → events fired, cycles, per-callback histogram
4. Run the census.           → logic elements, nets, width mix
5. Derive, showing the division:
     ns/event      = runEventLoop / events
     events/s warm = events / runEventLoop
     events/s incl = events / (initSimulation + runEventLoop)
     ev/cycle      = events / cycles
     cycles/s warm = cycles / runEventLoop
6. Record: circuit path, census, cycle count, clocking regime
   (internal Clock vs TestGen/-t vector — see 2.5), date, OS, CPU, JDK.
```

**Step 6 is not optional.** A throughput number without its census and its
clocking regime is not a measurement; it is a rumor.

### 7.4 Profiling

```
java -XX:FlightRecorderOptions=stackdepth=512 \
     -XX:StartFlightRecording=filename=deep.jfr,settings=profile \
     -cp .:$JAR <phase-timer> <circuit> <vector> <timelimit> 12
jfr print --events jdk.ExecutionSample deep.jfr > deep.txt
```

**`stackdepth=512` is required.** At the default depth roughly 30% of samples
are truncated and mis-attributed, which is enough to invert the ranking in
section 2.3.

For allocation, use `jdk.ObjectAllocationSample` from the same recording.
Expect `SigSim`'s `byte[]` to dominate until its quadratic stimulus parse is
fixed; measure it separately from the loop or the loop numbers are noise.

### 7.5 Reproducing the element-cost table (section 3)

This is the cheapest and most durable of the three, and it needs no CPU-scale
circuit:

1. Build a **fixed stimulus harness** circuit: a `Clock`, a small address
   counter (`Register` + `Adder` + `Constant`), a data counter, one 1-bit
   `Constant`. Measure it alone. That is the baseline row.
2. Add **exactly one candidate structure** and measure again. The
   **difference** is the element's cost; the absolute is context.
3. Repeat for each alternative implementation of the same architectural
   component, with identical stimulus.
4. Run 1,000 clock cycles at a 100-unit period. Report Δ elements and
   Δ events/cycle.

Vary depth and width independently to confirm 3.4's flatness result on any
new element before assuming it.

### 7.6 Microbenchmark discipline

The recorded numbers were produced with hand-rolled benchmarks (warm-up
rounds, then best-of-7 over 3 M iterations, results accumulated into a
checksum to defeat dead-code elimination). JMH is deliberately **not** a
dependency of this project. If a future measurement adopts JMH it should say
so, because the absolute numbers will shift even when the ratios do not.

---

## 8. Summary of the numbers, with their status

| Number | Value | Status |
|---|---|---|
| Warm-loop throughput | 3.14 × 10⁶ events/s, 318 ns/event | **measured**, one circuit, one machine |
| Including-`initSimulation` throughput, same workload | 1.78 × 10⁶ events/s | **measured** |
| Cross-circuit throughput band, mixed scopes | 2.0–2.6 × 10⁶ events/s | **measured**, six probes |
| Loop cost split | 47.7% queue bookkeeping, 37.6% value, 4.9% logic | **measured** (profile) |
| Events/cycle on the anchor CPU | 388.4 | **measured**, TestGen-driven — see 2.5 |
| Events/cycle, internal `Clock` vs TestGen | 121.5 vs 245.5 | **measured, unreconciled, 2.02×** |
| Register file: native / mirrored `Memory` / flip-flop farm | 6.94 / 18.00 / 114.53 ev/cycle | **measured** |
| Event cost versus register-file depth and width | flat (20.95 / 21.01 / 21.01) | **measured** |
| Levelized pass, 522 nodes, plane arrays | 4.32 ns/node (and 3.10 at 100% activity) | **measured, two figures, unreconciled** |
| Levelized ceiling for a ~580-element machine | ~86,000 cycles/s underated; 22,000–40,000 derated | derived from the above; **ceiling is not a target** |
| Save cost of a memory image | 15.87 bytes per 32-bit word | **measured** |
| Instructions to a Linux shell, RV32IMA nommu | 4.0 × 10⁷ | **measured** |
| Cost of a 100× slower machine | +8% instructions to the prompt | **measured** |
| Minimum RAM for a usable shell | 12 MiB floor, 16 MiB to design to | **measured** |
| Device tree size | 1,536 bytes (nommu) | **measured** |
| Minimum UART | 3 byte addresses, irq = 0, no PLIC | **measured** |
| `k` (events per active element per cycle) | 1.07 **or** 1.8 | **measured twice, 1.68× apart, unreconciled** |
| α (per-cycle active fraction) | 0.18 / 0.40 / 0.56 | **never measured** |
| CPI, structural | ~2.9 | **estimated** |
| Logic elements, nommu machine | ~580 (band 400–870) | **estimated** (census × glue factor) |
| Events per instruction | 468–485 at central inputs; 324–1,695 across the honest `k`/α sweep | **derived**, band dominated by unmeasured inputs |
| Structural boot, nommu | ~1.7 h at central inputs; **honest band 1.2–6 h** | **derived** |
| Sv32 boot | ~4 h | **estimated**; assumes an unmeasured ×1.4 instruction inflation and CPI 3.9 |
| Behavioral boot | ~2.5 min | **estimated**; its 12 ev/instr is modelled, and it moves with engine work |
| Semantics-preserving optimization stack | 2.26× | **derived** from stage midpoints; overlapping cost pools; conservative |
| Full optimization stack | **2.7–4.9×** | a **range**; 4.9× is its top, not its value |
| Live console today | 18,800–19,500 cycles/s; 1.5 s/char **at a 10⁴-instruction echo path** | **derived** on `BatchSimulator`; `InteractiveSimulator` is slower by an unmeasured factor |
| tty echo path length | 10⁴–10⁵ instructions | **never measured**; a 10× swing in every s/char figure |
