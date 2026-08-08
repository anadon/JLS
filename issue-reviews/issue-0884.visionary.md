# Issue #884: TASK-C880-2: erasure rides on the shared canonical form, and every one of the 435 pairs gets a deterministic score
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the tier vocabulary and #884 is one sentence: *before JLS spends 14-21 mw on a
plagiarism-evidence tool, spend 1.5 mw finding out whether structure alone distinguishes a
copy from an independent solution.* That instinct is right, it is the best thing in the
CAP-25 family, and the discipline around it — pre-registered refinement rule (AC-2),
byte-identical reruns (AC-4), no threshold anywhere (AC-6), no verdict vocabulary (AC-7) —
is the most carefully thought-out set of criteria I have read in this tracker. Nothing
below argues against the goal.

What I am arguing against is the substrate. AC-1 makes the whole task ride on #356's
canonical form and stops if it cannot. That single choice is what I think is wrong, and it
is wrong in three independent ways: it names the wrong architectural seam, it inverts the
purpose of the gate, and it makes an erasure layer do work that the fingerprint should be
doing for free.

## Reframing 1: invariance belongs to the fingerprint, not to a stored representation

CAP-25's PF-1 says "canonical netlist-graph *form*," and #884 inherits that noun — a
representation you build, store, and then erase things from. But label refinement does not
consume a representation. It consumes a graph plus an *initial coloring*, and it is
invariant to exactly whatever you leave out of that coloring.

Give each element node an initial label of `(SaveTags tag, port count, per-put width,
put role)` and each net node a label of `(bit width, driver count)`. Position was never in
the label. Name was never in the label. **There is no erasure layer, because there is
nothing to erase.** "Position and name erasure applied to the canonical representation" and
"a coloring that does not read position or name" are the same function; one of them needs a
canonicalizer to exist first and the other needs nothing.

This is not a dodge around KC-25-2. KC-25-2 exists to prevent *two canonicalizers that can
disagree about whether two circuits are the same*. A pure function `long[] fingerprint(Circuit)`
that stores nothing and normalizes nothing is not a second canonicalizer — it is a
consumer of the graph, in the same category as `HdlExporter.buildModel`, which already
walks a `Circuit` into a position-free structural model today (`src/jls/hdl/HdlModel.java`,
`HdlExporter.buildModel` at `src/jls/hdl/HdlExporter.java:170`) and which nobody has ever
called a rogue canonicalizer.

There is also a deeper tension AC-1 never notices. #356's canonical form exists to make
references *permanently name their referent* — #436's permanent ids, #491's `ElementId`
identity, `sid` uniqueness enforced at `Circuit.java:1310-1320`. Identity preservation is
its entire reason for being. Similarity detection needs identity *destroyed*. Layering
erasure on the identity-preserving form means the first thing your layer does is throw away
the one property the substrate was built to provide. Sharing a substrate is good
architecture when both consumers want the same invariant. Here they want opposite ones.

## Reframing 2: the right seam is #468, and it is already filed and unblocked

If the fingerprint needs anything from the tree, it needs *the graph* — nets and the
elements attached to them, in a deterministic order. That is precisely #468 (TASK-0007):
`jls.netlist.NetPartition.of(ends, folds)`, a pure headless pass returning components in
first-encounter order, with `JumpAliasing.fuse` for the coarser electrical net. Its
`blocked_by` is empty. It already has five waiting consumers and an ArchUnit rule to stop a
sixth private copy from appearing. #872 (the combinational-cone extractor) is ordered
behind it for exactly the reason #884 should be: a graph pass must not become another
private net traversal.

#356's canonical form is a *serialization*. Building a graph fingerprint "on" it means
parsing the canonical text back into a graph — which is what `Circuit.load` +
`finishLoad` already does. The canonical text is the wrong altitude for this consumer.

**Concretely: change `ordering_after: [356]` to `ordering_after: [468]`.** The KC-25-2
stop stays — it just points at the seam that actually carries the structure.

## Reframing 3: the ordering as written inverts the gate

#880 exists so a cheap measurement can kill a 14-21 mw capstone before it is funded. Trace
the real path to #884's AC-1 as written:

- #356 has **zero filed tasks** — TASK-0005/0031/0032 are all "not yet filed."
- #356 is `blocked_by: [319, 334]` — two further unlanded features.
- #356's own Open Question 1 records a 3.5-7.5 mw unowned residual that **blocks funding**.
- #356's §1 verifies at its evidence commit that `sref` does not exist, no validator
  exists, no merge machinery exists.

So the realistic critical path to "the shared form can carry erasure" is on the order of
20+ mw of file-format work. A 1-1.5 mw premise gate has been sequenced behind twenty times
its own cost, and the near-certain execution of #884 today is: read #356, observe it has no
filed tasks, write a comment on #880 and #506, close. That comment is a ten-minute tracker
check, not 1-1.5 mw — and #885's KC-25-1 verdict, the thing this whole feature exists to
produce, never gets written at all.

**A gate you cannot reach is not a gate.** This is the single most important thing in this
review: the ordering edge that was added to protect the architecture has, in practice,
disarmed the kill criterion it sits under.

## The transform class nobody has costed

#883 plants three transforms: moved components, renamed wires, inserted no-op buffers.
Under the coloring above:

- **Moved components** — invisible. Position is not a label. Erasure does nothing.
- **Renamed wires** — invisible. Names are not labels. Erasure does nothing.
- **Inserted no-op buffers** — **not handled at all.** A buffer adds a node and splits a
  net into two, which changes every refinement round downstream of it. No amount of
  "position and name erasure" touches this. It needs either a degree-2 pass-through
  contraction before refinement, or a fingerprint tolerant to chain length.

So the one transform that actually tests anything is the one AC-1's framing does not
address, and two of three are free under any structural fingerprint whatsoever. The issue
names the wrong hard problem. Whatever else changes, `AC-2`'s "the refinement rule is
written down before it is tuned" should explicitly cover **the pass-through normalization
rule**, because that is the only place a fitted-to-three-planted-pairs choice can hide.

## Reframing 4: the corpus should be a generator, not thirty files

#883 AC-2 already requires that independent solutions come from "a stated enumeration
procedure over correct implementations." Once that procedure is written down, it *is* a
generator. Committing its 30 outputs as static `.jls` files and then declaring n=30 a hard
ceiling (with growing the corpus named as an antipattern) throws away the best property the
apparatus has.

Make the corpus a seeded generator and #884 scores `n(n-1)/2` pairs for any declared n.
Then #885 reports separation as a **function of n** — n=30, 100, 300 — instead of a single
brittle yes/no at the one scale where KC-25-0-1 warns the null model may be degenerate.
That is not "silently growing the corpus to rescue a result"; it is the honest version of
the same measurement, and it answers KC-25-0-1's "state the smallest scale at which this
would be meaningful" with a curve instead of a guess. Determinism (AC-4) gets *easier*, not
harder: a seed plus a declared n. AC-5's committed table still holds — commit the table for
each declared n.

## Where this strengthens the larger arc

Build the fingerprint as a real core primitive, not a measurement script under `test/`.
`jls.netlist.CircuitFingerprint.of(Circuit) -> long[]` with a documented refinement rule
has three consumers the day it lands:

1. **Similarity** (CAP-25) — this issue.
2. **Convergence assertions** (#279/#352). The CRDT suite today can only assert two
   replicas produced the same *bytes*, via `CircuitSnapshot`'s save-text round trip. A
   structural hash lets it assert they converged to the same *circuit*, which is the
   property that was actually meant.
3. **Semantic diff matching** (#356 itself). Matching "which element in A corresponds to
   which in B" when ids do not line up is a graph-matching problem, and refinement colors
   are the standard cheap prior for it.

That is the inversion worth naming: the fingerprint is not a downstream consumer of the
semantic-diff canonical form — it is a primitive the semantic-diff work can consume. The
ordering edge points the wrong way.

## Acceptance criteria I am explicitly disregarding, and why

- **AC-1, discarded as written.** "Erasure layers on the existing canonical form, else
  stop" is replaced by: the fingerprint is a pure function over `NetPartition` (#468) that
  never reads position or name, stores nothing, and canonicalizes nothing. KC-25-2's real
  hazard — two forms that can disagree about sameness — is not triggered, because no second
  form is created. If #468 has not landed, follow #872's precedent: write the traversal so
  #468 can absorb it, with a `WAIVED:` comment naming #468.
- **Ordering, changed.** `ordering_after: [356]` → `[468]`, with the reasoning above
  mirrored onto #880 and #506.
- **AC-3, kept and strengthened.** Nested loop, no pruning, correct. Add: no early exit on
  identical fingerprints either — an exact tie is a datum.
- **AC-2, AC-4, AC-5, AC-6, AC-7 kept verbatim.** These are the criteria that make the
  measurement trustworthy and I would not change a word. AC-2 gains the pass-through
  normalization rule as an explicitly pre-registered item.
- **Boundary "this task is not #872", kept but reread.** It is currently written as "do not
  duplicate the cone walk." Under this reframing both passes consume the same
  `NetPartition`, which is the stronger version of the same instruction.

## Bottom line

The goal is right and rare: measure a premise before funding it, and let "the premise is
false" be a legible pass. Keep all of that. Cut the substrate along the netlist seam
(#468) rather than the save-format seam (#356), make invariance a property of the coloring
rather than a stored erasure, make the corpus a generator so separation is reported against
n, and name the pass-through-buffer normalization as the one real technical decision in the
task. Those four changes turn a task that most likely discharges by writing a "blocked"
comment into a ~1 mw pure function with three consumers and an answer for KC-25-1.
