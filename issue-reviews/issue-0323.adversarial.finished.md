# Issue #323: FEAT-025: a course's existing Logisim-Evolution material opens in JLS with its structure intact and every loss named, located and explained
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The engineering core of this issue is unusually well grounded: the two
concrete code citations (`src/jls/hdl/HdlExporter.java:79`, the
`ShiftRegister` name-collision case, and `src/jls/hdl/imp/NetlistImporter.java:410`,
the private `Builder`) both verify exactly against the current tree, and
the no-partial-circuit / fail-loud discipline it proposes to inherit is
real and documented in shipped code. But the issue sits on top of a large
planning apparatus — cost bands, a "LINK PASS" DAG derived from "57 feature
documents," an evidence-pinned commit — that either does not exist in this
repository or has already gone stale in ways the issue's own comments
acknowledge but the body never fixes. An executor who reads only the
rendered issue (not all three comments) will pick up a document that
mis-describes its own child tasks as unfiled and cites a cost band its own
capstone (#311) calls "the largest disagreement between two evidence
documents in this capstone's inputs" — without mentioning the second
number at all.

## Findings, most severe first

**1. [High] The cost estimate is one-sided: #323 states only the lower of two internally-inconsistent bands, and the higher one belongs to this exact feature.**
Open Question 1 here prints "The corpus band is 6-12 maintainer-weeks"
against a 4-week task-row sum, and frames the gap as "the residual...
Rides along." But #311 (CAP-16, the capstone this issue serves) states
plainly: "the two source estimates for FEAT-025 differ by 2x" and tabulates
FEAT-025 at **6-12 mw (feature-document band)** vs **12-18 mw (competing
estimate)**, explicitly instructing "treat the higher band until the demo
slice's two-day measurement is run." #323's own Open Question 1 never
mentions the 12-18 mw figure or the instruction to use it as the default.
An executor who reads #323 alone and picks the printed 6-12 mw band is
already contradicting the sibling issue that names this feature as its
"spine." Recommendation: pull the 12-18 mw figure and the "use the higher
band" instruction into #323's own Open Question 1 rather than leaving it
only in #311.

**2. [High] The planning corpus this issue's DAG walk and cost bands are said to derive from does not exist in this repository, and the issue's own comment says so.**
The body claims the ordering edges are "derived from the corpus ordering
record — the § Prerequisite features table of every one of the 57 feature
documents under `docs/plan/features/`, read in both directions." `ls
docs/plan` returns "No such file or directory" — verified independently,
not just from the linked comment. Comment #5171446448 on this same issue
confirms it structurally: "citations into `docs/plan/**`, ... cannot be
re-pinned at all — those 195 files do not exist on `master`." So the
document whose provenance the issue leans on hardest (a corpus of 57 files
"read in both directions") is unauditable by any contributor working from
a normal checkout, now and permanently (the source branch "will not be
merged and will be deleted" per that same comment). The two in-tree code
citations survive re-pinning; the planning-corpus citations do not.
Recommendation: either commit the referenced planning documents to
`master`, or rewrite the DAG-walk and cost sections to cite only
in-repository, permalink-stable evidence.

**3. [Medium] The machine block is stale in a way the issue's own comment already flags but the body was never corrected to reflect.**
§2's task table still reads "TASK-0054 (planned, not filed)" and
"TASK-0055 (planned, not filed)," and the YAML block's `planned_tasks`
entries are written in the present tense ("ABSENT at 2d0ca9d..."). But
comment #5181354908 (posted the same day, by the same author) states
explicitly: "TASK-0054 | **#451**" and "TASK-0055 | **#450**" — both
already filed. The Definition of Done requires "`planned_tasks` empty
(each resolved to a filed issue or descoped)," which is exactly the state
this issue has not reached in its own body even though the resolution is
sitting in a comment. Verified independently: #451 and #450 both exist and
are open, titled TASK-0054 and TASK-0055 respectively. An executor
skimming only the rendered issue body — the normal reading path — will
conclude these tasks are unfiled and either re-file them or stall looking
for owners that already exist. Recommendation: edit §2 and the YAML block
now rather than leaving the correction stranded in a comment.

**4. [Medium] `evidence_commit` doesn't resolve in this checkout, and part of what it pins is unrecoverable by any commit.**
`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails `git rev-parse`/`git
branch --contains` here ("no such commit" / "unknown revision") — this
matches comment #5171446448's account that the commit "exists only on a
branch that will not be merged and will be deleted." That comment re-pins
the two in-tree code citations (line-number shift confirmed: `HdlExporter`
`:84` on the branch is `:79` on `master`, verified above), which is good
diligence, but it explicitly cannot re-pin the `docs/plan/**` citations
(finding 2) because those files simply are not on `master` at any commit.
The issue therefore carries permanently-broken evidence links alongside
correctable ones, and nothing in the body itself flags which is which —
that distinction currently exists only in a comment.

**5. [Medium] Acceptance criteria I1 and I2 are well-specified in form but the corpus and construct taxonomy they depend on are undefined here, making them gameable within #323 alone.**
§3's totality equality `C_src(f) \ C_out(f) = R(f)` is a clean, checkable
assertion in principle — but "construct" is never enumerated for `.circ`
in this issue; that taxonomy is left entirely to TASK-0054 (#451) to
invent. A taxonomy that lumps many distinct dropped attributes into one
coarse "unsupported attributes" bucket satisfies the equality's letter
(the reported set still equals the dropped set) while defeating its
purpose ("named, located and explained" per the title). Separately, I1
("Import each file in a corpus of public `.circ` files...") names no
minimum corpus size or provenance anywhere in #323's own text — a run over
two trivial hand-picked files would technically satisfy I1 and I6 as
written here. (The concrete floor — "≥30 files, ≥3 independent public
course repositories" — exists, but only in #311's KC-16-1, not in this
issue.) Recommendation: either import that floor into #323's own I1/I6 or
add an explicit cross-reference so a reader of #323 alone knows the bar
is not self-contained.

**6. [Medium] TASK-0055 — a hard prerequisite of the reader per §3 and §6 ("necessity, not convention") — is itself blocked on an unresolved, externally-dependent licensing question with no owner or timeline.**
Open Question 2 states plainly: "The licence question must be settled
before absorbing any source... **Blocks filing children** — TASK-0055
cannot start without the answer," and recommends "(b) ask upstream" while
also recommending against clean-room re-derivation. There is no assignee,
no message sent, and no fallback timeline if upstream never answers (or
answers "GPL-3.0-only," which #311's KC-16-5 says forecloses the absorption
route entirely and forces a more expensive, more defect-prone
re-derivation). Since §6 states TASK-0055 must land *before* TASK-0054, an
unanswered email from an external, non-obligated party is a hard blocker
on the entire feature's critical path, and neither of the two cost bands
in finding 1 prices that risk.

**7. [Low] Solid: the reused-discipline framing is accurate and well-cited.**
The no-partial-circuit inheritance from `NetlistImporter.java:46-47`, the
`Builder` promotion requirement (`NetlistImporter.java:410`), and the
name-collision precedent (`HdlExporter.java:79`, re-pinned correctly by
comment) all check out against the current tree exactly as quoted. This is
the part of the issue that is genuinely reviewable as written, without
needing the missing planning corpus.

**8. [Low] Ownership is literally absent for a feature costed at up to 18 weeks and named as a capstone's "spine."**
Open Question 5: "UNOWNED... not grounds for deferral" per the cited "D10"
— a rule this checkout has no way to look up (it is presumably defined in
the same missing `docs/plan/evidence/BRIEF.md` flagged in finding 2). The
issue asserts the policy is binding without the policy document being
resolvable, which is a smaller instance of finding 2's core problem.

## What's solid

- The two concrete code citations (`HdlExporter.java:79`, `NetlistImporter.java:410`) verify exactly.
- The explicit scope exclusions (§1: a second migration source, coordinate-preserving layout, test-vector migration) are clearly stated and each points to a real, separate open issue rather than being silently absorbed.
- The boundary note against #556 (comment #5175966122) is a genuinely careful piece of scope discipline — it explains why the two issues are not duplicates with specific, checkable AC-level differences rather than a hand-wave.
- The `blocked_by: [314, 349]` edges are real: #314 is confirmed open and independently names #323 in its own `blocks` field (mirrored correctly).

## Recommendation

Before this issue is picked up for execution: (a) reconcile the cost band
with #311's competing 12-18 mw estimate rather than printing only the
lower figure; (b) fix the stale "not filed" language for TASK-0054/0055
now that #451/#450 exist; (c) either commit the `docs/plan/**` evidence
corpus this issue's DAG walk cites, or rewrite the provenance claims to
rely only on in-repository, re-pinnable evidence; (d) name an owner or an
explicit trigger for asking upstream about the GPLv3 "or later" question,
since it blocks the critical path.
