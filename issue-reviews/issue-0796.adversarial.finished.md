# Issue #796: TASK-C586-1: a committed capture manifest names every screenshot, and one command regenerates the whole image set on the existing headless-sway rig
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

A committed manifest names each screenshot (circuit, window/pane, theme); one
command regenerates the image set on #101's `scripts/wayland-rig.sh` rig; any
entry needing scripted interaction states its dependency on #91 rather than
adding `wtype` scripting to the rig itself. Part of feature #586.

## Findings, most severe first

**1. "Reuse #101's rig" is asserted, but the rig as it exists today cannot do
any of the three things the manifest needs it to do — this is a scope
contradiction, not a detail to fill in later.**
`scripts/wayland-rig.sh` (read in full) is a single-shot script: it boots
sway, launches a hardcoded control frame, then launches JLS with
`"$JAVA" -Dawt.toolkit.name=WLToolkit -jar "$JAR"` (line 308) — **no circuit
file argument, no window selector, no theme selector** — waits for one
window, and writes exactly two fixed-name screenshots
(`desktop-before.png`, `desktop-after.png`, lines 222 and 330). It has no
parameter for "which circuit," "which window/pane," or "which theme," and no
loop or multi-shot mode: one invocation = one screenshot of whatever the
default boot produces. AC-1's "one command regenerates the full image set"
and AC-3's "a manifest entry naming a window, pane or circuit … fails
loudly" both presuppose per-shot addressability the rig does not have.
Delivering this task therefore *requires* extending `wayland-rig.sh`'s
interface (new env vars/args for circuit path, target window, theme,
possibly a multi-shot loop inside one compositor session to avoid re-paying
sway/JBR/JLS boot cost per shot) — which is exactly the kind of change #101
frames as touching a **stable, owned surface** ("Provides (stable
surfaces): `scripts/wayland-rig.sh`: consumes `JBR_HOME`…") that #101, not
#796, is supposed to own. The issue's own boundary language — "Reuses #101's
rig, does not re-own it" (quoted from parent #586, which #796 operationalizes)
— is not reconcilable with what AC-1/AC-3 actually require without either
(a) #796 quietly extending #101's interface (contradicting "does not re-own
it") or (b) #796 wrapping/forking the rig (contradicting "rather than
standing up a second display apparatus"). Neither path is named.
*Recommendation:* either scope a small, explicit interface addition to
`wayland-rig.sh` (e.g. `CIRCUIT_FILE`, `TARGET_WINDOW_REGEX`,
`THEME_NAME` env vars, all optional/backward-compatible) as part of this
task with #101's owner sign-off, or state plainly that #796 is blocked on a
prerequisite task against #101 to add multi-shot capability.

**2. "Theme" is a manifest field with no existing mechanism to set it
non-interactively — the issue understates a real implementation gap.**
`src/jls/Theme.java` and `src/jls/UserPrefs.java` (both read) show theme
selection has exactly one path: a persisted `java.util.prefs.Preferences`
value (`THEME_KEY`), applied by `UserPrefs.applyStartup()` before any editor
window exists, and normally changed only via the GUI's theme menu (an
interactive gesture). There is no CLI flag or JVM property analogous to
`-Djls.laf` for theme (`grep -rn "jls.theme"` returns nothing in `src/`).
So a manifest entry with `theme: classic` cannot be realized by "boot with
this file and screenshot" as AC-4 implies is the baseline case — it needs
either a new startup override (a `-Djls.theme=` property, new code) or
externally pre-seeding the `Preferences` backing store before each capture
(itself new, undocumented machinery, and awkward in a disposable CI
container where the backing store may not even persist between runs). The
issue's phrasing groups "theme" with "circuit" and "window/pane" as if all
three are already achievable by non-interactive boot, but only "circuit"
actually is (see Finding 4).

**3. The manifest's "window/pane" axis collides with the issue's own
interaction-scripting carve-out, and the carve-out points at a capability
that doesn't do what's needed.** AC-4 says any entry needing more than "boot
and screenshot" must depend on "#91's interaction-scripting capability"
rather than smuggling `wtype` into the rig. But #91, read in full, is titled
*"Automated UI test harness (P5 residual): retire display-suite retry
masking and produce the 20-run zero-flake record"* — its Layer-2 "gesture"
capability is explicitly **synthetic `MouseEvent` dispatch inside an
in-process JUnit test** ("Robot fallback … abandoned for Layer 2 gestures …
synthetic `MouseEvent` dispatch … is deterministic"), run against a JFrame
constructed by the test itself, under Xvfb. It is not an out-of-process,
scriptable driver that could be pointed at a real, separately-launched JLS
process inside `wayland-rig.sh`'s sway/JBR compositor to click a menu item
and open the trace window before a `grim` screenshot. There is no bridge
between "JUnit calls `dispatchEvent` on an AWT component it constructed"
and "an external screenshot rig drives an already-running opaque JLS
process." Naming #91 as the dependency for "open the trace window, then
screenshot" is naming a capability that does not compose with this task's
pipeline — the real prerequisite (an out-of-process input-injection path,
which is what `wtype` actually is, or an IPC hook into the running JLS) does
not exist under any issue number. `grep -rn wtype` over the repo finds it
only in README prose listing available dev-container packages — it is
never wired into any script. Any manifest entry that wants a screenshot of
the trace window, a dialog, or any pane other than the just-booted main
editor window has no real path today, contrary to what AC-4's phrasing
suggests ("states its dependency … explicitly" implies the dependency,
once stated, is satisfiable).

**4. AC-2 ("a check names any image lacking a manifest entry and fails") is
a check on file *presence*, not on file *correctness* — gameable.** A CI
check that fails when a referenced image has no manifest entry is satisfied
by writing a manifest entry for every image, including entries that lie
about what the image shows (wrong circuit, stale window, wrong theme) or
entries whose regeneration silently produces a different-looking image that
still gets checked in without ever being re-verified against the manifest's
own claims. Nothing in the criteria requires that the regenerated image
actually matches its manifest's `circuit`/`window`/`theme` fields (only
AC-4 requires a *pixel-tolerance* check between reruns, not a check that a
given shot is *of what it claims*). A manifest entry could point at the
wrong circuit file and the completeness check would still pass. *(Note:
AC-4's pixel-tolerance criterion actually lives in parent #586, not in
this task's own acceptance criteria — #796 as scoped doesn't even commit to
determinism checking; see Finding 6.)*

**5. "One command … and the release procedure runs that command" is
underspecified against the actual release process.** `.github/workflows/
release.yml` exists and is triggered by pushing a `v*` tag (per README:
"Pushing a `v*` tag publishes a GitHub Release with the runnable jar and
the per-OS installers"). The issue doesn't say whether the capture command
runs as a new step inside that workflow (meaning: the tag-triggered release
job now needs the full sway/JBR GUI rig available and green, turning an
optional/advisory capability into a release-blocking one — a meaningful
new failure mode for every release) or is a maintainer-run pre-release
step whose output is committed before tagging (meaning captures can go
stale between the last manual run and the tag, defeating the "no image can
outlive the UI it claims to show" goal from #586). Both are legitimate
designs; the issue picks neither, so "runs that command" cannot be verified
against a concrete workflow diff.

**6. Determinism/tolerance is silently absent from this task's own
acceptance criteria despite being load-bearing.** #586 (the parent feature)
states AC-4: "captures are deterministic enough to be useful — same input,
same nominal image within a stated pixel tolerance … in the idiom #101
uses for `PIXEL_DIFF_MIN`." #796's own acceptance criteria (1–4, quoted in
full above) never mention a tolerance or a determinism check at all. Since
#796 is filed as the concrete "TASK" implementing #586's manifest-and-
regenerate-command outcome, silently dropping the determinism requirement
means a literal reading of #796 could be satisfied by a capture pipeline
that is not deterministic (different AE/pixel content every run, e.g. from
sway's headless output timing, JBR startup race, or font hinting) — which
would make "one command regenerates the image set" true but useless (every
regeneration is a diff-noise commit). Given #101's own honest-limits
section flags `PIXEL_DIFF_MIN` as still uncalibrated and the Wayland lane as
fail-open on JBR download failure, silence on tolerance in the child task is
not a safe omission — it needs to be pulled up from #586 explicitly or the
gap flagged.

**7. `band_mw: 1-1.5` (a 1–1.5 person-week estimate implied by the
task-id convention seen elsewhere in this tracker) looks materially
under-scoped against Findings 1–3.** Adding a multi-shot capture mode to (or
around) `wayland-rig.sh`, adding a theme-override mechanism to JLS startup,
defining and documenting a manifest schema, writing the completeness check
(AC-2), writing the "fails loudly on missing window/pane/circuit" validation
(AC-3), and wiring a release-time (or pre-release) regeneration step (AC-2)
is several independent pieces of new machinery, not a single afternoon's
work reusing an existing rig verbatim. The issue's own framing ("rather
than standing up a second display apparatus") reads as if the heavy lifting
is already done; Finding 1 shows it is not.

## What's solid

- Loading a specific circuit at boot is genuinely already supported:
  `JLSStart.parseCommandLine` captures a trailing non-flag argument into
  `startFile` (`src/jls/JLSStart.java` ~line 897), so `java -jar jls.jar
  circuit.jls` opens that circuit in the GUI — the "circuit" axis of the
  manifest is real and cheap, unlike "theme" or "window/pane."
- The explicit boundary against smuggling `wtype` into #101's rig is the
  right instinct — it correctly anticipates scope creep into a differently-
  owned artifact, even though (per Finding 3) the escape hatch it names
  doesn't currently lead anywhere.
- Grounding in existing hand-committed images (`resources/help/elements/
  keypad.jpg`, `up.gif`, `down.gif`) confirms the problem this task and
  #586 describe — stale hand-pasted screenshots — is real, not invented.
- `ordering_after: []` is consistent with the codebase: nothing here is
  blocked by other in-tree work beyond the cross-cutting gaps above.

## Verdict rationale

`needs-rework`. The task's premise — that #101's rig can be reused as-is —
does not hold for two of the three manifest axes (theme, window/pane); the
named escape hatch for interactive shots (#91) points at a capability that
cannot drive the rig's out-of-process JLS; the completeness check (AC-2) is
gameable on content-correctness; and the parent's determinism requirement
is missing from this task's own criteria. None of this means the outcome is
wrong to want — it means the task needs either a companion prerequisite
task against #101 (rig interface extension) and a stated design for theme
injection and interaction scripting, or a narrower initial cut (e.g.
"main-editor-window, default-theme shots only, explicitly excluding pane/
theme variation") that matches what today's tooling can actually do.
