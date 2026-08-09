# Issue #549: FEAT-C26-6: a keyboard-unreachable dialog fails the build — the standing a11y checklist becomes a CI ratchet over reachability and accessible names
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "a test that catches unreachable dialogs." The end is: **a contributor cannot
write an inoperable dialog in the first place.** #75 already proved the behaviors
work once; #549 exists because "works once" decays. The project's whole arc agrees
with that end — `HeadlessCoreRatchetTest`, `NotificationRatchetTest`,
`NullMarkedRatchetTest`, `SocketConfinementRatchetTest`, `PointerApiRatchetTest`,
`DialogCoverageRatchetTest`, the JaCoCo floor: JLS's characteristic move is to turn
a won property into a standing invariant. So the goal is right and the issue belongs.

What I want to reframe is *where the invariant lives*. The issue places it in a new
CI test (`OperabilityRatchetTest`) that extends a manual checklist. There is a
strictly stronger seam already sitting in the tree, and taking it makes most of the
issue's own work disappear.

## Reframing 1 — put the invariant in the constructor, not in a test

`src/jls/edit/ElementFormDialog.java` is already the universal chokepoint for
element dialogs. It owns `ok`/`cancel` and their `dialog.ok`/`dialog.cancel` names,
owns `labelled(label, field, name)` (which in one call sets `setLabelFor`, the #210
component name, and the accessible name), and owns `installDialogBehavior()` — whose
javadoc states that even the big self-laying-out editors "must instead place `ok`,
`cancel` and `getErrorLabel()` itself and call this before showing the window."
Every element dialog in the product passes through that one method.

Audit **there**: walk the content pane from the initial focus owner, and refuse to
show a dialog that has an unreachable focusable descendant, a focusable component
with a blank accessible name, no initial focus owner, or no Escape binding. Throw,
loudly, at construction — the exact idiom `SimpleEditor.paletteSlug` already uses
("refuses to build the tool bar if a palette entry does not map to a registered tag",
`docs/component-naming.md`).

The consequences are large and mostly subtractive:

- **No new enumeration.** `DialogCoverageRatchetTest` (95 lines, ArchUnit over
  `target/classes`) already proves *every* `ElementFormDialog` subclass is swept, and
  `DialogConstructionSmokeTest` already constructs 25 of them for real under a
  display with `EdtViolationDetector` installed. Add the auditor and every one of
  those tests becomes an operability test for free. The issue's gate needs no
  dialog list, no census, and no list to keep in sync — the two lists that exist
  already carry it.
- **The check fires in the contributor's editor**, not 20 minutes later in CI, and
  fires for forks that never run our CI at all.
- **It is total by construction, not by sampling.** "Accessible-name coverage for
  every focusable component" stated as a CI census is a promise you satisfy with an
  exemption list; exemption lists are where a11y gates die. Stated as "the only way
  to add a form input is `labelled(...)`", there is nothing to exempt.

`EdtViolationDetector` is the precedent inside this very package: a cross-cutting
correctness property enforced by an installed hook over the whole sweep rather than
by one assertion per test. The operability auditor is its sibling.

## Reframing 2 — a gate that lives only in the display suite is not a gate

This is the load-bearing objection to the issue as written, and it is checkable:

- `pom.xml`, the `display-tests` execution, carries
  `<rerunFailingTestsCount>2</rerunFailingTestsCount>`. A ratchet you may fail twice
  and still merge is not a floor.
- `.github/workflows/ci.yml` installs xvfb **best-effort** (`... || echo "some
  optional tools unavailable; their tests will skip"`) and then branches:
  `if command -v xvfb-run ... else mvn -B verify`. On an apt hiccup the entire
  display leg silently evaporates and the build is green. AC-1's "seeded dialog
  fails the build" is then false on exactly the days it matters.

So the ratchet's enforcement must sit in the **always-blocking headless execution**.
Two headless pieces, both in shapes this repo has already shipped twice:

1. A source/bytecode ratchet in the `NotificationRatchetTest` shape: forbid raw
   focusable-widget construction (`new JTextField(`, `new JCheckBox(`,
   `new JComboBox(`, `new JRadioButton(`) inside `src/jls/edit/*Dialog.java` and
   `src/jls/elem/**` outside the `labelled(...)` funnel, with an allowlist that only
   ever shrinks. That is "accessible-name coverage over every focusable component"
   delivered *totally and headlessly* — no display, no flake, no retries.
2. The constructor-time audit of Reframing 1, which the headless
   `KeyPadAccessibilityPinTest`-style source pins can back-stop for the handful of
   windows outside the funnel.

The display suite then keeps its proper job — proving the audit is behaviorally
faithful — instead of being the sole custodian of the gate.

## Reframing 3 — make falsification standing, not recorded

AC-1 requires "a seeded keyboard-unreachable dialog in a scratch branch fails the
build, and that red run is recorded **before** any ratchet pass is counted." I am
explicitly disregarding this criterion as written. A recorded transcript in an issue
comment is true once, at one commit, and decays silently thereafter — which is the
exact failure mode #549 exists to prevent, applied one level up.

The project already has the stronger form, twice:

- `test/jls/ui/package-info.java`: "every helper assertion in this package is itself
  pinned by at least one deliberately-failing test (assert-the-assertion, via
  `assertThrows(AssertionError.class, ...)`)".
- `scripts/wayland-rig-selftest.sh`: drives the *unmodified* rig against a stub
  toolchain and asserts each scenario's documented exit code, on every push.

So: ship the seeded dialog as a **permanent test fixture** — a deliberately broken
`ElementFormDialog` subclass under `test/` with a focusable, unnamed, unreachable
field — and assert with `assertThrows` that the auditor rejects it. Every build then
re-proves the gate is not a no-op. That satisfies CAP-26 AC-5's *intent* far better
than its letter, and it costs less than producing and archiving the transcript.

## Reframing 4 — reachability alone is the wrong primitive

The student-visible failure is rarely "a widget was not in the focus cycle." It is
"focus went into the dialog and could not get out," or "focus never landed anywhere,
so the first Tab went nowhere." `installDialogBehavior()` already wires Enter→confirm
and Escape→cancel uniformly; the audit should assert the **closed loop**: a non-null
initial focus owner, a traversal cycle that returns to it, an Escape path off the
window, and a non-blank announcement at every stop. A pure reachability check passes
happily on a dialog that is a roach motel. This is a one-line change to the design
and it is the difference between a compliance property and a usability property —
which is precisely CAP-26 §3 risk 1's own distinction.

## Alternatives considered and rejected (so they aren't re-litigated)

- **An off-the-shelf Swing a11y scanner.** There is no maintained equivalent of axe
  for Swing; `docs/library-survey-2026-07.md` already rejected AssertJ-Swing
  (unmaintained fork chain) and Cacio-tta (JDK-internals coupling) for the adjacent
  job. A ~150-line auditor inside a base class we already own is cheaper than any
  dependency here, and carries no supply-chain surface into a GPLv3 shaded jar.
- **Driving the audit through Orca/AT-SPI in the #101 rig.** Right for #544's spoken
  announcements; wrong here — it makes the *floor* depend on the most brittle
  substrate in the project. Keep the floor in-process.

## Alignment with the larger arc

Under the reframing, PF-6 stops being a sixth parallel workstream and becomes the
completion of the seam #210 already cut (`docs/component-naming.md`'s "Adding a new
element" list is three steps; this adds enforcement to step 3). It also folds
cleanly into ARCHITECTURE.md's honest "sixteen places" element-authoring list and
into #78's registry direction: the fewer hand-wired places, the fewer places to
regress. The 1–2 mw band is credible under this design; under the issue's own
framing (new test + accessible-name census + exemption maintenance) it is a
recurring tax rather than a one-time cost.

One honest boundary the issue should state and does not: the canvas is out of reach
of this gate by construction. `docs/keyboard-a11y-verification.md`'s own
out-of-scope section records that canvas elements are not exposed as accessible
objects at all — so this ratchet floors the *chrome* (menus, palette, dialogs) and
cannot floor the surface where the blind lab path actually happens. That is fine and
correct scoping, but #544 should not be sold as standing on a floor that does not
extend under it. Say so in the issue body, or the capstone's §2 sufficiency argument
overstates PF-6's contribution.

## Bottom line

Endorse the outcome; take the seam. Enforce in `ElementFormDialog.installDialogBehavior()`,
ratchet headlessly in the always-blocking execution, ship the seeded broken dialog as
a permanent fixture rather than a recorded transcript, and audit the closed focus
loop rather than bare reachability. The issue's acceptance criteria then all hold —
except AC-1's "recorded before any pass is counted," which I would replace outright
with a standing assert-the-assertion fixture.
