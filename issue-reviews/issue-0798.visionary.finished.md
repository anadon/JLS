# Issue #798: TASK-C586-3: capture determinism gets a measured tolerance, and every screenshot consumer draws from the emitted set instead of keeping private copies
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

Strip the wording and #798 asks for two things: (a) a green capture run should
mean something — not "a file was written" but "the right frame was written";
and (b) there should be exactly one screenshot set in the project, not one per
consumer. Both are right, and both are worth having. The mechanism proposed
for (a) — a measured pixel tolerance in the idiom of #101's `PIXEL_DIFF_MIN` —
is the wrong instrument, and (b) is already ~80% owned by the sibling task
#797. The task should survive; its acceptance criteria should not.

## The project has already written down the opposite discipline

`test/jls/ui/package-info.java`, describing #91's Layer 3, says the rendering
tier makes "semantic checks, **never brittle pixel goldens**." `RenderAssert`
asserts *an element paints, and paints only inside its index bounds plus the
culling margin* — a structural property, not a raster comparison. #798 proposes
a pixel golden with a fuzz factor, on the one surface (a software-rendered
compositor screenshot) where rasters are least stable. That is not a small
inconsistency; it is the recorded UI-verification stance of the project running
in reverse.

The second precedent is in README lines 53-60. Faced with artifacts that are
*not* byte-reproducible (the native installers, whose tooling embeds wall-clock
state), the project did not invent a fuzzy-equality check. It said so plainly
and moved integrity onto **provenance**: "installer integrity rests on the
attestation," while the jar and `bom.json` — the things that genuinely are
reproducible — carry byte-exact checks. Compositor screenshots are the installer
case, not the jar case: pixman software rendering, font package versions, JBR
pins and runner-image drift all move the bytes. The project has already decided
what to do with an artifact of that kind, once, deliberately. #798 re-litigates
it in a weaker form.

## Two defects in the criteria as written

**AC-4 asks for a comparison between numbers that are not of the same kind.**
#101's `PIXEL_DIFF_MIN` is a *floor* on `AE(desktop-before, desktop-after)` —
"the window drew at least this many pixels onto an empty desktop"
(`scripts/wayland-rig.sh` L340-L362; the formalization is #411 §7.10). It is a
liveness assertion against a blank-desktop baseline. #798's tolerance is a
*ceiling* on `AE(capture_A, capture_B)` — "two renders of the same input differ
by at most this much." Opposite directions, different reference images,
different failure meanings. They share the ImageMagick AE metric and nothing
else. Stating their derivations "in the same terms so the two numbers are
comparable" does not make them comparable; it invites a later reader to conclude
something false — e.g. that a tolerance above the floor is coherent, when in
fact a system can satisfy both, either, or neither independently.

**AC-2 cannot measure the variance AC-1 claims to derive.** "Repeating a capture
run twice on the same commit" holds fixed every real source of drift: same
runner image, same font packages, same JBR, same ImageMagick, same sway build.
It measures scheduler jitter and animation timing — the smallest term. #411 §11
already names the real terms ("measured on one runner image, one font set, one
theme; a runner image update can move it") and #411 H2 names the falsifier
("green-lane AE varies by more than 10x across runs"). A tolerance calibrated
from twice-on-one-commit is precisely the guess AC-1 forbids, wearing a
measurement's clothes. Honest calibration needs cross-runner, cross-refresh
variance — weeks of nightly data — which does not fit `band_mw: 0.5-1`.

## Alternative A (primary): tier the manifest by render path, and most of the
## tolerance problem disappears

The manifest (#796) currently treats "screenshot" as one thing. It is three,
with wildly different determinism:

- **Tier A — circuit content.** What #551's gallery needs, and most of what
  #545's README needs, is a picture of a *circuit*, not of window chrome. JLS
  already emits those headlessly and deterministically via `-i out.svg` /
  `-i out.png` (`jls.edit.CircuitRenderer`, exercised by `CliImageExportTest`).
  #551 AC-1 says so explicitly: "generated with the existing `-i out.svg` export
  path." No compositor, no JBR, no download fail-open, no tolerance — the
  correct gate is **byte equality**, in the idiom `DeterministicSaveTest`
  already establishes for saves. That is strictly stronger than any tolerance
  #798 could derive, and it is roughly a day of work.
- **Tier B — panes, dialogs, menus.** Paint the component into a
  `BufferedImage` with no window, exactly as `jls.ui.RenderAssert` /
  `RenderBoundsTest` already do headlessly. Deterministic modulo the JVM's own
  font rasterization, which is pinned by the build's JDK. Again: byte equality,
  or a structural assertion, not a fuzz factor. This is #91 Layer 3, already
  started, already scoped, already owned.
- **Tier C — whole-desktop screenshots.** Real window decorations, real
  compositor. This is the *only* tier that needs the sway rig, inherits #101's
  fail-open, and needs a tolerance at all. For a README and a manual, Tier C's
  necessary population may well be **zero** — a cropped app image is the better
  picture anyway. Store listings (#579/#854) plausibly want one or two.

Reframed this way, #798's question stops being "what tolerance does the capture
pipeline need?" and becomes "which shots genuinely require Tier C, and is that
set empty?" If it is empty, the whole tolerance problem evaporates and the
capture pipeline stops depending on a lane that fail-opens on a CDN outage. If
it is two shots, a tolerance for two shots is a much smaller, much more honest
measurement — and it should be derived from the *nightly* record #101 already
accrues, not from two runs on one commit.

## Alternative B (fallback, if Tier C is non-empty): provenance, not pixels

For Tier C, do what the project did for installers. Each emitted image carries a
sidecar (or PNG `tEXt` chunk) recording manifest entry id, commit sha, rig
identity (JBR pin, runner image, compositor, font package set) and the run id.
The shipped check becomes: *every image in the docs was emitted by a capture run
at this commit, by this rig* — a provenance assertion that is exact, cheap, and
cannot flake. Freshness, which is what #586's outcome actually promises ("no
image can outlive the UI it claims to show"), is a *provenance* property. It was
never a pixel property; a byte-identical screenshot of a UI that has since
changed is stale, and a legitimately-changed UI produces a large diff that a
tolerance must be widened to accept. The tolerance is measuring the wrong axis
for the stated outcome.

Non-blankness — the one thing a pixel metric genuinely buys — is already #101 P2
/ #411's job, and #797 AC-4 already requires this task family to name #411 where
it leans on that gate. #798 does not need to own a second pixel number.

## AC-3 mostly duplicates #797, and annexes part of #854

#797 AC-1 already fails the build on "any image referenced by README, hosted
manual or in-jar help that lacks a manifest entry." That is AC-3's inbound half,
in the same feature, ordered immediately before this task. The genuinely new
part is the *store listings* — but those live in external manifests
(Flathub/winget/Homebrew), and #854 (TASK-C579-4) already states its shop-window
content comes from the shared set. Two issues asserting the same edge from
opposite ends is how a check ends up implemented twice and enforced nowhere.
Given how carefully this repo maintains boundary notes and dedup passes, this
one should be resolved rather than left symmetric: the in-repo check belongs to
#797, and the store-listing check belongs to #854's release flow.

## What I would keep

The one-source property is the durable idea and it should be stated more
strongly than the issue does: **there is a single emitted image set, and every
surface — README, hosted manual, in-jar help, store listings, gallery — resolves
into it by manifest id, never by path.** Ids over paths is what makes a
re-render invisible to consumers and a deleted shot loud. That is worth more
than the tolerance and is not currently anywhere in the feature's criteria.

## Restated acceptance criteria (I am disregarding AC-1, AC-2 and AC-4 as written)

1. Every manifest entry declares its render tier (A: `-i` export; B: headless
   component render; C: compositor screenshot), and the manifest schema (#796)
   carries that field.
2. Tier A and Tier B captures are gated on **byte equality** across two runs on
   the same commit and same JDK — a zero tolerance, in the idiom of
   `DeterministicSaveTest`. A nondeterministic Tier A/B capture is a bug to fix,
   not a number to widen.
3. The Tier C population is enumerated and justified shot by shot; if it is
   empty, the capture pipeline declares no dependency on `scripts/wayland-rig.sh`
   and inherits none of #101's fail-open.
4. If Tier C is non-empty: each shot carries emission provenance (manifest id,
   commit, rig identity, run id), the shipped check is provenance equality, and
   any pixel tolerance is derived from #101's accumulated nightly AE record —
   explicitly documented as a *ceiling on inter-run difference*, distinct in
   kind from `PIXEL_DIFF_MIN`'s *floor against an empty desktop*, with that
   distinction stated rather than a comparability claim.
5. Consumers reference images by manifest id, not path. The inbound check stays
   in #797; the store-listing check moves to #854 by explicit hand-off comment.
