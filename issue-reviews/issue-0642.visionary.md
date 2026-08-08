# Issue #642: TASK-C563-2: above the stated input count the tool shows the 2^N arithmetic and refuses, and a selection with feedback names the offending element
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its two acceptance clauses, #642 asserts one thing about what JLS should
become: **an analysis tool that knows the price of the answer before it starts paying it,
and says the price out loud.** That is not a truth-table detail. It is the same value the
project already ratified elsewhere — `LoadError.Category.LIMIT_EXCEEDED` with a named cap
(`FileAbstractor.MAX_CIRCUIT_TEXT_BYTES`, `src/jls/FileAbstractor.java:65`), the fixed
exit-status lattice in `docs/batch-interface.md`, CAP-31's KC-31-1 "minimal or absent, with
no third state", ARCHITECTURE.md's recorded non-goals that name their revisit trigger. JLS's
identity, in the arc these documents describe, is *a tool that refuses legibly rather than
degrading silently*. #642 is that identity applied to enumeration. Endorse the claim.

The reframing is about the shape of the mechanism, not the goal. As written, #642 delivers
the weakest available form of its own idea: one magic constant, two hand-written refusal
strings, and a prose promise to reconcile with two siblings.

## The structural problem the issue inherits

The dedup comment on this issue is the tell. Three tasks (#642, #649, #652) each publish a
number, each phrase their refusal "the same way", and the comment must plead that *whoever
lands first records the number and the derivation; the other two either adopt it or state in
writing why their wall sits somewhere else*. That is a social protocol standing in for a
type. The comment even names the failure mode it cannot prevent: an unreconciled trio shrinks
#655's round-trip window, possibly to nothing.

This project does not normally settle cross-issue obligations in prose. It settles them with
catalogues and cross-check tests: `docs/extension-points.md` + `ExtensionPointCatalogTest`
("adding a typed-now row without a constant, or a constant without a row, is a build
failure"), `HelpTopicsTest`'s palette-coverage completeness test, `HeadlessCoreRatchetTest`,
`SaveTagsTest`. The idiom exists. #642 should use it instead of a promise.

## Alternative framing 1 — a cost estimate, not a bound

Replace "a number recorded with this task" with one small value type in the model layer:

    record AnalysisCost(long rows, int inputs, int outputs, long estimatedCells, String derivation)
    sealed interface AnalysisVerdict permits Admissible, Refused
    record Refused(Kind kind, String subject, AnalysisCost cost, String hint)  // Kind: SIZE_BUDGET, SEQUENTIAL_CONTENT, FEEDBACK, UNRESOLVABLE_REGION

Each analysis in CAP-31 implements `estimate(request) -> AnalysisCost` in its own currency —
enumeration in `2^N` rows, #649's minimizer in prime implicants, #565's hand entry in cells a
person will type — and **one shared policy object** compares the estimate to one budget table
in one place. The three walls stop being three constants that must be manually reconciled and
become three estimators against one policy: the reconciliation obligation disappears because
there is nothing to reconcile. The trio comment's correct observation — that the three costs
are genuinely different functions of different inputs — survives intact; it is exactly why
`estimate` is per-analysis while the budget is shared. Add a catalogue row per budget in
`docs/` and a cross-check test, and #649's one-sided obligation becomes a build failure
instead of a hope.

This also makes AC-3's "distinct in wording" obsolete, which is a mercy. Refusals should be
distinguished by *category*, exactly as `LoadError` does — "tests assert on these labels,
keeping the detail wording free to improve" (`src/jls/LoadError.java:36-38`). A test that
pins two diagnostics as textually unalike is a test that breaks when someone improves a
sentence, and passes when both categories render the same wrong text. Model the refusal on
`LoadError`'s four parts (category / location / detail / actionable hint), route it through
`TellUser` per the recorded contract, and #646 gets a machine-readable refusal for free
rather than inventing a second serialization of the same fact.

## Alternative framing 2 — teach the exponential instead of announcing it

I am explicitly disregarding **AC-1** as written. "The bound is visible in the UI before
extraction runs" produces a number posted in a dialog nobody reads, and a modal error at the
moment of failure. The better artifact for a pedagogy tool is the cost shown *live against
the current selection*: the Analyze menu item reads `Truth table (12 inputs → 4,096 rows)`
while the selection is admissible and `Truth table (24 inputs → 16,777,216 rows — over
budget)` greyed out, with the derivation in the tooltip. A student who adds one input and
watches the row count double has learned `2^N` in a way no refusal dialog teaches. It also
satisfies AC-2 trivially and better than a bound+1 timing test does: an over-budget analysis
is never *startable*, so "refuses within a fixed time" is not a property to measure but a
structural fact. And it removes the artificial modal from the demo slice CAP-31 leans on.

## Alternative framing 3 — the budget belongs to the sink, not the extractor

One number governing the view (#644), the value (#641), and the batch surface (#646) is a
category error, and it pulls against a stated project direction. README sells
`ghcr.io/anadon/jls` as the headless surface "for autograders and CI"; CAP-09 (#306) exists
so *a grader who did not draw the circuit* gets a real verdict. Under #642's single bound, a
grading script streaming a table to a file is capped at whatever a human can scroll. Those
are different costs: a `JTable` at 2^20 rows is unusable at any speed, while a streamed
machine-readable export at 2^20 rows is a few tens of MB written once. #872 (TASK-C563-0)
already got the analogous call right — "the bound lives with the consumer, not here… a
40-input cone is a perfectly good cone" — and #642 should extend the same reasoning one level
down: per-sink budgets over one shared estimator, with the *view's* budget the smallest.

## Alternative framing 4 — the wall is much further out than the issue assumes

The issue treats `2^N` as a wall near the interactive horizon. It is a wall on *rows*, not on
*time*, and those separate by ~64×. A combinational cone evaluated bit-parallel — 64 input
assignments packed per `long` per net, one pass over the cone per 64-row block — turns 2^20
rows into ~16K cone passes. Whether to build that is a real decision with a real constraint
attached: ARCHITECTURE.md's recorded #221 decision makes the discrete-event interpreter the
*sole* simulation execution strategy, and binds any second evaluator to bit-for-bit agreement
with it. A bit-parallel enumerator is arguably such an evaluator. But #641's AC-1 already
mandates the exact differential oracle #221 demands (golden-tested against exhaustive
simulation), so the honest move is to say so in writing: either declare enumeration-by-
simulation the permanent strategy and accept a low bound, or declare a bit-parallel cone
evaluator an analysis pass whose #641 golden *is* its #221 equivalence proof. #642 currently
picks neither, which means whoever implements it picks silently — and the number they record
becomes a fact about an unexamined implementation choice rather than a stated decision.

## Overlap to resolve before this is worked

#872 was filed today and its AC-2/AC-3 now deliver named, distinguishable refusals for
sequential content and feedback, keyed off a registry-backed table with a totality test.
That is #642's AC-3 in full, done better. #642's AC-4 ("the rejection reason comes from
#306's extractor") already concedes the point. AC-3 should be rewritten as a *surfacing*
criterion — the extractor's typed refusal reaches the GUI and the batch stream without being
flattened into a string — or dropped and inherited. Left as is, two issues claim the same
diagnostic and the second one to land will reimplement or restate it.

## What I would keep unchanged

The outcome paragraph. "Never hangs", "shows the arithmetic", "a different answer for a
different failure" are all right, and the insistence that a structural rejection is not a
size rejection is the single most valuable sentence in the issue — it is the same
"no third state" discipline as KC-31-1. Keep the sentence; give it a type instead of an
adjective.
