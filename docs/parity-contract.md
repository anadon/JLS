# The parity contract

**Status: proposed normative contract — written, not yet ratified.** It
becomes normative when the `ARCHITECTURE.md` decision block described in
§8.3 is recorded, and not before; until then it binds nothing and no
other document may cite it as settled. Nothing in the repository
currently implements any mechanism it governs (§1.3).

This document specifies what it *means* for the
same software to run on virtual logic and on virtual hardware "with
parity", precisely enough that a test can decide it. It defines the
objects being compared, the alphabet of the comparison, the set of
observations that must be bit-identical, the set that is permitted to
differ, the points at which observation happens, the checks that make
the claim falsifiable, and the circuits a fidelity binding must refuse
rather than approximate.

It is a contract, not a plan. It says nothing about when anything is
built and it does not authorize any mechanism. Where a mechanism it
governs does not exist at HEAD, this document says so in the same
sentence.

Its sibling documents:

- [`simulation-semantics.md`](simulation-semantics.md) stays
  **normative for the event model** — time, ordering, the value
  domain, initialization, propagation, per-element delays, edge
  triggering, tri-state resolution. Nothing here weakens it. Section 8
  states exactly where the two documents meet and what edit
  `simulation-semantics.md` requires before any binding may exist.
- [`batch-interface.md`](batch-interface.md) stays normative for the
  batch surface: the `-t` grammar, the stdout format, and the VCD
  profile, all frozen by its §6.
- [`file-format.md`](file-format.md) stays normative for what a saved
  circuit contains and what costs a `FORMAT` version.
- [`machine-calibration.md`](machine-calibration.md) owns **every
  measured engine constant and every cost figure** quoted here. This
  document never derives one; where it prints a duration it is quoting
  that document, and that document carries the method, the workload and
  the uncertainty band.
- [`virtual-hardware-parity.md`](virtual-hardware-parity.md) is the
  **proposal** that would build the mechanisms this contract governs —
  the layer stack, the milestones, the cost bands and the governance
  actions. It is explicitly non-normative. This document deliberately
  says nothing about when anything is built; that document says nothing
  about what "agree" means.

---

## 1. Scope

### 1.1 What binds

This contract binds any JLS mechanism that offers **two implementations
of one boundary** and claims they are interchangeable. Concretely, it
binds:

1. the comparison alphabet and the harness that consumes it;
2. any per-instance fidelity binding on a subcircuit;
3. any behavioral macro-element that is offered as standing in for a
   drawn design;
4. any future compiled or levelized evaluation pass, **in addition to**
   — never instead of — the equivalence criterion recorded against
   #221 in [`../ARCHITECTURE.md`](../ARCHITECTURE.md).

### 1.2 What does not bind

This contract says nothing about performance, about which tier a user
should run, or about how fast either tier is. It does not make a slow
implementation non-conformant and it does not make a fast one
conformant.

It also does not certify a *single* implementation against reality. Two
implementations that agree with each other and both disagree with the
RISC-V specification satisfy this contract completely. Section 9.3
records that as a known weakness and names the only fix
(an oracle that is not JLS).

### 1.3 What exists at HEAD

Everything in sections 5.1 and 5.2 that is described as *existing* has
a code anchor and was verified on
`claude/jls-virtual-hardware-linux-njsoma` at `36cbd37`. Everything else
does not exist:

| Named here | At HEAD |
|---|---|
| `Simulator.probeSample`, `Simulator.afterEvent`, `Simulator.beforeEvent`, `Simulator.beforeReact` | exist (`src/jls/sim/Simulator.java:285`, `:269`, `:252`, `:261`) |
| `BatchSimulator.probeSample`, `BatchSimulator.afterEvent` | exist and are overrides (`src/jls/sim/BatchSimulator.java:295`, `:140`) |
| `Circuit.getElementsInStableOrder` seeding | exists (`src/jls/Circuit.java:479`; `src/jls/sim/Simulator.java:196`) |
| A fidelity attribute on `SubCircuit` | **does not exist** |
| Any behavioral RV32 model, `RetireRecord`, `Differ`, any parity harness | **do not exist** |
| Any host byte port, any `Console` element | **do not exist** (grep for `System.in` over `src/` returns no matches) |
| Simulation-state serialization of any kind | **does not exist** — `Memory` and `Register` save their *initial* text, never a running store (`src/jls/elem/Memory.java` `save`) |

---

## 2. The objects

Six objects. A parity claim that does not name all six is not a parity
claim.

### 2.1 `D` — the machine definition

`D` is the single description from which both implementations are
supposed to follow. **`D` is the weakest object in this contract**: JLS
has no mechanism that derives two implementations from one artifact,
and none is proposed. Today `D` would be a hand-maintained agreement
between a drawing, a Java model, and a guest configuration. That is
what the industry does, and it is recorded here as a known weakness
(§9.1) rather than assumed away.

Because `D` is maintained by hand, its **contents must be enumerated**,
in one place, or the two implementations will silently disagree about
something that is in neither model. `D` is normatively required to
declare, at minimum:

| Item | Why it must be in `D` |
|---|---|
| The physical memory map, with every device base address and size | Address decode exists in the drawing and again in the model; nothing else compares them |
| RAM size in bytes | Bounds every memory digest in §3.2 |
| The power-on value of every architecturally visible register, including PC | Otherwise "both tiers boot" can mean "both tiers inherit different reset fictions". See §9.4 |
| The declared clock frequency, and the `HZ` and `mtimecmp` width of the timer | Decides where timer interrupts land; see §3.6 |
| The device-tree blob and its hash | It is guest-visible input, and it is not in the circuit |
| The kernel command line verbatim, including `lpj=` and `printk.time=0` | Guest configuration is part of the comparison; see §3.3 |
| The exclusion set `E` (§2.5) | An exclusion that is not in `D` is invisible policy |

**Required check.** One test reads the drawn machine's address decode
and the machine model's constants and fails when they disagree. Without
it, `D` is prose.

### 2.2 The bound boundary, and its two implementations

The unit of parity is a **boundary**, not a program and not a machine.
A boundary is a named region of the elaborated circuit with a declared,
ordered port list. Making parity a property of a boundary is what makes
it testable: the comparison has a finite alphabet, the alphabet is the
port list, and both sides of the comparison are in the same run.

- **Structural implementation `M_L`** ("virtual logic") — the drawn
  contents of the boundary, simulated by the one event loop, with every
  per-element propagation delay intact. This is what JLS does today and
  it is the default.
- **Behavioral implementation `M_H`** ("virtual hardware") — a
  hand-written implementation of the same boundary, computing the same
  function with **one `react()` and one lumped delay at the boundary's
  pins**. This is structurally the same thing `Adder` (delay
  `30 × bits`, `src/jls/elem/Adder.java`), `Memory` (access time 100),
  `TruthTable`, `StateMachine` and the newly shipped `RegisterFile`
  already are: an arbitrary function computed in one `react()` with no
  internal delays (`docs/simulation-semantics.md` §7).

**An unresolved question that this contract cannot settle.** There are two
incompatible answers to *what carries* the behavioral implementation, both
defensible, and the contest between them is recorded in
[`virtual-hardware-parity.md`](virtual-hardware-parity.md) §4 (layer L4) and
§7.8, where it is flagged as a decision the maintainer must take before the
merge that would encode it:

- **Reading A — the fidelity boundary:** a **per-instance saved attribute
  on `SubCircuit`** selecting among a sealed set of implementations of
  *that instance's definition*.
- **Reading B — the registered element:** a **registered `ElementType`**
  compared against a drawn subcircuit with the same port list, on the
  ground that a behavioral RV32 core is not derivable from any drawing and
  is therefore not an "implementation of that definition" in any honest
  sense.

(The labels A and B are the ones
[`virtual-hardware-parity.md`](virtual-hardware-parity.md) §4 L4 uses; they
name the same two readings.) Reading A buys a real legitimacy argument — the
structural referent is in the same file, so the behavioral model cannot
become the only thing that exists. That argument **does not survive at the
CPU boundary under reading B**, where the two sides are two separate
artifacts. The
contract below is written to be checkable under either reading: it
compares *observations at a port list*, and is silent about the carrier.
**The carrier must be decided before any binding is specified**, because
the two readings differ in sealed-permits entries, in registration cost,
and in format cost. It is recorded here as open, not resolved by
omission.

### 2.3 `P` — the program

The guest software, byte-exact, with its provenance: for a Linux boot,
the kernel version, its `.config`, the initramfs contents, the
toolchain, and the checksums of every produced artifact. `P` is not
"Linux"; `P` is one image.

**None of this exists at HEAD and nothing in the repository owns it.**
Building and pinning the guest image — kernel, `.config`, busybox,
initramfs, device tree, reset stub, cross toolchain, rebuild recipe — is
owned by no shipped work item, while three separate commitments in this
contract dereference it (§3.3, §5.4's T2, and every instruction count in
[`machine-calibration.md`](machine-calibration.md) §5.1, which is a property
of one particular image). [`virtual-hardware-parity.md`](virtual-hardware-parity.md)
§6 proposes an owner for it (program P21, 4–6 maintainer-weeks estimated);
that proposal is **not adopted**. A parity claim about a Linux boot is not
checkable until `P` exists and is pinned.

### 2.4 `I` — the input log, indexed by retirement

Host input is **not** timestamped in seconds, in simulated time, or in
cycles. It is timestamped in **retirement index**: "the byte `0x6C`
arrives before instruction 41,203,118 retires and after 41,203,117
retires."

This is the single most important design choice in the contract, and it
is what makes the two tiers comparable at all. The two implementations
retire the same instructions at wildly different simulated times and at
different cycles per instruction. Any input log keyed on time or cycles
delivers its bytes at different points in the instruction stream on the
two tiers, and the runs diverge for a reason that is not a bug.

Consequences, all binding:

- A recorded interactive session replays as a retirement-indexed log.
  **The recording, not the session, is the contract.** Replay is
  deterministic, threadless and reproducible; a live session is none of
  those.
- The replay invariant is testable directly: replay the same log with
  the `Clock` period changed and the guest output bytes must be
  identical while simulated time differs. A test that fails when
  someone quietly re-indexes the log in nanoseconds is the point.
- Any input that cannot be expressed in retirement index — a real
  wall-clock deadline, a host device that runs on its own schedule — is
  outside this contract. There is no clause that admits it.

### 2.5 `E` — the exclusion set

`E` is the enumerated set of architectural state that is **excluded
from the comparison, by name, with a reason**. It is normative and it
is small.

| Excluded | Reason |
|---|---|
| `mcycle` | Counts cycles. All timing may differ (§4) |
| `minstret` | Instruction counting is compared directly per §3.5; the CSR is redundant and is written at different points in the two tiers |
| `mtime` | Driven from simulated time, which differs by construction |
| `mtimecmp` | Written by the guest as an offset from `mtime` |

Rules that make `E` an exclusion set rather than an escape hatch:

1. **`E` is ratcheted.** A test asserts its exact contents, so `E` may
   shrink freely and cannot grow without a diff a reviewer sees and a
   CHANGELOG entry.
2. **`E` is printed in every parity report.** An exclusion that a user
   cannot see is policy hidden in comparison code.
3. **Excluding a field does not exclude its effect.** This is not a
   technicality; see §3.6, where it decides the whole comparison regime.

### 2.6 The sync point

A **sync point** is a declared instruction index at which both
implementations must present full architectural state. It is an index,
never a time, never a cycle, never a wall-clock instant. Sync points
are declared in `D` and are part of the contract; a run that reaches
different sync points on the two tiers has already failed.

---

## 3. What MUST be bit-identical

Given the same `D`, `P` and `I`, the following are required to be
bit-identical between the structural and behavioral implementations.
Anything not listed here is either permitted to differ (§4) or is not
observed at all.

### 3.1 The ordered per-retired-instruction architectural state delta

One record per retired instruction, monotonically indexed, no gaps and
no reuse. This is the **comparison alphabet**, and it is RISC-V
Formal's RVFI field list, adopted deliberately because it is the field
list the RISC-V verification ecosystem already compares against
(riscv-formal, RVVI, the Ibex and CVA6 co-simulation flows):

| Field | RVFI name | Meaning |
|---|---|---|
| `order` | `rvfi_order` | Retirement index. Strictly increasing by one, no gaps, no reuse |
| `pc_before` | `rvfi_pc_rdata` | PC of the retired instruction |
| `pc_after` | `rvfi_pc_wdata` | PC of the next instruction to retire |
| `insn_word` | `rvfi_insn` | The instruction word |
| `rd_index` | `rvfi_rd_addr` | Destination register, 0 when none |
| `rd_value` | `rvfi_rd_wdata` | Value written, 0 when `rd_index` is 0 |
| `mem_addr` | `rvfi_mem_addr` | Accessed address when either mask is non-zero |
| `mem_rmask` | `rvfi_mem_rmask` | Byte lanes read |
| `mem_wmask` | `rvfi_mem_wmask` | Byte lanes written |
| `mem_wdata` | `rvfi_mem_wdata` | Data written; only lanes selected by `mem_wmask` are compared |
| `privilege` | `rvfi_mode` | Privilege level at retirement |
| `trap` | `rvfi_trap` | Set when the instruction traps; see §3.4 |

**The record is a Java `record` with no field for cycles, simulated
time, pipeline state, or cache state.** This is the enforcement
mechanism, not a stylistic preference: §4's *permitted to differ* set
is made **unrepresentable by the type**, so over-constraining parity is
a compile error rather than a code review finding.

**RVFI fields deliberately not adopted, and what that costs:**

| RVFI field | Why not | The cost, stated |
|---|---|---|
| `rvfi_rs1_addr`, `rvfi_rs2_addr`, `rvfi_rs1_rdata`, `rvfi_rs2_rdata` | Diagnostic; not needed to detect divergence | An instruction that reads the wrong source register but happens to produce the right result is invisible **until the divergence propagates to a written value**. It is not invisible forever, but the reported first-divergence index will be later than the true one |
| `rvfi_halt` | JLS runs terminate through the batch outcome line and the time limit, not through a halt signal | None material |
| `rvfi_intr` | Derivable: `intr` is exactly `pc_before != previous.pc_after` | None |
| `rvfi_ixl` | RV32 only; constant | None until RV64 |
| `rvfi_mem_rdata` | Read data is compared where it matters — through `rd_value` and through the memory digest at sync points | A load that reads the wrong data and discards it is invisible. Accepted |
| The `rvfi_csr_*` family | CSR state is compared at sync points (§3.2), not per instruction | A CSR that is written wrongly and read back before the next sync point produces a divergence attributed to the *reading* instruction, not the writing one |

Trace files are untimestamped canonical text; the comparison is a
`diff`, and the first differing record is the answer.

### 3.2 Architectural state at every declared sync point

PC; all GPRs; every implemented CSR minus `E`; and a digest over the
non-volatile memory regions declared in `D`. Compared at each sync point
and at exit.

### 3.3 The guest-visible output byte stream

Every byte the guest emits through the console device, in order.

**This is the parity clock for any run too long to trace per
instruction**, and it is the only observation in this contract that
works at Linux scale, because it is keyed on the guest's own output
rather than on time. The same expect-style script drives a short
behavioral boot and a multi-hour structural boot for exactly that
reason.

**It is not tier-independent by default, and any claim that it is, is
false.** A default kernel configuration stamps every message with a
timer-derived timestamp (`CONFIG_PRINTK_TIME`), and the two tiers reach
any given instruction at different simulated times by construction —
which is precisely what §4 permits. The byte streams then differ on
their first line. Therefore `D` **must** declare, and `P` must be built
with, at minimum:

- `printk.time=0` on the command line, or `CONFIG_PRINTK_TIME=n`;
- a pinned `lpj=` so no delay-loop calibration result reaches the
  console;
- a fixed hostname and a prompt containing no date, time, or uptime.

Every remaining source of time-derived output — BogoMIPS reporting,
entropy-init ordering, dmesg interleaving — must either be removed by
configuration or added to `E` with a reason.

**The settling experiment that must run before this mechanism is
relied on:** boot the pinned image on a reference emulator at two
declared clock rates and diff the two console streams. If they are not
byte-identical, the guest configuration is not yet time-free and the
parity clock does not work. This is an afternoon of work and it gates
everything built on §3.3.

### 3.4 Trap occurrence and cause, attributed to the causing instruction

A trap must be reported by both implementations at the same retirement
index with the same cause. Attribution is part of the requirement: a
trap reported one instruction late is a failure, not a rounding.

Interrupts are **not** covered by this clause. An interrupt is not
caused by the instruction it lands on; where it lands depends on timing,
which §4 permits to differ. See §3.6.

### 3.5 Retired-instruction count between sync points

Exact. This is the clause that catches an implementation which reaches
the right state by executing a different number of instructions.

### 3.6 The interrupt problem, and the two comparison regimes

Excluding `mtime` from the alphabet (§2.5) does **not** exclude its
effect. Both implementations drive the timer from *simulated* time.
They retire at different rates. A timer interrupt therefore lands
**between different instructions** on the two tiers, injecting trap
records at different retirement indices and changing every subsequent
PC. At a declared 1 MHz clock with `HZ = 100`, a tick lands roughly
every 10⁴ cycles — inside any handover window and inside any Linux-scale
comparison.

There is no way to make §3.1 hold across an interrupt without coupling
the two models by a hand-maintained per-instruction cycle budget. That
option was considered and is rejected: under a single maintainer, "real
bug or stale budget entry?" is exactly the ambiguity that destroys trust
in a differential suite.

**Therefore the contract has two regimes, and every parity claim must
name which one it is making:**

| Regime | Applies to | Required identical | Not compared |
|---|---|---|---|
| **Trace regime** | Interrupt-free, deterministic programs: architecture tests, directed programs, the fuzz corpus | §3.1 in full, plus §3.2, §3.4, §3.5 | — |
| **Stream regime** | Interrupt-bearing and OS-scale runs | §3.3 in full, plus §3.2 at sync points computed **by the guest** (a checksum run in the guest, arriving over the console) | §3.1. The per-instruction trace is not compared |

The stream regime needs no memory-introspection API, no checkpoint, and
no new observation mechanism — which is why it is the one that works at
Linux scale. What it gives up is stated plainly: **in the stream regime,
a divergence is localized to a console byte, not to an instruction.**
Instruction-level localization exists only in the trace regime, and that
is where instruction-level bugs are actually found.

---

## 4. What is PERMITTED to differ

Each row carries its industrial precedent. This is not a list of
tolerated defects; it is the boundary between architecture and
implementation, drawn where the industry draws it.

| Permitted to differ | Precedent |
|---|---|
| **All timing**: cycles, elapsed simulated time, cycles per instruction, interrupt latency, the wall-clock duration of a run | ARM's Fast Models documentation states that its programmer-view models are not cycle-accurate and that software "must not rely on the accuracy of cycle counts, low-level component interactions" (ARM Fast Models User Guide v11.28, vendor documentation). ARM ships this as the *normal* way to run production software on a model |
| **All microarchitectural state**: pipeline contents, caches, TLBs, branch predictors, in-flight bus transactions, store buffers | gem5 (https://www.gem5.org/) does not attempt to preserve it: it refuses — panics — rather than checkpoint its classic caches, and explicitly flushes TLBs at checkpoint handover rather than pretend the state is portable. ARM's programmer-view models do not model caches at all |
| **Event ordering finer than the sync quantum**, and asynchronous interleaving between devices | Standard practice in transaction-level modeling: TLM-2.0's loosely-timed coding style exists to permit exactly this |

**The measurement that makes the first row concrete.** During the design
study that produced this document, one SystemC TLM-2.0 model was run at
six abstraction levels. All six produced a **bit-identical final memory
image** (FNV-1a `35d5d215e8dd3b83`) while reporting 170 M, 200 M and
60 M ns of simulated time — a **2.8× spread in reported time with zero
divergence in architectural result, and no warning emitted by anything**.
That is this contract in one experiment: the architectural result is the
invariant; the time is not.

*Provenance:* that measurement was made during the study, outside this
repository, and is **not reproducible from this repository**. It is
cited as corroborating evidence for a position that the vendor
documentation above already states normatively; nothing in this contract
depends on it.

---

## 5. Observation points and the harness

### 5.1 The four observation points, in JLS terms

The contract is checkable because JLS already has the seams. Three of
the four points reuse an existing observation channel rather than
adding a parallel one.

| # | Point | Mechanism | Granularity | Exists at HEAD? |
|---|---|---|---|---|
| 1 | **Fidelity boundary** | The bound region's pins, sampled by the boundary harness | Settled output word per sampling instant — **indexed, not timestamped** | Harness: no. Pins: yes |
| 2 | **Retirement** | Reserved-name nets sampled through `probeSample` | One record per retired instruction | Channel: yes. Records: no |
| 3 | **Guest output** | The console device's byte stream | Bytes, in order | No console device exists |
| 4 | **Sync points** | Architectural-state digest through the binding's declared state map | Per declared quantum and at exit | No |

**Point 2 is the load-bearing one, and it costs nothing in `jls.sim`.**
`Simulator.probeSample` is a no-op hook
(`src/jls/sim/Simulator.java:285-287`) called from `WireNet.propagate`
whenever a probed net settles, guarded by one field check per wire
(`src/jls/elem/WireNet.java:518-527`). `BatchSimulator` already
overrides it to accumulate VCD traces
(`src/jls/sim/BatchSimulator.java:295-312`). A retirement-trace recorder
is the same construction: a `Simulator` subclass overriding
`probeSample` and `afterEvent`
(`src/jls/sim/Simulator.java:269-270`, overridden at
`src/jls/sim/BatchSimulator.java:140-180`), with **zero change to
`jls.sim`**. This is not a proposal for a new seam; it is the mechanism
`BatchSimulator` already ships.

**Why the comparison is meaningful at all.** Event ordering in JLS is a
pure function of circuit content: same-time events fire FIFO by posting
order, and the time-0 seed walks the circuit's canonical stable-id order
rather than a hash order (`docs/simulation-semantics.md` §3;
`src/jls/sim/Simulator.java:186-200`;
`src/jls/Circuit.java:479-485`). Without that property a differential
harness would be comparing two nondeterministic runs and every result
would be noise.

**One determinism assumption remains unverified**, and this contract
depends on it: nothing in the tree asserts that a simulation is
bit-identical across a JDK upgrade or across operating systems.
[`reproducibility.md`](reproducibility.md) covers *build*
reproducibility — the jar and its bill of materials — and says nothing
about runs. The experiment that settles it is cheap and the platforms
already exist as CI jobs: run one circuit on all three and diff the
VCDs. Until it is run, "bit-identical" in this document means
bit-identical on one platform.

### 5.2 The harness

The equivalence check is a JUnit-runnable harness, not a report.

**Boundary equivalence** — run the same stimulus through both
implementations of a boundary and compare the observation function:

- **Exhaustive** where the boundary's total input width is 16 bits or
  fewer.
- Above that: a **seeded pseudorandom vector sample** plus **declared
  corner vectors** — width edges 1/31/32/33/63/64/65, HiZ, undriven,
  all-ones, all-zeros — with the seed recorded in the failure report so
  a failing run is reproducible from its output alone.
- The comparison is over **settled output words per sampling instant,
  indexed and not timestamped**. Sampling instants are quiescence points
  or edges of a declared sync net.

**Trace equivalence** — run `P` through both implementations, emit two
retirement traces, and diff them. On divergence the harness reports the
**first differing retirement index, both records side by side, and the
names of the fields that differ**. First-divergence reporting is the
whole product: a differ that reports "traces differ" has done nothing a
`cmp` could not.

**A reflective or bytecode guard** asserts that no implementation of a
boundary touches the event queue: `Simulator.post` permitted;
`eventQueue`, `dupCheck`, `poll` and `runEventLoop` forbidden. This is
the mechanical proof that a behavioral binding is a *model* change and
not a second execution strategy — a fact rather than a claim, and it is
what §8 rests on.

**The exclusion-set ratchet** — one test asserts that the code's
exclusion set is exactly the table in §2.5, in both directions. A
mismatch is a build failure.

### 5.3 The deliberately-failing null test

**A differential harness with no null test passes vacuously, and is
worse than no harness, because it converts an unexamined assumption
into a green check mark.**

Therefore: alongside every parity test, a **knowingly wrong
implementation** that the harness **must** reject. It ships in the test
tree, it is committed, and its test asserts *failure*.

Two requirements make it real rather than ceremonial:

1. **The null implementation must be subtly wrong, not obviously
   wrong.** An implementation that returns constant zero proves only
   that the harness is connected. The null must be wrong in the way real
   implementations are wrong: one instruction's sign extension, one
   byte lane in a store mask, one CSR's write-side-effect, an off-by-one
   in a branch target.
2. **The test asserts the diff report text, not the boolean.** Asserting
   only `assertFalse(agrees)` leaves the reporting path — where the
   value is — untested, and mutation testing will find survivors there.
   Assert that the report names the right retirement index and the right
   field.

A useful shape is a small family of nulls, one per class of defect, so
that "the harness catches this" is a statement about coverage rather
than about one example.

### 5.4 Where each check runs

| Tier | Cadence | Content | Cost |
|---|---|---|---|
| **T-null** | every push | The deliberately wrong implementations the harness must reject | milliseconds |
| **T0** | every push | Boundary equivalence on every binding (exhaustive ≤16 bits, else seeded + corners) | seconds |
| **T1** | every push | Architecture tests and the fuzz corpus through both implementations; trace files byte-identical | seconds |
| **T2** | nightly | Behavioral boot to prompt, console byte stream diffed against a frozen golden (stream regime) | Estimated **~2.5 min** at 12 events per retired instruction — a **modeled** figure, never measured. It is **not** immune to engine constant-factor work: it divides by the same 318 ns/event constant that work multiplies, so it falls to ~1.1 min after the semantics-preserving stack ([`machine-calibration.md`](machine-calibration.md) §4.4, §4.5) |
| **T3** | release cadence, by hand | Structural boot under the same script, byte stream diffed against T2's | Estimated **~1.7 h** at central inputs; **honest band 1.2–6 h** until the per-cycle active fraction `α` and the events-per-active-element constant `k` are measured ([`machine-calibration.md`](machine-calibration.md) §4.2, §4.4) |

**T3 is not in continuous integration, and this is an accepted cost
stated plainly, not papered over.** The headline result — the drawn
machine booting the same image — is verified by a human, in a project
whose constraints otherwise demand test-enforced checks. It is mitigated
by T0/T1 checking both implementations on every push, and it is recorded
in the CHANGELOG with the commit SHA of the run. A nightly cron trigger
with a documented single-lane convention already exists
(`.github/workflows/ci.yml`), so the missing pieces for T2 are a
`timeout-minutes` line, a large-fixture policy, and the pinned guest
image `P` — not the lane.

**Every cost figure in this table is an estimate carrying an unmeasured
input**, and each is labeled above. None of them is a property of this
contract, and none is derived here — they are quoted from
[`machine-calibration.md`](machine-calibration.md) §4, which owns their
method, so that nobody plans against a number whose uncertainty is
invisible.

---

## 6. What a binding must refuse

A fidelity binding **refuses**, loudly and by element path, rather than
degrading silently. Silent degradation is the failure mode that makes a
mixed-abstraction simulator untrustworthy, because the user cannot tell
which parts of the answer are real.

Every refusal names the offending element by its dotted qualifier — the
same path vocabulary the watched-element report uses
(`batch-interface.md` §3) — and refuses the **run**, not the element.

### 6.1 Observation crossing the boundary

- **A probe or a watched element strictly inside a bound region.** The
  behavioral implementation does not have the net the probe names; there
  is no honest value to report. Emitting nothing is a silent hole in a
  VCD and emitting a fabricated value is worse.
  *Refusal:* `cpu.alu.carry: probed net inside a boundary bound to a
  non-structural implementation; unbind the boundary or remove the
  probe`.
- **Any request for intra-cycle observation order inside a bound
  region.** It does not exist there; see §7.

### 6.2 Circuits whose meaning is timing

- **A tri-state net crossing the boundary.** Multi-driver resolution
  happens in `WireNet.propagate` (`docs/simulation-semantics.md` §9),
  outside the bound element — so tri-state resolution *at* the boundary
  is fine and unchanged. A tri-state net whose drivers are on both
  sides of the boundary is not: the behavioral side has no per-driver
  turn-on and turn-off times to resolve against.
- **A `TriState` whose behavior depends on turn-off relative to
  turn-on.** `TriState.react` uses an explicit `"off"` sentinel event
  (`docs/simulation-semantics.md` §3), and the ordering of that event
  against the turn-on is a timing phenomenon.
- **A `DelayGate` used as a delay line.** Its default delay is
  user-specified at creation and it is exempt from delay reset
  (`docs/simulation-semantics.md` §7). A circuit that uses it to
  construct a pulse is *computing with time*, and a settled-value
  implementation has deleted the computation.
- **A level-sensitive `Memory` write inside the boundary.**
  `simulation-semantics.md` §8.4 documents a glitch hazard on
  level-sensitive writes; quietly removing it under a behavioral binding
  would teach a student that #199's bug does not exist. Synchronous
  (`sync`) writes are fine.

### 6.3 Circuits with no settled value

- **More than one incommensurable `Clock` inside the boundary.** There
  is no single sampling instant to be settled at.
- **A region that does not settle.** A combinational cycle with no fixed
  point oscillates; the binding must report the oscillating set of
  elements by name rather than hang. A cross-coupled latch *does* have a
  fixed point — its settled value — and must not be refused; a
  metastable pair does not, and must be. The honest limit belongs in the
  diagnostic: **inside such a region a fixpoint reproduces the settled
  value, not the timing.**

### 6.4 State the harness cannot map

- **A boundary whose implementation cannot enumerate its architectural
  state.** Handover (§9.5) maps exactly one boundary's state; a binding
  that cannot say what its state is cannot be handed over, and must
  refuse the handover rather than resume from an unstated one.

### 6.5 One thing that must *not* be refused

**A boundary is not refused for containing an element that the HDL
exporter cannot render.** These are unrelated capabilities, and
conflating them would refuse the machine this contract is written for:
`HdlExporter.EXPORTED` lists exactly 22 element classes
(`src/jls/hdl/HdlExporter.java:422-428`) and includes **neither
`RegisterFile` nor `FieldExtend`** — both shipped, both ordinary
simulating elements. A separate consequence follows and is recorded
here because it is easy to get wrong: **no claim may be made that a
drawn machine built on `RegisterFile` round-trips to Verilog.** It does
not, at HEAD.

---

## 7. Naming: the axis is not gate-versus-compiled

The distinction this contract draws is **not** gate-level versus
compiled, and it is not slow versus fast. Those framings are wrong about
JLS in a way that has already misled readers.

JLS is a **word-level** simulator. A 32-bit `Adder`, a 32-input `Mux`, a
`Register`, a `Memory` and a `RegisterFile` are each **one element with
one `react()`**; ripple-carry survives only as a delay expression
(`30 × bits`, `src/jls/elem/Adder.java`). JLS has therefore been a
mixed-abstraction simulator since day one, and "descend to gates" is not
one end of this axis.

**The real axis is:**

> **event-accurate with per-element propagation delays**
> versus
> **settled value per sampling instant.**

Both ends are exact. Neither is approximate. What differs is *what is
observable*, and the whole content of this contract is the enumeration
of that difference.

**What the settled-value end gives up, exactly and exhaustively:**

1. **Combinational transport delay strictly inside the boundary.** The
   time at which an internal net takes an intermediate value.
2. **Intra-cycle observation order inside the boundary.** Which internal
   net settled first.
3. **Glitches inside the boundary.** A transient that appears and
   resolves before the sampling instant is not observable.
4. **Waveforms of internal nets.** They do not exist; see §6.1.

**What the settled-value end does NOT give up** — and this must be
stated affirmatively, or an unstated fifth surrender becomes available
by omission:

1. **The value domain is unchanged** — two states plus HiZ, exactly as
   `simulation-semantics.md` §2 specifies.
2. **Multi-driver and tri-state resolution are unchanged**, because they
   happen in `WireNet.propagate`, outside the bound element
   (`simulation-semantics.md` §9).
3. **Edge-triggering semantics are unchanged** (`simulation-semantics.md`
   §8). Sequential elements keep their own delays.
4. **Delays outside the boundary are unchanged.** The boundary itself
   carries one lumped delay at its pins, exactly as `Memory` carries
   `accessTime`.
5. **The event loop is unchanged.** One queue, one post/react
   discipline, one authority on time.

A reader who wants the four losses in one sentence: *inside a bound
boundary you may ask what the answer is, and you may not ask when the
answer arrived.*

---

## 8. Relation to `simulation-semantics.md` and to #221

### 8.1 `simulation-semantics.md` stays normative

Nothing in this contract changes the event model.
[`simulation-semantics.md`](simulation-semantics.md) remains normative
for time, ordering, values, initialization, propagation, delays, edge
triggering and tri-state resolution. This document specifies a
comparison; that document specifies what is being compared.

### 8.2 The one edit `simulation-semantics.md` requires

The normative delay table in `simulation-semantics.md` §7 assigns the
**subcircuit boundary a propagation delay of 0**
(`docs/simulation-semantics.md:285`). A behavioral binding gives the boundary a
lumped delay at its pins. That is a change to a normative table and it
must be made in that document, as a stated exception, **before any
binding exists** — not inferred from this one. A boundary's delay must
be discoverable from the normative delay table, or a reader computing a
circuit's timing from the specification computes a wrong answer.

A related in-tree defect, recorded here because it is the same class and
was found while writing this document: **`RegisterFile` and `FieldExtend`
appear in neither the delayed list nor the zero-delay set of
`simulation-semantics.md` §7**, while both save and expose an editable
delay attribute that has no simulated effect — both propagate at `now`
(`src/jls/elem/RegisterFile.java:559`,
`src/jls/elem/FieldExtend.java:478`). The normative delay table is
incomplete at HEAD, and anyone deriving a zero-delay closure from it
will derive a wrong one.

### 8.3 #221 — the recorded sole-strategy decision

[`../ARCHITECTURE.md`](../ARCHITECTURE.md) records Option 1 of #221: the
`jls.sim.Simulator` event-queue interpreter is JLS's **only** simulation
execution strategy, and any future pass is bound by an equivalence
criterion over four axes — the two-states-plus-HiZ value domain and
multi-driver resolution (§2, §9), edge triggering (§8), per-element
propagation delays (§6, §7), and bit-for-bit agreement with the RV32I
integration golden as a differential oracle.

This contract sits in a definite relationship to that decision, and the
relationship differs by implementation kind. **Getting this wrong in
either direction is the largest governance risk in the program**, so it
is stated in full:

| Implementation kind | Does it engage #221? | What is required |
|---|---|---|
| **`behavioral`** — hand-written, one `react()`, one lumped boundary delay | **Argued not to.** It is what `Adder`, `Memory`, `TruthTable`, `StateMachine` and `RegisterFile` have always been; the event loop, the queue, the post/react discipline and every delay outside the boundary are untouched, and §5.2's queue guard proves it mechanically | A **new recorded decision** in `ARCHITECTURE.md`, with its rationale and its revisit trigger, plus the §8.2 edit. Not an amendment to #221 |
| **`levelized` / compiled** — an evaluation order derived automatically from the drawing, replacing intra-timestamp event ordering | **Yes, directly.** `ARCHITECTURE.md` says in terms that "no levelized/compiled evaluation pass (the Verilator/CXXRTL elaborate-to-flat approach) is built now", and `simulation-semantics.md` §3 normatively specifies the intra-timestamp order such a pass replaces — an order that is **observable today**, since `probeSample` is fed per-propagation from `WireNet.propagate` | The recorded reopening process, in order: file the follow-up implementation issue that `ARCHITECTURE.md` says "deliberately does not exist yet"; present measured trigger evidence from the real machine; propose a **named second conformance level** in `simulation-semantics.md`, stating affirmatively that the §2/§9 value domain and the §8 edge semantics are unchanged and that only intra-cycle observation order and per-element delay are surrendered; bind the new strategy to two oracles; ship it opt-in behind a classifier that refuses circuits where the weakening is observable |

Two things this document explicitly refuses to do:

1. **It does not self-certify a levelized pass as "not a second
   strategy".** A document that both proposes a mechanism and rules that
   the mechanism does not engage a recorded decision is not evidence
   about that decision. Under a single maintainer, the ruling must be
   recorded against #221 itself — either as a note stating that
   zero-delay closure with byte-identical goldens sits inside Option 1,
   with its argument and its gate, or through the full reopening
   process. **Silence is neither.**
2. **It does not treat `simulation-semantics.md` as having a
   conformance-level concept today.** It has none. A named second
   conformance level is a **new normative structure**, and the proposal
   that introduces it must say so rather than imply it is filling an
   existing slot.

### 8.4 The RV32I golden is a live dependency of all of this

#221's equivalence criterion names bit-for-bit agreement with the RV32I
integration golden as its differential oracle. That golden is
`test/jls/RiscvCpuGoldenTest.java` against `test/fixtures/riscv-sum1to10.jls`
— both **tracked**, an ordinary `@Test` with no disable or tag, run by
every `mvn verify`, 34 simulated cycles and four value assertions. It
was executed while writing this document and passes.

Its javadoc points at `riscv/examples/sum1to10.s` and `riscv/README.md`
for regeneration (`test/jls/RiscvCpuGoldenTest.java:25`, `:38`). Both
live in a directory scheduled for deletion. **If the regeneration path
is deleted without being re-homed first, #221's binding criterion
becomes unexercisable, and with it every conformance argument in §8.3.**
The two javadoc references are `{@code}` spans, not `{@link}`, so the
`-Werror` doclint gate will not catch the rot: it will be silent.

---

## 9. Known weaknesses

Recorded, not solved. A contract that hides its holes is worth less than
one that names them.

### 9.1 `D` has no mechanism

Nothing derives the behavioral model and the structural drawing from one
artifact. They are two hand-maintained things asserted to agree by a
test. This is what the industry does and it is workable — but the test
is then the *only* thing holding them together, and §2.1's contents
table plus its address-decode check are the minimum that makes the
assertion mean anything.

### 9.2 The device models are outside the boundary mechanism

The console, the UART and the timer are **elements**, not subcircuits.
A per-instance fidelity boundary on `SubCircuit` therefore structurally
cannot hold two implementations of a UART equal — and the console byte
stream is precisely the observable §3.3 designates as the OS-scale
parity clock. Any design that ships a behavioral UART model *and* a
drawn one needs a mechanism relating them, and this contract does not
supply one. Naming it is the minimum; the alternative is a parity claim
whose oracle is itself unchecked.
[`virtual-hardware-parity.md`](virtual-hardware-parity.md) §4 (layer L4)
records the same gap against its own proposed mechanism and does not close
it either.

### 9.3 Both implementations can be wrong together

This contract compares two implementations to each other. Nothing in it
compares either to the RISC-V specification. The fix is an oracle that
is not JLS — the formal ISA model and the compliance framework the
RISC-V ecosystem already uses — and it has no home and no cost estimate
in this program. A JLS-authored reference emulator **must not** serve as
that oracle: it is written by the same author as the design under test,
which is exactly the correlation an independent golden exists to break.

### 9.4 The reset fiction

`Simulator` seeds a simulation from the circuit's stable-id order and
JLS supplies initial input values. If the drawn machine boots only
because of that supplied reset behavior, then "the structural tier
boots" is unsound, and `D` would pin a fiction into the contract as
though it were the machine's power-on state. **Whether the machine boots
without it is unmeasured**, it is hours of work to find out, and §2.1
requires the answer in `D` either way.

### 9.5 The handover transient is a declared blind spot

When a run hands the next window of cycles from the behavioral
implementation to the structural one, the structural side resumes with
an **empty event queue**. Its first cycles are a settling transient with
no counterpart on the behavioral side.

The handover instant is therefore **declared a sync point**, and
comparison resumes at the next retirement. **A bug whose only symptom
lives in that transient is invisible to this contract.** This is stated
rather than mitigated, and it is the same hole gem5 has — which is why
gem5 flushes TLBs at handover rather than pretend the state is portable.

### 9.6 The live-console refusal

**A live console on the structural tier is refused.** Not "not yet" — it
is outside the budget of the discrete-event interpreter by a factor that
no stack of semantics-preserving constant-factor work closes. The
numbers, which this document quotes rather than derives
([`machine-calibration.md`](machine-calibration.md) §4.6): the drawn
machine runs at **18,800–19,500 simulated cycles/s** today and at
**52,600–95,400** after the full optimization stack, against a
**10⁵–10⁶ cycles/s** requirement — the 10⁵ floor missed by **1.05–5.1×**
and the 10⁶ end by **10–51×**. Every per-character figure behind those
numbers assumes a **10⁴-instruction tty echo path that nobody has
measured**; at the 10⁵ end of the available band each multiplies by ten.

The refusal is narrower than it once was and its justification has
changed: the gap is a decision and a large amount of engineering, not a
physical limit. Any figure in maintainer-weeks for that engineering is
currently unsourced — **no work breakdown exists**. But nobody may
promise it, and nothing in this contract depends on it.

What replaces it is real and is the industry's own answer: **bounded
handover windows**. Toggle one boundary at a declared instant, run the
drawn logic for a bounded window with waveforms, starting from the exact
architectural state the interactive tier produced. That is gem5's
restore-with-a-different-CPU flow and ARM's model-then-RTL flow, not a
consolation prize.

### 9.7 Every latency figure in the corpus is measured on the wrong simulator

All of them are measured on `BatchSimulator`. A live console runs on
`InteractiveSimulator`, which does additional per-event work. **No
measurement of `InteractiveSimulator`'s per-event cost exists**, so every
interactive figure anywhere in this program is an upper bound of unknown
tightness. The experiment is one afternoon: run an existing fixture
under `InteractiveSimulator` with the same event counter.

### 9.8 Open questions this document deliberately does not answer

1. **Which carrier holds the behavioral implementation** (§2.2). Two
   readings, both defensible, different costs. For the maintainer.
2. **Where host input is drained.** Inside a device element's `react`,
   or at `Simulator.beforeEvent`. These are **not equivalent for
   determinism**: draining inside `react` makes an element's output
   depend on host arrival timing *within* an event; draining at
   `beforeEvent` confines it to a declared loop boundary
   (`src/jls/sim/Simulator.java:252-255`). The second is the defensible
   one. It must be decided before either is specified.
3. **Whether an event dropped past the time limit is a defect.** An
   event polled past `maxTime` is removed from the duplicate-check set
   *before* the limit test, then discarded without reacting and without
   being re-queued (`src/jls/sim/Simulator.java:224-233`). The eviction
   happening before the limit test is the dangerous part: a resumed run
   retains no record that the event existed. This must be adjudicated
   before any checkpoint or handover work, because both would inherit
   it.
4. **Whether simulation is deterministic across JDK versions and
   operating systems** (§5.1). If it is not, that finding outranks most
   of this document.

---

## 10. Terms

| Term | Meaning here |
|---|---|
| **Virtual logic** (`M_L`) | The structural implementation: drawn contents, one event loop, per-element delays intact |
| **Virtual hardware** (`M_H`) | The behavioral implementation: one `react()`, one lumped boundary delay |
| **Boundary** | A named region of the elaborated circuit with a declared, ordered port list — the unit of parity |
| **Sampling instant** | A quiescence point or an edge of a declared sync net; indexed, never timestamped |
| **Sync point** | A declared *instruction index* at which full architectural state is compared |
| **Retirement index** | A strictly increasing per-instruction counter; the only clock this contract admits |
| **Trace regime** | Per-instruction comparison; interrupt-free programs only (§3.6) |
| **Stream regime** | Console-byte comparison; interrupt-bearing and OS-scale runs (§3.6) |
| **`E`** | The enumerated, ratcheted, printed exclusion set (§2.5) |
| **Null test** | A knowingly wrong implementation the harness must reject (§5.3) |

---

## 11. The one-sentence test of this document

If a reader can look at a green build and say exactly which of the five
identity clauses in §3 were checked, on which programs, under which
regime, with which exclusions, and can name the wrong implementation the
harness rejected to earn that green — then this contract is doing its
job. If any of those is unanswerable from the output, the harness is
passing vacuously and §5.3 has not been honored.
