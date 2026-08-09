# Issue #91: Automated UI test harness (P5 residual): retire display-suite retry masking and produce the 20-run zero-flake record
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of its P1–P6 provenance, #91 wants one thing: **the display-tagged suite
should be believable enough to block a merge.** Everything else — the retry
counter, the number 20, the STATUS table — is instrumentation chosen in service
of that. The chain-integrity comment (2026-08-08) got this right by attaching it
to #317: this is a statement about *what the required gate is allowed to
contain*.

Judged against that goal, the issue's method is the weakest part of it. I am
explicitly disregarding two of the §14 completion criteria — deleting
`rerunFailingTestsCount` outright, and posting a 20-run table as an issue
comment — and proposing different deliverables for the same end. Reasons below,
strongest first.

## 1. Deleting the retry throws away the only flake *detector* the build has

The issue treats `rerunFailingTestsCount=2` (`pom.xml:293`) as masking. It is
masking only because nothing reads its output. Surefire already writes
`<flakyFailure>` into `target/surefire-reports/*.xml` when a test fails and then
passes — that XML element *is* the distinction between "flaky" and "broken",
recorded for free on every run, with both stack traces.

Delete the retry and a flaking display test produces a red build with one
stack trace and no way to tell a genuine regression from an Xvfb hiccup — which
is precisely the judgement call the 2026-07-17 quarantine rule ("one flake →
quarantine + file an issue") requires a human to make. Keep the retry *and fail
the build whenever a `flakyFailure` appears*, and you get both: the build is
fail-closed exactly as the decision demands, and the failure message can say
"this test failed then passed — it is flaky, quarantine it per the standing
rule." That is a strictly better instrument than either the status quo or the
proposed deletion.

Mechanically this is small: an `exec-maven-plugin` or `maven-enforcer` step (or
ten lines of shell in the Build job) that greps the surefire XML for
`flakyFailure`/`rerunFailure` and exits non-zero. No production code, no test
changes — the same footprint §7 already claims.

The reframe: **the retry is a measurement instrument; masking is what happens
when nobody acts on its output.** #91 conflates the two, and the P-1 criterion
("`rerunFailingTestsCount` absent from `pom.xml`") hard-codes the conflation.

## 2. The 20-run campaign is the wrong experiment, and this repo already knows it

The repo has a precedent for exactly this promotion, and #91 does not follow it.
`ci.yml:330-345` and `:481-500` record the #101 gui-wayland/gui-x11 promotions:
20/20 green **harvested retrospectively from real CI history** (run IDs listed,
push and PR legs separated, the one non-green event named and adjudicated as
infrastructure), written as a comment **next to the lane it justifies**.

#91 §8 instead proposes running the suite 20 consecutive times via "a trivial
shell loop or a temporary CI workflow-dispatch matrix", and §7.6 puts the table
in an issue comment. That is worse on all three axes:

- **Sampling.** The pom comment blames "loaded CI runners". A back-to-back local
  batch on one machine samples the wrong distribution. §11 says this out loud
  and then §8 prescribes it anyway.
- **Custody.** An issue-comment table is unlinked from `pom.xml:293`. When
  someone re-adds a retry in eighteen months, nothing in the tree contradicts
  them. The #101 record sits in the file it justifies, where a diff surfaces it.
- **Renewability.** A harvest is a script anyone can re-run against
  `gh run list`; a campaign is a one-shot ritual whose evidence expires quietly.

Follow the #101 pattern: harvest from history, record in `ci.yml` beside the
display lane (or `test/jls/ui/package-info.java`, which #162 already claims as
its home — pick one, not both).

## 3. Twenty is a number borrowed across an order-of-magnitude difference

#101's bar was set for a *boot smoke rig*: one binary observation per run.
Twenty greens there is a defensible sanity threshold. The display suite is
**93 `@Test`/`@ParameterizedTest` methods across 25 files** (counted at HEAD).
Twenty green runs bounds the per-run failure probability at roughly 14% (95%
one-sided) — a suite that reddens once every seven runs passes this bar with
better-than-even odds. Reusing "20" here is cargo-culting a threshold across a
two-order-of-magnitude change in surface area.

Worse, §P-3's rule — quarantine and *restart the counter* — makes the metric
non-monotone in the thing it measures: every quarantine both removes the flakiest
test and erases the history that would have exposed the next one. §11 spots this
("a quarantine that removes the flakiest tests can make the record look better")
and mitigates with "link the filed issues", which does not touch the incentive.

A rate with a stated interval, maintained continuously, is the right instrument
for a 93-test suite. A streak counter is the right instrument for a smoke rig.

## 4. A record of 20 runs of *nothing* satisfies P-2 as written

Every display test opens with
`Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), …)`
(all 25 files; e.g. `test/jls/ui/EditorGestureTest.java:44`). `ci.yml:79-85`
falls back to plain `mvn -B verify` when `xvfb-run` is absent, and the adjacent
comment explicitly blesses that path ("Best-effort so a transient apt failure
doesn't block the build"). In that state the `display-tests` execution runs, skips
all 93, and reports **zero failures and zero reruns** — literally P-2's stated
criterion.

#162 owns arming the lane, and the chain-integrity comment already ordered
#162 → #91. But §9's data-collection spec still lists only "tests
run/failures/errors/skipped … plus the rerun count" without a floor. Whatever
form the record takes, it must carry an **expected-executed-count assertion**
(93 today, or `skipped == 0`) or it is unfalsifiable. This belongs in §7.6, §9,
and the DoD regardless of which reframing is adopted.

## 5. The alternative nobody has costed: shrink the suite instead of certifying it

`test/jls/ui/package-info.java` states the harness doctrine — "built in layers
with **the cheapest layer preferred per assertion**". Twenty-five display-tagged
files is prima facie evidence that doctrine has not been audited since Layer 2
started growing, and #91 proposes to spend its entire budget certifying the
result rather than questioning it.

Two concrete openings:

- **Most of these tests do not need a window server; they need a non-headless
  `GraphicsEnvironment`.** The 2026-07-17 comment records that Layer 2 abandoned
  `java.awt.Robot` for synthetic `MouseEvent` dispatch precisely because the
  handlers read `event.getX()/getY()`, not the live pointer. Files like
  `ComponentIdentityTest`, `EditActionMatrixTest`, `MenuMnemonicAndAccessibleNameTest`,
  `PaletteButtonAccessibilityTest` and `MenuBarSpecTest` inspect component trees
  and declared tables — they are display-tagged because Swing realization and
  font metrics demand a graphics environment, not because any pixel or X
  round-trip is under test. Each one demoted to Layer 1 removes 1/93rd of the
  flake surface *permanently*, which is worth more than any number of green runs.
- **The one named flake source is a single mechanism in a single method.**
  `EditorGestureSupport.waitForMenuItem` (lines 601-635) documents it exactly:
  a synthetic BUTTON3 press "intermittently fails to materialize an *enumerable
  popup window*". Note `hasVisiblePopup` already walks the container tree, so
  lightweight popups are handled — meaning the flake is specifically about
  *heavyweight* popup realization under a WM-less X server. Before running a
  20-run campaign, run **one** instrumented experiment: log
  `popup.isLightWeightPopupEnabled()` and the popup's `Window` ancestry on a
  flaking run. If the popup is going heavyweight (it will, whenever it would
  overflow the test frame), sizing the fixture frame so popups fit — or
  asserting against the `JPopupMenu` model rather than `Window.getWindows()` —
  deletes the flake mechanism rather than measuring it. That is a one-afternoon
  hypothesis test that could make this entire issue moot.

This is the same strategy #316 (FEAT-008 RESIDUAL) pursues for `jls.edit`. The
chain-integrity comment carefully assigned #316 and #91 to different features —
correct on the letter, but it means the project is simultaneously funding
"make the display lane trustworthy" and "need fewer display tests" without
either issue costing the other. Shrinking is the cheaper path to #317's end
state and it compounds; certifying does not.

## 6. Three issues, three 20-run records, three different bars

The 2026-08-04 deduplication comment does careful work keeping #91's zero-rerun
bar distinct from #162's at-most-one-failure bar and #406's one-release grace.
The adjudication is correct and the distinctions are real. The visionary read is
that **the distinctions are the symptom.** Three issues each need to know "is
this display lane flaky, and by how much", and each answers it with a bespoke
one-shot campaign against a bespoke threshold, landing in a bespoke artifact.
The 2026-08-04 comment's own "efficiency note" — one campaign can satisfy both
bars — is the mechanism trying to surface.

What the project actually needs is **one flake-accounting mechanism applied to
every display lane on every platform**: fail-on-`flakyFailure` in the build
(item 1), plus a `scripts/flake-report.sh` that digests surefire XML or `gh run`
history into a rate per lane. Turn that on and #91's residual, #162's stability
record, and #406's Open Question 1 all reduce to "watch the number; promote when
it is zero" — no campaigns, no streak counters, no issue-comment tables to
re-derive per platform.

## Recommended reshape (replaces §8 and §14)

1. Keep `rerunFailingTestsCount=2`; add a build step that fails on any
   `flakyFailure`/`rerunFailure` in `target/surefire-reports`. Amend the pom
   comment and record the amendment here — §P-1 already permits this branch, and
   this is the justified reversal it anticipates.
2. Add an executed-count floor to the display execution so a skipped-everything
   run is red, not green (complements #162's arming; cheap and independent).
3. Run the one-shot heavyweight-popup experiment (§5) before any campaign.
4. Audit the 25 display-tagged files against the package-info cheapest-layer
   doctrine; file demotions as tasks under #316's feature.
5. Harvest the stability record from real CI history in the #101 style and
   record it in `ci.yml` next to the display lane — not as an issue comment.

Items 1 and 2 alone deliver the property #91 exists for, are provable in one PR,
and — unlike the 20-run record — keep holding after the PR merges.
