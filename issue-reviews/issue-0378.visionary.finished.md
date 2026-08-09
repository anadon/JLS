# Issue #378: TASK-0016: an hours-long test has a scheduled lane to run in, the required gate has a stated budget, and a large fixture has a declared home
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Its product is not a workflow file. It is the removal of a sentence — "we can't
write that test, it would make every PR take hours" — that currently blocks six
capstones (#317 `serves_capstones`). That purpose is real and worth serving, and
the lane/budget half of the task is straightforwardly good hygiene that the
project should have had since the first cron landed.

But the issue bundles three rules that only look related, and two of the three
are pointed at the wrong quantity. Below: one convention inversion, one
measurement error, and one reframing of what the lane is *for*.

## 1. The fixture cap measures a quantity the project does not pay

§ Threats to Validity is emphatic that the cap must be justified "in terms of
what a clone costs a student on a slow link, not ... a hosting provider's
limits." Correct instinct. But the enforcement (O6, P5, §7.10) is `ls -l` on the
working tree, and git does not ship working trees — it ships deflated blobs.
Measured at HEAD:

```
test/fixtures/riscv-sum1to10.jls   120,179 B on disk    10,415 B gzip -9    6,000 B xz -9
test/fixtures + test/resources     197,488 B on disk   ~23,422 B gzip -9 (whole corpus)
```

The entire corpus the policy is being written to police costs a student roughly
**23 KB** of clone. The single fixture that sets the cap number in Open Question
2 costs ~10 KB, not 120,179 B — a 12x error in the direction that makes the rule
look necessary. A cap on on-disk bytes is not a cap on clone cost for text
fixtures, and every fixture here is text.

This is not an argument that the rule is pointless — #317's own evidence names
the real hazard, "a 2.4 MiB kernel is a 33 MB `.jls` at 15.87 B/word," and that
one is genuinely large *even deflated*. It is an argument that the rule as
specified will be tuned against noise (a 120 KB fixture that is 95% air) and
will therefore land at a number chosen by taste, which is exactly the outcome §
Open Questions option (b) warns against.

**Concrete alternative.** Cap the *compressed* size, and let JLS's own file
format do the compressing. The project already ships three containers and a
`-savetext` flag for exactly this trade-off; today it stores its largest fixture
in the *least* compact of the three formats it supports. That is a defensible
choice (plain text diffs; `.gitattributes -text`, #111/#56) — but it is a
choice, and #378 never names it. The real policy decision is:

| tier | representation | rationale |
|---|---|---|
| small (diffs matter) | plain text `.jls` | reviewable diffs — today's default |
| large | XZ `.jls` (what JLS writes by default) | 20x here; loader already sniffs it |
| enormous / derivable | generated at test time from a committed recipe | #378's H3, kept |

Three tiers, no LFS, and Open Question 4 dissolves rather than "blocks
execution." Open Question 2's forced choice ("cap above 120,179" vs. "re-home
`riscv-sum1to10.jls` and entangle TASK-0025") also dissolves: re-saving that
fixture XZ puts it under any plausible cap without touching TASK-0025.

## 2. The source-of-truth is inverted against the project's own convention

§7.7 puts the cap number in `CONTRIBUTING.md` "beside 'Coverage ratchet'", and
P6 makes a test parse that prose. The section it sits beside says the opposite,
verbatim (`CONTRIBUTING.md:74-76`):

> The current floor values live in `pom.xml` under the `coverage-ratchet`
> execution — that comment block, not this file, is the source of truth for the
> numbers.

So #378 would place a second numeric ratchet next to the first, under the
opposite rule, and add a Markdown parser to the test suite to enforce the
inversion. It also adds a bespoke `pom.xml` XML reader (§7.5) for the group
lists. Two hand-rolled parsers for two numbers.

**Concrete alternative — one budget block, one ratchet.** The project is about
to have four numeric disciplines: coverage floors (pom), job timeouts
(TASK-0015), the gate budget, the fixture cap. Declare the latter three as
Maven properties beside the coverage floors —
`<jls.gate.budget.seconds>`, `<jls.fixture.max.bytes>`,
`<jls.job.timeout.default>` — with the same "raise-only, rationale in the
comment block" convention already documented and already understood by every
contributor. `CONTRIBUTING.md` gains one paragraph pointing at pom, exactly as
the Coverage ratchet section does. The ratchet test reads
`System.getProperty(...)` injected by surefire, not prose and not XML.

**I am explicitly disregarding P6 and §7.5 on these grounds.** P6 asserts a
coupling that should not exist; §7.5's pom parser is the cost of the coupling.
P4 (list-vs-substring in `excludedGroups`) survives and is a genuinely good
assertion — but it too is better written against the resolved surefire
configuration than against pom text, which is what § Falsification Criteria's
own "next move" for H1 already recommends. Make that the first move.

## 3. What the lane is *for*: an instrument, not insulation

This is the alignment question, and it is the one that matters most.

ARCHITECTURE.md records a decision with an explicit revisit trigger:

> **Simulation execution strategy: discrete-event interpreter is the sole
> strategy** ... **Revisit trigger:** a concrete CPU-scale design on the
> `riscv/` trajectory (#200/#201/#202) that is unusably slow interactively.

The hours-long runs #378 is building a home for — the #326 structural boot
above all — *are* that trigger firing. A required gate of 141 s and an
acceptance run measured in hours is a ~100x asymmetry that says something about
the simulator, not about CI. #378, as written, makes that asymmetry comfortable:
the run gets a lane, the lane goes red only on breach of a timeout nobody has
measured yet, and the pressure that would otherwise reach #221 is relieved.

That is not a reason to reject the task — the gate must be bounded regardless.
It is a reason to build the lane as a **measuring instrument**:

- The lane publishes its wall clock as a tracked series, not a one-time comment
  on this issue (§9). One number in an issue comment decays; a committed
  `longrun-timings` record does not.
- `CONTRIBUTING.md` (or the pom comment block, per §2 above) states a **trip
  wire**: when a long-run job exceeds N hours, that is the #221 revisit trigger,
  and the response is to file the levelized-pass issue — not to raise
  `timeout-minutes`. Without this, `timeout-minutes` becomes a ratchet that only
  moves the wrong way.
- The lane's `timeout-minutes` should *derive* from the source rather than be
  guessed. Replace P7's package whitelist (a rule that makes the issue's own
  goal — "an hours-long test is writable" — require editing a test to add one)
  with a required declaration: `@LongRun(expectedMinutes = N)` alongside the
  tag, asserted non-default by the ratchet. The lane's budget is then
  $\sum N_i$ plus headroom, computed, and a test that blows past its own
  declaration is a finding rather than a mystery. This answers §9's stated wish
  for "a measured basis rather than a guessed one" structurally.

## 4. An out-of-the-box alternative the issue never considers

FEAT-006 (#354), which #317 lists as `blocked_by`, is about long-run ergonomics:
a run that can be paused and that reports progress. Follow that one step
further and the multi-hour job stops being necessary at all.

JLS already serializes an entire circuit through the save/load path
(`CircuitSnapshot`, ARCHITECTURE "save/load pipeline"), and the save format
already persists the two things a mid-run state needs: `Memory` contents
(`init`/`initrle`) and `Register` init values. A **simulation checkpoint** is a
near neighbour of durable machinery that ships today, not new machinery.

With it, the #326 boot acceptance run is not an hours-long test. It is a chain
of gate-sized segments, each starting from a committed checkpoint and asserting
a differential golden at its end. Benefits over the lane:

- A failure localizes to a segment instead of "3 hours, red."
- Segments run **in the required gate**, in parallel, on every PR — which is
  what the capstones actually want and what a scheduled lane can never give.
- Checkpoints are exactly the artifact #221's equivalence criterion needs if a
  second execution strategy is ever built: bit-for-bit agreement checked at
  every segment boundary rather than only at the end.
- It converts the fixture question too: a checkpoint is a `Memory` dump, which
  is the same payload as a `$readmemh` image — and a `MEMORY ... IMAGE <path>`
  attribute (the standard HDL idiom, already implied by the Verilog-export
  roadmap in #33/#59) turns the "33 MB `.jls`" into a small circuit plus a
  `.hex` beside it. That is the single change that would make the entire
  fixture-size policy unnecessary for the case that motivates it.

I am not claiming this replaces #378 — the timeout/budget work stands on its own
and TASK-0015 is a prerequisite either way. I am claiming the lane should be
built **knowing** it may be a bridge, sized for a representative run (#317 Open
Question 1 option (a), which I endorse), and not sized for "every capstone's
acceptance run as they land" (option (b)), which would institutionalize the
thing that should be engineered away.

## 5. Smaller alignment notes

- **Cadence (Open Question 3).** The issue recommends nightly, offset "so three
  crons do not contend." Contention is not the criterion; *triage ownership* is.
  `mutation.yml` is weekly precisely because a single maintainer can triage one
  red per week. A nightly multi-hour red that nobody owns becomes a nightly
  ignored red in about three weeks. Recommend: **weekly + `workflow_dispatch` +
  a `run-longrun` PR label** so the lane can be aimed at the PRs that touch
  `jls.sim` instead of firing on the calendar. This also happens to be the exact
  shape H2 says transfers unchanged.
- **The `display` + `longrun` precedence rule (§7.10)** is well spotted and
  should survive any reframing — it is the kind of thing that silently costs a
  day.
- **The JaCoCo isolation (§7.11, H4)** is the sharpest thing in the issue.
  Keep it verbatim, including the requirement that the choice be justified in a
  pom comment.
- **Scope discipline** ("this task decides what a required lane may cost; it
  does not decide which lanes are required") is exactly right and should be
  preserved through any reframe.

## Verdict

**endorse-with-reframing.** Land the lane, the timeout budget, and the
JaCoCo isolation as specified. Reframe three things: put the numbers in
`pom.xml` beside the coverage floors and drop P6 and the prose parser; cap
compressed bytes with a three-tier representation policy (plain text / XZ /
generated) rather than on-disk bytes with an LFS question; and write into the
lane's own documentation that sustained occupancy is evidence for the #221
revisit, with an `@LongRun(expectedMinutes = N)` declaration replacing P7's
package whitelist so the lane's budget is computed rather than guessed. If a
simulation-checkpoint segmentation lands first (#354's neighbourhood), the lane
should shrink to a representative run and stay there.
