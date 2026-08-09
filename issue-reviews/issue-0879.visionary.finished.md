# Issue #879: TASK-C232-2: the value plumbing carries the new type, the 61 defensive clones are deleted rather than ported, and the event loop is measurably no slower on the recorded k2000 baseline
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not speed. #232 was filed as an allocation/GC investigation, but
`docs/capability-roadmap/keystone-c-performance.md` §0 already re-stated the keystone
correctly: **"give a signal a width, an immutable identity, and a plane encoding."**
#879 is the moment JLS stops modelling a wire as *a mutable bag of bits plus a `null`*
and starts modelling it as *a value*. Everything downstream — four-state (#322),
radix 3/4 (#344), strength resolution (keystone-c §9.1), the levelized plane arrays
(§6.2) — is blocked not on a class existing (#878) but on it being **the currency**.
That is this task, and the direction is right.

I checked the one obvious out-of-the-box reframing and the repo refutes it: skip the
per-value object and cut straight at the netlist (plane arrays, levelized). Keystone-c
§6.2 measures that as throwing away 80% of the win, and `ARCHITECTURE.md:340-368`
(#221) forbids a second execution strategy until its revisit trigger fires. Values
first is correct. My objections are to the **seam**, the **gate**, and the **order**.

## Reframing 1 — cut at the waist, and make the ratchet the proof (this is the main one)

`Output.propagate` → `WireNet.propagate` → `Input.setValue` is a genuine narrow waist:
`WireNet.java:498` clones once per sink, `Output.java:150` clones once per drive,
`WireNet.java:420/433/516` clone on every get/set. Keystone-c §5's "today's clone tax"
(28.72 vs 17.85 ns at w=1, and the tree has 787 one-bit nets) is **almost entirely that
fanout clone**. Convert the waist — `Put.currentValue`, `Output`/`Input`/`WireNet`, the
three `SimEvent` payloads, `TraceSample` — and the allocation count per propagate drops
from n+1 to 1 immediately, with a diff of six files whose byte-identity is provable and
whose profile delta is attributable.

The issue instead makes "delete all 61, not port" atomic, which forces 23 files, ~24
`react` bodies, 29 coercion sites and a performance measurement into one PR. That is a
big-bang migration wearing a task's clothes, and the one-shot proof it buys is weaker
than the alternative for a reason the issue's own lineage records: **the footprint grew
under the feature while it was open.** #232's cycle-4 note says `RegisterFile` (23 sites)
and `FieldExtend` (5 sites) landed *after* the plan was written. A PR that reaches zero
once does not keep it there.

So: **replace P7's "no compatibility overload survives" with a CI ratchet.** JLS already
has this culture — `HeadlessCoreRatchetTest`, `ArchitectureRulesTest`,
`ExtensionPointCatalogTest`. A `ValueMigrationRatchetTest` that counts `BitSet`
occurrences under `src/jls/elem` and `src/jls/sim` and fails if the number rises is
enforced forever, checkable by a machine rather than by reading a diff, and it lets each
element migrate in its own attributable change. I am explicitly disregarding P7 as
written: the temporary boundary conversion it forbids is the price of a guarantee that
outlives the PR, and the terminal state (zero) is identical.

## Reframing 2 — gate on allocations, not on a wall clock

P6 is the weakest part of a strong issue. It gates on 0.742 s / 318 ns/event measured on
one machine, one JDK, one workload — and keystone-c §10 itself says "the magnitudes are
not portable." A red gate then means *maybe* a surviving clone, *maybe* a different CPU.

The hypothesis under test is H1: no clone survived. Measure that directly.
`ThreadMXBean.getThreadAllocatedBytes` around `runEventLoop`, or a JFR
`ObjectAllocationSample`, gives **bytes and objects allocated per event** — deterministic,
machine-independent, and falsifying in exactly the way O5's diagnostic wants. Predict it
in advance: allocations per propagate go from n+1 to 1 (0 for interned constants).
Note also that #232 §5's own integration criterion is "**allocation rate and GC time
reduced** and wall-clock not regressed" — #879 kept the noisy half of its parent's gate
and dropped the sharp half. Report the wall clock too, as an observation; do not gate on it.

Related, and free: keystone-c §5 notes allocation goes to **zero** for interned constants
(all-zero, all-one, all-Z per width). #879 never mentions interning, yet 43 `Constant`s
and 787 one-bit nets in the census mean `Word(1,0)`/`Word(1,1)` interning removes most
remaining loop allocation. It lives entirely inside the type — no scope creep — and it
materially changes the number P6 reports.

## Reframing 3 — the op set was frozen without its consumers, and the seam leaks

This is a concrete contradiction between #878 and #879 as filed, not a stylistic point.
#878 §5 freezes the surface at bitwise ops, `slice`, `concat`, `signExtend`,
`zeroExtend`, `resolve`, `toLong`, `toBigInteger`, `format(radix 2 only)`. But
`Put.currentValue` is read today by:

- `BitSetUtils.SumCarry` — `src/jls/elem/Adder.java:404` (no arithmetic op exists);
- `BitSetUtils.ToInt` — `Memory.java:1385,1397` (addressing), `ShiftRegister.java:671`;
- `BitSetUtils.ToString(v,16)` / `(v,10)` / `ToStringSigned` — `InputPin.java:232-234`,
  `Register.java:831-833`, `Memory.java:1493`, `DisplayRenderer.java:62`,
  `Trace.java:341,413`, `MemoryContentsDialog.java:66-68`;
- `BitSetUtils.toDisplay` — `Display.java:295`, `OutputPin.java:114,224`,
  `Register.java:467,681`, `Wire.java:342`, `Memory.java:950-951`, `InputPin.java:128`.

Widening `currentValue` therefore forces one of: a `BitSet` back-conversion at ~20 display
sites (the compatibility path P7 forbids, and a clone site in disguise), or radix 10/16
and signed formatting on the type *now* — the very ops #878 pushed to #419. **The
#878/#879 cut was made along "type vs plumbing" instead of along "what the consumers
need", and it leaks.** The elegant fix is a read-only survey task before either: walk the
~24 `react` bodies and the display sites, derive the op set from them, then freeze it.
A frozen field list is defensible on a measurement; a frozen *op set* derived without
looking at its callers is a guess that #879 will have to break.

## Reframing 4 — T4 (`Adder`'s carry) is a design smell, not a javadoc obligation

`Adder.react` computes a `bits+1`-wide `allsum`, then `carry.set(0, sum.get(bits))` and
`sum.clear(bits)` (`Adder.java:404-421`). On a value that *knows* its width, a w+1 value
whose ports are w wide is precisely the "width as a side channel" defect the type exists
to delete. #879's answer — "decide the carry shape and record it in the javadoc" —
preserves the smell in prose. The reframing that makes T4 disappear:
`LogicValue.addCarry(a, b, cin) -> (LogicValue sum /* width w */, boolean carryOut)`.
No over-wide value ever exists, `Adder` gets shorter, and the same op is what a levelized
plane-array pass (§6.2) needs anyway. This belongs in the op set (Reframing 3), not in
`Adder`'s comment.

## Reframing 5 — retiring the `TraceSample` HiZ marker is *cheaper* than deferring it

O3 says widen the record and keep the marker; §6 defers retirement. That deferral is not
conservative, it is contradictory. The marker is a `bits+1`-wide `BitSet` with the top bit
set (`BatchSimulator.java:263-265,306-308`), i.e. a second, incompatible width encoding
carried inside a type whose entire purpose is to carry width. Keeping it means
`TraceSample` holds values whose `width()` disagrees with the port, which every op
(`format`, `toLong` — which #878 makes *throw* on undriven bits) must then special-case.
With a Z plane, the marker is just `allZ(w)` and the VCD path asks `isAllZ()`. The
observable `z`/`bz` bytes are unchanged (the marker is internal, never serialized), so P5
still holds. Fold it in; deferring it costs more than doing it.

## Reframing 6 — build the >64-bit oracle instead of disclosing its absence

T2 is correct and its remedy is too weak: "state the lack of witness in the PR." The tree
already contains a generator — `riscv/make_cpu.py` / `bench_kernel.py` build circuits
programmatically, and `riscv/verify.py` is a working differential oracle (11/11 per
keystone-c §2). Synthesize a 96/128-bit design, run it under the parent commit's `BitSet`
build and under the new one, diff. That converts a disclosed blind spot into a real
oracle for the `Wide` path for the cost of one script — and it is the only thing that will
ever exercise the sparse-plane design keystone-c §5 chose.

## The one-way door nobody has named

Widening `SimEvent.NewValue`, `MemoryWrite`, `TableOutput` and `TraceSample` makes
`LogicValue` the shape of every `react` signature. `ARCHITECTURE.md:330-340` and
`docs/extension-points.md` are heading toward module-contributed elements; the moment
these public records carry the type, **`LogicValue` is JLS's element-author ABI**, not a
`jls.core` internal. #878's private plane layout and package-private constructors are the
right instincts; #879 should record, in `ARCHITECTURE.md`'s recorded-decisions section,
that this is the commitment point. That paragraph is worth more to the project's arc than
any of the checkboxes it would sit beside.

## What I would keep unchanged

O1–O5, H1–H4 and the §4 ordering finding are exemplary — especially refusing to resolve
the #362/keystone-c §7.2 contradiction and instead naming it so the scheduler chooses
with it visible. T5 (do not land with #476) is right and follows from the attribution
logic. Keep them all.

## Net

Right keystone, right stage, wrong seam and wrong gate. Convert the waist, ratchet the
rest to zero, gate on allocations per event, derive the op set from its callers before
freezing it, and fold in the marker retirement and constant interning that the type makes
free. Acceptance criteria I am explicitly setting aside: P7 as absolutism (replaced by a
CI ratchet), "review the 29 sites individually" as a deliverable (replaced by routing them
through one named coercion whose call count is asserted, so #322 flips them in one place),
and the deferral of the `TraceSample` marker.
