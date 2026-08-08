# Issue #645: TASK-C599-1: the Basys-3 question gets a written, searchable answer — supported with its toolchain named, or refused with the cost arithmetic that produced the refusal
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the YAML and the D8/D10 vocabulary and #645 says one true thing: *a question a
reader can ask should have an answer in the tree, and a priced refusal is an answer.*
That is exactly the ethic of `docs/standards-adoption/11-costed-rejections.md` ("a
rejection with no price on it is a shrug") and of the playbook's four-word verdict
vocabulary in `docs/standards-adoption/README.md`. The instinct is right and the band
(0.5–1 mw) is honest. Endorsed on that axis.

But the issue frames the deliverable as **a bespoke prose document about one board,
choosing between two options, priced off a table computed under an assumption nobody
has revisited.** All three of those are wrong at the architectural level, and each has
a cheaper, more general replacement already latent in the tree.

## Reframing 1 — the answer belongs in a board registry, not a board essay

AC-1's discoverability test is "grep the repo for Basys-3." That is a proxy for the
real need, and it misses the two places a Basys-3 owner actually arrives:

- `test/jls/hdl/board/CliBoardExportTest.java:164` uses `basys3` as the **unknown-board
  fixture**, and `src/jls/JLSStart.java:1105` answers it with `(supported: <names>)`
  from `Boards.names()` (`src/jls/hdl/board/Boards.java:116`). So JLS does not "say
  nothing about it either way" — it says *unknown board* at precisely the moment of
  need, and that diagnostic is the highest-value sentence in this whole feature.
- #416 is already building `docs/board-handoff.md` (a section per board) and
  `docs/board-flash-record.md` (one row per `Boards.all()` entry, presence asserted by
  `FlashRecordTest`). A per-board status table with a build-enforced row already exists
  in the plan.

**Concrete alternative:** extend that structure from *supported boards* to *known
boards*. A `Boards.DECLINED` (or `BoardStatus`) list carrying `name`, `part`, `status`
(supported / demand-gated / declined), a one-line reason, and a doc anchor. Then:

1. the unknown-board diagnostic reads it and prints *"basys3: Artix-7, no open-flow
   evidence yet — see docs/board-handoff.md#basys3"* instead of a bare rejection;
2. `docs/board-handoff.md` grows a "boards JLS does not carry, and why" section whose
   rows are checked against the list by the same test shape #416 is already writing;
3. AC-1's grep passes as a side effect, and so does the grep for the *next* board.

The cost of answering board #2 through #29 (Logisim-Evolution ships 29 board XMLs —
`docs/hdl-support-research.md:173`) drops from "one task each" to "one row each."
Written as a one-off essay, #645 answers Basys-3 and leaves DE10-Lite, Arty, Nexys A7
and Tang Nano exactly as unanswered as Basys-3 is today. That is the seam this issue
should be cut along.

## Reframing 2 — the option set is missing the branch that dissolves the vendor question

The issue offers two outcomes: *supported via openXC7 or a documented vendor handoff*,
or *refused with arithmetic per the D8 cost table*. Two observations:

- **`openXC7` appears nowhere in this repository.** Not in
  `docs/standards-adoption/06-fpga-constraint-formats.md`, not in
  `docs/hdl-support-research.md`, not in `flake.nix`. Neither do `nextpnr-xilinx` or
  `prjxray`. Section 06 reasons entirely inside a Vivado-or-nothing world, and *that
  assumption is what generates every number the refusal would cite*: "Vivado and
  Quartus cannot be in CI, and that is final", "XDC and QSF emission will be
  golden-pinned but never machine-validated", "Do NOT do this if: the maintainer will
  not install Vivado/Quartus", and the 1.5–3 maintainer-day acceptance-run line in the
  sizing table.
- If an open Artix-7 flow (yosys → nextpnr-xilinx → prjxray bitstream →
  `openFPGALoader`) covers XC7A35T-CPG236, then **every one of those constraints
  evaporates**: CI can validate the constraint file against a real chipdb exactly as
  `NextpnrConstraintTest` is planned to for ECP5; KC-38-1 (no vendor-process driving)
  is never approached; #264's "both halves" rule is satisfiable without a human
  installing 50 GB of vendor software; and `scripts/icestick-handoff.sh:121-153`
  already invokes `openFPGALoader`, which is the same programmer a Basys-3 needs.
  Basys-3 stops being an exception to the project's arc and becomes the *third
  instance* of it.

So the single most decision-relevant fact in this entire feature is one the issue never
names: **does nextpnr-xilinx ship a usable chipdb for XC7A35T-CPG236, and can
`flake.nix` pin it?** That is a half-day of verification. It is worth more than the rest
of the task combined, because it selects the verdict rather than documenting one. A
refusal priced off section 06's table without answering it is arithmetic performed on a
premise that may be obsolete — precisely the failure the "re-cost finds them rather
than rediscovering them" language in AC-2 is trying to prevent.

**I am explicitly disregarding AC-2 as written.** "Show the arithmetic per the D8 cost
table" should become "state the deciding question, answer it, and then show the
arithmetic on the branch that survives." Note also that D8/D10 are defined nowhere in
the tree (`grep -rn '\bD8\b'` hits only `lf-01-parameterization.md:306`, an unrelated
"D8. Editor UX" heading) — an acceptance criterion whose compliance standard is not in
the repository cannot be checked by a reader, which is the exact failure #645 exists to
correct.

## Reframing 3 — the board is not the binding constraint; the exportable subset is

AC-4 is the best criterion in the issue: *state plainly what a course owning Basys-3
hardware can and cannot do with JLS today.* Its honest answer, though, is
board-independent and much larger than the board question.

`HdlExporter.EXPORTED` (`src/jls/hdl/HdlExporter.java:421-428`) lists 22 element
classes. `SubCircuit` and `Memory` are **not** among them, and their rejection is pinned
as intended behaviour by `test/jls/hdl/HdlPolicyTest.java` (`memoryIsRejectedByName`,
`subCircuitIsRejectedCleanly`). So JLS can export only flat, hierarchy-free,
memory-free designs — the `riscv/` CPU in this very repository cannot be exported at
all (`docs/capability-roadmap/sweep-06-physical-boundary.md`).

A Basys-3 is an XC7A35T: roughly 33k LUTs and 1.8 Mb of block RAM, about 25× the
iCEstick's HX1K. Adding it would hand a classroom a part 25× larger for designs that
cannot contain a subcircuit or a memory. The constraint file is not what is stopping
that course; the export policy is. **Write AC-4 first, board-agnostically, as one
paragraph in `docs/board-handoff.md` — "what any board can carry today" — and it costs
nothing to research, serves every board, and is more useful than the Basys-3 verdict it
was meant to accompany.**

## Alignment with the project's trajectory

Two pulls against the larger arc are worth naming:

**Priority inversion.** #264's founding invariant is "no board ships half-supported —
both halves, per board, before the next board." Yet at the evidence commits cited in
#264 and #416, the *one* supported board has never produced a real bitstream (CI runs a
stub-PATH selftest with fake tools) and has never been flashed
(`docs/icestick-bitstream-handoff.md`'s version table is all `_TBD_`). #416, which
would fix that, is `blocked_by: [386]`. Opening a third-board question ahead of the
first board's hardware evidence is exactly the drift #264 was consolidated to stop.

**Demand gate already recorded, and this issue overrides it in prose.**
`docs/standards-adoption/OPEN-QUESTIONS.md:119` records "which boards are actually
wanted" as an open question, naming Basys 3 / DE10-Lite / ULX3S as *illustrative*;
section 06's go/no-go says "Do NOT do this if: no user has asked for a specific board…
The correct trigger is a course or a user naming a board they own." #645 asserts as
settled that Basys-3 is "the board the ASEE-documented courses actually own" — and
`ASEE` occurs nowhere in the tree outside this review directory. Tracing #522's evidence
field back, the claim is that *ASEE literature pairs Logisim-Evolution with Basys-3*,
i.e. evidence about a different tool's users, not a JLS user naming hardware they own.

That gap is not a reason to skip the task; it is a reason the verdict vocabulary should
not be binary. The playbook already has the right word: **do-it-if**. The most honest,
most defensible output of #645 is a third outcome the acceptance criteria exclude —
*demand-gated, with the trigger named, the option set (openXC7 vs vendor handoff)
priced under each branch, and the recipe a Basys-3 owner follows meanwhile.* Adopting
the playbook's own vocabulary here also makes the decision legible beside the eleven
other decisions written in the same voice.

## What I would actually ship, in order

1. One paragraph in `docs/board-handoff.md`: what any board can carry today (the export
   subset), board-agnostic. Costs no research; satisfies AC-4 for every board.
2. Answer the deciding question: nextpnr-xilinx chipdb coverage for XC7A35T-CPG236 and
   its `flake.nix` pinnability. Half a day; selects the verdict.
3. A board-status list in `jls.hdl.board` plus its rows in `docs/board-handoff.md`, wired
   into the unknown-board diagnostic at `JLSStart.java:1105` and checked by a test in
   the shape #416 already plans. Basys-3 becomes its first non-supported row —
   `do-it-if`, trigger named, both branches priced.
4. Only then, if step 2 came back positive and a real user named the board, does #647's
   emitter work become schedulable — and it rides the same open-toolchain rails as
   #416 rather than opening a vendor boundary.

Minor but load-bearing for AC-1 as written: the tree spells the board `basys3` and
`Basys 3`; the hyphenated `Basys-3` the criterion demands appears nowhere. Whatever
lands should carry all three spellings, or the grep criterion passes only for the
string the author happened to choose.
