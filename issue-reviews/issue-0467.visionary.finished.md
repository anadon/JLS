# Issue #467: TASK-0110: concurrent edits converge to identical bytes, undo is per-user and never silently wrong, and no peer can name a type the allowlist excludes
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Three unrelated ends are bundled here: (1) multi-writer editing that cannot silently
corrupt a shared circuit; (2) an undo that never lies to the user; (3) a network-facing
type surface that cannot widen by accident. Ends (1) and (2) are #171's. End (3) is
#170's, and the issue's own re-home comments say so twice. The bundling is not clerical —
it determines what ships and when, and it is the first thing I would change.

## R1 — Ship the allowlist fix now, alone, ahead of everything

O2 is correct and I verified it at HEAD: `ElementRegistry.ALL` carries 35 types
(`src/jls/elem/ElementRegistry.java:38-77`, `TestGen` at the `Stop`/`Text` boundary),
`ElementVocabulary.ALLOWED` carries 34 (`src/jls/collab/op/ElementVocabulary.java:39-46`),
and the gap is exactly `TestGen`. The javadoc at `:26-30` actively instructs a future
contributor — or an LLM agent working the backlog — to make the naive delegation, and the
registry it waits for has landed. That hazard is live *today*.

The issue parks that fix behind `blocked_by: [415, 435]` plus an entire CRDT
implementation. Nothing about correcting a misleading javadoc and adding a
non-widening test depends on merge rules, a two-replica harness, or anti-entropy. This
should be a separate change of perhaps thirty lines, landed this week, closing #170's
allowlist clause and discharging the #78 record. Everything else in #467 can then take
as long as it takes without leaving a booby-trapped comment in the tree.

## R2 — Make the allowlist a predicate, not a deny list

I am disregarding the `registry minus deny list` acceptance criterion (P11, Stage 5).
It is the weaker of the two designs available and the issue's own H4 refutation branch
names why: "a second type that should be denied and is not noticed." A deny list is a
list of exceptions a human must remember to extend; Open Question 4 then proposes a
*checklist ritual* as the durable half of the design. Rituals are not enforcement.

`TestGen` is not excluded because someone named it. It is excluded because it is not
palette-creatable — a property, and one this codebase already knows:
`docs/collab-vocabulary.md:36-39` defines the allowlist as "every palette-creatable
element ... plus `WireEnd`", and `ElementVocabularyTest` already cross-checks against the
palette contract. The knowledge exists; it just lives in a test instead of in the model.

Concrete alternative: add a boolean to `ElementType` (`paletteCreatable`, or
`networkAdmissible` if the two ever diverge), so the network vocabulary is
`ElementRegistry.all().filter(...)`. No deny list, no reflection (P14/O4 satisfied — the
registry is already a plain `List<ElementType>` with method-reference factories, not
`Class.forName`), and the decision for a new element type is made at the one line where
that type is registered, which is the only place a contributor is guaranteed to be. This
converts H4's maintainability half from a hope into a compile-site fact and dissolves
Open Question 4 entirely. It is also strictly smaller than what the issue proposes.

## R3 — The unnamed gate: the op layer does not yet cover the mutations being merged

This is the finding that most affects whether the issue's headline claim can be true.
`docs/operation-layer.md:119-138` is the mutation-site inventory, and at HEAD:

- **Element dialog commits and `quickReset` are still inline** — "op implemented; gesture
  still inline — dialog commits mutate in place today" (rows at `:135-136`).
- **`EditOrderedRows` does not exist.** `CircuitOp`'s sealed permits list has eleven
  kinds (`src/jls/collab/op/CircuitOp.java:36-38`) and that is not one of them.
- Placement drop, wire-attach finish, paste and subcircuit import are likewise inline.

So `AttributeRegister` would be a per-attribute LWW register over attribute writes that
never become ops, and `OrderedRows` would be an RGA over an op kind that does not exist.
Worse, the op that *does* cover attributes is `SetElementConfig` — an element-state
**replace**. Per-attribute LWW over whole-element replaces is not per-attribute anything:
two peers editing two different fields of the same Register lose one peer's edit outright,
and no test in P1–P15 would see it, because every listed test exercises the CRDT classes
rather than what the editor actually emits.

The issue therefore contains an undeclared op-vocabulary extension (a new sealed permit, a
new `CircuitOpReader` case, a grammar change in `docs/operation-layer.md`, a new cap for
P12) inside a task that lists #167 as already-shipped and non-blocking.

The reframing: **the falsifiable claim that matters is not "the four types converge", it
is "every mutation that changes the canonical bytes is expressible as an op".** That is
testable today and cheaply: a ratchet asserting every `markChanged()` site in
`SimpleEditor` (16 at HEAD) is either an `OpSink.submit`/`submitAll` call or a listed,
justified exemption — the same shape as `HeadlessCoreRatchetTest` and
`NotificationRatchetTest`, which this project already uses well. Without it, P1 can go
green over a vocabulary that excludes the two gestures two students collaborating actually
perform most: dropping elements and editing dialogs.

## R4 — Two algebras, not four types; and the table should be data, not classes

H1 asks whether four types suffice for every table row. I would ask a sharper question:
how few algebras cover every row? JLS's model is a set of elements keyed by stable id
(#165), each carrying an attribute map, plus nets keyed by their endpoint sets. That is:

1. an **add-wins observed-remove map** `key -> (per-key LWW leaves)`, and
2. **RGA** for genuinely ordered collections.

`ElementSet` is that map with key = stable id. `WireSet` is the *same* map with key =
endpoint set — a key function, not a second type. `AttributeRegister` is the map's leaf
merge. Collapsing three classes into one parameterized structure is not tidiness: it means
a wire cannot have a merge bug an element does not, which is exactly the failure mode
"one class per table row" invites.

Consequently I am also disregarding the `everyTableRowHasATest()` criterion (P6) *as
framed*. Reflectively cross-checking test methods against a committed table is an
anti-vacuity guard made necessary only by the one-class-per-row structure. If a row is
**data** — record kind, key function, leaf merge — then rows are enumerated and driven by
a single parameterized test (`@ParameterizedTest` over the table), a new row cannot ship
untested because there is no code path for it to ship on, and #415's table becomes an
artifact the implementation *consumes* rather than a specification the implementation
*mirrors*. This also serves #356's offline merger from the same table, which the issue
notes both need.

## R5 — Undo should not be a second mutation path

O3's diagnosis is right: `invert()` is exact against the pre-apply circuit
(`src/jls/collab/op/package-info.java:13`), which is wrong under concurrency. But the
prescription — "recompute the inverse against current state, report when you cannot" —
introduces a second way to compute a mutation, alongside the merge algebra, and a second
way is a second place convergence can break. Note that P1's schedules contain no undos at
all, so SEC is never asserted over undone states under the issue's own evidence plan.

Reframing: **an undo is not a mechanism, it is an ordinary op submission.** Undoing `o`
means submitting `inv(o)` through the same `OpSink` and letting it merge by the same
algebra as every other op. Under R4's structure the outcomes fall out rather than needing
a policy: the element is gone → the inverse's key is absent → add-wins/observed-remove
already says the result (nothing happens) and *that* is the case worth reporting; the
attribute was rewritten remotely → the inverse is a later LWW write and wins, which is
exactly what would have happened had the user made the same edit by hand a second later.
This keeps P9's per-peer stack (correct, and the "one field" warning in §11 is well made)
and P10's report (correct, and the "silently skipped is worse than an error" argument is
the best paragraph in the issue), while deleting the recompute engine and the class of
bugs where recomputation and merge disagree about the same state.

## R6 — The arc question, stated plainly

`jls.collab` is 45 of 300 source files and 7,868 of 82,120 source lines, with 5,057 lines
of test — and **ARCHITECTURE.md, the 368-line contributor's map that says it "describes
HEAD", does not mention it once.** Its Module layout section stops at `jls`, `jls.elem`,
`jls.edit`, `jls.sim`. Meanwhile that same document records i18n as a non-goal explicitly
because there is "no requesting user", and records the plugin mechanism's removal, and
records the discrete-event interpreter as the sole simulation strategy — each with a
rationale and a revisit trigger. Collaboration has received the opposite treatment: four
packages and a dozen issues accumulated with no entry in the map and no recorded decision.

I am not arguing against the feature. I am arguing that #467 is the exact commit where the
program stops being headless substrate and becomes a live multi-writer surface on a
student's machine, and that is where the project's own discipline says a recorded decision
belongs. Concretely, this task should also add the `jls.collab` module to ARCHITECTURE.md's
Module layout and a "Collaborative editing: pure-P2P, opt-in, no server" recorded decision
with its revisit trigger — otherwise the largest subsystem in the tree remains invisible
to the very "contributors, maintainers, and LLM agents" the issue names as an audience.

## What I endorse without change

The compaction frontier argument (Stage 3, H5, P8) is the strongest technical content
here: roster-minimum versus connected-minimum is a one-symbol difference with irreversible
consequences, `ReachabilityTracker` already keeps unreachable-is-not-removed
(`src/jls/collab/session/package-info.java`), and the test configuration named — unreachable
across the compaction point, then reachable again — is the only one that distinguishes
them. The `PeerId` tie-break with opposite-direction clock skew (H2, P3) is right and the
"convergence must not be a function of NTP" framing is exactly the correct altitude.
Byte-equality rather than a structural comparator (Stage 1) is right for the stated reason.
Caps-before-allocation (Stage 6, P15) matches what `SecureLink` already does. Keep all of it.

## Suggested split

1. **Now, independent:** allowlist as an `ElementType` predicate + javadoc correction +
   non-widening test; close #170's allowlist clause; record on #78.
2. **Next, before any merge rule:** the `markChanged()`→`OpSink` coverage ratchet, the
   per-attribute op (or an explicit decision that `SetElementConfig` replace semantics are
   the merge granularity, with its cost stated), and `EditOrderedRows` as a real permit.
3. **Then:** one keyed-map algebra + RGA, table-as-data, anti-entropy and compaction,
   undo-as-op-submission — reported to #171, which is where convergence and undo live.
