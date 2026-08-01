# TASK-0068 - The console element

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0067

## Deliverable

The minimal serial element a guest kernel actually drives: three byte addresses,
polled, no interrupt controller.

1. **`src/jls/elem/Console.java`** - an in-tree `LogicElement` subclass
   implementing `Watchable`, `Editable`, `Timed`. Pins created in `init` on the
   `Memory` pattern (`src/jls/elem/Memory.java:181-202`): `address`, `input`,
   `WE`, `OE`, `CS`, and a tri-state `output`. There is **no `irq` output**, and
   that absence is a tested property, not an omission.

2. **The decode**, exactly the measured 16550 subset
   (`docs/machine-calibration.md` §5.3, measured on `cnlohr/mini-rv32ima`):
   - offset `0x0`, write - THR: emit the byte through `sim.hostPort().emit`.
   - offset `0x0`, read - RBR: the next received byte if one is available,
     otherwise `0`.
   - offset `0x5`, read - LSR: `0x60 | data_ready`.
   - every other offset in the `0x100` window: write ignored, read `0`.
   `irq = 0` is load-bearing: `8250_core.c` falls back to a kernel timer when a
   port has no hardware interrupt, and that is what removes the PLIC from the
   minimum SoC.

3. **The receive path self-schedules.** `initSim` clears the receive state and
   posts a first poll event; `react` re-posts at `now + pollPeriod`, the idiom
   `Clock.initSim`/`Clock.react` already use
   (`src/jls/elem/Clock.java:384-394,404`). `pollPeriod` is a saved `Timed`
   delay so it is visible and tunable. **Do not mint a new `SimEvent.Payload`
   record for it** - reuse `PinChanged`; see Notes.

4. **The registration ritual, complete.** `ARCHITECTURE.md:115-147` is the
   checklist; its opening sentence "There is no element registry yet" is stale
   at HEAD. The files, from the `38a0544` precedent plus what `970db41` added:
   `src/jls/elem/ElementRegistry.java:38-77` (one `ElementType` row),
   `src/jls/elem/LogicElement.java:17-21` (append to `permits`, no reordering),
   `src/jls/elem/SaveTags.java:41-76` (the `WRITABLE` row),
   `docs/file-format.md` §7 (the frozen tag table),
   `src/jls/collab/op/ElementVocabulary.java` (the network allowlist token),
   `src/jls/edit/Palette.java` (palette entry + grid),
   `src/jls/edit/images/` (toolbar icon), `resources/help/elements/**`,
   `resources/help/Map.jhm`, `resources/help/JLSHelpTOC.xml`; test-side
   `AllElementsRoundTripTest`, `CircuitTextBuilder`, `ElementDrawSmokeTest`,
   `ElementSimulationGoldenTest` (`COVERED`, `:516-526`), `PaletteContractTest`,
   `CapabilityInterfaceTest`, `SealedHierarchyTest`, `ComponentIdentityTest`,
   `FileFormatSpecTest`.

5. **Zero `FORMAT` version cost.** `docs/file-format.md` §7 tags are frozen data
   and a new tag is additive; no existing reader sees it. State that in the
   commit message so the next author does not bump the header defensively.

6. **`docs/simulation-semantics.md` gains a `Console` subsection** alongside §8.4
   (Memory writes), stating the decode, the polled receive, and that the element
   is a no-op whenever the granted port is `NullPort`.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-032 | The element a person or a guest kernel actually talks to; the seam without it drives nothing. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0067 | The host byte port seam | `Console.react` calls `sim.hostPort()`. The accessor, the sealed port type and the ring drain do not exist before that task; `jls.elem` may not open a host handle itself, which the same task's ratchet enforces. |

TASK-0076 (memory byte lanes) is **not** a prerequisite: this element decodes
its own address pins and does not ride a `Memory`. The byte-lane dependency in
the roadmap is about the guest *bus*, not about this element.

## Acceptance test

`test/jls/elem/ConsoleModelTest` (new, in the `MemoryModelTest` idiom -
`test/jls/elem/MemoryModelTest.java:377-432` is the closest existing shape):

- `lsrNeverReadsAllOnes()` - over every reachable `data_ready` state, the LSR
  byte has at least one low bit. The 8250 driver treats an all-ones LSR as a
  missing port and disables it; `0x60 | data_ready` satisfies this by
  construction, and the test is what keeps a later refactor from breaking it.
- `writeToTransmitHoldingEmitsExactlyOneByte()` - against a `PipePort`, one
  `WE`-asserted react at offset 0 produces exactly one byte, and a repeated
  react with unchanged inputs produces no second byte.
- `receiveBufferReadConsumesTheByteAndClearsDataReady()`.
- `unmappedOffsetsInTheWindowReadZeroAndIgnoreWrites()` - `@ParameterizedTest`
  over offsets `0x1`-`0x4` and `0x6`-`0xff`.
- `consoleHasNoInterruptOutput()` - asserts the element's `Output` names, so an
  `irq` pin added later fails a test rather than silently changing the SoC.

`test/jls/ElementSimulationGoldenTest`: add `"Console"` to `COVERED`
(`:516-526`) and a batch golden driving a write and a read.
`everySimulatingElementHasAGoldenOrARecordedExemption()` (`:549`) fails until
one of `COVERED`/`EXEMPT` names it.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the `Console` element | **no issue** |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - its registry half already shipped and is what makes this a ~65-line registration rather than a reflective-loader edit; this task does not close it |
| 202 | RV32I CPU worked example: integration golden, sample circuit, and HDL-export differential oracle | overlaps - the worked example is the first design whose observable is a byte stream rather than a watched register |
| 214 | In-editor test panel: a GUI front-end over the batch `-t` test-vector engine | informs - grading on emitted bytes is a second consumer of this element |

## Notes

- **`react`'s `switch (todo)` has no `default` arm** in all 27 implementations
  (`src/jls/elem/Register.java:815-817` is the shape: every unhandled payload is
  listed and throws). Adding a `SimEvent.Payload` record for the receive poll
  breaks the compilation of every one of them. Reuse `PinChanged` for the
  self-posted poll; the element distinguishes it from a pin change by its own
  state, exactly as `Memory` distinguishes a completing write from an input
  change by payload type.
- **`ElementRegistryTest.everyWritableRegisteredTagIsInTheFrozenTagTable`**
  (added by `970db41`) fails unless `SaveTags` and `docs/file-format.md` §7 are
  updated in the same commit as `ElementRegistry`.
- **`HelpTopicsTest#everyPaletteElementTypeHasAMappedHelpTopic`** derives its
  element list from `jls.edit.Palette`
  (`test/jls/HelpTopicsTest.java:167-206`), so adding the palette entry without
  the help topic is a build failure - which is the intended order.
- **`jls.elem` is a `HeadlessCoreRatchetTest` core prefix**
  (`test/jls/HeadlessCoreRatchetTest.java:74-79`): `Console.java` must import no
  `java.awt`, no `javax.swing`, no `jls.edit`. All host contact is
  `sim.hostPort()`. The renderer and the dialog are separate `jls.edit` files,
  as `RegisterFile`'s were.
- **`TellUser` is not available on the hot path.** `Memory.initSim` branches on
  `JLSInfo.noWindow()` to choose `System.out` or a dialog
  (`src/jls/elem/Memory.java:1264-1274`); a console must never do either during
  `react`.
- **`jls.elem` floors are 0.730/0.700/0.585** (`pom.xml:475-493`), well under
  `jls.sim`'s. A new element with a thin model test still moves the package
  ratio; run the floor check before proposing a raise.
- Baud, parity, FIFO depth, `IER`/`IIR`/`FCR`/`LCR`/`MCR`/`MSR`/`SCR`: all
  write-ignored, read-zero. The autodetector still concludes
  `ttyS0 ... is a XR16850`. Do not implement them.

## Evidence

- `docs/machine-calibration.md` §5.3 - the three-address UART table, `irq = 0`,
  the all-ones LSR hazard, and the "no PLIC node" device-tree measurement, with
  method (instrumented `mini-rv32ima`, 2026-07).
- `src/jls/elem/Memory.java:140-202` - the pin-creation pattern and its
  index-stability discipline (`clock` appended last for #199, `:193-197`).
- `src/jls/elem/Clock.java:384-394` - the self-scheduling idiom.
- `src/jls/elem/ElementRegistry.java:29-77` - the manual registry and the
  totality contract in its javadoc.
- `src/jls/elem/LogicElement.java:17-21` - the sealed permits list (24 entries).
- `src/jls/elem/SaveTags.java:33-76` - the frozen writable-tag table.
- `test/jls/ElementSimulationGoldenTest.java:516-526` (`COVERED`), `:533-546`
  (`EXEMPT`, with the `RegisterFile` reason), `:549` (the totality test).
- `git show --stat 38a0544` - the 14-file cost of adding two elements
  (`RegisterFile`, `FieldExtend`), the measured registration tax.
- `ARCHITECTURE.md:115-147` - the sixteen-place ritual, with its stale opening
  sentence.
