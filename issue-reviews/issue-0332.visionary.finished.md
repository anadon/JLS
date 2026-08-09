# Issue #332: FEAT-055: a circuit exists as parts that load independently — the single-file ceiling stops being the limit on how large a design can be
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Stripped of the plan-registry apparatus, #332 fuses three separable things:

- **(a) A load path that does not materialize the whole design.** Present value to every
  user; the named defect is real (`src/jls/Circuit.java:1345`, `:1369` — an `O(n)`
  `LinkedList.remove` inside the per-net walk).
- **(b) A new on-disk artifact: N part files plus a boundary description.** Value only to
  #333/#312. Nothing today reads or writes it.
- **(c) A partition-legality checker** (criterion 5: refuse a cross-partition combinational
  cycle by name). Latent general value, currently welded to a partitioner that does not exist.

Only (b) is genuinely new, and (b) is the piece I think is designed at the wrong seam. (a)
belongs to #353 and (c) is deliverable today without any of #332's four blockers.

## The seam is cut in the wrong place

#332 cuts at **the file format**: a design becomes "a set of files plus a boundary
description." That choice buys a second normative format surface — `docs/file-format.md` is
normative, and the project's strongest architectural arc is byte-exact determinism (#111
canonical newlines, #166 canonical save order and stable-id ordering in `Circuit.save`,
reproducible jar/BOM, the golden suites). #332 knows the cost: its own §7 makes "two
representations of one design" a permanent `REPLAN:` trigger. A permanent replanning trigger
declared at design time is a design smell, not a risk register entry.

**Alternative framing — cut at the elaboration source, not the artifact.** Define a
pull-based source: something that yields elements and net memberships incrementally and
declares its ports. Then a single `.jls` file, a set of part files, a Yosys JSON netlist
(#33/#59, `docs/hdl-support-research.md`), and `riscv/build_cpu.py`'s generator are all
*implementations of one interface*, and streaming elaboration is a property of the consumer,
tested once against a synthetic source. Consequences:

- Criterion 3's memory bound (`M(load) ≤ max_i M(D_i) + M(B) + c`) is stated and measured
  over the source. I1 becomes a unit test with a generator, not a multi-file fixture.
- Open decision 2 (per-part vs aggregate on-disk budget) **evaporates**: a source declares a
  budget; there is no file set to smuggle bytes past in aggregate.
- No second format surface, so §7's standing REPLAN trigger never gets armed.
- It converges with the roadmap that will actually produce huge designs. Nobody draws 10^9
  gates; Yosys emits them. #312's own §1 capacity walk-through *generates* the design from a
  deterministic generator with a recorded seed — a source, not a file set. The artifact form
  #332 builds is not what the capstone's own acceptance procedure exercises.

## The part-file set already exists in this codebase, and is called a subcircuit

This is the reframing I would push hardest.

- `SubCircuit.save` (`src/jls/elem/SubCircuit.java:282-289`) writes `super.save(output)` then
  **`getSubCircuit().save(output)`** — the entire nested circuit body, per instance.
- Load (`src/jls/Circuit.java:1007-1023`) builds a **fresh `Circuit` per SubCircuit element**;
  nothing is shared.
- The editor's Import is documented as "Import a **copy** of a subcircuit"
  (`src/jls/edit/SimpleEditor.java:5459-5489`).
- `docs/file-format.md:358-360`: nested blocks recurse, and may contain further `SubCircuit`s.

So a hierarchical design is serialized and resident at its **expanded** size: an ALU
instantiated 8 times costs 8 copies of its text and 8 copies of its heap, and depth
multiplies. That is the real single-file ceiling for any design built the way JLS invites you
to build one — and **#332 does not recover a byte of it.** A partitioned design made of K
copies is still K copies, now spread over files.

The alternative: **subcircuit-by-reference.** `SubCircuit.save` emits a name plus a content
hash instead of a body; load resolves and *shares* one `Circuit` per referenced part. That
alone delivers four of #332's five criteria almost for free:

| #332 criterion | Under subcircuit-by-reference |
|---|---|
| Part-file set + boundary description | A part *is* a file; the boundary description is the subcircuit's `Pin` set — already authored, already persisted, no new artifact. |
| Boundary net identity across a cut | Collapses. The parent wires to named `InputPin`/`OutputPin`; both sides agree by construction, not by a new naming scheme. |
| Streaming elaboration | One part resident at a time, plus deduplication #332 never counts. |
| Partitioned-vs-single-file equivalence | Becomes a round-trip test between inlined and referenced save, on fixtures that already exist. |

The honest objection is that hierarchy cuts are not arbitrary cuts on a flat net graph. But
#332 already chose **author-declared** cuts (open decision 1, option (a)), and no author hand-
declares cuts on a 10^9-element design — the author declares *hierarchy*. The author-declared
case is the hierarchy case. #332's own scoping decision argues for this reframing, not against it.

## Criterion 5 should ship now, and not as a partitioner property

`grep -rn "combinational\|oscillat" src/` returns nothing relevant: JLS has **no** combinational-
cycle analysis. A student who draws an unintended feedback loop today is bounded only by the
simulator's time limit (issue #25). A pass that finds combinational cycles and names the
elements is (i) buildable today, (ii) valuable to every student on the first day, (iii) reused
verbatim by a partitioner later. #332 rightly refuses to push it *downstream* into transport;
I would lift it *upstream* out of the partitioner entirely, where it needs none of
#319/#336/#353/#370 and is not gated behind a capstone that may never land.

## The 64 MiB cap is not a ceiling; it is a bomb guard

`src/jls/FileAbstractor.java:55-65` says so in the comment: "a tiny archive inflating to
gigabytes is treated as hostile, not as a big circuit (issue #38; SECURITY.md documents live
attacks)." #332's Background presents it as "the ceiling this removes," then invariant 7 and
open decision 2 concede the aggregate budget should be "checked and reported rather than
enforced silently." That is a decompression-bomb regression proposed as the recommended
default. The better move, and one streaming elaboration makes natural: replace a fixed
decompressed-size cap with a **ratio-and-rate guard over the stream**. Honest large files stop
hitting a wall; hostile small ones are still refused, and refused *earlier* than today.

## Does the work strengthen the arc, or pull against it?

Pulls against, on the current evidence:

- ARCHITECTURE.md's recorded decision for #221 (lines 341-368) states that classroom-scale
  circuits are the present workload and that capacity/throughput strategy work is "premature
  optimization until CPU-scale designs are actually common," with an explicit revisit trigger:
  a concrete design on the `riscv/` trajectory that is unusably slow. #332 does not claim that
  trigger has fired.
- The repository's own evidence is against it. The largest real design here is
  `test/fixtures/riscv-sum1to10.jls`: **1,038 elements, 120 KB of plain text** (it is not even
  compressed, and contains **zero** `SubCircuit` elements — it is machine-generated and flat).
  `riscv/gui/cpu.jls` is 52 elements. Against #312's numbers that is ~160x below today's
  defect ceiling (~165,000 elements), ~550x below the file cap, and ~10^6 below the target.
- **Undeclared overlap with #353.** #332 lists #353 as a blocker *and* claims
  `src/jls/Circuit.java:1300-1422` under its own §3 "Modifies." Two features scheduled to
  rewrite the same method is a merge conflict planned a year in advance. The dedup comment on
  this issue compared #332 only against #333 and never checked this one.
- The byte-identity claims in I3 rest on cross-platform run determinism that
  `docs/parity-contract.md:469-477` records as **unverified** (per the #333 boundary comment).
  That experiment is cheap and gates I3; it should precede any of this.

## What I would do instead

I am disregarding the five `planned_tasks` scopes as the unit of work, because three of them
exist only to serve an artifact format I think should not be built.

1. **Now, unblocked:** combinational-cycle DRC that names the elements. Ships value on day one;
   becomes criterion 5 later for free.
2. **Now, in #353:** fix `ends.remove(vend)` and make `finishLoad` incremental. Give the load
   path wholly to #353 and delete it from #332's §3 "Modifies."
3. **Now, small:** ratio/rate-based hostile-input guard replacing the flat 64 MiB cap; close
   open decision 2 by making it not a question.
4. **Next:** subcircuit-by-reference with shared resolution and content-hash identity. This is
   the highest capacity-per-line change available anywhere in the capstone, it removes an
   expansion factor #332 does not address, and it makes hierarchical designs diff cleanly —
   which serves the collaboration program (#163) too.
5. **Then, if and only if #312 is still funded:** define the elaboration-source interface and
   restate #332's criteria over it. At that point #332 is a thin issue, not a 10-16 mw one.

Endorsed in end, rebuilt in means: the capability "a design exists as parts that load
independently" is worth having; the route through a bespoke multi-file format and a bespoke
boundary description is the expensive way to reach it, and three of the five scopes disappear
if the seam is moved.
