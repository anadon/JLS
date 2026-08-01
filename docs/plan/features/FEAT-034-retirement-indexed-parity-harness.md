# FEAT-034 - Retirement-indexed parity harness and `RetireRecord`

**Status:** proposed | **Cost:** 10-16 mw | **Owner program:** P5 |
**Spine rank:** S23

## Capability delivered

Two implementations of the same machine - a drawn structural circuit and an
independent reference - are compared per retired instruction rather than per
cycle, so that a fast implementation and a slow one can be held to the same
answer without being held to the same microarchitecture. The comparison alphabet
is a record that carries architectural state and *has no field* for cycles,
pipeline or cache state, so over-constraining the comparison is a compile error
rather than a debugging session. A knowingly wrong implementation ships beside
it and the harness must reject it, so the harness cannot pass vacuously.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | "Booted Linux" is only a claim if something checked the machine against a reference; a transcript alone proves the transcript |
| CAP-03 | required | The ternary machine has no external reference at all, so its emulator is the counterparty and this is the comparison |
| CAP-04 | beneficial | The breadboard build is a third implementation of the same boundary and compares the same way |
| CAP-08 | required | An imported core is unfamiliar by definition; a differential oracle is the only honest acceptance test |
| CAP-09 | required | This *is* the capstone's mechanism - verifying a design you did not write is a differential and property claim |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-033 | The comparison needs a counterparty. `jls.mach` is where the independent implementation lives, and the harness compares against it |
| FEAT-031 | Parity is a property of a *boundary*; the fidelity binding is what defines which boundary is under comparison, and its null-toggle gate is the same discipline as the null test |
| FEAT-009 | A comparison that costs more than the run it checks does not get run. The affordability of a per-retirement comparison is measured, not assumed |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0072 | The retirement record and its trace emission | The record type and its emission through the existing probe-sample hook rather than a new seam |
| TASK-0073 | The differential comparator, exclusion set and sync points | The comparator, the enumerated per-bit exclusion set under its own ratchet, and the digest comparison at declared sync points |
| TASK-0070 | The machine package and its reference runner | Shared with FEAT-033: the parity counterparty must exist before there is a comparison |
| TASK-0069 | Transcript capture, replay and the console pane | Shared with FEAT-032 and FEAT-008: host input logged in retirement index, which is what makes a run with input reproducible |

## Acceptance criteria

1. `RetireRecord` carries the retirement field list and **no field** for cycle
   count, pipeline state or cache state. A field added later is a source change
   a reviewer sees, which is the entire design.
2. The trace is emitted through the existing probe-sample hook. No new
   observation seam is introduced into the simulator.
3. The comparator reports the first divergence by retirement index, with both
   sides printed, rather than reporting that a run failed.
4. The per-bit exclusion set is explicitly enumerated, is under its own ratchet
   test, and grows only with a recorded reason. An exclusion set that can grow
   silently makes every green run meaningless.
5. Full architectural state is compared as a digest at declared sync points, so
   a divergence that does not surface in a retirement delta is still caught.
6. **The null test.** A knowingly wrong implementation ships in the test tree and
   its test asserts *failure*. A harness with no null test is worse than no
   harness, because it converts an unexamined assumption into a green check.
7. Host input is logged in retirement index and replays deterministically, and a
   ratchet forbids goldens produced in live mode.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | closes - its differential-oracle half is this harness's first application |
| - | the retirement record, the comparator, the exclusion set and the null test | **no issue** |

Recorded decision context: `docs/vcd-interop.md:19-24` rejects live
co-simulation under #63 ("Graders must not depend on interacting with a running
simulation"). That is a **closed decision, not an open issue**, and it
contradicts `docs/grand-architecture.md`. This feature does not need live
co-simulation - it compares two traces offline - but the contradiction is
in-repo and belongs on the record.

## Design notes

**Do not restate `docs/parity-contract.md`; implement it.** That document
already specifies the objects (`D`, the bound boundary, `P`, the input log `I`
indexed by retirement, the exclusion set `E`, the sync point), what must be
bit-identical (§3.1-§3.6), what is permitted to differ (§4), the four
observation points and the harness (§5.1-§5.2), the null test (§5.3), and what a
binding must refuse (§6). This feature's job is to build the mechanism it
governs. Its status line is load-bearing: it is a **proposed** normative
contract, not yet ratified, and it becomes normative only when the
`ARCHITECTURE.md` decision block described in its §8.3 is recorded. Recording
that block is an open decision this feature must surface, not resolve
unilaterally.

The seam already exists and must be used rather than duplicated.
`Simulator.probeSample` is at `src/jls/sim/Simulator.java:285`, with
`afterEvent` at `:269`, `beforeEvent` at `:252` and `beforeReact` at `:261`;
`BatchSimulator` overrides `probeSample` at `:295` and `afterEvent` at `:140`.
The contract document verifies all six anchors in its §1.3 and states plainly
that `RetireRecord`, any `Differ`, any parity harness and any behavioral RV32
model **do not exist** at HEAD. That is the honest starting point.

Retirement indexing rather than cycle indexing is the load-bearing choice and it
is prior art, not invention: RVFI, RVVI and the ARM PV-versus-cycle distinction
all index the comparison at instruction retirement precisely so that
implementations with different timing can be compared at all. The corpus's
survey of gem5, TLM-2.0 and the RISC-V verification stack is in
`06-roadmap-thread.md` and `07-mvl-determination.md`; the synthesis is
`BRIEF.md` §6.

Four tasks *consume* this harness rather than being prerequisites of it, and the
registry lists them as enabling FEAT-034 for that reason: TASK-0080 (the
headless boot run and its transcript comparison), TASK-0082 (the ternary
reference emulator), TASK-0111 and TASK-0112 (the grading and property harnesses
in FEAT-053). They are its first users and their acceptance criteria are where
the harness is proven, but none of them blocks it.

## Risks

- **Both implementations can be wrong together.** The contract names this as a
  known weakness (§9.3). The mitigations are external: the conformance corpus,
  the external toolchain oracle in FEAT-023, and comparing against an
  implementation nobody on this project wrote.
- **The exclusion set is the failure mode of every differential harness.** Each
  excluded bit is a place the two implementations are allowed to disagree, and
  the set only ever grows under pressure. Criterion 4's ratchet is the only
  defense and it must be a required check, not a convention.
- **The contract is unratified.** Building the mechanism it governs before its
  §8.3 decision block is recorded risks building to a specification that then
  changes. The recommendation is to record the decision block first - it costs a
  decision, not a week.
- **Device models sit outside the boundary mechanism** (§9.2), so a divergence
  caused by a device is attributed to the boundary. The console and byte port in
  FEAT-032 are where that shows up first.

## Evidence

- The specification this feature implements: `docs/parity-contract.md` - status
  line (proposed, unratified) at `:1-10`; §1.3 what exists at HEAD at `:79-98`;
  §2.4 input log indexed by retirement at `:201`; §2.5 exclusion set at `:229`;
  §2.6 sync point at `:252`; §3.1-§3.6 at `:269-401`; §4 at `:402`; §5.1-§5.2 at
  `:433-513`; §5.3 the deliberately-failing null test at `:514`; §6 at `:572`;
  §8.3 the ratifying decision block at `:739`; §8.4 the golden as a live
  dependency at `:775`; §9.1-§9.3 at `:794-822`.
- The existing observation seam: `src/jls/sim/Simulator.java:285`
  (`probeSample`), `:269`, `:261`, `:252`; `src/jls/sim/BatchSimulator.java:295`,
  `:140`.
- Nothing implements any of it at HEAD: `docs/parity-contract.md:79-98`.
- The live-co-simulation contradiction: `docs/vcd-interop.md:19-24` versus
  `docs/grand-architecture.md`; `BRIEF.md` §8.
- Retirement indexing as prior art: `BRIEF.md` §6; `06-roadmap-thread.md`.
- Cost and spine placement: `10-capstone-plan.md` §2.1 row S23; owner P5
  (verification, proof and coverage), `docs/capability-roadmap/AMENDMENT.md:155`.
- Normative event model, unchanged by this feature:
  `docs/simulation-semantics.md`.
