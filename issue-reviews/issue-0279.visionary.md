# Issue #279: Simultaneous editing: per-kind confluent CRDT merge rules + P1 in-process convergence suite (collab Stage 2 slice)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the vocabulary away and the ask is one sentence: **the state of a replica must be a
function of the set of ops it has received, not of the order they arrived in.** Everything
else — add-wins, delete-wins, LWW registers, OR-sets, tombstones — is one particular
*implementation strategy* for that property, imported wholesale from the generic CRDT
literature via `docs/collaborative-editing-research.md` §3. The goal is right and load-bearing:
#171 → #163 has no value proposition without it, and the P2 property test
(N∈{2,3,5} replicas, random schedules, byte-equal `Circuit.stateHash()`) is exactly the
correct falsifier. Keep P2 verbatim. The reframing below is about §8 (Method), not §5.

## Reframing 1: the total order *is* the merge table — replay, don't merge

The §3 table was written for the generic case: a replicated set whose keys can be re-added,
attributes with no shared clock, no invertible ops, no canonical bytes. JLS is not that case.
It has three assets a CRDT library cannot assume, all already landed:

1. **`ElementId` is globally unique by construction** — replica id + counter, minted once
   (`src/jls/elem/ElementId.java:36-60`, `mintFresh()`), and `Wire extends Element`
   (`src/jls/elem/Wire.java:16`) so wires carry them too.
2. **Ops are invertible** — `CircuitOp.invert(before)` is a contract, not an aspiration
   (`src/jls/collab/op/CircuitOp.java`), and every kind already validates-then-mutates.
3. **State has canonical bytes and a cheap snapshot** — `Circuit.stateHash()`
   (`src/jls/Circuit.java:1548`) plus `CircuitSnapshot`, which already backs interactive undo.

Given those, replace the four merge mechanisms with one invariant:

> A replica's state is `apply(base, sort(deliveredSet))`, where `sort` is the deterministic
> total order (Lamport, peer-id, op-id tiebreak) and `apply` skips ops that fail validation.

Confluence then follows in one line — same base, same set, same order, deterministic
apply — instead of an N×N per-kind commutativity argument that §10 itself is nervous about
("recurrent failures across pairs mean the confluence design is unsound"). And every row of
the §3 table falls out as *observable behavior* rather than *code*:

- **add-wins / tombstones: vacuous.** Two replicas can never mint the same `ElementId`, so
  concurrent re-add of a removed key — the scenario tombstones exist for — cannot occur.
  State is `f(base, set)`, so nothing needs to remember that a delete happened.
- **delete-wins: free.** Concurrent `RemoveElements(E)` and `MoveElements(E)`: if the delete
  sorts first, the move fails `Ops.resolve` and is skipped; if the move sorts first, the
  delete still applies. Delete wins either way, with no rule written.
- **per-attribute LWW: the sort key already is the tiebreak.** Applying config ops in
  (Lamport, peer) order leaves exactly the LWW winner, without a register per attribute.
- **OR-set wires: same as elements**, since wires are id-bearing elements.

This is Bayou's roll-back/re-execute discipline, not a novel invention, and it is the same
shape #171 already plans for compaction (snapshot + replay). Mechanically: a `Replica` holding
(base snapshot, ordered delivered log, `CausalBuffer`); on delivery, insert by sort key; if the
insertion point is the tail, apply; otherwise restore the snapshot and replay the ordered set.
The undo/redo-suffix variant is an optimization — do it second, and only if measured.

**Why this is not merely simpler — it closes a real hole in H1.** Validation rejection is
*state-dependent*, and state-dependent rejection does not commute. P4 says an invalid remote
op is rejected with state untouched; but under arrival-order delivery with per-kind rules,
whether an op is valid depends on what already applied. Two peers concurrently wiring into the
same free put, or A deleting E while B wires E→F: replica 1 can accept X and reject Y while
replica 2 does the reverse, and *no per-kind merge rule repairs that* — the table governs how
two writes to the same key resolve, not which writes validate. H1 as stated is therefore
incomplete for the landed vocabulary, and the incompleteness lives precisely in `AddWire` /
`NetBlocks`, the most intricate ops in `jls.collab.op`. Total-order replay makes the
accept/reject decision itself a function of the ordered set.

Two honest costs, both bounded: rollback work on late arrivals (classroom-scale circuits;
`CircuitSnapshot` restore is already fast enough for interactive undo, and the causal-stability
horizon caps how far back a rollback can reach — which usefully pulls #171's compaction slice
earlier in the spine), and a transient visual jump when a remote op sorts before a local one.
That second cost is arguably a feature: it maps onto #163's tentative/committed sync indicator
honestly, where silent LWW does not.

It also makes #171's Stage 3 claim literally true. §7.8 currently says merge metadata's
"lifetime = session; loss mid-session is repaired by anti-entropy" — convergence metadata that
can be lost and re-derived is metadata you would rather not have. Under replay the only durable
artifacts are the op log and a base snapshot, which Stage 3 (offline merge) wants regardless,
and which #415 can then read as *the* specification rather than authoring a second table.

## Reframing 2: the oracle is a same-JVM oracle, and the P1 harness cannot see that

This is the finding I would escalate before any merge code is written. Element geometry is
recomputed from text metrics on apply — `RotateElement` calls `rot.rotate(dir,
SwingTextMetrics.forGraphics(g))`, `AddElements` and `SetElementConfig` call
`el.init(SwingTextMetrics.forGraphics(g))` — and `width`/`height` are persisted `Attribute`s
(`src/jls/elem/Element.java:231,245`) unless a class overrides `sizeIsRecomputedOnLoad()`.
Only `Gate` and `WireEnd` override it. Of the sixteen `Rotatable` classes, fifteen —
`Register`, `Memory`-adjacent, `Display`, `Mux`, `Decoder`, `SubCircuit`, `Pin`, … — persist
metrics-derived sizes.

Consequence: **two replicas on different platforms, DPI, or look-and-feel fonts can produce
different `stateHash()` from an identical, perfectly-merged op set.** Worse, a headless replica
applying with `g == null` takes a different sizing path than a GUI replica. Byte convergence
across real machines — the actual student-facing claim in #171 §1 — is not currently attainable
by any merge design, and the in-process P1 harness is structurally blind to it because all
replicas share one font stack (and, in tests, one `null` Graphics). §11's threat list names
shared memory but not shared metrics. Ten thousand green seeded trials would certify nothing
about a Mac and a lab Linux box.

**I am explicitly disregarding the DoD line "No changes outside §8 (Method)" here.** The right
move is to settle this at #279, since every downstream convergence claim rests on it. Three
routes, in preference order: (a) apply remote ops through a fixed deterministic `TextMetrics`
implementation so geometry is a pure function of content — `jls.core.TextMetrics` is already an
interface and `SwingTextMetrics` is already the seam; (b) exclude derived geometry from the
canonical form (#166 scope change); (c) record geometry as replica-local non-canonical state and
weaken the oracle honestly. Add a prediction — replicas driven by *different* `TextMetrics`
converge — and the harness stops lying by construction. Note in passing that `jls.collab.op`
importing `jls.edit.SwingTextMetrics` is the seam pointing at this.

## Alignment with the arc

Strengthens it, with one duplication risk already correctly identified in the issue's own
adversarial comment (#415's per-kind table). Reframing 1 dissolves that risk rather than
negotiating it: with no per-kind rule *table in code*, there is nothing for #415 to collide
with — its AUTO column becomes derivable from "sort key + validation," and its STRICT column
is the same replay with the tiebreak removed and disagreement reported instead. That is the
"one table" the adversarial comment asks for, and the mechanism is one paragraph rather than a
data structure someone must keep synchronized.

One thing neither design solves, worth naming for the #163 pilot: two peers concurrently
drawing the same wire yields two wire objects with distinct ids. Both designs converge; both
converge on something a human did not intend. That belongs beside "LWW hides intent" in the
human-judged pilot legs, not in the property suite.

## What I would keep unchanged

§5 P2/P3/P4 as predictions, the seeded-RNG and shrunk-counterexample discipline, the
"every envelope crosses the real frame path" invariant, the headless layering rule, the
bounded-PR-lane/nightly-full CI split, and the §10 escalation path to reopen build-vs-buy.
Those are the parts of this issue that are right independent of which mechanism wins.
