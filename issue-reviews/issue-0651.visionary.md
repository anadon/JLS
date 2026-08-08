# Issue #651: TASK-C564-4: minimization runs headless, and the minimized expression is proved equivalent to the original circuit by exhaustive differential test
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two claims are bundled here, and they are not the same kind of thing. One is
plumbing: minimization should be reachable without a display, because graders
run headless (CAP-31 AC-5, README's container-image pitch, `docs/batch-interface.md`
as a stability contract). The other is epistemic: **JLS should be able to say
"this expression is the same function as that circuit" and mean it**, rather
than diffing report bytes. The second claim is the interesting one, it is the
project's actual arc, and the issue files it as a test method.

`docs/capability-roadmap/lf-04-formal-and-grading.md` opens with the sentence
this issue is quietly an instance of: *"JLS has no representation of 'correct.'
It has a representation of 'what happened,' and grading is a string diff over
that."* That document already designs the general answer — `jls.formal`, an
AIG built by a third `HdlModel.StatementVisitor` implementation, four printers,
one solver — and observes that `HdlExporter.buildModel`
(`/home/user/JLS/src/jls/hdl/HdlExporter.java:166`) already does the port walk,
wire-net union-find and jump fusion such an extractor needs. #651 wants the
same thing at n≤4 for one narrow pair. That is fine as a starting point, but
it should be built as the *seed of that component*, not as a private method in
a `*GoldenTest`.

## The differential is not differential where the issue says it is

AC-1's stated rationale is that "a minimizer bug cannot hide behind a table
that was itself derived from the same code path." Trace the two sides:

- Left: circuit → exhaustive simulation → table (#563's extractor) → QM (#648)
  → expression → exhaustive evaluation.
- Right: circuit → exhaustive simulation.

Both sides run `jls.sim.Simulator` over the same elements. The only stage the
comparison is sensitive to is `table → expression`, i.e. the minimizer. It says
**nothing** about the extractor, which is the newer and more fragile component
(subgraph boundary selection, sequential-content rejection, input ordering). The
issue's own justification therefore over-claims: this is a minimizer check
wearing differential clothing.

That matters because JLS already ships the genuinely independent oracle and CI
already runs it. `test/jls/hdl/IverilogCompileTest.java` and
`test/jls/hdl/ToolLocator.java` compile JLS's Verilog export with an external
toolchain; `src/jls/hdl/yosys/` reads Yosys netlists. An extractor cross-check
against `iverilog`-elaborated export — or, later, Yosys `equiv`— is a real second
derivation. Split the claims:

- **Minimizer**: cover vs. table, exhaustive. Cheap, total, and (below) not a
  test at all.
- **Extractor**: table vs. an independently-derived function. Use the HDL path
  that exists, or wait for `jls.formal`.

## Reframing 1 — make it an invariant, not a golden (I am disregarding AC-1 as written)

The minimizer only ever runs *inside* a bound (#649 exists solely to enforce
that). Inside the bound, the ON-set is enumerable by construction — that is what
"bounded exact QM" means. So the check "every minterm of the table is covered
and no OFF-set minterm is covered" costs O(2^n × |cover|), strictly less than the
prime-implicant generation that just ran. There is no reason to spend that budget
only in CI.

Make it a postcondition of `Minimizer.minimize(...)`: the returned cover is
verified against its own input table before it is returned, always, in every
build, GUI and batch alike. Then:

- No expression JLS ever puts in a student's lab report can be wrong about the
  function. Today's plan gets that guarantee for four fixtures; this gets it for
  every table anyone ever minimizes.
- #649's AC-3 ("minimal or absent, no third state") stops being a test assertion
  about a code path and becomes structurally true: a cover that fails its
  postcondition is not returned, it is a refusal, and the refusal channel already
  has to exist.
- The remaining golden test then pins what goldens are actually good at — the
  documented tie-break of #648 AC-3, the operator notation of #650 AC-2, and the
  byte-determinism of the batch output — rather than re-proving arithmetic.

This is the reframing that makes the largest part of the issue's work disappear:
AC-1 and AC-2 collapse into "the invariant exists and a test proves the invariant
can fail" (the null test — the same discipline #347's review argues for), plus
a small fixture set for minimality and formatting.

## Reframing 2 — the input is a `.jls` file, always. JLS already has the table.

"Table in, expressions out" introduces a class of input the CLI has never had:
every current flag in `JLSStart.FLAGS` (`/home/user/JLS/src/jls/JLSStart.java:759`)
takes a circuit operand plus auxiliary files. A table-file grammar means a new
parser, a new hostile-input surface (#38's caps, `UntrustedFileHardeningTest`),
a new spot in the `LoadError` taxonomy, and a new format under #524's freeze — and
#565 will want to read the same thing, and #646 is already writing one.

JLS already has `jls.elem.TruthTable` (`/home/user/JLS/src/jls/elem/TruthTable.java:35`),
a first-class savable element with a `SaveTags` row, a headless-aware editor
(`TruthTableEditor`), and cells carrying `0`, `1`, `2 = don't care` (`:79-80`).
A hand-entered table *is already* a `.jls` file. So:

```
jls -analyze minimize [-region <name>] circuit.jls
```

where the subject is either an extracted combinational region or a named
`TruthTable` element. One input type for the whole CLI, save/load hardening
reused rather than duplicated, the GUI table editor becomes the table-entry UI
for free, and #565's "synthesis from a table" becomes the obvious thing:
*replace this `TruthTable` element with gates and prove the replacement
equivalent* — which is CAP-31 AC-2's round-trip, expressed as an edit rather
than as a file format.

## Reframing 3 — one analysis document, one schema version, not four flags

Count what is in flight against a flag table that #524 froze: #646 (table out),
#651 (expressions out), #565's TASK-C565-4 (synthesis round-trip), #306/CAP-09
(equivalence). Four tasks, four flags, four "output schemas documented alongside
the existing batch flags," four exit-code negotiations. A grader wants one
invocation and one parse.

Emit **one** analysis document with **one** schema version, whose sections are
populated by whichever analyses were requested: `table`, `expressions`,
`bound`/`refusal`, later `equivalence`. The stability promise then attaches to a
version field rather than to N ad-hoc formats, which is what #524 actually needs
in order to survive this capstone. Machinery is already half-present:
`src/jls/hdl/yosys/JsonValue.java` is an in-tree, dependency-free JSON reader; a
writer is small, and the format choice matters far less than the fact that there
is exactly one of it.

## The AC-1 / AC-2 contradiction nobody has named

AC-2 requires the differential to run over fixtures "covering don't-cares."
AC-1 requires comparing against "exhaustive simulation of the original circuit."
For a don't-care fixture whose original circuit is a `TruthTable` element, those
two requirements are in direct conflict, because of two facts in
`/home/user/JLS/src/jls/elem/TruthTable.java`:

```java
// :1446-1449
int outValue = table[matchingRow][pos+offset];
// don't care becomes false
if (outValue == 2)
    outValue = 0;
```

Output-side don't-cares are **destroyed at simulation time** — the exact
behaviour lf-04 calls out as making vector grading actively wrong. And on no
matching row, `react` returns holding previous outputs (`:1432`), so an
incomplete table is silently a latch, i.e. not combinational at all.

So a minimizer that correctly exploits a don't-care to produce a smaller cover
(#648 AC-2, which explicitly demands a *strictly smaller* case) will disagree
with exhaustive simulation of the very element that carried the don't-care. AC-1
as written would either fail that fixture or, worse, pressure the implementer to
make the minimizer treat don't-cares as zeros so the golden goes green —
enshrining the bug in a test. This is the concrete reason the oracle must be
"cover vs. table with its don't-care marks intact," not "expression vs.
simulation."

Consequence worth escalating to #564 or #515: **the don't-care semantics fix
(`:1446` and `:1432`) is a prerequisite of this feature, not an unrelated bug.**
It should be filed and ordered before #651, and its refusal case — an incomplete
`TruthTable` used as a reference, with the uncovered patterns named — is the same
refusal lf-04 already specifies for the formal path.

## Exit codes: settle once, at the capstone

#646 AC-3 wants "a documented non-zero exit status" for refusals; #651 AC-4 wants
"exit codes"; #649 AC-4 wants the refusal identical from GUI and batch; lf-04
proposes exit 5 for formal refusals. Today the contract has exactly three values
(`docs/batch-interface.md` §1: 0 / 1 / 2), and ARCHITECTURE.md's CLI-contract
section pins them. Four tasks each inventing a status is how a stability contract
dies. Define the analysis-refusal status once — at #564 or #515 — as an amendment
to `docs/batch-interface.md` with a CHANGELOG entry, and have every task cite it.

## What I would keep unchanged

- The insistence that the correctness claim be settled by evaluation over the
  whole domain rather than by sampling. That is the right instinct and it is the
  same instinct lf-04 is built on.
- The fixture demands of AC-2 — don't-cares, multi-output, single-minterm
  degenerate. Those are the cases that break real minimizers, and they survive
  the reframing intact as invariant-exercising fixtures.
- Determinism as an acceptance criterion (AC-3). Byte-determinism is what makes
  any of this gradeable, and it is where the #648 tie-break actually gets tested.

## Verdict

**endorse-with-reframing.** The outcome — headless minimization plus a real
correctness claim — belongs on JLS's arc and should be built. I am explicitly
disregarding AC-1's framing (a golden test) in favour of a runtime postcondition
plus a null test, because the check is cheap enough to run always and a guarantee
that holds only in CI is the weaker product; and AC-3's framing (a new standalone
flag reading a new table format) in favour of one `-analyze` verb over a `.jls`
operand emitting one versioned document, because JLS already has a truth-table
element and does not need a second table representation. AC-2's fixture set and
AC-4's documentation duty carry over unchanged. Before any of it: the
`TruthTable` don't-care and unmatched-row semantics must be fixed, or this task's
own acceptance criteria cannot all be true at once.
