# Issue #756: TASK-C549-1: a keyboard-unreachable dialog fails the build — the reachability gate, with its seeded red run recorded first
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the claim

#756 is TASK-C549-1, the first (and, per `ordering_after: []`, apparently
independent) task under FEAT-C26-6 (#549), itself serving CAP-26 PF-6
(#507). It asks for `OperabilityRatchetTest`: a CI gate that walks "every
dialog and window surface" from "the registry" and fails if any focusable
control is keyboard-unreachable from initial focus, preceded by a
committed red run against a seeded regression. The falsification-first
shape is good discipline in principle, but the issue is underspecified in
exactly the places that determine whether the eventual test measures
anything real, and it leans on two things — "the registry" and "K9" —
that do not exist in this checkout.

## Findings, most severe first

**1. [High] AC-1's "the registry" does not exist for dialogs or windows, so AC-1 as written cannot be implemented — only a new mechanism can satisfy it, and that mechanism is unscoped.**
Quote: *"`OperabilityRatchetTest` ... enumerates dialog and window
surfaces from the registry rather than a hand-maintained list."* The only
registry in this codebase is `ElementRegistry` (#78,
`docs/grand-architecture.md:90`), which maps element *types* to
factories/tags for the palette and the loader — it says nothing about
dialogs or top-level windows, and classes like `About`, `Tutorial`,
`MemTrace` (the trace window), and `JLSStart`'s main frame
(`grep` for `extends JFrame`/`extends Frame` in `src/`) are not elements
at all and are never touched by it. The one piece of prior art that
enumerates dialogs today, `test/jls/ui/DialogCoverageRatchetTest.java`,
explicitly does *not* use a registry: it ArchUnit-scans compiled
bytecode for `ElementFormDialog` subclasses and reconciles them against a
hand-maintained `SWEPT`/`REPRESENTED` map (lines 34–57 of that file) —
i.e. exactly the "hand-maintained list" AC-1 says to avoid, and it only
covers the ~22 element-creation dialogs, not the ~5 non-element
window/dialog classes (`MemTrace`, `Tutorial`, `About`, `JLSStart`,
`ElementFormDialog` itself) or anything reached through `TellUser`.
Building a genuine enumeration mechanism for *all* dialog and window
surfaces (bytecode scan for `JDialog`/`JFrame`/`Window` subclasses, or a
new registration point every window must opt into) is real, unscoped
design work, not a byproduct of writing one test.
*Recommendation:* either point AC-1 at the concrete mechanism this task
will actually build (a bytecode sweep like `DialogCoverageRatchetTest`'s,
extended to non-element windows) and drop the word "registry," or split
out the enumeration mechanism as its own task with its own acceptance
criteria, band, and false-negative analysis.

**2. [High] AC-4 cites "K9" as a settled constraint that resolves nowhere in this repository — the same gap flagged repeatedly elsewhere in this issue's own lineage.**
Quote: *"The gate itself changes no default visual theme and no
existing-user experience (K9)."* Grepping README.md, ARCHITECTURE.md,
CONTRIBUTING.md, and `docs/` for "K9" turns up nothing that defines it;
it is presumably shorthand for a maintainer directive recorded outside
this checkout. This is not a one-off: adversarial reviews already on
file for #549 (this task's parent), #550, #552, #676, and #764 raise the
identical "K9/D9 does not resolve" objection — this is a corpus-grounding
gap the fleet has already surfaced multiple times, and #756 reproduces it
verbatim instead of inlining the resolved text. As written, a reviewer or
implementer working from this checkout alone cannot check AC-4 at all.
*Recommendation:* inline the actual K9 text into the issue body (as at
least one sibling issue, #381, is noted to do) so AC-4 is checkable
without external context.

**3. [Medium] "Keyboard-reachable from initial focus" is not operationally defined, and the acceptance criteria are satisfiable by a test that checks a weaker property than the outcome promises.**
The Outcome paragraph's real goal is that a *student* can reach every
control with the keyboard. AC-1 only commits the test to asserting "every
focusable control is keyboard-reachable" with no stated method. Tab-order
reachability (does repeated `VK_TAB` eventually focus it?) is a much
weaker property than *usable* reachability — it says nothing about
whether the tab order is sane, whether a control is reachable but visibly
indistinguishable when focused (no focus ring), or whether reaching it
requires an absurd number of tab presses through a modal that also
contains off-screen or disabled components that swallow focus. A test
that walks `Component.getNextFocusableComponent()`/`FocusTraversalPolicy`
programmatically (cheap, Layer-1-ish) versus one that drives real
`KeyEvent`s through `KeyboardFocusManager` the way
`FocusFaithfulKeyboardTest` insists on for the existing keyboard suite
(`test/jls/ui/FocusFaithfulKeyboardTest.java`, whose own docstring
recounts a real focus bug that passed the suite until an adversarial
review caught it) are both "keyboard-reachable" by a loose reading of
AC-1, but only the latter matches the Outcome's intent. Nothing in #756
picks one.
*Recommendation:* name the mechanism explicitly (real `KeyEvent`
dispatch through the live focus owner, per the project's own established
practice) and add a criterion that the test itself is proven to catch a
real, not merely synthetic, unreachable-focus bug — not just the seeded
one in AC-2, which the author controls.

**4. [Medium] AC-2's "seeded dialog on a scratch branch, red run's transcript committed" is a manual, unverifiable ritual, not a CI-checkable falsification test — and it is gameable.**
Quote: *"A seeded keyboard-unreachable dialog on a scratch branch must
fail the build, and that red run's transcript is committed before any
ratchet pass is counted."* Nothing enforces that the committed transcript
actually came from running the seeded regression — a transcript is just
text; an implementer under time pressure can hand-write a plausible
failure log without ever running the seed, and no test, CI check, or
schema is proposed to distinguish a real red run from a fabricated one.
Compare to how `DialogCoverageRatchetTest`'s own docstring frames
"headless by design ... reads bytecode only" as the credibility anchor —
#756 has no equivalent anchor for its red-run claim. This is the same
"falsification-first" pattern used across the CAP-26 fleet (#549, #550,
etc.), and it is sound as an engineering *practice*, but as a written
acceptance criterion it cannot be checked by anyone who wasn't in the
room when it happened.
*Recommendation:* make the seed itself a permanent, checked-in test
fixture (e.g. a small `@Disabled`-by-default or separately-run "meta"
test that constructs a deliberately-unreachable dialog and asserts
`OperabilityRatchetTest`'s underlying assertion helper flags it) so the
falsification proof is a repeatable CI artifact rather than a one-time
transcript nobody can re-derive.

**5. [Medium] Scope risk: "every dialog and window surface" is a much larger surface than a 0.5–1 mw task band suggests, once non-element windows are included.**
The task's own `band_mw: 0.5-1` implies a small task, consistent with
"just write one enumeration test" — but finding #1 above shows the
enumeration mechanism for non-element windows doesn't exist yet, and the
existing analogous sweep (`DialogConstructionSmokeTest`, backed by
`DialogCoverageRatchetTest`) needed a hand-maintained exemption map even
for the narrower element-dialog case (`Gate`, `Group`, `Pin`, `State` —
lines 48–57). Extending "reachability" analysis (which needs the dialog
actually *constructed and focus-initialized*, per `@Tag("display")`
Xvfb-substrate tests like `ComponentIdentityTest`) rather than just
"constructed and cancelled" (the cheaper `DialogConstructionSmokeTest`
bar) to every window surface, including modal chains and the main editor
frame's menu-triggered dialogs, is a bigger lift than the band suggests.
*Recommendation:* either shrink AC-1's scope explicitly to a named subset
(e.g. "all `ElementFormDialog` subclasses, extended in a follow-up task
to non-element windows") or re-band the task to reflect building both the
enumeration mechanism and the reachability walk.

**6. [Low] `ordering_after: []` is inconsistent with the issue's own dependency framing.**
The Outcome paragraph calls this task "the floor every other CAP-26 claim
stands on," and #549's body separately says this ratchet is "the
substrate FEAT-C26-3's screen-reader work stands on" and depends on
"the component-identity work landed in #210" for accessible names. Yet
`ordering_after: []` declares no ordering dependency on anything,
including #210, which finding #1's registry gap and finding #3's
reachability-mechanism gap both implicitly assume is already in place
(stable component naming makes failures diagnosable; without it, a
red `OperabilityRatchetTest` failure just names an anonymous
`JButton@6f...`). This is a minor documentation gap, not a blocker, but
it undercuts the "floor" framing.
*Recommendation:* add #210 (and, if the enumeration mechanism is
extended per finding #5, #78) to `ordering_after`.

## What's solid

- The falsification-first shape (red run before any green counts) is the
  right instinct for a ratchet test guarding against exactly the kind of
  silent regression the Outcome paragraph describes.
- AC-3 ("a newly added dialog is covered automatically ... adding one
  does not require editing the test") is a well-posed, checkable
  requirement in isolation, and matches the project's stated direction in
  `ARCHITECTURE.md`'s "Adding an element today" section, which flags the
  lack of a registry-driven sweep as exactly the kind of manual-list
  problem #78 is meant to retire.
- The dependency on #549/#75/#210 is real and traceable; this is not an
  issue inventing infrastructure out of nothing — the checklist
  (`docs/keyboard-a11y-verification.md`) and the Layer-2 Xvfb substrate
  (#162) it would need already exist and are fit for purpose.

## Verdict rationale

Two of the four acceptance criteria (AC-1's "registry," AC-4's "K9")
reference things that do not resolve in this checkout, and AC-2's
falsification mechanism is a manual ritual rather than a checkable CI
artifact. These are fixable by rewording and by borrowing patterns
already present in the codebase (bytecode sweep, checked-in seed
fixture), but the issue should not proceed to implementation until AC-1
and AC-2 are rewritten to name concrete, checkable mechanisms — an
implementer handed this issue today would have to invent the registry
and the red-run discipline from scratch, silently deciding scope the
issue should have decided.
