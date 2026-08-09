# Issue #758: TASK-C549-2: accessible-name coverage is ratcheted, and the standing a11y checklist splits into automated checks and an explicitly manual remainder
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the issue

TASK-C549-2, part of feature #549, orders after TASK-C549-1 (#756). Two
deliverables: (1) a CI ratchet failing when a focusable component in `src/`
has a blank/missing accessible name, extending #210 and the KeyPad
accessible-name work from #75; (2) reclassifying every row of
`docs/keyboard-a11y-verification.md` (146 lines, confirmed by reading the
file) as "automated" or "manual," with every automated row keeping a
red-turning mutation, feeding #547's VPAT generator.

## Findings, most severe first

**1. AC #1's own ratchet mechanism is left vague enough to be the exact
"vacuous pass" the issue warns against in AC #3.** The repo already has two
structurally different a11y-test idioms: a headless *source-grep pin*
(`test/jls/KeyPadAccessibilityPinTest.java` — asserts a string like
`getAccessibleContext().setAccessibleName("Show keypad")` literally appears
in `KeyPad.java`) and a display-tagged *live component walk*
(`test/jls/ui/PaletteButtonAccessibilityTest.java` — boots the real editor,
reads `getAccessibleContext().getAccessibleName()` off actual `JButton`
instances). `docs/standards-adoption/03-accessibility-conformance.md:518-526`
proposes exactly the generalization AC #1 wants —
`AccessibleNameCoverageTest`, a *live* walk over every booted window and the
`DialogConstructionSmokeTest` sweep. AC #1's text, "a scratch component added
without one turns it red," is satisfiable far more cheaply by a source-grep
pin that checks call sites textually — which would pass on a component whose
name is set conditionally, computed at runtime, or inherited from an empty
`Action.NAME` (the exact real bug the doc records fixing in
`SimpleEditor.makeElement`). The issue never states which mechanism is
required, so a source-grep implementation satisfies the letter of AC #1 while
missing the class of bug #75 actually found. Recommend: name the mechanism
explicitly — "a live component-tree walk over every window/dialog the app
can construct, not a source scan" — the same way AC #3 already insists
automated *rows* aren't vacuous.

**2. Scope boundary with the canvas and `Trace` is unstated, and the gap is
exploitable.** `docs/keyboard-a11y-verification.md:123-127` and
`ARCHITECTURE.md` both record the canvas as a single custom-painted
component with no per-element accessible children — correctly out of scope.
But `docs/standards-adoption/03-accessibility-conformance.md:303-311` records
a second, *not* out-of-scope gap: `src/jls/edit/Trace.java:20` is
`MouseListener`/`MouseMotionListener`-only, **not even focusable**, so it is
invisible to a "focusable component has no accessible name" ratchet by
construction — the ratchet cannot fail on a component that was never made
focusable in the first place. Shipping AC #1 gives the false impression that
accessible-name coverage is now enforced project-wide, while a known,
already-documented, non-trivial a11y hole (the trace window) sails through
untouched and unmentioned. The issue should either explicitly disclaim
`Trace` (mirroring #75's canvas disclaimer) or require the ratchet to also
flag components that *should* be focusable-and-named but aren't currently
focusable at all.

**3. AC #2 is gameable by mass-reclassification.** "Every row... classified
as automated or manual, with no unclassified rows" can be satisfied in one
commit by labeling every not-yet-automated row "manual" — trivially true,
and the AC's own text is silent on any criterion for what counts as
"mechanizable" (a word used only in the Outcome prose, not the acceptance
criteria). AC #3 guards against a *fake automated* row (must have a
red-turning mutation) but nothing guards against a row that is genuinely
mechanizable being declared manual to avoid the work. Recommend adding a
criterion requiring a stated reason per manual row (mirroring the existing
"Deliberately out of scope / deferred" section's practice of always giving a
reason), or a review gate that questions any row classified manual where an
equivalent automated row already exists elsewhere in the table for a
structurally similar behavior.

**4. Feasibility: the 0.5–1 maintainer-week budget looks optimistic against
this repo's own cost model for the adjacent work.**
`docs/standards-adoption/03-accessibility-conformance.md:755` prices "the
accessible-tree golden + name-coverage ratchet + target-size test" *alone*
at 2 maintainer-days (~0.4 mw) — and that is for the coverage ratchet only,
without the second deliverable here (row-by-row reclassification of 146
lines with a red-turning mutation demonstrated for every existing automated
row, per AC #3). #758 bundles both for the same 0.5–1 mw as the doc's
narrower estimate for one of the two pieces. AC #3 in particular requires
proving a negative (a mutation that turns each automated row red) for
roughly a dozen existing rows in the table — that alone is a nontrivial
verification burden the estimate doesn't obviously cover.

**5. Hidden dependency on #756's infrastructure is unstated, risking
duplicated, redundant test harnesses.** `ordering_after: [TASK-C549-1]` sets
#756 first, and #756's AC #1 (issue #756 body) requires
`OperabilityRatchetTest` to "enumerate dialog and window surfaces from the
registry rather than a hand-maintained list." No such registry exists yet in
the tree — the closest analogue,
`test/jls/ui/DialogCoverageRatchetTest.java:34-57`, is explicitly a
hand-maintained `Set`/`Map` of dialog classes with exemptions, the opposite
of what #756 promises. #758's own AC #1 needs the same kind of
window/dialog/control enumeration to walk "every focusable component in
`src/`." The issue never states that #758's ratchet must reuse whatever
enumeration #756 builds; absent that, the two tasks each risk building their
own component-tree walker over largely the same surface (menus, toolbar,
every element dialog, KeyPad), doubling maintenance cost and doubling the
flake surface under the `@Tag("display")` Xvfb substrate the codebase is
already candid about being fragile (`gui-wayland` lane history cited in the
same doc). Recommend the issue name the shared enumeration explicitly as an
interface #758 consumes from #756, not reinvents.

**6. The task/feature/capstone hierarchy this issue cites is not GitHub-native
and can silently drift.** `issue_read get_parent` on #758 returns
`parent: null` and `get_sub_issues` on #549 returns `[]`, despite #758's YAML
front matter declaring `part_of_feature: 549` and #549's body listing this
exact ratchet as one of its own acceptance criteria (#549 AC bullet 2, nearly
word-for-word identical to #758 AC #1). The entire task/feature/capstone
graph (#549 → #756, #758 → #547 → #753, and onward) is tracked purely by
convention text in issue bodies, not by GitHub's actual parent/sub-issue
linking. Closing #758 will not update #549's checklist, and nothing enforces
that #549's own AC bullets 2–3 get marked satisfied when #758 closes — the
same numbering-collision risk the project's own
`docs/standards-adoption/03-accessibility-conformance.md:3-15` explicitly
warns about for a different pair of numbers. Low severity on its own, but
compounds with finding 5: without native linkage, nothing mechanically
verifies #758 actually consumed #756's promised registry rather than
duplicating it.

## What's solid

- AC #5 draws a clean, explicit boundary against re-absorbing #75's
  named residual (File>Close accelerator, GUI HDL-export entry, AT pass) —
  good scope discipline, and consistent with #75's own body, which lists
  those same three items as its own outstanding planned tasks.
- The citations to #210 and #75 are accurate: both issues, read in full,
  support the claims made about them (component-identity naming landed in
  #210; KeyPad accessible-name pins landed under #75 via PR #252).
- The ordering chain #756 → #758 → #753 (verified via issue bodies) is
  internally consistent — #753's `ordering_after` explicitly names
  `TASK-C549-2`, so the forward reference in #758's AC #4 ("#547's VPAT can
  consume the automated/manual split") is not a dangling promise.
- The core idea — generalize three existing named-component a11y tests
  (`PaletteButtonAccessibilityTest`, `KeyPadAccessibilityTest`,
  `MenuMnemonicAndAccessibleNameTest`) into one coverage ratchet — is a
  small, well-precedented, low-risk step given the pattern already proven
  three times in this codebase.

## Recommendation

Not a block, but rework before implementation starts: (a) pin down the
ratchet's verification mechanism (live walk, not source-grep) in the AC
text itself; (b) explicitly state the `Trace`/canvas boundary the same way
#75 does; (c) add an anti-gaming clause to AC #2 requiring a stated reason
per manual-classified row; (d) name #756's promised registry as a required
input rather than leaving reuse implicit.
