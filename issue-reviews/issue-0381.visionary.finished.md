# Issue #381: TASK-0030: a dark background is usable, a scaled display is verified, and a first run offers a next move — the residual of #76 and #73
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is now

After the owner's 2026-08-08 narrowing comment, four of #381's five workstreams
are struck to other owners (#289, #550/#770/#771, #548/#764/#766/#768,
#545/#760/#762/#866, #286, #287). What remains is exactly two things:

1. the scaling/screenshot matrix of `docs/flatlaf-evaluation-2026-07.md:109-123`
   — four platforms × three scale factors × light/dark, plus a visual pass over
   ~30 element dialogs, plus theme QA on three installer targets;
2. #73 §4's n=5 usability trial, provisionally.

The narrowing was correct and well done. My objection is to what survives it.
The remaining issue rests on one load-bearing premise, stated in §11 and again
in §9: *"The screenshot matrix cannot be automated and must be written down as
manual."* That premise was true in July, when `docs/flatlaf-evaluation-2026-07.md`
was written in a container with no display. **It is false at HEAD**, and the
whole character of the residual changes once you notice.

## The premise is false: four automated boot-and-screenshot rigs already exist

`.github/workflows/ci.yml` carries, by name:

```
354:    name: GUI boot (Linux, Wayland/JBR)     # sway + grim, scripts/wayland-rig.sh
502:    name: GUI boot (Linux, X11/Xvfb)        # Xvfb + xdotool + ImageMagick
595:    name: GUI boot (macOS, WindowServer)    # real Quartz, screencapture + sips
723:    name: GUI boot (Windows, WindowStation)
```

Every platform leg §6 calls "physical apparatus … this half is manual" is
already a job that boots the real `jls.JLS` entry point, asserts the window
maps, and captures a non-blank screenshot — on every push. The matrix's
platform dimension is *solved*. What is missing is one dimension: **scale
factor**.

And scale factor is not hardware. `-Dsun.java2d.uiScale=1.25` forces Java2D's
HiDPI transform on any display, including Xvfb, including headless sway,
including a hosted Windows runner at 96 DPI. The project already knows this —
`docs/standards-adoption/03-accessibility-conformance.md:356` says chrome
scaling *"comes from FlatLaf HiDPI / `-Dsun.java2d.uiScale`, and must be
tested, not assumed."* #381 read "must be tested" and reached for a hotel
room full of laptops.

The insertion point is a one-line edit. `pom.xml:283`, in the `display-tests`
execution, is already:

```xml
<argLine>@{argLine} -Djava.awt.headless=${jls.test.headless}</argLine>
```

Append `-Dsun.java2d.uiScale=${jls.test.uiscale}`, default `1.0`, and the
entire 25-file `@Tag("display")` suite becomes runnable at 1.0 / 1.25 / 2.0
for the cost of two more surefire executions or two more CI matrix legs.

## Reframing 1 — assert invariants, don't eyeball pixels

A human staring at 24 screenshots is looking for a small, enumerable set of
defects: clipped labels, dialogs whose fixed pixel sizing no longer contains
their content, hit-testing that stops agreeing with drawn geometry under a
non-integer transform, target sizes that fall below 24 px, bitmaps that go
soft. Every one of those is a machine-checkable property, and the machines
are already written:

- `test/jls/ui/DialogConstructionSmokeTest.java:128` drives
  `jls.edit.ElementDialogs.setup(...)` over the whole element set — that *is*
  the "visual pass over ~30 element dialogs," minus one assertion. Add
  "every laid-out child's `getPreferredSize()` fits its allocated bounds" and
  run it at three uiScales; a clipped label fails the build, at 1.25, on
  Linux, forever, instead of once in a PDF.
- `test/jls/ui/EditorGestureTest` press-drag-select at uiScale 1.25 is the
  single highest-value check in the whole matrix, because canvas hit testing
  (`SpatialIndex`) against a fractional device transform is where a real bug
  would actually live.
- `RenderAssert` / `RenderBoundsTest` already assert paint-within-envelope;
  they generalize to scale for free.
- 2.5.8 Target Size (palette icons "measure exactly 24×24 px … measure it, do
  not assume", same a11y doc) is an assertion, not an observation.

This is the same move the project has made everywhere else and is good at:
`PointerApiRatchetTest`, `HeadlessCoreRatchetTest`, `LookAndFeelPolicyTest`,
`SeedDirectoryTest`, `TutorialContentTest`. JLS's whole trajectory is *turn
the observation into a ratchet so it cannot rot*. A committed findings
document with a date on it is the one artifact shape this project has
otherwise stopped producing.

## Reframing 2 — JLS solved device-independence once already

`test/jls/ui/EditorZoomTest.java` pins a zoom ladder of
0.25/0.5/0.75/1.5/2.0/3.0/4.0, with the comment *"Ladder scales chosen so
model\*scale lands on integers for (72,120,96)"* — those are DPI values. #74
already built an affine model→device transform for the canvas and already
asserts gesture and geometry invariance across it.

uiScale is the same problem with a different multiplier. The elegant framing
is not "verify JLS at three scale factors" but **"the editor is correct at an
arbitrary composed rendering scale"** — one property, tested with `zoom ×
uiScale` as the outer product, subsuming the matrix by construction. That
also localizes the genuinely scale-*dependent* residue to exactly one thing:
the 33 raster GIFs in `src/jls/edit/images/`, which is #287's SVG redraw. If
you accept this framing, the matrix's answer for the icon leg is "blocked on
#287," not "screenshot it and see."

(Out-of-the-box aside, free: `src/jls/images/` is 34 GIFs that no Java source
references — a duplicate of the 33 that `PaletteEntry` actually resolves.
Delete it before #287 redraws anything, or the SVG conversion happens twice.)

## Reframing 3 — the irreducible residue already has a home

What genuinely still needs a human eye is much smaller than the matrix
implies, and it is not "Linux/Windows/macOS at 100/125/200": it is
compositor-level fractional scaling (`wp_fractional_scale_v1` under Mutter or
KWin renders at 2× and downsamples — a different code path from Java2D
uiScale, and the only place a headless rig genuinely cannot substitute),
FlatLaf's custom window decorations, and macOS backing-store behavior.

That residue is three checkboxes, and the project already owns the exact
vehicle for "a human looks once per release":
`docs/wayland-desktop-checklist.md`, which exists *because* a headless
software-rendered rig can diverge from GPU-backed compositors. Adding a
"§N. Scaling" section there is strictly better than committing a second,
differently-shaped, differently-dated manual-QA document — which is what
§14 asks for. One per-release checklist that grows is an asset; two parallel
manual-QA artifacts is the beginning of drift.

## Where this pulls against the arc

- **Claimant count.** #381's own narrowing comment names fourteen other issues
  owning its parts. The project's failure mode is visibly not under-specification;
  it is issue proliferation, each generation re-specifying the same work at
  greater length. The most valuable thing #381 can do for the arc is stop
  being an open issue and become a pointer.
- **The X11 leg contradicts the stated direction.** README's development-tools
  section says plainly *"X11 is deliberately not part of this project's
  tooling: no X server, no XWayland, no X11 utilities"* — while the required
  build runs `xvfb-run` and a `gui-x11` lane installs `xvfb xdotool
  x11-utils`. #381 inherits the July platform list and re-blesses an X11 QA
  leg the project is otherwise walking away from. Whoever picks this up should
  resolve the posture first, not screenshot both.
- **#586 is the better vehicle and knows it.** #586 AC-4 requires a *measured*
  pixel tolerance with recorded derivation, in #101's `PIXEL_DIFF_MIN` idiom.
  Both GUI-boot lanes currently ship `PIXEL_DIFF_MIN: "0"` — a blank frame
  passes today. Calibrating that gate is worth more than 24 hand-taken
  screenshots, and it is the prerequisite for any automated scale comparison.
  The narrowing comment says "check the rig first"; I would go further and say
  the rig *is* the issue.

## What I am disregarding, and why

I am disregarding §14's *"The screenshot matrix is run and its findings
committed as a document plus images"* and §11's *"cannot be automated and must
be written down as manual."* Those criteria encode a July fact (no display in
the evaluation container) that four CI lanes have since falsified. Satisfying
them as written buys a document that is true on its commit date and unowned
thereafter, at the cost of hardware access this fork has no reason to assume,
while leaving the same defects free to reappear on the next push.

## Recommended disposition

Close #381, and in the closing comment place its residual as three lines, not
a new tier:

1. **Scale becomes a build parameter.** A task on the display substrate
   (#162's neighborhood, or a successor to #91) adds
   `-Dsun.java2d.uiScale` to `pom.xml:283`'s `argLine`, runs the
   `@Tag("display")` suite at 1.0/1.25/2.0, and adds the layout-fit assertion
   to `DialogConstructionSmokeTest` and the gesture check to
   `EditorGestureTest`. This absorbs the dialog pass and most of the matrix.
   Note the ordering trap: #91's live residual is *removing*
   `rerunFailingTestsCount=2` (`pom.xml:293`) and producing a 20-run zero-flake
   record — tripling the display suite before that record exists makes the
   record harder to get. Sequence behind #91, not beside it.
2. **The irreducible residue becomes a section of
   `docs/wayland-desktop-checklist.md`**: fractional scale on a real
   Mutter/KWin desktop, FlatLaf decorations, macOS backing store. Three
   checkboxes in an existing per-release procedure.
3. **The usability trial moves to #511**, as the narrowing comment already
   recommends; CAP-27's "under ten minutes to a running, understood example"
   is the same instrument and the trial is homeless here.

And carry forward, to #289, the material the narrowing comment correctly
identifies as this issue's best: role totality/injectivity (§7.10), the
anti-vacuity clause on the ratchet allowlist, and §7.12 claim 5. #289 today
has P4 ("light rendering byte-identical") but carries neither the injectivity
property nor the anti-vacuity clause — that carry is real, unfinished work.
Note also that #289 declares `blocked_by: [286]` while #381 §Status asserts
`blocked_by: []` for the same sweep; that disagreement should be settled on
#289 before either is picked up.
