# TASK-0002 - Registry totality lint as a standing build rule

**Status:** proposed | **Cost:** 3 d | **Blocked by:** TASK-0001

## Deliverable

One reusable JUnit base class that any registry-keyed table extends to get its
totality check, plus the `CONTRIBUTING.md` rule that makes extending it
mandatory for new tables.

1. `test/jls/elem/RegistryTotalityTestBase.java`, new, abstract, with:
   - `protected abstract Set<String> covered()` - the tags the table under test
     actually covers;
   - `protected abstract Set<String> exempt()` - the tags deliberately outside
     it;
   - `protected abstract String remedy()` - the sentence the failure message
     tells the author to act on (which file and which line to add);
   - one `@Test final void tableIsTotalOverTheElementRegistry()` computing
     `ElementRegistry.all()` tags minus `exempt()` and asserting set equality
     with `covered()`, failing with both the missing and the stale tags named,
     followed by `remedy()`.
   - one `@Test final void everyExemptionIsARegisteredTag()` so an exemption set
     cannot rot into naming a type that no longer exists.

2. The four existing hand-rolled totality tests are rewritten as subclasses, so
   there is one implementation and not five:
   `PaletteContractTest.paletteIsTotalOverTheElementRegistry`
   (`test/jls/edit/PaletteContractTest.java:48-67`),
   `ElementRegistryTest.everyWritableRegisteredTagIsInTheFrozenTagTable`
   (`test/jls/ElementRegistryTest.java:124`),
   `HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry`
   (`test/jls/hdl/HdlPolicyTest.java:392`), and the vocabulary checks in
   `test/jls/collab/op/ElementVocabularyTest.java:49,121`.
   Behavior must not change: each keeps its exemption set and its message.

3. The tables TASK-0001 pinned (`ElementRenderers`, `ElementDialogs`) become
   subclasses too, not bespoke tests.

4. `CONTRIBUTING.md` gains a bullet in **Making changes**, next to the existing
   "Sealed dispatch" (#95) bullet: *any table keyed on the element registry, an
   `Orientation`, an `EditOp` or a save tag must have a test extending
   `RegistryTotalityTestBase`; adding an element type must be a build failure
   in every such table, not a runtime surprise.* It must name the two measured
   incidents (`970db41`, `b54e6ee`) as the reason.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-001 | Turns the one-off audit into a standing rule: the discipline survives the author who wrote it. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0001 | The base class's shape is determined by the inventory: which tables key on tags vs on `Class<?>`, and what each exemption set needs to record. Writing the base first means guessing the abstraction. |

## Acceptance test

`test/jls/ArchitectureRulesTest.java` (existing) gains
`everyRegistryKeyedTableHasATotalityTest()`: it reads the inventory table from
`docs/registry-keyed-tables.md` (TASK-0001's deliverable) and asserts that each
row's named test class exists on the test classpath and is assignable to
`RegistryTotalityTestBase`. It fails naming the row whose test is missing.

`RegistryTotalityTestBase` itself is proved by a deliberately-broken fixture
subclass in `test/jls/elem/RegistryTotalityTestBaseSelfTest.java`, asserting
that a `covered()` short by one tag produces a failure whose message contains
that tag and the `remedy()` string - the same "every assertion can fail"
discipline `jls.ui.UiHarnessPilotTest.EveryAssertionCanFail` already uses.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - #78's "compiler-enforced authoring contract" is exactly this, one level weaker (test-enforced rather than compile-enforced). This task does not close it |

No issue covers the lint rule itself.

## Notes

- **Test-enforced, not compile-enforced, and say so.** #78 asks for a compiler
  contract. A `sealed`-plus-exhaustive-`switch` table would give that, but the
  tables in question are `Map`s keyed on `Class<?>`
  (`src/jls/edit/ElementRenderers.java:24`,
  `src/jls/edit/ElementDialogs.java:25-29`) and a frozen `Map<String,Class<?>>`
  (`src/jls/elem/SaveTags.java:41`); neither can be made exhaustive without
  reshaping the element hierarchy. A JUnit base is the available mechanism at
  this cost. Do not claim it is the compiler contract.
- **Do not make the base a JUnit 5 `@TestInstance(PER_CLASS)`** unless a
  subclass needs it; the four existing tests are stateless statics and changing
  lifecycle silently changes their ordering guarantees.
- **`final` on the base's `@Test` methods is deliberate**: a subclass that
  overrides the totality check to weaken it is exactly the failure mode.
- **SpotBugs threshold High** runs in `mvn verify` (`CONTRIBUTING.md`); an
  abstract test base with protected abstract methods is clean, but do not add
  a `config/spotbugs-exclude.xml` entry for it.
- The `covered()` set for `ElementRenderers`/`ElementDialogs` is keyed on
  `Class<?>`, not on tag. Map through `ElementType` rather than
  `Class::getSimpleName`, or a future rename (which `SaveTags` explicitly
  permits, `src/jls/elem/SaveTags.java:20-25`) silently breaks the check.

## Evidence

- `test/jls/edit/PaletteContractTest.java:44-67` - the template being
  generalized, including the exemption-set pattern and the remedy sentence.
- `test/jls/ElementRegistryTest.java:124`, `test/jls/hdl/HdlPolicyTest.java:392`,
  `test/jls/collab/op/ElementVocabularyTest.java:49,121` - the four
  hand-rolled implementations.
- `CONTRIBUTING.md` "Making changes" - the section this rule joins; its
  "Sealed dispatch" bullet is the nearest existing rule of the same kind.
- `git show --stat 970db41`, `git show --stat b54e6ee` - the two incidents the
  rule cites.
