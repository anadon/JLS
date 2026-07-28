## Value domain: migration cost

*Keystone analysis, part B. What it would actually cost to replace
`@Nullable java.util.BitSet` with a per-bit multi-value type, counted
against the tree at HEAD (`df7072c`), with a staged plan in which the
tree is green after every stage.*

The five sibling sweeps in this directory establish **that** the value
domain is the keystone (sweep-01 names sixteen standards directly
blocked and twenty-four counting dependents; sweeps 02, 04, 05 and 06
each route their top item back through it). This document does not
re-argue that. It answers the next question: **what does it cost, where
exactly does the cost sit, and can it be done without the tree ever
going red.**

Headline: **17–22 maintainer-weeks** for the four-state core (sweep-01's
V1 + V7 + the resolution-function half of V2), done with a dual-mode
discipline that keeps every existing golden byte-identical until the
final stage. Sweep-01's estimate of 10–14 weeks for V1 is achievable
only as a big-bang branch with a long red period; the extra ~6 weeks
buys "green at every stage" and "no existing lab changes behaviour in
the release that ships it." Section 7 prices both.

The single most encouraging finding in the census: the migration is
**net line-count-negative in four places**. The whole-signal HiZ
encoding is reconstructed independently in four files, the
`TriStateOff`/`NewValue` payload fork is written out five times, and
`Splitter`/`Binder` each carry an all-or-nothing special case that a
per-bit type deletes outright. A per-bit value type is not only more
expressive than what is there, it is *smaller*.

---

### 1. The census

#### 1.1 Type surface

| measure | count | anchor |
|---|---|---|
| `BitSet` references in `src/` | **417**, across **51 files** | `grep -rn BitSet src/ --include=*.java` |
| `BitSet` references in `test/` | **134**, across **21 files** | same over `test/` |
| Total `src/` LOC | 80,215 | `find src -name '*.java' \| xargs wc -l` |
| `src/jls/elem/` LOC | 23,223 | the element package alone is 29 % of the tree |
| Files with ≥ 10 `BitSet` refs | 10 | `Memory` 51, `BitSetUtils` 35, `Register` 33, `BatchSimulator` 24, `Adder` 15, `edit/Trace` 13, `ShiftRegister`/`Mux`/`Decoder` 12, `WireNet` 10 |

Note the distribution: **`Memory.java` alone holds 12 % of all `BitSet`
references in the tree** (51 of 417). Memory is the single largest
element-level line item in this migration and should be scheduled
alone.

#### 1.2 Every `react` / `computeOutput` that consumes or produces values

`grep -rn "public void react" src/` returns **25** methods (24 concrete
elements plus the base). `grep -rn "protected BitSet computeOutput"`
returns **8** concrete implementations over the abstract declaration at
`src/jls/elem/Gate.java:663`. That is **33 value-computing methods**,
plus the two non-`react` resolution/transport sites
(`Output.propagate`, `src/jls/elem/Output.java:136`; `WireNet.propagate`,
`src/jls/elem/WireNet.java:442`).

**Mechanical — 22 methods.** A type swap; the four-state answer falls
out of the vector operations with no policy choice to make. Several get
*shorter*.

| method | anchor | note |
|---|---|---|
| `LogicElement.react` | `LogicElement.java:533` | base, throws `UnsupportedOperationException("no react")` |
| `Gate.react` | `Gate.java:695` | dispatch + `toBeValue` change check; needs only `LogicVector.equals` |
| `AndGate.computeOutput` | `AndGate.java:64` | `value.and(inVal)` → the standard 4-state AND table |
| `OrGate.computeOutput` | `OrGate.java:65` | |
| `NandGate.computeOutput` | `NandGate.java:66` | |
| `NorGate.computeOutput` | `NorGate.java:68` | |
| `XorGate.computeOutput` | `XorGate.java:71` | X-strict (no absorbing element); still no choice to make |
| `NotGate.computeOutput` | `NotGate.java:65` | |
| `DelayGate.computeOutput` | `DelayGate.java:126` | pure pass-through; the null branch just disappears |
| `Extend.computeOutput` / `Extend.react` | `Extend.java:170`, `:206` | 1-bit replicate; X in ⇒ all-X out |
| `Splitter.react` | `Splitter.java:204` | **deletes** the whole-value-null early return (`:210-215`); becomes a pure per-bit slice |
| `Binder.react` | `Binder.java:233` | **deletes** the `allOff` tracking (`:242-268`); becomes a pure per-bit compose |
| `JumpStart.react` | `JumpStart.java:479` | `TriStateOff`/`NewValue` fork (`:489-494`) collapses to one arm |
| `JumpEnd.react` | `JumpEnd.java:407` | same fork collapses (`:411-426`) |
| `InputPin.react` | `InputPin.java:198` | same fork collapses (`:202-217`) |
| `OutputPin.react` | `OutputPin.java:195` | same |
| `SubCircuit.react` | `SubCircuit.java:621` | same fork collapses (`:628-633`) |
| `Clock.react` | `Clock.java:404` | 1-bit toggle; a clock never sees an input |
| `Constant.react` | `Constant.java:474` | mask-to-net-width; and the in-place `newValue.and(mask)` at `:492` — which today **mutates the `BitSet` inside a `SimEvent.NewValue` record after the event was posted and hashed** — becomes impossible for free with an immutable value type |
| `Display.react` | `Display.java:387` | one assignment |
| `SigSim.react` | `SigSim.java:214` | schedule replay, no logic |

**Needs real thought — 8 methods.** A defensible answer exists, but a
human must choose it and write it into `docs/simulation-semantics.md`.

| method | anchor | the decision |
|---|---|---|
| `Adder.react` | `Adder.java:381` | The carry chain. `BitSetUtils.SumCarry` (`src/jls/BitSetUtils.java:196`) is a boolean ripple loop and must be rewritten as a 4-state full adder, or replaced by the cheap teaching rule "X from the lowest X bit upward". These give different answers; pick one and say so. |
| `Mux.react` | `Mux.java:519` | X selector: output X, or X-optimism (output the common value when all candidates agree)? Both are taught. Also the out-of-range branch (`:534-536`) that today yields a fresh zero. Should be a documented, switchable policy. |
| `Decoder.react` | `Decoder.java:459` | An X in the address makes an identifiable *subset* of outputs ambiguous and leaves the rest at 0. Computable, but a new algorithm. |
| `ShiftRegister.react` | `ShiftRegister.java:614` | Three separate rules: X shift amount ⇒ whole output X; X in data shifts through; arithmetic-right sign-fill from an X sign bit ⇒ X fill. |
| `Register.react` | `Register.java:747` | X on the clock must stop meaning "0, therefore never an edge" (which is at least not *wrong* today) and start meaning "Q is corrupted to X" — the rule that makes reset discipline and metastability teachable. `notQ` via `flip(0,bits)` (`:806`) becomes 4-state NOT (`¬X = X`). |
| `StateMachine.react` | `StateMachine.java:722` | An X input means "no transition is *known* to match", which today is indistinguishable from "no transition matches" — already routed to the once-per-run `TellUser` warning (`:770-778`). These must be separated. |
| `TruthTable.react` | `TruthTable.java:1400` | Two decisions: whether an X input can match a `-` column (`:1413`), and whether an output `-` becomes a value (X) or survives as a synthesis directive instead of being lowered by `"don't care becomes false"` (`:1447-1449`). |
| `Memory.react` | `Memory.java:1335` | Six control/data unknowns, each a separate pessimism choice (below). |

**Semantically ambiguous — 4 sites.** No defensible answer exists
without a *separate* model decision JLS has never made.

| site | anchor | why it is ambiguous |
|---|---|---|
| `WireNet.propagate` | `WireNet.java:442` | The scan-first-active loop (`:454-471`) is **not a resolution function** — it is not commutative or associative over its drivers, so it cannot be expressed as one. Choosing "conflict ⇒ X" is easy; choosing what a *net is* (`wire`/`wand`/`wor`/`tri0`/`tri1`) and whether drivers have strength is a separate decision (sweep-01's V2/V3). Until that is made, "X on conflict" is a placeholder, not a semantics. |
| `TriState.react` | `TriState.java:473` | Two problems. (a) An enabled buffer with an undriven data input today drives **0** (`:498-499`), not Z or X. (b) `toBeValue == null` as the in-flight "off" state (`:488-490`) plus the `TriStateOff` payload (`src/jls/sim/SimEvent.java:47`) *are* the whole-signal HiZ encoding, which per-bit Z retires — but "tri-state buffer" is only fully meaningful once strength exists. |
| `Stop.react` | `Stop.java:147` | Tests `in != null && in.cardinality() != 0`, so HiZ does **not** stop today. Does X stop? Stopping on maybe-1 makes batch grading non-deterministic in a new way; not stopping means a halt condition silently never fires. This is a product decision about what batch mode promises, and `docs/batch-interface.md` §3.1's outcome line is a **stability contract**, so either answer is an observable change. |
| `Pause.react` | `Pause.java:167` | Identical condition (`:176`), identical question, and pinned by `SimulationSemanticsRegressionTest.pausePausesOnlyOnNonZeroInput`. |

#### 1.3 Every site that treats null (HiZ) as zero

`docs/simulation-semantics.md:60-66` states this as intended behaviour.
The exact site count, from
`grep -rn -A2 "== null" src/jls/ | grep "new BitSet("`:

**27 HiZ-input coercions across 17 element classes** (sweep-01 said
"29 across 17"; two of the eight it attributed to `Memory` are store
lookups, not HiZ coercions — see below).

| class | sites | line | input coerced | what it should do instead |
|---|---|---|---|---|
| `Adder` | 3 | `:390`, `:393`, `:396` | a, b, cin | X in any operand bit taints sum bits at and above it, and the carry out; an undriven cin is not a 0 carry |
| `AndGate` | 1 | `:70` | each input | `0 AND x = 0`, `1 AND x = x` — the absorbing element survives, which is the whole pedagogical point |
| `NandGate` | 1 | `:72` | each input | dual of AND |
| `NorGate` | 1 | `:73` | each input | `1 OR x = 1`, `0 OR x = x`, then invert |
| `OrGate` | 1 | `:70` | each input | as above |
| `XorGate` | 1 | `:76` | each input | X-strict: any X ⇒ X (no absorbing element) |
| `NotGate` | 1 | `:68` | data | `¬x = x`, `¬z = x` |
| `DelayGate` | 1 | `:129` | data | pass X and Z through unchanged; it is a wire with a delay |
| `Binder` | 1 | `:245` | each range input | delete: per-bit compose makes a partly-undriven bundle representable, which the current `allOff` flag exists to approximate |
| `Decoder` | 1 | `:468` | address | drive X on the ambiguous output subset, 0 elsewhere |
| `Mux` | 2 | `:528`, `:539` | selector, data | X selector ⇒ X (or the agreed value under X-optimism); X data ⇒ X when routed |
| `Register` | 2 | `:755`, `:759` | clock, D | X clock ⇒ Q goes X (not "no edge"); X on D at an edge ⇒ X captured |
| `ShiftRegister` | 2 | `:623`, `:629` | amount, data | X amount ⇒ whole output X; X data shifts through, sign-fill from an X sign is X |
| `StateMachine` | 1 | `:731` | clock | X clock ⇒ state becomes unknown; distinguish from "no transition matched" |
| `TriState` | 1 | `:498` | data | drive X, not 0, when enabled over an undriven input |
| `TruthTable` | 1 | `:1416` | each input column | X matches a `-` column; X in a *cared-about* column ⇒ no row matches ⇒ outputs X |
| `Memory` | 6 | `:1344` CS, `:1348` OE, `:1354` WE, `:1360` addr, `:1371` sync clock, `:1382` data | control + data | X on CS/OE/WE is a *control* unknown (read X, write nothing, or write X — pick and document); X in the address is an *addressing* unknown; X on the sync clock is `Register`'s problem again |

Plus **3 non-HiZ zero coercions in `Memory`** that belong to sweep-01's
V5 (uninitialized state), not V1:

- `Memory.java:1455` — `stored == null → new BitSet()`: a word never
  written reads **0**. It should read **X**. This is the single line
  that makes a read-before-write bug invisible in the `riscv/` CPU
  trajectory.
- `Memory.java:923` — `new BitSet(1)` for the same lookup in the
  changed-locations report.
- `Memory.java:1491` — `what == null → new BitSet()` in the
  write-activity report.

And the initialization side, which is the same fiction from the other
direction:

- `LogicElement.initInputs` (`LogicElement.java:476-481`) sets every
  input at every depth to `BitSetUtils.Create(0)` — a *supplied reset*
  the design does not have. `SubCircuit.initInputs`
  (`SubCircuit.java:571-583`) recurses, so it is depth-uniform.
- `InputPin.initSim` (`InputPin.java:163-186`) already carries a
  half-measure toward bidirectionality: it sets `currentValue = null`
  when the boundary input is on a tri-state net (`:177`).

#### 1.4 Where JLS is already papering over the gap

Beyond the coercions, six structural workarounds. Each is evidence the
change is overdue, and each is *deleted* by the migration:

1. **The whole-signal HiZ marker, reconstructed four times.**
   `TraceSample` (`src/jls/sim/TraceSample.java:6-17`) documents it:
   "The BitSet width is the element's bit count plus one, with the
   extra top bit set to mark a HiZ value." That marker is rebuilt
   independently at `BatchSimulator.java:231`, `BatchSimulator.java:265`,
   `BatchTracePrinter.java:192`, `BatchTracePrinter.java:249`, and
   `Trace.java:127` (`off = new BitSet(bits+1)`). **Five construction
   sites for one out-of-band hack**, all of which a real four-state
   `TraceSample.value` removes.
2. **The `TriStateOff` / `NewValue` payload fork.** `SimEvent.java:39`
   and `:47` are two payload records for one concept; the fork is
   written out at `InputPin.java:202-217`, `JumpEnd.java:411-426`,
   `JumpStart.java:489-494`, `SubCircuit.java:628-633`, and
   `TriState.java:487-491`. Per-bit Z makes `TriStateOff` a
   `NewValue(allZ)` and deletes five switch arms.
3. **The bus-conflict dialog as a substitute for a value.**
   `WireNet.java:472-483` calls `TellUser.warn` because the value
   domain cannot carry "undefined". The tool already knows the answer
   is unknown; it just has nowhere to put it.
4. **`BitSetUtils.toDisplay` returning the string `"HiZ"`**
   (`src/jls/BitSetUtils.java:237-245`) — the fourth state smuggled out
   as text at the display boundary.
5. **`ImportSummary.coercedX`** (`src/jls/hdl/imp/ImportSummary.java:28`,
   `:59`, `:97-100`, incremented from
   `NetlistImporter.connectConstant`) — a counter whose only job is to
   report information the importer destroyed. Yosys already parsed the
   `x` bits faithfully; JLS throws them away and counts the losses.
6. **`VhdlEmitter`'s three-site disclaimer.** The generated header
   (`src/jls/hdl/VhdlEmitter.java:100-101`, "JLS simulates two states
   plus HiZ: this design drives '0'/'1'/'Z', never 'X'"), the class doc
   (`:24-25`), the hard-coded `(others => 'Z')` (`:345`), and the
   `when others` arms that satisfy `std_logic`'s nine-value coverage
   rule from a two-value simulator (`:495`, `:658`, `:690`). The
   emitter already writes a value model the simulator does not have.

---

### 2. `.jls` file-format impact

`docs/file-format.md` is normative; `test/jls/FileFormatSpecTest.java`
fails when it and the code drift apart.

**Do saved circuits change? Almost none of them.** The format stores no
simulation values at all, with exactly four exceptions:

| carrier | anchor | four-state impact |
|---|---|---|
| `Constant`'s `Int value` | file-format.md §7; `Constant.java` | An `Int` item is a non-negative BigInteger. A constant that drives X or Z (newly expressible under V1) needs a new encoding — a sibling `String value4` or a two-plane `Int aval` / `Int bval` pair. |
| `Register`'s `init` | simulation-semantics §8.1; `Register.initSim` | Under V5 this becomes *optional* and defaults to unknown. Today it is always present and always numeric. |
| `Memory`'s `String init` / `String initrle` | file-format.md §7; `Memory.java:460`/`:465` write, `:418-421` read, grammar in `Memory.initOK` (`:817-892`) | **The one hard case.** The grammar is hex `addr value` lines parsed with `Scanner.useRadix(16)` + `hasNextBigInteger`. Four-state words need either `x`/`z` digits in the value field — which `hasNextBigInteger` rejects, i.e. a *loud* failure, which is the good outcome — or a per-nibble mask. Either way this is a **change to the meaning of an existing record**, which §9 says requires a version bump. |
| `SigGen`'s `String signals` | `SigGen.initSim` (`:169-178`) delegating to `SigSim.initSim` | The `-t` grammar embedded inside the save file. Every `-t` extension (§3) is simultaneously a file-format change here. |

Everything else in the format — `bits`, the `int tristate 1` marker on
`WireEnd`s, `noncontig`, `pair` routing, `sid`, `probe` — is structural
and untouched.

**The FORMAT version story.** `Circuit.FORMAT_VERSION` is **2**
(`src/jls/Circuit.java:102`); the header is written at `:1482` from
`Circuit.formatVersionNeeded()` (`:1580-1587`), which takes the max of
each element's `Element.saveFormatVersion()` (`src/jls/elem/Element.java:819`);
`Circuit.load` refuses a declared version greater than
`FORMAT_VERSION` at `:765-770`.

Recommendation: **bump to FORMAT 3**, gated per-element exactly the way
FORMAT 2 is gated on vertical `Binder`/`Splitter` `orient` today. A
circuit that uses no four-state feature keeps saving `FORMAT 1` or
`FORMAT 2` **byte-identically**, which matters because §8's canonical
ordering makes the serialized bytes JLS's own convergence oracle and
state hash for collaboration (issue #163).

The bump is required on the letter of §9 for the `Memory.init` grammar
change alone ("any change to ... the meaning of an existing record").
For the new *attributes* (`valuemodel`, `Register.init` becoming
optional, a four-state `Constant`), §9's literal rule says no bump is
needed — but §9's own **silent-drop caveat** (`:458-478`) says a writer
SHOULD prefer a bump "whenever dropping the attribute would change
simulation behavior", and cites `Memory.initrle` and `Memory.sync` as
the standing instances of that failure class. Dropping a value-model
attribute changes simulation behaviour *by construction*: a
post-versioning JLS 5.x reader would load a four-state circuit as
two-state and silently produce different answers. That is the strongest
possible case for the bump, and the migration should also close the
open question §9 records about `Memory.sync` while it is in there.

**JLS 4.1 forward compatibility: unchanged, because it already ended.**
4.1 is *pre-versioning* — it has no `FORMAT` reader, and file-format.md
§4 records that a headerless file is implicitly version 0. Any file
whose first token is `FORMAT` already fails to load in 4.1, which has
been true since FORMAT 1. A FORMAT 3 file is refused by 4.1 exactly as
a FORMAT 1 file is; **this change does not worsen the 4.1 story, and
the plain-text container plus the `-savetext` flag remain the
interchange path.** The thing that *would* worsen it is taking the
"attributes only, no bump" route, which is precisely the
`initrle`-class silent loss the header exists to end.

Two housekeeping items: `test/fixtures/legacy-4.1/` currently contains
only a `README.md` and should be populated with real 4.1-era saves
before this lands, and `FileFormatSpecTest` needs the FORMAT 3 row plus
a "two-state circuits still save FORMAT ≤2" assertion.

---

### 3. `docs/batch-interface.md` impact

This is the document with the hardest constraint: §6 is a **stability
promise** — any change to the `-t` grammar, the stdout format, or the
VCD profile "requires a CHANGELOG entry and either a major version bump
or a compatibility flag that preserves the old behavior."

**§2, the `-t` grammar.** `value` is read as a BigInteger
(`SigSim.initSim`, `src/jls/elem/SigSim.java:40-130`; documented at
batch-interface.md:92-93). Tokens `x`, `z` and `-` are currently hard
errors (`specError("missing or invalid initial value for signal ...")`
→ stdout + exit 1). Adding them is therefore a **strict superset**: no
conforming input file changes meaning, so it is minor-version material
under §6's own carve-out for "additions that cannot break a conforming
consumer". Two traps:
- the hex rewrite at `SigSim.java:52` matches `-?0[xX][0-9a-fA-F]+`,
  which does not collide with a bare `x`/`z`/`-`, so the rewrite pass
  is safe;
- the Logisim catch-up bar (`docs/hdl-support-research.md:186-189`) is
  don't-cares on *outputs*, and `-t` only **drives inputs**
  (batch-interface.md §2 opening). Output don't-cares need a new file
  section or a new flag, not just a token. Cost that separately.

**§3.4, the stdout value display.** `BitSetUtils.toDisplay` (`:237-245`)
prints `HiZ` or `0xH (U unsigned, S signed)`. A partly-X value has no
unsigned or signed decimal. There are **real downstream parsers**:
`riscv/jlsrun.py:20-24` compiles four regexes (`_PIN`, `_REG`,
`_HIZ_PIN`, `_MEMLINE`) that pin the exact shape, and
`examples/autograde/autograde.py:52-56,84` documents "a value containing
HiZ stays a string (JLS never emits 'x')" as a design assumption.

The cheap and honest versioning story: **keep `toDisplay` byte-identical
for values that are fully two-state**, and emit the new form only for
values that actually contain X or per-bit Z. Every existing circuit
produces only two-state values, so every existing grader keeps working
with no flag at all, and the compat flag is needed only by circuits
that opt in. This costs nothing and should be a hard rule of the
migration.

**§4.3, the VCD profile.** `BatchSimulator.vcdValue` (`:522-551`) emits
`0`, `1`, `z`, `b<binary> <code>`, `bz <code>`. Per-bit X and Z make
mixed vectors like `b1z0` possible — which batch-interface.md:302-304
states explicitly **cannot occur**, and which
`VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`
pins as a contract. This is the one place where the normative document
directly *forbids* the change; the paragraph must be rewritten and the
test renamed and re-scoped (to "the two-state profile is still emitted
for two-state circuits"). The same "two-state circuits are
byte-identical" rule applies, so the three existing VCD byte-goldens
survive untouched.

**Deviation summary and versioning:**

| surface | deviation | version treatment |
|---|---|---|
| `-t` grammar | add `x`, `z`, `-` tokens | strict superset ⇒ minor + CHANGELOG |
| `-t` output expectations | new section/flag | new flag ⇒ minor + CHANGELOG |
| stdout §3.4 | new form only for non-two-state values | no observable change for existing circuits ⇒ CHANGELOG only |
| stdout §3.1 outcome line | `Stop`/`Pause` on X (§1.2) | genuinely observable ⇒ **major bump or compat flag** |
| VCD §4.3 | `x` and mixed vectors become possible | no observable change for existing circuits, but the *documented invariant* is retracted ⇒ major bump when the default flips |
| §5 golden mapping | new golden family | doc edit |

---

### 4. Test-suite blast radius

Totals at HEAD: **1,375 `@Test` methods across 204 test classes,
45,688 test LOC.** Of those, **21 test files reference `BitSet`** (134
references) and **17 construct or drive a simulation**.

The value-domain-facing set is **171 `@Test` methods across 23 files**:

| file | @Test | exposure |
|---|---|---|
| `test/jls/elem/MemoryModelTest.java` | 26 | word store, init, sync write |
| `test/jls/ElementSimulationGoldenTest.java` | 17 | per-element goldens |
| `test/jls/elem/RegisterModelTest.java` | 16 | edges, init, display shape |
| `test/jls/BatchSimulationGoldenTest.java` | 14 | gate truth tables, stdout order |
| `test/jls/elem/SubCircuitModelTest.java` | 13 | boundary values, tri-state propagation |
| `test/jls/SequentialGoldenTest.java` | 11 | flip-flops, latches, state machines |
| `test/jls/elem/MemoryInitEncodingTest.java` | 10 | RLE round-trip |
| `test/jls/ArchitectureRulesTest.java` | 10 | headless-core boundary |
| `test/jls/SimulationSemanticsRegressionTest.java` | 9 | the #98 verdicts |
| `test/jls/sim/SimEventContractTest.java` | 8 | payload records |
| `test/jls/ShiftRegisterTest.java` | 7 | fill rules |
| `test/jls/sim/SimEventDedupTest.java` | 6 | `equals`/`hashCode` over payloads |
| `test/jls/BitSetUtilsCreateTest.java` | 5 | the value constructor |
| `test/jls/VcdExportGoldenTest.java` | 4 | the two-state VCD contract |
| `test/jls/BitSetUtilsSumCarryTest.java` | 4 | the adder kernel |
| `test/jls/edit/TraceWindowingTest.java` | 4 | GUI trace, `off` marker |
| `test/jls/elem/WireValueChannelTest.java` | 3 | wire ink/stroke channel |
| `test/jls/edit/TraceRetentionTest.java` | 3 | trace history |
| `test/jls/VcdProbeExportTest.java` | 2 | probed nets |
| `test/jls/BatchTracePrinterTest.java` | 2 | `-r` printer |
| `test/jls/HeadlessCoreCanaryTest.java` | 2 | headless boot |
| `test/jls/sim/SimulatorNullContractTest.java` | 2 | setup contract |
| `test/jls/SimulationSeedOrderTest.java` | 2 | deterministic seed order |
| **total** | **171** | |

**By name pattern — the tests that assert, in their names, the exact
behaviour the change is for. 21 of the 171:**

| pattern / name | file | verdict |
|---|---|---|
| `multiDriverConflictResolvesDeterministicallyAndWarnsOnce` | `SimulationSemanticsRegressionTest` | **pins the wrong behaviour as correct**; must be re-derived to "conflict resolves to X and is diagnosable" |
| `agreeingTriStateDriversDoNotWarn` | same | survives in spirit, but its mechanism (the warn path) is being replaced |
| `initInputsReachesInsideSubcircuits` | same | pins the depth-uniform *zeroing*; V5 makes the value `U`, not 0 |
| `pausePausesOnlyOnNonZeroInput` | same | ambiguous under X (§1.2) |
| `vcdIsStructurallyWellFormedAndTwoStatePlusHiZ` | `VcdExportGoldenTest` | the test name *is* the invariant being retracted |
| `strokeMappingCoversAllThreeValueStates` | `WireValueChannelTest` | the name contains the count; becomes five |
| `renderedInkDiffersByValueStateAloneNotJustColor` | same | the a11y contract now has to hold over five states |
| `touchingWireEndGrowsARingGlyph` | same | survives |
| `logicalLeftShiftZeroFills`, `logicalRightShiftZeroFills`, `arithmeticRightShiftSignFills` | `ElementSimulationGoldenTest` | each needs an X-fill sibling |
| `logicalLeftShiftsInZeroesAndDropsHighBits`, `logicalRightShiftsInZeroes`, `arithmeticRightCopiesTheSignBit`, `amountBeyondTheWidthZeroFillsOrSignFills` | `ShiftRegisterTest` | same; four more |
| `readPastCapacityTurnsTheOutputOff`, `missingInitializationFileFallsBackToZeros`, `invalidBuiltInInitFallsBackToZeros` | `MemoryModelTest` | three that encode "unknown ⇒ zeros"; V5 reverses all three |
| `truthTableDontCareMatchesEitherValue` | `ElementSimulationGoldenTest` | the only existing don't-care assertion; V7 redefines what it means |
| `registerInitialValueAppearsBeforeAnyClockEdge` | `SequentialGoldenTest` | the golden that *asserts the fiction* V5 removes |

**Roughly:** ~120 of the 171 survive unchanged through stages 1–8 in
two-state mode (they run two-state circuits and get two-state answers,
which the dual-mode discipline keeps bit-identical); ~30 need
re-derivation or dual-moding; the 21 above assert the old behaviour by
name and must be renamed, split, or reversed.

**New tests required:** a four-state golden family. Two-input gate
tables over {0,1,X,Z} are 16 rows each × 5 two-input gates = 80
assertions; plus NOT (4), plus per-element X-propagation goldens for
the 8 "needs real thought" elements, plus resolution-function tables,
plus the new `LogicVector` type's own unit suite. Budget **60–90 new
test methods**, of which the `LogicVector` suite (~25) is written in
stage 1 and pays for itself immediately.

**Three under-appreciated gates that will bite:**

1. **`ElementSimulationGoldenTest.everySimulatingElementHasAGoldenOrARecordedExemption`**
   fails closed the moment any new simulating element appears. That is
   a *feature* — it forces a golden for `PullUp`/`BidirPin` under
   sweep-01's V3/V6 — but it means every new element is a two-PR item.
2. **JaCoCo package floors** (`pom.xml`): `jls.sim` at
   INSTRUCTION 93.0 / LINE 92.0 / BRANCH 84.5, `jls.elem` at
   73.0 / 70.0 / 58.5, `jls` at 51.5 / 50.0 / 55.5. New four-state
   branches land mostly in `jls.elem` (the lowest floor — good), but
   the VCD/stdout/trace work lands in `jls.sim` at a **92 % line
   floor**, meaning every new line there must be covered on the same
   PR. This is the single most likely cause of a red CI during the
   migration.
3. **The PIT mutation ratchet** — blocking at
   `mutationThreshold` 80 / `testStrengthThreshold` 82, baselined at
   82.98 % / 84.67 % over 905 mutants scoped to `jls.sim.*`,
   `jls.BitSetUtils`, `jls.Util`, `jls.SpatialIndex`, `jls.collab.op.*`
   (pom.xml `targetClasses`; CHANGELOG "Unreleased"). `BitSetUtils` is
   in scope and is being rewritten. Expect a mutant-count shift and
   budget a re-baseline PR.

Also load-bearing: `ArchitectureRulesTest.coreDependsOnNoGuiClasses`
constrains `jls.core..`, `jls.elem..`, `jls.sim..` to be AWT/Swing-free,
so a `LogicVector` placed in `jls.core` is architecturally correct but
must not reach for `java.awt.Color` to describe rendering; and
`HeadlessCoreRatchetTest`'s baseline list names
`src/jls/BitSetUtils.java`, so changes to its imports are watched.

---

### 5. The GUI: what X and weak-1 look like on screen

Five surfaces, in ascending order of difficulty.

**5.1 Wire ink — the hardest, because of a real a11y constraint.**
`WireRenderer.strokeFor` (`src/jls/edit/WireRenderer.java:43-56`) and
the colour choice in `WireRenderer.draw` (`:62-77`) give three states
today, each with **two** independent channels — colour *and* stroke:

| state | colour | stroke |
|---|---|---|
| HiZ (null) | `Palette.wireOffColor` | 1.0 px dashed `{4,3}` |
| non-zero | `Palette.nonZeroColor` | 3.0 px round-cap |
| zero | `Palette.wireZeroColor` | 1.0 px plain |

`WireValueChannelTest.renderedInkDiffersByValueStateAloneNotJustColor`
requires the distinction to survive without colour vision. So X and
weak-1 need **two more distinct strokes**, not two more colours.
Workable proposal: X = 3.0 px with a dense dash-dot pattern (reads as
"driven but wrong"); weak (V2 `pull`) = 2.0 px plain (reads as "driven
but less"). Both must stay distinguishable in `Theme.DEFAULT` and the
high-contrast theme (`src/jls/Theme.java`, surfaced through
`JLSInfo.Palette`, `src/jls/JLSInfo.java:132-151`).

The genuinely hard case is a **mixed vector**: a 32-bit bus with one X
bit. A wire is one stroke. Recommendation: **any-X ⇒ X ink** (the
pessimistic rendering, matching how waveform viewers colour a vector),
with per-bit detail visible only in the probe/trace readout. Document
it, because it is a deliberate loss of information at the drawing
level.

**5.2 `Display` element.** `DisplayRenderer` (`src/jls/edit/DisplayRenderer.java:45-62`)
prints `" HiZ "` or `BitSetUtils.ToString(value, base)`. Four-state
needs a radix-aware renderer with the standard Verilog semantics: base
2 exact; base 16 with a per-nibble `x`/`z` when that nibble is not
fully known; base 10 → `x` when the value is not fully known.

This has a knock-on: **five `BitSetUtils` conversions become partial** —
`ToString` (`:83`), `ToStringSigned` (`:103`), `ToInt` (`:127`),
`ToLong` (`:158`), `ToBigInteger` (`:177`). Each needs either a
`@Nullable`/exception contract or a four-state sibling. And there are
**four independent copies of the "0xH (U unsigned, S signed)"
formatter** — `BitSetUtils.toDisplay` (`:237`),
`Register.showCurrentValue` (`Register.java:829-836`),
`Pin.showCurrentValue`, and `MemoryContentsDialog` (`:64-68`).
Consolidating them into one four-state formatter is a prerequisite and
a small win on its own.

**5.3 `DisplayBool` (the truth-table grid).**
`src/jls/edit/DisplayBool.java` already renders a don't-care cell (the
model stores `2`, `TruthTable.java:79`), so this is the **one GUI
surface that already has a third symbol**. V7 changes what that cell
*means* — a surviving synthesis directive rather than something lowered
to 0 at `TruthTable.java:1447-1449` — not how it draws. Nearly free.

**5.4 The trace window.** `Trace.paintComponent`
(`src/jls/edit/Trace.java:290-430`) has three renderings today:
mid-height line for the `off` marker, top/bottom line for 1/0 on a
1-bit signal, and top+bottom rails with a centred hex string for a
multi-bit signal. X needs a fourth: the industry-standard full-height
hatched (conventionally red) band — which for the multi-bit case is
just the two rails it already draws, filled. The slider readout at
`:411-413` prints `"HiZ"` or a radix string and picks up 5.2's
formatter. The `off` sentinel (`:92`, `:127`) goes away entirely, along
with the six `equals(off)` comparisons scattered through the paint
loop (`:331`, `:340`, `:352`, `:366`, `:374`, `:412`) — this method
gets meaningfully **simpler**.

Pinned by `TraceWindowingTest`'s three
`windowedRepaintMatchesFullRepaintFor*` differential tests, which is
exactly the right harness: they compare a windowed repaint against a
full repaint, so they keep working as long as the new rendering is
deterministic.

**5.5 Probes.** Free. A probe's value reaches the VCD through
`WireNet.propagate`'s probe loop (`WireNet.java:512-527`) →
`Simulator.probeSample`; the GUI shows probe *names*, not values, so a
probe costs nothing beyond its net's own rendering.

---

### 6. Staged migration plan (green at every stage)

The organising trick is **stage 2**: instead of deleting the 27
HiZ-as-zero coercions, *rename* them. `if (v == null) v = new BitSet()`
becomes `v = v.coerceUndrivenToZero()` — a named, greppable, counted
method on the new type with identical behaviour. The tree stays green,
every golden stays byte-identical, and the remaining work becomes a
finite, mechanically enumerable worklist: 27 call sites to convert, one
at a time, each with a normative-doc paragraph.

The second organising trick is a **per-circuit value-model attribute**
defaulting to two-state, so four-state behaviour is opt-in until the
last stage. Every existing golden runs in two-state mode and produces
identical bytes throughout stages 1–8.

| stage | work | weeks | green because |
|---|---|---|---|
| **0. Fences** | Populate `test/fixtures/legacy-4.1/`. Add a differential harness that runs the whole golden corpus under both value models and asserts byte-equality in two-state mode. Record the current JaCoCo/PIT numbers. | 1 | nothing changed |
| **1. The type** | `jls.core.LogicVector`: sealed interface, `Binary` record (one `BitSet` plane, bit-identical to today) and `FourState` record (aval/bval two-plane, IEEE 1364 `s_vpi_vecval` encoding). Full op set: and/or/xor/not, slice, concat, sign/zero extend, resolve, `toLong`/`toBigInteger` (partial), radix formatting, `equals`/`hashCode`. ~25 unit tests. AWT-free (`ArchitectureRulesTest`). | 2 | new file, nothing consumes it |
| **2. Widen the plumbing** | `Put.currentValue`, `Input`/`Output` `setValue`/`getValue`, `Output.propagate` (`:136`), `WireNet.setValue`/`getValue`/`propagate`, `SimEvent.NewValue`/`MemoryWrite`/`TableOutput`, `TraceSample.value` all take `LogicVector`. `null` becomes `LogicVector.allZ(bits)`. Every element keeps computing in `Binary` via `coerceUndrivenToZero()`. | 2.5 | behaviour byte-identical; all 171 tests pass unchanged |
| **3. Delete the marker hack** | Retire the five `new BitSet(bits+1)` HiZ-marker reconstructions (`TraceSample`, `BatchSimulator:231`/`:265`, `BatchTracePrinter:192`/`:249`, `Trace:127`) in favour of the real value. Net **negative** LOC. | 0.5 | goldens unchanged; `TraceWindowingTest` differentials still hold |
| **4. Resolution function** | Rewrite `WireNet.propagate` (`:442-485`) as a per-bit fold over a driver list cached at `makeNet`/`recheck` time. Two-state mode keeps first-active-in-net-order; four-state mode resolves conflict ⇒ X. `TriState` drives per-bit Z; `TriStateOff` deprecated in favour of `NewValue(allZ)`. `Splitter`/`Binder` lose their all-or-nothing special cases. | 2 | mode-gated; two-state goldens byte-identical |
| **5. Element pass A** | The 22 mechanical methods (§1.2). Gate tables become 4-state; the five payload forks collapse; `Constant`'s in-place mutation bug disappears. | 2 | mode-gated per operation; two-state results identical by construction |
| **6. Element pass B** | The 8 "needs real thought" + 4 "ambiguous" sites, **one PR each**, each shipping (a) the semantics, (b) a normative paragraph in `docs/simulation-semantics.md`, (c) a four-state golden. `Memory` gets its own PR (51 `BitSet` references, 6 coercions). | 4 | mode-gated; each PR adds goldens rather than changing them |
| **7. Surfaces** | stdout §3.4 (new form only for non-two-state values), VCD §4.3 (`x` + mixed vectors), `-t` tokens `x`/`z`/`-`, wire ink (2 new strokes + a11y test), `Display`/`Trace`/`MemoryContentsDialog` formatters consolidated into one. | 2.5 | existing circuits produce identical bytes; `riscv/jlsrun.py` and `examples/autograde/autograde.py` keep parsing |
| **8. File format** | FORMAT 3 gated per element via `saveFormatVersion()`; `Memory.init`/`initrle` four-state grammar; `Constant` four-state encoding; `FileFormatSpecTest` rows; resolve the open `Memory.sync` version question while in there. | 1 | two-state circuits still save FORMAT ≤2 byte-identically |
| **9. Flip and re-derive** | Default the value model to four-state. Re-derive the 21 name-pinned tests and ~30 dual-moded goldens. Rewrite `simulation-semantics.md` §2/§5/§6/§9/§10/§12 and `batch-interface.md` §2/§3.4/§4.3/§6. CHANGELOG + major bump. Re-floor JaCoCo and re-baseline PIT. | 2.5 | this is the one stage where goldens change, and it changes them all at once with the spec rewritten in the same PR |

**Total: 20 weeks; honest range 17–22.**

Two notes on the estimate:

- **The dual-mode discipline costs about 6 of those weeks** (the mode
  flag, running the corpus twice, and the extra stage-9 flip). A
  big-bang branch that accepts a long red period lands in **12–14**,
  matching sweep-01's V1 figure. The trade is explicit: 6 weeks buys a
  green tree at every commit and a release in which no existing lab
  changes behaviour. For a single-maintainer project with a blocking
  CI ratchet on three axes (JaCoCo, PIT, ArchUnit), the dual-mode
  route is almost certainly the cheaper one in wall-clock time even
  though it is more weeks of work.
- The estimate covers sweep-01's **V1 + V7 + the resolution-function
  half of V2**. It does **not** include the strength lattice.

**Increments on top, sequenced by dependency:**

| increment | weeks | gate |
|---|---|---|
| V5 uninitialized start-up + `Register` async reset/preset pins | 2–4 | needs V1; the reset pins are ~1 week and touch `Register`'s geometry, dialog, save attributes and four orientation branches |
| V2 drive-strength lattice + real resolution | 4–6 | needs V1 + stage 4 |
| V4 IEEE 1164 nine-value mapping | 2–3 | needs V1 + V2 + V5; mostly tables |
| V3 driver kind, net kind, drawable `PullUp` | 3–5 | needs V1 + V2 |
| V8 setup/hold + metastability | 3–4 | needs V1 |
| V6 bidirectional pins + `inout` subcircuit boundary | 4–6 | needs V1 + V2 |

**Full program: 38–50 maintainer-weeks.** At half-time that is roughly
18 months; at a sustained one-day-a-week it is not finishable, which is
worth stating plainly. The staging above is designed so that stopping
after **any** stage leaves a shippable tree, and so that stopping after
stage 4 already delivers the two highest-value pedagogical items —
honest bus conflicts and per-bit HiZ — for 8 weeks rather than 20.

---

### 7. What the census says about the keystone claim

The evidence confirms it, and sharpens it in one way worth recording.

The value domain is on the critical path of every other sweep's top
item, and the migration is **finite and enumerable**: 33 value-computing
methods, 27 coercion sites, 5 marker reconstructions, 5 payload forks,
4 duplicated formatters, 4 file-format value carriers, 3 batch-interface
sections, 171 exposed tests. Not one of those numbers is unbounded, and
sections 1.4 and 5.4 show the change *removing* code in at least six
places.

The sharpening: the coercion sites are **not uniformly hard**. 22 of 33
methods are mechanical, 8 need a documented decision, and only 4 are
genuinely ambiguous — and of those 4, three (`WireNet`, `TriState`,
and the `Stop`/`Pause` pair) are ambiguous not because four-state
values are hard but because they depend on a *second* decision JLS has
never made: whether a driver has strength and whether a net has a kind.
That is sweep-01's V2/V3, and it means the true keystone is slightly
larger than "add X": it is **"a value has states and a driver has
strength"**. The four-state type without the strength lattice is a
complete, shippable, individually valuable stage — but it leaves
`WireNet.propagate` resolving conflicts to X as a *placeholder* rather
than as a semantics. Plan for V2 as stage 10, not as optional.
