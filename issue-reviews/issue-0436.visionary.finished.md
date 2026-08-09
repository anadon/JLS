# Issue #436: TASK-0005: inserting one element into a saved circuit changes a bounded number of lines, because every reference names a permanent id instead of a save-time position
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "smaller diffs". The end it serves is that a `.jls` file stops being a *document with
positions* and becomes an *addressable set of records*. Every downstream thing the project
has committed to — the op layer (#167, already in `src/jls/collab/op/`), CRDT replication
(#171), semantic merge (#356), per-section versioning (#319), stable addressing for nets and
nested instances (#318) — needs to name a thing in a file without knowing what else is in the
file. Diff size is the *measurement*, not the goal. `docs/grand-architecture.md` §8 puts
`#165 stable ids + #166 canonical save` at the root of the collaboration spine; this task is
the third leg of that root, and it belongs on the main arc. The goal is right and I endorse it.

The *design* is where I part company. As written, the issue installs a second reference
vocabulary (`sref`/`sprobe`) beside the first, declares a two-form epoch, hands the epoch's
end to #319, and leaves the file with two identities per element. That is a compatibility-shaped
compromise for a compatibility problem the format already solved in 2015-vintage form: the
`FORMAT` header.

## Reframing 1 — one identity, not two: retire `id`, re-type `ref`

The new item kind is justified by O6 and §9 of `docs/file-format.md`: a new kind makes an old
reader fail loudly rather than misparse. But look at what the writer does in the same change.
`Circuit.save` (this checkout, `src/jls/Circuit.java:1482`) emits `FORMAT ` +
`formatVersionNeeded()`, and `Circuit.load` refuses any version above its own `FORMAT_VERSION`
with `NEWER_FORMAT` (`:765`). A file carrying stable-id references declares `FORMAT 3` under
either design. **An old reader therefore says "needs a newer JLS" whether or not the kind is
new** — it never reaches the item. The new item kind buys nothing on the axis it was justified
by, and costs a permanent second vocabulary plus an epoch-end decision the issue itself has to
defer (Open Question 1).

The alternative, which I think is the shape this should land in:

| | issue as written | reframing |
|---|---|---|
| item kinds | 9 (`ref`,`probe` deprecated on arrival, `sref`,`sprobe` added) | 7, unchanged; `ref-item = "ref" attr-name quoted`, `probe-item = "probe" quoted quoted` |
| base attributes | `id` emitted, except in `sref`-form blocks (a new concept: a field's presence conditioned on a sibling's form) | `id` gone from the table; `sid` is the only identity in the file |
| reader | two parallel arms, forever or until #319 migrates | one arm, `input.hasNextInt()` discriminates bare-int legacy from quoted sid |
| epoch | declared, with a written end owed to #319 | none — the int shape only ever appears in `FORMAT ≤ 2` files, which §9 already binds readers to accept *indefinitely* |
| Open Q1 | blocks the epoch note | dissolved: there is no second vocabulary to delete |
| Open Q2 | blocks execution | answered by construction: `id` is the old reference form's file-local handle, and it leaves with it |

The discrimination is one token wide and unambiguous: a quoted value starts with `"`,
`Scanner.hasNextInt()` is already the loader's idiom (`src/jls/Circuit.java` `ref` arm and
`probe` arm), and `unquoteAndUnescape` is already there for the `String` arm. `getID()`/`setID()`
and the save-time assignment loop stay exactly as they are — HDL export still consumes the dense
index in memory, so `src/jls/hdl/HdlExporter.java` stays untouched and #373 keeps its surface.
Only the *emission* stops.

Honest counter, recorded so it is not re-derived: a polymorphic value under an unchanged kind
token is harder on a hand-written third-party parser than a distinct kind. If that is judged
decisive, the fallback is not the issue as written — it is `sref`/`sprobe` with **`ref`, `probe`
and `id` removed from the writer permanently in the same change**. What I am arguing against is
the *epoch as a design commitment*, not the spelling. A two-form writer is what makes the epoch
real; a two-form *reader* over a frozen legacy version is just the §9 policy the project already
has.

## Reframing 2 — assert the invariant, not the diff

The ratchet as specified measures unified-diff lines and hunks against a constant `C` that must
be stated and not fitted. That oracle drags three problems in with it: Open Question 3 (what is
`C`?), the "diff-tool dependence" threat (algorithm and context width), and the legacy-replica
trap (O5), which forces the fixture to have interleaved replicas or the ratchet measures rank-0
inserts only.

All three are artifacts of the chosen metric. The property actually wanted is:

> **Block invariance.** For circuits `E` and `E ∪ {x}`, every element of `E` serializes to
> byte-identical block text in both saves, and the relative order of those blocks is unchanged.

Invariance plus order-preservation *entails* a bounded diff for any sane diff algorithm — the
inserted block is the only contiguous novelty — so the entailment is proved once in the test's
comment instead of re-measured per fixture. And it is strictly stronger: it catches a
position-dependent field the line count could absorb inside `C`. Under it:

- Open Question 3 evaporates; there is no constant to pick.
- Diff-tool dependence evaporates; the assertion is on bytes the writer produced.
- The legacy-replica trap stops gating the oracle. A rank-0 insert into `riscv-sum1to10.jls`
  still satisfies invariance — every other block is byte-identical, the hunk is just at the top
  of the file. FEAT-003's "three replica-id classes" becomes a coverage nicety, not a
  precondition, and the per-file alias table stops blocking anything here.
- The wired-fixture requirement of §11 stays, but as coverage of the reference path rather than
  as the thing that makes the metric meaningful.

Concretely: `test/jls/DiffStabilityRatchetTest` asserts
`blocksOfUnchangedElementsAreByteIdentical()` by splitting both saves on `ELEMENT`…`END` and
comparing the maps keyed by `sid`, plus `orderOfUnchangedBlocksIsPreserved()`. Keep one
line-count assertion as a human-facing smoke check if you like, but do not make it the oracle.

## Evidence the issue missed, which strengthens its own case

The issue cites #167 as "informs" and quotes `docs/operation-layer.md` as recording "a workaround
forced by the positional reference form". That workaround is no longer only in a doc — it is
shipped code. `src/jls/collab/op/NetBlocks.java:88-127` serializes a wire net by **mutating every
participating element's dense id into a local numbering, calling `ElementBlocks.saveBlock`, and
restoring the prior ids in a `finally`** — with a comment explaining the restore is mandatory
because HDL export reads those ids. `AddWire` consequently carries `List<ElementId> attach`
*alongside* blocks that name `int`s: the op layer already speaks stable ids at its boundary and
has to trampoline through the dense index to reach the serializer.

Under either reframing above, `toAddWire` collapses to "serialize each end", the `priorIds`
array and the `try/finally` mutation of shared element state disappear, and `AddWire`'s `attach`
list becomes derivable from the blocks themselves. That is a present, in-tree consumer with real
code to delete — a better argument for this task than the 206-line diff, and it also corrects the
issue's framing that #356 is the consumer and #167 merely "informs".

## Where the issue pulls against the arc

- **It exports its unfinished decision to #319.** FEAT-013 owns *section* versioning and raw-payload
  migration; giving it a reference-form conversion as well makes a feature about containers
  responsible for the semantics of the record body. The reframing removes the handoff entirely.
- **Conditional base attributes are a new format concept.** "`id` is present unless the block's
  references are in the `sref` form" is a rule no other attribute obeys and no existing test shape
  expresses; §5's omitted-when column currently keys on the element's own state, never on a sibling
  item's spelling. Deleting `id` outright avoids inventing the concept.
- **Two identities per element is the defect, restated.** The file already carries `id` *and* `sid`;
  that duplication is what made the amplification possible. A design that ends with both still
  present, one of them conditional, has not removed the duplication — it has scheduled its removal.

## What I would not change

The measurement discipline is the best part of this issue and survives the reframing intact: the
ratio-not-magnitude framing of P2, the explicit refusal to widen `C` rather than amend, the rule
that a regenerated golden changing anything but reference and `id` lines is a bug, the loud refusal
of a dangling reference (which today is a raw `IllegalStateException` out of `WireEnd.init`, and
should become a `LoadError` on the way past), and the `HdlExporter`-untouched boundary. I also
concur with the adversarial comment already on the issue that `blocked_by: [315]` is not a real
edge — `jls.elem.SaveTags` maps element type tags, not item kinds — and that §8 as written cannot
turn O4's wire-free measurement green.

## Acceptance criteria I am explicitly disregarding

- **"A new `sref`/`sprobe` item kind"** and **"the reader accepts both forms for one declared
  epoch"** (§7.1, §7.11, DoD `srefIsInTheGrammarAndBumpsTheFormatVersion`). Replace with: `ref` and
  `probe` values become quoted stable ids at `FORMAT 3`; the int shape is a `FORMAT ≤ 2` legacy
  accepted forever under the §9 policy that already exists.
- **"The `id` base attribute stays … and stays the human-facing per-block index"** (§7.1). It goes.
  The human-facing per-block index is the `sid` line that is already there.
- **"The ratchet's constant `C` is a literal in the test"** (DoD). Replaced by block-byte invariance,
  which needs no constant.
- **Open Questions 1 and 3** are not deferred, they are dissolved; Open Question 2 is answered as a
  consequence rather than as an exception.

Everything else in §8's method — the `ElementId`-keyed map beside `elementMap`, `ElementId`-typed
`loadAttach`/`loadWires`, the rekeyed `probeMap`, `formatVersionNeeded()` extension (note it already
exists at `src/jls/Circuit.java:1580`), the pre-epoch fixture, the one reviewed golden-regeneration
commit — carries over unchanged and is, if anything, a smaller change than the issue prices.
