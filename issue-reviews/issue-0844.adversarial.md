# Issue #844: TASK-C573-3: the demo says out loud what it is not, and hands the visitor the installer — the funnel closes instead of dead-ending
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#844 is a task four layers into an unbuilt tree: CAP-32 (#516) → FEAT-C32-1
(#572, the CheerpJ feasibility spike) → FEAT-C32-2 (#573, the static demo
page) → TASK-C573-2 (#841, the full curated set) → TASK-C573-3 (#844, this
issue: state what the desktop adds, link the installers). Every ancestor is
confirmed open at HEAD: #572's only comment is a dedup boundary note, not the
"explicit written go/no-go" its own AC-4 promises; #573, #841, #840 are all
open with zero implementation. Nothing #844 depends on exists yet, and its
own mechanism (a page to add text and links to) is unnamed until #572
resolves. Beyond that inherited blocker, #844 adds its own defects: an
Outcome/AC mismatch on "platform" targeting, an internal inconsistency
between its two verification ACs, and an unacknowledged three-way overlap
with #573 AC-5 and #574 AC-4 over the same capstone requirement.

## Findings, most severe first

**1. The whole task is blocked on an upstream decision that has not been made, and the capstone's own viability is still an open question.**
#572 AC-4 requires "an explicit written go/no-go" before FEAT-C32-2's
mechanism is named; the only comment on #572 (2026-08-04) is a
deduplication boundary note against #573, not a verdict. #573, #841 (this
issue's direct `ordering_after` target), and #840 are all open, unimplemented
issues. There is, today, no demo page for #844 to add funnel copy or install
links to. Compounding this, CAP-32's own body carries a live self-doubt
clause — "If the maintainer judges even this inside the CAP-19 refusal's
intent, close this too" — and #572's dedup comment on #573 references it
without resolving it. Filing a task this granular (`band_mw: "0.25-0.5"`,
the smallest estimate in the whole C573 task set) four levels below an
unresolved mechanism decision and a capstone with an acknowledged closure
risk is the same premature-filing pattern independently flagged in the
existing #847 review (finding 4) for a sibling task under FEAT-C32-3.
*Recommendation:* do not schedule implementation until #572 posts its
go/no-go comment and #841/#840 have landed; until then #844 cannot be
started, only planned.

**2. The Outcome promises per-platform installer links; AC-2 only requires "links to the installers," and nothing names how platform targeting would work on a static, backend-free page.**
Quoted Outcome: "gets a direct link to the installers for their platform."
Quoted AC-2: "Each page links to the installers, and the link target is
checked in CI." AC-2 is satisfied by one generic link to the
[Releases page](https://github.com/anadon/JLS/releases) — the Outcome's
"for their platform" promise is not in any AC and could be silently dropped
with AC-2 still passing. If "for their platform" is taken seriously, it
requires client-side platform detection (`navigator`/UA sniffing) on a page
that, per #573 AC-2/AC-3, is meant to stay a read-only static artifact with
no runtime logic of its own — and the JLS repo currently ships zero
JavaScript anywhere (confirmed in the #573 adversarial review's
`grep -rli javascript src/` → 0). README.md's own install matrix (lines
12-46) has nine-plus distinct artifacts across six platform rows, including
edge cases plain UA sniffing gets wrong today (Windows-on-ARM vs. x86_64,
NixOS's flake-only path, RISC-V's "no installer, use the jar" carve-out,
Intel Mac's "use the jar below" fallback) — none of this detection logic is
costed anywhere in this issue or its ancestors.
*Recommendation:* either drop "for their platform" from the Outcome (link to
the Releases page, which already lists everything, matching AC-2 as
written) or add an AC that names the detection mechanism and its fallback
behavior for platforms it gets wrong.

**3. AC-2 and AC-4 disagree on how durable the link verification is supposed to be.**
AC-2 wants continuous protection: "the link target is checked in CI so a
moved release URL fails a lane instead of stranding visitors." AC-4 wants a
single pass: "The install link path is verified end to end once: demo page →
installer download page → a real installer asset" (emphasis on "once" in
the issue's own text). The deeper check — that the chain actually terminates
on a real installer binary, not just a URL that resolves — is explicitly a
one-time check, while the shallower link-target check runs forever. After
the one-time AC-4 pass, a change to the release pipeline's asset naming or
page structure (something this project's own README documents happening
across versions — see the deb/rpm/AppImage/msi/dmg naming conventions) could
silently break the deep chain while AC-2's shallower CI lane keeps passing.
*Recommendation:* either fold AC-4's asset-level check into the same
recurring CI lane AC-2 describes (even at reduced frequency, e.g.
release-triggered rather than per-push), or explicitly document AC-4 as a
launch-only gate and accept the residual drift risk in writing.

**4. Unacknowledged three-way overlap with #573 AC-5 and #574 AC-4 over the identical capstone requirement, with no stated owner.**
CAP-32 (#516) AC-4 reads: "The demo page states plainly what the full tool
adds and links the installers." #573 (this issue's parent feature) already
carries this near-verbatim as its own AC-5: "The page states plainly what
the full desktop tool adds beyond the demo and links the installers
(capstone AC-4)." #574 (FEAT-C32-3, the sibling shop-window-integration
feature) independently claims the same requirement in its AC-4 — "the demo
page states what the full tool adds and links the installers (capstone
AC-4)" — despite #574's own boundary notes disclaiming ownership of demo
page content ("Integration only... the demo mechanism and pages are
FEAT-C32-1/-2's scope"), a contradiction already flagged in #574's own
adversarial review (finding 1). #844 is now the third issue to carry
implementation responsibility for this one capstone sentence, and it neither
references #574 nor declares an `ordering_after`/boundary note resolving
which issue's PR is authoritative. Two independently merged PRs (#574's
shop-window links, #844's on-page content) could each claim "capstone AC-4
satisfied" while writing inconsistent capability lists or install-link
targets, with nothing in either issue checking the other's output.
*Recommendation:* have #844 (or #573) state explicitly that #844 is the sole
owner of the demo-page content and #574's AC-4 is redundant/should be struck
or narrowed to "links exist," closing the gap already identified upstream.

**5. AC-1's "specific capabilities rather than marketing adjectives" is a subjective bar with no test or example, and AC-3's "where a visitor asking 'can I edit?' will see it" is unmeasurable placement guidance.**
Both are reasonable intentions but neither is checkable mechanically: any
short bullet list technically satisfies "specific capabilities," and
"where...will see it" names no placement rule (above the fold? next to the
input toggles? in a persistent banner?). This is the same pattern
independently flagged in #573 (finding 7, "states plainly") and #574
(finding 7, "prominent"). Low-to-medium severity — a human reviewer will
likely catch an egregious failure — but as written a minimal-effort
implementation (one line of marketing copy, a footnote disclaimer) passes
both criteria literally.
*Recommendation:* borrow the concreteness precedent already set elsewhere in
this issue cluster (e.g. #545's "above the fold" language) — name a location
and a minimum content bar (e.g. "editing, saving, batch grading, and HDL
export each named individually, placed within one scroll of the interactive
circuit").

**6. AC-5 (light/dark theme, phone-width viewport) has no verification mechanism, and the project owns no infrastructure to check it.**
`.github/workflows/` (per the existing #574 adversarial review, confirmed
unchanged) has no Pages/site/web-QA lane — only `ci.yml`, `codeql.yml`,
`mutation.yml`, `release.yml`, `repro-installers.yml`, `scorecard.yml`.
ARCHITECTURE.md's own UI-testing layers (Layer 2 Swing-under-Xvfb, Layer 3
render-to-image) are explicitly "reserved," not built, and scoped to the
Swing GUI, not a static web page. Nothing in #844 or its ancestors proposes
even a manual checklist, let alone automated visual regression, for a
cross-theme/cross-viewport requirement on a page format this project has
never shipped before. This is the same "seam nobody owns" (a web-publishing
substrate) independently identified in the visionary review of #573.
*Recommendation:* name at minimum a manual pre-merge checklist (specific
browsers/viewports to eyeball) if automated visual testing is out of scope
for this task's budget.

**7. Cost estimate (`band_mw: "0.25-0.5"`) looks tight once findings 2, 3, and 6 are priced in.**
This is the smallest band in the entire TASK-C573 set (#840: 0.5-1, #841:
0.5-1, #844: 0.25-0.5) despite carrying: new CI link-check infrastructure
this project has never built for a web artifact, a deep one-time asset
verification across the release pipeline, and cross-theme/viewport QA with
no existing tooling to reuse. If "for their platform" (finding 2) is taken
literally, add a first-ever client-side JS component to the estimate too.
None of this is necessarily wrong to attempt at this band, but the estimate
reads as "copy-editing on an existing page" scope, not "stand up new CI +
QA infrastructure" scope.
*Recommendation:* re-estimate once #572's go/no-go and #573/#841's actual
build tooling are known — the true cost of #844 depends heavily on what CI
surface those ancestors leave behind to extend.

## What's solid

- AC-3's KC-32-2 citation is concretely placed ("where a visitor asking 'can
  I edit?' will see it") rather than a vague restatement — better than the
  placement vagueness flagged elsewhere in this cluster.
- AC-2's instinct to CI-check the install link is a good match for this
  project's established pattern (`HelpTopicsTest`'s link checker, cited in
  ARCHITECTURE.md's Test layout section) — the idea is right even though
  finding 3 shows its execution is inconsistent with AC-4.
- `ordering_after: ["TASK-C573-2 (#841)"]` correctly sequences behind the
  full example set rather than jumping ahead of it — the dependency shape is
  right, even though finding 1 shows the whole chain is still blocked
  further upstream.
- The non-duplication of content scope (this task adds no circuits, no
  mechanism decisions — only messaging and links) is a clean, narrow cut
  consistent with the rest of the C573 task decomposition.

## Recommendation

Hold #844 until #572 posts an actual go/no-go and #840/#841 land — there is
currently no page to edit. When it is picked back up: align the Outcome's
"for their platform" language with what AC-2 actually requires (or cost the
platform-detection work explicitly), unify AC-2 and AC-4 into one
consistently-enforced verification story, and resolve the three-way overlap
with #573 AC-5 and #574 AC-4 by naming #844 as the sole implementer of the
capstone's "states plainly / links installers" requirement.
