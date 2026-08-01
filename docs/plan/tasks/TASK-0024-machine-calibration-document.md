# TASK-0024 - Write the machine-calibration document

**Status:** proposed | **Cost:** 1 wk | **Blocked by:** TASK-0022, TASK-0023,
TASK-0025

## Deliverable

**Correction to the registry scope, recorded rather than hidden:
`docs/machine-calibration.md` already exists at HEAD - 1,124 lines, carrying the
element census, the engine constants with their clocking regime, the boot-cost
model, the guest-side facts and the re-measurement procedure.** The task is not
to write it. It is to **discharge its §6 and make its §7 executable**, which is
what `docs/virtual-hardware-parity.md:493-495` states L0's job to be.

1. **§6 replaced by measurements.** Each of §6.1-§6.7 currently states a spread,
   names the cheapest experiment, and stops. After TASK-0022 and TASK-0023 each
   becomes a measured value with its workload, date, hardware and method, and
   §6 is reduced to whatever genuinely remains open. §6.11's two-column map to
   L0(a)-L0(i) is updated in the same edit so the two documents cannot drift.
2. **§7.1 names the committed fixture.** Today it says: "Historically this was
   `riscv/build/k2000.jls` ... **That file was never tracked** ... If it was
   regenerated and committed as a fixture before the deletion, use that fixture
   and record its path here." TASK-0025 commits it; this task writes the path,
   the census and the generation method in.
3. **§1.2's re-homing table reconciled against what actually happened.** It is
   a table of preconditions. After TASK-0025 it must read as a record, and any
   row that was decided differently (notably `riscv/gui/cpu.jls`, one of only
   four tracked `.jls` files in the tree) must say what was decided.
4. **Four inherited factual errors corrected at their sources.** Folded here by
   the registry rather than given their own ids:
   - **The element-type count.** The corpus states `ElementRegistry.ALL` is "the
     complete 33-type list". At HEAD it is **35**
     (`src/jls/elem/ElementRegistry.java:38-77`); `RegisterFile` and
     `FieldExtend` were added by commit `970db41`. Every derived claim that
     counts element types is off by two.
   - **The save-format statement-kind count.** `docs/file-format.md` §3/§9
     enumerate the item kinds (`int`, `long`, `Int`, `String`, `ref`, `pair`,
     `probe`); any count quoted elsewhere is re-checked against that list.
   - **Subprocess versus file handoff.** The corpus asserts "there is not one
     `ProcessBuilder` in all of `src/`". Verify at HEAD before repeating it;
     the board and HDL on-ramps have moved.
   - **Emit versus cannot-emit for `$dff`.** The importer's javadoc
     (`src/jls/hdl/imp/NetlistImporter.java:42-47`) says `$dff` is a cell the
     `CellValidator` **accepts** but the mapper does not yet **realize**, and
     that it is reported as an import problem. That is a different and weaker
     claim than "cannot emit a flip-flop"; say the accurate one.
5. **`ARCHITECTURE.md:354-358` restated.** The #221 revisit trigger names "a
   concrete CPU-scale design on the `riscv/` trajectory (#200/#201/#202)" - a
   directory TASK-0025 deletes. Restate it quantitatively against the measured
   constants; `keystone-c` proposes "below 10 kcycles/s on the #202 golden's
   CPU". Do not weaken the binding equivalence criterion in the same edit.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-009 | Criterion 1 (measured, not estimated, each with its method) and criterion 6 (every published figure states its clocking regime; every ns/node figure states node count and pass count) are properties of this document, not of the harnesses |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0022 | §6.1 and §6.2 cannot be replaced with values that do not exist |
| TASK-0023 | §6.4 and §6.6 likewise |
| TASK-0025 | §7.1's fixture path, and §1.2's table becoming a record rather than a precondition list, both require the deletion to have happened |

## Acceptance test

`test/jls/MachineCalibrationDocTest.java`, new - a documentation-drift test in
the established family (`test/jls/CliFlagTableTest.java`,
`test/jls/HotkeysHelpAccuracyTest.java`, `test/jls/TutorialContentTest.java`):

- `everyUnmeasuredSectionIsDischargedOrExplicitlyStillOpen()` - parses the §6
  headings and asserts each subsection contains either a measured value line or
  a line beginning "Still open:". Fails on a subsection that merely describes
  an experiment. This is the assertion that makes "the document was updated"
  checkable.
- `theElementTypeCountMatchesTheRegistry()` - asserts every integer the document
  states as a count of element types equals `ElementRegistry.all().size()`.
  Fails at HEAD against any surviving "33".
- `sectionSevenNamesAnExistingFixture()` - extracts the fixture path from §7.1
  and asserts `Files.exists` on it and that it is tracked (not matched by the
  `*.jls` ignore, i.e. under `test/fixtures/`).
- `theL0MapIsBidirectional()` - asserts every row of §6.11 names a subsection
  that exists and that every §6 subsection appears in §6.11.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on - the #221 revisit trigger this task restates names #202's CPU as its subject, and `RiscvCpuGoldenTest` is the golden the restatement must keep binding |

No issue covers the calibration record itself. The registry records that the
calibration fixture and its document have no issue, and that CAP-00 - the
capstone this sits under - has none either.

## Notes

- **This is a documentation task inside a planning corpus, and the distinction
  matters.** `docs/machine-calibration.md` is an *evidence record* at HEAD, not
  a plan; its own preamble says it "authorizes nothing". This task changes its
  contents, never its status.
- **Do not average a disagreement away.** The document's stated discipline is
  that where two sources disagree, both are printed and the disagreement is
  named. §6.2 and §6.6 exist because that rule was followed; discharging them
  means replacing a disagreement with a measurement, not with a mean.
- **The five normative quoting rules are the document's only normative content**
  (§2.5, §2.6, §4.5, §4.6, §7.3 step 6). Each exists because it was already
  broken once. They survive this edit unchanged; the acceptance test's fourth
  method exists to keep §6.11 from rotting around them.
- **`RiscvCpuGoldenTest`'s javadoc rot is TASK-0025's, not this task's.** The
  two `{@code}` citations at `test/jls/RiscvCpuGoldenTest.java:25` and `:38`
  are not `{@link}` spans, so the `-Werror` doclint gate will not catch them
  when their targets are deleted. Recorded here because §1.2 records it;
  fixed there.
- **Budget shape.** One maintainer-week is prose plus one drift test. It is
  short because the hard part - the numbers - is TASK-0022 and TASK-0023, and
  because 1,124 lines of structure already exist to write into.

## Evidence

- `docs/machine-calibration.md` at HEAD: 1,124 lines; headings verified by
  `grep -n '^#'`; §1.2 at `:71-104`; §6 at `:846-995`; §6.11's map at
  `:975-995`; §7.1 at `:1001-1015`; §7.3 step 6 at `:1044-1047`.
- `docs/virtual-hardware-parity.md:493-495` - "L0's job is to replace its §6
  (nine unmeasured, load-bearing quantities) with measured values and their
  harnesses."
- `src/jls/elem/ElementRegistry.java:38-77` - 35 `new ElementType(` entries,
  counted at HEAD.
- `src/jls/hdl/imp/NetlistImporter.java:42-47` - the accepted-but-not-realized
  wording for `$dff`, `$dlatch`, `$add`, `$tribuf`, the reductions, `$bmux`
  and hierarchy instances.
- `docs/file-format.md:78-148` (§2, §3) and `:422-496` (§9) - the item kinds
  and the evolution policy the statement-kind count must agree with.
- `ARCHITECTURE.md:354-362` - the #221 revisit trigger and the binding
  equivalence criterion, read at HEAD.
- `test/jls/RiscvCpuGoldenTest.java:25,38` - the two `{@code}` citations.
- Drift-test precedent: `test/jls/CliFlagTableTest.java`,
  `test/jls/HotkeysHelpAccuracyTest.java`, `test/jls/TutorialContentTest.java`.
