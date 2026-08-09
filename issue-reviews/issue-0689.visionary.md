# Issue #689: TASK-C532-2: "why did this change?" renders the ancestor chain of a selected transition, each hop naming its element and its consumed delay
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is right and it is one of the best ends in the roadmap: JLS owns the
schematic the student drew, so a backward causal walk lands on *their picture*,
which Verdi, Questa and Indago structurally cannot do (`docs/capability-roadmap/
AMENDMENT.md:681-690`). #689 is the rendering half of that. I endorse the
outcome without reservation.

But #689 frames the deliverable as **a Swing panel**, and two of its four
acceptance criteria encode that framing into the data model. Both pull against
the project's own recorded direction. I am explicitly disregarding AC-3 and AC-4
as written; reasons below.

## Pull #1 — AC-3 picks the granularity the project already rejected, on paper

AC-3: "chain granularity follows whatever the scheduler already keys on." The
scheduler keys on `SimEvent`. `docs/capability-roadmap/lf-03-causal-debug.md:143`
states the opposite decision in bold: *"journal at net-change granularity, not
per-event. Per-event journalling is the obvious design and it is wrong."* Its
evidence is a census, not taste: 2,331,793 events fired on the 6004-cycle CPU,
of which **1,919,891 (82.3%) are `PinChanged`** — a zero-field record
(`src/jls/sim/SimEvent.java:30-31`) meaning only "something upstream moved" —
against 378,129 `NewValue`.

This is not an abstract disagreement; it changes what #689 renders. `Gate.react`
(`src/jls/elem/Gate.java:696-717`) shows the real shape: a `PinChanged` arm that
consumes **zero** delay and posts `NewValue` at `now+propDelay`, then a
`NewValue` arm that consumes zero delay and calls `Output.propagate`. At
scheduler granularity, an honest chain therefore alternates: *g7 (delay 0) → g7
(delay 10) → distribution → g12 (delay 0) → g12 (delay 8)…* — the same element
named twice per real hop, half the hops carrying delay 0. AC-1 promises "each hop
naming the element and its consumed delay"; at AC-3's granularity that promise is
mostly satisfied by noise hops. Net-change granularity gives exactly one hop per
cause, which is what the mock tree at `lf-03-causal-debug.md:204-211` renders.

It also multiplies the failure mode #688 AC-5 and KC-23-2 are watching for. A
ring buffer of N *events* spends ~82% of its capacity on records with no value
and no delay, so it retains roughly **five to six times less causal history** than
a net-change journal of the same bytes. AC-3 is thus the single largest
contributor to "the window truncated the chain for the very glitch the analyzer
caught" — the condition that stops the whole feature.

CAP-23 Open Question 1 offered scheduler granularity as a *recommended default*
for a question it left open and marked "Blocks PF-3's filing." lf-03 answered
that question with measurements. #689 quotes the placeholder and not the answer.

## Pull #2 — AC-4 forecloses the one axis where JLS could be unambiguously ahead

AC-4: the inspector "is not constructed at all in headless/batch runs." As a
statement about the *Swing component*, fine and correct. As written it reads as a
scope boundary on the capability, and CAP-23 Open Question 4 pushes the batch
cause-chain artifact off to "the CAP-06 lineage." Meanwhile the roadmap says:
*"The headless artifact, and it is the leapfrog axis: `jls -b --why
'alu_out[3]@41200' circuit.jls` printing a deterministic causality tree to
stdout — diffable, gradeable, CI-testable"* (`AMENDMENT.md:344-347`), and *"once
the journal exists it is a printer"* (`lf-03-causal-debug.md:566`). The panel is
parity with three commercial tools; the printer is the thing none of them
documents.

There is also a latent contradiction inside the same task family. Sibling #691
AC-2 requires `CauseChainCompletenessTest` to check the reported set against the
scheduler's event graph "as an invariant over the graph, not against a recorded
screenshot." That test cannot live behind a Swing constructor. So the chain
*walker* must already be headless and separately addressable — #691 needs the
seam that #689's AC-4 declines to name.

## Pull #3 — the shared index and seam that must be designed before code

`AMENDMENT.md:805-809` records a cross-programme decision: *"P9's journal site
index IS P8's levelization slot table IS P6's cross-probe map IS P4's
critical-path overlay key. One table, four payoffs. Must be designed BEFORE
either P8 or P9 writes code, or it is two permanently disagreeing indexes."*
Neither #688 nor #689 mentions a site index; #688 keys on event identity only.
Separately, `docs/extension-points.md:22` requires a pending seam to get its
catalog row before code, and lf-03 recommends `sim.journal-consumer` as its own
row (not folded into `sim.coverage-collector`). No such row exists. CAP-23 risk 1
says "never two cause-chain models" — filing #688/#689 ahead of P9's features is
exactly how the second one gets minted.

## The reframing

Keep the outcome; move the seam one layer down. Three deliverables instead of
one panel:

1. **`jls.sim.CauseChain` — a headless value type and a headless walker.** Given
   a (site, time) it walks TASK-C532-1's retention and returns an immutable tree
   of hops (`ElementId` from `src/jls/elem/Element.java:24`, consumed delay,
   accumulated total, termination reason: `STIMULUS` / `INIT` / sequential
   boundary / window edge). AWT-free by construction and already ratcheted there
   by `HeadlessCoreRatchetTest` (`ARCHITECTURE.md`, `jls.sim` module notes).
2. **A deterministic text renderer over that tree**, in the shape of
   `lf-03-causal-debug.md:204-211`. This is the gradeable artifact and it is
   ~100 lines once (1) exists. *Caveat I will not paper over:* exposing it as a
   `--why` CLI flag is "a change to a promise" and must ride P5's report channel
   (`lf-03-causal-debug.md:629-632`, `AMENDMENT.md:795-802`). So ship the
   renderer as a tested, stable-output component now; mint the flag when the
   report channel lands. The capability is not blocked by the flag.
3. **The inspector as a thin view over (1)**, satisfying #689's AC-1 and AC-2
   unchanged — and now built and tested against a model that already has a
   golden text form, which is far cheaper than asserting tree structure through
   a display-tagged Swing test.

Under this cut, AC-4 becomes *stronger and truer*: not "no inspector headlessly"
but "the chain model is headless; the AWT surface is a view with no logic in it,"
enforced by the existing ratchet rather than by a constructor guard.

## A different GUI form worth considering

The issue assumes a side panel listing hops with a numeric accumulated total.
The chronogram (PF-1, #529's lane surface) is already a *time axis* — so draw the
chain **on it**: arrows from each ancestor edge to its consequent edge, where the
arrow's horizontal length *is* the consumed delay and the span from root to
target *is* the accumulated total. Delay stops being a column of numbers and
becomes geometry. For #691's reconvergent case this is decisive: two ladders
leaving a common divergence edge and arriving at visibly different x-positions is
the hazard lesson, drawn, with no second panel and no new hit-testing model. The
text tree stays the primary artifact (lf-03 is right that it must exist first,
and right that the schematic cone may prove illegible — `lf-03:692-702`); the
ladder is a cheaper, better GUI than a list, and it reuses a surface #529 is
already building.

## What I would change in the acceptance criteria

- **AC-1, AC-2:** keep verbatim. They are good, and AC-2's "a truncated chain
  says so rather than presenting a partial chain as complete" is the best line in
  the issue.
- **AC-3:** replace. Journal at net-change granularity per `lf-03:143-164`;
  record the site index decision on #532 as the REPLAN that closes CAP-23 Open
  Question 1, and check it against P8's slot table before writing code.
- **AC-4:** replace with the layering above — chain model in `jls.sim`, view in
  `jls.edit`, plus the `sim.journal-consumer` catalog row.
- **Add:** a golden test on the text renderer for the seeded hazard fixture. It
  makes #691's `CauseChainCompletenessTest` a headless invariant test rather than
  a scripted GUI one, and it is the same fixture either way.

## Risk in my own proposal

Net-change granularity requires a site index that #688 does not currently build,
so the reframing pushes work into the ordering-before task rather than removing
it. That is the correct place for it — a wrong index is permanent and a missing
panel is a week — but it is real, and it means #688 should be amended before
#689 starts, not after.
