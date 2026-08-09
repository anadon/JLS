# Issue #502: CAP-21: one in-tree kit makes the same JLS lab autograde unchanged on Gradescope, GitHub Classroom, PrairieLearn and nbgrader — with byte-identical scores on all four
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

A well-structured capstone with real, honestly-argued scope boundaries (LTI
explicitly excluded, no server operation, degrade-with-a-named-error rather
than scrape undocumented platform behavior). But it rests on a cited
authority that contradicts itself, its own machine-readable dependency
fields understate a dependency chain the prose admits is real, and its
flagship acceptance criterion (byte-identical scores across four vendor
platforms) is not defined precisely enough to be checked rather than argued
about.

## Findings, most severe first

### 1. [High] KC-21-2 cites a self-declared non-normative document as a "permanent normative constraint"

KC-21-2 reads: *"If any adapter needs an interactive session or a live
protocol to grade, stop that adapter: **#498 §7.2's recording-not-session
clause is a permanent normative constraint, not a preference.**"*

Issue #498 is "part 3 of 3" of a rescued, never-merged branch document. Its
own framing, quoted verbatim in the issue body I fetched: *"**It is
explicitly non-normative.** Its own status line says so and is preserved
verbatim in part 1. **Nothing in it may be cited as settled policy.**"*
§7.2 itself is not a ratified decision — it is a *proposed* correction
("recording, not reopening") with an explicit multi-step **Process** that
has not been executed: "A decision issue quoting both sentences; **edit**
the section rather than leave the contradiction standing; a CHANGELOG
entry; an `ARCHITECTURE.md` decision block."

Checking the actual repository state: `ARCHITECTURE.md`'s "Recorded
decisions" section (read in full) lists exactly seven ratified decisions
(i18n, help delivery, look-and-feel, plugin removal, plugin trust boundary,
extension points, simulation execution strategy) — none of them is #63 or
"recording, not session." And `docs/vcd-interop.md:18-21` at HEAD still
reads the sentence #498 itself flags as needing correction: *"A live
co-simulation transport ... was evaluated and **rejected** — see issue
#63."* The proposed replacement text from #498 §7.2 has not landed.

So #502's kill criterion treats a proposal inside a document its own author
labeled unquotable-as-policy as already-binding law. If KC-21-2 ever fires
in practice, whoever adjudicates it should not be able to settle the
argument by pointing at #498 — the actual authority (`ARCHITECTURE.md`'s
recorded-decisions block, per that file's own stated convention) has not
recorded this decision yet.

**Recommendation:** either (a) drop the #498 citation from KC-21-2 and
replace it with a forward reference to the not-yet-filed `ARCHITECTURE.md`
decision block #498 §7.2 itself calls for, or (b) file that decision issue
first (as #498 §7.2's own "Process" section demands) and only then cite it
as settled. Citing the rescued document directly as normative is exactly
the failure mode #498's own header warns against.

### 2. [High] The machine block's `blocked_by: []` hides a real, multi-hop, mostly-unbuilt dependency chain

The yaml block declares `blocked_by: []` with a comment admitting the
opposite: *"real ordering exists on CAP-06's verdict machinery (FEAT-053
#369, TASK-0111 #466): PF-1 freezes a contract whose verdict half those
issues build."*

I traced that chain from the actual issues:
- #369 (FEAT-053) is OPEN, and is itself `blocked_by: [316, 321, 347]`
  (FEAT-008, FEAT-019, FEAT-034) — none filed as landed.
- #466 (TASK-0111, the verdict/report/exit-status-3 machinery #502's PF-1
  actually needs) is OPEN and its own body says its panel half is
  `blocked_by` **TASK-0021, which is not yet filed**, which is itself
  blocked on #91 (UI harness, open, "residual") and interacts with #162
  (display-lane hardening, open) and #75 (keyboard operability, open).
- Even TASK-0111's headless half (the part #502 actually needs — expectation
  file, exit status 3, xUnit report) is unbuilt at HEAD; #466's own
  Observation O1 shows `-check` is literally `unknown option` today.

So the real critical path to PF-1 alone is roughly: #91/#214/TASK-0021 →
TASK-0111 (#466) → TASK-0112 → FEAT-053 (#369) → PF-1 (#524) — five to
seven un-landed issues, several of which are themselves blocked on other
open issues, before #502's own "Standalone band: 12–17 mw" even starts
being spent. That band explicitly disclaims this cost ("contingent on
CAP-06's verdict machinery landing first ... not costed here"), but no
issue anywhere sums the true end-to-end cost, so a reader skimming #502's
cost line will underestimate the distance to done by what is plausibly
several additional maintainer-months.

Compare this to sibling issue #466, which — even for a prerequisite whose
issue number didn't exist yet at filing time — still wrote the edge into
prose and flagged "a link pass must add it." #502 has the issue numbers in
hand (#369, #466 both exist and are cited by number in the body) and still
left `blocked_by: []`, deferring the encoding to "PF-1's filing" — but PF-1
is itself several issues away from being filed.

**Recommendation:** put `[369, 466]` (or a documented reason not to) into
`blocked_by` now rather than deferring it past two more REPLAN cycles;
at minimum, replace the empty array with a comment-only placeholder
consistent with how #466 handled the same situation, and correct the cost
section to state the true (much larger) distance-to-first-milestone.

### 3. [Medium] AC-1's "byte-identical score vectors" has no defined canonical form, so it is gameable

AC-1 (`CrossPlatformScoreParityTest`): *"Identical per-student score
vectors, byte for byte, across Gradescope results.json, the Action's
summary, PrairieLearn results and the nbgrader gradebook export."* Four
independently-owned, heterogeneous formats (Gradescope's JSON score field,
a GitHub Actions/Classroom points summary, PrairieLearn's results schema,
nbgrader's gradebook export) are being compared "byte for byte" with no
stated canonicalization: no defined number format (integer points vs.
float vs. percentage), no defined field extraction, no defined rounding or
locale rule. As written, a test author can satisfy AC-1 by defining "score
vector" so narrowly (e.g., a JLS-internal intermediate array, never the
actual bytes a grader or student sees in each platform's UI) that the test
passes while the real per-platform artifacts silently diverge — exactly
the failure mode the issue's own KC-21-1 anticipates and pre-authorizes a
retreat from ("same verdicts, platform-native presentation"). An
acceptance criterion that already has a pre-planned escape hatch before any
adapter is built is a sign the criterion's literal wording was not
expected to survive contact with the four platforms.

**Recommendation:** AC-1 should specify the canonical comparison
representation (e.g., "the ordered `(student_id, points_awarded,
points_possible)` tuple list, integers only, extracted by a named
per-adapter extractor function checked into the fixture") before PF-6 is
built, not left to be improvised by whoever implements the hermetic
fixture.

### 4. [Medium] Feasibility: no plan for how a bus-factor-1 maintainer gets and keeps real access to a proprietary grading platform

Risk 5 correctly flags "Gradescope is proprietary (Turnitin)" and commits
to targeting only the documented spec. But AC-3 (`GradescopeCorpusTest`)
and AC-5 (`TemplateDocTest`) both require the template to actually be
exercised "end-to-end" and "from zero to a graded assignment" — which for
Gradescope specifically means either a real Gradescope course/instructor
account (Gradescope has no public self-serve sandbox for autograder
development at the time of writing) or trusting the documented spec alone
with no live validation, which risk 1 already says drifts on a vendor
schedule outside JLS's control. `README.md`/`ARCHITECTURE.md` describe JLS
as a "single-maintainer pedagogy tool" (i18n decision rationale). The issue
never states who holds or maintains the Gradescope-side credentials this
capstone's own acceptance tests presuppose, nor what happens when that
person's course access lapses.

**Recommendation:** name the acquisition/renewal plan for platform-side
test accounts (or explicitly scope AC-3/AC-5 down to "documented spec
only, no live account" and drop the "end-to-end on the real platform"
language) as part of PF-2's filing.

### 5. [Low] Open Question 1 is unresolved but the machine block already answers it

Open Question 1 asks whether to compose via `requires_capstones: [300]` or
consume FEAT-053/TASK-0111 directly, explicitly marking it "undecided at
filing" and "Blocks the first REPLAN." Yet the yaml block already ships
`requires_capstones: []`, i.e., the "consume directly" answer, without a
REPLAN comment recording that choice — the issue's own §5 Re-planning
Protocol requires exactly such a comment before this move. Minor
process debt, self-correctable, but worth naming since it is the kind of
drift the issue's own governance apparatus is designed to catch.

### 6. [Low] Title overclaims relative to the body's own hedge

The title promises "byte-identical scores on all four" unconditionally.
KC-21-1 already contemplates that this claim dies and pre-authorizes
re-scoping to "same verdicts, platform-native presentation." Not a
contradiction — the issue is honest about the risk in the body — but the
title should not assert a certainty the kill criteria immediately concede
might not hold.

## What's solid

- Scope boundaries are unusually well-argued: LTI is excluded with a named
  reopen trigger and a correct citation of #498 exclusion 7 ("A server, a
  network dependency, an install step, or a plugin execution surface ahead
  of demand"), which I verified is present in #498 §8 as quoted.
- The Background section's characterization of `examples/autograde/
  autograde.py` as "grading as literal bytes of a report format" is
  accurate — I read the file; `EXPECTED_STDOUT_LINES` is exactly a
  hard-coded three-line stdout match, and the VCD parse is a bonus check
  over the same fixture, not a generalized rubric.
- The comment's claims that #369 and #466 are OPEN, and that #524–#531
  were filed as the six PF issues, both check out against the live tracker.
- KC-21-3 (drop an adapter rather than track an undocumented platform
  interface) is a sound, well-scoped kill criterion with no gaps.
