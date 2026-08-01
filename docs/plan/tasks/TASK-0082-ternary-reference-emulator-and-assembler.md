# TASK-0082 - The ternary reference emulator and assembler

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0081, TASK-0070

## Deliverable

An independent executable implementation of the JLS-T3 specification, plus the
tools that produce programs for it. All in-jar; nothing shells out.

1. **`src/jls/mach/t3/`, a leaf under TASK-0070's `jls.mach`.**
   - `Bet` - the Binary Encoded Ternary codec. `pack(int[] trits) -> int`,
     `unpack(int word) -> int[]`, and `illegalLanes(int word) -> int` returning a
     16-bit mask of lanes holding the illegal `00` code. **The codec never
     silently normalizes an illegal lane**; that is T-null model (d) and the
     whole reason the mask exists.
   - `T3State` - 27 registers, PC, and a `MemoryView` (shared with the RV32
     model, per TASK-0070). Immutable snapshot plus a mutable working form; the
     snapshot is what the comparator sees.
   - `T3Cpu.step(T3State) -> RetireRecord` - one retired instruction, no
     internal loop, no timing, no cycle count. Every instruction family from the
     spec, including `BR3`'s three arms, `SHR3`'s round-to-nearest truncation,
     `DIV`/`MOD`'s defined sign and zero behavior, `LIT k`'s range check, and the
     six binary-interop ops operating on the raw 32-bit view.
   - `T3Trap` - the defined traps from the spec's undefined-value section, each
     naming which rule produced it.

2. **`src/jls/mach/t3/asm/`, in-jar and offline.**
   - `T3Assembler.assemble(List<String>) -> T3Image` - two-pass, labels, `.org`,
     `.word`, `.trit`, `.ascii`, and the balanced-ternary literal syntax
     (`-++0` and `t'-++0'`). Every malformed line is collected and reported
     together with its line number, following `PinBindings.parse`'s shipped
     idiom, so a user learns the whole repair job from one failure.
   - `T3Disassembler.disassemble(T3Image) -> List<String>` - the inverse, and the
     round-trip is an acceptance test rather than a convenience.
   - `T3Image` - the `.timg` flat executable: a fixed header (magic, spec
     version, entry point, section table) and raw 32-bit words. It is **data**,
     not a serialized Java object.

3. **The ISA-parameterized `RetireRecord` instantiation.** T3's record is
   `{order, pc_before, pc_after, insn_word, rd_index, rd_value, trap}` and it
   carries **no field for cycles, simulated time, pipeline state or cache
   state**, so over-constraining the comparison is not expressible rather than
   merely discouraged. RVFI's 12 fields are RISC-V's; this is the same shape at
   a different arity.

4. **The four T-null models from TASK-0081, as real classes** under
   `test/jls/mach/t3/nulls/`, each a subclass or a decorator of `T3Cpu`
   overriding exactly one rule. They live in test scope so they can never ship.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-039 | The oracle. Without an independent executable model the drawn CPU has nothing to be right against, and the assembler is what turns the monitor's source into something either of them can run. |
| FEAT-034 | The second ISA instantiation of the harness, which is what proves `RetireRecord` is ISA-parameterized rather than RISC-V with generics on it. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0081 | Specify the ternary ISA and its conformance corpus | This implements a document and is checked by a corpus, both of which only that task produces. Implementing first and documenting after is exactly the circularity the independence argument depends on avoiding. |
| TASK-0070 | The machine package and its reference runner | `jls.mach` and its `MemoryView`, its coverage floor and its runner contract are that task's. T3 is a leaf inside it, not a parallel package. |

## Acceptance test

`test/jls/mach/t3/T3ConformanceTest`:
- `exhaustiveOneAndTwoTritOpsMatchTheCorpus()` - all 9 and all 81 cases per
  ternary op, by count and by value.
- `sampledSixteenTritOpsMatchTheCorpus()` - 10^6 seeded vectors, **seed printed
  in the failure message**.
- `everyDeclaredCornerMatchesTheCorpus()` - including the 16 illegal-lane
  injections, one per lane.
- `shr3RoundsToNearestByTruncation()` - the Knuth property, stated as its own
  test because it is the one arithmetic result the ISA exists to demonstrate.
- `divAndModByZeroTrapAsSpecified()` and `litOutOfRangeTrapsAsSpecified()`.

`test/jls/mach/t3/T3NullModelTest`:
- `everyNullModelIsRejectedWithTheSpecifiedReportText()` - four cases, asserting
  the **report text** the spec fixes, not the boolean. A harness that returns
  false for the wrong reason passes a boolean assertion and fails this one.

`test/jls/mach/t3/asm/T3AssemblerTest`:
- `assembleThenDisassembleRoundTripsEveryCorpusProgram()`.
- `everyMalformedLineIsReportedWithItsLineNumber()` - the `PinBindings.parse`
  idiom, asserted the way `UnbindablePortsTest#malformedBindingLinesAreAllReportedWithLineNumbers`
  asserts it.
- `balancedImmediatesSpanTheFullSymmetricRange()` - `-1093` and `+1093` both
  assemble to one instruction with no second instruction and no relocation,
  which is the "signed for free" claim made checkable.

`test/jls/mach/RetireRecordShapeTest#t3RecordHasNoTimingField()` - reflection over
the record components asserting no component name matches the forbidden set
(cycle, time, stall, pipeline, cache).

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the JLS-T3 reference emulator, assembler and disassembler | **no issue** |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | informs - #202's `riscv/riscv_ref.py` and its assembler are the working precedent for "an independent oracle plus a directed and randomized differential suite". This task is that pattern, in Java, in-jar, for a custom ISA. |

## Notes

- **In-jar is a hard constraint, not a preference.** There is **not one
  `ProcessBuilder` or `Runtime.getRuntime().exec` in all of `src/` at HEAD** -
  verified, zero hits. An assembler that shells out would be JLS's first
  subprocess and would convert a self-contained offline jar into a toolchain
  installation problem for every student.
- **The precedent to replace, not to copy.** `riscv/` holds a Python reference
  emulator and assembler (`riscv_ref.py`, `make_cpu.py`) that JLS cannot call.
  TASK-0025 deletes `riscv/`. Do not reproduce its shape: the T3 oracle is Java,
  in the jar, callable from a test.
- **`ObjectInputStream`/`ObjectOutputStream` are banned repo-wide**, zero
  tolerance, by
  `test/jls/ArchitectureRulesTest#nothingUsesJavaObjectSerializationStreams`
  (`test/jls/ArchitectureRulesTest.java:201-212`). `.timg` is a byte grammar with
  typed rejection, like every other format in the tree.
- **`jls.mach` is born floored at the strong bar** (TASK-0070). A leaf added to it
  inherits that floor immediately - there is no grace period - and the per-package
  JaCoCo rule in `pom.xml:449-515` is where it is enforced. Budget the tests
  as part of the two weeks, not after them.
- **New package, new `package-info.java` with `@NullMarked`**
  (`test/jls/PackageInfoRatchetTest#everyPackageHasPackageInfo`,
  `test/jls/NullMarkedRatchetTest`).
- **The emulator must not know about the circuit.** `jls.mach` is a pure leaf:
  no `jls.elem`, no `jls.sim`, no `jls.edit` import. That is what makes it an
  independent construction rather than a second reading of the same code, and
  `test/jls/ArchitectureRulesTest` is where the boundary is pinned - add the rule
  in this task rather than trusting review.
- **A trap that will bite in the codec.** Balanced-ternary negation is a plane
  swap - zero gates, one wire crossing. If `neg()` is written as
  `pack(-unpack(w))` through an `int`, it will be correct and it will teach the
  implementer nothing, and it will diverge from the drawn machine's timing story
  in TASK-0083. Write it as the swap and test the two against each other.

## Evidence

- `test/jls/ArchitectureRulesTest.java:201-212` - the Java-serialization ban.
- `src/jls/hdl/board/PinBindings.java:37-70` - the collect-every-malformed-line
  reporting idiom, with its `@jls.testedby` pointing at
  `UnbindablePortsTest#malformedBindingLinesAreAllReportedWithLineNumbers`.
- `src/jls/elem/RegisterFile.java:141-154` - the 27-register bank as one element.
- `docs/parity-contract.md` §2.1 (`D`, the machine definition), §3.1 (the ordered
  per-retired-instruction state delta this record feeds).
- `docs/virtual-hardware-parity.md` L5 (`:859-890`) - `jls.mach` as a pure leaf
  package and why the counterparty must be an independent construction.
- Verified at HEAD: `grep -rE "ProcessBuilder|Runtime.getRuntime\(\).exec" src/`
  returns **0**.
