# Issue #883: TASK-C880-1: the 30-submission corpus exists with its planted pairs declared and its independent solutions generated, not mutated
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its tier vocabulary, #883 buys one thing: the right to answer KC-25-1 on #506
cheaply, before 14–21 mw is spent on a schematic-MOSS. The gate is well conceived — #880 is
the rare feature filed to make its own capstone killable — and #883 correctly puts the
apparatus first, because a carelessly built null side makes every later number meaningless.
I endorse the goal and the ordering. My objections are all about the *shape of the artifact*,
not the intent.

The reframing in one sentence: **the deliverable should be a committed generator that emits
the corpus, not 30 hand-authored files with a prose affidavit of independence** — and the
corpus it emits is a project asset (structurally diverse, provably equivalent circuits) that
survives even if KC-25-1 fires and CAP-25 dies tomorrow.

## The load-bearing problem: the corpus author controls the answer

AC-2 is the honest heart of the issue and it is also where the design quietly breaks. The
measurement asks "do independent correct solutions score below planted copies?" But the
independent population is *authored by the same person who wants an answer*, along axes that
same person chooses. Pick orthogonal axes (NAND-only vs. mixed, ripple vs. tree, flat vs.
sliced) and separation is guaranteed; pick timid ones and it is impossible. The prohibition
on mutating a sibling file blocks the crudest failure, but it does not touch this one: a
single author producing 27 "independent" solutions in sequence has one common ancestor —
their own head — and no manifest can attest otherwise.

The remedy is not more rigor in prose. It is three cheap mechanisms:

1. **Pre-register the corpus design.** Commit the assignment, the enumeration axes and the
   count *before* any score exists, in the same commit as the fixtures, with an explicit
   statement that the axis set is frozen. #880 AC-4 already insists the null result be a
   legible pass; pre-registration is what makes that promise structural rather than a
   promise. This costs a paragraph and removes the single biggest way the gate gets gamed.
2. **Separate authorship from scoring.** AC-5 half-does this (no scoring code embeds the
   pair list). Extend it: #884 must be implementable against the manifest schema alone,
   with the corpus swappable. Then a second, differently authored corpus can be dropped in
   later and the measurement re-run — which is the only real defence against a tuned corpus.
3. **Report an author-time degeneracy number, not a judgement.** See below.

## Concrete alternative 1: the degeneracy proxy, available before #356 lands

#880 is `ordering_after: [356]`, and #356's canonical form does not exist — it is itself
blocked on #334 and #319, whose tasks (TASK-0005/0031/0032) are unfiled; `src/jls/diff` and
`src/jls/merge` are absent from the tree. So #883 is authoring fixtures *against a
discriminator nobody can run*, and "structurally different" is being judged by eye.

There is a free oracle available today. Every JLS circuit already yields a cheap structural
signature with no canonicalizer at all: the multiset of element types plus the degree
sequence of the netlist. Computing the pairwise distribution of that trivial proxy over the
corpus, at corpus-authoring time, converts #880's KC-25-0-1 from a judgement call into a
number the task itself can report:

- If the independent pairs already separate from planted pairs under a signature this dumb,
  the corpus is too easy and the later measurement is vacuous — report it and widen the axes.
- If every independent solution collapses to one signature, the corpus is degenerate in the
  other direction — the assignment is too small, which is exactly the finding KC-25-0-1 asks
  for, obtained one task earlier and for near-zero cost.

This is a strict addition to #883's scope of maybe a hundred lines, and it is the only way
the task can discharge its own AC-1 reasoning ("small enough that 27 genuinely exist, large
enough that they are not all the same drawing") with evidence rather than assertion.

## Concrete alternative 2: emit the corpus, do not draw it

`src/jls/hdl/imp/NetlistImporter.java` already builds JLS circuits programmatically from
Yosys JSON, and `docs/file-format.md` is a normative grammar. The project can therefore
*generate* circuits; nothing requires 30 hand-drawn files. A committed generator makes AC-2
literal instead of aspirational — the "stated enumeration procedure" becomes an executable
procedure whose output is reproducible at a commit, and independence is a property a reader
can re-derive by reading a hundred lines rather than trusting a paragraph.

Two cautions I would write into the issue rather than discover later:

- **Synthesis is a tempting and wrong source of "independence."** Piping several Verilog
  descriptions through Yosys and importing them would look rigorous, but synthesised
  netlists are a different population from student drawings: `opt`/`abc` normalises toward a
  common form, so a null distribution built that way understates the diversity of real
  submissions and would make separation look easier than it is. Use the importer as a
  cross-check on structural diversity, never as the corpus source.
- **A generator invites exactly the antipattern #880 KC-25-0-1 names** — turning the dial to
  300 to rescue a weak result. Pin the scale in the manifest and require a REPLAN to change
  it; the generator's parameterisation is the temptation and must be fenced explicitly.

## Concrete alternative 3: pick the assignment by exhaustive verifiability, not by "small"

Open Question 1 asks 4-bit comparator vs. 2-bit ALU slice. The better selection criterion is
not size but **whether the input space can be exhaustively enumerated by the shipped batch
runner**. A 4-bit comparator is 8 inputs, 256 vectors — `jls -b -t` can prove any two
submissions equivalent, not merely agree on a sample. Then:

- AC-4's "functionally equivalent to its source" stops being a spot check and becomes a
  proof, at no extra cost.
- The independent solutions get the same treatment, closing the gap AC-2 leaves open: it
  never says the independent solutions must be *correct*, only independent. Under exhaustive
  vectors, "correct" and "equivalent to the reference" are the same mechanical check.

State the criterion as "exhaustively verifiable input space" and Open Question 1 answers
itself, durably, for whoever later grows the corpus.

## Concrete alternative 4: commit the corpus as plain text

AC-5 wants a reader to "audit the corpus's honesty without running anything." Default JLS
saves are XZ blobs (`ý7zXZ` magic) — 30 of them in `test/fixtures/` are 30 opaque binaries in
a tree that today holds exactly three `.jls` fixtures. `-savetext` (#129) exists precisely
for version-control legibility. Commit the corpus as `FORMAT 1` plain text; the diffs become
reviewable, the planted transforms become visible in review, and #717's "large-fixture
policy" concern mostly evaporates. This should be an acceptance criterion, not a preference.

## Where this pulls against the project's arc

**Duplication with #717.** #717 (TASK-C531-1) needs 300 committed submissions with recorded
provenance, documented composition and a regeneration command. #883 needs 30 with declared
provenance and a stated generation procedure. #883's Boundary correctly refuses to *merge the
artifacts* — different questions, different manifests — but both issues are independently
inventing corpus provenance, manifest schema, regeneration determinism and fixture-size
policy. The seam to cut along is the **generator and manifest schema**, shared; the corpora,
separate. Neither issue mentions the other's apparatus. If #883 lands first and defines a
manifest schema #717 can adopt, one artifact class serves both plus PF-5's later scale-up.

**A structurally diverse, provably equivalent circuit corpus is worth more than this gate.**
Framed as "the plagiarism fixture," the corpus is single-use and dies with CAP-25. Framed as
"N behaviourally identical, structurally distinct implementations of one specification with a
machine-checked equivalence relation," the same 30 files are regression material for #356's
canonical form (the canonicalizer's job is precisely to see through this variation), for HDL
export/import round-trip validation (#33/#59), for mutation testing, and for CAP-06 grading.
I would retitle the outcome accordingly and let the similarity measurement be its first
consumer. The cost is identical; the survival odds are not.

## Two internal tensions worth resolving at filing time

- **AC-2 vs. AC-3 on subcircuits.** AC-2 names "different subcircuit boundaries" as an
  independence axis; AC-3 excludes subcircuit repackaging from the planted transforms because
  its behaviour depends on PF-1's undecided flattening policy. So the null side leans on
  exactly the structure whose canonical treatment is unpinned — a chunk of the null
  distribution's spread is undefined by construction. Either pin a flattening assumption in
  the manifest, or drop subcircuit-boundary variation from the axes at this scale.
- **The transform classes are near-tautologically detectable.** Moved components, renamed
  wires and inserted no-op buffers are, by definition, exactly what a position/name-erasing
  canonical form erases; two of the three should score identically to 1.0 under any correct
  canonicalizer. The entire measurement therefore lives on the null side — 3 planted pairs
  against 351 independent ones. That imbalance argues for spending #883's remaining budget on
  independent-solution diversity rather than on planted-pair craftsmanship, and for #885
  reporting the null distribution's upper tail as the headline number.

## Verdict

**endorse-with-reframing.** The gate is right, the ordering is right, the boundaries are
right, and filing the apparatus before the measurement is exactly correct. Keep all six
acceptance criteria; add four things that change the artifact's shape: commit a generator
alongside the files, pre-register the axis set, report an author-time degeneracy proxy that
does not wait on #356, and choose the assignment for exhaustive verifiability and save it as
plain text. Then say out loud that what is being built is an equivalence corpus with a
similarity measurement as its first consumer — so that a KC-25-1 fire kills a capstone and
not an asset.
