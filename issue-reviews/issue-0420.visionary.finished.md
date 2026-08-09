# Issue #420: TASK-0053: the shipped HDL port scanners gain a consumer — a drawable black-box element whose body runs in an external simulator under a written, forward-only contract
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Two ends are welded together in the title and they pull apart.

**End A — the scanner has no consumer.** 2,566 lines in `src/jls/hdl/scan/` whose
`package-info.java` promises port lists "for the HDL component dialog", and no
dialog. Verified on `master`: nothing outside that package names `ScannedModule`
except its own tests.

**End B — a student should be able to drop an HDL file into a schematic.**

End A is a sunk-cost argument. It cannot justify the two weeks §7.9 says "is
where the risk lives", and it is discharged by a module-picker dialog for the
#61 import flow in a few dozen lines. **When an acceptance criterion (P2:
"observe at least one package outside `jls.hdl.scan` reads the scanner") can be
met by work unrelated to the value, the criterion is measuring the wrong
thing.** Judge this on End B — and on End B the chosen mechanism is the most
expensive of three available, and the one that fights the project's arc hardest.

## The trajectory test

- `docs/hdl-support-research.md` §6 files subprocess co-simulation as **Stage 3
  (optional)** and says: *"Decide only after Stages 1–2 see classroom use."*
  Stage 2 has shipped one increment — `NetlistImporter` realizes the
  combinational core only and refuses multi-module netlists
  (`src/jls/hdl/imp/NetlistImporter.java:156-159`), non-`$` cells, bit slices and
  width mismatches. Stage 2 cannot yet import a real design, so it has not seen
  classroom use. This task jumps the project's own recorded gate.
- `docs/capability-roadmap/sweep-03-elements-and-hdl.md` opens with the
  diagnosis *"JLS's HDL interchange is not limited by its HDL code. It is
  limited by the element model."* This issue adds an element deliberately
  outside the element model: a body JLS cannot see, export, single-step, or show.
- `docs/vcd-interop.md:18-22` sells JLS to graders on determinism — byte-exact
  VCD, a stable batch contract, `ghcr.io/anadon/jls` as a headless autograder
  image. §7.2 concedes each simulator invocation is version-sensitive and that
  the failure message prints the observed version. A lab handed to 200 students
  then yields results that depend on each student's local `iverilog`, and the
  container image cannot run the circuit at all. That collides with the
  batch-interface stability promise, and the issue never names it.
- Pedagogically, a black box is the one artifact that teaches nothing — a point
  the roadmap makes twice, filing the built-in `Adder` being "a black box — the
  lesson evaporates" as a *defect* (`lf-01-parameterization.md:425`).

## Alternative 1 (primary): elaborate, don't converse — and reuse `SubCircuit`

**Reframing: an HDL component is not a new element. It is a `SubCircuit` whose
body was written in Verilog.**

Every part already ships except the glue: `jls.hdl.yosys` (2,307 lines) and
`jls.hdl.imp` (1,067) turn a synthesized netlist into real, placed, wired JLS
elements that load through the real loader; `SubCircuit`
(`src/jls/elem/SubCircuit.java`) already holds a whole `Circuit` imported from
another file, saves inline (`src/jls/Circuit.java:1478`), and has a dialog,
renderer, hit-testing, undo, copy/rotate and an editor import menu.

The design: **File → Add HDL module.** The header scanner names the modules and
their ports (its real consumer — End A discharged honestly); Yosys elaborates
the chosen one; `NetlistImporter` realizes it; the result attaches as an
imported subcircuit carrying the four provenance attributes §7.7 already
specifies — relative path, module name, SHA-256, port list — for exactly the
reasons §7.7 gives.

Deleted outright: §7.10 stage 2 in full (the trigger set `E`, the conversation
`χ`, forward-only, the deadline `Δ`); the subprocess lifecycle, reaping,
restart, broken pipe and bounded stderr tail with P7 and P8; three of the five
visible states (only *file missing* and *file changed* can occur); P9 and P12,
which become vacuous rather than tested; the `SaveTags`/`ElementRegistry`/
`HdlExporter`-bucket/sealed-permits churn; and **both execution-blocking open
questions** — Δ's default stops existing, and Open Question 4 dissolves because
an imported subcircuit needs no palette row and never engages
`PaletteContractTest` or decision D9.

Kept: the scanner gets a real consumer; the durable port list and relative-path
rule; content-hash change detection (P5); report-before-modify re-scan (P6 —
still exactly right, now a re-elaboration); round-trip-by-value (P10, guarding
O6).

Gained: native simulation speed; byte-reproducible results in the container
image; a body a student can open and read; and HDL export the moment
`SubCircuit` export works.

Not covered: non-synthesizable behavioral code — `#` delays, `initial`,
`$readmemh` models, bus-functional models. That residue is the honest case for
co-simulation, but it is a residue: of the issue's own audience examples, a
vendor IP *header* has no body for either route to run, and student Verilog
from another course is usually synthesizable.

**Sequencing:** build elaboration first. It delivers the common case at a
fraction of the cost and then serves as the differential oracle if
co-simulation is ever built — far stronger than P11's "agrees with the native
adder", because it compares *the same module* by two paths.

## Alternative 2: the export bucket is backwards, and it names the real seam

§7.4 rules the right bucket is `REJECTED` because *"a black box has no portable
HDL rendering because its body is the external file."* That confuses rendering
the *body* with rendering the *instantiation*. A black box is the one element
whose Verilog is trivial and canonical — `mymod u0(.a(n1), .b(n2));` beside the
untouched source. Rejecting it kills the flagship story: a mixed schematic/HDL
lab that reaches a bitstream via #213/#215.

What actually blocks it is that `HdlModel` has no instance statement kind —
capability **C5** in `sweep-03-elements-and-hdl.md`, priced at 3-4
maintainer-weeks and called *"the cheapest high-value item in the sweep"*
because it also unlocks EDIF, BLIF, hierarchical Verilog, SystemC, hierarchy
import, and exporting JLS's own `SubCircuit`-built designs at all. The HDL black
box should be the **forcing function for C5**, not a new tenant of the reject
bucket. Both alternatives converge on that seam.

Note also that on `master` the `REJECTED` bucket and
`exportPolicyIsTotalOverTheElementRegistry` **do not exist**:
`src/jls/hdl/HdlExporter.java:422` holds an `EXPORTED` allow-list with no
totality test. The safety net §7.4 leans on is branch-only, so on `master` a new
unbucketed element ships green and fails later in somebody's export.

## If co-simulation is built anyway: three corrections

1. **`κ` is wrong.** §7.10 stage 3 maps `{0,1,x,z} → {0,1}`. JLS's domain is two
   states **plus HiZ** (`docs/simulation-semantics.md` §2, whole-signal null), so
   `z` has a representation and must map to it; only `x` needs coercing.
   `VerilogEmitter` already promises "0/1/z only … never x" on the export side —
   collapsing `z` here loses through the black box the one value the emitter is
   careful to preserve.
2. **There is a hole in `χ` bigger than the `INOUT` refusal.** `E` contains only
   times at which an *input* changed, so a module with autonomous internal time
   — a self-generated clock, `always #10`, an internal counter — is never
   sampled and is **silently mis-simulated**. `INOUT` at least gets refused by
   name. The contract must restrict a black box to a function of its input
   history sampled at input-change times, and the harness generator must refuse
   timing constructs. (Under Alternative 1 this vanishes: Yosys refuses
   non-synthesizable timing loudly.)
3. **Open Question 1 is answerable now, in JLS's favour.** #216 rejected an
   external tool *stepping JLS* — JLS as slave to a foreign scheduler, which
   breaks the batch contract. Here JLS is master and never yields control. That
   is a different thing, and `docs/hdl-cosimulation.md` should say so in
   direction-of-control terms rather than batch-vs-live terms.

## Acceptance criteria I am explicitly disregarding

Under Alternative 1: **P7, P8, P9, P12** (no run-time toolchain exists to hang,
reap, be absent, or be excluded from `mvn verify`); **P11** as written, replaced
by a same-module two-path differential; the three simulator-dependent rows of
§7.11; the registration obligations of §7.4/§8; and Open Questions 2 and 4,
which stop existing. **P1, P3, P4, P5, P6, P10 stand unchanged** — they are the
durable part of this issue.

## Verdict

**redirect.** The element's durable attributes, the relative-path rule, the
report-before-modify re-scan and the value-asserting round-trip are right, and
the analysis behind them is the best-argued part of the issue. The mechanism is
not. Co-simulation is the project's own optional Stage 3, gated on a Stage 2
that has not landed enough to be used; it buys the least-teachable form of the
capability at the highest cost and trades away the determinism the batch and
autograding trajectory is sold on. Re-scope to elaborate-and-instantiate over
the shipped `jls.hdl.yosys` + `jls.hdl.imp` path, carried by `SubCircuit`
instead of a new element type, with C5's instance statement as the export seam.
Keep subprocess co-simulation filed against the non-synthesizable residue, to be
built later with the elaboration path as its oracle.
