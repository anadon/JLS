# Issue #63: HDL Stage 3: black-box HDL component — hand-written header scanner for ports, external GHDL/Icarus co-simulation
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Strip the four planned tasks, the five canvas states and the transport ledger away and
one sentence remains, from the issue's own Intended Audience: *a lab can contain drawn
circuits and student-written Verilog/VHDL modules in one simulation.* That end is worth
having. Everything else in the issue is one particular mechanism for reaching it — JLS
hosting an HDL simulator process inside its own event loop, per component, in lockstep —
and the mechanism is the part I think is wrong.

**I am explicitly disregarding P1's transport half, P3 (latency), the T3 harness/transport
and T4 tasks, and the five-state canvas matrix.** They are well-specified consequences of
a premise I do not think survives contact with what this repository already ships.

## The premise, checked against the tree

Verified at the checkout, on `master`:

- `grep -rln "ProcessBuilder" src/` → **nothing**. Not one line of shipped source starts a
  process. T3 is first-mover work in a ~69k-line codebase, not an extension.
- `grep -rln "jls\.hdl\.scan|ScannedModule|ScannedPort" src/ | grep -v src/jls/hdl/scan/`
  → **empty**. The 2,566-line scanner has no consumer. That much of the issue's framing is
  real and measured.
- But `src/jls/hdl/` also contains `yosys/` (`YosysNetlist`, `CellValidator`,
  `YosysLocator`), `imp/` (`NetlistImporter`, `ImportResult.saveText()`), `layout/`
  (`HeuristicLayeredLayouter`), and `HdlExporter`/`VerilogEmitter`/`VhdlEmitter` at
  1364+752+1149 lines. **The machinery for turning HDL into JLS elements, and JLS
  elements into HDL, is already in the tree.** #61's importer already writes `ELEMENT`
  blocks and `WireEnd` chains that load through the real loader.
- `docs/vcd-interop.md` and `test/jls/AutogradeBridgeExampleTest.java` exist: #216 shipped
  the outward bridge (drawn circuit → Verilog → iverilog → VCD → viewer/autograder).
- The GitHub parent, #59, is **CLOSED**. `serves_capstones: [59]` names a closed issue;
  #304/#306/#310 grade this feature merely *beneficial* and none carries it. The DoD line
  "notify every capstone" cannot be discharged as written. Nowhere in 13 comments does an
  instructor appear asking for this.

So: the most expensive item on the HDL roadmap, first-mover in its riskiest dimension,
with a cost band its own thread calls 4x–7x over its task sum and UNOWNED, has no named
beneficiary — while two cheaper pipelines that reach the same user-visible outcome are
already 70% built next door.

## Reframing A — elaborate the box; do not converse with it

Make the HDL component an element whose **body is elaborated once, not executed live**:
Yosys `write_json` → `YosysNetlist` → `CellValidator` → `NetlistImporter` → a hidden
subcircuit body cached against the source's content hash and **saved into the `.jls`**.

What disappears, entirely:

- The transport. No stdin/stdout protocol, no `docs/hdl-cosimulation.md` as a normative
  wire contract, no `jls.hdl.cosim` package.
- **The transport adjudication itself** — delta-cycle-to-convergence vs. forward-only
  batch, open since 2026-07-17, deferred by three dedup passes, still the stated blocker.
  There is no conversation, so there is no contract to pick.
- Rollback, the 5 s deadline, zombie reaping, process lifetime under batch, the bounded
  stderr tail, the crashed state, the hang-detection-off-the-EDT requirement.
- P1 and P3. Parity with native elements is **by construction** — the body *is* native
  elements. Latency is by construction for the same reason.
- T1. There is no "scanner rejected the file, fall back to Yosys" boundary because Yosys
  is the only extractor; ports come from the netlist, exactly, with no subset.
- The 4-state/2-state seam. `docs/simulation-semantics.md` §2 stays the sole value domain;
  `κ: {0,1,x,z}→{0,1}` never enters the simulator.

What improves: the lab opens **and simulates** on a machine with no HDL tools at all —
strictly stronger than P5, which only promises it loads. And in a *schematic* teaching
tool the student can open the box and see the gates, which is the entire pedagogical
proposition of the product.

What it costs: synthesizable HDL only. The issue's answer is that #63 exists for "code
JLS will *never* realize." I would press that. The stated audience is *"instructors of
digital-logic courses that mix schematic capture with introductory HDL"* — and
introductory-HDL student modules are synthesizable RTL essentially by definition. #420's
counterexamples ("a vendor IP header, a testbench-only module") are not first-year
artifacts. The split between #61 and #63 is cut along a property of *the user's file*
rather than a seam in JLS, and that is why the two issues keep colliding in dedup passes.
Under Reframing A the seam vanishes: one pipeline, one failure taxonomy, one owner.

## Reframing B — flip the arrow for the genuine residue

For HDL that truly cannot be elaborated, the cheap route is the one already shipped in the
opposite direction. Export the drawn circuit to Verilog (`HdlExporter`, shipped, golden-
tested against `iverilog` in CI), generate a top-level that instantiates the export
alongside the student's module, run **the whole design once** under iverilog/GHDL, and read
the resulting VCD back into JLS's trace window. A spec-derived VCD parser already exists in
`test/jls/VcdExportGoldenTest`; the missing pieces are a testbench generator and that
reader.

Here the external simulator owns time completely. No lockstep, no deadline, no rollback
question, no per-event round trip, no second scheduler to reconcile — and JLS never has to
implement the hardest part of HDL semantics, which is not the language but *the scheduler
contract*. That is precisely what the issue's own threat "two simulators, two notions of a
delta cycle" names, and no amount of transport polish makes it go away.

Note the circularity the tracker has not noticed: **#216's title says live co-sim is "out
per #63", and #63 cites #216 as the rejection it must distinguish itself from.** Each issue
points at the other as the place the decision was made. It was not made anywhere.

## Where this pulls against the project's arc

- `docs/grand-architecture.md` §3 names #77 (headless core extraction) the single
  highest-leverage change in the tracker. Standing up a first-ever subprocess subsystem
  with its own lifecycle and IPC contract *before* that boundary exists spends the
  maintainer's scarcest resource on the branch of the roadmap with the weakest demand
  evidence.
- ARCHITECTURE.md's recorded decision "**discrete-event interpreter is the sole strategy**"
  binds any future second strategy to being *observably identical* to the event model, with
  divergence going through `docs/simulation-semantics.md` **first**. An external simulator
  stepping inside the event loop is a second execution strategy with a different value
  domain; the x/z coercion is exactly such a divergence, and the issue routes it through
  #61's rule rather than through that document. Reframing A eliminates the divergence;
  Reframing B confines it outside the loop.
- ARCHITECTURE.md declines partial i18n scaffolding until "a concrete request from an
  instructor or course." That gate is applied consistently across the recorded decisions —
  and not applied here, to a far larger commitment.
- The thread itself is the strongest evidence: 13 comments of absorb / boundary / withdraw /
  re-home bookkeeping, an absorb-then-retract pair on #420 **24 minutes apart**, and zero
  lines of #63-specific production code since PR #194 in July. Planning mass exceeding
  implementation mass is not a roster defect; it is the design refusing to converge. No
  further REPLAN will settle the transport question, because the transport is the thing
  that should not exist.

## The cheap probe, if neither reframing is taken today

The orphaned scanner is a real defect and deserves a *small* consumer, not the largest
conceivable one. Ship an **HDL port-stub element**: pick a file, scan it, draw the ports,
refuse `INOUT` by name, save path + module + hash + port list, and **execute nothing** —
undriven outputs, wired and exercised with ordinary test vectors. That is the
declare-the-interface-before-implementing-it workflow, it is a week, it consumes
`jls.hdl.scan`, it needs no tools installed, and it is the only honest way to learn whether
anyone wants HDL inside a schematic before the 6–12 unpriced weeks are spent. If no
instructor ever places one, the harness question is answered for free.

## Recommendation

Redirect. Keep the outcome; retire the mechanism. Fold the black-box element into #61's
elaborate-to-elements pipeline (Reframing A), route genuinely non-elaboratable HDL to a
whole-design export/VCD round trip built on #216's shipped bridge (Reframing B), ship the
port-stub as the scanner's immediate consumer, and close T3/T4 with their disposition
recorded. If the maintainer rejects both reframings, the one thing that must happen first
is not a roster edit: it is naming **one instructor with one lab** that Reframing A cannot
serve. Without that, this is the most expensive issue in the tracker built for a user who
has not appeared.
