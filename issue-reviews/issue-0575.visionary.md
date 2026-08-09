# Issue #575: FEAT-C33-1: an instructor teaching from the standard text finds a lab already written for each chapter — starter circuit, exercise prose and grading vectors, ready to assign
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not eight labs. The real object is **an adoption artifact**: the thing an
instructor can pick up in an afternoon that makes switching simulators cost
less than staying. CAP-33 (#517) says this outright — "adopts a course, not a
tool" — and the market read behind it (#510: DEEDS is 32-bit Windows-only,
closed, single-author, and its `.pbs` files cannot be imported, so *the course*
must port) is the sharpest strategic claim in the whole capability set. I
endorse the goal without reservation. It is also the highest-leverage
non-code work available to this project: JLS's engineering surface is already
strong (normative `docs/batch-interface.md`, reproducible builds, signed
artifacts, a headless container for autograders) and its *content* surface is
empty. Four `.jls` files exist in the entire tree
(`/home/user/JLS/test/fixtures/{fork-4.6-shiftregister,headless-canary-gate,riscv-sum1to10}.jls`
plus `/home/user/JLS/riscv/gui/cpu.jls`), and `/home/user/JLS/resources/`
contains only `help` and `packaging`. A tool with world-class release
provenance and zero example circuits is mis-weighted, and this issue is the
correction.

Where I would cut differently is the *seam*. Four reframings, in descending
order of how much they change.

## Reframing 1: map to topics, crosswalk to textbooks

Binding the pack "chapter-by-chapter to the Donzellini Springer text" buys one
segment and creates three liabilities: edition churn silently invalidates AC-3
the day a second edition renumbers; the pack's *organization* is keyed to a
copyrighted work even when every word is original, which is exactly the
question AC-5's provenance audit will be asked and the least comfortable
version of it; and the pack is illegible to the far larger population teaching
from Harris & Harris, Mano, Wakerly, or Katz.

Invert the indirection. Labs declare **topics** from a small stable taxonomy
(the one AC-1 already implies: combinational → sequential → FSM → datapath, at
finer grain — `mux`, `adder-carry`, `moore-fsm`, `regfile`). A **crosswalk** is
a separate data file per textbook mapping that book's chapters onto topics:

```
kits/core/labs/07-traffic-fsm/lab.yaml      topics: [moore-fsm, state-encoding]
kits/core/crosswalks/donzellini-2e.yaml     ch. 7 -> [moore-fsm, state-encoding]
kits/core/crosswalks/harris-ddca-3e.yaml    ch. 3 -> [moore-fsm, ...]
```

AC-3 becomes: every lab declares its topics, and at least one crosswalk maps a
named text onto them. A chapter-to-topic table is a factual index, not
expression — the provenance question evaporates rather than being audited. A
second textbook then costs an afternoon of table entry instead of a second lab
pack, and the "instructor finds the labs already written" outcome extends to
every instructor rather than DEEDS migrators alone. Donzellini ships first
because it is the wedge; nothing about the wedge requires the coupling.

## Reframing 2: one circuit corpus, layered metadata — not three authoring projects

Four open issues each propose to bring circuits into the tree with different
outcomes and, as filed, different homes:

- #548 (Examples menu): ≥10 curated circuits, caption + suggested exercise,
  read from the classpath under `resources/samples/`, "authored clean for
  licensing."
- #552 (build-along lessons): stepped lessons over the *first three* of those.
- #575 (this issue): ≥8 labs with starter circuit, prose, vectors, in a kit
  directory.
- #577 (CSE 260M): a real corpus as fixtures, adapted into a kit.

The pass-1 boundary note correctly shows #575 and #578 are producer and
specification. But the deduplication was run over *outcomes*, and the cost here
is not the outcome — it is **authoring and maintaining correct circuits**, and
that cost is currently budgeted three times over three layouts, with two
provenance stories and two CI lanes.

The elegant cut is metadata layering over one corpus. A circuit is a circuit.
Add a caption and it is an Example. Add exercise prose, grading vectors and a
reference solution and the same circuit is a Lab. Add ordered steps and it is a
Lesson. Then #548 is "surface the corpus in a menu," #552 is "add steps to
three of them," #575 is "add prose, vectors and solutions to eight," and #577
is "a second kit in the same shape." One directory, one classpath rule (#130:
never `user.dir`), one loader, one CI lane that loads-simulates-grades
everything. This does not merge the issues; it stops them forking the substrate
underneath them. I would state that explicitly in this issue's boundary notes,
because #575 is the one with the most authoring weight and will otherwise set
the layout by default.

## Reframing 3: vectors are the student's feedback loop, not the instructor's grader

The issue frames the `-t` vector file as the artifact that "decides the
submission" — an instructor-side grading input. That undersells it and quietly
narrows the audience to people who already have an instructor.

Ship each lab so the *student* runs the check before submitting. #300 already
argues this ("the same spec runs in the editor's test panel... so the verdict
is not a surprise delivered a week later"), and the container image already
makes it a one-liner:
`docker run --rm -v "$PWD:/work" ghcr.io/anadon/jls -b -t tests lab.jls`. With
that framing the same eight labs serve a self-learner with no course at all —
a far larger population than DEEDS migrators — and they satisfy CAP-27's
on-ramp goal as a side effect rather than through separately authored lesson
content. It also changes the prose you write: "build a Moore FSM that does X;
run this to see how close you are" is shorter, more useful, and much easier to
hold to a stated time budget than assignment text written for a grader.

## Reframing 4: derive vectors from a reference oracle instead of hand-authoring them

The hidden cost in AC-1 is not the circuits or the prose — it is producing
*correct, discriminating* vector files eight times, plus AC-2's planted-defect
variants. There is a route the issue never considers, built entirely from
capability this repo already ships and CI already installs: JLS exports
structural Verilog (`-export out.v`), and `iverilog` is a documented CI
dependency (README, "Optional development tools"; the HDL-export validation
tests already compile generated Verilog with it).

So: author the *reference solution* circuit, export it to Verilog, and let a
differential harness generate input vectors and take the expected outputs from
the two engines agreeing. Hand-authoring collapses to authoring one correct
circuit per lab. AC-2's "planted-defect variant red" becomes nearly free —
mutate the reference (swap a gate, drop a carry) and the differential disagrees
immediately, which is also a far better defect than a hand-planted one because
it is the defect a student actually makes. As a bonus this exercises the
Verilog exporter against eight new designs, which is real validation the
exporter does not have today.

## The ordering trap, and how to make it work for you

AC-2 promises the labs "autograde out of the box on a tagged release." Today
there is no verdict: `docs/capability-roadmap/lf-04-formal-and-grading.md` puts
it plainly — the `-t` grammar has four productions and "not one of them
mentions an output"; the exit-status contract has three values and none means
"completed and wrong"; the shipped grading story is
`/home/user/JLS/examples/autograde/autograde.py` diffing three literal stdout
lines for one input vector. `ordering_after` therefore parks this issue behind
#300, and #300 is a 12–20 mw capstone.

Do not wait, and do not write the vectors twice. Write each lab's expectations
**in #300's expectations-file shape from day one**, and ship a thin evaluator
that runs them against today's documented stdout report (`batch-interface.md`
§3 — a stability contract, so the shim is safe). Then the content never gets
rewritten when the verdict engine lands, and #300 inherits eight real
conformance cases the day it starts. That inverts the dependency in the
project's favour: the labs become the requirement document for the grading
engine rather than a consumer stuck behind it. KC-33-1 already blesses this
posture for #576; it belongs here too.

## The one cost that deserves a recorded decision

`ARCHITECTURE.md`'s "Recorded decisions" section is this project's best habit,
and every entry there is a refusal to carry an ongoing tax without a requesting
user (i18n as non-goal, plugin loader removed, one simulation strategy). AC-4
introduces the project's **first editorial maintenance obligation** — human
non-author completion reviews, time budgets, labs pulled after two failures —
in a codebase that has so far maintained only code and specs. Who re-reviews a
lab when simulation semantics change? What happens when the maintainer has no
non-author available?

Make the recurring half machine-carried: a lab's ongoing health should be a CI
fact (reference solution green, mutant red, load + simulate on every change),
with human review a one-time gate at authoring rather than a standing duty.
That keeps AC-4's quality bar — which is the right bar, and KC-33-2's
"pull it rather than pad the count" is the correct instinct — without creating
a content-maintenance treadmill the single maintainer will silently stop
walking. The convention issue (#578) can specify the CI shape; this issue is
where the tax is actually incurred, so it should say which half is human.

## Verdict

**endorse-with-reframing.** The goal is right, well-evidenced, and the highest
non-code leverage available to JLS. Keep AC-1, AC-2, AC-4 and AC-5 as written.
Rewrite AC-3 from "declares the chapter it maps to" to "declares its topics,
with a crosswalk file mapping at least one named text's chapters onto them,"
and add an acceptance criterion that the labs reuse the same circuit corpus and
classpath mechanism as #548 rather than establishing a second one. Then take
the two free rides: derive vectors from the Verilog/iverilog differential, and
author expectations in #300's shape behind a shim so the pack ships before the
verdict engine and pulls it forward instead of waiting on it.
