# 12 — HIGH-FREQUENCY DETERMINATION: JLS at the ~20 GHz boundary

**Status:** determination. The maintainer asked to *"start on research of high
frequency systems reaching on the order of magnitude of 20 GHz"* and framed it:
*"I suspect new design factors will come into play and require making the
underlying design more robust."* This document answers the second sentence
first, because it is the load-bearing one.

**Inputs:** seven angle analyses, all read in full —
`hf-physics-regime.md` (1), `hf-industry-methods.md` (2),
`hf-distributed-elements.md` (3), `hf-core-robustness.md` (4),
`hf-feasibility-arithmetic.md` (5), `hf-target-and-capstone.md` (6),
`hf-realist.md` (7). Plus `BRIEF.md` §§0-13 (D1-D10 binding),
`11-analog-determination.md` in full, `09-format-adoption-plan.md`,
`10-capstone-plan.md`, `docs/plan/**` (18 capstones, 57 features, 112 tasks),
`docs/simulation-semantics.md` §§1,2,7,9, `docs/capability-roadmap/`,
`docs/standards-adoption/`, `docs/standards-landscape.md`, `ARCHITECTURE.md`.

**Repo state:** HEAD = `529e5be`. Every HEAD claim below was re-verified in this
session; the transcript is §11. **Correction to the brief:** the planning corpus
is **18 capstones, 57 features, 112 tasks** (`docs/plan/REGISTRY.md` COUNTS
section, and `ls docs/plan/{capstones,features,tasks} | wc -l`). The brief's
"17 capstones, 53 features" is one revision stale and every id cited below uses
the current space.

**Governing decisions:** D1-D10. **D10 is binding throughout.** Nothing below is
refused by citing its absence. Two refusals appear that D10 explicitly permits —
a physical/mathematical limit shown by arithmetic, and a specific approach shown
to fail with the working approach named and priced in the same paragraph — and
they are labelled as such. Two prior in-tree positions are **reopened and
superseded** under D10 rule 4, with the path and the cost supplied.

---

## 0. THE DETERMINATION IN ONE PAGE

1. **The maintainer's suspicion is correct, and exactly ONE foundational
   assumption dies.** `WireNet`'s equipotentiality. It dies by **cardinality,
   not by degree**: a 50 mm net at 20 Gb/s carries **6.9 simultaneous distinct
   values** while `WireNet` holds one `@Nullable BitSet`. That is not an accuracy
   problem a tolerance fixes. Everything else — the value domain, the event
   queue, the 64-bit tick, multi-driver resolution — either survives untouched or
   needs a change already owed by four other capstones.

2. **THE REFRAME, and it is the single most valuable output of this study: the
   regime is entered by EDGE RATE, not by clock rate.** A 100 MHz circuit with a
   20 ps driver has a 25 GHz knee and a 0.48 mm critical length. A 150 mm
   breadboard jumper is **2.1× critical length for a 74AC part** and 0.24× for a
   74LS part — which is the mechanical answer to *"it works in the simulator and
   fails on the breadboard"*, and CAP-04 already exists. **JLS crosses this
   threshold silently today and cannot even ask the question, because the model
   contains neither a physical length nor a transition time.**

3. **ONE LAW closes over all five regimes, and it is the robustness answer.**
   *Anything that changes what a value MEANS goes on the PORT. Anything that
   changes whether a net HAS a value goes INSIDE an ELEMENT.* The MVL and analog
   determinations converged independently on the first half; high frequency is
   the case that establishes the second. Both mechanisms are already funded.
   **High frequency is not a fifth extension mechanism — it is a consumer of the
   fourth alphabet field plus one ordinary element.**

4. **Transient simulation is REFUSED as the method for a bit-error rate, by
   arithmetic, with the method that works named.** BER 1e-12 at 95% confidence is
   3e12 bits = 150 s of simulated time = **5.7-9.0 years** single-threaded at
   JLS's own measured solver rates, and the bathtub cannot extend below
   `1/N_bits` at any rate. The statistical / peak-distortion method computes the
   same contour in **0.02-0.24 s** and its cost is **flat in the BER target** —
   ratio ~1e9 at BER 1e-12, ~1e15 at BER 1e-18. **This refuses a METHOD, not a
   capability.** And the honest counterpart must be said too: the brief's own
   1e12-femtosecond-step case is **27.6-55 days**, which is useless for a lab but
   is *not* impossible, and claiming otherwise would be false.

5. **The prerequisite chain is far shorter than the brief assumed, and that is
   worth 24-45 maintainer-weeks.** The brief posits `analog S1 → S4 → S12 .ac →
   S9`. That chain is real for exactly one of four possible HF products. The
   other three need **FEAT-047 and nothing else from `jls.analog`**, because a
   channel is DATA, a statistical eye is array DSP, and a lossless line has a
   CLOSED FORM. **HF must not be used to justify building the analog solver, and
   the analog solver must not be treated as a gate on HF.**

6. **The honest target is DIAGNOSE → DEMONSTRATE → DELIVER, adopted as CAP-18 at
   11-19 mw marginal / 19-34 standalone**, cumulative **14.5-25 mw** including
   every prerequisite. The **first deliverable** — the electrical-length lint —
   is **5.5-10 mw cumulative** and is the highest teaching-value-per-week item
   anywhere in this space. The eye/BER capability is a **separate** capstone at
   22-38 mw cumulative and shares only FEAT-047.

7. **Sequence by PERMANENCE, not by cost.** The cheapest deliverable (a drawn
   transmission line, 2-3.5 mw) commits the most permanent public surface (a
   frozen tag, a palette entry, a K9 obligation), while the expensive statistical
   work commits none. The analog determination's stop-clean rule does not
   transfer and must be re-derived: **lint → constraint export → element → eye.**

8. **What it displaces: CAP-17 (38-62 mw, priority 18)** is the only large
   committed item whose deferral strands nothing — FEAT-054..057 are each
   consumed by CAP-17 alone (verified). CAP-18 at 11-19 mw is roughly CAP-13
   (6-12) plus change, or half of CAP-06. Everything queues behind CAP-00.

---

## 1. THE REGIME, STATED ONCE AND CORRECTLY

### 1.1 What 20 GHz means physically

| quantity | FR-4 stripline (ε_r 4.3) | FR-4 microstrip (ε_eff 3.0) | air |
|---|---:|---:|---:|
| velocity `v = c/√ε` | 1.4457e8 m/s | 1.7309e8 m/s | 2.998e8 m/s |
| λ at 20 GHz | **7.229 mm** | 8.654 mm | 14.99 mm |
| λ/10 (go/no-go lumped bound) | **0.723 mm** | 0.865 mm | 1.499 mm |
| λ/20 (use when it feeds a margin) | 0.361 mm | 0.433 mm | 0.750 mm |
| propagation delay | 6.917 ps/mm (175.7 ps/in) | 5.777 ps/mm (146.7 ps/in) | 3.336 ps/mm |

The brief's figure — ε_r ~4.3, v ~1.5e8 m/s, λ ~7.5 mm — is **confirmed**;
1.5e8 m/s is exactly `c/√4.0` and gives λ = 7.5 mm, λ/10 = 0.75 mm. Either basis
lands the same conclusion: **at 20 GHz a conductor stops being an equipotential
node below one millimetre. A 0402 chip resistor is 1.0 mm long.** A 20 GHz signal
cannot cross one passive component without the lumped assumption failing.

The λ/10 vs λ/20 choice is not folklore and should be exposed rather than
hard-coded: phase shift `θ = 2πl/λ`, so `l = λ/10` is 36° and `1 − cos 36° = 19%`
worst-case amplitude error, while `l = λ/20` is 18° and 4.9%.

**Loss, which is why the regime exists at all.** Over 10 inches at 20 GHz:
FR-4 **31.4 dB** (2.71% of drive amplitude survives), Megtron 6 14.6 dB (18.7%),
Rogers RO4350B 16.1 dB (15.8%), Tachyon 100G 14.5 dB (18.7%). Dielectric loss
scales as `f`, conductor loss as `√f`; skin depth in copper at 20 GHz is
**0.461 µm** against 35 µm of 1 oz copper, so the current occupies 1.3% of the
cross-section and a DC resistance calculation is wrong by **~150×**. The 2.71%
figure is the entire commercial argument for exotic laminates and the entire
reason equalisation exists: **the eye at a receiver input on FR-4 is closed
before any equaliser sees it.**

### 1.2 The workload, settled — and a correction to the brief

**No mainstream digital clock runs at 20 GHz and none is coming.** Top shipping
parts are 6.0-6.2 GHz (Intel i9-14900KS boost, Core Ultra 9 285K 5.7, Ryzen 7
9850X3D 5.6); the 9.12 GHz record is an LN2 overclock, not a product.

**20 GHz is a SerDes CHANNEL frequency:**

| link | symbol rate | Nyquist |
|---|---:|---:|
| 40 Gb/s NRZ | 40 GBd | **20.0 GHz exactly** |
| PCIe 5.0 (32 GT/s NRZ) | 32 GBd | 16 GHz |
| PCIe 6.0 (64 GT/s PAM4) | 32 GBd | 16 GHz |
| 112G PAM4 | 56 GBd | 28 GHz |
| 224G PAM4 | 112 GBd | 56 GHz (channel spec'd to ~84 GHz) |
| UCIe 1.0/1.1 | 32 GT/s | 16 GHz |

> **Correction to the brief.** It states that SerDes at 25-224 Gb/s have
> fundamental content in "12.5-112 GHz". 112 GHz is the **baud** rate of 224G
> PAM4, not its Nyquist — PAM4 carries 2 bits/symbol, so 224 Gb/s = 112 GBd and
> Nyquist = 56 GHz. **The correct band is 12.5-56 GHz fundamental.**

> **D-H1. The workload is A HIGH-SPEED SERIAL LINK AND ITS CHANNEL**: a
> transmitter with a known output impedance and edge rate, a passive interconnect
> described by S-parameters or frequency-dependent RLGC, a receiver with
> equalisation, and exactly two questions — *does the eye open*, and *at what
> BER*. **Explicitly out: mm-wave RF circuit design** (5G FR2 24-40 GHz, 60 GHz
> WiGig, 77 GHz radar). Different curriculum, different artifact, different tool
> class. D9's CS→ECE→EE arc ends at the digital/signal-integrity boundary, not at
> microwave engineering, and that boundary is named once in `ARCHITECTURE.md`
> with a revisit trigger rather than re-argued per feature.

**Scoping an HF programme as "make the digital engine run at 20 GHz" targets a
workload with no instances** — and is trivially satisfied anyway. Measured: the
event engine runs at 3.14 M events/s warm with ~386 reacted events per clock
cycle on the shipped RV32I, so 1 µs of a 20 GHz machine is 2.0e4 cycles =
7.72e6 events = **2.46 s of wall clock — identical at 1 Hz and at 20 GHz.**
Frequency never enters the engine.

### 1.3 THE REFRAME: edge rate, not clock rate

This is the finding that puts far more ordinary designs in the regime than
"20 GHz" suggests, and it is the reason this study is worth more to JLS's actual
users than its title implies.

Johnson & Graham's knee frequency (*High-Speed Digital Design: A Handbook of
Black Magic*, Prentice Hall 1993) is `f_knee = 0.5/t_r`. Evaluating λ/10 at the
knee gives a purely time-domain criterion in which **clock frequency does not
appear at all**:

| edge rate `t_r` | `f_knee` | `l_crit = v·t_r/6` FR-4 | on a breadboard (0.7 c) |
|---:|---:|---:|---:|
| 5 ps | 100 GHz | 0.12 mm | 0.18 mm |
| 20 ps | 25 GHz | **0.48 mm** | 0.70 mm |
| 50 ps | 10 GHz | 1.21 mm | 1.75 mm |
| 200 ps | 2.5 GHz | 4.82 mm | 7.00 mm |
| 1 ns | 500 MHz | 24.1 mm | 35.0 mm |
| **2 ns (74AC)** | 250 MHz | **48.2 mm** | **70.0 mm** |
| **18 ns (74LS)** | 27.8 MHz | **434 mm** | **630 mm** |

**Every critical-length rule in circulation is one rule at a different
strictness constant.** Let `D = v·t_r` be the distance an edge travels during its
own rise time. Then `λ_knee = 2D`, so `λ_knee/10 = D/5` (i.e. `t_pd = t_r/5`),
`λ_knee/12 = D/6` (Johnson's rule), and `λ_knee/4 = D/2` (the round-trip rule).
Verified numerically. The λ/10, λ/20, `t_r/2`, `t_r/3` and `t_r/6` rules that
practitioners complain contradict each other are one statement at different
tolerances.

> **D-H2. JLS adopts `l_crit = v·t_r/6` as the default (the conservative
> practitioner form), EXPOSES the constant, and documents that it is a tolerance
> choice rather than a law.** That is better pedagogy than any of the rules
> taught alone, and it costs nothing.

**The consequence for JLS.** A 150 mm breadboard jumper is 2.1× critical length
for 74AC and 0.24× for 74LS. **CAP-04 (breadboard CPU, priority 8) is already in
this regime and JLS cannot say so**, because `WireNet` has no length and the
delay model has no transition time. `docs/capability-roadmap/README.md` P4's own
verdict on zero-delay wires — *"That is not a simplification, it is a
falsehood"* — is exactly right and currently has no remedy. **This is the honest
form of "more robust": the tool states its own domain of validity instead of
silently exceeding it.**

---

## 2. WHAT BREAKS IN THE FOUNDATIONS

The maintainer's own question, answered assumption by assumption. Verdicts:
**ABSORB INTO CORE** / **ISOLATE BEHIND A BOUNDARY** / **REFUSE**.

### 2.1 `WireNet` as a lumped equipotential node — **ISOLATE**, as an ELEMENT

**It breaks, and it is the only foundational break in the list.** But it breaks
by cardinality, not by tolerance.

Verified at HEAD `529e5be`, the complete field list of `src/jls/elem/WireNet.java`:
`ends` (:22), `wires` (:24), `bits` (:26), `hasinput` (:28), `triState` (:30),
`value` (:405, one `@Nullable BitSet`), `conflictReported` (:407). **No length,
no delay, no impedance, no velocity.** `WireNet.propagate` posts every consumer
event at `now` (`:505-507`) — not a small delay, exactly zero — and
`docs/simulation-semantics.md:288` makes that normative.

Computed at v = 1.446e8 m/s: a 50 mm net at 20 GBd has `t_pd` = 345.6 ps against
a 50 ps UI = **6.9 simultaneous distinct values**; 100 mm = 13.8; 254 mm (10 in)
= 35. By contrast a 100 mm net at 100 MBd holds 0.07 values — i.e. one, which is
why `WireNet` is exactly right today and will remain right for almost every JLS
circuit forever.

**Equipotentiality is `WireNet`'s definition, not a parameter of it.** A net you
can relax it on is two nets and a relation.

> **D-H3. A transmission line is an ELEMENT BETWEEN TWO NETS, never a new net
> kind.** Measured cost of the element route: **~66 lines of registration tax
> across 12 files** (`git show --stat 38a0544`, which added `FieldExtend` and
> `RegisterFile` in one commit: 1,188 insertions, 1,055 in element bodies, 133
> across 12 other files for 2 types), **zero format version**, zero change to
> `WireNet.propagate`, `recheck`, multi-driver resolution, probes or VCD. The net-
> kind route lands on the 531-line class that carries #98's insertion-order
> determinism, touches four `SimpleEditor` connect sites plus
> `recheck`/`setBits`/`absorb`/`getValue`/`propagate`, and needs **FORMAT 3 for
> every file containing a long wire** — 4-6 mw more, for less honesty. It also
> matches SPICE `T`/`O`/`U`/`LTRA`, IBIS, Touchstone and every commercial SI
> tool: **nobody models a transmission line as a special wire.**

### 2.2 LENGTH — **ABSORB**, and it is the gap the corpus does not know it has

`P4`/FEAT-047 gives TIME a physical unit. **Nothing anywhere gives LENGTH one**,
and at 20 GHz length is the primary independent variable.

Verified: `grep -rliE "impedance|transmission line|s-parameter|touchstone|eye
diagram|signal integrity|crosstalk" docs/plan/` returns **5 files, all false
positives** — four saying *"transimpedance amplifier"* (TASK-0103, CAP-12,
FEAT-049) and one saying *"high-impedance"* (FEAT-020, FEAT-026). The same grep
over `src/` returns **zero** for the HF terms. Over
`docs/capability-roadmap/*.md`, `characteristic impedance` and `transmission
line` return **zero**.

**This is stated as a measurement of a gap, never as an argument against filling
it (D10 rule 2).** It is what makes the acceptance-test checks in §7 genuine
falsification guards.

> **D-H4. LENGTH is a DECLARED optional attribute, never derived from drawn pixel
> length. This SUPERSEDES Angle 1's HF-1 `lengthbase` proposal.**
>
> Arithmetic that decides it (`src/jls/core/Geometry.java:17,19`: `CIRCUITSIZE =
> 1000` squares, `SPACING = 12`): at 1 mm per grid square the **shortest drawable
> wire** is 0.133 λ at 20 GHz — already electrically long; at 0.1 in/square it is
> 0.339 λ. A scale small enough to stay lumped (≤0.5 mm/square, valid to 30 GHz)
> makes the 1000-square canvas a **half-metre board** — a layout, not a
> schematic. And schematic wire length is unrelated to routed trace length at any
> scale. Publishing a pixels→millimetres scalar installs exactly the class of
> falsehood P4's own text condemns in zero-delay wires.
>
> **Three layers instead:** **L1** length as a parameter on the drawn line
> element (`String len "50mm"`, D-A12's SPICE-spelled string, zero format
> version); **L2** an OPTIONAL DECLARED physical length on a net, absent by
> default, riding FEAT-047's FORMAT 3 bump; **L3** back-annotation from the routed
> board with CAP-05. An undeclared net reports **"not assessable"**, never a
> fabricated verdict.

**A format hazard nobody priced, and the cheap door expires.**
`docs/file-format.md:220` is normative — *"Unknown attribute names are silently
ignored"* — and `:464-475` records the precedent that this silently drops data. A
dropped lint input is fail-open and harmless; a dropped **constraint** that is
then not emitted to a board file is a silently unmanufactured requirement.
FEAT-047 already requires FORMAT 3 for exactly this reason (AC-4). **Ship the
length/impedance attribute inside FEAT-047's bump: ~1.5-3 mw incremental now,
versus FEAT-013's must-understand per-section versioning at 4-7 mw later.** This
is a sequencing decision worth 4-7 mw and **it expires when FEAT-047 merges.**

### 2.3 The value domain — **ISOLATE**, and no refinement of it ever reaches here

`docs/simulation-semantics.md:42-66` is NORMATIVE and verified: two-state
`BitSet`, HiZ is a **null reference**, *"There is no unknown/X state anywhere in
the simulator"*, *"Nearly every element's `react` treats a null (HiZ) input as
zero"*.

**It fails twice over, independently.**

*By CONTINUITY.* Fraction of a unit interval spent in transition, `2·t_r/UI`:

| link | UI | at t_r = 8 ps | 10 ps | 15 ps |
|---|---:|---:|---:|---:|
| 25 Gb/s NRZ | 40.0 ps | 40% | 50% | 75% |
| PCIe 6.0 PAM4 (32 GBd) | 62.5 ps | 25.6% | 32% | 48% |
| 112G PAM4 (56 GBd) | 35.7 ps | 44.8% | 56% | 84% |
| **224G PAM4 (112 GBd)** | **17.86 ps** | **89.6%** | **112%** | **168%** |

At 224G PAM4 the signal is in transition **more than all of the time** — the
waveform never reaches a level. Combined with §1.1's 2.71% surviving amplitude,
*"the value on this wire is 1"* has no referent. The observables are eye height
in mV, eye width in ps and BER, none of which is a function of a discrete value
at a discrete time.

*By CARDINALITY.* PAM4 needs an alphabet of **four ordered signal levels**.
P1/FEAT-026 supplies `{0, 1, X, Z}` — don't-know and not-driven. **The wrong
four.**

> **D-H5. REFUSED: reaching this regime by enriching the digital value domain
> (more states, a strength lattice, or PAM4-as-radix-4). The industry does not
> represent these in a digital value domain either — IBIS, IBIS-AMI and
> statistical channel analysis exist precisely to keep the continuous signal out
> of it. WORKS INSTEAD: the analog boundary D-A2/D-A4 already specifies, with
> STATISTICS exported across it rather than samples.**
>
> **Consequence for sequencing, and it saves real weeks:** P1's strength lattice
> (FEAT-027) and the whole MVL programme are **ORTHOGONAL** to 20 GHz, not
> stepping stones to it. The strength lattice is a drive-CONTENTION model; this
> regime's problem is PROPAGATION. **Anyone sequencing HF behind P1 is sequencing
> it behind work it does not need.** FEAT-027 remains worth having here for one
> narrow reason — a driver's output impedance is the source resistance the line
> needs — and that is a *beneficial*, not a *required*, relationship.
>
> **And D-A7 gets STRONGER, not weaker.** A PAM4 slicer is **three drawn `Adc`s
> at three thresholds plus a thermometer decoder** — exactly what *"an n-bit
> converter is DRAWN, not parameterised"* intends. Crossing policy P-b (bisect to
> the earliest integer TICK) **is** a receiver slicer, and its stated payoff —
> the goldened digital stream becomes independent of the LTE controller, the
> integration order and step-rejection policy — is worth **more** at 20 GHz than
> anywhere else, because that independence is exactly the property a bit-error
> measurement needs.

### 2.4 Multi-driver resolution — **REFUSE THE FRAMING** (it is a category error)

`docs/simulation-semantics.md:432` — *"the first active driver in net order
wins"* — is not a bad rule here; it is answering a different question. Two
drivers on a 20 GHz conductor is a **forward/backward wave superposition** with a
defined analog answer. **No refinement of the strength lattice reaches a wave
equation.** The rule stays exactly as it is for digital-domain nets and gains one
sentence: analog-domain nets have no driver-resolution concept, because
`hasInput()` is false on them by construction (D-A4).

### 2.5 TIME and the DELAY MODEL — **ABSORB**, three amendments, all inside FEAT-047

**Range is NOT the problem.** Computed against FEAT-047's grammar (AC-1:
magnitude 1/10/100, units s..fs):

| base | `defaultTimeLimit` = 1e8 ticks | 2^53 (double-exact) | 2^63 | ticks per 20 GHz period |
|---|---:|---:|---:|---:|
| 1 fs | **100 ns (2,000 UI)** | 9.007 s | 9,223 s (2.56 h) | 50,000 |
| 100 fs | 10 µs | 900.7 s | 10.7 days | 500 |
| **1 ps (D-A9 default)** | 100 µs | 9,007 s (2.5 h) | 106.8 days | **50** |
| 1 ns | 0.1 s | 104 days | 292 years | 0 |

2^63 femtoseconds is 2.56 hours of simulated time — five orders beyond anything
reachable (1e19 steps at 5 µs/step is 1.7 million years of compute). **The
grammar as designed is adequate, including for the 100-200 fs RMS jitter budgets
of a 112G link.** FEAT-047 is correct and its 2^53 assertion (AC-5) is well
placed.

**Three real breaks, all inside a feature already owed by SDF #89, Liberty #87,
SDC #93 and analog S4:**

**(a) `defaultTimeLimit` is a policy constant that outlived its premise.**
Verified `src/jls/JLSInfo.java:69` = `100000000`, consumed at
`src/jls/sim/Simulator.java:38`. At a 1 fs base that is **100 ns = 2,000 UI at
20 Gb/s** — a CDR does not lock in 2,000 UI. It must become a **unit-aware,
per-run quantity whose default is expressed in seconds** when a base is declared.

**(b) Declaring a time base retroactively assigns physical meaning to the whole
default delay library, and FEAT-047 is silent on it.** Verified
`docs/simulation-semantics.md:264-288` and `src/jls/elem/Adder.java:33,261`
(`defaultPropDelay = 30`, `propDelay = bits * defaultPropDelay`):

| base | NAND (5) | AND (10) | Register (50) | 32-bit Adder (960) | verdict |
|---|---:|---:|---:|---:|---|
| 1 ps | 5 ps | 10 ps | 50 ps | 960 ps | plausible; implies a **1.04 GHz ceiling** |
| 1 fs | 5 fs | 10 fs | 50 fs | 0.96 ps | **absurd** — faster than light crossing a transistor gate |

**There is no global scale factor that makes the shipped table simultaneously
true and a 20 GHz clock expressible.** FEAT-047 must state (i) whether default
delays rescale with the base or are declared unit-less integers whose physical
meaning is the user's responsibility, and (ii) that *"declared base with no delay
source"* is a reportable condition. FEAT-047's AC-2 (*absent by default keeps
every golden byte-identical*) is necessary but **not sufficient**.

**(c) The `>2^53` split representation stops being hypothetical.** FEAT-047 and
`11-analog-determination.md` §2.7 both describe it as *"asserted, not
discovered"*. A BER 1e-12 measurement needs 50 s of simulated time (1e12 bits at
20 Gb/s) against 2^53 fs = 9.007 s. **This regime is the first consumer in the
entire corpus to actually force it** — and it forces it only on the transient
path this determination refuses in §4, so it is a documentation obligation rather
than a build obligation. Record it as such.

**Cost: ~1-2 mw, inside FEAT-047. It should be fixed there, not discovered.**

**(d) Two decouplings the analog determination now needs.** If any SI circuit
declares a 1 fs base: the analog golden's resample lattice must be a **separately
declared `tstep`**, not the tick lattice (5e10 rows otherwise); and D-A7's policy
P-b bisection must terminate at a **declared crossing resolution**, not the full
tick lattice, or it costs `ceil(log2 2500) = 11` extra Newton solves per crossing
instead of ~1. Both are amendments to §2.10 and D-A7, not redesigns.

> **D-H6. Keep 1 ps as the global default (D-A9 unchanged). SI circuits declare a
> finer base. Amend FEAT-047 with (a), (b) and (c); amend §2.10 and D-A7 with
> (d).**

### 2.6 The event queue — **ABSORB**, five lines, and it is the RIGHT mechanism

**It does not break.** Measured (BRIEF §13): 3.14 M events/s warm, 318 ns/event,
~386 reacted events per clock cycle on the shipped 228-logic-element RV32I.
Cost depends **only on cycles simulated**:

| clock | span | cycles | events | wall |
|---|---|---:|---:|---:|
| 20 GHz | 1 µs | 2.0e4 | 7.72e6 | **2.46 s** |
| 20 GHz | 1 ms | 2.0e7 | 7.72e9 | 41 min |
| 100 MHz | 1 ms | 1.0e5 | 3.86e7 | 12.3 s |
| 1 Hz | 20,000 cycles | 2.0e4 | 7.72e6 | **2.46 s** |

Frequency never enters the engine. And the eye window is affordable **on the
existing digital engine**: 1e5 UI at 20 Gb/s is 5.0 µs simulated = 1.0e5 cycles =
**12.3 s**, which means a digital PRBS generator and error counter can run
alongside a channel for exactly the window an eye needs, with one to two orders of
margin.

**Architecturally the answer is the one `11-analog-determination.md` already
chose, and this regime VALIDATES it rather than challenging it.** D-A8 inverts
XSPICE's time ownership: the digital loop owns `now`, the analog region is a
self-scheduling element in `Clock`'s idiom (`Clock.java:392,421`), only accepted
results become posted events, `EVTbackup` is deleted. Three ways this regime makes
that decision *better*:

1. **A channel solve is LTI** — no Newton, no limiting, no gmin stepping, no
   convergence failure, fixed step. That is the **easiest possible case** for the
   inversion, and it means analog **S10** ("the stage that kills implementations",
   6-10 mw) is not on this critical path at all.
2. **The documented A-STEP pathology inverts.** The naive conservative cap costs
   ~875,000 wasted analog visits per boundary event for a 44.1 kHz `Dac` against a
   100 MHz clock. At 25 Gb/s the transmitter changes every 40 ps, so
   `nextEventTime() − t` is a **tight, useful** cap and digital and analog rates
   are comparable.
3. **A delay line does not touch A-STEP at all.** The delay couples the analog
   region's *private* time `t` to `t − Td`, entirely inside one `react()` at one
   integer `now`. Engine footprint beyond the `nextEventTime()` accessor: **zero
   lines.**

**Engine footprint: the five-line `nextEventTime()` peek already priced in analog
S5.** One caution with a number: `jls.sim` has only **8 addable-uncovered LINE
and 3 BRANCH** against its 93.0/92.0/84.5 floor (`pom.xml:449-471`, verified), so
five untested lines would consume 5 of the 8. **Rule: full coverage on the same
commit.** ~0.1-0.2 mw.

> **D-H7. REFUSED as a framing: "the event queue is the wrong mechanism for
> 20 GHz". Measured, it is frequency-independent and is 0.001% of the cost of the
> thing that is actually infeasible. The mechanism that is wrong for this regime
> is transient simulation of any kind, for one specific question (§4).**

### 2.7 One new clause the determinism rulebook does not have — **ABSORB**

`11-analog-determination.md` §4.1 states five controls (D-1..D-5) beyond
`strictfp`, grounded in the measured finding that `Math.exp` and
`StrictMath.exp` differ in 96,260 of 1,000,000 sampled inputs. **None of them
covers complex arithmetic or the FFT**, and both are new byte-determinism
surfaces the statistical path introduces:

- **Complex multiply/divide** have multiple algebraically equivalent formulations
  with different rounding (Smith's algorithm vs the naive division form;
  `hypot` vs `sqrt(re²+im²)`).
- **FFT twiddle-factor generation and butterfly ORDER.**

The precedent is already measured in the same corpus: ngspice 42 with
`.options klu` vs `.options sparse` differ in **987 of 1,022 rows** on one
machine with one binary, because floating-point addition is not associative and
the algorithm choice changed.

> **D-H8. Add clause D-6: all complex arithmetic uses ONE pinned formulation, and
> all spectral transforms use a fixed-radix implementation with a
> `StrictMath`-generated twiddle table and a fixed butterfly order. This
> SPECIFICALLY FORBIDS absorbing any FFTW-style planning library, whose entire
> design is to select a different algorithm per machine — the exact mechanism that
> produced the ngspice KLU/Sparse divergence.** ~0.5 mw, inside analog S2, and it
> must be **written before** any spectral code, not discovered after.

**A favourable correction while we are here.** The `StrictMath` penalty is
argument-range dependent and was measured only over the diode range. Measured
this corpus: `exp` over [0,30] is 15.85 vs 9.18 ns (**1.73× slower**, reproducing
the determination's 1.66-1.73×), but over [−1e-2, 0] — the range a
recursive-convolution kernel visits — `StrictMath.exp` is 7.81 ns vs `Math.exp`
9.19 (**1.18× FASTER**), and `StrictMath.cos` is 7.94 vs 15.16 (**1.91× faster**).
**D-1 costs this regime nothing**, and §8.7's kill criterion on the `StrictMath`
penalty should be re-scoped to the diode/BJT `load()` loop where it was measured.

### 2.8 Ground and the return path — **ISOLATE**, with a mandatory documented boundary

`11-analog-determination.md` §2.1/§2.4 makes `Ground` a **required drawn
element**, which is correct and is one of that document's best ideas. At 20 GHz
*"ground"* is not a node: the return path is a distributed structure, and
**return-path discontinuity at vias, connectors and layer changes is the dominant
real-world channel defect.**

> **D-H9. A distributed/channel element carries its OWN reference terminals and
> does NOT share the analog region's `Ground` node. The docs must state that the
> ideal `Ground` is honest below the electrically-short threshold and false above
> it.** ~0.25 mw of design and documentation. **Silence here would teach a
> falsehood at exactly the point where it costs the most**, and it must not be
> skipped.

### 2.9 Summary table

| assumption | verdict | reason |
|---|---|---|
| `WireNet` is a lumped equipotential node | **ISOLATE** as an element between two nets | Dies by cardinality: 6.9 simultaneous values on one 50 mm net at 20 Gb/s vs one `BitSet`. Equipotentiality is `WireNet`'s definition, not a parameter. Element route: 66 lines, zero format version. Net-kind route: +4-6 mw, FORMAT 3, and less honest. |
| LENGTH has no unit anywhere | **ABSORB** as a declared optional attribute | Nothing in 57 features gives length a unit; at 20 GHz it is the primary independent variable. Declared per net + per element + back-annotated — never pixels→mm (shortest drawable wire is already 0.133 λ). |
| Value domain = two states + HiZ | **ISOLATE** behind the analog boundary | Fails by continuity (112% of a UI in transition at 224G PAM4) and by cardinality (PAM4's four levels are not `{0,1,X,Z}`). The industry does not put it in a digital value domain either. **P1/MVL are orthogonal, not prerequisites.** |
| First-active-driver-in-net-order resolution | **REFUSE the framing** | Category error: two drivers on a 20 GHz conductor is a wave superposition. No strength-lattice refinement reaches it. The rule is right for digital nets and gains one scoping sentence. |
| Time is a dimensionless 64-bit integer | **ABSORB** — FEAT-047, already owed | Range is fine (2^63 fs = 2.56 h). Three amendments: unit-aware `defaultTimeLimit`, a stated policy for the default delay table under a declared base, and the >2^53 split promoted from asserted to documented. ~1-2 mw. |
| Per-element delay is a lumped scalar (`Adder` = bits×30) | **ABSORB** — add transition time beside propagation delay | P4's `DelayModel` carries rise/fall DELAYS and **no SLEW**; `sweep-02-timing.md:110` states the gap verbatim. Every SI rule is a function of `t_r`. Already owed to SDF #89, Liberty #87, SDC #93. |
| The discrete event queue | **ABSORB** — five lines | Cost is frequency-independent (2.46 s per 1 µs at 20 GHz *and* at 1 Hz). Survives untouched; D-A8's inversion fits this regime **better** than the audio capstones it was designed for. |
| Determinism rulebook D-1..D-5 | **ABSORB** — add D-6 | Complex arithmetic and FFT butterfly order are uncovered surfaces with a measured precedent for divergence. ~0.5 mw, written before the code. |
| Ideal `Ground` as a global datum | **ISOLATE** with a documented boundary | At 20 GHz the return path is distributed; the channel element carries its own reference terminals. ~0.25 mw of docs that must not be skipped. |

---

## 3. THE ROBUSTNESS ANSWER: ONE MECHANISM, PRECISELY SPECIFIED

This is the maintainer's real question and the most valuable output available
here. **The answer is yes, with one qualification that makes it stronger.**

### 3.1 The convergence that already happened, twice, independently

**MVL** (`07-mvl-determination.md` §3.1): *"Radix lives on the PORT and the NET,
never in the value. The alphabet is a static property of the drawing, resolved at
elaboration."*

**Analog** (`11-analog-determination.md` D-A4): *"Domain lives on the PORT and the
NET, never in the value, resolved at elaboration. This is the MVL
radix-on-the-port mechanism verbatim, and it is built ONCE."*

And D-A5 already generalised it and priced it: `PortAlphabet {domain, radix,
encoding, strength}` — **2.45 mw paid by whichever regime ships first, ~0.4 mw
for each of the others.** That is four regimes already named, priced and decided.

### 3.2 Does high frequency fit as a fifth field? — No, and that is the finding

The temptation is `PortAlphabet {domain, radix, encoding, strength, distributed}`.
It does not work, and the reason is what makes the architecture robust.

Every existing field answers one question: **"given that this net has a value,
what may that value be?"** — `strength`: how hard is it driven and who wins;
`radix`: how many code points per position; `encoding`: how are they laid out;
`domain`: is it a discrete code point at all, or a real number the solver owns.

**High frequency does not answer that question. It denies its premise.** §2.1
computed 6.9 simultaneous distinct values on one net. *"What may the value on
this net be?"* has no answer — not because the alphabet is too small, but because
**there is no *the* value.**

> **The four alphabet regimes are refinements of *what a signal is*. The fifth
> denies that a net has *a* signal.** A port descriptor can express the first
> kind of statement forever. It cannot express the second, because the port
> descriptor's own subject — *this port's value* — is the thing being denied.

### 3.3 THE LAW

> ### **D-H10. Anything that changes what a value MEANS goes on the PORT.**
> ### **Anything that changes whether a net HAS a value goes INSIDE an ELEMENT.**

| axis | question | mechanism | regimes served | resolved where |
|---|---|---|---|---|
| **ALPHABET** | *given a value, what may it be?* | `PortAlphabet {domain, radix, encoding, strength}` on `Put` and `WireNet` | four-state (FEAT-026); strengths (FEAT-027); radix / MVL; analog-continuous (D-A4) | **elaboration**, never carried in the value |
| **EXTENT** | *does this net have one value at all?* | **containment inside an element**: `SubCircuit impl "analog"` for a region; an N-terminal `Element` for a distributed structure | distributed / high-frequency | **the drawing** — the student draws the container |

**Both mechanisms are already funded.** The alphabet mechanism is D-A5's 2.45 mw.
The extent mechanism is D-A2's containment plus the measured ~66-line element
registration tax, both inside the analog programme.

**And they COMPOSE, which is what makes the architecture closed.** A distributed
element's terminals are analog-domain ports, so the extent mechanism *consumes*
the alphabet mechanism. **High frequency is not a fifth case: it is a consumer of
the fourth, plus one ordinary element.** That is the whole reason §6's core cost
is under ten weeks for the regime the brief framed as most likely to break
everything.

### 3.4 Why this is robust rather than merely tidy — four properties

1. **The alphabet mechanism is open under extension with ZERO cost per event.**
   Because the alphabet is *checked at connection* and *erased at elaboration*,
   adding a field costs four `SimpleEditor` connect sites, one `recheck`
   validation, one elaboration assertion and two `infoText` arms — D-A5's fixed
   ~0.4 mw — and nothing in the hot loop. Verified structurally: every
   `.propagate(` call site in `src/` is `<someOutput>.propagate(value, now, sim)`
   inside an element's `react`, so a net with no `Output` is never propagated.
   *(Honest caveat: the driver **resolution fold** is paid once, by P1, when
   `strength` arrives. `domain` and `radix` then cost nothing further.)*
2. **The extent mechanism is already universal.** An element with N terminals is
   the general container for any physics JLS will ever isolate: a transmission
   line, an S-parameter block, a coupled pair, an IBIS buffer, an entire analog
   region. No extension point, no ABI, no plugin — D7's *"circuit libraries are
   DATA, not plugins"* holds verbatim.
3. **Neither mechanism is reachable by a first-year.** Non-default alphabets
   require an explicit named action; extent containers are palette entries under
   D-A10's view dimension. **K9 is satisfied structurally, not by a preference
   toggle.**
4. **The refusal it makes is principled and statable.** Anything that is neither
   an alphabet refinement nor an extent container is out of scope *by
   construction*, and the boundary is written once in `ARCHITECTURE.md` with a
   revisit trigger rather than re-argued per feature.

### 3.5 The third axis, which the two mechanisms do NOT cover — UNITS

Stated separately because it is the axis with a missing owner:

| unit | maps | mechanism | status |
|---|---|---|---|
| **TIME** | integer tick → seconds | one optional `CIRCUIT` attribute, VCD `$timescale` grammar, recomputed never accumulated, FORMAT 3 iff present | **FEAT-047, 2-3 mw, owed by SDF/Liberty/SDC/analog** |
| **LENGTH** | *nothing* → metres | **NOT a `CIRCUIT` attribute** (§2.2). Declared per net (L2) or per element (L1); back-annotated from the board (L3) | **NO OWNER in 57 features. This determination assigns it to FEAT-058, 1.5-3 mw** |
| **VOLTAGE** | discrete level → volts | **deliberately per-element**: `Adc(vlow, vhigh)`, `Dac(vlow, vhigh, vhiz)` per D-A7 | **already correct** |

The voltage row looks like an inconsistency and is not. Time and length are global
because there is one physics of propagation in a design. Voltage is not global
because a real board has multiple rails — 1.2 V core, 3.3 V I/O, ±12 V analog —
and a per-circuit *"logic 1 = 3.3 V"* scalar would be false the moment a level
shifter is drawn. **The rule generalises: a unit is global iff the physics it
measures is global.**

### 3.6 The exact text to write, once

Three documents change; all three changes are owed anyway.

**`docs/simulation-semantics.md` §2**, re-anchored during P1's own motion (the MVL
determination establishes this costs a paragraph and that the re-anchored sentence
is better writing even if radix stays 2 forever). Add two clauses:

> The value domain is a **per-position alphabet**. The shipped alphabet is
> `{0, 1, X, Z, U}` at radix 2. **The alphabet a port and a wire net speak — its
> domain, radix, encoding and drive strength — is a static property of the
> drawing, resolved at elaboration, never carried in the value.**
>
> **A wire net is equipotential and instantaneous: every point on it holds one
> value at one instant.** This is a definition, not an approximation, and it is
> the reason a net has one `value`. Where that is physically false — a conductor
> long enough that a signal's transition time is comparable to its propagation
> delay — **the distributed structure is modelled as an ELEMENT between two nets,
> never as a property of a net.** §N states the electrical-length rule and the
> diagnostic that reports it.

**`docs/simulation-semantics.md` §9** gains one sentence: the resolution rule
governs digital-domain nets; analog-domain nets have no driver-resolution concept
because `hasInput()` is false on them by construction.

**`ARCHITECTURE.md`**, as a recorded decision in the #221 / i18n idiom:

> ### The alphabet/extent split, and the boundary of the physical model (recorded 2026-08)
>
> JLS carries two orthogonal extension mechanisms and no others.
> **(1) ALPHABET** — what a value may be — is a property of ports and wire nets
> (`PortAlphabet {domain, radix, encoding, strength}`), resolved at elaboration
> and never carried in the value. Four-state, drive strength, non-binary radix and
> the analog continuous domain are all this mechanism.
> **(2) EXTENT** — whether a net has one value at all — is expressed by
> containment: an `Element` with N terminals, or a `SubCircuit` with a
> non-structural implementation. Transmission lines, lossy channels, S-parameter
> blocks and whole analog regions are all this mechanism.
> **A wire net is equipotential by definition and is never extended to be
> otherwise.**
> **Boundary:** JLS models signal integrity to the digital/analog boundary —
> channels, equalisation, eye and BER. It does not model microwave circuit design
> (matching networks, LNAs, mixers, antennas), and it does not perform 3-D
> full-wave field solving; it **reads** the S-parameters someone else's field
> solver produced, which is why Touchstone exists.
> **Revisit trigger:** a capability that is neither an alphabet refinement nor an
> extent container, with a named consumer.

**Cost of §3.6: 0.5-1 mw**, and the re-anchored sentence is better writing on its
own merits.

---

## 4. THE METHOD: TRANSIENT VERSUS STATISTICAL, DECIDED ON ARITHMETIC

### 4.1 The measured rates everything below divides by

All Java, all measured in this corpus, on two different machines that agree in
order and direction within 1.6-2.6×:

| operation | rate |
|---|---|
| Dense LU factor+solve, N=28 (channel-sized MNA) | **2.388 µs** (Angle 7) / **3.765 µs** (analog determination) |
| Same, N=201 / N=324 / N=501 | 128.3 / 400.3 / 1312 µs |
| Banded solve, packed storage, fixed elaboration-time ordering, bw=2 | N=201 **6.795 µs** (18.9×), N=501 **11.83 µs** (111×) |
| Solve-only with the factorisation cached (linear fast path), N=24 | **0.545 µs** |
| Newton iterations per accepted timepoint, linear signal path | 2.00 |
| JLS warm event loop | 318 ns/event (3.14 M/s) |
| Peak-distortion sweep, 4096 taps × 64 phases | **0.858 ms** (Angle 7) / 0.330 ms (Angle 5) |
| Radix-2 FFT, N=65,536 | 2.699 ms |

### 4.2 The brief's own challenge, computed honestly

**1e12 femtosecond steps over 1 ms of simulated time:**

| basis | wall clock |
|---|---:|
| event queue alone, doing zero physics (318 ns/event) | 3.68 days |
| linear, dense LU N=28 at 2.388 µs | **27.6 days** |
| nonlinear driver, ×2.00 Newton iterations | **55.2 days** |
| dense LU N=50 at 13.839 µs | 160 days |
| transistor-level (15.0-26.3 µs/timepoint) | 174-304 days |

> **This is slow. It is not impossible, and the determination must not say it is.**
> A document that claims 1e12 steps is physically unreachable would be found false
> by the maintainer in an afternoon. What is true is narrower and more useful:
> **1 fs is the wrong step size.** 1 fs is 50,000 points per 20 GHz period,
> **2,500× finer than the 10-20 points/period convention**, against a second-order
> integrator whose roundoff floor already dominates. Channel simulators use
> **25-32 samples per UI = 1.56-2.0 ps at 20 Gb/s**. At 2.5 ps and N=201, 1 µs is
> **2.2 s** and 1 ms is **36.7 minutes**. *One microsecond — 20,000 UI — is where
> JLS's HF teaching lives, and it is interactive.*

### 4.3 The number that IS impossible, and it is not the step size — it is the BIT COUNT

Poisson confidence, not folklore: observing zero errors in N bits bounds
`BER < −ln(0.05)/N`, so **N = 3/BER at 95% confidence.** (The common *"1e12 bits
for 1e-12"* understates by 3× and carries no stated confidence.)

BER 1e-12 → 3e12 bits → **150 s of simulated time at 20 Gb/s** → 7.5e13 steps at
25 samples/UI:

| BER target | bits | simulated time | wall clock (linear, measured) |
|---|---:|---:|---:|
| 1e-6 | 3e6 | 150 µs | ~3 minutes |
| 1e-9 | 3e9 | 150 ms | ~2 days |
| **1e-12** | **3e12** | **150 s** | **5.7-9.0 years** |
| 1e-15 | 3e15 | 42 hours | 5,700-9,000 years |
| 1e-18 | 3e18 | 1,900 days | 5.7e6-9.0e6 years |

Multiply by 2.0-2.5 for a nonlinear driver. **And the bathtub cannot extend below
`1/N_bits` at any rate** — a million-bit run can never report below BER 1e-6.

**Independent cross-check against the commercial state of the art.** Published
IBIS-AMI time-domain rates are *"millions of bits per minute"*, putting 1e12 bits
at **34.7 days to 1.90 years**; and the industry states its own ceiling plainly —
time-domain AMI simulation *"can normally be used to compute probabilities of bit
error rate in the range of 1e-6 to 1e-8"*. **Same order of magnitude. This is a
property of the method, not a deficiency of JLS.**

### 4.4 The method that works, measured

Statistical / peak-distortion channel analysis (Casper, Haycock & Mooney, *IEEE
Symposium on VLSI Circuits*, 2002; and normatively, IEEE 802.3 Annex 93A's
Channel Operating Margin, *"a statistical based algorithm ... based on linear
time invariant assumptions about the channel, transmitter and receiver"*).

Components, measured:

| step | cost |
|---|---:|
| Pulse response by short transient (30-300 UI × 32-64 samples/UI, N=201) | 5.3-105.6 ms |
| Pulse response by AC + IFFT (two 65,536-point FFTs) | 5.4 ms |
| ISI cursor decomposition + per-tap PDF convolution (32-64 phases × 100-300 taps × 1024-4096 bins) | 3.9-94.4 ms |
| Peak-distortion sweep, measured directly | 0.33-0.86 ms |
| **Full statistical eye + bathtub + BER contour** | **0.02-0.24 s** |

**And its cost is FLAT in the BER target**, because the PDF tail is evaluated
analytically. BER 1e-18 costs exactly what BER 1e-6 costs.

| BER target | transient | statistical | ratio |
|---|---:|---:|---:|
| 1e-6 | ~3 min | ~0.1 s | ~2e3 |
| 1e-9 | ~2 days | ~0.1 s | ~2e6 |
| **1e-12** | **5.7-9.0 yr** | **~0.1 s** | **~2e9** |
| 1e-18 | 5.7e6-9.0e6 yr | ~0.1 s | **~2e15** |

The ratio gains **three orders per BER decade**, because transient is `O(1/BER)`
and statistical is `O(1)` in BER. **No cluster, no C rewrite, no JIT and no
CAP-17 closes an asymptotic gap.** A perfect 1000× cluster turns 5.7 years into
2.1 days — for ONE channel, ONE EQ setting, ONE corner, against a link budget
that sweeps 1e3-1e5 of those.

### 4.5 The decision

> **D-H11. REFUSED: brute-force transient simulation as the method for a
> bit-error rate. This is a physical/mathematical limit shown by arithmetic — the
> one form of refusal D10 explicitly permits — and it refuses a METHOD, not a
> CAPABILITY.**
>
> **WORKS INSTEAD: LTI channel characterisation + statistical / peak-distortion
> eye analysis**, ~0.1 s to any BER, priced at **9-16 mw**, hybridised with SHORT
> bit-by-bit transient (**1e4-1e6 bits = 0.03 s to minutes**, not years) for the
> parts that are genuinely not LTI.
>
> **The LTI boundary is standardised in the AMI API itself and must be documented
> rather than discovered:** `AMI_Init` treats transmitter and receiver as LTI and
> convolves impulse responses (statistical); `AMI_GetWave` takes a bit stream
> (time domain). *"Non-LTI features like CDR, gain compression, and DFE cannot be
> comprehensively handled with AMI_Init."* **Invalidity list to write into the
> capstone text: DFE with adaptation, CDR, AGC, gain compression, driver
> saturation, any limiting receiver front end.**
>
> **Note what this does NOT refuse.** Transient computes the impulse response
> once, in microseconds of simulated time. The analog solver remains a legitimate
> impulse-response source. The thing abandoned is the idea that **BER comes from
> simulating bits.**

### 4.6 The goldening rule this forces, and it costs nothing now

Determinism survives 1e9+ steps unimpaired — it is the property of executing the
same IEEE-754 operations in the same order, verified across seven JVM
configurations to md5 `13cbc7cb3ff955229f8c7f246f185c9a`, and nothing in that
mechanism is step-count dependent. Roundoff at 1e9 steps is 7.02e-12 relative
(`√n·ε`) and is never binding before wall clock is.

**The real hazard is a divergence amplifier the corpus already characterised:** the
LTE controller is a floating-point feedback loop in which a 1-ulp difference flips
an accept/reject and re-grids everything downstream. Measured analogue: *"A 1 mV,
1 Hz PPG signal moves its threshold-crossing tick by ~1.6e5 ticks for a 1 nV
solution perturbation. Cross-platform the perturbation is exactly zero."*

> **D-H12. Golden HF results on eye height, eye width and margin within a stated
> tolerance — never on a waveform sample, never on a crossing tick, never on the
> accepted time grid.** This is the HRM beat-count rule transplanted. It costs
> nothing now and a great deal if discovered at stage 8.

---

## 5. THE HONEST TARGET

### 5.1 "JLS supports 20 GHz" is five separable deliverables, not one

Folding them into a single claim is what makes 20 GHz look like a second JLS.

| rung | what it is | cost (cumulative, incl. prerequisites) |
|---|---|---:|
| **R1 DIAGNOSE** | The tool states its own domain of validity: a net's electrical length against its driver's edge rate, with the verdict and the arithmetic | **5.5-10 mw** |
| **R2 DEMONSTRATE** | A drawn transmission line: reflection, overshoot, ring-down, and the termination that fixes them | **7-11 mw** |
| **R3 DELIVER** | The electrical intent leaves JLS as a constraint file a real board tool enforces on a real board | **11-19.5 mw** |
| **R4 MEASURE** | Touchstone in, statistical eye / bathtub / BER contour out | **22-38 mw** |
| **R5 VENDOR MODELS** | IBIS-class behavioural I/O buffers | **+19-33 mw** |

**R1 + R2 + R3 = CAP-18**, marginal 11-19 mw, cumulative 14.5-25 mw.
**R4 is a separate capstone** (§6.5). **R5 is declined with a named re-open
condition.** Full 3-D EM is refused (§8).

### 5.2 The definition

> **D-H13. "JLS supports 20 GHz" means: a JLS design can DECLARE the two physical
> facts that put it in the regime (a driver's edge rate and a net's physical
> length); JLS can TELL the designer, correctly and with the arithmetic shown,
> when a net has stopped being a wire; JLS can SHOW what happens on that net and
> what fixes it; and JLS can EXPORT the electrical intent that encodes the fix in
> a form a tool JLS does not control will enforce.**
>
> It does **not** mean JLS computes a channel's insertion loss from geometry, and
> it does **not** mean JLS produces a bit-error rate. Those are R4's claim and
> R4's capstone.

### 5.3 The TEACHING win

**R1 + R2, and the reason is the reframe.** The teaching content is not "20 GHz";
it is *"the regime is entered by edge rate, and your circuit is already in it."*

The reflection lab, computed two independent ways this session (a hand
lattice/bounce diagram, and an implementation of the closed-form superposition
`v_far(t) = (1+Γ_L)·A·Σ_k (Γ_s·Γ_L)^k · v_s(t − (2k+1)T_d)` with a finite-rise-time
ramp), agreeing **to the printed digit** — 3.3 V rail, Z₀ = 50 Ω, T_d = 345.6 ps
(5 cm of FR-4 stripline):

| termination | Γ_s | Γ_L | launch | far-end peak | ring-down |
|---|---:|---:|---:|---:|---|
| R_s = 10 Ω, far end open | −0.6667 | +1.0000 | 2.7500 V | **5.5000 V = 166.7%** | 1.8333 / 4.2778 / 2.6481 / 3.1914 V |
| R_s = 50 Ω (series terminated) | 0.0000 | +1.0000 | 3.3000 V | **3.3000 V flat** | none |
| R_L = 50 Ω (parallel terminated) | −0.6667 | 0.0000 | 2.7500 V | **2.7500 V flat** | none |
| R_s = 10 Ω open, **t_r moved to 1 ns** | — | — | — | **4.3675 V = 132.3%** | collapsing |
| same, t_r = 5 ns | — | — | — | **under 105%** | negligible |

**Five things a student sees that are invisible in the `{0,1,Z}` value domain:**
`Γ = (R_L−Z₀)/(R_L+Z₀)`, the round-trip staircase, overshoot above the rail,
series versus parallel termination, and — **the fifth, which no other option in
this space delivers and which is the control experiment for the whole reframe** —
*move only the edge rate and watch the lesson disappear.*

**And it connects to a funded capstone at zero marginal cost.** CAP-04's
*"it works in the simulator and fails on the breadboard"* has a mechanical answer
for the first time: 150 mm of jumper is 2.1× critical length for 74AC and 0.24×
for 74LS.

### 5.4 The COMMERCIAL-BRIDGE win

**R3, and it is not the eye diagram.** A commercial SI engineer already owns a
tool that computes eyes; what they lack is a clean path from logical intent to
physical constraint without retyping. **That is a data-carriage problem and it is
exactly where D9 says JLS's differentiator lives** — the span, not competing with
an incumbent on the incumbent's ground.

**It is also the only option whose output an external tool already checks.**
Verified first-hand: KiCad's DRC rule parser accepts `length`,
`net_chain_length`, `stub_length`, `skew`, `return_path`, `diff_pair_gap` and
`diff_pair_uncoupled`; `DRC_CONSTRAINT_T` carries `LENGTH_CONSTRAINT`,
`SKEW_CONSTRAINT`, `NET_CHAIN_STUB_LENGTH_CONSTRAINT`,
`NET_CHAIN_RETURN_PATH_CONSTRAINT`, `DIFF_PAIR_GAP_CONSTRAINT`,
`MAX_UNCOUPLED_CONSTRAINT` and `DIFF_PAIR_INTRA_SKEW_CONSTRAINT`. CAP-05's
acceptance test **already invokes `kicad-cli pcb drc`.**

> **And the claim that CANNOT be made, verified rather than assumed.** KiCad's
> `NETCLASS` carries `m_TrackWidth`, `m_diffPairWidth`, `m_diffPairGap`,
> `m_diffPairViaGap`, `m_tuningProfile` and `m_Priority` — and **no impedance
> field**. There is no impedance constraint in the DRC enum either. *"JLS exports
> a controlled-impedance constraint"* would be a conformance claim no tool checks
> — the exact failure `docs/standards-adoption/` already warns about for
> JTAG/BSDL. **Emit the constraints that ARE checked, plus an impedance TARGET
> annotation with a resolved track width, documented as an annotation.**

**Sequence (a) then (b):** R3 creates the constraint set whose consequences R4
evaluates. Doing R4 first would put JLS's uncorrelated numbers next to a vendor's
correlated ones, which is worse than not competing.

### 5.5 The acceptance test

The full SEEN/CHECK form is §7's capstone document. In one sentence:

> A student declares an edge rate and a length; `jls -check` names the net,
> prints the critical length and the knee frequency, and says the lumped model is
> not valid; the student draws the line and sees **5.500 V on a 3.3 V rail**;
> a 50 Ω source resistance makes it **3.300 V flat**; moving only the edge rate to
> 1 ns collapses the peak to **4.368 V**; and the resulting length constraint,
> exported as a `.kicad_dru`, makes `kicad-cli pcb drc` **fail** an over-long
> route and **pass** the shortened one.

**The falsification guard is real:** every check fails today for every JLS
design, because no length, no edge rate and no signal-integrity vocabulary exists
at HEAD (verified §11).

---

## 6. THE PATH AND THE FULL PRICE

### 6.1 The prerequisite audit — the finding worth 24-45 maintainer-weeks

The brief posits `analog S1 → S4/FEAT-047 → S12 .ac → S9 devices`.

| product | what it needs from `jls.analog` |
|---|---|
| **A — the lint** | **FEAT-047 only.** No solver, no `AnalogElement`, no `Put` widening. |
| **B — the drawn reflection** | **FEAT-047 only**, because a lossless line with resistive terminations and a PWL source has a CLOSED FORM (§5.3, verified two ways). Plus D-A10's palette view dimension and a real-valued trace row. |
| **C — the eye and BER contour** | **FEAT-047 + S0's tick-resampler + S2's determinism discipline.** A channel is DATA (Touchstone, ASCII) and a statistical eye is array DSP over `double[]`. **Not S1, S3, S5, S9, S10, S11 or S12.** |
| **D — a drawn analog channel in the solver** | S1, S2, S3, S5, `.ac` promoted from S12, and a structured solver. **This is the only product the brief's chain describes.** |

> **D-H14. HF must not be used as a justification for building the analog solver,
> and the analog solver must not be treated as a gate on HF.** Three of four
> products need FEAT-047 and nothing else, and FEAT-047 is owed anyway by CAP-10,
> CAP-11, CAP-12 and CAP-14.

### 6.2 THE INVERSION — sequence by PERMANENCE, not by cost

**The cheapest HF deliverable commits the most permanent public surface, so the
analog determination's stop-clean rule does not transfer and must be
re-derived.**

| deliverable | cost | permanent surface committed |
|---|---:|---|
| Electrical-length lint | 1-2 mw | **none** — no element, no palette entry, no tag, no K9 obligation |
| Length/edge-rate attributes | 1.5-3 mw | one optional attribute inside a bump owed anyway |
| Constraint export | 5.5-9.5 mw | one optional versioned section; the emitted file format is external |
| **A drawn transmission line** | **2-3.5 mw** | **a frozen tag (a hard error in older readers), a mandatory palette entry under a GREEN totality test, a permanent K9 obligation** |
| Touchstone reader + statistical eye | 17-30 mw | **none**, provided the eye is an EXPORTED ARTIFACT rather than a canvas |

Verified: `test/jls/edit/PaletteContractTest.java:44-66`
(`paletteIsTotalOverTheElementRegistry`) asserts exactly one palette entry per
registered `ElementType` outside `{SubCircuit, WireEnd, TestGen}`, and at HEAD the
registry has **35** types against **32** palette entries. **A green test currently
enforces the violation**, which is precisely why D-A10's view dimension and the
K9 ratchet must ship **before** the first new element type.

> **D-H15. Order the programme: LINT → CONSTRAINT EXPORT → ELEMENT → EYE.** The
> stop-clean line sits **after** the expensive statistical work and **before** the
> cheap element, which is the opposite of the intuitive order.

### 6.3 The stages

| # | stage | delivers (visible) | mw | cumulative |
|---|---|---|---:|---:|
| **H0** | **FEAT-047 amendments + the length attribute in the same FORMAT 3 bump.** Unit-aware `defaultTimeLimit`; the declared-base policy for the default delay table; the >2^53 split documented; the optional declared physical length and impedance target on a net. **This door expires when FEAT-047 merges.** | A circuit declares `timebase "1ps"` and a net declares `len "50mm"`; every existing golden is byte-identical | **3.5-6** (of which FEAT-047's 2-3 is owed anyway) | 3.5-6 |
| **H1** | **Transition time on the delay model + THE ELECTRICAL-LENGTH LINT.** `t_r`/`t_f` beside `cell_rise`/`cell_fall` at the same arc granularity, degenerate-absent default; the lint reads both and reports. Exposed strictness constant. | `jls -check` prints, for every net that declared both: *"net CLK: length 50.0 mm, edge rate 50 ps, critical length 1.2 mm at v=1.446e8 m/s — this net is 41× electrically long; the lumped model is not valid."* **No element, no palette entry, no format risk, no solver.** | **2-4** | **5.5-10** |
| **H2** | **D-6, the sixth determinism clause.** One pinned complex-arithmetic formulation; fixed-radix transforms with a `StrictMath` twiddle table and a fixed butterfly order; no auto-planning transform library, ever. | Nothing visible. It is an ArchUnit rule and a paragraph, and it must exist **before** any spectral code. | **0.5** | 6-10.5 |
| **H3** | **SI CONSTRAINT AUTHORSHIP + PCB CONSTRAINT EXPORT.** Constraint vocabulary as an optional versioned section; netclass + `.kicad_dru` emitter; routed-length back-annotation; the DRC round-trip test on CAP-05's existing `kicad-cli pcb drc` harness. | A student sets `max_length 60mm, stub 5mm`, exports, routes at 75 mm in KiCad, and **`kicad-cli pcb drc` fails naming the net.** They shorten it; DRC returns 0. | **5.5-9.5** | **11.5-20** |
| — | **STOP AND RE-COST.** Nothing permanent has been committed. H1 is the calibration experiment; re-cost everything below from its measured weeks. | | | |
| **H4** | **THE CLOSED-FORM TRANSMISSION LINE + a real-valued trace row.** One element type, two parameters (Z₀ and length-or-delay), geometric superposition truncated at a stated tolerance; a real-valued row in the **existing** trace window, because `docs/simulation-semantics.md` §2 admits only a two-state `BitSet` or null and there is otherwise nothing to draw the waveform in. Ships **behind** the K9 ratchet and D-A10's view dimension. | **The reflection lab.** 5.500 V on a 3.3 V rail; 3.300 V flat with a series termination; 4.368 V when only the edge rate moves. | **3.5-5.5** | **15-25.5** |
| — | **CAP-18 IS COMPLETE HERE (H0-H4).** | | | |
| **H5** | **TOUCHSTONE READER + the `.sNp` inspector.** Reader/writer for Touchstone 1.x/2.x; S↔Y↔Z↔ABCD; renormalisation; cascading; interpolation; passivity/reciprocity/causality checks reported **before** the run, in D-A14's model-card-inspector idiom. Absorb `scikit-rf` (BSD-3) under D8. | *"covers 10 MHz-40 GHz in 8,001 points, no DC point (extrapolated), passes passivity at all sampled frequencies, fails causality by 3.1% of pulse energy before t=0."* A real vendor `.s4p` gets a verdict instead of garbage. | **4-7** | 19-32.5 |
| **H6** | **`jls.channel` — THE STATISTICAL EYE.** Impulse/pulse response by IFFT with DC extrapolation and band-limit windowing; ISI cursor decomposition; peak distortion; statistical eye; bathtub; BER contour; Rj/Dj jitter convolution; and a **native EQ vocabulary declared as DATA** (FFE taps, CTLE pole/zero/DC gain, DFE taps, CDR bandwidth). Array DSP, not a solver. Outputs as **goldened CSV**. | An eye diagram, a bathtub curve and a BER contour of a real 20 Gb/s channel **in under a second**, and the student moves one FFE tap and watches a closed eye open. | **9-16** | **28-48.5** |
| — | **THE EYE/BER CAPSTONE IS COMPLETE HERE (H0, H2, H5, H6).** Cumulative from zero: **22-38 mw**. | | | |
| **H7** | *(optional)* Closed-form 2-D quasi-static synthesis: Hammerstad-Jensen microstrip, Wheeler/Cohn stripline. Absorbs 633 SLOC of BSD-3 C. | A drawn cross-section with a width and a substrate reports Z₀, ε_eff(f), α_c(f), α_d(f) | 1.5-3 | — |
| **H8** | *(optional)* IBIS analog half: IBIS 7.2 reader, I/V and V/t tables on the existing PWL/B-source machinery. Absorbs KiCad's KIBIS (BSD-3, 6,784 lines). **Needs analog S5 + S10.** | A realistic transmitter model from the vendor's own file | 14-24 | — |
| **H9** | *(optional)* The drawn analog channel: `.ac` promoted from S12, method-of-characteristics line, banded/structured solve, vector fitting with passivity enforcement. **Needs analog S1-S5.** | Insertion-loss Bode plots; a lossy dispersive line inside the solver | 22-40 | — |

### 6.4 The cumulative price to the first HF deliverable, including every prerequisite

> **THE FIRST DELIVERABLE — the electrical-length lint — is 5.5-10 maintainer-weeks
> cumulative, of which 2-3 is already owed by CAP-10, CAP-11, CAP-12 and CAP-14.**
> **Net new attributable to this regime: 3.5-7 mw.**

| product | cumulative including every prerequisite |
|---|---:|
| A — the lint (H0+H1) | **5.5-10 mw** |
| A + R3 (H0-H3) | **11.5-20 mw** |
| **CAP-18 complete (H0-H4)** | **15-25.5 mw** (marginal 11-19; standalone 19-34) |
| C — the eye and BER contour (H0, H2, H5, H6) | **22-38 mw** |
| D — a drawn analog channel (analog S0-S5 + H9) | **48.5-78.5 mw** |

**Core-mechanism cost, separated from capability cost.** Of the above, the money
that buys *the ability to build the capability without a second migration later*
is: FEAT-047's three amendments 1-2; transition time 1-2; declared length + lint
1.5-3; the element registration 0.3-0.5; D-6 0.5; the normative re-anchor and the
`ARCHITECTURE.md` decision 0.5-1; the channel-reference-terminal boundary 0.25.
**Net new core: 5.05-9.25 mw = +1.2% to +3.0% of the committed roadmap.** The
`PortAlphabet` (2.45), the `Put`/`AnalogElement` widening (1.5-2.5) and
`nextEventTime()` (0.1-0.2) are already funded by D-A5 and analog S5 and must not
be double-charged.

### 6.5 What it displaces

**The denominator, computed rather than quoted:** the 18 capstone marginals sum
to **309.5-514 mw** (verified by summing the headers); the roadmap P1-P13 total is
151-220 mw de-duplicated; the analog programme is 49.5-75 mw as FEAT-045..049 but
**68.5-104.5 mw as staged** — quoting the feature figure understates by 28%.

> **D-H16. The named displacement is CAP-17 (distributed execution, 38-62 mw,
> priority 18) — the only large committed item whose deferral strands nothing.**
> Verified: FEAT-054, FEAT-055, FEAT-056 and FEAT-057 each list **CAP-17 and no
> other capstone** in their consumed-by table, and `REGISTRY.md` records CAP-17 as
> *"added at maintainer request after this registry closed"* with priority 18
> meaning *"appended, not yet ranked"*.

- **CAP-18 at 11-19 mw marginal** is roughly **CAP-13 (6-12) plus change**, or
  half of **CAP-06 (12-20)**, or about one third of the analog programme's first
  five stages. It is **+2.6% to +6.8%** of the capstone total.
- **The full ladder (CAP-18 + the eye capstone) at 33-52 mw** displaces
  **CAP-17 outright.**
- **The displacement is partly illusory in one direction that should be said:**
  H3 rides CAP-05's already-funded `kicad-cli pcb drc` acceptance harness and
  makes CAP-05 *worth more*, not later; and H1 answers CAP-04's central failure
  mode for the first time.
- **Everything queues behind CAP-00** (35-62 mw, the maintainer's own priority 1).
  Starting HF before it means drawing on a 2,897-LINE coverage commons through a
  24.64%-covered editor — and note that the batch-shaped HF deliverables draw
  almost nothing on that commons, which is a second, independent reason to do them
  before the drawn one.

**Calendar at bus factor 1:** the lint is 1.5-2.5 months; CAP-18 complete is
4-6 months; CAP-18 plus the eye capstone is 8-13 months.

### 6.6 K9 — the pedagogy floor, which costs ~0 mw incremental

**Every recommended deliverable except one is a lint, a report or an export —
surfaces a first-year never opens.** The lint, the constraint export, the
Touchstone reader and the exported eye add **zero palette entries, zero dialogs,
zero concepts and zero startup cost.** The only K9 obligation is *negative*: the
lint must be **silent** when no length is declared, which is also what makes it
correct.

The one item that costs K9 money — the drawn line — is covered by **D-A10's
existing `PaletteEntry` view dimension**, already funded by the analog programme
(which faces 22 element types against HF's one to three), whichever ships first
paying it.

> **But the K9 ratchet must ship BEFORE the first new element type** (~2 days):
> assert the default palette is exactly its current 32 buttons, assert dialog
> component names via the existing `setName` convention, and turn the startup and
> per-edit numbers into a test. `docs/virtual-hardware-parity.md:1903-1917` already
> says *"until that test exists, K9 is aspiration."*

**And the lint's CONTENT is itself the pedagogy under D9.** A third-year who
declares a length is told, in one sentence, that at a 20 ps edge a 0.48 mm trace
is a transmission line. That is the cheapest instance of progressive disclosure in
the entire corpus.

---

## 7. CAP-18 — WARRANTED, AND HERE IT IS

**Recommended.** It passes the plan's own test: a demonstrable outcome with a
SEEN/CHECK acceptance test and a demo slice that is the cheapest thing worth
building, whose rungs are each incomplete alone.

**Id space, verified:** `docs/plan/REGISTRY.md` COUNTS states *"18 capstones
(CAP-00 through CAP-17, no gaps). 57 features (FEAT-001 through FEAT-057, no
gaps). 112 tasks (TASK-0001 through TASK-0112, no gaps)"*, and the directory
listing confirms FEAT-054..057 exist. **The next free capstone id is CAP-18 and
the next free feature id is FEAT-058.** No TASK ids are minted, following the
recorded CAP-17 precedent (*"FEAT-054 through FEAT-057 have no tasks. The task id
space is closed"*).

**File it as `docs/plan/capstones/CAP-18-a-net-that-stopped-being-a-wire.md`.**
Everything between the rules below is ready to drop in.

---

# CAP-18 - A net that stopped being a wire

**Status:** proposed | **Priority:** 19 | **Marginal cost:** 11-19 mw |
**Standalone cost:** 19-34 mw

## Outcome

A drawn net that is too long for its driver's edge rate is identified as a
transmission line rather than an equipotential node, its reflection and overshoot
are shown on a trace the student made, a termination fixes them, and the
electrical intent that encodes the fix leaves JLS as a constraint file a real
board tool enforces on a real board - which is the point at which JLS stops
modelling wires as ideal.

## Acceptance test

SEEN: the student draws a 3.3 V clock driver into a flip-flop, declares the
driver's output edge rate as 50 ps and the net's length as 50 mm, and runs
`jls -check design.jls`. It prints `net CLK: length 50.0 mm, edge rate 50 ps,
critical length 1.2 mm at v=1.446e8 m/s - this net is 41x electrically long; the
lumped model is not valid`. They replace the net with a drawn transmission line
(Z0 50 ohm, one-way delay 345.6 ps, source 10 ohm, far end open) and run. The
far-end trace peaks at **5.500 V on a 3.3 V rail** and rings down through
1.833 V, 4.278 V and 2.648 V; a diagnostic names the receiving element and the
peak. The student changes the source resistance to 50 ohm and the trace is a
clean **3.300 V** step, flat, no ring, and the diagnostic clears. They then move
only the edge rate to 1 ns and watch the peak fall to **4.368 V**, which is the
lesson: the regime is entered by edge rate, not by clock rate. Finally they
declare `max_length 60mm, stub 5mm` on the net and run
`jls -export clk.net -si clk.kicad_dru design.jls`. In KiCad they route the trace
at 75 mm; `kicad-cli pcb drc --severity-error --exit-code-violations
board.kicad_pcb` reports a length violation naming CLK. They shorten the route
and DRC returns 0.

CHECK: six named tests.

- `ElectricalLengthLintTest` - over a table of (edge rate, declared length,
  propagation velocity) the verdict and the computed critical length are exact to
  the stated formula `l_crit = v*t_r/6`, the strictness constant is a declared
  parameter rather than a literal, and a net missing either declared attribute
  reports **"not assessable"**, never "PASS". A family whose check is vacuous must
  say so, in the CAP-04 fan-out idiom.
- `ReflectionGoldenTest` - the far-end waveform for the four canonical
  terminations (unterminated, series, parallel, and a mismatched load) is
  byte-identical to a golden **and** agrees with the closed-form lattice solution
  to 1e-12 relative. The golden is on the waveform; the analytic agreement is a
  separate assertion, so a golden regenerated for the wrong reason still fails.
- `EdgeRateCollapseTest` - the same circuit at t_r = 50 ps, 1 ns and 5 ns gives
  peaks of 166.7%, 132.3% and under 105% of the rail. This test pins the **lesson**
  rather than a number, and it is the one that fails if the source waveform is
  ever silently idealised back to a step.
- `SiConstraintExportGoldenTest` - the emitted `.kicad_dru` is byte-identical to a
  golden; every emitted keyword is one KiCad's rule parser accepts; the constraint
  set round-trips through save and load with an additive-only diff after inserting
  one unrelated gate.
- `KicadSiDrcTest` - opt-in through `ToolLocator.findOnPath("kicad-cli")` plus
  `Assumptions.assumeTrue`, the shipped idiom, container pinned by digest: a
  committed board fixture routed 25% over the declared maximum length **must
  fail** DRC, and the same board shortened **must pass**. The failing direction is
  asserted first.
- `SiPaletteVisibilityTest` - a first-year drawing an adder sees no transmission
  line, no edge-rate field and no SI-constraint dialog; visibility is derived from
  context, so the pedagogy floor is a test rather than an intention.

FALSIFICATION GUARD: `ElectricalLengthLintTest` and `SiConstraintExportGoldenTest`
fail today for every JLS design, because no length, no edge rate, no impedance and
no signal-integrity vocabulary exists at HEAD. Verified at `529e5be`:
`src/jls/elem/WireNet.java:22-30` holds `ends`, `wires`, `bits`, `hasinput` and
`triState` and no length or impedance field; `WireNet.propagate` posts every
consumer event at `now` (`src/jls/elem/WireNet.java:505-507`), which is not a
small delay but exactly zero; `Adder.resetPropDelay` is
`propDelay = bits * defaultPropDelay` (`src/jls/elem/Adder.java:261`), a delay
with no unit and no transition time; and a case-insensitive grep for impedance,
transmission line, S-parameter, Touchstone, eye diagram, signal integrity and
crosstalk across the 57-feature planning corpus in `docs/plan/` returns **five
files, all false positives** - four saying "transimpedance amplifier" and one
saying "high-impedance".

## Demo slice

**The lint alone, 3-6 mw.** Two declared attributes - a transition time on the
delay model and an optional physical length on a net - and a design check that
computes the critical length and prints a verdict for every net that has both. No
new element type, no format version bump of its own, no palette entry, no solver,
no GUI. `jls -check design.jls` prints an electrical-length report.

It is immediately useful to two funded capstones: on CAP-04's breadboard, a 150 mm
jumper is **2.1x** critical length for a 74AC part (v = 0.7c, t_r = 2 ns,
l_crit = 70 mm) and **0.24x** for a 74LS part, which is the mechanical answer to
"it works in the simulator and fails on the breadboard"; on CAP-05, the same
verdict applies to a routed trace once its length is back-annotated. And it is not
throwaway: FEAT-059 and FEAT-060 both consume the same two attributes and neither
adds a third.

## Prerequisite features

| FEAT | title | why THIS capstone needs it | need |
|---|---|---|---|
| FEAT-058 | Edge rate, declared physical length and the electrical-length lint | the two declared physical facts that put a design in this regime, and the verdict that names it; nothing else here is assessable without them | required |
| FEAT-059 | The closed-form transmission-line element and the reflection lab | the reflection, the ring and the termination that fixes them - the demonstration the diagnosis exists to motivate | required |
| FEAT-060 | Signal-integrity constraint authorship and PCB constraint export | the fix leaves JLS as something a real tool enforces, which is the only claim here checkable outside JLS | required |
| FEAT-047 | The physical time base and the nominal real-time scalar | a 345.6 ps flight time and a 50 ps edge are meaningless against a dimensionless tick; `docs/simulation-semantics.md` section 1 states simulation time has no unit | required |
| FEAT-042 | KiCad and gEDA netlist emitters with a manufacturability gate | the constraint file rides alongside the netlist and names the same nets; a constraint on a net a board tool never heard of is inert | required |
| FEAT-004 | Shared net-partition IR with stable net naming | a constraint is attached to a net, so a net needs a name that survives save, load and export unchanged | required |
| FEAT-013 | Per-section internal versioning with must-understand semantics | the SI attribute block rides as an OPTIONAL section, so an older JLS opens an annotated circuit structurally with a clean diagnostic instead of refusing it | required |
| FEAT-014 | Stable addressing and per-view geometry in the shared model | the back-annotated routed length is a second view's datum about a first view's net, addressed the same way every other view is | required |
| FEAT-031 | The per-instance fidelity toggle and its boundary harness | the closed-form line and the future analog line are two models of one element, which is exactly the boundary this feature exists to make testable - and the closed form is an exact oracle for the numerical one on the linear-resistive subset | beneficial |
| FEAT-027 | Strength lattice, driver kinds and net kinds | a driver's output impedance is the source resistance the line needs, and it belongs on the driver rather than on the line; until it exists the line carries it as a parameter | beneficial |
| FEAT-041 | Packing, refdes, cascade and electrical loading checks | a constraint file names refdes-bearing components, and the loading checks are the DC sibling of these AC ones | beneficial |
| FEAT-045 | Host audio sink and source without a solver | the tick-resampler and the CSV writer the trace output reuses verbatim | beneficial |
| FEAT-026 | The four-state value core with a resolution fold | a level that sits between V_IL and V_IH for 691 ps is X, and the honest re-thresholding wants to say so rather than pick a side | beneficial |
| FEAT-046 | The analog solver core and its determinism gate | upgrades the line to nonlinear drivers and reactive terminations, which the closed form cannot represent | beneficial |
| FEAT-049 | Analog device models, the drawn palette and convergence hardening | a **drawn** termination resistor instead of a dialog field, once an analog palette exists | beneficial |

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | A net that stopped being a wire | **no issue** - CAP-18 was derived from a maintainer research request after the registry closed; an issue should be filed before any work here starts |
| - | FEAT-058 through FEAT-061, the entire high-frequency programme | **no issue** - the planning corpus contains no signal-integrity vocabulary at all, verified by grep at `529e5be` |
| 89 | SDF timing annotation | overlaps - SDF carries transition times as well as delays, so FEAT-058's edge rate is owed to it independently |
| 87 | Liberty cell library | overlaps - `docs/capability-roadmap/sweep-02-timing.md:110` already states the gap verbatim: "JLS has one integer, no slew, no load, no fanout awareness" |
| 93 | SDC constraints | informs - a constraint object model is the same shape as an SI constraint set, and both want a time base |
| 213 | board export | informs - the `PcfEmitter`/`PinBindings` pair is the data-not-code precedent this capstone's constraint emitter copies |
| 78 | Element descriptor and registry | informs - one new element type, and a new element type costs zero format versions |
| 76 | Visual ergonomics and platform integration | informs - a waveform trace with a rail-overshoot marker inherits every requirement in that issue |

## Open decisions

1. **Is the transmission line a closed-form digital element or an analog device?**
   Recommendation: **closed-form `LogicElement` first, analog second, one element
   type with two `impl` bindings** under D4 and FEAT-031. Reason: computed and
   verified two ways, the exact solution for a lossless line with resistive
   terminations driven by a piecewise-linear source is a geometric superposition of
   delayed scaled copies of the source - peak 5.5000 V, matching the hand lattice
   to the printed digit, with 52 series terms at 1e-9 tolerance for the worst
   teaching case and 1 term for a terminated one. It needs no MNA, no Newton and no
   timestep control. Routing it through the analog engine instead puts the best
   teaching artifact in this regime behind analog stage S5 - 11-16 mw and that
   programme's own named stall point - raising its price from 2-3.5 mw to
   28-41.5 mw for a lesson the closed form teaches exactly.
2. **Where does physical length live?** Recommendation: a **declared** per-net
   attribute plus a per-element parameter on the line, **never derived from drawn
   pixel length**, with back-annotation from the routed board as the way the real
   number arrives. Reason: at 1 mm per grid square the shortest drawable wire is
   already 0.133 lambda at 20 GHz, and a scale small enough to stay lumped makes
   the 1000-square canvas a half-metre board. A schematic is symbolic; a to-scale
   schematic is a layout. The optional-declared route also degrades correctly - an
   undeclared net reports "not assessable" rather than a fabricated verdict.
3. **Does the driver own its output impedance and edge rate, or does the line?**
   Recommendation: **the line owns both as parameters in this capstone, and they
   move to the driver when FEAT-027 lands**, with the line's parameter becoming a
   defaulted override. Reason: `Output` has no strength or impedance today and
   acquiring one is P1's strength-lattice work; blocking a 2-3.5 mw element behind
   a 6-9 mw dependency for an attribute the line can carry meanwhile is the wrong
   trade. The migration is a defaulting rule, not a redesign.
4. **Is the impedance target emitted as a checkable constraint or as an
   annotation?** Recommendation: **as an annotation plus a resolved track width on
   the netclass, and say so in the documentation.** Reason: verified first-hand -
   KiCad's `DRC_CONSTRAINT_T` carries `LENGTH_CONSTRAINT`, `SKEW_CONSTRAINT`,
   `NET_CHAIN_STUB_LENGTH_CONSTRAINT`, `NET_CHAIN_RETURN_PATH_CONSTRAINT`,
   `DIFF_PAIR_GAP_CONSTRAINT`, `MAX_UNCOUPLED_CONSTRAINT` and
   `DIFF_PAIR_INTRA_SKEW_CONSTRAINT`, and `NETCLASS` carries `m_TrackWidth`,
   `m_diffPairWidth`, `m_diffPairGap`, `m_diffPairViaGap` and `m_tuningProfile` -
   and **no impedance field anywhere**. Claiming JLS exports a controlled-impedance
   constraint would be a conformance claim no tool checks.
5. **Does this capstone own the eye diagram?** Recommendation: **no.** Reason:
   22-38 mw cumulative, larger than this whole capstone; it shares only FEAT-047
   with this one; its acceptance test ("statistical eye height and width within 5%
   of a bit-by-bit run on an LTI fixture") cannot be run by any machinery here; and
   keeping them separate means a stall in either strands nothing in the other.
   FEAT-061 is reserved for it.
6. **What order do the three rungs ship in?** Recommendation: **lint, then
   constraint export, then the element** - which is not the order of increasing
   cost and is deliberate. Reason: the element is the cheapest deliverable and the
   only one that commits permanent public surface (a frozen tag that is a hard
   error in older readers, a mandatory palette entry under a green totality test,
   and a K9 obligation). Sequencing by permanence keeps the stop-clean line after
   the reversible work rather than before it.

## Kill criteria

- **K18-1 (correctness, before the drawn form).** If the four-termination corpus
  does not agree with the closed-form lattice solution to 1e-12 relative, the
  superposition is implemented wrong and no tuning fixes it. Stop before the
  dialog, the renderer and the palette entry.
- **K18-2 (the external checker).** If `kicad-cli pcb drc` cannot be made to honour
  an emitted rule file against a digest-pinned container, demote the tool-side
  check to advisory, narrow the claim from "a constraint a real tool enforces" to
  "a constraint file in a documented format", and re-cost FEAT-060 downward - the
  back-annotation is then the only part still carrying value.
- **K18-3 (lint noise).** If the lint produces a verdict other than "not
  assessable" on any circuit in the shipped `examples/` corpus with default
  attributes, the default is wrong. A lint that fires on designs that never opted
  in is a lint students learn to ignore, and that is a pedagogy failure in a new
  costume.
- **K18-4 (pedagogy floor, mechanism).** If the transmission line cannot be kept
  out of the default palette by a context-derived visibility rule, stop at the
  headless CSV form. The lesson survives a missing palette entry; the pedagogy
  floor does not survive an element a first-year meets while drawing an adder.
- **K18-5 (format surface).** If the SI attribute block cannot be made an OPTIONAL
  per-section-versioned section that an older reader skips with a clean diagnostic,
  do not put it in the file. Carry it in a sidecar. Every earlier JLS hard-refusing
  every annotated circuit is a worse outcome than a second file.
- **K18-6 (rate calibration).** If the demo slice - two attributes, one lint,
  no element and no GUI - exceeds 12 maintainer-weeks, the delivery rate every
  figure in this capstone divides by is at least 2x wrong. Stop, re-cost the whole
  programme, and do not start FEAT-060.
- **K9 (pedagogy floor, outranks all of the above).** Any regression to startup
  time, per-edit cost or default palette size for a first-year drawing an adder
  stops the responsible task regardless of what it costs this capstone.

## Evidence

- **The lumped threshold, computed.** At 20 GHz in FR-4 stripline (eps_r 4.3,
  v = 1.4457e8 m/s), lambda = 7.229 mm, so lambda/10 = 0.723 mm - shorter than a
  single 0402 passive. In microstrip at eps_eff 3.0, v = 1.7309e8 m/s and
  lambda = 8.654 mm. Loss over 10 inches of FR-4 at 20 GHz is 31.4 dB, i.e. 2.71%
  of drive amplitude survives, which is why equalisation exists.
- **The rule that actually applies is time-domain and keyed to edge rate:**
  `l_crit = v*t_r/6`, equivalently lambda/10 at Johnson's knee `f_knee = 0.5/t_r`
  (Johnson & Graham, *High-Speed Digital Design*, Prentice Hall 1993). Computed:
  t_r = 20 ps gives f_knee = 25 GHz and l_crit = 0.48 mm; t_r = 2 ns (74AC) gives
  48.2 mm on FR-4 and **70 mm on a breadboard**; t_r = 18 ns (74LS) gives 434 mm
  and 630 mm. A 150 mm breadboard jumper is therefore 2.1x critical length for a
  74AC part and 0.24x for a 74LS part. Clock frequency does not appear in the
  derivation.
- **Every critical-length rule in circulation is one rule.** With D = v*t_r,
  lambda_knee = 2D, so lambda_knee/10 = D/5, lambda_knee/12 = D/6 (Johnson's rule)
  and lambda_knee/4 = D/2 (the round-trip rule). Verified numerically. The
  strictness constant should therefore be exposed rather than hard-coded.
- **The cardinality failure, which is why the line is an element and not a net
  kind.** A 50 mm net at 20 Gb/s has a 345.6 ps flight time against a 50 ps unit
  interval = **6.9 simultaneous distinct values**, while
  `src/jls/elem/WireNet.java:405` holds one `@Nullable BitSet`. Equipotentiality is
  `WireNet`'s definition, not a parameter of it. Measured element-registration tax:
  **66 lines across 12 files, zero format version** (`git show --stat 38a0544`).
- **The reflection lab, computed two independent ways** (a hand lattice and an
  implementation of the closed-form superposition with a finite-rise-time ramp,
  agreeing to the printed digit). 3.3 V, Z0 = 50 ohm, Td = 345.6 ps (5 cm of FR-4
  stripline): R_s = 10 ohm into an open far end gives Gamma_s = -0.6667,
  Gamma_L = +1.0000, launch 2.7500 V, far-end peak **5.5000 V = 166.7%**, ringing
  1.8333 / 4.2778 / 2.6481 / 3.1914 V. R_s = 50 ohm gives Gamma_s = 0, one series
  term, and **3.3000 V flat**. R_L = 50 ohm gives Gamma_L = 0 and 2.7500 V flat. At
  t_r = 1 ns, which exceeds 2*Td = 691 ps, the peak falls to **4.3675 V = 132.3%**,
  which is the edge-rate lesson.
- **Convergence of the series.** For passive resistive terminations
  |Gamma_s*Gamma_L| < 1, so truncation at tolerance tol needs
  `ceil(log tol / log|Gamma_s*Gamma_L|)` terms: 52 at 1e-9 for the worst teaching
  case, 1 for any terminated case. The kernel is eight lines of textbook theory and
  two-thirds of the feature's line count is diagnostics and tests, which is the
  correct ratio for a teaching element.
- **A trace row is required and is not free.** `docs/simulation-semantics.md`
  section 2 admits only a two-state `BitSet` or a null reference, so a real-valued
  row in the existing signal-trace window is a prerequisite of showing the
  reflection at all. 0.5-1 mw, and it is the same courtesy row the analog
  determination already prices.
- **The external checker exists and was read.** KiCad's DRC rule parser accepts
  `length`, `net_chain_length`, `stub_length`, `skew`, `return_path`,
  `diff_pair_gap` and `diff_pair_uncoupled`; `DRC_CONSTRAINT_T` carries the
  corresponding constraint kinds; `NETCLASS` carries `m_TrackWidth`,
  `m_diffPairWidth`, `m_diffPairGap`, `m_diffPairViaGap`, `m_tuningProfile` and
  **no impedance field**. CAP-05's acceptance test already invokes
  `kicad-cli pcb drc --severity-error --exit-code-violations`.
- **The format hazard, and the door that expires.** `docs/file-format.md:220` is
  normative - "Unknown attribute names are silently ignored" - and `:464-475`
  records the precedent that this silently drops data. A dropped lint input is
  harmless; a dropped **constraint** is a silently unmanufactured requirement.
  FEAT-047 already requires FORMAT 3 for the same reason (its acceptance criterion
  4). Shipping the SI attribute inside that bump is 1.5-3 mw; retrofitting it
  behind FEAT-013's must-understand sections later is 4-7 mw.
- **Why transient simulation is not the method for a bit-error rate, with
  arithmetic - and why this capstone therefore does not attempt one.** BER 1e-12 at
  95% confidence needs 3/BER = 3e12 bits = 150 s of simulated time at 20 Gb/s =
  7.5e13 steps at 25 samples per unit interval, which is **5.7-9.0 years** at the
  measured Java dense-LU rates, and a bathtub curve cannot extend below 1/N_bits at
  any rate. The statistical method computes the same contour in **0.02-0.24 s** and
  its cost does not depend on the BER target at all. Published commercial IBIS-AMI
  time-domain rates put 1e12 bits at 34.7 days to 1.90 years, the same order of
  magnitude, so this is a property of the method rather than a deficiency of JLS.
  **The capability is reserved to its own capstone; only the method is refused.**
  Note the honest counterpart: 1 ms of ordinary transient at a 1 fs lattice is
  27.6-55 days, which is slow and not impossible.
- **Why full 3-D field solving is refused, and it is not the compute.** A 20 GHz
  FDTD of a 50 x 50 x 1.6 mm board at 100 um cells is 4.0e6 cells, dt = 192.6 fs by
  the Courant limit, 5.2e4 steps for 10 ns, **6.2e12 flops and 0.09 GiB - about
  1.7 hours single-threaded**. The blocker is the input: an FDTD needs a meshed 3-D
  geometry with a stackup and routed copper, which JLS does not produce and should
  not become a tool that produces. openEMS is GPL-3 and therefore absorbable under
  D8, and takes its geometry from a separate library - JLS could legally absorb it
  and would have nothing to feed it. **Works instead:** read the Touchstone the
  field solver produced, and closed-form 2-D quasi-static synthesis for a drawn
  cross-section, which is 633 SLOC of BSD-3 C already written.
- **HEAD facts verified at `529e5be`:** `src/jls/elem/WireNet.java:22-30` holds
  `ends`, `wires`, `bits`, `hasinput`, `triState` - no length, no impedance, no
  delay; `:405` holds one `@Nullable BitSet`; `:505-507` posts every consumer event
  at `now`; `src/jls/JLSInfo.java:69` is `defaultTimeLimit = 100000000`, which
  becomes 100 ns at a 1 fs base; `src/jls/elem/Adder.java:33,261` are
  `defaultPropDelay = 30` and `propDelay = bits * defaultPropDelay`;
  `src/jls/elem/Element.java:17-18` permits `DisplayElement, LogicElement, Wire`;
  `test/jls/edit/PaletteContractTest.java:44-66` asserts exactly one palette entry
  per registered element type outside `{SubCircuit, WireEnd, TestGen}`, with 35
  registry types against 32 palette entries; `docs/simulation-semantics.md`
  sections 1, 2 and 9 pin the dimensionless time base, the two-states-plus-null
  value domain and first-active-driver-in-net-order resolution.
- **Licensing under D8.** Nothing required here needs absorbing anything; the
  closed-form kernel is eight lines of textbook transmission-line theory. The
  optional upgrades are absorbable and every licence was read first-hand: ngspice
  `TRA` (Modified BSD, 447 C SLOC), Qucs-S microstrip synthesis (BSD-3, 633 SLOC),
  scikit-rf (BSD-3), PyBERT (BSD-3), KiCad KIBIS (BSD-3, 6,784 lines). Every one is
  compatible with GPL-3.0-or-later and each must carry its attribution and licence
  notice. BSD clause 3 forbids using the upstream names to endorse the derived
  work, so JLS describes itself as reading these formats, never as "powered by"
  any of them.
- **Cost reconciliation.** Marginal band 11-19 mw; cumulative including every
  prerequisite 15-25.5 mw. Its 8 required features sum to 24.5-42 mw and its 7
  beneficial features are additional. The marginal band is smaller than the
  required set because most of those features are shared spine, booked once against
  whichever capstone funds them first. "Marginal" here means the incremental cost
  given the spine is funded; the standalone figure in the header is the other end
  of that range. The required sum is printed rather than reconciled away. Every
  figure is proportional to the repository's ~200-250 shipped-and-tested lines per
  maintainer-week at the 93.0/92.0/84.5 JaCoCo aggregate plus the 80/82 PIT bar - a
  rate `11-analog-determination.md` section 8.2 names the weakest number in the
  corpus, which is why K18-6 exists.

---

### 7.1 The four new features

| id | title | cost | owner | required by | beneficial to |
|---|---|---:|---|---|---|
| **FEAT-058** | Edge rate, declared physical length and the electrical-length lint | 3-6 mw | **P4** (timing and analysis) | CAP-18 | CAP-04, CAP-05, CAP-07 |
| **FEAT-059** | The closed-form transmission-line element and the reflection lab | 2-3.5 mw (+0.5-1 for the real-valued trace row) | UNOWNED | CAP-18 | CAP-04 |
| **FEAT-060** | Signal-integrity constraint authorship and PCB constraint export | 5.5-9.5 mw | **P3** (interchange) | CAP-18 | CAP-05, CAP-13 |
| **FEAT-061** | Touchstone reader, channel characterisation and the statistical eye | 22-38 mw cumulative | UNOWNED | *(reserved for the eye/BER capstone)* | CAP-18 |

**FEAT-058 has a real owner, and that matters for the price.** P4 already owes the
transition time and does not know it: `docs/capability-roadmap/README.md` §P4
specifies a `DelayModel` keyed by (input pin, output pin) with rise/fall delays
and a min:typ:max triple — **delays, no transition times** — while
`sweep-02-timing.md:110` states the gap verbatim for Liberty: *"JLS has one
integer, no slew, no load, no fanout awareness."* **Slew is the edge rate.** So
FEAT-058's expensive half is already owed by SDF #89, Liberty #87 and SDC #93, and
this capstone adds the length attribute, the lint and its report. **That is why
the demo slice is 3-6 mw and not 8-9.**

**FEAT-060 is the only feature here with an external adjudicator.** Its acceptance
is not *"JLS emits a file"* but *"a tool JLS does not control fails a board it
should fail and passes one it should pass."* That is a stronger bar than any other
feature in this capstone and it is the reason to build it.

**FEAT-061 is reserved, not scheduled.** It exists so the eye/BER capstone can be
commissioned later without a registry collision.

**Registry deltas:** 19 capstones (CAP-00..CAP-18), 61 features
(FEAT-001..FEAT-061), 112 tasks unchanged. Priority 19 means *"appended, not yet
ranked"*, following CAP-17's recorded precedent. **The recommendation is to rank
the demo slice early** — it serves CAP-04 and CAP-05 immediately at zero palette,
zero format version of its own and zero element type — **and to leave the rest
unranked until the lint's noise rate on `examples/` is measured (K18-3).**

---

## 8. WHAT IS REFUSED

Each with arithmetic and with the approach that works named and priced in the same
entry (D10 rule 6).

### 8.1 REFUSED: brute-force transient simulation to a BER target

**Arithmetic.** BER 1e-12 at 95% confidence is `3/BER` = 3e12 bits = 150 s of
simulated time at 20 Gb/s = 7.5e13 steps at 25 samples/UI = **5.7-9.0 years**
single-threaded at the measured 2.388-3.765 µs per timepoint; ×2.0-2.5 with a
nonlinear driver. BER 1e-18 is 5.7e6-9.0e6 years. **And the bathtub cannot extend
below `1/N_bits` at any rate**, so no amount of compute lets a 1e6-bit run report
below BER 1e-6. Cross-checked against commercial IBIS-AMI time-domain rates
(*"millions of bits per minute"* → 34.7 days to 1.90 years for 1e12 bits) and
against the industry's own stated ceiling of BER 1e-6 to 1e-8 for time-domain AMI.
The cost is `O(1/BER)`; no cluster, no C rewrite and no JIT closes an asymptotic
gap.

**WORKS INSTEAD:** statistical / peak-distortion channel analysis (Casper, Haycock
& Mooney, IEEE Symp. VLSI Circuits 2002; normatively IEEE 802.3 Annex 93A COM) —
**0.02-0.24 s to any BER, flat in the target** — hybridised with SHORT bit-by-bit
transient (1e4-1e6 bits = 0.03 s to minutes) for DFE, CDR, AGC and nonlinear
drivers, carried by D-A8's existing A-STEP boundary. **H6, 9-16 mw.**
**This refuses a METHOD, not a CAPABILITY.**

### 8.2 REFUSED: femtosecond timestepping as a goal — and the honest counterpart

**Arithmetic.** 1 fs is 50,000 points per 20 GHz period, **2,500× finer than the
10-20 points/period convention**, against a second-order integrator. 1e12 steps
over 1 ms is 27.6-55 days measured; over 1 s it is 75-150 years.

**But 1e12 steps is NOT impossible, and this determination will not say it is.**
27.6 days is useless for a lab and perfectly achievable for a machine. **WORKS
INSTEAD:** 1.56-2.5 ps (25-32 samples/UI), which is what channel simulators
actually use — 1 µs of a 20 GHz link is 2.2 s and 1 ms is 36.7 minutes. Note also
that an LTE controller pins the step there anyway: a 0.1 pF pad against a 5 Ω
on-die driver is τ = 0.5 ps, so the choice is not free.

### 8.3 REFUSED: extending `WireNet` into a distributed net KIND

**Arithmetic.** A net holds ONE value; a 50 mm net at 20 Gb/s holds 6.9. The
change lands on the 531-line class carrying #98's insertion-order multi-driver
determinism, touches four `SimpleEditor` connect sites plus
`recheck`/`setBits`/`absorb`/`getValue`/`propagate`, and needs **FORMAT 3 for every
file containing a long wire**.

**WORKS INSTEAD:** a `TLine`/`Channel` **ELEMENT between two ordinary nets** —
measured **66 lines of registration tax across 12 files**, zero format version,
zero change to `WireNet.propagate`. **Saves 4-6 mw and a format version**, and it
matches SPICE `T`/`O`/`U`/`LTRA`, IBIS, Touchstone and every commercial SI tool.

### 8.4 REFUSED: `lengthbase`, i.e. making drawn pixel length physical

**This supersedes Angle 1's HF-1 as specified.** **Arithmetic.** At 1 mm/grid
square the shortest drawable wire is 0.133 λ at 20 GHz — already electrically
long; at 0.1 in/square it is 0.339 λ. A lumped-legal scale (≤0.5 mm/square) makes
the 1000-square canvas a half-metre board. And schematic wire length is unrelated
to routed trace length at any scale.

**WORKS INSTEAD:** three layers — a length parameter on the drawn element (L1,
zero format version), an OPTIONAL DECLARED per-net length (L2, inside FEAT-047's
bump), and back-annotation from the routed board (L3, with CAP-05). **L2 + the
lint = 1.5-3 mw**, and an undeclared net reports *"not assessable"*.

### 8.5 REFUSED: reaching 20 GHz by enriching the digital value domain

**Arithmetic.** Two independent failures: continuity (at 224G PAM4 the signal is in
transition 112% of the unit interval and never reaches a level; and a 10-inch FR-4
channel delivers 2.71% of drive amplitude at 20 GHz) and cardinality (PAM4's four
ordered levels are not P1's `{0,1,X,Z}`, which are don't-know and not-driven).

**WORKS INSTEAD:** the analog boundary D-A2/D-A4 already specifies, exporting
STATISTICS across it. A PAM4 slicer is three drawn `Adc`s at three thresholds plus
a thermometer decoder — exactly what D-A7 mandates. **~0 marginal core.**
**Consequence: P1 and MVL are ORTHOGONAL to this regime, not prerequisites.**

### 8.6 REFUSED: IBIS-AMI executable models

**Evidence.** `[Algorithmic_Model]` names a platform-specific `.dll`/`.so` with a
`Platform_Compiler_Bits` declaration where "platform" is OS **and** address bit
size. This is structurally identical to the OSDI case `11-analog-determination.md`
§1.5 already refused (`osdi/osdiregistry.c:31` = `dlopen(path,
RTLD_NOW|RTLD_LOCAL)`, `:39` = `LoadLibrary(path)`). HEAD has **zero**
`ProcessBuilder`/`System.loadLibrary`/`java.lang.foreign` in `src/`. It breaks the
single offline jar and the byte-golden gate in one move, and a `.jls` file that
causes a vendor binary to execute inverts #38's premise that a `.jls` file is DATA.

**WORKS INSTEAD, three routes:** (1) a **native EQ vocabulary declared as DATA** —
FFE tap weights, CTLE pole/zero/DC gain, DFE taps, CDR bandwidth — matching AMI's
parameter semantics without its binary ABI, **~1-2 mw inside H6**; (2) read the
`.ami` parameter file, which **is TEXT and IS readable**, so JLS interoperates with
AMI parameter sets; (3) support the spec's compile-free AMI DATA files, which are
platform-independent by definition. **Honest loss to be written into the capstone
text rather than discovered: JLS will not run a vendor's proprietary AMI binary.**
Under D9/K9 the native route is pedagogically superior — the student draws the
equaliser and sees why moving a tap opens the eye, which no educational tool
offers today. Same shape as §3.4's Verilog-A Door 3 is to OSDI. Reference:
PyBERT (BSD-3) implements exactly this stack natively.

### 8.7 REFUSED: 3-D full-wave electromagnetic field solving

**And NOT on licence, and NOT on compute — both were checked.** openEMS is
GPL-3.0-or-later (COPYING read), **GPL-compatible and genuinely absorbable under
D8**. A 20 GHz FDTD of a 50 × 50 × 1.6 mm board at 100 µm cells is 4.00e6 cells,
Courant `dt` = 192.6 fs, 5.19e4 steps for 10 ns = **6.23e12 flops and 0.09 GiB ≈
1.7 hours single-threaded**; at 50 µm cells 9.97e13 flops and 27.7 hours. **The
compute is affordable.**

**The blocker is the INPUT.** An FDTD needs a meshed 3-D geometry with a stackup
and routed copper. JLS produces none of it — `grep -rniE "footprint|refdes|pinout"
src/` returns zero, which is CAP-05's own falsification guard — and openEMS itself
takes its geometry from a separate library. **JLS could legally absorb openEMS and
would have nothing to feed it.**

**WORKS INSTEAD:** **read the S-parameters somebody else's field solver produced**
(H5, 4-7 mw — that is what Touchstone is for and what the industry does at this
layer boundary), plus closed-form 2-D quasi-static synthesis for a drawn
cross-section (H7, 1.5-3 mw, absorbing 633 SLOC of BSD-3 C, 1-2% accurate over the
standard geometry range every teaching board uses). The 2-D BEM/MoM field solver
stays as a **priced re-entry door at 10-18 mw** when a non-standard cross-section
is actually needed.

### 8.8 REFUSED: an interactive in-JLS eye-diagram canvas in v1

**Arithmetic.** `11-analog-determination.md` §2.9's measured 13× GUI tax applies: a
minimum canvas is 850-1,350 executable lines against a total commons of **2,897
addable-uncovered LINE** for all future untested code in the project, i.e. 29-47%
of the commons for one view, plus a permanent K9 obligation, at 6-12 mw.

**WORKS INSTEAD:** emit the eye density matrix, bathtub table, TIE record and
jitter decomposition as **goldened CSV** (zero GUI, zero draw on the commons, and
it is what lets CAP-06 autograde an eye), with an optional static PNG/SVG writer
at ~250 lines / 1-1.5 mw.

> **This is an AMENDMENT, not an endorsement, of §2.10's "GTKWave is the analog
> view, zero JLS GUI code" decision. GTKWave draws no eye diagram and no bathtub
> curve.** That decision **holds for waveforms and breaks for eyes**, and the
> amendment must be recorded rather than discovered. The same applies to the
> reflection lab: a real-valued row in the *existing* trace window is required
> (§7 evidence), because `simulation-semantics.md` §2 admits only a two-state
> `BitSet` or null.

### 8.9 REFUSED: a lumped RLGC ladder as the transmission-line model

**Arithmetic, measured first-hand on ngspice-42** (5 cm FR-4 line, T_d = 345.6 ps,
Z₀ = 50 Ω into an open, 25 ps edges, knee 14 GHz): the exact `T` device gives a
peak of 0.99995 V (−0.005% error on the open-circuit doubling), while LC ladders
give **+19.60% / +16.23% / +10.18% / +5.60% / +4.47%** spurious ringing overshoot
at N = 25 / 55 / 110 / 220 / 440 sections — at **~24× / ~112× the analysis time**.
The error converges only as `1/N`, so 440 sections still show a 4.5% overshoot a
student would read as a real signal-integrity problem that does not exist.
Independently: a natural 1 mm-per-section ladder is **3.6% slow**, which over a
667 ps 100 mm line is **24 ps = half a unit interval** at 20 Gb/s; 0.1% accuracy
needs 541 sections = 1,083 unknowns.

**WORKS INSTEAD:** the method of characteristics / Branin delay two-port —
**2 extra unknowns, exact delay, zero dispersion error, and no companion model and
therefore no trapezoidal companion-model ringing.** A ~270× reduction in matrix
size that simultaneously removes the dominant error term. **AND KEPT:** the ladder
stays correct and free where the structure is electrically short
(`t_flight ≲ t_r/6`), which is what URC's own lump-count formula encodes and what
already covers CAP-04's measured 2.0 ns round trip against 5-10 ns 74LS edges. The
two are not competing designs; they are the two sides of one threshold, computable
at elaboration.

### 8.10 REFUSED: ngspice as the CI tolerance oracle for high-frequency channels

**Evidence.** Published on the ngspice user list and in the LTspice wiki: *"None of
the ngspice transmission line models support frequency dependent parameters. More
specifically, you can't directly model dielectrical loss which requires frequency
proportional G nor skin effect loss which requires sqrt(frequency) proportional
R"*; and *"The RLGC and LTRA models are defined at only one frequency ... hence
these models are not causal except at low frequencies."* Meanwhile a 200 µm copper
microstrip's series resistance at 20 GHz is **~150× its DC value**. **There is
nothing above ~1 GHz to promote** under `11-analog-determination.md` §4.2 / S8's
per-family protocol, which lands directly on D-A15's entire validation story for
this one device class.

**WORKS INSTEAD, three ways, all cheaper than the missing oracle:**
(a) **closed-form analytic channels** — a uniform lossy line has the exact
frequency-domain transfer function `e^(−γl)` with `γ = √((R+jωL)(G+jωC))`, giving a
Tier-A fixture with a **derived** tolerance at 20 GHz where ngspice has none
(~1 mw); (b) **scikit-rf as a reference implementation** for the network algebra,
absorbed under D8 and checked against hand-derived cases (0 mw beyond H5);
(c) **reciprocity, passivity (‖S‖₂ ≤ 1), causality (Kramers-Kronig / Hilbert
consistency) and Tellegen power balance as Tier-C invariants — STRONGER than a
tolerance oracle because they are exact** (~1 mw). **S8's oracle design must be
amended before it is planned against.**

### 8.11 REFUSED: absorbing ngspice's `TXL`, `CPL` or `LTRA`

**Evidence, read first-hand.** `TXL` and `CPL` quantise time to **integer
picoseconds in an `int`** (`txlload.c:55,58`; `cplload.c:64,67`), then compute
`delta = time − before` (`txlload.c:90`) and **divide by it** (`:96, :101`) — one
rounding from a divide-by-zero at the sub-picosecond steps this regime requires,
and overflow at 2.147 ms of simulated time. Both register `.DEVaccept = NULL`
(`txlinit.c:49`, `cplinit.c:44`) so history is updated per Newton iteration rather
than per accepted timepoint, and `CPL` also `.DEVtrunc = NULL` (`cplinit.c:41`) so
it cannot cap the step to resolve its own delay. **ngspice's own `DEVICES` file
disowns them:** §3.1 *"This model comes from swec and kspice. It is not
documented... Probably a lot of memory leaks."* §3.4 *"There is some code left out
from compilation: TXLaccept and TXLfindBr. Any ideas?"* Separately, `LTRA` convolves
the **entire** accepted-timepoint history three times per line per load
(`ltraload.c:609,649,687`) — about 6e9 flops per line on a 20,000-timepoint run,
quadratic thereafter — and declares only constant `r/l/g/c` (`ltra.c:32-35`).

**And a licence landmine `11-analog-determination.md` §7's table did not catch:**
`ltramisc.c:196-199` carries the verbatim comment *"These are from the book
Numerical Recipes in C"* immediately above `bessI0`/`bessI1`/`bessI1xOverX`, inside
a file `COPYING` blanket-declares Modified BSD. **NR code is copyrighted and its
licence does not permit redistribution in a derived work, so `COPYING` is not
authoritative for those three functions**, and §7's *"no GPL-incompatible code
anywhere in the tree"* needs this footnote.

**WORKS INSTEAD:** modal decomposition onto ideal `T` lines for constant-RLGC
coupling (1.5-2.5 mw for the symmetric pair — and note ngspice's own `cpline` code
model is parameterised exactly this way, `ze/zo/ere/ero/ae/ao`); a fitted 2N-port
for frequency-dependent coupling; and delay extraction + pole-residue **recursive
convolution** in place of full convolution — measured 0.190 µs/step for 60 poles
against ~6e9 flops for the equivalent full convolution, about **1,600×**, and it is
the same kernel the S-parameter path needs, so it is one mechanism for two
capabilities. If Bessel I0/I1 are ever needed, take them from Bessels.jl (MIT,
licence verified) or implement fresh from Abramowitz & Stegun §9.8 (a US Government
work, public domain).

### 8.12 REFUSED: mm-wave RF circuit design

Matching networks, LNAs, mixers, antennas; 5G FR2 24-40 GHz, 60 GHz WiGig, 77 GHz
automotive radar. These are above 20 GHz but they are a different curriculum, a
different artifact and a different tool class (ADS, Microwave Office, Qucs-S).

**WORKS INSTEAD: nothing — this is genuinely outside JLS's trajectory.** D9's
CS→ECE→EE arc ends at the digital/signal-integrity boundary, not at microwave
engineering. **The action is to NAME that boundary once, explicitly, in
`ARCHITECTURE.md` with a revisit trigger** (§3.6), so it is a stated scope decision
rather than a silent gap, and so CAP-18's text scopes to high-speed serial
interconnect by definition rather than by cost argument later. **~0.1 mw.**

### 8.13 REFUSED as a framing: "make the digital engine run at 20 GHz"

**Measured:** discrete-event cost depends only on cycles simulated and is identical
at 1 Hz and 20 GHz (2.46 s per 1 µs of a 20 GHz machine). And no mainstream digital
clock runs at 20 GHz (top shipping 6.0-6.2 GHz; the 9.12 GHz record is LN2).
**That scoping targets a workload with no instances.**

**WORKS INSTEAD:** D-H1's scope — a high-speed serial link and its channel, and the
two questions *does the eye open* and *at what BER*.

### 8.14 REOPENED AND SUPERSEDED, not refused: the in-tree Touchstone / IBIS-analog position

Two prior in-tree positions are reopened under **D10 rule 4** (*"Inherited
AI-authored positions are INPUT, not authority"*). Both have verifiable defects,
which is why this is a correction rather than a disagreement.

**(1)** `docs/standards-landscape.md:338` — *"Every row is **OTHER**, and that is
the correct and **permanent** answer"* — covers **#112 IBIS** and **#113
Touchstone**, filed in *"Tier 7 — Physical implementation and layout data"* beside
GDSII, OASIS, MEBES and foundry DRC decks. Three defects: **Touchstone is a
measurement/behavioural interchange format and IBIS is a behavioural model
format**, neither is layout data; the tier's verdict is **already contradicted by a
binding decision**, since D8's re-sort grades *"GDSII / OASIS reader —
**Reimplement** — open spec, and JLS is already a 2D geometry engine"*, a row in the
same tier; and **"permanent" is precisely the phrasing D10 forbids.**

**(2)** `docs/capability-roadmap/sweep-06-physical-boundary.md:553-556` — *"#113
Touchstone, and the analog half of #112 IBIS. S-parameters and I/V/t buffer curves
require a continuous-time solver. Adding one is building SPICE, which is ground
(a)."* **The premise is dead:** the maintainer has decided to build the solver
(*"We keep skirting NGSPICE support. Just integrate it fully as a feature to feed
capstones"*). **And the refusal was doubly wrong for Touchstone specifically:
READING S-parameters does not require a continuous-time solver at all** (§6.1), and
a vector-fitted S-matrix lowers to `V R C L G F E` only — every one already Tier 0
of D-A13 — so it needs **zero new device models** even on the solver path.

**ACTION (~0.1 mw of document surgery):** reclassify #112 and #113 out of Tier 7,
strike "permanent", restate sweep-06's clause as **superseded**, and record the
re-derived costs: **Touchstone reader 4-7 mw** (H5), **the IBIS analog half 14-24
mw** (H8, absorbing KiCad's KIBIS — 6,784 BSD-3 lines, verified: *"Copyright (C)
2022 Fabien Corona ... Redistribution and use in source and binary forms"*,
deliberately not KiCad's GPL-3 because *"GPL is not acceptable because it is
incompatible with ngspice integration"*). Both absorbable under D8 with attribution.

**And record the reason the original ground was correct when written**, so this is
a supersession rather than a reversal of a mistake: it was written before D8
revoked the orchestrate-never-reimplement stance and before the maintainer
committed to the solver.

---

## 9. CORRECTIONS OWED TO THE CORPUS

These are amendments to documents already written, not new work. **They are cheap
now and expensive to discover later**, and several would silently mis-plan a stage.

| # | document | correction | mw |
|---|---|---|---:|
| **C-1** | `11-analog-determination.md` §2.3 | The *"Both wires have inputs"* multi-driver refusal is cited at **five** sites (`SimpleEditor.java:4020,4053,4147,4180,4253`). At HEAD `529e5be` it occurs at exactly **TWO**: `:4023` and `:4150`. The **width** check's four sites ARE confirmed exactly as cited (`:4015, :4142, :4247, :4358`), including the guard at `:4014` that D-A4 correctly calls the single most important line in the design. No conclusion changes; anyone implementing D-A4 should budget for two multi-driver sites, not five. | 0 |
| **C-2** | `11-analog-determination.md` §3.3 | The transmission-line re-entry price *"lossless T — 1.5-3.0 mw when the PCB capstone asks"* is **right for a lossless teaching element and wrong for this regime by 4-6×**. A lossless T has no `α_c ~ √f`, no `α_d ~ f`, no roughness and no `ε_r(f)`, and is non-causal above low frequency. **KEEP the 1.5-3.0 mw figure as a separate first-year reflection-and-termination element below ~1 GHz** — it is a different capability, not a cheap version of this one — and price a line that reaches 20 GHz at 10-17 mw. | 0 |
| **C-3** | `11-analog-determination.md` §3.3 | The ban on *"bypass, adaptive matrix reuse, symbolic-factorisation reuse ... any parallelism"* would, **as phrased**, forbid recursive convolution, which carries per-pole history state. Its INTENT is *"no state whose update is CONDITIONED ON A TOLERANCE COMPARISON"*. **Re-phrase it before any channel work collides with it** — a recursive-convolution recurrence is a fixed arithmetic update with no data-dependent branch and is byte-deterministic by the same argument as the trapezoidal integrator. | 0 |
| **C-4** | `11-analog-determination.md` §4.2 / S8 | The ngspice oracle has **no oracle above ~1 GHz** for transmission lines (§8.10). Amend the per-family promotion protocol with the three named replacements. | ~1 |
| **C-5** | `11-analog-determination.md` §2.10 | *"GTKWave is the analog view, zero JLS GUI code"* **holds for waveforms and breaks for eyes and for a reflection trace.** Record the exception and the two costs (goldened CSV at ~0; a static SVG/PNG writer at 1-1.5 mw; a real-valued trace row at 0.5-1 mw). | 0 |
| **C-6** | `11-analog-determination.md` §5 / S8 | The Tier B torture corpus lists *"zero-valued R/L/C"* but not **negative-valued R** or **1 F / 1 H idealisations**, both of which a vector-fitted channel emits **by construction** (verified in `skrf/vectorFitting.py:2463-2665`: `Rp{k} 0 x {-1/pole_re}`, `Cx{k} x 0 1.0`, `Le{i} e{i} 0 1.0`). Add them. | ~0.25 |
| **C-7** | `spice-numerics.md` §4.2, quoted in `11-analog-determination.md` §4.2 | **The reported dense LU figure of 9.2242 ms at N=1600 cannot be a dense-LU measurement.** A full dense LU is `2N³/3` = 2.731e9 flops, implying **296 Gflop/s** — measured first-hand on a genuinely dense random matrix with partial pivoting, this machine does **3.0-6.5 Gflop/s** (N=1600 → **917.2 ms**). The mechanism is benign: a zero-multiplier guard on a tridiagonal matrix in natural order creates zero fill (**the corpus's own table records "fill-in created: 0" in every row**), so the code has dense STORAGE and sparse WORK. **The staging conclusion — defer sparse LU — remains correct for every listed capstone at N ≤ 50, but the evidence does not extend to HF sizes**, where the honest dense-vs-structured gap is 94-444×. | 0 |
| **C-8** | `11-analog-determination.md` §3.3 | The sparse-LU deferral trigger (*"a circuit over ~2,000 unknowns"*, justified by *"every capstone circuit measured is 7-28 equations"*) **would fire at the first vector-fitted 2-port** (202 unknowns; 324 for a 4-port at 80 poles; 968 for an 8-port). **But the fix is BETTER than general sparse LU:** JLS synthesises the subcircuit and therefore knows which rows are state rows, so a **declared-structure block elimination** (or a banded solver on a fixed elaboration-time ordering) has **no pivot-tie-break problem** and does not drag in §8.7's *"largest un-derisked determinism item"*. Measured: banded 18.9× at N=201 and 111× at N=501 at **1.5-2.5 mw**, versus general Markowitz at 4-6 mw plus the determinism risk. **General sparse LU and its tie-break experiment STAY deferred.** This improves the analog programme independently of 20 GHz. | 0 |
| **C-9** | `11-analog-determination.md` §8.7 | The `StrictMath` kill criterion should be **re-scoped to the diode/BJT `load()` loop where it was measured.** Over the argument range a recursive-convolution or spectral kernel visits, `StrictMath.exp` is **1.18× FASTER** than `Math.exp` and `StrictMath.cos` **1.91× faster**. D-1 costs this regime nothing. | 0 |
| **C-10** | `docs/standards-landscape.md:338`, `sweep-06-physical-boundary.md:553-556` | Reopened and superseded (§8.14). | ~0.1 |
| **C-11** | `BRIEF.md` §7 / corpus-wide | The planning corpus is **18 capstones, 57 features, 112 tasks**, not 17/53/112. Every future document must cite the current counts. | 0 |

**Total: ~1.35 mw of corrections, almost all of it documentation.** C-3, C-7 and
C-8 are the three that would otherwise cause a stage to be planned against a false
premise.

---

## 10. WHAT WOULD CHANGE THIS DETERMINATION

In order of value. The first is mandatory and is a stage.

1. **H1 IS THE CALIBRATION EXPERIMENT (mandatory, in the plan).** Two attributes,
   one lint, no element, no GUI, taken to the full 93.0/92.0/84.5 JaCoCo + 80/82
   PIT gate. **Every figure in this document is linearly proportional to a
   delivered-lines-per-maintainer-week rate that `11-analog-determination.md`
   §8.2 names the weakest number in the corpus and that S1 was supposed to settle.**
   H1 settles it for HF at 2-4 mw and is a deliverable regardless. **K18-6 is its
   kill criterion at 12 mw.**
2. **A ~200-line Java statistical-eye spike (one afternoon).** The 0.02-0.24 s
   figure is measured *in components* on two machines but has never been run
   end-to-end. **Even a 30× miss leaves the transient/statistical ratio at ~1e8, so
   the CONCLUSION is robust** — but the headline number is not yet a single
   measurement and this document's own standard says label which. **Cheapest
   high-value verification available.**
3. **Fit one REAL `.s4p` and count the poles (~2 days).** Every unknown-count and
   solver-sizing figure on the drawn-channel path scales with pole count, and the
   corpus used 80-160 from the literature rather than from a file. If real channels
   need 200+, the structured-solve numbers and C-8's price both move.
4. **Does `kicad-cli` honour an emitted `.kicad_dru` against a digest-pinned
   container?** The parser and the constraint kinds are verified; the CLI wiring is
   not, and **K18-2 hangs on it.** If it does not, FEAT-060 loses most of its value
   and the commercial-bridge answer moves to the eye by default rather than by
   merit.
5. **The lint's noise rate on the shipped `examples/` corpus (K18-3).** Must be
   exactly zero — every net reports *"not assessable"* unless the user opted in.
   Untested, and it is the difference between a lint students read and a lint
   students learn to ignore.
6. **Can recursive convolution be proven BYTE-deterministic?** It should be — a
   fixed arithmetic recurrence with no data-dependent branch — but it is unmeasured
   and the entire drawn-lossy-channel path turns on it. Answer it with a digest
   experiment **before** committing H9, in the spirit of analog S2's week-8
   go/no-go. Related and urgent: **do C-3 first**, or the rule as phrased forbids
   the mechanism outright.
7. **Does passivity enforcement's Hamiltonian eigen-decomposition survive the
   byte-golden gate?** It needs a deterministic Hessenberg + shifted-QR eigensolver
   (~1,000 lines) JLS does not have, and QR shift selection involves exactly the
   data-dependent comparisons D-2..D-5 were written to control. Unresolved, and it
   sits inside the largest optional item.

**Two cheap factual gaps worth closing before anything is written into
`ARCHITECTURE.md`:** the exact copyright and distribution clauses of the Touchstone
2.0 and IBIS 7.2 specification PDFs (`ibis.org` returned HTTP 403 through this
environment's proxy in every session that tried; the trademark clause constrains
how JLS may NAME the feature — *"Touchstone® is a registered trademark of Agilent
Corporation"*), and the licence of `capn-freako/PyAMI` (PyBERT and ibisami were
both confirmed BSD-3-Clause from their repository pages; PyAMI was not).

---

## 11. VERIFICATION TRANSCRIPT (this session, HEAD `529e5be`)

| Claim | Command / anchor | Result |
|---|---|---|
| HEAD | `git rev-parse --short HEAD` | **`529e5be`** (the angle documents cite `636e3e0`; every claim below was re-verified at `529e5be`) |
| `WireNet` has no length/impedance/delay | `src/jls/elem/WireNet.java:22,24,26,28,30,405,407` | `ends`, `wires`, `bits`, `hasinput`, `triState`, `value` (one `@Nullable BitSet`), `conflictReported` — **and nothing else** |
| Wire delay is exactly zero | `src/jls/elem/WireNet.java:505-507` | `sim.post(new SimEvent(now, (Reacts) element, new SimEvent.PinChanged()))` |
| `defaultTimeLimit` | `src/jls/JLSInfo.java:69` | `public static final long defaultTimeLimit = 100000000;` |
| consumed at | `src/jls/sim/Simulator.java:38` | `protected long maxTime = JLSInfo.defaultTimeLimit;` |
| Time is a dimensionless long | `src/jls/sim/Simulator.java:36` | `protected long now = 0;` |
| Adder delay is a lumped scalar | `src/jls/elem/Adder.java:33,261` | `defaultPropDelay = 30`; `propDelay = bits * defaultPropDelay` |
| `Element` permits 3 | `src/jls/elem/Element.java:17-18` | `permits DisplayElement, LogicElement, Wire` |
| Width check sites | `grep -n "Bits don't match" src/jls/edit/SimpleEditor.java` | **`:4015, :4142, :4247, :4358`** — four, exactly as `11-analog-determination.md` cites |
| The guard that makes the domain check load-bearing | `src/jls/edit/SimpleEditor.java:4014` | `if (bits1 > 0 && bits2 > 0 && bits1 != bits2)` |
| Multi-driver refusal sites | `grep -n "Both wires have inputs" src/jls/edit/SimpleEditor.java` | **`:4023, :4150`** — **TWO**, not the five cited in `11-analog-determination.md` §2.3 (**correction C-1**) |
| Element registry size | `grep -c "new ElementType(" src/jls/elem/ElementRegistry.java` | **35** |
| Palette size | `grep -c "entry(Group\." src/jls/edit/Palette.java` | **32** |
| Palette totality is a GREEN test | `test/jls/edit/PaletteContractTest.java:44-66` | `NON_PALETTE_TAGS = Set.of("SubCircuit","WireEnd","TestGen")`; asserts exactly one entry per registered type |
| Value domain is normative and two-state | `docs/simulation-semantics.md:42-66` | *"Bits are two-state: 0 or 1. There is no unknown/X state anywhere in the simulator"*; HiZ is a **null** reference |
| Time model is normative and unitless | `docs/simulation-semantics.md:24-29` | *"Simulation time is a dimensionless non-negative 64-bit integer ... Time units are abstract; nothing binds them to seconds"* |
| Default delay table | `docs/simulation-semantics.md:264-288` | NAND/NOR/NOT 5, AND/OR/XOR 10, Mux 25, Register 50, Memory 100, **Adder 30 × bits**, wires 0 |
| Multi-driver rule | `docs/simulation-semantics.md:432` | *"the first active driver in net order wins"* |
| **Zero HF vocabulary in the planning corpus** | `grep -rliE "impedance\|transmission line\|s-parameter\|touchstone\|eye diagram\|signal integrity\|crosstalk" docs/plan/` | **5 files, ALL false positives** — TASK-0103, CAP-12, FEAT-049 (*"transimpedance amplifier"*), FEAT-020, FEAT-026 (*"high-impedance"*) |
| Zero HF vocabulary in `src/` | same grep over `src/` for the HF terms | **0** |
| Zero HF vocabulary in the roadmap | `grep -rniE "characteristic impedance\|transmission line" docs/capability-roadmap/*.md` | **0** |
| Prior refusal, verbatim | `docs/capability-roadmap/sweep-06-physical-boundary.md:553-556` | *"#113 Touchstone, and the analog half of #112 IBIS. S-parameters and I/V/t buffer curves require a continuous-time solver. Adding one is building SPICE, which is ground (a)."* |
| Prior classification, verbatim | `docs/standards-landscape.md:338` | *"Every row is **OTHER**, and that is the correct and **permanent** answer."* (#112, #113 in Tier 7) |
| Liberty slew gap stated verbatim | `docs/capability-roadmap/sweep-02-timing.md:110` | *"JLS has one integer, no slew, no load, no fanout awareness"* |
| FEAT-047 grammar reaches femtoseconds | `docs/plan/features/FEAT-047-...md` AC-1 | *"a magnitude of 1, 10 or 100 and a decimal unit from seconds down to femtoseconds"* |
| FEAT-047 requires a version bump, never silent ignore | same, AC-4 | *"refused by a reader that predates the attribute ... never silently ignored"* |
| FEAT-047 consumers | same, consumed-by table | CAP-12, CAP-14, CAP-10, CAP-11 required; CAP-07 beneficial — **owed anyway** |
| Id space is closed at CAP-17 / FEAT-057 / TASK-0112 | `docs/plan/REGISTRY.md` COUNTS | *"18 capstones (CAP-00 through CAP-17, no gaps). 57 features (FEAT-001 through FEAT-057, no gaps). 112 tasks ... no gaps"* — **brief's "17/53/112" is stale (C-11)** |
| CAP-17 is isolated | FEAT-054/055/056/057 consumed-by tables | each lists **CAP-17 and no other capstone** |
| CAP-17 is unranked, appended | `docs/plan/capstones/CAP-17-...md` open decision 5 | *"Priority 18 in the header means 'appended, not yet ranked'"* |
| Capstone marginal total | summed from the 18 capstone headers | **309.5-514 mw** |
| JaCoCo floors | `pom.xml:449-471` | `jls.sim` INSTRUCTION 0.930 / LINE 0.920 / BRANCH 0.845 |
| PIT thresholds | `pom.xml:812-813` | `mutationThreshold` 80, `testStrengthThreshold` 82; floors *"only ever move UP"* |

Everything else cited above is inherited from the seven angle analyses with its own
provenance recorded there; measurements are attributed to the angle that made them,
and where angles disagreed the resolution is stated at the point of use (the
statistical-eye figure, the brute-force BER band, the transmission-line route, and
the length-attribute mechanism are the four places that happened).

---

## 12. THE DECISION LIST

| # | Decision |
|---|---|
| **D-H1** | The workload is a **high-speed serial link and its channel**, not a 20 GHz digital clock. mm-wave RF circuit design is out, named once in `ARCHITECTURE.md` with a revisit trigger. **Correction: the SerDes fundamental band is 12.5-56 GHz, not 12.5-112.** |
| **D-H2** | The regime is entered by **EDGE RATE, not clock rate**. JLS adopts `l_crit = v·t_r/6`, **exposes the strictness constant**, and documents that it is a tolerance choice. Every critical-length rule in circulation is one rule. |
| **D-H3** | A transmission line is an **ELEMENT BETWEEN TWO NETS**, never a new net kind. 66 lines, zero format version, zero change to `WireNet.propagate`. |
| **D-H4** | **LENGTH is a DECLARED optional attribute** — per net (in FEAT-047's bump), per element, and back-annotated from the routed board. **Never pixels→millimetres.** Supersedes Angle 1's HF-1. |
| **D-H5** | **The value domain does not move.** No refinement of it reaches this regime. P1/FEAT-026/FEAT-027 and MVL are **orthogonal, not prerequisites**. A PAM4 slicer is three drawn `Adc`s — D-A7 gets stronger. |
| **D-H6** | Keep **1 ps as the global default time base**. FEAT-047 gains three amendments (unit-aware `defaultTimeLimit`; a stated policy for the default delay table under a declared base; the >2^53 split promoted from asserted to documented) and D-A7 / §2.10 gain two decouplings. ~1-2 mw. |
| **D-H7** | **The event queue survives untouched and is the right mechanism.** Cost is frequency-independent. D-A8's inverted A-STEP fits 20 GHz **better** than the audio capstones it was designed for; engine footprint is the already-priced five-line `nextEventTime()`. |
| **D-H8** | Add determinism clause **D-6**: one pinned complex-arithmetic formulation; fixed-radix transforms with a `StrictMath` twiddle table and fixed butterfly order; **no auto-planning transform library, ever.** Written before the code. ~0.5 mw. |
| **D-H9** | A channel element carries its **own reference terminals** and does not share the analog region's `Ground`. The electrically-short boundary of the ideal `Ground` is documented. ~0.25 mw, and must not be skipped. |
| **D-H10** | **THE LAW.** *Anything that changes what a value MEANS goes on the PORT. Anything that changes whether a net HAS a value goes INSIDE an ELEMENT.* Two mechanisms, both already funded, jointly closed over all five regimes. High frequency is a **consumer of the fourth alphabet field plus one ordinary element**, not a fifth mechanism. UNITS is a third axis, and LENGTH is the row with no owner. |
| **D-H11** | **REFUSED: brute-force transient as the BER method** (5.7-9.0 years at BER 1e-12; the bathtub cannot go below `1/N_bits`). **WORKS INSTEAD: statistical / peak-distortion analysis**, 0.02-0.24 s, flat in BER, 9-16 mw, hybridised with short bit-by-bit runs for the non-LTI parts. **A method is refused; the capability is not.** |
| **D-H12** | **Golden HF results on eye height, eye width and margin within a stated tolerance — never on a sample, a crossing tick or the accepted grid.** |
| **D-H13** | *"JLS supports 20 GHz"* means **DECLARE → DIAGNOSE → DEMONSTRATE → DELIVER**. It does not mean computing loss from geometry and it does not mean producing a BER. |
| **D-H14** | **HF must not justify building the analog solver, and the solver must not gate HF.** Three of four products need FEAT-047 and nothing else from `jls.analog`. |
| **D-H15** | **Sequence by PERMANENCE, not cost: lint → constraint export → element → eye.** The analog determination's stop-clean rule does not transfer. |
| **D-H16** | **CAP-18 is adopted** (11-19 mw marginal, 19-34 standalone, 15-25.5 cumulative), with FEAT-058, FEAT-059, FEAT-060 minted and FEAT-061 reserved. **The named displacement is CAP-17** (38-62 mw, priority 18, isolated). The eye/BER capability is a **separate** capstone at 22-38 mw cumulative. |

---

## 13. THE BIGGEST RISK

**Not the physics, not the arithmetic, and not K9. It is that CAP-18's cheapest and
most valuable rung — the lint — never ships, because it is the only deliverable in
the whole programme with nothing to show at a demo, and the drawn element ships
first instead.**

The drawn line is 2-3.5 mw, produces a spectacular picture, and permanently commits
a frozen tag, a mandatory palette entry under a green totality test, and a K9
obligation — in a programme whose delivery rate is uncalibrated, at bus factor 1,
behind a 35-62 mw CAP-00, on a 2,897-LINE coverage commons. **If the programme
stalls after the element, JLS owns a public transmission-line element with a
half-answered story and no way to remove it.** If it stalls after the lint, JLS
owns a correct sentence about its own domain of validity and nothing else — which
is a complete, defensible, permanently-true deliverable.

**That is what D-H15 is for, and it is the one decision in this document that will
feel wrong while it is being followed.**
