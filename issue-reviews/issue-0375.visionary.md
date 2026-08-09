# Issue #375: TASK-0002: a thirty-sixth element type fails the build in every registry-keyed table, because one JUnit base enforces totality and CONTRIBUTING makes extending it mandatory
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the apparatus and the claim is: *adding an element type should not be able to
half-land*. That goal is exactly right, it is the goal #78 has been chasing since July,
and it is the goal `PaletteContractTest`, `CapabilityInterfaceTest`, `PinFaceContractTest`
and `SealedHierarchyTest` already partially deliver. Nothing below disputes the goal.

What I dispute is the route. #375 proposes to keep N hand-maintained tables that are
supposed to be the same key set, and add a mechanism that proves they are. The
project's whole recorded arc points the other way: stop having N tables.

## The trajectory this sits in

`CONTRIBUTING.md:39-49` states the sealed-dispatch rule — dispatch on an element kind is
an exhaustive `switch` with no `default`, *so the compiler flags every site when the
hierarchy grows*. #78 H2 is the same move for `init`/`initSim`/`react`: replace runtime
stubs with compile-time obligations. #238 replaced boolean capability predicates with
interfaces. #246 replaced ~520 hand-rolled palette lines with one table plus one loop.
Every one of these deletes a place where a human has to remember something.

#375 adds places where a human has to remember something. Concretely it ships:
`docs/registry-keyed-tables.md` (hand-maintained), one `covered()` override per table
(hand-maintained), one `exempt()` set per table (hand-maintained), one `remedy()` string
per table (hand-maintained), and one `CONTRIBUTING.md` bullet (remembered by review).
That is *more* hand-maintained parallel state than exists today, purchased to prove that
the pre-existing hand-maintained parallel state is consistent. The fix ratio is inverted.

## Four things the issue's own evidence says, and the issue does not hear

**1. `covered()` is the defect, wearing a green hat.** §10 H3 says outright: "a `covered()`
that re-lists the table by hand rather than reading it is the likely cause and is itself
the defect." The API in §7.4 then makes `protected abstract Set<String> covered()` the
primary member an author writes. The base should never accept an author-supplied covered
set. It should accept *the table* — `protected abstract Set<Class<?>> keys()` returning
`BY_TYPE.keySet()` — and derive coverage. `ElementRegistryTest.everyLoadableElementClassIsRegistered`
(`test/jls/ElementRegistryTest.java:46-57`) already models the strong form: it derives the
expected set by scanning `src/jls/elem`, not from a list someone typed.

**2. The key set is not the registry, and the tree already proves it.** On master today:

```
registry(35) \ renderers(33) = FieldExtend RegisterFile TestGen
registry(35) \ dialogs(31)   = FieldExtend RegisterFile TestGen WireEnd
renderers    \ registry      = Wire            <-- the direction the issue never computes
```

`jls.elem.Wire` is `public final class Wire extends Element` with no `(Circuit)`
constructor, so it is deliberately not a registry row, and it must still be drawn
(`BuiltinElementRenderers` line ~100). Under §7.10's equality predicate `C(T) = K \ X(T)`,
`Wire` is *stale* and must be exempted — an exemption whose only honest reason is "the key
set of this table is not K." The renderer table's real domain is "drawable `Element`
subclasses"; the registry's is "loadable element types." Forcing the first through the
second and absorbing the difference into `exempt()` is how a set-equality proof becomes a
statement about a set nobody chose.

`ElementDialogs` is worse: it holds *two* maps (`BY_TYPE` at :26, `CHANGE_BY_TYPE` at :29,
8 `registerChange` sites). The change map can never be total — `Text` and `JumpEnd` have
nothing to change. So for dialogs the honest predicate is not "the set is total"; it is
"for every element type an author made an explicit decision." #375 models "decided absent"
as a `String` in an exemption set, which is untyped, remote from the element, and — per
#315 §4 invariant 5, quoted in its own words — "the containment assertion wearing a
different hat."

**3. The mechanism cannot close its own I2.** #315's I2 is "a new registry-keyed table
that does not extend the base is flagged." #375's answer is `ArchitectureRulesTest`
reading `docs/registry-keyed-tables.md`. A table written next month and not added to that
markdown file is invisible to the check. The failure mode being eliminated —
*a table nobody remembered to keep total* — is reintroduced one level up as
*a table nobody remembered to add to the inventory*. §11 concedes this ("the inventory is
an input this task does not control") and mitigates it with "re-read it first," which is
the review habit the feature exists to replace.

**4. The user-visible defect is real, is on master, and is explicitly deferred.**
`Element` no longer has a `draw` method at all; `ElementRenderers.draw` (:53-57) does an
exact-class lookup and, on a miss, calls `ElementRenderSupport.drawHighlight` — which
paints a pink rectangle only when selected. `ElementDialogs.setup` (:103-107) returns
"not cancelled" with no dialog. `RegisterFile` and `FieldExtend` have palette rows
(`src/jls/edit/Palette.java:156,160`) and appear nowhere else in `src/jls/edit/`.
A student drags a Register File onto the canvas today and gets an invisible, unconfigurable
element. The class javadocs still say "an unconverted element still draws itself"; that
sentence has been false since the #77 wave finished. #375 §12 puts fixing this out of
scope and files it behind ~1.5 weeks of inventory plus ~3 days of harness. For a tool whose
README describes it as a pedagogy instrument for students drawing circuits, that ordering
is upside down.

## Alternatives the issue never considers

**A (primary) — make totality structural, not asserted.** The two-layer descriptor split
(#78: core `ElementType`, GUI `PaletteEntry`) is already the right seam; it was simply never
carried far enough. Give the GUI-side descriptor the renderer and the dialogs as fields of a
sealed decision type:

```java
sealed interface Facet<T> permits Provided, DeliberatelyAbsent { }
record PaletteEntry(ElementType type, ..., Facet<ElementRenderer> renderer,
                    Facet<ElementDialog> create, Facet<ElementDialog> change) { }
```

`ElementRenderers.BY_TYPE` and both `ElementDialogs` maps become one-line derivations over
the same rows the palette is derived from. Then a thirty-sixth element type *does not
compile* until the author writes `Provided(new FooRenderer())` or
`DeliberatelyAbsent("wire ends are drawn by the wire renderer")`. No inventory document.
No `exempt()` sets. No `CONTRIBUTING` bullet to remember. No base class to extend. The
reason lives next to the element instead of in a `Set.of(...)` two directories away, and
— the part #375 cannot do at all — the rule applies to tables written *after* the audit,
because there are no separate tables. `Wire` keeps a renderer without lying about being a
registry row, because the drawable set is declared where drawables are declared.

**B (cheap, inside #375's own philosophy) — make the inventory executable.** If a test-only
route is preferred, delete `docs/registry-keyed-tables.md` as the machine input and mark the
tables themselves: an annotation on the field, or a `RegistryKeyedTable<K,V>` wrapper type
that all six use. Then the sweep finds every table by reflection, I3's "mechanical sweep
agrees with the document" collapses into a tautology, and I2 genuinely closes. This is
strictly better than the markdown form at lower cost, and it survives the audit finding
tables nobody predicted.

**C (converges the two open threads) — put cardinality on the seam.** #277 wires consumers
to the boot `ExtensionRegistry`; `ElementExtensionPoints.ELEMENT_PROVIDER` already types a
seam by contract class. Extending `ExtensionPoint` with a declared key-domain and cardinality
would make "this seam is total over the element key set" a property checked once, generically,
*at boot in production* — not N times in test. #375 treats #277 as a scheduling hazard
("whichever lands second decides"); under C, #277 is the vehicle and #375's harness is
subsumed rather than raced.

## What I would keep

The equality-not-containment insight (§7.10) is correct and should survive into any design.
The `RegistryTotalityTestBaseSelfTest` discipline — a check that has never been seen to fail
is vacuous — is the repo's best habit and belongs in whatever lands. The two incident
citations (`970db41`, `b54e6ee`) are the right evidence.

## Disregarding the stated acceptance criteria

I am explicitly setting aside these Definition-of-Done items: "the four rewritten checks
assert the same thing," "`CONTRIBUTING.md` carries the new bullet," and
"`ArchitectureRulesTest.everyRegistryKeyedTableHasATotalityTest()` reads the committed
inventory." They are internally coherent and they measure the wrong thing — the first two
lock in the parallel-table shape the architecture is trying to retire, and the third pins a
check whose completeness is bounded by a markdown file. Also set aside is §12's
"the renderer and dialog gaps are not this task's to fix": two palette elements that render
as nothing is the defect, and the lint is the scaffolding.

## Recommended reordering

1. Register renderers and dialogs for `RegisterFile` and `FieldExtend`; decide `TestGen`
   and `WireEnd` explicitly. One PR, immediate student-visible value, and it is the
   fastest way to learn what the facet decision type actually has to express.
2. Fold the facets onto the GUI descriptor (Alternative A) for renderers and dialogs.
   Delete `BuiltinElementRenderers.install()` as a hand-written list.
3. Keep #375's base class only for tables that genuinely cannot be derived — the frozen
   save-tag table (`SaveTags`, 32 entries against 35 registry rows, deliberately not the
   same set) and the HDL export policy — and mark them per Alternative B rather than
   documenting them.
4. Fix the stale javadoc in `ElementRenderers` and `ElementDialogs` that still claims an
   unregistered element draws and sets itself up. It does not, and that sentence is why
   the gap read as harmless.
