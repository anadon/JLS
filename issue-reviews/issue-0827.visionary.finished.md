# Issue #827: TASK-C569-3: someone who is not the maintainer follows the published walkthrough to a working element jar, from outside this repository
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the acceptance criteria and #827 is the only issue in the #569/#514 chain
that produces *evidence about a stranger*. #825 enumerates seams, #826 labels
them, #212 proves a mechanism — all three are things the maintainer can assert
about the maintainer's own code. #827 alone has a falsifiable subject the project
does not control: a person, outside, reading only what was published, and the
list of places they had to guess. That instrument is right, and it is the part of
this task worth defending against every simplification below. The adversarial
comment already fixed the recruitment hole in AC-3.

What it is *for*, one level up, is capstone #514's premise: that JLS can absorb
the developers Digital is stranding by offering the plugin story Digital never
gave. #827 is where that premise gets tested with a real person.

## The contradiction nobody in this chain has named

`jls.elem.Element` is a **sealed** hierarchy, and it is sealed *on purpose*:

- `src/jls/elem/Element.java:17` — `public abstract sealed class Element permits
  DisplayElement, LogicElement, Wire`
- `src/jls/elem/LogicElement.java:17-21` — permits exactly the 23 built-in
  subtypes, no more
- `src/jls/elem/Put.java:16` — even connection points are `sealed ... permits
  Input, Output`
- `test/jls/elem/SealedHierarchyTest.java:14-21` — a ratchet test pinning the
  whole `permits` tree, whose javadoc says the quiet part aloud: *"the element
  set is closed by design (#80 removed the plugin mechanism), so the type system
  says so."* Every leaf must additionally be `final` (line 85-94).

An external jar therefore **cannot define an element at all**. `ElementType`'s
constructor takes `Class<? extends Element>` and `Function<Circuit, Element>`
(`src/jls/elem/ElementType.java:57`); no type outside this compilation unit can
inhabit those bounds. #212 diagnoses its own blocker as the closed `List.of(...)`
literal at `ElementRegistry.java:38` — that is the *lesser* closure by an order of
magnitude, and #212, #569, #825, #826 and #827 do not mention sealing once.

So #827's AC-1 ("a jar whose element places, saves and loads in a released JLS
build") is not merely gated on #212's unfiled, demand-gated tasks. It is gated on
reversing decision #95, which is currently enforced by a green test. Three
recorded decisions — #95 (closed taxonomy), #222 (in-process trusted extension),
#212 (external providers when demand appears) — have never been reconciled with
each other. #827 sits three tiers above that unresolved contradiction and would
discover it at walkthrough-writing time, which is the most expensive possible
moment.

There is also a circularity the adversarial comment stopped one step short of:
#212 opens when *a named requester asks*; #827 exists to create requesters. The
documentation is gated on the demand it was filed to manufacture.

## Reframing 1 — the extension path that already ships (recommended)

#212's named beneficiary is "instructors who want a course-specific element (a
custom ALU, a bespoke bus element) loadable in students' stock JLS without
forking." For that person, the jar is an implementation detail they never asked
for. JLS already has the mechanism: `SubCircuit` plus the editor's import path
(`SimpleEditor.addToImportMenu` / `finishImport`, `src/jls/edit/SimpleEditor.java:477,679`).
A course-specific ALU *is* a subcircuit. It needs no Java, no classpath, no
`ServiceLoader`, no un-sealing, no trust boundary, no stability promise over 1,400
lines of protected surface, and no GPLv3 in-process-linking question.

What is missing is not a plugin API but a **circuit library**: a documented search
path (`-lib <dir>`, or a conventional directory beside the circuit), stable
element naming, and a palette group so library subcircuits are *placeable* rather
than import-menu-only. That is a small, in-band feature which serves most of
#212's stated audience, and it is the only extension path that works in a
released JLS *today*.

Concretely for this task: retarget #827 to

> someone who is not the maintainer follows the published walkthrough and gets a
> course-specific element into their palette, from outside this repository

deliberately silent on the vehicle. The evidence product — a stranger's
guess-points, recorded on #569 — survives intact, and the task detaches from both
#212's demand gate and #95's seal. **I am explicitly disregarding AC-1's "jar"
and AC-2's "compiled against the published API"** on the grounds that they name a
mechanism, not an outcome, and the mechanism is the part that does not exist.

## Reframing 2 — if the jar path is genuinely wanted, cut a different seam

Do not un-seal `Element`. Doing so publishes `Element`'s 865 lines plus
`LogicElement`'s 572 as de-facto extension API, and #826 would then have to put a
stability label on all of it — a label no single maintainer can honestly write as
"frozen". A better seam, and the ADR that should exist before any walkthrough
text:

Keep the taxonomy sealed and add exactly **one** new permitted leaf —
`final class ExternalElement extends LogicElement` — that delegates to a small,
closed, data-oriented SPI (`ElementBehavior`: declare puts, describe geometry,
describe persisted parameters, `initSim`, `react`, `showInfo`). External authors
implement an interface with roughly six methods and never touch a protected
field. Properties this buys:

- `SealedHierarchyTest` gains exactly one name; every exhaustive switch still
  compiles; #95 survives as a decision rather than being quietly reversed.
- #826's frozen surface is six method signatures, not two god classes — a
  guarantee the project can actually carry (#826 AC-4).
- It is the *only* shape that survives #222's reserved out-of-process move: a
  data-only behavior protocol is serializable across a socket; a subclass of
  `Element` never will be. Choosing the subclass seam today forecloses the
  isolation ARCHITECTURE.md line 295 says it is holding open.
- `ExtensionPointCatalogTest.contractsAreClosedTypes` (line 151) already demands
  extension-point contracts be interfaces/sealed/final — this shape satisfies it
  natively; an open `Element` subclass would not.

## Reframing 3 — make the document the example, and stop shipping four of them

AC-2's open question ("in-tree or companion repository", which the adversarial
comment correctly flags as needing a decision) dissolves if the walkthrough is
*literate*: one `docs/extending-jls.md` whose fenced code blocks are tangled out
by a small script and compiled in CI. The doc is then the source of truth rather
than a copy kept in sync with one, rot is structurally impossible, no second
repository is maintained, and AC-2's cost collapses.

More broadly: #223's catalog, #825's enumeration, #826's stability table and
#827's walkthrough are four artifacts describing one API on a single-maintainer
project. A stranger reads a *tutorial*, not a catalog. Collapse them into one
`docs/extending-jls.md` — walkthrough first, catalog and stability table
generated as appendices — with one CI check instead of three separate
build-failure gates. That halves the maintenance surface #514 is trying to make
attractive, and it is itself an act of the elegance-at-first-contact the capstone
claims to care about (PF-3).

## What I would do with this issue

1. File the prerequisite ADR — *seal vs. delegate*, reconciling #95, #212 and
   #222 — and record it in ARCHITECTURE.md's decisions section. Nothing in the
   #569 tree can honestly proceed before it.
2. Retarget #827 to the vehicle-agnostic outcome above, and let the subcircuit
   library be the vehicle that unblocks it now.
3. Keep AC-3′ (with the adversarial comment's fallback and kill criterion) and
   AC-4 verbatim — the guess-point ledger is this task's entire reason to exist.
4. Replace AC-2 with the tangled-doc mechanism; replace AC-5's "stability labels
   of the seams it uses" with the honest sentence that the library path is data,
   not code, and carries the save-format guarantee already in
   `docs/file-format.md` — a stronger promise than any seam label #826 could
   truthfully issue.
