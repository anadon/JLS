# TASK-0062 - N-ary interop: lowering, waveform manifest and test grammar

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0061

## Deliverable

A higher-radix design leaves JLS through the paths every other design already
uses, with the radix declared rather than implied.

1. **HDL lowering, classified by policy, not by accident.** Every new N-ary
   class from TASK-0061 gets an explicit row in one of `HdlExporter`'s four
   class sets (`src/jls/hdl/HdlExporter.java:429-435` `EXPORTED`, `:438-441`
   `SKIPPED`, `:443-445` `TOPOLOGY`, and `REJECTED` at `:460`). The
   lowering is **binary-encoded ternary (BET)**: each radix-3 digit becomes a
   2-bit binary field, each operator becomes its BET truth table, and the
   emitted module declares the encoding in a header comment. Any class the
   maintainer chooses not to lower goes in the refused set with the reason a
   user can act on - that bucket exists at HEAD precisely so refusal is a policy
   decision (`HdlExporter.java:448-460`, the comment says so in those words). The single
   `throw new HdlExportException` at `:201` is the one exit; nothing new throws.
2. **A radix manifest in the waveform dump.** `BatchSimulator.writeVcd`
   (`src/jls/sim/BatchSimulator.java:359-369`, header written at `:421-437`)
   already emits exactly one comment line,
   `$comment JLS batch simulation trace $end` at `:422`. A second `$comment`
   line follows it listing every non-binary signal as `name radix digits
   encoding`. VCD is a binary format and its `b<bits>` vectors stay binary
   (`vcdValue`, `:538-555`); the manifest is what lets a reader interpret them.
   **A circuit with no non-binary signal emits no manifest line and its VCD is
   byte-identical to today** - that is the acceptance criterion, not a hope.
3. **`-t` test-vector grammar extension.** `SigSim.initSim`
   (`src/jls/elem/SigSim.java:40-90`) already runs a token-rewrite pre-pass that
   converts `0x`-prefixed hex tokens to base 10 before the real parse at `:78+`.
   Radix tokens (`0t` for balanced ternary, `0q` for base 4) are three more
   branches in the same `matches(...)` chain at `:52`. No new parser.
4. **Balanced rendering.** A `-0+` formatter for balanced-ternary values in
   `Display`, the trace pane and `BitSetUtils.toDisplay`
   (`src/jls/BitSetUtils.java:237+`). `BitSetUtils.ToString(bs, radix)`
   (`:83-92`) is already radix-general via `BigInteger.toString(radix)` and is
   reused; `ToStringSigned` (`:103-118`) must **not** be reached from a balanced
   port - it reads two's complement and balanced ternary has no sign bit.
5. **Batch stdout unchanged.** `docs/batch-interface.md` §3.2's watched-element
   whitelist is `Register`, `Memory`, `OutputPin` and is part of the stability
   contract. The new types are outside it, so `-b` output does not change and no
   new flag is needed for the default path.

Done means: a drawn ternary datapath exports, dumps a VCD a third-party viewer
opens, and is testable with `-t` vectors - and a binary circuit's bytes are
unchanged on all three paths.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-029 | The "and its interop" half. Without it an N-ary design is a thing you can draw and cannot get out, test, or show anyone. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0061 | The policy tables are keyed on concrete element classes (`EXPORTED.contains(el.getClass())`). There is nothing to classify, lower, or declare in a manifest until the classes exist. |

## Acceptance test

- **`jls.hdl.HdlPolicyTest`** (existing at `test/jls/hdl/HdlPolicyTest.java`,
  added by commit `b54e6ee` "make the export policy total over the element
  registry"):
  extend so it fails until **every** newly registered N-ary class appears in
  exactly one of the four sets. This is FEAT-001's totality discipline reused,
  not a new mechanism.
- **`jls.hdl.BetLoweringGoldenTest.ternaryMinLowersToTheDeclaredBetTable()`**
  (new): export a two-input `MvlGate(MIN)` and assert the emitted Verilog is
  byte-equal to a committed golden, and that the golden's header declares the
  2-bit-per-digit encoding.
- **`jls.VcdExportGoldenTest.binaryCircuitVcdIsByteIdenticalAfterTheManifest()`**
  (extend the existing class, `test/jls/VcdExportGoldenTest.java`): re-run
  `clockedRegisterVcdMatchesGoldenByteForByte` and
  `testVectorStimulusVcdMatchesGoldenAndCoversHiZ` unchanged; both must stay
  byte-identical.
- **`jls.VcdExportGoldenTest.ternarySignalsAppearInTheRadixManifest()`** (new
  method in the same class): assert the second `$comment` line names each
  non-binary signal with its radix, digit count and encoding, and assert the
  `$var` widths are the *binary* widths.
- **`jls.elem.SigSimRadixTokenTest.balancedTernaryTokensParseToTheSameEventsAsDecimal()`**
  (new): assert `0t+0-` and its decimal equivalent post the identical event
  sequence, and assert an existing hex-token stimulus file is unaffected.
- **`jls.BatchSimulationGoldenTest`** (existing, all methods): byte-identical -
  the watched-element whitelist did not change.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second) | overlaps - this task adds classes to the export policy that #59's staging owns; it does not close #59 (the registry records #59 as a tracking issue no single item closes) |
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs - a BET-lowered ternary module is a plausible first black-box consumer; no dependency either way |

**No issue** exists for the radix manifest, the balanced formatter, or the
`-t` grammar extension. Recorded as a gap.

## Notes

- **Trap: `HdlExporter` classifies by exact class membership.**
  `EXPORTED.contains(el.getClass())` (`:429-435`) is `Set.of` over 22 classes, so
  a subclass of an exported class is **not** exported and a new class is refused
  by default. That is the fail-closed property TASK-0061 relies on; do not
  "improve" it into an `isAssignableFrom` walk while adding rows.
- **Trap: the VCD header is byte-pinned deliberately.** `BatchSimulator`'s
  comment at `:419-421` records that `$date`/`$version` are omitted so the same
  run always produces the same bytes. Adding an unconditional manifest line would
  break every VCD golden in the tree. The line must be conditional on a
  non-binary signal existing.
- **Trap: `SigSim`'s pre-pass is string concatenation in a loop.**
  `src/jls/elem/SigSim.java:44-75` builds `newLine` and `newSignals` with `+=`
  inside two nested loops - the quadratic stimulus parse TASK-0009 fixes and the
  largest `byte[]` allocator in the whole run (BRIEF §13). Adding two more token
  branches here makes it worse. Either sequence this after TASK-0009 or add the
  branches to whatever TASK-0009 leaves behind; do not extend the concatenation.
- **Trap: `vcdValue` reconstructs the HiZ marker.**
  `src/jls/sim/BatchSimulator.java:538-555` builds `new BitSet(bits + 1)` and
  sets bit `bits` to test for HiZ. Under a radix-3 signal the "bits" it is given
  must be the *binary* width, not the digit count - this is exactly the class of
  silent mis-sizing TASK-0059's loud `getBits()` shim exists to catch. Give
  `vcdValue` the explicit binary-only guard rather than trusting the caller.
- **Balanced ternary rendering is the cheapest, highest-visibility item in the
  whole N-ary program.** Ship it early in the task, not last.
- **Honest framing obligation for any user-facing page.** Setun (1958, ~50
  machines to 1965) was halted for administrative reasons, and the radix-economy
  argument descends from a 1950 cost model whose own source disclaimed itself.
  The honest sentence is "ternary was never given a fair industrial trial, *and*
  sixty years of device work has not produced a win either" - both halves.

## Evidence

- `src/jls/hdl/HdlExporter.java:429-435, 438-441, 443-445` - the three positive
  policy sets; `:201` - the single `HdlExportException` throw site.
- `src/jls/sim/BatchSimulator.java:359-369` (`writeVcd`), `:419-437` (the header
  and its byte-determinism comment), `:422` (the one existing `$comment` line),
  `:538-555` (`vcdValue` and the HiZ marker reconstruction).
- `src/jls/elem/SigSim.java:40-90` - the token-rewrite pre-pass, with the hex
  branch at `:52` that the radix branches join.
- `src/jls/BitSetUtils.java:83-92` (`ToString(bs, radix)`, radix-general),
  `:103-118` (`ToStringSigned`, binary-only), `:237+` (`toDisplay`).
- `docs/batch-interface.md` §3.2 - the three-class watched-element whitelist that
  keeps `-b` output unchanged.
- `git show --stat b54e6ee` - `src/jls/hdl/HdlExporter.java` +60 and
  `test/jls/hdl/HdlPolicyTest.java` (38 lines, new): the totality test this task
  extends.
- `07-mvl-determination.md` §1.1 stage 4 (3-4 weeks: BET-lowered export, VCD
  `$comment` manifest, `-t` grammar through the existing pre-pass, balanced
  rendering, doc rewrite).
