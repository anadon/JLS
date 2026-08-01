# FEAT-049 - Analog device models, the drawn palette and convergence hardening

**Status:** proposed | **Cost:** 21-33 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A student draws analog devices on the canvas the way they already draw gates, and
the circuit they drew converges. This feature makes the element hierarchy admit a
non-reacting device at all, gives that family a datum element and a node-partition
contract, puts it on a palette a first-year never sees, and supplies the model set
a teaching lab needs - resistors through transistors, with vendor subcircuit
models read as data rather than curated in tree - together with the continuation,
damping and limiting apparatus that separates "a tool that runs the four circuits
in the determination" from "a tool a course can set homework on". It is the
difference between an analog engine and an analog product.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-12 | required | the photodiode, the transimpedance amplifier, the high-pass and the Sallen-Key are drawn devices, and the hardening is what makes the sixth circuit a student draws converge rather than only the first four |
| CAP-14 | required | a parity corpus is device-shaped; two linear fixtures compared against an external simulator is not a parity claim about a device library |
| CAP-10 | beneficial | only the class-D rung, a rendered tone-and-distortion lab rather than a live demo, needs a real transistor half-bridge |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-046 | A device model is a set of matrix stamps and their derivatives. There is no matrix to stamp into, no Newton loop to harden and no timestep controller to veto until the solver core exists |

FEAT-008 is not a hard gate but bounds when this can be finished honestly: the
generic dialog and the symbol renderers are `jls.edit` surface, `jls.edit` carries
no coverage floor at HEAD, and no harness can construct a dialog in a test.
FEAT-048 runs the other way - the bridges depend on this feature's port widening.

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0103 | Device and transistor models | The device set, each with its parameter list, its temperature and charge-storage behavior, and its stamp test |
| TASK-0104 | Convergence hardening | Continuation, damping and limiting, plus the corpus of circuits that did not converge |
| TASK-0105 | Per-view palettes and the analog palette | The palette contract gains a view dimension; the analog palette ships default-hidden, opt-in and derived from the model |

## Acceptance criteria

1. An analog device is a **fourth permit** on the sealed element hierarchy, not a
   subclass of the reacting one. A test asserts it is never seeded by the
   simulator's initialization walk and never handed a pin-change payload. The port
   widening lands as a **standalone commit carrying no analog code**, so the
   compiler and the existing suite report the blast radius before any device
   exists, and the net-propagation cast becomes a guard - independently a
   correctness fix, and tested as one.
2. **Every analog region has exactly one datum.** A region with no ground element,
   and a region that partitions into disconnected sub-regions, each produce a
   diagnostic naming the drawn subcircuit and the element to place; no
   student-reachable path emits a matrix-singularity message. Node index
   assignment is a pure function of circuit content in stable-id order, asserted
   by a test that permutes save order and compares node indices.
3. **The palette ratchet ships before the first analog element type is
   registered.** Palette totality gains a view dimension, so totality becomes
   "exactly one entry in exactly one view's palette", and the default view's row
   count is pinned. Analog visibility is derived from the model - the editing
   context is an analog-bound subcircuit - never from a preference.
4. **One generic device dialog and one generic renderer** serve every device type,
   with exactly three bespoke dialogs named in advance, and the parameter
   descriptors for every device type written before any dialog code exists.
5. Each device has matrix-stamp tests asserted entry by entry on raw bit patterns
   in three topologies, so a sign error or a transposed conductance is caught
   below physical tolerance.
6. One bipolar model, one MOSFET level, one JFET. The simpler bipolar model is a
   documented **parameter tier** of the shipped one - asserted by a test that a
   card carrying only the three first-order parameters reproduces it exactly - not
   a second model.
7. **The limiting protocol is asserted, not assumed:** a step that was limited
   forces another Newton iteration regardless of the residual. A test drives a
   device into the limited region and asserts the extra iteration happened.
8. A **200-circuit hard corpus** exists, its non-convergence rate is published as
   a number and ratcheted as a test, and the **rejected-timestep rate is
   first-class output** - how many timesteps were rejected, out of how many, and
   which drawn element contributed most - so a student can act on it without
   reading solver source.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the analog element hierarchy, the device models, the drawn palette and convergence hardening | **no issue** |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the registry and palette descriptors this feature extends with a view dimension are #78's shipped half |

## Design notes

**Riding the reacting element type is actively wrong, not merely ugly.** A
no-op-react analog device inheriting from the logic element type would still be
handed pin changes by net propagation and still be seeded by the initialization
walk (`src/jls/sim/Simulator.java:196-198`), silently joining the digital net
graph. The fourth permit turns that into eight compile errors, each a decision
someone must make on purpose. The real blocker is one level down: a port's owner
is typed as the reacting element (`src/jls/elem/Put.java:25`), so an analog
element cannot own a terminal at all until that field widens - source-compatible
for all 35 existing element classes, and the largest hierarchy change in the
analog program.

**The palette is the K9 gate, and the gate is currently held shut by a green
test.** `paletteIsTotalOverTheElementRegistry`
(`test/jls/edit/PaletteContractTest.java:47-66`) asserts exactly one palette entry
per registered type outside three documented exceptions: 32 palette rows
(`src/jls/edit/Palette.java:123-190`) against 35 registry types. Registering
twenty-two analog types under that test lands twenty-two mandatory buttons on a
first-year's toolbar - 69% palette growth, enforced by a passing test - and an
unregistered type cannot round-trip through the save format at all. The view
dimension makes progressive disclosure mechanical instead of aspirational, and is
strictly stronger than a menu toggle because creating an analog region is an
explicit named action.

**The dialog decision is arithmetic, not taste.** A bespoke dialog and renderer
per device is roughly 8,250 source lines and about 71% of the measured
addable-uncovered line commons; one generic dialog over the existing form dialog
plus one renderer driven by static symbol paths, plus three bespoke dialogs, is
about 11.7%. The naive design does not fit the coverage configuration as written,
and a stall under it would bankrupt every other capstone's coverage budget too.

**Scope by era and by tier, and get op-amps for nothing.** The card grammar and
its size rule belong to FEAT-046; what this feature adds is per-device parameter
tiering, the same progressive-disclosure argument applied to model cards.
Operational amplifiers cost zero incremental model work because vendor
macromodels are subcircuits of primitives already shipped - which is why
polynomial controlled sources are mandatory rather than an extension. A behavioral
op-amp element ships beside them so a first-year has something to draw, and the
measured high-frequency divergence between the two is itself teaching content.

**Order convergence before breadth, deliberately against demo value.** The
remaining device types are explicitly low demo-value per week; hardening is
invisible and is what makes the tool assignable. The measured spread that
justifies a multi-week stage: at the same circuit size, one capstone circuit runs
at 2.00 Newton iterations per timepoint with one rejection in 10,012, and a diode
bridge with an astable runs at 20.4 iterations with 15.1% rejection. The code is
about one week - the limiting apparatus is 156 lines of permissively licensed C
that GPL-3.0-or-later can absorb with its notices - and validation is the rest.

**One accounting note.** The 21-33 band is the sum of the three
device-and-hardening stages. The entry increment inside this feature - the fourth
permit, the port widening, the datum element and its diagnostics, the first eight
drawn devices on the generic dialog and renderer, and the palette view dimension,
about 7-10.5 mw - is priced inside CAP-12's 11-16 mw marginal band and is
deliberately not added again here.

## Risks

- **The half-finished analog engine is the largest risk in the analog program.**
  Not a wrong solver: the drawn-element increment lands, a palette and a save
  surface become public, and the program stalls before hardening - leaving a tool
  that fails on the fifth circuit a student draws and cannot be withdrawn.
  Mitigations are structural: ship eight devices rather than twenty-two, hold the
  coverage draw at the generic-dialog figure, and stop before the transistor stage
  if the drawn capstone has not run by 24 cumulative maintainer-weeks.
- **Convergence may not reach homework grade.** If the hard corpus is above 5%
  non-convergent after six maintainer-weeks of hardening, restrict the shipped
  palette to the linear and diode set, publish the corpus as the refusal, stop.
  And if the coverage draw exceeds 900 uncovered lines at the end of the
  drawn-element increment, the generic-dialog design has failed and the remaining
  device types must not be built.
- **Getting the limiting protocol wrong produces plausible, wrong, reproducible
  answers** - the worst failure available to a teaching tool, because the golden
  then agrees with itself forever.
- **One deferral needs a maintainer call, not a study decision.** The higher
  MOSFET level is deferred on measured grounds - no capstone circuit uses it, it
  is materially worse-conditioned than the level that ships, and the shared
  transistor layer means adding it later costs nothing extra. That contradicts an
  earlier brief and belongs in CAP-14's open decisions.

## Evidence

- Element hierarchy, the eight forced decisions, the port blocker and the
  standalone-commit rule: `11-analog-determination.md:227-261` (§2.2, D-A3). Datum
  element, node partition in stable-id order and the two singularity diagnostics:
  `:334-378` (§2.4).
- Palette gate, view dimension and ratchet-first: `:582-609` (§2.8, D-A10). Dialog
  and renderer arithmetic and the three bespoke dialogs: `:610-639` (§2.9, D-A11).
- Device set, parameter tiering, the single-bipolar-model finding, the polynomial
  requirement and the free-op-amp measurement: `:725-796` (§3.2); the MOSFET
  deferral at `:772-786` (D-A13); vendor models as data at `:797-815` (D-A14).
- Stage bands: `:1220-1230` (transistors, 7-11 mw), `:1232-1246` (hardening,
  6-10 mw), `:1248-1256` (remaining palette, 8-12 mw). Kill criteria at
  `:1545-1571` and `:1609-1642`; the coverage kill at `:1651`.
- Verified at HEAD `7468f84`, whose `src/`, `test/` and `pom.xml` are byte
  identical to the registry anchor `b54e6ee`: `src/jls/elem/Element.java:17-18`
  (three permits); `src/jls/elem/Put.java:16-17,25` (two permits; owner field
  typed as the reacting element); `instanceof LogicElement` at exactly 8 sites in
  6 files; `src/jls/sim/Simulator.java:196-198` (initialization walk);
  `src/jls/elem/WireNet.java:507` (the blind cast);
  `src/jls/elem/ElementRegistry.java:38-76` (35 registered types);
  `src/jls/edit/Palette.java:123-190` (32 entries);
  `test/jls/edit/PaletteContractTest.java:36-66` (totality test and its three
  documented exceptions); `docs/virtual-hardware-parity.md:1903-1917`
  (32-against-35, and "until that test exists, K9 is aspiration").
- `StrictMath`, `ProcessBuilder` and `javax.sound` each return a zero count over
  all of `src/` at HEAD, so every constraint this feature inherits is greenfield.
