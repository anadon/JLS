# FEAT-013 - Per-section internal versioning with must-understand semantics

**Status:** proposed | **Cost:** 4-7 mw | **Owner program:** P11 |
**Spine rank:** S10

## Capability delivered

The saved file stops being accepted or refused as a single unit. It becomes a
sequence of independently versioned sections, each marked required or optional,
so a reader that does not understand an optional section skips it and preserves
it verbatim, and a reader that does not understand a required section refuses
with a diagnostic naming the section and its version. This is what lets a
checkpoint, a per-view geometry block, a bulk memory image, a library
provenance record or a radix manifest ride along in the same file without any of
them making the file unopenable by a reader that predates them - and it is what
makes a bulk binary payload expressible at all once the body is plain text.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | required | per-view geometry is a section per view, and a peer that does not know a view must not corrupt or drop it |
| CAP-02 | required | a kernel image and a multi-hour checkpoint cannot live in a plain-text body under the 64 MiB text cap |
| CAP-03 | required | the same, plus a radix manifest that older readers must refuse rather than misread |
| CAP-04 | required | breadboard geometry is a second view's section |
| CAP-05 | required | package and footprint bindings ride as an optional section rather than as element attributes |
| CAP-16 | beneficial | a migration report and its provenance travel with the migrated file |
| CAP-18 | required | the signal-integrity attribute and constraint blocks ride as OPTIONAL sections, so an older JLS opens an annotated circuit structurally with a clean diagnostic instead of refusing it - and a dropped constraint is a silently unmanufactured requirement, which is the one case the format's silent-ignore valve must not reach. Added 2026-08-03 under D16: the filed issue #319 declares `serves_capstones: [... 313 ...]` and #313 carries 319 in `requires_features`; this table's omission was a transcription defect |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-003 | the section frame is a rewrite of the container; performing it before the reference form changes means rewriting every golden and every fixture twice, and the reference change is the one with the measured payoff |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0033 | Section framing, must-understand flags and the epoch policy | the frame, both reader behaviors, and the written migration policy are one design |
| TASK-0034 | The raw bulk-image section | the first non-structural section, and the one that proves the frame carries binary without disturbing the text budget |
| TASK-0071 | Guest image build, pinning and residence | the residence question ("where does a guest image live") is answered by this frame or by a sidecar, and the answer must be decided once |

## Acceptance criteria

- A file carries per-section version numbers. Changing one section's version
  does not change any other section's bytes, and a test asserts exactly that:
  edit section A, assert section B's stored frame is byte-identical.
- An unknown **optional** section is skipped on load, is preserved verbatim on
  save, and produces an informational diagnostic. An unknown **required**
  section is refused with a diagnostic naming the section and its version. Both
  behaviors are tested from a hand-built file.
- A bulk image lives in a raw section, is referenced by length and content hash,
  and is rejected on load if either disagrees with the structure section's
  declaration. The size arithmetic against the decompressed-text cap is written
  down, not implied.
- A written format-epoch and migration policy exists, with a migration test that
  takes a file at the prior epoch and produces one at the current epoch.
- Sections declare whether they participate in the file's identity hash.
  Checkpoints do not.
- A file containing a section this build does not know round-trips through open
  and save with that section byte-identical.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) internal versioning - decision D3 - has no tracking issue | no issue |
| #163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | informs - a peer running a different build is the same problem as a reader running an older one |

## Design notes

The prior art to mirror is named in decision D3 and should be read before the
frame is designed: PNG's critical-versus-ancillary chunk bit, EBML's element
IDs, ELF section headers, and protobuf field-number evolution. The common shape
is that the *frame* is understandable without understanding the *content*, which
is what makes skip-and-preserve possible.

The synergy that makes this cheap relative to its value: a simulation-state
checkpoint is naturally an optional section, so a reader that knows nothing
about checkpoints still opens the circuit structurally with a clean diagnostic
instead of a hard refusal. The same is true of a second view's geometry, of a
package binding, and of a radix manifest. One mechanism, six consumers.

Today there is exactly one global integer for the whole file
(`docs/file-format.md` §4), which means any change to any record type bumps it
and the file is accepted or refused as a unit. The versioned-section design does
not remove that header; it demotes it to a frame version, and the content
versions live per section.

Three questions must be answered in TASK-0033 rather than discovered: whether
sections are framed inside the existing text grammar or the container becomes
multi-member; whether per-section hashes replace the single whole-file identity
hash; and whether compression, if it returns, is framed per section so that one
edit does not invalidate every following byte.

## Risks

- **The identity hash is a published surface.** `docs/reproducibility.md` and
  the deterministic-save test assert properties of a single whole-file hash.
  Moving to per-section hashes restates them, and that restatement must be
  deliberate and in the same change, not discovered by a failing test.
- **A skip-and-preserve reader is a correctness obligation.** A reader that
  preserves an unknown section but reorders it, or re-indents it, has broken
  byte-identity for the author who does understand it. The round-trip test is
  the guard and must be written first.
- **Scope creep into a successor format.** Several items in the diff-stability
  study are labeled "genuinely requires the successor". This feature is the
  minimum frame that unblocks them; it is not a mandate to redesign the grammar.

## Evidence

- Decision D3, verbatim from the maintainer: ".jlsx would need to also support
  internal versioning to remain flexible" (`BRIEF.md` §11).
- Verified at HEAD: `docs/file-format.md:159-194` documents a single `FORMAT`
  version for the whole file, with `FORMAT 2` emitted only when a file uses
  newer features; `:453-466` records the version history and the accept-or-
  refuse-as-a-unit semantics.
- Verified at HEAD: `src/jls/FileAbstractor.java:65` -
  `MAX_CIRCUIT_TEXT_BYTES = 64 << 20`, measured against decompressed text.
- Measured, `BRIEF.md` §11 D1: at 15.87 bytes per word, a 16 MiB RAM image is
  about 66 MB of escaped text and alone exceeds the 64 MiB cap before any
  circuit content.
- Measured, `05-diff-stability.md` R6: a single 16 MiB high-entropy image as
  escaped initialization text is 62,542,822 bytes, 93.2% of the cap; one word
  changed in that image produces 51,223,498 bytes of `git diff`, and two
  disjoint edits to it always conflict.
- Cost band basis: `05-diff-stability.md` §5 Part C items C1-C5 and C7, scoped
  to the frame and the raw section only.
- Do not restate: `docs/file-format.md` owns the grammar and the version
  history; `docs/reproducibility.md` owns determinism claims; decision D3 in
  `BRIEF.md` §11 is the authority for the requirement itself.
- **Cost reconciliation.** Band 4-7 mw. Tasks named for it: TASK-0033,
  TASK-0034, TASK-0071, totalling 5.5 wk. Band and task sum agree; no
  reconciliation is needed. Shared tasks counted once at the task level:
  TASK-0034, TASK-0071.
