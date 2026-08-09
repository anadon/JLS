# Issue #280: Simultaneous editing: anti-entropy resync + partition/heal convergence (P2) over the Transport seam (collab Stage 2 slice)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is one of the more carefully cross-referenced issues in the collab
roadmap: its permalinks (`VectorClock.java#L26`, `CausalBuffer.java#L33`,
`Transport.java#L38`, `SocketSession.java#L51`, `LoopbackTransport.java#L34`,
`ChaosTransport.java#L23`, `OpEnvelope.java#L46`) all resolve to the exact
class/interface declarations claimed, the DAG walk (`blocked_by: [279]`,
parent `#171` blocked by `[167,168,169]`) matches both `#279` and `#171`'s
own machine blocks verbatim, and the "cycle-3 adjudication... the #257
seam suffices" claim is directly substantiated by `#171`'s own "Corrections"
section. That grounding is real work and should be credited. The issue
still has real gaps, mostly in acceptance-criteria rigor rather than in the
premise.

## Findings, most severe first

### 1. Gameable acceptance criterion: P2 has no minimum trial count

Prediction P2 requires convergence "in 100% of seeded trials; failing seeds
shrink and become regressions" — but never states how many trials constitute
a pass. Contrast with the sibling task `#279`, filed by the same author on
the same day, whose P2 explicitly pins "N∈{2,3,5} replicas, random circuits,
random concurrent op schedules, random delivery orders — byte-identical
`stateHash()` in 100% of **≥10^4 seeded trials**." `#280`'s own P2 and P3
carry no analogous floor, and `#171`'s I2 (the feature-level version of this
same property) also omits a count. As written, an implementer can satisfy
"100% of seeded trials" with three hand-picked easy schedules and technically
close every stated completion-criteria checkbox — "P2-P3 in §5 verified;
command and output recorded" doesn't require the output to contain any
particular N.

**Recommendation:** pin a trial-count floor (mirror `#279`'s ≥10^4, or state
why partition/heal schedules need fewer/more) before work starts, and add it
to §5 and the Completion Criteria checklist explicitly.

### 2. "P3" is reused for two unrelated things across this issue and its parent

Within `#280`'s own §5, P3 means "under chaos (drop/duplicate/reorder/delay)
a flapping link never double-applies and the pending log stays within its
declared bound" — a sub-property of anti-entropy under this same task. But
`#171` (the parent feature) defines its own **I3 (P3)** as "compaction-horizon
rejoin — an absent peer with unsent local ops rejoins via snapshot adoption +
own-op replay... Spans #280 + the compaction slice; **rig does not exist
yet, built by the compaction slice**." These are different tests owned by
different (and in the second case, unfiled) work. Yet `#280`'s title and
abstract advertise this task as scoped to "(P2)" only, while its own
Completion Criteria bullet 1 reads "**P2-P3** in §5 (Predictions) verified" —
so the task's own definition of done silently folds in a second "P3" that a
reader skimming the title would not expect, and that is trivially confusable
with the parent's unrelated compaction-rejoin P3 the same issue explicitly
disclaims in §13 ("Out of scope: compaction/snapshot-horizon rejoin").
Anyone tracking `#171`'s roster by grepping for "P3" will get false hits
across these two unrelated properties.

**Recommendation:** rename `#280`'s internal chaos-suite prediction (e.g.
"P2b" or "P2-chaos") to stop colliding with the parent's I3/P3 label, or
fold it into P2 outright since it's the same partition/heal suite run with
chaos enabled rather than a distinct research question.

### 3. The new bounded op log's acceptance bar is "documented," not tested — weaker than this codebase's own established discipline

Anti-entropy requires retaining a window of *delivered* ops so a
reconnecting peer can be sent what it's missing — but no such retained-op
log exists anywhere in `src/jls/collab/crdt/` today (`CausalBuffer` only
tracks the delivery clock and the transient *undelivered* `pending` list, per
lines 38-46 of `CausalBuffer.java`). This is a genuinely new piece of state,
its cap value is explicitly left to "decided during execution" (Open
Questions), and §7.11's failure-mode spec for it is just "log at capacity →
behavior documented." Compare that to how every other cap in this exact
subsystem is currently enforced and tested: `SecureLink.MAX_PAYLOAD_BYTES`
has `SecureLinkTest` at/over-cap cases, `CausalBuffer.MAX_PENDING` has
`CausalBufferTest.pendingOverflowIsRefused` (both cited by `#170`'s own
Background section as the precedent). "Documented" is strictly weaker than
"boundary-tested," and it is exactly the failure mode `#170` itself warns
about for this subsystem: "RSS growth unbounded under flood despite the
caps... audit every collection reachable from frame handling." A generous
cap that is never exercised by any mandated test can pass every checkbox
in this issue while leaving the memory-growth hazard `#170` describes.

**Recommendation:** add a boundary-value test requirement for the new op
log to §8/§14 (mirroring `pendingOverflowIsRefused`), and require the cap
value to be recorded with a rationale in the PR, not left silent.

### 4. New attack surface with no disconnect policy behind it (mentioned nowhere in §11 Threats to Validity)

§7.3 correctly flags the new clock-exchange and re-sent-envelope frames as
hostile input requiring caps and reject-never-repair, "same discipline as
`OpEnvelope`." But the peer-misbehavior policy that turns repeated
rejections into a disconnect — the thing that would actually stop a peer
from flooding malformed resync frames forever — is `#170`'s "planned
(misbehavior policy)" task, explicitly listed there as **"Not filed"** at
the time `#280` was filed. So at the point this task lands, a peer that
sends an endless stream of individually-capped-but-rejected resync frames
gets each frame rejected (bounded per-frame cost) but is never dropped from
the session — an availability nuisance, not a memory-safety hole, but one
`#280`'s own §11 (Threats to Validity) never mentions despite naming three
other threats in detail.

**Recommendation:** either note this residual gap explicitly in §11 (it's a
one-line addition and keeps the "recovery not prevention" framing `#170`
already established), or confirm in the PR that per-connection rejection
counters at least feed a future `#170` consumer even before disconnect logic
exists.

## What's solid (no action needed)

- The DAG/cycle-walk claim is verified correct against both `#279` and
  `#171`'s live machine blocks — `blocked_by: [279]` and the "no path
  returns here" reasoning both check out.
- All code permalinks resolve to the exact lines/declarations quoted; the
  "anti-entropy exists only as design comments" observation is accurate —
  `grep -rn "anti-entropy" src/jls/collab/crdt/` today turns up exactly the
  four comment-only hits the issue lists, no implementation.
- The "no quorum, no freeze of either side" (anti-Raft) framing is
  consistent with `#171`'s Capability Statement and `docs/collaborative-
  editing-research.md` §5.4 ("Partitions: both sides keep editing").
- Scope boundaries (no `.jls`/CLI/GUI change, no new op kinds, gossip and
  compaction explicitly deferred) are consistent with `#171`'s owner table
  and don't overlap adjacent open issues.
- `#170`'s "hostile input" framing for the new frame types is the right call
  even though full alignment with `#170`'s own outstanding rate-cap/misbehavior
  slices isn't guaranteed (see Finding 4) — the issue correctly scopes itself
  to the per-frame discipline it *can* deliver alone.

## Net assessment

The premise, dependency graph, and code grounding are sound and unusually
well fact-checked for an issue of this size. The concerns are all in the
*rigor of the acceptance criteria* — an unbounded trial count, a label
collision with the parent's own roadmap, and a cap-and-log requirement that
asks for documentation where the project's own precedent asks for tests —
any of which would let a technically-compliant PR under-deliver on the
stated research question without technically failing a checkbox. None of
these block starting the work; all four should be tightened before the PR
that closes this issue is reviewed.
