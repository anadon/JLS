# TASK-0099 - Controlled sources, waveforms and model cards

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0097

## Deliverable

The three things that turn the bare solver into something a teaching lab can
build circuits with: dependent sources, time-varying independent sources, and
the ability to consume a vendor model or subcircuit as **data**.

Precisely what changes:

- `jls/analog/Devices.java` gains the four controlled-source stamps - `E`
  (VCVS), `F` (CCCS), `G` (VCCS), `H` (CCVS) - with native `POLY(n)` support,
  plus the `S` (voltage-controlled) and `W` (current-controlled) switches with
  declared hysteresis.
- `jls/analog/Waveforms.java` - `PULSE`, `PWL`, `SIN`, `EXP`, `SFFM` and `AM`,
  each a pure function of time with its parameter list and its breakpoint set.
  Breakpoints matter: they are what the timestep controller must land on
  exactly, and a waveform that reports none makes the accept/reject rule
  meaningless at edges.
- `jls/analog/DcSweep.java` - the `.dc` sweep as a loop over the escape ladder
  with warm starts from the previous point.
- `jls/analog/CardReader.java` - a **small** card grammar only:
  `.subckt`/`.ends`, `.model`, `.param`, and instance lines. Explicitly not a
  SPICE deck parser: no `.control`, no `.include` chains, no expressions beyond
  what `.param` needs. Every unrecognized card is **reported by name and line
  number**, never skipped silently - the same fail-loud posture FEAT-002
  applies to the `.jls` loader.
- `jls/analog/SpiceNumber.java` - the ~40-line suffix parser (`T G MEG K M U
  N P F`, including the classic `1M`-versus-`1MEG` hazard) shared by the
  dialogs, the `.jls` loader and the card reader. **One implementation**, or
  the diagnostic message diverges at three surfaces.
- Saved form: every analog parameter is a `String` holding its SPICE spelling
  (`String r "4.7k"`, `String c "10n"`), parsed to a transient double on load.
  The suffix table becomes normative text in `docs/file-format.md`. This costs
  **zero format version**; a `double` item kind would cost a version bump for
  every analog file and buy nothing.
- `.subckt` references save as **path + FNV-1a digest + positional terminal
  map, body not embedded** - libraries are data (D7), and embedding a vendor
  macromodel would import its redistribution terms. `F` and `H` current-
  controlling references must use the **stable id**, never the dense save-time
  id, which is reassigned on every save (`src/jls/elem/Element.java:21-22`).
- A model-card inspector: a read-only view listing each resolved `.model`, its
  parameter set, and its provenance path and digest.

Done means: an inverting amplifier built from a vendor op-amp `.subckt` and a
Sallen-Key filter's step response both solve, a mistyped card names its own
line, and a re-save of a circuit carrying analog parameters is byte-identical.

## Enables features

| FEAT | what this unblocks |
|---|---|
| FEAT-046 | The source and model-card layer of the analog solver core - the part that makes circuits expressible rather than only solvable. |

## Prerequisite tasks

| TASK | why |
|---|---|
| TASK-0097 | Controlled sources are matrix stamps and waveforms feed the timestep controller's breakpoint set. Both read structures only TASK-0097 creates; neither is meaningful without a solve. |

## Acceptance test

`test/jls/analog/ControlledSourceStampTest` (new class):

- `eachControlledSourceStampsExactlyTheExpectedEntries()` - one method per
  source kind, asserting the exact matrix and RHS entries on raw bits, with
  expected values written as the same expression the kernel computes. This is
  the tier that catches terms below physical tolerance - the canonical case
  being a sign flip on a `GMIN`-sized conductance, which survives every
  waveform test at every tolerance and dies only against an exact stamp.
- `polyOrderTwoMatchesTheHandExpandedStamp()` - `POLY(2)` against a hand-worked
  expansion.

`test/jls/analog/WaveformTest`:

- `everyWaveformReportsItsBreakpointsAndTheControllerLandsOnThem()` -
  parameterized over all six waveforms; asserts the accepted time points
  include every declared breakpoint exactly.
- `pwlWithUnsortedPointsIsRejectedWithTheOffendingIndex()`.

`test/jls/analog/CardReaderTest`:

- `everyUnknownCardIsReportedWithItsNameAndLineNumber()` - a fixture deck with
  four unsupported cards asserts four diagnostics in one pass and no silent
  acceptance.
- `aVendorOpAmpSubcktResolvesToItsTerminalsPositionally()`.
- `theDigestOfAnUnchangedSubcktFileIsStable()`.

`test/jls/analog/SpiceNumberTest.oneMegIsNotOneMilli()` - the named hazard,
plus a table-driven pass over every suffix, plus
`parseIsLocaleIndependent()` under a comma-decimal locale.

`test/jls/analog/AnalogParameterRoundTripTest.aReSaveDoesNotReformatTheSpelling()`
- saves, loads and re-saves a circuit carrying `"4.7k"` and asserts the bytes
are identical and the string was never round-tripped through a double.

## Related GitHub issues

**no issue.** `search_issues` over `anadon/jls` for `spice OR analog OR model`
returns no open issue touching analog device modeling; the registry records the
entire analog program (FEAT-045 through FEAT-049) as untracked.

Adjacent, and cited only for the discipline it shares:

| # | title | relationship |
|---:|---|---|
| #78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract… | informs - the "one table, compiler-enforced" posture the shared suffix parser and the card grammar both follow. Not closed by this task. |

## Notes

- **Op-amps arrive as data, at zero maintainer-weeks of model library.** This
  is the highest-leverage single item in the analog program after the host
  audio door and the linear fast path: a `.subckt` reader means the vendor
  ships the model and JLS ships the reader. Reimplementing a vendor model
  library (BSIM4 and relatives) is explicitly not plausible and is not
  attempted - scope the models, not the solver.
- **The format trap, stated precisely.** `docs/file-format.md:118-140` closes
  the item-kind set to `int|long|bigint|string|ref|pair|probe|circuit-block`
  and says a reader encountering anything else **MUST fail the load**;
  `Element.setValue` has exactly four overloads
  (`src/jls/elem/Element.java:344,359,374,389`) and `Attribute` declares
  exactly `IntAttribute`, `BigIntAttribute`, `StringAttribute` and
  `OrientationAttribute` (`src/jls/elem/Attribute.java:111,190,268,352`).
  There is no `double` anywhere. Saving the SPICE spelling is not a workaround;
  it is locale-proof, diff-perfect under D1/D2, and it is the notation every
  datasheet already uses.
- **`Element.setValue` returns silently on an unmatched attribute name**
  (`src/jls/elem/Element.java:344-351`, same shape on the three sibling
  overloads). Every analog parameter added here inherits that silent-data-loss
  path until TASK-0003 lands. Do not write a second silent path in the card
  reader: report the unknown card.
- **Sealed-hierarchy trap.** If the device kinds are a sealed interface, adding
  `E`/`F`/`G`/`H`/`S`/`W` stops the build at every exhaustive stamp switch.
  Keep it that way; no `default` arm.
- **Cost honesty.** 2 weeks is the sources-and-cards slice; the stage is 3-4
  maintainer-weeks including the model-card inspector. FEAT-046's 17.5-26 mw
  band carries the remainder.

## Evidence

- Scope of S3 - `E F G H` with native `POLY(n)`, `S`/`W`, the six waveforms,
  `.dc`, and a deliberately small card grammar - and the "op-amps as data"
  finding: `11-analog-determination.md` §5 stage S3.
- The saved-parameter decision, the suffix table, the `.subckt` reference form
  (path + FNV-1a digest + positional terminal map, body not embedded), and the
  stable-id requirement for `F`/`H` cross-references:
  `11-analog-determination.md` §2.10.
- The exact-stamp test tier and the `GMIN` sign-flip case that only it catches:
  `11-analog-determination.md` §4.2 (tier T3); the measured PIT effect of
  stamp tests, 76%/79% to 86%/88% against the repo's 80/82 gate
  (`pom.xml:812-813`).
- HEAD facts: `docs/file-format.md:118-140`;
  `src/jls/elem/Element.java:344,359,374,389`;
  `src/jls/elem/Attribute.java:111,190,268,352`.
