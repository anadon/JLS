# Issue #414: TASK-0045: a drawn circuit exports as a Yosys JSON netlist that Yosys itself reads back with the same interface
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the eleven checklists and #414 is one claim: *JLS's circuits should be citizens of the
open synthesis ecosystem, not just consumers of it.* That claim is right, it is the highest-value
unclaimed item on the HDL arc, and the writer is genuinely the small half of it — the read side
(`src/jls/hdl/yosys/YosysNetlist.java`, 953 lines; `CellValidator.java`, 276; `JsonValue.java`, 580)
already paid for the value model, the bit sentinels and the schema knowledge. I endorse building it.

But the issue undersells what it is building, and two of its own acceptance criteria pull against
the thing it is for. Below: one framing correction, one criterion I would disregard outright, and
three simplifications.

## Reframe 1: this is not "a third emitter", it is the hub format

The issue files this under "a third `HdlEmitter` beside Verilog and VHDL". That framing is
architecturally backwards, and it matters because it sets the ambition too low.

`HdlModel`'s statement kinds are *word-level*: `AdderStatement`, `SelectStatement`,
`ShiftStatement`, `RegisterStatement`, `TriStateStatement`. Yosys's `$`-cell library is also
word-level (`$add`, `$mux`/`$bmux`, `$shl`/`$shr`, `$dff`, `$tribuf`, with WIDTH parameters).
The JSON netlist is therefore the *most faithful* rendering of `HdlModel` that exists — closer to
the IR than either shipped printer. `VerilogEmitter` (752 lines) and `VhdlEmitter` (1,149) have to
invent syntax the IR does not have: look at `VerilogEmitter.stateMachine` (`:470-`) synthesizing
`always @(posedge clk) case (...)`, or `shift` (`:600-`) reaching for `$signed(...) >>>`. Those are
*lossy prints*. The netlist is a structural isomorph.

The consequence the issue never states: once JLS emits RTL-level Yosys JSON, every Yosys backend
becomes a JLS output format for free — `write_verilog`, `write_edif`, `write_blif`, `write_spice`,
`write_smt2`, `write_btor`, `write_cxxrtl`, `write_firrtl`. That is the actual answer to the
"6-10 maintainer-weeks of bit-level lowering" that #321 prices and rejects for the gate-level
formats, and it is also the answer to #369's equivalence-checking need (`write_smt2` + an SMT
solver is exactly how you check two circuits equal). The threats section worries that "the KiCad and
SPICE emitters that consume the same model" are exposed to positional-pin bugs — the right response
is that **those emitters should not be written at all**; they should be recipes over this output.

So: name it the hub. Say in the emitter's javadoc and in `docs/hdl-support-research.md` that the
netlist is the primary structural export and Verilog/VHDL are human-readable prints of the same
walk. That single sentence changes what the next contributor builds.

## Reframe 2 (disregarding a stated criterion): P8 / H1 / §7.12-claim-3 are the wrong constraint

I am explicitly disregarding **P8** ("`CellValidator.validate` returns `List.of()` for every
golden") and the `V_w ⊆ V_r` law in §7.10 as *hard* acceptance criteria. They conflate an internal
consistency property with an external correctness property, and enforcing them will amputate the
writer.

`CellValidator`'s nineteen cells are not "the format's vocabulary". They are what survives *JLS's
own import synth script* — `docs/hdl-support-research.md:353-359` shows the pipeline
(`read_verilog; proc; opt_clean; techmap -map jls_map.v`), and the class javadoc says so plainly:
`$sub`, `$eq`, `$pmux` are called "pipeline leftovers … their appearance means a techmap rule is
missing". That is a *post-mapping* allowlist for untrusted input, deliberately narrow. Pointing it
backwards at the writer means:

- `ShiftStatement` → `$shl`/`$shr`/`$sshr`: **not in the nineteen** → refused.
- `PriorityCaseStatement` → `$pmux`: **explicitly a teachable reject** → refused.
- `SelectStatement` beyond a clean `$bmux` shape → refused.
- `StateMachineStatement` → refused (the issue already plans this).

Four of eleven kinds, including two that export to `.v` today. The user-visible result is exactly
the asymmetry §11 flags and then accepts: *a circuit that exports to Verilog but not to the netlist*.
For a hub format that is not a documented wart, it is the failure of the whole idea.

The correct law is `V_w ⊆ V_yosys` (what the tool reads — pinned by P9, which is real evidence),
plus a *reported* comparison against `V_r` rather than a gate. And #321's round-trip criterion I3
should be restated as `import(techmap(emit(c))) ≅ c` — run the emitted netlist through the same
Yosys pipeline the importer already assumes, instead of demanding the writer pre-restrict itself to
the importer's post-techmap subset. That reframing makes the conflict disappear rather than trading
coverage for it.

## Reframe 3: derive the CLI dispatch from the seam instead of teaching two switches a third suffix

O3 is treated as "two decision points must both learn `.json`". The deeper observation is that
`src/jls/boot/HdlModule.java:41-43` already contributes both emitters to
`HdlExtensionPoints.EXPORTER`, and `HdlEmitter.fileExtension()` already exists (`VerilogEmitter:49`,
`VhdlEmitter:76`) — and `JLSStart` ignores all of it, hand-constructing `new VerilogEmitter() :
new VhdlEmitter()` at `:381-385` and hand-listing suffixes at `:1088-1091`. The seam publishes the
answer and the caller re-derives it wrongly (any non-`.v` name silently becomes VHDL).

Concretely: resolve the emitter by `registry.contributions(EXPORTER)` matched on
`fileExtension()`, and build the usage-error allowlist from the same list. Then `.json` costs zero
CLI edits, the "silently becomes VHDL" bug class is gone permanently, and #213/#264's board-aware
emitter and any future backend are free. This is strictly better than #321's own rejected
alternative 3 ("add a fourth branch") and is what decision #223 (typed seam catalog) exists for.
It also removes the need for `.vhd`/`.vhdl` special-casing to live in `JLSStart` at all.

## Reframe 4: 70 byte-exact goldens is the wrong evidence shape

O10 corrects the corpus to 37 `.v` + 33 `.vhdl`, and §11 accepts that "golden-writing effort scales
with it" plus one wholesale regeneration when #373 lands. Step back: a Yosys netlist's net ids are
**allocator-assigned integers**. A byte-exact golden over 70 such files pins the allocator, not the
meaning; any insertion renumbers everything and the diff is global and unreviewable. You will
regenerate all of them for #373, and again for any allocator change, and nobody will read the diff.

Better: 5-8 hand-reviewable goldens over structurally distinct circuits (gate, adder, register,
tristate, bundle/bitmap, FSM), and for the remaining corpus assert *properties* — schema validity,
`parse(write(v)) == v`, byte-identical re-emission, and Yosys interface equality (P9). That keeps
every claim the issue actually cares about while making the #373 regeneration a non-event.

## Reframe 5: the FSM refusal, and the artifact #369 should actually diff

**H3 is weaker than it reads.** "The external tool does the lowering" is false in this direction:
Yosys lowers *Verilog*, which JLS already emits. JLS already owns the FSM lowering — the state
register, the next-state table and the output table are all in
`HdlModel.StateMachineStatement` and are already rendered case-by-case in `VerilogEmitter:470-560`.
Emitting `$dff` + a mux tree over the same tables is a rendering change, not a second synthesis
pass. I would lower it. If you nonetheless refuse, the refusal message must name the recipe
(`export .v`, then `yosys -p 'read_verilog; synth; write_json'`) so the capability is redirected
rather than lost.

**And a warning about the downstream consumer.** #321 sells this netlist to TASK-0111/#369 as
"a machine-readable artifact a grader can diff". It is a bad grading artifact for the same reason
it is a bad golden: integer net ids and cell names are allocation artifacts, so a semantically
identical student submission diffs dirty and a subtly wrong one can diff clean. Grading should
compare *behavior* (the existing test-vector/VCD surface) or a canonical structural fingerprint —
or, better, use `write_smt2` equivalence via the hub. Say so here, because #414 is where that
expectation is being minted.

## What I would keep exactly as written

The `default`-less switch over the eleven kinds (P10), the shared `JsonValue` value model and the
reuse of `YosysNetlist.BIT_*` (O6), the `MAX_DEPTH` symmetry (P6), no new JSON dependency, no
`ProcessBuilder` in `src/`, the skip-when-absent `ToolLocator` idiom, and P9 as the strongest
check — all of that is exactly right and is the part of this issue that will still look correct in
five years. Note also that the evidence-pin comment (#493) means `HdlExporter.REJECTED` and
`HdlPolicyTest.exportPolicyIsTotalOverTheElementRegistry` do **not** exist on `master`
(`grep REJECTED src/jls/hdl/HdlExporter.java` finds nothing; only a prose mention at `:88`), so
P11's "inherit exactly this coverage" currently has nothing to inherit — that dependency on #492
should be stated rather than assumed.

## Summary

Build the writer. Then: call it the hub, not the third printer; replace the `V_w ⊆ V_r` gate with
`V_w ⊆ V_yosys` plus a reported comparison; resolve the CLI from the `EXPORTER` seam instead of
teaching two switches a suffix; keep 5-8 goldens and make the rest properties; lower the FSM or
redirect it by name. Every one of those makes the resulting system smaller than the one #414
specifies while making it reach further.
