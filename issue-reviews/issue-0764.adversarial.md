# Issue #764: TASK-C548-1: an Examples menu entry lists the shipped circuits from the classpath — one top-level entry, no other default-view load
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Issue body and machine-block YAML (`task_id: TASK-C548-1`, `part_of_feature: 548`,
`ordering_after: [381]`); zero comments. Fetched the four issues #764 leans
on: #548 (FEAT-C27-2, the feature this task is filed under), #381 (TASK-0030,
the samples/empty-state task it orders after), #73 (the onboarding feature
#381 executes), and #130 (closed — the `user.dir`-never seed-directory
decision, `SeedDirectoryTest`). Read `README.md`, `ARCHITECTURE.md`, and
`test/jls/SeedDirectoryTest.java` in full; grepped `src/jls/JLSStart.java`
for the current menu-bar construction (File/Edit/Element/Simulator menus,
lines ~1257-1710) and for `Action`/`EditOp` usage across `src/` and `test/`
to check AC3's premise against HEAD. Also read the sibling adversarial
review already on file for #770 (`issue-reviews/issue-0770.adversarial.md`),
a closely related TASK-tier issue in the same #381/#73/#548 cluster, since
several of its findings turn out to reproduce here almost verbatim.

## Findings, most severe first

**1. [High] A second, unreconciled UI surface for the same data.** #73's
own decomposition table plans "File→Open Sample" as a File-menu item
listing the bundled samples, and #381's Method explicitly builds
`resources/samples/` plus that same File-menu path ("Add the empty-state
panel with the four actions... New circuit / Open sample / Open tutorial").
#764 instead proposes a brand-new **top-level** "Examples" menu entry
listing the same shipped circuits from the same directory. The issue never
says whether Examples *replaces* File→Open Sample, *coexists* with it, or
*is* it under a new label — AC3's "not to duplicate handlers" gestures at
sharing but never states what the Examples items are shared *with*. Every
reading is a problem: if both survive, a student now has two menu paths to
the identical five-or-so files, which is exactly the "conceptual load" AC4
claims to avoid — a whole extra top-level menu is more prominent than the
single File submenu item #73/#381 already planned, not less. If Examples
silently obsoletes File→Open Sample, that's an uncoordinated contract
change against #73/#381 that neither of those issues records (#73 §7
requires exactly this kind of deviation go through a `REPLAN:` comment).
Recommendation: state explicitly whether #764 supersedes or wires into
#73/#381's File→Open Sample item, and if the latter, name the shared
object.

**2. [High] AC3's "shared `Action` objects... matching #381 P9's identity
discipline" does not hold against the current codebase, and #381's own
version of this claim is an unproven hypothesis, not settled fact.**
`src/jls/JLSStart.java:1389` (`JMenuItem newc = new JMenuItem("New")`) and
`:1407` (`Open`) are plain `JMenuItem`s wired with anonymous
`ActionListener`s, not `javax.swing.Action` objects — confirmed by reading
lines 1383-1432. The only real shared-identity discipline anywhere in the
tree is `EditOp`/`test/jls/ui/EditActionMatrixTest.java`, which pins
cut/copy/paste/delete reachable from canvas, popups, and the Edit/Element
menus — a completely different surface from opening a named sample file.
#381's own H4 states this as an open, falsifiable hypothesis ("**Refuted
if any of the four [empty-state actions] has no shared `Action` today**"),
with a stated fallback of building the Action-ification as part of that
work. #764 instead asserts the identity discipline as a fact to match
against, while depending on #381 landing infrastructure that #381 itself
does not yet claim to have. (This is the identical defect independently
found in the sibling TASK-C550-1 issue, #770 — see finding 2 there — which
strengthens rather than weakens the point: it is a pattern across this
issue cluster, not a one-off typo.)

**3. [Medium] AC2's "a test asserts no path reads `user.dir`" is close to
vacuous for a purely classpath-based mechanism.** `SeedDirectoryTest`
(`test/jls/SeedDirectoryTest.java:19-50`) pins `Util.defaultDirectory()`
and `Util.seedDirectory(String)` — helpers that back `JFileChooser` seeding
for interactive Open/Save. A classpath resource read (`getResourceAsStream`
against `resources/samples/*.jls`) never calls either helper and never
touches `user.dir` at all — there is no directory seed in that code path to
assert against. A test that just checks "the Examples-opening code doesn't
read `System.getProperty("user.dir")`" would pass trivially without
exercising the actual risk #381 names for this exact mechanism: H3, "a
classpath-read sample fails in the packed jar" even though it works fine
under the IDE/test classpath. Recommendation: point AC2 at a
`TutorialContentTest`/#381-P3-shaped packaged-jar smoke test instead of (or
in addition to) a user.dir non-assertion.

**4. [Medium] Silent partial-landing risk against the feature it claims to
implement.** #548 (FEAT-C27-2) — the `part_of_feature` #764 cites — has
five ACs: ≥10 circuits (AC-1), four category types plus an RV32I showcase
(AC-2), an extended `SampleCircuitsTest` (AC-3), a caption-length suggested
exercise per circuit (AC-4), and the same K9/D9 gate (AC-5). #764 covers
only AC-5 and a bare-bones slice of AC-1/AC-3 (menu presence, standard
reader) — explicitly allowing landing with whatever set #381 ships first,
which could be as few as three circuits with no captions or exercises. #764
names no sibling task that owns AC-2 (category coverage) or AC-4
(exercises), so closing #764 risks reading as progress on #548 while three
of its five acceptance criteria remain untracked by any filed issue — the
same partial-landing failure mode already found (with #550 in that case)
in the sibling review of #770.

**5. [Medium] `ordering_after: [381]` is ambiguous between "the samples
sub-deliverable landed" and "#381's Definition of Done is fully closed."**
#381 bundles this task's actual prerequisite (`resources/samples/` plus a
File-menu open action) together with a ~96-site hardcoded-color renderer
sweep, a new `Theme.DARK` variant with CIE76 delta-E verification, a manual
cross-platform/scale-factor screenshot matrix, and a 5-volunteer usability
trial — none of which #764 needs. #764's own `band_mw: 0.5-1` sizing
implies a small task, inconsistent with the literal reading of
`ordering_after` (#381's own checklist is what closes it) that would gate
#764 behind hardware access and human-subject recruiting unrelated to a
menu entry. Recommendation: state explicitly that #764 needs only #381's
samples-directory sub-item, not #381's full closure — #548's own
`ordering_after: [381]` note already does this more narrowly ("extends the
3-5 sample baseline TASK-0030 builds").

**6. [Low] No content-correctness criterion.** AC2 checks that selecting an
entry opens *through the standard reader* and *not via `user.dir`*, but
nothing checks that the entry opens the *right* circuit — a caption/label
that says "Full Adder" but is wired to the counter sample would satisfy
every stated AC. Worth one assertion (label ↔ resolved resource path, or a
loaded-circuit content check) matching #381 P8's "each carries a header
`Text` element naming what it demonstrates."

**7. [Low] AC1's "discoverable with no prior knowledge" is a UX claim
riding inside a structural acceptance criterion.** The only thing a unit
test can realistically assert is that a menu titled "Examples" exists in
the menu bar and is populated — a materially weaker property than
"discoverable," which #73's own IC1 usability trial (n=5, screen-recorded)
treats as something that needs human observation, not a green bar. #764
should either drop the discoverability language from AC1 (it's a menu-bar
entry; presence is the actual testable claim) or explicitly defer the
discoverability claim to #73's IC1/#548's own trial machinery.

## What's solid

- AC5 ("not constructed in headless or batch runs") is concrete and
  testable, and matches the project's existing headless discipline
  (`JLSInfo.batch`, `System.setProperty("java.awt.headless", "true")` —
  `src/jls/JLSStart.java:167-171`).
- The core intent of AC2 — classpath-only reads, never `user.dir` — cites
  the correct prior decision (#130, closed) and points at the right test
  lineage even though the specific test shape needs sharpening (finding 3).
- "Extends the `resources/samples/` mechanism... rather than forking a
  second sample mechanism" correctly identifies and heads off the risk of
  a parallel resource directory, which is the right instinct even though
  the menu-surface duplication in finding 1 shows the instinct wasn't
  carried all the way through to the UI layer.
