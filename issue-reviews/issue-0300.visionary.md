# Issue #300: CAP-06: one batch invocation turns 300 student submissions into deterministic per-student verdicts with counterexamples, replacing a three-line string diff
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the tier apparatus and CAP-06 is one claim: **JLS has a representation of
"what happened" and needs a representation of "correct."** That claim is right,
it is the sharpest unmet need on the headless surface, and it is aligned with
the whole arc of the fork — `docs/batch-interface.md` is already a frozen,
byte-deterministic output contract; `test/jls/VcdExportGoldenTest.java` and
`BatchSimulationGoldenTest` already compare runs to committed goldens; the
README already advertises the container image "for autograders and CI."
Everything is in place except the comparison, and the comparison currently lives
in Python outside the tool. Endorse the end.

The route is where I part company. The issue's spine is *"add an expectation
side to the `-t` grammar"* — invent a specification language, freeze it under
§6 forever, and then fund a 28-41 mw subcircuit-library program so the language
can be distributed. There is a route to the same outcome that needs no new
language at all, and the project has already written it down.

## The reframing: the reference circuit is the oracle

`docs/capability-roadmap/lf-04-formal-and-grading.md` (915 lines, present in the
tree at that path) proposes `-equiv reference.jls`: the instructor draws the
right answer — for the archetypal first-year assignment the reference *is* a
one-element `TruthTable` or `StateMachine` circuit — and JLS reports whether the
submission agrees, with a minimized counterexample rendered as a replayable `-t`
file. The capstone defers all of this as "proof grading," correctly noting the
four-state dependency for don't-care handling (§3, Open Question 3).

**But the reference-as-oracle framing does not need the solver.** Tier zero is
differential *simulation*: run the instructor's stimulus through the reference
and the submission in the same process, diff the traces.

```
jls -b -t lab3.t -ref ref_adder4.jls -cex out/alice.t -report out/alice.xml alice.jls
```

What that buys, against what the expectation grammar buys:

- **No grammar change.** `-t` is untouched, so AC-8 and KC-06-4 — a whole
  acceptance criterion and a kill criterion whose only job is to police an
  additive extension of a frozen contract — become vacuous rather than green.
- **A better counterexample, for free.** The trace diff localizes to
  *(time, signal, expected, observed)*. An expectation grammar can say "vector 7
  failed"; a trace diff says "at t=30, `cout` is 0, the reference has 1" and can
  emit the failing prefix as a `-t` file the student replays and single-steps.
  That is lf-04's counterexample rendering, delivered without a SAT solver.
- **Coverage stops being circular.** §3 flags — rightly — that the feature's own
  tests will define coverage the same way the implementation does, and KC-06-5
  exists to catch it. Under differential grading, coverage is not a number the
  grader invents: it is *the instructor's stimulus file*, byte-identical for
  reference and submission, reported as `lab3.t sha256:… — 256 vectors, 1,024
  input events`. The circularity does not get tested away; it disappears.
- **Upward-compatible with the real thing.** When the four-state core lands,
  `-equiv` is the same flag family, the same counterexample rendering, the same
  report channel, the same exit statuses. Vector-differential is lf-04's tier 0,
  not a parallel design that later has to be reconciled.
- **It is the pattern the tree already proved.** `riscv/verify.py:compare` builds
  a `problems` list by diffing registers and memory against a reference emulator —
  the most serious grading harness in this repository is already a differential
  scoreboard, written in Python "because the model has nowhere to put one."

## The exit-status table is already designed; do not design it twice

Open Question 2 calls the status extension "a one-shot design decision" and
recommends "one design pass, with room reserved." The pass has been made:
lf-04's batch-contract section allocates **3 = counterexample found, 4 = unknown
(solver/resource/BMC exhausted), 5 = not checkable**, and states that 4 and 5 are
never passes. CAP-06's Cost section removes the citation to
`lf-04-formal-and-grading.md` on the grounds that it "does not exist at any path"
— it does, at `docs/capability-roadmap/lf-04-formal-and-grading.md`. If the
demo slice ships status 3 as "wrong answer" in isolation and the equivalence
work later needs 3/4/5, the one-shot decision is broken on first contact.
**Take the table from lf-04 verbatim, including the meanings for 4 and 5, even
if only 3 is emitted in the first slice.** This is the single highest-value
change to the issue and it costs nothing.

## Consequence: half the required set is not about grading

FEAT-016 (#340, 3-5 mw) and FEAT-017 (#357, 25-36 mw) are **28-41 mw of the
51-81 mw standalone band** — the majority of the capstone — and their minimality
argument is entirely "a handout is a versioned library instantiated rather than
copied 300 times, so 'the same spec' in step 5 is verifiable."

Verifiability of "the same spec" is a *provenance* question, and provenance is a
digest. Print `spec: lab3.t sha256:…` and `reference: ref_adder4.jls sha256:…`
into every report and into the editor panel, and step 5's claim is checkable by
`sha256sum`, at roughly zero marginal cost. Divergent handout copies are then
*detected* rather than *prevented* — which is strictly what a grader wants,
since a student who edited the handout skeleton is exactly the case the
instructor needs surfaced. Nothing in §1 becomes impossible without #340 and
#357. **I am explicitly disregarding the minimality argument for those two rows**
and, with them, AC-4 (diffability of a handout-derived file), which is a
version-control ergonomics goal riding along in a grading capstone. Shared
subcircuit definitions are a good feature; they belong to #299, which already
funds them.

That leaves a required set of #369 (spine), #353, #354 (the run finishes),
#317 (it finishes on three platforms), #337 (it finishes without a display) —
around 21-36 mw standalone, and a capstone whose every row is about grading.

## Two design corrections inside the reframed slice

**AC-1 and Open Question 1 are in tension.** AC-1 demands byte-identical
`results.xml` across two machines and two JDKs; the recommended wire format is
xUnit XML, whose `timestamp`, `hostname` and `time` attributes are exactly what
every ingester expects and exactly what cannot be byte-identical across
machines. Emitting a degenerate xUnit that omits them trades ingestibility — the
one reason to choose the format — for determinism. **Split the artifact:** the
per-student *verdict record* (verdict, counterexample, stimulus digest,
reference digest, vector count) is canonical, sorted, timestamp-free and is what
AC-1 pins byte-for-byte; the xUnit file is a declared-nondeterministic envelope
around it. Determinism then belongs to one small object with no map iteration in
it, which also defuses §3's first risk at the root rather than by vigilance.

**Nothing owns per-submission fault isolation, and at n=300 that is certain to
bite.** `Simulator.runEventLoop` bounds *simulated* time (`now <= maxTime`,
`src/jls/sim/Simulator.java`); nothing bounds wall clock or heap. Directory mode
puts 300 untrusted student circuits in one JVM. Some will oscillate, some will
allocate, some will run to the time limit and produce nothing. §1's summary line
is `P pass, F fail, U unloadable` — there is no bucket for "exceeded its
budget," and AC-7's 30-minute figure is a whole-run budget with no per-submission
cap under it. The first real course run hangs on submission 47 and the
instructor learns nothing about the other 253. **A per-submission wall-clock and
heap budget, a fifth summary bucket, and a documented default (say 5 s) belong
in the demo slice**, not in FEAT-006's general long-run ergonomics.

## Grounding notes

- Every Cost band cites `docs/plan/features/FEAT-*.md:3` and AC-7 binds its
  reference machine to `docs/machine-calibration.md §2.1`. **Neither path exists
  in this checkout** (`docs/plan/` is absent; there is no `machine-calibration.md`).
  The issue applied exactly this test to two other citations and removed them; the
  numbers it kept rest on documents that fail the same test, which makes AC-7 and
  KC-06-2 unexecutable as written.
- The Abstract understates the shipped baseline by half: `autograde.py` grades
  two surfaces, the stdout lines *and* `EXPECTED_FINALS` parsed out of the VCD.
  Both are still one input vector, so AC-3's falsification point stands intact —
  but "a three-line string diff" is not what the tree contains.

## What I would build first

The 4-7 mw demo slice, re-aimed: `-ref`, `-cex`, `-report`, exit status 3 taken
from lf-04's table, a canonical verdict record, a per-submission budget, and a
directory operand. No new grammar, no library format, no GUI. Grade one real
lab with it — KC-06-3's own checkpoint — and let the shipped artifact settle
Open Questions 1, 2 and 4, which are currently being adjudicated in prose above
unwritten code. The comment history bears this out: six of seven ordering arrows
were wrong, two downstream edges were illegal, and #369 turns out to have no
incoming ordering edge from anything this capstone funds. Nothing is blocking
the spine. The bottleneck is not the graph; it is that the slice is not built.

Endorse the outcome, endorse the demo slice, redirect the mechanism from an
expectation grammar to a reference oracle, and cut #340/#357 out of the required
set.
