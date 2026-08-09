# Issue #746: TASK-C575-2: the sequential and FSM labs ship — the chapters where a grading vector file has to drive state, not just inputs
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the content framing and #746 is the first place in the whole CAP-33 program
where JLS has to answer a question it has never answered: **what does it mean for
a grading artifact to assert something about time?** The combinational labs of
#744 do not ask it — an input space is enumerable and a final value is a verdict.
The moment a counter or an FSM is graded, the claim being made is temporal ("after
three enabled edges the count is 3", "z asserts in S2 and nowhere else"), and JLS
has no way to write that down.

The issue's Outcome says so itself, in the sentence that carries the strategic
weight: this is "the reason a JLS lab pack can exist at all." I agree with the
diagnosis and disagree with the conclusion drawn from it. If the sequential slice
is where the differentiator lives, it should not be filed as a 1–1.5 mw content
task whose Boundary says "content only" and "records any limitation it hits as a
finding rather than fixing it here." That boundary spends the most informative
work in the program on producing three lab directories.

## The gap the issue walks into

`-t` is stimulus. It is not an oracle. `docs/batch-interface.md` §2.2 gives four
productions and none of them mentions an output; `docs/capability-roadmap/lf-04-formal-and-grading.md`
states the consequence outright — "there is no exit status meaning *the run
completed and the answer was wrong*." So the phrase inherited from #744 and #575,
"a `-t` grading vector file that decides the submission," names a file type that
does not exist.

What *does* exist is worse for sequential labs than for combinational ones:

- stdout is a **terminal-state** report (§3): one outcome line, then final values
  of watched `Register`/`Memory`/`OutputPin`. There is no per-cycle stdout, so a
  multi-cycle expectation cannot be checked on the frozen contract surface at all.
- the escape hatch is the VCD, graded by an external parser — and the shipped
  exemplar, `examples/autograde/autograde.py:parse_vcd_final_values`, literally
  discards the time axis ("the last change per identifier code wins"). The
  project's canonical grading bridge is a final-value extractor.
- the repo's own sequential goldens agree: every scenario in
  `test/jls/SequentialGoldenTest.java` (`simulateWithVectors`, `:183-215`) asserts
  **one final pin value**. That is the in-tree state of the art for "grading a
  clocked design."

Written as specified, #746 ships three labs plus an FSM lab whose graders are
bespoke Python VCD walkers — instance five of lf-04's list of five workarounds,
multiplied by four, published as exemplary course content that instructors will
copy. That pulls against the arc #300/#369 exists to correct.

The dependency chain is nominally fine (#746 → #744 → #300), so this is not a
sequencing bug. It is an altitude bug: the issue lets the expectation language be
decided upstream by a capstone whose own falsification test (#300 §1 step 6) is a
**256-vector combinational corpus**, and then tells the first consumer to find out
what it cannot express and file a finding.

## Reframing 1 — the sequential labs are the specification for the specification

Invert the direction of information. Make #746's primary deliverable a **temporal
expectation vocabulary**, driven by three or four worked scenarios and pinned by
in-tree fixtures; the finished lab directories fall out nearly free once #369
implements it. Concretely, the vocabulary has to settle five things that no
document in the tree settles today:

1. **Clock declaration belongs in the spec, not hand-expanded into vectors.**
   `clock clk period 20 cycles 12` beats 24 hand-written `for` steps, and
   `riscv/verify.py:gen_clock` already exists because somebody had to build this
   by hand once.
2. **Expectations indexed by cycle, not absolute time.** `at cycle 7: q == 0x5`.
   Absolute times couple the grader to the *student's* propagation delays — note
   the `delay 5` every `Register` carries in the golden builder. Cycle indexing is
   what stops a correct design being failed for using a different gate delay, and
   that false-negative class is exactly what would destroy instructor trust in the
   pack.
3. **One normative sampling instant.** Sample at edge+ε, or at end-of-cycle, stated
   once. Otherwise every lab picks its own and JLS's timing model leaks into course
   prose.
4. **Reset as a first-class concept.** `Register` carries an `init` attribute, so a
   reference and a submission can legitimately start in different states with no
   error anywhere. The spec must require an explicit reset sequence or assert the
   start state.
5. **Don't-care cycles.** Between reset and first meaningful output a correct design
   may produce anything. A temporal language with no "don't check here" reproduces,
   on the time axis, precisely the don't-care bug lf-04 documents at
   `TruthTable.java:1446-1449` — marking better-but-different designs wrong.

That list is discoverable only by authoring sequential labs. It is worth more than
the labs.

## Reframing 2 — the FSM lab does not want vectors at all

`lf-04` §"Sequential equivalence: three tiers" already describes the right answer
for the FSM lab and this issue never considers it. Tier 1 — cut reference and
submission at the register boundary, prove next-state and output functions equal —
is *cheaper and strictly stronger* than any vector file here: the reference is a
single `StateMachine` element (no circuit to draw), the assignment already fixes
the state encoding, and the verdict is "equivalent in every state for every input"
rather than "matched the twelve cycles I thought to write."

Under that framing **AC-4 disappears as a category**. "A planted defect only
observable after several clock cycles" is an artifact of grading through a keyhole;
a wrong next-state function is visible at the register boundary in zero cycles no
matter how long it takes to reach an output pin.

The formal core is 8–11 mw and not available at this issue's band, so the practical
move is the cheap hedge: **author every sequential lab so both graders can consume
it.** Name the registers per the assignment spec and state the state encoding in the
prose. Two sentences of authoring discipline, zero cost now, and the pack becomes the
formal path's first corpus with no re-authoring later. I would add that as an AC in
place of AC-4.

## Reframing 3 — planted defects should be operations, not files

AC-2 asks for a reference-green / defect-red CI lane per lab, and the obvious
implementation is a second `.jls` next to each reference. Four labs × N defects of
near-identical XZ blobs that silently drift apart is a maintenance trap, and the
defect itself is illegible in review — a binary diff.

The operation layer is already shipped (#167, `src/jls/collab/op/`, 20 files,
`docs/operation-layer.md`) with validation, atomic rejection and **exact inverses**.
Express each planted defect as a serialized `CircuitOp` patch against the reference —
`RemoveWire`+`AddWire` to mis-tap a register's D input, `SetElementConfig` to flip an
edge polarity — and generate the defect variant at test time. The defect becomes a
reviewable one-line description, the reference stays the single source of truth, and
`lf-07`'s "simulate 10,000 mutated variants and measure how many a given test set
catches" becomes reachable from the same seam.

Which suggests the stronger acceptance criterion: replace "at least one planted
defect" with a **mutation score over a generated defect set** — "these vectors kill
k of n mutants of this reference, and here are the survivors." The project already
runs PIT on Java (`docs/mutation-testing-trial-2026-07.md`, adopted with a ratchet
at 80/82); this is the same idea one level up, on circuits, and it answers AC-4's
actual worry ("vectors detect state errors and not merely wiring errors") with a
number instead of an anecdote.

## Reframing 4 — the findings need a destination

The Boundary defers FSM element limitations to #566 and says to "record any
limitation it hits as a finding." No AC names an artifact, so the findings have
nowhere to land. Meanwhile #566's *mandatory* deliverable is a written parity
document, and its verified-close path closes the feature on that document alone.
An FSM lab authored end-to-end is the best evidence that assessment can get — a
walk of the real workflow by someone who has to make it work. Say so: findings land
as rows in #566's parity document. Zero cost, and it turns a throwaway clause into
the feedback edge the program needs.

One such finding is already visible without writing a lab. `StateMachine` with no
matching transition holds state and warns once to **stderr**
(`StateMachine.java:663-664,774`, pinned by
`SimulationSemanticsRegressionTest.stateMachineWithNoMatchingTransitionStaysAliveAndWarnsOnce`)
while the run still exits 0. `docs/batch-interface.md` §1 says exit 0 means "stderr
empty." A student FSM with an incomplete transition set therefore passes a
status-based grader and fails a stderr-based one. Every FSM lab in this pack will
hit that, and it is the same latent-latch pathology lf-04 documents for
`TruthTable`.

## On the strategic claim

"The exact capability Logisim-Evolution's `-test` never had" is true and narrower
than the issue treats it. lf-04 records their CSV `--test-vector` as "exactly the
sampling surface JLS already has in `-t`" — so the differentiator as stated is
parity-of-weakness plus a time axis, and JLS has not yet built the verdict half of
it. hneemann's Digital already ships test cases plus sequential analysis. Staking
the pack's reason to exist on a capability a competitor could close in one release,
and that JLS does not yet have, is a weak footing. The durable moat is the other
half of #575: original, chapter-mapped content for a specific textbook, with CI
proving each lab discriminates a right answer from a wrong one. I would lead the
Outcome with that. (Also: "their #598 and #950" are load-bearing and unverified
here; a strategic claim should not rest on two foreign issue numbers nobody checked.)

## What I would keep and what I would replace

Keep: the labs themselves, the chapter/time-budget declarations (AC-3), the
reference-green/defect-red lane as a concept, the DEEDS-provenance discipline.
These are right and the pack needs them.

I am explicitly setting aside AC-1's "at least three labs plus one FSM lab" as the
primary success measure, and AC-4 entirely. Substitutes:

- **New AC-0:** a written temporal-expectation vocabulary (clock declaration,
  cycle-indexed expectations, sampling instant, reset, don't-care cycles) lands as
  a section of the grading-spec document, derived from the labs and consumed by
  #369 rather than discovered after it.
- **AC-1':** two sequential labs and one FSM lab — three, not four. The count buys
  nothing the vocabulary does not.
- **AC-2':** defects are serialized `CircuitOp` patches, and each lab reports a
  mutation score over a generated defect set with survivors named.
- **AC-4':** every reference names its registers per the stated encoding, so the
  pack regrades under register-boundary equivalence without re-authoring.
- **AC-5':** findings land as rows in #566's parity document.

The issue is right about where the frontier is. It is filed one level too low to
stand on it.
