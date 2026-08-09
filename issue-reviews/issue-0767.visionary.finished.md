# Issue #767: TASK-C578-1: "course kit" becomes a written layout — labs, vectors, schedule and rubric, each part named required or optional
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the wording and the purpose is: *make course content a distributable artifact
with an identity, a version, a license and a checkable shape, so someone outside this
repository can produce one*. That goal is squarely on the project's arc. JLS's whole
distinguishing discipline is turning folklore into contracts with oracles —
`docs/batch-interface.md` ("normative, and a stability contract", pinned by
`BatchSimulationGoldenTest`/`VcdExportGoldenTest`), `docs/file-format.md` pinned by
`SaveTagsTest`/`FileFormatSpecTest`, `docs/extension-points.md` cross-checked in both
directions by `ExtensionPointCatalogTest`. Course content is the last large surface
still shipped as folklore (`examples/` contains exactly one directory, `autograde/`,
holding one Python script). Writing it down belongs here, and doing it as files with no
service is the right posture — the same one #576 states and #502 §8 exclusion 7 permits.

So: endorse the end. What follows is an argument that the *unit* the issue picks, the
*identity scheme* it invents, and the *license mechanism* it implies are each one seam
off, and that moving each seam makes most of the specification work disappear.

## Reframing 1 (headline): cut at the lab, not the kit

The issue's unit of specification is the kit — "labs + grading vectors + schedule +
rubric" — and the acceptance criteria are about naming kit parts required or optional.
But look at what is actually mechanizable. #575 AC-2 already requires, per lab, that it
"loads, simulates, and autogrades out of the box" with a CI lane grading the reference
solution green and a planted defect red. #576's workflow (per its own ORDERING
CORRECTION comment) consumes starter files and vectors. #502's four platform adapters
consume a lab. #552's lessons and #548's Examples menu are collections of the same
thing. **Every consumer in the whole education arc consumes labs; nothing but an
instructor's eye consumes the kit.** A schedule and a rubric are prose a validator can
only assert the existence of.

Cut at the lab instead:

- **A lab is the specified unit** — a directory with a starter `.jls`, exercise prose,
  a `-t` vector file, a declared chapter mapping and time budget (#575 AC-3), and the
  reference-solution / planted-defect pair that makes it self-verifying.
- **A kit is then an ordered list of labs plus instructor prose.** Its manifest is a
  table of contents and a provenance record. There is almost nothing left to specify,
  and the "required or optional" table collapses to "labs are required; everything else
  is the instructor's".

The gains are not cosmetic. A lab becomes shareable, forkable and droppable
independently — which is precisely what #575 AC-4 demands when a lab fails two
reviews and is *pulled from the pack*, and what the unresolved "flagship kit" question
(#578's comment escalates #575-vs-#577 to #517) stops depending on. #577's CSE 260M
corpus and #575's Donzellini pack stop competing to be the shape of the convention and
become two collections of the same unit. And #578's validator gets a subject with real
failure modes (a vector file that does not grade its own reference solution green) instead
of a directory-listing checker.

## Reframing 2: profiles, not per-part optionality flags

If the kit level survives at all, "each part named required or optional" is the wrong
axis. Optional parts are what make validators toothless — the verdict becomes "valid,
but the rubric is missing", which tells an instructor nothing. Define two closed
profiles instead: **lab-pack** (labs + vectors; the thing #576's workflow consumes) and
**course-kit** (a lab-pack plus schedule, rubric and instructor prose; the thing an
instructor teaches from). Each profile has only required parts, and the validator emits
one crisp verdict per profile. This also discharges AC-4 structurally rather than as a
prose paragraph: the answer to "which parts does #576's workflow consume" is "exactly
the lab-pack profile", a boundary rather than a per-file annotation that will drift the
first time either side changes.

## Reframing 3: declare contract versions, not a JLS version range

The metadata requirement "the JLS version range it targets" mints a compatibility claim
the author cannot verify and the tool cannot check — a string that goes stale on the next
release. JLS does not make per-release promises; it makes **per-contract** promises: the
`FORMAT` header with must-understand refusal (`Circuit.readFormatHeader`, `NEWER_FORMAT`,
#79), the batch-interface stability contract, and — once #502 PF-1 lands — a frozen,
versioned CLI contract. A kit should declare the contracts it needs (`format: 1`,
`batch-interface: 1`, later `cli-contract: 1`), which JLS can check and refuse by name in
the existing `LoadError` idiom. This is strictly cheaper to specify, strictly more useful,
and it means a kit authored in 2026 keeps working in 2031 without anyone editing a range.

## Reframing 4: do not mint a second identity scheme

"Kit identity and version" is being invented in `area:distribution` with no reference to
#340 (FEAT-016), which is already building a four-field vendor/library/name/version
identifier plus a provenance record plus a stated policy on version comparability
(its Open Question 2) plus an invariant that absent identity is never given invented
values. #340 is required by #300 — the same capstone this whole education arc sits under.
Two identity vocabularies in one project, one for circuit libraries and one for course
kits, is a defect being designed in at 0.5 mw and paid for forever. Reuse #340's tuple
and provenance record verbatim for kit identity (not its binary container — kits are
markdown and `.jls` files, and the section-framing machinery has nothing to say about
them). Same for #755: the distribution/submission layouts and this kit layout must be one
document with one vocabulary, or an instructor will meet two directory conventions on
their first afternoon.

## Reframing 5: license via REUSE, not a metadata field

`license: CC-BY-4.0` in a manifest is an unenforceable assertion whose scope is ambiguous
— does it cover the starter circuits, the vectors, prose derived from a textbook's problem
set? The project has already researched the answer: landscape entry 171 (SPDX/REUSE) is
worked up in `docs/standards-adoption/09-cra-and-supply-chain.md`, which records that
zero of 886 tracked files carry an SPDX identifier and that REUSE conformance has an
objective definition — "`reuse lint` exits 0". Per-file `SPDX-License-Identifier` headers
with a `LICENSES/` directory give #575 AC-5's "clean and auditable provenance" and
#578 AC-4's content-license-distinct-from-code-license a machine oracle for free, and
#578's validator can shell out to `reuse lint` rather than checking a string field. Kit
content is also the ideal pilot for REUSE, since it is new files with no retrofit cost.

## Ordering: the derivation constraint is right, the sequencing makes it expensive

AC-3 ("derived from the worked pack rather than invented ahead of it") is good instinct
and I would keep it. But `ordering_after: [575, 576]` means a 0.5–1 mw specification waits
on a 3–5 mw content feature plus a whole workflow feature — so eight labs get authored
against no convention and are then retrofitted to one. Two further problems: #576's own
ordering was corrected on 2026-08-08 to `ordering_after: []` with its layout task (#755)
"ready now", so ordering behind all of #576 is already stale; and a convention derived from
exactly *one* instance is shaped like that instance, which matters while #517 has not
settled whether #575 or #577 is the flagship. Gate on **the first two labs of #575 plus a
sketch of #577's adapted kit**, and mark any schema element exercised by only one kit
`provisional` rather than `optional` — a third state the issue's binary does not have.

## What I would keep unchanged

The insistence that the spec live in tree; that gaps be "resolved in the spec, not
waived"; the boundary against #502, #300 and #552; and the no-service posture. Two
additions in the project's own house style: the document joins README's normative
Documentation list beside `batch-interface.md` and `file-format.md`, and the schema has
exactly one machine-readable source cross-checked against the prose in **both**
directions by a test, as `ExtensionPointCatalogTest` does for the seam catalog. A spec
whose only oracle is a validator filed under a different issue (#578 AC-2) is the one
anti-pattern this project has otherwise eliminated everywhere.
