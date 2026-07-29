## Parameterized subcircuits, generics, and hierarchy that scales

*Written against the tree at HEAD. Every JLS claim is anchored to a path and line
range; external claims are marked verified or unverified individually.*

---

### What is missing today

**There is no parameter mechanism anywhere in the model.** `SubCircuit` holds
exactly four pieces of state — a nested `Circuit`, a local name, two pin maps, and
an orientation (`src/jls/elem/SubCircuit.java:26-37`):

```java
private @Nullable Circuit subCircuit;
private @Nullable String name;
private Map<Input,InputPin> inmap = new HashMap<Input,InputPin>();
private Map<OutputPin,Output> outmap = new HashMap<OutputPin,Output>();
private Orientation orientation = Orientation.RIGHT;
```

There is nothing an instantiation site can *say* to a definition. An 8-bit adder
and a 32-bit adder are two drawings, and JLS has no way to know they are related.

**Instantiation is already a deep copy, and always has been.** Three independent
sites do the same thing:

- `SubCircuit.copy` (`:332-384`) constructs a fresh `Circuit`, walks
  `getSubCircuit().getElements()`, and runs `Util.copy` + `Util.partition` over
  every non-attached element.
- `SimpleEditor.doImport` (`src/jls/edit/SimpleEditor.java:5323-5351`) does the
  same for the Import menu — "make a set of all elements … copy elements to new
  circuit" — then `finishImport` (`:674-697`) wraps the copy in a new `SubCircuit`.
- `Circuit.loadElementItems` (`src/jls/Circuit.java:1008-1024`) constructs
  `new Circuit("")` per nested `CIRCUIT` block on load.

So **JLS's hierarchy is elaborated-by-copy already**. Two instances of "the same"
block are two unrelated object graphs. That is the fact this program builds on:
elaboration is not a new concept to introduce, it is an existing implicit phase
that has never had inputs.

**Width is a saved integer on 14 element classes.** `grep -l '"bits"'
src/jls/elem/` returns `Adder`, `Decoder`, `Display`, `Gate`, `Group`, `JumpEnd`,
`JumpStart`, `Memory`, `Mux`, `Pin`, `Register`, `ShiftRegister`, `State`,
`TriState`. Representative declarations: `Pin.java:186`, `Gate.java:266` (plus
`numInputs` at `:278`), `Mux.java:221` (plus `inputs` at `:203`), `Adder.java:141`,
`Register.java:299`, `Decoder.java:215`; `Memory` is handwritten
(`Memory.java:440-465`, `int bits` / `int cap`). Puts are minted from those ints in
each element's `init` — e.g. `Splitter.init` (`src/jls/elem/Splitter.java:35-56`)
builds one `Output` per `Entry` in `ranges` with `e.getSize()` bits. Every one of
those integers is a literal in the save file and a literal in the drawing.

**The `.jls` format has no slot for a parameter.** `docs/file-format.md:113-137`
gives seven item kinds (`int`, `long`, `Int`, `String`, `ref`, `pair`, `probe`)
plus a nested `circuit-block` for `SubCircuit` only; `:321` describes the
`SubCircuit` body as "one nested `CIRCUIT` block (no `FORMAT` line)"; `:358-360`
says nested blocks recurse with the same grammar. `SubCircuit.save`
(`:282-288`) writes the entire nested circuit inline. There is nothing on the
instance and nothing on the definition that could carry a binding.

**The editor's instance dialog is name + orientation.**
`src/jls/edit/SubCircuitDialog.java:30-155` — a `JTextField` for the name and two
radio buttons. That is the entire configuration surface of a JLS module.

#### The workaround, which is the strongest evidence

JLS's own flagship design does not use hierarchy at all. `riscv/build/addi.jls`
measured at HEAD:

```
1038 ELEMENT records, of which 810 are WireEnd -> 228 logic elements
  43 Mux   43 Constant  34 Splitter  34 AndGate  32 Register
   9 Binder  8 NotGate  5 XorGate  5 Extend  4 Adder
   3 ShiftRegister  3 OrGate  3 Memory  1 InputPin  1 Decoder
   0 SubCircuit
```

Zero `SubCircuit` in a 228-element processor. Instead there is a **second,
complete, independently maintained implementation of the normative save format,
written in Python**: `riscv/jlsbuild.py` (322 lines). Its own header says so
(`:11-14`):

> "Every element emitter below matches the exact save format and put names used by
> the JLS element classes under src/jls/elem/. The companion test file
> test_primitives.py validates each primitive against the real batch simulator."

`riscv/test_primitives.py` (217 lines) exists solely to keep that second
implementation in sync with the first. `docs/file-format.md` is NORMATIVE
specifically so third parties can write readers and writers — and the project's own
CPU is built by a writer the project also maintains, because there is no other way
to produce the circuit.

And inside it, the generate statement, written in Python
(`riscv/build_cpu.py:230-263`, method `regfile`):

```python
for i in range(1, 32):
    we = c.AND(1, 2)
    ...
    dmux = c.mux(2, 32)
    reg = c.register(f"x{i}", 32, init=0, kind="pff", watch=1)
```

Thirty-one registers, thirty-one AND gates, thirty-one 2-way muxes, then a
32-input read mux built by a second loop (`:255-261`). That is a `for-generate`
in a foreign language emitting a foreign tool's file format. The capability
roadmap already counts the cost of just the write-enable half — "**62 of the CPU's
228 elements — 27% of the entire processor — existing solely to synthesise a
write-enable the `Register` element does not have**"
(`docs/capability-roadmap/README.md:260-266`) — but attributes it to P2's missing
register pin. Half of it is P2. The *other* half is that even with a perfect
`Register`, a student still cannot draw thirty-two of anything.

`riscv/make_cpu.py:50-51` is the user-facing consequence: `build_cpu_circuit(...)`
then `c.save(out)`. The documented way to get a CPU in JLS is to run a Python
program.

---

### The capability

Parameters on a circuit definition, bindings at each instantiation, propagation
into inner element widths and counts, a drawn replication element, and one
explicit elaboration phase that turns all of it into the ordinary circuit JLS
already simulates.

#### D1. Parameters are declared by a drawable `Parameter` element

The definition circuit already declares its interface with drawable elements —
`InputPin` and `OutputPin`. Parameters follow the same idiom rather than
inventing circuit-level metadata, and this matters for a concrete reason: the
grammar at `docs/file-format.md:119` is `circuit-block = "CIRCUIT" name { element }
"ENDCIRCUIT"` — a `CIRCUIT` block has **no attribute items**, only elements. A
declaration that is not an element cannot be saved without restructuring the
grammar.

New element, tag `Parameter`, zero puts, drawn as a small labelled tag in the
definition:

```
ELEMENT Parameter
 int id 0  int x 12  int y 12  String sid "..."
 String pname "N"
 String kind "width"        ; width | count | value
 Int default 8
 String constraint "1..64"
 String doc "data path width"
END
```

`kind` exists so the editor and the elaborator can check the *use*: a `width`
parameter may feed a `bits` attribute, a `count` may feed `numInputs` / `inputs` /
`cap` / an `Array` count, a `value` may feed a `Constant` or a `Register` init.
Mixing them is a load-time diagnostic, not a mystery.

Registration cost is the sixteen-step element ritual `ARCHITECTURE.md` documents,
here concretely: `ElementRegistry.ALL` (`src/jls/elem/ElementRegistry.java:38-73`),
`SaveTags`, `ElementVocabulary.ALLOWED`
(`src/jls/collab/op/ElementVocabulary.java:39-46`), a palette entry, a help topic,
and the three totality tests that fail until they agree.

#### D2. Bindings live on the instance, as expressions

`SubCircuit` gains one field, `Map<String,String> bindings`, saved as repeated
`String` items — one existing item kind, no grammar change:

```
ELEMENT SubCircuit
 String orient "RIGHT"
 ...
 String bind "N=32"
 String bind "DEPTH=clog2(N)"
 CIRCUIT alu
 ...
 ENDCIRCUIT
END
```

A binding expression may reference the **enclosing** circuit's own parameters.
That is the property that makes hierarchy scale rather than just making leaves
resizable: a parameterized ALU instantiates a parameterized adder with `N=N`, and
one number at the top sizes the whole tree.

#### D3. The expression language: total integer arithmetic, frozen, specified

**Recommendation: integer literals, parameter identifiers, `+ - * / %`, `**`,
comparisons, `? :`, and exactly four builtins (`clog2`, `max`, `min`, `abs`). No
strings. No loops. No recursion. No user-defined functions. No reference to any
signal value.** Specified in a normative document and frozen the way the `-t`
grammar is (`docs/batch-interface.md` §2).

This is a deliberate rejection of the more powerful option (see the competitive
section: Digital uses a general scripting language, HGS, and can procedurally
`addComponent`/`addWire`). Three reasons, in order of weight:

1. `docs/file-format.md:67-71` states that "circuit files are exchanged between
   untrusting parties by design" and bounds container expansion at 64 MiB for
   exactly that reason. A file whose meaning requires *running a program* is not a
   data format. Total arithmetic means elaboration always terminates, always
   yields the same answer, and can be re-implemented by a third-party reader from
   the spec — which is the stated conformance target at `:19-29`.
2. It keeps the elaborator small enough to reason about. Every expression is a
   pure function of the bindings; there is no state, so there is no ordering
   question and no "which code ran first."
3. It is teachable to a first-year. `clog2(N)` is the only unfamiliar thing on the
   list, and explaining it *is* a lesson.

#### D4. Expression-backed attributes, with the elaborated int always present

Every structural int attribute gains an optional sibling: `int bits 32` may be
accompanied by `String bitsExpr "N"`. **The int is always written, and it always
holds the elaborated value.** Same for `numInputs`, `inputs`, `cap`, and
`Register`/`Constant` initial values.

This one decision carries the whole compatibility story (§ file format below) and
costs nothing at simulation time, because the simulator only ever reads the int.

The genuinely hard case is `Group` (`Splitter`/`Binder`), whose port structure is a
list of bit ranges encoded as `pair` items (`docs/file-format.md:339-354`,
`Splitter.init` at `:35-56`). A parameterized splitter's ranges are a *function* of
N, not a scaled integer. Recommendation: a `Splitter`/`Binder` may declare
`String rangesExpr` in one of three closed forms — `slices(N, W)` (N equal fields
of W bits), `fields(a,b,c…)` (an explicit list of expressions), or `bits(N)` (N
one-bit taps) — rather than an arbitrary generator. Anything else is refused. This
is the one place the design accepts less expressiveness than it could have, and it
should be revisited only with a real lab that needs it.

#### D5. Replication: an `Array` element, not a for-generate

**Recommendation: one new element, `Array`.** It names a definition circuit, a
count expression, and binds an index parameter `i` in each copy. Each port of the
definition declares one of five **combination modes**, and only five:

| mode | meaning | the thing it draws |
|---|---|---|
| `broadcast` | every copy sees the same net | one wire to a bracket |
| `split` | an (N×W)-wide net; copy *i* gets slice *i* | a bus into a fan |
| `bundle` | N copies' W-bit outputs concatenate to N×W | the fan, reversed |
| `chain` | copy *i*'s output feeds copy *i+1*'s input; both ends exposed | an arrow through the stack |
| `reduce` | copies' outputs combined by AND / OR / XOR | a converging tree |

Those five cover every structure the scope names. A ripple-carry adder is `chain`
on the carry and `split` on A/B/S. A register file is `split` on the write-enable
one-hot, `broadcast` on the write data and clock, `bundle` on the reads. A barrel
shifter is a nested `Array` of log₂N stages with `chain` on the data. A crossbar is
an `Array` of `Array`s with `split` on rows and `bundle` on columns.

**Why an element and not a textual generate.** The argument for a schematic tool is
that structure is visible. A `for-generate` written into a drawing is invisible
text — it reproduces, inside a schematic editor, the exact property that makes
Verilog `generate` the hardest construct in the language to teach and debug. An
`Array` is a drawn box labelled `×32` whose port modes are visible in the wire
shapes, with two actions: **open one copy** (read-only, with `i` displayed) and
**expand in place** (replace the `Array` with its expansion as ordinary elements,
undoably). That second action is both the escape hatch and the best lesson in the
feature: *draw the abstraction, expand it, count what you actually built.*

#### D6. Elaboration is a real, named phase — in `jls.core`, before `finishLoad`

Today the implicit elaboration is `Circuit.finishLoad` (`src/jls/Circuit.java:1300`):
mint stable ids (`:1305-1334`), call `el.init(g)` on every non-wire element so puts
come into existence from `bits` (`:1336-1342`), then partition wire ends into
`WireNet`s (`:1344-…`).

Recommendation: a new AWT-free package `jls.core.elab` with one entry point:

```java
Elaborated elaborate(Circuit design, Map<String,Long> topBindings)
```

It returns a *fresh, ordinary* `Circuit` tree in which every `bits` / `numInputs` /
`inputs` / `cap` is a concrete int, every `Array` has become N `SubCircuit`s plus
the splitter/binder/wire mesh its port modes imply, and every produced element
carries a stable id derived deterministically from (template sid, index path) so
`sid` uniqueness (`docs/file-format.md:376-402`) and `Circuit.stateHash` stay pure
functions of content.

**It runs before the existing `finishLoad`, and `finishLoad` is unchanged.** The
acceptance criterion of the whole program is therefore stateable in one sentence:
*nothing in `jls.sim` changes.* The simulator, the batch surface, VCD export,
watched-element printing, HDL export, collab ops and every golden see exactly what
they see today. `docs/simulation-semantics.md` §5's initialization contract
(`:128-171`) — `initInputs` recursing through `SubCircuit.initInputs` (`:571-583`),
`initSim` in stable-id order (`:592-611`) — is untouched, because by the time the
simulator runs there are no parameters left.

Being in `jls.core` also satisfies the one ordering constraint the roadmap already
records for the headless-core keystone (`docs/capability-roadmap/README.md:933-942`):
this lands *inside* the boundary rather than across it, and is covered for free by
`HeadlessCoreRatchetTest` and `ArchitectureRulesTest.coreDependsOnNoGuiClasses`.

#### D7. What is deliberately refused

- **Port *count* on a plain `SubCircuit` boundary may not vary.** Widths may.
  A parameter that would add or remove a port on a drawn instance changes the
  shape of a box the user has already wired. Varying counts are expressible — via
  `Array` — where the multiplicity is the point and is drawn. This makes JLS less
  expressive than Verilog on purpose, and it is the right call for a drawing.
- **Recursion.** A definition may not instantiate itself, directly or
  transitively. `Circuit.load` has no recursion guard today; the elaborator needs
  one with a stated depth bound and a diagnostic naming the cycle.
- **Zero, negative, or constraint-violating values.** Checked at elaboration,
  reported through the existing `LoadError` channel with the parameter name, the
  expression text, and the resolved value.

#### D8. Editor UX

Four affordances, no more.

1. **Definition side.** The circuit tab gains a Parameters panel: a table of
   (name, kind, default, constraint, doc). Adding a row adds a `Parameter`
   element; the table is a view over them.
2. **Instance side.** `SubCircuitDialog` (`src/jls/edit/SubCircuitDialog.java`)
   gains a bindings table below the name and orientation: one row per declared
   parameter, showing the expression and the resolved integer beside it. OK
   re-elaborates *that instance in place* and calls `SubCircuit.remapPins`
   (`:447-467`) — which already exists, for undo/redo, and is precisely the
   operation needed.
3. **On the drawing.** The instance box shows its bindings: `alu<N=32>`. Ports
   whose width changed are highlighted.
4. **Reconnection policy — decided, not left open.** Widths change; **wires
   survive**. A wire whose width no longer matches its puts becomes a *marked*
   net, surfaced through the ERC channel P5 builds, never a deleted wire. Deleting
   a student's wiring because they typed 16 instead of 8 is the single failure
   mode that would make this feature hated, and it must be designed out at the
   start. A binding change goes through `SimpleEditor`'s single mutation entry
   point so undo captures it as one atomic snapshot.

---

### The `.jls` format, existing files, and JLS 4.1

**One rule: a `.jls` always contains the fully elaborated circuit; the
parameterization is additive metadata beside it.**

- `SubCircuit` bodies are written inline exactly as today (`:282-288`).
- Expression-backed attributes appear *next to* the elaborated int, never instead
  of it (D4).
- An `Array` saves as its **elaborated expansion** — N `SubCircuit`s and the mesh —
  plus one `ELEMENT ArrayGroup` record naming the template, the count expression,
  the port modes, and the `sid`s of the expansion it owns.

Consequences, each intended:

**Every existing saved circuit loads unchanged and simulates identically.** There
is no migration. That is unusual in this roadmap and it is a direct consequence of
the rule above; it should not be traded away.

**An older reader that drops the new attributes loads a correct, working,
non-parameterized circuit.** Compare the standing precedent at
`docs/file-format.md:458-469`: `Memory`'s `initrle` is loaded by JLS 4.1 as
*nothing*, silently changing behaviour, and `sync` (`:471-478`) silently changes
write timing. Here, dropping every new attribute changes **no** simulation
behaviour, because the elaborated ints are all still there. This design is
strictly better-behaved than the two cases the format document already flags as
known hazards.

**Bump to FORMAT 3 anyway.** By the letter of §9 (`:429-435`) no bump is required —
new attributes on existing types, and dropping them does not change behaviour. But
`Parameter` and `ArrayGroup` are new *tags*, and §9 says an old reader meets a new
tag with "no element type named X" — a loud failure, but a confusing one for a file
whose logic the old reader could otherwise have simulated perfectly. Declaring
FORMAT 3 converts that into the accurate diagnostic the version header exists to
produce (`:180-184`), and is the honest signal that a re-save by an older JLS
loses the parameterization while keeping the circuit. Add the version-3 row to
`docs/file-format.md` §9's history and the tags to §7's table; `FileFormatSpecTest`
fails until the document and the code agree.

**The file gets bigger.** A 32-entry register file writes 32 elaborated slices.
That is real; `riscv/build/addi.jls` is already 1038 `ELEMENT` records. Accept it:
the container is XZ (`:45-49`) and repeated blocks compress hard, and the
alternative trades the entire compatibility property for bytes. If size ever
genuinely matters, the right fix is **P3's component table**, which composes with
this design rather than replacing it.

**The CI property that makes the redundancy safe.** `load → re-elaborate from the
saved bindings → save` must be byte-identical to `load → save`. That is
`Circuit.stateHash` equality, it proves the elaborated plane is a pure function of
the parameter plane, and it belongs in a `ParameterizationConsistencyTest` shaped
like `FileFormatSpecTest`. Removing it must be visible.

**Collaboration.** `SetElementConfig.rejectUnsupported`
(`src/jls/collab/op/SetElementConfig.java:148-152`) already refuses `SubCircuit`
("subcircuits are reconfigured through the subcircuit op kind") and
`ElementBlocks` (`:107-112`) refuses `SubCircuit` blocks. `ArrayGroup` needs the
same treatment, plus one decision: a binding change is **one op**, not N — the
peer re-elaborates from the binding rather than receiving the expansion.

---

### What it unlocks

**Standards.** This is not primarily a standards program, and it should not be
sold as one. Where it does touch the survey:

- **#31 Verilog / #25 VHDL** — `parameter`/`generic` and `generate`/`for-generate`
  become expressible in both directions. This matters most on *import*:
  `NetlistImporter` refuses multi-module netlists today
  (`src/jls/hdl/imp/NetlistImporter.java:156-159`, "flatten the design"), and even
  after P3's hierarchy work, Yosys emits one module **per parameter binding** —
  so parameterized Verilog stays unimportable-as-drawn until a JLS instance can
  carry a binding.
- **#4 IP-XACT** — configurable components and `modelParameters` are the
  parameterization mechanism; an IP-XACT component with no parameters describes
  fixed-width IP, which is not how IP is delivered. *(The IP-XACT element names
  here are from memory and are **unverified** against IEEE 1685; the JLS-side
  facts are verified.)*
- **#74 EDIF** — instance `property`. *(unverified in detail)*
- **#5 RV32I** — RV64 becomes a width parameter rather than a second CPU.

**Engineering capabilities.**

- One adder drawing at every width. One register file, sized. One barrel shifter.
  One crossbar. One FIFO.
- **The `riscv/` CPU becomes drawable.** That deletes the strategic liability of
  maintaining `riscv/jlsbuild.py` — a second implementation of a normative format,
  kept in sync by `riscv/test_primitives.py`. The Python stays useful as a *test*
  (a differential oracle for the writer), which is a much better job for it.
- Design-space exploration inside one file: set N and re-run.

**Teaching capabilities — concretely, what a student can do afterwards that they
cannot today.**

- **Build a 1-bit full adder from gates, then type 8.** Today the choices are: use
  the built-in `Adder` (a black box — the lesson evaporates), or hand-copy the
  1-bit cell eight times and draw twenty-four wires. Hand-copying teaches "this is
  tedious." The `Array` teaches "this is *regular*, and the carry is what makes it
  slow" — which is the actual content of the unit.
- **A 32×32 register file becomes a drawing.** It is currently not drawable at all;
  the flagship design in the tree proves it by not containing one drawn.
- **Scaling becomes a measurement.** With P4's STA: set N = 4, 8, 16, 32 on *the
  same drawing* and plot the critical path. Five minutes, a real O(N) curve, from a
  real design, with no confounds — because today the four widths are four separate
  drawings and any difference between them might be a drawing difference. This is
  the strongest available answer to "why does anyone build a carry-lookahead
  adder," and it needs both P7 and P4 to exist.
- **"Expand in place" is itself a lesson.** Draw the abstract form, expand it, count
  the gates, look at the fanout. Verilog can only show you the elaborated netlist
  in a synthesis log.
- **It deletes an actively bad lesson.** A student who draws an 8-bit block and
  then needs 16 currently learns that hierarchy does not help — that a JLS module
  is reusable only at exactly the width it was drawn at. That is the opposite of
  what a module is for, and JLS teaches it every term.

---

### Competitive position

**Commercial (Cadence / Synopsys / Siemens / Vivado / Questa).** Parameterization is
universal and total — and it lives *in the text*. Schematic entry died in these
flows, and this is a large part of why: not that drawing is worse, but that
undrawable scaling is worse. The nearest commercial thing to a parameterized
schematic is Vivado's IP Integrator block design, where blocks carry parameters and
a canvas connects them — but its leaves are packaged IP with GUI parameter forms,
not drawings the user made, and its replication answer is "write a generate in the
RTL." *(Characterization of IP Integrator is from general knowledge and is
**unverified** here.)* **Parity is achievable; leadership is available in the
drawn-leaf case, which is the case that matters for teaching.**

**hneemann's Digital — has solved this, and is the tool to beat.** Verified two
ways. The README states, verbatim: *"Supports generic circuits. This allows the
creation of circuits that can be parameterized when used. In this way, it is
possible, for e.g., to create a barrel shifter with a selectable bit width."*
(verified by fetch, github.com/hneemann/Digital). And the implementation, verified
by reading `src/main/java/de/neemann/digital/draw/library/ResolveGenerics.java`:
there is a `GENERIC` attribute on both the circuit and the embedding component; a
custom scripting language (HGS) with a `Parser`/`Context`; a `GenericInitCode`
element inside the circuit supplying defaults; `GenericCode` elements evaluated in
a context exposing `addComponent` and `addWire`, i.e. **procedural circuit
construction**; and a resolver that deep-copies the template and returns a
`CircuitHolder` of the concrete circuit plus the args that made it — an explicit
elaboration phase, exactly as proposed here.

Digital's design is the right target and its weakness is precise and real: **the
replication mechanism is a script embedded in the drawing.** The replicated
structure is not drawn, is not diffable, cannot be inspected without running it,
and requires learning a bespoke language — which is the same complaint that makes
Verilog `generate` hard to teach, reproduced inside a schematic tool. That is the
opening: JLS's `Array`-as-drawn-element with five closed port modes and
`expand in place` is a genuinely better *teaching* answer, and total-arithmetic
expressions are a genuinely better answer for a format that
`docs/file-format.md:67-71` says is exchanged between untrusting parties.

**Logisim-evolution — has not solved it for drawn circuits.** Verified by code
search on `logisim-evolution/logisim-evolution`:
`src/main/java/com/cburch/logisim/circuit/CircuitAttributes.java` has no parameter
attribute (its only "generic" match is an import of
`com.cburch.logisim.gui.generic.OptionPane`, a package name). Generics exist
*only* on the VHDL-entity component:
`src/main/java/com/cburch/logisim/vhdl/base/VhdlContent.java` declares
`public static class Generic extends VhdlParser.GenericDescription`,
`VhdlEntityAttributes` has a `VhdlGenericAttribute`, and `VhdlParser` matches a
`generic` clause. **The largest peer educational tool can parameterize somebody
else's VHDL and not its own subcircuits** — which is the same trade in miniature.

**DigitalJS — not applicable, and that is the whole argument.** Verified from its
README: it "is designed to simulate circuits synthesized by hardware design tools
like Yosys," with `yosys2digitaljs` converting Yosys output. DigitalJS gets
parameterization for free because the design is Verilog; the schematic is an
*output*, not an input. The tools that scale do not draw; the tools that draw do
not scale.

**Verdict: parity with Digital is the floor and must be reached; the `Array`
element is a plausible leapfrog over everyone, commercial included**, because "the
replication is a drawn object you can open, expand, and count" is a thing no
surveyed tool does. Be honest about the ceiling: JLS will never out-parameterize
SystemVerilog. It can out-*teach* it, and that is the whole of the claim.

---

### Relationship to the existing programs

**This is a new program — P7, parameterization and elaboration — not a slice of
P3.** The distinction is not bookkeeping:

- **P3 is about interchange.** Read `docs/capability-roadmap/README.md:298-377` and
  `docs/capability-roadmap/sweep-03-elements-and-hdl.md:332-380` (C5, the instance
  statement in `HdlModel`) and `:416-450` (C7, port metadata): every item is a
  change to `src/jls/hdl/` plus a save-format component table. **No P3 item changes
  what a student can draw.** P3 makes JLS's existing hierarchy legible to other
  tools.
- **P7 is about authoring.** It changes what a drawing can *express*. It touches
  `src/jls/elem/`, a new `jls.core.elab`, and the editor; it touches `src/jls/hdl/`
  only at the end.

Folding P7 into P3 would bury the largest authoring change in the roadmap inside a
program whose headline is "export your CPU," and it would make P3's 26–38 weeks
into 50–74 with no visible seam.

**Ordering constraints, stated precisely:**

1. **Hard: `Array` lands after P3's reuse-identity slice.** P3's component table
   ("two instances of the same block are the same block",
   `README.md:315-323`) is the same fact `Array` needs to say "these 32 instances
   share one template." Building `Array` first invents a second, parallel notion of
   block identity that will have to be merged later. The *parameter* half of P7 has
   no such constraint and can precede P3 entirely.
2. **Soft, and worth taking: `jls.core.elab` after #77's core extraction.** Same
   argument the roadmap already makes for P1's Stage 5 (`README.md:933-942`) —
   putting a new headless phase inside a boundary that is not yet drawn means
   drawing it as a side effect. Elaboration is far smaller than the value type, so
   this is a preference, not a blocker.
3. **P7 depends on nothing in P1, P4, P5 or P6.** It is parallel-safe against all
   four, and it is the fifth large parallel lane the roadmap's §3 spine does not
   currently show.

**What P7 changes about the other programs:**

- **P2 gets cheaper.** Several P2 items are partly subsumed: a parameterized
  `Memory` wrapper drawn once is a better answer than N compiled-in port
  configurations, and the arithmetic family (`Multiplier`, `Comparator`, …) needs
  fewer built-in width variants. P2's *primitive* additions still matter — a
  parameterized drawing of a bad primitive is still a bad primitive.
- **P3 export gains a decision.** The natural emission is a Verilog
  `parameter` + `generate`. **Emit the elaborated module first** — one module per
  distinct binding — because that is what Yosys sees anyway and it keeps P3's
  round-trip CI property (`README.md:330-338`) provable. Real `parameter`/`generate`
  emission is a later refinement worth ~1–2 weeks and worth deferring.
- **P4 gets its best demo.** The N-sweep critical-path curve above needs both.
- **P6 gets tractable.** "Fit your design in a Tiny Tapeout tile" is exactly the
  exercise of turning one width parameter down until it fits.
- **P5's ERC gets its first new consumer**: the width-mismatch marking in D8.4 is
  an ERC finding, not a modal dialog.
- **Extension points:** `Parameter` and `Array` are `elem.element-provider` +
  `gui.palette-contributor` rows (`docs/extension-points.md:30-31`) — existing,
  typed seams, no new mechanism. The elaborator itself is a concrete core type, not
  a seam; it must not become a plugin point, for the same reason
  `docs/grand-architecture.md` §6 protects the hot loop.

---

### Size and risk

**25–36 maintainer-weeks (6–8.5 maintainer-months).** Comparable to P3, and
estimated by the same method the roadmap uses — analogy to shipped work (#78's
element registry, #199's synchronous memory, #79's format versioning), not
measurement.

| Slice | Weeks | Reasoning |
|---|---:|---|
| Expression language + evaluator + frozen normative grammar doc | 2–3 | The `-t` grammar is the precedent; the document is half the work |
| `Parameter` element (registry, tags, vocabulary, palette, help, three totality tests) | 2 | The documented sixteen-step ritual, once |
| Expression-backed attributes across 14 `bits` classes + `numInputs`/`inputs`/`cap` | 3–5 | Mechanical but wide; `Memory`'s handwritten save (`:440-465`) and `Group`'s `pair`-encoded ranges are the two that are not |
| `jls.core.elab` — elaborator, provenance sids, recursion guard, constraint checks, diagnostics | 4–6 | New phase, but small and pure |
| `SubCircuit` bindings + dialog + re-elaborate in place + width-mismatch marking | 4–6 | The editor half is the larger share; touches `SimpleEditor:5012-5045` and `:5595-5610`, plus undo snapshots |
| `Array` element + five port modes + expansion + open-one-copy + expand-in-place | 6–9 | The biggest item and the one most likely to overrun |
| FORMAT 3, round-trip consistency test, `docs/file-format.md` §7/§9 amendments, `FileFormatSpecTest` | 2–3 | A promise, so it is slow |
| Collab op kinds for bindings and `ArrayGroup` | 1–2 | Pattern already exists at `SetElementConfig:148-152` |
| HDL export of parameters (elaborated modules) | 1–2 | After P3's hierarchy IR only |

**The useful floor is 8–11 weeks**: expression language + `Parameter` element +
expression-backed `bits` + bindings on `SubCircuit` + FORMAT 3, with **no `Array`**.
That alone delivers "one adder drawing, any width" — the single most-requested
thing here — is independently shippable, and reaches parity with Digital's headline
claim.

**The top three ways it goes wrong.**

1. **The elaborator becomes a second simulator.** If the expression language grows
   conditionals over signal values, or if the port modes acquire special cases,
   `jls.core.elab` turns into a compiler nobody can hold in their head — and every
   bug in it will look like a simulation bug, in a tree whose differential oracle
   (`RiscvCpuGoldenTest`) is designed to catch simulation bugs. *Mitigation:* the
   language is total integer arithmetic, frozen and specified before any code; the
   port modes are a closed enum of five and adding a sixth is a design review.
2. **Re-elaboration eats a student's wires.** The moment a width change deletes
   connections, redrawing from scratch becomes cheaper than using the feature.
   *Mitigation:* the decided policy — widths change, wires survive, mismatches are
   marked — plus one atomic undo snapshot per binding change, routed through the
   editor's single mutation entry point rather than around it.
3. **Someone "fixes" the file size by dropping the elaborated plane.** That trades
   the entire compatibility story — zero migration, every old file works, old
   readers degrade gracefully — for bytes in an XZ container. *Mitigation:* make
   "an older JLS loads this file and it simulates correctly" a stated, tested
   property, and keep the byte-identity round-trip test where deleting it is
   visible in review.

**What would make it not worth doing.** If `Array` cannot be made to produce
*readable* drawings — if five port modes turn out to need a sixth and a seventh and
the drawn form stops being self-explanatory — then abandon the replication half and
stop at the floor. Parameterized widths without replication is roughly 70% of the
value at 35% of the cost, and it is still parity with the best peer tool. The
program fails outright only in one scenario: if, after all of it, the answer to "how
do I build a 32-entry register file" is still "run a Python script." If the
`riscv/` CPU cannot be redrawn using this feature, the feature served toy cases
only and the 25–36 weeks bought nothing that matters. **That should be the
acceptance test of the whole program: delete `riscv/jlsbuild.py`'s role as a
producer and draw the CPU.**

---

### Sources

**Repo (verified at HEAD):**

- `src/jls/elem/SubCircuit.java:26-37` (the five fields; no parameter state),
  `:282-288` (inline nested-circuit save), `:299-302` (`saveFormatVersion`
  propagates up), `:332-384` (`copy` = deep structural copy via `Util.copy`/
  `Util.partition`), `:447-467` (`remapPins`), `:571-583` (`initInputs` recursion),
  `:592-611` (`initSim` in stable order), `:621-636` (`react` forwards to inner pins)
- `src/jls/Circuit.java:1008-1024` (nested `CIRCUIT` load; `new Circuit("")` per
  instance), `:1300-1352` (`finishLoad` — the de-facto elaboration: sid minting,
  `el.init(g)`, wire-net partition), `:1478-1484` (nested blocks omit `FORMAT`),
  `:1581-1587` (`formatVersionNeeded`)
- `src/jls/elem/ElementRegistry.java:38-73` (33 registered types)
- `src/jls/elem/Splitter.java:35-56` (`ranges` → puts), `src/jls/elem/Memory.java:440-465`
  (handwritten save; `int bits`, `int cap`), `Mux.java:203,221`, `Gate.java:266,278`,
  `Adder.java:141`, `Pin.java:186`, `Register.java:299`, `Decoder.java:215`
- `src/jls/edit/SubCircuitDialog.java:30-155` (name + orientation only),
  `src/jls/edit/SimpleEditor.java:674-697` (`finishImport`), `:5012-5045`
  (`doModify` opens the subcircuit tab), `:5323-5351` (`doImport` = copy),
  `:5595-5610` (undo `finishDo` re-links via `remapPins`)
- `src/jls/collab/op/ElementVocabulary.java:39-46` (closed 32-tag vocabulary),
  `src/jls/collab/op/SetElementConfig.java:148-152`,
  `src/jls/collab/op/ElementBlocks.java:107-112`
- `src/jls/hdl/HdlExporter.java:418-424` (`EXPORTED` set),
  `src/jls/hdl/imp/NetlistImporter.java:150-160` (multi-module refusal)
- `docs/file-format.md:19-29` (conformance targets), `:45-61` (containers),
  `:67-71` (untrusting parties, 64 MiB bound), `:113-137` (grammar; seven item
  kinds), `:180-195` (version negotiation), `:220-227` (unknown attribute names
  silently ignored), `:291-327` (the 32 frozen tags; `SubCircuit` row at `:321`),
  `:339-354` (`Binder`/`Splitter` `pair` semantics), `:358-360` (recursion),
  `:376-402` (`sid`), `:420-478` (evolution policy; the `initrle` and `sync`
  silent-drop caveats)
- `docs/simulation-semantics.md:128-171` (§5 initialization contract)
- `docs/batch-interface.md:144-200` (§3.2/§3.3 name-ordered hierarchical printing
  and the dotted `QUAL` qualifier — relevant to `Array` instance naming)
- `docs/extension-points.md:30-36` (seam table)
- `docs/capability-roadmap/README.md:260-266` (62 of 228 elements),
  `:298-377` (P3), `:315-323` (reuse identity), `:330-338` (round-trip claim),
  `:850-853` (spine), `:933-942` (#77 ordering constraint)
- `docs/capability-roadmap/sweep-03-elements-and-hdl.md:332-380` (C5 hierarchy IR),
  `:416-450` (C7 port metadata)
- `riscv/jlsbuild.py:1-18` (header: a second implementation of the save format),
  `:271-312` (`emit`); `riscv/build_cpu.py:230-263` (`regfile`, the Python generate
  loop), `:255-261` (32-input read mux); `riscv/make_cpu.py:50-51`;
  `riscv/test_primitives.py` (217 lines, keeps the two implementations in sync)
- Measured: `grep -c "^ELEMENT " riscv/build/addi.jls` = 1038; by tag, 810 `WireEnd`
  → 228 logic elements, **0 `SubCircuit`**

**External:**

- hneemann/Digital README, "Supports generic circuits… barrel shifter with a
  selectable bit width" — **verified** (fetched, github.com/hneemann/Digital).
- Digital's implementation (`GENERIC` attribute, HGS scripting language,
  `GenericInitCode`, `GenericCode` with `addComponent`/`addWire`, deep-copy
  resolver returning a `CircuitHolder`) — **verified** by reading
  `src/main/java/de/neemann/digital/draw/library/ResolveGenerics.java`.
- Logisim-evolution: no parameter attribute in
  `src/main/java/com/cburch/logisim/circuit/CircuitAttributes.java`; generics exist
  only on the VHDL-entity component (`vhdl/base/VhdlContent.java`'s `Generic`,
  `VhdlEntityAttributes.VhdlGenericAttribute`, `VhdlParser`'s `GENERICS`) —
  **verified** by GitHub code search.
- DigitalJS is driven by Yosys output via `yosys2digitaljs`; the schematic is an
  output — **verified** (fetched, github.com/tilk/digitaljs).
- Vivado IP Integrator characterization — **unverified**.
- IEEE 1685 IP-XACT `modelParameters` / configurable elements, and EDIF instance
  `property` — **unverified** (not read here).
