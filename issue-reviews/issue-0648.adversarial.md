# Issue #648: TASK-C564-1: an exact Quine–McCluskey minimizer turns a truth table into per-output sum-of-products, honoring don't-cares
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This task-level issue is a noticeably tighter spec than its parent feature (#564): it addresses
two of #564's own review findings head-on — AC-1 explicitly requires verification "against the
known result, not merely against a simulation of its own output," and AC-2 explicitly requires "a
test [that] pins a case where it is strictly smaller." The exact-only kill-criterion boundary is
consistent with CAP-31 (#515) and correctly deferred to a sibling task rather than restated loosely.
But the issue still carries an unjustified hard dependency on an unbuilt capstone chain, asserts a
"documented tie-break" that is documented nowhere, and leaves the AC-4 structured-value contract —
the exact thing #565's synthesis is supposed to consume without re-parsing — unspecified.

## Findings, most severe first

**1. HIGH — `ordering_after: ["TASK-C563-1"]` chains this task behind an unbuilt capstone extractor for no real code reason.**
Quoted: `ordering_after: ["TASK-C563-1 (its table is this minimizer's input)"]`. TASK-C563-1 is #641,
which is itself `ordering_after: [306]` — CAP-09's combinational-subgraph extractor, which #872 (filed
2026-08-04, reviewed 2026-08-08) already found "no filed issue delivered." So the literal ordering
chain to start #648 is `#306/#872 (unbuilt) → #641 (unbuilt) → #648`. But the actual object this
minimizer needs is a populated `jls.elem.TruthTable` (int[][] table with 0/1/2 values, input/output
names) — a class that already ships and is already independently constructible by hand today via
`TruthTableDialog`/`TruthTableEditor` (`src/jls/edit/`), with `TruthTable.makeDontCare` producing the
value-2 don't-care cells this task's AC-2 needs (`src/jls/elem/TruthTable.java`). This is the identical
defect the fleet already flagged one level up, in #564's finding 4 ("the stated `ordering_after` is
stricter than the actual code dependency") — but that fix was never carried down into this task's own
`ordering_after` field, so the task inherits the parent's problem verbatim. **Recommendation:** point
`ordering_after` at a populated-`TruthTable`-object dependency (satisfiable today via hand-entry
fixtures, e.g. matching `test/jls/elem/TruthTableModelTest.java`'s pattern) rather than at TASK-C563-1,
and note the "extracted from a drawn circuit" path as a downstream integration test once #563's chain
lands, not a build blocker now.

**2. HIGH — AC-3's "documented tie-break" is asserted to exist but is documented nowhere in this issue, and AC-1's "known result" is ambiguous without it.**
Quoted: "equal-cost covers resolve by a documented tie-break rather than by hash order." No tie-break
rule (lexicographic term order, essential-PI-first, literal count, input-index order, or anything else)
appears anywhere in the Outcome, AC list, or boundary notes — the word "documented" points at a document
that isn't there, the same pattern the fleet already flagged for #649's "bound is recorded here" (no
bound present) and #563's "extractor is #306's, already built" (it wasn't). This isn't cosmetic: exact
cover selection via Petrick's method commonly yields *multiple* equal-cost minimal covers for
non-trivial tables, so AC-1's "verified against the known result" is underspecified for any golden case
where more than one minimal SOP exists — an implementer has no way to know which "known result" a
reviewer will check against, and two correct implementations that pick different (equally minimal)
covers could each fail someone else's fixed-answer golden test. **Recommendation:** state the actual
tie-break rule in the boundary notes (e.g., "prime implicants ordered by ascending literal count, then
lexicographically by input index" or similar), and for AC-1 either pick golden tables with a
provably-unique minimal cover, or explicitly note that AC-1's goldens are chosen/normalized against the
stated tie-break so "known result" is well-defined.

**3. MEDIUM — AC-4's "structured value" has no stated contract, and #565 needs one to code against.**
Quoted: "The expression is a structured value, not a formatted string, so #565's synthesis can build a
netlist from it without re-parsing text." Ruling out a formatted string is a real, sound decision (and
consistent with the project's general typed-seam discipline — `LoadError`'s category taxonomy,
`docs/extension-points.md`'s typed catalog), but nothing here names the shape: per-output list of
terms, each term as a set/list of (input-index, polarity) literals, whether it carries the don't-care
set alongside, or how outputs are keyed back to `TruthTable`'s output names. `grep -rn
"QuineMcCluskey\|SumOfProducts\|BooleanExpr" src/` (already checked in the parent feature's review and
reconfirmed here) finds nothing existing to anchor this to, and TASK-C565-2 (#653, netlist construction)
is the concrete downstream consumer with its own AC list that presumably assumes some specific shape.
Two independently-plausible implementations of "a structured value" (say, a `List<List<Literal>>` vs. a
richer `record SumOfProducts(...)`) both satisfy AC-4 as worded while being incompatible with whatever
#653 actually expects, risking a renegotiation or adapter layer neither issue budgets for.
**Recommendation:** name the minimal contract now (field/record shape, one paragraph) so #653/#654 can
be written against it without waiting on #648's implementation to reverse-engineer the type.

**4. MEDIUM — no complexity guard exists at the point this task lands, and the issue doesn't flag the exposure window.**
The boundary notes correctly defer "the bound and its refusal" to TASK-C564-2 (#649) — a clean split in
principle. But #649 is *ordered after* #648, meaning the exact minimizer this issue delivers has no
refusal threshold at all when it first exists. #649's own review already established that minimization's
cost is driven by minterm/PI structure, not input count alone, and can blow up in cases a naive
input-count cap wouldn't catch. If any interim surface (a debug menu item, a batch flag stood up early,
a manual test harness) calls this minimizer core before #649 lands, pathological content can hang or
exhaust memory — the exact failure mode CAP-31's kill-criterion (quoted approvingly in the Outcome:
"there is no third result that is 'close'") is trying to prevent, just via a sequencing gap rather than a
design flaw. **Recommendation:** add a boundary note stating the core is not to be wired into any
interactive or batch surface until TASK-C564-2 lands, or ship a conservative internal-only cap in this
task as a stopgap.

**5. LOW — AC-1's "a set of tables with known minimal forms" has no floor on size or diversity, inviting a shallow pass.**
Nothing pins a minimum count, a source (textbook QM examples routinely include cases requiring Petrick's
method because essential prime implicants alone don't cover the table — a classic minimizer bug surface),
or coverage of edge cases (all-don't-care columns, single-output vs. multi-output, a case where the
naive essential-PI-only heuristic gives a non-minimal answer). Two trivial two-variable tables (AND, OR)
satisfy the plural "a set of tables" literally while testing none of the algorithm's actually hard parts.
**Recommendation:** name at least one golden requiring non-essential prime-implicant selection (Petrick's
method), not just essential-PI extraction, so AC-1 can't be satisfied by the easy 80% of the algorithm.

## What's solid

- The exact-only kill-criterion boundary (KC-31-1) is correctly scoped and consistent with CAP-31
  (#515) and with the parent feature #564 — no heuristic/Espresso-class fallback is smuggled in.
- AC-1 and AC-2 directly close the two specific gaps the fleet flagged one level up in #564
  (self-referential golden-testing and untested don't-care exploitation) — a real improvement over the
  parent issue's text, not a restatement of it.
- The three-way scope split (this task: core minimizer; #649: bound/refusal; TASK-C564-3: display/
  export) is clean and non-overlapping, matching the pattern already validated for #563's sibling tasks.
- AC-4's underlying decision (typed value over formatted string at an internal module seam) is the
  right call and matches the project's established typed-seam discipline elsewhere in the codebase.

## Verdict rationale

`needs-rework`: the core algorithmic scope and its boundary against sibling tasks are sound, and two
real defects inherited from the parent feature review are already fixed in this issue's own AC text.
What remains is a dependency edge that blocks on an unbuilt capstone for no real code reason, a
tie-break the issue calls "documented" without documenting it (which leaves AC-1's own golden-test
oracle ambiguous for any table with multiple minimal covers), and an unspecified data contract at
exactly the seam #565 needs to be stable. All are issue-text fixes, not implementation difficulty.
