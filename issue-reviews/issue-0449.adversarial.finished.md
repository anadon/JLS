# Issue #449: TASK-0048: a multi-module netlist imports as nested subcircuits instead of being refused, so a design written in more than one module can actually run
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Issue body, all 3 comments, `README.md`, `ARCHITECTURE.md`, `docs/file-format.md`,
`src/jls/hdl/imp/NetlistImporter.java` (current `master`, HEAD `5b05d67`),
`src/jls/hdl/HdlExporter.java`, `src/jls/elem/ElementRegistry.java`,
`test/jls/hdl/imp/ImportPipelineTest.java`, `test/resources/hdl/import/*`, `src/jls/Util.java`.
The issue is open, has 3 comments, and is well-evidenced overall — most `git show`-cited
snippets (O1, O2, O4, O5, O10) check out against the current tree by content. The findings
below are the ones that survive that grounding.

## Findings, most severe first

### 1. (HIGH) The plan's own directed fix for name legalization does not satisfy the format grammar it cites, and this threatens H1's falsification test

`docs/file-format.md:153-155` (read directly, not just cited): *"Nested `CIRCUIT` names
(subcircuits) are meaningful and MUST match `letter (letter | digit | "_")*`
(`Util.isValidName`)."* This is exactly the token the emitted `CIRCUIT <name>` header for
every realized definition (§7.6) must satisfy — the issue's own H1 says the whole approach
is refuted "if the emitted text fails to load through `Circuit.load` + `finishLoad`."

The issue's Method (§8) directs reusing the *existing* `legalize` helper for the collision
check: *"Add the `legalize` collision check naming both original names (O8)."* I read that
function at `src/jls/hdl/imp/NetlistImporter.java:1046-1065` (confirmed present at exactly
the cited line 1055): it strips a leading `\`, substitutes `"net"` for empty, and escapes
`\` and `"` — a contract documented as producing "a safe JLS **pin name**" for a *quoted
string attribute*. It does **not** enforce `letter(letter|digit|_)*`: it does not reject a
leading digit, `$`, `.`, `-`, or embedded whitespace. Yosys module/cell-type names commonly
contain exactly these characters — mangled names for parameterized modules
(`$paramod$<hash>\ModuleName`), SystemVerilog generate-block instance paths, and
backslash-escaped identifiers that can contain almost anything after the leading `\`
(spaces included). Feeding such a name straight into `out.append("CIRCUIT
").append(moduleName)` (line 805, unchanged by this task's own account) would emit a nested
`CIRCUIT` header that violates the grammar the issue cites as already sufficient, and could
plausibly break line-oriented parsing outright (a name containing a literal newline or a
reserved token like `ENDCIRCUIT`).

Nothing in §7 (Interface & Data Contract), §8 (Method) or §14 (Definition of Done) asks for
a *separate* identifier-legalization step (mapping to `Util.isValidName`) for the
module/definition name used as a nested `CIRCUIT` token, only the existing pin-name
`legalize` (wrong contract) plus a *collision* check (which presupposes some legalization
step exists and is well-defined, but never specifies what it is). **Recommendation:** add
an explicit requirement, and a prediction, that module/definition names are mapped through
an `Util.isValidName`-conformant legalizer before being used as a nested `CIRCUIT` name, and
that P9's collision check operates on the *legalized* identifier, not on `legalize()`'s
pin-name output.

### 2. (HIGH) $D_{\max}$ and $E_{\max}$ are load-bearing but unspecified, making P7 and the exponential-blowup risk (H4) self-certifying

§7.10 proves the realized element count is exponential in depth under fan-out
($\Theta(k^d)$) and states this is "the reason the summary must report definition and
instance counts separately." The only actual backstop against runaway realization is P7's
depth/element bound, refusing with "the computed figure." But Open Question 1 explicitly
defers the *numbers* to the executor: *"Recommended: a depth in the low tens and an element
bound derived from the existing 64 MiB load cap's implied element ceiling... Blocks
execution."* No number is given in the issue. Since the executor both picks $E_{\max}$ and
writes the fixtures that exercise P7, the criterion "P7 must hold" can be satisfied by
picking a bound large enough that no committed fixture ever approaches it — which leaves
the exponential-blowup hazard the issue itself calls out as real (§11, "the highest-severity
latent defect the task touches" is reserved for the *other* finding, but this one is
unguarded) effectively unverified by anything in the Definition of Done. **Recommendation:**
either put a concrete number (or a formula with concrete constants) in the issue body now,
or make the completion criterion explicitly include a fixture that is *designed to exceed*
whatever bound the executor picks, so the refusal path is exercised for real rather than
merely coded.

### 3. (MEDIUM) P8 — the one prediction that touches a real multi-module Verilog→Yosys pipeline — is both skippable and under-specified, so it can pass without ever exercising finding #1's hazard

P8 says: *"Run the shipped pipeline (O4) on a two-module Verilog fixture and import the
result unmodified; observe success."* §9 confirms it is "the yosys-gated pipeline leg,
self-skipping when the tool is absent, in the existing `ImportPipelineTest` idiom" — verified
against `test/jls/hdl/imp/ImportPipelineTest.java:88-90`, which does self-skip when `yosys`
is not on PATH. Combined with finding #1: nothing in the issue requires the Verilog fixture
used for P8 to include a parameterized module, a generate-block instance, or any other
construct that would make Yosys emit a non-`letter(letter|digit|_)*` module name. A
trivially-named two-module fixture (`module a; module top;`) will pass P8 and "`mvn verify`
green" while leaving the exact hazard in finding #1 completely unexercised — and in a CI
runner without `yosys` installed, P8 doesn't run at all. **Recommendation:** name the
required fixture shapes explicitly (at minimum: one module instantiated with a `#(...)`
parameter override, since that is what triggers Yosys's `$paramod` mangling), and make clear
that "Completion Criteria" is not satisfied by a green `mvn verify` in an environment where
P8 silently skipped.

### 4. (MEDIUM) The issue's own dependency metadata has already been corrected twice via bolted-on comments, not by editing the body — a reader who trusts the YAML block gets it wrong

The body's `Status & Dependencies` block declares `part_of_feature: 320` and
`related: [320, 342, 61, 62, 385, 357]`. Comment `5180927188` records that #320 "closed as a
duplicate" and re-homes the parent to #61; comment `5227453835` ("round-2 chain check")
records that the *previous* re-home comment was itself wrong/incomplete and re-corrects the
parent again, explicitly noting *"Any `STATUS:` / landing report the Definition of Done
directs at #320 goes to #61 instead. #320 is closed; a comment there reaches nobody."* Both
comments explicitly say the body text is left unedited ("No body or title of this issue was
edited... this comment is the migration record"). Definition of Done item 10 ("Landing
reported on #320 with a `STATUS:` comment...") is therefore **wrong as written in the body**
and only correct if the executor reads and orders three comments correctly before acting.
This is exactly the kind of hidden-assumption trap an execution-time reader can miss — a
mechanically-processed YAML block (which is plausibly how downstream tooling / other issues
consume this field, per the "link pass" language elsewhere in the issue) will report the
stale, closed parent. **Recommendation:** edit the body's `part_of_feature` field directly
rather than relying on a third corrective comment layered on two prior ones.

### 5. (LOW, already self-corrected) O9's cited `HdlExporter` snippet is branch-only and does not exist on `master`

Verified: `master`'s `src/jls/hdl/HdlExporter.java:60-90` has only a Javadoc bucket-list
("Reject... SubCircuit, Memory, and anything unrecognized... Rejection lists every offender
in one message") — there is no `REJECTED` map and no per-class reason string, contradicting
the issue's quoted `REJECTED` map with the string *"subcircuits cannot be exported yet:
..."*. This is already flagged by the issue's own comment `5171419350`, which correctly
identifies this exact range as branch-only code to be read as "motivation for #492," not
current fact. No new work needed here beyond what the comment already prescribes; I list it
only because an adversarial pass should confirm the self-correction is accurate, and it is.

## What is solid (one line each)

- O1/O2/O4/O5/O10's cited behavior is real and reproducible against current `master`
  (line numbers shift by a few lines per the tree drift, content matches exactly).
- The `top`-attribute string-not-boolean caution (O1, §7.2) is correct and worth keeping
  verbatim — a real, specific, previously-easy-to-get-wrong detail.
- Scoping out definition-sharing (#357), export-side hierarchy (#385), and layout placement
  (#342) with explicit boundary language is disciplined and avoids obvious scope creep.
- The cycle-refusal-not-`StackOverflowError` requirement (P6) is concrete and testable as
  stated.
- Keeping every existing fixture's outcome unchanged, including the four `reject_*.json`
  cases, is a reasonable and checkable non-regression bar.

## Verdict rationale

Not `should-not-proceed`: the core approach (reachability walk, existing `SubCircuit`
element, existing nested-block grammar) is architecturally sound and well-grounded in the
current tree. But `sound`/`sound-with-concerns` would understate two concrete, evidence-backed
gaps that go to the heart of the task's own success criterion (H1: does the emitted text
load?) and its own stated highest risk (H4: exponential blowup) — both currently
underspecified or self-contradicted in a way that lets the stated verification pass while
the real goal (a hierarchical design that actually, robustly imports) fails. `needs-rework`.
