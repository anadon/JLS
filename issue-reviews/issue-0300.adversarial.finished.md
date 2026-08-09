# Issue #300: CAP-06: one batch invocation turns 300 student submissions into deterministic per-student verdicts with counterexamples, replacing a three-line string diff
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

CAP-06 is a "capstone" governance issue that composes eight required feature-tier
issues (#317, #334, #337, #340, #353, #354, #357, #369) into a system-level
outcome: batch grading gains a verdict, a report, and a fourth exit status. The
issue itself is unusually long (285-line body, four self-audit comments spanning
2026-08-03 to 2026-08-08) and most of its low-level code citations check out
against the tree. The problems are structural: the evidentiary base the cost and
scope claims rest on doesn't exist in this repository, the minimality rule is
applied inconsistently to justify the two most expensive dependencies, and the
capstone's own spine feature has upstream dependencies the cost sum never counts.

## Findings, most severe first

### 1. The cited cost/sufficiency evidence corpus does not exist in this repository
The Cost table sources every feature's mw band to `docs/plan/features/FEAT-*.md`
(e.g. `docs/plan/features/FEAT-017-shared-and-parameterized-subcircuit-definitions.md:3`)
and the required-feature roster to `docs/plan/capstones/CAP-06-course-delivery-autograding.md`.
AC-7's "reference machine" is `docs/machine-calibration.md §2.1`. None of these
paths exist anywhere in the checked-out tree:
```
$ find . -iname "FEAT-*.md" -o -iname "CAP-*.md" -not -path "./.git/*"   → (empty)
$ find . -iname "*machine-calibration*" -not -path "./.git/*"           → (empty)
$ find docs/plan -maxdepth 2                                            → no such directory
```
`docs/` does contain a real, closely-related planning document
(`docs/capability-roadmap/lf-04-formal-and-grading.md`), but that is not the file
the Cost section cites, and the issue explicitly asserts the opposite about a
same-named file: "Citations to `10-capstone-plan.md` §3 and
`lf-04-formal-and-grading.md` have been removed from this section: neither file
exists at any path at `2d0ca9d` ... Every band above is sourced to a file that
does [resolve]." That claim is false against the tree available for this review.
Every one of the eight mw bands (2-4, 2-3, 3-5, 3-6, 4-7, 3-5, 25-36, 9-15), the
51-81 standalone sum, and the 12-20 marginal band are therefore numbers this
review cannot verify at all — they are asserted by an issue whose own Definition
of Done requires "every cited evidence document and permalink resolves on the
default branch at close." By the issue's own rule, it currently fails its own
closing checklist.
**Recommendation:** either commit the `docs/plan/` corpus and `docs/machine-calibration.md`
this issue depends on, or strike every citation to them and re-derive the cost
bands and AC-7's parameters from documents that actually exist (e.g. fold the
relevant content into `docs/capability-roadmap/lf-04-formal-and-grading.md`,
which already covers the same ground and does exist).

### 2. Minimality is applied selectively — FEAT-016/FEAT-017 survive on a much softer bar than FEAT-010/FEAT-011 cleared
§2 removes FEAT-010 and FEAT-011 from `requires_features` with a strict test:
"a residual only belongs in the required set if removing it breaks something in
§1" — and for both, no §1 step breaks. FEAT-017 (#357, priced at **25-36 mw**,
more than a third of the entire 51-81 mw standalone sum and larger than every
other required row combined) is kept in on this argument instead: "Remove
FEAT-017 (#357) and 300 students hold 300 divergent copies of the handout
definition." But re-reading §1's six steps: nothing in steps 1-4 or 6 actually
requires *shared, parameterized* subcircuit definitions — a batch grade run over
300 independently-saved `.jls` files (today's mechanism, `SubCircuit.copy()` at
`src/jls/elem/SubCircuit.java:332`) satisfies every observable in the outcome
statement. "Divergent copies" is an authoring/maintenance quality-of-life
argument, not a §1 falsifiability argument, which is exactly the standard §2
used to exile FEAT-010/FEAT-011. The same asymmetry applies more mildly to
FEAT-016 (#340, 3-5 mw): "the handout is a zip of copies with no version" — true,
but §1 step 5 ("same spec, same verdict") is satisfiable with an unversioned file
distributed out-of-band. **This makes the headline "cheap (12-20 mw)" framing in
the Abstract misleading**: the honest standalone sum the issue itself prints
(51-81 mw) is dominated by a feature whose necessity argument would have failed
the test the issue just used to cut two other features.
**Recommendation:** either apply rule E's minimality test uniformly (in which
case FEAT-016/FEAT-017 likely drop to "beneficial," and the required-set sum
shrinks substantially), or state explicitly why "divergent copies of a handout"
is a §1-breaking failure and FEAT-010/FEAT-011's absence isn't — right now the
issue does not reconcile the two applications of its own rule.

### 3. The capstone's own spine feature depends on issues its cost sum never counts
The 2026-08-03 and 2026-08-04 comments establish, and the 2026-08-08 comment
reconfirms, that `#369` (FEAT-053, the spine — "owns steps 1, 2, 3, 5, 6")
declares `blocked_by: [316, 321, 347]` — three features **outside** this
capstone's required set. The issue frames this only as a scheduling risk ("the
spine is not sequenced behind anything this capstone funds") and never revisits
the Cost section to account for it. But `blocked_by` on a required feature is
not a scheduling nicety; it means #369 cannot land — and therefore CAP-06 cannot
close — until #316, #321 and #347 close, and none of their cost is in the 51-81
mw sum, the 12-20 mw marginal band, or KC-06-3's "1.5x the 4-7 mw demo slice"
kill criterion. A reader using the printed cost figures to size the true
"time to close CAP-06" will underestimate it by the cost of three unbudgeted
prerequisite features.
**Recommendation:** either add #316/#321/#347's cost (at least the portion
FEAT-053 actually consumes) to a clearly-labeled "critical path, not standalone
cost" line, or get the disagreement between #369's declared `blocked_by` and
this capstone's stated critical path adjudicated on #369 (as the 2026-08-04
comment itself proposes) before this issue's numbers are treated as load-bearing.

### 4. AC-1 (cross-platform byte-identical determinism) is a real risk the issue names but does not close
§3's first bullet correctly flags composition-level determinism hazards (map
iteration order, locale, path separators, enumeration order) and cites prior
project bugs (#180, #181) as precedent. But the mitigation is only "the
determinism criterion must be written against the composed artifact ... not
after" — no owner, no artifact, no test skeleton, and no acceptance detail on
*how* AC-1 will be checked beyond "cmp results.xml." A submissions-directory
enumeration order, for instance, is filesystem-dependent on at least two of the
three target OSes; nothing in AC-1 or AC-8 pins a canonical sort order for
per-student ordering in the composed report. As written, an implementation could
pass AC-1 by testing only two same-OS/same-JDK CI runners with identical
filesystem semantics, never actually exercising the cross-platform path that
KC-06-1 is meant to gate.
**Recommendation:** AC-1 should name the canonicalization rule explicitly (e.g.
"submissions are sorted lexicographically by filename before grading; report
JSON keys are emitted in a fixed order") rather than leaving it to be invented
during implementation of #369.

### 5. AC-4 ("reviewable as a diff") is underspecified relative to AC-3's rigor
AC-3 pins an exact adversarial fixture (256-vector corpus, 1 correct / 255
wrong) that makes the criterion genuinely falsifiable. AC-4 by contrast says
only: "Observe a diff that shows the student's additions and nothing else — no
renumbering of untouched elements," with no committed fixture, no minimum edit
complexity, and no definition of "addition" once a student's edit involves
reconnecting existing wires (which any realistic circuit edit does). A trivial
implementation could pass AC-4 against a hand-picked, additions-only fixture
while still producing noisy diffs on the realistic edits (moves, rewiring,
deletions) 300 real submissions will contain.
**Recommendation:** commit an AC-4 fixture with a realistic mix of edit types
(the way AC-3 committed one), or narrow the claim to "additions" explicitly and
drop the "reviewable as a diff" framing, which implies more than what's tested.

### 6. Minor factual overstatement in the motivating example
The Abstract and §1 characterize the shipped grading story as "a Python script
diffing three literal lines of a text report for one input vector." Reading
`examples/autograde/autograde.py`, this is accurate for the *stdout* check but
omits that the same script also parses and checks three final VCD signal values
(`parse_vcd_final_values`, lines ~80-112) — a second, independent surface over
the same one input vector. The core criticism (one input vector, string/value
match rather than a verdict engine) survives fully; the "three literal lines"
description just isn't the complete story of what the script checks. Doesn't
change the conclusion, but a reviewer fact-checking the motivating example against
the file will find it slightly oversold.
**Recommendation:** amend to "diffing a handful of literal values against one
input vector across two output surfaces (stdout text and VCD)" for accuracy.

### 7. Process overhead is disproportionate to the described "cheap" scope
Three of the four comments on this issue (2026-08-03, 2026-08-03, 2026-08-04,
2026-08-08 — all from the same account, all machine-generated "REPLAN" /
"ADJUDICATED" / "verification" passes) exist to re-derive, then re-confirm, then
re-confirm again, the same eight-node dependency graph and the same six
contested mermaid arrows, without any of the underlying code landing. For a
capstone whose Abstract calls itself "cheap (12-20 mw marginal)," the
bookkeeping overhead already visible in the issue thread is a signal worth
naming: the governance process built around this issue (graph regeneration,
arrow-legality rules, REPLAN protocol) is consuming real review effort that the
Cost section does not count at all, on an issue that has not shipped a line of
the grading engine it describes.
**Recommendation:** no action item, but the maintainer should weigh whether this
level of self-auditing scales to the other 17 capstones without becoming the
dominant cost of the whole program.

## What's solid (no action needed)

- The falsification guard (§1 step 6 / AC-3 / KC-06-5 — 1 correct vector out of
  256, reported FAILING) is genuinely testable and is the strongest single
  criterion in the issue; the current `autograde.py`/`AutogradeBridgeExampleTest.java`
  pairing does pass a submission wrong on 255/256 vectors, exactly as claimed.
- The exit-status table citation (`docs/batch-interface.md:38-40`, exactly three
  values, no "wrong answer" status) is accurate, and treating that table as a
  one-shot, non-renumbered public interface (§3, KC-06-3 protocol item) is the
  right engineering call for a documented stability contract.
- "Where the spec lives is a security property" (§3) correctly identifies that a
  spec embedded in the student's own file is student-editable, and constrains
  FEAT-016/#369 jointly — a real hazard caught before it shipped.
- The additive-grammar requirement (AC-8/KC-06-4) against `docs/batch-interface.md`'s
  explicit stability-contract language is well-grounded and testable against the
  existing `-t` files in the tree.
- Code citations for the required rows (SigSim.java:74, Simulator.java:231-232,
  BatchSimulator.java:87-89, SubCircuit.java:332, FileAbstractor.java:52-53,
  Element.java:21/23, CircuitOp.java:34-37/51) all match the current tree
  precisely, including line numbers.
