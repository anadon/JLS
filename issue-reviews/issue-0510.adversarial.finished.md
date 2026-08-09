# Issue #510: Niche comparison survey, August 2026: head-to-head teardowns, winnable segments per competitor, and the universal gates to drawing their users and developers
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

This is a synthesis/survey issue (not itself an actionable work item): a 12-dimension,
1-5 score matrix comparing JLS against six competitor projects, three follow-up
comments patching a spawned traceability table, and a closing companion relationship
to #508. It does no work itself and commits the repo to nothing directly — but its
"adversarially verified against the repository" framing is the thing under test here,
and on spot-check that framing does not hold up in two places that matter for the
scores it produces.

## Findings, most severe first

### 1. [HIGH] The issue's own "adversarial verification" correction is itself false — `-board`/`-pins` are already live CLI flags

The HDL-interop footnote (²) states: *"a Yosys-JSON netlist importer (`jls.hdl.imp.NetlistImporter`, #61) and a CLI board/PCF flow (`-board`/`-pins`) exist at head of main with test suites — but are wired to no CLI flag or menu, hence unreachable by users."*

This is false for the board/PCF half. `src/jls/JLSStart.java:782-786` registers `-board` and `-pins` in the live `FLAGS` table consumed by the parser (`FLAGS` is iterated at `JLSStart.java:852` and again at `:1194` for `-h` output):

```
782: new FlagSpec("board", Arity.REQUIRED, "name", "a board name",
783:   "with -export and -pins: also write a pin-constraint file (.pcf) for the named FPGA board (supported: " ...
785: new FlagSpec("pins", Arity.REQUIRED, "file", "a pin-bindings file",
786:   "with -export and -board: port-to-pin bindings, one 'port pin' ...
```

`git log -S'"board"' -- src/jls/JLSStart.java` shows this landed in commit `9e809f2` ("Board-aware HDL export, first slice: iCEstick PCF constraints (#213)") on **2026-07-26**, nine days before #510 was filed (2026-08-04). The board/PCF flow is fully reachable from the CLI today; only the Yosys-*import* half is genuinely unwired.

**Impact:** the issue explicitly markets this footnote as an adversarially-checked correction to the score matrix ("Adversarial verification found the 'no HDL import' claim stale... 3-4 at head-of-main; releasing and surfacing them is cheap score"). Half of the correction is wrong in the direction of understating what already ships, which weakens confidence in the HDL-interop score of 3 and in every other "verified against the repository" claim in the issue that this reviewer did not independently check.

**Recommendation:** correct the footnote to say the board/PCF flow is already CLI-reachable (only surfacing it in help/README/menu discoverability is outstanding), and re-derive whether that changes the JLS HDL-interop score or its "cheap score" cost estimate.

### 2. [HIGH] The same footnote overstates the Yosys importer's readiness — it is not "cheap," it is most of an unstarted feature

The footnote treats `NetlistImporter` as done modulo wiring. But its own cited source, issue #61 (open, `tier:feature`), describes an admitted skeleton: the mapper "so far" covers only module ports, `$not`/`$and`/`$or`/`$xor`, `$mux`, and constants (`src/jls/hdl/imp/NetlistImporter.java:253`, `"...increment does not yet realize"`), and explicitly rejects Register, Memory, Adder, TriState, and hierarchy with a "not yet realize" message (`NetlistImporter.java:41,188,253,740`). #61's own decomposition lists **five outstanding slices — subprocess runner, `jls_map.v` completion, mapper completion, import UI, parity suites — all marked "not filed"** with zero linked PRs.

Grepping the tree confirms `NetlistImporter` is referenced from nowhere outside its own package and tests — not because a switch/menu is missing, but because the feature behind the switch does not yet do most of what a Verilog import needs to do.

**Impact:** "releasing and surfacing them is cheap score" materially misrepresents the cost of moving the HDL-interop dimension. Wiring a CLI flag to an importer that rejects registers and memories would not produce a usable Verilog-import feature, and shipping the real thing means funding most of an open, unscoped feature (#61), not a wiring task.

**Recommendation:** separate "board/PCF discoverability" (genuinely cheap, and already CLI-reachable per finding 1) from "Yosys Verilog import" (genuinely most of #61) in the footnote and in whatever capstone/feature inherits this line item.

### 3. [MEDIUM] The scoring matrix's evidentiary base lives on an unmerged side branch

The issue states the full teardown evidence is "committed at `docs/reviews/evidence/2026-08-niche-survey/` on `claude/jls-project-review-505pnf`." That branch exists on the remote and does contain the eight teardown files, but `git merge-base --is-ancestor origin/claude/jls-project-review-505pnf origin/master` returns false — **it is not merged into master.** The entire quantitative backbone of this issue (the 12×7 score matrix, every "score justified from sources" claim) rests on a branch that could be force-pushed, rebased away, or garbage-collected without anything on master noticing.

This is not a hypothetical risk in this repository: #508 (this issue's explicitly named companion) itself flags exactly this pattern as a live problem — *"Stop the corpus bleed (≈1 mw): re-land the two stranded D6 fixes (#488/#491); commit `docs/plan/**` to master before the branch dies (#493)"* — and comment 3 on #510 (2026-08-08) separately warns that *"any issue in the #295-#492 sweep citing `evidence_commit: 2d0ca9d` should be treated as unverified until re-derived by content"* because that commit **no longer resolves in the repository at all.** #510 repeats the identical anti-pattern its own companion issue is trying to stop.

**Recommendation:** land `docs/reviews/evidence/2026-08-niche-survey/` on master (or a durable tag) before this issue is treated as closed/actioned; otherwise the score matrix is unauditable within a few branch-cleanup cycles.

### 4. [MEDIUM] "Publish the benchmark" acceptance criterion is underspecified and partly already satisfied, inviting a cosmetic close

Universal gate 4 says: *"The harness exists (`riscv/bench_kernel.py`); Digital's 120 kHz claim wins by default because JLS publishes nothing."* In fact `docs/capability-roadmap/keystone-c-performance.md` already contains an extensive, dated, harness-derived performance write-up (ns/op costs, µs-scale levelization timings, profile breakdowns) built from this exact benchmark: its header says *"It produced the kernel measurements in `docs/capability-roadmap/keystone-c-performance.md`."* "JLS publishes nothing" is not accurate — an internal engineering write-up exists; what's actually missing is a README/marketing-facing, competitor-comparable throughput number.

Because the gate doesn't say what "publish" must produce (a single comparable-to-Digital's-120kHz number vs. dumping the existing internal jargon-heavy doc into a public page), a literal implementer could satisfy the letter of this gate by linking the existing roadmap doc without ever producing a number a prospective switcher from Digital could compare — which is the actual competitive claim gate 4 is trying to win.

**Recommendation:** state the acceptance criterion concretely — e.g. "README/docs publish one headline throughput number, on a named workload, directly comparable in units to Digital's 120 kHz claim" — and note the existing roadmap doc as a starting point, not a gap.

### 5. [MEDIUM] Hidden assumption: engineering work alone is assumed to move real adoption from ~zero

The developer-draw section assumes Digital's "named, reachable pool of demonstrably motivated, rejected contributors" (26 open PRs, #1464/#1470 closed unmerged) would prefer to redirect their effort to JLS. No outreach, survey, or expressed interest from anyone in Digital's community is cited anywhere in the issue. This issue's own companion #508 records JLS's actual current external footprint as **3 stars, 9 forks, 0 external issues, 1 merged external human PR since 2014**, and that the two 2026 external PR authors on *this* repo also bounced unmerged. The "positioning statement the survey supports" — *"JLS is the maintained, modern successor in the Digital tradition"* — is presented as a strategic finding, but it is an assumption about a market response to a repo with zero demonstrated ability to onboard or retain outside contributors so far. Nothing in the issue's own evidence tests that assumption; it tests competitor weakness, not JLS's demonstrated pull.

**Recommendation:** the "dev-draw play" items 3-4 (invite Digital's bounced PR authors by name) are cheap enough to just try and observationally validate the assumption before more strategy is built on it — flag this explicitly rather than stating the successor positioning as settled.

### 6. [LOW] Score-matrix precision is inconsistent with its stated method

Scores mix whole integers with a lone half-point ("Digital on-ramp: install = 2.5") across an otherwise 1-5 integer scale, with no stated tie-breaking or half-point rule, and no inter-rater or blinding discipline is described for a single-reviewer-per-teardown process scoring one's own project against six others on an identical rubric. This doesn't invalidate the scores, but a "score justified from sources" claim implies a repeatability the issue doesn't actually demonstrate — a different reviewer applying "identical severity" is not verifiable from the issue body alone (only from the branch evidence in finding 3, which is not durably stored).

### 7. [LOW] Issue has already needed two rounds of self-correction in four days

Comment 1 (2026-08-04) builds a 41-row gap→owner traceability table; comment 2 (2026-08-04, same day) withdraws a planning-ratchet recommendation the survey had inherited from #508; comment 3 (2026-08-08) re-audits the same table and finds one row (share-a-circuit-by-link) had no real owner, plus flags that the pre-tier generation's evidence commit (`2d0ca9d`) no longer resolves in the repo. None of these are fatal, but three corrective passes on a single issue in four days is a signal the initial filing was published before its own claims were fully checked — consistent with findings 1 and 2 above.

## What's solid

- **The `SimpleEditor` 5,852-line claim is exact** — `wc -l src/jls/edit/SimpleEditor.java` returns 5852, matching the issue's "worse than anything in Digital's codebase" framing precisely.
- **The empty first-run / no discoverable examples claim checks out** — only one non-test `.jls` file exists in the tree (`riscv/gui/cpu.jls`), and no tutorial-menu wiring was found in `SimpleEditor.java`.
- **The self-critical "adversarially verified... 11 of 12 confirmed, corrections below" framing is the right practice shape** — a survey that checks its own baseline row against the repo, even though (per findings 1-2) the check itself had errors, is a healthier pattern than an unverified competitor comparison would be.
- **Explicitly deferring execution to twelve newly-filed capstones (#511-#522) rather than making #510 itself an actionable item is reasonable process** — this issue is correctly scoped as a synthesis document, not a task.

## Note on scope of this review

#510 is a survey/synthesis issue with no direct acceptance criteria of its own; the substantive engineering commitments live in the twelve spawned capstones (#511-#522) and in existing issues it re-ranks (#61, #357, #504, etc.). This review evaluates #510's own factual claims and methodology, which is where its adversarial value lives — the spawned capstones each need their own review pass.
