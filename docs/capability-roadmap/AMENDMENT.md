# The leapfrog amendment

*An amendment to `docs/capability-roadmap/README.md`, not a replacement. Written
from the eight leapfrog studies (`lf-01`…`lf-08`) in this directory. Every claim
about JLS is anchored to a path in the tree at HEAD; claims about external tools
inherit the studies' verification status and are marked where the studies marked
them. The six capability programs, the keystone, the spine and the §6 exclusions
of the parent document stand except where this document explicitly amends them.*

---

## What the standards-driven sweep could not see

The capability roadmap ranked 304 standards by capability gained. That is a
better question than the survey's original one, and it produced six real
programs. But the enumeration method was *walk the standards and ask what each
one blocks* — so a capability with no standard pointing at it could not appear,
however large. Eight such areas were swept. Here is what the method did to each.

**Three areas are completely invisible to it — no survey entry exists at all.**

- **Semantic diff, merge and version control (lf-06).** `grep -in "version
  control\|revision control\|design data management" docs/standards-landscape.md`
  returns **nothing** — re-verified in this pass. The nearest neighbour is #4
  IP-XACT's VLNV tuple, which is a reuse-identity concept and belongs to P3. Not
  one of 304 rows points at the fact that inserting a single gate into
  `riscv/build/addi.jls` produces a **5,314-line textual diff of which 5,227
  lines are pure renumbering churn** (measured in lf-06 with the shipped jar),
  or that `.gitattributes` in this repo contains two CRLF guards and no `diff=`,
  `merge=` or `filter=`.
- **Fault simulation and DFT (lf-05a).** No standard. `grep -rni
  "stuck.at\|fault\|ATPG\|testabilit\|test.pattern" src/` returns **zero**
  simulation-domain hits. The finding that indicts the method most directly is
  in the record: `docs/standards-adoption/11-costed-rejections.md:813` costed
  IEEE 1149.1 boundary scan at 15–25 maintainer-days — *the only DFT item anyone
  has ever costed for JLS is the one with a standard attached.* Fault
  simulation, which is what boundary scan exists to deliver patterns for and
  which is the part actually taught in a first course, was never costed because
  no row points at it.
- **Compiled / levelized evaluation (lf-02).** An execution strategy is not a
  standard. It surfaced only as one standalone row ("Full cycle-based simulation
  strategy, 10–16 wk", `README.md:700`) and one half-line in the spine
  (`README.md:844-845`), against a measured profile in which **4.9% of the event
  loop is digital logic** and **82.3% of all events model no elapsed time at
  all** (keystone C §6.3).

**Two areas the method saw the wrong half of.**

- **The programmatic API (lf-07)** produced the sharpest methodological finding
  in the sweep. The standards frame *could* see #58 cocotb, #59 VPI/PLI, #60
  VHPI and #61 DPI-C — and retired all four against *"**E**'s embeddable API,
  natively and without a C boundary"* (`sweep-04-verification.md:647-651`).
  **Change E is a stimulus generator. There is no embeddable API and no program
  that would build one.** Four survey entries are currently discharged against a
  capability nobody scoped. The method could name the interface standards and
  then declined them by promising something outside its own frame.
- **Parameterization (lf-01).** #31 `parameter`, #25 `generic`, #4 IP-XACT
  `modelParameters` all touch it — as *interchange attributes*. Nothing in 304
  rows says "a drawing should be resizable," so the sweep saw the export side of
  parameterization and never the authoring side. The consequence is measurable:
  `riscv/build/addi.jls` contains **zero `SubCircuit` elements in 228 logic
  elements**, and the CPU is produced by `riscv/jlsbuild.py` — a **second,
  independently maintained implementation of the normative save format, in
  Python**, kept in sync by `riscv/test_primitives.py`.

**Two areas the method half-saw and then routed around.**

- **Clocks and CDC (lf-08).** #82/#93/#94 SDC/XDC and #89 SDF appear, but as
  constraint *files*; no entry specifies a clock-domain model or a crossing
  check. The roadmap tripped over the hole twice without naming it: P5's ERC
  list contains *"clock nets on data pins"* (`README.md:545`) with no clock-net
  concept to check against, and P3's port metadata observes that *"JLS cannot
  say a wire **is** a clock"* (`README.md:326-327`) without assigning anyone the
  job of fixing it.
- **Causal debugging (lf-03).** The nearest entries are the waveform tier as an
  *artifact* — and VCD and FST declare scopes, variables and value changes and
  carry **no connectivity whatsoever**, so no waveform standard can express
  causality. The one format that stores enough is #70 FSDB, a proprietary binary
  correctly declined at `README.md:1116-1118`. The roadmap itself names the
  missing skill — *"they see `x` spread through the cone and they learn to trace
  it back — the single most transferable debugging skill in RTL"*
  (`sweep-01:117-119`) — without noticing that **P1 creates that question and
  nothing in P1–P6 answers it.**

**One area the method saw correctly and mis-sized.** Formal equivalence (lf-04)
is the control case: #63 SMT-LIB, #64 AIGER/BTOR2, #49/#50 SVA/PSL were all in
the survey, correctly moved up, and correctly assigned to P5. What the standards
frame could not price is what happens *around* a printer. P5 prices changes F
and G at 6–9 weeks; that is the cost of the printer, and the printer really is
that cheap. It does not price the solver decision, the miter, port matching,
care sets, the uncheckability gate, the counterexample rendering, the GUI, or
the exit-status contract. **The capability is roughly three times the printer:
20–30 weeks.**

**How much was missed.** The eight areas add **130–190 maintainer-weeks of new
program work**, plus **14–22 weeks of under-pricing inside P5**. Against the
parent document's 151–220 weeks, that means the standards-driven sweep found
roughly **half** of the available capability — and the half it missed is
disproportionately the *leapfrog* half. Of the nine leapfrog claims in §The
competitive argument below, seven come from areas with little or no standards
coverage.

**A second blind spot, worth naming because it is methodological rather than
topical.** The frame could not see **the absence of a consumer**. P4's STA
produces a critical path; without an API it is a picture. P5's coverage produces
numbers; without an API they are a dialog. P6's cell mapping produces a cell
count; without an API it is a status bar. No standard is violated by any of
that.

**A third: self-inflicted structure.** The 5,314-line diff; the 82.3% of events
that model no time; `SubCircuit.react` (`src/jls/elem/SubCircuit.java:621-636`)
posting one event per input pin with a `HashMap` lookup and a `BitSet` clone
each, **to model zero elapsed time** — so hierarchy, the thing JLS teaches as
good practice, makes JLS slower, which is the same teaching inversion P3 names
in the exporter (`README.md:346-351`) reproduced in the kernel and never written
down. None of these is a standards gap. All are measured facts about the tree.

---

## The amended program structure

Seven of the eight studies independently proposed themselves as "P7." That
collision has to be resolved before anything else, and resolving it is most of
the structural work. The disposition below assigns each area exactly one owner,
under the parent document's own rule that **no change appears twice**.

### Folded into an existing program

**lf-04 formal equivalence and grading-by-proof → P5.** Not a new program: it is
P5's changes F and G promoted from a line item to the program's headline, plus
one slice P5 does not currently contain — the grading contract. lf-04 agrees
with every one of P5's judgements and changes only the size and the ordering.

**lf-05b power and activity → moves from P4 into P5.** `README.md:421` and
`:477` place "switching activity + SAIF" in **P4**; `README.md:518-522` places
"net toggle coverage per bit" in **P5**. **Those are the same counter.** Build
it once under P5's `sim.coverage-collector` seam, where the program depends on
nothing; P4 then *consumes* it for the energy story once the delay model and
glitch detector exist. Net: P4 −2…−3 weeks, P5 +1, duplication gone.

### New programs, standing alone

| # | Program | Size | Useful floor |
|---|---|---:|---|
| **P7** | Parameterization and elaboration | 25–36 wk | 8–11 wk (params, no `Array`) |
| **P8** | The compiled engine | 24–35 wk | 11–16 wk (Mode T alone) |
| **P9** | Causal debug and time travel | 19–27 wk | 6–8 wk (journal + `--why`, no checkpoints) |
| **P10** | Fault simulation and DFT | 12–18 wk | 6–9 wk (serial fault sim + coverage, no ATPG) |
| **P11** | Semantic diff, merge and version control | 18–27 wk | 9–13 wk (diff half only) |
| **P12** | The programmatic API and platform | 19–29 wk | 8–11 wk (no `Session`) |
| **P13** | Clock, reset and domain architecture | 13–18 wk marginal | 8–11 wk (structural: C1+C2+C3) |

---

### P5 — amended: verification, proof and coverage

**Amended size: 33–50 maintainer-weeks (7.5–11.5 maintainer-months).** From
19–28, less changes F and G at 6–9, plus lf-04's 20–30 for the whole formal
capability, plus ~1 for the toggle counter arriving from P4.

**Amended unlock list.** Everything the parent document lists, plus:

- **Grading by proof rather than by vectors.** `jls -b -equiv ref.jls -cex cex.t
  -formal-report result.xml submission.jls`, returning *"proved equivalent for
  all 512 inputs"* or a counterexample **minimized, simulated-to-confirm, written
  as a frozen-contract `-t` file, and rendered on the student's own drawing with
  the failing cone highlighted.** Today the shipped grading criterion is three
  literal lines of a report format in `examples/autograde/autograde.py`, pinned
  in CI by `AutogradeBridgeExampleTest`; a submission wrong on 254 of 256 inputs
  and right on one passes.
- **Don't-care-aware grading.** Today `TruthTable.react:1446-1449` destroys the
  don't-care on the output side (`if (outValue == 2) outValue = 0;`), so an
  instructor cannot express a BCD-to-seven-segment reference and **a student who
  exploits a don't-care to build a smaller circuit is marked wrong by the vector
  grader** — punished for the exact optimisation the Karnaugh-map lecture taught.
  With P1's `Bits4` and a care predicate in the miter, that student passes and
  the report says *"equivalent on the care set; differs on 6 don't-care inputs,
  using 3 fewer gates."*
- **Exactness as a computable property:** which input patterns a `TruthTable`
  fails to cover, whether two `TriState` enables can both be high, whether an
  output is constant — one SAT query each, all silent today.
- **A regression oracle for JLS's own biggest planned refactor.**
  `ARCHITECTURE.md:359-372` binds any future execution strategy to observable
  identity with the event model; today the only oracle is the #202 golden plus
  fuzzing. A formula extractor makes the criterion checkable by construction on
  the combinational subset — the capability aimed at grading students turns out
  to be the tool for grading P8.
- **Toggle/activity counting, SAIF out to OpenSTA**, and the honesty rule that
  goes with it (counts and ratios unconditionally; joules only with a named
  technology library and corner).
- **Electrical rule checking** as already scoped, now with a clock-net concept to
  check against once P13 lands.

**Amended dependencies — and this is a correction to a stated claim.** The
parent document says of P5: *"**Dependencies. None.** … It is the one program
that can run start-to-finish in parallel with everything else"*
(`README.md:598-602`). **That is true of A, B, C, D, E and H. It is not true of
F and G.** The formula extractor rides `HdlExporter.buildModel` and therefore
inherits the exporter's reject list verbatim — `Memory`, `SubCircuit`,
`ShiftRegister` (`src/jls/hdl/HdlExporter.java:418-424`, pinned by
`HdlPolicyTest`). Without P3 Stage 1, formal grading covers gate-level
combinational circuits and nothing else — weeks 1–6 of a first-year course, a
real capability, but the roadmap should say so. **The cheap escape is a
formal-only flattening elaborator, ~1 week**, which needs nothing from P3's
instance IR. And `Bits4` (P1-S5) is a genuine hard dependency for the don't-care
case and for nothing else.

**One structural rule that is the feature's licence to operate.** A result type
with three constructors and no default — `PROVED(proofLog) |
COUNTEREXAMPLE(model) | UNKNOWN(reason)` — with exit statuses 4 (unknown) and 5
(not checkable) that are **never passes**, and a test suite of deliberately
uncheckable circuits written *before* the solver. A single publicised false pass
ends the feature permanently, because the fallback — vectors — is still there
and is at least honestly weak.

---

### P7 — parameterization and elaboration

**What it changes.** What a drawing can express. `SubCircuit` holds four pieces
of state and none of them is a parameter (`src/jls/elem/SubCircuit.java:26-37`);
the instance dialog is a name and two radio buttons
(`src/jls/edit/SubCircuitDialog.java:30-155`); width is a saved integer on 14
element classes and a literal in both the file and the drawing.

- A drawable `Parameter` element (the `CIRCUIT` block grammar has no attribute
  items, so a declaration that is not an element cannot be saved).
- Bindings on the instance as expressions, referencing the *enclosing* circuit's
  parameters — which is what makes hierarchy scale rather than just making
  leaves resizable.
- A frozen, specified, **total** integer expression language: literals,
  identifiers, `+ - * / % **`, comparisons, `? :`, and exactly four builtins
  (`clog2`, `max`, `min`, `abs`). Deliberately less powerful than Digital's
  embedded scripting language, because `docs/file-format.md:67-71` says circuit
  files are exchanged between untrusting parties and *a file whose meaning
  requires running a program is not a data format.*
- An **`Array` element** — a drawn box labelled `×32` with five closed port
  modes (`broadcast`, `split`, `bundle`, `chain`, `reduce`), two actions **open
  one copy** and **expand in place**.
- `jls.core.elab`, a real named elaboration phase running *before* the existing
  `Circuit.finishLoad`, so that **nothing in `jls.sim` changes** — the whole
  program's acceptance criterion in one sentence.

**Unlocks.** One adder drawing at every width; a 32×32 register file that is
*drawable at all*; design-space exploration inside one file; the N-sweep
critical-path curve (with P4) that is the only clean answer to "why does anyone
build a carry-lookahead adder"; and the deletion of an actively bad lesson — that
a JLS module is reusable only at exactly the width it was drawn at.

**Compatibility, which is unusual in this roadmap: there is no migration.** A
`.jls` always contains the fully elaborated circuit and the parameterization is
additive metadata beside it. Every existing file loads unchanged and simulates
identically; an older reader that drops the new attributes gets a correct,
working, non-parameterized circuit — **strictly better-behaved than the two
cases `docs/file-format.md:458-478` already flags as known hazards** (`initrle`
and `sync`). FORMAT 3 anyway, for the new tags.

**Relationship.** New program. `Array` lands after P3's reuse-identity slice
(same fact: "these instances share one template"); the *parameter* half precedes
P3 entirely. Independent of P1, P4, P5, P6. **Makes P2 cheaper** — a
parameterized `Memory` wrapper drawn once beats N compiled-in port
configurations. **Corrects a P2 attribution:** the roadmap credits the 62 of 228
`riscv/` elements to P2's missing register pin (`README.md:260-266`); half is
P2, and the other half is that even with a perfect `Register` a student still
cannot draw thirty-two of anything.

**Acceptance test, and it should be stated as one:** delete
`riscv/jlsbuild.py`'s role as a producer and draw the CPU.

---

### P8 — the compiled engine

**What it changes.** JLS interprets. There is no elaboration step, so there is
nowhere to put a compiled netlist: `Simulator.initSimulation`
(`src/jls/sim/Simulator.java:180-202`) produces no netlist, no ordering, no
index, and simulation state stays scattered across the object graph the circuit
was drawn into. **There is no array of anything.**

- An elaborator producing a flat `Netlist` with dense integer ids, a
  **bidirectional `nodeId ↔ drawn element` map built on day one**, twelve
  opcodes covering >97% of the measured event mix, and an **ESCAPE opcode** for
  everything else — the decision that stops the second engine from becoming a
  second element library.
- A levelizer (Kahn, sequential elements cut) whose free by-products are
  **combinational-loop detection**, which JLS does not have at all, and **P4's
  timing DAG**.
- Tarjan SCC so feedback is *partitioned*, not refused — a cross-coupled NAND
  latch is a first-year lab and must not opt the sequential unit of a course out
  of the fast engine.
- **Mode T (timed-levelized): default, no flag, no semantic change.** The queue
  stays the sole authority on time; the compiled pass owns only the zero-delay
  closure. Removes ~82% of queue traffic. `ARCHITECTURE.md:359-368`'s
  equivalence criterion is satisfied **as written, with no amendment**, and every
  golden stays byte-identical.
- **Mode C (cycle-based): opt-in, a declared alternative strategy** with a
  normative document section, a #221 amendment, and a **refusal policy that names
  by element what it cannot model** — because a fast mode that quietly means
  something different is how a teaching tool teaches something false.
- **Deliberately not built: levelized-with-delays.** With reconvergent fanout and
  transport delay a net's within-cycle history is a *train* of transitions, and
  one sweep cannot produce it. `Adder.resetPropDelay` sets `propDelay = bits *
  defaultPropDelay` — 960 for 32 bits — while `riscv/verify.py:15` sets the clock
  half-period to 1000. Reproducing that is the queue's job.

**Unlocks.** Watchable execution of a real program on a drawn CPU (today
`Animate` repeats a step **once per second**, `InteractiveSimulator.java:136`);
differential fuzzing with control flow, at CI scale, instead of 6–24 straight-line
instructions in six concurrent JVMs; interactive projects — VGA timing, a pong
paddle, a UART echoing keystrokes — that need ~25,000 clocks per frame; and
**event-driven versus cycle-based as a taught topic**, via a `--compare-engines`
run no other tool in the class can give.

**Relationship.** New program, promoting `README.md:700`'s standalone row and
`:844-845`'s spine half-line. **Hard dependency on P1-S1**, measured, not
argued: 4.32 ns/node with plane arrays against 22.01 with `BitSet[]`. Building
it first yields a 1.4× engine, a second set of four-state truth tables written
against a layout that gets thrown away, and a "we did all that and got 40%"
narrative that kills the value-domain program with it. **Hard dependency on
#77.** Absorbs the standalone interactive-engine-batching row (`README.md:699`),
which is a *prerequisite*, not a companion.

---

### P9 — causal debug and time travel

**What it changes.** Nothing in the tree records why a value is what it is. The
simulator computes provenance on every propagate and discards it on the same
line: `WireNet.propagate:464-465` holds a reference to the winning driver and
`:484` drops it. `Input.setValue` (`src/jls/elem/Input.java:59-62`) is a two-line
field write that records neither when nor from where. `SimEvent` has **no cause
field**. `TraceSample` and `Trace.Change` are one field short of being causality
logs — and the caller has the `SimEvent` in hand at both write sites.

- **A causality journal at net-change granularity, not per-event.** The census
  makes this cheap: 2,331,793 events but only 378,129 `NewValue` payloads, so a
  full-fidelity journal for the entire 6004-cycle CPU run is **~380 K records,
  ≈9 MB** — a classroom circuit's whole run is tens of kilobytes.
- **Over-approximate `causalInputs()`, then refine four elements.** Default to
  all attached inputs: exactly correct for 21 of 25 `react`s, merely wider than
  necessary for `Mux`, `TruthTable`, `Register`, `Memory`. Over-approximation
  never omits the true cause, so the feature is correct on day one and gets
  sharper on a greppable worklist — keystone B's `zeroFill()` mechanic applied to
  a second migration.
- **The headless artifact, and it is the leapfrog axis:** `jls -b --why
  'alu_out[3]@41200' circuit.jls` printing a deterministic causality tree to
  stdout — diffable, gradeable, CI-testable.
- **Checkpoint / restore / step-back.** The entire simulation state of a
  1,551-element RV32I CPU is **≈9 KB raw, 2–3 KB deflated** — it fits in a
  network packet — because the queue's bulk is regenerable (max depth 12,093,
  essentially all pre-posted `SigSim` stimulus) and circuit structure is
  invariant during a run.

**Unlocks.** The right-click "why?" that turns twenty minutes of bisection into
ten seconds, on the drawing the student made. **X-source tracing, which is the
answer to the question P1-S2 creates** — shipping P1-S2 without it is shipping
the question without the answer. Assertion failures gaining a cause for free, at
zero cost to P5. Glitch attribution (P4's detector says *that*; the journal says
*why*). Deterministic replay becoming a **tested property** rather than a claim
in `docs/simulation-semantics.md` §3.

**Relationship.** New program. **Hard prerequisite: P1-S0's per-`Simulator`
sequence counter** — `SimEvent.sequence` is a mutable `static long`
(`src/jls/sim/SimEvent.java:87,119`) and a restore that does not reset it cannot
guarantee deterministic replay. P1-S0 is on the critical path of exactly one
thing in the roadmap, and this is it. Land the journal **after P1-S1** or it will
be measured as a slowdown and switched off forever. **Schedule P9's checkpoint
capture inside P1's element pass** — both walk the same 25 `react` / 28 `initSim`
implementations, and doing them six months apart is precisely the mistake
keystone C warns about for the value domain and the compiled pass.

---

### P10 — fault simulation and DFT

**What it changes.** Adds a fault model to a simulator that has none. Three
structural facts make it cheap: a single funnel per direction (`Input.setValue`,
`Output.propagate`, both writing `Put.currentValue`); fault sites that already
have permanent, save-stable, diffable names (`ElementId` + put name + bit); and
a re-simulation lifecycle that already supports "load once, simulate N faulty
machines."

- **The fault site is a `Put`, not a `WireNet`** — which gives the stem/branch
  distinction, the first non-obvious thing a testability unit teaches, directly
  out of a data structure JLS already has. A net-level model deletes the lesson.
- **Injection is a clamp field on the `Put`, set at elaboration, not a map
  lookup.** A `Map<Put,Fault>` consulted per propagate is exactly the shape of
  the `dupCheck` `HashSet` that keystone C measures at 25.4% of the loop, and
  must be rejected on that measurement. **Acceptance criterion stated up front:
  with fault support compiled in and no fault active, `RiscvCpuGoldenTest` is
  byte-identical and its runtime is within noise.**
- Collapsing at gates, across the seven zero-delay wiring elements (34
  `Splitter` + 9 `Binder` + 5 `Extend` of 225 logic elements on the CPU — the
  largest reduction available), and on fanout-free stems. **Three coverage
  numbers reported separately** (fault coverage, test coverage, fault
  efficiency) with the observable set printed in the header, because a coverage
  number without its observable set is not a number.
- **PODEM, not the D-algorithm** — it only ever assigns primary inputs, handles
  the XOR-heavy reconvergent circuits a student just drew, and is ~300 lines.
  The D-algorithm's residual value is watching the D-frontier move: ship it as a
  visualizer, not as the engine.
- Full scan as both the way to make ATPG work and the DFT lesson worth teaching;
  its scan cell **is** the #129 boundary-scan cell at a different boundary.

**Relationship.** New program. **Depends on P5** for the report channel and
exit-status contract and the `app.report-writer` seam — do not ship fault
results before that channel exists or they will land on stdout and break a
frozen format. **Depends on P4's timing-DAG cut for the ATPG half only.** Edits
**zero** of the 25 `react` implementations. **Gains honesty from P1-S2**, and
this is a real caveat rather than a nicety: in the two-state domain
`LogicElement.initInputs` zeroes every input at every depth, so an SA0 fault on
a never-driven line is classified untestable when the truth is "unknown." Ship
before P1 if the schedule wants it, but the caveat goes in the **report header**,
not only the documentation. **Shares its scan cell with the #129 standalone
item** — count it once across both.

**Scope discipline:** combinational ATPG over a full-scan cut. Sequential ATPG
is a research problem, and the deliverable drifting toward it is how the program
fails to finish.

---

### P11 — semantic diff, merge and version control

**What it changes.** JLS already has **four of the five** things a semantic
schematic merge needs, all shipped and tested under a different program: a
plain-text normative format, permanent per-element identity (`sid`, #165), a
byte-canonical serializer that is a pure function of content (#166), and a
closed, validated, invertible, serializable edit algebra (`src/jls/collab/op/`,
#167). The fifth — a function that compares two circuits and a function that
merges three — does not exist. `ls src/jls/` shows no `diff`, no `merge`, no
`vcs`. The `collab.op-observer` seam exists and **has zero contributors**.

- **C1 — FORMAT 3 diff-stable serialization.** `ref`, `probe` and `pair` anchors
  carry stable ids; the positional `int id` line disappears. Acceptance
  criterion stated as a number because the current number is measured: **the
  `addi.jls` one-gate insertion diff must fall from 5,314 lines to 9.** Plus
  `-canon [file|-]` and a **git clean filter** (not smudge — the reader sniffs
  the container, so plain text *is* a valid `.jls`).
- **C2 — `jls.diff`**, a typed `CircuitDelta` keyed on `(subcircuit instance
  path, sid)`, with a **mandatory** structural fallback for `legacy:` ids and a
  one-time `jls -adopt` rewrite. Diff on legacy files is best-effort and says
  so; **merge on legacy files is refused.**
- **C3 — rendering**, of which the SVG overlay is nearly free:
  `CircuitRenderer.exportImage` already produces **byte-deterministic** SVG,
  pinned by `SvgExportTest.exportingTwiceIsByteIdentical`.
- **C4 — three-way merge**, and the architectural insight that justifies the
  whole program: **the per-kind merge rule table is the same object for the
  online collaborative merge and the offline git merge, and neither exists yet.**
  Build it once, in state-based form, offline first — and the offline tool
  becomes the executable specification, the test oracle, and the anti-entropy
  primitive of the online CRDT. Implement the STRICT (partial) table and derive
  the AUTO (total) online policy by appending one deterministic tiebreak.
- **C5 — git integration**, with the acceptance rule that is the product: **the
  merged output either loads and elaborates (`finishLoad`, `WireNet.makeNet`) or
  is reported as a conflict. There is no third outcome.** Conflicts produce
  `alu.MERGE.jls` (with a `Text` annotation at each conflict site — an existing
  frozen tag, no format change), `alu.MERGE.txt` and `alu.MERGE.svg`.

**A defect found in passing, worth fixing regardless:** `-b -savetext out.jls`
**silently does nothing** — `JLSStart.start` is a mode chain (`:168`, `:282`,
`:363`, `:478`) and batch wins. Exit status 0, no file written, no diagnostic.
It has survived because nothing downstream consumes the text path.

**Relationship.** New program, and **the least entangled one in the roadmap**:
it depends on none of P1–P6, touches no `react`, no value domain, no timing
model. It extends the collaboration program (#163) by **pulling its Stage 3
forward in front of Stage 2** — which the maintainer's own research document
already sanctions (`collaborative-editing-research.md:579-582`) and which is
stronger than that document's framing: the offline merger is worth building even
if Stage 2 ships. **One genuine coupling, running from P3 to P11:** P3's
headline round-trip claim — *"equal to the original **modulo element ids**"*
(`README.md:335-338`) — is a structural comparison, and **nothing in the tree
can perform it today.** P11's structural matcher, built anyway for the legacy
fallback, is exactly that comparator. P3's most differentiating claim currently
has no oracle.

**Deliberately refused: any similarity score, cohort ranking or automated
plagiarism flag.** The `sid` replica id is *per install*, so every student on a
shared lab machine has the same one — a "matching replica id" in that
environment is not weak evidence, it is no evidence, and it systematically
implicates exactly the students who cannot afford their own laptop. Ship the
pairwise comparison; a comparison is a tool a human uses after forming a
suspicion, a score is a machine forming the suspicion.

---

### P12 — the programmatic API and platform

**What it changes.** JLS has exactly one programmatic surface: a command line
with one verb. There is no way, from any language including Java, to construct a
circuit without writing save-format text, ask what element types exist, get
diagnostics as data, get values as data, advance a simulation by a step, or run
a second circuit in the same process.

Five workarounds carry the evidence: `riscv/jlsbuild.py` (322 lines
transcribing `src/jls/elem/*.java`'s `save` methods into Python string
literals); `test/jls/CircuitTextBuilder.java` (422 lines — **the project's own
test suite cannot construct its own model without serializing to text**, and its
javadoc says it is at least the second time it has been written);
`riscv/jlsrun.py`'s five regexes over a human-readable report;
`riscv/build/k2000_clk.txt` at **193,040 bytes measured** to express
`advanceCycles(2000)`; and one JVM per experiment, of which **0.194 s of a
0.345 s run is startup, measured**.

- **`jls.api`** — five nouns: `Design`, `Edit` (**every verb is a
  `CircuitOp`**, so the API inherits validation, atomic rejection, exact
  inverses and undo, and *cannot construct a circuit the editor could not*),
  `Elaboration`, `Run`/`Session`, and **`Catalog`** — the element schema
  published as data, which is what makes a generator library *generated* rather
  than transcribed.
- **The scripting face: a documented NDJSON request/response protocol over
  stdio, `jls --serve`.** Not an embedded interpreter (the jar is **2.6 MB
  measured**, GraalJS is an order of magnitude larger, Jython is Python 2.7, and
  a student's assignment script is untrusted code that #222 already puts out of
  process). Not a generator library alone (that is `jlsbuild.py`'s ceiling: it
  can write and never read, elaborate, simulate, step or query).
- **Two design rules set on day one, not retrofitted.** Values cross the
  boundary as **four-state-capable strings** (`"10xz"`) from the first release,
  while every character is still `0` or `1` — the single decision that
  determines whether P1 breaks the protocol or extends it. And **no callback
  direction, ever**, written into the normative document as permanent, because
  `Session` is one feature request away from reopening #63.

**Unlocks.** Procedurally generated assignments — thirty students, thirty
different-but-equivalent problems, graded by the same script run backwards —
which is the single thing instructors ask for that JLS structurally cannot do.
A student plotting their own O(N) adder-delay curve. A generator lab. `riscv/`
stopping being a text generator. **Acceptance test: no save-format string
literal appears anywhere in `riscv/`.**

**Relationship.** New program, and the only one that **makes the other six
reachable from outside the GUI** — P12 is a multiplier, not a seventh peer.
**Hard dependency on #77, harder than any other program's**: the roadmap lets
P1's `LogicValue` land inside `jls.core` before the extraction, accepting that
P1's element pass doubles as part of it; **P12 has no such escape**, because its
entire content is "the core, addressable," and today `JLSInfo` is a
process-global hub (163 refs), `System.exit` is the error channel (77 sites),
and there is no boundary to publish. Conversely, **P12 is the strongest
available argument *for* #77**, because it converts "better layering" into a
shipped capability.

---

### P13 — clock, reset and domain architecture

**What it changes.** JLS has a clock element; it does not have a clock.
`src/jls/elem/Clock.java` is 432 lines producing a square wave, with three saved
attributes and no phase. A clock is not distinguishable from data anywhere:
`HdlExporter.java:485` treats `Clock` and `InputPin` identically, and
`HdlModel.RegisterStatement`'s field doc states the situation in six words —
*"1-bit clock; a literal clock never ticks"* — filing under commentary the fact
that the IR knows a register can be clocked by a constant. There is no clock-sink
concept, only three private copies of an edge detector (`Register.currentC`,
`StateMachine.oldClock`, `Memory.lastClock`), in a codebase that introduced the
`Timed` interface specifically to stop that pattern for propagation delay. The
clock lives in `Group.COMBINATIONAL`.

Consequences: **the most common multi-clock teaching setup — two identical
clocks 90° apart — is not expressible**, because the first rising edge is pinned
at `cycle − one` and there is no phase attribute. Setup time is zero and hold
time is zero. **A two-flop synchronizer in JLS is two flip-flops that cost 100
time units of latency and change nothing else** — JLS does not merely fail to
teach why the second flop exists; it demonstrates that it does not.

- **C1** a `Clocked` capability interface consolidating the three edge
  detectors, plus `int phase` on `Clock` mapping exactly onto SDC's `create_clock
  -waveform`.
- **C2** clock domains **inferred, not authored**, with derivation records
  (gated / generated / muxed / undriven) — **and the inference reports itself
  before it reports any violation**, because a CDC tool whose domain model is
  wrong produces confidently wrong violations.
- **C3** the crossing check, over *the same graph* P4's STA builds; synchronizer
  recognition **including the fanout condition**, which is the thing students get
  wrong; **silent by default on single-clock designs**, because a checker that
  talks in the common case is one students learn to ignore.
- **C4** `Synchronizer` and `ResetSynchronizer` elements — and deliberately **no
  `AsyncFifo` element**: the entire teaching value is that you build it, exactly
  the argument the roadmap already makes about the array multiplier.
- **C6** seeded metastability: on a setup/hold violation the capture drives X and
  resolves to 0 or 1 from a per-run seed, **independently per bit**. Two things
  are theatre and are refused **in writing** as normative non-claims: any **MTBF
  number** (needs τ and T₀ that JLS cannot have — it would be the §7 delay table
  again, worse, because the number looks authoritative) and **animating a
  decaying oscillation**. Draw X.

**Relationship.** New program, second-most-gated after P6, and small because it
*consumes*: P1 supplies the value, P4 the trigger, P2 the pins, P3 the port
roles, P5 the report channel. It contributes the clock-domain model, which no
existing program owns. **It splits cleanly, and the split is the scheduling
finding:** the structural half (C1+C2+C3+C5's checks, 8–11 wk) is parallel-safe
and gated on nothing; the dynamic half (C6, C7) is gated on P1-S2, P1-S4 and
P4's timing checks. **Ordering correction to P4:** `README.md:474` prices STA at
5–8 weeks assuming a single implicit clock; C2 must precede or accompany it, or
an STA over a two-clock design computes a number that means nothing.

---

## The competitive argument

The eight areas divide cleanly, and the division is not flattering everywhere.

### Parity — things serious tools have that JLS structurally lacks

| Capability | Who has it | JLS's position |
|---|---|---|
| Parameterization / generics | hneemann's Digital (**verified**: README + `ResolveGenerics.java` read); commercial universally, in text | Behind the best peer tool. Parity is the floor. |
| Compiled / levelized evaluation | Verilator (**verified** as JLS's named model in its own docs); Yosys CXXRTL (*unverified*); commercial | Behind by ~15 years. Mode C is catching up to a 2010 design. |
| Causal tracing and reverse debug | Verdi Temporal Flow View (*secondary-verified*); Questa Visualizer Time Cone (**verified**); Cadence Indago reverse stepping (**verified**) | Absent. Three of four major commercial debug environments have both. **Any claim of novelty on the idea would be false.** |
| Formal equivalence engines | Yosys `equiv_*`/`sat -verify`, EQY, ABC `cec`/`dsec`, SymbiYosys (**verified**) | Absent. The engines are free and better than anything JLS would write. |
| Fault simulation / ATPG engines | TestMAX, Tessent, Modus; AUCOHL Fault, Atalanta, HOPE (all **verified** as existing) | Absent. |
| Power / activity numbers | PrimePower, Joules; OpenSTA `read_saif` + `report_power` inside OpenROAD (**verified**) | Absent as a concept. |
| Schematic diff | Altium (**verified via vendor doc snippets**) | Absent — and Altium's is better and will stay better. |
| A scripting interface | SKILL, Tcl everywhere, Verilator's C++ API, KiCad IPC, DigitalJS-as-library | Absent. **Table stakes.** JLS is one verb into it. |
| Multi-clock STA and structural CDC | Questa CDC, VC SpyGlass CDC, Jasper, Meridian (*capabilities unverified*); multi-clock STA is table stakes in every FPGA/ASIC flow | Absent. |

Nine parity gaps. That is the honest headline of the amendment: **on most of
these axes JLS is not merely behind commercial tools, it is behind the best free
peer educational tool or has no implementation at all.**

### Leapfrog — where the incumbent is genuinely weak

Ranked by how well the incumbent weakness is evidenced.

**1. Proof-based grading with a counterexample on the student's own drawing
(P5/lf-04).** The cleanest leapfrog available, on three independent grounds: the
technique is fifty years old and completely documented, so there is no research
risk; the solvers are free, so there is no cost barrier; and the gap is an
*interface* gap nobody is trying to close. Conformal and Formality check RTL
against a gate netlist inside a synthesis flow and **none of them owns a
schematic editor**, so none can render a counterexample onto the thing the user
drew. Yosys/EQY/ABC are netlist-in, report-out. And the peer tier stops at test
vectors: Digital's README lists testing and analysis and **does not mention
formal, SAT or equivalence** (**verified by fetch**) — note the near-miss, that
circuit→truth-table *is* exhaustive equivalence by enumeration, the right idea
stopping at 2²⁰; Logisim-evolution has `--test-vector` CSV, i.e. exactly the
sampling surface JLS already has. **The claim: the only tool in any of the three
classes where a student draws a circuit, presses one button, and is told either
"proved correct for all 512 inputs" or "here is the input that breaks it," with
the failing input already loaded and the failing cone highlighted on their own
drawing.** Not because JLS out-solves anyone — because it is the only one
holding both ends, the drawing and the proof.

**2. A `git merge` driver whose output cannot be a file the tool refuses to load
(P11).** The incumbent weakness here is *verified in the incumbents' own words*.
Keysight's design-data-management material states that "the inability to
automate merging for schematics and layouts needs a centralized repository" and
prescribes locking (**verified via search snippet**). AMD's documented practice
for block designs is to store `write_bd_tcl` output and **regenerate rather than
merge** (**verified**) — the vendor's own answer is "don't version the graphical
artifact." KiCad, the best-positioned open tool, has per-symbol UUIDs and no
schematic revision diff; its ecosystem (KiRI, KiCad-Diff, plotkicadsch) renders
both revisions and **compares the pictures** (**verified**). Digital's `.dig`
carries **no element identifier at all** — identity is `<pos x= y=>` and
connectivity is coordinate coincidence (**verified by direct fetch**), so
semantic merge there is structurally out of reach. NYU tells Logisim students to
"work on a single computer at a time" (**verified**). AllSpice.io is a
venture-funded company whose premise is that diff was "ubiquitous in software
and nonexistent in hardware." **The four things nobody has: it is a git driver
rather than a captive vendor DDM layer; the merge output is validated by the
editor's own operation vocabulary; one rule table serves both the online and
offline merge, with the offline one as the online one's oracle; free, offline,
single-jar.** *The honest counter-argument, stated first: the mainstream escape
from schematic merge is to stop drawing schematics, and that answer has largely
won. JLS cannot take it — which is exactly why solving it here is
differentiating rather than redundant.*

**3. Fault simulation drawn on the schematic (P10).** The incumbent weakness is
categorical and verified on both sides: **the entire fault-simulation tool class
is netlist-in / text-report-out** (Fault, Atalanta, HOPE — all verified), and
**the entire schematic tool class does no fault simulation** (Digital: no;
Logisim-evolution: no — both verified against published feature lists). Fault
simulation is a fundamentally *structural* concept taught with a drawing: you
circle the stuck line, you highlight the sensitized path, you watch the D
propagate. **Nothing draws it, at any price.** JLS is the only tool positioned
in both halves. *One contradicting claim exists — a 2021 Hackaday page asserting
Digital simulates faults and generates a fault dictionary — and it is
**contradicted by Digital's own feature list**; treat it as unverified and
probably mistaken, and expect to find it when searching.*

**4. Schematic-native causality and a headless `--why` (P9).** The idea is not
novel and must not be sold as such. The weakness is precise: **all three
commercial tools explain RTL and synthesized netlists, not a drawing** — Verdi's
cone is over HDL statements, Questa's fan-in is over the elaborated design, and
*the schematic the student drew does not exist in either tool*, structurally,
because none owns a schematic-first model. JLS's model **is** the schematic. This
is the same argument P6 makes for layout cross-probing and it is **stronger
here, because it needs no cell library and no PDK**. Second: all three are gated
behind a licence, a proprietary database and a workflow, and none is reachable
by a first-year or runnable from a grading shell script. Third, and this is the
axis where JLS could be unambiguously ahead: **a deterministic batch causal
artifact** — *marked unverified as an absence*: I could not find a documented
`why is S = V at time T` stdout command for any of the three, but all three ship
Tcl and a determined user could script one, so the claim is that none
**documents** it, not that none is achievable.

**5. Clock-domain inference without a constraint file (P13).** One of the two
claimed advantages here is architectural and one is not, and the studies are
honest about which. **Real:** a commercial CDC run says nothing useful until the
constraints are right, and a *wrong* constraint file produces a clean report. In
a schematic the clock roots are *in the drawing* — a `Clock` element is
unambiguously a clock — so inference is exact and the constraint file is
optional. **No RTL tool can do that, because RTL has no `Clock` element.** Also
real: metastability injection is a licensed, separate, rarely-used flow, and JLS
can make it the default in a lab mode at zero licence cost, which turns "testing
does not find CDC bugs" into a number the student measured (run 100 seeds, 87
pass; add the synchronizer, 100 pass). **Not real, and explicitly disclaimed:**
report volume and waiver triage. That is a weakness *of scale*, and JLS never
has that scale — the advantage is "small designs," not "better tool." *The
absence of a maintained open-source structural CDC checker is an **unverified
negative** and must be re-checked before it is asserted in public.*

**6. A drawn `Array` you can open, expand in place, and count (P7).** Digital
has solved parameterization and is the tool to beat, but its replication
mechanism is **a script embedded in the drawing** (HGS, with `addComponent` /
`addWire` — verified by reading `ResolveGenerics.java`): the replicated
structure is not drawn, not diffable, and cannot be inspected without running
it, which is the same complaint that makes Verilog `generate` hard to teach,
reproduced inside a schematic tool. An `Array` with five closed port modes and
*expand in place* is a better teaching answer, and total-arithmetic expressions
are a better answer for a format that is exchanged between untrusting parties.
**Honest ceiling: JLS will never out-parameterize SystemVerilog. It can
out-teach it, and that is the whole of the claim.**

**7. A published `catalog` verb and a ratchet-tested versioned API contract
(P12).** Parity on existence — this is table stakes and JLS lacks it. The
leapfrog is only on contract quality, and the incumbent weakness is specific
rather than manufactured: vendor scripting surfaces are **undiscoverable** (no
machine-readable catalog; you learn a vendor API from a PDF), **unversioned** (no
vendor scripting surface carries the promise `docs/batch-interface.md` §6 makes;
flows breaking across tool releases is a standing operational cost in every EDA
shop), and in-process and therefore language-locked. JLS's surface is small
enough to *be* a promise — five nouns, ~30 verbs — and this project has already
held three frozen text formats behind four ratchet tests. **Cadence cannot
publish its whole API surface as a contract; JLS can.**

**8. Both engines in one jar, with CI proving they agree (P8).** Leapfrog *in
combination only*, and the combination is: a schematic you drew yourself running
a real program at tens of thousands of cycles per second, with the trace window
still working, every compiled node still mapped to the element you placed, and a
semantically slower reference engine in the same jar that CI proves agrees with
it. Verilator gives speed and no picture; Logisim and Digital give a picture and
(as far as can be verified) no compiled engine. *Most of that peer row is
**unverified** — Digital's fast mode, Logisim's simulation model and DigitalJS's
internals were not confirmed.* **JLS cannot lead on raw throughput and should not
try**; Verilator will stay several times faster and that is fine.

**9. Power on a drawing (P5/lf-05b).** The most modest claim and it should not be
oversold. The numbers JLS can produce without a technology library are strictly
weaker than OpenSTA's with one, and the right move is to **hand OpenSTA the
SAIF** rather than compete — a flow that is verified to work today. What JLS
uniquely has is the drawing (a per-net toggle heat map no waveform viewer and no
netlist power tool can show) and the three-views-of-one-phenomenon story: P4's
glitch detector makes a hazard visible, the toggle counter makes it countable,
the ratio makes it cost something.

### Where JLS cannot plausibly lead — stated once, plainly

Raw simulation throughput (Verilator). Solver scale (ABC, Yosys). Unbounded
sequential equivalence on non-matching encodings (delegate via BTOR2, always).
Sign-off multi-clock timing — clock uncertainty, jitter, OCV, CPPR — and
functional/protocol CDC. Million-instance causal tracing with clock-domain-aware
database indexing. Interactive polish of a mature commercial compare UI. ATPG
against TestMAX. Twenty-five years of SKILL flow libraries. And **JLS must never
claim CDC sign-off, shuttle-flow conformance, or IEEE 1149.1 conformance** — the
honesty rule inherited verbatim from the #129 costing applies to every one of
these programs.

---

## The amended spine

Three nodes appear that were in no program, and one existing node changes status.

```
#77 headless core ──────── WAS: "one hard ordering constraint" (before P1's Stage 5).
   │                       IS:  hard predecessor of P12 (no escape), and the natural
   │                            home of P8's netlist/plane arrays/levelizer and of
   │                            P7's jls.core.elab. Load-bearing for four programs.
   │                       src/jls/core/ holds eight files and all are geometry.
   │
P1-S0 kernel hygiene ───── ships alone, 2-3 wk. NOW ALSO P9's HARD PREREQUISITE:
   │                       SimEvent.sequence is a mutable static long (:87,119) and
   │                       a restore that does not reset it cannot replay determinis-
   │                       tically. P1-S0 is on the critical path of exactly one
   │                       other thing in the roadmap, and P9 is it.
   │
   ├─ [NEW NODE] THE ELABORATOR ── flatten hierarchy, union nets across jumps,
   │      │        dense ids, node<->element map.  4-6 wk, chargeable partly to P3.
   │      │        The code already exists IN THE WRONG PLACE: HdlExporter's
   │      │        UnionFind net walk (src/jls/hdl/HdlExporter.java:1038-1109).
   │      │        SEVEN CONSUMERS: P3 hierarchy IR, P4 timing DAG, P5's formal
   │      │        flattener, P8's netlist, P9's site index, P10's ATPG cut,
   │      │        P13's clock slice.  Two implementations of "which drawn wires
   │      │        are one signal" is how the engines come to disagree.
   │
   ├─ [NEW NODE] THE REPORT CHANNEL + EXIT-STATUS LATTICE ── 1 wk, five consumers.
   │      │        P5 assertions (3), formal (4 unknown / 5 not-checkable), fault
   │      │        reports, P11 diff (3) / merge3 (4), P12's structured results.
   │      │        Already flagged "must be designed first, because it is a change
   │      │        to a promise" (README.md:541) when it had ONE consumer.
   │      │        Design it ONCE, with the full verdict lattice, or reopen it four
   │      │        times.
   │
   ├─ [NEW DESIGN DECISION] THE SITE / SLOT INDEX ── P9's journal site index IS
   │               P8's levelization slot table IS P6's cross-probe map IS P4's
   │               critical-path overlay key. One table, four payoffs. Must be
   │               designed BEFORE either P8 or P9 writes code, or it is two
   │               permanently disagreeing indexes.
   │
P1-S1 LogicValue ───────── dependent count GREW. Old dependents unchanged, plus:
   │                       P8 (measured 4.32 vs 22.01 ns/node - hard),
   │                       P9 (journal allocates nothing under Word - effectively hard),
   │                       P10 (fault clamp: two ALU ops vs two BitSet ops),
   │                       P5-formal (dual-rail reads the value type's own planes).
   │
   ├─ P1-S2 X producible ── SHIP WITH P9's X-source tracing. Shipping P1-S2 without
   │                        it is shipping the question without the answer.
   ├─ P1-S3 strength     ── P13's crossing honesty; P5-formal projects to {0,1,X,Z}
   │                        and refuses strength-dependent designs by name.
   ├─ P1-S4 U + reset    ── P13's reset architecture stops being decoration.
   └─ P1-S5 Bits4        ── P5's don't-care-aware grading. The ONLY hard P1
                            dependency in the entire formal capability.

PARALLEL-SAFE, GATED ON NOTHING (the amended figure):
   P5's A-E-H + formal core   P7's parameter half   P10's serial fault sim (with a
   stated two-state caveat)   P11 entirely   P13's structural half   P3's export
   coverage / hierarchy / interfaces   P4's delay / units / glitch / STA / activity
   P2's additive-attribute half
```

**What changed about the critical path.**

1. **#77 moved onto it.** It was a soft note — *"sequence #77 before P1's Stage
   5, or accept that P1's element pass doubles as part of the extraction"*. P12
   has no such escape, and P7's and P8's homes are also inside that boundary. The
   keystone of *structure* and the keystone of *capability* are now both on the
   path, and **#77 is earlier**.
2. **A shared node appeared that no program owned.** The elaborator has seven
   consumers and one existing implementation buried in the HDL exporter. Building
   it once, early, inside P3's Stage 1, is worth more than any single program on
   this list.
3. **The report channel went from a 1-week promise to the contract bottleneck of
   five programs.**
4. **P1-S0 acquired a dependent** and stops being purely a hygiene item.

**Is the keystone still the keystone? Yes — with an amendment to what that
means.**

**On reach, more so than before.** Four new programs route through P1-S1, and
two of those dependencies are *measured* rather than argued (P8's 5.1× layout
factor; P9's allocation cost). The value domain is now the enabler of the fast
engine, the debugger, the fault clamp and half the formal encoding, on top of
everything §3 already claimed.

**On monopoly of the critical path, less so.** Five of the seven new programs
have useful floors that need nothing from P1: P7's parameter half, P10's serial
fault simulation, P11 entirely, P12's floor (given #77), and P13's structural
half. The parallel-safe fraction of the roadmap grew from ~60 weeks of 151–220
to roughly **125–165 weeks of 288–424** — more than double in absolute terms and
slightly larger as a share. **The dependency-critical path grew much less than
the total.**

**And the rival list gains a third entry.** The parent document names two rivals
honestly: P3's export coverage (the keystone by *urgency*) and the event queue
(the keystone by *cost*, at 47.7% of the loop). Add **#77, the keystone by
structural necessity** — the one thing that four separate programs cannot be
built correctly without, and the one the other roadmap already named as its own.

---

## Sequencing, amended

The parent document's stages 0–12 stand. What follows inserts four items and
changes one stage's contents.

| Stage | Content | Weeks | Change |
|---|---|---:|---|
| **0** | Kernel hygiene (unchanged) | 2–3 | Now also P9's hard prerequisite |
| **0a** *(new)* | **`-canon` to stdout; fix the `-b -savetext` silent no-op; git clean filter; `docs/version-control.md`** | 1–1.5 | **The cheapest real item in either sweep.** `.jls` stops being an opaque XZ blob in every downstream course repo |
| **1** | Total HDL export coverage (unchanged) | 5–8 | Now also builds the **shared elaborator** (+2–3), which seven consumers want |
| **1a** *(new)* | **The report channel and exit-status lattice, designed once with all five consumers' verdicts in it** | 1 | Was inside P5; promoted, because reopening it is the failure two studies independently name |
| **2** | The verification floor **+ the formal core floor** | 5–7 **+ 8–11** | The change that matters |
| 3–12 | As published, with P4 −2…−3 (activity moves), P5 grown, and P13's C2 landing before or with STA | | |

### The smallest piece of work that most changes JLS's competitive standing

**The formal-equivalence floor: 8–11 maintainer-weeks.** Concretely: a
`FormulaBuilder implements HdlModel.StatementVisitor` (a **third** implementation
of an interface that already exists and is already double-dispatch clean, with
`HdlExporter.buildModel` already doing the port walk, the net union-find and the
identifier legalization); an AIG as the internal IR; Tseitin to CNF; an in-tree
CDCL solver; the miter; `-equiv`; the counterexample written as a `-t` file and
**simulated back through `BatchSimulator` to confirm it before it is reported**;
and exit codes 0/3/4/5. Combinational only, over gates, `Mux`, `Decoder`,
`TruthTable`, `Adder`, `Splitter`, `Binder`, `Extend`, `Constant`.

Preceded by Stage 1a (1 week) and by either Stage 1 or the ~1-week formal-only
flattener. Total to the capability: **10–13 weeks.**

**Why this one.** It is the only item in either sweep where JLS would hold a
capability that no tool in its class has at all *and* that tools above it have
but structurally cannot deliver to this audience. It changes the meaning of the
product's most-used surface — autograding stops being string-matching against
`EXPECTED_STDOUT_LINES` and becomes a proof with a constructed counterexample.
And it lands a conceptual event in week three of a first-year course that
currently arrives, if ever, in a fourth-year elective: *proved, for all inputs*
is a different kind of sentence from *passed 20 of 20 vectors*, and understanding
why is the lesson.

**Against alternative 1 — P3 Stage 1, total HDL export coverage (5–8 weeks),
which is the parent document's own pick and is genuinely smaller.** Do it
anyway, and do it first: it is cheap, it removes the loudest embarrassment (JLS
cannot export its own flagship CPU), and it unblocks #82, #213, #215, P6 and the
non-gate half of formal. But **it is parity work.** Every peer tool exports
something; Yosys and DigitalJS start from netlists. Exporting a CPU makes JLS
not-behind. It does not let JLS say anything no other tool can say. And it is
*not* on the formal critical path, because lf-04 supplies a cheaper substitute
for exactly that dependency — a formal-only flattener at ~1 week, which needs
nothing from P3's instance IR.

**Against alternative 2 — Mode T of the compiled engine (11–16 weeks), which
delivers −30…−40% of loop time with zero semantic change, zero document
amendment and zero golden churn.** Three reasons it loses. It is bigger. Its
*real* cost is 23–32 weeks, because it hard-depends on P1-S1: built first it
yields a 1.4× engine, a second set of four-state truth tables written against a
layout that will be thrown away, and a "we did all that and got 40%" narrative
that takes the value-domain program down with it — measured, at 4.32 versus 22.01
ns/node. And speed is precisely the axis on which JLS **cannot** lead: Verilator
will stay several times faster, and a 30%-faster interpreter changes no
competitive claim.

**The runner-up, named because it is close.** The **diff half of P11 (9–13
weeks)** — C1a + C2 + C3a + C6. It is worth doing regardless of everything else,
because its grading and CI uses need no collaborative workflow to exist:
instructor review of two submissions, diff against a skeleton,
`--assert-fixed-unchanged` (which closes a real autograding hole — today a
student can rewire the parts of a template they were told not to touch and the
grader sees only the output), regression triage, and **P3's round-trip
comparator, which P3 currently lacks entirely.** Its first 1.5 weeks (Stage 0a
above) should ship immediately whatever else is chosen.

**One decision to make deliberately rather than in week two.** The formal floor
contains a solver/licence fork. Sat4j is dual EPL/LGPL, so `library-survey-2026-07.md`
rule 1 is satisfied — but rule 4 rejects projects with dormant release
histories, and Sat4j's release activity **[unverified — must be read from the OW2
GitLab before it is relied on]** may fail the project's own rule. The
recommendation is an in-tree CDCL (~900–1200 lines, thoroughly documented in the
literature, with the printers as a first-class escape hatch), with **DRAT proof
logging on the UNSAT side** — because SAT answers are self-checking through
JLS's own simulator and UNSAT answers are the dangerous ones. Record the choice
in `ARCHITECTURE.md` with a revisit trigger.

---

## Honest totals

De-duplicated. Each change counted once, under the program that owns it. The
"Δ" column is against `README.md` §7.

| Program | Weeks | Months | Δ |
|---|---:|---:|---|
| **P1** value and resolution | 28–36 | 6.5–8.5 | — |
| **P2** element vocabulary | 20–30 | 4.5–7.0 | −2 (P7 subsumes width variants; P13-C1 before register pins) |
| **P3** interchange and hierarchy | 26–38 | 6.0–9.0 | — (elaborator now shared; gains an oracle from P11) |
| **P4** timing and analysis | 23–35 | 5.5–8.0 | −3 (activity + SAIF → P5); STA's 5–8 now assumes P13's C2 |
| **P5** verification, proof and coverage | 33–50 | 7.5–11.5 | **+14…+22** (formal promoted and priced properly; + toggle counter) |
| **P6** silicon on-ramp | 20–32 | 4.5–7.5 | — |
| Standalone items | 8–13 | 2.0–3.0 | −2…−3 (cycle-strategy and interactive-batching rows → P8; scan cell shared with P10) |
| **Subtotal, amended existing** | **158–234** | **36.5–54** | |
| **P7** parameterization and elaboration | 25–36 | 6.0–8.5 | new |
| **P8** the compiled engine | 24–35 | 5.5–8.0 | new |
| **P9** causal debug and time travel | 19–27 | 4.5–6.5 | new |
| **P10** fault simulation and DFT | 12–18 | 3.0–4.0 | new |
| **P11** semantic diff, merge, version control | 18–27 | 4.0–6.5 | new |
| **P12** the programmatic API and platform | 19–29 | 4.5–7.0 | new |
| **P13** clock, reset and domain architecture | 13–18 | 3.0–4.0 | new (marginal over P4/P5; 15–21 standalone) |
| **Subtotal, new** | **130–190** | **30–44** | |
| **TOTAL, BOTH SWEEPS** | **288–424** | **66–98** | was 151–220 / 35–51 |

**Sixty-six to ninety-eight maintainer-months. Five and a half to eight
maintainer-years.** The amendment roughly **doubles** the roadmap.

**Three qualifications on that number, none of which shrinks it much.**

*First, the shared-node credit is conditional.* The table counts the elaborator
once (inside P8, 4–6 wk, wanted by seven), the report channel once (inside P5, 1
wk, wanted by five), the scan cell once (across P10 and the #129 standalone), and
P9's checkpoint element pass as riding P1's. **Those credits are worth a further
8–14 weeks and they are only real if the sequencing above is followed.** Built
independently the total is 300–440, not 288–424.

*Second, the estimates are analogies, not measurements.* Same basis as the parent
document: shipped work the repo records (#78's registry, #166's canonical save,
#167's op layer, #199's synchronous memory, #213's board export). The only
measured numbers anywhere in either sweep are keystone C's benchmarks, lf-02's
re-run of `bench_kernel.py`, lf-06's 5,314-line diff, and lf-07's JVM-startup and
file-size figures.

*Third — and this is the one that matters — the parallel-safe fraction grew.*
Roughly **125–165 of the 288–424 weeks are gated on nothing**: all of P11, P5's
formal core and A–E–H, P7's parameter half, P13's structural half, P10's serial
fault simulation, P3's export/hierarchy/interface work, P4's
delay/units/glitch/STA, P2's additive half, and P12's floor once #77 lands.
**The dependency-critical path is materially shorter than the total, and roughly
the same length as it was before the amendment.**

At a sustained half-time, 66–98 maintainer-months is eleven to sixteen calendar
years. **This is emphatically not a plan to finish, and it was never one.** It is
a spine along which to choose, and the staging is built so that stopping after
any stage leaves a shippable tree with something new in it.

### What JLS becomes if the whole thing lands

Everything the parent document's closing paragraph describes — X on a floating
input, a bus that goes red and spreads, an arbitrating I²C bus, a carry chain
that lights up and then shortens, a five-peripheral Wishbone system, HDL that
re-imports to the circuit it came from with CI proving it, cells on sky130 with
polygons that light up when you click a NAND — **and then**:

The student types `32` into one adder drawing instead of hand-copying a 1-bit
cell eight times, expands the `Array` in place, and counts what they actually
built. They run a real program on the CPU they drew, in the GUI, at watchable
speed, with the register display updating — and then run the same circuit under
both engines and see the two places the models disagree, which is a lecture no
other tool can give. When the answer is wrong they right-click the bad bit,
choose *why?*, and walk the cone back to the wire they crossed — or press *step
back* and watch the wavefront retreat into its cause. They circle a wire, say
"stuck at 0", and watch a coverage number move. They cross a button into a
clocked design, see the crossing flagged red on the wire they drew, run it a
hundred times with a hundred seeds and count the eighty-seven that passed, insert
a synchronizer, and count a hundred. They submit, and get back *proved equivalent
for all 512 inputs* — or a minimized counterexample already loaded and already
highlighted on their own drawing, with the note that their circuit uses three
fewer gates because they exploited a don't-care the way they were taught to.
Two of them branch a repo, build the datapath and the control separately, and
`git merge` combines them — or names the exact element they disagree about, on
the drawing, in a file that opens in stock JLS. And an instructor generates
thirty different-but-equivalent problems from a sixty-line script and grades
them with the same script run backwards.

The tool that does all of that is still one jar, still offline, still gradeable
from a shell script.

### What JLS becomes if only the first stage lands

Stage 0 + 0a + 1 + 1a + 2, with the formal floor: roughly **22–31 weeks — five to
seven months.** At the end of it:

- Batch runs finish noticeably sooner and setup cost is gone (Stage 0).
- `.jls` files are diffable text in git rather than opaque XZ blobs, and
  `-b -savetext` no longer silently does nothing (Stage 0a).
- **The flagship CPU exports.** `Memory`, `ShiftRegister` and `SubCircuit` leave
  the reject bucket, and the shared elaborator exists for six later consumers
  (Stage 1).
- There is one report channel and one exit-status lattice, designed once, that
  four later programs will not have to reopen (Stage 1a).
- Assertions are marked red on the drawing; there is a coverage report and xUnit
  XML that CI and every LMS autograder already ingests (Stage 2).
- **And grading is by proof.** An instructor writes one line in a marking
  script and a student gets back either a proof for every input or the single
  input that breaks their circuit, loaded and ready to step.

That last item alone is a different product from the one in the tree — and it is
the only item on the list that no other tool in JLS's class can match at any
price. Five to seven months buys JLS a claim it does not currently have the right
to make, and it buys it before the keystone's silent stretch has even begun.
