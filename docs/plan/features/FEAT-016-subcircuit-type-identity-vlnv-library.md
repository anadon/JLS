# FEAT-016 - Subcircuit type identity, VLNV and the circuit-library format

**Status:** proposed | **Cost:** 3-5 mw | **Owner program:** P7 |
**Spine rank:** -

## Capability delivered

A subcircuit definition has a name that means something outside the file it
happens to live in: a canonical structural digest that identifies the
definition independent of where its instances were dragged, plus vendor,
library, name and version strings, plus a stated policy for what happens when
two definitions claim the same identity and differ. On top of that identity, a
library of circuits becomes a distributable artifact with provenance, rather
than a folder of files that were copied at some point and have drifted since.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | beneficial | Peers must agree on which definition they are editing |
| CAP-06 | required | A lab handout is a distributed library of circuits with a version, not a zip of copies |
| CAP-08 | required | One module reused N times must import as one definition with N instances |
| CAP-15 | beneficial | Deduplicated hierarchy - one module reused N times rather than N uniquified copies |
| CAP-16 | required | `.circ` files carry named, reused subcircuits, and a migrated library needs identity to stay a library |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-013 | The library container is a versioned section-framed artifact. Without per-section versioning with must-understand semantics, a library written by a newer JLS is either refused wholesale or silently truncated by an older one |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0039 | Definition identity: structural digest and version strings | The identity itself; everything else in this feature is keyed on it |
| TASK-0040 | The circuit-library container and provenance | The distributable artifact and the provenance record that makes a shipped library auditable |

## Acceptance criteria

1. Two files containing the same definition placed at different coordinates,
   saved by different users, produce the same digest. Two definitions differing
   in any structural respect produce different digests.
2. The digest is stable across a save/load round trip and across a rename of
   the enclosing file.
3. Vendor, library, name and version are carried on the definition and survive
   round trip; a definition with none is representable and named as such rather
   than being given invented values.
4. A collision - same identity, different digest - is reported by name at load
   time with both provenances, and is not resolved silently in favor of either.
5. A library file carries N definitions plus their provenance, opens, and each
   definition in it is instantiable.
6. Nothing about identity requires a `Graphics`: the digest is computable
   headless, asserted by a headless test.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | Subcircuit type identity, VLNV, and the circuit-library container | **no issue** |
| 79 | Save-format stewardship: a `FORMAT` version header, a normative spec, and type tags decoupled from Java class names | informs, **closed** - it established the format-version header and the decoupling of type tags from class names that a definition identity is written against |
| 166 | Deterministic canonical serialization: byte-identical saves for identical circuits | informs, **closed** - canonical bytes are the precondition for a digest that means anything |

## Design notes

Nothing at HEAD has reuse identity to reference. A `SubCircuit` instance holds
its own `Circuit` and `SubCircuit.save` writes that nested circuit out per
instance, so N instances of one definition are N serialized copies that can and
do drift. That is the fact this feature exists to change, and it is also why
this feature is small while FEAT-017 - which actually collapses the copies - is
large: identity can be established over the current representation, and the
representation change is a separate, much larger purchase.

The standards-adoption study of IP-XACT is the prior art here and its technical
findings are usable even though its verdict is not adopted: there is no VLNV
anywhere in the tree, three of the four VLNV fields would have to be invented,
and `SubCircuit.save` inlining the definition per instance is exactly the
absence of reuse identity. Take the findings, not the demand gate; per D10 this
feature is a path and a cost.

Criterion 4 is where the real design decision sits and it is deliberately not
made here: whether a collision is a hard load failure or a diagnostic with a
disambiguating rename is a maintainer decision that TASK-0039 records.

## Risks

- **A structural digest is only as canonical as the serialization under it.**
  If any field enters the digest that is not canonically ordered, the digest is
  unstable and every downstream claim is unstable. The digest input must be a
  named projection of the model, not the saved bytes.
- **VLNV invites version semantics nobody has agreed.** Whether a version is
  ordered, comparable, or opaque changes what a library can promise. Keep it
  opaque until a consumer needs otherwise.
- **The library format is a second container.** It must reuse the file format's
  section framing rather than becoming a parallel format with its own drift.

## Evidence

- No reuse identity at HEAD: `src/jls/elem/SubCircuit.java:102-107`
  (`getSubCircuit()` returns the instance's own `Circuit`), `:282-289`
  (`save` writes the nested circuit per instance).
- Format version and the section-framing prerequisite:
  `src/jls/Circuit.java:102` (`FORMAT_VERSION`), `docs/file-format.md`.
- Prior art and its verified findings: `docs/standards-adoption/08-ipxact-export.md`.
- Owner: P7 in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 3-5 mw; TASK-0039 and TASK-0040 total 4 wk.
  These two tasks are the whole of this feature and the arithmetic agrees
  within the band.
