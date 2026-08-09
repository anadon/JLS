# Issue #726: TASK-C554-1: one documented command runs the benchmark suite and emits events/s and cycles/s per fixture, with each fixture's clocking regime recorded
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stated: a public benchmark suite for CAP-28's publication chain (#554 → #555 doc, #557
staleness lane, #560 head-to-head). That is the *smaller* half of its purpose, and
designing to it will produce the wrong instrument.

The larger half is visible in the tree. `docs/capability-roadmap/keystone-c-performance.md`
is the only surviving record of JLS's engine measurements — 318 ns/event, 8,090 cycles/s,
37.6% value work / 47.7% queue bookkeeping / 4.9% logic — and it stakes an entire
multi-stage engine programme on those numbers (§8's stage table: −1 through 3, each with a
predicted delta). Its §12 reproduction recipe points at
`/tmp/claude-0/…/scratchpad/bench/`, a path that no longer exists, and compiles ten
instruments (`KernelProbe`, `KernelProbe2`, `Census`, `NoDedup`, `ValueRep`, `Levelized`,
`Kernels`, …) that were never committed. `docs/machine-calibration.md`,
`docs/parity-contract.md` and `docs/plan/` — cited throughout #413 and #442 — are not in
this checkout at all. The single tracked instrument, `riscv/bench_kernel.py`, describes
itself in its own header as a scratch harness that "can be deleted", and D5 deletes it.

So: **every number the engine programme divides by is currently unreproducible, and
#726 is the recovery.** ARCHITECTURE.md:341-368 records "discrete-event interpreter is
the sole strategy" with a revisit trigger — "a concrete CPU-scale design … that is
unusably slow interactively" — that is prose, unfalsifiable, and keystone-c §2 already
asks for it to be restated quantitatively. #726 is what would make that decision
re-openable on evidence rather than on assertion. That is a far better reason to build
this than `docs/performance.md`, and it should drive the design.

## Reframing 1 — stop treating cycles/s as a measurement; publish its factorization

The issue's own principle ("an events/s number without its clocking regime is not a
measurement, it is a number") is right and does not go far enough. The project's evidence
base already establishes that throughput factors cleanly:

```
cycles/s  =  (1e9 / ns_per_event)  /  events_per_cycle
             \___ engine + machine ___/   \__ circuit + drive __/
```

`ns/event` is a property of the engine and the host. `events/cycle` is a property of the
circuit and its stimulus — #413 §7.10 derives it as α·β·|L(C)|, and #442 O6 records
**121.5, 243.1, 245.5 and 388.4 events/cycle on element-for-element identical circuits**
differing only in drive. cycles/s is their quotient and is therefore the *least*
transferable of the three: it moves when the machine changes, when the circuit changes,
and when the clocking regime changes, and a single figure cannot say which.

Reported as a product, the clocking-regime clause is a disclaimer bolted onto a number.
Reported as a factorization, the regime is **structural** — it is precisely what selects
the `events/cycle` factor, so the harness cannot emit a figure whose regime is unstated
without a hole in its own arithmetic. This also makes engine work legible: a stage that
halves `ns/event` and a stage that halves `events/cycle` (#393's zero-delay closure,
which removes 82% of postings) are different achievements that a single cycles/s figure
smears together.

**Concrete:** make `ns/event` and `events/cycle` the primary recorded fields, with
`events/s` and `cycles/s` derived and explicitly labelled as derived.

## Reframing 2 — derive the regime; extend the refusal to every way a figure can be a lie

AC-2 asks the harness to "refuse to report a figure whose regime it cannot state." If the
regime is a field a human types into a fixture manifest, that check is theatre — the
harness verifies that a string is non-empty. But the regime is *observable*: after
`Circuit.finishLoad`, the harness knows whether stimulus arrives from a `-t` SigSim vector
or from an internal `Clock` element, and it can count the clocked edges rather than being
told how many there were. Derive it, and the refusal becomes unnecessary because the
failure mode is gone.

Then spend the refusal principle where it still bites. On `test/fixtures/riscv-sum1to10.jls`
— today the only sub-CPU-scale circuit in the tree — #442 O4 measures **194 events, final
time 15**, i.e. roughly 60 µs of event loop. A "throughput" computed from that is noise
(#442 O4 observed a **4.2× ns/event spread across three reps in one JVM**). The harness
should refuse on: a run below a stated minimum event count or wall-clock floor; a run
whose warm-up discipline was not met; a JVM lacking the facility a field needs. Same
principle, applied to the defects that actually occur.

## Reframing 3 — the record's first consumer is the engine programme, not the doc

The adversarial comment on #554 (2026-08-08) already found four internal consumers waiting
on this instrument (#442 bands and the event-count equality, #476 queue share, #393
queue-traffic drop, #879 the k2000 loop-time gate) and warned each will hand-roll a probe.
That is the failure that matters, and it is a design constraint on #726, not a scheduling
note: a record carrying only events/s and cycles/s **cannot serve any of the four**, so
four private probes get written and none of the numbers are comparable.

The record must carry what the engine programme differences against:

- **phase split** — `initSimulation` vs `runEventLoop` separately. keystone-c §2 measures
  0.568 s vs 0.742 s on k2000; a single wall-clock figure is ~43% ambiguous, and §2 shows
  the setup half is quadratic in `SigSim` and would dominate any end-to-end number.
- **event census** — fired / posted / dup-suppressed, max queue depth, histogram by
  payload and by callback. These are the integers #442 asserts equality on.
- **fixture census** — the four components of #413 §7.10, so a figure attaches to a
  workload rather than a filename.
- **environment** — captured, not typed (#730's AC-3 has this right).

The deterministic integers and the noisy timings are different kinds of quantity and the
project already knows it: `test/jls/SpatialIndexTest.java:218-244` asserts the exact hit
count and merely *prints* the timing. Carry that split into the record's schema.

## Reframing 4 — "one documented command" already has two in-tree precedents; use them

Do not write a `scripts/bench.sh` and do not write Python (D5 forbids the latter; AC-3
already says so). `pom.xml` has both shapes this needs: a tagged surefire execution
(`display-tests`, `<groups>display</groups>`, its own argLine and retry policy) and
opt-in profiles (`errorprone`, `pitest`, lines 688-819) that exist precisely because they
must not run in the default build. A `@Tag("bench")` execution behind `-Pbench` is the
one-command answer, it runs from the in-tree build by construction (AC-3), it is excluded
from `mvn verify` by the same mechanism the display suite uses, and it inherits the
project's existing headless discipline.

And an out-of-the-box move #726 and #555 both miss: **`ghcr.io/anadon/jls` is already a
pinned, multi-arch, signed, headless environment.** #555's hardest criterion — "an
independent party reproduces each number from the doc alone" — is mostly an environment
problem, and the container already solves it. Publishing numbers against a named image
digest turns "same hardware, same JDK, same flags" from a paragraph of prose into a
`docker run` line. That is worth deciding here, because it changes what the environment
fields in #730's record must pin.

## The one real tension, named rather than glossed

#442 §7.5 forbids promoting its counting subclass to `src/`: "a production counter would
be a new engine field FEAT-030 would then have to preserve." #726 wants an instrument
citable by an outside party and surviving `riscv/`'s deletion. These pull opposite ways
and #726's body does not notice.

They are reconcilable, and the seam already exists: `Simulator.runEventLoop`
(`src/jls/sim/Simulator.java:215-241`) calls `beforeEvent()`/`afterEvent(event)`, both
`protected` no-op hooks, and `BatchSimulator` already overrides `afterEvent`. An
instrumenting subclass in the test tree costs the engine nothing and adds no field.
Recommendation: **instrument in the test tree, pin the environment with the container
image, and do not add production Java.** If a `--stats` flag on the shipped jar is ever
wanted, it is a separate decision with a separate cost, and `docs/batch-interface.md`'s
stability contract is what it would have to join.

## On AC-4, and on the ordering to #728

AC-4 ("figures within a stated run-to-run tolerance, and that tolerance is reported")
asks the harness to publish a number that is a property of the host, not of JLS —
portable only until someone runs it elsewhere. Replace it with: **state the estimator and
report the observed dispersion.** #442 §7.10 Stage 3 already settled the estimator (min
over reps, first two discarded, ceiling-only comparisons, never a floor) with the 4.17×
in-JVM spread as its justification. Report min/median/max alongside each timing; assert
equality only on the integers. A "tolerance" invites the failure mode #442 §11 names
directly — a band widened twice becomes decoration.

Ordering: #728 is `ordering_after` #726, but the harness's refusal thresholds, rep counts
and minimum-work floor are calibrated *by* what the fixtures can express, and the only
tracked candidate today retires 194 events. Land #726 against the existing tracked fixture
as its own self-test (a fixture that trips the too-short refusal is a fine first test),
then let #728 widen the population — or co-design the two. Do not calibrate a harness on
a 60 µs workload and then discover the thresholds were wrong.

## Verdict

**endorse-with-reframing.** The instrument should be built, it is a genuine precondition
of four engine tasks and of ever re-opening ARCHITECTURE.md's sole-strategy decision on
evidence, and nothing in the tree duplicates it. I am not disregarding the acceptance
criteria; I am asking for four amendments to them: report the `ns/event` × `events/cycle`
factorization with `cycles/s` derived (AC-1); derive the clocking regime and redirect the
refusal clause at short/unwarmed runs (AC-2); carry the phase split and event census so
the four internal consumers share this instrument rather than fork it (AC-1, and the
boundary section should claim being the *sole* instrument, not merely disclaim being a
second gate); and replace the run-to-run tolerance with a stated estimator plus observed
dispersion (AC-4). Deliver it as a `-Pbench` tagged execution, and let the publication
chain consume the record rather than shape it.
