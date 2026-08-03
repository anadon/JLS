# FEAT-011 - Accessibility, keyboard operability and onboarding

**Status:** proposed | **Cost:** 6-10 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

Every operation the editor offers can be reached without a mouse, the canvas
exposes an accessible scene model that assistive technology can read, the
colors that carry meaning survive the common color-vision deficiencies and
HiDPI scaling, and a person opening JLS for the first time is not dropped onto
a blank canvas with no next move. This is the difference between a tool a
course may assign and a tool a course may not, and it is the surface three open
issues older than a year all share.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-00 | required | Three open issues on the editor surface, all older than a year, all sharing one test substrate |
| CAP-06 | background - not required | A course cannot assign a tool a student cannot operate, and a first-run student must not be dropped on a blank canvas - but this **SUBSTANTIALLY SHIPPED** (`docs/keyboard-a11y-verification.md` is 146 lines at `2d0ca9d`; `test/jls/ui/` holds 34 files of which 28 are test classes; #75's own title records keyboard operability as landed with a named residual), and the residual owns no observation in CAP-06's outcome statement. Corrected 2026-08-03 under D16: this row read `required`, and the filed issues say otherwise - CAP-06's issue #300 omits 355 from `requires_features`, and **this feature's own issue #355 declares `serves_capstones: [296]`**, i.e. CAP-00 and not CAP-06. `CAP-06-course-delivery-autograding.md` carried the same stale grading and is corrected in the same pass. CAP-06's AC-6 asserts the shipped half has not regressed |
| CAP-16 | beneficial | A migrating user arrives fluent in another editor and lands in an unfamiliar one; onboarding is part of whether the migration succeeds |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-008 | The accessibility assertions are UI assertions. Keyboard-only construction, focus order and the accessible scene model can only be ratcheted against the harness and the `jls.edit` floor that FEAT-008 creates; without them each fix is verified once by hand and then decays |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0029 | Keyboard operability | The residual of #75: the accessible canvas scene model and the coverage gaps the shipped verification checklist names |
| TASK-0030 | Visual ergonomics and first-run onboarding | The residual of #76 and #73: the dark variant, the scaling and screenshot matrix, the welcome state and discoverable samples |

## Acceptance criteria

1. Every element in the palette can be placed, moved, configured and deleted
   using only the keyboard, asserted by a test that drives keystrokes rather
   than by a document.
2. The canvas reports a non-empty accessible child set whose entries carry the
   name and role of the elements drawn, so a screen reader reports a circuit
   rather than one opaque panel.
3. No information carried by color is carried by color alone, and the palette
   is checked against deuteranopia and protanopia simulations by a test, not by
   inspection.
4. The application renders correctly at the supported scaling factors on all
   three platforms, pinned by a screenshot matrix whose failures are legible.
5. A first run shows a welcome state offering a new circuit, a recently opened
   circuit and a sample; the samples exist as shipped resources and open.
6. The keyboard verification checklist is executable: each of its items has a
   mutation that turns it red, so the checklist cannot pass vacuously.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 75 | Keyboard operability and accessibility: focus follows the mouse, the menu bar has zero accelerators/mnemonics, and no element can be manipulated without a mouse | closes |
| 76 | Visual ergonomics and platform integration: color-vision-safe semantics, HiDPI scaling, system look-and-feel, dark mode, persistent preferences | closes |
| 73 | First-run onboarding: welcome/empty state, sample circuits, tutorial discoverability, applet-era cleanup, README screenshots | closes |
| 210 | Palette buttons and dialog fields carry no stable component identity (`setName` / `labelFor`), blocking robust automation and assistive-tech labelling | informs, **closed** - component naming is the mechanism criterion 2 builds on |
| 162 | UI-layer coverage: a CI display substrate, dialog-construction coverage for all 23 element dialogs, and interactive-simulator smoke | overlaps - the substrate these assertions run on belongs to FEAT-008 |

## Design notes

Substantial parts of all three issues have shipped and the tasks must be scoped
to the residual rather than re-authored. The focus-follows-mouse grab is gone,
shared Actions carry accelerators and mnemonics, `docs/keyboard-a11y-verification.md`
is a standing checklist with re-runnable mutations, the color-vision-safe
palette is enforced by a test, FlatLaf is adopted as the look-and-feel under a
policy test, and the applet-era tutorial leftovers are removed under a content
test. What is left is the accessible canvas scene model, the dark variant
(blocked by hardcoded colors in the renderers), the scaling matrix, and the
welcome state with shipped samples.

Criterion 2 is the expensive one and it is the one a course actually needs. It
is also the one with a shipped prerequisite: stable component identity, whose
absence #210 named and whose presence is what lets an accessible child carry a
name.

## Risks

- **The accessible scene model is unbounded if scoped as "expose everything".**
  Scope it to the drawn element set plus the selection, not to wires and
  geometry, or the cost is not 6-10 mw.
- **A dark variant touches every renderer.** The colors are hardcoded across
  the renderer classes; a variant is a mechanical change with a large blast
  radius and no test that currently detects a miss. Criterion 3's test must
  cover both variants or the second one rots.
- **Screenshot matrices are the most reformat-fragile check in the plan.** If
  the matrix cannot be made stable it should assert layout invariants rather
  than pixels.

## Evidence

- Shipped keyboard work at HEAD: `docs/keyboard-a11y-verification.md`, a
  140-line checklist with five re-runnable mutations; the `mouseEntered` focus
  grab removed from `src/jls/edit/SimpleEditor.java`.
- Shipped visual work: the color-vision-safe values in `src/jls/Theme.java`
  enforced by `ThemeTest`; FlatLaf adoption recorded in
  `docs/flatlaf-evaluation-2026-07.md` and pinned by a look-and-feel policy
  test.
- Component identity, the mechanism criterion 2 needs:
  `docs/component-naming.md`; issue #210, closed.
- Owner: **UNOWNED** in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 6-10 mw; TASK-0029 and TASK-0030 total 4 wk.
  The two tasks are the leading slices; the residual is the accessible scene
  model across the element vocabulary and the scaling matrix on three
  platforms, which no task id names. Do not read 4 wk as the feature.
