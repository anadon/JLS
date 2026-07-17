# Distributed Collaborative Circuit Editing for JLS — Research Report

*July 2026, revision 2. Prepared for the JLS maintainer. Revision 2
incorporates maintainer direction: the solution must be **purely
peer-to-peer** (no central server, no operated relay); determinism of
circuit representation/serialization must be **confirmed against the
code**, not assumed; sequence ("linked-list") CRDTs and Raft — including
its revised membership-change designs — must be evaluated; the peer
lifecycle (connect, add, manage, drop, unstable peers) must be planned
for correctness and ergonomics; and security must be treated as a
first-class concern, in particular the risk of arbitrary remote
actions. Codebase claims carry `file`/method anchors read from HEAD.
External claims are **[verified]** (checked against live sources this
month) or **[from memory]** (well-known but not re-verified — confirm
before a design commitment).*

*Implementation is planned in GitHub issues: tracking issue #163
(architecture, layering, interfaces, validation program) with
sub-issues #165 (stable ids), #166 (deterministic serialization),
#167 (operation layer), #168 (transport/SAS), #169 (session
lifecycle), #170 (security hardening), #171 (CRDT replication).*

## Executive summary

1. **Serialization is NOT deterministic today — confirmed, with the
   evidence in §2.** Elements live in a `HashSet` with identity
   hashing, so save order varies across JVM instances; element ids are
   assigned *by save order* and wires cross-reference those ids, so two
   saves of the same logical circuit can differ in content, not just
   line order. The test suite already works around this
   (`FileFormatSupport.canonicalize` sorts blocks and strips id lines,
   and documents itself as unsound for circuits with wires). Canonical,
   byte-deterministic serialization is achievable and cheap once
   elements have stable ids; it should be **Stage 0 work**, because a
   deterministic canonical form is the convergence oracle every P2P
   architecture needs, and it doubles as a user-visible "in sync"
   indicator.
2. **The Google Docs model does not transfer literally — but its
   lesson does.** Google Docs is centralized operational
   transformation (Jupiter lineage): an authoritative server orders
   every operation, which is exactly what a purely P2P design cannot
   have. **[from memory]** The P2P-compatible descendants of that line
   of work are CRDTs. Linked-list *sequence* CRDTs (RGA, Logoot/LSEQ,
   Yjs's YATA) solve ordered-text merge; a circuit, however, is an
   identity-keyed object graph, so the top-level document wants
   *set/map* CRDT shapes (add-wins element set, per-attribute
   last-writer-wins registers), with sequence CRDTs reserved for the
   genuinely ordered substructures (state-machine tables,
   signal-generator programs, memory initial contents). §3.
3. **Raft solves the wrong half of the problem, but its membership
   discipline is worth stealing.** Raft (with the paper's joint
   consensus or the dissertation's single-server membership changes —
   what implementations ship as "protocol version 2+") would give a
   totally ordered op log with an *elected* leader, satisfying "no
   fixed infrastructure." Its cost is quorum availability: in the
   dominant JLS session size — **two peers** — a majority is both
   peers, so any Wi-Fi blip freezes editing for everyone. That is the
   wrong availability profile for a lab tool. Recommendation: CRDT
   replication for the document (available under partition, merges on
   heal), with Raft's *membership-change rigor* — epoch-numbered,
   log-recorded, one-at-a-time membership operations — borrowed for
   the peer roster. §4.
4. **Peer lifecycle and ergonomics are designed in §5** as concrete
   flows: long-term per-install keypairs; join by `IP:port`; mutual
   verification by short authentication string rendered as
   emoji/word pairs (Signal/Telegram-style SAS, bound to the key
   exchange so a man-in-the-middle changes what both sides see);
   transitive admission with visible attribution; heartbeat failure
   detection where unreachable ≠ removed; version-vector anti-entropy
   resync for flaky peers; snapshot catch-up past the log-compaction
   horizon; and an honest treatment of NAT — pure P2P works cleanly on
   a LAN, and across networks only if at least one member is
   reachable, with any member able to relay frames for the others.
5. **Security (§6) starts from the maintainer's stated risk: arbitrary
   remote actions.** The mitigation is structural: the entire remote
   surface is a closed, data-only operation vocabulary — no file
   paths, no class names, no code, no settings. The one existing
   footgun is the reflective element loader
   (`Class.forName("jls.elem." + type)` in `Circuit.load`), which for
   network input must be replaced by an explicit allowlist registry.
   All frames are encrypted and signed per-peer (attribution enables
   targeted revert and ejection); resource caps reuse the
   hostile-input hardening; the listener exists only while a session
   is active and never in batch mode.
6. **Revised staged path** (§7): Stage 0 — stable ids, operation
   layer, deterministic canonical save; Stage 1 — P2P session with
   token floor control, SAS join, presence; Stage 2 — simultaneous
   editing via an operation-based CRDT over causal broadcast
   (bespoke core recommended over automerge-java's JNI dependency;
   tradeoff analyzed); Stage 3 — offline/async merge, which falls out
   of the persisted op log.

---

## 1. Constraints (recorded from maintainer direction)

- **Purely P2P.** No central sequencer, no operated relay service, no
  standing infrastructure. Peers are symmetric; any machine can leave
  and the session survives. (A member *forwarding* frames for members
  who cannot reach each other directly is still P2P; a dedicated
  relay box is not.)
- **Deterministic representation and serialization** must be
  established and pinned by tests, as the foundation for convergence
  checking.
- **Peer lifecycle is a designed artifact**, not an emergent behavior:
  connecting, adding, managing, dropping, and unstable peers all need
  specified correctness semantics *and* specified UX.
- **Out-of-band human verification** of peers (emoji-style SAS) is the
  preferred trust bootstrap.
- **Security is in scope from the start**; the headline risk is any
  path from network input to actions outside the circuit model.

## 2. Determinism audit (the confirmed answer)

**Question:** is a circuit's serialized form a pure function of its
logical content? **Answer: no — not today.** Four findings, each with
anchors:

1. **Element storage is unordered with identity hashing.**
   `private Set<Element> elements = new HashSet<Element>()`
   (`src/jls/Circuit.java:55`), and `Element` overrides neither
   `hashCode` nor `equals` (verified by inspection of
   `src/jls/elem/Element.java`) — so iteration order depends on
   identity hash codes, which vary per object, per run, per JVM.
2. **Save order and element ids inherit that nondeterminism.**
   `Circuit.save` iterates the set to assign ids sequentially
   (`src/jls/Circuit.java:1098-1102`) and again to emit `ELEMENT`
   blocks (`:1105-1107`). Same circuit, two load instances → different
   block order *and* different id assignments.
3. **Ids leak into content, not just order.** `WireEnd.save` writes
   `ref` lines using the save-time ids of referenced elements
   (`src/jls/elem/WireEnd.java:535,540`). So for any circuit with
   wires, the differences are not removable by sorting lines: the
   *bytes differ semantically*.
4. **The test suite already knows.** `FileFormatSupport.canonicalize`
   (`test/jls/FileFormatSupport.java:82`) exists precisely because
   "Elements live in a HashSet, so block order and the ids assigned at
   save time are not stable across load instances" (its own comment),
   and it documents itself as "Only sound for circuits without id
   cross-references (ref lines)". Round-trip tests compare saves only
   after canonicalization.

Two mitigating facts, also confirmed:

- **Within one live circuit instance**, repeated saves are stable in
  practice (a `HashSet`'s iteration order does not change while its
  contents do not), which is all `CircuitSnapshot` dedup relies on
  (`SimpleEditor.pushCopy`, `src/jls/edit/SimpleEditor.java:4238-4241`).
- **The repo already values load-side determinism**: the load path
  deliberately preserves file order via a `LinkedHashSet`
  (`loadedElements`, `src/jls/Circuit.java:74-76`) so that wire-net
  construction and multi-driver resolution are deterministic
  (issue #98, S1). This audit extends that same principle to the
  save side.

### What to build

**Canonical serialization**: saves emit elements sorted by **stable
element id** (§7 Stage 0a), and `ref` lines use stable ids. Then:
serialized form = pure function of logical circuit content, and
byte-equality of saves = replica convergence. This enables:

- the **convergence oracle** for all replication testing (two replicas
  apply the same operations in any order → byte-identical saves);
- a cheap **divergence detector** between live peers (exchange a hash
  of the canonical form; display "in sync ✓" in the peer panel);
- honest **snapshot dedup** across restores, and diff-friendly
  plain-text saves (`-savetext`, issue #129) that stop churning on
  re-save.

**Interim step, available before stable ids:** changing `elements`
from `HashSet` to `LinkedHashSet` makes *save → load → save* a byte
fixed point (load already preserves file order; insertion order would
then survive to the next save). That alone lets round-trip tests drop
`canonicalize` for the fixed-point half of their assertions. It does
**not** make two replicas converge (they insert in different orders) —
stable-id sorting remains the real fix. Draw order and save-id
assignment change observably; the existing golden suites are the
safety net.

Deliverable to pin it: a `DeterministicSaveTest` asserting (a)
save-twice byte equality, (b) save→load→save byte equality, both
*without* canonicalization, on fixtures including wires; retire
`FileFormatSupport.canonicalize` when it passes.

## 3. The "Google Docs model" and linked-list CRDTs

### What Google Docs actually is — and why it can't be copied here

Google Docs descends from the Jupiter/operational-transformation line:
every client sends operations to an **authoritative central server**,
which imposes the total order and transforms concurrent operations
against each other; clients converge because the server is the single
arbiter. **[from memory]** Under the pure-P2P constraint there is no
such arbiter, and decentralized OT (transform without a server) is a
famously failure-prone research area — several published algorithms
were later shown incorrect. **[from memory]** CRDTs were developed
largely as the principled escape from exactly that: datatypes whose
merge is commutative/associative/idempotent, so *no* ordering
authority is needed.

So: the maintainer's instinct — "the Google Docs idea, but as a
CRDT" — is precisely the right translation of that model into P2P.
The remaining question is *which* CRDT shapes fit a circuit.

### Where linked-list (sequence) CRDTs do and don't fit

Sequence CRDTs — RGA/causal trees, Logoot/LSEQ, Yjs's YATA — solve
one problem: maintaining a shared *ordered sequence* under concurrent
insert/delete, by giving each item a permanent identity and encoding
order relative to neighbors (the "linked list" view). **[from memory]**

A circuit document is mostly **not** a sequence. It is an object graph
keyed by identity: elements with attributes and positions, wires
referencing element connection points. Sequence CRDTs impose ordering
machinery where no order is meant. The natural decomposition is:

| Circuit state | CRDT shape |
|---|---|
| the set of elements | add-wins observed-remove set, keyed by stable id (tombstones for deletes) |
| element attributes (position, bits, delay, name, …) | one last-writer-wins register per attribute (Lamport timestamp + peer id tiebreak); multi-value registers only if surfacing conflicts to users ever becomes desirable |
| wires | OR-set of wire objects whose endpoints are (stable element id, put name) — the save format already references puts by name relative to their element |
| ordered substructures: state-machine state/transition tables, signal-generator programs (`SigSim`/`TestGen`), truth-table rows | **here sequence CRDTs genuinely apply** — an RGA-style list per substructure |
| memory initial contents | index-addressed → a map of address→LWW value, not a sequence |
| draw/z-order | today implicit (unordered set); treat as don't-care or a single LWW layer attribute — not worth a sequence CRDT |

One anti-pattern to name and avoid: treating the *save text* as a
collaborative text document and merging it with a sequence CRDT (each
line an item). It is seductive — the serializer exists, text CRDTs are
off-the-shelf — but merges would be syntactic: two valid interleavings
can yield duplicate ids, dangling `ref`s, or overlapping geometry.
The Google-Docs-lineage lesson that *does* transfer is **intention
preservation at object granularity**: operations must be expressed in
the vocabulary of the model ("move element E to (x,y)", "connect put P
to put Q"), and validated against model invariants on apply — never as
edits to the serialized bytes.

## 4. Raft, and Raft's revised membership changes

### What Raft would buy

Raft replicates a totally ordered log among peers with an **elected**
leader — no fixed infrastructure, so it is admissible under the P2P
constraint. It would give collaboration semantics of the strongest
kind: every peer applies the identical operation sequence; divergence
is impossible by construction; there is no merge logic at all. It also
brings the most rigorous published treatment of exactly what the
maintainer asked to have planned — **membership change**. The original
paper uses *joint consensus* (a transitional configuration requiring
majorities of both old and new member sets, so the cluster stays live
through the change); Ongaro's dissertation replaced it in practice
with simpler **single-server changes** (add or remove exactly one
member at a time), which is what mainstream implementations ship as
their revised ("v2+") membership protocol. **[verified]** (See
[the Raft paper](https://raft.github.io/raft.pdf), the
[Wikipedia summary](https://en.wikipedia.org/wiki/Raft_(algorithm)),
and an [engineering-practice discussion](https://www.alibabacloud.com/blog/raft-engineering-practices-and-the-cluster-membership-change_597742)
of joint-consensus vs single-server changes.)

### Why it is the wrong document-replication choice for JLS

- **Quorum vs the two-peer session.** Raft commits require a majority.
  The dominant JLS session is a lab pair: n=2, majority=2 — *any*
  single disconnect (sleep, Wi-Fi roam, cable pull) halts editing for
  **both** peers until reconnection. Even n=3 halts on two absences.
  A collaboration feature that freezes the healthy peer's editor
  because the flaky peer's laptop slept is ergonomically worse than
  not collaborating. CRDT replication inverts this: everyone can
  always edit; reconnection merges.
- **Partition behavior.** Raft's minority side becomes read-only. That
  is arguably the pedagogically "safe" behavior (no forked truth), but
  combined with the quorum math above it means unstable networks —
  the stated reality to design for — produce frequent editing freezes.
- **A leader still exists at runtime.** Topologically P2P, but all
  operations route through the current leader, and elections add
  visible stalls on churn. The symmetry is administrative, not
  operational.
- **Implementation weight.** Raft must not be hand-rolled — subtle
  bugs in reimplementations are a documented cottage industry
  **[from memory]** — and the credible JVM libraries (Apache Ratis;
  SOFAJRaft **[from memory]**) are heavy dependencies for a project
  that ships one runtime dependency (XZ, `pom.xml`) and multi-arch
  installers including riscv64.

### What to keep from Raft

The membership discipline, applied to the peer roster rather than the
document: **membership changes are explicit, signed, epoch-numbered
entries in the replicated state, applied one at a time** (the
single-server-change insight: never let two concurrent roster changes
race), and every peer can reconstruct the roster history. §5.3 uses
exactly this. If the maintainer ultimately weighs strong consistency
above availability — "the healthy peer freezing is better than ever
seeing a merge" — then Raft-via-Ratis is the honest Path B, with the
two-peer caveat stated in the UI; this report recommends against it.

### Recommendation

**Operation-based CRDT replication over causal broadcast** for the
document: ops carry (peer id, per-peer sequence number, causal
dependencies/vector clock); delivery is causal-order; concurrent ops
commute by the §3 datatype rules; convergence is testable via the §2
canonical-save oracle. Membership is Raft-style explicit entries.
No quorum anywhere; a lone peer on a train can keep editing and sync
later — which incidentally makes Stage 3 (offline/async merge) fall
out of the same machinery instead of being a separate project.

## 5. Peer lifecycle: correctness and ergonomics

### 5.1 Identity

Each installation generates a long-term signing keypair (Ed25519) on
first collaborative use, stored in the user config area; the **peer
id is the public-key fingerprint**, with a user-chosen display name
attached (names are labels, ids are keys — two "Alex"es never
collide). A session is identified by a random session id minted by
whoever starts it; the starter has no ongoing special role.

### 5.2 Connecting and verifying (the SAS flow)

The maintainer's sketch — add by IP, confirm out of band with emojis —
is the right shape; here it is made precise:

1. **Start session.** Peer A opens a circuit → "Share → Start
   session". JLS binds a listener (only now; §6.4) and shows a
   join string: `<ip>:<port>` (all candidate addresses listed; LAN
   address first).
2. **Join.** Peer B enters the string. A and B run an authenticated
   key exchange binding both long-term keys and fresh ephemerals
   (Noise XX pattern, or TLS 1.3 with raw public keys — either is
   fine; no CAs, no certificates **[from memory]**).
3. **Verify.** Both screens derive a **short authentication string**
   from a hash of the full handshake transcript (both identity keys +
   ephemerals + session id) and render the same ~40–60 bits as 5–7
   symbols. The peers compare them out of band — aloud across the
   lab bench, over a call — and each clicks *Confirm*. A
   man-in-the-middle necessarily changes the transcript and thus the
   symbols on one side; this is the Signal safety-number / Telegram
   call-emoji construction. **[from memory]**
4. **Persist.** On confirm, each side records the other's key as
   verified. Reconnections with a known key skip verification;
   a *changed* key for a known peer is a loud warning, never silent.

**Rendering caveat (practical, JLS-specific):** color emoji in Swing
across platform JREs — especially Linux — is unreliable; a fingerprint
UX must not depend on fonts users don't have. Recommendation: bundle a
small fixed set of ~64 distinguishable, *nameable-aloud* glyphs as
images (JLS already ships toolbar gifs) and display each with its
English name beneath ("🐢 turtle · 🔑 key · 🌲 tree …" — the PGP word
list precedent); comparison then works even rendered as words alone.

### 5.3 Membership: adding, managing, dropping

Membership is replicated state, not a connection side effect:

- **Admission.** Any verified member may admit a new peer (the group-
  chat model — matches lab reality where the session starter may
  leave first). Admission is a signed membership entry: *"key F
  (name) admitted by key A, epoch e, SAS-verified"*, applied
  one-at-a-time in epoch order (§4's borrowed discipline: concurrent
  roster changes for the *same* epoch resolve deterministically —
  lowest admitting-peer id wins, loser re-issues — so the roster
  never forks). Every peer sees "Alex added Sam ✓" in the session
  log. Whether admission should instead be starter-only is left as an
  open question (§8); transitive is recommended.
- **The roster is not the reachability set.** A member is `reachable`
  or `unreachable` (heartbeat state, local knowledge, shown in UI) —
  but stays a member either way; their edits remain valid and their
  key stays trusted. Removal is a distinct, explicit act:
  - **Leave**: a signed goodbye entry (clean, immediate).
  - **Eject**: any member can issue a signed removal entry; loudly
    attributed ("Alex removed Sam"). After removal, peers drop the
    key from the session and refuse its frames (new epoch =
    rekeyed session traffic). For a pedagogy tool the check on abuse
    is social visibility, not cryptographic governance — recorded as
    a deliberate simplification.
- **Peer panel** (ergonomics): every member with name, verified badge,
  reachability, last-seen, an attribution color used for their
  cursors/selections *and* recent-edit highlights, and the §2 sync
  hash ("in sync ✓" / "syncing…" / "diverged — merging").

### 5.4 Unstable peers, rejoin, and anti-entropy

- **Failure detection:** periodic heartbeats; missing k intervals →
  `unreachable` (grey in panel). At session sizes ≤8 a simple timeout
  is adequate (φ-accrual is overkill). Nothing else happens: editing
  continues everywhere.
- **Op transport:** each op is signed, has (peer id, sequence number,
  vector clock), and is gossiped to all reachable members; any member
  forwards for members without a direct path. Receipt is idempotent
  (dedup by op id) — flapping links cannot double-apply.
- **Resync on return:** peers exchange vector clocks, send exactly the
  missing ops, apply causally. A short blip heals in one round trip,
  invisibly.
- **Compaction horizon:** the op log is bounded; peers periodically
  agree (lazily, no consensus needed — minimum across last-known
  vector clocks) that a prefix is universally applied and fold it
  into a canonical snapshot (§2 form, deflated — the existing
  `CircuitSnapshot` machinery). A peer absent past the horizon
  rejoins by adopting the snapshot and **replaying its own un-acked
  local ops on top** — legal because ops are id-addressed and
  commute; ops referencing since-deleted elements fail validation
  and surface as "n of your offline edits no longer applied."
- **Partitions:** both sides keep editing (CRDT choice, §4); on heal,
  anti-entropy merges and the sync indicator narrates it. The UI must
  make partition state *visible* (banner: "2 peers unreachable —
  edits will merge when they return") so merges never feel like
  spooky action.

### 5.5 The NAT reality (honest limits of "purely P2P")

On a LAN — the lab, the classroom, a dorm — direct P2P works with
nothing but the join string. Across the public internet, two peers
who are both behind NAT cannot connect without either (a) one member
being reachable (port forward, university machine, VPS one student
already has), after which **any-member forwarding** (above) carries
everyone else, or (b) user-supplied overlays (Tailscale, ZeroTier)
that make machines mutually reachable, which JLS should document but
not depend on. **[from memory]** NAT hole-punching without any
rendezvous point is not reliably possible; this is a fact of the
internet, not a design defect, and the docs should say so plainly.
No JLS-operated infrastructure, per the constraint.

### 5.6 Session end

There is none, structurally: each peer holds the full circuit and can
save it at any time; when the last peer closes, everyone has a copy.
If the session converged, those copies are **byte-identical** (§2) —
worth surfacing at close ("saved; identical to Sam's and Alex's
copies"). Divergence at close (someone left mid-merge) is shown, not
hidden: "your copy includes 3 edits Sam hasn't seen."

## 6. Security

Threat-model addition to SECURITY.md, organized by attacker position.
The maintainer's headline risk — **arbitrary remote actions** — is
addressed structurally first.

### 6.1 No path from network input to actions

The remote-influence surface is a **closed, data-only vocabulary**:
circuit-model operations, membership entries, presence, and sync
frames. Nothing in any frame may name a file path, a class, a URL, a
setting, or code; there is no plugin negotiation, no capability
exchange, no remote-triggered dialog beyond rate-limited session
notices. Concretely:

- **Never Java object serialization** anywhere in the protocol
  (frames are a fixed binary/text schema parsed with the same
  hostile-input discipline as file loading).
- **The reflective loader is the one existing footgun.** `Circuit.load`
  instantiates elements via
  `Class.forName("jls.elem." + type).getConstructor(Circuit)`
  (ARCHITECTURE.md, save/load pipeline). Package-prefix constraint is
  acceptable for local files; for *network-delivered* snapshots and
  ops, the element type must be checked against an **explicit
  allowlist registry** before any reflection — which is the same
  registry issue #78 wants for its own reasons. The collaboration
  work should treat #78's registry as a dependency.
- **Semantic validation on apply:** every remote op passes the same
  model-invariant checks as a local gesture (placement validity, put
  existence, cap limits). Invalid ops are rejected and counted;
  passing a threshold disconnects the peer with a visible reason.
- **Enforcement ratchet:** following the repo's idiom
  (`NotificationRatchetTest`), a test asserting no socket/channel
  construction outside the collaboration package, so the network
  surface cannot quietly spread.

### 6.2 Network attacker (off-path / on-path)

All session traffic is encrypted and integrity-protected under keys
from the §5.2 handshake; the SAS binds the handshake to the humans, so
an on-path attacker cannot silently insert themselves at join time,
and key-change-on-reconnect warnings cover later substitution.
Every op is *individually signed* by its originator — required anyway
for attribution (§6.3) because ops are forwarded peer-to-peer, and it
makes forged authorship infeasible regardless of which member relayed
a frame. Replay is bounded by per-peer sequence numbers and the
session epoch.

### 6.3 Malicious or compromised member (valid-but-destructive ops)

An admitted peer can send semantically valid vandalism ("delete
everything") — no protocol prevents what the model permits. The
defenses are recovery and accountability, and they are cheap here:

- **Attribution everywhere:** signed ops → the peer panel colors and
  the session log show exactly who did what.
- **Targeted revert:** the operation layer records inverses (needed
  for collaborative undo anyway, §7 Stage 0b); "revert everything
  peer X did since T" is a mechanical replay of inverses, offered
  directly in the eject flow ("Remove Sam and undo their changes?").
- **Local history:** the existing undo stack and checkpoint files
  (`.jls~`) already bound worst-case loss.
- Residual risk stated honestly: a verified classmate can disrupt a
  session and be removed and reverted; that is the intended tradeoff
  for a pedagogy tool, and it is documented rather than
  crypto-engineered away.

### 6.4 Availability and resource abuse

- **Listener hygiene:** no socket exists until "Start session"; bound
  interface is user-visible; closed when the session ends; **never**
  in batch mode (`-b`) or default GUI start. No UPnP, no automatic
  port mapping.
- **Caps:** per-frame size limits, per-peer rate limits, op-backlog
  and element-count ceilings reusing the issue #38 hostile-input caps
  and `LoadError`-style structured rejection. Heartbeat floods and
  presence spam are rate-limited transient state (never persisted).
- **Key storage:** the identity key lives in the user config dir with
  restrictive permissions; compromise of a student account is in the
  "compromised member" bucket above — eject + revert, and the key
  can be regenerated (new identity, re-verify via SAS).

## 7. Revised staged path

**Stage 0 — foundations (no networking; each independently valuable).**

- **0a. Stable element identity.** Permanent id minted at creation
  (replica id + counter), persisted via a new `Attribute`, carried
  through copy/paste (paste mints fresh ids), surviving save/load and
  undo restore; `FORMAT` bump with ids minted at load for old files.
  The save-time sequential renumbering (`Circuit.save:1098`) stops
  overwriting identity; `ref` resolution moves to stable ids.
- **0b. Operation layer.** Reify the ~10 editor mutations as command
  objects (`apply`/`invert`/`serialize`) behind one entry point that
  the `SimpleEditor` state machine calls instead of mutating inline;
  migrate gesture-by-gesture under the existing snapshot-undo safety
  net. Gains precise undo, per-op attribution, and testability now;
  becomes the op vocabulary of Stage 2. Coordinates with #78.
- **0c. Deterministic canonical serialization** (§2): sort by stable
  id, stable refs, `DeterministicSaveTest` (save-twice and
  save→load→save byte equality, wires included), retire
  `FileFormatSupport.canonicalize`. Optional interim `LinkedHashSet`
  step. *This stage is where "confirm deterministic representation"
  becomes enforced rather than audited.*

**Stage 1 — P2P session, floor control, full trust machinery.**
Identity keys, SAS join/verify flow, membership entries, peer panel,
presence overlays, heartbeats, snapshot broadcast on
`markChanged`, editing token (one writer at a time; token is just
replicated state). No merge logic yet — but *all* transport, crypto,
membership, and lifecycle work (§5, §6) ships and gets exercised
here, and a pair demoing/tutoring gets real value.

**Stage 2 — simultaneous editing (op-based CRDT).**
Replace the token with §3's CRDT semantics over causal broadcast;
anti-entropy resync; convergence property tests replaying random
concurrent schedules and asserting canonical-save byte equality
across replicas; collaborative undo = inverse of own ops.
*Build-vs-buy:* *bespoke CRDT core recommended* — the op vocabulary
is small (~10 kinds), the merge rules are per-kind and simple
(§3 table), the oracle is strong (§2), and automerge-java, while real
and maintained **[verified]**, brings a JNI native library to a
pure-Java project shipping multi-arch (incl. riscv64) single-jar
installers, and its syntactic merges still need all the semantic
validation on top. Revisit if the bespoke core's testing burden
surprises.

**Stage 3 — asynchronous collaboration.** Persist the op log next to
saves; "merge changes from file" becomes anti-entropy against a file
instead of a socket. The separate semantic three-way merge tool for
plain-text saves remains a cheaper alternative if Stage 2 never ships.

## 8. Open questions for the maintainer

1. **Session size envelope:** is 2–8 peers the design target (full
   mesh, simple timeouts suffice), or must lecture-scale (30+)
   sessions work eventually (gossip fan-out, presence batching)?
2. **Admission policy:** transitive (any verified member admits, with
   attribution) or starter-only? §5.3 recommends transitive.
3. **Strong consistency vs availability, final call:** does the
   two-peer freeze problem (§4) settle it for CRDTs, or is
   "never merge, ever" worth Raft's availability cost in some mode?
4. **Glyph set for SAS:** bundled named images/word pairs (§5.2
   rendering caveat) acceptable, or is platform emoji rendering worth
   chasing?
5. **Does Stage 0 ride with #78** (element registry) as one
   modernization arc? The network allowlist (§6.1) and the operation
   layer both want the registry.
6. **Undo semantics decision to record when Stage 2 lands:** "undo
   reverts only your own operations" (the mainstream collaborative
   answer) vs whole-state undo.
