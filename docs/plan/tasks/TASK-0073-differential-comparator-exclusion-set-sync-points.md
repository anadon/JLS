# TASK-0073 - The differential comparator, exclusion set and sync points

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0072

## Deliverable

A comparator that names the exact instruction and the exact field, an exclusion
set under its own ratchet, and a verdict lattice in which "I don't know" is
never a pass.

1. **`src/jls/parity/TraceDiffer`** - reads two `RetireRecord` traces and
   reports, on divergence, **the first differing retirement index, both records
   side by side, and the names of the differing components**. A differ that
   reports "traces differ" has done nothing `cmp` could not; first-divergence
   reporting is the whole product.

2. **`src/jls/parity/ExclusionSet`** - exactly four entries, each with its
   reason string: `mcycle` (counts cycles; all timing may differ), `minstret`
   (compared directly per §3.5, and written at different points in the two
   tiers), `mtime` (driven from simulated time, which differs by construction),
   `mtimecmp` (written by the guest as an offset from `mtime`). The set is
   **printed in every report** - an exclusion a user cannot see is policy hidden
   in comparison code.

3. **`src/jls/parity/Verdict`** - `PARITY_HELD | DIVERGED_AT_INDEX | UNKNOWN |
   NOT_COMPARABLE`, with an `isPass()` implemented as an exhaustive switch **with
   no `default` arm** that returns `true` only for `PARITY_HELD`. Adding a fifth
   verdict is then a compile error at the one place that decides pass/fail.

4. **`src/jls/parity/SyncPointDigest`** - at each declared instruction index and
   at exit, both implementations present PC, all GPRs, every implemented CSR
   minus the exclusion set, and a digest over the non-volatile memory regions
   declared in the machine definition. A run that reaches different sync points
   on the two tiers has already failed and reports `NOT_COMPARABLE`, not
   `UNKNOWN`.

5. **`-diff-against FILE`** in `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:759-788`) and `apply` (`:1024-1134`). The verdict goes
   on the outcome line and into a machine-readable report - the sixth consumer
   of the shared report channel.

6. **The null family, committed and asserted to fail.**
   `test/jls/parity/nulls/` carries four deliberately, *subtly* wrong
   `jls.mach` implementations - one sign-extension defect, one store byte-lane
   defect, one CSR write-side-effect defect, one branch-target off-by-one - each
   with a test asserting **the report text**, naming the right index and the
   right field. An implementation that returns constant zero proves only that
   the harness is connected and does not count.

7. **The independent third object, and its home.** The harness needs a reference
   that does not share authorship with the design, or it proves the generator
   self-consistent rather than either model correct. Commit compiled conformance
   ELFs and their reference signatures as fixtures for the **blocking** lane;
   keep the toolchain-bearing RISCOF run **informational-nightly**. It must not
   be homed under `riscv/`, which TASK-0025 deletes.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-034 | The verdict. Two traces without a comparator are two files. |
| FEAT-053 | Grading an unfamiliar design on "does it agree with the reference, and where does it first stop" is the same machinery, aimed at a submission instead of at a tier. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0072 | The retirement record and its trace emission | This reads two traces in a format only TASK-0072 defines and writes, and compares fields only its record declares. |

## Acceptance test

`test/jls/parity/ExclusionSetRatchetTest#theExclusionSetIsExactlyTheContractTable()`
- asserts the code's set against `docs/parity-contract.md` §2.5 **in both
directions**, so `E` may shrink freely and cannot grow without a diff a reviewer
sees. A mismatch is a build failure.

`test/jls/parity/TraceDifferTest`:
- `firstDivergenceNamesTheIndexAndTheFields()` - two traces differing at index
  N in exactly two components; assert the report contains N, both records, and
  exactly those two component names.
- `identicalTracesHoldParity()` and `aShorterTraceIsNotComparable()`.
- `theReportPrintsTheExclusionSet()`.

`test/jls/parity/NullImplementationTest#everyCommittedNullIsRejectedWithItsOwnFieldNamed()`
- a `@ParameterizedTest` over the four nulls asserting **failure**, and asserting
the report string rather than the boolean. `assertFalse(agrees)` leaves the
reporting path - where the value is - untested, and the 80/82 pitest thresholds
(`pom.xml:813-814`) will find survivors there.

`test/jls/parity/VerdictTest#unknownAndNotComparableAreNotPasses()` - the direct
statement of the lattice rule.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the differential comparator, the exclusion set and sync points | **no issue** |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | closes (in part) - the issue's "differential oracle" is this comparator; the worked example is its first client |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | overlaps - the verdict and the report are the same channel the test panel renders; build the report once |
| 111 | Windows platform parity: promote the headless lane, arm HDL-sim + display suites, JaCoCo floor, JDK-26 leg | informs - "bit-identical" currently means bit-identical on one platform; nothing in the tree asserts a simulation is identical across a JDK upgrade or an OS. The experiment is cheap and the CI jobs already exist |

## Notes

- **Excluding a field does not exclude its effect.** `mtime` is excluded from
  the comparison, but a guest that reads it and branches on it produces an
  architectural divergence that is a real defect. This is not a technicality: it
  decides the comparison regime (`docs/parity-contract.md` §3.6) and it is why
  `mtime` must be driven by simulated time on both sides (TASK-0070).
- **Sync point zero is the trap that bites first.** JLS supplies a reset the
  design does not have, so two machines can agree on every record from
  instruction 1 and disagree at instruction 0. The machine definition must state
  the power-on value of every architecturally visible register, and on the drawn
  side that is TASK-0077's honest reset.
- **Retirement-boundary sampling.** A settling value that resolves before the
  sync point is permitted microarchitectural state; the same value surviving
  into a committed `rd_value`, `mem_wdata`, `pc_after` or `trap` is a real
  defect. Write both rules into the contract before any code, or the first
  divergence report will be argued about instead of fixed.
- **A guard that no boundary implementation touches the event queue.**
  `Simulator.post` permitted; `eventQueue`, `dupCheck`, `poll` and
  `runEventLoop` forbidden (`src/jls/sim/Simulator.java:25,27,215-241`). That is
  the mechanical proof that a behavioral binding is a model change and not a
  second execution strategy - a fact rather than a claim.
- **Both implementations can be wrong together** - recorded as a known weakness
  of the contract. Deliverable 7 is the mitigation, and it is why the third
  object is scoped into this task rather than deferred.
- **`docs/parity-contract.md` is not normative at HEAD.** Its status line says
  it binds nothing until the `ARCHITECTURE.md` decision block of its §8.3 is
  recorded. Landing this task is what ratifies it; the commit that merges the
  harness records the decision block, and whatever the built harness proves
  wrong is corrected in the same commit rather than in a new document.

## Evidence

- `docs/parity-contract.md` §2.5 - the four-row exclusion table with reasons and
  the three rules that keep it from being an escape hatch; §2.6 - sync points as
  indices, never times; §3.1-§3.5 - what must be bit-identical; §3.6 - the two
  comparison regimes; §5.2 - first-divergence reporting, the event-queue guard,
  and the exclusion-set ratchet; §5.3 - the deliberately-failing null test, with
  both requirements that make it real; §9.3 - both implementations can be wrong
  together; §5.1 - the unverified cross-platform determinism assumption.
- `docs/virtual-hardware-parity.md` P16 - the verdict lattice with "UNKNOWN and
  NOT_COMPARABLE are never passes", the two rules to write into the contract
  before any code, and the third object's homing problem.
- `pom.xml:813-814` - `mutationThreshold` 80, `testStrengthThreshold` 82, which
  is what makes "assert the report text, not the boolean" a build requirement
  rather than advice.
- `src/jls/sim/Simulator.java:25,27,215-241` - the queue members the guard
  forbids.
- `src/jls/JLSStart.java:759-788,1024-1134` - the flag table and `apply`.
