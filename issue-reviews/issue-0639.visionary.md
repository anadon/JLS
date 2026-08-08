# Issue #639: TASK-C562-3: the translated vectors return the same verdicts Digital reports, asserted mechanically over a real published circuit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the task framing away and #639 is making the strongest claim in the whole
CAP-29 migration arc: **a format importer is not done when circuits open; it is
done when the instructor's grading suite still says the same thing it said
before.** Every other task in this cluster (#635 emit vectors, #637 name the
losses, #558 map the elements) is an input to that sentence. #639 is the only
one that can fail in a way an instructor would actually notice on a Monday.
That claim is correct and it deserves to be the acceptance criterion the whole
capstone is judged by. My reframing is about route, not about goal — but the
route as written cannot be walked, so the reframing is not optional.

## The premise defect: there are no verdicts under `-t` to compare against

The Outcome says the harness "runs the translated vectors under JLS `-t`, and
asserts the verdicts agree." The Boundary note then insists "This asserts over
the existing `-t` runner; it does not extend it."

The existing `-t` runner has no verdicts. This is not a subtlety; it is stated
three times in this repository's own normative material:

- `docs/batch-interface.md` §2.2 — the `-t` grammar is four productions and not
  one mentions an output. `SigSim.initSim` posts stimulus at parse time
  (`src/jls/elem/SigSim.java`); nothing compares anything.
- `docs/batch-interface.md` §1 — the exit contract is 0/1/2, and none of them
  means "the run completed and the answer was wrong."
- `docs/capability-roadmap/lf-04-formal-and-grading.md`, opening line: "**JLS
  has no representation of 'correct.'**"

#466 (TASK-0111) is the issue that fixes this, and its abstract says it in the
same words: *"'per-vector pass/fail' is not a UI task over an existing verdict:
the verdict does not exist yet."* #466 builds `Expectations`, one shared
`TestVectorRunner`, a byte-deterministic `GradeReport`, `-check`/`-report`, and
**exit status 3**.

So #639 as written has exactly two executable readings, and both are bad:

1. **Grade the bytes.** Diff `JLSStart.displayResults` stdout against a recorded
   string. That is `examples/autograde/autograde.py`'s `EXPECTED_STDOUT_LINES`
   pattern, which lf-04 dissects as the defect: "a submission that is wrong on
   254 of the 256 possible inputs and right on that one passes." Building the
   flagship migration proof on the pattern the roadmap is trying to retire is a
   direct pull against the project's arc.
2. **Grow a comparator inside the `.dig` path.** A second parse-run-compare
   implementation living in the importer. That is precisely the drift #466's H2
   exists to prevent, and it would make #466's P3 parity theorem false the day
   it lands.

`ordering_after` lists `TASK-C562-1`, `TASK-C562-2`, `TASK-C558-5`. It is
missing the only dependency that actually gates the task: **#466**.

## Reframe 1 — the translator's output is a *pair*, and #635 is the issue to fix

The seam is cut in the wrong place upstream. A Digital test case is a table
whose header names signals and whose rows carry values; Digital decides
per-column whether a signal is driven or compared by looking up the pin
direction in the circuit. Stimulus and expectation are not separable halves of
the source — they are interleaved columns of one row.

#635 translates that into `-t` only. `-t` can express the stimulus columns and
structurally cannot express the expectation columns. So the expected-output
half of every single test case falls through to #637 and is dutifully filed as
a "named loss" — and then #639 asks verdict parity to hold over what is left.
The three tasks are individually coherent and jointly incoherent.

The fix is small and it is upstream: **#635 emits a `-t` vector file and a
`-check` expectations file per Digital test case.** One Digital row becomes one
stimulus segment plus N expectations. Then #639 becomes almost trivial to
state and to implement:

- "same verdict" = `jls -b -t v.txt -check e.txt circuit.jls` exits 0 where
  Digital passed and 3 where Digital failed.
- AC-4 ("which test, which vector line, which signal differed") is *free* —
  #466's `GradeReport` already carries `(expectation, observed, passed)` per
  testcase in xUnit XML, byte-deterministic and golden-able. #639 should
  consume that record, not invent a diff format.
- #637's loss report shrinks to what it should always have been: genuinely
  exotic constructs (`let`, `loop`, `repeat`, `bits`, `while`, don't-care `x`),
  not "we dropped the assertions."

## Reframe 2 — the real risk is timing-model reconciliation, not test-language fidelity

This is the part the issue never considers and the part most likely to burn the
1.5× stop-loss.

Digital's test rows are **untimed**: apply inputs, let the circuit settle,
compare outputs, advance. JLS is a discrete-event simulator with per-element
propagation delays (`docs/simulation-semantics.md` §6–7: gates 10, truth table
30, memory 100), a `-d` time limit, and expectations that — under #466's
proposed grammar — are sampled at an *absolute or relative instant*:
`expect ::= name ("at" time | "after" delay) value`.

Translating an untimed row into `at t` requires the translator to invent a
dwell constant per row and hope it exceeds the circuit's settling time. Choose
it too small and a correct circuit fails; choose it uniformly large and a
100-row suite blows the default time limit. Either way the failure looks like a
translation bug and will be debugged as one. A `C` clock-pulse column makes it
worse: the sample point must be after the edge *and* after the datapath
settles, which is a per-circuit quantity.

JLS already has the right concept and does not expose it as an assertion
trigger: `BatchSimulator`'s outcome line `Simulation: No More Activity` is
literally "the circuit has quiesced." The elegant route is a third `when` form
in #466's expectations grammar:

```
expect ::= name ("at" time | "after" delay | "when settled") value
```

`when settled` = sample at the first quiescent point at or after the preceding
stimulus change. That one production makes every row-based imported suite
translate exactly, with no invented constants, no dwell tuning, and no
sensitivity to a future delay-table change. It is also independently the right
primitive for hand-written combinational labs, which is most of the first-year
course load. **#639's most valuable output may be filing that grammar
requirement against #466 before #466 freezes §2.5** — the expectations grammar
joins the §6 stability promise the moment it ships, so this is a
now-or-major-version decision.

## Reframe 3 — get the negative direction from seeded mutation, not from luck

AC-2 is the best instinct in the issue: a suite that passes everything is a
failure of the task. But the stated route — find a real published `.dig`
circuit that has a *failing* test — is fragile by construction. Published
circuits pass their own suites; that is why they were published. The task will
either not find one, or will find one whose failure is an artifact of a Digital
version quirk, and AC-2 will get waived.

Better, and cheaper: **mechanical mutation of the imported circuit.** Import
the circuit, apply a small deterministic mutation (AND→OR on one gate, invert
one output pin, widen one splitter), and assert:

- the translated suite now reports FAIL, and
- it fails on the same test case and the same row that Digital's own suite
  fails on when handed the equivalently mutated `.dig`.

That is a two-directional oracle that does not depend on finding broken
published material, it scales to the whole corpus for free, and it directly
measures the property AC-2 actually wants: *the translated suite still has
teeth.* The repository has already run and adopted PIT
(`docs/mutation-testing-trial-2026-07.md`), so mutation-as-evidence is an
established idiom here, not a novelty.

Note also that #637's AC-4 already asks for "a mutation that drops it silently
fails the build." Making #639's negative control mutation-based aligns the two
tasks on one technique instead of two.

## Reframe 4 — the oracle should be generated, not transcribed

AC-3 wants Digital's verdicts as "a committed fixture with their provenance
(source circuit, Digital version), so the comparison is reproducible without
Digital installed in CI." Right requirement, wrong implementation implied: a
hand-recorded table of Digital's verdicts is a transcription, and transcription
is the exact failure mode AC-1 exists to rule out ("not by visual inspection").

This project already has a house pattern for external-tool ground truth, and it
fits without modification: `test/jls/hdl/ToolLocator.java` +
`YosysGroundTruthTest` / `IverilogCompileTest` / `GhdlCompileTest` — locate the
tool, `Assumptions.assumeTrue` skip when absent, run it, compare against the
committed corpus; CI installs the tool to arm the lane. Mirror it exactly:

- `scripts/record-digital-verdicts.sh` runs Digital headlessly over the corpus
  and *generates* the fixture, recording the Digital version it used. (Verify
  Digital's headless test entry point before designing around it; if it has
  none, the script must be a documented GUI-free recipe and the issue must say
  so rather than assume.)
- `DigitalGroundTruthTest` skips cleanly without Digital installed, and when
  armed asserts the committed fixture still matches — so a stale fixture is
  caught rather than trusted.
- The everyday CI lane asserts JLS's verdicts against the committed fixture, no
  Digital required. That is AC-3 satisfied *and* falsifiable.

Also: AC-1's "at least one real published circuit" is a floor that invites a
single fixture proving nothing about the translator's generality. Digital ships
its own corpus of `.dig` files with embedded tests under a GPL-compatible
license; a parameterized test over a committed subset costs the same to write
and measures something.

## The alternative I considered and reject

The out-of-the-box option is to **not translate at all**: make Digital's test
table a second front end to #466's `TestVectorRunner` — a reader that produces
verdict records directly from the `.dig` test sections. No emitted files, so
byte-determinism (#635 AC-2) becomes vacuous; no translation, so the loss
report (#637) collapses to "constructs the runner cannot express"; and parity
becomes a much shorter argument.

I reject it as the primary route because it defeats the capstone's actual
purpose: CAP-29's outcome is an instructor who can *leave* Digital, with
JLS-native artifacts checked into their assignment repository and diffable
there. A permanent `.dig`-test reader keeps the course coupled to the format it
is migrating off. But it is worth keeping as a **scaffold**: building the direct
reader first, then asserting the translated `-t`+`-check` pair produces the
identical verdict record, gives a far tighter differential oracle than a
recorded fixture — same code path, same runner, only the front end varies. That
is the same trick `docs/capability-roadmap` uses for the #202 RV32I golden as a
differential oracle for any future simulation strategy.

## Trajectory check

With the reframing, #639 pulls *with* the arc: it makes #466's verdict channel
load-bearing for a second, external constituency, it forces `when settled` into
the expectations grammar before it freezes, and it retires the string-diff
grading pattern in the one place where an outside tool's judgment is available
as ground truth. Without the reframing, it either re-blesses string-diff grading
or forks the comparator — both of which set the grading line back.

## The acceptance criteria I would write instead

I am explicitly disregarding AC-1–AC-4 as stated, because AC-1 and AC-2 name a
verdict the runner does not produce and the Boundary note forbids adding.

- AC-1': `ordering_after` gains **#466**; #635 is amended to emit a `-t` +
  `-check` pair per Digital test case. Neither task ships alone.
- AC-2': over a committed corpus of ≥3 published `.dig` circuits with tests,
  every case Digital passes exits 0 under `-b -t … -check …`, and the run is
  asserted per-case from `GradeReport`, not from stdout bytes.
- AC-3': for each corpus circuit, a deterministic seeded mutation makes the
  translated suite fail on the same case and row Digital fails on. A corpus
  with no failing direction fails the build.
- AC-4': the Digital-side fixture is produced by a committed generator script
  recording the Digital version; `DigitalGroundTruthTest` skips without Digital
  and re-derives the fixture when armed.
- AC-5': the dwell/sample-point rule is written down normatively (in
  `docs/batch-interface.md` alongside §2.5, not in translator comments), and a
  circuit whose settling exceeds the emitted `-d` fails loudly rather than
  reporting a wrong verdict.

## Verdict

**endorse-with-reframing.** The claim — migration is only real when the verdicts
survive — is the right claim and the right hill for this capstone. The stated
mechanism is unexecutable against HEAD, and the fix is to bind the task to
#466, move the expectation half of Digital's test rows into a `-check` file at
#635, get the negative direction from mutation instead of luck, and generate
the Digital-side oracle instead of transcribing it.
