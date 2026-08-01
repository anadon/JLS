# TASK-0072 - The retirement record and its trace emission

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

A record whose *type* enforces the permitted-to-differ set, and a trace emitted
through the hook `BatchSimulator` already overrides - with zero change to
`jls.sim`.

1. **New package `src/jls/parity/`** with `package-info.java` carrying
   `@NullMarked`. The record's own compilation unit imports **nothing from
   `jls.sim` or `jls.elem`**, so `jls.mach` (TASK-0070) can depend on it without
   losing leaf status.

2. **`RetireRecord`** - a Java `record` with exactly the twelve components of
   `docs/parity-contract.md` §3.1: `order`, `pc_before`, `pc_after`,
   `insn_word`, `rd_index`, `rd_value`, `mem_addr`, `mem_rmask`, `mem_wmask`,
   `mem_wdata`, `privilege`, `trap`. **No component for cycles, simulated time,
   pipeline state or cache state.** That absence is the enforcement mechanism:
   §4's permitted-to-differ set is made unrepresentable by the type, so
   over-constraining parity is a compile error rather than a review finding.

3. **`RetireTraceRecorder extends Simulator`** - a **third** `Simulator`
   subclass, constructed exactly as `BatchSimulator` is: override `probeSample`
   (`src/jls/sim/Simulator.java:285-287`, overridden at
   `src/jls/sim/BatchSimulator.java:294-312`) and `afterEvent`
   (`:269-270`, overridden at `:140-180`). Nothing in `src/jls/sim/` changes.

4. **The reserved retirement-net vocabulary.** The drawn machine names its
   retirement nets with a frozen prefix - `rvfi.order`, `rvfi.pc_before`, ... -
   one per record component plus `rvfi.strobe`. The recorder samples a record
   when and only when `rvfi.strobe` settles to its asserted value; it does
   **not** build a record per `probeSample` callback. Probe names are validated
   on attach by the mechanism TASK-0008 installs.

5. **The canonical trace writer** - untimestamped canonical text, one record per
   line, deterministic component order, written **incrementally to a sink** and
   never accumulated whole (`docs/parity-contract.md` §3.1: "the comparison is a
   `diff`, and the first differing record is the answer").

6. **One writer, two producers.** `jls.mach.Runner` (TASK-0070) emits through
   the same writer, so the two traces are the same format by construction rather
   than by convention.

7. **`-rvfi FILE`** added to `JLSStart.FLAGS` (`src/jls/JLSStart.java:759-788`)
   with the matching `case` in `apply` (`:1024-1134`), plus the
   `docs/batch-interface.md` CHANGELOG entry its §6 requires for a new flag.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-034 | The comparison alphabet as a running artifact. Every other part of the harness reads these records. |

## Prerequisite tasks

None. `RetireRecord` is a leaf type and the recorder is a sibling of
`BatchSimulator` built on hooks that exist at HEAD.

TASK-0070 is a **co-producer**: `jls.mach.step` returns the same record. Whichever
lands first defines the type; the second consumes it. Neither reads the other's
output, so neither blocks.

## Acceptance test

`test/jls/parity/RetireRecordTest`:

- `theRecordHasExactlyTheContractComponentsAndNoOthers()` - reflection over
  `RetireRecord.class.getRecordComponents()`, asserting the exact twelve names
  from `docs/parity-contract.md` §3.1 in order, **and** that no component name
  matches `(?i)cycle|time|stall|pipeline|cache|latency`. This is the test that
  turns "permitted to differ" from a paragraph into a build failure.

`test/jls/parity/RetireTraceRecorderTest`:

- `oneStrobeProducesExactlyOneRecord()` - drives a fixture circuit whose
  `rvfi.*` nets are probed and asserts one record per asserted strobe, with
  `order` strictly increasing by one, no gaps and no reuse.
- `transientsBetweenStrobesAreNotSampled()` - **the regression that pins
  deliverable 4**. Wiggle a probed `rvfi.rd_value` net several times between two
  strobes and assert exactly one record, carrying the settled value. Fails
  against any implementation that records per `probeSample` callback.
- `theRecorderDoesNotInheritTheTraceGate()` - asserts records are produced with
  `JLSInfo.printTrace` false and no VCD file set. `BatchSimulator.probeSample`
  returns early unless one of those is on (`:298-300`); the new subclass must
  not copy that guard.
- `jlsSimIsUnchanged()` - a source ratchet asserting no member added to
  `src/jls/sim/**` by this task, in the `SocketConfinementRatchetTest` idiom.

`test/jls/VcdExportGoldenTest#clockedRegisterVcdMatchesGoldenByteForByte()` must
stay byte-identical: the recorder is a sibling, not a modification, and if a VCD
golden moves, something was changed in the shared engine.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the retirement record and its trace emission | **no issue**. #232 covers only the value representation - not the queue, the fidelity boundary or the parity harness |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on - the worked example is the first design that carries `rvfi.*` nets, and the shipped student feature ("JLS names the exact instruction where your drawn CPU first disagreed") is its payoff |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | overlaps - a parity verdict is a sixth consumer of the same report channel the test panel needs |

## Notes

- **`WireNet.propagate` overwrites its cached value eagerly and calls
  `probeSample` on every settling** (`src/jls/elem/WireNet.java:512-527`), once
  per probed wire, with one field check per wire. A naive per-callback sampler
  therefore sees combinational transients and produces spurious divergences that
  look exactly like real bugs. Strobe-gated sampling is not an optimization; it
  is correctness.
- **`SimEvent.sequence` is a `static`** (`src/jls/sim/SimEvent.java:87`), shared
  across every `Simulator` in a JVM. It does not affect ordering within one run
  (the comparator is `(time, seq)` and `seq` is monotone), but it does mean a
  recorder constructed lazily mid-run inherits a nonzero base. Construct it
  before `initSimulation`. TASK-0074 makes it per-simulator; do not depend on
  that here.
- **`probeSample` is `public`, `afterEvent` is `protected`**
  (`src/jls/sim/Simulator.java:285,269`). The recorder overrides both at their
  existing visibilities; widening either is a change to `jls.sim` and defeats
  the point.
- **`jls.sim` sits at 0.930/0.920/0.845 with pitest 80/82**
  (`pom.xml:449-471,813-814`). `jls.parity` should be born at the same bar -
  it is pure logic with no GUI surface and no excuse.
- **Four RVFI fields are deliberately not adopted**, each with its stated cost:
  the `rs1`/`rs2` family (first-divergence index reported later than the true
  one), `rvfi_mem_rdata` (a load that reads wrong data and discards it is
  invisible), the `rvfi_csr_*` family (a CSR written wrongly and read back
  before the next sync point is attributed to the *reading* instruction), and
  `rvfi_halt`/`rvfi_intr`/`rvfi_ixl` (no material cost). Do not quietly add one;
  the twelve-component assertion will stop you, which is the point.
- Trace files are untimestamped canonical text. Do not add a header with a
  version, a date or a host name - the VCD exporter's deliberate omission of
  `$date`/`$version` (`src/jls/sim/BatchSimulator.java:420-422`) is the
  precedent.

## Evidence

- `docs/parity-contract.md` §3.1 - the twelve-field RVFI alphabet, the "record
  with no field for cycles" enforcement statement, and the not-adopted table
  with costs. §5.1 - point 2 is "load-bearing and costs nothing in `jls.sim`",
  with the mechanism named.
- `src/jls/sim/Simulator.java:269-270` (`afterEvent`), `:285-287`
  (`probeSample`, a no-op with a javadoc that explains the batch override).
- `src/jls/sim/BatchSimulator.java:140-180` (`afterEvent` override),
  `:294-312` (`probeSample` override), `:298-300` (the trace gate),
  `:420-422` (the timestamp-free header decision).
- `src/jls/elem/WireNet.java:512-527` - the per-wire probe callback in
  `propagate`, and its "one field check per wire" comment.
- `src/jls/sim/SimEvent.java:87` - the static sequence counter; `:88-95` - the
  comment explaining why `equals`/`hashCode` exclude `seq`.
- `src/jls/JLSStart.java:759-788,1024-1134` - the flag table and its `apply`
  switch.
- `docs/batch-interface.md` §6 - the CHANGELOG requirement for a new flag.
