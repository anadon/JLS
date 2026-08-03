# THE CANONICAL ID REGISTRY

The single source of truth for every `CAP-NN`, `FEAT-NNN` and `TASK-NNNN` id in
`docs/plan/`. Authored BEFORE any document, so that parallel authors cannot
collide or duplicate. **No author may mint an id that is not in this file.**

**Three parallel ID-spaces, not a tree.** A feature is not owned by a capstone;
a task is not owned by a feature. Overlap is the norm. Every relationship is an
id reference in both directions.

**Repository anchor.** All HEAD claims in this registry were verified at
`b54e6ee` (`fix(hdl): make the export policy total over the element registry`).
The three defect fixes this study landed are `970db41`, `36cbd37`, `b54e6ee`,
all present in `git log` at that anchor.

**Cost bands** are maintainer-weeks (`mw`) unless a task states days (`d`).
Basis: the repository's own calibration of ~200-250 lines of shipped-and-tested
code per maintainer-week at the 93.0/92.0/84.5 JaCoCo package aggregate plus the
80/82 PIT bar (`09-format-adoption-plan.md` §3 preamble; `cap-realist.md` §1).

**D10 is binding.** Every row is a path and a cost. No row refuses work by
citing the absence of the work.

---

## TABLE 1 - CAPSTONES (19)

Priority 1 is the maintainer's named highest. Priorities 2-17 rank by
demo-value-per-week and by real sequencing constraints, not by enthusiasm.
"Marginal" is the cost given the spine features are funded; "standalone" is
the cost with no shared features, where the corpus states one.

| id | title | outcome (one line) | pri | marginal cost | standalone | source of band |
|---|---|---|---:|---|---|---|
| **CAP-00** | Deferred maintenance: a decade of it | The known-defect backlog at HEAD is closed, the silent-data-loss paths are loud, and the quadratic paths, unfloored packages and untimed CI lanes stop being a tax on every later capstone. | **1** | **35-62 mw** | 35-62 mw | derived here from HEAD; raised from 24-42 mw by the link phase when FEAT-008 was regraded required (see the LINK-PHASE RECORD) |
| **CAP-13** | KiCad interoperation parity | A JLS schematic leaves as a KiCad netlist that `pcbnew` accepts without hand editing, and a KiCad project round-trips its net structure back. | 2 | 6-12 mw | 12-22 mw | `08-views-determination.md` §2.1 (KiCad `.net` PRINT 1-2 wk over the package table) + Tier 3 (5-8) |
| **CAP-15** | HDL toolchain parity (Yosys, Verilator, Icarus, GHDL) | Every design JLS can draw exports to and imports from the four open HDL toolchains, cross-checked in CI, with hierarchy intact. | 3 | 12-22 mw | 20-34 mw | `09-format-adoption-plan.md` §3 data 1 (3-4) + data 2 (4-6) + mapper increments + Wave 3 |
| **CAP-05** | A manufacturable PCB | A student's drawn circuit becomes a board file a fab house accepts, with a BOM and a manufacturability report. | 4 | 11-19 mw | 15-23 mw | `10-capstone-plan.md` §3.1 (C5) |
| **CAP-01** | Multi-discipline multi-user simultaneous development | Several people in several views edit one circuit at the same time and every replica saves byte-identical files. | 5 | 30-46 mw | 49-75 mw | `10-capstone-plan.md` §3.1 (C1) |
| **CAP-09** | Verify a design you did not write | A grader or reviewer takes an unfamiliar `.jls`, runs a property and coverage suite over it, and gets a machine-readable verdict. | 6 | 16-26 mw | 20-34 mw | `AMENDMENT.md` P5 33-50 (formal + coverage half) |
| **CAP-06** | Course delivery and autograding at scale | An instructor ships a lab, students submit `.jls` files, and a batch harness grades hundreds of them deterministically with per-student reports. | 7 | 12-20 mw | 18-30 mw | `AMENDMENT.md` P5; `lf-04-formal-and-grading.md`; issue #214 |
| **CAP-04** | A breadboard implementation of a simple CPU | A drawn CPU projects to a breadboard layout with 74-series parts, and the physical arrangement simulates. | 8 | 31-46 mw | 50-77 mw | `10-capstone-plan.md` §3.1 (C4) |
| **CAP-16** | Logisim-Evolution migration parity | A Logisim-Evolution `.circ` opens in JLS with a migration report naming every construct that did not survive. | 9 | 8-16 mw | 10-20 mw | derived: `.circ` is XML with a documented element set; cost by the `NetlistImporter` precedent |
| **CAP-08** | Import and run a third-party core | A published open-source core (PicoRV32 class) is imported through Yosys JSON and runs its own test program inside JLS. | 10 | 14-24 mw | 26-42 mw | `BRIEF` §13 (gap moved from validation to realization); `09-format-adoption-plan.md` Wave 2/5 |
| **CAP-10** | Audio output | A drawn circuit makes a sound the user hears from the host speakers, in real time. | 11 | 2-3 mw (after CAP-12) | 5-7 mw (S0 alone, no solver) | `11-analog-determination.md` §5.1 S0 (3-4.5), S7 (2-3) |
| **CAP-11** | Audio input | Live host audio enters a drawn circuit as samples and the circuit reacts to it. | 12 | 2-3 mw (after CAP-12) | 5-7 mw | `11-analog-determination.md` §5.1 S6 |
| **CAP-12** | A heart rate monitor | A drawn mixed-signal circuit takes a simulated PPG signal, detects beats, and displays a rate. | 13 | 11-16 mw | 26.5-38.5 mw (cumulative to S5) | `11-analog-determination.md` §5.1 S5 |
| **CAP-14** | ngspice interoperation parity | A JLS analog circuit exports a SPICE deck ngspice runs, and JLS's own results match ngspice's within a stated tolerance, nightly. | 14 | 10-16 mw | 37.5-55.5 mw (cumulative to S8) | `11-analog-determination.md` §5.1 S8 + S1-S3 |
| **CAP-07** | Tape out a student design on a shuttle | A student design goes out on a Tiny Tapeout / SKY130 shuttle and comes back as a chip. | 15 | 11.5-18 mw | 14-24 mw | `08-views-determination.md` §5 "the Tiny Tapeout path (11.5-18 wk)" |
| **CAP-02** | Boot a CLI-only Linux distribution and run commands | A JLS circuit boots Linux to a shell and a transcript of typed commands is byte-compared against a reference. | 16 | 32-58 mw | 155-250 mw | `10-capstone-plan.md` §3.1 (C2) |
| **CAP-03** | A ternary CPU with N-ary subcircuits and a custom kernel | A drawn balanced-ternary CPU runs a hand-written monitor and prints to a live console, verified per retired instruction. Stretch: DOOM. | 17 | 28-45 mw | 98-161 mw | `10-capstone-plan.md` §3.1 (C3, ARCH-B) |
| **CAP-17** | Distributed execution for cluster and grid deployments | A design too large for one machine is partitioned, simulated across hosts and observed as one design, and a campaign of independent runs is dispatched across a grid and aggregated. | 18 | 38-62 mw | 62-98 mw | added at maintainer request after this registry closed; band is the sum of its four new features |
| **CAP-18** | A net that stopped being a wire | A drawn net too long for its driver's edge rate is identified as a transmission line, its reflection and overshoot are shown, a termination fixes them, and the electrical intent leaves JLS as a constraint file a real board tool enforces. | 19 | 11-19 mw | 19-34 mw | added at maintainer request after this registry closed; `CAP-18-net-that-stopped-being-a-wire.md` header. **Both bands are superseded by #313's recomputed sums (marginal 11-20, standalone 35-60, cumulative 51-89); the header figures are carried here unchanged so the supersession stays visible** |

**Sequencing note that is not a priority claim.** CAP-02 and CAP-03 rank last on
priority but their spine features (F037-F042) are the most expensive in the plan
and are shared. `10-capstone-plan.md` §2.2 measures that sequencing CAP-02
before CAP-03 saves 42-70 mw from ordering alone. Priority 16/17 means "start
last", not "fund last".

---

## TABLE 2 - FEATURES (60)

`Owner` is a committed capability-roadmap program (`docs/capability-roadmap/`,
P1-P13) or `UNOWNED`. UNOWNED is information, not an objection: it means the
committed roadmap does not pay for this and someone must decide who does.
`Spine` is the rank from `10-capstone-plan.md` §2.1 where the feature is a spine
row, `-` otherwise.

| id | title | capability (one line) | cost | owner | spine | consumed by |
|---|---|---|---|---|---|---|
| **FEAT-001** | Registry-keyed table totality discipline | Every table keyed on `ElementRegistry.all()` is proven total by a test, and adding an element type fails the build until every table is updated. | 1-2 mw | P3 | S1 | CAP-00, 04, 05, 13, 14, 15, 16 |
| **FEAT-002** | Fail-loud loader and attribute dispatch | An unknown attribute name at load time raises a diagnostic instead of being silently discarded. | 1-2 mw | UNOWNED | - | CAP-00, 01, 08, 16 |
| **FEAT-003** | Uncompressed canonical default with stable-id references | The saved file is plain canonical text whose element references are stable ids, so one inserted element changes one hunk instead of the whole file. | 2-4 mw | P11 | S6 | CAP-00, 01, 05, 06, 13, 16 |
| **FEAT-004** | Shared net-partition IR with stable net naming | One net-partition pass in `jls.netlist` serves every exporter, and net names are a function of `stableId` rather than of save order. | 2-3 mw | P3 | S3, S5 | CAP-01, 02, 03, 04, 05, 07, 13, 14, 15 |
| **FEAT-005** | Quadratic and materializing I/O paths eliminated | Stimulus parse, load fixup and VCD emission stop being quadratic or whole-run-in-memory, so long runs and large circuits are expressible. | 2-3 mw | P1 | - | CAP-00, 02, 03, 06, 09 |
| **FEAT-006** | Simulation capacity and long-run ergonomics | An unbounded, resumable, interruptible batch run with a byte-budgeted memory and no silent event drop. | 3-5 mw | P2 | - | CAP-00, 02, 03, 06, 09 |
| **FEAT-007** | CI long-run lanes, timeouts and cross-platform parity | Every workflow has an explicit timeout, long runs have their own lane, and the full suite is a required check on all supported platforms. | 3-6 mw | UNOWNED | S14 | CAP-00, 02, 03, 06, 09, 14 |
| **FEAT-008** | `SimpleEditor` decomposition, a UI harness and a floored `jls.edit` | The editor is testable, `jls.edit` carries a coverage floor, and a canvas can be added without spending the whole bundle headroom. | 12-20 mw | UNOWNED | - | CAP-00, 01, 04, 10, 11, 12, 16 |
| **FEAT-009** | The measurement gate and a tracked calibration fixture | Every wall-clock estimate divides by measured constants, and the CPU-scale performance anchor is a tracked in-tree fixture, which is what unblocks deleting `riscv/`. | 5-10 mw | UNOWNED | S15 | CAP-00, 02, 03, 08, 09 |
| **FEAT-010** | Deterministic native installers and file association | A user installs JLS without bringing their own JDK, and the installer bytes are reproducible or have a declared bounded residual. | 8-16 mw | UNOWNED | - | CAP-00, 06 |
| **FEAT-011** | Accessibility, keyboard operability and onboarding | The whole editor is usable without a mouse, is legible under color-vision deficiency and HiDPI, and a first-run user is not dropped onto a blank canvas. | 6-10 mw | UNOWNED | - | CAP-00, 06, 16 |
| **FEAT-012** | Semantic merge safety and per-kind merge rules | A three-way merge that would produce a parsing-but-corrupt circuit is rejected with the reason, and each record kind has a declared merge rule. | 9-13 mw | P11 | - | CAP-00, 01, 06 |
| **FEAT-013** | Per-section internal versioning with must-understand semantics | Each file section carries its own version; an old reader skips an unknown optional section and refuses an unknown required one, which is also how bulk images and checkpoints ride along. | 4-7 mw | P11 | S10 | CAP-01, 02, 03, 04, 05, 16 |
| **FEAT-014** | Stable addressing and per-view geometry in the shared model | One artifact addressed as `view:instancePath:sid`, with stable net and group identity and per-view geometry in its own versioned section. | 11-17 mw | P3 | S7, S11, S12, S18 | CAP-01, 04, 05, 02, 03, 12, 13 |
| **FEAT-015** | The headless, programmatic `CircuitOp` layer | Every editor mutation is an invertible, serializable op that applies without a `Graphics`, which is also the supported way to build a circuit from a program. | 4-7 mw | P12 | S4, S13 | CAP-01, 02, 03, 04, 05, 06, 08 |
| **FEAT-016** | Subcircuit type identity, VLNV and the circuit-library format | A subcircuit definition has a canonical identity and version, so libraries of circuits can be distributed as data. | 3-5 mw | P7 | - | CAP-01, 06, 08, 15, 16 |
| **FEAT-017** | Shared and parameterized subcircuit definitions | One definition, N instances, with parameters, instead of N deep copies that have silently diverged. | 25-36 mw | P7 | - | CAP-01, 02, 03, 04, 05, 06, 08 |
| **FEAT-018** | Hierarchical instance structure in the HDL IR | The IR can instantiate a module, so a decomposed design exports as hierarchical Verilog and VHDL instead of throwing. | 4-6 mw | P3 | - | CAP-05, 07, 08, 13, 15 |
| **FEAT-019** | Yosys JSON write | JLS emits the netlist format that back-doors netlistsvg, DigitalJS, and Yosys's own EDIF/BLIF/SPICE backends. | 3-4 mw | UNOWNED | - | CAP-07, 08, 13, 14, 15 |
| **FEAT-020** | Yosys JSON read: mapper parity with the validator | The importer realizes every cell the validator already accepts, including flip-flops and memories, so an imported netlist actually runs. | 4-8 mw | P3 | - | CAP-02, 08, 15 |
| **FEAT-021** | Bidirectional ports in the IR and the element vocabulary | `INOUT` exists end to end, which six external formats need and none can be honest without. | 2-4 mw | P3 | - | CAP-05, 07, 13, 15 |
| **FEAT-022** | Schematic auto-layout for imported netlists | An imported netlist is drawn as a readable schematic rather than a pile at the origin. | 4-8 mw | UNOWNED | - | CAP-08, 15, 16 |
| **FEAT-023** | External toolchain differential oracle and the board on-ramp | `iverilog`, `ghdl`, Yosys, Verilator and nextpnr run against JLS's own output in CI, and a named board goes from schematic to bitstream. | 6-12 mw | P5 | - | CAP-07, 08, 09, 15 |
| **FEAT-024** | Black-box HDL component and external co-simulation | A JLS circuit instantiates a module whose body lives in an external HDL simulator. | 8-14 mw | UNOWNED | - | CAP-08, 09, 15 |
| **FEAT-025** | Logisim-Evolution `.circ` importer and migration report | A `.circ` file opens, and every construct that did not survive is named in a report rather than silently dropped. | 6-12 mw | UNOWNED | - | CAP-16, 06, 04 |
| **FEAT-026** | The four-state value core with a resolution fold | Values carry X and Z, and multi-driver resolution becomes a commutative fold rather than first-driver-in-net-order. | 28-36 mw | P1 | S2 | CAP-04, 05, 08, 09, 15, 02, 03 |
| **FEAT-027** | Strength lattice, driver kinds and net kinds | Open-drain, pull-up, pull-down and bus contention are modelable, which is what a breadboard and a real bus need. | 6-9 mw | P1 | - | CAP-04, 05, 12, 13 |
| **FEAT-028** | Radix-parameterized value and port type system | Radix is a property of a value and a net, validated at connection time, with radix 2 provably unchanged. | 8-12 mw | P1 | - | CAP-03 |
| **FEAT-029** | The N-ary element family and its interop | A drawable, simulable balanced-ternary datapath that exports, dumps and tests like any other JLS circuit. | 9-13 mw | P2 | - | CAP-03 |
| **FEAT-030** | Engine constant factors: the semantics-preserving stack | Every existing golden stays byte-identical and every event costs less, bought with a calendar queue, an immutable width-carrying value, and zero-delay closure. | 12-20 mw | P1 | S24 | CAP-02, 03, 01, 04, 09 |
| **FEAT-031** | The per-instance fidelity toggle and its boundary harness | One subcircuit instance runs behaviorally or structurally, chosen per instance and saved, which makes parity a property of a boundary. | 5-8 mw | P8 | S19 | CAP-02, 03, 01, 04, 09 |
| **FEAT-032** | The host byte port, a `Console` element and transcripts | A running circuit exchanges bytes with a human or a script through one door granted at invocation, and the exchange is replayable. | 10-16 mw | UNOWNED | S22 | CAP-02, 03, 04, 06, 09 |
| **FEAT-033** | `jls.mach`, the reference runner and the guest software stack | The architectural model lives in a pure leaf package under the full coverage bar, with a reference runner and the kernel/DTB/initramfs it must run. | 14-22 mw | UNOWNED | - | CAP-02, 03, 09 |
| **FEAT-034** | Retirement-indexed parity harness and `RetireRecord` | Two implementations of one machine are compared per retired instruction, and over-constraining the comparison is a compile error. | 10-16 mw | P5 | S23 | CAP-02, 03, 08, 09, 04 |
| **FEAT-035** | Checkpoint and simulation-state serialization | A running simulation can be saved and resumed, which is what makes a multi-hour run survivable and a handover free. | 10-17 mw | P9 | S25 | CAP-02, 03, 06, 09 |
| **FEAT-036** | Byte lanes on `Memory` and capacity as a byte budget | A drawn core can do a single-cycle read-modify-write, and memory capacity is a byte budget rather than a word-count cliff. | 3-7 mw | P2 | S20 | CAP-02, 03, 08 |
| **FEAT-037** | Reset semantics, clock and domain architecture | `Register` has honest reset, clock domains are declared, and crossings are checkable. | 13-18 mw | P13 | - | CAP-02, 03, 05, 07, 08, 15 |
| **FEAT-038** | The drawn structural RV32 machine | A machine drawn in the editor, brought up boundary by boundary against the reference, that a person can open and read. | 12-26 mw | UNOWNED | - | CAP-02, 08, 09 |
| **FEAT-039** | JLS-T3: the ternary ISA, toolchain and drawn CPU | A balanced-ternary instruction set, its reference emulator, an in-jar assembler, and the drawn CPU that runs them. | 18-30 mw | UNOWNED | - | CAP-03 |
| **FEAT-040** | The package and pinout library as data | 74-series and DIP part data - pinout, sections, gate equivalence, substitution - shipped as versioned data, not code. | 4-8 mw | UNOWNED | S21 | CAP-04, 05, 13, 16 |
| **FEAT-041** | Packing, refdes, cascade and electrical loading checks | Logic elements are assigned to physical packages in canonical order with a BOM, a wiring list, a width-cascade rule and a fan-out check. | 5-8 mw | P5 | S9, S16, S17 | CAP-04, 05, 13 |
| **FEAT-042** | KiCad and gEDA netlist emitters with a manufacturability gate | A netlist a real PCB tool accepts, plus the check that says whether the board can be built. | 5-10 mw | P3 | - | CAP-05, 13, 04 |
| **FEAT-043** | The breadboard canvas and its physical-simulation binding | A second canvas where parts are placed on a breadboard, consistent with the schematic, and the physical arrangement simulates. | 9-15 mw | UNOWNED | - | CAP-04, 12, 10, 11 |
| **FEAT-044** | Tiny Tapeout wrapper and shuttle handoff | The fixed `tt_um_*` top-level signature, `info.yaml`, and the documented path from a JLS design to a submitted shuttle entry. | 11.5-18 mw | P6 | - | CAP-07, 15 |
| **FEAT-045** | Host audio sink and source without a solver | Samples leave for the speakers and arrive from the microphone, with no analog engine involved at all. | 5-7 mw | UNOWNED | - | CAP-10, 11, 12 |
| **FEAT-046** | The analog solver core and its determinism gate | Sparse LU plus Newton-Raphson plus timestep control, producing byte-identical results across platforms and JDKs, checked against ngspice. | 17.5-26 mw | UNOWNED | - | CAP-12, 14, 10, 11 |
| **FEAT-047** | The physical time base and the nominal real-time scalar | One declared physical time unit per circuit, so waveforms, delays and audio rates stop being unitless. | 2-3 mw | P4 | - | CAP-10, 11, 12, 14, 07 |
| **FEAT-048** | A2D/D2A bridge elements and A-STEP synchronization | Drawable converters and a defined lock-step between the analog solver and the discrete-event loop. | 4-6 mw | UNOWNED | - | CAP-10, 11, 12, 14 |
| **FEAT-049** | Analog device models, the drawn palette and convergence hardening | The device set a teaching lab needs, drawable, that converges on homework-grade circuits. | 21-33 mw | UNOWNED | - | CAP-12, 14, 10 |
| **FEAT-050** | Module runtime consumed: extension points and providers | The module registry that already boots is actually read for dispatch, with a typed extension-point catalog and an external element-provider path. | 5-10 mw | P12 | S8 | CAP-01, 02, 03, 06, 16 |
| **FEAT-051** | P2P session foundation and shared session v1 | Two installs establish a verified encrypted session over one circuit, with membership, presence and snapshot sync. | 12-18 mw | UNOWNED | - | CAP-01, 06 |
| **FEAT-052** | CRDT replication, collaborative undo and security hardening | Concurrent edits converge without a server, undo is per-user, and network input cannot introduce an element type the peer did not allow. | 14-22 mw | P11 | - | CAP-01, 06 |
| **FEAT-053** | Test-vector front end and autograding at scale | A GUI front end over the batch test engine, plus a batch harness and machine-readable reports for grading many submissions. | 9-15 mw | P5 | - | CAP-06, 09, 04, 16 |
| **FEAT-054** | Flat, compact element representation | Runtime state lives in flat primitive arrays indexed by element, cutting the per-element footprint by about an order of magnitude. | 12-20 mw | UNOWNED | - | CAP-17 |
| **FEAT-055** | Partitioned model and streaming elaboration | A design exists as parts that load independently, with boundary nets named identically on both sides of every cut. | 10-16 mw | UNOWNED | - | CAP-17 |
| **FEAT-056** | Distributed simulation transport and barrier protocol | Partitions in separate processes exchange boundary events under a discipline whose result does not depend on partition count or arrival order. | 10-18 mw | UNOWNED | - | CAP-17 |
| **FEAT-057** | Campaign execution and artifact aggregation | Independent runs are dispatched across workers and aggregated into one report that does not depend on scheduling. | 6-8 mw | UNOWNED | - | CAP-17 |
| **FEAT-058** | Edge rate, declared physical length and the electrical-length lint | Two declared physical attributes and a design check that computes the critical length and prints a verdict, with "not assessable" where a net did not opt in. | 3-6 mw | P4 | - | CAP-18, 04, 05, 07 |
| **FEAT-059** | The closed-form transmission-line element and the reflection lab | A lossless-line element between two ordinary nets with the four canonical terminations, computed in closed form and shown on a real-valued trace row. | 2-3.5 mw | UNOWNED | - | CAP-18, 04 |
| **FEAT-060** | Signal-integrity constraint authorship and PCB constraint export | An authored SI constraint set on nets, emitted as a rule file an external DRC enforces, with routed-length back-annotation. | 5.5-9.5 mw | P3 | - | CAP-18, 05, 13 |

Also in the plan but deliberately NOT a separate feature: formal property
checking and equivalence (P5's formal half, 14-22 mw). It is folded into
FEAT-053's acceptance criteria and FEAT-034's harness because a separate feature
would share more than half its tasks with both. Recorded here so no author
re-mints it.

---

## TABLE 3 - TASKS (112)

A task is what one person can sit down and finish. Anything over ~2 weeks was
promoted to a feature. The id space is closed: `TASK-0001` through `TASK-0112`,
no gaps.

| id | title | deliverable (one line) | cost | enables |
|---|---|---|---|---|
| **TASK-0001** | Audit and pin every registry-keyed table | A written inventory of every table keyed on the element registry, an orientation enum, an edit-op enum or a save tag, each with a totality test asserting it covers the key set exactly and falls through nowhere. | 1.5 wk | FEAT-001, FEAT-002 |
| **TASK-0002** | Registry totality lint as a standing build rule | One reusable JUnit base any registry-keyed table extends, plus a `CONTRIBUTING.md` rule requiring it for new tables. | 3 d | FEAT-001 |
| **TASK-0003** | Make attribute dispatch total and the loader check it | The `setValue` overloads report an unmatched attribute name instead of returning silently, and the five loader call sites raise a diagnostic naming file, line, element and attribute. | 1 wk | FEAT-002 |
| **TASK-0004** | Silent-data-loss regression corpus | Fixture files carrying attributes no element declares, asserting a diagnostic rather than a quiet load. | 2 d | FEAT-002 |
| **TASK-0005** | Reference elements by stable id, with a diff ratchet | The writer emits stable ids rather than dense save-time indices, the reader accepts both for one epoch, and a ratchet asserts that inserting one element changes a bounded number of lines. | 2 wk | FEAT-003, FEAT-012 |
| **TASK-0006** | Plain text as the default container, with the autosave policy | The default write path is plain canonical text, compression stays an explicit option, and the autosave and undo-snapshot containers get a decided, tested policy. | 1 wk | FEAT-003 |
| **TASK-0007** | Extract the net-partition walk into its own package | The partition pass moves out of the HDL exporter into `jls.netlist` with its own tests, and the exporter consumes it. | 1.5 wk | FEAT-004 |
| **TASK-0008** | Key net and probe names off stable id, and validate them | Net naming becomes a documented function of stable id with a frozen convention, probe names are validated on attach, and the waveform variable-declaration checker rejects what the spec rejects. | 1.5 wk | FEAT-004, FEAT-005 |
| **TASK-0009** | De-quadratic the stimulus parse and the load fixup | The stimulus parse stops concatenating per line and wire-end fixup becomes linear, each pinned by a large-input benchmark fixture. | 1.5 wk | FEAT-005 |
| **TASK-0010** | Stream the waveform dump instead of materializing it | The dump writes incrementally to a sink; the whole-dump-as-one-string path is removed. | 4 d | FEAT-005 |
| **TASK-0011** | Adjudicate and fix the past-limit event drop | A recorded decision plus the code change: an event polled past the time limit is no longer removed from the duplicate-check set and then discarded. | 3 d | FEAT-006 |
| **TASK-0012** | Unbounded run duration | An explicit no-limit run mode; the default time limit stops being a silent ceiling. | 2 d | FEAT-006 |
| **TASK-0013** | Memory capacity as a byte budget, initialized copy-on-write | The dense-store cap becomes a byte budget with headroom above the guest minimum, and initialization stops doubling heap. | 1.5 wk | FEAT-006, FEAT-036 |
| **TASK-0014** | Long-lived batch mode with pause, heartbeat and clean interrupt | Batch pause stops being identical to stop; a progress heartbeat and a clean interrupt path exist. | 1.5 wk | FEAT-006, FEAT-035 |
| **TASK-0015** | Explicit timeouts on every workflow job | All six workflows carry per-job timeouts against the hosted ceiling. | 1 d | FEAT-007 |
| **TASK-0016** | Split CI into a required fast lane and a long-run lane, with a fixture policy | The required gate stays short, long runs move to their own scheduled lane, and fixtures above a stated size have a declared storage mechanism and a CI guard. | 1.5 wk | FEAT-007, FEAT-009 |
| **TASK-0017** | Promote the macOS and Windows headless lanes to required | The full suite runs headless on both platforms, with the display and HDL-sim suites armed and a coverage floor, as required checks. | 2 wk | FEAT-007 |
| **TASK-0018** | Wayland GUI rig first light | The GUI boots under a headless compositor in CI, is screenshotted, and the findings are published. | 1 wk | FEAT-007, FEAT-008 |
| **TASK-0019** | The editor decomposition plan and its coverage floor | A written plan naming each class to extract, its dependencies and its test surface, plus an initial `jls.edit` package floor at the measured value, entering the raise-only ratchet. | 1.5 wk | FEAT-008 |
| **TASK-0020** | Extract the mouse machine and replace the source-identity dispatcher | The nine-state mouse machine becomes a tested class with no drawing dependency in its transitions, and the source-identity dispatcher becomes typed action objects. | 2 wk | FEAT-008 |
| **TASK-0021** | The UI test harness, including dialog construction | A harness asserting element presence, geometry, relations, actions, menus and mouse interactions without a human, plus construction and validation coverage for every element dialog. | 2 wk | FEAT-008, FEAT-053 |
| **TASK-0022** | Measure the per-cycle active fraction, CPI and the calibration constant | The two-cycle unified-memory conversion of the shipped demo, event-counted, resolving the dominant modeling uncertainty. | 1.5 wk | FEAT-009 |
| **TASK-0023** | Measure the behavioral binding and the levelized cost at scale | An instrumented behavioral machine on a real bus, event-counted, plus the levelized benchmark re-run at CPU-shaped node counts, reporting node count and pass count. | 1.5 wk | FEAT-009, FEAT-030, FEAT-031 |
| **TASK-0024** | Write the machine-calibration document | The element census, events per cycle, the size-scaling exponent, the active fraction, CPI and events per instruction, each with its method. | 1 wk | FEAT-009 |
| **TASK-0025** | Commit the tracked calibration fixture, re-home the goldens, delete `riscv/` | A tracked in-tree fixture replaces the untracked performance anchor, the oracle fixtures and the salvaged reference-runner design move in-tree, and `riscv/` is removed. | 2 wk | FEAT-009 |
| **TASK-0026** | The simulation budget and allocation ratchet | Events per clock cycle as a hard equality on committed fixtures, a nanoseconds-per-event band, a per-event allocation band, and a gate asserting the whole existing golden corpus is byte-identical across any engine change. | 1 wk | FEAT-009, FEAT-030 |
| **TASK-0027** | Native installers per OS with file association | Installers that remove the bring-your-own-runtime barrier, with the file type associated. | 2 wk | FEAT-010 |
| **TASK-0028** | Installer reproducibility, independent rebuild and signing | Build-epoch and dependency pinning, a bill-of-materials guard in CI, a published build record with a declared reproducible-artifact scope and an independent rebuild check, and signed Windows installers. | 2 wk | FEAT-010 |
| **TASK-0029** | Keyboard operability | Menu accelerators and mnemonics, a decided focus policy, and every element placeable, movable, configurable and deletable from the keyboard. | 2 wk | FEAT-011 |
| **TASK-0030** | Visual ergonomics and first-run onboarding | Color-vision-safe value semantics, scaling and system look-and-feel, persistent preferences, a welcome state, discoverable samples, and the applet-era leftovers removed. | 2 wk | FEAT-011 |
| **TASK-0031** | Semantic validation of a merged file | A post-merge check that rejects dangling references and inconsistent nets with the reason, instead of accepting a parsing-but-corrupt file. | 1.5 wk | FEAT-012 |
| **TASK-0032** | The per-record-kind merge rule table | A written table of merge rules per record kind, with a test per row. | 2 wk | FEAT-012, FEAT-052 |
| **TASK-0033** | Section framing, must-understand flags and the epoch policy | Independently versioned sections marked required or optional, reader semantics tested both ways, and a written format-epoch and migration policy with a migration test. | 2 wk | FEAT-013 |
| **TASK-0034** | The raw bulk-image section | Memory and guest images ride in an optional binary section rather than as hex text, with the size arithmetic documented against the text cap. | 1.5 wk | FEAT-013, FEAT-036 |
| **TASK-0035** | Stable identity for instances, nets and groups | The instance-path addressing scheme specified and implemented, with stable ids for nets and groups and a uniqueness test that survives shared definitions. | 2 wk | FEAT-014 |
| **TASK-0036** | Per-view geometry section and the op view discriminator | Geometry per view in its own versioned section, preserved verbatim by readers that do not know the view, and the geometric ops carrying a view discriminator with exact inverses. | 2 wk | FEAT-014, FEAT-043 |
| **TASK-0037** | Headless op application and the complete op vocabulary | Op application takes a text-metrics abstraction instead of a drawing context, and placement drop, paste, wire-attach finish and dialog commits become ops, closing the set. | 2 wk | FEAT-015, FEAT-052 |
| **TASK-0038** | Programmatic circuit construction verbs | A documented, tested way to build a circuit from a program through ops, replacing the emit-text-and-reparse idiom. | 2 wk | FEAT-015, FEAT-038, FEAT-039 |
| **TASK-0039** | Definition identity: structural digest and version strings | A canonical digest identifying a definition independent of instance geometry, plus vendor/library/name/version strings and a collision policy. | 2 wk | FEAT-016 |
| **TASK-0040** | The circuit-library container and provenance | A versioned library format carrying definitions and their provenance, distributed as data. | 2 wk | FEAT-016 |
| **TASK-0041** | Definition/instance split with parameters | A definition exists once and instances reference it with bound parameters, replacing per-instance deep copies. | 2 wk | FEAT-017 |
| **TASK-0042** | The elaboration pass and its diagnostics | A shared elaborator that resolves parameters and reports every unresolved binding. | 2 wk | FEAT-017 |
| **TASK-0043** | Module instantiation and the hierarchy walk | The HDL IR gains an instantiation statement and multi-module output, and the exporter walks the hierarchy binding ports to nets, detecting cycles and propagating rejection. | 2 wk | FEAT-018 |
| **TASK-0044** | Hierarchical emitters and their goldens | Both HDL printers emit hierarchy, with goldens cross-checked against the external compilers. | 1.5 wk | FEAT-018, FEAT-023 |
| **TASK-0045** | The synthesis-tool netlist writer | A writer producing the netlist interchange schema, with goldens validated against the published schema. | 2 wk | FEAT-019 |
| **TASK-0046** | Document the tool-mediated netlist paths | Written, tested recipes that get the gate-level interchange formats out through the external synthesis tool rather than through a reimplemented lowering pass. | 3 d | FEAT-019 |
| **TASK-0047** | Realize sequential, memory and arithmetic cells on import | The importer emits flip-flops, latches, memories and word-level arithmetic for the cells the validator already accepts, closing the validator/realizer gap. | 2 wk | FEAT-020 |
| **TASK-0048** | Realize hierarchy instances on import | Hierarchical instances in an imported netlist become subcircuit instances. | 1.5 wk | FEAT-020, FEAT-022 |
| **TASK-0049** | Bidirectional ports end to end | The IR direction set gains a bidirectional case that every emitter handles or refuses explicitly, plus a bidirectional pin element with honest readback. | 2 wk | FEAT-021, FEAT-027 |
| **TASK-0050** | Heuristic layered layout for imported netlists | A layered layout producing a readable schematic in-process, with a layout-quality metric test. | 2 wk | FEAT-022 |
| **TASK-0051** | Arm the external toolchains in CI | The synthesis, place-and-route and fast-simulation tools run against shipped goldens in CI, alongside the existing compile-oracle legs. | 1 wk | FEAT-023 |
| **TASK-0052** | Per-board constraints and one real flash | Constraint emission per named board with a golden each, and the documented circuit-to-bitstream path exercised once on real hardware and recorded. | 2 wk | FEAT-023, FEAT-044 |
| **TASK-0053** | Black-box HDL component and its co-simulation contract | A port-list scanner for an external HDL file, a black-box element, and the forward-only contract governing conversation with an external simulator. | 2 wk | FEAT-024 |
| **TASK-0054** | The foreign-tool reader and its migration report | A reader for the migration source format with a written construct map, and a report naming every dropped or approximated construct, with a test asserting nothing is dropped silently. | 2 wk | FEAT-025, FEAT-002 |
| **TASK-0055** | Absorb the through-hole part data | Part data transcribed from license-compatible sources, carrying attribution and license notices. | 2 wk | FEAT-025, FEAT-040 |
| **TASK-0056** | Widen the value permits and migrate the value representation | The permitted value set is widened with accessors, and the plane-encoded, width-carrying immutable value replaces defensive cloning across the react bodies, with radix 2 provably byte-identical. | 2 wk | FEAT-026, FEAT-028, FEAT-030 |
| **TASK-0057** | The resolution fold | Multi-driver resolution becomes an order-independent fold replacing first-active-driver-in-net-order. | 2 wk | FEAT-026, FEAT-027 |
| **TASK-0058** | Strength lattice and pull elements | Drive strengths, driver kinds, net kinds, and pull-up and pull-down elements. | 2 wk | FEAT-027 |
| **TASK-0059** | Radix on ports and nets, validated not widened | Radix is carried on puts and nets and checked above the width check at the editor connection sites. | 1.5 wk | FEAT-028 |
| **TASK-0060** | The higher-radix operator kernel | Min, max, complement, cyclic and literal operators over the planes, written once, with the lane-packed balanced-ternary adder and its differential test. | 2 wk | FEAT-028, FEAT-029 |
| **TASK-0061** | The N-ary element family | The registered N-ary element types, shipped with model tests, in coverage-floor-sized batches. | 2 wk | FEAT-029 |
| **TASK-0062** | N-ary interop: lowering, waveform manifest and test grammar | Higher-radix designs export, dump and test through the existing paths with a declared radix manifest. | 2 wk | FEAT-029 |
| **TASK-0063** | Calendar queue with an intrusive queued flag | The priority queue and duplicate-check set are replaced by a time-bucketed calendar queue preserving total order exactly. | 2 wk | FEAT-030 |
| **TASK-0064** | Zero-delay closure | Events that model no elapsed time collapse without changing any per-element propagation delay or any golden. | 2 wk | FEAT-030 |
| **TASK-0065** | The saved per-instance fidelity attribute | A per-instance attribute naming one of a closed set of implementations of that instance's definition, with its versioning. | 1.5 wk | FEAT-031 |
| **TASK-0066** | The boundary handover harness | Toggling a binding at a declared instant maps that boundary's state, with a null-toggle equivalence gate. | 2 wk | FEAT-031, FEAT-035 |
| **TASK-0067** | The host byte port seam | A sealed byte-exchange seam, granted at invocation, never a property of the circuit file. | 2 wk | FEAT-032 |
| **TASK-0068** | The console element | The minimal serial element: transmit, receive and status, polled, no interrupt controller. | 2 wk | FEAT-032 |
| **TASK-0069** | Transcript capture, replay and the console pane | Host input logged in retirement index and replayed deterministically, a ratchet forbidding goldens produced in live mode, and a GUI pane bound through the existing runner/event-thread seam. | 2 wk | FEAT-032, FEAT-034, FEAT-008 |
| **TASK-0070** | The machine package and its reference runner | A pure leaf package for architectural logic, born floored at the strong bar, containing an independent implementation of the machine usable as the parity counterparty. | 2 wk | FEAT-033, FEAT-034 |
| **TASK-0071** | Guest image build, pinning and residence | The guest software stack built reproducibly with the declared clock and calibration pinned, plus a decided, tested answer for where the image lives. | 2 wk | FEAT-033, FEAT-013 |
| **TASK-0072** | The retirement record and its trace emission | A record carrying the retirement field list and no field for cycles, pipeline or cache state, emitted through the existing probe-sample hook rather than a new seam. | 2 wk | FEAT-034 |
| **TASK-0073** | The differential comparator, exclusion set and sync points | A comparator over two traces with an explicitly enumerated per-bit exclusion set under its own ratchet, plus full architectural state compared as a digest at declared sync points. | 2 wk | FEAT-034, FEAT-053 |
| **TASK-0074** | Serialize the queue, the clock and stateful element contents | The running queue, clock and duplicate-check state become saveable, and memory and register contents are written back rather than rebuilt from init text. | 2 wk | FEAT-035 |
| **TASK-0075** | Checkpoint round-trip equivalence test | A test that resuming from a checkpoint produces the byte-identical continuation of an uninterrupted run. | 1 wk | FEAT-035 |
| **TASK-0076** | Write-mask input on memory | A byte-lane write mask so a drawn core can do a single-cycle read-modify-write. | 1.5 wk | FEAT-036 |
| **TASK-0077** | Honest reset on the register element | Synchronous and asynchronous reset with declared polarity, exported honestly. | 1.5 wk | FEAT-037 |
| **TASK-0078** | Clock domains and crossing checks | Clock domains declared on the design and carried through the IR, with a check reporting every unsynchronized crossing. | 2 wk | FEAT-037, FEAT-041 |
| **TASK-0079** | Draw the machine and bring it up boundary by boundary | The datapath and control drawn in the editor against the census, brought up one boundary at a time against the reference. | 2 wk | FEAT-038, FEAT-031 |
| **TASK-0080** | The headless boot run and its transcript comparison | The end-to-end run, in the long-run lane, with a byte-compared transcript. | 2 wk | FEAT-038, FEAT-034 |
| **TASK-0081** | Specify the ternary ISA and its conformance corpus | The instruction set written down, including the three-way branch and the exact division rule, with a conformance corpus covering the undefined-value cases. | 2 wk | FEAT-039 |
| **TASK-0082** | The ternary reference emulator and assembler | An independent emulator for the ISA plus an in-jar assembler and disassembler. | 2 wk | FEAT-039, FEAT-034 |
| **TASK-0083** | Draw the ternary CPU | The drawn balanced-ternary machine, against a stated element census. | 2 wk | FEAT-039, FEAT-029 |
| **TASK-0084** | The monitor program | A hand-written monitor that runs on the drawn machine and prints to the console. | 2 wk | FEAT-039, FEAT-032 |
| **TASK-0085** | The package data schema and footprint binding | A versioned schema for pinout, sections, gate equivalence and substitution with provenance, plus the mechanism binding a logic element to a package, footprint name and part value. | 2 wk | FEAT-040, FEAT-042 |
| **TASK-0086** | Packing, refdes, BOM and wiring list | Deterministic first-fit packing of logic elements into package sections in canonical stable-id order, with reference designators, a BOM and a point-to-point wiring list as a batch report. | 2 wk | FEAT-041 |
| **TASK-0087** | Width decomposition and the cascade rule | A rule decomposing a word-wide element into physical slices with synthetic inter-slice nets in the IR. | 2 wk | FEAT-041 |
| **TASK-0088** | Fan-out and DC loading check | A loading check driven by package data, reported as a batch analysis. | 1.5 wk | FEAT-041 |
| **TASK-0089** | The PCB-tool netlist emitter | A netlist emitter whose output the target PCB tool accepts without hand editing, with a golden. | 1.5 wk | FEAT-042 |
| **TASK-0090** | The open-schematic emitter | A schematic emitter with derived symbol geometry, reference designators and sequential pin numbers. | 2 wk | FEAT-042 |
| **TASK-0091** | The manufacturability gate | A check reporting whether the design can be fabricated, with named rules. | 1.5 wk | FEAT-042 |
| **TASK-0092** | The breadboard canvas | A second canvas with its own geometry, ops, undo and palette, inside the coverage budget. | 2 wk | FEAT-043 |
| **TASK-0093** | Breadboard consistency check and physical binding | A check that the two views describe the same nets, reported per discrepancy, and the placed physical arrangement driving the simulation including contention the schematic hides. | 2 wk | FEAT-043, FEAT-041, FEAT-027 |
| **TASK-0094** | The shuttle wrapper and its metadata | The fixed top-level signature and the project metadata file, generated and validated. | 2 wk | FEAT-044 |
| **TASK-0095** | The shuttle submission path, documented and walked | The documented end-to-end path, walked once, with the result recorded. | 2 wk | FEAT-044 |
| **TASK-0096** | Host audio sink and source | Samples leave a circuit for the host audio device and arrive from the host input, each with a file mode for deterministic tests. | 2 wk | FEAT-045 |
| **TASK-0097** | Solver core and timestep control | Sparse factorization plus Newton-Raphson with stated convergence criteria, and adaptive timestep control with a stated error criterion. | 2 wk | FEAT-046 |
| **TASK-0098** | The analog determinism controls | The controls beyond strict floating point that make results byte-identical across platforms and runtimes, each tested. | 2 wk | FEAT-046 |
| **TASK-0099** | Controlled sources, waveforms and model cards | Controlled sources, standard waveforms, and model and subcircuit card parsing. | 2 wk | FEAT-046 |
| **TASK-0100** | The external-simulator differential corpus | A corpus comparing JLS against the reference analog simulator within a stated tolerance, run nightly, with a method for detecting a regression that stays inside the tolerance. | 2 wk | FEAT-046, FEAT-023 |
| **TASK-0101** | The nominal real-time scalar | One declared physical time unit per circuit, carried into every waveform and delay. | 1.5 wk | FEAT-047 |
| **TASK-0102** | Bridge elements and the synchronization protocol | Drawable converters with declared thresholds and rates, and the lock-step contract between the solver and the event loop with its rollback or forward-only rule stated. | 2 wk | FEAT-048 |
| **TASK-0103** | Device and transistor models | The device model set a teaching lab needs, each with its parameter list and its test, including the transistor families at the stated levels. | 2 wk | FEAT-049 |
| **TASK-0104** | Convergence hardening | Continuation, damping and limiting so homework-grade circuits converge, with a corpus of the ones that did not. | 2 wk | FEAT-049 |
| **TASK-0105** | Per-view palettes and the analog palette | The palette contract gains a view dimension, unblocking progressive disclosure that a currently-green test forbids, and the analog palette ships default-hidden and opt-in with a first-year-visibility test. | 2 wk | FEAT-049, FEAT-043, FEAT-029, FEAT-008 |
| **TASK-0106** | Consume the module registry for dispatch, with a typed catalog | The registry that already boots is read for dispatch instead of being populated and ignored, and every seam is enumerated with id, contract type, cardinality and lifecycle phase, cross-checked in both directions by a test. | 2 wk | FEAT-050 |
| **TASK-0107** | The element-provider discovery path | External element descriptors discovered through the service loader atop the registry. | 2 wk | FEAT-050 |
| **TASK-0108** | Session foundation: identity, transport, membership and sync | Per-install identity keys, an encrypted transport with out-of-band verification, and session membership lifecycle, floor control, presence and snapshot synchronization. | 2 wk | FEAT-051 |
| **TASK-0109** | The replica loop over a loopback transport | Two headless replicas over an in-tree loopback and a chaos transport, saving byte-identical files. | 2 wk | FEAT-051, FEAT-052 |
| **TASK-0110** | Convergent replication, collaborative undo and input hardening | Op replication with anti-entropy and compaction, a convergent type for the model's ordered collections, per-user undo, and a closed op vocabulary with an element-type allowlist, caps and ratchet tests for network input. | 2 wk | FEAT-052 |
| **TASK-0111** | The test panel, the grading harness and its reports | A GUI front end over the batch test-vector engine, a harness running a lab's vectors over many submissions, machine-readable export and grading reports, and one complete worked lab as the reference for course authors. | 2 wk | FEAT-053, FEAT-019, FEAT-034 |
| **TASK-0112** | Property checking, equivalence and coverage over an unfamiliar design | Properties expressible and checked with the answer-logging discipline recorded, an equivalence check between two designs, and toggle and branch coverage reported as data. | 2 wk | FEAT-053, FEAT-034 |

**Hygiene tasks folded into the rows above rather than given their own ids**,
recorded so no author re-mints them: rejecting incompatible batch flag
combinations at parse time (in TASK-0001), correcting the stale element and
statement-kind counts and the subprocess-versus-file-handoff and
emit-versus-cannot-emit claims at their sources (in TASK-0024), and adding the
HDL export menu item (in TASK-0051).

---

## TABLE 4 - GITHUB ISSUE MAP

**All 34 open issues** in `anadon/jls` at the time of writing, each mapped to the
plan ids that relate to it. Verified by `list_issues(state=OPEN)`; the API
reported `totalCount: 34` and returned 34.

| # | title | related plan ids | relationship |
|---:|---|---|---|
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | CAP-00, FEAT-007, TASK-0015, TASK-0017 | closes |
| 264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | CAP-15, CAP-07, FEAT-023, TASK-0052 | closes |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | CAP-00, CAP-02, FEAT-026, FEAT-030, TASK-0056, TASK-0026 | closes |
| 224 | Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue) | FEAT-050, TASK-0106 | tracking / depends on |
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | FEAT-050, TASK-0106 | closes |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | CAP-06, FEAT-053, TASK-0111 | closes |
| 212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` atop the #78 registry | FEAT-050, TASK-0107 | closes |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | CAP-02, CAP-09, FEAT-009, FEAT-034, FEAT-038, TASK-0025, TASK-0079, TASK-0080 | closes |
| 191 | Deterministic macOS installer: reproducible (or bounded-residual) dmg | CAP-00, FEAT-010, TASK-0028 | closes |
| 190 | Deterministic Windows installer: reproducible (or bounded-residual) msi | CAP-00, FEAT-010, TASK-0028 | closes |
| 188 | Deterministic native installers: per-format byte-reproducibility program | CAP-00, FEAT-010, TASK-0028 | tracking / closes |
| 185 | Reproducible Builds conformance: independent-rebuild verification, published `.buildinfo`, and a declared reproducible-artifact scope | CAP-00, FEAT-010, TASK-0028 | closes |
| 184 | Release-artifact reproducibility gaps: container apt pinning, installer `SOURCE_DATE_EPOCH`, and a BOM reproducibility guard in CI | CAP-00, FEAT-010, TASK-0028 | closes |
| 171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo | CAP-01, FEAT-052, TASK-0110 | closes |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | CAP-01, FEAT-052, TASK-0110, TASK-0001 | closes |
| 169 | Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel | CAP-01, FEAT-051, TASK-0108 | closes |
| 168 | P2P session foundation: per-install identity keys, encrypted transport, SAS out-of-band verification | CAP-01, FEAT-051, TASK-0108 | closes |
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | CAP-01, FEAT-015, TASK-0037 | depends on / closes |
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | CAP-01, FEAT-051, FEAT-052 | tracking |
| 162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | CAP-00, FEAT-008, TASK-0021 | closes |
| 134 | Authenticode-sign the Windows installers | CAP-00, FEAT-010, TASK-0028 | closes |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | CAP-00, FEAT-007, TASK-0017 | closes |
| 101 | Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings | CAP-00, FEAT-007, FEAT-008, TASK-0018 | closes |
| 91 | Automated UI test harness: assert element presence, geometry, relations, actions, menus, and mouse interactions | CAP-00, FEAT-008, TASK-0021 | closes |
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | CAP-00, FEAT-008, TASK-0019, TASK-0020 | closes |
| 82 | Distribution: jpackage installers per OS and `.jls` file association | CAP-00, FEAT-010, TASK-0027 | closes |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | FEAT-001, TASK-0001, TASK-0002 | informs (its registry half already shipped) |
| 76 | Visual ergonomics and platform integration: color-vision-safe semantics, HiDPI scaling, system look-and-feel, dark mode, persistent preferences | CAP-00, FEAT-011, TASK-0030 | closes |
| 75 | Keyboard operability and accessibility | CAP-00, FEAT-011, TASK-0029 | closes |
| 73 | First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots | CAP-00, FEAT-011, TASK-0030 | closes |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | CAP-15, CAP-08, FEAT-024, TASK-0053 | closes |
| 62 | HDL Stage 2 companion: schematic auto-layout for imported netlists | CAP-08, CAP-15, CAP-16, FEAT-022, TASK-0050 | closes |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | CAP-08, CAP-15, FEAT-020, TASK-0047, TASK-0048 | closes |
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | CAP-15, FEAT-018, FEAT-019, FEAT-020 | tracking / overlaps |

### Open issues that NOTHING in this plan touches

**None.** All 34 open issues map to at least one plan id. That is a genuine
finding rather than a convenience: the deferred-maintenance capstone (CAP-00)
was scoped from HEAD defects and it absorbed the entire distribution, CI, GUI
and accessibility backlog, which is where most of the open tracker lives.

Two issues are mapped **weakly** and authors must not overstate the link:
- **#78** is an architecture issue whose registry half already shipped;
  FEAT-001 builds *on* it rather than closing it. Cite it as "informs".
- **#59** is a staged tracking issue spanning three features; no single feature
  closes it.

### Plan items with NO issue

Explicitly recorded, because the gap is information. Every one of these must
carry "no issue" in its document rather than a blank field.

- **Every capstone except the collaboration one has no tracking issue.**
  CAP-00 and CAP-02 through CAP-16 have no issue. Only CAP-01 has one (#163).
- **CAP-00 itself has no issue**, despite being the maintainer's highest
  priority. Its constituent GUI, CI and distribution issues exist; the program
  that would sequence them does not.
- **The silent-data-loss path has no issue.** FEAT-002, TASK-0003, TASK-0004.
  `Element.setValue` returns silently on an unknown attribute
  (`src/jls/elem/Element.java:344-351` for the int overload, the same shape on
  the sibling overloads) and the loader calls it unconditionally at five sites
  (`src/jls/Circuit.java:1067,1078,1089,1105,1116`).
- **The quadratic and materializing paths have no issue.** FEAT-005,
  TASK-0009, TASK-0010.
- **The diff-amplification and format work has no issue.** FEAT-003, FEAT-012,
  FEAT-013, FEAT-014 - the whole of decisions D1, D2 and D3.
- **The calibration fixture that blocks deleting `riscv/` has no issue.**
  FEAT-009, TASK-0025. `riscv/build/k2000.jls` is untracked - ignored by
  `riscv/.gitignore:1` - and is the only CPU-scale performance anchor in the
  tree.
- **The entire N-ary program has no issue.** FEAT-028, FEAT-029.
- **The entire analog program has no issue.** FEAT-045 through FEAT-049.
- **The entire physical program has no issue.** FEAT-040 through FEAT-044.
- **The parity, device and machine layers have no issue.** FEAT-030 through
  FEAT-039. Issue #232 covers only the value representation - not the queue,
  the zero-delay closure, the fidelity boundary or the parity harness.
- **The migration-parity path has no issue.** FEAT-025.

### A caution on issue numbers cited by the corpus but NOT open

The evidence corpus cites `#37, #38, #77, #79, #80, #129, #165, #166, #201,
#213, #215, #220, #221, #222, #288-#303, #304` as recorded decisions or prior
work. **None of them appears in the open list of 34.** They are closed, are
recorded decisions rather than open work, or were consolidated (#213 and #215
into #264). **Authors must not cite any of them as open**; cite them as
recorded decisions or as closed work, and verify the number before citing it.
Do not create issues.

---

## DEDUPLICATION RECORD

This registry is the deduplication point. Merges performed, with the reason, so
that no author re-mints a merged item:

1. **Uncompressed default + stable-id references = FEAT-003.** Spine row S6
   states them as one shipment; separating them ships a format epoch that
   introduces a diff regression and then fixes it.
2. **Net-name stability (S3) + the shared net-partition IR (S5) = FEAT-004.**
   Names are derived by the partition pass; two features would share every task.
3. **Instance identity (S7) + net identity (S18) + per-view geometry (S11) +
   the op view discriminator (S12) = FEAT-014.** `10-capstone-plan.md` §2.2
   already groups these as one addressing scheme; they are one design problem.
4. **The headless op layer (S4) + programmatic construction verbs (S13) =
   FEAT-015.** The second is the first with a public verb set; splitting them
   invites two op grammars.
5. **MVL stages 0-2 = FEAT-028; stages 3-4 = FEAT-029.** Four features would
   produce four documents whose tasks interleave.
6. **Editor decomposition + the UI harness + the `jls.edit` floor = FEAT-008.**
   The decomposition is untestable without the harness and unratchetable
   without the floor. Issues #84, #91 and #162 all land here.
7. **All seven installer and reproducibility issues = FEAT-010.** #82, #134,
   #184, #185, #188, #190, #191 share one build pipeline and one task set.
8. **The three accessibility and ergonomics issues = FEAT-011.** #73, #75, #76
   share the editor surface and the same test substrate.
9. **The machine package + the reference runner + the guest software stack =
   FEAT-033.** The model without a runner is untestable and the runner without
   an image runs nothing.
10. **Packing + refdes/BOM + the cascade rule + the fan-out check = FEAT-041.**
    Spine rows S9, S16 and S17 are one batch report over one package table.
11. **The analog solver + sources/models + the determinism gate + the external
    oracle = FEAT-046.** The determinism gate and the external oracle are the
    same test discipline; splitting them produces a solver with no witness.
12. **The manufacturability gate folded into FEAT-042.** A one-to-two-week gate
    is not a feature; it is the acceptance criterion of the emitters.
13. **The board on-ramp (#264) folded into FEAT-023.** Arming external tools in
    CI and driving a real board are one toolchain-integration surface.
14. **Formal property checking folded into FEAT-053 and FEAT-034.** A separate
    formal feature would share more than half its tasks with both.
15. **CAP-10 and CAP-11 kept SEPARATE despite sharing FEAT-045.** They have
    distinct acceptance tests and distinct minimum analog stages - output is
    demonstrable with no solver at all, input is not usefully demonstrable
    below the mixed-signal stage. Merging them would hide that the cheap one is
    genuinely cheap.
16. **The engine constant-factor stack (FEAT-030) kept SEPARATE from the
    quadratic-path fixes (FEAT-005).** They touch adjacent files, but FEAT-005
    lands immediately under D6 as ordinary defect work while FEAT-030 is a
    12-20 week program gated on the measurement gate. Merging them would gate
    a two-week fix behind a five-month program.

---

## COUNTS

**19 capstones (CAP-00 through CAP-18, no gaps).**
**60 features (FEAT-001 through FEAT-060, no gaps).**
**112 tasks (TASK-0001 through TASK-0112, no gaps).**

Counted, not asserted: `ls docs/plan/capstones/` is 19 files and
`ls docs/plan/features/` is 60 files after CAP-18's three features were written.
**FEAT-061 was never minted.** It is *reserved* inside CAP-18's own text for a
future eye/BER capstone that does not exist; there is no FEAT-061 document, no
row here and no filed issue, so the id space ends at FEAT-060 and the count is
60 rather than 61. See the CAP-18 addendum below.
**34 open GitHub issues, all 34 mapped, none untouched.** See `issue-map.md`
for the per-issue mapping and the plan-ids-with-no-issue list.

Cross-checks a reader can run: every feature in Table 2 is referenced by at
least one capstone's consumed-by column; every task in Table 3 names at least
one feature in its enables column; every FEAT id named in Table 3 exists in
Table 2; every CAP id named in Table 2 exists in Table 1. The id space is
closed - no author may mint an id outside these ranges.

---

## LINK-PHASE RECORD

Everything below was decided by the link pass, after the seventeen capstone
documents, the feature documents and the 112 task documents were authored in
parallel against this registry. It is recorded here so that no later author
re-derives it or reverses it silently.

### The grade rule

Where a capstone and a feature disagreed on whether the feature is *required*
or *beneficial*, **the consuming capstone's grade is authoritative** and the
feature was edited to match. The reason is structural: a capstone declares its
own acceptance test and therefore knows what it must have, while a feature is
written without sight of every consumer's acceptance test. Nineteen
disagreements were reconciled this way. One reversal went the other direction
and is recorded as an editorial decision rather than as the rule:

- **CAP-00 / FEAT-008 was promoted, not demoted.** CAP-00 graded the editor
  decomposition *beneficial* on the grounds that its 12-20 mw is booked against
  CAP-01 and CAP-04. That reads wrong on the maintainer's highest-priority
  capstone, whose four largest constituent issues (#84, #91, #162, #101) are
  exactly that feature. CAP-00's grade was raised to **required** and its band
  raised from 24-42 mw to **35-62 mw**, which is the sum of its nine required
  features (FEAT-001 1-2, 002 1-2, 003 2-4, 005 2-3, 006 3-5, 007 3-6, 008
  12-20, 009 5-10, 011 6-10). FEAT-010 and FEAT-012 remain *beneficial* on
  CAP-00 with their cost booked against CAP-06 and CAP-01.

### The feature-side task table

A feature's `## Prerequisite tasks` table lists **every task whose `## Enables
features` names that feature** - which includes tasks that *exercise* the
feature rather than precede it. Those rows are labeled "Consuming task" in the
why column. The alternative, a table that means "strictly precedes", would have
broken the both-directions rule for five real edges (TASK-0080 and TASK-0082 on
FEAT-034, TASK-0084 on FEAT-032, TASK-0111 and TASK-0112 on FEAT-034).

### Cost arithmetic

Every capstone and every feature now carries a `**Cost reconciliation.**` line
as the last bullet of its `## Evidence` section, printing the arithmetic rather
than averaging it away. Two facts hold across the whole plan and are stated once
here:

1. **A feature's band is not the sum of its tasks.** Tasks are the leading,
   dividable slices of a feature - what one person can sit down and finish -
   and the task id space is closed at TASK-0112. Where the band exceeds the task
   sum, the residual is real work with no id. Where the task sum exceeds the
   band, tasks are shared between features and counted once at the task level.
2. **A capstone's marginal band is not the sum of its required features.** Most
   required features are shared spine, booked once against whichever capstone
   funds them first. "Marginal" is the incremental cost given the spine is
   funded; the standalone figure is the other end of that range.

### CAP-17 and FEAT-054 through FEAT-057

CAP-17 was added by the maintainer after this registry closed. The link phase
kept it, normalized its three tables to the capstone template, and created the
four feature documents its prerequisite table demanded. Consequences a reader
must know:

- Its priority is recorded as **18**, meaning "appended, not yet ranked". It
  collided with CAP-11 at priority 12 as authored. CAP-17's own open decision 5
  records the ranking as a maintainer decision.
- **FEAT-054 through FEAT-057 have no tasks.** The task id space is closed and
  the link phase did not mint any. Each of the four says so in its
  `## Prerequisite tasks` table rather than leaving it blank. Minting them is a
  maintainer decision.
- **FEAT-054 overlaps FEAT-030 and FEAT-026 and must not be funded twice.** The
  flat array layout is the same code the engine constant-factor work needs;
  whichever is funded first pays for it and the other is re-scoped. This is
  stated in FEAT-054, in FEAT-030's consumed-by row and in CAP-17.

### CAP-18 and FEAT-058 through FEAT-060

CAP-18 was added by the maintainer after this registry closed, in the same shape
as CAP-17 and by the same route. Its three features were written later still,
under maintainer ruling D14 (*"this is obviously a mechanical fix"*), by
transcription from `docs/plan/capstones/CAP-18-net-that-stopped-being-a-wire.md`
and `docs/plan/evidence/highfreq-determination.md`. Consequences a reader must
know:

- Its priority is recorded as **19**, meaning "appended, not yet ranked",
  following CAP-17's precedent. The recommendation in its own text is to rank
  the demo slice (FEAT-058) early and leave the rest unranked until the lint's
  noise rate on `examples/` is measured.
- **FEAT-058 through FEAT-060 have no tasks.** The task id space is closed at
  TASK-0112 and none was minted, following the recorded FEAT-054..FEAT-057
  precedent. Each of the three says so in its `## Prerequisite tasks` table
  rather than leaving it blank. Minting them is a maintainer decision.
- **The capstone document's registry-delta line claimed "61 features
  (FEAT-001..FEAT-061)". That count was never true and is corrected in place.**
  57 documents existed at `2d0ca9d`; three were added here; the total is **60**.
  FEAT-061 is reserved in prose for an eye/BER capstone and was never written,
  so the claimed 61 over-counted by four at the time it was written and by one
  now.
- **The three bands are the capstone's own §7.1 figures and two of the three do
  not reconcile with the same document's stage table.** Each feature prints both
  derivations in its Cost reconciliation rather than adjusting either. Summarised:
  FEAT-058 3-6 mw against a staged 3.5-7 (and an itemised 2.5-5); FEAT-059
  2-3.5 mw plus a separately-priced 0.5-1 mw trace row against a staged 3.5-5.5;
  FEAT-060 5.5-9.5 mw, which agrees on both derivations.
- **The three are sequenced by PERMANENCE, not by cost**: FEAT-058, then
  FEAT-060, then FEAT-059. FEAT-059 is the cheapest and is the only one
  committing a frozen save tag, a mandatory palette entry and a K9 obligation.
  A scheduler reading this table by cost alone will invert it; that inversion is
  a REPLAN, not a preference.

### Citation corrections

- **#233, #242 and #244 are pull requests, not issues.** Documents citing #233
  (the zero-margin coverage-floor flake) and #244 (the floor reconciliation and
  the 0.50 headless line milestone) as issues were corrected to say "PR". No
  cited number was fabricated: every number cited anywhere in `docs/plan/` was
  resolved against the live tracker - 34 open issues, 127 closed issues, and
  three pull requests.
- **#84's title contains a stale measurement.** The title says `SimpleEditor` is
  4,119 lines; it is 5,852 at HEAD. Citing the title verbatim is correct;
  citing 4,119 as a current measurement is not.

### Duplicates

The pairwise similarity scan over all 187 documents found **no two documents at
the same level describing the same work**. This registry did its job: parallel
authors did not duplicate. One duplicate was created and removed before the link
pass (a second FEAT-004 written under the title "shared net-partition IR", which
its author deleted in favor of the interchange author's copy). The one remaining
near-duplicate is a deliberate overlap, recorded above: FEAT-054 against
FEAT-030.
