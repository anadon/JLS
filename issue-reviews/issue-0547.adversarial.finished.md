# Issue #547: FEAT-C26-5: the VPAT writes itself from the test suite — no criterion claimed without a named passing test, and Swing limits listed as exceptions by name
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

A generator that mechanically binds every claimed WCAG 2.2 AA / VPAT criterion
to a named passing automated test (AC-4 `VpatCoverageTest`, per parent
capstone #507), lists Swing accessibility-API limits as named exceptions,
distinguishes manual-NVDA-checklist claims from automated-Orca claims, ships
claim strength capped at "guideline-compliant, machine-verified," and refuses
to ship a hand-authored VPAT if the mapping can't be made mechanical
(KC-26-3). `ordering_after: [FEAT-C26-1, FEAT-C26-3]` (#542, #544).

## Findings, most severe first

**1. (High) The core acceptance criterion is gameable in exactly the way the repo's own prior art warns against, and the current codebase already demonstrates the failure mode.**
AC-4 requires only "a named passing automated test" — it does not require that
test to demonstrate a real assistive-technology bridge delivers anything to a
user, as opposed to merely asserting Java Accessibility API state
(`getAccessibleContext().getAccessibleName()` non-blank, etc.). The repo
already contains a fully-worked internal playbook for this exact feature,
`docs/standards-adoption/03-accessibility-conformance.md`, whose "Top three
ways this goes wrong" item 1 states verbatim: *"The ACR is written from the
test suite and never from a screen reader. Every automated signal in this
repo reads the Java Accessibility API. Not one of them proves a bridge
delivers anything to a user... it is entirely possible to have a fully green
suite and an ACR claiming 502.3 support for a build where NVDA reads
nothing."* That document then proves the gap is live today, not
hypothetical: `scripts/build-installer.sh:145` derives the jlink module set
via `jdeps --print-module-deps --ignore-missing-deps "$JAR"` (confirmed by
reading the script), which cannot include `jdk.accessibility` because nothing
in the jar statically references it — so the shipped Windows `.msi` bundles a
runtime with **no Java Access Bridge**, and NVDA/JAWS get nothing from an
installed JLS today. A `VpatCoverageTest` built to AC-4's literal wording
would happily let a green `AccessibleNameCoverageTest` justify a "Supports"
row for 502.3/4.1.2 on the exact build where that claim is false for the
primary Windows distribution.
Recommendation: AC-4 must require a distinct evidence tier — API-level
assertions (`AccessibleTreeGoldenTest`-style) are necessary but not
sufficient for 502.3.*/11.5.2.*/4.1.2-class criteria; those rows need a dated
manual AT-bridge session recorded in `docs/accessibility-at-checklist.md` (or
equivalent) before `VpatCoverageTest` allows anything above "Not Evaluated."
The issue should also make landing the §1 Access Bridge fix (append
`jdk.accessibility`, write `accessibility.properties`) an explicit
precondition, per the playbook's own "Do NOT do this if... the §1 bridge fix
is not landed first" rule — #547 currently cites none of this.

**2. (High) `ordering_after` is stale by the project's own admission, and no REPLAN has fixed it.**
The single comment on #547 (a cross-issue dedup pass, 2026-08-04) states
plainly: *"the ordering list is arguably incomplete: a WCAG 2.2 AA claim
about keyboard operability or accessible names has no passing test to cite
until #549's gate exists. Recommended: add #549 to this issue's
`ordering_after`."* #549 (FEAT-C26-6, the CI operability ratchet) is **not**
in the machine block's `ordering_after: [FEAT-C26-1, FEAT-C26-3]`, and #507's
own tracking comment confirms the graph was filed as `{PF-1,PF-3}→PF-5` with
no PF-6 edge. As filed, `#547` can start work citing today's ad hoc keyboard
tests as "the" evidence for keyboard/name criteria, then have that evidence
silently invalidated (or duplicated under a different name) once #549 lands
its ratchet — the opposite of "no criterion claimed without a named passing
test" applied to the generator's own inputs.
Recommendation: block start-of-work on a `REPLAN:` comment on #507 (or a body
edit) adding #549 to `ordering_after` before any implementation begins, per
the process this repo's own tooling already flagged.

**3. (Medium-High) Silent dependency on a feature that may not exist in claimable form.**
#544 (FEAT-C26-3), one of the two `ordering_after` entries, is explicitly
**unfunded pending an Orca feasibility spike** with three branches: spike
passes → funded; live announcements unreachable → rescope to
navigation-only, record a VPAT exception; navigation itself can't speak →
**stop the feature and file a platform finding (KC-26-2)**. #547's body
anticipates only the middle branch ("Swing accessibility-API limits...
listed as exceptions by name"). It says nothing about the third branch, where
#544 is killed outright — the "category-defining" PF-3 claims the parent
capstone's Outcome leans on for procurement value would then not exist to
transcribe, and #547's own value proposition (`"the most-likely-requested
certification"`) is weakened without a stated fallback.
Recommendation: add an explicit note on how the generator behaves if
`ordering_after`'s FEAT-C26-3 closes as killed/rescoped rather than landed —
at minimum, confirm the mapping mechanism (#753) treats "feature never
shipped" identically to "criterion has no test" rather than as a special
case that needs code changes later.

**4. (Medium) The criterion→test mapping mechanism is under-specified in this issue and only partially specified in its sibling.**
#547 doesn't say who authors or maintains the criterion-to-test table, how
many rows it covers (the internal playbook estimates ~90 WCAG/508/EN
criteria × 5 product surfaces), or how the mechanism distinguishes "row
absent" (silently unclaimed — fine) from "row present but wrong" (a stale
test name that still happens to exist, still "passes," but no longer proves
what the row claims — the classic coverage-ratchet failure mode this repo's
own `DialogCoverageRatchetTest`/`IdentityKeyCoverageTest` family exists to
prevent for other purposes). The actual mapping work is deferred to #753
(TASK-C547-1, `part_of_feature: 547`), which is reasonable, but #547 doesn't
reference #753 at all — a reader of #547 alone has no way to know the
mapping-authoring mechanism is a separate, already-filed sub-task, or that
#753 itself only requires the table be "keyed by criterion identifier" with
no stated review process for correctness of the *mapping*, only for its
existence.
Recommendation: cross-reference #753 explicitly in #547's body (boundary
notes), and state who/what reviews a new mapping row for correctness, not
just presence.

**5. (Medium) Cost/ownership gap: the content the generator needs isn't fully covered by cited work.**
The internal playbook's sizing table attributes real, non-trivial prerequisite
work to producing an *honest* VPAT: §1 Access Bridge fix (1 day), §2 contrast/
focus-visible fixes including a new `focus` Theme role and canvas focus ring
(2 days), §3 help doc alt/lang fixes (1 day), §5 gap analysis across ~90
criteria (2 days), and a 3-platform manual AT audit (3 days) — 13 days total
before authoring even starts. None of #542, #543, #544, #546, #549, or #753
is named in #547 as owning the §1/§2/§3/§5 work, and #547's own `band_mw: 1-2`
matches only the "authoring, review, publication wiring" line item (2 days)
in that table, not the prerequisites. As written, #547 risks either (a)
generating a mostly "Does Not Support" / "Not Evaluated" document because the
underlying gaps (contrast failures, missing Access Bridge module, help
alt/lang) were never fixed by any cited issue, or (b) quietly absorbing that
prerequisite work itself, blowing the 1-2 mw band by roughly an order of
magnitude.
Recommendation: either explicitly scope #547 as "generator only, over
whatever the suite currently proves — expect mostly Not Applicable/Not
Evaluated rows at first landing," or file/point to the issue(s) that own §1/
§2/§3/§5, and reflect that dependency in `ordering_after`.

**6. (Low) `feat_id`/`ordering_after` values are provisional feature-IDs, not issue numbers — fine as documented ("provisional; adversarial phase renumbers"), but worth flagging that #542/#544 resolve correctly (verified: #542 = FEAT-C26-1, #544 = FEAT-C26-3) so no numbering-drift error exists there today.**

## What's solid

- The central design principle — no claim without a named, passing,
  mechanically-checked test — is sound and has real in-repo precedent
  (`DialogCoverageRatchetTest`, `IdentityKeyCoverageTest`,
  `KnownPeersCoverageTest`, etc. all implement the same "coverage ratchet"
  shape), so the mechanism itself is feasible to build.
- Distinguishing manual-NVDA-checklist claims from automated-Orca coverage is
  a specific, verifiable requirement that matches the parent capstone's own
  AC-2/§3.3 framing — good, checkable line.
- KC-26-3 ("no hand-authored VPAT ships if the generator can't be made
  mechanical") and the claim-strength-upgrade-only-by-evidence rule are both
  concretely falsifiable governance rules, not vague aspirations.
- No licensing or security hazard: this is a documentation-generation feature
  over existing test infrastructure; nothing here touches the GPLv3
  boundary, the plugin trust model, or introduces new external dependencies.

## Verdict rationale

The concept and most acceptance criteria are sound, but AC-4 as literally
worded is gameable against the project's own already-documented failure mode
(finding 1), the machine-readable ordering is acknowledged-stale by the
project's own dedup tooling with no REPLAN yet applied (finding 2), and the
issue is silent on what happens if its sole non-#542 dependency is killed or
rescoped (finding 3). These are fixable without discarding the issue — hence
**needs-rework**, not should-not-proceed — but work should not start until
the ordering_after gap is REPLANed and AC-4 is tightened to require
AT-bridge-level evidence (not just API-level test passes) for the
502.3/4.1.2 criterion class.
