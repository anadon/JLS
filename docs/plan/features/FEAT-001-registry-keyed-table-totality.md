# FEAT-001 - Registry-keyed table totality discipline

**Status:** proposed | **Cost:** 1-2 mw | **Owner program:** P3 |
**Spine rank:** S1

## Capability delivered

Every table in the tree whose key set is supposed to be the element registry is
*proven* to be that key set by a test, and adding a thirty-sixth element type
fails the build until every such table has a row for it. The class of defect
where an element exists, draws, simulates and saves but is invisible to one
downstream table - the save-tag table, the HDL export policy, an orientation
switch, an edit-op kind - stops being discoverable only by a user hitting it.
This is the cheapest feature in the plan and it is the precondition for every
capstone that adds element types, because each of those capstones otherwise pays
the same audit again.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | Two of the three defects already fixed on this branch were missing rows in registry-keyed tables; the ratchet is what stops the third |
| CAP-04 | required | The package and pinout binding is a new table keyed on element type; it must be born total |
| CAP-05 | required | The PCB emitters need every element type to have a declared physical disposition, including "cannot be placed" |
| CAP-13 | required | KiCad parity is exactly the claim that the emitter handles the whole element vocabulary |
| CAP-14 | required | Analog bridge elements enter the registry and every existing table must gain their rows |
| CAP-15 | required | HDL parity is a totality claim over the export policy - the shape of `b54e6ee` |
| CAP-16 | required | The `.circ` construct map is a table over the target vocabulary; a silent hole in it is a silent migration loss |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | None. This feature reads `ElementRegistry.all()`, which exists at HEAD, and adds tests and a build rule. It is the entry point of the spine |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0001 | Audit and pin every registry-keyed table | Nobody currently knows how many such tables there are; the inventory is the deliverable that makes the rule enforceable |
| TASK-0002 | Registry totality lint as a standing build rule | An inventory that is not a build rule decays within one release |

## Acceptance criteria

1. A committed inventory names every table keyed on the element registry, on an
   orientation enum, on an edit-op kind or on a save tag, with its file and its
   key type.
2. For each inventoried table a test asserts key-set **equality** with
   `ElementRegistry.all()` - not containment - and asserts that no arm falls
   through to a default that silently drops.
3. One reusable JUnit base exists that a new registry-keyed table extends in a
   few lines, and `CONTRIBUTING.md` requires it for new tables.
4. Adding a synthetic thirty-sixth element type to a test registry fails at
   least one totality test per inventoried table. This is asserted by a test,
   not by a comment.
5. The rule applies to the tables added by later features, not only to the ones
   present at authoring time - the base class, not the inventory, is the
   contract.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the registry half of #78 already shipped; this feature builds on it and does not close it |
| - | the totality lint itself | **no issue** |

## Design notes

`ElementRegistry.ALL` is a `List.of(...)` of 35 `ElementType` entries at
`src/jls/elem/ElementRegistry.java:38-77`, exposed as `all()` at `:132` and
indexed by tag and alias into `BY_TAG` at `:79-80`. That list is the key set.
Two totality tests already exist and are the pattern to generalize:
`test/jls/ElementRegistryTest.java` and `test/jls/elem/SaveTagsTest.java`.

The empirical case for the rule is that both defects this study landed were the
same defect in different tables. `970db41` registered `RegisterFile` and
`FieldExtend` in the frozen save-tag table and added the missing
registry-to-`SaveTags` totality test; `b54e6ee` made the HDL export policy total
over the registry. Neither was found by a user report and neither was found by
review - both were found by asking one table at a time whether it covered the
key set. TASK-0001 is that question asked exhaustively and written down.

Two hygiene items are folded into TASK-0001 rather than given their own ids:
rejecting incompatible batch flag combinations at parse time, and the
enumeration discipline for edit-op kinds. Both are the same shape of hole.

Decision D6 applies: this is defect work and lands immediately. It is not
sequenced behind the core extraction.

## Risks

- **The inventory can be incomplete and still look green.** A table nobody
  listed is a table nobody tests. Mitigation is criterion 3 - the reusable base
  plus the `CONTRIBUTING.md` rule catches the tables written after the audit,
  which is where the next defect will actually be.
- **Equality is stricter than the codebase currently is.** Some tables may
  deliberately exclude `Wire` or `WireEnd`. The correct response is an explicit,
  named exclusion set under its own test, not a containment assertion - a
  containment assertion is what let `970db41` happen.
- **1-2 mw is the audit, not the fixes it finds.** If the audit finds four more
  missing rows, fixing them is inside the band; if it finds forty, it is not.

## Evidence

- The key set: `src/jls/elem/ElementRegistry.java:38-77` (35 `ElementType`
  entries in `ALL`), `:132` (`all()`), `:79-80` (`BY_TAG`).
- Existing totality tests to generalize: `test/jls/ElementRegistryTest.java`,
  `test/jls/elem/SaveTagsTest.java`, `src/jls/elem/SaveTags.java`.
- The two defects of this shape already fixed: `970db41`, `b54e6ee`
  (`BRIEF.md` §12 D6; both present in `git log` at `b54e6ee`).
- Element count corrected from 33 to 35 types and 25 to 27 `react`
  implementations: `BRIEF.md` §13.
- Cost and spine placement: `10-capstone-plan.md` §2.1 row S1.
- Sequencing: decision D6, `BRIEF.md` §12.
