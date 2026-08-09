# Issue #841: TASK-C573-2: the whole curated example set is served with its captions, each reaching interactive in under thirty seconds from the click
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#841 is TASK-C573-2, the middle task of three under FEAT-C32-2 (#573):
after #840 (TASK-C573-1) proves the demo mechanism on one example, #841
scales it to the full CAP-27 curated set (#548), and #844 (TASK-C573-3)
adds funnel copy. The whole chain sits under CAP-32 (#516), which in turn
depends on FEAT-C32-1 (#572)'s CheerpJ-or-fallback feasibility spike. #572
has posted only a deduplication boundary-note comment, not "an explicit
written go/no-go" as its own AC-4 requires — confirmed by reading #572's
single comment directly. Beyond that inherited, already-documented blocker
(independently flagged for #573 and #844), #841 has its own defect: its
Outcome and its own acceptance criteria contradict each other over whether
the *whole* curated set ships or only the subset that clears the bar, and
several ACs invent artifacts (a "manifest," a "stated reference browser")
that no upstream issue defines.

## Findings, most severe first

**1. AC-1/Outcome and AC-3/AC-4 make incompatible promises about how much of the curated set actually ships, and the issue never reconciles them.**
Quoted Outcome: "every circuit in the CAP-27 curated set (#548) is served
from the static demo" — reinforced by the issue's own title, "the whole
curated example set is served." Quoted AC-1: "Every example in the #548
curated set is reachable on the demo." Now quoted AC-3, two sentences
later in the same Outcome paragraph: "An example failing the bar is
excluded from the demo with the reason recorded... rather than shipped as
a bad first impression." AC-1 says every example is reachable; AC-3
explicitly authorizes some to be absent. AC-4 compounds this: "A committed
check fails when the demo's example list and the shipped curated set
disagree" — but AC-3 already establishes that a legitimate divergence
(an excluded example) is the *correct* outcome, not a bug. As written,
AC-4's check either (a) fires on every intentional AC-3 exclusion, making
the "committed check" permanently red the moment one example is dropped,
or (b) needs an allowlist/exception mechanism for "excluded and recorded"
items that the issue never specifies. This is not a hypothetical tension —
it is two ACs in the same short issue asserting opposite guarantees about
the same set.
*Recommendation:* rewrite AC-1 as "every example that clears the bar is
reachable" (matching AC-3's authority to exclude), and give AC-4's check an
explicit exception path for entries recorded as excluded under AC-3, so a
legitimate drop and a silent regression are distinguishable.

**2. The task inherits #573/#572's unresolved mechanism blocker but doesn't even cite #572, and AC-2's core measurement ("click-to-interactive") is undefined if the upstream spike lands on the video fallback.**
`ordering_after` here is `["TASK-C573-1 (the single-example page)", "#548"]`
— #572 is absent even though both named predecessors are themselves
blocked on it (#840/TASK-C573-1's own `ordering_after` is `["#572"]`).
Read directly, #572's only comment (2026-08-04) is a cluster-dedup
boundary note, not the "explicit written go/no-go" its AC-4 promises;
#572 remains open with no verdict recorded. #572 AC-4 names two no-go
fallbacks, one of which is "(b) recorded video walkthroughs" — explicitly
non-interactive. If the mechanism spike lands on (b), #841's central
measurement, "click-to-interactive... reaching interactive in under
thirty seconds," has no referent: a video has no "interactive" state to
reach, and AC-2 as written cannot even be evaluated, let alone passed.
Nothing in #841 hedges this contingency; it is written as though the
CheerpJ-or-equivalent interactive outcome is already secured. This is the
same defect independently found in #573 (finding 1) and #844 (finding 1)
one and two levels up the chain — #841 does nothing to break the pattern
at its own level.
*Recommendation:* add #572 to `ordering_after` explicitly (not just
transitively through #840), and make AC-2 conditional on the interactive
mechanism, naming what "reaching interactive" means under the video
fallback (if anything) before work starts.

**3. AC-1 and AC-4's "generated from the same example manifest" assumes an artifact that #548 never defines.**
Quoted AC-1: "generated from the shipped example set rather than a
hand-kept copy." Quoted Outcome: "generated from the same example
manifest so the demo cannot drift from the shipped examples or invent
captions of its own." #548 (the curated-set issue this depends on) never
uses the word "manifest," and its own AC-3 describes each example
carrying "a caption element" — per #548's cited design source (#381 P8),
this is a Text element living *inside* the circuit file, not a separate
index/manifest file. If #548 lands that way — captions embedded
per-circuit with no aggregate manifest — there is no single artifact for
#841's "generated from" mechanism or AC-4's "committed check" to read from
or diff against; the implementer would have to invent a manifest format
as a side effect of #841, which is scope #841 never budgets for and #548
never commits to producing.
*Recommendation:* either add "produce a machine-readable example manifest"
as an explicit AC-3/AC-4 dependency on #548 (with #548 amended to commit
to that artifact), or specify how #841 derives its example list and
captions directly from per-circuit Text elements without an intermediate
manifest.

**4. AC-2's "stated reference browser and connection" is stated nowhere — in this issue, in #573, or in the capstone (#516) it serves.**
Quoted: "every published example meets <30 s on a stated reference browser
and connection." CAP-32 (#516) AC-1, which this line paraphrases, says
only "<30 seconds from click" with no browser/connection qualifier at all;
#572 and #573 don't define one either. #841 introduces the "stated
reference" framing itself but never states it, leaving the actual
benchmark environment to whoever implements — a materially gameable
choice (a fast browser on a wired connection can pass examples a
realistic classroom Chromebook-over-Wi-Fi setup would fail), especially
since AC-3's exclusion decision hinges directly on this threshold.
*Recommendation:* name the reference browser/connection profile in this
issue (or in #572/#573, and cite it here) before AC-2/AC-3 are
implementable as anything other than "pick numbers that pass."

**5. No test, script, or CI job is named for AC-2's measurement or AC-4's drift check, breaking the project's own established pattern.**
AC-2 ("measured per example and recorded") and AC-4 ("a committed check
fails when... disagree") are both prose commitments with no named
mechanism — contrast with the project's demonstrated discipline elsewhere
in this same task cluster and beyond: #548 AC-3 names `SampleCircuitsTest`
by shape, #551 AC-2 requires "one scripted regeneration command," #545
AC-4 requires a build-time drift check, and ARCHITECTURE.md documents this
project's general practice of pinning behavioral claims to named tests
(`ElementConstructorContractTest`, `HelpTopicsTest`, etc.). As written, a
one-time manual measurement pasted into a PR description technically
satisfies AC-2's "measured... and recorded," and any diff-shaped script
(even one comparing only counts, not content or captions) technically
satisfies AC-4's "committed check" — neither guards against later drift.
*Recommendation:* name the check's shape (a CI lane that re-measures load
time per example, and a test that diffs the demo manifest against #548's
shipped set by content, not just count) the way sibling issues in this
filing batch do.

**6. The cost band looks tight once the RV32I showcase is priced in, and AC-3's exclusion path is the likely real outcome for the marquee example — undercutting the issue's own title.**
`band_mw: "0.5-1"` — the same band as #840 (a single example) — covers
scaling to "the whole curated example set," which per #548 AC-2 must
include "the RV32I showcase." The independent #548 adversarial review
(finding 1) documents that RV32I cannot currently be placed onto a
readable schematic without auto-layout work gated behind #62, itself
gated behind #290 — none of which #841 cites or orders after. If RV32I
is included in #548's shipped set as specified, it is a strong candidate
to fail AC-2's 30-second/interaction-fidelity bar (or to be unusable to
look at at all) and be dropped under AC-3 — meaning the single example the
capstone and #548 both single out as the pedagogical draw is also the one
most likely to be the "excluded" case, undercutting "the whole curated
example set is served" in the title on day one, for a cost band that
assumes no such complication.
*Recommendation:* price RV32I's demo feasibility (or its likely AC-3
exclusion) into the band explicitly, and note in the issue that the title
may end up describing "the shippable subset" rather than the whole set.

## What's solid

- AC-5 (TASK-C573-1's read-only/no-backend/static-files properties must
  hold unchanged across the full set) is a clean, testable-by-inheritance
  criterion that correctly avoids re-litigating properties #840 already
  establishes.
- The scope-slicing itself — prove the mechanism on one example (#840)
  before scaling to the full set (#841) — is sound sequencing and keeps
  #841 from being the first issue to discover integration problems at
  full scale.
- AC-3's underlying instinct (drop an example that fails the bar rather
  than ship a known-bad first impression) is good UX judgment in
  isolation; the problem identified above is purely its unreconciled
  collision with AC-1's "every example" language, not the idea itself.

## Bottom line

#841 inherits the still-unresolved #572 mechanism blocker one level removed
without naming it, and independently contradicts itself over whether the
"whole" curated set or only a passing subset is the deliverable — AC-1 and
AC-3/AC-4 cannot both be taken literally as written. Two of its acceptance
criteria (the manifest, the reference browser/connection) point at artifacts
no cited issue defines. Needs rework on internal consistency and on naming
concrete mechanisms before this is schedulable, independent of the upstream
#572 blocker it shares with its siblings.
