# Issue #335: FEAT-009: every wall-clock claim in the plan divides by a measured constant, taken on a tracked fixture, under a ratchet that turns a regression into a build failure
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The stated goal — stop publishing boot-duration/throughput arithmetic over
unmeasured constants, and gate a regression as a build failure — is sound
engineering hygiene. But the issue's own evidentiary basis does not hold up
against the checked-out tree: the evidence commit it pins is unreachable, and
the document the whole feature is centered on discharging does not exist
anywhere in the repository. Both are load-bearing, not cosmetic, because
nearly every acceptance criterion in §5 quotes line numbers and prose from
that document. There is also live drift between the issue body's own
machine-readable status block and its comments, filed two days apart.

## Findings, most severe first

### 1. [Critical] Evidence commit `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not exist in this repository

The issue pins `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and
builds an entire "Every scope verified ABSENT at `2d0ca9d`" evidence block on
top of it — `git ls-tree`, `git show`, `git grep` output quoted verbatim, plus
GitHub permalinks in sibling issue #353 that resolve to blob URLs at that
SHA. In this checkout:

```
$ git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7
fatal: git cat-file: could not get object info
$ git log --all --oneline | grep '^2d0ca9d'
(no output)
```

268 commits are reachable from the current tip (`5b05d67`), none of them
`2d0ca9d`. Every "verified ABSENT at 2d0ca9d" claim in the issue is therefore
unverifiable against the actual tree a reviewer or implementer has to work
from. **Recommendation:** re-derive the evidence block against a commit that
actually exists in this repository, or explain (in the issue, not just
assumed) why the pinned commit is from a different history than what's
checked out — silently treating an unreachable SHA as ground truth is how a
regression slips through unnoticed.

### 2. [Critical] `docs/machine-calibration.md` — the document this feature exists to discharge — does not exist in the repository at all

The issue asserts as settled fact: *"`docs/machine-calibration.md` already
exists at `2d0ca9d` — 1,124 lines — so TASK-0024 fills and corrects it rather
than creating it."* It then cites specific content by section and line
number throughout: §6 ("What is still unmeasured and load-bearing," 9 open
entries 6.1–6.9), §7.2 ("Zero changes to `jls.sim` are required"), §7.1
("That file was never tracked" / "any successor circuit must be
re-characterized from scratch"), and line ranges `:870-871` and `:878-879`
with verbatim quotes.

None of this exists:

```
$ find / -iname "machine-calibration*"
(no output)
$ git log --oneline --all -- docs/machine-calibration.md
(no output)
$ ls docs/ | grep -i calib
(no output)
```

`docs/` has 27 files including a `capability-roadmap/` subdirectory with the
actual performance data this feature seems to be drawing on
(`docs/capability-roadmap/keystone-c-performance.md`, `AMENDMENT.md` — both
of which do reference `riscv/build/k2000.jls`, ns/event figures, and a
3.2x-style spread), but no file named or resembling
`machine-calibration.md` anywhere in the tree or its history. The only other
mention of "machine-calibration" in this repository is another reviewer's
output file (`issue-reviews/issue-0362.adversarial.md`), i.e. corroborating
that this is a real, independently-noticed defect, not a one-off
misreading.

This is not a cosmetic citation error. §5's criteria 1, 3, 4 and 5 — four of
five integration-level acceptance predictions — are phrased as "re-derive
from the tracked fixture and the recorded constants," "the resolution is
written down [already, elsewhere]," and "follows section 7 end to end… and
reproduces section 2." All of them presuppose an existing document with
existing content that a later task merely "discharges." If the document does
not exist, TASK-0024 is authoring a calibration methodology from scratch
under the guise of "filling in" nine already-scoped open items — a
materially larger and less well-specified task than the issue describes, and
every acceptance criterion anchored to "what the doc already says" has no
ground truth to check against. **Recommendation:** either locate/relink the
actual source of these quotes (a different path, a draft PR, an
uncommitted local file) and correct the issue, or strike the "already
exists, TASK-0024 corrects it" framing and re-scope TASK-0024 (and the cost
estimate under it) as document creation.

### 3. [High] The issue's own machine-readable status is already stale relative to its comments

The §2 table lists all six tasks (TASK-0022 through TASK-0026, TASK-0016)
with Status **"not filed"**, and the YAML block declares `requires_tasks:
[]`. But comment #1 (2026-08-03, one day after filing) states plainly:
*"**#413 (TASK-0025)** was filed under this feature"*, with its own
`blocked_by: [#377, #379]`. Comment #3 (2026-08-04) refers to "#442
TASK-0026" and "#378 TASK-0016" as already-filed, consumed-by-siblings
issues. None of this is reflected back into the issue body — `requires_tasks`
is still `[]`, and the §2 roster still says "not filed" for tasks the
issue's own comments treat as filed and being actively coordinated across
three other issues (#278, #554, #557).

The issue's own §7 Re-planning Protocol requires: *"A child is split
(HANDOFF). Update § 2's roster and the mermaid graph in the same edit as the
`REPLAN:` comment."* That rule was not followed for TASK-0025/0026/0016's
filing — the amendments happened in plain comments, not `REPLAN:` comments,
and §2 was never edited. This is exactly the drift the issue's elaborate
machine-readable apparatus (`requires_tasks`, `planned_tasks`, the mermaid
graph, "Machine block, roster table, and mermaid graph agree with reality at
close") is designed to prevent, and it has already happened within 48 hours
of filing. **Recommendation:** edit §2 and the YAML block to reflect #413,
#442 and #378 as filed, or explain why they intentionally stayed out of
`requires_tasks`.

### 4. [High] Several acceptance criteria are self-graded prose, not automated checks

§5 criteria 1, 3 and 5 are each annotated "*Recorded manual procedure, built
at close-out*" or "*No child asserts this; built at close-out*" — meaning
there is no test, script, or CI check gating them at merge time; the
evidence is whatever the closer writes in a closing comment. Concretely:

- Criterion 3 ("The ratchet actually fails") asks someone to perturb the
  engine once, observe a red build, and record the command/output. Nothing
  in the Definition of Done requires that perturbation-and-recovery to be
  captured as a standing regression test that runs on every future PR — so
  the gate can be demonstrated to work once at close-out and silently bit-rot
  afterward with no re-verification mechanism.
- Criterion 5 ("Section 7 is executable") asks for "a person who has never
  seen the code" to follow the procedure on a clean clone — but the
  evidence is again a recorded manual procedure, not an automated
  clean-clone CI job. A closer under time pressure can write "reproduced
  within band" without a genuinely naive execution ever having happened, and
  nothing in the Definition of Done catches that.
- Criterion 4 (the TestGen-vs-internal-`Clock` 2.02x/3.2x discrepancy) is
  simultaneously produced and adjudicated by the same feature: TASK-0022
  measures the numbers, and "this issue asserts the resolution is written
  down" — i.e. the bar is that *some* explanation exists in prose, not that
  an independent party validated it. A plausible-sounding but wrong
  rationalization satisfies the criterion as written.

**Recommendation:** convert at least criterion 3 into an actual CI-enforced
regression test (a fixture-and-mutation test that intentionally breaks the
equality and asserts the build goes red, run continuously — not just once at
close), and require criterion 5's reproduction to be scripted rather than
narrated.

### 5. [Medium] Open Questions blocking TASK-0025 are unresolved, but TASK-0025 (#413) is already filed

Open Questions §1 ("Where does a tens-of-megabyte fixture live?") and §2
("Which fixture is the CPU-scale anchor?") are both explicitly annotated
**"Blocks TASK-0025."** Both are still open in the issue body at time of
read (no decision recorded, no comment resolves them). Yet comment #1
confirms TASK-0025 has already been filed as #413, with its own §8
re-homing step reportedly naming only two of the six example files needed.
Either these blocking questions were answered somewhere not reflected in
#335 (more of the drift in finding #3), or #413 was filed against unresolved
fixture-identity decisions for a task whose own feature's §4 invariant 4
calls the fixture-before-deletion ordering "the one ordering constraint in
the feature that is a correctness constraint" — i.e. the highest-stakes
sequencing rule in the issue rests on a decision that isn't made yet.
**Recommendation:** resolve Open Questions 1–2 (or point to where they were
resolved) before treating TASK-0025 as unblocked to execute.

### 6. [Medium] Cost is large and cross-charged in a way that invites double- or under-counting

Band is "5-10 maintainer-weeks" for a feature whose Abstract states "This
feature ships no product code." The task-row sum (8.5 wk) includes
TASK-0016, TASK-0023 and TASK-0026 explicitly flagged "shared with sibling
features and counted once, here." The same convention appears independently
in #353 ("counted once, at the task level, not once per consuming feature")
and in the #554/#557 boundary comments, each with its own "counted once"
disclaimer. For a single-maintainer project, tracking which feature actually
absorbs a shared task's cost across four-plus interlocking issues is a real
scheduling hazard — if two features both assume the other is "the one that
counts it," the true cost of landing either is silently underrepresented,
and if a scheduler naively sums every feature's row independently, the true
cost of the graph is silently overrepresented. Nothing in #335 states which
failure mode the maintainer should defend against. **Recommendation:**
maintain a single ledger (or a dedicated tracking issue) that shows each
shared task's cost attributed exactly once across the whole graph, rather
than leaving the "counted once, here" disclaimer to be independently
re-asserted (and potentially contradicted) on every consuming issue.

### 7. [Low] Two similar-but-distinct spread figures are never cross-referenced

§3's boot-cost discussion states α is "unmeasured with a 3.1x spread." §5
criterion 4 separately cites a "3.2x spread" for the TestGen-vs-internal-
`Clock` events-per-cycle disagreement (121.5 vs 245.5 vs 388.4). These read
as two different measurements that happen to round to nearly the same
multiplier, attached to the same measurement effort, and the issue never
states explicitly that they are unrelated numbers — worth a one-line
disambiguation so a future reader (or an implementer skimming for "the 3.x
spread") doesn't conflate them.

## What holds up

- The fixture-before-deletion ordering constraint (§4 invariant 4: "No
  commit may remove `riscv/` before the replacement fixture is tracked and
  referenced") is a genuine, correctly identified correctness hazard, and it
  is stated as the one hard ordering rule in an otherwise "convention only"
  sequencing section — appropriately weighted.
- The claimed instrumentation seam is accurate: `Simulator.java:269`
  (`protected void afterEvent(SimEvent event) {}`, no-op) is indeed
  overridden at `BatchSimulator.java:140` — verified directly against the
  tree, so TASK-0022's "zero changes to `jls.sim` required" premise has real
  footing independent of the missing calibration doc.
- The `RiscvCpuGoldenTest.java` citations are accurate word-for-word: the
  fixture path at line 43, the "34 cycles" javadoc at line 28, and the
  register assertions at lines 83-85 all match the tree exactly, and
  `test/fixtures/riscv-sum1to10.jls` is genuinely tracked (not gitignored).
- `riscv/.gitignore` content (`build/`, `__pycache__/`, `*.pyc`) matches the
  issue's quoted output exactly, and the full `riscv/` file listing in the
  issue matches `ls riscv/` in this checkout.
- The scope boundary drawn against the sibling C28 benchmarking cluster
  (#554/#555/#557/#560) in the comments is a reasonable, carefully argued
  split (internal per-commit gate vs. public-facing benchmark suite vs.
  competitor comparison) and correctly avoids duplicating the constants
  program across five issues.
