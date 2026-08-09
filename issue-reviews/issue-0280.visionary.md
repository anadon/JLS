# Issue #280: Simultaneous editing: anti-entropy resync + partition/heal convergence (P2) over the Transport seam (collab Stage 2 slice)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

One sentence: *a laptop lid closes mid-lab-pair and when it opens the two circuits are
the same circuit again.* Everything else — clock frames, missing-op sets, log caps,
horizon rejoin — is machinery for that sentence. The goal is load-bearing and correctly
placed on the critical path: without post-heal convergence, #171's whole capability
claim is conditional on nobody's Wi-Fi flapping, which in a university lab is not a
condition.

The mechanism, however, is the textbook one rather than the one this codebase makes
nearly free, and choosing it commits #171 to a downstream slice that would otherwise not
need to exist.

## The reframing: ops on the hot path, *state* on the repair path

The issue's §7.10 pipeline is `reconnect → clock exchange → four-way compare → missing
OpId set → envelope transfer → dedup → merge → converged bytes`. Every stage after
"clock exchange" exists to **reconstruct state from history**. But JLS is the rare
project that already has the state itself in a canonical, hashable, compact form:

- `Circuit.stateHash()` (`src/jls/Circuit.java:1548`) — SHA-256 over the canonical save
  text. A one-frame divergence detector, already shipped by #166.
- `CircuitSnapshot` (`src/jls/edit/CircuitSnapshot.java`) — deflated canonical save
  bytes; already the undo and checkpoint representation, and already the "snapshot" the
  research doc §5.4 compaction horizon wants.
- The workload is small. The largest circuit anywhere in this tree is
  `test/fixtures/riscv-sum1to10.jls` at 120 KB; `riscv/gui/cpu.jls` is 8.9 KB. A
  classroom circuit's *entire state* is one LAN round trip.

So the resync can be: **exchange `stateHash()`; if equal, stop — zero further frames; if
unequal, exchange the CRDT state and join it.** That is legitimate precisely because
#279 is authoring add-wins element sets, tombstones, per-attribute LWW registers and
OR-set wires — every one of which the research doc §3 names in its *state-based* form.
These structures form a join-semilattice. Convergence then holds by algebra (join is
idempotent, commutative, associative) rather than by bookkeeping ("did I compute exactly
the right missing-op set?").

This is not "delete the op path". Ops stay on the hot path: they are small, incremental,
low-latency, carry intent for attribution and the sync indicator, and the `CausalBuffer`
that orders them is correct and landed. The claim is narrower and standard in the
literature: *op-based propagation, state-based anti-entropy*. You never need to re-send
an op, because repair is a state join.

### What disappears if you cut here

- §7.8's bounded per-session op log — gone. There is no log.
- §8's "bounded op log with declared cap; at-capacity behavior documented" — gone.
- The sole Open Question ("op-log cap value and at-capacity policy") — dissolved, not
  answered.
- §7.11's "log at capacity → behavior documented, horizon rejoin deferred" — the state
  it describes cannot occur.
- **#171's entire planned slice "log compaction to snapshots + horizon rejoin (P3)"**,
  its integration criterion I3, and the "n of your offline edits no longer applied"
  machinery. Compaction exists *only* because a log exists. This reframing deletes a
  whole task from the roadmap rather than deferring it.

### The safety argument, which is the real reason

Under op-diff, the repair mechanism can lie. A bug in the clock or dedup bookkeeping
converges the *clocks* while leaving the *states* divergent — both peers then believe
they are in sync and the sync indicator says "in sync ✓" over two different circuits.
That is exactly the silent-lost-edit harm #163's program gate exists to measure,
reintroduced by the thing meant to prevent it. §10's falsification criteria catch it only
if a seeded schedule happens to expose it, and §11 already concedes the seed corpus may
under-sample.

Under hash-compare-then-join, the oracle and the repair are the same representation.
Divergence is *detected* by the same bytes it is *repaired* into. Any residual bug
anywhere in the online op path — a merge-rule error, a dropped frame the transport never
noticed, an equivocating peer — is repaired by the next sync round. The property becomes
"eventually correct by construction", not "eventually correct if the tests were thorough
enough". For a tool whose users are students who will never file a bug report, that
difference is the whole game.

## Second reframing: there is no such thing as a reconnect

§7.4 places the entry point "on the session layer's reconnect path". But `Transport`
(`src/jls/collab/net/Transport.java`) has no reconnect concept at all — a channel ends
(`receive()` returns null, "then and forever") and the layer above constructs a new one.
`KnownPeers` mentions reconnect only to skip re-verification. So the trigger is really
*link establishment*, which is indistinguishable from a first join.

Accept that and **join, rejoin, partition-heal, and periodic health check become one
operation**: sync-with-peer, run unconditionally on every link-up and periodically over
live links. This is strictly stronger than the issue's design, whose on-reconnect-only
trigger cannot repair a divergence that arises without a link drop. It also makes #163's
sync indicator ("in sync ✓ / syncing… / diverged — merging") a *measured* fact rather
than an inferred one — a user-facing win the issue does not claim and would get free.

## Third reframing: this is the same function #415 and Stage 3 need

The adversarial comment already on #279 establishes that **#415** is authoring a
state-based per-kind merge table (`jls.merge.MergeRules`, STRICT/AUTO columns) from the
offline direction, and correctly insists #279 write its rules down "as data in one
place" so #415 need not re-author them. Follow that one step further:

| Consumer | Under #280 as written | Under the reframing |
|---|---|---|
| Online partition heal (#280) | vector-clock diff + op-log replay | `join(mine, theirs)` |
| Offline three-way file merge (#415) | separate state-based merger | same table, `merge3` |
| Stage 3 "merge changes from file" (research §7) | requires persisting the op log | same join, against a file |

Three mechanisms for one idea becomes one mechanism with three call sites. Stage 3 in
particular is described in the research doc as "anti-entropy against a file instead of a
socket" — that sentence is *trivially true* of a state join and requires a whole new
durable-log format under op-diff (§7.7 currently says "nothing written to disk", which
quietly forecloses Stage 3).

## Trajectory checks

- **Ethos fit.** ARCHITECTURE.md's recorded decisions are consistently "one mechanism,
  no speculative second": one simulation strategy (#221), in-process plugins with IPC
  *reserved* not built (#222), i18n a non-goal. An op log whose only job is to
  reconstruct state that is already cheaply representable whole — plus a compaction
  horizon whose only job is to bound that log — is precisely the second mechanism that
  ethos declines elsewhere.
- **Documentation debt.** `src/jls/collab/**` is ~7,900 lines across four packages,
  larger than `jls.sim`, and ARCHITECTURE.md — self-described as "the map a new
  contributor needs" — does not mention it once. #280 adds a distributed-systems
  mechanism to a subsystem that is invisible to the contributor map. Not #280's fault
  and not a blocker, but the arc is outrunning its own documentation discipline, and
  #171's DoD already owes ARCHITECTURE.md one entry.
- **No on-ramp yet.** `CollabModule.register()` contributes nothing; no student can start
  a session today. §11 says it honestly: "a green chaos harness is not a pilot pass."
  When you cannot yet observe the real failure mode, the argument for the mechanism with
  fewer moving parts gets *stronger*, not weaker.

## Acceptance criteria I am disregarding, and why

I am endorsing §5 (P2, P3), §10, §11, and the `Transport`-seam choice **verbatim** —
those predictions are mechanism-independent and are the best-specified part of the issue.
Partition/heal schedules with both sides editing must end at equal `stateHash()` with
zero loss and zero double-apply whichever way the repair is implemented.

I am disregarding, and recommending struck:

- §7.8 and §8 bullet 3 (bounded op log with a declared cap) — replaced by "no log".
- The Open Question on cap value and at-capacity policy — dissolved.
- §8 bullet 2's "missing-op computation … transfer" — replaced by "state hash compare;
  on mismatch, exchange and join CRDT state".
- §7.11's log-at-capacity and horizon-rejoin deferral clauses.
- Correspondingly, #171 should drop the compaction/horizon-rejoin planned slice and I3
  rather than carry them as unfiled debt.

What replaces them in §8: a state-frame codec built on the existing canonical save form
(same #170 discipline — strict typed reader, caps, reject-never-repair); a `join` on
#279's rule table asserted idempotent/commutative/associative by property test; a
periodic-and-on-link-up sync driver; and a tombstone-retention note (bounded by *deleted
elements*, not by ops — orders of magnitude smaller, and GC'd by the same lazily-agreed
minimum-clock horizon §5.4 already describes, if it ever matters).

## Residual risks of the reframing, stated honestly

Bandwidth per repair is O(state) not O(missing ops). At 8–120 KB over a LAN this is
noise; if lecture-scale sessions ever arrive (#163 open question 1), δ-state CRDTs give
back the op-sized deltas *without changing the semantics* — the simple thing scales in
the one direction the project might later need. Second, whole-state join requires the
merge metadata to be serializable, which is a real constraint on #279's API shape — but
#415 wants exactly that constraint anyway, so it is a coordination win, not a new tax.
Third, this makes #280 depend on #279 more deeply than the current edge does: #279 must
land its rules *as a joinable state table*, not merely as an apply-time decision
procedure. That should be recorded on #279 now, before it is picked up, because it is
cheap to design in and expensive to retrofit.
