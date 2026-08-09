# Issue #713: TASK-C530-1: an nbgrader unit's hidden cells grade a JLS lab by subprocess through the frozen contract, with no live session anywhere
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

Under the CAP-21 vocabulary, #713 is one artifact: a notebook whose hidden cells
run the pinned JLS build and turn xUnit verdicts into points. That artifact is
worth shipping — nbgrader is a platform instructors are already on, and today a
course lands on `examples/autograde/autograde.py`, which grades by string-matching
three literal stdout lines (`EXPECTED_STDOUT_LINES`, `examples/autograde/autograde.py:53-57`).

But three of the four acceptance criteria are not about the notebook at all. AC-2
asserts a property of the JVM, AC-4 asserts a supply-chain property of the kit, and
AC-3 asserts a property of the contract. Only AC-1 is genuinely this issue's work.
Each of the other three is a project-wide invariant being re-litigated inside one
adapter, which is the shape that makes four adapters cost four times what one costs.

## Reframing 1: let JLS refuse itself — kill AC-4's per-adapter refusal logic

AC-4 asks the unit to "refuse by name against an incompatible contract version."
#524 AC-5 already asks that the contract version be "queryable from the CLI itself,
so an adapter can refuse an incompatible build with a named error." If that stands,
four adapters (#525, #526, #528, #713) each implement a compatibility *predicate* —
four answers to "does contract 1.3 satisfy a unit written against 1.1?" — and the
"frozen contract" acquires four interpretations at exactly the seam it was frozen
to protect.

The reframing that makes the criterion disappear: **put the predicate in the CLI.**
`JLSStart.FLAGS` (`src/jls/JLSStart.java:759-789`) is already the single
authoritative flag table, `jls -h` is generated from it, and the exit-status
contract is already normative (`docs/batch-interface.md` §1). Add one flag —
`-requirecontract <range>` — that exits with the documented usage/refusal status and
one `jls: error: ...` line when the build does not satisfy the range. Every adapter's
version check then becomes a token in an argv list, zero lines of policy, and
`CliContractConformanceTest` in #524 pins the predicate once. AC-4's first half
(digest pinning) belongs to the kit, not the notebook: one shared pin descriptor
consumed by all four adapters, so a release bump is one edit rather than four.

And "pinned by digest" as stated is *weaker* than what JLS already ships. README
documents cosign keyless verification and `gh attestation verify oci://ghcr.io/anadon/jls`
for the batch container. A bare sha256 in a notebook re-implements supply-chain
verification badly; the kit should pin `ghcr.io/anadon/jls@sha256:...` and run the
existing verification recipe, which is provenance, not just identity.

## Reframing 2: AC-2 measures across a process wall it cannot see through

The unit invokes JLS as a *subprocess*. An "instrumented build asserting no
interactive session is ever opened" is therefore an assertion about the JVM's
internals made from a Python parent that observes only argv, exit status, and files.
The instrumentation has to live in the JVM — and it already does, more strongly:

- `test/jls/HeadlessCoreRatchetTest.java` — the headless core references no
  `java.awt.*`, `javax.swing.*`, `jls.edit.*`.
- `test/jls/HeadlessCoreCanaryTest.java` + `HeadlessCanaryMain` — a forked JVM
  loads a real circuit, runs `BatchSimulator`, and asserts those classes were never
  *loaded*.

That canary proves JLS *cannot* open a session on the grading path. AC-2 would prove
that one notebook *did not*, on one run. I am explicitly disregarding AC-2: satisfy
CAP-21 AC-4 by extending `HeadlessCoreCanaryTest` to the pinned grading build as a
#524 clause, and delete the criterion here. The "designated notebook witness" role
the #530 pass-2 note assigns this lineage is nominal — with #63's co-simulation
rejection standing and `docs/vcd-interop.md` recording it normatively, there is no
live protocol in existence for an adapter to reach for, so KC-21-2 cannot fire.

## Reframing 3: AC-3 is ambiguous against the contract that exists today

"Scrape no incidental stdout" reads as if stdout were incidental. It is not:
`docs/batch-interface.md` opens by declaring the watched-element stdout report a
stability contract requiring a CHANGELOG entry and a major bump to change, and §1
records a known deviation where `-t` parse errors land on stdout. What AC-3 actually
means is "consume the xUnit artifact file and the exit status, nothing else" — which
is a statement about *which* frozen surface, not about scraping. Say that, because
no xUnit emitter exists anywhere in the tree today (`grep -rli xunit src test` → no
hits outside `docs/capability-roadmap/`), so the criterion as written is
unverifiable and, read literally, forbids the one documented output JLS has.

## Reframing 4: the wrong cut between #713 and #715

#713 claims "the unit is runnable through nbgrader's ordinary autograde path" while
the CI doc-test lane is deferred to #715. So at #713's close, the only evidence the
notebook runs is a human having run it once — an nbgrader unit sitting in-tree with
nothing exercising it, in a repository whose entire discipline is "normative doc plus
golden test at the owning seam." Cut instead along *does it run* versus *does it
agree*: #713 = the unit plus the CI lane that autogrades the fixture; #715 = the
parity vector join with #531. That also front-loads the real risk, which is a
Jupyter/nbgrader dependency chain entering a tree that currently holds one Python
file — a risk a doc-tested lane surfaces and a hand-run notebook hides.

## The design the issue never considers: one run, many assertions

"Each cell invokes the pinned JLS build as a subprocess" is the wrong grain. nbgrader
executes the cells of a notebook in one kernel, in order; N hidden cells means N
simulations of the same circuit, N container starts, and N chances for the cells to
disagree about what they ran. The recording-not-session discipline, taken seriously,
says: **one setup cell runs the batch build once and materializes the record; every
hidden cell asserts over the parsed record.** The recording becomes a Python object
in the kernel — which is the most literal possible witness of the discipline, far
better than an instrumented build. It also forces the cell↔test-id mapping into the
open: one hidden cell per declared xUnit test id, point values read from the CAP-06
lab manifest rather than from cell metadata. Without that rule, nbgrader attributes
points to *cells* while Gradescope attributes them to *tests*, and CAP-21's
byte-identity is a fixture-maintained coincidence rather than a property.

## The larger arc

nbgrader's release/solution split is the one thing this platform has that the other
three do not: a **student-facing** artifact. JLS already exports SVG (`-i out.svg`)
and VCD. The notebook instructors would actually copy renders the student's circuit
inline, runs the vectors, and shows the waveform in the cell below — and the nbgrader
unit is that same notebook with solution cells stripped and hidden cells appended.
Same 0.5–1 mw band, one artifact instead of two, and it reaches courses that never
adopt nbgrader's gradebook. Shipped as a points machine only, this task uses Jupyter
as a worse shell script: recomputing, in Python, the scores three other adapters
already computed, against a contract that does not exist yet.

## Recommendation

Keep the artifact. Rewrite the criteria as: (1) one setup cell records, hidden cells
assert over the record, one cell per declared test id, points from the lab manifest;
(2) the unit consumes the xUnit artifact and exit status only; (3) version refusal is
a CLI flag, digest pinning is a kit-level descriptor verified with the existing cosign
and attestation recipes; (4) a CI lane autogrades the fixture in this issue, not the
next one. Drop AC-2 to `HeadlessCoreCanaryTest` under #524. Nothing here is startable
until #369/#466 land an xUnit emitter and #524 freezes the contract — which is the
right time to fix the criteria, before four adapters copy them.
