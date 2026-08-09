# Issue #717: TASK-C531-1: one fixture lab, 300 committed submissions and golden per-student score vectors — the apparatus the parity claim is measured against
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C531-1 asks for a "shared fixture" (one lab, 300 submissions, golden
per-student score vectors) that the whole CAP-21 cross-platform-adapter
parity claim (#502 → #531 → this task) is measured against. The framing
("one artifact, so 'byte-identical' has a referent") is sound in the
abstract. But the task is not actionable in the present tense: it asks to
freeze golden output derived from a CLI contract and a verdict format that
do not exist in the codebase yet, its acceptance criteria reference a
governance policy that does not exist anywhere in the repo, and its real
dependency graph lives in a same-day comment rather than in the issue body
or in linked GitHub issues.

## Findings, most severe first

### 1. The task's core deliverable cannot be produced yet — its own inputs are unbuilt (High)

AC1/AC2 require golden per-student score vectors "derived once from the
frozen CLI contract's xUnit output," regenerable byte-identically. Both of
those nouns are aspirational, not real:

- `ordering_after: [524, 300]` in the issue's own yaml header names the
  blockers. #524 (FEAT-C21-1, "the headless CLI stops being observed
  behavior and becomes a frozen, versioned promise... xUnit schema") is
  itself **open** and describes a contract that must still be written.
  #300 (CAP-06, the capstone that invents "verdict," "counterexample," and
  a machine-readable per-student report in the first place) is also
  **open**, priced by its own abstract at "12-20 mw marginal" for the
  capstone as a whole.
- `docs/capability-roadmap/lf-04-formal-and-grading.md:11-34` (read this
  pass) states the current state of the actual tree plainly: "**JLS has no
  representation of 'correct.'**... not one of them [the `-t` grammar
  productions] mentions an output... There is no expectation side, no
  comparison, no verdict... **There is no exit status meaning 'the run
  completed and the answer was wrong.'**" The same document later proposes
  `-formal-report <file>` "xUnit XML result" and exit status 3 as **future**
  additions to `JLSStart.FLAGS` (line ~392) — not present today.
- I grepped `docs/batch-interface.md` (the normative, frozen batch contract
  cited by ARCHITECTURE.md) for `xUnit` and `status 3`: zero matches. The
  document this task's own boundary note calls "the starting material, not
  the frozen artifact" (per #524's body) genuinely has neither concept yet.

So "the golden vectors are regenerable by a single documented command...
and regenerating them produces identical bytes" (AC2) cannot be checked
against real output today — there is no command that emits a verdict or an
xUnit report to regenerate from. Any golden vectors produced now are
necessarily golden vectors of something else (a hand-rolled stand-in), and
will need to be thrown away and re-derived once #524 and #300 land, which
means the "committed... with recorded provenance" work in AC1 is likely
one-time-use scaffolding rather than the durable artifact the Outcome
promises.

**Recommendation:** either (a) block this issue explicitly (not just via
`ordering_after`, which is advisory prose) so it cannot be picked up before
#524 and #300 close, or (b) rescope it now to "author the fixture lab and
the 300-submission corpus only, with score-vector golden generation split
into a follow-up task gated on #524/#300" — which is closer to what AC3's
boundary note ("re-owns no verdict content") already implies but AC1/AC2
contradict by asking for golden *score vectors* now.

### 2. AC4 references a policy that does not exist in the repository (High)

> "Corpus size on disk is stated, and **the large-fixture policy in force
> for the repo** is applied before it lands."

I grepped the full tree (`.md` files and beyond) for "large-fixture policy"
and "large fixture policy": zero hits anywhere, including
`CONTRIBUTING.md`, `ARCHITECTURE.md`, and the `docs/capability-roadmap/`
tree. No such policy is linked, named, or defined. As written this
criterion is either unsatisfiable (nothing exists to "apply") or trivially
true (vacuous — there's nothing to violate), which makes it impossible for
a reviewer to tell whether it was honored. The issue's own comment thread
notices a downstream consequence of this gap ("If [the unnamed] policy
forces the corpus smaller than 300...") without resolving it, i.e. even
the author treats this policy as hypothetical rather than "in force."

**Recommendation:** either link the actual policy issue/doc (if one is
being written elsewhere) or strike this bullet and replace it with a
concrete, checkable number (e.g. "corpus stays under N MB uncompressed,"
"corpus is generated/compressed rather than committed as N raw `.jls`
files") before this task is picked up.

### 3. Acceptance criteria are checkboxes with no named test or CI gate — gameable (Medium)

Contrast with sibling issues in the same feature: #531 names
`CrossPlatformScoreParityTest` explicitly; #697 names
`GradescopeCorpusTest` explicitly and a "declared wall-time budget...
enforced by the test." #717 names no test class and no CI job anywhere.
"Regenerating them produces identical bytes" (AC2) and "corpus's
composition... is documented" (AC1) are both satisfiable by a one-time
manual run and a paragraph of prose, with nothing in CI re-checking either
claim on every push the way `BatchSimulationGoldenTest` or
`VcdExportGoldenTest` do for the existing goldens (ARCHITECTURE.md's Test
layout section). A PR could close this issue with a corpus that was
regenerated once, by hand, and never re-verified.

**Recommendation:** add a named determinism-check test (two consecutive
regenerations diffed in CI, mirroring the pattern #531/#697 already use)
as an explicit AC, not left to be inferred from sibling issues.

### 4. Corpus composition and "adversarial" are undefined — no floor, two possible meanings (Medium)

AC1 asks that "the corpus's composition (correct / partial / malformed /
adversarial counts) is documented," but sets no minimum diversity bar — a
corpus of 297 near-identical "correct" submissions plus one each of the
other three categories technically satisfies the letter of the AC while
defeating its stated purpose (measuring parity across real variation). Separately,
"adversarial" is never defined, and the codebase supports two very
different readings: (a) circuits crafted to game a grading rubric (e.g.
exploit the `TruthTable` don't-care-becomes-0 bug documented in
`lf-04-formal-and-grading.md:76-95`, `src/jls/elem/TruthTable.java:1446-1449`),
or (b) hostile files meant to probe the loader's hardening caps
(`UntrustedFileHardeningTest`, ARCHITECTURE.md's container-sniffing
section). Committing genuinely hostile/oversized `.jls` payloads under
category (b) into a public git history is a materially different (and
riskier) decision than committing gameable-but-valid circuits under (a);
the issue conflates them under one label.

**Recommendation:** define "adversarial" explicitly and state a minimum
count per bucket (or a stated rationale for the actual split chosen).

### 5. Real dependency graph lives in a comment, not in linked issues (Medium)

The single comment on this issue (posted the same day, by the issue's own
author via an automated pass) asserts new `blocks` edges — "read this
issue as carrying `blocks: [697, …the other three adapter corpus-run
tasks]`" — where three of those four consumers are admittedly **not yet
identified**: "of which #697 is the one confirmed by name today." GitHub's
own issue-relationship fields (`has_children`/sub-issues, cross-links) show
no such edges recorded structurally; the graph exists only as prose in a
comment. Anyone triaging this repo's issue tree by its structured fields
(as this review fleet is doing) will miss the dependency the comment
describes. This is also a minor process concern: acceptance-criteria-
relevant corrections (the note that changing the corpus size would force
edits to #697's AC-1 and AC-2) are being asserted unilaterally in a comment
rather than reflected back into the issue bodies or tracked as a follow-up.

**Recommendation:** use GitHub's sub-issue/`blocks` relationship fields (or
open the "other three adapter corpus-run tasks" as real, linked issues) so
the dependency graph is queryable rather than only readable by a human
parsing prose.

### 6. Provenance of the "300 submissions" is unaddressed (Low-Medium)

The task requires "recorded provenance" for the corpus (AC1), but the
comment and issue both use that word to mean build/regeneration provenance
(how the goldens were derived), not *authorship* provenance of the 300
submission files themselves. Nothing in the issue states whether the 300
are synthetic/generator-produced or based on real student work (even
anonymized) — a material difference for a GPLv3-or-later public repository
that already documents careful licensing/attribution practice elsewhere
(`README.md`'s "License and provenance" section, `pop_GPLv3.pdf`
provenance letter). For an educational tool this is worth stating
explicitly rather than leaving implicit.

**Recommendation:** state plainly that the corpus is synthetic/fabricated
for the fixture (if true), or describe the consent/licensing basis if any
real submissions are involved.

## What's solid

- The core motivation — one shared fixture so "byte-identical" has a single
  referent instead of pairwise comparisons across four adapters — is a
  reasonable engineering goal and matches how `BatchSimulationGoldenTest`/
  `VcdExportGoldenTest` already anchor other goldens in this repo.
- The boundary section (this task = apparatus only; parity assertion,
  containerization, and conformance/determinism split into three sibling
  tasks) is a clean, non-overlapping decomposition of #531, and is
  consistent with #531's own acceptance criteria.
- `ordering_after: [524, 300]` correctly identifies the real blockers, even
  though (per Finding 1) advisory ordering isn't enough to actually stop
  someone from starting the work early.
