# TASK-0030 - Visual ergonomics and first-run onboarding

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

**Correction to the registry scope: the color-vision-safe palette, the
system-look-and-feel adoption and the tutorial refresh all shipped at HEAD.**
`src/jls/Theme.java:18-30` records a color-vision-safe `DEFAULT` palette
enforced against normal, deuteranopic and protanopic vision; FlatLaf light is
the default look-and-feel with `-Djls.laf=metal` as the escape hatch, pinned by
`test/jls/LookAndFeelPolicyTest.java`; the applet-era tutorial copy and the
orphaned 21 KB `tutorial.html` are gone, guarded by
`test/jls/TutorialContentTest.java`. Issues #76 and #73 remain open on a
residual with four parts.

1. **The dark variant** (#76). `Theme.java:27-31` states it is "deliberately
   absent for now: element bodies and their labels are drawn with hardcoded
   ... on a dark background would render them illegible. The record already
   carries every role a dark variant needs." Deliverable: the canvas colors
   routed through the `Theme` roles that already exist, a `DARK` variant added,
   and the toggle persisted through `UserPrefs` (which carries `THEME_KEY`,
   `GRID_KEY`, `BACKGROUND_KEY`, `UNDO_DEPTH_KEY` today - four keys, which is
   the whole preference surface).
2. **Scaling verified where it is claimed** (#76). `docs/flatlaf-evaluation-2026-07.md:109-123`
   names three things still open before the adoption is complete: the screenshot
   matrix across Linux X11/Wayland, Windows and macOS at 100/125/200 percent
   (especially Linux *fractional* scale), a visual pass over the element
   dialogs under FlatLaf light, and theme QA on the three installer targets.
   The `-Djls.laf` seam exists so this needs no rebuild. Deliverable: the matrix
   run and its findings committed, plus `flatlaf-extras`/`FlatSVGIcon` toolbar
   icon redraw and the chrome-color cleanup that the same document lists as the
   deliberately-deferred other half of #76.
3. **The welcome/empty state and bundled samples** (#73). `grep` for
   `welcome|firstRun` across `src/jls/` returns nothing and
   `resources/samples/` does not exist. Deliverable: an empty-state panel shown
   when no circuit is open, offering New circuit / Open sample / Open tutorial /
   Open recent (degrading gracefully when there is no recent list); three to
   five sample circuits under `resources/samples/` exposed via File to Open
   Sample, each carrying a header `Text` element naming what it demonstrates.
4. **README screenshots and positioning** (#73). `README.md` carries one badge
   image and no screenshots. Deliverable: two checked-in PNGs (editor with a
   circuit; interactive simulation with a trace), a five-line feature overview,
   an honest positioning paragraph against Logisim-Evolution and Digital, and a
   pointer to the in-app tutorial. The internal comparison material in
   `docs/hdl-support-research.md` is the source; the README is where a student
   can find it.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-011 | The ergonomics and onboarding half. A student's first ten minutes stop being unassisted, and the tool stops being unusable for anyone who needs a dark background or a scaled display |

## Prerequisite tasks

None. Every seam this task uses - `Theme`, `UserPrefs`, `-Djls.laf`, the
tutorial resources and the display substrate - exists at HEAD.

## Acceptance test

`test/jls/ThemeDarkVariantTest.java`, new (extends the existing `ThemeTest`
pattern):

- `everyCanvasRoleResolvesInBothVariants()` - asserts each color role defined on
  `Theme` returns a non-null value for `DEFAULT` and for the new `DARK`, and
  that no two roles collide within a variant.
- `everyWireStatePairIsDistinguishableUnderDeuteranopiaAndProtanopia()` -
  applies the same simulated-vision transform `ThemeTest` already applies to
  `DEFAULT`, now to `DARK`. The dark variant inherits the accessibility bar; it
  does not get an exemption for being new.
- `noElementBodyColorIsHardcodedOutsideTheme()` - a ratchet scanning
  `src/jls/edit/*Renderer.java` for `new Color(` and `Color.` literals, with a
  named allowlist that must be non-empty-checked. **Fails at HEAD** - this is
  the concrete blocker `Theme.java:27-31` names, converted into a test.

`test/jls/UserPrefsThemeTest.java`, extending `test/jls/UserPrefsTest.java`:

- `theSelectedThemePersistsAcrossRestart()` - writes through `UserPrefs`, reads
  back from a fresh instance, asserts the variant survives.

`test/jls/SampleCircuitsTest.java`, new:

- `everyBundledSampleLoadsAndSimulates()` - enumerates `resources/samples/*.jls`
  from the **classpath** (so the assertion holds for the packed jar, the way
  `TutorialContentTest` does it), loads each through `Circuit.load` plus
  `finishLoad`, and runs it briefly under `BatchSimulator`.
- `thereAreAtLeastThreeSamplesAndEachNamesWhatItDemonstrates()` - the
  anti-vacuity clause plus the header-`Text` requirement from #73 §7.

`test/jls/ReadmeOnboardingTest.java`, new - a drift test in the
`TutorialContentTest` family: asserts the README references at least two
checked-in image paths that exist on disk, and that it mentions the in-app
tutorial by its menu path.

The empty-state panel's own coverage lands in `test/jls/ui/` under
`@Tag("display")`, asserting the panel is shown with no circuit open and that
each of its four actions is the same shared `Action` object the menu bar uses -
the identity discipline `EditActionMatrixTest` established.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 76 | Visual ergonomics and platform integration: color-vision-safe semantics, HiDPI scaling, system look-and-feel, dark mode, persistent preferences | closes - items 1 and 2 are its stated residual; the color-vision-safe palette and the L&F adoption already shipped |
| 73 | First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots | closes - items 3 and 4; the tutorial refresh and the applet cleanup already shipped |
| 75 | Keyboard operability and accessibility | overlaps - #73 §7 asked for the menu-bar Edit menu as "the cheap half of the accessibility issue"; it shipped under #75, so do not build it again |
| 162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | depends on - the substrate the empty-state and scaling tests run under |

## Notes

- **The dark theme's blocker is hardcoded renderer colors, and that is a
  mechanical change with a large diff.** `Theme.java` already carries every
  role a dark variant needs; the work is routing `src/jls/edit/*Renderer.java`
  through them. Expect the ratchet in item 1 to be the noisy part of review.
- **The applet-era leftover is a comment, not code.**
  `src/jls/edit/SimpleEditor.java:452` says "Get reference to top so applet can
  put menu in." It is the last one; `grep -rn 'applet' src/` returns exactly
  that line at HEAD. #73 §5 P3's grep condition is otherwise already met.
- **Samples must load on a clean install.** #73 §10 flags this and it is why
  the test reads them from the classpath, not from a working directory.
  `test/jls/SeedDirectoryTest.java` records the seed-directory decision
  (`user.home`, never `user.dir`, issue #130); a sample opener that assumes
  `user.dir` violates it.
- **The screenshot matrix is manual and should be written down as manual.**
  It cannot be a JUnit assertion. The honest deliverable is a committed
  findings document plus the images, in the shape
  `docs/flatlaf-evaluation-2026-07.md` already uses.
- **K9's restatement governs the palette work.** Progressive disclosure, not
  audience exclusion: whatever this task adds must not increase conceptual load
  in the default view for a first-year student drawing an adder.
- **The usability trial in #73 §4 (n=5, screen-recorded) is the issue's own
  acceptance criterion and this task should run it**, recording outcomes in the
  PR. It is not a test, and pretending otherwise would be the second time the
  project mistook a green bar for a user.

## Evidence

- `src/jls/Theme.java:18-31` (the two shipped variants and the recorded reason
  the dark one is absent), `:52-56` (the color-vision-safe default palette);
  162 lines total at HEAD.
- `src/jls/UserPrefs.java:32-38` - the four persisted keys; 265 lines total.
- `docs/flatlaf-evaluation-2026-07.md:109-123` (what could not be verified,
  three named items), `:124-147` (the recommendation and what shipped),
  `:141-146` (the four deliberately-deferred follow-ups).
- `src/jls/edit/SimpleEditor.java:452` - the sole surviving applet reference.
- `src/jls/tutorial/` - four HTML walkthroughs and six images; no
  `tutorial.html`; no "network" string. Verified by `grep -rn 'network'`.
- `test/jls/TutorialContentTest.java:17-26` - the guard that keeps the applet
  copy and the orphan file out of the jar.
- `resources/samples/` - absent at HEAD; `resources/` contains `help` and
  `packaging` only.
- `README.md:3` - the only image is the OpenSSF Scorecard badge.
- Tests present at HEAD: `test/jls/ThemeTest.java`,
  `test/jls/LookAndFeelPolicyTest.java`, `test/jls/UserPrefsTest.java`,
  `test/jls/SeedDirectoryTest.java`.
- Issue #73 §7 (the six-item method checklist), §5 P3 (the applet grep), §10
  (clean-install sample loading); issue #76 title (the five named surfaces).
