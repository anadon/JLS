# Issue #394: TASK-0086: a drawn circuit becomes a parts order — refdes keyed on stable id, a BOM, a point-to-point wiring list, and a diff that is additive when you add a gate
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this is actually for

The end is not four text files. The end is the sentence in the Abstract: *a logical
gate is not a chip, and one drawn NAND is a quarter of a package that costs the same
as all four.* That is a real gap in JLS's teaching arc — the model is word-level and
package-blind everywhere (`src/jls/elem/Adder.java` is one element of arbitrary
width), and nothing in the tree tells a student otherwise. Aiming a headless batch
pass at it is aligned with the project's spine: #77's headless-kernel keystone,
`docs/batch-interface.md`'s exit/stability contract, and the #214 precedent that a
GUI fronts a headless engine rather than being one. Packing over `Circuit` rather
than over `HdlModel` is also the right seam, and the issue is right not to say why:
`HdlExporter.EXPORTED` (`src/jls/hdl/HdlExporter.java:422-428`) still rejects
`SubCircuit` and `Memory` (`:88`), so an IR-side packer could not pack the
repository's own flagship RISC-V CPU. Anything physical must walk the element graph.

So the goal is endorsed. The mechanism is where I part company, and it is not a
detail: the load-bearing acceptance criterion rests on an ordering property the code
does not have.

## The flaw the whole design is built on

H2 asserts: *"stable ids are minted monotonically and never reassigned (O3), so a
fresh id appends after every existing element in ≺."* Read the comparator
(`src/jls/elem/ElementId.java:278-285`):

```java
int byReplica = replica.compareTo(other.replica);
if (byReplica != 0) { return byReplica; }
return Long.compare(counter, other.counter);
```

The order is lexicographic on **replica id first**, and the counter is only a
tiebreaker within one replica. Two consequences kill P5 as written:

1. **Legacy circuits.** Elements loaded from a pre-#165 file carry the reserved
   replica `legacy` (`ElementId.java:38`). A freshly minted id carries this install's
   replica — 32 hex digits (`:41-58`). Hex is `[0-9a-f]`; `legacy` begins with `l`.
   Every hex replica sorts **before** `legacy`. Insert one gate into any legacy
   circuit and the new element lands *first* in stable order, renumbering every `U`
   in the file. P5 fails on the most common fixture class JLS has.
2. **Two installs, one circuit.** The same happens whenever the editing replica's id
   sorts below the authoring replica's — a coin flip, per circuit, per contributor.
   FEAT-041 §1 criterion 2 ("a different replica id produces the same output") is
   true for a *fixed file*; it is false across *edits*, and #394 needs the second.

Worse, P5 tests only insertion. The scheme in §7.10 numbers a package by counting
packages of the same prefix ordered before it, so **deletion renumbers everything
downstream**: delete the last occupant of the package that became `U2` and `U3…U35`
all shift down. Deletion is at least as common an edit as insertion, and a student
who has already written `U7` on a chip with a Sharpie is now holding a mislabelled
board. The one test declared load-bearing is the one edit direction under which the
design happens to survive.

## Reframe 1 — annotation is state, not a pure function

This is the reframing that makes the problem disappear. Every real schematic tool
learned it: reference designators are **assigned once and persisted**, not recomputed.
KiCad annotates into the schematic file and offers back-annotation precisely because
the physical board carries the label; a "pure function of circuit content" is
*required* to move designators when content changes, which is exactly the failure
mode invariant 2 of FEAT-041 calls "the single most damaging".

Two concrete routes, either of which retires H1/H2/P5 as properties-to-be-tested and
makes them invariants by construction:

- **(a) Refdes as an `Attribute`.** Mint on first pack, persist through the existing
  `Attribute` save/load plumbing, exactly as `ElementId` itself is persisted. Then no
  ordering assumption exists to break; additivity, deletion-stability, replica
  independence and cross-machine identity are all free. Cost: a `.jls` format change,
  which §7.12 forbids. **I am disregarding that constraint.** JLS already has
  `FORMAT` version negotiation and a refuse-newer path (`Circuit.readFormatHeader`,
  #79); spending one format bump to make the project's stated most-damaging failure
  mode structurally impossible is a good trade, and cheaper now than after 35 chips
  are labelled.
- **(b) `refdes.map` as input as well as output.** If the file exists in the target
  directory, read it, honour every assignment whose stable id still resolves, and
  mint only for new packages. No format change, and it is the same "sticky
  annotation" semantics. It costs the purity claim — the plan becomes a function of
  content *and prior assignment* — which is the honest description of what an
  annotation is.

Either route lets the report state *why* a designator is what it is, which no
recomputation can.

## Reframe 2 — one value with renderers, not four grammars

§7.6 ships four bespoke text grammars and §7.7 enrols all four in
`docs/batch-interface.md` §6's stability promise on day one. That is four permanent
compatibility obligations acquired before a single consumer exists, in a project that
treats format promises seriously enough to require a CHANGELOG entry and a major bump
to move them. The `PackPlan` is already the value TASK-0093 consumes; make it the
*only* durable artifact — one structured document with the library version, the JLS
version and the assignment — and make `refdes.map`, `bom.txt`, `wiring.net` and
`pack.log` renderings over it, promised as *human-readable*, not byte-stable. Then
TASK-0089's PCB netlist and TASK-0093's discrepancy report are two more renderers,
not two more parallel derivations that can disagree — which is the same argument O4
makes about the net partition, applied one level up.

Small but real: `bom` is already taken. `README.md:87` documents `bom.json`, the
CycloneDX **software** bill of materials, one of the two artifacts the project
advertises as byte-reproducible. Shipping `bom.txt` meaning "chips to buy" collides
with the project's own established vocabulary. `parts.txt` costs nothing.

## Reframe 3 — a wiring list is a route, not a relation

§7.10 emits `{(a,b) : a,b ∈ pins ∩ n, a ≺ b}` — the full clique of every net. A
20-pin bus net becomes 190 lines. Nobody wires a breadboard that way: you run a
*chain*, pin to pin, and the artifact a builder wants is a spanning path with a
defined start (usually the driver) and an append-only tail. That is strictly less
output, strictly more buildable, and *more* additive than the clique — adding a pin
appends one line instead of k. It also matters beyond legibility: §11 correctly
notes TASK-0093 inherits this vocabulary, so the clique's O(k²) shape propagates into
the breadboard discrepancy report. The issue's own H4 worry ("is the aggregate
readable on a real circuit?") is answered before it is asked: on the 1038-element
RISC-V CPU, a clique-form wiring list is not an artifact, it is a denial of service.

## Reframe 4 — there is already a physical-binding layer, and this is not in it

`jls.hdl.board` is the existing answer to "bind a drawn design to physical hardware":
`Board` is data-only (`Board.java:1-30`), `PinBindings` is the aggregate-error parse,
`PcfEmitter` is the deterministic renderer, `-board`/`-pins` are the flags, and
`UnbindablePortsTest` is the contract test. #394 proposes a second physical binding —
different package, different flag, different report family, different vocabulary —
that shares every one of those shapes and none of the code. The elegant cut is one
*physical target* concept: a target is data (an FPGA with named pins, or a part
family with sections, pin roles and supplies), binding is user-supplied or derived,
and the renderers hang off the resulting plan. That collapses `-board`, `-pins` and
`-pack` into one surface, gives TASK-0085's library and `Boards` one home, and means
the fifteenth flag row is a target kind rather than a new subsystem. If that is too
much for one task, at minimum put `jls.pkg` next to `jls.hdl.board` under a shared
`jls.phys` and say in the issue why they are siblings, so the second physical
program does not grow a private vocabulary the way §11 fears the emitter would grow a
private net namespace.

## Trajectory check

The project has already adjudicated a competing route to this exact end.
`docs/standards-adoption/11-costed-rejections.md:~498-512` argues the GAL/PLD path is
"the *only* item that closes the loop from drawing to physical hardware without an
FPGA toolchain, a vendor account, or a gigabyte download", buildable for under $100
of class hardware, with a `.jed` file a student can *read*. Neither #394 nor #365
cites it. Both routes are defensible and they teach different lessons (packing
teaches "a gate is a quarter of a chip"; a GAL teaches "logic is fuses"), but the
project is now funding two physical-closure programmes without having compared them.
That comparison belongs in #365, not here — but #394 is where the first money gets
spent, so it should name the sibling route rather than let the reader discover it in
a rejections document.

Also worth naming: `docs/capability-roadmap/sweep-06-physical-boundary.md:571-576`
declines IPC-D-356A on the grounds that "a bare-board test netlist without a board
layout has no consumer". `wiring.net` is the same shape of artifact with a human as
the consumer instead of a fabricator — which is a *good* answer to that objection and
should be stated as one, because it is the argument that says this work sits inside
the boundary the sweep drew rather than across it.

## What I would keep, unchanged

Headless, zero GUI, `Circuit`-side, first-fit-not-optimizer, one shared net partition
(TASK-0007), aggregate-not-first-error diagnostics, the `-parts` escape row as
diagnostic rather than refusal, and the honest boundary census over `ElementRegistry`
rather than a hand-written list. Those are all the project's own established
discipline correctly applied, and none of them is in question.

## What I would disregard

- **P5 as the load-bearing test**, and the §14 line making a changed `U`-number a
  blocking failure. Under reframe 1 it is not a test at all; under the current design
  it is a test that passes only on non-legacy, insertion-only edits.
- **§7.12's "no `.jls` format change"**, if route (a) is taken.
- **§7.7's four grammars joining the stability promise**, in favour of one plan value.
- **Open Question 2's "define U, R, C, J now"**, which is scaffolding for a numbering
  scheme that should not exist in that form once designators are persisted.

## Open design question the issue does not raise

Under H3's recommended flattening, what is the ordering key for an element inside a
twice-instantiated subcircuit? §7.10's `≺` is over `ElementId`, and flattening
produces one refdes per *instance*. The key must be (instance path, stable id), and
the instance path must itself be stable — which is a second identity problem the math
in §7.10 does not have a symbol for. Resolve it before H3 is answered, not after.
