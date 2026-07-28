## Timing, delay, power, and library standards

*Sweep 02. Survey entries #87–#99 (Tier 6), plus #72 (SAIF), #82 (XDC/QSF/LPF —
the timing half), #66/#67 (VCD/EVCD), #76/#75 (Verilog netlist export/Yosys
import), and #65 (RISCOF), all of which turn out to be gated on the same three
model changes.*

---

### 0. What JLS's timing model actually is, at HEAD

Established by reading `docs/simulation-semantics.md` §1, §3, §4, §6, §7,
`src/jls/sim/Simulator.java`, `src/jls/sim/SimEvent.java`,
`src/jls/elem/WireNet.java`, `src/jls/elem/Gate.java`,
`src/jls/elem/DelayGate.java`, `src/jls/elem/Register.java`,
`src/jls/elem/Input.java`, `src/jls/elem/Output.java`.

**The model in six sentences.**

1. Time is a dimensionless non-negative `long now`
   (`src/jls/sim/Simulator.java:36`); nothing binds a tick to a physical unit
   (`docs/simulation-semantics.md` §1).
2. Every delayed element carries **exactly one scalar `int propDelay`** — 16
   classes implement `jls.elem.Timed` (`src/jls/elem/Timed.java:25`): the seven
   `Gate` subclasses (`AndGate`, `OrGate`, `XorGate`, `NandGate`, `NorGate`,
   `NotGate`, `DelayGate`), plus `Adder`, `Decoder`, `Mux`, `Register`,
   `ShiftRegister`, `StateMachine`, `TriState`, `TruthTable`, `Memory` (which
   calls its scalar `accessTime`, `src/jls/elem/Memory.java:108`).
3. The scalar is **per-instance** and persists in the save file (`int delay`,
   `src/jls/elem/Gate.java:316`; `int time`, `src/jls/elem/Memory.java:444`) —
   so per-instance delay is the *one* timing dimension JLS already has.
4. The scalar is applied as **pure transport delay**, uniformly across every
   input→output arc, both edges, one value: `sim.post(new SimEvent(now+propDelay,
   this, new NewValue(value)))` (`src/jls/elem/Gate.java:708`, and the identical
   line at `Mux.java:547`, `Decoder.java:480`, `Adder.java:410`,
   `ShiftRegister.java:665`, `Register.java:768/779/790`, `TriState.java:491/505`,
   `StateMachine.java:789`, `TruthTable.java:1383/1456`, `Memory.java:1384/1396/1402`).
5. **Wires are ideal.** `WireNet.propagate` (`src/jls/elem/WireNet.java:443`)
   overwrites every sink `Input` synchronously and posts a same-time
   `PinChanged`; there is no per-net, per-sink, or fanout-dependent delay
   (`docs/simulation-semantics.md` §6.1).
6. **Nothing anywhere records when a signal last changed.** `src/jls/elem/Input.java`
   is a value cell (`setValue`/`getValue`, lines 59 and 72) with no timestamp;
   `SimEvent` carries a time but is discarded on `poll`
   (`Simulator.runEventLoop`, `src/jls/sim/Simulator.java:224-225`).

**What it therefore cannot express, itemised:**

| Timing concept | Expressible today? | Why not |
|---|---|---|
| per-instance delay | **yes** | `int delay` / `int time` save attributes |
| per-arc (pin→pin) delay | no | one scalar per element, applied to all outputs |
| rise vs fall asymmetry | no | `NewValue` carries no edge sense; one scalar |
| min : typ : max | no | `int propDelay`, one number |
| conditional delay (`COND`) | no | no arc structure to condition on |
| inertial delay / glitch filtering | no | `docs/simulation-semantics.md` §6.2 states transport explicitly |
| interconnect / fanout delay | no | `WireNet.propagate` is synchronous, `src/jls/elem/WireNet.java:443` |
| setup / hold constraint | no | no place to store it *and* no last-change timestamp to check against |
| timing-check violation | no | no X/violation value; `Register.react` (`src/jls/elem/Register.java:747-791`) samples D unconditionally on the edge |
| physical time units | no | §1 dimensionless; `1 ns` in VCD is a fiction (below) |
| clock definition / period intent | no | `Clock` has `cycle`/`one` (`docs/simulation-semantics.md` §8.3) — a stimulus, not a constraint |
| static timing analysis, slack, fmax | no | no analysis pass exists anywhere in `src/` |
| back-annotation from an external tool | no | no reader, no instance-naming contract |
| switching-activity / power | no | toggles are computed and immediately thrown away |

**Verified negative:** `grep -rni "glitch\|hazard\|critical.path\|static timing\|
setup time\|hold time" src/ test/ docs/*.md` returns **eleven** hits, and every
one is either the *memory write* glitch hazard of issue #199
(`docs/simulation-semantics.md` §8.4, `test/jls/SimulationSemanticsRegressionTest.java:52/618`,
`test/jls/elem/MemoryModelTest.java:378`), the one sentence in
`docs/simulation-semantics.md:219` declaring that inertial glitch suppression
does *not* exist, an unrelated licensing "hazard"
(`docs/grand-architecture.md:212`), or "critical path" used as a
project-management metaphor (`docs/grand-architecture.md:412/450/490`). **JLS
has no glitch analysis, no hazard analysis, no critical-path analysis, and no
timing checks of any kind.**

**The ripple-carry question, answered precisely.** Can a student who draws a
ripple-carry adder from gates *see* the carry-chain glitch today?

- *Partially, by accident, and only if they already know to look.* Transport
  delay means transients are **simulated correctly** — a static-1 hazard on a
  carry does produce two scheduled transitions, and `WireNet.propagate` feeds
  every change to `Simulator.probeSample` (`src/jls/elem/WireNet.java:521-527`)
  and to `InteractiveSimulator.afterEvent` (`src/jls/edit/InteractiveSimulator.java:879-895`).
  So a probe placed on the carry wire *will* record the glitch, and
  `Trace.addValue` (`src/jls/edit/Trace.java:180`) will keep it.
- *But nothing tells the student it happened.* There is no glitch marker, no
  count, no "3 transients on this net", no zoom-to-transient. At the default
  `scaleFactor = 1` (`src/jls/edit/InteractiveSimulator.java:98`) a 5-tick
  glitch is 5 pixels wide next to a run that may be 100,000,000 ticks long
  (`JLSInfo.defaultTimeLimit`); at any scale factor a student would plausibly
  choose, `Trace.paintComponent`'s `int rlen = (int)Math.round(len)`
  (`src/jls/edit/Trace.java:~320`) rounds the pulse to **zero pixels** and it
  vanishes. The information is in the data structure and absent from the screen.
- *And the critical path is not shown at all* — there is no code that computes
  it. A student cannot ask "what is the longest path in my adder", and if they
  ask "how fast can I clock this", JLS has no answer, in any unit.

So: **the glitch is simulated, invisible in practice, and unexplained; the
critical path does not exist as a concept in the tool.** Both are first-year
digital-logic curriculum.

---

### The blocked standards

| # | Standard | What blocks it today (code) | Change that unblocks |
|---|---|---|---|
| **87** | **Liberty (`.lib`)** | No library layer at all: delays are Java constants compiled into each class (`Gate.Kind` `defaultDelay`, `src/jls/elem/Gate.java:81/104`; `Adder.defaultPropDelay = 30`, `Adder.java:33`; `Memory.defaultAccessTime = 100`, `Memory.java:53`). A `.lib` cell carries `cell_rise`/`cell_fall` **2-D tables indexed by input slew and output load**; JLS has one integer, no slew, no load, no fanout awareness (`WireNet.propagate` never counts sinks for timing). | **A** (per-arc/edge/corner delay) + **F** (time units) + **G** (technology-library layer) |
| **88** | Liberty CCS / ECSM | Current-source models: nonlinear I(V,t) waveforms. Needs a continuous electrical value domain. | **stays out** (see §"What genuinely stays out") |
| **89** | **IEEE 1497 SDF** | Four separate blocks. **C1 parse:** no reader. **C2 instance resolution:** JLS has stable ids (`jls.elem.ElementId`, `test/jls/StableElementIdTest.java`) and `Circuit.getElementsInStableOrder` but **no published hierarchical instance-naming contract**. **C3 represent:** `IOPATH a y (r:t:f)(r:t:f)` is per-arc, per-edge, three-corner; JLS has one scalar (`Gate.java:708`). `INTERCONNECT` is per-source/per-sink wire delay; `WireNet.propagate` is synchronous (`WireNet.java:443`). `TIMESCALE 100ps` needs units; §1 has none. **C4 `TIMINGCHECK`:** needs setup/hold storage, a last-change timestamp (`Input.java` has none), and a violation value. | **A** + **F** + **G** + **D** (timing checks) + optionally **H** (interconnect) |
| **90** | IEEE 1481 / SPEF | RC parasitics extracted from layout. No layout exists; belongs to the physical tier. | **stays out** |
| **91** | DSPF / RSPF | Same. | **stays out** |
| **92** | IEEE 1603 ALF | Superset of Liberty (timing + power + physical + functional). The timing/power half is unblocked by **A**+**G**; the physical half is Tier 7. | **A** + **G** (partial adoption only) |
| **93** | **SDC** | `create_clock -period 10 [get_ports clk]` presupposes (i) named ports at the top level — JLS *has* these (`HdlExporter.buildModel` port walk, used by `jls.hdl.board`, `docs/hdl-support-research.md` §7.5); (ii) physical time units — absent (§1); (iii) **something that consumes a constraint**, i.e. a static timing analyser — absent from `src/` entirely. `set_input_delay`/`set_output_delay`/`set_max_delay`/`set_false_path` are all inputs to an STA that does not exist. A `Clock` element is stimulus (`cycle`/`one`, §8.3), not intent. | **E** (STA engine) + **F** (units) + **C** (constraint object model) |
| **94** | IEEE 1801 UPF | Power intent: domains, isolation cells, level shifters, retention registers, power switches. JLS has no notion of a supply, no domain attribute on `SubCircuit`, and no element that can be *off*. Coherent for a schematic tool — just entirely unbuilt. | **I** (power/activity layer) + new elements; low priority |
| **95** | Si2 CPF | Legacy alternative to #94; adopting both is redundant. | **stays out** (superseded, has a successor) |
| **96** | IEEE 2416 | System-level power modelling, above the gate. | **stays out** (different tool class) |
| **97** | ITF / ICT | Proprietary process interconnect description. | **stays out** |
| **98** | Si2 OpenDFM | Manufacturability rule interchange. | **stays out** |
| **99** | IEEE 1801.1 | Application profiles on #94. | follows #94 |
| **72** | **SAIF** *(Tier 4, misfiled)* | Toggle-count interchange for power analysis. **JLS already computes every datum SAIF needs and discards it**: `WireNet.propagate` sees every net transition (`WireNet.java:443-525`); `Output.propagate` already does change detection at the source (`Output.java:139-145`). Nothing blocks SAIF except that nobody counts. | **I**, and **I alone** — the cheapest real item in this sweep |
| **82** | XDC/QSF/LPF *(timing half)* | The survey's ROADMAP entry and issue #213 cover **pin** constraints only (`jls.hdl.board.Boards`, PCF today). `create_clock`/`set_max_delay` in an XDC is the SDC dialect — same blocker as #93. | **E** + **F** + **C** |
| **67** | EVCD | Adds strength and direction per signal. JLS has one strength (driven) plus null-for-HiZ (`docs/simulation-semantics.md` §2). | value-domain change (sweep 01 territory) |
| **76 / 75** | Verilog netlist export / Yosys import | Not blocked — but **silently timing-lossy in both directions**. `VhdlEmitter`/`VerilogEmitter` emit no delay at all: `grep -n "after" src/jls/hdl/VhdlEmitter.java` matches **only the reserved-word list at line 902**. A drawn circuit and its exported HDL therefore simulate differently, and `test/jls/hdl/IverilogCompileTest.java:32` only *compiles* the output (`iverilog … -o out.vvp`, never `vvp`) — so no behavioural equivalence between JLS and any external simulator is checked anywhere in the tree. | **A** + **F** make delay-annotated emission possible and make a differential run meaningful |
| **65** | RISCOF / `riscv-arch-test` | Functionally reachable already. But a compliance claim about a *CPU* with no fmax, no critical path, and no setup/hold checking is a claim about function only. | **E** upgrades the claim |

---

### The changes, and what each unlocks

Nine changes. **A, F, E** are the spine; everything else hangs off them.

---

#### Change A — Structured element timing: per-arc, per-edge, min:typ:max

**What it is technically.** Replace the `int propDelay` field on all 16
`jls.elem.Timed` implementors with a `DelayModel` value object held once on
`LogicElement`:

- a table keyed by **arc** = (input pin index, output pin index);
- each arc carries a **rise** and **fall** delay (and, for `TriState`, a
  turn-on / turn-off pair, which the element already distinguishes internally
  via `TriStateOff` vs `NewValue`, `src/jls/sim/SimEvent.java:47/39`);
- each delay is a **min : typ : max** triple, with a run-wide corner selector
  (`min` / `typ` / `max` / `random-within`);
- optionally a `COND` predicate on input state.

The degenerate case — one arc covering all pins, rise = fall, min = typ = max —
**is exactly today's scalar**, so every existing `.jls` file loads and simulates
byte-identically. `Timed.getDelay()`/`setDelay()` (`src/jls/elem/Timed.java:31/39`)
stay as the scalar view.

The `react()` change is mechanical and local: `now+propDelay` becomes
`now + delays.arcDelay(pin, edgeOf(oldValue, newValue))`. Because `Gate.react`
is the shared archetype (`src/jls/elem/Gate.java:697-712`), the seven gate
subclasses are covered for free by editing `Gate` alone.

**Standards unlocked:** #89 SDF C3 (`IOPATH`, rise/fall pairs, `min:typ:max`
triples, `COND`) — this is the single largest of the four SDF claims; #87
Liberty (`cell_rise`/`cell_fall`/`rise_transition` are per-arc per-edge by
construction); #92 ALF's timing half; #82's timing half (needs A to have
anything to report); #76 delay-annotated Verilog emission (`assign #(3,5) y = …`)
which in turn makes a JLS↔Icarus differential test possible for the first time.

**Pedagogical capabilities unlocked:**

- Rise/fall asymmetry is the observable consequence of CMOS pull-up vs pull-down
  strength — currently unteachable in JLS, and the single most common
  "why is my waveform not symmetric" question in a first-year lab.
- Process corners become a *demonstration*: run the same drawing at min and max
  and watch a design that works at typ fail at max.
- Per-arc delay makes the carry chain honest. Today `Adder` fakes a ripple chain
  with `propDelay = bits * 30` (`src/jls/elem/Adder.java:261`) — a lumped scalar
  that produces the *right total* and **no intermediate carry transitions at
  all**, so the very phenomenon the delay number is modelling is unobservable.
  With arcs, a `carry-in → carry-out` arc is distinguishable from
  `a → sum`, and a gate-built ripple adder and the built-in `Adder` finally
  agree about what is happening inside.

**What JLS papers over today:**

1. `Adder.resetPropDelay()` sets `propDelay = bits * defaultPropDelay`
   (`src/jls/elem/Adder.java:261`) — a **hard-coded structural timing model of
   an element whose structure is not simulated**. The element asserts "I am a
   ripple-carry chain and here is my depth-dependent delay" while behaving as a
   single lumped black box. That is the timing analogue of the
   `VhdlEmitter.java` nine-value-coverage example in the brief.
2. `DelayGate.resetPropDelay()` is an empty override (`src/jls/elem/DelayGate.java:113-116`)
   with the comment "Cannot reset propagation delay". A special case exists
   solely because the scalar model has no way to distinguish
   *default-from-the-kind* from *characterised-by-the-user*. A `DelayModel` with
   a provenance field (`default` / `user` / `library` / `back-annotated`) deletes
   the special case and enables "Global → Reset Propagation Delays"
   (`Circuit.resetAllDelays`, `src/jls/Circuit.java:1721-1730`) to do the right
   thing per source.
3. `Timed.usesAccessTime()` (`src/jls/elem/Timed.java:~63`) exists **only to
   change a dialog label** between "access time" and "propagation delay",
   because a memory's access time and a gate's propagation delay are the same
   `int`. In an arc model, a memory access time is the `address→data` arc and a
   write time is the `clock→stored` arc; they are different arcs, not different
   captions.
4. `Memory` names its scalar `accessTime` and saves it under a *different*
   attribute name (`int time`, `src/jls/elem/Memory.java:444`) than every other
   element (`int delay`) — two names for one concept because the concept was
   never modelled.

**Size: 6–9 maintainer-weeks.** Field + `DelayModel` records and their
save/load attributes 1.5; the `Gate` archetype plus the 9 non-gate
`react()` bodies 2; edge-sense derivation for multi-bit values (per-bit? whole
value? — a genuine design decision needing a spec paragraph) 1; the
`docs/simulation-semantics.md` §6/§7 rewrite 1; UI (`DelayChangeDialog`,
`src/jls/edit/DelayChangeDialog.java`, becomes an arc table editor) 1.5;
golden re-derivation and new tests 1–2.

---

#### Change F — Bind simulation time to physical units (timescale + resolution)

**What it is technically.** A per-circuit `timescale` (unit + precision, e.g.
`1ns / 1ps`) saved as a new attribute on the `CIRCUIT` block. `long now` stays
an integer count of *precision* units; the timescale is the interpretation.
Delay attributes gain an optional real form (`0.37`), stored as a scaled
integer.

**Standards unlocked:** #89 SDF (`TIMESCALE` is mandatory in a `DELAYFILE`);
#87 Liberty (`time_unit : "1ns"`); #93 SDC (`create_clock -period 10` is
meaningless without a unit); #82; and it makes **#66 VCD honest**.

**Pedagogical capabilities unlocked:** fmax reported in MHz rather than "ticks";
"this 74LS00 has t_PLH = 15 ns" transcribed straight from a datasheet into the
drawing; the whole vocabulary of ns/ps that every subsequent course uses.

**What JLS papers over today.** `docs/batch-interface.md` §4.2 states it
outright: *"one VCD time unit represents one JLS simulation time unit. JLS time
units are abstract; the nominal `1 ns` is the mapping chosen for tool
compatibility."* JLS **already writes `$timescale 1 ns $end`** into every VCD it
produces (`docs/batch-interface.md` §4.2, pinned byte-exactly by
`test/jls/VcdExportGoldenTest.clockedRegisterVcdMatchesGoldenByteForByte`).
The exporter has been asserting a physical unit the simulator does not possess
since the day VCD export shipped — the same shape of gap as
`VhdlEmitter.java`'s nine-value coverage. GTKWave reads that file and confidently
labels the axis in nanoseconds.

**Size: 2–3 maintainer-weeks.** File-format attribute + evolution-policy note
(`docs/file-format.md` §9 — this is a "bump required" case, because an old
reader silently ignoring a timescale mis-interprets every number in the file) 1;
`docs/simulation-semantics.md` §1 rewrite 0.5; VCD emitter + golden 0.5;
CLI/display formatting 0.5.

---

#### Change E — A static timing analyser (the graph pass)

**What it is technically.** A new headless package `jls.timing` (must stay
inside the `HeadlessCoreRatchetTest` core surface — no AWT):

1. **Graph extraction.** Walk `Circuit.getElementsInStableOrder()` and the
   `WireNet` structure — both already exist and are already canonical
   (`src/jls/Circuit.java`, issue #181) — into a timing DAG. Nodes are
   (element, pin); edges are the intra-element arcs of Change A plus the net
   connections. Cut at sequential boundaries: `Register`, `StateMachine`,
   `Memory` (sync mode), and at `Clock`/`InputPin` sources.
2. **Arrival/required/slack.** Forward longest-path for arrival, backward from
   the clock period for required, slack = required − arrival. Standard,
   textbook, O(V+E), no solver.
3. **Report.** Worst negative slack, fmax = 1/(critical path + setup), the
   ranked top-N paths with their element-by-element breakdown.
4. **Combinational-loop detection** falls out free (a cycle in the DAG) — JLS
   today has *no* loop detection; a cross-coupled NAND latch is legal and
   intended, so this is a report, not an error, but it needs to be distinguished.

**Standards unlocked:** **#93 SDC** (the whole point of SDC is to feed an STA;
with an STA, `create_clock` / `set_input_delay` / `set_output_delay` /
`set_max_delay` / `set_false_path` / `set_multicycle_path` become a small,
well-specified parser over a thing that already knows what to do with them);
**#82** XDC/QSF/LPF timing sections; **#87** Liberty as the delay source;
**#89** SDF as the delay source *and* — importantly — as a possible **output**
(`write_sdf` of JLS's own computed delays, which flips SDF from a
back-annotation problem to a forward-annotation one and sidesteps the survey's
C2 objection entirely); **#65 RISCOF** gets upgraded from "the CPU computes the
right answers" to "the CPU computes the right answers and here is its critical
path".

**Pedagogical capabilities unlocked — this is the biggest column in the sweep:**

- **Critical path highlighted on the schematic.** The student's ripple-carry
  adder lights up red along the carry chain. Then they draw a carry-lookahead
  adder and watch the red path get shorter. This is *the* comparison the whole
  adder unit of a first-year course exists to teach, and today JLS can only
  assert it verbally.
- **fmax**, in MHz, for a drawn design. Currently unaskable.
- **Slack** as a concept, with a number attached, before the student ever meets
  an FPGA toolchain that will report it in an unfamiliar dialect.
- **Pipelining taught by measurement:** insert a register, watch the critical
  path halve and fmax double, and watch latency increase. That trade is
  currently a claim on a slide.
- **Logic-depth vs gate-count** as competing objectives — the real content of
  "optimisation" — instead of gate-count alone.
- **Combinational-loop diagnosis** for the student who accidentally drew one.

**What JLS papers over today.** Nothing, exactly — it simply refuses the
question. But note the *shape* of the refusal: `docs/standards-landscape.md`
§13.3 rejects SDF as "the first step onto a timing-engine slope". Under the
inverted frame the timing engine is not a slope, it is the deliverable; SDF is
the diagnostic that found it.

**Size: 5–8 maintainer-weeks** (analysis 3–4; report format + a normative
`docs/timing-analysis.md` 1; GUI critical-path overlay on the schematic canvas
1.5–2; tests 1). **Depends on A** for arc data (degrades to today's scalars if A
is not yet done — an STA over uniform delays is still useful and still finds the
carry chain).

---

#### Change D — Timing constraints on elements, and timing checks in the kernel

**What it is technically.** Two halves.

*Storage:* sequential elements gain `tSetup`, `tHold`, `tRecovery`, `tRemoval`,
`tMinPulseWidth` as new save attributes. Additive to `Register`,
`StateMachine`, and `Memory` (sync mode, issue #199).

*Checking:* the kernel must know **when each input last changed**. Today
`src/jls/elem/Input.java` is a bare value cell. Add `long lastChange`, written
in `Input.setValue` — which is called from exactly one place,
`WireNet.propagate` (`src/jls/elem/WireNet.java:499`) — plus `initInputs`
(`LogicElement.java:475-480`). Then `Register.react`'s `PosFF` arm
(`src/jls/elem/Register.java:770-780`) compares `now − inputs.get(0).lastChange`
against `tSetup` at the moment it detects the edge, and schedules a hold check
at `now + tHold`.

*Reporting:* a violation is (i) a `TellUser` diagnostic on the existing reporter
channel — the same one bus conflicts and unmatched state-machine transitions
already use (`WireNet.java:479`, `docs/simulation-semantics.md` §8.2/§9), with
the same warn-once-and-re-arm discipline; (ii) a marker in the trace;
and (iii) *optionally* an unknown output value, which is where this change
touches the value domain.

**On the X-state coupling.** The survey treats "TIMINGCHECK needs X" as fatal
(`docs/standards-adoption/11-costed-rejections.md`, failure mode 2: "the X-state
cascade"). It is a real coupling but it is **not a precondition**: a violation
can be *reported* without being *modelled*, and reporting is 90% of the teaching
value. Modelling it — driving X or a random-but-deterministic value on
violation, which is what real flip-flops approximately do — is the correct
eventual answer and belongs with the value-domain work (sweep 01). Sequence
them: report first, model second.

**Standards unlocked:** #89 SDF C4 (`TIMINGCHECK`: `SETUP`, `HOLD`, `SETUPHOLD`,
`RECOVERY`, `REMOVAL`, `WIDTH`, `PERIOD`); #87 Liberty (`timing_type :
setup_rising` / `hold_rising`, and `min_pulse_width` constraint groups); #93 SDC
(the checks are what `set_input_delay` is *for*).

**Pedagogical capabilities unlocked:**

- **Setup and hold become observable phenomena rather than definitions.** Today
  `Register.react` samples D at the edge with zero regard for how recently D
  moved (`Register.java:770-780`) — a student can build a design that violates
  every setup constraint in the universe and JLS will pronounce it correct.
  *That is a teaching failure, not just a modelling gap:* it teaches that
  flip-flops are magic.
- Metastability gets a home. Currently unrepresentable.
- The classic clock-skew demonstration — insert a delay gate in one clock leg,
  watch a hold violation appear — becomes a five-minute lab exercise.
- It closes the same class of gap as issue #199 closed for memory: #199 added a
  *synchronous write mode* precisely because level-sensitive writes let
  combinational transients corrupt memory (`docs/simulation-semantics.md` §8.4).
  The fix was to make the element ignore glitches. The *general* fix is to make
  the tool report them.

**What JLS papers over today.**

- The read-latest rule (`docs/simulation-semantics.md` §6.1) is documented with
  the explicit admission: *"Same-time races (e.g. a clock edge and a data change
  at the identical timestamp) are resolved by this read-latest rule plus FIFO
  event order — deterministically, but **with no setup/hold modeling**."* The
  normative document names the gap and moves on.
- Issue #199's whole existence — a new element mode, a new save attribute, a
  known file-format compatibility hazard (`docs/file-format.md` §9, "`Memory`'s
  `sync` attribute … is a known instance of this class"), and three new pinned
  tests in `test/jls/elem/MemoryModelTest.java` — is a targeted workaround for
  one instance of the general problem.
- `docs/simulation-semantics.md` §8.4's advice to "gate `WE` with the clock
  phase so it is asserted only after the datapath has settled" is a normative
  document telling the user to hand-verify a timing constraint the tool cannot
  check.

**Size: 4–6 maintainer-weeks** (`Input` timestamp + kernel plumbing 1; the three
sequential elements' checks 1.5; attributes + file format + evolution note 0.5;
reporting/trace markers 1; docs + tests 1–2). Excludes X-state modelling.

---

#### Change B — Inertial delay, and glitch/hazard detection as a first-class result

**What it is technically.** Three pieces:

1. A per-element **delay type** (transport | inertial | inertial-with-reject-width).
   In `Gate.react`'s `PinChanged` arm (`src/jls/elem/Gate.java:697-708`),
   inertial means: if an output event is already pending for this element and
   the new event would fire within the rejection window, **cancel** the pending
   event. That requires `Simulator` to support **event cancellation**, which it
   does not today — `post` (`src/jls/sim/Simulator.java:165-170`) can suppress
   an exact duplicate via `dupCheck` but cannot withdraw. Add
   `Simulator.cancel(SimEvent)` (a `PriorityQueue.remove` plus a `dupCheck`
   removal, both O(n) — acceptable, the queue is small, and the hot loop is
   otherwise untouched).
2. A **glitch detector** in the kernel that does not depend on (1): when an
   element schedules a new output value while a different value is already in
   flight (`toBeValue` differs, the condition already computed at
   `Gate.java:706`), record a transient. This is a counter and a trace marker;
   it costs one field.
3. **Display**: transients rendered as a distinct mark on the trace even when
   sub-pixel (fixing the `Math.round(len) == 0` erasure in
   `src/jls/edit/Trace.java`), a per-net glitch count, and an editor overlay
   that marks glitching nets on the schematic.

**Standards unlocked:** #89 SDF `PATHPULSE` / `PATHPULSEPERCENT` (pulse
filtering is exactly this); IEEE 1076 VHDL fidelity — a plain VHDL signal
assignment is **inertial by default** and requires the `transport` keyword to be
otherwise, so **JLS's exported VHDL does not have JLS's semantics**, and today
that is undetectable because `VhdlEmitter` emits no delays at all (`after` at
`src/jls/hdl/VhdlEmitter.java:902` is only the reserved-word list). Same for
Verilog's `#` delay with its pulse-control defaults.

**Pedagogical capabilities unlocked:** this is the change that answers the
brief's ripple-carry question directly.

- **Static and dynamic hazards become visible, named, and countable.**
  Hazard analysis — "a static-1 hazard on this net because these two product
  terms don't overlap" — is core first-year Karnaugh-map content, and its whole
  motivation is a glitch the student cannot currently see.
- **The redundant-consensus-term lesson lands.** Add the consensus term, watch
  the glitch count go to zero. Today the student adds a term that does nothing
  to the truth table and has to take the instructor's word for why.
- **Transport vs inertial is itself the lesson** about why real gates swallow
  narrow pulses, and why simulators lie differently.
- It explains issue #199's memory corruption to a student instead of hiding it
  behind a checkbox.

**What JLS papers over today.** `docs/simulation-semantics.md:219` — *"There is
no inertial-delay glitch suppression; the only suppression is the
equal-pending-event rule of section 3 and the `toBeValue` change check."* The
document states the limitation and the tool proceeds to simulate glitches
faithfully and render them invisibly.

**Size: 3–4 maintainer-weeks** (cancellation in `Simulator` 0.5; delay-type
attribute across the 16 `Timed` classes 1; detector 0.5; trace + schematic
rendering 1; docs/tests 1). The **detector half alone is ~1.5 weeks and is the
highest teaching-value-per-week item in this entire sweep** — it needs no file
format change, no new value domain, and no arcs.

---

#### Change G — A technology-library layer (JLS elements become characterised cells)

**What it is technically.** A `TechLibrary` object: a named, versioned mapping
from (element kind, bit width, pin count) to a `DelayModel` (Change A), plus
area, plus energy-per-transition (Change I), plus constraint values (Change D).
Loadable from a **Liberty subset reader** (`src/jls/hdl/lib/LibertyReader.java`,
headless, in `jls.hdl` beside the existing HDL machinery). A circuit gains a
`library` attribute. "Global → Reset Propagation Delays"
(`Circuit.resetAllDelays`, `src/jls/Circuit.java:1721`) becomes "apply library".
JLS ships two built-ins: `jls_default` (today's constants, so nothing changes)
and one datasheet-derived TTL-ish library for teaching.

**Standards unlocked:** #87 Liberty (read subset); #92 ALF timing/power; and —
critically — **it dissolves the survey's C2 objection to #89 SDF**. The costed
rejection says: *"SDF is keyed to `CELLTYPE` + `INSTANCE` — cell instances of a
technology library. A JLS drawing has no cells."* Once JLS publishes a library
whose cell names *are* its element kinds and an instance-naming contract built
on `jls.elem.ElementId` + `Circuit.getElementsInStableOrder`, a JLS drawing
**does** have cells, and SDF instance resolution has a target. The rejection's
conclusion — *"a private format wearing an IEEE number"* — assumed JLS would
never own a library; owning one is the change.

It also repairs the rejection's **oracle** argument. That section concludes SDF
"cannot currently be asserted credibly" because Icarus Verilog's `$sdf_annotate`
is broken. But the natural oracle for a Liberty+SDC+netlist timing claim is
**OpenSTA** (open source, reads Liberty, reads structural Verilog — which JLS
already emits, `test/jls/hdl/VerilogExportGoldenTest.java` — reads SDC, and
writes SDF), not Icarus. That gives the house four-part conformance pattern
(`docs/standards-adoption/11-costed-rejections.md`) its missing part 3: an
independent third-party consumer, skip-when-absent, in the pattern of
`test/jls/hdl/ToolLocator.java`. *[OpenSTA's exact capabilities and licence are
asserted from general knowledge and are **unverified against a primary source in
this sweep** — verify before relying on it.]*

**Pedagogical capabilities unlocked:**

- **Datasheet literacy.** "Open the 74HC00 datasheet, find t_PD, put it in the
  library, re-run." A first-year student meeting a real datasheet with a real
  consequence.
- **Technology comparison as an experiment:** the same drawing at TTL speeds and
  at CMOS speeds, with different fmax.
- The library file is a legible artifact a student can read and edit — unlike
  `defaultDelay` constants buried in `Gate.Kind` (`src/jls/elem/Gate.java:81`).

**What JLS papers over today.** The delay defaults are *characterisation data
compiled into Java source*: AND/OR/XOR 10, NAND/NOR/NOT 5, Mux 25, Decoder 15,
Register 50, Memory 100, Adder 30×bits
(`docs/simulation-semantics.md` §7 and the constants it anchors). They encode a
technology nobody names, they cannot be changed without recompiling, and
`docs/simulation-semantics.md` §7's table is a *library datasheet published as a
normative simulator document*.

**Size: 4–6 maintainer-weeks** (Liberty subset reader + rejection diagnostics
2–3 — the reader must *refuse* everything outside the profile loudly, per the
house pattern; `TechLibrary` model + circuit attribute 1; two shipped libraries
1; the OpenSTA CI lane 1). **Depends on A.**

---

#### Change C — A constraint object model (SDC/XDC ingestion)

**What it is technically.** A `TimingConstraints` object attached to a circuit,
populated either from an SDC/XDC file or from a GUI panel, holding: clock
definitions (period, waveform, source port), input/output delays, false paths,
multicycle paths, max-delay exceptions, and clock groups. Consumed by Change E.
A **Tcl-subset** parser is the honest form (SDC is Tcl); a line-oriented
recogniser for the ~12 commands that matter is the pragmatic one, and must
reject unrecognised commands loudly rather than ignoring them.

**Standards unlocked:** #93 SDC directly; #82 XDC/QSF/LPF timing sections
(same command vocabulary, different spellings — and these are already on the
roadmap as #213 follow-ups for their *pin* half, so this completes an item the
project has already committed to half of).

**Pedagogical capabilities unlocked:** design *intent* becomes a written,
checkable artifact separate from the drawing — which is the single biggest
conceptual jump between "I drew a circuit" and "I designed to a specification".
And the constraint file a student writes for JLS is the same file they will
hand Vivado.

**Size: 2–3 maintainer-weeks.** Only useful after E.

---

#### Change H — Per-sink interconnect delay (retiring the ideal wire)

**What it is technically.** `WireNet.propagate` (`src/jls/elem/WireNet.java:443`)
currently mutates each sink `Input` in place and posts a same-time
`PinChanged`. Replace with: schedule a new `SimEvent.Payload` record
`SetInput(Input in, BitSet value)` at `now + netDelay(source, sink)`. Because
`Payload` is a **sealed interface** (`src/jls/sim/SimEvent.java:23`), adding one
record is a compile error at every one of the **25** `react()` implementations
until handled — which is the design working as intended, and makes the blast
radius exactly measurable.

Delay source: either explicit SDF `INTERCONNECT` values, or — better for
teaching — computed from **fanout**, which the net already knows (it iterates
`ends` to find sinks, `WireNet.java:486-508`).

**Standards unlocked:** #89 SDF `INTERCONNECT` and `PORT`; #87 Liberty's
load-dependent delay tables (the "output load" axis of `cell_rise`);
partial #90 SPEF in the sense that fanout is the poor man's parasitic —
though SPEF itself stays out.

**Pedagogical capabilities unlocked:** **fanout has consequences.** Today a net
driving one input and a net driving twenty inputs are timing-identical in JLS.
That is not a simplification, it is a falsehood, and it is the reason buffer
trees exist — a topic JLS cannot currently motivate.

**What JLS papers over today.** `docs/simulation-semantics.md` §6.1 elevates the
limitation to a normative rule: *"Wires are ideal … There is no per-wire or
per-segment delay."*

**Size: 3–5 maintainer-weeks, and this is the risky one.** It changes the
read-latest rule of §6.1, and therefore same-time race resolution in circuits
with no interconnect delay configured at all. **Must be opt-in per circuit**
(default zero net delay ⇒ scheduled-at-`now` ⇒ identical FIFO ordering ⇒
byte-identical results). `test/jls/RiscvCpuGoldenTest.java` is the canary and
must be run on every commit of this change.

---

#### Change I — Switching activity and power accounting

**What it is technically.** A toggle counter: `WireNet.propagate` already
computes "this net changed" and `Output.propagate` already does source-side
change detection (`src/jls/elem/Output.java:139-145`). Add per-net and
per-element counters (`toggles`, `timeHigh`, `timeLow`) behind the same gate the
trace machinery already uses (`JLSInfo.printTrace || vcdFileName != null`,
`src/jls/sim/BatchSimulator.java:144`). Then:

- **SAIF writer** — toggle counts and duty cycles, which is literally all SAIF
  contains;
- **energy estimate** — toggles × energy-per-transition from the library
  (Change G) → dynamic power, plus static power × time.

**Standards unlocked:** **#72 SAIF** — and this is the standout finding of the
sweep. The survey filed SAIF as OTHER in Tier 4 on the general ground that
JLS is a two-state gate simulator, but SAIF needs **no** value-domain change,
**no** timing change, **no** library, and **no** units. JLS already computes
every number in a SAIF file and discards it in `WireNet.propagate`. Partial #87
(Liberty power groups) and #92 (ALF power) follow once G exists. #96 IEEE 2416
stays out.

**Pedagogical capabilities unlocked:**

- **Power as a design axis at all.** Currently JLS teaches that circuits have
  correctness and (implicitly) speed. Power is the third axis of every real
  design decision and is entirely absent.
- Activity factor, clock gating, and glitch power connect directly to Change B:
  *glitches cost energy*, which is the strongest possible motivation for hazard
  elimination and is currently unavailable.
- Toggle counts double as a **coverage-ish** signal for the test-vector work
  ("this net never toggled during your test").

**What JLS papers over today.** Nothing is faked here — the capability is simply
absent while its input data flows through the hot loop every cycle.

**Size: 2–3 maintainer-weeks** for counting + SAIF (1.5 without the library);
**+1–2** for energy estimation once G exists.

---

### Ripple effects

**Normative documents (all three are contracts, all three move).**

- `docs/simulation-semantics.md` — §1 (time units, Change F), §6.1 (ideal wires,
  Change H), §6.2 (transport-only discipline, Change B), §7 (the entire
  per-element delay table becomes a *default library*, Change A/G), §8.1 (setup/hold
  in `Register`, Change D), §12 (the golden-mapping table). This is the largest
  single edit; the document explicitly requires that any change be *specified
  first*, never a silent behavioural difference (`ARCHITECTURE.md`, "Simulation
  execution strategy", recorded 2026-07-26).
- `docs/batch-interface.md` — §4.2 timescale stops being a fiction (F); §3.4 and
  §4.3 value display if a violation value is ever added (D). §6 is a **frozen
  stability promise**: any observable byte change requires a CHANGELOG entry
  plus a major version bump or a compatibility flag. Changes A/B/E/G/I are all
  *additive behind flags* and clear this bar; F does not, because it changes the
  `$timescale` line, so F must ship with a major bump or keep `1 ns` as default.
- `docs/file-format.md` — §5 (new attributes), §7 (new element types only if UPF
  work happens), §9 **evolution policy**. Per §9, adding an attribute needs no
  version bump *but* the "silent-drop caveat" applies with force here: an old
  reader that ignores `min:typ:max` or a timescale doesn't lose decoration, it
  **mis-simulates**. §9 already says writers "SHOULD prefer a version bump over
  an 'ignorable' attribute whenever dropping the attribute would change
  simulation behavior" — so F and A are FORMAT 3 material, and the open question
  currently tracked against `Memory`'s `sync` attribute gets decided along the way.
- New: `docs/timing-analysis.md` (E), `docs/liberty-profile.md` (G),
  `docs/sdf-profile.md` (89), `docs/sdc-profile.md` (C) — each in the house
  four-part conformance pattern.

**Element `react()` methods: 25 total** (`grep -rn "public void react(" src/`),
in `Adder`, `Binder`, `Clock`, `Constant`, `Decoder`, `Display`, `Extend`,
`Gate`, `InputPin`, `JumpEnd`, `JumpStart`, `LogicElement`, `Memory`, `Mux`,
`OutputPin`, `Pause`, `Register`, `ShiftRegister`, `SigSim`, `Splitter`,
`StateMachine`, `Stop`, `SubCircuit`, `TriState`, `TruthTable`.

- **Change A** touches the **10** that post delayed events: `Gate` (which covers
  all 7 gate subclasses at once), `Adder`, `Decoder`, `Mux`, `Register`,
  `ShiftRegister`, `StateMachine`, `TriState`, `TruthTable`, `Memory`.
- **Change D** touches **3**: `Register`, `StateMachine`, `Memory`.
- **Change H** touches **all 25**, by construction: `SimEvent.Payload` is sealed
  (`src/jls/sim/SimEvent.java:23`) and every `react` switches exhaustively with
  no default arm, so a new payload record is a compile error everywhere until
  handled. This is the intended safety property and also the honest cost.

**The simulation hot loop.** `Simulator.runEventLoop`
(`src/jls/sim/Simulator.java:215-243`) is 12 lines and should stay that way.
Changes A, D, G, I add **zero** loop work (all inside `react`). Change B adds
`Simulator.cancel` — an O(n) `PriorityQueue.remove`, called rarely. Change H is
the only one that increases event volume, roughly by the sink count of changed
nets, and is the reason it must be opt-in. `ARCHITECTURE.md`'s recorded decision
(single discrete-event interpreter, revisit trigger = "a concrete CPU-scale
design that is unusably slow interactively") is the right place to record any
observed regression; note that Change H makes hitting that trigger materially
more likely.

**The GUI.** `src/jls/edit/DelayChangeDialog.java` becomes an arc/corner table
editor (A). `src/jls/edit/Trace.java` needs sub-pixel transient rendering — the
`int rlen = (int)Math.round(len)` path currently erases short pulses — plus
glitch markers and violation markers (B, D). The schematic canvas needs a
critical-path overlay and a glitching-net overlay (E, B); `jls.edit`'s
`SimpleEditor` menu (`src/jls/edit/SimpleEditor.java:1066` "Change Timing")
gains a Timing menu. `src/jls/edit/InteractiveSimulator.java`'s `scaleFactor`
(line 98) needs a zoom-to-event affordance. All GUI work must stay outside the
core — `test/jls/HeadlessCoreRatchetTest.java` enforces it, and `jls.timing`
must be headless.

**Existing saved circuits.** Every change is designed additive-with-identity:
absent attribute ⇒ today's scalar ⇒ byte-identical simulation. The one genuine
migration is F (timescale), which needs either a default of "1 tick = 1 ns" (a
lie made explicit and harmless) or a FORMAT bump.

**Existing tests.** The goldens that must be re-derived or explicitly asserted
unchanged: `test/jls/BatchSimulationGoldenTest.java`,
`test/jls/SequentialGoldenTest.java`, `test/jls/VcdExportGoldenTest.java`
(especially `clockedRegisterVcdMatchesGoldenByteForByte`, which pins the
`$timescale` line, and `vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`, which
pins the absence of X), `test/jls/SimulationSemanticsRegressionTest.java` (all
seven S1–S7 pins), `test/jls/ElementSimulationGoldenTest.java`,
`test/jls/ShiftRegisterTest.java`, `test/jls/elem/MemoryModelTest.java`,
`test/jls/elem/RegisterModelTest.java`, `test/jls/VcdProbeExportTest.java`,
`test/jls/BatchTracePrinterTest.java`. **`test/jls/RiscvCpuGoldenTest.java` is
the differential oracle** named by `ARCHITECTURE.md`'s equivalence criterion and
must stay green through every one of these changes. New attributes drag in
`test/jls/elem/AttributePersistenceTest.java`,
`test/jls/AllElementsRoundTripTest.java`, `test/jls/FileFormatSpecTest.java`,
`test/jls/GenerativeRoundTripFuzzTest.java`; new CLI flags drag in
`test/jls/CliFlagTableTest.java` and `test/jls/CliSmokeTest.java`
(`JLSStart.FLAGS`, `src/jls/JLSStart.java:759`).

**A gap this sweep exposes independently of any change:**
`test/jls/hdl/IverilogCompileTest.java:32` and `GhdlCompileTest.java` only
**compile** exported HDL; nothing ever *runs* it and compares to JLS. There is
no behavioural equivalence check between JLS and any external simulator in the
tree. Changes A + F are what would make such a check meaningful (a zero-delay
comparison is possible today, and is worth doing as a standalone ~1-week item).

---

### What genuinely stays out, and why

Judged only against the three legitimate grounds (different tool class /
technically incoherent for a schematic-first logic simulator / obsolete with no
successor).

- **#88 Liberty CCS & ECSM** — *technically incoherent here.* These are
  current-source models: nonlinear I(V,t) driver waveforms and receiver
  capacitance. Consuming them requires an analogue solver, which is a different
  tool (SPICE), not a deeper digital model. The *scalar* Liberty NLDM tables
  (#87) are the digital-simulator-appropriate half and are in scope.
- **#90 IEEE 1481 / SPEF, #91 DSPF / RSPF, #97 ITF / ICT** — *different tool
  class.* These describe RC parasitics extracted from a physical layout. JLS has
  no layout and, per `docs/standards-landscape.md` §8's own correct conclusion,
  never will. The *consequence* of parasitics that matters pedagogically —
  fanout-dependent delay — is captured by Change H without any of these formats.
- **#96 IEEE 2416** — *different tool class.* System-level power modelling for
  IP blocks and processors, consumed by architectural exploration tools above
  the gate level.
- **#98 Si2 OpenDFM** — *different tool class.* Manufacturability rule
  interchange for mask/fab.
- **#95 Si2 CPF** — *obsolete with a successor.* Explicitly the legacy
  alternative to IEEE 1801 UPF; supporting both would be duplicated work for the
  same intent. If power intent is ever done, do #94.
- **#70 FSDB, #71 WLF** *(adjacent, for completeness)* — proprietary, no public
  specification.

Everything else in Tier 6 — #87, #89, #92, #93, #94, #99 — is **in scope**
under this frame, gated on Changes A/D/E/F/G/I, and #94 (UPF) in particular is
declined here only on *priority*, not on principle: power domains, isolation
cells, and retention registers are perfectly expressible as subcircuit
attributes and new elements, and would be a genuinely novel teaching capability
for a tool at this level. It is listed last because it depends on Change I
existing first.

---

### Sources

**Repository (all verified by reading at HEAD):**

- `docs/simulation-semantics.md` — §1 (time model), §2 (value domain), §3
  (events/ordering), §4 (loop/termination), §6.1 (ideal wires, read-latest,
  "no setup/hold modeling"), §6.2 (transport delay, line 219: "no
  inertial-delay glitch suppression"), §7 (the per-element delay table), §8.1
  (`Register`), §8.3 (`Clock`), §8.4 (memory glitch hazard, issue #199), §9
  (tri-state resolution), §12 (golden mapping).
- `docs/batch-interface.md` — §4.2 (`$timescale 1 ns` as an admitted fiction),
  §4.3 (value mapping, "`x` never appears"), §5, §6 (stability promise).
- `docs/file-format.md` — §5, §7 (32 tags), §9 (evolution policy, silent-drop
  caveat, the open `sync`-attribute question).
- `docs/standards-landscape.md` — §7 Tier 6 table (#87–#99, lines 282–305),
  §5 Tier 4 (#66–#73), §6 Tier 5 (#82), §13.1/§13.3 (rankings and the SDF
  rejection), §14 tally.
- `docs/standards-adoption/11-costed-rejections.md` — §1 (SDF: claims C1–C4, the
  25–40 maintainer-day floor, the three failure modes, the Icarus-oracle
  argument, the four-part house conformance pattern).
- `docs/hdl-support-research.md` — §7.2 (Yosys cell mapping gaps), §7.5 (board
  export, `jls.hdl.board`, PCF).
- `ARCHITECTURE.md` — "Simulation execution strategy" (recorded 2026-07-26,
  #221): single interpreter, the equivalence criterion, `RiscvCpuGoldenTest` as
  differential oracle.
- `src/jls/sim/Simulator.java` — 25/27 (queue + dupCheck), 36 (`now`), 38
  (`maxTime`), 165–170 (`post`, no cancel), 177–201 (`initSimulation`),
  215–243 (`runEventLoop`), 285 (`probeSample`).
- `src/jls/sim/SimEvent.java` — 23 (sealed `Payload`), 30/39/47/56/65/73/83
  (the seven payload records), 113–120 (constructor, seq), 134–150 (`compareTo`).
- `src/jls/sim/BatchSimulator.java` — 112–131 (`runSim`), 140–180 (`afterEvent`),
  144 (trace gate), 250–277 (`findProbes`), 294–299 (`probeSample`).
- `src/jls/elem/Timed.java` — 25 (interface), 31/39 (`getDelay`/`setDelay`),
  ~63 (`usesAccessTime`, the dialog-label-only distinction).
- `src/jls/elem/Gate.java` — 50 (`propDelay`), 81/96/104 (`Kind.defaultDelay`),
  316–326 (the `int delay` save attribute), 520–532 (`getDefaultDelay`,
  `resetPropDelay`), 655 (`toBeValue`), 697–712 (`react`, incl. 708 the
  `now+propDelay` post).
- `src/jls/elem/DelayGate.java` — 19–20 (`Kind("DELAY","DelayGate",1,0)`),
  102 (`infoText`), 113–116 (empty `resetPropDelay`), 126–132 (`computeOutput`).
- `src/jls/elem/Adder.java` — 33 (`defaultPropDelay = 30`), 259–262
  (`propDelay = bits * defaultPropDelay`), 410 (post).
- `src/jls/elem/Register.java` — 54 (`defaultPropDelay = 50`), 720–735
  (`initSim`), 745–800 (`react`: `Latch`/`PosFF`/`NegFF` arms, 768/779/790 posts).
- `src/jls/elem/Memory.java` — 53 (`defaultAccessTime = 100`), 108, 444
  (`int time` save attribute), 1384/1396/1402 (posts).
- `src/jls/elem/WireNet.java` — 20–30 (insertion-ordered `ends`/`wires`),
  404–434 (net value), 443–525 (`propagate`: 455–484 tri-state resolution and
  the bus-conflict `TellUser.warn`, 486–508 synchronous sink delivery,
  521–527 `probeSample`).
- `src/jls/elem/Output.java` — 136–169 (`propagate`, source-side change
  detection at 139–145).
- `src/jls/elem/Input.java` — 13, 59, 72 (value only; **no timestamp**).
- `src/jls/elem/LogicElement.java` — 470–480 (`initInputs`), 484–488
  (`resetPropDelay` default no-op).
- `src/jls/Circuit.java` — 1721–1730 (`resetAllDelays`).
- `src/jls/hdl/VhdlEmitter.java` — 470–496 (the `when others` full-coverage
  comment cited in the brief), 902 (`"after"` present **only** as a reserved
  word — no delay is ever emitted).
- `src/jls/edit/InteractiveSimulator.java` — 92–100 (traces/probes), 98
  (`scaleFactor = 1`), 851–871 (`beforeReact`), 879–895 (`afterEvent`),
  973–1015 (`findTraces`).
- `src/jls/edit/Trace.java` — 78 (`scaleFactor`), 164, 180–199 (`addValue`),
  228–330 (`paintComponent`, incl. the `Math.round(len)` sub-pixel erasure).
- `src/jls/sim/TraceGeometry.java` — `MIN_TIC_GAP`, `ticIncrement`,
  `labelStride`.
- `src/jls/edit/DelayChangeDialog.java` — 16–18, 69 ("Change Timing").
- `src/jls/edit/SimpleEditor.java` — 1066, 1623, 5121–5133 (`doTiming`).
- `src/jls/JLSStart.java` — 759 (`FLAGS` table).
- Tests: `test/jls/BatchSimulationGoldenTest.java`,
  `test/jls/SequentialGoldenTest.java`, `test/jls/VcdExportGoldenTest.java`,
  `test/jls/SimulationSemanticsRegressionTest.java` (52, 618 — the #199 glitch
  hazard), `test/jls/elem/MemoryModelTest.java` (378),
  `test/jls/RiscvCpuGoldenTest.java`, `test/jls/hdl/IverilogCompileTest.java`
  (26, 32 — compile-only, `-o out.vvp`, never executed),
  `test/jls/hdl/GhdlCompileTest.java`, `test/jls/hdl/ToolLocator.java`,
  `test/jls/HeadlessCoreRatchetTest.java`, `test/jls/StableElementIdTest.java`,
  `test/jls/sim/TraceGeometryTest.java`, `test/jls/sim/SimEventDedupTest.java`.
- Counting commands used: `grep -rn "public void react(" src/ --include=*.java`
  → **25**; `grep -rn "implements Timed\|Timed," src/jls/elem/*.java` → **16**;
  `grep -rni "glitch\|hazard\|critical.path\|static timing\|setup time\|hold time"
  src/ test/ docs/*.md` → 11 hits, none of them timing analysis.

**External documents (named, not fetched during this sweep — treat as
orientation):**

- IEEE Std 1497-2001 SDF / IEC 61523-3:2004 — `DELAYFILE`, `TIMESCALE`, `CELL`,
  `IOPATH`, `INTERCONNECT`, `PORT`, `TIMINGCHECK`, `PATHPULSE`, `COND`,
  `min:typ:max`. Paywalled. **Unverified in this sweep**; the construct list is
  taken from `docs/standards-adoption/11-costed-rejections.md` §1, which cites
  it.
- Liberty (Synopsys, open-published) — `cell_rise`/`cell_fall` NLDM tables
  indexed by input transition and output capacitance, `timing_type :
  setup_rising`/`hold_rising`, `time_unit`, internal/leakage power groups.
  **Unverified in this sweep.**
- SDC (Synopsys, Tcl-based) — `create_clock`, `set_input_delay`,
  `set_output_delay`, `set_max_delay`, `set_false_path`, `set_multicycle_path`,
  `set_clock_groups`. **Unverified in this sweep.**
- IEEE 1801 UPF, Si2 CPF, IEEE 1603 ALF, IEEE 2416, Si2 OpenDFM, IEEE 1481/SPEF
  — scope characterisations taken from `docs/standards-landscape.md` §7's own
  "Governs" column. **Not independently verified.**
- SAIF — toggle counts and duty cycles for power analysis
  (`docs/standards-landscape.md` #72). **Not independently verified.**
- OpenSTA as the candidate independent oracle (reads Liberty + structural
  Verilog + SDC, writes SDF). **Asserted from general knowledge; unverified —
  verify licence and capabilities before this is load-bearing.**
- steveicarus/iverilog#943 (Icarus SDF support incomplete) — cited from
  `docs/standards-adoption/11-costed-rejections.md`; **not re-verified here.**
