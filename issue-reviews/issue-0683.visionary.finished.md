# Issue #683: TASK-C350-6: going multi-host is a worker-count change, not a second implementation — the same campaign produces the same bytes across hosts
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

The end is right and I want to keep it: a campaign of thousands should stop being
bounded by one machine, and it should get there without forking the runner into a
"local" and a "grid" implementation that drift. Everything below keeps that end
and replaces the mechanism, the dependency, and one acceptance criterion.

The mechanism the issue picks — "substitutes a remote worker source behind the
runner's existing job interface", ordered after #333 — is the one place in
FEAT-057 where the feature stops being what #350 says it is. #350's own rationale
for putting this scope last is that building it first "would make the feature
depend on the transport, which destroys the property that makes it the demo
slice." Ordering it last does not remove that dependency; it only defers it. When
#683 lands, FEAT-057 acquires #333 — a feature `blocked_by: [318, 332, 348, 363]`
whose own body admits its cost band is "unvalidated by decomposition" and whose
criterion 8 rests on a cross-platform determinism experiment that has never been
run. The lightest scope in the capstone would be gated on its heaviest.

## The reframing: campaigns shard; they do not dispatch

#679 already commits the aggregate to "a fold over the description's job order
with each job's result **looked up** rather than streamed." That single sentence,
taken seriously, is the whole feature. If the aggregator is a pure function of
(description, artifact store), then it cannot observe a worker, a host, a socket
or a schedule — because none of those are in its argument list. Byte-identity
between a local run and a grid run stops being a test to run against a grid and
becomes a property of the type signature.

So build the machine boundary out of the description, not out of a runtime:

- `jls -campaign run camp.jlsc --shard k/N --out DIR` — executes the jobs whose
  index in **description order** is congruent to k mod N, through the existing
  batch surface, writing artifacts under #677's injective naming. The local
  multi-core runner (#676) is exactly `--shard 0/1` with a worker pool. The shard
  is a description-order fact, so it is reproducible and reviewable.
- `jls -campaign aggregate camp.jlsc DIR...` — the #679 fold, unioning artifact
  directories in description order. No network, no clock, no thread pool, no
  worker id anywhere in the process image. A job with no complete artifact in any
  DIR is a failure row (#681).
- `docs/campaign-grid.md` — one page of recipes: a Slurm array, GNU parallel over
  `ssh`, `docker run ghcr.io/anadon/jls` per host, a GitHub Actions matrix. The
  "transport" is `scp`/`rsync`/a shared mount, which every target environment
  already has.

Under this framing #683's headline claim gets *stronger*: multi-host is not "a
worker-count change" — **there is no multi-host code path at all**. AC3 ("a
campaign still runs with no distributed transport in the path") becomes
structurally unfalsifiable rather than something to re-check, and AC2 ("no
parallel implementation of collection or aggregation exists") is guaranteed by
construction because collection and aggregation are one program that runs once,
after everything, on a directory.

This is also the shape the project already ships to its users. The README's
autograder story is `docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls -b -t
tests circuit.jls`, and `docs/batch-interface.md` is a declared stability
contract. The seam that already exists — a headless CLI over a documented
contract, in a multi-arch container — is the distribution mechanism. #683 as
written walks past it to build a job scheduler.

## Why #333's transport is the wrong pipe specifically

`src/jls/collab/net/Transport.java` is "one bidirectional, in-order channel of
opaque frames between **two** collaboration peers", with a
`SecureLink.MAX_PAYLOAD_BYTES` cap, an SAS/identity handshake and `KnownPeers`
trust state — a two-party interactive editing session. A campaign wants N-way
fan-out and wants to move VCD dumps that can be large. Worse, the socket
confinement rule (`test/jls/ArchitectureRulesTest.java:249`,
`socketEndpointsAreConfinedToCollabNet`) means the campaign runner would either
have to live behind `jls.collab.net` or push batch-side concerns into the one
package with a security surface — for a fleet of independent processes that never
need to talk to each other. Reusing a transport is thrift; reusing *this*
transport for embarrassingly parallel fan-out is a category error, and it makes
the reviewers of #170's hostile-input discipline responsible for a grading
feature.

## The claim AC1 actually tests, and the better feature hiding inside it

"The same campaign, run locally and across hosts, produces byte-identical output"
does not test whether there is a second implementation. It tests whether **the
simulator is bit-deterministic across environments** — and that is a claim this
project has never verified. `docs/batch-interface.md` §4 says only "two identical
runs produce identical bytes"; that is one machine, one JVM. Meanwhile
`release.yml:212` publishes `linux/amd64,linux/arm64,linux/riscv64` and the
README documents JDK 25+ as the floor with an advisory lane on the newest GA
release. On a homogeneous grid (same jar, same image digest) AC1 is trivially
true and proves nothing; on a realistic heterogeneous grid it is *unknown*, and
#683 would be where the project discovers a divergence — at the last task of the
feature, with a REPLAN cascading back through #679's and #333's byte-identity
language.

Note also that #333's evidence for this gap is `docs/parity-contract.md:469-477`,
and that file **does not exist anywhere in the tree at HEAD** (`grep -rl
parity-contract`, excluding `issue-reviews/`, returns nothing). The one piece of
prior art #683 inherits is a dangling citation.

That is an opportunity, not just a hazard. Reframe #683 as **the cross-platform
determinism instrument**: a campaign whose shards run in the already-published
amd64 / arm64 / riscv64 container images, aggregated once, is precisely the
experiment #333 criterion 8 wants, that #265 arms, and that
`docs/reproducibility.md` and #184/#185 stop one step short of — they cover the
jar's bytes and say nothing about a run's bytes. JLS has a strong, unusually
mature reproducible-*build* arc; the missing half is reproducible *runs*, and a
campaign runner is the cheapest instrument the project will ever have for it.
Do that with sharding and no transport and this task serves #312, #333, #265,
#369 and the grading corpus (#697/#724) at once, at a fraction of the band.

For the provenance that a heterogeneous run genuinely needs without violating
#350 invariant 1, copy the pattern the project already uses: the reproducible jar
ships alongside a `.buildinfo` that records the environment and is not part of
the artifact's bytes. Emit a per-shard `runinfo` sidecar (arch, JDK, image
digest) that the aggregator never reads. Provenance stays available; the
aggregate stays clean.

## AC4 belongs to #681, and moving it removes the last reason for this task

"A host that dies mid-campaign produces failure rows for its in-flight jobs" is
already #681's third criterion — it lists "missing expected artifact" as a
distinct, non-collapsed failure kind, and its denominator is the description's
job count. State failure accounting at the *artifact* level ("any job in the
description without a complete, well-formed artifact is a failure row naming
why") and host death, OOM, `kill -9`, a full disk and a lost network mount are
all the same row, with no liveness detection, no leases and no heartbeats. Left
in #683, AC4 is a second implementation of exactly the accounting this issue
exists to prevent duplicating.

With AC1 reframed, AC2 and AC3 made structural, and AC4 re-homed to #681, the
residue of #683 is: a `--shard k/N` flag, a directory union in the aggregator,
one recipe doc, and one cross-arch determinism run. I would file it as that, at
roughly 0.5 mw, with `ordering_after: ["TASK-C350-4", "TASK-C350-5"]` and **no
edge to #333 at all**.

## Where I am disregarding the stated criteria, and the honest objection

I am explicitly discarding AC1's framing (grid-vs-local byte-identity as evidence
of single implementation), AC4 entirely (to #681), and the `ordering_after` edge
to #333. I keep the outcome sentence.

The real cost of sharding is that static assignment straggles when job runtimes
are skewed — a dynamic dispatcher balances, a shard does not. Three answers, in
order of preference: within a host, #676's pool already steals; across hosts, let
the user's real scheduler balance (Slurm and HTCondor exist and are better at
this than JLS will ever be, and a Slurm array *is* `--shard k/N`); and if
cross-host stealing is ever genuinely required, the right mechanism on the
shared filesystem such a grid already has is atomic `O_CREAT|O_EXCL` claim files
in the artifact directory — still no JLS-owned protocol, still no second security
surface, still nothing for the aggregator to observe. None of those paths run
through a two-peer session transport.

Finally, this reframing partly dissolves #350's Open Question 1, which the
2026-08-08 REPLAN escalated as the live blocker. If the aggregate is a pure fold
over a directory of injectively named artifacts, then what the grading harness
(#697/#724) and the campaign runner must agree on is an *artifact store
convention*, not a scheduler and not a runtime. One side owning a pure function
is a far smaller thing to negotiate than one side owning a dispatcher, and it can
be settled without deciding which feature ships first.
