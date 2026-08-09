# Issue #694: TASK-C525-1: the in-tree Gradescope template exists — pinned headless-JRE image, setup and run_autograder scripts, and an xUnit-to-results.json adapter with a visible/hidden split
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

TASK-C525-1 is the first of three child tasks under #525 (FEAT-C21-2), itself
serving CAP-21 (#502). Scoped narrowly — template + adapter for one lab, not
corpus-scale grading (#697/TASK-C525-2) or drift guards/doc-tests
(#699/TASK-C525-3) — the boundary discipline is real and the split is
sensible. The problems are that two of its four acceptance criteria rest on
things that do not exist in the tree today, one AC cites an explicitly
non-normative document as if it were settled policy, and the remaining ACs
leave the word "valid" and the word "bounded" undefined enough that a
minimal, non-functional implementation could satisfy them on paper.

## Findings, most severe first

### 1. (High) AC-4 and the Boundary section cite #498 §7.2 and §8 as settled policy — #498 explicitly forbids exactly that

AC-4 reads: *"the adapter opens no interactive session (CAP-21 AC-4, #498
§7.2)"*, and the Boundary section reads: *"No LTI tool and no JLS-operated
service (#498 §8 exclusion 7)."* I fetched #498 directly. Its own text
states, twice: *"It is explicitly non-normative. Its own status line says so
and is preserved verbatim in part 1. **Nothing in it may be cited as settled
policy.**"* §7.2 itself is not a ratified rule — it is a *proposal*, whose
own final paragraph reads: *"Process. A decision issue quoting both
sentences; **edit** the section rather than leave the contradiction
standing; a CHANGELOG entry; an `ARCHITECTURE.md` decision block..."* — i.e.
the amendment to `vcd-interop.md` that would make this binding has not
happened. §8 is titled "What is deliberately excluded" inside the same
non-normative rescue document. #694 cites both as though they were already
adopted project policy, in an issue whose own acceptance checkboxes an
implementer will tick off. This is not merely stylistic: it lets a claim
that has never gone through the project's own required decision process
(comment/CHANGELOG/`ARCHITECTURE.md` block) pass silently into a shipped
feature's Definition of Done. Recommend AC-4 and the Boundary note instead
cite the actual load-bearing, already-normative facts — `ARCHITECTURE.md`'s
"Batch mode never leaves the main thread and never touches Swing" and
`HeadlessCoreRatchetTest` — and drop the #498 citations, or explicitly flag
them as "pending ratification, tracked in #498" rather than presenting them
as settled.

### 2. (High) AC-2 depends on a "CAP-21 fixture lab" that does not exist anywhere in the tree, and #694 declares no dependency that would create it

AC-2: *"The zip grades a submission of the CAP-21 fixture lab, emitting
valid `results.json`... with a visible/hidden split declared by the lab, not
hard-coded in the adapter."* I searched `test/fixtures/` and `examples/`:
the only fixtures present are `fork-4.6-shiftregister.jls`,
`headless-canary-gate.jls`, `legacy-4.1/`, and `riscv-sum1to10.jls` — no
CAP-21 fixture, no "lab-as-data" artifact, no visible/hidden-split
declaration anywhere in the repository. `ordering_after: [524]` is the
issue's *only* declared prerequisite. #524 (the frozen CLI contract) does
not produce a lab fixture; the lab-as-data format is CAP-06 lineage
(#369/#466, per #524's own body), and the 300-submission fixture *corpus*
is explicitly owned by #531 per the sibling #525/#526 reviews already in
this tree. Nowhere in that graph is a single canonical "CAP-21 fixture lab"
instance identified as anyone's deliverable ahead of #694 — #694 both
consumes it and, as written, is the earliest issue in the chain that could
plausibly need one to exist. An implementer picking this up has to either
invent a placeholder fixture (which risks becoming the de facto lab format
by accident, pre-empting CAP-06's own design) or stall. Recommend adding the
issue that owns "the CAP-21 fixture lab exists as a committed artifact" to
`ordering_after`, or naming #694 itself as the owner if that's the actual
intent.

### 3. (High) The foundation this task is ordered against is not stable, so "1-1.5 mw" understates real cost

`ordering_after: [524]` is honest about the direct blocker, but #524 was
independently reviewed (`issue-reviews/issue-0524.adversarial.md`,
verdict `needs-rework`) and found to carry a stale `ordering_after` that
imports unrelated blockers, an AC-1 checkbox that is false as literally
worded at HEAD, and a status-3 exit code whose design is owned by #466 —
itself found self-contradictory in its own review. Grepping this tree
confirms the ground truth independently: `System.exit(3)` appears nowhere in
`src/jls/JLSStart.java` (only `System.exit(1)` sites), and no `xunit`/`xUnit`
string exists anywhere under `src/`, `test/`, or `docs/` — the adapter's
input format (the "xUnit verdicts") is not merely unfrozen, it does not
exist yet in any form. #694's outcome text — *"the frozen contract's xUnit
verdicts"* — talks about this format in the past tense as though it were
already a stable input to build against. It is not a defect unique to how
#694 is worded (it correctly names #524 as the blocker), but the stated
`band_mw: 1-1.5` is only honest for the adapter shim in isolation; the real
unblocked-to-ship cost inherits #524's and #466's unresolved instability,
which is not visible from #694's body alone.

### 4. (Medium) "Building it produces a valid Gradescope autograder zip" names no validation surface

AC-1's only check is that the build "produces a valid Gradescope autograder
zip." Gradescope is proprietary and the Boundary section correctly rules out
scraping or undocumented endpoints — but that also means there is no
in-tree schema, validator, or sandbox to check "valid" against, and the
issue names none. As worded, a zip containing a syntactically-well-formed
`setup.sh`/`run_autograder` pair that does nothing meaningful would satisfy
a naive reading of AC-1 without ever having been run against real Gradescope
infrastructure or even a documented offline validator. Recommend naming a
concrete, checkable proxy (a JSON-schema check on emitted `results.json`
against Gradescope's published schema, checked into the dedicated CI lane
`#525`'s Boundary already calls for) rather than leaving "valid" to mean
"well-formed by inspection."

### 5. (Medium) "Bounded output" and "a stated limit" state no number, so the AC is satisfiable by any bound at all

*"Per-test output is bounded by a stated limit, with truncation marked
rather than silent."* No figure appears anywhere in #694's body. Gradescope
enforces its own platform-side output ceilings independent of whatever this
adapter declares; a limit set high enough to never actually truncate (e.g.
100 MB per test) satisfies the letter of this AC while leaving the stated
outcome — "output bounded so a runaway diagnostic cannot blow the
platform's limits" — undischarged in practice. This is the same gap the
sibling #525 review flagged (finding 5) for wall-time budgets; here it
recurs for output size specifically, and #694 is the issue that actually
implements the bound, so the missing number is this issue's problem to fix,
not #525's. Recommend stating an actual byte/line ceiling, ideally checked
against Gradescope's documented per-test output limit rather than
self-selected.

### 6. (Low) The task graph is prose-only; GitHub's own relationship API sees #694 as unowned

`get_sub_issues` on #525 returns an empty list and `get_parent` on #694
returns `null`, despite #694's body declaring `part_of_feature: 525` and
#697/#699 (confirmed via search as the real TASK-C525-2/TASK-C525-3) both
existing as separate filed issues that reference "TASK-C525-1" by name only,
never by issue number. This matches a repo-wide pattern already noted in the
#524 review, so it is not scored heavily against #694 specifically, but a
scheduler or dependency-graph tool operating on GitHub's native issue
relationships would not discover #694 is blocked by #524, or that #697
depends on #694, from anything but reading prose.

## What's solid

- **Scope boundary is genuinely disciplined.** Deferring the 300-submission
  corpus run to #697 and drift/doc-test guards to #699 keeps this task to a
  single-lab, single-submission slice — narrower and more honest than its
  parent #525's own AC-1, which conflated single-lab and corpus-scale
  grading (per the #525 sibling review).
- **AC-2's "visible/hidden split declared by the lab, not hard-coded in the
  adapter"** is a real, specific guard against exactly the gaming pattern
  the repository already has a documented precedent for:
  `examples/autograde/autograde.py` grades by matching literal
  `EXPECTED_STDOUT_LINES`, a shortcut `docs/capability-roadmap/lf-04-formal-and-grading.md`
  calls out by name. Unlike AC-1 and the output-bound clause, this AC would
  actually catch a hard-coded adapter.
- **Grounding in `ghcr.io/anadon/jls`** is realistic, not speculative — the
  image is real, already shipped, multi-arch, and signed per `README.md`,
  so "pinned headless-JRE image" is buildable on top of an existing artifact
  rather than a green-field integration.

## Verdict rationale

The task's own scoping and its anti-gaming AC-2 clause are well judged. But
two of its four acceptance criteria point at artifacts that do not exist in
this tree and are not in its declared dependency set (the CAP-21 fixture lab,
and — transitively — the frozen CLI contract and xUnit schema #524 has not
yet delivered), one AC leans on a document that explicitly disclaims being
citable as policy, and two more leave numeric bounds unstated in a way that
lets a minimal implementation satisfy the letter of the criterion without
the stated goal. None of this requires killing the task — the shape is
right — but it is not safe to hand to an implementer as currently worded.
**needs-rework.**
