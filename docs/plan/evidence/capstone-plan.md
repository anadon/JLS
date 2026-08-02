# THE CAPSTONE PLAN

**The five capstones, organized as one lattice: acceptance tests, the shared
spine, marginal costs, a dated sequence, the unowned programs, the honest
total, the permanent decisions, and how a sixth capstone gets costed.**

Repository: `/home/user/JLS` at `b299d63`. Every in-tree citation below was
re-verified at that commit during the writing of this document; the
verification commands and their outputs are recorded in §11.

---

## 0. STANDING RULES AND WHAT THIS DOCUMENT IS

**D10 is binding throughout.** Not one capability in this plan is refused for
absence of demand, absence of prior art in the tree, absence from the roadmap,
or a prior AI-authored "no". Every row is a path and a price. Where a limit is
real it is shown with arithmetic (DOOM's frame budget, §1.3; the coverage
commons, §4.6; ARCH-N's 190–330 weeks, §7.7); where a specific *approach*
fails, the approach that works is named (LLVM → QBE, §7.7; a merge → a check,
§1.4; a second `(x,y)` on `Element` → a keyed side table, §7.1).

**What this document adds to the seven analyses it stands on.** The sibling
determinations (`cap-c1-collab.md`, `cap-c2-linux.md`, `cap-c3-ternary.md`,
`cap-c4-breadboard.md`, `cap-c5-pcb.md`, `cap-lattice.md`, `cap-realist.md`)
decomposed the capstones, found the spine, and calibrated the unit. This
document does four things none of them could:

1. It **reconciles their arithmetic** and states where they disagree (§3.4,
   §6.4, §11.2). Three of the totals in the sibling set do not add up; the
   corrections are here rather than left for a reader to infer.
2. It **orders the work for early demonstrable value** and commits to a
   calendar with a stated cadence (§4), naming which capstone demos first.
3. It **names the unowned work as proposed programs P14–P35** so it can be set
   beside P1–P13 like for like (§5).
4. It makes the lattice **extensible by construction** — §8 is a procedure that
   costs capstone 6 mechanically, with four worked examples, and a falsifiable
   claim about the procedure itself.

**One correction to the brief, load-bearing enough to state up front.** The
committed roadmap is **288–424 maintainer-weeks**, not 281–410
(`docs/capability-roadmap/AMENDMENT.md:979`, verified: `| **TOTAL, BOTH
SWEEPS** | **288–424** | **66–98** |`). The same line records that built
independently, without the shared-node credits, it is 300–440. Every
percentage in this document divides by 288–424.

**A second correction, also verified.** `HdlModel` has **eleven** concrete
`Statement` subclasses at HEAD, not ten (`src/jls/hdl/HdlModel.java:202, 253,
285, 317, 353, 401, 468, 538, 615, 725, 840`, plus the abstract base at `:113`).
None instantiates a module. The brief says that blocks EDIF, BLIF, KiCad
netlist and SubCircuit export simultaneously; that is true of four *exports*
and affects exactly **one** capstone (capstone 5), because capstone 4's wiring
list never enters `HdlModel` at all and capstone 5's board flow needs a
*flattened* netlist, which never needs to know two subcircuits share a type.
The reach of that one missing statement kind is overstated across this lattice
by roughly 4x.

---

## 1. THE FIVE CAPSTONES AND THEIR ACCEPTANCE TESTS

Each subsection below is written to be pasted into a GitHub issue: a scope
reading that fixes the ambiguous words, **what a person sees**, and **the
automated check that proves it**. Every check includes a *falsification guard*
— an assertion that fails today for a stated reason and can only go green when
the capability actually lands. A golden without a guard is a transcript, not an
oracle, and three of the five acceptance tests in the sibling documents
originally lacked one.

### 1.1 CAPSTONE 1 — a multi-discipline, multi-user, simultaneous development pattern

**Scope reading (D9).** One artifact carrying several **views**, each
discipline working in its own view, editing **concurrently**, with merge
correct **across** views. The weaker reading — several disciplines taking turns
in one schematic view — is covered by multi-user alone and is not worth a
capstone. The stronger reading makes this **two** programs: a replication
program and a per-view-model program, and it is the second that carries a
deadline.

**WHAT A PERSON SEES.** Three machines on a LAN, one 4-bit ALU. Alice (CS)
rewires the carry chain in the schematic view. Bob (ECE) attaches probes and
toggles watched signals in the waveform view. Carol (EE) assigns footprints and
drags packages in the board view. All three edit simultaneously — no token, no
turn-taking. Each sees peers' cursors in their own view, and a badge on the
shared object for a peer working in a different view: *"Carol · board"* beside
U3 in Alice's schematic. Alice deletes a gate Carol just footprinted: one
attributed conflict banner, and the footprint record is tombstoned **with** the
element rather than orphaned. All three save. The three files are
**byte-identical**, and the peer panel said so before they saved.

**AUTOMATED CHECK — `CircuitConvergenceTest`**, a direct structural sibling of
the shipped `test/jls/collab/session/RosterConvergenceTest.java` (1000 seeded
schedules × 80 steps × up to 6 peers, seed printed on failure).

1. N headless replicas (N ∈ 2..6) load one fixture circuit from bytes.
2. A seeded generator emits per-replica op streams over **all** op kinds
   including the view-qualified ones, biased toward collisions, containing the
   delete-vs-wire divergence witness as a named case.
3. Envelopes travel through the in-tree `test/jls/collab/net/ChaosTransport.java`
   (seeded drop/duplicate/reorder/partition/heal); each replica runs
   `CausalBuffer` → merge table → headless `OpSink`.
4. **Exactly one** bounded anti-entropy round, so convergence cannot hide behind
   "eventually".
5. Assert: (a) every replica's `Circuit.save` bytes identical — #166 is the
   oracle; (b) the converged circuit **loads and elaborates** —
   `Circuit.finishLoad` and `WireNet.makeNet` both succeed, which is the
   assertion that catches semantic corruption, because parsing is not the
   criterion, elaboration is; (c) `CausalBuffer.pendingCount() == 0` on every
   replica; (d) replaying each replica's own op inverses returns it to the
   fixture bytes, which is what makes "revert everything peer X did" mechanical.

**TWO ADDITIONAL GATES.**

- **`ViewIsolationTest`** — replica A emits only schematic ops, replica B only
  board ops, over the same elements; must converge with **zero** conflicts.
  This *is* the multi-discipline claim as an executable test. If it fails,
  per-view separation is fictional.
- **`OldReaderTest`** — a FORMAT-N reader opening a file with a must-understand
  VIEW section refuses to save, does not lose the section, and says why.

**FALSIFICATION GUARD.** `OldReaderTest`'s negative arm must fail today for a
verified reason: `Element.setValue` silently `return`s on an unknown attribute
name (`src/jls/elem/Element.java:344-351`) and the loader calls it
unconditionally at `Circuit.java:1067, 1078, 1089, 1105, 1116` with no
diagnostic. So an older JLS opening and re-saving a file whose view data lives
on elements **destroys the EE's work with exit status 0**. The guard is the
assertion that this path is closed, and it cannot pass by accident because the
silent-drop code is at HEAD.

**HARDEST PART, with a witness and no probabilities.** Making the 11 op kinds
**total** under concurrency. `OpRejected` is not a merge outcome. Replicas A
and B start byte-identical with unwired `AndGate` E and neighbour N. A issues
`RemoveElements([E])` — valid, `RemoveElements.java:126-137` requires unwired.
B concurrently issues `AddWire(attach=[E,N])`. They are concurrent, so
`CausalBuffer` delivers both immediately at both replicas in **arrival** order.
At A: Remove succeeds, then AddWire fails `Ops.resolve` (`Ops.java:36-42`) on
anchor E. Final state: no E, no wire. At B: AddWire succeeds, then
`RemoveElements` throws *"element '…' has a wire attached and cannot be removed
by this op"*. Final state: E present, wire present. Same op set, causal delivery
satisfied on both sides, byte-different canonical saves. `CausalBuffer`'s own
javadoc (`:22-24`) states this is out of scope for delivery — and the merge
rules "layered above this buffer" do not exist anywhere in the tree.

**THE MULTI-DISCIPLINE-SPECIFIC HARDEST PART**, narrower and sharper.
`SetElementConfig(ElementId id, String block)` carries the element's **entire**
serialized save block — verified at `src/jls/collab/op/SetElementConfig.java:48-53`:
*"@param block The reconfigured element's serialized block, declaring the same
stable id and type as the addressed element."* So "CS rewires the datapath while
EE assigns footprints" is two whole-block ops on one element, and **any**
last-writer-wins rule discards one discipline's entire edit including fields the
other never touched. There is no field granularity to merge at. Multi-user
works; multi-discipline does not. The fix is not clever merge — it is
**separating** the fields (per-view sidecar, ~0 marginal cost, falls out of the
VIEW section) and then **typing** them (a `SetAttribute` op, 3–5 wk, using the
`Element.savedAttributes()` enumeration that already drives the loader).

### 1.2 CAPSTONE 2 — boot a CLI-only Linux distribution and run commands

**Scope reading.** *A CLI-only Linux distribution* = a pinned mainline kernel +
busybox initramfs (Linux 6.5.12 RV32IMA nommu, 16 MiB, 4.0e7 retired
instructions to shell, measured; `docs/machine-calibration.md` §5.1) — **not**
Debian or Alpine, which force Sv32 + S-mode + OpenSBI + ELF and take the
structural boot from ~1.7 h to ~4 h (priced separately as a +3–5 week hedge).
*Run commands* = at least one command whose output is a pure function of the
guest image, delivered through a retirement-indexed input log, byte-compared:
`uname -a`, `ls /`, `cat /proc/cpuinfo`. Explicitly **not** `date`, `uptime` or
`dmesg` — simulated time is a permitted divergence (`docs/parity-contract.md`
§4), so an acceptance test that runs `uptime` fails its first structural run for
a reason that is not a bug. *Boot* = a shell prompt (`/ #`) at 4.18e7
instructions, not `Run /init` at 2.93e7 — **27% of the boot happens after init
starts.**

**TWO TESTS SHARING ONE GOLDEN. The sharing is the parity claim.**

**AT-C2-I (interactive, behavioral, nightly, ~2.5 min).** A person opens
`machines/rv32-soc.jls` — about ten top-level boxes; it looks like a computer —
presses Run, watches the kernel log for ~2.5 minutes, gets `/ #`, types
`uname -a` and is answered in 4–76 s (echo ~0.04 s/char at a 1e4-instruction
echo path, ~0.4 s at the unmeasured 1e5 end).

*The recording, not the session, is the contract:*
`jls -b machines/rv32-soc.jls -console replay:boot.itlog -d 0 --transcript out.txt`
then `cmp out.txt test/fixtures/c2/boot.golden`. `boot.itlog` is timestamped in
**retirement index**, never seconds, cycles or simulated time
(`docs/parity-contract.md` §2.4).

**AT-C2-S (structural, drawn logic, release cadence, by hand).** The *same
file*, one attribute flipped (`--fidelity cpu=structural`), headless,
1.2–6.0 h (~1.7 h central; 44–46 min after the 2.26x semantics-preserving
stack; 20–21 min after the full stack), then
`cmp build/boot-structural.txt test/fixtures/c2/boot.golden` — **the same
golden**. Not in CI: a 1.7 h boot fits `timeout-minutes: 60` only after the
engine work and fits **no** hosted lane at the pessimistic end. It is a
CHANGELOG entry carrying the run's commit SHA — the headline result verified by
a human, stated as an accepted cost.

**FALSIFICATION GUARD — the cheapest test in the study and it gates everything
downstream.** Re-run AT-C2-I with the `Clock` period changed 10x. Output bytes
must be **byte-identical** while simulated time differs. If that fails, the
golden encodes time and AT-C2-S is impossible. This guard is currently nobody's
deliverable in any roadmap document; it is ~1 week and it must be funded *with*
its rung, never after.

**Guest config is a contract term, not a convenience:** `printk.time=0`, a
pinned `lpj=`, a fixed hostname, a time-free prompt. Every remaining
time-derived output goes in the **ratcheted, printed** exclusion set E with a
stated reason.

**Carried on every push instead of the long boot:** T-null (knowingly-wrong
bindings the harness *must* reject — subtly wrong, not constant-zero, asserting
the report **text** not the boolean); T0 (boundary equivalence on ALU, register
file, decode, LSU, CSR, CLINT — exhaustive at ≤16 input bits, else 1e6 seeded
vectors plus declared corners with the seed in the failure report); T1
(riscv-tests plus a fuzz corpus through both bindings, retirement traces
byte-identical).

**Both claims must name their regime.** AT-C2-S is **stream** regime — guest
output bytes only; the per-instruction trace is not compared, because at a
declared 1 MHz with HZ=100 a timer tick lands every ~1e4 cycles, between
*different* instructions on the two tiers by construction, injecting trap
records at different retirement indices and changing every subsequent PC. A
divergence therefore localises to a console **byte**, not an instruction.
Instruction-level localisation exists only in T1's trace regime.

**HARDEST PART — not the boot, the oracle.** Three problems stack and only the
first has a cheap fix. (1) Interrupts kill the trace regime at Linux scale, as
above; coupling the models by a hand-maintained per-instruction cycle budget was
considered and rejected, because at bus factor 1 the question "real bug or stale
budget entry?" destroys trust in a differential suite. So the console byte
stream is the **only** Linux-scale oracle. (2) That stream is not tier-independent
by default — `CONFIG_PRINTK_TIME` stamps every kernel line from a timer — so the
designated oracle fails on its first run; the fix is trivial but the fixes are
*guest-config requirements* belonging in a machine definition D that has neither
a mechanism nor a contents list (`docs/parity-contract.md` §9.1). (3) **The
unfixed one:** `Console` is an *element*, not a subcircuit, so the per-instance
fidelity boundary **structurally cannot** hold a drawn UART equal to
`Uart16550Model`, nor a drawn CLINT equal to `ClintModel`. That is two UART and
two CLINT implementations with no harness relating any pair — and those devices
**produce every byte the oracle compares**. Either fund a `mach.dev`
differential harness (~2–3 wk, and it is in P16 in §5) or state plainly that the
oracle's producer is unguarded. By the T-null principle's own logic — an
unfalsifiable parity harness is *worse* than none, because it converts an
unchecked claim into a checked-looking one — this is the defect that could make
the whole capstone vacuous.

### 1.3 CAPSTONE 3 — a ternary CPU with N-ary subcircuits and a custom kernel

**Scope reading.** *Ternary* = **balanced** ternary (digits −1, 0, +1) — what
the word means when said aloud, what Setun was, and where all the pedagogy
lives; unbalanced base 3 is a per-port `Encoding` attribute costing nothing
extra. *Integrate in some way with general case N-ary connected or embedded
subcircuits* = a **mixed-radix composition contract**, not "support arbitrary
N": one drawing in which a radix-3 core, a radix-4 embedded coprocessor and a
radix-2 peripheral coexist, with the rules by which their nets meet specified,
enforced and drawable. "General case" is served by parameterising every rule in
(R1, R2) rather than special-casing 3, and by bounding N with an arithmetic
reason (§7.6). *Custom kernel* = a DOS-like **single-tasking monitor with a
command line and a filesystem**, per the maintainer — explicitly not a port of
Paterson 86-DOS or Sinclair QDOS, both bound to their host ISAs.

**Explicitly not read in:** ternary *device* research (CNTFET, memristive MVL,
Vdd/2 levels). JLS is word-level — a 32-bit `Adder` is one element with one
`react()` — so JLS can host ternary **architecture** and cannot host ternary
**devices**. Say it once, plainly, and stop apologising for it.

**THREE TESTS.**

**AT-C3-K (the capstone).** A student opens `machines/t3-soc.jls` — about ten
boxes: T3 CPU subcircuit, Memory, Console, BlockDevice, Clock, address decode —
presses Run, and a console prints:

```
JLS-T3 QDOS 0.1
27 registers, 16 trits/word, balanced ternary
MEM 4M  DSK QDOS.IMG
A>
```

They type `DIR` and get a listing; `TYPE README.TXT` and get text; `TRIT -5`
and get `-++` (because −5 = −9 + 3 + 1, and they have just seen a number system
with no sign bit); `RUN HELLO.TX3` and a C-compiled program prints. Echo is
~0.004 s/char behavioral, ~0.18 s/char structural — **this is the one capstone
where the behavioral tier is comfortably interactive**, because the guest is
10^4x smaller than Linux.

*Automated:* (1) run the drawn machine structural with
`-console replay:qdos.itlog -block qdos.img -d 0 --transcript --retire-trace`;
(2) run the reference emulator on the same itlog and image; (3) `cmp` the
retire traces **and** the transcripts **and** the transcript against the
committed golden; (4) **falsification guard** — re-run with `--clock-period`
10x and `cmp` again: changing every simulated time must change no output byte
and no record, or the golden encodes time and every other comparison is void.

The `RetireRecord` is `{order, pc_before, pc_after, insn_word, rd_index,
rd_value, mem_addr, mem_rmask, mem_wmask, mem_wdata, trap}` — RVFI's shape with
**no field** for cycles, simulated time, pipeline or cache state, so the
permitted-to-differ set is *unrepresentable by the type*.

Unlike capstone 2 this is a **trace-regime** claim: QDOS is single-tasking with
no timer interrupt, so the per-instruction trace **can** be compared and a
divergence localises to **one instruction** rather than one console byte — a
strictly stronger acceptance test than capstone 2 can offer, and a direct
consequence of the QDOS scope. It runs in ~30 minutes (1e7 instructions at
5,612 instr/s structural), so it **fits a hosted nightly CI lane**, which
capstone 2's 1.2–6 h Linux boot never will.

**AT-C3-M (the novelty).** `machines/t3-mixed.jls` holds three radices in one
drawing — radix-3 T3 core, radix-4 vector min/max coprocessor as an embedded
subcircuit, radix-2 console. `--lint radix` must report *"implicit radix
crossings: 0"*. Every net must have a uniform radix, validated at load.
**Deleting any converter must make the file FAIL TO LOAD** with a message naming
both radices and the converter to insert — a negative test on the message
**text**, not a boolean. The coprocessor's radix-4 result matches a Java
reference over 1e6 seeded vectors plus corners.

**AT-C3-N (what makes the others mean anything).** Exhaustive over 1-trit (9
cases) and 2-trit (81 cases) operands — **genuinely exhaustive, which no binary
32-bit ISA can offer**; 1e6 seeded 16-trit vectors with the seed printed in the
failure report; declared corners ±3^15, ±(3^16−1)/2, all-minus/zero/plus,
alternating, and the illegal BET code `2'b00` injected into each of the 16
lanes. Plus a **T-null corpus** of subtly-wrong reference models the harness
**must reject**: SHR3 rounding away from zero instead of to nearest; BR3's zero
and positive arms swapped; NEG implemented as complement-plus-one instead of a
plane swap; and `2'b00` silently read as 0 — that last one passes every ordinary
test and is wrong in exactly the case the encoding exists to catch. Assert the
report **text**, not the boolean.

**THE STRETCH DELIVERABLE, PRICED HONESTLY.** DOOM is ~36k lines of the core
game (62,458 lines of C over the full port surface), fixed-point throughout,
which is why it ports everywhere. It requires a C toolchain targeting the
ternary ISA (§7.7), a framebuffer element, and 4–8 MB of RAM (JLS `Memory`
reaches 16 MiB, so it fits). **The frame-rate arithmetic, which must appear in
the plan rather than be discovered by a reader:** DOOM at 320×200 is roughly
1–5M instructions per frame. At the behavioral tier's measured 261,883
instructions/s that is **4–20 s per frame**; on the structural tier it is
**minutes per frame**. So the honest deliverable is **DOOM as a slideshow plus
a time-lapse** — which is exactly the genre "it runs DOOM" belongs to, and it
does count. The plan says so plainly rather than implying gameplay. A 30-second
behavioral clip is 1.1–5.6 hours of wall clock; a 100-frame structural
time-lapse is 4.1–20.7 hours.

**A hard prerequisite nobody classified as one until the realist did:** the WAD
cannot live in `Memory`. Freedoom 0.13.0 (BSD-3) is 24,143,781 bytes against a
`DenseWordStore` ceiling of `1 << 22` words (`src/jls/elem/Memory.java:1224`,
verified). So `BlockDevice` + `FilePort` is a prerequisite of the DOOM demo, not
a QDOS nicety.

**HARDEST PART — writing the conformance corpus and the T-null models from
nothing.** Everywhere else in this study the golden comes from somewhere:
capstone 2 gets riscv-tests, riscv-arch-test, Spike and Sail free; the format
work gets the existing goldens; the engine work gets `k2000.jls`. For a custom
ISA there is nothing, and the same person authors both sides of every
comparison. The mitigation is method, and it is what turns "I wrote both sides"
from a fatal objection into a managed risk: exhaustive at 1 and 2 trits, 1e6
seeded vectors with the seed in the failure report, declared corners including
the illegal code, and a T-null corpus the harness must reject. **If a harness
cannot detect those four subtly-wrong models, it is detecting nothing — and
that is testable in milliseconds on every push.**

### 1.4 CAPSTONE 4 — design a breadboard implementation of a simple CPU

**Scope reading, per the maintainer's binding resolution: "more canvas, but
also physical simulation."** So it is **both** a breadboard canvas
(drag-and-drop parts and jumpers, per-view geometry) **and** a physical
simulation. The build-plan-only reading is **superseded** as the answer, though
it survives — and is recommended — as the early slice.

*Breadboard implementation* = the solderless-breadboard build of a CPU from
discrete DIP packages (SAP-1 / Ben Eater genre), not "a prototype" loosely — so
the artifact is packages, sections, pins, tie-point strips, jumpers, two supply
rails, and the packaging problem is the content. *Physical simulation* =
simulate the netlist the student **actually built**, with each pin carrying its
real part's electrical behaviour, in **discrete** time — explicitly not
continuous-time analog. The criterion that keeps the capstone from silently
acquiring a SPICE solver: discrete iff the value at any instant is a function of
current net state over a finite lattice; analog iff it needs a state variable
integrating over continuous time. *Simple CPU* = a **named, published** design
(SAP-1), not one JLS invents — inventing it would make the acceptance test
unfalsifiable.

**THE CONSEQUENCE, STATED PLAINLY AND PRICED.** Physical truth is now a **hard
prerequisite**, not optional. Floating inputs, fan-out limits, missing pull-ups
and contention are the classic breadboard failure modes and are the entire
pedagogical point. Real TTL floats **HIGH**; JLS today renders an un-wired
input as solid **LOW** — verified at HEAD,
`src/jls/elem/LogicElement.java:470-482`, *"Initialize all inputs to 0"*,
`in.setValue(BitSetUtils.Create((long)0))`. **A breadboard sim that models a
floating input as LOW teaches students the opposite of the truth.** That makes
program P1 (drive strengths, pull-up/pull-down, weak vs strong,
open-drain/wired-AND) a hard dependency, and capstone 4 is priced accordingly:
**24–34 of P1's 28–36 weeks, which is 85–95% of P1. There is no cheap subset of
P1 that buys physical truth.**

**THREE TIERS, each with a demonstrable end state and an automated check.**

**AT-C4-A (build plan; headless, no GUI, no P1).** *Seen:*
`jls -breadboard examples/sap1.jls -lib 74ls -o plan/` emits `bom.txt`
("6x 74LS04 U3 U9 U14…; 35 packages, 3 unused sections"), `refdes.map`,
`wiring.net` ("U3.1 -- U7.11 net BUS0"), `placement.brd`, `drc.txt`. **A person
orders the parts and builds it.**

*Check —* `BreadboardPlanGoldenTest`, seven assertions:
1. **Totality** — every logic element maps to exactly one (refdes, section), no
   section double-booked, errors aggregated per `PinBindings.parse`'s shipped
   idiom (`src/jls/hdl/board/PinBindings.java:38-60`).
2. **Netlist equivalence** — the union-find net partition extracted from
   `placement.brd` over hole occupancy is **equal, as a partition over pin
   identities**, to the schematic's `WireNet` partition pushed through the
   binding. This is the "consistent with the schematic" check, O(n·α(n)).
3. **Power completeness** — every placed package's VCC and GND pins are in the
   supply nets.
4. **No floating inputs** — every input pin of every used section is in a net
   with ≥1 driver or pull. *This detects the floating error statically, before
   P1 ships.*
5. **No contention** — per net ≤1 push-pull driver, or all open-drain plus ≥1
   pull-up.
6. **Fan-out** — per net, Σ sink unit loads ≤ min driver capacity in unit loads,
   with families where the DC check is vacuous reporting *"not DC-limited"*,
   never *"PASS"*.
7. **Determinism and diff stability** — re-run byte-identical (D2), and
   inserting one unrelated gate then regenerating must produce an
   **additive-only** diff.

**AT-C4-B (canvas).** *Seen:* the student drags 35 packages onto two rendered
breadboards and drags jumpers hole-to-hole; a discrepancy overlay lists, **in
physical terms**, every net the schematic requires that the board lacks and
every join the board has that the schematic does not; when the list empties they
press Run and the SAP-1 executes `LDA 9 / ADD 10 / OUT / HLT` and the output
register shows the sum. *Check:* a recorded gesture script replays to a
byte-identical `placement.brd` and a passing AT-C4-A; plus the **progressive-
disclosure guard** — with the breadboard view off, the first-year palette is
byte-identical to today's entries, asserted by the view-extended
`PaletteContractTest`.

**AT-C4-C (physical truth — the P1 gate as a falsifiable test).** *Seen:*
delete one jumper from a 74LS173's CLK net; the input goes **HIGH**, not LOW,
and the register free-runs exactly as the real board would. Swap to a 74HC173
and the same pin shows **X**, and the X propagates and turns the output display
red.

*Check —* `FloatingInputPhysicalTruthTest`, three lines: undriven 74LS173 CLK
resolves to `1` at `pull` strength; undriven 74HC173 CLK resolves to `X`; the
**same pin driven LOW** resolves to `0` at `strong` strength **and must differ
from the first**. The third line is the falsification guard: today all three
produce the identical integer `0`, so the test cannot be satisfied by accident
and goes green exactly when P1 stages 5+6+7 land.

**HARDEST PART — not the canvas and not P1, both of which are known, bounded,
priced work.** The schematic-to-breadboard correspondence is **not a
bijection**, so the disagreement cannot always be reported in logical terms. A
schematic net maps to a *set* of tie-point strips joined by jumpers; when a
student wires two holes the schematic has no net for, there is **no logical name
to attribute the error to**. So the discrepancy report must be legible in
physical terms — *"U3 pin 5 and U7 pin 11 are joined by the jumper in column
34, and nothing in your schematic joins them"* — not as a set difference over
stable ids. Making that message **good** is the part with no in-tree precedent
and no external prior art that solved it: Fritzing's ratsnest is exactly this
problem and it is the single top complaint about the tool after seventeen
years. **Budget for iteration on the message, not just the computation.**

**The design insight that makes the canvas tractable, and that Fritzing has not
found in 17 years:** the breadboard and the schematic are **not reconciled by a
merge, they are compared by a check, and the disagreement is the grade.**
Fritzing stores connectivity *per view* and reconciles by ratsnest. JLS should
store connectivity **nowhere** in the breadboard view — derive it by union-find
over hole occupancy — and make the schematic relationship a check. That is the
third option, and it is why capstone 4's canvas is 5–8 weeks rather than a
program.

### 1.5 CAPSTONE 5 — design a manufacturable PCB

**Scope reading.** *Manufacturable* = a fab accepts the artifact and builds it
without redesign, and the populated board works — so the chain ends in
gerbers/drill and a DFM-clean board, **not** in "JLS produces gerbers".
*Design a PCB* splits: JLS designs the **electrical** content (which part, which
package, which section, which pin, which net, which footprint is called for);
KiCad designs the **physical** content (placement, routing, stackup, DRC). Both
are design; refusing the second is not refusing the capstone — it is the
division every professional flow makes, and it is the maintainer's own format
reframe. *A PCB* = a **named board**: capstone 4's SAP-1 (stage 1: its
accumulator + adder module, ~7 packages; stage 2: the full ~35-package machine).

**The prior refusal turned on one real mechanical fact and was wrong only in
treating it as permanent.** `pcbnew` discards any netlist component with an
empty footprint field (`board_netlist_updater.cpp:151-160`, KiCad ref 10.0). A
footprint name is **one string per package in a data table**, and capstone 4's
package library already has to carry that table. Add a `footprint` column (~1
week) and the gate opens.

**WHAT A PERSON SEES.**
`jls -export sap1-alu.net -parts 74ls.parts examples/sap1-alu.jls` prints
*"packed 14 logic elements into 7 packages; wrote sap1-alu.net (7 components,
31 nets, 84 nodes, 0 unbound pins)"*. In KiCad 10, **File → Import Netlist…**
reports *"7 footprints added, 0 errors"*. The student places and routes.
`kicad-cli pcb drc --severity-error --exit-code-violations sap1-alu.kicad_pcb`
returns 0 violations. `kicad-cli pcb export gerbers` + `export drill` produce
the fab package. It is uploaded to a fab for ~$30 and three weeks later **a
board arrives, is populated, is clocked, and the accumulator adds.**

**AUTOMATED CHECK, JLS SIDE — `KicadNetlistAcceptanceTest`, headless, eight
assertions:**
1. Packing totality — every logic element maps to exactly one (refdes, package,
   section); errors aggregated per `PinBindings.parse`'s idiom.
2. **Footprint totality** — every `(comp)` record carries a non-empty
   `(footprint …)`. This is literally the condition
   `board_netlist_updater.cpp:151-160` tests.
3. Every pin bound — all 14 pins of a DIP-14 including VCC/GND appear in exactly
   one net or an explicit no-connect set; unused-section inputs **tied, not
   omitted**.
4. No unconnected net — every net has ≥1 Output node, ≥1 Input node, ≥2 nodes.
   Computable today because `Put` is sealed over Input/Output
   (`src/jls/elem/Put.java:16`).
5. Partition round-trip — re-parse the emitted `.net`; the induced partition
   over (refdes, pin) **equals** the source `WireNet` partition pushed through
   the packing binding, O(n·α(n)).
6. Determinism — byte-identical re-run.
7. Diff stability — insert one unrelated gate, regenerate, diff is
   **additive only**.
8. Fixture correspondence — the regenerated netlist is byte-identical to
   `test/fixtures/sap1-alu.net`, the file the committed
   `test/fixtures/sap1-alu.kicad_pcb` was built from.

**AUTOMATED CHECK, KiCad SIDE — `KicadBoardDrcTest`**, opt-in via
`ToolLocator.findOnPath("kicad-cli")` + `Assumptions.assumeTrue`, which is the
shipped idiom, verified at `test/jls/hdl/GhdlCompileTest.java:32-37`:
`kicad-cli pcb drc --severity-error --exit-code-violations --format json` exits
0; `pcb export gerbers` + `pcb export drill` produce a file-set matching a
committed manifest; if the schematic emitter is built,
`pcb drc --schematic-parity` passes. Pin the container by digest.

**THE SEAM, STATED PLAINLY.** netlist → board **cannot** be automated in KiCad
10 — there is no `sch import`; `pcb import --format` accepts only
auto/pads/altium/eagle/cadstar/fabmaster/pcad/solidworks; there is no import
job in `common/jobs/`. So the human imports, places and routes **once**, the
`.kicad_pcb` is committed as a fixture, and CI proves forever that JLS still
emits exactly the netlist that board was built from (check 8) and that the board
still passes DRC and still produces gerbers. **That is how hardware CI always
works.**

**FALSIFICATION GUARD.** Checks 2 and 8 both fail today for every JLS design —
verified: `grep -rniE "footprint|refdes|pinout" src/` returns **zero hits** at
HEAD, and refdes is not yet a pure function of circuit content. They go green
exactly when the package layer and the stable-id refdes rule land, and cannot
be satisfied by accident.

**HARDEST PART — not the format and not the footprint.** JLS's elements are
**word-level** and packages are not, and nobody priced the gap. A JLS `Adder` is
one element of arbitrary width (`propDelay = bits * 30`,
`src/jls/elem/Adder.java:259-262`); a 74LS83 is 4 bits. An 8-bit Adder is two
cascaded 74LS83s; a 32-bit one is eight. That needs a `cascade` rule in the
package layer (which pin is carry-in, which carry-out, how slice i binds to
slice i+1, what terminates the chain) and it creates **synthetic nets that exist
in no `.jls` file** — so the netlist emitter is **not** a pure projection of the
`WireNet` partition, and that must be in the IR from the start rather than
bolted on. 2–3 maintainer-weeks, shared with capstone 4, whose plan omits it.
**This document moves it into the spine (§2, row S17) so it is built once.**

**THE HONEST LIMIT THAT FOLLOWS, WITH ARITHMETIC RATHER THAN ASSERTION.** The
elements that decompose into cascadable 74-series parts: `Adder` (74LS83, w=4),
`Register`/`ShiftRegister` (74LS273/374/173/194, w=4–8), `Mux` (74LS153/151),
`Decoder` (74LS138/139), the gates (w=1), `Constant`, `TriState`
(74LS244/245). The elements that do **not** decompose at any width: `Memory`,
`RegisterFile`, `TruthTable`, `StateMachine`, `FieldExtend`, `SigGen`,
`TestGen`, `Display`, and `SubCircuit` (which flattens) — **9 of 35 registry
types** with no manufacturable realization, and two of them, `Memory` and
`RegisterFile`, are exactly what makes JLS good at CPUs. So capstone 5 is
manufacturable for designs drawn at or near gate level, plus the cascadable
word-level elements, plus a user-supplied `-parts` binding for anything else (a
`Memory` bound to a 62256 with its own pin map is a perfectly good `-parts` row;
the mechanism handles it, the default library cannot). **That is the true
boundary. It is sharper and more useful than "refuse", and it is a property of
physics, not of JLS's code.**

---

## 2. THE SPINE

**Definition, stated because it is ambiguous and the ambiguity moves the
number.** The spine is the set of capabilities **required by two or more**
capstones. That is exactly the set where building once versus N times is a real
saving; anything required by exactly one capstone is that capstone's own bill.
The **hard spine** — rows required by three or more — is reported separately.

**The spine is not a single number. It is a function of which capstones are in
scope.** With C2 and C3 out, rows S18–S25 fall away and the spine drops from
91–153 to **48–86**. Any quotation of "the spine costs X" without naming the
scope is dishonest, and §7.13 makes scope a maintainer decision.

**Ranking method, stated so it can be checked.**
`score = (required_count + 0.5 × beneficial_count) / midpoint_weeks`.
Ties broken by whether the row gates on nothing.

### 2.1 The spine, ranked

| # | Capability | Required by | Beneficial to | Cost (wk) | Score | Owner |
|---|---|---|---|---:|---:|---|
| S1 | **Export-policy totality test** over `ElementRegistry.ALL` (35 types) — every type lands in exactly one bucket | all 5 | — | **0.2** | **25.0** | P3 / F2a |
| S2 | **D1v — `LogicValue` permits widened** to `Word, Wide, WordN, WideN` + `radix()`/`digits()` with unreachable stubs | C3, C4 | C2, C5 | **0.2** | **15.0** | P1 stage 0 ⏰ |
| S3 | **Net-name stability** — names keyed off `Element.stableId`, not `getID()` | C4, C5 | C1, C2, C3 | 0.5–1 | 4.67 | P3 |
| S4 | **Headless op layer** — `CircuitOp.apply(Circuit, Graphics)` → `apply(Circuit, TextMetrics)` | C1, C2, C3 | C4, C5 | 1–2 | 2.67 | #167 |
| S5 | **Shared net-partition IR** extracted out of `HdlExporter` into `jls.netlist` | C4, C5 | C1, C2, C3 | 1–2 | 2.33 | P3 |
| S6 | **D1 uncompressed default SHIPPED WITH stable-id refs** | all 5 | — | 2–4 | 1.67 | P11 / P36 |
| S7 | **Instance identity** — the `view:instancePath:sid` key | C1, C4, C5 | C2, C3 | 2–3 | 1.60 | P30 ⏰ |
| S8 | **Module runtime CONSUMED** + the `OP_OBSERVER` notch actually fanned out | C1 | C2, C3 | 1–2 | 1.33 | #224 |
| S9 | **Fan-out / DC loading check** (package data, **not** P1) | C4, C5 | — | 1–2 | 1.33 | P31 |
| S10 | **D3 per-section versioning** with must-understand semantics | all 5 | — | 3–5 | 1.25 | P11 / P36 |
| S11 | **Per-view geometry** as an optional D3-versioned VIEW section | C1, C4 | C2, C3, C5 | 3–4 | 1.00 | P30 |
| S12 | **CircuitOp view discriminator** on the four geometric ops + exact inverses | C1, C4 | C3, C5 | 2–4 | 1.00 | P30 |
| S13 | **B6 — programmatic construction as `CircuitOp` verbs** (the D5 replacement for `riscv/build_cpu.py`) | C2, C3 | C1, C4, C5 | 3–5 | 0.88 | P30 |
| S14 | **Long-run ergonomics + two-lane CI split** + `timeout-minutes` | C2, C3 | C1, C4, C5 | 3–5 | 0.88 | P18 |
| S15 | **P19 tracked CPU-scale calibration fixture** — *blocks D5* | all 5 | — | 4–8 | 0.83 | P19 |
| S16 | **Packing pass + refdes in canonical stable-id order + BOM + wiring list** | C4, C5 | — | 2–3 | 0.80 | P31 |
| S17 | **Width decomposition / cascade rule** + synthetic inter-slice nets in the IR | C4, C5 | — | 2–3 | 0.80 | P31 |
| S18 | **Net identity** — stable ids for nets and groups | C1, C4, C5 | — | 4–6 | 0.60 | P30 |
| S19 | **Per-instance fidelity toggle** + boundary harness + T-null gate | C2, C3 | C1, C4 | 5–8 | 0.46 | P17 |
| S20 | **Byte lanes on `Memory`** (a write-mask input) + capacity as a byte budget | C2, C3 | — | 3–7 | 0.40 | P2 |
| S21 | **Package library `logical` section** (`jls.pkg`) — pinout, sections, gate equivalence, substitution | C4, C5 | — | 4–8 † | 0.33 | P31 |
| S22 | **HostBytePort device seam** + `Console` + transcript (one door granted at invocation) | C2, C3 | C4 | 10–16 | 0.19 | P14 |
| S23 | **Retirement-indexed parity harness** + ISA-parameterized `RetireRecord` + `mach.dev` differential | C2, C3 | C4 | 10–16 | 0.19 | P16 |
| S24 | **Engine constant factors — the semantics-PRESERVING 2.26x stack only** | C2, C3 | C1, C4 | 12–20 | 0.19 | P1-S0/S1, P8-T |
| S25 | **Checkpoint / state serialization + the D3 bulk-image section** | C2, C3 | — | 10–17 | 0.15 | P15 |

† S21 is banded 6–10 standalone; **4–8 after D8 absorption** — see §2.3.
⏰ = has an expiry date; see §7.1 and §7.6.

**SPINE TOTAL: 91–153 maintainer-weeks.**
**HARD SPINE (required by ≥3): S1, S6, S7, S10, S15, S18 = 12–23 weeks.**
**THE PURCHASE ORDER (S1–S5, gates on nothing, top five by score): 4.1–8.2 weeks.**

### 2.2 Why the spine is the reason five capstones cost far less than five projects

Built independently, the five capstones cost **334–556 maintainer-weeks**
(`cap-lattice.md` §10, per-capstone standalone totals summed). Built on this
spine they cost **233–387** (§3.3). **The spine saves 101–169 weeks — about
30%, or two to three and a quarter maintainer-years.**

The saving is concentrated, not diffuse. Three rows carry most of it:

- **S22 + S23 + S19 (25–40 wk)** are required by both C2 and C3 in full. Whichever
  of those two capstones runs second gets them **free**. Sequencing C2 before C3
  saves **42–70 weeks** on its own — about one maintainer-year, from ordering
  alone.
- **S21 + S16 + S17 + S9 (9–16 wk)** are the expensive half of *both* C4 and C5.
  C5 standalone is 15–23 weeks; C5 after C4 is **9–14**, and the difference is
  exactly these rows.
- **S7 + S18 + S11 + S12 (11–17 wk)** are the addressing scheme that C1, C4 and
  C5 all need. Today a flat `sid` is design-unique only because
  `SubCircuit.save` inlines the whole definition per instance; the moment
  definitions are shared (P7), that breaks. KiCad hit exactly this and rewrote
  around `SCH_SHEET_PATH` UUID chaining.

### 2.3 The one D8 absorption nobody found, and it is on the most-shared row

S21 — 74-series pinout data — is genuinely new primary data:
`grep -rniE "footprint|refdes|pinout" src/` returns **zero hits** at HEAD. The
sibling analysis surveyed Fritzing, Wokwi and Tinkercad and concluded it must be
authored from datasheets.

**It does not.** Two GPL-3.0 **Java** simulators carry exactly this data:
Logisim-Evolution's `std/ttl/` (69 `Ttl74xxx` files of 108) and hneemann's
Digital. D8 permits absorbing GPL-compatible code into this GPL-3.0-or-later
project. This is **transcription, not design**, it lands on the row shared by
two capstones, and it takes S21 from 6–10 to **4–8 weeks**. The saving is
2–4 weeks on the row with the second-most claimants in the study.

*Governance note, not a blocker:* Fritzing's parts library is CC-BY-SA-3.0
while its app is GPL-3.0. The **format** (`.fzp`/`.fz`) is always free to
implement and should be an importer; bulk-importing the parts **data** carries
per-part attribution and share-alike obligations that are a real burden at bus
factor 1. Confirm appetite before importing parts rather than just the format.
Logisim-Evolution and Digital carry no such split — both are GPL-3.0 throughout.

### 2.4 What the brief predicted that turns out to be false

The brief anticipated that shared/parameterized subcircuit definitions (P7) and
`jls.api` (P12) would be spine. **They are not.**

- **P7 parameterization (25–36 wk)** is *beneficial to all five and required by
  none*. Every capstone passes with today's per-instance deep copies
  (`SubCircuit.java:284-285`, measured sharing factor 1.00x). What P7 *does* is
  make S7's instance-path work **urgent** — it is the program that breaks flat
  sid uniqueness.
- **P12 `jls.api` (19–29 wk)** is likewise beneficial to all five and required by
  none. `src/jls/api` does not exist at HEAD (verified). D7 names it the
  extensibility story, and that is a legitimate goal — but each capstone needs
  **internal** programmatic construction, which is S13/`CircuitOp`, not a public
  surface.
- **P8's compiled engine half (24–35 wk)** is required by none: no capstone
  acceptance test is stated in wall clock.

**That is 68–100 committed weeks whose deferral pays for the spine almost
outright.** It is the single most consequential finding in the lattice, and it
is a *deferral with a named re-entry trigger* (§6.3), not a cancellation.

---

## 3. THE MARGINAL COST TABLE

Given the spine, this is what each capstone then costs **on its own**, and the
reduced slice that demonstrates it.

### 3.1 The table

| Capstone | Marginal cost given the spine | Standalone (no spine) | Reduced / demo slice | Reduced cost |
|---|---:|---:|---|---:|
| **C1** multi-discipline collaboration | **30–46** | 49–75 | Two headless replicas, one circuit, **two views**, a scripted op stream, byte-identical saves. No UI, no sockets, no crypto — `LoopbackTransport` and `ChaosTransport` are both in-tree and tested. | **10–15** |
| **C2** boot Linux, run commands | **32–58** | 155–250 † | **C2-min**: a JLS circuit boots Linux headless and a transcript is byte-compared. No fidelity boundary, no parity, no drawn machine, no GUI console pane. | **16.5–25** ‡ |
| **C3** ternary CPU + QDOS (+ DOOM) | **28–45** | 98–161 (ARCH-B) | A **drawn balanced-ternary CPU** — 27 registers, 16 trits/word, one-instruction three-way branch, free negation, exact round-to-nearest ÷3 — running a hand-written monitor from a flat image, printing `A>` to a **live console**, verified against the reference emulator **per retired instruction**, with the clock-period falsification guard. | **28–45** |
| **C4** breadboard CPU (canvas + physical sim) | **31–46** | 50–77 | **AT-C4-A** — the headless build plan. `jls -breadboard sap1.jls -lib 74ls -o plan/`. Zero P1, zero canvas, zero `jls.edit` code, **zero draw on the coverage commons**. | **1** § |
| **C5** manufacturable PCB | **11–19** | 15–23 | The SAP-1 accumulator + adder module alone: ~6–8 DIP packages, 2 layers, ~60×80 mm. ~$5–30 to fab, routable in an afternoon, and **every hard thing bites at once** — packing, cascading, power pins, decoupling, the footprint gate. | **10–15** |

† C2's 155–250 includes committed-roadmap slices (P1 stages 0/1, P2 in full,
part of P5, P8, P9's checkpoints, P12's floor, slices of P3/P7/P11/P13) that the
marginal column excludes. Different accounting, both consistent — see §11.2.

‡ Plus a **named 3–7 week byte-lane debt**. C2-min's behavioral core can
read-modify-write in Java on a 32-bit `Memory` and the `Console` can decode byte
addresses from the core's own address/size pins — but **a drawn core cannot do a
single-cycle RMW without byte enables**, so C2-min's machine is not the machine
C2-full will draw. Quote the debt in the same breath, every time.

§ C4's reduced slice is 1 week **only because §2 moved S9, S16, S17 and S21 into
the spine** — the package library, packing, refdes, cascade and DRC. Standalone
that slice is 11–18 weeks. This is the lattice thesis made concrete and
falsifiable: if funding the spine does not make C4's demo cost one week, the
thesis is wrong.

### 3.2 What each capstone's marginal column actually contains

**C1 (30–46):** `#167` completion — placement drop, paste, wire-attach finish,
dialog commits, `EditOrderedRows` (RGA), `ImportSubcircuit` (5–8); the per-kind
CRDT merge rule table, **shared with P11/lf-06 C4** (6–9); P32 — replica loop +
session service + transport UI + hardening (21–32, less the P11 overlap);
presence across views at ≤10 Hz, never persisted, never in the op log (3–5).

**C2 (32–58):** P20 `jls.mach` + the independent external golden (16–26); P21
the guest software stack (4–6, +3–5 hedge); the drawn RV32 machine brought up
boundary by boundary (12–26).

**C3 (28–45):** P23 the JLS-T3 ISA + reference emulator (6–10); the conformance
and T-null corpus (4–6); P24 the in-jar assembler/disassembler (3–5); P22-a BET
bundle discipline (2–3); the drawn T3 CPU, ~620–760 logic elements, band 450–950
(6–10); P25-lite the monitor (3–5); a P16 floor for a trace-regime comparator
(4–6).

**C4 (31–46):** P1 stages 5+6 — `LogicValue` lands byte-identical, then X becomes
producible and the resolution fold replaces `WireNet.propagate`'s
first-active-driver-in-net-order (16–22); P1 stage 7 — strength lattice, driver
kinds, `PullUp`/`PullDown`, net kinds (6–9); P33 the breadboard canvas **and its
GUI tests** (7–12); the physical-simulation binding (2–3); the view-aware palette
contract (~1).

**C5 (11–19):** H2 the `HdlModel` module-instantiation statement (3–4) — *and this
is the only capstone it blocks*; the hierarchy **flattening** walk (2–3); the
gEDA/Lepton `.sch` or KiCad `.net` emitter (3–4 for `.net`; 5–9 for `.sch`);
`physical.footprint` + the `-parts` binding (2–4); P34 the manufacturability
gate (1–2).

### 3.3 The rolled-up totals

| | Weeks |
|---|---:|
| Spine (§2) | **91–153** |
| C1 marginal | 30–46 |
| C2 marginal | 32–58 |
| C3 marginal | 28–45 |
| C4 marginal | 31–46 |
| C5 marginal | 11–19 |
| **ALL FIVE, ON THE LATTICE** | **233–387** |
| All five, built independently | 334–556 |
| **SAVING FROM THE SPINE** | **101–169 (30%)** |
| **All five REDUCED versions, on the lattice** | **96–154** |

### 3.4 Reconciliation with the sibling documents — where they disagree and who is right

This plan's numbers differ from three sibling totals. Each difference is stated
rather than smoothed.

1. **The spine: 91–153 here vs 85–142 in `cap-lattice.md`.** Two causes. (a) The
   lattice's own 23 rows sum to **90–152**, not 85–142 — its headline applied an
   unstated credit. (b) This document adds two rows the lattice omitted: **S3
   net-name stability** (0.5–1) and **S17 the cascade rule** (2–3), the latter
   identified in `cap-c5-pcb.md` as unpriced by anyone including
   `cap-c4-breadboard.md`. **91–153 is the number to use.**
2. **`cap-c3-ternary.md` §12 quotes its reduced version at 33–54 while its own
   step-table bands sum to 46–76.** The gap is explained if and only if P14 and
   P16 are paid by the spine. This plan charges C3's reduced slice **28–45**,
   which is the step table minus P14 (paid at S22) and minus P16's full cost but
   **plus** an explicit 4–6 week P16 floor for a trace-regime comparator, because
   the spine as sequenced here does not pay P16 in full before C3 runs.
3. **`cap-c4-breadboard.md` prices C4 at 50–77 standalone; `cap-lattice.md`
   charges it 60–95** because it bills C4 for spine rows C4 assumed were free.
   Both are right about different accounting. **The marginal column (31–46) is
   the one to plan against**, and the standalone column exists only to compute
   the lattice saving.

---

## 4. THE SEQUENCE

Optimized for **early demonstrable value**, which is read as: *a slice that
shows something at 40% completion, not one that shows everything at 100%*.

### 4.1 The unit, calibrated — and it reorders the plan

**Measured at HEAD:** 312 commits over 13 days; 104 merges; **198 of 312
(63.5%)** authored or co-authored by Claude. The cost bands' own anchor PR
(#201) landed **1,188 insertions across 14 files including 6 test files in ~23
hours** — 25–30x the study's assumed 200–250 tested lines per maintainer-week.

**So the week bands are not measuring typing. They are measuring irreversible
decisions and review risk.** Re-sorted on that basis, four resources are scarce
and only the first is in anyone's budget today:

1. **DECISIONS** — §7. Permanent once shipped.
2. **COVERAGE HEADROOM** — **2,897 addable uncovered LINE units, 1,475 BRANCH,
   14,668 INSTRUCTION** before the bundle floor fails. Computed at HEAD by
   solving `covered/(total+U) = floor` against `pom.xml:361, 366, 371`
   (0.545/0.535/0.505); LINE is 16,094/27,185 = 59.20%. §4.6.
3. **WALL CLOCK THAT IS NOT MAINTAINER TIME** — 1.7 h per structural boot;
   2–4 weeks per PCB fab turn; 4.1–20.7 h per DOOM time-lapse.
4. **PERISHABLE EXTERNAL INPUTS** — the RV32-nommu mainline removal proposed
   "by the beginning of 2027" is ~5 months out. §4.5.

### 4.2 The waves

**WAVE 0 — THE FORECLOSURE HEDGE. 4–8 weeks. Nothing demos, and that must be
said when funding it.**

| Item | Cost | Why here |
|---|---:|---|
| S1 export-policy totality over `ElementRegistry.ALL` | 0.2 | Closes the verified drift: 22 EXPORTED + 6 SKIPPED + 4 TOPOLOGY = **32 bucketed against a 35-entry registry**; `FieldExtend`, `Memory`, `RegisterFile`, `SubCircuit` are unbucketed and the first three abort at `HdlExporter.java:191-197`. |
| S2 `LogicValue` permits widened + `radix()`/`digits()` stubs | 0.2 | **0.2 wk now; 8–12 wk after the 27 `react()` bodies migrate.** A sealed permits list is a thing you widen once. |
| S5 net partition → a new floored `jls.netlist` package | 1–2 | 29 Verilog + 29 VHDL + 1 PCF goldens must not move — the strongest possible evidence that this is pure motion. |
| S3 net names keyed off `stableId` | 0.5–1 | **Must land before any golden is generated**, or every golden is regenerated twice. |
| S4 headless op layer, `Graphics` → `TextMetrics` | 1–2 | Uses the existing #77 `TextMetrics` seam. Add `jls.collab.op` to `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` in the same PR — the ratchet is what stops AWT creeping back into the replication path. |
| S8 the observer notch | 1 | `OP_OBSERVER` is declared at `src/jls/collab/op/OpExtensionPoints.java:25` and registered at `src/jls/boot/JlsModules.java:55`; the editor's anonymous sink at `SimpleEditor.java:5547-5570` is `op.apply(circuit, getGraphics()); markChanged();` and **never consults the registry**. Nothing downstream can start until local ops are observable. |
| `timeout-minutes` on every CI job | hours | **Zero occurrences across all six workflows**, verified, against a 6-hour hosted ceiling. |
| Re-home riscv/'s tracked dependents | ~1 | `RiscvCpuGoldenTest.java` and `test/fixtures/riscv-sum1to10.jls` must move before D5 deletes `riscv/`. |

**WAVE 0-OPTIONS — reserve four decisions in docs, ~0 weeks, highest value per
minute in the study.** These cost **zero code** and each costs **6–10 weeks plus
a format break** if taken later:

1. Reserve the key grammar `view:instancePath:sid` in `docs/file-format.md` and
   `docs/extension-points.md` **without implementing it**.
2. Reserve the **D3 section-id namespace** and the critical/ancillary
   must-understand rule in the first format PR, even though only one section
   exists.
3. Reserve the **`CircuitOp` view discriminator** shape before the replica loop
   ships — the op shape is in the wire envelope **and** the undo stack.
4. Reserve **refdes derivation as a pure function of circuit content**, ratcheted
   by a test on day one, or D2 diff stability dies at the physical tier.

**Constraint on later waves:** wave 3's hierarchy-flattening walk produces
hierarchical net names, which *is* an addressing scheme. It must **use the
reserved key**, not invent one. That is what discharges the deadline for ~0 weeks.

---

**WAVE 1 — THE FORMAT EPOCH. 5–9 weeks. ONE regeneration.**

S6 (D1 uncompressed default **with** stable-id refs, 2–4) + S10 (D3 per-section
versioning with must-understand semantics, 3–5), plus a headless
`Circuit.validate()` at the end of `finishLoad`, the legacy sort-order fix and
the `.gitattributes` lines.

**This is the highest-value sequencing rule in the document and no sibling
states it.** D1, D2 and D3 each regenerate every `.jls` golden, and **each
regeneration is a chance to bless a bug**. Land them atomically: one
regeneration, one review.

The ordering *within* the wave is itself the safety property. D1's uncompressed
default alone is the worst cell in the safety matrix — it removes XZ's
accidental binary-conflict protection while **keeping** the dense-id renumbering
hazard, which is the exact configuration that produced a clean three-way merge
that loaded and *simulated* a 4-bit pin carrying 0xFFF. Stable-id minting,
`Circuit.validate()`, the sort-order fix and `.gitattributes` are a hard
**precondition** of the container flip, not a preference.

*Demo:* small but real — `git diff` on a `.jls` shows a human-readable,
additive-only change where today it shows a binary blob.

---

**WAVE 2 — C4-A, THE HEADLESS BUILD PLAN. 9–17 weeks. → DEMO 1.**

S21 the package library as DATA (three D3-versioned sections:
`logical` / `electrical` / `physical.dip`), ~30 starter 74LS/74HC parts with
pinouts **absorbed under D8** from Logisim-Evolution `std/ttl` and Digital
(§2.3); S16 Tier-1 packing + stable-id refdes + BOM + pin-level wiring list;
S17 the cascade rule; S9 static physical DRC (all seven AT-C4-A checks); the
view-aware palette contract (~1 wk).

**Tier-1 packing is exactly optimal in O(n)**, not a heuristic: 74-series
small-logic packages are single-kind with interchangeable sections, so
`packages(k) = ceil(n_k / s_k)`. The inherited "refuse, it is bin packing"
verdict was a completeness judgement presented as a tractability one. Tier 2
(substitution — a NAND with inputs tied is an inverter) *is* a genuine covering
problem and ships opt-in later, documented as a heuristic **with its gap
reported**.

**Coverage-negative**: it lands in a new floored `jls.pkg` package, not in
`jls.edit`. Zero external dependencies. Iteration loop measured in seconds.
**Ships with a per-package JaCoCo floor in the same PR that creates it** (§4.6).

**DEMO 1 payload:** *a drawn NAND is one quarter of a 74LS00 that costs the same
as all four* — the direct ancestor of area and utilisation in FPGA and ASIC
flow. It catches the floating-input error **statically** (check 4), which is the
capstone's headline failure mode, and it **writes the specification-by-use for
the canvas rather than guessing it**.

---

**WAVE 3 — C5, THE BOARD. 10–15 weeks + fab latency. → DEMO 2, and it is a
physical object.**

**Do OQ-1 first; it costs nothing** (§7.12). Then: the hierarchy **flattening**
walk (2–3) — recurse `SubCircuit`, union child nets with parent through the
InputPin/OutputPin correspondence, cycle guard, hierarchical net naming; this is
**not** `HdlModel.InstanceStatement`, and that independence is worth 6–8 weeks,
because a flattened netlist never needs to know two subcircuits share a type.
Then `physical.footprint` (1–1.5); the KiCad `.net` emitter + goldens (3–4); the
`-parts` binding file mirroring the shipped 98-line `PinBindings` (2–4); the
acceptance harness — 8 headless assertions required, 3 `kicad-cli` assertions
opt-in and digest-pinned (1–2); and the board itself — human imports, places and
routes once, commits the `.kicad_pcb` fixture, orders from a fab, populates and
clocks it (1 wk + ~$30 + 2–4 weeks latency; **budget one respin**).

**Take the format reframe: KiCad IS the board view.** It is free, it is the
maintainer's own direction, and it removes 600–1,000 LINE units from the
coverage commons — which §4.6 shows is the difference between two canvases
fitting and three not.

---

**WAVE 4 — P21, THE GUEST IMAGE, PULLED FORWARD ON PERISHABILITY GROUNDS.
4–6 weeks (+3–5 hedge). Runs in PARALLEL from day one — it gates on nothing in
JLS.**

Demos nothing for years. It is insurance, and §4.5 is the argument.

**First action, one hour, free:** the RV32-nommu deprecation claim (a Feb-2024
patch proposing removal "by the beginning of 2027"; RV32 requires
`CONFIG_NONPORTABLE=y`) is **inherited and unverified**. Read the pinned
kernel's `arch/riscv/Kconfig` and the linux-riscv archive. One hour settles a
risk that otherwise sits under a 3-year plan.

Build the pinned kernel + busybox initramfs + DTB + reset stub + memory map
**now**, while the tree still builds it. Freeze by digest (kernel tag, `.config`,
toolchain container). Commit the checksummed rebuild recipe. **Never rebuild the
kernel in the required gate. Never track mainline.** Keep the
Sv32 + S-mode + OpenSBI hedge (+3–5 wk) documented as the non-deprecated path.

---

**WAVE 5 — C2-MIN, THE BEHAVIORAL BOOT. 16.5–25 weeks + a named 3–7 week
byte-lane debt. → DEMO 3, the world-first.**

L0 subset — the clocking-regime reconciliation, behavioral events/instruction,
the echo path (1.5–2); **S22's floor** — `Console` + `StdioPort`/`FilePort`/
`PipePort`, drained at `Simulator.beforeEvent` (the only thread-correct slot),
host access as **one door granted at invocation** (4–6); the capacity slice —
`-d 0` mapped to `Long.MAX_VALUE` not 0 (the loop guard is `<= maxTime`),
`DENSE_CAPACITY_LIMIT` as a byte budget, COW init, the D3 bulk-image section
(3–5); P20's floor — the behavioral RV32IMA+Zicsr+M-mode element (3–4); P21 (paid
in wave 4); S14's floor — the nightly long-run lane (1–2).

**`-d 0` must land before the first long run**: `defaultTimeLimit = 100000000`
(`src/jls/JLSInfo.java:69`, verified) is **1,920–2,300x short** of a Linux boot.
It is a hard ceiling, not a default.

**Ordering is binding on one point:** the `Console` work must **precede** any
scripting-API specification, or the `docs/vcd-interop.md` amendment becomes a
reversal of a permanent normative clause rather than the *recording of a
decision that was never recorded* — which is cheaper and stronger. There is no
recorded decision to reopen #63 (`ARCHITECTURE.md` has nothing for it), and
substantively there is no conflict: the console **pulls**, it is never called
back into. The contest is over wording only and is winnable only before the
wording is normative.

**Fund the clock-period falsification guard here (~1 wk), with its rung.** It is
the cheapest test in the study and everything downstream depends on it.

---

**WAVE 6 — P30 + C1-REDUCED. 19–29 weeks. → DEMO 4.**

S7 instance paths (2–3) + S18 net identity (4–6) + S11 the D3-versioned VIEW
section (3–4) + S12 the op view discriminator (2–4) + S13 programmatic
construction as ops (3–5), shipping `OldReaderTest` with it; then merge rules for
exactly those op kinds (2–3) and the replica loop + `CircuitConvergenceTest` +
`ViewIsolationTest` (3–4).

**Net identity is not optional bookkeeping.** `Wire.save` is literally
`// do nothing` (`src/jls/elem/Wire.java:120-126`, verified); nets are rebuilt by
traversal at load; and `HdlExporter` runs its **own** union-find that
additionally unions same-alias jump nets — so a jump pair is **one signal to the
exporter and two nets to the simulator**. Two net partitioners that can disagree
is a defect waiting for a capstone to surface it.

*Demo:* C1-reduced green — two headless replicas, one circuit, two views, a
scripted op stream, byte-identical saves. **The weakest-looking demo in the plan
and the one with the most downstream leverage**: it is the executable
specification of the merge rule table that P11/lf-06 needs anyway, so it is not
throwaway if the full capstone never runs.

---

**WAVE 7 — C3-REDUCED. 28–45 weeks. → DEMO 5, and it is the only capstone whose
acceptance test runs on every push.**

P23 the JLS-T3 ISA document + reference emulator (6–10) — 16 trits/word = 32 bits
BET; 27 registers (3 trits, compiler-driven not aesthetic — QBE's arm64 allocates
from ~30); fixed 16-trit instruction `[op:3][rd:3][rs1:3][rs2:3|imm7:7]`; ~28 ops.
P24 the in-jar assembler/`.timg` emitter/disassembler (3–5). AT-C3-N — the
conformance and T-null corpora (4–6), **the hardest item**. P22-a BET bundle
discipline, `-0+` display, illegal-code lint (2–3), with **no value-domain change
at all**. The drawn T3 CPU boundary by boundary (6–10). P25-lite the monitor,
console only (3–5). A P16 floor for the trace-regime comparator (4–6).

*Why it earns priority over the remaining tail:* **~30 minutes end-to-end, so it
fits a hosted nightly CI lane.** Everything else in this plan is nightly at best
and by-hand at worst, **and a capstone CI can defend does not silently rot.**

*Cheaper alternative if a ternary result is wanted before 2029:* **C3-micro,
8–14 weeks** — a 4-instruction ternary machine (ADD3/LD/ST/BR3), 3 registers, a
20-line monitor, one BET boundary to a binary `Console`, exhaustive 1- and
2-trit conformance. It proves the three novel things (BET encoding, mixed-radix
boundary, `-0+` display) without the ISA's ~30 irreversible encoding decisions,
at a quarter of C3-reduced's price.

---

**WAVES 8–12 — THE FULL-SCOPE TAIL.**

| Wave | Contents | Cost | Unlocks |
|---|---|---:|---|
| **8** | The machine seams in full: S25 checkpoint + bulk images (10–17); S23 P16 full, incl. the `mach.dev` differential harness (6–10 delta); S19 the fidelity boundary + its deliberately-failing null test + normative `docs/abstraction-levels.md` applied **retroactively** to Adder/Memory/TruthTable/StateMachine/ShiftRegister/RegisterFile/FieldExtend (5–8); S15 the tracked fixture, which unblocks D5 (4–8); S20 byte lanes (3–7); S14 in full (3–5) | **31–55** | Everything structural |
| **9** | C2 full: P20 `jls.mach` + the independent external golden (16–26); the drawn RV32 machine boundary by boundary (12–26); the structural headless boot (2–4 + wall clock) | **30–56** | AT-C2-S |
| **10** | C1 full: `#167` completion (5–8); the merge rule table **built once with P11** (6–9); P32 replica/session/transport/hardening (21–32 less overlap); presence across views (3–5) | **30–46** | AT-C1 |
| **11** | C4 full, the physical-truth half: #77 core extraction (roadmap cost, not charged here); P1 stages 5–6 (16–22); P1 stage 7 (6–9); P33 canvas + its GUI tests (7–12); the physical-sim binding (2–3) | **31–46** | AT-C4-B, AT-C4-C |
| **12** | The stretch: P26 BlockDevice + FilePort (3–5); P28 QBE t3 target + cproc + freestanding libc (14–24); P27 the framebuffer LogicElement (3–5); P29 DOOM bring-up + time-lapse renderer (4–8); P22-b/c the native radix-N domain + ternary element family, then **redraw T3 with radix-3 nets** (16–23) | **40–65** | DOOM; ternary as first-class |

### 4.3 The calendar

**Cadence, stated as an assumption with a basis, not a measurement.** A
maintainer-week = 40 focused hours.

- **S1** — 10 h/wk unassisted → 1 mw = **4 calendar weeks**
- **S2** — 20 h/wk → 1 mw = **2 calendar weeks**
- **S3** — 10 h/wk **with agent assistance at the level this repository
  demonstrated**, discounted hard from the observed 25–30x to an overall
  **2.5x** → 1 mw ≈ **1.6 calendar weeks**

From **2026-08-01, at S3:**

| | Cumulative (mw) | Calendar |
|---|---:|---|
| Wave 0 complete | 4–8 | Sep – Oct 2026 |
| Wave 1 complete | 9–17 | Nov 2026 – Feb 2027 |
| **DEMO 1 — AT-C4-A, the build plan** | 18–34 | **Feb – Aug 2027** |
| **DEMO 2 — AT-C5, a populated PCB on the desk** | 28–49 | **Jul 2027 – Mar 2028** (incl. fab + one respin) |
| Wave 4 (P21) | parallel | — |
| **DEMO 3 — AT-C2-I, Linux boots in a schematic simulator** | 44.5–74 | **Dec 2027 – Dec 2028** |
| **DEMO 4 — C1-reduced, byte-identical convergence across views** | 63.5–103 | **Jul 2028 – Oct 2029** |
| **DEMO 5 — AT-C3-K, a drawn ternary CPU running QDOS** | 91.5–148 | **May 2029 – Feb 2031** |
| All five, full scope (waves 8–12) | 257–422 | **2034 – 2039** |

**At S1 (10 h/wk unassisted) every date multiplies by 2.5:** DEMO 1 lands 2028,
DEMO 3 lands 2032, and all five at full scope is **2050 or later — a career, not
a plan**. The correct response at S1 is to ship the reduced versions of all five
(**96–154 mw = 7.4–11.8 calendar years at S1; 2.9–4.7 years at S3**) and declare
them.

**Non-maintainer costs, uncosted anywhere else in the study:** 51 hours of pure
waiting per 30-iteration structural bring-up campaign (5 hours with P15 restore
from a 90% sync point — **this is why S25 must be sequenced before structural
bring-up, and every sibling plan puts it after or omits it**); 1.1–5.6 h for a
30-second behavioral DOOM clip; 4.1–20.7 h for a 100-frame structural
time-lapse; 2–4 calendar weeks per PCB fab turn; ~$70–110 cash for board plus
breadboard parts.

### 4.4 Serial constraints — real collisions, not preferences

1. **P1's value-domain migration and P22-b's native radix domain rewrite the same
   27 `react()` bodies.** Strictly serial. This is a real dependency; the cost of
   ignoring it is doing the migration twice.
2. **P1 stages 5–7 and any structural CPU bring-up.** P1 changes what a wire
   carries, so every drawn-machine golden moves. Do the drawn machines entirely
   **before** P1 or entirely **after**, never across. This is why wave 11 sits
   after wave 9.
3. **The format epoch is one change, not three** (wave 1). See §4.2.
4. **Anything with a >1 h iteration loop must not be interleaved with interactive
   work.** Batch structural boots to nights.
5. **Two canvases must never be in flight at once.** The second to merge is the
   one that trips the bundle floor and gets blamed for the first's untested
   lines.

### 4.5 Perishability — the argument for wave 4 running out of order

Every capability in this plan is buildable whenever it is funded. **Exactly one
input is not under the project's control**: the RV32-nommu Linux target. A
Feb-2024 patch proposed removal "by the beginning of 2027" — ~5 months from now —
and every sibling calendar puts the structural boot in 2028–2031. So the guest
image is built **now**, frozen by digest, and never rebuilt in a required gate.

That converts the highest-ranked existential risk to capstone 2 into
**documentation rot**, which is recoverable. It costs 4–6 weeks of work that
demos nothing, and it is the only place in this plan where "do it now because it
demos nothing later" is the right call.

### 4.6 The coverage commons — a spine resource no single capstone document could see

**Measured at HEAD**, by solving `covered/(total + U) = floor` against
`pom.xml:361, 366, 371`:

| Counter | Covered | Total | Ratio | Floor | **Addable uncovered** |
|---|---:|---:|---:|---:|---:|
| LINE | 16,094 | 27,185 | 59.20% | 0.535 | **2,897** |
| BRANCH | 6,887 | 12,162 | 56.63% | 0.505 | **1,475** |
| INSTRUCTION | 76,791 | 126,232 | 60.83% | 0.545 | **14,668** |

**Three capstones want a GUI canvas.** Breadboard 850–1,350 lines + collaboration
UI 600–900 + an in-JLS board view 600–1,000 = **2,050–3,250 lines = 71–112% of
the entire remaining LINE budget.** **At most two fit, and only if nothing else
lands.**

Three responses, priced:

1. **Write tests per canvas** — 2–4 wk each. **Budget per canvas, never once.**
   Recommended, and already in wave 11's P33 number.
2. **Fund #91/#84 `jls.edit` testability** — unowned and **uncosted anywhere in
   the roadmap**; **priced here at 8–14 weeks**, being the op-layer migration's
   shape applied to a 5,852-line file. The root cause is one package and mostly
   one file: `jls.edit` is 37.1% of the bundle at 26.04% covered, is deliberately
   unfloored (`pom.xml:408-411`), is 23,910 lines, and `SimpleEditor.java` alone
   is 5,852 of them (24.5%). Filed as **P35**.
3. **Take the format reframe for C5** — removes the third canvas entirely, is the
   maintainer's own direction, and is **free**. Recommended and assumed
   throughout.

**Standing rule adopted by this plan: every new capstone package is born
floored.** `jls.pkg`, `jls.netlist`, `jls.mach`, `jls.mach.t3`, `jls.dev` each
ship with a per-package JaCoCo floor **in the same PR that creates it**, set ~1
point under its measured value. That converts a shared overdraft into per-package
budgets and stops a canvas hiding behind an emitter's coverage.

---

## 5. UNOWNED WORK, NAMED IN THE ROADMAP IDIOM

Stated as proposed programs so they can be set beside P1–P13 like for like.

| Program | Scope | Weeks | Floor | Serves |
|---|---|---:|---:|---|
| **P14** | Device and host-I/O seam: sealed `HostBytePort`, `Console` element (3 byte addresses: THR write, RBR read, LSR read returning `0x60｜data_ready`), host-thread ring drained at `Simulator.beforeEvent`, invocation-time grant, the transcript | 10–16 | 4–6 | C2, C3 req; C4 ben |
| **P15** | Simulation-state serialization, checkpoint/restore, bulk images; acceptance criterion `replay(ckpt[i]) == ckpt[i+1]` byte-identically in CI | 10–17 | 3–5 | C2, C3 req |
| **P16** | Retirement-indexed parity harness: `RetireRecord` as a Java record with RVFI's 12 fields and **no field** for cycles/time/pipeline/cache; first-divergence differ; verdict lattice where UNKNOWN and NOT_COMPARABLE are never passes; exclusion-set ratchet; the independent external golden; **the `mach.dev` device-model differential harness** | 10–16 (+2–4 ISA-param) | 4–6 | C2, C3 req; C4 ben |
| **P17** | The fidelity boundary, its harness, its deliberately-failing null test, and normative `docs/abstraction-levels.md` applied retroactively to seven shipped elements | 5–8 | 3–4 | C2, C3 req; C1, C4 ben |
| **P18** | Long-run ergonomics and the two-lane CI split: `timeout-minutes`, a large-fixture policy, batching `InteractiveSimulator`'s per-event trace/probe work | 3–5 | 1–2 | C2, C3 req |
| **P19** | In-tree tracked CPU-scale calibration fixture, benchmark harness and golden. **Blocks D5** | 4–8 | 2–3 | all five |
| **P20** | `jls.mach`: `ArchState`, `MemoryView` with two indistinguishable implementations, data-only decode table, pure `step()`, `Uart16550Model`, `ClintModel` driven by simulated time | 16–26 | 3–4 | C2 |
| **P21** | The guest software stack: pinned kernel + `.config`, busybox initramfs, DTB, memory map, reset stub, cross toolchain, checksummed rebuild recipe. **Gates on nothing in JLS** | 4–6 (+3–5) | 4–6 | C2 |
| **P22** | Mixed-radix value domain. **a:** BET bundle discipline, `-0+` display, illegal-code lint (2–3). **b:** native radix-N domain — `WordN`/`WideN`, radix on `Put` and `WireNet`, the loud `getBits()` shim over 89 call sites (10–14). **c:** ternary element family — MinGate, MaxGate, TernaryNegate, Literal (J_k selectors, required for functional completeness), the three converters, BalancedDisplay (6–9) | 18–26 | 2–3 | C3 |
| **P23** | JLS-T3 ISA specification + reference emulator in `jls.mach.t3` + the conformance and T-null corpora | 10–16 | 5–8 | C3 |
| **P24** | Balanced-ternary assembler + flat `.timg` emitter + disassembler, **in-jar**, so a student can edit one instruction and re-run without a toolchain | 3–5 | 2 | C3 |
| **P25** | QDOS monitor: banner, prompt, ~10 built-ins, `.TX3` loader, ~15-call ECALL syscall table, contiguous-extent filesystem (not FAT) | 5–8 | 3 | C3 |
| **P26** | `BlockDevice` element + `FilePort`. **Hard prerequisite of DOOM**, not a QDOS nicety | 3–5 | 3 | C3 |
| **P27** | Framebuffer element — a **`LogicElement`**, not a `DisplayElement` (`DisplayElement` permits `Text` only, and `Text` does not `react()`; JLS's own `Display` is a `LogicElement`). Batched-repaint contract written into the element **before** it is built, or 64,000 events/frame at 318 ns kills the queue | 3–5 | 2 | C3 |
| **P28** | C toolchain: **QBE** `t3/` target (MIT, v1.3 June 2026, active) + **cproc** front end (ISC, C11, already emits QBE IR) + freestanding libc. Build-time artifact only — never enters the offline jar | 14–24 | 8 | C3 |
| **P29** | DOOM bring-up over the six-function port surface (62,458 lines of C), CMAP256 8-bit pixels at 320×200 = 64,000 B/frame, WAD on the block device, offline time-lapse renderer | 4–8 | 2 | C3 |
| **P30** | **The shared per-view model** — instance paths, net identity, the D3-versioned VIEW section, the `CircuitOp` view discriminator, programmatic construction as ops. **⏰ +6–10 wk and a format break if deferred past P7/P3** | 14–22 | 5–7 | C1, C4, C5 |
| **P31** | **`jls.pkg`, the physical package library** — three D3-versioned sections (logical/electrical/physical), the O(n) packing pass, stable-id refdes, BOM, pin-level wiring list, cascade rule, static DRC | 12–21 | 9–14 † | C4, C5 |
| **P32** | The collaboration runtime — replica loop, session service (roster, floor control, heartbeats, anti-entropy, snapshot catch-up), transport UI (join/SAS-verify/key-change dialogs, bundled nameable glyph set), hardening (envelope signatures, per-peer caps, eject-and-revert), presence across views | 24–37 | 10–15 | C1 |
| **P33** | The breadboard canvas **and its GUI tests** — second coordinate space, hole hit-test, jumper snap, occlusion, the physical-terms discrepancy overlay | 7–12 | 7–12 | C4 |
| **P34** | The manufacturability gate — `kicad-cli pcb drc` + gerber acceptance + a gerber-vs-netlist connectivity check, digest-pinned | 1–2 | 1 | C5 |
| **P35** | **`jls.edit` testability (#91/#84)** — priced here for the first time. The gate on whether more than two canvases can ever exist | 8–14 | — | C1, C4, C5 |
| **P36** | **The format epoch** — D1 uncompressed with stable-id refs + D3 per-section versioning + `Circuit.validate()` + the sort-order fix + `.gitattributes`, landed atomically as **one** golden regeneration | 5–9 | 5–9 | all five |
| | **UNOWNED BLOCK TOTAL** | **196–320** | | |

**Against the committed roadmap of 288–424 weeks, the unowned block is 46–111%
on top.** But the plan does not fund all of it: waves 0–7 (the reduced versions
of all five capstones) consume **96–154 weeks**, of which roughly **75–120** is
unowned.

† P31's floor is the AT-C4-A slice with D8 absorption taken.

---

## 6. THE HONEST TOTAL AND WHAT IT DISPLACES

### 6.1 The total

| Scope | Maintainer-weeks | As % of the committed 288–424 |
|---|---:|---:|
| **The purchase order** (S1–S5, gates on nothing) | **4–8** | 1–3% |
| **Waves 0–1** (hedge + format epoch) | **9–17** | 2–6% |
| **Waves 0–3** (through DEMO 2 — two capstones green) | **28–49** | 7–17% |
| **Waves 0–7** (reduced versions of all five, five demos) | **96–154** | 23–53% |
| **All five, full scope, on the lattice** | **233–387** | 55–134% |
| All five, built independently (the counterfactual) | 334–556 | 79–193% |

**At bus factor 1 and 10 h/week with agent assistance, all five at full scope is
2034–2039. That is not a plan and this document does not present it as one.**
What *is* a plan is waves 0–7: **five demos, one of them a world-first, in
2.9–4.7 calendar years**, with the first landing in **six to twelve months**.

### 6.2 The three-way split of the committed roadmap

The five capstones do not simply add to the roadmap. They partition it.

**CONSUMED — committed weeks the capstones require (54–85 wk).** P1 stage 0
(2–3); P1 stages 5–7, which capstone 4 requires in full (22–31); P2's byte lanes
(3–7); P3's front — net partition, name stability, hierarchy flattening, the
instance statement, the emitter family (9–15); P11's merge rule table, shared
with capstone 1 (6–9); P1/P8's semantics-preserving engine work (12–20). **These
are not new spend. They are a sequencing decision.**

**DEFERRED — committed weeks that are beneficial to all five and required by
none (84–123 wk).** P7 parameterization (25–36); P12 `jls.api` (19–29); P8's
compiled engine half (24–35); P22-b's native radix domain (16–23). **Deferring
these buys the spine almost outright.** Each has a re-entry trigger (§6.3).

**NOT ON THIS PATH AT ALL (65–110 wk).** P4 timing and analysis beyond constant
factors; P6's silicon on-ramp beyond the 5–8 week package-library early slice;
P10 fault simulation and DFT; P5's formal half. These are neither needed nor
displaced. The reasons are substantive, not budgetary: **the parity contract
permits all timing to differ**; stuck-at faults have no bearing on booting; a
Linux SoC is orders of magnitude past a one-tile silicon budget; and unbounded
sequential equivalence on non-matching encodings is exactly what two fidelity
tiers are.

**MUST NOT BE DISPLACED.** The **D6 defect lane** — fixes land immediately, and
two already have (`970db41`, `36cbd37`). And the **D1/D2/D3 format work** (wave
1, 5–9 wk), which every capstone needs.

### 6.3 What gets cut, each with a named re-entry trigger

| Cut | Weeks | Re-entry trigger |
|---|---:|---|
| P7 parameterization | 25–36 | Any acceptance test that cannot go green without it. **None currently can.** |
| P12 `jls.api` | 19–29 | The extensibility story is prioritised over the capstones — a goal question (§7.14), not an engineering one. |
| P8's compiled engine half | 24–35 | An acceptance test stated in **wall clock**. None is. |
| P22-b native radix domain | 16–23 | ARCH-B ships and the pedagogy loss from BET bundles is judged too high (wave 12 buys it back). |
| AT-C2-S, the structural boot | 50–90 | Behavioral boot green **and** P15 checkpointing exists. Moved to a background campaign, not cancelled. |
| The live structural console | 30–45 | The maintainer funds it **with a work breakdown**. The arithmetic 19,500 → 44,000 → 52,600–95,400 cycles/s against a 1e5 interactivity floor makes it a **budget, not a limit** — the prior "physically impossible" framing was measured to be a cost. |
| One of {breadboard canvas, collab UI, board view} | 7–12 | #91/#84 (P35, 8–14 wk) is funded. Otherwise 71–112% of LINE headroom cannot fit three. |

### 6.4 What waves 0–3 specifically displace

**28–49 weeks, of which only ~15–25 is genuinely new spend.** The rest is P3's
front and P6's package-library early slice pulled forward. It displaces roughly
**P10 (12–18) plus P13 (13–18)**, or the entire standalone-items bucket (8–13)
plus P10.

**Phases 0–3 gate on nothing, draw nothing from the coverage commons, and turn
two of the five capstones green.** That is the strongest single argument in this
plan for the ordering it recommends.

### 6.5 The one displacement that runs backwards

**P30 gets more expensive if deferred.** 14–22 weeks now; **20–32 plus a format
break** once P7 or P3 write code, because both need an addressing scheme for
"which instance" and will invent one. It is the only item in the entire study
where waiting is the expensive choice — **and securing it costs a design
document, not code** (wave 0-options, ~0 weeks).

**Deciding to defer P30 is legitimate. Deciding it by not deciding is the
expensive path.**

---

## 7. DECISIONS THE MAINTAINER MUST MAKE BEFORE CODE STARTS

Each of these is effectively permanent once shipped. Each is stated as a
question with a recommended answer and the reason.

### 7.1 The instance-addressing scheme ⏰

**Q.** What is the key that addresses "this element, in this view, in this
instance of this subcircuit"?

**RECOMMEND:** `view:instancePath:sid`, where
`instancePath = instanceSid ('/' instanceSid)*`, with the **degenerate empty
path** permitted so the flat case costs nothing. **Reserve it in
`docs/file-format.md` and `docs/extension-points.md` in wave 0 (~0 weeks);
implement in wave 6.**

**WHY.** One table, five payoffs: per-view geometry, SDF `INSTANCE` keys, the
cross-probe map, package binding, and LibreLane cell names. Today a flat `sid`
is design-unique **only because** `SubCircuit.save` inlines the whole definition
per instance; sharing definitions breaks that. KiCad hit exactly this and
rewrote around `SCH_SHEET_PATH` UUID chaining. Cost of reserving: a design
document. Cost of not reserving: **+6–10 weeks and a format break.**

**Do not add a second `(x,y)` to `Element`.** `Element` has exactly one `x` and
one `y` (`src/jls/elem/Element.java:28-30`, verified), one `setXY`, one
`savePosition`/`restorePosition` — that is the single blocking fact for any
second view. A second pair does not extend to view 3, and capstone 5 is view 3.
Use a **side table keyed by (view, stableId)**; old readers skip it and still
open the circuit structurally.

### 7.2 The op-grammar shape ⏰

**Q.** Is the `CircuitOp` grammar extended once for multi-view and collaboration
together, or twice?

**RECOMMEND:** **once**, per D9, and **reserve the shape in wave 0**. Nine kinds:
view-qualified Move/Rotate/Flip; `SetViewItem`; `RemoveViewItem`;
`AddView`/`RemoveView` (epoch-fenced like `SessionEntry`); `SetAttribute`;
`EditOrderedRows` (RGA — the one genuine sequence-CRDT site); `ImportSubcircuit`;
plus path-qualifying `Ops.resolve`.

**WHY.** `MoveElements(List<ElementId>, int, int)` (verified at
`src/jls/collab/op/MoveElements.java:27`) has **no view field**, and
`AddElements` carries serialized element blocks which by construction cannot
carry view data. The op shape is in **the wire envelope and the undo stack**, so
extending it twice means extending it **incompatibly**. Reserving costs nothing;
retrofitting costs a protocol break.

### 7.3 The multi-discipline merge unit

**Q.** Per-view **sidecar** (Path B, ~0 marginal) or a typed `SetAttribute` op
(Path A, 3–5 wk)?

**RECOMMEND:** **both, sidecar first.** The sidecar falls out of the VIEW section
at near-zero marginal cost and is what makes "CS rewires while EE footprints"
mergeable **at all**. `SetAttribute` follows later, using the
`Element.savedAttributes()` enumeration that already drives the loader.

**WHY.** `SetElementConfig(ElementId, String block)` carries the element's
**entire** save block (verified, `SetElementConfig.java:48-53`). Any LWW rule
therefore discards one discipline's whole edit including fields the other never
touched. **The fix is not clever merge — it is separating the fields, then typing
them.**

### 7.4 The format epoch

**Q.** Do D1 (uncompressed default), D2 (stable-id refs) and D3 (per-section
versioning) land as one change or three?

**RECOMMEND:** **one** (wave 1, P36).

**WHY.** Each regenerates every `.jls` golden and **each regeneration is a chance
to bless a bug**. One regeneration, one review. D1 alone is the worst cell in
the safety matrix — it removes XZ's accidental binary-conflict protection while
keeping the dense-id renumbering hazard — so stable-id minting, a headless
`Circuit.validate()` at the end of `finishLoad`, the sort-order fix and the
`.gitattributes` lines are a hard **precondition** of the container flip.

### 7.5 The fidelity attribute's versioning

**Q.** FORMAT 3 bump, or route the fidelity attribute through D3's
must-understand mechanism?

**RECOMMEND:** **D3 must-understand, marked CRITICAL.**

**WHY.** `docs/file-format.md` says verbatim that writers SHOULD prefer a version
bump over an ignorable attribute whenever dropping it would change simulation
behaviour — and the SoC ships a CPU subcircuit whose **definition is initially a
stub** with a behavioral binding, so an older reader silently drops the binding,
runs the stub, and emits a **confidently wrong result with no diagnostic**.
`Element.setValue` silently `return`s on unknown attribute names
(`src/jls/elem/Element.java:344-351`, verified), so an ignorable attribute is a
silent-corruption path by construction. D3's must-understand gives the same
guarantee **per section** without a global version bump. **Applying D3 to the
image section but not to the attribute that changes simulation behaviour is an
inconsistent application of one decision to two cases in one design.**

### 7.6 The radix bound ⏰

**Q.** What is the maximum radix, and when is it declared?

**RECOMMEND:** **R ≤ 5 with full X/Z/U, declared in the sealed permits list in
wave 0 (0.2 wk).** `LogicValue permits Word, Wide, WordN, WideN` with
`radix()`/`digits()` and unreachable N-ary stubs.

**WHY, with arithmetic.** Three bit-sliced planes hold `R + |status alphabet| ≤ 8`
symbols, giving **R ≤ 5** with the full X/Z/U status set. The fourth-plane cliff
at radix ≥ 8 was measured; radix-2's fast path costs **zero**; the tagged-union
alternative was rejected at +32% time and +16 B/value. Radix 3 and 4 are what
the capstones need and 5 is free headroom for capstone 6. **0.2 weeks now;
8–12 weeks after the 27 `react()` bodies migrate**, because a sealed permits
list is a thing you widen once.

### 7.7 Capstone 3's architecture — the single largest cost decision in the study

**Q.** ARCH-B (binary-encoded trits in memory/addresses/IO, ternary confined to
the datapath and ALU) or ARCH-N (native ternary throughout)?

**RECOMMEND: ARCH-B, decisively.**

**WHY, with arithmetic and external evidence rather than taste.**

| | ARCH-B | ARCH-N |
|---|---:|---:|
| Standalone | **98–161 wk** | 190–330 wk |
| After capstone 2 | **56–91 wk** | — |
| As % of the committed roadmap | 24–57% | **46–117% — it can consume the whole thing** |
| Seam reuse from capstone 2 | **~60–70%** | ~25% |

The decisive evidence is not preference:

- **ISO C 6.2.6.2p1** requires unsigned integers to use a **pure binary
  representation**. A hosted C abstract machine on a balanced-ternary word is a
  research question, not a retarget.
- **QBE has four hard-wired IR value classes** — `Kw`, `Kl`, `Ks`, `Kd` — verified
  by reading `ops.h` and `all.h`.
- **DOOM's own `m_fixed.c:38`** is `((int64_t)a * (int64_t)b) >> FRACBITS`.

Under ARCH-B, **DOOM compiles with zero source changes** and QBE retargets at
arm64 scale (**measured: 1,514 lines**). Under ARCH-N there is no retarget, only
a compiler research project, and *"it runs DOOM"* degrades to *"it runs a fork of
DOOM"*. **Plausibly the difference between a 6-month and a 3-year capstone.**

**On the compiler specifically — do NOT default to an LLVM backend.** Ranked:
1. **QBE** (MIT, v1.3 June 2026, active) + **cproc** (ISC, C11, already emits QBE
   IR). **Recommended.** Retarget surface is `targ.c` (register set), `isel.c`
   (ternary/binary selection, the BR3 pattern), `abi.c` (calling convention),
   `emit.c` (assembly text). arm64's 1,514 lines is the anchor.
2. **chibicc** (MIT, 8,916 lines, frozen Dec 2020, with a uxn retarget
   precedent) — the fallback.
3. **TCC** (LGPL-2.1, stable, frozen 2017) — third.
4. **lcc is DISQUALIFIED**: its `CPYRIGHT` carries a no-sale clause that is
   GPL-incompatible, so D8's absorb route is closed.
5. **LLVM: never** — orders of magnitude larger, and the retarget is the whole
   project.

**What breaks honestly, even under ARCH-B:** word size in trits (16 chosen, see
§7.15), byte addressing (binary side, so it works), pointer representation
(binary), char size (8-bit, binary). The **binary interop instruction set**
(BAND/BOR/BXOR/BSHL/BSHR/BMUL64H) must be **in** the ISA — and the seam should be
**taught, not hidden**: it is the same lesson the mixed-radix contract puts
between subcircuits, moved inside the CPU.

**And the governance question that comes with it (§7.16).**

### 7.8 The device extension-point id

**Q.** What extension-point row does the device seam file, and when?

**RECOMMEND:** **one row, filed before any code.** `jls.device` contributes
sealed `HostBytePort permits {StdioPort, FilePort, PipePort, BlockPort}`; host
access is **one door granted at invocation**; injection is drained at
`Simulator.beforeEvent`, the only thread-correct slot.

**WHY.** Extension-point ids live in the module manifest and are **permanent
once a third-party module names one**. The module runtime shipped and is "wired
and unconsumed" — `OP_OBSERVER` is declared at `OpExtensionPoints.java:25` and
registered at `JlsModules.java:55` with zero contributors (verified). This is
also the **first host door**: `System.in` and `ProcessBuilder` both have zero
occurrences in all of `src/`. Get the shape right once.

### 7.9 Capstone 5's format reframe

**Q.** Is KiCad the PCB view, or does JLS host its own board canvas?

**RECOMMEND: the reframe — KiCad is the PCB view.**

**WHY.** It is free; it is the maintainer's own stated direction; and the
coverage arithmetic (§4.6) says three canvases do not fit (2,050–3,250 lines
against 2,897 addable LINE units). It takes capstone 5 from 32–54 to **11–19
marginal weeks**. Routing, DRC and gerber generation stay in KiCad — **that is
not a refusal, it is the division every professional flow makes**, and
`kicad-cli` makes it CI-assertable, which is *better* for the acceptance test
than owning it.

Note the contrast the maintainer already drew: capstone 4's breadboard canvas is
**ruled in** and teaches something JLS uniquely can (the schematic-to-physical
check). A board canvas would make JLS the worse KiCad.

### 7.10 The default 74-series subfamily

**Q.** 74LS or 74HC as the shipped default?

**RECOMMEND: 74LS as the default and the demo; carry both in the library.**

**WHY.** With 74LS the DC fan-out check is **real** (IOL 8 mA / IIL 0.4 mA = 20
unit loads) and **floating-reads-HIGH is unambiguous** — both are the pedagogy.
74HC's DC fan-out check is **vacuous** (input current ~1 µA; the real limit is
capacitive loading, which is on the analog side of the line) and its input-float
is **undefined**, not HIGH. Capstone 4 needs both regardless — AT-C4-C's second
line asserts the 74HC part resolves to **X** — but capstone 5 needs a default,
and 74LS matches the SAP-1 and the historical designs.

*Counter to state honestly:* 74HC is what a student can actually buy today. If
the demo is meant to be physically built by students rather than by the
maintainer, ship both fully, at the cost of doubling the library data work.

### 7.11 The agent-assisted cadence

**Q.** Is the agent-assisted cadence expected to continue?

**RECOMMEND: state it, publish what a maintainer-week means, and re-baseline
quarterly against merges-per-month actually observed.**

**WHY.** 198 of 312 commits (63.5%) over 13 days are Claude-authored or
co-authored, and the anchor PR landed 1,188 insertions across 14 files in ~23
hours — 25–30x the study's assumed line rate. **Every date in §4.3 turns on this
by 4x (S1 vs S3).** This is the single highest-value input the maintainer can
give and **no amount of further analysis substitutes for it.**

### 7.12 Where the guest image lives — an in-document contradiction, one side must be withdrawn

**Q.** Does `docs/virtual-hardware-parity.md` §8 exclusion 8 (no committed
multi-MB payloads, no Git LFS) get **narrowed to checkpoints only**, so the
~2–6 MB pinned guest image can be committed as a stored container member?

**RECOMMEND: yes, narrow it — and do so explicitly, as a reopening, not as a
route-around.**

**WHY.** §8 forbids committed multi-MB payloads; §6 requires a nightly lane that
boots and diffs; **a hosted runner has no payload unless it downloads one.** One
of those must give. Measured: 2,397,301 bytes of `.git` over ten revisions of a
stored image against a raw sidecar's 2,396,453 — **within 0.04%**. The offline
jar is untouched: the image is a **test fixture**, not a jar payload.

**Related and cheap: OQ-1 for capstone 5, and it should be done first because it
costs nothing.** Hand-write a ten-line KiCad `.net` with one `(comp)` carrying a
real footprint LIB_ID and two nets, and run File → Import Netlist… in KiCad 10.
Do the same for a ten-line gEDA `.sch` with an embedded symbol. **The
embedded-symbol path is in KiCad's code but not in KiCad's manual — it was read,
not run.** This is the cheapest possible falsification of capstone 5's central
claim.

**Also cheap, also unresolved: OQ-2.** Does a gEDA schematic with four `slot=`
instances sharing `refdes=U1` survive KiCad's ERC and netlist export, given that
a slot-imported symbol has `UnitCount() == 1` and `findNextSymbol` only
de-duplicates by refdes when `UnitCount() > 1`? The reading says **no**.
Consequence: route the **board** flow through the netlist emitter, not the
schematic's slot mechanism — which is what §4.2 wave 3 already does, so this is
a confirmation, not a fork. One afternoon.

### 7.13 Which capstones are actually in scope

**Q.** All five, or a subset?

**RECOMMEND:** commit to **waves 0–3 unconditionally** (they gate on nothing,
draw nothing from the coverage commons, and turn two capstones green for
28–49 weeks), and choose the rest after DEMO 2.

**WHY.** The spine is a **function of scope**: 91–153 with C2 and C3 in, **48–86**
with them out. With C2 and C3 out, P14 + P16 + P17 + P15 fall away entirely.
**The cheapest coherent subset is {C4, C5, C1-reduced} at roughly 75–115 weeks
for three demos**, and it is a legitimate answer. Choosing now lets wave 0
reserve only the options it needs; choosing later means reserving all of them,
which is also fine and only slightly more work.

### 7.14 Does `jls.api` still come before the capstones?

**Q.** P12 (19–29 wk) is named by D7 as the extensibility story, but no capstone
gates on it. Before or after?

**RECOMMEND: after.**

**WHY.** Each capstone needs **internal** programmatic construction, which is
S13/`CircuitOp`, not a public surface. `src/jls/api` does not exist at HEAD. The
same question applies to P7 (25–36) and P8's compiled half (24–35). Together
that is 68–100 committed weeks whose deferral pays for the spine almost
outright. **But this is a goal question, not an engineering one** — if the
extensibility story is the priority, it does not defer, and the plan reorders
without breaking.

### 7.15 The remaining ISA-shape decisions (capstone 3), bundled

| Question | Recommendation | Reason |
|---|---|---|
| Which ternary algebra? | **min/max lattice with complement negation** | The **only** family that collapses **exactly** to today's behaviour at N=2 — so the radix-2 fast path costs zero and every existing golden is unchanged. There are 3^9 = 19,683 ternary two-argument functions and no canonical ternary AND; this is a taste question with a decisive engineering tiebreak. **Only the maintainer can answer, but this is the reason.** |
| 16 trits or 18? | **16** | Aligns to the 32-bit substrate (`Memory`'s `bits <= 64` dense store, `RegisterFile`'s count × bits, QBE's `Kw`). 18 is Setun-faithful and costs a non-power-of-two `Memory` width, 4 wasted bits per word, and the `Kw` mapping. **Say why in the write-up**: a 16-trit balanced word holds log2(3^16) = 25.36 bits in 32 bits of storage — **79% density, intrinsic to BET, and a teachable number.** |
| Binary interop instructions in or out? | **IN** | C works, DOOM compiles unmodified, and the seam gets **taught** rather than hidden. Out, the machine is purer and the toolchain becomes a research project. |
| Does the assembler ship in the jar? | **Yes** (and therefore under the coverage floors). The **C compiler: no** — build-time only, so D8's orchestrate-vs-reimplement test never fires. | A student must be able to edit one instruction and re-run without a toolchain. |

### 7.16 Governance, not technical: a 6–10 week C-language work item in a Java project at bus factor 1

**Q.** Is the QBE `t3/` target — the only non-Java deliverable in the plan —
acceptable?

**RECOMMEND:** yes, **vendored** (QBE MIT + cproc ISC copied into `tools/` with
licence notices rather than depended on), and **accept that the guest C
toolchain is a dev-time tool, not in the single offline jar.**

**WHY.** The all-Java alternative — a hand-written IR and code generator emitting
T3 assembly — costs **12–18 weeks instead of 6–10** but keeps one language and
one test culture. That is a real trade and it is the maintainer's to make.
**Stretching the jar promise to cover a cross-compiler is what would make
capstone 3 unaffordable.**

### 7.17 Three loose ends that need a one-line ruling each

1. **D6's `#77` referent.** #77 is **CLOSED** (2026-07-25). The sequencing rule
   "everything else waits on #77" describes a gate already discharged. If D6
   meant "wait for the module runtime to be **CONSUMED**", the referent is
   **#224**, which is open, is 1–2 weeks, and sits on capstone 1's critical path.
   D6 is binding, so this goes back to the maintainer rather than being resolved
   silently. (Related drive-by: `CONTRIBUTING.md:21` names #33 as the tracking
   issue that orders the program; #33 is also closed, 2026-07-27.)
2. **Does #221's equivalence criterion need amending before P1 lands?** #221 names
   the two-states-plus-HiZ value domain and multi-driver/tri-state resolution
   **by reference** (`docs/simulation-semantics.md` §§2, 9). **P1 changes the
   criterion itself**, so those sections must be re-anchored by #221's own stated
   process **before** P1 lands. A governance prerequisite on the critical path of
   the longest pole.
3. **Undo semantics under concurrency.** "Undo reverts only your own operations"
   (the mainstream collaborative answer, **recommended**) vs whole-state undo. A
   UX ruling, not a technical one — and #171's replica loop **cannot be finished
   without it**.

---

## 8. HOW A SIXTH CAPSTONE WOULD BE COSTED

The maintainer said the list is not exhaustive. **The plan must therefore be
extensible by construction, and this section is the construction.**

### 8.1 The procedure

1. **Write its acceptance test first** — what a person sees, and the automated
   check, including a falsification guard that fails today for a stated reason.
   If the acceptance test cannot be written, the capstone is not yet a capstone.
2. **Mark it against the 25 spine rows** in §2.1: *required* / *beneficial* /
   *not needed*.
3. **Its marginal cost** = Σ(rows it marks REQUIRED that no funded capstone
   already marks required) + Σ(its own private rows).
4. **Check the four non-week budgets**: does it need a **new decision** (a format
   section, an op kind, an extension-point id, a radix)? Does it want a
   **canvas** (at most two ever fit)? Is its **iteration loop** longer than an
   hour? Does it depend on a **perishable external input**?
5. **If it needs a new op kind or a new format section, it pays the reservation,
   not the rewrite** — provided wave 0's reservations were taken. That is the
   entire value of wave 0-options.

### 8.2 Four worked examples

| Capstone 6 candidate | Spine rows it requires | Private rows | **Marginal cost** |
|---|---|---|---:|
| **"Run CP/M on a drawn Z80"** | S22, S23, S19, S25, S14, S15, S20, S24 — all already spine | Z80 ISA model + reference emulator; the drawn Z80; a CP/M BIOS on the block device | **20–39 wk** — 21% of capstone 2's own drawn-machine bill, because the seams were built general |
| **"A manufacturable FPGA daughterboard"** | S5, S16, S21 — already spine | One `-parts` row; one footprint column entry | **2–4 wk** — `PinBindings` (98 lines) and `PcfEmitter` (199 lines) already ship |
| **"An I2C bus lab: open-drain wired-AND across three devices"** | S21, S9 (package data); **P1 stage 7** | Three device models; one lab handout | **3–6 wk if capstone 4 is funded** (stage 7 delivers open-drain, net kinds and PullUp/PullDown); **25–34 wk if not**, because it then buys P1 stages 5–7 alone |
| **"Export a Verilog testbench a student runs in Verilator"** | S5 net-partition IR, S3 name stability, S1 export totality — all spine | The testbench harness; a `ToolLocator`-gated opt-in CI check on the shipped `GhdlCompileTest` idiom (`test/jls/hdl/GhdlCompileTest.java:32-37`) | **4–7 wk** — `VerilogEmitter` is 752 lines with 29 goldens and already ships |

The spread — 2 weeks to 39 — is the point. **The lattice does not make everything
cheap. It makes the price legible before anyone starts.**

### 8.3 The lattice's own acceptance test

**Demonstrable end state:** a sixth capstone is costed **mechanically, in under
an hour**, by marking it against the 25 rows and summing.

**Automated check:** a small script (`vhw/lattice.py` in the sibling work)
re-runs the whole matrix and reprints every total in this document, so **any
claim here is falsifiable by editing one mark and re-running.**

**The falsifiable claim, stated so it can be wrong:** *if capstone 6's marginal
cost cannot be computed from the 25 spine rows in under an hour, the lattice is
wrong and must be re-derived.* The cheapest test of the whole thesis is wave 2:
**if funding the spine does not make capstone 4's demo cost one week and
capstone 5's board cost 11–19 instead of 15–23, the thesis is false and it is
falsified cheaply.**

### 8.4 What would make the lattice need a redesign, and what would not

**Would NOT need a redesign** (these fall out of existing rows): a new radix ≤ 5;
a new view; a new device on the `HostBytePort` seam; a new ISA; a new package
family; a new export format; a new physical DRC rule; a new op kind, *if* wave
0-options were taken.

**WOULD need a redesign, each named with its cost:**

- **A radix > 5** — the fourth-plane cliff. Cost: re-encode the value planes,
  ~10–14 wk on top of P22-b. **Mitigated by declaring R ≤ 5 now (§7.6).**
- **Continuous-time analog** — capstone 4's §4 criterion draws the line
  deliberately: discrete iff the value at any instant is a function of current
  net state over a finite lattice. A capstone needing a state variable
  integrating over continuous time is a **different simulator**, and the honest
  answer is the maintainer's own format reframe: **ngspice is the analog view**,
  and JLS emits a SPICE `.subckt` (already inside P3's emitter family).
- **A third simultaneous canvas** — the coverage commons says no (§4.6). Cost of
  making it yes: **P35, 8–14 wk**, and it should be costed rather than
  discovered at a merge.
- **True multi-master, offline-capable git-branch divergence across views** — the
  CRDT structurally cannot cover it (a CRDT converges over a shared **op
  history**; two branches share an ancestor and two **result states**, and `.jls`
  persists no op log). Cost: it is already priced — **P11/lf-06's C4, 9–14 wk**,
  and it is the **same object** as capstone 1's merge table.

---

## 9. THE ONE-PAGE SUMMARY

**FIRST DEMO: capstone 4's headless build plan (AT-C4-A), Feb–Aug 2027 at the
assisted cadence.** `jls -breadboard sap1.jls -lib 74ls -o plan/` produces a BOM,
a refdes map, a pin-level wiring list and a DRC report a person orders parts
against and builds. It is the only slice that is cheap, strongly demoable,
dependency-free, **and coverage-negative**. It demos at 40% completion. It pays
for the expensive half of capstone 5. And it delivers the payload — *a drawn NAND
is one quarter of a 74LS00 that costs the same as all four*.

**THE SPINE: 91–153 maintainer-weeks, 25 rows, saving 101–169 weeks (30%) over
building five projects.** The purchase order — S1 export totality (0.2), S2 the
permits widening (0.2), S3 name stability (0.5–1), S4 the headless op layer
(1–2), S5 the net-partition IR (1–2) — is **4–8 weeks, gates on nothing, and is
required or beneficial to all five capstones without exception.**

**THE DEADLINE: P30.** 14–22 weeks now; **20–32 plus a format break** after P7 or
P3 write code. **Securing it costs a design document, not code** — reserve
`view:instancePath:sid`, the D3 section-id namespace, the `CircuitOp` view
discriminator and refdes purity in wave 0, for ~0 weeks.

**THE HONEST TOTAL.** Waves 0–7 — reduced versions of all five, five demos, one
of them a world-first — is **96–154 maintainer-weeks = 2.9–4.7 calendar years at
the assisted cadence, 7.4–11.8 unassisted.** All five at full scope is **233–387
weeks = 2034–2039, and this document does not present that as a plan.** It
displaces **84–123 committed weeks** (P7, P12, P8's compiled half, P22-b) that
are beneficial to all five capstones and **required by none** — and that deferral
pays for the spine almost outright.

**WHAT MUST NOT BE DISPLACED:** the D6 defect lane, and the D1/D2/D3 format work
— 5–9 weeks that every capstone needs.

---

## 10. WHAT I WOULD PUT IN FRONT OF THE MAINTAINER FIRST

In priority order, because at bus factor 1 the maintainer's attention is the
fifth scarce resource and nothing in this study modelled it:

1. **§7.11 — is the agent-assisted cadence expected to continue?** Every date
   moves by 4x. One sentence.
2. **§7.1 + §7.2 — take the wave-0 reservations.** ~0 weeks, and they are the only
   items in the study where waiting is the expensive choice.
3. **§7.7 — ARCH-B or ARCH-N for capstone 3.** The difference is 92–169 weeks —
   more than the entire reduced version, three times over.
4. **§7.13 — which capstones are in scope?** The spine is 91–153 or 48–86
   depending on the answer.
5. **§7.12's OQ-1 — one afternoon, no cost.** It falsifies or confirms capstone
   5's central mechanical claim, and capstone 5 is the cheapest whole capstone on
   the board.

---

## 11. VERIFICATION RECORD

### 11.1 Facts re-verified at `b299d63` while writing this document

| Claim | Evidence |
|---|---|
| Committed roadmap is 288–424, not 281–410 | `docs/capability-roadmap/AMENDMENT.md:979` — `\| **TOTAL, BOTH SWEEPS** \| **288–424** \| **66–98** \|`; `:991` notes 300–440 without shared-node credits |
| `HdlModel` has **11** concrete `Statement` subclasses, none instantiating | `src/jls/hdl/HdlModel.java:202, 253, 285, 317, 353, 401, 468, 538, 615, 725, 840` (+ abstract base `:113`) |
| Export policy is incomplete | `HdlExporter` EXPORTED(22) + SKIPPED(6) + TOPOLOGY(4) = **32** of a **35**-entry `ElementRegistry`; unbucketed: `FieldExtend`, `Memory`, `RegisterFile`, `SubCircuit` |
| `defaultTimeLimit` is a hard ceiling | `src/jls/JLSInfo.java:69` — `public static final long defaultTimeLimit = 100000000;` |
| `Memory`'s dense store caps at 1<<22 | `src/jls/elem/Memory.java:1224, 1234` |
| An un-wired input is literally 0 | `src/jls/elem/LogicElement.java:470-482` — *"Initialize all inputs to 0"*, `in.setValue(BitSetUtils.Create((long)0))` |
| `OP_OBSERVER` declared, registered, unconsumed | `src/jls/collab/op/OpExtensionPoints.java:25`; `src/jls/boot/JlsModules.java:29, 55` |
| `CircuitOp.apply` takes AWT `Graphics` | `src/jls/collab/op/CircuitOp.java:51` |
| `MoveElements` has no view field | `src/jls/collab/op/MoveElements.java:27` |
| `SetElementConfig` carries the whole save block | `src/jls/collab/op/SetElementConfig.java:48-53` |
| `Element` has one `(x,y)`; `stableId` separate from `id` | `src/jls/elem/Element.java:21-30` |
| `Wire.save` is a no-op | `src/jls/elem/Wire.java:120-126` — *"Wires don't get saved."* / `// do nothing` |
| No footprint/refdes/pinout data anywhere | `grep -rniE "footprint\|refdes\|pinout" src/` → **0 hits** |
| Proposed new packages do not exist | `src/jls/{api,pkg,netlist,mach,dev}` → all **absent** |
| No CI timeouts | `grep -rn "timeout-minutes" .github/workflows/` → **0** |
| Coverage floors | `pom.xml:361, 366, 371` → 0.545 / 0.535 / 0.505 |
| Coverage headroom | Computed from `target/site/jacoco/jacoco.csv`: LINE 16,094/27,185 = 59.20% → **2,897** addable; BRANCH 6,887/12,162 = 56.63% → **1,475**; INSTRUCTION 76,791/126,232 = 60.83% → **14,668** |
| Opt-in external-tool test idiom ships | `test/jls/hdl/GhdlCompileTest.java:32-37` — `ToolLocator.findOnPath` + `Assumptions.assumeTrue` |
| Chaos/loopback transports and `RosterConvergenceTest` ship | `test/jls/collab/net/ChaosTransport.java`, `LoopbackTransportTest.java`; `test/jls/collab/session/RosterConvergenceTest.java`; 45 source files under `src/jls/collab` |
| The merge table is one object, ruled | `docs/capability-roadmap/AMENDMENT.md:447-455` — *"the per-kind merge rule table is the same object for the online collaborative merge and the offline git merge… Build it once, in state-based form, offline first"* |
| `collab.op-observer` has zero contributors | `AMENDMENT.md:432` |
| #77 is on the critical path | `AMENDMENT.md:832-838` |

### 11.2 Where this document's arithmetic differs from its sources

| Figure | Sibling | Here | Why |
|---|---:|---:|---|
| Roadmap total | 281–410 (brief) | **288–424** | `AMENDMENT.md:979` verified |
| Spine | 85–142 (`cap-lattice`) | **91–153** | Lattice's own 23 rows sum to 90–152; + S3 (0.5–1) and S17 (2–3), the latter unpriced by anyone |
| `HdlModel` statements | 10 (brief) | **11** | Verified; and it blocks four *exports* but **one** capstone, a 4x overstatement of reach |
| C3 reduced | 33–54 (`cap-c3`) | **28–45** | Its step table sums to 46–76; the gap is P14+P16 being spine. Here P14 is spine, P16 is charged as a 4–6 wk floor |
| C4 marginal | 50–77 standalone / 60–95 (`cap-lattice`) | **31–46** | Marginal-given-spine accounting, stated as such |
| Coverage BRANCH headroom | 1,476 (`cap-realist`) | **1,475** | Recomputed; rounding |

**Method, stated once and applying to every number in this document that is not
labelled *measured*:** cost bands are **analogies against shipped work** — #78's
registry, #166's canonical save, #167's op layer, #199's synchronous memory,
#201's 1,188 insertions across 14 files — on the same basis as `AMENDMENT.md`'s
own estimates. **They are not measurements and must be quoted that way.** The
only measured figures here are the coverage headroom (§4.6), the engine
constants inherited from the brief, the element and line counts, and the commit
statistics.
