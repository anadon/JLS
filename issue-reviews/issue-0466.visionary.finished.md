# Issue #466: TASK-0111: batch mode gains a verdict — a separate expectations file, one shared runner behind the CLI and a GUI panel, a byte-deterministic xUnit report, exit status 3, and one complete worked lab
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the 14 sections and the issue makes one claim: **JLS should be able to say
"wrong", not just "here is what happened."** `docs/capability-roadmap/lf-04-formal-and-grading.md:9-11`
states the same gap in the project's own words — *"JLS has no representation of
'correct.' It has a representation of 'what happened,' and grading is a string diff
over that."* `sweep-04-verification.md` change H says the report channel and the
exit-status contract *"must be designed before anything above ships"* because they
are a **promise**, not code.

On that claim the issue is correct and correctly early. `docs/batch-interface.md`
§1 really does have three statuses; §2.2 really is four productions with no output;
`src/jls/sim/` really contains only `BatchSimulator, Reacts, SimEvent, Simulator,
TraceGeometry, TraceSample, package-info`. Four downstream consumers (#483 coverage,
#347 parity, #321 netlist differential, #757 cohort grading) plus two contract-freezing
issues (#524, #692) all queue behind this one channel. Building the channel once,
before anyone builds a second one, is exactly the right architectural move, and the
adversarial revision comment of 2026-08-08 already did the necessary surgery
(panel → #214, cohort command → #757).

So the strategic frame is right. My objection is to **what gets frozen inside it**.

## The central problem: this ships the weakest possible representation of "correct" — permanently

§7.7 is explicit that the expectations grammar *"joins the §6 stability promise from
the moment it ships."* #692 will then freeze the envelope around it, and #524 says
four grading adapters get written against it. So the grammar

```
expect ::= name ("at" time | "after" delay) value
```

is a one-way door. Judge it as such, and three things are wrong with it against the
project's own stated trajectory:

**1. It reproduces the pathology lf-04 filed this whole capability to kill.** lf-04
condemns `EXPECTED_STDOUT_LINES` because *"a submission that is wrong on 254 of the
256 possible inputs and right on that one passes."* A point-sample expectation list
has exactly that property. All this issue changes is that the sample comparison moves
from Python into the JVM and gains an exit status. That is a genuine ergonomic win
(xUnit, status 3, no string parsing) and it is **not** a representation of correct.
The issue should say so plainly rather than letting §13's "returns 3 on a wrong
circuit" imply otherwise — a course author reading that sentence will believe more
than the feature delivers.

**2. `at time` silently encodes a timing assumption the instructor did not intend.**
`docs/simulation-semantics.md` gives every element a propagation delay; `Adder.resetPropDelay`
is `bits * defaultPropDelay`. Two functionally identical submissions with different
gate counts settle at different simulated times. An instructor writing `out at 50 0xED`
is grading *when* the circuit answers, not *what* it answers — and the student who
builds the smaller, faster circuit fails, or passes by luck. This is the timing analogue
of the `TruthTable` don't-care bug lf-04 flags at `:1446-1449`, where *"JLS today punishes
the exact optimisation the Karnaugh-map lecture two weeks earlier taught them to make."*
lf-04's own rule for the formal path is the fix: **"JLS proves what the circuit settles
to, not when."**

The primitive should be **quiescence**, not absolute time. It already exists in the
simulator: `BatchSimulator.java:570` produces `Simulation: No More Activity` when the
event queue drains. Per-vector quiescence — the point where no events remain before
the next stimulus event — is the same predicate applied locally, and it is cheap.
Proposed grammar, same size, different semantics:

```
expect ::= name "settled" [ "after" vector-index ] value    # the default form
         | name ("at" time | "after" delay) value           # escape hatch, timing-sensitive
```

Open Question 1 currently asks only what the base of the *first* `after` is. That is
the wrong question at this altitude: the right one is whether absolute time should be
the default sampling mode at all. **I am disregarding Open Question 1 as posed.**

**3. The verdict is binary where #369 requires a trichotomy — and this is a live defect,
not a future refinement.** §7.10 defines `obs(C, V, n, θ(w_j))` as a total function.
It is not. A run can hit the `-d` time limit (`Simulation Time Limit`) or drain
(`No More Activity`) before `θ(w_j)`. Under the stated arithmetic that yields
`¬passed_j` → status 3 → **"your answer was wrong"** for a submission whose answer was
never observed. #369 §4 invariant 2 — *"'Could not run' is never reported as 'failed'"* —
is a **global invariant every child preserves at every intermediate landing**, and
#369 §3 makes `UNRUN` absorbing under the join. The issue defers the verdict lattice
to TASK-0073/#483, but the invariant binds this landing. The verdict record of §7.5
must be `(expectation, observed | UNREACHED, PASS | FAIL | UNRUN)` on day one, or the
first grading run over 200 submissions with a too-short `-d` marks the whole cohort
wrong.

## Reframing 1 (recommended, small): freeze the verdict model, render the reports

The issue freezes **two byte formats** — xUnit XML and a plain line format — each
golden-pinned, each with its own writer, each inheriting the determinism obligation
independently. That is the wrong seam. Every downstream consumer needs to emit verdicts:
#483 (coverage as `(covered, total, metric)`), #347 (parity at sync points), #321
(netlist differential), #757 (per-student aggregate). Freeze two *formats* here and
each of those grows its own writer and its own determinism argument.

Cut instead at the **verdict document**: one canonical, versioned, ordered structure
(`VerdictSet`), with the plain line format as its canonical text serialization and
xUnit XML as a *pure rendering function* over it. Then:

- Determinism is a property of one canonicalization, not of N writers — which is exactly
  what #692 needs to make its cross-container guarantee provable rather than re-tested
  per emitter.
- #524 freezes a **model**, which can gain fields compatibly, rather than a byte layout
  that cannot.
- LMS-specific or JSON renderings later cost a function, not a contract amendment.

**Concrete gap this exposes:** #369's DoD says *"The report schema carries a version
field"*, and the #692 boundary comment warns that shipping without it makes #524's
third acceptance criterion unsatisfiable. §7.6 of this issue lists the report's contents
— testcases, failures, no timestamp/hostname/duration — and **contains no version field**,
and the revised DoD in the 2026-08-08 comment does not add one. That is a one-line
omission that permanently costs a downstream issue its acceptance criterion. Add it.

## Reframing 2 (the out-of-the-box route the issue never considers): the reference is a circuit, not a table

The issue assumes the expectation must be *written* as literal values. It need not be.
The instructor already has the artifact that defines correct: **their reference circuit.**
Two routes follow, both cheaper in author effort than a value table and both stronger:

**(a) `-check golden.vcd` — trace comparison, zero new grammar.** `docs/batch-interface.md`
§4 already emits a byte-deterministic VCD covering *every* watched element at *any*
hierarchy depth plus every probed net (§4.1) — a strictly richer observable set than the
three-type stdout whitelist this issue's expectations are confined to (O9). Run the
reference circuit, keep its VCD, run the submission, compare. The check is **total over
the simulated window** rather than a hand-picked sample; the counterexample falls out as
"first differing time and signal", which is precisely the shape #347's parity harness and
lf-04's formal counterexample writer both need; and the format is already frozen and
already golden-tested. The known weakness is delay sensitivity — which the quiescence
sampling above fixes, by comparing settled values at vector boundaries rather than every
timestamp.

**(b) A drawn `Assert` element.** #369 Open Question 4 already prices this and
**recommends yes** — *"a property a student can draw is the pedagogically useful form"* —
beside `Stop` and `Pause` in the simulation-control family of the sealed `LogicElement`
permit list. A drawn assert inherits name resolution, bit-width checking and subcircuit
hierarchy from the existing element machinery instead of re-implementing them as string
lookup in a parser, **and it fires under `InteractiveSimulator` too** — a student sees
the property fail while stepping, with no second front end. This issue's separate-file
design can never reach interactive simulation without duplicating the runner, which is
the drift H2 exists to prevent, appearing on the other side.

The endgame both point at is the industry-standard shape: **a testbench circuit that
instantiates the submission as a subcircuit**, where JLS's own drawing surface is the
expectation language and a third hand-synchronized text artifact never exists. I am not
proposing that for this issue — `SubCircuit.save` writes nested circuits **inline**
(#369 evidence, `src/jls/elem/SubCircuit.java:282-288`), so instances have no shared
identity and a testbench cannot reference a submission file. That is a real blocker with
a real owner. But it is the direction, and the expectations grammar should be shaped so
it does not become the thing that makes it unreachable.

## What I would keep exactly as written

- **The gate-first discipline.** "Design the gate first; retrofitting it is how a promise
  gets broken" is the single best sentence in the issue, and H4 (status 3 unreachable
  without `-check`, *including on error paths*) is genuinely load-bearing: a grader that
  scores a cohort wrong on its own config error is the worst failure mode available here.
- **The separate file rather than a `-t` grammar change.** H1 is right for the grading
  case specifically: the instructor grades a circuit the student drew and cannot edit it.
- **Determinism as a contract item, not a nicety** (§7.9), and the omission of
  `timestamp`/`hostname`/`time`.
- **The empty `blocked_by`.** The headless verdict channel depends on nothing, and #524/#686/#757
  ordering behind #369 rather than behind #466 alone is the ordering defect the revision
  comment correctly identifies.

## Concrete asks, in priority order

1. Add the **schema version field** to §7.6 and to the DoD. One line; without it #524's
   third criterion is dead on arrival.
2. Make the verdict **three-valued** (`PASS`/`FAIL`/`UNRUN`) with `UNRUN` absorbing, and
   assert the unreached-sample case. #369 invariant 2 binds this landing.
3. Make **quiescence the default sampling mode**; demote `at time` to a documented,
   timing-sensitive escape hatch. Reopen Open Question 1 at that altitude.
4. Freeze the **`VerdictSet` model**; make xUnit a rendering over it rather than a second
   frozen byte format with its own writer.
5. Say plainly in §1 what the feature does **not** do: sampled expectations are not a
   representation of correct, and lf-04's total check remains ahead. Course authors will
   read §13 as a stronger promise than it is.
