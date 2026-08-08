# Issue #457: TASK-0076: a byte-lane write mask on Memory, so a drawn core does a sub-word store in one cycle instead of a read, a merge and a write
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its apparatus, the claim is: *a drawn processor should be able to
express `sb`/`sh` at its real cost*. Everything else — the `lanes` attribute, the
`WM` pin, the third record component, the partial-lane rule — is mechanism in
service of that. #364 states the same thing as its integration criterion 1: "a
sub-word store completes in one clock, in a real circuit, asserted by a
simulation golden rather than by inspection of the element."

That criterion is the real target, and it is worth noticing that it is stated
about a *circuit*, not about `Memory`. The issue never asks whether the circuit
can already meet it.

## Finding 1 — the enabling claim is false, and that changes the economics

§ Intended Audience says flatly: "A guest that does `sb`/`sh` against a 32-bit
word **cannot be modeled without this**." It can, today, with elements that
already exist, and the drawn RV32 core already contains the exact pattern.

- `docs/simulation-semantics.md` §8.4: with #199 synchronous write, a RAM's
  write commits **only on a rising clock edge**, while reads and the tri-state
  output are "unchanged in both modes". So during one clock a circuit may read
  the addressed word combinationally *and* commit a new one at the edge.
- `riscv/README.md` (Register file): "each register has a hold/load `Mux` on its
  `D` input so unselected registers keep their value." That is a write mask,
  drawn, at register granularity — the core already solves this problem once.
- Applying it at byte granularity is a `Splitter` on the memory output, a
  `Splitter` on the store data, four 8-bit 2:1 `Mux`es selected by the lane
  enables, and a `Binder` back into the data input. Roughly ten elements, all
  of which `riscv/build_cpu.py` already emits. No loop hazard: the feedback
  passes through the sync-write edge exactly as the register file's hold mux
  passes through its flip-flop.

The cost of the drawn route is one extra access-time on the store path's
critical path, plus drawing effort. The cost of the element route is permanent:
a saved attribute, a conditional pin, a widened kernel record, a merge helper,
a sentinel, a ROM diagnostic, a new one-string-two-surfaces width rule
(O10), two normative-doc edits, a §9 silent-drop caveat, and a mode
cross-product the issue itself budgets corner tests for (P8).

Two consequences follow, and they point in opposite directions:

1. **The drawn version is HDL-exportable today; the element version is
   exportable never-until-#291.** `HdlExporter` rejects `Memory` outright
   (`src/jls/hdl/HdlExporter.java:88`). A merge network of splitters, muxes and
   binders exports now, which puts it on the #59/#213/#215 bitstream trajectory
   the roadmap actually funds. The `WM` pin adds an obligation to a surface that
   currently refuses the element entirely.
2. **But a real SRAM has byte enables, and CAP-08 imports cores that assume
   them.** A student modeling an SRAM should see `BE` pins, not a merge network
   they had to invent; and an imported core's store path should be *realized*,
   not re-drawn. That is a genuine fidelity argument the drawn route cannot
   make. It is a weaker argument than "cannot be modeled", and the plan should
   say so honestly.

**Reframing R1 (I am disregarding the stated sequencing, and #364 §6's
"convention rather than necessity"): draw it first.** Land the merge-network
fixture and its golden *before* the element work. It satisfies #364 integration
criterion 1 immediately with zero format change; it gives #202/#326 a working
sub-word store path now; and it becomes a **differential oracle** for the
element mask — masked-`Memory` vs. drawn-merge must agree bit-for-bit on the
same stimulus. That is a far stronger acceptance test than P1's single vector,
and it is the same discipline `ARCHITECTURE.md` records for any future
simulation strategy ("agree bit-for-bit with the #202 RV32I integration golden
as a differential oracle").

## Finding 2 — the format hardcodes 8, and you cannot take that back

`int lanes 1` is a boolean whose meaning contains a magic number. Every
consequence the issue then has to legislate flows from that: Open Question 1
(what if `bits % 8 != 0`), Open Question 4 (lane bit order), §7.10's partial-top-
lane paragraph, §7.11's second failure mode, P9.

**Reframing R2: save the lane *width*, not a flag — `int lanewidth 8`.** `WM` is
`ceil(bits/w)` wide; lane *i* covers bits `[i*w, min(bits,(i+1)*w))`, stated once
for all *w*. Open Question 1 dissolves (a partial top lane is defined uniformly,
and `bits % w != 0` needs no refusal). Open Question 4 dissolves (order is stated
by the same formula). `w = 1` gives a per-bit write mask — the primitive form,
where `M(m) = m` and §7.10's expansion disappears — and `w = 4` gives nibble
lanes for BCD memories. Implementation cost is identical: the expansion loop is
parameterized by `w` instead of by a literal `8`. Format cost is identical.
This is the difference between a file format that can express one machine's
convention and one that can express memories generally, and it is free at
filing time and impossible after files exist.

## Finding 3 — the payload seam is one component too shallow

§7.4 widens `SimEvent.MemoryWrite(int, BitSet)` to `(int, BitSet, BitSet)` and
argues the blast radius is one deconstruction site. True, and irrelevant to the
real question: this is the **kernel's sealed `Payload` hierarchy** (`SimEvent.java:23`,
"adding a payload kind is a compile error at every consumer"), and `Memory` is
about to gain three more modes in the same feature (#439's byte budget, #445's
bulk image) plus whatever #291 needs. Each future latched control value is
another arity change to a type in `jls.sim` that fifteen files name.

**Reframing R3: widen once, with an element-owned type.** Make it
`MemoryWrite(int address, Memory.Write w)` where `Memory.Write` is a record in
`jls.elem` carrying data, mask, and whatever comes next. The precedent is in the
same file: `StateChanged(State state)` already imports `jls.elem.State` into the
kernel payload. After this, memory-write attributes cost nothing in `jls.sim`
forever, and the sealed exhaustiveness that #95 bought is untouched. Same
one-site edit, permanently instead of once.

## Finding 4 — `Memory` is becoming a mode lattice, and #78 was supposed to fix that

At close of #364, `Memory` carries RAM/ROM x sync x lanes x dense/sparse x
{inline, RLE, bulk} image. The issue's P8 tests "at least the corners" of one
2x2 slice; the next task will add its own. Nobody owns the lattice.

The elegant cut is available: `ElementRegistry`/`ElementType` shipped (#78), and
`docs/grand-architecture.md` §4 makes element *types* the canonical unit of the
module model. A byte-enabled RAM could be a registered element type sharing a
base class rather than the fifth flag on one class — at which point §7.11's
first failure mode (`lanes` on a ROM) does not exist, because a ROM is not that
type, and P5 has nothing to test. I am not asking #457 to pay for that: the
"honest list" in `ARCHITECTURE.md` still says sixteen places for a new element,
which is precisely the tax #78 was meant to remove and has not yet. But the
count belongs in #364 as a recorded mode matrix with one owner, so the *next*
Memory mode is the trigger to split the class rather than the fifth flag on it.

## What I would keep verbatim

- **H2, the post-time latch.** Correct and non-obvious, and the reasoning
  generalizes: any control value consumed at `now + accessTime` must be latched
  with the address. Keep P4 as the trap test.
- **H3, merge-before-`put`.** Also correct for a reason the issue does not
  state: two writes to the same address in flight within `accessTime` serialize
  correctly only if each merges against the store at *its own* completion.
  Merging at post time would clobber. Worth adding as a test.
- **`null` mask means all-ones.** #364 calls this the single most likely silent
  failure and it is right.

## Verdict

**endorse-with-reframing.** The capability belongs in JLS and is cheap; the
justification for it is overstated in a way that has already inflated the
mechanism. Do R1 first (drawn merge fixture as the golden and the differential
oracle), then land the element with R2 (`lanewidth`, not a hardcoded 8) and R3
(element-owned payload type), and hand the mode-lattice count to #364. Open
Questions 1 and 4 should not be answered as posed — R2 dissolves both.
