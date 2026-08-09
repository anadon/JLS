# Issue #759: TASK-C576-3: CI walks distribute → mutate one submission → grade → attribute, and a malformed or missing submission is a named result rather than an aborted cohort run
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the four checkboxes away and #759 is making one claim: **a cohort grading run
must be a total function from the enrolled students to results.** Every student gets
exactly one result; no input can reduce the domain; nothing about the run's mechanics
leaks into the output. "One broken upload must not cost the other 199 students their
grades" is that statement in prose. The end-to-end walk is evidence for it, not the
point of it.

That claim is worth building. It is also, almost word for word, an invariant another
open feature has already written down — which is the finding that matters most here.

## 1. The denominator invariant already has an owner, and #759 is about to become it

#350 (FEAT-057) §4 invariant 2: *"The aggregate's denominator is the job count in the
description. A failed job is present in the output."* §5 prediction 2: *"A failing job
does not shrink the denominator."* §3: *"Nothing may key an artifact name, an aggregate
ordering, or a report field on a worker id, a thread, a timestamp or a completion
index."* Those are #759's AC-4 and its denominator criterion, generalized from
`student` to `job`.

#350 §7 names the collision explicitly and refuses to leave it open:

> Two independent implementations of "run many things and collect the output" is the
> named failure mode. **Whichever of the two ships first owns the job description and
> the aggregation format, and the other consumes it.** … no resolution is not
> [acceptable].

#350 is `blocked_by: [354, 363]` — long-run ergonomics and checkpoint serialization,
neither small. #759 is banded 0.5–1 mw. **#759 ships first, so by #350's own rule #759
owns the job/result/aggregation vocabulary**, and #350's Open Question 1 is answered by
this task landing. #759's Boundary section does not mention #350 at all, and #576's
dedup comment separates them only on *dispatch* ("parallel across cores or hosts" vs.
"instructor-facing local convention") — a distinction that is true of the runner and
false of the invariant.

Concretely: define the cohort record as a job record with an instructor-facing
projection. A roster entry *is* a job description whose input is one `.jls` and whose
artifact is one report; the per-student result set *is* the aggregate. Name the internal
type in campaign terms (`JobId`, `JobResult`, denominator = |description|) and render
`student` at the boundary. Then when #350's runner arrives it substitutes a worker
source and nothing else changes, and #350 §2's "this is not throwaway work — the
capacity axis needs the same vocabulary" is bought two features early for free. Name it
`Student` all the way down and #350 builds a second one. **Post the ownership decision
on #350 as part of this task**; that is a five-line comment and it is the highest-value
artifact #759 produces.

## 2. The roster is a missing object, and #759 discovered it

The third and fourth criteria — "valid but does not correspond to any known student",
"denominator equals the expected roster size" — are unimplementable against #755's
layout as specified. #755 says only "how a submission identifies its student." Nothing
in it declares *who was expected*. Without that, the cohort is defined by what showed
up, which is exactly the failure mode #759 exists to prevent: a missing submission
cannot be missing unless something independent of the submissions directory says a
student was owed.

So the real input is a pair — `(roster, submissions/)` — and the roster is a committed,
diffable file, the same discipline #350 §3 requires of a campaign description and the
same discipline the save format is moving toward. That belongs in **#755**, not asserted
downstream in a test. #759's most useful act is to push one line upstream ("the layout
includes a roster naming the expected cohort") rather than to test around its absence.

With the roster present, the four criteria collapse into one property worth stating in
the spec and one test worth writing:

```
grade : (Roster, Dir) → Map<StudentId, Result>     with dom(result) ≡ Roster, exactly
```

Totality gives the denominator criterion for free. Unmatched files are the codomain's
complement — a separate, reported set, never silently empty. Three enumerated failure
cases become three inhabitants of one closed vocabulary, and the property is testable by
construction rather than by hand-picked scenarios.

## 3. The result vocabulary already exists; do not mint a second one

JLS has a fixed, tested taxonomy for "this file did not load and here is why":
`LoadError` with `IO_ERROR`, `NOT_A_CIRCUIT`, `MALFORMED`, `NEWER_FORMAT`,
`UNKNOWN_ELEMENT`, `ELEMENT_ERROR`, `LIMIT_EXCEEDED`, each carrying location, detail and
an actionable hint, published through `JLSInfo.setLoadError` under an explicit invariant
that *every front end shows the same message* (ARCHITECTURE.md, #58). "Malformed
submission" is not a new concept; it is `LoadError.Category` with a student id attached.
The per-student result should be a sum type over what already exists:

```
Result = Graded(verdict)            // #466's GradeReport payload
       | NotSubmitted
       | Unloadable(LoadError.Category, location, hint)
       | (and, outside the map) Unmatched(path)
```

An instructor who gets `Unloadable(NEWER_FORMAT)` learns the student used a newer JLS;
a bespoke grader string learns them nothing and drifts from the loader's own message.

**I am disregarding the "renamed" clause of AC-4 as written.** A renamed submission is
not a detectable state — the tool cannot distinguish `alice_final_v2.jls` from a
stranger's file without guessing, and a "renamed" verdict is a guess presented as a
finding. The honest report is the pair `NotSubmitted(alice)` + `Unmatched(alice_final_v2.jls)`,
which contains strictly more information and no inference. Keep the *scenario* in the
test corpus; delete "renamed" from the result vocabulary.

## 4. "A CI lane" is the wrong seam

Every job in `ci.yml` exists because `mvn verify` cannot host it: another OS
(`windows`, `macos`), a compositor (`gui-wayland`, `gui-x11`), Agda (`proofs`), native
packaging (`installer-reproducibility`). The cohort walk needs none of that — it is
headless subprocess work over the batch CLI, which is precisely what
`AutogradeBridgeExampleTest` already does: drive `examples/autograde/autograde.py` as a
subprocess against this JVM's classpath, `Assumptions.assumeTrue` out when `python3` is
absent, assert exit 0 and `PASS`.

A workflow lane would put the cycle outside `mvn verify`, outside SpotBugs and
warnings-as-errors, outside a contributor's local loop, and outside the instructor's
reach. The project's own recurring idiom is better and is proven twice: **ship the rig
the user runs, and self-test the rig's own classification logic** — `wayland-rig.sh` +
`wayland-rig-selftest.sh`, `autograde.py` + `AutogradeBridgeExampleTest`. Applied here:
`examples/autograde/cohort/` containing a real distribution tree, a roster, seeded
submissions (one right, one mutated, one truncated, one absent, one stranger), and the
grading driver an instructor actually copies; plus `test/jls/CohortWalkTest` driving
that same driver and asserting the map. The artifact CI exercises is the artifact the
instructor uses — which is also the only way the worked example stays true, the failure
mode `HelpTopicsTest` exists to prevent elsewhere in this tree.

## 5. One planted mutation is a weak oracle; the project owns better ones

`docs/capability-roadmap/lf-04-formal-and-grading.md` makes the argument against
single-point evidence in this exact domain: *"A submission that is wrong on 254 of the
256 possible inputs and right on that one passes."* AC-3's single mutation proves the
plumbing carries an id from disk to report. It does not prove attribution, and #759's
Boundary calls it "the end-to-end proof" — an overclaim that will be cited later as
evidence that grading works.

Two cheap upgrades, both from infrastructure already in the tree:

- **Attribution as a permutation property, not a scenario.** For any student *s* and any
  injected fault, the report names *s* and only *s*. Vary which student receives the
  fault, the roster order, and the filesystem enumeration order, and assert the report
  is invariant modulo the mutated student's identity. That is a handful of loops, and it
  catches the real bug class (off-by-one pairing of sorted rosters against sorted
  directory listings) that one planted mutation cannot.
- **Malformed submissions from `ContainerMutationFuzzTest`, not by hand.** That harness
  already generates 150 seeded byte-mutants per container format and asserts every
  outcome is either success or a classified `LoadError` with no escaping exception.
  Feeding its corpus through the cohort loop tests "does not abort" against hundreds of
  hostile files with deterministic seeds, for roughly the cost of writing one bad file
  by hand — and it is the same argument the loader already won.

And the invariant that actually protects the 199 students is stronger than "does not
abort": **the cohort report is byte-identical across runs, machines, and directory
enumeration order.** That is #757's own re-grade criterion, #466's `GradeReport`
determinism (P6/P7 — no timestamp, hostname or duration), #350's invariant 1, and this
repository's reproducible-jar culture, all the same value. Assert it here; it subsumes
"does not abort" (an aborted run is not byte-identical to a complete one) and it is what
lets a student's run be diffed against the instructor's.

## 6. Ordering: this task should be split by criterion, as its own parent already was

#576's 2026-08-08 correction re-pointed the feature's edges off #300/#369 onto #466 and
split them per criterion; it lists mirrors on #755, #369 and #300 — **#759 was missed**,
and inherits the stale #369 edge transitively through #757's `ordering_after: [C576-1,
369, 466]`. Worse, the whole of §2's totality property is testable against a *stub*
verdict function: the cohort loop's correctness is independent of what grading means.
So:

```yaml
ordering_after_by_criterion:
  AC-4 / denominator / unmatched: [755]        # ready as soon as the roster exists
  AC-3 end-to-end walk:          [755, 757]    # which carries the 466 edge
```

That split is the concrete scheduling payoff of the reframing: the half that saves the
other 199 students lands months before the verdict engine does, and it satisfies #576's
AC-5 degradation rule automatically — a total roster→result map is total in both modes,
and landing #466 adds fields to `Graded` rather than changing answers.

## What I endorse unchanged

The outcome paragraph, the refusal to let one bad upload abort the cohort, the insistence
that an unmatched-but-valid submission be reported rather than dropped, and the decision
to keep this service-free. Those are right, and the last one is the best thing about the
whole #576 family: "nothing that can die holding student work" is a real architectural
position, consistent with the single-self-contained-jar deployment model, and it should
be quoted in whatever docs this task touches.

## Verdict

**endorse-with-reframing.** The goal stands; the route changes in four places — own
#350's vocabulary and say so on #350; push the roster into #755 and restate the four
criteria as one totality property; build a shipped rig plus a Maven test instead of a
workflow lane, reusing `LoadError` and `ContainerMutationFuzzTest` rather than minting
new taxonomies and fixtures; and split the task by criterion so the robustness half is
unblocked today. One clause is dropped outright: "renamed" is not a result a grader can
honestly report.
