# Issue #779: TASK-C552-2: lessons 2 and 3 are authored for the next two example circuits, in the same format and to the same step budget
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#779 (TASK-C552-2) asks for build-along lessons 2 and 3, authored in the
lesson data format that TASK-C552-1 (#777) is supposed to define, targeting
"the next two" circuits of a "curated example set" that also does not exist
yet in the repo (`examples/` currently holds only `examples/autograde`, no
curated `.jls` set; `resources/samples/` does not exist). The issue is
well-scoped on paper (three lessons and stop, no course-kit creep) but rests
on two things nobody has actually pinned down: which circuits are "second"
and "third," and what a passing test really proves about lesson fidelity.

## Findings, most severe first

### 1. (High) "The second and third example circuits" has no defined referent — the curated set has categories, not an order

#779's body says the lessons cover "the next two circuits of the curated
example set," and #777 (TASK-C552-1, its sole hard dependency) already
assumes a "first" circuit ("Lesson 1, for the first circuit of the curated
example set"). But the task that actually ships that set, TASK-C548-2
(#766), defines it only by **category**, not by order:

> "AC-1: At least ten curated circuits ship under `resources/samples/`,
> with at least one each of combinational, sequential, FSM and datapath...
> AC-3: Each circuit is categorized in data the menu reads, so the
> categories are a property of the set rather than of the menu code."

Nothing in #766, #777, or #779 establishes a canonical 1st/2nd/3rd ordering
over ten circuits spanning four categories. Two different people (or the
same author revisiting later) could reasonably pick different circuits as
"second" and "third," and nothing in the acceptance criteria would catch a
mismatch between what #777's lesson-1 circuit is and what #779 assumes
comes next. This is a hidden assumption that the dependency chain
(#764→#766→#777→#779) does not actually resolve.

**Recommendation:** pin the ordering explicitly — e.g., a manifest/index in
the example-set data (already planned as "data the menu reads" per #766
AC-3) that assigns an ordinal to at least the first three circuits, and have
#779 cite that index as the source of "second" and "third" rather than
leaving it to lesson-author discretion.

### 2. (High) AC-4's test is gameable: it checks the destination, not the path, and not against AC-3's own target

> "AC-4: A test asserts each authored lesson's final step produces a
> circuit that loads and simulates, so a lesson cannot describe a circuit
> that does not work."

This only proves the final-step circuit is *some* loadable, simulatable
circuit. It does not verify:
- that this circuit is in fact the shipped example named in AC-3 ("Each
  lesson's target circuit is one of the shipped examples, so a learner
  finishing a lesson can compare their result against the shipped file") —
  AC-3 has no test of its own, only an aspirational rationale;
- that the intermediate steps actually build toward that circuit rather
  than being decorative text ending on an unrelated but valid circuit.

A lesson could satisfy the stated verification (AC-4's test) while failing
the actual goal (a learner who follows the steps ends up somewhere
different from — or unable to compare against — the shipped example). This
is the classic "test the artifact exists, not that the process produced
it" gap.

**Recommendation:** the test should assert structural or content equality
between the lesson's final-step circuit and the specific shipped example
file it claims as its target (per AC-3), not merely load+simulate success.

### 3. (Medium) "same step budget" and "no longer than a caption" are unquantified, in the title and in AC-1 alike

The title promises lessons "to the same step budget" as #777's lesson 1,
and AC-1 repeats #777's phrasing verbatim: "each step no longer than a
caption." Neither #777 nor #779 gives a numeric bound (max steps, max
chars/words per step), and no such bound-checking test exists yet in the
repo (`test/jls/TutorialContentTest.java`, the closest analog, checks page
count, image resolution, and banned strings — nothing about text length).
As written, "same step budget" can't be mechanically verified, and a
reviewer has no way to reject a lesson that is technically "captions" but
much longer than lesson 1's. This is inherited from #777 rather than
introduced here, but #779 repeats it without tightening it, so the same gap
recurs twice.

**Recommendation:** either cite a concrete bound established by #777 (if
one exists by the time #779 starts) or add one here — a step-count ceiling
and a character-length assertion, mirrored on lesson 1 for consistency.

### 4. (Medium) Ambiguous scope for "each authored lesson" in AC-4 — does it retroactively cover lesson 1?

> "A test asserts each authored lesson's final step produces a circuit that
> loads and simulates"

"Each authored lesson" reads naturally as *all* lessons authored so far
(1, 2, and 3), but lesson 1 is #777's deliverable, and #777's own
acceptance criteria contain no equivalent test requirement (#777 AC-4 is a
content requirement — "Lesson 1 ends with the circuit running" — not a
named test). If #779 is expected to backfill that test for lesson 1 too,
that's work migrating from #777's scope into #779's; if it is not, the
wording should say "lessons 2 and 3" to avoid an implementer either
under-covering or unknowingly expanding scope.

**Recommendation:** reword AC-4 to name the lessons in scope explicitly
("lessons 2 and 3's final steps"), and if lesson-1 parity is wanted, make
that an explicit line item so it isn't discovered as ambiguity mid-PR.

### 5. (Low) Sequencing risk: #779 can't be concretely scoped until #766 merges

`ordering_after: [TASK-C552-1]` correctly captures the immediate blocker
(the lesson format must exist first), but the identity of "second" and
"third" circuits is only knowable once TASK-C548-2 (#766) actually lands
ten categorized circuits. #779 doesn't list #766 in its own
`ordering_after`, relying on the chain through #777 to make that dependency
transitive. That's normal for atomic task tracking, but it means anyone
starting #779 before #766 merges is guessing at its own subject matter —
worth calling out explicitly rather than leaving implicit.

### 6. (Low) No licensing note for lesson prose, despite explicit content-sharing with a capstone that requires one

#552 (the parent feature) states "Content is shared with CAP-33's course
kits" (#517), and CAP-33 AC-4 requires "Kit content carries clean licensing
(course materials under a stated open license distinct from code)." #779
(and #777) are silent on what license the lesson step text ships under.
Not blocking for a 1-1.5 mw task, but if #779 lands before that licensing
decision is made anywhere, the CAP-33 content reuse inherits an unresolved
question.

## What's solid

- The scope boundary is clear and self-enforcing: "Three lessons is the
  stated scope... the bucket closes there rather than growing into course
  material — textbook-mapped lab packs and instructor workflow stay with
  CAP-33 (#517)." This correctly fences off #779 from CAP-33's much larger
  band (9-14 mw) and gives a reviewer an easy scope-creep tripwire.
- AC-2 ("each lesson ends with the circuit running and a one-line
  statement") mirrors #777's already-settled lesson-1 shape, so lessons 2
  and 3 won't diverge in structure from lesson 1 — good consistency
  discipline across the three-task sequence.
- The `ordering_after: [TASK-C552-1]` dependency itself is correct and
  necessary: lessons 2 and 3 genuinely cannot be authored "in TASK-C552-1's
  format" before that format is defined.
