## Multi-clock domains, clock-domain crossing, and reset architecture

*Area 08 of the leapfrog sweep. Extends `docs/capability-roadmap/README.md`.
Every JLS claim is anchored to a path at HEAD; every claim about another tool is
marked verified or unverified.*

---

### What is missing today

**JLS has a clock element. It does not have a clock.**

`src/jls/elem/Clock.java` is 432 lines that produce a square wave and nothing
else. Its entire saved state is three attributes — `cycle`, `one`, `orient`
(`Clock.java:203-281`). It has no inputs (`Clock.java:161` adds one `Output` and
that is all), does not implement `Timed`, and its `react` (`:404-430`) is a
self-perpetuating alternation: propagate, flip, repost at `now + one` or
`now + (cycle − one)`. `initSim` (`:384-394`) drives the output to 0 and posts
the first rising transition at `cycle − one`.

Downstream, **a clock is not distinguishable from data anywhere in the tree.**

- **The exporter cannot say a wire is a clock.** `HdlExporter.java:485` —
  `if (el instanceof InputPin || el instanceof Clock) { return; }` — treats the
  two identically: both are "ports that drive their nets directly." The register
  path takes `operand(ins.get(1), …)` (`:578`) and checks only that the operand
  is a net (`:579-582`). `HdlModel.RegisterStatement`'s field doc
  (`HdlModel.java:414`) states the situation in six words: *"1-bit clock; a
  literal clock never ticks."* The IR knows a register can be clocked by a
  constant, and files that under commentary.
- **There is no clock-sink concept, only three private copies of an edge
  detector.** `Register.currentC` (`Register.java:698`, compared at `:772`
  and `:783`, updated at `:794`), `StateMachine.oldClock`
  (`StateMachine.java:730-752`), and `Memory.lastClock` (`Memory.java:996`,
  `:1374`, `:1390`). Three fields, three hand-written rising/falling tests,
  three initialisations — in a codebase that introduced the `Timed` capability
  interface (`src/jls/elem/Timed.java`, issue #78) *specifically* to stop this
  pattern for propagation delay. The clock pin index is a magic number:
  `Register`'s clock is `inputs.get(1)` because `init` happens to add `D` then
  `C` (`Register.java:230-231`, repeated at `:240-241`, `:250-251`, `:260-261`).
- **The clock lives in the combinational drawer.** `src/jls/edit/Palette.java:169`
  registers `Clock` under `Group.COMBINATIONAL`, whose own javadoc
  (`Palette.java:47-48`) reads *"Combinational building blocks and the clock."*

**Multiple clocks: supported by accident, unusable on purpose.** Nothing couples
`Clock` instances — each is an independent element with its own event chain — so
two `Clock`s with different periods do run independently and correctly. But:

- **Phase and duty cycle are the same knob.** The first rising edge is pinned at
  `cycle − one` (`Clock.java:392`; normative in `docs/simulation-semantics.md`
  §8.3), and there is no phase attribute. Two 50 %-duty clocks of equal period
  therefore *always* rise on the same timestamp; the only way to separate them is
  to change one's duty cycle, or to insert a `DelayGate` in a clock leg —
  an element with no declared meaning to any consumer. **The most common
  multi-clock teaching setup — two identical clocks 90° apart — is not
  expressible.**
- **Simultaneous edges from two domains are resolved by draw order.**
  `docs/simulation-semantics.md` §6.1 (lines 197-204) states it: inputs are
  overwritten eagerly inside the driving element's `react`
  (`WireNet.java:487-509`), an element's `react` therefore always reads the
  *latest* same-time values, and *"same-time races (e.g. a clock edge and a data
  change at the identical timestamp) are resolved by this read-latest rule plus
  FIFO event order — deterministically, but with no setup/hold modeling."* FIFO
  order is `SimEvent`'s global sequence counter (`SimEvent.java:87`), assigned at
  construction. So which domain wins a race is a function of element seeding
  order — deterministic, reproducible, and *arbitrary with respect to the
  design*. Add an unrelated element to the drawing and it can flip.
- **Consequence: setup time is zero and hold time is zero.** A data change
  landing on the exact timestamp of a capture edge is captured. There is no
  window, no violation, and no report. **A two-flop synchronizer in JLS is two
  flip-flops that cost 100 time units of latency and change nothing else.** JLS
  does not merely fail to teach why the second flop exists; it demonstrates that
  it does not.
- **A fast clock silently drops edges.** `StateMachine.react` refuses a new
  transition while `busy` (`StateMachine.java:736-739`; normative in
  §8.2) — *"a clock faster than the propagation delay drops edges rather than
  queueing them."* In a two-clock design that is an unreported functional
  failure whose only symptom is a wrong answer.

**Clock gating, skew, and derived clocks.** All three are drawable and none is
modelled. A divide-by-two is `notQ → D` with the source clock on `C`; it works,
and it lands 50 time units (`Register`'s default delay,
`docs/simulation-semantics.md` §7) behind its source, which nothing records or
reports. An AND gate in a clock path gates the clock and adds 10 units of skew.
Because element delay is **transport, not inertial** (§6.2), a runt pulse on a
gated clock is *delivered*, and `Register.react`'s edge test sees it as an
ordinary rising edge — so **JLS already simulates the classic gated-clock
glitch-capture bug faithfully, and then renders the evidence away**:
`Trace.java:325` computes `int rlen = (int)Math.round(len)` and a pulse narrower
than a pixel disappears from the waveform. The bug happens; the proof does not.

**Reset does not exist as a signal.** `Register`'s port list is exactly
`D, C, Q, notQ` (`Register.java:230-235`). Reset is the `init` attribute
(`Register.java:311-330`), driven onto Q by a time-0 event (`initSim:734-735`)
and *pinned as intended behaviour* by
`SequentialGoldenTest.registerInitialValueAppearsBeforeAnyClockEdge`. Combined
with `LogicElement.initInputs` (`:476-481`) zeroing every input at every depth,
JLS supplies every design a reset it did not draw.

#### The workarounds — the strongest evidence the gap is real

1. **The flagship CPU does not use the clock element.** `riscv/build_cpu.py:125-126`:
   `# clock is an input pin so batch -t vectors can step it deterministically` /
   `self.clk = c.input_pin("clk", 1).out`. The module docstring at `:10` still
   says *"a single Clock element drives every flip-flop's rising edge"* — the
   documentation and the code disagree and the code won. The waveform is
   generated in Python (`riscv/verify.py:18-25`, `gen_clock`) as a `-t` text file
   with exactly `steps` rising edges, because JLS has no way to express "run for
   N clock cycles." `docs/capability-roadmap/README.md:583` already prices that
   scaffolding at 38.2 s against 4.1 s free-running.
2. **The CPU hand-builds a gated clock and calls it a write enable.**
   `riscv/build_cpu.py:404-406`: `# WE (active low) = NOT(MemWrite AND NOT clk)`
   — an inverter and an AND gate qualifying a memory write by the clock *level*.
   `riscv/README.md:125` and `docs/simulation-semantics.md` §8.4 both bless it as
   *"the workaround in the default mode."* A clock net is being consumed as a
   data qualifier, in the tree's most important design, and nothing in JLS knows.
3. **The importer tells students to change their reset architecture.**
   `src/jls/hdl/yosys/CellValidator.java:75-81`, `ASYNC_RESET_MESSAGE`: *"JLS
   registers reset synchronously. Move the reset into the clocked block … and the
   design will import exactly."* JLS asks a student to alter the design's reset
   discipline to fit a missing pin.
4. **P5's own ERC list contains a rule it cannot implement.**
   `docs/capability-roadmap/README.md:545` lists *"clock nets on data pins"*
   among the electrical rule checks. There is no clock-net concept to check
   against. The roadmap has already noticed the hole and routed around it.

---

### The capability

**A clock is a first-class object with an identity, a period, a phase and a
derivation; every sequential element belongs to exactly one clock domain;
crossings between domains are a structural property the tool computes, reports on
the drawing, and can inject the physical failure of.**

Six pieces, with recommendations rather than options.

**C1 — Consolidate the clock sink, and give `Clock` a phase.**
One capability interface in the `Timed` style — `interface Clocked { Input
clockPin(); Edge activeEdge(); }` — implemented by `Register`, `StateMachine`
and sync-mode `Memory`, replacing `currentC` / `oldClock` / `lastClock` with one
shared edge detector. This is the enabling refactor: *everything else needs to
ask "which pin is the clock" and today has to know each element's port order.*
Add `int phase` to `Clock` (0 ≤ phase < cycle, absent ⇒ 0, one FORMAT bump,
byte-identical for every existing file). **Recommendation: `phase`, not a
general waveform spec.** The `(cycle, one, phase)` triple covers every teaching
waveform and maps exactly onto SDC's `create_clock -period cycle -waveform
{phase, phase+one}`, so it costs nothing later.

**C2 — Clock domains, inferred, not authored.**
A new `jls.timing.ClockDomains` producing `Map<LogicElement, ClockId>` plus a
derivation record per clock. **Recommendation: infer, then allow override.** The
inference:

- *Roots* = every `Clock` element output; every top-level `InputPin` carrying
  P3's clock port role; anything a constraint file names with `create_clock`.
- *Backward slice* from every `Clocked.clockPin()` to a root through the clock
  cone. If the slice is pure combinational logic ⇒ **gated clock**, same domain,
  enable recorded. If it passes through a sequential element ⇒ **generated
  clock**, same domain, ratio and phase recorded (a divide-by-2 is
  period × 2, phase + propDelay). If it reaches two roots ⇒ **muxed clock**,
  flagged. If it reaches no root ⇒ **undriven clock**, flagged (today this is a
  register that silently holds its init value, warned about only in the HDL
  exporter at `HdlExporter.java:580-582`).
- *Domain identity is the root*, not the net. Two clocks derived from one root
  are synchronous; two roots are asynchronous unless a circuit-level group
  declaration says otherwise. This is exactly SDC's model, and adopting its
  vocabulary means the concept transfers to Vivado and PrimeTime unchanged.

**Recommendation: the inference reports itself before it reports any
violation.** A clock list — root, period, phase, derivation, sink count — is the
first artifact. A CDC tool whose domain model is wrong produces confidently
wrong violations, and this is the single most common way the capability fails.

**C3 — The crossing check.**
A crossing is: a `Clocked` sink in domain B whose data cone contains a `Clocked`
source in domain A ≠ B (or an undomained top-level input), with no recognised
synchronizer between them. Pure structural walk over
`Circuit.getElementsInStableOrder()` and the `WireNet` graph — the same graph
P4's STA builds, and it must be *the* graph, not a second one. Zero simulation
cost.

Recognition of a synchronizer, and this is where JLS's form gives it an
advantage:

- A `Synchronizer` **element** (C4) declares the boundary exactly.
- A hand-drawn chain of ≥ 2 `Register`s in the destination domain is recognised
  by pattern — **including the fanout condition**, that the intermediate node
  drives nothing but the second flop. That condition is the thing students get
  wrong, and it is checkable.
- A multi-bit crossing through per-bit synchronizers is flagged as
  **unsafe-by-construction** unless the source is gray-coded or the crossing
  carries a handshake — because independent flops resolve independently and the
  destination can observe a value the source never held. This is the single best
  demonstration in the whole area (see below).
- Everything else is flagged, with a one-click waiver stored in the `.jls`.

**Recommendation: silent by default on single-clock designs.** A first-year
circuit with one `Clock` must produce zero output. A checker that talks in the
common case is a checker students learn to ignore — which is the industrial
failure mode, imported.

**C4 — Two elements, and one deliberate non-element.**
`Synchronizer` (N ≥ 2 flops, N configurable, one box) and `ResetSynchronizer`
(async assert, synchronous release). Both are `elem.element-provider` +
`gui.palette-contributor` rows under the shipped mechanism
(`docs/extension-points.md:30-31`).
**Recommendation: do NOT ship an `AsyncFifo` element.** Ship it as an example
circuit built from `Synchronizer`, a P2 dual-port `Memory`, and drawn gray-code
pointer logic. The entire teaching value of an async FIFO is that you build it;
a black box teaches nothing, and the roadmap already makes exactly this argument
about the array multiplier (`README.md:267-270`).

**C5 — Reset architecture.**
Given P2's `CLR`/`PRE` pins, reset becomes a wire and therefore becomes
checkable: every sequential element reachable from a reset root; reset released
synchronously with respect to its own domain's clock; **a reset crossing domains
is itself a CDC crossing** (the case textbooks and students both get wrong);
reset not used as a clock and clock not used as a reset. The teaching value is
gated on P1-S4's `U`: without an uninitialized start-up state, a reset tree is
decoration, because the design already works without it.

**C6 — Metastability, and whether it is honest.**
The mechanism: P4's setup/hold check fires at the `Clocked` edge site; on
violation the capture drives `X` (P1) for one clock period; the `X` then
resolves. The question the brief asks is whether that is modelling or theatre.
The answer has four parts and they differ.

- **`X` on a setup/hold violation is honest.** It asserts exactly the true
  statement — *the captured value is not a function of the design* — and it is
  the same abstraction every gate-level sign-off simulator uses for a
  `$setuphold` violation (*unverified against the paywalled IEEE 1364 text; this
  is standard practice as described in vendor documentation*).
- **`X` resolving after one clock period is honest as a bounded abstraction and
  dishonest as physics.** The model asserts settling within one period; a real
  flop settles within a period with probability 1 − ε, and ε is the entire
  subject. Ship that sentence in `docs/simulation-semantics.md` and the model is
  honest.
- **Seeded random resolution is the honest part and the valuable part.**
  Recommendation: on violation, the flop resolves to 0 or 1 chosen from a
  per-run seed, **independently per bit**. This is not a probability model — it
  is the only correct representation, in a deterministic simulator, of "not a
  function of the design." It makes the bug non-reproducible *and* makes the
  non-reproducibility reproducible, which is what a teaching tool needs.
- **Two things are theatre and must be refused in writing.** (i) **Any MTBF
  number.** MTBF needs τ and T₀ — characterisation data JLS does not have and
  cannot have. A made-up τ would be `docs/simulation-semantics.md` §7's delay
  table all over again, and worse, because the number looks authoritative.
  Offer instead "run 100 seeds, count failures" — a number the student measured.
  (ii) **Animating a decaying oscillation on the waveform.** Draw `X`. Do not
  draw a picture of an analog phenomenon the simulator did not compute. Record
  both as normative non-claims, in the same register as the #129
  conformance-honesty rule (`README.md:45`).

**C7 — Skew and insertion delay.** Per-sink clock-net delay is P4's interconnect
item; the clock-specific half is a declared insertion delay per clock sink plus a
skew report ("this domain's worst skew is 12 units, between these two flops").

---

### What it unlocks

**Standards.** No survey entry is *blocked* solely on this — which is worth
saying plainly, because it means the case is pedagogical and engineering, not
conformance. It materially strengthens four: **#89 SDF** (`TIMINGCHECK` needs a
place for a violation to *go*; `README.md:40` already routes SDF's C4 objection
through P1, and the clock model is what makes the check meaningful rather than
decorative), **#31 Verilog** (`$setup`/`$hold`/`$setuphold`, and `always_ff @(posedge
clk or negedge rst_n)` becoming importable is P2's, but *checkable* is this),
**#82/#93/#94** (SDC/XDC constraint ingestion — `create_clock`,
`create_generated_clock`, `set_clock_groups -asynchronous`, `set_false_path`
are exactly the C2 object model; P4 already scopes the reader and this supplies
the thing it reads into), and **#65/#259 RISCOF** (a compliance claim against a
CPU whose reset is a save-file attribute is not a compliance claim —
`sweep-01:62` makes this argument for V5; the same argument applies to a CPU
whose clock is a text file).

**Engineering capabilities.**

- A clock list — every clock, its root, period, phase, derivation and sink count
  — before any analysis. JLS cannot produce one line of this today.
- Domain-correct STA. P4's static timing analyser must know which clock launches
  and which captures to compute a path's requirement; without C2 an STA over a
  two-clock design computes a number that means nothing. **This is an ordering
  constraint on P4, not a nice-to-have.**
- The exporter can finally emit port roles and a testbench that knows which port
  is a clock (P3's port metadata gets its first real consumer).
- Combinational-loop detection through clock paths, and the muxed-clock and
  undriven-clock cases, which today surface only as a wrong answer.
- Reset-tree completeness as a check rather than an assumption.

**Teaching capabilities — what a student can do afterwards that they cannot
today.**

- **Cross a button into a clocked design.** This is the first-year hook and it
  matters: every first-year course has a button, a button is asynchronous to
  everything, and today JLS says the naive design is correct. Afterwards: draw
  it, see the crossing flagged red on the wire, insert a synchronizer, see the
  flag clear.
- **Measure that testing does not find CDC bugs.** Run the unsynchronized design
  100 times with 100 seeds; 87 pass. Add the synchronizer; 100 pass. The student
  has just measured the defining property of the bug class — that it is not
  reproducible and that a passing test proves nothing. **No educational tool
  offers this, and it is the single most transferable lesson in the area.**
- **See why FIFO pointers are gray-coded.** Cross a 4-bit binary counter into
  another domain through four independent synchronizers. On the 0111→1000
  transition the destination can latch *any* of the intermediate combinations —
  values the counter never held. Change to gray code; only one bit ever moves;
  the destination is always either the old value or the new one. This is drawable
  in JLS today, and today JLS gives the right answer and destroys the lesson.
- **Watch a gated clock's glitch clock a flop** — the bug JLS already simulates
  correctly (transport delay + edge detection on any react) and currently renders
  invisible at `Trace.java:325`. With P4's sub-pixel transient rendering plus the
  clock model naming the net, the glitch becomes visible *and* attributable.
- **Hold a machine in reset and release it two ways.** Release asynchronously
  across a skewed reset tree and watch flops leave reset on different edges;
  release synchronously and watch them leave together. Requires P2's `CLR` pin
  and P1-S4's `U`.
- **Clock-enable versus gated clock as the design choice it is.**
  `sweep-03:176-181` already frames this; the clock model is what lets the tool
  show that the gated version has skew and a glitch hazard and the enabled
  version does not.

---

### Competitive position

**Commercial.** Structural CDC is a mature, expensive product category: Siemens
**Questa CDC**, Synopsys **VC SpyGlass CDC**, Cadence **Conformal/Jasper CDC**,
Real Intent **Meridian CDC**. Reset-domain-crossing (RDC) analysis is a newer
adjacent product in the same suites. Metastability injection into simulation
exists in at least the Siemens flow. *(All specific product capabilities here are
from general industry knowledge and are **unverified** — I have not fetched
vendor documentation.)* Multi-clock STA with `create_clock` /
`create_generated_clock` / `set_clock_groups` is table stakes in every FPGA and
ASIC flow (Vivado, Quartus, PrimeTime); Vivado additionally ships a `report_cdc`
command *(unverified)*.

**Where the incumbents are genuinely weak — stated honestly, and one of these is
not a real advantage for JLS.**

1. **Report volume and triage.** Structural CDC on a real SoC produces tens of
   thousands of violations, the great majority waived, and waiver-file management
   is where CDC sign-off actually lives. This is a real weakness — but it is a
   weakness *of scale*, and JLS never has that scale. **JLS's advantage here is
   "small designs," not "better tool," and it should not be claimed as the
   latter.**
2. **Setup cost, and silent failure of the constraint file.** A commercial CDC
   run says nothing useful until the clock/reset constraints are right, and a
   *wrong* constraint file produces a clean report. This one **is** an
   architectural advantage for JLS: in a schematic, the clock roots are *in the
   drawing* — a `Clock` element is unambiguously a clock — so inference is exact
   and the constraint file is optional. No RTL tool can do that, because RTL has
   no `Clock` element.
3. **Every simulator says the bug works.** That is why the static tool exists.
   Metastability injection is a licensed, separate, rarely-used flow. JLS can
   make injection the default in a lab mode at zero licence cost. Not a
   sign-off leapfrog; a decisive teaching one.
4. **Nobody shows it on a drawing the user made.** RTL CDC tools render a
   schematic *derived* from RTL, which the user did not draw and does not
   recognise; the crossing is a row in a report. **JLS's user drew the picture,
   and there is exactly one picture.** This is the same argument
   `README.md:654-659` makes for P6's cross-probing, and it is the strongest one
   in this area too.

**Open source.** Yosys has no CDC analysis. OpenSTA/OpenROAD handle SDC clocks
and multi-clock STA but do not do CDC. GTKWave and Surfer are viewers with no
clock concept. *I am not aware of a maintained open-source structural CDC
checker — that is an **unverified negative** and should be re-checked before it
is asserted in public.* If it holds, this is a genuine hole in open EDA.

**Peer educational.** hneemann's **Digital** has a clock component with a
frequency and supports multiple clocks; **Logisim-evolution** is fundamentally
tick-based, which makes clocks of unrelated periods awkward; **DigitalJS** is
synchronous with no timing model; **CircuitVerse** likewise. *(All four claims
unverified.)* **None of them has setup/hold, metastability, clock-domain
identity, or CDC analysis** — I am confident of this as a category statement and
it should still be checked before publication. Educational tools uniformly model
the flip-flop as magic, which is precisely the failure
`sweep-02:598-603` names for JLS.

**What JLS's version would be.** A schematic tool in which the clocks are
objects you can list, the domains are computed from the drawing without a
constraint file, an unprotected crossing is red on the wire you drew, the fix is
a palette element, and the failure is a seeded coin-flip you can run a hundred
times and count. **Parity** on domain modelling and structural crossing detection
(and deliberately *not* on functional/protocol CDC, on which see below).
**Leapfrog** on three things: inference-without-constraints (architectural, not
scale), the crossing marked on a user-authored drawing, and seeded
non-reproducibility as a measurable classroom experiment.

**Where JLS cannot plausibly lead, said plainly.** Sign-off-quality multi-clock
timing — clock uncertainty, jitter, on-chip variation, CPPR — belongs to
PrimeTime and requires characterised libraries JLS will not have. Functional CDC
(protocol checks on handshakes, FIFO depth analysis) needs P5's temporal layer
and, even then, real protocol coverage is out of reach. JLS must never claim CDC
*sign-off*; the honesty rule from the #129 costing applies verbatim.

---

### Relationship to the existing programs

**This is a new program — call it P7, the clock and reset architecture
program — and it is the second-most-gated program in the roadmap after P6.** It
is small because it consumes rather than builds: P1 supplies the value, P4
supplies the trigger, P2 supplies the pins, P3 supplies the port roles, P5
supplies the report channel. What it contributes that **no existing program
owns** is the clock-domain model itself, and the roadmap has already tripped over
that absence twice — P5's ERC lists "clock nets on data pins"
(`README.md:545`) with nothing to check against, and P3's port metadata
(`README.md:326-327`) observes that "JLS cannot say a wire *is* a clock" without
assigning anyone the job of fixing it.

**It splits cleanly in two, and the split is the important scheduling finding.**

- **Structural half (C1 + C2 + C3, plus C5's checks).** Static analysis. Touches
  no `react` body except C1's edge-detector consolidation, does not enter the hot
  plane, needs nothing from P1. **Parallel-safe, gated on nothing.** It can ship
  beside P5's ERC and P4's STA.
- **Dynamic half (C4's elements' teaching value, C6, C7).** Gated on **P1-S2**
  (X producible) and **P4's timing checks** (the trigger), and on **P1-S4** (`U`)
  for the reset half to mean anything.

**Ordering constraints, specifically.**

- **C2 must precede or accompany P4's STA.** An STA over a two-clock design that
  does not know which clock launches and which captures computes a meaningless
  number. `README.md:474` prices STA at 5–8 weeks assuming a single implicit
  clock; that assumption should be made explicit or the estimate revised.
- **C2 should precede P4's constraint reader.** `create_clock` /
  `create_generated_clock` / `set_clock_groups` need somewhere to land.
- **C1 should precede P2's register control pins.** P2 adds `CLR`/`PRE`/`EN`/`LD`
  to `Register` (`README.md:221-225`); doing that *after* the `Clocked` interface
  exists is one edit, doing it before is two.
- **C6 is P4's V8 plus a seed.** `sweep-01:440-473` already specifies V8
  (setup/hold → X, optional settling) at 3–4 weeks; the marginal work here is
  seeded independent-per-bit resolution and the multi-run seed sweep, which rides
  on P5's already-scoped multi-run batch mode (`README.md:527`).
- **C4's `AsyncFifo` example needs P2's memory port model** (dual-port RAM). The
  `Synchronizer` and `ResetSynchronizer` elements do not.

---

### Size and risk

| Piece | Weeks | Reasoning |
|---|---:|---|
| **C1** `Clocked` interface + `Clock.phase` + FORMAT bump | 2–3 | Three `react` bodies whose behaviour is pinned by `SequentialGoldenTest`, `MemoryModelTest` and `SimulationSemanticsRegressionTest`; a pure refactor that must be byte-identical. Comparable: P2's register pins at 3–5 (`README.md:282`) includes new behaviour; this does not. |
| **C2** domain inference + clock report | 3–4 | The graph walk is small; the derivation rules (gated / generated / muxed / undriven) and the *self-reporting* requirement are most of it. |
| **C3** crossing check + waivers + schematic marking | 3–4 | Walk 1 wk; synchronizer pattern recognition incl. the fanout condition 1 wk; GUI marking + waiver persistence 1–2 wk. Comparable: P5's ERC at 3–5 (`README.md:592`). |
| **C4** `Synchronizer` + `ResetSynchronizer` | 2–3 | P2's own figure: ~1.5 wk for the first element including plumbing, ~0.75 after (`README.md:285`), plus exporter arms. |
| **C5** reset-tree and reset-crossing checks | 2 | Reuses C2's slice machinery. |
| **C6** seeded metastability + seed sweep (marginal over P4's V8 and P5's multi-run) | 2–3 | The X-injection half is priced in P4. |
| **C7** clock skew / insertion delay report (marginal over P4's interconnect) | 1–2 | The per-sink delay is priced in P4. |
| **Total** | **15–21** | **Marginal over P4/P5: 13–18.** |

**Structural-only floor: 8–11 weeks (C1 + C2 + C3), parallel-safe, gated on
nothing.** That floor alone gives a clock list, domain-correct STA, and an
unsynchronized crossing marked red on the drawing.

**The top three ways it goes wrong.**

1. **False positives, and the checker becomes noise.** A CDC tool that flags
   correct designs teaches students to dismiss tool output — the industrial
   failure mode imported into a classroom, where it is worse because there is no
   senior engineer to triage. *Mitigations, all mandatory:* silent by default on
   single-clock designs; one-click waivers persisted in the `.jls`; never flag a
   crossing the tool cannot name a fix for; the clock-list report ships before
   the violation report.
2. **The inference is wrong on derived and gated clocks, silently.** A divider
   whose Q feeds both a clock pin and a data pin is genuinely ambiguous; a gated
   clock whose enable is itself a crossing is a hard case commercial tools get
   wrong too. Wrong domains make every downstream report confidently wrong.
   *Mitigation:* the inference reports its derivation for every clock, and
   ambiguous cases ask rather than guess — and the honest fallback is "I cannot
   determine this clock's relationship; declare it," not a default.
3. **Metastability shipped as physics.** If a JLS release prints a number
   labelled MTBF, the project permanently loses the standing to make the honesty
   arguments P1 and P6 depend on — the `VhdlEmitter` nine-value disclaimer, the
   §7 delay table, the #129 conformance rule. *Mitigation:* the refusal is
   written into `docs/simulation-semantics.md` as a normative non-claim before
   any code.

**What would make it not worth doing.**

- **If the metastability half is refused or indefinitely deferred**, the teaching
  claim collapses from "students meet a class of non-reproducible bug" to
  "students get a lint message." At that point this is not a program — it is two
  ERC rules and a clock-domain map inside P5, worth about 3–4 weeks, and it
  should be filed there instead of standing alone.
- **If P4's timing checks never land**, C6 has no trigger and the whole thing is
  static-only.
- **The genuine audience question the maintainer should answer first:** most
  first-year digital-logic courses are single-clock, and 15–21 weeks aimed at a
  second-year computer-organisation audience is a real allocation decision. The
  counter-argument, and I think it holds: **a button is an asynchronous input,
  and every first-year course has a button.** Debouncing and synchronizing a
  button is the first-year-accessible form of the entire lesson, and it makes the
  program pay in year one rather than year two. If that lab is not one the
  maintainer wants, the program should be declined honestly on those grounds and
  not on size.

---

### Sources

**Repository (all verified at HEAD).**

- `src/jls/elem/Clock.java` — class decl `:26` (no `Timed`); attributes
  `:203-281` (`cycle`, `one`, `orient` only); `init`/single output `:139-163`;
  `initSim` first edge at `cycle − one` `:384-394`; `react` `:404-430`.
- `src/jls/elem/Register.java` — ports `:230-235` (and `:240-245`, `:250-255`,
  `:260-265`); `init` attribute `:311-330`; `currentC` `:698`; `initSim`
  time-0 drive `:719-737`; edge arms `:771-794`.
- `src/jls/elem/StateMachine.java` — clock input `:198`; `react` edge detection
  and `busy` rule `:722-760`.
- `src/jls/elem/Memory.java` — sync-mode clock input `:196`; `lastClock` `:996`,
  `:1318`, `:1374`, `:1390`.
- `src/jls/elem/WireNet.java` — eager input overwrite + same-time `PinChanged`
  `:487-509`.
- `src/jls/elem/Timed.java` — the capability-interface pattern C1 copies.
- `src/jls/elem/LogicElement.java:476-481` — `initInputs` zeroing.
- `src/jls/sim/SimEvent.java:87` — the global sequence counter that decides
  same-time race order.
- `src/jls/sim/Simulator.java:165` (`post`), `:215` (`runEventLoop`).
- `src/jls/hdl/HdlExporter.java:271-284` (clock as a port), `:485`
  (`InputPin || Clock`), `:578-582` (register clock operand + warning).
- `src/jls/hdl/HdlModel.java:414` — *"1-bit clock; a literal clock never ticks."*
- `src/jls/hdl/yosys/CellValidator.java:75-81` — `ASYNC_RESET_MESSAGE`.
- `src/jls/edit/Palette.java:47-48`, `:169` — `Clock` in `Group.COMBINATIONAL`.
- `src/jls/edit/Trace.java:325` — `int rlen = (int)Math.round(len)`.
- `riscv/build_cpu.py:10` (docstring), `:125-126` (clock is an input pin),
  `:251`, `:404-406` (hand-built clock gating), `:436`.
- `riscv/verify.py:15-25` — `HALF`, `gen_clock`.
- `riscv/README.md:113-116`, `:125`, `:147`.
- `docs/simulation-semantics.md` §5 (`:135-170`), §6.1 (`:185-204`), §6.2
  (`:206-232`), §7 (`:264-289`), §8.1 (`:291-331`), §8.2 (`:333-360`), §8.3
  (`:362-370`), §8.4 (`:372-407`).
- `docs/batch-interface.md` §2 (`:57-130`) — the `-t` grammar the CPU's clock
  is written in.
- `docs/capability-roadmap/README.md` — P2 register pins `:221-225`, sizes
  `:282-289`; P3 port metadata `:326-327`; P4 `:405-411`, `:444-448`, sizes
  `:471-477`; P5 ERC `:545`, multi-run `:527`, sizes `:587-596`; P6 gating
  `:682-687`; honesty rule `:45`; cross-probing argument `:654-659`.
- `docs/capability-roadmap/sweep-01-values-and-logic.md` — V5 `:302-345`, V8
  `:440-473`.
- `docs/capability-roadmap/sweep-02-timing.md:598-613`.
- `docs/capability-roadmap/sweep-03-elements-and-hdl.md:168-185`.
- `docs/capability-roadmap/keystone-b-migration.md:98`, `:569`.
- `docs/extension-points.md:30-36` — the seams C4 and C3 deliver through.
- `docs/grand-architecture.md` §6 — the hot-plane rule; C2/C3/C5 are static
  analyses and never enter the loop, so §6 is satisfied by construction.

**External claims — all UNVERIFIED.** I did not fetch vendor or project
documentation for any of the following, and each should be checked before being
asserted in public: the existence and feature set of Questa CDC, VC SpyGlass CDC,
Conformal/Jasper CDC and Meridian CDC; Vivado's `report_cdc`; metastability
injection in any commercial simulator; the absence of CDC analysis in Yosys and
OpenSTA; the clock models of hneemann's Digital, Logisim-evolution, DigitalJS and
CircuitVerse; the negative claim that **no** maintained open-source structural
CDC checker exists; and the claim that `$setuphold` violations conventionally
drive X (IEEE 1364 is paywalled and I have not read it).
