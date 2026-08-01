# FEAT-020 - Yosys JSON read: mapper parity with the validator

**Status:** proposed | **Cost:** 4-8 mw | **Owner program:** P3 |
**Spine rank:** -

## Capability delivered

An imported netlist runs. The gap between what the importer's validator accepts
and what its mapper can actually realize closes, so sequential cells, memories,
tri-state drivers and word-level arithmetic become drawn JLS elements rather
than reported problems, bit slices and concatenations become splitter and binder
meshes instead of a wholesale refusal, and a multi-module netlist keeps its
module structure instead of being refused. The practical result is that a
synthesizable design written in an HDL, put through the external front end,
opens in JLS as a circuit that simulates.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-02 | beneficial | a soft core sourced as HDL is a candidate path to a drawn machine, and word-level import is the only tractable one |
| CAP-08 | required | this capstone *is* importing a third-party core and running its own test program |
| CAP-15 | required | parity means reading what the toolchain writes, for the cells the toolchain actually emits |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-021 | a bidirectional module port is refused at import today; a core with a bus cannot be imported without the direction existing end to end |
| FEAT-026 | beneficial - tri-state realization is honest only where the value domain carries the high-impedance and unknown states the cell implies |
| FEAT-036 | beneficial - realizing a memory cell with independent read and write ports needs the memory element's byte-lane and port work |
| FEAT-037 | beneficial - the asynchronous-reset flip-flop family is the commonest idiom students write and is currently on the validator's reject list pending honest reset |
| FEAT-017 | beneficial - hierarchy instances realize as subcircuit instances, and shared definitions are what stop N instances becoming N deep copies |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0047 | Realize sequential, memory and arithmetic cells on import | the mapper increments themselves, in roughly one-week slices |
| TASK-0048 | Realize hierarchy instances on import | multi-module netlists are refused at selection today |

## Acceptance criteria

- Every cell type the validator accepts is realized by the mapper, or is listed
  in a written, tested exclusion set with the reason. "Accepted but not
  realized" is not a valid state for a cell type.
- Bit-level slices and concatenations in the netlist's connection vectors are
  realized as splitter and binder meshes rather than refused wholesale.
- A multi-module netlist imports with its hierarchy intact, each instance
  becoming a subcircuit instance.
- The committed test that currently asserts a flip-flop cell is *rejected* is
  flipped to assert realization, in the same change that realizes it, so the
  reversal is visible in review rather than silent.
- An imported circuit simulates: for at least one published open-source core,
  the imported design runs a program and its output matches the reference.
- No partial circuit is ever emitted. A design containing an unrealizable cell
  is refused with a report naming every problem, which is the behavior the
  current importer already guarantees and which must survive the increments.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| #61 | HDL Stage 2: import synthesizable Verilog via external Yosys JSON netlists (restricted cell pipeline) | closes - this feature is the remaining realization half of that issue |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope | tracking - the staged parent; no single feature closes it |
| #62 | HDL Stage 2 companion: schematic auto-layout for imported netlists (heuristic layered layout; ELK only out-of-process) | depends on - realized cells must be placed to be readable; see FEAT-022, whose in-process half already ships |
| #63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | overlaps - the alternative for a module JLS cannot realize is to keep it external rather than to import it |

## Design notes

The single most important framing correction, verified this session: the gap has
**moved from validation to realization**. The validator already accepts nineteen
cell types including flip-flops, latches, tri-state drivers, wide multiplexers,
addition and both memory forms with a constrained port shape. The mapper
realizes five. The remaining work is therefore one-sided - a mapper increment
per cell family, with acceptance criteria already written on the issue - rather
than a design problem.

The import pipeline is deliberately word-level-preserving: no flattening, no
technology mapping, memories left unmapped. That choice is load-bearing and must
not be relaxed for convenience. A gate-mapped import of the same design was
costed at roughly four times the element count and four times the wall clock
against a word-mapped one; choosing the wrong input level is a permanent tax on
every simulation of every imported design.

The bit-mesh increment gates several of the others and should be scheduled
first: real designs slice constantly, and today any slice is refused for the
whole design.

The private builder inside the importer must be promoted to a shared class
before a second importer exists, or the migration importer (FEAT-025) will
duplicate it.

## Risks

- **Realization without semantics is worse than refusal.** A tri-state cell
  realized against a two-state value domain, or an asynchronous reset realized
  against an element with no reset, produces a circuit that runs and is wrong.
  Where the prerequisite feature has not landed, the cell stays on the exclusion
  list with the reason, and the exclusion list is under a ratchet.
- **A large imported design tests the engine, not the importer.** A core-sized
  netlist becomes a core-sized JLS circuit, and the capacity and performance
  limits it hits belong to other features. Import success is not run success and
  should not be reported as it.
- **Version coupling.** The netlist shape follows the external front end's
  releases. Pin the version in CI and record it in the import report.

## Evidence

- Verified at HEAD `addc6c5`: `src/jls/hdl/yosys/CellValidator.java:60-68`
  accepts nineteen cell types; `:114` accepts both memory forms and `:124-125`
  their port cells. `src/jls/hdl/imp/NetlistImporter.java:235-247` realizes
  **five**: the four word-level bitwise gates and the two-way multiplexer.
- Verified at HEAD: `NetlistImporter.java:35-47` documents the scope explicitly -
  the accepted-but-unrealized cells, bit-level slices and concatenations, and
  width mismatches "are all reported as import problems rather than silently
  mis-mapped: no partial circuit is ever emitted".
- Verified at HEAD: `NetlistImporter.java:186` refuses a bidirectional module
  port; `:125-161` (`selectModule`) refuses a multi-module netlist even though
  the validator accepts hierarchy instances; `:410` is the private builder that
  a second importer would need.
- Verified at HEAD: the reject-side decision on the asynchronous flip-flop
  family is at `CellValidator.java:144-149`, recorded 2026-07-17, and is
  revisited by FEAT-037 rather than assumed overtaken.
- Cost band basis: `09-format-adoption-plan.md` W5.2 (mapper increments, 5-7 wk
  in one-week slices), W1.5 (bit mesh, 2-3 wk) and W5.3 (hierarchy import,
  2-3 wk), scoped down for the increments this feature owns.
- Do not restate: `docs/hdl-support-research.md` owns the staged import plan and
  the restricted-cell rationale; issue #61 owns the per-cell acceptance
  criteria.
