# Issue #287: HiDPI-clean toolbar icons: 33 24×24 GIF bitmaps upscale blurry under JEP 263 scaling — redraw as SVG (FlatSVGIcon) through the makeElement seam
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The core technical premise is well-evidenced (real permalinks, a real seam, a
real dependency already in the tree) and the prerequisites it claims are
already merged (`blocked_by: []` checks out). The weak point is not the fix
itself but what the issue treats as its proof: the only *automated,
in-repo-checkable* acceptance criterion is "an SVG resource resolves on the
classpath," which is exactly what the existing `.gif` test already checks and
proves nothing about sharpness — while the criterion that would actually prove
the hypothesis (crisp rendering at 100–200% scaling) is pushed onto
infrastructure (PR #266's rig) that, on inspection, does not do what the issue
says it does.

## Findings, most severe first

### 1. [Medium-High] The cited PR #266 rig cannot produce the "100%/200%" screenshot evidence the issue's own Completion Criteria require

The issue states: *"On-screen blur confirmation is delegated to the Windows
display rig (PR #266), which screenshots the real window"* (§2), lists *"PR
#266 Windows/macOS display rigs for on-screen proof"* as Materials (§6), and
its Definition of Done requires: *"Display-rig screenshots at 100%/200%
recorded in the PR (P2)."*

I fetched PR #266 (merged, `anadon/JLS#266`, "ci: multi-platform CI parity +
promote/rename GUI boot lanes to hard gates"). It adds `scripts/windows-rig.ps1`
and `scripts/macos-rig.sh` — boot-smoke rigs whose entire pixel contract is:
launch the real jar, assert the top-level window **maps**, take **one**
screenshot, and assert it is non-blank (`unique-color count > 1`). There is no
OS display-scale control anywhere in that PR — no code that sets the runner to
125%/150%/200% scaling, captures a scale matrix, or does any before/after
sharpness comparison. The macOS lane is explicitly documented as running in
"DEGRADED MODE" on hosted runners much of the time (TCC/Screen-Recording
permission is frequently withheld, in which case pixel proof is skipped
entirely and only window-mapping is asserted).

So the "display-rig screenshots at 100%/200%" DoD item names a capability that
does not exist in the cited PR and is not scoped as work in *this* issue
either (§8's checklist is silent on "add scale control to the rig"). As
written, this criterion can only be satisfied by inventing new rig
capability outside this issue's stated method, by hand-capturing screenshots
locally and attaching them to the PR (undocumented, unrepeatable), or by
quietly not doing it and shipping anyway. Recommendation: either (a) scope the
scale-matrix screenshot capability explicitly as part of this issue's Method
(or as a named, filed follow-up this issue blocks on), or (b) drop the
100%/200% claim from the Completion Criteria and replace it with something
this repo's actual CI can produce today (e.g., the existing single
boot-screenshot at native scale, explicitly labeled as partial evidence).

### 2. [Medium] The automated acceptance surface is gameable: it only proves resource wiring, never sharpness

P1 and the first DoD bullet — *"`PaletteContractTest` extension green; grep
shows no runtime `.gif` references remain"* — are the only criteria checkable
by `mvn verify`. I read the existing test
(`test/jls/edit/PaletteContractTest.java:79-94`,
`everyIconResourceExistsOnTheClasspath`): it already does nothing more than
`SimpleEditor.class.getResource("images/" + name + ".gif") != null`. An
extended version that swaps `.gif` for `.svg` proves an SVG file exists at the
right path and nothing about `FlatSVGIcon` actually rasterizing sharply — a
malformed, blank, or accidentally-rasterized-at-24px-fixed SVG would pass this
gate identically to a correct vector one. Combined with Finding 1, this means
the entire issue can be marked "done" (`mvn verify` green, PaletteContractTest
green, `.gif` grep clean) while the stated research question — "does the
toolbar render crisp icons at 100–200% scaling" — remains unanswered in any
repo-checkable artifact. Recommendation: add at least one automated,
non-manual check that exercises `FlatSVGIcon`'s actual raster output (e.g., a
headless paint-to-`BufferedImage` test at 2× transform asserting an edge/PSNR
sharpness delta against the GIF baseline) — the issue's own §9 gestures at
this ("a paint-to-image test can assert the SVG path is exercised") but never
puts it in the Method checklist or the DoD, so it's optional currently, not
required.

### 3. [Medium] The "33 GIFs" inventory is imprecise: it both overcounts (dead assets) and undercounts (already-missing icons) what the palette actually renders

Observation 2 says *"`ls src/jls/edit/images/*.gif | wc -l` → 33"* and the
Method says *"Author one SVG per existing GIF (33 glyphs, 24×24 viewBox), same
base names."* I cross-referenced the 33 filenames against every icon name
actually reachable through the code:

- `src/jls/edit/Palette.java` supplies icon names for every `PaletteEntry`
  (verified by grep of every `entry(...)` call), and
  `SimpleEditor.java:2332` is the only other call site (`getImage("down")`,
  the hand-coded Import button).
- Three files on disk are referenced by **nothing**: `equal.gif`,
  `notequal.gif`, and `up.gif` in `src/jls/edit/images/`. No `PaletteEntry`
  names them and no `getImage(...)` call site does either (a same-named
  `up.gif`/`down.gif` pair does exist and *is* used, but by the unrelated
  `KeyPad` class reading from `src/jls/images/`, a different directory —
  `src/jls/KeyPad.java:51-55` — not the 33 counted here).
- Conversely, three icon names the palette *does* reference —
  `ShiftRegister`/`shiftregister`, `RegisterFile`/`registerfile`,
  `FieldExtend`/`fieldextend` — have **no** gif today; this is already
  pinned as a known gap by `PaletteContractTest.KNOWN_MISSING_ICONS`
  (`test/jls/edit/PaletteContractTest.java:76-77`) and the issue doesn't
  mention it at all, so it's silent on whether the "33 SVGs, same base
  names" plan is meant to finally draw those three or perpetuate the gap in
  SVG form.

Net effect: "one SVG per existing GIF (33 glyphs)" is neither the right
inventory to redraw (3 of the 33 are dead weight that would waste
hand-authoring effort on assets nothing shows) nor a complete one (3 in-use
icons are silently excluded). Recommendation: scope explicitly against
`Palette.entries()` iconNames + the Import button's `"down"` (30 live names)
and treat the 3 dead files and the 3 known-missing icons as separate,
explicitly-decided line items (delete vs. keep dead GIFs; draw vs. defer the
3 missing icons) rather than folding them silently into "33."

### 4. [Low] Minor scope creep, flagged rather than blocking

The `Dimension(32,232)` typo fix (§Method bullet 3, Observation 3) is
unrelated to icon rendering and is bundled in as a "fix in passing." It's a
one-line, low-risk change, so this isn't a real objection — but the issue
should say explicitly that it's a deliberate small drive-by rather than
letting it ride silently inside a checklist item titled "Swap the `getImage`
seam."

## What's solid (no action needed)

- **The core defect diagnosis is accurate and well-cited.** I confirmed the
  raster load at `SimpleEditor.java:2362-2367`, the 33-GIF directory (with the
  caveats in Finding 3), and that `grep -rn "MultiResolutionImage\|FlatSVGIcon"
  src/ test/` returns nothing today — the JEP 263 upscaling story is
  technically correct.
- **Prerequisites are genuinely merged, not aspirational.** `blocked_by: []`
  checks out: the `makeElement(PaletteEntry)` seam exists exactly as cited
  (`SimpleEditor.java:2381`), and `flatlaf` 3.7.2 is a real, current
  dependency (`pom.xml:82`) — `flatlaf-extras` is a natural, same-vendor
  add-on, not a new trust boundary.
- **`FlatSVGIcon extends ImageIcon`** (confirmed against FlatLaf's source),
  so §7.4's claim that `getImage(String)` "keeps its signature" while
  returning an SVG-backed icon is actually correct — the existing
  `ImageIcon image = getImage(...)` call sites at `SimpleEditor.java:2383`
  and the `JButton`/`AbstractAction` constructors at `2332`/`2385` continue to
  compile unchanged. I flagged this as a risk before verifying and it turned
  out not to be one — worth recording so a future reviewer doesn't re-raise it.
- **The missing-icon fallback is preserved deliberately** (§7.11, citing the
  real `SimpleEditor.java:2364-2366` null-check) rather than introducing a new
  failure mode — sound, low-risk design continuity.
- **The #380/#381 boundary comment is good scoping hygiene**, not a defect:
  it correctly declines to let this issue's icon-redraw work be silently
  absorbed by or absorb the larger #381 dark-mode/first-run task, and
  correctly cites #380 as the precedent for the disposition.

## Verdict rationale

Not `should-not-proceed` — the fix is real, small, and low-risk, and its
prerequisites are actually merged. Not `sound` outright, because the
Completion Criteria as written contain a check (100%/200% display-rig
screenshots) that the cited infrastructure cannot currently produce, and the
only checks that *can* run automatically don't test the thing the issue is
actually about. `sound-with-concerns`: proceed, but fix the DoD before
closing this out — either scope the scale-matrix screenshot capability as
real work, or replace it with evidence the repo's CI can actually generate,
and tighten the icon inventory (Finding 3) before authoring 33 SVGs that
include 3 nobody will ever see.
