# FEAT-004 - Shared net-partition IR with stable net naming

**Status:** proposed | **Cost:** 2-3 mw | **Owner program:** P3 |
**Spine rank:** S3, S5

## Capability delivered

There is exactly one answer in JLS to the question "which pins are on the same
net", it lives in its own package with its own tests, and every consumer -
HDL emitters, netlist writers, board constraint emitters, PCB and schematic
emitters, waveform variable declarations, the analog bridge, the breadboard
consistency check - reads that one answer. The names that partition produces are
a documented function of permanent element identity, so a net called `net_41`
today is called `net_41` after someone inserts an unrelated gate, and any
external tool that annotates a JLS net by name can key on it.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | beneficial | per-view geometry and cross-probing address artifacts by net, and a net whose name moves is not addressable |
| CAP-02 | beneficial | a boot-length differential comparison against an external simulator is keyed on signal names that must survive an edit |
| CAP-03 | beneficial | the same, for the ternary machine, plus a radix manifest keyed per net |
| CAP-04 | required | the breadboard consistency check asserts that two views describe the same nets, which presupposes one partition |
| CAP-05 | required | a board netlist is a net partition; two partitioners would let the schematic and the board disagree |
| CAP-07 | required | the shuttle wrapper binds top-level ports to nets, and a shuttle submission is not re-runnable if the names move |
| CAP-13 | required | net isomorphism against KiCad's recovered partition is the parity assertion itself |
| CAP-14 | required | a SPICE deck's nodes are nets; ngspice comparison is per node |
| CAP-15 | required | hierarchy goldens must be generated once, over names that do not move |
| CAP-17 | required | partition boundaries are cut on nets; a net must keep an identity that survives being cut and must name the same signal on both sides of the cut |
| CAP-18 | required | a signal-integrity constraint is attached to a net, so a net needs a name that survives save, load and export unchanged - otherwise the board tool's violation cannot name the net back. Added 2026-08-03 under D16: the filed issue #336 declares `serves_capstones: [... 313 ...]` and #313 carries 336 in `requires_features`; this table's omission was a transcription defect |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-001 | the extracted package inherits `HdlExporter`'s three-bucket element policy; a policy that is not total over the element registry silently drops types out of every consumer of the shared IR, not just out of one emitter |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0007 | Extract the net-partition walk into its own package | there is no `jls.netlist` package at HEAD; the walk lives inside the exporter and cannot be consumed by anything that is not an HDL emitter |
| TASK-0008 | Key net and probe names off stable id, and validate them | names are synthesized from the save-time index today, and probe names are never validated at all |

## Acceptance criteria

- `jls.netlist` exists as a leaf package with its own tests, and
  `src/jls/hdl/HdlExporter.java` consumes it rather than containing a partition
  walk. The whole shipped golden corpus - 32 Verilog, 32 VHDL and 1 board
  constraint file at HEAD - is byte-identical across the extraction; the move is
  proven pure by them.
- Synthesized net names are a documented function of `Element.getStableId()`.
  Inserting an unrelated element into a fixture and re-exporting changes no net
  name in the emitted artifact.
- User-supplied names still win: ports, named registers and jump aliases keep
  the name the user typed; only genuinely anonymous nets are synthesized.
- The naming convention is written down as a normative contract in
  `docs/file-format.md` or a sibling, because SAIF, SDF, external-simulator VCD
  comparison and any future annotation format key on it.
- A probe name is validated on attach against the same rule the format uses, and
  the waveform variable-declaration checker rejects exactly what the referenced
  waveform specification rejects. A probe named `my probe.name` no longer
  reaches the dump.
- A second consumer exists in tree that is not an HDL emitter, proving the seam
  is real rather than a renamed private method.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) net-name stability and the shared partition | no issue |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | informs - the partition is that issue's substrate, but it does not cover extraction or naming |
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | overlaps - `PcfEmitter` walks `model.ports()` and the invariant that it can never disagree with the Verilog emitter about the interface is currently a coincidence of shared code, not a property of a shared pass |

## Design notes

This is the enabling refactor for every netlist-shaped emitter in the plan, and
its cheapness is the reason it outranks work that looks more valuable. It is
pure motion plus a naming policy: no new primary data, no new element, no format
version.

Order matters and the dependency is real, not stylistic. Name stability must
land before any new goldens are generated. Hierarchy (FEAT-018) generates a
large golden corpus; the Yosys writer (FEAT-019) generates another; the gEDA and
KiCad emitters (FEAT-042) generate a third. Every corpus generated before names
are stable is regenerated afterward. `09-format-adoption-plan.md` W0.3 states
this as a hard gate on W2.1 and W4.1 for exactly that reason.

The naming function should be a short deterministic digest of the stable id
rather than the raw id, so that names stay readable and do not leak replica
strings into artifacts a student reads. Whatever the choice, freeze it, test it,
and write it down - the freeze is the deliverable, not the digest.

Probe-name validation is not cosmetic. A probe name lands in a waveform variable
declaration, and a malformed declaration is a file the reference viewer either
rejects or misreads. This is the cheapest correctness defect in the format
study.

## Risks

- **The extraction can silently change behavior** if the walk is rewritten
  rather than moved. The mitigation is the golden count: the goldens are the
  proof of purity and must not be regenerated in the same commit.
- **A frozen naming convention is a compatibility surface.** Once SAIF or an
  external comparator keys on it, changing it is a breaking change for
  downstream annotation. That is the point, and it should be stated as a
  stability promise with an epoch, not left implicit.
- **Two consumers is the test of the seam.** If the only consumer at the end of
  the work is still `HdlExporter`, the package boundary is decorative and the
  second partitioner will be written later anyway.

## Evidence

- Verified at HEAD `addc6c5`: `src/jls/netlist` does not exist; the partition
  walk is inside `src/jls/hdl/HdlExporter.java` (1,422 lines), whose union-find
  chain is referenced at `:1199,1209`.
- Verified at HEAD: `grep -rn stableId src/jls/hdl/` returns **0**. Permanent
  element identity has existed since issue #165 and no exporter reads it.
- Verified at HEAD: `src/jls/hdl/HdlExporter.java:353` synthesizes anonymous net
  names as `"net_" + el.getID()`, and `:638-639` synthesizes register names as
  `"reg_" + el.getID()`. `getID()` is the dense save-time index documented at
  `src/jls/elem/Element.java:21-22` as "reassigned on every save".
- Verified at HEAD: `src/jls/elem/Wire.java:462-468` (`attachProbe`) assigns
  `probeName` from its argument with no call to `Util.isValidName`
  (`src/jls/Util.java:219-234`).
- The three-renderer shape the extraction serves already ships:
  `src/jls/hdl/VerilogEmitter.java` (752 lines),
  `src/jls/hdl/VhdlEmitter.java` (1,149) and
  `src/jls/hdl/board/PcfEmitter.java` (199) all walk the same port list.
  Golden corpus counted at HEAD: 32 `.v`, 32 `.vhdl` under `test/resources/hdl/`
  and 1 board constraint golden under `test/resources/hdl/board/`. Several
  corpus documents state 34 and 29; those figures predate recent merges.
- Cost band basis: `09-format-adoption-plan.md` W1.2 (1-2 wk extraction) plus
  W0.3 (0.5-1 wk naming), leverage table rows 5 and 12.
- Do not restate: `docs/simulation-semantics.md` owns the value domain,
  `docs/vcd-interop.md` owns the waveform profile, `docs/file-format.md` owns
  identifier rules, `ARCHITECTURE.md` owns package layering.
- **Cost reconciliation.** Band 2-3 mw. Tasks named for it: TASK-0007,
  TASK-0008, totalling 3 wk. Band and task sum agree; no reconciliation is
  needed. Shared tasks counted once at the task level: TASK-0008.
