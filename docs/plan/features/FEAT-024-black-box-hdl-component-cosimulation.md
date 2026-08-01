# FEAT-024 - Black-box HDL component and external co-simulation

**Status:** proposed | **Cost:** 8-14 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A JLS circuit can instantiate a module whose body is a Verilog or VHDL source
file JLS does not read, drawn as an element with the ports the file declares,
and can run that instance by handing the boundary to an external simulator. The
inbound direction that FEAT-020's importer cannot serve - code JLS will never
realize as elements - stops being a wall and becomes a boundary with a stated
contract.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-08 | beneficial | The parts of a core that cannot be realized can still run, in the external simulator |
| CAP-09 | beneficial | The part of the design that cannot be read stays in the tool that can run it |
| CAP-15 | beneficial | The inbound direction for code JLS will never realize as elements |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-021 | The scanner already reports `INOUT` ports and nothing downstream can carry one; a black-box element that silently drops a bidirectional port is worse than one that refuses it, and refusing every real bus module makes the feature vacuous |
| FEAT-023 | Co-simulation means a second process running a real toolchain. The toolchains must be installed, pinned and exercised in CI before a contract can be tested against them |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0053 | Black-box HDL component and its co-simulation contract | The element, the harness and the forward-only conversation rule. The port-scanner half of the same issue already ships and is not this task's work |

## Acceptance criteria

1. A black-box element placed in a circuit exposes exactly the ports the
   scanned source declares, with their widths and directions, and refuses by
   name any direction it cannot carry.
2. The source file is referenced, not absorbed: editing the source and
   rescanning updates the element's ports and reports every port that changed.
3. The conversation with the external simulator is forward-only and stated as
   such. No rollback of committed simulation time is required or attempted,
   because none exists in the engine.
4. A circuit containing a black-box instance runs to completion in batch mode
   against a named external simulator, with the co-simulation transcript
   recorded and comparable across runs.
5. When the external simulator is absent, the circuit still loads and reports
   the missing dependency by name rather than failing to parse.
6. Nothing in the contract makes the external simulator a build dependency of
   JLS: the required check passes without it, and the co-simulation check runs
   in the toolchain lane.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | closes - but see the design note: its scanner half already ships |
| 216 | Waveform + verification interop: document the VCD to GTKWave/Surfer handoff and provide a batch-CLI autograde-bridge example (live co-sim explicitly out per #63) | informs, **closed** - it records that *live* co-simulation was rejected; the batch, forward-only conversation here is a different thing and the distinction must stay explicit |

## Design notes

Half of #63 has shipped and this feature must be scoped to the other half.
`src/jls/hdl/scan/` contains `VerilogHeaderScanner`, `VhdlEntityScanner`,
`ScannedModule` and `ScannedPort` with tests. What it does not have is a
consumer: no package outside `jls.hdl.scan` reads it. The remaining work is the
element that holds a scanned module, the lifecycle that keeps it in step with
its source, and the co-simulation contract. The registry's 8-14 mw band was set
before the scanner landed and should be re-measured against that fact.

Criterion 3 is the load-bearing constraint and it is not a preference. There is
no rollback machinery in the simulation engine, so any protocol that requires
un-committing time is out of scope by construction rather than by taste.

The adjacency to the recorded rejection in #216 needs stating in the document
that ships, not only here: a batch differential conversation is close enough in
shape to live co-simulation to be mistaken for it, and the maintainer should
confirm the distinction rather than inherit it from an evidence corpus.

## Risks

- **Two simulators, two notions of a delta cycle.** The boundary semantics
  between JLS's event loop and an HDL simulator's scheduler are the whole
  difficulty; get them wrong and results are plausible and unreproducible.
- **Process lifetime.** A long batch run holding an external process open is a
  resource-management problem the tree has no precedent for.
- **The band predates the shipped scanner** and is therefore an overestimate of
  unknown size. Re-measure before funding.

## Evidence

- The shipped scanner half: `src/jls/hdl/scan/VerilogHeaderScanner.java`,
  `src/jls/hdl/scan/VhdlEntityScanner.java`, `ScannedModule.java`,
  `ScannedPort.java`; no consumer outside that package.
- The recorded rejection of live co-simulation and the batch handoff that
  replaced it: `docs/vcd-interop.md`; issue #216, closed.
- No rollback machinery in the engine: `src/jls/sim/` contains no cancel,
  withdraw or rollback path.
- Owner: **UNOWNED** in `docs/capability-roadmap/`.
- **Cost reconciliation.** Band 8-14 mw; TASK-0053 is 2 wk. The single task is
  the element and the contract; the band prices the co-simulation harness
  against two external simulators and their failure taxonomy, which no task id
  names. Do not read 2 wk as the feature, and re-measure the band against the
  shipped scanner before funding it.
