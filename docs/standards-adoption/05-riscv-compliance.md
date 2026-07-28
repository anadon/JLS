## RISC-V architectural compliance: riscv-arch-test / RISCOF, and the RVI certification program (#65, #259)

### What conformance actually means

Two different things travel under the word "compliance" here, and they must not
be conflated.

**(a) Passing architectural tests.** `riscv-arch-test` (the Architectural
Compatibility Test suite, "ACT") is a corpus of hand-written assembly tests,
organised by extension into buckets such as `riscv-test-suite/rv32i_m/I/src/`
and `riscv-test-suite/rv32i_m/privilege/src/`. Each test executes a sequence of
instructions and stores results into a memory region delimited by the symbols
`begin_signature` and `end_signature`. RISCOF is the harness: it compiles each
test twice (once for the device under test, once for a reference model), runs
both, dumps each run's signature region to a text file (**4 bytes per line,
little-endian hex**), and diffs them. A test "passes" iff the two signature
files are byte-identical. The claim that rests on this is exactly: *for this
list of test source files, at this upstream commit, the DUT's signature equals
the reference model's signature.* It is not a proof of ISA conformance — the
suite is a finite corpus, not a coverage argument.

The artifact a claim rests on is the RISCOF run directory: the per-test
`DUT-jls.signature` and `Reference-sail_c_simulator.signature` files plus
RISCOF's generated HTML report, together with the pinned upstream commit SHA of
`riscv-arch-test`, the RISCOF version, and the reference model version.

**(b) The RISC-V International "RISC-V Compatible" trademark listing.** This is
a *trademark permission*, granted per **profile** (RVI20U32, RVA22, RVA23…),
not per instruction. Per RVI's Technical Processes wiki, the process is: run
the ACT tests that RVI declares required for the targeted profile, then file a
pull request adding a test report into a vendor-named directory of the
`riscv-non-isa/riscv-arch-test-reports` GitHub repository. (Verified only from
search summaries of the RVI Confluence pages and the RVspace forum post
describing StarFive's RVI20 filing; both RVI wiki pages returned HTTP 403 to
direct fetch, so treat the exact repository name and the current filing
mechanics as **unverified** and re-check before acting.)

**What JLS can and cannot claim.**

| Claim | Available? |
|---|---|
| "The `riscv/` CPU passes N of M tests in the `rv32i_m/I` bucket of `riscv-arch-test` @ *sha*, under RISCOF *v*, against the Sail RV32 reference model" | Yes, after this work |
| "RV32I compliant" (unqualified) | **No** — the excluded tests are part of the bucket |
| "RISC-V Compatible" / RVI-listed | **No** — see below |
| "RISC-V certified" | **No** — no such consumer-facing certificate exists for this design |

The blocking facts for (b): RVI20U32's mandatory base is RV32I with no
mandatory extensions (RISC-V Profiles 1.0, 2 April 2023), which sounds like a
match — but profile compatibility requires passing *all* ACT tests RVI marks
required for that profile, and the `riscv/` CPU has no CSRs, no traps, no
privilege modes, and no misaligned-access behaviour, so every test in
`rv32i_m/privilege` (the `misalign-*`, `ecall`, `ebreak` family) is
unconditionally out of reach. One search result also states `fence.tso` is
mandatory in that profile; I could not verify that against the ratified
Profiles PDF and it is **unverified**. Either way the honest position is:
**the trademark route is closed and should not be attempted.** Item #259 is a
"read the rules, write down why we are not doing it" item; #65 is the real work.

The DUT is also unusual: RVI's language is "RTL or a functional model." A JLS
circuit is a structural gate-level model that happens to run inside a teaching
simulator. Whether that is eligible for a listing is **unverified** and, given
the paragraph above, moot.

### Implementation procedure

Read `riscv/README.md` first; the tooling there is the substrate. Today:
`riscv/riscv_ref.py` assembles a small subset of assembly text (labels,
`nop/mv/li/j/ret`, no directives, no ELF) and emulates it;
`riscv/build_cpu.py` builds the datapath and bakes the assembled words into a
`Memory` ROM's `init` attribute; `riscv/jlsrun.py` shells out to
`java -jar target/jls-*.jar -b -d <limit> -t <clkvec> circuit.jls` and parses
the watched-element report; `riscv/verify.py` and `riscv/fuzz_diff.py` compare
final architectural state against `riscv_ref.py`. Memory model:
**Harvard, two separate spaces both starting at byte 0**, word-granular loads
and stores only (`lw`/`sw`), instruction address = `PC[iabits+1:2]` and data
address = `alu_out[dabits+1:2]` — i.e. both are *truncating*, so high address
bits are silently discarded.

Nothing below changes `src/`, the file format, or the batch interface. All new
code is Python under `riscv/` plus one or two Java tests under `test/`.

1. **Decide the memory-model story (the crux).** Arch tests are one unified
   address space: `.text.init` at the reset vector, `.data` (test operands) and
   the signature region elsewhere. The DUT is Harvard with truncating decode.
   **Recommendation: solve it entirely in the plugin's linker script, do not
   rebuild the CPU as a von Neumann machine.** Write
   `riscv/riscof/jls/env/link.ld` placing `.text*` at `0x00000000` and all data
   sections at `0x00010000`, and size the circuit `imem_cap = 4096` words and
   `dmem_cap = 4096` words. Then:
   - Reset PC is 0 and the link address of `.text` is 0, so PC-relative
     (`-mcmodel=medany`, `auipc`-based `la`) and absolute (`lui`+`addi`) symbol
     materialisation both yield correct values. Linking text anywhere else
     breaks one of the two.
   - The data window `[0x10000, 0x14000)` is exactly `dmem_cap*4` bytes and
     starts at a multiple of `dmem_cap*4`, so truncation maps it **1:1 and
     monotonically** onto dmem words `0..4095`: `word(A) = (A>>2) & 0xFFF`.
   - This is an unfaithful memory model and MUST be documented as such in
     `riscv/riscof/README.md`. The plugin MUST statically reject any test whose
     `.text` exceeds `imem_cap` words or whose data/signature symbols fall
     outside the window, rather than silently aliasing.
2. **Write an ELF reader: `riscv/elf_image.py` (new).** Pure stdlib, ~150
   lines: parse the ELF32 little-endian header and program headers, materialise
   `PT_LOAD` segments, and read `.symtab` for `begin_signature` /
   `end_signature` / `rvtest_code_end`. Output: `text_words: list[int]`,
   `data_words: dict[int,int]` keyed by dmem word index, and a symbol dict.
   `riscv_ref.py`'s assembler is **not** on this path and must not be extended
   to cope with the C-preprocessor macro soup in arch-test sources — the RISC-V
   GCC toolchain compiles the tests, exactly as RISCOF expects.
3. **Add a halt mechanism.** `RVMODEL_HALT` must end the run. JLS has a `Stop`
   element (`src/jls/elem/Stop.java`) that calls `sim.stop()` when any attached
   input is 1; I verified empirically that in batch mode a `Stop` firing gives
   outcome line `Simulation Stopped at <t>`, **exit status 0**, and the watched
   element report still prints. So:
   - Add a `stop()` factory to `riscv/jlsbuild.py` (element tag `Stop`, inputs
     `input0..input3`, no outputs) alongside the existing factories.
   - Add an opt-in halt decoder to `riscv/build_cpu.py`: compare the store word
     address against a magic word index AND `MemWrite`, feed the result to a
     `Stop`. Define `RVMODEL_HALT` in `model_test.h` as a store to that address
     (choose an address inside the mapped window but outside any signature or
     data region, and exclude it from the signature dump).
4. **Switch to a free-running clock for arch-test runs.** `build_cpu.py`
   currently drives the clock from an `InputPin` stepped by a `-t` vector whose
   length the reference emulator predicts — unusable when the cycle count is
   unknown a priori, and slow. `riscv/jlsbuild.py` already has a `clock(cycle,
   one)` factory (JLS `Clock` element). Add a `--free-clock` build option that
   substitutes a `Clock` for the `clk` input pin. Measured effect on this
   machine (JDK 25, jar `target/jls-5.0.5-SNAPSHOT.jar`): a 40 000-cycle run
   took **38.2 s** with the `-t` vector (1.3 MB vector file, two posted events
   per cycle) versus **4.1 s** free-running — ~1 000 vs ~9 700 simulated
   instructions/second. Keep the `-t` path unchanged as the default so the
   existing fixture `test/fixtures/riscv-sum1to10.jls` and
   `test/jls/RiscvCpuGoldenTest.java` are untouched.
5. **Write the signature dumper: `riscv/sig_dump.py` (new).** JLS has no
   memory-dump flag, and **it should not grow one for this**: adding a flag to
   batch mode is an addition to the `docs/batch-interface.md` stability
   contract (§6 — CHANGELOG plus minor-version material even for additive
   flags), and it is unnecessary. `docs/batch-interface.md` §3.3 specifies that
   a watched RAM prints `Changed locations in memory <name>` followed by
   `" 0x<addr>: OLD -> NEW"` in ascending address order, or `No changes in
   memory <name>`. The dmem is already `watch=1` in `build_cpu.py`. So the
   signature is reconstructed as: for each word index `i` in the signature
   range, `value = changed.get(i, initial_image.get(i, 0))` — the plugin knows
   the initial image because it loaded it from the ELF. Emit one 8-hex-digit
   word per line. (This reconstruction is exact only because the initial image
   is known; do not skip loading `.data` into dmem and assume zeros.)
6. **Create the RISCOF plugin tree under `riscv/riscof/` (all new).** Per the
   RISCOF documentation (file names and class contract taken from
   riscof.readthedocs.io "Building your Model Plugin"; the docs site 403'd to
   direct fetch so **verify against the installed RISCOF version** with
   `riscof setup --dutname=jls`):
   - `riscv/riscof/config.ini` — `[RISCOF] ReferencePlugin=sail_cSim`,
     `DUTPlugin=jls`, plugin paths, ISA/platform YAML paths.
   - `riscv/riscof/jls/riscof_jls.py` — class `jls(pluginTemplate)` with
     `__init__`, `initialise`, `build`, `runTests`. `runTests` generates a
     Makefile with one target per test; each target (i) compiles the test with
     `riscv32-unknown-elf-gcc -march=rv32i -mabi=ilp32 -static -mcmodel=medany
     -nostdlib -nostartfiles -T env/link.ld`, (ii) runs `riscv/elf_image.py` +
     `build_cpu.py --free-clock` to emit a `.jls`, (iii) runs the jar in batch
     mode, (iv) runs `sig_dump.py` to produce `<test>.signature`.
   - `riscv/riscof/jls/jls_isa.yaml`, `jls_platform.yaml` — riscv-config YAMLs
     declaring `RV32I` only. These are what RISCOF uses to compute the test
     list, so they are also the machine-readable statement of scope.
   - `riscv/riscof/jls/env/model_test.h` — `RVMODEL_BOOT` (empty; note the
     known upstream constraint that with binutils ≥ 2.38 an `rv32i` march
     string forbids CSR mnemonics, so any boot CSR write would have to be raw
     `.word` — we need none), `RVMODEL_HALT` (store to the magic address),
     `RVMODEL_DATA_BEGIN/END`, `RVMODEL_IO_*` as no-ops.
   - `riscv/riscof/jls/env/link.ld` — the layout from step 1.
   - `riscv/riscof/unsupported.md` — the exclusion list, per test file, with a
     one-line architectural reason each. This file is the honesty artifact.
7. **Pick the reference model.** RISCOF's official references are the **Sail**
   RISC-V model (`sail_cSim` plugin, ships with RISCOF) and Spike.
   **Recommendation: Sail.** `riscv/riscv_ref.py` MUST NOT be used as the
   RISCOF reference — it is JLS's own oracle, written by the same author as the
   decode tables in `build_cpu.py` (the README says so: "hardware and reference
   agree by construction"), so using it as the reference would make the whole
   exercise circular. Its role is unchanged: it stays the oracle for
   `verify.py` / `fuzz_diff.py`.
8. **Scope the claim precisely.** Enumerate the buckets from the checkout, do
   not trust any list from memory:
   `ls riscv-arch-test/riscv-test-suite/rv32i_m/*/src/`. Expected outcome:
   - **In scope now** — the `I` bucket's ALU/shift/compare tests (`add`, `addi`,
     `and`, `andi`, `or`, `ori`, `xor`, `xori`, `sll/slli`, `srl/srli`,
     `sra/srai`, `slt/slti/sltu/sltiu`, `sub`), `lui`, `auipc`, all six
     branches, `jal`, `jalr`, and the word-granular `lw-align`/`sw-align`
     tests.
   - **Probably in scope, verify** — `fence`: opcode `MISC-MEM` is not in the
     decode ROM, so `decode()` returns 0, which is an inert `RegWrite=0` NOP
     with `PC+4`. That is architecturally legal for a core with no reordering,
     so `fence-01` may pass unmodified.
   - **Out until sub-word memory exists** — `lb/lbu/lh/lhu/sb/sh` align tests.
     `riscv/README.md` already scopes this out ("a byte-lane mux on the read
     path and a read-modify-write on the store path"). Treat as a separate,
     later work item; note `docs/file-format.md` records a `Memory` `int sync 1`
     attribute (issue #199, clock-edge synchronous writes) that would make the
     read-modify-write store far cleaner than today's clock-low gating hack.
   - **Permanently out** — everything in `rv32i_m/privilege` (`misalign-*`,
     `ecall`, `ebreak`) and every `M`/`C`/`F`/`D`/`Zi*` bucket.
9. **Compatibility and migration.** No saved-file, format, or CLI surface
   changes. `build_cpu.py` gains two opt-in flags; existing invocations
   (`make_cpu.py`, `verify.py`, `fuzz_diff.py`) keep the `-t` clock path and the
   existing element set, so `test/fixtures/riscv-sum1to10.jls` stays
   byte-identical and `RiscvCpuGoldenTest` is unaffected. New Java tests MUST
   stay AWT-free per `test/jls/HeadlessCoreRatchetTest.java`. No stability
   contract is touched — explicitly, `docs/batch-interface.md` is *consumed*,
   not amended.
10. **Runtime budget (measured, not estimated).** With the free-running clock
    and a 3 000-instruction ROM image (`.jls` text 156 KB), 3 000 cycles ran in
    0.62 s wall including ~0.35 s JVM start; the marginal rate is ~10 000
    simulated instructions/second. Arch tests are macro-generated and typically
    execute low thousands of instructions, so **budget ~0.5–3 s per test,
    under ~2 minutes for a ~50-test bucket single-threaded**, less with the
    thread-pool pattern already used in `riscv/fuzz_diff.py`. This is
    comfortably CI-able; it is *not* comfortably `mvn verify`-able, which is why
    it belongs in its own lane.

### Testing procedure

The external-toolchain path proves conformance; a checked-in golden path proves
*non-regression* offline, which is what the repo's house style is for.

1. **`test/jls/RiscvSignatureHaltGoldenTest.java` (to be created).** New
   fixture `test/fixtures/riscv-signature-halt.jls` (to be created,
   generated by `build_cpu.py --free-clock` from a small program that writes a
   known pattern into dmem and then stores to the halt address). The test loads
   the fixture with `Circuit.load`/`finishLoad`, runs `BatchSimulator` with a
   generous `-d` limit and **no** test file, and asserts (a) the outcome line is
   `Simulation Stopped at <t>` for a pinned `t`, (b) the changed-locations
   report for `dmem` is byte-exact, (c) the signature words reconstructed from
   it equal a checked-in `.signature` golden. Model it on the existing
   `test/jls/RiscvCpuGoldenTest.java` (same load/run/assert shape) and on
   `test/jls/ElementSimulationGoldenTest#stopHaltsTheSimulationEarly`, which
   already pins `Stop`'s outcome-line behaviour. **Turns red when:** `Stop`
   semantics change, `Clock` edge timing changes, memory write timing changes,
   or `Memory.printChangedValues` output changes (i.e. a silent break of
   `docs/batch-interface.md` §3.3).
2. **`riscv/test_riscof_glue.py` (to be created).** Pure-Python unit tests in
   the style of `riscv/test_primitives.py`: `elf_image.py` against a small
   checked-in ELF (`riscv/riscof/testdata/hello.elf`, built once by the
   toolchain and committed with its build command recorded), and `sig_dump.py`
   against a captured batch stdout sample. Extend `riscv/test_primitives.py`
   with a `Stop` primitive check (the file's stated job is validating "every
   element emitter against the real simulator", so a new emitter needs one).
3. **Checked-in signature goldens.** For a pinned subset of ~8 tests spanning
   the instruction classes (one ALU, one shift, one compare, `lui`, `auipc`,
   one branch, `jal`/`jalr`, `lw`/`sw`), commit
   `riscv/riscof/goldens/<test>.signature` — produced by **Sail**, with the
   arch-test commit SHA, Sail version, and generating command recorded in
   `riscv/riscof/goldens/PROVENANCE.md`. Then `riscv/run_arch_subset.py` (to be
   created) rebuilds each test's circuit from committed ELFs, runs the jar, and
   diffs. This needs only `python3` + the jar — no GCC, no Sail, no network —
   so it is the lane that actually guards against JLS-side regressions.
4. **Full RISCOF run, skip-when-absent.** Follow the established gating
   pattern: `test/jls/hdl/ToolLocator.findOnPath(...)` plus
   `Assumptions.assumeTrue`, as in `test/jls/AutogradeBridgeExampleTest.java`
   (skips without `python3`) and `test/jls/hdl/IverilogCompileTest.java`. If a
   Java entry point is wanted at all, add `test/jls/RiscvArchTestSubsetTest.java`
   that skips unless `riscof`, `riscv32-unknown-elf-gcc`, and a
   `JLS_ARCH_TEST_DIR` env var are all present. Prefer keeping the full run out
   of JUnit entirely and driving it from the workflow.
5. **CI lanes (`.github/workflows/`).**
   - Add a `riscv-goldens` job to `.github/workflows/ci.yml`, next to the
     existing `build` job: build the jar, then `python3 riscv/run_arch_subset.py`.
     Budget ~2 minutes. Runs on every PR. This is the regression gate.
   - Add `.github/workflows/riscv-arch-test.yml` (to be created): `schedule:`
     nightly plus `workflow_dispatch`, **not** on PRs. Steps: install the
     RISC-V GCC toolchain and the Sail model (best-effort, in the style of
     ci.yml's "Install HDL toolchain and virtual display" step, which tolerates
     apt failure and lets the affected suites skip), `pip install riscof`,
     checkout `riscv-arch-test` at a **pinned SHA**, `riscof run`, upload the
     RISCOF HTML report and all signature files as artifacts, and fail the job
     on any regression against the previous known-good pass list committed as
     `riscv/riscof/passlist.txt`.
6. **Property/fuzz opportunities.** `riscv/fuzz_diff.py` already does
   randomized differential testing against `riscv_ref.py`. The high-value
   extension is to point the same generator at **Sail** instead of
   `riscv_ref.py` — that breaks the by-construction correlation between the
   hardware's decode ROM and the Python oracle, which is the single biggest
   soundness gap in the current verification story. Doing that is cheap once
   the plugin exists (the ELF/signature plumbing is shared) and is arguably
   worth more than the arch-test pass itself.
7. **Wording lint.** Add `test/jls/RiscvClaimWordingTest.java` (to be created):
   assert that `README.md`, `riscv/README.md`, `CHANGELOG.md`, and
   `docs/standards-landscape.md` contain none of the forbidden strings
   "RISC-V certified", "RISC-V Compatible", "RV32I compliant", "fully
   compliant". Cheap, and it is the mechanism that keeps the honest claim
   honest through future edits.

### Certification / conformance procedure

**The passing-tests half is pure self-assertion.** No body assesses it, no
registry lists it, there is no fee and no validity period. A credible
self-assertion consists of, published in `riscv/riscof/README.md` and linked
from `riscv/README.md`:

1. the exact claim sentence, naming the bucket, the pass count and denominator,
   the upstream `riscv-arch-test` commit SHA, the RISCOF version, the reference
   model and its version, and the JLS version;
2. the exclusion list with a per-test architectural reason
   (`riscv/riscof/unsupported.md`);
3. the reproduction command, runnable by a third party;
4. the committed signature goldens and their provenance;
5. an explicit disclaimer that this is not an RVI certification.

Model claim sentence: *"The `riscv/` CPU passes 34 of 41 tests in the
`rv32i_m/I` bucket of riscv-arch-test @ `abc1234`, run under RISCOF 1.24.0
against the Sail RV32 reference model; the 7 excluded tests and the
architectural reasons are listed in `riscv/riscof/unsupported.md`. JLS is not
certified by RISC-V International and makes no 'RISC-V Compatible' claim."*

**The RVI half is not available and should be formally declined.** For the
record, the process as far as it could be verified: successful completion of
the ACT tests required for a targeted **profile** is the prerequisite for
permission to use the "RISC-V Compatible" trademark; results are filed as a
pull request adding a test report to a vendor-named directory of the
`riscv-non-isa/riscv-arch-test-reports` repository; a Certification Steering
Committee governs policy, ISO alignment, and the accreditation of third-party
test labs. **Cost, elapsed time, membership prerequisites, validity period, and
renewal obligations: I could not verify any of these** — the two RVI Confluence
pages and riscv.org's brand-guidelines page both returned HTTP 403 to automated
fetch. Do not put a number on them without reading the primary pages.

Why JLS declines: the `riscv/` CPU cannot pass the `privilege` bucket (no CSRs,
no traps, no misalign behaviour), so no ratified profile is attainable, so the
trademark permission is unattainable. Record this as a decision in
`ARCHITECTURE.md`'s recorded-decisions section (the same form as the existing
"Simulation execution strategy" entry, with a revisit trigger: *a CSR/trap
implementation on the `riscv/` trajectory*).

**Trademark hygiene, which does apply regardless.** RISC-V, the RISC-V logos,
and "RISC-V Compatible" are trademarks of RISC-V International. The safe posture,
which costs nothing: use "RISC-V" only descriptively ("a RISC-V RV32I CPU built
in JLS"), never in the project's or an artifact's name, never as a logo, never
in a way implying endorsement or certification; carry a one-line attribution
("RISC-V is a trademark of RISC-V International") in `riscv/README.md`. The
specific permissible-use rules in RVI's brand guidelines are **unverified**
(403); read them before publishing any marketing-shaped text.

### Effort, risk, and failure modes

**Sizing: 8–12 maintainer-days**, reasoning by piece —

| Piece | Days |
|---|---|
| `elf_image.py` + linker script + address-window validation | 2 |
| `Stop` emitter, halt decoder, free-clock option in `build_cpu.py` | 1 |
| `sig_dump.py` + `riscv/test_riscof_glue.py` + `test_primitives.py` addition | 1 |
| RISCOF plugin, YAMLs, `model_test.h`, first green test end-to-end | 2–3 |
| Java golden + fixture (`RiscvSignatureHaltGoldenTest`) | 0.5 |
| Two CI lanes + pinned SHAs + pass list | 1 |
| `riscv/riscof/README.md`, exclusion list, CHANGELOG, ARCHITECTURE decision, wording lint | 1–1.5 |
| *(optional)* sub-word load/store to unlock `lb/lh/sb/sh` | +2–3 |

The 2–3 days for "first green test" is where the variance is: getting a
toolchain-compiled ELF, a macro-generated prologue, a linker script, and a
truncating Harvard address decode to agree is a debugging problem, not a coding
problem, and the failure mode is a silently wrong signature rather than a crash.

**Top three ways this goes wrong.**

1. **Silent address aliasing.** A data reference outside the mapped
   `[0x10000, 0x14000)` window wraps modulo 4096 words and corrupts an unrelated
   location, or the signature region straddles the window edge. The test then
   passes or fails for a reason unrelated to the ISA. *Mitigation:* the plugin
   validates every loadable segment and every signature symbol against the
   window before the run, and post-run asserts that no changed dmem address
   lies outside the union of the data and signature regions.
2. **Upstream drift rots the lane.** `riscv-arch-test` is actively
   restructuring — an `act4` branch exists in `riscv/riscv-arch-test` alongside
   the `riscv-non-isa/riscv-arch-test` `main`/`dev` branches, and the framework
   may move off RISCOF. A nightly lane that depends on unpinned upstream plus
   an apt-installable GCC and Sail will go red for reasons that are not JLS's,
   get muted, and then guard nothing. *Mitigation:* pin the arch-test SHA and
   the RISCOF version, and make the **offline golden lane** the PR-blocking
   gate; the nightly external lane is informational and must never be the only
   thing catching a JLS regression.
3. **Overclaiming.** "Passes the RV32I arch tests" becomes "RV32I compliant"
   becomes "RISC-V compatible" over three README edits, and the project has a
   trademark problem plus a false statement in a teaching tool. *Mitigation:*
   the fixed claim sentence, the exclusion list next to it, and
   `RiscvClaimWordingTest`.

**Do not do this if** any of the following hold: (a) the maintainer will not
own pinned-SHA maintenance of an external toolchain lane — a stale conformance
claim is worse than none; (b) the motivation is the RVI trademark — it is
unattainable and the effort is wasted; (c) it would put `riscof`,
`riscv32-unknown-elf-gcc`, or Sail anywhere on the `mvn verify` path, breaking
CONTRIBUTING's "JDK 25 and Maven are the only requirements"; (d) the sub-word
memory extension is treated as in-scope for this item — it is a separate
datapath change with its own differential-testing needs and should not be
bundled into the plugin work.

The honest counter-argument to doing it at all: `riscv/fuzz_diff.py` already
provides randomized differential coverage of every implemented instruction, and
the single most valuable improvement is **swapping its oracle from
`riscv_ref.py` to Sail** (step 6 of the testing procedure), which removes a
real correlated-error risk. That is roughly 2 days of the 8–12 and delivers
most of the verification value; the RISCOF plugin adds the *citable, externally
defined* corpus on top. If time is short, do the oracle swap first.

### Sources

Repo paths (all verified by reading):
`riscv/README.md`, `riscv/build_cpu.py`, `riscv/jlsbuild.py`, `riscv/jlsrun.py`,
`riscv/make_cpu.py`, `riscv/riscv_ref.py`, `riscv/verify.py`,
`riscv/fuzz_diff.py`, `riscv/test_primitives.py`, `riscv/examples/`,
`riscv/gui/README.md`; `docs/standards-landscape.md` (#65 line 221, #259 line
618, and the ranked entry — since commit `9ab4797` this is **§13.1 item 2**,
line 730, not "item 5"; that commit split §13 into §13.1 logic-design and
§13.2 institutional conformance); `docs/batch-interface.md` §1–§3, §6;
`docs/file-format.md` (Memory `init`/`initrle`/`sync`, line 307);
`ARCHITECTURE.md` (recorded decision, simulation execution strategy, ~line 340);
`CONTRIBUTING.md`; `README.md`; `pom.xml` (`<sourceDirectory>src</…>`,
`<testSourceDirectory>test</…>`); `.github/workflows/ci.yml` (build job, HDL
toolchain best-effort install, lines 55–80); `src/jls/elem/Stop.java`,
`src/jls/elem/Memory.java`, `src/jls/elem/Clock.java`;
`test/jls/RiscvCpuGoldenTest.java`, `test/fixtures/riscv-sum1to10.jls`,
`test/jls/ElementSimulationGoldenTest.java`
(`stopHaltsTheSimulationEarly`), `test/jls/AutogradeBridgeExampleTest.java`,
`test/jls/hdl/ToolLocator.java`, `test/jls/hdl/IverilogCompileTest.java`,
`test/jls/HeadlessCoreRatchetTest.java`, `test/jls/BatchSimulationGoldenTest.java`.

Measurements taken in this environment (JDK 25.0.3, `mvn package -DskipTests`
producing `target/jls-5.0.5-SNAPSHOT.jar`), reproducible from the scripts
described above: `-t`-driven 40 000-cycle CPU run 38.2 s; free-running-`Clock`
40 000-cycle run 4.13 s (~9 700 instr/s); 3 000-instruction ROM image →
156 KB `.jls`, 3 000 cycles in 0.62 s; `Stop` in batch mode → `Simulation
Stopped at 751`, exit 0, watched register still printed; `python3
riscv/verify.py` 11/11 pass in 4.2 s.

External, **partially verified** (search-result summaries only; both RVI
Confluence pages, riscv.org/about/brand-guidelines, and riscof.readthedocs.io
returned HTTP 403/404 to direct fetch — re-read the primaries before acting):
RISCOF plugin contract (`config.ini`, `riscof_<name>.py` with
`initialise`/`build`/`runTests`, Makefile-per-test, `asm/model.h` RVMODEL_HALT,
signature file named `<name>.signature`, 4 bytes per line little-endian,
`begin_signature`/`end_signature`); ACT report filing as a PR to
`riscv-non-isa/riscv-arch-test-reports`; Certification Steering Committee scope;
RVI20U32 mandating RV32I with no mandatory extensions (RISC-V Profiles 1.0,
2023-04-02); the binutils ≥ 2.38 restriction on CSR mnemonics under a plain
`rv32i` march string.

**Unverified and deliberately not numbered here:** any RVI fee, membership
prerequisite, elapsed time, validity period, or renewal obligation; whether a
structural model inside a logic simulator is an eligible DUT for a listing;
whether `fence.tso` is mandatory for RVI20U32; the exact current test-file
inventory of `rv32i_m/I` (enumerate it from the pinned checkout, not from
memory); whether the framework remains RISCOF or moves to an ACT4 runner.
