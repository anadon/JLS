# Issue #735: TASK-C557-1: a scheduled lane re-runs the published workloads under TASK-0026's ceiling bands — ceilings only, so getting faster never turns it red
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machinery and #735 is one sentence: *a number JLS says in public must keep
being true after the person who measured it stops looking.* That is CAP-28's (#512)
whole premise — the survey scored JLS 2/5 on scale/perf "for lack of receipts, not lack
of speed", and the deficit is epistemic. #735 is the only piece of the chain that runs
after the humans leave, so it is the piece that decides whether CAP-28 produced a
receipt or a snapshot.

Judged on that, the issue gets the two hardest calls right. It refuses to build a second
band mechanism (#442 owns them) and it refuses to decide lane policy (#378 owns that).
Both refusals are the project's "do not build it twice" clause applied correctly, and
they are the reason this issue is small.

But the frame it inherits is #442's, and #442 is a *regression detector for an internal
constant under a 4.17x in-JVM noise floor*. #735 is a *truth maintainer for a public
claim*. Those are different predicates, and three consequences follow.

## Reframing 1 — staleness is two-sided; this lane can only see one side

I am explicitly disregarding acceptance criterion 3 as the whole failure model. As a
statement about the *gate* it is right and I would not change it: no floor, no red on
improvement, no reflexive re-baselining. As a statement about *staleness* it is
backwards for this project's own trajectory.

`docs/capability-roadmap/keystone-c-performance.md` — the source of every number #555
will publish — predicts the engine getting materially faster: 37.6% of the loop in
`BitSet`, 47.7% in `PriorityQueue` + `dupCheck`, "no slower, expectation 15-25% faster"
for the value change alone (#232/#878/#879), plus #476 and #393 on the queue. The
published anchor is 318 ns/event and 8,090 cycles/s on `k2000`. On the roadmap's own
arithmetic, the most likely way `docs/performance.md` becomes false is that JLS gets
*faster than the doc says* — and a ceilings-only lane is designed to stay green through
exactly that, forever.

CAP-28 AC-2 is the criterion that notices: *"an independent party can reproduce the
number within a stated tolerance."* A tolerance is two-sided. AC-4 (regression → red) is
what #735 implements; AC-2 has no owner anywhere in the chain, and #735 is where it
should live.

The fix costs almost nothing and does not reintroduce flakiness, because the two
directions deserve different *consequences*, not different gates:

- measured worse than the ceiling → **red**, as written;
- measured better than the published point value by more than the doc's stated
  reproduction tolerance → **not red**; the lane opens a PR (or comments on #512)
  carrying the regenerated results file and the diff to `docs/performance.md`.

A false "you got faster" costs a reviewed PR that gets closed. A false "you got slower"
costs a red build, which is why only one of them may gate. The asymmetry is the design.
Ceilings gate; improvements notify. Nothing about #442's band mechanics changes.

## Reframing 2 — cut the seam at the results file, not at the doc

AC-1 says the lane runs "exactly the workloads whose numbers are published in
`docs/performance.md`". That is a set equality between a CI configuration and a Markdown
file, and the issue does not say how it is checked. #442's own O3 is the cautionary
tale: an enumerated golden list drifted from the discoverable set, six against eight,
and the corpus document would have been copied verbatim into the gate.

The sibling that already fixes this is #730 (TASK-C554-3): a documented machine-readable
results file, from which "`docs/performance.md` is generated or checked". Take that
seriously and the enumeration problem disappears rather than being solved:

- the **results file** is the single artifact of record and the workload manifest;
- `docs/performance.md` is a *rendered view* of it, checked byte-identical in the fast
  lane (the project already runs eight `*GoldenTest` byte-identity suites — this is one
  more golden, costing nothing);
- this lane iterates the manifest, not the prose.

AC-1 should then read: *the lane runs every fixture the results file declares, and fails
if the committed `docs/performance.md` is not the rendering of the results file it just
produced.* Same outcome, mechanically checkable, no second enumeration.

This also mostly deletes TASK-C557-2. "Naming the stale published number on failure" is
hard when a band row and a doc paragraph are separate artifacts joined by a human; it is
free when the doc line and the band row are two projections of the same record. The
boundary #735 draws to protect a sibling task is a boundary that exists only because the
seam was cut in the wrong place.

## Reframing 3 — bind the gate to the release, not to a Tuesday

Staleness is a property of a *published version*, not of a cron tick. `release.yml` is
718 lines and already gates what the public receives; `docs/performance.md` is a claim
attached to a release. The strongest, cheapest version of this issue's outcome is:
**a release cannot ship with a doc the suite does not reproduce.**

The project's own history argues for this. `.github/workflows/ci.yml`'s nightly runs
*only* `gui-wayland` — every other job carries `if: github.event_name != 'schedule'` —
and `mutation.yml` is weekly and never required. Both are useful; neither blocks
anything. `longrun` still appears nowhere in the tree, so #378's lane does not exist
yet. A scheduled lane whose red lands in no one's inbox is the same failure #442 names
for reflexively re-baselined gates: green-looking protection that protects nothing.

I would keep the cron — AC-1's "not per-PR-only" is right, early warning is worth having
— but make the cron the *notifier* (it files, it does not merely go red) and make the
release the *gate*. That is a real guarantee for a fraction of the vigilance.

## Reframing 4 — publish the integers, and most of this lane's hard job evaporates

The hardest thing #735 is signing up for is defending a wall-clock number on shared CI
runners, against a noise floor #442 measured at 4.17x in a single JVM. Consider not
making wall-clock the load-bearing claim.

The keystone measurement contains a quantity that is bit-exactly reproducible by anyone,
on any machine, forever: **2,331,793 events retired for 6,004 clocked cycles** on the
`k2000` workload, with the census (`elements=1551 wireNets=297 maxBits=32`) and the
clocking regime. #442's O4 shows the same determinism at small scale (194 events, three
runs, exactly). Digital's "120 kHz on a 2012 i5" is a wall-clock brag; the honest and
more useful counter is *"this circuit, this stimulus, retires exactly this many events —
here is the machine we timed it on."* Work per unit of simulated computation is a
property of the engine; nanoseconds are a property of somebody's laptop.

If the published claim leads with the exact quantities and annotates the wall-clock
figure, then the *fast* lane defends most of the doc on every PR at zero noise (#442's
equality already does it), and this scheduled lane shrinks to the genuinely noisy
residue: the ns/event ceiling and the CPU-scale fixture's wall time. That is a smaller,
more durable issue than the one filed, and it makes CAP-28 AC-2 achievable for a reader
who does not own the runner's hardware — which, as written, it currently is not.

## Where this sits in the arc, and one tier question

The work strengthens the arc; it does not duplicate or pull against it. The one thing I
would question structurally: #735 has no separable outcome of its own. It is "invoke the
suite in CI and exit nonzero", banded 0.5-1 mw, ordered behind #442, #554, #555 and
#378 — a chain whose own prerequisites (#377, #379, #413, TASK-0015) run four to six
deep, and whose subject (`docs/performance.md`) does not exist in the tree. Meanwhile
the adversarial finding on #512/#554 records that the *instrument itself* — `KernelProbe`,
`KernelProbe2`, `Census` — is not in the repository at all, so nothing yet produces the
numbers this lane would compare.

That ordering is not a defect in #735; it is evidence about tier. CI invocability is not
a separate outcome from a benchmark suite, it is a design constraint on one, and
splitting them is how a suite gets designed for a human at a terminal and then retrofitted
for CI. The #554 comment already warns about this for the output shape (phase timing and
the event census, retrofitted, means re-running every published number). I would fold
#735's substance into #726/#730 as acceptance criteria — "the suite has a comparison
mode that exits nonzero on band breach and prints the offending fixture" — and keep a
separate issue only for the workflow file and its schedule, which is genuinely #378's
territory anyway.

## Verdict

**endorse-with-reframing.** The end is right and centrally important: a public number
that cannot silently rot is the difference between CAP-28 shipping a receipt and shipping
a screenshot. The refusals to duplicate #442's bands and #378's lane policy are correct
and should stand verbatim. What I would change before execution: make the doc a rendering
of the results file rather than its peer (which deletes AC-1's unchecked set equality and
most of TASK-C557-2); keep ceilings-only as the *gate* but add an improvement *notifier*,
because AC-2's tolerance is two-sided and this project's roadmap predicts staleness in
the direction the gate is blind to; bind the blocking check to the release rather than to
a cron; and lead the published claim with the deterministic event counts so the noisy
part of the lane is the residue rather than the whole thing.
