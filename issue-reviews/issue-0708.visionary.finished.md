# Issue #708: TASK-C528-2: an example PrairieLearn question grades a JLS lab and returns per-test results in the platform's native format
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

The want underneath the YAML is one sentence: *an instructor at a PrairieLearn school
points a question at a JLS lab and students get per-test scores.* That want is aligned
with the project's arc. `docs/batch-interface.md` is already declared a stability
contract ("Scripts may parse these formats; JLS may not break them silently",
lines 3-10); `docs/vcd-interop.md` already blesses the subprocess-bridge grading
pattern and forbids live co-simulation (#63); `examples/autograde/autograde.py` is the
existing, embarrassing one-platform-less proof. Carrying that onto a real platform is
the right next move.

But #708 is not a delivery task. It is the place where two unresolved specification
holes surface, and building it as written would fill both by improvisation inside a
vendor-shaped kit — which is the one place they must not be filled.

## Hole 1: the declared integration surface cannot carry points or visibility

AC-4 reads: "Partial credit and the visible/hidden split are expressed by the lab,
mapped by the kit, and not invented by the adapter." Follow that requirement upstream
and it dead-ends.

- CAP-06 (#300) fixes the report artifact as **xUnit XML with a sidecar JSON for the
  counterexample** (Open Question 1, recorded as blocking #369). Its 58 KB body
  contains the words *score*, *weight*, *visibility*, *rubric* and *credit* exactly
  zero times. "Points" appears once, and not about grading.
- xUnit/JUnit XML has no per-test score, no max-points, no visibility. It carries
  name, classname, time, and pass/fail/skip. That is precisely why "every CI system
  already ingests it" — CI does not award partial credit.
- Every one of CAP-21's four target platforms needs exactly that missing information:
  PrairieLearn's results.json takes `points`/`max_points` per test, Gradescope's takes
  `score`/`max_score`/`visibility`, Classroom takes points, nbgrader scores cells.

So CAP-21 §1 step 4's central claim — byte-identical score *vectors* "derived from the
same xUnit output" — asks four adapters to derive numbers that the xUnit output does
not contain. Either the adapters invent the weighting (which AC-4 here explicitly
forbids), or the xUnit is extended with a JLS-private `<properties>` dialect (at which
point the "every CI already ingests it" rationale for choosing xUnit is spent), or a
rubric artifact must exist alongside the report. Only the third is honest, and nothing
in the CAP-21 tree owns it: #524 freezes the CLI and the xUnit *schema*, #531 compares
vectors, #706 builds an image, and #708 is asked to "map" a thing that has no producer.

This is not a sequencing nit of the kind the adversarial review catalogues. It is the
architectural seam of the whole capstone, and #708 is the first issue whose criteria
touch it.

## Hole 2: "static kit" and "lab-agnostic kit" cannot both hold

AC-3 wants a kit with "no lab-specific code … swapping the lab does not require
editing the kit's logic." The Boundary wants "static question kit only; randomized
generators are CAP-21 Open Question 4."

In PrairieLearn's static-question model those two are contradictory. A static question
is `info.json` + `question.html` as literal text: `externalGradingOptions` names the
image and entrypoint, and the student-facing upload element names the files it expects
(`<pl-file-upload file-names="lab3.jls">`). The submission filename, the point total,
and the test list are lab-specific literals sitting in the question directory. The only
way to vary them without editing the question is `server.py` generating question data
at render time — which is exactly the generator path Open Question 4 defers. A PR
chasing all four boxes at once will either quietly add `server.py` (breaking its own
Boundary) or quietly hard-code `lab3.jls` (breaking AC-3) and call it done.

There is a third way out, and it is better than either: **do not write the question,
emit it.**

## The design I would build instead

Three artifacts, in this order. None of them is a PrairieLearn artifact.

1. **A rubric section in the lab-as-data format** (owned by the CAP-06 lineage, #369 /
   #466 — not by a task under a PrairieLearn feature). Per test: id, weight/max points,
   visibility (`visible` / `hidden` / `after-due`), and the feedback policy for a
   failure. This is the single missing noun in CAP-21. Write it once, normatively,
   in the lab spec, next to `docs/batch-interface.md`'s existing normative sections.
2. **One score-vector emitter**: a pure function `(xUnit report, counterexample
   sidecar, rubric) -> ScoreVector`, plus a per-platform serializer of roughly fifty
   lines each. This is the seam the sibling reviews of #528 and #531 also land on, and
   Hole 1 is why it is not merely tidier but necessary: the join of report and rubric
   has to happen exactly once, or four adapters will each invent a rounding rule and
   #531's 300-submission four-way corpus becomes an experiment to discover which one
   guessed differently. With one emitter, parity is structural and #531 degrades to an
   ordinary golden test on a pure function.
3. **A question emitter**: `jls grade --emit-question prairielearn lab.yml -o
   questions/lab3/` writes `info.json`, `question.html`, and the `tests/` payload from
   the lab. This satisfies AC-3 in the only form PrairieLearn's static model permits —
   the kit contains no lab-specific *code* because it contains no code at all, only
   generated data — while keeping `server.py` out, so Open Question 4 stays genuinely
   deferred rather than smuggled in. Verification collapses to a golden-directory test
   over two fixture labs: no container, no platform account, no 300 submissions, no
   network. `-i out.png|out.svg`, `-export out.v`, `-vcd out.vcd` are the same idiom
   already: one computation, format chosen at the boundary, goldens per format.

Under that design #708's remaining work is a doc-test and a README — call it 0.1 mw,
not 0.5-1. The band in the issue is the cost of *inventing the rubric inside a vendor
kit*, which is the outcome to avoid.

## What I am disregarding, and why

I am disregarding AC-2 and AC-4 as written, and the issue's framing as a delivery task.

- AC-2 ("emitted in the shared parity-vector form, without adapter-specific
  normalization applied afterwards") describes a discipline that four independent
  adapters must remember. Replace it with a structural criterion: *the kit contains no
  code that computes a score* — it invokes the shared emitter, or it ships bytes the
  emitter produced. That is checkable by inspection and cannot rot.
- AC-4 is unsatisfiable through the currently declared surface (Hole 1). Rather than
  weaken it, promote it: file the rubric schema as its own issue in the CAP-06 lineage
  and make #708 order behind it. AC-4 then becomes true by construction — the kit maps
  fields that exist rather than deriving numbers that do not.

## Alignment with the project's arc

The project has a settled, good instinct for third-party interop and it is visible in
two places. `docs/batch-interface.md` is normative and ours; `docs/vcd-interop.md` is
"informative guide … nothing here adds to or changes it", a worked recipe over someone
else's ecosystem. And the maintainer has twice declined to take on third-party
custody obligations for a free university tool — no Apple Developer enrollment
(#128/#135), no project-held GPG key (#136), on the stated grounds that the obligation
outweighs the guarantee. Four in-tree, CI-gated, vendor-tracking platform adapters is a
category jump away from both instincts, taken without ever being argued. The
redirect above keeps the jump small: JLS owns the rubric, the report, and the score
vector — all normative, all testable offline, all ours — and each platform gets a
generated directory plus an informative recipe, in the shape `vcd-interop.md` already
proved works.

## Bottom line

The destination is right; #708 is aimed at the wrong artifact. Redirect it: file the
lab rubric schema (points, visibility, feedback policy) in the CAP-06 lineage, build one
score-vector emitter over `(report, rubric)`, and make the PrairieLearn "kit" a
generated directory whose regression test is a golden diff over two fixture labs. The
byte-identity claim then stops being a property to test and becomes a property that
cannot be false, and this task shrinks to the README it should always have been.
