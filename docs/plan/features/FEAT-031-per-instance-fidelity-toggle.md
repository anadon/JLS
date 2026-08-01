# FEAT-031 - The per-instance fidelity toggle and its boundary harness

**Status:** proposed | **Cost:** 5-8 mw | **Owner program:** P8 |
**Spine rank:** S19

## Capability delivered

One subcircuit instance in a design can be told to run as the drawn logic or as
a single fast implementation of the same definition, chosen per instance and
saved with the file. That makes correctness a property of a *boundary* that a
harness can check, rather than a property of a whole program that somebody
asserts: the fast implementation and the structural referent are in the same
file, so "these agree" is a test and not a promise. It is also what makes a
large design bringable-up one block at a time.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | one file, one attribute flipped, is what makes the behavioral and structural tiers share a golden |
| CAP-03 | required | the behavioral and structural tiers must be one file with one attribute flipped |
| CAP-01 | beneficial | lets a team member run one subcircuit behaviorally while another edits it structurally |
| CAP-04 | beneficial | bring the board up one module at a time against the schematic behavior |
| CAP-09 | beneficial | parity as a property of a boundary is how an unfamiliar subcircuit is verified in isolation |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-030 | The *compiled* binding wants the plane arrays and the queue this feature does not build. The *behavioral* binding needs none of it, so this dependency scopes to one arm and must not be used to gate the whole feature |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0065 | The saved per-instance fidelity attribute | The attribute itself, its closed set of implementations, and its versioning |
| TASK-0066 | The boundary handover harness | Toggling a binding at a declared instant and mapping that boundary's state, with the null-toggle equivalence gate |
| TASK-0023 | Measure the behavioral binding and the levelized cost at scale | Shared with FEAT-009 and FEAT-030: an instrumented behavioral machine on a real bus, event-counted |
| TASK-0079 | Draw the machine and bring it up boundary by boundary | Shared with FEAT-038: the first real use, which is also the test that the boundary discipline survives a 580-element design |

## Acceptance criteria

1. `SubCircuit` carries one saved attribute naming one member of a closed,
   core-internal, sealed set of implementations of *that instance's definition*.
   Structural is the default and is what happens today.
2. The observation function is written down **before** any binding exists:
   across a fidelity boundary the observable is the settled output word per
   sampling instant, indexed and not timestamped, where sampling instants are
   quiescence points or edges of a declared sync net. Only combinational
   transport delay strictly inside the boundary is quotiented out.
3. `BoundaryHarness` runs the same stimulus through both bindings and compares
   the observation function: exhaustive under 16 input bits; a seeded 10^6-vector
   sample plus declared corner vectors (widths 1/31/32/33/63/64/65, HiZ,
   undriven) above.
4. **The deliberately-failing null test passes by failing.** A knowingly-wrong
   binding is rejected by the harness on demand. If it is not, this feature
   stops and nothing downstream merges.
5. A reflective guard asserts no binding touches the event queue.
6. Refusals are by name and are refusals, never silent degradation: a
   `DelayGate` used as a delay line, a `TriState` whose behavior depends on
   turn-off relative to turn-on, a level-sensitive `Memory` write, more than one
   incommensurable `Clock`, a block that does not settle.
7. Whenever any non-structural binding is active, an abstraction banner appears
   on the outcome line and in the waveform header, and an instructor can
   restrict the permitted set so a lab must be drawn.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the fidelity boundary, the harness and the null test | **no issue** - #232 covers only the value representation, not the boundary |

## Design notes

The mechanism is chosen specifically to avoid adding a `Cpu` element to the
sealed permits list, which four of five surveyed proposals did.
`SubCircuit` is already in `Element`'s permits, so the toggle costs no new
sealed permit, no palette entry, no help page, no icon and no switch-
exhaustiveness ripple. A behavioral binding is one element with one `react()`,
structurally indistinguishable from `Adder` (lumped `30 * bits`), `Memory`
(lumped access time), `TruthTable`, `StateMachine` or `RegisterFile` - all of
which already ship. That is the legitimacy argument, and it is retroactive
articulation of a rule the repository already follows, not a new one.

The contract already exists at HEAD in draft: `docs/parity-contract.md` defines
the bound boundary and its two implementations (§2.2), what must be bit-identical
(§3), what is permitted to differ (§4), the harness (§5.2), the deliberately-
failing null test (§5.3) and what a binding must refuse (§6). It is **unratified**
- commit `b299d63` demoted it deliberately. Reference it; do not restate it; and
note that ratification is one of the open decisions this feature forces.

Handover is where this feature earns its place in the spine. Toggling a binding
at a declared instant maps only *one boundary's* architectural state; `Memory`,
the bus and the console are the same objects in the same run. That is why this
does **not** require FEAT-035's general simulation-state serialization, even
though TASK-0066 is shared with it - the shared task is the state mapping, and
the boundary case is the small one.

Provability at student scale is deliberate: an ALU subcircuit, drawn versus
compiled, with zero RISC-V and zero Linux, is a complete demonstration of the
mechanism.

## Risks

- **An unfalsifiable harness is worse than no harness**, because it converts an
  unchecked claim into a checked-looking one. Kill criterion K4 makes the null
  test a hard stop for exactly this reason; write the null test first.
- **Silent degradation is the tempting failure.** Every refusal in criterion 6
  is a case where the honest answer costs a demo. `Memory`'s level-sensitive
  write glitch is a timing phenomenon and quietly deleting it would teach that a
  recorded defect does not exist.
- **Scope pull toward a compiled engine.** The compiled arm wants FEAT-030's
  plane arrays and, past that, a strategy that recorded decision #221 governs.
  Keep the behavioral arm shippable alone or this 5-8 mw feature acquires a
  12-20 mw prerequisite it does not need.
- **The banner is not optional.** A run whose numbers came from a behavioral
  binding, presented without saying so, is the exact failure the parity contract
  exists to prevent.
- **The "costs no format version" argument is time-limited by FEAT-002.** It
  rests on unknown attribute *names* being silently ignored, which is exactly the
  behavior FEAT-002 removes. Old released readers keep ignoring it, so backward
  compatibility is unaffected; but once FEAT-002's policy epoch closes, a reader
  that does not know the fidelity attribute must have a declared answer. The two
  features must agree on the epoch (FEAT-013 owns it) rather than each assuming.

## Evidence

- The mechanism, its five-reason justification, the observation function, the
  harness sampling rule, the reflective guard and the named refusals:
  `03-determination.md` §2 L4.
- The contract at HEAD, unratified: `docs/parity-contract.md` §2.2, §3, §4,
  §5.2, §5.3, §6; demoted by commit `b299d63`.
- `SubCircuit` already in the sealed permits: `src/jls/elem/Element.java:17-18`;
  its per-instance save shape at `src/jls/elem/SubCircuit.java:282-289`.
- Lumped-behavior precedents already shipped: `Adder`'s
  `propDelay = bits * defaultPropDelay` where `defaultPropDelay = 30`
  (`src/jls/elem/Adder.java:33`, `:261`), `Memory`'s lumped access time
  (`src/jls/elem/Memory.java:108-109`, the `accessTime` field), and
  `RegisterFile`, whose own javadoc states it collapses the ~95 elements a RISC
  register file otherwise needs into one (`src/jls/elem/RegisterFile.java:21-28`).
- Unknown attribute *names* are already ignored by old readers, so the new
  attribute costs no format version: `BRIEF.md` §13.
- The null-test stop condition: kill criterion K4, `03-determination.md` §9.
- Cost and spine placement: `10-capstone-plan.md` §2.1 row S19 (5-8 wk).
