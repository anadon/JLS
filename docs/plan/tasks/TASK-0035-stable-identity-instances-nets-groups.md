# TASK-0035 - Stable identity for instances, nets and groups

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0007

## Deliverable

The addressing scheme `view:instancePath:sid` is specified and implemented, nets
and fused net groups gain stable identity, and the uniqueness test asserts a
property that survives shared definitions.

1. **The key grammar, specified.** `docs/file-format.md` §8 gains
   `key = view ":" instancePath ":" sid`, where
   `instancePath = [ instanceSid ( "/" instanceSid )* ]` - the **empty path is
   legal** and is the flat case, so today's circuits cost nothing. `view` is a
   token from a closed, registered set with `schematic` as the default. Reserve
   the same grammar in `docs/extension-points.md`.
2. **The type.** `jls.core.ItemKey` (record: `String view`,
   `List<ElementId> instancePath`, `ElementId sid`) with `parse`/`toString` as
   exact inverses, the `[0-9a-z]` replica grammar reused from
   `ElementId.parse` (`src/jls/elem/ElementId.java:245-284`), and a
   `Comparable` canonical order (view, then path element-wise, then sid) so any
   table keyed by it sorts deterministically.
3. **Net identity.** `jls.elem.WireNet` gains an `ElementId`, minted at net
   construction and **persisted**, not derived. Persistence rides on the net's
   lowest-`sid` wire end as a new base attribute ` String nid`, because
   `Wire.save` writes nothing at all (`src/jls/elem/Wire.java:119-126`) and
   nets are rebuilt by traversal at load. Files with no `nid` mint one at load
   under the reserved `legacy` replica in canonical net order, exactly the
   mechanism `ElementId.legacy(long)` (`:224-227`) already provides.
4. **Group identity.** The fused electrical net - a `WireNet` set unioned across
   same-named jump aliases - takes the smallest member net's id as its own.
   `jls.elem.Group` (Binder/Splitter, `src/jls/elem/Group.java:18-19`) needs
   nothing: it is an `Element` and already carries `sid`.
5. **Uniqueness, restated.** Today `sid` is unique per `CIRCUIT` block
   (`docs/file-format.md:394-396`) and design-unique *only because*
   `SubCircuit.save` inlines a full copy of the definition per instance
   (`src/jls/elem/SubCircuit.java:281-289`). The invariant this task ships is
   **`(instancePath, sid)` is unique in a design**, which is the one that
   survives FEAT-017's shared definitions. The load-time duplicate check moves
   to that key.
6. **`Ops.resolve` becomes path-qualified.** `src/jls/collab/op/Ops.java:33-41`
   scans `circuit.getElements()` for a bare `sid`; it takes an `ItemKey` and
   descends `SubCircuit.getSubCircuit()` for a non-empty path.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-014 | Every other part of the addressing scheme - per-view geometry, the op view discriminator, cross-probe maps, package binding - is a table keyed by this key. Without it each of them invents its own. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0007 | A net's persisted id must name exactly one partition. At HEAD there are two: `Circuit`'s `WireNet` partition, and `HdlExporter`'s own union-find (`src/jls/hdl/HdlExporter.java:1161-1200`) which additionally fuses same-named jump nets into a private `Group` (`:1146-1158`). Minting ids against one of two partitioners that can disagree ships an ambiguous identity into the file format. TASK-0007 is what makes there be one partition to key off. |

## Acceptance test

`test/jls/core/ItemKeyTest.java`, new:

- `parseIsTheInverseOfToString()` over the flat case, a one-deep path, a
  three-deep path and every legal view token.
- `theEmptyInstancePathIsLegalAndPrints()` - `schematic::r1:7` round-trips and
  compares equal to the flat address of the same element.
- `canonicalOrderIsViewThenPathThenSid()` with a shuffled input list.

`test/jls/NetIdentityTest.java`, new:

- `netIdsSurviveSaveAndLoad()` - build a multi-branch net, save, load, assert
  every net's id is unchanged.
- `aLegacyFileMintsTheSameNetIdsOnEveryLoad()` - load a checked-in pre-`nid`
  fixture twice in one JVM and assert identical ids, the same determinism
  property `ElementId.legacy` already carries.
- `jumpAliasedNetsShareOneGroupId()` - two nets bridged by a same-named
  JumpStart/JumpEnd pair; assert one group id, equal to the smaller net id.
- `insertingAnElementDoesNotRenumberAnyNetId()` - the diff-stability property;
  must fail against any derive-from-content scheme.

`test/jls/StableElementIdTest` gains
`instancePathAndSidAreUniqueAcrossTwoInstancesOfOneSubcircuit()`, which passes
today only by accident (the two instances are two copies) and is the assertion
that must keep passing after FEAT-017.

## Related GitHub issues

**No issue.** Net and group identity, and the whole `view:instancePath:sid`
addressing scheme, are unfiled - decisions D2 and D3's consequences.

| # | title | relationship |
|---:|---|---|
| 163 | Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue) | informs - replication addresses artifacts by identity, and a net with no identity cannot be an op's subject |
| 167 | Operation layer: reify editor mutations as invertible, serializable commands behind one entry point | depends on - `Ops.resolve` is the op layer's addressing primitive and this task changes its key |

Recorded decisions, closed, cite as such and not as open: **#165** (stable
element ids), **#166** (canonical order), **#98** (deterministic file order).

## Notes

- **Do not add a second `(x,y)` to `Element`.** `Element` has exactly one `x`
  and one `y` (`src/jls/elem/Element.java:28,30`), one `setXY` (`:72-76`), one
  `savePosition`/`restorePosition` pair (`:452-465`). A second pair does not
  extend to a third view. Geometry is a side table keyed by this key, and it is
  TASK-0036's deliverable, not this one's.
- **Minting beats deriving, and the reason is edits.** A net id derived from its
  members (say, the minimum member `sid`) is a pure function of content and
  therefore diff-stable, but it *changes* when that member is deleted - so an
  op recorded against the net, or a probe bound to it, silently retargets. Mint
  and persist.
- **The load-time counter guard applies to net ids too.**
  `ElementId.parse` advances the process counter past any id already in use for
  this replica (`src/jls/elem/ElementId.java:268-283`); net ids parsed from a
  file must go through the same path or a second run of one install can re-mint
  an id the file already declares.
- **`nid` is a "no bump needed" addition** by `docs/file-format.md:427-434`
  (a new attribute name on an existing type), but it is exactly the class of
  change the silent-drop caveat (`:459-472`) warns about: a pre-`nid` reader
  drops it and re-mints legacy ids. That is acceptable *only* because the legacy
  minting is deterministic. State it in the spec.
- **Every golden regenerates.** `nid` is a new saved attribute on wire-end
  blocks, so `DeterministicSaveTest`, `AllElementsRoundTripTest`,
  `CircuitRoundTripTest`, the worked example at `docs/file-format.md:497+` and
  the HDL export goldens under `test/resources/hdl/` (32 `.v` and 32 `.vhdl`
  files at HEAD) all move. Coordinate with
  TASK-0005 and TASK-0006 so the format epoch regenerates once, not three
  times.
- **`HdlExporter`'s `Group` is not `jls.elem.Group`.** They are unrelated types
  with the same simple name (`src/jls/hdl/HdlExporter.java:1146` versus
  `src/jls/elem/Group.java:18`). Name the new concepts so the collision does not
  propagate.

## Evidence

- `src/jls/elem/ElementId.java:36,196-227,245-284` - the existing id type, the
  two minting paths, the parse grammar and the counter guard.
- `src/jls/elem/Wire.java:119-126` - `save` is literally `// do nothing`, which
  is why net identity cannot ride on a `Wire` record.
- `src/jls/hdl/HdlExporter.java:1146-1158` (the fused-net `Group`),
  `:1161-1200` (the private union-find), and the jump-alias union loop that
  builds it - the second partition.
- `src/jls/elem/SubCircuit.java:281-289` - `save` inlines the whole nested
  circuit, which is *why* a flat `sid` is design-unique today.
- `docs/file-format.md:378-405` - stable ids, per-block uniqueness and the
  refusal on a duplicate `sid`; `:407-420` canonical order.
- `10-capstone-plan.md` §7.1 - the recommended key grammar, the KiCad
  `SCH_SHEET_PATH` precedent, and the measured cost of not reserving it
  (+6-10 wk and a format break).
- Do not restate: `docs/file-format.md` §8 owns ids and references;
  `ARCHITECTURE.md` owns the package layering this key must respect.
