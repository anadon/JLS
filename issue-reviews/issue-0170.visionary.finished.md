# Issue #170: Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests (collab cross-cutting)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its bookkeeping, #170 makes one claim: **the remote surface must be closed by
construction, and the closure must be enforced by something that fails the build.** That claim is
correct and it is the most defensible part of the whole #163 program. It is also the same claim
JLS already made three other times — `SaveTags`/`ElementRegistry` freezing tag text out of
`Class.forName`, `HeadlessCoreRatchetTest` freezing AWT out of `jls.sim`, and ARCHITECTURE.md's
#222 decision ("the closed, typed provider API is the plugin analogue of collab's closed data-only
op vocabulary"). Endorsed without reservation as a *goal*.

The reframing is about **where the closure lives**. As built, #170 keeps its closure in a
hand-written list, four unrelated constants, and a set of grep-and-bytecode ratchets that watch for
the list and the constants being edited. That is closure by vigilance. The project's own best move
— the one grand-architecture.md §10 names as its leverage argument — is closure by *totality over a
registry*. Three concrete redesigns follow, in increasing order of leverage.

## A. The allowlist is a copy of the registry. Make it a property of the registry.

Measured at the checked-out tree, not quoted from the issue:

- `ElementVocabulary.ALLOWED` holds **34** tokens. `ElementRegistry` holds **35** descriptors.
  The set difference is exactly `{TestGen}` — nothing else, in either direction.
- The issue's own comment history reports this list as 32, then 33, then 34, then 35 tokens across
  three weeks. The list has never been wrong about *security*; it has never once been right about
  its own size either.
- `SubCircuit` is in the allowlist, but `ElementBlocks.load` rejects `SubCircuit` outright and the
  "subcircuit-import op kind" it defers to does not exist among `CircuitOp`'s eleven permitted
  subtypes. So the allowlist's membership is already not the reachable set.
- `ElementVocabulary`'s javadoc and `docs/collab-vocabulary.md` both still pin the list "against the
  help system by `HelpTopicsTest`"; `ElementVocabularyTest` has pinned it against
  `jls.edit.Palette.entries()` since PR #246. The *normative* document is stale. That is the
  maintenance tax of a duplicate, appearing on schedule.

The #352 absorption diagnosed the duplication correctly and then patched it with a second
duplicate: `A = R \ Δ`, an explicit deny list, plus IC-5 ("empty the deny list and observe the test
go red") and IC-6 ("a new type is not network-reachable by default"). Δ is `{TestGen}` today. A deny
list is still a list somebody must remember to edit when a batch-only type is added — the same
failure mode, relocated.

**The elegant form:** network reachability is a property *of an element type*, and belongs on
`ElementType`, next to the tag and the factory it already carries. Add a required constructor
argument — `Reach.NETWORK` or `Reach.LOCAL_ONLY`, no default, no overload without it. Then:

- `ElementRegistryTest`'s existing totality check (grand-architecture §10 already cites it as an
  enforced boundary) makes IC-6 free *by construction*: a new type cannot be registered without its
  author stating reachability, and stating nothing does not compile.
- `ElementVocabulary` shrinks to `registry.stream().filter(NETWORK).map(tag)` plus `WireEnd` — the
  gate `requireAllowed` stays exactly where it is, with exactly the same signature and typed
  `OpRejected`. Nothing about the collab call sites changes.
- `docs/collab-vocabulary.md`'s list becomes generated, so it cannot go stale the way it just did.
- `TestGen`'s own javadoc ("cannot be created by the editor") becomes machine-readable rather than
  prose someone has to notice.
- #212's future invariant `A ∩ D = ∅` (discovered external types never network-reachable) also
  falls out: a discovered descriptor is constructed by a provider, and the provider is not permitted
  to pass `NETWORK`. Today that invariant needs its own bespoke ratchet test.

**I am explicitly disregarding IC-5 as imported.** Under this shape there is no deny list to empty,
so "observe the null test go red with Δ emptied" tests nothing. Its replacement is stronger and
cheaper: flip one descriptor's `Reach` to `NETWORK` in a test fixture and assert the ratchet fails;
register a new descriptor without touching `jls.collab` and assert the op naming it is rejected.

## B. Four caps in four classes is why the fourth one is hard to place.

`SecureLink.MAX_PAYLOAD_BYTES`, `CausalBuffer.MAX_PENDING`, `CircuitOpReader.MAX_IDS/MAX_STRING/
MAX_BLOCKS/MAX_LINES`, `ElementBlocks.MAX_BLOCK` — six constants, four classes, three packages, no
common owner. The consequences are visible in the tree:

- `CausalBuffer.offer` refuses at cap by throwing **`IllegalStateException`**, a raw runtime
  exception, on the flood path. #170's own Global Invariant 4 says hostile input must fail typed,
  "never with raw exceptions." The landed backlog cap already violates the invariant the issue
  wrote to protect it. Nobody noticed because no one object owns "what happens when a peer
  overruns."
- Open Question 1 ("rate-cap locus: `SecureLink` or the session layer?") only exists because there
  is no place where remote resource accounting lives. With a single `RemoteInputPolicy` threaded
  from the session — byte budget, backlog budget, structural budgets, rate budget, and the rejection
  counters — the question dissolves; the answer is "in the policy," and the transport-swap concern
  that motivates the recommended default evaporates.
- Two of the four `planned_tasks` collapse into one. The misbehavior policy is not a separate
  feature: rejection counters and a disconnect threshold are just the policy object's other methods,
  and every rejection already flows through it.
- §7's re-planning clause "audit every collection reachable from frame handling" becomes a
  structural property (every remote-facing collection is constructed with a budget) rather than a
  manual audit performed after RSS growth is observed.

This is a smaller change than it sounds: the constants keep their values, the ratchets keep
ratcheting, and the tests keep asserting boundaries. What changes is that they have an owner.

## C. The deepest seam: stop shipping element bytes over the wire.

`AddElements` is `record AddElements(List<String> blocks)` — peer-controlled **save-format text**,
handed to `ElementBlocks.load`, which runs it through `Circuit.loadElement` and the `Attribute`
`setValue` protocol of whichever of the 34 element classes the tag names. The allowlist chooses
*which parser* a peer may reach. It says nothing about what the peer may feed that parser.

So H1, at full strength, buys: the remote parse surface equals the local file parse surface, minus
`TestGen`. That is a real gain (the P1 `TestGen` witness is genuine), but it is one type of
narrowing on a surface that the issue's framing implies is closed. The honest statement of the
current posture is "network input is as trusted as a file the user chose to open, minus one type" —
which is a strictly weaker claim than §1's "any byte a peer sends either means a validated circuit
operation or dies as a typed rejection."

**The route that makes the problem disappear:** ops carry *typed attribute values*, not serialized
blocks. `Attribute` (#52) is already the declarative, typed, validating description of every
persisted element parameter; `ElementType.create` already builds a blank instance. An
`AddElements` that carries `(tag, Map<attribute, typed value>)` lets the receiver construct the
element locally through validators that already exist, and **no peer-controlled text ever reaches
the loader**. Consequences:

- The vocabulary becomes closed *by type* rather than by list — the thing #170 says it wants.
- The P2 fuzz corpus changes from "mutate save-format text and hope only typed rejections escape"
  to "drive arbitrary typed values through `Attribute` validators" — a smaller, sharper corpus that
  is *also reusable by the file loader*, strengthening #38 and #160 instead of paralleling them.
- The snapshot slice (currently blocked on #169) mostly stops being a security problem, because a
  snapshot becomes a stream of the same typed ops rather than a circuit file from a stranger.

Related, and worth naming because it is the reason the vocabulary cannot close today:
`CircuitOp.apply(Circuit, Graphics)` means an op is an *editor command* retrofitted as a wire
protocol. #337's TASK-0037 is treated as a prerequisite chore; it is really the seam. The clean cut
is two types — an editor command (rich, geometry-aware, local) and a wire op (data-only, versioned,
typed) — with a codec between them owned by `docs/collab-vocabulary.md`. Then "closed vocabulary"
is a property of a type, not of a promise plus a grep.

## Program alignment — one thing worth saying out loud

ARCHITECTURE.md and grand-architecture.md §9 refuse speculative surfaces with unusual discipline:
plugins gated on real user demand, out-of-process isolation *reserved*, a second simulation strategy
declined pending a named trigger, i18n declined for want of a requesting instructor, macOS signing
and GPG signing forgone on cost/custody grounds. Collaboration is the one trajectory in the document
with no demand gate — and #170 is the line item that shows what it costs: a permanent
adversarial-security obligation (fuzz lane, rate limits, misbehavior policy, disconnect attribution,
protocol versioning) on a single-maintainer educational tool, sized larger than the simulator core.

The out-of-the-box alternative that serves the same pedagogy at a fraction of the surface: JLS
already writes plain-text saves (`-savetext`, sold in the README as the version-control path). A
three-way merge driver for that text — plus an instructor-hosted or repo-mediated share — gives lab
partners collaborative circuit work with **no untrusted-peer surface at all**, no AEAD, no SAS, no
CRDT, no misbehavior policy. That is not #170's call to make, and I am not recommending #163 be
descoped on this review's authority. But #170 is the right place to record the price, and if #163
ever faces the demand gate every other speculative surface in this project faced, this issue is the
number that should be quoted.

Note that reframings A and C are worth doing *even if collaboration ships exactly as planned*: both
harden the file loader and the #212 provider boundary as a side effect, which the current
collab-local design does not.

## What I would keep unchanged

- The P2 fuzz lane, including the RSS bound restored on 2026-08-02 — that bound is the only criterion
  in the issue that can falsify the cap design rather than confirm it. Build it over the shared
  reader (files and network), not frames alone.
- The ratchets, and the vanished-evidence discipline. Recording a phantom branch as void rather than
  quietly re-opening the checkbox is the healthiest thing in this issue's history.
- `LoopbackTransport` as the delivery vehicle for every remaining test.

## Ordered next steps under the reframing

1. Fix `CausalBuffer.offer` to fail typed. It is a live violation of this issue's own invariant 4.
2. Move reachability onto `ElementType` (reframing A); delete the duplicate list, the imported deny
   list, and IC-5 as written; regenerate `docs/collab-vocabulary.md`'s table and its stale
   `HelpTopicsTest` reference.
3. Introduce `RemoteInputPolicy` (reframing B); land the rate cap and the misbehavior counters into
   it as one slice, closing Open Question 1 by dissolution.
4. Re-scope the fuzz lane over the typed-attribute path once #337's TASK-0037 lands (reframing C),
   and record whether `AddElements`-carries-text survives that review.
