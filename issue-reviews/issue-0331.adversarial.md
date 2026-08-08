# Issue #331: FEAT-049: a student draws analog devices the way they already draw gates, and the circuit they drew converges
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a coordination/planning issue (a "feature" tracker over three unfiled
tasks plus an unfiled "residual") for adding an entire SPICE-class analog
solver's *consumer surface* — device models, a drawn palette, convergence
hardening — to a single-maintainer, GPLv3, pure-Java, digital logic teaching
tool. The prose is dense with git-grep-verified line citations, which makes
it look more rigorously grounded than it is: two of its own load-bearing
evidence anchors do not resolve in this repository at all, none of its three
child tasks are filed, and its own cost accounting admits an unreconciled
3.5–5.5× gap. As filed, it is not something an engineer can pick up and
execute; it is a scope map whose scope, cost and provenance are each
internally contested.

## Findings, most severe first

**1. The evidence commit the entire issue is pinned to does not exist in this repository.**
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` appears in the
machine block and every "ABSENT at 2d0ca9d" claim cites it. `git cat-file -e
2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails ("could not get object
info"), and `git log --all` (268 commits) contains no matching or
partially-matching hash. The issue's rhetorical authority rests entirely on
"verify rather than trust" style citations (`git grep`, exact line numbers),
but the one thing that would let a reviewer actually replay those commands —
the commit itself — is unreachable. Recommendation: either the hash is
wrong/needs correcting to a real commit in this repo's history, or these
issues were authored against a different repository state that was never
pushed here; either way, re-anchor every "ABSENT at 2d0ca9d" claim to a
commit that resolves in `anadon/jls` before this issue is treated as
verified. (Note: I independently re-derived several of the numeric claims
against current HEAD — 35 `ElementRegistry` types, 32 `Palette.java` rows,
8 `instanceof LogicElement` sites across 6 files, and the exact text at
`Simulator.java:196-198` / `WireNet.java:507` — and they do check out today,
which is reassuring but does not fix the provenance problem: those matches
could be coincidence of an unchanged codebase, not evidence the cited
commit ever existed.)

**2. A document quoted as authoritative does not exist anywhere in the repo.**
The issue quotes `docs/virtual-hardware-parity.md:1909-1914` verbatim as
"the repository's own parity document" stating the 32-vs-35 palette
arithmetic and the "K9" pedagogy-floor consequence. `find` over the entire
tree turns up no file named anything like `virtual-hardware-parity`
anywhere, and `docs/` has no such content. Likewise `docs/plan/evidence/`
(cited for `BRIEF.md` §11 and `analog-determination.md`, both supplying the
cost band, D9, D-A1 and D-A7 quotes this issue and its siblings #351/#368
lean on) does not exist — `docs/plan/` is entirely absent, though a
same-named-in-spirit `docs/capability-roadmap/` does exist. Any reader
relying on this issue's citations to check its numbers independently will
hit a dead end on exactly the passages doing the most rhetorical work
(the D9 "progressive disclosure" framing, the cost band derivation).
Recommendation: attach or link the actual documents, or drop the quotes
and inline the (verifiable) reasoning instead.

**3. Zero of this issue's actionable work is filed.** `requires_tasks: []`,
and all three `planned_tasks` (TASK-0103, -0104, -0105) plus the
"residual" (fourth permit, port widening, datum element, first eight
devices) are explicitly "not filed, no id". The Decomposition table (§2)
and Open Question 4 ("which three dialogs are bespoke? … **Blocks filing
children**") both concede that filing is itself blocked on unresolved
decisions. So today there is no child issue anyone could be assigned, and
this parent cannot be worked directly — its own Completion Criteria require
`planned_tasks` to be empty by resolving to filed issues, which is circular
with respect to its current state. Recommendation: do not treat #331 as
"ready" for any engineering effort until at minimum the residual and
TASK-0105 are filed with concrete acceptance tests; right now closing this
issue and closing "doing nothing" are behaviorally indistinguishable from
outside.

**4. Acknowledged, unreconciled cost blowout (Open Question 2).** The issue's
own task rows sum to 6 maintainer-weeks (TASK-0103 + 0104 + 0105 at 2 wk
each) against a stated band of 21–33 weeks — "the band exceeds the row sum
by 3.5x at the low edge and 5.5x at the high edge. Both figures are printed
and no row is adjusted to make the band true." That is honest, but it also
means the only two numbers offered for planning purposes disagree by more
than a factor of five, with the gap attributed to a "residual" that "carries
the difference because no task id names it" — i.e., unscoped and unpriced
by design. Any downstream capstone (#303, #305, #309) or scheduling
decision that reads "6 wk" or "21-33 wk" off this issue is reading an
unresolved number. Recommendation: do not use either figure for
scheduling until Open Question 2 is actually closed, not merely
"printed".

**5. Scope is a large, undifferentiated commitment for a stated
single-maintainer project.** ARCHITECTURE.md's own recorded-decisions
section describes JLS as "a single-maintainer pedagogy tool" (used to
justify declining i18n work as too costly). #331 commits that same
maintainer to: a fourth element permit, a widened `Put.element` type across
35 element classes, a node-partition/datum contract, a view-dimensioned
palette rewrite, ~22 analog device types (bipolar + one MOSFET level + one
JFET + resistors/etc.), vendor `.subckt` model ingestion "read as data", a
200-circuit convergence corpus with a ratcheted non-convergence rate, and a
generic device dialog/renderer — and that's explicitly the *reduced* scope,
with "the remaining fourteen device types" and the higher MOSFET level
deferred. It depends on #351, which is itself unfiled at the task level and
proposes to "absorb" source from ngspice, XSPICE, Sparse1.3, SpiceSharp and
CircuitJS1 — a real license-compatibility question (mixed
BSD/MIT/custom/GPL sources folded into a GPL-3.0-or-later Java codebase)
that #351 defers to an undefined "D8" judgment call rather than resolving.
Recommendation: given Open Question 5 in #331 itself is literally
"Ownership. UNOWNED", get a maintainer commitment in writing before any
child is filed — the ARCHITECTURE.md i18n precedent suggests this
maintainer declines large, ongoing-tax scope expansions by default.

**6. Global invariant 5 is a universally-quantified claim tested by only two
fixtures.** "No student-reachable path emits a matrix-singularity message"
is stated as an invariant over *every* drawn circuit, but I3's evidence
plan only tests "no datum" and "disconnected sub-regions" — the two
failure modes MNA textbooks describe as *node*-partition problems. It does
not obviously cover other classic singularity sources (a loop of ideal
voltage sources, a floating capacitor-only island, a device parameterized
to a degenerate operating point, a shorted independent source pair). A
future contributor could satisfy I3 exactly as written while a student
still hits a raw matrix-singularity message from some other topology —
passing the test without meeting the invariant it's supposed to witness.
Recommendation: either bound global invariant 5 to the two named failure
modes explicitly, or expand I3 to a fuzzed/generated-topology fixture set
before this is accepted as "done".

**7. Unsourced precision smuggled in among verified numbers.** Most
quantitative claims here are backed by a `git grep`/line-number citation
(and several check out against current HEAD, per Finding 1's aside). But
some are bare assertions with no citation at all: "roughly 8,250 source
lines and about 71% of the measured addable-uncovered line commons" and
"about 11.7%" for the generic-dialog cost argument (§2 alternative 1), and
"one capstone circuit runs at 2.00 Newton iterations per timepoint with one
rejection in 10,012, and a diode bridge with an astable runs at 20.4
iterations with 15.1% rejection" (§3, the "measured spread" used to justify
the convergence-hardening stage as multi-week). Nothing in this feature or
#351 exists yet to have produced those Newton/rejection numbers — there is
no solver in the tree. The document's own convention (verified-commit
citations for everything else) makes these two figures stand out as
unfalsifiable set-dressing dressed in the same authoritative tone as the
verified claims. Recommendation: either cite where these numbers came from
(a prototype branch? a different simulator entirely?) or drop the false
precision.

**8. Self-flagged contradiction left unresolved, not merely deferred.**
"The higher MOSFET level. Deferred on measured grounds... That contradicts
an earlier brief and belongs in #309's open decisions, not this feature's
scope." The issue proceeds as though scope is settled while admitting a
live contradiction with prior direction, and punts resolution to a
capstone issue (#309) that #331 itself only serves, not owns. Anyone
scoping TASK-0103 off this issue inherits an undecided model boundary.
Recommendation: resolve or explicitly gate TASK-0103's filing on #309's
decision, rather than listing the deferral as settled scope here.

## What's solid

- The hierarchy-admission argument (Finding: sealed `Element`/`Put`, the
  blind `(Reacts)` cast at `WireNet.java:507`, the initialisation walk at
  `Simulator.java:196-198`) is accurate against current HEAD, and correctly
  identifies a real type-safety gap: an analog element riding the reacting
  hierarchy today would indeed be silently seeded and pin-changed.
- The palette-totality arithmetic (32 rows vs. 35 registry types,
  `PaletteContractTest.java:48`'s `NON_PALETTE_TAGS` exemption set) is
  exactly right against current HEAD, and the "ship the view dimension
  before registering any analog type" sequencing rule it derives is sound
  given that constraint.
- The `blocked_by`/`blocks` mirroring between #331, #351 and #368 is
  internally consistent (checked directly): #351 `blocks: [331, 368]`
  matches #331 `blocked_by: [351]` and #368 `blocked_by: [331, 351]`; #331
  `blocks: [368]` matches #368's mirror. The DAG claim holds for the edges
  actually present.
