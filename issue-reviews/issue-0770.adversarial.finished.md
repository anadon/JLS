# Issue #770: TASK-C550-1: first launch never shows an empty tab pane, and a display-tagged test fails if the launch path ever regresses
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Issue body and machine-block YAML (`task_id: TASK-C550-1`, `part_of_feature: 550`,
`ordering_after: [381]`); zero comments. Fetched the four issues #770 leans on:
#381 (TASK-0030, the panel's actual implementation owner), #73 (the onboarding
feature #381 executes), #355 (FEAT-011, #381's other parent), #76 (theming),
#548 (the Examples-menu feature #770's AC3 conditionally binds to), and —
because #770's own YAML names it and #770 never discusses it — #550
(FEAT-C27-3, the capstone task #770 claims to be part of). Grepped the
checked-out tree for `welcome`/`firstRun`, `resources/samples`, the File
menu's New/Open/Tutorial construction, and the shared-`Action` identity
pattern `EditActionMatrixTest` establishes, to verify #770's premises against
HEAD rather than against the issue's own claims.

## Findings, most severe first

**1. [High] #770 silently drops half of the acceptance criteria of the feature it claims to be part of (#550).**
The YAML header reads `part_of_feature: 550`, and #550 (FEAT-C27-3, "first
launch lands somewhere... and never regressing that is a per-commit gate")
has four ACs: AC-1 (display-tagged gate test — matches #770 AC1), AC-2
(shared-Action wiring + Open Example fallback — matches #770 AC2/AC3), but
also **AC-3** ("A per-commit startup-time regression check exists and holds
the KC-27-1 line: default palette, startup time and first-year conceptual
load unchanged") and **AC-4** ("The starter-circuit-vs-welcome-pane decision
is recorded on this issue with the K9/D9 rationale; whichever loses is
explicitly not built"). Neither AC-3 nor AC-4 appears anywhere in #770's
body, and #770 never mentions #550 in prose — only in the machine block. If
#770 is meant to be #550's complete near-term implementation, closing #770
as done will leave #550's startup-time gate and its recorded
starter-circuit-vs-welcome-pane decision permanently missing with nothing
tracking the gap. If #770 is meant to be a partial slice, the issue doesn't
say which other task owns the rest. Recommendation: either fold #550 AC-3
and AC-4 into #770 explicitly, or add a line naming the issue that owns
them, and cite #550 by number in the body the way the issue already cites
#381, #73, #355, and #548.

**2. [High] AC2's "same shared `Action` instance the menu bar uses" premise does not hold against the current codebase, and #770 doesn't acknowledge the prerequisite work.**
`src/jls/JLSStart.java:1389` (`JMenuItem newc = new JMenuItem("New")`),
`:1407` (`Open`), and `:2097`+ (the Tutorial items) are plain `JMenuItem`s
wired with anonymous `ActionListener`s — not `javax.swing.Action` objects.
The only place a real shared-`Action` identity discipline exists today is
`test/jls/ui/EditActionMatrixTest.java`, which pins `EditOp`s (cut/copy/
paste/delete) reachable from canvas key bindings, popup menus, and the
Edit/Element menus via `ed.editAction(op)` — a completely different surface
from File>New/Open or Help>Tutorial. #381 itself is honest about this risk:
its H4 is explicitly falsifiable ("**Refuted if any of the four has no
shared `Action` today**") with a stated fallback ("create it as a shared
`Action` and route the menu item through it too"). #770 instead states the
identity requirement as settled fact ("wired to the same shared `Action`
instance the corresponding menu item uses") and sizes itself at
`band_mw: 0.5-1` — consistent only if the Action-ification of three menu
items is assumed to be free, pre-existing, or someone else's problem. It is
none of those today. Recommendation: #770 AC2 should state explicitly that
it depends on #381 landing Action-backed construction for New/Open/Tutorial
(not just the panel), and #770 should fail loudly, not silently degrade, if
#381 ships the panel with H4 refuted.

**3. [Medium] "Open Example resolves to #548's set when present, and degrades... when not" names no detection mechanism, so the criterion is gameable.**
AC3 reads as a runtime conditional ("when present" / "when not"), but #548
is an entirely separate, unstarted `tier:feature` issue (provisional
`feat_id: FEAT-C27-2`) that defines no interface #770's welcome-pane code
could probe. Two readings are both consistent with the text: (a) a runtime
check against some registry #548 is expected to expose (undefined by either
issue), or (b) a compile-time swap made whenever #548 eventually lands, in
which case "degrades... when not present" just means "hardcode the #381
baseline forever until someone edits this file." Reading (b) satisfies the
letter of AC3 with a permanently-hardcoded fallback and no actual
degradation logic, which is exactly the kind of criterion that can pass
while the real intent (a self-updating Examples entry point) goes unmet.
Recommendation: name the concrete seam — e.g. an `ExtensionRegistry` lookup
per `docs/extension-points.md`'s pattern (ARCHITECTURE.md's "Extension
points" section), or an explicit statement that this is resolved at build
time by a follow-up PR once #548 exists, not "when present" at runtime.

**4. [Medium] `ordering_after: [381]` is ambiguous between "the panel sub-deliverable exists" and "#381 is fully closed," and the latter is a large, mostly unrelated blocker.**
#381 (TASK-0030) bundles the welcome panel with a ~96-site hardcoded-color
renderer sweep, a new `Theme.DARK` variant with CIE76 delta-E verification,
a manual screenshot matrix across Linux X11/Wayland/Windows/macOS at three
scale factors, and a 5-volunteer usability trial — none of which #770 needs.
#770's own `band_mw: 0.5-1` sizing implies a small task, but if
`ordering_after` is read as "#381's Definition of Done is satisfied" (the
literal reading, since #381's own checklist is what closes it), #770 is
gated behind hardware access and human-subject recruiting that have nothing
to do with a tab-pane emptiness test. Recommendation: state explicitly that
#770 only needs #381's empty-state-panel sub-item (and its shared-Action
wiring) landed, not #381's full closure — mirroring how #548's own
`ordering_after: [381]` note is phrased more narrowly ("extends the 3-5
sample baseline TASK-0030 builds").

**5. [Medium] "Clean preferences state" under-specifies the test's initial conditions; checkpoint files are a separate, unaddressed source of state.**
`SimpleEditor.writeCheckpointInBackground` (`src/jls/edit/SimpleEditor.java:202`)
persists `.jls~` checkpoints to a path under the seed directory governed by
the `user.home`-never-`user.dir` rule (issue #130, `test/jls/SeedDirectoryTest.java`),
which is independent of `java.util.prefs`/`UserPrefs`. AC1 only specifies "a
clean preferences state." If the test harness clears prefs but not the
checkpoint directory, a stale `.jls~` left by a prior test run could
auto-restore a tab and make the pane non-empty for reasons that have nothing
to do with the welcome surface actually firing — the assertion could pass
vacuously, or fail spuriously from cross-test leakage in CI.
Recommendation: AC1 should say "a clean preferences state and no checkpoint
file," matching the isolation `SeedDirectoryTest` already establishes as the
project's pattern for this class of test.

**6. [Low] Calling this a per-commit "gate" elides the project's own documented flakiness in the tier it depends on.**
`pom.xml` and #381 §11 ("Threats to Validity") both record that the
`@Tag("display")` suite is retried twice under Xvfb because "popup
realization is nondeterministic," and #381 explicitly warns "a new display
test that only passes on the retry is a flake, not a pass." #770 upgrades
exactly this kind of test from #381/#73's "polish" framing to a hard gate
("stops being polish and becomes a gate") without saying what happens when
the gate itself flakes on the documented-nondeterministic substrate, or
whether the standard two-retry policy is considered adequate assurance for
a merge-blocking check. Worth one sentence acknowledging the interaction.

**7. [Low] Naming drift between the consuming task and its producer: "Open Example" (#770, #550) vs. "Open sample"/"Open Sample" (#381, #73).**
Likely deliberate forward-naming toward #548's eventual "Examples" menu, but
if #381 ships a button/action literally labeled or keyed "Open sample" (as
its own body specifies throughout), and #770's display-tagged test asserts
against a name string rather than position/role, the naming mismatch alone
could fail the test independent of any real defect. Recommend #770
explicitly tell #381 (or a REPLAN comment) which label wins, or have the
test assert structurally (object identity to the shared Action, not string
match) so the two issues can't silently disagree.

## What's solid

- AC4 (Open Recent stays deferred to #76) is consistent with #73 §6's
  mermaid graph and #550's own note verbatim — no drift there.
- The "consumes, does not duplicate" framing, and the explicit pointer to
  #73/#355's REPLAN protocol if #381's scope moves, is the right discipline
  and is followed correctly for the panel-implementation boundary.
- AC1's core shape (display-tagged test, red-state recorded before the fix)
  matches the project's established Rule-3 pattern used throughout #381/#73
  and is genuinely testable, modulo finding 5 above.
