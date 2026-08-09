# Issue #369: FEAT-053: "this design was exercised" becomes "this design passed or failed, on this vector, on this signal" — the batch engine gains a verdict, a front end and a report
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Underneath the machine block and the four rosters, the claim is one sentence: **JLS should
be trusted as an oracle, not merely as a stage.** Today the batch engine tells you what
happened; the issue wants it to tell you whether that was right. That is the correct next
move for this project and it is already the direction the tree leans — `docs/batch-interface.md`
is a declared stability contract, the VCD emitter is byte-deterministic on purpose, the
container image exists specifically "for autograders and CI", and `examples/autograde/autograde.py`
is a working demonstration that the *only* thing missing is the comparison. The evidence
section is right that the verdict does not exist (`src/jls/sim/` still holds only
`BatchSimulator`, `Reacts`, `SimEvent`, `Simulator`, `TraceGeometry`, `TraceSample`,
`package-info`). Nothing below disputes the goal.

What I dispute is the shape. As packaged, this issue fuses a two-week keystone with an
open-ended formal-methods program, and that fusion has already cost the project real
scheduling damage — twice, in one day, at four different endpoints.

## Reframing 1: this is two features, and the split line has already been drawn by hand

Read the comment history as data rather than as bookkeeping. On 2026-08-04, #524/#686/#757
were re-pointed from `ordering_after: [369, 466]` to `[466]`. On 2026-08-08 the same
correction had to be repeated for #576 and #755, which had written the edge differently and
were missed by the first sweep. The reason is stated plainly in both: naming #369 imports
`blocked_by: [316, 321, 347]`, so consumers of the *verdict* inherited the SimpleEditor
decomposition (12–20 mw), the Yosys JSON writer, and the parity harness. A 0.5 mw
directory-convention document (#755) acquired an editor refactor as a transitive prerequisite.
The whole of CAP-21 (#502) — the contract freeze plus four platform adapters plus the parity
fixture — sat behind an HDL netlist exporter it never reads.

The last comment's remedy is a prose rule: *"cite #466 if you consume the verdict engine…
cite #369 only if you consume properties, equivalence or coverage."* That is a two-feature
boundary being enforced by convention because it is not enforced by structure. Make it
structural:

- **FEAT-053a — the verdict.** §1 criteria 1, 2, 3, 4, 7. Children #466, #214, #757.
  `blocked_by: []`. Depends on nothing that is not already shipped.
- **FEAT-053b — claims about a design you did not write.** §1 criteria 5 and 6. Child #483.
  `blocked_by: [316, 321, 347]`, correctly.

§2's defence of the single feature — "three payloads through one channel" — is a code-reuse
argument, not a scheduling one, and it survives the split untouched: #483 already declares
`part_of_feature: 369` and consumes #466's schema. What the split buys is that all three
serving capstones stop waiting on the wrong half. CAP-06 (#300) needs the harness; CAP-16
(#311) needs only replay; only CAP-09 (#306) needs the formal payload. As written, the DoD
holds #300 and #311 hostage to a decidable-class question (Open Question 2) that has no
answer yet.

There is a second, independent reason to split. Strip out what has migrated to #466, #214,
#757, #483 and #423, and what remains here is §1's capability statement, §4's invariants and
§5's integration predictions. That is a **charter**, and charters should not carry
`blocked_by` at all — a statement of what must be true cannot be "blocked". The ordering
edges belong on the children that actually consume the prerequisites.

## Reframing 2: the expectation should be a circuit, not a new file format

This is the one I would most like a maintainer to weigh, because it makes several of the
issue's hardest problems disappear rather than solving them.

Invariant 1 forbids touching the `-t` grammar, so expectations are pushed into a separate
user-authored file behind a new flag. The issue correctly notices that this creates "a
compatibility surface from day one" and requires a schema version field. Follow that
consequence downstream: the new format is what #524 exists to freeze, and what #525/#526/#528/#530
exist to adapt. An entire capstone's worth of work is the cost of stabilising a format this
issue is about to invent.

**JLS already has a versioned, frozen, round-trip-tested, hostile-input-hardened
user-authored format for describing circuit behaviour: the `.jls` file.** And it already has
most of an assert primitive. `src/jls/elem/Stop.java:147` is a `LogicElement` whose `react`
calls `sim.stop()` when an input rises — an assertion with no label and no verdict. Add an
expected value, a message, and *record-and-continue instead of stop*, and it is `Assert`.

The grading shape then follows the real HDL testbench model, which JLS can already express:

- The instructor ships `lab01_tb.jls`, which **imports the student's circuit as a
  `SubCircuit`** (the import path exists — `JLSStart.java:2510` → `FileAbstractor.openCircuit`
  → `Circuit.load`).
- It drives the DUT through the wrapper's own top-level input pins using the **existing,
  unchanged `-t` grammar** — which is legal precisely because §2.2 restricts `-t` to
  top-level pins.
- It checks with drawn `Assert` elements wired to the signals under test.
- `jls -b -t vectors.txt lab01_tb.jls` returns exit status 3 if any assert fired, naming
  the assert and the time.

Count what evaporates:

- **No new file format**, hence no new schema, no version field, no freeze, no adapters.
  Expectations ride `FORMAT 1` (`Circuit.readFormatHeader`), the frozen tag table
  (`SaveTags.resolve`), `AllElementsRoundTripTest`, and `UntrustedFileHardeningTest`.
- **Invariant 1 is satisfied by construction**, not engineered around: the checker is not in
  the stimulus file at all.
- **Integration criterion 3 ("the panel and the CLI agree") becomes vacuous in the good
  sense.** There is exactly one representation of an expectation — an element on a canvas —
  and one runner reading it. Invariant 5 is enforced by ontology rather than by a test that
  must be remembered. The in-editor panel (#214) shrinks from "a second front end that must
  be proved consistent" to a results readout, which also removes the argument that it needs
  #316 first.
- **The pedagogy gets better.** A student who draws a testbench that instantiates the DUT and
  asserts on it has learned the thing they will do in Verilog — which is where `src/jls/hdl/`
  (VhdlEmitter, yosys, board bindings) is taking this project anyway. An expectations-file
  grammar teaches a JLS-only skill that transfers nowhere.

Open Question 4 already recommends shipping `Assert` and `Cover` as drawn elements "because
a property a student can *draw* is the pedagogically useful form" — and prices the sealed
24-permit registration tax as a liability. Answer that question "yes" and the separate
expectations file becomes redundant; the tax is not a liability but the **price of admission
to a pipeline that is already tested, already versioned, and already frozen**, and it is
far cheaper than the format-plus-freeze-plus-adapters tower.

Honest counterweights, since this is a real trade:

1. *Text diffability.* Answered by `-savetext` (README: "plain-text saves diff cleanly in
   version control"), over the grammar in `docs/file-format.md`.
2. *Programmatic generation.* #562 wants to emit expectations from `.dig` test cases; emitting
   a wrapper circuit is harder than emitting a vector file. Real, but a line-oriented documented
   format is generatable, and #562's harder half is translation fidelity either way.
3. *Assert-vs-Stop semantics.* An `Assert` that terminates hides every later failure. It must
   record and continue; only the run-level join terminates. A design note, not an objection.

## Reframing 3: the formal half is a subprocess, not JLS code

Open Question 2 asks which equivalence class JLS decides, and offers "combinational over a
bounded input space / bounded-depth sequential / nothing beyond vector replay". All three
answers assume JLS builds a prover. ARCHITECTURE.md's recorded decision on #222 already says
otherwise: external tool integrations (#61 Yosys, #63 GHDL/Icarus, #62 ELK) "already sit on
that subprocess boundary and stay there", partly to avoid GPLv3 in-process linking hazards.
`src/jls/hdl/yosys/` already holds `YosysLocator`, `YosysVersion`, `YosysNetlist`,
`CellValidator`.

So the aligned answer is: **JLS does not decide equivalence — it asks something that does,
and translates the counterexample back into a `-t` vector the student can replay.** That
dissolves OQ2 entirely: the decidable class is whatever the invoked tool and mode decide,
stated by naming them, and invariant 4's "refused by name" is the tool's own refusal
surfaced verbatim. It also makes the counterexample *pedagogically* useful, which a
yes/no equivalence verdict never is.

Coverage splits the same way and is smaller than the band implies. **Toggle coverage is a
fold over the trace `BatchSimulator` already accumulates** — `TraceSample`, with per-signal
change dedup already happening at recording time (§4.1). `(covered, total, "signal toggles")`
is roughly a day's work and belongs in the verdict half, not behind #321. **Branch coverage
over a schematic has no agreed denominator** — what is a branch in a mux tree? — and is the
part that should be refused by name under invariant 4 rather than shipped to satisfy a
roster line.

## Reframing 4: promote invariant 3 out of the invariant list

"No single score. Coverage is data with a stated metric and a denominator; nothing in this
feature emits a bare percentage" is the best idea in the issue, and it is buried at §4.3
where only an executor of this one feature will read it. It is a statement about **what kind
of tool JLS is** — a teaching instrument, not a grading backend — and it is exactly the sort
of thing ARCHITECTURE.md's "Recorded decisions" section exists to hold, beside the i18n
non-goal and the single-simulation-strategy ruling, with a revisit trigger. Left where it is,
the first downstream adapter (#525/#526/#528/#530) that wants a percentage will compute one,
and the invariant will have constrained nothing.

## Disposition on the stated acceptance criteria

I am not disregarding them; I am re-homing them.

- Criteria 1, 2, 3, 4, 7 — endorsed as the keystone. They should be a feature with no blockers.
- Criterion 5 (vacuity) — endorsed, but only meaningful once "property" means something
  concrete; under reframing 2 that is a drawn `Assert` no vector can drive, which is a
  reachability question the external prover answers.
- Criterion 6 (coverage as data) — split: toggle now and cheaply, branch refused by name.
- The DoD clause requiring a report **schema version field** is correct and is #524's stated
  precondition; it survives every reframing above and should be treated as non-negotiable
  even under reframing 2, where it applies to the report rather than to the expectations.

Housekeeping already acknowledged in the comments and not re-litigated here: the evidence
commit `2d0ca9d` is gone, and §2's table still reads "Not filed" for children that are
#466, #483, #423 and #470. Both are symptoms of the same thing this review is about — the
issue has become a charter carrying an executor's ordering edges.
