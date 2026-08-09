# Issue #631: TASK-C561-2: Falstad's logic subset becomes a working JLS circuit, with labeled nodes surviving as net structure
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its Falstad specifics, #631 is the second of three tasks under FEAT-C29-4
(#561), itself CAP-29's (#513) declared *demo slice*: the smallest format, chosen to
prove that a loss-naming import report generalizes to a non-XML, non-JSON source. So
the deliverable that matters is not "Falstad works." It is **"JLS has one importer
spine, and a fourth source format costs almost nothing to hang off it."** #631 owns
the load-bearing middle of that proof: semantic mapping and connectivity.

Judged against that goal, the outcome is right and the seams are wrong in two places
that the issue could not see because it was written from the capstone downward rather
than from the tree upward.

## Finding 1 — JLS already has labeled nodes, and AC-2 asserts the wrong invariant

AC-2 requires that "two elements joined only by a shared label are in one net after
import, asserted structurally." That invariant is unachievable and undesirable in
JLS's model, and the issue's own boundary note ("this is not a layout task") is what
makes it unachievable.

JLS's net structure is geometric: `Wire`/`WireEnd`/`WireNet`
(`/home/user/JLS/src/jls/elem/WireNet.java`), serialized as `WireEnd` chains
(`/home/user/JLS/docs/file-format.md`). One `WireNet` spanning two distant labeled
sites means *drawing wires between them* — routing, which the issue forbids — or
fabricating an in-memory net with no wires, which the save format cannot express and
`CircuitSnapshot` round-tripping would destroy on the first undo.

But JLS already ships the exact construct Falstad's labeled node is:
**`JumpStart`/`JumpEnd`** (`/home/user/JLS/src/jls/elem/JumpStart.java`, "Starting
point of a named wire"; `JumpEnd.java`, "Receiving end of a named wire"), resolved by
name through `Circuit.starts`, a `SortedMap<String, JumpStart>`
(`/home/user/JLS/src/jls/Circuit.java:59`, with
`addJumpStart`/`getJumpStart`/`getJumpStartNames` at ~1744–1793), and catalogued in
`docs/file-format.md:306` as "named-net source."

Mapping a Falstad label to `JumpStart` + N × `JumpEnd` **makes the routing problem
disappear entirely**: each labeled site gets a small local element at the coordinates
Falstad already gives you, nothing crosses the canvas, the placement stays legible for
free, and the result is indistinguishable from a circuit a student drew with the
START/END gesture. That is the elegant route the issue never considered, and it is
strictly better than the one AC-2 encodes.

The genuinely hard problem — which #631 does not name at all — is that **JLS's named
wire is directional and Falstad's labeled node is not.** `Circuit.starts` permits
exactly one `JumpStart` per name (`JumpStart` has an input; `JumpEnd` has an output),
whereas a Falstad label is an undirected node that may be driven from any of its
occurrences. So the importer must *infer the driver*: trace each labeled site's local
connectivity to find which one reaches an output, elect it the `JumpStart`, and refuse
by name when zero or two sites drive. That inference, not table-writing, is where this
task's week actually goes. AC-2 as written hides it; a rewritten AC should demand it.

**I am explicitly disregarding AC-2's stated criterion.** The right assertion is:
*a Falstad label with N occurrences imports as exactly one `JumpStart` and N−1
`JumpEnd`s of matching width, with the driving occurrence elected by connectivity; a
label with no driver or multiple drivers refuses by name rather than guessing.*

## Finding 2 — the importer spine already exists; do not grow a second one

CAP-29 assumes the `.circ` importer (#323) will "build the loss-naming report
machinery once." It already exists, shipped, for a different source:
`/home/user/JLS/src/jls/hdl/imp/` — `NetlistImporter`, `ImportResult`,
`ImportSummary`, `ImportException`, tested by `test/jls/hdl/imp/ImportPipelineTest.java`
and `NetlistImporterTest.java`. Its shape is precisely what #631 needs:

- source model → `CellValidator` gatekeeper → `LayoutGraph` →
  `HeuristicLayeredLayouter` → **emit plain-text save format** → load through the real
  loader (`ImportResult.saveText()`, "loading it and re-saving yields a circuit
  indistinguishable from a hand-drawn one");
- refusal by name with a teaching message and a source location:
  `CellViolation(module, cell, type, sourceLocation, message)` — "it names the
  construct, why it is out of scope for JLS, and the rewrite that will import";
- a mapping/disposition report: `ImportSummary` (per-category counts, coercions).

Emitting save text and loading it through `Circuit.load`/`finishLoad` gives #631 three
of its neighbours' acceptance criteria *for free*: no partial circuit on failure
(#629 AC-3), undoability and single-operation semantics (#633 AC-4), and structural
nets that survive save/undo. A bespoke Falstad path that builds `Element` objects
directly earns none of that and re-litigates all of it.

There is also a catalogued, still-*pending* seam waiting for exactly this:
`docs/extension-points.md:33` — `hdl.importer`, home package `jls.hdl.imp`, contract
"cell-map/layout contract to be defined", status pending (#61/#62). The document's own
rule is that "pending seams are named here first… so nobody invents a parallel
mechanism in the meantime." #631 is about to invent one.

## Finding 3 — the mapping table wants to be data, not prose

AC-1 asks for "a written, reviewable table in `docs/`, one row per Falstad element
code." JLS's established idiom is stronger and cheaper: a **table in code, cross-checked
against the document by a test** — `ElementRegistry`'s manual `List.of(...)` with
`ElementRegistryTest` enforcing totality (`/home/user/JLS/src/jls/elem/ElementRegistry.java`),
`SaveTags`/`FileFormatSpecTest`, `ExtensionPointCatalogTest` checking
`extension-points.md` in both directions, `CliFlagTableTest` over `JLSStart.FLAGS`.
Prose in `docs/` with no mechanical link to the mapper is the one form of table this
project has systematically stopped writing.

Better: the foreign-code mapping is a facet of `ElementType`. `ElementRegistry`'s
javadoc already reserves `ElementType.aliases()` for alternate tag spellings; a
foreign-format code is an alias in another namespace plus a semantic adapter. Do that
once and `.dig`, `.cv` and `.circ` inherit it — which is the *actual* CAP-29 thesis,
applied one level deeper than CAP-29 thought to apply it.

## Finding 4 — #556's report lineage has forgotten a shipped importer

#556 generalizes "the `.circ` report" into the format-agnostic contract, and CAP-29
AC-3 demands "one schema across all importers." Neither mentions `ImportSummary` /
`CellViolation`, which are in the tree today and are a report over an import. If #556
lands as written, JLS ends with two import-report types in two packages — the exact
failure CAP-29 AC-3 exists to prevent, committed by CAP-29 itself. #631 is the issue
that will feel this first, since it is the first CAP-29 importer to touch code.

## Alternative framing (primary)

**Reframe #631 as: "the Falstad front-end is the second consumer of the #61 import
spine, and the thing that finally types the `hdl.importer` seam."** Concretely:

1. Define the `hdl.importer` extension point (source bytes → `ImportResult` +
   report), retiring the "to be defined" in `extension-points.md:33`; register the
   Yosys importer as its first contribution and Falstad as its second.
2. Generalize `ImportSummary`/`CellViolation` into #556's four-column contract
   (construct → disposition → location → explanation) *in place*, so #556 becomes a
   refactor of shipped code with a real regression oracle instead of a greenfield
   schema. `CellViolation.sourceLocation` already carries #629 AC-4's `location` on a
   line-oriented source ("counter.v:8.3-10.6" ↔ "falstad.txt:17").
3. Map labeled nodes to `JumpStart`/`JumpEnd` with driver election, as above.
4. Emit plain-text save format and load it through the real loader; assert AC-3's
   truth with a `-t` vector file per `docs/batch-interface.md`, so the imported circuit
   is graded by the same harness instructors grade with — and the fixture doubles as
   the migration artifact CAP-29 PF-5 is separately trying to produce for `.dig`.

This keeps every outcome #631 states, deletes its hardest unstated problem, and makes
the 2–3 mw band on #561 credible rather than aspirational.

## Alternative framing (secondary, cheap, and closer to the user)

Falstad circuits do not travel as files. They travel as **links** — the circuit text is
carried in the shared URL — and secondarily as text pasted into a forum post. An
importer that accepts only a file on disk serves a gesture the population does not
perform. Accepting pasted text (and a link, by extracting/decompressing the embedded
circuit) costs a dialog and a decoder, and is what makes #553/#784's "coming from
Falstad" page able to say "paste your link." Worth confirming the current share-URL
encoding before committing, but the failure mode of ignoring it is that the importer
technically ships and is never used.

## Where it strengthens the arc

The direction is right and I would not kill it. A named-wire-aware, save-text-emitting
importer front-end pushes on `ElementRegistry`, the `hdl.importer` seam, and the batch
grading contract simultaneously — three places the project already wants pressure. Only
the seams and one acceptance criterion need to change.
