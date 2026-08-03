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
| 313 | CAP-18: a drawn net that is electrically long says so, shows its reflections, and exports a constraint a real board tool enforces | **is** - this capstone, filed 2026-08-03. The "no issue" this row carried at authoring is superseded |
| 486 | FEAT-058: edge rate, declared physical length and the electrical-length lint | **is** - rung 1, filed 2026-08-03 under D14, `serves_capstones: [313]`, `blocked_by: [367, 336, 319]` |
| 487 | FEAT-060: signal-integrity constraint authorship and PCB constraint export | **is** - rung 2, filed 2026-08-03 under D14, `serves_capstones: [313]`, `blocked_by: [486, 336, 366, 318, 319]` |
| 490 | FEAT-059: the closed-form transmission-line element and the reflection lab | **is** - rung 3, filed 2026-08-03 under D14, `serves_capstones: [313]`, `blocked_by: [487, 367]`. Note the three were filed in **permanence** order, so their issue numbers do not follow their FEAT numbers |
| - | FEAT-061, the Touchstone reader / channel characterisation / statistical eye | **no issue, no document, no REGISTRY row, and no scope statement anywhere** - it is reserved in prose only, for an eye/BER capstone that has not been commissioned. That reservation protects nothing mechanical, which is why the registry count below is 60 and not 61 |
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
commissioned later without a registry collision. **Reserved in this sentence and
nowhere else: no FEAT-061 scope exists anywhere in the corpus** — no feature
document, no `REGISTRY.md` row, no filed issue, and no scope statement outside
the one table row above. A reservation held only in prose protects nothing
mechanical, which is why the count below stops at FEAT-060.

**Registry deltas (CORRECTED 2026-08-03 under D14):** 19 capstones
(CAP-00..CAP-18), **60 features (FEAT-001..FEAT-060)**, 112 tasks unchanged.
**The figure this line carried at authoring — "61 features
(FEAT-001..FEAT-061)" — was never true and is superseded rather than
reinterpreted.** The arithmetic: 57 feature documents existed at
`2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` (`git ls-tree --name-only 2d0ca9d
docs/plan/features/ | wc -l` → 57, highest FEAT-057), so the claim over-counted
by **four** when written; this pass wrote FEAT-058, FEAT-059 and FEAT-060 and
filed them, giving 57 + 3 = **60**, so it over-counts by **one** now.
**FEAT-061 was not minted**: it names no scope, and minting an id to make a
count true would be adjusting a number to fit. Priority 19 means *"appended, not
yet ranked"*, following CAP-17's recorded precedent. **The recommendation is to rank
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
