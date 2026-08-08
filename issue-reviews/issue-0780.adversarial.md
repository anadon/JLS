# Issue #780: TASK-C552-3: lessons are entered from the welcome pane and the Examples menu and render on-screen, and lesson 1 is completed from prompts alone
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#780 is the presentation-layer task at the bottom of a five-issue dependency
chain (#548 Examples menu, #550/#770/#771 welcome pane, #777/#779 lesson
content) — none of which have landed. Nothing in the codebase today
implements a "welcome pane," an "Examples menu," or a lesson format: `grep`
for "welcome" over `src/` and `test/` returns nothing, and "Examples" only
appears in two unrelated test class names. The idea itself — render
already-authored lesson steps with prev/next navigation — is small and sound,
and it correctly reuses the existing display-suite pattern
(`test/jls/ui/TutorialNavigationTest.java`, `@Tag("display")`, xvfb). But two
of its acceptance criteria assume entry points that the issues actually
defining those surfaces do not provide, one criterion (the "fresh-user
protocol... recorded") is unverifiable as written, and the issue leans on
undefined shorthand ("K9/D9", "band_mw") that resolves nowhere in this repo.

## Findings, most severe first

### 1. [High] AC-2's "reachable from the welcome pane" contradicts the welcome pane's own, already-filed acceptance criteria

#780 AC-2: "Lessons are reachable from the welcome pane and the Examples
menu." But the welcome pane is specified by #770 (TASK-C550-1), whose AC-2
fixes its action set at exactly three, by object identity:

> "The welcome surface offers New / Open Example / Tutorial, each bound to
> the same shared `Action` instance the corresponding menu item uses,
> asserted by object identity (#381 P9's discipline)."

#550 (the parent feature) repeats the same closed set: "opens a starter
circuit or a welcome pane offering New / Open Example / Tutorial." There is
no "Lesson" action, and none of the three existing ones is a plausible lesson
launcher without breaking something #780 itself promises to leave alone:
- "Tutorial" is pinned by identity to "the corresponding menu item" — i.e.
  the existing Help→Tutorial `Action` backing the static 4-page
  `Tutorial.java` dialog (`src/jls/Tutorial.java`). Repointing it to launch a
  lesson instead breaks the #770 AC-2 identity assertion (or forces changing
  the Help menu's Tutorial item too, which conflicts with #780 AC-5's "the
  existing Help→Tutorial content is... not removed by this task").
- "Open Example" is owned by #548 and, per #548's own AC-1–AC-5, only loads a
  circuit and shows a caption/suggested-exercise string — #548 has no
  acceptance criterion for launching an interactive lesson.
- Adding a fourth welcome-pane action is the only way left to satisfy AC-2
  literally, but that is new chrome on a surface #770/#550 explicitly closed
  at three items, and neither #780 nor #770 records that as a decision.

**Recommendation:** before this task starts, either (a) amend #770/#550 to
add a lesson entry point (and update the object-identity test's scope
accordingly), or (b) rewrite #780 AC-2 to specify precisely which existing
welcome-pane action lessons piggyback on and how (e.g., "Open Example" gains
a lesson affordance conditioned on the chosen example having one). As filed,
AC-2 cannot be satisfied without silently overriding a sibling issue's frozen
acceptance criteria.

### 2. [High] AC-3's "scripted fresh-user protocol... recorded" has no defined subject, artifact, or format

> "Lesson 1 is completed by following on-screen prompts only, with no
> external reading, under the capstone's scripted fresh-user protocol
> (CAP-27 AC-5), and the protocol run is recorded."

CAP-27 (#511) AC-2 describes this protocol as one "documented, re-runnable"
measurement across Windows/macOS/Linux — a human-subject usability test, not
an automated CI check. Neither #780 nor #511 nor anything in `docs/` (I
checked `docs/`, `CONTRIBUTING.md`, `ARCHITECTURE.md`, and
`ISSUE-AMBIGUITIES-2026-07.md`) says who the "fresh user" is, what "recorded"
means (video, transcript, checklist, a comment on the issue?), where the
recording lives, or how re-running it is enforced going forward. This is a
single-maintainer project (`labels: OWNER`); the term "fresh user" implies
someone who has never used JLS, which the maintainer structurally cannot be
on a second run. As written the criterion can be satisfied by an informal,
self-administered walkthrough that nobody can audit later — the opposite of
what "not an opinion" (the issue's own framing in its Outcome section) is
trying to guarantee.

**Recommendation:** pin this to something checkable: either fold it into the
Layer-2 display-suite harness (`test/jls/ui/`, already used for
`TutorialNavigationTest`) with an automated "does lesson 1 complete with only
the documented prompt sequence, zero out-of-band UI actions" assertion, or
specify the human-protocol artifact concretely (a checklist template, a
required recruiting source, a storage location) so "recorded" is falsifiable.

### 3. [Medium] AC-1's "holding no lesson text of its own" has no verification mechanism, unlike its sibling

#780 AC-1 imposes a structural purity constraint on the presentation layer —
no lesson text lives in the renderer. Its content-side counterpart, #777
(TASK-C552-1) AC-3, states the matching constraint explicitly *with* a
verification method: "The format contains no presentation markup and no
in-tool widget references, **asserted by a test**." #780 AC-1 states the
mirror-image constraint with no test named at all. Left open, this is
gameable: a developer can hardcode generic fallback strings ("Step X of Y",
error-state text, an empty-lesson placeholder) that are technically not
"lesson text" but blur the line, and there is no assert-the-assertion
requirement (per `test/jls/ui/package-info.java`'s stated discipline for this
harness) forcing a reviewer to draw it.

**Recommendation:** name the test explicitly in the AC, mirroring #777 AC-3 —
e.g., a source-scan or reflection-based test asserting the lesson-rendering
class holds no `String` literal overlapping any step-prompt token from the
lesson-format fixture.

### 4. [Medium] Undefined shorthand throughout — "K9/D9", "band_mw" resolve nowhere in this checkout

AC-2 cites "(K9/D9)" and the machine-readable header sets `band_mw: 1-1.5`.
I grepped the full tree (`docs/`, `*.md` at every level, `ARCHITECTURE.md`,
`CONTRIBUTING.md`, `CHANGELOG.md`, `SECURITY.md`) for `K9`, `D9`, and
`band_mw`: zero hits outside the issue-tracker prose itself. `CAP-27` also
cites an evidence path, `docs/reviews/evidence/2026-08-niche-survey/jls-baseline-adversarial-check.md`,
which does not exist in this tree either (`find docs -iname "*niche*"` and
`-iname "*evidence*"` are both empty). An implementer who is not the issue's
author has no way to resolve what invariant "K9" or "D9" name, or what unit
"1-1.5" is measured in, from the repository alone.

**Recommendation:** either land a short glossary (even a stub in
`ISSUE-AMBIGUITIES-2026-07.md` or a new `docs/planning-glossary.md`) defining
the K-series/D-series decision codes and `band_mw`, or inline the definitions
into the issues that use them. This is a process debt affecting the whole
capstone/feature/task graph, not unique to #780, but #780 is where it
actually blocks a reviewer from checking AC-2.

### 5. [Low] Dependency chain is long and entirely unbuilt; ordering_after may be incomplete

`ordering_after: [TASK-C552-2, TASK-C550-1]` (#779, #770) is transitively
correct for lesson *content* (#779 → #777 → the #548 subtask chain) and for
the welcome pane, but #780 also needs the **Examples menu itself** (#548,
the parent feature, not just its subtask) to exist and to expose whatever
entry point Finding #1 recommends — #548 is not in `ordering_after` at all,
directly or transitively through the listed tasks. Given #548, #550, #770,
#771, #777, #779 are all still open, this is speculative planning; flagging
so the ordering list gets a pass once #548's shape is settled, rather than
silently discovering the gap at pickup time.

## What's solid

- Reusing the existing Layer-2 display-suite pattern
  (`@Tag("display")`, `xvfb-run`, `TutorialNavigationTest.java`'s
  prev/next-button-state approach) for lesson navigation is a good,
  low-risk technical choice — precedent already exists and works.
- AC-4 (docs-pages fallback if the in-tool band is exceeded, tied to
  KC-27-2) is a genuine, well-designed escape valve that keeps the outcome
  from being hostage to a single implementation approach.
- AC-5 (old Help→Tutorial content stays, just superseded for first-run) is
  correctly scoped and avoids needless deletion of working, tested content
  (`Tutorial.java`, `TutorialContentTest.java`, `TutorialNavigationTest.java`
  all remain untouched by this task's own text).

## Verdict rationale

Two of five acceptance criteria (AC-2's dual entry points, AC-3's protocol
recording) either contradict an already-filed sibling issue's frozen scope or
cannot be objectively checked as written. These are fixable without
rethinking the feature, but they need rework before an implementer can build
against #780 without guessing.
