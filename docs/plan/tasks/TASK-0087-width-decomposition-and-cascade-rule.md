# TASK-0087 - Width decomposition and the cascade rule

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0007, TASK-0085

## Deliverable

JLS's elements are word-level and packages are not. A JLS `Adder` is one element
of arbitrary width - its propagation delay is literally `bits * 30`
(`src/jls/elem/Adder.java:261`) - and a 74LS83 is four bits. An 8-bit adder is two
cascaded 74LS83s; a 32-bit one is eight. This task is the rule that says so, and
the IR change that makes the result expressible.

1. **`Cascade` in the part schema, consumed.** Per part: the slice width, the
   carry-in pin role, the carry-out pin role, the chain-termination rule (tie the
   first slice's carry-in to ground, to the element's own carry-in, or to a
   constant), and whether the chain is ripple or look-ahead. TASK-0085 declares
   the record; this task is its only reader.

2. **`jls.pkg.Decomposer.decompose(Partition, PartBinding) -> PhysicalNetlist`.**
   For each realizable element wider than its part's slice width, emit
   `ceil(bits / sliceWidth)` `SlicedInstance` records - `(stableId, sliceIndex,
   part, section)` - and bind each slice's data pins to the corresponding bit
   range of the element's real nets.

3. **Synthetic inter-slice nets, in the IR, first-class.** A carry between slice
   *i* and slice *i+1* **exists in no `.jls` file**. It is not a projection of the
   `WireNet` partition, and that fact must be in the IR from the start rather than
   bolted onto an emitter later. Concretely:
   - `PhysicalNet` is a sum type over `SchematicNet` (carries the stable-id-keyed
     name from TASK-0008) and `SyntheticNet` (carries a **provenance** record:
     the owning element's stable id, the slice pair, and the role).
   - A synthetic net's name is a pure function of `(stableId, sliceIndex, role)`,
     so it is stable across unrelated edits for the same reason refdes is.
   - The two namespaces are **provably disjoint**, asserted by a test, not by a
     prefix convention nobody checks.

4. **The decomposition table, as data in the part library**, not as code:
   `Adder` -> 74LS83 (w=4); `Register` / `ShiftRegister` -> 74LS273 / 374 / 173 /
   194 (w=4-8); `Mux` -> 74LS153 / 151; `Decoder` -> 74LS138 / 139; the gates
   (w=1); `Constant`; `TriState` -> 74LS244 / 245. Adding a cascadable part is a
   library row.

5. **The residue is reported, never guessed.** A width that is not a multiple of
   the slice width leaves unused bits in the last slice; those pins are
   **tied, not omitted**, and the tie is recorded in the plan so the wiring list
   and the emitters both see it. A width below the slice width is one slice with
   a residue, not a refusal.

6. **The nine non-decomposable types keep their reason.** `Memory`,
   `RegisterFile`, `TruthTable`, `StateMachine`, `FieldExtend`, `SigGen`,
   `TestGen`, `Display`, `SubCircuit` do not decompose at any width. That is a
   property of physics, and the diagnostic names the `-parts` escape - a `Memory`
   bound to a 62256 with its own pin map is a perfectly good row - rather than
   saying no.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-041 | The cascade half, and the reason the physical netlist is not a pure projection of the schematic. Every downstream artifact - BOM counts, the wiring list, the fan-out check, both emitters - is wrong at any width above four without it. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0007 | Extract the net-partition walk into its own package | The physical netlist is the schematic partition **plus** synthetic nets. There must be one partition type to extend; extending a private copy inside an exporter is how the two artifacts diverge. |
| TASK-0085 | The package data schema and footprint binding | Reads the `Cascade` descriptor and the slice width. Neither exists at HEAD - `grep -rniE "footprint|refdes|pinout" src/` returns zero. |

## Acceptance test

`test/jls/pkg/CascadeDecompositionTest`:
- `anEightBitAdderBecomesTwoSlicesWithOneSyntheticCarry()` and
  `aThirtyTwoBitAdderBecomesEightSlicesWithSevenSyntheticCarries()` - the counts
  are the test; an off-by-one in the chain is the defect this catches.
- `theFirstSlicesCarryInIsTerminatedAsTheLibrarySays()` - three fixtures, one per
  termination rule.
- `syntheticAndSchematicNetNamespacesAreDisjoint()` - constructed adversarially: a
  schematic net deliberately named to collide with the synthetic pattern must
  still not collide, or the naming function is wrong.
- `syntheticNetNamesAreAFunctionOfStableIdAndSliceIndexOnly()` - regenerate after
  inserting an unrelated element and assert every synthetic name is unchanged.
- `aResidueWidthTiesTheUnusedPinsAndRecordsTheTie()` - a 6-bit adder over a 4-bit
  part: two slices, two unused bits, both tied, both in the plan.
- `everySyntheticNetCarriesProvenanceNamingItsOwningElement()` - a net with no
  owner is unattributable in a wiring list a person is holding, which is the whole
  failure mode.
- `decompositionIsTotalOverTheRealizationPolicy()` - every type in TASK-0085's
  `REALIZED` bucket either declares a slice width or is documented as w=1, and
  every type in `NO_DEFAULT_REALIZATION` is refused with an actionable reason.
  Registry-keyed and therefore build-breaking when an element type ships.
- `physicalNetCountEqualsSchematicNetsPlusSyntheticNets()` - the conservation law,
  asserted, so a lost carry is a failure rather than a smaller netlist.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | width decomposition, the cascade rule and synthetic inter-slice nets | **no issue.** Nobody has priced this gap in the tracker or in the committed roadmap. |
| 232 | Simulation hot path: per-signal `java.util.BitSet` allocation and bit-by-bit conversion churn the GC | informs - both concern the fact that a JLS value is word-level; neither changes the other. Do not couple them. |

## Notes

- **This is the part nobody priced.** Both the breadboard and the PCB plans
  assumed the netlist emitter is a pure projection of the `WireNet` partition. It
  is not, at any width above the part's slice width, and discovering that inside
  an emitter means retrofitting a net kind into a type that already has consumers.
  Put `SyntheticNet` in the IR in this task or pay for it twice.
- **Do not claim timing.** JLS models an `Adder` as one element with
  `propDelay = bits * defaultPropDelay` (`src/jls/elem/Adder.java:261`,
  `defaultPropDelay = 30` at `:33`). A physical ripple chain of 74LS83s has a
  carry-propagation delay that is a property of the parts, not of that formula.
  The plan reports **structure**, not timing, and the report header must say so -
  the generated-header idiom `VhdlEmitter` already uses is the precedent. Any
  timing claim belongs to a program that has per-part datasheet delays, which this
  library deliberately does not carry yet.
- **Ordering against TASK-0086.** Decomposition runs **before** packing: slices
  are what get packed into sections, not whole elements. If TASK-0086 lands first,
  its goldens must be regenerated when this task lands, because an 8-bit adder
  stops being one component and becomes two. Record that regeneration in the
  commit rather than letting a reviewer wonder why the BOM changed.
- **Slice ordering is little-endian and must be declared.** Slice 0 holds bits
  `[0, w)`. It is arbitrary and it must be written down, because the wiring list,
  the emitters and the breadboard consistency check all index it and a silent
  disagreement is a board that does not work.
- **Gate equivalence and substitution interact here.** A 74LS83 and a 74HC283 are
  the same function at different families with different unit loads. The
  substitution mechanism is TASK-0085's; this task must not assume the family is
  fixed at decomposition time, because TASK-0088's loading check may force a
  substitution after slices exist.

## Evidence

- `src/jls/elem/Adder.java:33` (`defaultPropDelay = 30`), `:261`
  (`propDelay = bits * defaultPropDelay`) - the word-level element with arbitrary
  width and a width-derived delay.
- `src/jls/elem/ElementRegistry.java:38-77` - the 35 registered types over which
  the decomposition policy must be total.
- `src/jls/hdl/HdlExporter.java:428-495` - the four-bucket totality shape the
  decomposition policy copies, and its stated reason.
- `src/jls/hdl/HdlExporter.java:1161-1226` - the private `UnionFind`, i.e. the
  second partition that this task must not turn into a third.
- `src/jls/elem/Element.java:619-622` - `getStableId()`, the only stable key a
  synthetic net name may be derived from.
- `docs/file-format.md` §3 - the `.jls` grammar, which has no representation for
  an inter-slice carry; the synthetic net lives in the physical IR and is never
  written back into a circuit file.
