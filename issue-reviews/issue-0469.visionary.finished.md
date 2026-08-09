# Issue #469: TASK-0014: an hours-long batch run can be paused, watched and interrupted without losing its output — and pause stops being a second name for stop
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the three deliverables back to the question they answer and it is one
question an instructor asks at hour three: *is this run alive, and if I have to
kill it, do I keep what it made?* The capability is right and it is squarely on
the project's arc — `docs/grand-architecture.md` §1 names the headless batch
surface as a co-equal front end, and the RV32 trajectory (#200/#201/#202) is
what turns a "long run" from hypothetical into the normal case.

But the three deliverables are not equally load-bearing, and the issue never
separates them:

- **Clean interrupt** is the one the audience actually consumes, and it needs
  no contract change (O7 is correct and well argued).
- **Heartbeat** is the one instructors asked for, and it is designed in the one
  way that cannot answer the question it exists to answer (below).
- **Pause** has, after this task lands, *no operator-facing caller at all*.
  There is no `-pause` flag; nothing in `JLSStart` can reach it. Its real
  customers are #363's checkpointer, #214's panel, and #350's campaign runner.
  That is a fine thing to build — but the abstract's capability sentence ("an
  hours-long batch run can be paused") is not delivered to the stated audience
  by this task, and that should be said plainly rather than implied.

Four reframings follow. I endorse the goal; I would rebuild the middle of it.

## Reframing 1 — pause belongs in `Simulator`, not a second time in `BatchSimulator`

§8 says: read `InteractiveSimulator`'s pause, but do not copy its Swing-timer
structure into `jls.sim`. Right about Swing, wrong about the conclusion. The
correct move is not "don't copy it" — it is **extract it downward**.

`src/jls/edit/InteractiveSimulator.java` already parks the sim thread inside
`beforeEvent()` (L736-L810) on `pauseSem.acquire()`, with `paused` volatile and
`stop()` releasing a permit (L960-L962). Note *its* predicate is not stop-aware
at all — it works only because `stop()` happens to release a permit, and the
scar tissue is visible at L598-L602 (`pauseSem.drainPermits()` because a
leftover permit from a previous run's `stop()` made the first Pause fall
through). TASK-0014's proposed monitor-with-predicate (`paused && !stopping`,
§7.11) is strictly the better design. Building it as a *second, batch-only*
implementation leaves the worse one running in the GUI forever.

Concretely instead: `Simulator` gains `protected final boolean awaitWhilePaused()`
(the monitor, the predicate, the `notifyAll`) and a concrete
`public void pause(boolean)`; the base `beforeEvent()` returns
`awaitWhilePaused()`; `BatchSimulator` then overrides *nothing* for pause, and
`InteractiveSimulator.beforeEvent()` does its EDT notification and delegates to
`super.beforeEvent()`, deleting `pauseSem`. This is still AWT-free in `jls.sim`
(`HeadlessCoreRatchetTest` stays green), and it buys three things this task
cannot buy otherwise:

- The stop-while-paused guarantee — §7.11's "single most important clause" — is
  proved once, for both front ends, instead of once for batch while the GUI
  keeps the permit-counting version.
- #363 gets **one** quiescent instant, not a batch-specific one. §12 already
  claims the parked `beforeEvent()` is the checkpoint's quiescent instant; that
  claim is only true across the product if the park is the engine's, not
  batch's.
- FEAT-006 §5 criterion 2 ("a run paused at event *k* and resumed is
  byte-identical to an uninterrupted run") becomes assertable on the interactive
  path too, where it has never been checked.

FEAT-006 §4 invariant 3 ("stop stays terminal and distinct from pause") is
preserved by this — it separates them harder, not less.

## Reframing 2 — an in-loop heartbeat goes silent exactly when it is needed

H2 puts the beat inside `beforeEvent()`, on the simulation thread, and calls
that "the complete answer." It is the complete answer to *synchronization*, and
the wrong answer to *observability*. A beat emitted by the loop stops when the
loop stops. Three cases, all of them the reason this issue exists:

1. A `react()` that blocks. #324 (host byte port) is listed in this issue's own
   `related` set, and its drain happens **inside this loop**. A blocking read
   there is the single most likely source of a genuine hang in the product's
   near future, and the in-loop beat reports it as silence.
2. A parked run. `pause(true)` — this task's own other deliverable — stops the
   beat. Suspended and hung become indistinguishable, which is the exact
   confusion the abstract opens with.
3. A GC death spiral on the materialized trace (Reframing 3). Silence again.

In all three the operator sees what a real hang looks like and is back to "wait
or kill". Put the observer **outside** the loop instead: the loop publishes
`progressTime` and `retired` with a release store (`AtomicLong.lazySet` /
`VarHandle.setRelease` — a plain store on x86 and ARM, dramatically cheaper than
the per-event `System.nanoTime()` that T4 is anxious about); a daemon thread
wakes every *P* seconds and prints. Consequences:

- **T4 disappears.** There is no `nanoTime` on the hot loop and no gating branch
  to measure. Open Question 2 dissolves with it.
- The beat can say the thing that actually diagnoses: *"simulated time
  unchanged for 300 s, last event at t=…"*. An in-loop beat structurally cannot
  emit that sentence.
- O5 is fully respected — the observer touches no collection. Drop queue depth
  from §7.6; §10's own falsification fallback already nominates it as the field
  to drop, and dropping it up front is what makes the observer thread legal.
- It generalizes to a **stall detector**, which is what #317's CI lane and
  #350's campaign runner need. Neither of those has a human watching stderr;
  they need a verdict, not a beat.

## Reframing 3 — the artifact should be the heartbeat, and that reorders the work

§7.7 and P6 promise that a clean interrupt still yields a complete VCD.
At HEAD that cannot be relied on for the runs this issue targets, and the
reason is one tier up in the tracker.

`BatchSimulator` accumulates every sample in `eventTrace`/`probeTrace` for the
whole run, `toVcd()` folds it into a single `StringBuilder` (L384-L476), and
`writeVcd()` copies that again into a `byte[]` (L366-L368) — #353's evidence §4
records "the complete dump exists three times over at peak." On the 1.66–4.00 h
structural boot this issue cites as its motivating workload, that write is the
largest, slowest thing the process does, and under interrupt it must complete
inside the shutdown hook's proposed **5 s** join. It will not. H3's reasoning
("everything after the loop runs on the simulation thread") is sound; the budget
claim resting on it is not. And §7.11's "either absent or complete, never
truncated" is not backed by anything — `Files.write` of a several-hundred-MB
array on a JVM being torn down is not atomic. `FileAbstractor` already solves
exactly this for circuits by writing a temp file and renaming; that precedent
should be cited or the guarantee withdrawn.

So: **this issue's `blocked_by: []` is wrong at the one place it matters.**
FEAT-005 (#353) — a declared blocker of this task's own parent #354 — carries
planned **TASK-0010, "stream the waveform dump"**, whose acceptance is
byte-identity with peak live-set dropping from Θ(|C|) to Θ(1). That is not a
nice-to-have adjacent to TASK-0014; it is the precondition that makes P6 true
for anything longer than a golden fixture. Streaming is also achievable
byte-identically: `findWatched`/`findProbes` both run *before* `runEventLoop`,
so the signal set, the name ordering, the identifier codes and the entire
`$dumpvars` block are all computable pre-loop; only the per-timestamp bucket
(flushed when `now` advances — event times are non-decreasing out of the
priority queue) and the trailing `#now` line need care, and that bucket
reproduces `fold`'s last-write-wins exactly.

And once the dump streams, the elegant thing happens: **the artifact is the
progress signal.** `ls -l run.vcd` grows; `tail -f` into Surfer or GTKWave is a
live waveform, which is a strictly better answer to "where is it?" than any
text line this issue could specify — and `docs/vcd-interop.md` already exists to
document the recipe. Interrupt-safety stops being a shutdown-budget gamble and
becomes structural: the file is complete-up-to-now at every instant, including
under `SIGKILL`, which T2 correctly concedes this design can never survive.

Sequencing correction: TASK-0010 → TASK-0014. Or, if the schedule forbids it,
narrow P6 explicitly to runs whose trace fits the join budget and record that
the multi-hour claim is deferred — do not ship the guarantee unqualified.

## Reframing 4 — do not hand-mint a fourth output contract

§7.6 declares a new machine-parseable single-line format on a new stream and
says "this issue plus the doc edit are its authoritative definition." The
project has already decided how new observation surfaces get made, and it is not
that. `docs/extension-points.md` is normative for seams and states the rule
verbatim: *"Pending seams are named here first, so nobody invents a parallel
mechanism in the meantime."* `collab.op-observer` is the working precedent —
typed contract, many contributions, deterministic order, wrong-typed
contributions rejected at the registry boundary.

Three open issues need this same tap: #214 (a GUI panel over a long batch run),
#350 (per-worker campaign progress), #363 (a checkpointer that wants to know the
run has parked). If the only tap is a human-readable stderr line, each of those
must spawn a subprocess and parse text designed for an instructor's eyes. That
is the parallel-mechanism failure the catalog rule exists to prevent, and this
issue would be the one that causes it.

Add a row: `sim.run-observer` / contract `jls.sim.RunObserver` / home `jls.sim` /
cardinality many / phase register-before-run, status typed-now with this issue
as owner. `RunProgress` is a record of `(simTime, wallNanos, retired)`. The
stderr line becomes one built-in contribution of about fifteen lines — and P5's
`cmp` of two stdout captures pins it byte-for-byte exactly as written. The
delta today is one interface, one record and one catalog row;
`ExtensionPointCatalogTest` then keeps it honest for free.

## What I am disregarding from the acceptance criteria, and why

- **H2 and T4 as written.** The in-loop beat is replaced by an out-of-loop
  observer; T4's measurement obligation and Open Question 2 both evaporate. Keep
  T1's discipline (bound derived from observed event rate) — it is good.
- **§7.6 as an authoritative format.** It becomes the default rendering of a
  typed seam, not a contract of its own.
- **`blocked_by: []`.** Should read `blocked_by: [353]`, or P6 must be narrowed
  in scope with the deferral recorded.
- **§8's "read `InteractiveSimulator` but do not copy it."** Extract downward
  instead; the GUI's permit-counting park is the weaker of the two and should
  not outlive this task.

Two criteria I would keep verbatim and defend hardest, because they are the
project's actual discipline and this issue chose them well: **P5** (stdout
byte-identity proven by `cmp`, with the command pasted) and **P8**
(`CliFlagTableTest` green *unedited*). Also keep §10's H4 instruction — "do not
'fix' the golden" — which is the single most valuable sentence in the issue.

## Net

The capability belongs in JLS and the seams chosen (`beforeEvent`, `volatile
stopping`, `Simulation Stopped`) are the right ones. What I would change is
where the mechanism lives (the engine, not one subclass), where the observer
runs (off the loop, not on it), what it publishes into (a typed seam, not a
bespoke line), and when it lands (after the dump streams, not before). Done that
way, this task ends up *smaller* than as filed — no `nanoTime` on the hot loop,
no measurement obligation, no new frozen format, no shutdown-budget gamble — and
#214, #350 and #363 each get a seam instead of a parser.
