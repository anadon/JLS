# FEAT-018 - Hierarchical instance structure in the HDL IR

**Status:** proposed | **Cost:** 4-6 mw | **Owner program:** P3 |
**Spine rank:** -

## Capability delivered

A decomposed design exports. The intermediate representation gains a statement
that instantiates a module and the ability to carry more than one module, and
the exporter walks the circuit's nesting binding instance ports to nets. A
student who drew a 4-bit adder out of four 1-bit slices gets hierarchical
Verilog and hierarchical VHDL instead of a refusal, and every downstream format
that is structurally an instance-of-module model becomes reachable for the first
time.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-05 | required | a design worth a board is decomposed, and a board netlist is a hierarchy of component instances |
| CAP-07 | required | a shuttle entry is a wrapper module instantiating the student's top module |
| CAP-08 | required | an imported third-party core keeps the module structure its author wrote, which requires the IR to hold it |
| CAP-13 | required | a KiCad netlist is instances plus nets, and a flattened one loses the structure the student drew |
| CAP-15 | required | parity with the HDL toolchains means round-tripping hierarchy, which the toolchains all assume |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-004 | this feature generates a large golden corpus; generating it before net names are a function of stable identity means regenerating every hierarchy golden a second time |
| FEAT-001 | `SubCircuit` currently sits in the exporter's explicit rejected bucket; the bucket must be total over the element registry before entries start moving out of it |
| FEAT-016 | beneficial - deduplicated hierarchy ("one module reused N times") needs a canonical definition digest; the uniquified form does not |
| FEAT-017 | beneficial - shared definitions make deduplication meaningful rather than accidental |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0043 | Module instantiation and the hierarchy walk | the IR statement, multi-module output, cycle detection, port-to-net binding and reject propagation are one change |
| TASK-0044 | Hierarchical emitters and their goldens | both printers must render it, cross-checked against the external compilers, or the IR change is unobservable |

## Acceptance criteria

- The intermediate representation carries more than one module and has an
  instantiation statement that the statement visitor dispatches; adding it does
  not silently skip in either emitter, because the visitor is total.
- A nested circuit exports as hierarchical Verilog-2005 and hierarchical
  VHDL-93. The emitted artifacts compile under the external Verilog and VHDL
  compilers in the existing CI legs, under the shipped skip-when-absent idiom.
- Instance names are uniquified deterministically, and re-exporting an unchanged
  circuit produces byte-identical output.
- A recursive nesting cycle is detected and reported with the path, not with a
  stack overflow.
- A rejection inside a nested circuit propagates to the top-level export with
  the instance path in the message, rather than being swallowed or reported
  against the top module.
- The rejected-bucket entry for nested circuits is removed, and the policy test
  that pins it is updated in the same change rather than deleted.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope | depends on / overlaps - this is the export-side completion that issue staged, but the issue spans three features and no single one closes it |
| - | (no issue) the instantiation statement itself has no dedicated issue | no issue |

## Design notes

Ship the **uniquified** form first: every instance becomes its own uniquely
named module. It is legal Verilog, legal VHDL, and accepted by the external
compilers, the fast simulator and the synthesis tool. It needs no structural
digest, no collision policy and no normative note, and it is the difference
between 4-6 weeks and 6-8. Deduplication - one module emitted once and
instantiated N times - is a later increment (FEAT-016) whose honest cost is that
the hierarchy goldens regenerate a second time.

The honest caveat on that choice, which is a maintainer call and not a cost
question: uniquified export forfeits the "one module reused N times" lesson that
is arguably the point of teaching hierarchy. Record the choice where a course
author will read it.

A correction the corpus makes to its own briefing and that a task author needs:
hierarchy is **not** new primary data. The nested circuit is already written
inline in the saved file and is already in the circuit graph. What is missing is
an intermediate-representation shape, an export-policy decision and a naming
policy. That is why this feature is priced as a projection rather than as a data
acquisition.

What hierarchy is sufficient for, and what it is not, should not be overstated:
it completes nested-circuit export and both hierarchical HDL outputs outright.
It is *necessary but not sufficient* for the gate-level interchange formats,
which additionally need a technology-mapping pass this plan deliberately routes
through the external synthesis tool (see FEAT-019), and for a PCB netlist, which
additionally needs package and footprint data (FEAT-040).

## Risks

- **Golden churn.** Two emitters, a large fixture corpus, and a naming scheme
  that must be frozen before the corpus is generated. The FEAT-004 prerequisite
  exists to make this happen once.
- **Reject propagation is where the bugs will be.** A nested circuit containing
  an unexportable element must fail the whole export with a path, and the
  natural implementation - catching and re-throwing per level - loses the path.
- **Uniquified names grow.** Deep nesting produces long module identifiers; the
  identifier legalizer must be shown a deep fixture, not only a two-level one.

## Evidence

- Verified at HEAD `addc6c5`: `grep -rn InstanceStatement src/` returns **0**.
- Verified at HEAD: `src/jls/hdl/HdlExporter.java:460-468` places `SubCircuit`
  in the `REJECTED` map with the reason "subcircuits cannot be exported yet: the
  HDL model has no module-instantiation statement, so hierarchy cannot be
  rendered - flatten the circuit to export it". The refusal is raised from the
  `offenders` throw at `:191-200`.
- Verified at HEAD: `src/jls/hdl/HdlModel.java` is 1,005 lines with a single
  module name field and a single flat statement list; its statement visitor
  declares eleven visit methods, not the ten several documents state.
- Verified at HEAD: the two printers that must learn hierarchy are
  `src/jls/hdl/VerilogEmitter.java` (752 lines) and
  `src/jls/hdl/VhdlEmitter.java` (1,149 lines); the external-compiler idiom they
  are checked under is `test/jls/hdl/GhdlCompileTest.java:34-36` and
  `test/jls/hdl/IverilogCompileTest.java`.
- Verified at HEAD: the nested circuit is already in the file -
  `src/jls/elem/SubCircuit.java:287` calls `getSubCircuit().save(output)`,
  writing the entire nested circuit inline - so hierarchy requires no new
  primary data, only an IR shape, an export-policy decision and a naming policy.
- Cost band basis: `09-format-adoption-plan.md` §3.2's sub-item table
  (instantiation statement plus multi-module model 1.5-2 wk; recursive walk
  1.5-2; emitter work 1-1.5; goldens and cross-check 1.5-2) totaling 4-6 wk
  uniquified, plus 1.5-2 for deduplication.
- Do not restate: `docs/hdl-support-research.md` owns the staged HDL plan,
  `ARCHITECTURE.md` owns the exporter's place in the layering.
