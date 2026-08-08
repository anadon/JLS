# Issue #651: TASK-C564-4: minimization runs headless, and the minimized expression is proved equivalent to the original circuit by exhaustive differential test
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what's being asked

Two bundled deliverables under one 1-mw task: (1) a headless batch flag that takes a
truth table and emits minimized SoP expressions, machine-readable, documented; (2) a
differential golden proving the minimizer correct by comparing the minimized
expression, evaluated exhaustively, against exhaustive simulation of the *original
drawn circuit* (not the table it was derived from). It sits at the end of the
FEAT-C564 chain (#648 minimizer, #649 bound/refusal, #650 display/export) and joins
#646 (TASK-C563-4, the truth-table-extraction batch flag). None of the prerequisite
code exists yet — confirmed by grep: no `minimiz`/`SoP`/`Quine`/`McCluskey` hit
anywhere under `src/`, and #563's own review comment confirms "JLS has no analysis
path" at HEAD (`grep -rn "Analyze" src/` matches one unrelated comment in
`VhdlEmitter.java`). This review does not re-litigate the earlier tasks' soundness;
it attacks #651 as written.

## Findings, most severe first

**1. (High) A coordination gap the project's own review process already found — but never wired into this issue's machine block.** Issue #646's 2026-08-04 comment explicitly names #646/#651/#656 as three headless surfaces landing on one batch CLI and lists three coordination points: (a) one flag table / mutual-exclusion policy via #372 (`FlagSpec`), (b) "the frozen CLI contract" — #686 (TASK-C524-1) freezes invocation/exit-status/artifact-path/xUnit-schema behind a conformance test, and the comment states plainly *"Three new flags landing after that freeze are contract additions and owe it a conformance case each. Landing before it means the freeze must enumerate them"* — and (c) output-encoding determinism, "whichever lands first sets the encoding; the other two match it." None of these three points was ever added as an `ordering_after` edge or boundary note on #651 (or on #646/#656). #651's boundary note treats #524 as settled fact ("The batch CLI stability promise is #524; this adds a flag under it") when #524/#686 is itself **open and unimplemented** — `docs/batch-interface.md` today documents exactly three exit statuses (0/1/2, confirmed in `ARCHITECTURE.md`'s error-reporting section and in `docs/batch-interface.md` §1), not the status-3 world #524 proposes. An executor picking up #651 today has no way to know from the issue itself whether to build against the current three-status contract or wait for #524/#686, and the sibling coordination comment that answers this was posted on a *different* issue (#646) four days after #651 was filed, with no cross-link added to #651.
   - *Recommendation:* add `ordering_after` covering #686 (or an explicit "lands before the freeze, #686 must enumerate it" decision recorded in the boundary notes), and cross-link the #646 coordination comment.

**2. (High) Invocation shape is unaddressed: "table in, expressions out" doesn't fit the documented batch-mode grammar.** The entire existing batch CLI is built around a mandatory circuit-file operand: `docs/batch-interface.md` §1 gives `jls -b [-s paramfile] [-t testfile] [-d limit] [-vcd file] [-r printer] [--] circuit.jls` — every existing batch flag modifies or observes a simulation of `circuit.jls`. #651's Outcome describes a flag whose *input is a table*, with (per AC-1's differential check) some separate mechanism supplying "the original circuit" for comparison. AC-3/AC-4 never say whether this flag (a) still requires a `circuit.jls` positional operand and reads the table from a companion file, (b) takes a table file as its own positional operand with no circuit at all, or (c) requires both a table and a circuit and is therefore not truly "table in, expressions out" as the Outcome claims. Given the boundary note's own framing — "this adds a flag under [the existing] contract" — the issue asserts drop-in compatibility with an invocation grammar it never checks its input shape against.
   - *Recommendation:* state the exact CLI invocation form (operand(s), flag name, whether `-b circuit.jls` remains mandatory) before implementation starts.

**3. (High) AC-1/AC-2's "proved equivalent... by exhaustive differential test" overclaims what the acceptance criteria actually verify, and the criteria are gameable.** The title asserts the minimized expression "is proved equivalent to the original circuit," and the Outcome claims "the correctness claim is settled by a differential golden." But AC-1 requires this for exactly **one** 4-input circuit, and AC-2's fixture set — "don't-cares, multi-output tables and a single-minterm degenerate case" — has no stated minimum count, no requirement to exercise inputs near TASK-C564-2's (#649) exponential bound (exactly where prime-implicant-growth bugs are likeliest), no case exercising TASK-C564-1's (#648 AC-3) documented tie-break rule for equal-cost covers, and no constant-0/constant-1 (tautology/contradiction) degenerate case alongside the single-minterm one. A minimizer that is correct on three or four hand-picked small fixtures but wrong on tie-breaking, on wide inputs, or at the bound boundary would pass AC-1/AC-2 in full while the Outcome's "correctness claim is settled" remains false for real use. This is exactly the "acceptance criteria the real goal could fail behind" pattern.
   - *Recommendation:* set an explicit minimum fixture count and require coverage of (i) a tie-break case, (ii) a bound-adjacent input width, (iii) constant-0 and constant-1 outputs, before AC-2 can be called satisfied. Replace "proved equivalent" in the title/Outcome with a claim proportionate to a fixed, finite fixture set (e.g. "verified equivalent on the documented fixture set").

**4. (Medium) The issue conflates two different deliverables — an internal correctness proof and a shippable CLI feature — without saying which one a grading script actually gets.** The Outcome opens "Two things a grading script needs" and then describes AC-1/AC-2 (a differential golden, which reads as an internal JUnit test akin to `BatchSimulationGoldenTest`/`SequentialGoldenTest` per `ARCHITECTURE.md`'s test-layout section) and AC-3/AC-4 (a CLI flag). Nothing in the issue states whether the differential-equivalence check is itself invokable by a grader (e.g., "verify this expression against this circuit" as a batch operation) or is purely a one-time dev-side proof that ships the minimizer with confidence. If it's the latter — which AC-1/AC-2's wording ("golden-tested," "fixture set") strongly suggests — then only AC-3/AC-4 is actually "what a grading script needs"; AC-1/AC-2 is quality assurance on the feature, not a deliverable a script consumes. The framing overstates what ships as usable surface.
   - *Recommendation:* split the Outcome into "what ships to users/scripts" (the flag) vs. "how we know it's correct before shipping" (the golden), and drop the "two things a grading script needs" framing if only one of the two is externally callable.

**5. (Medium) No shared table-serialization contract with sibling #646 (TASK-C563-4).** #651 consumes a "table" as batch input; #646 produces a "machine-readable table" as batch output from truth-table extraction. Neither issue states these must be the same schema, and neither assigns ownership of defining one shared table-file format. Built independently (plausible, since they're different features under different parents, #563 vs #564), the two could diverge and force rework or brittle translation glue exactly where the pipeline is supposed to be seamless ("#565's synthesis path... consumes FEAT-C31-2 expression").
   - *Recommendation:* name one issue (likely #646, since it lands first per its own `ordering_after`) as owner of the canonical table-file schema, and have #651 explicitly consume it by reference.

**6. (Medium) No coordination with #650 (TASK-C564-3) on expression notation.** #650 AC-2 requires a "documented and stable" operator convention for the GUI/export display, and AC-3 requires the exported text be byte-identical to what's displayed. #651's batch machine-readable output is a second serialization surface for the same minimized expressions and has no stated obligation to reuse #650's notation contract. Left unresolved, a student could get one operator convention from the GUI-export path (#650) and a different one from the batch flag (#651) for output derived from the same minimizer.
   - *Recommendation:* add a boundary note requiring #651's batch schema to reuse (or explicitly map to/from) #650's documented notation.

**7. (Medium) Exit-code semantics for the TASK-C564-2 bound refusal aren't reconciled with the currently-documented contract.** #649 (TASK-C564-2) defines the above-bound refusal as "the feature's real output above the line" — an expected, documented outcome, not a crash. The *current* CLI contract (`ARCHITECTURE.md` "Error-reporting contracts": exit 1 = runtime failure, exit 2 = usage error) has no natural slot for "expected complexity refusal that isn't a bug." Overloading exit 1 blurs an existing, load-bearing semantic distinction; the clean fix (a dedicated status, per #524/#686's proposed status 3) doesn't exist yet and isn't an ordering dependency of #651 (see Finding 1). AC-4 punts by saying only "documented alongside the existing batch flags," which doesn't resolve which status code the refusal actually uses.
   - *Recommendation:* state explicitly which of the *currently existing* exit codes (0/1/2) the TASK-C564-2 refusal maps to, or make landing after #524/#686 (which adds status 3) an explicit prerequisite.

**8. (Low) Ordering is transitively fragile.** `ordering_after` lists only #648, #649, #646 — not the deeper root dependency (#872, the CAP-09 combinational-cone extractor), which was itself filed only on 2026-08-08 as a correction to a hole discovered in #563's chain (per #563's own review comments, posted after #651 was already open for four days). This is mechanically fine (transitive ordering through #646→#641→#872 holds), but it means #651 has no direct visibility into the root dependency and would not surface for re-review if that root chain changes again, the way #563's did.

## What's solid — no rework needed

- **The core methodological choice is sound.** Checking the minimized expression against exhaustive simulation of the *original circuit* rather than against the extracted table correctly avoids a shared-bug blind spot (a bug common to extraction and minimization canceling out if checked table-vs-table); this is a real and non-obvious testing-oracle hazard the issue gets right.
- **AC-3's determinism requirement is consistent with #648's (TASK-C564-1) AC-3 tie-break mandate** — the two sibling tasks agree on what "deterministic" means here.
- **The boundary note correctly distinguishes this task's equivalence assertion from #565's TASK-C565-4 round-trip assertion**, avoiding a real conflation risk that the #563/#564/#565 dedup review had to work through elsewhere in this issue family.
- **CAP-31 citations (AC-1, AC-5) are accurate** against #515's actual acceptance-criteria text — no misquotation found.

## Bottom line

The task's intent and its core correctness-testing idea are defensible, but the issue
as written under-specifies the interface it's adding (invocation shape, table
schema, exit-code mapping) and sets acceptance criteria loose enough that a shipped
minimizer could pass every stated AC while still being wrong outside the four
hand-picked fixture shapes. The most damaging gap is that a coordination problem the
project's own review process already identified (the #646 comment on the CLI-freeze
ordering) was never propagated into this issue's own machine block — an executor
reading #651 alone would not know it exists.
