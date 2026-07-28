## Waveform formats: EVCD and FST (#67, #68)

**Recommendation up front: build neither.** Ship suffix-triggered gzip on the
existing `-vcd` writer instead (2–4 maintainer-days), and record EVCD as a
closed negative and FST as demand-gated with a named revisit trigger. The
reasoning and the exact procedure for all three paths are below.

*(`#67`/`#68` here are registry entries in `docs/standards-landscape.md` §5, not
GitHub issue numbers. The shipped VCD support is registry entry #66 and GitHub
issue #72; probes are GitHub issue #200.)*

### What conformance actually means

**There is no conformance program for any waveform format discussed here.**
Not for VCD, not for EVCD, not for FST. No registry, no accredited assessor,
no test suite published by IEEE or by GTKWave. Every claim in this space is a
self-assertion whose credibility rests entirely on the evidence the project
publishes alongside it. That fact drives the whole section: the question is
never "can we get certified" but "what artifact makes the claim checkable by a
skeptical reader."

**What JLS already claims, and what it rests on.** `docs/batch-interface.md`
§4 asserts that `-vcd` writes "a Value Change Dump per IEEE 1364-2001 section
18." That claim rests on four named artifacts, all of which exist:

1. a *normative profile* — `docs/batch-interface.md` §4.1–4.3 states exactly
   which optional constructs are used (`$comment`, `$timescale`, one flat
   `$scope module`, `$var wire`, `$dumpvars`) and which are deliberately
   omitted (`$date`, `$version`, `$dumpoff`/`$dumpon`, `$scope` nesting), with
   a code anchor per claim (`BatchSimulator.toVcd`, `.vcdValue`, `.vcdId`);
2. an *independent structural checker* —
   `test/jls/VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`,
   written from the document rather than from the emitter (its own Javadoc
   says so), re-deriving header order, `$var` shape, identifier uniqueness,
   `$dumpvars` completeness, strictly increasing timestamps, and the 0/1/z
   alphabet;
3. *byte-exact goldens* — `WAVE_GOLDEN` and `STIM_GOLDEN` in the same file,
   asserted against both `toVcd()` and the file written by the real CLI
   (`cliVcdFlagWritesTheGoldenFileAndGoldenStdout`);
4. a *stability promise* — `docs/batch-interface.md` §6.

That is a stronger conformance package than most tools in this class publish.
It is also, importantly, a **profile** claim, not a full-standard claim: JLS
emits a conforming subset and says which subset. That distinction is what
makes the claim honest, and it is the model any additional format must follow.

**One correction worth making regardless of what else is decided.** IEEE
1364-2001 is a superseded document; Verilog was folded into IEEE 1800
(SystemVerilog), where the VCD material lives in the clause-21 range. Citing
1364-2001 §18 is legitimate (it is the document the format was written
against, and it is what every other tool cites) but the docs should add one
informative sentence noting the successor location so a reader looking for a
live standard is not stranded. *Unverified:* the exact clause numbers in the
current 1800 revision (believed §21.7 four-state / §21.8 extended VCD, and
§18.1–18.5 within 1364-2001) — both standards are paywalled and I could not
open them. Do not write clause numbers into the docs that nobody has read off
the purchased text.

**What conformance would mean for EVCD (#67).** Extended VCD is the
`$dumpports` variant defined in the same clause of the same standard. It is
not "VCD plus extra fields"; it is a different value encoding for a different
subject. Its declarations are `$var port` (not `$var wire`), its dump section
is `$dumpports` (not `$dumpvars`), and each value record carries a *port
state* character plus **two strength digits** — one for the 0-driver and one
for the 1-driver — in the shape `p<state> <str0> <str1> <id>`. The state
alphabet distinguishes, per port, whether the value is being driven *in*, *out*,
both, or neither, so a bidirectional pad can be observed from both sides in one
record. *Unverified:* the exact state-character table (D/U/N/Z/L/H/T and the
lower-case conflict variants, as I recall it) and whether the strength digits
are the 0–7 Verilog strength levels (highz/small/medium/weak/large/pull/strong/
supply) or a different encoding. Anyone implementing this must read the table
off the standard; do not implement from my recollection.

Conformance to EVCD therefore means: emitting `$var port` declarations for the
**top-level ports of the dumped scope**, and populating, per port and per
change, a direction-aware state plus two strength values.

**What conformance would mean for FST (#68).** Nothing, in the normative
sense. FST ("Fast Signal Trace") is Tony Bybell's GTKWave-native container.
**The C implementation is the specification.** There is no IEEE document, no
RFC, no vendor spec, no versioned format document with a change process.
`fstapi.c` / `fstapi.h` in the GTKWave source tree is the normative artifact,
and the only prose is its block comments plus third-party reimplementation
notes (the Rust `wellen` reader and `fst-writer` writer are the most complete
independent readings I am aware of). *Unverified and load-bearing:* the exact
license header on `fstapi.c` (commonly described as MIT/permissive, which is
why Verilator, Icarus, nvc and cocotb all vendor it) — verify before any
vendoring or transliteration decision, because a GPLv3 project can absorb a
permissive file but the reverse is not true and a transliteration is a
derivative work either way. A "conformance claim" for FST can only ever be
"GTKWave 3.3.x and Surfer/wellen version N open our files and show the right
values" — an interop claim against named tool versions, which is a *weaker*
and more perishable claim than the profile-plus-parser package JLS already has
for VCD.

**What is not being claimed by anything in this section.** Not co-simulation
(rejected, issue #63). Not analog or mixed-signal waveforms. Not
strength-accurate simulation — JLS has no strength model at all (see below),
so no output format can make it strength-aware.

### Implementation procedure

#### Step 0 — establish the facts that decide this (0.5 day, do this first)

Before writing code, three facts must be checked in the tree, because they
close two of the three options:

1. **JLS has no strength model.** `docs/simulation-semantics.md` §2 is
   explicit: the value domain is two-state plus HiZ, "no unknown/X state
   anywhere," and HiZ is all-or-nothing per signal. Tri-state resolution
   (`WireNet.propagate`, `src/jls/elem/WireNet.java:445–481`) picks "the first
   active driver in net order" and warns on multiple active drivers — there is
   no supply/strong/pull/weak lattice to resolve *with*. Grep confirms: no
   `strength` identifier exists anywhere under `src/jls/sim/` or in
   `WireNet.java`.
2. **JLS has no bidirectional port element.** The element set under
   `src/jls/elem/` contains `InputPin.java` and `OutputPin.java` and nothing
   else pin-like; `inout` appears in the tree only inside the reserved-word
   lists of `src/jls/hdl/HdlNames.java:46` and
   `src/jls/hdl/VhdlEmitter.java:908`. There is no `InoutPin`, and the HDL
   emitters never emit an `inout` port.
3. **The VCD signal set is not a port set.** Per `docs/batch-interface.md`
   §4.1 the signals are *watched elements at any hierarchy depth* plus *probed
   wire nets* (`BatchSimulator.findWatched`, `.findProbes`) — a watched
   `Register` deep inside a subcircuit and a named internal net are both
   signals. `$var port` declarations for those would be a category error.

Facts 1–3 mean an EVCD writer would emit, for every record, a constant
strength pair and a state character drawn from a two-element subset of the
table, attached to `$var port` declarations for things that are mostly not
ports. That is not a conformant EVCD file with less information in it — it is a
file that *looks* like it carries drive-strength and direction information and
does not. That is worse than emitting nothing.

#### Step 1 — EVCD (#67): record the negative, do not build (0.5 day)

**Decision: do not implement.** Write it down where it will be found.

- Add a subsection to `docs/batch-interface.md` §4 — placed as **§4.4
  "Extended VCD: not emitted"**, informative, no normative keywords, so §6's
  stability promise is untouched. Three sentences: EVCD's payload is port
  direction and drive strength; JLS's value domain (`simulation-semantics.md`
  §2) has neither, and its element set has no bidirectional pin; an EVCD file
  from JLS would carry constant strength fields and would misrepresent the
  simulator. Cite `WireNet.propagate` as the anchor for "no strength lattice."
- Add a **Recorded decision** to `ARCHITECTURE.md` under the existing
  "Recorded decisions" heading (the section already holds i18n, help delivery,
  look-and-feel, plugin mechanism, plugin trust boundary — this is exactly its
  genre), in the house shape: rationale plus **revisit trigger**. The trigger
  should be a *semantic* one, not a format one: "JLS gains a drive-strength
  value domain or a bidirectional pin element." Absent that, EVCD stays closed
  even if a user asks for it, because the answer to the user is that the data
  does not exist.
- Flip `#67` in `docs/standards-landscape.md` §5 from `COULD` to `OTHER`, and
  amend the paragraph under that table (currently it only editorializes about
  #68) to state the EVCD reason. Also amend §13.1 item 4, which today reads
  "EVCD or FST waveform output (#67, #68). Only if trace size or
  strength/direction information becomes a real complaint" — the
  strength/direction half of that sentence is now known to be unreachable and
  should say so.

Nothing else changes: no flag, no code, no contract surface, no migration.

**Who actually consumes EVCD, for the record.** It is an ATE/vector-translation
and Verdi-lineage format — `vcd dumpports` in Questa, `$dumpports` in VCS,
Verdi/nWave, and tester-vector translators. *Unverified:* whether current
GTKWave reads EVCD (I believe it has some `dumpports` handling; I could not
confirm) and whether Surfer/wellen does (I believe not). Neither uncertainty
changes the decision, because the blocker is upstream of the reader: there is
nothing to put in the file.

#### Step 2 — FST (#68): defer with a named trigger, do not build (0.5 day)

**Decision: do not implement now.** The sizing that justifies this is in
"Effort, risk, and failure modes"; the design facts are:

- **No Java FST writer exists.** There is no FST library on Maven Central I
  could find, and the project has already reached the analogous conclusion one
  row over: `docs/library-survey-2026-07.md:266` records "No credibly
  maintained Java VCD writer exists on Maven Central; the in-tree writer is
  small and pinned by `VcdExportGoldenTest`." FST is a strictly harder target
  than VCD and has strictly less JVM ecosystem. *Unverified negative* — do a
  fresh Maven Central / GitHub search before acting, and record the search
  date in the decision.
- **A from-scratch writer is the only option, and it has no spec to be correct
  against.** Reimplementing `fstapi.c`'s writer path in Java means matching a
  ~7 kloc C file's framing, varint encoding, per-signal value-change chains,
  geometry and blackout sections, and hierarchy encoding, with correctness
  defined only as "GTKWave agrees." Every GTKWave release is an unversioned
  opportunity for drift, and there is no conformance suite to catch it.
- **The compression dependency is a real blocker, not a detail.** FST sections
  use zlib, LZ4 and FastLZ depending on section and writer mode (hierarchy
  blocks in gzip / LZ4 / LZ4-duo variants; value-change blocks in plain and
  "dynamic alias" variants). The JDK gives you **zlib only** —
  `java.util.zip.Deflater` / `GZIPOutputStream`, already used in-tree at
  `src/jls/edit/CircuitSnapshot.java:11,144`. LZ4 would need a new
  dependency (`org.lz4:lz4-java`, Apache-2.0 — GPLv3-compatible, but a new
  shaded jar entry, a new BOM line, and a new supply-chain surface); FastLZ has
  no maintained Java port I know of. *Unverified:* whether a zlib-only FST —
  emitting only the gzip-flavoured section types — is accepted by current
  GTKWave and by wellen. If it is, the dependency question dissolves and the
  cost is "only" the writer; if it is not, the cost includes a new dependency
  against the project's own active-maintenance policy. **This one question
  must be answered against `fstapi.h`'s block-type enum before any estimate is
  trusted.** Note that the project *does* already ship `org.tukaani:xz` 1.12
  (`pom.xml:62–66`, shaded per the maven-shade-plugin config at
  `pom.xml:229–248`) — but XZ/LZMA2 is not an FST codec, so that shipped
  dependency buys nothing here.
- **GTKWave already ships the converter.** Users who genuinely want FST can
  run `vcd2fst out.vcd out.fst` from the GTKWave distribution — maintained by
  the format's author, guaranteed current with the reader, zero JLS
  maintenance. *Unverified:* that `vcd2fst` is still packaged in the GTKWave
  builds shipped by Debian/Ubuntu and Homebrew today (I believe it is; the
  `vcd2fst`/`fst2vcd`/`vcd2lxt` helper set has long shipped with it). Confirm
  before writing it into the docs — but if it holds, it is decisive.

Actions:

- Flip `#68` in `docs/standards-landscape.md` §5 from `COULD` to `OTHER`, or
  keep `COULD` with an explicit gate — either is defensible, but the paragraph
  under the table must be rewritten. It currently claims FST "is a
  self-contained writer," which understates it: it is a self-contained writer
  *of an unspecified format with three codecs, one of which the JDK does not
  have*.
- Record the revisit trigger, in `ARCHITECTURE.md` alongside the EVCD entry.
  Make it conjunctive and falsifiable: **(a)** a real trace-size or
  viewer-load complaint from an actual user that gzipped VCD does not fix,
  **and** **(b)** a maintained JVM FST writer under a GPLv3-compatible
  license appearing on Maven Central. Either alone is insufficient — (a)
  alone gets answered by `vcd2fst`, and (b) alone is a solution without a
  problem.
- Add one informative sentence to `docs/vcd-interop.md` (which is explicitly
  the non-normative recipe page) pointing FST-wanting users at
  `vcd2fst out.vcd out.fst`. This is the whole user-facing deliverable of #68,
  and it costs a line.

#### Step 3 — the cheap path that is actually worth doing: gzip on `-vcd` (2–3 days)

This is the recommendation. It captures the only half of §13.1 item 4 that is
reachable ("if trace size becomes a real complaint") at roughly 3 % of FST's
cost, with zero new dependencies.

**Design decision — suffix-triggered, not a new flag.** `-vcd out.vcd.gz`
writes gzip; `-vcd out.vcd` writes exactly the bytes it writes today. Adding a
`-vcdgz` flag would grow `JLSStart.FLAGS` (`src/jls/JLSStart.java:759–789`),
grow the `-h` output, grow `docs/batch-interface.md` §1's flag table, and grow
`test/jls/CliFlagTableTest.java`, all to express something the filename already
says. The project already uses exactly this convention for `-i`, whose
FlagSpec help text reads "use .jpg/.jpeg for JPEG, .svg for SVG" — extension-
chosen output format is house style. Rejected alternatives: a `-z` modifier
flag (ordering ambiguity with `-vcd`, and the `-v`/`-vcd` longest-prefix rule
in `parseCommandLine` makes short flags in this area expensive to reason
about); always-gzip (breaks every existing consumer); XZ (`org.tukaani:xz` is
already on the classpath, but GTKWave and Surfer do not read `.vcd.xz`, so it
would produce files only JLS can open — the exact anti-goal).

**Files to touch:**

- `src/jls/sim/BatchSimulator.java`, `writeVcd()` at lines 359–369. Today it
  is `Files.write(Paths.get(fileName), toVcd().getBytes(UTF_8))`. Replace with
  a stream write, choosing `GZIPOutputStream` when the file name ends
  `.gz` (case-insensitive, checked on the file name only — not on any
  directory component). Wrap in try-with-resources so SpotBugs at threshold
  High stays quiet. Keep `toVcd()`'s signature and behavior **byte-identical**;
  it is the tested seam (`@jls.testedby` tags at lines 341–342, 357, 381–382
  point at it) and is what the golden asserts.
- `src/jls/JLSStart.java`: **no change**. The `-vcd` operand is already an
  opaque file name (`vcdFile` at line 108, assigned at line 1078, handed to
  `batchSim.setVcdFile(vcdFile)` at line 254, with the write and its error
  path at lines 271–277). The existing error path already produces the correct
  `jls: error: ...` single line and exit 1 for an unwritable target.
- `docs/batch-interface.md`: new **§4.5 "Compressed output"**, normative,
  RFC 2119 keywords per house style. It MUST state: (a) a `-vcd` operand whose
  file name ends in `.gz` (ASCII case-insensitive) is written as a gzip stream
  whose **decompressed content is byte-identical to the §4.2–4.3 output for
  the same run**; (b) any other operand is written uncompressed and unchanged;
  (c) the *decompressed* stream, not the compressed container, is the stable
  contract — the deflate encoding MAY differ between JDK builds. Also update
  §1's invocation line only if you decide the synopsis should hint at it (I
  would leave the synopsis alone and mention it in §4).
- `README.md` line ~129, the `-vcd` bullet: one clause, "or `-vcd out.vcd.gz`
  for a gzip-compressed trace (GTKWave and Surfer read it directly)".
- `docs/vcd-interop.md`: extend §1's recipe with the `.gz` variant and §4's
  autograde discussion with the two-line Python change (`gzip.open` when the
  path ends `.gz`). This file is explicitly informative, so no contract
  implications.
- `CHANGELOG.md`: an "Added" entry. Per `docs/batch-interface.md` §6 this is
  **minor-version material, not a major bump**: it is "a new optional output
  gated behind" a distinguishable operand and "cannot break a conforming
  consumer," since every existing invocation produces the same bytes. Say that
  explicitly in the entry so the next reader does not have to re-derive it.
- `examples/autograde/autograde.py`: `parse_vcd_final_values` takes text and
  the caller reads the file; add gzip detection at the read site. Small, and
  it keeps the runnable example honest.

**Stability-contract callout.** `docs/batch-interface.md` §4 is a frozen
profile and §6 requires a CHANGELOG entry plus either a major bump or a
compatibility flag for any observable byte change. This change is deliberately
constructed to require neither: for every operand a current consumer can pass,
the bytes are unchanged. The test that *proves* that (below) is the price of
claiming it, and it must be written before the feature, not after.

**Migration and compatibility.** None needed. No saved-file format is
touched (`docs/file-format.md` is unaffected — `.jls` container sniffing is a
separate mechanism and stays as documented). No existing command line changes
meaning. Grading scripts that pass `out.vcd` are bit-for-bit unaffected. The
one new failure mode a user can create is asking for `out.gz` and then feeding
it to a tool that does not decompress — which the error is self-describing
about.

**Headless boundary.** `java.util.zip` is not AWT or Swing, so
`test/jls/HeadlessCoreRatchetTest.java` (which forbids `java.awt.`,
`javax.swing.`, `jls.edit.` imports in core, per its regex at line 58) stays
green without special handling. Worth stating in the PR description anyway,
because every change to `jls.sim` is one import away from tripping it.

**A note on the real scaling limit.** `toVcd()` materializes the entire dump
as a `String` before anything is written (line 384, returned at 475, encoded
at 368). On a large run that is a multi-megabyte string plus a full byte-array
copy. If trace size ever *does* become a complaint, the first fix is streaming
the emitter — same output, same golden, no format change — and only then is a
container question worth reopening. Do not let "we should add FST" be the
answer to a memory-footprint problem.

### Testing procedure

Everything below assumes the recommendation (Steps 1, 2, 3). Tests for FST are
sketched at the end only for the case where the revisit trigger fires.

**Existing tests that must stay green unchanged.** `VcdExportGoldenTest`
(all four methods), `VcdProbeExportTest`, `AutogradeBridgeExampleTest`,
`BatchSimulationGoldenTest`, `CliFlagTableTest`, `CliSmokeTest`. If any of
them moves, the change is not what it claims to be.

**New: `test/jls/VcdGzipExportTest.java`** (to be created; `test/jls`,
package `jls`, JUnit 5, `@TempDir`, matching `VcdExportGoldenTest`'s shape —
build the circuit text in a static helper, run a real `BatchSimulator`).

1. `plainOperandIsByteIdenticalToTheUncompressedGolden()` — run the existing
   `waveCircuit()` fixture to `wave.vcd` and assert against
   `VcdExportGoldenTest.WAVE_GOLDEN` (promote it to package-private, or copy
   it with a comment naming the owner). **This is the stability-contract
   test**: it turns red the moment the gzip change leaks into the plain path.
2. `gzOperandDecompressesToTheSameGolden()` — run to `wave.vcd.gz`, read back
   through `GZIPInputStream`, assert equality with the same constant. This is
   the normative §4.5(a) claim, stated as an assertion.
3. `gzHeaderCarriesNoTimestamp()` — assert the first ten bytes are
   `1f 8b 08 00 00 00 00 00 00 ff`. I measured this on JDK 25.0.3 (Ubuntu) in
   this tree's environment: `GZIPOutputStream` writes MTIME = 0 and OS = 0xff,
   and two compressions of identical input in one JVM are byte-identical. The
   assertion exists so that a future JDK that starts stamping wall-clock time
   into the header turns the suite red instead of silently destroying output
   determinism.
4. `twoRunsProduceIdenticalCompressedBytes()` — run the same circuit twice to
   two paths, compare bytes. Same-JVM determinism.
5. `suffixMatchingIsCaseInsensitiveAndFileNameScoped()` — `out.VCD.GZ`
   compresses; a file named `out.vcd` inside a directory called `archive.gz/`
   does not. Cheap, and it pins the one place a naive `contains(".gz")` would
   be wrong.

**Golden-file strategy: do not commit compressed goldens.** Deflate output is
not guaranteed stable across JDK builds or zlib versions, so a committed
`.vcd.gz` golden would be a time bomb that fires on a JDK bump for no
correctness reason. The golden is the **decompressed** text (already committed,
as an inline constant, in `VcdExportGoldenTest`); the compressed layer is
tested by round-trip plus the header assertion plus same-run determinism. Say
this in `docs/batch-interface.md` §4.5(c) so the contract and the test agree.
This is a deliberate departure from the `test/resources/hdl/*.v` file-golden
style used for HDL export, and the reason belongs in the test's Javadoc.

**External-reader validation: `test/jls/WaveformReaderInteropTest.java`**
(to be created). Reuse `test/jls/hdl/ToolLocator.findOnPath` — it is already
the house skip-when-absent helper (`Assumptions.assumeTrue(tool != null, ...)`,
exactly as `IverilogCompileTest:33–35` and `GhdlCompileTest:35` do it), and it
handles Windows `PATHEXT`. Three arms, each independently skipped:

- `gtkwaveConverterAcceptsThePlainVcd()` — if `vcd2fst` is on PATH, run
  `vcd2fst wave.vcd wave.fst`, assert exit 0 and a non-empty output. This is
  the strongest cheap acceptance signal available: the format author's own
  parser accepting our file. It also validates the `vcd2fst` recipe the docs
  will recommend.
- `gtkwaveConverterAcceptsTheGzippedVcd()` — same, on `wave.vcd.gz`, if the
  converter handles gzip input. *Unverified:* whether `vcd2fst` transparently
  decompresses; if it does not, drop this arm and instead assert GTKWave
  proper opens it, or gate on `zcat`. Do not ship a docs claim that "GTKWave
  reads .vcd.gz" until an arm of this test has been observed passing on a
  runner with GTKWave installed.
- `fstRoundTripPreservesValues()` — if both `vcd2fst` and `fst2vcd` are
  present, convert out and back and compare the *semantic* content (parse both
  with the structural checker's logic and compare final values per signal),
  not the bytes. `fst2vcd` will not reproduce JLS's exact profile, so a byte
  comparison would be wrong.

Surfer has no headless verification mode I am aware of, so Surfer stays a
manual check — which is already the documented posture (`VcdExportGoldenTest`'s
class Javadoc says "GTKWave/Surfer validation stays manual, outside CI"). If
you add anything to CI here, that sentence needs updating; leaving it stale is
the kind of drift `CliFlagTableTest` exists to prevent elsewhere.

**Python-parser validation.** `test/jls/AutogradeBridgeExampleTest.java`
already runs `examples/autograde/autograde.py` and skips when `python3` is
absent. Extend the example to accept a `.gz` trace and add a second invocation
mode; the test then covers "a dependency-free Python consumer reads the
compressed form" without new machinery. Python's stdlib `gzip` makes this a
two-line change in the example, which is the point — if a consumer needs a
library to read our output, we picked the wrong container.

**Property/fuzz opportunities.** `docs/library-survey-2026-07.md` records jqwik
as rejected (upstream in maintenance mode), so use the house alternative: a
plain JUnit test with a seeded `java.util.Random`, printing the seed on
failure. The valuable property here is *not* about gzip (round-trip is a
theorem) but about the emitter it wraps: generate random watched-signal
histories — random widths 1–64, random change times, random HiZ intervals —
feed them through `toVcd()`, and assert the structural checker accepts the
result and that a re-parse recovers the input history exactly. That test is
worth writing whether or not gzip ships; it is the one thing the current
two-fixture golden suite cannot give you, and it would have caught, for
example, a `vcdId` collision past 94 signals (`BatchSimulator.vcdId`, lines
509–519, is base-94 and currently exercised only up to a handful of signals).

**CI lane changes (`.github/workflows/ci.yml`).** The `build` lane's "Install
HDL toolchain and virtual display" step (line ~62) already installs
`iverilog ghdl yosys xvfb` best-effort with `|| echo "some optional tools
unavailable; their tests will skip"`. Add `gtkwave` to that apt list, same
step, same best-effort semantics — that arms the interop test on Linux and
leaves Windows/macOS lanes skipping cleanly, exactly as the HDL tests already
do there. Caveat to weigh: the Debian `gtkwave` package pulls GTK/X
dependencies onto the runner. If that is unwelcome given the project's
"X11 is deliberately not part of this project's tooling" stance (README,
"Optional development tools"), the alternative is to leave the interop test
unarmed in CI and run it from the once-per-release manual checklist alongside
`docs/wayland-desktop-checklist.md`. I would arm it in CI — the converters are
CLI programs and the runner is disposable — but the stance is the maintainer's
call and should be recorded either way. **Do not** add gtkwave to
`.devcontainer/Dockerfile` without the same deliberation.

**What turns these tests red.** Any change to `toVcd()`'s bytes (goldens, both
tests). Gzip leaking into the plain path (test 1). A JDK that stamps time into
the gzip header (test 3). A regression in suffix detection (test 5). A change
that makes JLS emit a VCD `vcd2fst` rejects — malformed `$var`, non-monotonic
timestamps, an identifier collision (interop test). A change to the emitter
that breaks history round-tripping at an untested width or with HiZ runs
(property test).

**If the FST trigger ever fires**, the test plan is materially worse and that
is part of the argument: the goldens cannot be byte-exact against any external
authority (there is no reference file), so the suite degenerates to "GTKWave
and wellen both accept it and report the values we expected," which is an
interop assertion pinned to whatever tool versions the runner happens to have.
Budget a `test/jls/FstExportGoldenTest.java` (self-consistency: a matching
in-tree reader, which doubles the code) plus `test/jls/FstInteropTest.java`
(skip-when-absent `fst2vcd`, converting back to VCD and comparing semantics).

### Certification / conformance procedure

**There is no certifying body, no registry, no fee, and no validity period for
any of the three formats.** Not IEEE, not Accellera, not GTKWave. IEEE
standardizes VCD's syntax inside 1364/1800 but operates no conformance program,
publishes no test suite, and issues no marks for it. GTKWave is one
maintainer's project with a format defined by its own source. Nobody can be
asked to assess this and nobody will. **Cost: $0. Elapsed time: 0. Renewal:
n/a. Nothing external invalidates it.** Any vendor claiming otherwise is
selling something.

So the entire question is what a **credible self-assertion** consists of. JLS
already makes one for VCD, and the four legs listed in "What conformance
actually means" are the template. Stated as a procedure a maintainer can
follow for any format claim:

1. **Publish a profile, not a claim of full support.** Enumerate the optional
   constructs used and, explicitly, the ones omitted, with the omission
   rationale. `docs/batch-interface.md` §4.2's parenthetical — "no
   `$date`/`$version` sections — both are optional in the standard, and
   omitting them keeps output byte-deterministic" — is the exemplar. A claim
   of unqualified conformance to a clause nobody has fully implemented is the
   failure mode this avoids.
2. **Anchor every clause of the profile to code.** The batch doc's stated
   discipline ("Every claim below is stated from the implementation and carries
   a code anchor") is what makes it auditable by someone who does not trust
   you. `BatchSimulator.toVcd` / `.vcdValue` / `.vcdId` / `.findWatched` /
   `.findProbes` are all cited.
3. **Test with a checker derived from the document, not the emitter.**
   `vcdIsStructurallyWellFormedAndTwoStatePlusHiZ` re-derives the grammar from
   prose. Without this, goldens only prove the emitter is stable, not that it
   is right — a self-consistent wrong format passes byte goldens forever.
4. **Add an independent-reader acceptance check.** This is the one leg
   currently missing and the one this playbook adds: a third-party parser
   (GTKWave's `vcd2fst`) accepting the output in CI. It is the closest thing
   to external assessment that exists in this space, and it costs one apt
   package.
5. **Freeze it and say what unfreezing costs.** `docs/batch-interface.md` §6
   does this: CHANGELOG plus major bump or compatibility flag.

A project that does all five has a stronger conformance story than a
certificate would give it, because every element is independently checkable by
a reader who downloads the repo. That is worth stating plainly somewhere
user-facing — `docs/vcd-interop.md` §"What JLS offers" already gestures at it
and could carry one more sentence.

**What would invalidate JLS's existing VCD claim,** since nothing external
can: a change to `toVcd()` that violates the published profile without
updating it (caught by the goldens and the structural checker); a documentation
edit that outruns the code (caught by nothing today — the drift risk is real,
and `CliFlagTableTest`'s existence for the flag table shows the project already
recognizes this class of failure); or citing a standard clause nobody has read
(the 1364/1800 clause-number issue flagged above). Fix the last one; it is the
only currently-live threat to the claim's credibility.

**Whether to make an FST or EVCD claim at all.** If either is ever built, the
claim must be phrased as interop, not conformance: "written to be read by
GTKWave 3.3.x and Surfer N; there is no format specification to conform to."
Writing "conforms to FST" would be a false claim in a document whose whole
value is that its claims are true.

### Effort, risk, and failure modes

**Sizing (maintainer-days, single maintainer, includes docs, tests, CI, review):**

| Path | Days | Reasoning |
|---|---|---|
| EVCD (#67): record the negative | **0.5** | Three doc edits (`batch-interface.md` §4.4, `ARCHITECTURE.md` recorded decision, `standards-landscape.md` §5 + §13.1). No code. |
| FST (#68): defer with trigger | **0.5** | Two doc edits plus one `vcd-interop.md` line pointing at `vcd2fst`, plus the Maven Central search that makes the negative honest. |
| **gzip `-vcd` (recommended)** | **2–3** | ~20 lines in `writeVcd`, one new test class (5 methods), one interop test class (3 skipping arms), one CI list edit, four doc edits, one Python example edit, CHANGELOG. |
| Property test for `toVcd()` | **+1** | Optional but recommended; independent of everything above. |
| **Total recommended** | **3–5** | |
| FST if built anyway | **15–25** | See below. |

**Why FST is 15–25 and not 5–8.** The C writer path in `fstapi.c` is several
thousand lines; a minimal write-only subset is plausibly 1,200–2,000 lines of
Java (the Rust `fst-writer` crate is the closest existence proof of a subset
writer and is not small). On top: an in-tree reader for self-consistency
testing, because there is no reference file to golden against (call it +40 %);
the codec question, which is either free (zlib-only sections accepted) or a new
shaded dependency with BOM, reproducibility, and supply-chain consequences; and
a debugging loop whose only oracle is "GTKWave rendered something plausible."
The range is wide because the zlib-only question is unanswered — that is
exactly why Step 2's first action is to answer it, cheaply, before anyone
commits.

**Top 3 ways this goes wrong:**

1. **The gzip change leaks into the plain path and silently breaks the
   stability contract.** A refactor of `writeVcd` that routes both cases
   through one stream, or an over-broad suffix check, changes bytes for
   consumers who never asked for compression. Grading scripts that diff VCDs
   would break in the field, not in CI, and the project would have violated
   §6 without a major bump. Mitigation: test 1 above, written first;
   `toVcd()` untouched; suffix match on the file name only. This is the single
   highest-probability failure in the whole plan and it is fully preventable.
2. **Committing a compressed golden.** The tempting move — golden the `.gz`
   bytes, it is deterministic today — makes a JDK upgrade fail the suite for a
   reason that is not a defect, and the likely "fix" under time pressure is to
   re-record the golden, which trains everyone to re-record goldens. Mitigation:
   §4.5(c) states the compressed container is explicitly outside the contract,
   and the test Javadoc says why no binary golden exists.
3. **Someone starts the FST writer.** It is a genuinely interesting problem
   with a clear-looking scope, which is precisely the trap. Two-thirds through,
   the codec question turns out to require LZ4, or GTKWave 3.3.(n+1) rejects a
   section variant, and the project owns a 2,000-line unspecified binary
   emitter with no conformance suite, one user, and permanent bit-rot exposure
   — in a codebase whose stated policy rejects heavyweight machinery without
   demand. Mitigation: the conjunctive revisit trigger, plus `vcd2fst` in the
   docs as the standing answer, plus the honest range in this table.

**Do NOT do the gzip work if** any of these hold: no user has ever mentioned
trace size (then it is speculative feature work and the correct response is a
one-line docs note that `gzip out.vcd` exists); or the maintainer is unwilling
to add test 1 (the contract assertion is the deliverable, the compression is
incidental); or `docs/batch-interface.md` §4 is mid-revision for another reason
(do not stack a contract-adjacent edit on an unsettled contract).

**Do NOT do EVCD, ever, under the current simulation semantics.** Not "defer" —
the data does not exist. The only thing that reopens it is a change to
`docs/simulation-semantics.md` §2's value domain, which is a far larger
decision that would be made for its own reasons.

**Do NOT do FST unless both trigger conditions hold.** And if only condition
(a) fires — a real complaint about trace size — the ordered cheaper responses
are: gzip (this playbook), then streaming `toVcd()`, then `vcd2fst`. Exhaust
all three before reopening.

### Sources

**Repository (all read and verified in this tree):**

- `/home/user/JLS/docs/batch-interface.md` — §1 invocation/exit contract, §4
  the normative VCD profile (§4.1 signal set, §4.2 header, §4.3 value mapping),
  §5 golden-test relationship, §6 stability promise (the "new optional output
  behind a new flag is minor-version material" clause this plan relies on).
- `/home/user/JLS/docs/vcd-interop.md` — informative recipe; GTKWave/Surfer
  usage; the `examples/autograde` bridge; the co-simulation rejection (#63).
- `/home/user/JLS/src/jls/sim/BatchSimulator.java` — `setVcdFile` (344–347),
  `writeVcd` (359–369, the `Files.write` call site to change), `toVcd`
  (384–476), `fold` (489–498), `vcdId` (509–519, base-94), `vcdValue`
  (538–555, the 0/1/z mapping).
- `/home/user/JLS/src/jls/JLSStart.java` — `vcdFile` field (108), `FLAGS`
  table (759–789) including the `-vcd` FlagSpec (778) and the `-i`
  extension-chooses-format precedent (765), `setVcdFile` call (254), VCD write
  and error path (271–277), the `-v`/`-vcd` longest-prefix note (755–758).
- `/home/user/JLS/test/jls/VcdExportGoldenTest.java` — `WAVE_GOLDEN` (72–107),
  `STIM_GOLDEN` (155–177), the spec-derived structural checker
  `vcdIsStructurallyWellFormedAndTwoStatePlusHiZ` (238–320), the CLI golden
  test (325–365), and the class Javadoc's "GTKWave/Surfer validation stays
  manual, outside CI".
- `/home/user/JLS/test/jls/VcdProbeExportTest.java`,
  `/home/user/JLS/test/jls/AutogradeBridgeExampleTest.java` (skip-when-absent
  `python3`), `/home/user/JLS/test/jls/HeadlessCoreRatchetTest.java`
  (forbidden-import regex, line 58).
- `/home/user/JLS/test/jls/hdl/ToolLocator.java` — `findOnPath` (68–73), the
  reusable skip-when-absent locator; `IverilogCompileTest.java:33–35` and
  `GhdlCompileTest.java:35` for the `Assumptions.assumeTrue` idiom.
- `/home/user/JLS/docs/simulation-semantics.md` — §2 "Value domain: two states
  plus HiZ" (42–66), tri-state resolution (411–429).
- `/home/user/JLS/src/jls/elem/WireNet.java:445–481` — multi-driver resolution,
  "first active driver in net order wins"; no strength lattice.
- `/home/user/JLS/src/jls/elem/` directory listing — `InputPin.java`,
  `OutputPin.java`, no bidirectional pin element.
- `/home/user/JLS/src/jls/hdl/HdlNames.java:46`,
  `/home/user/JLS/src/jls/hdl/VhdlEmitter.java:908` — `inout` present only as a
  reserved word.
- `/home/user/JLS/pom.xml` — `org.tukaani:xz` 1.12 (62–66), maven-shade-plugin
  self-contained-jar config (229–248), SpotBugs (628–630), enforcer (601–620).
- `/home/user/JLS/src/jls/edit/CircuitSnapshot.java:11,144` — existing in-tree
  `java.util.zip.Deflater` use.
- `/home/user/JLS/docs/library-survey-2026-07.md:266` — "No credibly maintained
  Java VCD writer exists on Maven Central"; the active-maintenance policy that
  governs any new dependency.
- `/home/user/JLS/docs/standards-landscape.md` — §5 table rows 66–73 and the
  paragraph beneath (234–250); §13.1 item 4 (738–739).
- `/home/user/JLS/ARCHITECTURE.md:233+` — "Recorded decisions" section and its
  rationale-plus-revisit-trigger house shape.
- `/home/user/JLS/.github/workflows/ci.yml` — the best-effort
  `iverilog ghdl yosys xvfb` apt step (~61–62) and the `mvn -B verify` step.
- `/home/user/JLS/README.md` — `-vcd` bullet (~128–131), "Optional development
  tools" and its explicit no-X11 stance, single-self-contained-jar deployment.
- `/home/user/JLS/examples/autograde/autograde.py:76–113` —
  `parse_vcd_final_values`, the dependency-free consumer that must keep working.

**Measured in this environment (reproducible, not cited from memory):**

- `java.util.zip.GZIPOutputStream` on JDK 25.0.3 (Ubuntu) writes header bytes
  `1f 8b 08 00 00 00 00 00 00 ff` — MTIME zero, OS 0xff — and produces
  byte-identical output for identical input across two calls in one JVM.
- Compression ratio for VCD text in JLS's exact §4.2/§4.3 profile: **4.8×** on
  a 29.4 MB synthetic trace of 34 signals with uniformly random 32-bit vectors
  (worst case, incompressible payload), **6.6×** on a 6.9 MB structured trace
  (incrementing PC, toggling clock, correlated registers — closer to a real
  CPU run). Measurement scripts were not retained; regenerate rather than trust
  the numbers if they become load-bearing.
- `java -jar target/jls-5.0.5-SNAPSHOT.jar -b -vcd ... test/fixtures/riscv-sum1to10.jls`
  produces a 1,332-byte VCD — the fixture needs external stimulus
  (`riscv/jlsrun.py`) to run, so it is *not* a large-trace example. Useful
  context: nothing in the tree currently produces a trace where container
  choice matters.

**External, and explicitly UNVERIFIED — do not write any of these into repo
docs without checking the primary source:**

- IEEE 1364-2001 clause 18 subclause numbering for four-state vs extended VCD,
  and the corresponding clause numbers in the current IEEE 1800 revision.
  Both standards are paywalled and were not opened.
- The EVCD port-state character table, the exact `$var port` / `$dumpports`
  syntax, and whether the two strength fields are the 0–7 Verilog strength
  levels. Recollected, not read.
- Which tools consume EVCD today (Questa `vcd dumpports`, VCS `$dumpports`,
  Verdi/nWave, ATE vector translators — believed); whether current GTKWave or
  Surfer/wellen read EVCD at all.
- FST: that `fstapi.c`/`fstapi.h` carry a permissive (MIT-style) license; the
  exact block-type enumeration and which sections admit zlib-only encoding;
  whether current GTKWave and wellen accept a zlib-only FST. **This last one
  is the load-bearing unknown for any FST cost estimate.**
- That `vcd2fst` / `fst2vcd` still ship in distribution GTKWave packages
  (believed; central to both the docs recommendation and the interop test).
- That GTKWave and Surfer read `.vcd.gz` transparently (believed; the CI
  interop arm is designed to convert this belief into an observation before
  the docs assert it).
- That no JVM FST writer exists on Maven Central (searched from knowledge, not
  from a live query in this environment — redo the search and date it).
- Rust `wellen` / `fst-writer` as the most complete independent readings of
  FST, and their approximate size as a proxy for a Java subset writer.
