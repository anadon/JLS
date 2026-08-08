# Issue #648: TASK-C564-1: an exact Quine–McCluskey minimizer turns a truth table into per-output sum-of-products, honoring don't-cares
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Under the tier scaffolding, #648 is the one piece of CAP-31 (#515) that is a real
algorithm rather than a view: **JLS gains the ability to turn a Boolean function into a
minimum two-level cover, exactly, or to decline.** That capability belongs here. The
tree already argues for it independently of CAP-31 —
`docs/standards-adoption/11-costed-rejections.md:455` recommends "Quine–McCluskey with
don't-care support, capped at a documented input count (≤ 12–14 inputs per output cone)"
for the GAL/PLD path (#83), and `docs/capability-roadmap/sweep-03-elements-and-hdl.md:59`
names the missing minimizer as what blocks #83 outright. Two independently-filed
roadmap lines converge on the same 300-line component. Build it.

What I want changed is not whether, but **what shape the thing has**, because as written
the task defines a function `table -> expression` and leaves four decisions implicit that
the rest of the tree has already made differently or will have to pay for later.

## Reframing 1: this is a core component with three consumers, not a step inside a view

`ordering_after` binds #648 to #563's extractor output, and `band_mw: 1` sizes it as a
sub-part of a GUI feature. That framing invites the code to land next to the table view.
It should not. The minimizer's consumers, visible today:

- **#565 synthesis** — needs the cover as a netlist source (the task already knows this;
  AC-4 exists for it).
- **#83 PLD/JEDEC** — `11-costed-rejections.md:417` lists "equations → minimized
  sum-of-products" with owner **"decide"**; this task decides it. That path runs through
  `jls.hdl`, which must not depend on a truth-table *view*.
- **`jls.elem.TruthTable` itself** — the shipped element's `makeDontCare` /
  `removeInput` (`src/jls/elem/TruthTable.java:862`, `:764`) already perform a greedy,
  local, one-pair row merge with a `TellUser.error(null, "not possible")` when the user
  picks the wrong cell. That is hand-rolled proto-minimization in the element layer, and
  an exact minimizer subsumes it.

Concretely: put it in a new AWT-free core package (`jls.logic`, sibling to `jls.core`
and `jls.sim`), with the same headless ratchet `HeadlessCoreRatchetTest` applies to
`jls.sim` (ARCHITECTURE.md "Module layout"). Nothing in it imports `jls.edit`,
`jls.elem`, or Swing. Then #565, #83, and the element all consume one implementation.
The band-1 sizing is fine; the *placement* is what matters, and the task does not state it.

## Reframing 2 (load-bearing): "don't-care" already means something else in JLS, and it means zero

AC-2 says don't-care rows "are used to reduce rather than treated as zeros". In shipped
JLS, an output don't-care **is** zero, deliberately, in both engines:

- `src/jls/elem/TruthTable.java:1447` — `// don't care becomes false` / `if (outValue == 2) outValue = 0;`
- `src/jls/hdl/HdlModel.java:622` — "Per output column: 0 or 1 (don't care already lowered)".

And an *input* don't-care in that element is not a don't-care at all — it is a `casez`
wildcard resolved by **first-matching-row priority** (`TruthTable.react`, the
`break` on first match; `HdlModel.PriorityCaseStatement`). A table whose rows overlap is
a priority encoder, not a function, and a table with no matching row **holds its previous
outputs** (`:1431`, "leave the outputs unchanged") — i.e. it is not even combinational.

So the task's single word "table" spans three incompatible objects: #563's exhaustive
extracted table (a function), the shipped element's ternary priority table (a relation
with memory), and the mathematical (on-set, dc-set, off-set) partition QM actually needs.
If the minimizer reads a `2` in an output column as "free", the cover it returns is
**not equivalent to what JLS simulates**, and #564 AC-1 (golden vs. exhaustive
simulation) and #565 AC-1 (round-trip) will contradict each other for exactly the tables
AC-2 requires as evidence.

The reframe: **#648's input type is not "a truth table"; it is a `care-set function`** —
minterm on-set, dc-set, and input arity, with the dc-set carried explicitly and never
inferred from the `2` sentinel. Whoever adapts a `TruthTable` element into it owns the
priority-expansion and must choose, visibly, whether output `2` maps to dc or to 0 —
and today's answer, to preserve simulation equivalence, is **0**. Introducing a genuine
unspecified-output marker is a separate, larger change to the element, its save format,
`docs/file-format.md`, and the HDL lowering, and it should be named as such rather than
smuggled in through this task's AC-2. If AC-2's "strictly smaller" test is to exist at
all in v1, its input must be a hand-constructed care-set function, not a drawn element.

## Reframing 3: name the objective, or "minimal" is unfalsifiable

AC-1 asserts "a minimal cover" against "known minimal forms" without saying minimal in
what. Term count, literal count, and multi-output shared-product count give different
answers, and the two paying consumers want the third one: #565 draws gates (literals and
fan-in matter) and #83 fits a device with a shared product-term budget per output group.
Per-output QM is provably **not** minimal for the multi-output circuit that #565 draws.

Concretely: make the cover objective an explicit, documented parameter of the covering
step (`minimum term count, ties broken by total literal count` is the right v1 default,
because it is what every textbook chart method yields and what a student will check
against by hand), and state in the Javadoc and in the display that the result is
per-output minimal, not multi-output minimal. Multi-output QM is then a later,
compatible upgrade behind the same interface instead of a retraction of the word "exact".

## Reframing 4: the exponential the bound must catch is in the cover step, not the primes

#649 states the bound in terms of "prime-implicant growth" and enforces it "before work
begins" (its AC-2: refuse at bound+1 "within a fixed time; no run is started and
abandoned"). That is a pre-check on input count, and it does not deliver CAP-31 AC-3.
Prime generation at N ≤ 12 is trivially bounded; the NP-hard part is **minimum cover
selection over the prime chart**, and its cost is driven by the cyclic core, not by N.
A 10-input function with a large cyclic core can outlast a student's patience while
sitting comfortably below any input bound.

Two consequences, both of which land on #648 because #649 is ordered after it:

- **Petrick's method is the trap.** Algebraically expanding the product-of-sums over the
  chart is the textbook presentation and the standard way this feature dies in memory.
  Use essential-prime extraction, row/column dominance reduction, then branch-and-bound
  with a lower bound from a maximum independent set of the reduced chart — and give the
  search an explicit node/time budget.
- **The return type must therefore be a sum type**, `Minimal(cover, …) | CannotAnswer(reason)`,
  refusable from the middle of the search. AC-4 says "a structured value, not a formatted
  string" but describes only the expression. Pin the refusal into the type here, in the
  task that defines the type, or #649 inherits a signature that cannot express its own
  acceptance criterion and the "never hangs" promise becomes false for a case nobody tested.

## Reframing 5: verify by construction, not by curated goldens

AC-1's "a set of tables with known minimal forms" is a handful of hand-derived cases and
proves minimality nowhere else. Two cheaper and far stronger obligations are available at
this scale:

- **Always-on correctness invariant.** Within the bound, evaluating the returned cover
  over the whole care set is ≤ 2^12 rows — microseconds. Make the minimizer verify its
  own answer against its input *in production*, not only in tests, and turn a mismatch
  into `CannotAnswer` rather than a wrong expression. That makes the issue's own
  sentence — "the answer is minimal or the tool says it cannot answer" — a runtime
  property instead of a test-suite claim, and it is exactly the honesty posture #649,
  CAP-09 (#306), and the batch-interface contract are built on.
- **Exhaustive differential for small N.** Every 3-input function is 256 tables and every
  4-input function is 65,536; a slow, obviously-correct reference (brute-force minimum
  cover over all implicants) settles minimality for *all* of them. Run N ≤ 3 in CI and
  N = 4 as a tagged/offline sweep whose per-function optimal costs are checked in as the
  golden. That is a real proof over a closed universe, in the spirit of `proofs/` and of
  CAP-09's "a proof, not a confident answer".

## Reframing 6: for a teaching tool, the derivation is the product

CAP-31 exists because Digital, DEEDS and Issie out-teach JLS (#510 §2). A student is not
graded on `Y = A·B' + C`; they are taught prime implicants, essential primes, the chart,
and the cyclic core — the very intermediate structures this algorithm computes and then
throws away. Make the structured value of AC-4 a `MinimizationResult` carrying the
derivation — primes, the chart, essentials, the reduced core, the chosen cover — not just
the cover. Cost at construction time: near zero, they already exist. Payoff: TASK-C564-3's
display becomes a *rendering* of that value; a K-map view (the thing actually taught, and
which no CAP-31 feature currently plans) becomes another rendering rather than a new
feature; and AC-3's "documented tie-break" becomes explainable to the student instead of
a footnote.

While there: prefer **canonicality** over a documented tie-break. Order primes by their
(mask, bits) pair and select the cost-lexicographically-least cover; the output is then a
pure function of the input, which is what makes #565's round-trip goldens and any future
`.jed`/`.pld` golden byte-stable — consistent with this repo's reproducibility posture.

## Alternatives considered and closed (state them, so they stay closed)

- **Delegate to Yosys/ABC.** The subprocess seam already exists (`src/jls/hdl/yosys`,
  #61) and `abc` has SOP minimization. Rejected, and worth recording: ABC's flow is
  heuristic, so it cannot answer "minimal"; and a core classroom feature must work from
  the single self-contained jar with no external toolchain (README, "Running JLS from the
  jar"). Owning ~300 lines of QM beats a network of preconditions.
- **Skip QM; ask CAP-09's solver.** #306 is bringing an extractor, AIG, CNF and a SAT
  solver; exact minimum cover is a natural (Max)SAT query, and "one decision procedure"
  is architecturally attractive. Rejected *for now* on ordering and on pedagogy — the
  solver dependency is itself unsettled in #306, and a SAT answer has no chart to show.
  But it is the reason the covering step should sit behind a small internal interface:
  if #306 lands a solver, the exact-cover engine is swappable without touching prime
  generation or the result type.
- **BDD-based minimization.** More machinery, worse teaching artifact, no chart. No.

## Where this pulls with the project's arc

With the above, #648 stops being a step inside a table view and becomes the small,
verified, headless core component that #565, #83, and the existing `TruthTable` element
all draw on — one minimizer, three consumers, matching the "one extractor, two consumers;
do not build twice" discipline #563 and #515 already apply to CAP-09's extractor. As
written, it risks being the fourth private representation of a truth table in this tree
(`TruthTable.table`, `HdlModel.PriorityCaseStatement.Row`, #563's extracted table), and
the first component in JLS to claim exactness for an unnamed objective over a don't-care
notion its own simulator contradicts.

**Verdict: endorse-with-reframing.** I am not disregarding the acceptance criteria; I am
tightening three and contesting one. AC-2 as written asks the minimizer to honor a
don't-care semantics JLS does not have — it must be re-scoped to an explicit care-set
input, or it will manufacture expressions that disagree with `TruthTable.react:1447`.
