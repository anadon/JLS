# Issue #345: FEAT-039: a balanced-ternary machine is specified, independently emulated, assembled in-jar and drawn — and the drawn one agrees with the emulator per retired instruction
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Two goals are braided into one deliverable, and they have different natural shapes.

**(a) The teaching claim.** #295 states it plainly: "a student types `TRIT -5`, gets `-++`, and has
just seen a number system with no sign bit, in a tool they already use." That is squarely JLS's
identity — README line 5, "an educational digital logic circuit editor and simulator" — and
`docs/grand-architecture.md:50` already names the CPU-teaching trajectory as one of three funded arcs.

**(b) The trust claim.** Two implementations, spec-first, compared per retired instruction, with a
seeded-misreading test. This is the project's standing answer to "you wrote both sides," and #295 is
right that it is not ternary-specific.

The braid is the problem. Goal (a) wants the smallest drawable non-binary machine that prints. Goal
(b) wants the *hardest* verification subject available. #345 sizes the artifact for (b) and then
justifies it with (a) — and ternary is the worst possible first subject for (b), because no third
party on earth can check either side of the comparison. Goal (b) belongs on #202's binary machine,
where GCC, spike, and a hundred RV32 emulators exist to be the third opinion.

## Reframe 1 — decide Open Question 1 for binary-encoded ternary now, and invert the dependency

The issue calls BET the "fallback" and native radix 3 the real thing. On the merits it is the other
way round, and this is the single highest-leverage decision in the feature.

- JLS is word-level, and #295 already concedes ternary *devices* are out of scope. A "native" trit
  in the engine is therefore also an encoding — one hidden inside `BitSet`'s successor rather than
  visible on the canvas. Nothing about the pedagogy of "no sign bit" survives or dies on which.
- The entire teaching payload is *architectural*: symmetric range, the three-way branch, the
  contentious rounding rule. All three are unchanged at two bits per trit.
- BET is drawable **at HEAD, today**. A balanced-ternary min/max/negation/half-add lane is one
  `TruthTable` element; `RegisterFile`, `Memory`, `Mux`, `Decoder`, `Splitter`/`Binder`, `Adder`,
  `TriState` are all in the 35 registered types. Zero new element classes, zero of the sixteen-step
  ceremony in ARCHITECTURE.md.
- That removes #322 (28-36 mw), #344 (8-12 mw) and #361 (9-13 mw) from this feature's critical path
  — 45-61 mw of substrate the demonstration does not need. Those features may be worth building for
  their own reasons; they should not be this one's prerequisites.

The trade is real and should be printed: 2n-bit buses, roughly double the datapath element census,
and an illegal-lane detector that must be live rather than optional. Against that: the drawn machine
could exist this year instead of after a 172-265 mw capstone spine, and — decisively — #361's own
acceptance argument ("a ternary datapath is drawable, probeable, dumpable, testable, exportable")
gets a working existence proof *before* the family is built, instead of the family being built on
the promise of a machine that waits on it. Today the tail wags: the machine is blocked on the
substrate, and the substrate's completeness claim is largely proved by the machine.

## Reframe 2 — do not invent JLS-T3; re-create Setun-70

Integration criterion 1 ("a third implementation could be checked") is unfalsifiable as written: the
corpus is implementation-independent in *form*, but there will never be a third implementation of an
ISA invented in this repository last week. Adopt a published balanced-ternary architecture instead —
Setun / Setun-70 is the obvious candidate; its instruction set, its trit-word sizes and its
three-way branch are documented, and outside emulators exist. Consequences:

- Open Question 2 (division/rounding rule) stops being a fork in the plan and becomes a lookup, with
  a historical answer to defend rather than a preference to defend.
- "Two implementations by one author," which the issue names as its principal risk and admits no
  amount of testing addresses, gets a genuine mitigation rather than a discipline.
- The artifact becomes a *re-creation of a real computer that was not binary* — a strictly better
  story for the audience in §"Intended Audience" than a bespoke ISA nobody has heard of.

If a bespoke ISA is kept anyway, criterion 1 should be honestly downgraded to "the corpus has no
in-tree type dependency," which is all it can actually test.

## Reframe 3 — the third engine already ships: Verilog export under Icarus

The feature's whole anxiety is oracle independence, and it never mentions HDL export. The tree has
`src/jls/hdl/VerilogEmitter.java`, the README documents `-export out.v`, and CI already installs
`iverilog` (README, "Optional development tools"). Under BET the drawn machine lowers to ordinary
binary Verilog with no new emitter work. Export it, run the conformance corpus under Icarus, and the
comparison becomes two implementations *plus one execution engine written by nobody here* — a
categorically stronger claim than invariants 1 and 2 can produce, at near-zero marginal cost, and it
reuses #202's direction 3 rather than duplicating it. This should be an integration criterion.

## Reframe 4 — cut QDOS; I am disregarding scope item 6 as written

Scope item 6 and TASK-0084 want a single-tasking monitor with a command line and (per #295 step 2) a
filesystem, `DIR`/`TYPE`/`RUN`. This is the most cost-uncertain piece, the largest contributor to
the unnamed 8-20 mw residual, and it teaches DOS rather than ternary. Replace it with:

1. a self-check ROM whose verdict is one console byte, and
2. three programs of a few dozen instructions each, one of which prints `-++` for -5.

That deletes the command parser, the filesystem, most of the #364 byte-budget dependency, and makes
the golden transcript trivially stable across platforms and JDKs — criterion 5 becomes cheap instead
of load-bearing. If a monitor is genuinely wanted, it belongs on the binary machine (#202), where
the guest software is reusable and a third-party toolchain exists to build it.

## Reframe 5 — `riscv/` is this feature's own precedent, and the plan erases it instead of learning from it

`riscv/` already contains TASK-0082 and TASK-0083 in another radix: `jlsbuild.py` (322 lines,
netlist → `.jls`), `build_cpu.py` (477, a drawn RV32I datapath), `riscv_ref.py` (975, independent
emulator + assembler), `fuzz_diff.py` (randomized differential testing), `verify.py` (end-to-end
architectural-state comparison), plus a GUI driver — ~2,500 lines of stdlib Python that works.
D5 forbids routing through it and #335 deletes it, for defensible reasons (not in the jar; the
reference shares an author with the design). Fine — but the *method* is the asset, and #345 costs
its rebuild from a 200-250 LoC/mw ratio without once citing how long the existing instance actually
took. Before this feature is reported as funded, it should state what `riscv/` cost and what its
bring-up failures were. If the honest answer is "weeks," then 18-30 mw plus an 8-20 mw residual is a
statement about the chosen route — Java, in-jar, behind five features — and not about the problem.

## Graph hygiene the reframing depends on

- **#326 is closed as a duplicate of #202** (2026-08-04, two days after this issue's link phase).
  §"Related work" still cites it as a live sibling, and Open Question 4 asks who owns TASK-0038
  among three claimants of which one no longer exists. The live question is #337 versus #202.
- With #202 now carrying the construction verbs, the boundary-at-a-time bring-up method, the
  census-as-enforced-budget and the falsification criterion, **this feature is the second
  instantiation of a method #202 builds first**. It should inherit in one line rather than restate
  the discipline — and it should be sequenced after #202 has proved the method on a machine with
  third-party oracles, not in parallel with it.
- `docs/plan/`, `docs/machine-calibration.md` and `docs/parity-contract.md` are absent from the
  working tree at HEAD, so every §Cost citation and the `machine-calibration.md:87` self-consistency
  finding resolve to nothing for a reader of the repository. The Definition of Done requires those
  to resolve; today they do not.

## What I would not touch

The spec-before-emulator cut is correct and is the best judgement in the issue — writing the corpus
from the specification rather than from either implementation is exactly the defect criterion 3
exists to catch, one level up. Integration criterion 3 (seeded misreading caught on exactly one
side) is the criterion that makes the rest evidence, and criterion 6 (census checked against the
loaded circuit, not asserted in prose) is the right instinct. The single-tasking, no-interrupt
guest is genuinely a feature and the issue is right to refuse to apologize for it. Keep all four.

## Missing entirely

Nothing in #345 or #295 proposes to check the teaching claim that justifies the whole capstone. One
lab handout and one instructor using it once would be worth more to this project's arc than the
fifth decimal of the declared architectural field set — and #295's own re-planning protocol says the
feature has no beneficiary if #295 is descoped. The beneficiary is currently an assertion.

## Verdict

**endorse-with-reframing.** A drawn machine that is not binary, checkable and readable in the same
editor students already use, is the right thing for JLS to become. The route is heavier than the end
requires and weakest exactly where it cares most. Concretely: decide Open Question 1 for BET, drop
#322/#344/#361 from this feature's `blocked_by`, adopt a published ISA, add the Icarus arm, replace
the monitor with a self-check ROM plus three demo programs, and sequence after #202 rather than
beside it.

If Open Question 1 is instead decided for native radix, my verdict drops to **rethink**: the feature
then is not a demonstration at all but a 45-61 mw substrate bet with the demonstration held hostage
to it, and the first artifact a student can see moves years out for a distinction — encoded versus
native trits — that no student can perceive.
