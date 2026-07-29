## Causal debugging and time-travel: "why is this signal what it is?"

*A leapfrog study extending `docs/capability-roadmap/README.md`. Every claim about
JLS is anchored to a path and line at HEAD. External claims are marked verified or
unverified individually; where a claim is verified only through secondary sources,
that is said.*

---

### What is missing today

**Nothing in the tree records why a value is what it is.** The simulator computes
provenance on every propagate and discards it on the same line.

The clearest instance is `WireNet.propagate`. At `src/jls/elem/WireNet.java:454-485`
the tri-state arm walks the net's ends, finds the driving `Output`, and holds a
reference to the exact element that won the resolution:

```java
	if (out.getValue() != null) {
		if (actual == null) {
			actual = out.getValue();          // :464-465 — the winning driver, known here
		}
		else if (!actual.equals(out.getValue())) {
			conflict = true;
		}
	}
	...
	value = actual;                           // :484 — the driver identity is dropped
```

Ten lines later (`:488-510`) the resolved value is cloned into every sink `Input`
and a bare `PinChanged` is posted:

```java
	inp.setValue(newValue);                                   // :502
	sim.post(new SimEvent(now, (Reacts) element,
			new SimEvent.PinChanged()));                      // :507-508
```

`Input.setValue` (`src/jls/elem/Input.java:59-62`) is a two-line field write:
`currentValue = value`. It does not record when, and it does not record from where.
`Put.currentValue` (`src/jls/elem/Put.java:385`) is a bare `@Nullable BitSet`.
`SimEvent` (`src/jls/sim/SimEvent.java:96-102`) is `(time, seq, callBack, todo)` —
there is **no cause field**, no producing event, no arrival provenance. The event
loop polls at `Simulator.java:224`, fires at `:239`, and the event is unreachable
garbage by `:242`. `SimEvent.PinChanged` is a zero-field record (`:30-31`); the
census in `docs/capability-roadmap/keystone-c-performance.md:126-127` shows it is
**82.3% of all events fired** — four out of five events in a run carry no
information at all beyond "something upstream moved."

**The trace machinery is one field short of being a causality log, twice.**
`TraceSample` (`src/jls/sim/TraceSample.java:19`) is `record TraceSample(long time,
BitSet value)`. `Trace.Change` (`src/jls/edit/Trace.java:51`) is `record
Change(BitSet value, long when)`. Both are append-only per-signal value histories,
both dedupe on value equality (`BatchSimulator.java:173-178`,
`Trace.java:186-191`), and **neither records which event produced the change** —
which the caller has in hand at both write sites (`BatchSimulator.afterEvent`
receives the `SimEvent` at `:140` and reads its callback at `:148`;
`InteractiveSimulator.afterEvent` does the same at `:884`).

**What JLS and its users do instead — the workarounds, which are the evidence.**

1. **Click an element, get a modal dialog with one number and no time.**
   `ElementValueDisplays.show` (`src/jls/edit/ElementValueDisplays.java:29-33`)
   dispatches to `Element.showCurrentValue` (`src/jls/elem/Element.java:831`),
   whose typical implementation is `InputPin.java:228-238`: format the value three
   ways and hand it to `TellUser.info`. That is the entire in-GUI value-inspection
   surface. It answers "what is this now", never "why", never "when did it become
   that."

2. **Mark elements watched and read a waveform whose history is wiped on every
   run.** `InteractiveSimulator.runSim` calls `initSimulation()` at `:603` (which
   sets `now = 0` at `Simulator.java:180`) and then `traces.clear()`,
   `traceMap.clear()`, `wireMap.clear()`, `memTraces.clear()` at `:612-619`. There
   is no way to go back to time T without re-running from 0 and losing everything
   the previous run showed. `Trace` retains at most `MAX_RETAINED_CHANGES = 100_000`
   changes (`Trace.java:32`) — a *display* bound with no persistence behind it.

3. **Build a halt condition out of gates.** `Stop` (`src/jls/elem/Stop.java:147-161`)
   and `Pause` (`src/jls/elem/Pause.java:164-178`) are 1-bit-in, zero-out elements
   whose whole job is to call `sim.stop()` / `sim.pause(true)` when their input is
   non-zero. To stop "near the moment it went wrong" a student draws a comparator
   feeding a `Stop`. `docs/standards-adoption/05-riscv-compliance.md` step 3 does
   exactly this with a magic-address comparator. This is a breakpoint, hand-built
   out of logic, and it only goes *forward*.

4. **Bisect by deletion.** The universal student method: delete half the circuit,
   re-run, put it back. There is no tool support for it and no record that it
   happened.

5. **At CPU scale, diff the final state in Python.** `riscv/verify.py:66-76` builds
   a `problems` list (`x5: ref=0x2a hw=0x0`, `dmem[12]: ...`) by comparing
   end-of-run registers and memory against a reference emulator. It reports *that*
   the CPU is wrong and gives no path back to the cycle where it diverged.
   `riscv/fuzz_diff.py` finds failing programs; nothing localizes them.
   `examples/autograde/autograde.py:53-57` hard-codes `EXPECTED_STDOUT_LINES` and
   reports a string mismatch — a grade with no diagnosis attached.

6. **Step forward, one event at a time, and hope.** The only temporal control is
   `InteractiveSimulator`'s `stepEnd` (`:774-805`): peek the queue head, and if it
   is past the step boundary, set `now = stepEnd` and pause. Forward only. There is
   no `Simulator.runUntil`, no `restore`, no `cancel` (P4 already notes the last of
   these, `README.md:398-399`).

**And the roadmap already names the missing skill without noticing that no tool in
the tree provides it.** `docs/capability-roadmap/sweep-01-values-and-logic.md:117-119`:
*"With V1 they see `x` spread through the cone and they learn to trace it back — the
single most transferable debugging skill in RTL."* **"Trace it back" is, today,
entirely a manual activity.** P1 creates the question; nothing in P1-P6 answers it.

A structural check confirms the gap is not merely undocumented:
`grep -rni "causal\|root cause\|backtrace\|time.travel\|rewind\|step back" src/`
returns only `src/jls/collab/crdt/CausalBuffer.java` and its neighbours — causal
*message delivery* for collaborative editing, an unrelated use of the word.

---

### The capability

Two capabilities that share one substrate. Build the substrate once.

#### (a) Causal tracing — the causality journal

**The observation that makes this cheap.** Every value in JLS arrives through
exactly two mechanisms, and both already exist as single choke points:

- **Computation.** An element's `react` reads its inputs and posts a delayed
  `NewValue`/`TriStateOff` to itself. `Gate.react`
  (`src/jls/elem/Gate.java:694-719`) is the archetype: `computeOutput()` at `:700`,
  change-check against `toBeValue` at `:704`, `sim.post(new SimEvent(now+propDelay,
  this, new NewValue(value)))` at `:707`. That is a causal edge from *this
  element's input state at time `now`* to *the arrival at `now+propDelay`*.
- **Distribution.** `Output.propagate` (`src/jls/elem/Output.java:136-169`)
  change-checks at `:139-145` and calls `WireNet.propagate`, which resolves and
  distributes. That is a causal edge from *the winning driver* to *each sink
  `Input`*.

So the causal graph is already the shape of the code:
`Input values → (react) → SimEvent → (Output.propagate) → net resolution → Input values`.
Nothing about it needs to be inferred; it needs to be **written down**.

**The record. Decision: journal at net-change granularity, not per-event.**

```java
// jls.sim — headless core, AWT-free, covered by HeadlessCoreRatchetTest
public record NetChange(
        long  time,        // when this value took effect
        int   siteId,      // dense index of the value site (net or Put)
        Value value,       // BitSet today; LogicValue.Word after P1-S1 (inline, no alloc)
        int   producerId,  // the element that computed it, or -1 for stimulus/init
        int   causeIndex,  // index of the NetChange that this producer's fire consumed
        Kind  kind)        // COMPUTED | RESOLVED | STIMULUS | INIT | CONFLICT
```

Per-event journalling is the obvious design and it is wrong. The census
(`keystone-c-performance.md:126-131`) says the 6004-cycle CPU run fires **2,331,793**
events of which **1,919,891 are `PinChanged`** carrying no value, and only
**378,129 are `NewValue`**. Net changes are fewer still, because
`Output.propagate:139-145` already suppresses unchanged values. **A full-fidelity
journal for the entire flagship run is ~380 K records, not 2.3 M** — at 24 bytes a
record (8 time + 4 site + 4 producer + 4 cause + 4 inline value at ≤32 bits) that is
**≈ 9 MB for 6004 simulated CPU cycles**. A classroom circuit's whole run fits in
tens of kilobytes.

**The backward walk.** "Net N holds V at time T" resolves in four steps, each
O(log n):

1. Binary search N's change list for the last change at or before T. This is
   `Trace.firstChangeAtOrBefore` (`src/jls/edit/Trace.java:445-458`) — the algorithm
   is already written and already unit-tested (`TraceWindowingTest#firstChangeAtOrBeforeMatchesTheLinearScan`).
2. That record names the producing element and the fire that scheduled the arrival.
3. That fire names the element's input `Put`s and the time it read them.
4. For each input `Put`, find its net and recurse at the fire time.

The walk terminates at `STIMULUS` (a `SigSim`/`TestGen`-posted event), `INIT` (the
`initSim` seed), or a sequential boundary (`Register` naming its clock edge and D).

**Decision: over-approximate the inputs, then refine four elements.** The expensive
version of this asks every element "which inputs did you actually read?" — 25
`react` implementations to audit. Do not. Default `causalInputs()` to *all attached
inputs of the element*, which is exactly correct for 21 of 25 reacts and merely
*wider than necessary* for four: `Mux` (only the selected data input and the
selector matter), `TruthTable` (only the matched row's inputs), `Register` (only D,
at the edge), `Memory` (only the addressed word). Over-approximation is **safe** —
it never omits the true cause, only adds an irrelevant one — so the feature is
correct on day one and gets sharper on a named, greppable worklist. This is
keystone B's `zeroFill()` mechanic (`README.md:742-748`) applied to a second
migration, and it is the reason this program can keep the tree green throughout.

**Rendering. Two views, one dataset.**

- **The cone on the schematic.** Highlight the elements and nets in the backward
  closure, coloured by hop rank. **Decision: do not reuse `Element.highlight`.** It
  is a single boolean (`src/jls/elem/Element.java:41-42`), owned by selection,
  painted pink by `ElementRenderSupport.drawHighlight`
  (`src/jls/edit/ElementRenderSupport.java:31-37`), and `setHighlight` notifies the
  circuit (`Element.java:571-575`). The cone is *view state, not circuit content*.
  Hold it GUI-side as a `Map<ElementId,Integer>` overlay consulted by
  `CircuitRenderer`, keyed by the permanent id at `Element.java:24`. The model gains
  nothing and stays AWT-free.
- **The causality tree**, and this is the one that must exist first:

```
net "alu_out"[3] = 1 @ t=41200
 └ Adder "add1" fired @ t=41190  (propDelay 10)
    ├ input A[3] = 1 @ t=41180  ← net "rs1_data" ← Mux "regsel" @ t=41170
    └ input Cin[3] = 1 @ t=41185 ← net "c2" ← AndGate "g7" @ t=41175
       ├ input a = 1 ← net "a2" ← InputPin "A" ← STIMULUS @ t=40000
       └ input b = 1 ← net "b2" ← InputPin "A" ← STIMULUS @ t=40000   ⚠ same source
```

  The tree is the headless artifact: `jls -b --why 'alu_out[3]@41200' circuit.jls`
  prints it deterministically to stdout. **That is the thing that makes causal
  debugging gradeable, diffable and CI-testable**, and it is what nobody else has.

**Under P1: X-source tracing.** The same walk with one added rule — at a node whose
value is X, follow only the producer inputs that were themselves X. P1's
three-valued gate tables compute exactly that predicate, so the pruning is free.
This prunes the cone to the X cone, which is what Verdi's Temporal Flow View and
Questa's Time Cone do. It is also the **only** way to make the cone legible on a
32-bit datapath (see risk 1). Under two-state values the same machinery answers
"why is this **0**", which is the most common first-year question precisely because
a floating input reads 0 today.

**The compiled-evaluation tension — addressed, not deferred.** The standalone
levelized/compiled pass (`README.md:700`, 10-16 wk) evaluates 522 slots in one
statically-ordered sweep with **no `SimEvent` objects and no `WireNet.propagate`
call**. Naively, that destroys the journal.

**Resolution: make the journal a property of the value *site*, not of the event.**
Both engines assign values to the same set of sites (Puts and nets); each writes
`NetChange` when a site's value differs from the previous one. The compiled pass's
`producerId` is a *compile-time constant per slot* and its fan-in is *statically
known* — the levelization computed it. So the compiled pass emits a **cheaper and
more complete** journal than the interpreter, not a worse one. What it genuinely
cannot reproduce is arrival time *within* a timestamp, i.e. transport delay — which
is precisely what `ARCHITECTURE.md`'s recorded decision #221 and `README.md:700`
already say a cycle-based strategy cannot reproduce. Therefore: **event granularity
in the interpreter, timestamp granularity in the compiled engine**, stated in the
"declared alternative strategy" section of `docs/simulation-semantics.md` that the
compiled item already requires.

**The ordering constraint that falls out, and it is real:** the journal's site index
must be designed **before** the levelization's slot table, because they are the same
table. Cheap if done in that order; a second, disagreeing index if not.

#### (b) Time travel — checkpoint and replay

**What actually has to be captured.** The simulator's mutable state is finite and
enumerable:

| State | Location |
|---|---|
| `now`, `stopping`, `maxTime` | `Simulator.java:36,44,38` |
| `eventQueue`, `dupCheck` | `Simulator.java:25,27` |
| the event sequence counter | **`SimEvent.java:87` — a mutable `static long`** |
| every put's value | `Put.currentValue`, `Put.java:385` |
| every net's value + conflict latch | `WireNet.java:405,407` |
| scheduled-output shadow | `toBeValue` in 7 classes: `Gate.java:653`, `Mux.java:493`, `Adder.java:353`, `Register.java:694`, `TriState.java:445`, `Decoder.java:427`, `ShiftRegister.java:588` |
| register contents | `Register.java:696` |
| memory contents | `Memory.java:981`; **`WordStore.copy()` already exists at `Memory.java:1145-1148`** |
| FSM state, cached display values | `StateMachine`, `Display.java:346`, `Pause.java:130`, `JumpStart.java:427`, `JumpEnd.java:374` |

**Three reductions make this tractable rather than heroic.**

1. **The event queue's bulk is regenerable, not stored.** Max queue depth on the
   6004-cycle run is **12,093** (`keystone-c-performance.md:126`) — and that is
   essentially all pre-posted stimulus, because `SigSim.initSim` posts **every**
   `-t` event during initialization: the initial value at `src/jls/elem/SigSim.java:123`
   and every subsequent `for`/`until` transition at `:194-196`. 2 × 6004 = 12,008
   clock transitions; the number matches. `Clock` likewise self-reposts exactly one
   event at a time (`src/jls/elem/Clock.java:415-424`). So a checkpoint stores only
   the **dynamically posted** events — in-flight gate/register/memory arrivals,
   which at any instant number on the order of the design's logic depth, **tens of
   events, not twelve thousand**. Restore rebuilds the stimulus tail by re-running
   `SigSim` and discarding everything at or before the checkpoint time.
2. **Circuit structure is invariant during a run.** `runSim` disables the editor for
   the duration (`InteractiveSimulator.java:636-638`). The checkpoint therefore
   stores *only* simulation state keyed by `ElementId` (`Element.java:24`) plus a
   within-element ordinal — no geometry, no wires, no save-format text.
3. **Deterministic replay is already a written contract.** `docs/simulation-semantics.md`
   §3: ordering is "fully deterministic — a pure function of circuit content."
   Replay forward from a checkpoint therefore reproduces the run bit-for-bit, which
   is what lets checkpoints be coarse.

**Format. Decision: a new `jls.sim.SimCheckpoint`, not the `.jls` save format.**
`CircuitSnapshot` (`src/jls/edit/CircuitSnapshot.java:60-68`) is the right precedent
for the *technique* — serialize, deflate at `BEST_SPEED` (`:142-158`), and expose a
byte-equality test (`sameAs`, `:118-121`) — and the wrong one for the *content*: it
captures circuit text (invariant here) and it lives in `jls.edit` (GUI-side).

```
magic "JLSCKPT" + format version
long now, long seqNext, long maxTime
site table    : sorted by (ElementId, putOrdinal) -> value
net table     : sorted by canonical net id -> value, conflictReported
element state : sorted by ElementId -> per-class blob (toBeValue, Q, FSM state)
memory deltas : per Memory ElementId, only addresses in WordStore.present
dynamic queue : (time, ElementId, payload) for events with no regenerable source
```

Sorted everywhere, so **two checkpoints of identical state are byte-identical** —
inheriting `CircuitSnapshot`'s best property, which becomes the correctness oracle
(see risk 3). Deflated with the same helper.

**Size, for the flagship design.** Census at `keystone-c-performance.md:104-108`:
1551 elements (810 `WireEnd`, 513 `Wire`), 225 logic elements, 297 nets, maxBits 32,
three `Memory` elements at 256 × 32 bits (`riscv/build_cpu.py:349,398`).

| Component | Raw |
|---|---:|
| ~810 put values @ 4 B | 3.2 KB |
| 297 net values @ 4 B | 1.2 KB |
| ~225 element state blobs | ~1 KB |
| 3 memories, written words only | ≤ 3 KB |
| dynamic queue, tens of events | < 1 KB |
| **Total raw** | **≈ 9 KB** |
| **Deflated (save text deflates ~4×)** | **≈ 2-3 KB** |

That is the entire simulation state of a 1,551-element RV32I CPU. It fits in a
network packet.

**Interval policy. Decision: adaptive by fired-event count, not simulated time**,
because simulated time is not uniform in work. At the measured **318 ns/event**
(`keystone-c-performance.md:139`), a 50 ms replay budget is **N ≈ 150,000 events**.
On the 6004-cycle run (2,331,793 events) that is **16 checkpoints ≈ 40 KB
deflated**. Interactively, floor the policy at one checkpoint per second of wall
time so "step back" is always instant on a classroom circuit.

Layer a **logarithmic retention ladder** on top: keep every checkpoint within the
last N events, then every 2nd, 4th, 8th going back. Total storage is O(log run
length) with dense recent history — the standard reverse-debugger policy, and the
right one here because students step backward from *now*, not from t=0.

**Replay mechanism. Three methods on `Simulator`; the event loop is untouched.**

- `SimCheckpoint checkpoint()` — snapshot at a quiescent point (between events).
- `void restore(SimCheckpoint)` — load state, then rebuild the regenerable tail.
- `void runUntil(long targetTime)` — the existing loop with a bound. This is
  `InteractiveSimulator`'s `stepEnd` logic (`:774-805`) generalized and pushed down
  into `Simulator`, which is where it always belonged.

"Step backward one event at time T" = restore the newest checkpoint at or before T,
`runUntil(T-ε)`. **`runEventLoop` (`Simulator.java:215-243`) does not change**,
which is what keeps this compatible with `docs/grand-architecture.md:325-330`'s
hot-plane rule: checkpointing is a cold-plane operation performed *between* events,
and the per-event cost is one counter increment.

**Does the operation layer give us most of the machinery? No — and the reason is
worth stating, because the shape is seductive.** `CircuitOp`
(`src/jls/collab/op/CircuitOp.java:34-64`) is a sealed, validated, serializable,
**invertible** command vocabulary with `invert(Circuit before)` at `:64` and a
single typed entry point `OpSink.submit` (`src/jls/collab/op/OpSink.java:24`). That
is exactly the machinery a time-travel debugger wants — except it is invertible over
*circuit structure*, not *simulation state*. Its eleven kinds (`CircuitOp.java:35-37`)
are `ToggleWatched`, `AttachProbe`, `RemoveProbe`, `RotateElement`, `FlipElement`,
`MoveElements`, `AddElements`, `RemoveElements`, `SetElementConfig`, `AddWire`,
`RemoveWire`. **Not one of them touches a value.** And simulation is not invertible
in the op sense at all: an AND gate whose output falls to 0 has destroyed the
information about which input fell. So the op layer contributes **one idea and zero
code** — the discipline that state change flows through one typed, validated,
serializable entry point. The simulation-side analogue of that discipline is the
journal in (a), not `CircuitOp`.

**The event queue, by contrast, contributes a great deal**: it is already the
complete description of "what is going to happen", which is exactly what a
checkpoint must capture, and `SimEvent.compareTo` (`:133-150`) already makes the
ordering deterministic.

#### Why (a) and (b) belong in one program

The journal answers "why" but is O(run length): ~9 MB for 6004 CPU cycles, and
unbounded for a long batch run. Checkpoints let you **not keep it**. Retain the
journal only in a window around the cursor; when the student scrolls back past the
window, restore the nearest checkpoint and replay **with journalling on** to
regenerate exactly the journal they need. That converts causal tracing's storage
from O(run) to O(window).

**Neither capability is half of the other: the journal is the payload, the
checkpoints are the index.** Built separately, each is a demo. Built together, they
are a debugger.

---

### What it unlocks

**Standards. Almost none, and that is the finding.** The 304-entry survey contains
nothing that specifies interactive debug provenance — which is precisely why the
previous sweep could not see this capability. The nearest neighbours are the
waveform tier as the *artifact* it would export into (#66 VCD, #67 EVCD, #69 LXT/FST)
and #53 UCIS as a coverage database — **none of them carries causality**. The one
commercial format that stores enough to drive a causal tracer is **#70 FSDB**, a
proprietary binary with no published format and no independent reader, correctly
declined at `README.md:1116-1118`.

**So JLS's causal artifact would be its own format. There is no standard to conform
to — which also means there is no standard to be behind.** That is an argument for
building it, not against; it is exactly the class of capability the standards frame
is structurally blind to.

**Engineering capabilities.**

- **Localizing a CPU regression.** `riscv/verify.py:66-76` reports
  `x5: ref=0x2a hw=0x0` — a final-state diff with no path back to the divergent
  cycle. With `--why` the harness asks "why is x5 0 at the end", walks back through
  the register file's hand-synthesized write-enable mesh (the **62 of 228 elements**
  `riscv/build_cpu.py:239-252` exists to build, per `README.md:262-265`), and names
  the cycle and the control signal that failed. The difference between "the CPU is
  wrong" and "the `blt` arm of the branch comparator never asserts."
- **Dynamic combinational-loop and oscillation diagnosis.** JLS has no loop
  detection at all today; P4 notes static detection "falls out free" from STA
  (`README.md:416`). The backward walk detects a *dynamic* oscillation for free —
  it hits a node it already visited at the same timestamp — and reports the loop
  members **with the values going round**, which the static answer cannot.
- **Glitch attribution.** P4's glitch detector is priced at ~1.5 weeks and called
  "the highest teaching-value-per-week item in six sweeps" (`README.md:471-477`).
  It reports *that* a transient occurred. The journal reports *why*: which two
  arrival times raced, and through which arcs. Detector plus journal is a complete
  hazard lesson; detector alone is a red mark on a waveform.
- **Assertion failures gain a cause, for free.** P5's `Assert` element
  (`README.md:504-507`) fires and reports "property X failed at t=41200." With the
  journal it reports the cone that made the check signal true. **This is the single
  largest multiplier on P5's teaching value and it costs P5 nothing** — the two
  programs are already parallel-safe.
- **Deterministic replay becomes a testable property of the kernel**, not a claim in
  a document. `docs/simulation-semantics.md` §3 asserts determinism; nothing tests
  it end to end. `replay(ckpt[i]) == ckpt[i+1]` byte-for-byte does.

**Teaching capabilities. What a student can do afterwards that they cannot today.**

*Today*, a first-year whose 4-bit ripple-carry adder outputs the wrong sum has three
tools, all of them forward tools applied to a backward question: a modal dialog per
element showing the value *now* (`ElementValueDisplays.java:29-33` →
`Element.java:831` → `InputPin.java:228-238`); a waveform that is wiped by the next
run (`InteractiveSimulator.java:612-619`); and a hand-drawn `Stop`/`Pause`
breakpoint (`Stop.java:147-161`). The realistic fourth option is bisection by
deletion.

*After*, they right-click the wrong bit on the schematic at the moment the waveform
shows it go wrong, choose "why?", and the cone lights up: the carry into bit 3 was 1
because bit 2's generate term was 1 because A2 is wired where they meant B2. **Ten
seconds instead of twenty minutes — and the method transfers**, because that is
literally what an RTL engineer does in Verdi.

- **X-source tracing becomes the answer to the question P1 creates.** P1-S2 makes X
  producible and thereby makes "where did this X come from?" the most common
  question in the tool. **Shipping P1-S2 without causal tracing is shipping the
  question without the answer.** That is the sharpest ordering argument in this
  document.
- **Backward stepping teaches causality directionally.** Forward stepping — which
  hneemann's Digital does well (verified, below) — teaches "what happens next." A
  student who is debugging needs "what happened before." Pressing *Step Back* and
  watching the wavefront retreat *into* its cause is a different and better lesson,
  and it is not available anywhere in the educational tier.
- **P4's setup/hold and clock-skew labs become self-explaining.** "Your register
  captured the old D" → step back one event → "because D changed 3 units *after* the
  clock edge." Today `Register.react` samples D at the edge with no regard for how
  recently D moved, and the student is told the rule rather than shown it.
- **Bus-conflict pedagogy completes.** P1-S2 turns a conflicted net red
  (`README.md:172-173`); the journal names **both** drivers and the moment each
  turned on. Today the tri-state arm knows both (`WireNet.java:463-471`) and emits a
  one-shot text warning (`:477-483`) that names neither.
- **Autograder feedback becomes specific.** `examples/autograde/autograde.py:150-152`
  reports "stdout is missing `Output Pin ar: 0xED ...`". With a headless `--why`,
  the grader attaches the causal tree for the failing output to the student's
  feedback — automatically, per student. **No autograder in this class does that.**
- **"Save your simulation state and hand it in."** A 2-3 KB checkpoint of a
  1,551-element CPU is small enough to attach to a bug report, a forum post, or an
  assignment submission. "Here is the exact moment my CPU goes wrong" becomes a
  file.

---

### Competitive position

**Commercial — the incumbents do have this, and it must be said plainly.**

- **Synopsys Verdi.** The closest prior art. The Temporal Flow View displays a logic
  cone with the signal at centre and its driver to the left, crossing clock
  boundaries, traversing flip-flops, and annotating each node with its value at the
  cursor time. "Active Trace" catches the real driver of a signal at a given time
  from simulation results, invocable from the waveform or the source window. A
  "Trace X Value" facility exists for X-valued signals. *Verified via secondary
  sources only* (ecrionix.org Verdi guide; Verdi Debug Workshop training material);
  Synopsys primary documentation is behind login, so treat the exact feature names
  as secondary-verified. Prerequisites: an FSDB database (proprietary, #70) written
  by an instrumented Synopsys simulation, plus the RTL source.
- **Siemens Questa Visualizer.** The Time Cone window performs causality analysis:
  trace an event — explicitly including an X — back to its source through multiple
  clock domains; an automatic fan-in display traces a signal back to the next
  primary input or flip-flop; checkpoint & restore exists so a run need not repeat
  its reset phase. *Verified* via Siemens product pages and Verification Academy.
- **Cadence Indago.** Reverse debugging: single-step forward **or backward** in
  time, over a recorded database, with patented automated root-cause analysis.
  *Verified* via Cadence launch materials and the DVCon "Indago Debug Platform
  Overview" deck.

**So: causal tracing is not novel, and reverse stepping is not novel.** Three of the
four major commercial debug environments have both. Any claim of pure novelty here
would be false.

**Where the incumbents are genuinely weak — specific, not manufactured.**

1. **They explain RTL and synthesized netlists, not a drawing.** Verdi's cone is
   over HDL statements and net structures; Questa's fan-in is over the elaborated
   design. *The schematic the student drew does not exist in either tool.* JLS's
   model **is** the schematic (`docs/grand-architecture.md:43-45`: "That element
   graph is the asset everything else walks"), so the cone lands on the picture the
   student made, at the granularity they made it. No commercial tool can do this,
   because none owns a schematic-first model. This is the same argument P6 makes for
   layout cross-probing (`README.md:654-659`, *"E without D is a worse KLayout. E
   with D is a thing that does not exist"*) — and it is **stronger here, because it
   needs no cell library and no PDK**.
2. **All three are gated behind a licence, a proprietary database, and a workflow.**
   Verdi needs FSDB; Indago needs the Cadence stack; Visualizer needs Questa. None
   is reachable by a first-year student, and none runs from a grading shell script.
3. **The debug surface is a GUI sold as a GUI.** I could not find a documented
   *batch* causal-explanation artifact for any of the three — a command that prints
   "why is signal S equal to V at time T" to stdout, deterministically, for
   diffing or grading. *Unverified as an absence*: all three ship Tcl, so a
   determined user could script something; the claim is that none **documents** such
   an artifact, not that none is achievable.

**Open tier — structurally incapable, and this is a format fact.** GTKWave and
Surfer read waveform dumps. Surfer's README (*verified by fetch*) lists VCD, FST,
GHW and FTR, and its feature set is visualization and bit-vector translation with no
driver tracing or netlist-aware analysis. That is not a quality judgement: **VCD and
FST declare scopes, variables and value changes and carry no connectivity
whatsoever**, so a waveform viewer *cannot* trace a driver regardless of how good it
is. Yosys is a synthesis tool with no simulation-time debug surface. Verilator emits
traces; nothing in its runtime documentation describes reverse debugging
(*unverified as an absence*).

**Peer educational tier — nothing walks backward.**

- **hneemann's Digital.** The best of the class. Its "single gate mode" propagates a
  signal change **gate by gate**, highlighting after each step every gate with a
  changed input, expressly so a user can follow a change through the circuit and
  find the root cause of an oscillation — a feature built because "with Logisim it
  is hard to find the root cause for oscillating circuits." *Verified* from the
  project README. Its documentation **does not mention backward time-stepping or
  automated signal-cause tracing** (*verified as absent from the README; not
  verified against the full user manual*). **Digital steps forward well; it does not
  go back, and it does not compute a cone.**
- **DigitalJS.** Triggers stop the simulation on an edge or on a value appearing on
  a multi-bit signal, and clock triggers allow fast-forward to the next clock edge.
  *Verified* from its README. Forward tools; a nicer version of JLS's `Stop`
  element.
- **Logisim-evolution.** No causal-tracing or reverse-stepping feature found
  (*unverified as an absence* — search evidence for absence is weak).

**What JLS's version would be, and the verdict.**

**Parity on the idea; leapfrog on three specific axes.**

1. **Schematic-native causality.** The cone is drawn on the artefact the student
   authored, element by element and wire by wire. Structurally unavailable to every
   tool that has this feature today. **This is the leapfrog.**
2. **A headless, deterministic causal artefact.** `jls -b --why 'net@time'` printing
   a stable tree to stdout, diffable and gradeable, under `docs/batch-interface.md`'s
   existing stability contract. This is the axis where JLS could be *unambiguously
   ahead* rather than merely different — and once the journal exists it is a printer.
3. **Free, offline, one jar, aimed at the population that most needs to learn the
   skill.** The students who most need backward tracing are exactly the students
   with no access to the tools that have it.

**Where JLS cannot plausibly lead — state it.** **Scale.** Verdi and Visualizer
trace across million-instance designs with clock-domain awareness over multi-hour
databases, with years of investment in cone pruning and database indexing. JLS's
journal is designed for a 1,551-element circuit and a few million events. Do not
claim otherwise, and do not build for a scale that will never arrive. Likewise
**testbench-level causality** (UVM transactions, class objects, sequence items) is
Indago's and Visualizer's home territory and is correctly out of scope, exactly as
`README.md:1052-1061` declines the testbench-language tier.

---

### Relationship to the existing programs

**This is a new program — call it P7.** It is not a subset of P1-P6 and the roadmap
has no slot for it, because the standards frame that generated P1-P6 is blind to it:
**nothing in the 304 entries specifies debug provenance.** Its dependencies on the
existing programs are real but narrow, and its *contributions* to them are larger
than its dependencies.

**Hard dependency: P1-S0, kernel hygiene.** `SimEvent.sequence` is a mutable
`static long` (`src/jls/sim/SimEvent.java:87,119`). A checkpoint that restores
`now`, the queue and every value but not a **per-`Simulator`** sequence counter
cannot guarantee deterministic replay across a restore, because same-time tie-breaks
(`SimEvent.compareTo:143-146`) would resolve differently. P1-S0 already replaces
that static for its own reasons (`README.md:159-160`, Stage 0 in §4, 2-3 weeks,
ships alone). **Do P1-S0 first.** It is on the critical path of exactly one thing in
the roadmap, and this is it.

**Soft dependency: P1-S1 for cost, P1-S2 for the payload.** The journal works on
two-state values, and "why is this 0" is worth building alone. But:
- Under `BitSet` every journal record costs a defensive clone, on the very
  allocation path P1 exists to remove (`keystone-c-performance.md:275`: *n+1*
  allocations per change become **zero**). Under P1-S1's `Word(int,long,long,long)`
  the value is inline and the record allocates nothing. **Land the journal after
  P1-S1 or it will be measured as a slowdown and disabled.**
- X-source tracing — the version that matches Verdi and Questa — needs P1-S2.
  **Recommendation: build the journal in parallel with P1-S1 and ship the X-tracing
  view in the same release as P1-S2**, so the question and its answer arrive
  together.
- Per-bit tracing ("why is bit 3 of this bus wrong") needs P1's per-bit value
  domain to be honest. Today `Splitter`/`Binder` carry all-or-nothing HiZ special
  cases that per-bit Z deletes (`README.md:738-740`).

**Contributes to P4 and P5 at no cost to either.** P4's glitch detector and P5's
`Assert` element both produce "something happened at time T on signal S"; the
journal answers "why" for both with **zero change to either program**. Both are
already parallel-safe (`README.md:858`, `:479-483`), so P7 composes with them
without touching their schedules.

**Constrains the compiled-evaluation standalone item** (`README.md:700`). The
journal's site index and the levelization's slot table are the same table. **Add an
ordering constraint to that row: the site index is designed first, and the compiled
pass adopts it.** Cheap in that order; a permanent duplicate index otherwise.

**Documents it amends.**
- `docs/simulation-semantics.md` gains a §13 *"Causality and replay"*: what the
  journal records, at what granularity per engine, and the normative guarantee that
  replay from a checkpoint is bit-identical. Under `ARCHITECTURE.md` #221's own
  process clause — *"Any divergence is a specified, documented change to
  `docs/simulation-semantics.md` first"* — this must land before the code.
- `docs/batch-interface.md` gains `--why`. **It must ship through P5's report
  channel, not beside it**: P5's report-channel-and-exit-status work is priced at
  1 week and flagged *"This must be designed first, because it is a change to a
  promise"* (`README.md:537-541`). A second, parallel diagnostic channel would be
  the exact mistake that document exists to prevent.
- `docs/extension-points.md` gains one row. **Recommendation: a separate
  `sim.journal-consumer` seam, not a fold into P5's `sim.coverage-collector`** — a
  coverage collector aggregates and a journal consumer indexes; different lifetimes,
  different memory policies. Per the catalog's own rule (`README.md:966-970`), the
  row and its owning issue are written **before** any code.

**Hot-plane compliance.** `docs/grand-architecture.md:325-330` requires the event
loop to run with zero plugin indirection and results to reach watchers "batched,
rate-limited, never per-signal." The journal is a **concrete core type** in
`jls.sim` that the extension point *registers*; it never dispatches inside the loop.
Its write is gated by a single early return, exactly as `BatchSimulator.afterEvent`
opens (`:144-145`). Checkpointing happens *between* events. `runEventLoop`
(`Simulator.java:215-243`) is not edited. Note also the standalone hygiene item at
`README.md:699`: `InteractiveSimulator.afterEvent:891-894` already violates the
batching rule with a per-event loop over every probe and a `BitSet` clone each — P7
should not add a second offender, and would benefit from that item shipping first.

**Ordering summary.**

```
P1-S0 kernel hygiene ──── HARD PREREQUISITE (per-Simulator sequence counter)
   │
   ├─ P7-A  journal core + backward walk + headless --why   ── after P1-S1 for cost
   │     │
   │     ├─ P7-B  schematic cone overlay
   │     │
   │     └─ P7-D  X-source tracing ── SHIP WITH P1-S2, not after
   │
   └─ P7-C  checkpoint / restore / step-back ── share the element pass with P1
                                                 compiled-eval item adopts the site index
```

---

### Size and risk

| Slice | Weeks | Reasoning |
|---|---:|---|
| Journal core: site index, `NetChange`, two write points, over-approximating `causalInputs()` | 4-6 | Few write points (`WireNet.propagate:454-510`, around `Simulator.java:239`). The cost is the dense site index and the four refined elements (`Mux`, `TruthTable`, `Register`, `Memory`) |
| Backward walk + causality tree + headless `--why` | 2-3 | `Trace.firstChangeAtOrBefore:445-458` is the search, already written and tested; the tree printer is a printer |
| Schematic cone overlay + trace-window cross-probe | 3-4 | GUI. `Trace` already converts an x-coordinate to a simulation time (`:403,496`); the overlay channel is new but small |
| Checkpoint format, capture/restore, regenerable-queue reconstruction | 5-7 | The long pole: per-element state capture across 28 `initSim` and 25 `react` implementations. `Memory.WordStore.copy()` (`:1145-1148`) already exists; most elements are one or two `BitSet`s |
| Interval policy, retention ladder, replay-equivalence property test | 2-3 | The property test *is* the correctness argument (risk 3) |
| Backward stepping in the GUI (`Simulator.runUntil`, Step Back, Run Back To) | 2-3 | Generalizes `InteractiveSimulator:774-805` down into `Simulator` |
| `simulation-semantics.md` §13, `batch-interface.md` `--why`, extension-point row | 1 | A change to a promise; it is a week and it is not optional |
| **Total** | **19-27** | **4.5-6.5 maintainer-months** |

Comparable to **P5** (19-28 wk), and the same *kind* of program: mostly new surface,
very few edits to existing `react` bodies.

**The useful floor is 6-8 weeks**: journal core with the over-approximating default,
the headless `--why` tree, and the schematic cone — **no checkpoints**, journal held
in a bounded ring buffer over the last N net changes. For a classroom circuit the
entire run fits in the buffer, so the floor delivers the whole teaching capability
and defers every hard part. **Ship the floor first**, exactly as P5 ships its 5-7
week floor.

**The top three ways it goes wrong.**

1. **The cone is too big to read.** A backward closure from a 32-bit datapath net in
   the `riscv/` CPU reaches most of 225 logic elements within four hops. If the
   first thing a student sees is "everything is highlighted," the feature is worse
   than nothing — it is a picture of the whole circuit. **All four mitigations are
   needed, not one**: rank-limited expansion (2 hops, expand on demand); **bit
   slicing** — trace one bit of a bus, which needs P1's per-bit values to be honest;
   **stop at sequential boundaries by default** with an explicit "cross the flop"
   gesture, which is what Questa's fan-in-to-next-flip-flop does and is the right
   default; and under P1, prune to X-carrying inputs only. **Honest kill criterion:
   if the cone cannot be made legible on `riscv/build/k2000.jls`, the overlay is a
   demo and should not ship — ship the tree instead.**

2. **The journal costs too much when it is on.** A record per net change with a
   `BitSet` clone reintroduces exactly the allocation P1 exists to delete
   (`keystone-c-performance.md:275`). Two mitigations, both mandatory: **off by
   default behind a single early return** (`BatchSimulator.afterEvent:144-145` is
   the pattern and `grand-architecture.md:328-330` is the rule); and **land it after
   P1-S1** so the value is inline in a `Word` and the record allocates nothing.
   *A journal built on `BitSet` will be benchmarked, reported as a 2× slowdown, and
   switched off forever.*

3. **Checkpoint/restore silently diverges.** One uncaptured mutable field — a
   `toBeValue` in one of the seven classes, `WireNet.conflictReported` (`:407`), one
   `Memory` word outside `WordStore.present` — produces a replay that is *almost*
   right. That is worse than one that is obviously wrong, because the student then
   debugs a phantom and loses trust in the tool. **The only mitigation that works is
   to make divergence a test.** `replay(ckpt[i])` must produce a **byte-identical**
   `ckpt[i+1]`, asserted on every golden circuit under `test/jls/` and on
   `riscv/build/k2000.jls` in CI. `CircuitSnapshot.sameAs` (`:118-121`) is the
   precedent — byte-identity as the oracle — and the checkpoint format's sorted
   layout exists to make it possible. **Do not ship capture without that property
   test; it is not a nice-to-have, it is the feature's correctness argument.**

**What would make it not worth doing.**

- **If the cone cannot be made legible on a real design.** Then drop the overlay and
  ship the causality tree plus headless `--why`. That is most of the value at a
  third of the risk, and it keeps the leapfrog axis (the batch artefact) intact.
- **If P1 is abandoned.** Without X, this answers "why is this 0" — genuinely useful
  (a floating input reads 0 today, so it is the most-asked question) but a smaller
  lesson, and the competitive claim weakens from *"we do what Verdi does, on a
  schematic, for free, from a shell script"* to *"we have a nice tracer."*
- **If the checkpoint enumeration cannot share P1's element pass.** Both walk the
  same 25 `react` / 28 `initSim` implementations. Done together, it is one visit per
  class and the checkpoint slice drops toward its 5-week floor; done separately it
  is two audits six months apart, which is precisely the mistake keystone C warns
  about for the value domain and the compiled pass
  (`README.md:770-774`). **Recommendation: schedule P7-C inside P1's element pass.**
- **What would *not* justify declining it: "no standard requires it."** Nothing in
  the 304 entries does. That is a property of the survey, not of the capability.

---

### Sources

**Repository (all at HEAD, all verified by reading).**

*The absence of provenance*
- `src/jls/elem/WireNet.java:454-485` — tri-state resolution computes the winning driver (`:463-471`) and discards its identity (`:484`); `:477-483` one-shot conflict warning naming neither driver
- `src/jls/elem/WireNet.java:488-510` — sink distribution; `:502` `inp.setValue`; `:507-508` bare `PinChanged` post
- `src/jls/elem/WireNet.java:405,407` — net value and `conflictReported`
- `src/jls/elem/Input.java:59-62,72-75` — `setValue`/`getValue`, no time, no source
- `src/jls/elem/Put.java:385` — `protected @Nullable BitSet currentValue`
- `src/jls/elem/Output.java:136-169` — `propagate`; `:139-145` source-side change detection
- `src/jls/sim/SimEvent.java:23-84` sealed `Payload`; `:30-31` zero-field `PinChanged`; `:87,119` mutable `static long sequence`; `:96-102` fields; `:133-150` `compareTo`; `:162-172` `equals`
- `src/jls/sim/Simulator.java:25,27` queue and `dupCheck`; `:36,38,44` `now`/`maxTime`/`stopping`; `:177-201` `initSimulation` (`:180` `now = 0`); `:215-243` `runEventLoop` (`:224-225` poll, `:239` react, `:241` `afterEvent`); `:269-270` `afterEvent` hook; `:285-287` `probeSample` hook
- `src/jls/elem/Gate.java:653` `toBeValue`; `:694-719` `react` (`:700` compute, `:704` change-check, `:707` self-post with delay)

*Trace machinery — one field short*
- `src/jls/sim/TraceSample.java:6-19` — `record TraceSample(long time, BitSet value)`
- `src/jls/edit/Trace.java:51` `record Change(BitSet value, long when)`; `:32` `MAX_RETAINED_CHANGES`; `:180-199` `addValue`; `:214-218` `commit`; `:403-429` slider readout; `:445-458` `firstChangeAtOrBefore` (the search the backward walk reuses); `:496` x→time conversion
- `src/jls/sim/BatchSimulator.java:24-25` `eventTrace`; `:140-180` `afterEvent` (`:144-145` the single early return; `:148` callback cast; `:173-178` value dedup); `:294-312` `probeSample`
- `src/jls/edit/InteractiveSimulator.java:603` `initSimulation`; `:612-623` `traces.clear()` etc. on every run; `:636-638` editor disabled for the run; `:774-805` `stepEnd` forward-only stepping; `:879-896` `afterEvent` (`:891-894` per-event probe loop with a clone per probe)

*Workarounds*
- `src/jls/edit/ElementValueDisplays.java:29-33`; `src/jls/elem/Element.java:831`; `src/jls/elem/InputPin.java:228-238` — the modal one-value dialog
- `src/jls/elem/Stop.java:147-161`; `src/jls/elem/Pause.java:164-178` — hand-built breakpoints
- `riscv/verify.py:66-76` — final-state diff with no path to the divergent cycle
- `examples/autograde/autograde.py:53-57,150-152` — hard-coded expected stdout lines
- `docs/capability-roadmap/sweep-01-values-and-logic.md:117-119` — "they learn to trace it back", with no tool that does it

*Machinery the design reuses*
- `src/jls/edit/CircuitSnapshot.java:60-68` capture, `:85-105` restore, `:118-121` `sameAs` byte-equality, `:142-158` deflate — the technique, not the content
- `src/jls/elem/Memory.java:981` running contents; `:1145-1148` `WordStore.copy()`; `:1155-1200` `SparseWordStore`
- `src/jls/elem/SigSim.java:40-…` `initSim` posts all stimulus up front; `:123` initial value; `:194-196` `for`/`until` events
- `src/jls/elem/Clock.java:384-394` `initSim`; `:415-424` self-repost of exactly one event
- `src/jls/elem/Element.java:24` `stableId`; `:41-42` the single `highlight` boolean; `:571-575` `setHighlight`; `:586-589` `isHighlighted`
- `src/jls/edit/ElementRenderSupport.java:31-37` `drawHighlight`
- `src/jls/collab/op/CircuitOp.java:34-37` the eleven op kinds; `:51` `apply`; `:64` `invert`
- `src/jls/collab/op/OpSink.java:24` `submit`
- `src/jls/Circuit.java:479-485` `getElementsInStableOrder`
- `riscv/build_cpu.py:239-252` the 62-element write-enable mesh; `:349,398` `imem`/`dmem` at 256 × 32 bits

*Measurements quoted*
- `docs/capability-roadmap/keystone-c-performance.md:104-108` census (1551 elements, 297 nets, 225 logic, maxBits 32)
- `:126-131` event census: fired 2,331,793; `PinChanged` 1,919,891 (82.3%); `NewValue` 378,129 (16.2%); max queue depth 12,093
- `:137-139` 318 ns/event, 124 µs/cycle, ~8,090 cycles/s
- `:275` *n+1* allocations per change become zero under the plane encoding

*Roadmap and architecture*
- `docs/capability-roadmap/README.md:159-160` per-`Simulator` sequence counter (P1-S0); `:172-173` X on a conflicted bus; `:262-265` the 62-of-228 register-file mesh; `:398-399` `Simulator.cancel` does not exist; `:416` combinational-loop detection absent; `:471-477` glitch detector sizing; `:479-483` P4 parallelism; `:504-507` the `Assert` element; `:537-541` the report channel "must be designed first"; `:654-659` P6's cross-probing argument; `:699` `InteractiveSimulator.afterEvent` hygiene item; `:700` the compiled-evaluation item; `:738-748` the `zeroFill()` mechanic; `:770-774` on building two things against two layouts six months apart; `:858` P5 fully parallel; `:966-970` name pending seams before writing code; `:1052-1061` testbench-language tier declined; `:1116-1118` FSDB/WLF/UCDB declined as unimplementable
- `docs/grand-architecture.md:43-45` the element graph as the asset; `:325-330` the hot-plane rule; `:330-343` the levelized compiled pass as a core-internal second strategy
- `docs/simulation-semantics.md` §3 (determinism as "a pure function of circuit content"), §4 (the three termination reasons)
- `docs/batch-interface.md` §1 (flags, streams, exit codes; the stability promise)
- `ARCHITECTURE.md` recorded decision #221 and its process clause on changing `simulation-semantics.md` first

**External claims.**

| Claim | Status |
|---|---|
| Verdi Temporal Flow View shows a driver-side logic cone annotated with cursor-time values, crossing clock boundaries and flip-flops | **Verified via secondary sources** (ecrionix.org Verdi guide; Verdi Debug Workshop material). Synopsys primary docs are behind login |
| Verdi "Active Trace" catches the real driver of a signal at a given time from simulation results | **Verified via secondary sources**, same caveat |
| Verdi has a "Trace X Value" facility for X-valued signals | **Verified via secondary sources**, same caveat |
| Verdi requires FSDB, a proprietary binary format | **Verified** (consistent with `README.md:1116-1118`'s own finding) |
| Questa Visualizer's Time Cone traces an event, explicitly including an X, back to its source across clock domains; automatic fan-in traces to the next primary input or flip-flop; checkpoint & restore exists | **Verified** (Siemens product pages, Verification Academy) |
| Cadence Indago supports single-stepping forward **or backward** in time with automated root-cause analysis | **Verified** (Cadence launch materials, DVCon Indago overview deck) |
| No commercial tool documents a *batch* causal-explanation artifact (stdout, deterministic, diffable) | **UNVERIFIED as an absence.** All three ship Tcl and could be scripted; the claim is about documented artifacts only |
| hneemann's Digital has "single gate mode": forward gate-by-gate propagation, highlighting gates with changed inputs, built to find oscillation root causes | **Verified** (project README, fetched) |
| Digital has no backward stepping and no automated cause tracing | **Verified as absent from the README**; not verified against the full user manual |
| DigitalJS has value/edge triggers and fast-forward-to-next-clock-edge | **Verified** (project README) |
| Logisim-evolution has no causal tracing or reverse stepping | **UNVERIFIED** — search evidence for absence is weak |
| Surfer reads VCD, FST, GHW, FTR and does no driver tracing or netlist-aware analysis | **Verified** (README fetched) |
| VCD/FST carry no driver connectivity, so a waveform viewer structurally cannot trace a driver | **Verified** by the format definitions (IEEE 1364 §18 declares `$scope`/`$var`/value changes only); consistent with JLS's own writer at `BatchSimulator.toVcd:384-476` |
| Verilator has no reverse-debug facility | **UNVERIFIED as an absence** — its runtime documentation does not describe one |
