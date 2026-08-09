# Issue #101: Wayland GUI rig: boot the GUI on JBR's WLToolkit under headless sway in CI, screenshot it, and publish first-light findings
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the apparatus away and the claim is one sentence, and the issue states it
itself in §5 rule B: *a change that stops the GUI booting — or blanks its first
frame — cannot merge to master.* Everything else — sway, grim, JBR, `swaymsg -t
get_tree`, the AE metric — is instrumentation for that one invariant. The
project's larger arc backs it: `ARCHITECTURE.md` puts nearly all editor behavior
in one ~4k-line `SimpleEditor` state machine, `mvn verify` is headless by
construction (`HeadlessCoreRatchetTest`), and #91's Layer 1/2/3 harness climbs
toward the display from underneath. Nothing before #101 proved `main()` reaches a
painted window. That gap was real and closing it strengthens the arc.

But the three remaining tasks encode a 2026-07 view of how to close it, and the
repository has moved since. Each of the three is, in my reading, either already
obsolete, better solved elsewhere, or solving a problem the project should
dissolve rather than schedule. I endorse the feature and reframe all three.

## Reframing 1 — the P2 calibration task is redundant, and the better gate already ships

I am disregarding the acceptance criterion "`PIXEL_DIFF_MIN` calibrated non-zero
on both lanes."

`scripts/x11-rig.sh:325-370` already carries an armed, fail-closed, *semantic*
blank-frame gate: it polls `import -window $WINDOW_ID`, counts unique colors with
`identify -format '%k'`, and exits 1 if the JLS window screenshot has one color.
It waits out the Swing map-before-paint race with `RENDER_TIMEOUT` instead of
guessing a threshold. `scripts/macos-rig.sh` and `scripts/windows-rig.ps1` use the
same unique-color idiom. Three of four rigs already do the thing the calibration
task is meant to achieve, without a calibrated constant.

`PIXEL_DIFF_MIN` is the worse instrument on every axis. It compares
`desktop-before.png` against `desktop-after.png` — a whole-screen AE count on a
1280x800 pixman surface — so its value moves with the FlatLaf default (#153), with
the ~126 hardcoded chrome/canvas colors #76 will change, with which fonts the
runner happens to have, with a JBR bump, and with any window-geometry change. A
number calibrated to "~10% of the first green run's AE" has no semantic meaning
and no defensible revisit trigger; it will drift into either a no-op or a flake,
and nobody will be able to say which without re-deriving it. Worse, #586 is
already queued to inherit this idiom for documentation-screenshot tolerance
(comment of 2026-08-04) — propagating a magic constant into a second consumer.

Concrete alternative: **delete the `PIXEL_DIFF_MIN` knob from all rigs and port
the unique-color non-blank gate into `wayland-rig.sh`.** The Wayland rig already
reads the compositor tree; the window's `rect` from `swaymsg -t get_tree` feeds
`grim -g "<x>,<y> <w>x<h>"` directly, giving the same window-cropped capture the
X11 rig gets from `import -window`. The gate then reads "the JLS window painted
more than one color", which is true across look-and-feels, fonts, renderers, and
toolkits, needs no measurement, and unblocks #586 with a rule rather than a
number. This makes the calibration task, its blocking Open Question 2, and the
"measurement, not a decision" framing all disappear.

## Reframing 2 — retire the per-lane required-check registration, repo-wide

Task 3 asks a human to type two job names into repo settings, and §4 invariant 4
says those names must never change or the check silently un-registers. That
invariant is enforced by nothing in the tree — I grepped; no test, no schema, no
lint pins those strings. An invariant whose violation is silent and whose
enforcement is a comment is not an invariant.

This is not a #101 problem, it is a repo pattern. `ci.yml` carries the same
maintainer-registration debt for #188 (aarch64), #111 (Windows GUI), #265 (macOS
GUI), and the msi installer lane, each with its own "keep this name byte-stable"
comment. `needs:` appears nowhere in `ci.yml` — all 15 jobs are independent, so
branch protection must enumerate every context by hand, forever, and every new
lane re-incurs the cost.

Concrete alternative: **one aggregating job — `name: All required checks` — that
`needs:` every gating lane and fails if any dependency failed.** Register that
single context once. Then lane names are free to change, new lanes gate the moment
they are added to `needs:`, demotion is a one-line `needs:` edit in the same PR
that demotes the lane (rather than a settings edit racing the merge), invariant 4
evaporates, and #111/#265 inherit the fix for free. Task 3 becomes a
five-line workflow change plus one settings action that never repeats.

## Reframing 3 — the Wayland lane is an upstream watch, not a merge gate

This is the deepest tension. The issue's stated impact is that "Linux users on
Wayland-only sessions gain a continuously verified guarantee that JLS boots
natively." The DoD does not deliver that guarantee, and §5's own honest-limits
note says why: `gui-wayland` fail-opens on a JBR CDN failure
(`ci.yml:421-423`), so the fail-closed gate is `gui-x11` — an XToolkit lane.
After all three tasks land, the merge gate reads "JLS boots under X11", and the
Wayland claim is protected only on runs where a third-party CDN cooperated.

Separate the two claims and it resolves cleanly. JLS owns exactly one piece of
Wayland behavior: the toolkit-selection policy in `src/jls/ToolkitPolicy.java`
(#105) — pure logic, already pinned headlessly by `test/jls/ToolkitPolicyTest.java`
and `test/jls/WaylandStartupCliTest.java`, with zero JBR, zero compositor, zero
network. Whether `WLToolkit` then renders is upstream's code, not JLS's; JLS
cannot regress it and cannot fix it. So the marginal *gating* value of
`gui-wayland` is close to zero, while its value as a daily early warning against a
JBR/Wakefield regression is high — and the nightly cron already delivers exactly
that.

Concrete alternative: **keep `gui-wayland` nightly and advisory (an explicit
upstream watch, with a failure opening an issue rather than blocking a merge), make
`gui-x11` the sole registered Linux GUI gate, and state in the README matrix that
the Wayland-native row is verified by policy tests plus a nightly upstream watch
plus the per-release physical-desktop checklist.** That is honest about what is
guaranteed, removes one name from registration, and answers Open Question 1 by
dissolving it — no tarball mirroring, no fail-close decision, because a watch is
allowed to skip.

Note also that the README's optional-tools section still says "X11 is deliberately
not part of this project's tooling: no X server, no XWayland, no X11 utilities,"
while the hard merge gate now installs `xvfb xdotool x11-utils`. The stated posture
and the verification substrate have diverged; whichever reframing wins, that
paragraph needs to say "X11 is not a product dependency; Xvfb is the verification
substrate."

## The larger arc: four rigs are one rig

`wayland-rig.sh` (366) + `x11-rig.sh` (404) + `macos-rig.sh` (533) +
`windows-rig.ps1` (407) ≈ 1,700 lines, plus ~570 lines of self-tests, all
implementing the same state machine: bring up a display, map
`HelloSwingControl`, classify exit 1 vs 2, launch the jar, poll for a window owned
by the pid, capture, prove non-blank, emit an artifact bundle. Only four
primitives actually differ — start display, enumerate windows for a pid, capture a
region, tear down. The #265 first-light taxonomy in `ci.yml:713-724` (a PowerShell
scoping bug misreported as a JLS failure) is the predictable cost of maintaining
that logic four times in two languages.

Meanwhile `ARCHITECTURE.md` does not mention the rigs at all. The largest body of
new infrastructure in the repo is invisible to the document that claims to be the
map a new contributor needs.

The out-of-box alternative: **make the driver a JVM-side probe and the platform
part a thin adapter.** A `GuiBootProbe` — plausibly a `@Tag("boot")` JUnit test
that launches the shaded jar as a subprocess, or a small tool run by the JDK under
test — owns the control-frame comparison, the exit taxonomy, the timeouts, and the
artifact contract in Java, where it gets ordinary unit tests instead of four
bespoke stub-injection self-test scripts. Each platform contributes only display
bring-up and a capture command. This is the same seam #91's Layer 2/3 already cut
(`RenderAssert`, `EdtViolationDetector`, the `display`-tagged surefire execution),
and it is what #162 is hardening and #586 wants to reuse. Four issues are
independently growing the same substrate; #101 is the one with the most of it
built, so it is the natural place to say "this is a shared component, not a CI
lane" — even if the consolidation itself is a follow-up.

## What I would put in the DoD instead

1. Port the unique-color non-blank gate to `wayland-rig.sh` (window-cropped
   `grim -g` from the tree rect); delete `PIXEL_DIFF_MIN` from all four rigs.
2. Add an `All required checks` aggregating job; register that one context;
   drop invariant 4 and the byte-stability comments across all lanes.
3. Demote `gui-wayland` to nightly upstream-watch; register `gui-x11` only;
   amend the README matrix and the X11-tooling paragraph to match.
4. Replace "post a first-light comment" with a regenerated
   `docs/gui-boot-baseline.md` the rigs produce — a comment is the weakest
   possible home for a baseline #586 intends to cite.
5. File the rig-consolidation seam as a follow-up naming #91/#162/#586 as
   consumers, and add one paragraph to `ARCHITECTURE.md` describing the rigs.

Items 1-3 are each smaller than the task they replace, and each removes a
recurring cost rather than paying it once.
