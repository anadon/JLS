## Compiled / levelized evaluation: the 100x simulation engine

*Leapfrog sweep 02. Builds on `docs/capability-roadmap/keystone-c-performance.md`,
which measured the current loop; I do not re-derive its numbers. Every claim about
the tree carries a path and a line number verified at HEAD. The one number I
produced this session is a re-run of `riscv/bench_kernel.py` (§1.4), included
because the sweep's brief requires before/after claims to be anchored to that
harness. Claims about other tools are marked **verified** or **unverified**.*

---

### 0. The one-sentence version

JLS interprets. Every serious simulator of the last twenty years compiles, and
`docs/grand-architecture.md:331-342` already says so out loud — *"the `riscv/`
CPU-scale trajectory is exactly where Verilator's elaborate-to-flat approach
(levelize the graph once, run a statically-ordered evaluation pass, ~100×) pays
off"* — and then `ARCHITECTURE.md:341-368` records the decision not to build it,
behind a revisit trigger ("unusably slow interactively") that is not a testable
condition. Keystone C supplied the number the decision was taken without:
**8,090 simulated CPU cycles/s warm, ~1,100–1,450 end-to-end from the CLI**, of
which **4.9% is digital logic** and 95% is bookkeeping. This document specifies
the second engine, and — more importantly — specifies how the two are kept
telling the same truth.

---

### 1. What is missing today

#### 1.1 There is no elaboration step, so there is nowhere to put a compiled netlist

`Simulator.initSimulation` (`src/jls/sim/Simulator.java:180-202`) is the whole of
JLS's "elaboration": clear the queue, walk `Circuit.getElementsInStableOrder()`
(`:196`), call `initSim` on each `LogicElement`. It produces no netlist, no
ordering, no index. The simulation state stays scattered across the object graph
it was drawn into:

| state | where it lives |
|---|---|
| a net's value | `WireNet.value`, `src/jls/elem/WireNet.java:405` — one `@Nullable BitSet` per net object |
| an input's value | `Input.setValue`/`getValue` per `Put` object |
| a gate's in-flight value | `Gate.toBeValue`, and an independent copy of the same idea in `Register`, `TriState`, `ShiftRegister` |
| net structure | `LinkedHashSet<WireEnd> ends` / `LinkedHashSet<Wire> wires`, `WireNet.java:22,24` |

There is no array of anything. That is the single structural fact that makes a
compiled pass impossible today, and it is why keystone C §6.2's finding matters so
much: the levelized layout it measured — `long[] a; long[] b; long[] u` indexed by
node id — has no home in the current model, and the value-domain program is what
builds that home.

#### 1.2 Hierarchy is a *runtime* cost, which is backwards

`SubCircuit.react` (`src/jls/elem/SubCircuit.java:621-636`) crosses a module
boundary by posting one event per input pin, each with a `HashMap` lookup into
`inmap` (`:33`) and a `BitSet` clone:

```java
for (Input in : inputs) {
    InputPin pin = inmap.get(in);
    ...
    copy = new SimEvent.NewValue((BitSet)in.getValue().clone());
    sim.post(new SimEvent(now,pin,copy));
}
```

`SubCircuit.send` (`:645-652`) does the mirror on the way out. So every boundary
crossing is a hash lookup, a clone, a priority-queue insert, a dedup-set insert, a
poll and a dedup-set remove — to model **zero elapsed time**
(`docs/simulation-semantics.md` §6.2 lists `SubCircuit.react` among the zero-delay
elements). A student who structures a design into five levels of subcircuit pays
five times the queue traffic of the student who drew one flat mess.

The roadmap already names a *teaching inversion* in P3 — a well-structured design
is punished by the HDL exporter (`README.md:346-351`). This is the same inversion
in the kernel, and nobody has written it down: **hierarchy, the thing JLS teaches
as good practice, makes JLS slower.** An elaborate-to-flat pass deletes the tax
entirely; after elaboration a subcircuit boundary is a net alias and costs nothing.

#### 1.3 82.3% of all events model no time at all

Keystone C §6.3's census, on the 6004-cycle RV32I workload: `PinChanged`
**1,919,891 of 2,331,793 events (82.3%)**. `PinChanged` is the same-timestamp
notification `WireNet.propagate` sends every sink (`WireNet.java:507-508`), and
the zero-delay elements listed in `docs/simulation-semantics.md` §6.2 —
`Splitter`, `Binder`, `InputPin`, `OutputPin`, `SubCircuit`, `Constant` — chain
them arbitrarily deep. The CPU census is 34 `Splitter`s, 9 `Binder`s, 43
`Constant`s and 5 `Extend`s out of 225 logic elements: a third of the design is
pure wiring, and all of it is on the priority queue.

#### 1.4 The measured floor, re-confirmed this session

`riscv/bench_kernel.py` is committed, runs, and self-checks against
`riscv/riscv_ref.py`. On this tree at HEAD, jar
`target/jls-5.0.5-SNAPSHOT.jar`, JDK 25:

```
$ cd riscv && python3 bench_kernel.py 100
instructions retired (= clocked cycles): 304
wall: 1.017 s   outcome: Simulation Time Limit at 608000  rc=0
x1 = 5050 (ref 5050)

$ cd riscv && python3 bench_kernel.py 2000
instructions retired (= clocked cycles): 6004
wall: 5.179 s   outcome: Simulation Time Limit at 12008000  rc=0
x1 = 2001000 (ref 2001000)
```

**1,159 end-to-end cycles/s at 6004 cycles**, consistent with keystone C §2's
5.63 s / ~1,100–1,450 range. This is the baseline every claim below is measured
against, and the harness prints the reference emulator's answer beside the
circuit's precisely so that "fast because it stopped computing" is visible rather
than silent (`riscv/bench_kernel.py:13-14`).

#### 1.5 The workarounds — the strongest evidence the gap is real

Seven, all in the tree, all costing somebody something today:

1. **`riscv/bench_kernel.py` exists at all.** Its docstring
   (`riscv/bench_kernel.py:1-14`) says it is "the largest real simulation workload
   in the tree, so it is the honest baseline for any change to the event loop or
   the value representation." A project ships a hot-loop benchmark when the hot
   loop is a live concern.

2. **The RISC-V compliance plan is scoped around simulator speed.**
   `docs/standards-adoption/05-riscv-compliance.md:121-131`, step 4 of the arch-test
   plan, is titled *"Switch to a free-running clock for arch-test runs"* and says
   the `-t`-vector path is *"unusable when the cycle count is unknown a priori, and
   slow,"* with a measurement: **40,000 cycles took 38.2 s with the vector versus
   4.1 s free-running — ~1,000 vs ~9,700 instructions/s.** A step in a
   standards-conformance plan exists solely to work around throughput.

3. **`riscv/verify.py:gen_clock` generates exactly as many rising edges as the
   reference emulator took** (`riscv/verify.py:18-26`, `steps = cpu.run(...)` at
   `:31`). The circuit is not allowed to run one cycle longer than an *emulator*
   says is necessary. That is a simulator being rationed.

4. **`riscv/fuzz_diff.py` buys throughput with processes instead of engine
   speed.** Its docstring: *"JLS runs are a few seconds each, so programs are
   checked concurrently in a thread pool (each is an independent java
   subprocess)"* (`riscv/fuzz_diff.py:1-6`), default 6 workers (`:91`).

5. **Fuzzing depth is capped by simulation cost.** `rand_program` generates
   **6 to 24 instructions** (`riscv/fuzz_diff.py:97`), **straight-line only** — the
   comment says *"No control flow (covered by the directed suite) so length is
   exactly n"* (`:29-31`). Straight-line 24-instruction programs is not
   differential testing of a CPU; it is differential testing of an ALU. The
   reason is that a program with loops has an unpredictable cycle count and
   §1.5(3) means the vector length must be predicted.

6. **The GUI's answer to "watch it run" is a one-second timer.** The `Animate`
   button *"repeat[s] step every second"* (`src/jls/edit/InteractiveSimulator.java:136`,
   `TimerTask` at `:391-420`). And the interactive engine is *slower* than batch,
   not faster: `InteractiveSimulator.afterEvent` (`:879-896`) runs per event with
   a `getCurrentValue()` clone for the watched element and an **O(probes) walk
   with a clone each** (`:885-893`), while `beforeEvent` calls `Editors.of(circuit())`
   per event (`:738`). `docs/grand-architecture.md:328-329` requires watcher updates
   to be *"batched, rate-limited, never per-signal"*; that rule is honoured for
   exactly one thing, the clock label at 50 ms (`:838-871`).

7. **Grading is a JVM per submission.** `examples/autograde/` and the
   `-b -t` contract mean an instructor's batch grading cost is (JVM start +
   quadratic `SigSim` setup + interpreted run) × submissions.

#### 1.6 And a hole that is not about speed at all

Nothing in the tree computes a topological order or detects a cycle.
`Circuit.getElementsInStableOrder` (`src/jls/Circuit.java:479-485`) is a stable-*id*
order for deterministic seeding (`docs/simulation-semantics.md` §3), not a data-flow
order. `grep -rn "combinational loop"` over `src/` returns nothing;
`docs/capability-roadmap/sweep-02-timing.md:271-272` records the consequence — *"JLS
today has no loop detection; a cross-coupled NAND latch is legal and"* an
accidental one is a silent hang. The levelizer produces that detector as a
by-product.

---

### 2. The capability

Elaborate to a flat netlist once; levelize the combinational graph; evaluate it in
statically determined order over plane arrays; keep the event engine. Verilator's
shape (**verified**: named as the reference in `docs/grand-architecture.md:334-336`
and in `ARCHITECTURE.md:344-346`), adapted to a schematic tool that must keep an
interactive, animated, delay-accurate mode.

Six design decisions, each with a recommendation.

#### 2.1 Elaboration — and where the elaborator should live

**`jls.core.sim.Netlist`**, produced by a `NetlistBuilder` that:

- **flattens hierarchy.** Every `SubCircuit` is inlined; `inmap`/`outmap`
  (`SubCircuit.java:33,35`) become net-identity merges. This is easy today for an
  unhappy reason: `SubCircuit.save` writes the nested circuit **inline**
  (`SubCircuit.java:282-288`) and `Circuit.load` constructs a fresh `Circuit` per
  instance (`src/jls/Circuit.java:1015`), so instances are already independent
  copies. When P3 lands reuse identity, flattening becomes instantiation and the
  builder gains a per-instance path prefix — design for that now, it is one field.
- **unions nets across named jumps.** `JumpStart`/`JumpEnd` alias nets. **This code
  already exists**, in the wrong place: `HdlExporter`'s net walk with its
  `UnionFind` (`src/jls/hdl/HdlExporter.java:1038-1109`, documented at `:89-91`).
  **Recommendation: lift it into `jls.core` and make `HdlExporter` a consumer.**
  Two independent implementations of "which drawn wires are one signal" is exactly
  how the compiled engine and the exporter come to disagree about a circuit.
- **assigns dense integer ids** to nets and to evaluation slots, and keeps a
  bidirectional map `nodeId ↔ drawn Element / WireNet`. **Build this map on day
  one.** It is what traces, probes, watches, VCD, the critical-path overlay (P4)
  and cross-probing (P6) all need, and retrofitting it into a straight-line loop is
  the classic way a fast engine ships that nobody can use (§6, risk 2).
- **emits an opcode per slot**, not an object. Keystone C's census says the whole
  measured event mix is twelve shapes: `Mux` 875,291, `Register` 428,298,
  `Splitter` 250,115, `AndGate` 207,456, `Adder` 108,025, `ShiftRegister` 97,122,
  `XorGate` 82,011, `NotGate` 67,983, `Memory` 67,545, `Binder` 57,026, `OrGate`
  50,860, `Extend` 16,000. Roughly twelve opcodes cover >97% of the events on the
  flagship design.
- **provides an ESCAPE opcode** for everything else. `Memory` (access-time model,
  `MemoryRead`/`MemoryWrite` payloads), `StateMachine`, `TruthTable`, `SigSim`,
  `Display`, `Stop`, `Pause`, `Clock` keep their existing `react` bodies and are
  called through a shim. **This is the decision that stops the second engine from
  becoming a second element library.** 25 `react` implementations exist
  (`grep -rl "public void react(long now" src/jls/elem/` → 25 files); the compiled
  pass reimplements at most twelve of them, and only the ones that are pure
  combinational functions of their inputs.

State layout, exactly keystone C §6.2's measured one:

```
long[] a, b, u        // plane-major signal state, indexed by net id
int[]  width          // per net
int[]  opBase, ops    // flattened operand lists
int[]  result         // net id each slot drives
byte[] opcode
int[]  order          // slots in evaluation order
int[]  levelStart     // order[levelStart[k] .. levelStart[k+1]) is level k
long[] activity       // bitmap of dirty nets
```

#### 2.2 Levelization

Kahn over the combinational graph, sequential elements cut. Rank = longest path
from a source, so a level's nodes depend only on lower levels and **the pass
converges in one sweep with no fixpoint iteration** — which is why keystone C
measured 522 slots at 2.26 µs (4.32 ns/node) rather than at some
iteration-count multiple.

Free by-products, both of which the roadmap wants elsewhere:

- **combinational-loop detection** (the residue Kahn cannot emit) — sweep-02's
  missing item, and P5's ERC list (`README.md:543`) names it;
- **the timing DAG P4's static timing analyser needs** (`README.md:414-416`:
  "a timing DAG over `Circuit.getElementsInStableOrder()` and the `WireNet`
  structure, cut at sequential boundaries"). That is a description of the
  levelizer. Build the graph once, in `jls.core`, and let STA consume it.

#### 2.3 Sequential elements and clock edges

Registers, synchronous memories (`Memory.sync`, #199) and state machines are cut
points: their outputs are level-0 sources; their inputs are sinks with no
outgoing combinational edge. A clock edge is then the standard two-phase step:

1. settle the combinational cone (one levelized sweep);
2. **sample** every sequential element's inputs into a shadow array;
3. **commit** every sequential element's outputs from the shadow array
   simultaneously;
4. settle again.

Simultaneous commit is what makes a shift register a shift register rather than a
race, and it is the semantics JLS gets today only because `Register.react` posts
its output at `now + propDelay` (`src/jls/elem/Register.java:766-778`) and the
delay is uniform. That is worth stating in the semantics document as the thing the
cycle-based mode makes *explicit* rather than accidental.

#### 2.4 Feedback that cannot be levelized — partition, do not refuse

Kahn's residue is not an error; it is a cross-coupled NAND latch, a gated D-latch,
a ring oscillator, or a genuine mistake. **Recommendation: Tarjan SCC over the
combinational graph. Every non-trivial SCC collapses to one *irreducible block*
node in the level order.** Outside blocks, straight-line evaluation. Inside a
block, iterate the block's own nodes to a fixpoint with a bound
(`64 · |block|` is generous), and on hitting the bound report an oscillation at
that named set of elements — which is a far better diagnostic than today's silent
hang.

The alternative — "refuse to compile any circuit containing a loop, fall back to
the event engine" — is simpler and wrong, because the SR latch built from two
cross-coupled NAND gates is a first-year lab and would opt the entire sequential
unit of a course out of the fast engine. It is also wrong for the `riscv/` GUI CPU,
which `riscv/gui/README.md` describes as *"wired into two feedback loops"*.

Note the honest limit: inside an irreducible block, a cycle-based fixpoint does not
reproduce the *timing* of a real latch, only its settled value. A metastable
cross-coupled pair has no settled value and the bound fires. Say so in the
document; do not model it.

#### 2.5 Time: two modes, and a third one that must not be built

**Mode T — timed-levelized. Default, always on, no flag, no semantic change.**
This is keystone C §6.3's proposal and it is what should ship first. The event
queue stays the sole authority on *time*; the compiled pass owns only the
zero-delay closure. When a timed event lands, evaluate that element's zero-delay
cone in compiled order in one straight-line sweep, then post only the genuinely
delayed successors.

- Transport delays stay in the queue, so `docs/simulation-semantics.md` §6.2, §7,
  §8 are untouched, every golden is byte-identical, every VCD byte is identical,
  and **`ARCHITECTURE.md:359-368`'s equivalence criterion is satisfied as written,
  with no amendment.**
- Removes ~82% of queue traffic (§1.3) and collapses queue depth.
- Projected by keystone C §6.3 at −30…−40% of loop time on top of the value
  domain's −15…−25%.

**Mode C — cycle-based. Opt-in, a declared alternative strategy.** No per-element
delay; time advances one clock edge at a time; the whole design settles per edge.
This is where the order-of-magnitude lives. It **cannot** reproduce transport
delay (a pulse narrower than a propagation delay is explicitly *not* swallowed —
`docs/simulation-semantics.md` §6.2), so its oracle is "same settled values at
every clock edge," not "same VCD." It needs a new section in the normative
document and an amendment to the #221 recorded decision — and that decision
already prescribes the process: *"Any divergence is a specified, documented change
to `docs/simulation-semantics.md` first, never a silent behavioral difference"*
(`ARCHITECTURE.md:365-368`).

**The mode not to build: levelized-with-delays.** Compute settled values *and*
arrival times per net in one pass and reconstruct a delay-accurate VCD by replay.
It is the obvious "have both" and it is a trap: with reconvergent fanout and
transport (not inertial) delay, a net's history within a cycle is a *train* of
transitions, not a single arrival, and a single levelized sweep cannot produce it.
JLS's own `Adder` makes this concrete — `Adder.resetPropDelay` sets
`propDelay = bits * defaultPropDelay` (`src/jls/elem/Adder.java:261`), i.e. 960 for
32 bits, while `riscv/verify.py:15` sets the clock half-period `HALF = 1000`. The
CPU's clock was chosen to be *just* longer than one lumped adder delay. Reproducing
that faithfully is the queue's job. Leave it there.

#### 2.6 Switch-over policy

**Explicit for Mode C, invisible for Mode T, and refusal rather than silent
degradation.**

- **Mode T is not a user-visible choice.** It is an optimization of the single
  strategy, it changes nothing observable, and it needs no flag. One escape hatch
  for bisecting a suspected divergence: a system property `-Djls.levelize=false`,
  documented as a debugging aid, not in `docs/batch-interface.md` (which is a
  stability contract — `docs/batch-interface.md:1-11` — and should not grow a flag
  for this).
- **Mode C is opt-in per run**: a `-cycle` batch flag (an addition to the batch
  contract, so CHANGELOG plus a version bump per `batch-interface.md` §6) and a GUI
  run-mode selector beside Run/Step/Animate.
- **Mode C refuses, by name, what it cannot model.** A `DelayGate` used as a delay
  line; a `TriState` whose behaviour depends on turn-off timing relative to a
  turn-on; a level-sensitive `Memory` write (the glitch hazard of
  `docs/simulation-semantics.md` §8.4 is *a timing phenomenon* and a cycle engine
  would silently make it disappear — which would be JLS teaching that #199's bug
  does not exist); more than one `Clock` with incommensurable periods; an
  irreducible block that does not settle. The refusal message names the elements.
  **A fast mode that quietly means something different is how a teaching tool
  teaches something false** — the same failure the roadmap already identifies in
  `VhdlEmitter`'s nine-value `when others` and the VCD `$timescale`.
- **The hint.** Elaboration already knows the node count and the census. If a run's
  projected cost exceeds a threshold, print one stderr line naming `-cycle` and
  what it would cost. Suggest; never switch.

#### 2.7 Keeping the two engines identical — specify this, do not hope

The maintenance-trap question is the real one, and it has a structural answer and a
testing answer. Both are required.

**Structural: write the logic once.** Keystone C §6.2 states the contract —
`Word.and(x,y)` and the levelized `AND` case are *the same six `long` operations
over the same plane semantics*. Therefore `LogicValue`'s API must be specified so
that every op exists in two forms sharing one expression body: one over values,
one over a `(long[] a, long[] b, long[] u, int index)` plane slot. Keystone C §8.3
already says this is *"a small constraint if it is stated up front and an expensive
rewrite if it is not."* Validate both forms from **one** IEEE 1164 table oracle
(keystone A §5.5). Combined with the ESCAPE opcode (§2.1), the duplicated surface
is about twelve pure combinational functions, and every one of them is table-tested.

**Testing: engine A against engine B, in CI, as a first-class harness.** The
oracle stack already exists in three layers and #221 already makes it binding:
`test/jls/RiscvCpuGoldenTest.java` (bit-for-bit), `riscv/verify.py` (11 directed
programs vs `riscv/riscv_ref.py`), `riscv/fuzz_diff.py` (randomized differential).
Add a fourth layer that is engine-vs-engine rather than JLS-vs-emulator:

| what | Mode T comparison | Mode C comparison |
|---|---|---|
| every `.jls` in `test/fixtures/`, `examples/`, `riscv/build/`, `riscv/gui/cpu.jls` | full VCD **byte-for-byte**, plus stdout byte-for-byte | settled value of every net at every clock edge; final register and memory state |
| randomly generated circuits (a structural fuzzer: random gate DAGs, random fanout, random widths, seeded) | full VCD byte-for-byte | settled values at edges |
| the `fuzz_diff.py` program corpus, widened once the engine is fast | final architectural state | final architectural state |

Two properties make this cheap rather than heroic. First, Mode T's comparison is
*byte-identity*, which needs no new oracle at all — the existing goldens are the
test, and the acceptance criterion is "no golden changed." Second, the structural
fuzzer is ~200 lines and is reusable by P5's ERC and P4's STA.

**And a divergence enumeration in the normative document.** The failure to fear is
not a wrong AND gate; it is a *scheduling* divergence. Concretely: a clock edge and
a data change arriving at the identical timestamp are resolved today by
read-latest + FIFO (`docs/simulation-semantics.md` §6.1) — the register sees the
data change if it was propagated first. A two-phase cycle engine resolves the same
race by construction (sample-then-commit). These can differ, legitimately, and the
difference must be *written down as a specified property of Mode C*, not
discovered by a student whose counter counts wrong.

---

### 3. What it unlocks

#### 3.1 Standards

This is not primarily a standards item and should not be sold as one. Honestly:

- **#5 / #6 RISC-V arch-test compliance, in practice.** The plan in
  `docs/standards-adoption/05-riscv-compliance.md:121-131` contains a step that
  exists *only* because of speed. At Mode C rates that step becomes optional and
  the compliance suite becomes a CI job.
- **#49 / #50 / #53 (P5's constrained-random stimulus and coverage closure) become
  economically real.** A coverage-closure loop is thousands of runs by definition.
  Today `fuzz_diff.py` needs 6 concurrent JVMs to check 30 straight-line programs.
- **#66 / #67 waveform export over long runs** becomes practical rather than a
  1.3 MB-vector exercise.
- **#63 / #64 (SMT-LIB, AIGER/BTOR2)** benefit indirectly: the flat netlist with
  dense node ids and a cut at sequential boundaries is *exactly* the shape those
  printers want. The elaborator is shared substrate.

It unlocks **no** value-domain, element, or interchange standard on its own. Say
that plainly; the roadmap's other programs are where standards live.

#### 3.2 Engineering

- **Differential fuzzing at scale.** `riscv/fuzz_diff.py` today: 6–24
  instructions, straight-line, no control flow, 6 worker JVMs. After: thousands of
  instructions with loops and branches, hundreds of programs per CI run, single
  JVM with a multi-run batch mode (P5's `Stimulus` SPI, `README.md:525-528`).
  The class of bug this finds — a control-path bug that only manifests after a
  taken branch into a hazard — is currently unreachable by construction.
- **A regression suite in CI that runs the CPU.** `RiscvCpuGoldenTest` today is one
  small program because the golden must finish inside a test run.
- **Interactive what-if.** Change a constant, re-run 10,000 cycles, see the answer
  before you have forgotten the question. The netlist must be **cached and
  invalidated through the op layer** (`jls.collab.op.CircuitOp`/`OpSink`, #167) so
  the edit-run-edit loop does not re-elaborate from scratch on every Run press —
  a genuinely nice fit with the shipped operation vocabulary.
- **Larger designs at all.** A five-stage pipelined RV32I is roughly 3–5× the
  element count of `riscv/`'s single-cycle CPU, with worse fanout, and the event
  count grows faster than the element count.
- **The interactive engine stops being the slow one.** Combined with the
  standalone "interactive-engine batching" item (`README.md:699`, 1–2 wk), which is
  a *prerequisite*, not an optional companion: making the kernel 30× faster while
  `InteractiveSimulator.afterEvent:879-896` clones a `BitSet` per probe per event
  simply relocates the bottleneck to the GUI.

#### 3.3 Teaching — what a student can do afterwards that they cannot today

- **Run a real program on the CPU they drew, in the GUI, and watch it.** Today the
  answer to "watch it run" is the `Animate` button, which repeats a step **once per
  second** (`InteractiveSimulator.java:136`). After: a bubble sort, a memory test,
  a small interpreter, executing at watchable speed with the register display
  updating. Keystone C §9.2 puts it exactly right — *programs whose behaviour is
  the lesson, rather than programs chosen to fit the simulator.*
- **The debug loop that makes computer organization stick**: write assembly, run it
  on *your own hardware*, get the wrong answer, find the wrong wire. Today each
  turn of that loop is ~5 s at 6004 cycles (§1.4) plus rebuild; a student doing
  twenty turns spends the lab period waiting.
- **Interactive designs become assignable.** A VGA-timing generator needs ~25,000
  clocks *per frame*; at 8 kcycles/s that is three seconds per frame. At Mode C
  speeds a student can drive a simulated display, a pong paddle, a UART echoing
  their keystrokes — the projects that make people stay in the field.
- **"How fast is your CPU?"** becomes a question with an answer, and with P4's STA
  a *second* answer in MHz, and the two are different things — which is itself the
  lesson.
- **Event-driven vs cycle-based as a taught topic.** Keystone C §9.3 makes this
  point and it is a leapfrog in miniature: no peer educational tool has both
  engines, so none can run the same circuit under both and show the class the two
  places they disagree. A one-command `--compare-engines` run that prints exactly
  where the models diverge is a lecture no other tool can give.
- **Combinational loops become a diagnosable mistake** instead of a hung
  simulator. First-years draw accidental feedback constantly.

---

### 4. Competitive position

| tool | has it? | quality |
|---|---|---|
| **Verilator** | yes — the reference design | **verified** as JLS's named model (`grand-architecture.md:334-336`). Elaborate-to-flat, statically ordered, C++ codegen. Fastest open simulator by a wide margin. **Weakness, and it is a real one:** it is a compiler, not a simulator you interact with — no schematic, no editor, no GUI, and waveforms only if you instrument and recompile. |
| **Yosys CXXRTL** | yes, same family | **unverified** in detail; understood to be a C++ netlist-compiling backend in the Yosys ecosystem. Same trade: no schematic. |
| **Questa / VCS / Xcelium** | yes, plus acceleration modes | **unverified** on specifics. Commercial, licensed, and not a thing a first-year gets to run. |
| **Icarus Verilog** | no — pure interpreter | comparable *in kind* to JLS's engine; slow at CPU scale. |
| **GTKWave / Surfer** | n/a — viewers, not engines | |
| **Logisim-evolution** | **unverified**; I could not confirm any compiled or levelized mode | its simulation is understood to be propagation-based per component. If that is right, it is JLS's peer here. |
| **hneemann's Digital** | **unverified**; Digital is understood to have a fast/"run to break" mode, but I could not confirm whether it is levelized-compiled or an optimized interpreter | |
| **DigitalJS** | **unverified**; browser-based over Yosys netlists, so it starts from a flat netlist by construction — which is half of this capability for free | |

**Where JLS could genuinely lead.** Not on raw throughput: JLS is Java over a
schematic model and should not attempt native codegen; Verilator will stay several
times faster and that is fine. The leapfrog is the **combination nobody has**:

> a schematic you drew yourself, running a real program at tens of thousands of
> cycles per second, with the trace window still working, with every node in the
> compiled netlist still mapped back to the element you placed, and with a
> semantically slower reference engine in the same jar that CI proves agrees with
> it.

Verilator gives speed and no picture. Logisim and Digital give a picture and (as
far as I can verify) no compiled engine. Nobody gives both plus the differential
proof that the two agree. And the node-id ↔ drawn-element map (§2.1) is the same
asset P6 identifies as the thing that makes cross-probing possible and that "no
external viewer can ever know" (`README.md:654-659`) — one map, two payoffs.

**Verdict: parity in kind, leapfrog in combination.** Mode T is, as far as I can
verify, unique among schematic-first educational tools (zero-delay levelization
behind an unchanged delay model). Mode C is catching up to a design that has
existed since 2010 — at a fraction of Verilator's raw speed — but attached to a
canvas, a trace window and a grading CLI that Verilator does not have and does not
want.

---

### 5. Relationship to the existing programs

**A new program — call it P7 — with one hard predecessor and several shared
substrates.** It is not a seventh independent lane; it is the item
`docs/capability-roadmap/README.md:700` already lists as a standalone ("Full
cycle-based simulation strategy, 10–16 wk, after P1 + the zero-delay levelization,
needs a *declared alternative strategy* section"), plus the line the spine already
draws at `README.md:844-845` ("P1-S1 also enables zero-delay levelization — the
plane arrays, hoisted out of the value objects"). This document promotes those two
half-rows into one specified program and sizes them honestly.

**Hard dependency: P1-S1 must land first.** Keystone C §6.2 is unambiguous —
**4.32 ns/node with plane arrays vs 22.01 with `BitSet[]`, 5.1×.** Levelizing over
today's value type captures ~1.4× overall and is not worth thirty weeks, *and* it
commits the maintainer to writing the four-state truth tables a second time later
against a different layout. This is the single most important ordering constraint
in the document.

**Hard dependency: #77, for the same reason P1's Stage 5 has it.** The netlist,
the plane arrays and the levelizer belong in `jls.core`, which today holds eight
files and all of them are geometry (`src/jls/core/`: `Bounds`, `Geometry`,
`GridPoint`, `GridSize`, `Orientation`, `SegmentGeometry`, `TextMetrics`,
`package-info`). Same constraint, same answer as `README.md:933-942`.

**Independent and early: the elaborator.** Flattening + net union + dense ids
depends on nothing and is wanted by three consumers — this program, P3's hierarchy
IR, and P5's formula export (`README.md:530-534`). Recommendation: build it during
P3's Stage 1 (total HDL export coverage), lift `HdlExporter`'s `UnionFind` net walk
into it, and let both consume one implementation.

**Shares substrate with P4.** The levelizer's DAG, its topological rank and its SCC
partition *are* P4's timing DAG and its combinational-loop detector
(`README.md:414-416`). Whichever program runs first should build `jls.core`'s graph
layer; recommend it be this one, because it needs SCC handling anyway and STA needs
only the acyclic part.

**Enables P5 at scale**, does not gate it. P5's useful floor (report channel,
timestamp closure, `Assert`, coverage) needs nothing from here. P5's
constrained-random and coverage-driven stimulus needs this to be affordable.

**Prerequisite that is already on the list:** the interactive-engine batching item
(`README.md:699`), 1–2 weeks, must land before or with the GUI integration.

**Not an extension point.** `docs/extension-points.md` should gain **no** row for
this. `docs/grand-architecture.md:314-342` puts the simulation loop on the hot
plane, inside `core`, "invisible to every other module," and `ARCHITECTURE.md:349-353`
repeats it. A simulation strategy is core-internal by decision. That distinguishes
this from P5, which needs three new catalogued seams.

**Ordering, stated as a sequence:**

```
Stage 0 (roadmap)  kernel hygiene ─────────────────── independent
        │
P3 St.1 elaborator extracted from HdlExporter ─────── independent, wanted by 3 consumers
        │
P1-S1   LogicValue lands, plane encoding fixed ────── HARD PREDECESSOR
        │                (with keystone C §8.3's two-form op API stated up front)
        ├── P7-A  levelizer + SCC partition (also gives P4 its DAG)
        │      │
        │      ├── P7-B  Mode T: zero-delay closure. No semantic change.
        │      │           Acceptance: every golden byte-identical, loop −30…−40%.
        │      │
        │      └── P7-C  observability layer (node↔element map, batched watchers)
        │             │
        │             └── P7-D  Mode C: cycle-based, opt-in, declared strategy.
        │                        Requires: semantics doc section + #221 amendment.
        │
        └── P7-E  engine-vs-engine differential harness ── written alongside P7-B, not after
```

---

### 6. Size and risk

**24–35 maintainer-weeks (5.5–8 maintainer-months)**, reconciling with the
roadmap's own 10–16 week line for the cycle-based half.

| piece | weeks | reasoning |
|---|---:|---|
| Elaborator: flatten, union nets across jumps, dense ids, node↔element map, opcode assignment, ESCAPE shim | 4–6 | the `UnionFind` walk exists (`HdlExporter.java:1038-1109`) and is being moved, not invented; the map and the shim are the real work. Chargeable partly to P3. |
| Levelizer + Tarjan SCC + irreducible-block evaluator | 3–4 | textbook algorithms over a graph the elaborator hands over; the bounded-fixpoint block evaluator and its oscillation report are the new part |
| Mode T: zero-delay closure driving the queue, plus proving same-timestamp order identical | 4–6 | the ordering proof is the risk, not the code (§6, risk 1) |
| Observability: traces, probes, watches, VCD, stdout reconstructed from compiled state; batched watcher channel | 3–4 | underestimating this is the classic failure; it also subsumes `README.md:699` |
| Mode C: two-phase cycle stepping, refusal policy, the `docs/simulation-semantics.md` section, the #221 amendment | 6–9 | the normative-document work is a real fraction of this and cannot be skipped |
| Engine-vs-engine differential harness, structural circuit fuzzer, CI wiring, shared truth-table oracle | 2–3 | reusable by P4 and P5 |
| GUI run modes, refusal messages, netlist caching through the op layer | 2–3 | |

**Mode T alone is 11–16 weeks** and delivers the −30…−40% loop win with **zero**
semantic change, zero document amendment and zero golden churn. That is the
shippable slice; everything after it is the order-of-magnitude.

**Top three ways it goes wrong.**

1. **A scheduling divergence between the engines, found late.** Not a wrong gate —
   a wrong *moment*. Same-timestamp clock/data races (`simulation-semantics.md`
   §6.1's read-latest + FIFO), the exact instant a `Register` samples, and
   `Memory`'s level-sensitive write hazard (§8.4) are all places the two models can
   legitimately differ. Keystone C §10 already grades this "high if unmanaged" and
   supplies the mitigation: §6.1's rule *is* a topological evaluation of the
   zero-delay closure, "but this must be **proved** against the goldens, not
   assumed." Mitigation: enumerate the divergences in the normative document before
   writing Mode C; make Mode T's acceptance criterion byte-identity on every
   existing golden, which requires no new oracle; write P7-E alongside P7-B, not
   after.
2. **The observability tax eats the win.** The compiled pass is fast because
   nothing looks at it. Every trace, probe, watch, `Display` element and VCD signal
   is a hole in the straight-line loop, and the GUI — where the speed was supposed
   to matter — is where the holes are. Mitigation: design the channel first, not
   last. A per-run *declared* watch set, sampled at level boundaries into a ring
   buffer, flushed to the GUI on a 50 ms timer, extending the pattern already at
   `InteractiveSimulator.java:838-871`. Nothing per-signal, nothing per-event —
   which is what `grand-architecture.md:328-329` already requires and today does
   not get.
3. **Building it before P1-S1.** Measured, not argued: 22.01 vs 4.32 ns/node. The
   result is a 1.4× engine, a second set of four-state truth tables written against
   a layout that will be thrown away, and a "we did all that and got 40%" narrative
   that kills the value-domain program with it. This is the same failure mode
   keystone C §5 identifies for representation R1, and it is avoidable purely by
   ordering.

A fourth, smaller, worth naming: **re-elaboration cost in the edit-run-edit loop.**
Elaboration is microseconds for 1551 elements, but it runs on every Run press, and
`initSimulation` is already quadratic in the test vector (`SigSim.java:64-74`,
keystone C §2). Cache the netlist and invalidate it through `CircuitOp`/`OpSink`
(#167). Cheap if designed in, ugly if bolted on.

**What would make it not worth doing.**

- **If P1-S1 never lands.** Then this is 1.4× and a second semantics to maintain
  forever. Genuinely not worth it, and the honest recommendation in that world is
  to do nothing here.
- **If Mode T's ordering proof fails** — if some real circuit shows the zero-delay
  closure cannot reproduce FIFO + read-latest ordering byte-for-byte. Then Mode T
  shrinks to "levelize pure fan-out chains only," most of the 82% saving
  evaporates, and the remaining value is Mode C alone, which costs a semantics
  amendment and a second observable model. That is a much worse trade and should be
  re-decided rather than pushed through.
- **If CPU-scale designs stay a population of one.** The honest test is not
  "is it slow" but "who is waiting." Today the evidence is `riscv/` — the
  maintainer's own CPU — plus a compliance plan that routes around the speed and a
  fuzzer capped at 24 straight-line instructions. That is enough evidence to build
  Mode T, which costs nothing semantically. Whether it is enough for Mode C's 6–9
  weeks of normative-document work is a judgement the maintainer should make with
  `bench_kernel.py` numbers from an actual course in hand.

---

### 7. Sources

**Repo, verified at HEAD.**

- `src/jls/sim/Simulator.java:25,27` (`PriorityQueue` + `HashSet` dupCheck),
  `:165-170` (`post`), `:180-202` (`initSimulation`, the only elaboration),
  `:196` (stable-id walk), `:215-240` (`runEventLoop`), `:224-225` (poll +
  dedup remove), `:285-287` (`probeSample`).
- `src/jls/elem/WireNet.java:22,24` (`LinkedHashSet` ends/wires), `:97` (`makeNet`),
  `:272` (`recheck`), `:405` (the `@Nullable BitSet` net value), `:443` (`propagate`),
  `:457-486` (first-active-driver scan), `:476` (conflict warning), `:488-509`
  (sink loop), `:498` (per-sink clone), `:516` (net's own clone), `:522-527`
  (per-propagate probe scan).
- `src/jls/elem/SubCircuit.java:33,35` (`inmap`/`outmap`), `:282-288` (inline save),
  `:571-583` (`initInputs` recursion), `:592-611` (`initSim` recursion),
  `:621-636` (`react`: per-pin event + clone + hash lookup), `:645-652` (`send`).
- `src/jls/Circuit.java:479-485` (`getElementsInStableOrder`), `:1015` (fresh
  `Circuit` per subcircuit instance), `:1300-1422` (`finishLoad`).
- `src/jls/elem/Gate.java:695-720` (the `react` delay discipline; `:708` posts at
  `now+propDelay`).
- `src/jls/elem/Register.java:719-737` (`initSim`), `:747-795` (`react`, the
  remembered-clock edge detection; `:757` reads a 1-bit clock through
  `BitSetUtils.ToLong`), `:766-778` (`PosFF` arm).
- `src/jls/elem/Splitter.java:204-231` (`react`, including the whole-signal null
  fork at `:209-215`).
- `src/jls/elem/Mux.java:530` (`BitSetUtils.ToInt` for the selector).
- `src/jls/elem/Adder.java:255-262` (`resetPropDelay` = `bits * defaultPropDelay`).
- `src/jls/sim/SimEvent.java:23-83` (the sealed `Payload` hierarchy: `PinChanged`,
  `NewValue`, `TriStateOff`, `StateChanged`, `MemoryWrite`, `MemoryRead`,
  `TableOutput`), `:87` (the mutable static sequence counter).
- `src/jls/edit/InteractiveSimulator.java:136` (Animate = "repeat step every
  second"), `:391-420` (the animation `TimerTask`), `:738` (`Editors.of` per
  event), `:838-871` (the one rate-limited channel, 50 ms), `:879-896`
  (`afterEvent`: per-event clone + O(probes) walk).
- `src/jls/hdl/HdlExporter.java:89-91` (net-walk doc), `:166-393` (`buildModel`),
  `:1038-1109` (the `UnionFind` jump-aliasing net walk to lift into core),
  `:418-424` (the `EXPORTED` set).
- `src/jls/core/` — eight files, all geometry; the model/sim extraction (#77) has
  not happened.
- `src/jls/elem/ElementRegistry.java` — 33 registered types.
- 25 files under `src/jls/elem/` implement `public void react(long now, ...)`.
- `riscv/bench_kernel.py:1-14` (docstring: the honest hot-loop baseline),
  and the two runs in §1.4 of this document.
- `riscv/verify.py:15` (`HALF = 1000`), `:18-26` (`gen_clock`), `:31`
  (`cpu.run(max_steps=10000)` predicting the vector length).
- `riscv/fuzz_diff.py:1-6` (thread-pool workaround), `:29-31` (no control flow),
  `:91` (6 workers), `:97` (6–24 instruction programs).
- `riscv/gui/README.md` ("wired into two feedback loops").
- `docs/simulation-semantics.md` §2 (value domain), §3 (ordering, dedup), §6.1
  (read-latest + FIFO, ideal wires), §6.2 (transport delay; the zero-delay element
  list), §7 (the delay table), §8.1 (register edges), §8.4 (level-sensitive memory
  write hazard), §9 (tri-state resolution).
- `docs/batch-interface.md:1-11` (stability contract), §3/§4 (stdout and VCD
  profiles the compiled engine must reproduce), §6 (flag additions are
  CHANGELOG + version material).
- `docs/grand-architecture.md:314-342` (§6, the hot/cold plane split and the
  Verilator reservation), `:328-329` (batched, rate-limited watchers),
  `:334-336` (elaborate-to-flat, "~100×"), `:489-490` (the reservation repeated
  in the one-paragraph determination).
- `ARCHITECTURE.md:341-368` (#221: discrete-event is the sole strategy; the
  revisit trigger; the binding equivalence criterion; the amend-the-document-first
  process clause).
- `docs/capability-roadmap/keystone-c-performance.md` §2 (baseline: 318 ns/event,
  124 µs/cycle, 8,090 cycles/s; the quadratic `SigSim` setup), §3 (37.6 / 47.7 /
  4.9 attribution), §5 (the R0–R4 bake-off), §6.1–6.4 (the escape hatch: 4.32 vs
  22.01 ns/node; the 82.3% zero-delay finding; the interactive engine),
  §7 (queue costs by depth), §8.3 (the two-form op API constraint), §9.2–9.3
  (speed as pedagogy; two strategies as a lesson), §10 (the ordering risk).
- `docs/capability-roadmap/README.md:414-416` (P4's timing DAG = this levelizer),
  `:525-528` (P5 stimulus), `:530-534` (formula export over the flat model),
  `:543` (ERC includes combinational loops), `:654-659` (the node↔element map as
  P6's cross-probe asset), `:699-700` (the two standalone rows this program
  promotes), `:844-845` (the spine's existing levelization line), `:933-942`
  (the #77-before-P1-S5 constraint), `:972-983` (the hot-plane rule is being
  defended against a threat that does not exist).
- `docs/capability-roadmap/sweep-02-timing.md:271-272` (no loop detection today).
- `docs/standards-adoption/05-riscv-compliance.md:121-131` (the free-running-clock
  workaround; 38.2 s vs 4.1 s at 40,000 cycles).
- `docs/extension-points.md:28-36` (no `sim.*` seam; none should be added).

**External, marked.**

- **Verified** (in-repo, from `docs/grand-architecture.md:344-357`'s own source
  list): Verilator is the named reference design for elaborate-to-flat plus
  statically ordered evaluation, with the "~100×" figure quoted there.
- **Unverified**: Verilator's current 2-state/4-state trade-offs and tracing
  overhead; Yosys CXXRTL's internals; Questa/VCS/Xcelium acceleration modes;
  whether Logisim-evolution has any compiled or levelized mode (I could not
  confirm one); whether hneemann's Digital's fast-run mode is levelized-compiled
  or an optimized interpreter; DigitalJS's evaluation strategy beyond the fact
  that it consumes Yosys netlists. I did not fetch any of these this session and
  have not asserted anything about them beyond what is marked.
