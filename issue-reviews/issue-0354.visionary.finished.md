# Issue #354: FEAT-006: an hours-long batch run has no unannounced ceiling — it can be suspended, watched, interrupted cleanly, and address a real guest image
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the four ceilings away and one sentence remains: *the RV32I structural boot on the
`riscv/` trajectory takes 1.66–1.72 h, and JLS cannot survive its own run.* Every audience
in § "Intended Audience" is downstream of that single workload. The capability is real and
I endorse it. But the issue treats 1.7 hours as weather — a fact to be endured with better
raincoats — and it is not. `docs/capability-roadmap/keystone-c-performance.md` measured the
loop at **8,090 simulated CPU cycles/s warm, ~1,100–1,450 end-to-end, of which 4.9 % is
digital logic**; the other 95 % is `PriorityQueue`/`HashSet`/`BitSet` bookkeeping. The boot
is not long because the circuit is big. It is long because JLS pays a ~20× tax and then
schedules it.

That matters here because `ARCHITECTURE.md` § "Simulation execution strategy" (#221) records
the decision *not* to build a compiled pass behind a revisit trigger: *"a concrete CPU-scale
design on the `riscv/` trajectory that is unusably slow interactively."*
`docs/capability-roadmap/lf-02-compiled-evaluation.md` § 0 complains, correctly, that this
trigger "is not a testable condition." **#354 is the strongest evidence in the tracker that
the trigger has fired, and it does not mention #221 at all.** That is the alignment gap.

### Reframing 1 (goal level): the heartbeat is an instrument, not a courtesy

TASK-0014's heartbeat is specified as an ergonomic nicety — "a progress line appears on a
stated interval", counters listed as "events retired since last beat, queue depth,
wall-clock elapsed", explicitly ephemeral, § "Open Questions" 3 leaning opt-in-so-invariant-1-is-trivial.
Design it instead as the **measurement surface that makes #221's revisit trigger testable**:
emit sustained events/s and, where a clock element exists, simulated cycles/s, and emit a
final summary line at end-of-run unconditionally. Then every long run anybody ever does
produces the number keystone C had to build a bench harness to get, `riscv/bench_kernel.py`
becomes a consumer rather than the sole source, and "unusably slow" becomes a threshold
somebody can write down. Same code, same week of work, an order of magnitude more leverage:
FEAT-006 stops being a palliative for the interpreter tax and becomes the thing that decides
whether the tax gets paid off. Cost: invariant 1 has to be argued rather than obtained
trivially (stderr, not stdout, so § 4.4 still holds).

## Four seam-level redesigns

### 1. `pause(boolean)` is the wrong object, and TASK-0014 as written hangs autograders

`Simulator.pause(boolean)` is not an operator control. Its only non-GUI caller in the tree is
`Pause.react` (`src/jls/elem/Pause.java:177`) — an *in-circuit* halt element a student wires
to a comparator, which `docs/capability-roadmap/lf-03-causal-debug.md` § 3 correctly calls
"a breakpoint, hand-built out of logic." Redefining `BatchSimulator.pause(true)` from
"terminate" to "suspend" therefore does not only change what an operator can do; it changes
what a **circuit** does. A headless grading run over a submission containing a `Pause`
element stops today and, after TASK-0014, **blocks forever with nobody on the other end to
call `pause(false)`** — inside the CI lane (#317) and the campaign (#350) this feature exists
to serve. It also silently rewrites a normative document the issue never lists:
`docs/simulation-semantics.md:465-469` states *"in batch mode pause is meaningless and is
treated as stop (`BatchSimulator.pause`)"*. § 4 invariant 5 routes the byte budget through
`docs/file-format.md` review but nothing routes this through simulation-semantics review.

Concrete alternative: **leave `pause(boolean)` alone as the circuit-facing halt request** and
put suspend/resume on a separate run-control object that `beforeEvent()` consults. This is
strictly better than overloading the element callback, because the same seam already has two
other claimants: `docs/capability-roadmap/lf-07-api-and-platform.md:327-329` names
`Simulator.beforeEvent` as the mechanism for the `session.*` API, and #324's console must
drain inside the same loop. One `RunControl` (suspend / resume / interrupt / step-until),
three consumers, no semantics change to any element. § 4 invariant 3 ("`stop()` stays
terminal and distinct from `pause`") survives untouched, and the accidental fourth state
— circuit asked to pause, nobody listening — never exists.

### 2. TASK-0012 solves an expressible problem and skips the inexpressible one

`-d` already parses any positive `long` (`src/jls/JLSStart.java:1062-1074`), so `-d
9223372036854775807` is an unbounded run today; and `BatchSimulator.displayOutcome()`
(`:557-570`) already prints `Simulation Time Limit at <n>`, so the ceiling is not literally
unannounced. A sentinel is two days of ergonomics over a thing that works.

What genuinely does not work is the far more valuable adjacent thing: **the reason a run
ended is English prose on stdout and the process exits 0 either way.** "Simulation Complete",
"Simulation Time Limit" and "Simulation: No More Activity" are indistinguishable to
`autograde.py`. `docs/batch-interface.md:48` tells graders *"treat exit status, not stream
placement, as the boundary"* — advice that cannot be followed for the single most important
distinction a grading batch makes, because a truncated run scores as a pass. Reframe
TASK-0012 as **a machine-readable run outcome** (distinct exit status or a stable
`jls: outcome: …` stderr line), keep the sentinel as a one-line rider. That serves the
instructor audience, #350's aggregation and #317's lanes — none of which should ever run an
unbounded job — far better than infinity does. Note also that `InteractiveSimulator`
clamps `maxTime` into `int` range (`src/jls/edit/InteractiveSimulator.java:552-556`), so a
sentinel is batch-only and the two front ends diverge; § 3 does not say so.

### 3. TASK-0013: I am disregarding open question 2's acceptance criterion

The § 3 arithmetic ($B_\text{dense}(n) = 65n/8$, headroom factor $h$, a system property) is
careful ceremony over a fact that dissolves on inspection: `DENSE_CAPACITY_LIMIT` is a
**store-selection heuristic, not a capacity cap**. `newWordStore()`
(`src/jls/elem/Memory.java:1233-1237`) falls back to `SparseWordStore` past the limit; a
12 MiB guest is *addressable today*. What it is not is affordable — `SparseWordStore` is a
`HashMap<Integer,BitSet>` at ~100 bytes/word (`:1160-1216`), so 3 M words costs ~300 MB.
The BRIEF's "cost jumps" is exact; the issue's "can address enough memory" is not.

So the right fix is not to move the cliff up behind a declared headroom factor exposed as a
tunable — that is a new user-visible configuration surface, a `docs/file-format.md` review,
and an open question, all bought to relocate a discontinuity. **Remove the discontinuity:**
page the store — a two-level table of `long[]` blocks allocated on first touch, the way every
emulator holds guest RAM — and there is one store, cost proportional to *touched* words,
no threshold, no $h$, no system property, no open question 2, and no format-adjacent change
at all. § 4 invariant 5 becomes trivially true instead of needing an argument.

Paging also composes with the part of TASK-0013 that is unambiguously right, and is
mis-scoped as secondary: `initSim` does `mem = initMem.copy()` (`:1309`) and **retains both
for the whole run** — the doubling is not transient, as § 5 criterion 4 assumes ("does not
*transiently* allocate a second copy"). Pages make copy-on-write nearly free: share the
initial image, fork a page on first write, and reset stays O(1). That is the substance of
this task; the byte budget is not.

### 4. The fifth ceiling is the one that actually kills the stated run

`BatchSimulator.eventTrace`/`probeTrace` (`:24-36`) grow one `TraceSample` per value change
for the entire run, with **no bound**, and `toVcd()` (`:384`) then materializes the whole
dump into a `StringBuilder`. The interactive side has a bound —
`Trace.MAX_RETAINED_CHANGES = 100_000`, "roughly 14 MB" (`src/jls/edit/Trace.java:25-32`) —
batch has none. An instructor running the § "Intended Audience" grading batch with `-vcd`
OOMs hours before `DENSE_CAPACITY_LIMIT` is ever consulted. The issue pushes this to #353,
but #353 is scoped as *complexity* (a 3× peak, byte-identical output), not as *unbounded
accumulation*: making a materialized dump cheaper by a constant factor does not make an
hours-long trace fit. A streaming VCD writer — emit value changes as they happen, hold no
history — is the fifth ceiling, belongs in this feature's conjunction ("no one of the four
fixes alone makes it runnable" is stated, and is false as long as this one is outside), and
happens to be what #324's live console transcript wants anyway.

## Where the issue is right, and should not be diluted

- TASK-0011 as an **adjudication before a fix** is exactly correct, and the requirement that
  a test cite the decision by name in its failure message is a practice worth spreading. The
  recommended default (re-queue) is right: the interactive simulator lets a user raise the
  limit and resume, so today's drop is permanently lossy. Keep it.
- Refusing to unify `stop()` and `pause` (invariant 3) is right for the *terminal-vs-resumable*
  reason, even though § 1 above says the resumable one belongs on a different object.
- The `blocked_by: [353]` ordering — "raising the time limit without fixing the quadratic
  parse converts a fast failure into a slow one" — is a genuinely good piece of sequencing.

## Net

Endorse the capability; reframe the goal and three of the four children. Concretely: make the
heartbeat the instrument that decides #221; move operator suspend off `pause(boolean)` onto a
`beforeEvent()`-backed run control shared with #324/lf-07; make TASK-0012 about a
machine-readable outcome rather than a sentinel; page the word store and share the initial
image copy-on-write instead of declaring a byte budget with a headroom factor; and pull the
streaming trace writer into this feature, because without it the hours-long run this issue is
named for still does not finish.
