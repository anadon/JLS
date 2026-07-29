## Value domain: kernel and performance

*Keystone sweep C. Everything below that is a number was **measured this session**
on this tree at HEAD, with the jar built by `mvn -q package -DskipTests`
(`target/jls-5.0.5-SNAPSHOT.jar`) and the workload built by `riscv/make_cpu.py` /
`riscv/build_cpu.py`. Harnesses, raw profiles and reproduction commands are listed in
§12. Claims about the tree carry real paths and line numbers. Claims I could not
measure are marked **unmeasured** and say what would settle them.*

---

### 0. Verdict, stated first

**The performance objection to a richer value domain is not merely acceptable — it is
backwards. Measured, the multi-value representation makes JLS's inner loop *faster*,
and it is a *precondition* for most of the speedup the levelized escape hatch could
ever deliver.**

Three findings carry that verdict, and each is a measurement, not an argument.

1. **The value type is already the single largest cost in the event loop, and it is
   costly for reasons the new type removes.** 37.6% of in-loop profile samples land in
   `java.util.BitSet` work — `clone`, `and`, `equals`, `hashCode`, `trimToSize`,
   `recalculateWordsInUse`, `ArraysSupport.mismatch`. Almost none of that is logic. It
   is the overhead of a *mutable, unbounded, width-unaware* container being defensively
   copied at every hand-off. The element `react` bodies' own code — the actual digital
   logic — is **4.9%**.

2. **An immutable, width-carrying, plane-encoded value is about 2× cheaper per
   operation than today's `BitSet`, with four states plus a dormant `U` plane already
   included.** At the widths that actually occur in this tree (every width in every
   `.jls` file in the repository is ≤ 32; the RV32I CPU is 128 nets at 32 bits and 49
   at 1 bit) the measured cost is **10.85 ns/op vs 21.11 ns/op**. The `U` plane costs
   1.7 ns of that. The saving comes from deleting the defensive clone, which the
   immutability makes unnecessary — not from cleverness.

3. **The representation that the levelized compiled pass needs is the same
   representation.** A levelized pass over the RV32I CPU's 522 evaluation slots costs
   **2.26 µs (4.32 ns/node)** with plane arrays and **11.49 µs (22.0 ns/node)** with an
   array of `BitSet`s. Levelizing *without* changing the value domain throws away 80%
   of the available win. The two changes are not sequential; they are the same change
   applied at two scopes (per-value object, per-netlist array).

**The keystone claim is confirmed from this direction**, with one correction to how it
is usually stated. The keystone is not "replace `BitSet`". It is **"give a signal a
width, an immutable identity, and a plane encoding"** — three properties that happen to
arrive together, of which only one (the extra planes) is about multi-value logic. The
multi-value alphabet is nearly free once the other two land; it is the *reason* to do
the work, not the *cost* of it.

**The rival keystone I have to name honestly:** on the measured profile, the value type
is 37.6% of the loop and the **event-queue machinery is 47.7%** —
`PriorityQueue` 22.3% plus the `dupCheck` `HashSet` 25.4%. If the question were purely
"what is the cheapest way to make JLS faster", the answer would be the queue, not the
values. It is not the keystone by the capability frame's criteria — no standard is
blocked on it, no pedagogy depends on it, and it unlocks nothing — but it must be
scheduled alongside, because a value-domain migration that leaves the queue untouched
will show up as "we did all that work and got 18%". §7 sizes both together.

---

### 1. What the inner loop actually is

`src/jls/sim/Simulator.java:215-240` is the whole loop:

```java
while (!stopping && !eventQueue.isEmpty() && now <= maxTime) {
    if (!beforeEvent()) continue;
    SimEvent event = eventQueue.poll();       // :224
    dupCheck.remove(event);                   // :225
    now = event.getTime();
    if (now > maxTime) { now = maxTime; break; }
    beforeReact();
    event.getCallBack().react(now,this,event.getTodo());
    afterEvent(event);
}
```

Four data structures own the cost:

| structure | where | what it costs |
|---|---|---|
| `PriorityQueue<SimEvent> eventQueue` | `Simulator.java:25` | O(log n) sift on every poll and every add, comparing through `SimEvent.compareTo` (`SimEvent.java:134`) |
| `Set<SimEvent> dupCheck` | `Simulator.java:27` | a `HashSet` add on every `post` (`:167`) and a remove on every poll (`:225`); `SimEvent.hashCode` (`:186`) mixes `System.identityHashCode(callBack)` with `todo.hashCode()`, and for `NewValue` that reaches `BitSet.hashCode`, which walks the words |
| `@Nullable BitSet` signal values | `Put.currentValue`, `WireNet.value` (`WireNet.java:404`) | mutable ⇒ cloned at every hand-off; unbounded ⇒ width carried as a side channel; `null` ⇒ the whole channel is `@Nullable` |
| `LinkedHashSet<WireEnd> ends` / `LinkedHashSet<Wire> wires` | `WireNet.java:23,25` | re-walked with `isAttached()`, `getPut()` and an `instanceof` on **every** propagate (`:457`, `:488`, `:522`) |

The reacting elements sit behind `Reacts.react` (`src/jls/sim/Reacts.java`), a virtual
call into ~24 concrete `react` bodies. `docs/grand-architecture.md` §6's rule — no
plugin indirection, no capability lookup on the hot plane — is satisfied today and is
satisfied by every option in this document. The hot plane's problem is not
indirection. It is that the four structures above do far more work than the physics
they model.

---

### 2. The baseline: what a CPU-scale run costs today

**Workload.** The largest real design in the tree: `riscv/`'s single-cycle RV32I CPU,
built by `riscv/build_cpu.py` from a sum-1..N loop program, sized to **6004 clocked
cycles** (`riscv/build/k2000.jls`, 120 KB circuit, 193 KB `-t` vector). Census
(`jls.sim.Census`, §12):

```
elements(all, recursive)=1551   wireNets=297   maxBits=32   subcircuits=0
Constant 43, Mux 43, AndGate 34, Splitter 34, Register 32, Binder 9,
NotGate 8, Extend 5, XorGate 5, Adder 4, Memory 3, OrGate 3,
ShiftRegister 3, Decoder 1, InputPin 1     (+ 810 WireEnd, 513 Wire)
```

225 logic elements, 297 nets, flat, every width ≤ 32.
`riscv/verify.py` passes 11/11 on this jar — the differential oracle is intact before
any of the changes below are contemplated.

**Timing** (in-process, `jls.sim.KernelProbe2`, JIT-warm, best of 8 reps):

| vector | cycles | `initSimulation` | `runEventLoop` |
|---|---|---|---|
| `k500` | 1504 | 0.051 s | 0.170 s |
| `k1000` | 3004 | 0.155 s | 0.347 s |
| `k2000` | 6004 | 0.568 s | 0.742 s |

**Event census** (`jls.sim.KernelProbe`, 6004-cycle run):

```
fired 2,331,793   posted 2,596,496   dup-suppressed 264,702   max queue depth 12,093
payloads: PinChanged 1,919,891 (82.3%)   NewValue 378,129 (16.2%)
          MemoryRead 33,772 (1.4%)       MemoryWrite 1
callbacks: Mux 875,291  Register 428,298  Splitter 250,115  AndGate 207,456
           Adder 108,025  ShiftRegister 97,122  XorGate 82,011  NotGate 67,983
           Memory 67,545  Binder 57,026  OrGate 50,860  Extend 16,000
```

**The headline numbers:**

- **318 ns per event.**
- **124 µs per simulated clock cycle** (388 events/cycle).
- **≈ 8,090 simulated CPU cycles per second** in the warm event loop.
- **≈ 4,600 cycles/s** counting `initSimulation`.
- **≈ 1,100–1,450 cycles/s** end-to-end from the CLI (`java -jar … -b -t …`: 4.15 s at
  3004 cycles, 5.63 s at 6004 cycles, including JVM start and circuit load).

For context on `ARCHITECTURE.md:355-358`'s revisit trigger — "a concrete CPU-scale
design on the `riscv/` trajectory that is unusably slow interactively" — a student
running a 100,000-instruction program on the drawn CPU waits **~12 s headless**, and
the interactive engine is slower still (§6.4). A 1M-instruction program is ~2 minutes
headless. That is not "unusably slow" by a strict reading, but it is one order of
magnitude from it, and the recorded decision was taken without a number. **This
document supplies the number.** `ARCHITECTURE.md`'s trigger should be restated
quantitatively — e.g. "below 10 kcycles/s on the `#202` golden's CPU" — because "unusably
slow" is not a testable condition and nobody will ever agree it has been met.

**Two scaling facts, both important:**

- **The event loop is linear.** 2.04× then 2.14× per doubling of simulated time. The
  12,093-deep queue does *not* produce visible super-linearity at this scale, though it
  does cost a constant ~60 ns/event versus a shallow queue (§7.1).
- **`initSimulation` is quadratic.** 3.0× then 3.7× per doubling. The cause is exact
  and fixable: `src/jls/elem/SigSim.java:64,67,71,74` build the entire de-commented
  test-vector text by repeated `String +=`:

  ```java
  newLine += " " + token;                      // :67, per token
  newSignals += newLine + " ";                 // :74, per line
  ```

  On a 193 KB vector this is O(n²) byte copying; JFR attributes **259 of 1215 samples**
  to `SigSim.initSim` alone, and `byte[]` is the top allocation class of the entire run.
  At 6004 cycles the setup costs 0.57 s against 0.74 s of actual simulation. This is not
  a value-domain issue and should not wait for one — it is a `StringBuilder` and an
  afternoon. I flag it here because any benchmark of a value-domain change that measures
  end-to-end wall time will be dominated by it and will report noise.

---

### 3. Where the time goes: profile attribution

Java Flight Recorder, `settings=profile`, `-XX:FlightRecorderOptions=stackdepth=512`
(the default 64 truncates JLS's `react → propagate → post` recursion and mis-attributes
~30% of samples — worth knowing before anyone else profiles this), 12 reps, 973 samples
attributed inside the event loop:

| share | subsystem |
|---|---|
| **37.6%** | `BitSet` value work — `clone`, `and`, `or`, `set`, `equals`/`ArraysSupport.mismatch`, `hashCode`, `trimToSize`, `recalculateWordsInUse`, `BitSetUtils.ToInt/ToLong` |
| **17.1%** | `eventQueue.poll()` — `siftDownComparable` + `SimEvent.compareTo` |
| **15.3%** | `post()` → `dupCheck.add` — `HashSet.add` → `HashMap.putVal`, dragging `SimEvent.hashCode` → `BitSet.hashCode` |
| **10.1%** | `dupCheck.remove` at `Simulator.java:225` |
| **5.2%** | `eventQueue.add` — `siftUpComparable` |
| **4.9%** | element `react()` bodies' own code |
| 1.5% | loop scaffolding |

Grouped: **value representation 37.6%, event-queue bookkeeping 47.7%, actual logic
4.9%.**

**Read that again.** JLS spends roughly twenty times as long deciding *where an event
goes in a priority queue and whether it is a duplicate*, and copying `BitSet`s
defensively, as it spends computing what a gate outputs. `docs/grand-architecture.md`
§6 protects this loop from plugin indirection; the loop's real problem is that it is
95% bookkeeping already.

**Allocation** (JFR `ObjectAllocationSample`, same run): `byte[]` (the `SigSim`
concat), then `long[]` + `java.util.BitSet` (the value clones, 592 samples combined),
`jls.sim.SimEvent` (252), `HashMap$Node` (164), **`SimEvent$PinChanged` (87)**, and
**`LinkedHashMap$LinkedKeyIterator` (83)**. 82 young GCs across 12 reps.

Two of those are pure waste visible from the class name alone:

- **`SimEvent.PinChanged` is a zero-field record** (`SimEvent.java:38-39`) allocated
  fresh at `WireNet.java:507-508` for every sink of every propagate — 1.92 M
  allocations per run of an object with no state. It should be an interned constant.
  (Escape analysis eliminates some of them; the allocation profile shows it does not
  eliminate all.)
- **`LinkedKeyIterator`** is `WireNet.propagate` iterating its `LinkedHashSet`s — up to
  three times per propagate (`:457` tri-state resolution, `:488` sinks, `:522` probes),
  each allocating an iterator and each re-deriving structure that has not changed since
  the circuit was elaborated.

---

### 4. Where JLS is already papering over the gap — in the kernel

The frame asks where existing workarounds prove a change is overdue. Sweep 01 and
keystone A catalogue the value-model workarounds (`ImportSummary.coercedX`,
`TraceSample`'s marker bit, `VhdlEmitter`'s nine-value `when others`). The *kernel* has
its own set, and they are worse, because they are the ones that cost time on every
event.

**4.1 Arithmetic is bit-serial because the value has no width and no word view.**

`src/jls/BitSetUtils.java:196-226`:

```java
public static BitSet SumCarry(boolean carryIn, BitSet bs1, BitSet bs2) {
    BitSet sum = new BitSet();
    boolean carry = carryIn;
    int size = Math.max(bs1.size(),bs2.size());        // :200  -- size(), not width
    for (int index = 0; index < size; index += 1) { … } // 4-branch chain per bit
```

`BitSet.size()` is the *backing array capacity in bits*, not the signal's declared
width — the value does not know its width, so the adder cannot. For a 32-bit add on a
one-word `BitSet`, `size()` is **64**, so a 32-bit ripple add executes 64 iterations of
a four-branch chain and can set a carry bit at position 64.

Measured (`Kernels.java`, §12): **`SumCarry` + `ToLong` for one 32-bit add = 294.49
ns**. A `long` add plus a width mask = **0.60 ns**. That is ~490×, and it is a direct,
mechanical consequence of a value type that carries neither a width nor a word view.
`Adder.react` (`src/jls/elem/Adder.java:404`) fired 108,025 times in the benchmark run.

`BitSetUtils.ToInt` (`:127`) and `ToLong` (`:158`) are the same defect in miniature —
`for (int i=0; i<bs.length(); i+=1) if (bs.get(i)) value += pow;` — an O(width)
bit-serial loop over a container that is already `long[]`-backed. They are called from
the hottest places in the tree:

- `src/jls/elem/Register.java:757` — `int c = (int)(BitSetUtils.ToLong(inVal));` to read
  a **one-bit clock**. `Register.react` fired 428,298 times.
- `src/jls/elem/Mux.java:530` — `int which = BitSetUtils.ToInt(bw);` for the selector.
  `Mux.react` fired 875,291 times, the single most-fired element in the run, and
  `BitSetUtils.ToInt` shows up directly in the profile.

**A width-carrying value with a `long` payload makes all three of these one
instruction.** This is the clearest case in the whole capability program where the
model change and the performance fix are literally the same edit.

**4.2 The defensive-clone tax is a workaround for mutability.**

`WireNet.propagate` clones the value **once per sink** (`WireNet.java:496-498`) and once
more for the net's own copy (`:516`). `Output.propagate` clones (`Output.java:148-153`).
`Register.react` clones six or more times per event
(`Register.java:709,724,735,767,769,778,780,789,791,801,804,805`). `Adder.react` clones
three times (`:366,370,409,418`). None of these copies exist because the *semantics*
need a copy; they exist because `BitSet` is mutable and no caller can be trusted. An
immutable value makes every one of them a field assignment. A net with *n* sinks goes
from *n+1* allocations per change to **zero**.

**4.3 The net's structure is re-derived on every event.**

`WireNet.propagate:457-486` re-walks `ends`, calls `isAttached()` and `getPut()` and an
`instanceof Output` on each end, to find the active drivers — every single propagate, on
a structure that only changes when the user edits the circuit. `:488-509` re-walks the
same set for sinks. `:522-527` walks `wires` to look for probes — on the RV32I CPU there
are **zero** probes, and the scan runs anyway on every value change of every net.
`WireNet.makeNet` (`:97`) and `WireNet.recheck` (`:272`) already do this walk at
elaboration time to compute `bits`, `hasinput` and `triState`; caching `Output[] drivers`
and `Input[] sinks` there is a pure win with no semantic content. Keystone A §5.5(a)
proposes exactly this; the profile confirms it is not a micro-optimization but a
structural one, and it is **the change that makes real strength resolution cheaper than
today's first-driver scan**.

**4.4 The duplicate-suppression set costs more than the duplicates.**

This one I tested directly, because it is surprising enough to need an experiment rather
than an argument. `jls.sim.NoDedup` (§12) subclasses `BatchSimulator` and replaces
`post()` and `runEventLoop()` with dedup-free versions — `eventQueue.add(event)` with no
`dupCheck`, and a loop with no `dupCheck.remove`. Same circuit, same vector, alternating
A/B, 6 reps each:

```
dedup=true   loop 0.778 s   fired 2,331,793   final register state 9f07925e
dedup=false  loop 0.649 s   fired 2,596,499   final register state 9f07925e
dedup=true   loop 0.715 s   fired 2,331,793   final register state 9f07925e
dedup=false  loop 0.659 s   fired 2,596,499   final register state 9f07925e
```

**Identical final architectural state. 11% more events fired. 9–17% *less* time.** The
`HashSet` that exists to avoid firing 264,702 redundant events costs more to maintain
than those events cost to fire — because maintaining it drags `SimEvent.hashCode`
(`SimEvent.java:186-191`) and, through `NewValue`, `BitSet.hashCode` into the loop
twice per event.

Two honest caveats. (i) `dupCheck` is a **specified** behaviour with its own contract
test — `test/jls/sim/SimEventDedupTest.java`
(`duplicatePostingCoalescesToOneEvent`, `dequeueReleasesTheDedupEntry`,
`formerIdentityPayloadsNowCoalesceStructurally`) — and
`docs/simulation-semantics.md` §3 names the equal-pending-event rule as one of only two
glitch-suppression mechanisms JLS has. Removing it is a *semantics* change, not a
performance tweak, and would need `SimulationSemanticsRegressionTest` and every golden
to agree. (ii) One workload is not proof; a circuit with heavy reconvergent fanout (a
wide carry-select adder, a crossbar) could plausibly show the opposite. **Unmeasured:**
whether any circuit in `examples/` or `test/fixtures/` shows dedup paying for itself.

What the experiment *does* establish is that the coalescing rule should be
**reimplemented, not deleted**: an immutable value with a cached `hashCode` makes
`SimEvent.hashCode` a field read, and a per-element "pending event" slot (the
`toBeValue` pattern that `Gate.react` and `TriState.react` already use —
`docs/simulation-semantics.md` §6.2) can coalesce without a global hash set at all.
That is a value-domain-enabled fix to a queue-domain cost, and it is the second place
the two programs meet.

---

### 5. The representation bake-off

The frame names four candidates and pre-judges one. I benchmarked five, plus variants,
under the access pattern JLS's loop actually exhibits — combine two operands (AND),
compare the result against the previous value for change detection, hand a copy to each
of 3 sinks, read one bit out — which is precisely
`Gate.computeOutput` → `Output.propagate`'s change check → `WireNet.propagate`'s
per-sink clone → `Splitter.react`'s bit extraction.

`ValueRep.java` / `ValueRep2.java` / `WideSparse.java`, best of 7 after warm-up, ns per
simulated event-equivalent:

| width | R0 `BitSet` (today, 2-state) | R1 `BitSet` pair | R2 `byte[]` codes | R3 packed `long[]` | R4 3-plane record |
|---:|---:|---:|---:|---:|---:|
| 1 | 28.72 | 77.63 | 23.39 | 24.88 | **5.78** |
| 8 | 21.00 | 83.04 | 25.81 | 16.12 | **15.71** |
| 32 | 21.11 | 81.05 | 46.69 | **9.73** | 10.85 |
| 64 | 20.17 | 81.36 | 84.18 | 14.64 | **9.95** |
| 128 | 25.49 | 87.68 | 138.98 | **22.36** | 39.26 (3 arrays) / 30.56 (1 array) / 27.38 (sparse) |
| 256 | ~21 | — | — | — | 24.90 (sparse) |

Supplementary measurements:

- **`U`-plane cost.** 2-plane `Word` vs 3-plane `Word` at w=32: 9.14 vs 10.85 ns —
  **1.71 ns**, ≈0.5% of a 318 ns event. Keystone A's recommendation to ship the third
  plane dormant on day one so the migration happens once is confirmed as essentially
  free.
- **Today's clone tax.** `BitSet` with vs without the 3 fanout clones: 28.72 vs 17.85 ns
  at w=1, 21.11 vs 20.08 at w=32. The tax is largest at *narrow* widths — which is where
  most nets are (787 one-bit nets across the tree's `.jls` files, versus 2056 at 32 bits).

**Verdicts by representation.**

**R1, two parallel `BitSet`s — reject, and reject emphatically.** At **~81 ns/op it is
4× slower than today** at every width. This is the option sweep 01 recommends as the
smallest diff, and it is the worst measured option in the table. The reason is
mechanical: it inherits `BitSet`'s mutability, so the ~20 defensive-clone sites become
~40; it inherits `BitSet`'s unboundedness, so it needs `flip(0,width)` calls to
normalise above the width; and a 4-state AND needs five intermediate `BitSet`
allocations per operation where the plane encoding needs none. Keystone A rejects R1 on
design grounds (it fixes one of the four fused problems). The measurement says it should
also be rejected on performance grounds, and the performance grounds are more decisive:
**R1 is the one option that would make the frame's opponents right.** If a
value-domain program is attempted with R1 and someone benchmarks it, the program dies.

**R2, `byte[]` of per-bit state codes — reject for the kernel, adopt for
specification.** It is O(width) by construction: 46.7 ns at 32 bits, 84.2 at 64, 139.0
at 128 — 5.5× today's cost at 128 bits and getting worse. `docs/grand-architecture.md`
§6's fear of "putting the performance concern on the hot path" is exactly this
representation, and the fear is justified. It is, however, the right type for the
*specification* side — `TruthTable` cells (already `2` for don't-care at
`src/jls/elem/TruthTable.java:79`), `-t` vector expectations, synthesis don't-cares —
where widths are small, `-` must exist, and nothing is on a loop. Keystone A §5.4 gets
this exactly right and the measurement supports the split.

**The frame's guess that "object-per-value is presumably fatal" is refuted.** R4 *is*
object-per-value — a Java `record` with three `long` fields — and it is the fastest
option at every width ≤ 64, faster than the mutable `BitSet` it replaces. What is fatal
is not the object; it is (a) *mutability*, which forces a copy at every hand-off, and
(b) *per-bit* objects, which is R2 with boxing and would be catastrophic. An immutable,
fixed-size, width-carrying record is allocated once and shared by the net and all its
sinks — the allocation *count* per propagate goes from n+1 to 1, and to 0 whenever the
value is an interned constant (all-zero, all-one, all-Z, all-U per width). The JVM's
young generation is extremely good at this shape; `BitSet`'s two-objects-plus-`long[]`
shape with a `wordsInUse` invariant to maintain is not.

**R3, packed interleaved `long[]` — reject as the kernel type, keep as the *array*
layout.** It is genuinely fast (9.73 ns at w=32, the best measured at that width, and
the best at 128). But every operation begins with shift/mask de-interleaving, every
element author must understand the packing, and adding a third state plane means
re-packing from 2 bits to 4 bits per position — a second migration, which is the one
thing §8 must avoid. Where R3's virtue matters is the *levelized* layout (§6), where
signals live in flat arrays indexed by node id and no element author ever sees the
encoding. There, plane-major (`long[] a; long[] b; long[] u`) beats interleaved anyway,
because plane-major is what makes an operation a straight `long` op with no
de-interleaving at all.

**R4, the sealed width-split record — adopt.** `Word(int width, long a, long b, long u)`
for width ≤ 64, `Wide(int width, long[] a, long[] b, long[] u)` above. Measured 10.85
ns/op at w=32 versus 21.11 for today, with four states and a dormant `U` plane included.
This is keystone A §5.1's recommendation and I confirm it from the performance side
without qualification for width ≤ 64.

**One correction to keystone A, from measurement.** Its `Wide` — three separate
`long[]`s, always allocated — is the one place the design is slower than today: **39.26
ns at w=128 versus 25.49**, because a two-state wide value pays for three arrays it does
not use. Two fixes, both measured:

- one contiguous `long[]` of `3n` words instead of three arrays: **30.56 ns** (−22%);
- **`b` and `u` nullable, `null` meaning "all clear"**, so a two-state wide value is one
  `long[]` and its AND is one loop: **27.38 ns at w=128, 24.90 at w=256** — at parity
  with today's `BitSet`, and the 4-state path only pays when X or U is actually present.

Take the second. It costs one branch on the fast path and it removes the only width at
which the new type is slower than the old one. And note the context: **every width in
every `.jls` file in this repository is ≤ 32.** `Wide` is a correctness obligation, not
a performance one.

**Projected event-loop effect of the representation change alone.** Value work is 37.6%
of a 0.742 s loop = 0.279 s; halving it saves ~0.14 s ≈ **19% of the loop**. Removing
`BitSet.hashCode` from `dupCheck`'s critical path (a record can cache its hash) takes a
further bite out of the 25.4%. **The measured expectation is that the four-state
migration makes the event loop 20–25% faster, not slower.** That is a testable
prediction and Stage 0 (§8) is the experiment.

---

### 6. The escape hatch: levelized compiled evaluation

`docs/grand-architecture.md` §6 reserves it; `ARCHITECTURE.md:340-368` records the
decision *not* to build it, with the revisit trigger quoted in §2 and a binding
equivalence criterion: any future pass "must be observably identical to the event model
as specified in `docs/simulation-semantics.md`" — including "per-element propagation
delays (§6, §7)" — and "must agree bit-for-bit with the #202 RV32I integration golden".

The frame's question is whether a richer value domain makes the escape hatch *more*
necessary, and whether the two can be co-designed. My answers, in order: **no, less; and
yes, they must be.**

**6.1 A richer value domain does not make the compiled pass more necessary.**

Per-value, it makes the event loop faster (§5). The only mechanism by which four-state
logic increases cost is **more events**: a net that used to hold a stable 0 may now hold
X and toggle. In practice X is a transient — it appears at t=0 in an unreset design and
is flushed by the first reset or the first driven value — so the steady-state event
count should be unchanged. **Unmeasured**, and it is the one performance risk of the
whole program that I could not settle: I cannot count X-induced events without an
implementation. The gate is easy to state and belongs in the Stage 2 acceptance
criteria: *event count on the `#202` golden must not rise by more than 10% once the
design's reset has settled.*

**6.2 A richer value domain is a precondition for most of the compiled pass's win.**

This is the measurement that decides the co-design question. `Levelized.java` and
`LevelBitSet.java` (§12) model the same levelized evaluation over the RV32I CPU's real
shape — **522 evaluation slots** (225 logic elements + 297 nets), the measured width mix
(1-bit and 32-bit), a topologically ordered DAG, and a realistic opcode mix
(AND/OR/XOR/NOT/MUX/ADD/BUF/REG) implemented with **full 3-plane 4-state arithmetic**,
including the X-poison rule for `ADD` (`Long.numberOfTrailingZeros(b|u)` and a mask):

```
full levelized pass over 522 nodes, plane arrays : 2.26 µs/pass  (4.32 ns/node)
full levelized pass over 522 nodes, BitSet[]     : 11.49 µs/pass (22.01 ns/node)
```

**5.1×.** Levelizing while keeping `BitSet` as the signal type throws away four fifths
of the win. And the reason is structural, not incidental: the whole point of a levelized
pass is that state lives in **flat arrays indexed by node id**, touched by straight-line
`long` operations, with no allocation and no pointer chasing per node. `BitSet` cannot
be that; `byte[]`-per-bit cannot be that at any useful width; a parallel-`BitSet` pair
is twice as far from it. `long[] a; long[] b; long[] u` **is** that, and it is the same
encoding as `Word`'s three fields — hoisted out of the per-value object into per-netlist
arrays.

With an activity bitmap so unchanged cones are skipped:

```
activity 100% (522 live) : 1.62 µs/pass
activity  50% (252 live) : 1.33 µs/pass
activity  25% (123 live) : 0.89 µs/pass
activity  10% ( 46 live) : 0.68 µs/pass
```

Against today's **124 µs per simulated clock cycle**, a levelized design needing two
passes per cycle (one per clock edge; a levelized DAG converges in one pass by
construction, so no fixpoint iteration) costs **4.5 µs/cycle — ~27×**. Halve it for
being a model rather than an implementation, halve it again for real cache behaviour on
a design ten times larger, and it is still **~7×**. With `BitSet` state it would be
~1.4×, i.e. not worth building.

**So: the two changes are one change at two scopes, and doing them sequentially wastes
the design.** Concretely, the shared artifact is the encoding contract:

| scope | shape | who touches it |
|---|---|---|
| per-value (event-driven engine) | `record Word(int width, long a, long b, long u)` | ~24 `react` bodies, element authors |
| per-netlist (levelized engine) | `long[] a; long[] b; long[] u` indexed by node id | the compiler and the eval loop only |

`Word.and(x,y)` and the levelized `AND` case are the *same six `long` operations* over
the same plane semantics. Write the truth tables once, in one place, and test them once
— the IEEE 1164 table oracle keystone A §5.5 proposes then validates both engines from
one test. Write them twice, six months apart, against two different data layouts, and
the two engines will disagree, and the `#202` differential oracle will catch it late and
expensively.

**6.3 The honest obstacle: JLS's delay model, and the better first step.**

The equivalence criterion in `ARCHITECTURE.md:359-368` is not decorative. JLS has real
**per-element transport delay** (`docs/simulation-semantics.md` §6.2, §7): gates 10 or 5,
Mux 25, Register 50, Memory 100, Adder **30 × bits** (960 for a 32-bit adder), and
transport rather than inertial semantics, so a narrow pulse is *not* swallowed. Traces,
VCD (`BatchSimulator.toVcd`), the trace window and every golden depend on exact
timestamps. A Verilator-style cycle-based pass **cannot** reproduce that: it is a
different timing model. Any full cycle-based mode is therefore a *declared alternative
strategy* with its own paragraph in `docs/simulation-semantics.md`, whose oracle is
"same settled values at every clock edge", not "same VCD" — and `ARCHITECTURE.md`'s
"observably identical … including per-element propagation delays" would have to be
amended first, deliberately, in the open.

But there is a strictly better first step hiding in the event census, and it needs no
amendment at all.

**82.3% of all events carry no time.** `PinChanged` (1,919,891 of 2,331,793) is the
same-timestamp notification a net sends its sinks; only `NewValue` + `MemoryRead` +
`MemoryWrite` (411,902, **17.7%**) actually advance the clock. `docs/simulation-semantics.md`
§6.2 states the rule that creates them: "Zero-delay elements (`Splitter.react`,
`Binder.react`, `InputPin.react`, `OutputPin.react`, `SubCircuit.react`,
`Constant.react`) propagate within the same timestamp, so an arbitrarily deep chain of
wiring elements adds zero time." Every link in every such chain is a priority-queue
insert, a hash-set insert, a poll, a hash-set remove, and a `BitSet` clone — to model
*zero elapsed time*.

**Levelize the zero-delay plane and leave the timed plane on the queue.** Compute, at
elaboration, the topological order of the zero-delay subgraph (splitters, binders, pins,
constants, subcircuit boundaries, wire nets). When a timed event lands, evaluate that
element's zero-delay closure in compiled order in one straight-line pass, then post only
the genuinely delayed successors. This:

- **preserves the delay model exactly** — transport delays remain in the queue, so §6.2,
  §7, §8 and every golden and every VCD byte are untouched, and `ARCHITECTURE.md`'s
  equivalence criterion is satisfied *as written*, with no amendment;
- **removes ~82% of queue traffic** and drops max queue depth from 12,093 to roughly the
  timed-event working set, which is worth a further constant (§7.1);
- **is the same compiler and the same plane arrays** the full cycle-based mode would
  need — it is Stage 1 of that work, not a detour from it;
- **is where the value domain pays twice**: the zero-delay elements are exactly the ones
  keystone A §7 says get *simpler* under `LogicValue` (`Splitter.react`'s
  "input null ⇒ all outputs null" branch at `src/jls/elem/Splitter.java:218-224` and
  `Binder.react`'s `allOff` flag delete outright when Z slices per-bit).

Rough sizing, and it is a projection not a measurement: queue+dedup is 47.7% of the
loop; removing 82% of the postings and shrinking the heap should take out ~35–40% of
loop time on its own, on top of the ~20% from the representation. **Unmeasured:** the
cost of the closure walk itself, which I cannot model without knowing real zero-delay
chain depths.

**6.4 The interactive engine is a separate, worse problem.**

`docs/grand-architecture.md` §6 requires results to reach watchers "through a batched,
rate-limited channel, never per-signal". That is implemented for exactly one thing —
the clock label, at 50 ms (`src/jls/edit/InteractiveSimulator.java:851-871`). It is not
implemented for traces or probes. `InteractiveSimulator.afterEvent`
(`:879-896`) runs **per event**:

```java
Trace tr = traceMap.get(el);
if (tr != null) tr.addValue(el.getCurrentValue(), now);   // getCurrentValue() clones
for (Wire wire : wireMap.keySet()) {                      // O(probes) per event
    tr = wireMap.get(wire);
    tr.addValue(wire.getValue(), now);                    // getValue() clones
}
```

and `beforeEvent` (`:736`) calls `Editors.of(circuit())` — a map lookup — on every
event. So the interactive engine's per-event cost is O(number of probes) with a `BitSet`
clone each, on top of the 318 ns baseline. **Unmeasured** (no display available in this
environment) but structurally certain: the GUI figure is worse than 8 kcycles/s, and
gets worse the more the student probes — the exact opposite of what a teaching tool
wants. Batching this is a §6-conformance fix that is independent of the value domain and
should be scheduled on its own.

---

### 7. The queue, sized honestly

The value domain is the keystone by reach. The queue is 47.7% of the loop and unlocks
nothing. Both statements are true and the plan has to hold both.

**7.1 What the queue costs, isolated.** `Kernels.java`, a `PriorityQueue` of
`(time, seq, callback, payload)` records with poll-then-repost at steady depth:

| depth | poll + add | + `HashSet` dedup |
|---:|---:|---:|
| 64 | 82.32 ns | 116.64 ns |
| 512 | 116.89 ns | 173.75 ns |
| 4096 | 140.93 ns | 200.75 ns |
| 12000 | 141.91 ns | 212.30 ns |

Two readings. **The dedup set costs ~35–70 ns per event** across all depths — consistent
with the A/B experiment in §4.4. And **queue depth costs ~60 ns per event** between a
64-deep and a 12,000-deep heap. JLS's heap is 12,093 deep on this workload for a reason
that has nothing to do with the circuit: `src/jls/elem/SigSim.java:129,192` posts
**every** stimulus event at t=0, so the entire `-t` vector sits in the priority queue for
the whole run. Streaming the vector lazily — keep the parsed transitions in an array,
post only the next one per pin — would drop the heap to the active fanout and is a
contained change to one class.

**7.2 What to do about it, in dependency order.**

1. **Stream the test vector** (`SigSim`), and fix its quadratic string building while in
   there. Independent of everything else; removes the ~60 ns depth tax and the 0.57 s
   quadratic setup.
2. **Intern `SimEvent.PinChanged`.** One line; removes 1.9 M allocations per run.
3. **Cache `WireNet`'s driver and sink arrays** at elaboration (`makeNet`/`recheck`).
   Independent of the value domain, and it is also the change that makes strength
   resolution affordable later (§4.3).
4. **Replace the global `dupCheck` with per-element pending-event slots**, extending the
   `toBeValue` pattern that `Gate.react` and `TriState.react` already use. Needs
   `SimEventDedupTest` and `SimulationSemanticsRegressionTest` to be rewritten against
   the new mechanism, and needs `docs/simulation-semantics.md` §3 updated. **Sequence
   this after the value type lands**, because an immutable value with a cached hash makes
   the interim cost bearable and because the new coalescing rule wants to compare values,
   not events.
5. **Levelize the zero-delay plane** (§6.3). Wants (3) done and the value type landed.

One more kernel-hygiene item that is not a performance issue but will become one:
`SimEvent.sequence` (`src/jls/sim/SimEvent.java:87`) is a **mutable static** incremented
on every construction — 2.6 M writes to one static field per run, and a hard barrier to
any future parallel or re-entrant evaluation. Determinism does not require it to be
global; a per-`Simulator` counter would do. Fix it while the file is open.

---

### 8. Sequencing: how to land this without a benchmark ambush

Keystone A §8's Stage 0 — "the type lands; nothing observable changes", `LogicValue`
becomes the currency of the whole value channel, X and U never produced, all 29 coercion
sites become `.zeroFill()`, every golden byte-identical — is the right first move, and
from the performance side it is also the right *experiment*. Two amendments:

**8.1 Make Stage 0's acceptance criteria include a performance gate, with the number
already recorded.** The baseline is in this document: **0.742 s of `runEventLoop` for
2,331,793 events on `riscv/build/k2000.jls`, 318 ns/event, 8,090 cycles/s.** The gate
should be *"the event loop is no slower, and the expectation is 15–25% faster"*. Stating
it as an expected *improvement* rather than a tolerated regression is what makes the
migration defensible; and if Stage 0 comes in slower, that is a signal the
representation was implemented wrong (almost certainly: a clone that survived, or a
`Wide` on a path that should be a `Word`), not that the program is wrong.

Add a `test/jls/` harness that measures it. It does not need to be a JUnit assertion —
a timing assertion in CI is a flake factory — but the numbers should be *produced* by
the same command that produces the goldens, so that a regression is visible rather than
discovered.

**8.2 Do the two cheap independent fixes first, so the baseline is clean.** `SigSim`'s
quadratic setup (§2) and streaming (§7.2.1) currently make end-to-end wall time a
useless measure of anything. Land them before Stage 0 so that the value-domain change is
measured against a loop, not against a string concatenation.

**8.3 The stage that is missing from keystone A: the levelized zero-delay plane.** It
belongs between Stage 0 and the semantic stages, because it is the point at which the
plane encoding has to be lifted from per-value objects into per-netlist arrays, and
that is a design decision better made while the value type is still fresh than
retrofitted onto 24 migrated `react` bodies. Concretely, `LogicValue`'s API should be
specified so that a `Word` can be *materialised from* and *written back to* a
`(long[] a, long[] b, long[] u, int index)` triple without going through the object —
i.e. the ops should exist in two forms, one over values and one over plane slots, sharing
one set of truth-table expressions. That is a small constraint if it is stated up front
and an expensive rewrite if it is not.

Revised sequence, with the queue work interleaved:

| stage | content | perf expectation |
|---|---|---|
| **−1** | `SigSim` `StringBuilder` + lazy vector streaming; intern `PinChanged`; per-`Simulator` sequence counter | setup 0.57 s → ~0.01 s; loop −15% (depth) |
| **0** | `LogicValue` lands, X/U never produced, `.zeroFill()` everywhere, all goldens byte-identical | loop **−15…−25%** |
| **0.5** | cache `WireNet` driver/sink arrays; per-element pending-event coalescing replaces `dupCheck` | loop −10…−20% |
| **1** | levelized zero-delay closure, plane arrays, delay model untouched | loop −30…−40% |
| **2** | X actually produced; 1164 projection; strength on `Output` | event count +0…10% (gate) |
| **3** | `U`/reset semantics; optional full cycle-based mode as a *declared* strategy | n/a |

Compounding the middle of those ranges from the measured 8,090 cycles/s: roughly
**25–40 kcycles/s** on the RV32I CPU after stage 1, with four-state logic, before any
cycle-based mode exists at all. That is the number that decides whether
`ARCHITECTURE.md`'s revisit trigger ever fires.

---

### 9. What this makes possible pedagogically

The frame asks what a model limitation costs teaching. Three answers specific to the
kernel.

**9.1 The bus-conflict lie is a hot-loop artifact.** `WireNet.propagate:457-486` resolves
multi-driver conflicts by "first active driver in net order" plus a one-shot
`TellUser.warn` — a rule that is not commutative, not associative, and depends on the
order wire ends entered the net, which is why `docs/simulation-semantics.md` §9 needs a
breadth-first file-order walk to be deterministic at all. It is that way partly because a
real resolution fold looked expensive on the hot path. It is not: with cached driver
arrays (§4.3) and plane masks, the three-way fold (all-strong tri-state; single strong
driver; general strength) is *cheaper than the current scan*, because the current scan
re-derives the driver list from a `LinkedHashSet` with three virtual calls per end every
time. **The performance excuse for teaching students something untrue about hardware
does not survive measurement.**

**9.2 Speed is itself pedagogical.** At 8 kcycles/s a student can watch a CPU execute a
loop; at 40 kcycles/s they can run a sorting routine, a memory test, a small interpreter
— programs whose *behaviour* is the lesson rather than programs chosen to fit the
simulator. The `riscv/` trajectory in `docs/grand-architecture.md` §2 ("a serious
datapath / CPU teaching tool") is bounded today by a constant factor that is 95%
bookkeeping.

**9.3 The two simulation strategies are a lesson, not an implementation detail.**
Event-driven versus cycle-based, and transport versus inertial delay, are things
students in a digital-design course should meet. If a full cycle-based mode is ever
built, the right framing is not "a fast mode" but "a second model of time, with these
things it cannot show you" — and that framing requires `docs/simulation-semantics.md` to
gain a section rather than a footnote. The zero-delay levelization of §6.3 needs no such
section, which is another reason it is the right first step.

---

### 10. Risks, and the claims I could not settle

| risk | severity | mitigation / what would settle it |
|---|---|---|
| **X propagation raises event counts** | the one real perf risk of the program | measure on the `#202` golden at Stage 2; gate at +10% post-reset. Cannot be measured before an implementation exists. |
| Width-sensitive `equals` (`Word(4,3) ≠ Word(8,3)`) changes change-detection at `Output.propagate:139-145`, `Gate.react`, `Register.react`, `Mux.react`, `TriState.react` | correctness, not perf | keystone A §8 already flags it and budgets a week; the goldens plus `RiscvCpuGoldenTest` are the oracle |
| `Wide` (>64 bits) slower than `BitSet` | low — **no circuit in the tree exceeds 32 bits** | sparse (nullable `b`/`u`) planes measured at parity; take that design |
| Removing `dupCheck` changes glitch behaviour on a circuit I did not test | medium | it is specified behaviour (`SimEventDedupTest`, `docs/simulation-semantics.md` §3); replace with per-element coalescing, do not delete |
| A levelized zero-delay closure diverges from the queue on same-timestamp ordering | high if unmanaged | `docs/simulation-semantics.md` §3's "read-latest + FIFO" rule is exactly a topological evaluation of the zero-delay closure; but this must be *proved* against the goldens, not assumed |
| Benchmarks here are single-workload | medium | one circuit, one machine, one JDK. The direction of every result is large enough to survive noise; the magnitudes are not portable. |

**Explicitly unmeasured, and I want them on the record rather than glossed:**

- The interactive engine's per-event cost (§6.4) — structurally certain to be worse than
  batch, quantitatively unknown, no display in this environment.
- The cost of the zero-delay closure walk itself (§6.3) — needs real chain-depth data.
- Whether `dupCheck` pays for itself on any circuit (§4.4) — one workload says no.
- X-induced event inflation (§6.1) — the single most important open number.
- All microbenchmarks are hand-rolled with warm-up and best-of-N, not JMH (not a
  dependency of this project). Sources are in the scratchpad and are re-runnable; treat
  the ratios as sound and the absolute nanoseconds as indicative.

---

### 11. The verdict, restated with its conditions

**Is the performance cost of a richer value domain acceptable? The question is
mis-posed: there is no cost to accept.**

- **Under R4** — a sealed, immutable, width-carrying record split on width, three `long`
  planes for `width ≤ 64`, sparse (nullable `b`/`u`) `long[]` planes above — the measured
  per-operation cost is **10.85 ns against today's 21.11 ns at 32 bits**, four states and
  a dormant `U` plane included. Value work is 37.6% of the event loop, so the projected
  effect is a **15–25% faster loop**, and the reason is that the new type deletes the
  defensive clones that the old type's mutability required.
- **Under R1** (parallel `BitSet`s) the answer would be no: **4× slower**, at every
  width. If the value-domain program is going to fail on performance, that is how it
  fails, and sweep 01 recommends it. This is the most important single correction this
  sweep has to make to the other sweeps.
- **Under R2** (`byte[]` per bit) the answer is no for the kernel and yes for the
  specification-side type, exactly as keystone A §5.4 splits it.
- **R3** (packed interleaved) is not the kernel type but *is*, plane-major, the levelized
  array layout.
- "Object-per-value is presumably fatal" is **refuted by measurement**. Immutable
  object-per-value is the fastest option measured. Mutable object-per-value — what JLS has
  — is the slow one.

**Does a richer value domain make the levelized escape hatch more necessary?** No: it
makes the event loop faster. **Can the two be designed together?** They must be. A
levelized pass over plane arrays costs **4.32 ns/node**; the same pass over `BitSet[]`
costs **22.01 ns/node**. The value-domain change is not a tax the compiled pass has to
absorb — it is where **80% of the compiled pass's speedup comes from**. Building them
sequentially means writing the four-state truth tables twice against two data layouts,
six months apart, and discovering the disagreement through the `#202` differential
oracle.

**And the finding I did not expect to make.** On the measured profile, JLS's discrete-event
kernel spends **37.6% of its time on value-container overhead, 47.7% on event-queue
bookkeeping, and 4.9% on digital logic.** The `docs/grand-architecture.md` §6 line —
"the hot plane must stay free of indirection" — is being defended against a threat that
does not exist while the loop is already almost entirely overhead. The value-domain
program does not endanger the hot plane. **It is the first serious attention the hot
plane has had.**

---

### 12. Reproduction

Everything is under
`/tmp/claude-0/-home-user-JLS/c7a97eb3-cab3-5b44-a3e3-b63071913715/scratchpad/bench/`
and re-runnable against the tree at HEAD.

**Build and validate the oracle**

```sh
cd /home/user/JLS && mvn -q package -DskipTests
cd riscv && python3 verify.py            # 11 passed, 0 failed
```

**Build the CPU-scale workloads** (`/home/user/JLS/riscv/bench_kernel.py`, written this
session; it is a scratch harness, not part of the verification suite, and can be deleted)

```sh
cd /home/user/JLS/riscv && python3 bench_kernel.py 2000   # 6004 cycles, build/kbench.jls
```

Generated: `riscv/build/k500.jls`, `k1000.jls`, `k2000.jls` and matching `*_clk.txt`
(1504 / 3004 / 6004 cycles; time limits 3008000 / 6008000 / 12008000).

**Harnesses** (compiled against the jar; `jls.sim` package to reach the protected loop)

| file | what it measures |
|---|---|
| `jls/sim/KernelProbe.java` | event counts by payload and callback, queue depth, dup suppression |
| `jls/sim/KernelProbe2.java` | clean phase timing — `initSimulation` vs `runEventLoop`, no per-event instrumentation |
| `jls/sim/Census.java` | element / net / width census of a `.jls` |
| `jls/sim/NoDedup.java` | A/B of `dupCheck` on/off with a final-register-state oracle |
| `ValueRep.java` | R0–R4 representation bake-off at widths 1/8/32/64/128 |
| `ValueRep2.java` | `U`-plane cost, clone cost, single-array `Wide` |
| `WideSparse.java` | nullable-plane `Wide` at 128/256/512 |
| `Levelized.java` | levelized pass over plane arrays, 522 nodes, 4-state, activity variants |
| `LevelBitSet.java` | the same levelized pass over `BitSet[]` |
| `Kernels.java` | `BitSetUtils.SumCarry` vs `long` add; `PriorityQueue` ± dedup at depth 64/512/4096/12000 |

```sh
cd .../scratchpad/bench
javac -cp /home/user/JLS/target/jls-5.0.5-SNAPSHOT.jar -d . jls/sim/*.java
javac -d . ValueRep.java ValueRep2.java WideSparse.java Levelized.java LevelBitSet.java
javac -cp .:/home/user/JLS/target/jls-5.0.5-SNAPSHOT.jar -d . Kernels.java

java -cp .:$JAR jls.sim.KernelProbe  .../k2000.jls .../k2000_clk.txt 12008000 3
java -cp .:$JAR jls.sim.KernelProbe2 .../k2000.jls .../k2000_clk.txt 12008000 8
java -cp .:$JAR jls.sim.NoDedup      .../k2000.jls .../k2000_clk.txt 12008000 6
java -cp . ValueRep ; java -cp . ValueRep2 ; java -cp . WideSparse
java -cp . Levelized ; java -cp . LevelBitSet ; java -cp .:$JAR Kernels
```

**Profiling** — note the stack depth, without which ~30% of samples are truncated and
mis-attributed:

```sh
java -XX:FlightRecorderOptions=stackdepth=512 \
     -XX:StartFlightRecording=filename=deep.jfr,settings=profile \
     -cp .:$JAR jls.sim.KernelProbe2 .../k2000.jls .../k2000_clk.txt 12008000 12
jfr print --events jdk.ExecutionSample deep.jfr > deep.txt
```

Recordings kept: `bench/deep.jfr`, `bench/clean.jfr`, `bench/rec.jfr`, `bench/deep.txt`,
`bench/clean.txt`.

**Environment.** Linux 6.18.5 x86-64, the JDK on this container's PATH, default heap,
`JAR=/home/user/JLS/target/jls-5.0.5-SNAPSHOT.jar`. Microbenchmarks are hand-rolled
(warm-up rounds then best-of-7 over 3 M iterations, results accumulated into a checksum
to defeat dead-code elimination); JMH is not a dependency of this project. Ratios are
robust; absolute nanoseconds are machine-specific.
