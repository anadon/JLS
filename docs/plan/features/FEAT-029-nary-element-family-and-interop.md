# FEAT-029 - The N-ary element family and its interop

**Status:** proposed | **Cost:** 9-13 mw | **Owner program:** P2 |
**Spine rank:** -

## Capability delivered

A balanced-ternary datapath is something a person draws, clocks, probes, traces,
tests with `-t` vectors and exports - using the same palette, the same
waveform viewer, the same test grammar and the same HDL path as every binary
circuit in JLS. This is the user-visible half of the N-ary program: FEAT-028
makes higher radix expressible, this makes it drawable and makes the resulting
design leave the tool.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-03 | required | the drawable, simulable balanced-ternary datapath that exports, dumps and tests like any other circuit |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-028 | The elements' ports declare a radix and their bodies call the plane operator kernel; neither exists without FEAT-028 |

FEAT-050 is **not** a prerequisite: these are in-tree element types and reach the
palette through the compiled-in path. It becomes one only if the family is
delivered as an out-of-tree contribution, which is not the plan of record.

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0060 | The higher-radix operator kernel | Shared with FEAT-028: the element bodies are thin wrappers over this kernel |
| TASK-0061 | The N-ary element family | The registered element types themselves, shipped in coverage-floor-sized batches |
| TASK-0062 | N-ary interop: lowering, waveform manifest and test grammar | Export, VCD and `-t` support, with a declared radix manifest |
| TASK-0105 | Per-view palettes and the analog palette | Shared with FEAT-049, FEAT-043 and FEAT-008: the palette contract must gain a dimension before an N-ary palette can ship default-hidden |
| TASK-0083 | Draw the ternary CPU | Shared with FEAT-039: the drawn machine is the element census that proves the family is complete rather than merely present |

## Acceptance criteria

1. The family is registered, drawable, and simulable: min/max/literal gates, the
   three negation modes, an adder, a multiplexer (the T-gate), a constant, a
   display, a radix bridge and a truth table.
2. **No radix attribute is added to any existing element type.** These are new
   types; a test asserts every pre-existing type still reports radix 2.
3. A drawn ternary datapath runs, and its waveform dump carries a declared radix
   manifest so a reader can interpret the digits.
4. The `-t` test-vector grammar accepts higher-radix literals through the
   existing token-rewrite pre-pass, and a batch run of a ternary fixture matches
   a committed golden byte for byte.
5. Export lowers a balanced-ternary design to a binary encoding that an external
   flow consumes, and the encoding is documented rather than implied.
6. Balanced rendering (`-`, `0`, `+`) is what the trace, the display element and
   stdout show - not `0/1/2`.
7. The N-ary palette ships **default-hidden and opt-in**, with a test asserting
   a first-year user's default palette is unchanged.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the entire N-ary program | **no issue** |

## Design notes

The element count and the coverage floor interact, and that interaction sets the
calendar rather than the effort. `jls.elem` is floored at 0.730/0.700/0.585
(`pom.xml:478-491`) against a measured 74.65/71.64/60.62 (`pom.xml:391`), and
`pom.xml:317-321` records that the floor only ever moves up. Solving the
weighted-average constraint over the package gives roughly four to six new
element classes per release before the floor trips, so eight types is two
release cycles even at full effort - the *user-visible* capability lands
9-12 calendar months after FEAT-028 starts. The remedy is not lowering the
floor; it is shipping a `*ModelTest` suite with each element, which is already
the house pattern (`RegisterModelTest`, `MemoryModelTest`,
`SubCircuitModelTest`). TASK-0061 is written as batches for this reason.

The registration tax is small and the element body is not. A measured two-element
commit (`git show --stat 38a0544`) is 14 files and 1,188 insertions, of which
133 lines are registration - about 66 lines each - and the rest is body. Eight
types is roughly 4,400 lines of `jls.elem`, which is what makes the coverage
arithmetic above bind.

Cost basis is the project's own per-element rate: P2 prices its arithmetic
family at about 1.5 weeks for the first element including the plumbing pattern
and 0.75 each after (`docs/capability-roadmap/README.md:294-296`), giving
1.5 + 7 x 0.75 = 6.75 wk for the family, plus 3-4 wk for interop, display and
the documentation rewrite - the registry's 9-13 mw.

The palette dependency is not cosmetic. `07-mvl-determination.md` §4.5 records
that a currently-green test forbids the progressive disclosure this family
needs; TASK-0105 is what unblocks it, and it is shared with the analog palette
for exactly the same reason.

## Risks

- **Two release cycles is the honest schedule.** Anyone reading "9-13 weeks" as
  "three months to a drawable ternary CPU" will be wrong by a factor of three
  on the calendar. State effort and calendar separately.
- **Palette pollution.** Eight new types in the default palette is a direct hit
  on kill criterion K9, the pedagogy floor, which outranks everything else in
  the program. Criterion 7 is the guard and it must be a test, not a preference.
- **Interop honesty.** A binary-encoded lowering is a *lowering*, not a ternary
  netlist. If the documentation implies an external tool is simulating ternary
  when it is simulating an encoding, the capability is oversold. Name the
  encoding in the export header.
- **The family is only as complete as the machine that uses it.** Shipping eight
  types nobody has composed into a datapath is how an element family acquires a
  missing primitive discovered a year later. TASK-0083 is listed as a
  prerequisite for exactly this reason even though it belongs primarily to
  FEAT-039.

## Evidence

- Coverage floor and measured aggregate for `jls.elem`: `pom.xml:478-491`
  (0.730/0.700/0.585), `pom.xml:391` (74.65/71.64/60.62), `pom.xml:317-321`
  (the floor only moves up).
- Registration tax versus body size: `git show --stat 38a0544` - 14 files,
  1,188 insertions, 133 of them registration for two elements.
- Per-element cost rate: `docs/capability-roadmap/README.md:294-296`.
- Element type count at HEAD: 35 registered types,
  `src/jls/elem/ElementRegistry.java:38-77`.
- Stage content and banding: `07-mvl-determination.md` §1.1 stages 3 (6-9 wk)
  and 4 (3-4 wk); §1.3 (the two-release-cycle finding); §4.4 (new types, never
  attributes on existing types); §4.5 (the palette and the green test that
  currently forbids disclosure).
- **Cost reconciliation.** Band 9-13 mw. Tasks named for it: TASK-0060,
  TASK-0061, TASK-0062, TASK-0083, TASK-0105, totalling 10 wk. Band and task
  sum agree; no reconciliation is needed. Shared tasks counted once at the
  task level: TASK-0060, TASK-0083, TASK-0105.
