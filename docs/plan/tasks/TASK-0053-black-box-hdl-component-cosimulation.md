# TASK-0053 - Black-box HDL component and its co-simulation contract

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

**The port scanner half already shipped.** `jls.hdl.scan` exists at HEAD -
`VerilogHeaderScanner` (1,489 lines, `scan(String)` at `:77`),
`VhdlEntityScanner` (891 lines, `scan(String)` at `:69`), `ScannedModule`
(`ports()` at `:49`, `parameters()` at `:59`), `ScannedPort` with a three-value
direction including `INOUT` (`:13-20`), `HdlScanException`, and a ground-truth
suite validating the scanner against real Yosys `write_json`
(`test/jls/hdl/scan/YosysGroundTruthTest.java`). What does not exist is the
element, the subprocess, or the contract. The package javadoc says the scanners
produce port lists "for the HDL component dialog"; there is no HDL component
and no dialog.

1. **`src/jls/elem/HdlComponent.java`,** a `LogicElement`. Saved attributes:
   the HDL file path **relative to the circuit file** (#63 addendum's
   file-reference policy), the module name, the content hash taken at scan
   time, and the scanned port list (name, direction, width) so the element's
   shape survives a load with the file absent.
2. **Registration and export policy, or the build fails.** A row in
   `ElementRegistry.ALL` (`src/jls/elem/ElementRegistry.java:38-77`) and
   membership in exactly one `HdlExporter` bucket. The right bucket is
   `REJECTED` (`src/jls/hdl/HdlExporter.java:459-478`) with the reason stated
   the way the existing four entries state theirs - a black box has no portable
   HDL rendering because its body is the external file. Omitting it fails
   `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry`
   (`test/jls/hdl/HdlPolicyTest.java:392-407`).
3. **Put construction from `ScannedModule.ports()`.** `IN` and `OUT` become
   `Input`/`Output` puts; `INOUT` is refused **by name** with the reason and a
   pointer to the work that fixes it, because `HdlModel.Direction` has no
   bidirectional case at HEAD (`src/jls/hdl/HdlModel.java:28-33`) and TASK-0049
   is what adds it. A scanner that reports more than the element can carry is
   the exact failure this refusal makes visible.
4. **The co-simulation contract, written down** in `docs/hdl-cosimulation.md`:
   forward-only time, one value exchange per event time at which any of the
   component's inputs changed, a bounded response deadline (generous default,
   ~5 s, configurable), the x/z coercion rule (JLS is two-state -
   `docs/simulation-semantics.md` §1), and the explicit statement that JLS never
   re-enters the subprocess for a time already advanced. The contract is a
   document because it is the thing a second implementation must match.
5. **`src/jls/hdl/cosim/`:** a harness generator producing one testbench per
   component, a process lifecycle (spawn, kill, reap, restart) with a bounded
   stderr tail retained for the failure pane, and a value codec. All of it off
   the event-dispatch thread.
6. **The five visible states** from #63's addendum, each rendered on the canvas
   with a tooltip giving cause and remedy: normal, file missing, file changed
   since scan (hash mismatch), simulator unavailable, crashed.
7. **A re-scan that would orphan wires reports first.** "port `carry_out`
   removed - 2 wires will be deleted", before anything is modified. Silent wire
   deletion is a test failure, not a UX regret.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-024 | the element and the contract are the feature; the scanner that shipped is its input half |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| - | none. The scanners, `ToolLocator`, and the tri-state substrate all exist at HEAD |

TASK-0049 is not a blocker: the element ships refusing `INOUT` modules by name.
It is, however, the task that removes the refusal, and a bus-bearing component
is the common real case - sequence accordingly.

## Acceptance test

`test/jls/elem/HdlComponentLifecycleTest.java`, new - one test per failure
state, because #63 P4 requires every one of them to be reachable:

- `aMissingFileRendersTheMissingStateAndDoesNotThrow()`
- `aChangedFileIsDetectedByContentHash()`
- `aRescanThatOrphansWiresReportsBeforeModifyingAnything()` - asserts the wire
  count is unchanged after the report and before confirmation.
- `aHungSubprocessIsTreatedAsCrashedWithinTheDeadline()` - a harness that
  sleeps; assert simulation stops with a message naming the component and the
  stderr tail, and that the UI thread was never blocked.
- `closingTheCircuitReapsEverySubprocess()` - #63 P6, no zombies.

`test/jls/hdl/cosim/BlackBoxParityTest.java`, new, gated on
`ToolLocator.findOnPath("iverilog")`:
`aBlackBoxAdderMatchesTheNativeAdderOnTheBatchGoldens()` - #63 P1, the
assertion that proves the protocol carries semantics and not just bytes; and
`aTrivialWireModuleRoundTripsBeforeAnythingElse()` - #63 §10's advice to test
the protocol against a trivial module first, so protocol bugs cannot
masquerade as HDL semantics differences.

`test/jls/CircuitLoadTest`, extended:
`aCircuitWithAnHdlComponentLoadsWithNoSimulatorInstalled()` - #63 P5. HDL-tool
absence must never make a circuit *file* unloadable; a student must be able to
open the instructor's mixed lab at home.

`test/jls/hdl/scan/ScannedPortDirectionTest.java`, new:
`anInoutPortIsRefusedByNameWithAReason()`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | closes - the scanner half of §7 shipped; this task is the element, the harness, the lifecycle and the addendum's failure surfaces |
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - Stage 3 of the staged path |
| 162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | overlaps - a new element dialog raises the count #162 enumerates and must arrive with its construction test |

Recorded decision, **closed**, cite as such and not as open: **#49** (simulator
threading). #63 §2 makes it a hard prerequisite - "adding a subprocess handshake
to the current unsynchronized loop would compound existing races" - and it is
not in the open set, so the ordering constraint is satisfied, not outstanding.

## Notes

- **The scanner is not the risk; the handshake is.** 2,380 lines of scanner
  already exist and are validated against Yosys. Budget the two weeks for
  lifecycle, deadline handling and reaping.
- **`Element.setValue` returns silently on an unknown attribute**
  (`src/jls/elem/Element.java:344-351`), and the loader calls it unconditionally
  at five sites (`src/jls/Circuit.java:1067,1078,1089,1105,1116`). A renamed
  path or hash attribute would vanish on load and the component would silently
  re-point at nothing. TASK-0003 is the fix; until then the round-trip test in
  this task is the only guard, and it must assert the *values*, not just that
  the load succeeded.
- **Relative paths, always.** #63's file-reference policy stores the HDL path
  relative to the circuit file so a project folder that moves keeps working.
  An absolute path in a saved file is a defect, and a test should say so.
- **The deadline must not run on the event thread.** A hung external simulator
  hanging JLS is the specific failure #63 §10 predicts.
- **Do not grow the scanner.** #63 §9: any file class the scanner cannot handle
  routes to the external Yosys `write_json` extraction path, which
  `YosysNetlist` already parses. That fallback is an integration, not a parser.
- **Do not restate simulation semantics.** `docs/simulation-semantics.md` is
  normative on two-state values and event ordering; the contract document
  references it.

## Evidence

- `src/jls/hdl/scan/package-info.java:1-21` - the shipped scope, the
  scanner-not-parser stance, and the "for the HDL component dialog" purpose
  that has no consumer.
- `src/jls/hdl/scan/VerilogHeaderScanner.java:77`,
  `VhdlEntityScanner.java:69` - `scan(String)`; `ScannedModule.java:49`, `:59`;
  `ScannedPort.java:13-20` - the three directions.
- `test/jls/hdl/scan/YosysGroundTruthTest.java:43-44` - the scanner's ground
  truth check and its skip-when-absent idiom.
- `src/jls/hdl/HdlExporter.java:459-478` - the `REJECTED` bucket and the shape
  of a stated refusal; `test/jls/hdl/HdlPolicyTest.java:392-407` - the totality
  test the new element must satisfy.
- `src/jls/hdl/HdlModel.java:28-33` - no `INOUT` at HEAD.
- Issue #63 addendum - the five visible states, the file-reference policy, the
  simulation-failure surface and acceptance criteria P4-P6, verbatim.
