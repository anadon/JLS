# Issue #350: FEAT-057: many independent runs dispatch across cores or hosts and aggregate into one report whose contents do not depend on how many workers ran
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two things, and only the first is stated as the headline. (a) An **early, visible
deliverable** for CAP-17 (#312) that works before any partitioning exists — #312's own
Cost section calls it "CAP-17-MIN". (b) A **shared job/artifact/aggregate vocabulary**
that the grading harness (#369, and now the filed CAP-21 block behind #697/#724),
multi-seed verification (#306) and the capacity axis would each otherwise invent
separately. Both goals are right. Instructors grading 300 submissions and anyone running
a seed sweep are real users, today, on one machine.

I endorse the goal. I am rejecting the premise underneath the plan: that **JLS should own
dispatch**. Every expensive thing in this issue — the runner (#676), the multi-host scope
(#683), the `blocked_by: [354, 363]` edges, the 6-8 mw band, and the unresolvable
ownership deadlock in §7 — descends from that one premise, and the premise is not
load-bearing for any of the five acceptance criteria.

## The fact the plan does not exploit: the leaf is already deterministic

`BatchSimulator`'s VCD writer already omits `$date`/`$version` and dumps "in full-name
order" (`/home/user/JLS/src/jls/sim/BatchSimulator.java:376`, `:420`, `:453`); the stdout
report is name-ordered and pinned by goldens (`BatchSimulationGoldenTest`,
`VcdExportGoldenTest`); `docs/batch-interface.md` is a stability contract with a CHANGELOG
gate; `ARCHITECTURE.md` records that "**Batch mode never leaves the main thread** and never
touches Swing". The project has already paid for byte-determinism at the leaf, deliberately,
years ahead of this issue.

So criterion 2 — byte-identical aggregate at 1 worker and at N — is not a distributed-systems
theorem here. It is a corollary of two facts: the leaf is deterministic (holds today), and
the aggregate is a pure fold over the description's order (a property of one function).
§3 of the issue writes this out itself: `A = ⊕ r(jᵢ)`, "per-job results looked up rather
than streamed". Once stated that way, **the dispatcher is irrelevant to the correctness
claim** — which means it does not have to be JLS's code.

The issue's own strongest sentence argues against its plan: "*The entire correctness
content of this feature is that concurrency is not observable in the output.*" A feature
whose entire correctness content is the *absence* of an effect should not be the feature
that introduces the mechanism producing it. The cheapest way to make concurrency
unobservable is to have no concurrency inside the artifact-producing program.

## The alternative: a receipt contract and a pure aggregator; the OS dispatches

Three pieces, none of them a scheduler.

1. **A per-run receipt.** One `jls -b` run emits a self-describing, deterministic record:
   digests of the circuit file, the `-t` vectors and `-s` params, the `-d` limit, the JLS
   version, the §3 watched-output block, exit status. No clock, no host, no pid, no worker.
2. **A pure aggregator.** `jls -aggregate out.txt jobs.list dir/` folds the job list in
   list order, reading receipts by lookup. Missing receipt ⇒ `failed`. Zero threads,
   trivially unit-testable, and the 1-vs-N test collapses into an aggregator unit test plus
   the leaf goldens the suite already has.
3. **Dispatch is documentation, not code.** `xargs -P`, `make -j`, GNU parallel, a Slurm
   array, an Actions matrix, and the existing `ghcr.io/anadon/jls` image. This is exactly
   the seam `examples/autograde/autograde.py` and `docs/vcd-interop.md` already establish —
   "run JLS to completion as a subprocess, inspect finished outputs", the pattern #63
   chose over live co-simulation.

Score the acceptance criteria against it:

- **1 (committed, diffable description)** — a newline-delimited job list, the cheapest
  reviewable artifact there is, and the same file `xargs -P` consumes. Open Question 2
  dissolves; #674 does not need a serialization decision at all.
- **2 (byte identity)** — true by construction; nothing in JLS ever observes a worker.
- **3 (denominator)** — the fold's index set is the job list, so a job that produced no
  receipt is failed. **Strictly stronger than the issue's version**: it survives a worker
  being OOM-killed or a host going away, which an in-JVM runner reporting its own failures
  does not.
- **4 (naming from the description)** — path = digest of the receipt's input section.
  Injective by construction; no read-time collision check, so criterion 3 of §5 becomes
  unnecessary rather than tested. It also yields resume-by-skip for free, which answers
  Open Question 4 without #363 (checkpointing) — a campaign's unit of restart is a job,
  not a simulation state.
- **5 (no transport on one machine)** — vacuous.

And the dependency graph shrinks: **#683 and its coupling to #333's transport disappear**
(a grid is `sbatch --array` or `parallel --sshloginfile`, and works on clusters a home-grown
transport never would); **#363 stops gating** per criterion 4 above; **#354 weakens** —
under an external dispatcher, cancellation is SIGKILL of a subprocess, which works today,
where in-JVM clean cancellation is precisely what JLS lacks (Open Question 5). The process
boundary is this project's most reliable cancellation primitive. A demo slice that is
nearly unblocked is what a demo slice is supposed to be. Cost drops from 6-8 mw to roughly
1.5-3 mw: receipt format, aggregator, one docs page, one recipes page.

## Why this is the direction the project is already going

`docs/grand-architecture.md` §1: the architecture "may not assume a network, a server, or
an install step", and the batch surface is a co-equal front end. The settled HDL stance is
"orchestrate external tools, never reimplement". #312 itself warns against "a second
networking stack". #350 as written adds a scheduler, an artifact store and a transport
binding to a single-maintainer 69k-line Swing-era teaching simulator whose deployment model
is one self-contained jar. That pulls against three recorded positions at once. The reframe
pulls with all three.

## It also dissolves the deadlock §7 cannot resolve

Open Question 1 has been open since filing, the dedup pass explicitly declined to settle it
(comment of 2026-08-04), and today's REPLAN escalated it while blocking #674. The rule
itself is bad: "whichever ships first owns the job description and the aggregation format"
makes the project's most-reused vocabulary a **race outcome**. Under the reframe there is
nothing to race for. JLS never owns "run many things." The grading harness owns *verdict*
aggregation — rubric, PASS/FAIL/UNRUN, counterexamples, the frozen CLI of #686/#687. #350
owns the *receipt*, one layer below, which grading consumes rather than competes with.
Two features cannot fight over a dispatcher neither of them builds. That converts a
coin-flip into a layering an architect can decide this week.

## Objections I take seriously

- **Instructors on Windows have no `xargs`.** Fair. Answer: the container image is already
  the documented grading path, and a ~100-line dispatcher belongs in `examples/campaign/`
  next to `autograde.py` — as an *example*, replaceable without a CHANGELOG entry, never
  under `docs/batch-interface.md`'s compatibility promise. Keeping dispatch outside the
  stability contract is the architectural line that matters.
- **JVM startup × thousands of jobs.** The roadmap's own measurement is 0.74 s/run warm
  (`docs/capability-roadmap/lf-05-fault-and-power.md:218`), so startup is not dominant at
  fixture scale. If it ever binds, the correct fix is a `-b` mode that reads a job list on
  stdin and loops — still single-threaded, still deterministic, ~50 lines, and any
  dispatcher can spawn N of them. That is the only part of "the runner" worth writing in
  Java, and it is the last thing to build, not the first.
- **#312's walk-through literally says `jls -campaign sweep.yaml -j 200`.** That is a
  CLI-shape commitment, not an outcome. **I am explicitly disregarding the acceptance
  criteria that presuppose a JLS-owned dispatcher and a `-j` flag**, and keeping the
  outcome they were written to guarantee: a sweep dispatched across a grid returning one
  aggregated report whose bytes do not reveal the schedule.

## Fallback framing, if a `-campaign` surface is wanted anyway

`jls -campaign-plan sweep.yaml > Makefile`. You inherit `-j`, resume, failure reporting and
dependency-aware re-runs from a tool every lab machine and every CI already has, and the
"committed, diffable file" is the generated makefile — reviewable, and directly executable
by a human who wants to run one job by hand. Strictly better than owning a scheduler,
strictly worse than owning nothing.

## Concrete recommendation

Answer Open Question 1 by layering, not by shipping order: grading owns verdicts, #350 owns
the receipt. Re-scope #674 to the receipt format (not a job-description serialization),
keep #679 as the pure aggregator, fold #677 and #681 into it, and close #676 and #683 with
a `REPLAN:` naming the OS as the dispatcher. Ship the receipt plus aggregator plus a
recipes doc first; measure whether a stdin job-list worker is ever needed.
