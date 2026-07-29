## A programmatic API: JLS as a platform rather than an application

*Leapfrog sweep 07. Extends `docs/capability-roadmap/README.md`. Every claim about
JLS is anchored to a path in the tree at HEAD; measurements in this document were
taken here and are marked **measured**; claims about other tools are marked
verified or unverified.*

---

### What is missing today

JLS has exactly one programmatic surface: **a command line with one verb**. You
hand `jls -b` a file, it runs to completion, and it prints a human-readable
report. `docs/batch-interface.md` specifies that surface precisely and freezes it
as a stability contract — which is the project conceding the principle and then
stopping at the smallest possible instance of it.

There is no way, from any language including Java, to:

- construct a circuit without writing save-format text;
- ask what element types exist and what attributes they take;
- elaborate a circuit and get diagnostics as data;
- run a simulation and get values as data;
- advance a simulation by a step and look at it;
- run a second circuit in the same process.

Each of those absences has a workaround in the tree, and the workarounds are the
evidence.

#### Workaround 1 — `riscv/jlsbuild.py`: the save format, hand-transcribed into Python

`riscv/jlsbuild.py` is 322 lines whose entire job is to know what
`src/jls/elem/*.java`'s `save` methods write. Its own docstring says so
(`riscv/jlsbuild.py:1-18`): *"Every element emitter below matches the exact save
format and put names used by the JLS element classes under src/jls/elem/."*

It hard-codes, in Python string literals, knowledge that lives in Java:

- put names per element — `outs=[("S", bits), ("Cout", 1)]`, `ins=[("A", bits),
  ("B", bits), ("Cin", 1)]` for `Adder` (`riscv/jlsbuild.py:174-179`);
- attribute names and types — `" int cycle {cycle}"`, `' String orient "RIGHT"'`,
  `" Int init {init}"` (`:147-152`, `:210-218`);
- the fact that `Splitter`/`Binder` encode bit maps as repeated ` pair idx bit`
  items, and that a range's put name is `"HI-LO"` (`:235-268`, `:319-322`);
- the wire-net topology the loader accepts — a driver `WireEnd` carrying `ref
  wire` segments to sink ends, with `int tristate` replicated on every end
  (`:270-312`);
- `sel_bits`, a reimplementation of JLS's `32 - Integer.numberOfLeadingZeros(n-1)`
  (`:26-31`), with the Java expression quoted in the docstring so the reader can
  check it by eye.

None of that is checkable by the compiler on either side. The project knows it,
which is why `riscv/test_primitives.py` exists (217 lines) — *"Validate each
jlsbuild primitive against the real JLS batch simulator"* (`:1`). That file is a
duplicate-knowledge alarm, not a test of a feature. Every element JLS adds under
P2 (register control pins, memory ports, an arithmetic family, a bidirectional
pin) silently makes `jlsbuild.py` more incomplete, and nothing fails.

#### Workaround 2 — `test/jls/CircuitTextBuilder.java`: the same workaround, in-tree, in Java

The project's own tests build circuits by generating save-format text.
`test/jls/CircuitTextBuilder.java` is 422 lines of `StringBuilder` emitting
`ELEMENT Adder\n int bits …\n String orient "UP"\n int delay 12\nEND\n`
(`:70-79`), described in its javadoc as *"the shared extraction of the private
CircuitBuilder inside BatchSimulationGoldenTest"* (`:5-7`) — i.e. this is at
least the second time it has been written. **When a project's own test suite
cannot construct its own model without serializing to text, the model has no
construction API.** `jlsbuild.py` is not an outsider's hack; it is the same
artifact, in a second language.

#### Workaround 3 — `riscv/jlsrun.py`: five regexes over a human-readable report

`riscv/jlsrun.py:20-24` parses simulation results out of prose:

```python
_PIN = re.compile(r"^Output Pin (.+?): 0x([0-9A-Fa-f]+) \((\d+) unsigned, (-?\d+) signed\)$")
_REG = re.compile(r"^Register (.+?): 0x([0-9A-Fa-f]+) \((\d+) unsigned, (-?\d+) signed\)$")
_HIZ_PIN = re.compile(r"^Output Pin (.+?): HiZ$")
_MEMHEAD = re.compile(r"^Changed locations in memory (.+)$")
_MEMLINE = re.compile(r"^ 0x([0-9a-f]+): .* -> 0x([0-9A-Fa-f]+) \((\d+) unsigned")
```

This is why `docs/batch-interface.md` §3.4 is frozen: the value display
`0xH (U unsigned, S signed)` is load-bearing *because* somebody is scraping it.
Note what P1 does to those regexes. The moment a value can be `X`, `_PIN` stops
matching and `jlsrun.py` reports a pin as absent rather than as unknown — a
silent wrong answer in the differential fuzzer. The freeze protects the format
and does nothing for the consumer.

`examples/autograde/autograde.py` is the sanctioned version of the same pattern,
and it grades on `EXPECTED_STDOUT_LINES` (`:52-57`) — literal report bytes. The
capability roadmap already names this: *"the grading criterion is literal bytes
of a report format"* (`docs/capability-roadmap/README.md:574-576`).

#### Workaround 4 — the clock vector as a substitute for a `step` call

`riscv/verify.py:18-25` generates a `-t` file containing one `until <t> <v>` pair
per clock half-period, because there is no way to say "advance 2000 cycles":

```python
def gen_clock(steps: int) -> str:
    parts = ["clk 0"]
    for k in range(1, 2 * steps + 1):
        parts.append(f"until {k * HALF} {1 if k % 2 == 1 else 0}")
```

**Measured:** `riscv/build/k2000_clk.txt` is **193,040 bytes** — 193 KB of text
to express `advanceCycles(2000)`. `docs/standards-adoption/05-riscv-compliance.md`
records the consequence: 38.2 s of scaffolded run against 4.1 s free-running.

#### Workaround 5 — one JVM per experiment

`riscv/fuzz_diff.py:96-99` runs a `ThreadPoolExecutor` over independent `java`
subprocesses because there is no way to run N circuits in one process.

**Measured here, on this tree, `target/jls-5.0.5-SNAPSHOT.jar`, JDK 25.0.3
(min of 3–5 runs):**

| invocation | min wall |
|---|---:|
| `jls -h` (JVM start + class load, no work) | **0.194 s** |
| `-b` on `riscv/build/addi.jls` (1038 elements, 3 cycles) | **0.345 s** |
| `-b` on `riscv/build/k500.jls` (500 cycles) | 0.847 s |
| `-b` on `riscv/build/k1000.jls` | 1.320 s |
| `-b` on `riscv/build/k2000.jls` | 2.336 s |

For the short programs `fuzz_diff.py` actually generates (6–24 instructions,
`:94`), **more than half of every run is JVM startup that a session would pay
once.** That is why the fuzzer is a thing the maintainer runs by hand rather than
a thing CI runs on every push.

#### What the tree lacks structurally, not just conveniently

- **`System.exit` is the error channel.** 77 call sites across `src/`
  (`grep -rn "System.exit" src/ | wc -l`). `JLSStart.start`'s batch branch exits
  on every failure path (`src/jls/JLSStart.java:189-233`). A library may not do
  this.
- **Process-global mutable state.** `JLSInfo.sim`, `JLSInfo.batch`,
  `JLSInfo.frame`, `JLSInfo.loadError`, `JLSInfo.printTrace`
  (`src/jls/JLSInfo.java:75-101`), with 163 `JLSInfo.` references in `src/`.
  Two designs cannot coexist in one JVM. `grand-architecture.md:79` already names
  this: *"`JLSInfo` is a ~640-reference public-static hub wiring everything to
  everything."*
- **Diagnostics are strings in a static.** `Circuit.load` returns `boolean` and
  leaves the reason in `JLSInfo.loadError` (`src/jls/Circuit.java:692`;
  `JLSStart.java:203-217`). `LoadError` exists as a structured type
  (`src/jls/LoadError.java`) and is then flattened to a string on the way out.
- **The element schema is half data.** `Element.savedAttributes()`
  (`src/jls/elem/Element.java:306-311`) is a declarative attribute list — exactly
  the schema a catalog would publish — but 17 of the 33 registered types use it;
  `Memory`, `SubCircuit`, `Splitter`/`Binder` (via `Group`), `Extend` and
  `WireEnd` still carry handwritten `setValue` overrides
  (`grep -l "public void setValue" src/jls/elem/`). And `Element.setValue(String,
  int)` **silently ignores an unknown attribute name** (`:344-350`: the loop
  falls through and returns) — acceptable for a loader reading files JLS wrote,
  unacceptable as an API where a typo becomes a no-op.
- **Stepping exists, entangled with Swing.** `Simulator.beforeEvent`
  (`src/jls/sim/Simulator.java:252`) is the documented hook for pausing and
  stepping, and its only override is in `jls.edit.InteractiveSimulator:736`, a
  1437-line Swing class that blocks the sim thread on a `Semaphore` and posts
  `SwingUtilities.invokeLater` from inside the hook (`:741-766`). The mechanism is
  built; it is on the wrong side of the boundary.
- **The roadmap already promises an API it never designs.**
  `docs/capability-roadmap/sweep-04-verification.md:647-651` disposes of #59
  VPI/PLI, #60 VHPI and #61 DPI-C with: *"The capability they standardize —
  external programs observing and driving a run — is **E**'s embeddable API,
  natively and without a C boundary."* Change **E** in that sweep is a stimulus
  generator (`:370-400`), not an API. **Two survey entries are currently retired
  against a program that does not exist.** That is the gap this document fills.

---

### The capability

Two faces over one substrate.

#### (a) `jls.api` — the in-process Java API over the headless core

A small sealed surface in a new AWT-free package `jls.api`, covered for free by
`HeadlessCoreRatchetTest` and `ArchitectureRulesTest.coreDependsOnNoGuiClasses`.
Five nouns, no more:

1. **`Design`** — a circuit tree with an identity. `Design.load(Path)`,
   `Design.load(Reader)`, `Design.empty(String)`. Returns
   `Result<Design, Diagnostics>` — never `boolean` plus a static string, never
   `System.exit`.
2. **`Edit`** — mutation, and **every verb is a `jls.collab.op.CircuitOp`**
   (`src/jls/collab/op/`, 20 files, shipped under #167). `addElements`,
   `addWire`, `removeWire`, `removeElements`, `setElementConfig`, `attachProbe`,
   `toggleWatched`, `move`, `rotate`, `flip`. The API gets validation,
   atomic rejection (*"a rejected op leaves the circuit byte-identical"*,
   `docs/operation-layer.md:18-20`), exact inverses, and a serialized form —
   all already built and tested — and it gets **undo for free**. It also cannot
   construct a circuit the editor could not.
3. **`Elaboration`** — `Circuit.finishLoad` (`src/jls/Circuit.java:1300`) made a
   value: resolved nets, resolved subcircuit port maps, and diagnostics as
   structured data. `finishLoad` already takes `jls.core.@Nullable TextMetrics`,
   so the headless path exists.
4. **`Run` and `Session`.** `Run` is batch: `Run.of(design).limit(t)
   .stimulus(s).vcd(path).execute()` → `RunResult` with the outcome enum, watched
   values, traces (`BatchSimulator.getTraceSamples`, `:329`), and — once P5
   lands — assertion results and coverage. `Session` is the stepping handle:
   `set(pin, value)`, `advanceTo(t)`, `advanceBy(dt)`, `advanceCycles(n, pin)`,
   `read(pin)`, `netValue(probe)`, `close()`. It is a second consumer of
   `Simulator.beforeEvent`, headless, with no `Semaphore` and no EDT.
5. **`Catalog`** — the schema, published as data: per `ElementType`
   (`src/jls/elem/ElementRegistry.java:37-72`) the tag, the port list (name,
   direction, width rule), and the attribute list (name, kind, default,
   constraints) from `savedAttributes()`. This is the piece that makes a
   generator library *generated* rather than transcribed, and it is the direct
   answer to `jlsbuild.py`.

**How it stays stable.** A normative `docs/api-interface.md` under the identical
promise `docs/batch-interface.md` §6 carries: a change that alters anything a
conforming consumer could observe requires a CHANGELOG entry plus a major bump or
a compatibility flag. Enforced, not merely written, by an **API-surface ratchet
test**: a checked-in signature file of every public member of `jls.api`, compared
in CI, regenerated only alongside the CHANGELOG entry. That is the same mechanic
as `FileFormatSpecTest` (which *"fails when this document and the code drift
apart"*, `docs/file-format.md:31-33`) and `ExtensionPointCatalogTest`'s
two-directional cross-check. This project has shipped three frozen text formats
and four ratchet tests; it knows how to hold a contract.

#### (b) The scripting face — the decision

**Recommendation: a documented NDJSON request/response protocol over stdio,
started by `jls --serve`, generated client libraries shipped alongside.** Not an
embedded interpreter; not a generator library alone.

The three options, priced against this project's stated constraints:

**An embedded interpreter (JSR-223 / GraalJS / Jython / Rhino).** Rejected on
three grounds, of which the licensing one is the weakest.

- *The single-jar constraint.* `grand-architecture.md:29-34` calls the
  self-contained jar the constraint that *"more than any other, decides the
  plugin model."* The jar is **2.6 MB measured** (`target/jls-5.0.5-SNAPSHOT.jar`).
  GraalJS is an order of magnitude larger than the whole application (*exact
  figure unverified in this pass*). Rhino is small but is ES5-era JavaScript.
- *The ecosystem.* The language instructors and researchers actually write
  analysis in is Python, and there is no current embeddable Python for the JVM —
  Jython is Python 2.7 and unmaintained (*unverified as of 2026; it was true at
  last check and no Python-3 Jython release is known to me*). Embedding
  JavaScript to serve a Python audience is a category error, and the tree's own
  2,533 lines of programmatic use are Python.
- *Trust and the hot plane.* `ARCHITECTURE.md`'s recorded #222 decision puts
  untrusted code out of process and says in-process external code *"has full JVM
  authority; that must be stated plainly wherever the opt-in is offered."* A
  student's assignment script is untrusted code. And an in-process interpreter is
  one convenience method away from user code inside `runEventLoop`, which
  `grand-architecture.md` §6 forbids in the strongest language it uses anywhere.
- *Licensing, for completeness.* GPLv3 permits linking GPL-compatible code;
  Rhino's MPL-2.0 and GraalJS's UPL-1.0 are both GPL-compatible (*license
  identifications not re-verified in this pass*). So the linking question is
  answerable — it is simply not the deciding one. Out-of-process makes it moot,
  which `ARCHITECTURE.md:314-318` already observes for Yosys/GHDL/ELK.

**A generator library that emits `.jls`.** This is what exists. Its ceiling is
exactly `jlsbuild.py`'s: it can **write** and never read, never elaborate, never
simulate, never step, never query, and it duplicates knowledge that drifts
silently. It should survive — but as a **generated** client over `Catalog`, so
that adding `Register.CLR` under P2 updates the Python builder automatically
instead of making it quietly wrong.

**The stdio protocol — why it wins.**

1. **Language neutrality is the whole point, and the project's own most ambitious
   user is not in Java.** Python, R, Julia, MATLAB, a shell script, a Jupyter
   notebook, a Node CI job — all drive a byte stream. Neither of the other two
   options serves more than one language.
2. **It costs the jar nothing.** Zero new dependencies. JLS needs to *emit* JSON
   and *parse* a bounded ~30-verb request grammar; that is a few hundred lines
   under the `UntrustedFileHardeningTest` discipline the repo already applies to
   circuit files and to the op grammar (`docs/operation-layer.md:23-28`: *"strict:
   unknown kinds, unknown fields, malformed values, and oversized input are
   rejections, never repairs"*). No JSON library, no license question, no
   `module-info` friction.
3. **It is the boundary the project already chose.** #222 reserves out-of-process
   for untrusted providers and names KiCad's IPC model; §4.3 cites it. This is
   that reservation being drawn for the first case that needs it, rather than
   speculatively.
4. **It amortizes the cost the workaround actually pays** — 0.194 s of JVM start
   per experiment, measured above, and 193 KB of clock-vector text per long run.
5. **It is testable the way this project tests things.** A line protocol is a text
   format. It gets a normative document, a byte-exact golden in the shape of
   `VcdExportGoldenTest`, and a spec-derived parser in the test tree the way
   `VcdExportGoldenTest` re-checks the VCD *"with a parser written from this
   document rather than from the emitter"* (`docs/batch-interface.md:318-320`).
   An embedded interpreter's surface has no comparable discipline available.
6. **It does not fork the design.** The protocol is a thin server module over
   `jls.api`. Choosing it does not mean not building face (a) — it means face (a)
   ships with two clients instead of zero.

**Concrete shape.** One JSON object per line, `{"id":…,"op":…,…}` in,
`{"id":…,"ok":true,…}` or `{"id":…,"ok":false,"error":{…}}` out. NDJSON rather
than LSP-style `Content-Length` framing because the framing buys nothing at this
scale and costs a spec section. Verb groups: lifecycle (`open`/`new`/`save`/
`saveText`/`close`), schema (`catalog`/`describe`), mutation (one verb per
`CircuitOp` kind, plus `undo`), `elaborate`, `run`, `session.*`, `export`,
`image`, and — as P4/P5/P6 land — `sta`, `assertions`, `coverage`, `cells`.

**Two design rules that must be set on day one, not retrofitted:**

- **Values cross the boundary as four-state-capable strings** (`"1010"`,
  `"10xz"`, `"z"`), never as integers, **from the first release, while the
  simulator is still two-state and every character is `0` or `1`.** This costs
  nothing now and is the single decision that determines whether P1 breaks the
  protocol or extends it.
- **The protocol has no callback direction.** Strictly request/response; JLS never
  initiates. This is written into the normative document as a permanent
  constraint, the way `batch-interface.md` §6 writes the format freeze — see the
  #63 discussion below.

#### The #63 question, answered rather than dodged

`docs/vcd-interop.md:18-22` records live co-simulation as rejected and says
*"Graders must not depend on interacting with a running simulation."*
`examples/autograde/autograde.py:14-19` repeats it. Two honest statements:

- **Keep the rejection.** JLS as a *guest* in another simulator's time wheel —
  VPI/DPI/FLI, cocotb attaching to a JLS kernel, two event queues to
  synchronize, a foreign scheduler owning `now` — is a different tool class and a
  permanent maintenance obligation. Nothing here proposes it, and #63 should not
  be reopened.
- **`session.*` is the opposite direction and must be argued as such.** JLS owns
  the clock. The client asks JLS to advance and then reads. There is exactly one
  event queue and one owner of `now`, and the mechanism is the pause/step hook
  the GUI has used since `Simulator.beforeEvent` was written
  (`src/jls/sim/Simulator.java:252`, doc comment: *"A mode can block (pause), or
  set state and decline this iteration"*). This is a second consumer of an
  existing seam, not a new execution model.
- **`docs/vcd-interop.md` needs one paragraph of amendment**, and it is cheap
  because that document is *informative*. `docs/batch-interface.md` — which is
  normative — is **untouched**: the protocol is a new flag and a new stream, and
  §6 already covers *"a new flag, a new optional output gated behind a new flag"*
  as minor-version material.

#### Relationship to #212 and the module system

- The API is the **demand generator** for #212, and the answer to the question it
  will generate should be settled in advance: **scripts compose existing
  elements; new element *behaviours* are a Java module.** A scripted element would
  put client code on the hot plane, which §6 forbids categorically, and it would
  need a value-domain crossing per event. #212's `ServiceLoader`/trusted-jar path
  stays the way to add an element. That boundary is defensible, teachable, and
  identical to the one KiCad drew.
- **The API is itself a module**, and a textbook one:
  `ModuleManifest("app.api", 1, provides=["api"], requires=["core"], …,
  activation=OnCommand("serve"))` — the first non-GUI consumer that makes
  `Activation.OnCommand` (`src/jls/module/Activation.java`) earn its keep. The
  stdio server is a second module `requires: ["api"]`.
- **Two new seams for `docs/extension-points.md`**, named first per that
  document's own rule (*"pending seams are named here first, so nobody invents a
  parallel mechanism"*, `:22-24`):
  - `app.api-verb` — a module contributes protocol verbs. This is how P3's
    EDIF/BLIF/IP-XACT printers, P4's STA, P5's coverage and P6's cell mapping
    reach scripts without any of them editing the protocol core.
  - `app.report-writer` — **already requested by P5** (`README.md:966-970`). The
    API is its second consumer, which is the argument for designing it once.

---

### What it unlocks

#### Standards (survey entry numbers)

- **#58 cocotb** — currently ADJACENT on the ground that JLS has *"no
  live-stepping API"* (`sweep-04:57`). The capability arrives natively; the entry
  moves to delivered-differently, and the sweep's own reasoning ("there is a
  strictly better JLS-native answer") is finally true rather than promissory.
- **#59 VPI/PLI, #60 VHPI, #61 DPI-C** — the sweep retires all three against
  *"**E**'s embeddable API"* (`sweep-04:647-651`). This program is that API. The
  entries stay declined-as-languages and become delivered-as-capability.
- **#52 PSS, #51 `e`** — retargetable test intent is "stimulus is a program,"
  which is what a scripting face makes true. Combined with P5's `Stimulus` SPI
  the pair is complete.
- **#65 / #259 RISCOF** — the plugin's whole job is "run many compiled programs
  and diff signatures." A session process makes the harness ~2× cheaper on short
  tests (measured: 0.194 s of 0.345 s is startup) and removes the 193 KB clock
  vector entirely.
- **#57 VUnit (artifact shape)** and **#53 UCIS** — a structured report channel
  and a coverage query surface are the same design problem as P5's report
  channel; see the ordering note below.
- **Indirectly, every printer in P3 and P6** — a printer with a script driver is
  a pipeline stage; a printer without one is a menu item.

#### Engineering capabilities

- **`riscv/` stops being a text generator.** `jlsbuild.py` (322 lines) deletes;
  `jlsrun.py`'s five regexes (`:20-24`) delete; `test_primitives.py` (217 lines)
  becomes unnecessary because there is no second transcription to validate;
  `verify.py:gen_clock` becomes `session.advanceCycles(n)`. **This is the
  program's acceptance test: it is done when no save-format string literal
  appears anywhere in `riscv/`.**
- **`fuzz_diff.py` moves into CI.** Not because it gets faster in principle but
  because it stops paying a JVM per program — the concrete difference between a
  thing the maintainer runs and a thing that runs on every push.
- **`test/jls/CircuitTextBuilder.java` (422 lines) becomes a wrapper**, and every
  element added under P2 gets a builder from `Catalog` for free instead of a
  hand-written factory in two languages.
- **Third-party tooling becomes possible at all**: a `pytest` fixture, a Jupyter
  kernel, an LMS grading plugin, a VS Code extension, a batch layout-cleanup
  script, a corpus generator for testing JLS itself. None of these can exist
  today at any price.
- **Research use.** "Simulate 10,000 mutated variants of this circuit and measure
  how many a given test set catches" is a mutation-testing experiment over
  hardware, and it is currently a week of shell scripting and 10,000 JVM starts.

#### Teaching capabilities — what a student can do afterwards that they cannot today

- **Procedurally generated assignments — the highest-value item here.** Today an
  instructor authors one circuit and one `-t` file, and thirty students share one
  answer. With the API, a ~60-line script emits thirty adders of different widths
  with different injected faults, thirty stimulus files, and thirty answer keys —
  and grades them, because grading is the same script run backwards. Every student
  gets a different but equivalent problem. This converts collusion from a
  policing problem into a non-problem, and it is the single thing instructors ask
  for that JLS structurally cannot do.
- **A student runs their own experiment.** *"Does a 16-bit ripple-carry adder
  really take twice as long as an 8-bit one?"* is today a hand-drawn circuit and
  a stopwatch. Afterwards it is a ten-line loop over widths, and the student
  **plots it**. First-year students already learn Python in a parallel course;
  this is the first point at which the two courses touch, and the payoff is that
  hardware becomes something you can measure rather than something you are told
  about.
- **A generator lab.** *"Write a program that emits an N-bit carry-lookahead
  adder for any N; check N = 4, 8, 16, 32 against the built-in `Adder`."* That is
  a generator plus an equivalence check — a genuinely good assignment that no
  schematic editor can currently set, and the exact skill a student needs the
  first time they meet a parameterized Verilog module.
- **Instructor-authored analysis.** With P4, "print every student's critical path
  and fmax into a spreadsheet" is a script. With P5, "which truth-table rows did
  this student's tests never touch" is a rubric line rather than a screenshot.
  With P6, "cell count per submission" is a leaderboard.
- **The `riscv/` trajectory without the hack.** The CPU already exists
  (`riscv/build/addi.jls`: **1038 elements, 228 non-wire, 9367 lines**, measured).
  Afterwards, extending it — sub-word memory, a pipeline, a cache — is Python
  against a checked API instead of Python against a string format, and a student
  can do it.

---

### Competitive position

**Commercial.** Cadence Virtuoso has SKILL (a Lisp dialect); Synopsys Design
Compiler, Siemens Questa, Xilinx/AMD Vivado and Intel Quartus all have Tcl, most
with a Python layer bolted on top. *(These are widely known and were not
re-verified by fetch in this pass — treat as unverified in specifics, though the
pattern is not in doubt.)* In all of them the **GUI is a debugger for a flow that
is scripted**, because a professional flow must be reproducible, diffable,
code-reviewable, and runnable in a regression farm overnight.

**Is "an EDA tool without a scripting interface is a toy in professional terms"
fair?** Yes, and for a precise reason: a tool that can only be driven by a human
produces results that cannot be reproduced. **The educational equivalent is the
autograder** — and JLS has already conceded the argument, because
`docs/batch-interface.md` exists, is normative, and is frozen. JLS is not on the
wrong side of this; it is one verb into it.

**Open.** Verilator is the strongest form of the idea in the field: its *output
is an API* — a C++ class you instantiate and clock from your own `main()`
(verified by citation in this repo, `grand-architecture.md:357`). Yosys has a
command language and Python bindings (*unverified specifics*). KiCad has a Python
API and a newer out-of-process IPC API — cited by this repo at
`grand-architecture.md:356`, so verified as a claim about KiCad's direction.
GTKWave has Tcl scripting (*unverified*).

**Peer educational.** DigitalJS is distributed as a JavaScript library, so its
API *is* its primary surface — the one peer that genuinely leads here
(`grand-architecture.md:355` cites the repo; *specifics unverified*).
hneemann's Digital and Logisim-evolution both ship headless test-vector CLIs
(cited at `grand-architecture.md:374-375`); I found **no evidence in this repo,
and did not fetch either project in this pass, of a programmatic construction or
stepping API in either — unverified.**

**Where the incumbents are genuinely weak — and this is not manufactured.** Tcl
and SKILL are bad languages by 2026 standards, which everyone in the industry
knows; that is why every vendor has a thin, under-documented Python shim over the
Tcl one. More importantly, and specifically:

1. **They are undiscoverable.** There is no machine-readable catalog of what
   commands exist with what arguments. You learn a vendor API from a PDF.
2. **They are unversioned.** No vendor scripting surface carries the kind of
   promise `docs/batch-interface.md` §6 makes. Flows break across tool releases;
   this is a standing operational cost in every EDA shop.
3. **They are in-process and therefore language-locked and unsafe.**

**What JLS's version would be.** A **published `catalog` verb** so client
libraries are generated rather than transcribed; a **ratchet-tested, versioned
stability contract** the project has already demonstrated it can hold for three
formats; **language neutrality by construction**, because the boundary is a byte
stream; and a **specifiable-in-full** surface — five nouns, ~30 verbs — which is
possible precisely because JLS is small. Cadence cannot publish its whole API
surface as a contract; JLS can.

**Verdict: parity on the existence of the capability — it is table stakes and JLS
lacks it — and leapfrog on the quality of the contract.** The leapfrog is not
"we are cleverer"; it is "our surface is small enough to be a promise, and this
project has already proved it keeps promises about text formats."

**Where JLS cannot plausibly lead, stated plainly:** it will never have SKILL's
twenty-five years of accumulated flow libraries, and it should not chase Tcl
compatibility for familiarity's sake. An SDC-subset parser (already sized at
`docs/capability-roadmap/sweep-02-timing.md:522`) is a different, legitimate,
narrow thing and must not be confused with a general Tcl interpreter.

---

### Relationship to the existing programs

**This is a new program — P7 — with an unusual property: it is the only one that
makes the other six reachable from outside the GUI.** Every one of P1–P6
terminates in a capability whose sole consumer is a human clicking. P4's STA
produces a critical path; without an API it is a picture. P5's coverage produces
numbers; without an API they are a dialog. P6's cell mapping produces a cell
count; without an API it is a status bar. P7 is the multiplier, not a seventh
peer.

**Ordering constraints, hardest first:**

1. **Depends on #77 (headless core) — harder than any other program does.** The
   roadmap §5 says P1's `LogicValue` can land inside `jls.core` before the
   extraction, accepting that P1's element pass doubles as part of it. **P7 has no
   such escape**: its entire content is "the core, addressable," and today
   `JLSInfo` is a process-global hub (163 refs), `System.exit` is the error
   channel (77 sites), and there is no boundary to publish. **#77 before P7's
   face (a), non-negotiable.** Conversely, P7 is the strongest available argument
   *for* #77, because it converts "better layering" into a shipped capability.
2. **Depends on #167 (operation layer, shipped) for mutation** — and on its two
   named next steps (`docs/operation-layer.md:153-161`): preview-then-commit for
   move/placement/wiring, and wiring the dialog commits onto `SetElementConfig`.
   The API needs `SetElementConfig` to actually mutate, not merely to exist as a
   record in the sealed list.
3. **Co-design with P5, or duplicate it.** P5's report channel and exit-status
   contract carries the roadmap's own instruction: *"**This must be designed
   first**, because it is a change to a promise"* (`README.md:541`). P7's protocol
   contract is *the same design problem* — how does a run report structured
   results. Doing them independently produces two structured-result formats.
   **Recommendation: design P5's report channel as the first `jls.api` surface
   rather than as a flag**, and if only one ships, that one.
4. **Parallel with P1 and P2**, with one trap: the API must not freeze `BitSet`
   or an integer value encoding into its wire format. The four-state-string rule
   above is the mitigation and costs nothing to apply immediately.
5. **Feeds P3, P4, P6 delivery**, not their construction. Each contributes verbs
   through `app.api-verb` when it lands; none blocks P7 and P7 blocks none.

Against the roadmap's staging table (§4), P7's useful floor sits naturally
**after Stage 2** (the verification floor, which supplies the report channel) and
**alongside Stage 5** (`LogicValue`), where it costs the maintainer a visible
capability during the roadmap's one deliberately invisible stretch — which is
precisely the scheduling problem §4 says it is solving for.

---

### Size and risk

Estimated by analogy to shipped work the repo records (#167's op layer, #78's
registry, #72's VCD emitter and its normative document), which is the roadmap's
own stated basis.

| Slice | Weeks | Reasoning |
|---|---:|---|
| `jls.api` surface design + normative `docs/api-interface.md` | 2–3 | Design, not code; it is a promise, and this project prices promises separately (P5's report channel: 1 week, *"small, but it is a promise"*) |
| De-globalization: `System.exit` off the library path (77 sites), `JLSInfo` statics → injected context (163 refs), `TellUser` sink injection, `LoadError` as returned data | 4–7 | The largest and least glamorous chunk; #77's uncollected debt. `TellUser` is already the single enforced seam (`NotificationRatchetTest`), so this is one indirection, not a hunt |
| `Catalog` + finishing the `Attribute` conversion (`Memory`, `SubCircuit`, `Group`, `Extend`, `WireEnd`) | 2–4 | `Memory` alone is 1547 lines and the roadmap already says schedule it alone |
| `Design`/`Edit`/`Elaboration`/`Run` over ops + `BatchSimulator` | 3–4 | Mostly assembly; the ops and the simulator exist |
| `Session` (headless stepping, second consumer of `beforeEvent`) | 2–3 | Co-design with P5's `afterTimestamp` closure; the GUI's version is the reference implementation |
| NDJSON server + hostile-input hardening + `--serve` flag | 3–4 | ~30 verbs; reuses the op grammar's strictness discipline |
| Generated Python client + porting `riscv/` onto it + deleting `jlsbuild.py` | 2–3 | The acceptance test |
| API-surface ratchet + protocol golden + spec-derived parser test | 1 | Same shape as `FileFormatSpecTest` / `VcdExportGoldenTest` |
| **Total** | **19–29** | **4.5–7 maintainer-months** |

**The useful floor is 8–11 weeks**: de-globalization + `Design`/`Run`/`Catalog` +
the protocol + the Python client, with no `Session` and no `Attribute`
completion. That floor already deletes `jlsbuild.py`, deletes `jlsrun.py`'s
regexes, and puts `fuzz_diff.py` in CI.

**Top three ways it goes wrong.**

1. **The surface is frozen before the core is stable, and P1 breaks it.** If the
   protocol says `"value": 173` and P1 arrives with per-bit X/Z/U, the protocol
   either lies or breaks — and it will be *frozen* by then, because that is the
   point of it. Mitigation is the four-state-string rule, applied from the first
   release while every character is still `0` or `1`; likewise emit a timescale
   field before P4 needs one, and a `strength` field that is always `"strong"`
   before P1-S3 needs one. Cheap now, unaffordable later.
2. **It becomes a second, divergent mutation path.** If `jls.api` grows
   `setBits()` / `addGate()` beside the op vocabulary, the project acquires a
   mutation path untested against undo, collaboration, and the canonical save —
   and #167's whole rationale evaporates. Mitigation: **mutation verbs are
   `CircuitOp`s or they do not exist**, enforced by a ratchet test; the repo has
   roughly eight such ratchets and knows how to write another.
3. **Scope creep into co-simulation.** `Session` is one feature request away from
   "let me register a callback that runs inside the event loop" — a hot-plane
   violation and a #63 reopening in one move. Mitigation: **no callback direction
   at all**, written into the normative document as permanent, with the reasoning
   attached so a future maintainer knows it is a decision rather than an omission.

**Runner-up risk worth naming:** the protocol becomes the *only* face, and the
Java API is never cleanly extracted — leaving JLS with a text protocol over the
same static-global mess, i.e. a nicer CLI. The guard is sequencing: the
de-globalization slice ships and is ratcheted *before* the server slice starts.

**What would make it not worth doing.** One condition, and one non-condition.

- **Not worth doing if #77 never lands.** Built against `JLSInfo`'s static hub,
  the API's contract would be "one design per process, exits on error" — which is
  the CLI JLS already ships, dressed in JSON. In that world this is not a 19–29
  week program; it is that plus #77, and the honest move is to do #77 first and
  call it that.
- **Not a valid objection: "first-year students and their instructors will never
  write a script."** The tree falsifies it. `riscv/` is 2,533 lines of Python
  written by this project, and `examples/autograde/autograde.py` is a script this
  project ships as the *documented* grading pattern. The demand is not
  hypothetical; it is already in the repository, paying for the absence in
  transcription and regexes.

---

### Sources

**Repo (all verified at HEAD in this pass):**

- `riscv/jlsbuild.py:1-18` (save-format transcription docstring), `:26-31`
  (`sel_bits`), `:147-152`, `:174-179`, `:210-218` (attribute/put literals),
  `:235-268`, `:319-322` (`pair` encoding, range names), `:270-312` (`emit`,
  wire-net topology)
- `riscv/jlsrun.py:12-17` (jar discovery), `:20-24` (five report regexes),
  `:37-43` (subprocess invocation)
- `riscv/verify.py:18-25` (`gen_clock`), `:66-77` (`problems` scoreboard)
- `riscv/fuzz_diff.py:94-99` (JVM-per-program thread pool)
- `riscv/make_cpu.py:50-60`, `riscv/test_primitives.py:1`, `riscv/README.md`
  ("Files" table; closing line *"Nothing here modifies JLS itself"*)
- `test/jls/CircuitTextBuilder.java:1-13`, `:70-79` (422 lines total)
- `test/jls/RiscvCpuGoldenTest.java:41-70` (load/run/assert shape)
- `examples/autograde/autograde.py:14-19` (co-sim rejection), `:52-57`
  (`EXPECTED_STDOUT_LINES`)
- `src/jls/JLSStart.java:154-280` (batch branch, `System.exit` paths), `:672`
  (`displayResults`)
- `src/jls/JLSInfo.java:75-101` (mutable statics); `grep -rn "JLSInfo\." src/ | wc
  -l` = **163**; `grep -rn "System.exit" src/ | wc -l` = **77**
- `src/jls/Circuit.java:342` (`addElement`), `:692` (`load`), `:1300`
  (`finishLoad`), `:1466` (`save`)
- `src/jls/elem/Element.java:199-306` (`BASE_ATTRIBUTES`), `:306-311`
  (`savedAttributes`), `:344-350` (`setValue` silently ignoring unknown names)
- `src/jls/elem/ElementRegistry.java:37-72` (33 types)
- `src/jls/sim/Simulator.java:215` (`runEventLoop`), `:252` (`beforeEvent`),
  `:269` (`afterEvent`), `:285` (`probeSample`)
- `src/jls/sim/BatchSimulator.java:112` (`runSim`), `:329` (`getTraceSamples`),
  `:562` (`displayOutcome`)
- `src/jls/edit/InteractiveSimulator.java:736-766` (the only `beforeEvent`
  override; Swing-entangled stepping), 1437 lines
- `src/jls/module/{ModuleManifest,ExtensionPoint,ExtensionRegistry,Activation}.java`
- `src/jls/collab/op/` (20 files); `docs/operation-layer.md:16-30`, `:153-161`
- `docs/extension-points.md:22-24` (name-pending-seams-first rule), `:28-36`
  (seam table)
- `docs/batch-interface.md` §1, §3.2-3.4, §6 (stability promise)
- `docs/file-format.md:31-33` (`FileFormatSpecTest` drift guard)
- `docs/vcd-interop.md:18-22`, `:113-114` (#63 rejection)
- `docs/grand-architecture.md:29-34` (single-jar constraint), `:75-84` (#77),
  `:176-180` (lazy activation = #212's gate), `:203-212` (isolation),
  `:314-342` (hot/cold planes), `:353-357` (tool citations)
- `ARCHITECTURE.md:294-327` (#222 trust boundary), `:344-368` (#221 execution
  strategy)
- `docs/capability-roadmap/README.md:541` (P5 report channel designed first),
  `:574-576` (autograde on report bytes), `:918-1013` (§5, relationship to
  grand-architecture), `:966-970` (P5's three pending seams)
- `docs/capability-roadmap/sweep-04-verification.md:57` (#58 cocotb),
  `:370-400` (change **E**), `:647-651` (#59/#60/#61 retired against "**E**'s
  embeddable API")
- `docs/capability-roadmap/sweep-02-timing.md:522` (SDC as a Tcl subset)
- `docs/standards-landscape.md:215-232` (survey entries #48-#65)
- `docs/standards-adoption/05-riscv-compliance.md:207-215` (runtime budget)
- `pom.xml:19-24` (GPLv3), `:43` (Java 25), `:231-240` (shade plugin — the single
  jar)

**Measured in this pass** (JDK 25.0.3, `target/jls-5.0.5-SNAPSHOT.jar`, min of
3–5 runs): `jls -h` 0.194 s; `-b` on `addi.jls` 0.345 s; `k500` 0.847 s; `k1000`
1.320 s; `k2000` 2.336 s. Jar size 2,609,004 bytes. `riscv/build/k2000_clk.txt`
193,040 bytes. `riscv/build/addi.jls` 1038 `ELEMENT` blocks (810 `WireEnd`, 228
logic), 9367 lines. `riscv/*.py` 2,533 lines total.

**External claims and their status:**

- KiCad Python API and out-of-process IPC API — **verified by repo citation**
  (`grand-architecture.md:356`, `dev-docs.kicad.org/en/apis-and-binding/ipc-api/`).
- Verilator's elaborate-to-flat model as a driveable C++ class — **verified by
  repo citation** (`grand-architecture.md:335-340`, `:357`).
- Cadence SKILL; Tcl in Synopsys / Siemens / Vivado / Quartus; GTKWave Tcl;
  Yosys Python bindings — **unverified in this pass** (widely known; not fetched).
- GraalJS jar size and UPL-1.0 licensing; Rhino MPL-2.0; Jython's Python-2-only
  status — **unverified in this pass**. The recommendation does not rest on any
  of them: the single-jar and ecosystem arguments stand independently.
- hneemann's Digital and Logisim-evolution lacking a programmatic construction or
  stepping API — **unverified**. Neither project was fetched; this repo cites both
  only for their element-registration mechanisms
  (`grand-architecture.md:353-356`, `:363-375`).
- DigitalJS being distributed as a JavaScript library (hence API-first) —
  **unverified in specifics**; repo citation at `grand-architecture.md:355`.
