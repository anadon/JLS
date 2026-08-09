# Issue #188: Deterministic native installers: per-format byte-reproducibility program
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what this issue actually is

#188 is a `tier:feature` tracking issue coordinating three sub-tasks (#189
deb/rpm/AppImage, #190 msi, #191 dmg) toward a disjunctive goal per
installer format: either byte-identical across two builds (hard CI gate)
or a documented, attestation-covered bounded residual. #189 and #190 are
verifiably closed (`state: closed`, `state_reason: completed`) and their
technical claims check out against the tree (see Verified section). #191
is genuinely open. No source code is proposed by this issue itself — it
is pure coordination/documentation scope, consistent with its own
"Out of scope" section.

## Findings, most severe first

### 1. [High] The issue's own machine-readable state is already stale relative to facts recorded in its own comment thread, and nothing has re-synced it

The issue body's `related` field lists only `[33, 44, 82, 184, 185]` and
the dependency graph names only `#189/#190/#191`. But the two most recent
comments on *this issue* (2026-08-04, ids 5175921036 and 5176154923) are
entirely about a fourth and fifth issue — **#338** (a sibling
`tier:feature` tracker, "FEAT-010 (RESIDUAL)") and **#471** (a task under
#338) — and reach an explicitly unresolved conclusion:

> "**#191 vs #471 is a live double-ownership of one outcome and this pass
> cannot resolve it** — #191 is this issue's open child but sits outside
> cluster G, and #471 is outside it too. Escalated with both numbers
> named."

and separately:

> "**#471 declares itself the closer of this issue** (*'closes'* edge to
> #188 in its graph...)."

I independently confirmed #471's body states `part_of_feature: 338` and
draws a `-.->|closes| I188` edge, i.e. a second issue, not tracked
anywhere in #188's own machine block, claims authority to close #188. Yet
#188's body was last restructured (REPLAN, comment id 5154304453) on
2026-08-02 — *before* both of these comments — and has not been REPLANed
since to absorb this discovery. The issue's own Completion Criteria
require "Machine block, roster table, and mermaid graph agree with
reality at close" — by the issue's own comment record, that is already
false today, before anyone has even attempted to close it.

**Recommendation:** before any further work lands under #188, force a
REPLAN comment that either (a) adds #338/#471 to `related` and states
explicitly whether #191 or #471 executes the dmg verdict, or (b) gets a
maintainer ruling on which of #188/#338 is the authoritative closer of
the reproducibility program. Proceeding with #191 without this risks a
contributor's approved PR being immediately obsoleted by #471, or vice
versa.

### 2. [High] Two independently-filed "feature" trackers (#188 and #338) both claim ownership of the same outcome, and one asserts it supersedes the other

#338's Background section states outright: "**#188** — Deterministic
native installers: per-format byte-reproducibility program. **The
tracking issue this feature is the plan for.**" That is a strong
subsumption claim. #188's own comment thread responds "No merge" (pass 1)
but the correction in pass 2 leaves the disposition of #191 vs #471
"cannot resolve" rather than actually rejecting #338's claim. This is not
a hypothetical risk — #471's task body is fully fleshed out (a
`ReproducibilityScopeTest`, a payload-digest gate for msi, a koly-field
gate for dmg) and duplicates exactly the "give msi/dmg a verdict" work
#188 assigns to #191. If both proceed, the more likely outcomes are
wasted duplicate engineering effort or two different CI jobs asserting
conflicting things about the same artifact.

**Recommendation:** this is a process/governance defect, not something
a code PR against #188 can fix. Flag for maintainer decision before
picking up #191's remaining work.

### 3. [Medium] The acceptance criterion is a disjunction that, once satisfied, has no mechanism to detect regression — a real instance of this was found (by a sibling issue, not #188)

#188 §4 invariant 2 codifies: "'reproducible' is claimed only behind a
passing CI double-build gate; anything less is stated as an enumerated
residual." For msi (#190, closed), the "byte-identical cabinet payload"
half of the claim was established by **one** CI run
(29763534341) and then the double-build gate was **intentionally
removed** ([ci.yml:965-975](https://github.com/anadon/JLS/blob/29afb26/.github/workflows/ci.yml#L965-L975),
confirmed present at current HEAD). #471 — filed against a different
feature (#338) — independently discovered this is a real gap:

> "`windows-installer-msi` builds once... Its only assertion is existence
> and non-triviality... nothing anywhere checks that its cabinet payload
> — which `ci.yml`'s own comment says **is** byte-identical — stays that
> way. A regression in the payload would ship silently."

That #188 needed a *different* tracking issue to notice this about its
own closed child #190 is itself evidence the honesty-gate invariant, as
currently scoped in #188, only covers the initial measurement, not
ongoing drift. #188's DoD never asks "is there a regression-resistant
check," only "is there a documented verdict" — so the DoD can be
satisfied by a one-time measurement that then silently rots.

**Recommendation:** #188 should either absorb a "payload-gate stays
gated" invariant into its own §4, or explicitly hand that obligation to
#338/#471 in its `related`/dependency fields (currently it does neither).

### 4. [Medium] `evidence_commit: 29afb26` is 19 commits stale relative to HEAD, with no re-verification since

`git rev-list --count 29afb26..HEAD` = 19; the only commit touching
files this issue cites (`ci.yml`) since then is a dependabot action-pin
bump (`e02758a`), which I confirmed does not touch the reproducibility
job bodies — so nothing is *currently* broken by the drift. But the
issue's own sibling documents (#191 "re-derive on drift"; #338's Link
pass explicitly re-derives citations at pickup) treat evidence-commit
staleness as something to actively check, not assume harmless. #188
itself carries no equivalent instruction and no comment since 2026-08-04
re-confirms the citations still resolve. This is a minor issue today but
becomes a real risk the longer the issue sits open without a check-in,
since every one of its ~30 permalinks is commit-pinned prose that a
future reader (human or agent) may trust without re-verifying.

**Recommendation:** add a one-line note to #188 instructing whoever picks
it up next to re-run the permalink/line-number check against current HEAD
before acting on any citation.

### 5. [Low] A DoD checkbox that cannot fail as worded

DoD item: "Every entry in `requires_tasks` closed as landed **or** open
with a live lane." As written this is checked `[x]` — but "closed" or
"open" exhausts the state space of a GitHub issue; the checkbox is true
by definition regardless of whether #191 ever lands. It functions as
narrative, not as a gate. Compare to the sibling `#338`'s much sharper
IC-1/IC-2 ("parse the workflow, count experimental legs, observe 0"),
which is falsifiable. This isn't a blocking defect, but it's worth
flagging since #188 elsewhere (§4 invariant 2) explicitly prizes falsifiable
gates over narrative ones and doesn't apply its own standard to its own
checklist.

**Recommendation:** reword to something like "#189 and #190 closed
completed; #191 either closed completed or explicitly REPLANed with a
recorded disposition" — a predicate that can actually be false.

### 6. [Low] `planned_tasks`' one entry has no trigger condition that guarantees it is ever revisited

"Clean-VM install/.jls-associate/upgrade/uninstall verification of a
normalized msi — successor obligation from #190's WAIVED DoD criterion;
file when a Windows VM rig exists." Open Question 2 in the body concedes
the alternative is descoping this "if the risk assessment in #190 §11 is
accepted as sufficient" but nobody has made that call. Because it "blocks
feature close-out only," and there's no owner or date, this can sit
indefinitely — a plausible outcome given the single-maintainer project
context stated in ARCHITECTURE.md. Not a defect in the issue's logic,
but a real feasibility/cost risk: #188 cannot honestly close until either
infra that doesn't exist yet (a Windows VM rig) materializes, or a
maintainer explicitly waives it.

**Recommendation:** set an explicit revisit trigger or default-to-waive
date rather than leaving it open-ended.

## What holds up (adversarial checks that did NOT find a problem)

- All `scripts/build-installer.sh` line citations I spot-checked
  (L91-131 SOURCE_DATE_EPOCH/clamp derivation, L169, L240-244, L369-388
  rpm macros, L463-505 dmg/msi normalization calls) match the actual file
  content at the cited ranges — the technical narrative is grounded in
  real code, not fabricated.
- #189 and #190 are verifiably `closed`/`completed` via the GitHub API,
  matching #188's claims about them.
- `docs/reproducibility.md`'s table (checked directly) matches the
  "Yes — gated by CI" / "No — see §5" framing #188 describes.
- The "Out of scope" boundary (container image → #184, installer
  creation → #82, BOM guard → #184 Finding C) is stated clearly and is
  consistent with what those referenced issues actually own, reducing
  (if not eliminating, per Finding 2) same-feature scope creep.
- No content/security/licensing hazard: the invariant "never changes
  installer contents, only embedded timestamps/identifiers" is stated
  and, per #190's closed record, was honored (ProductCode/UpgradeCode
  left untouched deliberately to avoid breaking Windows upgrade
  semantics).

## Bottom line

The engineering claims in #188 are well-grounded and its two closed
children hold up under inspection. The verdict is
**sound-with-concerns** rather than **needs-rework** because the
technical content is solid — the concerns are almost entirely about
process/governance: an unresolved, self-acknowledged ownership conflict
with #338/#471 that the issue's own machine block has not absorbed, and
an acceptance-criteria design (measure-once, gate-forever) that a sibling
issue already found wanting for exactly this issue's closed msi work.
Both should be resolved by a maintainer before further work is queued
under #188, to avoid duplicated or conflicting effort with #338/#471.
