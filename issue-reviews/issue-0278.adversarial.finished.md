# Issue #278: RV32I integration-golden breadth: promote the fib and memtest directed programs into committed fixtures run by a parameterized RiscvCpuGoldenTest
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The core plan — bake `fib.s`/`memtest.s` into `.jls` fixtures with `make_cpu.py`, derive expected state with `riscv_ref.py`, and parameterize `RiscvCpuGoldenTest` — is mechanically sound and every file/line citation about the *current* test (`RiscvCpuGoldenTest.java` L42-47, L64-67, L69-80, L83-88, L91-109; `pom.xml`'s `testSourceDirectory`; the `test/fixtures/` listing) checks out exactly against the repo. But the motivating narrative contains two factual errors and one Completion-Criterion clause (P3) is unfalsifiable as literally written.

## Findings (most to least severe)

**1. [High] P3's fault-injection target does not exist — "revert simulator commit 9451ddc's Memory-write gating" is not a real operation.**
The issue's own citation for the first golden (`RiscvCpuGoldenTest.java#L38`) is commit `9451ddc`. Checking it directly:
```
$ git show 9451ddc --name-only
src/jls/elem/Put.java
src/jls/elem/WireEnd.java
test/fixtures/riscv-sum1to10.jls
test/jls/CircuitLoadErrorTest.java
test/jls/RiscvCpuGoldenTest.java
test/jls/ui/GuiConstructionObservationTest.java
```
That commit's `src/` diff is loader hardening for duplicate wire-end attachment (`Put`/`WireEnd`, issue #198) plus the CPU/GUI test additions — it touches **no memory-write logic at all**. The gated data-memory write-enable the issue is presumably thinking of ("Its write-enable is gated so a store commits only in the clock-low phase") is described in `riscv/README.md` as part of the *generated CPU circuit topology*, built by `riscv/build_cpu.py` — not a simulator-side behavior 9451ddc introduced or that can be "reverted" there. As written, P3 ("reverting simulator commit 9451ddc's Memory-write gating ... fails the memtest golden") sends the implementer to fix a nonexistent target. The parenthetical escape hatch — "(or an equivalent induced memory-timing fault)" — then lets P3 be satisfied by *any* contrived fault the implementer picks, with no criterion for what counts as "equivalent" to a real regression. That makes the DoD item "P1-P3 ... verified" gameable: someone could inject an unrelated, easily-caught fault, get a green/red flip, and call P3 satisfied without ever exercising genuine store/load timing.
*Recommendation*: fix the citation (point at `build_cpu.py`'s data-RAM write-enable gating term) and specify the actual fault to inject (e.g., a one-line diff removing the clock-phase AND gate on `dmem`'s WE), not "equivalent" left to discretion.

**2. [High] The Abstract's motivating claim — fib.s/memtest.s are "verified only by out-of-CI Python tooling" — is unsupported; nothing currently verifies them at all.**
`riscv/verify.py`'s 11-program directed suite (cited in Observation 4, `verify.py#L97 PROGRAMS = {`) contains inline programs named `addi, arith, logic_imm, shifts, slt, branch_taken, branch_nottaken, loop_sum, lui_auipc, jal_ret, mem` — none named `fib` or `memtest`, and none of them are the same source as `riscv/examples/fib.s` / `memtest.s`. A repo-wide grep confirms no `.py` file under `riscv/` ever opens or references `examples/fib.s` or `examples/memtest.s` by path (`grep -rn "examples/" riscv/*.py` → no hits). The two `.clk.txt` vectors were added once, in the same commit that first added the CPU (`ed48866`), and have never been touched since — they look like leftovers from a one-off manual `make_cpu.py examples/fib.s` invocation, not artifacts of any repeatable "Python tooling" verification path, in or out of CI.
*Impact*: the issue frames this as "promote already-verified goldens," which could tempt an implementer to skip actually validating fib/memtest against `riscv_ref.py` fresh and just trust the existing `.clk.txt` files. (To its credit, §8's Method does call for deriving expected state from `riscv_ref.py` directly, which sidesteps this — but the framing should not overstate existing verification that doesn't exist.)
*Recommendation*: correct the Abstract/Observations to say these two programs are currently **unverified** (only a stale, disconnected clock vector exists), not "verified out-of-CI."

**3. [Medium] "JUnit 5 (@ParameterizedTest already used in the corpus)" is false — this will be the first use in the entire test suite.**
```
$ grep -rl "ParameterizedTest" test/
(no results)
```
`org.junit.jupiter:junit-jupiter:6.1.2` is present (`pom.xml` L98-101) and does pull in `junit-jupiter-params` transitively, so `@ParameterizedTest` is *available* — but it is not "already used in the corpus" anywhere; there is no existing pattern to copy for method-source shape, naming, or fixture/expectation wiring. §6 Materials & Apparatus overstates precedent that doesn't exist, which understates the actual design cost: the implementer must invent the parameterization convention (a `record`/`MethodSource`, or hand-rolled `@TestFactory`) from scratch, not adapt one.
*Recommendation*: drop the "already used in the corpus" claim, or explicitly acknowledge this introduces the first parameterized test and note the convention should be documented for reuse by future fixture additions (the issue's own stated motivation).

**4. [Medium] Stale cross-reference: related issue #59 closed `not_planned` two days after this issue was filed.**
The Status & Dependencies block reads `related: [59]  # the future HDL-export oracle on #202 reuses these same programs`. #278 was filed 2026-08-01; #59 ("HDL interoperability... capstone") was closed `not_planned` on 2026-08-03. The "future HDL-export oracle" framing that motivated citing #59 no longer has that issue as a live tracking point (the CPU/fuzzer harness itself is still owned by open #202 per #59's body, so the *work* likely survives, but the citation is now dangling and should be re-verified, not taken at face value).
*Recommendation*: before starting work, re-check whether the `related: [59]` note still means what it says, or repoint it at whatever now owns the HDL-export-oracle scope (possibly #202 directly).

**5. [Low] Generalized expectation-table approach silently under-checks legitimate zero-valued memory words.**
`riscv_ref.py`'s `dump_dmem_words()` (L748-756) explicitly filters: `if val != 0: out[base // 4] = val`. If the JUnit expectation table for future fixtures is mechanically derived from this dump (as §7.6/§7.10 imply, generalizing the existing `register()`/`memoryWord()` pattern), any data word a program legitimately stores as `0` is invisible to both the oracle dump and thus the generated assertion — a stuck-at-wrong-nonzero-value regression at that address would go undetected. Not triggered by `fib.s`/`memtest.s` today (I checked both: fib stores the Fibonacci sequence 1,1,2,3,5,8,13,21,34,55 — never 0; memtest stores 0x123, 0x456, 0x579 — never 0), but the completion criteria don't call this out, so the gap is baked into the "generalized ... per-program expectation table" for any future program added under this same pattern.
*Recommendation*: add a note that the expectation table must assert every architecturally-touched address (from the program's own store trace), not just `riscv_ref.py`'s default non-zero dump.

**6. [Low, positive] Scope-note compliance is real.** Both `fib.s` and `memtest.s` use only `lw`/`sw` (word-granular) — no `lb/lh/lbu/lhu/sb/sh` — consistent with `riscv/README.md#L57-L64`'s stated hardware limitation. §11 Threats to Validity's claim "both fib and memtest already comply" is correct; verified by reading both files directly.

**7. [Low] Fixture-size/CI-time cost is unquantified but self-flagged.** The existing `riscv-sum1to10.jls` fixture is 9,360 lines / ~120 KB for a ~34-cycle program; two more comparably-sized CPU fixtures roughly triple the committed byte count and the per-run JVM circuit-load + batch-sim cost for this test class. The issue's own Open Questions section already defers the "promote all 11 vs. just 2" decision pending "measured fixture sizes and CI time," so this is an acknowledged rather than hidden risk — no action needed beyond what §"Open Questions" already proposes.

## Solid parts (no complaint)
- File/line citations for the *existing* `RiscvCpuGoldenTest.java` and `pom.xml` are all accurate — checked every one against HEAD.
- The `test/fixtures/` directory listing in Observation 1 matches exactly.
- Parent (#202, open) and hierarchy metadata (`has_parent`, `part_of_feature: 202`) are internally consistent.
- Falsification criteria (§10) correctly route a simulator/generator defect to a separate bug rather than papering over it here — good discipline.
- `config/spotbugs-exclude.xml` (referenced in the DoD) exists, so that checklist item is checkable as written.

## Recommendation
Fix finding #1 (P3's broken citation and its gameable fallback) before this is picked up — it's a Definition-of-Done item and currently cannot be executed as literally described. Correct the narrative overstatements in findings #2 and #3 so reviewers don't assume more prior art/verification exists than actually does. Findings #4-#7 are worth a line each in the issue but don't block starting the work.
