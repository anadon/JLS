# CAP-16 - Logisim-Evolution migration parity

**Status:** proposed | **Priority:** 9 | **Marginal cost:** 8-16 mw |
**Standalone cost:** 10-20 mw

## Outcome

A Logisim-Evolution `.circ` file opens in JLS as a working, readable circuit,
and everything that did not survive the crossing is named in a report - so an
instructor with a decade of course material can move it instead of rewriting it.

## Acceptance test

**What "parity" means here.** This is the one capstone that moves **users**
rather than files, so parity is not a round trip and not feature equality with
Logisim. It is three checkable claims:

1. **Opens, and lies about nothing.** A `.circ` from a public corpus loads
   without a crash, and every construct that was dropped, approximated or
   renamed appears in a migration report. A silent drop is a failure of this
   capstone even when the circuit still simulates.
2. **Behaves.** For a design that ships Logisim test vectors, the imported JLS
   circuit produces identical outputs on those same vectors. This is the parity
   assertion, and it is checkable precisely because the source tool has a
   test-vector format of its own.
3. **Reads.** The result is a schematic a person can open, understand and edit -
   laid out, not a pile at the origin - because a migrated circuit nobody can
   read has not migrated anything.

SEEN: an instructor opens last term's lab `.circ` in JLS, sees their circuit
laid out roughly as they drew it, runs the lab's own test vectors, and reads a
one-page report naming the three constructs that changed.

CHECK: five named tests.
- `CircImportCorpusTest` - the accept/reject table run over a corpus of public
  `.circ` files, reporting per-construct coverage as data.
- `CircMigrationReportTest` - claim 1. A fixture carrying an unmappable
  construct asserts a named report line rather than a quiet load; nothing is
  dropped without a diagnostic.
- `CircVectorParityTest` - claim 2. The source tool's test-vector file replayed
  against the imported circuit, outputs compared exactly.
- `ShiftRegisterCollisionTest` - the named trap: the source tool's shift
  register is sequential and JLS's is a combinational barrel shift despite the
  name, so mapping by name must be a **loud reject**, asserted by a test.
- `CircXxeHardeningTest` - one test per XXE vector, because a `.circ` is
  untrusted input and this would be the first XML parse in shipped code.

## Demo slice

Run the accept/reject table over a corpus of public `.circ` files and publish
the per-construct coverage - **about two days**, before anything is scoped,
because it could reorder every increment that follows and because the cost band
below is a re-derivation rather than a measurement. Then increment 1: the
structural subset (gates, wires, pins, splitters, subcircuits) with the
migration report and JLS-generated layout. **3-5 mw**, and it establishes claims
1 and 3 on real files from real courses.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-025 | Logisim-Evolution `.circ` importer and migration report | the reader, the construct map and the report - this capstone's spine | required |
| FEAT-002 | Fail-loud loader and attribute dispatch | claim 1 is exactly this discipline applied to a foreign format; an importer built on a loader that discards unknown attributes silently cannot make the claim | required |
| FEAT-022 | Schematic auto-layout for imported netlists | claim 3; an imported circuit needs a readable arrangement whether or not source coordinates survive | required |
| FEAT-053 | Test-vector front end and autograding at scale | claim 2 is a vector replay, and the batch engine it replays through is this feature | required |
| FEAT-001 | Registry-keyed table totality discipline | a construct map is a registry-keyed table; a non-total one is how constructs get dropped silently | required |
| FEAT-016 | Subcircuit type identity, VLNV and the circuit-library format | `.circ` files carry named, reused subcircuits, and a migrated library needs identity to stay a library | required |
| FEAT-013 | Per-section internal versioning with must-understand semantics | the migration report and provenance ride as sections an older reader can skip | beneficial |
| FEAT-003 | Uncompressed canonical default with stable-id references | a migrated course lands in version control, and an unreviewable diff makes the migration a one-way door | beneficial |
| FEAT-040 | The package and pinout library as data | the source tool ships through-hole part data that is absorbable with attribution, and this is the cheapest route to it | beneficial |
| FEAT-011 | Accessibility, keyboard operability and onboarding | a migrating user arrives fluent in another editor and lands in an unfamiliar one; onboarding is part of whether the migration succeeds | beneficial |
| FEAT-008 | `SimpleEditor` decomposition, a UI harness and a floored `jls.edit` | claim 3 is asserted about the editor, and the editor has no test harness to assert it with | beneficial |
| FEAT-050 | Module runtime consumed: extension points and providers | a construct the importer will never map is exactly what an external element provider is for | beneficial |

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) CAP-16 has no tracking issue, and neither does FEAT-025 - the entire migration path is untracked | no issue |
| #62 | HDL Stage 2 companion: schematic auto-layout for imported netlists | overlaps - the same layout consumed by a second importer; **appears substantially shipped at HEAD**, see Evidence |
| #214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | depends on - claim 2 replays vectors through that engine |
| #84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | depends on - claim 3 cannot be asserted about an editor with no harness |
| #73 | First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots | overlaps - a migrating instructor is a first-run user with strong prior habits |
| #212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` atop the #78 registry | overlaps - the escape hatch for constructs JLS will not absorb |
| #78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the construct map keys off the registry its shipped half provides |

## Open decisions

1. **Absorb the source tool's port geometry, or re-derive it?**
   *Recommendation: absorb, and record the license consequence.* Reason:
   connectivity in `.circ` is purely geometric and depends on per-component port
   offset rules with hard-coded special cases; clean-room reimplementation from
   readable source is both a worse legal position and far more error-prone in
   exactly those cases. The cost is that the upstream notice says "GPLv3" with
   no "or later", so absorbing it silently costs JLS its own "or later" unless
   upstream is asked first.
2. **Ask upstream whether "GPLv3" means "or later"?** *Recommendation: yes, and
   before any absorption.* Reason: the wording is ambiguous enough that the
   answer could be yes, and the question costs one message against a license
   change that is otherwise permanent.
3. **Does JLS commit to coordinate preservation?** *Recommendation: not in
   increment 1 - generate layout, then add coordinate preservation as its own
   increment.* Reason: it is separable, it is what makes the result look like the
   author drew it, and gating increment 1 on it delays every other claim.
4. **Is the migration one-way?** *Recommendation: yes, stated plainly, with no
   `.circ` writer.* Reason: this capstone moves users; a writer would commit JLS
   to tracking a format it does not control in both directions forever, for the
   benefit of leaving.
5. **Does a second migration source follow?** *Recommendation: decide only after
   the corpus measurement, and reuse the scaffolding rather than re-minting an
   importer.* Reason: a second source rides the same reader shape at a fraction
   of the cost, but only once the first one's real cost is known.
6. **Where does the migration report live?** *Recommendation: a machine-readable
   artifact next to the imported file, not a modal dialog.* Reason: an instructor
   migrating a course migrates dozens of files in a batch, and a dialog per file
   is not a migration path.

## Kill criteria

- K1. If the two-day corpus measurement shows that structure-complete import
  requires construct coverage far beyond the increment-1 subset for a majority
  of real course files, the cost band below is wrong by construction and the
  capstone must be re-costed before any weeks are committed.
- K2. If any circuit in the corpus imports **silently disconnected** - geometry
  replicated wrongly, connectivity lost, no diagnostic - stop. That is claim 1
  failing in the one mode a user cannot detect, and it is worse than refusing to
  import the file at all.
- K3. If a construct-name collision of the shift-register kind is found to map
  by name anywhere in the importer, treat it as a correctness defect, not a
  polish item: it produces a circuit that loads, simulates, and is wrong.
- K4. If any XXE vector lacks a covering test, do not ship the reader. A
  circuit file that can reach the filesystem breaks the premise the whole
  project rests on, and this is the first place shipped code parses XML.
- K5. If upstream's license answer is "GPL-3.0-only" and the maintainer is
  unwilling to give up "or later", the geometry-absorption route is closed and
  the capstone must be re-costed on the re-derivation route, which is both more
  expensive and more defect-prone - not quietly attempted at the absorbed price.

## Evidence

- Verified at `b54e6ee`: `grep -rli logisim src/` returns **0**; nothing on this
  path exists at HEAD. There is no XML parsing in shipped code either -
  `grep -rl "javax.xml\|DocumentBuilder\|XMLStreamReader\|org.w3c.dom" src/`
  returns **0** - so the reader would introduce the first one.
- The importer precedent the band is derived from:
  `src/jls/hdl/imp/NetlistImporter.java` (1,067 lines) with
  `ImportResult.java` (52), `ImportSummary.java` (102) and
  `ImportException.java` (32) - a reader, a result, a summary and a typed
  failure, which is the exact shape a second importer needs.
- Layout appears shipped and reusable:
  `src/jls/hdl/layout/HeuristicLayeredLayouter.java` (553 lines) with
  `LayoutGraph`, `LayoutMetrics` and `LayoutInvariants`, wired at
  `NetlistImporter.java:104`. Confirm before funding FEAT-022 for this capstone.
- The name-collision trap is in shipped code and documented:
  `src/jls/hdl/HdlExporter.java:83-84` notes that JLS's shift register holds no state
  despite its name (issue #122).
- **Cost-band caveat, stated because it is load-bearing.** The 8-16 mw band is a
  re-derivation from the shipped importer's line count. `09-format-adoption-plan.md`
  W8.1 prices the same importer at 12-18 weeks and §11 records that this figure
  is "an analogy, not a measurement" and that the corpus measurement has not been
  done. **Treat the higher band until the two-day measurement in the demo slice
  is run.** This is the largest disagreement between two evidence documents in
  this capstone's inputs.
- The four named risks - geometric connectivity, the name collision, XXE, and
  the license - are `09-format-adoption-plan.md` Wave 8 and §8 hazard 2.
- Do not restate: `docs/file-format.md` owns the `.jls` container and its
  versioning, `docs/simulation-semantics.md` owns what a circuit means once it
  is imported, `CONTRIBUTING.md` owns the coverage and mutation bars every new
  package inherits, `docs/capability-roadmap/` owns program boundaries.
- **Cost reconciliation.** Marginal band 8-16 mw. Its 6 required features
  sum to 24-44 mw and its 6 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
