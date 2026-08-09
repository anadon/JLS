# Issue #787: TASK-C570-1: each Digital-wishlist item gets its D10 path-and-cost justification in writing before any of it is implemented
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Strip the vocabulary and #787 is a discipline device: *do not build a feature because
somebody else's users wanted it.* That instinct is correct and it is one JLS already
practices well — ARCHITECTURE.md's "Recorded decisions" section is a live, working
example of exactly this genre (i18n declined, dark default deferred, plugin loader
removed, second simulation strategy refused), each with a rationale and a **revisit
trigger**. The instinct is not in question. The vehicle is.

My objection is that #787 builds a bespoke, one-off governance artifact for three
items, at a moment when it can no longer change any outcome, in a dialect nobody
outside the author can evaluate — and it does so for a capstone (#514 CAP-30) whose
whole stated purpose is making JLS legible to outsiders.

## 1. The gate arrives after the decision it is supposed to gate

#787 says the justification is written "before any of it is implemented," and #788/#789
carry `ordering_after: [TASK-C570-1]`. But #788 and #789 already exist, already name the
implementation path (`Action`/`EditOp` layer, `MenuAcceleratorPolicy`, `UserPrefs`),
already carry cost bands (2–3 mw, 1.5–2 mw), and #570's dedup comment already adjudicated
the design against #75/#76/#286/#593/#594/#596. #570's own Outcome asserts the conclusion:
"Each stands on its own JLS merit under D10 path-and-cost." A gate whose verdict is
pre-published on the parent, and whose subject issues are already fully specified with
acceptance criteria, cannot cut anything. AC-2 ("any item whose justification does not
stand on JLS merit is cut") has no reachable path to firing. This is ratification, and
0.5 mw of ratification is 0.5 mw that AC-2 of CAP-30 (the good-first-issue funnel) would
spend better.

## 2. #592 already is the machine this issue rebuilds by hand

#592 (FEAT-C37-1) is a **published, cited, graded catalog** of ergonomic behaviours scored
against Logisim-Evolution, Digital and CircuitVerse: one row per behaviour, a citation to
the originating complaint in the incumbent's tracker, a HAVE/GAP/REFUSE grade, a prose
reason mandatory on every REFUSE (AC-2), a funding score, and a stop-loss column (AC-3).
That is precisely the schema #787 describes in prose: path, cost, beneficiary, and a
recorded cut with its reasoning.

#787's AC-4 asks to "reconcile ownership overlap with CAP-37's parity catalog … before task
work starts" — but #592's boundary notes already did that reconciliation, in writing:
"the Digital-wishlist headline items … are owned by #570 and are catalogued here only as
HAVE-elsewhere cross-references, never re-funded." So AC-4 asks for a reconciliation that
exists, and AC-1 asks for a document whose format exists one directory over.

**Concrete alternative (process):** delete the standalone deliverable. The dive and the
keybinding surface get **three rows in #592's catalog** — graded GAP, cited to Digital #84
and #1470, funding score set, stop-loss column filled, "funded by #788 / #789" in the
owner column; dark mode gets a HAVE-elsewhere row pointing at #289. Then one paragraph
lands in ARCHITECTURE.md's *Recorded decisions* in the house form (rationale + revisit
trigger), because that is where this project's scope decisions actually live and where a
future maintainer will look. Close #787 as absorbed. Result: one competitive-justification
ledger for the whole project instead of a per-feature ledger genre, no new format, no
reconciliation step, and #592 gets stronger rather than fenced off from its own subject
matter.

## 3. "Would this ship only to poach?" is the wrong instrument here

KC-30-1's test is inherited without examination. CAP-30's thesis *is* poaching — PF-6 is
literally "invite Digital's rejected-PR authors by name," and the capstone's evidence
section is a strategic read of a competitor's decline. A kill criterion that forbids
competitor-motivated work sits inside a capstone that is competitor-motivated work. The
result is a test that cannot be applied honestly, so it gets applied nominally.

The question that actually protects the project is different and is the one ARCHITECTURE.md
already asks of every recorded decision: **who in JLS's own user model benefits, what is the
permanent maintenance tax on a single maintainer, and what would make us revisit?** Note
that i18n was declined not because nobody else has it but because "no requesting user" plus
"large, ongoing tax." Apply that test and item 2 discharges itself from documents already in
tree: rebindable accelerators are an accessibility capability (`docs/keyboard-a11y-verification.md`,
`docs/standards-adoption/03-accessibility-conformance.md`, #75's policy layer), serving users
who cannot use a mouse and lab machines with non-US keyboard layouts where a fixed accelerator
collides. That justification owes Digital #1470 nothing. Writing a task to discover it is
disproportionate.

## 4. The gate is unreadable to the audience the capstone exists to attract

"D10 path-and-cost," "KC-30-1," `band_mw` are load-bearing here and resolve to nothing in
this checkout (the only `D8`/`D10` hit outside `issue-reviews/` is an unrelated section
heading in `docs/capability-roadmap/lf-01-parameterization.md`). Set aside whether the
citation is repairable — the structural point is worse: CAP-30's outcome is "an outside
developer finds JLS, picks a labeled first issue … merges within a week," and its own
evidence names "the tracker's spec-prose reads as an internal monologue" as a repellent.
#787 is a task whose entire deliverable is spec-prose written in a private dialect, filed
under the capstone that identified spec-prose as the problem. Whatever else is true, this
issue is CAP-30 working against CAP-30. Any surviving version of this work must be
discharged in a form an outsider can read and check — which is another argument for the
`docs/` catalog and ARCHITECTURE.md, both of which are already web-readable and both of
which are cited by the README.

## 5. A better substantive framing for the dive — the cost is already mostly paid

Since the issue's product is a *path and a cost*, here is the path, derived from the tree,
which materially changes what #788 should be:

- Each `SubCircuit` instance already owns a **deep-copied `Circuit`**
  (`src/jls/elem/SubCircuit.java:332-358` — `copy()` builds `new Circuit(...)` and copies
  every element into it). #788's AC-3 ("N instances show N distinct live views") is a
  property of the existing model, not new work.
- The interactive simulator **already descends the hierarchy live**:
  `InteractiveSimulator.findTraces` recurses through `SubCircuit` into the child circuit
  and wires its probes and watched elements into the trace window
  (`src/jls/edit/InteractiveSimulator.java:967-993`). Students can already *observe*
  signals inside a running subcircuit; what they cannot do is *see the schematic light up*.
  That distinction is the honest statement of the gap and it is nowhere in #570 or #788.
- Opening a subcircuit instance in its own editor tab already exists —
  `SimpleEditor.doModify` (`src/jls/edit/SimpleEditor.java:5153-5195`) sets
  `subcirc.setImported(sub)`, builds an `Editor` over the instance's circuit, and registers
  it in `Editors`.
- The only reason this cannot happen mid-run is an **edit lock**, not missing state: one
  `volatile boolean enabled` (`SimpleEditor.java:126`, `enableEditor` at 705,
  `disableForSubcircuit` at 720), which `InteractiveSimulator` flips off for the duration
  of a run (`InteractiveSimulator.java:636-637`).

**Concrete alternative (substance):** the primitive to build is not "dive" but a
**read-only live circuit view** — split `enabled` into *editable* vs *interactive-viewable*,
open the existing subcircuit tab in view-only mode while `Simulator` is running, and repaint
it from the same EDT-marshalled hook that already refreshes traces (#49 H8 discipline). No
`jls.sim` change at all, which makes #788's AC-4 (kernel throughput) and AC-5 (batch output
byte-identical) true by construction rather than by measurement. The same primitive then
yields, for free, a detached instance inspector, side-by-side comparison of two instances of
one definition, and a coherent story for the trace window's hierarchy. It also exposes the
real risk #787 should have surfaced and did not: a bespoke modal dive path grown inside the
5,852-line `SimpleEditor` directly fights #316/FEAT-008 and **CAP-30's own AC-5** ("the
largest file in `jls.edit` is under 1,500 lines"). The ordering constraint that matters for
#788 is #316, not a justification memo.

## 6. What I am disregarding, and what I would keep

I am explicitly disregarding AC-1 (a new standalone justification artifact), AC-3 (a
dark-mode "row" restating what #570 AC-1 and its dedup comment already say twice) and AC-4
(a reconciliation #592 already recorded). AC-2 — cut and record the reasoning — is the only
durable idea here, and it belongs to the catalog's REFUSE-with-prose rule, which already
exists and already forbids "not scored."

Keep: the requirement that each item name a JLS-side beneficiary and a maintenance tax.
Move it into #592's rows and one ARCHITECTURE.md recorded decision with a revisit trigger.
Add to #788 the ordering dependency on #316 and the read-only-view framing above. Then this
task has nothing left to do, which is the correct outcome for a gate that was never able to
close.
