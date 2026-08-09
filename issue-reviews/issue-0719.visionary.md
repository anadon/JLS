# Issue #719: TASK-C531-2: CrossPlatformScoreParityTest asserts the four adapters produce byte-identical per-student score vectors from the same xUnit input
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this task is really for

The stated deliverable is one assertion. The end it serves is instructor and
student trust: *the number in the LMS is JLS's verdict, unmodified* — no platform
rounded it, no adapter dropped a hidden test, no rubric got reordered in
transcription. That end belongs on JLS's arc. The project already sells trust
through reproducibility everywhere else (byte-reproducible jar + `bom.json` +
`.buildinfo`, `DeterministicSaveTest`, `BatchSimulationGoldenTest`,
`VcdExportGoldenTest`, cosign attestations), and extending that discipline to
grading artifacts is right.

But the *instrument* chosen — compare four platforms' native outputs to each
other after normalization — is the weakest available evidence for that claim, and
it is the one instrument the project's own idiom normally refuses. I would
disregard AC-1 and AC-2 as written and get the same end structurally.

## The knot at the center: AC-1 and AC-2 cannot both hold

AC-1 requires extraction "from each of the four adapters' native outputs." AC-2
requires that extraction be "defined once, in the fixture, rather than as four
per-adapter normalizers." Those four native outputs are Gradescope `results.json`
(points objects), the Classroom Action's summary (annotations + Classroom
points), PrairieLearn's `results` (a `score` fraction in 0..1 plus `gradable`),
and an nbgrader gradebook export (per-cell points in a gradebook DB). There is no
single format-agnostic extractor over those; there are four parsers, necessarily.
So AC-2 is either unsatisfiable, or it means "the *shape* and comparison
semantics are defined once" — which is a canonical score vector by another name,
and the moment you have one, the four-way comparison is the wrong test to write.

Worse, the normalization step is where a real divergence would hide, and its
correctness has no oracle inside this test. If PrairieLearn reports 0.875 and
Gradescope 7/8, the fixture's extractor multiplies one by the point total and the
two agree — but that agreement was *authored*, not observed. A test whose passing
condition is "the map I wrote is the map I wrote" has near-zero falsifying power,
which is exactly what AC-3's perturbation check cannot detect (perturbing an
adapter's output tests the parser, not the map).

## Reframing A (the main one): render, don't parse — make parity a theorem

`#466` already builds `GradeReport` as a byte-deterministic canonical artifact,
and argues in its own §7.10 that because two paths factor through one runner,
"verdict-list equality is a theorem rather than a test target." Apply that idiom
one level up.

1. JLS emits the canonical **`ScoreVector`** itself — a documented, ordered,
   per-student/per-test projection of `GradeReport`, produced by the frozen CLI
   (#524), not by adapter code.
2. Each adapter is required to be a **pure renderer** of that vector: its native
   artifact is a function of the canonical vector and nothing else.
3. Each adapter ships one local test: render the golden vector → compare native
   bytes to a golden native artifact; and parse the native artifact back → recover
   the canonical vector exactly (round-trip identity).

Four-way parity then follows by composition and needs no cross-platform
comparison at all. Concretely better in four ways:

- **Attribution.** A red `PrairieLearnAdapterRoundTripTest` names the guilty
  adapter in that adapter's own PR. A red `CrossPlatformScoreParityTest` says
  "four files disagree" and hands a maintainer four containers to bisect.
- **Ordering.** As filed, #719 orders behind #525, #526, #528, #530 *and*
  TASK-C531-1 — it is the last domino of a 12–17 mw capstone, and CAP-21's
  central claim is unfalsifiable until every adapter exists. Under round-trip,
  AC-1 is discharged incrementally, one adapter at a time, starting with the
  first.
- **KC-21-1 fires earlier and more precisely.** If a platform's contract forces
  rounding or reordering, the round-trip test fails at that adapter and names the
  lossy field — which is precisely the finding KC-21-1 wants escalated, delivered
  per-platform months before the fourth adapter lands.
- **It matches how this codebase already earns guarantees.** `CircuitSnapshot`
  gets undo-equals-save/load by *reusing the load path*, not by asserting two
  implementations agree. Same move here.

## Reframing B: the score vector is the wrong noun to freeze

`docs/capability-roadmap/lf-04-formal-and-grading.md` argues at length that
vector grading is "not merely weak but **wrong**" for don't-care specifications —
a student who exploits a `-` cell to save gates is marked down — and proposes
`-equiv`/`-cex`/`-formal-report` with xUnit output as the successor grading path.
So the thing #719 proposes to freeze byte-for-byte across four platforms is a
number whose *semantics* the project intends to change, and whose per-test
composition will grow counterexample artifacts, don't-care-aware partial credit,
and a fourth status class (CAP-21's status 3; `docs/batch-interface.md` §1 still
documents only 0/1/2).

Parity should therefore be asserted over the **verdict envelope**, not the score
scalar: per test, `{pass | fail | error | skipped}` plus counterexample presence,
then the score as a derived field. This matters today, not later:
`docs/batch-interface.md` §1 records that test-file parse errors print to *stdout*
and exit 1. Four adapters will render that submission as `0` — byte-identical
score vectors, perfect green — while one platform says "ungradable" and another
says "student scored zero." AC-1 as written cannot see the difference between
agreement and shared information loss. Equality of lossy projections is not
evidence of fidelity.

## Reframing C: add the properties a golden cannot express

Even with canonical vectors, goldens only pin what someone thought to record.
Cheap metamorphic properties catch the platform-side failures this task fears:

- **Monotonicity:** if submission A's passing-test set is a subset of B's, then
  every platform's score for A is ≤ B's. Catches rounding and rubric inversion.
- **Identity invariance:** permuting student ids permutes the vector and changes
  nothing else. Catches ordering-dependent adapters.
- **Scale law:** each platform's score is the declared affine image of the
  canonical points. States the normalization as a *checked law* instead of
  burying it in the fixture's extractor.

These are a dozen lines each, run over the corpus, and they falsify things a
300-row golden cannot.

## Reframing D: this is not a JUnit test

`CrossPlatformScoreParityTest` under `test/` implies the Maven suite orchestrating
Docker, an Actions runner surface, and a Python nbgrader install.
`ARCHITECTURE.md`'s test layout is JUnit 5, headless, no display, no daemon;
`mvn verify` is the single green gate contributors are told to keep. Pulling four
vendor runtimes into it pulls hard against that. The project's own precedent is
better: `AutogradeBridgeExampleTest` asserts over *recorded* artifacts, and
releases prove determinism with `SHA256SUMS` + `.buildinfo` rather than by
re-running the build inside a test.

So: adapter lanes produce canonical vectors as artifacts; a script emits a
per-student SHA-256 manifest; the Java side owns one small test that diffs the
manifest against a golden and reports the first differing student, test and pair
of values. The demanded diff quality (AC-3) is easier to deliver there than from
a four-way structural comparison, and the manifest is itself useful to instructors
who want to check their run against the reference.

## What I endorse without change

- **AC-4's anti-weakening clause.** "The assertion is not relaxed to make it pass;
  the finding is escalated as a REPLAN" is the single most valuable sentence in
  the issue, and it survives every reframing above. It deserves promotion from
  this task to a repo-wide norm for goldens and ratchets generally.
- **AC-3's diff quality bar** — name the student, the test, and the two values.
  Keep it verbatim; apply it to the round-trip failures instead.

## If this task is rewritten

It shrinks to: (1) consume the canonical `ScoreVector` (owned by #524/TASK-C531-1);
(2) assert the composition theorem — every adapter's round-trip test green implies
four-way parity — as a small manifest-diff test with a first-difference report;
(3) carry the three metamorphic properties; (4) keep the escalation clause. The
per-adapter round-trip proofs move into #525, #526, #528, #530, where they can be
written the day each adapter lands. That is well under the filed 1–1.5 mw band,
falsifiable from the first adapter rather than the fourth, and it proves a
stronger statement than "four numbers matched."
