# Issue #494: Machine calibration, part 2 of 2: guest-side boot facts, the minimum SoC, what is still unmeasured, and how to re-measure (rescued from a branch that will be deleted)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This issue is not a work item; it is a verbatim paste of §5–§8 of a 1,124-line
document (`docs/machine-calibration.md`) that lived only on a branch the
maintainer has ruled will be deleted. The stated purpose is preservation —
"rescued" — for the benefit of ~40+ other filed issues that cite it by line
number. Judged as an *issue*, it has no acceptance criteria, no task, and no
definition of done; judged as a *document*, several of its central claims are
unverifiable from this repository and its packaging (GitHub issue body,
split across two issues, cited by line number from a commit that no longer
exists anywhere reachable) is a fragile substitute for just committing the
file.

## Findings, by severity

**1. (High) The "≥77 filed issues cite it" claim looks inflated and is stated with no method, which is ironic given the document's own rules.** The issue asserts "at least 77 filed issues cite it, #301 alone eighteen times." A `search_issues` query for `machine-calibration.md` across this repo returns `total_count: 43` — under 60% of the claimed figure, and #301 appears as only one hit among them, not eighteen. §7.3 step 6, quoted verbatim in this very issue, says "a throughput number without its census and its clocking regime is not a measurement; it is a rumor" — the same standard applied to "77" (no query shown, no date, no distinction between citing-by-path vs. citing-by-line) finds it wanting by its own rule. *Recommendation:* either cite the exact search used to derive 77, or drop the specific number and say "several dozen."

**2. (High) No acceptance criteria; the issue is trivially "closeable" without any of the substance it flags getting resolved.** The body is "Everything from here down is the source document, verbatim" — there is no checklist, no linked PR, no assignee, nothing that distinguishes "content preserved" from "the load-bearing gaps in §6 (α never measured, cross-platform determinism unverified, the `maxTime` event-drop adjudicated) actually got addressed." Closing this issue proves only that text was pasted, not that the 11 sub-items in §6 (α, events/cycle, tty echo path, etc.) were ever acted on. *Recommendation:* either mark this issue as archival/documentation-only with no closure expected, or split "preserve the text" from "track the 11 open experiments" so the latter has real DoD.

**3. (High) Using a GitHub issue body as the durable home for normative rules is the wrong tool, and the issue doesn't explain why the file wasn't just committed to `docs/`.** The issue reproduces §2.5, §2.6, §4.5, §4.6, and §7.3-step-6 as rules other issues "invoke by name," including "**Step 6 is not optional.**" Every other normative doc in this repo (`docs/simulation-semantics.md`, `docs/batch-interface.md`, `docs/file-format.md`) is listed in README.md's Documentation section and is subject to `HelpTopicsTest`'s link-integrity checking and ordinary code review via PR diffs. An issue body has none of that: no CI can assert its content didn't drift, no PR review gated its accuracy, and a single accidental edit (by a maintainer, a bot, or GitHub's own markdown mangling) silently invalidates every `machine-calibration.md:NNN` citation in the ~40 issues that depend on this table, with nothing to catch it. The obvious alternative — commit the rescued text to `docs/archive/machine-calibration.md` on master before the branch dies — is never discussed or ruled out.

**4. (Medium) The core "measured" numbers in §5 are unfalsifiable from this repository — no artifact, no script, no harness backs them.** Every instruction-count and RAM figure in §5.1–5.4 is attributed to work "built and patched in-session" on external tools (`cnlohr/mini-rv32ima`) with no log, script, or data file linked or committed anywhere reachable. §7.2 describes three harnesses needed to reproduce §2/§3 ("Event counter," "Phase timer," "Census") as subclassing `BatchSimulator` and overriding `afterEvent` — a real, correctly-cited extension point (`src/jls/sim/Simulator.java:269`, overridden at `src/jls/sim/BatchSimulator.java:140`, confirmed accurate) — but no such harness class exists anywhere under `test/` or `src/jls/sim/` today. The document is precise to 6+ significant figures ("38,046,720") for numbers nobody reading this repo can check.

**5. (Medium) §6.9's "silent corruption" framing overstates a bug that has no present trigger.** It correctly quotes `src/jls/sim/Simulator.java:224-233` (`poll()` → `dupCheck.remove(event)` → `now = event.getTime()` → `if (now > maxTime) { now = maxTime; break; }` — verified verbatim, including line numbers) but the hazard it describes only exists "under any future state capture or resume," a feature not present in this codebase (checkpointing in `SimpleEditor.checkpointWriter` saves circuit *files*, not in-flight simulator state). The issue itself concedes "Today it is a curiosity," which is honest, but listing it under "What is still unmeasured and load-bearing" alongside genuine open experiments (α, events/cycle) blurs a speculative future-proofing note with active feasibility blockers.

**6. (Low) The line-number citation table is unverifiable and has no error-detection mechanism.** The whole "How to resolve a `machine-calibration.md:NNN` citation" section depends on this issue's markdown reproducing a defunct file's exact line breaks at commit `2d0ca9d` on a branch that, by the issue's own account, will be deleted. `git cat-file -e HEAD:docs/machine-calibration.md` fails in this checkout (confirmed) — there is no way to cross-check the table's line/section mapping against a source of truth; any transcription slip is permanent and silent.

**7. (Low) Heavy cross-issue dependency load for what reads as a single "rescue" issue.** Understanding this issue's own boot-time numbers requires also reading part 1 (#496, confirmed open and correctly titled as referenced), plus the companions #484/#485, and likely #495/#499 for the parity-contract material §6.10/§6.11 point at. None of that is a defect in isolation, but it means #494 cannot be evaluated or acted on alone.

## What's solid

- Every claim anchored to actual source code checks out exactly, including line numbers: `Memory.DENSE_CAPACITY_LIMIT = 1 << 22` at `src/jls/elem/Memory.java:1224` and the dense/sparse gate at `:1234`; `FileAbstractor.MAX_CIRCUIT_TEXT_BYTES = 64L << 20` at `src/jls/FileAbstractor.java:65`; `JLSInfo.defaultTimeLimit = 100000000` at `src/jls/JLSInfo.java:69`; `Simulator.maxTime` typed `long`. This is unusually precise sourcing and gives real confidence in the code-anchored half of the document.
- The document is disciplined about labeling each number's epistemic status (measured/estimated/derived/never-measured) in the §8 table rather than presenting a single confident figure — and it surfaces its own internal disagreements (121.5 vs. 245.5 vs. 243.1 events/cycle; k = 1.07 vs. 1.8) instead of picking one and hiding the conflict. That honesty should survive any rework.
- The "on master vs. branch-only" reachability table in the issue's "What was dropped" section was spot-checked and is accurate: `docs/simulation-semantics.md`, `docs/capability-roadmap/README.md`, `docs/capability-roadmap/keystone-c-performance.md` and `ARCHITECTURE.md` all exist on master; `docs/parity-contract.md` and `docs/virtual-hardware-parity.md` do not.
- The RV32I byte-lane gap cited from `docs/capability-roadmap/README.md:88-90` correctly traces to `riscv/README.md:60-61`'s own scope note; not a fabricated cross-reference.
