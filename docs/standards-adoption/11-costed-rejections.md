## The costed rejections: SDF (#89), EDIF (#74), JEDEC JESD3-C (#83), IEEE 1149.1 BSDL (#129)

Four entries the landscape survey pushed away — two into §13.3
("deliberately not recommended"), two into the "teaching value, no
demand yet" bucket. A rejection with no price on it is a shrug. This
section prices each one so the maintainer can say *"we decided not to,
and here is the number"* rather than *"we never got to it."*

Each item gets the same six subheadings. Depth is spent on the decision
and its trigger, not on implementation detail the project has decided
not to write.

Two conventions used throughout:

- **"Self-asserted"** means no external body assesses conformance. All
  four of these are self-asserted; §"Certification / conformance
  procedure" below defines once what a *credible* self-assertion looks
  like in this repo, and each item says how far short it would fall.
- A note on naming collisions before anyone greps: `src/jls/hdl/scan/`
  is the **HDL header scanner** (`VerilogHeaderScanner`,
  `VhdlEntityScanner`, issue #63). It has nothing to do with boundary
  scan. JLS has no JTAG code today.

**What a credible self-assertion consists of, in this project.** The
repo already has a house pattern for "we conform to X", visible in the
VCD (#66) and PCF (#81) work, and it has four parts:

1. a normative document under `docs/` in RFC 2119 language that cites
   the clause numbers of the source standard and lists deviations
   explicitly (`docs/batch-interface.md` §4 is the model — it names the
   VCD profile *and* its known deviation);
2. byte-exact golden files under `test/resources/` with a `FooGoldenTest`
   asserting them (`test/jls/VcdExportGoldenTest.java`,
   `test/jls/hdl/board/PcfGoldenTest.java`);
3. an **independent third-party consumer** run in CI with
   skip-when-absent behavior, so the claim is checked by software the
   project did not write (`test/jls/hdl/IverilogCompileTest.java` +
   `test/jls/hdl/ToolLocator.java`);
4. a scope sentence in `README.md` saying what is *not* claimed.

Any of the four items below that ships without all four parts is
publishing a format, not claiming conformance. The distinction matters
most for EDIF, where part 3 is unobtainable.

---

### 1. IEEE 1497 SDF (#89) — timing back-annotation

#### What conformance actually means

IEEE Std 1497-2001, *Standard Delay Format for the Electronic Design
Process*, re-published as the dual-logo IEC 61523-3:2004. It is an
s-expression file that back-annotates a **structural netlist** with
timing: `(DELAYFILE (SDFVERSION …) (TIMESCALE …) (CELL (CELLTYPE …)
(INSTANCE …) (DELAY (ABSOLUTE (IOPATH a y (r:t:f) (r:t:f)) …))
(TIMINGCHECK (SETUP …) (HOLD …))))`.

"Consuming SDF" is not one claim; it is at minimum four, and they are
separable:

| Claim | What it needs |
|---|---|
| C1 — parse an SDF file without misreading it | a grammar for the `DELAYFILE` header, `CELL`, `DELAY`/`ABSOLUTE`/`INCREMENT`, `IOPATH`, `INTERCONNECT`, `PORT`, and the `min:typ:max` triple |
| C2 — resolve `INSTANCE` paths onto a JLS circuit | a stable, documented instance-naming scheme, hierarchy separator handling, wildcards |
| C3 — simulate with the annotated numbers | a delay model that can *represent* what SDF says |
| C4 — honor `TIMINGCHECK` | setup/hold violation detection, which requires a value domain that can express "violated" |

The artifact a conformance claim would rest on: a `docs/sdf-profile.md`
naming, clause by clause, the subset accepted and everything ignored,
plus golden circuits whose simulated output changes in the documented
way when an SDF file is applied.

**C3 is where this dies.** Read `docs/simulation-semantics.md` against
the four claims:

- §1: time is a dimensionless non-negative 64-bit integer. SDF carries
  real numbers scaled by a `TIMESCALE` (`1ps`, `100ps`…). Mapping
  0.37 ns onto an integer tick means choosing a global resolution and
  rounding — a specified, breaking change to every existing golden.
- §2: the value domain is **two states plus HiZ, with no X anywhere**,
  pinned by
  `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`.
  C4 (`TIMINGCHECK`) has nowhere to put a violation. Real simulators
  drive X. Adding X to JLS is a rewrite of the value domain, every
  element's `react`, the VCD writer, the batch stdout format (a
  **stability contract**, `docs/batch-interface.md`), and the file
  format's watched-value rendering.
- §6.1: **wires are ideal** — "propagation across a net takes zero
  simulation time and the whole net carries one value. There is no
  per-wire or per-segment delay." SDF's `INTERCONNECT` construct is
  precisely per-source/per-sink wire delay. Supporting it means
  `WireNet.propagate` (`src/jls/elem/WireNet.java`) stops delivering
  synchronously and starts scheduling per-sink events — which changes
  the read-latest rule in §6.1 that same-time race resolution depends
  on, and therefore changes results in circuits that have nothing to do
  with SDF.
- §6.2/§7: an element carries **one scalar `propDelay`**, applied as
  **transport delay** (no inertial glitch suppression), with defaults
  in the §7 table (AND/OR/XOR 10, NAND/NOR/NOT 5, Mux 25, Register 50,
  Memory 100, Adder 30 × bits). SDF's `IOPATH` is **per input-pin →
  output-pin arc**, with separate rise and fall values and optionally
  `COND`itional variants. `src/jls/elem/DelayGate.java` — the one
  element whose whole purpose is delay — is a `Gate` subclass with a
  single user-specified value and no arc structure at all
  (`Kind("DELAY","DelayGate",1,0)`, `implements Timed`). Per-arc,
  per-edge, conditional delay is a new element timing model, not a new
  parser.

**C2 is where it dies a second time, and this one has no fix.** SDF is
keyed to `CELLTYPE` + `INSTANCE` — cell instances of a *technology
library*. A JLS drawing has no cells; it has AND gates and Registers.
The only way to get a real SDF file for a JLS circuit is
JLS → Verilog → Yosys → technology mapping → place & route → static
timing analysis (OpenSTA/OpenROAD `write_sdf`) — and technology mapping
is exactly the step that destroys the 1:1 correspondence between the
drawn elements and the netlist instances. A 4-input AND drawn by a
student becomes some number of `sky130_fd_sc_hd__and2` cells with
generated names. There is no back-mapping, so the annotated delays
cannot be attributed to the drawing the student is looking at. SDF
consumption in JLS would only ever work on hand-written SDF files
authored to match JLS's own instance names — i.e. a private format
wearing an IEEE number.

#### Implementation procedure

Recorded for costing only; the recommendation is **do not build this**.
If it were built, the honest order is:

1. `docs/sdf-profile.md` first, as a normative subset spec, because the
   subset choice *is* the design. Recommend: `IOPATH` absolute delays
   only; reject `INTERCONNECT`, `TIMINGCHECK`, `COND`, `INCREMENT`,
   `PATHPULSE`, and negative delays at parse time with a `LoadError`-style
   structured diagnostic.
2. `src/jls/hdl/sdf/SdfParser.java` + `SdfFile`/`SdfCell`/`SdfDelay`
   records, in `jls.hdl` (headless — `HeadlessCoreRatchetTest` forbids
   AWT/Swing in the core and batch surfaces, and this must be usable
   from batch mode).
3. An instance-naming contract. JLS has stable element ids
   (`jls.elem.ElementId`, `test/jls/StableElementIdTest.java`, issue
   #181) and `Circuit.getElementsInStableOrder`; a hierarchical name
   built from subcircuit instance names plus stable id is the only
   defensible scheme. This becomes a **new stability contract** the
   moment anyone writes an SDF file against it.
4. Delay-model change: replace the scalar `propDelay` on `LogicElement`
   with a per-arc table, keeping the scalar as the default so every
   existing circuit is unchanged. Touches `Gate`, `Register`, `Mux`,
   `Adder`, `Memory`, `TriState`, `StateMachine`, `TruthTable`,
   `DelayGate` and `LogicElement.resetPropDelay`.
5. `docs/simulation-semantics.md` §6/§7 rewritten, with the equivalence
   criterion in `ARCHITECTURE.md`'s "Simulation execution strategy"
   decision applied: any change must be a *specified, documented* change
   to the semantics doc, never a silent behavioral difference.
6. A `-sdf file` flag in `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:759-787`), which drags in `CliFlagTableTest`.
   (`docs/batch-interface.md` §1 holds no flag table of its own — it names
   `JLSStart.FLAGS` as "the single authoritative flag list" and its synopsis
   line enumerates only the `-b` batch flags — so a new flag obliges a
   CHANGELOG entry under §6, not a §1 edit, unless the flag changes batch
   simulation itself. `-sdf` *would* change batch simulation output, so §1 and
   §3 are genuinely in scope here.)

**Compatibility story:** additive if and only if the per-arc table
defaults to today's scalar and no SDF file is supplied. The moment
`INTERCONNECT` or X are added, every golden under
`test/resources/` and both normative docs are in scope, and the batch
stdout format (a stability contract) changes. That is the slope.

#### Testing procedure

- `SdfParserTest.java` (to be created, `test/jls/hdl/sdf/`) — grammar
  acceptance and, more importantly, *rejection*: every construct outside
  the profile must produce a named error, not silent acceptance. This is
  the single most valuable test, because a timing file that is silently
  half-applied is worse than one that is refused.
- `SdfAnnotationGoldenTest.java` (to be created) — a small circuit
  simulated twice, without and with an SDF file, with both outputs as
  goldens under `test/resources/sdf/`. Regression that turns it red: any
  change to the delay resolution order.
- Property/fuzz: JLS already has `GenerativeRoundTripFuzzTest` and
  `ContainerMutationFuzzTest`; an SDF mutation fuzzer (byte-flip a valid
  file, assert a structured error and never a stack trace) fits the
  `UntrustedFileHardeningTest` house pattern directly.
- External validation is the problem. The obvious oracle — annotate the
  exported Verilog with the same SDF under Icarus Verilog via
  `$sdf_annotate` and compare to JLS — does not exist reliably: Icarus's
  SDF support is documented by its own maintainers as poor and
  incomplete (open tracking issue steveicarus/iverilog#943; a 2023 GSoC
  project existed specifically to improve it; known gaps include ports
  with indices, conditional IOPATHs, and tri-state enable arcs).
  *A twenty-year-old, widely used open simulator still has broken SDF
  support.* That is the size of this problem, stated by someone else.
- CI: would need a new `sdf` lane in `.github/workflows/ci.yml`
  installing `iverilog` and (for producing test SDF) OpenSTA — a
  dependency the project has never taken.

#### Certification / conformance procedure

**Self-asserted. No registry, no body, no fee, no expiry.** Nobody
audits SDF readers. IEEE Std 1497-2001 is paywalled (resellers list it
in the low hundreds of USD; *exact current price unverified*), and it
is superseded in the IEC catalogue by IEC 61523-3:2004, so a claim
should name which document was read.

A credible assertion would need all four house parts, and part 3 — an
independent consumer that agrees with JLS on the same SDF file — is
effectively unavailable per the Icarus finding above. **Conclusion: this
item cannot currently be asserted credibly even if implemented.**

#### Effort, risk, and failure modes

**25–40 maintainer-days**, and that number is a floor, not an estimate.
Reasoning: parser + records for the restricted profile 5–8; instance
naming and hierarchy resolution 4–6 (and it creates a new contract);
the per-arc delay-model change across ~10 element classes plus the
normative rewrite and golden re-derivation 10–15; tests and fuzzing 5;
CLI/doc/CHANGELOG 2. The floor caveat: C1+C2+C3-restricted is the
*useless* subset. The version anyone would actually want includes
`INTERCONNECT` and `TIMINGCHECK`, which are the X-state and wire-delay
rewrites, and those are not 40 days.

Top three failure modes:

1. **Silent partial annotation.** An SDF file whose instance names
   mostly do not match is applied to the few that do, producing a
   circuit that is 10% back-annotated and 90% default — and looks
   plausible. Grading scripts consume batch output; this is a
   correctness hazard with a teaching cost.
2. **The X-state cascade.** `TIMINGCHECK` is the natural next request
   ("my flip-flop has a setup time"), and answering it requires an
   unknown state, which invalidates §2 of the semantics spec, the VCD
   golden that asserts two-state-plus-HiZ, and the batch stdout
   contract.
3. **Ideal-wire erosion.** `INTERCONNECT` support changes
   `WireNet.propagate` from synchronous delivery to scheduled delivery,
   silently altering same-time race resolution in circuits with no SDF
   involved. `RiscvCpuGoldenTest` would be the canary, and it would go
   red for reasons nobody expects.

**Do not do this if** (all currently true): there is no requesting
course; JLS has no technology library and therefore no cells for SDF to
annotate; the simulation semantics document is a published contract with
a bit-for-bit equivalence criterion recorded in `ARCHITECTURE.md`; and
no open simulator can serve as the independent oracle.

---

### 2. EDIF (#74) — the standardized netlist nobody asks for

#### What conformance actually means

EDIF — Electronic Design Interchange Format. Three versions matter:
EDIF 2 0 0 (ANSI/EIA-548, 1988), EDIF 3 0 0 (EIA-618), EDIF 4 0 0
(EIA-682), with IEC re-publications as IEC 61690-1 / 61690-2. EDIF is
an s-expression format with an internal conformance vocabulary of its
own: a file declares its `edifLevel` and `keywordLevel`, and is written
against a *view type* (`NETLIST`, `SCHEMATIC`, `SYMBOLIC`, …). A
conformance claim therefore has to be as narrow as: *"EDIF 2 0 0,
`edifLevel 0`, `keywordLevel 0`, `viewType NETLIST`, no `SCHEMATIC`
view, no properties beyond X"*. (The exact `edifLevel`/`keywordLevel`
semantics should be re-read in the primary document before publishing
such a claim — **unverified in this pass**.)

This is the **only formally standardized netlist format** in Tier 5,
which is the entire reason it appears on the list. Everything else there
(Yosys JSON, BLIF, structural Verilog, FASM) is de facto.

#### Implementation procedure

An emitter is genuinely small, and that is the trap — cheapness is not
relevance. The mechanical shape, if built:

1. `src/jls/hdl/edif/EdifEmitter.java` implementing
   `jls.hdl.HdlEmitter` (`src/jls/hdl/HdlEmitter.java`: `String
   emit(HdlModel)` + `String fileExtension()`, with a documented
   determinism requirement — "same model, same bytes"). It walks the
   same `HdlModel` (`src/jls/hdl/HdlModel.java`: `Port`, `Net`,
   `Operand` records) that `VerilogEmitter` and `VhdlEmitter` walk, so
   no exporter or model change is needed.
2. Register it at the `hdl.exporter` extension point
   (`docs/extension-points.md`; contract `jls.hdl.HdlEmitter`, home
   package `jls.hdl`, pinned by `ExtensionPointCatalogTest`). This is
   the cleanest add in the whole playbook — the seam already exists and
   already has two contributions.
3. Extend `-export` dispatch in `src/jls/JLSStart.java` to select by
   extension (`.edn`/`.edf`/`.edif`), which is how `-export out.v` vs
   `out.vhdl` already works.
4. Identifier legalization: EDIF identifiers are more restrictive than
   Verilog's. `src/jls/hdl/HdlNames.java` already owns legalization and
   `HdlModel.renames` already records what changed, so this is a table
   addition rather than new machinery.

No file-format or batch-contract change. No migration story — it is a
new output only.

**Who could consume it.** Honestly: Vivado (`read_edif`, still in the
2025.2 Tcl command reference) and Quartus Prime (EDIF Input File `.edf`
with a Library Mapping File). Both are proprietary, registration-walled,
multi-gigabyte tools that cannot be installed in this project's CI.
And crucially — **Yosys already has `write_edif`**
(`backends/edif/edif.cc`, documented as generating EDIF for the Xilinx
place-and-route tools). JLS already emits structural Verilog-2005 that
Yosys reads. So the EDIF path *already exists today* as
`jls -export design.v` → `yosys -p 'read_verilog design.v; write_edif
design.edn'`, produced by a tool that maintains its EDIF backend for
real users. A first-party JLS EDIF emitter would be a worse,
untested-against-real-tools duplicate of a working delegation — and
delegation-not-duplication is the settled stance of
`docs/hdl-support-research.md` and `docs/grand-architecture.md` §2.

#### Testing procedure

- `EdifExportGoldenTest.java` (to be created, `test/jls/hdl/`) with
  `.edn` goldens under `test/resources/hdl/`, mirroring
  `VerilogExportGoldenTest`/`VhdlExportGoldenTest`. Cheap, and proves
  determinism.
- Structural assertions à la `test/jls/hdl/VerilogStructure.java`.
- **The part that cannot be built: an independent consumer in CI.**
  There is no open-source EDIF *reader* in the `iverilog`/`ghdl`/`yosys`
  class that the project could add to `ToolLocator` and run
  skip-when-absent. Yosys writes EDIF; it does not read it. The only
  readers are Vivado and Quartus. So the golden test would assert that
  JLS's output equals JLS's own previous output — a determinism test
  wearing a conformance label. Compare
  `IverilogCompileTest.everyGoldenCompilesUnderIverilog`, which asserts
  that software the project did not write accepts the output: that is
  the difference between conformance and theater, and EDIF cannot cross
  it.
- No CI lane change is possible or worth it.

#### Certification / conformance procedure

**Self-asserted, and the standard itself is hard to obtain.** EIA-548 /
EIA-618 / EIA-682 are old ANSI/EIA documents; EIA as a standards
organization dissolved in 2011 and its documents were dispersed
(*current custodian and availability unverified in this pass*). IEC
61690-1/-2 are purchasable from IEC. There is no EDIF conformance
body, no registry, no logo, no fee beyond buying the document, and no
renewal.

There is no way to make this credible under the four-part house rule:
part 3 (independent consumer) is structurally unavailable. **A
conformance claim here would be an assertion about a document the
maintainer bought, checked by nothing.** That is the definition of
theater, and the survey's word for it (§6) is correct.

#### Effort, risk, and failure modes

**5–8 maintainer-days**: emitter 2–3 (the `HdlEmitter` seam does most of
the work), goldens 1, name legalization 1, CLI/doc/help/CHANGELOG 1–2,
plus 1 for reading enough of the specification to pick a level. Cheap —
which is exactly why the rejection needs to be recorded rather than left
to drift.

Top three failure modes:

1. **A claim nobody can check becomes a claim nobody should make.** The
   project's credibility rests on evidence-backed conformance (51 HAVE
   entries with citations). One unverifiable entry devalues the others.
2. **Bit-rot with a user.** If one instructor uses the EDIF path into
   Quartus, JLS acquires a permanent output format it cannot regression-
   test, forever, for one user who is better served by the Yosys route.
3. **Level confusion.** Emitting EDIF 2 0 0 that Vivado accepts and
   Quartus rejects (or vice versa) is the normal EDIF experience;
   without either tool in CI, the maintainer debugs by proxy over email.

**Do not do this if** the goal is a conformance claim. The only
non-theatrical reason to emit EDIF would be a specific user with a
specific tool that cannot read structural Verilog — and the survey
records none. **Recommendation: `docs/hdl-support-research.md` gains one
paragraph documenting the Yosys `write_edif` delegation recipe, and #74
is closed as WONTFIX with that recipe as the answer.** That is a
half-day and it is the whole correct response to this entry.

---

### 3. JEDEC JESD3-C (#83) — draw logic, burn a real GAL

#### What conformance actually means

JEDEC **JESD3-C**, *Standard Data Transfer Format Between Data
Preparation System and Programmable Logic Device Programmer* — the
`.jed` fuse-map file. It specifies an STX/ETX-framed ASCII file of
`*`-terminated fields: a device/comment header, `QF` (fuse count),
`QP` (pin count), `F` (default fuse state), `L<addr>` fuse-data records,
`C` (fuse checksum), plus optional `G` (security fuse), `V`/`P`/`S`/`R`
test-vector fields, and a four-hex-digit **transmission checksum** after
ETX. (The exact field set and both checksum algorithms must be taken
from the document itself — summarized here from secondary sources and
**not verified against the primary text in this pass**; the archive.org
full-text mirror 403'd to automated fetching.)

Unlike every other item in this section, **the standard is free**: JEDEC
publishes JESD3-C for download at no charge with registration
(`jedec.org/standards-documents/docs/jesd-3-c`), and a full-text copy is
mirrored publicly at `archive.org/details/JEDECJESD3C`. Cost of the
specification: **$0**. That materially changes the arithmetic against
BSDL below.

The conformance claim is unusually crisp and unusually testable: *"the
file JLS writes for device D is byte-identical to what an independent,
established GAL assembler writes for the same logic, and both checksums
verify."*

But be precise about what is and is not claimed. JESD3-C conformance is
conformance to a **file syntax**. It says nothing about whether the fuse
pattern is correct for the device. A JESD3-C-valid `.jed` file with the
wrong fuses for a GAL22V10 is a conforming file that bricks nothing and
does nothing. **The hard part of this project is not the standard.**

#### Implementation procedure

The full pipeline, with the ownership decision at each stage:

| Stage | Owner | Why |
|---|---|---|
| drawn circuit → per-output Boolean equations | **JLS** | it is a walk of the element graph, close to `HdlExporter.buildModel` |
| equations → minimized sum-of-products | **decide** | there is no minimizer anywhere in the tree (verified: no Quine–McCluskey / Espresso / minimization code in `src/`) |
| SOP → device fitting (OLMC modes, product-term budget, pin assignment) | **decide** | device-specific; where all the real difficulty is |
| fitted design → `.jed` fuse map | **decide** | the actual JESD3-C surface |
| `.jed` → silicon | **never JLS** | a physical programmer (XGecu TL866II+/T48 class) — *specific model support unverified* |

The open tooling is real and checked: **galette** (Rust, MIT,
`simon-frankau/galette`) is a largely GALasm-compatible GAL assembler
that emits `.jed` for GAL16V8/22V10; **GALasm** (`daveho/GALasm`, a C
descendant of GALer) is the older one it is compatible with. Both
consume an equation source file (`.pld`) and produce the fuse map.
Neither minimizes: they fit equations you have already reduced.

**Recommendation — the two-phase split, and phase 1 is where to stop:**

**Phase 1 (the honest, cheap version — no JESD3-C claim).**
`src/jls/hdl/pld/PldEmitter.java` implementing `jls.hdl.HdlEmitter`,
registered at the `hdl.exporter` extension point exactly like the PCF
work: JLS emits a **galette/GALasm-compatible equation file** from the
drawn circuit and stops. The student runs `galette design.pld` to get
`design.jed`, then a programmer. This is the same delegation shape as
`src/jls/hdl/board/PcfEmitter.java` (emit the constraint file, let
nextpnr do the work) and it is architecturally identical to the settled
Yosys stance. JLS never writes a fuse and never claims JESD3-C.

**Phase 2 (only if a course asks for one tool).**
`src/jls/hdl/pld/JedecWriter.java` + a device model
(`src/jls/hdl/pld/Device.java`, `Gal22V10.java`, `Gal16V8.java`)
carrying `QF`, pin count, OLMC structure and the AND-array fuse
addressing, plus both checksums. *Then* JLS owns a JESD3-C claim, and
the golden strategy below becomes available. Note the device-model
authority problem: the fuse maps for GAL16V8 (2194 fuses) and GAL22V10
(5892 fuses) are published in vendor datasheets and reproduced in the
GALasm documentation, but JLS would be encoding them from secondary
sources — the datasheet is the primary and must be cited in the device
model's Javadoc.

Design decisions with recommendations:

- **Minimization: do not write Espresso.** Recommend Quine–McCluskey
  with don't-care support, capped at a documented input count (≤ 12–14
  inputs per output cone), refusing anything larger with a named error
  rather than exploding. Classroom PLD designs are small by
  construction; a GAL22V10 has 22 pins. An Espresso-quality heuristic
  minimizer is a research project and unnecessary at this scale.
- **Device set: GAL22V10 and GAL16V8 only.** Do not attempt ATF150x
  CPLDs — those need Atmel's proprietary fitter binaries, which is a
  wholly different delegation problem (*licensing and redistribution
  terms unverified*).
- **Failure mode discipline: copy `PcfEmitter`'s all-or-nothing
  contract.** Its Javadoc states every problem is collected and reported
  in a single `HdlExportException` "and no text is returned, so a
  partial or invalid constraint file can never reach disk"
  (`UnbindablePortsTest` pins it). A partial fuse map is worse than a
  partial constraint file: it programs a one-time-electrically-erasable
  part with wrong logic. Same rule, harder.
- **Surface:** `-export design.pld` (phase 1) / `-export design.jed`
  with `-device GAL22V10` (phase 2). Touches `JLSStart.FLAGS`
  (`src/jls/JLSStart.java:759-787`) and `CliFlagTableTest` — additive, no
  existing contract broken. It does **not** touch `docs/batch-interface.md`
  §1: `-export` is not in §1's batch synopsis and §1 carries no flag table,
  only a pointer to `JLSStart.FLAGS`. A CHANGELOG entry under §6 is the
  obligation.

**Migration/compatibility:** entirely additive. No `.jls` file-format
change (`docs/file-format.md` untouched), no simulation-semantics
change, no batch output change. That is what makes this the only
attractive item of the four.

**The 2026 hardware question, answered honestly.** The devices are *not*
dead. *(Every figure and date in this paragraph comes from search-result
summaries of vendor and distributor pages, not from a fetched primary
source — re-check before it reaches a syllabus or a purchase order.)*
Microchip's ATF16V8 and ATF22V10 families are listed **In
Production** as of 2025, sold through DigiKey and Microchip Direct;
Microchip qualified a new final test site for ATF22V10C with an
estimated first ship in May 2026, and released WinCUPL II v1.0.0 beta in
August 2025. The community project `peterzieba/5Vpld` exists precisely
because these are among the last 5 V programmable parts still available.
Original Lattice GALs are largely obsolete, but the Atmel/Microchip
successors are current parts at roughly a dollar or two. Programmers are
cheap consumer hardware. **A lab that says "your drawing is now a
physical chip on a breadboard" is buildable in 2026 for under $100 of
class hardware** — *distributor pricing and specific programmer support
unverified in this pass*.

**Pedagogical payoff.** This is the strongest of the four by a wide
margin, and the reason is structural: it is the *only* item that closes
the loop from drawing to physical hardware without an FPGA toolchain,
a vendor account, or a gigabyte download. The existing #213/#215
FPGA path (Yosys → nextpnr → bitstream) is more powerful and much more
opaque; a GAL is a device a student can reason about completely — an
AND array, an OR array, ten output logic macrocells, and a file
listing which fuses are blown. The `.jed` file is *readable*. That is
the pedagogy.

#### Testing procedure

This item has the best test story of the four, and it is worth stating
because it is what makes the "yes, if" defensible.

- `PldEmitterGoldenTest.java` (to be created, `test/jls/hdl/pld/`) with
  `.pld` goldens under `test/resources/hdl/pld/`, mirroring
  `test/jls/hdl/board/PcfGoldenTest.java` and its
  `test/resources/hdl/board/blinky_icestick.pcf` fixture.
- `JedecWriterGoldenTest.java` (phase 2) with `.jed` goldens. Byte-exact
  is the right bar: the file is ASCII, fixed-order, and both checksums
  are functions of the content, so nondeterminism is impossible if the
  writer is correct.
- **The independent-consumer test, which is the whole point:**
  `GaletteCrossCheckTest.java` (to be created) locating `galette` (or
  `galasm`) via the existing `test/jls/hdl/ToolLocator.java` and
  `Assumptions.assumeTrue(...)`-skipping when absent — the exact pattern
  of `IverilogCompileTest`/`GhdlCompileTest`. Phase 1: run galette on
  JLS's `.pld` and assert it succeeds. Phase 2: run galette on JLS's
  `.pld` and assert its `.jed` is **byte-identical** to JLS's own `.jed`
  for the same design. That is a conformance proof produced by software
  the project did not write, on a standard the project can legally read.
- `JedecChecksumTest.java` — both checksums as pure functions, with
  vectors taken from the JESD3-C text. Property test: for random fuse
  arrays, checksum(write(fuses)) is stable and `read(write(x)) == x`.
- Fuzz opportunity: random small circuits → equations → minimize →
  fit → `.jed` → parse back → simulate the fuse map against the original
  circuit with the existing `BatchSimulator`. That is a *semantic*
  round-trip oracle, not just a syntactic one, and it is the same
  differential-testing instinct `riscv/fuzz_diff.py` already embodies.
- CI: a new `pld` lane in `.github/workflows/ci.yml`, or — cheaper —
  add `galette`/`galasm` installation to the existing `build` job the
  way `iverilog` is already installed for the HDL suites. Prefer the
  latter; the project does not need another lane.
- **What turns it red:** any change to the equation extraction, the
  minimizer's canonical term ordering, the OLMC mode selection, or the
  fuse addressing. All four are exactly the things that must not drift
  silently, since the consequence is a mis-programmed physical part.

#### Certification / conformance procedure

**Self-asserted. JEDEC operates no conformance program for JESD3-C, has
no registry of conforming writers, issues no logo, and charges nothing.**
There is no submission step, no evidence package, no validity period, no
renewal, and nothing that can invalidate it except a later revision of
the standard (JESD3-C dates from the mid-1990s and is stable in
practice; *no successor revision found in this pass, unverified*).

A credible self-assertion here is achievable in full, which is rare:

1. `docs/pld-export.md`, normative, RFC 2119, citing JESD3-C field
   identifiers by name and listing exactly which optional fields are and
   are not emitted (recommend: emit `QF`, `QP`, `F`, `L`, `C`, `G`;
   emit no test-vector fields `V`/`P`/`S`/`R` and say so);
2. byte-exact goldens under `test/resources/hdl/pld/`;
3. the galette/GALasm cross-check in CI with skip-when-absent — an
   independent implementation agreeing byte-for-byte;
4. a README scope sentence: JLS generates programming files; it does not
   drive programmer hardware and makes no claim about any specific
   programmer or device revision.

All four are reachable. Cost: $0 in fees.

#### Effort, risk, and failure modes

- **Phase 1 (equation emitter, delegate the fuse map): 8–12
  maintainer-days.** Equation extraction from the element graph 3–4 (the
  `HdlExporter` walk is the template and much of the graph work is
  reusable); SOP flattening + Quine–McCluskey with a documented input
  cap 3–4; emitter + goldens + galette cross-check 2–3; docs, help page,
  CLI, CHANGELOG 1–2.
- **Phase 2 (own the `.jed`): +12–18 maintainer-days.** Device models
  for two parts including OLMC mode selection and fuse addressing from
  datasheets 5–8; JESD3-C writer + both checksums 2–3; the semantic
  round-trip fuzz oracle 3–4; the normative doc 2.
- **Total if both: 20–30 maintainer-days.**

Top three failure modes:

1. **The minimizer becomes the project.** Someone draws a 20-input cone,
   Quine–McCluskey explodes, and the maintainer is now writing Espresso.
   Mitigation is a *hard documented cap with a named error*, decided
   before any code is written, not after the first bug report.
2. **The fitter is where the bodies are buried.** GAL16V8 has three
   global modes (simple/complex/registered) with different OLMC
   behavior and different pin usability; picking the mode automatically
   from a drawn circuit is a real constraint problem, and getting it
   subtly wrong produces a file that assembles cleanly and behaves
   wrongly on hardware. Mitigation: support GAL22V10 first (uniform
   macrocells, one mode) and treat 16V8 as a separate later slice.
3. **A physical-hardware support channel.** The moment a student's chip
   does not work, the bug report is "JLS is broken" and the debugging
   surface includes the programmer, the socket, the power supply, and
   the part's erase state. A single maintainer cannot own that.
   Mitigation is documentation: the README scope sentence above, plus a
   help page that names galette and the programmer as third-party
   responsibilities.

**Do not do this if** no course has asked. **Do it if** one has — this
is the item that earns a "yes, if". The trigger should be concrete: an
instructor stating they will run a PLD lab, naming the device. On that
trigger, build **phase 1 only**, ship it, and let a second, separate
request drive phase 2.

---

### 4. IEEE 1149.1 BSDL (#129) — generate a boundary-scan description

#### What conformance actually means

IEEE Std 1149.1, *Standard for Test Access Port and Boundary-Scan
Architecture* — current revision 1149.1-2013. Two very different things
live under that number:

- **The architecture**: the 4-wire TAP (TCK/TMS/TDI/TDO, optional
  TRST), the 16-state TAP controller state machine, an instruction
  register, and the mandatory instructions BYPASS, SAMPLE/PRELOAD,
  EXTEST (plus optional IDCODE, INTEST, CLAMP, HIGHZ). This is
  *hardware*, and in JLS it would be a drawn circuit or a generated
  element.
- **BSDL**, the Boundary-Scan Description Language: a small declarative
  file describing one component's TAP — pin map, instruction opcodes,
  register lengths, and the boundary-scan register cell list. Through
  1149.1-2001 BSDL was specified as a *proper subset of VHDL-93*; the
  2013 revision relaxed that to "based upon VHDL" and added hierarchical
  package extensions for embedded instruments.

A "generate BSDL for your drawn chip" feature claims conformance to
**the BSDL clause only** — that JLS emits a syntactically valid,
semantically consistent BSDL file. It emphatically does *not* claim that
the drawn circuit implements a conforming TAP; that would require
verifying the 16-state controller, instruction decoding, and cell
behavior against the architecture clauses, which is a different and much
larger project.

Naming the artifact precisely: the claim rests on **a BSDL file that a
third-party BSDL parser accepts and that describes the design JLS
actually drew.**

The synergy worth noting: `src/jls/hdl/VhdlEmitter.java` and
`src/jls/hdl/HdlNames.java` already exist and already do VHDL identifier
legalization and reserved-word handling. BSDL-as-VHDL-subset is not a
new language for this codebase.

#### Implementation procedure

1. **Decide what the student draws.** Two options:
   - (a) *Generated wrapper*: JLS wraps the user's circuit — the user
     marks top-level pins as boundary-scannable, JLS synthesizes the
     TAP, instruction register, and boundary register around it. Fast to
     use, but the student learns nothing about JTAG because they never
     draw it.
   - (b) *Drawn chain*: JLS ships a `TapController` element and a
     `BoundaryScanCell` element; the student wires them; JLS walks the
     drawing and emits BSDL describing what was drawn.
   **Recommend (b).** The pedagogical value of this item is *entirely*
   in the student building the chain. If JLS generates it, the feature
   is a BSDL printer with no teaching content, and BSDL printers exist.
   Under (b), the drawn chain also *simulates* in the existing engine —
   the student can shift a pattern through the boundary register and
   watch it in the trace window, which is the actual lesson.
2. **New elements** (`src/jls/elem/`): `TapController` (the 16-state
   FSM — note JLS's own `StateMachine` element is edge-triggered with a
   busy state per `docs/simulation-semantics.md` §8.2, so a purpose-built
   element is cleaner than a drawn state machine) and `BoundaryScanCell`
   (BC_1 and BC_2 cell types cover most teaching cases). Each element
   costs roughly the sixteen-place checklist in `ARCHITECTURE.md`
   ("Adding an element today (the honest list)") — although
   `jls.elem.ElementRegistry` (#78) has since landed, which collapses
   part of it.
3. **`src/jls/hdl/bsdl/BsdlEmitter.java`** implementing
   `jls.hdl.HdlEmitter`, registered at `hdl.exporter`. It walks the
   `HdlModel` port list plus a new scan-chain traversal to produce
   `attribute PIN_MAP_STRING`, `attribute INSTRUCTION_OPCODE`,
   `attribute BOUNDARY_LENGTH`, `attribute BOUNDARY_REGISTER`, etc.
4. **Surface:** `-export design.bsd`. Additive; touches `JLSStart.FLAGS`
   and `CliFlagTableTest`, plus a CHANGELOG entry under
   `docs/batch-interface.md` §6. Not §1 — see the note under SDF step 6.
5. **File-format impact:** two new element tags in
   `src/jls/elem/SaveTags.java`, which is governed by
   `docs/file-format.md` (a **stability contract**) — but adding tags is
   the documented additive path (`SaveTagsTest`, `FileFormatSpecTest`,
   `AllElementsRoundTripTest`), and old files are unaffected. No
   migration burden.

**On the paywall.** IEEE 1149.1-2013 is not free. Reseller listings seen
during this pass ranged roughly $148–$396 USD for the PDF (ANSI webstore
listed highest; several discount resellers lower) — *exact current IEEE
Standards Store price unverified; the pages returned HTTP 403 to
automated fetching.* Set against a 15–25 day build, **$300 is not the
obstacle**, and framing it as one would be dishonest. What *is* true:

- A single unfunded maintainer should not be casually buying specs, and
  many contributors will not have access — so any BSDL work would be
  reviewed by people who cannot read the normative text. That is a real
  process problem, not a budget one.
- Partial free access exists: the IEEE 1149.1 working group published a
  BSDL input-specification PDF on `grouper.ieee.org`, and vendors publish
  thousands of real BSDL files for real parts, which are the best
  available examples. Neither is the normative document.
- **The rule to adopt if this is ever built:** if the maintainer has not
  read IEEE 1149.1, the docs must say *"JLS emits BSDL-shaped files
  accepted by <named parsers>"*, never *"JLS conforms to IEEE 1149.1"*.
  A conformance claim about an unread document is exactly the failure
  mode this whole playbook exists to prevent. University IEEE Xplore
  access, where the maintainer has it, resolves this cleanly.

#### Testing procedure

- `BsdlExportGoldenTest.java` (to be created, `test/jls/hdl/bsdl/`) with
  `.bsd` goldens under `test/resources/hdl/bsdl/`, following
  `VerilogExportGoldenTest`.
- **Independent consumer, skip-when-absent:** `UrjtagBsdlTest.java` (to
  be created) locating `bsdl2jtag` (UrJTAG's BSDL-to-declaration
  converter) through `test/jls/hdl/ToolLocator.java` and
  `Assumptions.assumeTrue`, asserting every golden converts without
  error. UrJTAG's BSDL subsystem is the most maintained open parser
  found; `cyrozap/python-bsdl-parser` exists but is described by its own
  README as unmaintained and broken on modern Python, so it is not a
  CI dependency. *Whether current UrJTAG packages in Debian/Ubuntu still
  build `bsdl2jtag` is unverified.*
- A second, stronger oracle available only under design (b): a
  **simulation cross-check** — drive the drawn TAP with a `-t` test
  vector sequence that shifts BYPASS and SAMPLE patterns, and assert the
  batch output against a golden. That proves the drawn chain behaves,
  which is the part students care about, using machinery that already
  exists (`BatchSimulationGoldenTest`, `SequentialGoldenTest` are the
  templates).
- Property opportunity: BOUNDARY_LENGTH declared in the BSDL must equal
  the number of cells found by the chain walk; generate random chains and
  assert the invariant. Cheap and catches the most likely emitter bug.
- CI: add `urjtag` to the tool installation in the existing `build` job
  of `.github/workflows/ci.yml` alongside `iverilog`/`ghdl`/`yosys`;
  no new lane.
- **What turns it red:** any change to chain traversal order (BSDL cell
  numbering is positional and order-significant), pin-map generation, or
  instruction opcode assignment.

#### Certification / conformance procedure

**Self-asserted. There is no BSDL conformance body, no registry, no
certification mark, and no fee.** In industry, BSDL files are validated
by whatever ATE/boundary-scan tool consumes them (ASSET, Intellitech,
Corelis, XJTAG, UrJTAG); "correct" operationally means "the tool loaded
it and the chain worked on the bench". There is no submission step, no
evidence package, no validity period, no renewal.

Costs and elapsed time that are real: **~$150–$400 to buy IEEE
1149.1-2013** (unverified, see above), plus 2–4 days to read the BSDL
clauses properly. Nothing else. What invalidates a claim: a later
revision of 1149.1 changing BSDL (the 2013 revision already did change
its relationship to VHDL, so this is not hypothetical).

The credible self-assertion, if built: `docs/bsdl-export.md` naming the
revision read and the attributes emitted; goldens; the UrJTAG check in
CI; and the README scope sentence limiting the claim to file generation
for circuits drawn in JLS, with no claim about the architecture clauses.

#### Effort, risk, and failure modes

**15–25 maintainer-days.** Reasoning: `TapController` element with a
correct 16-state controller, including its save/load, dialog, help page,
palette entry, round-trip fixture and simulation golden, 6–10 (the TAP
FSM is small but the element checklist is not, and the state machine has
real semantics to get right); `BoundaryScanCell` element 2–3 (much
simpler, and the second element reuses the first's patterns); chain
traversal + `BsdlEmitter` + goldens 3–5; UrJTAG cross-check 1–2; reading
the standard 2–4; docs/help/CHANGELOG 1–2.

Top three failure modes:

1. **The TAP controller is a semantics trap.** It is a 16-state Mealy/
   Moore hybrid clocked on both TCK edges in places (TDO changes on the
   falling edge). JLS's engine is fine with that, but getting it wrong
   produces an element that looks right and shifts garbage — and unlike
   a gate, nobody eyeballing the schematic can tell.
2. **Scope creep to 1149.6/1687.** The moment boundary scan exists,
   "can it do IJTAG?" follows. Every neighbour entry in Tier 9 is
   OTHER for good reasons; the scope sentence has to be written on day
   one.
3. **A conformance claim about an unread standard.** Covered above.
   This is the one that damages the project rather than merely wasting
   time.

**Do not do this if** there is no requesting course *and* the maintainer
cannot obtain IEEE 1149.1 — the combination means 20 days spent to
produce a file format nobody asked for, described by a document nobody
on the project has read. **Defer** is the right verdict now, but it is
the most *interesting* deferral of the four: unlike SDF and EDIF, it
would be real conformance with a real independent checker and real
teaching content.

---

### Summary: verdicts and reopening triggers

| Item | Verdict | Cost if built | Specific trigger that reopens it |
|---|---|---|---|
| **IEEE 1497 SDF (#89)** | **Do not.** Record as a permanent scope boundary. | 25–40 md (floor; the useful version is far more) | JLS acquires a technology-cell library *and* a name-stable synthesis path from drawing to netlist — i.e. #215's bitstream work produces a cell-instance mapping back to drawn elements. Absent that mapping, there is nothing for SDF to annotate. A request for setup/hold teaching is **not** a trigger; that is a different, smaller feature (a checked timing constraint on the Register element) and should be filed as such. |
| **EDIF (#74)** | **Do not.** Close as WONTFIX; spend a half-day documenting the `jls -export design.v` → `yosys write_edif` recipe in `docs/hdl-support-research.md` instead. | 5–8 md, of which 0 buy a checkable claim | A named user with a named tool that reads EDIF and cannot read structural Verilog-2005 or Yosys JSON. This is close to impossible, since Vivado and Quartus both accept Verilog netlists. Alternatively: an open-source EDIF *reader* appears that could serve as a CI oracle — at which point the claim becomes checkable and the item is worth 5 days. |
| **JEDEC JESD3-C (#83)** | **Do it if a course asks** — phase 1 only (equation emitter, delegate fuse generation to galette/GALasm). Phase 2 needs its own separate trigger. | 8–12 md phase 1; +12–18 md phase 2 | An instructor commits to a PLD lab and names the device. Then: build phase 1, ship, stop. Phase 2 (JLS owns the `.jed`) reopens only if that same course reports the two-tool workflow as a real classroom obstacle. Standard is free; devices are in production; the test story is the best of the four. |
| **IEEE 1149.1 BSDL (#129)** | **Defer**, with the design decision pre-recorded: if built, students *draw* the chain (option b), JLS does not generate it. | 15–25 md | A DFT or test-engineering course asks — *and* the maintainer has access to IEEE 1149.1-2013 (purchase or institutional IEEE Xplore). Without the second condition, the correct answer is still no, because the project would be asserting conformance to a document it has not read. If only the first condition is met, ship the drawn TAP elements (real teaching value, simulates today) and emit **no** BSDL. |

Two cross-cutting conclusions worth carrying out of this section:

- **The blocker is almost never the specification fee.** JESD3-C is free
  and is the best of the four; SDF and 1149.1 cost a few hundred dollars
  and that is negligible against 20–40 maintainer-days. What actually
  decides these is (i) whether an *independent implementation* exists to
  check the output in CI, and (ii) whether the item forces a change to
  `docs/simulation-semantics.md` or `docs/batch-interface.md`. SDF fails
  both. EDIF fails (i). JESD3-C passes both. BSDL passes both.
- **Three of the four are output formats, and JLS already has the seam
  for them.** `jls.hdl.HdlEmitter` + the `hdl.exporter` extension point
  means an EDIF, PLD, or BSDL emitter is a new class and a registration,
  not an architecture change. That is precisely why the discipline has to
  be *"is the claim checkable"* rather than *"is it cheap"* — cheapness
  is no longer a filter here, and the survey's own §13.2 note about not
  letting cheap outrank relevant applies with full force.

---

### Sources

**Repository (verified by reading at HEAD, commit `9ab4797`):**

- `docs/standards-landscape.md` — §6 (Tier 5, entries #74/#83), §7
  (Tier 6, #89), §10 (Tier 9, #129), §13.3 (deliberately not
  recommended).
- `docs/simulation-semantics.md` — §1 time model, §2 value domain
  (two-state + HiZ, no X), §6.1 ideal wires, §6.2 transport delay, §7
  per-element delay defaults, §8 edge triggering, §9 tri-state
  resolution.
- `src/jls/elem/DelayGate.java` — `Kind("DELAY","DelayGate",1,0)`,
  `implements Timed`; single scalar delay, no arc structure.
- `src/jls/elem/WireNet.java`, `src/jls/elem/Output.java`,
  `src/jls/sim/Simulator.java`, `src/jls/sim/SimEvent.java`.
- `src/jls/hdl/HdlEmitter.java` (the emitter contract),
  `src/jls/hdl/HdlModel.java` (`Port`/`Net`/`Operand` records),
  `src/jls/hdl/HdlExporter.java` (`buildModel`, `Result`),
  `src/jls/hdl/HdlNames.java`, `VerilogEmitter.java`,
  `VhdlEmitter.java`.
- `src/jls/hdl/board/PcfEmitter.java` — the all-or-nothing emitter
  contract this playbook recommends copying for `.jed`.
- `src/jls/hdl/scan/` — HDL *header* scanners (issue #63), **not**
  boundary scan; named here to prevent a misreading.
- `src/jls/JLSStart.java:759-787` — the authoritative `FLAGS` table.
- `test/jls/hdl/ToolLocator.java`, `test/jls/hdl/IverilogCompileTest.java`,
  `test/jls/hdl/GhdlCompileTest.java` — the skip-when-absent external
  validation pattern.
- `test/jls/hdl/board/PcfGoldenTest.java`,
  `test/jls/hdl/board/UnbindablePortsTest.java`,
  `test/resources/hdl/board/blinky_icestick.pcf`.
- `test/jls/VcdExportGoldenTest.java`, `test/jls/BatchSimulationGoldenTest.java`,
  `test/jls/SequentialGoldenTest.java`, `test/jls/HeadlessCoreRatchetTest.java`,
  `test/jls/CliFlagTableTest.java`, `test/jls/StableElementIdTest.java`.
- `docs/extension-points.md` — the `hdl.exporter` seam,
  `ExtensionPointCatalogTest`.
- `docs/batch-interface.md` (stability contract), `docs/file-format.md`
  (stability contract), `ARCHITECTURE.md` (element checklist; recorded
  decision on simulation execution strategy and its equivalence
  criterion), `docs/grand-architecture.md` §2 (delegation stance),
  `docs/hdl-support-research.md` (external-tool orchestration stance),
  `.github/workflows/ci.yml` (job list: `build`, `windows`, `macos`,
  `proofs`, `gui-wayland`, `gui-x11`, `macos-gui`, `windows-gui`,
  `reproducibility`, installer lanes).
- Verified absence: no logic-minimization code anywhere in `src/`
  (grep for Quine/Espresso/minimiz/Karnaugh returns only an unrelated
  "minimize selection rectangle" comment in `SimpleEditor.java`).

**External (searched July 2026; each marked):**

- IEEE 1497-2001 SDF, superseded/dual-logo as IEC 61523-3:2004 —
  IEEE SA and IEC catalogue listings. *Price unverified (vendor pages
  returned HTTP 403).*
- Icarus Verilog SDF limitations — steveicarus/iverilog issue #943
  (SDF tracking), issue #509 (indexed ports), SourceForge bug #960
  (tri-state enable arcs), and a 2023 GSoC project "Improving SDF
  support in Icarus Verilog". **Verified via search results; individual
  issue text not fetched.**
- OpenSTA/OpenROAD `write_sdf` exists. **Verified via search results.**
- EDIF consumption today: AMD Vivado `read_edif`/`write_edif` (UG835
  Tcl command reference, 2025.2 docs); Intel Quartus EDIF Input File
  (`.edf`) with Library Mapping File. Yosys `write_edif` backend
  (`yosys/backends/edif/edif.cc`, documented "generates EDIF files for
  the Xilinx place&route tools"). **Verified via search results.**
- EIA-548/618/682 custodianship after EIA's 2011 dissolution, and the
  exact `edifLevel`/`keywordLevel` conformance semantics —
  **unverified.**
- JEDEC JESD3-C: free download with registration at
  `jedec.org/standards-documents/docs/jesd-3-c`; public full-text mirror
  at `archive.org/details/JEDECJESD3C`. **Free availability verified via
  search; the field list and both checksum algorithms in this section
  are from secondary sources and were NOT verified against the primary
  text — the archive.org full-text URL returned HTTP 403 to automated
  fetching. Read the primary before writing any code.**
- galette (`simon-frankau/galette`, Rust, MIT, "largely galasm-
  compatible", emits `.jed` for GAL22V10) and GALasm (`daveho/GALasm`).
  **Verified via search results; repository contents not read.**
- GAL fuse-map sizes (GAL16V8 2194 fuses; GAL22V10 5892 fuses) — from
  the GALer/GALasm documentation reproduced in search results.
  **Not verified against a vendor datasheet.**
- Device availability 2026: Microchip ATF16V8 / ATF22V10B / ATF22V10C /
  ATF22V10CQZ listed "In Production"; a new final-test site qualified for
  ATF22V10C with estimated first ship 2026-05-10; WinCUPL II v1.0.0 beta
  released 2025-08-06; `peterzieba/5Vpld` community tooling.
  **Verified via search results (Microchip product pages, DigiKey,
  Future Electronics). Prices and specific programmer-model support
  unverified.**
- ATF150x CPLD fitters are proprietary Atmel/Microchip binaries —
  **redistribution terms unverified; explicitly out of scope above.**
- IEEE 1149.1-2013: BSDL is "based upon VHDL" rather than a proper VHDL
  subset as in earlier revisions (ASSET InterTech overview and
  Intellitech material). Reseller PDF prices seen: ANSI webstore $396,
  Accuris $331, en-standard $344.20, a discount reseller $148.
  **All price figures unverified — the vendor pages returned HTTP 403;
  treat the range, not any single number.** A BSDL input-specification
  PDF is published free by the working group at
  `grouper.ieee.org/groups/1149/1/`. **Verified as a search result;
  contents not fetched.**
- Open BSDL parsers: UrJTAG `bsdl2jtag` (Ubuntu manpage, UrJTAG book);
  `cyrozap/python-bsdl-parser` self-described as unmaintained and broken
  on modern Python. **Verified via search results; current Debian/Ubuntu
  packaging of `bsdl2jtag` unverified.**
