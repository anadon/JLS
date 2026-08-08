# N-ary wires: save-format changes and the migration path

**Status: proposal — input for issue elaboration, not normative.**
Companion to [`nary-01-value-model.md`](nary-01-value-model.md) (the
model) and [`nary-03-issue-handoff.md`](nary-03-issue-handoff.md)
(tracker actions). Normative anchors it builds on:
`docs/file-format.md` (FORMAT header and version negotiation),
`docs/batch-interface.md` (frozen batch contracts),
`docs/simulation-semantics.md` §2 (the value domain being replaced).

## 1. The existing machinery this rides on (nothing new needed)

- **Version negotiation already exists.** `Circuit.readFormatHeader`
  refuses files newer than `FORMAT_VERSION` as `NEWER_FORMAT` instead of
  misparsing; a writer emits the highest version whose features the file
  actually uses (`docs/file-format.md`, rule quoted in
  `docs/capability-roadmap/sweep-01-values-and-logic.md` "The file
  format"). So N-ary payloads are a **FORMAT bump with no compatibility
  event**: a circuit using no non-binary feature keeps writing the older
  header and older bytes, and old JLS keeps loading it.
- **Undo and crash recovery inherit for free.** `CircuitSnapshot` stores
  deflated save-format text and restores through the ordinary load path
  (`ARCHITECTURE.md`, save/load pipeline), so once save/load carries
  intervals, undo/redo and `.jls~` checkpoints do too.
- **The load-error taxonomy already has the right buckets.** Interval
  mismatches and multi-driver-on-non-binary refusals land in the
  structured `LoadError` categories (`MALFORMED` / `ELEMENT_ERROR`) with
  file, element, and both intervals named — the #344 criterion 4
  diagnostic, generalized.

## 2. New payloads

All additive; none is written for a purely binary circuit.

1. **Interval on ports/nets.** Carried on the elements that determine
   net properties (the same placement as `bits` today —
   `docs/simulation-semantics.md` §2: width is "a property of elements
   and wire nets"). Suggested save items: `int lo` and `int hi` on the
   owning element's attribute list, written only when the interval is
   not `[0,1]`. (Equivalent alternative: a single `range lo hi` line;
   pick whichever the `Attribute` registry expresses most naturally.
   Two `int` items need no new item kind.)
2. **New element tags** for the N-ary family and the bridge element —
   rows in the frozen `SaveTags` table, exactly the ritual
   `docs/file-format.md`'s tag table and `SaveTagsTest` /
   `FileFormatSpecTest` enforce.
3. **Values in saved state** (e.g. `Constant` value, register init,
   memory init if those elements ever go N-ary): serialized as the §4
   bundle numeral of `nary-01` (a single signed integer, or the existing
   RLE/encodings where applicable) — signed decimal in the text format,
   canonical form (no `+`, no leading zeros) to preserve the
   byte-canonical serializer discipline (#166, `DeterministicSaveTest`).

**Migration converter: none.** Existing files load unchanged and
re-save byte-identically; there is no rewrite pass, no version-0 hazard,
no `-adopt`-style step. The migration cost is entirely in the *code*
(the #322 value-type migration), not in user files.

## 3. Batch interface and interchange surfaces

`docs/batch-interface.md` is a stability contract; each change below
follows its own rule (compatibility flag or major bump, the `Memory
sync` precedent):

- **`-t` test-vector grammar**: gains signed integer literals for N-ary
  pins via the existing token-rewrite pre-pass (#361's mechanism —
  already how hex literals arrive). Binary vectors are untouched.
- **stdout / trace display**: N-ary values print as the bundle numeral
  (signed decimal), with X/Z/U rendered as the four-state work (#322)
  specifies. Balanced rendering (`-`, `0`, `+`) for `[-1,+1]` is a
  display option, per #361's balanced-rendering scope.
- **VCD**: VCD's value alphabet cannot carry N-ary. Ship a
  machine-parseable **radix/interval manifest in a `$comment`** (the
  #361 open-question-3 recommended default) mapping each N-ary variable
  to `(lo, hi, width)` with values dumped as the bundle numeral in a
  `real` or vector encoding — and state plainly in
  `docs/vcd-interop.md` that this is a JLS profile extension readers
  may ignore. Binary VCD output is byte-identical.
- **HDL export**: N-ary elements get explicit rows in `HdlExporter`'s
  four class sets; anything exported is a **binary-encoded lowering
  whose encoding is named in the emitted header** (#361's rule — an
  external tool simulates an encoding, not ternary, and the header must
  say so).

## 4. Sequencing against the four-state migration (#322)

The one rule that must not be missed, inherited from #344 §6 and
amended for the interval model:

> Reserve the interval accessors **inside** the #322 value-representation
> migration (its TASK-0056), not after it. Concretely: `Put`/`WireNet`
> gain `lo()`/`hi()` (both returning `0`/`1`), the value type's field
> list is frozen in writing with the generic-tier extension point named,
> and `docs/simulation-semantics.md` §2 is re-anchored as an
> alphabet-parameterized statement. If the value migration ships without
> this, the remaining work becomes a second value migration — the
> expensive kind.

This supersedes the `int radix()` formulation carried in #344's
TASK-0056 description. It is one accessor pair and one paragraph of
governance — the cost is the recorded-decision motion (see `nary-03`
§4), not the code.

Order of landings (each keeps the tree green, keystone B's discipline):

1. **#322 stages as filed**, with the reservation above folded in.
   Nothing observable changes; binary goldens byte-identical.
2. **Interval on ports/nets + validation** (the #344 TASK-0059 scope,
   generalized): editor and load refusals live; every registered type
   asserts `[0,1]`; still nothing drawable is non-binary; **no saved
   bytes move** (this is what keeps the format bump deferrable).
3. **Kernel + generic tier + differential oracle** (TASK-0060
   generalized): pure leaf module, nothing consumes it yet.
4. **Element family in coverage-sized batches + FORMAT bump** (the #361
   scope): the first commit that can *write* a non-binary file is the
   commit that bumps the format; the palette view dimension
   (TASK-0105) must land first so the default palette is unchanged.
5. **Interop surfaces** (`-t`, VCD manifest, export lowering rows),
   each behind its batch-contract gate.

## 5. Test blast radius (extends, not breaks)

- Round-trip and format: `AllElementsRoundTripTest`, `SaveTagsTest`,
  `FileFormatSpecTest`, `FormatHeaderTest`, `DeterministicSaveTest`,
  `GenerativeRoundTripFuzzTest` — all extend with N-ary fixtures; none
  is contradicted, because stage 2 moves no bytes and stage 4 is
  purely additive under negotiation.
- Semantics: new golden family for N-ary fixtures (batch run of a
  ternary fixture vs. committed golden, byte for byte — #361 IC-1);
  the kernel differential corpus (fast tier vs. generic tier, seeded,
  seed recorded).
- The #322 blast radius (goldens re-derived for X/Z/U, the 21 tests
  that pin two-state behavior by name) is **unchanged by this
  proposal** — it belongs to #322 and is already cataloged in
  `docs/capability-roadmap/keystone-b-migration.md` §4.
- Registry totality: interval reported by every element type, swept
  over `ElementRegistry` (the #344 IC-1 pattern), so a future type
  cannot silently default.

## 6. What a purely binary user experiences

Nothing. Same bytes on save, same goldens, same palette, same warm-loop
performance within noise, same VCD. That is the acceptance experience
for stage 2 and the standing invariant for every stage after it —
identical in spirit to #344's "the correct experience of this feature
for a binary user is that nothing happened."
