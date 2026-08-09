# Issue #377: TASK-0022: the per-cycle active fraction stops being "never measured" — a two-cycle machine, an internal clock, and per-callback event attribution yield α, CPI and k with their method
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The task is well-formed as a document — hypotheses, falsification criteria,
and a data-transformation model are all present, and its purely code-level
observations (O4–O8) are accurate against HEAD. But its central evidentiary
premise rests on a document, `docs/machine-calibration.md`, that **does not
exist anywhere in this repository's reachable history** — not at HEAD, not
on `origin/master`, and not even on the one branch where the evidence commit
it cites actually lived. The task instructs the implementer to edit specific
sections of a file that isn't there, and one of its own acceptance
predictions (P5) is fit to the data after the fact, so it cannot fail.

## Findings, most severe first

### 1. The primary evidence document does not exist in this repository — the task's core premise is unverifiable and its Method section is inexecutable as written

The whole abstract, O1–O3, and every numeric figure (α = 0.18/0.40/0.56,
k = 1.07/1.8, events/cycle = 388.4/245.5/121.5/243.1, "468 ev/instr") are
quoted from `docs/machine-calibration.md` at `evidence_commit:
2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`. Checked against the actual repo:

```
$ ls docs/machine-calibration.md
ls: cannot access 'docs/machine-calibration.md': No such file or directory
$ git log --all --oneline -- docs/machine-calibration.md
742da74 docs: remove the planning corpus now that it is encoded in issues
b299d63 docs: reconcile the parity study across documents...
64c137d docs: virtual-hardware/virtual-logic parity study...
$ git log origin/master --oneline -- docs/machine-calibration.md
(empty — the file never existed on origin/master's history at all)
$ git merge-base --is-ancestor 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7 origin/master && echo YES || echo NO
NO
$ git merge-base --is-ancestor 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7 HEAD && echo YES || echo NO
NO
```

The evidence commit `2d0ca9d` is real (it exists as a commit object and the
file is readable from it directly), but it sits on none of the branches
that matter: not `HEAD`, not `origin/master`. The one branch that ever
carried `docs/machine-calibration.md` (`claude/jls-virtual-hardware-linux-njsoma`)
subsequently deleted it in commit `742da74` ("remove the planning corpus now
that it is encoded in issues"). `docs/plan/` (home of the also-cited
`docs/plan/evidence/BRIEF.md`) likewise does not exist anywhere reachable
from `HEAD`.

This is not a cosmetic citation problem. §8 (Method) item 8 literally
instructs: *"Write α, CPI and k into `docs/machine-calibration.md` §6.1 and
§6.2 in place of 'never measured' and 'unexplained'."* There is no file to
edit "in place" of anything — the implementer must first decide whether to
resurrect a 1,124-line document from a commit that is not an ancestor of
their branch, or write a new one from scratch, and the issue gives no
instruction for that choice. The completion-criteria line *"Every cited
evidence document and permalink resolves on the default branch at close"*
is therefore false **on day one**, for the issue's own headline citations,
before any work happens — the task fails its own DoD item by inspection.

Sibling/parent issue #335 (FEAT-009) has the identical problem: same
`evidence_commit`, same references to the same absent document. This
appears to be a repo-wide pattern in this batch of filed issues, not unique
to #377, but it is squarely this task's problem since the whole abstract is
built on quoting that file's line numbers.

**Recommendation.** Before any of §8's work starts: locate and commit
`docs/machine-calibration.md` (and confirm it is genuinely at `2d0ca9d`'s
content, not a reconstruction) on the branch this work will land on, or
explicitly re-scope the task to create the document fresh and drop every
"in place of X" instruction that presumes prior content. Either way, the
`evidence_commit` pin needs to be replaced with a commit that is actually
an ancestor of the target branch, and the DoD's "resolves on default
branch" checkbox needs to be treated as currently failing, not assumed.

### 2. P5's acceptance band is fit to the measurement it's supposed to gate — the criterion cannot fail

P5 says: *"Compute α from P3 and P4 and assert it falls inside the band the
measurement commit writes into `docs/machine-calibration.md`. Observe:
inside."* Open Question 3 then supplies the method for setting that band:
*"set the band from the observed run-to-run spread on one platform and
record the spread beside it."*

That is: the same commit that measures α also defines the pass/fail band
for that measurement, from the measurement's own observed spread. There is
no independent target — the "test" is guaranteed to pass by construction,
because whoever writes the band writes it after seeing the number. This
directly contradicts the task's own stated rigor elsewhere (H4: *"No number
is adjusted to make a consistency check pass"*) — P5's band-setting method
is the mirror-image problem: not adjusting the number to fit the band, but
adjusting the band to fit the number, which is equally void as a check. As
written, P5 is not falsifiable and should not count as verification that α
was "measured" in any meaningful sense — it only checks that the test
author transcribed the number they just computed into a band around itself.

**Recommendation.** Fix the band from something external to this
measurement run — e.g., the pre-existing 0.18/0.40/0.56 candidate spread
that O1 cites, or a cross-platform spread computed from at least two CI
legs before the band is frozen, not "one platform, this run." If the band
must be set from this run's own data, say explicitly that P5 is a
regression guard for future changes, not evidence that the measured α is
"correct," and stop presenting it as a verification predicate.

### 3. The task overrides its parent feature's own dependency graph without amending the parent

#335 (FEAT-009)'s own mermaid graph draws `N353 --> T22` — i.e., #335
itself asserts that #353 (FEAT-005, the quadratic-parse fix) blocks
TASK-0022 specifically. #377 (this issue, = TASK-0022) instead declares
`blocked_by: []` and spends a full paragraph arguing the parent's implied
edge doesn't apply ("that reasoning is about wall clock... this task
measures event counts"). Whether or not the argument is correct, the
parent's graph is not corrected in the same breath — #335 still shows the
edge in its diagram, #377 unilaterally overrides it, and nothing in either
issue's DoD requires reconciling the two. A reader who trusts #335's
diagram and a reader who trusts #377's prose will draw opposite conclusions
about whether this task can start today, and #377's own quoted rule
("Sequencing claims must be real dependencies, not preference (D10,
obligation 5)") cuts both ways here: if the parent's edge was preference,
it should be removed from the parent, not silently disagreed with from a
child.

**Recommendation.** File the `REPLAN:` comment on #335 removing (or
justifying) the `N353 --> T22` edge in the same change that files #377,
rather than leaving a live contradiction between a feature's graph and its
own child task's stated dependencies.

### 4. Scope-creep risk dressed as "measurement only"

§0 states *"This task is measurement only. It ships no product code and
changes no behaviour."* But §6/§8 require: designing and hand-drawing a
two-cycle CPU datapath (merged Memory, new IR Register, two new Muxes, a
sequencer, wired to `RegisterFile` and `Clock`), giving it its own
functional correctness assertions "before any performance number is taken
from it" (§7.11, in the `RiscvCpuGoldenTest` shape), and committing it as a
tracked, frozen fixture. Open Question 1 explicitly forecloses the
programmatic path (`riscv/build_cpu.py`/`jlsbuild.py` are unusable per D5)
and recommends hand-drawing it in the GUI. Designing a *correct* small CPU
by hand, verifying its architectural behavior, and freezing it as a golden
is real design-and-implementation work with real correctness risk (Threat
#2 in §11 even names the specific failure mode: "a two-cycle machine can
accidentally re-introduce a synchronous memory read"). Calling this
"measurement only, ships no product code" undersells the size and risk of
the fixture-construction sub-task, which is arguably the majority of the
actual engineering effort here, not the counting harness.

**Recommendation.** Size and review the fixture construction as its own
piece of work with its own correctness bar, not as free scaffolding around
"the real" measurement task. The cost estimate on parent #335 (1.5 wk for
TASK-0022) should be sanity-checked against "hand-design and verify a
correct two-cycle RegisterFile-based CPU from scratch."

### 5. DoD line depends on issues that provably do not exist yet, with no owner for the follow-through

The DoD includes: *"the TASK-0016 and TASK-0023..0026 edges named in
Status & Dependencies have been added by the link pass."* The issue itself
states earlier: *"Sibling tasks being filed concurrently, whose numbers do
not exist yet... A link pass adds those five edges."* No owner, no
timeline, and no fallback are given for the "link pass." If any of
TASK-0023..0026 are never filed (plausible — they're aspirational, not
committed), this DoD line is permanently unsatisfiable and nothing says
what "done" means in that case, unlike other soft criteria in the same
list which do carry an explicit `WAIVED:` escape hatch.

**Recommendation.** Name who performs the link pass and by when, or fold it
into this task's own closing steps rather than treating it as ambient.

## What's solid

- **O4/O7 (the instrumentation seam).** Verified: `Simulator.post` at
  `src/jls/sim/Simulator.java:165`, the `afterEvent` no-op hook at `:269`,
  already overridden by `BatchSimulator.afterEvent` at
  `src/jls/sim/BatchSimulator.java:140`, and `event.getCallBack()` at
  `src/jls/sim/SimEvent.java:209` really is called immediately before
  `afterEvent` fires (`Simulator.java:239-241`). Per-callback-class
  attribution via this seam is genuinely a small, correct extension of the
  existing idiom, and the "zero changes to `jls.sim`" claim is credible.
- **O5/O8 (fixture state and gitignore/gitattributes exemptions).**
  Verified byte-for-byte: `test/fixtures/` currently holds exactly the four
  entries the issue lists, `.gitignore:8-10` and `.gitattributes:1-4` match
  the quoted text exactly, and `test/jls/RiscvCpuGoldenTest.java` confirms
  the single-cycle, pin-clocked, 34-step characterization (STEPS=34,
  "clock is an input pin stepped by a generated `-t` vector").
- **O6 (Memory's asynchronous self-posts).** Verified at
  `src/jls/elem/Memory.java` — the three `sim.post(...)` self-events for
  write/read/tristate-off are real and unconditional on any clock edge, so
  P7's warning about folding merged-memory construction into α is a
  legitimate, well-grounded threat.
- **The falsification criteria (§10) and negative-result framing (H3, the
  register-file-insensitivity case)** are genuinely designed so that a
  "no" answer is treated as a valid, useful outcome rather than a failure
  to be argued around — that's good experimental hygiene and rare enough in
  this issue corpus to be worth crediting explicitly.
- **The concurrency-model note (§7.9)** correctly identifies that
  `Simulator.post`/`PriorityQueue` has a single-thread contract and that a
  parallel surefire fork would corrupt per-run attribution — this is a real
  and easily-missed hazard for anyone just bolting a `@Test` onto the
  existing suite.
