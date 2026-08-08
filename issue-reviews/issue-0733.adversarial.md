# Issue #733: TASK-C555-2: the README carries one performance line that cites the doc — and no public performance claim exists anywhere the doc does not back
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Add one README line linking to `docs/performance.md`, sweep README + `docs/` for every existing performance claim (backed/corrected/deleted), add a build check that blocks unbacked "performance-shaped" claims going forward, and show the doc doesn't disagree with #335's internal-plan constants. Banded at 0.25-0.5 maintainer-weeks. Confirmed against the checkout: `docs/performance.md` does not yet exist, and README currently contains zero performance claims (`grep -n -i performance README.md` — no hits), so the README half of the task starts from an empty set.

## Findings, most severe first

**1. (High) AC-4 depends on an artifact and a measurement chain that do not exist yet, and the dependency is undeclared.**
AC-4 requires showing "the doc and #335's internal-plan constants" don't disagree. #335's own body describes those constants as living in `docs/machine-calibration.md` ("1,124 lines" at its evidence commit, sections 6/7). That file is absent from this repository in full history: `find /home/user/JLS -iname "*machine-calibration*"` and `git log --all -- docs/machine-calibration.md` both return nothing. Worse, #335 itself states its measurement tasks (TASK-0022 through TASK-0026, the ones that would produce trustworthy tracked constants) are all "not filed" — the whole point of #335 is that today's constants are "untracked, unmeasured, or measured under an unstated clocking regime." AC-4 as written asks #733 to certify agreement with numbers that, by #335's own diagnosis, are not yet fit to certify agreement with. This is circular: you cannot show non-disagreement with a ratchet that hasn't measured anything yet.
*Recommendation:* Either drop AC-4 from this task's scope (defer it to whenever #335's TASK-0022+ actually lands, with an explicit `blocked_by`), or rewrite it to something checkable today, e.g. "no numeric constant in `docs/performance.md` is asserted to equal a #335 constant unless that #335 constant is itself tracked and cited by commit."

**2. (High) The declared prerequisite chain is one hop; the real one is at least four.**
`ordering_after: ["TASK-C555-1"]` (#732) is the only stated dependency. But #732 itself orders after #554 (FEAT-C28-1, the benchmark suite), which orders after #413 (TASK-0025, re-homing the CPU-scale fixture and deleting `riscv/`), which per #335's Global Invariant 4 ("fixture first, deletion second") cannot land before #335's own TASK-0022/TASK-0023 measurement work — work that is "not filed" as of #335's evidence commit. So a contributor who reads only #733 and lands its one named prerequisite (#732) will hit a wall: #732 can't produce trustworthy numbers until a chain of at least three more unfiled tasks lands. None of that transitive depth is visible from #733's own dependency block.
*Recommendation:* Either add the transitive `blocked_by` chain explicitly (at minimum #554 and #413), or state plainly in the Boundary section that #733 is not actionable until that chain closes — right now the issue reads as independently startable when it structurally is not.

**3. (Medium) AC-3 ("a check fails the build when a performance-shaped claim appears... without a citation") is underspecified to the point of being ungameable-to-verify in both directions.**
No definition of "performance-shaped" is given, and none of README/`docs/` currently has such a linter. A literal regex over digits + throughput units would have to fire on the ~20 existing numeric performance mentions this repo already carries outside README — 9 in `docs/capability-roadmap/keystone-c-performance.md` alone (e.g. "≈ 4,600 cycles/s", "2,331,793 events on `riscv/build/k2000.jls`, 318 ns/event, 8,090 cycles/s"), plus more in `lf-02-compiled-evaluation.md`, `lf-03-causal-debug.md`, `AMENDMENT.md`, `sweep-04-verification.md`, `sweep-05-system-and-interfaces.md`. Those are speculative capstone-planning projections (e.g. "roughly 25–40 kcycles/s on the RV32I CPU after stage 1" — a forecast, not a published claim about the shipped build), not claims that belong cited against a *current* published-performance doc. A check that can't tell "measured today" from "projected if a future optimization lands" will either produce nonsense false positives across the whole roadmap corpus, or (if scoped narrowly enough to avoid that) will just as easily be satisfied by rephrasing a real unbacked claim in prose with no digits ("JLS handles large circuits smoothly") — gaming the check while leaving the actual goal (no unbacked public claims) unmet.
*Recommendation:* Define "performance-shaped" concretely (a regex/word-list, stated in the issue or the check itself) and explicitly scope the check to README + a stated allow-list of docs (not all of `docs/`, which includes admittedly-speculative roadmap material) — or exempt `docs/capability-roadmap/**` by name and say so.

**4. (Medium) Scope-vs-budget mismatch.** Taken literally, "a sweep of README and `docs/`" plus "every existing public performance claim is listed and dispositioned" covers at least 9 distinct performance figures in `keystone-c-performance.md` alone plus more spread across 5-6 other roadmap documents (20 hits total for `kcycles/s|cycles/s|events/s|ns/event|µs/cycle` repo-wide). Auditing, dispositioning (backed/corrected/deleted), and then editing each of those across several long planning documents, on top of building and wiring in a new CI check, is not a 0.25–0.5 maintainer-week task (1-2 days) — it's closer to what #335 itself (5-10 mw) budgets for a comparable measurement/ratchet exercise, and #335 is explicitly *not* claiming to touch the public docs surface.
*Recommendation:* Either shrink the literal scope (state that the sweep only covers README + a named short list of doc files considered "public claim surface," excluding the roadmap tree) or rebudget the task realistically.

**5. (Low) AC-2's "recorded" has no specified home.** The issue never says where the sweep's disposition list is supposed to live — a PR description, a repo file, an issue comment. Without a durable, checked-in artifact, "recorded" can be satisfied by a comment that vanishes from search a week later, and the next contributor has no way to tell the sweep was ever done without re-doing it — precisely the repeat-work risk criterion 3's automated check is supposed to prevent for the forward-looking case.
*Recommendation:* Name the artifact, e.g. a "Claims audited" section inside `docs/performance.md` itself or a dated `docs/performance-claims-sweep-YYYY-MM.md`.

## What's solid

- The core outcome (one README line, backed by a doc, with drift prevented by a check) is a sound, minimal, well-motivated shape for the problem — over-claiming performance in READMEs is a real and common defect class.
- Boundary line correctly separates this task from #335's internal-plan ratchet and correctly names the shared-constants risk (even though, per finding 1, it doesn't yet have the machinery to discharge it).
- The three-way disposition taxonomy (backed / corrected / deleted) for AC-2 is a reasonable, unambiguous set of outcomes per claim.
- Confirmed no pre-existing performance claim in README today, so the "no unbacked claim" half of AC-1/AC-4 for README specifically starts clean — that part is not lying about the current state.

## Bottom line

The README-line deliverable is fine and small. The sweep-and-ratchet deliverable, as scoped, silently assumes an unbuilt measurement chain (#335's constants, via #732→#554→#413) and a build check whose target concept ("performance-shaped claim") is never defined — both are load-bearing gaps, not polish items. Needs rework before this is actionable as a bounded 0.25-0.5 mw task.
