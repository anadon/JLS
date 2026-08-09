# Issue #306: CAP-09: a reviewer who did not draw the circuit gets a proof, a replayable counterexample, or an honest UNKNOWN — never a false pass
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the tier bookkeeping and the claim is short and correct: **JLS's grading
story today is sampling, and sampling is a lie.** `examples/autograde/autograde.py:45,53`
pins three literal stdout lines for one input vector; a shift-register wrong on
254 of 256 inputs passes. `docs/batch-interface.md`'s status table has 0/1/2 and
no way to say "the run finished and the answer was wrong." That is the real hole,
and it is squarely on the project's arc: JLS is a pedagogy tool whose most
defensible differentiator is not drawing circuits (every tool draws circuits) but
telling a student *why* their circuit is wrong, exactly, with an artifact they can
replay. `docs/capability-roadmap/lf-04-formal-and-grading.md` already argues this
better than the issue does, and its closing pedagogical point — coverage says "did
you exercise it," proof says "is it right," and teaching that these are different
achievements is the payoff — is the best sentence in the whole corpus.

So: the destination is endorsed without reservation. What follows is about the
route, and the route as drawn is wrong in one large way and two smaller ones.

## The mis-cut: the capstone costs 7x the outcome

The issue prices its own required set at **66–103 mw** and its own demo slice —
extractor, AIG, Tseitin/CNF, solver, miter, `-equiv`, counterexample-as-`-t`, exit
statuses — at **8–11 mw**, and then says that slice "satisfies AC-1, AC-2, AC-3 and
AC-9 outright." Four of the nine acceptance criteria, including **AC-9, the one the
issue itself calls "the single criterion that makes the whole capstone falsifiable
against the shipped state of the tree,"** are delivered by a tenth of the budget.
That is not a floor inside a capstone. That *is* the capstone, and the other 55–92
mw is a different capstone wearing this one's title.

Look at what the required set actually contains. FEAT-026 (#322, four-state core)
is **28–36 mw — 42% of the entire required sum** — and §2's minimality answer for
it is "remove FEAT-026 and step 7's UNKNOWN silently becomes a definite answer."
That conflates two unrelated tri-states:

- the **verdict** tri-state `PROVED | COUNTEREXAMPLE | UNKNOWN(reason)`, which is a
  property of the result type in `jls.formal` and of the exit-status lattice; and
- the **per-bit** four-state 0/1/X/Z in the simulator's value domain.

Step 7's corpus is a `Memory`, a two-driver net, a combinational loop, an
incomplete `TruthTable`, a 24-bit multiplier. Every one of those is refused by the
**uncheckability gate in the extractor** — lf-04 prices that gate at 1.5 mw and
says it "is what makes status 5 trustworthy." None of them needs a bit that can
hold X. lf-04 goes further and says today's two-state domain is *"an asset"* for
formal, and that four-state **changes the definition of equivalence** and doubles
formula size. Four-state genuinely earns its place for don't-care-aware grading of
`TruthTable` outputs (`src/jls/elem/TruthTable.java:1447-1449`, verified present on
master) — and lf-04 prices *that* as its own late 2–3 mw slice, gated behind Bits4,
not as a precondition of the verdict channel. The largest line in the required set
is required by one narrow sub-case of one element.

The same test dissolves most of the rest. **The formal path is untimed** — lf-04:
"`propDelay` is discarded." It does not run the event loop except for AC-2's
counterexample confirm, which is one short batch run per submission. So FEAT-005
(`SigSim.java:71,74` string-concatenation stimulus parse — real, verified) and
FEAT-006 (`Simulator.java:231-232` silent event drop past `maxTime` — also real)
are fixes the project should make, but the dominant cost of a verdict is SAT, not
simulation, and a dropped event cannot corrupt a proof that never entered the
queue. FEAT-034 (#347, retirement-indexed parity harness, `RetireRecord`) is
CPU-scale differential simulation; it is the spine of CAP-02/CAP-08, and it is in
this roster because §1 step 6 was written around a CPU. FEAT-023's external oracle
and FEAT-007's CI lanes are the *credibility-at-scale* arc.

That arc is worth funding. It is just not this outcome. **The seam to cut along is
"what is the verdict about," not "what makes a verdict feel credible."** Split:

- **CAP-09a — the verdict channel.** `jls.formal` (cone extractor, AIG, Tseitin,
  solver, miter, uncheckability gate), the one-shot exit-status lattice,
  counterexample-as-`-t`, the two corpora. ~10–16 mw. Delivers AC-1/2/3/9, closes
  the 254-of-256 hole, and is falsifiable against the shipped tree on day one.
- **CAP-09b — the answer is trustworthy at CPU scale.** FEAT-026, FEAT-034,
  FEAT-023, FEAT-009, FEAT-005/006/007. Its §1 is about differential agreement
  and measured budgets, and it is shared spine with #301 and #304 rather than
  borrowed by them.

I am explicitly disregarding this issue's minimality argument for FEAT-026,
FEAT-034, FEAT-005 and FEAT-006. Rule E asks "what breaks in §1 if removed," and
§1 was written so that the answer is "something." Rewrite §1 around the grader
opening a combinational lab submission — the audience the Abstract names first —
and nothing breaks.

## The reframing the issue never considers: refutation is the product

lf-04 states the asymmetry and then the capstone builds against it anyway:

> **SAT (not equivalent) is self-checking, for free, using JLS itself.** … **UNSAT
> (equivalent) is the dangerous answer.**

Every expensive defence in this issue guards the UNSAT half. KC-09-1 ("one false
pass, permanently"), AC-3's matched-port table, Open Question 1's DRAT logging and
proof checker (**4–5 of the 8–11 mw floor — lf-04 calls it "the risky slice"**),
the whole false-pass risk paragraph in §3. Meanwhile the SAT half is validated by
AC-2 replaying the counterexample through the simulator that already exists.

So ship **refutation first, and do not offer a PROVED constructor at all.**
`jls -b -refute ref.jls submission.jls` returns `COUNTEREXAMPLE(-t file)` or
`NOT-REFUTED(budget, method)`, where NOT-REFUTED is documented as *an UNKNOWN, never
a pass*. The consequences are not incremental:

- **KC-09-1 becomes structurally unreachable.** There is no PROVED to be wrong
  about. A capstone whose headline kill criterion cannot fire is a better-designed
  capstone.
- **AC-3's danger inverts.** A mis-matched port map under refutation produces a
  *spurious counterexample*, which AC-2's replay catches automatically. Under
  PROVED it produces a false pass with a proof attached — the issue's own
  "most dangerous artifact this capstone can produce."
- **The solver need not be complete, and the proof checker is not needed yet.**
  Drop DRAT and the checker from the floor and the floor is ~4–6 mw.
- The grader still gets the whole headline: the 254-of-256 submission gets a
  replayable counterexample naming its cone. AC-9 passes.

PROVED is then added later as a strictly additive verdict, once port matching and
the solver have been run against the submission corpus with a known answer key —
i.e. earned rather than asserted.

**And one step simpler still, which makes the SAT question partly disappear.** A
first-year lab's checkable cones are small. Exhaustive simulation over a cone's
input space through the existing `BatchSimulator` is *both* a refutation engine and
a genuine proof — "PROVED by exhaustion over 2^12 inputs" is not weaker than an
UNSAT, it is stronger, because it is checked by the same engine that will run the
student's circuit. No CNF, no AIG, no CDCL, no DRAT, no licence/dependency
question (Open Question 1 evaporates), no second semantics to keep in sync with
`docs/simulation-semantics.md`. The AIG/SAT path then becomes the *extension* for
wide cones, funded when the corpus shows how many cones exceed the exhaustion
threshold — which is a measurement the reference lab corpus produces for free.
The issue jumps to a SAT solver in its first sentence and never asks what fraction
of its own target corpus is decidable by brute force.

The prerequisite for both routes is the same and it is small: a callable
combinational-cone extractor. Per the 2026-08-08 comment that is now **#872**,
filed under CAP-31's #563 with `shared_with: [306]` — which is the tell. The
extractor is the durable artifact; this capstone is a consumer of it.

## Two smaller re-alignments

**The extractor must not inherit export policy.** §3's largest named risk and Open
Question 4 both rest on `HdlExporter`'s `REJECTED = Map.of(...)` at `:460-477`.
That code **is not on master** — the 2026-08-03 evidence-pin notice says so, and I
confirmed it (`grep -n "REJECTED" src/jls/hdl/HdlExporter.java` → nothing; rejection
is policy-by-name, `HdlPolicyTest#memoryIsRejectedByName`). Good. Verilog refuses
`SubCircuit` because a module boundary must stay *readable*; lf-04 says outright
that "formal has no readability requirement." So the right answer to Open Question
4 is not "fund a ~1 wk elaborator to work around the inheritance" but a recorded
principle: **the formal extractor flattens by default and shares no policy table
with the exporter.** That deletes the risk, deletes the day-one `SubCircuit`
exclusion, and removes the largest source of anxiety around KC-09-3's percentage.

**The corpora are more valuable than the capstone and should not be inside it.**
`test/fixtures/corpus/lab/` and `test/fixtures/corpus/submissions/` with a mutation
catalogue and per-entry ground truth are ~1–2 mw and serve CAP-06's grading,
CAP-02's oracle, FEAT-009's measurement fixtures, and file-format round-trip tests.
Today `test/fixtures/` holds four files and no corpus, and the 2026-08-04 coverage
comment records that #369's own body — filed before the REPLAN that made them its
deliverables — never mentions them. Give them their own issue, land them first,
and three of this issue's numeric criteria become computable before any solver
exists. The `ElementRegistry` census the manifest is measured against is real and
stable (35 `new ElementType` entries, verified on master).

## Architectural note worth recording

ARCHITECTURE.md §"Simulation execution strategy" (#221) records the discrete-event
interpreter as JLS's *sole* strategy, and binds any future strategy to bit-for-bit
agreement with the interpreter. `jls.formal` introduces a second evaluation
semantics — untimed, structural, two-state — and deserves the same treatment: a
recorded decision that it is an **analysis plane, not a simulation strategy**, with
the binding criterion that *every* formal verdict be confirmable by the
interpreter (AC-2, generalised from counterexamples to all verdicts). Under the
refutation-first framing that criterion is total, which is the strongest possible
version of it. Open Question 1 already asks for an ARCHITECTURE.md entry about the
solver; this is the entry that actually matters.

## Endorsement

The outcome is right, central, and long overdue. Reframe the required set around
it: make the demo slice the capstone, ship refutation before proof, measure how
much of the lab corpus brute force already settles, land the corpora first, and
move four-state, the parity harness and the external oracle into the
credibility-at-scale capstone where their own §1 can justify them.
