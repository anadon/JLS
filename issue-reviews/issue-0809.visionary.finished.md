# Issue #809: TASK-C595-1: every connect, width and name refusal names both disagreeing parties, their locations, and the edit that reconciles them
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The goal — "when the editor says no, it says what would make it yes" — is
correct, aligned with CAP-37, and worth funding. My objection is to the
route. As written, #809 is a *prose* task: author better sentences at the
sites that currently refuse, then freeze those sentences with a
content-asserting corpus. Issie's width-inference messages are good for a
different reason than good sentence-writing: Issie *infers* widths by
unification across the net, so the message is a **rendering of a typed
inference failure** — generated, never authored. Copy the mechanism and
the prose follows for free and forever; copy the prose and you have
twenty hand-written strings and a test suite that pins them in place.

## What the code actually says

- Every connect/width refusal in JLS today is a bare string assigned to
  `overlapMessage`, a private field of `SimpleEditor`
  (`src/jls/edit/SimpleEditor.java:1264`), set at ~20 sites
  (`canConnect` at 3992, 4229, ~4340) and painted into a status label at
  1999/3130/3558/3847. "Bits don't match" appears three times as a
  literal.
- The model has no notion of a width conflict at all.
  `WireNet.setBits` is `this.bits = Math.max(this.bits, bits)`
  (`src/jls/elem/WireNet.java:230`), and the recompute at 267–280 is the
  same max-fold over attached puts. There is no provenance: nothing
  records *which* put pinned the net to 8, so no message can name the
  first disagreeing party even if you wanted it to.
- The gesture is therefore the **only** enforcement point.
  `AddWire.apply` calls `net.setBits(vendPut.getBits())`
  (`src/jls/collab/op/AddWire.java:189`) with no agreement check, so an
  op-replayed wire — a collab peer's (#163), a future op-based paste, a
  netlist import — can build a width-inconsistent net and surface no
  refusal whatsoever. AC-1's scope ("every refusal *the editor*
  surfaces") ratifies that boundary at the exact moment #167 is making
  `OpSink` the universal mutation entry point.
- `OpRejected` is a plain `Exception` over a `String`
  (`src/jls/collab/op/OpRejected.java`), with ~40 concatenated literals
  across `jls.collab.op`. That is precisely the "scattered string
  literals" AC-5 objects to — already existing, one package over. #809
  as written would stand up a *second*, parallel refusal vocabulary in
  the editor and leave that one untouched. That is duplication of the
  project's own arc, not reinforcement of it.
- The project already solved this problem once, well: `LoadError`
  (`src/jls/LoadError.java`) is a record of category + location +
  detail + actionable hint, and its own doc comment states the design
  lesson — tests assert the **category**, "keeping the detail wording
  free to improve" (lines 34–37). AC-2 of #809 does the exact opposite.

## The reframing I would fund instead

**1. A structured refusal is the deliverable; the sentence is a view.**
Add one model-side record (`jls.core`, AWT-free) —

```
Refusal(Kind kind, Party a, Party b, Fix suggestion)
Party(String label, GridPoint where)   // "input 'A' of Adder 'sum'", (12,7)
```

— whose constructor *cannot be called* without both parties and their
locations. AC-1 then holds by construction instead of by audit, AC-4
("nothing in `SimpleEditor`") falls out because the editor only calls
`render()`, and AC-5 ("wording is data") falls out because `render()` is
the single surface. Make `OpRejected` carry a `Refusal`; the ~40 op
literals migrate into the same vocabulary and the collab/network path
gets the same quality of message as the mouse path.

**2. Width becomes unification with provenance, not a max-fold.**
Give `WireNet` a width that remembers who pinned it. A conflict is then
a data structure — two provenanced pins with different values — and the
message writes itself, identically, for the gesture, the op layer, the
loader, `-b` batch, and HDL import. This is the actual Issie transplant.
It also closes a live correctness hole: `Math.max` on disagreeing widths
is a silent wrong answer on every path that is not the mouse.

**3. The suggestion should be an op, not a sentence.** `Fix` should
carry (or be able to build) a real `CircuitOp` — insert an `Extend`,
retune the adder's `bits`, rename to a free name. Two things follow.
The test oracle becomes enormously stronger than string equality:
*apply the suggested op and assert the rejected op now validates.* A
message cannot claim to be actionable and be wrong. And the end state of
"says what would make it yes" is a program that **makes it yes** — the
refusal renders with a one-click fix. For the same 1–1.5 band that is a
materially bigger user win than better prose, and it lands entirely
inside seams the project is already building (#167 ops, #78 registry,
#165 stable ids), feeding #91/#441 rather than needing new machinery.

**4. For names, reuse what exists.** `jls.hdl.HdlNames` already
implements the reconciling edit for the commonest name refusal — "a name
already taken gets `_2`, `_3`, ... appended"
(`src/jls/hdl/HdlNames.java:20`). The editor should *offer that name*,
not describe the collision. And `Util.isValidName`
(`src/jls/Util.java:219`) is already a single choke point with ~16 call
sites, so the name half of this task is one function plus one `Refusal`
kind — most of the remaining name work is #810's, where refusing less is
strictly better than refusing more eloquently.

## Where I am explicitly disregarding the stated criteria

- **AC-2 (corpus asserted against expected message content).** I would
  not land this. It contradicts the recorded `LoadError` lesson, freezes
  prose written before a single student has read it, and converts every
  future wording improvement into a test edit — the standard mechanism
  by which message quality *stops* improving. Replace with: assert the
  refusal's structure (kind, both parties' identities and locations,
  suggestion kind) and assert the suggestion **applies and resolves the
  rejection**. Keep at most one golden render per kind as a smoke test.
- **AC-1's scoping to "the editor surfaces".** Widen to "every refusal
  any mutation path produces". The editor-only scope is what leaves the
  op/collab path silently wrong.
- **AC-5's translation motivation.** ARCHITECTURE.md records i18n as a
  non-goal with an explicit "PRs adding partial i18n scaffolding will be
  declined" and a named revisit trigger (#78 centralization). Justify
  the single surface on its own merits — one reviewable, testable place
  for wording — and do not hang it on a translation pass, or the task
  argues with a binding recorded decision it does not need to fight.

## Effect on ordering

Under this reframing the substance lands in `jls.core` and
`jls.collab.op` — outside `SimpleEditor` by construction, satisfying
KC-37-1 structurally rather than by discipline. That means the model half
is **not** blocked on #316's decomposition; only the render call sites
are, and those are one-line `info.setText(refusal.render())` edits that
#316 carries along for free. The #592 scored-row gate still applies to
which refusals get funded, but the refusal *type* and the width
provenance are infrastructure #810, #163, and the netlist importer all
want anyway, and should not wait behind a catalog scoring pass.

## Bottom line

Endorse the outcome; rebuild the route. Ship a `Refusal` value type,
width provenance in `WireNet`, `OpRejected` carrying `Refusal`, and
suggestions expressed as applicable ops — and let the corpus test the
fix, not the sentence.
