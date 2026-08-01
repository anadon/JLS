# TASK-0031 - Semantic validation of a merged file

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0005

## Deliverable

A post-load semantic check that rejects a file that **parses** but is
structurally corrupt, naming the reason, so that a three-way textual merge can
never hand a user a circuit that loads and then simulates something they did
not draw. Today the loader catches some of this and misses the rest, and the
gaps are specific.

1. **A `jls.SemanticCheck` pass over a fully loaded `Circuit`**, run after
   `Circuit.finishLoad` and before the circuit is handed to an editor or a
   simulator. It returns a list of typed findings, each carrying a category, the
   stable ids involved and a one-line remedy - the shape
   `src/jls/LoadError.java` already establishes for the #58 taxonomy. It does
   not mutate.
2. **The check set, chosen because each is a merge failure mode, not a
   hypothetical.**
   - **Unattached-but-declaring wire ends.** At HEAD the put-attachment
     resolution is nested *inside* the loop over wire references
     (`src/jls/elem/WireEnd.java:102-131`). A `WireEnd` record carrying
     `attach`/`put` but **no** `wire` refs - exactly what a merge produces when
     one side deletes a wire and the other keeps its end - skips the attach
     block entirely and loads with `put == null`: a live connection point read
     as undriven. This is the flagship finding.
   - **Reference integrity over `sref`.** Every stable-id reference resolves to
     an element declared in the same `CIRCUIT` block.
   - **Net width consistency.** A wire net whose member puts disagree on bit
     width. `finishLoad` calls `net.setBits(vendPut.getBits())` per visited end
     (`src/jls/Circuit.java:1375-1379`) - last writer wins, silently.
   - **Multi-driver legality.** A net with more than one `Output` and no
     tri-state arming, judged against `docs/simulation-semantics.md` rather than
     re-specified here.
   - **Name collisions among top-level pins**, the case
     `docs/capability-roadmap/lf-06-diff-merge-vcs.md:437-446` singles out as
     the one a textual merge merges cleanly and JLS then refuses.
   - **Orphaned probes** naming a wire that no longer exists.
3. **One CLI surface.** A `-check file` flag added to `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:759-790`), exiting non-zero with the findings on
   stderr, so a git merge driver, a CI lane and a person all use the same code
   path.
4. **Invoked automatically after a load that came from a merge.** The check is
   cheap relative to a load; run it always, and report findings through the
   existing `LoadError` channel rather than a second reporting mechanism.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-012 | This is the "or" in the feature's acceptance criterion: a merge result either loads and elaborates, or is reported as a conflict. There is no third outcome, and this task is what makes the first branch checkable |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0005 | The reference-integrity check reads `sref` items, which only TASK-0005 creates. Against HEAD's dense `ref` - reassigned on every save (`src/jls/elem/Element.java:20-21`) - a three-way textual merge renumbers every later element, so a checker built for that form validates a class of file that cannot survive the merge it exists to guard |

## Acceptance test

`test/jls/SemanticCheckTest.java`, new. Each method builds its corrupt input
with `test/jls/CircuitTextBuilder.java` (the in-tree fixture builder) rather
than committing a broken `.jls`, so the corruption is visible in the test:

- `aWireEndWithAPutButNoWiresIsReported()` - the flagship. Emits a `WireEnd`
  with `attach` and `put` items and zero `wire` refs, loads it, and asserts
  (a) the load **succeeds** at HEAD, proving the gap is silent, and (b) the
  semantic check reports it with category `UNRESOLVED_ATTACHMENT` naming the
  element's stable id.
- `aDanglingStableReferenceIsReported()` - an `sref` to an id declared in no
  block.
- `aNetWhoseMembersDisagreeOnWidthIsReported()` - two puts of different widths
  on one net; asserts the finding, and asserts the *load* still succeeds, so
  the test documents which layer catches what.
- `twoTopLevelPinsNamedTheSameAreReported()` - the lf-06 §C4 penultimate row.
- `aCleanCircuitProducesNoFindings()` - over all three tracked fixtures under
  `test/fixtures/`. The anti-false-positive clause; a checker that fires on
  every real file is worse than none.
- `theCheckIsPureAndDoesNotMutate()` - canonical-saves the circuit before and
  after the check and asserts byte equality, using `DeterministicSaveTest`'s
  canonical writer as the oracle.

`test/jls/CliFlagTableTest.java`, extended: `-check` appears in the generated
usage text and is accepted by the parser, per the existing single-authoritative-
flag-table contract (issue #71).

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests | overlaps - a peer-delivered snapshot is the same untrusted-input class as a merged file, and `src/jls/collab/op/ElementVocabulary.java` is the existing gate for the type-token half. The semantic check is the structural half, and both should be reachable from one entry point |

**No issue covers the diff, merge and format work.** The registry records this
explicitly for FEAT-003, FEAT-012, FEAT-013 and FEAT-014 - the whole of
maintainer decisions D1, D2 and D3.

## Notes

- **The `WireEnd` nesting bug is the reason this task exists, and it should be
  read before anything is designed.** `next:for (int elid : loadWires) { if
  (loadPut != null) { ... } ... }` - the attach resolution runs once per wire
  reference and not at all when there are none. Two consequences: zero refs
  silently drops a real attachment, and N refs run `p.setAttached(this)` N times
  (harmless only because the second call finds `getAttached() == this`).
- **HEAD already rejects three things; do not re-implement them.**
  `WireEnd.init` throws on a dangling attachment element id (`:105-110`), on a
  named put that does not exist (`:110-115`), and on two ends attached to one
  put (`:120-127`, the fan-out rule). `Circuit.finishLoad` rejects duplicate
  `sid` declarations (`:1310-1321`). The new check is what those three do not
  cover.
- **Do not change `WireEnd.init`'s throws into findings.** They are load
  failures and should stay load failures; the #58 taxonomy is deliberate about
  which class a problem lands in. This task adds a layer, it does not relocate
  one.
- **`finishLoad` swallows `Exception` and `Error` into one message**
  (`src/jls/Circuit.java:1400-1418`) with the stack trace to stderr only.
  Findings must not route through that path or they will all read as "the
  circuit could not be assembled after reading".
- **Reference `docs/simulation-semantics.md` for multi-driver legality; do not
  restate it.** That document is normative and this one is not. A check that
  encodes its own opinion of what a legal net is will disagree with the
  simulator on the first tri-state circuit.
- **Coverage.** The check is a leaf class with no GUI dependency, so the
  93.0/92.0/84.5 JaCoCo aggregate and the 80/82 PIT bar are reachable and will
  be applied.

## Evidence

- `src/jls/elem/WireEnd.java:94-157` - `init(Circuit)` in full: the
  `next:for (int elid : loadWires)` header at `:102`, the `if (loadPut != null)`
  block at `:104-131` nested inside it, and the three existing throws.
- `src/jls/elem/WireEnd.java:38-44,629-662` - `loadAttach`, `loadPut`,
  `loadWires` and the three `setValue` sites that populate them.
- `src/jls/Circuit.java:1300-1422` - `finishLoad`: the duplicate-`sid` check at
  `:1310-1321`, legacy id minting at `:1322-1334`, the net partition walk at
  `:1358-1394` with `net.setBits` at `:1375-1379`, and the catch-all at
  `:1400-1418`.
- `src/jls/Circuit.java:1054-1135` - the item dispatch, including the `ref`
  arm at `:1105-1114` this task's `sref` successor joins.
- `src/jls/elem/Element.java:20-21` - "the file-local reference index,
  reassigned on every save".
- `docs/file-format.md:366-421` - §8, ids and references, and the rule that
  refs resolve only within their block.
- `docs/capability-roadmap/lf-06-diff-merge-vcs.md:412-446` - the conflict
  taxonomy and the stated acceptance criterion "loads and elaborates, or is
  reported as a conflict. There is no third outcome."
- `src/jls/JLSStart.java:759-790` - the authoritative flag table;
  `test/jls/CliFlagTableTest.java:20-25` - the contract it must satisfy.
- `test/jls/CircuitTextBuilder.java` - the in-tree corrupt-input builder.
