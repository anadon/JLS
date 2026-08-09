# Issue #877: TASK-C543-2: the CVD preview is selectable in-app over the live canvas — not a static snapshot — and CI's three CVD legs drive through it
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the claim

#877 is one of two tasks split out of #543 (FEAT-C26-2) after its sole
original child (#736) was absorbed as a near-verbatim duplicate. The split
puts the colour-transform math in #876 (TASK-C543-1) and the in-app,
live-canvas selection surface plus CI wiring in this issue. That seam is
real and well-argued in the #543 disposition comment, and #877's boundary
notes correctly disclaim ownership of the colour maths (#876) and the
non-colour encoding (#731/#734). The issue is well-grounded in its own
lineage. The concerns below are about verifiability and integration cost,
not about whether the task should exist.

## Findings, most severe first

**1. AC-3's dependency on #734 is real but undeclared, and "drives through this preview path" is not operationally defined.**
Criterion 3 reads: *"#734's `CvdStateDistinguishabilityTest` drives its
three CVD legs through this preview path, so the instructor-facing mode
and the CI apparatus are the same code."* #734 is itself open and
unimplemented, and its own acceptance criteria (read via `issue_read`)
say the test "renders the shipped adder lab" and runs "on the display
substrate the rest of the display-tagged suite uses" — nothing in #734's
body commits it to driving the actual in-app selection control (menu
item, keyboard shortcut) versus calling a shared internal method that
happens to route through the same transform. #877's own `ordering_after`
lists only `[TASK-C543-1]` (#876) — not #734 — so there is no declared
edge forcing #734 to land, or to be re-verified, against whatever surface
#877 ships. This makes AC-3 gameable in either direction: #877 can be
merged claiming "CI drives through it" while #734 (landing later,
independently) satisfies its own criteria by calling a package-private
method that never touches the keyboard/menu surface #877 built, and
nobody notices the gap because neither issue's criteria force the two to
be checked together.
*Recommendation:* add `#734` to `ordering_after` (or add a reciprocal
note to #734), and define "drives through this preview path" concretely
— e.g. the test must invoke the same public entry point (a method or
enum setter) that the in-app control calls, named in both issues.

**2. Feasibility risk: the existing paint architecture draws directly to the on-screen `Graphics`, not to an intermediate framebuffer the issue can filter.**
`SimpleEditor.paintComponent` (`src/jls/edit/SimpleEditor.java:2448-2524`)
transforms and draws the grid, circuit, selection, and caret straight
onto the `Graphics2D` Swing hands it, and the method's own comment states
*"the paint-pass Graphics is NOT cached — Swing may dispose it after this
call"* (lines 2515-2518). Criteria 1-2 require the **live** canvas to be
filtered through a CVD transform that #876 defines as operating on a
"rendered image" (`BufferedImage`, per #876's wording). Getting from "draw
straight to `g`" to "draw to a buffer, run the transform, then blit" is a
real restructuring of the paint path — one that also touches the
`firstDraw`/`pushCopy()` undo-snapshot hook that currently runs inline in
this same method (lines 2519-2522). Neither #877 nor #876 mentions this
integration cost, and #877's inherited cost band (0.5-1 mw, per #543's
disposition comment) is sized for "one filter, one test," not for a paint
pipeline change with undo-snapshot adjacency risk.
*Recommendation:* have #877 explicitly scope (or a design note in its
body) whether the buffered-render path applies only when preview is
active, and confirm the `firstDraw` snapshot logic is unaffected.

**3. Criteria 2 and 5 pull in different directions for the "off" path, and the tension is unacknowledged.**
Criterion 2 requires a live filter (not a snapshot); criterion 5 requires
byte-identical output and zero added per-frame cost when preview is off.
Satisfying both without wasted work implies branching between the legacy
direct-to-`Graphics` path and a new buffer-then-filter path — i.e. two
code shapes in the live renderer, gated on preview state. That is not the
"second rendering path" AC-3 forbids (that clause is scoped to CI vs.
production), but it is the same shape of risk the issue is otherwise
alert to, and the issue offers no criterion pinning the two paths' pixel
output together outside of the "off" case. A subtly wrong buffered path
that AC-3's CI legs never exercise (because CI only turns preview *on*
for its three legs) could ship undetected in the *default-off* everyday
case if the "off" byte-identity check itself is not run as its own
automated assertion.
*Recommendation:* make AC-5's byte-identity claim an explicit named test
(as #876 criterion 3 already is), not just prose re-assertion, so it is
audited the same way AC-1 through AC-4 are.

**4. AC-4 leans on a gate (#756) that does not exist yet in the codebase or the dependency graph.**
Criterion 4 says the preview control must pass "the reachability gate
#756 (TASK-C549-1) establishes." A repo-wide search for
`OperabilityRatchetTest` (the test #756 would add) returns no matches —
#756 is itself open and unimplemented, and #877 does not list it in
`ordering_after`. If #877 ships first, "passes the reachability gate"
has no gate to pass yet; the criterion silently degrades to an unaudited
manual claim, which is exactly the failure mode CAP-26 (#507)'s own PF-5
rule warns against ("no criterion claimed without a named test").
*Recommendation:* either add `756` to `ordering_after`, or restate
criterion 4 as a concrete, independently-checkable requirement (e.g. "the
control has a JLabel/AccessibleContext name and is in the standard
Tab-focus chain," verifiable by an existing Layer-2 test pattern such as
`MenuMnemonicAndAccessibleNameTest`) that does not depend on #756 landing
first.

**5. Criterion 6 has no named test and no operational definition — the softest, most gameable line in the issue.**
*"Adding the preview does not increase what a first-year drawing an adder
has to look at ... progressive disclosure, not a new always-visible
control"* is the only criterion in #877 (and in the #543/#876/#734
cluster generally) with no test binding it, no concrete UI location
(toolbar toggle vs. menu item vs. status bar), and no measurable
threshold. Every sibling criterion in this cluster pins its claim to a
specific test or grep-verifiable fact (AC-2's hardcoded call site, AC-5's
byte-identity, #756's ratchet); this one does not, so any placement
decision can be waved through as "progressive disclosure" after the
fact.
*Recommendation:* pin it to something checkable, e.g. "the default
toolbar/menu screenshot used by an existing UI golden test is unchanged
except for one new menu entry under View," or fold it into whatever
screenshot-diff apparatus #76/#734 already use.

**6. Minor: the CI leg AC-3 depends on inherits the display-suite's flake-tolerance policy.**
Per `pom.xml` (`display-tests` execution,
`rerunFailingTestsCount>2`) and #734's own criterion 4 ("runs on the
display substrate the rest of the display-tagged suite uses"), the
`CvdStateDistinguishabilityTest` this issue's AC-3 depends on is
display-tagged and gets up to two automatic retries on failure. A
genuine, intermittent regression introduced by #877's live-buffer
integration (point 2 above) could pass on retry, weakening AC-3's stated
rationale ("so the CI legs prove something") without anyone noticing.
Not fatal — this is inherited project-wide policy, not something #877
introduces — but the issue doesn't flag it as a residual risk the way it
flags other inherited caveats.

## What's solid

- The framebuffer-vs-palette split from #876 is clean, independently
  testable, and matches the #543 disposition comment's reasoning exactly
  — no drift found between what the comment promised and what #877's
  body states.
- Criterion 5's byte-identical-when-off requirement is well precedented
  (mirrors `Theme.CLASSIC`'s "reproduces the pre-#76 look exactly"
  invariant already enforced by `ThemeTest`/#76 invariant 3).
- The boundary notes correctly disclaim ownership of colour maths (#876)
  and non-colour encoding (#731/#734/#542), which keeps this issue from
  re-absorbing work already assigned elsewhere — a real defense against
  the exact scope-creep failure mode that produced the #736 duplicate in
  the first place.

## Verdict

**sound-with-concerns.** The task boundary is well-reasoned and the issue
is honest about what it does not own, but it under-specifies the
integration cost of hooking a live pixel filter into a paint path that
was not built for one (finding 2), leaves two of its five acceptance
criteria (3 and 4) dependent on sibling issues (#734, #756) with no
declared ordering edge, and carries one criterion (6) with no test or
operational definition at all. None of these sink the task, but an
executor picking this up should resolve findings 1 and 4 (add the
missing `ordering_after` edges or make the criteria self-contained)
before starting implementation.
