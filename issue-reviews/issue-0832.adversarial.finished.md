# Issue #832: TASK-C333-2: cross-partition boundary events are framed, sent and drained over the collaboration transport that already exists, so no second networking stack and no second security surface is created
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The mechanism — reuse `jls.collab.net.Transport` rather than build a second
networking stack — is sound and the two code citations that anchor it
(`Transport.java:38`, `ArchitectureRulesTest.java:249-262`) check out exactly
against the tree. But AC-1's central term, "net name," names a concept that
does not exist anywhere in the current data model, and the one issue this
task is ordered after (#332) — the issue that would eventually *define* a
cross-cut net identity — has not filed a single line of code or even a
child issue for that artifact. AC-3's grounding citation ("the same
discipline the host byte port uses") also points at a feature that is not
built. Both gaps let an implementer satisfy the letter of the ACs with
something disconnected from the design this task is actually supposed to
feed into.

## Findings, most severe first

### 1. [High] AC-1's "net name" has no referent in the codebase, and the one issue that would create one hasn't

AC-1: "a boundary event survives a round trip through `LoopbackTransport`
and reconstructs equal in time, **net name** and value." I read
`src/jls/elem/WireNet.java` in full: it has `ends`, `wires`, `bits`,
`hasinput`, `triState` — no `name` field, no `getName()`, nothing net-scoped
that could serve as "net name." The only string identifiers wire-adjacent
code carries are per-wire *probe* names (`WireEnd`/`Wire`, optional,
user-set) and per-element `ElementId` stable ids (`NetBlocks.java:52-61`) —
neither is "the net's name." Net identity in this codebase is structural
(the set of connected `WireEnd`s), not nominal.

The task's own `ordering_after` names #332 ("a design must exist as parts
with named boundaries before there is anything to exchange") as the source
of that naming concept. I read #332 in full: its `planned_tasks` list five
scopes including "BOUNDARY NET IDENTITY ACROSS A CUT," every one marked
"not filed, no id," and its own evidence line states `git grep -rliE
'PartitionSet|BoundaryDescription|streamingElaborat' 2d0ca9d -- src/ test/`
returns zero files. #332's body says outright: "**There is no issue for the
partitioned model or streaming elaboration.**" So the thing that is
supposed to make "net name" a real, cross-side-agreeing identifier does not
exist as code, as a filed child issue, or even as a settled format —#332
itself only has a *mathematical* identity constraint ($\mathrm{name}_i(n) =
\mathrm{name}_j(n)$), not a chosen representation.

**Consequence:** AC-1 as written is satisfiable by inventing any
placeholder string field on the frame — the local `WireNet`'s connected
`Output`'s put name, a locally-generated UUID, anything — and asserting
round-trip equality on it, without that field having any actual relationship
to what #332 will eventually define as a stable cross-partition net
identity. The test can go green while the real goal (a name that means the
same thing on both sides of a cut, once cuts exist) is not served at all,
because nothing yet constrains what "net name" must be.

**Recommendation:** either descope the "net name" clause from this task
until #332 lands an actual boundary-net-identity artifact, or state
explicitly and narrowly what stand-in identifier this task uses (e.g. "the
watched element's declared name, subject to revision once #332's boundary
description exists") so the AC is falsifiable against something real rather
than against a self-chosen placeholder.

### 2. [Medium] AC-3's cited precedent ("the same discipline the host byte port uses") does not exist

I searched the full tree (`src/`, `test/`, `docs/`) for `byte port`,
`BytePort`, `host byte`: zero matches in production code. The host byte port
is FEAT-032 (#324/#424), and this fleet's own prior review of #426 already
established the identical fact independently ("A repo-wide search for `host
byte port` / `HostBytePort` / `hostBytePort` across `src/`, `test/`, and
`docs/` returns no matches"). Nor does an offer/drain cross-thread-to-simulator
pattern exist to copy in its absence: `src/jls/sim/Simulator.java` has no
`Queue`, `BlockingQueue`, or any cross-thread posting primitive at all — its
only cross-thread state is the single `volatile boolean stopping`
(`Simulator.java:44`). `InteractiveSimulator` runs the whole event loop on
one dedicated thread; nothing external currently posts work into a running
`Simulator`.

**Consequence:** "the same discipline" promises reuse of an established
pattern. There is no established pattern — this task has to invent the
first offer/drain path into `Simulator` from scratch, including whatever
locking or queuing keeps the drain from racing the event loop. That is
materially more design work than "mirror an existing discipline" implies,
and AC-3's test ("a test asserts no foreign thread posts into a simulator")
has no existing sibling test to model itself on either.

**Recommendation:** drop the host-byte-port comparison (it cites unbuilt
work) and instead state the offer/drain contract this task must invent
directly — queue type, bounding/backpressure behavior, and where in
`Simulator`'s event loop the drain happens — as its own design content, not
as an inherited discipline.

### 3. [Low-Medium] AC-2 is a no-op acceptance criterion for the work this task actually does

AC-2 requires `socketEndpointsAreConfinedToCollabNet` to stay green. This
task's own description says it operates entirely through the existing
`Transport`/`LoopbackTransport`/`ChaosTransport` seam and adds no socket
code. Since nothing this task does touches `java.net.Socket` or its
relatives, this test passes automatically regardless of how well or badly
the boundary-event work is done — it is a regression ratchet inherited from
#170, not a criterion that discriminates a correct implementation from an
incorrect one here. Keep it as a ratchet by all means, but it should not be
read (and the issue's framing — "adding a second consumer... deserves a
review independent of whether the synchronisation protocol is correct" —
invites exactly this reading) as evidence that this task's socket-confinement
implications were specifically reviewed. There is no new socket-confinement
risk in this task to review.

**Recommendation:** either drop AC-2 from this task's own criteria (it's
already enforced globally and unconditionally) or reframe it honestly as
"no new AC-2 risk is introduced" rather than as a criterion this task must
satisfy through its own design effort.

### 4. [Low] "Second consumer" undercounts what's already partly built, and the cited architecture-test comment is stale

The issue frames this task as adding "a second consumer" to `jls.collab.net`.
But `ArchitectureRulesTest.java:220-223` — inside the very rule this issue
cites — comments that `jls.collab.session` and `jls.collab.ui` "do not exist
yet (issues #169/#171 create them)." That is no longer true: `Roster.java`,
`PeerId.java`, `SessionEntry.java`, `ReachabilityTracker.java` already exist
under `src/jls/collab/session/`, alongside a populated `jls/collab/crdt/`
and `jls/collab/op/`. None of that code currently calls `Transport.send`/
`receive` in production (only `LoopbackTransport` and `SocketSession`
implement the interface; nothing wires `OpEnvelope` through it yet), so "no
consumer runs in production today" is accurate, but "the first consumer" is
further along than the stale comment this issue leans on suggests. This
doesn't block the task, but a reviewer relying on that comment for context
would misjudge how much of the "first consumer" is already staged.

**Recommendation:** no action required on #832 itself; flag the stale
comment at `ArchitectureRulesTest.java:220-223` for a documentation pass
independent of this task.

### 5. [Low] No frame-kind discrimination is specified, and it's left implicit whether one is needed

If a boundary-event frame and a future op-vocabulary frame (once #169/#171's
session/CRDT wiring lands) ever travel the same `Transport` connection,
nothing in the issue or in `Transport`'s contract (opaque, untyped payload
bytes, `Transport.java:38-53`) says how a receiver tells them apart. This is
plausibly moot if distributed-simulation partitions and collaborative-editing
sessions always get dedicated connections — but the issue doesn't say so, and
"a boundary event... becomes a frame kind on the shipped Transport seam"
reads as if frame-kind tagging is already a solved concept when it isn't.

**Recommendation:** state explicitly, in the issue or in TASK-C333-3, whether
boundary-event traffic ever shares a connection with any other frame kind;
if not, say so as a recorded scope boundary so a future reader doesn't have
to re-derive it.

## What's solid (one line each)

- The core citations are accurate: `Transport.java:38` is exactly the
  interface doc-comment cited, and `ArchitectureRulesTest.java:249-262` is
  exactly `socketEndpointsAreConfinedToCollabNet` as quoted.
- Reusing `Transport`/`LoopbackTransport`/`ChaosTransport` instead of a new
  networking stack is the right call and both concrete transports already
  exist and already implement the interface correctly (read in full).
- AC-4's payload-cap/hostile-input framing is grounded in real, already-shipped
  code: `SecureLink.MAX_PAYLOAD_BYTES`, `FrameRejected`, and
  `LoopbackTransport`'s cap check (`LoopbackTransport.java:129-133`) all
  exist and behave as described.
- AC-5's scope boundary against TASK-C333-3 (#834, the barrier/advance rule)
  is clean and matches the decomposition recorded in the #333 parent review.
- `ChaosTransport` (`test/jls/collab/net/ChaosTransport.java`) is a real,
  already-built fault-injection double with deterministic seeded fates, so
  "sendable... over the ChaosTransport double" is not aspirational — it
  works today for arbitrary opaque payloads.

## Verdict rationale

The transport-reuse mechanism is correct and cheap to build on top of what
already exists — none of the findings above argue against the approach.
`needs-rework` because two of the five acceptance criteria lean on things
that are not there: AC-1's "net name" borrows a concept from #332 that #332
itself has not yet created (not even as a filed child issue), and AC-3's
"same discipline" borrows a pattern from a feature (host byte port) that has
zero code in the tree. Both should be rewritten to state their own contract
directly rather than by reference to unbuilt precedent, so an implementer
can't satisfy the AC's letter with a placeholder that has no relationship to
where this task is actually headed.
