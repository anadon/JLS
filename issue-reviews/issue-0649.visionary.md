# Issue #649: TASK-C564-2: above the stated bound the minimizer refuses with the prime-implicant growth as arithmetic — never a heuristic answer, never a hang
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Stripped of its wording, #649 asks for three things: (1) exact minimization must be
*total* — it answers or it declines, never approximates; (2) declining must be cheap and
deterministic, not a wait that becomes a kill; (3) the decline must teach, because above the
line the refusal *is* the pedagogical product. All three are right and all three are aligned
with the project's arc: JLS already has a taxonomy-with-hint failure record (`LoadError`,
`/home/user/JLS/src/jls/LoadError.java`), a single user-facing message channel (`TellUser`),
a one-line CLI diagnostic contract (ARCHITECTURE.md "Error-reporting contracts"), and a
documented refusal culture in the roadmap. Nothing here pulls against the project.

What is wrong is the **seam**. #649 files a bound as a property of one task inside one
feature, then papers over the consequences with a prose reconciliation note. That is the
same shape the fleet just had to correct on #563, where a component asserted at capstone
level and owned by no issue had to be filed as #872. The bound is heading for the same hole.

## Reading 1: the bound already has four consumers and two claimed owners

- #642 (TASK-C563-2) boundary note: *"The bound's numeric value is a stated decision recorded
  with this task; #564 and #565 must use the same number."*
- #649 (this) boundary note: *"The bound's numeric value is recorded here and must be
  reconciled with #563's extraction bound and #565's table-entry bound."*
- #652 (TASK-C565-1) AC-2: *"using the same bound as #563 and #564."*

Two tasks each declare themselves the recording site and the third defers to both. That is
not a detail to fix by editing one sentence; it is the tracker telling you the artifact has
no home. And there is a fourth consumer nobody counted: the PLD/JESD3-C work in
`/home/user/JLS/docs/standards-adoption/11-costed-rejections.md` independently specifies
*"Quine–McCluskey with don't-care support, capped at a documented input count (≤ 12–14 inputs
per output cone), refusing anything larger with a named error"* (~line 455) and names "the
minimizer becomes the project" as its top failure mode. That path needs #648's minimizer and
#649's bound and is not filed as an issue at all. (Note: the roadmap's `#83` for that row is
a standards-table id; GitHub #83 is an unrelated closed PR. Do not cite it as the issue.)

Worse, the three bounds are being forced to agree when they *should not*. 2^N enumeration is
linear in table size; prime-implicant generation is not (the classic worst case is ~3^N/N
implicants, and exact cover on top of it is NP-hard). A 16-input table is a perfectly
tractable 65 536 rows to enumerate and export headlessly, and a plausible catastrophe to
minimize exactly. "Same number or state why theirs differs" pushes the executor toward the
one number that is safe for the worst operation — which silently amputates extraction, the
capstone's own demo slice (#515 PF-1).

## Alternative A — the bound is a policy object, not a constant in a task

Concretely, and idiomatic to this codebase:

- `jls.analysis.AnalysisLimits` — one class holding a limit **per operation**
  (`ENUMERATION_INPUTS`, `MINIMIZATION_INPUTS`, `MINIMIZATION_WORK_BUDGET`,
  `TABLE_ENTRY_INPUTS`), each with the arithmetic that produced it in Javadoc.
- `jls.analysis.AnalysisRefusal` — a record modeled directly on `LoadError`: category,
  the arithmetic **as data** (inputs, rows, implicants reached, budget), and one actionable
  hint, with one `render()` that every front end calls. `LoadError`'s docstring already states
  the principle: the legacy string view is derived "so every front end shows the same message."
- `docs/analysis-bounds.md` (or a section of `docs/batch-interface.md`, since the batch
  refusal is a grading-visible contract), plus a doc↔code cross-check test in the style of
  `test/jls/ExtensionPointCatalogTest.java` / `CliFlagTableTest`.

What this buys: #649's AC-4 ("identical from GUI and batch") stops being a test of two
independently written strings and becomes true by construction — the same move `TellUser` and
`LoadError` already made. AC-1 ("stated wherever minimization can be invoked") gets a single
source to state. The #642/#649/#652 reconciliation note evaporates, replaced by four named
constants a reader can diff. And the unfiled PLD path inherits it for free instead of
inventing a fifth cap. Today's counterexample is instructive: `FileAbstractor.MAX_CIRCUIT_TEXT_BYTES`,
`Memory.MAX_INIT_WORDS`, `Trace.MAX_RETAINED_CHANGES`, `VerilogHeaderScanner.MAX_BITS`,
`CircuitOpReader.MAX_*` are five unrelated caps with five unrelated failure vocabularies;
only the loader family got a taxonomy. Analysis is the second family. Give it one.

## Alternative B — N is a proxy; the real guarantee is bounded work

I am explicitly disregarding **AC-2 as written** ("A table at bound+1 refuses within a fixed
time; *no run is started and abandoned*"). The second clause forbids the better design in
order to enforce the weaker property. What a student and a grading script actually need is:
the tool terminates in bounded time with a deterministic result. A pure input-count gate
delivers that, but it is a bad proxy in both directions — it refuses an 11-input function
with six minterms that minimizes instantly, and it accepts a 9-input function that generates
millions of implicants. Both are common in coursework; the first is the one that makes a
student think the tool is broken.

The design that satisfies the real guarantee: a **metered budget**. Generation and cover
selection increment a counter; crossing `MINIMIZATION_WORK_BUDGET` aborts *deterministically*
(the budget is a pure function of the algorithm's own steps, not of wall-clock, so the same
table always yields the same verdict on every machine — which is what "no third state" and
reproducible grading actually require). Keep a cheap static pre-check for the provably
hopeless (table itself too large to hold), so the common refusal is still instant. Then the
refusal message improves from "N > 10 is refused" — which teaches only that a maintainer
picked 10 — to "11 inputs, 900 minterms; prime-implicant generation passed 2 300 000
implicants against a budget of 250 000." *That* is the exponential growth named as
arithmetic, which is what the issue's own Outcome paragraph says it wants. If the AC must keep
a fixed-time clause, restate it as: **no input causes an unbounded or wall-clock-dependent
run, and the verdict for a given table is identical on every machine and every run.**

## Alternative C — make the refusal sentence true, and record the door it leaves open

"Exact minimization is exponential, and the honest answer above a certain size is 'no'" is
half true. Exponential is a property of **Quine–McCluskey**, which materializes every prime
implicant. Exact minimization above the bound is reachable by engines that never do that —
incremental SAT ("is there a cover of size ≤ k?"), which is exact, not Espresso-class, and
therefore does not touch KC-31-1 at all. JLS is already walking toward that machinery:
`docs/capability-roadmap/lf-04-formal-and-grading.md` designs a CDCL/MiniSat-class solver and
Tseitin encoding for equivalence checking, and #222's recorded decision keeps external
engines (Yosys, GHDL, Icarus) on a subprocess boundary that a future exact minimizer could
sit behind without a GPL linking problem.

I am **not** proposing to build that here — it is nowhere near a 1 mw task and KC-31-1 is
correctly drawn. I am proposing that the refusal text not teach a falsehood. Say *"this
minimizer is exact by exhaustive prime-implicant generation, which this function is too large
for"*, not *"an exact answer is not available"*. One sentence, no cost, and it leaves a
revisit trigger — written in ARCHITECTURE.md's "Recorded decisions" form, the project's own
convention for exactly this — reading: *a course or the PLD path needs exact minimization
above the QM budget; at that point file a SAT-based exact minimizer behind the same
`Minimization` result type, not a heuristic.*

## Alternative D — "no third state" is a type, not a test

AC-3 asks a test to assert every returned expression is minimal or absent. #648 AC-4 already
requires the expression to be a structured value. Then close the loop in the type system:
`sealed interface Minimization permits Minimal, Refused` — `Refused` carrying the
`AnalysisRefusal` above. There is no fallback path to test for, because there is no
representable third case, and no caller can forget to handle one. The codebase is already
here: 46 files use records, `LoadError` is a record, and `jls.collab.op.CircuitOp` is a sealed
hierarchy. This costs nothing and turns AC-3 from a promise into a compile error.

## What I would keep untouched

The exactness stance (KC-31-1), refusal-as-product, and the GUI/batch parity requirement are
the right commitments and are the reason this capstone is worth doing at all. The instinct
that a refusal must state arithmetic rather than a policy number is the best idea in the
CAP-31 tree; Alternative B is that idea taken seriously rather than diluted by a constant.

## Net

Endorse the outcome; reframe the seam. Concretely: re-home the bound from #649/#642/#652 into
one `AnalysisLimits` + `AnalysisRefusal` pair with a doc↔code cross-check (this is a #872-shaped
filing, and it should be filed once as a shared component, homed under 563 with
`shared_with: [564, 565]` and the PLD row noted as a future consumer); replace the single N
gate with a per-operation limit plus a deterministic work budget; restate AC-2 as
machine-independence rather than "never started"; make AC-3 a sealed return type; and fix one
sentence of refusal text so it names Quine–McCluskey's limit rather than mathematics'. What is
left of #649 after that is roughly half a maintainer-week of wiring the minimizer to a policy
it did not have to invent — which is the correct size for this task.
