# Issue #697: TASK-C525-2: GradescopeCorpusTest grades all 300 fixture submissions inside a declared wall-time budget, with two runs identical
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Read up the chain, #697 is not really about Gradescope. It is one of four
sibling "corpus tasks" (TASK-C525-2 here, plus C526-1 / C528-2 / C530-2), each
running one vendor adapter over the same 300-submission corpus (#717) and
emitting a score vector, so that #719 can assert the four vectors are
byte-identical — CAP-21 (#502) AC-1. #697's own three assertions (corpus-scale
grading, a wall-time budget, two-run identity) are instruments in service of
that one claim, plus a secondary claim that grading a real cohort will not blow
a platform timeout.

Both ends are worth wanting. The instruments named are, I think, the wrong ones,
and they are wrong in a way that costs four times over because three siblings
copy them verbatim.

## The reframe: parity is a property of a pure function, not of four pipelines

CAP-21 §1 step 4 says the byte-identical vectors are "derived from the same
xUnit output". That sentence contains the whole design. Once PF-1's frozen
contract exists, each adapter is a transform

    (xUnit bytes for one submission, lab manifest) -> platform-native output

and the score vector is a projection of that output. Simulation — the only
expensive, circuit-dependent, potentially nondeterministic step — happens
*before* the adapter and is identical for all four. So the natural seam is at
the xUnit artifact, not at the container:

1. Run JLS batch once over the 300 submissions. Freeze the 300 xUnit artifacts
   as the corpus's second golden layer (#717 already owns golden score vectors;
   this is the same act, one stage earlier).
2. Run each of the four adapters over those frozen bytes — in-JVM or as a plain
   subprocess, no container, no JLS — and project each native output to the
   shared vector.
3. Assert the four projections equal.

That is 300 simulator runs total instead of 4 x 300 x 2 = 2400 containerized
ones, and it makes the parity failure *diagnosable*: with a frozen xUnit input,
a red #719 can only mean an adapter diverged. In the issue's architecture, each
adapter re-derives its own xUnit from its own container run, so engine
nondeterminism and adapter divergence land in the same red lane wearing the same
colour. The design as filed weakens the very claim it exists to make.

Note this is not a fringe optimization: today there is no `docker` invocation
anywhere in `/home/user/JLS/.github/workflows/ci.yml` or under `test/`. Four
containerized corpus lanes are net-new standing CI infrastructure, and #697 is
the issue that first commits to them.

## A second, cheaper instrument the project already knows how to build

If the adapter is a pure function of (xUnit bytes, manifest) — no clock, no
locale-sensitive formatting, no filesystem iteration order, no network, no
hostname — then two-run identity and four-way parity are corollaries rather
than experiments. JLS already enforces exactly this class of property
structurally: `HeadlessCoreRatchetTest` keeps AWT out of `jls.sim`,
`NotificationRatchetTest` keeps raw `JOptionPane` out of the tree,
`ExtensionPointCatalogTest` cross-checks a catalog both directions.

The missing test is `GradescopeAdapterPurityRatchetTest` (and its three
siblings, or one parameterized ratchet): the adapter's code touches no
`System.currentTimeMillis`, no default `Locale`/`Charset`, no `File.list`
ordering, no environment. That is a few hours of work, it runs in
milliseconds, it cannot flake, and it forecloses the loophole the adversarial
review flagged — an adapter engineered to emit only deterministic fields is
exactly what you *want*, provided it is proved rather than asserted.

## Disregarding AC-2 as written: the budget measures a quantity that does not exist

AC-2's stated purpose is that "a performance regression in grading surfaces as
a red lane rather than as a Gradescope timeout in a live course." But Gradescope
grades one submission per autograder container invocation, against a
per-submission timeout. No student ever waits on the sum of 300 submissions;
nothing in the deployment model has a cohort wall-time. A total-corpus budget is
a number about JLS's CI runner, not about the risk it names — and on shared CI
hardware a sum-of-300 threshold is the classic flaky-perf-gate shape: pad it
until it guards nothing, or watch it go red on runner noise.

The faithful budget is **per-submission worst case, expressed as a fraction of
the platform's documented timeout**: assert max-over-corpus (and separately, max
over #717's adversarial subset — the runaway loops and oversized circuits that
are the actual timeout risk) stays under, say, 25% of the documented limit.
That is a stable statistic (a max over 300 samples is far less runner-sensitive
than a sum), it is comparable across all four platforms with each platform's own
limit substituted, and it survives #717's corpus being resized by the
large-fixture policy — which, note, does not exist in the tree today, so any sum
declared now is provisional anyway.

## Disregarding AC-3 as written: "two consecutive runs" is the weakest possible test

Two runs back-to-back on the same runner, same container, same second, same TZ,
same locale, same paths — the failure modes that actually break byte-identity
are precisely the ones held constant. The project has already solved this
better and written it down: the `reproducibility` lane
(`/home/user/JLS/.github/workflows/ci.yml`, from line 798) does a same-runner
rebuild *and* an independent perturbed rebuild under a different workspace path,
`TZ=Pacific/Kiritimati`, `LC_ALL=C`, `umask 077` — issue #185's discipline. Ask
the corpus run for the same thing: second pass under shifted TZ, C locale,
different container hostname, different working directory, and submissions fed
in reverse order. Same cost, incomparably more signal, and it reuses an idiom
maintainers already trust. `/home/user/JLS/test/jls/DeterministicSaveTest.java`
and `Circuit.stateHash()` are the in-tree precedent for the same instinct at the
model layer.

## The arc: this work sits on the perishable half

CAP-21 is candid that the vendor adapters are a permanent tax (risk 1) and
individually disposable (KC-21-3: drop an adapter rather than track an
undocumented interface). The durable assets are PF-1's frozen contract, the
corpus, and the xUnit-to-vector projection — all four-platform-agnostic. #697
spends the single heaviest CI commitment in the capstone on the disposable half,
and three sibling issues will spend it three more times. Meanwhile the engine
determinism this buys is already covered cheaply in-JVM by
`BatchSimulationGoldenTest`, `SequentialGoldenTest`, `RiscvCpuGoldenTest`, and
`DeterministicSaveTest`.

The same single-producer argument the maintainer's own comment used to keep one
corpus applies to the harness: there should be **one** parameterized
`AdapterCorpusTest` in the fixture, taking a per-adapter descriptor (how to
invoke, where native output lands, how to project to the shared vector). Then
#719 is nearly free, a fifth platform costs a descriptor instead of a task, and
"extraction is defined once, in the fixture" (#719 AC-2) is satisfied by
construction rather than by vigilance.

## What survives from the issue as written

- The containerized end-to-end run is worth having **once, at n≈3** as a smoke
  and doc-test — that is #694/TASK-C525-3 territory and the honest scope of
  "the template works".
- AC-4's constraint (shared form, no after-the-fact per-adapter normalization)
  is the right instinct and gets stronger under the reframe: with a frozen xUnit
  input there is nothing left to normalize away.
- The Boundary's refusal to re-own the corpus or the parity assertion is good
  discipline and should be preserved through any re-cut.

## Verdict rationale

**rethink.** The end — a Gradescope adapter proved at cohort scale, feeding a
falsifiable four-way parity claim — is right and aligned. The means are not: the
seam is cut after the expensive stage instead of before it, the budget measures
a quantity the deployment model does not have, the determinism assertion is
ceremonial, and the whole apparatus is about to be written four times. Re-cut as
(a) one shared xUnit golden layer, (b) four cheap adapter projections under one
parameterized harness, (c) a per-adapter purity ratchet, (d) a per-submission
latency guard against each platform's documented timeout, and (e) one small
containerized smoke test. That is a smaller, faster, more diagnostic apparatus
that makes a *stronger* claim than the one filed.
