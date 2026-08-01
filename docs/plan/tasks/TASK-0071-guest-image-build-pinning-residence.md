# TASK-0071 - Guest image build, pinning and residence

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

The thing that is actually booted, built reproducibly, pinned by checksum, with
its determinism requirements in the machine definition rather than in prose -
and a decided, tested answer for where it lives.

1. **`scripts/build-guest-image.sh`**, beside the existing
   `scripts/build-container.sh` and `scripts/build-installer.sh`. It produces,
   from pinned inputs: a kernel version and its `.config`; busybox and an
   initramfs including the digest utility the parity contract names
   (`docs/parity-contract.md` §3.2); a device tree source and its compiled blob;
   the memory map with every device base address; a reset stub; and a
   `SHA256SUMS` file over every emitted artifact. The RV32 nommu cross
   toolchain is named and pinned but **not built by this script** - see Notes.

2. **The determinism requirements become machine-definition fields, not prose.**
   With any default kernel config the console byte stream is not
   tier-independent. The pinned set, each with its reason:
   - `printk.time=0` - `CONFIG_PRINTK_TIME` stamps every kernel line from a
     timer, and the two tiers reach any instruction at different simulated
     times.
   - a pinned `lpj=` - `loops_per_jiffy` is the guest measuring its own speed,
     so it may legally differ between two otherwise identical implementations.
   - a fixed hostname and a time-free shell prompt.
   - `timebase-frequency` declared at 1 MHz (`0xf4240`, the measured
     configuration), which shrinks every `udelay` by 50x against a 50 MHz
     declaration at zero architectural cost.
   These live in a `jls.mach` machine-definition record, checked into the tree
   with the image.

3. **The residence decision, made and recorded.** The pinned kernel + initramfs
   is ~2-6 MB, far under GitHub's 100 MB per-file block. Commit it as a
   **stored (uncompressed)** artifact under `test/fixtures/guest/`, with
   `.gitattributes` marking it `-text -diff` - the file already carries
   `*.jls -text` for exactly this reason (byte-exact test inputs must not be
   CRLF-rewritten). Record the decision in `docs/machine-calibration.md` as a
   new subsection, including the size and the `.git` growth basis.
   **This explicitly reopens the "no committed guest images" exclusion.** It is
   a decision, not an oversight to route around: the alternative - a nightly
   lane that downloads an unpinned image - is the lane that goes red for reasons
   that are not JLS's, gets muted, and then guards nothing.

4. **A rebuild recipe K8 is written against.** Once the image, the `.config`,
   the initramfs and the DTB are checksummed artifacts with a documented
   rebuild, an upstream RV32 nommu removal becomes documentation rot rather than
   a program-ending event.

5. **The settling experiment, run and recorded.** Boot the pinned image under
   `jls.mach.Runner` at two declared clock rates and diff the console streams.
   One afternoon. Its result is what proves the four levers above are sufficient
   and it is the acceptance test below.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-033 | The runner without an image runs nothing. This is the fourth part of the feature and the only one with no JLS dependency. |
| FEAT-013 | The image is the first real consumer of the raw bulk-image section: it is binary, it is never diffed, and it must sit outside the circuit text cap. |

## Prerequisite tasks

None. The guest stack depends on nothing in JLS, which is why it can start
immediately and in parallel - and doing so retires the RV32-nommu-removal risk
early.

TASK-0034 (the raw bulk-image section) is the consumer, not the producer: it
defines how the image rides *inside a `.jls`*. The on-disk fixture this task
commits is readable by `jls.mach.Runner` without it.

## Acceptance test

`test/jls/mach/GuestImageFixtureTest`:

- `theCommittedImageMatchesItsRecordedChecksums()` - recomputes SHA-256 over
  each artifact under `test/fixtures/guest/` and compares against the committed
  `SHA256SUMS`. Cheap, runs in the required lane, and catches an LFS smudge, a
  CRLF rewrite or a partial checkout immediately.
- `theBootIsTierIndependentAtTwoDeclaredClockRates()` - **the settling
  experiment as a test**. Boot the pinned image under `jls.mach.Runner` with
  `timebase-frequency` at two declared values and assert the console byte
  streams are byte-identical. **This fails today for any default kernel
  config**, because `CONFIG_PRINTK_TIME` stamps each line; it passes only with
  the four levers of deliverable 2 applied, which is what makes it the
  acceptance criterion rather than a smoke test.
- `theImageIsWithinTheFixtureSizeBudget()` - asserts the total against the
  threshold TASK-0016's fixture policy declares. Owned there; asserted here.
- `theMachineDefinitionDeclaresEveryDeterminismLever()` - reflective assertion
  that the machine-definition record carries `printkTime`, `lpj`, `hostname` and
  `timebaseFrequency`, so a lever cannot be dropped silently in a refactor.

The long boot itself (~4.0 x 10^7 instructions to a shell) runs in the long-run
lane (TASK-0016), not the required fast lane, with its length stated.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the guest software stack, its pinning and its residence | **no issue** - and nothing in the roadmap or the layer stack owns it either, while three commitments dereference it |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - the worked example needs a program to run; this is the large end of the same need |
| 184 | Release-artifact reproducibility gaps: container apt pinning, installer `SOURCE_DATE_EPOCH`, and a BOM reproducibility guard in CI | informs - the pinning discipline is the same one, applied to a different artifact class; do not conflate the two build pipelines |

## Notes

- **Userspace is bFLT, not ELF.** `CONFIG_BINFMT_FLAT=y` with uClibc, which
  means building the rootfs needs a uClibc + elf2flt riscv32 nommu toolchain -
  a multi-hour, network-heavy buildroot job. That job must never sit on a
  required CI lane. Pin the toolchain by container digest; run the rebuild
  informationally, on demand, and compare against the committed checksums.
- **The circuit-text cap is the reason the image cannot be `Memory` init text.**
  `MAX_CIRCUIT_TEXT_BYTES = 64 MiB` (`src/jls/FileAbstractor.java:65`) is
  measured against **decompressed** text, so container choice does not move it.
  A 16 MiB 32-bit RAM image as saved init text is ~66.6 MB - 99.2% of the cap,
  leaving ~0.5 MB for the entire circuit. The image rides TASK-0034's binary
  section or an on-disk sidecar; it never rides hex text.
- **12 MiB is the practical floor for a shell, 16 MiB is the number to design
  to.** 8 MiB dies in `binfmt_flat` with `errno -12`; 6 MiB panics with
  `No working init found`. Do not "save space" below 12.
- **The boot is compute-bound, not timer-bound.** A 100x slower declared machine
  costs only ~8% more instructions to the prompt. There is no timeout cliff, so
  a slow clock is free - but only if `mtime` is driven by simulated time
  (TASK-0070 deliverable 5).
- **Do not home anything under `riscv/`.** TASK-0025 deletes it.
- The Sv32/OpenSBI hedge is a **third** guest artifact with its own cost band.
  Name it in the recipe as unbuilt; do not build it here.

## Evidence

- `docs/machine-calibration.md` §5.1 - ~4.0 x 10^7 retired instructions to an
  interactive shell (Linux 6.5.12, RV32IMA nommu, 16 MiB, busybox), measured on
  instrumented `mini-rv32ima`, 2026-07, with independent corroboration from
  `raspiduino/arv32-opt` agreeing within 15%.
- §5.2 - the RAM sweep table (64/16/12/8/6/4 MiB) and the exact
  `MAX_CIRCUIT_TEXT_BYTES` collision arithmetic, including 15.87 bytes per
  32-bit word of save text measured on a generated probe.
- §5.3 - the device tree that boots: 1,536 bytes compiled, one UART, one CLINT,
  one syscon, no PLIC node.
- §5.4 - the two free levers, and the measured table (100 MIPS vs 1 MIPS:
  56,849,791 vs 61,233,095 instructions to the login prompt).
- §5.5 - the RV32 nommu `CONFIG_NONPORTABLE=y` removal proposal and the pinning
  mitigation.
- `src/jls/FileAbstractor.java:65` - `MAX_CIRCUIT_TEXT_BYTES = 64L << 20`.
- `.gitattributes:1-5` - the existing `-text` discipline for byte-exact
  fixtures.
- `scripts/` - `build-container.sh`, `build-installer.sh` establish where a
  build script lives.
- `docs/virtual-hardware-parity.md` P21 and "Where the image lives, and the
  contradiction that must be resolved" - the exclusion this task reopens, with
  the measured `.git` cost comparison (2,397,301 B stored vs 2,396,453 B raw
  over ten revisions, measured outside this repository and not reproducible from
  it).
