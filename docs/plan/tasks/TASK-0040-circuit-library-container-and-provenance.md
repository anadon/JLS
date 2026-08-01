# TASK-0040 - The circuit-library container and provenance

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0039, TASK-0033

## Deliverable

A versioned container that carries N circuit definitions plus the provenance
that makes them redistributable, readable and writable headlessly, and resolvable
from a circuit that references one.

1. **The container.** A `.jlslib` file in TASK-0033's section frame:
   - a **required** `LIBRARY` section: the library's own four-field name
     (`DefinitionId` from TASK-0039, with `name` naming the library), the
     container version, and an index of the definitions it holds - each by
     `defid` and `defdigest`.
   - one `DEFINITION` section per definition, each carrying a `CIRCUIT` block in
     the existing grammar minus the `FORMAT` line, exactly as a nested
     subcircuit block is written today (`docs/file-format.md:355-360`).
   - a **required** `PROVENANCE` section: author, SPDX license identifier,
     source URL, the JLS version that produced it, and the build epoch.
2. **The reader and writer.** `jls.lib.Library` with `read(Path)` /
   `write(Path, Library)`, in a package that is born under
   `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` with no baseline entry - the
   way `jls.core`, `jls.hdl` and `jls.module` were. Reading validates every
   definition's `defdigest` against its content and refuses on mismatch, naming
   the definition.
3. **Type-token confinement.** Every `ELEMENT` token in a library goes through
   `ElementVocabulary` (`src/jls/collab/op/ElementVocabulary.java:38-45`) before
   it reaches the reflective instantiation in `Circuit.load`. A library is
   redistributed material, not a file the user chose to open, so it gets the
   network-grade gate that the local file loader does not (that distinction is
   `ElementVocabulary`'s own stated rationale, `:5-16`).
4. **Resolution.** A `SubCircuit` carrying a `defid` but no inlined body
   resolves against, in order: definitions already in the open file, then
   libraries named on the command line, then a per-install library directory
   under the XDG config base - the same convention `ElementId`'s replica file
   already uses (`src/jls/elem/ElementId.java:71-79`). An unresolved `defid` is
   a load refusal naming the `defid` and the search path, never a placeholder.
5. **The CLI.** `-lib list <file>` prints the index; `-lib extract <file>
   <defid> <out.jls>` writes one definition as a standalone circuit. Both are
   batch-mode paths with no GUI dependency; the flag table gains its rows and
   `CliFlagTableTest` covers them.
6. **Determinism.** Two runs of `write` over the same library produce
   byte-identical files, with the build epoch taken from `SOURCE_DATE_EPOCH`
   when set - the discipline `docs/reproducibility.md` already owns.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-016 | The container is the feature's second half; TASK-0039 gives definitions names, this gives them a distribution unit and a license notice. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0039 | The index is keyed by `defid` and validated by `defdigest`. A container that cannot say which definition it holds, or detect that one changed, is a zip file with extra steps. |
| TASK-0033 | The container is section-framed, and gets its optional/required must-understand semantics and its per-section versions from that frame. A library that a newer JLS extends must stay openable by an older one for the definitions it does understand. |

## Acceptance test

`test/jls/lib/LibraryContainerTest.java`, new:

- `aLibraryRoundTripsByteIdentically()` - write, read, write; assert identical
  bytes, with `ElementId.pinForTesting` (`src/jls/elem/ElementId.java:170-181`)
  pinning the replica so the result is reproducible.
- `aDefinitionWhoseDigestDisagreesIsRefused()` - flip one gate in a `DEFINITION`
  section without updating the index; assert refusal naming the definition, the
  indexed digest and the computed digest.
- `aMissingProvenanceSectionIsRefused()` - `PROVENANCE` is required, so its
  absence is a hard error, not a warning.
- `anUnknownOptionalSectionIsPreservedVerbatim()` - the TASK-0033
  skip-and-preserve behavior exercised through the library reader.
- `anElementTokenOutsideTheVocabularyIsRefused()` - a hand-built library naming
  a type absent from `ElementVocabulary.ALLOWED`; assert rejection before any
  reflective class lookup.
- `resolutionOrderIsFilesThenCommandLineThenInstallDirectory()` - three
  libraries each defining `local:local:adder:1.0.0` with distinguishable
  contents; assert which one wins, per position.
- `anUnresolvedDefidIsRefusedNamingTheSearchPath()`.

`test/jls/CliFlagTableTest` gains the `-lib` rows; `test/jls/CliSmokeTest` gains
`libListPrintsTheIndex()`.

## Related GitHub issues

**No issue.** The circuit-library format is unfiled; D7 names it as "the biggest
single win" and no tracker entry exists for it.

| # | title | relationship |
|---:|---|---|
| 212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` atop the #78 registry | overlaps - and the contrast is the point: #212 distributes *code* behind a demand gate, this distributes *data* with no ABI and no trust boundary (D7). Neither substitutes for the other, and this one does not wait on #212's gate |
| 224 | Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue) | informs - `jls.lib` is a headless leaf package under #224's layering |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the element registry is what `ElementVocabulary` should eventually delegate to, and the library reader is a second consumer of that decision |

## Notes

- **Data, not plugins. This is a ratified decision, not a preference.** D7 is
  explicit: "Circuit libraries are DATA, not plugins … No ABI, no trust
  boundary." Nothing in this task may load a class, register a service, or
  execute anything from a library file. If a reviewer sees a `ServiceLoader`,
  a `URLClassLoader` or a `ProcessBuilder` in this change, it is wrong.
- **The 64 MiB text cap applies per member, and the arithmetic must be
  restated.** `MAX_CIRCUIT_TEXT_BYTES = 64L << 20`
  (`src/jls/FileAbstractor.java:65`) is measured against decompressed text; a
  library of N definitions must state whether the cap is per definition or per
  file, and the streaming guard (`:347-353`) must be exercised by a test with a
  hostile compressed library.
- **A license notice is not optional metadata.** JLS is GPL-3.0-or-later; a
  redistributed library of circuits authored elsewhere carries its own terms.
  `PROVENANCE` being a *required* section is what makes an unlicensed library a
  refusal instead of a silent redistribution. `TASK-0055` faces the same
  obligation for part data and should use the same section.
- **`ElementId` replica pinning is what makes a published library
  reproducible.** Without `jls.replicaId` set, two builds of one library mint
  different `sid`s (`src/jls/elem/ElementId.java:54-57`, `:124-133`), so its
  digest is stable but its bytes are not. The build recipe must pin it, and the
  test must too.
- **Do not invent a compression story.** D1 makes plain text the default; if a
  library is large the user compresses it, exactly as D1 says users do for
  circuits. If compression returns it is framed per section, which is
  TASK-0033's open question, not this one's.
- **The nested-block grammar is already specified; reuse it.** A `DEFINITION`
  section's payload is the same `CIRCUIT` block a `SubCircuit` body carries
  (`docs/file-format.md:323`, `:355-360`), which means the existing loader
  (`src/jls/Circuit.java:1006-1024`) reads it unchanged.

## Evidence

- `docs/file-format.md:323` and `:355-360` - the nested `CIRCUIT` block grammar
  a `DEFINITION` section reuses; `:159-194` the single global `FORMAT` integer
  the section frame replaces.
- `src/jls/collab/op/ElementVocabulary.java:5-16` (the file-versus-network
  distinction), `:38-45` (the 34-token closed list).
- `src/jls/Circuit.java:1006-1024` - `loadElementItems`' nested-`CIRCUIT` arm,
  which constructs a fresh `Circuit` per block.
- `src/jls/FileAbstractor.java:65`, `:347-353` - the text cap and the
  decompression guard.
- `src/jls/elem/ElementId.java:71-79` (the XDG config convention), `:170-181`
  (test pinning).
- Decision D7 in `BRIEF.md` §12, verbatim on libraries as data and on `jls.api`
  as the extensibility story; decision D10 in §12 on why the #212 demand gate
  does not apply to the maintainer's own roadmap.
- Do not restate: `docs/reproducibility.md` owns determinism claims;
  `docs/extension-points.md` owns the seam catalog; `docs/grand-architecture.md`
  owns the layering.
