# Issue #416: TASK-0052: a second board exists with both halves, and the first board's bitstream path has actually been walked on hardware and written down
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary

The task's core engineering claim — one board, one constraint format, a
runtime guard that is the only thing currently catching a forgotten
emitter — verifies against the live tree: `Boards.java:81`
(`List.of(ICESTICK)`), `Board.java:35-41` (`Format` enum with the sole
constant `PCF`), and `PcfEmitter.java:61` (`if (board.format() !=
Board.Format.PCF)`) all match the issue's citations exactly. The
hypotheses (H1-H3) are genuinely falsifiable with stated next moves, and
the out-of-scope list correctly matches sibling-issue ownership as
cross-checked against #264 and #386's real bodies. That said, several
concrete problems would misdirect an executor or let the task's teeth —
the hardware evidence — go ungamed.

## Findings, most severe first

**1. [HIGH] The stated `blocked_by: [386]` rationale is factually wrong — #386 does not install the tools #416 needs.**
`blocked_by`'s comment says: *"#386 TASK-0051 — the place-and-route
validation leg reads `nextpnr-ecp5` and `ecppack` from PATH... without
it the leg can only skip"*, and §6 Materials repeats *"None installed at
`2d0ca9d`; #386 installs them."* I fetched #386 in full: its abstract,
hypotheses, predictions, and Method are scoped entirely to `iverilog`,
`ghdl`, `yosys`, `nextpnr-ice40`, `icestorm`/`icepack`, and `verilator`
— the string `ecp5` does not appear anywhere in #386's body, and
`ecppack`/`openFPGALoader` appear nowhere either. Even if #386 lands
exactly as filed, it will **not** put `nextpnr-ecp5` or `ecppack` on any
PATH, so the premise this task's blocking edge rests on is false. The
DoD does allow a waiver ("the dependency was waived per rule 10... the
P&R leg can only skip"), which softens the practical impact, but as
written an executor who sees #386 close will wrongly believe the ECP5
toolchain is now armed. Recommendation: correct `blocked_by`'s
rationale and §6, and either broaden #386's scope or file the missing
task that actually installs the ECP5 toolchain before treating #386's
closure as satisfying this precondition.

**2. [HIGH] `evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` does not exist in this repository.**
`git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` fails and
`git log --all --oneline | grep 2d0ca9d` returns nothing across all 272
commits in this checkout. #386 (the blocking issue) cites the identical
hash; #264 (the "closes" target) cites a wholly different one
(`29afb26`). The Method's own step 1 ("re-verify O1–O10... re-derive if
HEAD has moved") tacitly concedes the pinned commit is not something an
executor can actually check out and diff — it is aspirational
provenance rather than a real anchor. Independently, the current
tree does happen to match most of the cited observations (verified
below), so this is not evidence of fabricated *findings*, but it is
evidence that the "pinned to an exact commit" rigor this whole
issue-family leans on is unverifiable as written. Recommendation: cite
a commit that actually resolves, or drop the specific-hash framing in
favor of "verified against HEAD as of <date>."

**3. [MEDIUM] Contested ownership is disclosed but left unresolved at filing time.**
Open Question 1 states plainly that the task is filed under #359
because "the corpus feature documents' task tables say so," but that
"#359's own Open Question 3 recommends #264 instead," and that filing
under both would be a single-owner violation. The recommended default
is to re-home to #264 "before execution starts." This is commendably
honest, but it means the issue as published is internally
self-contradicting about its own parent, and the DoD requires the
decision "recorded on both #359 and #264 before close" — a
housekeeping step that is easy to skip under time pressure and leaves
the dependency graph carrying the "half-edge" the issue itself warns
about (§11 Threats to Validity).

**4. [MEDIUM] The one existing comment records a coordination hazard the issue body never absorbed.**
The 2026-08-04 comment on this issue (from the same author) explicitly
warns that #647/#599/#645 (a Basys-3 go/no-go) could collide with "this
task's second board" if the Basys-3 is chosen, and that #632 requires
"adding a board needs no GUI change" — a constraint this task's board
addition must honor. Neither #647/#599/#645 nor #632 appears anywhere
in §12 Related Work or the Open Questions of the issue body itself,
even though Open Question 2 ("Which ECP5 board?") is exactly the
decision point the comment is warning about. An executor who reads only
the body (not the comment thread) can select a board or touch the CLI
board-picker without ever learning about either constraint.
Recommendation: fold both warnings into §12 and Open Question 2 rather
than leaving them recoverable only via comment archaeology.

**5. [MEDIUM] The hardware-flash acceptance criterion is gameable exactly where it matters most.**
The issue is admirably candid that `FlashRecordTest` "asserts the
record's presence and shape, never its truth" and calls this "the
single most misreadable thing in the task" (§7.11, §11). But nothing in
the Definition of Done requires any artifact harder to fabricate than
prose in a markdown table — no photo/video reference, no reviewer
attestation naming a person, no linkage to a build/PR that could be
cross-checked. Since the physical flash is also the one step no CI
system and no automated agent can perform (§6: "Real ECP5 hardware and
a human"), the task's hardest requirement is simultaneously its least
verifiable one. A plausible-sounding row (a real-looking tool version,
a today's date, "LEDs blinked as expected") satisfies the test whether
or not any board was touched.

**6. [MEDIUM] Feasibility gap for autonomous execution is real, not hypothetical.**
Given the issue corpus's evident design for machine/agent pickup (the
`STATUS:` comment convention, `rule 6`, `rule 10` waiver protocol, the
falsifiable-hypothesis apparatus), an agent executing this task can
complete every checkbox in §14 except "Walk the flash on real hardware"
— which §6 states outright "cannot be" a CI artifact. As filed there is
no named checkpoint at which the task hands off to a human with
hardware; the natural failure modes are an indefinite stall or the
fabrication described in Finding 5. Recommendation: split code/golden/
CI work (agent-completable) from the hardware-evidence row
(human-completable) into separate tracked items with an explicit
hand-off, rather than one Definition of Done spanning both.

**7. [LOW] Minor line-citation drift, pre-acknowledged by the issue itself.**
O4 cites `PcfEmitter.java:116-120` for the aggregated-bind-failure
guard; in the current tree that guard (`if (!errors.isEmpty())`) is at
line 115. Immaterial, and the issue's own §11 already lists "line-number
drift" as an expected threat to re-derive at pickup — noted only
because the Method's step 1 makes re-derivation a checklist item, not
optional.

## What's solid

- H1/H2/H3 are genuinely falsifiable, each with a concrete next move if
  refuted — the compile-break demonstration for H2 (P7) is a
  particularly good test of the "total dispatch" claim.
- The technical citations for the totality trap (O1, O3, O8-O10) verify
  exactly against the live tree, including exact line numbers for
  `Boards.java:81`, `Board.java:72`/`:80`, and `PcfEmitter.java:61`.
- Scope boundaries (§13, out-of-scope list) correctly track ownership
  as independently cross-checked against #264's and #386's actual
  bodies — no scope creep into Verilog generation (#59), the shuttle
  wrapper (#328), or toolchain installation (#386) is claimed.
- The "both halves" rule and the reused `NATURAL_PIN_ORDER`/aggregated-
  exception contracts are concrete, code-anchored requirements rather
  than vague prose.
