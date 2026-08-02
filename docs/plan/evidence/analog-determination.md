# 11 — ANALOG DETERMINATION: full SPICE-class analog simulation in JLS

**Status:** determination. The decision to build this is the maintainer's and is
made ("We keep skirting NGSPICE support. Just integrate it fully as a feature to
feed capstones."). This document is the *how* and the *price*.

**Inputs:** seven angle analyses, all read in full —
`spice-ngspice-facts.md` (Angle 1), `spice-numerics.md` (2),
`spice-device-models.md` (3), `spice-xspice-mixed.md` (4),
`spice-jls-integration.md` (5), `spice-determinism-testing.md` (6),
`spice-capstone-feed.md` (7).

**Repo state:** HEAD = `b54e6ee`. Every HEAD claim below was re-verified in this
session; the verification transcript is §12.

**Governing decisions:** D1-D10 of `BRIEF.md` §11-13. D10 is binding throughout:
no capability is refused by citing its absence, no gate is demanded against the
maintainer's own roadmap, and no prior deferral in this study is treated as
evidence. Where a *specific* approach is refused below, the approach that works
is named and costed in the same paragraph.

---

## 0. THE DETERMINATION IN ONE PAGE

1. **Port the numerics to Java.** Build `jls.analog` in-tree, pure Java,
   `StrictMath`-only, single-threaded. Keep real ngspice as an **optional,
   non-gating, maintainer-side CI oracle** at 1e-4 tolerance. Never link it,
   never subprocess it from `src/`.
2. **Analog devices are drawn elements on the existing canvas**, legal only
   inside a `SubCircuit` carrying D4's per-instance `impl "analog"`. `Ground` is
   a required element. Imported `.subckt` files are **leaves inside** a region,
   never a parallel top-level mechanism.
3. **An analog net is the same `WireNet` class carrying no value at all** — only
   an MNA node index assigned at elaboration. Zero changes to
   `WireNet.propagate`, zero new `SimEvent.Payload` records, zero format
   version.
4. **Invert XSPICE's time ownership.** The digital event loop owns `now`; the
   analog region is a self-scheduling element in `Clock`'s idiom. That single
   inversion **deletes `EVTbackup`** — the hardest piece of the XSPICE
   mechanism and the one JLS structurally cannot build.
5. **Byte-identical goldens are achievable and are the required gate.**
   Tolerance comparison is *also* required, for a different job. Both.
   No PIT exemption is needed — measured 86%/88% against an 80/82 gate.
6. **69-105 maintainer-weeks** for the full programme, central ~86. **31-45** to
   all three analog capstones. **3-4.5** to the first audible demo, which needs
   no solver at all. Against 281-410 committed: +17% to +37% full, +8% to +16%
   for the three-capstone cut.
7. **Capstone 4 (breadboard) does not wait on this by one week.** Verified with
   arithmetic, §7.4.

---

## 1. THE INTEGRATION CHOICE

### 1.1 The decision

> **D-A1. PORT the numerics to Java as `jls.analog`. Absorb ngspice, XSPICE,
> Sparse1.3, SpiceSharp and CircuitJS1 source under D8. Keep an external
> ngspice binary as a maintainer-side, non-shipping, tolerance-based CI oracle
> behind a self-skip, promoted per-device-family. There is no `libngspice` and
> no `ProcessBuilder` in `src/`, ever.**

This is a **hybrid**, and the hybrid line matters: the *product* is a pure-Java
port; the *validation* uses real ngspice. Those are different jobs and neither
substitutes for the other (§4.2).

### 1.2 The four surfaces, weighed against the four hard constraints

| | (a) subprocess | (b) libngspice + FFM | (c) **port** | (d) hybrid = (c) + (a) in CI |
|---|---|---|---|---|
| Single offline jar | **broken** — user installs ngspice | **broken** — 8,303,696 B for ONE platform, 3.15× the whole 2,637,854 B JLS jar; ~40-50 MB across six platforms, vs the 7.3 MB / 18-lib zstd-jni rejection | preserved | preserved |
| Self-contained even then | n/a | **no** — XSPICE code models are separate `.cm` objects (514,536 B total) referenced *by filesystem path* from `spinit`; a jar-resident library must extract to disk. KiCad's macOS bundle hits exactly this and users install ngspice anyway | yes | yes |
| Byte-identical goldens | **impossible, measured** | **impossible, same engine** | **achievable, measured** | achievable |
| CI | 3-OS toolchain matrix + skip logic + a version pin that rots | same, plus `--enable-native-access` (JEP 472) on every student `java -jar` | no matrix, joins the existing golden culture | oracle lane is non-gating and self-skipping |
| Bus factor 1 | every ngspice bug is a JLS bug; ngspice's version matrix becomes JLS's support matrix | same, plus a C `abort()` in a code model kills the editor with the student's unsaved circuit | more code owned, in the language and toolchain already owned, under the existing gates | same |
| First-mover cost | **first `ProcessBuilder` in `src/`** — a `.jls` file that causes an arbitrary external binary to run inverts #38's premise that a `.jls` file is DATA | first native load | none | ProcessBuilder lives in `test/`, alongside the existing `iverilog`/`ghdl`/`yosys` pattern |
| Build cost | 8-14 mw | 12-20 mw | 69-105 mw | 69-105 mw + ~3-5 for the oracle |

### 1.3 Why the port, stated as evidence rather than preference

**The decisive measurement (Angle 1 §4.1, first-hand).** Two ngspice builds —
Ubuntu's 42 (KLU + OpenMP) and a source build of 46+/47-pre (Sparse1.3) — on a
**two-element linear RC** with a pulse source, `.tran 1u 3m`, `numdgt=17`:

| Metric | Result |
|---|---|
| rows | 3,056 both |
| internal time points that differ | **2,026 of 3,056 (66%)** |
| sample values that differ | **3,035 of 3,056 (99.3%)** |
| max relative difference in `v(2)` | **5.38e-04** |
| first divergence | row 1024, t = 1.0000 ms — *exactly the pulse breakpoint* |

The cause is identifiable: ngspice-46's `NEWS` lists "Equalise the last two time
steps before a breakpoint", implemented at `dctran.c:541-549` and `:592-600`.
A BJT+diode netlist gave 603/608 samples differing at 4.52e-10. Same build twice
= byte-identical.

Corroborating, independently measured by Angle 6 on **one machine with one
binary**: ngspice 42 with `.options klu` vs `.options sparse` differ in **987 of
1,022 rows**, worst 7.54e-14 relative. The linear solver is a runtime
configuration choice, and floating-point addition is not associative.

Structurally: `configure.ac` exposes `--enable-nobypass`, `--enable-capbypass`,
`--enable-nodelimiting`, `--enable-predictor`, `--enable-newpred`,
`--disable-klu`, all of which change the numerics; OpenMP is default-on
(`configure.ac:1321`) with bare `#pragma omp parallel for` over instance arrays
and zero `omp critical`/`atomic` in `bsim4/b4ld.c:81-87`. The rawfile header
itself carries `Date: <ctime>` (`rawfile.c:115`) and `Command: … Build …`
(`:117`) — confirmed in a generated file: `Date: Wed Jul 29 06:26:57  2026`.

> **Consequence, and it is a measured limit rather than a preference: no JLS
> required-gate golden can be built on an external ngspice, whether subprocess
> or shared library. A golden would break on every ngspice release, and would
> need a 1e-4 tolerance — loose enough to hide real regressions.**

**The counterpart, also measured (Angles 2 and 6).** A 229-line Java MNA/Newton/
LU/trapezoidal/LTE kernel emitting every accepted timepoint as raw IEEE-754 hex
produced the **identical md5 `13cbc7cb3ff955229f8c7f246f185c9a` across seven
configurations**: JDK 25 twice, `-Xint`, `-XX:TieredStopAtLevel=1`,
`-XX:-UseFMA -XX:UseAVX=0 -XX:UseSSE=2`, `-XX:+UseSerialGC -Xmx32m`, and JDK 21.
The FMA and AVX rows are the two mechanisms by which an x86-64 result would
differ from aarch64, and disabling both changed nothing.

This is not a tiebreaker. **It is a property orchestration cannot buy at any
price**, because the nondeterminism enters through the platform libm, which JLS
cannot pin inside a native binary. Under D8's cost-and-specialism axis that is
the strongest possible form of the reimplementation case.

### 1.4 The condition the brief left open, and it must be enforced mechanically

`StrictMath` is pinned to fdlibm 5.3 **by specification** for 18 named functions
(`sin cos tan asin acos atan exp log log10 cbrt atan2 pow sinh cosh tanh hypot
expm1 log1p`) and is specified to return "exactly the same results on all
platforms". `java.lang.Math` explicitly is **not**: its class javadoc says
implementations "are not defined to return the bit-for-bit same results", it
carries 1-ulp budgets, and it is `@IntrinsicCandidate`. Measured:
`Math.exp` and `StrictMath.exp` differ in **96,260 of 1,000,000** sampled inputs
(9.63%) — a 1-in-10 hazard. Swapping only `exp` changed the whole rectifier
trajectory digest in every configuration tested.

Both candidate reference implementations violate this: CircuitJS1 uses
`Math.exp` 44× and `StrictMath` zero times; SpiceSharp uses `Math.Exp` 99×.

**But `StrictMath` guarantees nothing about the reduction code written around
it.** Summation order, pivot order and node numbering all change the last bits
and none of them is a `Math` call. So the rulebook has five clauses, not one
(§4.1), and the determinism gate must test the **matrix path**, not merely ban
`Math`.

Cost of the discipline, measured: `StrictMath.exp` 9.64-9.93 ns vs `Math.exp`
5.72-5.82 ns over the argument range a diode actually visits = **1.66-1.73×**.
(Angle 2's earlier 1.47× figure was measured over a wider, less realistic range;
Angle 7's is the one to quote.) On a 5e8-exp-call research-scale run that is
about two extra seconds. At teaching scale it is unmeasurable.

### 1.5 What is refused, and the approach that works instead (D10 rule 5)

- **Refused: `libngspice` via FFM.** It fails the exact constraint that killed
  zstd-jni, by 6×, and does not buy determinism. **Works instead:** the port.
- **Refused: ngspice as a required-gate oracle.** Measured 5.38e-4 cross-version
  drift on a two-element linear circuit. **Works instead:** ngspice as a
  *non-gating* nightly tolerance lane at 1e-4, promoted per-device-family after
  20 green runs, using the repo's existing best-effort-apt + self-skip pattern
  (`iverilog`, `ghdl`, `yosys`, `xvfb`) with the `ProcessBuilder` in `test/`.
- **Refused: OSDI / compiled Verilog-A.** `osdi/osdiregistry.c:31` is
  `dlopen(path, RTLD_NOW|RTLD_LOCAL)` and `:39` is `LoadLibrary(path)` — per-
  platform native objects. **Works instead:** a Verilog-A subset compiler
  targeting the Java compact-model SPI, 8-15 mw, reusing the `B`-source
  expression and differentiation engine (§3.4, Door 3).

---

## 2. THE ARCHITECTURE

### 2.1 How analog exists in the JLS model

> **D-A2. Analog devices are first-class drawn `Element`s on the existing
> canvas (option a), legal only inside a `SubCircuit` whose per-instance
> implementation is `analog` (option b), with imported `.subckt` definitions as
> leaf elements inside a region (option d). There is no separate analog canvas
> (option c), ever.**

**Why drawn and not text-in-a-box.** Drawing a resistor is the pedagogical point
of the analog capstones. D9's CS→ECE→EE single trajectory means concretely that a
first-year who has drawn an adder can draw an RC filter with the same gesture.
An analog body that is an opaque netlist is "dead content that lies about what
runs" — the objection the KiCad and breadboard determinations correctly raised,
and it applies here unless analog is drawn.

**Why contained in a `SubCircuit` and not just a convention.** Three independent
reasons:

1. **The boundary is what makes parity testable.** D4's `Boundary` façade, port
   congruence B1, boundary totality B2, initialisation congruence B3, purity B4,
   retained drawing B7, the R1-R9 refusal set, `-fidelity`, the manifest and its
   FNV-1a digest, and all of `jls.sim.equiv` apply **verbatim**. A region with no
   boundary has nothing to compare.
2. **It makes K9 mechanical rather than aspirational.** The palette can ask the
   *model* a question — *am I editing an analog-impl subcircuit?* — instead of
   asking a user preference (§2.7).
3. **It makes the domain rule finite.** With containment, a domain crossing can
   occur only at (i) the region's own external ports, which are digital by
   construction, and (ii) an `Adc`/`Dac` the student drew. Without it, every net
   in every circuit is a potential crossing.

`AnalogImpl` becomes a **third `SubCircuitImpl` permit** alongside
`StructuralImpl` and `LevelizedImpl` — a permit, never a parallel mechanism.
This is the single most important sequencing constraint in the document: if
analog invents its own binding, JLS acquires two boundary mechanisms with two
save shapes, two refusal sets and two equivalence stories, which is exactly the
"solved twice, incompatibly" hazard D9 names.

**Why not a separate canvas.** Two reasons, cost and correctness.
*Cost:* a minimum canvas is ~850-1,350 executable lines, nearly all branches,
against a total commons of **2,897 addable-uncovered LINE / 1,476 BRANCH** for
all future untested code in the project (fourth independent replication, exact
agreement). One canvas is 29-47% of the LINE commons.
*Correctness, and this is the decisive half:* **analog needs no new geometry.** A
resistor is a rectangle with two terminals. Hit-testing, snapping, selection,
undo, copy/paste, the op layer and the CRDT are already correct for analog,
because analog changes *what a symbol means*, not *where it is*. A breadboard
canvas is genuinely different; analog is not. What works instead: the same
canvas with per-view palette gating, a per-domain wire stroke, and a distinct
renderer group — ~0 marginal.

### 2.2 The element hierarchy, and the blocker nobody had stated

> **D-A3. `AnalogElement` becomes the FOURTH `Element` permit. `Put.element` is
> widened from `LogicElement` to `Element`. `WireNet.propagate`'s blind
> `(Reacts)` cast is replaced by an `instanceof Reacts` guard.**

Verified at HEAD:

- `Element.java:17-18` — `public abstract sealed class Element permits
  DisplayElement, LogicElement, Wire` (3 permits).
- `Put.java:17-18` — `public abstract sealed class Put permits Input, Output`;
  `:29` `protected @Nullable LogicElement element`. **There is no bidirectional
  terminal anywhere in JLS.**
- `instanceof LogicElement` appears at exactly **8 sites in 6 files** across all
  of `src/` (verified: count = 8).

**Riding `LogicElement` is actively wrong, not merely ugly.** `LogicElement
implements Reacts`, so a no-op-react analog device would still be handed
`PinChanged` by `WireNet.propagate` and still be seeded by
`initSimulation`'s `instanceof LogicElement` walk (`Simulator.java:196-200`) —
silently joining the digital net graph. The fourth permit forces exactly 8
decisions, each a compile error until made.

**The real blocker is `Put`, and it was found only by Angle 5.** An
`AnalogElement` that is not a `LogicElement` cannot own a `Put` as those types
stand. Widening `Put.element` to `Element` is source-compatible for all 35
existing element classes. The `instanceof Reacts` guard in `WireNet.propagate`
is *independently a correctness fix* — the cast is safe today only by hierarchy
accident — and it gives the analog domain rule a second enforcement point that
does not depend on the editor having refused the connection.

**Sequencing rule:** do the `Put` widening as a standalone commit with no analog
code, and let the compiler and the suite report. 3 days, and it de-risks the
largest hierarchy change in the programme.

### 2.3 Keeping analog and digital nets apart

> **D-A4. An analog net is the SAME `WireNet` class, carrying no value at all —
> only an MNA node index assigned at elaboration. Domain lives on the PORT and
> the NET, never in the value, resolved at elaboration. This is the MVL
> radix-on-the-port mechanism verbatim, and it is built ONCE.**

| | Digital net (HEAD) | Analog net |
|---|---|---|
| Class | `WireNet` | **`WireNet`** |
| Carries | `int bits` + `@Nullable BitSet value` | **nothing**; voltage is `x[nodeIndex]` in the solver's `double[]`, branch currents are extra MNA unknowns |
| Width rule | `bits = Math.max(p.getBits(), bits)` (`WireNet.java:280`) — negotiated | **no width**; all analog puts declare `bits = 0` |
| Drivers | `hasinput` true iff some end is an `Output` (`:283`) | **always false**; every analog terminal is an `Input` |
| Propagation | `Output.propagate` → `WireNet.propagate` → `post(PinChanged)` | **never runs** — no `Output` exists on the net |

**The structural result, verified by grepping every `.propagate(` call site in
`src/`:** all of them are `<someOutput>.propagate(value, now, sim)` inside an
element's `react`. Therefore an analog net requires **zero changes to
`WireNet.propagate`, zero changes to `Simulator.post`, zero new
`SimEvent.Payload` records** — Angle 4 measured a new payload kind at 16 file
edits against 16 exhaustive switches with no `default` arms — **and zero format
version.** The connectivity graph JLS already maintains *is* the MNA node
partition.

A free consequence worth pinning by a test: `hasInput()` is always false on an
analog net, so the "Both wires have inputs" multi-driver refusal
(`SimpleEditor.java:4020,4053,4147,4180,4253`) correctly does **not** fire —
two resistors on a node is legal.

**THE TRAP, and it is the single most important line of code in this design.**
The width check at `SimpleEditor.java:4015` is guarded:

```java
if (bits1 > 0 && bits2 > 0 && bits1 != bits2) { overlapMessage = "Bits don't match"; }
```

`Put.java:34` documents `bits` as "0 implies arbitrary". **An analog terminal
declaring `bits = 0` makes the width check unconditionally pass** — so it would
connect to a 32-bit bus with no complaint, and `WireNet.recheck`'s
`Math.max(p.getBits(), bits)` at `:280` would silently give the mixed net a
width of 32. **The domain check must sit ABOVE the width check and be
UNCONDITIONAL, at all four sites** (`:4015, :4142, :4247, :4358`, all verified
at HEAD as `"Bits don't match"`).

The rejected alternative — give analog terminals `bits = 1` — fires the wrong
message for a domain error and lets a 1-bit digital wire connect silently, which
is the exact case the `Adc` exists to mediate.

**Four enforcement layers, matching the MVL determination clause for clause:**

1. **Edit time** — four `SimpleEditor` sites, domain check above width,
   unconditional, message naming the sanctioned bridge.
2. **Load time** — `WireNet.recheck` (`:272-302`) **validates, never widens**;
   error names both **stable ids** and both domains so the message survives a
   re-save (D2).
3. **Elaboration** — `IllegalStateException`, phrased as a JLS defect, because
   by then two layers have failed.
4. **`Adc`/`Dac`** — drawable, named, sanctioned, **shipping in the same release
   as the check**. No coercion is defined, ever.

Plus one small real fix: `WireEnd.infoText:421-423` and `Wire.infoText:338-340`
append `", no input"` when `!net.hasInput()` and return `"not connected"` when
`bits <= 0`. Both are wrong and alarming on every analog net. ~20 lines, two
files, must ship with the mechanism.

> **D-A5. Build `Put` alphabet validation ONCE as a `PortAlphabet` descriptor
> carrying `{domain, radix, encoding, strength}`, populated per increment.**
> Analog populates `domain`; MVL populates `radix`/`encoding`; P1's already-
> committed X/Z/U + strengths populates `strength`. Whichever ships first pays
> ~2.45 mw; the others pay ~0.4. Saving ~1.5 mw — and the architecture is the
> real return, because the alternative is three refusal vocabularies.

### 2.4 What the model holds at elaboration

For each `SubCircuit` instance with `impl "analog"`:

1. Collect the region's `AnalogElement`s and the `WireNet`s their puts belong to.
2. Partition: each analog `WireNet` is one MNA node. Exactly one net must contain
   a `Ground` terminal; that net is node 0, eliminated from the matrix. Multiple
   `Ground` symbols merge silently into one node, exactly as SPICE does for
   node `0`.
3. Assign extra MNA unknowns: one branch current per `VoltageSource`, per
   `Inductor`, per `Vcvs`/`Ccvs`, per current-controlled element's controlling
   source.
4. Check DC path to ground per node.
5. Hand the solver: node count, unknown count, the device list with resolved
   parameters, and the boundary map.

**`Ground` is a required element type and appears in no prior document in this
corpus.** MNA is singular without a datum: the conductance matrix of a floating
network has a null space spanned by the all-ones vector, so LU hits a zero
pivot. SPICE reserves node `0` **by name**; JLS's nets are anonymous, identified
by connectivity, so the datum must be **drawn**. The payoff is that the two most
common real SPICE failures become precise diagnostics instead of a singular
matrix:

```
Analog region 'tia' has no Ground element. Every analog region needs exactly one
ground reference — the 0 V the solver measures every other node against. Drop a
Ground on the node you want to call zero.
```
```
Nodes {n7, n8, n9} in region 'tia' have no DC path to ground. The devices on
them are: C4, C5, Q2 (base). Add a resistor to ground, or check for a missing
wire.
```

> **D-A6 (determinism obligation).** Node index assignment must be a pure
> function of circuit content in **stable-id order**, matching
> `Simulator.initSimulation`'s #181 seeding (`Simulator.java:189-200`) — **not**
> the `LinkedHashSet` insertion order `WireNet` uses, because insertion order is
> a function of *file* order and stable-id order is a function of *content*, and
> D2's merge story can diverge those. Node ordering changes the pivot sequence,
> which changes the summation order, which changes the last bits of every
> voltage. This is a one-line choice with a golden-stability consequence and it
> belongs in `docs/simulation-semantics.md`.

### 2.5 The A2D / D2A boundary

> **D-A7. `Adc` and `Dac` are ONE-BIT LEVEL BRIDGES, exactly as XSPICE's
> `adc_bridge`/`dac_bridge` are. An n-bit converter is DRAWN, not
> parameterised. SAMPLE RATE IS NOT A PARAMETER of either element.**

`Adc(vlow, vhigh, tdelay)`:
- `vlow < vhigh` gives a Schmitt trigger for free — three parameters
  (`in_low`, `in_high`, and the *sign* of their difference) yield thresholding,
  a dead band and hysteresis from one function. `vlow > vhigh` is refused as a
  typo.
- **Crossing policy P-b (recommended over XSPICE parity):** the crossing is the
  **earliest TICK at which the solved trajectory crosses**, found by bisecting
  the step, bounded by `ceil(log2(Δticks))` extra Newton solves, terminated for
  free by the integer lattice. Verified first-hand: ngspice's *shipped*
  `adc_bridge` contains **no interpolation** — it calls `cm_event_queue(TIME)` at
  the current accepted timepoint. JLS can do better because its time is integer.
  **Payoff: the goldened digital stream becomes independent of the LTE
  controller, the integration order, and step-rejection policy.**
- `tdelay >= 1` tick is a mandatory publication floor. JLS has no `EVTiter`-style
  `max_event_passes` bound, so without it a crossing published at `now` could be
  consumed by logic that changes a boundary input at `now`, giving an unbounded
  same-timestamp loop the event loop cannot break. This is the analog
  counterpart of D4's B5 causal delay floor and should be written in the same
  clause.
- Until P1 lands X, a `t=0` value inside the hysteresis band lands on 0 and is
  **reported** in the house coerce-count-report idiom.

`Dac(vlow, vhigh, vhiz, trise, tfall, rout)`:
- A digital change at tick `t_d` **ramps** as PWL over `[t_d, t_d + trise]` and
  registers a breakpoint at `t_d + trise`. **Ramping rather than stepping is not
  optional**: a step into a continuous solver is a discontinuity the integrator
  must reject and re-take.
- Because `t_d` and `t_d + trise` are **integers**, landing exactly on the
  breakpoint is exact. **JLS needs no `CKTminBreak` analogue**, and ngspice-46's
  "equalise the last two time steps before a breakpoint" heuristic — the very
  change that produced the 5.38e-4 cross-version drift in §1.3 — is unnecessary
  by construction.

**Why sample rate must not be a parameter.** Putting one on the `Adc` creates a
second clock competing with `Clock` for who owns time, which is how mixed-signal
simulators acquire their worst bugs. Sampling is *drawn*: `Adc → Register ←
Clock`. An 8-bit converter is 8 `Dac`s and 16 `Resistor`s (R-2R), or a
comparator + R-2R + a SAR register split across the boundary. That preserves
"what you draw is what runs" and honours the standing refusal against automatic
connect-module insertion.

Both bridges live **inside** the analog region, so the region's external ports
stay digital, `Boundary` stays `BitSet`-valued, and D4's B1 port congruence
type-checks unmodified.

`IdealAdc`/`IdealDac` ship as the **behavioural rung** of an abstraction ladder.
`BoundaryEquivalence.compare` already takes two `Dut`s with a time-free
observation function indexed by ordinal `k`, so *"draw it behaviourally, then
replace it with resistors, and have the tool prove they agree"* is a passing test
for one fixture and two tests. No educational tool does this, and it is D9's
CS→ECE→EE trajectory made machine-checkable.

### 2.6 Synchronisation with the event loop — A-STEP

XSPICE's coordination loop is stated as pseudocode in its own source
(`evtbackup.c:69-84`, Public Domain, Georgia Tech, PROJECT A-8503):

```
while(not end of analysis) {
    while (next event time <= next analog time) {
        do event solution ...;
        if any instance set analog breakpoint < next analog time
            set next analog time to breakpoint
    }
    do analog timestep solution ...;
    if (analog solution doesn't converge) Call EVTbackup
    else Call EVTaccept
}
```

Implemented at `dctran.c:604-644` (the `EVTnext_time`/`EVTdequeue`/`EVTiter`
loop cutting `CKTdelta` to the breakpoint) and `dctran.c:425` (`EVTaccept`).
`EVTbackup` rewinds five structures.

> **D-A8. INVERT the ownership. The digital event loop owns `now`
> (`Simulator.java:228`); the analog region is a self-scheduling element holding
> exactly one pending self-event, posted as `new SimEvent(t_next, this, new
> PinChanged())` — `Clock`'s exact idiom (`Clock.java:392,421`). This deletes
> the requirement for `EVTbackup`.**

**Why the deletion is sound, not a dodge.** XSPICE needs `EVTbackup` because it
drains the digital queue **speculatively** into a window the analog solve has not
yet earned. That is a consequence of an ownership choice, not a law of
mixed-signal simulation. Inverted:

- **A6.** Newton retries, gmin/source stepping, pseudo-transient continuation and
  timestep rejection all happen **inside one `react()` call at one value of
  `now`**. Only the *accepted* result becomes a posted event. JLS time never runs
  backwards; only the solver's private `double t` does.
- This is exactly JLS's existing post-a-computed-future-value idiom:
  `Adder.react` posts at `now+propDelay` (`Adder.java:410`); `Memory.react` posts
  at `now+accessTime` (`Memory.java:1396`).

Reusing `PinChanged` means **no new `Payload` record**, therefore no edits to the
16 exhaustive switches. A self-event and a `WireNet`-posted `PinChanged` at the
same tick **coalesce** in `dupCheck` (`SimEvent.equals`, `:162-172`), which is
exactly right: one visit per tick handles both.

**The engine footprint is five lines:**

```java
/** Time of the earliest pending event, or Long.MAX_VALUE if none.
 *  Observational only: it cannot mutate the queue. */
protected final long nextEventTime() {
    SimEvent e = eventQueue.peek();
    return e == null ? Long.MAX_VALUE : e.getTime();
}
```

`Simulator`'s own `runEventLoop` javadoc already sanctions the peek: *"a hook
that needs the upcoming event's time (e.g. stepping) may peek. The queue is only
modified on this thread, so peek-then-poll returns the same event."*

**The step cap, in three increments — and this is Angle 7's contribution, which
supersedes the epoch-speculation design.**

| Regime | Cap | Cost | Covers |
|---|---|---|---|
| **Sensor-only** — the region has **zero** `Dac`s | **∞, unconditionally** | **~0** | HRM; audio-input with `IdealAdc` |
| **Synchronous-`Dac` guard** — every `Dac` is driven only by clocked elements | `min(δ_LTE, next breakpoint, nextEventTime() − t)`, exact because `Clock` self-schedules its next transition (`Clock.java:392,421`), so the earliest possible `Dac` change is already in the queue | **~1 mw** | audio output, R-2R ladders, SAR sequencers, class-D modulators |
| **Asynchronous speculation** — epoch counter, private accepted-point ring, invalidate-and-re-integrate | Cap-S | **deferred, possibly forever** | nothing in the twelve capstones |

A region with no `Dac` **cannot be invalidated by any digital event**, because
the only path from the digital engine into the solver is a `Dac`. That is a
property of the region's *ports*, and it deletes the entire rollback question for
two of the three analog capstones.

The naive conservative cap (`cap = nextEventTime() − t` unconditionally) is
arithmetically fatal and must not be shipped alone: at ~386 reacted events per
simulated clock cycle, a 44.1 kHz `Dac` against a 100 MHz clock is ~2,268 cycles
per sample × 386 = **~875,000 wasted analog visits per genuine boundary event**.

**Digital → analog** is by breakpoint: a boundary input change at tick `t_d` *is*
the breakpoint, and the `Dac` ramp registers a second one at `t_d + trise`.
**Analog → digital** is policy P-b at accepted timepoints. **The analog/digital
timing error is ZERO, not bounded** — the entire horizon/lateness analysis, the
`max_sync_interval` fallback quantum and the declared error bound that a
co-simulation design required are deleted, because JLS's event queue already
contains the exact integer time of every future digital transition and every one
of them is a breakpoint.

**One documented behaviour change.** A region always holds one pending
self-event, so `runEventLoop`'s `!eventQueue.isEmpty()` (`Simulator.java:217`)
never fails while analog time remains, and `BatchSimulator.java:568-570` always
reports `"Simulation Time Limit"`, never `"Simulation: No More Activity"`. That
is correct — it is SPICE's `.tran tstop` — and it is free reuse: `maxTime`/`-d`
**becomes `tstop`**. It must be documented in `batch-interface.md`, not
discovered.

### 2.7 The physical-time-units contract (P4)

`docs/simulation-semantics.md:26-29` is normative: *"Simulation time is a
dimensionless non-negative 64-bit integer (`long now`). Time units are abstract;
nothing binds them to seconds."* Meanwhile `BatchSimulator.java:423` writes
`$timescale 1 ns $end` into every VCD. **The fiction is already in the tree and
already load-bearing on an external tool.** Analog does not create this problem;
it makes it operative, which is an argument *for* fixing it.

> **D-A9. One optional `String timebase` attribute on the `CIRCUIT` block, using
> VCD's own `$timescale` grammar `<1|10|100><s|ms|us|ns|ps|fs>`. Default 1 ps.
> Absent ⇒ dimensionless, exactly as today. `FORMAT 3` iff present.**
>
> **`t_seconds = now × 10^exp`, RECOMPUTED FROM THE INTEGER EVERY TIME, NEVER
> ACCUMULATED.** That one sentence is the whole numerical contract, and it is
> what SPICE cannot have: ngspice's `CKTtime += CKTdelta` accumulates rounding,
> which is why `CKTminBreak` exists and why version-to-version breakpoint
> heuristics exist.

*Why bump-required:* `docs/file-format.md` §5 silently ignores unknown
attributes, which here would mean an old reader misreading every number in the
file. `FORMAT_VERSION = 2` at `Circuit.java:102`; the refusal path is
`Circuit.java:765-769`. **Absent-by-default is what keeps every existing golden
byte-identical**, including `VcdExportGoldenTest`.

**Range, computed:**

| base | 2^63 ticks | 2^53 ticks (double-exact) |
|---|---|---|
| 1 fs | 9,223 s (2.6 h) | 9.0 s |
| **1 ps** | **106.8 days** | **2.5 h** |
| 1 ns | 292 years | 104 days |

Past 2^53 ticks the tick count is no longer exactly representable in a `double`;
device equations must then take a split representation (integer seconds + integer
sub-second ticks). **That limit is asserted, not discovered.**

**44.1 kHz is not exactly expressible on any decimal tick lattice** —
`44100 = 2²·3²·5²·7²` and the `3²·7²` is fatal. Quantisation error: 11.6 ppm at
1 ns, **1.7 ppb at 1 ps**, 2.0e-11 at 1 fs. Take the decimal base for VCD
compatibility (a rational timebase would make 44.1 kHz exact and break the one
reason for choosing the grammar) and **put the ppm figure in the audio
capstone's documentation**, so a student who FFTs their WAV and finds a 1.7 ppb
offset finds it explained rather than files a bug.

P4 is charged to this programme at 2-3 mw but is **owed anyway** by SDF (#89),
Liberty (#87) and SDC (#93), and the roadmap already prices it.

### 2.8 K9: invisible to a first-year drawing an adder

**The current state is worse than "not yet handled" — a GREEN TEST enforces the
violation.** `test/jls/edit/PaletteContractTest.java:44-66`,
`paletteIsTotalOverTheElementRegistry`, asserts exactly one palette entry per
registered `ElementType` outside `{SubCircuit, WireEnd, TestGen}`. Verified at
HEAD: `ElementRegistry` has **35** types and `Palette` has **32** entries. So 22
analog types would land as **22 mandatory buttons on the first-year's toolbar, a
69% palette growth, enforced by a passing test** — and an unregistered type
cannot round-trip through the save format at all.

> **D-A10. Give `PaletteEntry` and `PaletteContractTest` a VIEW dimension, so
> totality becomes "exactly one entry in exactly one view's palette". The analog
> palette group renders IFF the editing context is a `SubCircuit` with
> `impl "analog"` — visibility DERIVED FROM THE MODEL, not from a preference.**

This is strictly stronger than a menu toggle: a first-year cannot reach a state
where the analog group exists, because creating an analog subcircuit is an
explicit named action. And the palette becomes a *correct description of what is
legal*, since D4's refusal set already forbids `Clock`/`Stop`/`Pause`/`Display`/
`SigGen` inside a bound region.

**Ship the K9 ratchet BEFORE the first analog element** (~2 days): assert the
default palette is exactly 32 buttons, assert dialog component names via the
existing `setName` convention, and turn the startup/per-edit numbers into a test.
`docs/virtual-hardware-parity.md:1903-1917` already says *"until that test
exists, K9 is aspiration"*. This makes the ratchet the evidence.

### 2.9 The GUI tax — the 13× decision

This is the binding constraint on the JLS side and it is arithmetic, not taste.

Measured: coverage commons = **2,897 addable-uncovered LINE / 1,476 BRANCH /
14,669 INSTRUCTION**. Source-to-executable ratio **3.02**. `jls.edit` is 24.64%
LINE-covered. Element dialogs median ~220 source lines, renderers ~155.

| Design | Source | LINE units | Uncovered LINE | % of commons |
|---|---:|---:|---:|---:|
| Naive: bespoke dialog + renderer per device (22 × ~375) | 8,250 | 2,732 | **2,059** | **71%** |
| One generic `AnalogDeviceDialog` over the existing `ElementFormDialog` (401 lines, #26) + one `AnalogDeviceRenderer` driven by 22 symbol paths | ~600 | ~200 | ~150 | 5.2% |
| …plus the three genuinely bespoke dialogs | — | — | ~340 | **11.7%** |

> **D-A11. ONE generic `AnalogDeviceDialog` generated from each device's declared
> parameter list, ONE generic `AnalogDeviceRenderer` driven by static symbol
> paths, and exactly THREE bespoke dialogs: the `V`/`I` waveform selector, the
> `B` expression field, and the `SpiceSubcktRef` file chooser + port map + model-
> card inspector.**

The naive design does not fit the CI configuration as written. Settle the risk
that the generic dialog cannot express all 22 devices by **writing the parameter
descriptors for all 22 on paper before any code** — 2 days.

Related: `jls.sim` has only **8 addable uncovered LINE and 3 BRANCH** against its
93.0/92.0/84.5 floor (verified at `pom.xml:449-471`). The five-line
`nextEventTime()` accessor lands in `jls.sim` and would consume 5 of the 8 if
shipped untested. **Rule: full coverage on the same commit**, and the analog
trace type goes in `jls.analog`, not `jls.sim` next to `TraceSample`.

### 2.10 File format, and results

**New element types cost ZERO format version.** In-tree element addition is
~65-82 lines across 12 files; new element *tags* are hard errors in old readers,
which is the correct loud refusal.

**The problem nobody had stated: there is no real-number item kind.**
`docs/file-format.md:123-140` is normative — item kinds are
`int|long|bigint|string|ref|pair|probe|circuit-block`, and *"A reader
encountering anything else where an item kind is expected MUST fail the load"*.
`Attribute.java` declares exactly `IntAttribute`/`BigIntAttribute`/
`StringAttribute`/`OrientationAttribute`; `Element.setValue` has four overloads;
there is no `double` anywhere.

> **D-A12. Every analog parameter is saved as a `String` holding its SPICE
> spelling — `String r "4.7k"`, `String c "10n"`.** Zero format version,
> locale-proof, byte-stable, diff-perfect under D1/D2 (a re-save cannot reformat
> `4700.0` into `4.7E3`), and it is literally the notation every datasheet and
> every vendor `.lib` already uses. The suffix table (`T G MEG K M U N P F`,
> with the classic 1M-vs-1MEG warning) becomes normative in
> `docs/file-format.md`. The parsed double is a transient recomputed on load. One
> shared ~40-line parser serves the dialog, the loader and the `.model` card
> reader, or the message diverges at three surfaces.

A `double` item kind buys nothing and costs FORMAT 3 for every analog file.

**What DOES bump:** P4's `timebase` and D4's `impl`, both owed by other committed
work. **The analog programme rides ONE bump to FORMAT 3 and contributes none of
its own.** The gate is `SubCircuit.saveFormatVersion()`.

`.subckt` references save **path + FNV-1a digest + positional pair terminal map,
body NOT embedded** — D7's "libraries are DATA", and embedding a vendor
macromodel would import its redistribution terms. `K`/`F`/`H` cross-references
must use **stable id**, not the dense save-time id, which `Element.java:21-22`
says is "reassigned on every save".

**Results: three outputs, exactly one goldened.**

| Form | Flag | Consumer | Goldened? |
|---|---|---|---|
| **Tick-resampled trace** | `-analog-csv` | CI, plotting, anything | **YES — the only goldened analog artifact** |
| ngspice rawfile (no `Date:`/`Command:`) | `-analog-raw` | GTKWave, PySpice, any SPICE plotter | no, and cannot be |
| Digital VCD | `-vcd` (unchanged) | existing graders | **YES — unchanged, byte-identical** |

**Why tick-resampling is the right golden, and this is the crux.** Measured
first-hand: an ngspice `.tran 10u 200u` requesting 20 print points produced
`No. Points: 74` — the writer emits *accepted internal timepoints*, ramping from
1e-11 at the initial discontinuity. **The raw row set is LTE-determined and can
never be a byte-identical golden.** Goldening on it would make every analog test
fragile against precisely the improvements the project would most want (Gear-2,
variable order, better LTE).

Tick-resampling makes the analog golden a function of the tick lattice and the
solution, independent of the timestep controller — **by the identical argument
that makes policy P-b do the same for the digital stream. So both halves of an
analog run are goldened against the same invariant, and that invariant is JLS's
integer time, the property this study repeatedly treated as analog's blocker.**

The interpolation formula and its evaluation order
(`v0 + (v1-v0)*(t-t0)/(t1-t0)`, stated exactly) must be written into
`docs/simulation-semantics.md` **before the emitter exists**, or two
implementations of "linear interpolation" will differ in the last bits. One day
now; a week to retrofit.

**What is NOT built: an in-JLS analog waveform canvas.** The rawfile emitter makes
GTKWave and every SPICE plotter the analog view for **zero JLS GUI code and zero
draw on the commons**. The one exception worth ~0.5 mw is a real-valued row in
the *existing* signal-trace window, as a courtesy during interactive runs.

**WAV is not a nicety.** It reuses the same tick-resampler — one implementation,
two consumers — and for the transistor-level audio rung it is the *only* viable
output (§7.2).

---

## 3. THE SCOPE

### 3.1 Analyses IN

`.op` (with the full escape ladder), `.tran` (trapezoidal, later Gear-2 with
order selection), `.dc` sweep (loop over the ladder with warm starts), `.ac`
small-signal (complex MNA, one complex LU per frequency, no Newton — measured to
match the analytic RC transfer function to **8 significant figures** across five
decades at 4.93 µs/point; deferred to the tail but IN).

### 3.2 Devices IN — sixteen models, and one scope lever worth ~60%

**Tier 0, no `.model` card:** `R C L K V I E G F H B`, plus **`POLY(n)` on
`E F G H`**, plus waveforms `DC AC PULSE SIN PWL EXP SFFM AM`.

**Tier 1, `.model`-carded:** `D` (Spice3f5 level 1), `Q` (Gummel-Poon),
`M` level 1 (Shichman-Hodges), `J` (JFET), `S`, `W`.

**Tier 2, structure:** `.subckt`/`.ends`/`X` nested with node scoping, `.model`
card reading with level dispatch, `.include`/`.lib`, `.param`.

Four scope findings that change the shape:

1. **`POLY(n)` is MANDATORY and was missing from the first plan.** 13 POLY cards
   appear in ngspice's own `examples/TransImpedanceAmp/output.net`, and all three
   Analog Devices macromodels in it use it. Without POLY, JLS cannot read the
   majority of real vendor op-amp macromodels, which falsifies "op-amps are free
   via `.subckt`". Implement it **natively inside `E F G H`** — ngspice rewrites
   POLY into XSPICE A-device code models and, on an OP177A run, printed
   *"Reducing trtol to 1 for xspice A devices"*, silently degrading its own
   timestep tolerance as a side effect. A polynomial is smooth and trivially
   differentiable; there is no reason to inherit that detour. 0.5-1.0 mw.

2. **Ebers-Moll and Gummel-Poon are ONE model.** `bjt/bjtload.c:565-566` builds
   `q1` from *inverse* Early voltages and guards the `q2` branch with
   `if (tinvRollOffF == 0 && tinvRollOffR == 0)`. Absent VAF/VAR/IKF/IKR ⇒
   inverses 0 ⇒ `q1 = 1`, `qb = 1` ⇒ pure Ebers-Moll. **Building both would waste
   3-6 mw.** Ship Gummel-Poon; expose Ebers-Moll as a documented week-1 parameter
   tier (IS, BF, BR only).

3. **Implement Spice3f5-era model cards, not ngspice-current ones.** Measured:
   ngspice's diode model card has **88 settable model parameters**; Spice3f5's
   had 14, and SpiceSharp's `Diodes/ModelParameters.cs` has 15. The extra 74 are
   level-3 JUNCAP, tunnelling, recombination, self-heating, temperature
   coefficients of temperature coefficients, and safe-operating-area limits.
   **Size against SpiceSharp, not against ngspice.** Teaching subsets: diode 12
   of 88; BJT ~28 of 143 tiered by week; MOSFET L1 ~14 of 36; JFET ~9 of 26.
   **That tiering IS K9 progressive disclosure applied to model cards.**

4. **`J` (JFET) is ADDED to the brief's list.** Every electret condenser capsule
   contains an internal JFET source follower, and that is most of the pedagogy of
   the audio-input front end. 1.5-3.0 mw — the cheapest of the four
   semiconductors. Dropping it saves the weeks and costs the student the reason
   the bias resistor to Vcc exists.

**And one place this determination contradicts the brief.**

> **D-A13. MOSFET Level 3 is DEFERRED, not shipped in v1.** Reclaims 1.5-3.0 mw.
> Evidence: none of AUDIO OUT, AUDIO IN, HEART RATE MONITOR, breadboard CPU or
> PCB uses it; across 174 ngspice example files `level=1` appears 26 times and
> `level=3` only 4, while `level=49/54` (BSIM3/4) appears 24 times and only
> inside the Skywater and IHP open-PDK examples — i.e. the tape-out capstone,
> which needs BSIM4 and is out of scope regardless. Its convergence is materially
> worse than level 1 (Vdsat contains a sqrt whose argument can go negative under
> a Newton excursion; NFS>0 splices a subthreshold exponential with a
> discontinuous second derivative). **Adding it later costs nothing extra:** the
> shared MOSFET Common layer (bulk diodes, Meyer capacitances, limiting,
> temperature) is built for level 1, and level 3 drops in beside it. **This needs
> a maintainer call, not a study decision.**

**Op-amps cost ZERO incremental model work.** Measured: ngspice's own TIA example
— an optical receiver built from unmodified Analog Devices macromodels (AD8009,
OP177A, AD780A) — ran clean and histograms to R 66, D 35, V 34, C 26, G 19, F 14,
E 13, I 12, Q 11, X 5, L 3, with `.model` kinds npn/pnp/d only and 13 POLY cards.
**Zero MOSFETs, zero switches, zero transmission lines, zero BSIM.** That is
exactly the v1 set. Ship a *behavioural* op-amp element too (0.8-1.5 mw), purely
for K9 progressive disclosure — and note the measured gap that is itself the
teaching content: OP177A and a 5-primitive behavioural op-amp in the same
closed-loop ×11 are identical to 1 kHz (20.83 dB both) and diverge at 100 kHz
(17.37 dB behavioural vs 15.03 dB vendor). **Do NOT invent an op-amp `.model`
type.**

> **D-A14. JLS reads vendor `.model`/`.subckt`/`.lib` files directly, as DATA,
> and curates NO library.** This is D7 applied literally: students download the
> manufacturer's model for the exact part they will solder; JLS ships zero vendor
> files and takes on zero redistribution licensing; the library grows at zero
> maintainer-weeks under bus factor 1. Java floor is measured: jspice implements
> `.include`/`.subckt`/`.ends`/`.model`/`.param`/`.tran`/`.step` in **664
> non-blank Java lines**. ngspice's ceiling with full dialect compatibility is
> 15,300, almost all of it PSpice/HSPICE/LTspice quirk-chasing JLS must not
> chase. **Target Spice3f5 + ngspice spellings, DOCUMENT the target, and emit
> "unsupported construct LAPLACE from PSpice dialect" rather than a syntax
> error.** 5.5-11.0 mw total.

Two rules that make this survivable:

- **PARSE every Spice3f5-era parameter including KF/AF and warn-once-and-ignore.**
  The OP177A model literally contains `.MODEL DEN D(IS=1E-12, RS=14.61K,
  KF=2E-17, AF=1)`; rejecting unknown parameters makes every vendor file
  unloadable.
- **Ship a ~200-line model-card inspector** that reports, per library file, which
  subcircuits are fully supported and which need an unimplemented model:
  *"this .lib defines 14 subckts and 31 models; 12 are fully supported; AD8620
  needs MOSFET level 2; OP2177 uses TABLE, PSpice dialect."* It converts the
  commonest failure mode into a teaching moment, before simulation starts.

### 3.3 What is OUT

| Out | Arithmetic | Re-entry path, priced |
|---|---|---|
| MOSFET **level 3** | needed by nothing in the twelve capstones | drops into the L1 common layer unchanged — **1.5-3.0 mw** |
| BSIM3/4, HiSIM, SOI, VBIC, HICUM, MEXTRAM, PSP, VDMOS, MOS levels 2/6 | **BSIM4 alone is 25,006 SLOC declaring 897 model + 98 instance parameters in one 1,087-line file** — at this study's own rate that is 47-125 mw for ONE device, more than the entire analog programme | Door 2 then Door 3, below |
| Transmission lines `T O U P` | LTRA is 2,460 SLOC for a capability no capstone names | lossless `T` — **1.5-3.0 mw** when the PCB capstone asks |
| `.noise .disto .sp .pz .sens .pss`, harmonic balance, RF | no capstone names them; each multiplies every device by an extra load routine | `.noise` across all six models — **3-5 mw** |
| Self-heating, Monte-Carlo, `.temp` sweeps, CIDER TCAD | — | not priced; nothing asks |
| A full SPICE deck front end | `inp.c` 2,884 + `inpcom.c` 10,221 + `subckt.c` 2,200 ≈ 15,300 lines, almost entirely dialect compatibility. **And it is not needed** — JLS builds analog from drawn elements | Qucs `parse_spice.ypp`, a complete 972-line Bison grammar, GPL-2.0-or-later — **~2 mw** if a `.cir` reader is ever wanted |
| KLU sparse solver | refused on **scale, not principle** — LGPLv2 and absorbable, but its advantage appears at thousands of unknowns and every capstone circuit measured is 7-28 equations | revisit trigger: **a circuit over ~2,000 unknowns** |
| `libngspice` / ngspice subprocess **in `src/`** | §1.2 | none wanted; ngspice stays a CI oracle |
| An in-JLS analog waveform canvas | nearly all custom painting, nearly all branches, against 2,897 LINE of commons | GTKWave/Surfer via `-analog-raw` — **0 mw** |
| Live streaming of transistor-level class-D audio | 11.19 M accepted timepoints per second of audio ⇒ **2.8-4.9 minutes of Java compute per audio second** (measured) | render to WAV; the *linear* rungs are real-time (§7.2) |
| **Bypass, adaptive matrix reuse, symbolic-factorisation reuse across topology changes, any parallelism in the solve** | all introduce path-dependent state; bypass makes the answer depend on the history of tolerance comparisons | **forbidden in v1 by written rule.** Each needs its own byte-golden proof if profiling later demands it. Cheaper to forgo than to prove |

**The three re-entry doors, priced (D10 rule 5):**

- **Door 1 — the `B` source, already in v1.** Any `I = f(V,t)` or `V = f(I,t)`
  device is expressible with correct Jacobians: thermistor, photodiode
  responsivity, memristor, solar cell, varactor, nonlinear speaker suspension.
  **Cost already paid.**
- **Door 2 — a Java compact-model SPI** registered through the shipped
  `jls.module` `ExtensionPoint`/`ExtensionRegistry`. A module is a jar, not a
  native library, so the single offline jar survives. **3-6 mw, and it should NOT
  be built until a second consumer exists** — the interface you design before you
  have two consumers is the wrong interface.
- **Door 3 — a Verilog-A subset compiler targeting Door 2.** Parse the analog
  block, symbolically differentiate it (*the same machinery the `B` source
  already needs*), emit an interpreted tree or generated Java. **8-15 mw.** This
  is the door that scales to arbitrary third-party compact models, and it is the
  named replacement for OSDI, which fails on `dlopen`.

**Note the naming reconciliation owed:** D7 names `jls.api` as the extensibility
story, but there is no `src/jls/api/` at HEAD — the shipped runtime is
`src/jls/module/`. Reconcile before anyone plans against `jls.api`.

---

## 4. THE DETERMINISM ANSWER

> **D-A15. BYTE-IDENTICAL, as the required gate. AND tolerance-based, as a
> separate required layer. They are different tests and neither replaces the
> other: a byte-identical golden pins JLS against itself and says nothing about
> correctness; a tolerance test against a closed form says nothing about
> regression at the 1e-12 level.**

### 4.1 The five controls, beyond `strictfp`

JEP 306 (JDK 17) makes all FP arithmetic strict — no extended precision, no
permitted reassociation, and, unlike C, **no FMA contraction**. `pom.xml:43`
pins release 25, so this is already in force and `strictfp` is a no-op. On top
of that:

| | Control | Enforcement |
|---|---|---|
| **D-1** | `StrictMath` only inside `jls.analog`. `Math.sqrt`/`abs` permitted (IEEE-exact; measured 0 divergence in 2M samples) | ArchUnit rule — the repo already depends on ArchUnit, and `grep -rn StrictMath src/` returns **0** at HEAD, so the rule starts clean. Highest value-per-line test in the programme; under a day |
| **D-2** | No parallelism inside the solve | ArchUnit + written rule. FP addition is not associative |
| **D-3** | No hash-ordered iteration reaching the matrix. **Stamp order IS accumulation order.** `Circuit.elements` is a plain `HashSet` (`src/jls/Circuit.java:48`) — elaborate into an index-ordered array once, in stable-id order (D-A6) | ArchUnit + the elaborator's contract |
| **D-4** | A **totally ordered** pivot tie-break (Markowitz product, then \|value\|, then row, then col; strict `>` so the lowest row wins), asserted on the permutation vector | test. **This is the biggest un-derisked determinism item** — see §8 |
| **D-5** | Every adaptive decision a pure function of state — no wall clock, no random, no "convergence has been slow lately" | ratchet test in the `SocketConfinementRatchetTest` idiom |

**Plus two inherited hazards that must be closed first, in hours:**

- `ElementId.mintFresh()` derives a per-install random 32-hex replica id and
  `getElementsInStableOrder()` sorts on it. CI never pins `JLS_REPLICA_ID`. This
  is benign today only because every committed fixture carries `legacy:N` ids and
  `'l'` sorts after every hex digit. **In an analog kernel the device iteration
  order IS the floating-point accumulation order.** Pin `JLS_REPLICA_ID` in
  `ci.yml` and in the analog test base before the first analog fixture lands.
- **Never `Double.toString` in a golden** — its output changed in JDK 19
  (JDK-8291475). Use `Double.toHexString` or a digest over `doubleToLongBits`.
  Canonicalise ±0.0.

**And one absorbed-code hazard, verified not hypothetical.** CircuitJS1's
`OpAmpElm.java:176,179` calls `app.getrand(4)` — **unseeded `java.util.Random`
(`CirSim.java:110-115,201`) inside the Newton `doStep()`** — as a saturation-
branch tie-break; `GateElm.java:262` does the same. **Do not absorb `OpAmpElm`.**
Build the behavioural op-amp as a stamped RC-macromodel equivalent, which has no
branch to oscillate between and therefore needs no tie-break. Budget **1.0-2.0
mw** of audit over every absorbed file for `java.util.Random`, `Math.*`,
wall-clock, hash iteration and parallel reduction, and make the lint a **merge
gate**, not a later check.

### 4.2 The four-tier golden discipline

Conflating the pinning job with the validating job is what makes people think
analog and byte goldens are incompatible. They are four different tiers.

**T1 — BYTE (required gate).** Full waveform record as raw IEEE-754 hex, exact
compare. Four record-format rules learned the hard way in the experiment:

- (a) emit `doubleToRawLongBits`, not formatted decimal;
- (b) **record `h` explicitly, NEVER infer it from the time column** — measured,
  accepted steps double exactly but differences of accumulated times are 1 ulp
  off (`2.980232238769531e-15` → `5.9604644775390614e-15`);
- (c) put `steps/rejects/nrIters/nrFails/points` in the header;
- (d) write expected values as the same expression the kernel computes —
  asserting `A[0][0] == 0.02` for `2.0*1e-6/1e-4` **fails**, because the kernel
  produces `0.019999999999999997`.

**T2 — PHYSICS (required).** Solver vs closed form, tolerance **derived** from
RELTOL/VNTOL/ABSTOL with the derivation in a comment. A bare numeric literal
tolerance is a review defect. Two anti-cheat assertions: assert the numerical
error is **nonzero** (a solver returning the analytic answer is not
integrating), and assert disagreement with an "independent" oracle has a **lower
bound** (agreement to 1e-15 means the oracle is not independent).

**T3 — STRUCTURE (required; the load-bearing new idea).** Exact per-device MNA
matrix and RHS assertions, entry by entry, on raw bits. **This is the only layer
that can reach anything whose effect is below physical tolerance.** Canonical
case: `GMIN = 1e-12`, where flipping `+GMIN*vd` to `-GMIN*vd` survives every
waveform test at every tolerance and is killed only by evaluating the diode at
`vd = 0`, where the exponential vanishes and GMIN is the whole conductance.

**T4 — EXTERNAL (nightly, non-gating).** ngspice at a **1e-4 relative envelope**
— two decades above ngspice's own build-to-build noise (~1e-13 measured) and two
decades below real model error (~1e-2). Best-effort apt install alongside
`iverilog`/`ghdl`/`yosys`/`xvfb`; `ProcessBuilder` in `test/` only; self-skip
when absent; `continue-on-error` lane promoted per-device-family after 20 green
runs. **Unpinned version, deliberately** — a pinned oracle rots, a loose oracle
keeps working. Do not tighten below ~1e-8 without pinning.

### 4.3 Detecting a regression that stays inside tolerance

Five detectors; the first two are free and should be non-negotiable.

- **R1 — pin solver statistics exactly in the golden header.** A run that keeps
  `v(t)` within 1e-6 but takes 20% more Newton iterations is a convergence
  regression invisible to every waveform compare. **12 lines to pin.**
- **R4 — store the MEASURED analytic error in the header and ratchet it**, so a
  move from 6.5e-7 to 3.1e-6 is visible even though both pass. This also makes
  golden regeneration self-policing: a regeneration that improves diff-noise
  while worsening the physics is **visible in the diff** and rejectable on sight.
- **R2** — the T3 stamp tier for sub-tolerance terms.
- **R3** — report `‖Ax−b‖∞` as a first-class output.
- **R5** — keep two or three fixtures deliberately near a convergence limit.

**Step-grid flips:** compare header statistics first; if `steps`/`rejects` match,
the grids match and pointwise compare is valid (the common case — measured, the
grid survived a 1e5-ulp perturbation). If they differ, resample both onto the
fixture's declared `tstep` and compare with per-signal tolerances, and **report
the flip as a distinct outcome, not a pass or a fail.**

**Why tolerance goldens are meaningful at all, measured:** error propagation in
the circuit classes the capstones need is **linear, not chaotic**. Perturbing a
rectifier's 1 kΩ load by 1/2/8/1,000/100,000 ulps gave worst deviations of
2.118e-22 / 4.235e-22 / 1.694e-21 / 4.841e-14 / 1.784e-11 — exactly linear — with
the accepted-step count unchanged at 5,785 in every case. Dissipative circuits
(amps, filters, rectifiers, ECG front ends) have left-half-plane Jacobians, so
perturbations decay and a deviation beyond tolerance is a real change.
**Three stated exceptions:** self-oscillators accumulate phase error linearly
forever (compare in the period/frequency or RMS domain); chaotic circuits (Chua)
make pointwise comparison meaningless at any tolerance; circuits at a bifurcation
(Schmitt at threshold) **do** flip the accept/reject decision — which is why the
grid-flip detector exists.

### 4.4 The PIT / JaCoCo answer for a numerical kernel

**No governance exemption is needed, and none should be requested. Asking for one
would be the weaker move on the evidence.**

Measured, in a standalone Maven project using the repo's own configuration
(pitest-maven 1.25.8, pitest-junit5-plugin 1.2.3, default mutators, `threads=4`),
against the repo's `mutationThreshold` 80 / `testStrengthThreshold` 82 (verified
at `pom.xml:812-813`):

| Test suite | Mutation | Test strength | Verdict |
|---|---:|---:|---|
| waveform assertions only (25 tests) | 76% | 79% | **fails 80/82** |
| **+ per-device MNA stamp goldens (29 more tests)** | **86%** | **88%** | **passes, +6/+6 headroom** |

278 mutants, 1m24s, 99.9% line coverage. The pom's convention requires ≥2 points
of headroom under the worst of three clean runs; six points on both counters
clears that on a first attempt.

**The number alone does not transfer the knowledge.** The governance deliverable
is one sentence in `CONTRIBUTING.md`:

> *A new analog device model lands with an exact per-terminal MNA stamp test
> before it lands with a waveform test.*

That sentence is worth more than the threshold, because it is what makes the
threshold reachable and a contributor cannot infer it from the number.

**Three concrete governance recommendations:**

1. **Package placement: `jls.analog`.** JaCoCo `<element>PACKAGE</element>`
   matches exactly, so a new package is **unfloored by default**, like
   `jls.edit`. And `jls.sim.analog` would be *outside* the JaCoCo `jls.sim` rule
   but *inside* the PIT `jls.sim.*` glob (verified at `pom.xml:780-786`) — an
   asymmetry that will confuse someone. Use `jls.analog` with **explicit new
   rules for both tools**.
2. **Set the analog floors from the FIRST measurement and ratchet — not
   `jls.sim` parity on day one.** This is the pom's own documented convention
   ("these only ever move UP … from a fresh headless canonical-JDK-25
   measurement, keeping ≥2pt headroom"). Landing under a 93/92/84.5 floor from
   commit one converts every incremental PR into a coverage emergency. **This is
   the one thing that looks like an ask, so state it plainly: it is not an
   exemption, it is a new floor set by the existing ratchet rule.**
3. **File equivalent mutants as identifications with no exclusions**, following
   `docs/mutation-testing-trial-2026-07.md` §4b. Keeps the score honest and the
   floor set under the achieved number. One genuinely equivalent mutant found in
   the experiment — `A[k][k] -= 0.0` in the DC inductor branch, a deliberate
   no-op written for symmetry — should be **deleted rather than excluded**.

**The honest residual:** 38 survivors remain at 86% (25 `CONDITIONALS_BOUNDARY`,
9 `MATH`, 2 `NEGATE_CONDITIONALS`, 1 `NULL_RETURNS`, 1 `VOID_METHOD_CALLS`).
**86% is where a first pass lands, not a ceiling.** I make no claim that 95% is
reachable and would not promise it. Plan against a band of **80-86**, and
re-measure at each device-family milestone.

**The fixture design that makes it work:** every device kind must be stamped in
**three topologies** (terminal→gnd, gnd→terminal, node1→node2, with node index 0
appearing at each terminal), plus boundary tests **at** and **above** each
limiting threshold. That is a fixture matrix, not a cleverness problem.

### 4.5 CI time — analog is cheap; the required gate is not the problem

Measured: required gate `Build (Linux, JDK 25)` = **141 s**; whole 16-job run
7m02s; no `timeout-minutes` anywhere. Dense LU in Java: 0.09 / 0.39 / 0.79 /
4.47 / 18.69 / 80.76 / 355.00 µs at n = 5 / 10 / 20 / 50 / 100 / 200 / 400.
2.4-2.5 Newton iterations per accepted step.

A bounded ~205-test analog corpus (every fixture under 20,000 steps, n ≤ 20)
budgets to **~4 s = +3% on the gate**, inside its existing run-to-run variance —
and it is **self-enforcing**, because R1 already pins step counts.

- **What must NOT go in the required gate:** audio-length transients (1 s of
  audio at a 1 µs max step is 1e6 steps ≈ 4 s each). A **new nightly
  `analog-long` lane** takes those, the ngspice oracle and the cross-platform
  matrix. That lane does not exist today and analog is its honest first occupant.
- **PIT** on a real kernel will generate 8,000-12,000 mutants ≈ 45-65 min. That
  is fine for the existing **schedule-only** weekly `mutation.yml` and
  intolerable in a PR check — which is why `mutation.yml` is already
  schedule-only. **No change to PIT governance is needed.**
- **The one CI change that IS required:** a **REQUIRED** job running *only* the
  analog byte-golden suite on Windows, macOS and aarch64. The tree has **no
  run-vs-run simulation determinism test anywhere** (all 30 `runSim()` call sites
  assert against literal constants) and cross-platform lanes are
  `continue-on-error` today. A byte-identical claim enforced on one platform is
  not enforced. The analog golden suite is seconds of headless work with no GUI
  or filesystem surface, so it should be materially more stable than the
  full-verify lanes it sits beside. Add the run-vs-run equality test alongside it.

---

## 5. THE STAGED PLAN

Ordered by demo-value per maintainer-week, subject to real dependencies. **Every
stage ends in something a student can see, hear or plot.**

### S0 — HEAR IT, WITH NO SOLVER — 3-4.5 mw. Depends: nothing.

`HostAudioSink`/`HostAudioSource` as in-tree `LogicElement`s over
`javax.sound.sampled` (measured working on JDK 25, zero external dependencies —
it is `java.desktop`, already on a Swing application's classpath, and there is no
`module-info.java` in `src/` to widen); a PCM WAV codec; **the tick-resampler
that every later analog output reuses verbatim**; two batch flags (`-wav`,
`-audio-in`); the D7 one-door-at-invocation grant; the
`docs/extension-points.md` row; the `SECURITY.md` paragraph; and the **explicit
reconciliation of `docs/vcd-interop.md:19-24`**, which rejects live
co-simulation under #63.

**Demo:** draw a counter + a `Memory` holding a wavetable + a `Register`, run
headless, **play the WAV**. Measured ceiling ~209,000 samples/s against 44.1 kHz
— real time with 4.7× margin.

**Why first.** It produces a capstone-shaped artifact before any analog code
exists; the work is 100% reused; and **it retires the one governance question in
the whole programme — does the host door survive review? — in week 4 rather than
week 25.** `grep -rn "System.in" src/` returns 0 at HEAD; this is the first
read-side host door in JLS's history. If it is refused, the audio capstone pair
changes shape and the maintainer finds out for 3-4.5 mw instead of 30.

### S1 — THE SOLVER, HEADLESS, AND THE CALIBRATION — 3.5-5 mw. Depends: nothing.

`jls.analog`; `R L C V I D` stamps; dense LU with partial pivoting; Newton with
`pnjlim` junction limiting; gmin + source stepping; trapezoidal with a **linear**
predictor and predictor-corrector LTE; `.op` and `.tran`; a digest golden
harness. **Plus the linear fast path** (+0.5-1.0 mw): an elaboration-time
`isLinear()` predicate and a factorisation cache keyed by step size, which is
what makes S7 real-time and is far cheaper to design in than to retrofit around a
Newton loop.

**Demo:** an RC step response and a full-wave rectifier, plotted from a CSV — a
complete first-year "the world is not digital" lab, in month two. A 328-line Java
spike already does this: RC at its corner gives 0.707104 against an analytic
0.70710678; the rectifier gives a 4.274730 V peak from 5 V (a 0.725 V junction
drop) with 0.371 V ripple, in 24,881 timesteps with 10 rejections.

**S1 IS ALSO THE CALIBRATION EXPERIMENT, and this is mandatory.** Take it to the
full 93.0/92.0/84.5 JaCoCo + 80/82 PIT gate, **measure the weeks**, and re-cost
everything downstream proportionally. Also measure, in the same pass: the
`StrictMath.exp`-vs-`Math.exp` cost *inside the device `load()` loop* (§8), the
PIT cost on one hand-written diode model, and the factorisation-cache hit rate on
a PWM carrier.

**Sequencing, stated because D10 rule 5 demands real dependencies: S1 has NO
dependency on P4.** The solver owns its own time internally; P4 is needed only at
S5. Doing S1 first costs zero extra and de-risks the programme.

### S2 — THE DETERMINISM GATE — 4-6 mw. Depends: S1. **Week 8.**

The ArchUnit rule banning `java.lang.Math` from `jls.analog` (starts clean:
`grep -rn StrictMath src/` = 0) plus the no-parallelism and no-hash-iteration
ratchets; `JLS_REPLICA_ID` pinned in `ci.yml` and the analog test base; hex-float
and digest golden writers with an **independent spec-derived reader** (the
`VcdExportGoldenTest` idiom); the resample-on-grid-flip comparator; the
run-vs-run equality test the tree lacks entirely; `docs/analog-determinism.md`
stating D-1..D-5 and the golden regeneration protocol; and **the 4-platform ×
2-JDK CI matrix**.

**Demo:** a green matrix showing byte-identical `.tran` output on
Linux/macOS/Windows × x64/aarch64. No student sees it. **It is the go/no-go for
the entire justification, and it is falsifiable in week 8 for 4-6 mw rather than
week 40.**

### S3 — CONTROLLED SOURCES, WAVEFORMS, `.model`/`.subckt` — 3-4 mw. Depends: S1.

`E F G H` stamps with native `POLY(n)`; `S`/`W` switches; `PULSE PWL SIN EXP
SFFM AM`; `.dc` sweep; and a **small** card grammar (`.subckt`/`.ends`/`.model`/
`.param`/instance lines) — **not** a full SPICE deck parser. Plus the model-card
inspector.

**Demo:** an inverting amplifier built from a **vendor op-amp `.subckt`** and a
Sallen-Key filter's step response. **Op-amps arrive as DATA (D7) at zero
maintainer-weeks of model library — the highest-leverage single item in the
programme after S0 and the linear fast path.**

### S4 — P4 PHYSICAL TIME BASE — 2-3 mw. (Owed anyway by SDF/Liberty/SDC.)

`timebase` on `CIRCUIT`; FORMAT 3 iff present; the no-accumulation conversion
class with the 2^53 assertion; the tick-quantisation rule with its stated ppm
figure; `simulation-semantics.md` §1 rewrite.

**Demo:** the VCD stops being a fiction — a circuit declaring `timebase "1ps"`
emits `$timescale 1 ps $end` and every existing golden is untouched.

### S5 — THE HEART RATE MONITOR, END TO END — 11-16 mw. Depends: S3, S4.

| Piece | mw |
|---|---:|
| `Put.element` widened to `Element`; `AnalogElement` as the 4th permit; the `instanceof Reacts` guard; the 8 `instanceof LogicElement` decisions | 1.5-2.5 |
| The `PortAlphabet` domain mechanism — four `SimpleEditor` sites **above** the width check, `WireNet.recheck` validate-don't-widen, elaboration assertion, the `infoText` arms (shared with MVL and P1) | 2.45 |
| Analog `SubCircuit` binding (`impl "analog"` as a third `SubCircuitImpl` permit), the elaborator, node partition in stable-id order, `Ground` + its two singularity diagnostics | 2-3 |
| `Adc` + `Dac` — policy P-b bisection, ramped D2A, `vhiz`, `rout` | 2-3 |
| Sensor-only cap + synchronous-`Dac` guard + `nextEventTime()` | 1 |
| 8 drawn devices (`Ground R C L V I D SpiceSubcktRef`) on ONE generic dialog + ONE generic renderer | 2-3 |
| `PaletteEntry` view dimension + context-derived visibility (K9) | 1 |
| Diagnostics naming drawn elements, not matrix rows | 0.5-1 |

**Demo:** a **drawn** photodiode → transimpedance amp → 0.16 Hz high-pass → ×101
gain → 5 Hz Sallen-Key → comparator → `Adc` → a student-written digital beat
counter printing BPM, in real time, from a recorded PPG waveform. **The first
complete analog capstone.**

**This is the expensive, indivisible stage.** Everything before it is
infrastructure; everything after it is increments on a working system. See §8 for
the kill criterion attached to it.

### S6 — AUDIO INPUT — 2-3 mw. Depends: S5, S0.

`IdealAdc`/`IdealDac`; `HostAudioSource` wired to the analog side; the
op-amp-macromodel preamp — **no transistors needed**.

**Demo:** play a WAV into the circuit, watch the preamp output on the trace,
44.1 kHz samples land in a JLS digital circuit and come back out as a WAV.
Measured ~0.84 s of Java per second of audio.

### S7 — AUDIO OUTPUT, DRAWN AND AUDIBLE — 2-3 mw. Depends: S5, S0, S1's fast path.

The drawn 8-bit R-2R ladder and the drawn LC reconstruction filter fed by a
digital modulator; the `Dac`-edge breakpoint path; the synchronous-`Dac` guard.

**Demo:** a drawn R-2R DAC and a drawn LC filter **playing a 440 Hz tone through
the speakers in real time**, with a live trace on the filter node. Measured
0.72-0.88 s of Java per second of audio on the linear fast path. **This is the
demo that sells the programme, and it exists only because of S1's linear fast
path.**

### S8 — TEST CORPUS AND THE NGSPICE ORACLE — 7-11 mw. Depends: S2.

Tier A: 10 closed-form fixtures with derived tolerances plus the nonzero-error
and lower-bound-on-disagreement assertions. Tier C: 5 invariants (reciprocity,
Tellegen power balance, charge conservation, superposition, node-ordering
invariance). Tier B: 11 convergence-torture fixtures with pinned statistics
(series diodes, bridge with no DC path, bistable latch, Schmitt swept through
threshold, 1e6-gain op-amp, ideal switch at a breakpoint, 1e9 time-constant-ratio
stiff RC, floating capacitor, zero-valued R/L/C, relaxation oscillator, charge
pump). The ngspice oracle: netlist emitter, `-b` runner with date-line stripping,
ASCII parser, 1e-4 comparator, self-skip, nightly `continue-on-error` lane with
per-family promotion. CI wiring, the nightly `analog-long` lane, the required
cross-platform analog-goldens-only job, and the `jls.analog` JaCoCo/PIT floors.

**Demo:** a green nightly matrix showing JLS and real ngspice agreeing to 1e-4 on
every Tier A and Tier B fixture — the strongest available evidence that the Java
solver is *correct*, not merely *reproducible*.

### S9 — TRANSISTORS: BJT + MOSFET L1 + JFET — 7-11 mw. Depends: S5, realistically S10.

Gummel-Poon `Q` (Ebers-Moll free), Shichman-Hodges `M` level 1, `J`, with charge
storage, temperature and the full limiting family (`fetlim`, `limvds`, `limvgs`,
`limvbs`). **MOSFET level 3 dropped** (D-A13). Includes the stamp-golden tonnage
in three topologies per device.

**Demo:** class-D audio with a real MOSFET half-bridge and the **THD lab** — move
dead time, carrier frequency and filter corner, watch THD change. Renders offline
at a measured 2.8-4.9 minutes per second of audio, **and the docs say so.** Also
unlocks the real electret capsule and the fully-drawn SAR converter.

### S10 — CONVERGENCE HARDENING — 6-10 mw. Depends: S3.

Dynamic/Gillespie gmin, **pseudo-transient continuation** (`OPtran` — the rung
reimplementations omit and the only one that converges a ring oscillator's
`.op`), the per-device `convTest` **veto** (a node voltage can be stationary
while a device current is not), the 200-circuit hard corpus, and diagnostics that
name the **offending drawn element** rather than "matrix singular at row 7".

**Demo:** nothing new is visible; things stop failing. **This is the stage that
converts "a tool that runs the four circuits in this document" into "a tool a
course can assign homework on", and it must not be compressed.** The measured
contrast that justifies the band: the HRM runs at 2.00 Newton iterations per
timepoint with 1 rejection in 10,012; a diode-bridge-plus-astable at the *same
circuit size* runs at **20.4 iterations/timepoint with 15.1% rejection**. A 10×
spread from the models alone. The *code* is one week — the entire limiting
apparatus is **156 lines of Modified-BSD C** (`devsup.c:20-185`) — and the
validation loop is the rest.

### S11 — THE FULL DRAWN PALETTE — 8-12 mw. Depends: S5, S9.

The remaining 14 device types (`K E F G H B Q M J S W AnalogProbe`, POLY
support) plus the three bespoke dialogs. **Deliberately low demo-value per
week** — the capstones are already delivered.

**Demo:** every analog device a teaching course needs, drawn.

### S12 — THE DEFERRABLE TAIL — 10-16 mw.

Sparse LU + Markowitz with the specified total-order tie-break (4-6 — the largest
single deferral: measured, dense LU is 3.765 µs at N=28 and 13.839 µs at N=50,
and every capstone circuit measured is 7-28 equations; sparse buys 27× at N=50
and 770× at N=1600, so the trigger is a real circuit over ~100 unknowns);
Gear-2 and variable order (3-4); `.ac` (1.5-2); the rawfile emitter without the
`Date:` line (0.5); normative docs, the `extension-points.md` rows, and the
`ARCHITECTURE.md` decision block with a revisit trigger (2).

**Demo:** Bode plots; circuits over 100 unknowns; GTKWave as the analog waveform
view for zero JLS GUI code.

### 5.1 The cumulative line

| Stage | mw | Cumulative | Demo |
|---|---:|---:|---|
| **S0** | 3-4.5 | **3-4.5** | **hear your circuit** (no solver) |
| **S1** | 3.5-5 | 6.5-9.5 | RC step, diode rectifier — **and the re-cost** |
| **S2** | 4-6 | 10.5-15.5 | byte-identical on 4 platforms × 2 JDKs |
| **S3** | 3-4 | 13.5-19.5 | op-amp filters from a vendor `.subckt` |
| **S4** | 2-3 | 15.5-22.5 | the VCD stops lying |
| **S5** | 11-16 | **26.5-38.5** | **the PPG beat detector, drawn, real time** |
| **S6** | 2-3 | 28.5-41.5 | speak in, samples out |
| **S7** | 2-3 | **30.5-44.5** | **drawn DAC playing a tone, real time** |
| **S8** | 7-11 | 37.5-55.5 | JLS vs real ngspice, green, nightly |
| **S9** | 7-11 | 44.5-66.5 | class-D + THD lab, drawn SAR |
| **S10** | 6-10 | 50.5-76.5 | homework-grade convergence |
| **S11** | 8-12 | 58.5-88.5 | the full drawn palette |
| **S12** | 10-16 | **68.5-104.5** | Bode plots, big circuits |

**Demo-value per week, ranked:** S0 ≫ S3 > S1 > S7 ≈ S6 > S2 > S5 > S9 > S8 >
S12 > S10 ≈ S11. Dependencies force S5 before S6/S7; that is the one place the
schedule and the ranking disagree.

---

## 6. THE CAPSTONE FEED

### 6.1 HEART RATE MONITOR — minimum stage **S5**

**Arithmetic (measured on ngspice-42, converted with Java primitives measured
in the same session).** Chain: photodiode `I(DC 200 nA + SIN 2 nA @ 1.2 Hz) ∥
Cj 50 pF ∥ Rsh 1 GΩ ∥ D` → TIA (op-amp, Rf 1 MΩ, Cf 10 pF) → 0.16 Hz high-pass →
×101 gain → 5 Hz Sallen-Key → comparator.

- **24 circuit equations** (18 solved node voltages + 4 branch currents).
- 10 s of simulated PPG at a 1 ms lattice: **10,012 attempted / 10,011 accepted /
  1 rejected**; 20,022 Newton iterations = **2.00 per timepoint**; 0.053 s in C.
  At a 10 ms lattice, 1,017 accepted, 0.004 s. LTE-free (100 ms cap): **124
  accepted timepoints for 10 s**.
- Java: dense LU copy+factor+solve at N=24 measured **2.727 µs**; device load
  0.80 µs/iteration in C taken at 2-3× → **4.3-5.2 µs per Newton solve** →
  **8.6-10.4 ms of Java per second of PPG at a 1 kHz lattice (96-116× real
  time)**, 0.87-1.06 ms at 100 Hz (~1,000×).
- TIA sits at −0.19998 V with a 2.0 mV pulsatile ripple — exactly the 1% the
  physics predicts.

**It is the EASIEST analog capstone, not the hardest**, because op-amp
macromodels keep the signal path almost entirely linear with no forward-biased
junction in it. Its timestep is set by the *sample lattice the student asks for*
(25-250 Hz PPG → 4-40 ms), not by the physics and not by LTE.

It needs only **`R C I D` + `.subckt` + basic Newton**. It needs **no
transistors** and **no convergence hardening**. And it is a **sensor-only
region** — zero `Dac`s — so its step cap is ∞ unconditionally and it needs none
of the speculation machinery.

**One golden-design consequence that must be in the acceptance test from the
first commit.** A 1 mV, 1 Hz PPG signal moves its threshold-crossing tick by
~1.6e5 ticks for a 1 nV solution perturbation. Cross-platform the perturbation is
**exactly zero**, so the golden is reproducible everywhere — but it is fragile
against *deliberate* solver changes. **The HRM golden must be on detected beat
count and interval within a stated tick tolerance, never on an exact crossing
tick.**

### 6.2 AUDIO OUTPUT — minimum stage **S0** (audible), **S7** (drawn), **S9** (class-D)

Four rungs, three of them shippable before transistors exist:

| Rung | Stage | Nodes | Accepted timepoints per second of audio | Wall clock |
|---|---|---:|---:|---|
| Digital wavetable → host sink | **S0** | 0 (no solver) | n/a | **~209,000 samples/s = 4.7× real time** |
| Drawn PWM → ideal switched source → LC filter | **S7** | 11 | 4.34 M | **0.72-0.88 s of Java per audio second** |
| Drawn 8-bit R-2R ladder → RC | **S7** | 28 | 1.17 M | **~0.81 s per audio second** |
| Transistor-level class-D half-bridge | **S9** | 17 | 11.19 M (335,830 accepted + 118,104 **rejected** = 26.0%) | **2.8-4.9 minutes per audio second** |

**The linear fast path is what makes rungs 2 and 3 real-time**, and it is 0.5-1.0
mw. Measured Java, copy+factor+solve vs solve-only with the factorisation reused:
N=11 0.541 → 0.167 µs (3.2×), N=17 1.469 → 0.317 (4.6×), N=24 2.727 → 0.545
(5.0×), N=28 3.765 → 0.692 (5.4×). A linear region needs no Newton loop (~2×),
no step rejection (measured 5.7-12.0% on the linear paths vs 26.0% for class-D),
and one factorisation instead of one per step. **This is the highest
performance-per-week item in the programme and it belongs in S1, not the tail.**

Class-D per second of audio: 11.19 M accepted timepoints, 41.9 M Newton
iterations, 84.6 s of C; Java at 2.0-3.5× C (derived, not assumed: ngspice's
whole per-iteration cost is 2.02 µs at N=17, while measured Java dense LU at N=17
is 1.469 µs for factor+solve alone) → **169-296 s**.

> **The class-D rung renders to a WAV and the student plays the file. The
> capstone's written definition must say so.** The linear rungs are real-time, so
> "hear your circuit live" survives as the primary experience and class-D becomes
> the THD lab you render.

**Correction to the corpus:** the R-2R-vs-class-D fidelity ratio is **7.2×, not
40×**. The 40× figure came from a fixture in which seven of eight R-2R bit
sources were static DC. Regenerated with all eight bits stepping at 44.1 kHz from
a real 440 Hz sine (105,233 bit transitions per audio second): 11.7 s of C per
audio second against class-D's 84.6. **Do not quote 40× again.** The fidelity-
toggle argument survives intact and gets a much larger multiplier (200-350×) by
the better route — the linear fast path.

### 6.3 AUDIO INPUT — minimum stage **S6**

Chain: electret capsule (`I ∥ C ∥ R` into an internal JFET source follower) →
coupling cap → common-emitter BJT preamp → Sallen-Key 15 kHz → comparator.

- **26 circuit equations.** 100 ms of audio at a 22.6 µs (44.1 kHz) lattice:
  5,489 attempted / **5,065 accepted** / 4 rejected; 16,876 Newton iterations =
  2.19 per timepoint; 0.034 s of C → **0.34 s of C per audio second** →
  **~0.84 s of Java per audio second** (measured solve-only at N=26 = 0.615 µs).
  **Real-time.**
- **The minimum needs no transistors at all.** `J` and `Q` model a *real*
  electret and a *discrete* preamp; an op-amp `.subckt` preamp (DATA, D7) gets
  the same capstone. So audio input lands **7-11 maintainer-weeks before the
  transistor library**.
- **The ADC is deliberately NOT an analog model.** Comparator + R-2R DAC in
  analog, SAR register and sequencer in JLS's *digital* engine — the best
  available demonstration of why the mixed-signal boundary exists, needing zero
  new device models. Drawing the SAR costs **108×** (DAC node stepping 353,200
  times per audio second → 5.50 M accepted timepoints/s), so it is the offline
  fidelity-toggle rung: S9.

### 6.4 CAPSTONE 4 (BREADBOARD) — **does NOT wait on this. Not one week.**

Verified with arithmetic on the only two semantics that are genuinely analog:

- **RC edge rates.** A 74LS output sinking 8 mA at 0.5 V is ~60 Ω effective;
  twenty 74LS inputs at ~5 pF plus stray is of order 150 pF; **τ ≈ 9 ns** against
  a student clock period of 1 µs to 1 s — **two to eight orders of magnitude
  below**, and already below the 74LS propagation delay.
- **Transmission lines.** A breadboard jumper is at most 20 cm; propagation is
  ~5 ns/m; round-trip flight is **≤ 2.0 ns** against 74LS/74HC transition times
  of order 5-10 ns — **electrically short by a factor of 4-5**. They become real
  only for 74AC/74F (~1.5-2.5 ns), which is the family a breadboard CPU should
  not be specified in.

Everything else capstone 4 needs passes without a solver: floating-input-reads-
HIGH is an implicit constant driver at pull strength (P1 V1+V2+V3), pull-ups are
P1 V3's shipped element, contention is P1 V2's resolution fold, open-drain/
wired-AND is P1 V3's driver and net kinds, and DC fan-out is an **integer unit-
load sum over the netlist, O(pins), with no simulation at all**. Its
physical-truth content is **P1 stages 5+6+7 (24-34 mw) plus a static integer DRC
plus a canvas.**

**Stated the D10-compliant way:** capstone 4 does not depend on analog; analog,
once it exists, **improves** capstone 4 for free. The RC manual-clock debounce
network and the Schmitt-inverter/555 free-running clock are analog, are
pedagogically central, appear in no prior document, and compose at approximately
zero marginal cost once a `Dac` has `rout > 0`.

**And one correctness argument the prior work did not make:** simulating rail sag
or ground bounce would require a supply-network model (rail inductance, contact
resistance, bypass ESR) **the student never drew**, so a solver fed invented
parasitics returns a precise, reproducible, *fictional* number. A rule that
renders and checks is strictly **more honest**. That is a correctness argument,
not a cost one.

### 6.5 The one sentence that should govern all capstone documentation

> **Analog cost is set by the fastest thing that MOVES in the analog region — a
> carrier, not a signal.**

Six orders of magnitude across one programme, every figure measured, at 7-28
equations throughout:

| Circuit | Accepted timepoints per second of signal |
|---|---:|
| PPG (1.2 Hz signal, 5 Hz filter) | 12 - 1,000 |
| Audio-input front end (15 kHz filter) | 50,650 |
| R-2R at 44.1 kHz | 1.17 M |
| PWM at a 250 kHz carrier | 4.34 M |
| 8-bit SAR at 8 × 44.1 kHz | 5.50 M |
| Class-D switching MOSFETs | 11.19 M |

**Node count is nearly irrelevant across this range.** Nonlinearity is a separate
10× on top. **Any capacity planning based on node count will be wrong by orders
of magnitude** — and the students' own fidelity choices (carrier frequency,
converter architecture) are the dominant cost variable, which is a teaching
opportunity as much as an engineering one.

---

## 7. THE LICENSE FINDING

> **Every component of ngspice is absorbable into a GPL-3.0-or-later Java
> project. There is no GPL-incompatible code anywhere in the tree. The licence
> gate that has hovered over this study is fully open.**

Verified from `ngspice-cur/COPYING` (current tree) plus per-file headers:

| Component | Licence | GPL-3 compatible? |
|---|---|---|
| bulk (`Files: *`) | **Modified BSD** (3-clause) | yes |
| `src/maths/sparse` (Kundert Sparse1.3) | "unnamed MIT license"; grant verified verbatim at `spdefs.h:18-29` and `spfactor.c:31-45` — Kundert & UC Regents, attribution-only | yes, **with attribution to the authors and the University of California** |
| `src/xspice` | **PUBLIC DOMAIN**, Georgia Tech Research Corporation, PROJECT A-8503 (`evtnext_time.c:6`) | yes, no obligations at all |
| `src/xspice/icm/table` | GPLv2-or-newer | yes |
| `src/maths/KLU` (SuiteSparse) | LGPLv2 (`klu.c:36`, Tim Davis) | yes, via LGPL §3 |
| `src/osdi` | MPL-2.0 (`osdi.h:5-7`) | yes, via the MPL 2.0 secondary-license clause |
| `src/frontend/numparam` | LGPLv2-or-newer | yes |
| `src/tclspice.c` | LGPLv2 | yes |

**Three obligations that must be honoured, not just noted:**

1. **Attribution is per-file** (BSD clause 1; Sparse1.3's credit clause naming
   the authors and UC).
2. **BSD clause 3 forbids using the names "ngspice", "Regents of the University
   of California" or "Georgia Tech" to endorse the derived work.** JLS must
   describe itself as **SPICE-compatible**, never as "powered by ngspice" or
   equivalent.
3. **The combination is ONE-WAY.** Nothing can go back upstream: ngspice's own
   `COPYING` says *"GPL is not suitable for code to be directly linked into
   ngspice"*. **If the maintainer values an upstream relationship with the
   ngspice project, this should be an explicit, recorded trade rather than a
   discovered consequence.**

**One landmine to avoid:** the Debian-excluded vendor model files
(`src/spicelib/devices/adms/mextram`, `psp102/admsva`, and several TI/ADI `.LIB`
files) are third-party and are *not* covered by ngspice's own licence. Nothing in
the v1 model set needs them. The same applies to the Analog Devices macromodels
in `examples/TransImpedanceAmp/` — **cite them as evidence, never ship them.**

**Other absorbable artefacts, licences verified:**

| Artefact | Licence | Status | Verdict |
|---|---|---|---|
| **SpiceSharp** | **MIT**, © 2017 svenboulanger (LICENSE read verbatim) | active, HEAD 2026-06-16, NuGet 3.2.3 | **Highest value per week.** A full Spice3f5-derived simulator already moved out of C into a managed GC'd OO language; C#→Java is near-mechanical. `BiasingSimulation.cs` is 646 lines for the whole biasing simulation including the full escape ladder. **Do NOT port `ISolver<T>`** — Java erases generics and would box every matrix entry in an O(nnz) inner loop. **Write two concrete solvers** over primitive arrays (real on `double[]`, complex interleaved), kept in sync by a shared test template. **This is a week-1 decision and expensive to discover in week 20.** |
| **CircuitJS1** | **GPL-2.0-or-later** (verified verbatim in file headers) | active, HEAD 2026-07-19, already Java | Absorb **physics only**: `Diode.limitStep` (a Java `DEVpnjlim` **with zener/BV handling**, the fiddliest part), `TransistorElm`, `MosfetElm` as Java-shaped cross-checks. **De-Math every import.** **Do NOT absorb `OpAmpElm`/`GateElm`** (unseeded `Random` in the Newton loop) or `AudioOutputElm`/`AudioInputElm` (GWT JSNI shims into the browser Web Audio API — ~50 lines of architecture, not 640 lines of code). |
| CircuitJS1 `matrix/SparseLU.java` etc. | **Apache-2.0** (derived from EJML, header verbatim) | active | one-way GPL-3 compatible; a ready-made Java sparse LU. **Audit for pivot-tie and iteration-order determinism before use.** |
| **jspice** (knowm) | **GPL-3.0** (header verbatim) | **DORMANT since 2020-12-31** | Absorbable. **Read it, do not depend on it.** It is the proof that a usable Java SPICE reader is ~700 lines. |
| **Xyce** (Sandia) | **GPL-3.0-or-later** (verified in `N_CIR_Xyce.C`) | continuous since 1999 | Absorbable but wrong shape (MPI/Trilinos). Architecture reference for a modern MNA/DAE formulation. |
| Qucs SPICE converter | GPL-2.0-or-later, © 2004-2009 Stefan Jahn | upstream dormant; Qucs-S does not carry it | Snapshot the grammar if a `.cir` reader is ever wanted. Acceptable for absorbed code. |
| ahkab | GPL-2.0 per repo metadata — **UNVERIFIED whether -only or -or-later** | dormant, author died 2015 | **If GPL-2-ONLY it cannot be absorbed.** Reading three file headers settles it. Value is pedagogical reading only. |

---

## 8. WHAT COULD GO WRONG

Each with a mitigation and a **kill criterion** — a stated condition under which
the programme stops or narrows, decided in advance rather than argued about
later.

### 8.1 Determinism does not hold cross-platform

**Risk.** The byte-identity claim is verified **by specification plus seven
single-machine configurations**, not by a multi-architecture measurement. Nobody
in this study had an aarch64, macOS or Windows machine.

**Mitigation.** S2 is the 4-platform × 2-JDK matrix and it runs in **week 8** for
4-6 mw. The FMA and AVX rows already cover the two mechanisms by which
architectures usually differ.

**KILL:** if S2's matrix does not produce byte-identical `.tran` output on S1's
goldens across Linux/macOS/Windows × x64/aarch64 on JDK 25 and 26-ea, **stop
before S3** and rewrite the justification. The port is still worth building, but
the *decisive* argument for it is gone and the maintainer must know that in week
8, not week 40.

### 8.2 The delivery rate is uncalibrated — every number scales with it

**Risk.** The 150-400 delivered-lines-per-maintainer-week rate used across four
angles **could not be calibrated from this repository**: `git log --reverse`
starts 2026-07-16 with a squashed history, and 313 commits / 161,535 added lines
in 13 calendar days is assisted throughput, not a human-week figure. Every
downstream number is linearly proportional to it. **This is the weakest number in
the entire corpus and it is stated as such.**

**Mitigation.** S1 *is* the calibration experiment: ~800-1,200 source lines to
the full JaCoCo + PIT gate, weeks measured, everything downstream re-costed
proportionally.

**KILL:** if S1 takes **more than 8 maintainer-weeks** (2× the 3.5-5 estimate),
every downstream figure is at least 2× wrong. Stop at S3 and ship *"analog
exists, headless, for lab exercises"* as the terminal deliverable — a real,
defensible product at ~14-20 mw.

### 8.3 Convergence — the stage that kills implementations

**Risk.** S10's band (6-10 mw) is **not LOC-bounded**: the code is one week
(156 lines of BSD C for the entire limiting apparatus), the rest is a validation
loop measured in circuits-that-converge. It could be 2× wrong in either
direction. Measured spread at the *same circuit size*: 2.00 Newton
iterations/timepoint (HRM) vs **20.4 with 15.1% rejection** (diode bridge +
astable).

**Mitigations.** Absorb `devsup.c`'s four limiters **verbatim** — and preserve
the `icheck` protocol, because *a limited step must force another Newton
iteration regardless of residual*; getting that wrong yields plausible, wrong,
**reproducible** answers, the worst failure mode for a teaching tool. Build
**one** gmin ramp plus source stepping plus pseudo-transient continuation, not
ngspice's three gmin variants. Ship the discontinuity diagnostic that **names the
offending `B` source** when the timestep collapses (measured: a hard ternary
comparator rejected 11,748 of 46,395 timepoints — 25.3% — versus 0.02% for the
HRM). And surface the **rejected-timestep rate** as first-class output for ~0.5-1
mw: *"11,748 of 46,395 timesteps were rejected (25%); the largest contributor was
B1."* It converts the most opaque part of analog simulation into something a
student can act on.

**KILL:** if, after 6 maintainer-weeks inside S10, the 200-circuit hard corpus
non-convergence rate is **above 5%**, the tool is not homework-grade. Restrict
the shipped palette to the linear + diode set, document the refusal with the
corpus results attached, and stop. Do not spend 10 more weeks hoping.

### 8.4 CI time and gate stability

**Risk.** The required gate is 141 s. An unbounded analog corpus, or a flaky
cross-platform lane, breaks it.

**Mitigation.** The bounded corpus (n ≤ 20, every fixture under 20,000 steps)
budgets to ~4 s = +3%, and is **self-enforcing** because R1 pins step counts.
Audio-length transients, the ngspice oracle and PIT all live outside the required
gate.

**KILL:** if the bounded analog corpus exceeds **15 s** on the required gate
(>10% of 141 s), or the required cross-platform analog job is red more than once
per 20 runs for reasons other than a real defect, **move the analog lane out of
the required gate** and demote it to nightly. That is a kill for *"analog goldens
are a required gate"*, not for analog — but it costs the autograding capstone its
strongest claim, so it must be a decision, not a drift.

### 8.5 Model curation becomes a support burden

**Risk.** PSpice, HSPICE and LTspice each add `.func`, `TABLE`, `LAPLACE`,
`VALUE=`, `PARAMS:` and model-card extensions; ngspice spends 10,221 lines in
`inpcom.c` chasing them. *"My downloaded model doesn't load"* is the predictable
failure mode, and at bus factor 1 it is unbounded.

**Mitigation.** Target Spice3f5 + ngspice spellings, **document the target**,
parse-and-warn on unknown Spice3f5-era parameters (the OP177A card carries
`KF=2E-17, AF=1`), emit *named* diagnostics (`"unsupported construct LAPLACE from
PSpice dialect"`) rather than syntax errors, and ship the model-card inspector so
the failure is legible **before** simulation starts.

**KILL:** if the model-card inspector shows that **fewer than half** of a
representative sample of real downloaded vendor `.lib` files load with a named
diagnostic rather than a parse error, the "libraries are DATA" story has failed.
Drop the "download the model for the part you will solder" promise from the
capstone text, ship a small curated set of op-amp macromodels JLS is licensed to
redistribute, and record the reversal. **Measure this at S3, not at S9.**

### 8.6 THE HALF-FINISHED ANALOG ENGINE — the biggest risk

**Risk, stated concretely rather than as a mood.** The failure mode is not "the
solver is wrong". It is: **S5 lands, twenty-two element types and a palette and a
save format are committed, and the programme then stalls before S10** — leaving a
tool that runs the four circuits in this document, fails on the fifth circuit a
student draws, and cannot be removed because the format and the palette are
public. At bus factor 1, with S5 an **indivisible 11-16 mw lump** that touches
the sealed `Element` hierarchy, `Put`, four `SimpleEditor` sites, the palette
contract test and D4's toggle — none of which can be half-landed — and with the
delivery rate uncalibrated (§8.2), this is the most likely bad outcome in the
whole programme.

**Four mitigations, all structural:**

1. **The stage order is the mitigation.** S0-S4 (15.5-22.5 mw) are each
   independently valuable and none of them commits a public element type or a
   format bump that analog owns. The programme can stop at S4 with a real
   deliverable and zero permanent surface area.
2. **D-A11's generic dialog** keeps the coverage draw at ~11.7% of the commons
   rather than 71%, so a stalled programme does not also bankrupt every other
   capstone's coverage budget.
3. **Ship S5 with only 8 drawn devices, not 22.** The remaining 14 are S11 and
   are explicitly low demo-value. A stall after S5 leaves an 8-device palette
   that is coherent, not a 22-device palette that is half-supported.
4. **S10 is scheduled before S11**, deliberately against demo-value ranking, so
   that "make it converge" outranks "add more devices".

**KILL:** if S5 has not produced a **drawn, running HRM at 24 maintainer-weeks
cumulative**, stop before S9. The terminal deliverable is then S0-S4: a headless
solver, host audio, and op-amp filters from vendor `.subckt` files — documented
as a lab tool, with **no drawn analog palette, therefore no K9 debt, no 22 element
types to maintain, and no format surface analog owns.**

### 8.7 Other risks, with kill criteria

| Risk | Mitigation | KILL |
|---|---|---|
| **PIT unreachable on real device code.** The 86%/88% measurement is on a 229-line kernel; larger kernels accumulate more genuinely equivalent mutants | Stamp goldens in three topologies per device; re-measure and ratchet at each family; plan against a band of **80-86**, not 86 | Two consecutive device families below 80 **with stamp goldens written** ⇒ the device library stops at the diode and the shipped scope becomes linear + diode + `.subckt` |
| **`StrictMath` in the device `load()` loop.** Measured: device evaluation is **51.7-59.6% of transient runtime**, and HotSpot intrinsifies `Math.exp` but not `StrictMath.exp`. The determinism cost is concentrated in the hottest code, and **the factor inside the loop is the most consequential unmeasured number in the corpus** | Measure it in S1, not S6. If large, the mitigation is a **deterministic software exp/log** (correctly rounded, table-driven, 1-3 mw), **not** abandoning determinism | If a correctly-rounded software exp cannot get within 2× of `Math.exp`, and the loop is >60% of runtime, accept the slowdown and document it — do not weaken D-1 |
| **Sparse LU pivot-tie determinism.** Markowitz has far more tie-breaking than the dense partial pivoting that was measured; ties are common in `(rowcount−1)×(colcount−1)` products and threshold pivoting admits many acceptable pivots. **The largest un-derisked determinism item** | A dedicated 2-3 day experiment with deliberately tied matrices, asserting permutation vectors are a deterministic function of the matrix pattern — **before** S12, and ideally as a throwaway at S2 | If a totally ordered tie-break cannot be specified without measurable cost, **keep dense LU** and set the sparse trigger at a circuit size no capstone reaches (measured: every capstone circuit is 7-28 equations) |
| **The host audio door is refused at review.** `docs/vcd-interop.md:19-24` rejects live co-simulation under #63, and #38's hardening assumes a `.jls` file is DATA that cannot touch your machine | S0 takes it to review in **week 4** and reconciles #63 **explicitly**, not by implication | If refused: the audio capstones become file-in/file-out only, no live playback. The HRM is unaffected (it reads a recorded waveform). Cost of finding out: **3-4.5 mw instead of 30** |
| **Coverage commons exhaustion.** The programme draws ~685 uncovered LINE (JLS side) + ~223 (engine) ≈ **31% of the 2,897 commons** | D-A11's generic dialog; per-package floors in the same PR that creates the package (`jls.analog` ships with its floor) | If the draw exceeds **900 LINE at the end of S5**, the generic-dialog design has failed and **S11's remaining 14 element types must not be built** |
| **Golden brittleness at bus factor 1.** One change to a limiting function, a convergence constant or the LTE formula invalidates every analog golden | Digest goldens (one line per test) for most cases + a small pinned full-waveform set + a reviewed regenerate script + R4's measured-error header, which makes a bad regeneration visible in the diff | If regeneration churn exceeds one full-corpus regeneration per month, demote T1 full-waveform to nightly and keep only the digest tier in the required gate |
| **Trapezoidal ringing on the audio LC tank.** Both audio-output paths contain an LC — exactly where trapezoidal ringing appears. Measured: at `h/τ = 1000` trapezoidal's amplification factor is **−0.996** (it alternates sign and decays 0.4% per step — that IS the ringing, and it is the exact consequence of trapezoidal being A-stable but not L-stable), while Gear-2 gives 0.0223. But at `h/τ = 0.1` Gear-2 is **4.3× less accurate** and damps an LC tank that physically should not damp | One day at S7 with a step discontinuity on the filter node settles it | If ringing appears, Gear-2 (3-4 mw) moves from S12 onto the critical path. Build both, default trapezoidal, document `method=gear` — and ship the measured stability table as a lab exercise, since it falls out of the implementation for free |
| **`docs/vcd-interop.md:19-24` vs `docs/grand-architecture.md`** — a recorded in-repo contradiction, and `grand-architecture.md` §6 says the simulation inner loop lives entirely inside core with zero plugin indirection, while this design adds a **second hot loop** | S12's normative docs; an `ARCHITECTURE.md` decision with a revisit trigger; a `docs/extension-points.md` row filed **BEFORE** the seam exists, as the catalogue's own rules require. Note the analog stepper is core-internal and sealed, the same posture the fidelity toggle takes — this is a sequencing tax, not a veto | If the maintainer will not reopen #63, S0's host doors are refused and the audio capstones are file-only. Settle at S0, in week 4 |

---

## 9. THE HONEST TOTAL, AND WHAT IT DISPLACES

### 9.1 The number

> **69-105 maintainer-weeks for the full programme, central ~86.**
> **31-45 to all three analog capstones.**
> **27-39 to the first analog capstone drawn and running.**
> **3-4.5 to the first audible demo, which needs no solver.**

Method: bottom-up per stage, from the seven angles' independently derived
figures, reconciled and de-duplicated. Three anchors calibrate the line counts:
(1) the port target measured in C at **26,456 SLOC** — 4.6% of ngspice's 572,822;
(2) the same scope measured in a managed language from SpiceSharp's matching
subset at ~31,055 raw C# lines ≈ 19-21k SLOC ≈ 18-22k SLOC of Java; (3) test mass
from this repo — 47,097 test lines to 82,137 src lines = **0.573**, uplifted to
0.8-1.2× for branch-dense numeric code under the 93.0/92.0/84.5 + 80/82 gates,
and cross-checked against the experiment's own 229 kernel lines : 492 test lines
= 2.15× at 99% line coverage and 86/88 PIT. Element registration is anchored on
the measured ~82 lines/element across 12 files, zero format version.

**Convergence across the angles, which is the reason to believe the band:**
Angle 1 derived **69-117** from C SLOC; Angle 4 derived **65-108** from the
boundary mechanism outward; Angle 7 plus Angle 6's additive test spine gives
**70-110**; this determination's stage sum gives **68.5-104.5**. Four
independent bottom-ups inside a 10% envelope.

**The weakest number, stated plainly:** the delivered-lines-per-maintainer-week
rate could not be calibrated from this repository (§8.2). Everything is
proportional to it. **S1 settles it and the whole plan is re-cost from that
measurement.** This is the single highest-value experiment available and it
doubles as Stage 1's deliverable.

**What this band already assumes:** absorption of BSD / public-domain / MIT /
GPL-compatible source under D8, not clean-room work. A from-textbook plan roughly
**doubles** S10 (convergence) and S9 (devices).

### 9.2 What it displaces

Against the committed **281-410 maintainer-weeks**:

| Cut | mw | % of roadmap | Calendar at bus factor 1 |
|---|---:|---:|---|
| First audible demo (S0) | 3-4.5 | ~1% | one month |
| Headless lab tool (S0-S4) | 15.5-22.5 | +4% to +8% | 4-6 months |
| **All three analog capstones (S0-S7)** | **30.5-44.5** | **+8% to +16%** | **8-11 months** |
| Homework-grade (S0-S10) | 50.5-76.5 | +12% to +27% | 13-19 months |
| **Full programme (S0-S12)** | **68.5-104.5** | **+17% to +37%** | **17-26 months** |

**Stated the way it will be felt:** the full analog programme and the live
console do not both happen in the same eighteen months.

**Concrete displacement candidates at comparable scale in the recorded corpus:**
P8 compiled backends / Mode T (~30-45 mw — which BRIEF §13 says is *"a DECISION
plus ~30-45 maintainer-weeks, not a physical limit"*), P11+P12 diff/API (18-27 mw
— `jls.api`, which D7 names as **the** extensibility story), P10 (12-18), and
#91/#84 editor testability (8-14).

**The full programme displaces roughly P8 + P11. The three-capstone cut displaces
roughly P11 alone.**

**Three credits that are real and should not be double-charged:**

1. **P4 physical time units (2-3 mw)** is charged here but **owed anyway** by SDF
   (#89), Liberty (#87), SDC (#93) and BRIEF §3's "declare a slow clock" lever
   for the Linux boot.
2. **The `PortAlphabet` mechanism (2.45 mw)** is shared with the MVL work and
   with P1's already-committed X/Z/U + strengths. Whichever ships first pays it;
   the others pay ~0.4.
3. **D4's fidelity toggle** — riding it as a third `SubCircuitImpl` permit rather
   than building a parallel binding saves **~4-9 mw** (6.5-10.5 standalone vs
   ~0.8 reused). Real, but not decisive; the decisive argument is that JLS must
   not acquire two boundary mechanisms.

**One displacement runs backwards, and it is worth naming:** S0's host-I/O door
is on capstone 2's Linux-console critical path too, so paying it here **retires
that governance risk for the whole capstone set**.

**And one displaced item pays back into this programme:** #91/#84 editor
testability (8-14 mw). Stages S5 and S11 carry ~340 of the ~685 uncovered LINE
this programme draws — **half of it** — precisely because `jls.edit` is
untestable at 24.64% LINE coverage with `SimpleEditor.EditWindow` the single
largest class in the whole JaCoCo report at 1,962 LINE units. Doing #84 first
would take those two stages from *"must be minimised"* to *"ordinary work"*, and
it relieves the commons for **every** future view rather than one. **If two or
three canvases are ever wanted (breadboard is the named one), #84 first is
cheaper than paying the per-canvas test tax repeatedly.** That choice belongs in
front of the maintainer explicitly rather than being assumed away.

### 9.3 What is bought that could not be bought any other way

Not cost savings — capabilities that the two cheap surfaces cannot deliver at any
price:

- The **single offline jar** is preserved rather than broken.
- **Byte-identical analog goldens across platforms and JDKs** — which no SPICE
  offers, and which is exactly what the autograding and verification capstones
  need to compare a student's transient waveform *exactly* rather than within a
  tolerance.
- **Zero `ProcessBuilder` in `src/`** preserved; the `.jls`-file-is-DATA premise
  intact.
- A **required** CI lane instead of an `assumeTrue` lane with engine-version rot.
- **Zero analog/digital timing error** instead of a declared horizon quantum.
- **Threshold, comparator-trip-point and metastability labs return to scope** —
  the guard-band hard-fail existed only because an external solver's
  near-threshold answer is not reproducible. A bit-exact in-process solver makes
  it exactly reproducible on every platform forever, so the guard band becomes a
  *warning*. **The prior contract refused precisely the capstone the maintainer
  just added: the HRM is entirely a near-threshold comparator problem.**
- `.op`/`.dc`/`.tran`/`.ac` as **first-class JLS analyses**, not an external
  tool's.
- A **machine-checked abstraction ladder** — behavioural element ↔ drawn digital
  ↔ drawn analog, compared by `BoundaryEquivalence` — for roughly one fixture and
  two tests. No educational tool does this, and it is D9's CS→ECE→EE trajectory
  made testable.

---

## 10. THE DECISION LIST

| # | Decision |
|---|---|
| **D-A1** | Port the numerics to Java as `jls.analog`. ngspice is a maintainer-side CI oracle only. No `libngspice`, no `ProcessBuilder` in `src/`. |
| **D-A2** | Analog devices are drawn elements on the existing canvas, contained in a `SubCircuit` with `impl "analog"`, fed by `.subckt` leaves. No new canvas. |
| **D-A3** | `AnalogElement` is the 4th `Element` permit. `Put.element` widens to `Element`. `WireNet.propagate`'s cast becomes an `instanceof Reacts` guard. |
| **D-A4** | An analog net is the same `WireNet` carrying no value — an MNA node index only. Domain lives on the PORT and NET. The domain check sits **above** the width check, **unconditionally**, at all four `SimpleEditor` sites. |
| **D-A5** | Build `Put` alphabet validation ONCE as `PortAlphabet {domain, radix, encoding, strength}`, shared with MVL and P1. |
| **D-A6** | Node index assignment is a pure function of circuit content in **stable-id order**. Written into `simulation-semantics.md`. |
| **D-A7** | `Adc`/`Dac` are one-bit level bridges. Sample rate is not a parameter. Crossing = earliest **tick**, by bisection (policy P-b). `Dac` **ramps**, never steps. `tdelay ≥ 1` tick. |
| **D-A8** | Invert XSPICE's ownership: the digital loop owns `now`; the analog region self-schedules in `Clock`'s idiom. **No `EVTbackup`.** Sensor-only cap ∞; synchronous-`Dac` guard via `nextEventTime()`; asynchronous speculation deferred. |
| **D-A9** | `timebase` on `CIRCUIT`, VCD `$timescale` grammar, default 1 ps, FORMAT 3 iff present. `t = now × 10^exp`, **recomputed, never accumulated**. |
| **D-A10** | `PaletteEntry` and `PaletteContractTest` gain a view dimension. Analog palette visibility derived from the model. K9 ratchet ships **first**. |
| **D-A11** | One generic dialog, one generic renderer, exactly three bespoke dialogs. |
| **D-A12** | Analog parameters save as SPICE-spelled `String`s. Zero format version from analog. |
| **D-A13** | MOSFET level 3 is **deferred** (contradicts the brief; needs a maintainer call). JFET is **added**. Gummel-Poon is the only BJT. `POLY(n)` is mandatory. |
| **D-A14** | JLS reads vendor `.model`/`.subckt`/`.lib` directly as DATA and curates no library. Parse-and-warn on unknown Spice3f5-era parameters. Ship the model-card inspector. |
| **D-A15** | Byte-identical goldens as the required gate, **plus** tolerance validation. Four tiers. **No PIT exemption.** `jls.analog` gets its own JaCoCo/PIT floors set from first measurement and ratcheted. |

---

## 11. WHAT WOULD CHANGE THIS DETERMINATION

Six experiments, in order of value. The first two are mandatory and are stages.

1. **S1's calibration** (mandatory, in the plan). Settles the rate that every
   number scales with.
2. **S2's 4-platform × 2-JDK matrix** (mandatory, in the plan, week 8). Settles
   whether the decisive argument for the port survives.
3. **The `StrictMath` cost inside the device `load()` loop** (~1 day, at S1). The
   most consequential unmeasured number in the corpus, because that loop is
   51.7-59.6% of transient runtime.
4. **Sparse-LU pivot-tie determinism** (2-3 days, ideally at S2). The largest
   un-derisked determinism item.
5. **The factorisation-cache hit rate on a PWM carrier** (~2 days, at S1). The
   difference between 0.72-0.88 s and 2.35 s of Java per audio second — i.e.
   between real-time drawn audio and a render.
6. **`git bisect` ngspice 42→46 on the linear-RC netlist** (~1 hour). Would make
   the version-instability finding airtight rather than merely overwhelming. Not
   required — the finding is already measured four independent ways.

Two cheap factual gaps worth closing: whether ahkab is GPL-2-**only** (three file
headers), and whether the ngspice manual contains a normative rawfile appendix
(one PDF). Neither changes a decision.

---

## 12. VERIFICATION TRANSCRIPT (this session, HEAD `b54e6ee`)

| Claim | Command / anchor | Result |
|---|---|---|
| No host execution surface in `src/` | `grep -rn "ProcessBuilder\|Runtime.getRuntime().exec\|System.loadLibrary\|java.lang.foreign" src/` | **0** |
| `StrictMath` unused today (the lint starts clean) | `grep -rn StrictMath src/` | **0** |
| `Element` permits 3 | `Element.java:17-18` | `permits DisplayElement, LogicElement, Wire` |
| `Put` is sealed on `LogicElement` | `Put.java:17-18,29` | `permits Input, Output`; `protected @Nullable LogicElement element` |
| `bits` = "0 implies arbitrary" | `Put.java:34` | confirmed verbatim |
| Element registry size | `grep -c "new ElementType(" ElementRegistry.java` | **35** |
| Palette size | `grep -c "entry(Group\." Palette.java` | **32** |
| `instanceof LogicElement` blast radius | `grep -rn "instanceof LogicElement" src/` | **8** |
| Width check sites | `grep -n "Bits don't match" SimpleEditor.java` | `:4015, :4142, :4247, :4358` |
| `recheck` widens by `Math.max` | `WireNet.java:280` | `bits = Math.max(p.getBits(),bits);` |
| `hasinput` set only from an `Output` | `WireNet.java:283` | confirmed |
| Format version | `Circuit.java:102` | `FORMAT_VERSION = 2` |
| Dimensionless integer time | `Simulator.java:36` | `protected long now = 0;` |
| VCD timescale is hardcoded | `BatchSimulator.java:423` | `$timescale 1 ns $end` |
| Time model is normative and unitless | `docs/simulation-semantics.md` §1 | *"Time units are abstract; nothing binds them to seconds"* |
| Palette totality is enforced by a green test | `PaletteContractTest.java:44-66` | `NON_PALETTE_TAGS = Set.of("SubCircuit","WireEnd","TestGen")` |
| `jls.sim` JaCoCo floors | `pom.xml:449-471` | INSTRUCTION 0.930 / LINE 0.920 / BRANCH 0.845 |
| PIT scope and thresholds | `pom.xml:778-813` | `jls.sim.*`, `jls.BitSetUtils`, `jls.Util`, `jls.SpatialIndex`, `jls.collab.op.*`; `mutationThreshold` 80, `testStrengthThreshold` 82; floors "only ever move UP" |
| Compiler release (JEP 306 in force) | `pom.xml:43` | `<maven.compiler.release>25</maven.compiler.release>` |

Everything else cited above is inherited from the seven angle analyses with its
own provenance recorded there; measurements are attributed to the angle that made
them, and where an angle corrected another (the 40× → 7.2× fidelity ratio, the
1.47× → 1.66-1.73× `StrictMath.exp` penalty, Angle 2's M5 6-9 mw → Angle 5's
19-28.5 mw) the later, better-instrumented figure is the one used.
