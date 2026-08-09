# Issue #507: CAP-26: a colorblind student and a blind student complete the same JLS lab — one through a verified palette with redundant encoding, one through spoken navigation, prose narrative and a swell-paper tactile export
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what this issue actually is

This is a capstone (CAP-26) with `requires_features: []`/`requires_capstones: []` at filing time; its
2026-08-04 comment confirms all six planned features have since been filed as standalone issues
(#542, #543, #544, #546, #547, #549). So #507 itself is now mostly a tracking/roster document, and
most of the concrete engineering risk lives in those six issues, reviewed separately. This review
attacks the capstone's own claims, dependency graph, and acceptance criteria.

## Findings, most severe first

**1. PF-3's hard prerequisite (an accessible canvas scene model) is filed nowhere and is
mischaracterized as "related" rather than blocking.** PF-3 promises "spoken element, connection and
live signal-state announcements" for screen-reader navigation. But the editor canvas today is, in
`ARCHITECTURE.md`'s own terms, a single `SimpleEditor` canvas painted by one `paintComponent` — and
issue #355 (FEAT-011 RESIDUAL, open) says this explicitly: *"the editor canvas is one custom-painted
panel, so assistive technology sees one opaque object where a circuit is"*, and names the fix as its
own not-yet-filed **TASK-0029** ("the accessible canvas scene model... element children with
name/role/location/selection, wire relations"). #355 is itself `blocked_by: [316]` (FEAT-008, the UI
harness). #507's machine block carries #355 only in `related` ("the filed neighbor this extends"),
never in `requires_features`, and its mermaid graph has no edge from #355/#316 into PF-3. Without
TASK-0029 landing, there is structurally nothing for a screen reader to enumerate — PF-3 cannot
deliver AC-2 no matter how the 5–8 mw band is spent. This is a real, checkable gap in the dependency
graph, not a nitpick: `ARCHITECTURE.md`'s own "Test layout" section confirms Layers 2/3 of the UI
harness (`test/jls/ui/package-info.java`) are "reserved," i.e. not yet built either.
Recommendation: add #355 (at least its TASK-0029 half) to `requires_features` or an explicit
`blocked_by`, and re-derive PF-3's cost band to include-or-exclude scene-model construction
explicitly — right now it's silently assumed to exist for free.

**2. The issue's own "proof" that nothing exists on this path is false, and trivially so.** The
Background section runs `grep -rli "tritanop\|vpat\|tactile\|swell" src/ test/ docs/ | wc -l` and
reports `0`. Reproducing that exact command against the cited `evidence_commit`
(`828822672fc3a8e2cb6da25192472079f04c29dd`) and against current HEAD both return **8** matching
files, including `docs/standards-adoption/03-accessibility-conformance.md` — a substantial (800+
line) VPAT/ACR planning document dated 2026-07-28 (about a week before #507 was filed) that already
recommends the **VPAT 2.5 INT edition** (WCAG + Revised 508 + EN 301 549 in one document), names the
non-web-software SC exemptions, and explicitly states *"A new GitHub issue must be opened for this
work; it is called the ACR issue throughout."* PF-5 (filed as #547) does not appear to cite or
reconcile with this document; Open Question 2's "recommended default: WCAG 2.2 AA" is a narrower,
less-developed answer than what's already recorded in-tree. Either the evidentiary grep was run
against a different working tree than claimed, or it was not actually run. Recommendation: fix the
Background claim, and have PF-5/#547 explicitly reconcile with (or supersede) 03-accessibility-conformance.md
rather than risk authoring a second, inconsistent VPAT plan.

**3. AC-2 promises automation that the cited CI rig explicitly disclaims, and that a sibling issue
frames as manual.** AC-2 (`OrcaLabSessionTest`) requires "a scripted Orca session" building and
simulating a circuit "in the Wayland CI rig" (#101). But #101's own Capability Statement lists
*"interaction scripting with wtype and any appearance-level assertions"* as **out of scope**, owned by
#91 — and #91's harness is, per `ARCHITECTURE.md`, still at Layer 1 (headless model assertions) with
Layers 2/3 "reserved." Separately, #75 (keyboard operability, "related" to #507) frames its own
VoiceOver/Orca check as a **manual, human** pass ("Assistive-tech manual pass... human + hardware
input to schedule"), not a scripted/automated one. #507's own §1 Step 2 states the Orca observation as
a flat fact with no hedge; the real risk only surfaces later in KC-26-2 ("if Swing cannot deliver live
signal-state announcements through Orca at all... re-scope"). A reader who stops at the Outcome
Statement, or a scheduler funding the "demo slice," would not see that AC-2's automation premise is
unproven and contradicted by two cited issues. Recommendation: state the Orca-automation risk in §1
itself, not only in the kill criteria, and make PF-3/#544's Orca feasibility spike a hard prerequisite
of AC-2 rather than a parallel concern.

**4. AC-3's guideline citation is a category mismatch, and half of it is unsourced.** AC-3 requires
"narrative ordering passes the guideline checklist test" for the prose narrative, and separately "the
SVG passes the tactile lint," both attributed to "the cited tactile-graphics guidelines" (BANA,
per Open Question 3). BANA's guidelines govern embossed tactile *graphics* — line width, spacing,
symbol substitution — not text/prose ordering. Open Question 3's recommended default only resolves the
guideline source for the SVG lint; no standard is named anywhere for what makes a prose narrative's
"part-to-whole" ordering correct or checkable. For an AC billed as mechanically verifiable, half its
substance (the prose half) currently has no named authority to check against.
Recommendation: name a distinct source for the narrative-ordering rule (e.g. an image-description /
long-description guideline) before PF-4/#546 is implemented, or split AC-3 so the unsourced half is
visibly weaker than the sourced half.

**5. AC-1's tritanopia/redundant-encoding bar is unquantified where the shipped analog is
quantified.** The shipped floor (deuteranopia/protanopia) is a specific, testable number: "at least 25
CIE76 delta-E apart," enforced by `ThemeTest` (`src/jls/Theme.java:20-23`). AC-1 extends the claim to
tritanopia and to "grayscale" and to a "non-color channel alone" but states no analogous numeric or
mechanical threshold for any of the three — "distinguishable... verified by automated screenshot
analysis" is exactly the kind of criterion the issue's own Risk 1 warns about ("Automated checks are
necessary, not sufficient... the capstone must not ship prose that claims more than its tests prove").
A screenshot-diff test could pass by choosing an easy circuit/state pair while still failing on
adversarial real content. Recommendation: pin a numeric or algorithmic bar for the redundant-encoding
and grayscale checks the same way the delta-E floor pins the color checks, in #542/#543 (PF-1/PF-2),
before AC-1 is treated as satisfied.

**6. `K9` is used three times and defined nowhere.** §1 Step 6 and KC-26-4 both cite "(K9)" for the
pixel-identical-default-theme requirement, but no definition of "K9" appears in #507's body, and a
repo-wide search of the tracked docs finds no other occurrence at all. If K9 is a convention from
another (unlinked) planning document, the issue is unreadable in isolation; if it's a typo/leftover
reference, it should be resolved to plain language. Minor, but it is exactly the kind of drift the
issue's own completion checklist ("every cited evidence document... resolves") is meant to catch, and
today does not.

**7. The re-planning protocol is already being violated by the issue's own tracking comment.** §5 rule
1 states "A planned feature is filed → REPLAN resolving the PF entry." The 2026-08-04 comment confirms
all six PFs are filed, but explicitly notes "the machine block was not edited by this pass" — so
`planned_features` in the current issue body still lists six unresolved YAML strings instead of the
filed issue numbers, and `blocked_by`/mermaid are stale relative to the six new issues' own graphs
(e.g. #355's transitive edge into PF-3, finding 1 above). This isn't a close-time violation yet, but
it is drift the issue's own process claims should not accumulate un-REPLAN'd.

## What's solid

- The kill criteria (KC-26-1..4) are concrete and correctly target the actual risk concentrations:
  grayscale legibility vs. schematic clarity, Swing/Orca's live-announcement ceiling, VPAT honesty,
  and default-theme pixel-identity. This is unusually good self-skepticism for a filed issue.
- The Background section's verifiable claims about `src/jls/Theme.java` (Okabe-Ito DEFAULT palette,
  the 25-delta-E `ThemeTest` floor, the `CLASSIC` variant, ~126 hardcoded-black call sites) all check
  out against the file as it stands today.
- Explicitly deferring PF-1/PF-6 co-funding to FEAT-011's (#355) residual and the theme dark-variant
  work is the right instinct — it's just not applied consistently to PF-3, which needs #355 more
  structurally than PF-1 does (finding 1).
- K9/theme-pixel-identity as a gate on every PF-1 commit rather than a release check is a sound,
  cheap regression guard.

## Bottom line

The capstone's risk register (kill criteria, cross-feature risks) shows real engineering judgment, and
the six PFs are now filed for individual review. But the dependency graph omits a hard blocker
(finding 1), the Background section's central evidentiary claim is factually wrong and easily
disproven (finding 2), and one system-level acceptance criterion (AC-2) asserts automation that two
cited, in-repo issues say is out of scope or manual (finding 3). None of these sink the capstone's
premise — they're fixable with a REPLAN and a dependency-graph correction — but they should be fixed
before PF-3/#544 work is funded or scheduled, since the cost estimate and AC-2 both currently assume
away the accessible-scene-model prerequisite.
