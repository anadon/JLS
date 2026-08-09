# Issue #442: TASK-0026: a simulation regression becomes a build failure — an exact event-count equality, two ceiling-only bands, and one gate over every golden
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the apparatus and the claim is: *before FEAT-030 (#362) rewrites the queue, the
value representation (#232/#879), and the zero-delay closure (#393), the project needs a
mechanical definition of "semantics-preserving" so that a speedup PR carries evidence
instead of an intention.* That goal is correct, it is load-bearing for three engine
changes, and nothing in the tree serves it today. I endorse the goal without reservation.

I do not endorse the four artifacts. Of the four assertions proposed, one is right and
badly encoded, two contradict the project's own performance keystone, and one is a no-op
wrapper around tests that `mvn verify` already runs. The reframing below replaces all
four with a single artifact the repo already knows how to review.

## The reframing: a committed event census golden, not a budget properties file

`docs/capability-roadmap/keystone-c-performance.md:123-131` already contains the exact
artifact this task wants, produced by a scratch harness (`jls.sim.KernelProbe`, §"Harnesses",
line 828) that was thrown away:

```
fired 2,331,793   posted 2,596,496   dup-suppressed 264,702   max queue depth 12,093
payloads: PinChanged 1,919,891 (82.3%)   NewValue 378,129 (16.2%) ...
callbacks: Mux 875,291  Register 428,298  Splitter 250,115  AndGate 207,456 ...
```

Every number there is a **deterministic integer**, a pure function of (circuit, stimulus).
None of them is a wall clock, none needs warm-up reps, best-of-N, `getThreadAllocatedBytes`,
a thread-identity assertion, a surefire parallelism decision, a ceiling, a band, or a
tolerance. The proposal:

1. Promote `KernelProbe` into `test/` as a census producer over the fixtures the golden
   suites already load, and **commit its formatted output as a golden file**, reviewed
   the same way every other golden in this repo is reviewed.
2. Delete `test/fixtures/simulation-budget.properties` from the plan entirely — with it
   go the properties schema, the ISO-8859-1-vs-UTF-8 decision (§7.3), the private parser,
   the anti-vacuity clause P6, the regime-column parse rule P7, and the re-baseline
   protocol §12 wants documented in `CONTRIBUTING.md`. A golden file has no schema to
   get wrong and no empty state that reads as green: an emptied golden is a diff.
3. The "clocking regime" problem (O6, four different events/cycle figures on
   element-identical circuits) **disappears**, because the census is produced by a test
   that owns its stimulus in code, exactly as `RiscvCpuGoldenTest.clockVector()` already
   does. The regime is not a column to be declared and validated; it is the test.

This is not a stylistic preference. It is the repo's existing idiom: `BatchSimulationGoldenTest`,
`SequentialGoldenTest` and `RiscvCpuGoldenTest` carry expected values in code and fail on
any change. The task as written invents a second, weaker mechanism next to a strong one.

## Three concrete corrections that survive independent of the reframing

**C1 — the measured baseline of 194 events is an artifact of running the fixture with no
stimulus, and the tree already says so.** O4's probe calls `runSim()` at the default time
limit with no `-t` vector; O5 then concludes the fixture "does not exercise the queue" and
uses that to justify #413 blocking this task. But `test/jls/RiscvCpuGoldenTest.java:26-36`
drives the *same* fixture with a generated 34-cycle clock vector and describes it as
exercising "multi-thousand-event scheduling". The fixture is fine; the probe under-drove
it. The 194 is a measurement of an undriven circuit settling, and any declared number
derived from it is wrong. This also weakens the #413 blocking edge: a CPU-scale census is
available today by reusing the vector the golden test already builds.

**C2 — O1's "nothing asserts an event count" is an artifact of grepping for vocabulary
rather than for the concept.** `test/jls/SimulationSemanticsRegressionTest.java:292-309`
defines a `CountingSimulator` that overrides `post()` and counts, and lines 565-582 assert
`assertEquals(1|2|3, sim.posts, ...)`. `test/jls/elem/RegisterModelTest.java:348-397`
defines `RegisterPostCountingSimulator` and asserts `assertEquals(2, sim.registerPosts,
"exactly the initSim seed and the first capture...")`. `src/jls/sim/Simulator.java:163`
carries `@jls.testedby jls.SimulationSemanticsRegressionTest.CountingSimulator#post()`.
The precedent this task claims to be establishing already exists twice, is already the
right shape, and should be cited and scaled rather than re-invented. This is good news
for the issue, not bad: the design is already validated in-tree.

**C3 — §7.5's package constraint rests on a Java misreading.** O2 says `afterEvent` "is
`protected`, so the ratchet's subclass must live in package `jls.sim` to reach it." A
subclass may override a protected member from any package; `RegisterPostCountingSimulator`
lives in `jls.elem` and overrides `BatchSimulator.post` today. The placement decision
should be made on merit, not on a false constraint.

## Where the issue pulls against the project's trajectory

**The timing band contradicts the document that measured the constant it would guard.**
keystone-c §8.1, on the very gate this task is meant to be: *"It does not need to be a
JUnit assertion — a timing assertion in CI is a flake factory — but the numbers should be
produced by the same command that produces the goldens, so that a regression is visible
rather than discovered."* The issue's own O4 supplies the refutation (4.17x spread in one
JVM), its own O7 cites the in-tree precedent that **prints** timings and asserts only the
exact quantity, and then Open Question 3 proposes to gate on wall clock anyway "with
generous headroom", pre-authorising a demotion. A band wide enough not to flake on a
shared runner is wider than every regression FEAT-030 could plausibly introduce. Drop the
ns/event assertion from the required lane; it belongs to #735's scheduled lane, where the
comment on this issue already puts it.

**The bytes/event band measures the wrong thing for the same reason.** What the project
cares about is not "bytes on a HotSpot thread counter" but "1.92 M zero-field `PinChanged`
records allocated per run" — a *count*, deterministic, already in the census. Count the
allocations you care about instead of weighing the heap, and H3, the JDK-version threat,
the `com.sun.management` cast, the skip-with-a-reason path, and §7.9's whole
thread-identity argument all evaporate.

**`GoldenCorpusByteIdentityTest` is a no-op.** All eight `*GoldenTest` classes already run
on every `mvn verify` and already fail the build byte-for-byte; the expectations are
in-code, not in files a separate runner must compare. An aggregating test that re-runs them
adds latency to the required lane and protects nothing. What remains is the *enrolment
invariant* — and a new golden class is discovered and run by surefire automatically, so the
invariant guards against a failure mode that cannot occur. Note that O3 spends its evidence
demonstrating that a planning document's list of six was wrong; that is a correction to a
document, not a defect in the build. Delete this deliverable and Open Questions 1 and 2 go
with it.

**§7.5's ban on a production counter pulls against LF-03.**
`docs/capability-roadmap/lf-03-causal-debug.md:324` decides checkpoint intervals
"adaptive by fired-event count, not simulated time", and cites the same census at :786-787.
A per-`Simulator` fired-event counter is on the roadmap regardless; keystone-c:640-642
independently asks for the static `SimEvent` sequence counter to become per-`Simulator`.
Forbidding the counter in `src/` because "FEAT-030 would then have to preserve it" inverts
the argument — a counter FEAT-030 must preserve is exactly what a semantics-preservation
gate is made of.

## A second, more ambitious framing worth weighing

Make the census a **product surface**, not test scaffolding: a batch flag (`-stats`) that
prints the census after a run. Then the gate is a CLI golden — the repo's strongest idiom,
already exercised by `CliSmokeTest` and the batch goldens — and three audiences get served
by one mechanism: FEAT-030 gets its regression gate, instructors get "your students'
circuit fires 2.3 M events" as a gradable observable, and LF-03 gets the counter it needs.
The issue names instructors as beneficiaries "indirectly, protected"; under this framing
they are served directly. The cost is a documented output surface under
`docs/batch-interface.md`'s stability contract, which is a real cost and the reason I
offer this as the alternative rather than the recommendation.

## Disregarding two of the stated acceptance criteria, explicitly

- **P4/P5 and the "ceiling only, never a floor" rule.** I am setting this aside, not
  softening it. A one-sided ceiling is designed never to fail on improvement — which means
  the single most interesting event in FEAT-030's life, the moment the event count or
  allocation drops, produces **silence**. Under a census golden, an improvement turns the
  build red and the diff *is* the evidence the PR must present. That is precisely how this
  repo's other ratchets behave: `HeadlessCoreRatchetTest` fails when you clean a file and
  makes you delete the baseline line by hand. The right instrument here is a shrinking
  baseline, not a ceiling.
- **P9's ban on pinning queue depth and dup-suppression counts.** The stated reason is that
  they are FEAT-030's legitimate targets. Under a golden census, that makes them the most
  valuable rows in the file: max queue depth 12,093 → "roughly the levelized depth" is
  #393's entire claim (keystone-c:554), and a golden diff is how anyone will verify it. Do
  not pin them with an assertion; **record** them in the golden and let the diff be read.
  The second comment on this issue reaches the same conclusion from another direction —
  "when it goes red, the required output is the diagnosis, not a new baseline."

## What I would keep verbatim

The event-count equality as an `assertEquals` with no tolerance, and its failure message
naming the baseline's location (P3), are exactly right and are the heart of the task.
The insistence on three deliberate red runs in the PR is the right discipline and should
survive into the census framing (change one census number; delete the golden; add a
fixture without a census row). H5 — that stimulus-driven and `Clock`-driven fixtures cannot
share a population because `SigSim.initSim` pre-posts during elaboration — is a real
observation and is worth keeping as a note on the census producer.

## Recommended shape

One new test class producing a formatted census over at least two driven fixtures (the
riscv CPU under the vector `RiscvCpuGoldenTest` already builds, plus one small circuit),
one committed golden file, a `CONTRIBUTING.md` line saying a census diff needs a stated
reason, and nothing else. No properties file, no timing assertion in the required lane, no
allocation bean, no golden-corpus wrapper, no enrolment invariant. That is roughly a fifth
of the specified work, it removes four of the five open questions, and it removes the
#377/#379 blocking edges outright — measured *ceilings* need those tasks' numbers; a
committed census needs only a run.
