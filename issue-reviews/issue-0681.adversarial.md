# Issue #681: TASK-C350-5: a failed job is reported with its inputs and its output, and the aggregate's denominator stays the job count
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

Failure accounting for the not-yet-built campaign runner (#350): a failed job
must appear in the campaign output as a failure row (inputs + captured
output), the aggregate's denominator must stay the total job count, four
distinct failure kinds must be represented (`non-zero exit, crash, timeout,
missing expected artifact`), and failure rows must be byte-stable across
worker counts/dispatch order. Nothing named here exists yet: `git grep -i
campaign -- src/` returns nothing at HEAD, matching #350's own evidence-commit
claim. This is one of six sibling tasks (#674, #676, #677, #679, #681, #683)
decomposing #350; I checked #681 against #350's body and against its two
direct siblings (#676 the local runner, #679 the aggregator) since #681's
acceptance criteria are written entirely in terms of them.

## Findings, by severity

**1. (High) "Timeout" as a failure kind contradicts the frozen batch contract as written, and the issue never disambiguates which "timeout" it means.**
`docs/batch-interface.md` §3.1 (lines 133-143) specifies `BatchSimulator.displayOutcome`'s four possible outcome reasons, and "Simulation Time Limit" (reaching `-d`) is reason 2 — printed with **exit 0** alongside a full results report, i.e. a *success* path under the current, explicitly "frozen ... any change requires a CHANGELOG entry and either a major version bump or a compatibility flag" contract (§6, lines 324-336). Issue #681 lists `timeout` as one of the four failure kinds that must be "distinguishable in the row, not collapsed into 'failed'" without saying whether this is (a) the existing `-d` limit reclassified as a failure — which would silently break the stability promise of §6 — or (b) a new, campaign-level wall-clock watchdog distinct from `-d` that doesn't exist anywhere in #350's `planned_tasks` or in this issue's own boundary. Either reading is a real problem: (a) is a contract violation, (b) is undeclared scope. Recommend: before coding, state explicitly which timeout is meant, and if it's (b), it needs its own line in #350's task table rather than being smuggled into "accounting only."

**2. (High) "Crash" as distinct from "non-zero exit" presupposes a subprocess execution model that no sibling task commits to.**
#676 (TASK-C350-2, the local runner) says only that jobs run "across one machine's cores through the existing batch surface" and that "a job's failure or hang does not corrupt or stall unrelated jobs" — a requirement equally satisfiable by an in-process thread pool calling `BatchSimulator`'s Java API directly (no OS process, no exit code) or by spawning `java -jar ... -b` subprocesses per job. #681's taxonomy only makes sense under the subprocess reading: "non-zero exit" (clean `exit 1` from `DefaultExceptionHandler`) versus "crash" (signal death / native JVM crash / OOM-kill, observable only as a process return code >128 or the process vanishing) is not a distinction that exists for an in-process call — there, a fatal JVM-level crash (segfault, true OOM) takes the whole campaign process down with it, unrelated in-flight jobs included, which cannot then be individually reported as a "byte-stable failure row." The issue treats this taxonomy as settled accounting work when it is actually gated on an architecture decision that hasn't been made in #350, #676, or here. Recommend: block this task on an explicit statement (in #676 or #350) of subprocess-per-job vs. in-process dispatch before the four failure kinds are implemented.

**3. (Medium) The Boundary line misattributes an authority that doesn't exist yet.**
"It does not decide *why* a simulation failed — that is the batch surface's verdict" reads as delegation to an existing arbiter, but per finding 1, the current batch surface (`JLSStart`/`BatchSimulator`, `docs/batch-interface.md` §3.1) has **zero** failure-kind vocabulary — all four of its outcome reasons are non-failure, exit-0 paths. "Missing expected artifact" is a concept that belongs to TASK-C350-1's not-yet-filed campaign description (expected artifacts named per job), not to the batch surface. So the classification logic for all four kinds has to be invented somewhere, and this issue's own scope statement quietly assumes it's someone else's problem when no task in #350's roster is named for it either.

**4. (Medium) Acceptance criterion 2 ("sufficient to reproduce the failure without re-running the campaign") is unfalsifiable as worded.**
> "Each failure row carries the failing job's inputs and its captured output, sufficient to reproduce the failure without re-running the campaign."

"Sufficient to reproduce" cannot be checked by any test that doesn't itself re-run the campaign — it's an aspiration, not a criterion. The tempting-but-wrong implementation this issue's own preamble warns about (dropping information) applies here too: a row that copies a truncated stdout tail and omits stderr, exit code, or elapsed time would pass any naive "does the row have an output field" test while failing actual reproducibility. Recommend replacing it with an enumerated, diffable field list (full stdout, full stderr, exit code or termination signal, wall-clock/elapsed time, and the exact inputs as passed to the batch surface) pinned by a golden test — the same rigor #350 demands of the aggregate itself.

**5. (Medium) Ordering vs. #350's own stated independence claim creates a latent dependency the issue doesn't acknowledge.**
#350 states: "failure accounting can be developed alongside collection — it consumes the runner, not the collector," and #681's `ordering_after` is `["TASK-C350-2"]` only (not TASK-C350-3, artifact collection/naming, #677). But acceptance criterion 2 requires failure rows to carry "captured output," and criterion 4 requires them to be "byte-stable... like every other row" inside the aggregate TASK-C350-4 (#679) assembles from TASK-C350-3's naming scheme. Whether a failed job's captured output is inlined directly in the failure row (consistent with the declared independence from #677) or written through the same artifact-naming path as successes (which would silently reinstate the dependency #350 says doesn't exist) is never stated. Recommend the issue say explicitly which.

**6. (Low) The four-kind taxonomy is presented as exhaustive with no closed schema and no "other" bucket.**
A free-text or ad hoc `reason` field with four expected substrings would satisfy "each represented and distinguishable" on the test data given while providing no guard against a fifth failure mode falling through uncategorized (e.g., a worker-pool exhaustion before a job ever starts, or a malformed *per-job* expected-artifact declaration that TASK-C350-1's reader didn't catch because it's not injective-collision but simply unresolvable at run time). Recommend a closed enum with a mandatory `unknown`/`other` bucket that a test asserts is exercised at least once, so a future fifth failure mode is caught as a gap rather than silently mis-bucketed as one of the four.

**7. (Low, feasibility) The 0.5-1 mw band assumes pure accounting logic on top of an already-decided execution model.**
If finding 2 resolves toward subprocess-per-job (needed to make "crash" meaningful), the real cost includes process supervision — capturing exit vs. signal termination, cleaning up orphaned children on a killed job, wiring a watchdog for finding 1's timeout — none of which is "accounting only." This isn't a defect unique to #681 (the whole band is stated as non-independent, per #350's cost section), but the band understates cost if the ambiguous architecture question lands on the heavier side.

## What's solid

- Acceptance criterion 1 (denominator stays `m`, not `m-1`) is concrete, directly testable, and traces cleanly to #350's integration criterion 2 and invariant 2 — verified against #350's body, the quoted text matches.
- The Boundary's scope cut (accounting, not root-causing failures) is the right instinct even though finding 3 shows the "batch surface" side of that split doesn't exist yet.
- Cross-references to #350 (invariant 1, invariant 2, integration criterion 2) are all accurate — no fabricated citations.
- Filing failure accounting as a separate task from aggregation (rather than folding it into #679) is a reasonable decomposition given the stated "tempting implementation" failure mode (append-successes-only).

## Recommendation

Do not start implementation until: (a) the subprocess-vs-in-process execution model for campaign jobs is settled (blocks findings 1 and 2 — this really belongs in #676/#350, but #681 cannot be scoped correctly without it), (b) "timeout" is defined precisely against the existing `-d`/outcome-line contract, (c) acceptance criterion 2 is rewritten as an enumerated, testable field list, and (d) the captured-output storage path (inline vs. through TASK-C350-3's artifact naming) is stated explicitly. None of these require re-architecting #350; they're clarifications that belong in this issue or a short comment on #676 before a PR is opened against it.
