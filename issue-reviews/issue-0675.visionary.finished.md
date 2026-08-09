# Issue #675: TASK-C101-1: the Wayland rig's first light is published — a green run's artifacts and every startup exception verbatim, as an in-tree findings document
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and the claim underneath is: *a gate is only
legitimate if the thing it gates is described somewhere durable.* That claim is
squarely on JLS's trajectory. The repo already runs on it — `docs/mutation-testing-trial-2026-07.md`,
`docs/keyboard-a11y-verification.md`, `docs/flatlaf-evaluation-2026-07.md`,
`docs/dmg-reproducibility.md`, `docs/reproducibility.md` are all dated in-tree
findings documents that exist so a later reader can tell a measurement from a
guess. README's Wayland section (the supported-desktop matrix) is a promise to
users; nothing in the tree currently shows what keeps that promise honest. So
the *end* is right and the gap is real.

The *mechanism* the issue picks — one human, one green run, transcribed by hand
into prose — pulls against the project's other and stronger arc: in this repo,
durable claims are machine-produced and machine-defended. `scripts/wayland-rig-selftest.sh`
guards the exit contract on every event with no JBR and no network. The
`*RatchetTest` family exists (#411 O8 lists seven of them) precisely so that a
placeholder cannot silently return. `pom.xml` carries coverage floors.
`docs/reproducibility.md` is backed by a rebuild job. A findings document with
no producer and no ratchet is the one artifact class this project does not
otherwise tolerate — and it is the class that rots fastest, because the JBR pin,
the runner image, and the sway version underneath it will all move.

## The reframing I would apply

**1. The rig should emit the findings; the document should be the checked-in
snapshot of what the rig emitted.**

AC1 asks the document to name the run id, the JBR pin, the sway version, and the
commit under test. Measured: `scripts/wayland-rig.sh` emits none of those. Its
artifact list (L10-L20) is `sway.log`, `control-*.log`, `control-verdict.txt`,
`control.png`, `jls-*.log`, `tree.json`, `desktop-{before,after}.png`,
`pixel-diff.txt` — no environment manifest, no version capture. `grep` for
`sway --version`, `get_version`, `GITHUB_RUN_ID` across the rig and `ci.yml`
returns nothing. So AC1 as written is a human retyping four facts the apparatus
declines to record, into a file nothing regenerates.

Cheaper and permanent: add ~30 lines to `wayland-rig.sh` that write
`$ARTIFACTS_DIR/first-light.md` (and/or `env.json`) with `sway --version`,
`"$JBR_HOME/bin/java" -version`, `$GITHUB_RUN_ID`, `$GITHUB_SHA`, the
control verdict, the AE, and the full stderr census — then commit *that output*
to `docs/wayland-first-light-2026-08.md`. Every future run regenerates it for
free, and a JBR bump produces a new dated snapshot instead of quietly falsifying
the old one. This is the same move `scripts/measure-dmg-repro.sh` already makes
for `docs/dmg-reproducibility.md`.

**2. n=1 cannot answer the question AC3 is being asked for.**

AC3 records "the observed AE" from one run so #411 can set
`PIXEL_DIFF_MIN = floor(0.10 × a)`. But #411's own H2 is falsified by
*variance*: "a green-lane AE that varies by more than 10x across runs, which
would make any single threshold either vacuous or flaky." A single sample cannot
refute or support that hypothesis. Meanwhile the nightly cron has been accruing
`gui-wayland` runs since PR #266 — dozens of independent AE measurements are
already sitting in the artifact store, and `wayland-gui-boot`'s upload step
(ci.yml L1145 sets `retention-days: 30` elsewhere; this upload takes the 90-day
default) means most of them are still fetchable.

Reframe AC3: report the AE **distribution over the nightly record** — n, min,
max, median, and the max/min ratio — not one number. That single change turns a
transcription chore into the measurement that decides whether #411 ships a
scalar threshold or the floor-plus-band its §10 already contemplates, *before*
the gate is armed rather than after it starts flaking on master. It is the
higher-value deliverable and it costs one `gh run download` loop.

**3. The exception census wants to become a ratcheted baseline, not a paragraph.**

AC2's "every exception verbatim, classified JLS-side vs upstream" is a snapshot
of a thing that should be continuously asserted. The precedent is in this repo
already: the gsettings-schemas discovery (ci.yml L398-L403) was a first-light
finding that got *encoded* — into an apt install and a comment — rather than
merely written down. The natural successor is
`scripts/wayland-known-stderr.txt`: an explicit allowlist of benign upstream
JBR/Wakefield notices, with the rig warning (or, later, failing) on anything not
on it. Then "the baseline of a good boot" is a file under version control whose
diffs are reviewable, which is exactly `HeadlessCoreRatchetTest`'s
empty-baseline discipline that #411 O8 cites. Writing the census into prose
gets the observation; writing it into an allowlist gets the observation *and*
the regression detector, for maybe twenty more lines.

## Where it collides with the rest of the plan

This must be resolved before anyone writes a line:

- **#411 asserts this work is already done, and it is wrong.** #411 §1 says
  "First-light findings are **published**: `docs/wayland-desktop-checklist.md`",
  under the heading "Do not re-do first light." Read the file: it is a
  *per-release manual spot-check on a real GPU desktop (Mutter/KWin)*, serving
  #100's threats-to-validity, whose results are recorded as comments on #100. It
  is not a record of the headless lane's boot, contains no exception census, and
  no AE. #675 is right that the finding was never published. But #411 also
  declares "#101 — **Closed by this task**", so two open issues each claim to
  discharge #101's close-out. Whoever picks up #675 should post the correction on
  #411 first; otherwise #411 lands, closes #101, and #675 is orphaned mid-flight.
- **AC3's hand-off is redundant with #411's own method.** #411 §8 already says
  "Read the AE from the most recent green run's `pixel-diff.txt` artifact." Two
  tasks reading the same artifact, with one passing the number to the other
  through prose, is a coupling with no benefit — unless #675 delivers the
  distribution (reframing 2), which #411 does not compute and genuinely needs.

## The bigger thing the document should not merely record

AC4 asks the doc to state the fail-open weakness and name #411 as its closer.
Check #411: it does **not** close the fail-open. Its O5 deliberately preserves it
("an outage must stay a skip, not a failure"), and correctly — a required check
wedged on JetBrains' uptime is worse than an unpinned one. So AC4 as written
would publish a false forward reference. #101's Open Question 1 has the real
answer: mirror the JBR tarball so the download stops being someone else's
availability problem, and *then* fail-close.

That is also where the project's own arc points. JLS is unusually serious about
supply chain for a teaching tool — build-provenance attestations, `bom.json`,
byte-reproducible jar, OpenSSF Scorecard, `SECURITY.md`'s custody reasoning for
declining a GPG key. A required GUI gate whose runtime arrives unverified from a
third-party CDN, and which passes silently when that CDN is down, is the loudest
remaining inconsistency in that story. Mirroring the pinned JBR as a release
asset of this repo (attested like every other asset) collapses three problems at
once: the pin becomes trivially verifiable, the fail-open becomes unnecessary,
and the "stale pin hides upstream fixes" caveat gets a visible, dated home. I
would have the first-light document say that explicitly and file it, rather than
point at #411 for something #411 declines to do.

## Verdict

**endorse-with-reframing.** Publish the findings — the gap is real and the
project's documentation culture demands it. But land the report as the rig's
*output* rather than a human's transcription (reframing 1), report the AE as a
distribution over the accrued nightly record rather than one run (reframing 2),
and turn the exception census into a stderr allowlist the rig consults
(reframing 3). Correct #411's false "already published" claim before starting,
and replace AC4's forward reference with the mirror-and-fail-close proposal,
since #411 does not close the fail-open and does not intend to. Executed that
way, a one-off close-out chore becomes a renewable capability that survives the
next JBR bump — which is the only version of "first light" worth committing to a
repository that will still be pinning runtimes in five years.
