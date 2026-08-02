# 07 — DETERMINATION: native ternary and N-ary logic in JLS

**Re-derived under BRIEF §12 D10. This file REPLACES the prior version.** That
version reached its verdict by leading with *"zero mentions across 944 tracked
files"* and by invoking #212's demand gate against the maintainer's own proposal.
D10 strikes both by name. The survey measurements from all six angles stand and
are reused throughout; **the verdict below is derived from arithmetic —
maintainer-weeks, bytes, nanoseconds, plane counts, coverage headroom — and from
nothing else.**

Every HEAD claim carries `file:line`, re-verified this session at `b299d63` (code
unchanged since `36cbd37`). Where I contradict an angle, I say so and show the
check. Six full analyses read: `mvl-representation.md`, `mvl-semantics.md`,
`mvl-research-grounding.md`, `mvl-interop-refusals.md`, `mvl-p1-and-pedagogy.md`,
`mvl-adversary.md`.

---

## 0. VERDICT

**Do it. Radix 2, 3 and 4, natively, as a bounded four-stage program costing
17–25 maintainer-weeks on top of P1 — of which exactly one week must be spent
*during* P1 and the remaining 16–24 can be spent at any later time with no
penalty.**

The reason this is a "yes" where the prior version was a "no" is a single
arithmetic fact that no angle stated in this form:

> **P1's own value record already has the spare code points.**
> `record Word(int width, long a, long b, long u)` is three bit-planes = **8 code
> points per position**, and P1 uses **five** (`0 1 X Z U`,
> `docs/capability-roadmap/README.md:130-133`). A radix-*r* alphabet with full
> X/Z/U needs `ceil(log2(r+3))` planes: **r=3 → 6 codes → 3 planes. r=4 → 7 codes
> → 3 planes. r=5 → 8 codes → 3 planes.** Radix 3 and radix 4 with complete
> four-state modelling ride P1's record **with zero new fields, zero new bytes,
> zero new sealed arms, and zero change to the primitive-long lane.**

The fourth-plane cliff is at **radix 6** (`r+3 = 9 > 8`), and that is the
arithmetic reason to bound N. It is not a preference.

The second reason is that the one remaining cost — telling elements *which*
alphabet a port speaks — has a placement that costs radix-2 literally nothing:
**radix is a property of ports and nets, exactly as bit width already is.**
`docs/simulation-semantics.md:57-59` already says it: *"Bit width is a property of
elements and wire nets (`WireNet.getBits`), not of the BitSet, which is unbounded.
Reading code interprets a value at the reader's declared width."* Generalise that
sentence from *how many positions* to *how many positions, in what alphabet*, and
the value type never learns about radix at all.

**What this buys.** JLS becomes the only drawn, simulated, deterministic, offline
logic simulator in which a balanced-ternary datapath can be drawn, clocked,
probed, traced, tested by `-t` vectors, and lowered to binary-encoded Verilog that
an external ternary flow already consumes. That is a real research capability in
JLS's own idiom, and no existing tool occupies that step (§5).

**What it does not buy, stated plainly.** It does not make JLS a place to do MVL
*device* research. The field's contested question is device-level energy at
parity; its currency is HSPICE transients and power-delay product; and JLS models
neither energy nor real time — simulation time is *"a dimensionless non-negative
64-bit integer… nothing binds them to seconds"*
(`docs/simulation-semantics.md:24-30`), and there is no power model at all
(`docs/capability-roadmap/lf-05-fault-and-power.md:100-101`). Native ternary makes
JLS good at ternary **architecture**, and no change to the value type reaches the
device tier. Buy it knowing that.

---

## 1. THE PATH AND ITS COST

### 1.1 The four stages

| Stage | Content | Weeks | What ships | Basis for the estimate |
|---|---|---:|---|---|
| **0 — Reserve** *(must happen inside P1's Stage 0)* | Re-anchor `docs/simulation-semantics.md` §2 as an **alphabet-parameterised** statement; reopen recorded decision #221's equivalence criterion **once**, not twice; add `int radix()` (returns 2) to `Put`/`WireNet`; freeze `Word`'s field list in writing. | **~1** | Nothing user-visible. | Doc + governance motion + two accessors. The week is the #221 reopening at bus factor 1, not the code. |
| **1 — The port type system** | Radix on `Put`/`WireNet`; **validate** (not widen) in `makeNet`/`recheck`; radix check above the width check at the four `SimpleEditor` sites; `getBits()` policy over 89 call sites; fix `Util.convert`'s `return ""`; build the **K9 ratchet**. Zero N-ary elements ship. | **3–5** | Nothing user-visible. Every existing golden byte-identical. No drawable circuit can reach radix ≠ 2. | `makeNet`/`recheck` (`WireNet.java:97-165`, `:272-302`) are the *same two methods P1 already rewrites* for the resolution fold (`README.md` P1). The four editor sites are three lines each (`SimpleEditor.java:4015,4142,4247,4358`, all `overlapMessage = "Bits don't match"`, verified). K9 ratchet ≈ 2 days — `docs/virtual-hardware-parity.md:1903-1917` calls the palette assertion *"minutes of work"*. |
| **2 — The radix-3/4 kernel** | The alphabet-parameterised operator module `keystone-c-performance.md:512-517` already prescribes (*"write the truth tables once, in one place"*); min/max/complement/cyclic/literal over the three planes; the **lane-packed Kogge-Stone balanced-ternary adder** and its 200k-vector differential test; plane↔lane conversion; add `jls.core.*` to PIT `targetClasses` and re-baseline. | **4–6** | Nothing drawable yet. | P1's own "type + ops + tests" line is 2 weeks for the *binary* four-state case (`sweep-01-values-and-logic.md` V1). Ternary is ~4× the table surface and the prefix-carry adder is genuinely hard against the 80/82 PIT floors (`pom.xml:812-813`). |
| **3 — The element family** | ~8 new registered types: `MvlGate` (min/max/literal), `MvlNot` (3 negation modes), `MvlAdder`, `MvlMux` (= the T-gate), `MvlConstant`, `MvlDisplay`, `RadixBridge`, `MvlTruthTable`. **No radix attribute on any existing type.** | **6–9** | **The capability.** A drawable, simulable balanced-ternary datapath. | The project's *own* per-element rate: P2 prices its arithmetic family at *"~1.5 for the first element including the plumbing pattern, ~0.75 each after"* (`docs/capability-roadmap/README.md:293-297`). 1.5 + 7×0.75 = **6.75 weeks**, inside 6–9. |
| **4 — Interop, display, docs** | BET-lowered HDL export; VCD with a `$comment` radix manifest; `-t` grammar extension through `SigSim`'s existing token-rewrite pre-pass; a new stdout line shape behind a new flag; balanced `-0+` rendering; the normative doc rewrite. | **3–4** | The design leaves JLS. | Angle 4 sized the interop delta at ~100 lines plus a manifest emitter and three tests under the new-types design; the doc corpus edit and the goldens are the rest. |
| | **TOTAL** | **17–25** | | |

### 1.2 What it displaces

The committed roadmap totals **151–220 maintainer-weeks / 35–51 maintainer-months**
(`docs/capability-roadmap/README.md:1205-1209`). 17–25 weeks is:

- **the whole of P2's element-vocabulary program minus its memory-port half**
  (P2 is 22–32 weeks, `README.md:293`), or
- **about two-thirds of P1's keystone-B four-state core** (17–22 weeks,
  `README.md:215-220`), or
- **roughly the full semantics-preserving engine stack** that BRIEF §13 measures
  at 2.26× — the one that turns a 1.66 h structural boot into 44–46 min.

At bus factor 1 that is **four to six months of the only maintainer**. That is the
trade, stated once, honestly, and it is the strongest argument against — not
demand, not precedent.

### 1.3 Two scheduling facts that are not in the week count

**Stage 3 spans two releases, not one.** `jls.elem` is floored at 73.0/70.0/58.5
(`pom.xml:478-491`) against a measured 74.65/71.64/60.62. Angle 5's solve of
`(measured·T + c·N)/(T+N) ≥ floor` over the package's 30,724 instructions gives
**~3,900 new instructions at 60% new-code coverage before the floor trips ≈ 4–6
new element classes per release.** Eight types is therefore two release cycles even
at full effort, so the *user-visible* capability lands 9–12 calendar months after
Stage 1 starts. The remedy is not lowering the floor — `pom.xml:317-321` records
*"the floor only ever moves UP"*, and raises need three clean headless
canonical-JDK-25 runs with ≥2pt headroom (`pom.xml:806-812`). The remedy is
`test/jls/elem/*ModelTest`-style suites shipped with each element, which is already
the house pattern (`RegisterModelTest`, `MemoryModelTest`, `SubCircuitModelTest`).

**A new element type is ~550 lines of body, not ~65.** BRIEF §13's *"~65-line
registration tax across 12 files"* is verified — `git show --stat 38a0544`: 14
files, 1,188 insertions, of which `FieldExtend.java` is 486 and
`RegisterFile.java` is 569, leaving **133 lines of registration for two elements =
~66 each**. The registration tax is small; the element *body* is not. Eight types
≈ 4,400 lines of `jls.elem`, which is what makes the coverage arithmetic above
bind.

### 1.4 Scope recommendation, on cost grounds

**Ship radix 2, 3 and 4. Permit 5 at zero marginal storage cost. Refuse ≥ 6.
Refuse arbitrary N.**

| radix | alphabet with X/Z/U | planes | storage cost vs P1 | arithmetic cost |
|---:|---:|---:|---|---|
| 2 | 5 | 3 | **zero — it *is* P1's `Word`** | native |
| 3 | 6 | 3 | **zero** | one algorithm in one method: **9.79 ns/op** done right vs **178.32 ns/op** done naively (Angle 1, `Add3Swar.java`, 32 digits, verified against a per-digit reference on 200,000 random vector pairs) |
| 4 | 7 | 3 | **zero** | **essentially native** — a base-4 word *is* a binary word: **6.51 ns** vs **5.89 ns** for a 32-digit add (Angle 1) |
| 5 | 8 | 3 | **zero**, exactly saturating | no native carry chain; same technique as radix 3 |
| **6** | **9** | **4** | **a fourth plane = a second storage migration** | — |
| 16 | 19 | 5 | a fifth plane | — |

The refusal of radix ≥ 6 is arithmetic, and it is the *same* arithmetic
`keystone-c-performance.md:400-407` uses to reject the interleaved layout:
*"adding a third state plane means re-packing from 2 bits to 4 bits per position —
a second migration, which is the one thing §8 must avoid."* A variable plane count
in `Word` also destroys the primitive-long lane, which is the entire cost model
P1's 20–25% event-loop speedup rests on (`keystone-c-performance.md:426-435`).

**Correction to Angle 5**, which placed the cliff at radix 8. With X/Z/U in the
same code-point space the cliff is at **radix 6**. It changes no recommendation —
{2,3,4} is inside both — but the number in the type contract must be right.

---

## 2. THE DISTINCTION, RESTATED

| | **(a) Modelling MVL — this is P1** | **(b) Computational MVL — this is the question** |
|---|---|---|
| What the extra symbols mean | *epistemic*: unknown, undriven, uninitialised, weakly driven | *arithmetic*: base-3 or base-4 digits; balanced {−1,0,+1} |
| Instances | IEEE 1164 nine-value, Verilog 4-state, Kleene X-propagation | Setun, radix-3/4 ALUs, CNTFET ternary gates, MRCS/REBEL-2 |
| Radix | **stays 2** | **changes** |
| Status in JLS | **funded**: P1, 28–36 maintainer-weeks, unlocks 28 standards (`README.md:173-176`) | this document |
| Cost of the value type | the whole of P1's Stage 5 (12–16 weeks) | **zero extra** — rides P1's three planes |
| Cost of everything else | strength, resolution fold, `U`, 1164 projection | the port type system and the element library |

**A third row exists and is routinely miscited as evidence for (b).** BitNet
b1.58 ternary weights, ternary neural networks and TCAM are all **ternary-valued
data on binary hardware** — BitNet accelerators pack ternary weights into 8-bit
integers and use integer addition with zero-skipping (arXiv:2402.17764); TCAM's
third state is *don't-care*, physically two binary SRAM cells per bit. Both are
served by binary simulators and both are drawable in JLS today with 2-bit bundles.
Angle 3 and the adversary establish this independently and I adopt it: **do not
put ternary NNs in a roadmap document as a motivation for (b)**, because it will
be cited as settled later — the exact failure mode BRIEF §12 D8 records for the
revoked "orchestrate, never reimplement" stance.

**Which is the maintainer asking about?** The phrasing — *"ternary and N-ary logic
in addition to the standard binary"*, framed as research interest — reads as
**(b)**, and the brief instructs me to serve (b). I do. Three angles recommended
asking the maintainer to disambiguate before scheduling anything; I decline to
make that a gate, because under D10 the answer is not allowed to be *"ask first,
then maybe."* The answer is: **(b) is the question, here is the path, and here is
the fact that makes it cheap — P1 was going to build the machinery anyway.**

**The one substitution I refuse to make.** Kleene three-valued X-propagation is
genuinely "ternary logic" in the literature, and P1 delivers it. It is **not**
balanced ternary and it is not radix-3 arithmetic. Offering P1 as if it satisfied
this question would be a bait-and-switch a researcher would correctly resent, and
sycophancy is the named failure mode here. **P1 is a prerequisite, not a
substitute.**

---

## 3. THE REPRESENTATION DETERMINATION

### 3.1 The choice

> **One value type. `record Word(int width, long a, long b, long u)` — P1's,
> unchanged, field list frozen. Three bit-sliced planes carrying eight code points
> per position. Radix lives on the PORT and the NET, never in the value. The
> alphabet is a static property of the drawing, resolved at elaboration.**

This is the headline the task asked for, and it is stronger than any angle's
version: **bit-sliced planes unify X-states and radix into ONE change, and the
change is one P1 is already committed to making.** Radix 3 and 4 are not a
generalisation of `Word` — they are an *interpretation* of code points `Word`
already has and does not use.

Concretely, the three planes `(a, b, u)` encode a per-position code point
`c = a + 2b + 4u ∈ [0,8)`. P1 assigns five: `0 1 X Z U`. Radix 3 assigns six
(`0 1 2 X Z U`); radix 4 assigns seven; radix 5 saturates at eight. The
**assignment table** is one small class — exactly the *"write the truth tables
once, in one place"* module `keystone-c-performance.md:512-517` already requires so
that the event-driven and levelized engines cannot disagree (*"Write them twice,
six months apart, against two different data layouts, and the two engines will
disagree, and the `#202` differential oracle will catch it late and expensively"*).

### 3.2 The radix-2 fast path costs nothing, and the proof is stronger than the measurement

Angle 1 *measured* the fast path at **7.11 ns/op through the sealed switch vs 7.20
ns direct vs 7.01 ns control, 40.0 B/op in all three** — indistinguishable
(`MvlBench2.java`, OpenJDK 25.0.3, keystone-c §5 access pattern, w=32, 2M iters, 5
warm-ups, best of 11, 64-entry operand pool defeating hoisting, allocation via
`ThreadMXBean.getThreadAllocatedBytes`). That measured a *sibling-record* design.

Under the design determined here the argument does not need the measurement:
**there is no second value type to profile against.** `Word` is byte-for-byte what
P1 ships. A radix-2 circuit executes the identical machine code before and after
MVL exists. The cost is not "measured at zero"; it is **structurally zero**.

Three corollaries, each an open problem in one of the angles, now closed:

1. **Angle 1's ~1.4 ns/op type-profile pollution in mixed circuits disappears.**
   That cost came from one compiled method seeing two value shapes. There is one
   shape. Dispatch instead happens at the *element* — `AndGate.computeOutput` does
   `value.and(inVal)` (`src/jls/elem/AndGate.java:72`, verified) and
   `MvlGate.computeOutput` does a min over the same planes — so each call site
   stays monomorphic by construction, even inside a mixed-radix drawing.
2. **Angle 1's tagged-union rejection is preserved and generalised.** Its measured
   penalty — **9.24 vs 7.01 ns/op (+32%) and +16 bytes per value in a *pure binary*
   circuit** — is precisely the cost of the fourth plane the radix-≤5 encoding
   avoids. That measurement is the reason `Word` must not gain a `radix` field
   *or* a fourth plane, and it should be quoted in the frozen-field-list paragraph
   so a future contributor reads the number rather than the preference.
3. **The change-detection problem evaporates.** `keystone-c-performance.md:740`
   budgets a week for width-sensitive `equals` touching
   `Output.propagate:139-145`, `Gate.react`, `Register.react`, `Mux.react`,
   `TriState.react`. Angle 1 predicted a *second* pass for radix-sensitive
   `equals`. Under port-radix there is no second pass: `Output.propagate`'s check
   is `currentValue.equals(value)` (`src/jls/elem/Output.java:143`, verified) and
   compares two values **on the same port**, hence the same radix, always —
   guaranteed by the connection rule in §3.4. **One pass, not two.**

### 3.3 Composition with the primitive-long lane and the levelized plane

- **Primitive-long lane: unchanged, literally.** The brief's *"primitive-long value
  lane for widths ≤ 64, retaining a BitSet lane above"* is at HEAD P1's
  `Word`(≤64)/`Wide`(>64) split; there is no separate long-lane project. Neither
  arm changes. Interned constants per width, cached `hashCode`,
  identity-then-hash comparison — every optimisation aimed at `Word` applies
  unchanged, at every radix.
- **Digit count occupies the lane, and it is small.** A 32-trit balanced-ternary
  machine — larger than Setun's 18 trits — is 32 positions in a 64-position
  `Word`. `keystone-c-performance.md:426-429` records that *every* width in *every*
  `.jls` in this repository is ≤ 32. Nothing about radix 3 or 4 pushes a value into
  `Wide`.
- **Levelized plane arrays: unchanged shape.** `keystone-c` §6.3's
  `long[] a; long[] b; long[] u` indexed by node id (`:508`) is the same three
  arrays at every radix ≤ 5. The levelizer needs the region's radix at compile time
  and must refuse a levelized region that mixes radices — and that falls out for
  free, because radix cannot cross a net. **State it so it is not rediscovered.**

### 3.4 The connection rule (the one place radix is *not* like width)

Width is **negotiable** in JLS: `WireNet.setBits` takes a `Math.max`
(`src/jls/elem/WireNet.java:232`, verified), `recheck` recomputes `bits` as
`Math.max(p.getBits(),bits)` over ends (`:280`, verified), and `bits == 0` means
*"not connected to any elements yet"* (`:236-238`) so a `Constant` can adapt
(`docs/simulation-semantics.md` §6.2).

**Radix has no arbitrary value and no max.** `recheck` must **validate**, not
widen. Enforcement, in four layers:

1. **Edit time — the primary gate.** `SimpleEditor` refuses width mismatches at
   four sites, all `overlapMessage = "Bits don't match"`:
   `src/jls/edit/SimpleEditor.java:4015` (end–end), `:4142` (end–wire), `:4247`
   (end–put), `:4358` (put–put) — all four verified at HEAD. Each gains a sibling
   check **above** it (above, so a radix mismatch is never mis-reported as a width
   mismatch) and **unconditionally**, with no `> 0` escape hatch, because radix is
   always known. Message: `"Radix doesn't match: base-3 cannot drive base-2"`.
2. **Load time** — `WireNet.recheck` (`:272-302`), a load error naming both element
   ids and both radices, in the style `docs/file-format.md` §5 already requires.
3. **Simulation start** — an `IllegalStateException` in `initSimulation`. By then
   an invalid radix graph is a bug in JLS, not in the drawing.
4. **`RadixBridge`** — a drawable, named converter is the *sanctioned* crossing,
   the same pattern `Splitter`/`Binder` provide for width. **It ships in the same
   release as the check or users are stuck.**

**No coercion is defined, ever.** A base-3 digit `2` has no image in `{0,1}`.
Folding it repeats exactly the mistake the roadmap is currently *removing* —
`NetlistImporter.connectConstant` folds Yosys `x` to 0 and reports the loss through
a field literally named `coercedX` (`src/jls/hdl/imp/ImportSummary.java:28,59,97-100`,
listed in `README.md` among *"six structural workarounds, each deleted by the
change"*). **The program must not delete one silent coercion and add another.**

### 3.5 `getBits()` — 89 call sites, and the loud shim

`getBits()` has **89 call sites in `src/`** (heaviest: `HdlExporter` 15,
`SimpleEditor` 13, `BatchSimulator` 6, `BatchTracePrinter` 5). Almost all mean *how
many binary bits do I write, draw or mask*. Silently redefining it as "digits"
would mis-size VCD vectors, HDL port declarations and memory words **with zero
compile errors** — the worst possible outcome.

Determination: **add `getRadix()` (2 forever) and `getDigits()`; keep `getBits()`
as a deliberately loud shim that throws `IllegalStateException` when radix ≠ 2.**
That converts 89 silent mis-sizings into 89 loud failures that only fire on an
N-ary circuit — a bounded, mechanical, testable migration. Sites that genuinely
are binary (`HdlExporter`, `BatchSimulator.vcdValue`) get an explicit binary-only
guard, which is honest, because Verilog and VCD *are* binary.

### 3.6 Balanced ternary

**A port-level interpretation, not a storage choice.** Store codes `0/1/2`;
interpret as `−1/0/+1`. This is precedent-following, not invention:
`BitSetUtils.ToStringSigned(bs, bits)` (`src/jls/BitSetUtils.java:103-118`,
verified — test the top bit, `temp.flip(0,bits)`, add one) already reads a stored
*unsigned* BitSet as two's complement at the caller's declared width, and
`docs/simulation-semantics.md:57-59` states it as the general rule. Balancedness is
the radix-N generalisation of that sentence.

Carry it as an `Encoding` enum (`UNSIGNED` / `BALANCED` / `TWOS_COMPLEMENT`) on the
**port**, alongside radix. Consequences worth having:

- **Negation is one plane op** (swap codes 0↔2) where binary needs a full
  carry-propagate two's complement. Demonstrable in a drawn circuit.
- **Sign-extension is zero-extension** — leading digits are already 0.
- **No most-negative-number pathology**: *n* balanced trits span a symmetric range,
  so a balanced comparator is *strictly simpler* than the binary one, and the
  pathology is revealed as an artifact of representation rather than of arithmetic.
- **Division by 3 with round-to-nearest is truncation** — exact. This is what the
  Setun literature is actually about, and Knuth's *"the prettiest number system of
  all"* (TAOCP vol. 2 §4.1) is about this property, not about radix economy.
- **`ToStringSigned` has no meaning here** and must not be reached. Sign is the
  leading non-zero trit; there is no sign bit. A balanced formatter is new code.
- **Balanced digit sets exist only for odd radix.** Radix 4 balanced is either
  asymmetric `{−2..1}` or redundant `{−2..+2}`. Refuse it with a named message.

**Honest framing obligation for any docs page.** Setun (Moscow State University,
1958; ~50 machines to 1965) was halted for administrative/political reasons, and
the radix-economy argument descends from a 1950 vacuum-tube ring-counter cost model
whose own source disclaimed itself (*"These assumptions are, of course, only
approximately valid, and the choice of 2 as a radix is frequently justified on more
complete analysis"*). The honest version is **"ternary was never given a fair
industrial trial in 1965, AND sixty years of device work has not produced a win
either"** — both halves, or it is the blog-post version.

---

## 4. THE SEMANTICS DETERMINATION

### 4.1 Default operator family: **min/max lattice with complement negation**

**Chosen on K9 grounds, not aesthetics.** It is the only family that collapses
*exactly* to today's binary behaviour at N=2:

- `min(a,b)` at N=2 **is** `value.and(inVal)` (`src/jls/elem/AndGate.java:64-75`,
  verified: `BitSet value = new BitSet(bits); value.set(0,bits); … value.and(inVal)`).
- `max(a,b)` at N=2 is bitwise OR (`OrGate.java:65-75`).
- `(N−1)−d` at N=2 is bitwise NOT — `notQOut.flip(0,bits)`
  (`src/jls/elem/Register.java:805`, verified).

Łukasiewicz's strong conjunction `max(0,a+b−1)` also collapses at N=2 but its
implication does not collapse to `max(¬a,b)` for N>2. Post's cyclic negation
`(d+1) mod N` collapses at N=2 only coincidentally and is **not an involution** at
N=3. Min/max is also the only family in which the algebra a first-year student
already learned — De Morgan, distributivity, absorption, idempotence — survives.

Offer Łukasiewicz, Post (cyclic negation) and Allen–Givone literals as **selectable
modes on the new element types**, never as attributes on existing ones (§4.4).

**Two documented hazards, mandatory, because they are pedagogic traps:**

1. **NAND is NOT functionally complete for N>2.** The Sheffer-stroke result does
   not generalise: min/max/complement generate only the monotone-plus-complement
   fragment, and completeness needs a non-monotone operator — Post's theorem: max
   plus cyclic negation is complete for N-valued logic
   ([SEP, *Many-Valued Logic*](https://plato.stanford.edu/entries/logic-manyvalued/)).
   Every help page saying *"NAND is universal"* becomes **false** the moment N>2 and
   must be conditioned. This is the reason the Post family must be offered at all.
2. **XOR loses parity.** Three inequivalent generalisations exist: sum mod N (the
   recommended default, since XOR *is* addition-without-carry in binary, so this
   preserves the identity and gives GF(N) for prime N); a not-equal indicator; and
   `|a−b|`. Under all three, a chained XOR over N>2 is not a parity function.
   `XorGate.computeOutput` is `value.xor(inVal)` (`src/jls/elem/XorGate.java:71-81`).

**The T-gate is `Mux` with an N-ary selector.** The Mouftah–Jordan T-gate — the
universal MVL primitive, by Shannon expansion — is exactly
`src/jls/elem/Mux.java:519-566` (`ToInt(bw)` at `:530`, then
`inputs.get(which+1)`) with a base-R selector and N^k data ports. **The universal
MVL primitive costs one new element class, not a family.** This is the most
economical single finding in the whole study and it comes from Angle 2.

### 4.2 The 27 `react()` implementations, classified

Verified at HEAD: `grep -rln "public void react(" src/jls/` returns **exactly 27
files**; `ElementRegistry.ALL` lists **35 types** (`ElementRegistry.java:38-77`, the
roadmap's 33 is stale as BRIEF §13 says). Separately there are **9 concrete
`computeOutput` overrides** plus one abstract declaration at `Gate.java:663` —
*correction to Angle 5, which counted 10 by including the abstract.* Total
value-computing methods at HEAD: **36** (matching the adversary, not Angle 5).

**GENERALIZES — moves values, never interprets them (13).** No logic change; the
alphabet flows through untouched.

`Gate` (`:695` dispatch shell), `DelayGate`, `Extend` (`:206`), `InputPin`
(`:198`), `OutputPin` (`:195`), `JumpStart` (`:479`), `JumpEnd` (`:407`),
`SubCircuit` (`:621`, plus a new edit-time radix port check where today only width
is checked), `RegisterFile` (`:504`), `Memory` (`:1335`, data path only), `TriState`
(`:473`, data path only), `Stop` (`:147`), `Pause` (`:167`). `LogicElement`
(`:534`) is the base sentinel that throws — listed so the count reconciles to 27.

**Special mention — `Splitter` (`:204-231`) and `Binder` (`:233-267`) generalise
unchanged**, because they extract *index ranges*, and a digit-range extract is a
plane-wise shift/mask at any radix. That is independent confirmation the plane
encoding is the right shape. Their one new rule is a refusal: **sub-digit ranges
are rejected**, because a bit-level range on a radix-3 net would expose the
encoding — the abstraction leak this design accepts only while it stays named.

**NEEDS A NAMED CHOICE (13).** Each is a line in `docs/simulation-semantics.md`
before it is a line of code.

| element | anchor | the choice |
|---|---|---|
| `AndGate` | `:64-75` | min (default) |
| `OrGate` | `:65-75` | max |
| `NotGate` | `:65-74` | complement `(N−1)−d` default; cyclic `+1`; diminish `−1` |
| `NandGate` / `NorGate` | `:66-78`, `:68-79` | inherited — **plus the "NAND is not universal" hazard** |
| `XorGate` | `:71-81` | sum mod N (default); not-equal; `\|a−b\|` — **loses parity** |
| `Adder` | `:381-425` | balanced vs unbalanced; carry-port width changes by mode. Today carry is literally the bit at index `bits`: `carry.set(0, sum.get(bits)); sum.clear(bits)` (`:418-421`, verified) |
| `Mux` | `:519-566` | = the T-gate; selector via `ToInt` (`:530`) generalises to base-R digit-string→int with N^k inputs |
| `Decoder` | `:459-497` | **the first mixed-radix element**: radix-N input, *binary* one-hot outputs. Guard the N^k blow-up (3^16 = 43M lines) |
| `Register` | `:747` | what `notQ` means — `notQOut.flip(0,bits)` (`:805`) has no canonical base-3 analogue |
| `ShiftRegister` | `:660` | `ArithmeticRight` copies `input.get(bits-1)` as a sign bit (`:701`) — does not generalise |
| `TruthTable` | `:1400` | cell code `2` **already means don't-care** (`:79`, verified: *"0, 1 or 2 (don't care)"*) and collides head-on with radix-3 data. Fix by moving don't-care to a sentinel outside the digit range — which is *also* sweep-01 V7's don't-care/don't-know separation. **One fix, two programs.** |
| `StateMachine` | `:722` | binary clock plus radix-N transition literals |
| `Constant` | `:474` | truncate-to-width becomes reduce mod N^digits — the exact generalisation of `simulation-semantics.md` §6.2's `value mod 2^bits` |
| `Display` | `:387` | base 3/4 and balanced glyphs — cheapest, highest-visibility item; and `BitSetUtils.ToString(bs, radix)` (`:83-92`, verified) is **already radix-general** via `BigInteger.toString(radix)` |
| `SigSim` | `:214` | its `react` throws; all the work is in the `-t` grammar |

**REFUSES — element level (exactly 1).** `Clock` (`src/jls/elem/Clock.java:404`,
alternating one bit at `:415-425`). Message: *"Clock drives a binary square wave; a
radix-3 clock is not defined. Use a binary clock and radix-3 data."* Record the
threshold-crossing generalisation (declare threshold *t*; positive edge =
`remembered < t ≤ current`; collapses exactly at N=2, t=1) as
**specified-but-unimplemented**, so the refusal is scoped rather than absolute.
Real MVL sequential hardware clocks on a binary clock and stores a ternary datum,
so this refusal matches the hardware, not just the code.

**REFUSES — port level (8 ports across 5 elements).** `Register.C`
(`:765,776,783-790`), `StateMachine.clock` (`:743-753`), `Memory.CS/OE/WE/clock`
(`:1344-1360,1370-1375`), `TriState.control` (`:482-487`), plus
`ShiftRegister.amount` and `Decoder`'s output side. Message: *"`<element>.<port>` is
an enable, not a value: it accepts radix-2 only. Attached net is radix-`<N>`."*

**The consequence nobody can design around: MIXED-RADIX IS THE NORMAL CASE.**
Because the clock stays binary, *every* sequential radix-N design has binary clock
nets and radix-N data nets in one drawing. There is no "pure ternary"
simplification at any point. Under port-radix this is free — it is exactly how
per-port width already works — which is a third independent reason radix belongs
on the port.

### 4.3 The one real algorithm, and the trap next to it

A 32-digit balanced-ternary add, **written naively** (extract digit from planes,
add, compare against 3, re-insert): **178.32 ns/op** — 30× radix-2, roughly
doubling a 318 ns event by itself (Angle 1). Angle 5 independently measured
**365–400 ns/op** for a digit-serial variant and **23.1–23.8 ns/op** for a
bit-parallel plane carry loop; two independent implementations on two encodings
agree the naive/optimised gap is **16–30×**.

**Written right** — hold digits 4 bits per lane, 16 per `long`, so lane-wise `a+b`
cannot overflow (2+2+1 = 5) and digit sums are *one native `long` add*; then
resolve carries by Kogge-Stone parallel prefix over generate (`lane ≥ 3`) and
propagate (`lane == 2`) masks in 4 steps for 16 lanes: **9.79 ns/op**, plus ~7 ns
for plane↔lane conversion ≈ **17 ns**, ~2.9× radix-2 and far under one event.
Correctness: 200,000 random 16-digit vector pairs agree with an independent
per-digit reference (`Add3Swar.java`).

**Engine effect, with the assumptions stated.** `keystone-c`'s event census
(`:124-132`) records `Adder` at **108,025 of 2,331,793 fired events = 4.63%** —
*this closes Angle 1's "the adder fraction is unmeasured" open question; it was
measured, in keystone C, and nobody looked.* All `react()` bodies together are
**4.9% of loop time** (`:190-194`). Taking the adder body as ~3× the average react
body: the prefix-carry algorithm costs **~+1.8% of loop time**; the naive one costs
**~+40%, a 1.4× whole-engine slowdown for a ternary machine.** Binary circuits pay
zero either way, because dispatch is per element class.

**Therefore the algorithm belongs in `docs/simulation-semantics.md`, not in a code
comment**, together with the 200k-vector differential test as the model — a slow
reference implementation the fast one is diffed against also gives PIT something to
kill against the 80/82 floors (`pom.xml:812-813`).

**Platform caveat that must be recorded, not asserted.** The lane predicates use
`Long.compress`/`Long.expand` (Java 19+; available since `pom.xml` sets
`maven.compiler.release 25`). These compile to `PEXT`/`PDEP` on x86-64 BMI2, which
are **microcoded and slower than a software fallback on AMD Zen 1, Zen+, Zen 2 and
Hygon Dhyana** ([has_fast_pdep](https://github.com/seancroach/has_fast_pdep),
[TalkChess measurements](https://talkchess.com/viewtopic.php?t=72538)). JLS ships
one offline jar to unknown student laptops; a 2019 Ryzen is a plausible target.
Either build the predicates with pure shift/mask at a measured cost, or accept and
document a platform-dependent constant. **Unmeasured on affected hardware —
flagged as an open question.**

### 4.4 API shape: new element TYPES, never attributes on existing types

This is the load-bearing structural decision and it buys four things at once.

| what it buys | mechanism |
|---|---|
| **HDL export classifies correctly for free** | `HdlExporter` classifies by `EXPORTED.contains(el.getClass())` — **exact-class** membership over a 22-class `Set.of` (`src/jls/hdl/HdlExporter.java:422-428`, `:444-445`, verified), with the single `throw new HdlExportException` at `:194`. A new class is classified by policy automatically. A radix *attribute* would make `EXPORTED.contains(Adder.class)` true, the element would reach `buildStatement`, and both emitters would emit `assign sum = a + b;` — valid Verilog computing the wrong thing. **Fails open.** |
| **Batch stdout stays silent for free** | `docs/batch-interface.md` §3.2's watched-element whitelist is three classes (`Register`, `Memory`, `OutputPin`) and is *part of the contract*. New types are outside it, so no `-b` output changes. |
| **The save format stays at FORMAT 2** | `docs/file-format.md:463-472`, verified: *"Writers SHOULD prefer a version bump over an 'ignorable' attribute whenever dropping the attribute would change simulation behavior"*, with `Memory`'s `initrle` and `sync` named as the bad class. An old reader silently ignoring `int radix 3` loads **a circuit that computes different numbers** — squarely that class. But an unknown **tag** is a loud hard error (`:227-230`) and a new element type costs **zero format version** (BRIEF §13, verified above). |
| **The coverage gate is satisfiable** | Angle 5's measurement: a radix control in all 27 element dialogs is 27 × ~20 = **540 lines at ~0% coverage against a 610-line bundle LINE budget = 89% of the entire remaining headroom** (`pom.xml:332-335` records headless 56.22/54.70/52.53 against floors 54.5/53.5/50.5 at `:357-372`). The floors cannot be lowered (`pom.xml:317-321`). **The radix-attribute design does not fit the CI configuration as written** — it is not merely against a principle. |

**This overrides Angle 2's `family`-attribute-on-existing-types recommendation.**
Angle 2 reached it by minimising the registration tax, which is the right objective
against the wrong constraint: the tax is ~66 lines, and the *dialog coverage budget*
is what binds. Angle 4's R1 and Angle 5 reach the opposite conclusion from
independent evidence, and they are right.

### 4.5 The palette, and K9's missing mechanism

`Palette.ENTRIES` holds **32 rows** (`src/jls/edit/Palette.java:123-188`, counted at
HEAD) across 8 declared `Group`s (`:36-60`), against 35 registry types, with
`PaletteContractTest` making a registered type without a palette row a build
failure.

Determination: **add a ninth `Group` that is present in the table (so
`PaletteContractTest` stays green) but rendered only when the circuit radix ≠ 2** —
one visibility predicate on `PaletteEntry`, filtered in
`SimpleEditor.makeElements`. Then **assert the default-view palette is exactly 32
buttons.**

That assertion is the K9 ratchet, which does not exist:
`docs/virtual-hardware-parity.md:1903-1917` — *"The highest-ranked criterion in this
document is a sentence… Until that test exists, K9 is aspiration, and this document
says so."* Shipping the ratchet *with* the gated group makes the ratchet the
evidence. **It must precede any MVL commit** and it is ~2 days: a palette-size
assertion, a dialog-component-name assertion using the existing
`docs/component-naming.md` `setName` convention, and the existing startup/per-edit
numbers turned into a test.

**And one live defect to fix first, MVL or not.** `Util.convert` returns the empty
string for any base outside {2,10,16} (`src/jls/Util.java:337`, verified:
`return ""; // shouldn't happen`), while `Constant` and `Display` persist an
unvalidated `int base` attribute. **`int base 3` loads today with no diagnostic and
renders a `Constant` as nothing on the drawing.** That contradicts
`docs/file-format.md` §5's rule that elements reject out-of-range values during load
with a diagnostic. Fix it independently — and **do not reuse or overload `base` for
radix**, because that would silently change the simulation meaning of every existing
file carrying it.

---

## 5. THE REFUSAL BOUNDARY, AS A CONTRACT

**One correction up front, and it is the most consequential single change from the
prior version of this file.** Angles 3 and 4 both concluded that an N-ary circuit
*"can never leave JLS"* and that HDL export must refuse permanently. That is wrong,
and the brief was right to flag the objection as weaker than stated.

**Every ternary design fabricated on a commercial process since Setun is
binary-encoded ternary.** The Tiny Tapeout 03 balanced-ternary calculator states
its own encoding (verified by direct fetch of
`github.com/aiunderstand/tt03-balanced-ternary-calculator`): *"Binary Encoded
Ternary (BET), 1-trit (3 values) encoded in 2-bits"*, `−1 = 2'b01`, `0 = 2'b11`,
`+1 = 2'b10`, `2'b00` illegal, *"since current CMOS processes can only construct
binary transistors."* Its flow is MRCS → **Verilog** → Vivado testbench → FPGA →
OpenLane → HSPICE. And **MRCS is GPLv3 and emits Verilog for ternary designs** —
Bos, *Beyond 0 and 1: A mixed radix design and verification workflow for modern
ternary computers*, Univ. of South-Eastern Norway, 2024
([USN Open Archive 11250/3127984](https://openarchive.usn.no/usn-xmlui/handle/11250/3127984)).

Therefore: **`RadixBridge` applied to the whole circuit *is* an HDL lowering, and it
is exactly the lowering the field itself uses.** An N-ary JLS circuit exports as
ordinary binary Verilog over a documented BET encoding, runs in Vivado or Icarus,
and can be diffed against an MRCS-generated reference for the same design. **An
external oracle exists.** It is *manual* — a human authors the design twice — so it
is weaker than the `#202` differential golden, and I state that limit rather than
overclaim. But "no consumer at all" is not true, and the same lowering makes VCD
honest, which demotes Angle 1's `$var real` proposal from primary to fallback.

| surface | contract | anchor / cost |
|---|---|---|
| **Verilog / VHDL export** | **LOWER, do not refuse.** Emit the BET encoding with a header comment declaring radix, digits, code assignment and illegal codes. Refuse only *native* N-ary HDL, which exists in neither language. `HdlExporter`'s message must split *"does not support **yet**"* (Memory, RegisterFile, FieldExtend, SubCircuit — genuine increments) from *"cannot be represented in `<language>`"* — the current single word "yet" promises a future that cannot arrive for a trit. | `HdlExporter.java:175-194`, `:422-428`, `:444-445`; both emitters consume one model (`VhdlEmitter.java:18-20`) so the policy is language-independent. Amend both emitters' class docs and the VHDL generated-header disclaimer, which stay true but become incomplete |
| **VCD** | **ENCODE AND REPORT, do not refuse.** Packed binary through the existing `b<binary>` path, plus a `$comment` radix manifest, plus per-digit `$var`s behind a new flag, plus a stderr loss count. State in the profile that **an N-ary VCD is not self-describing**: a consumer ignoring `$comment` sees a binary bus and cannot tell. House precedent is *coerce, count, report by name* (`ImportSummary.coercedX`), which governs here rather than *"refuse rather than mis-load"* — that rule governs **reading** untrusted input; VCD is **writing** our own output into a weaker alphabet. | `BatchSimulator.vcdValue:538-555`; `docs/batch-interface.md:295` (*"`x` never appears"*); IEEE 1364 §18's alphabet is `0/1/x/z` per bit. Do **not** use `$var real`: it loses digit structure, cannot express HiZ, is inexact above 2^53, and breaks the `$var wire` invariant the structural checker matches |
| **The value-domain golden** | **DO NOT reopen. SCOPE.** `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ` asserts literally `assertFalse(line.contains("x"), "JLS values are 0/1/z only: " + line)` (`test/jls/VcdExportGoldenTest.java:321`, verified) over **two committed constant strings**, not over emitter output — so **computational MVL does not break it at all. P1 does.** Add a sibling `…ForNaryCircuits`. Any *rename* is itself a documented-decision change, since `docs/simulation-semantics.md:44-49` cites the test **by name**. | verified at HEAD; this is the cheapest available K9 evidence and must stay byte-identical |
| **Batch stdout** | **SILENT by default** (the §3.2 three-class whitelist excludes new types); N-ary rendering behind a **new flag** — §6's only compatible escape hatch. **Never modify or insert a line**: a `)$`-anchored regex or any diff-based grader breaks. `BitSetUtils.toDisplay` hardcodes `"0x… (n unsigned, n signed)"` (`:237-245`, verified); hex is meaningless for base 3 and `ToStringSigned` (`:103-118`) is meaningless for balanced ternary. | those exact strings are the autograder contract, `docs/batch-interface.md` §3.4 |
| **`-t` test vectors** | **EXTEND, additively.** `SigSim.initSim` already rewrites every `-?0[xX][0-9a-fA-F]+` token to decimal *before* parsing (`src/jls/elem/SigSim.java:48-64`, verified) — the right place for a `0t` balanced literal, ~15 lines. **And fix a live defect found by Angle 4:** the range check is `value.bitLength() > bits` (`:106-119`, verified), which for a 3-trit pin (bits=6) admits everything to 63 against a legal 0–26 — **2.4× over-permissive, silently**. Replace with a digit-count check. The negative branch `v + 2^bits` (`:120-124`) is two's complement and is **wrong** for balanced ternary; replace or refuse. | `-t` is *input*; accepting a strictly larger token set cannot invalidate any file valid today. Caveat: §2.4 makes parse errors fatal, so a script asserting exit 1 on `0t201` starts passing — CHANGELOG, not a blocker |
| **Yosys import** | **REFUSE at the CLI/dialog only, ~5 lines.** The importer is closed by construction: `NetlistImporter.mapCell` is a closed switch over `$not/$and/$or/$xor/$mux` with a diagnostic default (`:233-258`). There is no path from a netlist to an N-ary element. **Do not add a `CellValidator` row** — no ternary cell exists to reject. | Yosys has no ternary cell |
| **Save format** | **FORMAT stays 2.** New element types cost zero format version; unknown tags are loud hard errors (`docs/file-format.md:227-230`). A `radix` *attribute* would require FORMAT 3 under `:463-472`. This is the format argument for §4.4. | |
| **N-ary net → binary port** | **REFUSE at edit time**, all four `SimpleEditor` sites, unconditionally, above the width check. `RadixBridge` is the sanctioned crossing and ships in the same release. | §3.4 |
| **Multi-driver on N-ary nets** | **REFUSE in increment 1**, with the deferred function *specified* so the refusal is a scheduling decision rather than a semantic one: highest strength class wins; within it resolve by **min** over digits; equal-strength disagreement → **X**. Min because it is the wired-AND generalisation, it is commutative/associative/idempotent (so it *is* a resolution function in VHDL's sense, unlike today's net-order scan), and it degenerates to today's behaviour for one active driver. **Blocked on P1**, because the disagreement case needs X. | `docs/simulation-semantics.md` §9; `WireNet.propagate:443-484`; sweep-01 row #25 |
| **Device-level MVL** | **REFUSE the claim, permanently and in writing.** JLS is word-level — a 32-bit `Adder` is one element with one `react()` (BRIEF §1; `Adder.java:381`) — and models neither energy nor real time. CNTFET multi-threshold inverters, memristive cells and Vdd/2 levels are SPICE, and `grand-architecture.md` §9 excludes analog. Any MVL docs page must say so in its own words, or the feature is what the roadmap calls **conformance theatre** — and it would be the second self-documenting lie after `VhdlEmitter`'s nine-value disclaimer. | `lf-05-fault-and-power.md:100-101`; `simulation-semantics.md:24-30` |
| **Radix ≥ 6, and arbitrary N** | **REFUSE on arithmetic.** `ceil(log2(r+3)) > 3` at r ≥ 6 = a fourth plane = a second storage migration, and a variable plane count in `Word` destroys the primitive-long lane. State the ceiling in the type contract, not in a comment. | §1.4 |
| **Ternary NNs / TCAM as motivation** | **REFUSE the citation.** Both are ternary-valued data on binary hardware. Correct the record once, with the citation, so it cannot enter a roadmap document and be treated as settled later (BRIEF §12 D8). | §2 |

---

## 6. SEQUENCING

**Verdict: Stage 0 goes *inside* P1. Stages 1–4 go strictly *after* P1. The serial
penalty is ~1 week if Stage 0 is taken, and ~2–3 weeks plus a second governance
reopening if it is not.**

This resolves the split between Angle 1 (*"decide now during P1 or pay a second
migration"*) and Angle 2 (*"sequence strictly after P1"*). Both were right under
different representation premises; **port-radix is the premise that reconciles
them**, because under it there is *no* sealed-`permits` sweep and *no* second pass
over the 27 `react()` bodies. What is paid twice if Stage 0 is skipped is **not
engineering. It is governance.**

### 6.1 What is genuinely paid twice

| item | paid twice? | why |
|---|---|---|
| The 27 `react()` bodies | **No** | Port-radix does not change the value type; existing elements never learn about radix |
| The 94 `new BitSet(` sites in `src/jls/elem/` | **No** | Same reason |
| The five change-detection sites `keystone-c:740` budgets a week for | **No** | §3.2 corollary 3 — radix cannot cross a net, so `equals` always compares same-radix values |
| The golden re-derivation (2–3 wk in P1's own decomposition) | **No** | Binary goldens stay byte-identical; N-ary goldens are new files |
| **Recorded decision #221's equivalence criterion** | **YES** | `ARCHITECTURE.md:359-368` binds any future strategy to *"the two-states-plus-HiZ value domain and multi-driver/tri-state resolution (§2, §9)"* and requires *"a specified, documented change to `docs/simulation-semantics.md` first, never a silent behavioral difference."* P1 must reopen it. MVL must reopen it again — **unless §2 is re-anchored once, in alphabet-parameterised language.** At bus factor 1 a documented reopening is a real, non-trivial motion |
| **`docs/simulation-semantics.md` §2** | **YES**, same reason | It is normative and pinned by name |
| Two JaCoCo/PIT re-baselines | **YES**, ~0.3 wk each | Each needs three clean headless canonical-JDK-25 runs with ≥2pt headroom (`pom.xml:806-812`) |

### 6.2 The one sentence that removes the double payment

Re-anchor `docs/simulation-semantics.md` §2 — **during P1, inside P1's own motion**
— to read, in substance:

> The value domain is a **per-position alphabet**. The shipped alphabet is
> `{0, 1, X, Z, U}` at radix 2. **Radix is a property of ports and wire nets**,
> like bit width, whose only accepted value today is 2. The kernel encoding is
> three bit-planes = eight code points per position; alphabets requiring more than
> eight code points are outside the shipped encoding.

That sentence is **better writing than the two-states-plus-HiZ sentence even if
radix stays 2 forever**, because P1 is changing that sentence anyway and this
version is the one that does not have to be changed again. It costs a paragraph.

### 6.3 Hard dependencies (real, not preference)

1. **N-ary multi-driver resolution reads X, which only P1 creates.** Genuine
   blocking dependency in D10's own terms ("X reads data only Y creates").
2. **Radix 3 in a three-plane encoding needs the code points P1's five leave
   spare.** Before P1 there is no X, so the spare code points have no meaning and
   an "invalid digit" concept would have to be invented that P1 then absorbs —
   the second migration `keystone-c-performance.md:408-419` exists to prevent.
3. **Three format records need changing for *both* programs**: `TruthTable`'s
   `int[][]` cells where `2` already means don't-care (`TruthTable.java:79`);
   `Memory`'s `init`/`initrle` hex grammar, already flagged as *"the one hard
   case"* for four-state (`keystone-b-migration.md:220`, the `hasNextBigInteger`
   wall); and the `-t` grammar. **One format-version conversation, not two. Have
   it once.**
4. **`#77` before P1's Stage 5** — `src/jls/core/` today holds only geometry (8
   files, 665 lines), and `AMENDMENT.md:834-836` already moved #77 onto the
   critical path.

### 6.4 What it would cost to do MVL *first* (D10 rule 5 requires this answer)

Roughly **+8 to +12 weeks and a worse tree.** You would have to invent a
digit-status concept without X; migrate `Output.propagate`/`WireNet.propagate` away
from `@Nullable BitSet` yourself (which *is* P1's Stage 5, 12–16 weeks, so you
would be doing P1 anyway but without its dual-mode green-tree discipline and
without its greppable `zeroFill()` progress lever); and then reopen #221 for the
four-state work afterwards. It is not forbidden; it is strictly more expensive and
it forfeits P1's *"no existing lab changes behaviour in the release that ships it"*
promise (`README.md:215-220`).

---

## 7. THE MINIMUM CREDIBLE VERSION, AND THE GATE

### 7.1 Minimum credible version

**Stage 0 alone (~1 week) is the minimum unconditional buy** and is recommended
without qualification. It ships nothing and forecloses nothing.

**The minimum version that delivers a *capability* is Stages 0+1+2+3 with radix 3
only, balanced encoding only, and six element types** — `MvlGate` (min/max),
`MvlNot`, `MvlAdder`, `MvlMux`, `MvlDisplay`, `RadixBridge` — at **13–19
maintainer-weeks**. Dropping radix 4, unbalanced mode, `MvlTruthTable` and
`MvlConstant` removes the even-radix refusal, the N^k decoder guard, most of the
dialog work, and one release cycle of coverage pressure. This is the version a
maintainer who wants *a beautiful teaching artifact* should buy; the full 17–25
week version is the one a maintainer who wants *a platform* should buy.

### 7.2 There is a cheap preview and it should ship first, as data

**The BET circuit library: ~1–2 maintainer-weeks, zero engine change, zero format
version, zero golden churn, zero K9 exposure.** JLS's existing `TruthTable`
expresses an arbitrary function of encoded inputs, so `TNOT` / `STI` / `PTI` / `NTI`
/ `TAND(min)` / `TOR(max)` and a balanced-ternary half- and full-adder are each
**one truth table over 4 input bits**, and a multi-trit word is a `Splitter`/`Binder`
idiom. This is D7's *"circuit libraries are DATA, not plugins… this is also the
biggest single win"* applied exactly.

It also **exports today**: an encoded-ternary circuit is an ordinary binary
circuit, so it reaches VCD, Verilog, Yosys and Tiny Tapeout like anything else —
which is precisely how real CNTFET ternary designs are validated against binary
references.

**Ship it as Stage −1** — not as a substitute for the native path, and explicitly
**not as a demand test**, but because it is the cheapest possible way to (i) find
out whether `TruthTable`'s dialog carries an N-ary table comfortably, (ii) settle
the algebra question against something concrete rather than in the abstract, and
(iii) give the Stage 2 kernel work a hand-authored reference implementation to diff
against. Every one of those is a Stage 1–3 *input*.

**Verify one thing before promising the library:** `TruthTable.react` destroys
don't-cares (*"don't care becomes false"*, `:1447-1449`). Harmless for a *total*
ternary function, but build one 2-input balanced-ternary gate first.

**Anticipate and pre-refuse one thing in the library's own docs**: the request to
show trit values natively in `Display`/pins/traces rather than as 2-bit codes. That
request is exactly where a data-only library starts turning into a value-type
change, and it should be answered by pointing at the native path rather than by
patching the library.

### 7.3 On the gate the task asked for

The task asks for *"the DEMAND GATE in front of the rest, in the idiom the project
already uses for #212."* **I am declining to write a demand gate, and saying so
plainly is the point.** BRIEF §12 D10 is explicit — *"Demand gates apply to
third-party asks, NOT to the maintainer's roadmap"* — and it names **this file** as
having invoked #212's gate against the maintainer's own proposal. Writing one here
under a different label would be laundering the same move.

**What legitimately gates Stage 3 is a decision only the maintainer can make, and it
is a real dependency, not a filter.** Eight element types encode a *pedagogical
commitment* — which of Kleene min/max, Łukasiewicz, Post cyclic, or Allen–Givone —
that lands in a help page and a lab handout and cannot be changed later without
breaking saved circuits, because the algebra is baked into the frozen element tag
(`docs/file-format.md`'s tag-stability rule: *"the canonical write-tag for every
type is frozen forever"*). That is *"X reads data only Y creates"* in D10's own
terms. So:

> **SCOPE GATE (not a demand gate).** Stage 3 does not start until the maintainer
> has answered, in writing:
> **(i)** which operator family is the shipped default — the recommendation is
> min/max with complement negation, on the K9 grounds in §4.1;
> **(ii)** balanced, unbalanced, or both;
> **(iii)** what `notQ` means on a base-3 `Register` (2−d, cyclic +1, or cyclic −1);
> **(iv)** whether the goal is *a beautiful teaching artifact* or *a research
> platform*, because §1.1's stage list and §7.1's minimum version differ materially
> between them.
> Stages −1, 0, 1 and 2 do not wait on this.

---

## 8. REVISIT TRIGGERS — in `ARCHITECTURE.md` recorded-decision idiom

To be recorded under **Recorded decisions**, in the shape of the i18n and #221
entries (`ARCHITECTURE.md:238-250`, `:340-368`). Written so it can be recorded
whichever way the maintainer rules.

> ### Multi-valued computational logic: bounded to radix 2–5, radix 2 shipped (recorded 2026-07)
>
> JLS's value domain is a **per-position alphabet** carried in three bit-planes,
> eight code points per position, of which the shipped alphabet (`0 1 X Z U`,
> radix 2) uses five. **Radix is a property of ports and wire nets, like bit
> width** (`docs/simulation-semantics.md` §2), never of the value object, and
> `record Word(int width, long a, long b, long u)`'s field list is **frozen**: no
> radix field, no `long[]` value plane. Both destroy the primitive-long value lane,
> measured at **+32% and +16 bytes on every value in every circuit including pure
> binary ones** for the radix-tagged variant.
>
> **Radix 3, 4 and 5 are inside the shipped encoding at zero storage cost**
> (`ceil(log2(r+3)) ≤ 3`). **Radix 6 and above are outside it** — a fourth plane is
> a second storage migration, which is the one thing the value-representation design
> exists to avoid. **Arbitrary N is a non-goal**, on that arithmetic.
>
> **Modelling MVL (X/Z/U, IEEE 1164) is program P1 and is a different thing.**
> Kleene three-valued X-propagation is not balanced ternary; the two must not be
> substituted for one another in any document.
>
> **Device-level MVL is permanently out of reach**: JLS is word-level and models
> neither energy nor real time. Any MVL documentation must say so in its own words.
>
> **Revisit triggers:**
> **(a)** *for the native path* — P1 has shipped and is stable; §2 is re-anchored in
> alphabet-parameterised language; and the maintainer has recorded an answer to the
> four §7.3 scope questions. Then Stages 1–4 are schedulable at 16–24
> maintainer-weeks against whatever they displace.
> **(b)** *for the encoded BET library* — none. It is data under D7 and can ship at
> any time.
> **(c)** *to reopen the radix ceiling above 5* — a measured demonstration that a
> fourth value plane does not regress the radix-2 event loop, on the same harness
> and access pattern as `keystone-c-performance.md` §5, plus a named design that
> does not put a variable plane count on the hot path.
> **(d)** *to reopen the device-level exclusion* — `docs/grand-architecture.md` §9's
> analog exclusion is lifted, which is a different and much larger decision.
>
> Until (a) is met, PRs adding partial N-ary scaffolding to **existing** element
> types will be declined — **not because the capability is unwanted, but because a
> `radix` attribute on an existing type fails open at HDL export, requires FORMAT 3
> under `docs/file-format.md` §9's silent-drop rule, and does not fit the coverage
> gate** (27 dialogs × ~20 lines = 89% of the bundle's LINE headroom). N-ary belongs
> in new element types.

---

## 9. EFFECT ON THE VIRTUAL-HARDWARE / PARITY PROGRAM

**Orthogonal, with one sentence to add and one mild synergy. Radix does not change
the parity contract.**

**Why.** The parity contract's comparison alphabet is RVFI's field list —
`{order, pc_before, pc_after, insn_word, rd_index, rd_value, mem_addr, mem_rmask,
mem_wmask, mem_wdata, privilege, trap}` (BRIEF §6) — plus full architectural state
at sync points, the guest output byte stream, trap cause, and retired-instruction
count. Those are **integers**. Under the port-radix determination, a value's radix
is a property of the ports it flows through, not of the integer it denotes, so a
balanced-ternary `M_L` and a binary `M_H` produce the *same* parity records. One
clarification is needed:

> The parity comparison is over **integer values**, not over their textual
> rendering. Two machines whose ports declare different radices compare equal when
> their architectural integers are equal.

That is one sentence in the parity contract, and it should be added whether or not
MVL ever ships, because it is also what makes a hex-rendering `M_H` and a
decimal-rendering `M_L` comparable.

**Mild synergy, worth noting but not worth scheduling around.** Two structural
patterns coincide:

- The parity program's core move is D4's **per-subcircuit toggle** between
  full-fidelity structural operation and a compiled/behavioural implementation,
  which makes parity *a property of a BOUNDARY* — which is what makes it testable.
  `RadixBridge` makes radix a property of a boundary in exactly the same way. A
  ternary subcircuit whose "compiled" side is a behavioural Java element is the same
  mechanism with a different reason. **MVL is a good second customer for the
  boundary machinery**, and a second customer is mild evidence the boundary is
  well-shaped.
- The levelized/compiled pass must be told a region's radix at compile time and must
  refuse a mixed-radix levelized region. Because radix cannot cross a net, that
  falls out for free — but the parity work should state it, because the levelizer is
  the parity program's second strategy and `keystone-c` §6.3's plane arrays are the
  shared artifact.

**Neither help nor hindrance.** MVL does not accelerate the parity program and does
not block it. It competes with it for the *same 17–25 maintainer-weeks*, and that
competition is §1.2's displacement statement, not a technical interaction.

---

## 10. THE HONEST CASE AGAINST

At full strength, with nothing drawn from precedent, demand, or what the tree
currently contains.

1. **17–25 weeks is four to six months of the only maintainer, and it displaces a
   named alternative.** It is roughly all of P2 (22–32 wk), or two-thirds of P1's
   four-state core, or the entire 2.26× semantics-preserving engine stack that turns
   a 1.66 h structural boot into 44–46 min. Every week is real.
2. **The user-visible capability lands 9–12 calendar months after Stage 1 starts**,
   because `jls.elem`'s coverage floor tolerates 4–6 new element classes per release
   and eight types is two cycles. Stages 0–2 are 8–12 weeks with *zero* user-visible
   payoff — the same shape as P1's Stage 5, which the roadmap itself flags as *"the
   only multi-month stage with no user-visible payoff, which is exactly why it is
   fifth."*
3. **The field's contested question is device energy, and JLS models none of it.**
   Etiemble's series (arXiv:1908.06841, 1908.07299, 2005.02678, 2101.01516) argues
   the ternary/binary transistor-count ratio exceeds log(3)/log(2) and that *"for
   arithmetic circuits such as adders and multipliers, the ternary circuits are
   always outperformed by the binary ones using the same technology"*; Zahoor et al.
   (*Results in Engineering*, 2024) argues the opposite; Bos's thesis proposes *"a
   novel radix comparison methodology to improve fairness"* — i.e. the community
   names fair device-level comparison as its own core problem. A word-level simulator
   with a dimensionless time axis cannot participate in that argument at all.
4. **The BET library delivers a large fraction of the demonstration value for ~1–2
   weeks**, exports to every existing surface, and is what practitioners already do.
   If the goal is a teaching artifact rather than a platform, the native path is
   roughly **10× the cost for ergonomics**.
5. **The algebra choice is irreversible in saved files.** Choosing Kleene min/max
   and later wanting Łukasiewicz means a new element family or a format break,
   because the algebra is baked into the frozen element tag.
6. **The prefix-carry adder's 9.79 ns is platform-dependent and unmeasured on the
   affected hardware.** `Long.compress`/`expand` are microcoded on AMD Zen 1 / Zen+ /
   Zen 2. JLS ships one offline jar to unknown student laptops. The fast version will
   look fast on the maintainer's machine and may be slow in a lab.
7. **MRCS already occupies the synthesis niche** — GPLv3, browser-based, with the
   HSPICE and Verilog exports that decide usefulness for a device-level user, and it
   produced a balanced-ternary CPU (REBEL-2) and four tapeouts. JLS's genuine
   differentiator is narrower than "an MVL tool": it is *the drawn, simulated,
   interactive step that does not exist anywhere in that flow*. That is a real
   differentiator and it is a smaller one than it sounds.
8. **Every JLS element added after this point must decide, implement, document and
   test its N-ary behaviour or ship a refusal path** — P2 alone proposes Multiplier,
   Divider, Comparator, Subtractor, Counter, priority encoder, a real shift register
   and a CSR set. This is a permanent tax on future element work at bus factor 1. It
   is *bounded* by the port-radix design (a new binary element declares radix 2 and
   is done) but it is not zero. Note the countervailing scheduling argument:
   balanced-ternary multiplication is structurally *simpler* than binary (digit-wise
   ×(−1) is free negation, no Booth recoding), so authoring `Multiplier`/`Divider`
   radix-aware from the start is cheaper than retrofitting — which couples two
   otherwise unrelated backlog items.

---

## 11. CORRECTIONS TO THE ANGLES, AND CLOSED OPEN QUESTIONS

| angle claim | correction, verified at HEAD |
|---|---|
| Angle 5: the fourth-plane cliff is at radix 8 | **Radix 6.** `ceil(log2(r+3))`: r=5 → 8 codes → 3 planes; r=6 → 9 codes → 4 planes |
| Angle 5: 10 `computeOutput` implementations | **9 concrete overrides** (`AndGate`, `DelayGate`, `Extend`, `FieldExtend`, `NandGate`, `NorGate`, `NotGate`, `OrGate`, `XorGate`) plus one abstract at `Gate.java:663`. Total value-computing methods = 27 + 9 = **36**, matching the adversary |
| Angle 1: *"the adder fraction is unmeasured"* | **Measured.** `keystone-c-performance.md:124-132`'s event census: `Adder` 108,025 of 2,331,793 fired = **4.63%** |
| Angle 1: sibling records `WordN`/`WideN` under the sealed interface | **Not needed for radix ≤ 5.** Port-radix over P1's existing three planes is strictly cheaper, needs no `permits` change, and removes the mixed-circuit type-profile cost entirely. Angle 1's tagged-union *rejection* is preserved and is why `Word`'s fields are frozen |
| Angle 2: `family` attributes on existing element types | **Overridden.** New types instead — attributes fail open at HDL export, force FORMAT 3, and 27 dialog radix controls consume 89% of the bundle LINE headroom |
| Angles 3 and 4: an N-ary circuit can never leave JLS; HDL export refuses permanently | **Wrong.** BET lowering is what the field itself does (TT03, verified by fetch), and MRCS is GPLv3 and emits Verilog, so an external (manual) oracle exists. **Lower, don't refuse** |
| Adversary: `ElementRegistry` 35 types, 27 `react` | **Confirmed at HEAD.** `ElementRegistry.java:38-77` = 35; `grep -rln "public void react("` = 27 files |
| Adversary + Angle 5: `Palette.ENTRIES` = 32 | **Confirmed**, `Palette.java:123-188`, across 8 groups at `:36-60` |
| BRIEF §13: ~65 lines across 12 files per new element type | **Confirmed.** `git show --stat 38a0544`: 14 files, 1,188 insertions, of which the two element bodies are 486 + 569, leaving 133 for two elements. **But the element *body* is ~500–570 lines**, and that is the number that binds against the coverage floor |
| Prior 07 determination: *"zero mentions across 944 tracked files"* as the lead argument | **Struck by D10.** It measures what JLS has never offered. The adversary flagged its own circularity and the flag was right |

**Open questions that remain open:**

1. **Which algebra**, and what `notQ` means on a base-3 `Register`. Pedagogical,
   maintainer-only, gates Stage 3 (§7.3).
2. **`Long.compress`/`expand` on AMD Zen 1/Zen+/Zen 2.** Needs a measurement on
   affected hardware, or a pure shift/mask fallback with its own measured cost.
3. **Whether `TruthTable`'s dialog carries an N^k table comfortably.** Testable in
   an afternoon by building one 2-input balanced-ternary gate — the first thing
   Stage −1 does.
4. **Whether `Memory`'s `sync` version question** (`docs/file-format.md:473-482`,
   still open, tracked with #199) resolves toward "bump". It is the nearest
   precedent for the attribute-vs-bump call and should be settled before any
   attribute-shaped MVL is ever reconsidered.
5. **The exact plane↔lane conversion cost for base 3**, inferred at ~7 ns from the
   radix-4 interleave round trip (13.43 − 6.51) rather than measured directly. The
   ~17 ns total for a base-3 32-digit add is an estimate until it is measured.
6. **Vocabulary.** "Ternary" is ambiguous between the two programs and the corpus
   already spends "three-valued"/"nine-value" on P1. Fix terms — *multi-radix* for
   computational, *multi-valued* for modelling — in `docs/component-naming.md` and
   `docs/collab-vocabulary.md` **before any code**, because every future issue title,
   doc section and test name depends on it.
7. **Circuit-level vs subcircuit-level radix.** I recommend per-circuit default with
   per-port override, but D4 wants parameterised subcircuits, and a radix that is a
   subcircuit *parameter* would be the natural home under P7/lf-01. Unresolved:
   whether a radix-3 subcircuit instantiated in a radix-2 parent makes sense, and if
   so what the boundary does. That is the mixed-radix problem moved to a boundary
   where it is at least testable.

---

## 12. WHERE I DEPART FROM THE MAINTAINER'S FRAMING

Three places, stated plainly rather than softened.

1. **"While we're at it" understates the sequencing and overstates the coupling.**
   Only *one week* of this work genuinely belongs inside P1 — the §2 re-anchoring and
   the #221 motion. The other 16–24 weeks are a separate program that touches almost
   none of P1's code. The good news is bigger than the framing suggests (the value
   type is free); the scheduling news is worse (it is its own program, not a rider on
   one).

2. **"More interesting for research" splits, and half of it is unreachable.** JLS can
   host ternary *architecture* research — a Setun-class balanced-ternary datapath,
   radix-3/4 ALUs, word-level ternary accumulators — and cannot host ternary *device*
   research, which is where the field's growth and its contested question both are.
   No change to the value type moves the second. The maintainer should buy the first
   knowing the second is permanently out of reach and why.

3. **The strongest research position in this repository is not MVL.** BRIEF §12 D9's
   span argument — specification → logic → RTL → synthesis → P&R → board → silicon,
   spanned by no existing educational tool — is a stronger and already partly-funded
   research position than multi-valued logic, and it competes for the same
   maintainer-weeks. That is not an argument against MVL. It is the comparison the
   displacement decision in §1.2 should be made against.
