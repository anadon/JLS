# Issue #505: CAP-24: every figure in a lab handout — schematic, timing diagram, eight-cycle animation — is exported camera-ready from the circuit that actually ran, and the LaTeX doc builds in CI
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched issue #505 (open, `tier:capstone`, 1 comment) and its comment; cross-read
README.md, ARCHITECTURE.md; verified every file/line citation in the issue body
against the pinned `evidence_commit` (`828822672fc3a8e2cb6da25192472079f04c29dd`,
a real commit — `git cat-file -t` confirms, merges PR #294); fetched related
issues #71 (closed), #154 (closed), #369 (open), #405 (open), #498 (open, cited
§7.2), and #508 (open, the August product-direction review). Grepped the tree
for `tikz`/`circuitikz` and `wavedrom`/`wavejson`.

## Findings, most severe first

**1. [HIGH] The issue's own tracking data is stale, self-contradicted by its own comment, and the issue's stated self-governance protocol has already been violated.**
The machine block reads `requires_features: []` with the annotation "none
filed yet; planned_features below resolve via REPLAN when filed." But the
issue's single comment (2026-08-04, same day, from the issue's own author)
reports that **all six** `planned_features` (PF-1..PF-6) have in fact been
filed as open `tier:feature` issues #536–#541, and flags this exact
contradiction itself: *"Machine block vs. reality: `requires_features: []`
... is now stale ... but no `REPLAN:` comment has yet resolved the
`planned_features` entries or regenerated the mermaid graph."* I independently
confirmed #536–#541 exist as filed, open issues (`area:gui`/`area:batch`,
`tier:feature`). §5 of this very issue states as a hard rule: *"Every response
ends in a `REPLAN:` comment here."* The one comment posted does not end in a
`REPLAN:` comment — it explicitly declines to edit the issue body. So the
issue is currently in a state its own governance rules forbid: filed children
exist, `requires_features` doesn't list them, `planned_features` isn't empty,
and the DoD checkbox *"planned_features empty (each resolved to a filed issue
or descoped)"* is unmet with no waiver recorded. A scheduler reading the issue
body alone (not the comment) would believe no PF has been filed yet and could
re-file duplicates, or would miss that six live issues already carry this
capstone's real scope and estimates.
**Recommendation:** post the `REPLAN:` comment the issue's own protocol
requires — resolve `requires_features` to `[536, 537, 538, 539, 540, 541]`,
empty `planned_features`, regenerate the mermaid graph with real issue
numbers — before this issue is used to schedule anything.

**2. [HIGH] A live, unresolved conflict with the project's own strategic review is sitting unaddressed in the plan.** #508 (the August 2026 product/direction review, same author, overlapping dates) explicitly recommends **cutting PF-4** (APNG/GIF animated capture) on cost grounds, and classifies CAP-24 overall as "Keep-strategic (cheap slice now, rest gated)" rather than fully funded. The comment on #505 itself acknowledges this: *"#508 disposition pending: the product review recommends cutting PF-4 (≈9–14 mw realistic without it). #539 and #541 both carry the recommendation; the adjudicating REPLAN on this issue has not happened yet."* Yet issue #505's own body still lists PF-4 as required (§2 table: "What breaks in §1 if removed: Step 1's animation gone"), still budgets 2–3 mw for it, and §4's AC-5 (`AnimationCaptureTest`) is still a system-level acceptance criterion with no caveat. A team funding "CAP-24 as filed" today would build the exact component the project's own product review just recommended dropping, with the contradiction visible only by reading a linked issue's comment thread, not the plan itself.
**Recommendation:** this is a `REPLAN` trigger per §5 ("A planned feature is filed → REPLAN..."), and belongs in the same REPLAN as finding 1 — either accept the cut and restate §1 step 1 / §2 / AC-5 for four artifact kinds instead of five, or explicitly overrule #508's recommendation with a stated reason. Leaving both readings live in different documents is the failure mode this issue's own §5 exists to prevent.

**3. [MED] AC-2's cross-platform byte-identity claim is gameable at the Definition-of-Done level, even though the issue itself is aware of the risk.** §3 risk 1 correctly names this "the known-hard part," and KC-24-1 correctly gates it to a cheap 2–3 mw demo slice before the rest is funded — that discipline is sound. But the Completion Criteria checklist only requires *"KC-24-1's determinism measurement recorded from the demo slice before PF-2..PF-6 are funded"* — recorded, not passed. §5's re-planning protocol allows re-scoping AC-2 "never silently weaken it" via REPLAN if the text-metrics approach fails on any platform, but nothing in the DoD enforces that the REPLAN actually happens before PF-2..PF-6 proceed; a negative KC-24-1 result could be "recorded" in a comment and PF-2..PF-6 funded anyway without the required re-scoping REPLAN ever landing, exactly mirroring the process gap in finding 1.
**Recommendation:** change the DoD line to require KC-24-1's measurement to be **passing** (or a REPLAN re-scoping AC-2 to be **filed and merged**) before PF-2..PF-6 funding, not merely "recorded."

**4. [MED] PF-2's LaTeX-in-CI dependency is not priced or scoped as a CI-infrastructure change, and doesn't get the same dev-time-only carve-out KC-24-3 gives WaveDrom.** §1 step 2 requires the in-tree LaTeX sample document to "build clean in CI," and AC-1 spans this. Nowhere in the repo today (README's "Building from source," the CI description, or ARCHITECTURE.md) is there any TeX toolchain — this is a wholly new CI dependency, materially heavier than the single WaveDrom renderer pin that KC-24-3 explicitly restricts to dev-time-only to protect the "no Node dependency in the required matrix" guarantee (§8 exclusion 7, cited from #498). The issue prices PF-2 at "2–3 mw" with no separate line for standing up and maintaining a LaTeX build lane across (implicitly) the same three-platform CI matrix AC-2 already strains, and there's no equivalent kill criterion protecting the required CI matrix from a comparable dependency creep on the LaTeX side.
**Recommendation:** either state explicitly that TeX Live (or a minimal subset) becomes a new required CI dependency and price the maintenance cost, or add a kill-criterion-style guard analogous to KC-24-3 bounding how heavy that dependency may become.

**5. [MED] PF-4's core technical premise (pure-Java, deterministic APNG/GIF encoding) is asserted, not evidenced.** OQ-4 rules out MP4 because "native encoders violate the pure-Java/single-jar constraint," which is a real and correctly-applied project rule — but the issue never establishes that a suitable pure-Java, GPLv3-compatible, *deterministic-frame* APNG/GIF encoder exists, unlike PF-1's SVG path, which explicitly rode on a completed library evaluation (#154, JFreeSVG, GPLv3, evaluated and adopted). No comparable library-survey issue is cited or proposed for the animation encoder. Given this is already the component under live cost pressure (finding 2), an unresolved feasibility/licensing question for its core dependency is a real risk to the 2–3 mw estimate, not a formality.
**Recommendation:** either cite the intended encoder library and its license compatibility, or file the PF-4-analogue of #154 before funding PF-4 (which, per finding 2, is itself contested).

**6. [LOW, note only] The evidence citations are unusually solid.** Every file:line reference (`JLSStart.java:764-765`, `:820-821`, `Theme.java` at 162 lines) checks out exactly at the pinned `evidence_commit`, and the `grep` transcripts in "Background" (0 tikz/circuitikz hits, 0 wavedrom/wavejson hits in src/test, 2 survey-only hits in docs) reproduce exactly when run against that commit (the only reason HEAD shows more `tikz` hits today is the review-fleet's own `issue-reviews/*.visionary.md` files, which postdate the pin). This is a well-grounded plan on the "what exists today" axis — commend and move on.

**7. [LOW, note only] AC-4's falsification requirement is a genuinely good, gameproof-conscious acceptance criterion.** Requiring a recorded "scratch element with no print symbol fails the ratchet" red run, rather than trusting a green palette sweep alone, closes the obvious hole where a totality test never actually exercises its own failure path. No concerns here.

## What's solid (one line each)

- The five-step §1 outcome walkthrough is concrete, observable, and each step maps cleanly to a named test in §4 — good sufficiency argument structure.
- Cost banding (12–19 mw standalone vs. a costed 2–3 mw demo slice) with an explicit "marginal, co-funded" caveat on PF-5 is honest about what is and isn't shared cost.
- The theming-seam-sharing note with CAP-19 (§3 risk 2) and the "one registry-keyed mapping, two renderings" constraint is a sensible anti-fork guard, correctly scoped as a rides-along open question rather than a blocking one.

## Bottom line

The plan's shape (feature decomposition, sufficiency table, acceptance
criteria, kill criteria) is competently built, and its factual claims about
the current codebase check out precisely against the pinned commit. But the
issue is not currently in a self-consistent state: its own machine-readable
tracking data contradicts its own comment, its own §5 protocol ("every
response ends in a REPLAN") has already been violated once, and a cost
component it still budgets and still tests for (PF-4) is the subject of an
unresolved kill recommendation from the project's own strategic review. None
of that requires new engineering — it requires the REPLAN this issue's own
rules already demand — but until it happens, funding "CAP-24 as filed" means
funding a plan that disagrees with itself about what it contains.
