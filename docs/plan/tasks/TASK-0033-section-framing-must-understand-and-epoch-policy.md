# TASK-0033 - Section framing, must-understand flags and the epoch policy

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0005

## Deliverable

Decision D3 made concrete: "`.jlsx` would need to also support internal
versioning to remain flexible." Today there is exactly **one** global `FORMAT`
integer for the whole file (`src/jls/Circuit.java:102`, `FORMAT_VERSION = 2`),
so any change to any record type bumps it and the file is accepted or refused
as a unit. This task replaces that with independently versioned sections
carrying must-understand semantics.

1. **Section framing.** A file becomes a header followed by named sections.
   Each section declares its kind, its own version, and a **required/optional**
   flag. An old reader **skips** an unknown OPTIONAL section with a diagnostic
   and **refuses** an unknown REQUIRED one. The prior art to mirror is named in
   D3 and should be cited rather than reinvented: PNG critical versus ancillary
   chunks, EBML, ELF sections, protobuf field-number evolution.
2. **The initial section set**, deliberately small: `CIRCUIT` (required),
   and the machinery for `GEOMETRY`, `CHECKPOINT` and `IMAGE` to be added later
   as optional without a further epoch. The synergy D3 names is the argument
   for shipping the machinery now: a simulation-state checkpoint is *naturally*
   an optional section, so a reader that knows nothing about checkpoints still
   opens the circuit structurally with a clean diagnostic instead of a hard
   refusal.
3. **Merge participation as a declared per-section property.** Structural
   sections are merged; blobs and checkpoints are hashed, never merged, and
   resolved to one side with a recorded conflict. Declaring it in the format
   rather than in the merge driver is what keeps TASK-0032's table from having
   to special-case each new section kind.
4. **The format-epoch and migration policy, written into
   `docs/file-format.md` §9.** What constitutes an epoch, what a reader must do
   at each boundary, and the rule that version support is only ever added,
   never removed. §9 already carries the bump criteria and the version history
   (0, 1, 2); this extends it rather than replacing it.
5. **An explicit, separately-committed migration** - a `-migrate` batch mode on
   `JLSStart.FLAGS` - because the rewrite of every pre-`sref` file is
   unavoidable and must not be discovered accidentally inside a user's first
   edit.
6. **The two open items §9 already flags, resolved in the same pass.**
   `Memory`'s `sync` attribute (issue #199) is a known silent-mis-load case
   that changes write timing and whose version treatment §9 records as an open
   question; and `Memory`'s `initrle` is the standing example of the same class.
   Section versioning is the mechanism that answers both; answer them.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-013 | The feature is this task plus TASK-0034's bulk-image section. Without must-understand semantics every later optional section forces a whole-file epoch |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0005 | `FORMAT_VERSION` is a single integer and both changes claim the next value. TASK-0005's `sref` item kind is a new item kind, which §9 makes a mandatory bump; so is section framing. Landing them as two epochs weeks apart forces users through two migrations and ships a format that introduces a diff regression and then fixes it. `05-diff-stability.md` §5 Part B states them as one bump for exactly this reason |

## Acceptance test

`test/jls/FormatSectionFramingTest.java`, new (sibling to the existing
`test/jls/FormatHeaderTest.java`, whose structure it should mirror so the two
can be read together):

- `anUnknownOptionalSectionIsSkippedWithADiagnostic()` - loads a file carrying a
  section kind the reader does not implement, marked optional; asserts the load
  **succeeds**, the circuit is complete, and a diagnostic naming the skipped
  kind was recorded.
- `anUnknownRequiredSectionIsRefused()` - the same file with the flag flipped;
  asserts the load fails with a "this file needs a newer JLS" message in the
  #58 taxonomy, not a misparse.
- `aKnownSectionAtAnUnknownVersionIsRefused()` - per-section version
  negotiation, independent of the file's epoch.
- `sectionVersionsAreIndependent()` - a file whose `CIRCUIT` section is at the
  reader's version and whose optional section is newer loads, and the converse
  refuses. This is the whole point of D3 stated as one assertion.
- `headerlessLegacyFilesStillLoadUnchanged()` - version 0 and version 1 and
  version 2 files all load. `FormatHeaderTest` pins this today and it must not
  regress; version support is only ever added.
- `theFormatVersionIsStatedExactlyOnce()` - nested subcircuit `CIRCUIT` blocks
  carry no header and no section frame of their own, per §4.

`test/jls/FormatMigrationTest.java`, new:

- `migrateRewritesEveryTrackedFixtureAndTheResultReloads()` - runs `-migrate`
  over the three tracked `test/fixtures/*.jls` files plus
  `test/fixtures/legacy-4.1/`, asserts each result loads, canonical-saves
  idempotently (migrating twice is a no-op), and preserves every element's
  stable id.
- `migrationIsRefusedWithoutTheFlag()` - asserts an ordinary save of a legacy
  file does **not** silently rewrite it into the new epoch.

`test/jls/FileFormatSpecTest.java`, extended - the existing spec-drift test
gains the section grammar, so `docs/file-format.md` and the reader cannot
diverge.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | informs - a value-representation change is the kind of change that eventually wants a bulk-image section; the framing this task ships is what lets that arrive as optional |

**No issue covers the format successor.** The registry records the whole of
decisions D1, D2 and D3 - FEAT-003, FEAT-012, FEAT-013, FEAT-014 - as untracked
by the issue tracker. #79 (the `FORMAT` header itself) and #199 (`Memory.sync`)
are recorded decisions, **not** open issues; cite them as such and do not
create them.

## Notes

- **This is an epoch and it should be treated as one.** `docs/file-format.md`
  §9's rule is that the version bumps when - and only when - a change could make
  an older reader misparse or silently mis-load. Section framing changes the
  block structure, which §9 lists explicitly as a mandatory bump.
- **The pre-bump failure mode is the intended one.** A version-2 reader
  confronted with a framed file reports a malformed file, and §3 says that is
  the designed behavior. Say so in the policy text rather than treating it as a
  regression.
- **`Circuit.formatVersionNeeded()` (`src/jls/Circuit.java:1580-1587`) is the
  writer-side rule and it must generalize.** Today it answers "emit `FORMAT 2`
  only if some group is vertical" - a whole-file question. Under sections it
  must answer per section, and the writer must emit the *lowest* epoch whose
  features the file uses, so files that avoid new features stay readable by
  older JLS.
- **The `FORMAT` token is currently rejected inside an element body** as an
  unknown item kind, which is what enforces "exactly once". Section keywords
  need the same treatment or a nested subcircuit will be able to declare a
  section.
- **`FileAbstractor.MAX_CIRCUIT_TEXT_BYTES` is a whole-file cap**
  (`src/jls/FileAbstractor.java:65`, `64L << 20`) enforced on both the plain
  path (`:325-328`) and the decompressed stream (`:347-381`). Sections make
  per-member limits meaningful; re-specify the cap as per-section **plus**
  total, or a single hostile section keeps the whole budget.
- **`Memory`'s `sync` and `initrle` are the two live instances of the silent-
  drop caveat**, and they are the honest test of whether this machinery works:
  if section versioning cannot express "this attribute changes simulation
  behavior, refuse rather than drop", it has not solved the problem D3 named.
- **Do not fold TASK-0034's raw bulk-image section into this task.** They share
  the machinery, and shipping them together makes the framing untestable
  independently of a blob format. This task ships the frame; TASK-0034 ships
  the first interesting tenant.

## Evidence

- `src/jls/Circuit.java:102` - `FORMAT_VERSION = 2` at HEAD; `:718-770` - the
  header read and the newer-version refusal; `:1482` - the header write;
  `:1580-1587` - `formatVersionNeeded`.
- `docs/file-format.md:159-197` - §4, the header and version negotiation, with
  the exactly-once rule and the write-the-lowest-sufficient-version rule;
  `:422-496` - §9, the evolution policy, the version history, the silent-drop
  caveat, the `initrle` and `sync` instances, and the tag-stability rule.
- `docs/file-format.md:78-148` - §2 and §3, the lexical structure and grammar
  the section frame must not disturb; §2's "a quoted value MUST begin and end
  on the line of its item" is the normative rule that makes a multi-line blob
  impossible without this work.
- `src/jls/FileAbstractor.java:65,284-330,347-381` - the container detection,
  the size cap and its two enforcement paths.
- `test/jls/FormatHeaderTest.java:17-25` - the existing header contract, in the
  words the new tests must extend; `test/jls/FileFormatSpecTest.java` - the
  spec-drift test.
- `test/fixtures/legacy-4.1/README.md` - the tracked legacy corpus the
  migration test runs over.
- BRIEF §11 D3 - the maintainer's verbatim requirement and the four named prior
  arts; `05-diff-stability.md` §5 Part B/C - the one-bump rule and the section
  properties (C3, C4, C5) this task ships the machinery for.
