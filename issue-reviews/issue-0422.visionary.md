# Issue #422: TASK-0060: min, max, complement, cyclic and literal are written once over the planes, radix 2 provably collapses to today's BitSet operations, and radix 6 is refused with the arithmetic
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is actually for

One sentence, stripped of apparatus: **there should be exactly one place in JLS
where "what does AND mean on a non-binary digit" is answered.** That end is
right and I endorse it without reservation. Everything else in this issue — the
plane-wise boolean realizations, the lane-packed Kogge-Stone adder, the PEXT/PDEP
hardware survey, the 200,000-vector seeded corpus, the 80/82 PIT re-baseline — is
a *route* to that end, and I think the route is roughly four times the size it
needs to be and cut along the wrong seam.

## The seam this task refuses to cut, and should

§7.12 is explicit: "The five element bodies of O3 are **not** rewritten by this
task — that is FEAT-029's work when the N-ary elements arrive." Read that against
H4, which says the kernel exists "to keep two engines from disagreeing." The task
therefore ships a module with **zero in-tree callers**, and simultaneously
institutionalizes the exact duplication H4 names: after this lands, `min` at
radix 2 lives in `jls.core.RadixOps` *and* `value.and(inVal)` lives in
`AndGate.computeOutput` (`src/jls/elem/AndGate.java:72`), forever, with P2 as a
test asserting they agree rather than a refactor making them the same code.

That is not a nitpick about ordering; it is the design. The alternative seam is
already sitting in the tree and the issue never looks at it:

```
src/jls/elem/Gate.java:27   public abstract sealed class Gate extends LogicElement
src/jls/elem/Gate.java:663  protected abstract BitSet computeOutput();
src/jls/elem/Gate.java:695  public void react(long now, Simulator sim, SimEvent.Payload todo)
```

`Gate` is **sealed**, with one abstract seam and one shared `react`. Five
subclasses fill in one line each. That is already "the operator written once per
kind, in one place, dispatched by the base class." The radix-general move is not
to build a parallel kernel beside it — it is to widen that seam so
`computeOutput` is parameterized by the port's alphabet.

**Reframing 1 — there is no ternary AND gate. There is an AND gate whose ports
are ternary.** The existing `AndGate` computes min at whatever radix its ports
declare; at radix 2 min *is* `BitSet.and` and the code path is literally
unchanged. Consequences:

- The kernel gains a real caller on day one. "Written once" becomes structurally
  true instead of test-asserted.
- **P2 stops being ceremony and becomes load-bearing.** As written, P2 asserts an
  equality that no production path depends on — 10,000 random pairs proving two
  implementations that never meet still agree. Under Reframing 1, if min at N=2
  diverged from AND, `BatchSimulationGoldenTest` and `SequentialGoldenTest` go
  red on the first run. The existing golden corpus becomes the oracle, which is
  what P7 already half-admits.
- FEAT-029 (#361) shrinks from "a family of N-ary elements" to "existing elements
  accept a radix." The palette does not double; a student does not learn a second
  AND gate; `resources/help/**`, `Map.jhm`, `JLSHelpTOC.xml`, `SaveTags`,
  `AllElementsRoundTripTest` (ARCHITECTURE.md's sixteen-place list) are not paid
  eight more times. This is a large downstream saving that this task's scoping
  decision quietly forecloses.

## Reframing 2 — the table is the specification; the boolean formulas are derived, and verification is exhaustive

§7.10 says each operator is "realized plane-wise, i.e. as a fixed boolean
expression on $(a,b,u)$ per output plane." That sentence hides the actual labor.
At $N=2$ the collapse to AND/OR/NOT/XOR is an artifact of the code-point
assignment (0/1 living alone on plane $a$). At $N=3$ with a dense assignment,
`min` is **not** bitwise AND — it is a comparator, several ops per plane. So the
deliverable is not "one table"; it is **4 radices × 7 operators × 3 output
planes ≈ 84 hand-derived boolean expressions**, each needing its own argument,
and H4's refutation condition ("an operator was missing") cannot even see the
more likely failure ("an operator was wrong at radix 4 only").

The elegant route: **make the r-ary operator table the artifact, and derive the
plane expressions from it mechanically.**

1. `RadixAlphabet` produces the code-point assignment and, for each operator, the
   $8\times8$ (or $8$) result table over code points. Tables are data; they are
   readable, reviewable, and diffable.
2. Application is table lookup per digit position. This is the reference
   implementation and it is *total* — every operator at every radix, including
   ones nobody enumerated.
3. If and when a plane-wise fast form is wanted, synthesize it by exhaustive
   search over 3-input boolean functions (the search space is trivial) and assert
   the synthesized form against the table.

The verification consequence is the strong part: **the entire operator surface is
exhaustively checkable.** Every binary operator over 3 planes has $8 \times 8 =
64$ input pairs; every unary one has 8. P2's "10,000 random operand pairs at four
widths" is *weaker and larger* than enumerating all 64 code-point pairs and
noting that per-position purity (§7.9) makes width irrelevant by construction.
The same holds for the adder: per-digit balanced-ternary add-with-carry is
$3\times3\times3 = 27$ cases. Random sampling is the right tool only for the
lane-packing layer — which brings me to the third reframing.

## Reframing 3 — ship the slow reference only, and let #221's own rule decide when to optimize

ARCHITECTURE.md records, for the closest analogous question:

> the `jls.sim.Simulator` event-queue interpreter remains JLS's **only**
> simulation execution strategy … a second strategy is premature optimization
> until CPU-scale designs are actually common. **Revisit trigger:** a concrete
> CPU-scale design … that is unusably slow interactively.

This task proposes, for a datapath **nobody can draw yet**, a lane-packed
Kogge-Stone adder, a plane↔lane round-trip layer (P5), a committed PRNG and
200k-vector differential corpus (P4, Open Question 4), a normative algorithm
section in `docs/simulation-semantics.md` (P9), and an empirical survey of
`PEXT`/`PDEP` microcoding across Zen 1 / Zen+ / Zen 2 / Hygon Dhyana (H5, P10,
Threat 1) — which at bus factor 1 means **acquiring AMD hardware to close a
checkbox in a task that ships nothing a user can see.**

Ship the per-digit reference. Only the reference. It is obviously correct, it is
exhaustively verifiable per Reframing 2, and it deletes: the lane layout, P4, P5,
the PRNG question, P9, P10, H5, Open Question 1, and Threats 1, 2 and 5 — most of
this issue's surface area and nearly all of its permanent maintenance obligation.
The differential test is not lost, it is *deferred to the moment it has a
subject*: when a real ternary circuit is measurably slow, you write the fast
adder and P4 is the first test you write, with the reference already in tree.
That is exactly the shape #221 prescribes, applied one tier down.

The §7.10 cost model does not argue against this. It says the naive path costs
~+40% of loop time **on a ternary machine**, and "binary circuits pay zero either
way." A 1.4× slowdown on a circuit that does not exist, in an interactive
classroom tool where the workload is gate-count-tens, is not a number that
justifies owning a parallel-prefix adder today.

## Smaller observations that point the same direction

- **The `Long.compress`/`Long.expand` question has a third answer the issue never
  lists: don't.** Options (a) and (b) both presuppose the lane layout. Shift/mask
  is portable, has no microarchitectural cliff, and JLS ships "one offline jar to
  unknown student laptops" — the issue's own framing argues for the boring
  choice. Choosing it costs zero measurements and closes Open Question 1 in a
  sentence.
- **The illegal-code-point guard is vacuous exactly where it matters most.**
  Three planes give 8 code points; radix 5 needs $5+3=8$. At $r=5$ every code
  point is assigned, so §7.11's "a digit outside $[0,N)$ → a named failure"
  cannot fire at the widest supported radix — the one whose hand-derived formulas
  are most likely to be wrong. Worth knowing before it is written as a safety net.
- **The "any later levelized pass" justification cites a decision the project made
  the other way.** ARCHITECTURE.md records the compiled pass as explicitly not
  built, with a named revisit trigger. Using a hypothetical second engine as a
  reason to build a shared kernel now is borrowing motivation from a road the
  project declined to take.
- **P8 makes an uncalled module a mutation-coverage obligation.** Adding
  `jls.core.*` to PIT `targetClasses` and re-baselining 80/82 for code with no
  caller means the reference and the fast path exist largely to kill each other's
  mutants. Under Reframings 1 and 3 the coverage comes from circuits that use it.

## What I am disregarding, and why

I am disregarding H3, P4, P5, P9, P10, H5, Open Questions 1 and 4, and the
`BalancedTernaryAdder`/lane-packing half of §6, §7 and §8 — not because they are
wrong on their own terms (they are unusually careful) but because they are the
price of building the optimized form before the unoptimized form has a user. I am
also disregarding §7.12's "the five element bodies are not rewritten by this
task": that clause is what makes the kernel callerless, makes P2 ceremonial, and
hands FEAT-029 a second element family instead of a widened one. If exactly one
thing changes about this issue, it should be that clause.

## The task I would file instead

`jls.core.RadixAlphabet` (code-point assignment + the $\lceil\log_2(r+3)\rceil$
bound with the arithmetic in its refusal message — P3 and P6 survive intact and
are the best part of this issue), plus operator **tables** over code points, plus
a per-digit application function, verified exhaustively over all 64 code-point
pairs per operator per radix; and `Gate.computeOutput` widened to take the
alphabet, with the five subclasses expressed over the kernel so that radix 2
byte-identity is proven by `BatchSimulationGoldenTest` running unregenerated
rather than by a synthetic sampling test. No lane layout, no prefix carry, no
PRNG, no hardware survey, no new PIT baseline for uncalled code. That is perhaps
a third of the stated work, has a caller the day it lands, and leaves every
optimization decision open with its trigger written down — which is how this
project has decided every other question of this shape.
