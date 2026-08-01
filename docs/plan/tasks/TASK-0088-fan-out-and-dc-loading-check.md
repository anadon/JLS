# TASK-0088 - Fan-out and DC loading check

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0085, TASK-0086, TASK-0087

## Deliverable

A static electrical analysis over the packed plan, driven entirely by part data.
It needs no simulator change and no value-domain work: **this is a package-data
check, not a P1 check**, and it reports a real class of build failure years
before four-state values land.

1. **`jls.pkg.LoadingCheck.run(PhysicalNetlist, PartLibrary) -> LoadingReport`.**
   Per physical net (schematic and synthetic alike), from `Electrical`:
   - sum the sink unit loads of every input pin on the net;
   - take the minimum drive capability, in unit loads, over every driving pin;
   - the net **fails** when the sum exceeds that minimum.
2. **Four verdicts, and the third is the honest one:**
   `OK`; `OVER_FANOUT` with the numbers (*"net BUS0: 14 unit loads on a driver
   rated 10"*); **`NOT_DC_LIMITED`** for families where a DC fan-out check is
   vacuous - the report must say *"not DC-limited"* and must **never** say
   *"PASS"*, because a CMOS family's real limit is capacitive and this check does
   not model it; and `UNDECIDABLE` where the library lacks a rating for a part on
   the net, naming the missing datum.
3. **Two preconditions that fall out of the same pass and are reported with it:**
   - **No undriven input.** Every input pin of every used section must be on a net
     with at least one driver or one pull. A driverless net has no drive capacity
     to compare against, so the check has to notice it anyway - and it is exactly
     the canonical first-year breadboard bug.
   - **No contention.** A net with more than one push-pull driver has no single
     minimum capacity. Report it here with the driver list; open-drain plus at
     least one pull is the permitted case once FEAT-027's driver kinds exist, and
     until then a second push-pull driver is unconditionally reported.
4. **Mixed-family compatibility**, from the same table: a driver whose output
   levels do not meet a sink family's input thresholds is reported per
   driver-sink pair, not per net, because that is the pair a person has to change.
5. **Unused-section inputs are checked, not skipped.** An unused gate in a placed
   package with floating inputs is a real board defect and a real exam question.
6. **`loading.txt` in the `-pack` output directory**, sorted by net name, one
   block per non-`OK` net, with the numbers and the offending pins in physical
   terms (`U3.6`, not a stable id). Exit status per `docs/batch-interface.md` §1:
   `OVER_FANOUT` or contention or an undriven input makes the run exit 1;
   `NOT_DC_LIMITED` and `UNDECIDABLE` do not.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-041 | The electrical half. Packing tells a student which chips to buy; this tells them whether the thing they wired will actually drive. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0085 | The package data schema and footprint binding | Reads `Electrical` - drive capability, input load, the vacuous-DC-check flag and the threshold data. None of it exists at HEAD; `grep -rniE "footprint\|refdes\|pinout" src/` returns zero. |
| TASK-0086 | Packing, refdes, BOM and wiring list | The check runs over pins of packed sections, not over drawn elements. There are no pins to load before packing assigns them. |
| TASK-0087 | Width decomposition and the cascade rule | At any width above the part's slice width the real drivers and sinks are slice pins, and the carry chain is a set of nets the schematic does not contain. Running the check on undecomposed elements would report an 8-bit adder's carry as unloaded and its data nets with the wrong driver count. |

## Acceptance test

`test/jls/pkg/LoadingCheckTest`:
- `aNetOverItsDriversCapacityIsReportedWithBothNumbers()` - assert the message
  contains the summed load and the driver rating, not just a verdict. A fan-out
  failure a student cannot act on is a failure report nobody reads.
- `theWeakestDriverOnTheNetSetsTheLimit()` - two drivers of different families on
  one net; the minimum governs.
- `aVacuousFamilyReportsNotDcLimitedAndNeverPass()` - assert the exact string. The
  wrong string here is a lie about what was checked, and asserting the enum only
  would let it through.
- `aMissingRatingIsUndecidableAndNamesTheMissingDatum()` - a `-parts` row without
  a drive rating; the report says which part and which field.
- `anUndrivenSectionInputIsReported()` - **fails today for every JLS design** and
  is the falsification guard: the simulator initializes every input to 0
  (`src/jls/elem/LogicElement.java:473-482`, *"Initialize all inputs to 0"*), so
  a floating input is invisible in simulation and this static check is the only
  honest report of it until FEAT-026 and FEAT-027 land.
- `twoPushPullDriversOnOneNetAreReportedWithTheDriverList()`.
- `unusedSectionInputsAreCheckedNotSkipped()`.
- `mixedFamilyThresholdViolationIsReportedPerDriverSinkPair()`.
- `syntheticCarryNetsAreLoadCheckedLikeAnyOther()` - the cascade chain from
  TASK-0087 is real copper and gets the same treatment.
- `reportIsByteIdenticalOnReRun()` - sorted output, no map iteration order.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the fan-out and DC loading check | **no issue.** The physical program (FEAT-040 through FEAT-044) is untracked in the tracker and in the committed roadmap. |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | informs - unrelated mechanism; recorded only so a reader does not assume the loading check is a simulator change. It is not; it never runs the simulator. |

## Notes

- **This is spine work priced at 1-2 weeks precisely because it is not P1.** The
  temptation is to wait for drive strengths and the strength lattice; do not. The
  static check reads part data and net structure, both of which exist as soon as
  TASK-0085 through TASK-0087 land, and it catches the failure on the bench. When
  FEAT-027's driver kinds arrive, the contention rule gains the open-drain case
  and nothing else changes.
- **The defect this closes, in the project's own words.** *"A student can wire one
  NOT gate to two hundred inputs and JLS simulates it happily at delay 5"* -
  `docs/capability-roadmap/README.md:653-657`, which also records that JLS today
  has *"no area, no input capacitance, no drive strength, no fanout limit, no
  load dependence"*. This task removes the fourth of those five for the physical
  path, and only for the physical path; the simulator is unchanged.
- **Say what is not modeled, in the report header.** No capacitive loading, no
  transmission-line effects, no decoupling, no supply drop, no temperature
  derating. The `VhdlEmitter` generated-header idiom is the precedent for stating
  a limitation inside the artifact rather than in a document nobody opens.
- **Unit loads, not milliamps.** A unit load is family-relative and is what
  datasheets and textbooks use; converting to current invites a precision the
  library does not have. Keep the arithmetic integral and exact so the report is
  reproducible across platforms with no floating point anywhere in it.
- **`Put` is sealed over `Input` and `Output`** (`src/jls/elem/Put.java:16-17`),
  so "is this pin a driver or a sink" is decidable at the schematic level with no
  new type. `TriState` is the one element whose output is conditionally a driver;
  handle it explicitly rather than letting it fall into the push-pull count.
- **Do not fold this into the manufacturability gate.** TASK-0091 aggregates
  named rules into a fabricate-or-not verdict; it consumes this report. Two
  implementations of fan-out, one in each, is the predictable outcome of leaving
  the boundary unstated.

## Evidence

- `docs/capability-roadmap/README.md:653-657` - the fan-out gap in the project's
  own words, and the enumerated list of what the delay model does not carry.
- `src/jls/elem/LogicElement.java:473-482` - `initInputs()`, *"Initialize all
  inputs to 0"*, `in.setValue(BitSetUtils.Create((long)0))`: why a floating input
  is invisible in simulation and must be caught statically.
- `src/jls/elem/Put.java:16-17` - `abstract sealed class Put permits Input,
  Output`, which makes driver-versus-sink classification total by construction.
- `src/jls/hdl/board/Board.java:64-80` - deterministic sorted listings, the
  discipline the report output follows.
- `docs/batch-interface.md:33-48` - the exit-status contract the verdict mapping
  honors.
- `docs/simulation-semantics.md` §4 and §6 - the value and multi-driver semantics
  this check deliberately does not depend on, referenced so the independence is
  explicit rather than assumed.
