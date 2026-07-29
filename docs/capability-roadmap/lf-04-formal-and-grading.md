## Formal equivalence checking, and autograding by proof rather than by vectors

*Leapfrog pass 04. Every JLS claim is anchored to a path in the tree at HEAD.
External claims are marked **[verified]** (fetched in this pass) or
**[unverified]** (recalled; must be checked before anything is built on it).*

---

### What is missing today

**JLS has no representation of "correct."** It has a representation of "what
happened," and grading is a string diff over that.

The batch surface is complete and well-specified, and it is entirely one-way.
`docs/batch-interface.md` §2.2 gives the whole `-t` grammar:

```
file    ::= { signal }
signal  ::= name initial { step } "end"
step    ::= ( "for" duration | "until" time ) value
initial ::= value
```

Four productions, and **not one of them mentions an output.** `-t` drives
top-level `InputPin`s and nothing else; `SigSim.initSim`
(`src/jls/elem/SigSim.java:40-120`) parses the file and calls
`sim.post(new SimEvent(t, pin, value))` for every value *at parse time*, before
time 0. There is no expectation side, no comparison, no verdict. The circuit's
answers come out of `JLSStart.displayResults` as formatted text —
`Output Pin QUAL.name: 0xH (U unsigned, S signed)` — over a three-type whitelist
(`Register`, `Memory`, `OutputPin`, `docs/batch-interface.md` §3.2), and the
exit-status contract (§1, lines 36-40) has exactly three values: 0 run completed,
1 runtime failure, 2 usage error. **There is no exit status meaning "the run
completed and the answer was wrong."**

So the comparison happens outside the tool, in a script, against bytes. The
repository ships the canonical example of this and documents it as the supported
pattern:

```python
# examples/autograde/autograde.py
EXPECTED_STDOUT_LINES = [
    "Output Pin ar: 0xED (237 unsigned, -19 signed)",
    "Output Pin ll: 0xD4 (212 unsigned, -44 signed)",
    "Output Pin lr: 0x2D (45 unsigned, 45 signed)",
]
```

That is the grading criterion for the shipped example: **three literal lines of a
report format.** It is an assertion about a text encoding, written outside the
circuit, about one input vector (181 shifted by 2), pinned in CI by
`test/jls/AutogradeBridgeExampleTest.java`. A submission that is wrong on 254 of
the 256 possible inputs and right on that one passes.

The workarounds are the evidence, and there are five of them in the tree:

1. **`examples/autograde/autograde.py`'s `EXPECTED_STDOUT_LINES` /
   `EXPECTED_FINALS`** — the criterion is bytes, and the sample is one point.
2. **`riscv/verify.py:compare`** builds a `problems` list by diffing register and
   memory dictionaries against a reference emulator. A scoreboard, in Python,
   because the model has nowhere to put one.
3. **`riscv/fuzz_diff.py:rand_program`** is a stimulus generator with hand-tuned
   weights `[30, 30, 12, 8, 4, 8, 8]` and a comment reading *"No control flow
   (covered by the directed suite)"* — a **coverage argument written in a code
   comment**, unmeasured. This is the shape of every sampling-based grader: you
   argue about the distribution because you cannot argue about the domain.
4. **`docs/standards-adoption/05-riscv-compliance.md` step 3** builds the halt
   condition out of a magic-address comparator feeding `Stop`
   (`src/jls/elem/Stop.java:147-161`) — a property assembled out of gates.
5. **`TruthTable.react` on an unmatched row** (`src/jls/elem/TruthTable.java:1432`)
   silently holds its outputs and warns nobody, so an under-specified reference
   *silently* becomes a latch. Grading against it is grading against a bug.

And there is a sixth absence that matters more than any of them, because it is
the one that makes vector grading actively *wrong* rather than merely weak.
`TruthTable` stores three symbols per cell — `0`, `1`, and `2` meaning don't-care
(`src/jls/elem/TruthTable.java:79-80`). On the input side the `2` is honoured:
`react` skips that column when matching (`:1413`). On the **output** side it is
destroyed:

```java
// src/jls/elem/TruthTable.java:1446-1449
int outValue = table[matchingRow][pos+offset];

// don't care becomes false
if (outValue == 2)
    outValue = 0;
```

An instructor who writes a reference truth table with don't-care outputs — the
normal way to specify a decoder, a BCD seven-segment driver, a priority arbiter —
gets a reference that produces **0**, and any student who exploits the don't-care
to build a smaller circuit is marked wrong by the vector grader. JLS today
punishes the exact optimisation the Karnaugh-map lecture two weeks earlier taught
them to make.

There is no `jls.formal`. `src/jls/` contains `collab core edit elem hdl images
module sim tutorial util`. Grepping the whole tree for `sat4j|SMT|AIGER|btor|
equivalence check` returns survey rows in `docs/standards-landscape.md:230-231`
(#63 SMT-LIB, #64 AIGER/BTOR2, both marked ADJACENT) and nothing else. The tool
has never had a proof obligation of any kind.

**What it does have, and this is the finding that sizes the whole capability:**
`src/jls/hdl/HdlModel.java` is already a language-neutral formula-shaped IR, and
`HdlModel.StatementVisitor` (`:148-193`) has exactly ten `visit` methods —
`GateStatement`, `ReplicateStatement`, `ConstantStatement`, `TriStateStatement`,
`AdderStatement`, `RegisterStatement`, `BitMapStatement`, `SelectStatement`,
`PriorityCaseStatement`, `StateMachineStatement`. Two classes implement it today
(`VerilogEmitter`, `VhdlEmitter`). **A formula extractor is a third
implementation of an interface that already exists and is already double-dispatch
clean.** `HdlExporter.buildModel` (`:166`) already does the port walk, the
wire-net union-find, the jump fusion and the identifier legalization a formula
printer needs. The proofs directory (`proofs/README.md`,
`SpatialIndexCorrectness.agda`, checked in CI's `proofs` job with "no postulates
and no holes") establishes that machine-checked claims are already part of this
project's culture. The pieces are laid out; nobody has connected them.

---

### The capability

**Given a reference circuit R and a submission circuit S, decide whether they
compute the same function — for every input, not for a sample — and when they do
not, produce the input that distinguishes them, rendered on the student's own
drawing and replayable through the existing `-t` interface.**

#### The architecture: one AIG, four printers, one solver

Do not translate the element graph to CNF. Translate it to an **and-inverter
graph**, and make CNF one of four things printed from the AIG. The pipeline:

```
Circuit ──HdlExporter.buildModel──▶ HdlModel  (10 statement kinds, exists today)
                                       │
                          FormulaBuilder implements StatementVisitor
                                       │  bit-blast, constant-fold, structural hash
                                       ▼
                                  jls.formal.Aig
                        ┌────────────┬───┴────┬─────────────┐
                    Tseitin       AIGER    BTOR2        SMT-LIB
                    (CNF)         (#64)    (#64)         (#63)
                        │             └────────┴─────────────┘
                   in-tree CDCL          external solvers via ToolLocator
```

The AIG as the internal IR rather than CNF-direct is the load-bearing choice.
Structural hashing on an AIG collapses the enormous redundancy in student
circuits for free (a hand-drawn 4-bit adder is eight instances of the same
three-gate pattern), it makes AIGER a dump rather than a translation, and it is
the representation every industrial equivalence checker uses. Tseitin from an AIG
is thirty lines: three clauses per AND node, one unit clause for the output.

#### Which elements are easy, which are hard

**Free — already a bit-vector expression in `HdlModel`:**

| Element | Statement kind | Encoding | Cost |
|---|---|---|---|
| `AndGate`/`OrGate`/`NandGate`/`NorGate`/`XorGate`/`NotGate` | `GateStatement` | direct AIG nodes | 1–3 nodes/bit |
| `Constant` | `ConstantStatement` | folded away | 0 |
| `Splitter`/`Binder` | `BitMapStatement` | pure rewiring — **no nodes at all** | 0 |
| `Extend` | `ReplicateStatement` | fan-out of one literal | 0 |
| `Mux` | `SelectStatement` | one-hot decode + AND/OR tree | ~3·n·2^s |
| `Decoder` | `SelectStatement` | AND of selector literals per line | ~n·2^n |
| `Adder` | `AdderStatement` | ripple carry chain, or a bit-vector `bvadd` term in SMT-LIB | 5 nodes/bit |
| `TruthTable` | `PriorityCaseStatement` | first-match-wins priority chain | rows × cols |
| `InputPin`/`OutputPin`/`Clock` | ports | free variables / observed outputs | 0 |

`Splitter`/`Binder` costing **zero** is worth stating aloud: bit-level
connectivity is where a schematic tool is structurally *better* off than an HDL
front end, because the wiring is already explicit and does not have to be
inferred.

**Two subtleties in the "easy" column that will bite if unhandled.**
`TruthTable` is a *priority* structure, not a function: `react` breaks at the
first matching row (`:1408-1430`), so overlapping rows mean the encoding must be
an if-then-else chain in row order, not a sum of products. And `TruthTable` on
*no* matching row holds its previous outputs (`:1432`) — which makes it a latch,
which makes a combinational reference secretly sequential. **The formal path must
refuse an incomplete `TruthTable` used as a reference, with a message naming the
uncovered input patterns** — and computing that set is itself a two-line SAT
query. This is one of the places where the feature makes the tool better *before
anyone grades anything*.

**Medium — state elements, handled by cutting rather than by unrolling:**

`Register` (`RegisterStatement`, with its `Kind` enum at `HdlModel.java:399`) and
`StateMachine` (`StateMachineStatement`, `:720`, whose `StateCase`/`Condition`
records already carry the transition structure). See "sequential" below.

**Hard — and each needs a stated policy, not a heroic encoding:**

- **`Memory`.** Not merely hard: it is **not in the exporter's `EXPORTED` set at
  all** (`src/jls/hdl/HdlExporter.java:418-424`) and is rejected by name, pinned
  as intended behaviour by `HdlPolicyTest.memoryIsRejectedByName`. Policy: a ROM
  becomes a `SelectStatement`-shaped ITE chain (cheap up to a few thousand words,
  and every teaching ROM is); a RAM becomes an SMT-LIB array term (`QF_ABV`) on
  the external-solver path only, and is **refused** on the bit-blasting path with
  an explicit message, because a 2^32-word RAM has no CNF. Do not pretend
  otherwise. This is the single biggest limit on what the feature can grade, and
  it is honest to say so on day one.
- **`SubCircuit`.** Also rejected by the exporter today
  (`HdlPolicyTest.subCircuitIsRejectedCleanly`), and `SubCircuit.save`
  (`src/jls/elem/SubCircuit.java:282-288`) writes the nested circuit **inline**,
  so instances have no shared identity. For the formal path this is *easy*, and
  differently easy from P3's problem: formal has no readability requirement, so
  **flatten**. A recursive elaboration with a depth guard and name qualification
  is a week, and it does not wait on P3's instance IR.
- **`TriState` and multi-driver nets.** This is the genuinely awkward one, and
  the awkwardness is a fact about JLS's current semantics rather than about SAT.
  `docs/simulation-semantics.md` §9 and `WireNet.propagate`
  (`src/jls/elem/WireNet.java:454-485`) resolve a conflict to *"the first active
  driver in net order,"* where net order is a breadth-first walk from the first
  wire end **in file order**. That is not a function of the drivers' values; it is
  a function of the order the student drew the wires. **A file-order-dependent
  resolution is not expressible as a formula**, and any encoding of it would be
  proving a theorem about the save file rather than about the circuit. Policy,
  pre-P1: the formal path performs a structural check and **refuses** any net
  with more than one potentially-active driver, exit 5, message naming the net.
  Post-P1: the resolution function becomes a real fold and *is* expressible —
  which makes P1 an enabler for formal, not just for pedagogy.
- **Combinational loops.** JLS has no loop detection at all (P4 notes this). A
  combinational cycle has no fixed point to extract. The formal path must detect
  cycles in the elaborated AIG-building walk and exit 5. This falls out of the
  topological order for free.
- **Delay.** The whole formal path is **untimed**. `propDelay` is discarded;
  `Adder.resetPropDelay`'s `propDelay = bits * defaultPropDelay`
  (`src/jls/elem/Adder.java:261`) has no formal meaning. That is sound for the
  settled value of a loop-free circuit and unsound for anything else, which is why
  loop detection is a precondition and not a nicety. Say this in the normative
  document, once, plainly: **JLS proves what the circuit settles to, not when.**

#### What P1's multi-value domain does to the encoding

Sweep 04 G calls today's two-state domain "an asset" for formal, and it is —
today one propositional variable per bit and no more. P1 changes that, in three
increasing degrees of pain:

1. **X and U (P1-S2/S4).** Dual-rail: two variables per bit, `(a, b)`, exactly
   matching keystone A's recommended `record Word(int width, long a, long b, long
   u)` — the encoding the value type will already carry. Gate tables become
   three-valued; formula size roughly doubles; the ratio of AIG nodes to bits
   stays constant. Tractable, and the plane layout means the extractor reads the
   value type's own representation rather than translating out of it.
2. **The definition of equivalence changes**, and this is more important than the
   size. With X in the domain there are three defensible relations and they grade
   differently:
   - **strict** — R and S agree bit-for-bit including X. Too strong: a student
     whose circuit is *more* defined than the reference fails.
   - **refinement (recommended default)** — wherever R produces X, S may produce
     anything; elsewhere they must agree. This is exactly synthesis's
     don't-care semantics and exactly what a grader wants.
   - **two-state projection** — X mapped to 0 before comparison. This is what
     JLS does today by accident, and it is the behaviour the whole P1 program
     exists to stop; do not carry it forward as a default.
3. **Strength (P1-S3).** Do **not** encode. A resolution over `(strength0,
   strength1)` pairs is a lattice fold with eight levels per Verilog; encoding it
   is possible and pointless for grading. Policy: formal mode projects to
   `{0,1,X,Z}` and refuses designs whose behaviour depends on a non-default
   strength pair, naming them.

And one thing P1 *gives* formal that it does not have today: `Bits4`, the
separate specification type carrying `-`. Without it the reference cannot express
a don't-care output at all, because `TruthTable.react:1447-1449` destroys it. See
the don't-care section under grading — this is the single hardest dependency in
the whole capability.

#### The solver question — recommendation, not a survey

Three options, and the project's own written rules decide between them faster
than any technical argument.

**Option A, bundle Sat4j.** Pure Java, dual EPL/LGPL **[verified: dual EPL + GNU
LGPL; the exact LGPL *version* was not verified and must be read from the source
headers before shipping]**. `docs/library-survey-2026-07.md:19-25` rule 1 says
"Apache-2.0, MIT, BSD, LGPL, and GPLv3 itself are fine; plain EPL-2.0 … is not,"
so the LGPL arm is taken and the licence question closes cleanly. But rule 4
(`:38-45`, adopted 2026-07-17) says: *"projects in declared 'maintenance mode' or
with dormant release histories are rejected regardless of technical fit."*
Sat4j's newest published release appears to be **2.3.6**, with a 3.0 line
described as in development **[unverified: release dates and current activity not
confirmed in this pass — check the OW2 GitLab before relying on this]**. On the
face of it, **Sat4j fails the project's own rule 4, not rule 1.** That is a
surprising result and it should be checked rather than assumed, but it cannot be
waved past.

**Option B, external solver only.** Print DIMACS/SMT-LIB/AIGER and shell out,
using the `ToolLocator` + `Assumptions.assumeTrue` skip-when-absent pattern
`test/jls/hdl/IverilogCompileTest.java:32-34` already establishes, and the
subprocess boundary `ARCHITECTURE.md:314-317` explicitly endorses precisely
because it *"sidesteps GPLv3 in-process-linking hazards."* Perfect fit for the
delegation stance — and **fatal for grading**, because an autograder that
requires the instructor to install Yosys or Z3 on the marking machine is not the
same product as `java -jar jls.jar`. `library-survey-2026-07.md:31-37` rule 2:
*"The self-contained jar is the product."*

**Option C, and the recommendation: write the solver, in `jls.formal`, and keep
the printers as the escape hatch.** A CDCL solver of the MiniSat class — watched
literals, VSIDS, 1UIP conflict analysis, Luby restarts, clause deletion — is
roughly 900–1200 lines of Java that has been written thousands of times and is
described completely in the published literature. The queries the grading path
generates are small: a 4-bit adder miter is a few hundred clauses; an 8×8 array
multiplier miter is a few thousand; a 32-bit ALU miter is tens of thousands, all
of which a competent CDCL solves in milliseconds. It removes the dependency
question entirely, it keeps the jar self-contained, and — not incidentally for
this project — it is itself a teaching artifact of the kind `proofs/` already
signals an appetite for.

The obvious objection is correctness, and it has an unusually clean answer,
because the two answers a SAT solver gives have opposite risk profiles:

- **SAT (not equivalent) is self-checking, for free, using JLS itself.** Take the
  model, write it as a `-t` file, run it through `BatchSimulator` on both
  circuits, and confirm the outputs actually differ. If they do not, the extractor
  or the solver is wrong and the run must abort rather than fail the student.
  Every counterexample JLS reports is therefore a counterexample JLS has
  *simulated*. This also means the counterexample artefact is produced by the
  verification step rather than in addition to it.
- **UNSAT (equivalent) is the dangerous answer** — it is the one that says "your
  circuit is correct." Require the solver to emit a **DRAT proof log** and check
  it, either with a small in-tree checker or with `drat-trim` when present via
  `ToolLocator`. The trusted computing base for a passing grade then shrinks to
  the proof checker plus the extractor.

The extractor itself is covered by a third mechanism the tree already supports:
a differential fuzz test that generates random small circuits, exhaustively
simulates them through `BatchSimulator`, and compares against the AIG's own
evaluation. That is `GenerativeRoundTripFuzzTest`'s pattern applied to semantics
instead of persistence.

So: **in-tree CDCL as the default; DIMACS/SMT-LIB/AIGER/BTOR2 printers as a
first-class escape hatch for anything the in-tree solver times out on, or for a
student who wants to see the real tools.** The escape hatch is not a consolation
prize — it is how a class gets from "JLS proved my adder" to "Yosys and ABC
proved my adder," which is a lesson in itself.

#### Sequential equivalence: three tiers, in order

1. **Register-boundary matching (the default, and the one to build first).** If R
   and S declare registers with matching names and widths — which they will,
   because the assignment specified the state encoding — cut both circuits at the
   register boundaries and prove two *combinational* obligations: next-state
   functions equal, and output functions equal, given equal current state. This
   is exactly the key-point matching that industrial LEC tools do, it reduces to
   the combinational engine with no new machinery, and it proves equivalence for
   **all** reachable and unreachable states, unboundedly. For teaching it is the
   right default because the state encoding is usually part of the specification.
2. **Bounded model checking on the product machine.** When the encodings differ,
   unroll R and S k steps from a common reset, constrain inputs equal, assert
   outputs equal. Finds bugs fast; never proves. Report it as
   `NO_COUNTEREXAMPLE_WITHIN_K`, **never** as `EQUIVALENT`. This distinction is
   the entire integrity of the feature.
3. **Unbounded sequential equivalence** — induction, PDR/IC3, reachability.
   **Delegate. Always.** Print BTOR2 (#64) and hand it to `btormc` or ABC. The
   capability roadmap already draws this line at §6(b) ("unbounded liveness …
   needs a model checker in the loop") and it should be drawn here too, for the
   same reason: a model checker is a research programme, not a feature.

#### Counterexample rendering

The counterexample is the product. Three renderings, all from the same verified
model:

- **A `-t` file.** `docs/batch-interface.md` §2 is a frozen contract (§6), so a
  counterexample written as `-t` is guaranteed loadable, replayable, diffable and
  distributable forever. For a combinational counterexample it is one line per
  input pin. For a k-step BMC trace it is k values per pin plus a generated
  clock — which is, incidentally, exactly the scaffolding `riscv/verify.py:gen_clock`
  builds by hand today.
- **On the schematic.** Load the `-t`, run to settle, and annotate: failing output
  pins in red, the differing value beside each, and the cone of logic feeding them
  highlighted. The cone is a backward reachability walk over the same net
  structure `HdlExporter.buildModel` already unions, so it costs nothing extra.
- **Minimized.** A raw model assigns every input; most of them are irrelevant.
  Re-solve with each input's literal successively released and keep the releases
  that stay SAT — a linear number of tiny queries — and report *"the failure needs
  only `cin=1, a[3]=1`; the other seven inputs are free."* The difference between
  "here is one of 512 failing inputs" and "here is the pattern that breaks it" is
  the difference between a diff and a diagnosis. This is where the feature earns
  its teaching value, and it is about a hundred lines.

#### The batch contract

Additive, exactly as `docs/batch-interface.md` §6 blesses. New flags on
`JLSStart.FLAGS` (`src/jls/JLSStart.java:759-789`, 14 entries today):

```
-equiv <reference.jls>    prove the operand circuit equivalent to the reference
-map <file>               port correspondence (default: match by name)
-assume <file|circuit>    care-set / input constraint
-cex <file>               write any counterexample as a -t test-vector file
-formal-report <file>     xUnit XML result (P5 change H's artifact shape)
-bmc <k>                  bounded depth when register matching is unavailable
```

Exit statuses, extending §1's 0/1/2 and P5's proposed 3:

| status | meaning |
|---|---|
| 0 | **proved equivalent** (with a DRAT-checked proof) |
| 3 | **counterexample found** (simulated and confirmed; written to `-cex`) |
| 4 | **unknown** — solver timeout, resource limit, or BMC exhausted at depth k |
| 5 | **not checkable** — `Memory` on the bit-blasting path, multi-driver net, combinational loop, port mismatch, incomplete reference `TruthTable` |

**4 and 5 are never passes.** A run without any of the new flags returns 0/1/2
and prints the same bytes as today, so `BatchSimulationGoldenTest` and
`VcdExportGoldenTest` stay byte-identical and no conforming consumer can observe
a change. That non-movement is the compatibility proof.

---

### What it unlocks

**Standards.**

- **#63 SMT-LIB 2.6** — from ADJACENT to shipped. Two uses, and they are
  different: bit-vector `QF_BV` as the escape hatch for large queries, and
  `QF_ABV` array theory as the *only* honest way to reason about a RAM without
  bit-blasting it. The second is the one that decides whether memory-bearing
  designs are gradeable at all.
- **#64 AIGER / BTOR2** — from ADJACENT to shipped, and AIGER is nearly free once
  the AIG is the IR (its alphabet is literally AND, NOT and latches, which is
  JLS's element vocabulary). BTOR2 is the delegation path for tier-3 sequential.
- **#49 SVA / #50 PSL** — the formal path makes an emitted assertion *checkable*
  rather than merely simulatable. An `Assert` element (P5 change B) emitted as an
  SVA `assert property` and pushed through SymbiYosys is proof, not sampling.
- **#53 UCIS** — indirectly, and in a way worth naming: once you can prove a
  property you can also compute the **uncovered set** — the input patterns no
  test vector reaches — as a SAT query. That is coverage computed exactly rather
  than counted approximately, which no coverage tool does.
- **#65 RISCOF** — materially cheaper. Its whole job is "run many programs and
  diff signatures"; register-boundary equivalence against a reference decode
  table replaces a chunk of that with a proof.

**Engineering.**

- **A regression oracle for JLS itself.** `ARCHITECTURE.md:359-372` makes any
  future compiled/levelized simulation strategy carry a binding equivalence
  criterion — *"it must be observably identical to the event model"* — and today
  the only oracle is the #202 RV32I golden run plus fuzzing. With a formula
  extractor, the criterion becomes checkable by construction on the combinational
  subset. The capability aimed at grading students turns out to be the tool for
  grading the maintainer's own biggest planned refactor.
- **Incomplete-specification detection.** "Which input patterns does this
  `TruthTable` not cover" and "can these two `TriState` enables ever both be
  high" are one SAT query each, and both are today either silent
  (`TruthTable.react:1432`) or a one-shot dialog (`WireNet.java:472-483`).
- **Redundancy and constant detection.** "Is this output constant?" "Is this gate
  input observable?" "Are these two nets always equal?" — all one query. Dead-logic
  reporting is a synthesis feature students never see.
- **Test-vector *generation* by proof.** Given a reference, generate a minimal
  input set distinguishing it from a family of plausible mutations. An instructor's
  vector file stops being hand-written folklore.

**Teaching — what a student can do afterwards that they cannot today.**

- **Submit and get an answer that means something.** Today: "your circuit matched
  20 of 20 vectors." After: *"proved equivalent for all 512 inputs"* or *"differs
  at a=0110, b=1011, cin=1: reference says sum=0001 carry=1, yours says sum=0001
  carry=0 — here is the vector, loaded; step it."* The counterexample is
  **constructed for the student's specific mistake**, which is categorically more
  instructive than a vector they happened to stumble into.
- **Be rewarded for exploiting a don't-care instead of punished for it.** See
  below; this is the single largest teaching change in the capability.
- **Meet "exhaustive" as a real word.** First-years are taught that testing
  cannot prove absence of bugs and then given only testing. Being handed a tool
  that says *proved, for all inputs* — and understanding why that is a different
  kind of sentence — is a genuine conceptual event, and it lands in week three
  rather than in a fourth-year formal-methods elective.
- **See the state-space explosion as a fact rather than a warning.** "Your 4-bit
  comparator proved in 3 ms. Your 16-bit multiplier did not finish in 60 s. Here
  is why." Complexity becomes something the student *hits*, which is the only way
  anyone ever believes it.
- **Learn that a specification is a separate artifact.** A reference `TruthTable`
  or `StateMachine` next to an implementation, with a machine deciding whether
  they agree, teaches the implementation/specification split more effectively than
  any lecture — and it is the same lesson P5's `Assert` element teaches from the
  other side.
- **Debug by proof.** "Prove my decoder is one-hot." "Prove these two enables are
  never both high." "Prove this FSM cannot reach the error state." Questions a
  student can ask *of their own design*, on their own, with no instructor in the
  loop.

---

### Competitive position

**Commercial.** Cadence Conformal LEC and Synopsys Formality are the incumbents;
Siemens (via the OneSpin acquisition) and Questa Formal cover the assertion side.
They are excellent at what they do and structurally unable to do this. Three
reasons, all architectural rather than commercial:

1. They check **RTL against a gate netlist** inside a synthesis flow. They assume
   you have RTL and a synthesized netlist. A first-year student has neither; they
   have a drawing.
2. Their output is a report and a waveform. **None of them owns a schematic
   editor**, so none can render a counterexample onto the thing the user drew.
3. Their user model presumes a verification engineer who already knows what a
   key point and a compare point are.

Pricing is the usual argument and I will not lean on it, because I cannot verify
it: seat costs for Conformal and Formality are **[unverified — not published;
under NDA in practice]**. The structural argument is stronger anyway and does not
depend on a number.

**Open.** This is where the real prior art is, and it is genuinely good.

- **Yosys** ships `equiv_make`, `equiv_simple`, `equiv_status`, `equiv_miter`,
  `miter -equiv` and `sat -verify` **[verified: YosysHQ command reference]**.
- **EQY** ("Equivalence checking with Yosys") is a dedicated front-end driver,
  ISC-style licence, shipped in the free OSS CAD Suite **[verified: YosysHQ EQY
  docs and github.com/YosysHQ/eqy]**.
- **ABC** provides `cec` and `dsec`; **SymbiYosys**/`btormc` cover bounded and
  unbounded model checking **[recalled, consistent with the above; individual
  command names unverified in this pass]**.

So the *engines* are solved, free, and better than anything JLS would write. **JLS
should not compete with them and the design above does not — it prints to them.**
What none of them has is the last mile: every one is a command-line tool over a
netlist, whose counterexample output is a table of signal names or a VCD. There
is no drawing to put it on. Turning `eqy`'s output into something a first-year
can act on is an unsolved interface problem that the incumbents are not trying to
solve because their users are not first-years.

**Peer educational.** This is the clean part of the assessment.

- **hneemann's Digital** — README lists "Simple testing of circuits: you can
  create test cases and execute them" and "Analysis and synthesis of
  combinatorial and sequential circuits," including deriving a truth table from a
  circuit. Formal verification, SAT and equivalence checking are **not
  mentioned** **[verified: README fetched this pass]**. Note the near-miss:
  circuit → truth table *is* exhaustive equivalence for small combinational
  circuits, done by enumeration. It is the right idea stopping at 2^20.
- **Logisim-evolution** — README lists designer, simulation, chronogram, board
  integration, VHDL components, TCL console, component library. No formal, no SAT,
  no equivalence **[verified: README fetched this pass]**. It has CSV test vectors
  via `--test-vector` **[verified second-hand via
  `docs/hdl-support-research.md:425`]** — i.e. exactly the sampling surface JLS
  already has in `-t`.
- **DigitalJS** — a browser simulator over Yosys-synthesized netlists. Whether it
  exposes any of Yosys's formal commands is **[unverified]**; its published
  framing is simulation and visualization.

**Verdict: LEAPFROG, and an unusually clean one.** The three conditions for a
real leapfrog all hold. The technique is fifty years old and completely
documented, so there is no research risk. The solvers are free, so there is no
cost barrier. And the actual gap — a schematic-native, student-legible
counterexample wired into a grading contract — is one nobody is trying to close,
because commercial tools serve verification engineers, open tools serve netlists,
and educational tools have stopped at test vectors.

Concretely, **JLS's version would be the only tool in any of the three
categories where a student draws a circuit, presses one button, and is told
either "proved correct for all 512 inputs" or "here is the input that breaks it,"
with the failing input already loaded and the failing cone highlighted on their
own drawing.** Not because JLS out-solves anyone, but because it is the only one
holding both ends: the drawing and the proof.

**Where JLS cannot lead, stated plainly.** It will never out-perform ABC or
Yosys on scale, and should not try; the in-tree solver is for classroom-sized
queries and the printers exist for everything else. Unbounded sequential
equivalence on non-matching encodings is genuinely hard and belongs delegated.
And the whole capability is untimed — it says nothing about setup, hold or the
critical path, which is P4's territory and stays there.

---

### Relationship to the existing programs

**Not a new program. This is P5's changes F and G, promoted from a 6–9-week line
item to the program's headline deliverable, plus one slice P5 does not currently
contain: the grading contract.** The roadmap's §2 P5 entry already lists
"in-tool differential/equivalence checking" and "formula export — SMT-LIB, AIGER,
BTOR2 as printers over the model `HdlExporter.buildModel` already constructs"
and names "the counterexample loop" as worth calling out separately. The analysis
above agrees with every one of those judgements. What it changes is the size and
the ordering.

**A dependency P5 does not name, and it is hard.** Sweep 04 says of P5 as a
whole: *"Dependencies. None. This program needs nothing from P1 … It is the one
program that can run start-to-finish in parallel with everything else."* That is
true of A, B, C, D, E and H. **It is not true of F and G.** The formula extractor
rides `HdlExporter.buildModel`, and therefore **inherits the exporter's reject
list verbatim** — `Memory`, `SubCircuit`, `ShiftRegister`
(`HdlExporter.java:418-424`, pinned by `HdlPolicyTest`). Without **P3's Stage 1
export coverage**, formal grading can prove things about gate-level combinational
circuits and nothing else. That is still a real capability — it covers the first
third of a first-year course — but the roadmap should say so rather than leave F
and G in the "parallel with everything" bucket.

Ordering, precisely:

```
P3 Stage 1 (export coverage: Memory, SubCircuit, ShiftRegister)
   │   ── OR, cheaper and available immediately: a formal-only flattening
   │      elaborator that does not need P3's instance IR, ~1 week
   ▼
FORMAL CORE ── AIG + Tseitin + CDCL + miter + -t counterexample + exit codes
   │            (independent of P1, P2, P4, P6)
   ├──▶ P5-B Assert ── proof of drawn properties, not just equivalence
   ├──▶ P5-D coverage ── the uncovered set computed exactly rather than counted
   ├──▶ P5-H report channel ── MUST be designed first; formal needs statuses 4/5
   │                            that H's single "3 = property failed" does not have
   ▼
P1-S5 (Bits4, the `-` specification type)
   ▼
DON'T-CARE-AWARE GRADING ── the case below
   ▼
P1-S2 (X producible) ── refinement-based equivalence
```

- **Depends on P5-H, strictly.** The exit-status contract must be designed once,
  and formal needs a richer verdict lattice than "property failed." If H ships
  with only status 3, it will have to be reopened. This is the one thing that
  must happen in the right order.
- **Depends on P3 Stage 1** for anything beyond gate circuits — or on a
  formal-only flattener, which is the cheap path and worth taking.
- **Depends on P1's `Bits4`** for don't-care grading, and gains refinement
  semantics from P1-S2. Does not need P1 for the basic capability.
- **Enables P2 indirectly**: every new element (`Multiplier`, `Comparator`,
  `Counter`, the register-file `Memory`) can be *proved* against its gate-level
  construction, which is exactly the lesson `CellValidator.ARITHMETIC_MESSAGE`
  currently offers as a workaround.
- **Independent of P4 and P6 entirely.** It is untimed and technology-free.

---

### The grading application, in detail

**What the instructor writes.** A reference circuit (`ref_adder4.jls`), and
either nothing else or a short mapping file. Then one line in the marking script:

```sh
jls -b -equiv ref_adder4.jls -cex cex.t -formal-report result.xml submission.jls
```

For assignments specified as a table or a state diagram, the reference *is* a
one-element circuit: a `TruthTable` or a `StateMachine` with pins. That must work
on day one, because otherwise the feature is only usable by instructors who
already possess a solution circuit — and "here is the truth table, build it from
NANDs" is the archetypal first-year assignment.

**What the student sees.** On success: `proved equivalent (512 input
combinations, 0.004 s)`. On failure, an annotated schematic and a table:

```
NOT EQUIVALENT — counterexample (minimized: 2 of 9 inputs constrained)

  input      a[3:0]=0110  b[3:0]=1011  cin=1     (7 other inputs free)
  output     reference          submission
    sum[3:0]   0010               0010    ok
    cout       1                  0       <-- differs

  counterexample written to cex.t
  replay:  jls -t cex.t submission.jls
```

and, in the GUI, `cout` outlined red with its cone of logic highlighted. The
student steps the loaded vector and watches their carry chain produce 0.

**The don't-care case — the interesting one, and it has three distinct shapes.**

*Shape 1: output don't-cares (specification incompleteness).* The reference
`TruthTable` has `-` in an output cell — a BCD-to-seven-segment decoder over
inputs 1010–1111, a decoder line for an unused opcode. This is the common case
and it is where vector grading is not merely weak but **wrong**: a student who
exploits the don't-care to save three gates produces a *better* circuit and gets
marked down. Today JLS cannot even express the reference, because
`TruthTable.react:1447-1449` turns `-` into 0 before anything sees it.

The formal answer is textbook: build a **care predicate** alongside the miter and
only assert output equality where care holds:

```
miter := OR over outputs of ( care_i(inputs) AND (R_i XOR S_i) )
```

UNSAT means *"equivalent everywhere the specification says anything."* A student
whose gate count is lower than the reference's, because they filled the
don't-care differently, **passes** — and the report says so explicitly:
*"equivalent on the care set; your circuit differs from the reference on 6
don't-care inputs, using 3 fewer gates."* That sentence is a whole lesson about
what a specification is, delivered by a marking script.

This is the hard dependency: `care_i` cannot be constructed until `Bits4` (P1's
specification type) lets `TruthTable` carry `-` through to `react`. Everything
else in the capability can ship before P1; this cannot.

*Shape 2: input don't-cares (unreachable stimulus).* The reference is only
meaningful under an assumption — a one-hot selector, a valid BCD digit, a
protocol precondition. Without it the checker finds "counterexamples" at inputs
that cannot occur, and the student is failed for a case the assignment never
mentioned. This is the classic false-positive that makes people distrust formal
tools, and it will be the most common support question. The answer is an
**assumption circuit**: `-assume valid.jls`, a circuit over the same inputs whose
single output must be 1, conjoined into the miter's antecedent. Drawing a
constraint is a good exercise in its own right — it is where a student first
writes down what they were assuming.

*Shape 3: sequential don't-cares (initialization and unreachable states).* R and
S agree in every reachable state and differ in states neither can enter, or differ
only during the reset window. Register-boundary matching (tier 1) proves
equivalence over *all* states including unreachable ones, so it will report a
counterexample in a state that cannot happen. This is a genuine limitation and
must be surfaced honestly: the report says *"differs only in state 1011, which may
be unreachable — reachability is not checked."* Fixing it properly needs a
reachability analysis, which is tier 3, which is delegated. Do not fake it.

**Composition with the existing batch interface.** The additive discipline is
already written: `docs/batch-interface.md` §6 blesses "a new optional output gated
behind a new flag," and `JLSStart.FLAGS` is the single authoritative table pinned
by `CliFlagTableTest`. Formal grading adds flags, adds a §8 to the batch document,
adds exit statuses 4 and 5, and changes not one byte of §2, §3 or §4. The proof of
that is `BatchSimulationGoldenTest.watchedElementsPrintInNameOrder` and
`VcdExportGoldenTest` staying green unchanged — the same non-disturbance argument
P5 change A makes for the timestamp hook.

**And the composition that matters most.** Formal equivalence and P5's coverage
model answer different halves of one question, and neither is sufficient:
coverage says *"did you exercise it"*, formal says *"is it right."* A grading
scheme that reports both — `proved equivalent for all inputs` **and** `your own
test vectors covered 4 of 9 truth-table rows` — teaches something no single number
does: that being right and having demonstrated you are right are separate
achievements. That is the strongest pedagogical argument in this document, and it
costs nothing extra once both exist.

---

### Size and risk

**20–30 maintainer-weeks for the whole capability, with a useful floor at 8–11.**

| Slice | Weeks | Notes |
|---|---|---|
| `FormulaBuilder implements StatementVisitor` + bit-blasting + structural hash + AIG | 3–4 | ten `visit` methods; the walk exists (`HdlExporter.buildModel:166`) |
| Tseitin → CNF, DIMACS printer | 0.5 | trivial from an AIG |
| In-tree CDCL solver + DRAT logging + proof checker | 4–5 | the risky slice; see below |
| Miter construction, port matching, care/assume sets | 2 | |
| Counterexample: model → `-t`, simulate-to-confirm, minimization | 1.5 | the confirm step reuses `BatchSimulator` |
| Formal-only flattening elaborator (`SubCircuit`, ROM) | 1 | avoids waiting on P3 |
| Uncheckability gate (multi-driver, comb loop, incomplete table, `Memory`) | 1.5 | this is what makes status 5 trustworthy |
| CLI flags, exit statuses, xUnit report, `batch-interface.md` §8, goldens, `CliFlagTableTest`/`CliSmokeTest` rows | 2 | a promise; design with P5-H, not after |
| GUI: counterexample overlay, cone highlight, one-button "check against reference" | 2–3 | the GUI half is the larger share, as with P5-B |
| Sequential tier 1 (register-boundary matching) | 2–3 | |
| Sequential tier 2 (k-step BMC unrolling) | 1.5–2 | |
| AIGER + BTOR2 + SMT-LIB printers, external-solver path via `ToolLocator` | 2–3 | #63, #64 |
| Don't-care-aware grading (**after P1's `Bits4`**) | 2–3 | |
| Instructor documentation, worked assignment templates, `examples/formal/` | 1–2 | without this it does not get used |

**The useful floor, 8–11 weeks:** extractor + AIG + CNF + CDCL + miter + `-equiv`
flag + `-t` counterexample + exit codes 0/3/4/5, combinational only, over gates /
`Mux` / `Decoder` / `TruthTable` / `Adder` / `Splitter` / `Binder` / `Extend` /
`Constant`. That alone converts autograding of every combinational assignment in a
first-year course from sampling to proof.

**A correction to sweep 04's own numbers.** It prices change F at 1–2 weeks and
change G at 3–4 (+2–3 for BTOR2 and sequential unrolling) — 6–9 total. That prices
*the printer*, and the printer is genuinely that cheap. It does not price the
solver decision, the miter, port matching, care sets, the uncheckability gate, the
GUI, the exit-status contract, or the licence/dependency question. The capability
is roughly three times the printer.

**The top three ways it goes wrong.**

1. **Unknown silently graded as pass.** A solver timeout, a resource limit, an
   unsupported element, a mis-matched port, a BMC that exhausted depth k — any of
   these produces "no counterexample found," and the natural code path returns
   that as success. Every student with a broken circuit and a big design passes.
   This is the failure that ends the feature's credibility permanently, and it is
   the *easy* failure to write. Mitigation is structural, not procedural: a result
   type with three constructors and no default — `PROVED(proofLog) |
   COUNTEREXAMPLE(model) | UNKNOWN(reason)` — distinct exit statuses 4 and 5, and
   a test suite of deliberately-uncheckable circuits (a RAM, a two-driver net, a
   combinational loop, an incomplete `TruthTable`, a 24-bit multiplier) whose
   assertion is the exit status. Write those tests before the solver.
2. **The miter is built wrong and proves the wrong theorem.** Ports matched by
   position when names differ; a width mismatch zero-extended silently; a
   reference and submission whose input pins are in different orders. An UNSAT on
   a mis-built miter is a false pass, which is failure mode 1 wearing a different
   hat. Mitigation: refuse on any ambiguity, require an explicit `-map` when names
   do not match exactly, and **print the matched port table in every report,
   including the passing ones**, so the instructor can see what was actually
   proved.
3. **Scope creep into being a model checker.** Unbounded sequential equivalence,
   reachability, induction, PDR, liveness. Each is individually reasonable and
   collectively they are a research programme that will consume the remaining
   roadmap. The line is already drawn in the roadmap's §6(b) and must be drawn
   here identically: **tier 3 is delegated via BTOR2 and never implemented.**

And a fourth, smaller but concrete: **the solver correctness/licence fork.** If
the in-tree CDCL is chosen and is subtly wrong, false UNSATs are false passes
(mitigated by DRAT checking and by the fact that every SAT answer is confirmed by
simulation); if Sat4j is chosen and rule 4 of `library-survey-2026-07.md` is
enforced later, the dependency has to come back out. Decide this deliberately,
record it in `ARCHITECTURE.md` as a decision with a revisit trigger, and do not
let it be decided by whichever is easier in week two.

**What would make it not worth doing.**

- **If the reference is harder to author than the vectors.** For "build this from
  the truth table," the reference is the table and authoring is free. For "build a
  4-bit ALU," the instructor must draw a correct ALU — which they probably have —
  but for anything where the specification is prose, formal grading demands an
  artifact vector grading did not. If `TruthTable` and `StateMachine` cannot serve
  as references on day one, the feature only serves instructors who already have
  solutions, and the effort is better spent on P5's coverage.
- **If the checkable subset does not cover what the course actually assigns.**
  Combinational gate circuits are weeks 1–6. If `Memory`, `SubCircuit` and
  multi-driver buses stay outside the subset, the feature grades the first third
  of the syllabus and stops. That is why the flattening elaborator and P3's export
  coverage are not optional extras — they are what decides whether this is a
  feature or a demo.
- **If it cannot be trusted.** A single publicised false pass is worse than not
  having the feature, because the instructor's fallback — vectors — is still
  there and is at least honestly weak. The DRAT log, the simulate-to-confirm step,
  the differential fuzz test against `BatchSimulator`, and the never-pass-on-
  unknown rule are not polish; they are the feature's licence to operate.

---

### Sources

**Repo paths, all read in this pass.**

- Grading as it exists: `examples/autograde/autograde.py` (`EXPECTED_FINALS`,
  `EXPECTED_STDOUT_LINES`, `parse_vcd_final_values`); `test/jls/AutogradeBridgeExampleTest.java`
  (named, not opened); `riscv/verify.py` (`compare`, `gen_clock`, `run_reference` —
  cited via `docs/capability-roadmap/sweep-04-verification.md`, not opened this pass);
  `riscv/fuzz_diff.py` (same).
- Batch contract: `docs/batch-interface.md` §1 (`:36-49`, exit statuses),
  §2.2 (`:81-99`, the `-t` grammar), §2.3 (`:102-116`), §3.2 (`:144-159`,
  the three-type whitelist), §3.4 (`:198-207`), §4.3 (`:294-304`),
  §6 (`:324-336`, the stability promise).
- Simulation semantics: `docs/simulation-semantics.md` §2 (`:42-67`, two states
  plus HiZ, null-as-HiZ, HiZ-reads-as-zero), §9 (`:409-446`, tri-state resolution
  and the file-order-dependent conflict rule).
- Elements: `src/jls/elem/TruthTable.java:79-80` (`int[][] table`, "0, 1 or 2
  (don't care)"), `:1408-1430` (first-match-wins), `:1432` (no-match holds
  outputs), `:1446-1449` ("don't care becomes false");
  `src/jls/elem/WireNet.java:443` (`propagate`), `:454-485` (first-active-driver
  resolution), `:472-483` (`TellUser.warn`);
  `src/jls/elem/Mux.java:519-548`; `src/jls/elem/Decoder.java:459-482`;
  `src/jls/elem/Stop.java:147-161`; `src/jls/elem/Adder.java:261`
  (`resetPropDelay`); `src/jls/elem/SigSim.java:40-120` (`initSim`, post-everything
  at parse time); `src/jls/elem/TestGen.java:19` (`extends SigSim`, batch-only);
  `src/jls/elem/SubCircuit.java:282-288` (inline nested save);
  `src/jls/elem/ElementRegistry.java:39-73` (33 registered types).
- HDL layer: `src/jls/hdl/HdlModel.java:28-33` (`Direction {INPUT, OUTPUT}`),
  `:113-135` (`Statement`), `:148-193` (`StatementVisitor`, ten `visit` methods),
  `:197,248,280,312,348,396,399,463,533,610,720,731,744,762` (the statement
  classes and `StateMachineStatement`'s records);
  `src/jls/hdl/HdlExporter.java:166` (`buildModel`), `:169` / `:1243-1253`
  (`orderedElements`), `:190` (the rejection message), `:418-424` (`EXPORTED`),
  `:427-429` (`SKIPPED`), `:431-433` (`TOPOLOGY`).
- Kernel and CLI: `src/jls/Circuit.java:479-485` (`getElementsInStableOrder`);
  `src/jls/JLSStart.java:759-789` (`FLAGS`, 14 entries).
- Policy: `ARCHITECTURE.md:314-317` (subprocess boundary for external tools,
  "sidesteps GPLv3 in-process-linking hazards"), `:344-372` (#221, the
  discrete-event-only decision and its binding equivalence criterion);
  `docs/library-survey-2026-07.md:17-25` (rule 1, licence: "Apache-2.0, MIT, BSD,
  LGPL, and GPLv3 itself are fine; plain EPL-2.0 … is not"), `:31-37` (rule 2, the
  self-contained jar), `:38-45` (rule 4, actively-maintained-only);
  `pom.xml:228-258` (the shade plugin; xz is the only bundled runtime dep).
- Formal-methods precedent: `proofs/README.md`, `proofs/SpatialIndexCorrectness.agda`,
  and the CI `proofs` job ("no postulates and no holes").
- External-tool pattern: `test/jls/hdl/IverilogCompileTest.java:32-34`
  (`Assumptions.assumeTrue(iverilog != null)`); `test/jls/hdl/ToolLocator.java:46,62,80`.
- Roadmap: `docs/capability-roadmap/README.md` §2 P5 (`:488-602`), §4 stage table
  (`:881-895`), §6(b) (`:1096-1099`, the unbounded-liveness line);
  `docs/capability-roadmap/sweep-04-verification.md` (change F `:404-424`,
  change G `:428-467`, change H `:471-500`, the #63/#64 rows `:62-63`, and the
  "Dependencies. None" claim at README `:598`).
- Survey rows: `docs/standards-landscape.md:230-231` (#63 SMT-LIB, #64
  AIGER/BTOR2, both ADJACENT); `docs/hdl-support-research.md:104-150` (Digital),
  `:151`, `:425` (Logisim-evolution `--test-vector`).

**External claims.**

- **[verified this pass]** Yosys ships equivalence-checking passes `equiv_make`,
  `equiv_simple`, `equiv_status`, `equiv_miter`, and documents `miter -equiv` as
  the way to build a miter — https://yosyshq.readthedocs.io/projects/yosys/en/stable/cmd/index_passes_equiv.html
- **[verified this pass]** EQY is a Yosys-based equivalence-checking front end,
  permissively licensed, shipped in the free OSS CAD Suite —
  https://github.com/YosysHQ/eqy and https://yosyshq.readthedocs.io/projects/eqy/en/latest/
- **[verified this pass]** hneemann's Digital advertises test cases and
  combinational/sequential analysis and synthesis; its README mentions no formal
  verification, SAT solving or equivalence checking —
  https://github.com/hneemann/Digital
- **[verified this pass]** Logisim-evolution's README advertises designer,
  simulation, chronogram, board integration, VHDL components, TCL console and a
  component library; no formal verification, SAT, or equivalence checking —
  https://github.com/logisim-evolution/logisim-evolution
- **[verified this pass, partially]** Sat4j is distributed under a **dual EPL /
  GNU LGPL** licence — https://www.sat4j.org/ (via search; the site itself
  returned 403 to direct fetch). **The LGPL *version* is unverified** and must be
  read from the source headers, since GPLv3 compatibility depends on it (LGPL
  2.1 §3 and LGPL 3 §2 both permit it, but this must be confirmed rather than
  assumed).
- **[unverified]** Sat4j's current release cadence. The newest release found was
  **2.3.6**, with a 3.0 line described as in development; no 2025–2026 release was
  located. Whether this constitutes a "dormant release history" under
  `library-survey-2026-07.md` rule 4 must be settled against the OW2 GitLab
  before Sat4j is adopted — https://mvnrepository.com/artifact/org.ow2.sat4j
- **[unverified]** Cadence Conformal LEC and Synopsys Formality seat pricing and
  academic-programme availability. Not published; the competitive argument in this
  document deliberately rests on the architectural claims (netlist-oriented, no
  schematic, verification-engineer user model) rather than on price.
- **[unverified]** DigitalJS's feature set with respect to formal verification.
- **[unverified, recalled]** ABC's `cec`/`dsec` command names; SymbiYosys and
  `btormc` as the unbounded model-checking back ends; the AIGER, BTOR2 and
  SMT-LIB 2.6 format details. All are small, stable and published, and all must be
  read before an emitter is written.
- **[unverified, recalled]** MiniSat-class CDCL implementation size and the DRAT
  proof format. The 900–1200-line figure is an estimate by analogy to published
  implementations, not a measurement.
