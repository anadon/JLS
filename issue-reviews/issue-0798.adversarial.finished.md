# Issue #798: TASK-C586-3: capture determinism gets a measured tolerance, and every screenshot consumer draws from the emitted set instead of keeping private copies
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the issue

TASK-C586-3, third of three tasks under FEAT-C35-3 (#586), closes out the
screenshot-capture-pipeline feature: (1) derive and enforce a measured pixel
tolerance for repeatability of captures from the same manifest entry, in the
idiom #101 uses for `PIXEL_DIFF_MIN`, and (2) make every named screenshot
consumer (#545, #551, CAP-34 store listings) draw from the emitted capture set
rather than keeping private copies. It orders after TASK-C586-2 (#797), which
itself orders after TASK-C586-1 (#796) — the manifest-and-regeneration-command
task.

## Findings

### 1. [HIGH] AC-4's "comparable numbers" claim is a category error

AC-4: *"The tolerance's derivation is stated in the same terms as #101's
`PIXEL_DIFF_MIN` note, so the two numbers are comparable rather than
independently invented."* Per #101/#411 (`ci.yml:386,513`,
`scripts/wayland-rig.sh` L60), `PIXEL_DIFF_MIN` is the ImageMagick AE (pixel
count) between a screenshot of the **empty desktop** and a screenshot **after
JLS has mapped a window** — it answers "did anything render at all" (a
non-blank-frame gate), calibrated to ~10% of that observed AE (#411 §7.10).
#798's tolerance instead measures AE between **two screenshots of the same
UI state on two different runs** — a repeatability/rendering-noise metric.
These are not the same quantity: one is signal-vs-blank, the other is
signal-vs-signal-noise. A "10% of AE" derivation on two conceptually
different baselines does not make the resulting numbers "comparable" in any
useful sense — at best they share a *derivation idiom* (measure, then take a
fraction of the observed baseline), not a comparable scale.
**Recommendation:** reword AC-4 to require the same *documentation
discipline* (raw measurement + derivation + date recorded beside the value,
per #101's note) rather than claiming the two numbers themselves are
comparable — the current wording invites a "well, both are X% of an AE
measurement" false equivalence in review.

### 2. [HIGH] AC-2's negative control is gameable — no lower bound on "deliberately altered"

AC-2: *"a deliberately altered UI element produces a diff outside it"*. No
size, area, or realism constraint is given for the alteration. An
implementation satisfies this trivially by picking an oversized change (e.g.
deleting an entire panel or toolbar) that clears any tolerance by a wide
margin, while never demonstrating that a realistic regression — a
mis-colored icon, a shifted label, a font substitution — is actually caught.
As written, the acceptance test can pass while the tolerance is simultaneously
too loose to catch the regressions the feature exists to catch (#586's stated
goal: "no image can outlive the UI it claims to show").
**Recommendation:** specify the alteration class the negative control must
use (e.g. a single-widget color or geometry change of a stated minimum pixel
footprint), or require the negative-control diff to be reported alongside the
tolerance so reviewers can judge headroom, the way #411 §9 requires the
AE/threshold ratio to be recorded as "headroom."

### 3. [MEDIUM] Determinism is assumed to be a noise-floor problem; live/animated UI state isn't addressed

`ARCHITECTURE.md` (Threading model) notes the interactive simulator has a
rate-limited **clock display** updated from the sim thread, and
`scripts/x11-rig.sh`/`wayland-rig.sh` run on the pixman **software renderer**
under headless sway/Xvfb. If any manifest entry captures a pane showing a
running clock, a blinking cursor, or focus-ring animation timing, repeated
captures of "the same input" will differ by construction — not because of
rendering noise a pixel tolerance can absorb, but because the UI state itself
is non-deterministic between captures. AC-1/AC-2 treat determinism purely as
"pick a tolerance number"; nothing in the issue requires manifest entries to
freeze or exclude such elements (e.g. pause the clock, disable cursor blink)
before capture. This risks either a flaky gate (tolerance too tight for
legitimately time-varying widgets) or a tolerance loosened until it hides
real regressions (too loose to be a meaningful contract).
**Recommendation:** add an AC requiring manifest entries to either avoid
live/animated widgets or document how such state is frozen/masked before
comparison, rather than folding that entirely into "the number."

### 4. [MEDIUM] Boundary with #797's consumer-check is unstated — likely duplicate or ambiguous scope

#797 (TASK-C586-2) AC-1 already builds: *"A check names any image referenced
by README, hosted manual or in-jar help that lacks a manifest entry, and
fails the build."* #798's AC-3 — *"Every named consumer — #545, #551, the
CAP-34 store listings — references images from the emitted set; a check
fails when a consumer references an image outside it"* — reads as the same
check, restated for a named subset of consumers, with no boundary statement
explaining whether it's the same mechanism narrowed, a second parallel check,
or a superset that also covers CAP-34 (a consumer #797 never mentions).
Contrast with #586 itself, which is careful to state boundaries ("reuses
#101's rig, does not re-own it"); #798 gives AC-3 no such framing.
**Recommendation:** state explicitly whether AC-3 extends #797's existing
check to CAP-34 artifacts (most likely intent) or is a distinct mechanism —
as written, an implementer could build a second, redundant ratchet test.

### 5. [MEDIUM] "The CAP-34 store listings" overstates the actual consumer set

Of CAP-34's (#518) children fetched for this review, only #854 (TASK-C579-4,
the Flathub listing) commits to sourcing screenshots from the shared set
("at least three screenshots sourced from the CAP-27 (#511) set"). The
winget task (#580) and Homebrew cask task (#581) titles and the CAP-34
tracking issue itself give no indication either channel carries screenshots
at all (winget/Homebrew store listings are typically metadata-only, unlike
Flathub's AppStream metainfo with embedded screenshot URLs). "The CAP-34
store listings" (plural, capstone-wide) is therefore broader than the one
issue actually shown to need this — AC-3 as worded could be read to demand a
check against listings that never carry images.
**Recommendation:** name #854 specifically, or verify winget/Homebrew
actually gain screenshot fields before citing them as consumers.

### 6. [MEDIUM] Silent inheritance of #101's known weaknesses is unaddressed here, unlike its sibling task

#586 (the parent feature) explicitly flags: *"a capture run must not inherit
either weakness silently"* — `PIXEL_DIFF_MIN` still `"0"` (record-only,
confirmed live at `ci.yml:386` and `:513`) and the `gui-wayland` lane's
CDN-download fail-open. #797 (TASK-C586-2) carries this forward as its own
AC-4: *"The check does not inherit #101's known weaknesses silently...
names #411."* #798 has no equivalent acceptance criterion. Since AC-1 claims
enforcement ("a capture outside it fails"), and captures run on #101's rig,
a silent fail-open on JBR-download failure would mean AC-1's "enforced" gate
simply doesn't execute on some fraction of runs — without anything in #798
requiring that gap to be surfaced, the same way #797 requires it.
**Recommendation:** add an AC mirroring #797 AC-4, naming #411 as the
tracked fix for the underlying fail-open/record-only gaps.

### 7. [LOW] Solid: dependency ordering is correct

`ordering_after: [TASK-C586-2]` is the right dependency — a
determinism-tolerance-and-consumer check needs the manifest and base ratchet
infrastructure #796/#797 build first. No issue here.

### 8. [LOW] Solid: the underlying goal is well-motivated and consistent with #586/#101

The push toward build-product screenshots with no private, rotting copies is
a legitimate, clearly-scoped documentation-hygiene goal, and correctly reuses
#101's rig rather than proposing new capture infrastructure. `band_mw: 0.5-1`
is a plausible estimate for "add a threshold + extend a consumer check" given
#796/#797 already exist as prerequisites.

## Verdict rationale

The core intent is sound and properly sequenced against its sibling tasks,
but the acceptance criteria have real, fixable gaps: AC-4 conflates two
different pixel metrics as "comparable," AC-2's negative control has no
guard against being satisfied by an unrepresentative oversized change, the
consumer-check boundary with #797 is unstated, "CAP-34 store listings" names
a broader set than is evidenced, and the parent feature's explicit warning
against silently inheriting #101's fail-open/record-only weaknesses is not
carried into this task's own acceptance criteria the way its sibling #797
does. None of these require re-scoping the task, but all four ACs need
tightened wording before implementation starts — hence sound-with-concerns
rather than sound.
