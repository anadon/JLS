# Issue #171: Simultaneous editing: op-based CRDT replication, anti-entropy, compaction, collaborative undo (collab Stage 2)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is a feature-tier tracking issue for turning JLS's landed causal-delivery
substrate (`jls.collab.crdt`) into confluent, byte-identical, multi-writer
replication with anti-entropy, compaction, and per-user undo. It is unusually
disciplined for a tracking issue — decomposed into filed children (#279,
#280), a DAG dependency walk, and an explicit convergence oracle
(`Circuit.stateHash()`). The core claims check out against the repo at HEAD
(`5311625`). The findings below are about staleness between the issue body
and its own most-recent comment, an unstated cost/feasibility budget, and one
under-examined technical risk (font-metric-dependent geometry in a headless
merge path) that bears directly on the "byte-identical" oracle this issue's
entire acceptance criterion rests on.

## Findings, most severe first

### 1. The issue's own last comment admits the body is stale, and nothing has updated it

The 2026-08-04 comment absorbing #352 (issue #171#issuecomment-5175826860)
introduces four new integration criteria (IC-2 transfer-bound, IC-3
compaction-is-a-no-op, IC-4 per-kind inverse round-trip, IC-8 merge-table/type
agreement), a new shared dependency (#356, via the shared merge-rule table),
a new capstone (#299), and sharpened undo-semantics options — and says so
explicitly: *"This feature's `planned_tasks` currently name merge rules only
in the online form; the shared table with #356 is new information and
should be reflected there."* That is a self-issued TODO. As fetched, the
issue **body** (§5 Integration Criteria, the `related:` list, `planned_tasks`)
still only has I1–I4 and `related: [165, 166, 170]` — no #356, no IC-2/3/4/8,
no #299. A future contributor or agent working strictly from the body (the
place the issue's own rules say the DoD lives — "Machine block, roster table,
and mermaid graph agree with reality at close") will miss criteria the
maintainer has already decided belong here. **Recommendation:** fold the
comment's additions into the body now, not at close — the issue's own rule 6
discipline (correcting stale claims) applies to itself, not just to
inherited citations.

### 2. No cost/feasibility budget carries into this issue despite absorbing one

The absorbed #352 carried a explicit "Cost" section: band 14–22 maintainer-weeks,
a printed row sum of 8.0 wk with 2.75× headroom to the band ceiling, and an
open question about the gap. #171's own body has **no Cost section at all** —
not before the absorption, not after. The scope #171 now owns (per its own
Decomposition table and the absorbed comment) is: merge rules (#279, filed),
anti-entropy (#280, filed), compaction, undo, gossip/token retirement, RGA
for ordered substructures, the P4 pilot suite, *and* the shared merge-rule
table with #356's semantic-merge-safety feature. That is materially larger
than what #279+#280 alone estimate, yet the tracking issue that is supposed
to own the aggregate feasibility judgment states no budget and carries none
forward from the issue it absorbed. **Recommendation:** either restate #352's
cost band here (adjusted for the parts #170 now owns instead) or explicitly
record "no cost estimate carried" as a decision, since the completion
criteria checklist has no line item that would catch its absence.

### 3. The byte-identical convergence oracle has an unexamined dependency on host font metrics for at least two op kinds

`RotateElement.apply` (`src/jls/collab/op/RotateElement.java:24-31`) calls
`rot.rotate(orientation, SwingTextMetrics.forGraphics(g))`, and
`FlipElement` follows the same pattern. `SwingTextMetrics.forGraphics(null)`
returns `null` by design (`src/jls/edit/SwingTextMetrics.java:60-70`,
docstring: *"Element sizing treats a null TextMetrics the same way it treated
a null Graphics (skip sizing)"*) — so headless merge application (no AWT
`Graphics` available on a replica applying a remote op) silently **skips**
geometry recomputation, while the originating peer's local apply had a live
`Graphics` and *did* recompute geometry from real font metrics. Every element
class implementing `Rotatable.rotate(Orientation, TextMetrics)` (Adder, Mux,
Gate, Register, Splitter, TriState, Group, Display, Clock, Constant, Decoder,
Binder, FieldExtend, Pin, ShiftRegister — `src/jls/elem/*.java`) takes this
nullable-metrics parameter, meaning the null-safe path is pervasive, not an
edge case in one class. Under the anti-Raft design, a second peer can receive
and merge-apply a `RotateElement` op it did not originate. If the resulting
saved geometry bytes depend on whether sizing ran with real font metrics vs.
was skipped, then two replicas that both eventually apply the *same*
`RotateElement` — one locally (with `Graphics`) and one via replication
(with `null`) — are not obviously guaranteed to serialize identically. I
found no test in `test/jls/collab/` (searched `test/jls/collab/op` and
`test/jls/collab/crdt`) that asserts byte-identity between a `Graphics`-backed
apply and a `null`-Graphics apply of the same op — `CircuitOpTest.java` is
the only file that even references null-Graphics application, and it was not
possible to confirm from static inspection alone whether it checks this
cross-replica parity case or just single-replica null-safety. This is
precisely the class of hazard #171 §11 (inherited into #279/#280) gestures at
generically ("Byte convergence does not prove meaning convergence if
canonical serialization under-captures state") but never names concretely.
**Recommendation:** #279's P1 suite should explicitly include a schedule
where a rotate/flip op is applied once via `Graphics` and once via `null`
on different replicas, asserting equal `stateHash()` before this ships as a
closed finding — right now it is an assumption, not a verified property.

### 4. `blocked_by: [167, 168, 169]` is asserted as "close-out gates only, never children's start," but #167 — one of those three — is the very issue that closes the CircuitOp headless/Graphics gap raised in Finding 3

The body states: *"#167/#168/#169 gate close-out only, never children's
start."* That is defensible for #168 (transport, superseded by the #257
seam per the cycle-3 adjudication) and #169 (roster/presence surfaces, not
consumed by #279/#280's in-process work). But #167 is explicitly the issue
that owns closing the op vocabulary to headless application — its own body
says the remaining children (#282, #283) migrate "placement, wire-drawing,
paste" and "dialog commits" into ops, and the absorbed #352 comment names
`CircuitOp.apply(Circuit, Graphics)` at `CircuitOp.java:51` (confirmed still
present at HEAD, `src/jls/collab/op/CircuitOp.java:51`) as the concrete
un-closed surface — "TASK-0037... this feature *requires* the closure, it
does not own it." If Finding 3's geometry-parity gap is real, it is a #167
problem in substance (closing the vocabulary to true headless application)
being declared irrelevant to #171's children's *start* while remaining
directly relevant to whether #279's P1 suite can honestly assert
byte-identity for the ops that exist today. The dependency-graph mechanics
are internally consistent (no cycle), but the substantive claim that #167
only gates *close-out* undersells that #167's remaining work touches the
exact op kinds #279 must already merge-test.

### 5. The "gesture-complete" claim is narrower than its plain reading suggests

Cycle-3's comment states flatly: *"the OR-set wire merge rule its full
real-gesture input space."* The body's Prior Work section similarly presents
add/remove/move/wire ops as complete inputs for #279's merge rules. That is
true for the *kinds* #279 scopes itself to (§11: "ordered-substructure RGA is
explicitly out of scope here"). But #167 (still open, 15 comments, two open
child tasks #282/#283) lists outstanding gestures — placement, wire-drawing
commit-time composition, paste, dialog commits via `SetElementConfig` for
`ClockDialog`/`ConstantDialog`/`MemoryDialog` — that are not yet routed
through `OpSink` at all per #167's own Decomposition table ("#282 | open",
"#283 | open"). A reader who takes #171's "gesture-complete for flat
structure" at face value could reasonably assume the full editor surface is
already covered by ops and therefore mergeable; it is not — #279/#280 are
scoped to a subset, and that scoping is correct and stated, but the framing
in the Prior Work section reads more sweeping than the actual coverage.
**Recommendation:** state explicitly in §1 (Capability Statement) which
gestures are and are not yet expressible as ops, rather than only in #167's
separate tracking issue.

### 6. Acceptance criterion I1 is honest but expensive, and its CI-lane compromise is left undecided at the tracking-issue level

I1 requires "≥10^4 seeded trials" per replica count in {2,3,5} for 100%
`stateHash()` equality. This is a legitimate, hard-to-game bar (unlike a
handful of hand-picked scenarios) — a genuine strength, noted per instructions.
But the "Open Questions" section punts the PR-lane-vs-nightly split to #279,
and #279 in turn punts the exact trial split to "executor decides at
implementation; rides along." Given `mvn verify` is the blocking gate for
every push (README, CONTRIBUTING), a bounded-trial PR lane is almost
certainly required to keep CI tractable — but no default number is
committed anywhere in the three issues (#171, #279, #280), which is a
concrete way this acceptance criterion could be gamed later: an executor
picks a trivially small "bounded" PR trial count, and the honest 10^4 count
only runs nightly where a maintainer must notice failures. Not a defect in
the issue as filed, but a real gap the issue could have closed by picking a
number now instead of three times deferring it.

## What is solid (verified against the repository)

- **The core dependency graph is internally consistent.** `#279` has
  `blocked_by: []`, `#280` has `blocked_by: [279]`, both mirror `part_of_feature:
  171`, and the walk in #171's body matches — no cycle, verified by reading
  both children directly.
- **The "corrections" in §2 are honest, not self-serving.** The body
  explicitly retracts its own prior false claims (the stale `grep` transcript,
  the stale undo line citation) rather than quietly dropping them — a real
  and unusual discipline for this template.
- **The substrate-vs-plan boundary is accurately described.** `ls
  src/jls/collab/crdt/` at HEAD shows exactly `CausalBuffer.java,
  OpEnvelope.java, OpId.java, VectorClock.java, package-info.java` — no
  merge type, matching the issue's claim that only delivery, not confluence,
  has landed. `CausalBuffer.java`'s own javadoc (quoted correctly in the
  issue) says convergence is "the job of the per-kind CRDT merge rules
  layered above this buffer, not of delivery."
- **The anti-Raft "no quorum, lone peer keeps editing" requirement is a
  clean, falsifiable design constraint** that #280's falsification criteria
  correctly treat as a stop condition ("If convergence requires blocking the
  partitioned editor, the design violates the anti-Raft requirement").
- **The #352 duplicate-merge resolution is real and traceable**: #352 is
  confirmed `state: closed, state_reason: duplicate` via the GitHub API, and
  its "Related issues" section names #171 and #170 as the two survivors —
  consistent with #171's absorption comment.

## Net assessment

The issue is well-constructed relative to typical tracking issues — concrete
file/line evidence, an honest correction log, and a real falsifiable
convergence oracle. It is marked **sound-with-concerns** rather than
**needs-rework** because none of the findings above are internal
contradictions that block starting #279/#280 (both are independently
well-specified and already in progress); they are staleness, missing
cost accounting, and one unverified technical assumption underlying the
oracle every downstream child inherits. Finding 1 (stale body vs. latest
comment) and Finding 3 (font-metric geometry parity under headless merge)
are the two worth resolving before more children are filed against this
issue's criteria.
