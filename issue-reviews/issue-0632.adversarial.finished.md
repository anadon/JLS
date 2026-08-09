# Issue #632: TASK-C597-1: the board flow gets a File-menu entry and a board picker read from Boards.all() — keyboard-reachable, stably named, and needing no GUI change when a board is added
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is the entry-point task of FEAT-C38-1 (#597): a File-menu item plus a
board picker sourced from `Boards.all()`, with the pin-assignment dialog
(#634, TASK-C597-2) and the one-click handoff action (#636, TASK-C597-3)
correctly left to sibling tasks. The decomposition is sound and the scope is
tight. The problems are in the acceptance criteria themselves: one of them
(AC-1) describes a test the current codebase has no way to write, and two
others (AC-2, AC-4) lean on verification methods that can pass while the
stated goal is only partly true.

## Findings

### 1. [High] AC-1's "synthetic board" test has no seam to attach to

AC-1: *"the picker's entries are read from `Boards.all()`, asserted by a
test that adds a synthetic board and observes it appear with no GUI source
change."*

`Boards.all()` returns a hard-coded field with no injection point:

```java
// src/jls/hdl/board/Boards.java:81
private static final List<Board> ALL = List.of(ICESTICK);
...
public static List<Board> all() {
    return ALL;
}
```

There is no setter, no `ServiceLoader`, no test-only registration hook —
`Boards` is `final` with a private constructor and exactly one static
factory method returning an immutable list built at class-init time
(confirmed: `grep -n "static" src/jls/hdl/board/Boards.java` shows only
`all()`, `byName()`, `names()`, none of which accept a `Board`). The sibling
task #416 (TASK-0052, second board) treats adding a board as editing this
source file, not as a runtime operation. `test/jls/hdl/board/BoardFixtures.java`
— the existing test-fixture pattern for this package — likewise builds
circuits, not boards, and has no board-injection helper either.

So AC-1 as literally written cannot be satisfied without one of: (a) adding
a test-only mutation path to `Boards` that production code must never use
(a foot-gun this codebase generally avoids — cf. the deliberate immutability
of `Boards.ALL` and `Board`'s own compact constructor pinning `pins` to an
unmodifiable sorted copy), (b) parameterizing the picker widget over an
injected `List<Board>` so the *test* supplies the synthetic board while
*production* wiring passes `Boards.all()` — which then requires a second,
separate assertion that production wiring actually calls `Boards.all()`
(the AC does not distinguish these two tests), or (c) reflection tricks
against a `private static final` field, which is exactly the kind of
brittle test this codebase's `ElementConstructorContractTest`/`SaveTagsTest`
style deliberately avoids elsewhere.

**Recommendation:** state explicitly which of (a)/(b) is intended, and if
(b), split AC-1 into two predictions: "the picker renders whatever board
list it is given" (unit-testable with a synthetic list) and "production
wiring passes `Boards.all()`" (a one-line source-scan or reference-equality
check, not a synthetic-board scenario).

### 2. [High] AC-2's naming citation doesn't cover this widget class

AC-2: *"Every widget in the flow is reachable keyboard-only and carries a
stable component name per `docs/component-naming.md`."*

`docs/component-naming.md` currently defines exactly two naming families:
palette/tool-bar mirror-menu names (`palette.<slug>`, `menu.elements.<slug>`)
and `ElementFormDialog`-based element create/modify dialogs (`dialog.ok`,
`dialog.cancel`, `dialog.<slug>.<field>`, wired through the shared
`labelled(...)` helper). A board picker is neither: it is not an element
palette entry and, being a board chooser rather than an element-parameter
form, has no obvious reason to extend `ElementFormDialog` or to have a
"slug" in the `ElementRegistry` sense the doc's field-naming scheme assumes.
The doc's own "Adding a new element" section (the only extension guidance
it gives) is scoped to elements, not to menu-driven flows like this one or
like #288's still-unbuilt "Export HDL…" item.

The AC treats the document as if it already has a rule for this case; it
doesn't. Whoever implements this either invents an ad hoc scheme that may
collide with what #634/#636 invent independently for the pin dialog and
handoff button (both siblings under the same parent feature, both citing
the same doc), or the doc needs a new section first — and that documentation
work isn't listed anywhere in this issue's acceptance criteria.

**Recommendation:** either add the missing "menu flow" naming section to
`docs/component-naming.md` as part of this task's own deliverable (cheap,
and it unblocks #634/#636 from guessing independently), or have #632/#634/#636
explicitly agree a `menu.board.*`/`dialog.board.*` prefix in one of their
bodies before any of them starts.

### 3. [Medium] The dependency on #288 is real but the prose overstates its readiness

The issue frames reuse of "#288's menu, chooser and accessible-name pattern"
as drawing on established precedent. At the time of this review neither
exists: `src/jls/JLSStart.java`'s `fileMenu()` (lines 1377-1590) has
`JMenuItem`s for New/Open/Save/Import/Export Image/Close/Exit only — no
"Export HDL…" item, confirmed by `grep -n "Export HDL" src/jls/JLSStart.java`
returning nothing. #288 itself is open with every Definition-of-Done box
unchecked. `ordering_after: [264, 288]` in the YAML front-matter does
correctly encode this as a prerequisite, so this is not a hard defect, but
an executor who reads only the prose ("reuses #288's ... pattern") rather
than the machine block could reasonably start work believing there is
something to copy from. #264 is in a similar state: still open, with its
own "iCEstick real-toolchain evidence" and Stage-2 rows unstarted — though
#264's *board data model* (`Boards`, `Board`, `PcfEmitter`) that #632
actually depends on is already landed, so #264's ordering dependency here is
looser than #288's (this task needs #264's already-shipped data model, not
#264's still-open hardware-evidence rows).

**Recommendation:** note in the body (not just the YAML) that #288 has not
landed at issue-filing time, so a picker built ahead of #288 has no
"Export HDL…" menu to sit next to and no established chooser/accessible-name
pattern yet to copy.

### 4. [Medium] "Keyboard-reachable" is pinned only by a source-scan test, not real focus-traversal

AC-2 asks for this to be "asserted by a headless pin test in the
`MenuAcceleratorPolicyTest` pattern." Reading that test
(`test/jls/MenuAcceleratorPolicyTest.java`) confirms the pattern: it is a
"compensating control" that reads `JLSStart`/`SimpleEditor` *source text*
and checks accelerator/mnemonic constants — it does not construct or drive
a live GUI, because (per its own javadoc) "the GUI cannot be constructed
under surefire's `java.awt.headless=true`." `ARCHITECTURE.md`'s "Test
layout" section is explicit that this is Layer 1 only ("headless model
assertions"); Layers 2 (Swing harness under Xvfb) and 3 (render-to-image)
are "reserved," i.e., not yet used by any test in this codebase. So a test
in this pattern can confirm a widget was assigned a mnemonic/accelerator in
source, but cannot confirm Tab order actually reaches it, that focus isn't
trapped, or that a screen reader announces it correctly in the live dialog.
AC-2's "reachable keyboard-only" claim will be satisfied by a test that
proves something weaker than what it says.

**Recommendation:** either scope AC-2's language down to "every widget
declares a mnemonic/accelerator and a component name" (what the test
pattern actually proves), or add a `@Tag("display")` Xvfb-driven check as
#288 flags as an open question for its own chooser flow — don't claim
Layer-1 coverage proves a Layer-2 property.

### 5. [Medium] AC-4's "no board list of its own" test is a heuristic that a cache can satisfy without honoring the spirit

AC-4 wants a test asserting "the GUI holds no board list, no format list and
no pin table of its own." As stated this is almost certainly implemented as
a source-scan (no hard-coded board/pin literals in the new GUI class),
mirroring the `MenuAcceleratorPolicyTest`-style compensating control used
elsewhere in this codebase. That check cannot distinguish "queries
`Boards.all()` fresh each time the menu opens" from "calls `Boards.all()`
once at construction and caches the returned reference in a field" — both
produce GUI source with no board-name literals, but only the former
actually delivers AC-1's promise that "a board arriving through #264 or
#416 appears with no GUI change" for a JLS process whose `Boards` table
could change between menu opens (relevant to a future `ServiceLoader`-based
board registry, foreshadowed by the recorded "Plugin mechanism" decision in
`ARCHITECTURE.md`, though not by anything shipped today). Low practical risk
today since `Boards.ALL` is `static final` and cannot change within a
running JVM anyway — but the AC doesn't say so, and a reviewer checking the
letter of AC-4 wouldn't catch a component that resolves the board list once
at class-load instead of per-open.

**Recommendation:** state in the AC whether "reads from `Boards.all()`"
means "queries on every menu open" or "may cache for process lifetime" —
currently immaterial given `Boards`' immutability, but worth pinning
explicitly since the AC's own justification ("no GUI change when a board is
added") implicitly assumes fresh reads.

### 6. [Low] `band_mw: "0.5-1"` is unitless and undefined in-repo

Same gap flagged elsewhere in this review fleet (e.g. issue #765, #777):
`band_mw` appears in the YAML front-matter with no definition anywhere in
`docs/` or `ARCHITECTURE.md` for what unit or estimation method it encodes.
Not a defect specific to this issue, but it means the "0.5-1" estimate here
can't be checked or held to account.

### 7. [Solid] Decomposition and non-goals are correctly scoped

One line each, no elaboration needed:

- The three-way split against #634 (pin dialog) and #636 (handoff button)
  is clean — #632 genuinely only needs to cover menu entry + picker, and
  the sibling issues (verified) pick up exactly what #632 leaves out.
- AC-5 ("nothing about the headless path changes... their tests pass
  unmodified") is concrete, cheap to verify (`mvn verify` plus a diff-empty
  check on `jls.hdl.board`/`scripts/`), and correctly protects #264/#416's
  ownership of the headless contract.
- AC-3 ("board name and FPGA part... read from the `Board` record, never
  re-typed") matches the actual `Board` record's fields
  (`src/jls/hdl/board/Board.java:26-27`: `name`, `fpga`) exactly — no
  invented fields, no scope mismatch with what the record can supply.

## Verdict rationale

Not `needs-rework`: the scope, decomposition, and non-goals are correct, and
none of the findings require re-architecting the task. But AC-1 as written
describes a test that cannot be built against the current `Boards` API
without a design decision the issue doesn't make, and AC-2/AC-4's
verification methods can pass while understating or missing the real
property being claimed. `sound-with-concerns`.
