# Issue #349: FEAT-040: JLS knows what a real part is — pinouts, sections, footprints and loading are queryable data, extensible with a text file and no Java
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the schema talk, #349 asks for two things JLS has never had: **a library of
hardware building blocks expressed as data**, and **a mapping from a drawn element to a
member of that library**. Everything the issue then lists — BOM, wiring list, netlist
footprint column, fan-out check, breadboard placement — is a consumer of those two things.
That is the right ambition and it is the project's own recorded direction, not a new one:
`docs/capability-roadmap/sweep-06-physical-boundary.md:73` records the gap as *"JLS has no
concept of a technology cell… a cell cannot be data"*, and change **D** at `:250-262` asks
for exactly "cells as data, not classes — name, pin list (name, direction, width), logic
function, timing figure, area — loaded from a library file", plus a lookup at
`NetlistImporter.mapCell` (`src/jls/hdl/imp/NetlistImporter.java:227-232`) where a
technology cell is currently rejected outright. The 74-series DIP table is the *first
population* of that library, and the element→part binding is JLS's *first technology
mapping*.

The issue never says this. It frames the deliverable as a package library for
through-hole parts, and that framing is what produces the specific shapes I want to change.

## Where it already pulls with the arc (endorse, unchanged)

- **"A part is data, not code"** is the correct axis, and the precedent is shipped:
  `src/jls/hdl/board/Board.java:8-25` ("a board is deliberately just data… adding a board
  is adding a table entry, never new code"). Classpath data with an integrity test is also
  established — `resources/help/Map.jhm` + `HelpTopicsTest`.
- **Data-only user extension** is the *ideal* case under the ratified trust posture
  (ARCHITECTURE.md, "Plugin trust boundary", #222): a closed, data-only vocabulary is
  precisely what that decision prefers over code providers. Say so in the issue; it is a
  stronger justification for criterion 1 than "no recompilation".
- **No geometry (invariant 1)** is right, and it is safe for a subtler reason than the
  issue gives: the footprint *name* is a pointer to geometry someone else curates. See the
  namespace point below.

## Reframing 1 — named columns, and the version treadmill disappears

§7 says any column addition bumps the schema version, and Open Question 5 exists only
because of that ("add the footprint column later means revisiting every entry"). Both are
artifacts of a positional record. Make the file declare its columns in a header
(`COLUMNS number pins sections footprint …`) and reading becomes name-keyed: adding a
column is backward compatible, an unknown column is a named error, and the version bumps
only on a *semantic* change. Open Question 5 then stops blocking TASK-0085 — the footprint
column can be added on the day #366 needs it, at zero cost to existing entries. This also
makes Reframing 4 (columns earned by a consumer) cheap instead of expensive.

## Reframing 2 — a part is a *function* × a *family/package*, not a flat 13-tuple

This is the change with the largest leverage, and the issue's own Open Question 2 is the
symptom. "What is the default subfamily?" is only a question because the schema keys
everything on one part-number row. Factor it:

- **Function code** (`00` = quad 2-input NAND, `138` = 3-to-8 decoder) fixes pin count, pin
  names and directions, section count, the section-to-pin map, supply pins, and the
  gate-equivalence class. 74LS00, 74HC00, 74HCT00 and 74ALS00 share all of it.
- **Family** (`LS`, `HC`, `HCT`) fixes the electrical figures and floating-input behaviour.
- **Package** (`N`/DIP-14) fixes the footprint name.

Consequences, all of them removals of work the issue currently plans to do by hand:
Open Question 2 dissolves (there is no default subfamily; a row names its family or is a
function row). The substitution list stops being transcribed — parts substitute when they
share a function code and have compatible levels — which matters because §3's three
predicates **do not check substitution symmetry or transitivity at all**; a flat table can
say 74LS00 substitutes 74HC00 while 74HC00 says nothing, and nothing catches it. Gate
equivalence likewise becomes derived rather than asserted. And IC-8's per-entry manual
cross-check — the most expensive line item in the whole feature — is paid **once per
function**, not once per (function × family) row, which is also where the 4–8 week band's
upper half lives (Open Question 3). The transcription cost drops roughly by the family
multiplier.

## Reframing 3 — one library seam, not a parts-only one

`src/jls/pkg/` with its own grammar, its own version header, its own override flag, and its
own aggregation idiom is JLS's *third* bespoke table mechanism (`Boards.ALL`, the §7 delay
table in `docs/simulation-semantics.md:275-300`, now parts) and its *second* library format
(Liberty/LEF subset reading is change D's part 2/3). The elegant cut is one loader —
versioned header, aggregated errors, documented shipped∘user resolution — with the *entry
shape* per library, and the `.parts` table as its first client. Two concrete dividends:

1. **`Boards.java:81` (`List.of(ICESTICK)`) migrates onto it.** The #400 boundary comment
   flags that hardcoded entry as a warning; a shared loader *fixes* it instead of admonishing
   about it, and it gives the loader a second client on day one — the only real test that a
   mechanism is general.
2. **The binding is a technology-mapping seam, not a "package binding".** Name it that.
   `docs/extension-points.md:28-37` catalogues seams by contract, `ElementType`
   (`src/jls/elem/ElementType.java:31`) is already the descriptor+factory half, and a
   drawn-element→library-member map will be wanted at least three times: DIP parts here,
   sky130 cells under change D, KiCad symbols under #307. Built once as
   "element → library member (+ instance/section index)", the sky130 case is data; built as
   "element → 74-series package", change D re-cuts it.

Register the seam in `docs/extension-points.md` as `pkg.part-library` (status *pending*,
owner #349) so the catalog test carries it — the project's own discipline for a seam that
is named before it is typed.

## Reframing 4 — the inert columns are the wrong solution to a real fear, and IC-1 contradicts IC-7

The fear is right: JLS must not report drive-strength answers its simulator cannot back
(`docs/simulation-semantics.md:422-443`, first-driver-wins). The mechanism is wrong, and it
is *internally inconsistent* as written:

- **IC-1** requires a newly added part to be visible "from the packing pass, from an
  emitter, and **from the loading check**".
- **IC-7** requires that the loading figures have **no non-test reader**, enforced by an
  architecture test.
- A loading check *is* a non-test reader of the loading figures, and #365 is explicitly
  **not** blocked on #341 for it (this issue's own comment of 2026-08-04: "worth having
  *before* the simulator models drive strength").

The three cannot all hold. The intent is expressible exactly, in an idiom the tree already
uses (`HeadlessCoreRatchetTest`, `NullMarkedRatchetTest`, `NotificationRatchetTest`): a
**package-dependency ratchet** — `jls.sim` must not import the library package, and the
value domain of `docs/simulation-semantics.md` §2/§9 is unchanged by anything here. Then
the fan-out check may read the figures (it is arithmetic over datasheet numbers, correctly
scoped to #365), the simulator may not, and no criterion contradicts another. I am
explicitly setting aside invariant 3 and IC-7 as written.

Related: figures that no shipped code reads are figures no test can falsify, and IC-8's
manual cross-check still has to cover them. Under Reframing 1 they cost nothing to add
later. Prefer earning each column with a consumer over transcribing speculatively.

## Sequencing — consumer-first, not schema-first

§2 rejects "data first, schema later" on a real dependency. It never considers
**consumer-first**: land the thinnest vertical slice that a user can see — one drawn
circuit of 74-series gates producing a BOM and a point-to-point wiring list — with the table
holding only the columns that slice needs. Every later column is then pulled by a failing
consumer test rather than pushed by transcription. Note that IC-1, this feature's declared
discriminating criterion, names three consumers (`packing pass`, `emitter`, `loading check`)
that live entirely in #365/#366 — the issues this one *blocks*. As written, the feature
cannot demonstrate its own close-out criterion without stubbing three passes that do not
exist. Consumer-first repairs that; otherwise IC-1 must be re-homed downstream and this
feature must say so plainly rather than listing an unbuildable criterion.

## One cheap decision worth recording now

Make the footprint field's namespace explicit: KiCad's standard library naming
(`Package_DIP:DIP-14_W7.62mm`). #307 (CAP-13) is a KiCad round-trip capstone, so the string
JLS emits should be one KiCad resolves without a mapping table, and the DIP body width — the
one geometric fact a breadboard placement plan needs — rides along inside the name without
any geometry entering the schema. An opaque unvalidated string (comment 3) becomes an
opaque string *in a named namespace*, which costs nothing and buys the whole downstream.

## Verdict

**endorse-with-reframing.** The capability is right, needed by three capstones, and
consistent with the project's recorded trajectory — this is the physical programme's first
real step and it should be taken. But the issue frames a permanent seam as a parts table:
key it as function × family, declare columns by name, build one library loader (with
`Boards` as its second client), name the binding as technology mapping, and replace the
"inert columns" invariant with a `jls.sim` dependency ratchet so IC-1 and IC-7 stop
contradicting each other. Reframed that way, Open Questions 2 and 5 dissolve, Open
Question 3's bound shrinks by the family multiplier, and change D of the physical-boundary
roadmap arrives as data rather than as a second library format.
