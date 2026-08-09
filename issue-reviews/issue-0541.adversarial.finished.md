# Issue #541: FEAT-C24-6: one command exports the whole handout figure set — schematic, TikZ, timing figure, animation — from one circuit and one recorded run
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## What this issue actually is

#541 is the composition-only "bundle" feature in the CAP-24 (#505) print-figure
cluster: one command over one circuit + one recorded run emits five artifact
kinds (schematic SVG/PDF, CircuiTikZ, WaveJSON+timing SVG, animation) so they
cannot disagree with each other (CAP-24 risk 4). It has been through four
rounds of comment-thread editing on the same day (2026-08-04 dedup pass,
2026-08-08 absorption of #727, a same-day AC-1 wording correction, and a
same-day disposition splitting it into child tasks #874/#875) — but the issue
**body itself was never edited** to reflect any of the three post-filing
corrections. That gap is the spine of this review.

## Findings, most severe first

**1. (High) The issue body states acceptance criteria its own comment thread
has twice retracted, and anyone reading only the body implements the wrong,
weaker contract.** The body's AC-2 reads *"The command takes exactly one
circuit and one recorded-run artifact as input"* — an arity statement with no
refusal behavior. Comment 5226987749 calls this exact gap *"the load-bearing
half"* and supplies the fix (*"supplying a second run, or none, is refused by
name rather than defaulted"*), which only #727 and #874 actually carry.
Likewise the body's AC-3 lists the deterministic set as *"(SVG, TikZ,
WaveJSON)"*, omitting PDF; the same comment adds PDF to the set and explains
why (#711/TASK-C536-3 already owns PDF's byte-identity, so excluding it here
is *weaker than the artifact it composes*). And the body's AC-1 says
*"produces all five artifact kinds"*; comment 5227057625 retracts "five" as
**false the moment a REPLAN lands** and requires *"all artifact kinds"*
instead. None of these three corrections ever landed in the issue body.
Every downstream consumer of #541 who reads the issue itself (not all four
comments in sequence) gets the superseded, self-inconsistent version.
Recommendation: edit the body's AC section to match the corrected text, or at
minimum add a pinned note at the top: "superseded by #874/#875 — see
disposition comment."

**2. (High) The issue's central promise is not tested by its own stated AC-1,
and the issue's own thread says so without fixing the body.** The Outcome
section's whole reason to exist is *"the figures cannot disagree with each
other or with the run — the self-consistency failure... (CAP-24 risk 4)."*
But AC-1 as written only requires the bundle to *"produce all five artifact
kinds"* and the LaTeX document to build. Comment 5227333130 states this
plainly: *"Five files from five different runs satisfy it... CAP-24 risk 4 is
stated in the Outcome and asserted nowhere"* — and goes on to file #875's
criterion 2 specifically to close that hole. This is a textbook gameable
acceptance criterion, already caught by the issue's own maintainer-thread,
yet the body's AC section is untouched. A reviewer reading the raw issue
would credit AC-1 with testing the feature's core claim; it does not.

**3. (Medium) The rationale citation misattributes support.** The Outcome
cites *"recording-is-the-contract, #498 §7.2"* to justify why the bundle must
take one recorded run as input. I fetched #498 directly: §7.2
(*"`docs/vcd-interop.md` and #63 — recording, not reopening"*) is about
whether a **live interactive GUI console session** or its **replay
transcript** may serve as autograding input — a policy question about
interactive-vs-batch grading determinism in a ~150-KB virtual-hardware/Linux-
boot design study, with no mention of figures, exports, schematics, or
multi-artifact composition anywhere in that section. The phrase *"the
recording, not the session, is the contract"* is being borrowed rhetorically
from an unrelated normative discussion; it reads as citation-backed but the
cited section does not actually support the point.

**4. (Medium) The structured sub-issue relationship was never created,
contradicting the prose disposition.** `issue_read(get)` on #541 returns
`"has_children": false`, and `issue_read(get_sub_issues)` returns `[]`. Yet
comment 5227333130 explicitly disposes of the empty-roster defect by filing
*"#874 TASK-C541-1"* and *"#875 TASK-C541-2"* as *"a two-task roster"* under
#541. Anyone consuming this repository's issue hierarchy through GitHub's
actual sub-issue mechanism (the UI panel, or the `sub_issue_write`/
`get_sub_issues` API this fleet's own tooling uses) will see #541 as a leaf
issue with zero children — the opposite of what four rounds of comments
established. The roster exists only as prose.

**5. (Medium) `ordering_after`'s symbolic feature IDs are never mapped to
concrete issue numbers anywhere in #541's own body or comments.** The body's
YAML frontmatter reads `ordering_after: [FEAT-C24-1, FEAT-C24-2, FEAT-C24-3,
FEAT-C24-4]`. Comment 5176164248's dedup table lists #536/#537/#538/#539 in
that same order with artifact descriptions, but never states "FEAT-C24-1 =
#536" etc. — the correspondence is only inferable by matching row order to
digit order, a positional assumption nowhere confirmed. (Comment
5226987749 later resolves the *task-level* edges to #711/#714/#718/#722, but
that is a different, more granular list — the original feature-level edges
in #541's own frontmatter are still unresolved literals.)

**6. (Medium) Identifier collision: two different GitHub issues have both
held the ID "TASK-C541-1."** #727's own YAML frontmatter declares
`task_id: TASK-C541-1`. After #727 closed as a duplicate, comment
5227333130 assigns the identical string — *"#874 TASK-C541-1"* — to a new,
different issue. This project's own dedup process (comment 5176164248,
5226987749) works by grepping/matching titles and ids across issues; reusing
a retired id for an unrelated issue number is exactly the kind of ambiguity
that process exists to prevent, and it now has to special-case its own
output.

**7. (Low) No closure criterion links the parent feature to its two child
tasks.** Nothing in #541 states when #541 itself should close (e.g., "closes
when #874 and #875 both merge and verify"). Given #541 has been reduced to a
pure composition/administrative wrapper per its own boundary note, this is
a minor but real gap for whoever eventually closes it.

**8. (Low) `area:gui` label on a headless-by-design feature.** AC-4 requires
the command be *"available headlessly for CI/course-repo use,"* and every
cited producing artifact (#536/#711, #537/#714, #538/#718, #539/#722) is
batch-oriented. Nothing in the body asks for GUI wiring. (The sibling
review of #874 flags the identical mislabel one level down; it originates
here.)

## What holds up

- The core design principle is sound: composing five independently-exported
  artifacts risks exactly the cross-run disagreement defect described, and
  forcing one recorded-run input structurally forecloses it — a real,
  well-motivated contract, not manufactured scope.
- The composition-only boundary against #536-#539 (comment 5176164248) is
  rigorous and independently cross-checked against all four sibling issues
  rather than asserted.
- The disposition to split #541's now-implementation-grade criteria into two
  child tasks along the arity/determinism vs. LaTeX/self-consistency seam is
  well-reasoned, and both #874 and #875 (reviewed separately) confirm the
  split is real rather than cosmetic.
- The `ordering_after` resolution to concrete *last-task* issue numbers in
  the later comments (#711/#714/#718/#722) is specific and — per the sibling
  #874 review, which independently fetched each — accurate.

## Verdict rationale

`needs-rework`: the underlying design is defensible and the maintainer
thread has already done real, careful correction work — but that work never
made it back into the issue body. As filed, #541 presents an AC section that
(a) omits the arity-refusal clause its own thread calls load-bearing, (b)
excludes PDF from a determinism guarantee a sibling issue already provides
unconditionally, (c) uses "five artifact kinds" wording explicitly retracted
as false-under-REPLAN, (d) cannot detect the exact defect (CAP-24 risk 4)
the feature exists to prevent — a gap the issue's own comments diagnose but
never fix in the body, and (e) cites #498 §7.2 for support it does not
provide. Add to that a sub-issue hierarchy that exists only in prose, not in
GitHub's structured relationship, and this is not a "close the loop with an
edit" nitpick — it is the issue actively misleading anyone who reads the
body rather than replaying all four comments in order.
