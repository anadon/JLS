# Issue #604: TASK-C332-4: a cut that crosses a combinational cycle is refused by name at partition time instead of simulated to a different answer
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two things, and they are not the same thing.

1. **Do not silently give a different answer.** A partitioned design must not
   simulate to something other than its single-file self. That is a real and
   important commitment.
2. **Do not let #606's equivalence harness pass vacuously.** The issue says this
   explicitly ("without it, the equivalence harness can pass vacuously on designs
   that never exercise a boundary").

Goal 1 is worth serving. Goal 2 is served by something else entirely, and the
conflation is the reason this task is shaped the way it is.

## The claim that does not survive contact with the tree

The Boundary notes assert: *"The combinational dependency graph this reads is the
existing one; this task does not introduce a second cycle analysis."*

There is no such graph. `Util.partition` / `Circuit.finishLoad:1354` "partition"
means grouping `WireEnd`s into `WireNet`s — a connectivity walk, not a dependency
graph, and it is a different sense of the word than #332's. The only structure in
the tree that mentions combinational cycles is `jls.hdl.layout.LayoutGraph`, whose
`Edge.feedback` flag is a **caller-supplied boolean** passed into `connect(...)`
(`src/jls/hdl/layout/LayoutGraph.java:231-241`); `HeuristicLayeredLayouter` merely
skips those edges to keep its layered problem acyclic (`:154`, `:169`, `:273`).
Nothing computes an SCC, a topological order, or a loop. `jls/core/` is eight
files and all eight are geometry.

So this task does not read an existing analysis — it introduces the project's
**first** combinational dependency graph, and it introduces it inside a
partitioner, behind a feature that is blocked on four other features. That is the
single most consequential fact about #604 and the issue is written as if it were
not happening.

## Where the project already decided this question, the other way

`docs/capability-roadmap/lf-02-compiled-evaluation.md` §2.4 is titled, verbatim:
**"Feedback that cannot be levelized — partition, do not refuse."** Its argument:

> "The alternative — 'refuse to compile any circuit containing a loop, fall back to
> the event engine' — is simpler and wrong, because the SR latch built from two
> cross-coupled NAND gates is a first-year lab ... It is also wrong for the
> `riscv/` GUI CPU, which `riscv/gui/README.md` describes as *'wired into two
> feedback loops'*."

The same document places the loop detector where it belongs: a Tarjan SCC pass in
`jls.core`'s graph layer, shared with the static-timing DAG (P7-A, "the levelizer's
DAG, its topological rank and its SCC partition *are* P4's timing DAG and its
combinational-loop detector"). #604 builds that analysis in the wrong place, for
the narrowest possible consumer, and uses it to say no to exactly the circuits
lf-02 says must keep working.

Note also what refusal costs at the user's end. A cross-coupled NAND latch or a
gated D-latch straddling a cut is *not* an uncuttable construct in JLS's
semantics — it is a legal circuit that the author must not be told is illegal
because of where a file boundary fell.

## Reframing 1 — the real hazard is zero total delay, not "a cycle"

JLS's own semantics dissolve most of the stated problem. `docs/simulation-semantics.md`
§6.1: "Wires are ideal ... all delay in JLS lives in elements, none in wiring." A
cut is a cut through a *net*, so a cut adds and removes no time. §6.2: gates carry
transport delay at `now + propDelay`. A combinational cycle through any delayed
element is therefore not a fixpoint problem at all — it is a ring oscillator whose
events advance monotonically in time, and a conservative cross-partition barrier
has positive lookahead across every such cut. The classic reason distributed
simulators refuse combinational loops (zero-lookahead Chandy–Misra deadlock) does
not apply to the case #604 names.

The case that *is* dangerous is the **zero-delay** cycle: §6.2 lists `Splitter`,
`Binder`, `InputPin`, `OutputPin`, `SubCircuit`, `Constant` as zero-delay, and
`Gate.setDelay` (`src/jls/elem/Gate.java:551-553`) and the `"delay"` `IntAttribute`
(`:316-327`) accept `0` with no validation anywhere. A cycle with zero total delay
posts events forever at one timestamp — **today, in the single-file editor, as a
silent hang.** That is a shipping defect, it is partition-independent, and lf-02
§2.4 already prescribes the diagnostic ("report an oscillation at that named set of
elements — a far better diagnostic than today's silent hang").

Rebuild the outcome on that seam:

- **`jls.core` gains a combinational-graph + SCC pass** (the one lf-02 wants
  anyway), and JLS names zero-delay loops instead of hanging. This helps every
  student today, on the single-file path, with no partitioner in sight.
- **The partition refusal narrows to one line**: refuse a cut only when a cycle
  crossing it has zero total delay — which is the same predicate, evaluated on the
  same graph, with no partition-specific analysis at all.
- A delayed cycle spanning parts is *not* refused. It is a correctness obligation
  on the transport (#333), and stating it that way is what makes #333 testable.

I am explicitly disregarding AC-1 and AC-3 as written. AC-1 refuses the wrong set;
AC-3's "the check is on cycles that span, not on cycles" is the exact inversion —
the property that matters is intrinsic to the cycle (its delay), not to where the
file boundary happens to fall.

## Reframing 2 — the anti-vacuity guard is coverage, not refusal

A green refusal test does not stop #606 from comparing two runs that never moved a
byte across a boundary. These are unrelated propositions, and AC-4 ("armed before
the harness is trusted") formalizes the non-sequitur into an ordering constraint.
What actually falsifies a vacuous pass:

- **Boundary traffic is a measured output.** The harness asserts `boundary events
  transported > 0` — better, records the count, the way #332's I1 records the
  memory number rather than asserting a bound.
- **A differential mutation oracle.** Perturb one part (flip a gate, change a
  delay) and require the harness to *notice*. The project already lives this way:
  `riscv/fuzz_diff.py` is a randomized differential oracle against an independent
  RV32I emulator, and lf-02 schedules "P7-E engine-vs-engine differential harness
  — written alongside P7-B, not after."

That is one or two days of test work inside #606, and it retires #604's stated
purpose without a partitioner refusal existing at all.

## Reframing 3 — cut at subcircuit ports and the question stops being asked

JLS already has an authored decomposition with named ports: `SubCircuit` /
`Circuit.setImported`, saved as nested `CIRCUIT … ENDCIRCUIT` blocks written once
and referenced (`Circuit.java:1478-1483`). If a *part* is an externalized imported
subcircuit, then #600's part-file set is a small change to a reference already in
the format, #602's boundary net identity is free (a port name is the identity, and
it is already independent of which side you read it from), and the boundary
description is **derived rather than authored** — deleting an entire class of
author error that #604 exists to catch. You cannot cut through a gate, so every cut
is a port set, and the only remaining question is Reframing 1's delay predicate.

## Does this strengthen the project's arc?

Honestly: the arc it belongs to is itself unwitnessed. The largest `.jls` in the
tree is `test/fixtures/riscv-sum1to10.jls` at 120 KB — 0.2% of the 64 MiB cap the
capacity capstone exists to remove — and keystone C measures the RV32I CPU at 522
evaluation slots and 128 nets. No document under `docs/` describes distributed
simulation at all; the measured roadmap argues the opposite route (value domain
−15…−25%, Mode T −30…−40%, Mode C an order of magnitude, all single-process),
while `ARCHITECTURE.md:341-368` records the interpreter as the sole strategy
because CPU-scale designs are not yet common. A multi-process partitioned
simulator is a much larger bet than the levelized pass the project has already
declined as premature. #604 is a well-made brick in a wall the tree's own measured
documents argue against building.

## Verdict

**rethink.** Keep the commitment (never simulate a partitioned design to a
different answer); discard the mechanism and its justification. Concretely: move
the SCC analysis into `jls.core` where lf-02 wants it and cash it in *today* as a
named zero-delay-oscillation diagnostic on the single-file path; narrow any
partition refusal to zero-total-delay cycles; move the anti-vacuity guard into
#606 as boundary-traffic coverage plus a mutation oracle; and reconsider #600/#602
against subcircuit ports as the natural cut seam. If #312 stays unwitnessed, the
right disposition is **redirect**: land the oscillation diagnostic on its own
merits and let the partitioner wait for a design that needs it.
