# Issue #761: TASK-C577-1: the CSE 260M corpus lands in tree as committed compatibility fixtures with recorded provenance
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "files in a directory." The end is that JLS can **mechanically answer an external
party's question about its own maturity**. #509's payload is an instructor's undefined
phrase ("well enough matured"); the round-2 ordering correction on #577/#509 already
concluded that the corpus should come *first* and that the criteria conversation should be
conducted against a demonstrated artifact. That is exactly right, and it means the artifact
this task ships is not a fixture set — it is **the evidence instrument the conversation runs
on**. Everything below follows from taking that seriously.

The direction is squarely with the project's arc, not against it. `docs/grand-architecture.md`
§1 names the headless batch/grading surface as *co-equal* with the GUI, and
`docs/batch-interface.md` is already a declared stability contract. Every fixture the repo
has today is either self-authored or one interop file (`fork-4.6-shiftregister.jls`, built
from `bsiever/JLS@038a5b67` — the same fork). An externally-sourced corpus is the first
evidence that the contract holds against material the project did not write. That is a real
strengthening of the arc.

## The precedent that should alarm us

`test/fixtures/legacy-4.1/` already is this issue, one lineage earlier: a designated corpus
home, a provenance README, an acquisition plan. It has been **empty since #56**, because
acquisition was deferred to "the maintainer, with unblocked egress." #761 as written has the
same shape and the same failure mode, and its premise is thinner than it looks: the #577
ordering comment asserts the corpus is what "the Spring 2025 course site publishes and
bsiever/JLS releases distribute" — but releases distribute *binaries*, and a course's
publishable material is starter circuits, while the artifacts that actually pin grading
behaviour are instructor solutions, which courses do not publish. The realistic outcome of
executing AC-1 literally is a second empty directory with a README explaining why.

## Reframing 1 — the unit of the corpus is a conformance case, not a circuit

A bare `.jls` file tests exactly one thing: that the loader accepts it. But #509's AC-3 and
#763's lane both say **"load + simulate + grade."** Grading requires a triple, and the repo's
own batch interface already defines it: circuit + `-t` vectors + watched-output stdout. If
this task lands bare circuits, #763 inherits a corpus with no oracle and must either fabricate
vectors (inventing course intent) or silently degrade to a load-only smoke lane — the precise
vacuity #763's AC-2 exists to prevent, arriving through the front door.

Worse, #763's "grades differently **than on the origin fork**" implies running bsiever's jar,
which nothing in the C577 tree scopes. The elegant escape is the idiom this repo already uses
everywhere (`BatchSimulationGoldenTest`, `VcdExportGoldenTest`, release `.buildinfo`): capture
the baseline **once**, commit it, and make the lane a diff.

```
test/conformance/cse260m/<case>/
  circuit.jls        # origin bytes, verbatim (.gitattributes already pins -text)
  vectors.t          # the -t file, from the course or reconstructed-and-marked-as-such
  origin.out         # stdout captured from the ORIGIN FORK's jar, once
  ORIGIN.yaml        # source URL, upstream rev/date, permission, sha256s,
                     # origin-jar release tag + sha256, disposition
```

Now the origin-fork baseline is a committed artifact with its own provenance, the lane needs
no second JVM and no network, and a divergence is a one-line diff naming the case. #509's AC-3
becomes literally checkable rather than aspirational.

## Reframing 2 — the deliverable is a manifest mechanism, and by-reference entries are legal

Per-file provenance as prose does not survive N files; the repo's current idiom is a javadoc
paragraph and a README, which is fine for two fixtures and rots at twenty. Ship instead a
single machine-checked `MANIFEST` per corpus plus one test asserting **bijection** (every
manifest row has a file, every file has a row) and **hash match**. That one decision hands
#763 its census AC for free — "a shrinking corpus fails rather than passes" is a manifest
property, not a separate CI feature — and it retrofits onto `legacy-4.1/` and the `riscv/`
ELFs so the project has one corpus mechanism rather than three conventions.

Critically, let a manifest row be **present-by-reference**: origin URL + sha256 + permission
state + `vendored: false`. The lane runs what is present and reports referenced-only cases as
skipped-with-reason. This is the move that makes the licensing problem *disappear as a
blocker*: the task lands complete and honest even if zero files can be redistributed today,
and a later permission grant is a data change, not a code change. It also gives the #509
conversation a far better opening than a request — a table of exactly which artifacts we
cannot legally hold, which is a question Dr. Siever can answer in one email.

I am explicitly **disregarding AC-3 as written**: there is no "repo's large-fixture policy"
anywhere in tree (nor a "CI lane-budget policy" for #763), so the criterion is unsatisfiable
by reference. Do not write a policy document to satisfy it. Record `bytes` in the manifest and
assert a ceiling in the same manifest test — one assertion, and `.jls` files are XZ text, so
a semester of labs is plausibly under a megabyte against today's 144K of fixtures.

## Reframing 3 — this is conformance, not a kit; the naming tension dissolves

#577's dedup comment records an unresolved question: two candidates for "the flagship kit"
(#575's Donzellini pack vs. this corpus). They are not competing kits; they are different
*roles*. The Donzellini pack is an **exemplar** — ours, authored, pedagogical. CSE 260M is a
**conformance baseline** — external, unowned, adversarial to us by design, valuable precisely
because it exists to catch us being wrong. Put it under `test/conformance/`, never under kit
vocabulary, and #761's AC-2 ("compatibility fixtures only; no adapted material") stops being a
promise and becomes a structural property of where the files live. #765 then adapts and ships
separately without ever reusing this tree.

## The larger goal this task should be the first cell of

The real prize is a **JLS Conformance Suite**: a versioned, publicly re-runnable set of
conformance cases that *any* JLS fork can run to prove interchange — bsiever's, GVSU's
`JLSCircuitTester` lineage (already named as the fallback source in `legacy-4.1/README.md`),
and whatever comes next. The project already speaks this language: `docs/standards-adoption/`
describes the RISC-V ACT as "a finite corpus, not a coverage argument," which is the same
epistemics. That reframing inverts the adoption ask entirely — instead of the project
petitioning an instructor to judge it mature, it publishes an instrument that answers the
question, and #509 AC-1 becomes "here is the conformance report on your own labs." Same cost,
strictly better goal, and it turns a one-off favour into infrastructure.

## Alternative considered and declined, so a later pass does not re-derive it

**Don't vendor; pin URLs and fetch in CI.** Rejected. It contradicts the self-contained,
network-free ethos stated in `grand-architecture.md` §1 and the reproducibility posture in
README; and `legacy-4.1/README.md` records the concrete failure — an egress-blocked
environment made the upstream source unreachable and the corpus never materialised. Vendor
what may be vendored; reference the rest in the manifest.

## Concrete revision of the acceptance criteria

1. A corpus manifest format lands with a bijection+hash test; `legacy-4.1/` is migrated onto
   it as the second consumer, proving it generalises.
2. Every CSE 260M artifact appears as a manifest row with origin, revision-or-date, permission,
   sha256, size, and `vendored: true|false`. Rows that cannot be vendored are recorded, not
   omitted — omission is the failure this task exists to prevent.
3. The corpus unit is a conformance case directory (circuit + vectors + origin-fork stdout),
   with the origin jar's tag and sha256 recorded; cases lacking vectors are rows marked
   `load-only` rather than untested files.
4. The tree lives under conformance vocabulary, not kit vocabulary; #509's relationship note
   goes in the corpus README as AC-4 already requires.
5. Total size is a manifest field with an asserted ceiling. No policy document.

## Bottom line

Endorse the goal and the corrected sequencing without reservation — this is the highest-leverage
half-milliwatt in the tracker. But ship the *mechanism* (manifest, by-reference rows,
conformance-case unit, committed origin-fork goldens), with CSE 260M as its first instance.
Ship the directory alone and the most likely 2027 state of this repository is
`test/fixtures/cse260m/README.md` explaining, in careful prose, why it is empty.
