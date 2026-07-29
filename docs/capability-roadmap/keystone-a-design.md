## Value domain: design options

*Design pass over keystone A. Inputs: the six capability sweeps in this directory,
`docs/simulation-semantics.md` (normative), `docs/batch-interface.md` (stability
contract), `ARCHITECTURE.md`, `docs/grand-architecture.md`, `docs/file-format.md`,
and the tree at HEAD. Every JLS claim below carries a real path; external standards
claims are marked **unverified** where I did not read the standard.*

---

### 0. Verdict on the keystone claim, first

**Confirmed, with one honest dissent and one rival that is not a rival.**

The value domain is on the critical path of more standards, more pedagogy, and more
existing workarounds than any other single change in the six sweeps. The convergent
evidence:

- Sweep 01 counts 16 survey entries blocked directly and 24 counting dependents, and
  finds **four independent workarounds for one missing state**: `ImportSummary.coercedX`
  (`src/jls/hdl/imp/ImportSummary.java:28,59,97-100`), `TraceSample`'s marker-bit hack
  (`src/jls/sim/TraceSample.java:1-20`), `BatchSimulator`'s HiZ normalisation
  (`src/jls/sim/BatchSimulator.java:160-166`), and `BitSetUtils.toDisplay`'s literal
  `"HiZ"` string (`src/jls/BitSetUtils.java:237-245`).
- Sweep 03 makes it C1 and calls it "the single biggest teaching change in the sweep".
- Sweep 05 makes it B and says of I²C "this is the whole standard".
- Sweep 06 makes it C and finds it gating #100 LEF (inout pins), #112 IBIS' digital
  shadow, and #129 JTAG boundary cells.
- Sweep 02 finds SDF's `TIMINGCHECK` and EVCD both terminate on it.
- The frame's own example is worse than stated: `VhdlEmitter` asserts a value model the
  simulator does not have in **three** places — the `when others` full-coverage arm whose
  own doc comment says it exists to "satisf[y] VHDL's full-coverage rule over std_logic's
  nine values" (`src/jls/hdl/VhdlEmitter.java:466-472`), the hard-coded `(others => 'Z')`,
  and a generated-header disclaimer that says out loud "JLS simulates two states plus HiZ:
  this design drives '0'/'1'/'Z', never 'X'." (`src/jls/hdl/VhdlEmitter.java:100-101`).

**The dissent (record it):** sweep 04 explicitly says the verification tier does *not*
need X — "X is *not* required for the useful subset" — and that assertions/coverage touch
~3 of the 25 `react` files rather than all 25. That tier is genuinely independent of this
work and should not be sequenced behind it.

**The rival that is not a rival:** sweep 06's finding that JLS cannot HDL-export its own
flagship design (`HdlExporter.EXPORTED` at `src/jls/hdl/HdlExporter.java:417-424` omits
`SubCircuit`, `Memory`, `ShiftRegister`) is a bigger *immediate embarrassment* and is
blocked by **nothing in the value domain**. It shares no code with this work. The two
programs should run in parallel, not in series. The value domain is the keystone by
*reach*; export coverage is the keystone by *urgency*.

---

### 1. What is actually being replaced — the exact inventory

Not "BitSet". Four distinct things fused into one under-specified channel:

| # | Thing | Where it lives now | Problem |
|---|---|---|---|
| 1 | The **bit alphabet** | `java.util.BitSet` bits | two-state only |
| 2 | **Undrivenness** | a `null` reference in `Put.currentValue` (`src/jls/elem/Put.java:385`), `Input.setValue/getValue` (`src/jls/elem/Input.java:59,72`), `Output.propagate` (`src/jls/elem/Output.java:136`), `WireNet.propagate` (`src/jls/elem/WireNet.java:443`), `WireNet.value` (`:404-405`), `SimEvent.TriStateOff` (`src/jls/sim/SimEvent.java:47`) | all-or-nothing per signal; forces `@Nullable` through the whole value channel |
| 3 | **Width** | *nowhere in the value* — it is a property of `Put.bits`/`WireNet.bits`, and `BitSet` is unbounded | forces `Constant.react` to mask to the net width; forces `BitSetUtils.ToStringSigned(bs, bits)` and `toDisplay(value, bits)` to take width as a side-channel argument; forces `TraceSample` to encode HiZ as "bit `bits` set" in a `bits+1`-wide BitSet |
| 4 | **Mutability** | `BitSet` is mutable, so every hand-off clones | `WireNet.propagate` clones **per sink** (`:496-498`) and again for the probe copy (`:513-516`); `Output.propagate` clones (`:148-153`); `TriState.react` clones twice per event; `Register.react` clones per posting; ~20 sites total |

And one thing that does not exist at all: **drive strength**. `grep -rn "strength" src/jls/`
returns nothing under `src/jls/sim/` or `src/jls/elem/`; the only `pullup`/`pulldown` in the
tree is the Verilog reserved-word list at `src/jls/hdl/HdlNames.java:51`.

The measured migration surface (reproduced this pass):

```
grep -rn "BitSet" src/ --include=*.java | wc -l        → 417   (51 files)
grep -rl "BitSet" test/ --include=*.java | wc -l       →  21   files
grep -rl "public void react(" src/ | wc -l             →  25   (24 concrete + the throwing base)
grep -rn "BitSet computeOutput" src/jls/elem/          →   9   (8 concrete + the abstract at Gate.java:663)
"== null" immediately followed by "new BitSet()"       →  29   sites across 17 element classes
```

The 29 coercion sites are the load-bearing ones. They are the places where "I am not being
driven" is rewritten to "I am zero" — e.g. `AndGate.computeOutput`
(`src/jls/elem/AndGate.java:64-75`), `Mux.react`'s selector
(`src/jls/elem/Mux.java:~526-541`), `Register.react`'s clock and data
(`src/jls/elem/Register.java`, head of the `PinChanged` arm), `TriState.react`'s control
and data (`src/jls/elem/TriState.java:~478-508`), and eight in `Memory.react`.

---

### 2. Two orthogonal axes

The four candidates in the brief conflate two independent choices. Separate them:

- **Axis 1 — the per-bit alphabet** the simulator computes over.
- **Axis 2 — where drive strength lives**: in the value (every signal carries it), or on
  the driver (only `Output` carries it, and only the net's *resolved* observable carries
  the resolved strength).

Getting axis 2 right is what makes axis 1 cheap. **Strength is a property of a driver and
an observable of a net; it is not a property of a signal.** A gate's inputs do not care
whether the 1 they see was driven strongly or by a pull-up — IEEE 1164's own `to_X01`
collapses `H`→`1` and `L`→`0`, which is exactly this projection (**unverified**, but it is
the defining behaviour of the function). Putting strength in the signal type multiplies the
cost of every element `react` by a factor of three for information no element uses.

---

### 3. Candidate alphabets

#### A1 — 4-state per bit (0/1/X/Z), Verilog's model

Five values' worth of meaning in four codes. Two planes per bit; the IEEE 1364 VPI
`s_vpi_vecval` aval/bval encoding (**unverified**: cited from sweep 01, which also flags it
unverified): `(a,b)` = `(0,0)`→0, `(1,0)`→1, `(0,1)`→Z, `(1,1)`→X.

- Gets: X propagation, per-bit Z, honest bus conflict, `x` in VCD, faithful Yosys import,
  `logic` vs `bit`, mixed vectors like `b1z0`.
- Misses: `U` (uninitialized, distinct from unknown), `W`/`L`/`H` (weak values), `-`.
- Cost: cheapest. Bitwise ops are 6–8 `long` operations per 64-bit word.

#### A2 — IEEE 1164's nine values (U/X/0/1/Z/W/L/H/-) as the kernel alphabet

The VHDL model `VhdlEmitter` already emits against.

- Gets: everything A1 gets, plus U, plus weak values, plus don't-care, plus a real
  resolution function with the standard's own table as a test oracle.
- Costs, and they are structural, not merely large:
  1. Nine values need 4 bits, so either 4 planes or a byte-per-bit code array. Plane
     encoding of the 1164 tables is *not* natural — the tables are defined by lookup, and
     reproducing them plane-parallel means first collapsing L→0, H→1, W/Z/-→X (i.e.
     computing A1 anyway) and then re-applying U-stickiness.
  2. **`W`, `L`, and `H` are not independent states — they are `X`, `0`, and `1` at weak
     strength.** Making them kernel alphabet symbols while there is no strength model puts
     the *shadow* of strength in the alphabet without the substance. That is the same
     category of pretence as `VhdlEmitter:466-472`, moved one layer down.
  3. **`-` is not a simulation state at all.** 1164 never *resolves to* `-`; it appears in
     `std_match` and case choices. It is a specification value: a truth-table cell
     (`TruthTable` already stores it as `2`, `src/jls/elem/TruthTable.java:79`), a
     test-vector expectation, a synthesis don't-care. Putting it in the runtime alphabet
     forces every element to answer "what does AND of don't-care mean" — a question with no
     hardware referent.
- Verdict: **the right interchange alphabet, the wrong kernel alphabet.**

#### A3 — A1 (or A2) plus an explicit strength lattice (Verilog's 8 levels)

`supply(7) / strong(6) / pull(5) / large(4) / weak(3) / medium(2) / small(1) / highz(0)`,
with independent `strength0` and `strength1` per driver (**unverified**). This is what EVCD
needs (`docs/standards-adoption/07-waveform-formats.md:150-151` names "JLS gains a
drive-strength value domain" as its own revisit trigger, verbatim).

Under axis 2, this is **not an alphabet change**: it is two 3-bit fields on `Output` plus a
fold in `WireNet`. Cost is contained to one class and one function.

#### A4 — A1 plus a `U` plane, plus A3's strength on the driver — **recommended**

Three planes per bit: `a` (value when known), `b` (unknown-or-undriven), `u` (never
assigned). Five kernel states: `0 1 X Z U`. Strength on the driver. Nine-value IEEE 1164 is
then a **total, testable projection** of `(kernel state, resolved strength)`:

| 1164 | produced from |
|---|---|
| `U` | `u` plane set |
| `X` | `b=1, a=1`, resolved at a forcing strength (supply/strong) |
| `W` | `b=1, a=1`, resolved at a non-forcing strength (pull…small) |
| `0` / `1` | `b=0`, resolved at a forcing strength |
| `L` / `H` | `b=0`, resolved at a non-forcing strength |
| `Z` | no driver above `highz` |
| `-` | **never produced by simulation** — specification side only |

Eight of the nine values are simulation states; the ninth is a specification value with its
own type. That is a defensible, complete story rather than a subset with an apology.

**Why `U` earns a whole plane.** It is the only 1164 value that is not derivable from
(4-state, strength), and it is the state that makes reset teachable. Today
`LogicElement.initInputs` (`src/jls/elem/LogicElement.java:476-481`) zeroes every input at
every depth and `Register.initSim` drives the configured `init` — so a design with no reset
network simulates perfectly, a student never learns why reset exists, and the `riscv/` CPU
passes tests a real CPU would fail because JLS is quietly supplying a reset the design does
not have. "Never assigned" and "contended" are different diagnoses and a student should see
different characters for them. The 1164 AND-table's U-stickiness rule (U dominates except
against a known 0) is one extra `long` operation per word (**unverified table**, verify
before pinning a test to it).

**Why ship the `u` plane on day one even though nothing sets it until stage 3.** The entire
cost of this program is the migration — 417 `BitSet` references, 24 `react` bodies, every
golden. Doing that migration twice is not acceptable. A dormant third plane costs 8 bytes
per value and 2–3 `long` ops per gate evaluation and removes a second migration. Reserve
it.

---

### 4. Candidate Java representations

Assume 64-bit HotSpot with compressed oops; sizes are per value object for a 32-bit signal.

| | Representation | Bytes | Ops per 32-bit AND | Carries width? | Immutable? | Extends to `u` plane? |
|---|---|---|---|---|---|---|
| **R1** | Two parallel `BitSet`s | ~112 (2 objs + 2 arrays) | 2 `BitSet.and` + bookkeeping | no | no — clone count *doubles* | third BitSet, ~168 B |
| **R2** | `byte[]` of state codes | ~76 | 32 table lookups | via `length` | array is mutable | free (more codes) |
| **R3** | packed `long[]`, 2–4 bits interleaved per position | ~68 | SWAR shift/mask gymnastics | needs a field | array is mutable | needs re-packing |
| **R4** | **Sealed value type over `long` planes, split on width** | **48** | **~8 `long` ops** | **yes, a field** | **yes — clones disappear** | **yes, one more `long`** |
| — | *today's* `BitSet` | ~56, mutable | 1 `BitSet.and` + `wordsInUse` bookkeeping | no | no | n/a |

**R1 (two parallel BitSets)** is what sweep 01 recommends, and it has the smallest diff:
every existing `and`/`or`/`xor`/`get`/`set` idiom ports one-for-one and `BitSetUtils`
survives. Rejected anyway, on four counts: (i) it keeps `BitSet`'s mutability, so the ~20
defensive `clone()` sites become ~40 rather than zero; (ii) it keeps `BitSet`'s
unboundedness, so problem #3 in §1 — the missing width — survives untouched, and with it
`Constant.react`'s masking, `toDisplay(value, bits)`'s side-channel argument, and
`TraceSample`'s marker-bit hack; (iii) two objects plus two arrays is 2× today's footprint
where R4 is 0.85×; (iv) adding `u` later means a third parallel BitSet and a third clone at
every site. It is the honest fallback if the migration proves unaffordable, and worth naming
as such — but it fixes one of the four problems in §1 and R4 fixes all four.

**R2 (`byte[]` of state codes)** is the easiest to read and the only one where getting nine
values right is trivial. It is ~30× slower per gate on a 32-bit bus (a loop of table lookups
against one word operation), which lands squarely on the hot plane that
`docs/grand-architecture.md` §6 protects by name. Rejected for the kernel — **and adopted
for the specification-side type** (§5.4), where `-` exists, widths are small, and
performance is irrelevant.

**R3 (interleaved packed longs)** carries the same information as planes but every operation
needs shift/mask extraction first. Planes dominate it strictly. Rejected.

**R4** is the recommendation.

---

### 5. The recommended design

#### 5.1 `LogicValue` — the signal type

Lives in `jls.core` (which already exists and is AWT-free: `Bounds`, `Geometry`,
`Orientation`, …), so it is inside the `#77` headless kernel by construction and
`HeadlessCoreRatchetTest` covers it for free.

```java
public sealed interface LogicValue permits LogicValue.Word, LogicValue.Wide {

    /** Declared bit width. Always known; never zero for a driven signal. */
    int width();

    /** The 5-state code at bit i. */
    Logic get(int i);

    /** No X, no Z, no U anywhere: the fast path and the guard before toLong(). */
    boolean isFullyKnown();

    boolean hasUnknown();      // any X
    boolean hasUndriven();     // any Z
    boolean isFullyUndriven(); // what "null" used to mean

    // ... ops in §5.3
}

/** width <= 64: three longs, no array, one object. */
record Word(int width, long a, long b, long u) implements LogicValue { }

/** width > 64. */
record Wide(int width, long[] a, long[] b, long[] u) implements LogicValue { }
```

Per-bit codes `(a, b, u)`:

| a | b | u | meaning |
|---|---|---|---|
| 0 | 0 | 0 | `0` |
| 1 | 0 | 0 | `1` |
| 0 | 1 | 0 | `Z` — undriven |
| 1 | 1 | 0 | `X` — unknown/conflict |
| – | – | 1 | `U` — never assigned (dominates) |

Bits at or above `width()` are canonically zero in all three planes, so `equals` is exact
and `hashCode` is free. All values are canonicalised at construction.

```java
public enum Logic { ZERO, ONE, X, Z, U }
```

Deliberately **not** the sealed `Binary` / `FourState` split sweep 01 proposes. The split
there is on *state-ness*, which flips constantly during a run — every net that goes X and
comes back changes shape, producing megamorphic call sites at every op. The split here is on
*width*, which is fixed for a signal's whole lifetime, so each call site stays monomorphic in
practice. The "two-state values are cheap" property is preserved anyway, by the `b == 0 && u
== 0` test inside `Word` — a single `long` OR and a branch, not a type dispatch.

Interned constants for `width <= 64`: `zeros(w)`, `ones(w)`, `allZ(w)`, `allX(w)`, `allU(w)`.
Most propagations of an all-zero or all-Z value then allocate nothing at all.

#### 5.2 Strength lives on the driver; `NetValue` is the net's observable

```java
// on Output, defaulting to (STRONG, STRONG) == today's behaviour
private byte strength0 = 6;   // Verilog level driving 0
private byte strength1 = 6;   // Verilog level driving 1
```

Everything the sweeps want from a driver-kind enum falls out of this pair, with **no new
kernel concept**:

| idiom | encoding |
|---|---|
| push-pull (today) | `(strong, strong)` |
| open-drain / open-collector | `(strong, highz)` — drives 0, releases for 1 |
| open-source / open-emitter | `(highz, strong)` |
| pull-up element | a constant-1 `Output` at `(highz, pull)` |
| pull-down element | a constant-0 `Output` at `(pull, highz)` |
| tri-state gate off | both drop to `highz` for the whole word |

A `driverKind` enum still belongs in the *dialog and save format* as a named preset over the
pair — that is a UI affordance, not a model concept. This is a deliberate simplification of
sweep 01's V3 and sweep 05's B.

The net's resolved observable carries strength; the value delivered to sinks does not:

```java
/** What a net actually resolved to: value plus per-bit winning strength. */
record NetValue(LogicValue value, long s0, long s1, long s2) { }   // 3 bit-planes = level 0..7
```

`WireNet` stores the `NetValue` (for probes, the trace window, stdout display, VCD/EVCD, and
the 1164 projection); `Input`s receive `NetValue.value()`. Collapsing `H`→`1` at a receiver
is what `to_X01` does and what hardware does.

**One exception, and it maps onto an interface that already exists:** the `TriProp`
implementors (`src/jls/elem/TriProp.java` — pins, splitter, binder) are the elements through
which tri-state-ness already propagates at edit time (`WireNet.setTriState`,
`src/jls/elem/WireNet.java:359-388`). Make them **strength-transparent**: they forward the
incoming resolved strength rather than re-driving at their own. Without that, an open-drain
bus stops working the moment it crosses a subcircuit boundary. Every other element drives at
its `Output`'s declared strength.

#### 5.3 Operations — the API element authors actually touch

Today an element author writes:

```java
BitSet inVal = input.getValue();
if (inVal == null) inVal = new BitSet();       // ×29, across 17 classes
```

After:

```java
LogicValue d = inputs.get(0).get();            // never null, width known
```

The op surface, grouped by what it is for:

**Bitwise (4-state tables built in).** `and(a,b)`, `or`, `xor`, `nand`, `nor`, `xnor`,
`not(a)`. Implemented plane-parallel, ≤8 `long` ops per word. AND, for example: with `k0 =
~a & ~b` ("known zero"),

```
b' = (b1|b2) & ~(k0_1|k0_2)        // unknown unless somebody is a known 0
a' = ((a1&~b1) & (a2&~b2)) | b'    // normalised so unknown bits have a=1
u' = (u1|u2) & ~(k0_1|k0_2)        // 1164 U-stickiness (unverified table)
```

**Arithmetic-shaped helpers.** `add(a,b,carryIn)` — the useful rule is that unknowns
poison upward: if the lowest unknown bit is at position *k*, every bit from *k* up is X.
With this encoding that is `Long.numberOfTrailingZeros(b|u)` and a mask — one instruction
where a byte-array representation needs a loop. This is the concrete payoff of the plane
encoding, and it replaces `BitSetUtils.SumCarry` (`src/jls/BitSetUtils.java:196-226`).

**Structural.** `slice(lo, hi)`, `concat(...)`, `withBit(i, Logic)`, `resize(w)`,
`signExtend(w)`, `zeroExtend(w)`, `reverse()`. `Splitter.react` and `Binder.react` are
written entirely in these and get *shorter* — the all-or-nothing branches at
`src/jls/elem/Splitter.java:~207-215` and `src/jls/elem/Binder.java:~243-250` delete
outright.

**Interpretation, with an explicit guard.**
- `long toLong()` / `int toInt()` / `BigInteger toBigInteger()` — **throw** if
  `!isFullyKnown()`. An element that indexes an array with a signal value must decide what
  an unknown index means; the throw makes forgetting a compile-time-visible bug rather than
  a silent zero.
- `OptionalLong asLong()` — the checking form.
- **`LogicValue zeroFill()`** — replaces every X/Z/U bit with 0 and returns a fully-known
  value. This is *the* migration lever (§7): one named, greppable, javadoc'd method whose
  doc says "legacy coercion: reproduces the pre-four-state rule that an undriven input reads
  as zero (`docs/simulation-semantics.md` §2). Every remaining call site is an element whose
  unknown-input semantics have not yet been decided."

**Display.** `toDisplay()` with no `bits` argument — the value knows its width.
`BitSetUtils.toDisplay(value, bits)` (`src/jls/BitSetUtils.java:237-245`) and
`ToStringSigned(bs, bits)` (`:103-118`) lose their side-channel parameter, and the literal
`"HiZ"` string becomes per-bit rendering.

#### 5.4 `-` gets its own type

`Bits4` (an `R2`-style `byte[]` of codes over `{0, 1, X, Z, -}`) is the **specification-side**
value: `TruthTable` cells (already stored as `2` at `src/jls/elem/TruthTable.java:79` and
then destroyed by `react`'s "don't care becomes false"), `-t` test-vector expectations
(`docs/batch-interface.md` §2.2 admits only integers today — the Logisim catch-up bar named
at `docs/hdl-support-research.md:185-190` requires don't-cares, high-Z and sequential mode),
and synthesis don't-cares reaching the exporter (`HdlModel`'s "don't care **already
lowered**"). It converts *to* `LogicValue` by a stated policy and never appears as a
simulated net value. Small, slow, readable — exactly right for this job, and exactly wrong
for the kernel.

#### 5.5 The resolution function

Today (`src/jls/elem/WireNet.java:454-485`): on every propagate, re-walk `ends`, call
`isAttached()`, `getPut()`, and `instanceof Output` on each, take the **first active driver
in net order**, and if two disagree raise a one-shot `TellUser.warn`
(`src/jls/elem/WireNet.java:472-483`). That rule is not a resolution function — it is not
commutative, not associative, and depends on the order wire ends entered the net, which is
why `docs/simulation-semantics.md` §9 has to explain a breadth-first file-order walk to make
it deterministic at all.

Replacement, in two parts:

**(a) Elaboration-time caching.** `WireNet.makeNet` (`:97-165`) and `WireNet.recheck`
(`:272-302`) already walk the ends to compute `bits`, `hasinput` and `triState`. Have them
also cache `Output[] drivers` and `Input[] sinks`. The per-event loop then runs over arrays
instead of re-walking a `LinkedHashSet` with three virtual calls and an `instanceof` per
end. **This makes strength resolution faster than today's first-driver scan, not slower** —
the one place in this program where the honest answer to "what does it cost" is "less than
zero".

**(b) A real fold.** Per bit: take the maximum strength among drivers driving 0 (call it
`s0`) and among drivers driving 1 (`s1`); an X driver contributes to both piles at its own
strength.

```
s0 == 0 && s1 == 0  ->  Z
s0 >  s1            ->  0 at strength s0   (rendered 1164 'L' if s0 is non-forcing)
s1 >  s0            ->  1 at strength s1   (rendered 'H' if non-forcing)
s0 == s1            ->  X at that strength (rendered 'W' if non-forcing)
u on any winner     ->  U
```

Three fast paths keep the common cases at today's cost or below:

1. **One cached driver, strong push-pull** (every non-tri-state net in every existing
   circuit): deliver the driver's value verbatim, no fold.
2. **Tri-state, all drivers strong push-pull** (every tri-state net in every existing
   circuit): fold with three running masks — `driven`, `ones`, `conflict` — where
   `conflict = driven & (previouslyDriven) & (ones ^ newOnes) | ...`. Six `long` ops per
   driver per word. No strength arithmetic at all.
3. **Weak drivers present**: the general per-level fold. Only nets with a `PullUp`,
   `PullDown` or open-drain driver reach it.

Verilog's ambiguous-strength ranges are explicitly **not** modelled: JLS collapses ambiguity
to X at the higher strength. Say so in `docs/simulation-semantics.md` §9 rather than
discovering it later.

`wand`/`wor`/`tri0`/`tri1` net kinds stay a real net attribute (wired-AND of two strong
drivers is 0, which strength alone cannot produce) — but note that **open-drain drivers plus
a pull-up produce wired-AND behaviour emergently**, which is the physically honest mechanism
and the better one to teach. The net kinds become a Verilog-import convenience rather than
the primary way to build a bus.

**The test that today's code cannot pass and the new one can:** a property test asserting
that `resolve` is invariant under permutation of the driver list. Add
`ResolutionAlgebraTest` alongside a table test against IEEE 1164's own resolution table (all
81 pairs; **the table is unverified — read 1164 before pinning it**). Passing the permutation
test retires the file-order dependence in `docs/simulation-semantics.md` §9 entirely.

#### 5.6 How HiZ stops being all-or-nothing — site by site

| Site | Today | After |
|---|---|---|
| `Put.currentValue` (`Put.java:385`) | `@Nullable BitSet` | non-null `LogicValue` |
| `Input.setValue` / `getValue` (`Input.java:59,72`) | `@Nullable` | non-null |
| `Output.propagate` (`Output.java:136-169`) | `@Nullable` param, null-vs-null change check, clone | non-null, `equals` on an immutable record, no clone |
| `WireNet.value` (`WireNet.java:404-405`) | `@Nullable BitSet` | `NetValue` |
| `WireNet.propagate` (`WireNet.java:443-529`) | `@Nullable` param, clone per sink, clone for probe | non-null, **share one immutable object with all sinks** |
| `SimEvent.TriStateOff` (`SimEvent.java:47`) | a sentinel payload meaning "drive null" | either kept as an intent marker or folded into `NewValue(allZ(w))` |
| `Splitter.react` (`Splitter.java:~207-215`) | input null ⇒ **all** outputs null | Z bits slice through per-bit; branch deletes |
| `Binder.react` (`Binder.java:~243-250`) | output null only if **every** input null, else HiZ inputs contribute zeros | Z bits concatenate per-bit; `allOff` flag deletes |
| `TraceSample` (`TraceSample.java:1-20`) | HiZ = "extra top bit set in a `bits+1`-wide BitSet" | `TraceSample(long, LogicValue)`; hack deletes |
| `BatchSimulator` HiZ normalisation (`:160-166`) | build a marker BitSet before comparing | `LogicValue.equals` |
| `BatchSimulator.vcdValue` (`:510-553`) | `0`/`1`/`z`/`bz` only; `docs/batch-interface.md` §4.3 says mixed vectors "cannot occur" | full `0 1 x z` alphabet, `b1z0` emittable |
| `BitSetUtils.toDisplay` (`:237-245`) | returns the literal `"HiZ"` for null | per-bit rendering, width from the value |
| `LogicElement.initInputs` (`:476-481`) | every input at every depth ⇒ 0 | unconnected input ⇒ all-Z (or all-U under stage 3) — **"your input is floating" becomes visible for the first time** |
| `ImportSummary.coercedX` (`:28,59,97-100`) | a counter reporting information destroyed on import | field and its plumbing delete |

---

### 6. Memory and hot-loop budget

Per 32-bit value: today ~56 bytes across two objects, mutable, cloned at ~20 sites.
After: 48 bytes in one object, immutable, cloned nowhere.

The bigger win is object *count*, not size. `WireNet.propagate` currently allocates one
clone **per sink** (`:496-498`) plus one for the net's own copy (`:513-516`). With immutable
values, a net with *n* sinks goes from *n+1* allocations per change to **zero** — the same
object is shared by the net and every input. Interned constants remove the allocation for
all-zero and all-Z propagations entirely. **Net allocation per simulation event goes down,
not up.**

Per-gate CPU: 6–8 `long` ops with three planes, versus today's `BitSet.and` (a word loop
with `wordsInUse` bookkeeping, an allocation for the result, and a `recalculateWordsInUse`).
For width ≤ 64 the new path is very likely *faster*. For width > 64 it is comparable.

`docs/grand-architecture.md` §6's hot-plane rule is satisfied by construction: `LogicValue`
lives in `jls.core`, `Simulator.runEventLoop` is untouched, and nothing on the inner loop
does a capability lookup or a cross-module call.

**The gate for all of this already exists.** `ARCHITECTURE.md:359-368` binds any future
simulation strategy to bit-for-bit agreement with the `#202` RV32I integration golden as a
differential oracle. Use the same gate here: `test/jls/RiscvCpuGoldenTest.java` plus
`riscv/verify.py`'s differential fuzzing against a reference emulator, run in two-state
compatibility mode, must be byte-identical to pre-change output.

---

### 7. What the migration means for element authors

Triage of the 24 concrete `react` bodies:

- **8 mechanical pass-throughs** — `Constant`, `JumpEnd`, `JumpStart`, `InputPin`,
  `OutputPin`, `Stop`, `Pause`, `SigSim`. Type swap only. (`Constant.react`'s width masking
  can *delete*: the value carries its width.)
- **6 that get simpler** — `Splitter`, `Binder`, `Extend`, `SubCircuit`, `Clock`, `Display`.
  Their all-or-nothing HiZ branches disappear.
- **10 that need a real semantic decision** — `Gate` (+ the 8 `computeOutput`s), `Adder`,
  `Decoder`, `Mux`, `Register`, `ShiftRegister`, `StateMachine`, `TruthTable`, `TriState`,
  `Memory`. `Memory` alone holds 8 of the 29 coercion sites.

**The lever that makes this affordable:** in the first pass, every one of the 29 coercion
sites becomes `.zeroFill()` — a mechanical, one-token substitution that preserves behaviour
exactly. The semantic decisions (a proper three-valued AND table, X on an unknown mux
selector, no latch on an unknown clock, X out of an unknown memory address) are then made
**one element at a time, each with its own golden and its own `docs/simulation-semantics.md`
§6 paragraph**, and progress is measurable as the `grep -c zeroFill` count falling. That
converts a 24-file flag-day into a sequence of reviewable commits, and it is the single most
important mechanic in this design.

---

### 8. The staged path — and the intermediate shippable stage

#### **Stage 0 — the type lands; nothing observable changes.** ~2–3 weeks. **This is the answer to "what is the intermediate shippable stage".**

Introduce `LogicValue` in `jls.core` with all three planes present but **X and U never
produced by any element**, and make it the currency of `Put`, `Input`, `Output`, `WireNet`,
`SimEvent.NewValue`/`MemoryWrite`/`TableOutput`, `TraceSample`, and all 24 `react`s. `null`
is banished from the value channel: it becomes `LogicValue.allZ(width)`. All 29 coercion
sites become `.zeroFill()`.

**Why this is byte-identical:** Z behaves exactly as `null` did (every reader zero-fills it),
X and U are never constructed, and the resolution rule is untouched. `Splitter`'s "input Z ⇒
all outputs Z" now *falls out* of slicing rather than being a special case, and produces the
same values. Every golden — `BatchSimulationGoldenTest`, `SequentialGoldenTest`,
`VcdExportGoldenTest`, `ElementSimulationGoldenTest`, `RiscvCpuGoldenTest` — must pass
unmodified. That is the gate, and it is cheap.

**The one real risk in Stage 0** is that `equals` becomes width-sensitive where `BitSet`'s
was not: `Word(4, 0b0011)` ≠ `Word(8, 0b0011)`, whereas the two BitSets are equal today.
Every change-detection site is affected — `Output.propagate`'s check (`:139-145`),
`Gate.react`'s `toBeValue` compare, `Register.react`'s `d.equals(toBeValue)`,
`Mux.react`'s, `TriState.react`'s. Mitigation: values are constructed at the producing put's
declared width throughout, and the golden suite plus `RiscvCpuGoldenTest` is the oracle.
Budget a week for the widths that turn out to disagree.

**Why it is worth shipping on its own, with no new capability:**
- Deletes ~20 defensive `clone()` calls and reduces allocation per propagate from *n+1* to 0.
- Deletes `TraceSample`'s marker-bit hack and `BatchSimulator`'s HiZ normalisation.
- Deletes `@Nullable` from the entire value channel — a real class of NPE risk, and a
  simplification the jspecify annotations in the tree are already reaching for.
- Makes the value carry its width, which retires `Constant.react`'s masking and the
  `bits` side-channel argument in `toDisplay`/`ToStringSigned`.
- Converts the risky 417-reference, 24-`react`, every-golden migration into a *mechanical*
  refactor with a bit-identical oracle — so all subsequent stages are small.
- `docs/simulation-semantics.md` §2 is rewritten once, honestly: "the value type is
  five-state; X and U are not yet produced by any element."

Sequencing note: `LogicValue` lands *in* `jls.core`, which
`docs/grand-architecture.md` §3 names as the not-yet-extracted keystone (#77). `jls.core`
already exists as a package with AWT-free contents, so Stage 0 does not require the full
#77 extraction — but it is a large type migration across a boundary that is not yet drawn.
Prefer #77 first; accept Stage 0 as part of the extraction if not.

#### Stage 1 — X becomes producible. ~3–4 weeks.

Two X sources turn on: (a) `WireNet` resolution — simultaneous conflicting drivers resolve
per-bit to X instead of "first active driver in net order", and the `TellUser.warn` at
`WireNet.java:472-483` becomes an *optional diagnostic on top of a real value* instead of a
warning *in place of* one; (b) the Yosys importer stops coercing
(`NetlistImporter.connectConstant`, `ImportSummary.coercedX` deleted). Element semantics
land here, one at a time, as `zeroFill()` call sites are retired. X renders on the trace
(conventional red), `x` in VCD, `X` on stdout.

Guarded by `--value-model=two-state|four-state` defaulting to `two-state` for one release —
`docs/batch-interface.md`'s stability-contract preamble requires exactly this, and it is the
same shape `Memory`'s `sync` attribute used for #199. Autograders do not break mid-semester.

Retired here: `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ` becomes
mode-conditional; `SimulationSemanticsRegressionTest.multiDriverConflictResolvesDeterministicallyAndWarnsOnce`
is re-derived (it currently pins the wrong answer as correct);
`SimulationSemanticsRegressionTest.triStateDoesNotRepostUnchangedOutputEvents` changes
protocol.

**This is the pedagogically decisive stage.** Bus conflict propagates X and the student
watches the datapath go red instead of dismissing a dialog; a floating input is visible
instead of silently zero; three-valued gate tables become demonstrable and assignable.

#### Stage 2 — strength, driver kinds, pulls. ~3–4 weeks.

`Output` gains `strength0`/`strength1`; `TriProp` implementors become strength-transparent;
the general fold and the `PullUp`/`PullDown` elements land; net kinds `wand`/`wor`/`tri0`/
`tri1`. Unlocks the open-drain I²C lab in full (wired-AND arbitration, ACK/NACK, clock
stretching), EVCD (`07-waveform-formats.md:150-151`'s own revisit trigger fires verbatim),
and the IEEE 91 / IEC 60617-12 output qualifying symbols that
`docs/standards-adoption/01-iec-ieee-symbols.md:44-46` currently carves out of the
conformance claim.

#### Stage 3 — `U` turns on; the reset model. ~2–3 weeks.

The `u` plane goes live. `Register.init` becomes optional and defaults to unknown;
uninitialised `Memory` words read U; `Register` gains async reset/preset pins (independently
the `$adff` gap named at `docs/hdl-support-research.md:466-470`). Per-circuit
`init-model = zero | unknown` attribute defaulting to `zero`; flip the default at the next
major. Retires `SequentialGoldenTest.registerInitialValueAppearsBeforeAnyClockEdge` and
`SimulationSemanticsRegressionTest.initInputsReachesInsideSubcircuits`, both of which
currently assert the free-reset fiction, and closes the `riscv-arch-test` correctness gap
(a compliance suite against a CPU the simulator is secretly resetting is not a compliance
claim).

#### Stage 4 — the IEEE 1164 projection and `-`. ~2 weeks.

`Std_logic` view over `(LogicValue, NetValue strength)`, tested against the standard's own
resolution and logic tables. `Bits4` specification type for truth-table don't-cares and the
`-t` grammar. `VhdlEmitter` deletes its header disclaimer (`:100-101`) and its class-doc
caveat; the `when others` arm at `:466-472` stops being a fig leaf and becomes an ordinary
default. GHDL differential testing in `test/jls/hdl/GhdlCompileTest.java` becomes meaningful
for the first time, because the two simulators finally share a value domain.

**Total: 12–16 maintainer-weeks**, consistent with sweep 01's 10–14 and sweep 03's 10–16.

---

### 9. Ripple effects that must be scheduled, not discovered

- **`docs/simulation-semantics.md`** is the blast radius: §2 rewritten wholesale, §5, §6.1,
  §6.2, §9 (replaced by a resolution function), §10 (splitter/binder HiZ rules vanish), §12
  (the golden mapping table). §7's delay table is untouched.
- **`ARCHITECTURE.md:359-368`** carries a *binding* equivalence criterion that names "the
  two-states-plus-HiZ value domain and multi-driver/tri-state resolution (§2, §9)" by
  reference. It must be re-anchored to the new §2/§9 **before** any of this lands. The
  process for doing so is written in the same clause: "Any divergence is a specified,
  documented change to `docs/simulation-semantics.md` first, never a silent behavioral
  difference."
- **`docs/batch-interface.md`** is a stability contract; Stage 1 changes §3.4 and §4.3 and
  Stage 4 changes §2. The `--value-model` flag plus a major bump is the honest route.
- **File format**: `FORMAT 3` (`Circuit.FORMAT_VERSION` is 2 at `src/jls/Circuit.java:102`).
  Per `docs/file-format.md:192-195` a writer emits the highest version whose features the
  file *uses*, so every existing circuit still writes `FORMAT 2` and still loads in old JLS.
  New payloads: `Output` strength pair (written only when non-default), net kind,
  `PullUp`/`PullDown` `SaveTags` rows, `Register` `init = unknown` and reset pins, `Memory`
  init digits `x`/`u`, per-circuit `init-model`.
- **One behavioural change to existing saved circuits**, deliberately not flagged away: a
  circuit that today silently resolves a bus conflict to "first driver in net order" will
  show X. That is the correction the whole program exists to make. CHANGELOG headline, not
  a compatibility flag.
- **New tests to write:** `LogicValueLawsTest` (exhaustive per-bit reference oracle over all
  5×5 operand pairs for each op), `ResolutionAlgebraTest` (permutation invariance —
  the property today's code fails), `Ieee1164TableTest` (against the standard's tables, once
  read), `StrengthLatticeTest`, `TwoStateCompatibilityTest` (the Stage 0 gate).

---

### 10. Unverified external claims in this document

Flagged so they are checked before anything is pinned by a test:

- IEEE 1364's VPI `s_vpi_vecval` aval/bval bit assignment (the encoding in §5.1).
- IEEE 1364's 8-level strength lattice, the independent `strength0`/`strength1` per driver,
  and the ambiguous-strength-range rules this design deliberately drops.
- IEEE 1164's nine values, its resolution table, its AND/OR/XOR tables and the exact
  U-stickiness rule, and the behaviour of `to_01`/`to_X01`/`is_X`.
- The claim that 1164 never *resolves to* `-` (the load-bearing justification for putting
  `-` in a separate specification type).
- EVCD's port-state character table and whether its two strength fields are the 0–7 Verilog
  levels — flagged unverified by `docs/standards-adoption/07-waveform-formats.md:662-667`
  and still unverified here.
