# Issue #708: TASK-C528-2: an example PrairieLearn question grades a JLS lab and returns per-test results in the platform's native format
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

A static PrairieLearn question kit (question config, file-upload UI, wiring to
an external-grader image) that runs the CAP-21 fixture lab and emits
per-student scores in PrairieLearn's native results format and in a "shared
parity-vector form." No code in the tree today mentions PrairieLearn — #502
(CAP-21, the parent capstone) states this itself: `grep -rli
"gradescope\|prairielearn\|nbgrader" . --exclude-dir=.git` returns 0 at
filing time, and the same is still true at HEAD (verified: no PrairieLearn
hits anywhere under `/home/user/JLS`).

## Findings, most severe first

**1. Every acceptance criterion depends on artifacts that do not exist yet, and the issue doesn't say so.**
The four checkboxes assume: (a) a frozen CLI contract with an xUnit schema —
that's FEAT-C21-1 / #524, still open, and its own acceptance criteria
propose *adding a new exit status 3* that doesn't exist in
`docs/batch-interface.md` today (which documents exactly three exit codes:
0/1/2, `docs/batch-interface.md:36-40`); (b) CAP-06's verdict/counterexample
machinery (#369, #466), also open, which #524 explicitly orders behind; (c)
the "shared parity-vector form" itself, which is #531/#719's deliverable
(also open); (d) the sibling grader image, TASK-C528-1 (#706), also open.
#708 names only one of these dependencies (`ordering_after:
["TASK-C528-1"]`) and is silent on #524, #369, #466, and #531 — the deeper
three-quarters of the dependency chain. A reviewer picking up #708 today has
no CLI contract to bind to, no xUnit schema to consume, and no defined
"parity-vector form" to target. **Recommendation:** either add the full
transitive `ordering_after` list, or state explicitly (as #528's Boundary
section at least gestures toward) that this task is not startable until
#524 and #369/#466 land, so nobody begins building against a moving target.

**2. Acceptance criteria are unverifiable as written — no named test, unlike every sibling and parent issue.**
Compare: #502 (CAP-21) names `CrossPlatformScoreParityTest`,
`CliContractConformanceTest`, `RecordedArtifactOnlyTest`,
`TemplateDocTest`; #528 (FEAT-C21-4, #708's direct parent) names concrete,
checkable outcomes tied to CAP-21 AC-4 and a CI lane; #719
(TASK-C531-2) names `CrossPlatformScoreParityTest`. #708 names *no* test
class, no CI lane, and no concrete comparison mechanism for any of its four
boxes. "Returns per-test results in PrairieLearn's native format" and "no
lab-specific code in the question itself" are both prose assertions with no
stated verification method. As written, a PR could satisfy these criteria
with a one-off manual demo (run once, screenshot the PrairieLearn UI,
declare done) without any regression test enforcing that swapping the lab
continues to work, or that the score vector continues to match the parity
form, on the next change. That is precisely the "gameable acceptance
criteria" failure mode. **Recommendation:** name the test(s) — e.g. a
`PrairieLearnKitFixtureTest` that runs the kit against at least two
different CAP-06 lab-as-data fixtures and asserts identical kit code, plus
a golden-file comparison against the parity-vector schema once #531 defines
it.

**3. AC-2's "shared parity-vector form" cannot be checked against a spec that doesn't exist.**
"The kit's per-student scores for the fixture class are emitted in the
shared parity-vector form, without adapter-specific normalization applied
afterwards" presupposes a canonical, already-specified parity-vector
format. That format is #531/#719's deliverable (open, unmerged) — #531's
own boundary note says "this fixture consumes the lab-as-data format, it
does not re-own verdict content," i.e. the parity-vector shape is not
settled even there. #708 cannot be verified — even in principle — until
that schema exists. This is a sequencing defect, not just a documentation
gap: the issue is currently written as if the target format is stable.

**4. The Boundary section's citation to #498 §7.2 misapplies a document that explicitly disclaims authority.**
"randomized generators are CAP-21 Open Question 4 (M2-gated per #498
§7.2)." Issue #498 is a *rescued, explicitly non-normative* branch
document; its own text states plainly: "**It is explicitly non-normative.**
… Nothing in it may be cited as settled policy." §7.2 itself is about
amending `docs/vcd-interop.md`'s wording on live co-simulation ("Graders
must not depend on interactive input… M2 must precede any scripting-API
specification") — a console/transcript milestone, not a statement that
randomized PrairieLearn question generators require it. Treating an
unratified, self-disclaimed rescue document as a hard gate ("M2-gated per
…") is exactly the citation failure #498 warns against. (To be fair, #708
inherited this verbatim from #502's Open Question 4 rather than inventing
it — but propagating it unexamined into a task-tier issue compounds the
problem rather than catching it.) **Recommendation:** either drop the
citation (randomized generators are already out of scope via "Static
question kit only," which is sufficient on its own) or replace it with a
real, ratified decision record once #498's M2 material is formally adopted
into `ARCHITECTURE.md`.

**5. Costing looks optimistic given the dependency depth, though this may be inherited rather than #708's own error.**
#528 (the parent feature) costs the whole PrairieLearn feature at 2 mw;
#708 is banded 0.5-1 mw for the question-kit slice specifically. That's
plausible *only* once #524's CLI contract, #369/#466's verdict machinery,
and #531's parity-vector schema are all merged — none are. As a standalone,
startable task today, the effective cost is "blocked, cost unknown."
**Recommendation:** note in the issue that the band assumes upstream
landings and is not a current estimate to start work now.

## What's solid

- The scoping discipline is good: "Static question kit only; randomized
  generators are … deferred" and "The byte-identity assertion across all
  four adapters is #531" both correctly keep this issue's blast radius
  small and hand off the genuinely hard cross-platform parity work to a
  dedicated issue (#531) rather than folding it in here.
- The batch-only design ("no interactive session," CAP-21 §1 step 3) is
  consistent with the codebase's actual architecture: JLS's headless batch
  mode (`Simulator`/`BatchSimulator`, ARCHITECTURE.md's threading-model
  section) already has no interactive coupling, and #498 §7.2's substantive
  point ("recording, not the session, is the contract") — separate from the
  citation problem in finding 4 — matches how `examples/autograde/autograde.py`
  and `docs/batch-interface.md` already work (subprocess, parse stdout/VCD,
  no live driving). A Docker image invoking `jls -b` as a subprocess fits
  the existing pattern cleanly.
- "no lab-specific code in the question itself — swapping the lab does not
  require editing the kit's logic" is a good design constraint in
  principle (it's the right requirement for a reusable kit); it just needs
  a test, per finding 2.

## Bottom line

The issue is coherently scoped and internally consistent about what it
*excludes*, but it stacks four unresolved upstream dependencies (#524,
#369/#466, #531, #706) without naming most of them, references a target
data format (the parity-vector form) that isn't specified anywhere yet, and
provides no verification mechanism for any of its four acceptance criteria
— a combination that lets this task look "ready" while being neither
startable nor checkable today. It needs the dependency list completed, a
named test for each criterion, and the #498 citation either dropped or
replaced before it should be picked up.
