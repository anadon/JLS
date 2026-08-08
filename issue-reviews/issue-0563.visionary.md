# Issue #563: FEAT-C31-1: a drawn combinational circuit reads out as its truth table, within a stated input bound that refuses with arithmetic instead of hanging
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated outcome is "a student reads a truth table." The *project-level* purpose is
larger and better: JLS today can only answer questions about a circuit by stimulating it.
Every oracle in the tree — `BatchSimulationGoldenTest`, `SequentialGoldenTest`,
`VcdExportGoldenTest`, `examples/autograde/autograde.py` — is a stimulus-and-compare.
CAP-09 (#306) opens by naming exactly that as the defect: a submission wrong on 254 of 256
inputs passes because one vector was checked. #563 is the first issue in the tree that
produces a **complete behavioral description of drawn logic** rather than a sample of it.
That is the thing worth building. The truth-table *view* is a rendering of it; CAP-31's
minimization (#564) and synthesis (#565) are transforms of it; CAP-09's combinational
equivalence is a comparison of two of them.

Judged on that axis the issue is right and well-placed. Three reframings would make it
much cheaper and make it pay CAP-09 immediately.

## Reframing A — the artifact already exists: emit a `TruthTable` element, not a new view

`jls.elem.TruthTable` ships (1,491 lines) with `jls.edit.TruthTableEditor`,
`DisplayBool`, `TruthTableRenderer`, `TruthTablePrintable`; it round-trips through
`AllElementsRoundTripTest`, has a normative save grammar (`docs/file-format.md:324`), an
`ElementRegistry` descriptor, and an HDL emission path in `HdlExporter`. The task
decomposition treats all of this as absent: #644 specifies a new table view, #646
specifies a new machine-readable schema behind a new batch flag, #641 says "the table is a
value, not a view."

Make the value **be the element**. Extraction's output is a `TruthTable` instance whose
input/output column names are the cone frontier's net names. Then:

- **#644 (view) mostly evaporates.** Dropping the element on the canvas and opening its
  existing editor *is* the view, with named columns (AC-2) and existing keyboard/a11y
  behavior. What remains is a read-only mode and the refusal surface (AC-3).
- **#646 (batch schema) evaporates.** The machine-readable, byte-deterministic output
  (its AC-2) is the existing plain-text save written by `-savetext`, already normative,
  already golden-tested by `DeterministicSaveTest`, already diffable in version control.
  Do not design a second table serialization that has to be kept honest against the first.
- **#565's round-trip stops being self-referential.** The dedup comment worries that
  #565's AC-1 asserts something about #563's extractor. If extraction emits an element,
  the round-trip is checkable by *simulation*: replace the selected cone in a copy of the
  circuit with the extracted `TruthTable` element and run the existing batch golden
  machinery — the two circuits must agree on every vector. That is a differential oracle
  against the shipped simulator, strictly stronger than a hand-written golden table, and
  it uses no new test infrastructure.
- **CAP-31 becomes one data type with three verbs** (extract / minimize / synthesize)
  instead of three subsystems sharing a private record.

Concrete blockers to check before adopting this, all small and all worth filing:
`addInput`/`addOutput` call `TellUser.error(null, …)` on duplicate names
(`src/jls/elem/TruthTable.java:610-630`) — a model method that talks to the user is wrong
for programmatic construction when two frontier nets share a name; and `addInput` doubles
and copies the whole array per call, so building an N-input table costs O(N·2^N) with N
reallocations. Both want a bulk `TruthTable.of(inputNames, outputNames, int[][])`
constructor, which is a one-task prerequisite, not a redesign.

## Reframing B — the cone extractor is a re-cut of the HDL walk, not a new pass

#872 was correctly filed (the ordering edge onto an unowned component was a real hole),
but it is scoped as though nothing in JLS walks the element graph. `jls.hdl.HdlExporter`
already does all of it: it partitions nets, unions same-named `JumpStart`/`JumpEnd`
aliases, derives module **ports** from `InputPin`/`OutputPin` (that is a frontier), and —
critically — carries an enumerated per-element policy classifying every element class into
exported / net-topology / warn-and-skip / reject, with the sequential ones
(`Register`, `StateMachine`, `Clock`) distinguished from combinational ones and
`ShiftRegister` explicitly documented as combinational despite its name
(`src/jls/hdl/HdlExporter.java:60-100`). `jls.hdl.layout.LayoutGraph` already detects
combinational cycles as feedback back-edges.

So #872's AC-2 ("a registry-keyed state-holding table with a totality test") is about to
create JLS's **second** authority on what "combinational" means, while `HdlExporter`'s
bucket policy stays the first and `LayoutGraph` keeps its own cycle notion. That is the
same failure #872 exists to prevent, one level up. The elegant seam: put the
combinational/sequential predicate on `ElementType` in `jls.elem` (it is exactly the kind
of headless, loader-adjacent metadata #78 says belongs there), make `HdlExporter`'s policy
read from it, and let #872's extractor be a thin selection-scoped walk over #468's net
partition plus that predicate. One definition, three consumers (HDL export, cone
extraction, CAP-09), and the totality test lands where `ElementRegistryTest` already
enforces totality.

## Reframing C — compute the whole table in one pass; the bound is display, not arithmetic

This is where I am **explicitly setting aside the issue's AC-2 as written**. AC-2 makes
the headline outcome a *refusal*: "above N inputs the tool refuses with the row-count
arithmetic." #642 hardens that into "a test at bound+1 refuses within a fixed time rather
than starting and being cancelled." That design is a straight consequence of an unstated
implementation assumption in #641 — that enumeration means running the simulator 2^N
times. It does not.

Evaluate the cone **bit-parallel**: give every net in the cone a `BitSet` of length 2^N
holding its value across *all* input combinations at once, seed the frontier inputs with
the standard alternating patterns, and evaluate the cone in one topological sweep. Each
gate is one `BitSet.and`/`or`/`xor`/`flip`. The whole table costs one traversal with 64
rows per machine word, not 2^N simulator runs; JLS already has `BitSetUtils` and the cone
is by construction acyclic and stateless, so no event queue is needed at all. At N=16 each
net holds 8 KB. At N=20, 128 KB. The compute cliff people fear is at N≈24-26, not N≈10.

Three consequences that change the shape of the feature:

1. **The honest bound is about reading and writing, not computing.** A million rows is
   unreadable in a view and pathological in the `pair`-per-cell save grammar. So state
   *two* bounds — an evaluation bound (memory arithmetic) and a materialization bound
   (rows the view/serializer will produce) — and let the second one be the low, visible
   one. The pedagogy improves: the tool says "I computed this function; 2^18 rows is too
   many to show you — here is the minimized expression, or ask me about a row."
2. **Simulation stays the oracle and stops being the implementation.** AC-1's
   "golden-tested against exhaustive simulation" is exactly right and should be kept
   verbatim — it becomes a genuine differential test between two independent evaluators
   rather than a test of enumeration against itself.
3. **CAP-09 gets its small-circuit path free.** Combinational equivalence of two cones
   below the evaluation bound is `BitSet.equals` on their output vectors, and a
   counterexample is the index of the first differing bit, decoded back to an input
   vector — replayable in the GUI, which is CAP-09's own headline requirement. The
   AIG/CNF/SAT machinery #306 contemplates is then only needed *above* the bound, and it
   gains a free differential oracle below it. #563 built this way is not a sibling of
   CAP-09; it is CAP-09's floor.

If the bit-parallel core is what gets built, the truth vector — one `BitSet` per output
over a documented input ordering — is also the ideal input to #564's Quine–McCluskey and
the ideal canonical form for #565's round-trip, so KC-31-1's "exact only" bound and this
feature's bound become the same number for the same reason.

## Alignment and residual risk

This work pulls **with** the project's arc: headless-first model code
(`HeadlessCoreRatchetTest`), goldens as oracles, normative specs over ad-hoc formats,
one-definition components. It duplicates part of the arc in exactly two places — the HDL
walk (Reframing B) and the file-format-vs-new-schema question (Reframing A) — and both are
avoidable without changing what the student sees.

Risks the reframings do not remove: the `TruthTable` element is one bit per column, so
#641's AC-4 must choose bit-expansion (splitters are what students draw anyway) and say
so; the element's don't-care→false collapse and hold-on-no-match behavior
(`TruthTable.java:1432-1449`, cited in #306) are inert for a fully-specified extracted
table but become live the moment #564 writes don't-cares back into one, so that defect
must be fixed before #564, not before #563; and a `pair`-per-cell save of a 2^12 table is
~50k lines, which is survivable under XZ but is a real argument for the materialization
bound of Reframing C.

**Verdict: endorse-with-reframing.** The outcome is right, the placement in the tree is
right, and #872's filing corrected a genuine hole. Build the value first as a bit-parallel
truth vector over an `ElementType`-keyed cone predicate, materialize it as the existing
`TruthTable` element, and let the view, the batch surface, and #565's round-trip fall out
of machinery JLS already ships and already tests.
