# Issue #836: TASK-C333-4: a design whose boundary lookahead is too low is refused by name, naming the boundary that caused it, instead of running slowly and silently
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this task is really for

Two different things are bundled here, and only one of them is a real capability:

1. **Lookahead is a derived, per-boundary quantity** — `min` over the nets at the cut of
   the driving element's delay. That is correct and worth having.
2. **A design whose lookahead is below a declared constant is refused before the run.**
   That is not a capability; it is a *tourniquet on a protocol wound* inflicted upstream
   in TASK-C333-3 (#834), which says outright that it picked the barrier-synchronous
   shape over null-message "because it makes the checkpoint coherence of TASK-C333-6
   trivial and the determinism argument short."

#836 is the bill for that convenience. The Outcome paragraph even concedes it: "a
conservative discipline on a design with poor lookahead degrades toward serialisation."
The task's response is to refuse the design. The alternative nobody considered is to
refuse the *protocol*.

## The number that decides this

`riscv/` is the project's only design at the scale #333 exists to serve, and its
delays are on disk:

- `riscv/jlsbuild.py:154-189` — every generated gate, adder, mux and decoder takes
  `delay: int = 1`; `extend` takes `delay: int = 0`.
- `riscv/build_cpu.py:118` — `clock_cycle: int = 2000`; `riscv/verify.py:15` —
  `HALF = 1000`, so a rising edge every 2000 ticks and `time_limit = 2 * steps * HALF`
  (`verify.py:41`).
- `src/jls/JLSInfo.java:69` — `defaultTimeLimit = 100000000`.

So the flagship large design has **L_min = 1 tick** at essentially any cut, and a
10,000-instruction run spans 20,000,000 ticks. Under a fixed-window barrier the round
count is `T_end / L_min`: **20 million barrier rounds**. Cut at registers instead
(`Register.defaultPropDelay = 50`, `src/jls/elem/Register.java:54`) and it is 400,000
rounds — still one round-trip per 50 ticks of a design that settles in far fewer events
than that. At 100 µs per network round trip, the *barrier alone* costs 33 minutes on a
run that takes seconds whole.

This is not an edge case the threshold screens out. It is the condition of every JLS
design, because JLS's time base is dense (gate delays of 1–50 against clock periods of
thousands) and its event density is sparse. An honestly derived threshold — one whose
"basis recorded next to it" (AC-3) is a real measurement rather than a plausible number —
would land **above every lookahead a JLS circuit can produce**, and the refusal would
refuse everything. Any threshold low enough to let the riscv CPU through (i.e. ≤ 1) is
identical to the test `L > 0`, which is a *legality* question, not a *quality* one.

There is no third value. AC-3 asks for a constant that does not exist.

## AC-4 cannot fail

"No existing fixture gains a new refusal." No existing fixture is partitioned — #332 is
still five unfiled scopes, and `git grep -inE "lookahead|partition|barrier" src/ test/`
returns only wire-net partitioning and a collab comment. The refusal path is unreachable
from every fixture in the tree at any threshold, including `Long.MAX_VALUE`. The
criterion is a guard that is armed against nothing and will stay green while the feature
refuses the only design anyone cares about.

The criterion that would have caught this: *the riscv CPU (`riscv/make_cpu.py` output),
cut at its own module boundaries, is not refused.* It would fail immediately at any
threshold above 1 — which is the finding, and the reason it should be the criterion.

## Reframing A — the legality half already has a home, and it is #332

Strip the threshold and what remains is: *this cut crosses a net no delay-bearing
element drives.* Per `docs/simulation-semantics.md` §6.1–6.2, wires are ideal and
`Splitter`, `Binder`, `Constant`, `InputPin`, `OutputPin` and **the subcircuit boundary**
are all delay 0 — and the subcircuit boundary is exactly where a human cuts a large
design. So the natural cut has L = 0, and L = 0 is not "slow", it is a deadlock: no
partition can ever advance.

That is a cut-legality rule, decidable statically, in the same walk as #332's fourth
planned scope: "UNCUTTABLE-CONSTRUCT REFUSAL AT PARTITION TIME: a cut crossing a
combinational cycle spanning partitions is refused by name." A zero-lookahead cut *is* an
uncuttable construct — same phase, same graph walk, same message shape, same test
harness. #332 also records the reason it lives there: "criterion 5 is the falsification
guard for the whole capacity capstone, and it belongs here. Refusal is a property of the
partitioner, not of the transport." #836 rejects that ruling without arguing against it,
and duplicates the mechanism one feature downstream.

Implementation is a few lines, and the seam already exists: `src/jls/elem/Timed.java`
(#78's capability interface) is implemented by every delay-bearing element and by no
zero-delay one. Lookahead for a cut net is
`min over drivers (d instanceof Timed t ? t.getDelay() : 0)`, and `L == 0` is refusable.
No constant, no basis to invent, no threshold to defend. AC-1's unit test survives intact
and becomes a test of that expression.

## Reframing B — the protocol change that deletes this task

Fixed-window advance (`t ≤ min_j (T_j + L_j)`, #834) is the *weakest* form of Chandy–
Misra–Bryant and the only one whose cost is `T_end / L`. The standard repair is to carry
the sender's **next-event time**, not just its clock: a partition announces "I will send
you nothing before *t*", where *t* is `min(next local event time, now) + L`. Rounds then
scale with **boundary traffic**, not with elapsed simulated time. On the riscv datapath,
boundary events occur a handful of times per 2000-tick clock cycle, so a design with
L = 1 costs a few thousand rounds instead of 20 million — a four-order-of-magnitude
difference that comes from the protocol, not from the design.

Under that protocol, low lookahead is no longer pathological, `L > 0` is the only
requirement, and #836 has nothing left to refuse. Checkpoint coherence (#839), the reason
#834 gave for the weaker shape, is recoverable by *forcing* a global-minimum barrier at
checkpoint points — you pay the synchronisation once per checkpoint instead of once per
lookahead window. That trade is strictly better and it is the trade every production
conservative simulator makes.

I am therefore disregarding AC-2, AC-3 and AC-5 as stated. The refusal they specify is
well-engineered (name the boundary, refuse rather than warn, declare the constant once) —
and it is well-engineered scaffolding around a decision that should be reversed instead.
Fix #834's advance rule; #836 shrinks to AC-1 plus a `L > 0` legality clause in #332.

## Reframing C — what the user actually needs is a report, not a refusal

Even granting the threshold, a bare refusal is a dead end. The author whose only natural
cut is a subcircuit boundary is told "no" and given nothing: no second-best cut, no cost,
no way to ask "what if I cut at the register file instead?" The instrument that serves
that user is a **partition report** — `jls -partition-report design.jls cuts.json`
printing, per boundary, the cut nets, the driving elements, `L_min`, and the predicted
round count `T_limit / L_min` against the run's own `-d` limit (which is known before the
run, `JLSStart` flag table). That turns an opaque constant into an arithmetic the author
can read and act on, and it prices cuts *before* anyone builds a cluster to discover they
were bad. It also fits `docs/batch-interface.md`'s existing discipline of stable,
documented headless output far better than a new named constant in the engine does.

Note the dimensional error the report exposes: AC-3's threshold is in *time units*, but
the quantity that governs whether a run degrades is *rounds per run* — dimensionless,
and a function of the time limit as much as of the cut. A constant expressed in the wrong
units cannot have a defensible basis, which is precisely why AC-3 struggles to name one.

## Alignment with the project's arc

ARCHITECTURE.md's #221 decision declines even an in-process levelized compiled pass as
"premature optimization until CPU-scale designs are actually common," with the revisit
trigger "a concrete CPU-scale design on the `riscv/` trajectory that is unusably slow
interactively." #836 asks the project to spend a named constant, a refusal path, a
message contract and a fixture on tuning a *distributed* simulator's efficiency — a
strategy far beyond the one already declined — for a user who does not exist, using a
number nobody can measure. Meanwhile the same `riscv/` trajectory that would trigger #221
is the design this task's threshold would refuse.

The part of #836 that strengthens the arc is small, cheap and startable today: derive
lookahead per boundary from `Timed`, and make `L == 0` an illegal cut in #332. The part
that pulls against it is the threshold, and it should not be built.

## Concrete restatement

1. **Into #332** (uncuttable-construct refusal scope): a cut crossing a net with no
   delay-bearing driver is refused by name at partition time, naming the boundary and the
   zero-delay element. Unit-tested against a cut whose min delay is known by
   construction. *(This is AC-1 + AC-2 + AC-5, minus the constant.)*
2. **Into #834**: replace fixed-window advance with next-event-time advance; force a
   global barrier at checkpoint points for #839. Re-price both.
3. **New, small**: `-partition-report` printing per-boundary `L_min` and predicted round
   count against the run's time limit — advisory, no refusal.
4. **Delete**: the named threshold constant, its basis paragraph, and AC-3/AC-4.
5. **Replace AC-4** wherever a refusal does land, with: the `riscv/` CPU at its declared
   cut is not refused.
