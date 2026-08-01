# TASK-0039 - Definition identity: structural digest and version strings

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

A subcircuit definition acquires a canonical identity that is independent of
where and how it is placed, plus the four-field name every interchange format
already assumes, plus a decided policy for what happens when two definitions
claim the same identity.

1. **The digest.** `jls.elem.DefinitionDigest.of(Circuit) -> String`: SHA-256,
   hex, over a canonicalization of the definition's element blocks in which
   (a) blocks are emitted in the canonical stable-id order the writer already
   uses (`src/jls/Circuit.java:1492-1497`), (b) each element's `sid` is
   replaced by its **rank** in that order, so two copies of one drawing digest
   equal, and (c) the geometry attributes `x`, `y`, `width`, `height` and the
   annotation attribute `trpos` are elided. What remains is structure: types,
   configuration, and connectivity by rank.
2. **The four-field name.** `jls.elem.DefinitionId` (record: `vendor`,
   `library`, `name`, `version`). `name` defaults to the nested circuit name -
   today a subcircuit's entire identity is that one token
   (`docs/file-format.md:323`, grammar at §3.1). The other three have no source
   in the tree and are *authored*: defaults are `local`, `local`, and `0.0.0`,
   and each is editable in the subcircuit dialog.
3. **The saved form.** `SubCircuit.save`
   (`src/jls/elem/SubCircuit.java:281-289`) gains two `String` attributes
   before the nested `CIRCUIT` block: ` String defid "vendor:library:name:version"`
   and ` String defdigest "<hex>"`. `SubCircuit.setValue` (`:305-326`) gains
   their arms. Neither is a new item kind, so no `FORMAT` bump is required by
   `docs/file-format.md:427-434` - but see the caveat in Notes.
4. **The collision policy, decided and enforced.** Three cases, each with a
   defined outcome, each tested:
   - same `defid`, same digest -> the same definition; deduplication is legal.
   - same `defid`, **different** digest -> **load refusal** naming the `defid`,
     both digests, and both instance paths. Two different circuits under one
     version string is the failure mode a library cannot survive.
   - different `defid`, same digest -> load succeeds with an informational
     diagnostic; structurally identical circuits legitimately have different
     provenance.
5. **The version-string rule.** `version` is compared as an opaque string for
   equality and ordered for display only; JLS does not implement semantic
   version ranges in this task. State that in `docs/component-naming.md`.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-016 | Identity is the feature's first half; the container (TASK-0040) is the second, and a container that cannot name what it holds is a zip file. |

## Prerequisite tasks

None. The digest is computed over the nested `CIRCUIT` block that
`SubCircuit.save` already writes, and the canonical order it needs already
exists at HEAD.

## Acceptance test

`test/jls/elem/DefinitionIdentityTest.java`, new:

- `twoInstancesDifferingOnlyInPlacementShareADigest()` - import one circuit
  twice, move one instance's contents, assert equal digests. This is the
  property the whole task exists for and it fails against any digest taken over
  raw save bytes.
- `twoIndependentlyDrawnCopiesOfOneCircuitShareADigest()` - the sid-rank
  normalization; two `SubCircuit`s built from the same source but loaded into
  different files have different `sid`s and must still digest equal.
- `changingOneGateChangesTheDigest()` and `changingOneWireChangesTheDigest()` -
  the sensitivity half.
- `renamingAnElementChangesTheDigest()` - names are structure, not annotation;
  pin the decision either way but pin it.
- `sameDefidDifferentDigestIsRefused()` - two `SubCircuit` blocks in one file
  claiming `local:local:adder:1.0.0` with different contents; assert a load
  refusal whose message carries both digests and both instance paths.
- `differentDefidSameDigestLoadsWithADiagnostic()`.
- `theDigestIsStableAcrossASaveLoadCycle()` - digest, save, load, digest again.

`test/jls/FileFormatSpecTest` gains `subcircuitIdentityAttributesAreInTheSpec()`,
asserting the §7 `SubCircuit` row lists `defid` and `defdigest` and states the
no-bump decision and its caveat.

## Related GitHub issues

**No issue.** Subcircuit type identity is unfiled; the written analysis lives in
`docs/standards-adoption/08-ipxact-export.md`, not in the tracker.

| # | title | relationship |
|---:|---|---|
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - #78 gave *element types* self-description; this gives *circuit definitions* the same, and the two identity spaces must not be conflated |
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | informs - hierarchical export deduplicates modules by exactly this identity (TASK-0043) |

## Notes

- **Three of the four VLNV fields must be invented, and that is the finding, not
  an obstacle.** `docs/standards-adoption/08-ipxact-export.md` §"What does not
  check out" item 1 verified it: a JLS subcircuit's whole identity is the nested
  `CIRCUIT` name token - no vendor, no library, no version, no author, no
  revision. Inventing them with stated defaults is the cheapest possible fix and
  is what makes IP-XACT, Yosys module names, `.circ` library references and a
  JLS circuit library all addressable by one key. That document's "do not build
  this yet" verdict is a demand gate against the maintainer's own roadmap and
  does not bind (D10); its *technical* findings do.
- **Handwritten-save elements are not covered by `savedAttributes()`.** The
  canonicalization must elide geometry from `Memory` too, whose `save` is
  handwritten (`src/jls/elem/Memory.java:436-468`) and whose base attributes
  come from `super.save(output)` at `:439`. Drive the elision from
  `Element.BASE_ATTRIBUTES` (`src/jls/elem/Element.java:200-305`) by attribute
  name over the emitted text, not by a per-class allowlist that will rot.
- **`StateMachine` blocks have an ordered sub-record grammar** whose sequence is
  significant (`docs/file-format.md:321`) and whose canonical order is already
  defined (`:411-419`). The canonicalization must not re-sort inside them.
- **The no-bump decision has a caveat and it must be written down.** By
  `docs/file-format.md:427-434` a new attribute name needs no bump; by the
  silent-drop caveat (`:459-472`) a pre-`defid` reader drops both attributes and
  the file loads as N unrelated inlined copies - which is exactly what it means
  today, so the loss is benign *now* and stops being benign the moment
  TASK-0041 makes the definition shared. Record that the bump belongs to
  TASK-0041, not here.
- **Do not fold in bus interfaces, memory maps or parameters.** All three are
  absent from JLS (`08-ipxact-export.md` items 4, 5, 6) and none is needed to
  name a definition. Parameters are TASK-0041's.
- **`SubCircuit.copy` mints a fresh `sid` per element** (`Element`'s `sid`
  attribute copies nothing, `src/jls/elem/Element.java:296-303`), which is why
  the digest must normalize sids rather than hash them.

## Evidence

- `src/jls/elem/SubCircuit.java:281-289` - `save` writes `ELEMENT SubCircuit`,
  the orientation, the base attributes, then the entire nested circuit inline;
  `:305-326` - `setValue`'s String arm; `:331-384` - `copy`, the per-instance
  deep copy.
- `src/jls/Circuit.java:1492-1497` - the canonical sort by stable id that the
  digest's rank normalization rests on; `:1478-1484` - nested blocks omit the
  `FORMAT` line.
- `src/jls/elem/Element.java:200-305` - `BASE_ATTRIBUTES`, including `x`, `y`,
  `width`, `height`, `trpos` and `sid`, in save order.
- `docs/file-format.md:323` (the `SubCircuit` row: "body contains one nested
  `CIRCUIT` block"), `:355-360` (nested blocks recurse), `:427-446` (the bump
  rule), `:459-472` (the silent-drop caveat).
- `docs/standards-adoption/08-ipxact-export.md` - the verified inventory of what
  a JLS subcircuit does and does not carry, including `HdlModel.Port` being
  field-for-field the IP-XACT wire-port payload.
- Decision D7 in `BRIEF.md` §12: "Circuit libraries are DATA, not plugins …
  needs the definition/instance split + a library format with versioning and
  provenance … This is also the biggest single win."
- Do not restate: `docs/file-format.md` owns the grammar;
  `docs/standards-adoption/08-ipxact-export.md` owns the IP-XACT mapping;
  `docs/capability-roadmap/lf-01-parameterization.md` owns program P7.
