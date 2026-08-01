# TASK-0005 - Reference elements by stable id, with a diff ratchet

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

The writer emits stable ids rather than dense save-time indices in reference
positions, the reader accepts both for one epoch, and a ratchet test asserts
that inserting one element changes a bounded number of lines.

1. **A new item kind, `sref`**, taking a quoted stable id, added to the grammar
   at `docs/file-format.md:126-136` alongside `ref` and `probe`. Per §9
   (`docs/file-format.md:441-446`) a new item kind **requires a `FORMAT` bump**:
   `Circuit.FORMAT_VERSION` goes 2 -> 3 (`src/jls/Circuit.java:102`) and
   `formatVersionNeeded()` emits 3 only for files that actually contain an
   `sref`, keeping every `sref`-free circuit readable by current JLS.
2. **The writer.** `src/jls/elem/WireEnd.java:604-613` is the only site that
   emits references: ` ref attach <elid>`, ` ref wire <elid>` and
   ` probe <elid> "<name>"`, each from `LogicElement.getID()` / `WireEnd.getID()`
   - the dense index reassigned on every save (`src/jls/elem/Element.java:22`,
   assigned in `Circuit.save` at `src/jls/Circuit.java:1499-1503`). These become
   ` sref attach "<sid>"`, ` sref wire "<sid>"` and ` sprobe "<sid>" "<name>"`.
3. **The reader accepts both for one epoch.** `src/jls/Circuit.java:1107-1116`
   (the `ref` arm) and the `probe` arm gain `sref`/`sprobe` siblings resolving
   through a new `Map<ElementId,Element>` built alongside the existing
   `elementMap` (`src/jls/Circuit.java:84`). `WireEnd.setValue`
   (`src/jls/elem/WireEnd.java:625-640`) grows `ElementId`-typed `loadAttach`
   and `loadWires` counterparts; `WireEnd.init` (`:94-157`) resolves through
   whichever the file supplied. The epoch's end is a recorded decision, not
   part of this task.
4. **`Element.setID` / `getID` stay** - the dense id remains the `id` base
   attribute (`docs/file-format.md:214`) and stays the human-facing per-block
   index. Only *reference positions* move.
5. **The ratchet.** A test fixture circuit of N elements; insert one element
   whose stable id sorts into the middle; save; diff. Assert the changed-line
   count is bounded by a constant times the inserted element's own block size,
   independent of N.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-003 | Uncompressed canonical text is only useful if a one-element edit is a small diff; with dense refs it is not. |
| FEAT-012 | A three-way textual merge over dense indices produces files that parse and are semantically corrupt (decision D2). Stable references remove the renumbering that creates most of those conflicts. |

## Prerequisite tasks

None at HEAD: stable ids already exist, are minted at construction, survive
save/load/undo, and are unique per block (`src/jls/elem/Element.java:24`,
`src/jls/Circuit.java:1305-1334`, `docs/file-format.md:378-405`).

## Acceptance test

`test/jls/DiffStabilityRatchetTest.java`, new:

- `insertingOneElementChangesABoundedNumberOfLines()` - builds a 200-element
  circuit via `CircuitTextBuilder`, saves it plain-text, inserts one element
  with a stable id that sorts to the middle of the canonical order, saves
  again, and asserts the unified-diff changed-line count is at most a stated
  constant (the inserted block's own lines plus its neighbors' reference
  lines). Must fail on today's code: a mid-insert renumbers every later `id`
  and every `ref` to them.
- `theSameRatchetHoldsAtFourTimesTheSize()` - the identical assertion at 800
  elements with the same constant, so the bound is proved independent of N
  rather than merely small once.

`test/jls/FileFormatSpecTest` gains
`srefIsInTheGrammarAndBumpsTheFormatVersion()`, asserting the spec's item-kind
list and `Circuit.FORMAT_VERSION` agree, and that a circuit containing no
`sref` still writes `FORMAT 2`.

`test/jls/CircuitRoundTripTest` gains
`aFileWrittenWithDenseRefsStillLoads()`, loading a checked-in pre-epoch fixture.

## Related GitHub issues

**No issue.** The diff-amplification and format work has no tracker entry - the
whole of decisions D1, D2 and D3 (FEAT-003, FEAT-012, FEAT-013, FEAT-014) is
unfiled.

| # | title | relationship |
|---:|---|---|
| 171 | Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo (collab Stage 2) | overlaps - #171 addresses elements by stable id already (`docs/file-format.md:404-405`); moving the *file* to stable references removes the last dense-index surface between the two |

Recorded decisions, closed, cite as such and not as open: **#165** (stable ids),
**#166** (canonical order), **#129** (plain-text container).

## Notes

- **The `FORMAT` bump is not optional.** §9 is explicit: a new item kind
  requires the bump so current readers say "needs a newer JLS" instead of
  "malformed file" (`docs/file-format.md:441-446`). Adding `sref` under
  `FORMAT 2` would make every 4.x reader report a corrupt file.
- **Every golden regenerates.** `DeterministicSaveTest` (six byte-identity and
  fixed-point tests, `test/jls/DeterministicSaveTest.java:58-244`),
  `AllElementsRoundTripTest`, `CircuitRoundTripTest`,
  `jls.elem.AttributePersistenceTest.savedBytesMatchTheHandwrittenFormat`, the
  worked example at `docs/file-format.md:497+`, and the HDL goldens under
  `test/resources/hdl/` all pin exact bytes. Regenerate deliberately and review
  the diffs; a regenerated golden that changes more than the reference lines is
  a bug in this task.
- **Stable ids are strings, not ints** (`replica:counter`,
  `docs/file-format.md:385-389`), so `sref` values must be quoted and go through
  the §6 escaping path, not `Scanner.nextInt`. `WireEnd`'s `probeMap` is keyed
  `Map<Integer,String>` (`src/jls/elem/WireEnd.java:46`) and must be rekeyed.
- **`getID()` has non-file consumers.** `HdlExporter` synthesizes net names from
  it - `net_<id>` at `src/jls/hdl/HdlExporter.java:353`, `net_u<id>` at `:381`,
  and `mux_`/`dec_`/`tt_`/`sm_`/`unc_` at `:725, 753, 870, 1003, 1313`. Those
  are TASK-0008's problem; this task must not change them, and must not assume
  `getID()` disappears.
- **`legacy:` ids are minted at load in file order**
  (`src/jls/Circuit.java:1321-1334`) for pre-#165 files, so an old file
  re-saved under this task gets `sref`s pointing at `legacy:N` ids - stable
  thereafter, but not stable across the one conversion. Say so in the epoch
  note.
- **Canonical order already sorts by stable id** (`src/jls/Circuit.java:1493-1497`),
  so the diff bound is achievable; without #166 it would not be.

## Evidence

- `src/jls/elem/Element.java:21-24` - the dense `id` "reassigned on every save",
  and `stableId` minted at construction.
- `src/jls/Circuit.java:1486-1503` - canonical sort by stable id, then dense id
  assignment in that order.
- `src/jls/elem/WireEnd.java:604-613` - the three reference-emitting lines, the
  only such site in `src/`.
- `src/jls/Circuit.java:1107-1116` - the `ref` reader arm; `:84` the
  `elementMap`.
- `docs/file-format.md:366-380` (ids "not stable across saves"), `:378-405`
  (stable ids), `:407-420` (canonical order), `:441-446` (item-kind bump rule).
- BRIEF.md §11 D2: dense save-time ids "reassigned on every save" are the
  structural cause; "referencing by STABLE ID rather than by dense file-local
  index is the structural fix".
