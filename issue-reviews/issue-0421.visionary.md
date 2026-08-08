# Issue #421: TASK-0066: a subcircuit instance's implementation can be switched mid-run and the continuation is either byte-identical or a named divergence with an index and a port
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the fidelity-toggle framing, #421 builds the first machinery in JLS
for the sentence **"two models of the same thing agree, and here is where they
stop agreeing."** That is not a niche need inside #325. It is the same sentence
four separate strands of this project are waiting on:

- `docs/capability-roadmap/lf-02-compiled-evaluation.md` §5, whose plan graph
  ends in `P7-E engine-vs-engine differential harness — written alongside P7-B,
  not after`, and whose §6 risk 1 is "a scheduling divergence between the
  engines, found late";
- `docs/capability-roadmap/lf-04-formal-and-grading.md`, which opens with **"JLS
  has no representation of 'correct.' It has a representation of 'what
  happened,' and grading is a string diff over that"**;
- #202's differential oracle and #392's boundary-by-boundary bring-up;
- `ARCHITECTURE.md:359-368`, whose #221 equivalence criterion is a promise no
  code in the tree can currently evaluate.

So the arc is right and the timing is right. The design discipline is unusually
good — null test written first, `Inconclusive` as a first-class arm, P6's
reflective signature restriction, P7's bytecode scan, one engine change and no
second loop. My objections are not about competence. They are about the issue
choosing, four times over, the *smaller* version of the thing it is building.

## R1 — `runUntilQuiescent` is the buried treasure; ship it first, alone

The issue calls it "the one engine change," a means to an end, gated behind #389
and behind a 4-task feature whose own §7 declares a hard-stop kill criterion.
Look at what it actually is: **JLS has no combinational-loop diagnostic today.**
`Simulator.runEventLoop` (`src/jls/sim/Simulator.java:215-243`) terminates on
`now > maxTime`, but `SubCircuit.react` posts at `now` (`:621-636`) and
Splitter/Binder/Extend are zero-delay, so a zero-delay cycle spins forever at one
timestamp with no message and no progress — `maxTime` never fires. lf-02 §2.4
names the fix and calls a named oscillation report *"a far better diagnostic than
today's silent hang."*

A budgeted quiesce loop plus a "this design did not settle; these elements kept
re-firing" diagnostic is a **standalone, user-facing, shippable feature** that
needs no `implId`, no `Boundary`, no sealed `Verdict`, and no unratified
contract. It is also independently wanted by #363 (checkpoint at a quiescent
point), lf-03 §"`SimCheckpoint checkpoint()` — snapshot at a quiescent point",
and lf-02's Mode C ("the whole design settles per edge"). Split it out, land it
first, give it its own coverage budget under `pom.xml`'s 0.930/0.920/0.845 floors
rather than amortizing it into a harness PR. If #325 is later killed by its own
K4, this survives — and it is the piece students would notice.

## R2 — the verdict lattice is one arm short, and lf-04 already named the arm

`Verdict` is sealed over `Equivalent`, `Divergent`, `Inconclusive`,
`PortMismatch`, and §7.4's integrity argument is that "I don't know" is never a
pass. Correct — and then applied to only one of the two ways this harness can not
know. §7.3 admits `Exhaustive(totalInputBits) (<= 20)` and `Seeded(seed, words)`.
#325's own coverage rule is 10^6 vectors above 16 bits. The demonstration #325
names is an **ALU subcircuit, drawn versus compiled** — call it 32+32+opcode ≈ 68
input bits. 10^6 samples of 2^68 is a probability-zero slice, and this harness
will print `Equivalent(1000000)` over it.

lf-04 §"Sequential equivalence" already solved exactly this naming problem, in
this repository, in the strongest available language: bounded model checking
*"Finds bugs fast; never proves. Report it as `NO_COUNTEREXAMPLE_WITHIN_K`,
**never** as `EQUIVALENT`. This distinction is the entire integrity of the
feature."*

Concretely: make the passing arm `NoCounterexample(stimulusDescriptor)` unless
the stimulus was exhaustive over the whole input space, in which case
`ExhaustivelyEquivalent(w)`. Cost today: a rename and one extra arm. Value: the
name stops overclaiming on the feature's own flagship demo, and it leaves a
`Proved(certificate)` arm free for the day lf-04's miter-plus-SAT path answers
the same question for combinational boundaries **completely** rather than by
sample. That is the ladder — sampled at runtime, then proved — and #421 should
declare itself the lower rung explicitly instead of occupying the top rung's
vocabulary.

## R3 — the divergence artifact should be a `-t` file, not a Java object

I am explicitly disregarding §7.1 ("N/A for user-visible surfaces") and §14's
"asserted on the **report text**." §5's P2 is called the most important test in
the task, and it is right that it is — but its product is an assertion inside
`test/jls/sim/equiv/`. Nobody outside `mvn verify` can re-run it.

lf-04 §"Counterexample rendering" states the better shape and the reason:
*"**A `-t` file.** `docs/batch-interface.md` §2 is a frozen contract (§6), so a
counterexample written as `-t` is guaranteed loadable, replayable, diffable and
distributable forever."* The grammar exists, `SigSim` already parses it, and a
combinational counterexample is one line per input pin. Making `Divergent` able
to *write the vector that produced it* turns this from a CI-internal gadget into
the debugging tool that #392's 580-element bring-up will actually live in: "the
harness says boundary `alu.adder` diverges at index 4113 on port `cout` — here is
the file, load it and look at the schematic." Same code path, ~50 lines, and it
is the artifact #202, #390/TASK-0073 and lf-04's `-cex` all separately want.

## R4 — ship the observation function, not just the comparator

The real invention in this issue is Ω_B: **settled values per declared instant,
indexed and not timestamped**. That single object is simultaneously lf-04 tier-1's
"output functions equal given equal current state," lf-02 Mode C's oracle ("same
settled values at every clock edge, not same VCD"), #390's retirement index, and
#202's differential oracle. Four consumers, one concept — and #421 keeps it
private (§7.7: "Nothing is written to disk by this task"; `Observation` never
leaves `compare`).

Serialize it. A per-boundary observation trace in a stable text form makes the
comparator a `diff`, makes the checkpoint a boundary-scoped snapshot at instant
*n*, and — importantly — **strengthens P5**. §11 correctly concedes that
byte-identity over a VCD plus the watched-element report is "necessary, not
sufficient," since two internal states can produce the same output. Asserting
byte-identity over the observation trace *and* the versioned state blob (both
already in memory) is strictly stronger for zero extra machinery, and it is the
gate on which the issue stakes everything.

## R5 — do not mint a second notion of "module boundary"

`jls.sim.Boundary` derives the port surface from `SubCircuit`'s `inmap`/`outmap`
at runtime. JLS already has a port surface: `HdlModel.Port(name, direction, bits,
…)` (`src/jls/hdl/HdlModel.java:43`), built by `HdlExporter.buildModel`'s
net-unioning walk — which lf-02 §5 wants extracted as a shared *elaborator*
"wanted by 3 consumers" and on which lf-04's entire formal path rests. After
#421 lands, the project has two, with no stated relationship, and the elaborator
would make three.

Add one test: for an exportable subcircuit fixture, `Boundary`'s ordered port
list agrees with the `HdlModel` port list for the same definition. One day of
work, and it permanently pins the two concepts together — which is also the
bridge that lets the same `Boundary` later serve as the cut point for a SAT
miter instead of needing a parallel notion invented in `jls.formal`.

## R6 — Open Question 1: neither (a) nor (b)

The issue offers "ratify the contract by landing the harness" or "implement
against it while it drifts," recommending (a). I would answer **(c): record the
`ARCHITECTURE.md` decision block first, in the #221 style, then implement.** That
file's seven recorded-decision blocks — each with rationale and an explicit
revisit trigger — are the most valuable governance artifact in this repository,
and they work precisely because a decision is taken *as a decision*, not
inherited from whatever a PR happened to do. Ratifying a normative contract as a
side effect of merging its first implementation inverts that.

Two further notes on the same footing. First, `docs/parity-contract.md` **does
not exist in this checkout at HEAD** (`bd54461`), nor does `04-mechanisms.md`,
and the evidence commit `2d0ca9d` is not an object in this repo — both #325 and
#421 cite them with line numbers. The visionary consequence is not pedantic: a
four-task feature and its hard-stop kill criterion currently rest on a document
the tree does not contain. Second, #325's coverage rule says exhaustive at w ≤ 16;
#421 §7.3 says ≤ 20. Small, but it is the one number that decides how often the
`Equivalent` arm of R2 gets to lie.

## R7 — the alignment gap, stated plainly

§12's Related Work table lists nine issues and not one capability-roadmap
document. #421 reasons entirely inside the #325 subtree while building the exact
mechanism that lf-02 §5 and lf-04 both specify from a wider vantage. Add both,
and state the relationship in one sentence: *this task is the runtime, sampled
rung of an equivalence ladder whose upper rungs are lf-02's engine-vs-engine
differential harness and lf-04's proof-based `-equiv`; it deliberately builds the
rung that needs no new IR, and names its verdicts so the upper rungs extend the
same type rather than replacing it.*

## On the deeper question: is the fidelity toggle the right shape at all?

Worth asking, since #421 spends P7 and a bytecode scan defending the claim that a
toggle is a model change and not a second execution strategy. The defense is
sound — a behavioural binding is one element with one `react()`, structurally
indistinguishable from `Adder`'s lumped `30 * bits` or `RegisterFile`'s collapse
of ~95 elements. But if the *motivation* is speed, lf-02's Mode T claims
−30…−40% with **zero** semantic change, no per-instance attribute, no abstraction
banner, and every golden byte-identical — strictly better on every axis, and
`ARCHITECTURE.md:359-368` is satisfied "as written, with no amendment."

I do not think that redirects this issue, because #325 §7's soft case already
concedes the point ("the *motivation* for a non-structural arm changes but the
boundary discipline does not"). The toggle's real value is **mixed-abstraction
bring-up as a taught method** — the way real hardware verification actually
proceeds — with named refusals and a mandatory banner as the pedagogy. That is a
genuinely good thing for an educational simulator to become, and it is a better
justification than speed. Say so in #421, and then the conclusion in R3 follows
inevitably: a bring-up methodology's harness belongs in the user's hands, not in
the test package.
