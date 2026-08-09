# Issue #322: FEAT-026: a signal can say "unknown" and "undriven" per bit, and contention resolves as a property of the driver set rather than of draw order
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machine block away and the claim is: *JLS should stop lying about what it
does not know.* That is the right claim, and it is the single highest-leverage
semantic change available to this project. The README's whole arc is JLS becoming a
bridge rather than a sandbox — VCD export for GTKWave/Surfer, structural Verilog
export, Yosys-synthesized netlist import, a container image for autograders, the
`riscv/` trajectory. Every one of those destinations is four-state. `README.md:137`
already ships an apology in the flag documentation — "note JLS's two-state-plus-HiZ
semantics" — and `src/jls/hdl/imp/ImportSummary.coercedX` is a counter whose entire
job is to report information JLS destroyed after Yosys parsed it faithfully. The
two-state domain is the largest fidelity gap between JLS and the toolchain a student
graduates into, and it is load-bearing on six capstones. Endorse the goal without
reservation.

What I am reframing is the *routing*: who owns the value type, where the first cut
falls, and how the compatibility promise is honoured. As filed, the issue is aimed
correctly and plumbed backwards.

## The dependency inversion is the finding

The 2026-08-08 roster comment records that TASK-0056 — the value type itself — left
this feature and is now #881 under **#232**, with the absorbing rationale that "#322
consumes it but does not own it." I read that as a governance-tidy answer to an
engineering question, and I think it is wrong in a way that matters more than
anything else on this issue.

Read #232's own §3 and §4. Its contract states: *"External contracts consumed/provided
— unchanged by definition: `.jls` save format, batch `-t` stdout, and the VCD profile
are byte-exact; HiZ/undriven ... behave identically"*, and its invariants require
*"Golden/round-trip suites byte-identical: save format, batch stdout, VCD."* #322's
invariant 2 requires the exact opposite: goldens **must** change where the semantics
changed, and criteria 2 and 4 demand new per-bit `z` and new `x` at every rendering
point. A feature whose success criterion is "no observable byte changed" cannot be
the vehicle for a change whose entire point is that some observable bytes must change.
#881 inherits a contract that forbids its purpose.

Worse, #232 §1 outcome (a) and §7 make the swap conditional on a falsification gate:
*"if the baseline refutes H1 (BitSet allocation not a meaningful cost share), the
feature closes with the profile as evidence and no swap is built."* The roster comment
notices this and files it as a contingency for a scheduler to remember. It is not a
contingency; it is a structural error. The semantic foundation of #295, #297, #301,
#304, #306 and #310 is currently downstream of an allocation benchmark that its own
issue says may come back null. And the two parents decide the same question
incompatibly: #232 OQ-2 calls the HiZ encoding "decidable by the swap child; rides
along", while #322 OQ-1 calls the plane count load-bearing on a filed capstone
commitment. Two issues, one type, two decision procedures.

**Reframing 1 — put the currency back under semantics.** The representation is being
changed for a reason, and the reason is the alphabet, not the allocator. #881 (or its
successor) should declare `part_of_feature: 322`, or stand as a neutral `jls.core`
kernel issue that #232 *consumes*; and #232's role should invert from gate to
instrument. Its H1 profile and H3 width histogram are genuinely valuable — they are
what should decide `Word` vs `Wide`, the 64-bit threshold, and whether three planes
cost what keystone C measured. That is measurement informing an encoding, which is
the right use of a benchmark. Letting it decide *whether the alphabet exists at all*
is not. The correct edge is `#322 blocks #232`'s swap child, not the reverse.

## The seam I would cut along instead

The issue's decomposition is representation → fold, justified by "the fold's operator
is defined over the representation's code points." True as stated, and it produces a
17–22 week critical path (`docs/capability-roadmap/README.md:215-220`) across 338
`BitSet` references and 27 `react` bodies before anything is observable. I verified
those counts at HEAD; they are exact. But the sequencing means criteria 2 and 4 — the
only criteria a student or an instructor can see — are evaluable last, which the issue
itself flags in §6 as "a bad place to discover a code-point choice was wrong."

**Reframing 2 — observability before semantics.** `WireNet.propagate` already computes,
at zero additional cost, exactly the two facts the pedagogy claim rests on. The loop at
`src/jls/elem/WireNet.java:454-471` walks every attached `Output` and already
distinguishes *no active driver* from *one active driver* from *two or more
disagreeing*. Today the first fact is thrown away (`value = actual` with `actual ==
null`, then coerced to zero at every reader) and the third is spent on a modal
`TellUser.warn`. Ship a **net-condition side channel**: a per-net, per-time condition
alongside the value, routed to the wire rendering, the trace, and — behind a flag —
VCD `x`. That is one method plus the rendering paths. It changes zero `react` bodies,
zero goldens by default, and delivers the two headline classroom wins ("a floating
input becomes visible", "the datapath visibly goes red") in weeks rather than the
better part of a year.

It also unblocks the fold. Order-independence does **not** require X in the value
alphabet: the condition lattice {undriven, driven(v), contended} is already
commutative, associative and idempotent, with `undriven` as identity and disagreement
absorbing to `contended`. #391 can be built, proven and landed against that lattice
today, and the later alphabet migration substitutes `Σ` for the lattice's payload
without redoing the proof. The issue's §2 rejects "keep `null` and add a sentinel
`BitSet` for X" on the grounds that the compiler then cannot force every reader to
handle the fourth state — sound for a *value-channel* sentinel, and not applicable to a
side channel, which makes no completeness claim and which the full migration deletes
on arrival. Note the roadmap already lists six such structural workarounds it will
delete; adding a seventh that is designed for deletion is a cheap trade for pulling
the visible payoff nine months forward.

## The compatibility promise the issue does not name

`docs/batch-interface.md` §4.3 does not merely describe today's behaviour — it freezes
it: *"the four-state VCD alphabet is used as `0`, `1`, `z` — **`x` never appears**"* and
*"mixed vectors like `b1z0` cannot occur."* §6 makes that a stability contract requiring
*"a major version bump, **or** a compatibility flag that keeps the format specified here
available unchanged."* The README advertises this contract to autograder authors. #322
modifies `docs/simulation-semantics.md` §2 and §9 in its DoD and never mentions
`batch-interface.md` at all — yet criteria 2 and 4 violate two of its frozen clauses
directly. `ARCHITECTURE.md:359-368` is the second unlisted casualty: the #221 recorded
decision binds any future execution strategy to "the two-states-plus-HiZ value domain
and multi-driver/tri-state resolution (§2, §9)". Both documents need amending in this
feature, and the DoD should say so.

**Reframing 3 — strictness per circuit, not visibility per view.** OQ-3 asks what a
first-year sees and recommends "X rendered distinctly but only in views past the
first-year default." I would disregard that recommendation. Hiding X in a view means
the value the simulator holds and the value the student sees diverge — which is
precisely the dishonesty this feature exists to end, reintroduced as a UI setting. It
also leaves invariant 1 ("no existing lab changes behaviour") in flat contradiction
with criteria 2 and 4, reconcilable only by a review discipline that invariant 2 admits
is a human check.

Make it structural instead: a `semantics` level saved in the circuit file — `compat`
for files that load without one, four-state for new files, switchable in the editor and
by CLI flag. Then no existing lab changes behaviour *by construction* rather than by
vigilance; every existing golden stays byte-identical because compat mode emits the
frozen §4.3 profile; the batch stability promise is honoured through the exact escape
hatch §6 already names; and the instructor opts a course into X in the week they teach
it. This costs a saved attribute and a format-version bump — which #341 is already
paying for its net-kind state, so the two should share one bump. It is a permanent
dual-semantics maintenance cost, and I think that cost is worth it: it converts the
single largest risk in the feature (invariant 4's 27 silent coercion changes) from a
correctness hazard into a mode boundary.

## Smaller structural observations

- **The risk-bearing work has no owner.** §4 invariant 4 makes each of the 27 `react`
  coercions a recorded decision, §6 names them as the parallelism the schedule depends
  on, and the cost note says the residual "has no task id because the registry's task
  space is closed at TASK-0112." A closed registry is a bookkeeping fact; it should not
  be why the highest-risk, most-parallel work in a 28–36 mw program is unfiled.
- **Don't-care needs none of this.** `TruthTable.java:79` already stores `2` for a
  don't-care and `react` destroys it. Recovering that is a *specification*-side type
  (the roadmap's `Bits4`) consumed at synthesis and expectation-checking time — it
  touches no `Put`, no `WireNet`, no value channel. It is arguably the highest
  pedagogy-per-line item the roadmap lists and it is being carried, unnecessarily,
  behind a nine-month kernel migration. Cut it loose.
- **Evidence commit.** `2d0ca9d` does not resolve; the third comment re-pins to
  `8288226`. I re-derived the load-bearing anchors at HEAD and they all hold exactly:
  `Put.java:385` is `protected @Nullable BitSet currentValue;`, the net-order scan is
  `WireNet.java:454-471`, 27 `react` implementations, 35 `ElementRegistry` types, 338
  `BitSet` references under `src/jls/elem/`, and `src/jls/core/` still holds only
  geometry. (The roadmap's own "33 registered types" at `README.md` §P2 is stale
  against 35 — worth a one-line fix while the doc is open.)

## Verdict

**endorse-with-reframing.** The destination is right and the project should get there.
Three changes to how it gets there: (1) take the value type back from #232 so a
performance falsification gate cannot strand six capstones, and invert that edge so the
profile informs the encoding instead of authorizing the alphabet; (2) land the
net-condition side channel and the fold's algebra first, so contention and floating
inputs become visible in weeks and the code-point choices are validated against real
rendering before 338 references move; (3) replace "hide X from beginners" with a saved
per-circuit semantics level, which makes invariant 1 structural, honours the
`batch-interface.md` §6 stability promise by the mechanism that document already
specifies, and lets `docs/batch-interface.md` and `ARCHITECTURE.md:359-368` be amended
honestly rather than left unmentioned.
