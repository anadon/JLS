# TASK-0076 - Write-mask input on memory

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

A byte-lane write mask on `Memory`, so a drawn core can do a single-cycle
read-modify-write instead of a read cycle, a merge and a write cycle.

1. **An opt-in per-element mode, on the #199 precedent.** A saved
   `int lanes 1` attribute in `Memory.save`, written **only when on**
   (`src/jls/elem/Memory.java:445-449` is the exact shape `sync` uses, with the
   comment explaining why: pre-#199 circuits re-save byte-identically).
   `setValue(String,int)` (`:365-395`) gains the `lanes` case beside `sync`
   (`:383-387`).

2. **A `WM` input, appended last.** When the mode is on, `init`
   (`:181-202`) adds `new Input("WM", this, 0, 7*s, laneCount)` where
   `laneCount = ceil(bits/8)`, **after** the optional `clock` input - the same
   index-stability discipline #199 used (`:193-197`: "appended last so the
   pre-#199 input indices are unchanged"). `Memory.react` reads inputs by name
   (`getInput("WE")`, `getInput("address")`, ...) so index stability costs
   nothing here, but `copy` and the HDL walker do not, and a reordering breaks
   them silently.

3. **`SimEvent.MemoryWrite` gains a `mask` component.**
   `record MemoryWrite(int address, BitSet data, BitSet mask)`
   (`src/jls/sim/SimEvent.java:60-66`). The mask must be sampled at **post**
   time, exactly as the data is, not read from the element at completion - a
   completing write must commit the lanes that were selected when it was issued.

4. **The write path.** `react`'s `PinChanged` arm samples `WM` alongside `input`
   and posts the three-component payload (`:1372-1390`). The `MemoryWrite` arm
   (`:1408-1435`) reads the stored word, merges only the selected lanes, and
   `put`s the merged word; the `WriteRecord` it appends to `activity`
   (`:1002-1024`) records the **merged** word so `printChangedValues`
   (`:902`) stays honest about what the memory now holds.

5. **A stated rule for widths that are not a byte multiple.** `checkBits`
   (`:87-95`) is the one-string-two-surfaces constraint. Either the mode
   requires `bits % 8 == 0` and says so in that string, or the top lane is
   declared partial and its width is stated. Pick one and write it into
   `docs/simulation-semantics.md` §8.4.

6. **ROM refuses the attribute.** `init` creates `input`/`WE` only for
   `Type.RAM` (`:186-190`), so `lanes` on a ROM has no pin to attach to. It must
   raise a load diagnostic, not be silently ignored - a silent ignore is exactly
   the class of hole TASK-0003 is closing.

7. **Docs.** `docs/simulation-semantics.md` §8.4 gains the lane rule beside the
   level-sensitive and synchronous-write rules; `docs/file-format.md` §5 gains
   the attribute. No new tag, so **no `FORMAT` version cost**.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-036 | The "byte lanes on `Memory`" half of the feature verbatim. A guest that does `sb`/`sh` against a 32-bit word cannot be modeled without it, and a 3-byte-address UART is not addressable through a memory without it. |

## Prerequisite tasks

None. TASK-0013 (capacity as a byte budget) touches the same class but a
different question - *how many* words fit, not *what* a word write means -
and neither reads the other's output.

## Acceptance test

`test/jls/elem/MemoryModelTest`, extended (the file already carries the #199
synchronous-write suite at `:377-432`, which is the exact shape to copy):

- `laneMaskedWriteLeavesUnselectedLanesUnchanged()` - a 32-bit word holding
  `0x11223344`, a write of `0xAABBCCDD` with mask `0b0010`, asserting
  `0x1122CC44`.
- `allLanesSelectedIsIdenticalToAnUnmaskedWrite()` - the equivalence that lets an
  existing circuit turn the mode on with no semantic change; run the same
  stimulus against a `lanes`-off memory and compare `storedAddresses()` and every
  stored word.
- `zeroMaskWritesNothingAndRecordsNoActivity()` - asserts
  `getActivityTrace()` (`:1486`) is unchanged, so a no-op write does not pollute
  `printChangedValues`.
- `laneMaskIsSampledAtPostTimeNotAtCompletion()` - **the trap test**. Change `WM`
  between the post and the `now + accessTime` completion and assert the write
  uses the posted mask. This is the same defect class #199 was filed for: a
  transient on a control input committing the wrong thing.
- `laneMaskOnARomIsRefusedAtLoad()` - asserts a diagnostic, not a quiet load.

`test/jls/AllElementsRoundTripTest` - the existing `Memory` fixture must
re-save **byte-identically** (the attribute is written only when on), and a
second fixture with `lanes 1` round-trips as a fixed point.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | byte lanes on `Memory` | **no issue** |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on - `sb`/`sh` against a word-addressed memory is the first place a drawn RV32 core needs this |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | overlaps - the merge is one more `BitSet` operation per write on the hot path. Write it against `BitSet` today; #232 owns the representation change and will rewrite this merge with the rest |

## Notes

- **The record-pattern trap.** Adding a component to `MemoryWrite` changes its
  canonical constructor and breaks every **deconstruction pattern**. There is
  exactly one: `case MemoryWrite(int addr, BitSet data)` in `Memory.react`
  (`src/jls/elem/Memory.java:1408`). The other 26 `react` implementations name it
  only as `case MemoryWrite _` in their throw arms
  (`src/jls/elem/Register.java:815-817` is the shape) and are unaffected. The
  compiler finds all of them; do not go looking by grep.
- **Why not carry the mask on the element instead of in the payload.** Because
  the mask must be latched at issue time along with the address and the data.
  The element field would be re-read at `now + accessTime`, which is precisely
  the glitch hazard `docs/simulation-semantics.md` §8.4 documents for
  level-sensitive writes.
- **`DenseWordStore.put` truncates to one `long`**
  (`src/jls/elem/Memory.java:1120-1125`: `words[addr] = asLongs.length == 0 ? 0
  : asLongs[0]`). Do the merge on the `BitSet` **before** calling `put`, or the
  merge is silently applied to a truncated word for `bits <= 64` and not at all
  for the sparse path.
- **`checkBits` and `CAPACITY_CONSTRAINT` are one-string-two-surfaces
  contracts** (`:56-95`, enforced at `setValue` `:367-379`) pinned by
  `DialogValidationTest`. A new width rule gets the same treatment or the dialog
  and the loader drift.
- **Synchronous write and lanes are orthogonal and must compose.** The write
  gate (`:1362-1374`) decides *whether* to post; the mask decides *what* to
  commit. Test the cross product, at least at the corners.
- **The unmasked path must stay allocation-neutral.** With the mode off, do not
  allocate an all-ones mask per write; pass a shared constant or a `null`-mask
  sentinel handled once in the completion arm.

## Evidence

- `src/jls/elem/Memory.java:181-202` - `init`, the pin order, and the #199
  comment at `:193-197` on appending last.
- `:365-395` - `setValue(String,int)`, with the `sync` case at `:383-387`.
- `:436-470` - `save`, with the write-only-when-on `sync` block at `:445-449`.
- `:1335-1470` - `react`: the `PinChanged` arm's write post at `:1383-1387`, the
  `MemoryWrite` record pattern at `:1408`, the `activity` append at `:1420-1427`,
  the `put` at `:1430`.
- `:1002-1024` - `WriteRecord` and `ACTIVITY_LIMIT`; `:902` -
  `printChangedValues`; `:1486` - `getActivityTrace`.
- `:1072-1145` - `DenseWordStore`, with the truncating `put` at `:1120-1125`;
  `:1156-1214` - `SparseWordStore`.
- `src/jls/sim/SimEvent.java:60-66` - the `MemoryWrite` record.
- `docs/simulation-semantics.md` §8.4 - the level-sensitive default, the glitch
  hazard found building a single-cycle CPU (#199), the synchronous-write mode,
  and the three `MemoryModelTest` methods that pin it.
- `docs/machine-calibration.md` §5.3 - the three-address UART, which is the
  concrete consumer of sub-word addressing on the guest bus.
