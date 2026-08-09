# Issue #423: TASK-0073: a parity failure names the exact retirement index and the exact field, the exclusion set is printed and ratcheted, and "I don't know" is never a pass
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machinery and the claim is: **JLS has no vocabulary for "this design is
right."** The batch surface (`docs/batch-interface.md` §3.1) can say
`Simulation Complete at 68000` and print register values; it cannot say *passed*
or *failed*. That single absence is what #369 (autograding), #214 (test panel),
#202 (differential oracle) and this issue are all circling. #423 is the first of
them to be filed with a concrete verdict type in it, so it is not really "a
comparator task" — it is the issue that gets to decide what a JLS verdict *is*.
That framing is worth more than the four classes it names, and the review below
is mostly about not spending the opportunity on an RV32-shaped answer.

The gap is real. `src/jls/parity` does not exist; neither does `src/jls/mach`;
`docs/parity-contract.md` and `docs/plan/` are not in this checkout and the
evidence commit `2d0ca9d` is not reachable from HEAD (`git cat-file -t` fails),
so every §2 observation is unverifiable here and must be re-derived at pickup as
§8 already instructs. First-divergence reporting as *the product* rather than a
boolean is correct and non-obvious, and "a differ that reports 'traces differ'
has done nothing `cmp` could not" is the right sentence to have written down.

## Where it pulls against the arc

**1. `SyncPointDigest` hardcodes RV32 into the generic layer.** §7.4 defines the
digest over "PC, all GPRs, every implemented CSR minus `E`". Its parent feature's
IC-6 demands the opposite: "point the same comparator at the ternary machine and
observe it works with **no comparator change** — only a new record projection."
A ternary machine has no GPRs indexed 0..31 and no CSRs. As written, TASK-0073
guarantees IC-6 fails, and #347's own re-planning protocol says a TASK-0073
refutation refutes the whole feature.

**2. The exclusion set is a subtraction from an implicit universe.** Every threat
the issue records about `E` — H2, §11's "pressure point", Open Question 1's
unanswerable "bounded by what?", falsification criterion "a set that grows under
pressure is how a differential harness dies" — follows from one design choice:
comparing *everything the implementation happens to have*, then subtracting. Note
that #390's record is already the opposite polarity: its *type* enumerates what
is compared, which is why it needs no exclusion set. `E` exists only where the
issue reverts to a universal ("every implemented CSR", "full architectural
state") and then claws back.

**3. The null family is hand-rolled mutation testing, and PIT is already in the
build.** `pom.xml:765-813` configures `org.pitest:pitest-maven` behind a `pitest`
profile with the 80/82 thresholds O8 cites, and
`docs/mutation-testing-trial-2026-07.md` records the adoption. "Four subtly wrong
`jls.mach` implementations, committed, each asserting its own report text" is
four hand-written mutants under a hand-written harness — and §11 admits the
weakness in advance ("a fault class none of them represents is untested").

**4. The project already had a differential oracle and the plan deletes it.**
`riscv/fuzz_diff.py` generates random RV32I programs, runs each on
`riscv_ref.py` and on the drawn JLS circuit, and reports per-register and
per-address `ref=… hw=…` diffs (`fuzz_diff.py:78-86`). O9 mentions `riscv/` only
as a place the new work must not live, and TASK-0025 deletes it. The related-work
table never says this code exists. Rebuilding a differential oracle in Java is
right; deleting the working one without harvesting its **program generator** is
the part that pulls against the arc, because the generator is the asset and
`build_cpu.py` is not.

## Reframing A — let `D` declare the alphabet; delete `E` entirely

`docs/parity-contract.md` §2.6 already says sync points are *declared in `D`*,
and §7.4 already takes the non-volatile memory regions from `D`. Extend that one
step: **`D` declares the comparison alphabet as an ordered list of named state
items**, and the comparator compares exactly that list — nothing minus nothing.

What disappears, not by argument but by construction:

- `ExclusionSet`, its ratchet test, its both-directions doc assertion (P3), its
  print-in-every-report rule (P5 becomes "the report echoes the declared
  alphabet", same user-visible property), H2, and Open Question 1's bound.
- The code-versus-Markdown-table drift test. JLS already carries several of these
  (`CliFlagTableTest`, `ExtensionPointCatalogTest`); asserting a `Set<String>`
  against a prose table in `docs/` is the pattern this repo should stop adding to,
  not extend.
- The RV32 shape in `SyncPointDigest`, which becomes a digest over an ordered
  sequence of `(name, value)` presented by whichever machine is bound — so IC-6
  is satisfied by construction and the ternary counterparty (TASK-0082) needs a
  fixture, not a comparator change.

What is preserved: `mcycle`/`minstret`/`mtime`/`mtimecmp` are simply absent from
the declared alphabet with their reasons as comments in `D`. Growth is still
reviewer-visible — it is a fixture diff — but now growth makes the test
*stronger* and shrinking is the dangerous direction, which is the direction a
ratchet is actually good at guarding. §11's "every excluded bit is a place the
two implementations are permitted to disagree" stops being a standing risk.

## Reframing B — the verdict is a sealed interface over records, not an enum

`isPass()` with no `default` arm is the right instinct aimed at the wrong object.
The real hazard is not a fifth enum constant; it is that `UNKNOWN` with no
payload *is* a pass in practice — a reviewer sees it, shrugs, and re-runs. JLS
already has both precedents in tree:

- `jls.collab.op.CircuitOp` — "a sealed interface over data-only records"
  (`src/jls/collab/op/CircuitOp.java:11,34`).
- `jls.LoadError` — a category taxonomy plus location, detail, and an actionable
  hint, published through one channel so every front end shows the same message
  (`ARCHITECTURE.md`, "Error-reporting contracts").

Model the verdict the same way: `sealed interface Verdict permits ParityHeld,
DivergedAtIndex, NotComparable, Unknown`, where `DivergedAtIndex` carries
`(k, recordD, recordP, fields)`, `NotComparable` carries a typed cause
(`DifferentSyncPoints`, `Malformed`, `ShortTrace`), and `Unknown` carries a
reason it is *required* to name. A pattern switch over a sealed hierarchy gives
H3's compile-error property exactly as the enum does, and additionally makes
"I don't know" unable to be filed without saying what it does not know. This is
the "Kotlin's transferable practices as compiler-enforced properties" line from
`docs/grand-architecture.md` §1 applied where it pays.

## Reframing C — mutate the reference; don't hand-carve four nulls

Keep P6's *discipline* (assert the report string, no constant-zero
implementation) and replace its *implementation*. Commit one golden trace from a
directed program, add `jls.mach.*` to `targetClasses` in the `pitest` profile
with the parity test as the suite, and **every surviving mutant is a null the
harness failed to reject.** That converts §11's "the null family is four faults"
from a permanent admitted weakness into a ratcheted survivor count, using a tool
already adopted and already thresholded in this build.

Honest limits, which belong in the issue if this route is taken: mutation runs
cost one trace comparison per mutant, so the lane runs against `#278`'s short
fib/memtest fixtures rather than a boot-length trace, and unreached-code mutants
mean the bar is a survivor-count ratchet, not zero. Both are cheaper problems
than "four faults are the whole null family forever." Pair it with the
`fuzz_diff.py` generator ported into `test/` **before** TASK-0025 deletes
`riscv/` — H1 ("does the reported index localise the fault?") is a question a
generator answers over thousands of programs and four hand-picked faults cannot.

## Reframing D — build `jls.report`, not `jls.parity`'s private formatter

§7.1 calls the report "the sixth consumer of the shared report channel" and §7.7
versions its schema "because #369 and #214 render it" — the issue has already
noticed the shared object and then homes it privately in `jls.parity` (§7.5).
Cut the seam where the reuse is: a small `jls.report` with the sealed `Verdict`,
a schema-versioned record, and one writer; `TraceDiffer` becomes its first
producer, and #369, #214, #202 and the `-t` engine become the next four without
renegotiating a format. Given `docs/extension-points.md`'s typed-seam catalog,
"verdict producer" is a plausible future point id; at minimum the report should
not be born inside the parity package. Minor, same theme: `-diff-against` as the
fifteenth flag on a comparator that never loads a circuit or starts a simulator
argues for a verb (`jls diff a.trace b.trace`) rather than a flag — worth one
line of thought now because the flag table is a documented stability contract.

## Acceptance criteria I am explicitly disregarding

- **The `ExclusionSet` class, P3, P5-as-written, and Open Question 1.** Under
  Reframing A there is no exclusion set to ratchet or bound. If the maintainer
  keeps `E`, Open Question 1's option (a) — a hard numeric cap — is right, but
  the better move is to make the question unaskable.
- **P6's "four committed nulls" as the *only* null family.** Keep four as a
  fast, readable smoke family; do not let them be the answer to IC-1.
- **§7.4's `SyncPointDigest` field list.** It should name no RV32 concept.
- **"Ratify the contract in the merge commit" (§7.12 claim 5).** §11 already
  concedes this leaves the specification with no independent review. Reframings
  A and B change the contract's §2.5 and §5.2 substantively, which is precisely
  the case where ratify-first (#347's Open Question 1, option (a)) costs a
  decision and saves a rewrite.

## Verdict

**endorse-with-reframing.** The gap is genuine and correctly identified — nothing
open covers the comparator — and first-divergence reporting, `UNKNOWN`-is-never-a-pass,
and the refusal of a constant-zero null are all the right instincts. But three of
the issue's own recorded threats (the unbounded exclusion set, the four-fault null
family, the RV32-shaped digest against IC-6) are consequences of design choices
rather than facts about the problem, and each dissolves under a reframing that
uses machinery already in this repository: declare the alphabet in `D` instead of
subtracting from a universe; make the verdict a sealed hierarchy that forces every
non-pass to carry its evidence; and let PIT plus a harvested program generator be
the null family. Land the comparator; land it ISA-agnostic, with a shared report,
and with `E` designed out rather than policed.
