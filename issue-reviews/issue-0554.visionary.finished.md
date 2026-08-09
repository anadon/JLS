# Issue #554: FEAT-C28-1: the measurement behind every public number is a committed, documented benchmark suite — with fixtures below CPU scale, tracked in-tree, that outlives the deletion of `riscv/`
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Not speed. The capstone (#512) says it outright — the survey scored JLS 2/5 "for lack
of receipts, not lack of speed", and KC-28-1 mandates publication even if JLS loses 5×.
So the deliverable is not a benchmark; it is an **instrument plus a provenance chain**,
and it belongs to the same project arc as `bom.json`, `.buildinfo`, cosign signatures,
`docs/reproducibility.md`, the normative specs, and the ratchet tests. JLS's actual
differentiator is not being fast, it is being *checkable*. Read that way, three things
about the issue as written are off-axis, and one is a missed opportunity large enough to
change what gets built.

## Reframing 1 — the instrument should be a shipped CLI capability, not a benchmark harness

The issue promotes `riscv/bench_kernel.py`'s *measurement* into a *benchmark suite*.
That keeps the instrument in the benchmark's private world. The better seam is one layer
down: put the measurement in the product, under an existing contract.

JLS already ships a headless, contract-stabilised batch surface
(`docs/batch-interface.md` §6: "any change to them requires a CHANGELOG entry and either
a major version bump or a compatibility flag"), and a container image
(`ghcr.io/anadon/jls`) whose stated audience is autograders and CI. Add a run-record
output to that surface — a file flag in the shape of `-vcd`, e.g.
`-stats run.json`, written to a file rather than stdout so §1's "stdout carries *only*
the simulation results" is untouched — carrying:

- census (logic elements, wire nets, max width, per-tag histogram — the exact projection
  #413 §7.10 defines, so census and measurement stop being two artifacts),
- event counts by payload and by callback, dup-suppressed count, max queue depth,
- **phase timings separated**: `initSimulation` vs `runEventLoop`,
- simulated cycles, sim-time span, wall time, jar version, JDK, flags.

Then the "benchmark suite" is a thin loop over fixtures invoking the shipped command,
and AC-4's "consumable without hand-editing" is satisfied by a documented stability
contract instead of a private format that only the suite understands.

What this buys beyond AC-4:

- **The four internal consumers get the same instrument for free.** The adversarial
  comment names #442 (event-count equality), #476, #393, #879, each about to hand-roll a
  probe. A `test/` harness serves them; a CLI record serves them *and* survives a rewrite
  of the test tree.
- **Event count is a pedagogical and grading metric, not just a perf metric.** "How many
  events did your adder take to settle" is a direct, gradeable measure of glitch and
  hazard activity — exactly the thing keystone-c §9.1 says JLS currently teaches
  *wrongly*. No competitor publishes this. A capstone filed defensively ("Digital
  publishes a number and we do not") comes out of it with an offensive capability.
- It removes the last Python from the measurement path (see the constraint below).

Cost, honestly: one `FlagSpec`, `CliFlagTableTest`, a help topic + `Map.jhm` +
`JLSHelpTOC.xml` row, a `docs/batch-interface.md` section, a CHANGELOG entry, and a new
promise you cannot break silently. That is a real tax and it is the right one — the
alternative is a format with the same downstream consumers and none of the discipline.

## Reframing 2 — every measured fixture must also be a correctness golden

AC-3 asks for "2-3 smaller standard circuits (e.g. a counter and a memory loop) added as
tracked fixtures". Minting perf-only circuits reintroduces the failure that
`bench_kernel.py` explicitly guards against in its own header: it prints the reference
emulator's result beside the circuit's *"so a run that is fast because it stopped
computing is visible rather than silent."* A benchmark-only fixture has no oracle, and a
regression that breaks it will read as an improvement.

Better: **the measured set is a declared subset of the existing golden fixtures, with a
census attached.** The tree already has `BatchSimulationGoldenTest`,
`SequentialGoldenTest`, `ElementSimulationGoldenTest`, and — decisive for the CPU-scale
end — `test/fixtures/riscv-sum1to10.jls` (120 KB, 1551 elements) already driven
in-process through `BatchSimulator` by `RiscvCpuGoldenTest`. That test is the shape the
suite should take: load, `setTestFile`, `runSim`, assert against an oracle — now also
emit a record. Then "fast because it stopped computing" is structurally impossible, and
the suite costs no new circuits to maintain.

## Reframing 3 — publish the record, not the prose

`docs/performance.md` (#555) transcribing numbers by hand is the weakest link in the
chain: it is exactly the artifact that goes stale silently, and #557 (ceilings-only)
will not catch a doc that is merely *out of date*. The project already solved this class
of problem twice — `CliFlagTableTest` for flag docs, `HelpTopicsTest` for help
coverage.

Concretely: ship the run record as a **release artifact** beside `SHA256SUMS`,
`bom.json` and `.buildinfo`, stamped with commit, jar sha256, **fixture sha256**, JDK
build, flags and CPU model. Generate `docs/performance.md`'s tables from it. Then add a
drift ratchet — a test that scans README and `docs/**` for number-shaped performance
claims and fails on any that is not present in the committed record. Without such a
test, this issue's headline claim ("this suite is the sole producer of every performance
number JLS publishes") is unenforceable prose; with it, it is a property of the build,
and #555's AC-2 ("an independent party reproduces from the doc alone") stops depending
on a human keeping two files in sync.

## The unstated constraint that will bite: no Python

#413/D5 is explicit — *"after this task nothing in the tree consumes Python"* — and this
issue orders after it while never saying what the suite is written in. Promoting a
Python harness into a committed Python harness discharges AC-2 and violates D5 in the
same commit. State the mechanism in the body. The project's own precedent is right
there: `scripts/wayland-rig.sh` + `scripts/wayland-rig-selftest.sh`, mirrored for macOS
and Windows — a rig plus a self-test that drives the *unmodified rig* against a stub and
asserts the contract without needing the real environment. A `perf-rig.sh` +
`perf-rig-selftest.sh` pair does the same job here, and the self-test is the answer to
keystone-c §8.1's "a timing assertion in CI is a flake factory": the record's *shape*
is asserted deterministically, the numbers are merely produced.

## Two smaller corrections

**AC-1 conflates two independent axes.** It asks for events/s and cycles/s "at stated
node counts", but the only scaling data on master — k500/k1000/k2000, 1504/3004/6004
cycles — is a *run-length* sweep at constant node count (1551 elements throughout).
Circuit size and run length are orthogonal; publishing a length sweep as if it were a
size sweep states a scaling shape JLS has never measured. The record should carry both,
from independent sources (census for size, vector for length), and the doc should never
divide one into the other implicitly.

**The ordering edge to keystone-c §8.2 is missing, but should not become a block.**
`initSimulation` is quadratic today (`SigSim` `String +=`, 0.568 s vs 0.742 s of loop at
k2000) — 43% of a naive wall-clock figure is a string concatenation. Do *not* order this
issue after the fix: the suite is what proves the fix worked. Order the **publication**
after it, and let phase separation (Reframing 1) make that safe — after stage −1 the
record shows setup collapsing and the loop unchanged, which is a far better first public
story than a single number that moves for reasons nobody can attribute.

## One place this pulls against the arc

`ARCHITECTURE.md` records the interpreter as JLS's sole simulation strategy with the
revisit trigger *"a concrete CPU-scale design … that is unusably slow interactively"* —
untestable, as keystone-c §2 says outright. It is inverted to publish a quantitative
number to strangers while the project's own strategy gate stays a feeling. The same
record should make that trigger testable (e.g. "below 10 kcycles/s on the CPU golden"),
and this issue should claim that as an outcome. One instrument, two audiences; otherwise
someone files a third measurement path for the internal gate and the thing this capstone
exists to prevent happens inside the repo.

## What I would not do

Do not add JMH (the project records hand-rolled best-of-N as its precedent and carries
no benchmark dependency), and do not reach for a third-party trend-charting GitHub
Action to discharge #557 — pinned-action and Scorecard posture makes that a supply-chain
cost out of proportion to a chart. Borrow a conventional JSON shape; do not import the
consumer.

## Verdict

**endorse-with-reframing.** The outcome is right and the boundary work in the first
comment is careful and correct. Keep the acceptance criteria, but move AC-4's output
contract into the shipped batch interface, replace AC-3's new circuits with censused
existing goldens, name the non-Python mechanism, split the size and length axes in AC-1,
and add one criterion the issue is missing: the record is published as a provenance-
carrying artifact and a ratchet test enforces that no public number exists outside it.
That last one is what converts "sole producer" from an intention into a property.
