# Issue #884: TASK-C880-2: erasure rides on the shared canonical form, and every one of the 435 pairs gets a deterministic score
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#884 is the middle task of a three-task chain (#883 corpus → #884 scoring →
#885 verdict) that measures, on a 30-submission synthetic corpus, whether any
schematic-similarity signal can separate 3 planted copies from 432
independent pairs — the premise gate for capstone #506/CAP-25. The task text
is internally disciplined (it repeatedly quotes its own boundary and cites a
KC-25-2 "stop, don't fork" clause), but two of its load-bearing premises do
not hold against the actual repository, and its central acceptance criterion
is not machine-checkable the way it is written.

## Findings, most severe first

### 1. The task's real prerequisite ("the shared canonical form") does not exist in the codebase at all — AC-1 is very likely to fire on day one

`ordering_after: [356]` and AC-1 both say erasure must be "built on" the
canonical representation "#356 and its task set own (#436's permanent-id
discipline, #409's structural-corruption reporting, #491's `ElementId`
identity)". I checked all three:

- `grep -rn "sref|sprobe" src/` and `docs/file-format.md` — **zero matches**.
  The stable-id reference item kind that #436 (TASK-0005) is supposed to
  introduce does not exist anywhere in the tree.
- `grep -rn "class SemanticCheck" src/` — **zero matches**. #409's
  (TASK-0031) post-load validation pass, which #884's own `related` list
  cites for "structural-corruption reporting," does not exist.
- #356 itself is `state: open`, and its own body states under "Absent at
  `2d0ca9d`, verified": "There is no validator," "the `sref` item kind
  TASK-0005 introduces does not exist," "There is no merge machinery of any
  kind." #436 and #409 are themselves `state: open` — neither has landed.

So the thing AC-1 requires erasure to be built "on" is, at the time this
issue was filed, a design document with no shipped code. `ElementId`
(`src/jls/elem/ElementId.java`) does exist and is real (#165/#183, closed),
but that is bare per-element identity, not the reference-form / merge-safety
"canonical form" #356 and #884 both mean by the phrase. AC-1's own escape
hatch — "If the shared form cannot carry erasure, this task stops and
reports that" — is therefore the *overwhelmingly likely* outcome for anyone
who picks this up today, not a contingency. Filing a 1–1.5 mw task whose
first acceptance criterion is expected to immediately resolve to "not ready,
bounced to #506/#506" is either premature filing or the task is quietly
expected to sit unstarted for a long time; either way the issue doesn't say
which, and doesn't flag how far away #356 actually is (its own critical path
is TASK-0005 → TASK-0031 → TASK-0032, none filed-and-landed, feature band
9–13 mw).

**Recommendation:** Either state explicitly in the issue that it is expected
to be picked up and immediately discharged as "waiting, here is what's
missing" (making the 1–1.5 mw band misleading — the real cost of a bounce is
under an hour, not a week), or make `blocked_by` include #436 and #409
(not just #883), so the tracker itself reflects that #884 cannot productively
start before those land.

### 2. AC-2's "label-refinement fingerprint" is not specified enough to produce a "score," and the gap is exactly the kind of judgment call AC-6 tries to rule out

AC-2 requires "the refinement rule, the label alphabet and the number of
rounds" to be stated up front, "so a later reader can tell a principled
choice from one fitted to three planted pairs." But label-refinement
(Weisfeiler–Leman-style) hashing produces, per round, a *partition of nodes
into label classes* — not a scalar similarity between two graphs. Turning
that into "a score for every one of the 435 pairs" (the Outcome, and AC-3)
requires an additional, unstated step: e.g. histogram intersection, Jaccard
similarity of label multisets per round, tree/subtree kernel inner product,
edit distance between label sequences, etc. The issue pins the refinement
rule and the round count but is silent on the comparison function that turns
two fingerprints into a number — which is at least as consequential a design
choice as the round count, and is exactly the kind of "fitted to three
planted pairs" decision AC-2 says it wants to guard against. As written, an
executor could choose any pairwise-comparison function post hoc, observe how
well it happens to separate the 3 planted pairs, and ship it — with AC-2
technically satisfied (rounds/alphabet/rule are all documented) while the
actual discriminating step was tuned exactly the way the criterion warns
against.

**Recommendation:** AC-2 should also require the score function (how two
per-round label fingerprints combine into a scalar) to be stated and
committed before the scores are computed, with the same "principled, not
fitted" language applied to it explicitly.

### 3. No erasure of topology-preserving no-ops is specified, which is likely to break exactly one of the three planted transform classes

#880's Open Question 2 (and #883 AC-3) fix the three planted disguises as
"moved components, renamed wires, and inserted no-op buffers." Positional
and name erasure (AC-1's scope) handles the first two by construction —
position isn't in the graph, names are erased. It does **not** handle the
third: inserting a no-op buffer adds a node and two edges where there was
one edge, which under any WL-style label-refinement scheme changes the
multiset of labels at every node within the buffer's neighborhood-radius,
for every round after the one that first touches it. Unless erasure also
collapses degree-2 pass-through elements (a *structural*, not merely
positional/nominal, canonicalization), the buffer-inserted planted pair is
likely to score conspicuously *lower* than a genuinely unrelated pair of
similar size — a false negative baked into the tool, not a genuine
finding about the premise. The issue's Boundary section explicitly excludes
"a production-grade invariant canonical graph... flattening policy" (that's
PF-1) but says nothing about the much narrower no-op-collapse step that
this specific corpus's own planted transform class requires. This is a real
tension between #884's stated boundary and #883's stated fixture design that
neither issue calls out.

**Recommendation:** Either add an AC (or an explicit Boundary carve-out with
rationale) addressing whether no-op/pass-through structural erasure is in
scope, or flag on #880/#883 that the "inserted no-op buffers" transform class
may not be measurable by "position and name erasure" alone, before #883's
corpus locks in that transform choice.

### 4. AC-1's "on, not beside" test is not operationally checkable — a private canonicalizer and a legitimate erasure layer are literally indistinguishable from the criterion as written

"Building a second canonicalizer is a KC-25-2 stop and fails this criterion
even if every other criterion passes" is stated three times across #884 and
its parent #880, which signals the author is worried about exactly this
failure mode — but no concrete, testable signal is given for telling the two
apart (e.g., "erasure code must call `Circuit.getElementByStableId` /
whatever #356 ships and must not introduce its own element-identity or
element-ordering data structure"). Without that, "built on the canonical
form" versus "beside it" is a code-review judgment call, and an executor
under schedule pressure (this is a small, likely-second-priority task
sitting behind a 9–13 mw feature) has a real incentive to write a thin
adapter that technically imports #356's types but re-derives the ordering or
identity logic itself to avoid waiting — which would satisfy a naive reading
of AC-1 while failing its stated intent.

**Recommendation:** Name the specific API surface (once #356 ships one) that
erasure code must route through, and/or add a structural test (e.g., "no new
identity-comparison or element-ordering code outside of calls into
`jls.<canonical-form-package>`") so AC-1 is checkable by a test rather than
by trusting the diff.

### 5. `blocked_by: [883]` points at a corpus whose own metadata is self-contradictory, and the corpus does not exist yet either

#883's machine block declares `blocks: [883, 884]` — #883 lists itself in
its own `blocks` field, a self-loop that is almost certainly a copy-paste
error in the tracker metadata (compare #884's own clean `blocks: [885]`).
Separately, `test/fixtures/similarity-corpus/` does not exist in the repo
(confirmed by directory listing), so #884's other stated prerequisite is
also unbuilt. Neither defect is fatal to #884 on its own, but combined with
Finding 1 it means **both** of #884's dependencies are simultaneously
unstarted, and one of them carries a data error in its own tracker
bookkeeping — worth a one-line fix on #883 before either is picked up.

### 6. Solid, no notes

- **AC-3 (exhaustive, no pruning) and AC-4 (byte-identical, manifest-order
  output)** are concretely testable and correctly scoped — nested-loop
  O(n²) over 30 items is trivial, and pinning row order to the manifest
  rather than map/filesystem iteration is the right call for determinism.
- **AC-6 (no threshold) and AC-7 (no verdict vocabulary)** are clear,
  narrow, and consistent with the sibling issues' (#880/#885) shared
  discipline — easy to audit by grep for banned words.
- **435 = C(30,2)** checks out arithmetically against #883's 30-submission
  corpus.
- **The band (1–1.5 mw) sums correctly** against parent #880's stated
  2–3 mw total together with #883 (0.5–1) and #885 (0.5) — unlike #356,
  where the same kind of arithmetic is off by 1.6–2.4x. No issue here.
- **The task decomposition rationale** (why determinism is split from
  honesty/#883 and from interpretation/#885) is genuinely well-motivated and
  reduces blast radius of review — a good structural choice.

## Verdict rationale

The acceptance criteria that are testable are well-written; the problem is
that the task's central technical premise (a shared canonical form to layer
erasure on) does not exist yet, one of its explicit dependencies (#883) has
a data bug and is also unbuilt, and its one genuinely novel technical
criterion (AC-2's fingerprint) has an unspecified step that undermines its
own "principled, not fitted" goal. This needs rework before execution: either
the ordering/blocking metadata should honestly reflect the #436/#409
dependency, or the issue should say plainly that immediate "AC-1 stop and
report" is the expected outcome, and AC-2 should be tightened to cover the
score function, not just the refinement rule.
