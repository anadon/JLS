# Issue #832: TASK-C333-2: cross-partition boundary events are framed, sent and drained over the collaboration transport that already exists, so no second networking stack and no second security surface is created
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Stripped of its framing, #832 asks for two things: **a codec** for the tuple
`(simulation time, net name, value)`, and **a binding of that codec to
`jls.collab.net.Transport`**. Everything else in the body — the socket-confinement
argument, the "second consumer deserves its own review" argument, the offer/drain
discipline — is justification for the *binding*, not for the codec. The codec is the
part FEAT-056 (#333) actually needs; the binding is a bet that the human-collaboration
link is the right pipe for machine-to-machine simulation traffic. That bet is where I
disagree, and it is worth separating because the codec can land today while the binding
is, on the evidence below, both premature and load-bearing in ways the issue does not
acknowledge.

## Grounding

- `src/jls/collab/net/Transport.java:38` exists and carries **opaque** payloads. There
  is no frame-kind tag anywhere: `grep -rn "Transport" src/ | grep -v collab/net/`
  returns nothing. Nothing in `src/` consumes a `Transport` at all — not the CRDT layer,
  not `CausalBuffer`, not `OpEnvelope`. The "second consumer" would in fact be the
  **first**.
- `ChaosTransport` is `final class` (package-private) in `test/jls/collab/net/`, so #333's
  criterion 2 apparatus is reachable only from that test package.
- `src/jls/collab/net/package-info.java` states the property the socket-confinement rule
  exists to protect: `SessionListener` is "the one place in JLS that binds a server
  socket, and only ever after an explicit Share gesture", so "a default GUI start and
  batch mode — which construct no listener — open no port."
- `Sas` is seven glyphs "both humans compare out of band"; `Handshake` is a two-party
  exchange; `SecureLink` poisons the link permanently on one bad tag. This is a
  **two-party, human-attended, fail-closed** trust model.
- ARCHITECTURE.md records (#221) that JLS will not build even a *single-process*
  levelized evaluation pass until a measured trigger fires — "premature optimization
  until CPU-scale designs are actually common". `docs/grand-architecture.md:478-495`
  names JLS's three trajectories: CPU-scale teaching, FPGA deployment, collaborative
  editing. Cluster simulation is not among them, and out-of-process isolation is
  reserved there "for untrusted providers and external tools."

## Where the issue's own headline fails

**"No second security surface" is the claim, and it is the one thing #832 does not
establish.** AC-2 pins a *bytecode* rule: no class outside `jls.collab.net..` names a
socket type. Reusing the package satisfies that rule by construction — it cannot fail.
But the invariant the rule was written to defend is semantic, recorded in the package
javadoc and in #170 P4: **batch mode binds nothing, and a link is trusted only after two
humans read seven glyphs to each other.** A partitioned cluster run is unattended, batch,
and N-party. It must bind listeners with no Share gesture and establish trust with no SAS
comparison. So #832's successors will either bypass the handshake — a second trust model
over the same socket-owning package, which is a second security surface wearing the first
one's clothes — or invent an unattended N-party bootstrap, which is a far larger scope
than "framing, send, drain" and is named nowhere in #333's four planned scopes. Sharing
the *code* is not sharing the *threat model*. AC-2 will be green on the day the property
it stands for is gone.

**The frame-kind demultiplexer does not exist and nobody owns it.** "Becomes a frame kind
on the shipped `Transport` seam" presupposes a kind-tagged, versioned envelope. Today the
only payload shape in the tree is `OpEnvelope`'s save-format text, and it is not even
wired to a transport. Introducing kind-tagging is a **wire-format compatibility decision
for the collaboration program** (#163/#168/#171) — once shipped, every future JLS must
parse it. Having a simulation task invent it as a side effect is exactly the seam
mis-cut #333 §2 says it wants to avoid.

**The codec's hard part is not #832's to answer.** "Reconstructs equal in time, net name
and value" (AC-1) is trivial for a length-prefixed tuple — unless "net name" must be
stable across a re-partition, which is #336 (FEAT-004) and #318 (FEAT-014). So AC-1 is
either near-vacuous or it is quietly doing another feature's work. Either way the
acceptance criterion as stated does not discriminate.

## Alternative 1 (primary): cut the seam at `BoundaryChannel`, not at `Transport`

Define `jls.sim.dist.BoundaryChannel` — `offer(BoundaryEvent)` / `drain()` — with the
**in-process implementation as the shipped default**: a bounded queue between two
partition threads in one JVM. Distribution becomes a single adapter class living inside
`jls.collab.net` (or a sibling), written when someone actually has a cluster.

What this buys:

- Every downstream criterion of #333 — the barrier protocol (#834), the lookahead refusal
  (#836), the 1/2/4/8 invariance suite (#838), checkpoint/resume (#839) — is developable,
  testable and **shippable** with zero sockets, zero handshake, zero N-party trust
  question. AC-2 becomes trivially true because nothing new goes near a socket.
- It delivers a capability real users get today: **partitioned simulation across cores on
  one host.** Multicore is universal; clusters in a teaching context are not. This is the
  honest first increment of #312's capacity axis and it needs no networking at all.
- The reordering guarantee #333 criterion 2 wants is *better* served in-process: an
  in-JVM channel double can enumerate delivery permutations deterministically and
  exhaustively at small N, which `ChaosTransport`'s seeded holdback cannot. Note also
  that `ChaosTransport` **drops** frames, and a conservative barrier over a lossy channel
  deadlocks rather than diverges — so it is a poorer oracle here than the issue assumes.
- It defers, rather than pre-commits, the wire-format decision that belongs to collab.

Concretely: replace #832 with two issues — (a) `BoundaryEvent` plus its codec and the
in-process `BoundaryChannel`, owned by sim; (b) "the collaboration transport gains a
kind-tagged, versioned envelope", owned by #163/#168 and filed there. (b) is not a
prerequisite for (a), for #834, or for #838.

## Alternative 2 (more radical): the boundary is an element, and the port is the one #222 already reserved

Model a cut as a pair of pseudo-elements — a boundary source and a boundary sink — whose
`react` reads and writes a port. The simulator then needs **no distributed run mode at
all**: no second consumer, no new threading model, no new advance rule inside
`Simulator`. Distribution collapses into an element implementation plus a scheduling
policy, which is precisely the shape the recorded architecture already favours (#78
registry, #223 typed extension points, `docs/extension-points.md`).

And when a real out-of-process backend is wanted, the recorded home for it is not the
collab link but the boundary ARCHITECTURE.md §"Plugin trust boundary" (#222) already
reserves: "a stable serialized API over a socket, provider in its own process", the same
subprocess boundary the headless services module already uses for Yosys/GHDL/Icarus. That
boundary is designed for *untrusted, unattended, machine* peers. The collab link is
designed for *two humans who trust each other after comparing glyphs*. #832 picks the
wrong one of the two boundaries JLS has already thought about.

The same element-shaped port pays for itself several more times over — external
co-simulation, hardware-in-the-loop on the FPGA trajectory, and the "host byte port" the
issue itself cites as precedent (which, note, does not exist in the tree:
`grep -rn "byte port" src/ docs/` returns nothing). A general port is worth building; a
single-purpose boundary frame kind on the collaboration socket is not.

## Alternative 3 (cheapest): prove the equality before building the pipe

#333 §6 concedes that a scheduler "may build the suite first against a stub, which is the
better order if the team can afford it." I would go further: **the stub is the product for
now.** Run the partitions as coroutines under one deterministic scheduler, land #838's
1/2/4/8 byte-identity suite and #836's lookahead refusal against it, and the multi-host
case later inherits a proven equality instead of having to establish it over a network.
Transport-first is the one ordering that lands a pipe before there is anything determinate
to put in it.

## Trajectory

I am disregarding #832's acceptance criteria as the measure of success. Taken alone they
are satisfiable by an afternoon's work (a tuple codec plus a loopback round-trip test) and
they certify none of the things that actually make this direction safe or valuable. The
larger tension is that this whole program — CAP-17, #332, #333 and their tasks — pulls
against decisions the project has already recorded and reasoned about: JLS declined a
single-process compiled evaluation pass as premature (#221), yet is planning multi-host
conservative PDES; the grand architecture names three trajectories and cluster capacity is
not one of them; and the property "batch mode opens no port" is treated in this issue as
an architecture-test line item rather than as the deliberate posture of an educational
tool installed on lab machines. If CAP-17 is genuinely funded, the honest first
increments are #370 (compact element representation) and #353 (the quadratic load path) —
both of which raise per-host capacity with no networking and no new trust model — followed
by Alternative 1's in-process partitioning. Boundary events over a socket is the *last*
step of that sequence, not the first.

## What to keep

AC-3 (drain on the simulation thread; no foreign thread posts into a simulator) and AC-4
(the #170 hostile-input discipline, refusal by name, inherited payload cap) are correct
and should survive into whatever replaces this issue — AC-3 unchanged, AC-4 restated
against `BoundaryEvent`'s decoder rather than against a frame kind. AC-5's discipline
(this task decides nothing about advance) is exemplary and should be copied. AC-1 needs
to name *which* net identity it is asserting, and AC-2 should be replaced by the semantic
invariant it is proxying for: **a batch run binds no listener, and no unattended peer is
trusted without a stated bootstrap.**
