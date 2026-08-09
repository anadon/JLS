# Issue #635: TASK-C562-1: a .dig file's embedded test cases become -t vector files whose bytes are the same every time they are regenerated
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this is really for

The end is not "JLS can write `.t` files from `.dig` XML". The end, stated by the
parent (#562) and the capstone (#513 PF-5), is that **an instructor's grading suite
survives the migration** — "the piece that actually converts courses". I judge that
end, and I think it is exactly the right end: it sits on the project's strongest arc
(a documented batch contract, a headless container image for autograders,
`docs/vcd-interop.md`, the `-equiv`/`-cex` line in `lf-04`). The goal belongs. The
artifact this task picks to carry it cannot carry it.

## The structural problem: `-t` is the half that doesn't need migrating

`docs/batch-interface.md` §2.2 has four productions and not one mentions an output.
`SigSim.initSim` (`/home/user/JLS/src/jls/elem/SigSim.java`) posts every value as a
`SimEvent` at parse time and compares nothing. #369 and #466 state this in as many
words: *"the verdict does not exist yet."*

A Digital test case is a table with **input columns and expected-output columns**.
The expected-output columns are the entire grading content — they are what encodes
"correct". Translating that table into a `-t` file keeps the stimulus and drops the
expectations, because `-t` has nowhere to put them. So the deliverable of #635 is
precisely the half an instructor could regenerate from a loop in ten minutes, and
the half that took them a decade to write is discarded at the format boundary.

This is not a gap TASK-C562-3 ("verdict parity") can close later. Parity needs
something to compare against, and the comparison channel is #466's `Expectations` /
`TestVectorRunner` / exit-status-3 work, which is itself behind #316, #321 and #347
via #369. **The load-bearing ordering edge for this task is #466, not #558.** The
machine block names only the parse dependency, so a scheduler reading it would fund
a 2–3 mw band against a prerequisite chain nobody priced here. Under KC-29-1 that is
how a format gets "downgraded to a documented external-conversion recipe" for a
reason that was visible at filing time.

## The second structural problem: nobody has designed the time model

Digital's test language is **row-indexed and cycle-based**. It has no time units. A
`C` cell means "pulse this clock and sample the outputs at the defined point in the
cycle". `-t` is **continuous-time**: `for d v` / `until t v`, absolute integer times,
values posted before time 0.

Translating one into the other means *inventing* a time discretisation and a
sampling instant, and then hoping the verdicts re-derived from continuous-time
observation land on the same rows. Every rounding choice in that invention is a
place where verdict parity dies silently — which is the one failure mode #562 AC-1
exists to prevent. Nothing in #635 acknowledges that it is designing a time model;
it reads as if the translation were a re-encoding.

## The third: the value domains do not meet

`docs/simulation-semantics.md:47` — "There is no unknown/X state anywhere in the
system"; `:56` — "There is no per-bit HiZ". Digital's test tables use `x` freely in
both input and output columns, and `z`. There is no way to write "don't care" for an
input in `-t` at all: `value` is a `BigInteger` that must fit the pin's width. The
review of #323 already found this wall on the `.circ` side; it is the same wall here,
and #635 does not mention it. If `x` in an input column routes to TASK-C562-2's
"untranslatable" bucket, a realistic Digital course loses most of its sequential
tests to a named loss — technically honest, and a migration nobody takes.

There is also a reach problem AC-3 half-notices: `-t` binds only **top-level**
`InputPin`s, and #466's expectations bind only the three-type stdout whitelist. A
Digital test that checks an internal signal has no home. JLS already has the right
mechanism sitting unused — **probes** (`BatchSimulator.findProbes`, VCD-only,
already deterministic, §4.1). Internal-signal expectations riding the probe channel
is a one-paragraph design that this task could unlock for everyone, not just `.dig`.

## Disregarding AC-1 and AC-2 deliberately

**AC-1** ("the existing batch runner accepts the files without hand editing") is
satisfiable by a translator that emits correct stimulus and zero expectations. It is
a green check that certifies the wrong thing. I would not ship against it.

**AC-2** (byte-determinism so regeneration never churns a repository) is a symptom,
not a requirement. Ask what regeneration is *for*. If the migration is one-way — the
premise of CAP-29 — the emitted vectors become the instructor's source and are never
regenerated; determinism is moot. If regeneration is routine, then `.dig` remains the
source of truth and JLS is a second-class runner for hneemann's format, which
contradicts the "Digital-successor play" this capstone exists to make. Determinism is
already a project-wide reflex (`DeterministicSaveTest`, the reproducible jar, the VCD
goldens, #466's P6/P7); restating it as a headline AC adds nothing and hides the
unanswered product question underneath it.

## The reframing I would fund instead

**(A) Cut at a test-case model, not at a file format.** Define one internal
`TestPlan` — an ordered list of rows, each row a set of driven assignments, a set of
expected assignments, a don't-care mask, and a clocking discipline. Then:

- `.dig` translation is a *parser into* `TestPlan`;
- `-t` + #466 expectations emission is *one serialiser out of* it;
- the #214/#466 panel authors into it;
- `lf-04`'s `-cex` counterexample writer already wants to produce one;
- Logisim's own test-vector format (`.circ`, scoped out by #323) and CircuitVerse
  become two more parsers, not two more bespoke translators.

The "which file do I emit" question disappears, the time model gets designed once
instead of once per importer, and TASK-C562-3 finally has a surface to assert parity
on. This is the same seam the project already chose elsewhere: one `TestVectorRunner`
behind two front ends (#466 H2), one report contract behind every importer (#556).

**(B) Interpret before you translate, and use the interpreter as the oracle.** Ship
a small evaluator for Digital's test language behind a new flag (`-digtest tests.dig`)
running on #466's runner and report. Verdict parity stops being a hope and becomes
near-definitional: you are executing the source semantics, so don't-cares, `loop`,
`let` and clock columns survive intact, and no derived files land in anyone's
repository — AC-2 evaporates because there is nothing to regenerate. Then make the
translator's correctness *checkable* by differential execution against the
interpreter, which is the discipline this project already uses for its hardest
correctness claims (`RiscvCpuGoldenTest` as a differential oracle, FEAT-034's parity
harness, the `-equiv` miter). Translation becomes an "eject" command an instructor
runs once, whose fidelity was proven rather than asserted.

**(C) Make the evidence the deliverable, not the directory.** What converts a course
is not a folder of `.t` files; it is one line of output: *"41 test cases: 38 translate
and reproduce Digital's verdicts, 2 use don't-care outputs (named, located), 1 uses
`loop` (named)."* That is a #556-contract migration report with vector files as a
byproduct — and it matches CAP-29 AC-1 ("zero unexplained losses") far better than a
runner that accepts files without complaint.

## Where the work does strengthen the arc

The instinct to make `.dig` tests first-class is right, and the boundary discipline
("this emits the runner's input format, it does not extend the runner") is the kind
of hygiene that keeps this repo's issue graph honest. AC-3's insistence that an
unresolvable signal fail loudly rather than emit a vector that silently never matches
is exactly the project's temperament (the loud loader, #314). AC-4 is right too. None
of that is in dispute. What is in dispute is that the chosen output format is
strictly weaker than the input, and no amount of care inside the translator fixes a
lossy target.

## Concretely

Re-aim this task as: **"a `.dig` file's embedded test cases become a `TestPlan`, and
a `TestPlan` runs to a verdict"** — parser plus model, ordered after #466, with `-t`
emission as a serialiser that falls out. Keep AC-3 and AC-4 verbatim. Replace AC-1
with "translated cases produce Digital's verdicts under `-check`", drop AC-2 to a
one-line hygiene note, and add an explicit AC for the time model and the don't-care
disposition. If #466 cannot land first, ship (B) — the interpreter — and let the
translator follow with the oracle already in hand.
