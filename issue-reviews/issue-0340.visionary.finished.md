# Issue #340: FEAT-016: a subcircuit definition has an identity that means something outside the file it lives in, and a library of circuits is a distributable artifact with provenance
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machinery and three things are wanted: (a) a design can *point at*
another design instead of swallowing it, (b) an instructor can hand out a
bundle of circuits that stays identifiable after it is copied, (c) two things
that are the same block can be recognized as the same block. (a) and (b) are
real, near, and serve every trajectory in `docs/grand-architecture.md` §2
(CPU labs, FPGA bridge, collaboration). (c) is the interesting one, and this
issue's own § 1 hands its expensive half to #357.

The problem: the issue's route reaches (c)-flavoured machinery first — a
bespoke projection digest and a four-field vendor/library/name/version
identifier — and reaches (a), the load-bearing capability, third, as the one
row that is unfiled, unowned, and explicitly at risk of being descoped
(comment 5227041254's escalation). The plan's ordering is inverse to the
plan's value.

## Reframing 1 — the digest already exists and is better than the one proposed

`src/jls/Circuit.java:1548` `public String stateHash()` returns the SHA-256 of
the canonical save text, derived-never-stored, headless, pinned by
`test/jls/DeterministicSaveTest#stateHashIsContentDetermined`. It exists
because #163 needed a convergence oracle. It is the digest criterion 7 needs.

The issue rejects hashing bytes ("the digest input must be π(D) … and **not**
the saved bytes"), but that objection was written against a world before #166
and #165 landed. Canonical serialization plus stable element identity is
exactly what makes the bytes content-determined; `Circuit.save` at
`:1483-1500` sorts by stable id and rewrites file-local ids from that order
for precisely this reason. The remaining difference — π excludes coordinates
and annotations — buys one thing: the same definition drawn at two positions
digests equal. That property has exactly one consumer, and it is **N intra-file
instances**, which § 1 assigns to #357 by name.

For a cross-file reference, the referenced file's coordinates are not a
variable. So: **use `stateHash()` for the reference-and-recorded-digest
record, and let #357 mint a projection if and when it needs one.** This deletes
Open Question 3 — one of the two questions currently blocking TASK-0039 from
being filed — and it preserves something a bespoke projection destroys: an
instructor can `sha256sum` a plain-text save and get the number JLS records.
Given this project's entire provenance culture (SHA256SUMS, attestations,
`docs/reproducibility.md`, "verify rather than trust"), an identity no external
tool can compute is a step away from the arc, not toward it.

## Reframing 2 — VLNV is an ecosystem's identity, and JLS has ruled on ecosystems

`docs/standards-adoption/08-ipxact-export.md` finding 1 is in-tree and blunt:
"There is no VLNV, and three of its four fields must be invented." The whole
document is held behind a demand gate, in the same posture as #212. This issue
takes the finding, adopts the identifier anyway, and then writes Global
Invariant 4 forbidding invented values. Compose the two: in every real file
every field is ⊥, so the collision predicate `v(D₁)=v(D₂) ∧ v≠⊥ ∧ δ(D₁)≠δ(D₂)`
never fires. Open Question 1 — called "the real design decision in the feature"
and a filing blocker — governs a state no user can reach until a naming
authority exists that nothing in the plan creates.

Alternative: **identity = (resolution source, content digest)**. A reference
records where it resolved from and what it hashed to; that is enough for every
criterion in § 1 except 3 and 4, and criteria 3 and 4 exist only to support
names nobody issues. If a course exchange or registry ever appears, names are
three fields of optional state and a round-trip test — the issue's own § 2 says
so. Build them then, on demand, per §9 of the grand architecture.

## Reframing 3 — a library is not a new file kind

`FileAbstractor.readZip` (`src/jls/FileAbstractor.java:295`) already opens a zip
whose single entry is named `JLSCircuit`. A library is that container with N
entries plus a `MANIFEST` entry mapping entry name → digest → provenance. The
zip central directory is self-describing, so an older reader that meets an entry
kind it does not know can skip it by name — which is the entire service
`blocked_by: [319]` is being purchased for. Cutting the library along the
container seam instead of the record-framing seam **removes a whole feature from
this one's critical path**, reuses a reader that ships and is already hardened
against hostile input (`UntrustedFileHardeningTest`), and makes the artifact
verifiable with `unzip -l` and `sha256sum`. Even simpler first cut: a library is
a *directory plus a manifest*, and zipping it is packaging, not semantics.

## Reframing 4 — one resolution seam, not two, and the accounting argument

§ Related issues declines to give import-to-subcircuit to #357 on three grounds;
the third is cost visibility — that #357's band would hide the row. That is a
bookkeeping reason deciding an architectural placement, and it should be named
as one. The technical consequence of the split is two resolvers: #357 builds an
elaborator resolving an instance against an in-file definition table, this issue
builds a resolver walking to another file, and they will land months apart with
different diagnostics for the same user-visible failure ("I cannot find that
definition").

Concrete alternative: publish **one seam** — a `DefinitionSource` contract with a
stated resolution order, catalogued as a `pending` row in
`docs/extension-points.md` (`elem.definition-source`, home `jls.elem`, owner
#340) exactly as that document requires seams to be named before their contract
exists. Sources: the current circuit's definition table (#357), a sibling file,
a library archive (TASK-0040). Then Open Question 5(a) is not a policy invented
here; it is the seam's ordering rule, written once, and #357 contributes to it
rather than duplicating it.

## What the issue does not consider, and should

- **Undo and checkpoints re-enter the loader.** `CircuitSnapshot` stores the
  circuit as save-format text and restores through the ordinary load path
  (ARCHITECTURE, "The save/load pipeline"). If a saved reference resolves from
  disk, every undo re-resolves from disk, and a referenced file edited mid-session
  changes what undo restores. Resolution must therefore be a load *phase* with an
  explicit context — visited set, search path, resolved-content cache — not a
  recursion guard bolted into `Circuit.load`. That framing also gives Invariant 7
  (acyclicity) for free instead of as a separate 100-150 line row.
- **Collaboration's oracle weakens.** #163's convergence indicator is
  `stateHash()` over the canonical text. With by-reference nesting, two peers can
  hold byte-identical circuits and simulate different designs, because peer B may
  not have `soc.jls` at all. Recording the digest in the parent (which criterion 7
  does) detects it; nothing in the issue says what collab *does* about it. Name it.
- **Autograding assumes one file.** The README's container recipe is
  `docker run -v "$PWD:/work" ghcr.io/anadon/jls -b -t tests circuit.jls`. A
  design that references a sibling stops being a submittable unit.

That last point suggests the most elegant move available here: **keep inlining as
the distribution form and add references as the authoring form**, joined by a
`jls --bundle out.jls design.jls` verb that flattens references back into a
self-contained file. JLS already writes the flattened form perfectly
(`SubCircuit.save` → `getSubCircuit().save(output)`); no new format is needed for
it. With bundling in hand, Open Question 5(a)'s "how do we make a failure
reproducible on another machine" has a one-command answer instead of a search-path
specification, and the handout use case is served by a *verb*, not a container.

## Disregarding part of the stated criteria

I am setting aside acceptance criteria 3 and 4 (the four-field identifier and the
collision policy) and Open Questions 1 and 3 as things to decide *now*. They are
the two blockers preventing TASK-0039 from being filed, and they are blocking on
an ecosystem the project has twice recorded a decision not to build ahead of
demand. Criteria 1, 2, 5, 6 and 7 survive intact under `stateHash()`.

## The order I would ship in

1. By-reference nesting on a path plus `stateHash()`, with the three refusals
   (missing, mismatch, cycle) and a resolution phase in the loader. This is
   criterion 7, IC-7, IC-8, and Invariants 7-8 — the whole of D15's second
   sentence — and it needs no new identity design at all.
2. `--bundle` (flatten to self-contained). Cheap, and it retires the entire class
   of "the referenced file is not on this machine" failures for grading and
   distribution.
3. Library as zip-plus-manifest, sources unified behind one resolution seam.
4. Names (VLNV) and the projection digest, when a consumer exists — #357, or a
   real course exchange.

## Alignment

The goal strengthens the arc: files that compose are prerequisite to the CPU
trajectory (`riscv/`), to hierarchy-preserving HDL import, and to shipping course
material. The route as written pulls against two recorded stances — no ecosystem
surface ahead of demand, and identity that outside tools can verify — and defers
the one row that delivers the capability. Reframe the identity down to
(source, `stateHash`), reframe the container onto the zip reader that ships, and
this feature gets smaller, lands sooner, and loses nothing a real user asked for.

## Citations

- `/home/user/JLS/src/jls/Circuit.java:1548` (`stateHash`), `:1466-1500` (canonical save)
- `/home/user/JLS/src/jls/elem/SubCircuit.java:282-289` (inline save)
- `/home/user/JLS/src/jls/edit/SimpleEditor.java:679` (`finishImport`), `:5463` (`doImport`)
- `/home/user/JLS/src/jls/FileAbstractor.java:295` (`readZip`)
- `/home/user/JLS/docs/standards-adoption/08-ipxact-export.md` findings 1-2 and the demand gate
- `/home/user/JLS/docs/extension-points.md` (seams are named before contracts exist)
- `/home/user/JLS/docs/grand-architecture.md` §2, §9
- Note: the D15 ruling is cited from `docs/plan/evidence/BRIEF.md`, which does not
  exist in this checkout — the DoD requires every cited document to resolve at close.
