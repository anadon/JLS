# THE JLS IMPLEMENTATION PLAN

## This is planning work. It is not documentation, and nothing here is normative.

Every document under `docs/plan/` **proposes work that does not exist**. None of
it describes JLS at HEAD, none of it binds anyone, and none of it may be cited
as a specification, a contract or a decision. Status on every document is
`proposed`, and it stays `proposed` until a maintainer says otherwise somewhere
that is normative.

The normative documents live elsewhere and are **referenced, never restated**,
here:

| document | what it is normative for |
|---|---|
| `ARCHITECTURE.md` | the layering, the packages and the recorded architectural decisions |
| `docs/simulation-semantics.md` | what a simulation means |
| `docs/file-format.md` | what a `.jls` file is |
| `CONTRIBUTING.md` | how work is done, including the coverage ratchet and the sealed-dispatch rule |
| `docs/capability-roadmap/**` | the committed programs P1-P13 and their cost bands, which this plan names as owners |
| `docs/standards-adoption/**` | the recorded standards decisions |

Where a plan document appears to contradict one of those, the other document
wins and the plan document is wrong. Where a plan document cites HEAD, it cites
`file:line`; those citations were verified when written and can drift, so verify
before relying on one.

---

## Three parallel id-spaces, not a tree

There are three levels and **none of them contains another**:

- **Capstones** (`CAP-NN`, in `capstones/`) - a demonstrable outcome. Eighteen.
- **Features** (`FEAT-NNN`, in `features/`) - a capability. Fifty-seven.
- **Tasks** (`TASK-NNNN`, in `tasks/`) - what one person can sit down and
  finish. One hundred and twelve.

A feature is **not owned by** a capstone. A task is **not owned by** a feature.
The directories are flat and the ids are global.

**The reason is overlap, and the overlap is the normal case, not the exception.**
Most features serve several capstones and most tasks serve several features. The
shared net-partition work is required by seven capstones. The registry-totality
discipline is required by seven. The four-state value core is required by six.
One task - the value-representation migration - is claimed by three different
features. If the plan were a tree, each of those items would have to be
duplicated once per parent, or arbitrarily assigned to one parent and referred to
as an aside from the others. Both outcomes are worse than the flat form: the
first means the same work is estimated, scoped and eventually built more than
once, and the second hides the fact that funding one capstone pays for part of
another.

**Every relationship is an id reference in both directions.** If a capstone lists
a feature, that feature lists the capstone back, with the same required/beneficial
grade. If a task enables a feature, that feature names the task. No document says
"see the parent", because there is no parent. Nothing is duplicated between
levels: a capstone does not describe how a feature works, and a feature does not
describe how a task works.

**Where the grades disagreed, the capstone won.** A capstone declares its own
acceptance test and therefore knows what it must have; a feature is written
without sight of every consumer's acceptance test. The one deliberate reversal is
recorded in `REGISTRY.md` under LINK-PHASE RECORD.

---

## How to read this

**Top down - "what would it take to do X?"** Start at the capstone. Its
*Outcome* says what would exist; its *Acceptance test* says what a person would
see and what the automated check asserts; its *Demo slice* is the cheapest thing
worth building. Then read its *Prerequisite features* table and open each
feature; each feature's *Prerequisite tasks* table names the tasks. Three hops,
no nesting.

**Bottom up - "if I did this, what would it unblock?"** Start at the task. Its
*Enables features* table names every feature it serves; open any of those and its
*Consumed by capstones* table names every capstone that would move. This is the
direction that shows leverage, and it is the direction the tree form destroys.

**Sideways - "what does this issue cost?"** Start at `issue-map.md`, which maps
all 34 open GitHub issues to the plan ids that touch them, lists the plan ids
that have no issue, and lists the issues nothing in the plan touches.

**For ids, costs and the deduplication record**, start at `REGISTRY.md`. It is
the id authority: no id exists that is not in it.

---

## Cost, and how to read a number here

Costs are **maintainer-weeks** (`mw`) at the tree's own calibration of roughly
200-250 lines of shipped-and-tested code per maintainer-week, at the coverage and
mutation bars `CONTRIBUTING.md` sets. Tasks state days or weeks.

Two arithmetic facts hold everywhere and are printed in each document's
*Evidence* section as a **Cost reconciliation** line rather than averaged away:

1. **A feature's band is not the sum of its tasks.** Tasks are the leading,
   dividable slices - what one person can finish - and the task id space is
   closed at `TASK-0112`. Where a band exceeds its task sum, the residual is real
   work with no id. Where a task sum exceeds a band, tasks are shared between
   features and counted once at the task level.
2. **A capstone's marginal band is not the sum of its required features.** Most
   required features are shared spine, booked once against whichever capstone
   funds them first. *Marginal* is the incremental cost given the spine is
   funded; *standalone* is the cost with nothing shared.

Every item is a **path and a cost**. No document here refuses work by citing the
absence of the work, and no document gates the maintainer's own roadmap behind a
demand for prior demand.

---

## Index: capstones

| id | title | pri | marginal | standalone | required features | doc |
|---|---|---:|---|---|---:|---|
| **CAP-00** | Deferred maintenance: a decade of it | 1 | 35-62 mw | 35-62 mw | 9 | [capstones/CAP-00-deferred-maintenance.md](capstones/CAP-00-deferred-maintenance.md) |
| **CAP-01** | Multi-discipline multi-user simultaneous development | 5 | 30-46 mw | 49-75 mw | 10 | [capstones/CAP-01-multi-discipline-multi-user.md](capstones/CAP-01-multi-discipline-multi-user.md) |
| **CAP-02** | Boot a CLI-only Linux distribution and run commands | 16 | 32-58 mw | 155-250 mw | 16 | [capstones/CAP-02-boot-linux.md](capstones/CAP-02-boot-linux.md) |
| **CAP-03** | A ternary CPU with N-ary subcircuits and a custom kernel | 17 | 28-45 mw | 98-161 mw | 17 | [capstones/CAP-03-ternary-cpu.md](capstones/CAP-03-ternary-cpu.md) |
| **CAP-04** | A breadboard implementation of a simple CPU | 8 | 31-46 mw | 50-77 mw | 11 | [capstones/CAP-04-breadboard-cpu.md](capstones/CAP-04-breadboard-cpu.md) |
| **CAP-05** | A manufacturable PCB | 4 | 11-19 mw | 15-23 mw | 11 | [capstones/CAP-05-manufacturable-pcb.md](capstones/CAP-05-manufacturable-pcb.md) |
| **CAP-06** | Course delivery and autograding at scale | 7 | 12-20 mw | 18-30 mw | 10 | [capstones/CAP-06-course-delivery-autograding.md](capstones/CAP-06-course-delivery-autograding.md) |
| **CAP-07** | Tape out a student design on a shuttle | 15 | 11.5-18 mw | 14-24 mw | 6 | [capstones/CAP-07-tapeout-shuttle.md](capstones/CAP-07-tapeout-shuttle.md) |
| **CAP-08** | Import and run a third-party core | 10 | 14-24 mw | 26-42 mw | 13 | [capstones/CAP-08-import-third-party-core.md](capstones/CAP-08-import-third-party-core.md) |
| **CAP-09** | Verify a design you did not write | 6 | 16-26 mw | 20-34 mw | 8 | [capstones/CAP-09-verify-a-design-you-did-not-write.md](capstones/CAP-09-verify-a-design-you-did-not-write.md) |
| **CAP-10** | Audio output | 11 | 2-3 mw (after | - | 4 | [capstones/CAP-10-audio-output.md](capstones/CAP-10-audio-output.md) |
| **CAP-11** | Audio input | 12 | 2-3 mw (after | - | 4 | [capstones/CAP-11-audio-input.md](capstones/CAP-11-audio-input.md) |
| **CAP-12** | A heart rate monitor | 13 | 11-16 mw | 26.5-38.5 mw (cumulative through the mixed-signal stage) | 6 | [capstones/CAP-12-heart-rate-monitor.md](capstones/CAP-12-heart-rate-monitor.md) |
| **CAP-13** | KiCad interoperation parity | 2 | 6-12 mw | 12-22 mw | 8 | [capstones/CAP-13-kicad-interoperation-parity.md](capstones/CAP-13-kicad-interoperation-parity.md) |
| **CAP-14** | ngspice interoperation parity | 14 | 10-16 mw | 37.5-55.5 mw (cumulative through the analog stage this | 7 | [capstones/CAP-14-ngspice-interoperation-parity.md](capstones/CAP-14-ngspice-interoperation-parity.md) |
| **CAP-15** | HDL toolchain parity (Yosys, Verilator, Icarus, GHDL) | 3 | 12-22 mw | 20-34 mw | 10 | [capstones/CAP-15-hdl-toolchain-parity.md](capstones/CAP-15-hdl-toolchain-parity.md) |
| **CAP-16** | Logisim-Evolution migration parity | 9 | 8-16 mw | 10-20 mw | 6 | [capstones/CAP-16-logisim-evolution-migration-parity.md](capstones/CAP-16-logisim-evolution-migration-parity.md) |
| **CAP-17** | Distributed execution for cluster and grid deployments | 18 | 38-62 mw | 62-98 mw | 10 | [capstones/CAP-17-distributed-execution.md](capstones/CAP-17-distributed-execution.md) |

CAP-17 carries priority 18 meaning "appended, not yet ranked": the maintainer
commissioned seventeen capstones and added it afterwards without ranking it.

---

## Index: features

| id | title | cost | owner | spine | required by | beneficial to | tasks | doc |
|---|---|---|---|---|---:|---:|---:|---|
| **FEAT-001** | Registry-keyed table totality discipline | 1-2 mw | P3 | S1 | 7 | 0 | 2 | [features/FEAT-001-registry-keyed-table-totality.md](features/FEAT-001-registry-keyed-table-totality.md) |
| **FEAT-002** | Fail-loud loader and attribute dispatch | 1-2 mw | UNOWNED | - | 4 | 0 | 4 | [features/FEAT-002-fail-loud-loader-attribute-dispatch.md](features/FEAT-002-fail-loud-loader-attribute-dispatch.md) |
| **FEAT-003** | Uncompressed canonical default with stable-id references | 2-4 mw | P11 | S6 | 4 | 2 | 2 | [features/FEAT-003-uncompressed-canonical-default-stable-id-refs.md](features/FEAT-003-uncompressed-canonical-default-stable-id-refs.md) |
| **FEAT-004** | Shared net-partition IR with stable net naming | 2-3 mw | P3 | S3, S5 | 7 | 3 | 2 | [features/FEAT-004-shared-net-partition-ir-stable-net-naming.md](features/FEAT-004-shared-net-partition-ir-stable-net-naming.md) |
| **FEAT-005** | Quadratic and materializing I/O paths eliminated | 2-3 mw | P1 | - | 5 | 1 | 3 | [features/FEAT-005-quadratic-and-materializing-io-paths.md](features/FEAT-005-quadratic-and-materializing-io-paths.md) |
| **FEAT-006** | Simulation capacity and long-run ergonomics | 3-5 mw | P2 | - | 6 | 0 | 4 | [features/FEAT-006-simulation-capacity-and-long-run-ergonomics.md](features/FEAT-006-simulation-capacity-and-long-run-ergonomics.md) |
| **FEAT-007** | CI long-run lanes, timeouts and cross-platform parity | 3-6 mw | UNOWNED | S14 | 6 | 1 | 4 | [features/FEAT-007-ci-long-run-lanes-timeouts-platform-parity.md](features/FEAT-007-ci-long-run-lanes-timeouts-platform-parity.md) |
| **FEAT-008** | `SimpleEditor` decomposition, a UI harness and a floored `jls.edit` | 12-20 mw | UNOWNED | - | 3 | 4 | 6 | [features/FEAT-008-simpleeditor-decomposition-ui-harness-floored-jls-edit.md](features/FEAT-008-simpleeditor-decomposition-ui-harness-floored-jls-edit.md) |
| **FEAT-009** | The measurement gate and a tracked calibration fixture | 5-10 mw | UNOWNED | S15 | 6 | 0 | 6 | [features/FEAT-009-measurement-gate-and-calibration-fixture.md](features/FEAT-009-measurement-gate-and-calibration-fixture.md) |
| **FEAT-010** | Deterministic native installers and file association | 8-16 mw | UNOWNED | - | 1 | 1 | 2 | [features/FEAT-010-deterministic-native-installers.md](features/FEAT-010-deterministic-native-installers.md) |
| **FEAT-011** | Accessibility, keyboard operability and onboarding | 6-10 mw | UNOWNED | - | 2 | 1 | 2 | [features/FEAT-011-accessibility-keyboard-operability-onboarding.md](features/FEAT-011-accessibility-keyboard-operability-onboarding.md) |
| **FEAT-012** | Semantic merge safety and per-kind merge rules | 9-13 mw | P11 | - | 1 | 2 | 3 | [features/FEAT-012-semantic-merge-safety-per-kind-merge-rules.md](features/FEAT-012-semantic-merge-safety-per-kind-merge-rules.md) |
| **FEAT-013** | Per-section internal versioning with must-understand semantics | 4-7 mw | P11 | S10 | 5 | 1 | 3 | [features/FEAT-013-per-section-internal-versioning.md](features/FEAT-013-per-section-internal-versioning.md) |
| **FEAT-014** | Stable addressing and per-view geometry in the shared model | 11-17 mw | P3 | S7, S11, S12, S18 | 5 | 3 | 2 | [features/FEAT-014-stable-addressing-and-per-view-geometry.md](features/FEAT-014-stable-addressing-and-per-view-geometry.md) |
| **FEAT-015** | The headless, programmatic `CircuitOp` layer | 4-7 mw | P12 | S4, S13 | 4 | 3 | 2 | [features/FEAT-015-headless-programmatic-circuitop-layer.md](features/FEAT-015-headless-programmatic-circuitop-layer.md) |
| **FEAT-016** | Subcircuit type identity, VLNV and the circuit-library format | 3-5 mw | P7 | - | 3 | 2 | 2 | [features/FEAT-016-subcircuit-type-identity-vlnv-library.md](features/FEAT-016-subcircuit-type-identity-vlnv-library.md) |
| **FEAT-017** | Shared and parameterized subcircuit definitions | 25-36 mw | P7 | - | 5 | 2 | 2 | [features/FEAT-017-shared-and-parameterized-subcircuit-definitions.md](features/FEAT-017-shared-and-parameterized-subcircuit-definitions.md) |
| **FEAT-018** | Hierarchical instance structure in the HDL IR | 4-6 mw | P3 | - | 5 | 0 | 2 | [features/FEAT-018-hierarchical-instance-structure-hdl-ir.md](features/FEAT-018-hierarchical-instance-structure-hdl-ir.md) |
| **FEAT-019** | Yosys JSON write | 3-4 mw | UNOWNED | - | 1 | 4 | 3 | [features/FEAT-019-yosys-json-write.md](features/FEAT-019-yosys-json-write.md) |
| **FEAT-020** | Yosys JSON read: mapper parity with the validator | 4-8 mw | P3 | - | 2 | 1 | 2 | [features/FEAT-020-yosys-json-read-mapper-parity.md](features/FEAT-020-yosys-json-read-mapper-parity.md) |
| **FEAT-021** | Bidirectional ports in the IR and the element vocabulary | 2-4 mw | P3 | - | 4 | 0 | 1 | [features/FEAT-021-bidirectional-ports.md](features/FEAT-021-bidirectional-ports.md) |
| **FEAT-022** | Schematic auto-layout for imported netlists | 4-8 mw | UNOWNED | - | 3 | 0 | 2 | [features/FEAT-022-schematic-auto-layout-imported-netlists.md](features/FEAT-022-schematic-auto-layout-imported-netlists.md) |
| **FEAT-023** | External toolchain differential oracle and the board on-ramp | 6-12 mw | P5 | - | 4 | 0 | 4 | [features/FEAT-023-external-toolchain-oracle-board-onramp.md](features/FEAT-023-external-toolchain-oracle-board-onramp.md) |
| **FEAT-024** | Black-box HDL component and external co-simulation | 8-14 mw | UNOWNED | - | 0 | 3 | 1 | [features/FEAT-024-black-box-hdl-component-cosimulation.md](features/FEAT-024-black-box-hdl-component-cosimulation.md) |
| **FEAT-025** | Logisim-Evolution `.circ` importer and migration report | 6-12 mw | UNOWNED | - | 1 | 2 | 2 | [features/FEAT-025-logisim-evolution-importer-migration-report.md](features/FEAT-025-logisim-evolution-importer-migration-report.md) |
| **FEAT-026** | The four-state value core with a resolution fold | 28-36 mw | P1 | S2 | 6 | 1 | 2 | [features/FEAT-026-four-state-value-core.md](features/FEAT-026-four-state-value-core.md) |
| **FEAT-027** | Strength lattice, driver kinds and net kinds | 6-9 mw | P1 | - | 4 | 0 | 4 | [features/FEAT-027-strength-lattice-driver-kinds-net-kinds.md](features/FEAT-027-strength-lattice-driver-kinds-net-kinds.md) |
| **FEAT-028** | Radix-parameterized value and port type system | 8-12 mw | P1 | - | 1 | 0 | 3 | [features/FEAT-028-radix-parameterized-value-and-port-types.md](features/FEAT-028-radix-parameterized-value-and-port-types.md) |
| **FEAT-029** | The N-ary element family and its interop | 9-13 mw | P2 | - | 1 | 0 | 5 | [features/FEAT-029-nary-element-family-and-interop.md](features/FEAT-029-nary-element-family-and-interop.md) |
| **FEAT-030** | Engine constant factors: the semantics-preserving stack | 12-20 mw | P1 | S24 | 3 | 3 | 5 | [features/FEAT-030-engine-constant-factors.md](features/FEAT-030-engine-constant-factors.md) |
| **FEAT-031** | The per-instance fidelity toggle and its boundary harness | 5-8 mw | P8 | S19 | 2 | 4 | 4 | [features/FEAT-031-per-instance-fidelity-toggle.md](features/FEAT-031-per-instance-fidelity-toggle.md) |
| **FEAT-032** | The host byte port, a Console element and transcripts | 10-16 mw | UNOWNED | S22 | 2 | 3 | 4 | [features/FEAT-032-host-byte-port-console-transcripts.md](features/FEAT-032-host-byte-port-console-transcripts.md) |
| **FEAT-033** | `jls.mach`, the reference runner and the guest software stack | 14-22 mw | UNOWNED | - | 2 | 1 | 2 | [features/FEAT-033-jls-mach-reference-runner-guest-stack.md](features/FEAT-033-jls-mach-reference-runner-guest-stack.md) |
| **FEAT-034** | Retirement-indexed parity harness and `RetireRecord` | 10-16 mw | P5 | S23 | 4 | 1 | 8 | [features/FEAT-034-retirement-indexed-parity-harness.md](features/FEAT-034-retirement-indexed-parity-harness.md) |
| **FEAT-035** | Checkpoint and simulation-state serialization | 10-17 mw | P9 | S25 | 2 | 3 | 4 | [features/FEAT-035-checkpoint-and-simulation-state-serialization.md](features/FEAT-035-checkpoint-and-simulation-state-serialization.md) |
| **FEAT-036** | Byte lanes on `Memory` and capacity as a byte budget | 3-7 mw | P2 | S20 | 3 | 0 | 3 | [features/FEAT-036-memory-byte-lanes-and-byte-budget.md](features/FEAT-036-memory-byte-lanes-and-byte-budget.md) |
| **FEAT-037** | Reset semantics, clock and domain architecture | 13-18 mw | P13 | - | 5 | 1 | 2 | [features/FEAT-037-reset-clock-and-domain-architecture.md](features/FEAT-037-reset-clock-and-domain-architecture.md) |
| **FEAT-038** | The drawn structural RV32 machine | 12-26 mw | UNOWNED | - | 1 | 2 | 3 | [features/FEAT-038-drawn-structural-rv32-machine.md](features/FEAT-038-drawn-structural-rv32-machine.md) |
| **FEAT-039** | JLS-T3: the ternary ISA, toolchain and drawn CPU | 18-30 mw | UNOWNED | - | 1 | 0 | 5 | [features/FEAT-039-jls-t3-ternary-isa-toolchain-and-cpu.md](features/FEAT-039-jls-t3-ternary-isa-toolchain-and-cpu.md) |
| **FEAT-040** | The package and pinout library as data | 4-8 mw | UNOWNED | S21 | 3 | 1 | 2 | [features/FEAT-040-package-and-pinout-library-as-data.md](features/FEAT-040-package-and-pinout-library-as-data.md) |
| **FEAT-041** | Packing, refdes, cascade and electrical loading checks | 5-8 mw | P5 | S9, S16, S17 | 3 | 0 | 5 | [features/FEAT-041-packing-refdes-cascade-loading-checks.md](features/FEAT-041-packing-refdes-cascade-loading-checks.md) |
| **FEAT-042** | KiCad and gEDA netlist emitters with a manufacturability gate | 5-10 mw | P3 | - | 2 | 1 | 4 | [features/FEAT-042-kicad-geda-emitters-and-manufacturability-gate.md](features/FEAT-042-kicad-geda-emitters-and-manufacturability-gate.md) |
| **FEAT-043** | The breadboard canvas and its physical-simulation binding | 9-15 mw | UNOWNED | - | 1 | 3 | 4 | [features/FEAT-043-breadboard-canvas-and-physical-binding.md](features/FEAT-043-breadboard-canvas-and-physical-binding.md) |
| **FEAT-044** | Tiny Tapeout wrapper and shuttle handoff | 11.5-18 mw | P6 | - | 1 | 1 | 3 | [features/FEAT-044-tiny-tapeout-wrapper-and-shuttle-handoff.md](features/FEAT-044-tiny-tapeout-wrapper-and-shuttle-handoff.md) |
| **FEAT-045** | Host audio sink and source without a solver | 5-7 mw | UNOWNED | - | 2 | 1 | 1 | [features/FEAT-045-host-audio-sink-and-source.md](features/FEAT-045-host-audio-sink-and-source.md) |
| **FEAT-046** | The analog solver core and its determinism gate | 17.5-26 mw | UNOWNED | - | 4 | 0 | 4 | [features/FEAT-046-analog-solver-core-and-determinism-gate.md](features/FEAT-046-analog-solver-core-and-determinism-gate.md) |
| **FEAT-047** | The physical time base and the nominal real-time scalar | 2-3 mw | P4 | - | 4 | 1 | 1 | [features/FEAT-047-physical-time-base-and-real-time-scalar.md](features/FEAT-047-physical-time-base-and-real-time-scalar.md) |
| **FEAT-048** | A2D/D2A bridge elements and A-STEP synchronization | 4-6 mw | UNOWNED | - | 4 | 0 | 1 | [features/FEAT-048-a2d-d2a-bridges-and-astep-synchronization.md](features/FEAT-048-a2d-d2a-bridges-and-astep-synchronization.md) |
| **FEAT-049** | Analog device models, the drawn palette and convergence hardening | 21-33 mw | UNOWNED | - | 2 | 1 | 3 | [features/FEAT-049-analog-device-models-palette-convergence.md](features/FEAT-049-analog-device-models-palette-convergence.md) |
| **FEAT-050** | Module runtime consumed: extension points and providers | 5-10 mw | P12 | S8 | 0 | 5 | 2 | [features/FEAT-050-module-runtime-consumed.md](features/FEAT-050-module-runtime-consumed.md) |
| **FEAT-051** | P2P session foundation and shared session v1 | 12-18 mw | UNOWNED | - | 1 | 1 | 2 | [features/FEAT-051-p2p-session-foundation-and-shared-session.md](features/FEAT-051-p2p-session-foundation-and-shared-session.md) |
| **FEAT-052** | CRDT replication, collaborative undo and security hardening | 14-22 mw | P11 | - | 1 | 1 | 4 | [features/FEAT-052-crdt-replication-undo-and-hardening.md](features/FEAT-052-crdt-replication-undo-and-hardening.md) |
| **FEAT-053** | Test-vector front end and autograding at scale | 9-15 mw | P5 | - | 3 | 1 | 4 | [features/FEAT-053-test-vector-front-end-and-autograding.md](features/FEAT-053-test-vector-front-end-and-autograding.md) |
| **FEAT-054** | Flat, compact element representation | 12-20 mw | UNOWNED | - | 1 | 0 | 0 | [features/FEAT-054-flat-element-representation.md](features/FEAT-054-flat-element-representation.md) |
| **FEAT-055** | Partitioned model and streaming elaboration | 10-16 mw | UNOWNED | - | 1 | 0 | 0 | [features/FEAT-055-partitioned-model-and-streaming-elaboration.md](features/FEAT-055-partitioned-model-and-streaming-elaboration.md) |
| **FEAT-056** | Distributed simulation transport and barrier protocol | 10-18 mw | UNOWNED | - | 1 | 0 | 0 | [features/FEAT-056-distributed-simulation-transport-and-barrier.md](features/FEAT-056-distributed-simulation-transport-and-barrier.md) |
| **FEAT-057** | Campaign execution and artifact aggregation | 6-8 mw | UNOWNED | - | 1 | 0 | 0 | [features/FEAT-057-campaign-execution-and-artifact-aggregation.md](features/FEAT-057-campaign-execution-and-artifact-aggregation.md) |

*Owner* is a committed capability-roadmap program (`P1`-`P13`) or `UNOWNED`.
`UNOWNED` is information, not an objection: it means the committed roadmap does
not pay for this feature and someone must decide who does. Twenty-five of the
fifty-seven are unowned, and they are not the cheap ones.

---

## Index: tasks

| id | title | cost | blocked by | enables | doc |
|---|---|---|---|---:|---|
| **TASK-0001** | Audit and pin every registry-keyed table | 1.5 wk | none | FEAT-001, FEAT-002 | [tasks/TASK-0001-audit-registry-keyed-tables.md](tasks/TASK-0001-audit-registry-keyed-tables.md) |
| **TASK-0002** | Registry totality lint as a standing build rule | 3 d | TASK-0001 | FEAT-001 | [tasks/TASK-0002-registry-totality-lint.md](tasks/TASK-0002-registry-totality-lint.md) |
| **TASK-0003** | Make attribute dispatch total and the loader check it | 1 wk | none | FEAT-002 | [tasks/TASK-0003-total-attribute-dispatch.md](tasks/TASK-0003-total-attribute-dispatch.md) |
| **TASK-0004** | Silent-data-loss regression corpus | 2 d | TASK-0003 | FEAT-002 | [tasks/TASK-0004-silent-data-loss-corpus.md](tasks/TASK-0004-silent-data-loss-corpus.md) |
| **TASK-0005** | Reference elements by stable id, with a diff ratchet | 2 wk | none | FEAT-003, FEAT-012 | [tasks/TASK-0005-reference-elements-by-stable-id.md](tasks/TASK-0005-reference-elements-by-stable-id.md) |
| **TASK-0006** | Plain text as the default container, with the autosave policy | 1 wk | none | FEAT-003 | [tasks/TASK-0006-plain-text-default-container.md](tasks/TASK-0006-plain-text-default-container.md) |
| **TASK-0007** | Extract the net-partition walk into its own package | 1.5 wk | none | FEAT-004 | [tasks/TASK-0007-extract-net-partition-walk.md](tasks/TASK-0007-extract-net-partition-walk.md) |
| **TASK-0008** | Key net and probe names off stable id, and validate them | 1.5 wk | TASK-0005 | FEAT-004, FEAT-005 | [tasks/TASK-0008-net-and-probe-names-off-stable-id.md](tasks/TASK-0008-net-and-probe-names-off-stable-id.md) |
| **TASK-0009** | De-quadratic the stimulus parse and the load fixup | 1.5 wk | none | FEAT-005 | [tasks/TASK-0009-dequadratic-stimulus-parse-and-load-fixup.md](tasks/TASK-0009-dequadratic-stimulus-parse-and-load-fixup.md) |
| **TASK-0010** | Stream the waveform dump instead of materializing it | 4 d | none | FEAT-005 | [tasks/TASK-0010-stream-the-waveform-dump.md](tasks/TASK-0010-stream-the-waveform-dump.md) |
| **TASK-0011** | Adjudicate and fix the past-limit event drop | 3 d | none | FEAT-006 | [tasks/TASK-0011-past-limit-event-drop.md](tasks/TASK-0011-past-limit-event-drop.md) |
| **TASK-0012** | Unbounded run duration | 2 d | none | FEAT-006 | [tasks/TASK-0012-unbounded-run-duration.md](tasks/TASK-0012-unbounded-run-duration.md) |
| **TASK-0013** | Memory capacity as a byte budget, initialized copy-on-write | 1.5 wk | none | FEAT-006, FEAT-036 | [tasks/TASK-0013-memory-byte-budget-copy-on-write.md](tasks/TASK-0013-memory-byte-budget-copy-on-write.md) |
| **TASK-0014** | Long-lived batch mode with pause, heartbeat and clean interrupt | 1.5 wk | none | FEAT-006, FEAT-035 | [tasks/TASK-0014-long-lived-batch-mode.md](tasks/TASK-0014-long-lived-batch-mode.md) |
| **TASK-0015** | Explicit timeouts on every workflow job | 1 d | none | FEAT-007 | [tasks/TASK-0015-explicit-workflow-timeouts.md](tasks/TASK-0015-explicit-workflow-timeouts.md) |
| **TASK-0016** | Split CI into a required fast lane and a long-run lane, with a fixture policy | 1.5 wk | TASK-0015 | FEAT-007, FEAT-009 | [tasks/TASK-0016-fast-lane-long-run-lane-fixture-policy.md](tasks/TASK-0016-fast-lane-long-run-lane-fixture-policy.md) |
| **TASK-0017** | Promote the macOS and Windows headless lanes to required | 2 wk | TASK-0015 | FEAT-007 | [tasks/TASK-0017-promote-macos-and-windows-lanes.md](tasks/TASK-0017-promote-macos-and-windows-lanes.md) |
| **TASK-0018** | Wayland GUI rig first light | 1 wk | none | FEAT-007, FEAT-008 | [tasks/TASK-0018-wayland-gui-rig-first-light.md](tasks/TASK-0018-wayland-gui-rig-first-light.md) |
| **TASK-0019** | The editor decomposition plan and its coverage floor | 1.5 wk | none | FEAT-008 | [tasks/TASK-0019-editor-decomposition-plan-and-floor.md](tasks/TASK-0019-editor-decomposition-plan-and-floor.md) |
| **TASK-0020** | Extract the mouse machine and replace the source-identity dispatcher | 2 wk | TASK-0019 | FEAT-008 | [tasks/TASK-0020-extract-mouse-machine-and-dispatcher.md](tasks/TASK-0020-extract-mouse-machine-and-dispatcher.md) |
| **TASK-0021** | The UI test harness, including dialog construction | 2 wk | none | FEAT-008, FEAT-053 | [tasks/TASK-0021-ui-test-harness-and-dialog-construction.md](tasks/TASK-0021-ui-test-harness-and-dialog-construction.md) |
| **TASK-0022** | Measure the per-cycle active fraction, CPI and the calibration constant | 1.5 wk | none | FEAT-009 | [tasks/TASK-0022-measure-active-fraction-cpi-calibration-constant.md](tasks/TASK-0022-measure-active-fraction-cpi-calibration-constant.md) |
| **TASK-0023** | Measure the behavioral binding and the levelized cost at scale | 1.5 wk | TASK-0022 | FEAT-009, FEAT-030, FEAT-031 | [tasks/TASK-0023-behavioral-binding-and-levelized-cost.md](tasks/TASK-0023-behavioral-binding-and-levelized-cost.md) |
| **TASK-0024** | Write the machine-calibration document | 1 wk | TASK-0022, TASK-0023, TASK-0025 | FEAT-009 | [tasks/TASK-0024-machine-calibration-document.md](tasks/TASK-0024-machine-calibration-document.md) |
| **TASK-0025** | Commit the tracked calibration fixture, re-home the goldens, delete `riscv/` | 2 wk | TASK-0022, TASK-0023 | FEAT-009 | [tasks/TASK-0025-calibration-fixture-and-riscv-deletion.md](tasks/TASK-0025-calibration-fixture-and-riscv-deletion.md) |
| **TASK-0026** | The simulation budget and allocation ratchet | 1 wk | TASK-0022, TASK-0023, TASK-0025 | FEAT-009, FEAT-030 | [tasks/TASK-0026-simulation-budget-and-allocation-ratchet.md](tasks/TASK-0026-simulation-budget-and-allocation-ratchet.md) |
| **TASK-0027** | Native installers per OS with file association | 2 wk | none | FEAT-010 | [tasks/TASK-0027-native-installers-and-file-association.md](tasks/TASK-0027-native-installers-and-file-association.md) |
| **TASK-0028** | Installer reproducibility, independent rebuild and signing | 2 wk | none | FEAT-010 | [tasks/TASK-0028-installer-reproducibility-rebuild-and-signing.md](tasks/TASK-0028-installer-reproducibility-rebuild-and-signing.md) |
| **TASK-0029** | Keyboard operability | 2 wk | none | FEAT-011 | [tasks/TASK-0029-keyboard-operability.md](tasks/TASK-0029-keyboard-operability.md) |
| **TASK-0030** | Visual ergonomics and first-run onboarding | 2 wk | none | FEAT-011 | [tasks/TASK-0030-visual-ergonomics-and-onboarding.md](tasks/TASK-0030-visual-ergonomics-and-onboarding.md) |
| **TASK-0031** | Semantic validation of a merged file | 1.5 wk | TASK-0005 | FEAT-012 | [tasks/TASK-0031-semantic-validation-of-a-merged-file.md](tasks/TASK-0031-semantic-validation-of-a-merged-file.md) |
| **TASK-0032** | The per-record-kind merge rule table | 2 wk | TASK-0005, TASK-0031 | FEAT-012, FEAT-052 | [tasks/TASK-0032-per-record-kind-merge-rule-table.md](tasks/TASK-0032-per-record-kind-merge-rule-table.md) |
| **TASK-0033** | Section framing, must-understand flags and the epoch policy | 2 wk | TASK-0005 | FEAT-013 | [tasks/TASK-0033-section-framing-must-understand-and-epoch-policy.md](tasks/TASK-0033-section-framing-must-understand-and-epoch-policy.md) |
| **TASK-0034** | The raw bulk-image section | 1.5 wk | TASK-0033 | FEAT-013, FEAT-036 | [tasks/TASK-0034-raw-bulk-image-section.md](tasks/TASK-0034-raw-bulk-image-section.md) |
| **TASK-0035** | Stable identity for instances, nets and groups | 2 wk | TASK-0007 | FEAT-014 | [tasks/TASK-0035-stable-identity-instances-nets-groups.md](tasks/TASK-0035-stable-identity-instances-nets-groups.md) |
| **TASK-0036** | Per-view geometry section and the op view discriminator | 2 wk | TASK-0033, TASK-0035 | FEAT-014, FEAT-043 | [tasks/TASK-0036-per-view-geometry-and-op-view-discriminator.md](tasks/TASK-0036-per-view-geometry-and-op-view-discriminator.md) |
| **TASK-0037** | Headless op application and the complete op vocabulary | 2 wk | none | FEAT-015, FEAT-052 | [tasks/TASK-0037-headless-op-application-and-vocabulary.md](tasks/TASK-0037-headless-op-application-and-vocabulary.md) |
| **TASK-0038** | Programmatic circuit construction verbs | 2 wk | TASK-0037 | FEAT-015, FEAT-038, FEAT-039 | [tasks/TASK-0038-programmatic-circuit-construction-verbs.md](tasks/TASK-0038-programmatic-circuit-construction-verbs.md) |
| **TASK-0039** | Definition identity: structural digest and version strings | 2 wk | none | FEAT-016 | [tasks/TASK-0039-definition-identity-digest-and-versions.md](tasks/TASK-0039-definition-identity-digest-and-versions.md) |
| **TASK-0040** | The circuit-library container and provenance | 2 wk | TASK-0033, TASK-0039 | FEAT-016 | [tasks/TASK-0040-circuit-library-container-and-provenance.md](tasks/TASK-0040-circuit-library-container-and-provenance.md) |
| **TASK-0041** | Definition/instance split with parameters | 2 wk | TASK-0039 | FEAT-017 | [tasks/TASK-0041-definition-instance-split-with-parameters.md](tasks/TASK-0041-definition-instance-split-with-parameters.md) |
| **TASK-0042** | The elaboration pass and its diagnostics | 2 wk | TASK-0041 | FEAT-017 | [tasks/TASK-0042-elaboration-pass-and-diagnostics.md](tasks/TASK-0042-elaboration-pass-and-diagnostics.md) |
| **TASK-0043** | Module instantiation and the hierarchy walk | 2 wk | none | FEAT-018 | [tasks/TASK-0043-module-instantiation-and-hierarchy-walk.md](tasks/TASK-0043-module-instantiation-and-hierarchy-walk.md) |
| **TASK-0044** | Hierarchical emitters and their goldens | 1.5 wk | TASK-0043 | FEAT-018, FEAT-023 | [tasks/TASK-0044-hierarchical-emitters-and-goldens.md](tasks/TASK-0044-hierarchical-emitters-and-goldens.md) |
| **TASK-0045** | The synthesis-tool netlist writer | 2 wk | none | FEAT-019 | [tasks/TASK-0045-synthesis-tool-netlist-writer.md](tasks/TASK-0045-synthesis-tool-netlist-writer.md) |
| **TASK-0046** | Document the tool-mediated netlist paths | 3 d | TASK-0045 | FEAT-019 | [tasks/TASK-0046-document-tool-mediated-netlist-paths.md](tasks/TASK-0046-document-tool-mediated-netlist-paths.md) |
| **TASK-0047** | Realize sequential, memory and arithmetic cells on import | 2 wk | none | FEAT-020 | [tasks/TASK-0047-realize-sequential-memory-arithmetic-cells.md](tasks/TASK-0047-realize-sequential-memory-arithmetic-cells.md) |
| **TASK-0048** | Realize hierarchy instances on import | 1.5 wk | none | FEAT-020, FEAT-022 | [tasks/TASK-0048-realize-hierarchy-instances-on-import.md](tasks/TASK-0048-realize-hierarchy-instances-on-import.md) |
| **TASK-0049** | Bidirectional ports end to end | 2 wk | none | FEAT-021, FEAT-027 | [tasks/TASK-0049-bidirectional-ports-end-to-end.md](tasks/TASK-0049-bidirectional-ports-end-to-end.md) |
| **TASK-0050** | Heuristic layered layout for imported netlists | 2 wk | TASK-0047 | FEAT-022 | [tasks/TASK-0050-heuristic-layered-layout-for-imports.md](tasks/TASK-0050-heuristic-layered-layout-for-imports.md) |
| **TASK-0051** | Arm the external toolchains in CI | 1 wk | none | FEAT-023 | [tasks/TASK-0051-arm-external-toolchains-in-ci.md](tasks/TASK-0051-arm-external-toolchains-in-ci.md) |
| **TASK-0052** | Per-board constraints and one real flash | 2 wk | TASK-0051 | FEAT-023, FEAT-044 | [tasks/TASK-0052-per-board-constraints-and-one-real-flash.md](tasks/TASK-0052-per-board-constraints-and-one-real-flash.md) |
| **TASK-0053** | Black-box HDL component and its co-simulation contract | 2 wk | none | FEAT-024 | [tasks/TASK-0053-black-box-hdl-component-cosimulation.md](tasks/TASK-0053-black-box-hdl-component-cosimulation.md) |
| **TASK-0054** | The foreign-tool reader and its migration report | 2 wk | TASK-0003 | FEAT-002, FEAT-025 | [tasks/TASK-0054-foreign-tool-reader-and-migration-report.md](tasks/TASK-0054-foreign-tool-reader-and-migration-report.md) |
| **TASK-0055** | Absorb the through-hole part data | 2 wk | TASK-0085 | FEAT-025, FEAT-040 | [tasks/TASK-0055-absorb-through-hole-part-data.md](tasks/TASK-0055-absorb-through-hole-part-data.md) |
| **TASK-0056** | Widen the value permits and migrate the value representation | 2 wk | none | FEAT-026, FEAT-028, FEAT-030 | [tasks/TASK-0056-widen-value-permits-and-migrate-representation.md](tasks/TASK-0056-widen-value-permits-and-migrate-representation.md) |
| **TASK-0057** | The resolution fold | 2 wk | TASK-0056 | FEAT-026, FEAT-027 | [tasks/TASK-0057-the-resolution-fold.md](tasks/TASK-0057-the-resolution-fold.md) |
| **TASK-0058** | Strength lattice and pull elements | 2 wk | TASK-0057 | FEAT-027 | [tasks/TASK-0058-strength-lattice-and-pull-elements.md](tasks/TASK-0058-strength-lattice-and-pull-elements.md) |
| **TASK-0059** | Radix on ports and nets, validated not widened | 1.5 wk | TASK-0056 | FEAT-028 | [tasks/TASK-0059-radix-on-ports-and-nets-validated-not-widened.md](tasks/TASK-0059-radix-on-ports-and-nets-validated-not-widened.md) |
| **TASK-0060** | The higher-radix operator kernel | 2 wk | TASK-0056 | FEAT-028, FEAT-029 | [tasks/TASK-0060-the-higher-radix-operator-kernel.md](tasks/TASK-0060-the-higher-radix-operator-kernel.md) |
| **TASK-0061** | The N-ary element family | 2 wk | TASK-0059, TASK-0060 | FEAT-029 | [tasks/TASK-0061-the-n-ary-element-family.md](tasks/TASK-0061-the-n-ary-element-family.md) |
| **TASK-0062** | N-ary interop: lowering, waveform manifest and test grammar | 2 wk | TASK-0061 | FEAT-029 | [tasks/TASK-0062-n-ary-interop-lowering-manifest-and-test-grammar.md](tasks/TASK-0062-n-ary-interop-lowering-manifest-and-test-grammar.md) |
| **TASK-0063** | Calendar queue with an intrusive queued flag | 2 wk | TASK-0011 | FEAT-030 | [tasks/TASK-0063-calendar-queue-with-an-intrusive-queued-flag.md](tasks/TASK-0063-calendar-queue-with-an-intrusive-queued-flag.md) |
| **TASK-0064** | Zero-delay closure | 2 wk | TASK-0063 | FEAT-030 | [tasks/TASK-0064-zero-delay-closure.md](tasks/TASK-0064-zero-delay-closure.md) |
| **TASK-0065** | The saved per-instance fidelity attribute | 1.5 wk | none | FEAT-031 | [tasks/TASK-0065-the-saved-per-instance-fidelity-attribute.md](tasks/TASK-0065-the-saved-per-instance-fidelity-attribute.md) |
| **TASK-0066** | The boundary handover harness | 2 wk | TASK-0065 | FEAT-031, FEAT-035 | [tasks/TASK-0066-the-boundary-handover-harness.md](tasks/TASK-0066-the-boundary-handover-harness.md) |
| **TASK-0067** | The host byte port seam | 2 wk | none | FEAT-032 | [tasks/TASK-0067-host-byte-port-seam.md](tasks/TASK-0067-host-byte-port-seam.md) |
| **TASK-0068** | The console element | 2 wk | TASK-0067 | FEAT-032 | [tasks/TASK-0068-the-console-element.md](tasks/TASK-0068-the-console-element.md) |
| **TASK-0069** | Transcript capture, replay and the console pane | 2 wk | TASK-0068 | FEAT-008, FEAT-032, FEAT-034 | [tasks/TASK-0069-transcript-capture-replay-console-pane.md](tasks/TASK-0069-transcript-capture-replay-console-pane.md) |
| **TASK-0070** | The machine package and its reference runner | 2 wk | none | FEAT-033, FEAT-034 | [tasks/TASK-0070-machine-package-and-reference-runner.md](tasks/TASK-0070-machine-package-and-reference-runner.md) |
| **TASK-0071** | Guest image build, pinning and residence | 2 wk | none | FEAT-013, FEAT-033 | [tasks/TASK-0071-guest-image-build-pinning-residence.md](tasks/TASK-0071-guest-image-build-pinning-residence.md) |
| **TASK-0072** | The retirement record and its trace emission | 2 wk | none | FEAT-034 | [tasks/TASK-0072-retirement-record-and-trace-emission.md](tasks/TASK-0072-retirement-record-and-trace-emission.md) |
| **TASK-0073** | The differential comparator, exclusion set and sync points | 2 wk | TASK-0072 | FEAT-034, FEAT-053 | [tasks/TASK-0073-differential-comparator-exclusion-set-sync-points.md](tasks/TASK-0073-differential-comparator-exclusion-set-sync-points.md) |
| **TASK-0074** | Serialize the queue, the clock and stateful element contents | 2 wk | TASK-0011, TASK-0033 | FEAT-035 | [tasks/TASK-0074-serialize-queue-clock-and-element-state.md](tasks/TASK-0074-serialize-queue-clock-and-element-state.md) |
| **TASK-0075** | Checkpoint round-trip equivalence test | 1 wk | TASK-0074 | FEAT-035 | [tasks/TASK-0075-checkpoint-round-trip-equivalence-test.md](tasks/TASK-0075-checkpoint-round-trip-equivalence-test.md) |
| **TASK-0076** | Write-mask input on memory | 1.5 wk | none | FEAT-036 | [tasks/TASK-0076-write-mask-input-on-memory.md](tasks/TASK-0076-write-mask-input-on-memory.md) |
| **TASK-0077** | Honest reset on the register element | 1.5 wk | none | FEAT-037 | [tasks/TASK-0077-honest-reset-on-the-register-element.md](tasks/TASK-0077-honest-reset-on-the-register-element.md) |
| **TASK-0078** | Clock domains and crossing checks | 2 wk | TASK-0007, TASK-0077 | FEAT-037, FEAT-041 | [tasks/TASK-0078-clock-domains-and-crossing-checks.md](tasks/TASK-0078-clock-domains-and-crossing-checks.md) |
| **TASK-0079** | Draw the machine and bring it up boundary by boundary | 2 wk | TASK-0038, TASK-0065, TASK-0070, TASK-0076 | FEAT-031, FEAT-038 | [tasks/TASK-0079-draw-the-machine-boundary-by-boundary.md](tasks/TASK-0079-draw-the-machine-boundary-by-boundary.md) |
| **TASK-0080** | The headless boot run and its transcript comparison | 2 wk | TASK-0016, TASK-0069, TASK-0071, TASK-0079 | FEAT-034, FEAT-038 | [tasks/TASK-0080-headless-boot-run-and-transcript-comparison.md](tasks/TASK-0080-headless-boot-run-and-transcript-comparison.md) |
| **TASK-0081** | Specify the ternary ISA and its conformance corpus | 2 wk | none | FEAT-039 | [tasks/TASK-0081-specify-the-ternary-isa-and-conformance-corpus.md](tasks/TASK-0081-specify-the-ternary-isa-and-conformance-corpus.md) |
| **TASK-0082** | The ternary reference emulator and assembler | 2 wk | TASK-0070, TASK-0081 | FEAT-034, FEAT-039 | [tasks/TASK-0082-ternary-reference-emulator-and-assembler.md](tasks/TASK-0082-ternary-reference-emulator-and-assembler.md) |
| **TASK-0083** | Draw the ternary CPU | 2 wk | TASK-0038, TASK-0061, TASK-0082 | FEAT-029, FEAT-039 | [tasks/TASK-0083-draw-the-ternary-cpu.md](tasks/TASK-0083-draw-the-ternary-cpu.md) |
| **TASK-0084** | The monitor program | 2 wk | TASK-0068, TASK-0082, TASK-0083 | FEAT-032, FEAT-039 | [tasks/TASK-0084-the-monitor-program.md](tasks/TASK-0084-the-monitor-program.md) |
| **TASK-0085** | The package data schema and footprint binding | 2 wk | none | FEAT-040, FEAT-042 | [tasks/TASK-0085-package-data-schema-and-footprint-binding.md](tasks/TASK-0085-package-data-schema-and-footprint-binding.md) |
| **TASK-0086** | Packing, refdes, BOM and wiring list | 2 wk | TASK-0007, TASK-0008, TASK-0085 | FEAT-041 | [tasks/TASK-0086-packing-refdes-bom-and-wiring-list.md](tasks/TASK-0086-packing-refdes-bom-and-wiring-list.md) |
| **TASK-0087** | Width decomposition and the cascade rule | 2 wk | TASK-0007, TASK-0085 | FEAT-041 | [tasks/TASK-0087-width-decomposition-and-cascade-rule.md](tasks/TASK-0087-width-decomposition-and-cascade-rule.md) |
| **TASK-0088** | Fan-out and DC loading check | 1.5 wk | TASK-0085, TASK-0086, TASK-0087 | FEAT-041 | [tasks/TASK-0088-fan-out-and-dc-loading-check.md](tasks/TASK-0088-fan-out-and-dc-loading-check.md) |
| **TASK-0089** | The PCB-tool netlist emitter | 1.5 wk | TASK-0007, TASK-0085, TASK-0086, TASK-0087 | FEAT-042 | [tasks/TASK-0089-pcb-tool-netlist-emitter.md](tasks/TASK-0089-pcb-tool-netlist-emitter.md) |
| **TASK-0090** | The open-schematic emitter | 2 wk | TASK-0007 | FEAT-042 | [tasks/TASK-0090-open-schematic-emitter.md](tasks/TASK-0090-open-schematic-emitter.md) |
| **TASK-0091** | The manufacturability gate | 1.5 wk | TASK-0085, TASK-0086, TASK-0088 | FEAT-042 | [tasks/TASK-0091-manufacturability-gate.md](tasks/TASK-0091-manufacturability-gate.md) |
| **TASK-0092** | The breadboard canvas | 2 wk | TASK-0021, TASK-0036 | FEAT-043 | [tasks/TASK-0092-breadboard-canvas.md](tasks/TASK-0092-breadboard-canvas.md) |
| **TASK-0093** | Breadboard consistency check and physical binding | 2 wk | TASK-0058, TASK-0086, TASK-0092 | FEAT-027, FEAT-041, FEAT-043 | [tasks/TASK-0093-breadboard-consistency-and-physical-binding.md](tasks/TASK-0093-breadboard-consistency-and-physical-binding.md) |
| **TASK-0094** | The shuttle wrapper and its metadata | 2 wk | TASK-0049, TASK-0077 | FEAT-044 | [tasks/TASK-0094-shuttle-wrapper-and-metadata.md](tasks/TASK-0094-shuttle-wrapper-and-metadata.md) |
| **TASK-0095** | The shuttle submission path, documented and walked | 2 wk | TASK-0094 | FEAT-044 | [tasks/TASK-0095-shuttle-submission-path.md](tasks/TASK-0095-shuttle-submission-path.md) |
| **TASK-0096** | Host audio sink and source | 2 wk | none | FEAT-045 | [tasks/TASK-0096-host-audio-sink-and-source.md](tasks/TASK-0096-host-audio-sink-and-source.md) |
| **TASK-0097** | Solver core and timestep control | 2 wk | none | FEAT-046 | [tasks/TASK-0097-solver-core-and-timestep-control.md](tasks/TASK-0097-solver-core-and-timestep-control.md) |
| **TASK-0098** | The analog determinism controls | 2 wk | TASK-0097 | FEAT-046 | [tasks/TASK-0098-analog-determinism-controls.md](tasks/TASK-0098-analog-determinism-controls.md) |
| **TASK-0099** | Controlled sources, waveforms and model cards | 2 wk | TASK-0097 | FEAT-046 | [tasks/TASK-0099-controlled-sources-waveforms-model-cards.md](tasks/TASK-0099-controlled-sources-waveforms-model-cards.md) |
| **TASK-0100** | The external-simulator differential corpus | 2 wk | TASK-0051, TASK-0098, TASK-0099 | FEAT-023, FEAT-046 | [tasks/TASK-0100-external-simulator-differential-corpus.md](tasks/TASK-0100-external-simulator-differential-corpus.md) |
| **TASK-0101** | The nominal real-time scalar | 1.5 wk | none | FEAT-047 | [tasks/TASK-0101-nominal-real-time-scalar.md](tasks/TASK-0101-nominal-real-time-scalar.md) |
| **TASK-0102** | Bridge elements and the synchronization protocol | 2 wk | TASK-0097, TASK-0105 | FEAT-048 | [tasks/TASK-0102-bridge-elements-and-synchronization.md](tasks/TASK-0102-bridge-elements-and-synchronization.md) |
| **TASK-0103** | Device and transistor models | 2 wk | TASK-0097, TASK-0099 | FEAT-049 | [tasks/TASK-0103-device-and-transistor-models.md](tasks/TASK-0103-device-and-transistor-models.md) |
| **TASK-0104** | Convergence hardening | 2 wk | TASK-0097, TASK-0099 | FEAT-049 | [tasks/TASK-0104-convergence-hardening.md](tasks/TASK-0104-convergence-hardening.md) |
| **TASK-0105** | Per-view palettes and the analog palette | 2 wk | TASK-0036 | FEAT-008, FEAT-029, FEAT-043, FEAT-049 | [tasks/TASK-0105-per-view-palettes-and-analog-palette.md](tasks/TASK-0105-per-view-palettes-and-analog-palette.md) |
| **TASK-0106** | Consume the module registry for dispatch, with a typed catalog | 2 wk | none | FEAT-050 | [tasks/TASK-0106-consume-module-registry-for-dispatch.md](tasks/TASK-0106-consume-module-registry-for-dispatch.md) |
| **TASK-0107** | The element-provider discovery path | 2 wk | TASK-0106 | FEAT-050 | [tasks/TASK-0107-element-provider-discovery-path.md](tasks/TASK-0107-element-provider-discovery-path.md) |
| **TASK-0108** | Session foundation: identity, transport, membership and sync | 2 wk | none | FEAT-051 | [tasks/TASK-0108-session-foundation.md](tasks/TASK-0108-session-foundation.md) |
| **TASK-0109** | The replica loop over a loopback transport | 2 wk | TASK-0037, TASK-0108 | FEAT-051, FEAT-052 | [tasks/TASK-0109-replica-loop-over-loopback.md](tasks/TASK-0109-replica-loop-over-loopback.md) |
| **TASK-0110** | Convergent replication, collaborative undo and input hardening | 2 wk | TASK-0032, TASK-0109 | FEAT-052 | [tasks/TASK-0110-convergent-replication-undo-and-hardening.md](tasks/TASK-0110-convergent-replication-undo-and-hardening.md) |
| **TASK-0111** | The test panel, the grading harness and its reports | 2 wk | TASK-0021 | FEAT-019, FEAT-034, FEAT-053 | [tasks/TASK-0111-test-panel-grading-harness-and-reports.md](tasks/TASK-0111-test-panel-grading-harness-and-reports.md) |
| **TASK-0112** | Property checking, equivalence and coverage over an unfamiliar design | 2 wk | TASK-0111 | FEAT-034, FEAT-053 | [tasks/TASK-0112-property-equivalence-and-coverage.md](tasks/TASK-0112-property-equivalence-and-coverage.md) |

---

## The templates

Every document at a level has the same sections in the same order, so two
documents of one level can be diffed against each other. A document that does not
match its template is a defect.

**Capstone** - `capstones/CAP-NN-slug.md`

```
# CAP-NN - <title>
**Status:** proposed | **Priority:** <n> | **Marginal cost:** <band> |
**Standalone cost:** <band>
## Outcome              one sentence: what exists that does not exist now
## Acceptance test      SEEN: what a person observes. CHECK: the named test
## Demo slice           the smallest version that demonstrates it, with its cost
## Prerequisite features   FEAT-NNN | title | why THIS capstone needs it | required/beneficial
## Related GitHub issues   # | title | closes / depends on / overlaps / informs
## Open decisions       each with a recommendation and the reason
## Kill criteria        numeric conditions that should stop this capstone
## Evidence             file:line anchors, doc references, measured figures
```

**Feature** - `features/FEAT-NNN-slug.md`

```
# FEAT-NNN - <title>
**Status:** proposed | **Cost:** <band> | **Owner program:** <P1-P13 or UNOWNED> |
**Spine rank:** <Sn or ->
## Capability delivered   what becomes possible, one paragraph, no implementation
## Consumed by capstones  CAP-NN | required/beneficial | what it needs from this feature
## Prerequisite features  FEAT-NNN | why (a REAL dependency)
## Prerequisite tasks     TASK-NNNN | title | why
## Acceptance criteria    checkable statements
## Related GitHub issues
## Design notes           only what a task author needs and cannot derive
## Risks
## Evidence
```

**Task** - `tasks/TASK-NNNN-slug.md`

```
# TASK-NNNN - <title>
**Status:** proposed | **Cost:** <days or weeks> | **Blocked by:** <TASK ids or none>
## Deliverable         precisely what changes: files, types, methods, tests
## Enables features    FEAT-NNN | what this unblocks
## Prerequisite tasks
## Acceptance test     the specific named test and what it asserts
## Related GitHub issues
## Notes               traps, prior-art anchors, exact call sites
## Evidence
```

Two conventions a reader should know. A feature's *Prerequisite tasks* table
lists **every task whose *Enables features* names it**, including tasks that
exercise the feature rather than precede it; those rows say "Consuming task".
And where no GitHub issue exists, the issue table carries an explicit **no
issue** row rather than a blank field, because the gap is information.

---

## The purchase order

What to do first, and why. This is a recommendation with stated reasons, not a
schedule and not a commitment.

**1. CAP-00's demo slice - about 2.5 maintainer-weeks.** TASK-0002 (3 d),
TASK-0004 (2 d), TASK-0015 (1 d), TASK-0011 (3 d), TASK-0012 (2 d). Five short
tasks that install four standing ratchets - registry totality, a
silent-data-loss corpus, workflow timeouts, and an adjudicated event-drop - and
need nothing from any other feature. **Why first:** each one is a check that can
never silently regress again, and together they make everything after them
measurable. Nothing else in the plan has this ratio.

**2. FEAT-009, the measurement gate - 5-10 mw.** **Why second:** six capstones
require it and every wall-clock estimate in this plan divides by constants it
establishes. Until it lands, every band downstream of it is a projection with a
stated basis rather than a measurement - including the ones this plan prints most
confidently. It is also what unblocks deleting `riscv/`.

**3. The rest of CAP-00's defect closure - FEAT-001, 002, 005, 006, 007, about
10-18 mw.** **Why third:** the maintainer named deferred maintenance the highest
priority, these five are pure defect closure, and every one of them is a tax the
other seventeen capstones pay repeatedly if it is not paid once. FEAT-001 and
FEAT-007 are each required by six or seven capstones and cost 1-2 and 3-6 mw.

**4. FEAT-004 and FEAT-003 - 4-7 mw together.** **Why fourth:** FEAT-004 is the
highest-leverage cheap item in the plan - required by seven capstones, beneficial
to three more, at 2-3 mw - because a net partition and a stable net name are what
every exporter, every differential comparison and every board netlist is keyed
on. FEAT-003 is the same shape for review: without stable-id references a circuit
change cannot be reviewed at all.

**5. CAP-13 and CAP-15, the parity capstones - priorities 2 and 3.** **Why
fifth:** they are the highest demo-value-per-week outcomes in the plan, they
reuse the partition and totality work just purchased, and they are the two whose
acceptance a person outside the project can check. CAP-15 also carries the only
substantial tracker coverage of any capstone's spine, so its progress is visible
in the issue list rather than only here.

**6. FEAT-008 - 12-20 mw.** **Why sixth and not earlier:** it is the largest
single maintenance debt in the tree, it is required by CAP-00, CAP-01 and CAP-04,
and every editor-surface feature after it is cheaper. It is placed sixth rather
than first only because the ratchets in steps 1-3 are what make a decomposition
of that size safe to attempt. The debt is growing while it waits - the class in
issue #84 is 42% larger than the number in that issue's title - so "later" has a
measurable price.

**7. Everything else, by capstone priority**, with two standing exceptions the
plan records rather than hides:

- **CAP-02 and CAP-03 rank last on priority but must not be funded last.** Their
  spine features are the most expensive in the plan and are shared, and
  sequencing CAP-02 before CAP-03 is measured to save 42-70 mw from ordering
  alone. Priority 16 and 17 mean "start last", not "fund last".
- **The analog chain (CAP-10, CAP-11, CAP-12, CAP-14) is priced from a rate the
  corpus could not calibrate in this repository.** The solver's first stage is
  designated the calibration experiment; until it runs, every analog band is a
  projection. Do not commit to the chain on those numbers - commit to the
  experiment.

**What not to do first.** Do not start any capstone whose acceptance test rests
on an unfalsified premise before falsifying it. Two are named in the documents
and both are cheap: whether KiCad's gEDA importer installs embedded symbols
(CAP-13, one afternoon), and whether the flat element representation reaches its
byte budget (CAP-17, gated on step 2 above). Each is a half-day that decides
whether a multi-week program is worth funding at all.
