# Issue #381: TASK-0030: a dark background is usable, a scaled display is verified, and a first run offers a next move — the residual of #76 and #73
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue's technical content (Theme roles, CIE76 delta-E bar, sample
loading, ratchet test design) is well-specified and its factual claims
against the codebase check out. But the issue as filed duplicates work
already owned elsewhere in the corpus, and — critically — the issue's own
single comment (posted the same day as this review, 2026-08-08) already
concedes this and strikes four of the five workstreams. The body was never
edited to match. Anyone reading only the body (which is most of it — the
comment is easy to miss and is not reflected in title, labels, or the
`## Method` / `## Completion Criteria` sections it claims to supersede)
will duplicate work that already has better-specified, lower-numbered
owners. That is the central defect and it is severe enough to block
picking this issue up as written.

## Findings, most severe first

**1. The issue duplicates its own dependency graph's sibling tasks — and this was true at filing time, not just in hindsight.**
The body's machine block declares `part_of_feature: 355` (FEAT-011). But
#355 itself — filed 2026-08-02, *one day before* #381 — already
decomposes its "TASK-0030" line item as "planned, not filed" with the
same four-part scope this issue restates almost verbatim. Separately, and
more damningly, #76 (tier:feature, "closes on items 1 and 2" per this
issue's own §12) already lists a **filed, numbered sub-issue #289**
("Dark mode: FlatDarkLaf plus a dark Theme variant... currently
impossible because ~113 draw sites hardcode black") with
`part_of_feature: 76`, filed 2026-08-02 — again, before #381. #289
duplicates §7.10's role-totality formalism, the CIE76 bar, the
anti-vacuity allowlist clause, and the DEFAULT-pixel-unchanged claim
almost word for word. #73 (tier:feature, "closes on items 3 and 4")
independently lists the empty-state panel, samples, and README pass as
its own `planned_tasks`. So at the moment #381 was filed, its four
visual/onboarding workstreams already had a live or planned home under
the very issues (#76, #73, #355) it claims as parents. This is not a
future risk the issue failed to anticipate — it is a filing-time
collision the issue's own "rule 6, supersession check" language should
have caught and did not.
- *Evidence:* #289 body, `part_of_feature: 76`, created `2026-08-02T02:00:13Z`; #381 created `2026-08-03T14:20:12Z`. #355 body, `planned_tasks` entry "TASK-0030... Not filed", created `2026-08-02T22:08:04Z`.
- *Recommendation:* Do not execute §8 of #381 as written. Follow the issue's own comment (finding #2 below) and close or narrow the body to match.

**2. The issue's own comment supersedes §8/§13/§14 of the body, but the body was never edited — the document now contradicts itself.**
The single comment on #381 (posted `2026-08-08T17:46:26Z`, same author,
`author_association: OWNER`) opens: *"SCOPE NARROWED — four of this
task's five workstreams are owned elsewhere... This comment supersedes
§8 (Method), §13 (Conclusion) and §14 (Completion Criteria) of the
body."* It then reassigns the dark-mode sweep to #289, the welcome
panel/samples to #550/#548 (via #770/#771/#764/#766/#768), the README
pass to #545/#866 (via #760/#762), and the icon/chrome cleanup to
#287/#286 — all cited as lower-numbered or better-specified. Yet the
issue body's `## 8. Method / Experimental Design` checklist and `## 14.
Completion Criteria` still list all fourteen original steps unmarked,
with no strikethrough, no `WAIVED:` annotation, and no edit. A
contributor who reads the body top-to-bottom (the normal way to consume
an issue) has no signal to stop before attempting the full original
scope. This is the single most important defect: **the issue, as a
document, is self-contradictory**, and the contradiction is resolvable
only by reading a comment that the body gives no pointer to.
- *Recommendation:* Edit the body directly — strike or replace §8/§13/§14 per the comment's table, or close #381 and let the two remaining items (scaling matrix, usability trial) be refiled as a right-sized task. Do not leave a superseding comment as the only record of current scope.

**3. Even the two items the comment leaves in #381 have unresolved ownership.**
The comment's own "Open question, flagged not decided" asks whether
#73 §4's usability trial (n=5) stays in #381 or moves to #511 (CAP-27),
and says explicitly *"Resolve by `REPLAN:` on #511. Until then it stays
here."* No such REPLAN is visible in the fetched history, so as of this
review the trial's ownership is still ambiguous — meaning even the
"genuine, unduplicated residual" the comment claims for #381 (the
scaling matrix + the trial) has one item whose ownership is openly
unsettled by the maintainer's own admission.
- *Recommendation:* Resolve the REPLAN before treating #381's remaining scope as stable; otherwise a second duplicate PR is a live risk.

**4. `blocked_by: []` is asserted but at least one real dependency is missing even under the narrowed scope.**
The body's Related Work says of #162 ("open") — "depends on. The display
substrate the empty-state and scaling tests run under" — yet the
machine block declares `blocked_by: []`. Under the *original* scope this
is at minimum an internal inconsistency (§12 says "depends on," the
machine block says nothing blocks it). Under the *narrowed* scope from
the comment, the scaling matrix explicitly needs the display substrate
(the comment even flags #586/#796-#798's headless-sway rig as "the
obvious vehicle... whoever picks this up should check the rig first"),
so the ambiguity survives narrowing. `blocked_by` should either name
#162 or the body should explain why it doesn't block.
- *Recommendation:* Either set `blocked_by: [162]` or add a sentence explaining why #162's open status doesn't gate this issue's scaling-matrix work.

**5. The Definition of Done is gameable in at least one spot: the sweep's "anti-vacuity" allowlist.**
§7.10 states the ratchet must satisfy `L \ A = ∅ ∧ A ≠ ∅` (some
allowlist entries are required, else the check is deemed vacuous). This
is a good instinct, but it creates a perverse incentive: a contributor
under deadline pressure can satisfy both the emptiness and non-emptiness
conditions by allowlisting one or two genuinely hard cases and calling
it done — the checklist item "every entry carries a written reason"
(§14) is the only guard against a lazy, technically-compliant allowlist
that's actually 40 entries wide with boilerplate reasons. Nothing in the
Method or Completion Criteria bounds the allowlist's *size*, only that
it's non-empty and non-total. A reviewer must manually judge whether
each reason is substantive — that's a code-review burden the acceptance
criteria don't make explicit.
- *Recommendation:* Add an upper bound or a review gate ("each allowlist entry requires a second reviewer's sign-off in the PR description") so "reasoned" doesn't collapse into "rubber-stamped."

**6. The n=5 usability trial is explicitly not a gate anywhere, but §14 still lists it as a checked completion item.**
§9 says "The usability trial's outcomes are recorded as prose... a green
bar is not a user." §11 says "Its findings are directional." Good self-
awareness — but §14's checklist item is "#73 §4's usability trial run,
outcomes recorded in the PR, and the PR says plainly that n=5 is
directional" — a checkbox that can be satisfied by running the trial and
recording *any* outcome, including a clear failure, and just writing
"n=5, directional" next to it. There is no failure criterion tied to the
trial (contrast with §10's falsification criteria, none of which
mention the trial). A trial that shows 1/5 subjects succeed is exactly
as checkbox-complete as one showing 5/5.
- *Recommendation:* Either drop the trial from the mechanical DoD (make it a recorded observation only, as #73's own IC1 does with an explicit ≥4/5 bound) or import #73's actual success threshold (≥4/5 within ten minutes) into #381's own falsification criteria.

**7. Scope-creep: this is a `tier: task`, but its own comment calls it "a feature's worth of work wearing a task label."**
The original checklist spans a 96-site color-literal sweep with a
pixel-identity regression proof, a new palette variant with a formal
accessibility proof, a new Swing panel with action-identity tests, 3-5
authored circuits with golden simulation runs, README authorship
including a competitive-positioning paragraph, a manual cross-platform/
cross-scale-factor/cross-installer QA matrix, an icon redraw, a chrome
cleanup, and a human-subjects usability study. #355 — the very feature
this issue claims as parent — separately estimates its "TASK-0030" line
at "2 wk" against a feature-level band of "6-10 mw" and flags its own
row-sum-vs-band gap as unresolved (#355 §"Cost reconciliation"). #381's
actual checklist is closer to the multi-week end of that band, not 2 wk,
and the issue never states an estimate at all — leaving cost invisible
until someone tries to schedule it.
- *This finding is largely mooted if finding #2's narrowing is applied* (the remaining scope — scaling matrix + trial — is plausibly task-sized), but as filed and unedited, the body still reads as the full bundle.

**8. Minor: the pinned-commit reproducibility mechanism is fragile in a way the issue anticipates but doesn't fully protect against.**
§ Observations opens with `git diff --quiet 2d0ca9d HEAD -- src test
pom.xml .github/workflows && echo IDENTICAL` as a freshness gate. Re-run
against the current checkout: it now prints **DIFFERS** (unrelated
element-registry changes have landed in `src/jls/elem/`,
`src/jls/hdl/HdlExporter.java`, and several test files were removed —
none of which touch `Theme.java`, `UserPrefs.java`, or
`src/jls/edit/*Renderer.java`, so the issue's specific numeric claims
[110/113/96, all independently re-verified below] still hold). Rule 6
("re-verify O1-O4 at pickup... citations re-derived if HEAD had moved")
is the documented mitigation, and it's a sound instinct — but the
`IDENTICAL` framing invites a false sense that the whole evidence base
is either "still valid" or "invalid," when in practice only a targeted
re-grep is needed. Not a blocking defect, just a device that will
generate false alarms for a careless reader who runs the literal command
from the issue and stops there.

## What checks out (solid, no action needed)

- **O1-O4 factual claims all independently reproduce on the current tree**: `grep -ro 'Color\.black\|Color\.BLACK' src/jls/edit/` → 110; same over `src/` → 113; renderer-literal count over `src/jls/edit/*Renderer.java` → 96, with `RegisterRenderer.java` highest at 17. `resources/` contains only `help/` and `packaging/`, no `samples/`. `README.md` has exactly one image reference (the Scorecard badge). `UserPrefs.java:32-38` has exactly the four keys claimed.
- **The CIE76/delta-E accessibility bar is real and matches `ThemeTest.java`**: `DISTINGUISHABLE = 25.0` at `test/jls/ThemeTest.java:34`, with the CIE76 machinery present. P6's 25-delta-E claim is not invented.
- **The formalism in §7.10 (theta totality/injectivity, the delta-E universal quantifier, the ratchet's set-difference condition) is unusually precise for an issue body** and gives an implementer little room to misinterpret "must hold" — a genuine strength if the issue is ever executed as scoped.
- **§7.7's migration story (unknown theme name falls back to `DEFAULT`, never throws) is the right call** given `UserPrefs`'s existing degrade-to-in-memory design (`src/jls/UserPrefs.java:14-19`).
- **The `#130` seed-directory citation (classpath-only sample loading, never `user.dir`) is a real, verifiable prior decision** (`test/jls/SeedDirectoryTest.java` exists) and correctly constrains where a sample loader may read from.

## Bottom line

Technically the issue is well-built. Organizationally it is not
currently safe to execute: it was filed a day after a sibling issue
(#289) had already claimed the largest single piece of its scope, its
feature parent (#355) already listed the same work as merely "planned,"
and the issue's own most recent comment says outright that four of five
workstreams belong elsewhere — yet the body itself was never edited to
retract them. Before anyone starts work here, the body needs to be
brought into agreement with its own supersession comment (or the issue
closed in favor of the successors it names), the trial's home resolved
against #511, and `blocked_by` corrected for #162.
