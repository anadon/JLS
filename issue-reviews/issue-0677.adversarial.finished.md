# Issue #677: TASK-C350-3: every per-job artifact lands at a path derived from the job description, never from dispatch order
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Grounding

This is one of six `TASK-C350-*` children of feature #350 (FEAT-057,
campaign execution/aggregation). `git grep -ic "campaign" -- src/` at
#350's evidence commit returns no hits, and a fresh grep of this checkout
confirms it: there is no campaign, job, worker-dispatch, or artifact-collection
concept anywhere in `src/` today. `docs/batch-interface.md` and
`ARCHITECTURE.md` describe only the single-circuit batch surface
(`BatchSimulator`, `src/jls/sim/BatchSimulator.java`, 574 lines). All six
sibling tasks (#674 TASK-C350-1, #676 TASK-C350-2, #677, #679 TASK-C350-4,
#681 TASK-C350-5, #683 TASK-C350-6) are open and unimplemented, so #677 is
pure design-on-paper with no code to anchor it against; every claim below is
checked against the issue's own text and its two named upstream tasks
(#674, #676).

## Findings

**1. (High) AC3 and AC4 contradict each other on what a second write to the same path means.**
AC3: "Running the same campaign twice produces byte-identical per-job
artifacts" — this *requires* a rerun to overwrite the same path with
(identical) content and succeed. AC4: "A job that produces no artifact is
distinguishable from a job whose artifact was overwritten... the
injectivity check from TASK-C350-1 is enforced here as well, at write
time" — this requires the write path to be able to flag an "overwrite" as
an anomaly. Both scenarios are, mechanically, "a write is attempted at a
path that already has content." Nothing in the issue specifies how the
write-time check tells a legitimate same-job rerun (must succeed, per AC3)
apart from a genuine cross-job collision (must be caught, per AC4) — e.g.
by job-id-tagged manifest, content hash, or ownership metadata. As written,
an implementation satisfying AC4 literally (fail on any second write) breaks
AC3, and one satisfying AC3 literally (always allow overwrite) makes AC4's
"defence in depth" a no-op. **Recommendation:** add a criterion that
specifies the disambiguating mechanism explicitly (e.g. "the collector
records the writing job's id per path; a write from a different job id to
an existing path is refused, a write from the same job id is permitted")
before this is handed to an implementer.

**2. (High) AC4's "distinguishable" is gameable — no observable signal is defined.**
"A job that produces no artifact is distinguishable from a job whose
artifact was overwritten" names an outcome but not a mechanism or a test
oracle: distinguishable *how* — an exception at write time, a status field
in a manifest, a log line, a sentinel file? A shallow implementation could
satisfy a naive test ("assert file absent when job has no artifact, file
present when it does") without ever detecting an actual overwrite, since
overwrite-detection requires state the criterion doesn't ask for (see
Finding 1). Combined, these two ACs describe a real, hard problem (identify
collisions and reruns) using language soft enough that the stated
verification could pass while the goal fails.

**3. (Medium) The "no completion-ordered stream" ban has no enforcement mechanism, unlike this project's own precedent.**
"Collection exposes a lookup-by-job interface; no completion-ordered stream
of results is offered to any consumer" is a negative API-shape constraint.
The codebase already has a pattern for exactly this kind of thing —
`HeadlessCoreRatchetTest` (ARCHITECTURE.md, "headless by construction") and
`NotificationRatchetTest` (`TellUser`, ARCHITECTURE.md "Error-reporting
contracts") — both are architectural ratchets with a standing automated
check. #677 states the constraint but proposes no analogous ratchet, so a
later PR can silently reintroduce a completion-ordered API (e.g. "just for
progress reporting") with nothing to catch it. **Recommendation:** either
name a ratchet test in the acceptance criteria or fold this into #679's
(TASK-C350-4) acceptance criteria where the consumer actually lives.

**4. (Medium) No atomicity/concurrency requirement, despite concurrent writers being the whole premise.**
#676 (TASK-C350-2, the runner this task is `ordering_after`) dispatches "N
workers... concurrently and independently" (per #350 §3). #677's write-time
injectivity check and artifact writes have no stated atomicity guarantee —
no mention of the temp-file-and-rename pattern `FileAbstractor.writeCircuit`
already uses elsewhere in this codebase ("wraps it in XZ and renames a temp
file over the target so a crash mid-write never destroys the previous
save," ARCHITECTURE.md). Two workers racing to write near-simultaneously to
paths that should have been rejected at read time (#674) but weren't (bug,
or a naming function with a subtle non-determinism) could corrupt each
other's output instead of failing cleanly — exactly the "defence in depth"
scenario AC4 claims to guard against, but the guard itself isn't specified
to be race-safe.

**5. (Low) Dependency link to TASK-C350-1 is implicit, not cited.**
`ordering_after: ["TASK-C350-2"]` is consistent with #350's own mermaid
graph (P2 → P3), so the sequencing is not wrong. But the acceptance
criteria repeatedly lean on semantics owned by TASK-C350-1/#674 (the
"job description," its "expected artifacts" field, and "the injectivity
check from TASK-C350-1") without linking #674 directly. Since #677 only
lists #676 in its machine-readable header, a triage pass or estimator
reading just the YAML block will miss that #677 is also load-bearing on
#674's still-undecided serialization format (#674's Open Question 2 is
unresolved). **Recommendation:** add `674` to a `depends_on`/`related`
field, not just prose.

**6. (Low, feasibility) Cost band looks tight for the surfaced subtlety.**
`band_mw: 0.5-1` (half a week to one week) is asked to cover injective
path derivation, a lookup-only collection API, a byte-identical-rerun
guarantee, and a write-time collision/overwrite detector that (per Finding
1) needs job-ownership bookkeeping the issue never asks for outright. Once
Finding 1 is resolved with an explicit mechanism, the band should be
revisited — "storage layout and lookup only" underestimates what
distinguishing rerun-overwrite from collision-overwrite actually costs.

**7. (Low) AC1's test crosses the stated boundary.**
"Boundary: Storage layout and lookup only" — reasonable, matches #350's
task decomposition — but AC1 requires "a test asserts that no artifact path
... varies between a 1-worker and an N-worker run of the same campaign,"
which is only exercisable once the runner (#676) exists and multi-worker
dispatch is real. That's an acceptable inter-task test dependency given the
`ordering_after` link, but it means #677 cannot actually be closed out
stand-alone — worth flagging so it isn't scored as "done" on unit tests
alone before #676 lands.

## What's solid

- AC1 (path is a pure function of job description; independent of worker
  count, thread, completion index, wall-clock time) is concrete, testable,
  and matches #350's global invariant 1 and integration criterion 1 exactly.
- The "no distributed transport in this task" boundary and the deferral of
  aggregation to TASK-C350-4 (#679) are clean, non-overlapping cuts
  consistent with #350 §2's stated rationale for splitting collection from
  aggregation ("collection fails by name collision, aggregation fails by
  being order-sensitive").
- VCD export is already deliberately timestamp-free (`BatchSimulator.java:376,420-423`
  — "no $date/$version headers"), so AC3's byte-identical-rerun goal is not
  undermined by an existing wall-clock leak in the artifact formats this
  task will wrap.

## Verdict rationale

The scope cut and single-run invariant (AC1) are sound, but AC3 and AC4 as
written specify mutually incompatible behavior for the exact case
("overwrite an existing path") that is this task's core hazard, and AC4
offers no way to fail a shallow implementation. This needs rework on the
acceptance criteria before implementation starts, not a full re-scope —
should-not-proceed would be too harsh given how close the rest of the issue
is to shippable.
