# Issue #171: Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo (collab Stage 2)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of its machine block, #171 claims one thing: **JLS should be able to merge
two people's edits to one circuit without a server, without a leader, and without
ever producing a file that means something neither of them drew.** Everything else —
causal broadcast, vector clocks, anti-entropy, compaction, gossip — is transport
plumbing in service of that claim. The claim is right, it is already funded by the
project's own trajectory (`docs/grand-architecture.md` §2 names the collaborative
editor as one of three latent products), and the recorded anti-Raft stance
(`docs/collaborative-editing-research.md` §4: a lab pair is n=2, majority=2, so any
sleep freezes both) is a genuinely good architectural judgement that I would not
disturb.

But the issue locates that claim in the wrong place in the tree, and the project has
already written down the better location — in a document #171 never cites.

## Reframing 1 (the load-bearing one): the merge table is not this feature's to author

`docs/capability-roadmap/lf-06-diff-merge-vcs.md` §C4 states the position outright:

> The per-kind merge rule table is the same object for the online collaborative
> merge and the offline git merge. Neither exists yet. Build it once, in a
> state-based form, offline first — and the offline three-way merge tool becomes the
> executable specification and the test oracle of the online CRDT.

That is now filed: **#356 (FEAT-012)** owns semantic merge safety and **#415
(TASK-0032)** owns the table — with a STRICT column that may report a conflict, an
AUTO column derived from it by appending exactly one `(Lamport, peer id)` tiebreak,
and a cross-check property `merge3(base, apply(A), apply(B))` ≡ post-exchange replica
bytes. #415 says in terms that "the AUTO column **is** the CRDT's merge policy".

#171's own 2026-08-04 absorption comment agrees ("#356 … shares TASK-0032, the merge
rule table. Not currently in this feature's machine block; add it, mirrored") — but
the body's `blocked_by: [167, 168, 169]` still does not carry #356, and **#279, the
declared critical-path head, is filed as fully unblocked and authors the rules
itself**: its §8 reads "Merge metadata + rules per the research doc §3 table … in
`jls.collab.crdt`", online-only, no STRICT/AUTO split, no edge to #415. Two agents
picking up #279 and #415 in the same week produce two answers for the same record
kind, and the file that comes out of a git merge is not the file that comes out of a
converged session. That is exactly the drift both documents exist to prevent, and it
is live right now, not hypothetical.

**Concretely:**

1. Add `356` (and the task edge `279 ← 415`) to #171's `blocked_by`/roster, mirrored
   on #356 and #415.
2. **Retitle and rescope #279** from "per-kind confluent CRDT merge rules + P1 suite"
   to *"derive the AUTO policy from `jls.merge.MergeRules` by appending the
   `(Lamport, peer id)` tiebreak; prove H3 (offline merger ≡ replica exchange) with
   the P1 convergence suite"*. The online-specific code left in #279 is then the
   tiebreak and the property harness — nothing else. This is a large scope reduction
   for the critical path, not an addition.
3. Delete "per-kind merge rules" from #171's §3 *Modifies* list. `jls.collab.crdt`
   becomes a **consumer** of `jls.merge`, not the home of merge semantics. This also
   removes a layering oddity: `jls.merge` is usable by the batch/CLI surface, which
   `docs/grand-architecture.md` §1 calls a co-equal front end, whereas anything under
   `jls.collab.crdt` is reachable only from a live session.

If the maintainer would rather keep the table in `collab`, that is defensible — but
then #356/#415 must be closed as duplicates, and the offline half of the value dies.
It cannot be both.

## Reframing 2: make the replica a pure fold, not an imperative apply-in-place

#279 §7.4 provides "apply a delivered `OpEnvelope` under the per-kind rules" —
mutable replica state plus per-element merge metadata (§7.8). Every later slice then
pays for that choice: anti-entropy needs clock exchange and missing-op transfer
(#280), compaction needs a horizon and a rejoin protocol, undo needs inverse stacks,
and each gets its own suite.

The alternative is one function:

```
fold : (snapshotBytes, Set<OpEnvelope>) -> canonicalBytes
```

deterministic in the *set*, independent of arrival order, with non-applicable ops
skipped and counted rather than thrown. Grant that and the following stop being
separate slices:

- **Convergence (I1)** is `fold(S, O)` invoked twice on shuffled iteration orders —
  a pure-function property, no N-replica scheduler, no in-process rig.
- **Anti-entropy (I2/#280)** is set difference over op ids. Zero loss and zero
  double-apply are properties of a *set*, not of a protocol; IC-2's "transfers less
  than a full snapshot" is then a message-size assertion, not a correctness one.
- **Compaction (I3/IC-3)** is `fold(fold(S, prefix), suffix) == fold(S, all)` — an
  algebraic identity checked without a rejoin rig at all. #171's I3 currently needs a
  rig that "does not exist yet".
- **Rejoin past the horizon** is the same call with a newer `S`.
- **Offline merge (Stage 3)** is the same call with `S` from a file.

This is what #415 means by "state-based form": the document state is a join-semilattice
whose normal form is the canonical save, and op-based causal broadcast is a *delivery
optimisation* over it, not its definition. `CausalBuffer`'s own javadoc already says
delivery is not convergence. I would write the fold first and keep `CausalBuffer` as
the latency optimisation it is.

## Reframing 3: undo by subtraction, not by inversion

#171 plans per-peer stacks of validated inverse ops, and the absorbed #352 raises it to
a global invariant ("every op in the vocabulary has an exact inverse, at every
intermediate landing") plus a whole risk class (H3: an inverse arriving
uncorrupted-but-invalid is also a #170 finding), plus an unresolved three-way open
question about dependent ops (refuse / cascade / let-fail).

Under the fold, "undo my last op" is `fold(S, O \ {o})`. Consequences:

- It is *selective* undo for free, always exactly valid, because the result is
  by construction a state the fold can produce.
- The dependent-op question dissolves: an op whose target no longer exists simply does
  not apply and lands in the count the P3 slice already wants to surface as
  "n edits no longer applied". The three-way decision (a)/(b)/(c) never has to be made.
- Invariant 5 (universal exact inverses) can be dropped, and with it the constraint it
  places on every future op kind filed under #167.
- The H3 corruption class disappears rather than being tested for.

Cost: a re-fold per undo. For classroom circuits under a compaction bound that is
milliseconds, and the existing snapshot-undo path (`SimpleEditor.java` ~L5641,
whole-circuit `CircuitSnapshot` restore) is already far more expensive per gesture.
`CircuitOp.invert` still earns its keep for the local editor and for #415's inverse
round-trip test — it just stops being the *mechanism* of collaborative undo.

## The contradiction nobody has named: Stage 3 versus §4

#171 §4 makes it a global invariant that there is "no `.jls` format change" and §3
records "**Durable data:** none new on disk; the op log is session-ephemeral". The
research doc simultaneously promises that Stage 3 (offline/async merge) "falls out of
the same machinery". Both cannot hold: a persisted op log is new durable data, and
per-element CRDT metadata in the save is a format change. LF-06 §1 measures the
consequence — the `collab.op-observer` seam
(`src/jls/collab/op/OpExtensionPoints.java:25-27`) has **zero contributors**, so
nothing on disk exists from which history could be reconstructed.

Decide it now, because it changes what #415's table may assume:

- **(a) Sidecar `.jlslog` next to the save** — no `.jls` change, invariant intact,
  Stage 3 and "merge changes from file" become real, and the op-observer seam finally
  gets its first contributor.
- **(b) Metadata-free three-way merge from a common base** — no log at all; the table
  is keyed on stable ids and derives situations by comparison. This is what #415
  already assumes, and it is why #415 does not need a vector clock.

(b) is the cheaper and more elegant answer and it is already the filed one; (a) is
strictly additive later. Either way, say so in §3 rather than leaving "Stage 3 is
deferred, unfiled" next to an invariant that forbids it.

## Trajectory: this is the most-built subsystem serving the least-evidenced demand

Four packages, ~30 classes, Ed25519 identity, handshake, SAS, secure link, roster,
transports and 20 op types have shipped for collaboration — and **no student has yet
used any of it**, because Stage 1 (#169's session lifecycle and floor-control token)
has not landed. #171 proposes 14–22 maintainer-weeks (its own §8 cost record) to
*remove* a token nobody has yet found painful, on a single-maintainer project whose
own grand architecture calls the unextracted headless core (#77) "the highest-leverage
single change in the tracker". Meanwhile `CircuitOp.apply(Circuit, Graphics)` still
takes a `Graphics`, so the headless replication core cannot be headless until #337
lands — a keystone problem wearing a collab hat.

The project applies a demand gate elsewhere and should apply it here: i18n was declined
for having "no requesting user" (ARCHITECTURE.md), and #212's external providers wait
on a demand gate. **Recommendation:** make Stage 1 in one real lab session the revisit
trigger for the P4/pilot legs and the token-retirement slice. The merge-rule and fold
work below it is worth doing regardless, because #356/#415 need it with or without a
socket — which is precisely LF-06's argument, and it is the strongest argument in
favour of doing most of #171's substance sooner rather than later.

## Disregarding parts of the stated acceptance criteria

I am explicitly setting aside three Definition-of-Done lines:

- **"Stage 1 token retired: read-only mode removed"** — retiring a mode that has never
  shipped to a user is not a completion criterion, it is a hypothesis about ergonomics.
  Replace with: the token stays as a user-selectable mode until a pilot shows it is
  the worse experience.
- **"The undo-semantics decision recorded in ARCHITECTURE.md's decision log"** — under
  Reframing 3 there is no decision left to record; the entry becomes "undo is op-set
  subtraction and re-fold", one line, and the open question closes without adjudication.
- **I1's "N∈{2,3,5} replicas … ≥10⁴ seeded trials"** — under Reframing 2 the same
  guarantee is a permutation property of one pure function. Keep the seed discipline,
  drop the replica count from the criterion; it is measuring the harness, not the claim.

One addition of my own: **ARCHITECTURE.md's module layout does not mention
`jls.collab` at all**, despite four shipped packages. A subsystem this large being
invisible in the contributor's map is a bigger documentation defect than the missing
undo entry, and fixing it costs a paragraph.

## What I would not touch

The byte-identical `stateHash()` oracle (and #352's insistence that weakening it to
structural equivalence is a stop condition); no quorum anywhere; the `Transport` seam
with `LoopbackTransport`/`ChaosTransport` (#257) — that was the right seam and it
retired a blocker cleanly; the rejection of `automerge-java` on pure-Java/multi-arch
grounds; the strict reject-don't-repair envelope reader; and the enforced headless
layering. The bones of this feature are good. The reframing is about *where* the merge
semantics live, *what shape* the replica is, and *in what order* the value lands.
