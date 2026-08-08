# Issue #676: TASK-C350-2: a campaign runs across one machine's cores through the existing batch surface, with no distributed transport anywhere in the path
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

#676 is TASK-C350-2, the local multi-core dispatch task under #350 (FEAT-057,
"campaign execution and aggregation"). It wants a runner that fans a parsed
job list across N workers on one machine, executing each job "through the
existing batch surface (`BatchSimulator`, stdout, exit codes, the VCD
profile)" unmodified, with worker-count/memory bounds honoured and job
isolation guaranteed. `ordering_after: ["TASK-C350-1"]` (#674, the campaign
description format/reader, itself still open).

## Findings, most severe first

**1. [HIGH] The execution model — in-process threads vs. subprocesses — is never chosen, and nearly every other criterion depends on which one it is.**
The Outcome text says each job runs "through the existing batch surface
(`BatchSimulator`, stdout, exit codes, the VCD profile)." `BatchSimulator` is
a Java class with no notion of an exit code — exit status is assigned by
`JLSStart.start` at the CLI/process boundary (`docs/batch-interface.md` §1:
"stream and exit-status contract... status 0/1/2"). Stdout and exit codes are
process-level artifacts; a `BatchSimulator` instance called in-process from a
thread pool produces neither — it produces Java return values. So "reuse the
existing batch surface" reads two incompatible ways: (a) spawn `java -jar
jls... -b ...` subprocesses per job (real exit codes, real stdout, real
process isolation), or (b) drive `BatchSimulator` objects concurrently inside
one JVM via an `ExecutorService` (no exit codes, shared heap, shared
classloader). The issue never picks one, yet criteria 2 and 3 (below) are
only achievable under one of the two readings.
*Recommendation:* state the execution model explicitly in the task body
before work starts; it is a design decision, not an implementation detail.

**2. [HIGH] "A description that bounds... per-job memory is honoured, and exceeding the bound is reported as a named error" is not achievable under an in-process thread-pool reading, and the issue does not say it must be subprocess-based.**
The JVM has no per-thread heap quota mechanism; nothing in the standard
library lets one thread's allocations be capped and reported independently
of the rest of the heap it shares with every other worker. The only way to
honour a *per-job* memory bound and report an excursion as a named error is
one JVM (or cgroup) per job — i.e., subprocess dispatch with `-Xmx` or an OS
resource limit — which is exactly the ambiguity in finding 1. As written,
this acceptance criterion is a checkbox with no design behind it that would
make it true; an implementer who reads "existing batch surface" as
`BatchSimulator`-in-a-thread-pool cannot satisfy it at all.
*Recommendation:* either mandate subprocess dispatch explicitly, or drop the
per-job memory bound from this task's acceptance criteria and re-home it to
wherever the execution model gets decided.

**3. [HIGH] "A job's failure or hang does not corrupt or stall unrelated jobs" has no kill mechanism available at HEAD, and the mechanism that would provide one is explicitly out of scope.**
At `src/jls/sim/BatchSimulator.java:75-90`, both `stop()` and `pause(boolean)`
just set `stopping = true`; the javadoc at `:82` says "It doesn't make sense
to pause it in batch mode." That flag is only checked between event
retirements in `Simulator`'s loop, so it bounds *simulated* time (`now <=
maxTime`) but does nothing for a wall-clock hang caused by an expensive
per-event computation that never advances sim time far enough to trip the
limit. The clean-interrupt / heartbeat / suspend machinery that would let a
runner detect and abort a stuck job is FEAT-006 (#354) — itself open, not
landed, and #350 explicitly places "pause distinct from stop, heartbeat,
clean interrupt" out of this feature's scope ("A campaign is many long batch
runs and depends on those, but they are a separate feature," #350 §1
"Out of scope"). Without subprocess isolation (again, unspecified — finding
1), the only way to forcibly stop a hung in-process worker thread is the
deprecated/unsafe `Thread.stop()`. As written, this criterion cannot be
honestly satisfied by an in-process implementation, and the issue gives no
indication that the author intends subprocess dispatch to be mandatory.
*Recommendation:* either require subprocess dispatch (so `Process.destroy`/
`destroyForcibly` provides real isolation), or explicitly relax this
criterion to "cannot corrupt shared state" while documenting that a genuine
wall-clock hang is not yet recoverable until #354 lands — the same tradeoff
#350's own Open Question 5 already offers ("ship the local runner against
today's batch surface and accept that a job cannot be cleanly cancelled,
recommended for the demo slice, with the limitation documented"). #676 does
not carry that documented limitation forward; it states the stronger,
unqualified claim instead.

**4. [MEDIUM] Acceptance criterion 3 is not self-contained: it depends on a schema field TASK-C350-1 (#674) never commits to providing.**
#676 AC3 requires "a description that bounds worker count or per-job
memory" to be honoured. That description format is TASK-C350-1's deliverable
(#674), and #674's own acceptance criteria (read directly) cover job
naming, input/artifact fields, malformed-description rejection, and
naming-collision detection — none of them commits to a worker-count or
memory-bound field. #350's Open Question 3 recommends "(a) an explicit
worker cap and per-job memory bound in the description" but leaves it
unresolved ("Blocks integration criterion 5"). #676 is written as if that
field already exists in the format it consumes; if #674 lands without it,
#676's AC3 is unsatisfiable without #676 unilaterally extending a format
that is explicitly not its boundary to own.
*Recommendation:* either add the bound fields to #674's acceptance criteria
now (so #676 has something real to consume), or make #676 AC3 conditional /
defer it to whichever task actually lands the schema field.

**5. [MEDIUM] A known, already-flagged static-mutable-state hazard directly contradicts #676's own "jobs share no mutable state" criterion, and #676 doesn't cite it.**
`src/jls/sim/SimEvent.java:87` declares `private static long sequence = 0;`,
incremented unsynchronized at `:116,119` (`seq = sequence; ... sequence +=
1;`). This field is shared by every `Simulator`/`BatchSimulator` instance in
one JVM. If #676 is implemented as concurrent in-process worker threads (a
live reading per finding 1), this is a genuine unsynchronized read-modify-
write race across jobs — the opposite of "jobs share no mutable state" — and
it would also make tie-break ordering nondeterministic across a run,
threatening TASK-C350-4's (#679) byte-identity requirement one task later.
This exact defect is already recorded and slated for repair in #363
(FEAT-035): *"SimEvent's static sequence counter made per-Simulator
(TASK-0074)"* — but #363 fixes it for checkpointing, not for concurrent
dispatch, and #676 neither cites #363 nor gates on it nor calls out the
hazard for whoever implements it.
*Recommendation:* either require subprocess dispatch (sidesteps the shared
static entirely, each job gets its own JVM), or add an explicit acceptance
criterion that `SimEvent.sequence` (and any other static/global mutable
state in `jls.sim`) is audited and made per-instance before in-process
concurrent dispatch ships.

**6. [MEDIUM] The ordering dependency on TASK-C350-1 (#674) is asserted only in prose/YAML, not enforced anywhere GitHub tracks.**
#676's front matter says `ordering_after: ["TASK-C350-1"]`, but
`issue_dependencies_summary` on #676 reports `total_blocked_by: 0` — there is
no GitHub-level `blocked_by` link forcing #674 to land first. #674 is itself
open and unlanded. Nothing stops someone from picking up #676 before the
committed description format exists, inventing an ad hoc job-list shape to
unblock themselves, and creating rework (or worse, a de facto format that
#674 then has to match) once #674 actually lands.
*Recommendation:* file the GitHub `blocked_by`/`blocks` edge between #674 and
#676 explicitly, matching the discipline #350 and its sibling features use
elsewhere in this same tracker (see #350's and #354's own Link-phase
mirrored-edge sections).

**7. [LOW] "Runs to completion across N workers" does not require N>1 to actually be exercised, so the headline claim is gameable.**
Nothing in the acceptance criteria forces a test to run with N>1 concurrent
workers; an implementation that hardcodes or defaults to N=1 (sequential
dispatch) satisfies the literal text of AC1 while never exercising the
concurrency-safety claims the rest of the criteria assume (shared state,
hang isolation, bound enforcement). The N-vs-1 byte-identity property that
would actually force real concurrency to be tested lives one task later, in
TASK-C350-4 (#679) — #676 alone has no test that requires N>1.
*Recommendation:* add an explicit "runs with N>1 on a multi-core host" case
to this task's own acceptance criteria rather than leaving that assurance
entirely to a downstream task.

**8. [LOW] "reports exceeding its bound rather than thrashing the host" leaves both the trigger and the response undefined.**
Is bound-checking a pre-flight validation (reject the campaign before any
job runs) or a live runtime cap (throttle admission once N workers are
already busy)? Both are plausible readings of "reports... rather than
thrashing," and they imply different code paths and different test setups.
*Recommendation:* pick one (pre-flight validation is cheaper to test and
matches TASK-C350-1's "reject at read time, not write time" philosophy used
elsewhere in #350) and say so.

## What is solid

- The Boundary section (dispatch only; naming/aggregation/failure-accounting
  explicitly deferred to #677/#679/#681) is a clean cut and matches #350's
  own decomposition rationale — no scope creep in that direction.
- The "no AWT, Swing or `jls.edit`" constraint is consistent with the
  codebase's existing headless discipline (`BatchSimulator.java:13-15`'s
  javadoc, enforced today by `HeadlessCoreRatchetTest` per ARCHITECTURE.md)
  and is mechanically checkable, not aspirational.
- "The batch contract is unmodified" lines up cleanly with
  `docs/batch-interface.md`'s stability promise (§6) — there is no tension
  between this task and that normative document as written.

## Verdict rationale

The idea — wrap the existing single-job batch surface in a multi-worker
dispatcher — is sound, and the boundary against sibling tasks is well drawn.
But three of its five acceptance criteria (memory bounding, hang isolation,
worker-count bounding) quietly assume an execution model the issue never
commits to, and under the most natural in-process reading two of those three
are not achievable at all with today's JVM primitives. Combined with a real,
already-documented static-mutable-state hazard (`SimEvent.sequence`) that
directly contradicts the "no shared mutable state" criterion, and a schema
dependency on #674 that #674's own acceptance criteria don't yet promise —
this needs a design decision (subprocess vs. in-process) and a criteria
rewrite before implementation starts, not just careful coding.
