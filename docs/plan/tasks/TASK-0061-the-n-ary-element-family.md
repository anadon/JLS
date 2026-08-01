# TASK-0061 - The N-ary element family

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0059, TASK-0060

## Deliverable

The first shippable batch of registered N-ary element types, each a thin `react`
over the TASK-0060 kernel, each with its model test, sized so the `jls.elem`
coverage floor does not trip.

1. **Batch 1 (this task): `RadixBridge`, `MvlConstant`, `MvlGate`, `MvlNot`.**
   - `RadixBridge` is **non-negotiable and ships first**: TASK-0059's connection
     check makes a mixed-radix drawing impossible, and this is the sanctioned
     crossing, the same role `Splitter`/`Binder` play for width. Its conversion
     is declared per direction and refuses the undefined direction loudly - a
     base-3 digit `2` has no image in `{0,1}` and is never folded.
   - `MvlGate` carries a `Family { MIN_MAX, LUKASIEWICZ, POST }` and an
     `Operation { MIN, MAX, LITERAL }`. Default `MIN_MAX`, because it is the
     only family that collapses *exactly* to today's binary behavior at N=2.
   - `MvlNot` carries `Mode { COMPLEMENT, CYCLIC, DIMINISH }`, default
     `COMPLEMENT` = `(N-1)-d`.
   - `MvlConstant` reduces its value mod `N^digits` - the generalization of
     `docs/simulation-semantics.md` §6.2's `value mod 2^bits`.
   Batch 2 (`MvlAdder`, `MvlMux`, `MvlDisplay`, `MvlTruthTable`) is the same
   task shape in the next release cycle; the id space does not mint a second
   task for it because the work is identical and the batching is a coverage
   constraint, not a design one.
2. **No radix attribute on any existing element type.** This is the load-bearing
   structural decision and it is not restated here beyond its consequence:
   `HdlExporter` classifies by `EXPORTED.contains(el.getClass())` over an
   exact-class `Set.of` (`src/jls/hdl/HdlExporter.java:429-435`), so a radix
   attribute on `Adder` would leave `EXPORTED.contains(Adder.class)` true and
   both emitters would emit `assign sum = a + b;` - valid Verilog computing the
   wrong thing. New classes fail *closed*; new attributes fail *open*.
3. **Full registration per type**, the ~66-line tax measured at
   `git show --stat 38a0544`: `LogicElement.java:17-21` permits,
   `ElementRegistry.java:38-77` `ALL`, `SaveTags.java:41-75` `WRITABLE`,
   `Palette.java:123+` `ENTRIES` (in a **non-default, opt-in palette group**,
   per D9's progressive-disclosure restatement of K9),
   `collab/op/ElementVocabulary.java`, plus the eight test files that commit
   named.
4. **A `*ModelTest` per element**, shipped in the same commit as the element -
   the house pattern (`RegisterModelTest`, `MemoryModelTest`,
   `SubCircuitModelTest`, `TruthTableModelTest` in `test/jls/elem/`).
5. **Port-level refusals implemented, with the message the determination
   specifies.** `Register.C`, `StateMachine.clock`, `Memory.CS/OE/WE/clock`,
   `TriState.control`, `ShiftRegister.amount` and `Decoder`'s output side accept
   radix 2 only: *"`<element>.<port>` is an enable, not a value: it accepts
   radix-2 only. Attached net is radix-`<N>`."* `Clock` refuses at the element
   level: *"Clock drives a binary square wave; a radix-3 clock is not defined."*

Done means: a balanced-ternary min/max datapath can be drawn, clocked from a
binary clock, and simulated, with a `RadixBridge` at each binary boundary.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-029 | The drawable half. Everything before this task is invisible to a user; this is the first commit that changes what can be placed on a canvas. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0059 | An `MvlGate` declares `getRadix() == 3` on its puts. Without TASK-0059 that value is neither carried nor validated, so a radix-3 gate could be wired to a radix-2 net and the mismatch would propagate as a silently wrong number. |
| TASK-0060 | Every one of these elements' `react` bodies is a call into `RadixOps`/`RadixAlphabet`. Writing them before the kernel means writing the truth tables inside four element classes, which is the exact duplication the kernel exists to prevent. |

## Acceptance test

- **`jls.elem.MvlGateModelTest.minMaxCollapsesToAndOrAtRadix2()`** (new): drive
  an `MvlGate(MIN_MAX, MIN)` at radix 2 and an `AndGate` with identical inputs
  through the same simulator; assert the propagated values are equal at every
  timestamp. Same for `MAX` vs `OrGate`.
- **`jls.elem.MvlGateModelTest.minMaxOverTheFullRadix3TruthTable()`** (new):
  all 9 input pairs, asserted against a table written out in the test - not
  computed by the code under test.
- **`jls.elem.RadixBridgeModelTest.aBridgeIsTheOnlyLegalRadixCrossing()`** (new):
  assert a drawn radix-3 net attached to a radix-2 put through a `RadixBridge`
  connects, and assert the same connection without the bridge is refused by the
  `SimpleEditor` check with the `"Radix doesn't match"` message.
- **`jls.elem.RadixBridgeModelTest.digitTwoIsNeverFoldedToBinary()`** (new):
  assert the down-conversion of digit `2` raises the declared diagnostic rather
  than producing `0` or `1`. This is the anti-`coercedX` assertion.
- **`jls.elem.RadixPortRefusalTest.enablePortsAcceptRadix2Only()`** (new,
  parameterized over the eight refusing ports): assert each refuses with a
  message naming the element and the port.
- **`jls.ElementRegistryTest`, `jls.elem.SaveTagsTest`,
  `jls.elem.SealedHierarchyTest`, `jls.edit.PaletteContractTest`,
  `jls.AllElementsRoundTripTest`, `jls.ElementDrawSmokeTest`,
  `jls.ui.ComponentIdentityTest`, `jls.elem.CapabilityInterfaceTest`**
  (all existing): each fails until every new type is registered everywhere.
- **`jls.PalettePedagogyRatchetTest`** (from TASK-0059): the default first-year
  palette must be unchanged - the N-ary group is opt-in.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - the registry half already shipped and is what makes the registration tax a compile error rather than a discovered omission; this task does not close it |

**No issue** exists for the N-ary element family. Recorded as a gap.

## Notes

- **Trap: the `jls.elem` coverage floor is what caps the batch size.**
  `pom.xml:475-491` floors the package at 73.0/70.0/58.5 against a measured
  74.65/71.64/60.62, and `pom.xml:317-321` records that the floor only ever moves
  up (raises need three clean headless canonical-JDK-25 runs with >=2pt headroom,
  `pom.xml:806-812`). Solving `(measured*T + c*N)/(T+N) >= floor` over the
  package's 30,724 instructions gives ~3,900 new instructions at 60% new-code
  coverage before the floor trips - about **4-6 new element classes per
  release**. Eight types is therefore two release cycles at full effort. The
  remedy is the per-element model test, not a lower floor.
- **Trap: a new element type costs ZERO format version, an attribute does not.**
  Unknown *tags* are a hard error (`docs/file-format.md` §3) so an old reader
  never silently mis-loads an `MvlGate`. An old reader silently ignoring
  `int radix 3` on an `Adder` would load a circuit that computes different
  numbers - squarely the class `docs/file-format.md:470` says must bump the
  version. This asymmetry is the second reason for new types over attributes.
- **Trap: 27 element dialogs is 89% of the remaining bundle headroom.** A radix
  control in every existing dialog measures 27 x ~20 = 540 lines at ~0% coverage
  against a 610-line bundle LINE budget (`pom.xml:332-335`, headless
  56.22/54.70/52.53 against floors 54.5/53.5/50.5). The radix-attribute design
  does not fit the CI configuration as written - that is arithmetic, not
  principle.
- **Trap: `TruthTable` cell code `2` already means don't-care**
  (`src/jls/elem/TruthTable.java:79`, verified: "0, 1 or 2 (don't care)") and collides
  head-on with radix-3 data. `MvlTruthTable` in batch 2 must move don't-care to
  a sentinel outside the digit range - which is also sweep-01 V7's
  don't-care/don't-know separation. One fix, two programs.
- **Trap: NAND is not functionally complete for N>2.** The Sheffer-stroke result
  does not generalize; min/max/complement generate only the monotone-plus-complement
  fragment. Every help page saying "NAND is universal" becomes false the moment
  N>2 and must be conditioned. This is why the Post family (cyclic negation) is
  offered at all.
- **Trap: XOR loses parity.** Three inequivalent generalizations exist (sum mod
  N, not-equal indicator, `|a-b|`); under all three a chained XOR over N>2 is not
  a parity function. Pick `sumModN` as default and say so in the help text.

## Evidence

- `src/jls/hdl/HdlExporter.java:429-435` - `EXPORTED` as an exact-class
  `Set.of` over 22 classes; `SKIPPED` at `:437-441`; `TOPOLOGY` at `:443-446`.
  This is the classification that makes new types safe and new attributes unsafe.
- `src/jls/elem/ElementRegistry.java:38-77` - 35 registered types at HEAD.
- `src/jls/elem/LogicElement.java:17-21` - 24 permits at HEAD.
- `src/jls/elem/SaveTags.java:41-75` - 34 writable tags at HEAD.
- `src/jls/edit/Palette.java:118-123` - `ENTRIES` in exact toolbar order; the
  N-ary group is appended as a non-default group.
- `git show --stat 38a0544` - 14 files, 1,188 insertions for two elements, of
  which 133 lines are registration (~66 per element).
- `pom.xml:475-491`, `:317-321`, `:332-335`, `:806-812` - the floors and the
  raise-only ratchet.
- `07-mvl-determination.md` §1.1 stage 3 (6-9 weeks for ~8 types, from the
  project's own 1.5-then-0.75-per-element rate), §4.1 (the family choice and the
  two pedagogic hazards), §4.2 (the port-level refusal list), §4.4 (types not
  attributes, with the four things it buys).
