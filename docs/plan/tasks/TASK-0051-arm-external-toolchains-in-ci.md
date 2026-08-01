# TASK-0051 - Arm the external toolchains in CI

**Status:** proposed | **Cost:** 1 wk | **Blocked by:** none

## Deliverable

The external-tool test legs stop being able to pass by not running, and the
tools the board and parity work need are actually installed.

1. **The best-effort install becomes required on the required lane.**
   `.github/workflows/ci.yml:73` is
   `sudo apt-get install -y ... iverilog ghdl yosys xvfb || echo "some optional tools unavailable; their tests will skip"`.
   The `|| echo` means an apt hiccup silently disarms four suites and the build
   stays green. On the required Linux LTS leg (`build`, `ci.yml:28-31`,
   `matrix.java == 25`) the install must fail the job; the advisory
   feature-release leg may keep the soft path.
2. **A test that fails instead of skipping when arming is demanded.** Four
   suites self-skip today: `IverilogCompileTest.java:34`,
   `GhdlCompileTest.java:35`, `scan/YosysGroundTruthTest.java:44`,
   `imp/ImportPipelineTest.java:89`, all via
   `Assumptions.assumeTrue(ToolLocator.findOnPath(...) != null)`. Add
   `JLS_REQUIRE_HDL_TOOLS=1` to the required lanes and a test that asserts every
   named tool resolves when that variable is set - so "green" proves the legs
   ran rather than proving they were skipped.
3. **The missing tools get installed.** `nextpnr-ice40`, `icestorm`
   (`icepack`), and `verilator` appear nowhere in the repository - no workflow
   installs them and no test references verilator at all. Add them to the Linux
   install step, to the macOS `brew install` line (`ci.yml:288`), and note that
   the pinned Windows `oss-cad-suite` bundle (`ci.yml:158-232`) already carries
   nextpnr and icepack.
4. **A synthesis and place-and-route smoke over the shipped golden.** The
   iCEstick handoff is exercised today only against *stubbed* tools
   (`ci.yml:47-57` running `scripts/icestick-handoff-selftest.sh`). With
   nextpnr installed, run the real `yosys -> nextpnr-ice40 -> icepack` chain
   over the committed `.pcf` golden (`test/resources/hdl/board/blinky_icestick.pcf`)
   and assert a non-empty bitstream. This is #264's "CI synth+P&R smoke gated
   on toolchain presence if runners allow".
5. **Versions recorded, because drift is the standing threat.** Each armed job
   echoes `yosys -V`, `iverilog -V`, `ghdl --version`, `nextpnr-ice40 --version`
   into the job summary. #61 §10 and #63 §10 both name version drift; nothing
   records the observed versions today.
6. **The HDL export menu item.** Export is command-line only at HEAD
   (`src/jls/JLSStart.java:363-460`); there is no File->Export->HDL action. Add
   it, with the language chosen by the file chooser's suffix, matching the two
   suffix decisions at `JLSStart.java:383-385` and `:1088-1090`. This is
   recorded here so no other author re-mints it.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-023 | "the external toolchains run against JLS's own output in CI" is the feature; an assumeTrue-skipped suite is not a differential oracle |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| - | none. Every suite this arms exists and passes when its tool is present |

TASK-0015 (explicit workflow timeouts) and TASK-0016 (fast/long-run lanes) touch
the same file and should be sequenced together to avoid three rounds of
workflow conflicts, but neither creates data this task reads.

## Acceptance test

`test/jls/hdl/ToolchainArmedTest.java`, new:

- `everyRequiredToolResolvesWhenArmingIsDemanded()` - when
  `JLS_REQUIRE_HDL_TOOLS=1`, assert `ToolLocator.findOnPath` returns non-null
  for `yosys`, `iverilog`, `ghdl`, `nextpnr-ice40`, `icepack`, `verilator`;
  when unset, the test itself skips. The failure message names the missing
  tools in one pass, matching `PcfEmitter`'s aggregation style.
- `theArmingVariableIsSetOnTheRequiredLanes()` - reads the workflow file and
  asserts the required Linux, Windows and macOS build jobs set it, so removing
  it from CI fails a test rather than quietly disarming the suite.

`test/jls/hdl/board/IcestickSynthesisSmokeTest.java`, new, gated on
`nextpnr-ice40`: `theShippedPcfPlacesAndRoutes()` - runs the real chain over the
golden and asserts a non-empty `.bin`.

`test/jls/edit/HdlExportMenuTest.java`, new, display-tagged (the surefire
execution `ci.yml` activates under xvfb, issue #162):
`theFileMenuOffersHdlExportAndItsSuffixesMatchTheCli()`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | depends on - this task installs the toolchain #264's DoD row "place-and-route ... validated where the toolchain is present" needs |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | overlaps - the Windows `oss-cad-suite` arming already landed; this task extends the same intent to the Linux and macOS lanes |
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | overlaps - promoting lanes to required is TASK-0017; arming the tools inside them is this task |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | informs - #61 §10 names version drift as the threat this task's version recording answers |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs - same, for GHDL/Icarus |
| 162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | overlaps - the export menu item's test runs in #162's display-tagged execution |

## Notes

- **The trap is that this task can appear to succeed while doing nothing.**
  Removing `|| echo` without adding item 2 leaves a build where apt succeeds,
  a *different* tool is missing, and the suites skip anyway. The arming
  assertion is the deliverable; the workflow edit is the enabler.
- **Skipping must stay possible for contributors.** A developer without yosys
  must still get a green `mvn verify`. That is why arming is an environment
  variable set by CI rather than a hard requirement in the test.
- **`verilator` has no test to arm yet.** Installing it here is deliberate
  pre-positioning for FEAT-023's differential oracle; say so, and do not claim
  Verilator parity on the strength of an apt line.
- **macOS brew names drift.** `ci.yml:283` already notes the tap/name
  uncertainty for `icarus-verilog`; `nextpnr` is not in core homebrew under an
  obvious name. If a macOS install cannot be made reliable, arm Linux and
  Windows and record macOS as advisory - with the reason in the workflow
  comment, not in a commit message.
- **Do not fold the long-run policy in here.** TASK-0016 owns the fast/long-run
  split and the fixture policy; this task only arms tools in the lanes that
  exist.

## Evidence

- `.github/workflows/ci.yml:28-31` (the `build` job), `:47-57` (the stubbed
  handoff self-test), `:65-73` (the best-effort install), `:143-146` and
  `:158-232` (the Windows `oss-cad-suite` pin and extraction), `:259-292` (the
  macOS brew line).
- `test/jls/hdl/ToolLocator.java:14-46` - the shared locator and its account of
  the five duplicated `findOnPath` helpers it replaced; `:57-73` `findOnPath`.
- `test/jls/hdl/IverilogCompileTest.java:34`, `GhdlCompileTest.java:35`,
  `scan/YosysGroundTruthTest.java:44`, `imp/ImportPipelineTest.java:89` - the
  four `assumeTrue` skips.
- `scripts/icestick-handoff.sh`, `scripts/icestick-handoff-selftest.sh`,
  `docs/icestick-bitstream-handoff.md:19-31` - the documented tool list, none of
  which CI installs.
- Repository-wide search: no file under `src/`, `test/` or `.github/` mentions
  `verilator`.
