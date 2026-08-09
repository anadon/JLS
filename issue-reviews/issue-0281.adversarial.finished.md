# Issue #281: Shared session v1: snapshot broadcast over the Transport seam — markChanged capture, epoch-tagged last-wins restore via the load path (collab Stage 1b slice)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The issue is unusually well-grounded: every cited file, method, and line
number was checked against HEAD (53116252) and matches (`SimpleEditor.java`
markChanged at L5497, `Circuit.stateHash()` at L1548, `Roster.highestEpoch`/
`entries`/`tokenHolder`, `CircuitSnapshot.capture`/`restore`, the §2
Observation-3 absence greps for `CircuitSnapshot` and `collab.session` in
`src/jls/collab`/`src/jls/edit` both return nothing, exactly as claimed).
The dependency graph to #169/#171/#257/#166/#39 is real and consistent with
those issues' own bodies. That grounding is real work and should be
credited. The concerns below are about specific technical assumptions the
plan bakes in without stating them, several of which are checkable against
the code the issue itself cites and don't hold up.

## Findings, most severe first

### 1. The chosen hook point (`SimpleEditor.markChanged`) has a documented sibling method the issue never mentions, and picking the wrong one causes a rebroadcast storm

Observation 1 anchors the broadcast hook at `SimpleEditor.markChanged()`
(`src/jls/edit/SimpleEditor.java:5497`, the "single post-mutation choke
point"). But there are *two* `markChanged`s in this call graph, and the
existing undo/redo restore path (`finishDo`, the exact precedent Observation
5 cites for "cancel-then-apply") deliberately calls the *narrow* one:

- `SimpleEditor.markChanged()` (L5497-5536) — the broad one: pushes an undo
  snapshot, clears redos, may write a checkpoint, and (per this issue) would
  now also broadcast.
- `Circuit.markChanged()` (`src/jls/Circuit.java:293-303`) — just sets the
  dirty flag, invalidates the spatial index, and stops the simulator.
- `finishDo` (`SimpleEditor.java:5728-5754`), used by both undo and redo,
  swaps in the restored circuit and at L5754 calls `circuit.markChanged()`
  — the **narrow** one — precisely so that restoring from a snapshot does
  not re-push an undo entry, does not clear redos, and does not re-trigger
  the broad bookkeeping for a change that didn't originate at the keyboard.

The issue's §2 Observations cite `finishDo`'s neighboring `cancelGesture()`
call (L5641-5645) as the precedent to reuse for remote restores, but never
mention the L5754 `circuit.markChanged()` vs. `SimpleEditor.markChanged()`
distinction one paragraph later in the same method — even though it is the
more load-bearing precedent for this exact task. §7.10's data-transformation
pipeline ("… → `cancelGesture()` then swap into the editor") is silent on
which `markChanged` the swap should call.

If a follower's remote-restore path is wired through the broad
`SimpleEditor.markChanged()` — the only choke point the issue names — the
consequences are concrete and testable: every applied remote snapshot pushes
a spurious undo entry (letting a follower "undo" the writer's edit), writes
an unnecessary checkpoint, and — because the broadcast hook lives at that
same choke point — **re-broadcasts the snapshot the follower just received**.
Under `ChaosTransport`'s duplicate/reorder fates this could cascade. None of
§5's predictions (P1-P4) or §10's falsification criteria would catch this:
P1 only asserts eventual `stateHash()` equality, which an echo storm doesn't
violate, it just wastes bandwidth and corrupts follower undo history
silently.

**Recommendation:** add an explicit interface-contract line: "remote apply
uses `Circuit.markChanged()`, never `SimpleEditor.markChanged()`, mirroring
`finishDo`'s L5754 precedent" — and add a prediction/test that a follower's
applied-snapshot count does not trigger a second outbound frame.

### 2. "Epoch-tagged last-wins" leans on `Roster.highestEpoch()`, which counts the wrong events

§2 Observation 4 lists `Roster.highestEpoch()` under "the epoch machinery"
available for this slice. But `Roster`'s epoch (`src/jls/collab/session/
Roster.java:290-298`, `nextEpoch()` at L328-335) numbers **membership/floor-
control `SessionEntry` proposals** (join/leave/token-grant/token-claim) —
it advances only on roster changes, not on circuit edits. A single token
holder can fire `markChanged()` (and thus a snapshot broadcast) dozens of
times between any two roster events, and if those frames are tagged with
`Roster.highestEpoch()` they all carry the *same* epoch number. "Last-epoch-
wins" then can't order them — P2 ("a stale-epoch snapshot delivered late
never overwrites a newer applied one") has nothing to discriminate on within
a single roster epoch, which is the common case, not the exception.

This also makes P1/P2 gameable in the assignment's specific sense: because
`ChaosTransport` reorders via a *bounded holdback* and never permanently
drops the last frame (`test/jls/collab/net/ChaosTransport.java:12-22`:
"never a wall-clock sleep," bounded release), an implementation that ties
snapshot ordering to the (frequently-constant) roster epoch will still pass
a `stateHash()`-equality-after-quiescence test, because eventual delivery of
every frame plus "last one applied wins by arrival order" converges anyway
— the epoch tag is doing no real work, and the test can't tell the
difference between "epoch-tagged last-wins" and "whatever arrived last,
in whatever order." The falsification criterion in §10 ("if followers
diverge... audit epoch comparison") won't fire because nothing in the
described rig produces divergence; it just produces an epoch field that
never does its stated job.

**Recommendation:** specify explicitly that the frame needs its own
strictly-monotonic counter scoped to the current writer (mirroring
`OpEnvelope`'s origin+seq, not `Roster`'s membership epoch), and add a
directed test that sends two same-roster-epoch snapshots out of order and
asserts the earlier one is dropped after the later one is applied.

### 3. The layering the issue assumes for the new service is not the layering the codebase enforces

§7.4 places the new service in `jls.collab.session` and §7.10 has it call
`CircuitSnapshot.capture`/`.restore` directly (`Circuit → CircuitSnapshot
.capture ... → CircuitSnapshot.restore ... → swap into the editor`).
`CircuitSnapshot` is `package jls.edit` (`src/jls/edit/CircuitSnapshot.java:1`).
`test/jls/ArchitectureRulesTest.java:225-238`
(`replicationStackDependsDownwardOnly`) forbids any class in
`jls.collab.session..` from depending on `jls.edit..` — not just Swing
types, the whole package, and it is not `allowEmptyShould`-exempted once
code exists there (the comment says the rule exists precisely so #169/#171
are "born clean"). §7.4's own carve-out ("no Swing types in the service
itself — the EDT marshalling is injected by the app wiring") shows the
author is aware of *a* boundary concern here, but only names Swing;
`CircuitSnapshot` isn't a Swing type, so that carve-out doesn't obviously
cover it, and the issue never says capture/restore must *also* be injected
callbacks rather than direct calls from the session-package service. An
implementer following §7.4/§7.10 literally writes code that fails `mvn
verify`'s ArchUnit gate (which is in the DoD anyway) — but only discovers
the layering mistake after building the wrong shape, not from reading the
contract.

**Recommendation:** state explicitly, next to the Swing carve-out, that
`CircuitSnapshot.capture`/`.restore` calls also live outside
`jls.collab.session` and reach the service only via injected
`Supplier<byte[]>`/`Consumer<byte[]>`-shaped seams, matching the existing
Swing-marshalling carve-out's pattern.

### 4. Single-writer is assumed by the correctness story but not enforced or tested here, and the gap is explicitly punted to a sibling issue

H1/P1 implicitly assume exactly one peer is producing snapshots at a time
(that's what makes "last-epoch-wins" meaningful at all). But §12 states
"token gating (P3 read-only affordance — a sibling slice, not here)" is out
of scope, and neither §7.4 nor §8's Method checklist has a step that gates
the broadcast (or local editing) on `Roster.tokenHolder()` — Observation 4
lists `tokenHolder()` as available machinery but nothing in the plan
requires using it. Nothing in this repository's current editor code stops
a non-holder from typing at their own keyboard and calling `markChanged()`
locally. Since read-only enforcement is deferred to a different issue, this
slice's own snapshot-broadcast hook — wired at `markChanged()`, which fires
for *any* local edit on *any* peer, not just the designated writer — has no
stated behavior for the case its own success criteria assume can't happen.
The N-replica schedule apparatus described in §6/§9 ("seeded schedule runner
in the style of `RosterConvergenceTest`") drives edits from a single
scripted writer, so this gap is structurally invisible to the acceptance
tests as specified — a second source of gameable-but-passing criteria.

**Recommendation:** either make broadcast conditional on
`self.equals(roster.tokenHolder())` as part of this slice's contract (cheap,
self-contained, doesn't require #169's UI-level read-only affordance), or
add an explicit falsification/threat-model line owning the gap instead of
silently relying on a sibling issue that isn't `blocked_by` here.

### 5. Minor: every follower-side apply stops that follower's running simulator

`Circuit.markChanged()` (the narrow method Finding 1 recommends using) calls
`JLSInfo.sim.stop()` unconditionally (`Circuit.java:300-301`). If a follower
is running a simulation while the writer is actively editing, each coalesced
snapshot arrival halts it — not called out anywhere in §7.11 (Failure modes)
or the audience-impact section, though it's a direct, checkable consequence
of the exact restore path the issue specifies.

## What's solid

- Every permalink/line citation checked resolves and matches HEAD; the
  "absence" greps in Observation 3 are accurate and reproducible.
- Dependencies (`Roster`, `ReachabilityTracker`, `Transport`,
  `LoopbackTransport`, `ChaosTransport`, `CircuitSnapshot`,
  `Circuit.stateHash()`, `OpEnvelope`) are real, landed, and used as
  described; `blocked_by: []` is accurate.
- The cancel-then-apply (#39) precedent citation (L5641-5645) is accurate
  and a genuinely good pattern to reuse for P4.
- Scope boundaries against #168 (sockets), #169 (token gating/presence/
  panel), #170 (hostile-payload policy), #171 (CRDT replacement) are
  clearly drawn and consistent with those issues' own stated scopes.
- Falsification criteria (§10) are real falsification criteria, not
  vacuous checkboxes — they just don't cover Findings 1 and 2 above.

## Verdict rationale

Not `needs-rework`: the plan's shape (capture on markChanged, epoch-tagged
frames, last-epoch-wins, load-path restore) is a reasonable Stage 1b design
and the scaffolding it depends on is real and correctly cited. But two of
the findings above (1 and 2) are not stylistic nits — they describe specific
ways an implementation that satisfies the letter of §7/§8 as written
produces either a rebroadcast/undo-corruption bug or an ordering scheme that
doesn't actually order anything, in both cases without the stated acceptance
tests (P1-P4) catching it. Those need to be resolved in the issue text (or
in a design comment) before implementation starts, which is why this is
`sound-with-concerns` rather than `sound`.
