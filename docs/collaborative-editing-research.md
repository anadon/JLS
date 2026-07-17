# Distributed Collaborative Circuit Editing for JLS — Research Report

*July 2026. Prepared for the JLS maintainer as an investigation of ways to
implement distributed, simultaneous multi-user editing of circuits. This is
a single-pass investigation: codebase claims below carry `file`/method
anchors and were read directly from HEAD; external-tool and library claims
are marked **[verified]** (checked against the live source this month) or
**[from memory]** (well-known but not re-verified — confirm before relying
on them for a design commitment).*

## Executive summary

1. **JLS already owns the hardest primitive**: a deterministic,
   round-trip-tested text serialization of the whole circuit, wired into
   the editor as the undo mechanism (`CircuitSnapshot`,
   `src/jls/edit/CircuitSnapshot.java`). State-based synchronization —
   "ship the whole circuit after every gesture" — is nearly free to build
   and is the right *first* stage, not a throwaway.
2. **The two real gaps are identity and operations, not networking.**
   Elements have no stable identity (`Circuit.save` reassigns sequential
   ids on every save, `src/jls/Circuit.java:1098`), and there is no
   command/operation layer — every mutation is inlined in the
   `SimpleEditor` mouse state machine (~4.5k lines). Any
   finer-than-whole-circuit sync protocol needs both. These
   prerequisites are independently valuable (undo, checkpointing,
   scripting) and should be built first regardless of which
   collaboration architecture wins.
3. **A central sequencer beats OT and CRDTs for this domain.** Circuit
   editing is low-frequency, coarse-grained, and object-based — closer
   to Figma's design-object model than to character-stream text editing.
   Figma's published architecture (server-ordered operations,
   per-property last-writer-wins over objects with stable ids, no
   general OT/CRDT machinery) is the proven shape for exactly this kind
   of document. **[from memory]** Operational transformation is
   overkill and notoriously hard to get right; full CRDTs
   (automerge-java is real, maintained, and JVM-usable **[verified]**)
   buy offline merge that a classroom pedagogy tool does not need in
   its first iterations, at the cost of owning the document
   representation.
4. **Deployment reality constrains the transport.** JLS ships as one
   self-contained jar with a single runtime dependency (XZ, `pom.xml`),
   used by students on lab machines and university networks. The
   realistic topologies are (a) LAN sessions where one JLS instance
   hosts, and (b) a small optional relay for NAT-crossing sessions.
   Nothing should require standing infrastructure to use JLS normally.
5. **Recommended staged path** (§6): Stage 0 — stable element ids and an
   operation layer; Stage 1 — hosted shared session with snapshot sync
   and floor control ("one hand on the mouse", everyone sees live
   state); Stage 2 — sequencer-ordered operations with optimistic local
   apply and element-level conflict rules (true simultaneous editing);
   Stage 3 (only if demand appears) — CRDT document for offline/async
   merge. Each stage ships usable value and none forecloses the next.

---

## 1. What "collaborative editing" would mean here

Three distinct capabilities hide under the phrase; they have different
costs and different pedagogical value:

- **Shared live session (synchronous).** An instructor and students, or a
  lab pair, see and edit one circuit at the same time. This is the thing
  people usually mean, and the thing with clear classroom value: JLS is
  "a single-maintainer pedagogy tool serving an English-language course
  ecosystem" (ARCHITECTURE.md, i18n decision), and its users work in
  pairs and lab sections.
- **Presence and awareness.** Seeing collaborators' cursors, selections,
  and in-flight gestures. Cheap to bolt onto any of the architectures
  below (it is transient state, never merged), and disproportionately
  important for the experience.
- **Asynchronous merge (offline divergence).** Two people edit copies of
  a `.jls` file independently and merge later. This is a *version
  control* problem more than a collaboration problem; the existing
  plain-text save mode (`-savetext`, issue #129) already gives diffable
  files, and a semantic merge tool could exist with or without any
  real-time layer. Treated here as out of scope for the first stages
  (§6, Stage 3).

Non-goal worth recording: collaborative *simulation* control. The
simulator runs on each replica locally (§5.3); synchronizing interactive
simulation sessions (shared clock stepping, shared probe windows) is a
separate project and should not be coupled to editing.

## 2. What the codebase gives us, and what it lacks

### Assets

- **Deterministic full-state serialization, already on the mutation
  path.** Every mutating gesture already calls
  `SimpleEditor.markChanged()` (`src/jls/edit/SimpleEditor.java:4192`),
  which captures a `CircuitSnapshot` — the circuit serialized in the
  save format and deflated. Two snapshots of identical circuits are
  byte-identical (used to drop no-op undo entries). This is exactly the
  hook and exactly the payload a state-sync protocol needs: a
  collaboration layer can observe every completed gesture at one choke
  point and obtain a compact, canonical state blob for free. Snapshots
  restore through the ordinary load path (`CircuitSnapshot.restore` →
  `Circuit.load`/`finishLoad`), so "apply remote state" is also already
  implemented and pinned by the round-trip test suite.
- **A hardened hostile-input loader.** Applying bytes received from the
  network is exactly loading an untrusted file, and that path is already
  hardened with caps and a structured error taxonomy (issue #38,
  `UntrustedFileHardeningTest`; `LoadError`). A collaboration layer
  should funnel *all* remote input through this pipeline and inherit its
  guarantees.
- **A threading discipline with the right precedents.** The EDT owns all
  Swing; the interactive simulator already runs on a dedicated thread
  with `volatile` flags and `SwingUtilities.invokeLater` bridging, and a
  background daemon thread already handles checkpoint I/O so the EDT
  never blocks (ARCHITECTURE.md, threading model). A network I/O thread
  posting received updates to the EDT via `invokeLater` follows the
  established pattern; no new concurrency model is needed.
- **Format versioning that can carry new data.** The `FORMAT` header
  (issue #79) plus the typed `setValue`/`Attribute` parameter protocol
  means new persisted fields (stable ids, §5.1) have a defined,
  test-pinned way to enter the file format, with old files loading
  unchanged.

### Gaps

- **No stable element identity.** `Element.id` is documented as "unique
  for every element when written to file" (`src/jls/elem/Element.java:24`)
  and is *reassigned sequentially on every save* (`Circuit.save`,
  `src/jls/Circuit.java:1098-1102`). Undo restores build an entirely new
  object graph. So today there is no way to say "element 12 moved" and
  have that name mean the same thing on two replicas — the fundamental
  requirement of every architecture except whole-state sync.
- **No operation layer.** Mutations (place element, move selection,
  draw wire, edit attribute, delete, paste, import) are performed
  inline by the `SimpleEditor` state machine across ~4.5k lines; the
  only reified "operation" is the full-state snapshot. Operation-based
  sync (§3.C, §3.D) requires refactoring these into discrete,
  serializable command objects applied through a single entry point.
  This is the single largest work item in this whole area — and it is
  also what issue #78 (element registry) and any future
  scripting/macro facility want.
- **No network code and a threat model that excludes it.** There is
  currently "no network-facing code beyond loading local files"
  (MAINTENANCE_AUDIT.md). Adding any listener changes SECURITY.md's
  threat model and must be opt-in, off by default, and never touched by
  batch mode.
- **Whole-snapshot undo is wrong under collaboration.** Undo restores
  the entire circuit (`SimpleEditor.undo`), which under collaboration
  would revert *other people's* concurrent work. Collaborative undo
  must become "invert my own recent operation" — which again requires
  the operation layer (§5.2).

## 3. The design space

Four architectures, in increasing order of concurrency fidelity and
cost. The classic lineage: turn-taking → state sync → server-ordered
operations (Jupiter/Google-Docs style OT, Figma's object model) →
CRDTs (Automerge, Yjs). **[from memory]**

### A. Floor control (shared view, one writer at a time)

One instance hosts; all replicas render the same circuit; an explicit
"editing token" designates the single writer, passed on request. Every
`markChanged()` on the writer broadcasts a snapshot; everyone else
applies it read-only.

- *Effort:* smallest. No identity, no operations, no merges — only
  transport, session join, and a read-only editor mode.
- *Value:* already covers the strongest classroom use case
  (instructor demonstrates; student drives while partner watches;
  "let me show you" over a session code).
- *Limit:* not simultaneous. But every later stage needs everything
  built here (session, transport, snapshot broadcast, catch-up), so
  nothing is thrown away.

### B. State-based sync (simultaneous, coarse merges)

Everyone edits; each completed gesture broadcasts a full snapshot; a
central host serializes them (first-received wins) and rebroadcasts.
Concurrent gestures conflict at whole-circuit granularity: the loser's
gesture is silently replaced by the winner's state.

- *Effort:* small increment over A — remove the token, add "your edit
  was overridden" feedback.
- *Value:* genuine simultaneity when people work in different areas
  *and* gestures are short (they mostly are: place, drag, wire).
- *Limit:* lost updates under real concurrency; unacceptable as the
  end state but fine as an interim behind a "shared session (beta)"
  label. Snapshot size is not the problem — deflated save text of
  student circuits is small (undo already keeps a whole stack of
  them, `JLSInfo.undoStackDepth`).

### C. Central sequencer over operations (recommended target)

The Figma-shaped design. **[from memory]** Requirements: stable ids
(§5.1) and the operation layer (§2). Each client applies its own
operations optimistically and sends them to the host; the host imposes
a total order, resolves conflicts with *semantic* per-object rules, and
broadcasts the ordered stream; clients that predicted wrong reconcile
(in the worst case by requesting a snapshot — which stage A already
built).

Conflict rules can be simple and per-kind rather than a general
transform:

- attribute edits: per-element, per-attribute last-writer-wins;
- moves: last-writer-wins on position; JLS's existing placement
  validity checks (overlap, wire attachment) run on the host's
  authoritative copy, rejecting ops that no longer apply;
- delete vs. concurrent edit: delete wins; the editor already handles
  elements vanishing (undo does it wholesale);
- wiring: the hardest case — a wire references two `Put`s and
  participates in a `WireNet`; the safe rule is to validate
  wire-creating ops on the host against current geometry and reject
  ones whose endpoints are gone, letting the client's UI report
  "connection failed, circuit changed."

No transformation functions, no per-character algorithms; correctness
rests on the host's total order plus the existing load-path
validation. This is tractable to test with the same golden/round-trip
philosophy the repo already uses: property tests that replay random
concurrent op schedules onto two replicas and assert byte-identical
saved text.

### D. CRDT document (offline merge, no central authority)

Represent the circuit as a CRDT — elements as a map keyed by stable id,
attributes as LWW registers, wires as a set with tombstones — using a
library rather than hand-rolling: **automerge-java** exists, is
maintained by the Automerge org (last updated April 2026), and binds
the Rust core via JNI. **[verified]** Yjs's Rust port (`yrs`) has
bindings for several languages but no first-party JVM binding.
**[from memory]**

- *Buys:* merge without a live host — true peer-to-peer and offline
  editing; the sync protocol comes with the library.
- *Costs:* the CRDT document becomes the source of truth, so either
  the whole `Circuit`/`Element` model is rebuilt over it (a rewrite)
  or a bidirectional mapping layer keeps the object graph and the
  CRDT in sync (subtle, and the mapping is where bugs live). Adds a
  JNI native dependency to a pure-Java single-jar project that ships
  a CycloneDX SBOM and multi-arch installers including riscv64 —
  every platform needs the native library. CRDT merge is also
  *syntactic*: it will happily merge two edits into an overlapping,
  invalid circuit, so all of §C's semantic validation is still
  needed on top.
- *Verdict:* not first. Revisit if asynchronous merge becomes a real
  request (e.g. group projects across sessions) — and note that a
  cheaper 80% answer for async is a semantic three-way merge tool for
  plain-text saves, offline, no networking at all.

### Prior art, briefly

- **Figma** — server-sequenced ops, per-property LWW over stable-id
  objects; explicitly rejected OT as overkill for object graphs.
  **[from memory]** The closest published architecture to what JLS
  needs.
- **CircuitVerse** (web logic simulator): real-time collaboration has
  been an open discussion since 2018 (issue #35) and is not a shipped
  core feature — evidence that this is genuinely hard to retrofit,
  not table stakes. **[verified]**
- **Logisim-evolution / Digital** (the two closest desktop peers):
  neither has any collaborative editing. **[from memory]** JLS
  shipping even Stage 1 would be a differentiator.
- **TinkerCAD Circuits, Wokwi** (web): real-time collab exists in the
  web-native generation of tools, backed by vendor cloud infra JLS
  neither has nor wants. **[from memory]**

## 4. Transport, topology, and security

- **Topology:** one JLS instance is the *session host* (the sequencer
  in §3.C; the snapshot serializer in §3.A/B). No standing server. Two
  join paths: (1) LAN — host listens on localhost/LAN, advertises via
  mDNS (JmDNS is a pure-Java library **[from memory]**) or by showing
  `host:port`; (2) optional tiny relay for NAT-crossing sessions —
  a dumb byte-forwarder keyed by session code, deployable by an
  instructor or offered as a community service. The protocol must not
  care which path carried it.
- **Wire protocol:** length-prefixed frames over TCP (or WebSocket if
  the relay path wants to live behind ordinary HTTPS infra; the JDK
  has a built-in WebSocket *client*, `java.net.http.WebSocket`
  **[from memory]** — the host side would need a small library or a
  hand-rolled server). Frame payloads: session control, snapshot
  (deflated save text — the `CircuitSnapshot` bytes), operation
  records (Stage 2), presence. Text-based payloads in the existing
  save-format idiom keep the protocol debuggable and diffable.
- **Security posture:** collaboration is opt-in per session, listener
  bound to localhost unless explicitly shared, session join gated by a
  code/passphrase (which can double as a PSK for TLS or at minimum
  authentication). All inbound circuit data flows through the
  hardened `LoadError`-taxonomy load path with its hostile-input caps
  — never a Java deserialization mechanism. SECURITY.md's threat
  model gains a section; batch mode and default GUI runs open no
  sockets. SpotBugs and the existing "no raw `JOptionPane`"-style
  ratchet tests suggest the same enforcement idiom: a test asserting
  no socket construction outside the collaboration package.

## 5. Cross-cutting problems (every architecture hits these)

### 5.1 Stable identity

The prerequisite for everything past Stage 1. Design sketch: give every
`Element` a permanent id (creation-time counter scoped to a
session-unique replica id — effectively a compact UUID), persisted via
a new `Attribute`, carried through copy/paste (paste = new ids),
surviving save/load and undo restore. Bump `FORMAT` to 2; version-0/1
files get ids minted at load. The save-time sequential renumbering
(`Circuit.save:1098`) must stop overwriting it — the current `id` can
remain as the file-local reference index it really is, with the stable
id saved separately, so the existing `ref`-resolution machinery in
`finishLoad` is untouched. Wires and `Put`s are referenced relative to
their element (element-id + put-name), which the save format already
effectively does.

### 5.2 Undo under collaboration

Snapshot-stack undo (`SimpleEditor.undo`) must be scoped: in a shared
session, undo means "revert *my* last operation if still applicable,"
implemented as inverse operations — which the operation layer gives
naturally (each command records its inverse). Interim rule for Stage 1
(floor control): only the token holder may undo, and undo broadcasts
like any other change; this keeps the existing mechanism unmodified.

### 5.3 Simulation during a session

Simulation stays local: each replica simulates its own copy on demand.
The only rule needed: a remote update arriving mid-simulation either
queues until the run stops or stops the run — mirroring what the
editor already does about editing-while-simulating. No shared-clock
semantics in scope.

### 5.4 Subcircuits and imports

Circuits nest (`circuit.isImported()`, checkpoint logic at
`SimpleEditor.java:4208`). The session must own the *top-level*
circuit and its whole subcircuit tree — the same resolution
`markChanged` already performs for checkpoints. Editing inside a
subcircuit is just operations addressed by a circuit path.

### 5.5 Presence

Transient, unmerged, trivially lossy: replica id → cursor position,
selection (as stable ids), in-flight gesture hint. Broadcast on a
timer, rendered as overlays in `paintComponent`, dropped on
disconnect. Ship it with Stage 1; it is most of the perceived value.

## 6. Recommended staged path

**Stage 0 — prerequisites (pure refactoring, no networking).**
(a) Stable element identity (§5.1) with a `FORMAT` bump and round-trip
tests. (b) An operation layer: reify the ~10 editor mutations as
command objects with `apply`/`invert`/`serialize`, applied through one
entry point that `SimpleEditor` calls instead of mutating inline.
Adopt incrementally — snapshot undo keeps working throughout, and each
migrated gesture gains precise undo. Coordinates with the #78 element
registry. *This stage improves JLS even if collaboration never ships.*

**Stage 1 — shared session with floor control (§3.A + presence).**
Host/join UI, snapshot broadcast on `markChanged`, read-only follower
mode, editing token, presence overlays, localhost/LAN + session code.
Ships real classroom value with no merge logic at all.

**Stage 2 — simultaneous editing via sequenced operations (§3.C).**
Replace the token with host-ordered operation streams, optimistic
local apply, semantic conflict rules, snapshot resync as the recovery
hatch. Property-test with replayed concurrent schedules asserting
replica convergence (byte-identical save text — the repo's existing
oracle style).

**Stage 3 — only on demand.** Either a semantic three-way merge tool
for plain-text saves (async collaboration with zero networking), or a
CRDT document layer (automerge-java) if peer-to-peer/offline live
merge is truly wanted. Neither blocks nor is blocked by Stages 0–2.

### Effort and risk (rough, relative)

| Stage | New code surface | Risk | Independent value |
|---|---|---|---|
| 0a stable ids | format + `Element` + tests | low | undo/debugging/merge tooling |
| 0b operation layer | `jls.edit` refactor (large) | medium — touches the 4.5k-line state machine | precise undo, scripting, testability |
| 1 floor-control session | new `jls.collab` package + small UI | low-medium — first network code, threat-model update | demos, pair work |
| 2 sequenced ops | protocol + conflict rules | medium-high — distributed-systems testing burden | true simultaneous editing |
| 3 CRDT | document mapping + JNI dep | high — rewrite-adjacent, native deps on 5 platforms | offline merge |

## 7. Open questions for the maintainer

1. **Which user is this for?** Instructor-demonstrates (Stage 1
   suffices for a long time) vs. student group projects (Stage 2 is
   the point). This decides how much of the path to walk.
2. **Is a hosted relay acceptable to operate**, even a trivial one, or
   is LAN-only the boundary? (LAN-only removes all infrastructure and
   most of the security surface, and matches the lab setting.)
3. **Does the operation-layer refactor ride with #78** (element
   registry) as one modernization arc, or land separately first?
4. **Undo depth of feeling:** is "undo only your own ops" acceptable
   to users trained on whole-app undo? (Every mainstream collaborative
   editor answers yes, but it is a behavior change worth recording as
   a decision when Stage 2 lands.)
