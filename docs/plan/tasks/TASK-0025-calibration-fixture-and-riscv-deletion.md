# TASK-0025 - Commit the tracked calibration fixture, re-home the goldens, delete `riscv/`

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0022, TASK-0023

## Deliverable

Decision D5 is discharged: `riscv/` is gone, and nothing that mattered went
with it. Three distinct jobs, in this order.

1. **Commit the CPU-scale calibration fixture.** `riscv/build/k2000.jls` is the
   performance anchor for every number in
   `docs/capability-roadmap/keystone-c-performance.md` and it is **untracked** -
   `riscv/.gitignore` line 1 is `build/`. Regenerate it with
   `riscv/bench_kernel.py` **while that file still exists**, commit it and its
   clock vector under `test/fixtures/` (which `.gitignore:8-10` exempts from
   the `*.jls` ignore), and record its census - logic elements, wire nets, max
   width, per-type histogram - beside it. Without this, section 2 of
   `docs/machine-calibration.md` becomes permanently unreproducible, because
   **events per cycle is a property of the circuit, not of the engine**.
2. **Re-home the golden's regeneration path.** `test/jls/RiscvCpuGoldenTest.java`
   is tracked and runs on every `mvn verify`. Its javadoc cites
   `riscv/examples/sum1to10.s` and `riscv/README.md` as the regeneration recipe.
   Move the `.s` source, the `.clk.txt` vector and the assembly/clocking
   procedure into `test/fixtures/` (or `docs/machine-calibration.md` §7), and
   **fix the javadoc**: the two citations at `:25` and `:38` are `{@code}`
   spans, not `{@link}`, so the `-Werror` doclint gate will not catch them
   going dangling. Silent rot, therefore worse than loud rot.
3. **Salvage the differential-harness design, then delete the code.**
   `riscv/riscv_ref.py` (a 975-line RV32I reference emulator),
   `riscv/fuzz_diff.py` (randomized differential runner requiring identical
   final architectural state) and `riscv/verify.py` (11 directed programs) are
   the design `docs/parity-contract.md` formalizes. Transcribe the design **and
   its limitation** - `riscv_ref.py` was written by the author of the design
   under test, so it is a self-consistency oracle, not an independent one -
   into `parity-contract.md` before the code goes.
4. **Decide `riscv/gui/cpu.jls` explicitly.** It is one of only four tracked
   `.jls` files in the repository and is used as a real-world fixture for
   measured save-format churn. Keep it (moved) or lose it - but on the record,
   not by omission. A synthetic circuit does not substitute for it.
5. **Delete the directory.** `git ls-files riscv/` returns **26 tracked files**
   at HEAD; inventories of three or four have circulated and are wrong.
   Regenerate the inventory from `git ls-files` in the deletion commit message.
   Remove `riscv/` from `.gitignore` if it is named there, and grep the whole
   tree for surviving references before the commit.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-009 | Criteria 2 and 3 in full: the anchor becomes tracked, and `riscv/` stops being a dependency of anything. This is the task that discharges D5 |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0022 | Its measurements are taken with `riscv/bench_kernel.py`. Deleting the generator before the measurements exist destroys the ability to characterize the fixture that replaces it |
| TASK-0023 | Same generator, same reason |

## Acceptance test

`test/jls/CalibrationFixtureTest.java`, new:

- `theCalibrationFixtureIsTrackedAndLoads()` - asserts the fixture path exists
  under `test/fixtures/`, loads through `Circuit.load` + `finishLoad`, and
  reports the census the document quotes. This is the assertion that makes
  "committed" mean "usable", not merely "present".
- `theCensusMatchesTheRecordedNumbers()` - counts non-`Wire`, non-`WireEnd`
  elements and asserts equality with the constants the document states. A
  fixture regenerated with a different generator seed fails here loudly rather
  than silently re-basing every throughput figure.

`test/jls/RiscvCpuGoldenTest.java`, extended:

- `theRegenerationRecipeResolves()` - new method asserting every path the
  class's javadoc names exists on disk. This is the doclint gap closed by a
  test, since `{@code}` spans are invisible to `-Werror`.

`test/jls/NoRiscvDirectoryReferencesTest.java`, new - a ratchet in the
established `*RatchetTest` family (`test/jls/NullMarkedRatchetTest.java`,
`test/jls/PackageInfoRatchetTest.java`, `test/jls/HeadlessCoreRatchetTest.java`):

- `nothingReferencesTheDeletedDirectory()` - scans tracked `src/`, `test/`,
  `docs/`, `scripts/`, `.github/` and the root markdown files for the literal
  `riscv/` path prefix and asserts the only surviving mentions are inside an
  explicit allowlist of historical-record files (`docs/machine-calibration.md`,
  `CHANGELOG.md`, `ARCHITECTURE.md`). Fails if the allowlist is empty, so a
  scanner that silently matched nothing cannot pass forever.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - #202 asks for exactly the in-tree worked example this task is re-homing. Closing #202 and finishing this task are close to the same act, but #202 also asks for the HDL-export differential oracle, which is FEAT-023/TASK-0051 |

The calibration fixture that blocks deleting `riscv/` has **no issue**. That is
the registry's explicit finding, and it is worth noticing: the highest-priority
capstone's hardest precondition is untracked in both senses.

## Notes

- **Order is load-bearing and it is not tidiness.** `bench_kernel.py` generates
  the anchor. Delete it first and the anchor cannot be regenerated at all.
  Every other ordering in this task is preference; this one is arithmetic.
- **`test/jls/RiscvCpuGoldenTest.java` and `test/fixtures/riscv-sum1to10.jls`
  are TRACKED, not gitignored**, and the test runs on every `mvn verify`
  (executed during the study: 1 test, 0 failures, 0.426 s). Two earlier
  inventories claimed otherwise. Do not re-derive; `git ls-files` is the
  authority.
- **`.gitignore:8-10` is the mechanism.** `*.jls` is ignored;
  `!test/fixtures/**/*.jls` exempts fixtures (issue #56). A fixture committed
  anywhere else will be silently dropped by `git add`.
- **`.gitattributes` matters for this fixture.** `*.jls -text` and
  `test/resources/** -text` exist because a CRLF rewrite on a Windows checkout
  would change what the round-trip and determinism suites compare against
  (issue #111). A new fixture inherits the `*.jls` rule; a new `.txt` clock
  vector under `test/fixtures/` does **not**, and needs its own line.
- **The 64 MiB load cap applies.** `FileAbstractor.MAX_CIRCUIT_TEXT_BYTES`
  is `64L << 20` (`src/jls/FileAbstractor.java:65`) and is enforced on both the
  plain and the compressed path. A CPU-scale fixture at ~15.9 bytes per word is
  comfortably inside it, but check rather than assume, and note that the cap
  applies to the *decompressed* stream (`:347-381`).
- **This task is a precondition of TASK-0024, not a follow-up of it.**
  `docs/machine-calibration.md` §7.1 explicitly branches on whether the fixture
  was committed before the deletion.
- **CI cost.** A CPU-scale fixture is a large tracked file and CI has no
  large-fixture policy at HEAD. TASK-0016 owns that decision; this task must
  not invent a second one. Coordinate on where the fixture lives and which lane
  runs it.

## Evidence

- `git ls-files riscv/` - 26 tracked files at HEAD, enumerated this session.
- `riscv/.gitignore` line 1 is `build/`; `riscv/build/k2000.jls` therefore
  untracked.
- `git ls-files '*.jls'` - exactly four: `riscv/gui/cpu.jls`,
  `test/fixtures/fork-4.6-shiftregister.jls`,
  `test/fixtures/headless-canary-gate.jls`, `test/fixtures/riscv-sum1to10.jls`.
- `.gitignore:8-10` - the `*.jls` ignore and its `test/fixtures/**` exemption.
- `.gitattributes:1-11` - the `-text` rules and their stated reason.
- `test/jls/RiscvCpuGoldenTest.java:25,38` - the two `{@code}` citations;
  `:41-42` the fixture path; `:44-46` the 34-step program.
- `docs/machine-calibration.md:71-104` - §1.2, the asset-by-asset re-homing
  table; `:1001-1015` - §7.1's branch on whether the fixture was committed.
- `docs/virtual-hardware-parity.md:500-527` - the salvage preconditions and the
  two corrections to the inherited record.
- `src/jls/FileAbstractor.java:65,306-328,347-381` - the size cap and its two
  enforcement paths.
- Ratchet-test precedent: `test/jls/NullMarkedRatchetTest.java`,
  `test/jls/PackageInfoRatchetTest.java`,
  `test/jls/HeadlessCoreRatchetTest.java`.
