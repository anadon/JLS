# Issue #779: TASK-C552-2: lessons 2 and 3 are authored for the next two example circuits, in the same format and to the same step budget
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the task framing and #779 is two-thirds of the content payload behind CAP-27's
sharpest acceptance criterion — AC-5, *"lesson 1 is completable by following on-screen
prompts only"* — and behind #552's whole premise that **the first three circuits teach
themselves**. The capstone's own evidence is brutal and correct: the survey (#510) scored
JLS's on-ramp 2/5, first launch is an empty `JTabbedPane`, `examples/` holds nothing
user-facing (verified — the only `.jls` files in-tree are `test/fixtures/*` and
`riscv/gui/cpu.jls`), and the in-jar tutorial is four static HTML pages behind a menu
(`src/jls/Tutorial.java`, `PAGES`/`TITLES`, 4 entries). Writing three build-along lessons
is squarely on the project's arc, it is cheap, and it gates the value of every
adoption-facing capstone. Endorsed as work.

The reframing is about **what a lesson step is made of**, and it is not a stylistic
preference: as filed, the one criterion in #779 that is not already in #777 cannot bite.

## The load-bearing criterion is inert as written

AC-4 — *"a test asserts each authored lesson's final step produces a circuit that loads
and simulates, so a lesson cannot describe a circuit that does not work"* — is the only
new guarantee in this issue. AC-1 restates #777 AC-2; AC-2 restates #777 AC-4; AC-3 is a
scoping rule. So AC-4 is where the value is. But trace what it can actually assert:

- #777 fixes the lesson format as ordered steps of **prompt text** plus a **target
  circuit reference**, with "no presentation markup and no in-tool widget references."
- #779 AC-3 requires that target to be one of the shipped examples.
- #548 AC-3 already requires *every* shipped example to load through the standard open
  path and simulate under the batch simulator in a headless test (`SampleCircuitsTest`).

The lesson's "final step" therefore *produces* nothing a test can execute; it names a
file the suite already checks. AC-4 collapses into a second assertion of #548 AC-3
wearing a new label. The failure it is written to prevent — step 6 says "wire the
carry-out of the left adder into the B input of the right adder" when the shipped circuit
does no such thing, or nine steps that assemble a circuit missing an element — passes
green forever. And this is the failure that *will* happen: examples get re-authored
(#548 explicitly plans to extend #381's set in place), prose does not follow, and the
learner ends up at a circuit that does not match the file they were told to compare
against. Lesson rot is the whole risk of the bucket, and AC-4 does not see it.

## The reframing: a lesson step is an op plus a caption

JLS already contains, in-tree and under test, the exact artifact that makes this
checkable — and it was built for a different reason. `jls.collab.op` (issue #167,
`docs/operation-layer.md`) is a **closed, validated, invertible, serializable vocabulary
of editor mutations**: 13 op kinds shipped (`AddElements`, `AddWire`, `RemoveElements`,
`RemoveWire`, `MoveElements`, `RotateElement`, `FlipElement`, `SetElementConfig`,
`AttachProbe`/`RemoveProbe`, `ToggleWatched`, …), addressed by stable element ids (#165),
applied through one entry point (`OpSink`), serialized in the save-format idiom with a
strict inverse reader (`CircuitOpReader` — "unknown kinds, unknown fields, malformed
values … are rejections, never repairs"), and layered AWT-only/Swing-never with
`ArchitectureRulesTest.collabLayersAreHeadless` enforcing it.

A lesson step *is* one editor gesture plus a caption. So:

> **Lesson = ordered list of (prompt text, op block).**

This does not weaken #777's presentation-independence — it hardens it. An op is pure
data with no widget reference by construction; ARCHITECTURE.md's #222 decision already
names "the closed data-only op vocabulary" as the project's model for what crosses a
boundary safely. And AC-4 becomes an assertion with teeth:

```
replay(lesson steps → OpSink → empty Circuit) → Circuit.save (canonical, #166)
    ==  the shipped example's canonical save bytes
```

A lesson that describes a different circuit than the one it claims to build fails
`mvn verify`. Drift is caught from either side.

The elegant form, if the ordering permits it: **make the lesson the source and the
example the artifact.** For the first three circuits only, `examples/…​.jls` is a
committed build product of the lesson replay, and the test asserts the committed file
still equals the replay — the same shape as the project's existing reproducibility and
golden ratchets. Then #779 AC-3 ("a learner can compare their result against the shipped
file") is true *by construction* rather than by an author's diligence, and AC-4 needs no
separate wording. The less disruptive variant, given that #777 already orders after
TASK-C548-2, is to keep the examples authored first and assert replay-equals-committed;
that still catches every drift and requires no reordering.

## Why this is alignment, not a test trick

1. **Authoring inverts.** `OpSink` already records the migrated gestures. Lessons become
   *recorded, not written*: draw the circuit once in the editor, capture the op stream,
   then write nine captions against nine ops. #779's 1–1.5 mw of prose becomes caption
   work, and lesson N+1 becomes nearly free.
2. **It is the thing CAP-33 will need.** #779's boundary note is right that the bucket
   closes at three and must not grow into course material — but #517 PF-1 wants ≥8
   textbook-mapped labs with starter circuits, and #517 KC-33-2 kills any lab a non-author
   cannot complete. The right thing to close is **hand-authoring cost**, not the lesson
   count. As filed, this task closes the bucket and leaves nothing behind; with ops, it
   leaves behind the machinery that makes CAP-33's content affordable and self-verifying.
3. **#780 gets much better for free.** With ops the panel can highlight the element a
   step is about, diff the learner's live circuit against the step's expected post-state,
   and say *"not yet — you still need the second XOR"*. CAP-27 AC-5's scripted fresh-user
   protocol is a coin flip on prose quality when steps are prose; it is a near-certainty
   when the tool can tell a stuck learner they are off track. It also enables the
   "show me" mode — apply the step yourself and let the learner watch the circuit
   assemble — which *is* the SimCast/DigiSim pattern #510 identified and #552 cites as
   its model. A page of captions is not that pattern; it is a manual.
4. **The op layer gains a second consumer, which it needs.** `jls.collab.op` exists today
   entirely on the strength of a collaboration program (#163) that is far off. A cheap,
   shipping second consumer validates the vocabulary's completeness *now*, at the cost of
   a few captions, rather than at collab Stage 2.

## Honest costs

- **Vocabulary coverage.** `docs/operation-layer.md`'s migration table shows placement,
  wiring-finish and dialog commits are still inline *gestures* — but the ops themselves
  (`AddElements` for the unwired add, `AddWire` at net granularity, `SetElementConfig`)
  are implemented and tested. A lesson is exactly place-then-wire-then-configure, so the
  three on-ramp circuits are plausibly expressible today. This proposal needs the **ops**,
  not the migrated gestures — it is not blocked on collab.
- **Over-binding coordinates.** Byte-equality forbids "place it roughly here." Compare on
  a structural normalization (elements, connectivity, configuration) if that bites, or
  adopt the generate-the-example variant, where the question disappears.
- **Band and ownership.** This is a *format* decision and therefore belongs in #777, not
  here. #779's own band should not move: it inherits the linkage and spends its 1–1.5 mw
  on captions.

## An alternative I considered and reject

Drop authored lessons for the on-ramp entirely and ship a **recorded-session player**:
the example `.jls` plus its op stream, replayed with per-step captions. Cheapest of all,
and it would demo beautifully. Reject it: watching is not doing, so it fails CAP-27 AC-5
outright. Worth naming because the machinery above gives it to you as a bonus mode, not
as a substitute.

## Recommendation

Proceed with #779 as a work item — the lessons should be written and the three-lesson
scope is correct. Two changes, only one of which touches this issue:

- **In #777:** bind each lesson step to a `jls.collab.op` op block alongside its prompt
  text, keeping the format presentation-free (ops are data, not widgets).
- **In #779:** replace AC-4 with *"a test replays each lesson's steps into an empty
  circuit and asserts the result equals the shipped example's canonical save."* If that
  upstream change is refused, delete AC-4 rather than ship it — as written it duplicates
  #548 AC-3 and buys a guarantee the project does not actually hold.
