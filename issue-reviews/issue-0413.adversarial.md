# Issue #413: TASK-0025: the CPU-scale calibration anchor becomes a tracked, censused fixture and `riscv/` is deleted without taking the measurement basis with it
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what checks out

Several of the issue's load-bearing observations are independently verifiable in
the checkout and are correct:

- **O1** — `riscv/build/k2000.jls` is genuinely absent and untracked; `riscv/build/`
  doesn't even exist on disk. Confirmed.
- **O3** — `git ls-files riscv/` returns exactly 26 paths, matching the enumerated
  list (`.gitignore`, `README.md`, `bench_kernel.py`, `build_cpu.py`, 6
  `examples/*`, `fuzz_diff.py`, 9 `gui/*`, `jlsbuild.py`, `jlsrun.py`,
  `make_cpu.py`, `riscv_ref.py`, `test_primitives.py`, `verify.py`). Confirmed.
- **O4** — exactly four tracked `.jls` files, one of them (`riscv/gui/cpu.jls`)
  inside the directory slated for deletion. Confirmed.
- **O5** — `riscv-sum1to10.jls` at `test/fixtures/…` and `riscv/examples/sum1to10.s`
  citations at `{@code}` (not `{@link}`) in `test/jls/RiscvCpuGoldenTest.java`.
  Confirmed, and the doclint-blind-spot observation is real and useful.
- **O6/O8** — `.gitignore`/`.gitattributes` mechanics as described. Confirmed.
- `riscv/riscv_ref.py` is exactly 975 lines as claimed, and `bench_kernel.py` /
  `build_cpu.py` contain no RNG/seed calls — the generator is deterministic, so
  the census-reproducibility risk the issue itself flags in §Threats to
  Validity ("if `bench_kernel.py` is seeded...") is a non-issue in practice.

## Findings, most severe first

**1. (Critical — feasibility/ordering) Both stated blockers are unlanded; the task is not actually startable yet.**
`blocked_by: [377, 379]` — I fetched both. #377 (TASK-0022) and #379 (TASK-0023)
are both `state: open`, both still list every deliverable as "not filed" /
"does not exist," and #379 is itself `blocked_by` on #377 landing first. #413's
own Method step 2 says: *"Confirm #377 (TASK-0022) and #379 (TASK-0023) have
landed. Their measurements are taken with `riscv/bench_kernel.py`; running the
deletion before them destroys the ability to characterize the fixture that
replaces it."* An engineer who opens #413 today and starts on step 3
("Regenerate `k2000.jls`...") without first checking #377/#379's status has
no guard against violating H3's one-directional ordering constraint — the
issue puts the check in prose, not in anything mechanical. Recommendation:
either add `blocked_by` as a literal GitHub-relationship (not just a
YAML comment) so tooling can enforce it, or state plainly at the top of the
issue "DO NOT START — #377/#379 open" until they close.

**2. (Critical — internal contradiction) The evidentiary foundation the issue cites does not exist in this repository, and the "six documents" claim is provably wrong on the tree as checked out.**
The issue repeatedly asserts `docs/machine-calibration.md` "already exists"
at the evidence commit ("1,124 lines... TASK-0024 fills and corrects it
rather than creating it" — this exact claim is also made independently in
#335 and #377, so it isn't a one-off typo). It does not exist anywhere in
this checkout: `find … -iname "*machine-calibration*"` returns nothing, and
`docs/` lists no such file. Likewise `docs/plan/evidence/BRIEF.md` (the
source cited for "Decision D5, binding") and `docs/plan/REGISTRY.md` (one of
O2's "six documents [that] cite it") do not exist — `docs/plan/` is not a
directory anywhere in the tree. Of O2's claimed six citing documents, only
four are actually present and grep-confirmed
(`AMENDMENT.md`, `keystone-c-performance.md`, `lf-03-causal-debug.md`,
`lf-07-api-and-platform.md`); the other two are phantom. This matters
concretely: Method step "Rewrite every live citation in the six documents of
O2" and Completion Criterion "no live citation of a `riscv/` path anywhere
outside the ratchet's allowlist" cannot be executed against files that don't
exist, and P8 ("`docs/machine-calibration.md` §2 states the fixture's
tracked path and its census") requires *creating* a document the issue's own
scope section says is out of scope to create ("Out of scope: ... filling
`docs/machine-calibration.md` §6 (TASK-0024)" — filling is disclaimed, but
creating the whole file from nothing is not addressed at all). The
`evidence_commit` SHA (`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7`) also does
not resolve in this repo's local git history (`git log` on it: "bad
object") — inconclusive on its own since this is a shallow clone, but
combined with the missing files at HEAD, a picker-upper of this issue has no
way to verify a single one of the dozens of specific line-number citations
(`docs/machine-calibration.md:434`, `:458-463`, `:878-879`, etc., cited
across the linked chain) against real content. Recommendation: before this
task is actionable, someone must confirm whether `docs/machine-calibration.md`
and `docs/plan/` exist on some other branch/commit this checkout doesn't
have, or concede the whole evidentiary chain (D5's binding status included)
is unverifiable and must be re-derived from scratch.

**3. (High — scope conflict / gameable acceptance) Overlap with #728 is acknowledged but left structurally unresolved, and nothing in P1–P8 detects a duplicate fixture.**
The issue's own comment (2026-08-04, restated 2026-08-08) says #728
(TASK-C554-2) may commit the *same* CPU-scale circuit as this task's anchor,
and explicitly concludes: *"Disposition: no merge, no closure. Both tasks
stay open."* None of P1–P8 or the completion checklist checks whether #728
already produced a CPU-scale fixture before this task commits a second one.
P3 ("count elements in the committed fixture; observe equality with the
census constants recorded beside it") only checks the fixture against its
own recorded census — it has no cross-reference to #728's artifact. A
contributor executing #413 in isolation can pass every stated prediction
while creating exactly the two-different-denominators-for-one-claim problem
comment 3 warns about elsewhere in the same issue ("two independently
characterized CPU-scale circuits would give the calibration record and the
published performance doc different denominators for the same claim").
Recommendation: add a prediction that fails if `test/fixtures/` already
contains a CPU-scale entry from #728 at execution time.

**4. (High — gameable acceptance criterion) H4's ratchet is satisfied by "non-empty," which is a one-line bypass.**
§7.5 and P5 require only that `NoRiscvDirectoryReferencesTest`'s allowlist be
non-empty ("so it cannot pass by matching nothing"). That guards against the
*trivial* failure mode (empty allowlist always green) but does nothing to
stop the *realistic* one: a future contributor who reintroduces a `riscv/`
reference can simply add it to the allowlist with any one-line
justification, and the test — which only checks non-emptiness, not that
each entry is one of the originally-approved historical files — passes.
The completion checklist's "each entry states why" is a human-reviewed
bullet, not a machine-checked invariant of the test itself. Recommendation:
have `NoRiscvDirectoryReferencesTest` assert the allowlist equals a fixed,
named set (or is a strict subset with an explicit max size) rather than
merely "non-empty," so growing it requires touching the test, not just a
comment.

**5. (Medium — feasibility/verifiability) Comment 3's k2000 numbers are asserted as established fact about a file that has never existed on disk.**
Comment 3 (2026-08-08) states specific figures for `riscv/build/k2000.jls`
— "6004 clocked cycles, 120 KB circuit, 193 KB `-t` vector," "1551 elements
... 225 logic elements + 297 nets = 522 nodes" — sourced from
`docs/capability-roadmap/keystone-c-performance.md` rather than from a
regeneration in this checkout (the file, per O1, is confirmed absent). Since
the generator is deterministic (finding above, no RNG), a regeneration
*should* reproduce these, but the issue treats them as settled rather than
as the thing P3 is supposed to independently establish. If the shipped
`keystone-c-performance.md` figures were themselves taken under a stale
version of `bench_kernel.py`/`build_cpu.py` (both are 20 KB+ files that
could plausibly have changed since those numbers were recorded), the "census
equality" check in P3 would silently validate against numbers nobody
re-derived. Recommendation: P3 should assert against a number generated in
the same PR that commits the fixture, not against a number copied from a
roadmap doc.

**6. (Medium — scope creep) The task quietly requires authoring a brand-new document, `docs/parity-contract.md`, which doesn't exist and isn't scoped as its own deliverable.**
Method: *"Transcribe the differential-harness design into `docs/parity-contract.md`
**before the code goes**"* — describing `riscv_ref.py` (975 lines), `fuzz_diff.py`,
and `verify.py` (11 directed programs) and their self-consistency-oracle
limitation. Confirmed the file doesn't exist yet
(`find … -iname "parity-contract*"` returns nothing). Writing a new
from-scratch design document — accurately transcribing a 975-line reference
emulator's semantics and a randomized differential harness's guarantees — is
a nontrivial, easy-to-get-subtly-wrong sub-task bolted onto what the Abstract
frames as "commit a fixture and delete a directory." It has no prediction or
falsification check of its own beyond "it exists before the code goes";
nothing verifies the transcription is *accurate*, only that it's present.
Recommendation: either split this into its own task with its own acceptance
criteria (accuracy-checked against the actual Python, not just presence-checked),
or explicitly size the cost estimate to include a careful reading of all
three scripts.

**7. (Low — internal precision) O2's "six documents" count is off given finding #2 above, and O7's "three more of the 26 files" undercounts by the issue's own math.**
O7 says `bench_kernel.py` "imports `riscv_ref`, `build_cpu` and `jlsrun` —
three more of the 26 files," implying 4 files total (bench_kernel.py + 3
imports) are load-bearing for regeneration; verified those four files exist
and the import list is accurate in `bench_kernel.py`'s header. This part is
fine as a narrow claim, but it undersells H1's real exposure: the issue's
own §8 later folds in `fib`/`memtest` example files via comment 1's
amendment (re-homing all six example files, not just `sum1to10`), which is
correctly tracked as an amendment — this part of the issue's self-correction
process worked as intended and is worth calling out as sound.

## What's solid (brief)

- The core ordering argument (H3: generate before delete, because the
  generator is itself deleted) is logically sound and the "delete-first"
  failure mode is real and well-argued.
- The `.gitignore`/`.gitattributes` mechanics forcing `test/fixtures/` as the
  only viable destination (O6) are accurately described and verified.
- The `riscv/gui/cpu.jls` keep-or-lose decision being called out as
  blocking-before-`git rm` (rather than left implicit) is good process
  discipline.
- Falling back to "stop, don't proceed to deletion" as the first failure
  mode in §7.11 is the correct default given H3.

## Recommendation

Do not execute this task yet. First: (a) verify whether
`docs/machine-calibration.md` and `docs/plan/` genuinely exist somewhere
reachable from this repo (a branch, a commit outside this shallow clone) —
if not, the entire D5/evidence-commit framing needs to be re-derived from
files that actually exist, not cited from files that don't; (b) confirm
#377 and #379 have actually landed before regenerating anything; (c) resolve
the #728 fixture-duplication risk with an actual test, not a comment
disclaiming responsibility; (d) tighten the ratchet's allowlist check beyond
"non-empty."
