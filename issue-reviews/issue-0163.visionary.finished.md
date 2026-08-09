# Issue #163: Distributed collaborative circuit editing: pure-P2P shared sessions (tracking issue)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## 1. What this capstone is actually for

Strip the means away and the stated end is one sentence from the issue's own audience
section: *lab pairs and study groups edit one circuit together instead of emailing `.jls`
files and hand-merging.* Everything else in the body — SIGMA handshakes, SAS emoji, vector
clocks, causal buffers, anti-entropy, floor-control epochs, a two-person LAN pilot — is
machinery chosen to serve that end under one self-imposed constraint recorded in
`docs/collaborative-editing-research.md` §1: **purely P2P, real-time**.

That constraint is doing almost all of the cost. The pedagogical need is "two students
work on one circuit and neither loses work." The design answers a much harder question:
"two students' keystrokes interleave live over an untrusted network with no server."

## 2. Three facts about JLS's trajectory that pull against the issue as written

**(a) The end is already filed, cheaper, and better-evidenced — as #356/#415.** LF-06
(`docs/capability-roadmap/lf-06-diff-merge-vcs.md` §0) states that four of the five
ingredients for a semantic three-way merge already ship: plain-text canonical saves, stable
`sid` identity (#165), a byte-canonical serializer (#166), and the op algebra (#167). #415
then does something #163 never does: it *measures the user's actual pain*. Its O1 shows
`git merge-file` producing a circuit that loads clean and simulates `0xfff` on a 4-bit pin —
a value neither author's own file produces — and O2 shows a clean merge with two elements
claiming id 5. That is the emailing-and-hand-merging workflow failing **silently, today, on
files JLS itself wrote**. #163 has no comparable evidence for anyone wanting live
co-editing; its beneficiary is hypothetical.

**(b) The project has a recorded principle that #163 violates.** `ARCHITECTURE.md` declines
i18n as a "large, ongoing tax with no requesting user," and declines out-of-process plugin
isolation because it "is not built speculatively" pending an actual untrusted-provider case.
#163 is the largest ongoing tax in the tracker *and* it builds a full untrusted-network
boundary speculatively — a per-install keypair, a listening socket, a bespoke SIGMA
handshake, hand-rolled AEAD framing, and a permanent CVE surface — in a GPL-3 educational
desktop tool at bus factor 1. The same document's #222 decision even names #170 as sharing
the plugin threat model. The consistent application of JLS's own recorded reasoning declines
this surface until someone asks for it.

**(c) #508 already ruled, and the ruling is load-bearing, not scheduling noise.** Adoption of
this repo is zero; the live user base (WashU CSE 260M) is on the *bsiever* fork; the filed
programme prices at ~22 maintainer-years; the funded direction is grading integrity,
prominence, accessibility, distribution. #163 is on the defer list. The issue's comment
thread treats deferral as pure scheduling and then spends its energy *defending the
capstone's acceptance bar against a weaker successor* (the #224 REPLAN comment). That is
effort spent protecting a gate for work no one is going to do, on behalf of users who do not
exist yet — while `jls.merge`, which serves users who demonstrably do, does not exist
(`ls src/jls/merge` → no such directory).

## 3. The reframing: merge is the product; real-time is one optional consumer

Invert the research doc's own stage order. Stage 3 ("asynchronous collaboration") is
currently gated behind Stage 2 (CRDT), and the doc concedes in §7 that "the separate semantic
three-way merge tool for plain-text saves remains a cheaper alternative if Stage 2 never
ships." Take that seriously and it stops being a fallback and becomes the deliverable:

- **State-based merge depends only on what has already landed.** `merge3(base, ours, theirs)`
  over three loaded `Circuit`s needs `sid` (#165, closed) and canonical save (#166, closed).
  It needs **none** of #168, #169, #170, #171, and — critically — none of #167's *funnel
  exclusivity*. It merges documents, not op streams.
- **It works with the transport students already have**: git, Drive, Nextcloud, email, a USB
  stick, a shared lab drive. JLS ships no socket, no key, no NAT story, no pilot protocol.
- **It is testable by one person with no second machine and no second human.** #415's P1–P9
  are all CI-executable. #163's A5 explicitly has "no CI substitute" and requires a maintainer
  to schedule a two-person two-machine LAN exercise — at bus factor 1, that gate is not
  merely expensive, it is *unschedulable*, and it sits on the capstone's critical path.
- **It is category-unique.** LF-06: a `git merge` driver that cannot produce a file the tool
  refuses to load appears in none of the 304 surveyed standards and no surveyed competitor.
  Real-time co-editing, by contrast, is a commodity students already have via Zoom/Teams
  screen share with remote control. JLS cannot out-Google Google Docs; it is the only program
  in the world that can merge two `.jls` files correctly.

**Concrete ordering change this implies.** The #163 thread records "#279 blocks #415" — the
online CRDT merge rules gating the offline merger, with #415's cross-check measuring itself
against `CausalBuffer`. **That edge is backwards and should be reversed.** #415's own H2/H4
already say why: the AUTO (online) column is the STRICT (offline) column plus one
`(Lamport, peer id)` tiebreak, and a partial merge that reports a conflict dominates a total
one that silently picks a winner. The merge *semantics* are pure functions of two circuits and
their base; `VectorClock`/`OpEnvelope`/`CausalBuffer` are delivery discipline, not semantics
(#415 O7 says exactly this). Build the table offline, unblocked, today; let any future CRDT
inherit it. Under that ordering #171 is not a prerequisite for anything a user can see.

## 4. If live shared editing is genuinely wanted anyway: two designs the issue never considers

**Design A — borrow the transport.** The `Transport` seam already exists
(`src/jls/collab/net/Transport.java`: opaque in-order frames, null on clean close). Add
`PipeTransport` over stdin/stdout and a `jls --collab-serve` mode; the session is started as
`ssh partner@host jls --collab-serve`. This is still purely P2P by the research doc's own §1
definition (no relay, no operated infrastructure) and it deletes, in one stroke: the identity
keypair, `Handshake`, `Crypto`, `Sas`, `KnownPeers`, the listening socket, the NAT discussion,
`BootListenerHygieneTest`'s reason to exist, and most of #170's network hardening. Peer
authentication becomes SSH host-key verification, which lab users already do and which has
thirty years of review behind it — versus a bespoke handshake whose cipher was chosen because
"CodeQL's allowlist predates ChaCha20-Poly1305" (a tell that the crypto is being steered by
tooling convenience rather than analysis). #168 collapses to roughly one class.

**Design B — sync the document, not the ops.** For the two-peer floor-control case the whole
CRDT is unnecessary. Circuits are kilobytes; `Circuit.save` is canonical and deterministic
(#166); `CircuitSnapshot` already stores a whole circuit as deflated save bytes and restores
it through the ordinary load path, which is precisely how undo works today. A shared session
in floor-control mode is therefore: *on `markChanged`, the holder sends its canonical bytes;
the receiver loads them; `stateHash()` is displayed as the in-sync indicator.* Convergence is
trivially true because there is one writer and the state **is** the document. No ops on the
wire, no causal ordering, no anti-entropy, no compaction, no convergence property suite — and
no dependence on the funnel being exclusive. #171 disappears; #169 shrinks to a peer panel
and a token; A3/A4/A6 all evaporate.

Design B delivers the pair-tutoring value the research doc §7 attributes to Stage 1, for a
small fraction of Stage 1+2, and it is a strictly better place to *stop* than the current plan
admits — the plan treats floor control as a way-station to the CRDT rather than as a possible
terminus.

## 5. A seam problem the issue's layering rules do not catch

`CircuitOp.apply(Circuit, Graphics)` (`src/jls/collab/op/CircuitOp.java:51`) takes an AWT
`Graphics` for geometry recomputation. `ArchitectureRulesTest.collabLayersAreHeadless` passes
because AWT-not-Swing is permitted — but the consequence is that **applying a remote peer's op
requires a graphics context**, in exactly the layer (`crdt`) the design insists is headless and
mechanism-only. #415 §7.9 has already had to route around this ("any `Graphics` requirement
discovered is a contract deviation to report"). This is not a bug to patch; it is evidence that
the op algebra was shaped by the editor and is not yet the neutral kernel the replication story
assumes. The state-based route sidesteps it entirely — `Circuit.load`/`save` are already headless
and already the undo path.

## 6. What I am disregarding from the stated acceptance criteria, and why

- **A5 (the two-person, two-machine pilot).** Disregarded as a gate. It is the only criterion
  with "no CI substitute," it requires a second human the project does not have, and its
  presence means the capstone cannot close no matter how much code lands. A capstone whose
  acceptance depends on a resource of quantity zero is not a plan.
- **A4 ∧ A6.** The adversarial comment correctly notes A4 cannot close before A6. A6 requires
  every `markChanged` gesture migrated; `docs/operation-layer.md` still lists placement, paste,
  wiring commit, dialog commits, `EditOrderedRows` and `ImportSubcircuit` as inline or deferred,
  inside a 5,852-line `SimpleEditor`. So this capstone's acceptance transitively contains a full
  editor rewrite (#84). Under the redirect that coupling vanishes: a document merge does not care
  how a gesture mutated the document, only what the document became.
- **"Purely P2P" as an axiom.** Recorded as maintainer direction, not derived from a user need.
  Design A honours it while deleting the cost; the merge route makes it moot.

## 7. What survives, and where it should live

- **#165, #166 — keep, already closed.** They are the redirect's entire foundation.
- **#167 — keep, but re-home.** Its value is editor architecture (#84 decomposition, headless
  core #77, the `collab.op-observer` seam in `OpExtensionPoints`, precise undo, replay,
  scripting), not replication. It belongs to #224, which the #508 REPLAN already leaves it in.
  It should not be gated on a collaboration outcome, and this capstone should not gate on its
  exclusivity.
- **#168 → collapse to Design A's `PipeTransport` if ever wanted; retire the crypto.**
- **#169 → optional Design B slice; #170 → fold its untrusted-input work into the merge path**
  (a merged file from a colleague is the same untrusted-input class — #415 §7.3 says so).
- **#171 → retire.** Nothing a user can see depends on it once #415's rule table exists.
- **The one thing to file now:** unblock #415 by striking the `#279 blocks #415` edge and
  restating the merge rules as a state-based table with the online tiebreak as a derived,
  unbuilt column.

## 8. Verdict

**redirect.** The issue is exceptionally well specified — better than almost anything in this
tracker — and specified against the wrong target. Its end (lab partners collaborating without
losing work) is reachable now, with landed foundations, no network surface, no second human,
and a category-unique result, via semantic three-way merge. The pure-P2P real-time stack is a
speculative untrusted-input boundary with no requesting user, an unschedulable acceptance gate,
and an acceptance criterion that transitively contains a full editor rewrite. Retarget this
capstone at #356/#415's merge line, invert the #279→#415 ordering, keep #167 as kernel
architecture under #224, and hold the session stack behind the two cheap designs above should a
real user ever ask.
