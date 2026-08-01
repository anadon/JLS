# FEAT-017 - Shared and parameterized subcircuit definitions

**Status:** proposed | **Cost:** 25-36 mw | **Owner program:** P7 |
**Spine rank:** -

## Capability delivered

A subcircuit definition exists once and its instances reference it, with
parameters bound per instance, replacing the present arrangement in which every
instance carries its own deep copy of the definition. Editing the definition
changes every instance; instances differ only in what their parameters say they
differ in; and a design built from repeated modules stops being a design in
which the repeated modules have silently diverged. An elaboration pass resolves
the parameter bindings once and reports every binding it could not resolve.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | required | Concurrent edits to a definition instantiated N times must land once, not N times divergently |
| CAP-02 | required | A machine drawn as per-instance deep copies diverges silently |
| CAP-03 | required | A mixed-radix composition of per-instance deep copies diverges silently |
| CAP-04 | beneficial | A teaching CPU is drawn as repeated modules; N deep copies pack to N divergent BOMs |
| CAP-05 | beneficial | One definition, N instances - otherwise N copies pack to N divergent BOMs |
| CAP-06 | required | One handout definition instantiated per student, parameterized per section |
| CAP-08 | required | Otherwise the same module imports as N deep copies that then diverge |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-016 | A definition cannot be referenced until it has an identity to reference. The digest and version strings are what an instance's reference resolves against |
| FEAT-014 | An instance inside a shared definition is addressed by instance path; without stable addressing that survives sharing, two instances of one definition are indistinguishable to every consumer that names an element |
| FEAT-013 | The definition/instance split is a file-format change of the kind the section framing and must-understand policy exist to govern; without it an older reader silently loses either the definitions or the bindings |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0041 | Definition/instance split with parameters | The representation change itself: one definition, N referencing instances, with bound parameters |
| TASK-0042 | The elaboration pass and its diagnostics | A shared definition is meaningless until parameters resolve; an unresolved binding must be reported, not defaulted |

## Acceptance criteria

1. A file containing N instances of one definition serializes the definition
   once. The saved size of an N-instance circuit grows sublinearly in N for the
   definition body.
2. Editing the definition is observable at every instance without touching the
   instances.
3. Parameters are declared on the definition, bound per instance, and typed;
   a binding of the wrong type is refused by name at elaboration time.
4. Every unresolved binding is reported with the instance path, the parameter
   name and the reason. Elaboration does not substitute a default silently.
5. A file written before the split loads, and its per-instance copies are
   either shared automatically where they are structurally identical or left
   unshared with the reason recorded. Which of the two is a stated decision, not
   an accident of implementation.
6. Every existing golden file remains loadable and every existing simulation
   golden remains byte-identical after the migration.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | The definition/instance split and parameterization | **no issue** |
| 165 | Stable element identity: permanent ids minted at creation, surviving save/load/undo/copy | informs, **closed** - the identity discipline this split extends from elements to definitions |

## Design notes

This is the second most expensive feature in the plan after the four-state
value core, and its cost is not in the idea but in the blast radius: every
consumer that today walks an instance's own `Circuit` must be taught to walk a
definition through a binding. Its 25-36 mw band should be read as a program
with an elaboration pass in the middle of it, not as a refactor.

The migration in criterion 5 is the decision that determines whether this
feature is safe. Automatically sharing structurally identical copies is what
users expect and is also what silently merges two things a user deliberately
made different. The recommendation the task carries is: do not auto-share on
load; offer it as an explicit, reported operation.

Criterion 6 is the gate. This feature changes representation, not semantics; a
single byte of golden drift means it changed semantics and did not notice.

## Risks

- **Divergence already exists in the field.** Files saved today contain
  instances that were once one definition and no longer are. Any auto-sharing
  migration silently picks a winner among them.
- **Parameters are a language.** Their type system, their scoping and whether
  they can be expressions are three decisions that can each double the cost.
  Keep them values, not expressions, in the first shipment.
- **The elaboration pass is a new compile step in a tool with no compile step.**
  Its diagnostics are the whole user experience of this feature, and diagnostics
  are the part most easily deferred and never done.

## Evidence

- Per-instance deep copies at HEAD: `src/jls/elem/SubCircuit.java:102-107`
  (each instance holds its own `Circuit`), `:282-289` (`save` writes the nested
  circuit for each instance), `:301` (the format version needed is delegated to
  the nested circuit).
- Owner: P7 in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 25-36 mw; TASK-0041 and TASK-0042 total 4 wk.
  The gap is the largest in the plan and it is real: the two tasks are the
  representation change and the elaboration pass, while the band prices
  migrating every consumer of `SubCircuit.getSubCircuit()` and re-greening the
  golden corpus behind it. The closed task id space does not name that residual.
  Do not read 4 wk as the feature.
