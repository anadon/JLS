# TASK-0084 - The monitor program

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0068, TASK-0082, TASK-0083

## Deliverable

A hand-written single-tasking monitor, in JLS-T3 assembly, that runs on the drawn
machine and prints to the console. Single-tasking is not a limitation to
apologize for: it removes scheduling, preemption, address spaces, a TLB and every
source of nondeterminism that makes a booted-Linux transcript hard to compare.

1. **`machines/cpu-t3/qdos/`**, assembled by TASK-0082's in-jar assembler into a
   `.timg` that is loaded into the drawn machine's instruction memory. Sources are
   `.s` files in the tree, not a generated blob.

2. **Reset, trap and console paths in assembly.** Reset vector, stack setup,
   banner, the `ECALL` dispatch table, and a **polled** console driver -
   transmit, receive, status, no interrupt controller, because that is the whole
   of TASK-0068's element and adding an interrupt path here would be inventing a
   device the machine does not have.

3. **The `ECALL` table, ~10 calls**, each with a fixed number, a fixed register
   convention and a defined error return: `exit`, `putchar`, `getkey`,
   `write`, `read`, `brk`, `memcpy`, `memset`, `time`, `version`. The table is
   **data in the monitor and a constant in a test**, so a renumbering is a
   failing test rather than a mystery.

4. **The prompt and six built-ins that need no block device**: `HELP`, `MEM`
   (the memory map and free space), `TRIT n` (print `n` in balanced ternary),
   `PEEK addr`, `POKE addr val`, `GO addr`. `DIR`, `TYPE`, `COPY`, `DEL`, `REN`
   and `RUN` are the filesystem set and are **explicitly out of scope here** -
   they need a `BlockDevice` element that is nobody's task in this registry.
   Record that as a gap rather than half-implementing a filesystem.

5. **A memory-resident `.TX3` loader.** A flat executable already present in
   memory is relocated to the load address and entered. That is the loader
   mechanism; where the image comes from is the block device's problem.

6. **`TRIT` is the demonstration and it must be exact.** `TRIT -5` prints `-++`,
   because -5 = -9 + 3 + 1. A student who has never seen a number system without
   a sign bit has just seen one, in one command, on a machine they can open and
   read.

7. **A transcript golden.** The session `HELP`, `MEM`, `TRIT -5`, `TRIT 21523360`,
   `POKE`/`PEEK` round trip, `GO` into a three-instruction program - captured
   through TASK-0069's retirement-indexed replay and compared byte for byte, on
   both the behavioral and the structural binding of the CPU boundary.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-039 | The end state. A drawn CPU with no software on it is a datapath; the monitor is what makes it a computer a person can type at. |
| FEAT-032 | The console's first non-trivial consumer, and the one that exercises the polled status path, the transmit-while-busy case and the replay determinism contract at the same time. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0068 | The console element | The monitor's driver reads and writes a transmit/receive/status port that exists only after that task. There is no other byte door in the tree. |
| TASK-0082 | The ternary reference emulator and assembler | The monitor is assembly; the assembler is that task's. It is also developed against the emulator long before the drawn machine is fast enough to iterate on. |
| TASK-0083 | Draw the ternary CPU | "Runs on the drawn machine" is half the deliverable, and the structural transcript golden has no subject without it. |

## Acceptance test

`test/jls/machines/QdosMonitorTest`:
- `bannerAndPromptMatchTheGoldenOnTheBehavioralBinding()` - reset to first
  prompt, byte-compared.
- `theSameTranscriptIsProducedOnTheStructuralBinding()` - the **same golden**,
  the CPU boundary flipped. This single comparison is the parity claim at this
  capstone's scale, and it is cheap here because the guest is small.
- `tritPrintsBalancedTernary()` - a table-driven case list including `-5` ->
  `-++`, `0` -> `0`, `+21523360` and `-21523360` (the symmetric extremes), and a
  value whose printed form has a leading zero trit.
- `everyEcallNumberMatchesTheDeclaredTable()` - the monitor's table parsed out of
  the assembly source and compared against the test's constant, so a renumbering
  fails here.
- `peekPokeGoRoundTripsThroughMemory()`.
- `consoleStatusIsPolledAndNeverBusyWaitsForever()` - assert a bounded
  instruction count between a keystroke's arrival in the replay log and its echo,
  so a driver that spins on a status bit that never clears fails as a test rather
  than as a six-hour CI job.
- `clockPeriodDoesNotChangeAnyTranscriptByte()` - the same falsification guard
  TASK-0080 uses. If it fails, the golden encodes time and the structural
  comparison means nothing.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the QDOS monitor, its `ECALL` table and its transcript golden | **no issue.** The ternary program and the host-byte-port program are both untracked. |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | overlaps - a monitor session is exactly the kind of thing a test panel would drive interactively; the transcript golden is the headless half and must land first. |

## Notes

- **No filesystem in this task, and say so out loud.** The full monitor scope -
  a contiguous-extent directory of 32-byte entries with start sector and length,
  `DIR`/`TYPE`/`COPY`/`DEL`/`REN`/`RUN`, and a `.TX3` loader reading from disk -
  needs a `BlockDevice` element over a host file. That element is **not in this
  registry** and prices at 3-5 weeks on its own. The built-in set here is chosen
  precisely so nothing in it lies about having storage.
- **A contiguous-extent directory, not FAT, when the filesystem does land.**
  FAT's chain walk buys nothing at this scale and costs about a week.
- **The console is one door granted at invocation, never a property of the
  circuit file.** A `.jls` that names a host file or a device would be a circuit
  that touches the host on open. TASK-0067's seam is where the grant lives; the
  monitor may assume the door exists and may not assume where it goes.
- **Determinism is the reason the transcript is the contract.** No automated test
  can prove a human typed. The replay is the assertion, the GUI session is the
  demo, and a ratchet must forbid any golden produced in live console mode
  (TASK-0080 owns the tree-wide assertion; this task's goldens are its first
  subjects).
- **Do not write the banner's version string as a literal in two places.** It is
  the classic drift and this tree already single-sources the equivalent through
  `JLSInfo.versionString` from `jls/version.properties` (`src/jls/JLSInfo.java:16-49`).
  The monitor's version is the ISA spec version from TASK-0081, referenced once.
- **Iterate on the emulator, gate on the machine.** At the structural tier echo is
  roughly 0.18 s per character; a monitor debugged one keystroke at a time on the
  drawn machine is a week of waiting. Develop against `jls.mach.t3`, and let the
  structural transcript be the gate rather than the loop.

## Evidence

- `docs/parity-contract.md` §2.4 - the input log indexed by retirement, never by
  time; §3.3 - the guest-visible output byte stream as a bit-identical object.
- `docs/virtual-hardware-parity.md` L3 (`:645-730`) - the host boundary as one
  door granted at invocation, and why a device must not be a file property.
- `src/jls/JLSInfo.java:16-49` - the single-sourced version idiom the banner
  should follow.
- `docs/batch-interface.md` §1 - the stream and exit-status contract a headless
  monitor session must not violate.
- `src/jls/elem/ElementRegistry.java:38-77` - the 35 registered types at HEAD;
  none of them is a console or a block device, which is why both are new
  `LogicElement` subclasses and each costs a registration tax rather than a
  format version.
