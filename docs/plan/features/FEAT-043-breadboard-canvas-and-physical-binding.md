# FEAT-043 - The breadboard canvas and its physical-simulation binding

**Status:** proposed | **Cost:** 9-15 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A second canvas exists in which parts are placed on a solderless breadboard -
rows, rails, holes and jumper wires - with its own geometry, its own ops, its
own undo and its own palette. A check reports whether the breadboard and the
schematic describe the same nets, per discrepancy. And the placed physical
arrangement, not the schematic, drives the simulation, so a wire in the wrong
row is a wrong circuit and a contention the schematic hides becomes visible.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-04 | required | The second canvas and the hole-occupancy structure the acceptance test replays |
| CAP-10 | beneficial | The classroom form of an audio output stage is on a breadboard |
| CAP-11 | beneficial | A microphone preamp is a breadboard circuit before it is anything else |
| CAP-12 | beneficial | The classroom form of this circuit is on a breadboard |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-014 | A second canvas means a second geometry for the same artifact. Per-view geometry in its own versioned section and a view discriminator on the geometric ops are exactly what a second canvas requires; without them the two views overwrite each other's coordinates |
| FEAT-008 | A second canvas is added to the editor. Adding it to an unfloored, untestable editor class spends the whole remaining coverage headroom and cannot be regression-tested |
| FEAT-041 | What gets placed on a breadboard is a package, not a logic element. Packing and width decomposition are what turn the drawn design into placeable parts |
| FEAT-027 | The physical binding's value is reporting contention the schematic hides, and contention is only expressible once drive strengths and net kinds exist |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0092 | The breadboard canvas | The canvas itself, its geometry, ops, undo and palette, inside the coverage budget |
| TASK-0093 | Breadboard consistency check and physical binding | The two-view consistency report and the placed arrangement driving the simulation |
| TASK-0036 | Per-view geometry section and the op view discriminator | Shared with FEAT-014: without a view discriminator the second canvas's ops are indistinguishable from the schematic's |
| TASK-0105 | Per-view palettes and the analog palette | Shared with FEAT-049, FEAT-029 and FEAT-008: the breadboard palette is a different palette for the same registry, which the current total-palette contract forbids |

## Acceptance criteria

1. A breadboard view holds parts, rows, rails and jumpers with its own geometry,
   and that geometry round-trips without disturbing the schematic's.
2. Every breadboard mutation is an op with an exact inverse and a view
   discriminator, and undo in one view does not disturb the other.
3. The consistency check reports, per discrepancy, a net the two views do not
   agree on, naming the holes and the parts involved.
4. The simulation driven from the breadboard placement differs from the
   schematic-driven simulation exactly when the placement differs from the
   schematic, and the difference is reported rather than silently produced.
5. A contention created by placement - two outputs in one row - is reported.
   Where the engine cannot yet represent contention, the rule is shipped
   explicitly unimplemented rather than emitting a finding the engine
   contradicts.
6. Adding the canvas does not regress per-edit editor cost or startup time.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | The breadboard canvas, the consistency check and the physical binding | **no issue** |
| 84 | Decompose `SimpleEditor`: 4,119 lines, a 9-state mouse machine, a 305-line `source==` dispatcher that already caused #37, and whole-circuit undo snapshots | overlaps - a second canvas is grafted onto that class; the line count in the title is stale and the class is materially larger at HEAD |

## Design notes

A breadboard strip is a net with no wire in it. That single sentence is the
design problem: the net type at HEAD is built from wire ends, and its iteration
order is pinned by an existing determinism test. Either synthetic wires are
fabricated to represent strips, or a pinned class changes. TASK-0092 recommends
the safe option and records that the maintainer must choose; the choice moves
both breadboard tasks by weeks.

Criterion 6 is not boilerplate here. This is the feature most likely to violate
the pedagogy floor, because it adds a whole view to the editor a first-year
student opens.

## Risks

- **Two views of one artifact double the surface every later format change
  must migrate.** The per-view section in FEAT-014 is what keeps that bounded.
- **The physical binding can produce results the schematic view cannot explain**,
  which is the point and also the support burden.
- **The palette contract currently forbids progressive disclosure**, and a
  breadboard palette is disclosure. TASK-0105 is a hard prerequisite, not a
  convenience.

## Evidence

- The pinned net-iteration determinism that constrains the strip
  representation: the multi-driver determinism regression test over
  `src/jls/elem/WireNet.java`.
- The palette totality contract that a second palette must not break:
  `test/jls/edit/PaletteContractTest.java`, `src/jls/edit/Palette.java`.
- The editor class a second canvas is added to:
  `src/jls/edit/SimpleEditor.java`, 5,852 lines at HEAD.
- Owner: **UNOWNED** in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 9-15 mw; TASK-0092, TASK-0093, TASK-0036 and
  TASK-0105 total 8 wk, of which TASK-0036 and TASK-0105 are shared with
  FEAT-014, FEAT-049, FEAT-029 and FEAT-008 and counted once at the task level.
  The unshared remainder is 4 wk against a 9-15 mw band; the residual is the
  part-placement vocabulary across the package library, which no task id names.
