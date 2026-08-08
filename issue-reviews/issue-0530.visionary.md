# Issue #530: FEAT-C21-5: an nbgrader unit's hidden test cells grade from recorded batch artifacts alone — subprocess invocation, and no adapter ever drives a live session
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the CAP-21 machinery, #530 asks for four things: a notebook, an
`nbgrader_config.py`, a scripted README, and a doc-test lane. That is a
legitimate row in the delivery kit — nbgrader is the fourth platform an
instructor might already be on, and a course that wants JLS there today
writes its own harness. Endorse the row.

Two of its framings do not survive contact with the tree.

## Reframing 1: the "recording-not-session witness" is already witnessed, better, and elsewhere

AC-1's centerpiece — "an instrumented build asserts no interactive session is
ever opened" — and the comment's claim that this makes #530 "the designated
notebook witness for the recording-not-session discipline" both assume the
invariant is something an adapter can establish. It is not, for two reasons.

First, the invariant is already enforced at the correct seam, in-tree, twice:

- `test/jls/ArchitectureRulesTest.java` (`coreDependsOnNoGuiClasses`) plus
  `test/jls/HeadlessCoreRatchetTest.java` — a bytecode/import rule that the
  headless core references no `java.awt.*`, `javax.swing.*`, or `jls.edit.*`.
- `test/jls/HeadlessCoreCanaryTest.java` with `HeadlessCanaryMain` — a forked
  JVM that loads a real `.jls`, runs a `BatchSimulator`, and asserts none of
  those classes were ever *loaded*. Its own javadoc names the beneficiary:
  "a headless embedder (a server, a CI grader, a batch grader)."

That canary is strictly stronger than what #530 proposes. It proves JLS
*cannot* open a session in the grading path; a per-adapter instrumented build
proves only that one Python notebook *did not*.

Second, the property is structurally unavailable to violate. There is no live
protocol to open: #63 rejected a co-simulation transport, `docs/vcd-interop.md`
§ "Not offered: live co-simulation" records it normatively, and the only door
out of batch mode is subprocess + files. `examples/autograde/autograde.py`
already is a subprocess-invoking, recording-only grader, already doc-tested in
CI by `AutogradeBridgeExampleTest`. KC-21-2 — "if this adapter turns out to need
a live protocol to grade, it stops" — therefore cannot fire. A kill criterion
that cannot fire is decoration, not a constraint, and the distinctiveness the
#525 adjudication note grants #530 on that basis is largely nominal.

Concretely: drop AC-1's instrumented-build clause from #530 and satisfy AC-4 of
#502 by extending `HeadlessCoreCanaryTest`'s class-load assertion to the pinned
grading build once #524 exists. One test, in Java, in the repo that owns the
property — instead of four adapters each carrying the obligation and one of them
elected witness.

## Reframing 2: cut the seam at a shared reducer, not at four adapters

The load-bearing claim of CAP-21 is AC-1: byte-identical score vectors across
four platforms. As designed, that is a coincidence policed after the fact by
#531's 300-submission fixture — four independent adapters each parse xUnit, each
map verdicts to points, and a corpus test checks they agree. Any divergence is
found by a red lane, and only for inputs the corpus covers.

The alternative that makes the problem disappear: **one dependency-free Python
reducer, `xunit → score vector`, shipped in-tree; four ~30-line shims that
render its output into each platform's native envelope.**

- `run_autograder` on Gradescope calls it and formats `results.json`.
- The PrairieLearn grader calls it and writes `results.json` in that schema.
- The Classroom Action calls it and emits annotations.
- The nbgrader hidden cell calls it — that is #530's entire unique content.
- `examples/autograde/autograde.py` should be rewritten onto it, replacing its
  hard-coded `EXPECTED_STDOUT_LINES` (the artifact CAP-21's Background rightly
  calls "grading as literal bytes of a report format").

Byte-identity then holds by construction, and #531 demotes from *the proof* to a
cheap regression test. This also caps the maintenance surface: JLS's tree today
contains essentially one Python file, and a single maintainer is about to take on
a Jupyter/nbgrader dependency chain. A stdlib-only reducer plus a pinned
container keeps nbgrader's churn out of the grading path — the notebook imports
the reducer, not the reverse.

## The design constraint the issue does not state

AC-1 says the hidden cells "assign points." That verb is the parity bug. nbgrader
attributes points to *cells*; Gradescope attributes them to *tests* in
`results.json`; PrairieLearn to question parts. If each adapter owns its own
partition of points, four-way byte-identity is a fixture-maintained accident.

Points and test ids belong in the CAP-06 lab-as-data manifest — declared once,
with stable test ids and per-test point values — and every adapter must be a pure
renderer of that manifest, forbidden from assigning points of its own. For
nbgrader this imposes a real authoring rule the issue never mentions: each hidden
test cell must correspond to exactly one declared xUnit test id, or the vectors
cannot align. That constraint should flow *upward* into the lab format and #524's
contract, not be discovered per-adapter. I would rewrite AC-1 as: "each hidden
cell reports the verdict for exactly one declared test id; point values come from
the lab manifest, never from cell metadata."

## The out-of-the-box alternative worth one paragraph

If 1–2 mw is going into Jupyter anyway, the most valuable notebook JLS could ship
is not a points machine. SVG circuit export (`-i out.svg`) and VCD already exist;
a `examples/notebook/` that renders the student's circuit inline, runs vectors,
and shows the waveform in-cell is the artifact instructors would actually copy —
and the nbgrader release version of it is that same notebook with solution cells
stripped and the reducer call in the hidden cells. Same cost band, one artifact
instead of two, and it reaches courses that never adopt nbgrader's gradebook at
all. If the kit ships only the gradebook unit, it delivers the least imaginative
possible use of a notebook: recomputing, in Python, points the other three
adapters already compute.

## Alignment with the project's arc

Positive: files-only, no JLS-operated service, doc-tested README, dedicated CI
lane, documented-contract-only — all consistent with README/ARCHITECTURE
discipline and with #498 §8 exclusion 7. Ordering behind #524/#369/#466 is
correct; no xUnit emitter exists in `src/` today, so nothing here is startable.

Negative, if built as written: a fourth independent xUnit parser, a fourth points
model, and a redundant restatement of an invariant the Java test suite already
holds more strongly. That is scope that pulls against the project's own
"normative doc + golden test at the owning seam" habit.

## Recommendation

Keep #530; rewrite it as: *the notebook shim over the shared reducer, plus the
scripted README doc-test.* Move the no-session assertion to `HeadlessCoreCanaryTest`
against the pinned grading build (a #524 clause), move point allocation to the lab
manifest (a CAP-06 clause), and file the reducer as a new sibling feature that
#525/#526/#528/#530 all consume. I am explicitly disregarding AC-1's
instrumented-build criterion and the "designated witness" role: they verify a
property at the weakest available vantage point, and the stronger check already
runs on every push.
