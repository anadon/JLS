# BRIEF: the evidence base for the JLS virtual-hardware / virtual-logic parity study

Authoritative distillation of three recon workflows (28 agents, ~1.6 MB of
reports in this directory). Downstream phases MUST treat this file as the
shared factual ground and may only contradict it with new evidence and a
citation. Full backing: `00-fact-base.md`, `01-prior-art-synthesis.md`,
`02-element-count-determination.md`, `recon-*.md`, `art-*.md`, `c2-*.md`.

## 0. Maintainer directives (binding, override any inherited assumption)

1. **`riscv/` will be stripped entirely.** It is remnant work. It may be cited
   as *calibration evidence* (it is the only measured CPU-scale JLS circuit)
   but NOTHING may depend on it, and no recommendation may route through
   `riscv/build_cpu.py` or `riscv/jlsbuild.py` as a deliverable mechanism.
   The *approach* those files embody (programmatic word-level construction) may
   survive; the files may not. Whatever replaces them must be a first-class,
   in-tree, tested JLS mechanism.
2. **Stale in-tree artifacts are not authority.** Anchor to HEAD source and the
   normative docs (`ARCHITECTURE.md`, `docs/grand-architecture.md`,
   `docs/simulation-semantics.md`, `docs/batch-interface.md`,
   `docs/extension-points.md`, `docs/file-format.md`, `docs/reproducibility.md`).
3. **Prior art is industry-wide**, not IBM-only: Intel/Simics, ARM FVP, Cadence/
   Synopsys/Siemens emulation, SystemC TLM-2.0, gem5/QEMU, RISC-V RVFI/RVVI.

## 1. The single most important correction: JLS is word-level, not gate-level

A 32-bit `Adder`, a 32-input 32-bit `Mux`, a `Register`, and a 256x32 `Memory`
are each **one element with one `react()`**. Ripple-carry survives only as
`propDelay = bits * 30` (`src/jls/elem/Adder.java:259-262`).

**JLS is already a mixed-abstraction simulator at the word/RTL tier.** Every
"gate-level simulation is 1-10 cycles/s, therefore hopeless" verdict in the
literature is a **category error** when applied to JLS. Two of twelve recon
probes made exactly that error; it is resolved against them.

Measured placement on the industry speed ladder:

| Tier | Speed | Source |
|---|---|---|
| QEMU TCG (DBT) | >300 MIPS | R2VM, arXiv 2005.11357 |
| ARM Fast Models / FVP | 20-200 MIPS, "OS boots in tens of seconds" | Fast Models v11.28 User Guide |
| Simics (2002 interpreter) | 2.1-8.7 MIPS, booted 4 real OSes | IEEE Computer 35(2) |
| TLM-2.0 LT + decoupling + DMI | 67.4M transactions/s (9.5x plain LT) | measured this session |
| **JLS (word-level, measured)** | **4,618-6,425 cycles/s in-loop; 8,328-10,338 clock-driven; 914-2,900 end-to-end CLI** | six independent probes |
| ModelSim, comparable small SoC | 18,300 cycles/s | practitioner report |
| Commercial SW RTL sim, large SoC | 1,260 cycles/s | vendor |
| Verilator | 1.2M cycles/s (Murax) to 6.6 kHz (Rocket) | 180x spread = design size |
| Icarus vs Verilator, IDENTICAL RTL | 1.49 kHz vs 42.66 kHz (29x) | cleanest same-design measurement |
| Gate-level + SDF, 100K gates | 1-10 cycles/s | Cadence GLS white paper |
| Emulation (Palladium/ZeBu/Veloce) | 1-5 MHz, costs millions | vendor |

**JLS is within ~3x of ModelSim per cycle and ~4x FASTER than commercial
software RTL simulation on a large SoC.** Descending to real gates costs a
measured 24.8-47x and buys nothing.

## 2. Measured engine constants

- **2.0-2.6 M events/s** retired, warm (six probes; ceiling 5.66 M/s on a
  33-gate ring). Outliers 0.63M and 4.4M explained by measurement scope.
- **~386-409 reacted events per clock cycle** for the shipped 228-logic-element
  single-cycle RV32I (four independent instrumented counters).
- **~1.8 events per active logic element per cycle.**
- **Engine throughput is nearly FLAT in circuit size: R ~ L^-0.12.** This kills
  the assumption that element count enters boot time linearly twice. It enters
  once, via events/instruction.
- **~48% of loop time** is `PriorityQueue` + `HashSet` dup-check bookkeeping
  (207.6 ns of 431 ns/event). **~62% of allocation** is `BitSet` + `long[]`
  churn on values that are all <= 64 bits.
- **Wire objects post NO events.** `WireNet.propagate` posts to
  `p.getElement()`, never to the `WireEnd`; a `WireEnd` has no `react` and
  would throw. The 6.8x-14x wire multiplier costs **load time and heap only**,
  not simulation speed. (This disproves a claim made by one C2 method.)
- Default time limit `JLSInfo.defaultTimeLimit = 100000000`
  (`src/jls/JLSInfo.java:69`) - a hard ceiling that a boot run exceeds.

## 3. The boot-cost arithmetic

- **Linux reaches a shell prompt in 4.0e7 retired instructions.** MEASURED on
  instrumented mini-rv32ima (Linux 6.5.12, RV32IMA nommu, busybox initramfs),
  cross-checked at 4.5e7 via uARM. NOT the 1e8-1e9 that nine of twelve probes
  assumed.
- **The apparent contradiction with NEORV32's ~1-2.5e9 resolves to delay
  loops.** `calibrate_delay` / `udelay` burn cycles in proportion to the
  DECLARED clock frequency and compute nothing. Therefore two levers, free:
  - **declare a slow clock** (1 MHz instead of 50 MHz shrinks every delay 50x
    at zero architectural cost);
  - **pin `lpj=` on the kernel command line** to skip calibration entirely.
  These are worth more than any engine optimization.
- **The boot is compute-bound, not timer-bound**: running the emulated machine
  100x slower raised instructions-to-login by only 8%, provided CLINT `mtime`
  is driven by SIMULATED time. This is what makes a very slow simulator viable.
- Linux needs **>=12 MiB RAM** for a usable shell (16 MiB recommended). JLS's
  `DenseWordStore` reaches 16 MiB at 32-bit words (2^22). **They meet with zero
  headroom** - one word past the limit and cost jumps.
- Minimum SoC is tiny: device tree 1,536 bytes; the entire UART is **3 byte
  addresses** (THR write, RBR read, LSR read returning `0x60 | data_ready`);
  one CLINT; **no PLIC** (16550 polled at irq=0).

## 4. Element count: C2 resolved from 240x to 2.1x

**Nommu machine: ~600 functional logic elements** (band 420-1,100), where
"functional logic element" = non-Wire, non-WireEnd. Multiply ~6.8x for total
runtime objects (~4,100). **Sv32 + S-mode + OpenSBI: ~760** (delta +160).

The 170,000 figure was an **artifact of a Yosys flag**: plain `synth` flattens
72 Kib of I$/D$/regfile SRAM into 73,374 `$_DFFE_PP_` + 74,175 `$_MUX_` cells.
171,576 cells with memories flopped vs **20,308 with memories preserved** -
88.2% of the scary number is SRAM that JLS models as single `Memory` elements.
A further 8.2x is gate-mapping itself, which buys nothing at word level.
Normalized like-for-like the four methods give 480 / 520 / 700-1,000 / 900.

**Boot-time model** (`N_instr x events_per_instr / events_per_sec`):

| Option | Logic elems | CPI | ev/instr | Wall clock |
|---|---|---|---|---|
| Behavioral macro-element ("virtual hardware") | ~10 | 1 | ~12 | **~3 min** |
| Authored word-level, mirrored-Memory regfile, iterative M | 600 | 2.9 | 500 | **1.9 h** (0.8-5.3) |
| + behavioral Multiplier/Divider elements (HYBRID) | 560 | 2.4 | 430 | **1.6 h** |
| Authored, combinational multiplier | 750 | 2.4 | 420 | 1.6 h |
| Authored, 31-flip-flop regfile | 689 | 2.9 | 750 | 2.8 h |
| Sv32 + S-mode + OpenSBI, authored | 760 | 3.9 | 850 | **4.4 h** (2-12) |
| Word-mapped Yosys import | 2,300 | 1.3 | 1,120 | 4.3 h |
| Gate-mapped import, memories preserved | 9,500 | 1.3 | 3,965 | 18.4 h |
| Gate-mapped, memories FLATTENED (the "170k" artifact) | 200,000 | 1.3 | 83,460 | ~26 days |

Design choices worth knowing:
- **Register file**: 2 mirrored `Memory(32x32)` = 9 elements, vs 31 `Register` +
  31 hold-Mux + 31 AndGate + two 32:1 read Muxes + Decoder = 98 elements (43%
  of the entire measured demo). Worth +/-89 elements AND ~+/-250 ev/instr.
  Forced by `Memory` having exactly one address port.
- **No `Multiplier` and no `Divider` element exists.** `ElementRegistry.ALL` is
  the complete 33-type list. Adding two behavioral elements beats building a
  194-element combinational array: *spend element-library effort, not circuit
  effort.*

**The dominant remaining uncertainty is NOT element count.** It is **alpha, the
per-cycle active fraction** of a multi-cycle machine (spread 3.1x: 0.18 / 0.40 /
0.56), because no multi-cycle JLS machine exists to measure. CPI is second
(adjudicated to ~2.9; the 6.6 estimate over-counted because `Memory` reads are
**asynchronous** - an address change posts a read at `now+accessTime` with no
clock, `Memory.java:1396-1397` - so fetch is one cycle and a load is one cycle).

## 5. The live-console constraint (the binding one)

A tty echo path is 1e4-1e5 instructions. Human-perceptible latency needs
>=1e5-1e6 cycles/s. Against 2.0-2.6M events/s that is a **total budget of ~2-23
events per simulated cycle, versus the 386 measured today.**

> **No arrangement of these constants supports a live console on any structural
> CPU model, gate or word-level.**

At current speeds a keystroke echo is **8 seconds to 13 minutes per character**.

This is not a defeat - it is the argument for the two-tier framing. Live
interaction belongs to the **behavioral (virtual hardware)** tier (~3 min boot,
and interactive); the **structural (virtual logic)** tier boots headless in
hours and is bound to the behavioral one by a parity contract.

## 6. The parity contract (synthesized from ARM PV-vs-cycle, TLM-2.0, gem5, RVFI)

Objects: `D` single-source machine definition; `M_H` virtual hardware
(behavioral); `M_L` virtual logic (structural); `P` program; `I` input log
**timestamped in RETIREMENT INDEX, not seconds**; `E` an explicitly enumerated
per-bit exclusion set.

**MUST be bit-identical** given the same `D`, `P`, `I`:
1. The ordered sequence of committed architectural state deltas, **one record
   per retired instruction**, monotonically indexed, no gaps, no reuse:
   `{order, pc_before, pc_after, insn_word, rd_index, rd_value, mem_addr,
   mem_rmask, mem_wmask, mem_wdata, privilege, trap}` - this is RVFI's field
   list and it is the comparison alphabet.
2. Full architectural state at every declared **sync point**: PC, GPRs,
   implemented CSRs minus `E`, memory digest over non-volatile regions.
3. The guest-visible **output byte stream**, in order.
4. Trap/exception occurrence and cause, attributed to the causing instruction.
5. Retired-instruction count between sync points.

**PERMITTED to differ** (each with industrial precedent):
- **All timing** - cycles, elapsed simulated time, CPI, interrupt latency. ARM:
  "you must not rely on the accuracy of cycle counts, low-level component
  interactions". Measured this session: six SystemC abstraction levels produced
  a **bit-identical memory image** (FNV-1a `35d5d215e8dd3b83`) while reporting
  170M / 200M / 60M ns of simulated time - a 2.8x spread, zero warnings.
- **All microarchitectural state** - pipeline, caches, TLBs, predictors,
  in-flight transactions. gem5 *panics* rather than checkpoint classic caches,
  explicitly flushes TLBs at handover; ARM PV models do not model caches at all.
- **Event ordering finer than the sync quantum**, and asynchronous interleaving.

## 7. What JLS structurally LACKS (the gap list, all verified at HEAD)

| Gap | Evidence | Severity |
|---|---|---|
| **No simulation-state serialization at all** - save format is structure-only; `Memory`/`Register` save their *init text*, never the running store. No checkpoint, no resume, no sim time, no event queue, no net identity | `Circuit.save` takes no simulator; `Memory.initSim` rebuilds from init text; no write-back path exists | fatal |
| **No host I/O and no device concept** - zero `System.in` in all of `src/`; only two host-I/O sites exist, both read-only text at `initSim` | grep-verified | fatal |
| **`Element` is `sealed permits DisplayElement, LogicElement, Wire`** - a device element cannot be added from outside; `LogicElement` sealed over 22 permits | `src/jls/elem/Element.java:17-18` | fatal |
| **No event-injection path** - all `post()` sites are in `jls.elem`; `post()` is unsynchronized over a plain `PriorityQueue` with a single-thread contract. `beforeEvent()` is the only thread-correct injection point, and its contract was written for pausing | `Simulator.java:165-170` | fatal |
| **`BatchSimulator.pause()` is literally identical to `stop()`** - batch is one-shot: load, run to completion, print | source | major |
| **No programmatic construction API** - `Element` sealed; all six in-tree generative paths emit save-format TEXT and re-parse it | verified | major |
| **Subcircuits are per-instance deep copies with NO parameters** - measured sharing factor exactly 1.00x; only orientation persists | `SubCircuit.java:284-285` | major |
| **64 MiB load cap** => ~695k element ceiling; save is unbounded, so JLS will write files it then refuses to open | `FileAbstractor.java:65` | major |
| **`NetlistImporter` realizes only 5 cell types** (`$not/$and/$or/$xor/$mux`) and **cannot emit a flip-flop** - a committed test *asserts* `$dff` is rejected | `NetlistImporter.java:234-259`; `NetlistImporterTest.java:227-235` | major |
| **O(n^2) `SigSim` stimulus parse = 80% of end-to-end wall time and 95% of all allocation** (39.58 GB of 41.76 GB for a 50k-cycle run) | measured | major, but an ordinary bug |
| **O(W^2) `Circuit.finishLoad`** - 80k wire ends = 46 s | measured | major |
| GUI per-edit cost 58 ms @10k elements, 552 ms @100k (whole-index invalidate + whole-circuit snapshot) | measured | moderate |
| The golden oracle is **34 simulated cycles and 4 assertions**, gitignored, never run by CI, RV32I-only | verified | major |
| No CI lane can host a multi-hour run: required gate is 141 s; no `timeout-minutes` anywhere; 6-hour hosted ceiling. No Git LFS, no large-fixture policy; a 2.4 MiB kernel is a 33 MB `.jls` at 15.87 B/word | verified | major |

## 8. Recorded decisions this work touches

- **#221 - the discrete-event interpreter is the SOLE simulation strategy.** Its
  revisit trigger: "a concrete CPU-scale design on the `riscv/` trajectory that
  is unusably slow interactively." **The trigger is now quantitatively met and
  instrumented for the first time.** But the recorded process requires FILING
  the follow-up issue first (it "deliberately does not exist yet"), and any
  second strategy is bound to bit-for-bit agreement with the golden and to
  observably identical per-element propagation delays - **which is exactly what
  Verilator-class levelization gives up.** Note ~48% of per-event cost is
  removable *without* changing semantics, so constant-factor work is available
  before the trigger must be pulled.
- **`docs/vcd-interop.md:19-24` REJECTS live co-simulation** under #63:
  "Graders must not depend on interacting with a running simulation." This
  **directly contradicts** the goal and must be explicitly reopened or
  reconciled - it is an in-repo contradiction with `docs/grand-architecture.md`,
  not a probe error. Both documents verified this session.
- `grand-architecture.md` §6 hot/cold plane rule: the simulation inner loop
  lives entirely inside `core` with zero plugin indirection. A second strategy
  must be a core-internal change, not an extension point.
- `grand-architecture.md` §9 exclusions; the #222 plugin trust boundary; the
  extension-point catalog requires a `docs/extension-points.md` row (id,
  contract type, cardinality, lifecycle phase) BEFORE a new seam can exist.
- Governance is a hard constraint: bus factor 1; single self-contained offline
  jar; **93.0/92.0/84.5% JaCoCo as a PACKAGE aggregate** with no per-class
  exemption, plus 80/82 PIT mutation thresholds, on any new code.

## 9. Nobody has done this

No drawn/schematic logic simulator has ever booted any OS - not Logisim,
Logisim-Evolution, Digital, CircuitVerse, DigitalJS, or Falstad. The class
ceiling is CircuitRISCV (Logisim-Evolution): RV32IM with a terminal, no OS.
Meanwhile hneemann's Digital - also Java, also event-based, also educational -
runs a complete processor at **120 kHz**, i.e. **20-120x faster than JLS**, on
2012-era hardware. **The JVM is not the ceiling; JLS's constant factors are.**

## 10. Open contradictions still unresolved

- **alpha (per-cycle active fraction), 3.1x spread, never measured.** Now the
  dominant uncertainty in the entire model.
- The 2.02x TestGen-vs-Clock events/cycle discrepancy (245.5 vs 121.5 on
  element-for-element identical circuits) - unexplained.
- Whether the drop of an event past `maxTime` (polled and removed from
  `dupCheck` before the limit check, then discarded without reacting or
  re-queueing) is a defect or intended.
- RV32 nommu is on a published deprecation trajectory: a Feb-2024 patch proposed
  removal "by the beginning of 2027", and RV32 requires `CONFIG_NONPORTABLE=y`.

**The cheapest experiment that collapses the biggest remaining uncertainty:**
convert the existing single-cycle demo into a 2-cycle unified-memory machine
(~10 new elements: merge imem/dmem into one `Memory`, add an IR `Register`, a
fetch-vs-data address `Mux`, a PC-hold `Mux`, a 2-state sequencer) and count
events with an internal `Clock` element. That one experiment measures alpha,
CPI, and the calibration constant simultaneously.

---

## 11. MAINTAINER DECISIONS (added mid-study; BINDING on all later phases)

These are decisions, not options. Do not re-litigate them; design to them.
Where a decision has an engineering consequence, engineer the consequence.

### D1. Uncompressed is the default saved format.

Maintainer, verbatim: *"For collaborative and teaching purposes, I think using
an uncompressed format is more useful. Now, with transparent file compression on
new filesystems like BTRFS, there is less pressure for using compressed file
formats. Users can also independently compress files themselves, which is a
common idiom."*

Supporting evidence found this session:
- **The mechanism already exists and is already tested.** `FileAbstractor` has a
  `Container` enum with `XZ` and `PLAIN_TEXT`; plain-text write is implemented
  and pinned by `FileAbstractorTest.plainTextWriteIsTheBareCircuitText` and
  `plainTextWriteReplacesAnXZFileAtomically`; `openCircuit` sniffs XZ, zip, and
  plain text. Changing the default is a POLICY change plus test updates, not an
  implementation project. (`src/jls/FileAbstractor.java:43-53, 180-230`.)
- Uncompressed restores BOTH textual diff AND git delta compression between
  revisions. Delta matters more than absolute size: N revisions become N deltas
  rather than N full opaque blobs.
- The 64 MiB cap does NOT regress, because `MAX_CIRCUIT_TEXT_BYTES` is already
  measured against DECOMPRESSED text (`FileAbstractor.java:65`).

**Engineering consequence that MUST be designed for:** bulk binary payloads are
incompatible with an uncompressed text body. At the measured 15.87 bytes/word a
16 MiB RAM image is ~66 MB of text - **it alone exceeds the 64 MiB cap**, before
any circuit content. `initrle` helps sparse/repetitive memory but not a kernel
image. Therefore the format splits along content kind:

| Content | Storage | Diffed |
|---|---|---|
| Structural circuit content | uncompressed canonical text, in file | YES - the reviewable artifact |
| Memory/kernel images, simulation-state checkpoints | sidecar file or separate raw section | NO - never expected to diff |

This also resolves the diff-vs-compression tension: those are two different kinds
of content, not one contested one.

Still to settle (do not assume): whether `.jls~` autosave checkpoints stay
compressed (write volume, not diffability, is their constraint); whether
in-memory `CircuitSnapshot` undo stays deflated (memory pressure, unrelated to
diff); and whether XZ write support is retained as an explicit user option.

### D2. Diff stability is a first-class requirement of the format.

Maintainer asked directly about it. Consequences established this session:
- Canonical order (#166) and stable ids (#165) already make the serialized form
  a pure function of circuit content (`docs/file-format.md` §8) - a good base.
- BUT save-time reference ids are DENSE and "reassigned on every save"
  (`src/jls/elem/Element.java:21-22`), so inserting one element renumbers every
  later element and every `ref` to them. Referencing by STABLE ID rather than by
  dense file-local index is the structural fix.
- A three-way textual merge can produce a file that PARSES but is semantically
  corrupt (dangling ref, inconsistent net). Silent acceptance of that is a
  correctness bug, not an ergonomics issue.

### D3. The format successor needs INTERNAL (per-section / per-record) versioning.

Maintainer, verbatim: *".jlsx would need to also support internal versioning to
remain flexible."* Today there is exactly ONE global `FORMAT` integer for the
whole file, so any change to any record type bumps it and the file is accepted or
refused as a unit. Required: independently versioned sections with
**must-understand** semantics (an old reader SKIPS an unknown OPTIONAL section
and REFUSES an unknown REQUIRED one). Prior art to mirror: PNG critical vs
ancillary chunks, EBML, ELF sections, protobuf field-number evolution.
Synergy: a simulation-state checkpoint is naturally an OPTIONAL section, so a
reader that knows nothing about checkpoints still opens the circuit structurally
with a clean diagnostic instead of a hard refusal.

### D4. Direction of travel (from the maintainer, explicitly non-prescriptive)

- Nested / shared / parameterized subcircuit definitions.
- A switch to toggle a subcircuit between full-fidelity structural operation and
  a compiled/optimized implementation. (Adopted as a PER-SUBCIRCUIT toggle: it
  makes parity a property of a BOUNDARY, which is what makes it testable.)
- Compiled backends: self-contained at the Java level preferred; external
  HDL/Verilog/SystemC delegation acceptable but must not become the flagship
  path, because the single offline jar is load-bearing.
- The in-program definition of elements gets expanded.

### D5. `riscv/` will be deleted (restated - this keeps being forgotten)

It is calibration EVIDENCE only. No deliverable, recommendation, or dependency
may route through `riscv/build_cpu.py` or `riscv/jlsbuild.py`. Whatever replaces
them must be a first-class, in-tree, tested JLS mechanism.

---

## 12. MAINTAINER RULINGS, round 2 (BINDING)

### D6. Sequencing: fixes land immediately; the program waits on #77.
Defect fixes are NOT gated on the core extraction. Two have already landed and
pushed on `claude/jls-virtual-hardware-linux-njsoma`:
- `970db41` fix(format): register RegisterFile and FieldExtend in the frozen tag
  table (+ the missing registry->SaveTags totality test).
- `36cbd37` fix(elem): advance the creation counter past stable ids already in
  use (+ the long-edited-circuit reproduction).
Everything else in this program sequences behind #77.

### D7. Extensibility: adopted as recommended.
- **Circuit libraries are DATA, not plugins.** Distributing preprogrammed
  circuits and embedding reusable circuits as subcircuits needs the
  definition/instance split + a library format with versioning and provenance
  (P7/lf-01). No ABI, no trust boundary. This is also the biggest single win.
- **`jls.api` (P12/lf-07) is the extensibility story**, matching the actual
  commercial pattern: EDA extensibility is scripting + data libraries (Cadence
  SKILL; Tcl in Synopsys/Siemens/Vivado/Quartus), not a binary plugin ABI.
- **`HostBytePort` is NOT a plugin seam.** Sealed intermediate following the
  existing Gate/Group/Pin/SigSim pattern; host access is ONE DOOR GRANTED AT
  INVOCATION (the `-serial stdio` model: a human grants it, never a property of
  the circuit file); the device subsystem is hosted in the shipped module
  runtime (first-party, in-process, one type namespace, §4.3's sanctioned case).
  Rationale: #212's demand gate has not opened; #38's hardening assumes a `.jls`
  file is DATA that cannot touch your machine; and the golden-test culture needs
  the set of host doors closed and known. If ever overruled, host-touching
  providers go OUT-OF-PROCESS (§4.3's reserved case) - in-process is the one
  variant that cannot be walked back.

## 13. CORRECTIONS from the new corpus + new code (supersede sections above)

| Was | Now | Anchor |
|---|---|---|
| 2.0-2.6 M events/s; 431 ns/event | **3.14 M events/s, 318 ns/event** in the WARM EVENT LOOP; queue+dedup 151.8 ns of 318 (47.7%). 2.0-2.6 M was the INCLUDING-`initSimulation` figure - always label which | keystone-c-performance.md:126,136 |
| Register file: 9 elements (mirrored Memory) vs 98 (flip-flop farm), worth +/-89 elements and +/-250 ev/instr | **ONE element. The design choice is DELETED.** Native `RegisterFile` has independent RA/WA ports, so the one-address-port constraint that forced mirroring is gone | `src/jls/elem/RegisterFile.java:21-28,139-161` |
| mirrored-Memory regfile ~12 ev/cycle | **MEASURED 18.00** (brief was 33% optimistic). Native `RegisterFile` **6.94**; 31-flip-flop farm **114.53**. Cause: `Memory.react` posts a self-event at `now+accessTime` on EVERY inbound PinChanged | measured this session; `Memory.java:1379-1403` |
| ~600 logic elements (band 420-1,100); 500 ev/instr | **~580 central (band 400-870); 468-485 ev/instr.** Sv32 ~750 | re-derived; `FieldExtend` also removes 4 `Extend`s |
| Boot: nommu 1.9 h, Sv32 4.4 h, behavioral ~3 min | **nommu 1.66-1.72 h; Sv32 4.00 h; behavioral 2.5 min.** After the semantics-PRESERVING stack (2.26x): **44-46 min**. After the full stack (4.9x): **20-21 min**. The 31-flip-flop row is STRUCK (strictly dominated). Engine work accrues ENTIRELY to the structural tier - the behavioral row does not move | keystone-c-performance.md:685-697 |
| "No arrangement of these constants supports a live console on ANY structural CPU model" | **NARROWED to the discrete-event interpreter.** ~19,500 cycles/s today (1.5 s/char at a 1e4 echo path); **~44,000 after the semantics-preserving stack (0.66 s/char)**; ~96,000 after the full stack (**0.30 s/char**); 22,000-86,000 under an unbuilt Mode C - against a 1e5-1e6 requirement. **The 1e5 floor is missed by only 1.2-5x. This is a DECISION plus ~30-45 maintainer-weeks, not a physical limit.** The two-tier framing survives; its JUSTIFICATION changes | keystone-c:474,490-495 |
| The module system is unbuilt | **SHIPPED (#220/#223/#224) and boots in EVERY run mode** - `JlsModules.boot()` runs from `JLS.main` before `JLSStart.start()`. BUT "wired and UNCONSUMED" by its own javadoc: the registry is populated and nothing reads it for dispatch. **A device module contributed today would boot correctly and be invisible** | `src/jls/boot/JlsModules.java:31-36`; `src/jls/JLS.java:60` |
| `Element` sealed => a device element is impossible - FATAL | **Grade SPLITS.** `Element` permits still 3; `LogicElement` 22 -> 24. Still fatal for OUT-OF-TREE and deliberately so; **MODERATE in-tree** - a measured ~65-line registration tax across 12 files, demonstrated twice in one commit. Plan UART/CLINT as **in-tree `LogicElement` subclasses**. A new element type costs **ZERO format version** | verified at HEAD; `git show --stat 38a0544` |
| 33 element types; 25 `react` impls | **35 types; 27 `react` impls.** "No Multiplier and no Divider" SURVIVES. **Every element/react count in `docs/capability-roadmap/` is stale** - that branch was cut before #201 and #220 merged | `ElementRegistry.java:38-77` |
| `NetlistImporter` realizes 5 cells, cannot emit a flip-flop | **Mapper claim SURVIVES, now doubly pinned.** But `CellValidator` now accepts **19 cell types** incl. `$dff`/`$dlatch`/`$tribuf`/`$add`/`$bmux`, hierarchy instances, and `$mem_v2` with one unclocked read + 0/1 rising-edge write ports (#61). **The gap MOVED from validation to realization** - remaining work is one-sided mapper increments with acceptance criteria already written | `NetlistImporter.java:234-258`; `CellValidator.java:59-68,231-266` |
| ~62% of allocation is BitSet churn | **~50% of in-loop allocation among named non-`byte[]` classes.** The largest allocator of the whole run is `byte[]` from `SigSim`'s quadratic string concatenation - the same defect already recorded at 95%; one run seen at two scopes. NEW: `SimEvent.PinChanged` is a **ZERO-FIELD record allocated 1.92 M times/run** | JFR, measured |

### Errors this digest caught in its OWN sibling agents (kept as a caution)
Two digest agents misread keystone-c's levelized figure. **4.32 ns is per NODE**,
and 522 nodes = 225 logic elements + 297 nets (2.32 nodes per logic element), and
a levelized design needs **two passes per cycle**. A ~580-element machine is
~1,345 nodes: 2 x 1,345 x 4.32 ns = 11.6 us/cycle = **86 kcycles/s UNDERATED
CEILING**, and keystone-c itself derates twice to 22-40 kcycles/s. One agent was
wrong by 4.6x (counted logic elements as nodes, one pass); another presented the
underated ceiling as achievable. **Any figure derived from ns/node must state
node count and pass count.**

### D8. "Orchestrate external tools, never reimplement" is NOT a maintainer decision. REVOKED as policy.

Maintainer, verbatim: *"I didn't put in place the 'orchestrate external tools,
never reimplement' stance. If it makes more sense to reimplement then do that
and use the existing implementations to just speed development."*

Provenance: the stance originates at `docs/grand-architecture.md:58` and §9 - a
forward-looking determination document, not a ratified decision - and propagated
into at least six documents that each cite it as settled:
`docs/standards-landscape.md:836`, `docs/hdl-support-research.md:40`,
`docs/icestick-bitstream-handoff.md:12`,
`docs/capability-roadmap/sweep-04-verification.md:156`,
`docs/capability-roadmap/lf-05-fault-and-power.md:332-333`. It is exactly the
failure mode D-series ruling 2 warns about: an AI-authored claim treated as
authority. **It binds nothing.**

**The replacement axis is COST AND SPECIALISM PER CAPABILITY, not policy.** For
each capability ask: what does it cost to build and OWN in Java, versus what does
orchestrating it cost to MAINTAIN FOREVER?

Reimplementation actively serves constraints that ARE ratified, and orchestration
damages them:

| Constraint | Reimplement | Orchestrate |
|---|---|---|
| Single self-contained OFFLINE jar | preserved | **broken** - the student needs a toolchain install |
| Determinism / byte-identical goldens | pure Java is bit-reproducible | external float solvers are **not** reproducible across platforms or versions |
| CI | no toolchain matrix, no skip logic | a version-compatibility surface that rots |
| First-mover cost | none | **there is not one `ProcessBuilder` in all of `src/` at HEAD** |
| Bus factor 1 | more code owned | less code, but an unowned dependency that moves |

**Licensing is far more permissive than the corpus assumed, and this is the key
enabler of "use existing implementations to speed development".** JLS is
GPL-3.0-or-later, which can ABSORB most open-source EDA code outright: ngspice
(BSD), Yosys (ISC), Verilator (LGPL-3 / Artistic-2), KLayout (GPL-2-or-later).
So "speed development with existing implementations" can mean actual code reuse
and porting, not merely reading for reference. The genuine hazard is narrow and
specific: **GPL-INCOMPATIBLE** licenses (e.g. ELK's EPL-2.0), which is the case
`grand-architecture.md` §4.3 actually identified before it was over-generalized
into a blanket stance. Any absorbed code must carry its attribution and license
notice.

**Re-sorted by cost, not policy:**

| Capability | Verdict under D8 |
|---|---|
| Levelized / compiled evaluation (Verilator's *technique*) | **Reimplement.** Already the plan (P8 / Mode T); keystone-c measured 4.32 ns/node with plane arrays. D8 removes the last objection |
| Yosys JSON netlist mapping | **Already reimplemented in-tree** (`NetlistImporter`). Remaining work is one-sided mapper increments |
| GDSII / OASIS reader | **Reimplement** - open spec, and JLS is already a 2D geometry engine |
| Teaching-grade SPICE (DC + transient; linear elements, diode, MOSFET L1/L3) | **Plausibly reimplement.** Sparse LU + Newton-Raphson + LTE timestep control is a known graduate-scale project, and it PRESERVES the offline jar and determinism. Reimplementing ngspice's full model library (BSIM4 and friends) is NOT plausible - scope the models, not the solver |
| Verilog / SystemVerilog **parser and elaborator** | **Do not reimplement.** This is what the stance was really protecting against and the protection is sound on COST grounds: Verilog is genuinely hostile to parse. Keep importing via Yosys JSON - a cost judgment, not a policy |
| Place & route, DRC, LVS, gerber generation | **Another tool class.** Refuse on scale, not on principle |

**Consequence for work already briefed:** the multi-view/SPICE workflow
(`wf-views.js`) was briefed with the revoked stance and told to apply a
COMPUTE/DISPLAY/FRONT-END test in which "compute" meant "refuse". Under D8,
that test remains useful as a COST classifier but is no longer a REFUSAL
classifier. Its determination phase must be re-run with this correction; the
survey phase is stance-independent and stands.

### D9. The audience is CS -> ECE -> EE, and cross-disciplinary teams are a target use case.

Maintainer, verbatim: *"Having this scale from CS to ECE to EE is perfectly fine.
It is the same problem space. It would be reasonable to even assemble a team of
students from all of these disciplines to work on broader projects for the same
ultimate task in practice."*

**What this settles.** The audience-fit objection raised against the breadboard
view - "breadboarding is an EE course JLS does not serve, and K9 makes audience
fit decisive" - is **withdrawn**. It is one trajectory, not three audiences.

**Why the claim is well-founded, not merely permissive.** The views under
discussion are the educational-scale version of one real industrial flow:
specification -> logic -> RTL -> synthesis -> place-and-route -> board -> silicon.
A design genuinely does travel all of it. And no existing educational tool spans
it: Logisim/Digital/CircuitVerse stop at logic; KiCad starts at schematic/PCB;
Vivado/Quartus start at HDL. **The span itself is the differentiator**, and it is
a stronger position than "commercial grade" ever was, because it is not competing
with Vivado on Vivado's ground.

**K9 is RESTATED, not repealed.** The pedagogy floor was "no regression for a
first-year student drawing an adder". Under D9 that becomes **progressive
disclosure, not audience exclusion**: the first-year must never SEE the ECE/EE
machinery, but the machinery may exist. Concretely - views are default-hidden and
opt-in; palettes are per-view, not one growing global palette; startup time and
per-edit cost are still ratcheted; conceptual load in the default view is
unchanged. K9 remains a hard gate; it now gates *visibility* rather than
*existence*.

**THE SYNTHESIS THIS UNLOCKS - multi-view and collaboration are the same
program.** A cross-disciplinary team working on one artifact, each member in a
different view, editing concurrently, is not a new requirement: it is exactly
what the collaboration stack already under way is for (#163 pure-P2P, #167
`CircuitOp`/`OpSink` shipped, #168 net -> #169 session -> #171 crdt, #170
security). Multi-view says the shared model must carry per-view geometry and
per-view element data; collaboration says concurrent mutations to that model must
merge. **They are one design problem: one artifact, N views, M editors.** The op
vocabulary is the seam for both - which means (a) multi-view ops must be
expressible in the existing `CircuitOp` grammar or the grammar must be extended
once, deliberately, for both; and (b) the per-view geometry problem and the
CRDT's merge problem must be solved together or they will be solved twice,
incompatibly. This connection was not drawn by any prior phase and should be
carried into the views determination.

**HONEST CAVEAT - audience fit is not demand evidence.** D9 establishes that
these users are legitimately in scope. It does not by itself establish that any
of them has asked. The project's own standard elsewhere (the i18n non-goal
requires "a concrete request from an instructor or course"; #212 requires a
demonstrated external provider) distinguishes *may* from *should now*. The
maintainer's phrasing is "it would be reasonable to" - a statement of
plausibility, not a report of an existing team. Any determination must therefore
still rank these views by cost and by evidence of demand, and must not launder
this ruling into a demand signal it does not claim to be.

### D10. THE CIRCULARITY ANTIPATTERN IS FORBIDDEN. (Standing directive, overrides prior determinations.)

Maintainer, verbatim: *"Throughout this process you keep shutting tight
development that I'm trying to pry open with the circular reasoning that the
development currently doesn't support the requested development. This is a real
antipattern that I want reversed."*

**The charge is correct.** Multiple determinations in this study refused
capabilities by citing the absence of those capabilities. Specifically:
- the MVL determination led with "zero mentions across 944 tracked files" - a
  measurement of what JLS has never OFFERED, and its own adversary flagged the
  circularity ("nobody requests a feature from a tool that visibly cannot do it")
  before the verdict overrode it;
- the multi-view determination led with "tracker search returns total_count: 0";
- the KiCad netlist verdict ("looks front-end, is a new tool") was an argument
  about completeness presented as one about tractability;
- the live-console refusal was stated as a physical impossibility and later
  measured to be a cost (~30-45 maintainer-weeks);
- #212's demand gate was invoked against the MAINTAINER'S OWN proposal.

**Aggravating factor:** several enforced constraints were never the maintainer's.
The "orchestrate external tools, never reimplement" stance was AI-authored and is
revoked (D8). The demand-gate standard comes from the same inherited documents. A
demand gate is a legitimate filter for THIRD-PARTY feature requests; applied to
the project owner's own roadmap it inverts its own purpose.

**THE RULE. When the maintainer asks whether something can be done, the absence
of the capability is the PREMISE OF THE QUESTION, not a rebuttal to it. The
question to answer is always "what is the path, and what does it cost" - never
"does the current state justify it".**

| LEGITIMATE objection | FORBIDDEN objection |
|---|---|
| Measured cost, and what it displaces | "No one has asked / zero demand evidence" |
| Sequencing: X genuinely requires Y first | "The current code doesn't do this" |
| Evidence a SPECIFIC APPROACH fails, with the alternative named | "A prior doc recorded no" (esp. if AI-authored) |
| A real conflict with a goal the MAINTAINER stated | "It's not in the roadmap" |
| A physical/mathematical limit, shown by arithmetic | An unmeasured assertion of impossibility |

**Obligations on every later phase:**
1. Answer with a PATH AND A COST. "Not tractable" requires arithmetic, not
   precedent. If something is genuinely impossible, show the limit.
2. Never treat a capability's absence as evidence against acquiring it.
3. Demand gates apply to third-party asks, NOT to the maintainer's roadmap.
4. Inherited AI-authored positions are INPUT, not authority. Cite them as prior
   reasoning to be re-argued on merits, never as settled.
5. Sequencing claims must be REAL dependencies ("X reads data only Y creates"),
   not preference. Say what it would cost to do X first anyway.
6. When refusing a specific APPROACH, name the approach that would work. A bare
   refusal is not an answer.
7. Cost estimates still bind: say what a thing displaces. Honest cost is not the
   antipattern - substituting precedent for cost is.

**Determinations to be RE-RUN under this directive:** the MVL determination
(07-mvl-determination.md) and the multi-view determination
(08-views-determination.md). Their SURVEY evidence stands; their VERDICTS were
reached under the forbidden reasoning and must be re-derived as paths and costs.

## 14. MAINTAINER RULINGS, round 3 (BINDING; supersede every determination above)

Six questions were put to the maintainer after the capstone and feature tiers
were filed. All six are answered. Recorded here as D11-D16 so no later pass
re-opens them.

### D11. #59 is closed. The ultimate outcome is what matters, not the bookkeeping.

Verbatim: *"Close it? It really doesn't matter what so long as the ultimate
outcome is good."*

Executed 2026-08-03: #59 closed as `not_planned`, `tier:capstone` retired from
its labels, mirror comments on #59 and #310. #310 (CAP-15) is now the sole
capstone-tier owner of the staged HDL programme, in the consume-its-features
form.

**The one live obligation this created**, recorded on #310 because a closed
issue cannot carry one: #59 held #291 (the Stage-1 Memory export remainder) as
a capstone-direct task under the tier-model exception. **No filed feature claims
#291.** It needs re-homing, most plausibly into #358 (FEAT-018).

The governing half of this ruling is the second sentence. Bookkeeping form is
not worth a decision cycle; the outcome is. Do not escalate a
disposition-shaped question again where any disposition leaves the outcome
sound - pick one, execute it, and record which.

### D12. Citations pin to a commit, or anchor to a landmark. Not to a bare line.

Verbatim: *"PR to some commit, then. Or use some relative offset from landmarks
like a function declaration."*

The problem this answers: `docs/plan/evidence/` landed in `3a81a4a`, after the
`2d0ca9d` evidence commit the issues declare, so citations to it do not resolve
at the commit they are pinned to, and bare line numbers rot on the next edit
regardless.

Two accepted forms, either is sufficient:
1. **Commit-pinned.** Cite `<path>:<line>` together with the commit the line was
   read at, and say plainly when that commit differs from the issue's own
   `evidence_commit` (which pins CODE claims). A permalink at that commit is the
   strongest form.
2. **Landmark-relative.** Anchor to a named, greppable declaration - a method or
   field signature, a heading - and express the citation relative to it. A
   landmark survives edits above it; a line number does not.

Prefer landmark-relative for anything expected to move. Prefer commit-pinned for
one-shot evidence. A bare `file:line` with no commit and no landmark is not a
citation and does not satisfy the evidence rule.

### D13. Dependencies are a list of prerequisite issues. Nothing more.

Verbatim: *"A dependency system simply listing prerequisite completed issues is
sufficient. I don't think that these require such fuss. Just make something that
works."*

This SUPERSEDES the elaborate treatment the planning corpus and the filed issues
grew around ordering. Specifically:

- The "beneficial features are hard `blocked_by` prerequisites of required
  features" finding is **not a rule-E violation and needs no reconciliation**.
  A prerequisite is a prerequisite. Whether a feature also owns an observation
  in a capstone's outcome statement is a separate and much less important
  question, and the two need not agree.
- Do not re-derive sufficiency or minimality arguments because an ordering edge
  crosses a required/beneficial boundary.
- Do not open questions, file REPLANs, or write reconciliation obligations for
  this class. Record the prerequisite and move on.

The standing test for an edge is unchanged and is the whole of it: **A must land
before B.** Scheduling preference is not an edge.

### D14. CAP-18's missing features are a mechanical fix.

Verbatim: *"This is obviously a mechanical fix."*

FEAT-058, FEAT-059 and FEAT-060 are named in #313's required set with no feature
document, no REGISTRY.md row and no filed issue - the whole of that capstone's
11-20 mw marginal band sits in them. The corpus document also claims a registry
of 61 features where only 57 exist.

Write the three documents from CAP-18's own content, add their REGISTRY rows,
file them, and resolve #313's `planned_features` to their numbers. This is
transcription from a source that already exists, not new planning. Do not raise
it as a question again.

### D15. The guest image is a FILE. Files require import-to-subcircuit.

Verbatim: *"File. Files will also require 'import to subcircuit' to nest
systems."*

Settles the #301 vs #343 disagreement in favour of the sidecar file with a
recorded digest, against the in-format optional binary section. Consequence
already priced: #343's position was that a sidecar needs no section mechanism
and therefore does not gate on #319 (FEAT-013), which is what keeps CAP-02's
demo slice near 10 mw rather than 30.

**The second sentence is a scope addition, not a restatement.** Choosing files
as the carrier creates a requirement that did not exist before: an
**import-to-subcircuit** path, so a system carried as a file can be nested
inside another design as a subcircuit. Without it, files compose only at the
top level and "nest systems" is false. This lands as scope on the
subcircuit/definition features - it is not free and must not be absorbed
silently into an existing band.

### D16. Stale corpus documents are mechanical fixes, not questions.

Verbatim: *"These are again mechanical fixes and not actually a question."*

Where a filed issue and its corpus document disagree and the issue has been
verified correct at a named commit, correct the document. Known instances:
`CAP-06-course-delivery-autograding.md` on FEAT-010/FEAT-011 (both shipped);
`FEAT-005-quadratic-and-materializing-io-paths.md:26` grading CAP-17
`beneficial` where three other sources say required-and-first; the consumed-by
tables of FEAT-004, FEAT-013, FEAT-014, FEAT-042 and FEAT-047 carrying no
CAP-18 row though their filed issues all name 313 in `serves_capstones`.

Generalise it: a document/issue disagreement where the issue is verified is a
transcription defect. Fix the document. Do not file an open question, do not
ask, do not carry it as a reconciliation obligation.
