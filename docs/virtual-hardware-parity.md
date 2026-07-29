# Virtual hardware and virtual logic: parity, and a booted Linux

**Status: proposal and evidence record.** Nothing in this document is
normative. It states what was asked, what the measured evidence supports,
what would have to be built, what would have to be decided, and what is
refused. Where it proposes changing a recorded decision it says so, names
the document and the process, and does not pre-empt the outcome. Every
claim about HEAD carries a `file` / method anchor and was verified on
`claude/jls-virtual-hardware-linux-njsoma` at `36cbd37`; every number is
labeled **measured** or **estimated** and carries its method.

Two companion documents are written in parallel and are not duplicated
here: [`parity-contract.md`](parity-contract.md) (the comparison alphabet,
the exclusion set, the sync-point rules) and
[`machine-calibration.md`](machine-calibration.md) (the measured engine
constants, their harnesses, and the experiments that are still owed).
Where this document quotes a constant, that document owns its method.

A warning that applies throughout. The strongest numbers here — the engine
constants — are measured in-tree on `BatchSimulator` and are recorded in
[`capability-roadmap/keystone-c-performance.md`](capability-roadmap/keystone-c-performance.md).
The weakest are the ones the whole plan divides by: the per-cycle active
fraction, the events-per-cycle constant under an internal `Clock`, the
behavioral tier's events per instruction, and the length of a tty echo
path. None of those four has ever been measured. §10's first milestone
exists to measure them, and **nothing downstream of it is costed here**,
because every wall-clock figure in this document is a quotient with one of
them in the denominator.

---

## 1. What was asked, and the honest restatement

### 1.1 The ask

> *"parity of software running on virtual logic and virtual hardware … I
> want simulated hardware to be able to run and boot into a terminal-only
> Linux distribution that can be interacted with live."*

This is the pre-silicon bring-up problem, and it is the ordinary practice
of every large hardware vendor: boot the operating system on a fast
functional model long before the structural model can run it, and hold the
two models to a written, machine-checked equivalence contract. §3 places
JLS against that prior art.

### 1.2 The restatement

This is a **sharpening**, not a retreat. One clause of it is a plain
refusal, stated in §1.4.

> JLS gains a **fidelity boundary**: a declared boundary in a circuit may
> be bound at elaboration either to a **structural** implementation — the
> drawn contents, simulated by the one event loop, unchanged — or to a
> **behavioral** implementation of the same boundary, and the two are held
> equal by a differential harness that fails the build. A RISC-V SoC drawn
> in JLS, with its CPU bound **behavioral**, boots a terminal-only Linux to
> a login prompt in **a few minutes**, and a human types at it in a console
> pane. The **same circuit**, with the **same** CPU bound **structural** —
> the drawn machine, on the order of 580 ordinary JLS elements — boots the
> **same** image headless in **hours**, and the two runs are proved to have
> produced the same retirement trace on every interrupt-free program short
> enough to check both ways, and the same guest output byte stream on the
> boot itself.

The two tiers are **one circuit, one subcircuit binding apart**. That is
the whole architectural idea, and it is what makes parity a property of a
*boundary* rather than an assertion about two products.

### 1.3 The numbers, with their uncertainty attached

All rows are quotients of measured engine constants and **estimated**
machine parameters. The estimates dominate.

| | Behavioral tier (interaction) | Structural tier (fidelity) |
|---|---|---|
| Boot to a shell | **~2.5 min**, estimated | **~1.7 h** at central inputs, estimated |
| Honest band on the boot | 2.5–5.3 min (K1's threshold doubles it) | **1.2–6 h** (see §1.5) |
| After the semantics-preserving engine stack (2.26×) | ~1.1 min | **44–46 min** |
| After the full engine stack (2.7–4.9×) | ~0.5–0.9 min | **20–38 min** |
| Echo latency per character | ~0.09–0.47 s modeled; see below | **1.5 s** today; 0.66 s at 2.26×; 0.30–0.55 s at 2.7–4.9× |
| Live console? | **yes, in the 1970s-terminal sense** | **no — refused, see §1.4** |

Four disclosures that must travel with those figures and are the reason
this table is not a result:

1. **Every echo figure assumes a 10⁴-instruction tty echo path.** The
   evidence base carries that path as a band of 10⁴–10⁵ instructions and
   records that nobody measured it. At the 10⁵ end **every** s/char figure
   above multiplies by ten, and the structural tier's post-optimization
   console goes from laggy-but-usable (0.3–0.7 s) to unusable (3–7 s). The
   experiment that settles it is one afternoon of work and is named in
   §10 M1.
2. **The behavioral tier's cost is modeled, not measured.** Twelve events
   per retired instruction is a model, cross-checked only by an
   order-of-magnitude argument. If it measures at 25 — kill criterion K1's
   threshold — the boot is over five minutes and every latency roughly
   doubles.
3. **Every latency figure is measured on `BatchSimulator`.** A live console
   runs on `InteractiveSimulator`, which performs a `traceMap` lookup, a
   `BitSet` clone and an `O(probes)` `wireMap` walk **per event**
   (`src/jls/edit/InteractiveSimulator.java:879-896`) and calls
   `Editors.of(circuit())` inside `beforeEvent` (`:736`). Its per-event cost
   is **unmeasured and structurally higher**. Every interactive number in
   this document is therefore an **upper bound of unknown tightness**.
   Compounding (2) and (3) conservatively is how a modeled 0.1–0.5 s/char
   becomes a plausible **0.3–1.5 s/char** in practice. Nobody may quote the
   optimistic end as a fact.
4. **The behavioral tier is not exempt from engine work.** An earlier draft
   of the evidence base asserted that the optimization program accrues
   entirely to the structural tier and that the behavioral row does not
   move. That is arithmetically incoherent: the 2.5-minute figure is
   computed by dividing by the same 318 ns/event constant the optimization
   stack multiplies. Either the figure moves, or it is not derivable. It
   moves. The tier *ratio* — about 39× — is what stays fixed, and the ratio
   is the load-bearing fact.

### 1.4 What is refused, plainly

**A live console on the structural tier is not offered, and this document
does not promise one.**

The refusal is narrower and better priced than it once was, and the
narrowing matters because it changes what kind of thing the refusal is.

- The drawn machine runs at **18,800–19,500 simulated cycles/s** today
  (measured engine constant ÷ estimated events per cycle; the two arms are
  the two register-file bookkeeping conventions). A tty needs
  **10⁵–10⁶ cycles/s**.
- The **semantics-preserving** engine stack — none of which reopens a
  recorded decision — reaches ~44,000 cycles/s.
- The **full** stack, including one semantics-changing stage, is
  **2.7–4.9×**, reaching **52,600–95,400 cycles/s**.
- So the 10⁵ floor is missed by **1.05–5.1×** on the optimistic arm and
  **1.9–5.1×** against the honest 2.7× composition. The 10⁶ end is out of
  reach by 10–50× on any arrangement.

**This is a decision and a body of engineering work, not a physical
limit.** The evidence base prices it at "roughly 30–45 maintainer-weeks";
**no work breakdown supporting that figure exists anywhere**, and the two
comparable estimates that *do* carry breakdowns (17–22 weeks for a
four-state value domain, 24–35 weeks for the compiled cycle engine) do not
sum to it. Treat 30–45 weeks as an order-of-magnitude marker, not a number.

What replaces "live" on the structural tier is two things, both real:

- the fidelity boundary makes the interactive tier **the same circuit**
  rather than a different product; and
- **bounded handover windows** — toggle the CPU boundary at a declared
  instant and hand the next ten thousand cycles to the drawn logic, with
  waveforms, for about **half a second** of wall clock (10⁴ cycles ×
  ~161 events/cycle ÷ 3.14 M events/s = 0.51 s, estimated). That is gem5's
  `--restore-with-cpu`, Simics hybrid mode, and the FVP-then-RTL flow: the
  industry's answer, not a consolation prize.

**A second, quieter refusal.** "Live" on the behavioral tier means a 1970s
timesharing terminal, not a native shell. The echo is fast; the command is
not. `ls` is a fork, an exec and a readdir — on the order of 10⁶–2×10⁷
instructions — and answers in **19–76 s** on the behavioral tier today
(estimated, at 261,883 instructions/s). The number a user experiences is
command latency, and it is the headline metric.

### 1.5 The correction that governs every structural wall-clock figure

The structural boot time is not known to ±2%. It is known to about a
factor of five, and the reason is worth stating precisely because two
separate documents in the evidence base got it wrong in the same way.

Events per retired instruction decomposes as `k · α · L · CPI`, where `k`
is events per active logic element per cycle, `α` is the per-cycle active
fraction, `L` is the logic-element count and `CPI` is cycles per
instruction. The often-quoted band **468–485** is *not* an uncertainty
band on that product — it is a band over which register-file bookkeeping
convention the inherited figure used. The real uncertainty is upstream:

- `α` has a **3.1× spread** (0.18 / 0.40 / 0.56) and has **never been
  measured**, because no multi-cycle JLS machine exists to measure it on.
- `k` has **two mutually inconsistent measured values in the evidence base
  — 1.07 and 1.8 — differing by 1.68×**, and neither downstream document
  declares which one it used. Under `k = 1.8`, the shipped 468 implies
  `α = 0.155`, *below the floor of α's own band*.

Sweeping both at `L = 580`, `CPI = 2.9`, `R = 3.14×10⁶ events/s` gives
**324–1,695 events per instruction and a boot of 1.15–6.0 h**.

**Quote it as: "~1.7 h at central inputs; honest band 1.2–6 h until α and
`k` are measured."** Every derived figure — the 44–46 min row, the
20–38 min row, the CI sizing — inherits that band.

The same discipline applies to two further rows:

- **Sv32 ~4 h** is estimated, and rests on an undisclosed assumption of
  **1.4× more instructions** than the measured nommu boot plus an estimated
  **CPI 3.9**. Three significant figures beside it are unjustified.
- **Behavioral 2.5 min** is estimated from a **modeled** 12 events per
  instruction.

---

## 2. Why this is not the category error it looks like

This is the single most important framing correction in the document, and
it belongs early because without it every performance verdict in the
literature reads as a refusal.

### 2.1 JLS is word-level, not gate-level

A 32-bit `Adder`, a 32-input 32-bit `Mux`, a `Register`, a 256×32 `Memory`
and a 32×32 two-read-one-write `RegisterFile` are each **one element with
one `react()`**. Ripple-carry survives only as a lumped propagation delay
of `30 × bits` (`src/jls/elem/Adder.java`, and
[`simulation-semantics.md`](simulation-semantics.md) §7's delay table).
`RegisterFile` collapses what would otherwise be ~95 discrete elements into
one class (`src/jls/elem/RegisterFile.java:21-28`), and its measured event
cost is a function of **port count only** — 32×32, 1024×64 and 65536×32 all
cost the same.

**JLS is therefore already a mixed-abstraction simulator at the word/RTL
tier.** Every "gate-level simulation runs at 1–10 cycles/s, therefore this
is hopeless" verdict in the literature is a **category error** when applied
to JLS. Descending to real gates was measured during this study at a
**24.8–47× cost** and buys nothing at word level.

The corollary matters as much: JLS *already ships* lumped-delay behavioral
abstraction of drawn logic, and has since day one. `Adder`, `Memory`,
`TruthTable`, `StateMachine`, `ShiftRegister` and now `RegisterFile` all
compute arbitrary functions in one `react()` with no internal delays and no
mechanism asserting they agree with any structural referent. The fidelity
boundary of §4's L4 does not introduce behavioral abstraction to JLS. It
makes the abstraction JLS already performs **selectable at a boundary** and,
for the first time, **machine-checked**.

### 2.2 The speed ladder

| Tier | Speed | Source |
|---|---|---|
| QEMU TCG (dynamic binary translation) | >300 MIPS | R2VM paper, as recorded in the study's prior-art survey |
| Arm Fast Models / FVP | 20–200 MIPS; "an OS boots in tens of seconds" | Arm Fast Models User Guide |
| Simics (2002 interpreter) | 2.1–8.7 MIPS; booted four real operating systems | Magnusson et al., *Simics: A Full System Simulation Platform*, IEEE Computer 35(2), Feb 2002 |
| SystemC TLM-2.0 loosely-timed + decoupling + DMI | 67.4 M transactions/s (9.5× plain LT) | measured during this study |
| **JLS (word-level)** | **8,090 simulated CPU cycles/s warm** on the RV32I demo | measured in-tree, `capability-roadmap/keystone-c-performance.md` |
| ModelSim, comparable small SoC | ~18,300 cycles/s | practitioner report |
| Commercial software RTL simulator, large SoC | ~1,260 cycles/s | vendor figure |
| Verilator | 1.2 M cycles/s (small core) to 6.6 kHz (large core) | 180× spread — that spread *is* design size |
| Icarus vs Verilator, identical RTL | 1.49 kHz vs 42.66 kHz (29×) | cleanest same-design comparison available |
| Gate-level with SDF, 100K gates | 1–10 cycles/s | vendor white paper |
| Hardware emulation (Palladium / ZeBu / Veloce) | 1–5 MHz, costs millions | vendor |

The external rows are quoted from the study's prior-art survey — vendor
documentation, published papers and practitioner reports — and were **not
re-measured here**. Only the JLS row is measured in-tree. The ladder is
heterogeneous by construction (MIPS and cycles/s are not the same unit) and
is offered for placement, not for arithmetic.

**Placement: JLS is within roughly 2–3× of ModelSim per simulated cycle on
a comparable design, and several times faster than a commercial software
RTL simulator on a large SoC.** JLS is not a slow simulator. It is a
word-level simulator whose constant factors have never been optimized:
**47.7% of loop time is `PriorityQueue` + dedup bookkeeping, 37.6% is value
representation, and 4.9% is the actual digital logic**
(`capability-roadmap/keystone-c-performance.md`, JFR at `stackdepth=512`).

### 2.3 The one number everybody gets wrong

Any figure derived from the measured levelized cost of **4.32 ns per node**
must state **node count and pass count** in the same sentence. The measured
522 "nodes" are **225 logic elements + 297 nets — 2.32 nodes per logic
element** — and a levelized design needs **two passes per clock cycle**. A
580-element machine is therefore ~1,346 nodes, and
`2 × 1,346 × 4.32 ns = 11.63 µs/cycle = 86,014 cycles/s`, which is the
**underated ceiling**; the source itself derates twice, to
**22,000–40,000 cycles/s**. Two independent analyses during this study got
this wrong — one by 4.6× (counting logic elements as nodes, one pass), one
by presenting the ceiling as achievable.

One unreconciled caveat in the primary source: it states **two** per-node
costs for the same 522-node pass — 4.32 ns and 3.10 ns — and never
reconciles them. Every derivation here uses the conservative 4.32. At 3.10
the ceiling would be ~119,900 cycles/s. Any ns/node figure must name which
line it used.

### 2.4 Nobody has done this

No drawn/schematic logic simulator has booted any operating system — not
Logisim, Logisim-Evolution, Digital, CircuitVerse, DigitalJS or Falstad.
The class ceiling is a Logisim-Evolution RV32IM with a terminal and no OS.
Meanwhile hneemann's Digital — also Java, also event-based, also
educational — runs a complete processor at ~120 kHz, i.e. 15–120× faster
than JLS, on older hardware. **The JVM is not the ceiling; JLS's constant
factors are.** One open question that changes the argument if answered:
whether Digital's fast mode is levelized-compiled. If it is, Digital is a
working existence proof for §4's L9 in Java, and one afternoon of reading
its source settles it.

---

## 3. The prior art, and the one thing all of it does

Six traditions, all solving the same problem, all reaching the same shape.

**Pre-silicon bring-up (IBM and every large vendor).** Boot the OS on a
functional model years before RTL can run it; keep an emulator and an
accelerator for the structural runs; hold the tiers to a written
equivalence contract. The practice is old, industrial, and unglamorous.

**Simics** (Magnusson et al., IEEE Computer 35(2), 2002; now Intel
Simics). A full-system interpreter that booted four real operating systems
at 2.1–8.7 MIPS in 2002 — i.e. **an interpreter, not a compiler**, was
sufficient for OS bring-up two decades ago. Simics ships hybrid modes that
hand a running machine between abstraction levels, which is exactly the
handover of §1.4.

**Arm Fast Models / FVP.** Programmer's-view models at 20–200 MIPS, with a
documented contract that is the single best precedent for
[`parity-contract.md`](parity-contract.md): *"you must not rely on the
accuracy of cycle counts, low-level component interactions"*. Arm PV models
do not model caches at all. The flow is FVP first, RTL later, same
software.

**Hardware emulation (Palladium / ZeBu / Veloce).** 1–5 MHz for millions of
dollars. The relevant lesson is negative: **when you need three orders of
magnitude you buy different hardware; you do not optimize the simulator.**
JLS has no such option, which is precisely why §1.4's refusal stands.

**SystemC TLM-2.0** (IEEE 1666; Accellera TLM-2.0). The canonical
abstraction ladder — loosely timed, approximately timed, cycle accurate —
with temporal decoupling and DMI as the speed levers. Measured during this
study: **six SystemC abstraction levels produced a bit-identical final
memory image while reporting 60 M / 170 M / 200 M ns of simulated time — a
2.8× spread in reported time with zero functional divergence and zero
warnings.** That single experiment is the empirical justification for
`parity-contract.md`'s "all timing may differ".

**gem5 and QEMU.** gem5 *panics* rather than checkpoint classic caches and
explicitly flushes TLBs at a CPU handover, rather than pretending
microarchitectural state is portable. `--restore-with-cpu` is the handover
primitive. QEMU is the counter-example that defines the refusal in §1.4:
anyone who wants a native-feel shell wants QEMU and should use QEMU.

**RISC-V RVFI / RVVI.** The formal verification interface: one record per
retired instruction, `{order, pc_before, pc_after, insn_word, rd_index,
rd_value, mem_addr, mem_rmask, mem_wmask, mem_wdata, privilege, trap}`.
This is a solved problem with a published field list, and
[`parity-contract.md`](parity-contract.md) adopts it verbatim as the
comparison alphabet rather than inventing one. Note that `grep -ril
"rvfi\|rvvi"` over this repository returns **nothing**: JLS has no concept
of a retired instruction anywhere, and no roadmap program proposes to
introduce one.

### What every one of them does, and JLS must copy

1. **Partition abstraction; do not make one engine faster.** Not one of
   these traditions solved OS bring-up by optimizing a single simulator by
   three orders of magnitude. Every one of them ran the software on a fast
   *functional* model and the structure on a slow *structural* model, and
   invested the engineering in the **contract between them**. This is the
   entire argument for the two-tier framing, and it is why §4's keystone is
   the boundary and not the engine.
2. **Write down what may differ, negatively and by type.** Arm's sentence,
   gem5's panic and TLM's levels all say the same thing: over-constraining
   parity is as damaging as under-constraining it.
3. **Index the contract by retirement, not by time.** RVFI's `order` field
   is the whole trick.
4. **Use an oracle that does not share authorship with the design.** RISCOF
   and Sail exist because a differential harness whose reference was
   written by the author of the design proves the *generator*
   self-consistent, not either model correct. This refutes a
   single-source-of-truth reading of the parity contract, and
   [`parity-contract.md`](parity-contract.md) carries a third object for
   that reason.

---

## 4. The layer stack

In the idiom of [`grand-architecture.md`](grand-architecture.md). Each
layer names its purpose, its new mechanisms, the files it touches and what
it depends on. **The keystone is L4.** The dependency spine is at the end
of the section.

### L0 — The measurement gate (ships no product code)

**Purpose.** Every wall-clock number in this document divides by a constant
nobody measured. L0 measures them. It is days-to-weeks of work and it is
commit #1; nothing below is honestly costed until it lands.

**New mechanisms.** None. Throwaway experiments and two documents.

- **(a) α, CPI, and `k`.** Convert the shipped single-cycle demo into a
  2-cycle unified-memory machine (~10–15 elements: merged instruction/data
  `Memory`, an IR `Register`, a fetch-vs-data address `Mux`, a PC-hold
  `Mux`, a 2-state sequencer) and count events **driven by an internal
  `Clock`**. This settles the 3.1× spread in α *and* adjudicates the two
  values of `k`.
- **(b) The clocking-regime reconciliation, which is a prerequisite of
  (a).** Events per clock cycle has been measured at **121.5, 243.1 and
  245.5** on element-for-element identical circuits, and the in-tree
  keystone figure of **388.4** was measured **`SigSim`-driven** (a `-t`
  vector). A boot **cannot** be `SigSim`-driven: `SigSim.initSim` pre-posts
  every stimulus transition during elaboration, so a boot needs ~1.9×10⁸
  resident `SimEvent`s — **over 12 GB of heap before the first event
  fires**. An internal `Clock` (`src/jls/elem/Clock.java` — `initSim` posts
  exactly one event, `react` self-reposts exactly one) is therefore a
  **hard prerequisite**. The study's most-cited constant was measured in the
  one clocking regime a boot cannot use, on exactly the axis of an
  unexplained 3.2× discrepancy. Re-run the existing counters with
  per-callback attribution before anything else; it is hours of work and it
  moves every number in this document by up to 2×.
- **(c) Behavioral events per retired instruction.** A ~200-line
  accumulator machine behind a fidelity boundary, wired to a real `Memory`
  on a real bus, event-counted. The modeled 12 has never been measured
  because nothing like it exists. K1 is written against this measurement.
- **(d) Levelized ns/node at CPU scale.** Re-run the in-tree levelized
  harness at ~1,400 slots instead of 522, with the activity variants, and
  **reconcile the two per-node costs the source states for the same pass**
  (4.32 ns and 3.10 ns). K7 is written against this.
- **(e) The tty echo path.** Count instructions between a `getchar` and the
  echoed byte on the same instrumented emulator that produced the measured
  4.0×10⁷ instructions to a shell. A 10× band currently decides whether the
  behavioral console is comfortable or the structural console is hopeless.
- **(f) `InteractiveSimulator` per-event cost.** Run the existing
  6,004-cycle fixture under `InteractiveSimulator` with the same event
  counter. One afternoon. It is the engine the flagship demo runs on and
  every latency figure in this document is measured on the other one.
- **(g) The reset-fiction gating run.** `LogicElement.initInputs` zeroes
  every input at every depth and `Register.initSim` drives the configured
  init — JLS quietly supplies a reset the design does not have. Run the
  demo **once** with that zeroing disabled. If it does not boot, "the drawn
  machine boots" is an unsound claim and the parity contract would pin a
  fiction into the machine definition. Cheap check, high stakes.
- **(h) Cross-platform determinism.** Run one circuit on the three CI
  platforms that already exist as jobs and diff the VCDs. Nothing in the
  tree asserts a simulation is bit-identical across a JDK or OS change, and
  iteration order in any `HashMap`-backed path would break it silently. The
  parity contract assumes an answer nobody has. **If determinism does not
  hold, that finding outranks most of this document.**
- **(i) Adjudicate the `maxTime` event drop.** An event polled past the
  time limit is **removed from `dupCheck` before the limit is tested**, then
  discarded without reacting or re-queueing
  (`src/jls/sim/Simulator.java:224-233`). The dedup eviction happening
  *before* the limit test is the dangerous part: a resumed or state-captured
  run retains no record that the event existed. A curiosity today; silent
  corruption under any future checkpoint. Adjudicate and file **before** any
  checkpoint work.

**Files.** New [`machine-calibration.md`](machine-calibration.md)
(normative-evidence: the element census, events/cycle with its clocking
regime, `R ~ L^-0.12`, α, CPI, `k`, events/instruction, each with method) —
so `riscv/`'s evidence outlives its files; the salvaged *design* of
`riscv/riscv_ref.py`, `riscv/fuzz_diff.py` and `riscv/verify.py` written
into [`parity-contract.md`](parity-contract.md) §0 **before** deletion; a
tracked, in-`test/fixtures/` CPU-scale benchmark fixture and harness
replacing the untracked `riscv/build/k2000.jls`.

**The `riscv/` salvage is a precondition of D5's deletion, and the
inventory in circulation is wrong.** `git ls-files riscv/` returns **26
tracked files**, not three. Among them:

- `riscv/gui/cpu.jls` — **one of only four tracked `.jls` files in the
  repository**, and the fixture used for the measured second-author diff
  churn result;
- `riscv/examples/*.s` and `*.clk.txt`, which
  `test/jls/RiscvCpuGoldenTest.java:25` cites by name and `:38` cites
  `riscv/README.md` as the regeneration path — both in `{@code}` spans, not
  `@link`, so the `-Werror` doclint gate will **not** catch the rot. Silent,
  therefore worse;
- the generators `riscv/build_cpu.py`, `riscv/jlsbuild.py`,
  `riscv/make_cpu.py`, `riscv/bench_kernel.py`, and the oracle stack
  `riscv/riscv_ref.py`, `riscv/fuzz_diff.py`, `riscv/verify.py`.

`riscv/build/k2000.jls` — the performance anchor for every number in
`capability-roadmap/keystone-c-performance.md` — is **untracked**
(`riscv/.gitignore` line 1 is `build/`). It cannot be "re-homed"; it must be
**regenerated and committed, with its generation method preserved**, or
every roadmap performance number becomes unreproducible.
`ARCHITECTURE.md:354-358`'s recorded revisit trigger for #221 will also
name a deleted directory and needs restating in the same pass.

Two corrections to the inherited record while this is being done:
`test/jls/RiscvCpuGoldenTest.java` and `test/fixtures/riscv-sum1to10.jls`
are **tracked, not gitignored**, and the test is **run by every
`mvn verify`** — executed during this study: 1 test, 0 failures, 0.426 s.
The residual true part is that its regeneration path is inside the deleted
directory.

**Depends on.** Nothing. **Blocks.** Everything.

### L1 — Engine constant factors (a recorded roadmap program; this is its first consumer)

**Purpose.** Buy 2–5× on every event before spending a day on
architecture. This is the roadmap's own compiled-engine Mode T plus the
measured constant-factor stack; it is **not new work and must not be
re-specified here**.

**New mechanisms.** A time-bucketed calendar queue with an intrusive queued
flag replacing `PriorityQueue` + `HashSet` dedup, preserving the
`(time, sequence)` total order exactly; a width-carrying, immutable,
plane-encoded value replacing defensive `BitSet.clone()`; levelized
zero-delay closure, which collapses the **82.3% of events that model no
elapsed time**; and the two O(n²) defects (`SigSim.initSim` string
concatenation, `Circuit.finishLoad`).

**The stack, honestly composed.** Stage −1 (−15%), stage 0 (−15…−25%),
stage 0.5 (−10…−20%, **semantics-changing**), stage 1 (−30…−40%). Stages
−1, 0 and 1 compose to **2.26×** and are semantics-preserving. The full
stack including 0.5 is **2.7–4.9×**; composing all four midpoints gives
2.66×, and 4.9× is the top of an independently stated projection, not a
composition. **Use the range.** And label the composition for what it is:
*a conservative multiplicative composition of per-stage midpoints whose
cost pools overlap* — stage 1 deletes 82.3% of all events, which removes
82.3% of the queue cost stage −1 shrank and of the value cost stage 0
halved. It lands conservatively, so ship it; do not present it as derived.

**Acceptance gate, which is the whole point.** **Byte-identical VCD and
stdout across the entire existing golden corpus.** If any golden moves, L1
is wrong (K3). Locked in afterwards by a simulation-budget ratchet:
events-per-clock-cycle as a **hard equality** on committed fixtures, plus
ns/event within a band, so a performance regression is a build failure
rather than a discipline.

**Governance, and this is not optional.** `ARCHITECTURE.md:345-347` records,
in terms, that "no levelized/compiled evaluation pass (the
Verilator/CXXRTL elaborate-to-flat approach) is built now".
[`simulation-semantics.md`](simulation-semantics.md) §3 normatively
specifies the intra-timestamp order a levelized pass replaces ("events at
the same time fire in ascending sequence number … FIFO within a timestamp …
a pure function of circuit content"), §6.2 enumerates the zero-delay
element set the closure collapses, and intra-timestamp values are an
**observable surface today** — `BatchSimulator.probeSample` is fed
per-propagation from `WireNet.propagate`. A document that applies an
anti-reinterpretation rule to L9 cannot exempt L1 by exactly a
reinterpretation, and at bus factor 1 the maintainer cannot self-certify
against his own recorded decision inside a document he also wrote.
**Required: either (a) a recorded note on #221 and a new `ARCHITECTURE.md`
decision block stating that zero-delay closure with byte-identical goldens
sits inside the recorded option, with the argument and the K3 gate; or
(b) run L1 through §7's five-step ritual. Silence is neither.** L1's file
list must include [`simulation-semantics.md`](simulation-semantics.md).

**Files.** `src/jls/sim/Simulator.java`, `src/jls/sim/SimEvent.java`,
`src/jls/elem/WireNet.java`, `src/jls/BitSetUtils.java`,
`src/jls/Circuit.java`, `src/jls/elem/SigSim.java`, the value path across
the **25 elements that actually simulate** (27 files declare
`public void react`, but `src/jls/elem/LogicElement.java:530-537` and
`src/jls/elem/SigSim.java:214-217` throw);
[`simulation-semantics.md`](simulation-semantics.md); a new
`test/jls/sim/SimulationBudgetRatchetTest.java`.

**Depends on.** L0(i). **Note.** The in-tree projection after stage 1 alone
is **25–40 kcycles/s**, from 8,090.

### L2 — Capacity and the long-lived batch run

**Purpose.** Make a multi-billion-event, multi-megabyte-guest,
human-attended run *expressible*. Removes ceilings; makes nothing faster.

**New mechanisms.**

- **An unbounded time limit.** `JLSInfo.defaultTimeLimit = 100000000`
  (`src/jls/JLSInfo.java:69`) is **1,920–2,300× short** of a boot at 2,000
  simulated time units per clock period. `Simulator.maxTime` is a `long`, so
  the value fits. `-d 0` is a **safe** addition:
  [`batch-interface.md`](batch-interface.md) §6 freezes §2 (the `-t`
  grammar), §3 (stdout) and §4 (the VCD profile) — **not** §1, the flag
  surface — and `src/jls/JLSStart.java:1061-1074` already rejects
  `limit <= 0` while `batch-interface.md:51` documents "a positive
  integer", so no conforming consumer passes `-d 0`. It is a documented CLI
  addition plus a CHANGELOG entry. Implementation note: the loop guard is
  `now <= maxTime` (`src/jls/sim/Simulator.java:217`), so "unbounded" must
  map to `Long.MAX_VALUE`, not to 0.
- **A memory capacity that is not a cliff.** `Memory`'s
  `DENSE_CAPACITY_LIMIT = 1 << 22` (`src/jls/elem/Memory.java:1224`) is
  **exactly 16 MiB at 32-bit words, with zero headroom** against Linux's
  12–16 MiB. The gate is on **word count, not bytes**, which is why
  "replace it with a byte budget" is the correctly-shaped fix.
- **Copy-on-write init.** `Memory.initSim` does `mem = initMem.copy()`
  (`src/jls/elem/Memory.java:1309`), doubling heap.
- **A raw-binary memory image section.** At the measured 15.87 bytes/word a
  16 MiB RAM image is **66.6 MB of text — 99.2% of the 64 MiB cap**
  (`MAX_CIRCUIT_TEXT_BYTES = 64L << 20`, `src/jls/FileAbstractor.java:65`),
  leaving about half a megabyte for the entire circuit. It does **not**
  exceed the cap; it consumes it. (An 8 MiB kernel image is a 33 MB `.jls`;
  a 2.4 MiB kernel is 10 MB.) Per D3 this is an **optional, independently
  versioned section with must-understand semantics**, not a private
  sidecar: an old reader skips it and opens the circuit structurally with a
  clean diagnostic.
- **A long-lived batch mode.** `BatchSimulator.pause()` is literally
  identical to `stop()` today (`src/jls/sim/BatchSimulator.java:75-78`,
  `:87-90`, whose javadoc reads `@param which Ignored.`).
- **Progress reporting and clean SIGINT.** **Correction:** a progress
  heartbeat may **not** go to stdout.
  [`batch-interface.md`](batch-interface.md) §3 states that "after the run,
  batch mode prints exactly two things to stdout". Send it to stderr, or
  gate it behind a new flag — §6's only sanctioned additive case is "a new
  flag, a new optional output gated behind a new flag".

**Files.** `src/jls/JLSInfo.java`, `src/jls/sim/Simulator.java`,
`src/jls/sim/BatchSimulator.java`, `src/jls/elem/Memory.java`,
`src/jls/JLSStart.java`, [`file-format.md`](file-format.md),
[`batch-interface.md`](batch-interface.md) (+CHANGELOG per its own §6).

**Depends on.** L1 (order only, not correctness).

### L3 — The host boundary: one door, granted at invocation

**Purpose.** Give a running circuit — *either* tier — a byte exchange with
a human or a script, without a transport, without a foreign thread posting
events, and without destroying determinism. Verified: **zero `System.in`
and zero `ProcessBuilder`/`Runtime.exec` in all of `src/`**. This is
genuinely the first host door.

**New mechanisms.**

- **`jls.elem.Console` — the only new element in this architecture.** The
  measured minimum 16550: three byte addresses (THR write, RBR read, LSR
  read returning `0x60 | data_ready`), polled, `irq = 0`, no PLIC. An
  in-tree `LogicElement` subclass on the pattern #201 established; the
  measured registration tax is **~65–70 lines across 14 files** and **zero
  format version**. Note the file count: `970db41` added
  `src/jls/elem/SaveTags.java` and [`file-format.md`](file-format.md) to
  that set and made skipping them a build failure, and
  `ARCHITECTURE.md:115-146`'s honest sixteen-place ritual (whose opening
  sentence "there is no element registry yet" is itself stale at HEAD) is
  the checklist to work from.
- **`jls.io.HostBytePort` — a sealed contract**, permitting `NullPort`,
  `StdioPort`, `FilePort`, `PipePort` (the in-memory test double) and
  `PanelPort`. Per **D7** this is a **sealed in-tree collaborator, not a
  plugin seam**, and §7 explains why the catalog must say so rather than
  advertise a seam nothing can contribute to.
- **No cross-thread `post()`, and the drain point is decided.**
  `Simulator.post` is unsynchronized over a plain `PriorityQueue` with a
  single-thread contract (`src/jls/sim/Simulator.java:25,27,165-169`), and
  every `post()` call site outside `jls.sim` is inside `jls.elem`. The host
  thread offers bytes to a lock-free ring; the simulation thread drains it
  **at `Simulator.beforeEvent`** (`:246-255`), the only thread-correct slot,
  and the same slot an in-tree causal-debug design independently identified
  as the only legal *capture* point. **This resolves a live contradiction in
  the evidence base**, which specified draining inside `Console.react` in
  one document and at `beforeEvent` in another. They are not equivalent for
  determinism: draining inside `react` makes an element's output depend on
  host arrival timing *within* an event; draining at `beforeEvent` confines
  it to a declared loop boundary. **Take `beforeEvent`.**
- **The RX poll self-schedules.** The idiom is legitimate on the strength of
  §6.2's delay discipline and `Clock.react`'s implementation. **It is not
  blessed by [`simulation-semantics.md`](simulation-semantics.md) §8.3**,
  which documents `Clock`'s `cycle`/`one` parameters and first-rising-edge
  timing and nothing more. Do not cite §8.3 as authorizing it.
- **Record and replay.** A transcript of `(stamp, byte)` where the stamp is
  **retirement index or simulated time, never wall clock**. This extends
  #181's determinism invariant from "every simulated value is a pure
  function of circuit content" to "…of circuit content **and the
  transcript**". That sentence is normative and lives at
  [`simulation-semantics.md`](simulation-semantics.md) §3, mirrored in code
  at `src/jls/sim/Simulator.java:195` and `src/jls/Circuit.java:471` — so
  that document belongs in L3's file list, and this layer proposes to change
  a normative determinism invariant.
- **A session-boundary rule, enforced by a test.** No `java.io`, no host
  handle, no extension lookup on any path reachable from `Reacts.react()`.
  A `SessionBoundaryRatchetTest` alongside the seven ratchets already in
  `test/jls/`. This turns `grand-architecture.md` §6's hot/cold prose into a
  build failure.
- **Grant, never ambient.** Host I/O requires an explicit invocation flag or
  a GUI action, and the grant is named on the run's outcome line — the
  `-serial stdio` model. A loaded `.jls` never acquires host I/O.

**Files.** New `src/jls/elem/Console.java` and the fourteen-file ritual;
new AWT-free `src/jls/io/*`; `src/jls/sim/Simulator.java` (a `hostPort()`
accessor, ~10 lines); `src/jls/sim/BatchSimulator.java`;
`src/jls/JLSStart.java`; [`extension-points.md`](extension-points.md);
[`simulation-semantics.md`](simulation-semantics.md);
[`reproducibility.md`](reproducibility.md).

**Depends on.** L2. **Independently valuable:** a drawn FSM can print. It
is the most conspicuous element JLS lacks against Logisim-Evolution and
Digital, which both ship TTY components.

**Note on visibility.** The module runtime **shipped** (#220/#223/#224) and
boots in every run mode — `src/jls/JLS.java:60` calls `JlsModules.boot()`
before `JLSStart.start()`. But it is **wired and unconsumed** by its own
javadoc: *"the registry is populated but nothing reads it for dispatch
yet"* (`src/jls/boot/JlsModules.java:31-34`). `CoreModule.register` copies
all 35 `ElementRegistry` rows in; `Circuit.load` still uses the static
table. **A device element contributed through the seam today would boot
correctly and be invisible.** L3's file list is therefore the *static-table*
ritual, which is the correct path today, and the #220/#224 integration
slice is a hard prerequisite for anything that dispatches through a device
seam.

### L4 — The fidelity boundary — **the keystone**

**Purpose.** Make parity a property of a **boundary**, which is what makes
it testable. This is the maintainer's own adopted framing (D4), and it is
the one item of D4's four that no roadmap program owns.

**The mechanism.** A per-instance saved attribute naming one of a closed,
core-internal, sealed set of implementations of that instance's boundary.
Structural is the default and is what happens today. A behavioral binding
is **one element with one `react()` and one lumped propagation delay at its
pins** — structurally indistinguishable from `Adder`, `Memory`,
`TruthTable`, `StateMachine` or `RegisterFile`.

**Why a boundary rather than a new `Cpu` element.**

1. **The structural referent is in the same file.** A behavioral-first
   program's legitimacy condition — realizable *and* realized *and* checked
   — is discharged by construction rather than by a promissory note, because
   the binding is meaningless without a definition to bind to.
2. **No new sealed permit, no palette entry, no icon, no help page, no
   `-Werror` switch-exhaustiveness ripple.** `SubCircuit` already exists in
   `LogicElement`'s permits.
3. **Handover is free.** Toggling at a declared instant maps only *one*
   boundary's architectural state; `Memory`, `Console` and the bus are the
   same objects in the same run. No general simulation-state serialization
   is required.
4. **It is provable at student scale on day one** — an ALU subcircuit,
   drawn versus compiled — with zero RISC-V and zero Linux.

**An unresolved contradiction the maintainer must settle before M3.** The
evidence base contains two incompatible readings of this keystone and
neither document notices the other:

| | Reading A (fidelity boundary) | Reading B (registered element) |
|---|---|---|
| Mechanism | a saved attribute on a subcircuit instance | a registered `ElementType`, e.g. `jls.elem.Rv32Core` |
| New permits entries | 0 | 1, plus the ~65-line, 14-file tax |
| Format cost | +1 attribute (but see below) | **zero** |
| Harness compares | two implementations of one definition | two devices under test |
| Argument | the structural referent is in the same file | *a behavioral RV32 core is not derivable from any drawing* — it is a hand-written model of one particular machine, so there is nothing for reading A to bind to at the CPU boundary |

**Reading B's objection is correct at the CPU boundary and wrong below
it.** Reading A holds cleanly for ALU, register file, decode, load/store
and CSR sub-boundaries, where a drawing exists and the behavioral model is
a contraction of it. At the CPU boundary there is no drawing to contract
until L8 is finished, so a behavioral CPU is a device under test, not an
implementation of a definition. The likely resolution is **both**: the
attribute for derivable boundaries, a registered element for the CPU. That
is a decision, not a deduction, and **M3 is the merge that would encode it**.

**The format cost is not zero, and this is a recorded-policy question.** The
proposal that a fidelity attribute costs no format version rests on the true
premise that unknown attribute *names* are silently ignored — and inverts
the recorded policy. [`file-format.md`](file-format.md) states verbatim:
*"Writers SHOULD therefore prefer a version bump over an 'ignorable'
attribute whenever dropping the attribute would change simulation
behavior."* A fidelity attribute is the definitive member of that class;
changing simulation behavior is its entire purpose. The same section records
that FORMAT 2 was bumped for exactly this shape, and names `Memory`'s
`sync` flag as a live open instance. Worse, L7's drawn SoC ships a CPU
subcircuit whose **definition is initially a stub** with a behavioral
binding: an older reader silently drops the binding, runs the stub, and
emits a confidently wrong result with no diagnostic. **Required: FORMAT 3
for files carrying a non-default binding, or route the attribute through
D3's must-understand mechanism.** Applying D3 to the memory-image *section*
and not to the *attribute that changes simulation behavior* is an
inconsistent application of the same binding decision to two cases in one
design.

**The observation function** (this is the contract, and it must be written
before any binding exists): across a fidelity boundary the observable is
the **settled output word per sampling instant, indexed and not
timestamped**. Sampling instants are quiescence points or edges of a
declared sync net. What is quotiented out is **combinational transport
delay strictly inside the boundary and nothing else** — sequential elements
keep their own delays and their §8 edge semantics, the value domain is
unchanged, and tri-state resolution at the boundary still happens in
`WireNet.propagate`, outside the toggled element.

**Enforcement, not policy.**

- A boundary harness running the same stimulus through both bindings and
  comparing the observation function: **exhaustive under 16 input bits; a
  seeded 10⁶-vector sample plus declared corner vectors (width edges
  1/31/32/33/63/64/65, HiZ, undriven) above.**
- **A deliberately-failing null test.** A knowingly-wrong binding the
  harness *must* reject. An unfalsifiable parity harness is worse than none
  — see K4.
- A reflective or bytecode guard that no binding touches the event queue.
- **Declared refusals, by name**: a `DelayGate` used as a delay line; a
  `TriState` whose behavior depends on turn-off relative to turn-on; a
  level-sensitive `Memory` write (§8.4's glitch hazard is a *timing*
  phenomenon and quietly deleting it would teach that #199's bug does not
  exist); more than one incommensurable `Clock`; a block that does not
  settle. **Refusal, never silent degradation.**
- New `docs/abstraction-levels.md`, normative: the legitimacy test for
  behavioral abstraction, written as the **retroactive articulation of a
  rule the repo already follows**, applied element-by-element to `Adder`,
  `Memory`, `TruthTable`, `StateMachine`, `ShiftRegister`, `RegisterFile`
  and `FieldExtend` **first**, and only then to a CPU binding.
- An **abstraction banner** whenever a non-structural binding is active —
  **flag-gated**. As originally proposed it lands in two frozen
  `batch-interface.md` sections (§3's "exactly two things to stdout" and
  §4.2's byte-deterministic VCD header, which deliberately omits `$date`
  and `$version` for that very reason) and is gated on **circuit content**
  rather than on a flag. §6 permits "a new optional output gated behind a
  new flag"; it does not permit this. Flag-gate it, or take §6's
  major-bump/compatibility-flag path.

**A parity gap this layer does not close, and must not be assumed to.**
`Console` is an **element**, not a subcircuit, so the per-instance boundary
**structurally cannot** hold a drawn UART equal to a `Uart16550Model`, nor a
drawn CLINT subcircuit equal to a `ClintModel`. That is two UART
implementations and two CLINT implementations with no harness relating any
pair — and the console byte stream is precisely the observable
[`parity-contract.md`](parity-contract.md) designates as the Linux-scale
parity clock. Either the device models get their own comparison mechanism
or the contract's Linux-scale oracle is unguarded. Name it; do not let it
pass silently.

**Files.** `src/jls/elem/SubCircuit.java` (+1 saved attribute, +1 dispatch —
note that copy semantics live at `SubCircuit.copy()`, `:332-384`, not at the
range older documents cite); [`file-format.md`](file-format.md); new
`src/jls/sim/equiv/*`; new `docs/abstraction-levels.md`;
[`extension-points.md`](extension-points.md).

**Depends on.** L1. **Blocks.** L5–L9.

### L5 — `jls.mach`: the machine model, in a pure leaf package

**Purpose.** Hold every line of architectural logic where it can carry the
full bar, and make the reference runner the parity harness needs fall out
for free.

**Forced by a verified constraint.** There is no `module-info.java` anywhere
in `src/`, so a permitted subclass of a sealed type must sit in the same
package as its parent — and `pom.xml` floors `jls.elem` at
**0.730/0.700/0.585** against `jls.sim`'s **0.930/0.920/0.845**. Putting
~3,000 lines of ISA logic behind an element would hide it behind the weaker
bar.

**New mechanisms.** `ArchState` (record-shaped, `int`/`long`, no `BitSet`);
a `MemoryView` with two indistinguishable implementations — an array-backed
one for the reference runner and a bus-backed one for the fidelity binding,
which is what makes the wired core and the reference runner *provably the
same code*; a data-only decode table; a pure `step()` function;
`mach.dev.Uart16550Model`; `mach.dev.ClintModel` with `mtime` driven by
**simulated** time. Zero AWT, zero `jls.sim`, zero `jls.elem`,
unit-testable with no simulator and no circuit. Added to
`HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` **in the same commit that
creates it** — that list is the only thing that polices a package, and it
takes **path** prefixes.

**Files.** New `src/jls/mach/**`, `test/jls/mach/**`; `pom.xml`;
`test/jls/HeadlessCoreRatchetTest.java`;
`test/jls/NullMarkedRatchetTest.java` and
`test/jls/PackageInfoRatchetTest.java` (see the governance band).

**Depends on.** L4, L0(c).

### L6 — The parity contract and its harness

**Purpose.** Make "the two tiers agree" a build failure rather than a
claim, and ship the machinery as a **student feature before any Linux
work**.

**New mechanisms.** A `RetireRecord` carrying the RVFI field list verbatim,
as a **Java record with no field for cycles, simulated time, pipeline state
or cache state**, so the permitted-to-differ set is enforced **negatively by
the type** and over-constraining parity becomes a compile error. Trace
capture folded into the **existing, already-overridden**
`BatchSimulator.probeSample` and `afterEvent` hooks — a retirement-trace
recorder is a **third `Simulator` subclass** needing **zero changes to
`jls.sim`**, the identical mechanism `BatchSimulator` already ships
(`src/jls/sim/Simulator.java:269-270`, `:285-287`). A differ with
first-divergence reporting, both records side by side and the differing
fields named. `--rvfi FILE` and `--diff-against`.
[`parity-contract.md`](parity-contract.md), normative, plus a ratcheted
exclusion set.

**The third object.** The harness needs a reference that does **not** share
authorship with the design, or it proves the generator self-consistent
rather than either model correct. The in-tree standards work is blunt about
this and prescribes Sail. That work homes its whole RISCOF plugin tree at
`riscv/riscof/**` — a directory D5 deletes, and which does not exist yet.
**It needs a new home and a cost band.** The cheap resolution the corpus
never applies to its own case: commit the compiled test ELFs and reference
signatures as fixtures for the blocking lane, and keep the
toolchain-bearing RISCOF run informational-nightly — which is that
document's own two-lane split.

**The shipped student feature, before Linux:** *"JLS names the exact
instruction where your drawn CPU first disagreed with the reference, and
prints both records."* That is a better datapath assignment than anything
in this software class, it falls straight out of the trace machinery, and
it justifies the program on its own.

**Files.** New `src/jls/parity/*`; `src/jls/sim/BatchSimulator.java`;
`src/jls/JLSStart.java`; [`batch-interface.md`](batch-interface.md) (new
flags, CHANGELOG per §6); new [`parity-contract.md`](parity-contract.md).

**Depends on.** L5, L4, L3.

### L7 — Virtual hardware and the front ends

**Purpose.** The moment the maintainer's sentence becomes true.

**New mechanisms.** A drawn SoC of ~10 top-level boxes: a CPU subcircuit
(definition initially a stub, binding behavioral), `Memory`, `Console`, a
CLINT subcircuit, address decode, `Clock`, watched pins. A student opens it
and sees a computer. A GUI console pane bound to a `Console` element's
panel port through `InteractiveSimulator`'s existing Runner-thread/EDT
seam: per-keystroke `KeyListener` on the EDT into the ring, output batched
at ~30 Hz via `invokeLater`, **never per signal** (`grand-architecture.md`
§6's own rule). **Two terminal modes**: *block mode* is the default — type
the line locally at keyboard speed with local echo, submit on Enter, ^C/^D
as immediate sideband keys, pay one command latency per command; *character
mode* is honest pass-through for shell line editing. Block mode is
**presentation only** — the guest sees the same bytes at the same simulated
times, the transcript is identical, parity is unaffected. Headless
`-console stdio` is line-buffered by default and says so. The free
multipliers are taken: declare a slow clock and pin `lpj=` on the kernel
command line, which together are worth more than any engine optimization
because `calibrate_delay`/`udelay` burn cycles in proportion to the
*declared* frequency and compute nothing.

**One governance note nobody has stated.** This layer — the flagship
user-facing deliverable, the thing that makes the sentence true — lands in
`src/jls/edit/`, **the one package with no coverage floor and no headless
test path** (`pom.xml`; `CONTRIBUTING.md` records that `jls.edit` is
deliberately unfloored until #84/#91 make it testable headlessly). That is
not a violation, but it means the headline result is delivered by the
least-tested code in the plan, in a project whose constraints otherwise
demand test-enforced checks. Say so; do not discover it later.

**Files.** `src/jls/edit/` (the console panel — the only AWT in this
architecture); `src/jls/JLSStart.java`; a `machines/` directory; new
`docs/virtual-hardware.md`.

**Depends on.** L5, L6, L3, and the guest-image program (§6, P21).

### L8 — Virtual logic: the drawn machine, brought up boundary by boundary

**Purpose.** The legitimacy witness, the differential counterparty, and the
genuine first.

**Construction.** Build no bespoke forge. The drawn machine is authored in
the editor where structure is pedagogically load-bearing (datapath,
control, CSR block) and generated through the roadmap's recorded
programmatic API — **every verb of which is a shipped
`jls.collab.op.CircuitOp`** — where it is mechanical repetition (bus
fan-out, decode tables, the CSR file). That contributes to a recorded
program instead of forking one, gets validation, atomic rejection, exact
inverses and undo for free, and **cannot construct a circuit the editor
could not**.

**Correction to a claim in circulation.** "There is no programmatic
construction API" is overstated. A working in-process path exists **today**:
`new AndGate(circuit)` → `Circuit.addElement` (`src/jls/Circuit.java:342`)
→ `Util.partition` (`src/jls/Util.java:145`), with no text round-trip.
Element constructors are public because the registry's factories are method
references. The honest claim is: **the primitives are public but
undocumented, untested as an API, and bypass the `CircuitOp` mutation layer
that collaboration and undo observe; there is no `src/jls/api`.** That
shrinks this layer from "build a construction API" to "wrap and ratify
one".

**Bring-up is boundary by boundary, and this is the whole method.** Each of
ALU, register file, decode, load/store, CSR and CLINT is a subcircuit with
a fidelity boundary and a behavioral binding from `jls.mach`. Each is
checked by L4's harness at its own boundary the day it is drawn. Then the
CPU boundary itself. **At every commit, whatever is drawn is checked.** This
removes the failure mode every reviewer bet on: a year of work ending at
the kernel decompressor with nothing verified.

**A hard blocker this layer inherits and does not own.** `lb/lh/lbu/lhu/sb/sh`
are unimplemented **because 32-bit `Memory` has no byte lanes**
(`capability-roadmap/README.md:88-90`), and the minimum SoC's 16550 is
**three byte addresses on a 32-bit bus**. Without sub-word access there is
no UART driver and no Linux. This is owned by the roadmap's element-vocabulary
program at 3–7 weeks and it is **absent from the layer stack entirely**
unless stated here. It gates L7 and L8, not L3.

**A second inherited defect.** `HdlExporter.EXPORTED`
(`src/jls/hdl/HdlExporter.java:422-428`) lists exactly **22 classes and
omits both `RegisterFile` and `FieldExtend`** — so any circuit using them
fails export outright. The two elements that make a Linux-capable datapath
cheap to author are exactly the two that make it un-exportable to Verilog.
**No HDL-round-trip or formal-path claim may be made about the drawn
machine** until both are added to the exporter, and adding them should be
priced as in-scope work.

**Files.** `machines/cpu-rv32/**`; `src/jls/api/**`; a nightly CI workflow
with explicit `timeout-minutes`.

**Depends on.** L4, L6, L0(a), byte lanes.

### L9 — Mode C, the cycle engine (gated; the only layer that touches #221)

**Purpose.** Additional speed for the structural tier, deliberately last
and deliberately optional.

**Restate the payoff honestly.** Against today's interpreter (19,473
cycles/s), a derated Mode C at 22,000–40,000 cycles/s is **1.1–2.1×**, and
at its underated ceiling of 86,000 it is 4.4×. Against the 2.26×
interpreter (44,010 cycles/s) it is **0.5–2.0× derated**. A claim of
"3–20×" is unsupported at either basis. **L9's honest proposition is
0.5–2× over an optimized interpreter, for the most expensive governance
token in the repository.**

**The mechanism behind that.** Mode C settles **every node every cycle** —
α ≡ 1 by construction. The event interpreter touches only active elements,
and a multi-cycle CPU has α somewhere in 0.18–0.56. **Mode C wins on dense,
high-activity designs and loses on sparse multi-cycle ones, and a
multi-cycle CPU is the sparse case.** An activity bitmap is the only thing
that would change this and nobody has specified how Mode C would use one.
**Therefore α is not merely the dominant boot-time uncertainty; it is the
decision variable for a 24–35-week engine program**, and measuring it in L0
must precede committing to L9.

Opt-in per run, never the default, behind a static classifier that
**refuses** circuits where the weakening is observable and falls back with a
one-line notice rather than an error — so a first-year student drawing an
adder never sees it. It requires a **named second conformance level** in
[`simulation-semantics.md`](simulation-semantics.md) (which has no
conformance-level concept today — this introduces a new normative
structure) and an argued reopening of #221 (§7).

**Depends on.** L1 exhausted and measured, L0(b) and L0(d), L6 as its
equivalence oracle.

### Cross-cutting — the governance band (ships no runtime code)

**This band is where the schedule is most likely to be wrong, because the
bar the plan volunteers for is stricter than the bar the repo enforces.**

**What the repo actually enforces at HEAD.** JaCoCo floors are
`BUNDLE 0.545/0.535/0.505`; `jls 0.515/0.500/0.555`;
`jls.sim 0.930/0.920/0.845`; `jls.elem 0.730/0.700/0.585`;
`jls.collab.op 0.905/0.895/0.750`; `jls.edit` deliberately unfloored.
**There is no default rule for a new package** — `jls.mach`, `jls.parity`,
`jls.io`, `jls.sim.equiv` and `jls.api` inherit only the bundle floor until
someone writes a rule whose `<includes>` matches them. **93.0/92.0/84.5 is
`jls.sim`'s floor, not a repository-wide requirement on new code.** If the
program adopts it for its new packages, that is a **self-imposed bar** and
must be labeled one — because K5, the criterion that would abandon the
Linux target, is written against it.

**PIT is not what the plan assumes either.** There is exactly **one** global
`mutationThreshold` / `testStrengthThreshold` pair (80/82) over one
`targetClasses` scope (`jls.sim.*`, `jls.BitSetUtils`, `jls.Util`,
`jls.SpatialIndex`, `jls.collab.op.*`), it lives in an opt-in profile, and
`CONTRIBUTING.md` records that the weekly run "stays
schedule/`workflow_dispatch`-only and is **never a required PR check**".
"PIT 80/82 per new package" is not expressible, and adding packages to the
scope moves the **aggregate** and can breach the existing floor. One
unpriced consequence: **`jls.sim.equiv` is already in scope** — the glob
`jls.sim.*` matches sub-packages — so it lands under 80/82 automatically
the day it exists.

**Floors cannot be pre-committed.** The recorded convention in
`CONTRIBUTING.md` is measure-then-floor-just-below: floors only ever move
up; leave 0.5–1.0 points of headroom (the #233 incident: a floor set from
JDK 25 measured lower on JDK 26 and failed an unrelated PR); raise from the
canonical JDK only; raise from headless numbers only; use `mvn clean
verify` when touching floors; and **a floor that has never been seen to
fail should be assumed vacuous — include patterns must be dot-form package
names, because the slash form silently matches nothing.** Restate the plan
as: *land the package, measure it headless on JDK 25, floor it at or below
with headroom, in the same PR, and verify non-vacuity.*

**Four gate families the layer stack omits and every new package must
clear.**

1. **`@NullMarked` at birth.** NullAway plus JSpecify, enforced inside
   packages whose `package-info.java` declares it; the ratchet list only
   grows and new packages are born marked. Five or six new packages, each
   needing a `package-info.java` and rows in
   `test/jls/NullMarkedRatchetTest.java` and
   `test/jls/PackageInfoRatchetTest.java`.
2. **Sealed dispatch discipline** (#95): exhaustive `switch` with **no
   `default` arm** over a sealed type, sealed leaves stay `final`, and the
   permits tree is pinned by `test/jls/elem/SealedHierarchyTest.java`.
   `HostBytePort`'s five permits inherit this, and adding `Console` to
   `LogicElement`'s permits (24 today) edits a test-pinned tree.
3. **Javadoc `-Werror` down to private members** (#192), SpotBugs at
   threshold High with no blanket excludes, CodeQL on every PR. On a
   ~3,000-line ISA model with a decode table and CSR/trap logic the javadoc
   gate alone is a material fraction of the writing cost and is absent from
   L5's estimate.
4. **The extension-point catalog cross-check.**
   `ExtensionPointCatalogTest` collects constants from a **hardcoded list of
   holder classes**; a constant in a fifth home package is invisible to it
   and its documentation row becomes a phantom-row build failure unless that
   list is edited in the same commit.

**New ratchets this program adds.** `SimulationBudgetRatchetTest`;
`SessionBoundaryRatchetTest`; the exclusion-set ratchet; a determinism
ratchet asserting **no golden or VCD fixture may be produced in live console
mode**; and — see K9 — **a pedagogy ratchet, which does not exist and which
no layer currently builds.**

### The dependency spine

```
L0 measurement gate ─┬─> L1 engine constants ─┬─> L2 capacity ──> L3 host boundary ─┐
                     │                        │                                     │
                     └────────────────────────┴──> L4 FIDELITY BOUNDARY <───────────┘
                                                        │
                                        ┌───────────────┼────────────────┐
                                        v               v                v
                                    L5 jls.mach ──> L6 parity ──> L7 virtual hardware
                                                        │              ^
                                                        │              │ P21 guest image
                                                        └──────> L8 virtual logic
                                                                 ^    │  ^
                                       P2 byte lanes ────────────┘    │  └── jls.api
                                                                      v
                                                              L9 Mode C (gated on α)
```

---

## 5. The gap list, ranked

Every gap verified at HEAD. "Program" names the
[`capability-roadmap/`](capability-roadmap/) program that owns it where one
does; **P14–P21 are proposed in §6** and do not exist yet.

| # | Gap | Severity | Layer | Program |
|---|---|---|---|---|
| 1 | **No host I/O, no device concept.** Zero `System.in` and zero `ProcessBuilder`/`Runtime.exec` in all of `src/` | fatal | **L3** | **P14** (new) |
| 2 | **Parity has no observation point.** No boundary concept, no fidelity toggle, no equivalence harness. `grep -ril "rvfi\|rvvi"` over the tree returns nothing | fatal | **L4**, **L6** | **P16**, **P17** (new) |
| 3 | **No machine model and no in-tree reference runner.** The only RV32I reference emulator in the tree is Python, inside the directory D5 deletes | fatal | **L5** | **P20** (new) |
| 4 | **No guest software stack.** No kernel pin, no `.config`, no busybox, no initramfs, no device tree blob, no memory map, no reset stub, no cross toolchain, no rebuild recipe. Three commitments dereference it and none owns it | fatal | **L7** | **P21** (new) |
| 5 | **`Memory` has no byte lanes**, so `lb/lh/lbu/lhu/sb/sh` do not exist and a 3-byte-address UART is not addressable. A hard Linux blocker | fatal | **L8** | **P2** (3–7 wk) |
| 6 | **`SigSim` cannot express or hold a boot.** `initSim` pre-posts every transition: ~1.9×10⁸ resident events, >12 GB of heap before the first event fires. The `-t` vector for a boot is also ~11 GB of text | fatal | **L0(b)**, **L2** | **P1-S0**, **P12** |
| 7 | **47.7% of loop time is queue/dedup, 37.6% is value representation, 4.9% is logic; 82.3% of events model no elapsed time** | major, no decision needed | **L1** | **P1-S1**, **P8 Mode T** |
| 8 | **Time limit is 1,920–2,300× short; `pause()` is `stop()`; batch is one-shot** | major | **L2** | **P18** (new) |
| 9 | **`DENSE_CAPACITY_LIMIT` is exactly 16 MiB with zero headroom; `initSim` doubles heap** | major | **L2** | **P2** |
| 10 | **Kernel image unshippable.** A 16 MiB image is 66.6 MB of text — 99.2% of the 64 MiB cap, leaving ~0.5 MB for the circuit | major | **L2** | **P15** (new) |
| 11 | **No simulation-state serialization at all.** `Memory` and `RegisterFile` save init text with no write-back path | major | **not closed** (§8) | **P9** + **P15** |
| 12 | **No supported construction API.** The primitives are public but undocumented, untested as an API, and bypass `CircuitOp`; there is no `src/jls/api` | major | **L8** | **P12** |
| 13 | **Structural tier is 1.05–5.1× outside the 10⁵ interactive floor and 10–50× outside the 10⁶ end** | major | **L1**, then **L9** gated | **P8** |
| 14 | **The golden oracle is 34 cycles and four value assertions, RV32I-only.** *(It is tracked and it does run in CI — the inherited "gitignored, never run" claim is false in two of three clauses; its regeneration path is what dies with `riscv/`)* | major | **L6** | **P19** (new) |
| 15 | **The performance anchor is untracked and its generator is being deleted.** Every measured number in the roadmap becomes unreproducible | major | **L0** | **P19** (new) |
| 16 | **No independent external golden, and its planned home is inside the deleted directory** | major | **L6** | **P16** (new) |
| 17 | **`InteractiveSimulator` does per-event `traceMap` lookup, `BitSet` clone and an `O(probes)` walk, and is unmeasured** — every latency figure here is measured on the other engine | major | **L0(f)**, **L7** | **P18** (new) |
| 18 | **`RegisterFile` and `FieldExtend` are un-exportable to HDL** — `EXPORTED` lists 22 classes and omits both | major | **L8** | **P3** |
| 19 | **Diff and merge are unsafe, and D1 alone makes them less safe.** Plain text with dense ids removes XZ's accidental conflict protection and keeps the renumbering hazard — the exact configuration that produced a clean merge that loaded *and simulated* a 4-bit pin carrying `0xFFF` | major | — | **P11**; ordering in §7 |
| 20 | **Stable-id minting can collide**, so JLS writes files it then refuses to open. *(Partly fixed at HEAD by `36cbd37`; the persisted per-install counter and the save-time uniqueness assertion are still absent)* | major | — | **P11** |
| 21 | **No CI lane hosts a multi-hour run.** *(Partly reversed: a nightly cron with a documented single-lane convention and its own concurrency group already exists at `.github/workflows/ci.yml:8-13,21-25`. What is missing is `timeout-minutes` — zero occurrences repo-wide — a large-fixture policy, and the guest image)* | major | **L2** | **P18** (new) |
| 22 | **The single-source machine definition has no mechanism and no contents list.** The memory map, device base addresses, power-on register state, DTB hash, kernel cmdline, clock frequency, CLINT HZ and RAM size live implicitly in three places with nothing asserting they agree | major | **L5**, **L6** | **P16** (new) |
| 23 | **The console byte stream is not tier-independent by construction.** With any default kernel config `CONFIG_PRINTK_TIME` stamps every line from a timer, and the two tiers reach any instruction at different simulated times — so the designated Linux-scale oracle fails on its first run | major | **L7** | **P21** (new) |
| 24 | **K9, the highest-ranked kill criterion, is a sentence.** No performance ratchet of any kind exists in `test/` | major | governance band | — |
| 25 | **Simulation determinism across JDK builds and OSes is unverified** and the parity contract assumes it | unknown, load-bearing | **L0(h)** | — |
| 26 | **Subcircuits are per-instance deep copies with no parameters**; measured sharing factor exactly 1.00× | major | **not closed**; L8 is sized assuming no sharing | **P7** |
| 27 | **`RegisterFile`/`FieldExtend` declare a `propDelay` they never use**, and appear in **neither** the delayed list nor the zero-delay set of [`simulation-semantics.md`](simulation-semantics.md) | moderate, and it bites L1 | **L1** | **P2** |
| 28 | **`NetlistImporter` realizes 5 cell types and cannot emit a flip-flop**, while `CellValidator` accepts 19 direct types plus 2 parameterized memory shapes. *(And `CellValidator` claims hierarchy instances are realized as subcircuits; `NetlistImporter.mapCell:226-232` rejects exactly that. Hierarchy import is not started)* | major, out of scope here | — | **P3** |
| 29 | **GUI per-edit cost is 58 ms at 10k elements, 552 ms at 100k** | moderate | — | **P12/#84** |

Two entries in the inherited gap list are **dissolved, not closed**:

- *"No event-injection path — fatal."* A polled 16550 **pulls**; the drain
  happens at `beforeEvent`, which already exists. Zero lines in `jls.sim`
  are needed for the injection itself.
- *"`Element` is sealed — fatal."* `Element` permits exactly three types and
  that is unchanged and deliberate for out-of-tree; **in-tree it is a
  measured ~65-line tax across 14 files and zero format version**, and HEAD
  has already added two first-party permits.

And one is **corrected**: the simulation inner loop does **not** live in
`jls/core`. `src/jls/core/` is eight geometry files (`Bounds`, `Geometry`,
`GridPoint`, `GridSize`, `Orientation`, `SegmentGeometry`, `TextMetrics`,
`package-info`). `grand-architecture.md` §6's hot-plane rule is a constraint
on the **future** core module; today the loop is in `jls.sim` and already
satisfies it in substance — no cross-package `post()`, nothing dispatching
through the registry.

---

## 6. What no program owns

Written in the roadmap's idiom: what it delivers, cost band, minimum useful
version, dependencies. **Cost bands here are analogies against shipped work
(#78's registry, #166's canonical save, #167's op layer, #199's synchronous
memory, #201's two elements at 1,188 insertions across 14 files). They are
not measurements. Say so wherever they are quoted.**

### P14 — Host I/O and the device element  *(the one that cannot be worked around)*

**Delivers.** A first-class device concept: an in-tree `LogicElement`
subclass whose `react` exchanges bytes with a host resource, plus the seam,
lifecycle and injection contract that make it legal. Four parts: the
extension-point row with an owning issue, filed **before** any code; a
device module publishing descriptors into the shipped registry, cold plane
only; in-tree device elements (`Console`, and later a full 16550), hot
plane, in core, inside the seal; and the injection contract — the host
thread fills a non-blocking ring, the element drains it at
`Simulator.beforeEvent`.

**Why nothing covers it.** The corpus's designated home for "system and
interfaces" contains zero device concept, zero host resource, zero
character stream and zero interactive input, and its own "what genuinely
stays out, and why" section enumerates 20+ exclusions among which a host
character stream is **not listed** — not declined, not deferred, not
costed. Absent.

**Governance is the hard part, not the code.** See §7.

**Cost band: 10–16 weeks. Floor: 4–6 weeks** — one output element draining a
watched net to `System.out` on a rate-limited channel plus an input element
polled at `beforeEvent`. That alone gives a headless boot a visible kernel
log, which is the first real milestone. **Depends on:** the #220/#224
integration slice; **P2's byte lanes** (a 3-byte-address UART is not
addressable without them). **Blocks:** everything interactive.

### P15 — Simulation state as a versioned format, and bulk images

**Delivers.** A checkpoint as a **D3-shaped optional, internally versioned
section**; the **bulk-image sidecar** for kernel and RAM images, never
diffed, outside `MAX_CIRCUIT_TEXT_BYTES`; the loader, running before
`Circuit.finishLoad` so nothing in `jls.sim` changes; **content
initialization for `RegisterFile`**, which today has no `init`, no `file`
and no write-back; an **input-log cursor** and a **retirement index**
alongside `now`, because simulated time is a permitted divergence and the
same input log must replay into both tiers; and a **pinned** flag exempt
from any retention policy, or the boot snapshot is evicted by the policy
meant to protect it.

**Non-negotiable acceptance criterion:** `replay(ckpt[i]) == ckpt[i+1]`
**byte-identically, in CI**. One uncaptured mutable field produces a replay
that is almost right, and the field enumeration is already stale twice
over. **Gate on the property test, not on reviewing a list that decays
every time an element ships.**

**Cost band: 8–14 weeks. Floor: 3–5 weeks. Depends on:** P1-S0's
per-`Simulator` sequence counter; P9's element pass (schedule it **inside**
P1's element pass — both walk the same 25 simulating `react`s and 28
`initSim`s); D3; and **L0(i)'s `maxTime` adjudication**, because a
checkpoint taken after termination has a queue missing a dropped event.

### P16 — The retirement-indexed parity harness

**Delivers.** The comparison alphabet as a running artifact: one record per
retired instruction, sync-point architectural dumps, a differential
comparator, and a verdict lattice
(`PARITY_HELD | DIVERGED_AT_INDEX_N | UNKNOWN | NOT_COMPARABLE`, where
**UNKNOWN and NOT_COMPARABLE are never passes**), plus a simulate-to-confirm
rule. Plus the **independent external golden** (§4 L6) and its home.

**Why nothing covers it.** Every comparator in the repository is
**end-state only**. In the contract's terms the entire in-tree verification
culture is "architectural state at the end of the run" with the sync quantum
set to the whole run, and per-instruction comparison entirely absent.
**Nothing in JLS knows what an instruction is, and no program proposes to
teach it.** Over 4×10⁷ instructions, attribution *is* the job.

**Two rules to write into the contract before any code.**
**Retirement-boundary sampling** — a settling value that resolves before the
sync point is permitted microarchitectural state; one surviving into a
committed `rd_value`/`mem_wdata`/`pc_after`/`trap` is a real defect. And
**sync point zero** — JLS supplies a reset the design does not have, so two
machines can agree on every record from instruction 1 and disagree at
instruction 0; the machine definition must specify the power-on state of
every architecturally visible register.

**Cost band: 10–16 weeks. Floor: 4–6 weeks. Depends on:** P5's report
channel (**1 week, five consumers, and the window is open now** — a parity
verdict must be a sixth consumer *within that week*); P5's timestamp
closure (a prerequisite, not a nicety, because `WireNet.propagate`
overwrites eagerly and a naive per-event sampler produces spurious
divergences); P15 for bisection; and a behavioral golden for `RegisterFile`,
which is **the only simulating element without one**
(`test/jls/ElementSimulationGoldenTest.java` lists it `EXEMPT`) and which
produces two of the twelve record fields. **Shared deliverable:** this **is**
the replacement for `riscv/verify.py` that the value-domain migration also
loses to D5. **Fund it once, for both.**

### P17 — The per-subcircuit fidelity toggle

**Delivers.** D4's fourth direction-of-travel item, and the *only* one
nobody owns. Nested/shared/parameterized definitions are owned by P7;
compiled backends by P8; expanded element definition by P2. `grep -rn
'fidelity|per-subcircuit'` across all roadmap documents returns nothing.

**Cost band: 6–10 weeks. Floor: 3–4 weeks** — a saved per-instance
attribute plus an elaborator that honors it, with the compiled side stubbed
to the existing interpreter, so the toggle is testable before P8 exists.
**Depends on:** P3's reuse identity — you cannot toggle "the UART" if there
is no "the UART", only *n* inline copies. **Open and undesigned by anyone:**
what a **mixed-fidelity checkpoint** contains when one subcircuit runs on
the interpreter at event granularity and another compiled at timestamp
granularity.

### P18 — Long-run ergonomics and the multi-hour CI lane

**Delivers.** The time-limit model; `maxTime` overflow semantics (**a
`simulation-semantics.md` change under #221's process clause**, because
fixing it changes observable behavior); a progress and resume contract; the
**two-lane CI split** — an offline golden lane on every PR as the blocking
gate, plus a nightly long lane that is **informational only**, with the
in-tree warning attached: *"a nightly lane that depends on unpinned
upstream will go red for reasons that are not JLS's, get muted, and then
guard nothing"*; a **large-fixture policy** and `timeout-minutes`
everywhere; and **batching `InteractiveSimulator`'s per-event trace/probe
work** through the rate-limited channel `grand-architecture.md` §6 already
requires — **binding for a live console, because the console runs on that
engine.**

**Cost band: 3–5 weeks. Floor: 1–2 weeks.** Smaller than it looks: the
nightly trigger and its house convention already exist, so adding a lane is
a copy of an existing pattern plus one `timeout-minutes` line.

### P19 — The in-tree CPU-scale calibration fixture  *(blocks D5)*

**Delivers.** A first-class, tracked, tested, CI-runnable CPU-scale circuit
plus its benchmark harness and golden, replacing four things at once: the
generator, the differential oracle, the performance harness, and the
untracked fixture. `riscv/bench_kernel.py` says in its own header that it
produced the kernel measurements in
[`capability-roadmap/keystone-c-performance.md`](capability-roadmap/keystone-c-performance.md)
and instructs the reader to **re-run it rather than trust those numbers** —
and D5 deletes it.

Consequences, all real: P1's stage-5 acceptance criterion loses its
baseline; P11's headline criterion (a 5,314-line diff falling to 9) becomes
**unreproducible**, because its fixture is untracked and its generator is
deleted; P12's whole-program criterion ("no save-format string literal
appears anywhere in `riscv/`") becomes **vacuously satisfied by
`rm -rf riscv/`**.

**Cost band: 4–8 weeks. Floor: 2–3 weeks. Ordering: this must land BEFORE
`riscv/` is removed.** It is cheap, it is fully D5-compliant, and it serves
the roadmap at least as much as it serves this program. **It is the
highest-goodwill item on offer.**

### P20 — The behavioral machine tier

**Delivers.** The virtual-hardware half: RV32I + Zicsr + M-mode traps,
enough to boot nommu Linux with the same memory map as the drawn machine.
**Why nothing covers it:** P2 is the vehicle (a new `LogicElement`
subclass) but does not scope it, and no roadmap program contemplates an
element whose body is an instruction-set interpreter. The corpus's
element-cost rate (~1.5 weeks for the first element including the plumbing
pattern) is calibrated on a `Multiplier`, not on RV32IMA with CSRs and
traps.

**Cost band: 6–10 weeks. Floor: 3–4 weeks. Depends on:** P14 (it needs the
same device elements); P16 (its record emitter is trivially cheap — it
*knows* what an instruction is, which the drawn machine does not).
**Design constraint:** it must reproduce whatever reset fiction the drawn
machine depends on, or sync point zero cannot hold. See L0(g).

### P21 — The guest software stack  *(new here; owned by nothing, dereferenced by three commitments)*

**Delivers.** The thing that is actually booted. Nothing in the layer stack,
the roadmap, or any milestone owns it, yet the Linux-scale parity mechanism
requires a guest-computed digest in the initramfs, the nightly lane requires
a bootable image, and the headline instruction count is a property of one
particular image.

Contents: a **pinned kernel version and `.config`**; **busybox and an
initramfs** including the digest utility the contract names; a **device tree
blob** and `dtc`; the **memory map and every device base address**; a
**reset stub**; an **RV32 cross toolchain**; and a **rebuild recipe with
checksums** that K8 is written against.

**Plus the determinism requirements the contract silently assumes.** With
any default kernel config the console byte stream is **not** tier-independent:
`CONFIG_PRINTK_TIME` stamps every kernel line from a timer, and the two
tiers reach any instruction at different simulated times. Same class:
BogoMIPS (`lpj`-dependent), crng-init ordering, hostname and date in the
prompt, dmesg interleaving. The fixes are trivial — `printk.time=0`, a
pinned `lpj=`, a fixed hostname, a time-free prompt — but they are
**guest-config requirements that belong in the machine definition**, and the
machine definition has neither a mechanism nor a contents list. The cheapest
settling experiment: boot the pinned image under the instrumented emulator
at two declared clock rates and diff the console streams. One afternoon;
belongs in M1.

**Cost band: 4–6 maintainer-weeks first time; +3–5 more if the Sv32/OpenSBI
hedge fires** (a third guest artifact nobody has named or costed).
**Floor: the same 4–6 weeks** — there is no smaller version that boots.
**Depends on:** nothing in JLS. It can start immediately and in parallel,
and doing so **retires K8's existential risk early**: once the image is
pinned and checksummed, an upstream RV32 nommu removal becomes
documentation rot rather than a program-ending event.

### Where the image lives, and the contradiction that must be resolved

The layer stack simultaneously specifies a **nightly CI lane that boots the
behavioral tier and diffs the console stream against a golden** and
**excludes committed guest images, committed checkpoints and Git LFS**. A
hosted runner has no outside payload unless it downloads one, and an
unpinned nightly download is precisely the lane the in-tree standards work
warns will "go red for reasons that are not JLS's, get muted, and then guard
nothing". **One of the two must be withdrawn.**

The resolution the evidence supports: a minimal RV32 nommu kernel plus a
busybox initramfs is ~2–6 MB, far under GitHub's 100 MB per-file block, and
a measured comparison found a **stored (uncompressed) container member cost
2,397,301 bytes of `.git` over ten revisions against a raw sidecar's
2,396,453 — within 0.04%.** Committing the pinned image as a stored member
is cheap and makes the lane runnable. It requires **explicitly reopening the
exclusion**, which is a decision, not an oversight to route around.

### Unowned roll-up

| | Delivers | Cost | Floor |
|---|---|---:|---:|
| **P14** host I/O + device element | the console; the only irreplaceable item | 10–16 | 4–6 |
| **P15** state serialization + bulk images | checkpoint/resume; kernel loading | 8–14 | 3–5 |
| **P16** parity harness + external golden | the record stream; replaces the deleted oracle | 10–16 | 4–6 |
| **P17** per-subcircuit fidelity toggle | D4's one unowned item | 6–10 | 3–4 |
| **P18** long-run ergonomics + CI | makes a multi-hour run runnable and gated | 3–5 | 1–2 |
| **P19** in-tree calibration fixture | unblocks D5; restores the roadmap's baselines | 4–8 | 2–3 |
| **P20** behavioral machine tier | the interactive half of parity | 6–10 | 3–4 |
| **P21** guest software stack | the thing that boots | 4–6 (+3–5) | 4–6 |
| **total** | | **51–90** | **24–36** |

**And the roadmap slices this objective requires.** Mapping the objective
onto the thirteen recorded programs gives roughly **105–159 weeks of the
288–424** (about 37%): P1 stages 0 and 1, P2 (the largest required block,
including byte lanes and the CSR/trap element set), part of P5, P8, P9's
checkpoints, P12's floor, plus benefiting slices of P3, P7, P11 and P13.
P4, P6, P10 and P5's formal half are **irrelevant to this objective** and
are skipped — the parity contract permits all timing to differ, a Linux SoC
is orders of magnitude past a one-tile silicon budget, stuck-at faults have
no bearing on booting, and the corpus itself lists unbounded sequential
equivalence on non-matching encodings under *where JLS cannot plausibly
lead*, which is exactly what the two tiers are.

**Honest total for the full objective: roughly 155–250 maintainer-weeks —
three to five maintainer-years at bus factor 1.** Two much smaller subsets
exist and both are defensible: *headless parity* (everything through the
structural boot, no live console) and *the parity contract alone, no boot*
(the report channel, the harness, the calibration fixture and the sequence
counter — nothing about Linux, everything about parity, and it serves the
value-domain migration's oracle problem with the same artifact).

---

## 7. Recorded decisions

**The architecture reverses no recorded decision.** It respects D1–D7,
#165/#166/#167, #181, #222 and `grand-architecture.md` §6 and §9. Two
recorded positions must be **formally addressed** before code lands, and
several process claims in circulation are wrong and are corrected here.

### 7.1 #221 — the discrete-event interpreter as the sole strategy

**Respected by L0–L8, with one exception that must not be self-certified.**
A fidelity binding is one element with one `react()` and one lumped delay at
its pins — structurally indistinguishable from `Adder`, `Memory`,
`TruthTable`, `StateMachine` and `RegisterFile`, all of which already
compute arbitrary functions in one `react()` with no internal delays. JLS
has shipped lumped-delay behavioral abstraction since day one; L4 makes it
**selectable at a boundary** and, for the first time, **machine-checked**.

**The exception is L1's zero-delay closure.** `ARCHITECTURE.md:345-347` says
in terms that no levelized/compiled evaluation pass is built now.
[`simulation-semantics.md`](simulation-semantics.md) §3 normatively
specifies the order that pass replaces, §6.2 enumerates the element set it
collapses, and intra-timestamp values are observable today through
`probeSample`. **Process, and there are only two acceptable forms:**
(a) a recorded note on #221 plus a new `ARCHITECTURE.md` decision block
stating that zero-delay closure with byte-identical goldens sits inside the
recorded option — with its rationale, its revisit trigger, and K3 as its
gate; or (b) run L1 through §7.2's ritual. Self-certification inside a
document the same person wrote is not a third option at bus factor 1.

**The revisit trigger is now quantitatively met and instrumented for the
first time.** The recorded trigger is "a concrete CPU-scale design on the
`riscv/` trajectory that is unusably slow interactively"; the in-tree
performance keystone proposes restating it quantitatively as "below
10 kcycles/s on the #202 golden's CPU" and measures **8,090**. Two
consequences: the recorded process requires **filing the follow-up
implementation issue that `ARCHITECTURE.md` says deliberately does not exist
yet**, with the measured constants attached — and then **immediately
deferring it**, because the semantics-preserving budget must be spent and
re-measured first. And the trigger's wording **names `riscv/`**, a directory
D5 deletes, so restating it is part of the L0 salvage.

**The ritual for L9, when it comes.**
1. File the follow-up issue **first**.
2. Present the trigger evidence as **measured on the real drawn machine**,
   including the L1 result showing semantics-preserving work was exhausted.
3. Propose a **named second conformance level** in
   [`simulation-semantics.md`](simulation-semantics.md) — *cycle-settled
   equivalence*: identical values at every clock-domain settling point and
   identical retirement traces, with **no** guarantee about intra-cycle
   observation order or per-element propagation delay. **State
   affirmatively** that the §2/§9 value domain and the §8 edge semantics are
   **unchanged**, and that the criterion's fourth axis — bit-for-bit
   agreement with the #202 golden — is retained; the criterion binds four
   axes and naming only two leaves an unstated third surrender available by
   omission. Note also that the document has **no conformance-level concept
   today**, so this introduces a new normative structure rather than filling
   an existing slot.
4. Bind the new strategy to **two** oracles: the parity differ and the #202
   golden.
5. Ship it opt-in behind a static classifier that **refuses** circuits where
   the weakening is observable and falls back with a one-line notice.

**And record the counter-argument in the same issue**, with numbers: the
tier gap is ~39× in *events* but L9 is worth **0.5–2×** over an optimized
interpreter (§4 L9), for the most expensive governance token in the
repository.

### 7.2 `docs/vcd-interop.md` and #63 — recording, not reopening

[`vcd-interop.md`](vcd-interop.md) lines 18–23 read: *"Not offered: live
co-simulation … was evaluated and **rejected** — see issue #63. Graders must
not depend on interacting with a running simulation."* That contradicts the
goal and must be addressed explicitly.

**Four corrections to how this has been framed.**

1. **There is no recorded decision to reopen.** `ARCHITECTURE.md`'s recorded
   decisions cover i18n, help delivery, look-and-feel, the #80 plugin
   removal, #222, #223 and #221 — and **nothing for #63**. The correct
   action is **recording a decision that was never recorded**, which is
   cheaper and stronger than reversing one.
2. **#63 is open, and its body plans the very thing the sentence says was
   rejected.** Issue #63 is OPEN, milestoned "M3 — Consumer modules", and
   specifies an external GHDL/Icarus subprocess stepped in lockstep with
   JLS's event loop as a planned staged deliverable. The `vcd-interop.md`
   sentence is not faithful to the issue it cites, independently of this
   program.
3. **The document is self-declared informative** — *"the normative
   contract … lives in `batch-interface.md`; nothing here adds to or changes
   it"* — so the amendment is a documentation edit.
   [`batch-interface.md`](batch-interface.md) is untouched.
4. **The strongest scope argument is one nobody has made.** JLS's own GUI has
   interacted with a running simulation since `Simulator.beforeEvent`
   existed — its javadoc reads *"A mode can block (pause), or set state and
   decline this iteration"* — and JLS ships a `Pause` element. A blanket
   reading would make the shipped product non-conformant with its own
   document. **The sentence means graders must not build on interaction, not
   that the simulator must not be interactive.** Say that.

**Proposed replacement text:**

> *Graders must not depend on interactive input. The supported grading
> surfaces are batch artifacts: exit status, stdout report, VCD, the
> retirement trace, and a recorded console transcript. **An interactive
> session is a recording device; the recording, not the session, is the
> contract.** Replay of a transcript is deterministic, threadless,
> byte-reproducible and CI-runnable.*

Today "don't interact" is enforced by there being no way to. Afterwards it
is enforced by a replay test, which is stronger.

**Process.** A decision issue quoting both sentences; **edit** the section
rather than leave the contradiction standing; a CHANGELOG entry; an
`ARCHITECTURE.md` decision block carrying **a rationale and a revisit
trigger** as `ARCHITECTURE.md` requires — proposed trigger: *an external
testbench or transport requesting to own `now`*. Then a consistency pass
across the **five** in-repo sites, not one: `vcd-interop.md` twice,
`examples/autograde/autograde.py`, `test/jls/AutogradeBridgeExampleTest.java`,
and `docs/standards-adoption/07-waveform-formats.md`. Note also that
`grand-architecture.md` §9 lists **subprocess co-sim on the sanctioned
side** while `vcd-interop.md` calls live co-simulation rejected; they are
reconcilable but currently read in opposite directions, and the amendment is
the moment to state which, once.

**This window is closing.** One roadmap sweep proposes to **strengthen** the
prohibition, and the amendment document would write *"no callback direction,
ever"* into a normative document **as permanent**. If that lands first, this
becomes a documented reversal of a permanent normative clause rather than a
paragraph. Substantively there is no conflict — the console **pulls**, it is
never called back into — so the contest is over wording only, and it is
winnable only before the wording is normative. **M2 must precede any
scripting-API specification, and this document says so.**

### 7.3 The extension-point catalog (#223) — honored, with corrections

The catalog rule binds: *"A seam gets its row (and its owning issue) before
its contract exists."* Two things must be fixed before anything is filed.

**One device seam, not two.** The evidence base specifies the device seam
**twice, incompatibly**: as `app.device-provider` with cardinality `many`
published by a device module, and as `elem.host-port` over a **sealed**
`HostBytePort` with cardinality "one active per device instance". Different
id, different contract, different cardinality, different home package. Since
**a point id never changes once shipped**, this must be resolved before
either is filed. The reading this document adopts:

- **`app.device-provider`** is the legitimate module-facing **cold-plane**
  seam: `many`, contributable, owned by a device module, with an owning
  issue.
- **`HostBytePort` is not a seam at all.** It is the sealed in-tree
  collaborator **D7** describes. Presenting it as an extension point reads
  *against* D7 and against the catalog's own opening sentence — these are the
  seams **modules contribute through**, and a sealed contract admits no
  external contribution, so the row would advertise a surface nothing can
  contribute to. **Honest minimum: declare it in the catalog under a "sealed
  in-tree collaborators" subsection, or in a row whose Status cell says it
  is sealed and closed to external contribution** — visible so nobody
  invents a parallel mechanism, without claiming a seam it does not offer.

**Three smaller catalog corrections.** "One active per device instance" is
**not a legal cardinality**; the column vocabulary is exactly `many` and
`one active`, and inventing a third silently amends it. Both proposed rows
lacked **owning issue numbers**; milestone labels are not issues. And a
`fidelity-binding` seam homed in `jls.sim.equiv` should carry the `sim.`
prefix, not `elem.` — the prefix **is** the home area, and ids never change.

**Deliberately not added:** no execution-strategy seam (a second strategy is
core-internal with zero plugin indirection); no machine-model seam (the
model is compiled in and closed); no new element seam for `Console`, which
is a contribution to the existing `elem.element-provider` row. Fewer seams
is better governance.

### 7.4 #222 — the plugin trust boundary: extended, not reopened

Direction correct and consistent with D7, with one clause repaired. **"A
loaded `.jls` never acquires host I/O" is right and it is the whole
argument** — it is #38's and #170's threat model, `UntrustedFileHardeningTest`
exists at HEAD, and an invocation-time grant is the right shape. But
*"sealed, so #212's external-provider gate structurally cannot reach it"* is
the wrong reason: #222 concedes in terms that a trusted external jar has
full JVM authority, and such a jar calls `System.in` directly, seal or no
seal. Sealing prevents new **implementations**; it does not prevent an
in-process module from **acquiring** a granted port. The load-bearing
protections are (a) `Element`'s seal — still exactly three permits — plus
#212 being gated shut (verified: **#212 is OPEN**, and
`grand-architecture.md` §9 requires "a real user asks, with the
trust/sandbox stance resolved first"), and (b) the invocation-time grant.

The right instrument is an **addendum to #222 in #222's own vocabulary**,
carrying a revisit trigger as `ARCHITECTURE.md` requires — proposed: *if
ever overruled, host-touching providers go out-of-process*, which is
§4.3's reserved case, because in-process is the one variant that cannot be
walked back.

### 7.5 Format and diff decisions

- **D1 (uncompressed default) has not landed.** `FileAbstractor` already has
  a tested `Container.PLAIN_TEXT`, and `writeCircuit` still delegates to XZ
  (`src/jls/FileAbstractor.java:195-198`). Flipping the default is policy
  plus test updates, not an implementation project. The 64 MiB cap does not
  regress: it is measured against **decompressed** text
  (`src/jls/FileAbstractor.java:65`).
- **The ordering is the safety property, and it is currently a
  parenthetical.** Plain text with dense save-time ids **removes XZ's
  accidental conflict protection while keeping the renumbering hazard** —
  the exact configuration that produced clean-but-wrong merges, one of which
  loaded *and simulated* a 4-bit pin carrying `0xFFF`. And uncompressing
  without fixing identity makes repo bloat **worse** in the guaranteed
  front-insert case (11,003 B/rev text against 7,531 B/rev XZ). **Stable-id
  minting, a headless `Circuit.validate()` at the end of `finishLoad`, the
  legacy sort-order fix, and the `.gitattributes`/`.gitignore` lines are a
  hard precondition of the container flip, not a preference.** This is also
  the largest under-examined first-year-student impact in the program.
- **D2 (diff stability).** `Circuit.save` reassigns **dense file-local ids
  on every save** (`src/jls/Circuit.java:1499-1503`), so inserting one
  element renumbers every later element and every reference to them —
  measured at **5,312 changed lines in 234 hunks where the correct answer is
  10 lines in 1 hunk**. Referencing by stable id is the structural fix and
  its acceptance criterion is measured.
- **D3 (internal versioning).** There is exactly **one global `FORMAT`
  integer per file** (`src/jls/Circuit.java:1482`), no per-section version
  and no must-understand distinction anywhere. Both the memory-image
  **section** and the fidelity **attribute** (§4 L4) need it — applying it to
  one and not the other is inconsistent.
- **A precondition worth naming.** `36cbd37` advanced the id creation
  counter past stable ids already in use. That is not an unrelated tidy-up:
  **any stable-id-referencing format inherits exactly that collision failure
  mode**, and the persisted per-install counter and the save-time uniqueness
  assertion are still absent.

### 7.6 `batch-interface.md` §6 — the additions are not all additive

§6 freezes §2 (the `-t` grammar), §3 (stdout) and §4 (the VCD profile), and
requires a CHANGELOG entry **and** a major version bump or a compatibility
flag for "a change that alters any byte a conforming consumer could
observe". The only sanctioned additive case is **a new flag, or a new
optional output gated behind a new flag**.

- **Safe:** `-d 0`, and any new flag. §1 is not frozen.
- **Not additive as proposed:** the **abstraction banner** (gated on circuit
  content, landing in §3's "exactly two things" and in §4.2's deliberately
  byte-deterministic VCD header) and the **progress heartbeat** on stdout.
  Flag-gate both, or take §6's major-bump path.

### 7.7 Two process facts the whole corpus has wrong

- **#77 is CLOSED** (completed, 2026-07-25, milestone "M1 — Headless
  kernel"). The sequencing rule "everything else waits on #77" describes a
  gate that is **already discharged**, and the program's start date moves
  accordingly. **If D6 meant "wait for the module runtime to be *consumed*",
  the referent is #224 and the #220/#224 integration slice, which is
  open.** D6 is binding; this ambiguity must go back to the maintainer
  rather than be resolved silently.
- **#33 is CLOSED** (completed, 2026-07-27, 28 sub-issues, 25 completed).
  `CONTRIBUTING.md:21` — *"#33 is the tracking issue that orders the current
  program of work"* — is **stale at HEAD**, and any issue filed "under #33"
  is filed under a closed tracker. The live umbrella is **#224** (open,
  parent of #221). Fix `CONTRIBUTING.md` as a drive-by.

### 7.8 The keystone contradiction, unresolved

§4 L4 states it: a per-instance fidelity attribute versus a registered
behavioral core element. The two readings differ in permits entries, in
format cost, in what the harness compares, and — decisively — in whether the
legitimacy argument holds at the CPU boundary. **It must be resolved by the
maintainer before M3, which is the merge that would encode it.** It is a
decision, not a deduction, and neither of the documents that assert opposite
answers noticed the other.

---

## 8. What is deliberately excluded

In the idiom of `grand-architecture.md` §9. Each exclusion states its price
and, where one exists, its re-entry trigger.

1. **A live console on drawn logic.** 1.05–5.1× outside the 10⁵ floor after
   a fully costed optimization program, 10–50× outside the 10⁶ end. **This
   is a decision and a substantial body of engineering work, not a physical
   limit** — but it is not offered, and no documentation may promise it.
   **Re-entry trigger:** the maintainer decides to spend the engine budget,
   with a work breakdown that does not exist today.
2. **General simulation-state checkpoint/restore.** A permanent per-element
   serialization tax across the whole element library in service of
   re-running a boot; boundary handover supplies what parity actually needs.
   **Re-entry trigger:** three consecutive structural expeditions failing
   past the one-hour mark, **or** a CI plan that would re-boot the behavioral
   tier more than once per lane. When it comes it arrives as a D3 optional
   section, not a fourth sidecar format. *(Note the tension: P15's floor is
   listed as required for parity-harness bisection. If bisection is wanted,
   this exclusion is already partly withdrawn — say which.)*
3. **A machine-description IR or DSL.** A new language in a bus-factor-1
   project. The circuit is the description; the recorded construction API is
   the construction API.
4. **HDL as the source of truth.** RTLIL's word-level cell set and JLS's
   element set do not span each other; the decisive levers are *idioms*, not
   cell maps; and a flagship whose source of truth requires an out-of-jar
   synthesizer rots.
5. **DMI and temporal-decoupling fast-forward.** At a large quantum the
   drawn bus shows a sampled fiction and the only mitigation is a warning
   banner — social, not structural. Cut entirely; no arithmetic here depends
   on it.
6. **Any second execution strategy in L0–L8.** L9 only, gated, argued,
   opt-in.
7. **A server, a network dependency, an install step, or a plugin execution
   surface ahead of demand.**
8. **Committed multi-MB checkpoints and Git LFS.** The jar stays
   self-contained and offline. **But see §6: the pinned guest image is a
   different question, the nightly lane cannot run without it, and the
   measured cost of committing it as a stored container member is within
   0.04% of a raw sidecar. This exclusion must either be narrowed to exclude
   checkpoints only, or the nightly boot lane must be withdrawn.** One of
   the two; not both.
9. **A CI lane for the structural boot.** At central inputs a 1.7-hour boot
   fits inside a `timeout-minutes: 60` job only *after* the L1 stack (44–46
   min), and at the pessimistic end of the honest band it fits no hosted lane
   at all. The structural boot is a **release-cadence expedition recorded in
   CHANGELOG with its commit SHA**. **Accepted cost, stated plainly:** the
   headline result is verified by a human, in a project whose constraints
   otherwise demand test-enforced checks. Mitigated, not solved, by the
   per-push tiers checking both bindings on every push.
10. **Per-class coverage exemptions.** None requested. *(Note that governed
    exemptions do exist in-tree — `jls.edit` is deliberately unfloored, the
    SpotBugs exclusions are scoped, and the element golden suite has an
    `EXEMPT` set — so "none available" would be false. None are requested
    here.)*

---

## 9. Kill criteria

Numeric, measured at named milestones, each with a stated consequence. If
one fires, the consequence is taken, not argued with. **K9 outranks
everything above it.**

**K1 — behavioral events per retired instruction (M1).** Modeled 12.
- **> 25:** boot > 5.3 min, echo roughly doubles, `ls` > 1.5 min. The word
  "live" is retired from all documentation; the claim becomes
  "responsive-ish, one command per minute". Program continues.
- **> 46** (twice the 23 events/cycle budget ceiling): **stop the
  live-console claim entirely.** Ship headless, transcript and replay. L7's
  GUI console pane is cut.

**K2 — α, and with it the structural boot (M1).** Band 0.18 / 0.40 / 0.56,
with `k` adjudicated in the same experiment.
- **Boot ≥ 9.7 h and L1 delivers < 2.0×:** the structural deliverable is
  restated as a release-cadence expedition and no nightly structural lane is
  ever built.
- **Boot > 12 h:** cut the full structural boot claim. The structural
  deliverable becomes riscv-tests parity plus bounded handover windows, and
  "no drawn logic simulator has booted an OS" stays true of JLS too. **Say
  so.**

**K3 — L1's acceptance gate (continuous).** If byte-identical VCD and stdout
cannot be achieved across the **entire** existing golden corpus, **L1 stops
at the failing change.** No semantic change is permitted to buy speed. There
is no partial credit.

**K4 — the null test (M3).** If the deliberately-wrong fidelity binding is
**not** rejected by the harness, **L4 stops.** An unfalsifiable parity
harness is worse than no harness, because it converts an unchecked claim
into a checked-looking one. Nothing downstream of L4 may merge until the
null test fails on demand.

**K5 — `jls.mach` under its coverage bar (M4).** Budget 4 months.
- **> 8 months without reaching the bar:** cut the model to RV32I + Zicsr +
  M-mode only, **abandon the Linux target**, and promote an in-jar M-mode
  self-checking payload to *the* parity workload. The architecture survives
  this intact; the flagship demo does not.
- **This criterion is written against a self-imposed bar.** 93.0/92.0/84.5
  is `jls.sim`'s package floor, not a repository requirement, and PIT is
  neither per-package nor a PR gate (§4, governance band). **If the bar is
  adopted voluntarily, K5 must say so** — otherwise the harshest criterion
  in this document threatens to cut the Linux target for failing a gate the
  repository does not impose.

**K6 — drawn-machine authoring (M7).** **More than three ground-up
revisions, or more than 16 weeks of authoring**, and further hand-drawing
stops: programmatic generation is escalated ahead of it, with an issue.

**K7 — Mode C's gate (before L9 is scheduled).** The measured levelized cost
is 4.32 ns/node at 522 slots (with an unreconciled 3.10 ns/node in the same
source — L0(d) settles it).
- **Break-even against the 2.26× interpreter is 8.45 ns/node**, derived:
  44,010 cycles/s is 22.7 µs/cycle; ÷ 2 passes ÷ 1,346 nodes = 8.45 ns.
- **A real implementation at ~1,400 slots measuring worse than 8.45
  ns/node: do not build it. #221 is not reopened.** The structural tier's
  speed is whatever L1 made it, and that is the end of the matter. *(An
  earlier draft set this threshold at 15 ns/node; at 15 ns Mode C is already
  **slower** than the optimized interpreter — 24,772 against 44,010
  cycles/s. The corrected threshold is 8.45, and the measured 4.32 clears it
  by only 2×.)*

**K8 — the guest image (continuous).** If the pinned kernel and initramfs
cannot be rebuilt from their documented recipe **by the maintainer alone in
under 2 hours** at any point — RV32 nommu removal, toolchain drift,
`CONFIG_NONPORTABLE` withdrawal — the Linux target is demoted below the
in-jar M-mode payload and the demo is restated. A published removal proposal
targets "the beginning of 2027", **inside this program's window**; the
kernel version is pinned as a documented artifact from day one, which
downgrades this from an existential risk to documentation rot. *(The
upstream removal claim is inherited and was **not** verified from this
environment. Verifying it is one hour of work: read the pinned kernel's
`arch/riscv/Kconfig` for the gate and the linux-riscv archive for the
thread.)*

**K9 — the pedagogy floor (continuous, and it outranks everything above).**
GUI startup time, per-edit cost (measured 58 ms at 10k elements, 552 ms at
100k) and palette size are ratcheted. **Any regression to the first-year
student drawing an adder stops the responsible layer, regardless of what it
costs the flagship.** The pedagogy audience is the product; the Linux boot
is a demonstration.

**K9 is currently unenforceable, and that must be fixed in M1.** There is no
performance ratchet of any kind in `test/` — the seven ratchet tests cover
collaboration security, headless core, notifications, nullness, package
info, pointer API and socket confinement, and **none is a performance
ratchet**. No layer in §4 builds one. The highest-ranked criterion in this
document is a sentence. **The cheap fix is minutes of work**: one headless
assertion on palette row count and `ElementRegistry.ALL` size —
`src/jls/edit/Palette.java`'s `ENTRIES` list holds **32** rows against
**35** registry types, a difference `test/jls/edit/PaletteContractTest.java`
already governs through its `NON_PALETTE_TAGS` set, so the ratchet pins the
*count* rather than re-litigating the set, and palette bloat is caught
outright — plus one timing test with a generous band on the existing
10k-element path.
**Until that test exists, K9 is aspiration, and this document says so.**

---

## 10. The milestones

Ordered and costed. **Cost figures below M1 are estimates without work
breakdowns and inherit M1's measurement risk**; they are the layer stack's
own costs and do **not** include the roadmap slices §6 shows are required
(byte lanes, the report channel, the sequence counter, the element
vocabulary work). The honest total for the full objective is §6's, not this
section's.

### M1 — The measurement gate and the salvage  *(~4–6 weeks, no product code)*

**Deliverable.** All nine L0 experiments; [`machine-calibration.md`](machine-calibration.md);
the `maxTime` adjudication filed; **P19's tracked CPU-scale fixture,
benchmark and golden committed**; the `riscv/` re-homing inventory
regenerated from `git ls-files` (26 files, not 3) including
`riscv/gui/cpu.jls` and the two dangling javadoc references at
`test/jls/RiscvCpuGoldenTest.java:25,38` that doclint will not catch;
`ARCHITECTURE.md:354-358`'s revisit trigger restated so it does not name a
deleted directory; the differential-harness *design* written into
[`parity-contract.md`](parity-contract.md) §0; **then** `riscv/` deleted.
Plus **the K9 ratchet**, which is minutes of work and without which the
highest-ranked kill criterion does not exist.

**Acceptance test.** `machine-calibration.md` carries a measured value with
a named harness for α, CPI, `k`, events per cycle **with its clocking
regime**, behavioral events per instruction, levelized ns/node at ~1,400
slots with the two costs reconciled, echo-path length, `InteractiveSimulator`
per-event cost, and a three-platform VCD diff. The new benchmark fixture
reproduces the keystone counters. `mvn verify` is green with `riscv/` gone.

**Unblocks.** Everything. **Nothing else in this document is honestly
costed until M1 lands**, because every wall-clock figure divides by one of
its outputs. *Four separate analyses scheduled this experiment after
committing to an architecture. It is days of work and it is commit #1.*

### M2 — `Console`, `HostBytePort`, the transcript, and the `vcd-interop` recording  *(~4–6 weeks)*

**Deliverable.** A drawn FSM prints. A GUI session records and replays in
batch byte-identically, which finally joins JLS's two front ends. The
`app.device-provider` row with its owning issue, filed **before** code; the
`HostBytePort` visibility entry per §7.3; `SessionBoundaryRatchetTest`; the
`vcd-interop.md` amendment with its `ARCHITECTURE.md` decision block,
rationale, revisit trigger and five-site consistency pass.

**Acceptance test.** A recorded transcript replays byte-identically in batch;
a determinism ratchet asserts no golden or VCD fixture may be produced in
live console mode; `ExtensionPointCatalogTest` is green in both directions.

**Unblocks.** Everything interactive. Contains no RISC-V, no Linux and no
parity machinery, and is immediately useful to a first-year lab.
**Ordering:** this must precede any scripting-API specification, or the
amendment becomes a reversal of a permanent normative clause (§7.2).

### M3 — The fidelity boundary, its harness, its null test  *(~5–8 weeks)*

**Blocked on a decision, not on code:** the §7.8 keystone contradiction, and
the §4 L4 format-version question (FORMAT 3 or D3 must-understand).

**Deliverable.** The boundary, the harness, the deliberately-wrong binding,
the declared refusal set, and normative `docs/abstraction-levels.md` applied
retroactively to `Adder`, `Memory`, `TruthTable`, `StateMachine`,
`ShiftRegister`, `RegisterFile` and `FieldExtend` **before** any CPU
binding. Demonstrated on an **ALU subcircuit** — drawn versus compiled. Zero
RISC-V.

**Acceptance test.** K4: the null test fails on demand, in CI. An
all-structural run is byte-identical to the existing golden. A bytecode scan
proves no binding touches the event queue.

**Unblocks.** L5–L9. *If the program stops here, JLS has a console,
deterministic replay, a normative abstraction-level policy applied
retroactively to seven shipped elements, and a machine-checked fidelity
toggle — all independently valuable and none of it wreckage.*

### M4 — `jls.mach` and the external golden  *(~4–6 months; the dominant cost)*

**Deliverable.** The ISA model, the device models, the reference runner, and
riscv-tests through both bindings. Plus the **independent external golden**
and its new home (§4 L6), and the `RegisterFile` behavioral golden that is
a prerequisite rather than a follow-up.

**Acceptance test.** riscv-tests pass through both bindings with
byte-identical trace files; the external golden agrees; the new packages
carry `@NullMarked` `package-info.java` files, ratchet rows, and floors
**measured then set** per `CONTRIBUTING.md`'s convention (§4, governance
band).

**Kill criterion.** K5, with its self-imposed-bar caveat.

### M5 — The retirement trace and `--diff-against`, shipped as a student feature  *(~3–5 weeks)*

**Deliverable.** *"JLS names the exact instruction where your drawn CPU
first disagreed with the reference, and prints both records."*

**Acceptance test.** A seeded divergence is reported at the correct index
with both records and the differing fields named; the exclusion set is
ratcheted and printed in every report.

**Unblocks.** M7's boundary-by-boundary bring-up, and it justifies the
program on its own.

### M6 — The guest image, and the behavioral SoC boots Linux  *(~6–10 weeks; the sentence becomes true)*

**Deliverable.** P21 (4–6 weeks, and it can run in parallel from day one),
plus the drawn SoC with its CPU bound behavioral, plus the GUI console pane
with block and character modes. **Prerequisite from outside the layer stack:
P2's byte lanes**, without which the 16550 is not addressable and there is
no Linux.

**Acceptance test.** A nightly lane boots to a prompt under a console script
and diffs the console byte stream against a frozen golden — which requires
the §6 image-location decision to have been taken. Guest determinism
(`printk.time=0`, pinned `lpj=`, fixed hostname, time-free prompt) is
asserted by the same lane.

**Unblocks.** The headline claim, and the differential counterparty M7
needs.

### M7 — The drawn machine, brought up boundary by boundary  *(~3–6 months)*

**Deliverable.** ALU, register file, decode, load/store, CSR and CLINT, each
drawn, each with a fidelity boundary, each checked by M3's harness the day
it is drawn. Then the CPU boundary. Authoring in the editor where structure
is pedagogically load-bearing; generation through the recorded construction
API where it is mechanical repetition.

**Acceptance test.** At every commit, whatever is drawn is checked. K6 is
the escape hatch.

### M8 — The structural headless boot  *(~2–4 weeks plus wall clock)*

**Deliverable.** The drawn machine boots the same image headless; the two
byte streams are diffed. Recorded in CHANGELOG with the commit SHA, because
no CI lane hosts it (§8).

**Acceptance test.** The console byte stream matches M6's golden, under the
same script, keyed on output bytes rather than on time.

**Kill criterion.** K2.

### M9 — Mode C  *(gated, and possibly never)*

**Gate.** K7 at 8.45 ns/node, measured at ~1,400 slots; α measured in M1;
the L1 stack exhausted and re-measured; the §7.1 ritual completed. **The
honest proposition is 0.5–2× over an optimized interpreter for the most
expensive governance token in the repository**, and it may well not be
worth taking.

### Two standing tracks, not milestones

- **L1, the engine constant factors.** Independently valuable to every
  student today, needs no architecture at all, and lands somewhere across
  M1–M4 as its own track. Its gate is K3 and its governance is §7.1.
- **The format and diff track.** Stable-id minting, headless
  `Circuit.validate()`, the sort-order fix and the `.gitattributes` lines —
  **then** the D1 container flip, in that order, because the ordering *is*
  the safety property (§7.5).

### The honest totals

- **To the maintainer's sentence** (M1–M6, layer-stack scope only):
  roughly **6–9 months** at a single maintainer's realistic cadence.
- **To the structural boot** (through M8, layer-stack scope only): roughly
  **14–22 months**.
- **For the full objective including the roadmap slices it requires**
  (§6): roughly **155–250 maintainer-weeks — three to five maintainer-years
  at bus factor 1.**

Those three numbers are estimates without work breakdowns, and the first two
exclude work the third shows is mandatory. The gap between them is not
sloppiness; it is the difference between costing an architecture and costing
a program. **Cost the program.**
