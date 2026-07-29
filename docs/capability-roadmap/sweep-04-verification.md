## Verification, assertion, and coverage standards

*Sweep 04 of the capability-expansion re-examination. Survey entries #48–#65
(`docs/standards-landscape.md:204–221`), plus #73 (line 245) and #259
(line 618). Every JLS claim below is anchored to a path in the real tree.*

**The paragraph this sweep exists to refute** is
`docs/standards-landscape.md:222–229`:

> **JLS's position:** it has a verification story (`-t` test vectors,
> differential fuzzing in `riscv/fuzz_diff.py`, golden files) that
> conforms to *none* of these and shouldn't.

That is a preservation verdict wearing the clothes of a scope verdict. It
is true that JLS should not implement UVM. It does not follow that JLS
should have no assertion capability, no coverage model, and no stimulus
generator — those are three different things, and the survey collapsed
them into one "OTHER" because each *individually* would require changing
the simulation model. Under the capability frame the correct reading is
the opposite: this tier is where JLS's model is thinnest, because it is
the only tier where JLS has **no model at all**. There is no object in
the tree that represents "a property that must hold." There is no object
that represents "something that was exercised." Both absences are
currently paid for in Python, in `TellUser.warn` one-shot dialogs, and in
hard-coded expected strings in `examples/autograde/autograde.py`.

The headline finding, stated once up front because it inverts the survey's
cost intuition: **this program is cheap in the element model and expensive
only in the frozen contracts.** JLS has 25 files under `src/jls/elem/`
defining `public void react` — and the assertion/coverage program needs to
edit approximately **three** of them. The kernel seams it needs
(`Simulator.afterEvent` at `src/jls/sim/Simulator.java:269`,
`Simulator.probeSample` at `:285`, `Simulator.runEventLoop` at `:215`,
`WireNet.propagate` at `src/jls/elem/WireNet.java:443`) all already exist
and are all already used for exactly this shape of work by the VCD
exporter. Compare the SDF rejection in
`docs/standards-adoption/11-costed-rejections.md`, which correctly prices
an X-state value domain as touching all 25. The verification tier is not
that. It was declined as though it were.

---

### The blocked standards

| # | Standard | What specifically blocks JLS today (code) | What change unblocks it |
|---|---|---|---|
| 48 | IEEE 1800.2 **UVM** | A SystemVerilog class library requiring SV class semantics, phasing, factory, and constrained randomization. `src/jls/hdl/` is **emit-only** (`VerilogEmitter.java`, `VhdlEmitter.java`); JLS executes no HDL, and live co-simulation is a recorded rejection (`docs/vcd-interop.md:18–22`, issue #63). | Nothing — and correctly so *as a library*. But UVM standardizes a **shape** (generator → driver → monitor → scoreboard → coverage) that JLS already hand-builds in `riscv/verify.py:compare` (a scoreboard written in Python) and `riscv/fuzz_diff.py:rand_program` (a generator written in Python). Changes **D** + **E** give that shape a home in the tool. |
| 49 | **SVA** (in IEEE 1800) | Four independent blocks. (1) No assertion object: `src/jls/elem/ElementRegistry.java` registers 33 element types, none of which expresses a property. (2) No **sampling** notion: edge detection is privately re-implemented in `Register.react` (`currentC`), `StateMachine.react` (`oldClock`, `src/jls/elem/StateMachine.java:730–753`), and `Memory.react`'s sync mode — there is no shared clocking abstraction to sample against, and `docs/simulation-semantics.md` §6.1's read-latest rule means a signal can hold several values *within* one timestamp. (3) No X: `docs/simulation-semantics.md` §2, two states plus HiZ, pinned by `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`. (4) No failure channel: `docs/batch-interface.md` §1 defines exit codes 0/1/2 with no "property failed" outcome, and §3's stdout format is frozen by §6. | **A** (sampling/timestamp closure) + **B** (Assert element + report channel) + **C** (minimal temporal subset) + **H** (contract change). X is *not* required for the useful subset. |
| 50 | IEEE 1850 **PSL** | Same four blocks as #49. PSL is additionally the more natural target because it is language-neutral (VHDL and Verilog flavors), and JLS emits both. | Same as #49. PSL's *simple subset* (`always`, `never`, `next`, bounded `until`, `->`) is a near-exact match for the minimum temporal fragment in **C**. |
| 51 | IEEE 1647 `e` | A full aspect-oriented verification language with essentially one implementation. | Declined on grounds, see below — the constrained-random capability is delivered by **E** without the syntax. |
| 52 | Accellera **PSS** | Stimulus in JLS is a *fixed schedule posted before the run*: `SigSim.initSim` (`src/jls/elem/SigSim.java`) parses the whole `-t` file and calls `sim.post(new SimEvent(...))` for every value at parse time (`docs/batch-interface.md` §2.3). Nothing can generate a stimulus value from what the circuit did. PSS additionally targets SoC software-driven test. | **E** (reactive stimulus SPI + constrained-random generator) delivers the capability. PSS-the-language stays out (different tool class). |
| 53 | Accellera **UCIS** | JLS measures **no coverage of any kind**. Nothing counts net toggles, `State` visits, `State` transition firings, or `TruthTable` row selections. There is no coverage database and no export. | **D** (coverage model) + a UCIS-shaped printer. This is the one entry in the tier where JLS could make a genuine *conformance* claim, in the same house pattern as VCD (#66) and PCF (#81). |
| 54 | Accellera **SCE-MI** | Transaction transport between a host and a hardware emulator. | Declined on grounds (different tool class). |
| 55 | **OSVVM** | A VHDL package library; JLS emits structural VHDL (`src/jls/hdl/VhdlEmitter.java:15`) and does not run it. | Declined as a library. But OSVVM's `CoveragePkg` (bins over crossed ranges, with a "holes" report) is the best available **design prior art** for **D**'s bin model, and should be read before designing it. |
| 56 | **UVVM** | Same as #55. | Same as #55. |
| 57 | **VUnit** | Same as #55 — a VHDL/SV test runner. | Declined as a framework. Its **artifact shape** — an xUnit XML result file — is adopted in **H**, because that is what CI and every LMS autograder already ingests. |
| 58 | **cocotb** | No live-stepping API. `Simulator.runEventLoop` (`src/jls/sim/Simulator.java:215`) runs to completion; nothing external can drive or observe a mid-run simulation. `docs/vcd-interop.md:18–22` records the rejection (#63), and `examples/autograde/autograde.py` says so in its docstring. | Under the capability frame the survey's ADJACENT is **not** justified by the frame's three grounds — this is not a different tool class. It is justified by there being a strictly better JLS-native answer: properties belong *in the model* (**B**/**C**), not in an external Python testbench poking a VPI. **E**'s "many stimuli in one JVM" mode delivers the throughput cocotb users actually want; `riscv/fuzz_diff.py` currently pays a fresh JVM start per program (~0.35 s measured, `docs/standards-adoption/05-riscv-compliance.md`, "Runtime budget"). |
| 59 | **VPI / PLI** | JLS is not an HDL simulator. | Declined on grounds (different tool class). The capability — external observe/drive — is **E**'s embeddable API, natively. |
| 60 | **VHPI** | Same as #59. | Same. |
| 61 | **DPI-C** | Same as #59. | Same. |
| 62 | IEEE **1735** | IP encryption/rights management for commercial IP delivery; v2 has published cryptographic weaknesses. | Declined on grounds (different tool class), and it would be a bad thing to model for students. |
| 63 | **SMT-LIB 2.6** | No property to check (there is no assertion object), and no formula extractor. Everything else is *already present*: the two-state value domain (`docs/simulation-semantics.md` §2) means no X to model, `Circuit.getElementsInStableOrder` gives a content-determined element walk (issue #181), and `HdlExporter.buildModel` (`src/jls/hdl/HdlExporter.java:143`) already performs exactly the port/net walk a formula printer needs. | **B** (something to prove) + **G** (formula printer). This is a **reclassification from ADJACENT to a first-class target**: it is a printer over a walk that already exists, and it turns autograding from "matches 40 vectors" into "equivalent for all inputs." |
| 64 | **AIGER** / **BTOR2** | Same as #63. AIGER's alphabet is literally AND + NOT + latches — which is JLS's element vocabulary. | **G**. AIGER for the combinational subset is the smallest possible formal export; BTOR2 carries registers word-level. External model checkers (ABC, `btormc`, SymbiYosys) do the solving — the delegation stance already recorded in `ARCHITECTURE.md` and `docs/hdl-support-research.md` §6. |
| 65 | `riscv-arch-test` / **RISCOF** | Already COULD and fully costed at 8–12 maintainer-days in `docs/standards-adoption/05-riscv-compliance.md`. Two of its costs are **paper-overs of this sweep's gaps**: (a) the halt mechanism is built by decoding a magic store address into a `Stop` element (`src/jls/elem/Stop.java:157`, step 3 of that doc) — an assertion assembled out of comparator gates because there is no assertion element; (b) the signature is *reconstructed* from `Memory.printChangedValues` deltas plus a separately-known initial image (step 5), because batch mode can only report **changed** RAM words (`docs/batch-interface.md` §3.3). | **B** gives the halt/check a first-class element with a name and a severity. **H**'s report channel gives an absolute state-dump surface, removing the delta-reconstruction. Neither is a prerequisite for #65; both make it smaller and less fragile. |
| 73 | vendor **UCDB** | Proprietary coverage databases. | Superseded by #53 for interchange. |
| 259 | RVI **"RISC-V Compatible"** listing | Architecturally closed: the `riscv/` CPU has no CSRs, traps, privilege modes, or misaligned-access behavior, so no ratified profile is attainable (`docs/standards-adoption/05-riscv-compliance.md`). | Nothing in this sweep. Declined on grounds — already correctly declined, with a written reason. |

---

### The changes, and what each unlocks

#### A. Timestamp closure: a sampling model in the kernel

**Technically.** `Simulator.runEventLoop` (`src/jls/sim/Simulator.java:215`)
advances `now` per event and calls `afterEvent` per event. There is no
notion of "this timestamp is finished." Because `WireNet.propagate`
overwrites inputs eagerly and same-time notifications coalesce
(`docs/simulation-semantics.md` §3, §6.1), a signal can legitimately hold
several distinct values *within* one timestamp, and an observer that reads
after each event sees intermediate values that no downstream element ever
acted on.

The change: peek the queue head (`eventQueue` is a `PriorityQueue<SimEvent>`,
`Simulator.java:25`, so peek is O(1)); when its time is strictly greater
than `now`, or the queue drains, fire a new `afterTimestamp(long t)` hook
before advancing. That hook is the **sampling region** — JLS's analogue of
SVA's preponed region — and it is the single place every checker and every
coverage counter reads from.

Design constraint, and it must be tested as one: the hook is
**observation-only**. It posts no events and mutates no element state, so
it cannot change a single simulated value. The proof is that
`BatchSimulationGoldenTest`, `SequentialGoldenTest`, `VcdExportGoldenTest`,
`SimulationSeedOrderTest`, and `SimulationSemanticsRegressionTest` stay
byte-identical.

**Unlocks (standards).** #49 SVA and #50 PSL both *require* a defined
sampled value; without this they are unimplementable, not merely
expensive. #53 UCIS toggle coverage requires it too, or every glitch
counts as a toggle and the numbers are noise. It also gives #89 SDF's C4
(`TIMINGCHECK`, priced as dead in
`docs/standards-adoption/11-costed-rejections.md` because "a violation has
nowhere to go") a place to go that is **not** an X value: a named
assertion failure. That is a partial reopening of a costed rejection, on
new grounds.

**Unlocks (pedagogy).** "When exactly does a value settle?" is a question
JLS currently cannot answer, because it has no word for it. Timestamp
closure makes "settled" a named concept students can see in the stepper,
which is the conceptual prerequisite for teaching setup/hold at all.

**What JLS papers over today.** `BatchSimulator.toVcd`'s `fold` helper
(`src/jls/sim/BatchSimulator.java:489`) folds a signal's samples into a
`TreeMap<Long,BitSet>` where "the last sample recorded at a given time
wins." The emitted VCD is correct — but only *accidentally*, because a
map collapses the same-time duplicates that `afterEvent`
(`BatchSimulator.java:140`) recorded per-event. The exporter is already
doing timestamp closure by hand, in the wrong layer, after the fact.

**Size:** 2–3 maintainer-weeks. Most of it is not code; it is amending
`docs/simulation-semantics.md` §3/§4 (a normative document) and proving
non-disturbance against every golden.

---

#### B. A drawable `Assert` element and a failure channel

**Technically.** A new `LogicElement`, save tag `Assert`, palette group
`TEST` — which **already exists** in `src/jls/edit/Palette.java` and today
holds only `SigGen` and `Display`.

Ports: `check` (1 bit — the Boolean property, built from ordinary gates,
which costs nothing new), optional `enable`, optional `clock`.
Attributes: `name`, `severity` (note / warning / error / fatal — the same
four levels VHDL's `assert` statement uses), `mode` (level-sensitive, or
sampled on a clock edge), `message`.

Semantics: at timestamp closure (**A**), if enabled and `check` is 0,
append a failure record `(time, full name, severity, message, actual
values of referenced signals)`.

**The element archetype already exists.** `Stop`
(`src/jls/elem/Stop.java:157`) and `Pause` (`src/jls/elem/Pause.java:177`)
are precisely this shape: a 1-bit-in, zero-out element whose whole job is
a side effect on the simulator when a condition holds. `Stop.react` scans
its attached inputs and calls `sim.stop()` on any non-zero. An `Assert` is
`Stop` with a name, a severity, and a report instead of a halt. The
palette already describes them as *"stop simulator when asserted"* — the
word is in the tree; only the object is missing.

**Unlocks (standards).** The immediate-assertion subset of #49 and #50
(VHDL's `assert`, SV's `assert final`). It also makes the emitters carry
properties outward: `VhdlEmitter` can render an `Assert` as a real VHDL
`assert … report … severity …` and `VerilogEmitter` as an SVA
`assert property`, so a JLS drawing becomes checkable by *external*
simulators and by SymbiYosys — the "orchestrate external tools" stance
already recorded in `ARCHITECTURE.md` and `docs/hdl-support-research.md`
§6, applied to verification for the first time. #57's artifact shape
(xUnit XML) is the natural report format.

**Unlocks (pedagogy).** This is the largest teaching change in the sweep.
Today a design error surfaces as a wrong number in a `Display`, 200 time
units and three subcircuits downstream of its cause. With assertions it
surfaces **at the point and the moment the intent was violated**, marked
red on the drawing the student is looking at. Concretely newly teachable:

- "these two tri-state drivers must never both be enabled" — currently a
  one-shot `TellUser.warn` from `WireNet.propagate`
  (`src/jls/elem/WireNet.java:475–478`) with no name and no count;
- "this FSM must never be in `RUN` while `reset` is high";
- "the ALU must never assert `zero` and `negative` together";
- "this handshake must never see `ack` without `req`."

It also teaches, correctly and early, that a *specification* is a thing
you write down, separate from the implementation — which is the single
most transferable idea in the whole verification field.

**What JLS papers over today.** Four distinct places:

1. `examples/autograde/autograde.py` hard-codes
   `EXPECTED_STDOUT_LINES = ["Output Pin ar: 0xED (237 unsigned, -19 signed)", …]`
   — the grading criterion is *literal bytes of a report format*. That is
   an assertion, expressed outside the circuit, against a text encoding.
2. `riscv/verify.py:compare` builds a `problems` list by diffing register
   and memory dicts. That is a scoreboard, in Python, because the model
   has no place to put one.
3. `docs/standards-adoption/05-riscv-compliance.md` step 3 constructs a
   halt condition from a magic-address comparator feeding `Stop`. That is
   an assertion built out of gates because there is no assertion element.
4. `TellUser.warn` one-shot reports: the bus conflict
   (`WireNet.java:407,475`) and the unmatched FSM transition
   (`StateMachine.java:664,772`, `noMatchReported`). Both are assertions
   with no name, no severity, no count, and no machine-readable channel —
   and both re-arm or fire exactly once per run, so a circuit that
   conflicts 10,000 times reports one line. Related, and worse:
   `TruthTable.react` (`src/jls/elem/TruthTable.java:1430–1434`) on an
   unmatched row **silently holds its outputs** and warns nobody at all.

**Size:** 2–3 maintainer-weeks. The element itself is small; the cost is
the surface it must join — `ElementRegistry`, `SaveTags`, `Palette`, a
renderer, a dialog, `docs/file-format.md` §7, and the totality tests that
turn each omission into a build failure (which is the good kind of cost).

---

#### C. A minimal temporal layer

**Technically.** Extend `Assert` with a bounded temporal property
evaluated on the sampling clock from **A**. The minimum useful subset,
chosen by what first-year designs actually need rather than by what the
standards contain:

1. `stable(x)` — x did not change at this sampling edge. (This is the
   brief's "signal A must be stable when clock rises," and it is the
   single most requested property in a first sequential-logic course.)
2. `rose(x)` / `fell(x)`.
3. `a |-> b` — overlapping implication (same edge).
4. `a |=> b` and `a ##N b` — b on the next edge / exactly N edges later.
5. `a |-> ##[1:N] b` — the brief's "B must follow A within N cycles."
6. `always p` / `never p` over the run; `$past(x, n)` for n ≤ N.

Implementation: a per-property bounded NFA plus a ring buffer of the last
N sampled values per referenced signal. No solver, no unbounded state,
memory linear in (properties × N). Deliberately **out** of the minimum:
unbounded liveness (`s_eventually` with no bound), SERE repetition
operators, local variables, `disable iff` interactions.

**What a property looks like in a schematic tool** — both forms, because
the tool has two kinds of user:

- **(a) Drawn.** The Boolean part is built from ordinary gates into the
  `check` port (free today); the temporal part is a *dialog choice* on the
  element ("must be stable at clock edge", "must follow within [3] cycles
  of `trigger`"). No language for the student to learn. This is the form
  first-years use.
- **(b) Written.** A `String property` attribute holding a one-line
  property in a small documented grammar — specified with the same rigor
  as the `-t` grammar in `docs/batch-interface.md` §2, and frozen the same
  way. This is the form instructors use, and it is what makes properties
  diffable and distributable in an assignment repository.

Prior art, and it points at (b): Digital (hneemann) puts a **text test
program inside a drawable element** — its test-case component holds a
table plus a small DSL. Logisim-evolution takes the other route entirely,
a CSV test-vector file with a CLI (`--test-vector`,
`docs/hdl-support-research.md` §7.1) — which is what JLS already has in
`-t`, and which is exactly the surface this change is meant to move
*beyond*. *(Digital's DSL details — `let`, `loop`, `bits`, `repeat`,
don't-care `x`, a `C` clock-pulse column — are recalled, not verified in
this pass; read the source before copying.)*

**Unlocks (standards).** The genuinely-used core of #49 SVA and #50 PSL —
and, via emission, checking of JLS-drawn circuits by external formal
tools. PSL's *simple subset* is the closer match and the better spec to
write against, because it is language-neutral and JLS emits two languages.

**Unlocks (pedagogy).** Sequential-logic intent becomes expressible.
"After `start`, `done` must arrive within 8 cycles" is the actual
specification of most first-year FSM assignments, and today it is stated
in English in the handout and checked by a human reading a waveform.

**What JLS papers over today.** `riscv/verify.py:gen_clock` generates a
clock waveform with *exactly `steps` rising edges*, where `steps` is
predicted in advance by running the reference emulator
(`run_reference` → `cpu.run(max_steps=10000)`). The harness must know how
long the hardware will take before it runs it, because there is no way to
say "run until `done`." `docs/standards-adoption/05-riscv-compliance.md`
step 4 calls this out as "unusable when the cycle count is unknown a
priori, and slow" (38.2 s vs 4.1 s free-running) and works around it with
a free-running `Clock` plus the `Stop` hack. A bounded temporal property
("`halt` within N cycles") is the direct expression of what that
scaffolding approximates.

**Size:** 3–4 maintainer-weeks: grammar, NFA evaluator, dialog, two
emitters, a normative grammar section, tests.

---

#### D. A coverage model: what coverage *means* in a schematic

**Technically.** A `CoverageCollector` in `jls.sim`, fed from the
timestamp-closure hook (**A**) and from three instrumentation points.
Four measures, each grounded in a structure that already exists:

1. **Toggle coverage on nets.** Per `WireNet`, per bit: was 0 seen, was 1
   seen, and (on a tri-state net) was HiZ seen. The site already exists —
   `WireNet.propagate` (`src/jls/elem/WireNet.java:443`) already calls
   `sim.probeSample(...)` at `:525` for probed nets. Nets already carry
   `getBits()`. Cost: two long masks per net.
2. **State *and transition* coverage on `StateMachine`.** Which `State`
   objects were entered, and which transitions fired.
   `State.getNextState()` (`src/jls/elem/State.java:1272`) already walks
   the transition list and returns the match; instrumenting it is one
   increment. Transition coverage strictly dominates state coverage and
   costs the same, so measure transitions. Note the survey's own
   observation (`docs/standards-landscape.md` §3) that state diagrams and
   ASM charts are a **deliberately un-standardized space** — that is
   precisely why JLS gets to define the pedagogically right measure here
   rather than inheriting one.
3. **Row coverage on `TruthTable`.** The element already holds
   `int[][] table` with `rows`/`cols`
   (`src/jls/elem/TruthTable.java:80,94,96`), and `react` already computes
   `matchingRow` at `:1408–1428`. "Which rows were selected" is one
   `boolean[rows]`. This is the schematic analogue of branch coverage, and
   it is the measure that tells an instructor *"your vectors never
   exercised the carry case."* It also surfaces the silent-hold hole at
   `:1430–1434` — a table with no matching row for some input holds its
   outputs and tells nobody — as an uncovered "no row matched" bin.
4. **Select coverage on `Mux`/`Decoder`**, and **value-bin coverage** on
   `Register`/`Memory` (declared bins over ranges), as cheap follow-ons.

Plus two JLS-specific measures worth having because they map to real
model warnings: **HiZ coverage** (how often a net was undriven) and
**conflict counts** (how often multi-driver resolution actually fired) —
turning today's one-shot warnings into numbers.

**Export.** UCIS (#53) is a plausible target and the only real
conformance opportunity in this tier. Its data model — scopes containing
coveritems containing bins with counts — maps onto JLS's four measures
with no violence: a circuit is a design-unit scope, a `SubCircuit` an
instance scope, and each net-bit / state / transition / table row is a
coveritem. Recommended shape, following the house pattern already used
for VCD (#66) and PCF (#81): a **JLS-native report as the primary
artifact**, with a UCIS export as a printer over it and a
`docs/ucis-profile.md` naming the subset and every deviation.
*(UCIS 1.0 defines a data model with both a C API and an XML interchange
form; the exact schema was **not verified in this pass** — read the
Accellera document before writing an emitter, and do not assume the XML
form is the interoperable one.)*

**Unlocks (standards).** #53 directly; #73 conceptually.

**Unlocks (pedagogy).** This is the one that changes what an instructor
can *ask for*. Today a student can pass a lab with a circuit whose entire
carry path is dead, because the vectors never hit it — and neither the
student nor the instructor can tell. Coverage makes that visible and
gradeable:

- "your testbench must exercise every row of your truth table";
- "your stimulus must drive the FSM through every transition, not just
  every state" (a distinction students consistently get wrong, and which
  is nearly impossible to teach without a tool that measures both);
- "every bit of this bus must toggle";
- and the meta-lesson, which is the real prize: **passing tests is not the
  same as being tested.**

**What JLS papers over today.** Nothing measures this, so the paper-over
is in the *human* process — write more vectors and hope. It shows up in
the tree as hand-tuned heuristics standing in for measurement:
`riscv/fuzz_diff.py:rand_program` weights instruction kinds
`weights=[30, 30, 12, 8, 4, 8, 8]` by judgement, and its comment
"No control flow (covered by the directed suite)" is a **coverage
argument made in a code comment**, unmeasured and unchecked.

**Size:** 2–3 maintainer-weeks for the collector plus measures 1–3 and a
native report; +1 week for the UCIS export and its profile document.

---

#### E. Reactive and constrained-random stimulus

**Technically.** Today every stimulus is a schedule fixed before time 0:
`SigSim.initSim` (`src/jls/elem/SigSim.java`) parses the entire `-t` file
and posts every `SimEvent` up front, and `docs/batch-interface.md` §2.3
specifies exactly that. Nothing can choose a value based on what the
circuit did. Four pieces:

1. A `Stimulus` SPI in `jls.sim` that may post events *during* the run,
   invoked from the timestamp-closure hook.
2. A constrained-random generator: per input pin, a declared domain
   (range / set / weights), seeded. Deterministic as a function of (seed,
   circuit content) — the same discipline `Simulator.initSimulation`
   already enforces via stable-id seed order (issue #181) and the
   byte-deterministic VCD.
3. Coverage-driven feedback: choose the next stimulus to hit an uncovered
   bin. This is why **D** and **E** are worth more together than apart.
4. A **multi-run batch mode**: N stimuli in one JVM. `riscv/fuzz_diff.py`
   currently spends a JVM start per program
   (`ThreadPoolExecutor` over independent `java` subprocesses;
   ~0.35 s each, measured in `docs/standards-adoption/05-riscv-compliance.md`).

Grammar note: `-t` is frozen (`docs/batch-interface.md` §6). The generator
gets a **new flag and a new file**, and the `-t` grammar stays
byte-compatible forever. Purely additive.

**Unlocks (standards).** The capability behind #52 PSS, #51 `e`, and the
randomization halves of #55/#56 — without adopting any of their syntax.
Makes #65 RISCOF materially cheaper: that plugin's whole job is "run many
compiled programs and diff signatures," and the multi-run mode is exactly
that harness.

**Unlocks (pedagogy).** "Generate 10,000 random operand pairs and check my
adder against the built-in `Adder`" becomes something a **student** can do
from the GUI. Today it requires writing Python against a subprocess CLI —
which is why only the maintainer does it.

**What JLS papers over today.** The whole of `riscv/fuzz_diff.py` is this
feature, implemented outside the tool, for one circuit, by the maintainer.

**Size:** 2–3 maintainer-weeks; +1 for the multi-run batch mode.

---

#### F. In-tool differential/equivalence checking

**Technically.** A batch mode (and a GUI action) that drives two
circuits — or a circuit and a declarative model of it — from the same
stimulus and asserts equality at sampling points. JLS already has every
structural piece: `SubCircuit` composes circuits; `TruthTable` *is* a
declarative specification of a combinational function; a ROM `Memory` *is*
a declarative lookup specification. The change is a driver plus an
`Assert`, not new model concepts.

**Unlocks.** The concept behind `riscv/verify.py` and `riscv/fuzz_diff.py`,
moved into the tool and available for every circuit rather than one.
Combined with **G**, it becomes exhaustive rather than sampled.

**Unlocks (pedagogy).** The canonical first-year assignment — "build a
4-bit adder out of gates" — becomes gradeable as *"is it equivalent to the
reference for all 2⁹ input combinations"*, exhaustively, instead of *"does
it match on the 20 vectors I wrote."* That is a categorical change in what
a grade means.

**Size:** 1–2 maintainer-weeks on top of **B** and **E**.

---

#### G. Formula export: SMT-LIB, AIGER, BTOR2 (#63, #64)

**Technically.** A printer over the model `HdlExporter.buildModel`
(`src/jls/hdl/HdlExporter.java:143`) already constructs for the Verilog
and VHDL emitters. For a combinational circuit each element becomes a
bit-vector constraint and each `Assert` becomes a negated proof
obligation; for sequential circuits, either bounded unrolling to depth k
(SMT-LIB) or BTOR2's native state elements. Solving is delegated
entirely — ABC, `btormc`, SymbiYosys — matching the recorded stance in
`ARCHITECTURE.md` and `docs/hdl-support-research.md` §6, and the
skip-when-absent CI pattern already used by
`test/jls/hdl/IverilogCompileTest.java` and `ToolLocator`.

The two-state value domain (`docs/simulation-semantics.md` §2), which is a
*liability* for SDF (#89) and for Yosys import of x/z designs
(`docs/hdl-support-research.md` §7.2), is here an **asset**: there is no X
to encode, so a JLS circuit is a clean Boolean/bit-vector formula.

**The piece worth naming separately: the counterexample loop.** A solver
returns a falsifying assignment. Render it back as a `-t` **test-vector
file** — an interface that already exists and is already a frozen
contract — and the student replays the counterexample in the GUI, steps
it, and watches the property fail. That closes the loop from "your circuit
is wrong" to "here is the input that breaks it, loaded and running,"
through a surface JLS already promises to keep stable.

**Unlocks (standards).** #63 SMT-LIB and #64 AIGER/BTOR2 move from
ADJACENT to shipped exports. Via emitted SV assertions (**B**/**C**),
the whole SymbiYosys ecosystem becomes reachable without JLS containing a
solver.

**Unlocks (pedagogy).** "Prove your decoder is one-hot for every input."
"Prove this FSM cannot deadlock." "Prove your circuit is equivalent to the
reference." For first-years, bounded proofs with a drawn counterexample
are strictly more instructive than a pass/fail vector diff, because the
failure is *constructed for them* rather than stumbled into.

**Size:** 3–4 maintainer-weeks for combinational SMT-LIB/AIGER plus the
counterexample→`-t` writer; +2–3 for BTOR2 and bounded sequential
unrolling.

---

#### H. The report channel and the exit-status contract

*Separated from **B** because it is a change to a **stability promise**,
and must be designed before anything above ships.*

**Technically.** `docs/batch-interface.md` §1 defines exactly three exit
statuses (0 run completed, 1 runtime failure, 2 usage error) and §3
defines an outcome line with exactly four frozen reasons in precedence
order. There is no way to say "the run completed and a property failed."
`examples/autograde/autograde.py` mirrors the same 0/1/2 on itself.

The additive route §6 already blesses: a new flag (`-check <file>` and/or
`-cov <file>`) whose presence gates every new observable. A run **without**
the new flag returns 0/1/2 and prints the same bytes as today, so no
conforming consumer can observe a change; a run **with** it may return a
new status (3 = property failed) and writes a report file.

Report format: **xUnit XML** as the primary — it is what CI and every LMS
autograder already ingests, and it is the artifact shape #57 VUnit made
standard practice — plus a plain line format for humans and diffs.
Both byte-deterministic and golden-pinned, following
`test/jls/VcdExportGoldenTest.java`.

**Unlocks.** Every change above needs somewhere to put its result. It also
retires the reconstruct-memory-from-deltas workaround in
`docs/standards-adoption/05-riscv-compliance.md` step 5.

**Size:** 1 maintainer-week including the `docs/batch-interface.md`
amendment, CHANGELOG entry, `CliFlagTableTest`/`CliSmokeTest` rows, and
goldens. Small, but it is a **promise**, so it must be got right first.

---

**Total honest size:** 16–24 maintainer-weeks for the full program;
**5–7 weeks for the useful floor** (A + B + H + toggle/row/transition
coverage), which alone converts autograding from output-matching to
property-and-coverage grading.

---

### Ripple effects

**Normative documents.**

- `docs/simulation-semantics.md` — **A** amends §3 (events and ordering)
  and §4 (the event loop) with the timestamp-closure hook; §11
  ("simulation-control elements", today `Stop` and `Pause`) gains `Assert`
  and `Cover`; §12's spec↔golden mapping table gains rows. This is the
  document the whole sweep is stress-testing, and it must be edited
  *before* the code.
- `docs/batch-interface.md` — §1's flag table (driven by
  `JLSStart.FLAGS`, `src/jls/JLSStart.java:759`, 14 entries today) gains
  rows; a new §7 specifies the assertion/coverage report; §6's stability
  promise is extended to cover it. **§2's `-t` grammar and §3's stdout
  format must not move a byte** — that non-movement is the compatibility
  proof, and `BatchSimulationGoldenTest.watchedElementsPrintInNameOrder`
  and `VcdExportGoldenTest` staying green unchanged is how it is
  demonstrated.
- `docs/file-format.md` — §7's tag table (32 written tags today, 33
  registered including the load-only `TestGen`) gains `Assert` and
  `Cover`. §9's evolution policy is favorable: adding a new **element
  type** needs **no version bump**, because an older reader fails loudly
  with "no element type named X" — detectable, not a misparse. Adding
  **attributes to existing elements** is the dangerous class (the
  silent-drop caveat, with `Memory`'s `initrle` and `sync` as the standing
  examples), so this program should prefer new element types over new
  attributes wherever the choice exists.
- `ARCHITECTURE.md` — a new recorded decision for the verification
  strategy. Note that the existing decision "Simulation execution
  strategy: discrete-event interpreter is the sole strategy" (#221,
  `ARCHITECTURE.md:341`) carries an **equivalence criterion binding on any
  future compiled pass**; sampling semantics joins the list of things such
  a pass would have to reproduce, which makes the criterion stricter.
- `docs/extension-points.md` — new seams (`sim.checker`,
  `sim.coverage-collector`, `app.report-writer`). `ExtensionPointCatalogTest`
  checks the catalog **in both directions**, so a row without a constant
  or a constant without a row is a build failure.
- `docs/standards-landscape.md` — the §4 position paragraph
  (`:222–229`) is rewritten; relevance marks change on #49, #50, #53, #63,
  #64; §13.1's ranked list gains entries; §14's by-relevance tally shifts.
- `docs/standards-adoption/` — new numbered documents (assertions,
  coverage, formal export), and `05-riscv-compliance.md` gets a note that
  its `Stop`-as-halt and signature-reconstruction workarounds are
  superseded. `docs/vcd-interop.md:18–22` ("not offered: live
  co-simulation") needs one sentence saying in-model assertions are the
  answer instead of a VPI.

**File format.** Two new element tags; no version bump (§9). New
attributes are ordinary `String`/`int` items — no new item kind, so no
bump on that ground either.

**Element `react()` methods.** 25 files under `src/jls/elem/` define
`public void react`: `Adder`, `Binder`, `Clock`, `Constant`, `Decoder`,
`Display`, `Extend`, `Gate`, `InputPin`, `JumpEnd`, `JumpStart`,
`LogicElement`, `Memory`, `Mux`, `OutputPin`, `Pause`, `Register`,
`ShiftRegister`, `SigSim`, `Splitter`, `StateMachine`, `Stop`,
`SubCircuit`, `TriState`, `TruthTable`. **This program modifies three of
them** — `TruthTable.react` (record `matchingRow`, `:1408`),
`State.getNextState` (record the firing transition, `:1272`), and
`WireNet.propagate` (record toggles, `:443`) — plus the new elements' own.
Everything else hooks into `Simulator`'s existing seams. Contrast with the
X/4-state change priced in
`docs/standards-adoption/11-costed-rejections.md`, which touches all 25.
The verification tier was declined as though it cost what the value-domain
change costs. It does not.

**Simulation hot loop.** `runEventLoop` gains one queue-head peek and one
comparison per event — O(1), on a `PriorityQueue`. Collection must be
gated exactly as tracing is today: `BatchSimulator.afterEvent:144` opens
with `if (!JLSInfo.printTrace && vcdFileName == null) return;`, and
`probeSample:298` repeats it. A run with no checkers and no coverage must
pay one predictable branch and nothing else. `grand-architecture.md` §6's
hot-plane rule (the inner loop holds zero plugin indirection) means the
collector is a **concrete core type**; the extension point registers it,
it does not dispatch inside the loop.

**GUI.** A palette entry in the existing `Group.TEST`
(`src/jls/edit/Palette.java`); renderers in the `BuiltinElementRenderers`
table; creation dialogs via `ElementDialogs`/`ElementFormDialog`; in the
interactive simulator, a red-mark annotation at the failing element and a
checks/coverage panel beside the existing `Trace`/`MemTrace` windows
(`src/jls/edit/Trace.java`, `MemTrace.java`). The GUI half is the larger
share of **B**'s cost, not the model half.

**Existing saved circuits.** Unaffected. No existing file contains the new
tags; coverage and checking are off unless requested; the seed order,
event ordering, and delay model are untouched.
`test/fixtures/fork-4.6-shiftregister.jls`, `riscv-sum1to10.jls`,
`headless-canary-gate.jls`, and `test/fixtures/legacy-4.1/` all load and
simulate byte-identically.

**Existing tests that will demand work.**

- *Totality gates that turn omissions into build failures (the good kind):*
  `ElementRegistryTest`, `SaveTagsTest`, `FileFormatSpecTest` (holds the
  tag table, `SaveTags`, and the writers' real output in lock-step),
  `PaletteContractTest`, `ExtensionPointCatalogTest`,
  `AllElementsRoundTripTest`, `GenerativeRoundTripFuzzTest`,
  `ElementDrawSmokeTest`, `DialogConstructionSmokeTest`,
  `DialogCoverageRatchetTest`, `ElementConstructorContractTest`,
  `CapabilityInterfaceTest`.
- *Contract pins that need new rows:* `CliFlagTableTest`, `CliSmokeTest`.
- *Goldens that must stay byte-identical, as the non-disturbance proof:*
  `BatchSimulationGoldenTest`, `SequentialGoldenTest`,
  `VcdExportGoldenTest`, `VcdProbeExportTest`, `SimulationSeedOrderTest`,
  `SimulationSemanticsRegressionTest`, `ShiftRegisterTest`,
  `ElementSimulationGoldenTest`, `RiscvCpuGoldenTest`,
  `DeterministicSaveTest`.
- *Boundaries:* `HeadlessCoreRatchetTest` / `HeadlessCoreCanaryTest` — all
  checker and coverage code lives in `jls.sim` and must import no AWT.
  `ArchitectureRulesTest`, `NullMarkedRatchetTest`,
  `PackageInfoRatchetTest`, `SealedHierarchyTest` — note `SigSim` is
  `sealed … permits SigGen, TestGen`, so a reactive generator (**E**)
  either joins that `permits` clause or takes a different base.
- *Should grow a property-based section:* `AutogradeBridgeExampleTest`
  and `examples/autograde/autograde.py`.
- *Naming hazard:* `test/jls/CoverageAgent.java` already exists and is
  **Java code coverage** for the build. Circuit coverage needs a
  distinguishable name in code and docs from day one.

---

### What genuinely stays out, and why

Only items failing the frame's three legitimate grounds.

- **#48 UVM (as a class library), #51 IEEE 1647 `e`, #52 PSS (as a
  language).** Testbench *programming languages* for a tool class JLS is
  not — constrained-random SoC verification environments with class-based
  factories and solver-backed constraint languages. Their capability
  (generator / driver / scoreboard / coverage; retargetable test intent)
  is delivered natively by **D** + **E**; adopting their syntax with a 5 %
  implementation would be conformance theater and would teach students a
  dialect they could not use anywhere else.
- **#54 SCE-MI.** Transaction transport between a host and a hardware
  emulator box. Different tool class; no schematic-first meaning.
- **#59 VPI/PLI, #60 VHPI, #61 DPI-C.** C APIs *into HDL simulators*. JLS
  does not execute HDL and does not intend to (`docs/vcd-interop.md:18–22`,
  `docs/hdl-support-research.md` §6). Different tool class. The capability
  they standardize — external programs observing and driving a run — is
  **E**'s embeddable API, natively and without a C boundary.
- **#62 IEEE 1735.** IP encryption and rights management for commercial IP
  delivery. Different tool class, and v2's published cryptographic
  weaknesses make it something a teaching tool should not model.
- **#55 OSVVM / #56 UVVM / #57 VUnit (as libraries and frameworks).** VHDL
  packages and runners for tools that execute VHDL. Different tool class.
  Their *design ideas* are taken (OSVVM's bin/holes model informs **D**;
  VUnit's xUnit artifact shape is adopted in **H**) — the standards are
  not.
- **#73 vendor UCDBs.** Proprietary, per-vendor, and superseded by #53 for
  interchange.
- **#259 RVI "RISC-V Compatible" trademark listing.** Architecturally
  closed, not merely expensive: the `riscv/` CPU has no CSRs, traps,
  privilege modes, or misaligned-access behavior, so no ratified profile
  is attainable, so the trademark permission is unattainable. Already
  costed and declined with a written reason in
  `docs/standards-adoption/05-riscv-compliance.md`.

**Explicitly NOT staying out, contra the survey:** #49 SVA, #50 PSL, #53
UCIS, #58 cocotb's capability, #63 SMT-LIB, and #64 AIGER/BTOR2. Each was
marked OTHER or ADJACENT on grounds of model change or scope fit, and none
of them fails the three legitimate tests.

---

### Sources

**Repo paths (all read in this pass).**

- Normative: `docs/simulation-semantics.md` (§1–§12 and the appendix;
  §2 value domain, §3 event ordering, §6.1 read-latest, §6.2 transport
  delay, §9 tri-state resolution, §11 Stop/Pause);
  `docs/batch-interface.md` (§1 exit/stream contract, §2 `-t` grammar,
  §3 stdout format, §4 VCD profile, §6 stability promise);
  `docs/file-format.md` (§7 tag table, §9 evolution policy).
- Architecture: `ARCHITECTURE.md:341` (simulation execution strategy,
  #221, and its equivalence criterion); `docs/grand-architecture.md`
  §§1–4, §6; `docs/extension-points.md` (the seam catalog and its
  bidirectional test).
- Survey: `docs/standards-landscape.md:181–183, 200–229, 238–245,
  618, 708–800`.
- Adoption docs: `docs/standards-adoption/05-riscv-compliance.md` (the
  `Stop`-as-halt construction, the signature-reconstruction workaround,
  the 38.2 s / 4.1 s clock measurements, the 8–12 day sizing, and the
  Sail-oracle recommendation); `docs/standards-adoption/11-costed-rejections.md`
  (the SDF C1–C4 analysis, and the X-domain cost against all 25 `react`s).
- HDL: `docs/hdl-support-research.md` §7.1 (Logisim-evolution's
  `--test-vector` CLI as the catch-up bar), §7.2 (x/z as a
  loudly-rejectable gap), §6 (delegation stance);
  `docs/vcd-interop.md:18–22, 113–114` (co-simulation rejection, #63).
- Kernel: `src/jls/sim/Simulator.java:25, 215, 252, 269, 285`;
  `src/jls/sim/BatchSimulator.java:140, 144, 250, 295, 298, 384, 489`;
  `src/jls/sim/TraceSample.java`; `src/jls/sim/SimEvent.java`.
- Elements: `src/jls/elem/SigSim.java` (the `-t` parser and its
  post-everything-at-init behavior); `src/jls/elem/TestGen.java`;
  `src/jls/elem/Stop.java:157`; `src/jls/elem/Pause.java:177`;
  `src/jls/elem/StateMachine.java:664, 686, 722–782`;
  `src/jls/elem/State.java:1272, 1315`;
  `src/jls/elem/TruthTable.java:80, 94, 96, 1400–1434`;
  `src/jls/elem/WireNet.java:406–478, 443, 520–525`;
  `src/jls/elem/ElementRegistry.java` (33 registered types);
  `src/jls/elem/Watchable.java`.
- CLI/GUI: `src/jls/JLSStart.java:672–695` (`displayResults`), `:759`
  (`FLAGS`, 14 entries); `src/jls/edit/Palette.java:140–183` (the `TEST`
  group); `src/jls/TellUser.java`; `src/jls/edit/InteractiveSimulator.java`;
  `src/jls/edit/Trace.java`, `MemTrace.java`.
- Emitters: `src/jls/hdl/VhdlEmitter.java:465–470` (the `when others`
  standing in for std_logic's nine values — the sweep's canonical
  paper-over); `src/jls/hdl/HdlExporter.java:143`.
- Verification-as-it-exists: `examples/autograde/autograde.py`
  (`EXPECTED_STDOUT_LINES`, `EXPECTED_FINALS`, `parse_vcd_final_values`);
  `riscv/verify.py` (`gen_clock`, `run_reference`, `compare`);
  `riscv/fuzz_diff.py` (`rand_program` weights, `check_one`, the
  subprocess-per-program thread pool).
- Tests named above, all present under `test/`: `BatchSimulationGoldenTest`,
  `SequentialGoldenTest`, `VcdExportGoldenTest`, `VcdProbeExportTest`,
  `SimulationSemanticsRegressionTest`, `SimulationSeedOrderTest`,
  `ElementSimulationGoldenTest`, `ShiftRegisterTest`, `RiscvCpuGoldenTest`,
  `ElementRegistryTest`, `FileFormatSpecTest`, `AllElementsRoundTripTest`,
  `GenerativeRoundTripFuzzTest`, `CliFlagTableTest`, `CliSmokeTest`,
  `HeadlessCoreRatchetTest`, `HeadlessCoreCanaryTest`,
  `ExtensionPointCatalogTest`, `AutogradeBridgeExampleTest`,
  `test/jls/elem/SealedHierarchyTest.java`,
  `test/jls/ui/PaletteContractTest.java` (under `test/jls/edit/`),
  `test/jls/CoverageAgent.java`, `test/jls/hdl/ToolLocator.java`,
  `test/jls/hdl/IverilogCompileTest.java`.

**External documents (not fetched in this pass — treat as recalled and
verify before implementing).**

- IEEE 1800 (SVA): sampled-value functions `$stable`/`$rose`/`$fell`,
  `$past`, overlapping/non-overlapping implication `|->`/`|=>`, cycle
  delay `##N` and range `##[m:n]`, and the preponed sampling region.
  **The exact sampling-region semantics are the load-bearing detail and
  must be read from the standard before Change A is specified.**
- IEEE 1850 (PSL): the *simple subset* — `always`, `never`, `next`,
  `until`, `before`, `->` — is the recommended spec target for Change C
  because it is language-neutral.
- Accellera **UCIS 1.0**: data model of scopes → coveritems → bins, with a
  C API and an XML interchange form. **Which form is actually
  interoperable, and the schema itself, are unverified here.**
- Accellera **PSS**, **SCE-MI**; IEEE **1647**; IEEE **1735** — classified
  from the survey's descriptions plus general knowledge; not re-read.
- **Digital** (hneemann): its test-case element embeds a text test program
  in a drawable element. The DSL details (`let`, `loop`, `bits`, `repeat`,
  don't-care `x`, clock-pulse column) are **recalled, not verified** —
  read `github.com/hneemann/Digital` before copying any of it.
- **Logisim-evolution** test vectors and `--test-vector` CLI: verified
  second-hand via `docs/hdl-support-research.md` §7.1, which cites
  `docs/test_vector.md` in that repo.
- **OSVVM `CoveragePkg`** (bin/cross/holes model) and **VUnit** (xUnit XML
  output): recalled, unverified; both are design references, not adoption
  targets.
- SMT-LIB 2.6, AIGER, BTOR2 formats and the ABC / `btormc` / SymbiYosys
  back ends: recalled; formats are small and stable but should be read
  before an emitter is written.
