# TASK-0013 - Memory capacity as a byte budget, initialized copy-on-write

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

`Memory`'s dense-storage decision becomes a byte budget with declared headroom
above the guest-RAM minimum, and initializing a memory stops allocating a
second full copy of it.

1. **The word-count cliff becomes a byte budget.**
   `Memory.DENSE_CAPACITY_LIMIT = 1 << 22`
   (`src/jls/elem/Memory.java:1224`) is a *word* count, so a 32-bit memory
   tops out at exactly 16 MiB — the figure `docs/machine-calibration.md` §5.2
   gives as the Linux **minimum** for a usable shell. Replace it with
   `DENSE_BUDGET_BYTES` and a predicate in `newWordStore()`
   (`:1232-1237`) over the real cost of `DenseWordStore`: `8` bytes per word
   for the `long[]` plus `1/8` byte per word for the `present` `BitSet`
   (`:1074-1088`), i.e. `8.125 × capacity` regardless of `bits <= 64`. Set the
   default budget so a 16 MiB 32-bit guest RAM sits inside it with stated
   headroom, and state the resulting word count in the field's javadoc rather
   than leaving the reader to multiply.

2. **The budget is a declared, overridable number, not a literal.** Expose it
   as a system property with the constant as the default, so a machine-scale
   experiment (TASK-0022, TASK-0080) can raise it without a source edit and so
   the chosen value appears in one place that documentation can cite.

3. **Initialization stops doubling heap.** `Memory.initSim` builds `initMem`
   (`:1248`), fills it from the init file or the built-in text
   (`:1250-1303`), then does `mem = initMem.copy()` (`:1309`), which for the
   dense store is `words.clone()` plus a `BitSet` clone (`:1094-1098`). At the
   budget above that is two full images resident for the whole run. Introduce a
   copy-on-write `WordStore` implementation: `mem` delegates reads to the
   shared `initMem` until its first `put`, at which point it materializes its
   own backing. `WordStore` (`:1030-1065`) gains no new method; the new class
   implements the existing four.

4. **`addresses()` stays correct under sharing.** The dense implementation
   walks `present.nextSetBit` in ascending order (`:1128-1136`) and both the
   save path and `MemTrace` depend on that order. The copy-on-write store must
   produce the same ascending union of base and overlay addresses.

5. **A package-private witness for the sharing.** Add
   `boolean sharesBackingWith(WordStore other)` (package-private, documented as
   test-only) so the acceptance test can assert copy-on-write structurally
   rather than by sampling heap.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-006 | "Capacity as a byte budget rather than a word-count cliff" is the feature's own phrasing; today a one-word overrun silently changes the storage strategy and the per-word cost by an order of magnitude. |
| FEAT-036 | The byte-lane work is about *what* a word is; this is about *how many* fit. Both must agree on the cost model before either ships a Linux-capable memory. |

## Prerequisite tasks

None. TASK-0034 (the raw bulk-image section) writes the same images to *file*;
this task governs their *runtime* representation and neither reads the other's
output.

## Acceptance test

`test/jls/elem/MemoryModelTest.java`, extended (the file already carries the
Memory model suite that raised the `jls.elem` floor):

- `denseStorageIsChosenByBytesNotByWordCount()` — a `@ParameterizedTest` over
  `(bits, capacity)` pairs asserting that two memories with equal
  `bits × capacity` byte cost get the same store class, and that the pair
  straddling the budget flips exactly once. **Fails at HEAD**: a 64-bit
  4 Mi-word memory and a 4-bit 4 Mi-word memory both take the dense path
  despite a 16× cost difference in the guest's terms.
- `aSixteenMebibyteThirtyTwoBitGuestRamStaysDense()` — the concrete regression
  the number exists for: `bits=32, capacity=4194304` is dense, and so is the
  next power-of-two step up, which is the "zero headroom" finding.
- `initializedMemorySharesItsBackingUntilTheFirstWrite()` — after `initSim`,
  `sharesBackingWith(initMem)` is true; after one `put`, false; and reads
  before and after the write return byte-identical values to a run against
  today's eager copy.

`test/jls/elem/MemoryInitEncodingTest#rleMemorySimulatesLikeRawMemory()` must
stay green unchanged — it is the existing proof that the two init encodings
produce identical simulations, and it is what catches a copy-on-write read
path that diverges.

## Related GitHub issues

**No issue** for either half.

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | overlaps — `DenseWordStore.get` returns `BitSet.valueOf(new long[]{…})` per read (`Memory.java:1110-1116`), which is #232's allocation pattern inside the store. This task must not change that signature; #232 owns it |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on — any machine with realistic RAM crosses this budget |

## Notes

- **The cap is not a validity rule and must not become one.**
  `Memory.checkCapacity` (`src/jls/elem/Memory.java:72-75`) rejects only
  `capacity < 1`, with the same string on both the dialog and the loader
  surfaces (`CAPACITY_CONSTRAINT`, `:58`), and is enforced at
  `setValue("cap", …)` (`:373-379`). A memory above the budget must still
  *load*; it falls back to `SparseWordStore` (`:1156-1214`) as it does today.
  Adding a capacity refusal here would break every existing large-memory file
  and would trip `DialogValidationTest#memoryCapacityRuleIsOneStringOnTwoSurfaces`.
- **`capacity` is an `int`.** The budget can therefore never exceed
  `2^31 - 1` words no matter what bytes are allowed; say so in the javadoc so
  the next reader does not propose a 64-bit budget against a 32-bit field.
- **The sparse fallback is also the `bits > 64` path** (`:1234`). Widening the
  budget without touching that condition is correct and deliberate — a
  128-bit word cannot live in a `long[]`.
- **`put` truncates to one long today** (`:1120-1125`:
  `words[addr] = asLongs.length == 0 ? 0 : asLongs[0]`). The copy-on-write
  overlay must preserve that exact behavior, including the empty-`BitSet`
  case, or narrow-word goldens shift.
- **Do not make the shared `initMem` mutable after `initSim`.** Nothing writes
  it today, but `Memory.react` and the reset path both reach for it; a stray
  write into a shared base would corrupt the "initial image" the save format
  round-trips.
- **Budget arithmetic belongs in the commit message and the javadoc.** The
  study's figure is 15.87 bytes per word of *save text*
  (`BRIEF.md` §11 D1) — that is the file cost, not the heap cost, and the two
  must not be conflated in the constant's documentation.

## Evidence

- `src/jls/elem/Memory.java:1224` — `DENSE_CAPACITY_LIMIT = 1 << 22`, with the
  comment "32 MB of longs" that omits the `present` bitmap.
- `:1072-1145` — `DenseWordStore`: the `long[]`, the `present` `BitSet`, the
  copy constructor at `:1094-1098`, `get` at `:1110-1116`, `put` at
  `:1120-1125`, `addresses` at `:1128-1136`.
- `:1156-1214` — `SparseWordStore`, the fallback.
- `:1232-1237` — `newWordStore`, the single decision site.
- `:1245-1321` — `initSim`; the eager `mem = initMem.copy()` at `:1309`.
- `:56-75` — the capacity constraint string and `checkCapacity`; `:373-379` —
  the loader enforcement site.
- `docs/machine-calibration.md` §5.2 — the >= 12 MiB / 16 MiB recommended
  guest-RAM figures with their method.
