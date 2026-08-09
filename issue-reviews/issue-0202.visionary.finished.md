# Issue #202: RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the machinery and #202 asserts one thing: *JLS should be able to hold a real
processor, and the fact that it can should be load-bearing.* That claim is squarely
inside the project's arc. `README.md` opens with "educational digital logic circuit
editor and simulator"; `docs/grand-architecture.md` §2 names "a serious datapath /
CPU teaching tool" as trajectory one of three and cites `riscv/` as the evidence it
is already funded; and ARCHITECTURE.md's simulation-strategy decision (#221) makes
"the #202 RV32I integration golden run as a differential oracle" the *binding
equivalence criterion* on any future compiled-evaluation pass. The project has
already spent its trajectory-one credibility on this issue. I am not arguing it away.

What I am arguing is that #202 bundles three things that share a noun and nothing
else, and that the bundle is now actively costing more than it earns.

## The bundle, and why it pulls against itself

Three purposes hide under "the CPU":

| Purpose | Counterparty | Wants the artefact to be |
|---|---|---|
| Simulator differential net (dir. 1) | a program-level emulator | broad in element coverage, cheap to regenerate, disposable |
| Teaching object (dir. 2) | a human reader | small, hierarchical, hand-laid-out, stable |
| Exporter differential net (dir. 3) | `iverilog` on exported Verilog | drawn *only* from `HdlExporter.EXPORTED` |

These are not three harvests of one artefact. They are three different oracles that
happen to be pointed at the same drawing, and their requirements conflict:

- Decomposing the machine into `SubCircuit` boundaries — which is exactly what the
  absorbed #326 criteria demand for readability ("one `.jls` per boundary plus a top
  level that instantiates them") — makes it **strictly less exportable**.
  `HdlExporter` rejects `SubCircuit` as firmly as it rejects `Memory`
  (`/home/user/JLS/src/jls/hdl/HdlExporter.java:422-428`, and the rejection is
  *pinned as intended* by `HdlPolicyTest.subCircuitIsRejectedCleanly`). Direction 2
  moves direction 3's goalposts further away.
- Direction 1 wants many programs cheaply; direction 2 wants one machine, stable and
  reviewable. #278's open question "promote 2 or all 11?" is that tension, unresolved.

A feature whose sub-goals move each other's gates is not a feature. It is a topic.

## Reframe 1 — cut along the oracle, not along the artefact

Give #202 exactly one thing to own: **the machine as an artefact** — drawn,
hierarchical, regenerable, readable, census-bounded. Let each oracle be its own
feature that *consumes* it:

- *Simulator parity* is already a live, unblocked program (#278 today, per-retirement
  parity later). It is gated on nothing and should not be reported inside an issue
  carrying `blocked_by: [59, 62, 317, 324, 325, 337, 343, 347, 364]`.
- *Exporter parity* has a wholly different failure mode (an emitter bug, not a
  simulator bug), a different toolchain, and a different beneficiary. See reframe 2.

The evidence that the current cut is wrong is in the issue's own comment stream: four
of the eight comments exist solely to re-explain which halves of #202 are and are not
touched by #326, #347, #364, #391/#425. That is the shape of a decomposition fighting
its contents.

## Reframe 2 — the export oracle is a corpus property, not a CPU (I am disregarding IC2 as written)

IC2 says: "exported CPU under `iverilog` matches JLS batch bit-for-bit on the full
directed suite." I would drop that as the acceptance criterion. It is the maximal
test — it requires the two hardest exporter features (`Memory`, `SubCircuit`) before
it yields *one bit* of signal, which is why direction 3 has been "planned, gated" for
three status cycles while #59 shipped `ShiftRegister` export and moved on.

The property JLS actually wants is smaller and available today:

> for every circuit *C* in the golden corpus that `HdlExporter` accepts, and every
> committed test vector *V*: `iverilog(export(C))(V) ≡ BatchSimulator(C)(V)`.

`/home/user/JLS/test/jls/hdl/IverilogCompileTest.java:24` already says the quiet part:
"Simulation parity against JLS batch goldens is the next slice." Today the harness
only asserts that goldens *compile*. Closing that gap is a parameterized test over
the existing corpus, buildable now against the 22 classes in `EXPORTED`, with zero
dependence on #59/#291/#292. It grows for free whenever `EXPORTED` grows — the
`ShiftRegister` addition in PR #270 would have been covered on arrival rather than
noted in a status comment. And it makes the CPU the *last row of the table* rather
than the price of admission: when `Memory` and `SubCircuit` export land, the machine
joins the same harness with no new apparatus.

Concretely: direction 3 should leave #202 entirely and become a feature under the HDL
program. #202's dependence on #59 then disappears, not because the work is descoped
but because it was never #202's work.

## Reframe 3 — make the fixture hostage disappear

`test/fixtures/riscv-sum1to10.jls` is an opaque blob whose only regeneration path is
`riscv/make_cpu.py`, and #278 proposes to commit two more of them
(§7.7: "the exact `make_cpu.py` invocation is recorded in the test javadoc"). Decision
D5 deletes `riscv/`. That is a direct contradiction the merge comment records but does
not resolve: #202's new DoD says "no committed artefact requires anything under
`riscv/` to regenerate," while its only filed, actionable child manufactures three
such artefacts.

There is a route where the problem stops existing. **Do not commit the circuit at
all.** Commit the assembly source, the expected architectural state, and build the
machine at test time from the in-tree construction program (TASK-0038's verb set on
`jls.collab.op` — see `/home/user/JLS/docs/operation-layer.md`, and `Circuit.save`
canonicalization under #166). Then:

- a format epoch costs a re-run, not a re-draw, *and* costs no fixture migration;
- byte-identical regeneration stops being a criterion to test and becomes the way the
  test works;
- the "2 or all 11 programs?" question evaporates — eleven `.s` files cost about as
  much as one;
- `riscv/jlsbuild.py` (322 lines of hand-written save-format emission that nothing in
  `mvn verify` pins — a second, out-of-tree implementation of the file format) has no
  successor to justify.

This also reverses the planned "Java port of the fuzzer" framing. The valuable port is
not the *fuzzer*; it is the *builder*. Once construction is Java-side and op-based,
randomized programs are a loop, and the project's existing generative-property muscle
(`test/jls/GenerativeRoundTripFuzzTest.java`, seeded, dependency-free, #160) is the
pattern to copy rather than a Python subprocess to re-home.

## Reframe 4 — readability comes from hierarchy, not from auto-layout

`blocked_by: 62` (auto-layout) as the gate on the sample circuit is a misdiagnosis. A
580-element flat schematic auto-placed by ELK is not a teaching object; nobody has
ever learned a datapath from a 580-node force-directed graph. The absorbed #326
criteria already contain the real answer — per-boundary `.jls` files, each small
enough to lay out deliberately, plus a top level that instantiates them. The current
overlapping coordinates are an artefact of `build_cpu.py` being a *netlist* emitter
with no geometry model, not evidence that JLS needs a layout engine before it can
ship a readable CPU. Geometry is a property the construction program should own.
Drop #62 from this issue's gates; auto-layout remains valuable, just not here.

## Reframe 5 — buy independence instead of building it

The merge makes much of `docs/machine-calibration.md:87`: the reference emulator "was
written by the same author as the design under test, so it is a self-consistency
oracle, not an independent one," and concludes that #343 (`jls.mach`, a second RISC-V
implementation written in-house) is a hard prerequisite. Two observations.

First, the objection is narrower than stated. For **simulator** bugs — the #221
equivalence criterion, the thing ARCHITECTURE.md actually leans on — `riscv_ref.py`
*is* independent of the artefact under test, because the artefact under test is
`jls.sim`, not the CPU. Common design authorship threatens ISA-conformance claims,
not the regression net.

Second, for the claims it does threaten, the project has already costed a genuinely
external oracle: `/home/user/JLS/docs/standards-adoption/05-riscv-compliance.md`
(#65, #259) specifies `riscv-arch-test` under RISCOF against the Sail reference model,
signature-diffed, with the exact claim wording and a pinned-SHA scheduled lane. A
third-party test corpus plus a third-party golden model is more independent than any
second emulator a single maintainer writes, and it is *less* code in a repository
whose stated architectural stance (grand-architecture §2) is "orchestrate external
tools, never reimplement." I would make riscv-arch-test the independence story and
treat #343 as the thing that must justify itself against it.

## What I would keep unchanged

The core intuition, and #278's specific gap — `sum1to10` never executes a `lw`, so
load/store timing is unpinned by the only whole-CPU golden. That is a real hole and
worth closing this week. The `RiscvCpuGoldenTest` javadoc
(`/home/user/JLS/test/jls/RiscvCpuGoldenTest.java:30-37`) is an honest statement of
the class of net a CPU golden provides and should survive any restructuring.

## Recommendation

Rethink the decomposition, not the ambition. Reduce #202 to the drawn, regenerable,
hierarchical machine; re-home the export oracle into the HDL program as a
corpus-parameterized parity property; make the golden's build-at-test-time so the
fixture hostage and the `riscv/`-deletion contradiction both vanish; drop the #62
gate. Resolve the #301 capstone pointer that currently names a closed issue before
anything else is filed here — a feature with nine `blocked_by` edges and no live
consumer is the state that produces reviews like this one.
