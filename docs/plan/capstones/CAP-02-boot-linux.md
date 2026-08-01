# CAP-02 - Boot a CLI-only Linux distribution and run commands

**Status:** proposed | **Priority:** 16 | **Marginal cost:** 32-58 mw |
**Standalone cost:** 155-250 mw

## Outcome

A CPU drawn in the JLS editor boots a pinned mainline Linux kernel to a shell
prompt and answers typed commands, and the transcript is byte-compared against
the same golden the behavioral tier produces - which no drawn logic simulator
has ever done.

## Acceptance test

**Scope, fixed so the test is unambiguous.** *A CLI-only Linux distribution* is a
pinned Linux 6.5.12 RV32IMA nommu kernel plus a busybox initramfs in 16 MiB -
not Debian or Alpine, which force Sv32, S-mode, OpenSBI and ELF and take the
structural boot from ~1.7 h to ~4.0 h. *Boot* is a shell prompt at 4.18e7
retired instructions, not `Run /init` at 2.93e7; 27% of the boot happens after
init starts. *Run commands* means at least one command whose output is a pure
function of the guest image - `uname -a`, `ls /`, `cat /proc/cpuinfo` -
explicitly not `date`, `uptime` or `dmesg`, because simulated time is a
permitted divergence.

**SEEN.** A person opens `machines/rv32-soc.jls` - about ten top-level boxes; it
looks like a computer - presses Run, watches the kernel log for about 2.5
minutes, gets `/ #`, types `uname -a` and is answered.

**CHECK - two tests sharing one golden. The sharing is the parity claim.**

- **AT-C2-I** (interactive tier, nightly, ~2.5 min).
  `jls -b machines/rv32-soc.jls -console replay:boot.itlog -d 0 --transcript out.txt`
  then `cmp out.txt test/fixtures/c2/boot.golden`. `boot.itlog` is timestamped in
  **retirement index**, never in seconds, cycles or simulated time.
- **AT-C2-S** (structural tier, release cadence, by hand). The **same file**,
  one attribute flipped (`--fidelity cpu=structural`), headless, then
  `cmp build/boot-structural.txt test/fixtures/c2/boot.golden` - **the same
  golden**. Not a CI job: 1.2-6.0 h fits no hosted lane at the pessimistic end.
  It is a CHANGELOG entry carrying the run's commit SHA.

**FALSIFICATION GUARD, and it is the cheapest test in the plan.** Re-run AT-C2-I
with the `Clock` period changed 10x. Output bytes must be **byte-identical**
while simulated time differs. If that fails, the golden encodes time and AT-C2-S
is impossible. About one week; it must be funded **with** its rung, never after.

**Carried on every push instead of the boot.** T-null (knowingly-wrong bindings
the harness must reject - subtly wrong, not constant-zero, asserting the report
**text**); T0 (boundary equivalence on ALU, register file, decode, LSU, CSR,
CLINT - exhaustive at 16 or fewer input bits, else 1e6 seeded vectors plus
declared corners with the seed printed); T1 (riscv-tests plus a fuzz corpus
through both bindings, retirement traces byte-identical).

**Regime, declared.** AT-C2-S is a **stream**-regime claim - guest output bytes
only. At a declared 1 MHz with HZ=100 a timer tick lands every ~1e4 cycles,
between different instructions on the two tiers by construction, so a divergence
localizes to a console **byte**, not an instruction. Instruction-level
localization exists only in T1.

## Demo slice

The behavioral tier alone, headless, with the clock-period guard:
FEAT-033 + FEAT-032 + the guard, roughly TASK-0067, TASK-0068, TASK-0070,
TASK-0071, TASK-0080 = **~10 maintainer-weeks**. It boots Linux and answers
`uname -a` in about 2.5 minutes with nothing drawn. It is not the capstone - the
capstone is the drawn machine hitting the same golden - but it produces the
golden, the guest image and the guard that everything else is measured against.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | required/beneficial |
|---|---|---|---|
| FEAT-033 | `jls.mach`, the reference runner and the guest software stack | Supplies the parity counterparty and the pinned kernel/DTB/initramfs | required |
| FEAT-034 | Retirement-indexed parity harness and `RetireRecord` | The two tiers are only comparable per retired instruction, and over-constraining must be a compile error | required |
| FEAT-032 | The host byte port, a `Console` element and transcripts | Every byte the oracle compares comes from here, and the input log must replay | required |
| FEAT-038 | The drawn structural RV32 machine | This is the capstone artifact - a machine a person can open and read | required |
| FEAT-031 | The per-instance fidelity toggle and its boundary harness | One file, one attribute flipped, is what makes the two tests share a golden | required |
| FEAT-037 | Reset semantics, clock and domain architecture | A drawn CPU without honest reset does not come out of reset | required |
| FEAT-036 | Byte lanes on `Memory` and capacity as a byte budget | A drawn core needs single-cycle read-modify-write, and 16 MiB is 4,194,304 words | required |
| FEAT-026 | The four-state value core with a resolution fold | A real bus needs Z and order-independent multi-driver resolution | required |
| FEAT-030 | Engine constant factors: the semantics-preserving stack | 1.66-1.72 h becomes 44-46 min at 2.26x; every golden must stay byte-identical | required |
| FEAT-006 | Simulation capacity and long-run ergonomics | A multi-hour run needs an unbounded duration, a heartbeat and a clean interrupt | required |
| FEAT-005 | Quadratic and materializing I/O paths eliminated | The stimulus parse is 80% of end-to-end wall time; a 4e7-instruction run cannot pay it | required |
| FEAT-035 | Checkpoint and simulation-state serialization | A 1.7 h run that cannot be resumed is a run that cannot survive an interruption | required |
| FEAT-009 | The measurement gate and a tracked calibration fixture | Every wall-clock claim here divides by measured constants, and D5 deletes `riscv/` | required |
| FEAT-007 | CI long-run lanes, timeouts and cross-platform parity | AT-C2-I is nightly; the boot needs a lane that is not the required gate | required |
| FEAT-013 | Per-section internal versioning with must-understand semantics | The guest image and the checkpoint ride as optional sections | required |
| FEAT-017 | Shared and parameterized subcircuit definitions | A ~580-element machine drawn as per-instance deep copies diverges silently | required |
| FEAT-004 | Shared net-partition IR with stable net naming | Probe and net names must not be a function of save order across a multi-hour run | beneficial |
| FEAT-014 | Stable addressing and per-view geometry in the shared model | Addresses the boundary being toggled and the probes being read | beneficial |
| FEAT-015 | The headless, programmatic `CircuitOp` layer | Building a 580-element machine by hand is what K6 counts revisions against | beneficial |
| FEAT-020 | Yosys JSON read: mapper parity with the validator | An alternative on-ramp to the drawn machine if authoring stalls | beneficial |
| FEAT-050 | Module runtime consumed: extension points and providers | The device subsystem is hosted in the module runtime that boots and is unread | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | closes |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | depends on (FEAT-026, FEAT-030) |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | overlaps (FEAT-020, the alternative on-ramp) |
| 265 | CI test parity across supported platforms | depends on (the nightly lane) |
| - | The machine, device and parity layers (FEAT-030 through FEAT-038) | **no issue** - #232 covers only the value representation, not the queue, the fidelity boundary or the parity harness |
| - | The tracked calibration fixture and deleting `riscv/` (FEAT-009) | **no issue** |
| - | CAP-02 as a capstone | **no issue** |

## Open decisions

1. **Where does the guest image live?** Recommendation: an **optional binary
   section** of the `.jls` file under FEAT-013, with a sidecar permitted for
   development. Reason: a 16 MiB RAM image is ~66.6 MB as hex text against a
   64 MiB cap on decompressed text, so it cannot ride in the text body; the
   corpus contradicts itself here and one side must be withdrawn.
2. **Is `Console` an element or a subcircuit?** Recommendation: **both** - keep
   the element, and fund a `mach.dev` differential harness (~2-3 wk) so the
   drawn UART and CLINT can be held equal to `Uart16550Model` and `ClintModel`.
   Reason: those devices produce every byte the oracle compares, and today no
   harness relates any pair. Without this the capstone risks being vacuous.
3. **Is the guest config a contract term?** Recommendation: **yes** - `printk.time=0`,
   a pinned `lpj=`, a fixed hostname, a time-free prompt, and every remaining
   time-derived output in a printed, ratcheted exclusion set with a stated
   reason. Reason: `CONFIG_PRINTK_TIME` makes the designated oracle fail on its
   first run for a reason that is not a bug.
4. **nommu now, or hedge to Sv32?** Recommendation: **nommu now**, price the
   Sv32 hedge separately at +3-5 wk. Reason: Sv32 roughly doubles the structural
   boot and adds OpenSBI and ELF to the surface.
5. **Does CAP-02 run before CAP-03?** Recommendation: **yes.** Reason: a real
   sequencing measurement, not a preference - ordering CAP-02 first saves 42-70
   mw because CAP-03 reuses its machine, device, parity and checkpoint layers.

## Kill criteria

- **KC-02-1 (K1).** Behavioral events per retired instruction above **25**: the
  word "live" is retired from all documentation and the claim becomes one
  command per minute; the program continues. Above **46** (2x the 23 ev/cycle
  budget ceiling): the live-console claim stops entirely - ship headless,
  transcript and replay, and cut the GUI console panel.
- **KC-02-2 (K2).** Active fraction at or above 0.56 with the engine stack
  delivering under 2.0x: the structural boot is 9.7 h or more, fits no lane, and
  becomes a release-cadence expedition with no nightly structural lane. If the
  boot exceeds **12 h**, cut the full structural boot claim outright: the
  deliverable becomes riscv-tests parity plus bounded handover windows, and
  "no drawn logic simulator has booted an OS" stays true of JLS too. Say so.
- **KC-02-3 (K3).** Byte-identical VCD and stdout cannot be achieved across the
  entire existing golden corpus: FEAT-030 stops at the failing change. No
  semantic change may be taken to buy speed. There is no partial credit.
- **KC-02-4 (K4).** The deliberately-wrong fidelity binding is not rejected by
  the boundary harness: FEAT-031 stops and nothing downstream merges. An
  unfalsifiable parity harness is worse than none.
- **KC-02-5 (K5).** `jls.mach` does not reach 93.0/92.0/84.5 as a package
  aggregate plus PIT 80/82 within **8 months** against a 4-month budget: cut the
  model to RV32I plus Zicsr, M-mode only, **abandon the Linux target**, and
  promote the in-jar M-mode self-checking payload to the parity workload.
- **KC-02-6 (K6).** More than three ground-up revisions or more than 16 weeks of
  hand-authoring the drawn machine: authoring stops and FEAT-015's programmatic
  construction is escalated ahead of it.
- **KC-02-7 (K8).** The pinned kernel plus initramfs cannot be rebuilt from its
  documented recipe by the maintainer alone in under 2 hours at any point: the
  Linux target is demoted below the in-jar M-mode payload. A published upstream
  proposal targets removing RV32 nommu "by the beginning of 2027", inside this
  program's window; the kernel version is pinned as a documented artifact from
  day one.
- **KC-02-8 (K9).** Any regression to the first-year student drawing an adder
  stops the responsible feature, regardless of cost to the flagship.

## Evidence

- Measured guest constants: Linux 6.5.12 RV32IMA nommu, 16 MiB, busybox
  initramfs; **4.0e7 retired instructions to a shell**
  (`docs/machine-calibration.md:676, 1108`); 16 MiB is where a shell and root
  login fit (`:706`, `Memory: 13668K/16380K available`); 12 MiB is the practical
  floor (`:712`).
- The image-size arithmetic that decides open decision 1: 4,194,304 words is
  exactly 16 MiB with zero headroom (`docs/machine-calibration.md:719`); as text
  that is ~66.6 MB, 99.2% of the 64 MiB decompressed-text cap, leaving ~0.5 MB
  for the entire circuit (`:733`). The cap is at
  `src/jls/FileAbstractor.java:65`; the dense store ceiling is
  `DENSE_CAPACITY_LIMIT = 1 << 22` at `src/jls/elem/Memory.java:1224`, which is
  exactly the 16 MiB figure and leaves no headroom.
- Engine constants, warm event loop: **3.14 M events/s, 318 ns/event**, of which
  queue plus dedup is 151.8 ns (47.7%). Boot: nommu **1.66-1.72 h** central;
  **44-46 min** after the 2.26x semantics-preserving stack; **20-21 min** after
  the full 4.9x stack. Behavioral 2.5 min. Element census ~580 central, band
  400-870; 468-485 events per instruction.
- The live-console figure is a cost, not a limit: ~19,500 cycles/s today
  (1.5 s/char at a 1e4 echo path), ~44,000 after the semantics-preserving stack
  (0.66 s/char), ~96,000 after the full stack (0.30 s/char), against a 1e5-1e6
  requirement. The 1e5 floor is missed by 1.2-5x - a decision plus ~30-45
  maintainer-weeks. D10 forbids restating this as impossibility.
- Any figure derived from ns/node must state node count and pass count: 4.32 ns
  is **per node**, a ~580-element machine is ~1,345 nodes, and a levelized design
  needs two passes per cycle.
- Parity framing exists at HEAD and is referenced, not restated:
  `docs/parity-contract.md` §2.4 (input log indexed by retirement), §4
  (permitted to differ), §5.3 (the deliberately-failing null test), §9.1 (the
  machine definition has no mechanism and no contents list). The contract is
  **unratified** as of `b299d63` - that is a decision to take, not a blocker.
- HEAD gaps this capstone must close, verified: no simulation-state
  serialization (`Circuit.save` takes no simulator; `Memory.initSim` rebuilds
  from init text); batch pause identical to stop
  (`src/jls/sim/BatchSimulator.java:75-78, 87-90`); past-limit events polled and
  dropped after removal from `dupCheck` (`src/jls/sim/Simulator.java:224-232`);
  quadratic stimulus parse (`src/jls/elem/SigSim.java:64-74`); no
  `timeout-minutes` in any of six workflows.
- `LogicElement` permits grew 22 to 24 for ~65 lines of registration tax across
  12 files, demonstrated twice in one commit. A UART and a CLINT are planned as
  in-tree `LogicElement` subclasses and cost **zero format versions**.
- Owner programs: FEAT-005, FEAT-026 and FEAT-030 under P1; FEAT-006 and
  FEAT-036 under P2; FEAT-004 and FEAT-020 under P3; FEAT-034 under P5;
  FEAT-031 under P8; FEAT-035 under P9; FEAT-013 under P11; FEAT-015 and
  FEAT-050 under P12; FEAT-037 under P13. FEAT-007, FEAT-009, FEAT-032,
  FEAT-033 and FEAT-038 are **UNOWNED**.
- Marginal band 32-58 mw versus standalone 155-250 mw (`10-capstone-plan.md`
  §3.1, row C2) - the 4.8x gap is the whole argument for the shared spine.
- **Cost reconciliation.** Marginal band 32-58 mw. Its 16 required features
  sum to 159-253 mw and its 5 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
