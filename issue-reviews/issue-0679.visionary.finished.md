# Issue #679: TASK-C350-4: the same campaign at one worker and at N workers produces a byte-identical aggregate — scheduling is not observable in the result
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is right and it is squarely on the project's arc. JLS has, over the last
year, accumulated a determinism doctrine that it applies to every artifact it
emits: the jar and `bom.json` are bit-reproducible with a `.buildinfo` and an
independent-rebuild recipe (README, `docs/reproducibility.md`); the save format
is canonical — "a circuit's serialized form is a pure function of its content"
(`test/jls/DeterministicSaveTest.java`, #166), surfaced as `Circuit.stateHash()`;
the VCD emitter deliberately omits `$date`/`$version` so the bytes stay stable
(`docs/batch-interface.md` §4.2) and is golden-pinned byte-for-byte. #679 is that
same doctrine stated for a fourth artifact class: the campaign aggregate. Not a
new idea for this project — a fourth application of an idea it already lives by.

So I endorse the outcome. What I want to change is the route, because the route
as written buys the property with a test when the project's own idiom buys it
with structure, and because it invents a report format the project has already
chosen twice over.

Four reframings. The last two mean I am explicitly disregarding acceptance
criterion 4 as stated.

## Reframing 1 — make the aggregator a pure offline pass, and the theorem stops needing a test

The issue's Outcome sentence is a *discipline*: assemble by folding the job list,
look results up, never append on completion. AC-2 then asks a reviewer to verify
a negative — "no code path appends in completion order" — across a codebase that
also contains a thread pool. That is exactly the review burden that gets tired.

Cut the seam one notch further out instead. #677 already requires that every
per-job artifact land at a path that is a function of the job description and
that collection expose lookup-by-job. Given that, the aggregate is a pure
function of `(description, artifact store)` and nothing else. So make it one:

```
jls -campaign-report campaign.txt outdir/      # single-threaded, separate invocation
```

The reporter reads the committed description, reads the tree, folds, writes. It
imports no `java.util.concurrent`, receives no completion callback, and cannot
observe worker count, dispatch order or completion order *because it is not
running while any of that happens*. AC-2 becomes vacuously true — you cannot
append in an order you have no access to. The property #679 exists to defend
is no longer defended by a test; it is unavailable to violate.

Three consequences that are worth more than the tidiness:

- **Regenerating the report costs nothing.** An instructor who fixes a rubric,
  or wants the aggregate in a second format, re-folds the store instead of
  re-running 300 simulations. This is the difference between a campaign being a
  pipeline and being a database.
- **A killed campaign still has a report.** Given #350's `blocked_by: [354, 363]`
  and Open Question 5 (does the first landing ship without clean cancellation?),
  "the aggregate exists over whatever landed" is a materially better answer than
  "the run died before the aggregation step."
- **#350 invariant 6 (no AWT/Swing/`jls.edit`) comes free** — the reporter sits
  beside `BatchSimulator` in the headless core, already policed by
  `HeadlessCoreRatchetTest` and `ArchitectureRulesTest`.

The cost is that the aggregate is not available in-process at end of run. That is
consistent with how every other batch output already works (`-vcd`, `-i`,
`-export` all write files), not a regression.

## Reframing 2 — do not invent an aggregate format; the project already picked xUnit XML

AC-4 asks for a decision between "this format" and "the grading harness's", and
#350's Open Question 1 has been escalated twice (the 2026-08-08 REPLAN on #350
says a dedup pass has no standing to settle it). The question is framed as
ownership between two internal parties. It should not be.

`docs/capability-roadmap/sweep-04-verification.md` (L488) and
`docs/capability-roadmap/README.md` (L550) both already record the decision:
**xUnit XML as the primary report artifact** — "what CI and every LMS autograder
already ingests… plus a plain line format for humans and diffs. Both
byte-deterministic and golden-pinned, following `test/jls/VcdExportGoldenTest`."
And #686 (TASK-C524-1) is right now freezing an xUnit schema as a *pinned schema
artifact* inside the headless CLI contract, so a report can be validated
mechanically.

An xUnit `<testsuite tests="m" failures="k">` with one `<testcase>` per job in
description order gives #679 and its sibling almost everything for free:

| #350 requirement | xUnit mechanism |
|---|---|
| invariant 2, denominator is the job count | `tests="m"` is written from the description, not from results |
| #681 failure rows with inputs and output | `<failure type=…>` + `<system-out>`/`<system-err>` |
| order-independence | element order *is* document order *is* description order |
| no worker id / timestamp (invariant 1) | omit `timestamp=`/`hostname=` exactly as the VCD omits `$date` |

More importantly it dissolves Open Question 1 instead of answering it. §7's rule
— "whichever ships first owns the format, the other consumes it" — produces a
JLS-shaped format that the loser must adapt to. But **neither side should own
it**: both emit an externally-standard shape that Gradescope, Autolab, Jenkins
and GitHub Actions already parse without a JLS-specific reader. That is the same
delegation stance the roadmap takes everywhere else (Yosys/ABC/SymbiYosys do the
solving; JLS emits and consumes standard shapes). It also means the campaign
aggregate and the grading harness's report are *comparable artifacts*, which is
what #531's four-way `CrossPlatformScoreParityTest` needs anyway.

**I am disregarding AC-4 as written.** The right criterion is: "the aggregate is
an xUnit report validating against the schema #686 pins, plus a plain line format
for humans and diffs, both golden-pinned; the ownership question is answered by
recording that both features emit the pinned schema and neither owns it."

## Reframing 3 — a 1-vs-N diff is the weakest oracle available; use this repo's ratchet idiom

AC-1 is two samples with a committed seed. It catches a deterministic bug on the
run it happens to catch, and a 1-in-50 scheduling race passes the required gate
for a year. This project does not normally settle for that: `HeadlessCoreRatchetTest`
scans source text, `ArchitectureRulesTest` runs ArchUnit over bytecode precisely
because a text scan "cannot be fooled by mentions in comments or strings",
`NullMarkedRatchetTest` and `SocketConfinementRatchetTest` do the same for their
invariants. Enforcement by construction is the house style.

Two cheap additions worth more than the seeded diff:

1. **An ArchUnit rule over the reporting package**: no dependency on
   `java.lang.Thread`, `java.util.concurrent.*`, `System.currentTimeMillis`/
   `nanoTime`, `java.time.Clock`, `ProcessHandle`, and no `HashMap`/`HashSet`
   iteration on any path that reaches the writer. That is #350 invariant 1
   mechanically checked, and it survives refactors a golden file will not.
2. **Hash the aggregate**, following `Circuit.stateHash()`. Then 1-vs-N is a
   one-line comparison and can be run at 1000 randomized schedules in the
   scheduled lane rather than 2 samples in the required gate — which is what
   #350 invariant 5 asks for anyway ("long campaigns run in the scheduled lane").

## Reframing 4 — under 1+3, this task is nearly empty, and #681 folds into it

Look at what the four ACs actually contain. AC-1 is #350 §5 integration criterion
1, which #350 itself says spans dispatch, collection and aggregation and is
verified "at close-out" — not ownable by one task. AC-2 is a constraint on #677's
interface, already AC-2 there ("collection exposes a lookup-by-job interface; no
completion-ordered stream is offered"). AC-3 restates #350 invariant 1. AC-4 is a
maintainer decision no task can execute.

What is left after the reframings is: a fold over the job list emitting two
byte-stable serializations, plus goldens. That is real work, and it is well under
the 1–1.5 mw band; the slack is currently absorbing risk that belongs to #674
(what the description looks like) and #676 (subprocess-per-job vs in-process
dispatch, still undecided and noted in the #681 adversarial review). I would
restate the band at 0.5–1 and move the ownership decision to #674, whose own body
already says Open Question 1 blocks it.

And note that #681 stops being a separate hazard. Its rationale is that "append
successes, drop failures" passes every success-only test. Under a fold over the
description's job list with `tests="m"` written from the description, a job with
no artifact is a *failed lookup* and is forced to emit a row — the tempting bug is
unwritable, same as in reframing 1. #681 should shrink to the failure *taxonomy*
(what the row says: non-zero exit vs crash vs timeout vs missing artifact) and
land in the same change, not sequence ahead of it. The current
`ordering_after: ["TASK-C350-3", "TASK-C350-5"]` here versus `["TASK-C350-2"]` on
#681 is a symptom of a seam drawn in the wrong place.

## One alternative I will name and not recommend

The maximal version of reframing 1 is to not build a dispatcher at all: lower the
campaign description to a Makefile or ninja file and let `make -j N` dispatch.
Worker bounds (`-j`, `-l`), restart-on-eviction, per-job failure capture and
worker-count independence all come from a tool every lab machine already has, and
#676 collapses to a code generator over #674's format. It is genuinely on the
project's delegation arc. I do not recommend it as the mandate — JLS ships
Windows MSIs to students and a hard GNU make dependency is not acceptable there —
but #676 should record it as an evaluated alternative rather than reach for a
thread pool by default, because the campaign-as-build-graph framing is what makes
#683's multi-host scope a `-j`-style substitution instead of a transport project.

## Bottom line

Endorse the property; rebuild the route. Aggregation should be a pure offline
pass over the artifact store, emitting the xUnit shape the roadmap already chose
and #686 is already freezing, guarded by an ArchUnit ratchet rather than by a
two-sample seeded diff. That version makes the "entire correctness content of
#350" structurally unavailable to violate, settles the escalated ownership
question by taking it outside the project, and shrinks this task to something a
reviewer can check in an afternoon.
