# Issue #557: FEAT-C28-3: a published performance number cannot silently go stale — the scheduled lane re-runs the suite under TASK-0026's ceiling bands and turns red before the doc lies
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-28 (#512) diagnoses an *epistemic* deficit, not a speed deficit: JLS scored 2/5 on
scale/perf "for lack of receipts, not lack of speed." The whole capstone is about receipts.
#554 builds the instrument, #555 publishes the claim, #560 measures the competitor, and #557
is meant to be the thing that keeps the claim true over time.

So the real goal of #557 is: **a published number can never quietly become false.** That is a
statement about the *doc*, not about a CI runner. The issue then reaches for the only
mechanism in view — #442's ceiling bands on a schedule — and in doing so cuts the seam in the
wrong place.

## The load-bearing objection: the lane cannot measure the quantity the doc publishes

`docs/performance.md` will publish a machine-stamped figure — keystone-c already has the
shape: **0.742 s of `runEventLoop` for 2,331,793 events on `k2000.jls`, 318 ns/event,
8,090 cycles/s**, with a stated environment. That number is a property of *(engine, fixture,
JDK, that machine)*. A GitHub-hosted `ubuntu-latest` runner is a different machine, and a
heterogeneous fleet at that. The scheduled lane therefore measures a different quantity than
the one in the doc, and no red X on the lane licenses the sentence AC-3 demands — "published
number X is now stale."

The arithmetic makes it worse. #442's own O4 measured a **4.17× ns/event spread across three
reps in one JVM on one machine, with no code change**, and its Threats section calls that a
*lower bound* on shared-runner noise. A ceiling with enough headroom to survive that spread
passes a genuine 2× engine regression without blinking. And #442 has already pre-authorized
demoting the timing assertion to a *reported* number on its first false failure (H2's
falsification clause, Open Question 3). **#557 builds its entire outcome on the one assertion
its own dependency expects to demote.** When that demotion happens, AC-1 evaporates and the
lane becomes a green rectangle — which #442 itself names as worse than no gate.

I am therefore **disregarding AC-1 as written.** "A scheduled lane fails when a measured value
regresses beyond its ceiling band" is not achievable at a signal-to-noise ratio that makes it
mean anything, and pursuing it produces a gate that is either flaky or decorative. The Outcome
sentence and AC-3/AC-4 survive intact and are worth building.

## Reframing A (primary): split staleness into a deterministic part and a noisy part

A published performance claim is a tuple, and only one component of it is noisy:

| Component | Example | Machine-invariant? |
|---|---|---|
| fixture identity | `k2000.jls`, sha256 | yes (exact) |
| node census | 1551 elements, 297 wireNets, maxBits 32 | yes (exact) |
| clocking regime | testgen-vector, 6004 cycles | yes (exact) |
| retired/posted/dup-suppressed events | 2,331,793 / 2,596,496 / 264,702 | yes (exact — #442's H1) |
| max queue depth | 12,093 | yes (exact) |
| seconds | 0.742 s | **no** |

Every row but the last is an integer that a *fast-lane, per-PR* test can assert with
`assertEquals` and zero flake. If any of them moves, the doc's description of what was
measured is false — which is the majority of the ways a published number actually goes stale
(a fixture edit, an element added, an engine change that alters the event count, a regime
change). That check is cheap, exact, and names the offending published line directly.

Concretely, the net-new artifact #557 should own is a **`PublishedNumberProvenanceTest`**:
parse the numbers block of `docs/performance.md`, and assert bidirectionally against
`test/fixtures/simulation-budget.properties` that (a) every published figure has a backing row
with matching fixture hash, regime, census and event counts, and (b) no performance claim
exists in README/docs without a backing row. The tree already has the exact precedent —
`test/jls/ExtensionPointCatalogTest.java` cross-checks `docs/extension-points.md` against code
constants in both directions, including an "undocumented set must be empty" assertion. Reuse
that shape verbatim.

This also picks up a criterion that currently has **no mechanism anywhere**: #555's AC-4 ("no
public performance claim exists in README/docs that the doc does not back") is pure prose in a
doc-writing feature. It is a test, and this is the issue that should write it.

## Reframing B: make seconds dimensionless before you gate on them

For the residual timing question, do not assert wall-clock on a runner. Run a fixed
calibration kernel (allocation-free integer loop plus an allocation-rate probe) **in the same
JVM, in the same run**, and publish and gate the *ratio* `ns_per_event / ns_per_calibration_op`.
That is a dimensionless number that survives a runner-fleet change, and it is the exact
pattern #442 O7 already identified in-tree: `SpatialIndexTest` measures scan and index in one
run, asserts the ratio-adjacent invariant, and merely *prints* the timings. A 2× engine
regression moves the ratio; a slower runner does not. This is the different architectural seam
worth cutting along, and it costs one extra kernel in the suite (#554's command) rather than a
new gate.

## Reframing C: a doc that is regenerated cannot be stale

The deepest version of the reframing: stop *guarding* the doc and start *emitting* it. #554
AC-4 already requires machine-consumable output. Let the numbers block of
`docs/performance.md` be generated from that output, and let the scheduled lane
**re-run and open a PR with the regenerated block plus its environment stamp**, rather than
turning red. Staleness then becomes structurally impossible — the worst case is one cadence of
lag, and every drift is a reviewable diff with a delta, not a red X someone must interpret.
The honesty guard KC-28-1 wants is preserved by labelling/failing the *PR* when the delta
exceeds the band, so a bot can never silently walk a number backwards. This converts the
lane's output from "something is wrong somewhere" into "here is the new number and here is how
far it moved," which is strictly more information and cannot rot.

## Alignment with the project's arc

The direction is right and the project needs it. But note what the issue actually adds once
its own boundary notes are honored: #442 owns the bands and the properties file, #378 owns the
long-run lane and its cadence, #554 owns the command, #555 owns the doc. Subtract those and
#557's residue is one properties row, one `@Tag("longrun")`, and a workflow trigger — a
config change, not a **1–2 mw** feature. The band is overstated by roughly an order of
magnitude *for the work as scoped*; it is about right for the work as reframed, because
the provenance test and the calibration ratio are real engineering.

One further arc observation, from #512's own 2026-08-08 correction: the instrument
(`KernelProbe`, `KernelProbe2`, `Census`) is **not in the tree**, and four engine tasks
(#442, #476, #393, #879) are each waiting on a measurement they have no committed way to take.
The highest-leverage move in this chain is committing that instrument (#554/#726), not adding
a lane on top of it. #557 should be sequenced explicitly *after* the instrument exists and
should not be allowed to grow a private probe of its own — the failure mode #554's boundary
comment names.

## An out-of-the-box alternative the issue never considers

The strongest defence against a stale published number is not one noisy runner — it is
**every reader**. JLS already ships a multi-arch headless container. A committed benchmark
entry point reachable as `docker run --rm ghcr.io/anadon/jls <bench invocation>` lets any
instructor, reviewer or competitor reproduce the number in one line with no JDK and no build,
printing "your machine: X; published: Y; ratio Z." That directly satisfies CAP-28 AC-2
(independent reproduction, today the criterion with the weakest mechanism), turns the user
base into the regression detector, and gives #560's head-to-head a harness others can rerun.
Caveat to respect: the CLI flag table is a stability contract (`docs/batch-interface.md`,
`CliFlagTableTest`) — so ship this as a documented script/entry point explicitly *outside*
that contract until it has settled, rather than minting a fifteenth flag.

## What I would keep verbatim

- The Outcome sentence. It states the right goal.
- AC-3 (a failure names the at-risk published number) — the provenance test satisfies it
  precisely, and better than a timing band could.
- AC-4 (ceilings only, faster never goes red) — non-negotiable and correctly stated.
- AC-2 (consume #442's machinery, do not build a second one) — and extend it: do not build a
  second *measurement path* either.
