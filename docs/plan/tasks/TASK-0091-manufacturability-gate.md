# TASK-0091 - The manufacturability gate

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0085, TASK-0086,
TASK-0088

## Deliverable

`jls.pcb.ManufacturabilityGate` - one headless analysis that answers "can this
design be built as a board?" with a named rule per finding, never a bare
boolean.

Precisely what changes:

- `jls/pcb/ManufacturabilityGate.java`: `check(PhysicalNetlist, PackPlan,
  PartLibrary, LoadingReport)` returning `GateReport` - the same four values
  TASK-0085 through TASK-0088 already produce, and no new inputs.
- `jls/pcb/GateRule.java`: an enum, one constant per rule, each carrying a
  stable rule id, a severity (`ERROR` / `WARNING`), and a one-line rationale.
  The initial rule set, each of which is decidable from data JLS will have:
  - `M1_NO_FOOTPRINT` - an element in the packing result with no footprint
    binding. ERROR (the KiCad updater refuses it outright).
  - `M2_UNPACKED_ELEMENT` - a logic element with no package/section assignment.
    ERROR.
  - `M3_PIN_COUNT_MISMATCH` - the terminals a packed element needs exceed the
    signal pins the bound footprint declares. ERROR.
  - `M4_NO_POWER_PIN_MODEL` - the design uses parts whose package data declares
    power/ground pins that no JLS net drives. WARNING, with the honesty
    sentence: JLS has no power concept, so this is unresolvable inside JLS and
    is the user's job in the PCB tool.
  - `M5_UNUSED_SECTION` - a package with sections left unassigned; carries the
    tie-off advice. WARNING.
  - `M6_FANOUT_EXCEEDED` - the DC loading finding produced by TASK-0088,
    re-reported here so one report answers the question. ERROR.
  - `M7_BIDIRECTIONAL_UNMODELED` - a `TriState` on a net that also has a
    non-tri-state driver, which no netlist can render honestly until
    `HdlModel.Direction` gains a bidirectional case. ERROR.
  - `M8_WORD_LEVEL_NO_PACKAGE` - a word-level element (`Adder`, `Mux`,
    `Memory`, `RegisterFile`) with no cascade decomposition and no package.
    ERROR, naming TASK-0087 as the mechanism that resolves it.
- `jls/pcb/GateReport.java`: findings in canonical stable-id order, a text
  renderer, and a machine-readable renderer (one line per finding:
  `rule-id<TAB>severity<TAB>element-stable-id<TAB>message`) so a grader can
  consume it.
- CLI: `-manufacturability <file>` in the `FLAGS` table
  (`src/jls/JLSStart.java:759-789`) with its parse case and usage text; exit
  code 0 when no ERROR finding exists, 2 when one does, matching the existing
  batch exit-code contract.

Done means: a reviewer can run the gate on a fixture and, for each finding,
name the rule id, the element, and what to change. No finding is emitted
without a rule id.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-042 | The gate is FEAT-042's acceptance criterion - the registry folded it into that feature deliberately rather than minting a feature for a 1.5-week check. |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0085 | M1 and M3 read the footprint binding and the declared pin count; only TASK-0085 defines that schema. |
| TASK-0086 | M2, M5 and M8 read the `PackPlan`; only TASK-0086 produces it. |
| TASK-0088 | M6 re-reports `LoadingReport`'s fan-out and DC finding; only TASK-0088 computes it. Re-implementing it here would give JLS two loading checks that can disagree. |

## Acceptance test

`test/jls/pcb/ManufacturabilityGateTest` (new class), one test per rule plus
two structural tests:

- `everyGateRuleHasAFixtureThatTripsIt()` - reflects over `GateRule.values()`
  and asserts each constant appears in at least one finding produced by the
  committed fixture set. This is the totality assertion: adding a rule without
  a fixture fails the build.
- `aCleanDesignProducesNoErrorFindingsAndExitsZero()` - the four-NAND DIP-14
  fixture from TASK-0089 yields zero `ERROR` findings and the CLI exits 0.
- `missingFootprintIsReportedAsM1AndNotAsAnException()` - asserts the gate
  *reports* rather than throws, which is the difference between the gate and
  the emitter (the emitter refuses; the gate explains).
- `findingsAreOrderedByStableIdAndAreByteStableAcrossRuns()` - two runs over
  the same fixture produce identical report bytes.
- `theMachineReadableRendererIsTabSeparatedAndParsesBack()` - round-trips the
  report through its own reader.

## Related GitHub issues

**no issue.** No open issue in `anadon/jls` covers PCB manufacturability;
`search_issues` for `pcb OR footprint OR netlist` returns only HDL-interop
issues. Adjacent:

| # | title | relationship |
|---:|---|---|
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff | informs - its per-board "supported only when both halves exist" discipline is the same all-or-nothing posture this gate encodes for boards. |

## Notes

- **The gate is the honest boundary, and it must say so.** Everything a
  schematic needs, JLS has or derives; everything a *board* needs - footprints,
  package pin numbers, gate-to-package packing, power and ground - is new
  primary data. M4 and M8 exist so a student sees where the tool stops instead
  of discovering it in the fab house's DRC report.
- **Do not let this grow into DRC/LVS.** Layout rules (clearance, drill,
  annular ring, layer stack) derive from a board layout JLS has no reason to
  acquire; they belong to the PCB tool. The gate's scope is exactly the set of
  facts JLS holds and the PCB tool does not.
- **Severity is not cosmetic.** `M4_NO_POWER_PIN_MODEL` must be a WARNING, not
  an ERROR: making it an error would make every real design un-gateable, which
  turns the gate into a thing people pass by disabling.
- **Exhaustive switch trap.** Rendering severity and rule text should switch
  over `GateRule` with no `default` arm, so a new rule stops the build at the
  renderer until it has a message.
- **The flag-table traps of TASK-0089 apply unchanged**
  (`test/jls/CliFlagTableTest.java:82-89,102`).
- **Cost note.** 1.5 weeks buys the eight rules above over data other tasks
  produce. It does not buy new data acquisition; every rule that would need new
  data is stated as a rule that reports the gap rather than closes it.

## Evidence

- The footprint gate is mechanical, not advisory: KiCad's netlist updater
  refuses a component with an empty footprint field, places nothing, and
  therefore applies none of that component's nets (`fmt-kicad-geda.md` §3,
  read from KiCad `10.0` `board_netlist_updater.cpp`).
- The projection/new-primary-data split, item by item: `fmt-kicad-geda.md` §4
  (net partition, names, terminals, geometry, hierarchy, ordering all present;
  footprint, package pin numbers, packing, power pins all absent).
- JLS models no power and no energy, stated against HEAD:
  `docs/simulation-semantics.md:41-67` (two states plus HiZ, HiZ read as zero,
  no X state); delay is a dimensionless integer
  (`src/jls/elem/Adder.java:259-262`, `propDelay = bits * defaultPropDelay`).
- `HdlModel.Direction` is `{INPUT, OUTPUT}` at HEAD, which is why M7 exists:
  `src/jls/hdl/HdlModel.java:28-33`.
- Batch report and exit-code precedent: the all-or-nothing aggregation in
  `src/jls/hdl/board/PcfEmitter.java:14-30` and
  `src/jls/hdl/board/PinBindings.java` (every problem reported in one pass).
