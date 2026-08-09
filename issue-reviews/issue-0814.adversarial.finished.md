# Issue #814: TASK-C168-1: the 64 SAS glyphs ship as bundled, licence-cleared images, and a missing or unlicensed glyph fails the build rather than the Verify dialog
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Bundle 64 named, GPLv3-compatible images for `Sas`'s fixed glyph vocabulary
(`src/jls/collab/net/Sas.java:38-54`), resolved by index through one
accessor, with per-image provenance recorded, a totality test tying
vocabulary size to asset count, and headless loadability. Five acceptance
criteria. The groundwork (`Sas.vocabularySize()`, `Sas.word(index)`,
`Sas.glyphIndices()`) already exists and is exactly the seam AC-1 needs. The
problems are upstream of the AC text: this task's own existence and framing
conflict with what its parent feature issue (#168) says about splitting it
out, and the licensing criterion cannot be enforced by anything `mvn verify`
checks.

## Findings, most severe first

**1. (Critical) This task is exactly the decomposition #168's own rationale rejected, and the contradiction was never reconciled.**
#168 §2 ("Decomposition & Rationale") states in the issue body (still current
as of the 2026-08-02 REPLAN, unedited since): *"An alternative cut (glyph
images as their own task) was rejected: the Verify dialog is untestable
without the images, so they ship together."* #814 is precisely that rejected
cut — glyph images filed as their own task (TASK-C168-1), decoupled from the
Verify dialog task (#816, TASK-C168-3), which merely lists
`ordering_after: ["TASK-C168-1", "TASK-C168-2"]`. #168's most recent comment
(2026-08-08) does record the 4-way split (#814/#815/#816/#817) as a
boundary note, but it never strikes or updates the earlier rejection text,
and #168's own `planned_tasks:` YAML machine block still shows the old
2-task shape — meaning the feature's own "Completion Criteria" bullet
*"Machine block, roster table, and mermaid graph agree with reality at
close"* is already false today, before close. The risk the original
rationale warned about is real and independently found: the sibling review
of #816 (`issue-reviews/issue-0816.adversarial.md`, finding 6) notes #816
has no documented fallback if #814 stalls. **Recommendation:** either #814
should be re-merged into the dialog task per #168's stated rationale, or
#168 §2 should be edited via REPLAN to retract the rejection and record why
splitting is now considered safe (e.g., because AC-2's totality test makes
#814 atomic/all-or-nothing, which at least preserves the "ship together"
spirit at the asset level even though it ships as a separate issue).

**2. (High) AC-1's "existing bundled-image precedent" doesn't establish what this issue needs it to establish.**
AC-1 cites `src/jls/edit/images/` as precedent for licence/provenance
discipline. Checked: that directory holds 33 `.gif` toolbar icons with no
companion license file, no NOTICE, no SPDX headers, and no provenance
record anywhere in-tree (`grep -rn "SPDX\|licen" src/jls/edit/images/*`
returns nothing). There is no existing precedent for *recording* licence and
provenance for bundled images in this repo — AC-3's discipline is new, not
inherited, and citing precedent that doesn't exist could lead an
implementer to look for a documentation format to follow that isn't there.
Worse, this task doesn't extend its own new discipline backward: the 33
existing icons remain permanently un-audited even after this closes.
**Recommendation:** drop the "existing precedent" framing, and either scope
an audit of the 33 existing icons into this task or explicitly note it as
future work (issue #814 is the natural place to raise it, since it is
introducing the first in-tree licence-tracking convention for images).

**3. (High) AC-3's licence claim is asserted, not verifiable — nothing green-lit by AC-5 can catch a wrong claim.**
*"The set's licence is recorded in-tree and asserted GPLv3-compatible... if
no compatible set can be obtained, the gap is recorded and escalated to the
maintainer rather than vendored."* No test checks that an asserted licence
is actually GPLv3-compatible (that's a legal judgment `mvn verify` cannot
make), so `mvn verify` green (AC-5) is compatible with a provenance table
containing fabricated or simply wrong licence claims. The escalation
fallback also has no trigger criteria — "if no compatible set can be
obtained" doesn't say how much sourcing effort counts as exhausted, so an
implementer under time pressure can satisfy AC-3 by escalating after a
token search, and a reviewer has no AC-anchored way to tell a good-faith
escalation from a lazy one. **Recommendation:** require the provenance
record to cite a checkable source (URL + licence file/version, or a named
human reviewer sign-off for "original work" claims) and give the escalation
path a minimum-effort bar (e.g., N candidate sets considered and rejected,
named) so AC-3 is auditable rather than self-certified.

**4. (Medium) The claimed "close the feature's one Open Question" doesn't actually happen.**
#168's Open Questions section has exactly one entry — *"draw ~64 original
named images vs. adapt an existing GPLv3-compatible set... Rides along."* —
and #814's Outcome claims *"This closes the feature's one Open Question...
with the decision written down rather than implied."* But AC-1/AC-3 never
require picking one strategy: AC-3 only demands *per-image* provenance
("source or 'original work'"), which is satisfied equally by an arbitrary
per-glyph mix of adapted and hand-drawn images. A compliant implementation
can leave the actual strategic question (which path, and whether the
resulting 64-image set is stylistically coherent) exactly as open as it is
today, just itemized rather than decided. **Recommendation:** either AC-3
should require a single stated strategy for the set as a whole (with
per-image entries as evidence of following it), or the Outcome section
should stop claiming the Open Question is closed by this task.

**5. (Medium) Feasibility: 64 phonetically-distinct, visually-distinguishable, single-license, consistently-styled icons is a bigger task than `band_mw: "0.5-1"` implies.**
Several words cluster into overlapping visual categories that will be hard
to make instantly distinguishable at a glance under time pressure (a
real security-verification context): apple/cherry/grape/lemon (fruit);
flower/clover/leaf/maple/rose (foliage); crown/helmet (headwear-adjacent).
No single existing openly-licensed 64-icon set matching these exact nouns
in one consistent style is cited, and none is obviously known to exist —
assembling one (search, licence-check, and stylistically normalize 64
individual items, or draw 64 originals) is realistically a multi-day design
task, not a half-week-or-less one. This mirrors a pattern the sibling #816
review independently flagged for that task's own screenshot-rig estimate.
**Recommendation:** size this task against an actual candidate-set survey
(or a decision to commission/draw originals) before committing to the
stated band.

**6. (Low) Image size/format is unspecified, and the cited precedent points toward the wrong scale.**
The existing toolbar icons are 24×24 px (`file src/jls/edit/images/and.gif`
→ `GIF image data, version 89a, 24 x 24`). AC-1's "following the existing
bundled-image precedent" gives no explicit minimum resolution, so a
technically-compliant implementation could ship 24×24 thumbnails — fine for
a toolbar button, likely too small for two humans to compare a glyph
image confidently in a security dialog (the Signal-safety-number
precedent this feature cites uses much larger emoji). **Recommendation:**
state a minimum size/format explicitly rather than relying on an analogy to
toolbar-icon scale.

**7. (Low) The accessor's package/home is unstated.**
AC-1 requires "one accessor" but not where it lives. `Sas.java` sits in
`jls.collab.net`, which carries its own purity rules
(`ArchitectureRulesTest.transportKnowsNothingOfCircuits`); `jls.collab.ui`
doesn't exist yet (confirmed: `find src/jls/collab -type d` shows only
`net`, `session`, `op`, `crdt`). Neither placement is forbidden by anything
checked here, but leaving it unstated invites an accessor bolted onto `Sas`
itself, coupling a crypto/protocol class to an asset-loading concern.
**Recommendation:** name the target package/class in the AC text.

## What's solid

- `Sas`'s existing API (`vocabularySize()`, `word(index)`,
  `glyphIndices()`, `Sas.java:107-153`) is exactly the by-index seam AC-1
  needs; no change to `Sas`'s public contract is required.
- AC-2's totality-test pattern (bind vocabulary size to asset count and
  accessor) has real precedent in this codebase —
  `HelpTopicsTest`'s palette-coverage completeness test, `SaveTagsTest`,
  `ElementVocabularyTest` — and is concretely testable as stated.
- AC-4's "no Swing dependency below `jls.collab.ui`" restates an
  already-enforced rule (`ArchitectureRulesTest.java:150-160`,
  `collabLayersAreHeadless`), so this criterion is close to free to satisfy
  and trivial to check.
- AC-5 (no new runtime dependency) is realistic: image loading needs only
  JDK APIs already used elsewhere in the codebase.

## Verdict rationale

Not `should-not-proceed` — the technical seam is sound and the security
motivation (glyph images make the Verify dialog usable) is real. Not
`sound-with-concerns` — finding 1 is not a nitpick: the task's own parent
feature explicitly argued against filing it this way, for a reason a
sibling review independently confirmed is live, and that conflict sits
unresolved in #168's own text. Findings 2-4 mean AC-3 in particular can be
satisfied on paper without closing the real licensing/design gap. Recommend
`needs-rework`: reconcile the #168 decomposition conflict, make AC-3
auditable rather than self-certified, and either force or drop the "closes
the Open Question" claim before this is picked up.
