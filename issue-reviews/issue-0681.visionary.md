# Issue #681: TASK-C350-5: a failed job is reported with its inputs and its output, and the aggregate's denominator stays the job count
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the packaging and #681 is not about failures at all. It is about **totality**:
the claim that a campaign's aggregate is a *total* function from the described job
set to outcomes, so that a number read off the report can be believed. Every
consumer #350 names — an instructor grading a cohort (#300), a multi-seed
verification run (#306), #312's capacity demo — is buying exactly one thing from
this feature: the right to trust an aggregate they did not watch being produced.
A report that silently drops a third of its rows is not "slightly wrong"; it is
worse than no report, because it manufactures confidence.

That goal is squarely on the project's arc, and it is the same discipline JLS
already practices elsewhere: `LoadError`'s fixed category taxonomy exists so a
load failure is a *named* outcome rather than a stack trace (ARCHITECTURE.md,
"Error-reporting contracts"), and `docs/batch-interface.md` is a stability
contract precisely so autograders can trust what they parse. #681 is that same
instinct applied one level up. **I endorse the outcome without reservation.**

What I am rejecting is the cut. As scoped, this task mints a *second, cruder*
failure taxonomy at the process boundary, ahead of the aggregate it constrains
and ahead of the ownership question #350 §7 says must be settled first — and the
taxonomy it mints would still report 100% on the campaign the issue's own opening
paragraph describes.

## Reframing 1 — the four failure kinds are cut at the wrong boundary, and miss the dominant one

AC-3 names four kinds: non-zero exit, crash, timeout, missing expected artifact.
Three of those four are observations a parent process makes about a child. That
choice quietly discards everything JLS itself knows about why a run went wrong,
and it lets the most likely real failure through as a success.

`BatchSimulator.displayOutcome` (`src/jls/sim/BatchSimulator.java:562-572`) picks
one of four reasons — `Simulation Stopped`, `Simulation Time Limit`,
`Simulation: No More Activity`, `Simulation Complete` — prints it to stdout, and
the process then exits **0 in all four cases**. `docs/batch-interface.md` §1 is
explicit: status 0 means "run completed", full stop. So a parameter sweep in which
some parameter drives a combinational loop, or a fault injection that leaves a
circuit oscillating, hits `-d` and exits 0 with a truncated, meaningless trace.
Under #681 as written that job is a **success**. So is a run whose `-t` file was
malformed in a way the parser tolerated. So, arguably, is `No More Activity` on a
circuit that was supposed to keep clocking.

The issue's motivating sentence is "quietly reports 100% on a campaign where a
third of the jobs died." The taxonomy it proposes reproduces that exact defect
for the failure mode a sweep is *most* likely to hit. Meanwhile it collapses in
the other direction: "non-zero exit" is one bucket covering usage error (status 2),
runtime failure (status 1), and `-t` parse errors — which per §1's recorded
deviation print to **stdout**, not stderr, and still exit 1.

**I am explicitly disregarding AC-3 as written.** The kinds worth distinguishing
are not the ones the operating system happens to expose.

## The alternative: let the batch surface report its own outcome, and make the campaign a dumb collector

Add an additive, machine-readable per-run outcome record to the batch surface —
a sidecar file selected by a new flag, carrying the `displayOutcome` reason, the
final simulation time, whether the `-d` ceiling was reached, the `LoadError`
category if one occurred, and the artifacts actually written. This does **not**
touch stdout, exit codes, or the VCD profile, so #350 invariant 4 holds by
construction: it is a new flag, not a changed format. Write it in the same
line-oriented, sorted-key text discipline the save format and batch output already
use — diffable, byte-stable, no new dependency (note `jls.hdl.yosys.JsonValue` is
parse-only, and its javadoc records that JLS deliberately carries no JSON library).

The campaign's failure row is then that record, plus the two facts only a parent
can know: *killed* (signal, wall-clock kill) and *no record produced at all* —
which is the honest, general form of "crash" and subsumes "missing expected
artifact" as a special case against #674's declared expectations.

Why this is the better route, in one line each:

- **It is strictly more informative** than exit-code sniffing, and it is the only
  route that catches the `Simulation Time Limit`-as-success hole above.
- **AC-2 becomes achievable.** "Sufficient to reproduce the failure without
  re-running the campaign" needs the *inputs* (which #674's description already
  holds) plus *what JLS observed* — which today exists only as prose on stdout.
- **Four consumers for one artifact.** The campaign runner; #369/CAP-21's grading
  harness, which needs per-run outcome facts for its verdicts; the autograde
  bridge pattern in `docs/vcd-interop.md`, which today parses prose; and #214's
  in-editor test panel. Building it here and calling it campaign-internal wastes it.
- **It is where the knowledge lives.** #681's own Boundary says it "does not
  decide *why* a simulation failed — that is the batch surface's verdict." Correct
  — so ask the batch surface, rather than guessing from its exit status.

## Reframing 2 — make totality a property of the type, not of a test

The issue justifies its own existence by saying the tempting implementation
(append successes, drop failures) passes every success-only test. True. The
conclusion drawn is "so file it as a separate task with its own tests." The
better conclusion is **make that implementation unwriteable.**

On JDK 25 the shape is one sealed interface:

    sealed interface JobResult permits Ok, Failed, NotRun

and one aggregation step that folds the *description's* job list, looking each
result up by job identity, with a missing lookup an error rather than a skip.
Then the denominator is `jobs.size()` because there is no other list in scope, and
"append in completion order" is not a bug to be tested for — it is a program that
cannot be expressed, since nothing hands the aggregator a completion stream. That
is exactly the fold #350 §3 already writes down mathematically; the issue treats
it as a property to verify rather than as a data model to adopt.

This makes the current cut counterproductive. #681 is ordered after the runner
(TASK-C350-2) and #679 is ordered after #681 — so this task must assert
"a campaign reports m jobs with one failure" (AC-1) **before the aggregate that
would carry that count exists**. The seam between #681 and #679 is drawn along
success-path/failure-path, which is precisely the seam that produces the bug both
exist to prevent. The right seam is *result algebra* (this task) versus
*rendering and byte-stability* (#679) — and honestly, at 0.5-1 mw and 1-1.5 mw,
they are one landing.

## Reframing 3 — do not mint a second result vocabulary; adopt the grading harness's

#350's own §7 names the failure mode: "two independent implementations of 'run
many things and collect the output'", and the 2026-08-08 REPLAN comment escalates
it rather than settling it, noting that the grading side now has filed children
(#697, #724) and a frozen CLI contract (#686, #687) behind it. #681 defines a row
format. A row format *is* the aggregation format. So this task is, quietly, one of
the two places Open Question 1 gets decided by whoever types first.

The evidence points one way. That side's vocabulary is already **PASS / FAIL /
UNRUN** (per the dedup comment on #350) — and note that #681's four kinds have no
UNRUN state at all. A campaign aborted midway, a host eviction (#350 Open
Question 4), a worker lost: none of those are "non-zero exit, crash, timeout, or
missing artifact", yet all of them must appear in a total aggregate or the
denominator claim is false in exactly the way this issue exists to prevent. The
mature algebra already has the state this one is missing.

Recommendation: this task should **consume** the grading side's result algebra and
contribute only the two process-level observations that harness cannot make, with
the decision recorded on #350 the day it is made.

## One architectural fact this task silently settles for its upstream

AC-3's "non-zero exit" and "crash" presume jobs are **separate OS processes**.
That is a decision belonging to #676, not to its downstream consumer — and it is
not a free one. In-process dispatch is not currently available at any price:
`JLSStart` calls `System.exit` from dozens of sites including the batch branch,
and the batch path writes global mutable statics — `JLSInfo.sim = batchSim`
(`src/jls/JLSStart.java:247`), `JLSInfo.batch`, and most damningly
`JLSInfo.setLoadError` / `JLSInfo.lastLoadError` (`src/jls/JLSInfo.java:81-111`).
The single field carrying *why a job failed* is a static that N concurrent jobs
would clobber. So forking is right for now — but say so at #676, price the JVM
startup per job, and note that the outcome-record sidecar proposed above is the
thing that makes the forked child's knowledge survive the process boundary.

## What I would keep unchanged

- The outcome sentence, verbatim. It is the correct goal.
- AC-4 (byte-stability of failure rows under worker count) — non-negotiable, and
  free once rows come from a fold over the description.
- AC-2's reproduction standard, which the sidecar makes real rather than aspirational.
- The Boundary. "Not deciding why a simulation failed" is the right line; the fix
  is to ask the batch surface for its verdict, not to infer one.

## Verdict: rethink

The goal is right and belongs in JLS. The task as cut would, if implemented
exactly to its acceptance criteria, ship a campaign that reports a truncated
oscillating run as a success — the precise class of silent-100% failure it was
filed to prevent — while minting a second result vocabulary in a space #350 §7
says must be claimed by one side, and asserting a denominator in an aggregate that
does not yet exist. Concretely: fold #681 into #679 as one landing over a sealed
result type; replace AC-3 with an additive per-run outcome record emitted by the
batch surface plus killed/no-record observed by the parent; adopt the grading
harness's PASS/FAIL/UNRUN algebra rather than inventing a fourth-kind taxonomy;
and settle Open Question 1 before either lands.
