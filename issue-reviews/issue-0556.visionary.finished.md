# Issue #556: FEAT-C29-1: every importer tells its losses in one voice — the .circ report becomes a format-agnostic contract of construct, disposition, location and explanation
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the CAP-29 framing away and the claim underneath is: **JLS never loses
information silently, and when it does lose information it says so in one
vocabulary that a human and a script both read.** That is not an importer
concern. It is the single most consistently expressed value in this
repository, and #556 has found a real instance of it — then scoped the fix
to one quadrant of the surface.

## The trajectory already says this, in five places, none of which #556 cites

1. `src/jls/LoadError.java` — a four-part diagnostic record
   (`Category` / detail / location as line+element / actionable `hint`),
   a fixed taxonomy tests assert on, published through one channel so
   "every front end shows the same message" (ARCHITECTURE.md, "Error-reporting
   contracts"). #556's proposed contract — construct → disposition → location
   → explanation — is the same quadruple with the taxonomy relabelled.
2. `src/jls/hdl/HdlExporter.java:65-91` — an **enumerated disposition
   vocabulary** for every element class, up front, in four buckets:
   *Exported*, *net topology*, *warn and skip*, *reject* (with "rejection
   lists every offender in one message and nothing is written"), each bucket
   pinned by a named `HdlPolicyTest` case. This is exactly what #556 wants to
   invent, already shipped, on the other side of the boundary — but its
   machine-readable half is `record Result(String text, List<String> warnings)`
   at `HdlExporter.java:112-125`, i.e. untyped prose.
3. `src/jls/hdl/imp/ImportSummary.java` + `ImportResult.java` — the shipped
   importer's report: a category→count map plus `coercedX`. The migration
   keystone doc already indicts that counter by name
   (`docs/capability-roadmap/keystone-b-migration.md:193-198`) as "a counter
   whose only job is to report information the importer destroyed."
4. `docs/file-format.md` §9's silent-drop caveat, and the README's JLS-4.1
   `initrle` warning — the canonical silent-loss failure the whole doctrine
   exists to end, inside JLS's *own* format.
5. `docs/extension-points.md` — see below; it already has the seam.

So the loss-naming contract is not a CAP-29 asset waiting to be generalized
out of #323. It is a project-wide doctrine that has been re-implemented four
times, and #556 is proposing the fifth implementation, narrowed to
foreign-format importers that do not exist yet.

## Reframing A — the ledger is the importer's return type, so totality stops being a test

AC-2 asks that the equality `C_src \ C_out = R` be "provided by the shared
infrastructure once." Better: make it unprovable-because-unbreakable. Have
the parse stage yield a construct list, and require the mapper to return a
**total** `Map<Construct, Disposition>` whose key set the builder checks
against the parsed list before it will hand back a circuit. Then the report
is a *projection* of the importer's output, not a parallel artifact, and
"nothing dropped silently, nothing reported spuriously" holds by
construction. #323's §3 equality survives as documentation of a type
invariant rather than as a per-format assertion anyone can forget to write.
This is the same move JLS already makes with `SaveTags` (tag text never
reaches `Class.forName`), `ElementConstructorContractTest`, and the ratchet
tests: prefer a shape that cannot express the bug.

## Reframing B — widen the noun: one fidelity ledger for every boundary crossing

The report should cover **export as well as import**. An instructor bringing a
course *in* and a student taking a design *out* to Verilog face the identical
question ("what did not survive, where, and why"), and today they get a typed
`LoadError` for one, an untyped `List<String>` for another, a count map for a
third, and nothing at all for the fourth. One `jls.core` disposition record —
construct, disposition (`realized` / `approximated` / `skipped` / `rejected`),
location, explanation — consumed by `HdlExporter.Result`, `ImportSummary`, the
four CAP-29 importers, and the `.circ` importer, is barely more work than the
import-only version and retires the export-side generalization issue that will
otherwise be filed in six months. It also gives `LoadError` an obvious
sibling rather than a competitor.

## Reframing C — the seam is already catalogued, and #556 is the issue that should type it

`docs/extension-points.md` carries this row today:

| Importer | `hdl.importer` | cell-map/layout contract to be defined | `jls.hdl.imp` | many | on-command (import) | pending (#61/#62) |

The catalog's stated purpose for pending rows is verbatim: *"A seam gets its
row (and its owning issue) before its contract exists, so nobody invents a
parallel mechanism in the meantime."* #556 is currently on course to invent
exactly that parallel mechanism: #323 §3 plans "a new package under
`src/jls/imp/`", and #556 defines a cross-importer contract without touching
`hdl.importer` at all. Fix by adoption, not by addition — #556 becomes the
issue that types the pending seam (id widened from `hdl.importer` to something
format-neutral, since it will carry `.dig`/`.cv`/Falstad, and the id-stability
rule makes now the last cheap moment to rename it), with the disposition
ledger as part of the seam's contract type. AC-4 ("a written document a new
importer can adopt without reading another importer's source") is then
discharged by an existing normative document that `ExtensionPointCatalogTest`
already cross-checks in both directions, instead of by a new bespoke artifact
class nobody has a test for.

## Reframing D — the report is the exhaust; the engine is what should be shared

This is the bigger claim. Four importers sharing a report shape saves a few
hundred lines. Four importers sharing a **front end** saves thousands, and
emits the report for free. Logisim-Evolution, Digital, CircuitVerse and
Falstad all describe the same object: a hierarchical netlist of parameterized
components with coordinates and labelled nodes. What differs is the
serialization substrate and the component vocabulary — which is precisely
what CAP-29's own dedup comment (XStream XML / JSON / XML / compact text,
XXE vs JSON-bounds) enumerates, and it is the *thin* part. The thick part —
vocabulary map with semantic (not name) matching, part binding, coordinate
honouring with fallback to `jls.hdl.layout.SchematicLayouter`, circuit
construction, disposition accounting — is common to all four. If PF-1 were
"the shared import pipeline: a format-neutral construct graph, a mapper
contract, and the ledger as its projection," then PF-2/3/4's bands (4-6, 3-5,
2-3 mw) should measurably drop, and CAP-29's 13-20 mw total is the number to
re-derive. Generalizing only the report leaves the expensive duplication
untouched while creating the *appearance* that the shared-infrastructure box
has been ticked.

There is a live fork to settle inside that pipeline, and #556 is where it
surfaces: the only importer that exists builds by emitting save text and
reparsing it (`ImportResult.saveText()`), which #323 §2 alternative 2
explicitly rejects in favour of programmatic construction verbs (#337 /
`docs/operation-layer.md`). A shared pipeline forces one answer. A shared
report lets two incompatible importer families coexist under a common
letterhead — the worst of both.

## Sequencing: the ordering edge is backwards

`ordering_after: #323` derives an abstraction from a single instance that has
not been written yet, while a second, real instance sits in tree. Invert it:
define the ledger now against `ImportSummary`, `HdlExporter.Result` and
`LoadError` — three shipped shapes whose union is knowable today — and let
#323 be its first *consumer*. This is cheaper (the contract is read off
working code instead of guessed), it removes the #556/#323 merge conditional
in the sibling rule, and it makes the "does CAP-16's REPLAN absorb this?"
question moot, because a seam contract is never owned by one format's
importer.

## What I am disregarding, and why

- **AC-3 (round-trip #323's report through the shared schema with no
  information loss) should be deleted, not met.** It is a migration test for
  a migration that only exists because of the backwards ordering above. Land
  the contract first and `.circ` is born inside it; there is nothing to
  round-trip. Keeping AC-3 institutionalizes the wrong sequence.
- **AC-2 as an assertion.** Per Reframing A it should be a construction
  invariant. If it stays a test, it is a test that the type system failed.
- **The boundary note's "must not fork, re-implement, or alter `.circ`
  mapping decisions"** stays correct and load-bearing; nothing above touches
  `.circ` semantics, only where the contract is defined and how wide it is.

## What holds as written

The Outcome sentence is right and worth the mw. AC-1's "machine-readable and
human-readable, golden-tested" is right, and the golden-test discipline
matches `BatchSimulationGoldenTest`/`VcdExportGoldenTest` precedent. The
1-2 mw band is plausible for the narrow version and probably still plausible
for Reframings A-C; Reframing D is a re-plan of CAP-29's decomposition, not a
re-band of this issue, and should be raised on #513 rather than absorbed here.

## Risks of the reframing

Widening to export (B) risks pulling a shipped, tested surface
(`HdlExporter.Result.warnings`, consumed by CLI `-export`) into a
not-yet-needed refactor; mitigate by making the ledger additive — the
exporter gains a typed accessor, `warnings()` stays until a follow-up retires
it. Adopting the `hdl.importer` seam (C) couples this issue to #61/#62's
owner; that coupling is real and is the point — two importer seams is the
outcome to avoid.
