# Issue #610: TASK-C556-2: the dropped set equals the reported set, asserted once in shared infrastructure — and the .circ report round-trips through the shared schema losslessly
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the task framing and the end is one thing: **an instructor can believe the
migration report.** If the report says nothing else was lost, nothing else was
lost. #323 says this in plain words — the report is "what converts a partial
import from a failure into a work list" — and #451 says the equality "is the
feature, and it is the easiest thing to fake."

#610 is right that this trust should be manufactured once rather than per format.
It is wrong about *what* to manufacture, *where* it lives, and *when*.

## The load-bearing flaw: the equality is self-referential

`Reported = Seen \ Realized`. Ask where each set comes from. In #451's §7.10,
`Seen` is "the multiset of construct kinds **the reader encountered**" and
`Realized ⊆ Seen` is "those **it** turned into JLS elements." Both are counters
the importer increments as it walks its own mapping table. `Reported` is what the
same walk wrote down.

So the equality holds automatically for the failure mode that actually matters:
a construct the importer's author never contemplated is not counted into `Seen`,
is not realized, and is not reported — and the assertion is green. The property
is *self-consistency*, not *soundness*. It is armed against bookkeeping slips and
blind exactly where losses actually occur.

AC-2 senses this danger and misdiagnoses it. A mutation test proves the *test* is
armed against changes to code the test already covers; it cannot make two sets
derived from one traversal independent. Mutating the report writer kills mutants
in the report writer. It says nothing about the construct that never entered the
traversal.

Generalizing this shape into shared infrastructure — AC-1's whole point — does not
fix it. It industrialises it, and then #558/#559/#561/#562 inherit a green light
that means less than it reads.

## Alternative A: two oracles, neither of them the mapper

Make both sides of the equality come from outside the mapping code.

**Left side — a construct census over the parse substrate, not the mapping table.**
Enumerate everything syntactically present, before any mapper sees it: for XML,
every element node and every attribute; for JSON, every object key path; for
Falstad, every line and token. Thirty lines per substrate, and — critically —
written against the parser, so a construct nobody thought about still lands in
`Seen`. Note this is *three* substrates for five formats (`.circ` and `.dig` are
both XML), so it shares better than the per-importer parameterisation of AC-1.

**Right side — realization read off the artifact, not off a counter.** #451's own
O3 is the proof this is necessary: `Element.setValue` falls off the end of its
attribute loop (`src/jls/elem/Element.java:344-351`, verified at HEAD) and
`Circuit.load` calls it unconditionally, so a construct the reader believes it
realized can vanish during load with `load()` returning `true`. Derive `Realized`
from the re-loaded `Circuit` (or the emitted save text), which every importer
produces anyway. One realization oracle, shared by all formats, no parameterisation.

Then the equality reads: *the file, the artifact, and the report agree.* That is
the claim an instructor needs, and it is falsifiable by a fixture the importer's
author did not anticipate — which the current form is not.

## Alternative B: delete the round-trip by never taking the detour

AC-3 and AC-4 exist only to protect a sequencing nobody has committed to. The
`.circ` report **does not exist**: #451 is unstarted and blocked on #404, and
`src/jls/imp/` is absent from the tree. `src/jls/hdl/imp/ImportSummary.java` — the
only report-shaped thing that exists — is element *counts* and an `x`-coercion
tally, and names no loss at all.

So AC-3 proposes to prove that a conversion is lossless between two artifacts that
are both unbuilt, scheduled a fortnight apart, and written by the same maintainer.
Reverse the order instead: land #608's four-field record first (it is a `record`
and a document — days, not weeks) and make #451 its first consumer, natively.
AC-3's field-by-field round-trip and AC-4's "mapping decisions unchanged"
discipline both evaporate, because there is no carrier to move off and no second
pass over the mapper. `ordering_after: [TASK-C556-1, 323, 451]` should become
"#608 before #451", and this task loses two of its four criteria.

## Alternative C — the big one: this is a loader property, not an importer property

The strongest reframing is to notice that JLS already has this exact machine and is
about to build a second one beside it.

`LoadError` (`src/jls/LoadError.java:38-66`) is a **closed category taxonomy**
carrying location, detail, and an actionable hint, published through one channel
"so every front end shows the same message" (ARCHITECTURE.md). #608's schema —
construct, disposition, location, explanation, with disposition a closed vocabulary —
is the same animal with one field renamed. A `LoadError` is the terminal case
(nothing loaded) of what a report entry is (this construct did not load).

And the native loader is the biggest silent dropper in the tree.
`docs/file-format.md:220-222` is normative that "unknown attribute names are
silently ignored… the value is dropped without error," deliberately, as the
format's forward-compatibility valve. #404 wants to make that loud; #609 works
around it with must-understand sections; #451's O3 is blocked on it.

If the totality property is installed in *importer* infrastructure, then the one
read path every user takes every day — opening a `.jls` file — is exempt from it.
If instead the seam is **"a reader, its result, and its ignore-list"**, then `.jls`,
`.circ`, `.dig`, `.cv`, Falstad and Yosys JSON are all readers with loss reports,
the assertion is inherited by the native format, and the file-format valve stops
being an exception and becomes a *disposition* — `dropped-by-design`, which is
already in #608's vocabulary. That single move subsumes #404, #408, #451's report,
#608, #610 and four consumers onto one contract, and it is the direction #556
already gestures at by naming #314 (fail-loud loader) a hard prerequisite.

## The abstraction is being cut before there is anything to abstract over

Zero importers today emit a loss report. #610 proposes the shared, parameterised
assertion before importer #1 exists. #556's own boundary comment already names the
right moment: #561 (Falstad) is "the *falsification test* for this issue's contract
against a non-XML, non-JSON source." Write the assertion concretely inside `.circ`,
and let #561 be where it generalizes — with a real second shape in hand, the seam
gets cut where the two importers actually differ rather than where one importer
happened to put it.

The schema (#608) is the opposite case and should stay up front: vocabularies are
cheap to agree and expensive to unify later. Schema early, assertion late.

## AC-2 collides with a recorded apparatus decision

`pom.xml`'s `pitest` profile scopes `targetClasses` to `jls.sim.*`,
`jls.BitSetUtils`, `jls.Util`, `jls.SpatialIndex`, `jls.collab.op.*` — no importer
package is in scope — behind global floors (`mutationThreshold` 80,
`testStrengthThreshold` 82) on a weekly workflow that is never a required PR check.
`docs/mutation-testing-trial-2026-07.md`'s addendum is explicit that a scope shift
makes the numbers non-comparable. Discharging AC-2 therefore means either
perturbing a ratchet with a recorded climb discipline, or hand-writing two negative
fixtures — one drop-without-report, one report-without-drop, each asserted to fail.
The second is the honest thing and #451's P2 fixture ("three deliberately
unmappable components") already nearly supplies it. Call it a negative-test pair;
do not invoke the mutation apparatus for it.

## What I am disregarding, and why

I am setting aside **AC-3 and AC-4** entirely. They are the cost of a two-step
migration that should not be taken (Alternative B). I am also setting aside
**AC-2's mutation-test clause** as specified — it arms the wrong vacuity and
collides with the pitest scope decision.

**AC-1 I would keep but relocate**: one assertion, yes — but over independently
derived sets (Alternative A), sited at the read seam rather than an importer seam
(Alternative C), and generalized at the second consumer rather than the zeroth
(#561).

## Verdict rationale

`rethink`, not `endorse-with-reframing`: the goal is right and the route is wrong,
but the defect is in the premise, not the plan. An equality whose two sides are
produced by one traversal is not the property the issue believes it is asserting,
and shipping it as shared infrastructure propagates a false green to four
downstream importers. Reformulate the property first (independent oracles), fix the
ordering so the detour never happens (#608 → #451, schema-native), and decide
deliberately whether the seam is "importers" or "readers, native format included" —
because that choice is worth far more to JLS's arc than this task's stated scope.
