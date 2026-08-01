# TASK-0081 - Specify the ternary ISA and its conformance corpus

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

A written instruction set and the corpus that decides whether an implementation
obeys it. No Java, no circuit, no assembler. Two artifacts.

1. **`docs/jls-t3-isa.md`**, normative for JLS-T3 only and explicitly not
   normative for anything in `docs/simulation-semantics.md`. Contents, each
   stated as a rule with its consequence, not as prose:
   - **Word.** 16 balanced trits, stored as 32 bits, Binary Encoded Ternary:
     one trit in two bits, `01` = +1, `00` = **illegal**, `10` = 0, `11` = -1
     (the exact code assignment is a decision this document makes and fixes).
     Range +/-21,523,360. State the waste - 3^16 = 43,046,721 of 2^32 - as a
     teachable number, not a footnote.
   - **Registers.** 27 = 3^3, so a register field is exactly 3 trits = 6 bits.
     `r0` reads as zero and discards writes.
   - **Encoding.** Fixed 16 trits. R-form `[op:3][rd:3][rs1:3][rs2:3][fn:4]`,
     I-form `[op:3][rd:3][rs1:3][imm7:7]`. 3 trits of opcode = 27 primary
     opcodes; the 1-trit function modifier in the R-form spare field gives 81.
     **A balanced-ternary immediate is signed for free**: `imm7` spans
     -1,093..+1,093 symmetrically, with no sign bit, no sign extension, no
     `ADDI`/`ADDIU` split and no `LUI`+`ADDI` pair to materialize a negative
     constant. Say so where a reader will see it; it is the ISA's best lesson.
   - **The ~28 instructions**, in five families:
     arithmetic `ADD SUB MUL DIV MOD NEG`;
     ternary logic `MIN MAX INV CYCP CYCN LIT k`;
     shifts `SHL3 SHR3` (multiply and divide by 3^k);
     memory `LDW STW LDB STB`;
     control `JMP JAL JALR SIGN BR3`;
     binary interop `BAND BOR BXOR BSHL BSHR BMUL64H`;
     system `ECALL HLT`.
   - **`BR3 rs1, off_neg, off_zero, off_pos`** - one instruction, one compare,
     three targets, because the sign of a balanced word *is* a trit. This is the
     ISA's identity and its encoding must be specified exactly, including what
     happens when two of the three offsets are equal.
   - **The exact division rule.** `DIV`/`SHR3` by 3^k **round to nearest by
     truncation** - the Knuth property. Specify `DIV` by a non-power-of-three,
     `DIV` by zero, `MOD`'s sign convention, and `MOD` by zero, each as a
     defined result or a defined trap. No case may be left to the
     implementation.
   - **`LIT k`, the J_k literal selector**, with the reason it exists: `MIN`/`MAX`
     alone are **not functionally complete** in base 3.
   - **Memory model.** Byte-addressed, little-endian, 32-bit words,
     `CHAR_BIT = 8`, addresses are **binary**. `LDB`/`STB` are the mixed-radix
     boundary inside the CPU and the document must say that plainly rather than
     hiding it - it is what makes `char`, strings and the console work.
   - **Undefined-value cases, enumerated.** Reading a word one of whose lanes
     holds the illegal `00` code; `DIV`/`MOD` by zero; `LIT k` with `k` out of
     range; a `BR3` whose target is misaligned; a binary-interop op on a
     register holding an illegal lane. Each gets a defined behavior or a defined
     trap, and each gets a corpus entry.

2. **`test/fixtures/t3/conformance/`**, the corpus, as data with a manifest:
   | tier | content |
   |---|---|
   | exhaustive | every ternary op over 1-trit (9 cases) and 2-trit (81 cases) operands - genuinely exhaustive, not sampled |
   | sampled | 16-trit ops, 10^6 seeded vectors, the seed in the manifest and in every failure report |
   | corners | +/-3^15, +/-(3^16-1)/2, all-`-`, all-`0`, all-`+`, alternating, and **the illegal `00` code injected into each of the 16 lanes** |
   | T-null | four knowingly-wrong reference models a conforming harness **must reject** |

3. **The four T-null models, specified here and built in TASK-0082**: (a) `SHR3`
   rounding away from zero instead of to nearest; (b) `BR3`'s zero and positive
   arms swapped; (c) `NEG` implemented as complement-plus-one instead of a plane
   swap; (d) the illegal `00` code silently read as `0`. Every one is **subtly**
   wrong and none is constant-zero. **(d) is the one that would otherwise be
   missed** and it is specific to BET: an implementation that treats `00` as `0`
   passes every ordinary test and is wrong in exactly the case the encoding
   exists to catch.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-039 | Everything. The emulator, the assembler and the drawn CPU are three independent constructions from this one document, and that independence is the entire non-circularity argument. |

## Prerequisite tasks

None. This is a specification and a data corpus; it reads nothing the tree does
not already have. Recorded explicitly because a spec is the classic thing to
defer behind an implementation, and deferring it is what makes the two
implementations agree by accident instead of by construction.

## Acceptance test

`test/jls/mach/t3/IsaSpecCorpusTest` - the corpus is checkable before any
implementation exists, and that is the point:
- `everyInstructionInTheDocumentHasACorpusEntry()` - parses the instruction table
  out of `docs/jls-t3-isa.md` and asserts each mnemonic appears in the manifest.
  Fails when an instruction is added to the doc and not to the corpus.
- `everyEnumeratedUndefinedCaseHasACorpusEntry()` - the same drift check over the
  undefined-value section.
- `exhaustiveTierIsExhaustive()` - asserts exactly 9 one-trit and 81 two-trit
  cases per ternary op, by count, so "exhaustive" is not a claim.
- `illegalCodeIsInjectedIntoAllSixteenLanes()` - asserts 16 corner entries, one
  per lane, not one entry.
- `everyEncodingRoundTripsThroughTheDocumentedFieldLayout()` - a pure decoder
  written from the field table in the doc, over every corpus instruction word.
- `theFourTNullModelsAreDeclaredAndDistinct()` - asserts four declared models,
  each naming the single rule it violates, and that none is constant-valued.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the JLS-T3 instruction set, its conformance corpus and its T-null models | **no issue.** The entire N-ary and ternary program is untracked; the registry records this as a gap, not an oversight. |

## Notes

- **There is no external conformance target, by construction, and that is not a
  weakness.** For RISC-V a self-written reference model would be circular because
  Sail and Spike exist. For a custom ISA the industry's answer is that the
  executable specification *is* the oracle - RISC-V's own golden model was
  written by the ISA's authors, and ARM's ASL is the same object. What makes it
  non-circular here is that the emulator (TASK-0082) and the drawn machine
  (TASK-0083) are **two independent constructions from this one document**,
  compared per retired instruction. Write the document so that independence is
  possible: no pseudocode a Java implementer would copy verbatim into a circuit.
- **The T-null tier asserts report *text*, not a boolean.** A harness that returns
  false for the right reason and a harness that returns false for the wrong one
  are the same boolean. TASK-0073's comparator is where that is enforced; specify
  the expected report wording here so the two do not drift.
- **Do not specify a native-radix memory subsystem.** Trit-addressed memory,
  ternary addresses and tryte I/O price at +20-35 wk for the memory subsystem
  alone and make every C pointer non-conforming: ISO C 6.2.6.2p1 requires a
  **pure binary representation** for unsigned integer value bits, and 5.2.4.2.1
  requires `CHAR_BIT >= 8`. Binary-encoded is the recorded architecture decision;
  this document states it and does not reopen it.
- **The status-alphabet bound is real and this ISA sits inside it.** Three
  bit-sliced planes give 8 codes per digit, so `R + |status alphabet| <= 8`: with
  the full `{X, Z, U}` alphabet, `R <= 5`. Radix 3 with the full four-state
  alphabet needs **no fourth plane and no new field** on the value type FEAT-026
  introduces. Any later widening of the alphabet is a fourth-plane decision, not
  a free one; record the arithmetic in the document so nobody re-derives it.
- **Balanced ternary, not unbalanced base 3.** Free negation, exact rounding, the
  three-way branch and the absence of a sign bit are where all the pedagogy is.
  Unbalanced base 3 is a footnote in the document, not an option in the encoding.

## Evidence

- `docs/capability-roadmap/AMENDMENT.md` and `docs/standards-adoption/**` - the
  program framing this ISA joins; no existing standard covers it, which is why
  the conformance corpus is written rather than adopted.
- `src/jls/elem/RegisterFile.java:141-154` - independent RA/WA ports, which is
  why a 27-register bank costs one element rather than 27.
- `src/jls/elem/Memory.java:1224,1234` - `DENSE_CAPACITY_LIMIT = 1 << 22` and the
  `bits <= 64 && capacity <= DENSE_CAPACITY_LIMIT` dense-store predicate, which is
  what makes a 16 MiB 32-bit-word guest expressible at HEAD.
- `docs/simulation-semantics.md` §4 and §6 - the binary semantics this ISA must
  collapse to at radix 2, referenced and not restated.
- `docs/parity-contract.md` §3 - the retirement-indexed methodology the
  differential tier adopts.
