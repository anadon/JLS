# Issue #463: TASK-0097: a headless MNA transient solver in pure Java — device stamps, sparse LU with a totally ordered pivot tie-break, Newton with junction limiting, and LTE timestep control
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the apparatus and #463 asks for one thing: **an analog answer a grader can
commit to git.** Every other clause — StrictMath, the pivot total order, raw-bit
goldens, single-threadedness — exists to serve "byte-identical on every
platform," and byte-identity exists to serve "a committed golden instead of a
screenshot" (#351 § Intended Audience). The MNA solver is not the goal. It is the
cheapest thing that has to exist before the goal is testable.

That reading matters because the issue's own risk ranking is upside-down relative
to it. Two weeks of numerics are scheduled first, and the falsifiable claim that
justifies the entire programme is deferred to TASK-0098, **which is not filed**,
behind `blocked_by: []`. #351 § 6 says in as many words: *"Do not schedule
TASK-0098 late."* #463 does exactly that, then softens the check: its P8 is *two
runs in one JVM* — the weakest determinism assertion available, and one that
would pass on a kernel riddled with `HashMap` iteration as long as the JIT
behaved identically twice in a row. #351's IC-1 is three operating systems × two
architectures × two JDKs, as a **required gate**. P8 is not a down-payment on
IC-1; it is a different, much easier claim wearing IC-1's clothes.

## Where this sits on the project's arc

Two honest tensions, stated before the design comments.

1. **The tree's own roadmap records the opposite verdict.**
   `docs/capability-roadmap/README.md:1034-1040`, § 6 "What still stays out",
   ground (a) "Different tool class": *"Continuous-time and analog… Supporting
   these means being a SPICE-class solver — a different tool, not a deeper
   digital model."* `:83` is blunter: *"No continuous-time solver, and none
   should be added."* #463 does not cite, quote, or supersede that. It instead
   cites `docs/plan/evidence/BRIEF.md` D8/D10 and `11-analog-determination.md`,
   **neither of which exists anywhere in this checkout** (`find / -name BRIEF.md`
   returns nothing; `docs/plan/` does not exist). Every load-bearing
   justification for reversing a written architectural decision lives in
   documents a reader of this repository cannot open. That is not a reason to
   reject the work — a decision may be reversed — but the reversal belongs in
   `docs/capability-roadmap/README.md` § 6 as an amendment with its trigger,
   in the format ARCHITECTURE.md's "Recorded decisions" already uses, and it
   should land **before** the first line of `jls.analog`, not be inherited from
   an invisible brief.

2. **The project already decided how to do this shape of thing, and #463
   diverges.** `docs/capability-roadmap/lf-04-formal-and-grading.md:297-332` is
   the same problem one domain over — in-tree numeric/logic kernel vs. shelling
   out — and its recommendation is Option C: *write the solver in-tree, keep the
   printers as a first-class escape hatch,* **and make the dangerous answer carry
   a checkable certificate** (SAT self-checks via `BatchSimulator`; UNSAT must
   emit a DRAT proof, so "the trusted computing base for a passing grade shrinks
   to the proof checker"). #463 adopts the first two-thirds of that pattern and
   drops the third, replacing "certificate" with "digest." That substitution is
   my central objection and my central reframe.

## Reframe 1 (the important one): certify the answer, don't fingerprint the arithmetic

A digest tells you the bits did not move. It does not tell you the bits were ever
right, and it is invalidated by every intended numerical change — #463 § 7.7 says
so itself ("regenerated when a documented numerical change lands"). So the
artifact the whole design is bent around is a **change tripwire**, not a
correctness argument, and it is the most brittle possible tripwire: refactoring
the accumulation order of a stamp, which is semantically free, breaks it.

The alternative the issue never considers: **a residual certificate.** At each
accepted timepoint, substitute the returned `x` back into the assembled device
equations and assert

  ‖F(x)‖ ≤ RELTOL·‖x‖ + ABSTOL,

with the companion-model charge/flux balance (Σ i at every node, and the
capacitor charge and inductor flux increments implied by the trapezoidal step)
asserted alongside. This is ~30 lines, is computed from data the solver already
has, and has properties the digest does not:

- It is **independent of the solver's internals** — LU, pivot order, Newton path,
  the linear fast path, and any future sparse rewrite all survive it unchanged.
- It **detects wrongness**, not just movement. A sign error on a conductance
  stamp — the exact defect #351 IC-3 is worried about — shows up as a residual,
  on any circuit, without a hand-derived golden per device.
- It is **portable across platforms in a way bit-identity can never be**, so it
  keeps working if IC-1 is ever narrowed (which #351 § 7 explicitly plans for).

This is not a foreign idea to this repository. `proofs/README.md` records the
house epistemology already: prove the invariant over a model, pin the model's
assumptions (A1)-(A5) with named tests in `ProofBridgeTest`, and label what stays
empirical. A residual + conservation check is the analog-kernel version of
`query-parity`. **Recommendation: make the residual/conservation certificate the
correctness gate, and demote the digest to a tripwire** that a PR may rebaseline
with a one-line note, exactly as goldens elsewhere in the tree are treated. The
grading story survives intact — a golden whose header says "residual ≤ bound at
every accepted point" is a *stronger* thing to hand an instructor than a hex blob
whose only claim is that it matched last Tuesday.

## Reframe 2: cut the seam at determinism, not at numerics

#463 § 4 H1 cites, as its own supporting evidence, *"a 229-line Java
MNA/Newton/LU/trapezoidal/LTE kernel that produced the identical digest across
seven JVM configurations."* That kernel already exists. The programme's single
falsifiable claim is therefore purchasable in **days**, not after two weeks of
device stamps: land the 229-line kernel as a test-scope fixture, build the
digest harness and the full IC-1 CI matrix around it, and *then* grow the real
solver inside a harness that has already proven the property. If byte-identity
fails on Windows/aarch64, you learn it in week one with 229 lines at risk instead
of week four with a full kernel and a written claim.

This inverts the issue's dependency prose without contradicting its content:
TASK-0098 stops being a downstream enforcement pass and becomes the **scaffold**,
which is what "the discipline starts here or TASK-0098 becomes a rewrite"
(§ Intended Audience) is already groping toward without following through.

## Reframe 3: I am disregarding the sparse-LU acceptance criteria

This is the one place I recommend overriding stated acceptance criteria
outright, because **#463 contradicts its own parent.** #351 § 1 "Out of scope"
reads: *"Sparse factorization. Deliberately deferred, with a stated re-entry
trigger and the pivot tie-break carried with it."* Its Open Question 2 gives the
measurement: dense factorization is **3.765 µs at N = 28** and 13.839 µs at
N = 50, and *"every capstone circuit examined is between 7 and 28 unknowns."*
#463's title, § 7.4 and § 8 all mandate **sparse** LU, and § 7.10 stage 4 makes
the **Markowitz product** the first key of the tie-break — a fill-in-reduction
heuristic that is *meaningless in a dense factorization* and only exists to serve
the sparsity the parent deferred. The issue then names that tie-break "the
least-derisked determinism item in the whole analog program" (H3) and builds a
day-one test for it.

It is a self-inflicted risk. Drop sparsity and the Markowitz key, and the
tie-break collapses to a lexicographic `(−|a_rj|, row)` over a fixed column
order: a total order, trivially, with no fill-in estimate to compute
deterministically and no §Open-Question-2 hazard to carry. The named risk mostly
evaporates rather than being managed. Keep P5 — assert the permutation vector on
a constructed tie — because it is cheap and it is the right habit; but assert it
on the dense kernel that the measurements say is the right kernel for a 28-node
teaching circuit. Sparsity is a documented re-entry trigger (a circuit above
~100 unknowns), not week-one work.

Concretely: strike "sparse" from the title and § 7.4; strike Markowitz from
§ 7.10 stage 4 and from Open Question 2's recommendation; keep everything else in
the LU bullet.

## Smaller things the frame surfaces

- **`jls.analog` as a top-level leaf is chosen partly to dodge a coverage
  floor.** § 7.12 and O5 say so plainly: `jls.sim.analog` "would fall outside the
  `jls.sim` rule while looking like it was inside it," so the package is hoisted
  to top level and lands "unfloored on arrival." Choosing a package name for its
  relationship to `pom.xml:427-505` is the tail wagging the dog. Two corrections
  worth making: the **BUNDLE** rule (`pom.xml:356-372`, 0.545/0.535/0.505) still
  applies, so "unfloored" is only true per-package; and the 86%/88% mutation
  precedent the issue promises to reach **cannot even be measured** until
  `jls.analog.*` is added to pitest's `targetClasses` (`pom.xml:781-785`, today:
  `jls.sim.*`, `jls.BitSetUtils`, `jls.Util`, `jls.SpatialIndex`,
  `jls.collab.op.*`). Add both floors in the same PR that creates the package —
  a new package with a written promise to be floored "in the programme" is how
  `jls.edit` became untracked area, which the issue itself notes.
- **A capability nobody can reach is not yet a capability.** § 7.1 states that
  nothing imports `jls.analog` and it imports nothing — clean, and correct as
  layering. But the whole of FEAT-046 lands before a student can see a single
  volt. `ARCHITECTURE.md`'s module layout already omits `jls.core`, `jls.boot`,
  `jls.module`, `jls.util`, `jls.collab` and `jls.hdl`; the tree has a demonstrated
  drift between packages that exist and packages that are documented and reachable.
  A one-verb batch surface (`-tran circuit.jls`, printing the sample table) as the
  *first* consumer, even behind an undocumented flag, converts the package from a
  library-in-waiting into a vertical slice — and gives IC-1's CI matrix something
  end-to-end to hash.
- **The stable-id dependency is not free, and `blocked_by: []` hides it.**
  H4/P7 rest on `Circuit.getElementsInStableOrder()` (`src/jls/Circuit.java:479`),
  which sorts by `ElementId`, whose `compareTo` (`src/jls/elem/ElementId.java:278-285`)
  compares **replica first**, then counter — and the replica is a random 32-hex
  UUID draw when `JLS_REPLICA_ID` is unset (`:43-51`), unpinned in CI per #351
  Open Question 6. Within one process building one fixture programmatically the
  order is fine. The moment a fixture is loaded from a `.jls` or mixes replicas,
  device order — and therefore the accumulation order the digest depends on —
  turns on a random string. #351 says this "blocks integration of IC-1 and IC-8";
  #463 inherits it silently. Pin it in this PR; it is one line of workflow YAML.
- **What is straightforwardly right and should not be relitigated:** StrictMath
  discipline from line one (O2's 9.673% is decisive, and the cost is nothing at
  teaching scale); single-threaded as a correctness property, not a preference;
  goldens on raw bits rather than formatted decimals; expected values written as
  the kernel's own expression; the elaborator sorting rather than iterating —
  which is already the house pattern, pinned by `SimulationSeedOrderTest` and
  used by `Simulator:151`, `CircuitRenderer:262` and `SubCircuit:577`, so H4 is
  less a discovery than a requirement to keep doing what the tree does. P3's
  anti-cheat assertion is genuinely excellent and I would keep it verbatim.

## Verdict

**endorse-with-reframing.** The end — a pure-Java continuous-time kernel whose
answers can be committed, so an analog lab becomes gradeable rather than
screenshotted — is a real capability and consistent with FEAT-046. The route
needs three changes: (1) make a **residual/conservation certificate** the
correctness gate and the digest a tripwire, following `lf-04`'s Option C rather
than half of it; (2) build the **determinism harness first** around the 229-line
kernel the issue already cites, so IC-1 is falsified or confirmed in week one;
(3) **drop sparse LU and the Markowitz tie-break**, which contradict #351's
recorded deferral and manufacture the very risk H3 then names as the programme's
worst. And before any of it, amend `docs/capability-roadmap/README.md` § 6(a) —
the reversal of "no continuous-time solver, and none should be added" must be
visible to a reader of this repository, not only to a reader of a brief that
isn't in it.
