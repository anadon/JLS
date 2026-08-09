# Issue #666: TASK-C111-6: `GUI boot (Windows, WindowStation)` earns its required check — a blank window or an unbootable jar turns the merge red
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

The README publishes a supported-desktop matrix (Windows / macOS / X11 / Wayland-native /
headless) as a promise to students on lab machines. #111 and #265 exist because that promise
is currently kept by a Linux-only gate plus advisory lanes. So the *end* this task serves is:
**a shipped jar that cannot start on a supported desktop must not reach a tag.** That end is
squarely on the project's trajectory and I endorse it.

The *route* — accrue 20 runs of `windows-gui`, drop `continue-on-error`, register the byte-stable
name in branch protection — is where I disagree, on four counts, in descending importance.

## 1. The promotion oracle does not test the thing the title promises

The title says a blank window turns the merge red. It would not.

`scripts/windows-rig.ps1:160` captures `SystemInformation::VirtualScreen` — the *whole desktop*,
not the JLS window — and `Get-ColorCount` (`:181`) samples that bitmap and returns the distinct-ARGB
count. AC 3 promotes "unique-colour count greater than 1" to a gating criterion. A Windows desktop
with a taskbar and a background clears that bar before JLS ever launches. The other half of the
conjunction, `Get-WindowCount` via `MainWindowHandle`, is satisfied by any realized top-level frame,
painted or not.

Concretely: ship a regression where `SimpleEditor`'s canvas throws on its first `paintComponent`
and the frame comes up empty — `MainWindowHandle` is non-zero, the desktop has many colours,
exit 0, green check. The gate proves *the capture API worked*, not *JLS painted*.

Note the sibling rig already solved this better: `scripts/wayland-rig.sh:222/330/349` captures
`desktop-before.png` and `desktop-after.png` around the launch and compares them with
`compare -metric AE`. That is a real oracle — it proves the screen changed *because JLS mapped*.
The colour-count check is what you write when you cannot diff; here you can. Even staying wholly
inside the issue's frame, AC 3's criterion should be the before/after diff, not the colour count.

## 2. Reframing: make the issue's own "fallback" the primary — prove the boot from inside the JVM

AC 4 treats in-JVM `java.awt.Robot` self-capture as a contingency for when hosted runners refuse
capture. I would take it now, and go further: the interesting facts about a boot are all
*observable from inside the process*, and none of them need a window station.

Split the assertion the rig conflates:

- **(a) "the shipped artifact starts and realizes a working UI"** — main class resolves out of the
  shaded jar; `installLookAndFeel` (`src/jls/JLSStart.java:994`) picks the Windows system LAF and
  it actually loads; fonts resolve; the frame is displayable; the menu bar is realized; **zero
  uncaught EDT throwables**; the canvas completed ≥1 paint. Every one of those is a boolean the
  app can write to a probe file under `-Djls.bootprobe=<path>`, deterministically, in ~2 seconds,
  with no pixels and no flake. This repo already has exactly this idiom: `test/jls/HeadlessCanaryMain.java`,
  `BootListenerHygieneTest`, `DefaultExceptionHandlerWatchdogTest`.
- **(b) "the window is not blank on this platform's real compositor"** — genuinely needs pixels,
  is inherently flaky, and the issue itself concedes it may be unreachable on hosted runners.

Gate on (a). Keep (b) as an uploaded artifact and a non-required lane, forever. That inverts the
risk profile of this task: (a) can be promoted honestly because it is deterministic, and (b) stops
being a promotion blocker for something it was never able to prove anyway. It also catches the
blank-window class *better* than the screenshot does — an EDT-exception counter and a paint counter
name the defect; a colour histogram does not.

Note what (a) does **not** need: PowerShell, `System.Drawing`, `MainWindowHandle` polling, a
selftest with injected stubs. One Java class, one flag, all four platforms.

## 3. Reframing: four rigs are one rig

`wayland-rig.sh` + `x11-rig.sh` + `macos-rig.sh` + `windows-rig.ps1`, with their four selftests,
are ~2,500 lines in two languages implementing one algorithm: launch `HelloSwingControl` to
classify environment-vs-app, launch the jar, wait for a window, capture, judge, exit 0/1/2.
Four independent implementations of one taxonomy means four independent ways to get it wrong —
and this task's own history proves it: first light (run `30322375242`) died on a PowerShell
scoping bug in `Start-JavaProcess` and *misclassified a rig fault as a JLS failure*, the exact
thing invariant 6 exists to prevent. That bug is not expressible in the other three rigs.

Under reframing 2 the residue that is honestly per-platform shrinks to two lines: how to bring up
a session (nothing on Windows/macOS; sway or Xvfb on Linux) and how to grab pixels (`grim` /
`import` / `screencapture` / `CopyFromScreen`). Everything else — control-frame-first, the
timeouts, the 0/1/2 taxonomy, the verdict — belongs in one shared harness, tested once. That is
the seam to cut along, and this task is the last of the four rigs, i.e. the moment the duplication
is complete and the refactor is cheapest to justify.

## 4. Reframing: stop registering required checks by hand

AC 2 requires a maintainer console action plus a confirming comment. #111 Open Question 3 flags this
as a DoD blocker; #265, #101, #188 and #190 each carry their own copy. That is ~10 hand-registered
byte-stable strings, and invariant 3 ("names stay byte-stable") exists only to protect a registry
that lives outside the repo. The rename already bit once — #111 §2 records the Linux lanes being
renamed away from the predicted `GUI boot (Wayland, JBR)`.

The standard alternative: one aggregator job — `ci-required`, `if: always()`, `needs: [...]`,
fails if any dependency did not succeed — registered in branch protection exactly once. Promotion
then becomes *adding a job to a `needs:` list in `ci.yml`*: a reviewable diff with git history,
instead of a console click whose only record is an issue comment. Job names become free to change.
Open Question 3 disappears for all four trackers, not just this one. (Mechanically, advisory lanes
simply stay out of `needs:` rather than carrying `continue-on-error`; the flag's remaining job is
cosmetic run-level colour.)

## 5. The burn-in machinery was built once and not shared

`ci.yml:8-12` runs the nightly cron for `gui-wayland` alone — ISSUE-AMBIGUITIES §6 resolved #101's
P3 exactly so a record could accrue independent of push traffic — and `ci.yml:724` opts `windows-gui`
*out* of schedule events. So this task's 20-run record is serialized behind push cadence, as are
`gui-x11`'s and `macos-gui`'s, while the one lane that already has its answer runs nightly. Widening
the cron to "every lane currently accruing a record" is a one-line `if:` change that unblocks three
promotions in parallel. Pair it with a `scripts/promotion-record.sh` that derives the record from
`gh run list --json` for a given check name: the record becomes a computed fact, reusable by #111,
#265, #101, #188, #190, rather than run IDs copied into comments by hand.

## What I keep from the issue as written

The control-frame-first classification and invariant 6 (rig faults are exit 2, never JLS's fault)
are genuinely good design and should survive into the shared harness unchanged. The boundary
against the `@Tag("display")` suite is right: W3 exercises UI classes in-process on the test
classpath; only this lane exercises the *shaded artifact's* `Main-Class`, manifest and module
resolution. That is the durable value here, and reframing 2 preserves it — the probe is written
by the jar's own JVM, launched as a user launches it.

## What I am explicitly disregarding

AC 3's `unique-colour count greater than 1` as a promotion criterion (it does not detect a blank
JLS window), and AC 2's branch-protection console registration as the promotion mechanism (it
should be a `needs:` edit). AC 4's fallback should be promoted to the plan rather than held in
reserve. AC 1's 20-run discipline I keep — applied to the deterministic boot probe, where 20 green
runs mean something, rather than to a screenshot gate whose failures are mostly about the runner.

## Trajectory

One more thing worth naming: ARCHITECTURE.md is the contributor's map and mentions none of this.
Roughly 2,500 lines of GUI-boot rigging, a four-platform failure taxonomy and a promotion protocol
that governs every lane in the repo are documented only in `ci.yml` comments and issue bodies. If
the rigs are unified per reframing 3, that unified contract — boot proof, taxonomy, promotion —
earns a section there. That is what turns this program from CI plumbing into part of the project's
stated architecture.
