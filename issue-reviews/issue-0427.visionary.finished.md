# Issue #427: TASK-0087: an 8-bit adder becomes two cascaded parts with one synthetic carry net in the IR, not one component no factory can build
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the apparatus and the claim is: **a JLS element is word-level, a real part is
not, and every downstream physical artifact is wrong until something reconciles
them.** That claim is true, it is load-bearing for #365/#329/#366, and no other
issue makes it. Keep the finding, keep the slot in the sequence, keep the two-week
budget.

What I am rejecting is the answer: a new net *kind* (`SyntheticNet`) inside a new
physical netlist IR, populated by a `ceil(w/k)` rule read from a tuple-shaped
library row. That design is a decoration of a netlist. The thing it is trying to
express is a **circuit** — and JLS can already draw, save, simulate and render
that circuit today, with no new IR and no new record kind.

## The reframing: lower the design, do not decorate the netlist

`jls.pkg.Decomposer` should be `Circuit -> Circuit`, not `(Partition, PartBinding)
-> PhysicalNetlist`. An 8-bit `Adder` becomes, in a *derived* circuit, two 4-bit
`Adder` elements plus `Splitter`/`Binder` for the bit ranges, with slice 0's `Cout`
wired to slice 1's `Cin` by an **ordinary wire**.

This is not hypothetical. `src/jls/elem/Adder.java:105-107` already mints
`Input("Cin", …, 1)` and `Output("Cout", …, 1)` on every adder — the carry chain
is an existing, first-class, one-bit put. `Splitter` and `Binder` exist.
`riscv/jlsbuild.py` is a committed, tested proof that a JLS circuit is a netlist
you can synthesize programmatically ("geometry is irrelevant to simulation").
`docs/capability-roadmap/lf-01-parameterization.md:37-45` states the seam outright:
*"JLS's hierarchy is elaborated-by-copy already … elaboration is an existing
implicit phase that has never had inputs."* Width decomposition is that phase with
`(width, sliceWidth)` as its input. The project is already walking toward this door.

What the reframing dissolves, in order:

- **`PhysicalNet` as a sum type, and `SyntheticNet` entirely.** The carry between
  slices is a `WireNet` in the derived circuit. §7.1's "surface change that must
  not be retrofitted later" is a surface that never needs to exist.
- **Open Question 2 (declared execution-blocking).** There is no synthetic
  namespace, so nothing can collide with a schematic name. P3 evaporates.
- **P4, P6, P8.** Names come from `TASK-0008`'s stable-id keying on ordinary nets;
  provenance attaches to *elements* (`derived element -> (sourceStableId,
  sliceIndex)`), which is where #394's refdes assignment wants it anyway, not to
  nets. The conservation law is replaced by something far stronger — below.
- **P5's residue plumbing.** Unused bits on the last slice are tied by a real
  `Constant` element to a real net. "Tied, not omitted" stops being an assertion
  and becomes structurally impossible to violate; the tie shows up in the wiring
  list because it is a wire.
- **The refusal bucket (P7, O6).** Lowering has an identity case. A `Memory` bound
  to a 62256 lowers to itself, one slice, and the only refusal left is "no part
  binding", which #394's unbound list already owns per #365 §1 clause 1. Two
  registry-keyed totality tables with subtly different buckets become one.
- **The central claim of §7.12 clause 2 and #365 §2** — "the netlist emitter is not
  a pure projection of the `WireNet` partition" — **stops being true.** It is only
  true because decomposition was placed *after* the schematic. Put it before, and
  the emitter is a pure projection again: of a different circuit. One partition
  type suffices, which makes the `#336`/TASK-0007 dependency exactly right rather
  than "over-strong".

And it buys something the issue cannot: **the decomposed design is simulatable.**

## Why that matters more than everything else here

§9's oracle is *counting nets*. §11 then lists three risks the counts cannot touch:
slice ordering silently read big-endian, a residue tied to the wrong rail, a family
substituted after slices exist. A count of 7 carries for a 32-bit adder is equally
satisfied by a chain wired backwards, by slice 3 bound to bits [8,12), and by a
carry-out fed into the wrong slice's carry-in. **P1 as specified passes on a board
that does not work** — which is precisely the failure the issue exists to prevent.

Lower to a circuit and the oracle becomes differential: run the drawn 8-bit adder
and its lowered 2x4 cascade under `BatchSimulator` against the same `-t` vectors
and require bit-identical outputs. That is this project's own established
correctness idiom — `riscv/fuzz_diff.py` differentially fuzzes the CPU against a
reference emulator, and ARCHITECTURE.md's recorded #221 decision makes bit-for-bit
agreement with the RV32I golden *binding on any future evaluation strategy*. A
counting law is a weaker equivalence than this repository accepts anywhere else it
has two representations of one thing. Compare `CircuitSnapshot`
(ARCHITECTURE.md:106-111): undo does not get a parallel snapshot format with a
conservation law, it reuses save/load so undo semantics *are* save/load semantics.
Decomposition should reuse the circuit model so board semantics *are* circuit
semantics.

## H1 is already refuted, by the issue's own §7.1 table

The issue nominates carry-look-ahead as H1's refutation candidate. It does not have
to wait: three of the seven rows it proposes break the `ceil(w/k)` + one-chain-net
model at filing time, checkable against the tree.

- **`Decoder` -> 74LS138/139.** `src/jls/elem/Decoder.java:183-206` — one `input`
  of `bits`, one `output` of `1<<bits`. A 5-bit JLS decoder is 32 outputs and needs
  **four** '138s plus a '139 decoding the top two address bits into the enables.
  `ceil(5/3) = 2`. The slice count is wrong, and the inter-slice structure is not a
  chain at all: it is high-order bits fanning into enable pins, plus polarity glue
  ('138 outputs are active-low; JLS's are a one-hot bus).
- **`Mux` -> 74LS153/151.** `src/jls/elem/Mux.java:147-168` — a JLS mux has *two*
  independent size axes, `bits` (data width) and the input count. A '153 is a dual
  4:1 one bit wide. Widening slices along `bits`; going past 4:1 slices along the
  select axis and needs enable decoding plus output combining. A scalar
  `sliceWidth` cannot see the second axis.
- **`ShiftRegister` -> 74LS194.** The chain is serial-in/serial-out and
  *direction-dependent*: a bidirectional '194 cascade has two chain nets per
  boundary plus shared mode control, not `s-1` single nets.

The pattern: **the row that fits the model is `Adder`, and the model was
generalized from it.** The honest general form of a decomposition rule is not a
tuple, it is a small parameterized circuit template — enable decoding, polarity
inverters, output combining and residue ties are *elements*, and elements have no
representation in `(width, sliceWidth, carry role, termination)`. §10's prescribed
next move ("extend the `Cascade` record in #400, do not special-case it in the
decomposer") walks that record straight toward being an ad-hoc circuit description
language. Better to admit at the outset that the row *is* a circuit generator and
let it be written in the representation JLS already has. "Adding a cascadable part
is a library row, not code" survives the reframing — the row is just a template
instead of a tuple.

## What survives unchanged, and what still needs a physical IR

I am not claiming the physical netlist disappears. Package-level facts — Vcc/GND
pins, section occupancy, no-connects, refdes — are not expressible as circuit
elements and stay in #394's layer. The change is that that layer becomes a
**pure projection of the lowered circuit's ordinary partition**, plus power and
no-connects, instead of a partition-plus-a-second-net-kind. §7.6's little-endian
declaration, §7.9's purity, the aggregated-error idiom, and P9's structure-not-
timing header all carry over verbatim. The ordering against #394 (§7.12 clause 5)
gets *easier*: packing runs over the lowered circuit and never needs to know that
lowering happened.

One deliberate note on P9. Refusing to publish timing is right today. But a lowered
circuit made of real slice-width parts is the only artifact in either plan that can
*later* accept per-part datasheet delays — `docs/capability-roadmap/sweep-02-timing.md:233`
names exactly that lesson ("this 74LS00 has t_PLH = 15 ns transcribed straight from
a datasheet into the drawing"). A plan-only IR with `timing = ⊥` has nowhere to put
them and will have to be rebuilt to get them. The reframing keeps that door open at
no cost.

## Disregarded acceptance criteria, named

I am explicitly setting aside: the DoD line "**`SyntheticNet` is in the netlist
IR**", §7.4's `PhysicalNet` sum type, P3, P4, P6, P8, Open Question 2, and the
`ceil(w/k)` arithmetic of §7.10 as the general rule. Reason: each is a cost paid to
maintain a second representation of connectivity that JLS does not need, and the
strongest of them (P8) is a weaker check than a differential simulation the repo
can run today. The replacements are: a `Circuit -> Circuit` lowering pass; a
derived-element provenance map to source stable ids; a differential-simulation
equivalence test per decomposed fixture; and decomposition rows expressed as
circuit templates.

## Concrete next move

File the reframing as a comment on #365 before #400 freezes the `Cascade` record's
shape — the record is the coupling point, and #400 is `blocked_by` this issue's
own dependency edge. If the maintainer disagrees and keeps the netlist-decoration
design, the minimum change I would still press for is replacing P1's count
assertions with a lowered-circuit differential run, because a correct count on a
backwards carry chain is the exact defect this task was written to stop.
