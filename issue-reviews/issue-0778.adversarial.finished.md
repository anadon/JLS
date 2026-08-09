# Issue #778: TASK-C588-3: each note's JLS claims ship a runnable appendix a stranger reproduces, and a claim found fixed upstream is retracted rather than defended
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C588-3 is the "reproducibility apparatus" slice of a three-task chain
(#774 prose, #776 prose, #778 apparatus+freshness) feeding FEAT-C588 (#588)
under CAP-36 (#520). The prose/apparatus split is a sound decomposition and
the underlying technical apparatus it needs (deterministic batch output,
golden VCD tests) already exists in this codebase. But the acceptance
criteria, taken literally, drop a scoping word that makes them contradictory
for competitor-sourced claims, the "freshness gate" the Outcome promises is
not actually gated by anything in the AC text, and the task's own boundary
line forbids it from touching the one thing its verification work will
routinely need to touch.

## Findings, most severe first

### 1. [High] AC-2's wording drops "JLS" and thereby demands the infeasible for competitor quotes

#778 states AC-2 as:

> "Each note has an appendix directory of committed fixtures and one
> documented command; running it on a clean checkout reproduces the quoted
> output, byte-for-byte where the claim is determinism"

Compare the source AC in the parent feature #588:

> "AC-2 — Every **JLS** claim has a runnable appendix. Each note ships an
> appendix directory of committed fixtures plus a single documented command;
> a reviewer running that command on a clean checkout obtains the output the
> note quotes"

#588 scopes the reproducibility requirement to claims *about JLS*; #778 drops
the word "JLS" and instead says "the quoted output" unqualified. Both notes
also quote competitor material verbatim under AC-3's citation regime — e.g.
#774 requires citing Logisim-Evolution's own concession that its CLI
verification docs are "incomplete and possibly misleading" (issue #1546). No
command run on a clean JLS checkout can reproduce a sentence written in
another project's GitHub issue. As literally worded, #778's AC-2 is
unsatisfiable for exactly the quotes AC-3 requires the notes to carry.
**Recommendation:** restore the "JLS claim" qualifier explicitly in #778's own
AC text (don't rely on the reader chasing it back to #588); state plainly
that competitor quotes are satisfied by AC-3's citation-to-source requirement,
not by AC-2's reproduction requirement.

### 2. [High] The freshness "gate" the Outcome promises is, by the AC's own text, only a promise

The Outcome section claims: "freshness becomes a gate rather than a promise."
But the corresponding acceptance criterion is:

> "A documented recheck step exists that re-verifies each competitor claim
> against a current release and records the date and version checked (AC-4)."

"A documented step exists" is satisfied by writing a procedure once; nothing
in the AC requires the step to ever run again, requires a cadence, or wires
it into CI. Contrast this repo's own conventions: `ARCHITECTURE.md` describes
freshness-style claims elsewhere enforced by named automated
checks — `HelpTopicsTest`'s link/reachability checker, the mutation-testing
weekly cadence, `FormatHeaderTest`. The sibling capstone this task serves,
CAP-28 (#512), states its analogous freshness requirement as an actual gate:
"AC-4: A regression beyond the band turns a scheduled lane red before the
published number is a lie." #778 has no equivalent — a "documented recheck
step" can be written, filed, and never executed again, and the AC as worded
would still pass. **Recommendation:** require either a scheduled CI job that
re-checks citation links/versions and fails on staleness beyond N months, or
at minimum a recorded first execution with a re-run cadence stated as a
testable fact (e.g., "recheck ran on <date>; next due <date>, tracked in
issue X"), not just a written-down procedure.

### 3. [Medium] "A review checklist enforces AC-5" is an unverifiable, unowned artifact

> "A review checklist enforces AC-5 — a note submitted with no named
> competitor advantage is rejected."

No location, owner, or mechanism is specified: is this a PR template
checkbox, a CONTRIBUTING.md section, a CI grep for a "Where they win"
heading? Every other enforcement point this issue and its neighbors cite
(`HelpTopicsTest`, `ElementConstructorContractTest`, `FormatHeaderTest`, CAP-
28's CI lane) is a concrete, testable artifact; this one is prose about
prose. A checklist that exists but that no one is required to consult
satisfies the letter of the AC while the real goal (no note ships without a
named competitor advantage) goes unenforced. **Recommendation:** replace with
a mechanical check — a small script/test that fails a note lacking a
recognizable "competitor advantage" section/heading, analogous to the
existing `HelpTopicsTest` completeness pattern.

### 4. [Medium] Boundary line forbids editing the one thing this task's own verification will find broken

> "Boundary: Reproducibility apparatus and freshness discipline; the notes'
> prose is TASK-C588-1 and TASK-C588-2."

The whole point of the appendix-and-recheck apparatus is to *discover* when a
claim no longer holds (upstream fix, drifted output, stale version). When it
does — which is the expected, not exceptional, outcome of a working freshness
gate — the retraction has to land in the prose, which this issue explicitly
disclaims ownership of. #774/#776 may be closed and merged by the time #778
runs (#778's own `ordering_after` puts it strictly after both). No process is
written down for reopening or amending #774/#776's shipped prose when #778's
apparatus flags it; in practice whoever implements #778 will either edit
prose outside their stated boundary or file yet another follow-up task,
neither of which the issue anticipates. **Recommendation:** either fold "file
a retraction PR against the note" explicitly into #778's scope, or add an
explicit reopening/handoff step naming who owns amending already-published
notes.

### 5. [Medium] Unacknowledged dependency: performance citations route through two more open, unshipped issues

Both #774 and #776 permit performance numbers to appear if "any that appears
cites #512 / #560." I confirmed both are open: #512 (CAP-28) states baldly
that "JLS publishes nothing" on performance today and that this is exactly
the gap it exists to close; #560 (FEAT-C28-4) is the not-yet-built same-
machine head-to-head harness. If either comparison note ends up citing a
number from #512/#560 (plausible — timing/perf framing is a natural fit for
a "timing honesty" note), #778's byte-for-byte reproducibility requirement
for that claim is unsatisfiable until those two issues ship. #778's own
`ordering_after: ["TASK-C588-1", "TASK-C588-2"]` does not list #512/#560,
unlike the parent feature #588 which does order after them.
**Recommendation:** either state explicitly that #778's notes must not cite
unshipped perf numbers (i.e., defer any such citation until #512/#560 land),
or add them to `ordering_after`.

### 6. [Low] Primary evidence for the required citations lives off-main, and #778 doesn't account for that

#588 (which supplies #778's citation list transitively) says the full
teardown evidence is "committed at `docs/reviews/evidence/2026-08-niche-
survey/` on `claude/jls-project-review-505pnf`" — I confirmed this directory
does not exist in this checkout (presumably tracking `main`/`master`; `find`
across the whole repo tree turned up nothing matching `niche-survey` or
`evidence`). If the notes' prose or #778's appendix fixtures need to draw
detail from that audit (beyond the numbered upstream issue links, which are
independently fetchable from GitHub), the source material isn't merged yet
and no task in this chain lists that merge as a dependency.
**Recommendation:** confirm whether the evidence branch needs merging before
#778 can be considered complete, and note it explicitly if so.

### 7. [Low] Band estimate looks tight against the stacked scope

`band_mw: 0.5-1` for #778 covers: two fixture-and-command appendices, a
recheck procedure, a retraction procedure, and a review checklist — four
distinct deliverables layered on top of two sibling 0.5-1 mw prose tasks it
must wait on. Findings 1-5 above are exactly the kind of ambiguity that
burns time before a contributor can even agree what "done" means for this
task; the estimate doesn't obviously account for that resolution cost.

## What's solid

- The prose-vs-apparatus split between #774/#776 and #778 is a clean,
  reasonable decomposition, and `ordering_after` correctly sequences #778
  after both prose tasks.
- The reproducibility machinery this task needs is not speculative: the
  codebase already has deterministic batch output (`docs/batch-interface.md`
  lines 215 and 249 state and pin byte-determinism) and golden-file
  infrastructure (`BatchSimulationGoldenTest`, `VcdExportGoldenTest` per
  `ARCHITECTURE.md`) that a "grading determinism" / "timing honesty" appendix
  can build directly on.
- KC-36-1, as quoted in this issue ("a claim found fixed upstream is
  retracted rather than defended"), matches the parent capstone #520's actual
  kill criterion text verbatim in substance — no drift there.
- The transitive dependency on CAP-06 (#300, open, and itself describing
  today's grading story as a fragile three-line stdout diff with no verdict
  exit status) is correctly handled: #774 lists #300 in its own
  `ordering_after`, so #778 inherits that gate rather than ignoring it.
