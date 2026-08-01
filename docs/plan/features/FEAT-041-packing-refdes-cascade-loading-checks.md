# FEAT-041 - Packing, refdes, cascade and electrical loading checks

**Status:** proposed | **Cost:** 5-8 mw | **Owner program:** P5 |
**Spine rank:** S9, S16, S17

## Capability delivered

A drawn design becomes a buildable parts list. Every logic element is assigned
to a physical package and a section within it, in an order that is a pure
function of circuit content, so the same design always produces the same
reference designators. Word-level elements that no single part realizes are
decomposed into physical slices and chained by an explicit cascade rule, with
the inter-slice connections appearing as real nets in the shared netlist IR
rather than as a private convention inside one emitter. The result is reported
as a bill of materials, a point-to-point wiring list, and two checks a person
would otherwise do by hand on a datasheet: is any input left floating, and does
any driver drive more loads than it can.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-04 | required | the build plan - "6x 74LS04, U3 U9 U14; 35 packages, 3 unused sections" and "U3.1 -- U7.11 net BUS0" - is this feature's output |
| CAP-05 | required | the netlist's component records are packing output; an 8-bit adder emitted as one component is not manufacturable |
| CAP-13 | required | the importing tool needs refdes and component records that are stable across regenerations, or every re-import churns the board |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-040 | Packing reads pin maps, section counts, gate equivalence and loading figures that only the package library creates. There is nothing to pack into until it exists |
| FEAT-004 | The cascade rule creates synthetic inter-slice nets, and a wiring list names nets. Both require a netlist IR that exists outside the HDL exporter and net names keyed off stable ids rather than load-order integers |

The loading check is deliberately **not** dependent on FEAT-027. It is arithmetic
over datasheet figures - unit loads in, unit loads out - and it is worth having
before the simulator models drive strength, because it catches the static error
class (a fan-out of 14 on a part rated for 10) that no amount of simulation
surfaces.

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0086 | Packing, refdes, BOM and wiring list | The packing pass itself and the three reports it emits |
| TASK-0087 | Width decomposition and the cascade rule | The rule that turns one word-wide element into N physical slices and the synthetic nets that chain them |
| TASK-0088 | Fan-out and DC loading check | The loading analysis, reported as a batch report with its own vocabulary |
| TASK-0078 | Clock domains and crossing checks | Shared with FEAT-037: a decomposed element's clock is distributed across packages on a real board, and the crossing report is the analysis that names a path the schematic hid |
| TASK-0093 | Breadboard consistency check and physical binding | Shared with FEAT-043 and FEAT-027: the placed physical arrangement is checked against the same packing binding this feature produces |

## Acceptance criteria

1. **Packing totality.** Every logic element maps to exactly one (refdes,
   package, section), no section is double-booked, and every element that no
   part can realize appears in an explicit unbound list with the reason - never
   silently omitted. Errors are aggregated per `PinBindings.parse`'s idiom.
2. **Refdes purity.** Reference designators are a pure function of circuit
   content in canonical stable-id order. Running the pass twice produces
   byte-identical output; running it on a machine with a different replica id
   produces the same output.
3. **Diff stability.** Inserting one unrelated gate and regenerating produces an
   **additive-only** diff of the BOM, the refdes map and the wiring list.
4. **Cascade correctness.** A word-wide element wider than any single part
   decomposes into slices; the carry or chain pins bind slice *i* to slice
   *i+1*; the chain is terminated explicitly at both ends; and the synthetic
   inter-slice nets are present in the netlist IR, visible to every consumer, not
   invented inside an emitter.
5. **Partition equivalence.** The partition over (refdes, pin) induced by the
   packed result equals the source `WireNet` partition pushed through the
   packing binding, computed by union-find, for every fixture.
6. **Every pin accounted for.** All pins of a placed package - including supply
   pins and the inputs of unused sections - appear in exactly one net or in an
   explicit no-connect set. Unused-section inputs are **tied, not omitted**.
7. **Loading check vocabulary.** Per net, the sum of sink unit loads is compared
   against the weakest driver's capacity. A family for which the DC check is
   vacuous reports *"not DC-limited"* - never *"PASS"*.
8. **Floating-input detection is static.** Every input pin of every used section
   is in a net with at least one driver or one pull; violations are reported
   before any simulation runs, and therefore before FEAT-027 exists.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | packing, refdes, the BOM and wiring list, the cascade rule, the loading check | **no issue** |

No open issue touches the physical program. Recorded rather than left blank.

## Design notes

**The cascade rule is the item that was not priced anywhere before, and it
changes an interface.** JLS elements are word-level: a `Adder` is one element of
arbitrary width whose only concession to physical reality is
`propDelay = bits * defaultPropDelay` (`src/jls/elem/Adder.java:261`). A 74LS83
is four bits. An 8-bit adder is two cascaded parts; a 32-bit adder is eight. The
consequence is that the netlist emitter is **not** a pure projection of the
`WireNet` partition - it emits nets that exist in no `.jls` file - and that fact
must be in the IR from the start. Retrofitting it around a finished emitter is
how the emitter acquires a private net namespace.

**The honest boundary, stated as arithmetic rather than as refusal.** Elements
that decompose into cascadable through-hole parts: `Adder`, `Register`,
`ShiftRegister`, `Mux`, `Decoder`, the gates, `Constant`, `TriState`. Elements
that do not decompose at any width: `Memory`, `RegisterFile`, `TruthTable`,
`StateMachine`, `FieldExtend`, `SigGen`, `TestGen`, `Display`, and `SubCircuit`
(which flattens) - nine of the 35 registered types (`ElementRegistry.java:38-76`,
verified at `addc6c5`). Two of those nine, `Memory` and `RegisterFile`, are
exactly what makes JLS good at CPUs. The mechanism that closes the gap is a
user-supplied part binding: a `Memory` bound to a 62256 with its own pin map is a
perfectly good library row. The default library cannot carry every such row; the
binding mechanism must exist so a course can.

Packing is a first-fit over sections in canonical order, not an optimizer.
Minimizing package count is a nice-to-have that would make refdes assignment
depend on a search; determinism is worth more than one spare 74LS04.

## Risks

- **Refdes churn.** If refdes depends on anything other than stable-id order -
  load order, hash order, packing search order - then every regeneration
  reshuffles the board and the student re-labels 35 parts. This is the single
  most damaging failure mode and criterion 2 is its ratchet.
- **Synthetic nets leaking.** If the cascade's inter-slice nets are named in a
  scheme that can collide with user net names, a design that happens to use the
  name loses a connection silently. Reserve a prefix and test the collision.
- **Checks that are confidently wrong.** A fan-out check reading transcribed
  datasheet figures inherits every transcription error in FEAT-040. Report the
  source of each figure in the analysis output so a disagreement is traceable.
- **Scope drift into placement.** Packing decides *which part*; it must not
  start deciding *where on the board*. That is CAP-04's canvas and the board
  tool's job.

## Evidence

- Spine rows and bands: S9 fan-out / DC loading check (1-2 wk, package data,
  explicitly not P1), S16 packing + refdes + BOM + wiring list (2-3 wk), S17
  width decomposition / cascade rule (2-3 wk): `10-capstone-plan.md:593-620`.
- The build-plan acceptance test with its seven assertions, including
  netlist-partition equivalence, power completeness, floating-input detection,
  contention, fan-out and the additive-only diff:
  `10-capstone-plan.md:396-427` (AT-C4-A).
- The netlist-side checks - packing totality, every pin bound, partition
  round-trip, determinism, diff stability: `10-capstone-plan.md:499-520`.
- The cascade finding and its consequence for the IR, with the 74LS83 width
  arithmetic and the nine non-decomposable types:
  `10-capstone-plan.md:545-576`.
- Word-level width as it exists at HEAD: `src/jls/elem/Adder.java:261`
  (`propDelay = bits * defaultPropDelay`); 35 registered types at
  `src/jls/elem/ElementRegistry.java:38-76`.
- Stable-id ordering already exists and is tested:
  `src/jls/Circuit.java:479-485`, seeded in that order by
  `src/jls/sim/Simulator.java:151,196`.
- The error-aggregation idiom: `src/jls/hdl/board/PinBindings.java:36-55`.
