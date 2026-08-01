# TASK-0006 - Plain text as the default container, with the autosave policy

**Status:** proposed | **Cost:** 1 wk | **Blocked by:** none

## Deliverable

The default write path becomes plain canonical text, compression stays an
explicit option, and the autosave and undo-snapshot containers get a decided,
tested policy.

1. **The default flips.** `Circuit.saveContainer` is initialized to
   `FileAbstractor.Container.XZ` (`src/jls/Circuit.java:63-64`); it becomes
   `PLAIN_TEXT`. `FileAbstractor.writeCircuit(File,String)` - the two-argument
   overload - hardcodes `Container.XZ` (`src/jls/FileAbstractor.java:197`); it
   becomes `PLAIN_TEXT`. The `Container` enum javadoc
   (`src/jls/FileAbstractor.java:43-57`) and the class javadoc (`:27-35`), both
   of which state XZ is the default, are corrected.
2. **XZ stays available and explicit.** `Editor.saveAs`
   (`src/jls/edit/Editor.java:201-204`) currently maps the text filter to
   `PLAIN_TEXT` and everything else to `XZ`; it inverts, with a compressed
   filter selecting `XZ`. No write path loses the option; reading is unchanged
   because `openCircuit` already sniffs XZ, zip and plain text
   (`src/jls/FileAbstractor.java:120-135`).
3. **The autosave policy is decided and implemented.** `.jls~` checkpoints go
   through `SimpleEditor.writeCheckpointInBackground`
   (`src/jls/edit/SimpleEditor.java:203-222`), which calls the two-argument
   `writeCircuit` and therefore inherits whatever the default is - today XZ,
   after step 1 plain text, in both cases *by accident*. Make it explicit:
   pass a `Container` at the call site with a comment stating the reason.
   Recommended value **XZ**: a checkpoint's constraint is write volume on every
   edit, not diffability - it is never committed and never reviewed. Whichever
   is chosen, it must be an argument, not the default.
4. **The undo-snapshot policy is decided and recorded.**
   `CircuitSnapshot.capture` deflates save-format bytes
   (`src/jls/edit/CircuitSnapshot.java:32-42, 66`). Its constraint is heap
   pressure across an undo stack, not diff. Recommendation: **keep deflating**,
   recorded in the class javadoc as a decision rather than an accident.
5. **`docs/file-format.md` §1 Containers** (`:36-77`) is updated to state which
   container the reference implementation writes by default and that reading is
   unaffected. This is a policy statement, not a format change: **no `FORMAT`
   bump** - the bytes inside the container are identical.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-003 | The uncompressed canonical default is the feature; without it, textual diff and git delta compression between revisions are both unavailable regardless of what the text looks like. |

## Prerequisite tasks

None. The mechanism is already built and tested; this is a policy change plus
test updates.

## Acceptance test

`test/jls/FileAbstractorTest.java` (existing) - the two tests that pin today's
default invert and are renamed, so the change is visible in the test names:

- `aFreshCircuitDefaultsToTheXZContainer` (`:115`) becomes
  `aFreshCircuitDefaultsToThePlainTextContainer()`, asserting
  `new Circuit(...).getSaveContainer() == Container.PLAIN_TEXT`.
- `defaultWriteIsStillXZCompressed` (`:124`) becomes
  `defaultWriteIsBarePlainText()`, asserting the written bytes equal the
  circuit text with no compression wrapper - the assertion
  `plainTextWriteIsTheBareCircuitText` (`:133`) already makes, now on the
  default path.
- `plainTextWriteReplacesAnXZFileAtomically` (`:174`) gains a mirror,
  `xzWriteReplacesAPlainTextFileAtomically()`, so the now-explicit option keeps
  its atomicity proof.

`test/jls/edit/CheckpointWriterTest.java` (existing) gains
`checkpointUsesTheDeclaredContainerNotTheDefault()`: it writes a checkpoint and
asserts the on-disk bytes match the container step 3 chose, by sniffing rather
than by reading `FileAbstractor`'s default - so a later default flip cannot
silently retarget autosave again.

## Related GitHub issues

**No issue.** The format and diff-stability work is unfiled; decision D1 is
recorded in BRIEF.md §11 and nowhere on the tracker.

Recorded decisions, closed, cite as such: **#21** (chose XZ for size),
**#129** (added the plain-text container), **#38** (the expansion cap and
hostile-archive hardening).

## Notes

- **The 64 MiB cap does not regress.** `MAX_CIRCUIT_TEXT_BYTES`
  (`src/jls/FileAbstractor.java:65`) is measured against *decompressed* text, so
  flipping the default changes nothing about it. Do not "relax" it as part of
  this task.
- **Bulk payloads are the real consequence and are out of scope here.** At the
  measured 15.87 bytes/word a 16 MiB RAM image is ~66 MB of text and alone
  exceeds the cap (BRIEF.md §11 D1). The split - structural content in
  uncompressed text, memory images and checkpoints in a raw section or sidecar -
  is TASK-0034's raw bulk-image section, not this task. This task must not
  ship a default that makes a `Memory`-heavy circuit unsavable; if the corpus
  shows one, that is a real sequencing finding, not a reason to abandon the
  flip.
- **`Editor.save` reads `circuit.getSaveContainer()`**
  (`src/jls/edit/Editor.java:70-72`), so a circuit loaded from an XZ file keeps
  writing XZ. Decide and test whether opening an XZ file pins the container
  (recommended: yes, silent conversion on save surprises users) and state it in
  the doc.
- **`JLSStart` already has `-savetext`** (`src/jls/JLSStart.java:787`) writing
  `PLAIN_TEXT` explicitly (`:514-516`). After the flip its name is misleading;
  keep the flag working (`CliFlagTableTest` pins the flag table) and record
  that its meaning is now "write plain text regardless of the circuit's
  container".
- **Goldens that assert compressed bytes** must be found before the flip:
  `CliTextSaveTest`, `ContainerMutationFuzzTest` (which builds mutants of both
  container shapes) and `FileHandleReleaseTest` all touch container bytes.
- **This is a policy change, not an implementation project** - plain-text write
  is implemented and pinned today. Budget the week for test updates and the
  autosave/undo decisions, not for new I/O code.

## Evidence

- `src/jls/Circuit.java:63-64` - `saveContainer` initialized to `XZ`.
- `src/jls/FileAbstractor.java:43-57` (the enum and its "XZ is the default"
  javadoc), `:65` (the cap, measured on decompressed text), `:120-135` (the
  sniffing reader), `:190-197` (the two-argument overload hardcoding XZ).
- `src/jls/edit/Editor.java:70-72` (save reads the circuit's container),
  `:201-204` (Save As chooses it).
- `src/jls/edit/SimpleEditor.java:203-222` - the checkpoint writer inheriting
  the default.
- `src/jls/edit/CircuitSnapshot.java:11, 21, 32-42, 66` - the deflated undo
  snapshot.
- `test/jls/FileAbstractorTest.java:115, 124, 133, 151, 174` - the five tests
  this task inverts or mirrors.
- BRIEF.md §11 D1, verbatim maintainer decision, and its supporting evidence
  that the mechanism "already exists and is already tested".
