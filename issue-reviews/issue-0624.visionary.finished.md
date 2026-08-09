# Issue #624: TASK-C559-3: CircuitVerse subcircuits arrive as JLS subcircuits with hierarchy intact, and an unmappable one refuses instead of flattening
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the format name away and the claim is: **JLS should be able to accept a
hierarchy from outside itself.** The instructor's design is a tree of named
modules; a tool that can only accept one flat sheet has not accepted the design.
That claim is right, and it is bigger than `.cv`.

The issue then makes a scope decision I think is wrong: it puts the hierarchy
assembly *inside the CircuitVerse importer*. Everything below follows from that
one seam choice.

## The hole this task is actually standing on

`docs/operation-layer.md`'s mutation-site inventory ends with one row:

| Subcircuit import | `ImportSubcircuit` | deferred |

That op does not exist, and three shipped op kinds already refuse work by
pointing at it:

- `src/jls/collab/op/ElementBlocks.java:107` — `"subcircuits travel through the
  subcircuit-import op kind, not through element blocks"`
- `src/jls/collab/op/RemoveElements.java:114`
- `src/jls/collab/op/SetElementConfig.java:148`

So JLS at HEAD has **no validated, invertible, serializable way to add a
subcircuit to a circuit**. The only paths are `SimpleEditor.finishImport`
(`:679`, GUI-bound, mutates live, no inverse) and the loader's nested-`CIRCUIT`
branch (`Circuit.java:1006-1030`).

That same hole is refused, in prose, three other places in the tree and the
roster:

- `NetlistImporter` refuses hierarchy twice — `:157` (`"multi-module (hierarchy)
  import is not built in this increment - flatten the design"`) and `:229`
  (`"hierarchy (subcircuit) import is not built in this increment"`).
- #558 (`.dig`) refuses generics/parameterized circuits "by name until #357".
- #323 (`.circ`) has subcircuits in its source format and no hierarchy story
  beyond its construct map.

#624 would therefore be the **fourth** place hierarchy assembly gets written,
and the first one to actually write it — inside an importer for one JSON format,
where the netlist importer, the `.dig` importer, the `.circ` importer, paste of
a subcircuit, delete of a subcircuit, and collab replication of a subcircuit
cannot reach it.

## Alternative A — build `ImportSubcircuit` as a core op; make #624 an adapter

Concretely: file (or re-home this task as) *"the subcircuit op kind"* —
`ImportSubcircuit` / `RemoveSubcircuit` in `jls.collab.op`, built the way
`AddWire`/`RemoveWire` were: validate-atomically, byte-exact inverse, blocks in
save-format idiom via `ElementBlocks`/`NetBlocks`, addressed by stable id.
Its validation is exactly what #624's AC-2 and AC-3 want, stated once for the
whole project rather than once per source format:

- port set, order and direction of the instance agree with the definition's
  `InputPin`/`OutputPin` set, or reject;
- the nested `CIRCUIT` name is legal and unused, or reject;
- the nesting is acyclic and depth-bounded, or reject.

Then #624 collapses to: walk the parsed `.cv` scope graph bottom-up, emit one
`ImportSubcircuit` per instance, batch through `OpSink.submitAll`. What that
buys, none of which #624 currently claims:

- **TASK-C559-5's AC-2 falls out for free.** `OpSink.submitAll` is already "one
  gesture, one undo snapshot, however many ops express it"; a rejected op leaves
  the circuit byte-identical (`CircuitOpTest.rejectionsLeaveTheCircuitUnchanged`).
  Atomic, undoable, no-partial-circuit import stops being a per-importer
  obligation and becomes a property of the op layer.
- **#448 / #61's hierarchy refusal becomes fixable**, and #558's and #323's
  hierarchy work becomes an adapter too.
- **#323's Open Question 4** ("how is `NetlistImporter.Builder` promoted, and by
  whom? A second importer must not fork it") gets an answer that is not "the
  importer that lands first": the promoted builder *is* the op vocabulary.
- **#323's rejected alternative 2 dissolves.** #323 rejected "emit save text and
  reparse" in favour of "construction verbs" — but `ElementBlocks`/`NetBlocks`
  are already both: ops carry save-format blocks and load them through
  `Circuit.loadElement` against a scratch circuit, "so an added element is
  indistinguishable from a loaded one". The dichotomy that shaped #323's
  decomposition is stale; the op layer is the reconciliation and no importer
  needs to know that.

The cost is roughly #624's own band, spent once instead of per format. If the
maintainer wants `.cv` shipped before the op work, the honest sequencing is
`ordering_after: [167]` plus a stated intent to migrate, not silent duplication.

## AC-4 as written is unachievable, and its escape hatch is worse than the bug

**I am disregarding AC-4 and recommending it be restated.** "Instances of the
same source subcircuit reference one imported definition, not one copy per
instance" is not a property of an import — it is a property of JLS's
representation, and JLS does not have it. `SubCircuit` owns a `Circuit`
(`SubCircuit.java:26`), `save` writes the body once per instance
(`:287`), the format spec confirms the body is inlined per instance
(`docs/file-format.md:321`), and #357's own evidence measures "sharing factor
exactly 1.00x".

The boundary note says to "refuse by name where #357 has not landed". #357 is a
25-36 mw program, blocked on #340/#318/#319, with Open Question 1 unratified —
it is not landing inside CAP-29's 13-20 mw. So AC-4 in practice means: **refuse
every `.cv` project that instantiates a scope twice**, which is most real ones
(an ALU with four identical slices, a register file, a handout with two
half-adders in a full-adder). The importer whose purpose is "the single
highest-leverage migration lever" (#510 §3) would refuse the designs that made
the instructor want hierarchy in the first place.

Restate it as a *report* obligation, which is the honest thing and is exactly
what #559 AC-3 already does for queue-priority delay:

> AC-4′: the importer's model records instance→definition identity from the
> `.cv` scope id. Where the current JLS representation cannot express sharing,
> each duplicated definition is emitted once per instance **and named in the
> report** as a non-preserving materialization — "scope `ALU` instantiated 4×;
> JLS 5.x stores 4 independent copies, editing one will not change the others" —
> with a test asserting the report row appears. When #357 lands, only the
> emitter changes; the model already carries the identity.

That keeps the design's structure recoverable, keeps the promise honest, and
keeps the importer from being hostage to a program an order of magnitude larger.

## The unmappable case the issue never names

AC-3 asks for a live refusal fixture but does not say what the refusals *are*.
Two are concrete and neither is in the issue:

1. **Names.** Nested `CIRCUIT` names are meaningful and must match
   `letter (letter|digit|_)*` (`Util.isValidName:219`, `docs/file-format.md:153`).
   CircuitVerse scope names are free-form UI strings — `"Full Adder"`,
   `"4-bit ALU"`, `"ALU (v2)"`. Under AC-3 read literally, a typical project
   refuses wholesale. The right rule is a *stated, reported legalization*:
   deterministic mangling, collision-checked against `Circuit.addName:1621`, one
   report row per renamed scope. Silent renaming would be a silent difference;
   refusing would be uselessly strict. Neither AC covers it.
2. **DAG, not tree.** `.cv` scopes form a DAG (two scopes may instantiate a
   third); the `.jls` file is a tree of inlined bodies. Sharing collapses to
   duplication (see AC-4′), and a *cyclic* scope reference — which a
   hand-edited or hostile `.cv` can contain even though the CircuitVerse UI
   prevents it — must be detected as a cycle, not discovered as an
   `OutOfMemoryError` during inlining. That is a hardening obligation belonging
   with #559 AC-4's depth/size bounds and it is currently in nobody's task.

AC-1's oracle is also weaker than it needs to be: "comparing the imported
hierarchy against the source's, level by level" invites a bespoke comparator.
JLS already has a stronger, cheaper one — canonical save text (#166 makes it
byte-deterministic), so the fixture is a committed golden `.jls` and the
assertion is `assertEquals` on bytes, with TASK-C559-2's net-partition equality
applied per scope rather than only to the top sheet.

## What I would keep untouched

The refusal-over-flattening principle is exactly right and is the best sentence
in the issue: a flattened import "simulates plausibly and can never be edited
back into the design the instructor had". That is the same instinct as the
loud-loader discipline (#314), the `LoadError` taxonomy, and the
`NotificationRatchetTest` — it strengthens the project's arc rather than pulling
against it. The consumes-#357-rather-than-rebuilds boundary is also correct in
spirit; only its fallback (refuse) is miscalibrated.

## Verdict

**endorse-with-reframing.** The goal — hierarchy survives migration, nothing
flattens silently — is right and worth the band. But the work is core work
wearing a format's clothes: it should land as the `ImportSubcircuit` op the
operation layer already has a hole for, with #624 reduced to a `.cv` adapter
over it, so `.dig`, `.circ` and the netlist importer inherit hierarchy instead
of each re-deriving it. AC-4 must be restated from "reference one definition"
(impossible before #357, and a de-facto refusal of most real projects) to
"record the identity, materialize what the format can hold, and name the
duplication in the report". AC-3 needs its two real refusal cases — illegal
scope names and cyclic scope references — written down, with name legalization
as a reported transformation rather than a rejection.
