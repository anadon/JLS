# CAP-03 - A ternary CPU with N-ary subcircuits and a custom kernel

**Status:** proposed | **Priority:** 17 | **Marginal cost:** 28-45 mw |
**Standalone cost:** 98-161 mw

## Outcome

A drawn balanced-ternary CPU, composed with radix-3, radix-4 and radix-2
subcircuits in one drawing, runs a hand-written single-tasking monitor with a
command line and a filesystem, prints to a live console, and is verified against
an independent emulator per retired instruction.

## Acceptance test

**Scope, fixed so the test is unambiguous.** *Ternary* is **balanced** ternary
(digits -1, 0, +1) - what the word means when said aloud, what Setun was, and
where the pedagogy lives; unbalanced base 3 is a per-port encoding attribute
costing nothing extra. *N-ary subcircuits* is a **mixed-radix composition
contract**, not "support arbitrary N": one drawing in which radices coexist, with
the rules by which their nets meet specified, enforced and drawable. *Custom
kernel* is a DOS-like single-tasking monitor with a command line and a
filesystem, per the maintainer - not a port of 86-DOS or Sinclair QDOS, both
bound to their host ISAs. Ternary **devices** (CNTFET, memristive MVL, Vdd/2
levels) are explicitly out: JLS is word-level, so it can host ternary
architecture and cannot host ternary devices. Said once, plainly.

**SEEN.** A student opens `machines/t3-soc.jls` - about ten boxes: T3 CPU
subcircuit, Memory, Console, BlockDevice, Clock, address decode - presses Run,
and a console prints a banner and an `A>` prompt. They type `DIR` and get a
listing; `TYPE README.TXT` and get text; `TRIT -5` and get `-++`, because -5 is
-9 + 3 + 1 and they have just seen a number system with no sign bit;
`RUN HELLO.TX3` and a compiled program prints. Echo is ~0.004 s/char behavioral
and ~0.18 s/char structural - **the one capstone where the behavioral tier is
comfortably interactive**, because the guest is 10^4 times smaller than Linux.

**CHECK - three tests.**

- **AT-C3-K (the capstone).** Run the drawn machine structurally with
  `-console replay:qdos.itlog -block qdos.img -d 0 --transcript --retire-trace`;
  run the reference emulator on the same itlog and image; `cmp` the retire traces
  **and** the transcripts **and** the transcript against the committed golden.
  **Falsification guard:** re-run with `--clock-period` 10x and `cmp` again -
  changing every simulated time must change no output byte and no record, or the
  golden encodes time and every other comparison is void. This is a
  **trace**-regime claim: QDOS is single-tasking with no timer interrupt, so a
  divergence localizes to **one instruction** - strictly stronger than CAP-02 can
  offer. It runs in ~30 minutes (1e7 instructions at 5,612 instr/s structural),
  so it **fits a hosted nightly lane**, which CAP-02's boot never will.
- **AT-C3-M (the novelty).** `machines/t3-mixed.jls` holds three radices in one
  drawing - radix-3 T3 core, radix-4 vector min/max coprocessor as an embedded
  subcircuit, radix-2 console. `--lint radix` must report *"implicit radix
  crossings: 0"*. Every net must have a uniform radix, validated at load.
  **Deleting any converter must make the file FAIL TO LOAD** with a message
  naming both radices and the converter to insert - a negative test on the
  message **text**, not a boolean. The coprocessor's radix-4 result matches a
  Java reference over 1e6 seeded vectors plus corners.
- **AT-C3-N (what makes the others mean anything).** Exhaustive over 1-trit (9
  cases) and 2-trit (81 cases) operands - genuinely exhaustive, which no binary
  32-bit ISA can offer; 1e6 seeded 16-trit vectors with the seed printed in the
  failure report; declared corners at plus and minus 3^15, plus and minus
  (3^16-1)/2, all-minus, all-zero, all-plus, alternating, and the illegal code
  injected into each of the 16 lanes. Plus a **T-null corpus** of subtly-wrong
  reference models the harness **must reject**: shift-right rounding away from
  zero instead of to nearest; the three-way branch's zero and positive arms
  swapped; negate as complement-plus-one instead of a plane swap; and the illegal
  code silently read as 0 - that last one passes every ordinary test and is wrong
  in exactly the case the encoding exists to catch. Assert the report **text**.

## Demo slice

`AT-C3-M` alone, with no CPU: the radix type system plus the higher-radix
operator kernel plus a drawn radix-4 min/max block and its converters -
TASK-0059 + TASK-0060 + TASK-0061 = **~5.5 maintainer-weeks** on top of the
one-week Stage 0 re-anchoring. It demonstrates mixed-radix composition, produces
the load-time refusal message, and is the part of this capstone nobody else has
built. The CPU and the monitor are the other 23-40 weeks.

## Prerequisite features

| FEAT-NNN | title | why THIS capstone needs it | required/beneficial |
|---|---|---|---|
| FEAT-028 | Radix-parameterized value and port type system | Radix as a property of a value and a net, validated at connection, with radix 2 provably unchanged | required |
| FEAT-029 | The N-ary element family and its interop | The drawable, simulable balanced-ternary datapath that exports, dumps and tests like any other circuit | required |
| FEAT-039 | JLS-T3: the ternary ISA, toolchain and drawn CPU | The ISA, its reference emulator, the in-jar assembler, and the drawn CPU - this is the capstone artifact | required |
| FEAT-034 | Retirement-indexed parity harness and `RetireRecord` | The trace-regime comparison, with the permitted-to-differ set unrepresentable by the type | required |
| FEAT-032 | The host byte port, a `Console` element and transcripts | The monitor prints here and the itlog replays here | required |
| FEAT-033 | `jls.mach`, the reference runner and the guest software stack | The independent emulator that is the counterparty; without it the same person authors both sides with no method | required |
| FEAT-031 | The per-instance fidelity toggle and its boundary harness | Behavioral and structural tiers must be one file with one attribute flipped | required |
| FEAT-037 | Reset semantics, clock and domain architecture | A drawn CPU without honest reset does not come out of reset | required |
| FEAT-036 | Byte lanes on `Memory` and capacity as a byte budget | The monitor's filesystem and the block image need a byte budget, not a word-count cliff | required |
| FEAT-026 | The four-state value core with a resolution fold | Supplies the three-plane record whose spare code points radix 3 and 4 occupy | required |
| FEAT-030 | Engine constant factors: the semantics-preserving stack | ~30 minutes structural is what makes AT-C3-K fit a nightly lane | required |
| FEAT-017 | Shared and parameterized subcircuit definitions | A mixed-radix composition of per-instance deep copies diverges silently | required |
| FEAT-013 | Per-section internal versioning with must-understand semantics | The radix manifest and the block image ride as sections an old reader must refuse | required |
| FEAT-006 | Simulation capacity and long-run ergonomics | A 1e7-instruction run needs unbounded duration and a clean interrupt | required |
| FEAT-005 | Quadratic and materializing I/O paths eliminated | The stimulus parse is 80% of end-to-end wall time on every long run | required |
| FEAT-009 | The measurement gate and a tracked calibration fixture | Every wall-clock claim here divides by measured constants | required |
| FEAT-007 | CI long-run lanes, timeouts and cross-platform parity | AT-C3-K is a ~30 minute nightly job, not a required-gate job | required |
| FEAT-035 | Checkpoint and simulation-state serialization | Makes a long structural run resumable and a boundary handover free | beneficial |
| FEAT-004 | Shared net-partition IR with stable net naming | The radix manifest is keyed off net identity | beneficial |
| FEAT-014 | Stable addressing and per-view geometry in the shared model | Addresses the toggled boundary and the probed nets | beneficial |
| FEAT-015 | The headless, programmatic `CircuitOp` layer | Drawing a ternary datapath by hand is what K6 counts revisions against | beneficial |
| FEAT-050 | Module runtime consumed: extension points and providers | The N-ary palette contributes through the registry that boots and is unread | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC; evaluate a value-typed (long,width) signal representation | depends on - the three-plane value record it proposes is what supplies radix 3 and 4 their code points |
| - | The entire N-ary program (FEAT-028, FEAT-029) | **no issue** |
| - | The ternary ISA, emulator, assembler and drawn CPU (FEAT-039) | **no issue** |
| - | The machine, device and parity layers (FEAT-030 through FEAT-038) | **no issue** |
| - | CAP-03 as a capstone | **no issue** |

## Open decisions

1. **Is the one-week Stage 0 taken inside P1?** Recommendation: **yes,
   unconditionally.** Re-anchor `docs/simulation-semantics.md` §2 once, in
   alphabet-parameterized language - "radix is a property of ports and wire nets,
   like bit width, whose only accepted value today is 2". Reason: what is paid
   twice by skipping it is **not engineering, it is governance** - recorded
   decision #221's equivalence criterion and the normative §2 must each be
   reopened twice, plus two JaCoCo/PIT re-baselines at ~0.3 wk each, at bus
   factor 1. Serial penalty for taking it: ~1 week. For skipping it: ~2-3 weeks
   plus a second governance reopening.
2. **The radix bound.** Recommendation: **radix 2, 3 and 4 natively, bounded by
   arithmetic, not by taste.** The three-plane record gives eight code points per
   position; alphabets needing more are outside the shipped encoding. State the
   bound with its reason so "general case N-ary" is served by parameterizing
   every rule in (R1, R2) rather than by an unbounded promise.
3. **The CPU architecture - the single largest cost decision in this plan.**
   Recommendation: decide before TASK-0081. Reason: the standalone band spans
   98-161 mw and the architecture choice is most of that spread.
4. **The remaining ISA-shape rulings**, bundled: word size in trits, register
   count, the three-way branch encoding, and the exact division rounding rule.
   Recommendation: 16 trits per word, 27 registers, and division rounding
   **stated in the ISA document before the emulator is written**. Reason: the
   T-null corpus deliberately contains a wrong rounding model, so the rule must
   exist independently of both implementations or the corpus proves nothing.
5. **Does the DOOM stretch ship?** Recommendation: **yes, as a slideshow plus a
   time-lapse, priced here rather than discovered by a reader.** DOOM at 320x200
   is roughly 1-5M instructions per frame; at the measured 261,883
   instructions/s behavioral that is **4-20 s per frame**, minutes per frame
   structural - so a 30-second behavioral clip is 1.1-5.6 hours of wall clock
   and a 100-frame structural time-lapse is 4.1-20.7 hours. It counts. The plan
   should say plainly what it is rather than imply gameplay.

## Kill criteria

- **KC-03-1.** The T-null corpus is not rejected - the harness accepts any of the
  four subtly-wrong reference models: FEAT-034 stops and nothing downstream
  merges. This is K4 applied to a custom ISA, and it is the criterion that turns
  "I wrote both sides of every comparison" from a fatal objection into a managed
  risk. It is testable in milliseconds on every push, so there is no excuse for
  discovering it late.
- **KC-03-2.** AT-C3-M's negative arm passes without the converter present, or
  the load-time refusal message does not name both radices: FEAT-028 stops. A
  radix system that does not refuse an implicit crossing is a comment.
- **KC-03-3.** Radix 2 is not provably byte-identical across the entire existing
  golden corpus after TASK-0056 and TASK-0059: stop at the failing change. K3
  applies unchanged - no semantic change may be taken to buy generality.
- **KC-03-4 (K6).** More than three ground-up revisions or more than 16 weeks of
  hand-authoring the drawn ternary CPU: authoring stops and FEAT-015's
  programmatic construction is escalated ahead of it.
- **KC-03-5.** AT-C3-K exceeds **50 minutes** structural (1.7x the ~30 minute
  estimate): it no longer fits a hosted nightly lane, and the trace-regime claim
  is demoted to release cadence like CAP-02's - which removes this capstone's
  main advantage over CAP-02 and should be said out loud.
- **KC-03-6 (K9).** The N-ary palette is visible to a first-year student drawing
  an adder, or GUI startup or per-edit cost regresses: the responsible feature
  stops. Under D9 the machinery may exist but must be default-hidden and opt-in.
- **KC-03-7.** The block image cannot be made to hold the guest: 4,194,304 words
  is the dense-store ceiling, so a 24 MB WAD cannot live in `Memory`. If
  `BlockDevice` plus a file port does not land, the DOOM stretch is cut - it is a
  hard prerequisite of that demo, not a QDOS nicety.

## Evidence

- Sequencing arithmetic for open decision 1, itemized in the corpus: the 27
  `react()` bodies, the 94 `new BitSet(` sites in `src/jls/elem/`, the five
  change-detection sites and the golden re-derivation are **not** paid twice
  under port-radix; recorded decision #221's equivalence criterion
  (`ARCHITECTURE.md:359-368`), the normative `docs/simulation-semantics.md` §2,
  and two JaCoCo/PIT re-baselines **are**. Both normative documents are
  referenced, not restated.
- The code-point arithmetic that makes radix 3 and 4 nearly free: P1's value
  record is three bit-planes, eight code points per position; radix 3 needs
  three and radix 4 needs four, so both fit inside the encoding CAP-02 is paying
  for anyway. Program cost 17-25 mw on top of P1, of which exactly one week must
  be spent **during** P1 and the remaining 16-24 at any later time with no
  penalty.
- Performance: ~30 minutes for 1e7 instructions at 5,612 instructions/s
  structural; ~0.004 s/char behavioral echo and ~0.18 s/char structural. Warm
  event loop measures 3.14 M events/s at 318 ns/event. Any figure derived from
  ns/node must state node count and pass count.
- DOOM: ~36k lines of core game over a 62,458-line port surface, fixed-point,
  4-8 MB of RAM. Freedoom 0.13.0 (BSD-3) is 24,143,781 bytes against
  `DENSE_CAPACITY_LIMIT = 1 << 22` words (`src/jls/elem/Memory.java:1224`,
  verified at HEAD) - so `BlockDevice` plus a file port is a prerequisite.
- Element inventory this capstone extends: 35 registered types and 27 `react`
  implementations (`src/jls/elem/ElementRegistry.java:38-77`, verified at HEAD),
  with no Multiplier and no Divider. `LogicElement` permits grew 22 to 24 for
  ~65 lines of registration tax across 12 files; a new element type costs
  **zero format versions**.
- HEAD gaps this capstone shares with CAP-02, verified independently here:
  `src/jls/elem/SigSim.java:64-74` (quadratic parse);
  `src/jls/sim/Simulator.java:224-232` (past-limit events polled then dropped);
  `src/jls/sim/BatchSimulator.java:75-78, 87-90` (pause equals stop); zero
  `timeout-minutes` across six workflows; no simulation-state serialization.
- Sequencing, measured rather than preferred: CAP-02 before CAP-03 saves 42-70
  mw because CAP-03 reuses CAP-02's machine, device, parity and checkpoint
  layers (`10-capstone-plan.md` §2.2). Priority 17 means "start last", not
  "fund last".
- Owner programs: FEAT-005, FEAT-026, FEAT-028 and FEAT-030 under P1; FEAT-006,
  FEAT-029 and FEAT-036 under P2; FEAT-004 under P3; FEAT-034 under P5;
  FEAT-031 under P8; FEAT-035 under P9; FEAT-013 under P11; FEAT-015 and
  FEAT-050 under P12; FEAT-037 under P13. FEAT-007, FEAT-009, FEAT-032,
  FEAT-033 and FEAT-039 are **UNOWNED** - the committed roadmap pays for the
  radix and element work and for none of the machine work.
- Marginal band 28-45 mw versus standalone 98-161 mw (`10-capstone-plan.md`
  §3.1, row C3).
- D10 is binding here more than anywhere: the MVL determination's original
  verdict led with "zero mentions across 944 tracked files", which is a
  measurement of what JLS has never offered. Its survey evidence stands; its
  verdict was re-derived as a path and a cost.
- **Cost reconciliation.** Marginal band 28-45 mw. Its 17 required features
  sum to 172-265 mw and its 5 beneficial features are additional. The marginal
  band is smaller than the required set because most of those features are
  shared spine, booked once against whichever capstone funds them first.
  "Marginal" here means the incremental cost given the spine is funded; the
  standalone figure in the header is the other end of that range. The required
  sum is printed rather than reconciled away.
