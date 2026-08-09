# Issue #438: TASK-0012: a batch run can be asked to end when the queue drains, and the default ceiling stops being silent
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its mechanics, #438 asserts one thing: **a JLS run's answer must never be an
artifact of a bound the operator did not choose and cannot see.** That is a real claim about
what JLS should become, and it is aligned with the project's stated arc — `README.md` sells
batch mode to autograders and CI (`ghcr.io/anadon/jls -b -t tests circuit.jls`), and
`docs/batch-interface.md` is written as a *contract with a grading script*. A contract that can
silently truncate the thing being graded is the deepest kind of defect this project can have.
The escape hatch (a spelling for "no limit") is not optional work; without it the CAP-02/CAP-03
processor-boot arc that #354 names is simply not runnable. Endorse that core.

Three things are bundled here, and they are not of equal weight:

1. **The escape hatch** — sentinel + `-d unlimited`. Necessary, cheap, correct.
2. **Provenance of the limit** — say so when the *default* ended the run. Right instinct,
   wrong plumbing (below), and the issue's abstract overstates the disease.
3. **The GUI field stops lying.** Treated as a rider; it is actually the tell that the
   underlying model is wrong.

## The abstract overstates one thing, and understates another

Overstates: stdout is **not** silent. `BatchSimulator.displayOutcome` already prints
`Simulation Time Limit at 100000000` — one of four *distinct* frozen reasons
(`docs/batch-interface.md:133-143`), naming the tick. The run is not "reported as an ordinary
outcome"; it is reported as a time-limit outcome. What is genuinely missing is *provenance*
("was that limit mine?") and *discoverability* ("there is a way out"). Both are advisory, which
is exactly why they belong on stderr. Reviewers should not let the word "silent" buy more scope
than the evidence supports.

Understates: **the run exits 0.** `docs/batch-interface.md:48-49` tells grading scripts to
"treat exit status, not stream placement, as the failure signal" — and this task then puts its
new signal in precisely the stream the doc tells graders to ignore, while ruling out a non-zero
exit because it "would break every existing grading script." Both halves are available: an
**opt-in strict mode** (`-d strict`, or a `--fail-on-default-limit` flag) makes a
default-ceiling termination exit non-zero for consumers who ask for it, leaving #354's global
invariant 1 intact by construction. For the audience the issue names first — instructors
auto-checking in batch — the stderr line is not actionable and the exit status is. I would make
strict mode a completion criterion, or record explicitly why not.

## Reframing 1 (the main one): the limit wants to be a value, not a magic long plus a boolean

The issue's §7.4 exports `public static final long NO_TIME_LIMIT = Long.MAX_VALUE` and then, in
H4, contemplates threading a second "the limit was defaulted" signal from `JLSStart` into
`BatchSimulator`. That is two facts about one concept, carried separately, through four call
sites, none of which owns it. The GUI already demonstrates where that ends:
`InteractiveSimulator.java:78` renders `maxTime+""` and `:555` clamps through `int` — the field
lies *today* about nothing in particular, and the issue's own §11 lists "headless-vs-GUI
divergence" as an unmitigated threat. Adding a sentinel to a bare `long` makes a second lie
possible; it does not make the first one impossible.

Cut the seam at the value instead. A small immutable `jls.sim.TimeLimit`:

- `TimeLimit.defaulted()` / `TimeLimit.of(long)` / `TimeLimit.unlimited()` / `TimeLimit.parse(String)`
- `long ticks()` (`Long.MAX_VALUE` when unlimited), `boolean isDefault()`, `boolean isUnlimited()`,
  `String display()`

What falls out:

- **H4 dissolves.** `displayOutcome` asks `limit.isDefault()`. No new setter, no
  `timeLimit == 100000000` heuristic, and P7 (a user-supplied `-d 100000000` is silent) holds by
  construction rather than by care.
- **One parser, two front ends.** `TimeLimit.parse` serves both `JLSStart`'s `case "d":` and
  `InteractiveSimulator.setMaxTime`. §11's "headless-vs-GUI divergence" threat collapses from a
  standing hazard to a type invariant, and §11's "the GUI half is only human-verified" threat
  shrinks to the Swing wiring — the parse/render round trip (including `unlimited`, `-1`, `0`,
  `unlimted`) becomes a headless unit test next to `test/jls/sim/InteractiveSimulatorFieldTest.java`.
- **The stale `int` comment at `:552-556` is deleted, not amended.** The clamp has no reason to
  exist once rendering is the value's job.
- `Simulator` stays headless-by-construction (`HeadlessCoreRatchetTest`); `jls.sim.TimeLimit`
  imports nothing. `setTimeLimit(long)` stays as-is for existing callers and tests.

This is not more work than the issue's plan — it is roughly the same diff plus one ~50-line
class — and it deletes two of the five threats to validity the issue itself lists.

## Reframing 2: this is ceiling one of four; make it a shape, not a one-off

#354 names four ceilings. TASK-0013 is a *memory* bound; TASK-0014 adds a heartbeat and (per
FEAT-007) wall-clock lane timeouts. If each ceiling invents its own spelling, its own default,
and its own bespoke advisory line, JLS ends up with four unrelated dialects for one idea —
"a bound, who chose it, and how to escape it." The cheap move now is not to build a general
limits framework; it is to make this task's notice a *named seam* rather than an inline literal:
one package-private helper (`noteUnannouncedBound(what, value, escapeSpelling)`) that ceiling #2
and #3 extend instead of imitate. §7.5 currently specifies the opposite ("a private constant or
inline literal in `BatchSimulator`"). One method, and three future ad-hoc decisions become one.

Same reflex for the flag table: `-d` is becoming an enumerated-plus-numeric domain, and its
`FlagSpec` (`src/jls/JLSStart.java:770-771`) still describes that in prose that
`CliFlagTableTest` and `WaylandStartupCliTest` both pin. Carrying the accepted operand set as
data on the `FlagSpec` means the next flag widened this way does not repeat the amendment dance.

## Reframing 3: the unit is wrong for the audience, and a wall-clock cap kills this task's worst hazard

`docs/simulation-semantics.md:25-29` is explicit: "Time units are abstract; nothing binds them to
seconds." So neither an instructor nor a student can convert 1e8 ticks into "will this finish
before lunch," and the issue concedes that `-dunlimited` must be documented as requiring an
external `timeout(1)` until TASK-0014 lands — its own §11 calls it a foot-gun.

The humane control is a **wall-clock budget**, not an unbounded tick count: `-d unlimited
--max-wall 5m`. It bounds the resource commitment without bounding simulated time; it is
checkable in the `beforeEvent()` hook that #354 already identified as the pause seam
(`src/jls/sim/Simulator.java:252-255`); and it makes the "unbounded run nobody can stop" hazard
disappear *now* rather than waiting on TASK-0014's shutdown hook. If FEAT-006 wants to ship one
more thing in this task, this is the one — it is the difference between an escape hatch and a
trap door.

## Where the issue is right and should not be second-guessed

- **Open Question 1, option (a)** — `unlimited` as a `-d` operand, not a new flag letter. Right:
  one concept, one flag, and `usageText()` regenerates from the table.
- **Exact-literal match, no case folding, matched before `Long.parseLong`.** Right, and the
  ordering argument in §7.10 Stage 1 is the correct way to state it.
- **stderr, not stdout.** Right, given §6's freeze — and the strict-mode suggestion above does
  not violate it, since exit status is not stdout bytes.
- **H1 is safe, confirmed independently.** `grep -rn maxTime src/` shows every use is a
  comparison or the clamp assignment `now = maxTime`; there is no arithmetic on `maxTime`
  anywhere, so `Long.MAX_VALUE` cannot overflow. The interactive comparisons at `:401` and
  `:657` are `now >= maxTime` and are simply false under the sentinel, which is the wanted
  behaviour.
- **The GUI half is not cosmetic.** If #410 lands its recommended default (re-queue the
  past-limit event), "raise the limit and resume against the same queue" becomes a real
  interactive workflow — and the field that shows `2147483647` is then actively destructive, not
  merely untruthful. That is the strongest argument for the GUI work and the issue does not make it.

## Net

The goal is right and the project needs it. I am not disregarding the acceptance criteria, but I
would amend three of them: replace the naked `NO_TIME_LIMIT` sentinel plus H4 boolean with a
`jls.sim.TimeLimit` value shared by the CLI and the GUI (which retires H4 and two of the five
threats to validity); give the advisory a named emitter so ceilings 2-4 inherit it; and either
add an opt-in strict exit status or a wall-clock cap, so the audience named first in the issue
gets a signal it can actually gate on and the unbounded run is not a foot-gun on arrival.
