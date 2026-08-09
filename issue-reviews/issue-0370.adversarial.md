# Issue #370: FEAT-054: a circuit's runtime state lives in flat primitive arrays indexed by element, so the per-element footprint falls by roughly an order of magnitude and the largest design that fits one machine grows by the same factor
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what the issue asks for

A capacity-motivated refactor: move per-element simulation state out of the
current object graph (`Circuit.elements`, a `HashSet<Element>` at
`src/jls/Circuit.java:48`) into flat primitive arrays indexed by element,
under an unchanged element-facing `react` contract, gated on a heap-per-element
measurement (≤150 B/element) and a byte-identical golden corpus. The code
claims in §1 check out against the current tree (verified below); the
structural problems are elsewhere: stale self-description, an unresolved
collision with a sibling feature that is already executing, and an evidence
base that is provably unrecoverable for part of its own acceptance criteria.

## Findings, most severe first

**1. The issue's own children already exist and collide with a sibling feature — and the issue body doesn't say so.**
Open Question 1 states plainly: *"The task ids do not exist and minting them
is a maintainer decision... **Blocks filing children.**"* That is false as of
the issue's own `updated_at` timestamp (2026-08-08, same day as this text).
`get_sub_issues` on #370 returns `[]` (no native GitHub linkage), but issue
#846 ("TASK-C370-3: runtime state moves out of per-element objects...")
explicitly declares `part_of_feature: 370` and is open — one of at least six
children (#842, #843, #846, #848, #850, #851) filed under a `TASK-C370-N`
convention that sidesteps the "registry closed at TASK-0112" excuse entirely.
Worse, a comment already on this issue (2026-08-08, the sole comment) reports
two unresolved collisions between #370's children and #362's (FEAT-030)
children: #846 and #879 both relocate the same runtime state under the same
byte-identity acceptance criterion "from opposite motivations," and #848's
index contract is asserted against the event queue that #362's #476 deletes
outright. §7's Re-planning Protocol requires *"Every response ends in a
`REPLAN:` comment here"* for exactly this scenario (FEAT-030 funded first ⇒
"re-scoped, not re-estimated"), and none exists. **The issue text a reader
sees is stale relative to work already in flight under its own name**, and
the one documented safety mechanism for the conflict it warns about hasn't
fired. Recommendation: do not treat §"Open Questions" as current; check
children/comments before scoping any work against this issue.

**2. Part of the acceptance-criteria evidence base is permanently unrecoverable — a fact the issue does not disclose.**
The Cost section anchors the byte-budget derivation to `docs/plan/REGISTRY.md:125`
and the 1,190 B / 2,150 B / 6.8× / 150 B-per-element figures to
`docs/plan/evidence/` documents landed at commit `3a81a4a`. Issue #493 (filed
2026-08-03, open) establishes that `evidence_commit: 2d0ca9d...` — which #370
itself declares — is a merge commit on a branch that "will not be merged and
will be deleted," and that `docs/plan/**` (192 files, explicitly including
`REGISTRY.md`) is **"Unrecoverable by re-reading... These files never existed
on `master`"**. I confirmed independently: `git merge-base --is-ancestor
2d0ca9d HEAD` and the same for `3a81a4a` both return false, and
`docs/plan/REGISTRY.md` / `docs/plan/evidence/` are absent from the working
tree entirely. So Completion Criteria item *"Every cited evidence document and
permalink resolves on the default branch at close"* is, for this issue's cost
band and byte-budget provenance, **unsatisfiable as written** — there is
nothing to re-pin it to, unlike the seven code files #493 gives a master
equivalent for. Recommendation: before this issue is picked up, the numeric
claims in §3/Cost need a durable citation (e.g. inline quotation, as the issue
already does for some figures) or an explicit `WAIVED:`/re-derivation, not a
dead permalink.

**3. The capacity/throughput split is asserted, not enforced, and both sides are executing in parallel right now.**
§1 states "Rejected: fold this into FEAT-030 (#362) outright... The correct
treatment is one contract cited by both." That's a reasonable design stance,
but Finding 1 shows the two features are *not* sharing one contract in
practice — they're independently filing overlapping children. The issue's
own § 7 anticipates this ("Whoever funds one pays for it; the other must be
re-scoped rather than re-estimated") but provides no mechanism forcing the
check to happen *before* a second migration is written, only after ("An
executor discovers the overlap by writing the second migration" is exactly
what the mirrored comment describes almost happening). This is a process gap
dressed as a solved problem.

**4. The ≤150 B/element acceptance threshold is measured against an unconstrained "generated design."**
§5 criterion 1 and Open Question 2 fix the number (150 B/element) but not the
fixture beyond "a generated design at scale." The mirrored comment on this
issue (Finding 2 in that comment) independently flags this: "#842 should
measure on the same fixture the throughput side already uses... If #842
generates a different design, the two features' numbers cannot be compared
and the 'same code from two sides' argument becomes unfalsifiable." A design
that is homogeneous/regular (repeated small elements, few large-field types)
will trivially post a lower B/element than the CPU-shaped fixture the sibling
feature and `keystone-c-performance.md` already use — the criterion as
written can be passed by picking a favorable generator without touching the
actual worst case (wire-heavy or field-heavy circuits) it's meant to bound.
Recommendation: pin the same fixture the sibling feature uses
(`riscv/build/k2000.jls`, per `docs/capability-roadmap/keystone-c-performance.md`
§2) explicitly in this issue, not just as a recommendation in a comment.

**5. Invariant 3 ("outranks the capacity gain... can veto the whole feature") has no measurement method defined anywhere in this issue.**
§4 invariant 3 and §5 criterion 3 require "startup time and per-edit cost
measured before and after on the same fixtures," but no method, tool, or
threshold is specified — contrast with the heap criterion, which cites a
"measurement gate" (#335) by name. Without FEAT-009 (#335, also a hard
`blocked_by` dependency) actually landing a per-edit-cost methodology, this
veto is currently unimplementable and untestable, meaning the single
strongest safety property in the issue is presently unenforceable. This is
consistent with the issue's own sequencing (#335 is `blocked_by`), but the
issue doesn't flag invariant 3 as blocked the way it flags criterion 1.

**6. Scope-boundary/prerequisite chain is long and every prerequisite is itself an open, unlanded issue.**
`blocked_by: [322, 335, 362]` — FEAT-026 (#322, open), FEAT-009 (#335, open),
FEAT-030 (#362, open, and per Finding 1/3 actively colliding with this issue's
own children) — plus §6's stated true prerequisite, FEAT-005 (unnamed by
number, "not funded" branch acknowledged in §7), which the text says must land
*first* even though it isn't in `blocked_by` at all: "FEAT-005 is therefore
first... FEAT-005 is not funded: this feature can still be built, but its
measurement cannot be taken above the load-path wall." That's a fourth hard
prerequisite missing from the machine-readable `blocked_by` list — a gap
between the prose and the DAG the issue's own "Link phase" paragraph claims to
have walked and verified as consistent.

## What's solid

- The code citations that matter for §1 (`Circuit.java:47-48`, `:1345`,
  `:1368-1369`; `FileAbstractor.java:65` and the eleven `MAX_CIRCUIT_TEXT_BYTES`
  read-only call sites, confirmed — no save-side check exists in
  `writeCircuit`) check out against the current tree and are not among the
  seven files #493 flags as branch-only-diverged, so they're safe to treat as
  accurate at master.
- The 35-registered-element-type and 27-`react`-implementation counts both
  reproduce exactly against current `ElementRegistry.java` / `grep -rl "void
  react("`.
- Global invariant 1 (byte-identical goldens) and invariant 6 (no `.jls`
  format change) are concrete, testable, and appropriately non-negotiable.
- Explicitly scoping out throughput-as-acceptance-test is the right call given
  the measured near-flat engine throughput vs. circuit size claim, and keeps
  this issue from being gamed by a speed benchmark that doesn't touch heap.

## Recommendation

Do not scope new work directly against this issue's Open Questions or
decomposition table as currently written — they are stale relative to six
already-filed children and an unresolved cross-feature collision flagged in
the issue's own comment thread. Before further work: (a) post the `REPLAN:`
this issue's own protocol requires, naming which of #846/#879 and #848/#476
wins the runtime-state relocation; (b) replace the `docs/plan/REGISTRY.md` /
`docs/plan/evidence/` citations with a durable source, since they are
confirmed unrecoverable; (c) pin the measurement fixture explicitly rather
than leaving it as "a generated design."
