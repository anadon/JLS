# Issue #264: Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215)
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

This is a feature-tier tracking issue over real, verifiable code: `Boards.ICESTICK`
(`src/jls/hdl/board/Boards.java:34-35`), `PcfEmitter.emit`
(`src/jls/hdl/board/PcfEmitter.java:58-121`), the golden test
(`test/jls/hdl/board/PcfGoldenTest.java:20-34`), `scripts/icestick-handoff.sh`, and
`docs/icestick-bitstream-handoff.md` all exist and match the issue's citations at
`29afb26` (verified: that commit is a real ancestor of HEAD). The CLI flags
`-board`/`-pins` are real (`src/jls/JLSStart.java:111-116, 783-916`). The document is
honest about what's unproven — it explicitly retracts an earlier progress-comment tick
("Handoff script produces a bitstream…") once it noticed the CI evidence was only a
stub-PATH selftest. That self-correction is a genuine strength. The problems below are
about what remains open, not about fabricated claims.

## Findings (most to least severe)

### 1. Core deliverable requires physical hardware nobody is committed to providing
The Definition of Done's two hardest boxes — "Handoff script produces a real bitstream
for the sample circuit on each board" and "Manual hardware flash recorded with board +
tool versions per board (#215 P2)" — both require a person with an actual iCEstick and,
for Stage 2, an actual ECP5/ULX3S board, running the real toolchain and physically
flashing hardware. Nothing in the issue (machine block, planned_tasks, Open Questions)
names who owns that hardware or budgets for acquiring it. `README.md`/`ARCHITECTURE.md`
describe JLS as a "single-maintainer pedagogy tool." Open Question 2 in the issue itself
concedes CI-based real-toolchain smoke is unresolved ("Recommended default: attempt a
non-required CI lane… fall back to a recorded manual run"). Until someone is named as
hardware owner, these two DoD boxes have no path to being checked short of a maintainer
personally buying and wiring up two FPGA boards — a cost/feasibility risk the issue
does not surface as a risk.
**Recommendation:** either name a hardware owner/procurement plan in the issue, or
descope the physical-flash requirement to a `WAIVED:` with a named successor, per the
issue's own rule ("Every skipped or waived criterion carries a WAIVED: comment").

### 2. Unresolved cross-issue ownership collision on the exact scope this issue plans to do next
The 2026-08-04 comment (`5175757103`) flags, in its own words: *"Three issues now point
at one task; it needs one recorded owner."* Specifically: this issue's `planned_tasks`
lists "Stage 2 — ECP5 (ULX3S-class)… `Boards` entry + LPF emission… extend handoff
script with nextpnr-ecp5 -> ecppack" as owned here, while #359 rosters the identical
work as TASK-0052 (#416), and #599's AC-4 independently assumes #416 owns it. The
comment's own recommended fix ("file TASK-0052 as #264's child… striking its roster row
by REPLAN") has **not been applied** — the issue body at the time of this review still
lists Stage 2/ECP5 as an unfiled `planned_tasks` entry with no REPLAN reconciling it
against #359/#416. A second commenter thread (`5181391231`, same day) independently
notes this issue "has no task coverage of its own" and that seven downstream GUI issues
(#632, #634, #636, #638, #640, #643, #647) already assume a pipeline this issue has not
actually filed as buildable work. **Concretely: if a contributor picks up Stage 2 from
#264's planned_tasks at the same time another picks up TASK-0052 from #359, duplicate
ECP5/LPF implementations land.** This is not a hypothetical — it's the exact failure
mode the issue's own re-planning protocol exists to prevent, and it is currently live.
**Recommendation:** block starting Stage 2 work until the REPLAN reconciling #264/#359
ownership actually happens, not just gets recommended in a comment.

### 3. "Real bitstream" evidence criterion is self-attested prose with no artifact to falsify it
Integration Criterion 1 (§5) is *"pinned by: a recorded run (commands + tool versions)
in the docs recipe"* — i.e., someone types values into the Markdown table at
`docs/icestick-bitstream-handoff.md:117-119` (currently all `_TBD_`). There is no
requirement to attach a build log, a checksum of the produced `.bin`, a CI artifact, or
even a photo of the flashed board. As written, the acceptance criterion is satisfiable
by any contributor filling in five plausible-looking tool-version strings and a date,
with nothing downstream to catch a fabricated or copy-pasted entry — which is exactly
the failure mode this issue's own §2 "Evidence correction" paragraph just caught once
already (a ticked box for work that turned out to be stub-only). The DoD does not
require the same auditing rigor for the *replacement* evidence that it demanded of the
one it just retracted.
**Recommendation:** require at minimum a CI artifact or checksummed `.bin` alongside
the manual-flash table row, or an explicit statement of why prose-only attestation is
acceptable for a pedagogy tool's "supported board" claim.

### 4. `requires_tasks: []` + "planned_tasks empty" DoD box is gameable by filing without doing
Completion Criterion 1 reads: *"Every entry in `requires_tasks` closed as landed, or
descoped… `planned_tasks` empty (each resolved to a filed issue or descoped)."* Read
literally, this box is satisfied the moment the two `planned_tasks` rows become filed
child issues — it does **not** require those children to actually land. Filing two
empty placeholder issues ("iCEstick hardware evidence" and "ECP5 Stage 2") and pointing
`planned_tasks` at them would tick this box while zero additional work has happened.
This is caught in practice by the *other*, more concrete DoD boxes (hardware flash
table, real-bitstream criterion) still being separately unchecked — so the issue as a
whole isn't fully gameable — but this specific box is a weak proxy that a hurried
closer could point to as if it meant something it doesn't.
**Recommendation:** rephrase to "closed as landed, or descoped" for planned_tasks too,
removing the "resolved to a filed issue" escape hatch, or explicitly note elsewhere
that filing alone never substitutes for the hardware-evidence boxes.

### 5. Self-imposed serialization convention risks stalling code-ready work on a hardware bottleneck
§6 states Stage 2 (ECP5) is "technically independent" of the iCEstick real-toolchain
evidence task — "the emission and script work touch disjoint code paths" — yet the
mermaid graph and the "board fully done before next board" convention keep P1
(hardware-gated) ahead of P2 (code-only) as the documented default ordering. Combined
with Finding 1 (no named hardware owner for P1), this means Stage 2 — which needs no
hardware to write LPF emission and extend the shell script — is conventionally blocked
behind a step that has no committed timeline. The issue does correctly mark this
convention as "not necessity" and allows a scheduler to break it with a REPLAN, so this
is a soft risk, not a hard defect — but combined with Finding 1 it is the likely
critical path if nobody intervenes.
**Recommendation:** if no hardware owner materializes for P1 within the current
milestone (M3), REPLAN to run P2 concurrently rather than let the convention stall the
whole feature.

### 6. Consolidation lineage is asserted, not independently checkable from this issue alone
The Abstract claims #213 and #215 are "closed" and that this issue "consolidates the
remaining scope," with the drift narrative (PCF-first vs ECP5-first) given as the
rationale. I did not re-fetch #213/#215 to verify their closure state or that their
bodies actually match this characterization (out of scope for a single-issue review,
but worth flagging): if #213 or #215 were reopened or amended after this consolidation,
`related: [59, 61, 213, 215]` being "non-blocking" per the ordering-cycle walk means
this issue would not surface that drift automatically. Low severity since `related`
edges are explicitly documented as non-blocking by design, but it is a place a
consolidation issue could silently go stale.
**Recommendation:** a periodic check that #213/#215 remain closed-as-consolidated would
close this gap cheaply.

## What holds up
- All file/line citations in §2 and the landing-evidence list were spot-checked against
  the repository at the pinned commit and are accurate (`Boards.java`, `PcfEmitter.java`,
  `PcfGoldenTest.java`, `icestick-handoff.sh`, `ci.yml:54-56`, the docs table) — no
  fabricated evidence found.
- The "no JLS-side bitstream code" invariant (#215 H2) is honestly upheld: the script is
  a thin wrapper (`scripts/icestick-handoff.sh:139-157`), consistent with
  `ARCHITECTURE.md`'s recorded external-tool subprocess-boundary decision (#222).
- The self-correction of the "Handoff script produces a bitstream" tick (§2's "Evidence
  correction" paragraph) is exactly the right behavior when a claim outruns its evidence
  — a strong signal this issue's authors are willing to un-tick their own boxes.
- The out-of-scope boundaries (HDL export owned by #59, Yosys import owned by #61, no
  JLS-side synthesis) are clear and non-overlapping with what's actually implemented.

## Verdict rationale
Not `needs-rework`: the landed portions are real, well-cited, and the issue is unusually
disciplined about not overclaiming. Not `sound` outright because of the live, unresolved
ownership collision (Finding 2) and the unaddressed hardware-procurement gap (Finding 1)
— both are concrete risks to the next unit of work, not nitpicks. `sound-with-concerns`.
