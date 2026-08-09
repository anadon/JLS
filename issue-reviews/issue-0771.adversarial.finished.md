# Issue #771: TASK-C550-2: starter circuit versus welcome pane is decided in writing, and startup time becomes a per-commit regression check
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

#771 (TASK-C550-2, part of feature #550, itself PF-3 of capstone #511
CAP-27) has two deliverables: (1) a written starter-circuit-vs-welcome-pane
decision recorded on #550 with "K9/D9 rationale," the losing option
explicitly not built; (2) a per-commit startup-time regression check with a
budget "derived from the current startup time, not a guessed number,"
demonstrated red-then-green, that also polices palette-unchanged and
"no added conceptual load."

## Findings, most severe first

**1. The decision this task asks to "record" is already foreclosed by
sequencing — the criterion can pass without ever being a real choice.**
#771's own front matter reads `ordering_after: [TASK-C550-1]`, i.e. after
#770. #770 (TASK-C550-1) already builds "The welcome surface offers New /
Open Example / Tutorial, wired to the same shared `Action` objects the menu
bar uses" — consuming #381's empty-state panel, which itself only ever
specifies a welcome/empty-state panel (never a "starter circuit") across
#73's planned-task roster, #381 H4/P2/P9, and #550 AC-2. By the time #771
starts, the welcome-pane implementation is already merged and gated by
#770's own display-tagged test. AC-1 of #771 ("the option not chosen is
explicitly not implemented") is therefore satisfiable by writing a
post-hoc justification for whatever was already shipped, not by an actual
evaluation that could have gone either way. A "decision record" produced
after the losing option was never on the table in any prior issue is
theater, not a gate. **Recommendation:** either reorder so the decision
issue precedes and blocks the panel's implementation (#770), or rewrite
AC-1 to say plainly that this is a retrospective rationale for the
welcome-pane choice already implied by #73/#381/#550, not a live decision.

**2. AC-4 folds an unautomatable judgment ("no added conceptual load") into
a "check," inviting a green build that proves nothing about the thing it
claims to hold the line on.** AC-4: "The check holds the KC-27-1 line as a
whole: default palette unchanged, startup time within budget, and no added
conceptual load for a first-year drawing an adder." Palette-unchanged and
startup-time-in-budget are mechanically testable (diff `Theme.DEFAULT`,
measure wall-clock). "No added conceptual load" is a UX/pedagogy judgment
with no defined metric anywhere in this issue, #550, #511, or
`ARCHITECTURE.md`'s K9/D9 references — it is not something a per-commit CI
check can assert. As written, a contributor can make "the check" green by
satisfying only the two automatable clauses and asserting the third in
prose, and the acceptance criterion offers no way to tell those two
situations apart. **Recommendation:** split AC-4 into an automated
sub-check (palette + timing) and a separately-verified, human-signed-off
claim (conceptual load), and say so explicitly — don't let one CI badge
stand in for both, which is exactly the failure mode #381 §9 itself warns
against ("a green bar is not a user, and the project has already mistaken
one for the other once").

**3. The codebase's own precedent says CI timing assertions are unreliable
here, and the issue doesn't engage with that.** `test/jls/SpatialIndexTest.java:184-188`
explicitly documents `reportsIndexVsScanTiming()` as "Not an
assertion-bearing benchmark (CI timing is noisy)" — timing numbers are
logged, not asserted on, precisely because CI runners are too noisy for a
hard pass/fail bound. `pom.xml:285-293` independently documents that even
GUI *presence* tests (not timing) need a 2x retry under Xvfb because
"window-manager-less realization timing is nondeterministic on loaded CI
runners." AC-2/AC-3 ask for a hard per-commit startup-time budget with a
red/green transition and no discussion of retry policy, runner-load
variance, or how the derived budget accounts for the noise the project has
already had to design around twice. Left unaddressed, this check will
either be set loose enough to catch nothing (defeating AC-3's own "a
deliberately slowed startup path turns the check red" requirement, since a
loose budget may not catch a modest regression) or tight enough to flake
on every noisy runner, and the issue gives no guidance on which failure
mode is acceptable. **Recommendation:** AC-2 should require the budget
derivation to state its noise margin and CI-runner sampling method
explicitly (e.g., median of N runs, or the SpatialIndexTest pattern of
report-don't-assert plus a separate looser gate), not just "derived from
the current startup time."

**4. "Startup time" is undefined — measured where, from what entry point,
including or excluding the GUI?** JLS has at least three very different
startup paths: batch/headless (`java -jar ... -b`, no AWT), GUI cold start
(Swing, FlatLaf, AWT toolkit selection incl. Wayland detection per
README's Wayland section), and the display-tagged panel test path itself
(under Xvfb, already known-nondeterministic per finding 3). Neither #771
nor #550 nor #511 says which one "startup time" means, and the three have
wildly different variances and CI availability (batch mode needs no
display and is what CI already exercises broadly; GUI/display timing
depends on Xvfb, JBR/Wakefield, or headless-sway per README's Wayland
section). Since the thing actually at risk of regressing here is the *new
welcome-pane construction path* (GUI-only, since batch mode "never shows
the panel" per #381 §7.9), the natural reading is GUI cold-start time, but
the issue never says so, and a reviewer could legitimately implement a
"startup time" check against the headless/batch path — which never
touches the panel at all — and technically satisfy AC-2/AC-3 while leaving
the actual regression risk (the welcome-pane construction) completely
unmeasured. **Recommendation:** name the entry point and the code path
explicitly (e.g., "wall-clock from `main()` to the first `paintComponent`
call on the welcome panel or the opened starter circuit, GUI mode only").

**5. Scope/ownership overlap with #550's own AC-3 and AC-4 is undisambiguated.**
#550 AC-3 ("A per-commit startup-time regression check exists and holds the
KC-27-1 line") and AC-4 ("decision recorded ... with K9/D9 rationale") are
close to verbatim duplicates of #771's two deliverables. #550's own comment
(2026-08-04) already had to adjudicate a three-way collision between #73,
#381, and #550 over who owns the welcome panel itself ("Only one of the
three should ever be implemented as new code... do not write an
empty-state panel under this issue's number"). #771 doesn't cite that
comment or otherwise establish that its version of AC-3/AC-4 is the
authoritative one versus #550's identically-worded criteria — a future
closer could plausibly claim either issue closes these criteria,
double-counting or leaving a gap depending on which is checked off first.
**Recommendation:** #771 should state explicitly that it *is* the
implementation of #550 AC-3/AC-4 (not a duplicate obligation), the way
#770 does for #550 AC-1/AC-2.

**6. Minor — "and its derivation is recorded" (AC-2) has no specified
location or format**, unlike #381's Method section, which pins every
artifact to a named file or test class. A "recorded" derivation could be a
single unreviewed PR comment, and nothing in #771 requires it live
somewhere durable (an ADR file, a code comment on the budget constant,
etc.) the way #550/#381/#511's decision records do. Low severity, but it
is the kind of gap that lets AC-2 be satisfied by a comment that
disappears once the PR is squash-merged. **Recommendation:** require the
derivation to land as a comment next to the budget constant/threshold in
the check's source, not just "recorded" somewhere in issue history.

## What's solid

- AC-3's red-before-green discipline (deliberately slow the startup path,
  record the failing run, then require a genuine pass) is a good ratchet
  pattern and matches the project's established practice elsewhere (e.g.
  #381's P1 ratchet, ThemeTest).
- The `ordering_after` / ownership-boundary discipline (explicitly not
  re-specifying #73/#381's implementation work) is consistent with how
  this repo's task-tier issues are supposed to relate to their parent
  features, and #771 does cite the right upstream issues (#73, #381,
  #550).
- Labeling (`area:test`, `area:ux`, `tier:task`) matches the actual content
  of the two deliverables.

## Verdict rationale

`needs-rework`: the core mechanism (a written decision + a perf ratchet) is
reasonable in shape, but finding 1 (decision recorded after the choice was
already made elsewhere) undermines the stated purpose of AC-1, and findings
2-4 leave the startup-time check's scope, noise tolerance, and "conceptual
load" clause too underspecified to implement or verify without inventing
answers the issue should have supplied. None of these are fatal to the
feature's existence, but as written the acceptance criteria can be
satisfied by paperwork and a narrowly-scoped timing check that misses the
actual regression risk.
