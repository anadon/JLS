# Issue #379: TASK-0023: the behavioral events-per-instruction constant and the levelized per-node cost stop being estimates, and every ns/node figure carries its node count and pass count
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

Build an unregistered `~200`-line behavioral accumulator element in `src/jls/elem/`,
measure events-per-retired-instruction on three instruction mixes, re-run a "levelized"
harness at ~1,346/~1,400 slots, and write both results into `docs/machine-calibration.md`
§6.4/§6.6/§2.6/§6.11. Depends on TASK-0022 (`blocked_by` left empty "pending a link pass").

## Findings, most severe first

**1. The deliverable document does not exist on this repository, and the issue's own
comment thread already says so.** `docs/machine-calibration.md` is absent from HEAD
(`ls docs/` shows no such file; `find` over the whole tree finds nothing). It only ever
existed on the abandoned branch `claude/jls-virtual-hardware-linux-njsoma`, whose tip
`742da745c6e5eac3da161ef6d4a1fee9ac2e38ee` explicitly states: *"The maintainer ruled that
this branch will not be merged and will be deleted, and that anything not carried into a
GitHub issue dies with it."* The `evidence_commit` this issue pins
(`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`) is not an ancestor of `HEAD` at all — it lives
only on that doomed branch. This is not a theory: issue #379 itself carries a 2026-08-08
comment from the repo owner titled *"EVIDENCE CORRECTION: this task's premise cites a file
that does not exist on master"*, stating `git ls-tree -r origin/master` has "no
`docs/machine-calibration.md`" and that the commit "is not present in the repository at
all." **The issue body was never edited to reflect this** — every checklist item under
§8/§14 still instructs editing that file's §6.4/§6.6/§2.6/§6.11, which is currently
impossible as literally written. Recommendation: rewrite the Method and Completion
Criteria to target the actual surviving home for this material (`docs/capability-roadmap/
keystone-c-performance.md`, per the same comment) before anyone picks this up, or the first
hour of work is spent discovering what the maintainer already told the tracker.

**2. Half of this task's stated problem (§6.6, the O2 "1.39x ambiguity") is already
measured on master, undermining the task's premise that it is "worse than unmeasured."**
`docs/capability-roadmap/keystone-c-performance.md:38-39,468,474-475,490` carries: *"A
levelized pass over the RV32I CPU's 522 evaluation slots costs 2.26 µs (4.32 ns/node) with
plane arrays and 11.49 µs (22.0 ns/node)"*, with the node breakdown stated plainly
(*"522 evaluation slots — 225 logic elements + 297 nets"*) and the activity-table 100% row
at 1.62 µs also present at line 490 — i.e., the exact "two figures for one pass" the issue's
O2 complains is unreconciled already carries its census and its two pass identities in the
document that actually exists. The owner's own comment on #379 confirms this: *"the
'every ns/node figure carries its node count and pass count' half of this title is already
satisfied by the source document."* So the task as scoped would spend part of its budget
re-deriving something the tracker already has, because the issue was written (and remains
open, unedited) against a deleted source instead of the live one.

**3. `blocked_by` is empty but the real dependency is filed, open, and unlanded — the
issue's own Definition of Done is unmet by its own machine block.** The YAML block reads
`blocked_by: []  # ... the TASK-0022 edge is real and is added in the link pass`. TASK-0022
is not hypothetical: it is issue **#377** (open, filed the same day as #379, 2026-08-03),
and it has not landed. The "link pass" promised in the issue text never happened — as of
today (2026-08-09) the machine-readable dependency field still says `[]` while §14's
completion checklist requires *"The `blocked_by` edge to TASK-0022 is written into the
machine block by the link pass and that task has landed, or the dependency was waived."*
Neither condition holds. Anyone triaging by the YAML block alone (which several automation
patterns in this fleet do) will conclude #379 is unblocked and pick it up prematurely.
Recommendation: fix the machine block now, not at pickup time.

**4. O5's forbidding of a `.jls`-based measurement circuit assumes a claim (about the
frozen tag table) that is only pinned at the dead evidence commit, and the "not registered"
plan collides with recent registry-totality work in flight.** O5 counts 35
`ElementType(` rows via `git show 2d0ca9d:...ElementRegistry.java`, which is fine as a
historical citation but is silent on what the totality lint at HEAD (TASK-0001 = #372,
TASK-0002 = #375, both **open**, per `test/jls/ElementRegistryTest.java` and
`test/jls/edit/PaletteContractTest.java` already in-tree) will do to an element deliberately
absent from `ElementRegistry.ALL`. Open Question 2 in the issue correctly flags this as
"Blocks execution" and recommends "a named single-entry exclusion," but that exclusion
mechanism doesn't exist yet either — it depends on #372/#375, which are themselves open and
not named in `blocked_by`. This is a second unstated real dependency, same shape as finding
3.

**5. Acceptance criteria are gameable on the H1 mix-sensitivity question.** D1 says *"If
max − min over the three [ev/instr figures] exceeds the run-to-run spread of any one of
them, H1 holds."* Nothing in §5/§9 requires the run-to-run spread to be measured with
any minimum number of repetitions or a stated confidence procedure beyond "at least three
repetitions" mentioned once in §9 — a noisy single machine could report near-zero spread
by accident of one favorable run, flipping the H1 verdict either way. The falsification
criteria (§10) do not describe what happens if repeat runs disagree with each other about
whether D1 holds. Recommendation: fix the repetition count and a tolerance/CI method before
D1 is treated as decisive.

**6. Scope-creep risk baked into the "one product-code line" framing.** P1/O4 honestly
documents that a `permits` clause edit is unavoidable (verified independently: current
`src/jls/elem/LogicElement.java:17-21` still lists exactly the 24 permitted subclasses the
issue's own §7.12 point 3 anticipates), which is a solid, self-aware piece of the issue.
But §6/§7.4 also commit to two new `@Tag("slow")` test classes, a ~200-line element, three
synthetic programs, a reference-loop oracle, and a from-scratch levelized harness re-run at
two new slot counts with a 4-way activity sweep — a large surface for a task whose
"Completion Criteria" (§14) run to 20 checklist items, several of which depend on other
unlanded tasks (TASK-0022, TASK-0001/0002, TASK-0016's `longrun` lane). The abstract frames
this as "days," but the true unit of work, once its real dependencies are counted, is
closer to the sum of TASK-0022 + TASK-0023 + partial TASK-0001/0002 — undercounted cost.

## What's solid

- O4's sealed-hierarchy observation is accurate and reproducible against the current tree
  (`Element`/`LogicElement` permits verified verbatim at HEAD, not just at the evidence
  commit) — a genuine, well-evidenced feasibility finding.
- O6/the `afterEvent` instrumentation seam claim (`src/jls/sim/Simulator.java:269-270`,
  overridden at `src/jls/sim/BatchSimulator.java:140`) is accurate at HEAD.
- The falsification criteria (§10) and the "no `.jls` file" / "no format bump" compatibility
  discipline (§7.12) are unusually rigorous and worth keeping once the target document is
  corrected.
- The clocking-regime warning (O3, "never a `-t` vector") is well-grounded and matches the
  same discrepancy independently documented in `docs/capability-roadmap/
  keystone-c-performance.md` and issue #377.

## Bottom line

The measurement idea (H1: does ev/instr vary by mix) is not obviously broken, but the issue
as currently written instructs contributors to edit a file that provably does not exist on
this repository, understates its own real blocking dependencies in the machine-readable
`blocked_by` field, and — per the maintainer's own comment already sitting on this exact
issue — has had half its stated problem (§6.6) resolved elsewhere without the issue being
updated to say so. This needs a rewrite pass (re-target the doc, fill in `blocked_by`,
tighten D1's statistics) before it is safe to assign.
