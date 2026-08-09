# Issue #777: TASK-C552-1: lesson content is a data format authored apart from any presentation layer, and lesson 1 is written in it
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C552-1 is the founding task of a three-task chain under FEAT-C27-5
(#552): #777 defines the lesson data format and writes lesson 1, #779
(TASK-C552-2) writes lessons 2 and 3 "in the same format," and #780
(TASK-C552-3) builds the in-tool panel that renders them. The
presentation-independence goal is sound and traces cleanly to a real kill
criterion (KC-27-2 in CAP-27, #511). But two of #777's own acceptance
criteria have defects that this task, being first in the chain, is the one
that should have caught: "the first example circuit" has no defined
referent, and AC-3's "renderable... as an in-tool panel" test is asked to
prove a property against a component (#780) that is scheduled to be built
two tasks later. A sibling review of #779 already surfaced the downstream
symptom of the first problem; this review traces it to its source in #777.

## Findings, most severe first

### 1. [High] "The first example circuit" has no defined referent — only categories exist, no order

AC-2 says: "Lesson 1 for the first example circuit is authored in that
format." #777's own `ordering_after: [TASK-C548-2]` correctly gates it on
the task that creates the example set — but that task, #766 (TASK-C548-2),
defines the set only by category, not by order:

> "AC-1: At least ten curated circuits ship under `resources/samples/`,
> with at least one each of combinational, sequential, FSM and datapath...
> AC-3: Each circuit is categorized in data the menu reads, so the
> categories are a property of the set rather than of the menu code."

Nothing in #766 or #777 says which circuit is "first" — alphabetically,
simplest-first, combinational-before-sequential, or author's choice. I
verified `resources/samples/` does not exist in this checkout (only
`examples/autograde/`), so there is no existing convention to fall back on
either. A sibling review of #779 (TASK-C552-2) independently flagged this
same defect from the downstream side ("nothing in #766, #777, or #779
establishes a canonical 1st/2nd/3rd ordering... a hidden assumption the
dependency chain does not resolve") — but the ambiguity originates here:
#777 is the task that picks lesson 1's target and sets the precedent
#779 inherits. **Recommendation:** add an explicit ordinal field to #766's
"data the menu reads" (AC-3) and require #777 to cite it, or state the
selection rule directly in #777's AC-2 (e.g., "the first combinational
circuit in the set, chosen for having the fewest elements").

### 2. [High] AC-3's dual-renderability test is asked to prove a property against a component that doesn't exist yet

> "3. The format contains no presentation markup and no in-tool widget
> references, asserted by a test — the same lesson must be renderable as a
> docs page and as an in-tool panel."

The in-tool panel is not this task's deliverable — it is #780
(TASK-C552-3), whose stated `ordering_after` is `[TASK-C552-2, TASK-C550-1]`
(#779, #770), i.e. scheduled strictly after #779, which is itself scheduled
after #777. When #777 lands, no in-tool lesson panel exists in the codebase
for a test to render into. As literally worded, the AC-3 test either (a)
cannot be written honestly against real in-tool tooling and must be
deferred or weakened to a stub/mock renderer, or (b) degenerates to what is
actually checkable today — a markup-absence regex over the lesson file
(banning HTML tags, widget-ref tokens) — which proves the format is
*inert*, not that it is *renderable* the two stated ways. That gap is
exactly the "test the artifact's shape, not the process/goal it claims to
verify" pattern: a lesson file with zero markup but content that a real
renderer chokes on (e.g. a step referencing a circuit element name that
doesn't parse, or exceeding whatever the eventual panel's line-wrap
assumes) would pass AC-3's test while the actual "renders as both" claim
goes unverified until #780, two tasks later. **Recommendation:** narrow
AC-3 to what #777 can actually deliver now — a schema/markup-absence test
plus a minimal reference docs-page renderer (a script or test that turns
the lesson into rendered HTML/Markdown and asserts it succeeds) — and defer
the "in-tool panel" half of the claim explicitly to #780's own acceptance
criteria, where the real renderer will exist to test against.

### 3. [Medium] "No longer than a caption" is unquantified and untestable

Both the Outcome text and AC-2 use "each step no longer than a caption" as
the content constraint, but no character/word bound is given anywhere in
#777, and no such bound exists in the repo today — the closest analog,
`test/jls/TutorialContentTest.java`, checks page count, image resolution
and banned strings, never text length (verified by reading the file). This
is the same gap #779's review flagged as "inherited from #777... #779
repeats it without tightening it." Since #777 is the origin, this review
flags it at the source: AC-1 ("a lesson data format is defined") is the
right place to pin a concrete ceiling (e.g. "≤120 characters per step"),
because everything downstream (#779, and #780's presentation layer) will
either invent its own bound or silently drift without one.
**Recommendation:** state a numeric bound in AC-1 or AC-2 and add an
assertion for it alongside the AC-3 test.

### 4. [Medium] KC-27-2 and `band_mw` are cited as binding but are undefined anywhere in this repository

The Outcome leads with "KC-27-2 honoured structurally rather than
promised," and the YAML header carries `band_mw: 1-1.5`, but I confirmed by
grep across the full tree that neither `KC-27` nor `band_mw` appears
anywhere outside issue-tracker prose (this review file and sibling reviews
included) — not in ARCHITECTURE.md, not in docs/, not in any
capability-roadmap document. A contributor working from the checkout alone,
with no memorized GitHub issue graph, has no way to look up what KC-27-2
actually says (it is CAP-27/#511's second kill criterion: "if PF-5's lesson
tooling exceeds its band, ship lessons as docs pages and cut the in-tool
variant") or what a "band" even bounds. #777 doesn't even link #511 by
number in its body — the only traceable pointer is the informal `part_of_
feature: 552` header, one hop short of the capstone that actually defines
the term. **Recommendation:** either land a short glossary (e.g. in
ARCHITECTURE.md's "Recorded decisions" or a new `docs/roadmap-glossary.md`)
defining KC-*, band_mw, K9/D9 once, or have each task at minimum cite the
capstone issue number inline next to first use of jargon it borrows.

### 5. [Low] Licensing of lesson prose is unaddressed, despite an explicit downstream content-sharing claim

FEAT-C27-5 (#552) states "Content is shared with CAP-33's course kits"
(#517), and CAP-33 requires "Kit content carries clean licensing (course
materials under a stated open license distinct from code)." #777, which
authors the first piece of that shared content, says nothing about what
license lesson 1's step text ships under. Not blocking for a 1-1.5 mw
task, but if #777 lands before that decision is made anywhere in the repo,
the gap simply propagates (the #779 review flagged the same absence for
lessons 2-3, without resolution since #777 doesn't resolve it either).

### 6. [Low] Band estimate looks tight against what AC-3 actually implies

`band_mw: 1-1.5` nominally covers: designing a data format, documenting it
in-tree, authoring lesson 1's steps, and building a test that proves
dual-renderability. Finding 2 above shows that last piece realistically
needs at least a stub docs-page renderer to be non-gameable — that is
implementation work beyond "define a format and write some text," and the
estimate doesn't visibly account for it.

## What's solid

- The core goal — content defined apart from presentation, so KC-27-2's
  fallback (docs pages if in-tool tooling exceeds band) is structurally
  guaranteed rather than aspirational — is a genuine, well-motivated
  requirement traceable to a real capstone kill criterion (#511), not
  invented scope.
- `ordering_after: [TASK-C548-2]` (#766) is the correct and necessary gate:
  lesson 1 cannot target a circuit that doesn't exist yet, and the
  dependency is stated explicitly rather than left implicit.
- AC-4 ("Lesson 1 ends with the circuit running and a one-line statement of
  what the learner just built") mirrors #552's own AC-3 verbatim in
  substance — no drift between the task and its parent feature on this
  point.
- The three-task decomposition itself (format+lesson-1 / lessons-2-3 /
  in-tool rendering) is a clean, reviewable split, and #777 does not
  overreach into either sibling's territory.
