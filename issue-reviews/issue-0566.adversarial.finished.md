# Issue #566: FEAT-C31-4: the shipped state-machine element measures up to Digital/DEEDS/Issie's FSM design workflow — every named gap closed or refused in writing
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue as filed

Child of CAP-31 (#515, PF-4), itself justified by the niche survey #510 §2.
Deliverable is a written parity assessment of `StateMachine`'s design
workflow against Digital/DEEDS/Issie, with every named gap either closed
or explicitly refused, plus a verified-close escape hatch (KC-31-2) if the
assessment finds parity already exists. Band: 2-3 mw.

## Findings, most severe first

**1. AC-4 silently weakens the parent capstone's binding acceptance
criterion — an internal contradiction, not just a restatement.**
CAP-31 (#515) AC-5 states unconditionally: "All analysis is headless-callable
(batch flag), so graders can use it." Issue #566's AC-4 reads: "Whatever FSM
analysis exists after closure is headless-callable **where the assessment
says it should be**" (emphasis on the added qualifier). That qualifier turns
a fixed, external requirement into one the assessment document gets to
define for itself. Since the assessment is written by whoever does this
work, AC-4 can be satisfied by writing "no analysis in this closure needs
headless access" — trivially true if no analysis is added — while CAP-31's
own AC-5 is left unmet. A sub-issue's AC should narrow or implement the
parent's AC, not grant itself discretion to opt out of it.
*Recommendation:* either drop the qualifier and inherit CAP-31 AC-5 verbatim,
or, if a carve-out is intended, state it explicitly and get it ratified on
#515, not silently introduced here.

**2. The verified-close escape hatch (KC-31-2) is graded by the same party
who writes the grading document — no independent check on "parity."**
"If the assessment shows the existing state-machine element already at
parity, this feature closes as verified with the document as the artifact —
the document, not new code, is the mandatory deliverable." There is no
external rubric (unlike, e.g., #290's `LayoutMetrics` public threshold
constants) defining what counts as parity. The assessor chooses which
competitor capabilities to enumerate (AC-1), decides whether each is "at
parity," and can write "refused, out of scope" for any gap under AC-2 with
no bar on what counts as an adequate reason. Given the 2-3 mw budget and the
zero-code closure path, the path of least resistance is an assessment that
finds convenient parity and cheap refusals for everything else — the
opposite of the "no silent omissions" language in AC-2. Confirmed against
the actual code: `StateMachineDialog.java` (1929 lines) is a `JList`/dialog
editor for states, outputs, and transitions — text-and-list based, not a
graphical state-diagram canvas with drag-drawn states/arrows the way
Digital and Issie present FSM entry. A cursory assessment could still
plausibly rate "state-diagram entry" as a wash by redefining the bar
loosely enough. *Recommendation:* CAP-31 (or this issue) should fix an
explicit, named capability checklist up front (e.g., "graphical
state/transition diagram entry: yes/no", "transition-condition expression
syntax: yes/no", "Moore output: yes/no", "Mealy output: yes/no") so the
assessment is checked against a pre-committed list, not one authored
after the fact by the same person grading it.

**3. AC-3's verification mechanism does not test the class of gaps the
issue is actually about.** AC-3: "Closed gaps carry tests: a state machine
entered as states/transitions simulates to the same trace as its
hand-built register-and-logic equivalent." That is a simulation-equivalence
test — and `StateMachine.react`/`initSim` (`src/jls/elem/StateMachine.java`
lines 674-811) already does exactly this: states and transitions already
compile to simulated register-and-logic behavior today, for every existing
machine. The named competitive gaps this issue exists to close are workflow
gaps — graphical diagram entry, transition-condition syntax, Moore/Mealy
selection — not simulation-correctness gaps. A change that adds a
graphical FSM canvas produces no new simulation trace to compare (the
underlying `State`/transition model is unchanged), so AC-3 cannot exercise
or fail on the actual deliverable for that class of gap. As written, AC-3
can be satisfied while the workflow gaps it's meant to validate remain
wholly unaddressed. *Recommendation:* add a UI/model-level acceptance
criterion per closed workflow gap (e.g., an editor-model test that a
diagram-entered machine round-trips to the same `State`/transition set as
the list-entered equivalent), not only a simulation-trace test.

**4. Scope is asymmetric and effectively unbounded on the "close" side
while capped at 2-3 mw.** The Outcome section commits to a diagram-style
design workflow ("designs a state machine as states and transitions — not
gates"), but the current implementation (`StateMachineDialog`, list/dialog
based; no canvas) is closer to Digital's/Issie's older, less visual
entry modes than to their current drag-and-drop diagram editors. Actually
building graphical state-diagram entry (canvas, drag states, draw
transition arrows, live editing) — the single most load-bearing named gap
against Digital and Issie — is realistically a multi-week UI feature on
its own, not a fraction of a 2-3 mw budget shared with the write-up and
three competitor teardowns. The issue gives no fallback if the true
gap-closure cost blows the budget beyond "refuse by name" (finding 2),
which means budget pressure and the refusal escape hatch point the same
direction: toward writing gaps off rather than closing them.
*Recommendation:* pre-declare in the issue which named gaps, if found, are
in-budget to close (e.g., only Moore/Mealy mode toggle) versus which are
expected to be refused up front (e.g., full graphical canvas, deferred to
a follow-up feature), rather than deciding that under time pressure during
the work itself.

**5. Hands-on assessment of DEEDS is not obviously feasible in this
environment, and the issue doesn't say documentation-only assessment is
acceptable — but nothing stops it either.** #510 (§3 table) itself
characterizes DEEDS as "32-bit-Windows-only, closed, single academic."
The repo here is worked in a headless Linux container; there is no
indication DEEDS is installable or licensable for hands-on comparison.
AC-1 demands "the existing element's status against each [named
capability]" without specifying whether the competitor capability list may
be sourced secondhand (from #510's teardown, or DEEDS's own textbook
docs) versus verified hands-on. Given finding 2 (self-graded document), a
documentation-only pass for DEEDS specifically is a plausible and
undetectable shortcut, weakening the "measures up to" claim in the title
for one of the three named competitors. *Recommendation:* state explicitly
whether DEEDS parity claims may be sourced from #510/its cited public
material versus requiring direct tool access, and note the access
constraint if the latter is infeasible.

## What's solid

- The `ordering_after: []` and "#290 is adjacent evidence, not this
  workflow assessment" note is accurate: #290 is a layout-quality golden
  task for imported HDL netlists (bounding-box/crossing metrics), wholly
  unrelated to FSM design-workflow parity — correctly disclaimed rather
  than wrongly cited as a dependency.
- The verified-close path (KC-31-2) is inherited faithfully from CAP-31
  and is a reasonable capstone-level pattern in principle (see finding 2
  for why its execution here lacks a rubric).
- Grounding the feature in #510's competitive teardown is legitimate:
  #510 independently names FSM tooling as a JLS gap versus Digital/DEEDS/
  Issie, and the current `StateMachine`/`StateMachineDialog` code
  corroborates the gap (list-based entry, no diagram canvas).
- band_mw (2-3) and feat_id correctly trace back to CAP-31's PF-4 entry;
  no numeric drift found between the two documents there.

## Verdict rationale

`needs-rework`: the core problem is not that the assessment-first approach
is wrong (KC-31-2's verified-close path is sound as a capstone pattern),
but that this issue as filed hands the same party both the measuring
stick and the grade, without a pre-committed capability checklist, without
AC-3 covering the class of gap actually at stake, and with AC-4 quietly
loosening a binding parent-capstone requirement. Fix items 1-3 (pin the
checklist, restore AC-4 to CAP-31 AC-5, add a workflow-level test
requirement per closed gap) before work starts; items 4-5 are scope/process
notes worth resolving but not blocking.
