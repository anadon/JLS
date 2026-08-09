# Issue #657: TASK-C566-1: the FSM-workflow parity assessment exists as a document — every Digital, DEEDS and Issie capability listed against what the shipped element does today
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue as filed

TASK-C566-1, band 1 mw, ordering_after `[510]`. Sub-task of feature #566
(FEAT-C31-4), itself a planned feature of capstone CAP-31 (#515). Deliverable:
a written document enumerating Digital/DEEDS/Issie's FSM design-workflow
capabilities against JLS's shipped `StateMachine` element (present/partial/
absent + citation), scoped to workflow (not layout, per #290's disclaimer),
landing in `docs/`. Boundary notes explicitly exclude closing or refusing
gaps (that's #658/TASK-C566-2) and state that a parity finding lets the
whole of #566 close verified on this document alone.

## Findings, most severe first

**1. The four-task decomposition #657 belongs to already overruns its own
parent's declared budget.** #566's frontmatter states `band_mw: "2-3"`. Its
four sub-tasks declare, respectively: #657 `band_mw: "1"`, #658
(TASK-C566-2) `"1-2"`, #659 (TASK-C566-3) `"1"`, #660 (TASK-C566-4) `"1"`.
Summed: 4-5 mw against a parent commitment of 2-3 mw — roughly double, even
in the best case. #657 is the first task in the ordering chain and the one
whose completion determines whether #658/#659/#660 run at all (per its own
boundary note), so it is the task that should have caught or flagged this
before being filed as "band_mw: 1" against a budget the whole chain cannot
fit inside. *Recommendation:* reconcile #566's band_mw upward, or cut a
sub-task's scope, before #657 starts — starting under a budget the
decomposition already can't honor invites either scope-cutting under
pressure or a silent overrun.

**2. The sibling chain has already assumed an outcome #657 is supposed to
discover neutrally.** #657's Outcome/Boundary notes frame parity-vs-gap as
an open question ("if it shows parity already, the feature closes..."). But
#659 (TASK-C566-3) AC-1 requires trace-equivalence tests for "at least one
Moore machine and **one Mealy machine**" — i.e., #659 was filed assuming
Mealy-mode output support will exist to test. Grounding against the actual
code: `State.Out` (`src/jls/elem/State.java:89`) binds outputs to states,
consistent with Moore-only semantics, and a repo-wide grep for
`Moore|Mealy` matches zero files under `src/` (only docs/reviews use the
terms) — no source evidence of a labeled or selectable Mealy mode today.
So the planning graph around #657 already presupposes at least one real,
specific gap (Mealy output) that #657's own document is nominally tasked
with discovering for the first time. The "assessment first, then decide"
framing in #657 is in tension with sibling tasks that were filed as if the
gap were already known. *Recommendation:* either #657 states plainly that
Mealy-mode absence is a near-certain finding (removing the pretense of a
fully open question), or #659 is revised to not presuppose it.

**3. The parity escape hatch is self-graded, high-leverage, and has no
pre-committed capability checklist.** Per the Boundary notes, a parity
finding in #657's document alone closes the *entire* #566 feature — skipping
#658's gap closures, #659's trace-equivalence tests, and #660's headless
wiring (3-4 mw of downstream work by finding 1's own numbers). AC-1 says
"every enumerated competitor capability has a row" — but the enumeration is
performed by the same document's author, with no external, pre-agreed
checklist (contrast #290's public `LayoutMetrics` threshold constants,
which fix the bar before the measurement runs). Given finding 2's evidence
that at least Mealy-mode output is very likely absent, and a quick read of
`src/jls/elem/State.java:121-133` shows `Transition` conditions are a single
signal tested for equality/inequality against one value (`signal`, `eq`,
`value`, `bits` — no compound/boolean expressions across multiple signals),
genuine parity looks unlikely — but nothing in #657 stops a narrow
enumeration (omitting Mealy mode and compound conditions from the "every
enumerated capability" list) from manufacturing it, and the incentive
(skip most of the remaining feature) points exactly that way.
*Recommendation:* fix the capability checklist (e.g., "graphical
state-diagram entry: y/n", "compound/boolean transition conditions: y/n",
"Moore output: y/n", "Mealy output: y/n", plus whatever DEEDS/Issie-specific
items the teardown surfaces) in the issue text or on #566/CAP-31 before
work starts, not after, and require it to be superset-complete against
#510's own listed items rather than assessor-defined.

**4. AC-1's parenthetical citation points to the wrong acceptance
criterion.** AC-1 reads "...a citation — a source file, a dialog, or an
explicit 'no such surface' (CAP-31 AC-4)." CAP-31's actual AC-4 (#515) is:
"The FSM parity assessment is a written document with each gap either
closed or refused by name." That is a description of the *disposition*
step — explicitly #658/TASK-C566-2's job, which #657's own Boundary notes
disclaim ("Assessment only. Closing or refusing the gaps is TASK-C566-2.").
Tagging #657's AC-1 (which only requires listing status + citation, not
closing or refusing anything) with "(CAP-31 AC-4)" misattributes the
authority: completing AC-1 does not satisfy CAP-31 AC-4 except in the
degenerate zero-gap branch, yet the citation reads as if it does.
*Recommendation:* drop the citation or replace it with the correct anchor
(CAP-31's Outcome/AC-1, which is about the analysis loop generally); leave
"(CAP-31 AC-4)" attached to #658's own AC-1 instead, where it already
appears verbatim and correctly.

**5. The workflow/layout boundary (AC-3) is not precise enough to block
misclassification.** AC-3 distinguishes "FSM *workflow* (entry, conditions,
output modes)" from "FSM *layout*," disclaiming #290 as adjacent, not part
of, this assessment. But #290 is about auto-layout metrics for *imported
HDL netlists* — a wholly different feature — while "state-diagram entry" as
a competitor capability (drag states onto a canvas, draw transition arrows)
is inherently graphical, i.e., it sits on the workflow/layout seam AC-3
draws no example for. `StateMachineDialog.java` (1929 lines) is
`JList`/dialog-based, not a canvas — so whether "no drag-and-draw diagram
canvas" gets recorded as an honest "absent" workflow gap, or gets waved off
as "that's layout, out of #290's — and therefore this document's — scope,"
is left to the same assessor incentivized by finding 3 to find parity.
*Recommendation:* add one worked example to AC-3 (e.g., "diagram-style
entry and transition-arrow drawing are workflow, in scope; automatic
node placement / wire-routing aesthetics of an already-drawn diagram are
layout, out of scope").

**6. DEEDS access/methodology is unaddressed.** #510 itself characterizes
DEEDS as "32-bit-Windows-only, closed, single academic" software; this repo
is worked in a headless Linux container with no stated path to install or
license DEEDS for hands-on verification. AC-2 requires citations "to their
own documentation or trackers, not to recollection" but does not say
whether DEEDS-specific claims may be sourced secondhand (its own
textbook/course docs, or #510's teardown) versus requiring direct tool
access — a plausible, undetectable methodology shortcut for exactly the
one competitor that is hardest to verify hands-on.
*Recommendation:* state explicitly that DEEDS capabilities may be cited to
its own published documentation without requiring a running instance, and
note the access constraint in the assessment document itself.

## What's solid

- Scope separation is well-designed: assessment (#657), disposition (#658),
  trace tests (#659), and headless wiring (#660) are cleanly split, and
  AC-4's "numbered gap list... so TASK-C566-2 can dispose of each item by
  reference" gives the handoff a concrete, checkable contract.
- AC-1's three-way status vocabulary (present/partial/absent) plus a
  citation requirement ("a source file, a dialog, or an explicit 'no such
  surface'") is concrete and falsifiable — a reader can check it against
  the repo, unlike a prose-only claim.
- The premise is corroborated, not fabricated: #510 independently names
  FSM tooling as a JLS competitive gap, and a direct look at the code
  supports it — `StateMachineDialog.java` is list/dialog-based with no
  diagram canvas, and no Mealy/Moore mode selector appears anywhere in
  `src/`.
- `ordering_after: [510]` is correctly scoped — #657 depends on the survey
  that motivates it and nothing else.

## Verdict rationale

`needs-rework`: the assessment-first, document-as-deliverable design is
reasonable in principle, but three compounding problems make it unsafe to
start as filed — the sub-task chain's combined budget already exceeds its
own parent's (finding 1), a sibling task has pre-assumed a specific gap
finding that undercuts the "open question" framing (finding 2), and the
zero-code parity escape hatch is graded by the same party who would benefit
most from finding it (finding 3), with no pre-committed checklist to keep
that honest. Findings 4-6 are smaller correctness/precision defects worth
fixing in the same pass. None of this blocks the underlying goal — a
capability-by-capability FSM parity document is a sound idea — but the
acceptance criteria as written let the document's own author decide how
much of #566 the rest of the team has to do.
