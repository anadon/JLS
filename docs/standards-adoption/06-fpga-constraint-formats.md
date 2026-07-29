## Vendor pin-constraint emitters: XDC, QSF, LPF (#82) and the board table

### What conformance actually means

There is no standards body here and no conformance suite. XDC, QSF, and LPF are
vendor de-facto formats documented only in vendor user guides:

- **XDC** — AMD/Xilinx Vivado's constraint language: a restricted Tcl subset,
  itself a restricted dialect of Synopsys SDC (landscape entry #93, marked
  ADJACENT). Documented in AMD *UG903, Vivado Design Suite User Guide: Using
  Constraints* (document number verified as UG903; the current revision/year is
  **unverified** and must be cited by revision when the acceptance record is
  written).
- **QSF** — Intel/Altera "Quartus Settings File": a flat list of Tcl-ish
  `set_*_assignment` commands read by Quartus Prime. Documented in the Intel
  *Quartus Prime Settings File Reference Manual* (**revision unverified**).
- **LPF** — Lattice "Logical Preference File", consumed by Lattice Diamond and,
  for the ECP5/Nexus families, by the open `nextpnr-ecp5`/prjtrellis flow.
  Documented in the Diamond preference/constraint reference (**document number
  and revision unverified**).

None of the three has a published grammar, a version number a file can declare,
or a validator. So "conformance" here is not a clause-by-clause claim against a
specification. It is exactly one testable proposition, and the section that
follows exists to make that proposition checkable:

> For each board in `jls.hdl.board.Boards`, the constraint file JLS emits
> alongside an `-export` of a JLS-authored design is accepted without error by
> that board's vendor toolchain, and places every top-level port at the physical
> pin the bindings file named.

**Explicitly claimed:** pin location, I/O standard, and (where the board declares
an oscillator frequency) a single board-clock period constraint; byte-determinism
of emission; all-or-nothing binding.

**Explicitly not claimed:** general timing constraints (no SDC coverage — no
input/output delays, no multicycle or false paths, no clock groups); timing
closure; correctness of the transcribed pin table beyond "it matches the vendor's
published master constraint file"; bitstream generation or programming (that is
issue #215); any support for constraints a user hand-adds afterwards; and
anything about designs JLS did not export (the emitter walks
`HdlExporter.buildModel`'s port list — hypothesis H1 of #213 — so it can only
constrain what JLS itself emitted).

**The artifact a claim rests on** is three things, and they are of different
strengths:

1. The committed golden files under `test/resources/hdl/board/` plus the emitter
   sources. These prove *shape and determinism*, nothing about vendor acceptance.
   `test/resources/hdl/board/blinky_icestick.pcf` is the shipped example.
2. For LPF (and PCF), an *automated* external-tool check: `nextpnr-ecp5` /
   `nextpnr-ice40` parsing the file against a real device database in CI, armed
   when the tool is on `PATH` (the `jls.hdl.ToolLocator` +
   `Assumptions.assumeTrue` pattern already used by
   `test/jls/hdl/GhdlCompileTest.java`).
3. For XDC and QSF, a *manual, dated, per-release* acceptance record — the only
   possible evidence, because Vivado and Quartus cannot run in this project's CI
   (see "Certification"). This is the same evidentiary regime as
   `docs/wayland-desktop-checklist.md`.

Any wording stronger than "accepted by Vivado 20xx.y on the Basys 3, recorded on
<date>" would be false. There is no "Vivado-certified" status to claim.

### Implementation procedure

**Step 0 — the honest state of the code (read before designing).** There is *no*
constraint-emitter interface today. `jls.hdl.HdlEmitter` is the **language**
emitter seam (Verilog/VHDL) and is unrelated: its contract is
`String emit(HdlModel)` with no board and no binding, and it is the type behind
the `hdl.exporter` extension point (`src/jls/hdl/HdlExtensionPoints.java`,
catalogued in `docs/extension-points.md:32`). The constraint side is a single
final class with static methods, `src/jls/hdl/board/PcfEmitter.java`, whose
`emit(HdlModel, Board, PinBindings)` does four separable jobs in one method:
guard the format (line 61), walk-and-validate the bindings (lines 66–119),
render PCF lines (lines 102–105), and prepend a header (lines 121–131). It even
guards against being handed a non-PCF board with an `IllegalArgumentException`
— a placeholder for exactly this work. **`PcfEmitter` must be generalized before
a second format is added**; adding XDC by copy-paste would fork the
all-or-nothing validation logic, which is the part that must never diverge
between formats.

**Step 1 — extract the binder (no behaviour change).** Create
`src/jls/hdl/board/PinBinder.java`, moving the port walk from `PcfEmitter.emit`
lines 66–119 and the two helpers `diagnoseLeftover` / `portList` verbatim,
including every error string. Give it:

```java
public record BoundPin(String key, HdlModel.Direction direction,
        String pinName, Board.Pin pin) { }

static List<BoundPin> bind(HdlModel model, Board board, PinBindings bindings)
        throws HdlExportException;
```

Order is the existing order — `model.ports()` order, bits ascending — and the
exception message keeps the existing `cannot bind module "X" to board Y: ...`
prefix and `; `-joined error list. The regression guard for this refactor is
that `PcfGoldenTest` and `UnbindablePortsTest` pass **unchanged**; do not touch
either file in this commit.

**Step 2 — the emitter interface and dispatcher.** New
`src/jls/hdl/board/ConstraintEmitter.java`:

```java
public interface ConstraintEmitter {
    Board.Format format();
    Constraints emit(HdlModel model, Board board, PinBindings bindings)
            throws HdlExportException;
}
```

with `record Constraints(String text, List<String> warnings)` mirroring
`HdlExporter.Result` (same immutable-copy compact constructor), so an emitter
can say "clk is on a non-clock pin, no timing constraint emitted" through the
channel `JLSStart` already prints (`jls: warning: ...`, `src/jls/JLSStart.java`
lines 433–435).

New `ConstraintEmitters.forFormat(Board.Format)` returns the stateless singleton
for a format. `JLSStart` line 427 changes from
`PcfEmitter.emit(model, board, bindings)` to
`ConstraintEmitters.forFormat(board.format()).emit(model, board, bindings)`.

**Recommendation, not a survey:** do **not** route constraint emitters through
`jls.module`/`ExtensionPoint`. A `hdl.constraints` extension point would cost a
row in `docs/extension-points.md`, an update to
`test/jls/ExtensionPointCatalogTest.java`, and a registry lookup, and would buy
nothing: the format set is closed by the board table, and the project has
recorded a rejection of heavyweight plugin mechanisms (ARCHITECTURE.md, "Plugin
mechanism: removed (5.0.0, #80)"). A four-entry switch in `ConstraintEmitters`
is the right size. Keep `PcfEmitter` as a `ConstraintEmitter` implementation;
its public static `emit` may be deleted once `JLSStart` no longer calls it
(nothing outside the repo depends on `jls.hdl.board` — it is not part of any
stability contract), but the `@jls.testedby` tags in its javadoc must be moved
to whatever methods survive (the tag is a real build feature: `pom.xml` lines
560–564 register `jls.testedby` as a custom javadoc block tag).

**Step 3 — extend `Board` with the metadata the vendor formats need.** Today
`Board.pins` is `Map<String, String>` (pin name → location). That carries less
than any of the three new formats requires. Replace it with a small value type
in the same file or a sibling:

```java
public record Pin(String location, @Nullable IoStandard io,
        @Nullable Long clockHz, Pull pull, @Nullable Integer driveMilliamps) {
    public static Pin at(String location) { ... }              // inherits board default
    public static Pin clock(String location, long hz) { ... }
}
public enum Pull { NONE, UP, DOWN }
```

and add to `Board`: `IoStandard defaultIo` (so table entries stay one line each),
`String part` (the exact ordering-code device string, e.g. `XC7A35T-1CPG236C`),
and `String family` (Quartus's `FAMILY` value, e.g. `MAX 10`).

**The I/O-standard decision matters and has one right answer:** the *token*
differs per vendor for the same electrical standard — Vivado writes
`LVCMOS33`, Quartus writes `"3.3-V LVTTL"`, Lattice writes `LVCMOS33`. Store the
electrical standard as an enum and let each emitter spell it:

```java
public enum IoStandard {
    LVCMOS33("LVCMOS33", "3.3-V LVTTL", "LVCMOS33"),
    LVCMOS25(...), LVCMOS18(...);
    public String xdc(); public String qsf(); public String lpf();
}
```

This keeps the board table describing *the board* and keeps vendor syntax inside
vendor emitters — the same separation `HdlModel`/`HdlEmitter` already enforces
for languages. A board entry that carried a Vivado string would be a design bug.

Compatibility: `Board`, `Pin`, `Boards`, and the emitters are internal API in
`jls.hdl.board`. They appear in **no** stability contract —
`docs/batch-interface.md` covers `-b`/`-t`/stdout/`-vcd` only, and
`docs/file-format.md` covers saved circuits. Nothing a user has on disk changes.
`test/jls/hdl/board/BoardPinOrderTest.java:29` constructs `new Board(...)`
directly and must be updated with the signature.

**Step 4 — the exact syntax each emitter produces.** Keep the shipped house
style: a three-line header, a blank line, then per port bit a `# <direction>
<key> <- pin <name>` provenance comment followed by the constraint line(s).
Single spaces, no column padding (padding is deterministic but makes every
golden churn when a long port name appears).

*XDC* (`out.xdc`), for `read_xdc` / `add_files -fileset constrs_1`:

```
# Board: basys3 (AMD Artix-7 XC7A35T-1CPG236C)
# Module: blinky, exported by JLS 5.0.0
# Format: XDC, for AMD Vivado (add to constrs_1, or read_xdc)

# input sw[0] <- pin SW0
set_property -dict { PACKAGE_PIN V17 IOSTANDARD LVCMOS33 } [get_ports {sw[0]}]
# input clk <- pin CLK100MHZ (100.000 MHz board oscillator)
set_property -dict { PACKAGE_PIN W5 IOSTANDARD LVCMOS33 } [get_ports {clk}]
create_clock -add -name clk -period 10.000 -waveform {0 5.000} [get_ports {clk}]
```

`IOSTANDARD` is not optional in practice: a port with an unset I/O standard trips
Vivado's `UCIO-1` / `NSTD-1` DRC at bitstream time, which is an *error* by
default. Braces around the port name are required for bus bits.

*QSF* (`out.qsf`):

```
# Board: de10lite (Intel MAX 10 10M50DAF484C7G)
# Module: blinky, exported by JLS 5.0.0
# Format: QSF, for Intel Quartus Prime (source this from your project .qsf)

set_global_assignment -name FAMILY "MAX 10"
set_global_assignment -name DEVICE 10M50DAF484C7G
set_global_assignment -name TOP_LEVEL_ENTITY blinky

# input sw[0] <- pin SW0
set_location_assignment PIN_C10 -to sw[0]
set_instance_assignment -name IO_STANDARD "3.3-V LVTTL" -to sw[0]
# input clk <- pin MAX10_CLK1_50 (50.000 MHz board oscillator)
set_location_assignment PIN_P11 -to clk
set_instance_assignment -name IO_STANDARD "3.3-V LVTTL" -to clk
# Quartus takes timing from an .sdc, not this file; add to your project:
#   create_clock -period 20.000 -name clk [get_ports clk]
```

Emit the three globals (they are idempotent when the file is sourced into a
project whose device already matches, and they make a bare `.qsf` usable) but
emit **no** timing constraint: Quartus reads timing from SDC via TimeQuest, and
a `create_clock` in a `.qsf` is silently useless. Unquoted `-to sw[0]` matches
Quartus's own generated assignment style; **verify at first acceptance run**
(this is one of the two naming details most likely to be wrong).

*LPF* (`out.lpf`), for `nextpnr-ecp5 --lpf` and Diamond:

```
# Board: ulx3s (Lattice ECP5 LFE5U-85F-6BG381C)
# Module: blinky, exported by JLS 5.0.0
# Format: LPF, for the open ECP5 flow (yosys + nextpnr-ecp5 --lpf)

# input sw[0] <- pin SW0
LOCATE COMP "sw[0]" SITE "E8";
IOBUF PORT "sw[0]" IO_TYPE=LVCMOS33;
# input clk <- pin CLK25 (25.000 MHz board oscillator)
LOCATE COMP "clk" SITE "G2";
IOBUF PORT "clk" IO_TYPE=LVCMOS33;
FREQUENCY PORT "clk" 25.000000 MHZ;
```

Bracketed bus-bit names (`"sw[0]"`) match the port names in Yosys's JSON netlist
and therefore what `nextpnr-ecp5` looks for — and match what the shipped PCF
emitter already writes (`set_io led[1] 98`). Diamond/Synplify may instead flatten
bus bits to `sw_0`; **this is unverified and is an open question**, not a fact to
assert in docs. Emit the bracketed form (the target flow is the open one) and
record the Diamond behaviour when someone first runs Diamond.

**Step 5 — the clock decision (the one genuinely subtle design call).** A JLS
`Clock` element becomes the input port `clk` with a comment naming its *cycle in
simulation units* (`HdlExporter` lines 271–285: "cycle N units (high M); drive
this input from the testbench"). Simulation units have no physical meaning.
Therefore, normatively:

- The emitter **MUST NOT** derive any period, frequency, or waveform from the
  JLS `Clock` element's `cycle`/`one` attributes.
- The emitter **MUST** emit a timing constraint only when the *board pin* the
  port is bound to declares `clockHz`, and **MUST** compute the period from that
  board oscillator frequency, printed with fixed 3-decimal nanoseconds so the
  bytes are stable.
- When a port named by the model as a clock (the `Clock`-derived port) is bound
  to a pin with no `clockHz`, the emitter **MUST** emit the location constraint,
  **MUST NOT** emit a timing constraint, and **MUST** return a warning:
  `"clk is bound to pin PMOD1, which is not a clock-capable pin of icestick;
  no clock constraint emitted"`. On some parts this is also a placement error
  the vendor tool will raise later — the warning is the early tell.

**Step 6 — adding a board.** The procedure, unchanged in spirit from #213 H2
(the table grows on demand; JLS never gains a board-description file format):

1. Add one `private static final Board` constant to
   `src/jls/hdl/board/Boards.java`, with the pin map transcribed **only** from
   the vendor's published master constraint file (Digilent's board master XDC,
   Terasic's system-builder QSF, the board vendor's reference LPF).
2. Record provenance in the constant's javadoc: document title, revision, and
   date. `ICESTICK`'s javadoc (Boards.java lines 26–33) already does this in
   prose; make it a rule and include the revision so a later reader can re-check
   the transcription.
3. Append to `ALL` (`Boards.java:81`) — the list is documentation order and
   drives `Boards.names()` in the usage text and unknown-board errors.
4. Add a golden for the shared fixture and a row in the parameterized golden
   table (Testing, below). `BoardTableTest` fails if you skip this.
5. Add a row to the table in `docs/hdl-support-research.md` §7.5 and a CHANGELOG
   entry.

Recommended first entries, one per format: **Basys 3** (Artix-7
`XC7A35T-1CPG236C`) for XDC, **DE10-Lite** (MAX 10 `10M50DAF484C7G`) for QSF,
**ULX3S** (ECP5 `LFE5U-85F-6BG381C`) for LPF — the ULX3S first, because it is the
only one whose constraint file a CI machine can check. Exact pin letters/numbers
in this document are illustrative and **unverified**; transcribe them at
implementation time.

**Step 7 — the CLI surface, and what it must not break.** `-board` and `-pins`
keep their arity, their pairing rules (`JLSStart` lines 908–916: both require
`-export`, and each requires the other), their case-insensitive board matching,
and their exit codes (2 usage, 1 runtime). The constraint file name is already
format-driven — `JLSStart` lines 470–474 strip `.v`/`.vhd`/`.vhdl` and append
`board.format().extension()` — so a board whose `Format` is `XDC` writes
`out.xdc` with **no CLI change at all**. That is the whole point of the existing
design and it should not be redesigned.

Two required edits and three recommended refusals:

- **Required:** `JLSStart.java:783` hardcodes "`(.pcf)`" in the `-board` flag
  help. Change to format-neutral wording ("also write the board's pin-constraint
  file — `.pcf`/`.xdc`/`.qsf`/`.lpf` — next to the HDL"). `jls -h` already lists
  the supported board names because the help string interpolates
  `Boards.names()`; `test/jls/CliFlagTableTest.java` asserts usage is *generated
  from* the flag table rather than pinning this literal, so the edit is safe, but
  re-run `HelpTopicsTest` too.
- **Required:** at the wiring point (both the language emitter and the board are
  in hand, `JLSStart` lines 283–286 and 421–428), warn when the export language
  is VHDL, the format is XDC or QSF, and any port name contains an uppercase
  letter. `HdlNames` preserves case, VHDL identifiers are case-insensitive, and
  Vivado's elaborated netlist typically lowercases them — so `[get_ports {LED}]`
  can match nothing and `set_property` then *warns* rather than errors, silently
  dropping the pin assignment. That is the nastiest available failure and costs
  five lines to flag.
- **Recommend refusing:** per-binding attributes in the `-pins` file
  (`led[0] LED1 pull=up`). The two-token grammar is documented in
  `docs/hdl-support-research.md` §7.5 and in the CHANGELOG; turning it into a
  key/value mini-language is the first step toward the board-description format
  #213 refused. If it ever becomes necessary, it is a documented deviation:
  trailing `key=value` tokens, unknown keys rejected with the line number, plus
  a CHANGELOG entry.
- **Recommend refusing:** new flags such as `-iostd`/`-freq`. Electrical
  properties belong to the board, not to the invocation.
- **Recommend refusing:** a `-boards` listing flag. `jls -h` and the
  unknown-board error already list the table.

**Step 8 — invariants that still hold.** `jls.hdl.board` is headless (no
`java.awt`/`javax.swing` imports; `test/jls/HeadlessCoreRatchetTest.java`
enforces it) and `@NullMarked` (`package-info.java`), so the new nullable record
components need explicit `@Nullable` or NullAway fails the build under
`-Werror`. Emission must stay byte-deterministic: sort nothing at emit time —
order comes from the model port walk — and format all numbers with
`Locale.ROOT`.

**Docs to update:** `docs/hdl-support-research.md` §7.5 (the board table gains
rows; add a short "what each format carries" subsection stating the normative
clock rule from Step 5), `README.md` line ~135 (the export bullet says
"Verilog export"; it does not currently mention `-board` at all — worth fixing
while there), and `CHANGELOG.md`. No change to `docs/batch-interface.md` or
`docs/file-format.md`: neither contract is touched.

### Testing procedure

**Golden files** (house style, `test/resources/hdl/board/`): one per
(board, format) pair, named `blinky_<board>.<ext>`, built from the existing
`test/jls/hdl/board/BoardFixtures.java` fixture (`sw[1:0]` in, `clk` in,
`led[1:0]` out — every port shape the exporter produces) with a per-board
bindings list. The `@VERSION@` tokenization and the
`-Djls.hdl.regenerate=true` regeneration switch already exist in
`test/jls/hdl/board/PcfGoldenTest.java`; copy that mechanism exactly.

New test classes (all **to be created**, under `test/jls/hdl/board/`):

- **`ConstraintGoldenTest.java`** — one `@ParameterizedTest` over a table of
  (board name, bindings, golden file). Asserts byte equality against the golden,
  and asserts the same double-walk determinism check `PcfGoldenTest`
  (`emissionIsDeterministic`) does — two independently loaded circuits must
  produce identical bytes. Leave `PcfGoldenTest` untouched so the PCF refactor
  keeps its own dedicated guard.
- **`BoardTableTest.java`** — the anti-drift tests: every `Board.Format` value
  has an entry in `ConstraintEmitters.forFormat` (loop `Format.values()`, so a
  new enum constant with no emitter fails at test time, not at a user's command
  line); every board name is lower-case and unique; every pin location is
  non-blank; every board resolves an `IoStandard` for every pin (board default or
  explicit); and **every board in `Boards.all()` appears in
  `ConstraintGoldenTest`'s table** — this is what turns red when someone adds a
  board without a golden.
- **`ConstraintBindingFailureTest.java`** — the all-or-nothing cases, per format,
  extending `UnbindablePortsTest`'s coverage across the new emitters via the
  shared `PinBinder`: missing binding, unknown board pin (message lists the
  available pins), unknown port (message lists the ports), scalar form for a wide
  port and vice versa, out-of-range bit, and one pin claimed twice. Plus the new
  case: `clk` bound to a non-clock-capable pin produces a **warning** and a file,
  not an exception.
- **`ConstraintEmitterPropertyTest.java`** — property/fuzz, in the style of the
  existing `test/jls/GenerativeRoundTripFuzzTest.java` and
  `test/jls/ContainerMutationFuzzTest.java`. Over randomly generated port sets
  (legal identifiers, widths 1–16) and random valid bindings, assert the
  format-independent invariants: every port bit appears exactly once in the
  output; every emitted location is a location of the board; no location appears
  twice; emission is a pure function of (model, board, bindings). This is where a
  bad `Board.NATURAL_PIN_ORDER` interaction or a lost bit would surface.
- **`CliVendorBoardExportTest.java`** — subprocess-level, cloned from
  `test/jls/hdl/board/CliBoardExportTest.java`: for each new board,
  `jls -export out.v -board <name> -pins pins.txt blinky.jls` exits 0 and leaves
  `out.<ext>` beside `out.v` with no `.tmp` survivor, and an unbindable port
  exits 1 leaving **nothing** on disk (no HDL, no constraint file, no temp).

**External-tool validation, and where it stops.**

- **LPF and PCF can be machine-checked.** New
  **`test/jls/hdl/board/NextpnrConstraintTest.java`** (to be created), following
  `test/jls/hdl/GhdlCompileTest.java` exactly: locate the tool with
  `jls.hdl.ToolLocator.findOnPath` (public, in `test/jls/hdl/`, reusable),
  `Assumptions.assumeTrue(tool != null, ...)` to skip when absent, then run
  `yosys -p 'synth_ecp5 -json x.json' out.v` followed by
  `nextpnr-ecp5 --85k --package CABGA381 --lpf out.lpf --json x.json
  --textcfg out.config`, asserting exit 0 with the tool's output in the failure
  message. The `--pcf` equivalent for `nextpnr-ice40 --hx1k --package tq144`
  arms the *existing* iCEstick golden, which today has **no** external
  validation at all. **Do this one first**: it is the cheapest confidence win in
  this whole item and it also validates the shared `PinBinder` refactor against
  a real place-and-route tool.
  What it proves: the file parses, every named site exists on that part, and the
  design fits with those constraints. What it does not prove: that Lattice
  Diamond accepts the same text.
- **CI lane change:** `.github/workflows/ci.yml:62` currently installs
  `iverilog ghdl yosys xvfb` best-effort (`|| echo "some optional tools
  unavailable; their tests will skip"`). Append `nextpnr-ice40 nextpnr-ecp5`
  (and, for the ECP5 chip database, whatever package the distro splits it into).
  Whether `ubuntu-latest`'s repositories carry those package names is
  **unverified** — but the best-effort `|| echo` and the assumption-based skip
  mean a missing package degrades to a skip rather than a red build, which is
  exactly the failure mode the existing line was written for. The Windows lane
  already downloads a version-pinned `oss-cad-suite` bundle
  (`.github/workflows/ci.yml:153`) and prepends its `bin` to `PATH`; that bundle
  is expected to contain `nextpnr-ice40`/`nextpnr-ecp5` as well (**unverified** —
  the workflow comment only names iverilog, ghdl, yosys), so the Windows leg may
  arm for free.
- **Vivado and Quartus cannot be in CI, and that is final.** Multi-tens-of-GB
  installers, mandatory account registration and click-through licence
  acceptance, EULA terms that make redistributing or caching the installer
  legally fraught, and GitHub-hosted runner disk (~14 GB free) and 6-hour job
  limits. A self-hosted rig could in principle host them, but the project has one
  maintainer and its self-hosted rigs today are for GUI/desktop verification
  (`scripts/wayland-rig.sh`, `scripts/macos-rig.sh`,
  `scripts/windows-rig.ps1`), not multi-hour FPGA builds. **Consequence, stated
  plainly:** XDC and QSF emission will be *golden-pinned but never
  machine-validated*. The docs must not claim otherwise, and the manual
  acceptance record below is the only evidence that exists for those two formats.

**What regression turns each test red:** any byte change in emitted constraints
(goldens); a reordering of the shared port walk (every constraint golden fails at
once, which is the diagnostic signal that `PinBinder`, not one emitter, changed);
a new `Format` without an emitter or a new board without a golden
(`BoardTableTest`); a lost or duplicated port bit under an unusual name
(`ConstraintEmitterPropertyTest`); a site name that does not exist on the target
part (`NextpnrConstraintTest`, armed lanes only); a change that lets a partial
file reach disk (`CliVendorBoardExportTest`).

### Certification / conformance procedure

**There is no external body, no registry, no accreditation, and no fee.** AMD,
Intel, and Lattice do not certify third-party constraint-file writers; none of
them publishes a conformance suite or a self-certification programme for XDC,
QSF, or LPF. This item is **entirely self-asserted**, and the credible form of
that self-assertion is a *recorded, versioned acceptance run against the real
tool* — the same shape as the once-per-release Wayland spot-check.

**Create `docs/fpga-acceptance-checklist.md`** (to be created), modelled
directly on `docs/wayland-desktop-checklist.md`: status line ("release
procedure"), an environment section, one numbered section per format, a failure
triage rule, and a paste-as-issue-comment results template. Results are recorded
as a comment on the tracking issue (the #213 follow-up that carries #82), one per
release, exactly as the Wayland checklist posts to #100.

**Per-format procedure.**

*XDC / AMD Vivado.* Install Vivado ML Edition (free tier covers Artix-7 parts;
account registration required; installed size for a single 7-series device family
is large — commonly cited around 50 GB, **unverified**). Create a project for the
board's part, add the exported `out.v` (or `out.vhd`) and `out.xdc` to
`constrs_1`, run synthesis, implementation, and `write_bitstream`. Accept only
when: zero errors; **zero `UCIO-1`/`NSTD-1` DRC violations** (this is precisely
what the emitted `IOSTANDARD` exists to satisfy — if they appear, the emitter is
wrong); and `report_io` shows every port at the `PACKAGE_PIN` the bindings named.
Watch for `set_property` *warnings* about ports that matched nothing — that is
the VHDL case-sensitivity failure from Step 7, and it is silent otherwise. If
hardware is present, program the board and confirm the design behaves.

*QSF / Intel Quartus Prime Lite.* Quartus Prime Lite is free and needs no licence
file for MAX 10 (registration to download; install size commonly cited around
20–30 GB, **unverified**). Create a project, `source out.qsf` from the project
`.qsf` (or paste its contents in), add the exported HDL, run Analysis & Synthesis
then the Fitter. Accept only when the Fitter's "Input/Output Pins" report places
every port at the expected `PIN_`, **and** the message log contains no "ignored"
assignment warnings naming a port. Quartus *ignores* an assignment whose `-to`
name does not match rather than failing — so reading the ignored-assignment list
is a mandatory, not optional, step. This is the check that catches wrong bus-bit
quoting.

*LPF.* Two consumers, two levels of evidence. `nextpnr-ecp5` is covered
automatically in CI (above) and needs no manual step. Lattice Diamond is manual
and only required if a Diamond-targeted board is added; note that Diamond's free
licence covers some ECP5 devices and not others (**exact device coverage
unverified** — check before promising Diamond support for a specific part).

**Evidence package** (per format, per release): tool name and exact version;
board and device ordering code; JLS version; the exact exported files (attach or
link the goldens); the tool log excerpt or `report_io`/Fitter pin table;
pass/fail per checklist section; anomalies. **Cost:** $0 in fees; the real cost is
disk, download time, and a Windows/Linux machine that can host the tools.
**Elapsed time:** roughly one maintainer-day per vendor tool for the first
install and project setup; 30–60 minutes per format per release once the projects
are kept on disk. **Validity period:** none — there is nothing that expires.
**What invalidates it:** any change to the emitted bytes for that format; a new
board using that format; a new major version of the vendor tool; a change to the
part. Rule to write into the checklist: *re-run the checklist for any format
whose golden changed since the last recorded run, and re-run all formats at least
once per minor release.*

**What a credible self-assertion consists of** — and the docs should contain
exactly this, no more: (1) the tool name and version, and the board, the file was
accepted on; (2) the reproducible command/GUI steps; (3) the committed golden
files; (4) an explicit non-claim ("pin location and I/O standard only; no timing
closure, no SDC coverage, no claim about designs JLS did not export"); (5) a
date and an issue-comment link. A board that has never been through the checklist
must be marked in `docs/hdl-support-research.md` §7.5 as **emitted but not yet
accepted by the vendor tool** — the research-doc convention of marking verified
vs unverified claims applies directly.

### Effort, risk, and failure modes

**Sizing** (maintainer-days, reasoning per line):

| Work | Days | Why |
|---|---|---|
| `PinBinder` extraction + `ConstraintEmitter` + `ConstraintEmitters` + `JLSStart` rewiring, PCF bytes unchanged | 1 | Pure move of ~55 lines with two existing tests as the guard |
| `Board`/`Pin`/`IoStandard`/`Pull` records, board default, `part`/`family`, table + `BoardPinOrderTest` updated | 1 | Small, but touches every construction site and NullAway |
| XDC emitter + Basys 3 entry + goldens | 1 | Emitter is ~60 lines; the pin transcription is the slow part |
| QSF emitter + DE10-Lite entry + goldens | 1 | Same, plus the globals decision |
| LPF emitter + ULX3S entry + goldens + `NextpnrConstraintTest` | 1.5 | Extra half-day for the yosys→nextpnr subprocess test and its skip behaviour |
| `BoardTableTest`, property test, `CliVendorBoardExportTest`, CI apt line, help text, README/§7.5/CHANGELOG | 1 | Six files, all mechanical |
| First manual vendor acceptance run (both tools, from zero) | 1.5–3 | Dominated by downloading and installing Vivado and Quartus |

**Total 8–10 maintainer-days**, of which ~3 are not code. Emit-and-golden only,
with vendor acceptance explicitly deferred and the docs saying so: **6–7 days**.

**Top three ways this goes wrong.**

1. **A transcription error in the pin table.** The file is syntactically perfect
   and physically wrong; a student's output drives a pin the board has tied
   elsewhere. This is the only failure mode in this item with a hardware
   consequence, and no test in the plan catches it — goldens pin what the code
   emits, not whether `V17` is really `SW0`. Mitigations: transcribe only from
   the vendor master constraint file, cite document + revision in the constant's
   javadoc, and require one on-hardware acceptance run before a board is
   documented as supported (mark others "not hardware-tested" in §7.5).
2. **Format details only the real vendor tool rejects.** Bus-bit naming
   (`led[0]` vs `led_0`), QSF quoting, a missing `IOSTANDARD` failing DRC at
   bitstream time, `LOCATE COMP` vs `LOCATE PORT`, and above all Quartus/Vivado
   *silently ignoring* an unmatched assignment. CI can catch none of these for
   XDC and QSF. Mitigation: the acceptance checklist gates the documentation
   claim, and until the first accepted run the docs say "emitted, not yet
   accepted".
3. **Scope creep into a constraint language.** The next requests after pins are
   pull-ups, drive strength, differential pairs, bank voltages, input/output
   delays, false paths, and per-binding overrides — each individually reasonable,
   collectively a board-description format and an SDC writer, which #213 H2 and
   the project's delegation rule both refuse. Mitigation: a *fixed* metadata set
   on `Pin`, no per-binding attributes, and one explicit line in the docs: if you
   need more, hand-edit the emitted file or write your own SDC; JLS emits pins,
   the vendor tool does constraints.

Secondary: the board table becomes a standing maintenance liability (board
revisions, part-suffix changes, boards going out of production), and three
manual per-release checklists is a real recurring tax on a single maintainer.

**Do NOT do this if:**

- **No user has asked for a specific board.** This is demand-gated exactly like
  #212. Three formats and three boards is 8–10 days plus a permanent per-release
  checklist for a feature whose PCF sibling may not yet have a single user. The
  correct trigger is a course or a user naming a board they own.
- **The maintainer will not install Vivado/Quartus.** Then ship LPF only, where
  `nextpnr-ecp5` gives real machine-checked evidence, and leave XDC/QSF on the
  roadmap. An emitter that claims vendor compatibility nobody has ever observed
  is worse than no emitter: it converts a missing feature into a false claim.
- **#215 (scripted bitstream handoff) is not going to happen for that vendor.**
  A constraint file with no documented path from `.jls` to a programmed board is
  half a feature; the iCE40 path works because `docs/hdl-support-research.md`
  §7.5 already documents the full `yosys` → `nextpnr-ice40` recipe.

**Recommended slice order** (each independently shippable): (0) arm
`nextpnr-ice40` against the *existing* PCF golden — cheapest confidence, no new
surface; (1) the `PinBinder`/`ConstraintEmitter` refactor with PCF bytes
unchanged; (2) LPF + ULX3S, because CI can validate it; (3) XDC + Basys 3, only
once a Vivado rig exists; (4) QSF + DE10-Lite last.

### Sources

**Repo paths (all read and verified):**

- `src/jls/hdl/board/PcfEmitter.java` — the only constraint emitter today; static
  `emit`, format guard at line 61, binding walk lines 66–119, PCF rendering lines
  102–105, header lines 121–131. No emitter interface exists.
- `src/jls/hdl/board/Board.java` — `record Board(name, fpga, Format, Map<String,
  String> pins)`; `Format` enum has one constant (`PCF("pcf")`, lines 35–63) with
  a javadoc note that XDC/QSF are #213 follow-ups; `NATURAL_PIN_ORDER` natural
  sort at lines 72–146.
- `src/jls/hdl/board/Boards.java` — the built-in table; `ICESTICK` constant lines
  34–78 with provenance prose in its javadoc, `ALL` line 81, `byName` line 99,
  `names()` line 116.
- `src/jls/hdl/board/PinBindings.java` — the two-token `-pins` grammar, `#`
  comments, all-errors-at-once parse.
- `src/jls/hdl/board/package-info.java` — `@NullMarked`; states the headless and
  all-or-nothing invariants.
- `src/jls/hdl/HdlExporter.java` lines 255–290 — the port walk: input pins, then
  `Clock` elements as an input port `clk` whose comment records cycle time in
  *simulation units*, then output pins.
- `src/jls/hdl/HdlModel.java` lines 27–45 — `Direction`, `Port(name, direction,
  bits, comment)`.
- `src/jls/hdl/HdlEmitter.java`, `src/jls/hdl/HdlExtensionPoints.java`,
  `docs/extension-points.md:32` — the *language* emitter seam; not the constraint
  seam.
- `src/jls/JLSStart.java` — board wiring: flag specs lines 782–786 (the `.pcf`
  literal at 783), pairing rules 908–916, board lookup 392, bindings read
  394–412, emit 421–428, warning print 433–435, constraint filename derivation
  465–474.
- `test/jls/hdl/board/{PcfGoldenTest,UnbindablePortsTest,BoardPinOrderTest,CliBoardExportTest,BoardFixtures}.java`
  — the existing test regime and the `blinky` fixture.
- `test/resources/hdl/board/blinky_icestick.pcf` — the shipped golden, with the
  `@VERSION@` token.
- `test/jls/hdl/GhdlCompileTest.java`, `test/jls/hdl/ToolLocator.java` — the
  skip-when-absent external-tool pattern to copy.
- `.github/workflows/ci.yml` lines 55–62 (Linux best-effort
  `iverilog ghdl yosys xvfb`), 147–219 (Windows `oss-cad-suite` pin).
- `docs/hdl-support-research.md` §7.5 (lines ~505–550) — the shipped design, the
  board table, the all-or-nothing rule, and the explicit deferral of XDC/QSF/LPF.
- `docs/standards-landscape.md:266–267` (#81 HAVE, #82 ROADMAP) and lines
  736–737 (each format is "a small printer over the existing port walk").
- `docs/wayland-desktop-checklist.md` — the per-release manual-verification and
  results-template pattern the acceptance checklist should clone.
- `docs/batch-interface.md`, `docs/file-format.md` — the two documented stability
  contracts; neither is touched by this item.
- `pom.xml` lines 536, 560–564 — the `@jls.testedby` custom javadoc tag.
- `ARCHITECTURE.md` "Recorded decisions" — plugin mechanism removed (5.0.0, #80);
  the reason not to make constraint emitters an extension point.
- `CHANGELOG.md` lines 163–174 — the shipped #213 entry, which already names
  XDC/QSF as follow-ups.

**External documents (cited by name; revisions NOT verified in this
environment — verify before quoting in project docs):** AMD *UG903, Vivado Design
Suite User Guide: Using Constraints*; Intel *Quartus Prime Settings File
Reference Manual*; Lattice Diamond preference/LPF reference; Digilent Basys 3
master XDC; Terasic DE10-Lite user manual and system-builder QSF; ULX3S reference
LPF; `nextpnr` documentation for `--lpf`/`--pcf`.

**Explicitly unverified in this document:** all illustrative pin letters/numbers
in the XDC/QSF/LPF examples; Vivado and Quartus install sizes; whether Ubuntu's
repositories package `nextpnr-ice40`/`nextpnr-ecp5` under those names; whether
the pinned `oss-cad-suite` bundle ships `nextpnr-*`; whether Lattice Diamond
flattens bus bits to `sw_0` where nextpnr-ecp5 expects `sw[0]`; whether Quartus
prefers quoted or unquoted `-to` names for bus bits; Diamond's free-licence
device coverage for ECP5 parts.
