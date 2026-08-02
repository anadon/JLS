# 06 — THE ROADMAP THREAD: a driving use case for a capability roadmap

**What this document is.** `docs/capability-roadmap/` is a 13-program, 288–424
maintainer-week capability roadmap with costs, dependency analysis, external
prior art, and no driving use case. This study is a driving use case —
*terminal-only Linux booting on JLS-simulated hardware, interactable live, with
a parity contract between the behavioral and structural models* — with measured
constants and no program structure. Neither is complete without the other.

**The relationship, stated precisely and grep-verified.** The roadmap never
names Linux, an operating system, a console, `stdin`, or a device. `grep -ril
"stdin\|operating system"` over `docs/capability-roadmap/` returns **nothing**;
`grep -ril "rvfi\|rvvi"` over the entire tree returns **nothing**; `UART`
appears three times across ~25,000 lines and every occurrence is a peripheral to
*draw*, never a thing connected to a terminal. Exactly one sentence in the whole
corpus points at this use case, and it is a P8 unlock claim: the cycle-based
strategy "unblocks OS-scale `riscv/` work" (`README.md:711`).

So:

> **The roadmap supplies MECHANISMS, COSTS and IMPLEMENTATION PATHS.
> This study supplies the ORDERING FUNCTION — and, in doing so, exposes seven
> capabilities that none of the thirteen programs covers.**

The roadmap's own ordering function is *competitive leapfrog for a teaching
tool*. That is why it ranks the compiled engine low ("speed is precisely the
axis on which JLS cannot lead", `AMENDMENT.md:922-931`) and ranks formal
equivalence first (`AMENDMENT.md:886-909`). Under this use case both rankings
invert. **That inversion is the single most useful thing this document
produces**, and §3 states it as a proposal, not a decision.

Read `BRIEF-DELTA.md` first for the corrected constants. Everything here uses
them.

---

## 1. THE MAPPING — P1…P13 against the use case

Legend. **REQUIRED** = the use case cannot be delivered without it.
**BENEFITS** = materially cheaper or better with it, deliverable without.
**IRRELEVANT** = no bearing. **COUNTER-INDICATED** = the program *as specified*
makes the use case worse.

Cost bands are the roadmap's own (`AMENDMENT.md:955-982`).

| P | Verdict | What the use case needs FROM it | What it explicitly does NOT need | Needed slice / total |
|---|---|---|---|---:|
| **P1** value & resolution | **REQUIRED (S0 + S1 only)** · **S4 COUNTER-INDICATED** | **S0** — per-`Simulator` sequence counter (`SimEvent.sequence` is a mutable `static long`, `SimEvent.java:87`, and a restore that does not reset it cannot guarantee deterministic replay), plus `SigSim` `StringBuilder`/lazy-streaming hygiene. **S1** — `LogicValue` as `record Word(int width, long a, long b, long u)`: the *measured* hard dependency of P8 (**4.32 vs 22.01 ns/node**, `AMENDMENT.md:316`) and the thing that turns a 294.49 ns 32-bit add into 0.60 ns | S2 X-production, S3 strength, S5 `Bits4`/1164. A booting kernel is a fully-driven synchronous design. **Strength is provably irrelevant**: sinks receive `NetValue.value()` only, so it can never enter the RVFI field list. **S4 is a live hazard** — unwritten `Memory` words reading X and `initInputs` no longer zeroing removes the free reset the RV32 PC depends on | **14–19 of 28–36** |
| **P2** element vocabulary | **REQUIRED — the largest required block** | **`Memory` byte lanes.** `lb/lh/lbu/lhu/sb/sh` are unimplemented *because 32-bit `Memory` has no byte lanes* (`README.md:88-90`, verified) and the minimum SoC's UART is **3 byte addresses on a 32-bit bus**. This is a **hard Linux blocker the brief did not name** (4–7 wk). **CSR/trap element set** — "masked-write addressable state that RISC-V privileged mode needs" (`README.md:259-261`), i.e. the brief's Sv32/S-mode +160-element delta delivered as elements (4–6 wk). **Multiplier + Divider** at ~1.5 wk for the first and ~0.75 after ⇒ **~2.25 wk** against a 194-element combinational array (`README.md:295-296`). `Register` reset/enable pins (3–5 wk) | `BidirPin`, `PullUp`/`PullDown`, priority encoder, the SR/JK latch parade, `ShiftRegister` rework | **13–20 of 20–30** |
| **P3** interchange & hierarchy | **BENEFITS — two REQUIRED transitive pieces** | **The shared elaborator** (4–6 wk): flatten hierarchy, union nets across jumps, dense ids, node↔element map. **Seven consumers.** The code already exists *in the wrong place* — `HdlExporter`'s UnionFind net walk (`HdlExporter.java:1038-1109`); "two implementations of which drawn wires are one signal is how the engines come to disagree" (`AMENDMENT.md:788-795`). **Reuse identity / component table** (3–6 wk): `SubCircuit.save` writes each instance's body inline and `Circuit.load` builds a fresh `Circuit` per instance, so "two instances of the same block are two independent copies with no way to tell they are the same" (`README.md:329-332`). **This is the only thing that gives D4's per-subcircuit toggle a boundary to attach to** | HDL export of the CPU, EDIF/BLIF/SPICE/IP-XACT/SystemRDL, the round-trip CI property. A booting machine is never exported | **7–12 of 26–38** |
| **P4** timing & analysis | **IRRELEVANT, and partly COUNTER-INDICATED** | Physical time units, marginally — it is where "declare a slow clock" belongs. Free by-product: P4's timing DAG falls out of P8's levelizer (`AMENDMENT.md:288-289`) | Per-arc `DelayModel` with min:typ:max, inertial delay, setup/hold, STA, SDC, interconnect delay, SAIF. **The parity contract explicitly permits all timing to differ.** Per-arc lookup *adds* per-event work to a loop already 47.7% bookkeeping / 4.9% logic | **0–3 of 23–35** |
| **P5** verification, proof & coverage | **PARTIALLY REQUIRED — the split is sharp** | **Report channel + exit-status lattice, 1 wk, Stage 1a** (`AMENDMENT.md:882`) — designed ONCE for five consumers; the parity verdict must be a sixth. **The `Stimulus` SPI that may post DURING the run**, 2–3 wk (`README.md:534-538`) — *the only designed event-injection mechanism in thirteen programs*. **Timestamp closure**, 2–3 wk (`sweep-04:72-122`) — `WireNet.propagate` overwrites eagerly, so a signal legitimately holds several values within one timestamp and a naive per-event sampler produces spurious divergences; `afterTimestamp(t)` is the only well-defined sync point. **Register-boundary key-point equivalence**, 2–3 wk (`lf-04:338-347`) — discharges a *boundary* obligation for all inputs, unboundedly | **The entire 20–30 wk formal capability — the amendment's own #1 pick.** The corpus refutes it itself: "unbounded sequential equivalence on non-matching encodings (delegate via BTOR2, always)" is listed under *where JLS cannot plausibly lead* (`AMENDMENT.md:759-761`), and M_H vs M_L have non-matching encodings by construction. Coverage, ERC, UCIS | **6–10 of 33–50** |
| **P6** silicon on-ramp | **IRRELEVANT** | Nothing. Cells-as-data is combinational-function-only and cannot carry a device or a behavioral CPU | All of it. A Linux SoC is orders of magnitude past Tiny Tapeout's one-tile / 8-in / 8-out / 8-bidir budget (`README.md:673-675`) | **0 of 20–32** |
| **P7** parameterization & elaboration | **BENEFITS — one REQUIRED piece** | **`jls.core.elab`** — "a real named elaboration phase running *before* the existing `Circuit.finishLoad`, so that **nothing in `jls.sim` changes**" (`AMENDMENT.md:240-242`). The only proposed home for loading a 16 MiB kernel image from a D1 sidecar. Parameterization lets one drawing be both the nommu and the Sv32 machine. Its frozen **total** integer expression language (no strings, loops, recursion or signal references — "a file whose meaning requires running a program is not a data format") transfers verbatim to any new optional section | **The `Array` element.** `SubCircuit.react` posts **one event per input port on every react**, so an `Array ×32` of a 5-port cell posts 160 boundary events per activation against 2 for a native `RegisterFile`. Use native parameterized elements, not Array-expanded hierarchy, until P8's elaborate-to-flat lands | **8–11 of 25–36** |
| **P8** the compiled engine | **REQUIRED — and the most contested** | **Mode T** (11–16 wk): levelize only the zero-delay closure, leave transport delay in the queue. Removes ~82% of queue traffic; "`ARCHITECTURE.md:359-368`'s equivalence criterion is satisfied as written, with no amendment", every golden byte-identical, and per `lf-02:338-339` not even a CLI flag. **≈1.43–1.67× at ZERO governance cost.** **Mode C** — the only lever in the roadmap with live-console *order of magnitude*, and the source of the `ESCAPE` opcode and the bidirectional nodeId↔element map a hybrid circuit needs. **But see §5-A4: Mode C may not beat the optimized interpreter on this workload** | Nothing meaningful. `--compare-engines` as a taught topic is a bonus; as an engine-vs-engine parity oracle it is directly reusable | **24–35 of 24–35.** Hard deps: **P1-S1**, **#77** |
| **P9** causal debug & time travel | **REQUIRED — and the roadmap's own ordering is BACKWARDS here** | **Checkpoint / restore FIRST.** The published useful floor is explicitly "journal + `--why`, **no checkpoints**" (`AMENDMENT.md:147`). Under this use case that inverts: every experiment that must re-boot costs 44 min – 1.7 h, a checkpoint at the shell prompt costs seconds, and a full-fidelity boot journal is **~67 GB** (infeasible) while the O(window) mode is ~580 KB. **`jls -b --why 'net@time'`** is the only proposed tool for "the boot diverged at instruction 12,345,678", built on an over-approximate `causalInputs()` that is exactly correct for 21 of 25 `react`s | GUI backward stepping; X-source tracing (no X in a boot); the unbounded journal | **13–20 of 19–27** — the inverse of the published floor |
| **P10** fault simulation & DFT | **IRRELEVANT** | Nothing | Stuck-at models, PODEM, scan, ATPG | **0 of 12–18** |
| **P11** semantic diff, merge & VCS | **BENEFITS — and it is where D1 and D2 are actually DELIVERED** | **Stage 0a, 1–1.5 wk** (`AMENDMENT.md:880`, "the cheapest real item in either sweep") — `-canon [file|-]`, a git **clean** filter (not smudge: the reader sniffs the container, so plain text *is* a valid `.jls`), `docs/version-control.md`, and fixing the `-b -savetext` silent no-op. **This IS D1.** **C1 FORMAT 3** diff-stable serialization inside the 9–13 wk diff half: refs/probes/pair-anchors carry stable ids, the positional `int id` line disappears. **This IS D2**, with a measured acceptance criterion — the one-gate-insertion diff falls from **5,314 lines to 9**, of which 5,227 are pure renumbering churn | Three-way merge, the git merge driver, `alu.MERGE.jls` conflict artifacts | **10–14 of 18–27** |
| **P12** programmatic API & platform | **REQUIRED — and it contains the study's single biggest conflict** | **`Run`/`Session` with a step verb.** `riscv/build/k2000_clk.txt` is **193,040 measured bytes to express `advanceCycles(2000)`** (`AMENDMENT.md:502-503`), so clocking a 1.16e8-cycle boot needs **~11 GB of `-t` text** — the format cannot express a boot at any parser speed. **`Catalog` + `Edit`-over-`CircuitOp`** is D5's programmatic half, and the algebra already ships (20 files, #167, atomic validate-then-mutate, exact inverses, "it cannot construct a circuit the editor could not") | The generator-library framing alone; procedurally generated assignments | **8–11 floor of 19–29.** Hard dep: **#77, with no escape** |
| **P13** clock, reset & domains | **MOSTLY IRRELEVANT — one small REQUIRED piece** | **C1's `Clocked` capability.** The amendment names three private edge detectors to consolidate; **there are now four** (`RegisterFile.currentC`, `RegisterFile.java:450`). This is the only place M_H/M_L reset and start-state consistency can be made uniform — which matters because JLS silently supplies a reset the design does not have | Clock phase, domain inference, CDC checks, `Synchronizer`, seeded metastability. **A Linux SoC is one clock domain** — and Mode C *refuses* more than one `Clock` with incommensurable periods anyway | **2–4 of 13–18** |

### Roll-up

| bucket | programs | weeks |
|---|---|---:|
| REQUIRED | P1(S0+S1), P2, P5(partial), P8, P9, P12(floor) | **72–105** |
| BENEFITS | P3(partial), P7(elab), P11(0a+C1), P13(C1) | **27–41** |
| IRRELEVANT / COUNTER-INDICATED | P4, P6, P10, P5's formal half, P1-S2…S5 | *(95–140 skipped)* |
| **Roadmap total needed** | | **~105–159 of 288–424 (≈37%)** |
| **#77** — headless core extraction | on the critical path of **four** programs | **UNPRICED ANYWHERE** |
| **Unowned (§2)** | P14–P20 | **+47–79** |
| **HONEST TOTAL FOR THE USE CASE** | | **~152–238 weeks ≈ 35–55 maintainer-months** |

At bus factor 1, under 93.0/92.0/84.5% JaCoCo package aggregates and 80/82 PIT
thresholds on all new code, that is **three to four and a half maintainer-years**
for the full objective. §3.4 gives two much smaller viable subsets.

---

## 2. THE UNOWNED CAPABILITIES — P14 … P20

These are what the use case needs that **no** program covers. They are the
genuinely new work this study contributes. Written in the roadmap's own idiom:
what it delivers, cost band, minimum useful version, dependencies.

Cost bands here are **analogies against shipped work**, on the same basis the
roadmap uses (#78's registry, #166's canonical save, #167's op layer, #199's
synchronous memory, #201's two elements at 1,188 insertions / 14 files). They
are not measurements. Say so wherever they are quoted.

---

### **P14 — Host I/O and the device element** ⭐ *the one that cannot be worked around*

**Delivers.** A first-class device concept: an in-tree `LogicElement` subclass
whose `react` exchanges bytes with a host resource, plus the seam, lifecycle and
injection contract that make it legal.

Four parts:
1. **`app.device-provider` extension-point row** in `docs/extension-points.md`
   (id, contract type, cardinality `many`, lifecycle phase), with an owning
   issue — filed **before** any code, per the catalog rule.
2. **A `DeviceModule`** with `Activation.OnCommand("device.console")` publishing
   device descriptors into the shipped `ExtensionRegistry`. Cold plane only.
3. **In-tree device elements**: `Uart16550` (THR/RBR/LSR — 3 byte addresses) and
   `ConsoleIn`/`ConsoleOut`. Hot plane, core, inside the seal, on the #201
   pattern. Measured tax: ~65 lines of registration + the class.
4. **The injection contract.** Under the interpreter: a host thread fills a
   non-blocking byte queue; the element drains it at `Simulator.beforeEvent`,
   the *only* thread-correct slot (`post()` is unsynchronized over a plain
   `PriorityQueue` with a single-thread contract). Under Mode C: a **pre-settle
   input-plane write at a clock-step boundary** — which is *cleaner*, because a
   clock-step boundary is a defined quiesce point.

**Why nothing covers it.** `sweep-05` is the corpus's designated home for
"system and interfaces" and contains zero device concept, zero host resource,
zero character stream, zero interactive input. Its own "what genuinely stays
out, and why" section enumerates 20+ exclusions and a host character stream is
**not among them** — not declined, not deferred, not costed. Absent. The
nearest mechanism, P5's `Stimulus` SPI, is scoped to seeded constrained-random
generation.

**Governance is the hard part, not the code.** The repository is converging on a
documented, permanent, **three-place** prohibition of exactly this:
`docs/vcd-interop.md:19-24` (#63), #222's out-of-process trust boundary, and
P12's proposed "**no callback direction, ever**, written into the normative
document as **permanent**" (`AMENDMENT.md:518-523`).

**The way through is already written, by `lf-07:314-336`, and it should be
adopted verbatim.** #63 rejects *JLS as a guest in a foreign time wheel* — two
event queues, a foreign scheduler owning `now`. A console is the **opposite
direction**: JLS owns the clock, the client asks JLS to advance and then reads,
one event queue, one owner of `now`, through the pause/step hook the GUI has
used since `beforeEvent` was written. `vcd-interop.md` is **informative** and
needs one paragraph; `docs/batch-interface.md` is normative and is untouched.

**Cost band: 10–16 weeks.**
**Minimum useful version: 4–6 weeks** — one `ConsoleOut` element draining a
watched net to `System.out` on a rate-limited channel, plus a `ConsoleIn`
element polled at `beforeEvent`. That alone gives a headless boot a visible
kernel log, which is the first real milestone.
**Depends on:** the #220/#224 integration slice (the registry is populated but
**nothing reads it for dispatch yet** — a device module today would boot and be
invisible); P2's byte lanes (a 3-byte-address UART is not addressable without
them); *benefits from* P12's `Session`, but does not require it.
**Blocks:** everything interactive. There is no substitute anywhere.

---

### **P15 — Simulation state as a versioned format, and bulk images**

**Delivers.** The other half of P9. P9 designs checkpoint/restore as an
in-memory-and-to-a-packet capability; **nothing owns the file format, and
nothing owns loading a kernel image at all.**

1. **`SimCheckpoint`** as a D3-shaped **OPTIONAL, internally versioned section**
   — a reader that knows nothing about checkpoints still opens the circuit
   structurally with a clean diagnostic. Sorted everywhere, so two checkpoints
   of identical state are byte-identical, which is D2's discipline applied to
   state. P9 supplies the layout (`AMENDMENT.md:348-352`, `lf-03:287-305`).
2. **The bulk-image sidecar** (D1's second row): kernel and RAM images as a raw
   sidecar, never diffed, outside `MAX_CIRCUIT_TEXT_BYTES`. **Required**: at the
   measured 15.87 B/word a 16 MiB RAM image is ~66 MB of text and **alone
   exceeds the 64 MiB cap** before any circuit content.
3. **The loader**, at P7's `jls.core.elab`, before `Circuit.finishLoad`, so
   nothing in `jls.sim` changes.
4. **Content init for `RegisterFile`**, which today has no `init`, no `file` and
   no write-back — the blocker on the Harvard-main-memory path.
5. Two additions P9 does not have: an **input-log cursor** record
   (`inputLogId`, `byteOffset`, `nextInjectionRetirementIndex`) and a
   **retirement index** alongside `now`, because sim time is a *permitted*
   M_H/M_L divergence and the same input log must replay into both tiers. Plus a
   **PINNED** flag exempt from P9's retention ladder, or the boot snapshot is
   evicted by the very policy meant to protect it.

**Scale correction P9 explicitly declines to make.** Its ≈9 KB / 2–3 KB deflated
figure is for three `Memory` elements at 256×32, and it says "do not build for a
scale that will never arrive". Re-derived for a nommu machine: 16 MiB RAM +
present bitmap + puts + nets ≈ **17.3 MB raw, ~3–8 MB deflated** — still 3.8×
better than the 66 MB text form, and a separate file never sees the cap.

**Cost band: 8–14 weeks** on top of P9's 5–7 wk capture/restore.
**Minimum useful version: 3–5 weeks** — a named, pinned, whole-state checkpoint
file plus a raw sidecar for `Memory`/`RegisterFile` init. No retention ladder,
no journal.
**Depends on:** P1-S0 (the `static long` sequence counter — "on the critical
path of exactly one thing in the roadmap, and this is it"); P9's element pass
(schedule it **inside** P1's element pass; both walk the same 25 `react` / 28
`initSim` implementations, and "doing them six months apart is precisely the
mistake keystone C warns about"); P7's `jls.core.elab`; D3.
**Non-negotiable acceptance criterion, from P9's own risk 3:**
`replay(ckpt[i]) == ckpt[i+1]` **byte-identically**, in CI. *One* uncaptured
mutable field produces a replay that is almost right — and the enumeration is
already stale twice over (`TruthTable.toBeValue` is an eighth `toBeValue`;
`RegisterFile.words`/`currentC` are in no row). **Gate on the property test, not
on reviewing a list that decays every time an element ships.**

---

### **P16 — The retirement-indexed parity harness**

**Delivers.** The comparison alphabet of `BRIEF.md` §6, as a running artifact:
one record per retired instruction, `{order, pc_before, pc_after, insn_word,
rd_index, rd_value, mem_addr, mem_rmask, mem_wmask, mem_wdata, privilege,
trap}`, emitted by both M_H and M_L, plus sync-point architectural dumps, plus a
differential comparator, plus a verdict lattice.

**Why nothing covers it — and this is the sharpest gap in the study.**
`grep -ril "rvfi\|rvvi"` over the whole tree returns **nothing**. Every
comparator in the repository is **end-state only**: RISCOF diffs a signature
region once at the end; `riscv/verify.py:66-77` diffs final `x1..x31` and final
`dmem`. In `BRIEF.md` §6 terms the entire in-tree verification culture is item 2
with *sync quantum = the whole run*, and item 1 entirely absent. **Nothing in
JLS knows what an instruction is, and no program proposes to teach it.**

Cite the corpus against itself: `05-riscv-compliance.md:355-366` names its own
top failure mode as "a silently wrong signature rather than a crash", and a test
that "then passes or fails for a reason unrelated to the ISA". **Over 4.0e7
instructions, attribution IS the job.**

**Four components.**
1. **An observation hook, already shipped twice.** `Simulator.afterEvent`
   (`:269`) and `probeSample` (`:285`) are empty no-op hooks that
   `BatchSimulator` already overrides for VCD, both opening with
   `if (!JLSInfo.printTrace && vcdFileName == null) return;`. A per-retire
   recorder is a **third subclass**, needing zero core edits and zero new seams,
   with the identical one-predictable-branch gate. This makes the observation
   half materially cheaper than `BRIEF.md` §7 implies.
2. **A retirement observable on M_L.** Sampled at `afterTimestamp` (P5), not
   per-event. `RegisterFile` must first be added to the batch watched-element
   whitelist — *the architectural register file cannot print today.*
3. **The comparator and its verdict lattice**, on P5's report channel:
   `PARITY_HELD | DIVERGED_AT_INDEX_N | UNKNOWN(budget exhausted) |
   NOT_COMPARABLE(model lacks an observable)`, with **"UNKNOWN and
   NOT_COMPARABLE are never passes"** and a **simulate-to-confirm** rule (a
   reported divergence must be reproduced by simulation before it is believed).
   Both patterns lifted verbatim from `lf-04:396-408, 313-318, 766-775`.
4. **An independent external golden.** `05-riscv-compliance.md:172-178` forbids
   the exact structure `BRIEF.md` §6 is built on: using JLS's own oracle as the
   reference "would make the whole exercise circular". **`BRIEF.md` §6's
   single-source `D` is right for reproducibility and wrong for soundness.** The
   contract needs a third object sharing no authorship with `D` — Sail, or the
   instrumented `mini-rv32ima` that produced `N_instr = 4.0e7`.

**Two rules to write into the contract before any code.**
- **Retirement-boundary sampling.** An X (or a settling value) that resolves
  before the sync point is permitted microarchitectural state; one surviving
  into a committed `rd_value`/`mem_wdata`/`pc_after`/`trap` is a real defect.
  Without this, the first four-state parity run produces a flood of spurious
  mismatches.
- **Sync point zero.** JLS supplies a reset the design does not have
  (`initInputs` zeroes every input at every depth; `Register.initSim` drives the
  configured init). Two machines can agree on every record from instruction 1
  and disagree at instruction 0. Require `D` to specify the power-on state of
  every architecturally-visible register.

**Cost band: 10–16 weeks.**
**Minimum useful version: 4–6 weeks** — an RVFI emitter on M_H, a sync-point
architectural dump on M_L, a line-oriented comparator, and the four exit
statuses. No bisection, no coverage.
**Depends on:** P5's report channel (**1 week, and the window is open now** —
it is being designed once for five consumers and reopening it is the failure two
independent sweeps name); P5's timestamp closure (a *prerequisite*, not a
nicety); P15 (bisection to a divergence at instruction 3.7e7 is impossible
without restore); H.3 (`RegisterFile`'s missing behavioral golden).
**Shared deliverable:** this **is** the replacement for `riscv/verify.py` that
P1's migration also loses to D5. **Fund it once, for both.**

---

### **P17 — The per-subcircuit fidelity toggle**

**Delivers.** D4's fourth direction-of-travel item, and the *only* one nobody
owns. Checked against all thirteen programs:

| D4 item | owner |
|---|---|
| nested / shared / parameterized subcircuit definitions | **P7** ✓ |
| compiled backends, Java-level self-contained preferred | **P8** ✓ (and "self-contained at the Java level" matches the maintainer's wording exactly) |
| the in-program definition of elements gets expanded | **P2** ✓ (+ P6 partially) |
| **a switch to toggle a subcircuit between full-fidelity structural operation and a compiled/optimized implementation** | **NOTHING** |

P8's Mode T/Mode C is a **whole-circuit engine** toggle and it *flattens
hierarchy by construction* — "every `SubCircuit` is inlined; `inmap`/`outmap`
become net-identity merges", and deleting the boundary **is part of the win**.
P8's `ESCAPE` opcode is an interpreter fallback for unsupported opcodes, not a
fidelity choice. P6's cells-as-data is combinational-only.
`grep -rn 'fidelity|per-subcircuit'` across all 21 roadmap documents returns
**nothing**.

**Exactly one of D4's four items is unowned, and it is the one the maintainer
adopted because "it makes parity a property of a BOUNDARY, which is what makes
it testable."**

**The mechanism, which is one paragraph from what `lf-02` already specifies.**
A subcircuit pinned to full structural fidelity **is an `ESCAPE` node** whose
shim body is a nested event-driven evaluation of that subcircuit's contents. The
pin is an elaboration-time instruction: *do not flatten this instance; emit it as
`ESCAPE`*. The cost is exactly the boundary tax `lf-02` quantifies at
`SubCircuit.react:621-636` — hash lookup, `BitSet` clone, queue insert, dedup
insert, poll, dedup remove, **per input pin per crossing** — paid only at pinned
boundaries. Cold-plane selection through a seam with cardinality "one active"
(precedent `gui.theme`); hot-plane evaluation in core, per
`grand-architecture.md` §6.

**Discharge mechanism, supplied by P5 and unrecognized by it.**
Register-boundary (key-point) equivalence (`lf-04:338-347`): cut both
implementations at matching register boundaries and prove next-state and output
functions equal combinationally — *for all reachable and unreachable states,
unboundedly, with no new machinery.* **So parity is PROVED at subcircuit
boundaries and CO-SIMULATED at the machine level.** That two-layer framing is
the study's answer to "how does the industry get cross-abstraction confidence",
and it is the only form the corpus can actually support.

**Cost band: 6–10 weeks.**
**Minimum useful version: 3–4 weeks** — a saved per-instance boolean plus an
elaborator that honours it, with the compiled side stubbed to the existing
interpreter (so the toggle is testable before P8 exists).
**Depends on:** P3's reuse identity (**hard** — you cannot toggle "the UART" if
there is no "the UART", only *n* inline copies); P8's elaborator and `ESCAPE`.
**Open and undesigned by anyone:** what a **mixed-fidelity checkpoint** contains
when one subcircuit runs on the interpreter (event granularity) and another
compiled (timestamp granularity), and whether a `--why` walk can cross that
boundary at all.

---

### **P18 — Long-run ergonomics and the multi-hour CI lane**

**Delivers.** The things that decide whether a 44-minute-to-4-hour run can be
*run, resumed, bounded and gated* at all. Individually trivial; collectively
fatal if none is picked up.

1. **The time-limit model.** `JLSInfo.defaultTimeLimit = 100000000` is
   **1,920–2,300× short** of a boot at 2,000 time units per cycle.
   `Simulator.maxTime` is a `long`, so the value fits; the default and the CLI
   path both need changing.
2. **`maxTime` overflow semantics.** The head event is polled, **removed from
   `dupCheck`**, then discarded without reacting or re-queueing. `BRIEF.md` §10
   left this open as a curiosity; **under a resume contract it is an
   unambiguous blocker**, because a checkpoint taken after termination has a
   queue missing a dropped event and `replay(ckpt[i]) == ckpt[i+1]` cannot mean
   anything. Fixing it changes observable semantics and therefore needs a
   `docs/simulation-semantics.md` change under #221's process clause.
3. **A progress / resume contract** for headless long runs.
4. **The two-lane CI split**, adopted verbatim from `05-riscv-compliance.md:261-273,
   368-376`: an **offline golden lane on every PR** (needs only the jar, ~2 min,
   the blocking gate) plus a **nightly external/long lane that is informational
   only** — with that document's own warning attached: *"A nightly lane that
   depends on unpinned upstream will go red for reasons that are not JLS's, get
   muted, and then guard nothing."*
5. **A large-fixture policy.** No `timeout-minutes` anywhere today, no Git LFS,
   no policy. A 2.4 MiB kernel is a 33 MB `.jls` at 15.87 B/word — and under D1
   it is uncompressed text.
6. **Batch `InteractiveSimulator`'s per-event trace/probe work** through the
   rate-limited channel `grand-architecture.md` §6 already requires and which is
   implemented for exactly one thing (the 50 ms clock label). **Binding for a
   live console, because the console runs on that engine, not `BatchSimulator`.**

**Cost band: 3–5 weeks.** **Minimum useful version: 1–2 weeks** (items 1, 2, 4).
**Depends on:** P15 for resume; P5's report channel for the verdict.

---

### **P19 — The in-tree CPU-scale calibration fixture** ⭐ *blocks D5*

**Delivers.** A first-class, tracked, tested, CI-runnable CPU-scale circuit plus
its benchmark harness and golden, replacing four things at once:
`riscv/build_cpu.py` (the generator), `riscv/verify.py` (the differential
oracle), `riscv/bench_kernel.py` (the performance harness), and
`riscv/build/k2000.jls` (the fixture — **which is not even tracked**;
`riscv/.gitignore` line 1 is `build/`).

**Why this is not optional.** `riscv/bench_kernel.py:4-9`, verbatim: *"This is
the largest real simulation workload in the tree… It produced the kernel
measurements in `docs/capability-roadmap/keystone-c-performance.md`; **re-run it
before and after any such change rather than trusting those numbers.**"*
`AMENDMENT.md:996-998` confirms that keystone C's benchmarks, `lf-02`'s re-run
of `bench_kernel.py`, `lf-06`'s 5,314-line diff and `lf-07`'s file-size figures
are **the only measured numbers anywhere in either sweep**.

D5 deletes all of it. Consequences, all real:
- **P1's Stage-5 acceptance criterion** ("a 15–25% faster event loop") loses its
  baseline.
- **P11's C1 acceptance criterion** (5,314 → 9 lines) becomes **unreproducible**:
  its fixture is untracked and its generator is deleted.
- **P12's whole-program criterion** ("no save-format string literal appears
  anywhere in `riscv/`") becomes **vacuously satisfied by `rm -rf riscv/`.**
- Both keystones' migration oracle (`riscv/verify.py`'s differential fuzzing)
  and keystone-b's stage-7 gate evaporate.
- The study's own golden oracle problem (34 simulated cycles, 4 assertions,
  gitignored, never run by CI) is the *same* deliverable.

**Cost band: 4–8 weeks.** **Minimum useful version: 2–3 weeks** — a tracked
`test/fixtures/` CPU circuit, a `test/jls/` benchmark that emits the same
counters keystone C reports, and a golden with more than four assertions.
**Depends on:** P12's `Edit`/`Catalog` (D5's stated authoring replacement:
"delete `riscv/jlsbuild.py`'s role as a producer and draw the CPU") **or** P7's
parameter half. Its comparator half is **P16**.
**Ordering: this must land BEFORE `riscv/` is removed.** It is cheap, it is
fully D5-compliant, and it serves the roadmap at least as much as it serves the
study. **This is the highest-goodwill item the study can offer the maintainer.**

---

### **P20 — The behavioral machine tier (M_H)**

**Delivers.** The virtual-hardware half of the parity contract: a behavioral
macro-element whose `react()` is an ISA interpreter — `BRIEF.md` §4's ~10-element,
~12-ev/instr, **2.5-minute, interactive** tier.

**Why nothing covers it.** P2 is the vehicle (a new `LogicElement` subclass) but
does not scope it; no program in the roadmap contemplates an element whose body
is an instruction-set interpreter. The corpus's element-cost rate (~1.5 wk for
the first element including the plumbing pattern) is calibrated on a
`Multiplier`, not on RV32IMA + CSRs + traps.

**Why it is the load-bearing half.** Under the corrected table
(`BRIEF-DELTA.md` §C) the behavioral row is **the only row that is interactive
today**, and it is **the only row the entire 2.26–4.9× optimization program does
not improve** — at ~10 elements its cost sits in the 4.9% "react bodies' own
code" bucket, and under a compiled engine it is a single `ESCAPE` node. That
asymmetry is why the two-tier framing survives §D's re-derivation: **all the
engine work accrues to the structural tier, and the behavioral tier is already
fast.**

**Cost band: 6–10 weeks.** **Minimum useful version: 3–4 weeks** — RV32I +
`Zicsr` + M-mode traps only, no A extension, no Sv32; enough to boot nommu Linux
with the same memory map as M_L.
**Depends on:** P14 (it needs the same device elements); P16 (its RVFI emitter
is trivially cheap — it *knows* what an instruction is, which M_L does not).
**Design constraint:** it must be built to reproduce whatever reset fiction M_L
depends on, or sync point zero cannot hold. See P16.

---

### Unowned roll-up

| | delivers | cost | floor | hard deps |
|---|---|---:|---:|---|
| **P14** host I/O + device element | the console; the only irreplaceable item | 10–16 | 4–6 | #220/#224 integration; P2 byte lanes |
| **P15** state serialization + bulk images | checkpoint/resume; kernel loading | 8–14 | 3–5 | P1-S0, P9, P7-elab, D3 |
| **P16** parity harness | the RVFI record stream; replaces `verify.py` | 10–16 | 4–6 | P5 report channel + timestamp closure; P15 |
| **P17** per-subcircuit fidelity toggle | D4's one unowned item | 6–10 | 3–4 | P3 reuse identity; P8 ESCAPE |
| **P18** long-run ergonomics + CI | makes a multi-hour run runnable and gated | 3–5 | 1–2 | P15 |
| **P19** in-tree calibration fixture | unblocks D5; restores the roadmap's own baselines | 4–8 | 2–3 | P12 `Edit` or P7 |
| **P20** behavioral machine tier (M_H) | the interactive half of parity | 6–10 | 3–4 | P14, P16 |
| **total** | | **47–79** | **20–30** | |

---

## 3. THE RE-ORDERING — *a proposal to the maintainer, not a decision*

Nothing in this section is settled. Every item is a request to reconsider a
published ordering in the light of one specific use case, and the maintainer may
reasonably decline any of it — the roadmap's ordering function (competitive
position for a teaching tool) is a legitimate one and this study does not
override it.

### 3.1 Moves UP

| what | from | to | why |
|---|---|---|---|
| **P11 Stage 0a** | a small item inside P11 | **first, week 1** | 1–1.5 wk, delivers **D1 outright**, and the corpus calls it "the cheapest real item in either sweep". Nothing depends on it and it unblocks every diff-based comparison. |
| **P5's report channel + exit-status lattice** | one row inside a 33–50 wk program | **week 2, Stage 1a** | **1 week, five consumers, and the window closes.** A parity verdict must be a sixth consumer *within that week* or the study reopens a stability promise later — precisely the failure two independent sweeps name. |
| **P1-S0** | stage 0 of a 28–36 wk program | **immediately after** | 2–3 wk. The per-`Simulator` sequence counter is the hard prerequisite for deterministic replay, which the parity contract assumes. Also kills `SigSim`'s 0.57 s setup. |
| **P2's `Memory` byte lanes** | one change among many in P2 | **a hard Linux blocker, before anything else in P2** | Without `sb`/`lb` there is no 16550 driver and no Linux. The corpus records the cause; nobody connected it to a boot. 3–7 wk, "the cheapest change in this sweep with the highest ratio of unlocked standards to weeks." |
| **P9's checkpoints** | *explicitly excluded* from P9's own useful floor | **P9's first deliverable** | The published floor is "journal + `--why`, **no checkpoints**". Under this use case the unbounded journal is **infeasible** (~67 GB) and checkpoints are the load-bearing half: every re-boot costs 44 min–1.7 h; a snapshot at the shell prompt costs seconds. |
| **P8** | ranked low; Mode T explicitly rejected as a top pick because "speed is precisely the axis on which JLS cannot lead" | **the deciding program** — *with §5-A4's caveat* | It is the only lever in 288–424 weeks with live-console order of magnitude, and Mode T is a **1.43–1.67× win at zero governance cost**. |
| **#77** (headless core extraction) | an unnamed background prerequisite | **the critical path of four programs (P1-S5, P7-elab, P8, P12), and unpriced by every document** | "P12 has no such escape: its entire content is the core, addressable." `src/jls/core/` still holds eight files and all are geometry. **Someone must price this.** |
| **P19** (new) | — | **before `riscv/` is deleted** | Otherwise D5 destroys the reproducibility of every measured number the roadmap has. |

### 3.2 Moves DOWN

| what | from | to | why |
|---|---|---|---|
| **P5's formal capability, 20–30 wk** | **the amendment's own #1 recommendation** | **skipped entirely for this objective** | The corpus refutes it itself: "unbounded sequential equivalence on non-matching encodings (delegate via BTOR2, always)" is listed under *where JLS cannot plausibly lead*, and M_H vs M_L have non-matching encodings by construction. The formal floor is combinational-only over gates. **Retain only register-boundary key-point equivalence (2–3 wk) for P17.** *The corpus refuting its own top pick is much stronger evidence than the study asserting it.* |
| **P1-S2…S5, 14–17 wk** | the spine of the roadmap ("this IS the program") | **skipped; S4 actively opposed** | A booting kernel is fully-driven and synchronous. Strength never reaches a sink. **S4 removes the free reset the RV32 PC depends on and neither cluster document notices.** The study's ask on P1 is a *negative guarantee*: V1–V8 opt-in, default-off, **permanently** for the boot configuration — not the one-release courtesy the corpus offers. |
| **P4, P6, P10 — 55–85 weeks** | three whole programs | **zero** | Parity permits all timing to differ; a Linux SoC is orders of magnitude past Tiny Tapeout; stuck-at faults have no bearing on booting. |
| **P7's `Array` half** | half the program | **skipped until P8 flattens** | `SubCircuit.react` posts one event **per input port per react**. An `Array ×32` of a 5-port cell posts 160 boundary events where a native element posts 2. `lf-01` recommends the floor on *cost* grounds and is unaware of the *event-count* grounds, which are stronger. |
| **P11's merge half, 9–14 wk** | half the program | **contingent** | The diff half is worth doing regardless (grading, CI, regression triage). The merge half depends on whether `.jls` files are shared under version control at all — a question only the maintainer can answer. |
| **Mode C relative to the semantics-preserving stack** | "the order of magnitude lives here" | **decide after measuring alpha** | See §5-A4. This is the largest single re-ordering claim in this document. |

### 3.3 The proposed sequence

```
week 1        P11 Stage 0a            D1 delivered.
week 2        P5 report channel       + the parity verdict, in the same week.
weeks 3–5     P1-S0                   deterministic replay prerequisite.
weeks 3–9     P19 floor               in-tree fixture, BEFORE riscv/ dies.        [parallel]
weeks 5–12    P2 byte lanes           the hard Linux blocker.
weeks 6–10    P5 Stimulus SPI         event injection + a clock-step verb.        [parallel]
weeks 8–14    P14 floor               ConsoleOut/ConsoleIn + the seam row.
weeks 10–16   P20 floor               M_H, the behavioral machine.                [parallel]
weeks 12–18   P15 floor + P7 elab     checkpoint + the kernel-image sidecar.
weeks 14–20   P16 floor               the parity harness.
weeks 16–22   P2 Mul/Div + CSR/trap   RV32IMA and privileged mode.
weeks 18–20   P18                     time limits, maxTime, the CI lanes.
─────────────  MILESTONE: headless nommu Linux boots, parity-checked, in CI.
              ~32–47 weeks.  ALPHA IS NOW MEASURED.
─────────────  DECISION POINT on P8 (§5-A4), taken with data.
then          P1-S1 (12–16) · #77 (?) · P8 (24–35) · P3 reuse identity (3–6)
              · P17 (6–10) · InteractiveSimulator batching (1–2)
─────────────  MILESTONE: live console, ~1.2–5× short of comfortable.
              ~+40–60 weeks, and NOT GUARANTEED TO ARRIVE.
```

### 3.4 Two much smaller subsets, if the full objective is too large

- **"Headless parity" (~32–47 weeks).** Everything above the first milestone.
  Delivers a booting structural machine, a behavioral machine, a per-retire
  parity contract that runs in CI, checkpoint/resume, and a measured `alpha`.
  **It also delivers P19, which the roadmap needs regardless of this study.**
  This is the recommended target.
- **"The parity contract alone, no boot" (~14–20 weeks).** P5 report channel +
  timestamp closure, P16, P19, P1-S0. Delivers a retirement-indexed differential
  comparator against an external golden, replacing `riscv/verify.py` for the
  value-domain migration too. **Serves P1's migration gate and the study's
  contract with one artifact.** Nothing about Linux; everything about parity.

---

## 4. THE CONVERGENCE EVIDENCE

Independent convergence is the strongest evidence either body of work can offer,
because the roadmap sweeps and this study's 28 recon agents were answering
different questions with different instruments and never spoke.

1. **388.4 vs 386–409 events per clock cycle.** The study: four independent
   instrumented counters. The corpus: a JFR profile plus a payload census on a
   different vector length (`keystone-c:104-137`). Dead centre. **This is now
   the most robustly triangulated number in either body of work, and the boot
   arithmetic of `BRIEF.md` §3–§4 can be treated as measured ground.**
2. **47.7% vs ~48% event-queue bookkeeping.** Different instrumentation, and the
   convergence is stronger than it looks: the corpus establishes that JFR's
   default `stackdepth=64` mis-attributes ~30% of JLS's samples. Two
   attributions that *could* have diverged by 30% agreed to 0.3 points.
3. **The interactivity floor, derived twice from unrelated starting points.**
   The study: 1e5–1e6 cycles/s, from a tty echo path of 1e4–1e5 instructions.
   The corpus, arriving via P8's unlock list: "interactive projects — VGA
   timing, a pong paddle, **a UART echoing keystrokes** — that need ~25,000
   clocks per frame" (`AMENDMENT.md:310`) = 7.5e5–1.5e6 clocks/s at 30–60 fps.
   Same decade. **The corpus's own example is literally a UART echoing
   keystrokes.** This makes the requirement citable rather than assumed.
4. **#221's revisit trigger is met, and the corpus supplies the numeric form the
   study lacked.** `README.md:1009-1014`: the trigger "is not a testable
   condition and nobody will ever agree it has been met… Restate it
   quantitatively, e.g. below 10 kcycles/s on the #202 golden's CPU." Its own
   measurement two sections earlier is **8,090 cycles/s**. The follow-up issue
   can now cite an in-repo document rather than the study alone.
5. **`beforeEvent()` is the only legal slot — reached twice, for two different
   reasons.** The study: it is the only thread-correct *injection* point. `lf-03`:
   checkpointing must happen *between events* to stay inside
   `grand-architecture.md`'s hot-plane rule, so it is the only legal *capture*
   point. **They are the same slot**, so a console byte and a checkpoint are
   trivially ordered against each other with no edit to `runEventLoop`.
6. **The differential oracle is the same clause, chosen independently.**
   `ARCHITECTURE.md:359-368` binds any future simulation strategy to bit-for-bit
   agreement with the #202 RV32I golden. The study reached for it as the parity
   gate; keystone-a reached for it as the value-migration gate ("the gate for all
   of this already exists"). **Two unrelated programs selecting the same oracle
   is the strongest possible endorsement of `BRIEF.md` §6's framing.**
7. **The element census, to the type.** `sweep-05:25-29` independently counted
   `test/fixtures/riscv-sum1to10.jls` at 1,038 ELEMENT records = 810 WireEnd +
   228 real elements with a full type breakdown — exact agreement with the
   study's "228-logic-element single-cycle RV32I".
8. **"Spend element-library effort, not circuit effort" — and the maintainer
   then did it, twice, in one commit.** The study's §4 principle; #201 shipped
   `RegisterFile` and `FieldExtend`, collapsing ~95 elements into one class in
   569 lines. **The recommendation stopped being a proposal and became a
   description of the maintainer's own move.**
9. **Deep copies / 1.00× sharing, from three directions.** The study measured it;
   `lf-01` reached it from three distinct copy sites (`SubCircuit.copy`,
   `SimpleEditor.doImport`, `Circuit.loadElementItems`); `lf-06` reached it from
   the save/load side. And `hneemann`'s Digital — verified by reading
   `ResolveGenerics.java` — **also deep-copies**, independently corroborating
   that elaboration does not buy sharing.
10. **A checkpoint is naturally an OPTIONAL per-section-versioned section.** D3
    anticipated it (`BRIEF.md:332-334`); P9 arrived at the same shape
    independently. **D1 + D3 + P9 compose without friction — the cleanest
    three-way agreement anywhere in this phase.**
11. **"Design the report channel and exit-status lattice ONCE."** `lf-06:738-743`
    and `lf-07:534-540` insist on it independently, in different programs, in
    the same words. The study should join that conversation rather than open a
    fourth.
12. **The prohibition on live co-simulation is a real, two-document, hardening
    position — not a stale line.** The study flagged it as an in-repo
    contradiction; the corpus proposes to *strengthen* it. Convergence on the
    fact, disagreement on the direction — which is what makes §5-A5 worth
    resolving before anyone writes code.

---

## 5. THE DISAGREEMENTS — named and adjudicated

### A. Between this study and the corpus

**A1. Engine throughput: 2.0–2.6 M ev/s vs 3.14 M ev/s.**
**→ The corpus wins, with a scope label.** It separates `initSimulation` from
`runEventLoop`, names the harness, validates the oracle on the same jar, and
best-of-8s. The study's band is exactly reproduced by folding elaboration back
in. **Adopt 3.14 M ev/s (318 ns/event) warm; keep 2.0–2.6 M as the
including-elaboration figure and always say so.** Not really a disagreement —
the study's spread explained.

**A2. Whether a live console is possible on a structural model.**
**→ The study's claim is NARROWED, not killed, and two corpus digests
over-corrected.** See `BRIEF-DELTA.md` §D. Reconciled: ~19.5 kcycles/s today,
~44 k after the semantics-preserving stack, ~96 k after the full stack,
22–86 k under Mode C, against a 1e5–1e6 requirement. The blockquote must be
scoped to "the discrete-event interpreter's constants". **`new-roadmap-core`'s
385 kcycles/s is wrong by ~4.6× (600 logic elements counted as nodes; one pass
counted instead of two) and must not be inherited by any downstream phase.**

**A3. Mirrored-`Memory` register file at ~12 ev/cycle.**
**→ The study's own earlier estimate loses to this session's measurement:
18.00 ev/cycle**, 33% optimistic. Cause: `Memory.react` posts a self-event at
`now+accessTime` on *every* inbound `PinChanged`, unconditionally.

**A4. Whether Mode C is the deciding program.** ⭐ *the biggest one*
The corpus's own progression table shows the fully optimized **interpreter**
reaching **~86–96 kcycles/s** while the **cycle-based mode** it recommends for
interactivity lands at **22–40 kcycles/s** after its own double derating.
**→ Adjudication: Mode C is not obviously faster on this workload, and the
mechanism is `alpha`.** Mode C settles every node every cycle — alpha ≡ 1 by
construction. The interpreter touches only active elements, and a multi-cycle
CPU has alpha 0.18–0.56. **Mode C wins on dense designs and loses on sparse
multi-cycle ones, and a multi-cycle CPU is the sparse case.** The activity
bitmap (`lf-02:234`, declared and never explained) is the only thing that would
change this and nobody has specified how Mode C uses it.
**Consequence: `alpha` is not merely the dominant boot-time uncertainty — it is
the decision variable for a 24–35-week engine program.** Build the
semantics-preserving stack first (2.26×, no governance cost), measure alpha on a
real multi-cycle machine, then decide about Mode C. **Neither the study nor any
single digest reached this; it falls out only of reconciliation.**

**A5. Live co-simulation: reopen #63, or route around it?**
The study said "#63 must be explicitly reopened or reconciled". `sweep-04`
proposes to *strengthen* the prohibition; P12 would make it permanent.
**→ `lf-07`'s framing wins and should be adopted verbatim: #63 rejects JLS as a
guest in a foreign time wheel; a console is the opposite — JLS owns `now` and
the client steps it.** That turns a governance fight into one paragraph of an
*informative* document. **But the study must contest the "no callback direction,
ever" wording NOW.** It is cheap to contest before P12 is specified and
expensive after: reopening a normative permanent rule costs a documented
decision reversal on top of #63 and `vcd-interop.md`.

**A6. `BRIEF.md` §6's single-source `D`.**
**→ The corpus is right and the study must amend.**
`05-riscv-compliance.md:172-178` forbids exactly this structure: an oracle that
shares authorship with the design "would make the whole exercise circular". A
parity pass between two models generated from one definition proves the
*generator* self-consistent, not either model correct. **Add a third object to
the contract: an independent external golden (Sail, or the instrumented
`mini-rv32ima`).** Single-source `D` is right for reproducibility and wrong for
soundness; keep both properties, separately.

**A7. Does a richer value domain cost speed?**
`sweep-05` says cost goes up (O(drivers × bits) resolution); `sweep-01` and
keystone-c say it goes *down*.
**→ `sweep-01`/keystone-c win, measured: `record Word` at 10.85 ns vs `BitSet`
at 21.11 ns at w=32**, because immutability deletes ~20 defensive `clone()`
sites and takes a net with *n* sinks from *n+1* allocations per change to zero.
`sweep-05`'s concern applies only to multi-driver nets, of which a synchronous
RISC-V core has almost none. **Separately: the parallel-`BitSet`-pair
representation that `sweep-01` recommends as the smallest diff is measured at
~81 ns — 4× SLOWER than today.** The corpus calls this "the most important
single correction this sweep has to make to the other sweeps", and it is.

**A8. Whether four-state X helps or hurts a boot.**
keystone-a/b: X is a debuggability precondition; the free reset is a correctness
problem. `sweep-01`/`sweep-05`: V1 and V5 are *actively harmful* — X-pessimism
can poison a machine into never booting, and registers powering up X mean no
boot at all.
**→ Both are right about different things, and the resolution is a one-off
audit plus a permanent default.** Run the machine **once** with
`initInputs`' depth-uniform zeroing disabled, as a gating experiment: if it
does not boot, "M_L boots" is an unsound claim and the parity contract would pin
a fiction into `D`. Then keep **two-state permanently** for the boot/parity
configuration. Cheap, decisive, and it belongs on `BRIEF.md` §10's experiment
list next to the alpha measurement.

### B. Internal to the corpus

**B1. Is #221's trigger met?** `keystone-c:142-150` says "not *unusably slow* by
a strict reading, but one order of magnitude from it" — then proposes restating
the trigger as "below 10 kcycles/s on the #202 golden's CPU", against its own
measured **8,090**.
**→ Met, by the corpus's own proposed threshold**, and `ARCHITECTURE.md:355-358`
says "unusably slow **interactively**", which a 1.5 s/char echo is by any
reading. The disagreement is about *which workload counts*: keystone-c measures
a 12-second classroom program; the study measures a boot 400× larger plus an
interactive tty.

**B2. `lf-06` and `lf-07` both call themselves "P7".**
**→ Cite `AMENDMENT.md:149-150`: P11 = diff/merge/VCS, P12 = API/platform.** A
downstream phase citing "P7" from a raw sweep file is talking about a different
program from one citing P7 from `README.md`'s original numbering. **Citation
hazard, not a factual dispute.**

**B3. Does `FieldExtend` erode Mode T's payoff?** `lf-02` argues it converts
zero-delay events (which Mode T deletes free) into timed events.
**→ False at HEAD.** `FieldExtend` implements `Timed` and **ignores its own
`propDelay`** — `grep -n "now +"` returns nothing; it propagates at `now`. It is
observably zero-delay. Mode T's 82.3% fraction is not eroded by #201.

**B4. keystone-a says 12–16 weeks for the value domain; keystone-b says 17–22
for overlapping scope.**
**→ keystone-b is better grounded** (line-by-line census with anchors) **and
explains the gap** (the dual-mode discipline that keeps the tree green costs 6 of
20 weeks). The maintainer should adjudicate which number enters the schedule;
for this study only S0+S1 are in scope either way.

**B5. Every "verified at HEAD" claim in `docs/capability-roadmap/` predates
#201 and #220.** Element counts, `react` counts, coercion-site counts, the
`HdlExporter` reject list, the `toBeValue` enumeration and the clock-edge-detector
count are all stale — see `BRIEF-DELTA.md` §I.
**→ Re-verify roadmap anchors rather than trusting the "verified" marker.**

**B6. D5 vs the corpus's evidence base.** D5 is binding and deletes the harness,
generator, oracle and fixture behind every measured number in 288–424 weeks of
roadmap, plus both of P11's and P12's headline acceptance criteria.
**→ Not adjudicable — it is a genuine collision between a binding decision and
the corpus, and it is resolved by work, not argument: P19 must land first.**

---

## 6. THE THREE WINDOWS THAT ARE CLOSING

Ordered by how fast they shut.

1. **P5's report channel and exit-status lattice is a ONE-WEEK design for five
   consumers.** A parity verdict must go in during that week. `AMENDMENT.md:797-803`:
   "Design it ONCE, with the full verdict lattice, or reopen it four times."
2. **P12's "no callback direction, ever" is not yet written.** Contest it — or
   propose a narrowly-scoped device SPI distinct from the scripting protocol —
   before P12 is specified. Afterwards it costs a documented reversal of a
   normative permanent rule, on top of #63 and `vcd-interop.md`.
3. **P11's FORMAT 3 is the only free opportunity to introduce a section
   boundary** on a normative format with a compatibility promise. If C1 ships as
   a bare global bump for diff stability alone, **D3's per-section versioning and
   D1's raw-sidecar boundary force a FORMAT 4 within a year.** The study's D1
   content-kind split table is the missing half of `lf-06`'s C1 and should be
   handed over before code starts.

And one that is already open and free: **the site / slot index.** P9's journal
site index, P8's levelization slot table, P6's cross-probe map and P4's
critical-path overlay key are **the same table** (`AMENDMENT.md:805-809`), and
P17's fidelity toggle makes it a *fifth* consumer. "One table, four payoffs.
Must be designed BEFORE either P8 or P9 writes code."
