# Issue #169: Shared session v1: membership lifecycle, snapshot sync, floor control, presence, peer panel (collab Stage 1b)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of the machine block, #169 asks for one thing: **two students at one bench
can draw one circuit together, and it is unambiguous who is drawing.** Everything
else — epochs, anti-entropy, partition banners, three-machine LAN runs — is apparatus
for that sentence. The issue is competently planned as a distributed system. My
concern is that it is planned as a *stage* of a distributed system whose final form
was never actually decided, and that the staging makes #169 pay for a successor it
may never need.

## The trajectory it has to fit

`ARCHITECTURE.md` describes a codebase governed by *recorded decisions that refuse
speculative build*: i18n is a non-goal until an instructor asks; the plugin loader
was deleted rather than kept; out-of-process module isolation is "reserved for a
future untrusted-provider case… not built speculatively"; a second simulation
strategy is "premature optimization until CPU-scale designs are actually common,"
with a named revisit trigger.

Collaboration is the one large subsystem in this tree with **no such recorded
decision at all.** Neither `README.md` nor `ARCHITECTURE.md` mentions it. And it is
already the largest speculative build in the repository: `src/jls/collab/**` is
**7,868 lines**, and

```
$ grep -rln "Roster\|ReachabilityTracker" src/ test/ | grep -v collab/session
(no output)
```

Zero consumers. `jls.collab.crdt` (789 lines) is referenced only by
`ArchitectureRulesTest` and `NullMarkedRatchetTest` — the rules that police it. #169
is right that this must gain a consumer, and the comments' "wiring, not algorithm"
re-scoping is the best thing in the thread. What #169 never asks is whether the
*shape* of that consumer was ever settled.

## Claim 1 — the token is probably the destination, not a stepping stone

`docs/collaborative-editing-research.md` §7 stages this as "Stage 1 token → Stage 2
CRDT replaces the token." #169 inherits that and pays for it in §3, §4 and its DoD:
"roster/presence/panel APIs delivered in the form #171 consumes unchanged."

But read §4's actual argument. The reason CRDT beat Raft is **availability**: n=2,
majority=2, so one sleeping laptop freezes *both* editors. That argument does not
transfer to floor control. A token never freezes the holder, and `TOKEN_CLAIM` —
already landed, already epoch-fenced — lets the survivor take the pen after a
timeout. The token's worst case is "wait ~6 s to take the pen," not "nobody edits."
So the strongest recorded case for Stage 2 is an argument against *Raft*, and #171
has never been justified on its own terms. §8 open question 3 ("strong consistency
vs availability, **final call**") is still open in the tree.

Meanwhile §3 concedes that sequence CRDTs mostly don't apply here, and recommends
add-wins sets plus per-attribute LWW registers. For a wiring graph that means
last-writer-wins on structure: two students routing into the same `Put`, or one
deleting a subcircuit the other is wiring into, converge by **silently discarding
one student's work.** In a teaching tool that is a worse outcome than waiting for the
pen — "my gate vanished and nothing said so" is precisely the failure a pedagogy
tool must not have. Driver/navigator is also the pairing model the audience is
already taught. A talking stick *models* it.

**Reframing:** declare single-writer floor control the terminal design for
collaboration in JLS, record it in `ARCHITECTURE.md`'s Recorded Decisions with the
same shape as #221/#222 — rationale, and a named revisit trigger ("a course reports
the pen is the blocker") — and move #171 behind that demand gate. The immediate
consequence for this issue is that **the "#171 consumes these APIs unchanged"
constraint disappears from §3, §4 and the DoD.** That is a tax paid to a consumer
that does not exist, on a surface that has never met a user.

## Claim 2 — one data path, not two thrown-away ones

#169's steady-state sync is whole-circuit snapshot, epoch-tagged, last-wins (#281).
#171's is ops. Both are built; the first is deleted. That is the seam I would cut
differently.

Under a token, writes are **already totally ordered** — which is exactly the
condition under which an op log needs no CRDT at all. And the log substrate already
exists: `OpSink.submit`/`submitAll` (`src/jls/collab/op/OpSink.java`) is the single
mutation choke point and its javadoc already names the #163 layer as co-observer; 20
op types are implemented; `Circuit.stateHash()` (`src/jls/Circuit.java:1548`) hashes
the canonical save text and is a cheap per-op agreement check.

Concretely: the session frame becomes `(epoch, seq, opBytes)`; followers apply in
`seq` order; a `seq` gap or a `stateHash` mismatch triggers a **snapshot request**.
Snapshot stops being the steady state and becomes the *bootstrap and repair* path.
That is better on four counts:

- The #39 cancel-then-apply hazard — a restore swapping the object graph under a
  live drag — moves out of the common case into a rare, deliberately-entered repair.
- Rejoin becomes "snapshot once, then follow the log," which is what I4 wants anyway.
- The byte seam is unchanged: op bytes are still `Σ*`, so `transportKnowsNothingOfCircuits`
  stays green by the same argument.
- It is on the path in *both* futures. If the token is ever relaxed, an ordered op
  log is the substrate a CRDT needs; snapshot-last-wins is discarded either way.

The honest objection is that #167's migration is partial — only the delete and move
gestures route through `OpSink` today — which is very likely why snapshot-first was
chosen. Fine: keep #281, but **retarget it from "the sync mechanism" to "join
bootstrap and divergence repair,"** which is needed regardless and which the P9
torn-snapshot assertion tests perfectly. Then make the steady-state op relay depend
on #167 completing. That gives #167's gesture-by-gesture grind the visible payoff it
currently lacks, instead of a parallel data path that competes with it.

## Claim 3 — the surface is a status strip, not a dashboard

The planned panel carries members, verified badge, reachability, last-seen,
attribution colors, stateHash sync indicator, token controls, partition banner and a
close-time divergence notice. That is an operator console for a distributed system.
The users are two people sitting next to each other who can *ask out loud* whether
the other laptop is awake. The comments price this residual at 8–14 maintainer-weeks
and call it the largest unfunded item under the feature — that number is a symptom of
designing for the wrong reader.

Minimum operable surface: **who has the pen**, one button (Take / Pass), N colored
cursors, and one state word (connected / syncing / diverged). Everything else routes
through the pattern this codebase already has and enforces: `TellUser` as the single
message channel (`NotificationRatchetTest`) plus a `LoadError`-shaped typed taxonomy
— `SessionError{UNREACHABLE, DIVERGED, KEY_CHANGED, FRAME_REJECTED, NOT_ROUTABLE}`
with location, detail and an actionable hint. That *is* #433's carried requirement
that a peer who cannot connect be "reported by name," expressed in the project's own
idiom rather than as a banner. It also gives the failure paths headless tests, which
a banner does not have.

Worth correcting one carried threat: `test/jls/ui/` now holds ~30 files including
`EditorGestureTest`, `RenderAssert`, `EdtViolationDetector`. The #91 harness is no
longer Layer-1-only, so the read-only affordance is testable today — the UI risk
here is cost and audience fit, not untestability.

## The radically simpler route worth considering first

**Follow mode.** One broadcaster, N read-only viewers. No token transfer, no reclaim
timer, no ejection, no partition banner, no presence rate limiter — the writer is
whoever opened the session, and that never changes. It is a strict subset of #169
(nothing built is wasted), it is perhaps 15% of the remaining work, it serves the
instructor-demo capstones directly, and it is the piece most likely to be used by a
real class *this term*. Ship it, put it in front of one course, and let the observed
demand decide whether pen-passing, ejection and three-machine sessions get built —
the same demand-gate discipline #212 already lives under. Today the stated gap ("pair
work means passing files around") is asserted, not measured; the i18n decision in
`ARCHITECTURE.md` demands a named instructor before spending, and collaboration has
been held to a lower bar than string externalization.

## Acceptance criteria I am explicitly disregarding

1. **"Roster/presence/panel APIs delivered in the form #171 consumes unchanged."**
   Disregarded per Claim 1: it constrains a first surface for a speculative consumer.
2. **The peer-panel dashboard criteria** (verified badge, last-seen, partition
   banner, close-time divergence notice). Disregarded per Claim 3; replace with the
   status strip plus a typed session-error taxonomy.
3. **I5 as the close-out gate.** Keep the three-machine run as evidence, but the gate
   for a feature whose stated beneficiary is a lab pair should be *one real pair
   using it for one lab*, not a systems demonstration on three laptops.

## What I would keep untouched

The byte seam (`restore ∘ Session ∘ capture`, `CircuitSnapshot` staying in `jls.edit`);
the four `ArchitectureRulesTest` methods and the socket ratchet green *unmodified*;
`stateHash()` as the convergence oracle; unreachable ≠ removed; no second trust
boundary beyond #170; the headless-session-service-first answer to open question 1;
delay distributions in the chaos toggles; and the **first-consumer contract audit** —
the single best deliverable in the whole thread, and the one that should be produced
*before* any UI slice is filed.
