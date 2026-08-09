# Issue #521: CAP-37: the editor stops losing switchers on ergonomics — a parity catalog built from the incumbents' own top complaints, closed item by item with each fix pinned by a harness test
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

CAP-37 is a capstone that bundles a documentation gate (PF-1, the parity
catalog) with four code-touching features (PF-2..5, selection ergonomics,
findability, message quality, small-parity items) scored against it. The
catalog idea is sound and the scope boundary against `SimpleEditor` is
correctly stated. But the issue's own dependency graph is already broken as
of today, its acceptance criteria have gameable or unverifiable clauses, and
the bulk of the funded scope is silently contingent on a large, unstable,
in-flight refactor that the issue's cost band does not account for.

## Findings, most severe first

**1. `ordering_after` names a task that was closed as a duplicate the same day this issue was filed, and the staleness has already propagated to a child issue.**

The issue's YAML front matter reads:
```
ordering_after: ["#316 FEAT-008 / #84 (...)", "#441 TASK-0020 (headless interaction machine — parity fixes must be assertable without a display)"]
```
Issue #441 (TASK-0020) is `state: closed`, `state_reason: duplicate`, closed
2026-08-08T16:43:08Z — the same day as this review — with a comment
explaining it was merged into #84 ("Superseded by #84 ... Lower number wins,
so #84 survives and this one closes into it"). #521 still points at #441 as
a live prerequisite. Worse, the child feature #592 (filed under this
capstone as PF-1) repeats the identical stale reference: "Acceptance
vehicle: #91 (UI harness) and #441 TASK-0020 (headless interaction machine)
are what make the graded rows assertable." Anyone picking up #521 or #592
today has to know, from outside the issue text, to redirect to #84.
**Recommendation:** update `ordering_after` and #592's "Acceptance vehicle"
note to cite #84 (and note #441's absorption), or this capstone's tracked
graph misleads its own executors from day one.

**2. The bulk of the funded scope is hard-gated on an unstable, unfinished dependency that the cost band does not discount for.**

KC-37-1 states plainly: "Nothing in PF-2..5 lands inside `SimpleEditor` —
the decomposition boundary is a hard gate; if FEAT-008 stalls, this capstone
waits." PF-2 (3–4 mw) + PF-3 (1.5–2 mw) + PF-4 (2–3 mw) + PF-5 (2–4 mw) sum
to 8.5–13 of the stated 10–16 mw band — effectively the whole capstone
except the 1–1.5 mw catalog. The gating dependency, #316 (FEAT-008
RESIDUAL), is itself not close to done: `SimpleEditor.java` is measured at
5,852 lines in #84/#316's own evidence, up from ~4,119 across "five
re-baselines in July 2026," and `ARCHITECTURE.md` (line ~28-30 in this
checkout) still describes it as "~4k lines" — the architecture doc is
already stale against the file #521 depends on being decomposed. #316's own
critical path is TASK-0019 → TASK-0020 → integration criteria, further
`blocked_by: [317, 337]` at the feature level, with #84 (the task-tier
residual) carrying 17 comments and an active, still-open completion-criteria
checklist. There is no evidence in #521 that its 10-16 mw estimate accounts
for "wait for #316 to fully land" as a real, open-ended cost. **Recommendation:**
either state the band as "1-1.5 mw now, remainder unscheduled pending #316,"
or make #521 a `blocked_by` (not just `ordering_after`) dependent so its
tracker status reflects reality.

**3. The bsiever-fork issue references (#18, #4) will auto-link to the wrong issues in this repo, and the fix so far is a warning comment, not a correction.**

The issue body writes: "JLS additionally carries its own course-era
ergonomic debt (bsiever #18 compound selection; over-restrictive identifier
rules, bsiever #4)." GitHub autolinks bare `#N` to same-repo issues
regardless of a preceding qualifier word like "bsiever." In `anadon/JLS`,
issue #18 is "Undo system deep-copies the entire circuit on every change"
(closed, completed) and #4 is an unrelated closed PR ("Collision
Performance") — neither is the bsiever-fork compound-selection or
identifier-naming issue this capstone means to cite. This is not a
hypothetical risk: the issue's own coverage-verification comment already
caught it — "the bare `#18`/`#4` forms will auto-link to unrelated issues in
this repo... recorded here so a later reader does not chase the wrong
issues" — but the mitigation was to leave a warning in a *comment*, not to
fix the *issue body* (e.g. with `bsiever/jls#18` or code-fenced text that
GitHub won't autolink). AC-3 ("The bsiever-fork ergonomic issues (#18, #4)
are each closed or refused by name") and #592's AC-1/AC-4 both still carry
the same bare, autolinking form. **Recommendation:** edit the body text (not
just a later comment) to a non-autolinking citation form.

**4. The issue's own machine-readable block contradicts its own comment thread.**

Front matter: `planned_features: [PF-1 unfiled, PF-2 unfiled, PF-3 unfiled, PF-4 unfiled, PF-5 unfiled]`.
The issue's only comment (same day, "Feature-coverage verification —
CAP-37") reports all five filed: #592, #593, #594, #595, #596. The issue
body was never edited to match. Sibling capstone #316 explicitly treats its
own machine block as authoritative over prose ("Machine block, roster
table, and mermaid graph agree with reality at close" is a completion
criterion there); #521 has no such reconciliation, so a reader or tooling
pass trusting #521's front matter alone will conclude no PF has been filed
when in fact all five have. **Recommendation:** update the front matter's
`planned_features` line to the five issue numbers.

**5. AC-4's timed-task benchmark is under-specified to the point of being gameable.**

"AC-4: A timed editing task (build a 4-bit counter from scratch) is measured
before/after; the after is not slower." No sample size, no tester
consistency/blinding, no control for learning/practice effects between the
"before" and "after" measurement (the same tester doing the task twice will
almost certainly be faster the second time regardless of any ergonomics
change), no machine/environment normalization, and no margin — "not slower"
against a single anecdote can be satisfied by a deliberately sluggish
baseline run or dismissed as noise if it fails. #592 (PF-1, the catalog) is
assigned to record the baseline, but neither issue specifies a methodology
(repeated trials, different testers pre/post, or a statistical bar) that
would make the criterion resistant to being satisfied without any real
ergonomic improvement. **Recommendation:** specify trial count, tester
independence between before/after, and an explicit tolerance band.

**6. AC-5's "K9 stands" references an undefined external convention.**

"AC-5: K9 stands: no new default-visible complexity; startup and per-edit
cost ratchets hold." No file under `docs/`, `ARCHITECTURE.md`, `README.md`,
or anywhere else in this checkout defines "K9" (grep across the tree returns
nothing). A repo-wide issue search finds the exact phrase "K9 stands" used
in exactly one other capstone (#511) and nowhere else — meaning it is a
maintainer-side convention that has never been written into the tracked
corpus. As written, AC-5 cannot be verified by anyone without access to that
outside context, which fails this repo's own stated norm of citing
verifiable evidence. **Recommendation:** either link to wherever "K9" is
actually defined, or restate the ratchet rule in this issue's own words.

**7. AC-3's "closed or refused by name" is authority-underspecified across a repo boundary.**

anadon/JLS has no authority to close issues in bsiever's fork. AC-3 is
satisfiable two very different ways: (a) a JLS-side row/issue that cites the
bsiever items is closed or refused, or (b) someone actually acts on the
upstream bsiever issues (comment, PR, or close request there). Only (a) is
achievable from inside this repository, but the text doesn't say so, and a
verifier could reasonably read AC-3 as demanding the latter. **Recommendation:**
state explicitly that AC-3 is satisfied by JLS-side disposition of the
catalog rows, not by action on the upstream fork.

**8. Evidence chain leans on external trackers this repo cannot verify or keep current.**

The `evidence` field and PF-2/PF-3/PF-4/PF-5 descriptions cite specific
upstream issue numbers (Digital #882/#1308/#1129, Logisim-Evolution
#88/#1234, Issie's width-inference UX, LogicCircuit's bus ergonomics) that
live entirely outside this repository. That's consistent with how #510 (the
survey this issue cites as its evidence source) was built, so it isn't a
defect unique to #521, but it means AC-1's "every row cited" catalog is only
as good as those external numbers staying valid — upstream issues get
closed, renumbered, or resolved, and nothing in #521 or #592 commits to
re-verifying citations at execution time versus filing time (2026-08-04).

## What holds up

- The scope boundary against `SimpleEditor` (KC-37-1) is directionally
  correct and consistent with #84/#316's own stated hard boundary — new
  editor behavior belongs in decomposed collaborators, not the god class.
- The scope split with #514/#570 (CAP-30's Digital-wishlist headline
  features — dark mode, live subcircuit dive, rebindable keys) is clean and
  reciprocally acknowledged: #570's own Notes section flags the same overlap
  and defers to #521 for reconciliation, so there's no double-funding here.
- PF-1 (the catalog, 1-1.5 mw, non-code) is a reasonable low-risk demo slice
  that is genuinely unblocked by the #316 stall, unlike PF-2..5.
