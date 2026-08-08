# Issue #748: TASK-C575-3: the small-datapath labs complete the pack at eight or more, spanning combinational through datapath
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

Ship 3 more labs (ALU slice, register-and-bus, controlled datapath) to bring
the Donzellini-mapped lab pack (#575) to "at least 8" labs, each following a
layout and CI convention this issue assumes already exists from TASK-C575-1
(#744) and TASK-C575-2 (#746). Confirmed against the tree: there is currently
no lab pack at all — `examples/` contains only `examples/autograde/`
(a single unrelated shift-register grading demo), no `docs/` file defines a
lab layout, and no CI lane (`.github/workflows/{ci,codeql,mutation,release,
repro-installers,scorecard}.yml`) references labs, chapters, or
"planted-defect." Grepping the tree for "planted-defect," "course kit," and
"Donzellini" outside `issue-reviews/` returns nothing. So this issue's entire
content is prospective, and everything it "conforms to" is itself unbuilt.

## Findings, most severe first

**1. AC-1's "at least 8" has zero margin against the downstream quality gate it depends on — the count is gameable exactly the way #575 warns against.**
Arithmetic from the sibling tasks: TASK-C575-1 (#744) ships exactly 2
combinational labs (its own AC says "two combinational labs," no "at least").
TASK-C575-2 (#746) requires "at least three labs covering sequential elements
and at least one FSM lab" — a floor of 4, not a target. This issue adds
exactly 3. Minimum total after this issue: 2 + 4 + 3 = 9 — one lab of slack
over the "at least 8" bar. But TASK-C575-4 (#751), which is *ordered after
this issue*, can pull any lab that "fails two consecutive reviews," and its
own body states the risk this issue then walks past: *"without it the
incentive is to declare eight labs and let two of them be unusable
(KC-33-2)."* If TASK-1 and TASK-2 ship at their stated minimums (2 and 4),
one single pulled lab post-review drops the pack to 8, and a second pulled
lab drops it below the AC-1 floor with no mechanism in this issue's scope to
backfill. AC-1 as written is satisfied by a raw count check that can pass
today and fail the moment #751 runs. Recommend either building slack into
this issue (ship 4, not 3) or making AC-1 explicitly re-verified after #751
closes, not just at this issue's close.

**2. AC-2 requires "verified against the release artifact, not the working tree" — a hard external dependency this issue cannot itself satisfy.**
Quoted: *"Every lab in the pack loads, simulates and autogrades out of the
box on a tagged release — verified against the release artifact, not the
working tree."* Per README.md, tagged releases are cut by pushing a `v*` tag,
which runs `scripts/build-installer.sh` and publishes jar + installers
(README.md lines 196-201). This is a real, separate event on the release
cadence, not something the labs work can trigger on its own. As written, AC-2
cannot be checked off at PR-merge time — the issue is either implicitly
blocked on a maintainer cutting a release after content lands (unbounded
wait, no owner named), or "verified" will in practice get rubber-stamped
against CI's working-tree run instead, silently weakening the AC to what it
explicitly says it is not. Recommend stating who cuts the verifying release
and by when, or explicitly deferring AC-2's final check to the next
scheduled release with a named tracking mechanism.

**3. Only 2 of the 4 acceptance-criteria bullets carry an AC-n label; the numbering is incomplete and inconsistent with the parent issue.**
The body lists four checkbox items but only tags the first ("AC-1") and third
("AC-2") explicitly — the second (layout conformance + chapter/time-budget
declaration + CI lane) and fourth (chapter-coverage table) are unlabeled.
Compare to the parent #575, which cleanly numbers AC-1 through AC-5. Sloppy
numbering here is not cosmetic: several sibling issues in this cluster
(#751, #752) cross-reference specific AC numbers from #575 by ID for
traceability (e.g., #751 cites "KC-33-2" and #575's AC-4 by name). An
automated or human tracker checking "did #748 close its ACs" has no stable
identifier for two of its four criteria. Recommend numbering all four
bullets explicitly before work starts.

**4. The chapter-coverage table's format, location, and chapter-numbering source are unspecified.**
Quoted: *"The pack's chapter coverage is stated as a table, including which
chapters are deliberately not covered and why."* No file path is given for
where this table lives (kit root? README? a `docs/` file — #578 AC-1 says
the kit layout is "specified in tree," but that specification is itself
still open), and no edition/citation of the Donzellini text is pinned down
anywhere in this issue or #575/#578, so two different lab authors could map
"chapter 7" to different chapters of different editions with no way to
detect the drift. This is a real risk for a pack whose whole pitch (per #575)
is chapter-accurate mapping to a specific commercial textbook. Recommend
pinning the exact Donzellini edition/ISBN once, in #575 or #578, and having
this issue's table cite it.

**5. This issue omits the "original content only" restatement that TASK-C575-1 explicitly carries, inviting a provenance gap for exactly the riskiest new labs.**
#744's AC-5 states in the issue itself: *"All prose and circuits are
original; no DEEDS assets, text or figures are copied, and the provenance
statement for these labs is recorded."* #748 has no equivalent line — it
defers all provenance work to TASK-C575-5 (#752), which is ordered *after*
this issue closes. The datapath/ALU-slice labs proposed here are the most
textbook-structurally-bound of the whole pack (an ALU slice and a controlled
datapath map very directly to how such chapters are conventionally
illustrated), making them the highest-risk labs for inadvertent close
paraphrase of a commercial Springer text — yet this is the one task in the
C575 set that does not carry its own originality checkpoint. Recommend
importing #744's AC-5 language into this issue rather than leaving the
originality check entirely to a later, separately-scoped task.

**6. Hard sequencing dependency on two prerequisite issues that show no evidence of being started.**
`ordering_after: ["TASK-C575-2"]` (#746), which itself orders after
TASK-C575-1 (#744). Neither #744 nor #746 have any artifacts in the tree
(confirmed above) — no lab layout doc, no starter `.jls` fixtures under a
labs directory, no CI lane. This issue's own ACs ("each new lab conforms to
the layout," "covered by the reference-green / planted-defect-red CI lane")
presuppose conventions #744 has not yet defined. This is expected and
declared (ordering_after exists precisely for this), but it means #748 is
not actually actionable in isolation today — a reviewer or implementer
picking up #748 without #744/#746 already merged will immediately have to
invent the layout this issue assumes, silently expanding its own scope.
Worth flagging loudly in the issue itself (a one-line "blocked on #744/#746
merging first" note), not just implied by YAML frontmatter a human triager
may not read.

**7. Metadata convention inconsistency across the C575 task set (minor, but relevant if any tooling parses `ordering_after`).**
#744's `ordering_after` uses bare issue numbers (`[300, 552]`); #746 and #748
use quoted task-id strings (`["TASK-C575-1"]`, `["TASK-C575-2"]`) instead of
numeric issue IDs. If any automation in this repo's issue-tooling resolves
`ordering_after` to build a dependency graph, a mixed key space (numbers vs.
task-id strings) will silently fail to resolve for half the set. Low
severity, but cheap to fix — recommend switching to numeric issue refs
consistently, or documenting that task-id strings are resolved via a lookup
elsewhere.

## What's solid

- The Boundary section is clean and consistent with what #576 and #578
  actually say about their own scope — no contradiction found between #748's
  boundary claims and the referenced issues' bodies.
- The content/plumbing split (labs here, platform delivery in #502, grading
  engine in #300) is consistently maintained across #575, #744, #746, and
  #748 — no scope-creep into batch-engine or CI-platform work is asked for
  here.
- The three proposed lab topics (ALU slice, register-and-bus, controlled
  datapath) are a reasonable, textbook-conventional set for "small datapath"
  coverage and don't overlap the sequential/FSM content #746 already owns.

## Bottom line

The scope is coherent and well-bounded relative to its siblings, but the
acceptance criteria as written let the pack nominally hit "8" while carrying
no slack against the quality gate (#751) designed to catch exactly this
failure mode, defer a hard release-artifact verification with no named
owner, and drop the originality checkpoint its own predecessor task carried.
These are fixable by tightening the ACs (numbering, slack, explicit
originality restatement, a named release-verification path) before
implementation starts, not by rewriting the outcome.
