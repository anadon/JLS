# Issue #676: TASK-C350-2: a campaign runs across one machine's cores through the existing batch surface, with no distributed transport anywhere in the path
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

#350's whole correctness content is one sentence: *concurrency must not be
observable in the output*. Every other task in the C350 family exists to make
that true — the description (#674) supplies a job order that is not a dispatch
order, artifact naming (#677) keeps paths off the scheduler, aggregation (#679)
folds in description order, failure accounting (#681) keeps the denominator
honest. #676 is the one task that actually *introduces* the concurrency those
four then have to neutralize. So the right question for this issue is not "does
it dispatch jobs" but "which seam does it cut concurrency along, such that the
other four tasks have the least work to do and the least to get wrong."

The issue leaves that seam unnamed, and the phrasing pushes the wrong way. It
says jobs run "through the existing batch surface (`BatchSimulator`, stdout,
exit codes, the VCD profile)". That list conflates two different surfaces: a
Java class and a process contract. An implementer reading it will reach for an
`ExecutorService` over `BatchSimulator` instances, because that is what naming
a class invites. That reading is not available.

## The in-JVM seam is closed, and the evidence is in the tree today

Acceptance criterion 2 — "jobs share no mutable state" — is false by
construction for threads in one JVM, at three separate places:

- `src/jls/sim/SimEvent.java:87` — `private static long sequence = 0`, a
  plain non-atomic static, incremented in the constructor (`seq = sequence;
  … sequence += 1;`) and used as the same-time tie-breaker in `compareTo`.
  Two simulators constructing events concurrently can be handed the *same*
  seq; two same-time events with equal seq compare 0 and the `PriorityQueue`
  orders them arbitrarily. That is a nondeterministic simulation result
  caused by worker count — the exact failure #350 invariant 1 and integration
  criterion 1 exist to forbid, arriving flakily rather than reproducibly.
- `src/jls/Circuit.java:89` — `private static int lineNumber`, reset in
  `load` and incremented throughout, feeding every `LoadError`. Concurrent
  loads cross-contaminate each other's error locations. Paired with
  `JLSInfo.setLoadError` writing the process-global `JLSInfo.lastLoadError` /
  `loadError` statics (`src/jls/JLSInfo.java:95-115`), job A's diagnostic can
  land in job B's failure record — which is #681's contract, broken from
  underneath.
- The batch report is written straight to the process-global stream:
  `BatchSimulator.displayOutcome` (`:571`) and `TestGen` (`:74,96,97`) call
  `System.out.println` directly, and `JLSStart` keeps every parsed flag in
  mutable statics (`:98-126`) and calls `System.exit(1)` on ~30 failure paths.
  Threaded jobs would interleave stdout, and separating them means threading
  a `PrintStream` through `BatchSimulator`, `TestGen` and `JLSStart` — i.e.
  modifying the batch surface, which criterion 4 of this very issue forbids.

None of this is a bug to fix in passing. It is the shape of a program that has
always been one run per JVM, and #221's recorded decision (the interpreter is
the sole strategy, the hot plane stays core-internal) gives no mandate to
re-plumb it for a campaign runner.

## The reframing: the unit of isolation is a process, not a thread

Dispatch child JVMs. One job, one process, invoked exactly as
`docs/batch-interface.md` specifies — `-b [-s param] [-t tests] [-d limit]
[-vcd out] circuit.jls` — with the runner consuming stdout bytes, the exit
status, and the VCD file on disk. Nothing else.

This is not a workaround; it is the project's own recorded pattern, scaled.
`docs/vcd-interop.md` §"The supported grading pattern is a plain subprocess
bridge" already says this is how harnesses consume batch, and
`examples/autograde/autograde.py` (175 lines, CI-green on every push via
`test/jls/AutogradeBridgeExampleTest`) is a working single-job instance of it.
Fifteen test classes already spawn JLS this way. ARCHITECTURE.md's recorded
plugin-trust decision puts external tools on the subprocess boundary for the
same reason. A campaign runner is the autograde bridge with a worker pool.

What the reframing pays for itself with — each of these is an open #350
question that stops being a design problem and becomes a flag:

- **Criterion 2 (no shared mutable state):** address-space isolation, for
  free, against a codebase that will never be audited static-by-static.
- **Criterion 2's second half (a hang does not stall unrelated jobs) and
  #350 Open Question 5:** `Process.waitFor(timeout)` + `destroyForcibly`.
  Today's batch surface has no cancellation at all — OQ5 recommends shipping
  without it and documenting the limitation. Process dispatch makes clean
  per-job cancellation available on day one, without the long-run-ergonomics
  feature landing first.
- **Criterion 3 and #350 Open Question 3 (bounding):** per-child `-Xmx`,
  plus a semaphore on worker count. Exceeding a bound is a child exiting
  non-zero with an OOM, observed by the parent and named — not host thrash.
  The option-(a) recommendation in OQ3 becomes a one-line child argv change.
- **Criterion 5 (no AWT/Swing/`jls.edit` in the runner path):** trivially
  true — the runner imports `java.lang.ProcessBuilder` and nothing from
  `jls.*` at all. Stronger than the criterion asks.
- **#683 (multi-host):** the "worker source substitution" #350 §6 wants
  becomes literal. `ProcessBuilder` → `ssh host …` or a container run is a
  worker-source change over identical argv. The transport carries a command
  line and a file, not a Java object graph. That is what makes #683 not a
  second implementation, and it is decided *here*, in #676, by picking the
  job's wire form.

**One rule this forces, and the issue should state it:** strictly one job per
JVM. The obvious optimization — hand a worker JVM a slice of jobs to run
sequentially — reintroduces `SimEvent.sequence` and `JLSInfo.loadError`
carryover *between* jobs, making a job's output depend on which jobs preceded
it in its slice. That is invariant 1 violated by dispatch order, arriving
through the back door. If per-job JVM startup ever matters, amortize with
AppCDS, never with worker reuse.

## A second, more radical framing worth considering before building anything

Ask what only JLS can do here. Running N independent commands across cores is
solved by `xargs -P`, `make -j`, Slurm, and every CI matrix in existence. What
is *not* solved anywhere is #350's novel content: a diffable job description,
description-derived artifact names, and a byte-identical order-independent
fold. Those live in #674, #677, #679, #681. #676 is the commodity part.

So consider making the runner a **plan compiler with a bundled default
executor**: `jls campaign plan` emits the job list as an executable plan (a
line-per-job argv file, and/or a Makefile whose targets are the artifact
paths), and `jls campaign run` is a thin portable pool over that same plan for
users on Windows or a bare lab machine with no `make`. The plan is a committed,
diffable artifact — exactly what #674 wants — and it is the honest interface to
every grid the world already has. A Makefile in particular hands you worker
bounding (`-j N`), per-target failure isolation, and re-run-only-what-changed
for free, and its targets *are* the injective artifact-path check #674 demands,
checkable by construction.

I would not drop the bundled executor: the single self-contained jar is JLS's
deployment model (README, and the ARCHITECTURE.md help-delivery decision cite
it), instructors on Windows have no `xargs`, and #312's acceptance test wants
one command. But shipping the plan as a first-class artifact alongside it costs
almost nothing and turns #683 into documentation rather than code.

## What I am disregarding, and why

The acceptance criteria as written are fine — I am not asking to weaken any of
them. I *am* disregarding the parenthetical "(`BatchSimulator`, stdout, exit
codes, the VCD profile)" in the Outcome. Naming a Java class as the surface is
the single most consequential word in this issue and it points at the seam that
cannot hold. Replace it with: "each job executed as a child process against the
CLI contract in `docs/batch-interface.md` — argv, stdout, exit status, VCD
file". Then add two criteria the issue is currently missing:

- No two jobs run in the same JVM, ever — including under any worker-reuse
  optimization. (Guard: a ratchet test asserting the runner touches no `jls.*`
  simulation type, mirroring `HeadlessCoreRatchetTest`'s style.)
- A job that exceeds its wall-clock or memory bound is killed and reported as
  a named failure, with its partial stdout retained for #681.

The `SimEvent.sequence` / `Circuit.lineNumber` / `System.out` findings above
also belong in #674's or #350's record as the *reason* the wire form is argv,
so a future contributor does not "simplify" the runner back into threads.

## Verdict

**endorse-with-reframing.** The task is correctly cut, correctly ordered, and
is genuinely the demo slice #350 claims it is. But its most important decision
— the isolation seam — is left implicit and phrased in a way that invites the
one choice the codebase cannot support. Cut along the process boundary the
project already uses for autograding, make the job's wire form an argv line,
and four of #350's open questions and one of its later tasks get easier at the
same time.
