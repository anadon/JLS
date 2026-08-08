# Issue #673: TASK-C265-7: `GUI boot (macOS, WindowServer)` earns its required check, with TCC denial classified as environment and never as a JLS failure
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the staging vocabulary and #673 asks for one thing: **a change that makes the
JLS jar unable to start on macOS must not be mergeable.** Everything else — the
20-run record, the byte-stable name, the exit taxonomy, TCC — is machinery in
service of that sentence. Judged against the project's arc that goal is
unambiguously right: the README ships a macOS dmg as a first-class install path,
`ARCHITECTURE.md` §"Test layout" and the #162 display layer already treat "can a
real user's window come up and paint" as testable, and #265's own evidence
(`HotkeysHelpAccuracyTest`, `VK_DELETE` → `⌦`) proves macOS-only divergence is
real and catchable.

The route chosen to get there is where I part ways.

## The route has already failed its own re-plan trigger

AC 4 says: "if the non-blank-screenshot gate is permanently unreachable under
TCC, the promotion criterion is re-planned on #265 rather than quietly relaxed to
degraded mode."

That condition has already fired. `.github/workflows/ci.yml` (macos-gui header,
the FIRST-LIGHT TAXONOMY block) records first light — run `30322375242` — as:
control window mapped, window capture returned **0 unique colors**, Screen
Recording withheld. `scripts/macos-rig.sh:394-401` then *relaxes to degraded
mode*, and the fail-open pre-grant step's own comment concedes the sqlite
workaround is expected to lose to SIP. The capture path question (Open Question 2)
is answered: `screencapture` is the path, and it hands back black.

So #673 as written asks to promote the relaxed artifact, while its own AC 4 says
the honest response is a REPLAN on #265. The two cannot both be satisfied. That
alone means this task should not be executed as specified.

## Concrete defect: dropping `continue-on-error` breaks invariant 6

AC 3 asserts a TCC/WindowServer block "is exit 2 and never counted against JLS."
The taxonomy lives entirely in the *script*. The *workflow* consumes none of it:

```yaml
      - name: Run the macOS GUI boot rig
        run: ARTIFACTS_DIR="$RUNNER_TEMP/macos-artifacts" bash scripts/macos-rig.sh
```

Actions fails a step on any non-zero exit. Drop the job-level
`continue-on-error: true` and exit 2 turns the required check red and blocks the
merge — exactly what invariant 6 forbids. AC 2 and AC 3 are therefore
*mutually unsatisfiable* under the current plumbing. This is not a macOS quirk:
`gui-x11` and `gui-wayland` invoke their rigs the same way and are *already*
promoted, so the hole is live today. Whatever else happens, the four lanes need a
classify step (capture `$?`; 2 → `::warning` + `exit 0`) before any of them is
required.

Worse, after degradation the sole remaining gate is `osascript`/System Events —
the *Accessibility* TCC service, sibling of the Screen Recording service that is
already withheld. A required check whose only proof depends on a permission Apple
and the runner-image maintainers can revoke in a monthly image bump puts an
external, uncontrollable owner directly on JLS's merge path. Contributors would
face a red gate no source change can fix.

## The reframing: prove the boot inside the JVM, not on the screen

The rig conflates two questions the project can and should separate:

1. **Did the app boot and paint?** (product behaviour — what #673 actually wants)
2. **Did the compositor display it?** (session integration)

Question 1 needs no OS permission at all. `Window.isShowing()` plus a fired
`windowOpened` already proves LWCToolkit built a native peer through the
WindowServer; `Container.printAll(Graphics)` into a `BufferedImage` renders the
live hierarchy through the same paint path as the screen and yields the exact
artifact the rig is trying to extract with `screencapture` — with **zero** TCC
involvement, deterministically, identically on all four platforms.

This is not speculative infrastructure. The project already has it:
`test/jls/ui/EditorGestureSupport.java:92` realizes the real editor frame,
`test/jls/ui/RenderAssert.java` and `RenderBoundsTest` assert over rendered
`BufferedImage`s (#162 layer 3), and `GuiConstructionObservationTest` drives the
live editor and observes exported pixels. A `@Tag("display")`
`AppBootProbeTest` — invoke the real `jls.JLS` entry point, wait for a realized
top-level window, `printAll` it, assert unique colours > 1, write the PNG under
`target/` — is roughly one file of ~150 lines.

And here is the part that makes the whole task collapse: **#265 Stage 5 already
plans to arm `display-tests` on macOS with `-Djls.test.headless=false`, riding
the `Build (macOS, JDK 25)` check that Stage 3 is already promoting.** Once
Stage 5 lands, that boot test gates macOS merges for free — no second lane, no
second 20-run record, no second branch-protection registration, no TCC, no
degraded mode, no `sqlite3` poke at a SIP-protected database. #673's unique
contribution over Stage 5 shrinks to "the compositor composited it", which the
degraded rig does not prove either.

The cost side is not small: `scripts/{macos,x11,wayland}-rig*.sh` plus
`windows-rig*.ps1` total **2,504 lines** of bespoke bash/PowerShell, in three
languages, reimplementing one algorithm (control frame → window map → non-blank)
against four native toolchains (`screencapture`/`sips`/`osascript`, `grim`/`swaymsg`,
xdotool-class X11 tools, `System.Drawing`/`Get-Process`), each needing its own
stub-toolchain self-test because none of it is reachable by the normal suite.
That pulls against the project's stated ethos elsewhere — self-contained jar,
built-ins only, "X11 is deliberately not part of this project's tooling."

**The better seam is not per-OS, it is per-ownership of the display session.**
Linux, where JLS *provisions its own* Xvfb/sway, can keep a screen-level rig
honestly — it also proves something macOS cannot: the `WLToolkit` auto-selection
in the README's Wayland row. macOS and Windows, where a hosted image's policy
owns the session, should use the portable in-JVM probe. Cutting there deletes
half the rig code and all of the TCC surface.

## Two smaller reframings with leverage across #265 and #111

**Required checks belong in the repo, not a console.** `ci.yml` has no `needs:`
anywhere; branch protection is the only wiring. A single `ci-complete` aggregator
job that `needs:` the lanes it gates, registered once as *the* required check,
would make every future promotion a reviewable, revertible one-line PR. That one
change dissolves invariant 3 (byte-stable names stop mattering), Open Question 3
(the console action), and AC 2's "registration confirmed in a comment" — across
all nine stages of #265 and all of #111, not just here.

**The 20-run record should be computed, not transcribed.** `ci.yml` already
hand-carries run IDs in at least four places (L331, L340, L482, L919). A ~30-line
`scripts/promotion-record.sh` over `gh run list --json` would make every stage's
record exact, re-runnable and auditable. While there: "20 consecutive runs" is the
wrong shape for a macOS lane whose real risk is the monthly runner-image bump.
"20 runs spanning ≥14 days across ≥2 runner-image versions" measures the hazard
that actually exists.

## Disregarding the stated acceptance criteria

Explicitly: I would not execute AC 1, 2 or 4. AC 4's re-plan trigger has already
fired; AC 1 would accrue a record for a proof that measures Apple's TCC posture
more than JLS's boot health; AC 2 cannot be satisfied simultaneously with AC 3
under the current plumbing.

Recommended replacement, in order:
1. Add the exit-2 classify step to all four GUI-boot lanes — required before any
   of them is a gate, and it retroactively repairs the two promoted Linux lanes.
2. Land `AppBootProbeTest` (`@Tag("display")`, in-JVM realize + `printAll` +
   non-blank) so the boot proof is portable and permission-free.
3. Let #265 Stage 5 carry the macOS boot gate on `Build (macOS, JDK 25)`; re-file
   #673 as a REPLAN on #265 recording that Stage 9's screen-level promotion is
   superseded.
4. Keep `macos-gui` permanently advisory as a nightly canary for the compositor
   path — no branch protection, no record ritual.
5. If the fleet still wants a screen-level macOS gate afterwards, land the
   `ci-complete` aggregator first so the promotion is a PR rather than a click.
