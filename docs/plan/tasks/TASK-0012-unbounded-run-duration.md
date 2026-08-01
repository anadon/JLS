# TASK-0012 - Unbounded run duration

**Status:** proposed | **Cost:** 2 d | **Blocked by:** none

## Deliverable

A run can be asked to end when the event queue drains rather than at a clock
value, and the default limit stops being an unannounced ceiling.

1. **A no-limit sentinel on the engine.** `Simulator.maxTime` is
   `protected long`, initialized to `JLSInfo.defaultTimeLimit`
   (`src/jls/sim/Simulator.java:38`; the constant is `100000000` at
   `src/jls/JLSInfo.java:69`). Add `public static final long NO_TIME_LIMIT =
   Long.MAX_VALUE` and let `Simulator.setTimeLimit`
   (`src/jls/sim/Simulator.java:104-107`) accept it. No loop change is needed:
   `while (!stopping && !eventQueue.isEmpty() && now <= maxTime)`
   (`:217`) and the post-poll check at `:230-233` are already correct at
   `Long.MAX_VALUE` — the second becomes unreachable, which is the point.

2. **A CLI spelling.** `-d` is `Arity.REQUIRED` with the description
   "set simulation time limit (a positive integer)"
   (`src/jls/JLSStart.java:766-767`), and the parser rejects `limit <= 0`
   outright (`:1069-1072`). Accept the literal operand `unlimited`
   (`-dunlimited` must work too — operands attach, `usageText()` advertises
   `-d10000` at `:1204`) and map it to `NO_TIME_LIMIT`. The numeric branch and
   its `<= 0` rejection are unchanged. Update the `FlagSpec` description in the
   same edit; `usageText()` is generated from the table so the help text
   follows automatically.

3. **The default stops being silent.** When a batch run ends because
   `now >= maxTime` and the limit was the default rather than one the user
   supplied, `BatchSimulator.displayOutcome`
   (`src/jls/sim/BatchSimulator.java:562-571`) additionally writes one line to
   **stderr** naming the default and the `-dunlimited` escape. stdout is
   untouched: the four outcome strings and their precedence are frozen by
   `docs/batch-interface.md:133-143` and §6 (`:324-336`).

4. **The GUI field does not lie.** `InteractiveSimulator` shows `maxTime` in a
   text field (`src/jls/edit/InteractiveSimulator.java:78`) and re-parses it
   through `NumericField.parse(window,tlimit,1,previous,"Time limit")` with
   `previous` clamped to `Integer.MAX_VALUE` (`:552-556`). With
   `NO_TIME_LIMIT` set, render the field as the word `unlimited` and accept
   that word back; do **not** let it display `2147483647`.

5. **`docs/batch-interface.md:51-52`** gains the `unlimited` operand. This is a
   §6 "addition that cannot break a conforming consumer", so it is CHANGELOG
   material and not a version bump.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-006 | "Unbounded" is the first word of the feature's capability sentence. A boot run is measured in 10^10-10^12 time units against a 10^8 ceiling, so without this every long run is truncated and reported as success. |

## Prerequisite tasks

None. Nothing here reads state another task creates.

## Acceptance test

`test/jls/BatchTimeLimitTest.java`, new:

- `defaultLimitStopsTheRunAndSaysSoOnStderr()` — runs a free-running `Clock`
  fixture with no `-d`, asserts stdout's last line is
  `Simulation Time Limit at 100000000` **byte for byte** (the frozen §3.1
  form) and that stderr names `-dunlimited`. Passes at HEAD for the stdout half
  and fails on the stderr half.
- `unlimitedRunsPastTheDefaultCeilingUntilTheQueueDrains()` — a fixture whose
  activity provably ends after time > `JLSInfo.defaultTimeLimit`, run with
  `-dunlimited`; asserts the outcome line is `Simulation: No More Activity at
  <t>` with `t > 100000000`. **Must fail at HEAD**: the parser rejects the
  operand with exit 2.
- `unlimitedIsNotAcceptedAsANumber()` — asserts `-d 0` and `-d -1` still exit 2
  with the existing message, so the sentinel did not widen the numeric domain.

`test/jls/CliFlagTableTest` needs no new case but **must stay green**:
`usageDocumentsExactlyTheParserFlags()` (`test/jls/CliFlagTableTest.java:81-91`)
scans `usageText()` for lines starting `"  -"`, and
`helpPrintsTheGeneratedUsageAndExitsZero()` (`:93-100`) compares the printed
help against `JLSStart.usageText()` exactly.

## Related GitHub issues

**No issue.** The default time limit is recorded in the study corpus
(`BRIEF.md` §2, last bullet) and nowhere in the tracker.

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on — any CPU-scale headless run in #202 exceeds `defaultTimeLimit` and is silently truncated without this |

## Notes

- **The guard clause that bites is `src/jls/JLSStart.java:1069-1072`.** It
  rejects `limit <= 0` *after* `Long.parseLong`, so a sentinel must be matched
  before the parse, not smuggled through as a magic number.
- **Do not reuse `0` or `-1` as the sentinel.** `docs/batch-interface.md:51`
  documents "a positive integer" and the parser enforces it; a negative
  spelling would make a typo an unbounded run on a grading machine.
- **`WaylandStartupCliTest#helpIsUnaffectedAndDocumentsTheEscapeHatch`**
  (cited from `src/jls/JLSStart.java:1188`) also pins the help text. Both CLI
  tests fail together on a description edit; that is intended, not a surprise.
- **`now = maxTime; break;`** (`src/jls/sim/Simulator.java:231-233`) is the
  same code TASK-0011 adjudicates. This task makes the branch unreachable under
  `-dunlimited`; it does not settle whether the drop is a defect under a finite
  limit, and must not be used to argue that it no longer matters.
- **Overflow.** `Long.MAX_VALUE` as `maxTime` means `now <= maxTime` can never
  be false, so a run without a queue-draining end never terminates. That is the
  requested behavior; TASK-0014 supplies the clean interrupt that makes it
  survivable, and the two should land in the same release even though neither
  blocks the other.
- **`InteractiveSimulator`'s two comparisons** at `:401` and `:657` use
  `now >= maxTime`. They are safe at `Long.MAX_VALUE` but must be read before
  editing, because the second drives the stop-reason label.

## Evidence

- `src/jls/JLSInfo.java:69` — `defaultTimeLimit = 100000000`.
- `src/jls/sim/Simulator.java:38` (field), `:104-107` (`setTimeLimit`),
  `:217` (loop guard), `:230-233` (post-poll limit check).
- `src/jls/JLSStart.java:97-98` (the `-d` field), `:249`
  (`batchSim.setTimeLimit(timeLimit)`), `:766-767` (the `FlagSpec` row),
  `:1061-1073` (the parse and its `<= 0` guard), `:1191-1210` (`usageText`).
- `src/jls/sim/BatchSimulator.java:562-571` — `displayOutcome` and its
  precedence chain.
- `src/jls/edit/InteractiveSimulator.java:78` (the field), `:552-556`
  (the int-clamped re-parse), `:401`, `:657`.
- `docs/batch-interface.md:51-52` (the `-d` contract), `:133-143` (§3.1 the
  four frozen outcome strings), `:324-336` (§6 the stability promise).
