# Issue #659: TASK-C566-3: a machine entered as states and transitions simulates to the same trace as its hand-built register-and-logic equivalent
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue as filed

TASK-C566-3, part of feature #566 (FEAT-C31-4, itself PF-4 of capstone
CAP-31 / #515). It sits after TASK-C566-2 (#658, gap disposition) in the
`ordering_after` chain and before TASK-C566-4 (#660, headless callability).
The deliverable is a differential test harness: simulate a state machine
entered as states/transitions alongside a hand-built register-and-logic
circuit for the same machine, and assert cycle-identical traces, with a
fixture per gap #658 records as closed.

## Findings, most severe first

**1. AC-1 demands a Mealy-machine fixture the shipped element cannot
represent, and nothing in the issue accounts for that dependency being
unmet.** AC-1: *"For at least one Moore machine and one Mealy machine, the
states-and-transitions form and the hand-built equivalent produce identical
traces over a shared stimulus."* But the state-machine element's output
model is Moore-only today: `State.Out` (`src/jls/elem/State.java:89-101`)
attaches `signal`/`bits`/`value` to a *state*, not to a transition or an
input condition, and `State.sendOutputs` (`State.java:1315` on) iterates
only `outs` per current state — there is no output type conditioned on the
transition taken or the current input. `HdlModel` names the concept
directly: `public record MooreOutput(...)` (`src/jls/hdl/HdlModel.java:749`)
and `public final List<MooreOutput> outputs;` (line 791) — Mealy is not a
representable case anywhere in the export model either. So a "Mealy
machine" entered in "the states-and-transitions form" does not exist yet;
AC-1 is only satisfiable if TASK-C566-2 (#658) both (a) identifies
Mealy-output support as a gap and (b) actually closes it (not refuses it,
not defers it to a follow-up task under #658's own AC-2 escape valve for
gaps too big for one PR). #659 treats this as a given ("for at least one
... Mealy machine") rather than as a contingency, and has no fallback
clause analogous to AC-2's "recorded reason why it is not trace-observable"
for the case where Mealy support simply isn't there when this task is
picked up. *Recommendation:* make AC-1's Mealy clause conditional — "if
#658 closes Mealy-output support; otherwise record why the clause is
inapplicable" — mirroring the escape hatch AC-2 already grants for
non-trace-observable gaps.

**2. The AC-1 citation "(CAP-31 AC-3)" points at the wrong acceptance
criterion — an internal contradiction with the very issue it cites.**
CAP-31 (#515) AC-3 reads: *"Bounds are stated and enforced: above N inputs
the tool refuses with the arithmetic, never hangs"* — a truth-table/
minimization input-count refusal criterion for PF-1/PF-2, unrelated to FSM
trace testing. The FSM-relevant criterion in #515 is AC-4: *"The FSM parity
assessment is a written document with each gap either closed or refused by
name."* Neither AC actually asserts anything about trace-identical
simulation — that requirement exists nowhere in CAP-31 itself, so the
citation is not just off-by-one but attached to a clause about a different
planned feature entirely. A reader following the citation to verify this
task traces back to its capstone will find an unrelated bound-refusal
requirement and reasonably conclude the citation was never checked.
*Recommendation:* fix the citation to CAP-31 AC-4 (the closest actual
match) or drop the parenthetical if no CAP-31 AC really underwrites this
specific claim.

**3. AC-2's escape hatch ("or a recorded reason why it is not
trace-observable") is wide enough to swallow the whole task, and there is
no rubric for what counts as an adequate reason.** Given finding 1, and
given that #658's disposition can be an editor-only UI change (dialog
layout, diagram entry, expression editor ergonomics — none of which touch
`react`/`getNextState`/`sendOutputs`), a plausible and likely outcome is
that most or all gaps #658 actually closes are GUI-workflow gaps with no
simulation-trace signature at all — the same failure mode #566's own AC-3
already has (see the sibling review of #566, finding 3: "states and
transitions already compile to simulated register-and-logic behavior
today... the named competitive gaps... are workflow gaps... not
simulation-correctness gaps"). If that holds, AC-2 is satisfied by writing
"not trace-observable" next to every closed gap, and this task lands with
zero new fixtures beyond whatever AC-1 forces — which itself may reduce to
a single Moore-only pair if finding 1 goes unresolved. Nothing here defines
what makes a "not trace-observable" claim checkable versus a convenient
dodge. *Recommendation:* require the "not trace-observable" reason to name
the specific UI/model surface the gap touched (e.g. "closed via
`StateMachineDialog` layout change; no `State`/`Transition`/react change"),
checkable against the actual diff, not a free-text sentence.

**4. AC-4 (divergence reporting: cycle, signal, both values) has no
verification mechanism and is dead code on a passing suite.** A test
harness only exercises its own failure-reporting path when it fails.
Nothing in the acceptance criteria requires a test-of-the-test (e.g. an
intentionally-mismatched fixture asserting the failure message contains
cycle/signal/both values) the way this codebase does elsewhere for
similar meta-guarantees (`docs/batch-interface.md` names golden tests per
clause; `LookAndFeelPolicyTest` tests the fallback path itself, not just
the happy path). As written, AC-4 could be "implemented" by writing a
comparator that would produce such a message if it ever ran, and never be
exercised at all — an implementer or reviewer has no way to confirm the
message format actually holds without deliberately breaking a fixture by
hand outside the suite. *Recommendation:* add one self-test fixture with a
deliberately injected single-cycle divergence and assert the failure
message's shape (cycle number, signal name, both values) directly.

**5. The #658 dependency is asserted only in freeform YAML, not a
structural GitHub link — the same gap already flagged on sibling task
#660.** `ordering_after: ["TASK-C566-2 (the closed gaps this pins)"]` is
prose in a fenced code block; `issue_read` on #659 reports `has_parent:
false, has_children: false`. Nothing stops this task from being picked up
before #658 has disposed of any gap, at which point AC-2 is vacuously
unsatisfiable (there is no "TASK-C566-2 recorded as closed" list to
iterate) rather than failing loudly. *Recommendation:* use GitHub's native
issue-dependency links (or at minimum a pinned "blocked by #658" line) —
this is the identical fix already recommended for #660's #657/#658
dependency.

**6. AC-3's "not generated by the same code path under test" is sound in
principle but unenforced.** Nothing requires the hand-built circuits to be
checked in as committed `.jls`/circuit-text fixtures reviewed independently
of the generator, versus, e.g., a helper method that assembles registers
and logic elements programmatically inside the same test class that also
drives the `StateMachine` element — which would technically be "not the
same code path" (no `StateMachine`/`State` classes involved) while still
being authored by the same PR under the same time pressure as the fixture
it is meant to check against. This is a milder version of finding 3: a
good rule with no independent-review mechanism behind it.
*Recommendation:* require the hand-built circuit fixtures to be reviewed
(or built) by inspecting the FSM's truth table independently, e.g. citing
`HdlExporter.buildStateMachine`'s already-existing state-encoding/
transition-table output (`src/jls/hdl/HdlExporter.java:866`) as the
worked-out reference rather than hand-deriving register logic from scratch
each time, which reduces (but doesn't eliminate) the risk of correlated
errors between the two "independent" implementations.

## What's solid

- AC-3 (committed, independently-built hand equivalents rather than
  generator-produced ones) is the right instinct against circular
  self-validation — see finding 6 for the gap in enforcing it.
- The Boundary note correctly disclaims #290 ("Layout of the FSM is #290's
  golden, not this harness") — #290 is an HDL-import layout-quality golden
  for a wholly different subsystem (`src/jls/hdl/layout/`), and this task
  does not conflate the two.
- Anchoring "closed gap → fixture" to TASK-C566-2's disposition list (AC-2)
  is a coherent traceability design in principle, mirroring how #660 (task
  4) anchors its own scope to the same upstream documents — the pattern is
  consistent across the C566 sub-issues even where (see finding 1, 3, 5)
  the specific wiring has gaps.
- band_mw "1" is at least plausible in isolation for a Moore-only
  differential-trace harness with a couple of fixtures — the estimate risk
  here is smaller than sibling task #660's fully-undefined-scope estimate,
  precisely because AC-1 gives (Mealy caveat aside) a concrete minimum
  scope rather than an open "whatever the assessment says" clause.

## Verdict rationale

`needs-rework`: the task's own headline acceptance criterion (AC-1) commits
to testing a machine type — Mealy — the shipped element cannot currently
express, with no contingency if TASK-C566-2 doesn't deliver that capability
in-scope; its citation to CAP-31 AC-3 points at an unrelated requirement,
suggesting the cross-reference was never checked against the parent issue;
and its two most load-bearing guarantees (AC-2's non-observability escape
hatch, AC-4's divergence-reporting format) have no rubric or self-test
distinguishing genuine coverage from a plausible-sounding shortcut. Fix
items 1 and 2 before work starts (they are checkable against the repo
today, at zero investigation cost); items 3-4 need a rubric/self-test
addition before the harness is trusted as a regression gate; items 5-6 are
process hygiene worth doing but not blocking.
