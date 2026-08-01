# TASK-0059 - Radix on ports and nets, validated not widened

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** TASK-0056

## Deliverable

Radix becomes a carried, enforced property of ports and nets. **Zero N-ary
elements ship in this task and no drawable circuit can reach radix != 2.**

1. **Carried.** `Put.radix` (default 2) alongside the existing `Put.bits`
   (`src/jls/elem/Put.java:34-35`), with `getRadix()` / `getDigits()` promoted
   from the stubs TASK-0056 reserved to real fields. Same pair on `WireNet`
   beside `WireNet.bits` (`src/jls/elem/WireNet.java:25-26`).
2. **Validated, not widened - this is the whole task.** Width is negotiable in
   JLS: `WireNet.setBits` takes a `Math.max` (`WireNet.java:232`) and `recheck`
   recomputes `bits = Math.max(p.getBits(), bits)` over the ends (`:280`), with
   `bits == 0` meaning "not attached to anything yet" (`:236-238`). Radix has no
   maximum and no arbitrary value. `recheck` and `makeNet` (`:139`) must
   **reject** a net whose attached puts disagree on radix, with a load error
   naming both element ids and both radices in the style `docs/file-format.md`
   §5 already requires.
3. **Refused at edit time, above the width check.** The four connection sites in
   `src/jls/edit/SimpleEditor.java` - `:4015` (end-end), `:4142` (end-wire),
   `:4247` (end-put), `:4358` (put-put), all four verified at HEAD as
   `overlapMessage = "Bits don't match"` - each gain a sibling radix check
   **immediately above** the width check, so a radix mismatch is never
   mis-reported as a width mismatch. The check is **unconditional**: the width
   checks all guard with `bits1 > 0 && bits2 > 0` because 0 means unknown; radix
   is always known, so there is no `> 0` escape hatch.
   Message: `"Radix doesn't match: base-3 cannot drive base-2"`.
4. **Refused at simulation start.** An `IllegalStateException` from
   `Simulator.initSimulation` (`src/jls/sim/Simulator.java:177-201`) if any net's
   radix disagrees with an attached put's. By then a mixed-radix net is a bug in
   JLS, not in the drawing.
5. **`getBits()` becomes a loud shim.** 75 call sites in `src/jls`
   (measured; heaviest `HdlExporter`, `SimpleEditor`, `BatchSimulator`,
   `BatchTracePrinter`) almost all mean "how many *binary* bits do I write, draw
   or mask". `getBits()` throws `IllegalStateException` when radix != 2, which
   converts 75 silent mis-sizings into 75 loud failures that can only fire on an
   N-ary circuit. Sites that genuinely are binary-only - `HdlExporter`,
   `BatchSimulator.vcdValue` (`src/jls/sim/BatchSimulator.java:538-555`) - get an
   explicit binary-only guard instead, because Verilog and VCD *are* binary.
6. **`jls.Util.convert` stops returning `""`.** `src/jls/Util.java:320-338`
   handles bases 2, 10 and 16 and falls off the end with
   `return ""; // shouldn't happen`. Make it total or make it throw; a silent
   empty string is a display bug waiting for base 3.
7. **The K9 ratchet.** A test asserting that the default first-year palette and
   the default element dialogs are unchanged by this task - progressive
   disclosure, not audience exclusion (BRIEF §12 D9).

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-028 | The port type system. Radix on the port rather than in the value is what makes radix 2 cost structurally zero; this task is where that is enforced rather than merely stated. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0056 | This task promotes `Put.getRadix()`/`getDigits()` from the reserved constant-2 accessors TASK-0056 adds to enforced fields, and the loud `getBits()` shim needs a value type that can hold a digit outside `{0,1}` for its failure mode to mean anything. |

## Acceptance test

- **`jls.elem.RadixConnectionTest.recheckRejectsAMixedRadixNet()`** (new):
  build a net with one radix-2 put and one radix-3 put; assert `recheck` reports
  a load error whose message contains both element ids and both radices, and
  assert the net's radix is **not** silently set to either value (contrast with
  `setBits`'s `Math.max`, which this test explicitly does not mirror).
- **`jls.edit.RadixConnectionRefusalTest.allFourConnectionSitesRefuseARadixMismatch()`**
  (new): drive each of the four `SimpleEditor` sites and assert
  `overlapMessage` starts with `"Radix doesn't match"` - not `"Bits don't match"`.
  Parameterized over the four sites so a fifth connection site added later fails
  the test by omission.
- **`jls.elem.RadixConnectionTest.getBitsThrowsOnANonBinaryPut()`** (new):
  assert `IllegalStateException` from `getBits()` at radix 3, and assert it does
  **not** throw at radix 2 - the 75 existing call sites must be unaffected.
- **`jls.UtilFunctionsTest`** (existing, `test/jls/UtilFunctionsTest.java`):
  extend with `convertRefusesAnUnsupportedBase()` asserting the chosen total
  behavior rather than `""`.
- **`jls.PalettePedagogyRatchetTest.defaultPaletteIsUnchangedByTheRadixProgram()`**
  (new, the K9 ratchet): assert `Palette.entries()` in the default view is
  byte-equal to a committed expected list.
- **Every existing golden byte-identical.** Radix is 2 everywhere, so this is an
  equality assertion and there is no tolerance clause.

## Related GitHub issues

**No issue.** The entire N-ary program (FEAT-028, FEAT-029) has no tracker
entry - recorded in the registry's gap list. Issue **#232** (open) touches the
value representation only and does not reach radix; do not cite it as covering
this. Issue **#51** ("Low-severity grab-bag from the 2026 audit:
`TextFilter.setMax` radix, ...", **closed**) uses "radix" in the unrelated
`TextFilter` sense and must not be cited as prior art here.

## Notes

- **Trap: `makeNet` assigns, `recheck` maxes.** `WireNet.java:139` is
  `net.bits = put.getBits();` inside a loop over attached ends - the last put
  wins - while `:280` takes a `Math.max`. Two different widening policies in two
  methods that must now agree on one *rejection* policy. Unify deliberately; do
  not copy either.
- **Trap: `bits == 0` means "unattached", and there is no radix analogue.**
  `WireNet.java:236-238` documents 0 as the unconnected sentinel, and a
  `Constant` adapts to the net's width because of it (`docs/simulation-semantics.md`
  §6.2). Radix has no such sentinel: every put is born radix 2. Do not invent
  `radix == 0`.
- **Trap: `RadixBridge` must ship in the same release as the check, not this
  one.** The check makes a mixed-radix drawing impossible; the sanctioned
  crossing element is TASK-0061's. Users are stuck between the two. Sequence the
  release, not the task.
- **No coercion is defined, ever.** A base-3 digit `2` has no image in `{0,1}`.
  Folding it would repeat exactly the defect the roadmap is currently removing:
  `NetlistImporter.connectConstant` folds Yosys `x` to 0 and reports it through a
  field named `coercedX` (`src/jls/hdl/imp/ImportSummary.java:28, 59, 97-100`,
  verified). The program must
  not delete one silent coercion and add another.
- **Mixed-radix is the normal case, not an edge case.** The clock stays binary
  (`Clock` alternates one bit: `src/jls/elem/Clock.java:391, 416` both `flip(0)`), so every sequential
  radix-N design has binary clock nets and radix-N data nets in one drawing.
  Per-port radix makes that free; a per-circuit radix would make it impossible.

## Evidence

- `src/jls/elem/Put.java:34-35, 139-148` - `bits` and `getBits()`; radix has no
  counterpart at HEAD.
- `src/jls/elem/WireNet.java:25-26, 230-233, 236-244, 272-302` - the width
  negotiation this task must *not* copy for radix.
- `src/jls/elem/WireNet.java:97-165` - `makeNet`; the assignment at `:139`.
- `src/jls/edit/SimpleEditor.java:4015, 4142, 4247, 4358` - the four
  `"Bits don't match"` sites, all verified at HEAD.
- `src/jls/Util.java:320-338` - `convert`, with `return "";` at `:337`.
- `getBits()` call-site census, measured at HEAD: 75
  (`grep -rn '\.getBits()' src/jls --include=*.java`).
- `docs/simulation-semantics.md:57-59` - "Bit width is a property of elements and
  wire nets, not of the BitSet ... Reading code interprets a value at the
  reader's declared width." Radix is the generalization of that sentence.
- `07-mvl-determination.md` §3.4 (the four enforcement layers), §3.5 (the loud
  shim), §1.1 stage 1 (3-5 weeks for the whole port type system, of which this
  task is the enforcement core).
