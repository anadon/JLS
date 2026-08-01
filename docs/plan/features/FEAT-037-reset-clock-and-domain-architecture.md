# FEAT-037 - Reset semantics, clock and domain architecture

**Status:** proposed | **Cost:** 13-18 mw | **Owner program:** P13 |
**Spine rank:** -

## Capability delivered

A register has an honest reset - synchronous or asynchronous, with declared
polarity - that exports as a reset rather than as an initial value, so a drawn
machine comes out of reset the way a real one does and so the most common
sequential idiom a student writes stops being on the export policy's reject
list. Clocks stop being anonymous: a design declares its clock domains, the
domains travel through the HDL intermediate representation, and every crossing
between them is reported.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | required | A drawn CPU without honest reset does not come out of reset |
| CAP-03 | required | A drawn CPU without honest reset does not come out of reset |
| CAP-05 | beneficial | A populated board needs an honest power-on reset, not an initial value |
| CAP-07 | required | The shuttle wrapper makes `rst_n` mandatory and ASIC synthesis discards initial values |
| CAP-08 | required | A third-party core is reset-driven; an initial value is not the same thing |
| CAP-15 | required | The most common sequential idiom students write is on the validator's reject list for want of a reset model |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-004 | A clock domain is a property of a net, and a crossing is a pair of nets in different domains. Both require the shared partition pass and stable net naming; done against the exporter's private union-find, domain assignment would be a fourth copy of the partition |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0077 | Honest reset on the register element | The reset itself: mode, polarity, and honest export |
| TASK-0078 | Clock domains and crossing checks | Domains declared, carried through the IR, and every unsynchronized crossing reported |

## Acceptance criteria

1. A register carries a reset mode - none, synchronous or asynchronous - and a
   declared polarity, and both round-trip through save and load.
2. A reset register exports to Verilog and VHDL as a reset, and the exported
   text compiles under the external compilers already armed in CI.
3. A file written before this feature loads with reset mode "none" and
   simulates byte-identically to HEAD.
4. Clock domains are declarable on a design, carried on nets through the IR,
   and reported in the exported model.
5. Every net driven from one domain and sampled in another is reported as a
   crossing, with the two domains and the element named, and a two-flop
   synchronizer chain is recognized and not reported.
6. The crossing report is a batch analysis with machine-readable output, not a
   dialog.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | Reset semantics, clock domains and crossing checks | **no issue** |
| 199 | Memory: optional synchronous (clock-edge) write mode for glitch-safe RAM in combinational datapaths | informs, **closed** - the same class of change on the memory element; its `sync` attribute is the precedent for adding a clocking attribute without a format epoch |
| 125 | Register creation defaults to Latch; the fork changed the default to the rising-edge D flip-flop for classroom use | informs, **closed** - the recorded discussion of the register's clocking defaults this feature extends |

## Design notes

The register at HEAD has three triggering modes - transparent latch,
positive-edge and negative-edge flip-flop - and an initial value. It has no
reset. An initial value is not a reset: ASIC synthesis discards it, a shuttle
wrapper's mandatory `rst_n` has nothing to bind to, and an imported core that
is reset-driven has no counterpart to map onto. That is the gap, and it is the
reason this feature is required by five capstones rather than being a nicety.

Adding an attribute to an existing element type does not need a format version
bump - the format's own specification says unknown attribute names are ignored
by older readers. The real hazard is the opposite of a version conflict: silent
loss of the reset mode when an older reader opens the file. That hazard belongs
to FEAT-002's fail-loud discipline and FEAT-013's epoch policy, and the task
must say which one it relies on rather than assuming a bump protects it.

The crossing check in criterion 5 is the half with the larger cost, because
recognizing a synchronizer by pattern is a graph query over the partition, and
because a check that over-reports is a check that gets turned off.

## Risks

- **Reset polarity conventions differ by target.** A design that is correct for
  an FPGA flow and wrong for the shuttle wrapper is the failure mode; the
  polarity must be explicit at the element, not inferred at export.
- **Crossing checks produce false positives on teaching circuits.** A first-year
  circuit with one clock will generate no findings; a two-clock lab exercise
  can generate many. Suppression must be per-crossing and recorded.
- **Domains are a new declaration surface** and therefore a new thing to get
  wrong in the file format, in the editor and in the IR at once.

## Evidence

- The register at HEAD has no reset: `src/jls/elem/Register.java:26-40` - the
  `Type` enum is exactly `Latch`, `PosFF`, `NegFF`; the initial value is applied
  at `:165` and reloaded at `:320-324`.
- The clock element and its palette registration:
  `src/jls/elem/Clock.java:26`, `src/jls/edit/Palette.java`.
- The attribute-compatibility rule this feature relies on and its hazard:
  `docs/file-format.md`.
- Owner: P13 in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 13-18 mw; TASK-0077 and TASK-0078 total 3.5 wk.
  The two tasks are the register attribute and the crossing check; the band
  prices carrying domains through the IR, both emitters, the exporter's policy
  and the golden corpus behind them. No task id names that residual. Do not read
  3.5 wk as the feature.
