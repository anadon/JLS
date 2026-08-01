# TASK-0080 - The headless boot run and its transcript comparison

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0069, TASK-0071, TASK-0079, TASK-0016

## Deliverable

The end-to-end run, and the three commands that make it a claim rather than a
demo.

1. **`--transcript <file>`**, one new `FlagSpec` row in `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:758-788`), `Arity.REQUIRED`, writing the guest-visible
   output byte stream from reset to termination. It is a **byte stream on its own
   file handle**, never stdout - `docs/batch-interface.md` §1 promises stdout
   carries only simulation results so it can be piped and diffed, and a kernel
   log on stdout breaks that promise.

2. **The behavioral run, in the nightly lane** (`AT-C2-I`):
   ```
   jls -b machines/cpu-rv32/rv32-soc.jls \
       -console replay:test/fixtures/c2/boot.itlog \
       -d 0 --transcript build/boot.txt
   cmp build/boot.txt test/fixtures/c2/boot.golden
   ```
   `boot.itlog` is TASK-0069's input log, **indexed by retirement, never by
   seconds, cycles or simulated time**. It contains `root\n`, `uname -a\n`,
   `ls /\n`, `cat /proc/cpuinfo\n`. `-d 0` is TASK-0012's no-limit mode.

3. **The falsification guard, which is the load-bearing half.**
   ```
   jls -b machines/cpu-rv32/rv32-soc.jls --clock-period 10x \
       -console replay:test/fixtures/c2/boot.itlog \
       -d 0 --transcript build/boot-slow.txt
   cmp build/boot-slow.txt test/fixtures/c2/boot.golden
   ```
   Changing the declared clock period changes every simulated time in the run and
   must change **no output byte**. `--clock-period` is a run-time multiplier on
   every `Clock` element's cycle, not a file edit. If this fails, the golden
   encodes time and the structural comparison is impossible. It is the cheapest
   test in the program and it gates everything built on the stream regime.

4. **The exclusion set `E`, as a committed file with a reason per entry**, plus a
   ratchet test asserting `E` did not grow without a recorded reason and that
   every report prints it. Guest configuration is a contract term, not a
   convenience: `printk.time=0` (or `CONFIG_PRINTK_TIME=n`), a pinned `lpj=`, a
   fixed hostname, and a prompt carrying no date, time or uptime. Those belong in
   TASK-0071's image build; this task owns the check that they held.

5. **A determinism ratchet asserting the converse**: no golden and no VCD fixture
   in the tree may have been produced in live console mode. TASK-0069 provides the
   provenance marker; this task provides the tree-wide assertion.

6. **The structural run is a release-cadence expedition, recorded, not a CI job.**
   The same file with the CPU boundary flipped to `structural`, the same
   `boot.itlog`, the **same golden**. Wall clock ~1.7 h at central inputs, honest
   band 1.2-6.0 h. It is recorded in `CHANGELOG.md` with the commit SHA of the
   run. That single `cmp` is the entire parity claim at Linux scale.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-038 | The end state. A drawn machine with no boot run is a drawing. |
| FEAT-034 | The stream-regime half of the harness. Per-instruction comparison across an interrupt is refused by the contract; the transcript comparison is what carries the claim instead, and it needs this artifact to compare. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0069 | Transcript capture, replay and the console pane | This task reads `boot.itlog` and the retirement-indexed replay mechanism, both of which only that task creates. Without it there is no deterministic input and no transcript to compare. |
| TASK-0071 | Guest image build, pinning and residence | The golden is a function of the kernel, the DTB and the initramfs. A transcript compared against an unpinned image compares nothing. |
| TASK-0079 | Draw the machine and bring it up boundary by boundary | The structural half of the claim runs the drawn machine. Nothing else in the tree is one. |
| TASK-0016 | Split CI into a required fast lane and a long-run lane, with a fixture policy | A ~2.5-minute nightly plus a multi-hundred-megabyte guest image has nowhere to live in the required gate. The lane and the large-fixture storage policy are that task's. |

## Acceptance test

`test/jls/machines/BootTranscriptTest` (long-run lane, not the required gate):
- `behavioralBootMatchesTheGolden()` - the `cmp` in step 2, as an assertion on the
  byte array, reporting the first differing offset and 64 bytes of context.
- `clockPeriodDoesNotChangeAnyOutputByte()` - step 3. **This is the test that
  makes the previous one mean anything**; it must be written first and it must
  fail on a deliberately time-encoding golden.
- `exclusionSetEntriesAllCarryAReason()` - parses `test/fixtures/c2/exclusions.txt`
  and fails on an entry with an empty reason field.
- `exclusionSetDidNotGrow()` - the ratchet, against a committed count.

`test/jls/machines/GoldenProvenanceRatchetTest`:
- `noGoldenOrVcdFixtureWasProducedInLiveConsoleMode()` - walks `test/fixtures/**`
  and asserts every transcript-derived fixture carries the replay-mode provenance
  marker.

`test/jls/CliFlagTableTest` extends over the two new flags, since
`JLSStart.commandLineFlags()` is the single authoritative table driving both the
parser and `usage()` (`src/jls/JLSStart.java:751-756`), and
`docs/batch-interface.md` §1's flag table is drift-tested against it.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | **closes (partly)** - this is the integration-golden half of #202 at SoC scale. |
| 265 | CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks | overlaps - the transcript comparison must be byte-identical across platforms or the golden is a Linux artifact; do not promote this lane to required until #265's platform question is settled. |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | informs - the wall-clock band for the structural expedition moves with #232 and the rest of FEAT-030; the transcript must not. |
| - | the transcript flag, the exclusion set, and the release-cadence structural run | **no issue** |

## Notes

- **`timeout-minutes` appears zero times repo-wide** - verified at HEAD across
  all six workflows in `.github/workflows/`. A nightly boot job without one is a
  six-hour hosted-runner bill on the first hang. TASK-0015 owns the sweep; this
  task must not land its lane before it.
- **A nightly cron already exists and is single-lane by design**
  (`.github/workflows/ci.yml:11-13,22-25`): the schedule event runs only the
  Wayland GUI lane and every other job skips on `github.event_name ==
  'schedule'`. Adding a boot lane means adding a second scheduled job, not
  widening the existing one - #101's stability record accrues on a fixed cadence
  and must not be diluted.
- **The known stdout deviation is a trap for the transcript.**
  `docs/batch-interface.md` §1 records that test-file parse errors print to
  *stdout* and exit 1 (`TestGen.specError`). If the transcript ever shares
  stdout, a malformed `-t` file silently corrupts the golden instead of failing.
  Separate handles; assert exit status, not stream content.
- **Two comparison regimes, and every claim must name which.** The structural run
  is a **stream-regime** claim: the per-instruction trace is deliberately not
  compared, because a timer interrupt lands between different instructions on the
  two tiers by construction. Do not write a per-instruction assertion into this
  test and do not let a reviewer ask for one; `docs/parity-contract.md` §3.6 is
  the recorded reason.
- **Live interaction is demonstrated, never asserted.** No automated test can
  prove a human typed. The replay is the assertion; the GUI session is the demo.
- **`-d 0` does not exist at HEAD.** `JLSStart` rejects a non-positive time limit
  with "option -d requires a positive integer time limit"
  (`src/jls/JLSStart.java:1067-1073`). TASK-0012 changes that; if it has not
  landed, this task's command line does not parse.

## Evidence

- `docs/parity-contract.md` §2.4 (`I`, the retirement-indexed input log), §2.5
  (`E`, the exclusion set and its ratchet), §3.3 (the guest-visible output byte
  stream as a must-be-bit-identical object), §3.6 (the two regimes and the
  interrupt problem), §5.4 (the observation cadences).
- `docs/virtual-hardware-parity.md` §1.3 and §1.5 - the wall-clock band and the
  correction that governs every structural figure.
- `docs/batch-interface.md:33-48` - the stream and exit-status contract and the
  recorded stdout deviation; §6 - the stability promise the new flags join.
- `src/jls/JLSStart.java:751-788` - the single authoritative flag table driving
  both the parser and `usage()`; `:1067-1073` - the positive-`-d` guard.
- `.github/workflows/ci.yml:1-25` - the nightly cron, its single-lane convention
  and the concurrency groups; zero `timeout-minutes` across all six workflows,
  verified at HEAD.
