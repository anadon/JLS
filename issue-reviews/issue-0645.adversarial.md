# Issue #645: TASK-C599-1: the Basys-3 question gets a written, searchable answer — supported with its toolchain named, or refused with the cost arithmetic that produced the refusal
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#645 asks for a decision document on Digilent Basys-3 support, verdict either way, with arithmetic shown. The instinct — price the decision instead of leaving it silent — is sound. But the issue prejudges the very fact it is supposed to establish, cites at least two "compliance" artifacts (a D8 cost table, KC-38-1) that do not exist anywhere in this repository, and its acceptance criteria are checkable by a keyword grep rather than by anything that would catch a document that gets the decision wrong. This is the parent decision task for #647 (TASK-C599-2), whose own adversarial review already found the downstream task inherits an unverified ASEE premise from this issue; that finding traces directly back here.

## Findings, by severity

### 1. (High) The issue states its own open question as settled fact, and the codebase it must be grounded in says the opposite

The Outcome section asserts: *"The Digilent Basys-3 ... is the board the ASEE-documented courses actually own."* I grepped the full tree for "ASEE" and "Basys" (case-insensitive): the only hits are inside `docs/standards-adoption/` and `docs/hdl-support-research.md`, and none of them cite an ASEE source or any course roster. `docs/standards-adoption/06-fpga-constraint-formats.md` explicitly lists Basys 3, DE10-Lite, and ULX3S as merely **"illustrative"** pin examples, and its own go/no-go section says: *"Do NOT do this if: No user has asked for a specific board... The correct trigger is a course or a user naming a board they own."* `docs/standards-adoption/OPEN-QUESTIONS.md:119` lists *"Which boards are actually wanted"* as an explicit, unresolved open question. So the one document in this repo that actually surveyed board demand says the premise #645 states as fact is unconfirmed. A task executor following #645 literally would write a "supported" verdict resting on a claim the project's own research contradicts, or would have to silently re-litigate #645's premise before doing #645's job. (This is also finding #7 of the sibling review at `issue-reviews/issue-0647.adversarial.md`, which traces the same problem to this issue.)

**Recommendation:** rewrite the Outcome to state the ASEE/course-ownership claim as the thing to verify or cite, not as given; if no citation exists, say so and make resolving it part of AC-1.

### 2. (High) AC-1 lets "supported" be satisfied by naming a toolchain nobody in this project has evaluated

AC-1 requires the toolchain be "named" with "its version basis stated" — nothing more. `openXC7` (the named open-source alternative to Vivado for Xilinx 7-series parts) appears **nowhere** in this repository outside issue text: not in `docs/hdl-support-research.md`, not in `docs/standards-adoption/06-fpga-constraint-formats.md` (which did deep, dated research on XDC/Vivado/Quartus/LPF but never mentions it), not in `docs/standards-landscape.md`. There is no in-tree evidence anyone has checked whether openXC7 actually supports the Basys-3's exact part (`XC7A35T-1CPG236C`), its maturity, or its license compatibility with a GPLv3 project. A one-sentence "supported via openXC7 v_X_" satisfies AC-1's letter while the actual claim could be false — exactly the failure the project's own docs warn against elsewhere: *"An emitter that claims vendor compatibility nobody has ever observed is worse than no emitter: it converts a missing feature into a false claim"* (`06-fpga-constraint-formats.md`).

**Recommendation:** require AC-1's "supported" branch to cite what was actually checked about the named toolchain (part support, maturity, license), not just its name and version.

### 3. (High) AC-2's "D8 cost table" and "D8/D10-compliant form" are not defined anywhere in this repository

I grepped the entire tree for "D8" and "D10" outside `issue-reviews/`. The only hit is an unrelated `#### D8. Editor UX` heading in `docs/capability-roadmap/lf-01-parameterization.md` — a section-numbering convention in a different roadmap document, not a cost-arithmetic schema. There is no `docs/plan`, no `BRIEF.md`, no file that defines what "D8/D10-compliant form" means for pricing a refusal. This is the same fabricated-citation pattern independently found by several sibling adversarial reviews in this corpus (`issue-0349.adversarial.md`: *"'D10' is asserted with no definition or link anywhere"*; `issue-0453.adversarial.md`, `issue-0463.adversarial.md`, `issue-0465.adversarial.md`, `issue-0777.adversarial.md`: *"KC-27-2 and band_mw are cited as binding but are undefined anywhere in this repository"*). AC-2 asks a document to conform to a format that does not exist in the checkout, so conformance cannot be checked by anyone reading only this repository.

**Recommendation:** either point AC-2 at a real, existing cost-presentation model in this repo (e.g. the sizing tables in `docs/standards-adoption/06-fpga-constraint-formats.md` or `11-costed-rejections.md`, both of which do show reasoning-per-line cost breakdowns) or strike the "D8/D10-compliant" wording.

### 4. (Medium) "KC-38-1" and "Logisim-Evolution's #91" are asserted as settled, citable authorities that cannot be verified from this repository

AC-3 requires the vendor-toolchain boundary be stated "with Logisim-Evolution's #91 cited as the recorded cost of owning vendor-tool detection forever," and the parent feature #599 states "KC-38-1 binds this feature." Neither term appears anywhere in this repo (same grep as finding 3). `#91` is an issue number in an **external** repository (`logisim-evolution/logisim-evolution`); this review session's GitHub tool explicitly refused to fetch it ("repository ... is not configured for this session. Allowed repositories: anadon/jls"), and nothing in this repo's own Logisim research (`docs/grand-architecture.md:351-353`, `docs/hdl-support-research.md`'s July follow-up sources list) cites that specific issue number or its "cost." The issue asks the executor to cite a specific external fact as pre-established without the fact being checkable in-tree.

**Recommendation:** either add a verified summary of what Logisim-Evolution #91 actually says (with a fetch date) to an in-repo research document before this task starts, or drop the specific-issue citation requirement and describe the vendor-detection cost in this project's own terms.

### 5. (Medium) AC-1's discoverability test can fail on the issue's own literal wording

AC-1 requires the decision be "discoverable by searching the repository for 'Basys-3'" (hyphenated, as spelled throughout this issue). Every existing in-repo reference to the board uses a different spelling: `docs/hdl-support-research.md:130,173` and `06-fpga-constraint-formats.md` all write "Basys 3" (space, Digilent's own product name) or "BASYS3" (no separator, matching Digital's board-XML naming). A decision document that (reasonably) matches the project's existing house spelling — "Basys 3" — would not literally satisfy a search for the string "Basys-3" as AC-1 specifies it. This is a small thing, but it is exactly the kind of literal-match acceptance criterion that lets a technically-compliant document dodge its own verification.

**Recommendation:** state the search term case/spelling-insensitively ("Basys 3", "Basys-3", or "BASYS3"), matching the forms already in the tree.

### 6. (Medium) No verification mechanism distinguishes a correct decision from a merely-present one

Compare this task to its neighbors: #416 gates its own claims with mechanical tests (`BoardFormatTotalityTest`, `FlashRecordTest`) that assert presence *and* shape. #645's only checks are a keyword grep (AC-1) and "the arithmetic is shown, not asserted" (AC-2) — a standard with no defined pass/fail boundary. A document containing a single unexamined sentence per criterion (a named toolchain with no due diligence per finding 2, a three-line "arithmetic" table with no defined completeness bar per finding 3, one sentence naming the vendor boundary) would satisfy every AC as literally written, while still leaving a reader unable to trust the verdict. Unlike #647 (code + goldens) or #416 (compile-time totality), #645 has zero artifact-level check.

**Recommendation:** add at minimum a structural checklist the document must satisfy (e.g., the six-subheading shape `11-costed-rejections.md` already uses for its costed items: what conformance means, implementation cost, testing cost, certification cost, effort/risk, sources) so "arithmetic shown" has a concrete, comparable shape.

### 7. (Low) `ordering_after: [264, 416]` is unexplained for a documentation-only task

#645 produces a decision document, not code. #264 (Board on-ramp) and #416 (TASK-0052, the second open-toolchain board) are both open, unimplemented features about ECP5/LPF — a different vendor family from Basys-3/XDC entirely. Nothing in #645's own body explains why a Basys-3 decision document must wait on ECP5 work landing; #647's own review already flagged the resulting ambiguity one level downstream ("the existing total emitter dispatch" language reading as present-tense when #416 hasn't landed). Leaving the ordering rationale unstated here just pushes the same confusion onto whichever task consumes it next.

**Recommendation:** either state explicitly why the ECP5 dependency matters to a document-only decision (e.g., "so the doc can describe the real board-table shape"), or drop it from `ordering_after`.

### 8. (Low) The issue calls #59 the current owner of "the exported Verilog" without noting it is closed `not_planned`

AC-5 defers to "#59's exported Verilog" as settled scope to leave alone. I fetched #59: it is **closed**, `state_reason: not_planned`. The underlying Verilog-export code did land (confirmed via `ARCHITECTURE.md` and `docs/hdl-support-research.md` — `HdlExporter`, `VerilogEmitter` are real, on master), so this is likely not a functional problem, but citing a closed, not-planned tracking issue as a live scope-owner without noting its closure state risks confusing a reader who opens #59 expecting an active issue to defer to.

## What's solid

- Requiring either verdict — "supported" or "refused with arithmetic" — to count as a complete, successful outcome (rather than only "supported" counting) is a sound anti-pattern guard against a decision task that quietly never produces anything; the sibling #647 review calls out the same instinct positively.
- Explicitly fencing off #416's ECP5 work and #59's exported Verilog as out of scope (AC-5) is the right instinct for keeping a documentation task from growing into an implementation task, independent of finding 8's naming nit.
- Requiring the vendor-toolchain boundary be stated explicitly (AC-3's core ask, separate from the unverifiable #91 citation in finding 4) is a genuinely useful piece of institutional memory for a single-maintainer project fielding the same question repeatedly.
