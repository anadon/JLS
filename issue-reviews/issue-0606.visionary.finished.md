# Issue #606: TASK-C332-5: where a design fits both ways, the partitioned form and the single-file form produce byte-identical simulation output
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Of #332's five scopes (#600–#604, #606), this is the only one that is *falsifiable* and the only
one whose value survives if partitioning is descoped. Its claim about what JLS should become is:
**a design's meaning must not depend on how the design was delivered.** That is a good claim, it
is one this project already half-believes, and it deserves to be a project-wide invariant rather
than a fixture set owned by one unbuilt feature.

The stated mechanism, though — commit fixture pairs in both forms, diff stdout, name the first
differing byte — cuts at the wrong seam. Byte-identity of output is a *lagging indicator* of the
property that actually matters, it forces a second committed format surface (which the issue's own
Boundary notes concede is a hazard and then manages with a REPLAN clause rather than removing), and
"first differing byte" is a diagnostic you then have to reverse-engineer semantics out of.

## The arc it is measured against

The project has already fought and won the representation-independence fight once, at the level
below this one:

- `Circuit.getElementsInStableOrder()` seeds `initSimulation` in canonical stable-id order
  (`src/jls/sim/Simulator.java:189-197`, #181), explicitly so that "every simulated value [is] a
  pure function of circuit content" rather than of hash iteration order.
- The event queue is ordered by `(time, seq)` (`src/jls/sim/Simulator.java:24-25`), so posting
  order — fixed by the seed order above — fully determines same-time resolution.
- Save is canonical: blocks sorted by stable id (`src/jls/Circuit.java:1486`, #166), bare `\n`,
  `save → load → save` a byte fixed point (`test/jls/DeterministicSaveTest.java:76`).
- `Circuit.stateHash()` (`src/jls/Circuit.java:1548`) is SHA-256 over that canonical text — an
  existing, tested content-equality oracle, built for #163 as a convergence oracle.
- Batch output is *contractually* byte-deterministic (`docs/batch-interface.md:215`), and the VCD
  profile deliberately omits `$date`/`$version` to keep it so (`:247-249`).
- `ARCHITECTURE.md` already records a binding equivalence criterion of exactly this shape for the
  #221 decision: any second execution strategy "must agree bit-for-bit with the #202 RV32I
  integration golden run as a differential oracle."

So JLS already owns (a) a canonical form, (b) a content hash over it, (c) a determinism contract on
the output, and (d) the concept of a differential oracle with a named reference design. #606
proposes to build a private, fixture-backed instance of (d) for one feature, without citing any of
(a)–(c). That duplicates part of the arc instead of extending it.

## Reframing 1 — compare the model, not the bytes

Given the four facts above, `sim({D_i}, B) == sim(D_flat)` byte-for-byte is not an independent
property to be sampled with fixtures. It is *entailed* by a single stronger statement:

> `stateHash(elaborate({D_i}, B)) == stateHash(elaborate(D_flat))`

If the two loads produce the same canonical circuit, the seed order is the same, the posting order
is the same, the event stream is the same, and the bytes cannot differ. If they produce different
canonical circuits, you learn it *at load time*, as a structural diff of two save texts — "element
`and0` has stable id 7 here and 9 there", "net `clk` is named `clk_2` in part 1" — instead of at
output time as byte offset 4,193. That diagnostic is strictly better than AC-2's, and it is free:
the comparator already exists and is already tested.

This also dissolves AC-1's fixture problem. Do not commit designs in both forms. Commit one, and
have the harness *derive* the partitioned form by applying the partitioner at test time. Then there
is exactly one artifact to maintain, no fixture drift, and the harness runs over the **entire
existing corpus** — every `AllElementsRoundTripTest` fixture, every batch golden,
`test/fixtures/riscv-sum1to10.jls` — as a property test, rather than over "at least one fixture
whose cut actually crosses nets." AC-3's vacuity worry (a cut that touches nothing) becomes a
property of the generated cut, checkable in the harness itself (`assume |B| > 0`, and report the
crossing count per fixture), instead of a cross-issue ordering dependency on #604 being green.

## Reframing 2 — the seam is `SubCircuit`, not a new part-file format

`SubCircuit` is documented as "connecting this circuit to an imported subcircuit"
(`src/jls/elem/SubCircuit.java:14`). Today the import is resolved at edit time and the body is
inlined into the saved file as a nested `CIRCUIT` block (`docs/file-format.md:321`), and — the
detail that matters — **stable ids restart at 0 inside each nested block** (`:370`). JLS therefore
already has per-part identity scoping, a pin-set boundary with stable names, and a hierarchy the
loader walks recursively.

A "part file set plus boundary description" is, at this seam, just *a SubCircuit stored by
reference instead of by value*. Cut there and:

- the boundary description is not a new artifact — it is the subcircuit's pin list, which already
  has stable names that already survive being cut;
- #602's "a cut net names the same signal on both sides" is the existing pin-name contract;
- #604's uncuttable refusal narrows to "a combinational cycle through a SubCircuit's pins", which
  is a statement about a construct that exists today;
- and **#606 becomes the inlining theorem**: a design with an imported-by-reference subcircuit
  elaborates to the same canonical circuit as the same design with that subcircuit inlined.

That property has users *now*, with zero distribution ambition: a course shipping a library of
standard components (an ALU, a register file) as separate `.jls` files that labs reference rather
than copy. Today the only way to reuse a block is to inline a copy, and there is no way to fix a
bug in it across the designs that copied it. Chasing that user gets the mechanism built, tested and
exercised by real designs — and #332's capacity story falls out later as a consequence, if anyone
ever needs it, instead of being the sole justification for the whole chain.

## Reframing 3 — one representation-equivalence harness, four customers today

Whatever the seam, do not build this as `PartitionEquivalenceTest`. Build
`RepresentationEquivalenceTest`: a parameterized differential harness over (fixture × transform),
where a transform is any operation that must not change meaning, asserting equal `stateHash` and
equal batch stdout/VCD. Registered transforms available *before* any partitioning exists:

1. save → reload (the #18 round-trip, already pinned);
2. undo snapshot → restore (`CircuitSnapshot`; ARCHITECTURE.md states undo semantics *are*
   save/load semantics — so this must hold and is not currently asserted against simulation output);
3. container re-encode: XZ ↔ zip ↔ plain text (`-savetext`), three containers the loader already
   sniffs;
4. subcircuit inline ↔ by reference (reframing 2);
5. later: partitioned ↔ flat, and any future compiled evaluation pass, which #221's revisit trigger
   already obliges to pass exactly this test against the #202 RV32I golden (`RiscvCpuGoldenTest`).

Built this way, the harness lands in days against transforms 1–3, proves itself on real designs,
and #332 inherits a *proven* oracle instead of shipping an unproven one at the end of a five-task
chain. It also gives the project the thing it has been assembling piecemeal since #163/#166/#181:
one written-down statement that a JLS design's meaning is a function of its content alone.

## What I am disregarding, and why

- **AC-1's fixture-pair form** — replaced by derived partitions over the whole existing corpus.
  Committed dual fixtures *are* the second format surface the Boundary notes warn about; deriving
  one form from the other removes the hazard rather than instituting a REPLAN protocol for it.
- **AC-2's "first differing byte"** — a byte offset is the wrong diagnostic primitive. Compare
  canonical text and the structured trace stream; the signal and time come out named, by
  construction, and stdout byte-identity follows from `docs/batch-interface.md:215`.
- **AC-3's ordering dependency on #604** — the vacuity guard belongs inside the harness (assert the
  cut crosses nets, record the crossing count) rather than as a cross-issue precondition recorded in
  test output. The falsify-first instinct behind AC-3 is excellent and should be kept; its
  *implementation* as an inter-issue handshake is fragile and untestable.
- **The position of this task last in the #332 chain.** Under any of the reframings above, the
  harness comes *first*: it is the cheapest, most reusable, and most immediately useful piece, and
  it is the only one that pays off if #332 is descoped — which the visionary review of the serving
  capstone #312 recommends.

AC-4 I keep as written: divergence is evidence about the design, not a fixture to be adjusted.

## Disposition

**Endorse-with-reframing.** The claim — that the two forms must be shown equivalent, not assumed —
is right and is the best-aimed scope in #332. Change three things: assert model equality via the
existing `Circuit.stateHash()` rather than diffing output bytes; derive the second representation at
test time rather than committing fixture pairs; and host the harness as a general
representation-equivalence property with the four transforms that exist today, so it is built and
trusted before partitioning arrives rather than after. If #332 is later redirected, this work
survives intact — which is precisely the test of whether a task was cut along the right seam.
