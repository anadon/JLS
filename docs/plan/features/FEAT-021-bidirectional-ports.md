# FEAT-021 - Bidirectional ports in the IR and the element vocabulary

**Status:** proposed | **Cost:** 2-4 mw | **Owner program:** P3 |
**Spine rank:** -

## Capability delivered

JLS can say that a wire goes both ways. A third direction exists in the
intermediate representation, every emitter either renders it or refuses it
explicitly, and a bidirectional pin element exists whose readback reflects what
is actually on the net rather than what this circuit last drove. Six external
formats need this to be expressible at all, an imported design with a bus stops
being refused at its module boundary, and a board or shuttle design gains the
only port kind real hardware uses most.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-05 | required | a PCB netlist declares pin types, and a bidirectional pin declared as an output is a wrong netlist that passes |
| CAP-07 | required | the shuttle wrapper's fixed signature includes bidirectional pins with an output-enable vector; a first submission can tie them off, a second cannot |
| CAP-13 | required | KiCad's pin type vocabulary includes bidirectional and JLS cannot currently say it |
| CAP-15 | required | a synthesizable module with a bus is refused at import today, which is a parity gap on the commonest real interface |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-027 | beneficial - the intermediate-representation half lands without it, but a bidirectional pin that cannot express contention or an open-drain driver is honest only about direction |
| FEAT-026 | beneficial - honest readback on a shared net is a resolution question, and resolution is that feature's fold |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0049 | Bidirectional ports end to end | the direction member, every emitter's handling or explicit refusal, and the pin element with honest readback are one change |

## Acceptance criteria

- The intermediate representation's direction set has three members. Every
  emitter, and every consumer of the direction set, either renders the third or
  refuses it with a reason naming the emitter - no emitter silently treats it as
  an input or an output.
- A bidirectional pin element exists, is drawable, and reads back the resolved
  value on the net rather than the value this element last drove. A test asserts
  the difference on a net with a second driver.
- A netlist whose module declares a bidirectional port imports, rather than
  being refused at the port scan.
- Both HDL emitters render the direction, and the emitted artifact compiles
  under the external compilers in the existing CI legs.
- The board constraint emitters carry the direction into the constraint file
  where the target format expresses it, and refuse where it does not.

## Related GitHub issues

| # | title | relationship |
|---|---|---|
| - | (no issue) the direction gap has no dedicated issue | no issue |
| #59 | HDL interoperability: staged VHDL/Verilog support (export first, Yosys-netlist import second); SystemC out of scope | informs - export honesty about port direction is inside its staging, but the issue does not name it |
| #63 | HDL Stage 3: black-box HDL component - hand-written header scanner for ports, external GHDL/Icarus co-simulation | informs - the scanned-port model already has the third direction; the export model does not, and that asymmetry is the measurable form of the gap |
| #264 | Board on-ramp: per-board pin constraints + scripted bitstream handoff, end to end (consolidates #213 + #215) | overlaps - a board pin binding wants a direction and today can only be told two of three |

## Design notes

The asymmetry at HEAD is the cheapest possible statement of what is missing:
the scan side, which reads foreign HDL and describes its ports, already models
three directions. The export side, which describes JLS's own ports, models two.
JLS can already *describe* a bidirectional port it did not write and cannot
*declare* one it did.

Split the work deliberately, because the two halves have different owners and
different risks. The intermediate-representation half is one enum member plus
every switch over it plus the emitters' handling, and it is bounded at one to two
weeks. The element half - a drawable pin whose readback is honest - is the part
that touches the value domain, and it is the part that should not ship dishonest.
An emitter that can declare a direction is useful before the element exists,
because import and board binding both consume the declaration.

Sequence note that is a real dependency rather than a preference: the shuttle
path does not need this on its critical path for a first submission, because the
fixed wrapper's bidirectional vector can be tied to all-inputs. It becomes
critical for the second design that uses the pins, and for every board and PCB
target immediately.

The three-way switch must be made total in the same change. A direction enum
with three members and a two-arm switch somewhere is exactly the silent-drop
shape FEAT-001 exists to prevent.

## Risks

- **Honest readback is where this gets expensive.** Reading the net rather than
  the driver is a simulation-semantics change, not an emitter change, and it
  interacts with resolution order. Scoping the element half behind the
  representation half keeps the cheap part cheap; merging them buys the
  representation at the value core's price.
- **Partial honesty is worse than refusal.** An emitter that renders the
  direction into a format whose semantics JLS cannot back - drive strength, for
  instance - produces an artifact that looks like it carries information it does
  not. Refuse and say why.
- **Under-scoped fixtures.** The interesting cases are two drivers and a
  turnaround, not a single bidirectional pin driven by one element. The fixture
  corpus must contain contention.

## Evidence

- Verified at HEAD `addc6c5`: `src/jls/hdl/HdlModel.java:27-33` defines the
  direction enum with exactly two members, `INPUT` and `OUTPUT`.
- Verified at HEAD: `src/jls/hdl/scan/ScannedPort.java` (52 lines) models three
  directions including the bidirectional case - the read side already has what
  the write side lacks.
- Verified at HEAD: `src/jls/hdl/imp/NetlistImporter.java:186` refuses an
  imported module port with the message that it "is an inout (tri-state bus)".
- `09-format-adoption-plan.md` §3 leverage table row 4 records this as the
  highest necessary-leverage item in the study: necessary for six formats at
  1-2 maintainer-weeks for the representation half, with the element half and
  honest readback owned separately.
- `09-format-adoption-plan.md` §4.3 records that a first shuttle submission can
  tie the bidirectional vector off, so this is not on that capstone's critical
  path for a first attempt.
- Cost band basis: the 1-2 wk representation half plus a bounded element
  increment, at the repository's ~200-250 shipped-and-tested lines per
  maintainer-week calibration.
- Do not restate: `docs/simulation-semantics.md` owns the value domain and what
  a driver may assert; `docs/standards-adoption/06-fpga-constraint-formats.md`
  owns the constraint formats' own direction vocabularies.
- **Cost reconciliation.** Band 2-4 mw. Tasks named for it: TASK-0049,
  totalling 2 wk. Band and task sum agree; no reconciliation is needed. Shared
  tasks counted once at the task level: TASK-0049.
