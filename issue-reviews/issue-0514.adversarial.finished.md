# Issue #514: CAP-30: an outside developer's first PR merges within a week, and JLS retains external contributors — the developer base Digital's decline is stranding has somewhere to land
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what this issue is

A capstone (`tier:capstone`, CAP-30) built on the strategic survey #510: JLS should recruit
the contributors that the rival simulator "Digital" (hneemann) is losing to its own decline.
Six planned features (PF-1..6), six acceptance criteria, two kill criteria. The issue already
carries two dense self-adversarial-review comments and a third "REPLAN NEEDED" comment posted
the same day as this review (2026-08-08 17:44 and 18:24 UTC), so several defects below are
already known to the process — I flag which ones and add what those passes missed.

## Findings, most severe first

**1. Unresolved internal contradiction as of the latest comment: a required feature has no
referent.** The `planned_features` roster names `FEAT-C30-3` (via #571's `ordering_after`),
but no issue anywhere — open or closed — is `FEAT-C30-3`. The issue's own 18:24 comment says
so explicitly and proposes two candidate resolutions (absorbed into #570, or into #75) without
picking one. This is not a hypothetical risk I'm inferring — it is the tracker's live state at
review time: a required row in a capstone whose Definition-of-Done depends on "roster agrees
with reality" currently does not agree with reality. **Recommendation:** do not let this sit;
either issue owner or the next automated pass must land a `REPLAN:` comment naming the
resolution before any PF-5/PF-6 work is scheduled, since #571 orders against the missing number.

**2. The body conflates two different issues that share the number "#84," and GitHub will
autolink the wrong one.** In the Outcome/PF-5 bullet, "#84" means *Digital's* issue (`live
dive-into-subcircuit-during-simulation, their #84, open nine years`) — confirmed against #510
§5, which frames it the same way. But the `related` field of this same issue's machine block
reads `"#84/FEAT-008 #316"`, and comment 1 glosses that as *this repo's* issue #84 ("Decompose
SimpleEditor — residual: extract the 9-state mouse interaction machine as GoF State objects,"
verified: `anadon/JLS#84` is real, open, parented to #316, and has nothing to do with
subcircuit-dive). Both usages appear in the tracking apparatus around a single issue, and
GitHub always resolves a bare `#84` to `anadon/JLS#84` — never to Digital's. A reader who
clicks the PF-5 "their #84" link lands on a SimpleEditor refactor task, not the feature it's
citing. **Recommendation:** every cross-repo reference to Digital's tracker must be a full
URL or `hneemann/Digital#NNN`-style qualifier, never a bare `#NNN` — apply this project-wide,
not just here, since #510 has the same pattern for Digital's #1477/#1464/#1470.

**3. That same unqualified-cross-repo-number pattern is a forward-compatibility landmine, not
just a readability nit.** `#514`, via #510, cites Digital's `#1477`, `#1464`, `#1470` as bare
numbers. Today those don't collide with anything in `anadon/JLS` (issue numbering here is in
the 800s per the sibling review files in this directory), but this repo is creating issues at
a striking clip — #84 (2026-07-08) to #514 (2026-08-04) to #889 (2026-08-08, per the fleet
review already covering that number) is roughly 800 issues in a month. At that rate,
`anadon/JLS` reaches issue #1464 within weeks, at which point the un-qualified reference
silently starts resolving to a real, unrelated local issue instead of staying a dead link.
A landmine that detonates itself with no code change and no warning is worse than a bug that
needs a trigger. **Recommendation:** same fix as #2 — qualify every external-repo issue
reference; a repo-wide lint/grep for bare `#[0-9]{4,}` in bodies referencing "Digital" would
catch this class before it ships.

**4. The machine block's own `ordering_after` field contradicts the comment sitting next to
it.** `ordering_after: []   # PF-4 orders behind #223 API freeze; PF-5's features order behind
their own dependencies`. The structured field — the one any automation or future audit would
actually parse — asserts zero ordering constraints, while the trailing comment asserts two.
This is exactly the failure mode the project's own tooling seems built to prevent (see #212's
and #84's careful "Cycle check" sections that treat the YAML block as ground truth). A script
trusting the field sees no constraints; a human reading the comment sees real ones.
**Recommendation:** move the two real constraints into the field itself
(`ordering_after: ["#223", "PF-5-internal"]` or equivalent), or drop the comment — don't let
the two disagree.

**5. AC-5 was already shown unsatisfiable by the feature it "consumes," and the body was
never fixed — only patched by comment.** AC-5: *"The largest file in `jls.edit` is under
1,500 lines (consumes FEAT-008's outcome)."* Verified independently: `wc -l
src/jls/edit/SimpleEditor.java` → 5852 (matches the issue's own figure), and comment 2
(2026-08-08) already found `StateMachineDialog.java` at 1,929 lines with no issue anywhere
that owns bringing it under 1,500. #316/#84 (FEAT-008, the thing AC-5 says it "consumes") owns
only `SimpleEditor.java`. So AC-5 as literally worded would still read FAIL the day FEAT-008
closes cleanly, by 429 lines, in a file the survey never named as a contributor-repellent.
The comment proposes two fixes but the *issue body itself* — what anyone skimming without
reading all three comments will act on — still states the unsatisfiable version.
**Recommendation:** edit AC-5 in the body (option (a) from comment 2 — narrow to
"`SimpleEditor.java` under 1,500 lines, and no file in `jls.edit` newly exceeds 1,500 as a
result") rather than leaving the correction stranded in a comment thread.

**6. `related` cites a closed-as-duplicate issue, and the acceptance criterion built on it
(AC-6) rests on a demand gate whose retirement is asserted, not adjudicated in the open.**
`related` names "#399 TASK-0107 (external element jar)"; #399 was closed 2026-08-08 as
`duplicate_of: 212`, and AC-6 depends on #212, whose own Definition-of-Done still carries
*"Demand gate explicitly resolved … never built speculatively"* with a standing recommendation
of *hold*. Comment 2 asserts the gate is retired because "this capstone is the named demand,"
citing a BRIEF.md rule that maintainer-roadmap asks aren't subject to third-party demand
gates. That may well be the correct call, but it's a self-referential resolution — the
capstone that needs the gate open is also the thing declared to open it — recorded in a
comment on #514, not as a `REPLAN:` comment on #212 itself where #212's own DoD checklist
lives. **Recommendation:** land the gate-retirement decision on #212 directly (its own
"Demand gate explicitly resolved" checkbox), not only inferred from #514's comment thread,
so a reader of #212 alone doesn't see a stale "hold."

**7. Several acceptance criteria are gameable as worded.**
- AC-2 ("good-first-issue label holds ≥10 open items continuously for a quarter") counts
  labeled items, not completable ones — nothing stops satisfying it with ten issues that are
  technically small-scoped but require deep codebase familiarity, which given #84's own
  documented "16 places to touch to add an element" and the SimpleEditor tiptoe problem is a
  real risk in this specific codebase, not a generic worry.
- AC-4 ("Three external PRs merged … over a rolling quarter") doesn't define "external."
  The two 2026 data points this capstone's own evidence rests on (#4, #5, #187) include one
  PR (#187) explicitly disclosed as authored by "an autonomous AI coding agent (Claude Code)"
  operated by an account with `NONE` author association — verified via `pull_request_read`
  (both #4 and #187 confirmed `merged: false`, closed unmerged, supporting the "bounced both"
  claim). If AI-agent-authored PRs from freshly created accounts count toward AC-4's "three,"
  the metric can be satisfied without recruiting a single human developer — the stated goal.
- AC-6 ("One external element jar … by someone other than the maintainer … loads and runs")
  has no floor on who that someone is or why they built it; a jar built to order by an
  acquaintance for the sole purpose of ticking the box satisfies the letter while defeating
  the "real external demand" spirit that #212's own demand-gate language cares about.
  **Recommendation:** define "external" for AC-4 (a GitHub account with commit history
  predating this capstone and no financial/organizational tie to the maintainer) and make
  AC-6 require the requester to be independently identifiable (name, course, or org) per
  #212's own gate language, not just "loads and runs."

**8. PF-1's premise is only partly true against the current file.** The issue claims
CONTRIBUTING "gains a 10-line quickstart above the contract details" as if none exists.
`CONTRIBUTING.md` already opens with a "## Getting started" section (`mvn verify`, `java -jar
target/jls-*.jar`, four lines) before the denser "## Making changes" section that immediately
cites internal ledger references (#94, #95, `SealedHierarchyTest`, NullAway/JSpecify). The
real gap is that the *first* contributor-facing document already assumes familiarity with the
project's internal audit/ticket vocabulary two sections in, not that a quickstart is wholly
absent. AC-1's stronger claim — no bug/feature-request issue template exists — is correctly
verified (`.github/ISSUE_TEMPLATE/` holds only `capstone.md`, `feature.md`,
`scientific_task.md`, confirmed by directory listing) and is the real, sharper version of the
same point. **Recommendation:** state PF-1/CONTRIBUTING's problem precisely (jargon density
past line ~15, not absence) so the eventual PR is scoped correctly.

**9. `planned_features` in the *body* is stale in a way that misleads anyone who doesn't read
all three comments.** The body still reads `[PF-1 unfiled, PF-2 unfiled, PF-3 unfiled, PF-4
unfiled, PF-5 unfiled, PF-6 unfiled]`, even though five were filed the same day the issue was
created (comment 1, 2026-08-04) and the sixth (PF-3) was deliberately left unfiled by design
(owned by #316). A tool or reviewer that reads only the machine block — the thing meant to be
authoritative and grep-able — gets a materially wrong picture of the capstone's status.
**Recommendation:** this is a process gap broader than this one issue (edit-the-body-on-status-
change vs. append-only-comment), but at minimum this issue's machine block should be edited in
place now that the roster has stabilized, rather than left as a landing point for readers.

## What's solid (no rework needed)

- The core evidence — both 2026 external PR authors bounced unmerged (#4, #187, confirmed via
  `pull_request_read`), zero bug/feature-request template, a 5,852-line `SimpleEditor.java`
  (confirmed by `wc -l`) — checks out against the actual repository state, not just the
  survey's say-so.
- The kill criteria are well-formed: KC-30-1 (each PF-5 feature must justify itself on JLS
  merit, not poaching) and KC-30-2 (two quarters with zero external PRs falsifies the outreach
  premise and forces re-scope) are genuinely falsifiable, dated, and not gameable in the way
  several ACs above are.
- The dependency ordering (PF-4 behind the #223 API freeze; AC-6 gated on #212) is
  directionally correct even where the details need tightening (finding #6).
- The band estimate (8–14 mw) is internally consistent with the sum of the six PF ranges
  (0.5–1 + 1–2 + 0 (referenced) + 2–3 + 4–7 + 0.5–1 ≈ 8.5–14) — no padding or arithmetic slip.

## Verdict rationale

Three of the findings above (1, 4, 9) describe the tracker being in a state its own process
calls broken *right now*, not a hypothetical risk; finding 2/3 identifies a concrete misleading
cross-link plus a self-inflicted future collision that costs nothing to prevent (qualify the
reference) and a lot to discover later (silent wrong-issue linking). None of this invalidates
the strategic premise (#510's finding that Digital's decline is a real, time-limited opening is
independently well-evidenced), so `should-not-proceed` is too strong — but the issue is not
currently in a state where a contributor picking it up gets an accurate, self-consistent
picture without reading three follow-up comments and independently re-deriving what the body
says. That is the definition of needing rework before execution, not during it.
