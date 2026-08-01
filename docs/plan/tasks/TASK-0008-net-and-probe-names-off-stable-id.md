# TASK-0008 - Key net and probe names off stable id, and validate them

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0005

## Deliverable

Synthesized net naming becomes a documented function of stable id under a
frozen convention, probe names are validated when attached, and the waveform
variable-declaration writer rejects what its spec rejects.

1. **Synthesized net names stop depending on the dense save index.**
   `HdlExporter` derives seven families of identifier from `Element.getID()` -
   the file-local index reassigned on every save
   (`src/jls/elem/Element.java:22`, assigned at `src/jls/Circuit.java:1499-1503`):

   | site | pattern |
   |---|---|
   | `src/jls/hdl/HdlExporter.java:353` | `net_<id>` / `net_<id>_<k>` |
   | `:381` | `net_u<id>` |
   | `:725` | `mux_<id>` |
   | `:753` | `dec_<id>` |
   | `:870` | `tt_<id>_<k>` |
   | `:1003` | `sm_<id>_<k>` |
   | `:1313` | `unc_<id>_<k>` |

   All seven move to a single helper deriving the suffix from
   `Element.getStableId()`. The convention - how a `replica:counter` id
   (`docs/file-format.md:385-389`) becomes a legal HDL identifier, and how
   collisions are broken - is **frozen and documented**, because it appears in
   every exported netlist and in every golden.

2. **The undriven-net sort key follows.** `src/jls/hdl/HdlExporter.java:373-379`
   builds `String.format("%09d_%s", el.getID(), put.getName())` purely to make
   the undriven-net naming order deterministic. It becomes a stable-id-keyed
   comparator; the nine-digit zero pad is a dense-index artifact and goes.

3. **Probe names are validated on attach.** `Wire.attachProbe`
   (`src/jls/elem/Wire.java:462-481`) accepts any non-null string with no
   check; the only validation is a non-empty retry loop on the interactive
   prompt path (`:474-478`). Add a single validator and call it from all four
   attach sites: `src/jls/elem/Wire.java:462`,
   `src/jls/collab/op/AttachProbe.java:31`,
   `src/jls/collab/op/AddWire.java:163`, `src/jls/Util.java:117`, plus the load
   path at `src/jls/elem/WireEnd.java:155`. Rejection on the load path is a
   diagnostic, not a refusal, for the same forward-compatibility reason as
   TASK-0003.

4. **The VCD writer enforces its own spec.** `BatchSimulator.toVcd`
   (`src/jls/sim/BatchSimulator.java:384-476`) emits
   `$var wire <bits> <code> <name> $end` at `:425-434` from the raw signal name.
   IEEE 1364-2001 §18 identifiers cannot contain whitespace, and JLS's own
   contract document is `docs/batch-interface.md` (named at
   `src/jls/sim/BatchSimulator.java:372-374`). Add an escape-or-reject rule at
   the `$var` site, applied to element full names and probe names alike, and
   record it in `docs/batch-interface.md`.

5. **`docs/file-format.md` §3.1 Names** (`:149-158`) gains the probe-name rule;
   it currently constrains circuit and subcircuit names only.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-004 | Stable net naming is half the feature's title; a net name that moves when an unrelated element is inserted is not an addressable name. |
| FEAT-005 | Removes the last dense-index consumer outside the file format, which is what lets the load path stop maintaining one. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0005 | This reads stable ids in positions that today read `getID()`. TASK-0005 establishes that stable ids are the reference currency and regenerates the format goldens; doing the naming change first would mean regenerating the same HDL goldens twice for the same reason. The dependency is on the epoch decision, not on the file bytes: nothing here reads a byte TASK-0005 writes. |

## Acceptance test

`test/jls/hdl/NetNameStabilityTest.java`, new:

- `synthesizedNetNamesSurviveInsertingAnUnrelatedElement()` - exports a fixture
  to Verilog, inserts one element that sorts earlier in canonical order,
  re-exports, and asserts the set of synthesized `net_*` names is unchanged.
  Must fail at HEAD: the insert renumbers every later `getID()` and so renames
  every net after it.
- `everySynthesizedFamilyIsStableUnderInsertion()` - the same over `mux_`,
  `dec_`, `tt_`, `sm_`, `unc_` and `net_u`, so no family is missed.

`test/jls/elem/ProbeNameValidationTest.java`, new:
`everyAttachSiteRejectsAnInvalidProbeName()` - a `@ParameterizedTest` over the
four in-memory attach sites asserting the same rejection for a name containing
whitespace, and `loadOfAnInvalidProbeNameIsADiagnosticNotARefusal()` asserting
a fixture with such a name still loads.

`test/jls/VcdExportGoldenTest` gains
`aProbeNameThatIsNotAVcdIdentifierNeverReachesTheVarLine()`, asserting the
emitted `$var` line is parseable and that the golden path is unaffected.

## Related GitHub issues

**No issue** for net naming or probe validation.

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - the exporter whose names change is #59 stage 1; nothing in #59 closes on this |

Recorded decisions, closed, cite as such: **#72** (VCD export), **#200**
(probed nets in the trace), **#165**/**#166** (stable ids, canonical order).

## Notes

- **Every HDL golden regenerates.** `test/resources/hdl/`,
  `VhdlExportGoldenTest`, `VhdlEmitterPolicyTest`, `HdlPolicyTest`'s inline
  expectations and `test/jls/hdl/board/PcfGoldenTest` all contain literal
  `net_<n>` strings. This is the largest mechanical cost of the task; budget
  it.
- **`getID()` is only valid after a save.** `setID` is called from exactly one
  place, `Circuit.save` (`src/jls/Circuit.java:1501`). `HdlExporter.buildModel`
  is called directly on a loaded circuit by
  `test/jls/hdl/board/PcfGoldenTest.java:53` and
  `test/jls/hdl/board/UnbindablePortsTest.java:35`, where the ids are whatever
  the last save left - or the loader's file ids. That latent incoherence
  disappears with this task; do not preserve it.
- **The VCD name-collision path already exists and is not the same problem.**
  `src/jls/sim/BatchSimulator.java:401-411` disambiguates a probe name that
  collides with an element full name by appending `_probe`. Keep it; this task
  adds *character-set* validation, which that loop does not do.
- **`Wire.attachProbe(null)` prompts the user** (`src/jls/elem/Wire.java:466-478`)
  via `TellUser.prompt`. A validator that throws on the GUI path turns a typo
  into a stack trace; the prompt loop must re-prompt with the reason, matching
  its existing empty-name behavior.
- **The convention must be frozen, not merely chosen.** Once exported netlists
  carry these names, external toolchains, board constraint files
  (`src/jls/hdl/board/`) and course materials reference them. Write the rule
  into `docs/batch-interface.md` and `docs/file-format.md` in the same commit.
- **Do not shorten stable ids by hashing** without stating the collision
  behavior; `HdlNames.synth` already allocates unique identifiers
  (`src/jls/hdl/HdlNames.java`, used at every site above) and is where any
  collision break belongs.

## Evidence

- `src/jls/hdl/HdlExporter.java:346-357` (`net_<id>`), `:359-382` (`net_u<id>`
  and the `%09d` sort key), `:725, 753, 870, 1003, 1313` (the five other
  families), `:102-107` (the javadoc stating the naming precedence).
- `src/jls/elem/Element.java:22` - "file-local reference index, reassigned on
  every save"; `src/jls/Circuit.java:1499-1503` - where it is assigned.
- `src/jls/elem/Wire.java:462-481` - `attachProbe` with no validation beyond
  non-empty on the prompt path.
- Attach sites: `src/jls/collab/op/AttachProbe.java:31`,
  `src/jls/collab/op/AddWire.java:163`, `src/jls/Util.java:117`,
  `src/jls/elem/WireEnd.java:155`.
- `src/jls/sim/BatchSimulator.java:372-374` (the contract doc reference),
  `:401-411` (name-collision disambiguation), `:425-434` (the `$var` line).
- `docs/file-format.md:149-158` (§3.1 Names, which does not cover probes),
  `:335-336` (the probe item's save form).
