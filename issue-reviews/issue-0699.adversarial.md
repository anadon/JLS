# Issue #699: TASK-C525-3: Gradescope spec drift turns a dedicated lane red, never a live course — plus the template README executed as CI doc-test steps
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue as filed

Sub-task of FEAT-C21-2 (#525), which itself serves CAP-21 (#502, the four-platform
autograder kit). `ordering_after: ["TASK-C525-1"]` (#694, the Gradescope template +
adapter, itself unimplemented). Four checkbox ACs: (1) a dedicated CI lane validating
`results.json` against "the pinned spec," failing by name on mismatch; (2) the template
README executed as CI doc-tests, clean checkout to graded fixture, no manual step; (3)
an instrumented build asserting no interactive session opens; (4) failure output that
names whether the fault is JLS-side, adapter-side, or platform-side. Confirmed against
the repo: nothing in the tree yet mentions Gradescope — only `examples/autograde/autograde.py`
exists, which is the literal artifact CAP-21's own background section calls out as "the
gap's measure." So #699 is pure spec at this point, several layers deep in an unbuilt
chain (#699 → #694 → #524, the not-yet-frozen CLI contract).

## Findings

### 1. [High] The title's core claim — "spec drift turns a lane red" — is not achievable by the described mechanism
The Outcome says the lane "validates emitted `results.json` against the pinned
documented spec." A validator checked against a **static, committed copy** of
Gradescope's schema can only ever catch two things: (a) the adapter's own output
regressing against that pinned copy, or (b) a human manually re-pinning after noticing
Gradescope changed something. It structurally **cannot** detect that Gradescope's real,
live spec has drifted away from the pinned copy — there is no described mechanism (no
periodic re-fetch, no diff against a live sandbox, nothing) that would ever turn the
lane red *because Gradescope changed something*, which is exactly what the title
promises ("Gradescope spec drift turns a dedicated lane red, never a live course"). As
written, the lane silently stays green after real vendor drift until a maintainer
notices independently (e.g., a live course breaks) and updates the pin — the one outcome
the issue exists to prevent. KC-21-3 in the parent capstone (#502) only requires
"dropping the adapter if it needs to track an undocumented interface"; it does not
establish a live-drift-detection mechanism either, so the gap traces up to CAP-21 itself.
**Recommendation:** either (a) rename/reframe the AC to what it actually does — "catches
adapter regressions against our last-verified copy of the spec" — or (b) add a concrete
mechanism for keeping the pin current (e.g., a scheduled job that fetches Gradescope's
published spec/changelog and diffs it, failing loudly on any change, mirroring the
`gui-wayland` nightly-cron pattern already in `.github/workflows/ci.yml:12`/`:353`).
Without one of these, "fails by name when shapes disagree" is describing a regression
test, not a drift detector.

### 2. [High] The parent capstone names concrete oracle tests this issue never commits to
CAP-21 (#502) §4 names `TemplateDocTest` as the exact spanning test for AC-5 ("Each
template README executes end-to-end as scripted steps in CI") and `RecordedArtifactOnlyTest`
for AC-4 ("no adapter opens an interactive session"). #699's ACs cite "(CAP-21 AC-5)" and
"(CAP-21 AC-4)" by number but never name these classes or commit to producing them —
unlike sibling tasks in the same feature, which *do* name concrete oracles
(`GradescopeCorpusTest` in #697, `CliContractConformanceTest` in #687/#724). A closer
of #699 could satisfy the checkbox prose with an ad hoc shell script or a one-off
GitHub Action step that never surfaces as `TemplateDocTest` or `RecordedArtifactOnlyTest`
anywhere, leaving CAP-21's own Definition of Done ("Every criterion in §4 verified
end-to-end at a named commit; command and output recorded") unsatisfiable without a
follow-up rename/rewire. **Recommendation:** name the test classes explicitly in the ACs,
as the sibling tasks do, or explain in the issue why this task deliberately doesn't own
them (e.g., if `RecordedArtifactOnlyTest` is meant to be a later cross-adapter task and
#699 only needs to satisfy its Gradescope slice under a different name).

### 3. [Medium] "Named error" is used for two different, unreconciled things
The Outcome paragraph says the lane "fails by name when shapes disagree" (bullet 1
elaborates: "fails with a named error on mismatch"). AC bullet 4 separately requires the
failure output to classify the fault as JLS-side / adapter-side / platform-side. Nothing
ties these together — is the "name" in bullet 1 the JLS/adapter/platform classification
from bullet 4, or a distinct exception-class name (e.g. `ResultsJsonSchemaMismatchException`)?
As written, an implementer could satisfy bullet 1 with a bare named exception and treat
bullet 4 as a separate, weaker "which CI job failed" signal (job names alone don't
distinguish JLS-contract violations from adapter mapping bugs — those can both manifest
as the same downstream `results.json` shape failure). **Recommendation:** merge these
into one AC or explicitly state the relationship (e.g., "the named error's category *is*
the JLS/adapter/platform classification").

### 4. [Medium] "No interactive session opened" instrumentation has no described mechanism and no cited precedent
The repo already has a rigorous precedent for exactly this class of claim:
`HeadlessCoreRatchetTest` (`ARCHITECTURE.md:55-56`) enforces that `jls.sim` imports no
AWT/Swing/`jls.edit` at the bytecode/import level, in-process. #699's AC 3 asks for the
analogous guarantee across a Docker container running an external adapter — a materially
harder claim to instrument (there's no single classloader to scan; "interactive session"
for a containerized batch process could mean anything from an AWT `Frame` to a lingering
subprocess waiting on stdin). The issue neither names a mechanism nor references the
existing ratchet pattern it's clearly modeled on. A weak but literally-compliant
implementation — e.g., asserting `-Djava.awt.headless=true` was passed, or grepping the
adapter's own source for `Toolkit`/`JFrame` — would pass the checkbox while proving
nothing about what actually happens at runtime inside the container. **Recommendation:**
specify the instrumentation approach (e.g., "assert the container's JLS invocation is
`-b` batch mode only, verified from the recorded process invocation log, not a source
grep") before this is picked up.

### 5. [Medium] Four materially different deliverables bundled into one 0.5–1 mw task invites a shallow pass
The task packs: (a) a `results.json` schema/contract validator, (b) an end-to-end README
doc-test harness (clean checkout → graded fixture), (c) session-instrumentation across a
container boundary, and (d) a three-way fault classifier — at `band_mw: 0.5-1`, less than
the template+adapter task it depends on (#694, `band_mw: 1-1.5`) and comparable to the
single-purpose corpus test (#697, also `0.5-1`, but scoped to one property: wall-time +
determinism). Bundling four distinct pieces of infrastructure at a lighter budget than
building the thing they test creates real pressure to under-build one or more of them
(most likely (c) and (d), the two with no named oracle per Finding 2) to close the issue
on time. **Recommendation:** split into two tasks (schema-drift lane + doc-test lane) or
explicitly de-scope (c)/(d) to a follow-up if the budget is meant to stay at 0.5–1 mw.

### 6. [Low] Deep, currently-unbuilt dependency chain, understated in the issue
`ordering_after: ["TASK-C525-1"]` (#694) is correct as far as it goes, but #694 itself is
unimplemented and depends transitively on the frozen CLI contract (#524, PF-1, still
open) and on CAP-21's Open Question 1 (capstone composition, explicitly "undecided at
filing"). Confirmed by repo grep: zero occurrences of Gradescope/PrairieLearn/nbgrader/
Classroom in the tree outside `examples/autograde/autograde.py` and the issue trackers
themselves. None of this makes #699 wrong, but the issue gives no signal that picking it
up today is pure planning motion — worth a note so a contributor doesn't start on the CI
lane before the adapter it validates exists.

### 7. [Low] No note on sourcing/licensing the vendored "pinned spec"
"Validates against the pinned documented spec" implies a committed copy of Gradescope
(Turnitin, proprietary) schema/documentation living in this GPLv3-or-later tree. The
project is otherwise careful about license-linking hazards (README/ARCHITECTURE.md
explicitly discuss GPL/EPL-2.0 interaction for ELK, and the Windows-signing and rpm/deb
signing custody rationale in README show the project documents this kind of boundary
explicitly). A one-line note that the pin is a minimal interface description (not
copyrightable expression) would preempt an easy objection; its absence isn't a blocker
but is a gap relative to house style.

## What's solid
- The "dedicated lane, not the core toolchain matrix" structure directly mirrors the
  project's existing `gui-wayland` lane (on-push + nightly cron,
  `.github/workflows/ci.yml:353`) — a proven, already-working pattern in this codebase,
  not an invented one.
- The Boundary section correctly excludes the template itself (#694) and corpus-scale
  grading (#697), keeping this issue's scope from creeping into either neighbor.
- Every AC traces to a specific line in the parent capstone (CAP-21 risk 5, KC-21-3,
  AC-4, AC-5) rather than inventing new requirements — the issue is honest about where
  its obligations come from.

## Verdict rationale
Two high-severity issues — a title/mechanism mismatch on the central "spec drift"
claim, and missing traceability to the parent capstone's own named oracle tests — mean
this issue can be closed by a PR that satisfies every literal checkbox while missing the
actual goal (catching real Gradescope drift before it hits a live course) and while
leaving CAP-21's Definition of Done unsatisfied downstream. Rework the drift-detection
mechanism and name the oracle tests before this is picked up.
