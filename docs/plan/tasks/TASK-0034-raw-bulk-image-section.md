# TASK-0034 - The raw bulk-image section

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0033

## Deliverable

A memory or guest image rides in an optional, independently versioned binary
section instead of as escaped hex text in an element attribute, and the size
arithmetic against the text cap is written down rather than implied.

1. **The section.** One new optional (skip-and-preserve) section kind, `IMAGE`,
   in the frame TASK-0033 defines. Payload is opaque bytes. Its frame header
   carries: a section id unique in the file, the payload length in bytes, and a
   SHA-256 of the payload. Optional means a reader that predates `IMAGE` opens
   the circuit structurally, warns, and re-writes the section byte-identically.
2. **The reference.** `Memory` gains one saved String attribute, `imgref`,
   naming an `IMAGE` section id. It is mutually exclusive with the existing
   `init` (`src/jls/elem/Memory.java:418-419`) and `initrle`
   (`:420-425`) attributes; a block declaring two of the three is a load
   diagnostic, not a last-writer-wins. `Memory.save`
   (`src/jls/elem/Memory.java:436-468`) chooses `imgref` when the encoded text
   would exceed a stated threshold, `initrle` when RLE is shorter
   (`:457-461`, unchanged), else `init`.
3. **The load check.** On load, the declared length and hash are compared
   against the section actually present; disagreement is a refusal naming the
   section id, the declared length and the found length. A dangling `imgref`
   is likewise a refusal, not a silent zero-fill.
4. **The writer path.** `Circuit.save` writes through a `PrintWriter`
   (`src/jls/Circuit.java:1466-1512`) and cannot emit raw bytes. The section
   payload is therefore written by `FileAbstractor` at container level, outside
   the text stream; `Circuit.save` emits only the frame reference. The
   split is stated in `docs/file-format.md` in this change.
5. **The arithmetic, written down.** In `docs/file-format.md`: at the measured
   15.87 bytes per escaped word, a 16 MiB image is ~66 MB of text against
   `MAX_CIRCUIT_TEXT_BYTES = 64 << 20` (`src/jls/FileAbstractor.java:65`) - the
   image alone exceeds the cap before any circuit content. State that the cap
   governs the *text* body only, state what governs `IMAGE` payload size, and
   state how `Memory.MAX_INIT_WORDS = 1L << 24`
   (`src/jls/elem/Memory.java:94`) relates to it.
6. **A format bump.** A new item kind is not introduced (`imgref` is a `String`
   attribute), but the section frame is a block-structure change, which
   `docs/file-format.md:437-441` makes a required bump. The bump is TASK-0033's;
   this task does not add a second one.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-013 | The first non-structural section, and the one that proves the frame carries binary without disturbing the text budget. |
| FEAT-036 | A byte-budgeted memory is unexpressible while its contents must survive as escaped text under a 64 MiB text cap. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0033 | This section is a section *in that frame*: it reads the must-understand flag, the per-section version and the skip-and-preserve reader behavior TASK-0033 creates. Without the frame there is nowhere to put it that an older reader can survive. |

## Acceptance test

`test/jls/BulkImageSectionTest.java`, new:

- `anImageSectionRoundTripsByteIdentically()` - build a circuit with a 4 MiB
  pseudo-random image, save, load, save again; assert the second file is
  byte-identical to the first and the image bytes compare equal.
- `aDeclaredLengthMismatchIsRefused()` - hand-build a file whose `IMAGE` frame
  declares a length one byte short of its payload; assert a load refusal whose
  message names the section id, the declared length and the found length.
- `aDeclaredHashMismatchIsRefused()` - flip one payload byte; same shape.
- `aDanglingImgrefIsRefused()` - a `Memory` block naming a section id no
  section carries.
- `initAndImgrefTogetherAreRefused()` - a `Memory` block declaring both.
- `aReaderThatDoesNotKnowImageRewritesItVerbatim()` - drive the TASK-0033
  skip-and-preserve path over an `IMAGE` section and assert the re-written
  bytes are identical.

`test/jls/FileFormatSpecTest` gains `imageSectionIsInTheSpec()`, asserting the
spec's section table lists `IMAGE`, its optional flag, and the size arithmetic
paragraph exists.

`test/jls/elem/MemoryInitEncodingTest` gains
`largeImagesChooseImgrefOverInitrle()`, asserting the writer's threshold at the
boundary in both directions.

## Related GitHub issues

**No issue.** The bulk-image path is part of decisions D1 and D3, and the whole
of the diff-amplification and format work is unfiled.

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - a CPU worked example needs an instruction image, and hex text is how it would otherwise arrive |

Recorded decisions, closed, cite as such and not as open: **#21** (XZ container
and the `initrle` encoding), **#38** (untrusted-file hardening, the origin of
`MAX_INIT_WORDS`), **#47** (the silent-drop caveat), **#129** (plain text as an
interchange container).

## Notes

- **`Memory` is the only producer at HEAD.** `RegisterFile.save`
  (`src/jls/elem/RegisterFile.java:319`) writes no initial-contents attribute,
  so the section ships with exactly one writer. Say so; do not generalize the
  attribute across elements speculatively.
- **`initrle` helps sparse images and not kernel images.** It encodes runs of
  identical words (`src/jls/elem/Memory.java:454-461`); a compressed kernel
  image is high-entropy and does not run-length encode. The measured figure is
  in `05-diff-stability.md` R6: a 16 MiB high-entropy image as escaped text is
  62,542,822 bytes, 93.2% of the cap, and changing one word in it produces
  51,223,498 bytes of `git diff`.
- **`MAX_INIT_WORDS` is a decode bound, not a size policy.** It is applied twice
  (`:424`, `:458`) against `Math.min(capacity, MAX_INIT_WORDS)`. Do not raise it
  in this task; state its relationship to the new payload bound and leave the
  hardening property (#38) intact.
- **The 64 MiB cap is measured against decompressed text**
  (`src/jls/FileAbstractor.java:65`, and the streaming guard at `:347-353`), so
  moving image bytes out of the text body genuinely buys the whole budget back.
  A payload bound that reuses the same constant would give that back away.
- **The escaping path is not the place for binary.** `Memory.save`'s three
  `replace` calls (`:462-465`) are the raw-text path; the section must not route
  through them, and `StringEscapeRoundTripTest` must keep passing untouched.
- **Do not decide the sidecar-versus-section question here.** D1 names both
  ("sidecar file or separate raw section") and TASK-0071 owns the residence
  question for guest images specifically. This task builds the section; if
  TASK-0071 chooses a sidecar for kernels, `IMAGE` still serves memory contents.

## Evidence

- `src/jls/elem/Memory.java:436-468` - the save method, the `initrle`/`init`
  choice and the raw escaping; `:404-428` the loader arms; `:94` the cap.
- `src/jls/FileAbstractor.java:65` - `MAX_CIRCUIT_TEXT_BYTES = 64L << 20`;
  `:43-57` the `Container` enum.
- `src/jls/Circuit.java:1466-1512` - `save` writes text through a
  `PrintWriter` only.
- `docs/file-format.md:308` - the `Memory` row (`init` / `initrle`);
  `:427-446` - the no-bump / bump-required rule; `:459-472` - the silent-drop
  caveat, whose standing example is `initrle` itself.
- Decision D1, verbatim in `BRIEF.md` §11, including the content-kind split
  table ("Memory/kernel images, simulation-state checkpoints -> sidecar file or
  separate raw section - never diffed") and the 15.87 bytes/word measurement.
- Do not restate: `docs/file-format.md` owns the grammar; TASK-0033 owns the
  frame and the epoch policy.
