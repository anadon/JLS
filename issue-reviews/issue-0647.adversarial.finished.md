# Issue #647: TASK-C599-2: the Basys-3 verdict lands as code or as a recipe — a Boards entry with a byte-pinned golden, or the documented path a Basys-3 owner follows instead, with the open-toolchain goldens proven unmoved
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#647 (TASK-C599-2) is the "consequence" task that executes whatever #645 (TASK-C599-1) decides about Basys-3 support. It is well-scoped in isolation, but its "if supported" branch (AC-1, AC-2) quietly assumes infrastructure that does not exist yet in this repository and, per the project's own research, understates the actual engineering cost. Its "if refused" branch (AC-3) has no verification mechanism at all. Both branches carry a dependency the `ordering_after` field does not capture.

## Findings, by severity

### 1. (High) AC-2's precondition — a GUI board picker — does not exist and is not in `ordering_after`

AC-2 says: *"the board appears in the GUI board picker with no GUI source change (#597 AC-1)."* I grepped the whole `src/` tree for `picker`, `BoardPicker`, and any Swing board-selection widget: there are zero hits. The board flow today is CLI-only (`src/jls/JLSStart.java:111-114`, `:392`, `:421-428`, `:783-786`, `:908-916` — the `-board`/`-pins` flags). The GUI board picker AC-2 references is the subject of **issue #597** ("FEAT-C38-1"), which I fetched and confirmed is **open, unimplemented**, and itself depends on #264/#288.

`ordering_after` in #647's frontmatter is `[264, 416, "TASK-C599-1"]` — #597 is absent. As written, AC-2 is unsatisfiable until #597 lands, but nothing in #647 gates on that. If #647 is executed before #597 (which its own ordering permits), AC-2 cannot be verified because there is no picker to add the board to.

**Recommendation:** add 597 to `ordering_after`, or reword AC-2 to state explicitly that "GUI appearance" is deferred/vacuous until #597 exists, so a premature closer can't claim AC-2 "trivially holds" by pointing at nothing.

### 2. (High) AC-1's "existing total emitter dispatch" does not exist yet in the codebase, and depends on an open issue (#416) for its very existence

I read `src/jls/hdl/board/Board.java` and `PcfEmitter.java`. `Board.Format` has exactly one constant (`PCF`), and format dispatch is a single `if (board.format() != Board.Format.PCF) throw new IllegalArgumentException(...)` (`PcfEmitter.java:61-64`) — not a switch over `Format.values()`, and there is no `ConstraintEmitters`-style dispatcher. The "total dispatch with no default arm" AC-1 refers to is the deliverable of **#416 (TASK-0052)**, which is itself **open and unimplemented** (confirmed by reading #416: it explicitly states "no board ships half-supported" and lists the switch conversion as a *prediction that must hold after* its own work lands).

`ordering_after: [264, 416, "TASK-C599-1"]` does correctly list 416 first, so the sequencing intent is right — but the issue body's Outcome section describes the dispatch in the present tense ("land under the **existing** total emitter dispatch") as though it's already there. It is not, at either #647's filing or at the time of this review. A reader picking up #647 without also reading #416 will misjudge scope and effort.

**Recommendation:** state plainly that AC-1 is blocked on #416 landing, not merely ordered after it, and phrase the dispatch as "the total dispatch #416 introduces," not "the existing" one.

### 3. (High) AC-1's implied cost ("a `Boards` entry ... under the existing total dispatch") materially understates what the project's own research already found is required for XDC

`docs/standards-adoption/06-fpga-constraint-formats.md` (in-repo, evidently the design study behind this whole feature line) is explicit that adding XDC is **not** a drop-in `Boards` entry like PCF/LPF. It requires, as prerequisites:
- Extracting a shared `PinBinder` (moving ~55 lines out of `PcfEmitter`) — "Step 1".
- A new `ConstraintEmitter` interface and `ConstraintEmitters.forFormat` dispatcher — "Step 2".
- **Replacing `Board.pins` (`Map<String,String>`) with a `Pin` record carrying `IoStandard`, `clockHz`, `Pull`, and drive strength**, plus new `Board.part`/`Board.family` fields — "Step 3" — because, per that same document: *"IOSTANDARD is not optional in practice: a port with an unset I/O standard trips Vivado's UCIO-1/NSTD-1 DRC at bitstream time, which is an error by default."*
- The doc's own sizing table puts the refactor + Board/Pin extension alone at **2 maintainer-days** before the XDC emitter (1 more day) and a manual Vivado acceptance run (1.5–3 days) are even started; total for XDC+QSF+LPF combined is quoted as **8–10 maintainer-days**.

#647's `band_mw: "1.5-3"` (maintainer-*weeks*, i.e. 7.5–15 days) is roughly consistent with the low end of that range only if it absorbs the whole prerequisite refactor — but AC-1's wording ("a `Boards` entry ... lands under the existing total emitter dispatch, with a byte-pinned golden ... no second ordering") reads as though the work is exactly as small as adding a fourth `Boards` entry, which is what #416 (LPF/ECP5) is doing. It is not the same size of task, and the issue does not say so.

**Recommendation:** either fold the `PinBinder`/`ConstraintEmitter`/`Pin` refactor into #647's explicit scope (and re-cost it), or split it into its own prerequisite task and have #647 consume it — the way #416 is already split out as a prerequisite of #647.

### 4. (Medium-High) Internal contradiction: AC-1's "no second ordering" / AC-4's "no golden of theirs moves" vs. the Board-record change XDC requires

AC-1 says the new board lands with "the same `NATURAL_PIN_ORDER` — no second ordering," and AC-4 requires the iCE40 and #416's board goldens to be provably unmoved. But if XDC is implemented correctly per finding #3 above, `Board`'s pin type changes from `Map<String,String>` to a `Pin`-valued map with an `IoStandard`. That is a breaking signature change to `Board`'s constructor — the research doc says as much: *"`test/jls/hdl/board/BoardPinOrderTest.java:29` constructs `new Board(...)` directly and must be updated with the signature."* Every existing `Boards` entry (`ICESTICK`, and whatever #416 adds) must be re-expressed in the new shape. The byte content of PCF/LPF goldens may well survive this (PCF/LPF emission doesn't print `IOSTANDARD` in the current examples), but that has to be *demonstrated*, not simply asserted as AC-4 requires ("asserted rather than assumed") — and #647 doesn't call out that this refactor is the actual mechanism by which that assertion could fail. This is exactly the class of regression AC-4 exists to catch, and the plan that would deliver AC-1 is the one most likely to trip it.

**Recommendation:** name the `Board`-record migration explicitly as an in-scope risk to AC-4, with a stated plan for re-verifying both existing goldens byte-for-byte after the record shape changes (not just "no PCF-format-specific code touched").

### 5. (Medium) AC-3 ("if refused") has no verification mechanism at all

Compare AC-1, which is checked by a byte-pinned golden test, and AC-4, which is checked by "no golden moves" (a diff). AC-3 says the refusal recipe *"does not imply support anywhere in the repository"* — but no test, grep pattern, or doc-lint is specified to check this, unlike AC-4/#416's `ProcessBuilder` grep. "Does not imply support anywhere in the repository" is a whole-repository negative claim (README, `docs/hdl-support-research.md`, help text, CHANGELOG, code comments) that a human reviewer would have to eyeball at PR time; it's the kind of criterion a rushed reviewer waves through with "looks fine to me." Given this project's own convention elsewhere (#416's `everySupportedBoardHasAFlashRecordRow`, `BoardFormatTotalityTest`) of turning exactly this sort of implicit-consistency worry into a mechanical check, its absence here is a real gap given that this task self-selects for the "no automated backstop" branch.

**Recommendation:** at minimum, specify a grep/lint (e.g. "no occurrence of `basys` (case-insensitive) outside the recipe doc and issue history") the PR must run and paste output for, mirroring the `ProcessBuilder` grep already in AC-5.

### 6. (Medium) AC-1 is gameable: a byte-pinned golden proves shape, not vendor acceptance — and the issue never asks for the latter

`06-fpga-constraint-formats.md` states this outright: golden files "prove shape and determinism, nothing about vendor acceptance," and for XDC/QSF specifically: *"Vivado and Quartus cannot be in CI, and that is final... XDC and QSF emission will be golden-pinned but never machine-validated."* It also warns: *"An emitter that claims vendor compatibility nobody has ever observed is worse than no emitter: it converts a missing feature into a false claim."*

#647's AC-1 requires only the golden and the aggregation contract — it says nothing about a manual Vivado acceptance run, nor does it require the "emitted, not yet accepted" caveat the research doc says must appear in docs until a real acceptance run happens. A contributor can satisfy AC-1 to the letter while shipping a `.xdc` file that Vivado's DRC rejects on a real Basys-3 (wrong bus-bit quoting, missing `IOSTANDARD`, a transcription error in the pin table — the research doc's own top risk, which it says "no test in the plan catches"). The task's stated Outcome ("a `Boards` entry ... land[s]") would then be technically true while the actual goal (a Basys-3 owner can use this) silently fails.

**Recommendation:** add an AC requiring either a real acceptance record (per the research doc's `docs/fpga-acceptance-checklist.md` proposal) or, failing that, an explicit "emitted, not yet vendor-accepted" disclaimer wherever the board is documented as supported.

### 7. (Medium) The demand premise this whole chain rests on is unverified, and the project's own docs say so

#645 (TASK-C599-1, the decision task #647 depends on) asserts as settled fact that Basys-3 "is the board the ASEE-documented courses actually own." I found no ASEE citation or course-ownership evidence anywhere in the repository — the only two hits for "Basys" or "ASEE" are both inside `docs/standards-adoption/`, and `docs/standards-adoption/OPEN-QUESTIONS.md:119` lists *"Which boards are actually wanted"* as an **explicit open question**, naming Basys-3/DE10-Lite/ULX3S as merely "illustrative." The same research doc's own go/no-go section says: *"Do NOT do this if: No user has asked for a specific board... The correct trigger is a course or a user naming a board they own."* #647 doesn't re-litigate this (it's #645's job), but since #647 exists and is banded at 1.5–3 maintainer-weeks regardless of #645's verdict, its filer is already committing scheduling weight to a decision whose supporting evidence, on the current record, doesn't check out.

**Recommendation:** #645 should resolve or retract the ASEE claim before #647 is picked up; #647 could note this as a stated risk rather than silently inheriting an unverified premise.

### 8. (Low) `ordering_after` mixes issue numbers with a task-ID string, which may not resolve

`ordering_after: [264, 416, "TASK-C599-1"]` mixes two numeric issue references with a bare task-ID string. #645 itself (which *is* TASK-C599-1) uses only numeric `ordering_after: [264, 416]` when referencing its own predecessors — so the convention elsewhere in this same feature is numeric. If whatever tooling parses `ordering_after` for scheduling/gating expects issue numbers, `"TASK-C599-1"` won't resolve to #645 without a separate name→number lookup, silently dropping that ordering edge.

**Recommendation:** use `645` (the actual issue number) rather than the task-ID string, consistent with the other two entries.

## What's solid

- The "either way, open-toolchain coverage is untouched" framing (AC-4) is the right invariant to protect, and reusing `NATURAL_PIN_ORDER` rather than inventing a second sort is correct per prior art (#416 O4).
- Requiring a comment-per-functional-block naming the documentation source (AC-5) is a reasonable, cheap mitigation for the transcription-error risk the research doc flags as the single worst failure mode.
- The `git grep -n "ProcessBuilder" -- src/` check (AC-5) correctly inherits the project's "delegate, don't reimplement" invariant (#215 H2) verbatim from #416, keeping the two tasks consistent.
- Making "the verdict does not stop at a document" an explicit design goal (something concrete lands either way) is a sound anti-pattern guard against a decision issue that never produces an artifact.
