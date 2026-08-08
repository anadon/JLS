# Issue #343: FEAT-033: a parity claim gets an independent counterparty — a pure architectural model, a reference runner that executes it, and a reproducibly built guest stack
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue asks for `src/jls/mach/` (a dependency-isolated architectural
model + headless reference runner, TASK-0070) and a reproducible,
digest-pinned guest-image build (TASK-0071). Both are absent from the
repo today (`git ls-tree -d src/jls/mach` at HEAD returns nothing;
`scripts/` has no `build-guest-image.sh` — confirmed against the live
tree). The two named tasks are plausibly scoped. The problem is almost
entirely in how the issue justifies itself and how its own machine-
readable state has already drifted from reality.

## Findings, most severe first

**1. The evidence commit the whole filing is pinned to does not exist in this repository.**
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is cited as the
basis for every "ABSENT at 2d0ca9d" claim (the package doesn't exist, the
build script doesn't exist) and for the entire § Cost section. `git cat-file
-e 2d0ca9d...` fails ("MISSING") and it does not appear in `git log --all`
(268 total commits, none matching). Issue #343's own second comment
concedes this directly: "`2d0ca9d…` is a merge commit existing only on a
branch scheduled for deletion and is **not** an ancestor of `master`
(`8288226…`)" — i.e. the author already knew the pin was ephemeral. A
reviewer working from the checked-out repo (as this review does, per
instructions) cannot verify a single one of the issue's "verified ABSENT"
claims against the cited commit; they can only be spot-checked against
HEAD, which the comment concedes may already have diverged. Pinning
load-bearing claims to a commit known to be about to vanish is a
credibility hazard the issue creates for itself.
**Recommendation:** re-pin `evidence_commit` to a commit that is actually
an ancestor of the default branch before this issue is treated as
actionable, and stop citing commits from doomed branches as evidentiary
anchors.

**2. The entire cited documentary infrastructure is absent from the repository.**
`docs/plan/features/*`, `docs/plan/evidence/BRIEF.md` (the source of the
"D15" ruling, quoted verbatim as binding maintainer authority), and both
`docs/parity-contract.md` and `docs/machine-calibration.md` (cited
elsewhere in this issue's comment thread) do not exist anywhere in this
checkout (`Glob docs/plan/**` → no files; `ls docs/plan` → no such
directory; direct reads of both `.md` paths fail). The issue treats D15 as
a ratified, binding decision and quotes it as settled fact, but nothing in
the actual repository lets an outside reader confirm the ruling was made,
by whom, in what context, or what else that document says. This is the
exact failure mode the issue's own Abstract warns against ("the same
author writes both the drawn machine and the thing it is compared
against, and agreement proves only that one person was consistent") —
applied reflexively to the planning process itself: one party (repo
owner, issue author, and the bot generating these comments) is the sole
source for claims about documents nobody else can read.
**Recommendation:** either commit `docs/plan/**`, `docs/parity-contract.md`,
and `docs/machine-calibration.md` to the tree before citing them as
authority, or strip the verbatim-ruling framing down to "maintainer said
so in this issue thread," which is the only claim actually checkable.

**3. The issue body's own dependency graph is already stale.**
The YAML block still reads `blocks: [326, 345, 347]` and the mermaid
diagram still names `#326 FEAT-038: the drawn structural RV32 machine` as
a downstream node. Issue #326 is now **closed** (`state_reason:
"duplicate"`, closed 2026-08-04, absorbed into #202 — confirmed by
directly fetching #326). The fourth comment on #343 acknowledges this
("that edge is `blocks: [202, 345, 347]`") but the correction lives only
in a comment; the body's machine block and mermaid graph were never
edited. The issue's own Completion Criteria demand "Machine block, roster
table, and mermaid graph agree with reality at close" — they do not agree
with reality *now*, four days after filing, and nothing in the process
forces the body to converge with the comment thread.
**Recommendation:** edit the issue body directly when an edge's endpoint
changes; a comment-only correction is not discoverable by someone reading
the body in isolation, which is how most of this issue will be consumed.

**4. The independence property the whole feature exists to buy is explicitly, admittedly unfunded.**
Open Question 4: "Bus factor 1 on both sides. Even with no shared code, one
author's misreading of the specification lands identically on both
implementations. The mitigation is an external conformance corpus, not
more tests... **Rides along**, but it is the risk the whole feature's
value rests on." `ARCHITECTURE.md` independently confirms JLS "is a
single-maintainer pedagogy tool." Criterion 6's dependency-disjointness
check (`deps(M_ref) ∩ deps(M_drawn) ⊆ B`) only proves the two
implementations don't share *code*; it says nothing about whether the one
person writing both made the same conceptual mistake in both places. The
issue diagnoses exactly this failure mode in its own Abstract as the
reason the counterparty is needed, then leaves the actual fix (an
external conformance corpus) as an unfunded "rides along" item rather
than a required task.
**Recommendation:** either fund adoption of an external ISA conformance
corpus as a named task under this feature, or state plainly in the
Capability Statement that the independence property delivered is
*mechanical* (no shared code) rather than *epistemic* (no shared
misunderstanding), so downstream consumers (#295, #301) don't over-read
the guarantee.

**5. The stated verification for "independence survives a refactor" cannot catch the failure mode the feature cares most about.**
§5 prediction 2: "Perform a mechanical rename across the drawn side and
assert `jls.mach` does not compile-fail and does not change behavior."
This only detects *accidental* coupling introduced by an IDE-driven rename
or a shared import. It is silent on an engineer transcribing the drawn
machine's decode table into `jls.mach` by hand — no import, no shared
symbol, same bug reproduced twice — which is precisely the "duplication is
the deliverable... the first time the runner imports a helper from the
drawn side, the parity claim quietly becomes a self-comparison" scenario
§2 names as the reason to reject code reuse. Under time pressure to hit
the 2-week TASK-0070 estimate, hand-transcription is the *easiest* way to
pass every stated check (architecture rule green, rename test green) while
defeating the actual goal.
**Recommendation:** add a criterion that inspects for structural/logical
duplication (e.g. an independently-authored differential fuzz corpus
neither side was tuned against, or a second-author review gate on
`jls.mach`'s decode logic), not just import-graph disjointness.

**6. Cost and scope are in tension: the funded tasks explicitly do not cover what Completion Criteria require.**
§5 demands the runner "boots the pinned image end to end... and reports
architectural state at instruction granularity for the whole run" —
i.e., an instruction-complete implementation and a bootable guest stack.
§ Cost is explicit that the two funded rows (TASK-0070 2wk + TASK-0071
2wk = 4wk) cover only "the package seam and the image pipeline," not "an
instruction-complete architectural model, a runner that executes a real
guest to a shell, and the debugging of a boot that does not work" — a
residual the issue itself prices at **10-18 additional maintainer-weeks**
(a 3.5x-5.5x gap over the named tasks) and defers as Open Question 5,
unresolved. The issue nonetheless declares itself a "source" ready to
"start immediately." Under the stated Definition of Done, none of the
checkboxes that matter (the instruction-complete model, the boot-to-shell
run) can be checked by the two named children alone; roughly three-quarters
of the feature's own priced cost has no task, no owner, and no plan beyond
"open question."
**Recommendation:** either file the residual 10-18 mw as named follow-up
tasks before closing this issue, or explicitly descope the
instruction-complete/bootable requirements out of this feature's
Completion Criteria and re-derive #295's and #301's sufficiency arguments
accordingly (which the issue's own re-planning protocol already requires
for exactly this situation, but which has not been done).

**7. The case against reusing the existing in-tree reference implementation rests on a document that isn't in the repo, and contradicts the repo's own committed documentation.**
`riscv/riscv_ref.py` already exists and its own `riscv/README.md`
describes it as verifying "the hardware against an **independent**
reference emulator." The issue's stated reason for building a new one
instead (a quote from `docs/machine-calibration.md:87`: the existing
emulator "was written by the same author as the design under test, so it
is a self-consistency oracle, not an independent one") cites a file that,
per finding 2, does not exist in this checkout. As things stand, the only
committed, readable project documentation on this question says the
opposite of what the issue claims to justify the new build.
**Recommendation:** if `docs/machine-calibration.md` is real, commit it and
cite the specific evidence for why the Python reference fails
independence rather than a single audited line number; if it isn't
committed yet, don't treat its conclusion as settled while funding new
work on the strength of it.

**8. Minor internal tension: "can start immediately" overstates what's actually unblocked.**
§6 and the DAG walk both assert `blocked_by: []` and that "both TASK-0070
and TASK-0071 can start immediately," but Open Question 1's remaining half
(commit the sidecar blob vs. rebuild on demand) is stated three separate
times to "block TASK-0071" pending a maintainer ratification that has not
happened. Only TASK-0070 is actually unblocked today.
**Recommendation:** qualify the "source, start immediately" framing to
name TASK-0070 only, or get the ratification landed before advertising
the feature as fully unblocked.

## What's solid

- The digest-mismatch / absent-sidecar behavior (§4 invariant 4, §5
  prediction 5) is concretely specified and testable, and correctly ties
  into the existing fail-loud loader pattern (issue #314) rather than
  inventing a new error path.
- The coverage-floor and scripts-directory claims that *are* checkable
  against the live tree are accurate: `pom.xml`'s `jls.sim` package rule
  (INSTRUCTION 0.930 / LINE 0.920 / BRANCH 0.845) and bundle floor
  (0.545/0.535/0.505) match `pom.xml:360-471` exactly, the `scripts/`
  listing matches file-for-file, and `src/jls/mach` and
  `scripts/build-guest-image.sh` are both genuinely absent at HEAD. The
  `@NullMarked package-info.java` precedent claim is also true (verified
  in `src/jls/boot`, `src/jls/collab/{crdt,net,op,session}`, though the
  pattern is in fact used more broadly than the five packages cited).
- The three-way scope boundary against #347 (the harness) and #202/#326
  (the drawn machine) is coherently argued and the "different seeded
  failure" distinction between this issue's §5 prediction 1 and #347's
  IC-1 holds up on inspection.

## Bottom line

The two concrete deliverables (a dependency-isolated leaf package plus a
reproducible sidecar image pipeline) are reasonable and worth building.
But the issue's justification leans heavily on evidence a reviewer
literally cannot check — an orphaned commit hash the author already knew
was about to be deleted, and three cited documents that do not exist in
the repository — while its own machine-readable dependency state has
already gone stale within days of filing and the fix lives only in a
comment. Combine that with an admitted-but-unfunded single-author bus-
factor risk sitting under the feature's whole reason for existing, and a
cost band whose top three-quarters has no task attached, and this needs
rework before it should be treated as ready to start: re-pin the
evidence, commit or drop the cited docs, fix the body's stale graph, and
either fund or explicitly descope the residual.
