# TASK-0077 - Honest reset on the register element

**Status:** proposed | **Cost:** 1.5 wk | **Blocked by:** none

## Deliverable

`Register` gains synchronous and asynchronous reset with declared polarity, and
both HDL emitters export it or refuse - never emit a register with its reset
quietly dropped.

1. **Two saved attributes**, appended to `Register.OWN_ATTRIBUTES`
   (`src/jls/elem/Register.java:272-395`, the declarative `Attribute` list this
   element already uses): `reset` in `{none, sync, async}` and `rstpol` in
   `{high, low}`. Both written **only when `reset != none`**, so every existing
   file re-saves byte-identically - the discipline `Memory`'s `sync` attribute
   established (`src/jls/elem/Memory.java:445-449`).

2. **An `R` input, appended last in all four orientation arms** of `init`
   (`:199-266`: RIGHT `:230-235`, LEFT `:240-245`, UP `:250-255`, DOWN
   `:260-265`), created only when `reset != none`.

3. **`react` and `copy` stop indexing puts positionally.** This is the trap.
   `react` reads `inputs.get(1)` for the clock and `inputs.get(0)` for D
   (`:753-757`); `copy` copies exactly two inputs and two outputs by index
   (`:441-448`); and `HdlExporter` reads `ins.get(0)`/`ins.get(1)`
   (`src/jls/hdl/HdlExporter.java:640,665`). All three must become name-based
   (`getInput("D")`, `getInput("C")`, `getInput("R")`) or width-agnostic, or the
   reset pin is silently dropped on paste and silently omitted on export.

4. **The reset value is the existing `init` attribute.** No second constant.
   `initSim` already drives `init` onto Q at time 0 (`:719-737`), so sync point
   zero - the power-on value of every architecturally visible register, which
   TASK-0073's comparator needs stated - has exactly one answer.

5. **Semantics, written into `docs/simulation-semantics.md` §8.1.**
   - **async**: on any react in which R is at its active level, `currentValue`
     and `toBeValue` become the reset value and a `NewValue` is posted at
     `now + propDelay`, regardless of the clock.
   - **sync**: the reset is evaluated on the register's own edge (the
     remembered-clock scheme at `:772-796`), **before** the D capture, so a
     reset and a capture on the same edge resolve to the reset.
   - Both suppress the "D equals `toBeValue`, post nothing" short-circuit
     (`:768,776,787`) while R is active, or a register already holding the reset
     value will not re-drive after a glitch.

6. **Exported honestly, or refused.**
   `HdlModel.RegisterStatement` (`src/jls/hdl/HdlModel.java:401-461`) gains
   `resetKind`, `resetActiveHigh` and a `reset` `Operand`; its fields are
   `public final`, set in one package-private constructor (`:442-459`), so both
   emitters break at compile time until they handle them.
   `VerilogEmitter.register` (`src/jls/hdl/VerilogEmitter.java:325-360`) emits
   `always @(posedge clk or posedge rst)` with an `if (rst)` head for async, and
   an `if (rst)` inside the clocked block for sync.
   `VhdlEmitter` (`src/jls/hdl/VhdlEmitter.java:399-...`) emits the matching
   process shapes. A configuration neither emitter can express **throws
   `HdlExportException`** - the existing refusal mechanism - rather than
   emitting a register without its reset.

7. **`docs/file-format.md` §5** gains the two attributes. No new tag, so no
   `FORMAT` version cost.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-037 | "`Register` has honest reset" is the feature's own first clause. Clock domains and crossing checks (TASK-0078) build on the same pin vocabulary. |

## Prerequisite tasks

None. The `Attribute` machinery, the four-orientation `init`, the emitters and
their goldens all exist at HEAD.

## Acceptance test

`test/jls/SequentialGoldenTest`, extended (it already pins §8.1's four
behaviors by name):

- `asynchronousResetTakesEffectWithoutAClockEdge()` - a pff with `reset async`,
  a clock that never rises, R asserted at t; Q is the reset value at
  `t + propDelay`.
- `synchronousResetTakesEffectOnlyOnTheClockEdge()` - R asserted between edges
  changes nothing; the next edge applies the reset and **not** the D value
  present at that edge.
- `activeLowResetPolarityIsHonored()` - the same two, with `rstpol low`.
- `resetReDrivesEvenWhenTheHeldValueAlreadyMatches()` - pins deliverable 5's
  short-circuit suppression.

`test/jls/hdl/VerilogExportGoldenTest` and `VhdlExportGoldenTest` gain
`register_pff_async_rst` and `register_pff_sync_rst` goldens under
`test/resources/hdl/` (which holds 67 files including `register_pff.v`,
`register_pff.vhdl`, `register_nff.*`, `register_latch.*`).
**The three existing `register_*` golden pairs must stay byte-identical** -
that is the proof the attributes are written only when on and that the emitters'
no-reset path is untouched.

`test/jls/hdl/IverilogCompileTest` and `test/jls/hdl/GhdlCompileTest` must accept
the new goldens. The external compile oracle is what makes "honestly" checkable
rather than asserted.

`test/jls/AllElementsRoundTripTest` and `test/jls/elem/CapabilityInterfaceTest`
sweep the new pin and attributes automatically; the `Register` fixture needs a
reset-bearing sibling added.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | honest reset on `Register` | **no issue**. The parity, device and machine layers are untracked; #232 covers only the value representation |
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope for import | overlaps - a register exported without its reset is an export-fidelity gap inside the staged program this issue tracks; no single task closes #59 |
| 61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | informs - `CellValidator` already accepts `$dff`/`$dlatch`; the import side will need a reset-bearing flip-flop to realize, and this task defines what it realizes *to* |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | depends on - a drawn CPU with no honest reset has no defined instruction 0, which is where TASK-0073's comparator first diverges |

## Notes

- **The `type` attribute's setter leaves an unknown string unchanged**
  (`src/jls/elem/Register.java:378-390`, with the comment "unknown strings leave
  the type unchanged, as the handwritten loader did"). The two new string
  attributes must **not** copy that idiom - an unknown `reset` or `rstpol` value
  is a load diagnostic. Copying it would put a fresh hole into exactly the
  surface TASK-0003 is closing.
- **`docs/simulation-semantics.md` §8.1 is normative and its worked derivation
  must still hold**: clock `cycle 20, one 10` first rises at t=10; a pff with
  delay 5 samples D=5 and Q becomes 5 at t=15, which is what the first golden
  asserts. Reset absent, nothing about that changes - state that in the doc edit
  so a reader knows the default is untouched.
- **`RegisterStatement`'s fields are `public final` and read in three places**:
  both emitters' `visit` methods
  (`src/jls/hdl/VerilogEmitter.java:197`, `src/jls/hdl/VhdlEmitter.java:230`)
  and `VhdlEmitter`'s name reservation (`:1014-1015`,
  `claim(((RegisterStatement) statement).regName)`). A reset net name must go
  through `HdlNames` (`src/jls/hdl/HdlNames.java`) or it can collide with a user
  net.
- **The unconnected-clock path already warns and emits a comment**
  (`src/jls/hdl/HdlExporter.java:640-644`;
  `src/jls/hdl/VerilogEmitter.java:355-360`: "no clock connected: ... holds its
  initial value"). An async reset with no clock is meaningful and must not
  inherit that comment path.
- **`ShiftRegister` is not a register** despite the name - it is a combinational
  barrel shift (`src/jls/hdl/HdlModel.java:830-834`). It gets no reset.
- **`RegisterFile` has a shared clock and no reset** either
  (`src/jls/elem/RegisterFile.java:154,460-472`, `initSim` zeroes every word).
  Widening it is TASK-0074's content-initialization work, not this task's;
  do not do half of it here.

## Evidence

- `src/jls/elem/Register.java:26-27` (the element and its capability
  interfaces), `:199-266` (`init`, four orientation arms), `:272-395`
  (`OWN_ATTRIBUTES`, including the `init`/`type`/`delay`/`watch` entries and the
  unknown-string-is-silent setter at `:378-390`), `:419-425` (`save`),
  `:441-448` (`copy`, indexing two inputs and two outputs), `:693-698` (the
  simulation fields), `:719-737` (`initSim`), `:747-820` (`react`, with the
  three type arms at `:762-796` and the throw arm at `:815-817`).
- `docs/simulation-semantics.md` §8.1 - the normative register semantics, the
  four pinning `SequentialGoldenTest` methods, and the worked derivation.
- `src/jls/hdl/HdlModel.java:401-461` - `RegisterStatement`, its `Kind` enum and
  its one constructor; `:830-834` - the `ShiftRegister` note.
- `src/jls/hdl/HdlExporter.java:632-668` - the register walk; the clock at
  `:640`, the no-clock warning at `:642-643`, the `Kind` switch at `:647-660`,
  and the D operand at `:665`.
- `src/jls/hdl/VerilogEmitter.java:318-360` - the emitted `reg` declaration,
  the three clock kinds, and the unconnected-clock comment.
- `src/jls/hdl/VhdlEmitter.java:230,399,1014-1015` - the visitor, the process
  emitter and the name claim.
- `test/resources/hdl/register_pff.v`, `register_nff.*`, `register_latch.*` -
  the byte-compared goldens that must not move (67 files in that directory).
- `docs/parity-contract.md` §5.1 and `docs/virtual-hardware-parity.md` P16 -
  sync point zero: two machines can agree from instruction 1 and disagree at
  instruction 0 unless the power-on state is specified.
