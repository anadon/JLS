# Issue #586: FEAT-C35-3: every screenshot in the docs is a build product — the headless-sway rig captures them per release, so no image can outlive the UI it claims to show
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core idea — a manifest-driven, regenerated-per-release capture pipeline
reusing the #101 rig — is sound in principle. But the issue is written as if
it is converting an *existing* body of hand-pasted screenshots, and that
premise is false at HEAD: there are no UI screenshots in README, the hosted
manual (which does not exist), or the in-jar help to convert. That false
premise cascades into gameable acceptance criteria, at least one factual
contradiction with a named consumer issue, and an unstated dependency on
capabilities (animated capture, interaction scripting) nobody has built.

## Findings, most severe first

### 1. AC-5's claim that #551 is a consumer is contradicted by #551 itself

`AC-5: the emitted set satisfies the consumers by name — CAP-27 PF-1/PF-4
surfaces (#545, #551) ... draw from it rather than keeping private copies.`

I fetched #551 (FEAT-C27-4, the SVG gallery). Its own text: *"rides on
export machinery that already ships: the `-i out.svg` batch export"* and,
explicitly, *"CAP-35 (#519) owns the hosted versioned manual and screenshot
pipeline; the gallery is a single page and must not grow into that
scope."* The gallery's images are headless batch-mode SVG renders of
circuit files (`jls -i out.svg circuit.jls`), not GUI screenshots of a
running window/pane produced by the sway rig this issue builds on. #551
names no dependency on #586/CAP-35 anywhere in its own acceptance criteria
or boundary notes; it names #536 and #516 instead. AC-5 asserts a
producer/consumer edge that the alleged consumer denies having.
**Recommendation:** drop #551 from AC-5, or get a corresponding boundary
note added to #551 before citing it here. As written, AC-5 is either wrong
or requires #551 to take on scope it has explicitly refused.

### 2. AC-2 is checking against artifacts that don't exist yet, so it can pass vacuously

I grepped the actual repo: `README.md` has zero `![...]` image
references (only the OpenSSF badge link); `resources/help/**` contains only
element-palette icon gifs and one `keypad.jpg` (an element icon, not a UI
screenshot); there is no hosted manual (`docs/` has 24 files, none of them
a published site — the hosted manual is #519 PF-2, itself unfiled/unbuilt).
So today, "no screenshot referenced by README, the hosted manual or the
in-jar help is a hand-committed file" (AC-2) is **already true, trivially,
because zero screenshots exist anywhere in those three surfaces**. A "check
that fails on any unmanifested image" can be built, merged, and pass on day
one having verified nothing — the pipeline is never actually exercised
against a real hand-pasted image, because none exists to catch. The
issue's own framing ("stops being hand-pasted files") presumes a body of
existing images that isn't there. **Recommendation:** either (a) explicitly
sequence this after #545 lands its planned hand-captured README screenshots
(see #3 below) and require the AC-2 check to be demonstrated catching at
least one real pre-pipeline image before it counts as satisfied, or (b)
rewrite the outcome to "screenshots are never hand-committed *going
forward*" rather than implying an existing problem it is fixing.

### 3. Undeclared ordering dependency on #545, despite `ordering_after: []`

The machine block says `ordering_after: []`. But #545 (FEAT-C27-1, the
README shop-window feature) is the issue that actually proposes adding the
first screenshots to the README, and its own boundary note says: *"CAP-35
(#519) owns screenshot-generation-from-source infrastructure; hand-captured
images here are acceptable until that lands."* That is a real ordering
relationship stated from the other side: #545 is explicitly allowed to ship
hand-captured images *until* #586 lands, implying #586 is expected to land
after and then convert them. Declaring `ordering_after: []` hides this.
Combined with finding #2, doing #586 first is exactly the sequencing that
lets AC-2 pass vacuously. **Recommendation:** add #545 to
`ordering_after`, or explicitly document why sequencing doesn't matter.

### 4. AC-5's promise to serve #545 conflicts with the issue's own scope boundary on interaction scripting

#545 AC-1 requires *"an animated GIF of drawing-and-simulating."* #586's
own boundary section says: *"Interaction scripting is not #101's ... Any
manifest entry needing more than 'boot with this file and screenshot'
depends on that capability [#91], and the dependency must be stated rather
than smuggled into this rig."* Drawing-and-simulating on camera is
interaction scripting almost by definition — placing elements, wiring,
running the simulator, all while recording — yet AC-5 lists #545 as a
satisfied consumer without stating this dependency. Separately, I found no
animated/video capture tool anywhere in the rig: `scripts/wayland-rig.sh`
only ever calls `grim` for single-frame PNG screenshots
(`control.png`, `desktop-before.png`, `desktop-after.png`); there is no gif
or screen-recording facility named in the script, in `README.md`'s
tooling list, or in ARCHITECTURE.md. The GIF half of #545's needs has no
capture mechanism today, is not #101's, and is not owned by #586 per its
own text — so nobody currently owns building it, yet AC-5 claims it is
served. **Recommendation:** either scope the GIF explicitly into this
issue (with the new tooling and cost that implies), or strike "GIF" from
what AC-5 claims to satisfy and file the animated-capture capability as
its own dependency, the same way the boundary note already does for
static-frame interaction scripting.

### 5. AC-4's pixel-tolerance ask depends on #101 work that #101 itself lists as "Not filed"

`AC-4: ... the tolerance is a measured number with its derivation
recorded, in the idiom #101 uses for PIXEL_DIFF_MIN.` I fetched #101
directly: its own decomposition table lists *"planned (P2 calibration):
`PIXEL_DIFF_MIN` set to ~10% of the first green run's observed AE ... Not
filed."* #101's `PIXEL_DIFF_MIN` is confirmed still `"0"` (record-only) in
`scripts/wayland-rig.sh` (`PIXEL_DIFF_MIN="${PIXEL_DIFF_MIN:-0}"`, and the
header comment: *"0 records the metric without gating"*). #586's own text
acknowledges this ("its `PIXEL_DIFF_MIN` is still `\"0\"`... a capture run
must not inherit ... silently") but then requires #586 to produce its own
calibrated, derivation-recorded tolerance *in that idiom* — meaning #586 is
implicitly on the hook for doing #101's own unfinished calibration work (or
duplicating it independently), without that cost being counted anywhere in
`band_mw: "2-3"`. **Recommendation:** either block #586's AC-4 on #101's
calibration task landing first (a real `ordering_after` edge, not stated),
or explicitly budget the calibration work inside #586's own estimate and
say so.

### 6. AC-1's "window and pane" manifest field bakes in a capability the boundary section disclaims

AC-1 describes manifest entries naming "which window and pane." Most JLS
panes worth documenting (the trace/signal window, a specific dialog, a
non-default tab) are not visible on a bare boot — reaching them requires
menu clicks or keystrokes, i.e., interaction scripting, which the issue's
own boundary paragraph assigns to #91 and says must not be smuggled in.
The schema in AC-1 already assumes that capability exists for every
manifest entry, not just the ones the boundary section flags as
dependent. **Recommendation:** scope AC-1's manifest schema explicitly to
"boot + default-view screenshot" for this issue, with a separate, named
field/flag for entries that require #91-class interaction, so the schema
doesn't imply the rig already handles both cases the same way.

### 7. Hidden assumption: dark-theme captures are viable, but dark mode is a known-broken area

The manifest schema names "which theme" as a per-shot dimension. But
ARCHITECTURE.md records, as a live recorded decision: *"A dark default is
out of scope here: ~126 hardcoded chrome/canvas color call sites ... still
fight every look-and-feel and are #76's follow-up."* Capturing
release-quality dark-theme screenshots of a UI whose dark-mode styling is
known to be broken in ~126 places produces either misleading marketing
images or a manifest full of entries that must be suppressed/waived for
dark theme — neither is discussed. **Recommendation:** either scope theme
capture to light-only until #76 lands, or state explicitly that dark
captures are expected to show known rendering defects and are not
release-blocking.

### 8. Feasibility: the release procedure depends on a rig CI already documents as fail-open

AC-1 requires "the release procedure runs that command." #101's own text
states plainly: *"the promoted `gui-wayland` lane fail-opens — if the JBR
CDN download fails, the step ... succeeds without executing P1/P2."* If the
release procedure reuses the same rig with the same fail-open semantics, a
release can proceed with stale or missing captures and a green run — the
exact "image can silently outlive the interface" failure mode AC-3 and the
issue's own title claim to prevent, just moved from CI to the release path.
Making the release-time invocation fail-closed (unlike the CI lane) is a
real design decision and implementation cost that AC-1 doesn't mention.
**Recommendation:** state explicitly whether the release-time capture run
is fail-closed (blocks the release on any capture failure) or fail-open
(the same posture as CI today, in which case AC-3's "fails loudly" promise
is undermined at the one point — release — where it matters most).

### 9. `band_mw: "2-3"` looks underpriced against the dependencies actually pulled in

Once findings #3, #4, #5 and #8 are taken seriously, #586's real scope
includes: #101's own unfinished PIXEL_DIFF_MIN calibration (or an
independent one), new animated/GIF capture tooling nobody has built,
pane-level interaction scripting overlapping #91, and a fail-closed
variant of a rig that's fail-open by design in CI. Any one of these is
plausibly its own multi-week slice; the issue prices the whole thing at
2-3 maintainer-weeks while explicitly disclaiming ownership of most of the
hard parts ("Reuses #101's rig, does not re-own it," interaction scripting
"is not #101's" [nor claimed as #586's]). If those disclaimed parts are
truly out of scope, the AC's that need them (AC-4's calibration, AC-5's
GIF promise, AC-1's "pane" field) should be trimmed to match, not left in
as unscoped requirements riding on a small estimate.

## What's solid

- Reusing #101's rig apparatus rather than standing up a second display
  stack is the right call and is well-justified.
- The explicit refusal to silently inherit #101's two named weaknesses
  (PIXEL_DIFF_MIN=0, fail-open Wayland lane) shows real self-awareness —
  the problem is that awareness isn't followed through into scope or
  estimate (see #5, #8).
- AC-3 ("a manifest entry naming a window, pane or circuit that no longer
  exists fails loudly") is a good, concrete, testable requirement for the
  part of the schema that doesn't require interaction scripting.
- The boundary note distinguishing #101 (gating: "did the GUI boot") from
  #586 (docs pipeline: "is every doc image a build product") is a correct
  and cleanly stated non-overlap, and the one posted comment (pass-2
  dedup note) confirms it holds without merge — that comment, however,
  only checks for issue-cluster duplication and does not touch any of the
  risks above.

## Recommendation

Rework before starting: fix AC-5's #551 citation (finding #1), make the
#545 ordering dependency explicit (finding #3), decide whether the GIF and
dark-theme captures are in scope and price them if so (findings #4, #7),
and resolve whether AC-4's calibration piggybacks on or duplicates #101's
still-unfiled task (finding #5). As written, an implementer could satisfy
every stated AC while shipping a pipeline that has never converted a real
image, doesn't serve one of its two named consumers, and silently
excludes exactly the capture cases (panes, animation) that make the
feature worth having.
