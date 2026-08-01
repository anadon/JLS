# TASK-0060 - The higher-radix operator kernel

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0056

## Deliverable

One module, written once, that every engine reads. Nothing drawable ships.

1. **`jls.core.RadixAlphabet`** - the code-point assignment table. The three
   planes `(a, b, u)` of the value from TASK-0056 encode
   `c = a + 2b + 4u` in `[0, 8)`: eight code points per position. Radix 2 uses
   five (`0 1 X Z U`); radix 3 uses six (`0 1 2 X Z U`); radix 4 uses seven;
   radix 5 saturates at eight. `ceil(log2(r + 3))` is the plane count, so the
   fourth-plane cliff is at **radix 6** and the class refuses `r >= 6` with that
   arithmetic in the message. This is the "write the truth tables once, in one
   place" module: the event-driven engine and any later levelized pass must read
   the same table or they will disagree six months apart.
2. **`jls.core.RadixOps`** - `min`, `max`, `complement` (`(N-1)-d`), `cyclic`
   (`(d+1) mod N`), `diminish` (`d-1`), Allen-Givone `literal`, and `sumModN`,
   all plane-wise over the three planes, all parameterized by radix. Each must
   collapse to the exact HEAD binary operation at N=2, which is what makes the
   radix-2 path structurally free:
   `min` at N=2 is `value.and(inVal)` (`src/jls/elem/AndGate.java:64-75`),
   `max` is `value.or(inVal)` (`src/jls/elem/OrGate.java:65-75`),
   `complement` is `notQOut.flip(0, bits)` (`src/jls/elem/Register.java:805`),
   `sumModN` is `value.xor(inVal)` (`src/jls/elem/XorGate.java:71-81`).
3. **`jls.core.BalancedTernaryAdder`** - the lane-packed Kogge-Stone add.
   Digits held 4 bits per lane, 16 per `long`, so a lane-wise `a + b` cannot
   overflow (2+2+1 = 5) and the digit sums are one native `long` add; carries
   resolved by parallel prefix over generate (`lane >= 3`) and propagate
   (`lane == 2`) masks in 4 steps for 16 lanes. Plus `planesToLanes` /
   `lanesToPlanes`.
4. **A slow per-digit reference implementation**, in the same package, marked
   as the oracle and never called from the engine.
5. **`jls.core.*` added to the PIT `targetClasses` list** (`pom.xml:780-786`,
   which today lists `jls.sim.*`, `jls.BitSetUtils`, `jls.Util`,
   `jls.SpatialIndex`, `jls.collab.op.*`) and the 80/82 thresholds
   (`pom.xml:812-813`) re-baselined from a fresh headless canonical-JDK-25 run.
6. **The algorithm goes in `docs/simulation-semantics.md`, not a code comment.**
   The prefix-carry construction is the specification; the reference
   implementation is its executable form.

Done means: a reviewer can read the alphabet table, the operator set and the
adder, run the differential test, and see the radix-2 collapse asserted rather
than claimed.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-028 | The arithmetic half of the radix-parameterized type system. Without the kernel, radix on a port is a label with nothing behind it. |
| FEAT-029 | Every N-ary element in TASK-0061 is a thin `react` over this kernel. Writing the operators inside the elements instead would duplicate them eight ways. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0056 | The kernel operates on the three planes of the value record TASK-0056 defines. There is no plane encoding at HEAD - values are `java.util.BitSet` - so there is nothing for a plane-wise operator to be plane-wise over. |

## Acceptance test

- **`jls.core.BalancedTernaryAdderTest.matchesThePerDigitReferenceOn200kVectors()`**
  (new): 200,000 seeded random 16-digit balanced-ternary operand pairs; assert
  the lane-packed result equals the per-digit reference result exactly. The seed
  is committed so a failure is reproducible. This is the differential test the
  determination measured against (`Add3Swar.java`).
- **`jls.core.RadixOpsTest.everyOperatorCollapsesToTheBinaryOperationAtRadix2()`**
  (new): over 10,000 random operand pairs at widths 1, 8, 32, 64, assert
  `min == BitSet.and`, `max == BitSet.or`, `complement == flip(0,bits)`,
  `sumModN == BitSet.xor`. This is the assertion that the radix-2 fast path is
  free; without it the claim is an argument.
- **`jls.core.RadixAlphabetTest.radixSixIsRefusedWithThePlaneArithmetic()`**
  (new): assert radices 2, 3, 4, 5 are accepted and 6 and 16 are refused, and
  assert the refusal message contains the plane count.
- **`jls.core.RadixOpsTest.planesToLanesRoundTrip()`** (new): plane -> lane ->
  plane is the identity over random vectors at every supported radix.
- **PIT**: the new package must clear the 80/82 thresholds. The slow reference
  gives PIT something to kill the fast implementation against, which is the
  cheapest way to reach the bar.

## Related GitHub issues

**No issue.** FEAT-028 and FEAT-029 have no tracker entry at all. Issue **#232**
(open) is the value representation and stops short of operators; cite it as
adjacent, never as covering. Do not create issues.

## Notes

- **Trap: `Long.compress` / `Long.expand` are microcoded on AMD Zen 1, Zen+,
  Zen 2 and Hygon Dhyana.** They compile to `PEXT`/`PDEP` on x86-64 BMI2, and on
  those parts the intrinsic is slower than a software fallback. JLS ships one
  offline jar to unknown student laptops. Either build the lane predicates with
  pure shift/mask at a measured cost or accept and **document** a
  platform-dependent constant. `07-mvl-determination.md` §4.3 flags this as
  unmeasured on affected hardware; measuring it is inside this task.
- **Trap: the naive implementation is 16-30x slower and looks correct.** A
  32-digit balanced-ternary add written by extracting a digit, adding, comparing
  against 3 and re-inserting measures **178.32 ns/op** against **9.79 ns/op** for
  the prefix-carry version plus ~7 ns of plane/lane conversion. Two independent
  implementations on two encodings agree on the gap. `Adder` is 108,025 of
  2,331,793 fired events (4.63%) on the RV32I census and all `react()` bodies
  together are 4.9% of loop time, so the prefix-carry algorithm costs ~+1.8% of
  loop time and the naive one costs ~+40% - a 1.4x whole-engine slowdown for a
  ternary machine. Binary circuits pay zero either way because dispatch is per
  element class.
- **Trap: `BitSetUtils.SumCarry` is the existing binary adder and it must not
  change.** `src/jls/BitSetUtils.java:196-235` is pinned by
  `jls.BitSetUtilsSumCarryTest` and is on the PIT target list already
  (`pom.xml:782`). The ternary adder is a sibling, not a generalization of it.
- **Trap: `BitSetUtils.ToString(bs, radix)` is already radix-general** via
  `BigInteger.toString(radix)` (`src/jls/BitSetUtils.java:83-92`) - but
  `ToStringSigned` (`:103-118`) reads the stored value as two's complement at
  the caller's declared width and **has no meaning in balanced ternary**, where
  sign is the leading non-zero trit and there is no sign bit. A balanced
  formatter is new code; reaching `ToStringSigned` from a balanced port is a bug.
- **Balanced digit sets exist only for odd radix.** Radix 4 balanced is either
  asymmetric `{-2..1}` or redundant `{-2..+2}`. Refuse it with a named message
  rather than picking one silently.

## Evidence

- `src/jls/elem/AndGate.java:64-75`, `OrGate.java:65-75`, `XorGate.java:71-81`,
  `Register.java:805`, `NotGate.java:65-74` - the five HEAD binary operations the
  kernel must collapse to, each verified.
- `src/jls/BitSetUtils.java:83-92` (radix-general `ToString`), `:103-118`
  (`ToStringSigned`, binary-only), `:196-235` (`SumCarry`).
- `pom.xml:780-786` - the PIT `targetClasses` list, which does not include
  `jls.core` at HEAD; `pom.xml:812-813` - the 80/82 thresholds.
- `07-mvl-determination.md` §0 (the plane arithmetic and the radix-6 cliff),
  §4.3 (the measured 178.32 vs 9.79 ns/op, the 200k-vector method, the
  `PEXT`/`PDEP` caveat), §1.1 stage 2 (4-6 weeks for the whole kernel stage, of
  which this task is the core).
- BRIEF §13 - 318 ns/event warm; `react()` bodies are 4.9% of loop time.
