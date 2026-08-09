# Issue #742: TASK-C560-2: the head-to-head table publishes with at least one workload a competitor wins, and Digital's 120 kHz claim gets a measurement rather than a counter-claim
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the acceptance criteria away and #742 is one move: **convert a reputational deficit
into an epistemic asset.** CAP-28 (#512) says it plainly — the #510 survey scored JLS 2/5
on scale/perf "for lack of receipts, not lack of speed" — and this task is the moment the
receipts become public and adversarial rather than internal and flattering. The mandated
published loss (AC-2) is the whole point; a comparison table that JLS wins everywhere
would be worth less than none.

That instinct is correct and rare, and I endorse it without qualification. Everything
below is about the *axis* and the *artifact form*, both of which the issue inherits
unexamined from the competitor it is answering.

## The axis was chosen by Digital, and it is the axis JLS does not intend to win

`docs/capability-roadmap/keystone-c-performance.md` already measured what this table will
report. On `riscv/build/k2000.jls`: **8,090 simulated cycles/s** in the warm loop, 4,600
including `initSimulation`, **1,100–1,450 end-to-end from the CLI**. Against Digital's
published 120 kHz that is ~15× behind on the warm loop and ~90× behind end-to-end. The
engine stack (#476, #879, keystone-c §8's staged plan) projects 25–40 kcycles/s — still
3–5×, i.e. sitting exactly on KC-28-1's ">5× behind" threshold rather than clearing it.

Two consequences the issue does not confront:

1. **#742 publishes *before* the engine stack lands** (#560's boundary note makes that
   explicit and correct). So the first published headline row is the 15× number, not the
   3–5× one. KC-28-1's "publish anyway" clause is written for the *post-engine* case;
   #742 invokes it for the pre-engine case. The discipline is right either way, but the
   issue is quietly committing to a harsher publication than the kill criterion describes.
2. **Nothing in the project's trajectory says JLS should win on cycles/s.**
   `docs/grand-architecture.md` §1–§3 names the assets: a single self-contained jar, two
   co-equal front ends, an enforced-headless kernel (#77 — the one discipline where JLS is
   explicitly *ahead* of Digital and Logisim, §3), a batch interface that is a stability
   contract, byte-reproducible artifacts, a differential RV32I oracle. `ARCHITECTURE.md`
   even records the decision *not* to build a compiled evaluation pass, with a revisit
   trigger phrased as "unusably slow interactively" — not "slower than Digital."

Publishing a loss on an axis you have deliberately declined to optimize is honest. Letting
the competitor pick which axis your public performance page leads with is a strategic
choice made by default.

## The table's central column is not a comparable quantity

"Cycles/s" across three simulators is only meaningful if a cycle costs the same modelled
work in each. JLS models **per-element transport delay** — gates 10 or 5, Mux 25, Register
50, Memory 100, Adder **30 × bits** (960 for a 32-bit adder) — with transport rather than
inertial semantics, so narrow pulses survive (`docs/simulation-semantics.md` §6.2, §7;
keystone-c §6.3). 388 events per simulated clock cycle on the RV32I fixture. A tool
running the same schematic under a coarser or cycle-based timing model is computing a
strictly smaller thing per cycle and will report a larger number for it.

AC-3 says Digital's 120 kHz gets "a measured row for the comparable workload, with the
measurement conditions stated." Stating conditions is necessary but not sufficient: unless
the table also states **what each tool computed per cycle**, the honest publication becomes
a misleading one in the reader's hands — and it misleads *against* JLS, which is the one
failure mode the honesty discipline cannot detect. The harness must record, per row, the
timing model and the event/step count, not just the wall clock. Concretely, add a
`what-was-modelled` column and the per-tool event counts; a row where JLS does 388 timed
events per cycle and a competitor does one levelized pass should say so on its face.

## Reframing 1 — measure the instructor's task, not the inner loop

The number a course actually feels is not cycles/s. It is *how long it takes to grade the
submissions*. JLS's own measurements make this vivid: **0.742 s of event loop, but 4.15 s
end-to-end at 3004 cycles and 5.63 s at 6004** — JVM start, circuit load, and a quadratic
`SigSim` string concatenation (`src/jls/elem/SigSim.java:64,67,71,74`) dominate by roughly
6×. The README already positions the container image for exactly this audience
("for autograders and CI, `ghcr.io/anadon/jls` runs the headless surface").

So there is a workload class that is (a) the real user task, (b) the axis on which JLS's
headless-core and batch-contract investments actually cash out, (c) something Digital's
120 kHz claim says nothing about, and (d) improvable by an afternoon's `StringBuilder`
work. **Add a task-level row: wall-clock to grade N submissions of a standard lab, from
cold start, in each tool.** Publish it beside the cycles/s row, not instead of it. That
single addition turns a page that only records a loss into a page that records a loss
*and* locates the axis the project is actually competing on — without softening anything,
which is what AC-2 forbids.

## Reframing 2 — the durable artifact is a differential oracle, not a table

A table of three tools × N workloads on one machine on one date is stale the moment any
competitor releases. #557 defends *JLS's* numbers on a schedule; nothing defends the
head-to-head, and AC-4's date-and-commit stamp is an honest admission of that rather than
a fix.

The reframing: the harness of #740 already has to drive three simulators over the same
circuit with the same stimulus. Add one line of comparison and it stops being a benchmark
and becomes a **cross-tool differential-conformance harness** — compare the three tools'
*answers* first, their timings second. That yields:

- an oracle that is durable, cheap to re-run, and gets *more* valuable as competitors
  change (the opposite of a table);
- runnable evidence for #588's grading-determinism and timing-honesty notes, which today
  rest entirely on citations to competitor trackers (Logisim-Evolution #598, #950, #1123,
  #441, #185; CircuitVerse #1412, #5328). A reproduced disagreement on a committed fixture
  is a far stronger and more respectful claim than a quoted bug report;
- a defence against the worst outcome of a pure speed table: JLS being slower *and* the
  reader having no evidence that the extra time buys anything.

`riscv/verify.py`'s differential pattern (11/11 against `riscv_ref.py`) is the template;
this is the same idea with three simulators instead of an emulator.

## Reframing 3 — make the table generated output, not prose

AC-4 asks the table to record its measurement date and harness commit "so a later re-run
is a re-publication rather than a silent edit." The stronger form of that wish is
structural: **the harness emits the table; `docs/performance.md` includes it.** Then a
re-run is a diff in a generated file, staleness is visible in `git log`, and #557's
discipline extends to the head-to-head for free. Hand-maintaining a numeric table in a
markdown document is precisely the mechanism CAP-28 exists to abolish for JLS's own
numbers; #742 should not reintroduce it for the comparison.

## The two structural obstacles the issue does not see

**Workload portability.** "The same workloads" means the same circuit in three
incompatible file formats. There is no converter, and hand-building an RV32I CPU three
times is not a 0.25–0.5 mw task — it dwarfs the band on #740 as well. The out-of-the-box
route already exists in the project's own trajectory: JLS ships structural Verilog export
(`-export out.v`) and the HDL roadmap stages Yosys-netlist import (#33, #59, #61–#63,
`docs/hdl-support-research.md`). Define the comparison corpus **in a neutral form and
generate each tool's input**, and the head-to-head becomes a consumer of the HDL bridge
rather than an orphan with a porting bill. That also makes "the same workload" a checkable
property instead of a claim.

**Logisim-Evolution probably cannot run the flagship row at all.** #588's own required
citations say Logisim's test vectors cannot drive sequential circuits (#598, #950), `-test`
silently broke between versions (#441, #185), and the project concedes its CLI verification
docs are "incomplete and possibly misleading" (#1546). The flagship workload is a clocked
CPU. #740 AC-4 anticipates this ("recorded as not-applicable with the reason") — but then
#742 AC-1's "all three tools across the harness's workloads" is unsatisfiable as written,
and the mandated competitor win (AC-2) has to come from a non-flagship row. Better to
design for that now: a small **portability matrix** (which tool can express and headlessly
drive which workload) is itself a publishable result, and arguably a more interesting one
than the timings.

## What I would keep, and what I would set aside

Keep, unchanged: the mandated published loss with no softening qualifier; the refusal to
withhold; the same-machine/same-settings fairness inherited from #740; the boundary against
#588's prose.

Set aside as written: **AC-3's framing of "answer Digital's 120 kHz."** It contains an
ambiguity that cannot be resolved by measuring harder — answering a competitor's *claim*
requires running *their* circuit under *their* configuration, while comparing *tools*
requires the same circuit in both. Doing one and calling it the other is the kind of
mistake that gets a comparison page dismissed. Split it into two explicitly labelled row
types: a **claim-replication row** (Digital's own published example, re-run on our machine,
reporting whether 120 kHz reproduces at all and under which of Digital's modes) and a
**same-workload row** (one circuit, three tools). The first is a fact about Digital's
claim; the second is a fact about the tools. Only the pair is honest.

## Verdict

Endorse the discipline, reframe the instrument. #742's premise — that credibility is built
by publishing the number that hurts — is the healthiest thing in this capstone and should
survive intact. But as scoped it publishes a 15× loss on an axis chosen by the competitor,
in a hand-written table with no defence against staleness, using a quantity that is not
comparable across timing models, over workloads that may not exist in two of the three
tools. Add the task-level (grading wall-clock) row, generate the table from the harness,
make the harness differential so the comparison outlives its date, state per row what each
tool actually modelled, and split the Digital claim into replication versus comparison.
Same honesty, aimed where the project is actually going.
