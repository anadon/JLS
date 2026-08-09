# Issue #301: CAP-02: a CPU drawn in the JLS editor boots Linux to a shell and answers typed commands against the same golden the behavioral tier produces
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

A capstone tracking issue: a drawn RV32 machine (`machines/rv32-soc.jls`) must boot Linux to a shell both "behaviorally" (fast reference model) and "structurally" (gate-level, slow) and produce byte-identical transcripts against one shared golden. It declares 16 required feature issues (#317–#364), 9 acceptance criteria, 9 numeric kill criteria, and a 159–253 "maintainer-week" (mw) cost band, and it has already been rewritten five times in two days (5 comments, 2026-08-03 to 2026-08-04) via a `REPLAN:`/adjudication protocol. Zero of the 16 required features are implemented at HEAD; this is pure planning.

## Findings, most severe first

**1. The load-bearing evidence base does not exist anywhere in this repository.** The issue and its comments cite `docs/machine-calibration.md` (by line number, repeatedly: `:517`, `:1118`, `:733`, `:1119`, `:85`, `:83`, `:516`, `:577`), `docs/parity-contract.md:3`, and `docs/plan/evidence/BRIEF.md` (the source of "maintainer ruling D15," quoted verbatim as *"File. Files will also require 'import to subcircuit' to nest systems."*) plus a `docs/plan/features/` corpus of per-feature prerequisite tables. None of these paths exist in the working tree or in `git log --all` for those paths:
```
$ find . -iname "machine-calibration*" / -iname "parity-contract*" / -type d -iname plan
(no output — nothing found)
$ git log --all --oneline -- docs/machine-calibration.md docs/parity-contract.md
(no output — never committed)
```
Every quantitative claim this issue's cost band, cadence decision, and kill-criteria thresholds rest on (the 2.26× speedup, the 1.66–1.72h central structural-boot figure, the "12 events/instruction" behavioral figure, the "93.0/92.0/84.5" coverage bar, the maintainer rulings D13/D15 themselves) is sourced to files a reviewer cannot open. This is not a nit — the issue explicitly frames its own credibility as resting on being checkable ("a cold reviewer can now check the cadence decision instead of taking it on assertion"), and that check fails at the first file open.
**Recommendation:** either commit the evidence documents this issue depends on, or strip every citation down to what's actually verifiable in-repo, before any REPLAN comment is trusted as authoritative.

**2. A specific factual claim is directly contradicted by the checked-out repository.** The "Prior art" section states, quoting the (nonexistent) calibration doc: *"the existing `RiscvCpuGoldenTest`'s regeneration path as already broken — it cites `riscv/examples/sum1to10.s` and `riscv/README.md`, and 'Both are deleted'."* Both files are present at HEAD:
```
$ wc -l riscv/examples/sum1to10.s riscv/README.md
  10 riscv/examples/sum1to10.s
 153 riscv/README.md
$ grep -n "sum1to10\|README" test/jls/RiscvCpuGoldenTest.java
24: * <p>The fixture {@code test/fixtures/riscv-sum1to10.jls} is the CPU with the
25: * program in {@code riscv/examples/sum1to10.s} baked into its instruction ROM:
38: * <p>See {@code riscv/README.md} for how the circuit and fixture are generated.
```
and `git log` shows `sum1to10.s` was added, never removed (`ed48866`). Either the issue is asserting a hypothetical future state ("D5 deletes...") as present-tense fact, or the citation is simply wrong. Either way it's used as supporting evidence for a scope decision (re-homing the regeneration recipe onto #278/#335), so the error isn't cosmetic.
**Recommendation:** re-verify every "X is deleted/broken" claim against the actual tree before it's used to justify closing or re-homing scope.

**3. Self-contradicting arithmetic left unreconciled in the live text.** Open Question 5 says, in one sentence: *"#295's roster and this one share **twelve** rows (#353, #354, #317, #335, #319, #357, #322, #362, #325, #324, #343, #347, #364, #327 — **fourteen** in fact...)."* The parenthetical lists 14 issue numbers while the sentence claims "twelve," and the correction is folded in rather than fixed — in an issue whose entire operating discipline is "every number that moved is shown with its arithmetic." This is exactly the kind of drift the issue claims to guard against (see the standalone-cost-band bug it self-reports fixing in the Abstract) recurring one paragraph later.
**Recommendation:** fix the sentence; audit the rest of the arithmetic-heavy prose for the same class of error rather than assuming the two self-corrections already caught (cost band, cadence justification) were the only ones.

**4. Feasibility: the cost is a multi-year full-time commitment on a project whose own architecture doc says it can't absorb that.** `ARCHITECTURE.md`'s "Internationalization: non-goal" section states plainly: *"JLS is a single-maintainer pedagogy tool... PRs adding partial i18n scaffolding will be declined"* — the project explicitly manages scope around one person's bandwidth. This issue's own cost band is **159–253 maintainer-weeks** (≈3.1–4.9 person-years of full-time work) for this capstone alone, and the issue itself records that it is one of "nineteen capstones filed in this pass" (#295–#313) sharing a pool of 57 features (#314–#370). There is no discussion anywhere in the issue of how a solo maintainer sequences a multi-year program like this against ordinary bug fixes, releases, and the other 18 capstones — the marginal-cost sharing arithmetic (117–186 mw) only nets out cost *within* this capstone's own dependency graph, not against the maintainer's total available time.
**Recommendation:** state, even roughly, the assumed timeline/staffing model this cost band implies, and reconcile it against the "single-maintainer" framing the rest of the repo's docs take as settled.

**5. Kill criteria are graded against numbers admitted to be unmeasured, and the reproduction script for the one number everything divides by is scheduled for deletion.** KC-02-1 compares the "live console" claim against **12** events/retired-instruction, explicitly labeled *"modelled not measured"*. Open Question 8 separately concedes: *"the CPU-scale performance anchor every wall-clock figure in §2 rests on was never tracked... and D5 deletes the script that produced it"* — i.e., the artifact needed to regenerate the number this issue's entire cadence/cost arithmetic (the 2.26× stack claim, AC-9's wall-clock gate, AC-2's cadence branch) divides by is being removed by a separate, apparently already-decided action (D5) before the residual measurement work (#335) can re-derive it independently. A kill criterion whose input cannot be reproduced by anyone but its original author is not a functioning safety valve.
**Recommendation:** land #335's tracked calibration fixture (or preserve the D5 script) *before* deleting the only reproduction path, not concurrently with an unrelated cleanup ruling.

**6. AC-1/AC-2's byte-identical golden check is gameable via the open-ended "exclusion set."** Open Question 3 proposes handling every time-derived kernel-boot artifact (`printk` timestamps, `lpj=`, etc.) via "a printed, ratcheted exclusion set with a stated reason" that grows as needed. There's no cap on what can be added to it, no independent review gate distinct from the PR that adds the feature it's excluding, and no criterion for when an exclusion is legitimate (genuinely nondeterministic) versus convenient (masking a real divergence). `cmp out.txt golden` exiting 0 is only as strong as that list is honest — the acceptance criterion as specified can pass while real behavioral drift is silently carved out.
**Recommendation:** cap or externally audit the exclusion set, or specify a byte-mask/regex diff instead of an open-ended list that any contributor can extend to make CI green.

**7. KC-02-1's stated remedy at the first threshold is cosmetic, not corrective.** Crossing 25 events/instruction "retires the word 'live' from all documentation" — a wording change — while "the program continues" unchanged. A kill criterion whose first tier changes prose rather than engineering direction weakens the claim that these are genuine stop/reduce gates rather than a compliance-theater checklist; only the second tier (46, 2× budget) has real teeth (cut the GUI console).
**Recommendation:** either fold the 25-threshold into a real engineering response (e.g., trigger #362's optimization work explicitly) or drop it as a distinct criterion.

**8. Process overhead is already outrunning implementation.** Five comments in roughly 26 hours (2026-08-03 00:39 through 2026-08-04 02:57) rewrite the dependency graph, reverse an edge, delete and re-add a cost caveat, retire and reinstate a "beneficial vs. required" distinction (added by D13, itself walking back nine places the issue had just added it), and re-verify all 16 roster rows — against a required-feature set that is 0% landed. This is a lot of bookkeeping churn to sustain before a single line of the 16 features exists; it's a plausible leading indicator that the planning apparatus (REPLAN protocol, mermaid graphs, machine blocks) is consuming maintainer time that competes directly with finding #4's already-tight budget.
**Recommendation:** timebox planning REPLANs, or accept that some of the ~250mw budget is itself process overhead and say so in the cost section.

**9. (Minor/scope) Licensing of the guest image sidecar is unaddressed.** D15 settles the guest image as "a sidecar file with a recorded digest" pinned into the repo/fixtures for CI goldens — i.e., a Linux kernel image (GPL-2.0) plus initramfs/BusyBox. The issue is otherwise careful about provenance and licensing elsewhere in the project (see `README.md`'s signing/attestation discussion, and the GPLv3 in-process-linking caution in `ARCHITECTURE.md`'s plugin-trust section), but nowhere discusses source-availability obligations, redistribution terms, or repo-size impact of committing a prebuilt kernel/initramfs binary as a tracked fixture.
**Recommendation:** add a line to Open Question 3/AC-1 on how the pinned kernel/initramfs satisfies GPL source-corresponding-to-binary obligations for whatever gets committed or hosted.

## What's solid

- **AC-3 (clock-period falsification guard)** is a genuinely good design: re-running with a 10× clock period and requiring byte-identical output is a real, cheap check against the golden silently encoding wall-clock time rather than logical behavior.
- **The numeric kill-criteria framing in general** (KC-02-2 through KC-02-9) is more disciplined than most planning issues — thresholds are stated as numbers, not vibes — even though finding #5 shows the numbers behind them are currently unverifiable.
- **Recognizing and correcting its own edge/count errors in-thread** (the seven removed graph edges, the reversed edge, the standalone-cost-band bug) shows real self-auditing discipline; the process caught real mistakes. It just hasn't caught all of them (see #2, #3).
