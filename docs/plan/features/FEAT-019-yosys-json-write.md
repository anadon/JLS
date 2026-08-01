# FEAT-019 - Yosys JSON write

**Status:** proposed | **Cost:** 3-4 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

JLS writes the netlist interchange format that the open synthesis toolchain and
its ecosystem read. One emitter, over the net partition and the intermediate
representation that already exist, makes a drawn circuit renderable as a
publication-quality schematic by an external renderer, runnable as a browser
simulator shareable by link, and convertible by the synthesis tool itself into
the gate-level interchange formats - so JLS reaches those formats through real
technology mapping instead of through a lowering pass it would have to write,
own and defend.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-07 | beneficial | the shuttle flow is a synthesis flow, and speaking the synthesis tool's own netlist shortens it |
| CAP-08 | beneficial | the import path and the export path must agree; a writer is what makes the round trip assertable |
| CAP-13 | beneficial | reaches gate-level and SPICE-shaped consumers through the synthesis tool without a second lowering pass in JLS |
| CAP-14 | beneficial | the SPICE deck a digital design hands to ngspice is produced by the synthesis tool from this format |
| CAP-15 | required | parity with the toolchain means writing what it reads, not only reading what it writes |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-004 | the writer emits nets; without one shared partition JLS acquires a second one that can disagree with the HDL emitters about the same circuit, and without stable names the goldens regenerate on every unrelated edit |
| FEAT-018 | beneficial - a flat netlist ships first; hierarchy in the netlist follows the intermediate representation gaining instances |
| FEAT-001 | beneficial - a new emitter inherits the export policy, and a non-total policy drops element types out of the netlist silently |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0045 | The synthesis-tool netlist writer | the writer itself, with goldens validated against the published schema |
| TASK-0046 | Document the tool-mediated netlist paths | the gate-level interchange formats are reached by documented recipes over this output, not by new emitters |
| TASK-0111 | The test panel, the grading harness and its reports | consumes the emitted netlist as one of the machine-readable artifacts a grader can diff |

## Acceptance criteria

- A drawn circuit emits a netlist that validates against the published machine-
  readable schema for the format, pinned by byte-exact goldens for the fixture
  corpus.
- The external synthesis tool reads the emitted file and reports the same module
  interface JLS declared, under the shipped skip-when-absent tool-locator idiom
  so the check skips cleanly where the tool is absent.
- Round trip: a netlist JLS writes, read back by JLS's own importer, produces a
  circuit whose net partition is isomorphic to the original by stable id.
- Documented, tested recipes exist that produce the gate-level interchange
  formats and a SPICE-shaped netlist by running the external synthesis tool over
  JLS output. Each recipe is exercised in CI where the tool is present.
- The command-line export dispatch accepts the new suffix in **both** places it
  is currently decided, so the emitter does not ship behind a two-way switch.
- Re-emitting an unchanged circuit is byte-identical.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope | overlaps - that issue staged the import direction only; the write direction is not in it |
| #61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | informs - the read side; the schema and the cell vocabulary are shared and must not fork |
| - | (no issue) the write direction has no issue | no issue |

## Design notes

This is the highest formats-unlocked-per-week item in the format study, and the
reason is structural rather than enthusiastic: the parse side of the format
already ships in tree as a 580-line value model, so the writer is the small half;
eight of the eleven statement kinds map directly; and the bit-routing statement
is free because bit routing is already expressed in the connection arrays the
format uses.

The reason it outranks the gate-level interchange formats is a correction to a
prior document, and a task author needs it: those formats are **not** printers
over the existing intermediate representation. Their value level is
instance-of-cell and single-output two-level nodes, and JLS's is word-level -
there is no word-level addition statement in either. Emitting them directly
requires a bit-level lowering pass costed at 6-10 maintainer-weeks before a line
of syntax is written, and that pass duplicates software this repository already
depends on in tests. The named alternative is this feature plus a documented
recipe, at a fraction of the cost, with real technology mapping.

The seam to emit through already exists as a published extension point in the
HDL package; use it rather than adding a fourth branch to the command-line
dispatch.

## Risks

- **Schema drift.** The format is defined by an implementation and a
  community-maintained schema rather than by a standard with a change process.
  Pin the schema as a test fixture and pin the tool version in CI; a golden that
  regenerates on every upstream release is a maintenance event at bus factor 1.
- **Unmappable statements.** The statement kinds that do not map directly must
  be refused explicitly with a reason, not approximated. An approximated cell in
  a netlist is silently wrong downstream.
- **Ownership.** Nothing in the committed capability roadmap pays for this. It
  is the best item in the format plan and it has no owner; leaving it unowned is
  how it gets scheduled inside someone else's week.

## Evidence

- Verified at HEAD `addc6c5`: `src/jls/hdl/yosys/JsonValue.java` is 580 lines
  and is parse-only; `src/jls/hdl/yosys/YosysNetlist.java` (953 lines) and
  `CellValidator.java` (276) are read-side; there is no writer in
  `src/jls/hdl/`.
- Verified at HEAD: the export dispatch decides the suffix in two places -
  `src/jls/JLSStart.java:381-385` (a binary Verilog-or-VHDL choice) and
  `:1088-1091` (an extension allowlist that rejects anything not `.v`, `.vhd` or
  `.vhdl`). Both must learn a third suffix.
- Verified at HEAD: `src/jls/hdl/HdlExtensionPoints.java` publishes the emitter
  seam; `src/jls/hdl/HdlEmitter.java` is the 29-line interface the two shipped
  printers implement.
- The projection thesis is shipped rather than proposed: `VerilogEmitter.java`
  (752 lines) and `VhdlEmitter.java` (1,149) render the same port walk in
  unrelated syntaxes, and `board/PcfEmitter.java` (199) renders it in a third.
- `09-format-adoption-plan.md` §3 leverage table row 1: sufficient for five
  downstream consumers at 3-4 maintainer-weeks, the best ratio in the study;
  §3.3 records why the gate-level formats are an engine rather than a datum, and
  names the recipe alternative at 0.25-0.5 weeks of documentation each.
- Cost band basis: `09-format-adoption-plan.md` W1.3 (3-4 wk) at the
  repository's ~200-250 shipped-and-tested lines per maintainer-week
  calibration, against the shipped `PcfEmitter` and `VerilogEmitter` datapoints.
- Do not restate: `docs/hdl-support-research.md` owns the staged HDL plan and
  the tool-mediated routes; `docs/standards-adoption/` owns the format
  assessments.
- **Cost reconciliation.** Band 3-4 mw. Tasks named for it: TASK-0045,
  TASK-0046, TASK-0111, totalling 4.6 wk. The task sum exceeds the band
  because 1 of these tasks are shared with other features (TASK-0111) and
  their weeks are counted once, at the task level, not once per consuming
  feature.
