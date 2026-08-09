# Issue #310: CAP-15: every drawable design leaves for the four open HDL toolchains, is checked against them, and comes back as a hierarchy
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the roster apparatus and one sentence survives: *JLS should stop being the
only authority on what a JLS circuit means.* That is a real and important claim,
and it is the right claim for this project. A pedagogy tool whose entire
correctness argument is its own goldens teaches students a semantics no one else
has ever checked. Making Icarus/GHDL/Verilator/Yosys into witnesses rather than
compilers is the single highest-leverage epistemic upgrade available to JLS.

Everything below accepts that end and disputes the route, the required set, and
where the seam is cut.

## The two assets the issue never names

The body is 48 KB long and does not contain the string **"VCD"** or the string
**"riscv"** (checked). Both are the closest existing prior art in this repo.

**1. JLS already ships the oracle's data format.** `-vcd` writes IEEE 1364-2001
§18 VCD, byte-deterministic, and `test/jls/VcdExportGoldenTest.java` (370 lines)
contains a **VCD parser written from the spec document, not from the emitter**
(its own header comment). `docs/vcd-interop.md` is already the "external tools
consume the finished outputs" doctrine, and `docs/batch-interface.md` already
makes the `-t` grammar and the watched-element set a stability contract.
Meanwhile `iverilog`, `verilator` and `ghdl` all emit VCD from `$dumpvars` with
no adapter. So AC-1's "emit a testbench, run it under Icarus, read the resulting
waveform back, compare" is not a new subsystem — it is `$dumpvars` on one side,
`-vcd` on the other, and a diff over two already-parseable files. The issue
prices FEAT-023's residual at 6-12 mw and describes it as "a testbench emitter,
a waveform reader, a signal-aligned comparator". The waveform reader is written.

**2. JLS already contains a working differential oracle.** `riscv/verify.py` and
`riscv/fuzz_diff.py` run randomized RV32I programs against an independent
reference emulator (`riscv/riscv_ref.py`) and a JLS-simulated CPU circuit, then
compare architectural state — subprocess-driven, tooling-scope, with a fuzzer.
That is the exact shape CAP-15 wants, already debugged, already living outside
`src/`. And `ARCHITECTURE.md`'s recorded decision on simulation execution
strategy *already commits* to differential oracles as the project's equivalence
mechanism: any future levelized pass "must agree bit-for-bit with the #202 RV32I
integration golden run as a differential oracle."

The project therefore has one differential oracle in Python against a reference
model, and a promised second one against a future simulation strategy. CAP-15
proposes a third, in Java, against HDL tools, and notices neither of the others.

## Reframing A — the seam is a conformance harness, not an HDL programme

Cut along `(design, stimulus) -> VCD` and make the *backend* the varying part:

| backend | how it produces the VCD |
|---|---|
| `jls` | `jls -b -t stim -vcd out.vcd design.jls` (ships today) |
| `iverilog` | export Verilog, wrap in a `$dumpvars` testbench, `vvp` |
| `verilator` | same source, `--trace` |
| `ghdl` | export VHDL, `--vcd=` |
| `yosys` | synthesize the export, re-emit, run under `iverilog` |
| *(future)* levelized sim strategy | #221's reserved second strategy |
| *(future)* `riscv_ref.py` | already exists, different serialization |

Under this framing "the four open HDL toolchains" is a **row count in a config
table**, not an outcome; AC-1, AC-2, AC-4 and AC-6 collapse into one mechanism
plus corpus rows; and the harness pays for ARCHITECTURE.md's #221 obligation for
free. Crucially it also honours §3 risk 7 by construction: the whole thing lives
in `test/` and tooling, where `ProcessBuilder` already lives (0 in `src/`, 15 in
`test/`), so nothing pushes a subprocess toward the single offline jar.

The capstone outcome then reads: *JLS has a conformance harness with N
independent backends and a corpus, and both grow.* That is a claim that keeps
paying after the issue closes. "Everything a student can draw leaves for four
named tools" is a snapshot that is either true once or forever unfinishable.

## Reframing B — FEAT-026 is not a prerequisite; it is a finding. I am disregarding the required set here.

FEAT-026 (#322, four-state value core) is **28-36 mw of a 67-101 mw band** — up
to 40% of the whole capstone — and it is not a feature. It is an amendment to
`docs/simulation-semantics.md` §2 ("two-state: 0 or 1. There is no unknown/X
state anywhere in the model") and §9 ("no conflict (X) state"), both normative,
both pinned by `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`,
and both load-bearing for the equivalence criterion ARCHITECTURE.md binds onto
any future simulation strategy. Burying that as row 8 of a ten-row roster
understates what it is.

§5 offers a false dichotomy — either fund FEAT-026, or restate step 2 as
"X/Z declared a mismatch". There is a third reading, and it is the standard one
in equivalence flows: the comparison is a **refinement check with three
verdicts**, not an equality check with two.

- tool says `0`/`1`/`z`, JLS agrees → **AGREE**
- tool says `0`/`1`/`z`, JLS says otherwise → **DIVERGE** (this is the defect
  class the capstone exists to catch; it is fully detectable in two states)
- tool says `x` → **UNCONSTRAINED**: the tool declined to commit, so no JLS value
  can contradict it. Not a pass, not a failure — a *coverage hole*.

Report `UNCONSTRAINED` as a per-run percentage. That single number is the
empirical answer to "does JLS need four states, and for which designs?" — asked
and answered for a few mw, *before* spending 28-36 mw rewriting the value core of
a shipping simulator on the strength of an assertion. The current plan has the
dependency backwards: it funds the most invasive, least reversible row first and
lets the cheap measurement that would justify it arrive last (FEAT-023 is
`blocked_by [317, 358]`, i.e. dead last, behind a CI feature outside the roster).

**Concretely: remove #322 from `requires_features`.** §2's sufficiency argument
for it ("the comparator must call every X a mismatch or silently coerce it") is
refuted by the third verdict, not weakened by it. The band drops to 39-65 mw and
the capstone stops being gated on a semantics change with its own architectural
decision record.

## Reframing C — two outcomes are wedged into one

§1 step 1 (a decomposed design exports as hierarchical Verilog/VHDL instead of
`HdlExporter`'s refusal) is FEAT-018 alone, 4-6 mw, and it is the entire
student-facing payload of the Impact section — "drawing a 4-bit adder from four
1-bit slices stops being a reason the export button fails." That is a bug fix
with a roster attached. `HdlModel.java:891` is `public final String moduleName;`
and `HdlModel.Direction` has two members; both are small, local, obviously-right
changes.

§1 step 3 (Yosys JSON write, synthesize, read back, realize 19-of-19 cells,
lay out hierarchy readably) is FEAT-019 + FEAT-020 + FEAT-022-residual, 11-20 mw,
and serves the much rarer user who has a netlist. It is also the half carrying
KC-15-1, the contested route against #304, and Open Question 3's admission that
JLS would be writing a format its own reader realizes 5 of 19 cells of.

Shipping the hierarchy *export* is not gated on the *import* round trip in any
way the issue argues for. Splitting them lets the high-value/low-risk half land
in weeks and leaves the netlist-interchange half to be judged on its own merits —
which is exactly the "netlist interchange sub-capstone" Open Question 7 already
imagines and then declines to create.

## Where this pulls against the project's arc

- **The apparatus is now the work.** Six comments: a REPLAN re-deriving a mermaid
  graph, an ADJUDICATED comment settling eight withdrawn arrows, a REPLAN
  executing ruling **D13** — *"A dependency system simply listing prerequisite
  completed issues is sufficient. I don't think that these require such fuss.
  Just make something that works."* The demo slice is priced at 4-7 mw. The graph
  bookkeeping has plausibly cost that already, and D13 is the maintainer saying
  so in his own words. The content of this issue deserves to survive; its form is
  the thing the maintainer has explicitly ruled against.
- **The evidence base is partly fictional.** Per #493, `HdlExporter.java:429-477`
  and `:465-468` — the anchors under §1 step 1, AC-5 and Background — exist only
  on a branch that will be deleted. On `master` the buckets sit at `:422`/`:436`
  and the four-entry REJECTED map with its subcircuit reason is not there at all.
  A capstone whose headline observation ("instead of the refusal that
  `HdlExporter.java:465-468` emits today") cites code that never shipped needs
  its Background section re-derived against `master` before any of it is costed.
- **Trajectory fit is otherwise excellent.** `src/jls/hdl/board/` (PCF emission),
  `docs/hdl-support-research.md`, `docs/standards-landscape.md`, the container
  image's headless HDL export, `docs/icestick-bitstream-handoff.md` — JLS is
  clearly becoming a front end to the open silicon flow. Nothing here duplicates
  that; it hardens it. My objections are all about shape and sequencing.

## What I would keep untouched

- The core inversion — *compiled against* → *checked by* — verbatim. It is the
  best idea in the nineteen-capstone set.
- **AC-1's falsification requirement.** "The test must first be shown red against
  a deliberately mis-emitted design (inverted reset polarity), and that transcript
  recorded." That is the single most valuable line in the issue and it should be a
  house rule for every oracle in the repo, including the `riscv/` one.
- KC-15-4 / AC-6's platform narrowing, and §3 risk 5's "green because it never
  ran". `IverilogCompileTest.java` gates on `Assumptions.assumeTrue`; a skipped
  suite asserting nothing is the failure mode most likely to make this capstone
  claim more than it proved.
- FEAT-001's residual (#315, 1-2 mw): a standing totality rule is cheap, correct,
  and independent of everything above.

## Suggested re-cut

1. **CAP-15a — the conformance harness** (~10-16 mw): backend table, `-t` →
   testbench emitter, VCD-to-VCD three-verdict comparator with an X-coverage
   number, falsification transcript, required (not skippable) lanes. Requires
   FEAT-004 (#336) for stable names; nothing else. Delivers the whole inversion.
2. **CAP-15b — hierarchy leaves** (~6-10 mw): FEAT-018 (#358) + FEAT-021 (#339),
   verified as a new corpus row in 15a. Delivers the student outcome.
3. **CAP-15c — netlist interchange** (FEAT-019/020/022-residual): its own issue,
   reconciled with #304 up front, judged on demand rather than carried.
4. **FEAT-026 → its own architectural decision issue**, opened only if 15a's
   X-coverage number says it is needed, and written as an amendment to
   `docs/simulation-semantics.md` §2/§9 and to ARCHITECTURE.md's #221 equivalence
   criterion — not as a roster row.
5. **FEAT-037 (#327, 13-18 mw)** re-examined the same way: honest reset export is
   ~2 mw of emitter work; the clock-domain-crossing report is a separate product.
