# FEAT-059 - The closed-form transmission-line element and the reflection lab

**Status:** proposed | **Cost:** 2-3.5 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A drawn element sits between two ordinary nets and behaves like a lossless
transmission line: it carries a characteristic impedance and a one-way delay,
its far end reflects, and the reflection is visible as a real-valued waveform a
student can read. The four canonical terminations - unterminated, series,
parallel and a mismatched load - are computed from the closed-form
superposition, so a 3.3 V driver into an open 50 ohm line peaks at **5.500 V**,
the same circuit with a 50 ohm source resistance is a flat **3.300 V** step, and
moving only the edge rate to 1 ns drops the peak to **4.368 V**. That last
number is the lesson the whole capstone exists to teach: the regime is entered
by edge rate, not by clock rate. It needs no MNA, no Newton iteration and no
timestep control.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-18 | required | the reflection, the ring and the termination that fixes them - the demonstration the diagnosis exists to motivate. Remove it and FEAT-058's verdict has nothing to show |
| CAP-04 | beneficial | the breadboard's own threshold: the lumped ladder stays correct where `t_flight <~ t_r/6`, and this element is the other side of that same threshold |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-047 | A 345.6 ps one-way delay and a 50 ps edge are numbers in seconds. Against a dimensionless tick the element has no parameter it can state honestly |
| FEAT-060 | Not a data dependency - a **permanence** ordering, and it is deliberate. This element is the cheapest deliverable in CAP-18 and the only one committing permanent public surface: a frozen save tag that is a hard error in older readers, a mandatory palette entry under a green totality test, and a K9 obligation. It ships after the reversible work. See Design notes |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| - | No task id. The registry's task space is closed at TASK-0112 and this feature was minted with CAP-18 after it closed, following the recorded FEAT-054..FEAT-057 precedent. Minting its tasks is a maintainer decision | - |

## Acceptance criteria

1. One new element type between two ordinary nets, carrying a characteristic
   impedance and a length-or-delay, with the source and load resistances as
   parameters on the element for now (see Design notes on the FEAT-027
   migration).
2. The far-end waveform for the four canonical terminations is byte-identical
   to a golden **and** agrees with the closed-form lattice solution to **1e-12
   relative**. Two separate assertions on purpose: a golden regenerated for the
   wrong reason still fails the analytic one.
3. The same circuit at `t_r` = 50 ps, 1 ns and 5 ns gives far-end peaks of
   **166.7%, 132.3% and under 105%** of the rail. This criterion pins the
   **lesson** rather than a number, and it is the one that fails if the source
   waveform is ever silently idealised back to a step.
4. A real-valued row exists in the **existing** signal-trace window, because
   `docs/simulation-semantics.md:44` at `2d0ca9d` admits only a two-state
   `BitSet` or a null reference and there is otherwise nothing to draw a
   reflection in. Headless CSV output of the same row is the fallback form.
5. A diagnostic names the receiving element and the peak when the far-end
   voltage exceeds the rail, and clears when a termination fixes it.
6. **A first-year drawing an adder sees no transmission line and no edge-rate
   field.** Palette visibility is derived from context, asserted by a test
   rather than by intention. At `2d0ca9d` the element registry holds 35 types
   against 32 palette entries; after this feature the registry count rises and
   the palette count must not.
7. The series truncation tolerance is a stated parameter and the term count is
   reported, not hidden: 52 terms at 1e-9 for the worst teaching case, 1 term
   for any terminated case.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 78 | Element descriptor and registry | informs - one new element type, and a new element type costs zero format versions; the measured registration tax is 66 lines across 12 files |
| 76 | Visual ergonomics and platform integration | informs - a waveform trace with a rail-overshoot marker inherits every requirement in that issue |
| 232 | Simulation hot path: per-signal `BitSet` allocation | informs - the value domain this element cannot express is the same one that issue is about, from the other side |
| - | the closed-form transmission line and the reflection lab | **no issue** |

## Design notes

**Why an element and not a net kind.** A 50 mm net at 20 Gb/s has a 345.6 ps
flight time against a 50 ps unit interval, i.e. **6.9 simultaneous distinct
values in flight**, while `src/jls/elem/WireNet.java:405` at `2d0ca9d` holds one
`@Nullable BitSet`. Equipotentiality is `WireNet`'s definition, not a parameter
of it. Extending it into a distributed net kind lands on a 531-line class
carrying #98's insertion-order multi-driver determinism, touches four editor
connect sites, and needs a format bump for every file containing a long wire.
The element route was measured at **66 lines of registration tax across 12
files, zero format versions**, and it matches SPICE `T`/`O`/`U`/`LTRA`, IBIS,
Touchstone and every commercial SI tool.

**Why closed form and not the analog solver.** For a lossless line with
resistive terminations driven by a piecewise-linear source the exact solution is
a geometric superposition of delayed scaled copies of the source. Computed and
verified two independent ways - a hand lattice and an implementation of the
superposition with a finite-rise-time ramp, agreeing to the printed digit. The
kernel is eight lines of textbook theory and two-thirds of the feature's line
count is diagnostics and tests, which is the correct ratio for a teaching
element. Routing it through the analog engine instead puts the best teaching
artifact in this regime behind that programme's own named stall point and raises
its price from 2-3.5 mw to **28-41.5 mw** for a lesson the closed form teaches
exactly. Under FEAT-031 the two become one element type with two `impl`
bindings, and the closed form is then an exact oracle for the numerical one on
the linear-resistive subset.

**The arithmetic the golden is checked against.** 3.3 V, `Z0` = 50 ohm,
`Td` = 345.6 ps (5 cm of FR-4 stripline). `R_s` = 10 ohm into an open far end
gives `Gamma_s = (10-50)/(10+50) = -2/3` exactly and `Gamma_L = +1.0000`, launch
`3.3 * 50/60 = 2.7500 V`. The far-end value after the k-th arrival is

`V_k = V_final * (1 - Gamma_s^k)`, `V_final = 3.3 V`, `Gamma_s = -2/3`

which generates **5.5000, 1.8333, 4.2778, 2.6481, 3.7346, 3.0103, 3.4931 V** for
k = 1..7, converging to 3.3 V. Peak **5.5000 V = 166.7%** of the rail. `R_s` =
50 ohm gives `Gamma_s` = 0, one series term and **3.3000 V** flat; `R_L` = 50
ohm gives `Gamma_L` = 0 and 2.7500 V flat. At `t_r` = 1 ns, which exceeds
`2*Td` = 691 ps, the peak falls to **4.3675 V = 132.3%**.

**One corpus number is superseded and is recorded here rather than repeated.**
`docs/plan/capstones/CAP-18-net-that-stopped-being-a-wire.md` prints the ring as
"1.8333 / 4.2778 / 2.6481 / **3.1914**". 3.1914 V is not a term of this series -
`V_5` is 3.7346 V, and 3.1914/3.3 = 0.96709 gives no integer k. The first three
ring values are confirmed; the fourth is superseded by the closed form above,
which is what criterion 2's 1e-12 assertion is written against. #313 recorded
the same correction; this document does not re-derive it silently.

**Convergence.** For passive resistive terminations `|Gamma_s * Gamma_L| < 1`,
so truncation at tolerance `tol` needs
`ceil(log tol / log|Gamma_s * Gamma_L|)` terms. Re-derived: at
`Gamma_s = -2/3`, `|Gamma_s|^k < 1e-9` needs
`k >= ceil(9 * ln10 / ln1.5) = 52`; a terminated case needs 1.

**Why a lumped RLGC ladder is not an acceptable substitute, measured.** On
ngspice-42 with the same 5 cm line, the exact `T` device gives a peak of
0.99995 V (-0.005% error on the open-circuit doubling), while LC ladders give
**+19.60% / +16.23% / +10.18% / +5.60% / +4.47%** spurious ringing overshoot at
N = 25 / 55 / 110 / 220 / 440 sections, at ~24x and ~112x the analysis time.
The error converges only as 1/N, so 440 sections still show a 4.5% overshoot a
student would read as a real signal-integrity problem that does not exist. The
ladder stays correct and free where the structure is electrically short
(`t_flight <~ t_r/6`) - the two are the two sides of one threshold, computable
at elaboration, not competing designs.

**Where the source impedance lives.** The line owns both the source impedance
and the edge rate as parameters here, and they move to the driver when FEAT-027
lands, with the line's parameter becoming a **defaulted override**. `Output` has
no strength or impedance at HEAD and acquiring one is FEAT-027's 6-9 mw
strength-lattice work; blocking a 2-3.5 mw element behind it for an attribute
the line can carry meanwhile is the wrong trade. The migration is a defaulting
rule, not a redesign - and both contracts must say so, or it becomes one.

## Risks

- **This is the only rung that commits permanent public surface**, and it is the
  cheapest. A frozen save tag is a hard error in older readers, a palette entry
  is mandatory under a green totality test, and a K9 obligation does not expire.
  If the programme stalls after this feature, JLS owns a public
  transmission-line element with a half-answered story and no way to remove it.
  That is why it is sequenced third.
- **The palette totality test is a joint constraint (K18-4).**
  `test/jls/edit/PaletteContractTest.java` asserts one palette entry per
  registered element type outside `{SubCircuit, WireEnd, TestGen}`. Adding an
  element type therefore *forces* a palette entry unless visibility is derived
  from context. If it cannot be, stop at the headless CSV form: the lesson
  survives a missing palette entry; the pedagogy floor does not survive an
  element a first-year meets while drawing an adder.
- **Correctness is a stop condition, not a tuning target (K18-1).** If the
  four-termination corpus does not agree with the closed-form lattice to 1e-12
  relative, the superposition is implemented wrong and no tuning fixes it. Stop
  before the dialog, the renderer and the palette entry.
- **The real-valued trace row is shared scope.** It is priced inside this
  feature but the analog capstones need the same row. Whichever lands first pays
  for it; the second must be re-scoped rather than re-estimated.
- **A green test currently enforces a violation.** At `2d0ca9d` the registry has
  35 types against 32 palette entries, so the totality assertion and the tree
  already disagree. This feature must not be the change that discovers it.

## Evidence

- **The cardinality failure that decides element-versus-net-kind.** Verified at
  `2d0ca9d`: `src/jls/elem/WireNet.java:405` -
  `private @Nullable BitSet value = new BitSet(1);` - one value per net, against
  6.9 simultaneous values in flight on a 50 mm net at 20 Gb/s.
- **Propagation through a net is exactly zero, not small.** Verified at
  `2d0ca9d`: `src/jls/elem/WireNet.java:505-507` posts every consumer event at
  `now`.
- **The value domain has no real numbers**, which is why criterion 4 exists.
  Verified at `2d0ca9d`: `docs/simulation-semantics.md:44` admits a
  `java.util.BitSet` or a null reference.
- **A new element type costs zero format versions.** The element hierarchy at
  `2d0ca9d` is `src/jls/elem/Element.java:17-18`,
  `public abstract sealed class Element` permitting
  `DisplayElement, LogicElement, Wire`. The 66-lines-across-12-files
  registration tax was measured with `git show --stat 38a0544`.
- **The palette contract.** `test/jls/edit/PaletteContractTest.java:44-66` at
  `2d0ca9d` (landmark: `paletteIsTotalOverTheElementRegistry`), asserting one
  palette entry per registered type outside the three excluded tags.
- **The reflection arithmetic and the ngspice ladder measurement.**
  `docs/plan/capstones/CAP-18-net-that-stopped-being-a-wire.md`, the Evidence
  section and refusal §8.9, read in the working tree at `839fb3a`; the same
  material is at `docs/plan/evidence/highfreq-determination.md` §8.9 in that
  tree. **Neither path resolves at `2d0ca9d`** - the determination landed in
  `3a81a4a` - and that is stated here per D12 rather than left as a citation
  that does not resolve.
- **Cost reconciliation.** Band **2-3.5 mw**, from CAP-18 §7.1's feature table,
  which is the figure #313's Cost table carries for this row and which other
  issues resolve against. **The real-valued trace row is a further 0.5-1 mw**
  and is priced as its own row in #313 rather than folded into this band, so a
  scheduler funding this feature alone should budget **2.5-4.5 mw**. The same
  source's stage table prices H4 (this element plus that trace row) at
  **3.5-5.5 mw**, which the two rows do not reproduce; both figures are printed
  and neither was adjusted to make the other true. No task rollup exists: this
  feature has no task ids.
