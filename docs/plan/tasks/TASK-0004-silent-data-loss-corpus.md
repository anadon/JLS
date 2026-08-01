# TASK-0004 - Silent-data-loss regression corpus

**Status:** proposed | **Cost:** 2 d | **Blocked by:** TASK-0003

## Deliverable

A tracked corpus of fixture files that carry attributes no element declares,
each asserting a diagnostic rather than a quiet load, so the fail-loud behavior
TASK-0003 adds cannot silently regress.

1. **`test/resources/silent-loss/`**, new, holding plain-text `.jls` fixtures
   (the container `FileAbstractor.Container.PLAIN_TEXT` writes,
   `src/jls/FileAbstractor.java:56`) - one per case, readable in a diff:
   - `unknown-int.jls` - an `AndGate` block with `int notAnAttribute 7`;
   - `unknown-long.jls`, `unknown-bigint.jls`, `unknown-string.jls`,
     `unknown-ref.jls` - one per item kind, covering all five loader arms at
     `src/jls/Circuit.java:1067, 1078, 1089, 1105, 1116`;
   - `initrle-on-a-gate.jls` - `Memory`'s real `initrle` name attached to an
     element that does not declare it, the historical instance the spec names
     (`docs/file-format.md:470-473`);
   - `sync-on-a-register.jls` - the same for `Memory`'s `sync` (#199), the
     second recorded instance (`docs/file-format.md:481-487`);
   - `misspelled-base-attribute.jls` - `trpos` written as `trpo`, the realistic
     hand-edit case where the drop changes what the user sees;
   - `wireend-handwritten-names.jls` - `ref attach`, `ref wire`, `int tristate`
     on a `WireEnd`, the **negative** control that must produce zero
     diagnostics because `src/jls/elem/WireEnd.java:625-640` consumes them
     outside `savedAttributes()`.
2. **A manifest**, `test/resources/silent-loss/expected.txt`, one line per
   fixture: file name, expected diagnostic count, and the substrings the
   diagnostic must contain (element tag, attribute name, line number). A
   fixture with no manifest line fails the run, so adding a file without an
   expectation is an error.
3. **`docs/file-format.md` §5's silent-ignore paragraph gains a pointer** to
   this directory as the reference corpus for the reporting behavior.

Every fixture must still **load successfully**: the spec's forward-compatibility
valve (`docs/file-format.md:220-228`) is normative and unchanged.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-002 | Converts the fail-loud change from a code edit into a pinned contract with a growing corpus, which is what stops the behavior rotting back. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0003 | The corpus asserts on a diagnostic that does not exist until `setValue` reports and the loader records it. Before TASK-0003 every fixture here loads silently and there is nothing to assert. |

## Acceptance test

`test/jls/SilentDataLossCorpusTest.java`, new:

- `everyFixtureLoadsAndReportsExactlyItsManifestDiagnostics()` - a
  `@ParameterizedTest` sourced from `test/resources/silent-loss/expected.txt`.
  For each row it loads the fixture, asserts `Circuit.load` plus `finishLoad`
  returned true, asserts the warning count equals the manifest count, and
  asserts each declared substring appears in the diagnostic detail.
- `everyFixtureFileHasAManifestRow()` - lists the directory and fails naming any
  `.jls` file absent from the manifest.
- `theNegativeControlProducesNoDiagnostic()` - asserts
  `wireend-handwritten-names.jls` loads with zero warnings, so an
  over-eager implementation that reports every name not in `savedAttributes()`
  fails here.

## Related GitHub issues

**No issue.** The silent-data-loss path has no tracker entry (registry TABLE 4,
"Plan items with NO issue"); this task, TASK-0003 and FEAT-002 are the whole of
it.

Cited as context, not as owners: **#199** (`Memory.sync`) is the open-question
instance the spec records at `docs/file-format.md:481-487`; it is not open on
the tracker and must not be cited as such.

## Notes

- **Fixtures must be plain text, not XZ.** `FileAbstractorTest` already proves
  both containers load (`test/jls/FileAbstractorTest.java:151`); a compressed
  fixture is unreviewable in a diff and defeats the point of the corpus under
  decision D1.
- **Line numbers are load-bearing** in the assertions. `Circuit.lineNumber` is a
  `private static int` (`src/jls/Circuit.java:89`) incremented per item at each
  of the five arms. It is static, so a test that loads two circuits in one JVM
  without resetting will see carried-over numbers - assert on the substring the
  loader emits, and if the numbers prove unstable, fix the static rather than
  weakening the assertion.
- **Every fixture needs a `FORMAT` line or none, deliberately.** Headerless
  files are version 0 and still legal (`docs/file-format.md:452-455`); pick one
  convention per fixture and record it in the manifest so a future `FORMAT` bump
  does not silently retarget the corpus.
- **`CircuitTextBuilder`** (`test/jls/CircuitTextBuilder.java`) already builds
  circuit text programmatically and is what `VcdExportGoldenTest` and
  `DeterministicSaveTest` use. Prefer checked-in files here anyway: the point is
  that a human can read the malformed input, and a builder that constructs the
  bad attribute is a second place the expectation can drift.
- **Do not add a fixture that fails the load.** An unknown *item kind* or an
  unknown *tag* is a hard error by spec (`docs/file-format.md:139-142`,
  `:225-228`) and already covered by `CircuitLoadErrorTest.unknownElementTypeIsRejected`
  and `ContainerMutationFuzzTest`; duplicating them here blurs what this corpus
  is for.

## Evidence

- `src/jls/Circuit.java:1067, 1078, 1089, 1105, 1116` - the five arms one
  fixture each must exercise.
- `src/jls/Circuit.java:89` - `lineNumber` is `private static`.
- `src/jls/elem/WireEnd.java:625-640` - the hand-written names the negative
  control protects.
- `docs/file-format.md:220-228` (the silent-ignore rule), `:466-487` (the
  `initrle` and `sync` instances the corpus reproduces).
- `test/jls/FileAbstractorTest.java:133, 151` - plain-text write and
  both-containers-load already pinned.
- `test/jls/CircuitTextBuilder.java` - the existing programmatic builder, and
  why it is not the mechanism here.
