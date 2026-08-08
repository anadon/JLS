# Issue #757: TASK-C576-2: one command grades a directory of submissions and returns per-student verdicts with counterexamples
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not a command. The end is that the *cohort* becomes a first-class object of the
batch interface: today `docs/batch-interface.md` §1 says batch mode "takes one circuit file
operand", `JLSStart.FLAGS` has 14 entries and none of them names a set, and the shipped
grading story (`examples/autograde/autograde.py:53-57`) is three literal stdout lines for one
input vector. Everything above the single run — how 300 files become 300 answers, how those
answers combine, what stays true when you run it again — is unowned. #757 is the first issue
in the tree that would own it.

That is a genuinely load-bearing seam and it deserves to be cut deliberately. The issue cuts it
in the least durable place available: at the invocation.

## The arc it sits in, and the duplication it does not yet see

The comment of 2026-08-08 resolved #757 against #466 and corrected the #369 ordering edge. Both
fixes are right. But three further claims on this same ground are untouched:

- **#300 CAP-06's own headline** is "one batch invocation turns 300 student submissions into
  deterministic per-student verdicts with counterexamples", and its 4-7 mw demo slice explicitly
  includes "a directory-mode invocation that grades a folder". That is this issue's Outcome,
  verbatim, one tier up and in a different lineage (#757 serves #576 → #517 CAP-33).
- **#350 FEAT-057** is dispatch-and-aggregate over "a parameter sweep, a fault-injection
  campaign, **a directory of student submissions**", and its §7 contains a standing instruction
  addressed to precisely this moment: *"Whichever of the two ships first owns the job description
  and the aggregation format, and the other consumes it… no resolution is not [acceptable]."*
  #757's Boundary says it "may consume" #350 but does not resolve the ownership question, which
  is the one thing §7 asks for.
- **#502 CAP-21** grades on Gradescope, GitHub Classroom, PrairieLearn and nbgrader — four
  platforms that hand a grader **one submission per container** and never invoke a directory
  command at all. Its parity claim (AC-1, byte-identical score vectors across four platforms)
  is a claim about *merging* per-student artifacts, not about a cohort driver.

Read together, those three say something the issue does not: the durable asset is not the
directory walk. Three different consumers need the same fold, and only one of them wants a
directory.

## Reframe 1 (primary) — ship the fold, not the driver

Define #757's deliverable as a **pure, order-independent fold from a set of per-student
`GradeReport` artifacts to one cohort report**, plus the thinnest possible reference driver
over it. In #350's own notation, already written down in its §3:

```
A = ⊕_{i=1..m} r(j_i),   r(j) ∈ {ok(a(j)), failed(inputs, output)},   path(j) = f(desc(j))
```

with `f` injective over the #755 submission layout and the fold's index set being the roster,
not the successes. Everything #757 wants then falls out instead of being tested for:

- **AC-4 (re-grade determinism) becomes compositional rather than empirical.** #466 already
  makes the per-student artifact byte-deterministic (its P6/P7: no `timestamp`, `hostname` or
  `time`). A pure fold over a set keyed by student id is deterministic by construction. Today
  AC-4 is a property someone has to remember to preserve inside a Python script.
- **#466's Open Question 6 (how many submissions in parallel?) evaporates.** It is only a
  question because the aggregate might be built by appending in completion order — the exact
  defect #350's §3 names. With a lookup-based fold, worker count is unobservable and the answer
  is "as many as you like", stated as a property rather than a default.
- **CAP-21's four adapters get the merge they actually need.** A Gradescope container produces
  one artifact; the cohort report is what the four platforms must agree on. Building the fold
  as a named, tested unit is what makes #502 AC-1 a diff instead of a re-implementation.
- **#350 collapses from a duplicate to a consumer.** Its local runner becomes a worker source
  behind the same job/artifact/aggregation vocabulary, and its multi-host scope is a worker-count
  change rather than a second implementation.

The driver — walk the directory, invoke `jls -b -t v -check e -report r.xml` per submission,
call the fold — is then genuinely thin, which is what the Outcome claims it is but the current
AC set does not make true.

## Reframe 2 — claim #350's vocabulary explicitly, in one sentence

#757 is smaller and closer to shipping than #350, so it will ship first, so under #350 §7 it
**owns** the job description and aggregation format. Say so. Concretely: the submission layout
of #755 is not a rival concept to a campaign description — it is a *generator* of one, via a
stated function `layout → job list`. Write that function down, emit the aggregate in the shape
#350 §3 specifies, and mirror the decision onto #350 as the `STATUS:` comment its §7 asks for.
This costs a paragraph now and removes a re-plan later. Leaving it as "may consume #350" is the
condition §7 exists to prevent.

## Reframe 3 — decide where the command lives, and prefer the jar

#466 §6 proposes `examples/autograde/lab-01/grade.py`; #757 says "one documented command" and
never names the artifact. That ambiguity is a real fork, and the project's own recorded reasoning
already decides it. `docs/library-survey-2026-07.md` rule 2 is "the self-contained jar is the
product", and `docs/capability-roadmap/lf-04-formal-and-grading.md` rejects the external-solver
path with the sentence that settles this one: *"an autograder that requires the instructor to
install Yosys or Z3 on the marking machine is not the same product as `java -jar jls.jar`."*
Python on the marking machine is the same objection wearing a different hat — and a Python
driver cannot be inside CAP-21 PF-1's frozen, conformance-tested CLI contract, which is where
every future integration is supposed to build.

Recommendation: the fold and the directory driver ship **in the jar**, additive behind new flags
(`-grade <dir>`, reusing `-check`/`-report`), with a `CliFlagTableTest` row, `docs/batch-interface.md`
§6-blessed and CHANGELOG'd — exactly the route #466 takes for `-check`. `grade.py` survives as a
worked example that calls it, not as the thing itself. This also keeps `jls.sim` headless and keeps
the cohort path inside the ratchet tests (`HeadlessCoreRatchetTest`) rather than outside every
guarantee the project has.

## Reframe 4 — the cohort report's real product is the class's misconception, not 300 rows

A per-student list is what a spreadsheet wants. What an instructor wants at 08:00 on Monday is
*"41 students failed `carry_out` at the same counterexample."* Once the per-student artifact is
byte-deterministic and carries its counterexample (#757 AC-2), grouping by `(failing test,
counterexample)` is a `group-by` over the fold's own input — tens of lines, zero new machinery.
It is also the one thing a cohort run can produce that no per-submission platform adapter ever
can, which makes it the strongest argument for this issue existing at all rather than being
absorbed into CAP-21.

It compounds later: when `lf-04`'s equivalence path lands, counterexamples are *minimized*
("the failure needs only `cin=1, a[3]=1`"), so the same group-by clusters on the mistake instead
of on one arbitrary witness of it. I would put the clustered view in the cohort report's shape
now — even if the first landing prints one cluster per distinct failure and nothing cleverer —
so the artifact does not have to be re-specified when the counterexamples get sharper.

## On the stated acceptance criteria

I keep AC-1, AC-2 and AC-4, with AC-4 restated as a property of the fold (and, per the 2026-08-08
comment, answered for *across machines* — #502 AC-1 and #531 both need the strong reading).

I would disregard **AC-3's degraded mode as written**. Against today's three-exit-status contract
there are no tests, no verdicts and no counterexamples — only stdout bytes — so a degraded cohort
command is `autograde.py` at 300× scale: the exact artifact CAP-06 exists to kill, published as
an instructor-facing deliverable and given a documented format that someone will then depend on.
The layout and the fold are verdict-agnostic and can ship early; the *verdict* cannot be
synthesized. Degraded mode should therefore fold real per-student results whose verdict field is
"no verdict channel available (exit 0/1/2 only)" — never a pass or a fail. That keeps KC-33-1's
intent (this issue is not held hostage by the verdict slice) without shipping a grader whose
answers are wrong in the specific way the whole capability was filed to fix.

## Verdict

**endorse-with-reframing.** The seam is real, the boundary discipline is good, and the issue is
correctly small. Cut it one layer lower: own the deterministic fold and the aggregation format
(claiming #350 §7's ownership explicitly), put the command in the jar rather than in Python, let
the directory walk be the thin part it claims to be, and shape the cohort report around clustered
failures rather than a roster dump.
