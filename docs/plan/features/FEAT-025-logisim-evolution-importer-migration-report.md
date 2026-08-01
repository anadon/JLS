# FEAT-025 - Logisim-Evolution `.circ` importer and migration report

**Status:** proposed | **Cost:** 6-12 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A course's existing material opens. A file written by the incumbent teaching
tool loads into JLS with its structure intact, and every construct that did not
survive - unmapped component, unsupported attribute, approximated behavior - is
named in a report the instructor reads, with the location and the reason. This
is the only item in the plan that moves *users* rather than files: it is the
difference between "JLS is a better tool" and "you can bring your labs".

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-16 | required | this feature is that capstone's spine |
| CAP-06 | beneficial | a course is its assignments; migrating the circuits without the graded material migrates half a course |
| CAP-04 | beneficial | the incumbent's through-hole and 74-series material is the readiest source of the part data a breadboard view needs |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-002 | a migration report that names every dropped construct is the fail-loud loader discipline applied to a foreign format; building the report against a loader that silently discards unknown attributes produces a report that is confidently incomplete |
| FEAT-040 | the part data the imported designs reference has to exist as data before an importer can bind to it |
| FEAT-022 | beneficial - designs whose source coordinates cannot be honored need placement, and that is the same layout seam with a different engine |
| FEAT-015 | beneficial - the importer should build circuits through the programmatic construction verbs rather than by emitting save text and reparsing it |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0054 | The foreign-tool reader and its migration report | the reader, the written construct map and the report, with a test asserting nothing is dropped silently |
| TASK-0055 | Absorb the through-hole part data | the part data the migrated designs reference, with its attribution and license notices |

## Acceptance criteria

- A `.circ` file opens and produces a JLS circuit whose net structure matches the
  source's, checked against a corpus of public files rather than against
  hand-written fixtures.
- Every construct the importer does not map appears in the migration report with
  its location and the reason. A test asserts that the set of constructs present
  in the source and absent from the result is exactly the set named in the
  report - nothing is dropped silently.
- The construct map is written down as a document, not encoded only in a switch,
  and a name collision between a source construct and a JLS element with the same
  name but different semantics is a **loud reject**, never a mapping.
- Parsing is hardened as untrusted input: external entities disabled, document
  type declarations disallowed, entity resolvers neutralized, with a test per
  attack vector. This is the first parse of its kind in the shipped source tree
  and it must not be the weakest one.
- Absorbed part data carries its attribution and license notice in tree, and a
  license audit passes.
- The measurement exists before the estimate is trusted: the accept and reject
  tables are run over a corpus of public files and the result is recorded.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) the migration path has no tracking issue at all | no issue |
| #62 | HDL Stage 2 companion: schematic auto-layout for imported netlists (heuristic layered layout; ELK only out-of-process) | informs - the same layout seam serves a coordinate-preserving engine for migrated designs |
| #214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | informs - migrating the circuits without the assignment's test vectors migrates half of what a course is |
| #61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | informs - the existing importer is the structural precedent, and its private builder must be promoted before a second importer duplicates it |

## Design notes

Four hazards are known before a line is written and each one is a specific,
avoidable defect rather than general caution.

**Connectivity in the source format is purely geometric.** Components connect by
coordinate, and per-component port offsets are computed by rules with special
cases keyed on input count and body size. An importer that does not replicate
those rules exactly produces circuits that import silently disconnected - the
worst failure mode available, because the file opens and looks right.

**There is a name-collision trap that is a silent correctness disaster.** At
least one component name means a sequential element in the source tool and a
combinational element in JLS. Mapping by name is not a shortcut here; it is a
wrong circuit. Every mapping must be by semantics with the name as a hint, and
every ambiguous name must reject loudly.

**This would be the first parse of its kind in the shipped tree.** A migrated
file is untrusted input, and the project's own security premise is that a
circuit file cannot touch the machine that opens it. The hardening is not
optional and is not a follow-up.

**The estimate is not a measurement.** No converter from this format to any
other format exists anywhere, which is why the capability is valuable and also
why nobody has data on how much of a real corpus maps. Run the accept and reject
tables over public files first; the result could reorder everything after it.

License note that must be settled before absorbing any source: the incumbent's
per-file notices name a specific version without the customary "or later"
clause. Absorbing such code into this project is compatible but silently costs
this project its own "or later". The options are to accept and record it, to ask
upstream, or to re-derive - and re-deriving is recommended *against*, because a
clean-room reimplementation from source you have read is a worse legal position
than complying, and it is far more error-prone in exactly the geometric
special cases that matter.

## Risks

- **Silent disconnection.** The geometric connectivity rules are the highest-
  defect-density part of the work and the failure is invisible. Mitigation: a
  connectivity assertion per imported file comparing net counts and net
  membership against the source's own computed nets, not a visual check.
- **A partial migration reads as a broken tool.** An instructor whose lab
  imports at 70% will not adopt. The report is what converts a partial import
  from a failure into a work list, which is why it is an acceptance criterion
  and not a nicety.
- **Corpus availability.** The measurement depends on public files that are
  representative of course material. If the corpus is unrepresentative, the
  accept table is optimistic and the estimate follows it.
- **Scope pull.** A second migration source reuses the scaffolding cheaply,
  which is exactly why it should be a separate decision with its own trigger
  rather than an assumed continuation.

## Evidence

- Verified at HEAD `addc6c5`: `grep -rn "javax.xml\|org.w3c.dom" src/` finds no
  XML parse in the shipped tree; this importer would be the first.
- Verified at HEAD: the structural precedent is `src/jls/hdl/imp/` -
  `NetlistImporter.java` (1,067 lines), `ImportResult.java` (52),
  `ImportSummary.java` (102) - whose builder at `NetlistImporter.java:410` is
  `private static final class` and must be promoted before a second importer
  exists.
- Verified at HEAD: the no-partial-circuit discipline this importer should
  inherit is documented at `NetlistImporter.java:41-47`.
- Verified at HEAD: the name-collision trap is real and already recorded in
  JLS's own exporter - `src/jls/hdl/HdlExporter.java:84` notes of the JLS
  element that shares a name with a sequential component in the source tool that
  it "holds no state, issue #122 - rendered as one dataflow shift".
- Verified at HEAD: the layout seam a coordinate-preserving engine would plug
  into is `src/jls/hdl/layout/SchematicLayouter.java`, kept engine-neutral by
  the 2026-07-17 adjudication recorded in that package's `package-info.java`.
- `09-format-adoption-plan.md` Wave 8 costs the importer at 12-18 wk for the
  first increment plus 1-2 for the test-vector reader and 2-3 for
  coordinate-preserving layout, and states explicitly that the figure "must not
  be mistaken for a measurement" until the corpus run is done. This feature's
  band is the reduced scope that reuses the existing importer scaffolding and
  part data; the wider figure is the standalone one.
- Do not restate: `ARCHITECTURE.md` owns the layering, `SECURITY.md` owns the
  untrusted-input premise, `docs/file-format.md` owns what a valid JLS circuit
  is.
- **Cost reconciliation.** Band 6-12 mw. Tasks named for it: TASK-0054,
  TASK-0055, totalling 4 wk. The named tasks are the leading, dividable slices
  of this feature, not the whole of it; the residual has no task id, because
  the registry's task space is closed at TASK-0112. Do not read 4 wk as the
  feature.
