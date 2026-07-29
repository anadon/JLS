## Fault simulation, DFT teaching, and power/activity analysis

*Leapfrog sweep 05. Two capabilities that the six capability programs
(`docs/capability-roadmap/README.md`) do not own, because 304 standards produced
only one attached entry between them (#72 SAIF) and fault simulation has no
standard at all. Both are cheap **given a working simulator**, because in both
cases the data already flows through the hot loop and is discarded. Repo claims
carry `file:line` at HEAD. External claims are marked verified or unverified.*

---

### What is missing today

#### (a) Fault simulation — absent, and absent without a workaround

`grep -rni "stuck.at\|fault\|ATPG\|testabilit\|test.pattern"` over `src/` returns
**zero simulation-domain hits**. Every match is prose: `jls/collab/net/*` on
fault-tolerant sessions, `jls/edit/*` on default/fallback behaviour. There is no
fault object, no fault site, no coverage metric, no test-pattern concept, and no
place in the element model where "this pin is stuck" could be expressed.

The three structural facts that make it *cheap* are all in the tree already:

- **A single funnel per direction.** Every value in JLS reaches an element through
  `Input.setValue` (`src/jls/elem/Input.java:59`) and leaves through
  `Output.propagate` (`src/jls/elem/Output.java:136`). Both write
  `Put.currentValue` (`src/jls/elem/Put.java:385`). There is no third path. A
  stuck-at fault is a clamp at those two lines.
- **Fault sites already have permanent names.** `ElementId`
  (`src/jls/elem/ElementId.java:36`) mints a stable id per element that survives
  save/load and undo; `Circuit.getElementsInStableOrder`
  (`src/jls/Circuit.java:479-485`) sorts by it. `(stable id, put name, bit)` is a
  fault site identifier that is stable across sessions and diffable in an
  assignment repository. Nothing needs inventing.
- **Re-simulation in-process is already clean.** `Simulator.initSimulation`
  (`src/jls/sim/Simulator.java:177-201`) clears `now`, the queue and `dupCheck`,
  re-runs `initInputs` and re-runs `initSim` on every element in stable order;
  `Memory` keeps the initial image separate from the running store
  (`initMem` → `mem`, `src/jls/elem/Memory.java:981-987`) and clears its write
  history at `:1320-1321`. So "load once, simulate N faulty machines" is
  supported by the existing lifecycle — it has simply never been asked for.

**The workaround, and it is real.** A student who wants to see a stuck line today
edits the `.jls` file to splice a `Constant` where a wire was, re-runs
`jls -b -t vectors.txt`, and diffs the stdout by eye. That works for exactly one
fault; it changes the element set, so stable ids move and net order moves — and
net order is load-bearing, because `WireNet.propagate:443-479` resolves a
multi-driver net to "the first active driver in net order"
(`docs/simulation-semantics.md` §9). It produces no coverage number, no fault
list, and no notion of which faults are equivalent.

**The institutional workaround is stronger evidence.**
`docs/standards-adoption/11-costed-rejections.md:813` costed IEEE 1149.1 boundary
scan at 15–25 maintainer-days and then deferred it "until a DFT or
test-engineering course asks." The capability roadmap already overturned the
deferral half of that (`README.md:45`) while keeping the honesty rule. But note
what the deferral reveals: **the only DFT item anyone has ever costed for JLS is
the one with a standard attached.** Fault simulation — which is the thing boundary
scan exists to deliver patterns *for*, and which is the part actually taught in a
first course — was never costed, because no survey row points at it.

#### (b) Power and activity — the data is computed and thrown away, twice

- `Output.propagate` (`src/jls/elem/Output.java:139-145`) opens with source-side
  change detection: `if (currentValue.equals(value)) return;`. Every reaching line
  in that method is, by construction, **a transition**. Nothing counts them.
- `WireNet.propagate` (`src/jls/elem/WireNet.java:443`) resolves the net value,
  stores it at `:518-522`, and hands probed nets to the trace at `:525`. Every
  net-level transition in the entire simulation passes through this method.
  Nothing counts them.
- The measured volume, from `docs/capability-roadmap/keystone-c-performance.md`
  §2 on the 6004-cycle RV32I workload: **2,331,793 events fired**, of which
  378,129 are `NewValue` payloads — i.e. roughly 378k output transitions plus the
  net-level fanout, per run, discarded.

**JLS already does activity accounting — for exactly one element type, for exactly
one dialog.** `Memory` keeps a `LinkedList<WriteRecord> activity` bounded to
`ACTIVITY_LIMIT = 10_000` (`src/jls/elem/Memory.java:1017-1024`, appended at
`:1423-1430`), whose stated purpose is "to populate the activity dialog"
(`:1000-1001`) and whose only consumer renders it as a string (`:1482-1489`).
That is a per-element switching record, bounded, GUI-only, and unavailable to the
batch surface. The concept exists; it has been implemented once, for one element,
and generalized nowhere.

**The workaround here is post-processing the VCD**, and JLS ships the pattern:
`examples/autograde/autograde.py:11-13` includes "a deliberately minimal,
dependency-free" VCD parser and grades against the emitted waveform. Counting
toggles from that file is the obvious move — and it is wrong for three reasons
the file format itself creates: the VCD carries only **watched** elements and
**probed** nets (`docs/batch-interface.md` §4.1), so the denominator is whatever
the student happened to mark; consecutive equal values are deduplicated at
recording time (`BatchSimulator.afterEvent:174`), so a glitch that returns to its
prior value inside one timestamp is already gone before the file is written; and
per-bit toggle counts are unrecoverable for a signal whose whole value went HiZ
(`BatchSimulator.vcdValue:550-554` emits `bz`, because "JLS has no per-bit HiZ",
`docs/batch-interface.md` §4.3).

Finally, `docs/simulation-semantics.md` §7 publishes a per-element delay table
(AND=10, NAND=5, Register=50, Memory=100) as normative — a library datasheet with
the technology removed. There is no corresponding **energy** column, no
capacitance, no area, no fanout limit. Power is not modelled badly in JLS; it is
absent as a concept, on both the drawing and the report.

---

### The capability

#### (a) Fault simulation

**The fault site is a `Put`, not a `WireNet`.** Recommended, not surveyed. A JLS
`WireNet` is the whole fanout structure — one `Output` driving N `Input`s
(`WireNet.propagate:488-513`). Placing the fault on the `Output` gives you the
**stem** fault; placing it on each `Input` gives you the **fanout branch** faults.
That stem/branch distinction is the first non-obvious thing a testability unit
teaches (a branch fault can be detectable when the stem fault is not, and vice
versa), and here it falls directly out of a data structure JLS already has. A
net-level fault model would collapse the distinction and delete the lesson.

**The injection mechanism is a clamp flag on the `Put`, set at elaboration, not a
lookup.** Two fields beside `Put.currentValue` (`src/jls/elem/Put.java:385`):

```
/** Bits held at a fixed value by an injected fault; empty when unfaulted. */
private @Nullable BitSet faultMask;
/** The values those bits are held at. */
private @Nullable BitSet faultValue;
```

`Input.setValue` and `Output.propagate` gain one predictable branch on a field
already in the same cache line as the value they are about to write. When no fault
is active — every run that exists today — the branch is never taken, and every
golden stays byte-identical. This is what `docs/grand-architecture.md` §6's
hot-plane rule requires: no capability lookup, no cross-module call, no map probe
on the inner loop. **The alternative — a `Map<Put,Fault>` consulted per propagate
— is precisely the shape of the `dupCheck` `HashSet` that keystone C measures at
25.4% of the event loop, and it must be rejected on that measurement.**

Per-bit clamping matters: a stuck-at fault is a property of one wire, and JLS
signals are up to 32 bits. With today's `BitSet` the clamp is
`v.andNot(mask); v.or(value)` — two `BitSet` ops on a value already being copied.
With **P1's `Word(int width, long a, long b, long u)`** it is
`a = (a & ~mask) | (val & mask)` — two ALU ops, free.

Rejected alternative: structural injection (splice a `Constant`). It re-elaborates
the circuit per fault, perturbs stable ids, perturbs net order (which §9 makes
semantically load-bearing), and cannot express a per-bit fault on a 32-bit net
without a `Splitter` mesh. It is what a user does today by hand, and it is the
thing being replaced.

**Collapsing rules.** Two equivalences, reported separately.

1. *Structural equivalence at gates*, the textbook table, over the six gate classes
   in `src/jls/elem/` (`AndGate`, `OrGate`, `NandGate`, `NorGate`, `NotGate`,
   `XorGate`): AND input-SA0 ≡ output-SA0; OR input-SA1 ≡ output-SA1; NAND
   input-SA0 ≡ output-SA1; NOR input-SA1 ≡ output-SA0; NOT input-SA0 ≡ output-SA1
   and input-SA1 ≡ output-SA0; XOR collapses nothing (which is *why* XOR-heavy
   circuits are the hard ATPG case, and is worth showing).
2. *Zero-delay wiring collapse.* `docs/simulation-semantics.md` §6.2 lists the
   elements that propagate within the same timestamp — `Splitter`, `Binder`,
   `InputPin`, `OutputPin`, `SubCircuit` boundary, `Constant`, plus `Extend`. These
   are renamings, so a fault on a `Splitter` output bit is the same fault as on the
   corresponding input bit. On the RV32I census in keystone C §2 that is
   **34 `Splitter` + 9 `Binder` + 5 `Extend` of 225 logic elements** — the single
   largest reduction available in a JLS circuit, and it is a pure graph walk.
3. *Fanout-free stem/branch collapse*: an `Output` whose net has exactly one
   attached `Input` collapses with it. The driver/sink arrays P1-S1 caches at
   `WireNet.makeNet`/`recheck` make this O(1) instead of a `LinkedHashSet` walk.

**Dominance collapsing stays off by default.** It reduces further but is harder to
explain and it changes the coverage denominator. **Report both counts** — collapsed
and uncollapsed — because "97% coverage" means two different things depending on
the denominator, and making students see that is half the point.

**The coverage metric, defined normatively rather than assumed.** Three numbers,
because textbooks make students compute all three and tools quote them
interchangeably:

- **Fault coverage** = detected / total faults.
- **Test coverage** = detected / (total − proven-untestable).
- **Fault efficiency** = (detected + proven-untestable) / total.

"Detected" requires an **observable set**, and this is the design decision that
must be written down rather than inherited. The honest default is *top-level
`OutputPin`s only* — that is what a tester can actually see, and it is already a
distinguished set in the tree: `JLSStart.displayResults` (`src/jls/JLSStart.java:672`)
prints exactly `Register`, `Memory` and `OutputPin`
(`docs/batch-interface.md` §3.2). Offer `--observe=pins|watched|all` with `pins`
as the default, and print the choice in the report header, because a coverage
number without its observable set is not a number.

**Fault dictionary.** Per fault, a signature: the set of (vector index, observable,
faulty value) triples where the faulty machine diverges. Stored compactly as a
bitvector over (vector × observable) positions. Two faults with identical
signatures are **diagnostically equivalent** — a *second and different* equivalence
relation from the structural collapsing above, and the difference between "these
two faults behave the same everywhere" and "these two faults behave the same on
*your* test set" is one of the better lessons available. Emit the aliasing classes
alongside the dictionary.

**Parallel fault simulation — assessed, and deliberately not first.** The classic
trick packs 64 machines into one machine word.

- **Today it is unavailable.** A JLS value is a `BitSet` already spent on signal
  width; a 64-machine pack needs `BitSet[64]`, which keystone C measured as the
  worst representation in the study (the two-`BitSet` option at **~81 ns/op, 4×
  slower than today**; the `BitSet[]` levelized pass at **22.0 ns/node against 4.32
  for plane arrays**). Building bit-parallel fault simulation on `BitSet` would
  reproduce that measurement.
- **After P1**, parallel-*pattern* (PPSFP: 64 vectors, one fault) works cleanly for
  the **1-bit gate-level** portion, where the `Word`'s `long` is otherwise 63/64
  empty. This is exactly what Atalanta does (verified: FAN plus "parallel pattern
  single fault propagation"). It does *not* work for the datapath: at 32 bits the
  word is already spent, and there the available axis is parallel-*fault*, which
  needs a `long[64]` plane per signal — i.e. the **compiled/levelized engine's data
  layout**, not the interpreter's.
- **Therefore: serial first, and it is enough.** A first-year lab is a 4-bit adder:
  ~40 collapsed faults × a 16-vector run of a few milliseconds. Even the RV32I CPU
  is 0.74 s/run warm (keystone C §2), so a 500-fault campaign is ~6 minutes — slow
  but not disqualifying, and **fault dropping** (stop simulating a fault the moment
  it is detected) typically removes an order of magnitude on a good test set. Build
  fault dropping, a single cached good-machine reference run, and the observable
  diff. Build parallel fault simulation later, *on* the compiled engine's plane
  arrays, never beside them.

**ATPG: build PODEM, not the D-algorithm.** Recommendation, with reasons. PODEM
only ever assigns *primary inputs*, so it never has to justify an internal line
value and never backtracks over internal consistency; it is ~300 lines over a
netlist JLS already has plus a controllability heuristic; and it handles the
reconvergent, XOR-heavy circuits (parity trees, ECC, the carry-lookahead adder a
student just drew) on which the D-algorithm's runtime is the classic pathology.
Every standard text presents PODEM as the fix. The D-algorithm's residual teaching
value is *watching the D-frontier move*, which is a GUI animation over the same
netlist — ship it as a visualizer, not as the production engine.

**ATPG needs a combinational view, and that view already has an owner.** JLS
circuits are sequential. The honest scope is *combinational ATPG over the
combinational blocks cut at sequential boundaries* — which is exactly the timing
DAG **P4's static timing analyser** builds ("a timing DAG over
`Circuit.getElementsInStableOrder()` and the `WireNet` structure, cut at
sequential boundaries", `README.md:414-416`). ATPG should consume that extraction,
not duplicate it. **Full-scan mode** — treat every `Register` as a scan cell so the
whole design becomes combinational — is both the way to make ATPG work on a real
design *and* the DFT lesson worth teaching, and its scan-cell element is the same
element the boundary-scan standalone item (#129) needs.

**Batch and report surface.** New flags, additive:
`-faults <file|auto>`, `-faultreport <file>`, `-faultdict <file>`,
`-atpg <file>`. `docs/batch-interface.md` §6 explicitly permits this: "a new
optional output gated behind a new flag" is minor-version material. **Fault results
must not touch stdout** — §3's format and its three-element whitelist are a frozen
contract, and `BatchSimulationGoldenTest.watchedElementsPrintInNameOrder` pins it
byte-exactly. The report artifact should be P5's, not a new one: xUnit XML plus a
machine-readable fault report, through P5's pending `app.report-writer` seam
(`README.md:966-970`).

#### (b) Power and activity

**Counting mechanism: primitive counters on the objects, no map, no allocation.**
Add to `WireNet` and to `Output`:

```
long toggles;          // whole-signal transitions
long[] bitToggles;     // per-bit, allocated once at elaboration; null when off
long timeHigh;         // integral of value over time, for duty cycle
long lastChangeTime;
```

Increment sites, both immediately after an existing change decision:
`Output.propagate` after the `currentValue.equals(value)` early return
(`src/jls/elem/Output.java:139-145`), and `WireNet.propagate` where the resolved
value is stored (`src/jls/elem/WireNet.java:518-522`), directly beside the
existing `sim.probeSample` call at `:525`.

The cost discipline is decided by keystone C's profile — the loop is **37.6%
`BitSet` work, 47.7% queue bookkeeping, 4.9% actual logic**. A `long` increment on
an object already in cache is roughly a nanosecond and is dwarfed by the
`BitSet.clone` two lines above it. **A `Map<WireNet,long[]>` lookup is not**, and
must be rejected for the same reason as the fault-lookup: it is the `dupCheck`
`HashSet` shape that already costs 25.4%.

**Per-bit counting is where the allocation hazard lives.** With today's `BitSet`,
per-bit toggles need `old.xor(new)` — one allocation per propagate, on the hottest
path in the program. Gate it: whole-signal counting always available at ~zero cost,
per-bit counting only behind the flag, and the acceptance criterion is that
`RiscvCpuGoldenTest` runtime is unchanged within noise when the flag is off. After
P1 it is `Long.bitCount(oldA ^ newA)` and the hazard disappears.

**The gate itself.** Reuse the shape `BatchSimulator.afterEvent:144` already uses
(`if (!JLSInfo.printTrace && vcdFileName == null) return;`) but **not the
mechanism**: read a `boolean` field on the simulator, not a `JLSInfo` static —
`JLSInfo` is the ~640-reference public-static hub `docs/grand-architecture.md` §3
names as the thing #77 exists to dissolve.

**Rejected alternative: derive everything from the VCD in a script.** It is the
cheapest thing that could possibly work and it is wrong, for the three reasons
given in "What is missing today": the VCD's signal set is whatever the student
marked watched, its recording-time dedup has already destroyed intra-timestamp
glitches, and per-bit history is lost on any HiZ vector. Those are not
implementation gaps — they are consequences of a *frozen* format contract.

**Fidelity, layered, with the honest ceiling stated.** This is the part where a
teaching tool can do real damage, so the tiers are the design:

| Tier | Claim | Requires | Honest? |
|---|---|---|---|
| 0 | "Net `carry3` toggled 4,812 times." | nothing | Yes — exact, no model, no units |
| 1 | Activity factor = toggles / (2 × cycles); duty cycle = timeHigh/total | a declared clock or window | Yes |
| 2 | "Your design does **3.1×** the switching of the reference." Dimensionless ratio. | a named reference circuit | Yes, **with one caveat printed**: the ratio cancels the unknown per-transition energy only if the two designs have comparable element mixes. All-NAND vs. `Memory`-heavy does not cancel. |
| 3 | Weighted switching activity, Σ(toggles × fanout) | net fanout, which the net already knows | Yes, **if labelled a proxy** — it is the standard academic proxy and is defined as one |
| 4 | **Picojoules** | P6's Liberty `capacitance` + a named corner | **Not without the library, ever** |

**The rule to write into `docs/simulation-semantics.md` before the code:** *JLS may
report counts and ratios unconditionally; it may report joules only when a named
technology library is loaded, and the report must name the library and corner.*
Write it first, because JLS has already made this exact mistake once:
`docs/batch-interface.md` §4.2 concedes that every VCD carries
`$timescale 1 ns $end` while "JLS time units are abstract" — byte-pinned by
`VcdExportGoldenTest.clockedRegisterVcdMatchesGoldenByteForByte` — and GTKWave
confidently labels the axis in nanoseconds. A picojoule figure would be the same
error with worse consequences, because a number with a unit next to a circuit gets
quoted and graded on.

**And the honest ceiling is higher than it looks, because the real units are
somebody else's job.** OpenSTA reads SAIF or VCD switching activity together with
a Liberty library and produces a power report (`read_saif` / `read_vcd` +
`report_power`) — verified; Antmicro published exactly this flow with Verilator
generating the SAIF (verified via published blog posts, July 2025). So the correct
architecture is: **JLS emits SAIF; OpenSTA computes the joules, from sky130's real
library, in the open flow P6 is already building toward.** That makes #72 SAIF not
a checkbox but the interface to a genuine power flow, and it means JLS never has
to invent a number it cannot know. This is the same delegation stance
`docs/grand-architecture.md` §9 already records for HDL ("orchestrate external
tools, never reimplement").

**Report surface.** `-activity <file>` writes SAIF; `-activityreport <file>` writes
the human report (per-net toggles, activity factors, duty cycles, the top-N
switching nets, the ratio to a named reference). Additive behind new flags, same
§6 argument as the fault flags. GUI: a **toggle heat map on the schematic** —
colour each wire by its switching count after a run. That is the deliverable no
waveform viewer can produce, and it costs almost nothing once the counters exist.

**One integration constraint that saves a rebuild.** P5's coverage model includes
"net toggle coverage per bit" (`README.md:518-522`). That is *the same counter*.
Build it once, under P5's `sim.coverage-collector` seam, and let P4's energy story
consume it. Building the toggle counter twice — once for coverage, once for power —
is the obvious mistake and it is currently latent in the roadmap, because
`README.md:421` and `:477` assign activity to **P4** while `:518` assigns toggle
coverage to **P5**.

---

### What it unlocks

**Standards.**

- **#72 SAIF** — directly, and it is already named the cheapest real item in six
  sweeps (`README.md:47`). The activity work *is* SAIF.
- **#137 STIL / #138 WGL** — a fault campaign produces exactly what these formats
  carry: an ordered vector set with expected responses at named observables. The
  standalone item already prices the printers at 3–5 weeks over
  `BatchSimulator.getTraceSamples` (`README.md:696`); fault simulation gives them a
  *reason* — patterns generated for coverage rather than hand-written for a demo.
- **#129 IEEE 1149.1 / #134 IEEE 1500** — the scan-cell element that full-scan ATPG
  needs *is* the boundary-scan cell at a different boundary. Building either builds
  most of the other, which changes the joint cost of the pair.
- **#87 Liberty (power groups), #92 ALF** — become reachable once P6's cell layer
  exists and there is an activity number to multiply.
- **#53 UCIS** — toggle coverage is one of the measures a coverage database carries;
  it arrives with P5's collector rather than separately.
- **#96 IEEE 2416 stays out**, correctly (`README.md:1047`) — system-level power
  modelling above the gate is another tool class.

**Engineering capabilities.**

- A coverage number for a test-vector set, which is a *different and better*
  quality signal than "does it pass": a `-t` file can pass every check while
  leaving the entire carry path unexercised.
- Fault dictionaries, and with them diagnosis: "your board fails; here is which
  fault signature matches the observed responses."
- Combinational ATPG, so a test set can be *generated* instead of guessed.
- Redundancy detection for free: a fault PODEM proves untestable is a line whose
  value cannot be observed — i.e. logic that can be deleted. That is a synthesis
  result arriving out of a test tool, and it is the same fact the don't-care work
  in P1 exploits from the other direction.
- SAIF out to OpenSTA, i.e. a real power number for a drawn circuit.
- A switching heat map on the drawing.

**Teaching capabilities — what a student can do afterwards that they cannot today.**

- **Circle a wire and say "stuck at 0", and watch what happens.** Today this is a
  paper exercise for every student in every course that teaches it. Testability is
  a standard topic in a first digital-design sequence, and it is currently the only
  major topic in that sequence with **no tool support anywhere in the educational
  tier** — students compute fault coverage by hand on a five-gate circuit and never
  see it on anything larger.
- **Write a test set, get a percentage, and improve it.** "Your 8 vectors detect
  62% of the faults in your adder. Add vectors until you reach 95%." That is a lab
  with a measurable objective and an obvious next action, and it is gradeable
  headlessly by the existing batch surface.
- **See why a fault is undetectable.** PODEM returning "untestable" on a redundant
  AND term, with the sensitized path it tried highlighted, teaches redundancy and
  observability at the same time — and connects directly to the consensus-term
  lesson P4's glitch detector delivers from the timing side (add the consensus term
  to kill a hazard; discover you have created a fault you can no longer test; that
  trade-off is real and is currently unteachable in any free tool).
- **Stem versus branch.** Draw a fanout, put SA1 on the stem and SA1 on one branch,
  and find the vector that distinguishes them. Impossible to motivate on paper,
  trivial once the fault site is a `Put`.
- **Full scan, and why chips have scan chains.** Stitch the registers into a chain,
  watch the sequential circuit become combinational, watch the fault coverage jump,
  and count the area you paid for it. Then the boundary-scan lab
  (`sweep-05` §G: "your chip has 40 pins, one is shorted to its neighbour, find it
  without touching a probe") sits directly on top.
- **Power as a third axis.** Today JLS teaches correctness and, implicitly, speed.
  A student can compare two functionally identical designs and see one do 3× the
  switching — and then be told *why*: this one glitches. Which requires P4's glitch
  detector, and is the strongest possible motivation for hazard elimination,
  because "glitches cost energy" is a number rather than an assertion.
- **Clock gating, drawn.** Add an enable, watch the toggle count on the downstream
  register collapse to near zero, and see the cost in the added gate. That is a
  complete lab and it needs nothing but the counters.
- **"Which net in my CPU switches most?"** — a heat map on the drawing of the
  RV32I fixture, answering a question no student in this tool class has ever been
  able to ask.

---

### Competitive position

**Fault simulation and ATPG.**

| Tier | Who has it | How well |
|---|---|---|
| Commercial | Synopsys TestMAX ATPG (formerly TetraMAX), Siemens Tessent, Cadence Modus | Excellent and expensive. All consume a **gate-level netlist plus a Liberty library**; none consumes a schematic; none draws anything. Sign-off tools with several-hundred-page manuals. *(Product existence verified by common knowledge of the tool class; specific current capabilities and pricing unverified.)* |
| Open | **AUCOHL/Fault** — ATPG, scan stitching, JTAG insertion, stuck-at fault simulation, Apache-2.0, OpenLane-targeted (**verified** from its repository and documentation). **Atalanta** — FAN ATPG + parallel-pattern single-fault-propagation fault sim, combinational, ISCAS bench format (**verified**). **HOPE** — three-valued parallel fault simulation for synchronous sequential circuits (**verified** from published description) | Real and free. All three are **netlist-in, text-report-out**. No schematic, no GUI, no animation, no notion of "the wire the student just drew." |
| Peer educational | **hneemann's Digital: NO** — its published feature list names no fault, stuck-at, ATPG, or test-pattern capability (**verified** against the repository's feature list). **Logisim-evolution: NO** — same check, same result (**verified**). **DigitalJS: unverified**, but it is a Yosys-netlist viewer/simulator and there is no reason to expect it. | Nothing. |

**One claim I found and am flagging rather than repeating.** A 2021 Hackaday
survey-of-logic-simulators page carries an assertion that Digital "is able to
simulate faults (open to Z, stuck at 0, etc.) and generate a fault dictionary."
That is **contradicted by Digital's own feature list**, which names no such
capability. Treat it as **unverified and probably mistaken** — most likely a
comment describing a different tool in the same thread. It is worth naming because
it is the single search result that would otherwise make this section wrong, and
because a maintainer doing the same search will find it.

**Verdict: LEAPFROG, and the incumbent weakness is specific rather than
manufactured.** JLS would not beat TestMAX at ATPG and should not try. The gap is
elsewhere and it is structural: **fault simulation is a fundamentally *structural*
concept — the fault is at a named pin on a named gate, and the test is a
*path* from that pin to an output — and it is taught with a drawing.** You circle
the stuck line. You highlight the sensitized path. You watch the D propagate. And
**no tool in existence draws it**, because the entire fault-simulation tool class
operates on netlists, and the entire schematic tool class does no fault simulation.
JLS is the only tool positioned in both halves. A fault simulator that highlights
the stuck pin in red on the schematic, animates the D-frontier advancing, greys out
the fault sites no vector reaches, and lets you click an undetected fault to see
the vector PODEM generated for it — that is a thing that does not exist at any
price, commercial or free. It is also, not incidentally, the version a first-year
student needs.

**Power and activity.**

| Tier | Who has it | How well |
|---|---|---|
| Commercial | Synopsys PrimePower, Cadence Joules/Voltus, Siemens PowerPro | Excellent, expensive, netlist + library only. *(Existence verified as tool class; capabilities unverified.)* |
| Open | **OpenSTA `read_vcd`/`read_saif` + `report_power`**, inside OpenROAD (**verified**); **Verilator `--coverage-toggle`** per-bit toggle counters, with SAIF emission demonstrated by Antmicro (**verified**) | Genuinely good, and the flow works today — for RTL and netlists. Nothing in it renders a schematic. |
| Peer educational | **Digital: NO. Logisim-evolution: NO** (both **verified** against feature lists). | Nothing. |

**Verdict: parity on the data, a modest leapfrog on presentation — and the leapfrog
should not be oversold.** The numbers JLS can produce without a technology library
are strictly weaker than what OpenSTA produces with one, and the right move is to
hand OpenSTA the SAIF rather than compete with it. What JLS uniquely has is two
things. First, **the drawing**: a per-net toggle heat map on the schematic the
student authored, which no waveform viewer and no netlist power tool can show,
because neither has the schematic. Second, **the glitch connection in one tool**:
P4's glitch detector makes a hazard *visible*, the toggle counter makes it
*countable*, and the energy ratio makes it *cost something* — three views of one
phenomenon on one drawing. Verilator can count the toggles; it cannot show you the
gate. The honest headline is: *JLS becomes the first schematic-first tool in which
power is a design axis at all, and its route to real units is emitting SAIF into an
open flow that already works.*

---

### Relationship to the existing programs

**Power and activity: extends P4 in the roadmap as written, but should be moved.**
`README.md:421` and `:477` place "switching activity and power / activity + SAIF
2–3" inside **P4**, while `:518-522` places "net toggle coverage per bit" inside
**P5**. Those are the same counter. Recommendation: **build the counter under P5**,
where it lands behind the `sim.coverage-collector` seam P5 already has to catalogue
(`README.md:966-970`), and where the program depends on nothing and runs fully in
parallel with everything else. P4 then *consumes* it for the energy story once the
delay model and glitch detector exist. Net effect on the totals: P4 loses 2–3
weeks, P5 gains ~1 (the counter is most of what its toggle-coverage line already
paid for), and the duplication disappears.

Ordering constraints for the activity work, all of them soft:
- Nothing blocks it. It is buildable at HEAD.
- **P1-S1 makes per-bit counting free** (`Long.bitCount` instead of `BitSet.xor`),
  so per-bit should be gated until then or accept the allocation behind a flag.
- **P4's glitch detector** turns the counts into the hazard lesson.
- **P6's Liberty layer** is the *only* thing that authorizes a number with a unit.
- **Stage 0's kernel hygiene** should land first for a clean benchmark: keystone C
  measures `SigSim`'s quadratic `String +=` at 0.57 s of a 1.31 s run
  (`src/jls/elem/SigSim.java:64,67,71,74`), and any end-to-end measurement of the
  counter's overhead will be swamped by it and report noise.

**Fault simulation: a new program — call it P7 — depending on P5.** It is not a
sub-item of anything existing, and the test for that is concrete: it edits **zero**
of the 25 `react` implementations, adds **no** value-domain concept, changes **no**
timing semantics, and adds **no** element (until the scan cell). Its deliverable is
a new *analysis over the elaborated netlist* plus two clamp fields on `Put`. That
shape matches P5 more closely than anything else, and it should share P5's
infrastructure rather than grow a parallel one:

- **Depends on P5** for the report channel and the exit-status contract
  (`README.md:537-541` — "this must be designed first, because it is a change to a
  promise"), for the `app.report-writer` seam, and for the observable-set concept.
  Do not ship fault results before that channel exists, or they will land on stdout
  and break a frozen format.
- **Depends on P4's timing-DAG extraction** for the ATPG half only — the
  combinational cut at sequential boundaries (`README.md:414-416`). The *fault
  simulation* half needs none of it.
- **Gains honesty from P1-S2 (X producible), and this is a real caveat rather than
  a nicety.** In the two-state domain `LogicElement.initInputs` (`:476-481`) zeroes
  every input at every depth, so the good machine and the faulty machine agree at
  t=0 for reasons that are an artifact of the value model, and an SA0 fault on a
  never-driven line is classified untestable when the truth is "unknown." Three-
  valued fault simulation is the standard for exactly this reason (HOPE uses
  0/1/X — verified). **Ship serial fault simulation before P1 if the schedule
  wants it, but the report must carry a stated caveat and the normative document
  must say which faults the two-state domain cannot honestly classify.**
- **Gains reach from P2 and P3** without waiting on them: more element types means
  more fault sites and more collapsing rules; hierarchy means faults inside
  subcircuits get qualified names.
- **Feeds the #129 standalone item.** The scan-cell element that full-scan ATPG
  needs is the boundary-scan cell at a different boundary, so the pair should be
  scheduled together and the joint cost is well under the sum.

Neither capability disturbs the hot-plane rule. Both are gated flags reading
primitive fields inside `core`, exactly as tracing already is
(`BatchSimulator.afterEvent:144`'s single early return), and neither dispatches
through an extension point inside the loop.

---

### Size and risk

**Power and activity: 3.5–5 maintainer-weeks.**

| Piece | Weeks | Reasoning |
|---|---:|---|
| Counters on `WireNet`/`Output` + gate + per-bit + benchmark | 1–1.5 | Two increment sites; the work is the acceptance benchmark proving `RiscvCpuGoldenTest` is unchanged in time and bytes |
| SAIF writer | 0.5–1 | A printer over the counters; SAIF's content is literally toggle counts and state times |
| Activity report + ratio-to-reference + WSA + heat map | 1–1.5 | The heat map is a `CircuitRenderer` pass; the report is the design work |
| Normative doc + the units honesty rule | 0.5 | `simulation-semantics.md` §7 gains an energy discussion; `batch-interface.md` gains two flags |
| OpenSTA hand-off recipe + a CI test that OpenSTA parses the SAIF | 0.5–1 | The interop claim must be tested or it is a claim |

Higher than sweep-02's 2–3 weeks (`sweep-02-timing.md:615`) because that figure
covered counting and SAIF only, and omitted the report design, the honesty rule,
and the interop test. **+1–2 weeks for energy in real units once P6's Liberty layer
exists.**

*Top three ways it goes wrong.*
1. **The counter allocates.** Per-bit counting via `BitSet.xor` puts one allocation
   per propagate on the loop keystone C measured at 37.6% `BitSet` overhead. This
   is the single failure mode. Gate it, or wait for P1's `Word`.
2. **Picojoules leak into the UI.** The moment a number with a unit appears beside
   a circuit, it gets quoted and graded on, and the tool is lying — the same class
   of error as the `$timescale 1 ns` line already in every VCD. Write the rule
   before the code.
3. **The reference ratio is taken across incomparable element mixes** and reports
   nonsense. The report must refuse to print a ratio without a named reference and
   should print the element-mix delta beside it.

*What would make it not worth doing:* if SAIF had no consumer and the ratio were
never used in a lab. Neither holds — the SAIF consumer is verified (OpenSTA) and
the ratio's consumer is the instructor. This is the weakest "not worth it" in the
sweep, and it is the cheapest real item in it.

**Fault simulation: 12–18 maintainer-weeks full; useful floor 6–9.**

| Piece | Weeks | Reasoning |
|---|---:|---|
| Fault-site model, `Put` clamp, fault-list generation, collapsing | 2–3 | The clamp is small; the collapsing rules across 6 gate classes + 7 zero-delay wiring elements + fanout-free nets are the work, and each needs a test |
| Serial fault-sim driver: N runs, good-machine reference, fault dropping, observable diff | 2–3 | Re-simulation lifecycle is already sound (`initSimulation`, `Memory.initMem`), so this is orchestration plus a lot of care about determinism |
| Coverage report, fault dictionary, batch flags, normative section | 2–3 | Three coverage metrics defined normatively; the dictionary's signature encoding; a frozen-format-safe report surface |
| GUI: fault list, red stuck-pin highlight, sensitized-path overlay, "show the detecting vector" | 2–3 | This is the leapfrog and it should not be cut |
| PODEM ATPG over the combinational cut | 4–6 | The algorithm is ~2 weeks; the DAG extraction/sequential cut (shared with P4's STA), full-scan mode, and the "why is this untestable" explanation are the rest |
| Scan-cell element + full-scan stitching | 2–3 | Shared with the #129 boundary-scan item — count it once across both |
| *(later)* parallel-pattern fault sim on the compiled engine | +4–6 | Do not attempt before the compiled engine exists |

**The useful floor is 6–9 weeks**: fault-site model + clamp + collapsing + serial
simulation + the coverage report. That alone delivers the entire first-year
testability lab and a gradeable coverage number, with no ATPG and no scan.

*Top three ways it goes wrong.*
1. **The clamp becomes a lookup.** If the fault is found by consulting a map rather
   than by reading a field on the `Put`, every run pays — including the 100% of runs
   with no fault — and the measured 47.7%-queue / 37.6%-value profile gets worse.
   **Acceptance criterion, stated up front: with fault support compiled in and no
   fault active, `RiscvCpuGoldenTest` is byte-identical and its runtime is within
   measurement noise.**
2. **Coverage numbers that are not honest, because X does not exist.** In the
   two-state domain, `initInputs` supplies a zero that the circuit did not earn,
   and faults get classified on an artifact. The number will be quoted; it must
   carry its caveat from the first release, in the report header and not only in
   the documentation.
3. **Scope creep into sequential ATPG.** Sequential ATPG is a research problem, not
   a six-week project. If the deliverable drifts from "combinational ATPG over a
   full-scan cut" to "test patterns for arbitrary sequential circuits," it does not
   finish. Draw that line in the design document and keep it.

*What would make it not worth doing:* if the scope were **ATPG-first**. Sequential
ATPG is a trap, and even combinational ATPG is the expensive half. But the
**fault-simulation** half stands on its own without any DFT course asking, because
"your test set detects 62% of the faults in your adder" is a strictly better
grading signal than "your test set passes" and is available to every instructor
using the batch surface on day one. Scoped that way, there is no honest argument
against it.

---

### Sources

**Repository (all at HEAD).**

- `src/jls/elem/Put.java:385` — `Put.currentValue`, the single value cell; the
  fault-clamp site.
- `src/jls/elem/Input.java:59,72` — `Input.setValue` / `getValue`.
- `src/jls/elem/Output.java:136`, `:139-145` — `Output.propagate` and its
  source-side change detection; every reaching line is a transition.
- `src/jls/elem/WireNet.java:443` — `WireNet.propagate`; `:454-479` tri-state
  resolution and the one-shot bus-conflict warning; `:488-513` the fanout loop
  (stem vs. branch); `:518-522` value store; `:525` `sim.probeSample`.
- `src/jls/elem/Gate.java:700-710` — the `PinChanged` arm; `if
  (!value.equals(toBeValue))` at `:706` is the in-flight comparison a glitch
  detector and a transition counter both key on.
- `src/jls/elem/LogicElement.java:476-481` — `initInputs` zeroes every input.
- `src/jls/elem/Memory.java:981-987` (`initMem`/`mem`), `:1000-1001`, `:1017-1024`
  (`ACTIVITY_LIMIT = 10_000`, the `activity` list), `:1320-1321` (cleared at
  initSim), `:1423-1430` (appended per write), `:1482-1489` (rendered as a string).
- `src/jls/elem/ElementId.java:36`; `src/jls/Circuit.java:479-485`
  (`getElementsInStableOrder`).
- `src/jls/elem/ElementRegistry.java:38-73` — the 33 registered types; the gate
  classes the collapsing table covers.
- `src/jls/elem/SigSim.java:64,67,71,74` — the quadratic `String +=` that will
  swamp any end-to-end benchmark.
- `src/jls/sim/Simulator.java:177-201` (`initSimulation`), `:215-243`
  (`runEventLoop`), `:269` (`afterEvent`), `:285` (`probeSample`).
- `src/jls/sim/BatchSimulator.java:144` (the trace gate), `:174` (recording-time
  dedup), `:550-554` (`vcdValue`'s whole-signal `bz`).
- `src/jls/JLSStart.java:672` (`displayResults`), `:759-788` (the `FLAGS` table).
- `examples/autograde/autograde.py:11-13,53-57` — the shipped VCD-parsing grader
  and its hard-coded expected stdout lines.
- `docs/batch-interface.md` §3.2 (the three-element stdout whitelist), §4.1 (the
  VCD signal set), §4.2 (`$timescale 1 ns` while "JLS time units are abstract"),
  §4.3 (no per-bit HiZ), §6 (the frozen-format promise and the new-flag exemption).
- `docs/simulation-semantics.md` §6.1 (ideal wires), §6.2 (the zero-delay wiring
  elements the collapsing rules use; transport-delay discipline), §7 (the delay
  table with no energy column), §9 (first-driver-wins, which makes net order
  load-bearing).
- `docs/grand-architecture.md` §3 (`JLSInfo` as a ~640-reference static hub), §6
  (the hot-plane rule), §9 (delegate to external tools).
- `docs/extension-points.md:28-36` — the seam table; `app.report-writer`,
  `sim.checker` and `sim.coverage-collector` are P5's and do not yet exist.
- `docs/capability-roadmap/README.md:45` (#129's deferral overturned, honesty rule
  kept), `:47` (#72 SAIF as the cheapest real item), `:414-416` (P4's timing DAG),
  `:421`,`:477` (activity assigned to P4), `:518-522` (toggle coverage assigned to
  P5), `:537-541` (P5's report channel must be designed first), `:696` (STIL/WGL
  printers), `:966-970` (P5's three uncatalogued seams), `:1047` (#96 stays out).
- `docs/capability-roadmap/keystone-c-performance.md` §2 (2,331,793 events;
  378,129 `NewValue`; 0.74 s at 6004 cycles; the RV32I census with 34 `Splitter`,
  9 `Binder`, 5 `Extend` of 225 logic elements), §3 (37.6% value / 47.7% queue /
  4.9% logic), and its representation measurements (10.85 vs 21.11 ns/op; the
  two-`BitSet` option at ~81 ns/op; 2.26 µs plane arrays vs 11.49 µs `BitSet[]`).
- `docs/capability-roadmap/sweep-02-timing.md:123`, `:588-615` — Change I, the
  prior 2–3 week activity+SAIF estimate.
- `docs/capability-roadmap/sweep-05-system-and-interfaces.md:420-470` — item G,
  boundary scan, and the honesty rule kept verbatim.
- `docs/standards-adoption/11-costed-rejections.md:813` — #129 costed at 15–25 md
  and deferred "until a DFT or test-engineering course asks."
- `docs/standards-landscape.md:255` (#72 SAIF, OTHER), `:389-399` (#129, #134,
  #135, #137, #138).

**External.**

- **Verified** — hneemann's Digital publishes no fault-simulation, stuck-at, ATPG,
  test-pattern, power, toggle or switching-activity feature; checked against the
  feature list at `https://github.com/hneemann/Digital`.
- **Verified** — Logisim-evolution publishes no such feature; checked against
  `https://github.com/logisim-evolution/logisim-evolution`.
- **Verified** — AUCOHL/Fault is an open-source (Apache-2.0) DFT toolchain with
  ATPG, scan stitching, JTAG insertion and stuck-at fault simulation, targeting
  OpenLane: `https://github.com/AUCOHL/Fault`, `https://fault.readthedocs.io/`.
- **Verified** — Atalanta: FAN-based combinational ATPG plus parallel-pattern
  single-fault-propagation fault simulation (`https://github.com/hsluoyz/Atalanta`).
- **Verified** — HOPE: parallel fault simulation for synchronous sequential
  circuits using three logic values (0, 1, X), per its published description.
- **Verified** — OpenSTA reads VCD and SAIF switching activity with a Liberty
  library and produces a power report (`read_vcd`/`read_saif`, `report_power`):
  `https://openroad.readthedocs.io/en/latest/main/src/sta/README.html`.
- **Verified** — Verilator's `--coverage-toggle` inserts a per-bit toggle counter
  on every signal bit (`https://verilator.org/guide/latest/simulating.html`);
  Antmicro published a Verilator→SAIF→OpenROAD power-estimation flow (July 2025,
  `https://antmicro.com/blog/2025/07/power-estimation-in-openroad-using-the-saif-wave-format-in-verilator/`).
- **Unverified and flagged as probably mistaken** — a 2021 Hackaday
  survey-of-logic-simulators page carries an assertion that Digital "is able to
  simulate faults (open to Z, stuck at 0, etc.) and generate a fault dictionary."
  Contradicted by Digital's own feature list; most likely describes a different
  tool. `https://hackaday.com/2021/06/10/survey-of-simple-logic-simulators/`
- **Unverified** — DigitalJS fault or power capability (not checked; no reason to
  expect either from a Yosys-netlist viewer/simulator).
- **Unverified** — specific current capabilities, licensing terms and pricing of
  Synopsys TestMAX ATPG / PrimePower, Siemens Tessent / PowerPro, and Cadence
  Modus / Joules. Their *existence* as the commercial tool class is common
  knowledge; nothing beyond that is asserted here.
- **Unverified** — the commonly quoted "equivalence collapsing removes ~50–60% of
  the stuck-at fault list on ISCAS benchmarks." Directionally standard in the
  literature; not measured for this document and not measured on JLS circuits.
