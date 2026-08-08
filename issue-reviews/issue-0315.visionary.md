# Issue #315: FEAT-001 (RESIDUAL): a thirty-sixth element type cannot ship until every registry-keyed table has a row for it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

*(Evidence below is read from the working tree at `/home/user/JLS`, not from the
disputed pins `2d0ca9d` / `07a0bea`; see the issue's own correction comments.)*

## What this issue is actually for

The outcome sentence is right and worth having: **adding an element type should be
one edit, and every place that owes that type a row should say so before the change
ships.** That is a real defect class, `ARCHITECTURE.md` "Adding an element today (the
honest list)" prices it at sixteen touch points, and seven capstones are downstream.
I endorse the outcome without reservation.

What I am reframing is the *mechanism*. #315 proposes to certify a set of
hand-maintained side tables with (a) a committed inventory document and (b) a
mandatory JUnit base class plus a `CONTRIBUTING.md` rule. That is the design you
build when the key set is **open** — when new keys appear at runtime and no compiler
can see them. JLS ratified the opposite world.

## The mechanism the project already paid for, and this issue never mentions

`src/jls/elem/Element.java:17-18` — the element hierarchy is **sealed**:

```java
public abstract sealed class Element
		permits DisplayElement, LogicElement, Wire {
```

and `test/jls/elem/SealedHierarchyTest.java` states the intent in its own words:
a new element subtype "must appear here as well as in the `permits` clause, **which
is the authoring checklist the compiler now enforces at every exhaustive dispatch
site**." That is #315's capability statement, already shipped, for every table
expressed as a dispatch instead of as a `Set`/`Map`.

The single sharpest exhibit is that **both patterns live in the same file, sixty
lines apart**. `src/jls/hdl/HdlExporter.java:1071-1080`:

```java
return switch (gate) {
case AndGate _ -> HdlModel.GateStatement.Op.AND;
...
case Extend _ -> null;
};
```

No `default`. Add a ninth `Gate` and `javac` fails, naming file and line — exactly
integration criterion I1, for free, today. Meanwhile `HdlExporter.java:422-438`:

```java
private static final Set<Class<?>> EXPORTED = Set.of(...);
private static final Set<Class<?>> SKIPPED  = Set.of(...);
private static final Set<Class<?>> TOPOLOGY = Set.of(...);
```

Three hand-maintained class sets whose union is 31 of the 35 registered types
(`Memory`, `RegisterFile`, `FieldExtend`, `SubCircuit` fall through — the #492 / #873
/ #358 finding). This is the table #315 wants to pin with a test base. It is one
mechanical rewrite away from being the exhaustive switch above it. The project does
not need a new discipline; it needs to finish applying the one it adopted in #95.

## Alternative framing: three kinds of table, three different right answers

#315 treats "registry-keyed table" as one category with one remedy. It is three, and
only the third wants a test base.

**(1) Tables that should be dispatch, not data.** The HDL three-bucket policy, any
orientation or edit-op kind table, the print-symbol map of #540. Remedy: convert to a
`switch` over the sealed hierarchy with no `default`. Payoff beyond totality — the
*exemption* stops being a named set in a document with a written reason (§4 invariant
5, Open Question 3, the `NON_PALETTE_TAGS` idiom at
`test/jls/edit/PaletteContractTest.java:43-45`) and becomes `case Display _ -> SKIP;`
at the dispatch site with the reason as its comment. An exemption that lives at the
site it exempts cannot drift from it, and reviewing it is reviewing one line rather
than reconciling a doc against a test against a set.

**(2) Tables that should not exist.** `src/jls/elem/SaveTags.java` maps 32 tag
strings to classes; `ElementRegistry.ALL` maps 35 tags to classes and factories;
and each element writes its own tag a *third* time as a string literal
(`src/jls/elem/FieldExtend.java:291`, `src/jls/elem/RegisterFile.java:321`). Three
copies of one namespace, none derived from another. `SaveTags`'s own javadoc says the
ending: "When the element registry (issue #78) lands, this table is the save-format
column it absorbs." The registry has landed. #488 — attached to this feature as "a
named counterexample to the tag table's totality" — is not a totality defect at all;
it is copy A and copy C disagreeing with copy B, and the two missing tags are exactly
the two types that exist in the registry and the save methods but not in `WRITABLE`.
Under #315 as written, the fix is two rows plus a totality test that pins three copies
in sync forever. Under the reframe, the fix is deleting `WRITABLE` and deriving it
from the registry, after which the defect class is *unrepresentable* and no test is
owed. **Totality tests are the recurring rent on duplication; the elegant move is to
stop duplicating.** A table converted this way leaves the inventory rather than
entering it.

**(3) The genuine residue.** Key sets a compiler cannot see: resource files
(`resources/help/**`, `Map.jhm`, `JLSHelpTOC.xml`, the toolbar gifs), the normative
markdown tag table in `docs/file-format.md`, the ordered `Palette.entries()` list
whose *ordering* is data even though its *coverage* is not, and anything assembled at
boot from `ExtensionRegistry` after #277. These want #375's base class. That is a
much smaller domain — and, crucially, one that can be written **first and quickly**,
because its shape is no longer contingent on an audit of everything.

## The concrete redesign, child by child

- **#372 (TASK-0001)** stops being "audit and pin every registry-keyed table" and
  becomes **"convert or delete"**: a mechanical sweep for `Set<Class<? extends
  Element>>`, `Map<String, …>` keyed by element tag, and `instanceof` ladders over
  elements; each hit is dispositioned *convert to exhaustive switch* / *derive from
  the registry and delete* / *residue, needs a runtime check*. The deliverable is the
  conversions, landing one table per PR, each independently reviewable and revertible.
  The surviving document is a short §  in `ARCHITECTURE.md` naming the residue —
  not a `docs/registry-keyed-tables.md` enumerating everything, which is the artifact
  the issue itself admits "decays within one release."
- **#375 (TASK-0002)** ships the base for the residue only, and is **unblocked** —
  the §6 "blocked by necessity, because `exempt()`'s shape depends on the audit"
  argument dissolves once categories (1) and (2) never reach the base class. The
  `CONTRIBUTING.md` rule becomes narrower and therefore actually enforceable: *a new
  per-element table is a `switch` over the sealed hierarchy; if it cannot be, say why
  in the PR and extend the base.*
- **#78** (the roster addition — make the runtime-throw stubs compile-time
  obligations) stops being the "compile-time half" of a mostly-runtime programme and
  becomes the same move applied to the hierarchy. Under the reframe #78 and #372 are
  one idea, which is an argument for doing them adjacently.

## What I am disregarding, explicitly

Two Definition-of-Done items should not be met as written:

1. *"The committed inventory names every registry-keyed table with its file, key
   type, miss behaviour, exemption set and pinning test."* An inventory is a
   second, unlinked copy of a fact the code already holds — the same failure mode as
   `SaveTags` vs `ElementRegistry`, one level up. It is also the item that drives the
   unresolved 1-2 mw band vs 2.1 wk row sum (Open Question 1) and the unbounded
   "how large a finding still lands here" (Open Question 2). Both questions
   **disappear** under a per-table conversion programme: there is no audit cliff to
   price, and every downstream issue holding `blocked_by: [315]` stops waiting on a
   monolith it does not consume.
2. *"`CONTRIBUTING.md` carries the rule that a new registry-keyed table extends the
   base class."* A rule enforced by a document plus a lint is strictly weaker than a
   language feature that makes the wrong thing not compile, and it taxes every future
   author. Keep the rule only for the residue.

I would also drop I1's synthetic-thirty-sixth-type harness for categories (1) and
(2). The observation "at least one totality test fails per inventoried table" is a
costlier restatement of "these nine switches no longer compile," and the compiler's
message already names the file and line — which is I1's actual requirement.

## Where #315 is right and must survive the reframe

- **Equality, not containment.** $C(T) = K \setminus X(T)$ is correct, and the
  stale-row term is the one containment cannot see. An exhaustive switch gives it in
  both directions natively: an unreachable `case` for a deleted type is also a
  compile error.
- **The closed-world premise is already ratified**, which is why this reframe is
  aligned rather than contrarian: #80 removed the plugin loader, #95 sealed the
  hierarchy for that reason, #222 keeps first-party modules in one classloader with
  one type namespace. #315's design is the open-world design; the project is closed
  by decision.
- **The harm claim is real but table-dependent** and the issue would be stronger for
  saying so: HDL export already fails loud on an unclassified type
  (`HdlExporter.java:191-195` throws naming the offenders), and the palette is
  already covered. The genuinely silent tables are the save-tag namespace and
  anything reached by a `getOrDefault` — which is another reason to attack by
  category rather than uniformly.

## The one thing that could refute this reframe

**#277 and #212.** If element types ever arrive from a `ServiceLoader` provider, the
key set is open at boot and compile-time exhaustiveness is unavailable downstream of
it — and #315's runtime machinery becomes the right answer after all. The reframe's
answer, and I think it is the better architecture regardless: keep the **row** set
closed (sealed hierarchy, one registry) and let modules contribute **columns** —
palette entries, emitters, print symbols — through the #223 typed seams. "A module
may add a column, never a row" is a sharper and more defensible boundary than
#315's Open Question 4 ("does the base class key off `ElementRegistry.all()` or off
the boot snapshot?"), and it is the boundary #222's demand gate already implies.
That decision belongs in this feature, not deferred to whichever of #277/#315 lands
second.
