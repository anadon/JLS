# Issue #839: TASK-C333-6: a partitioned run checkpoints at a barrier and resumes to a byte-identical continuation, so a cluster run can be suspended and rescheduled
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually claiming

Read as a claim about JLS's future, #839 says: *JLS should be a simulator whose runs are
cluster citizens* — preemptible, rescheduable, movable between machines — and the way to get
there is to add a partition-aware checkpoint on top of a whole-design checkpoint that does not
exist yet. It is the sixth leaf of the #333 family (#830, #832, #834, #836, #838, #839), which
is itself one of eleven required features under #312's capacity capstone.

Its own stated contribution is narrow and honest: "the coherence argument — that only a barrier
is a consistent cut — is this task's contribution; the serialization is not." That is roughly
one paragraph of reasoning plus a fixture. It carries a 3-5 mw band.

## The trajectory it has to answer to

- `README.md` line 5: "JLS is an educational digital logic circuit editor and simulator.
  Students draw circuits from gates, wires, registers…". The deployment story is a jar, a `.deb`,
  a Gatekeeper right-click, a lab machine.
- `ARCHITECTURE.md`, "Simulation execution strategy: discrete-event interpreter is the sole
  strategy (recorded 2026-07-26, #221)": no second execution strategy is built, because
  "classroom-scale gate circuits are the present workload" and a second strategy is "premature
  optimization until CPU-scale designs are actually common." The revisit trigger is a concrete
  `riscv/` design that is unusably slow interactively. **That trigger has not fired.** A
  levelized compiled pass — an in-process, single-machine change behind one boundary — is
  judged premature; #333 proposes a conservative-PDES engine, a second `Transport` consumer, a
  cluster deployment contract, and a partition-count-invariance suite. That is one to two orders
  of magnitude more architecture than the thing the project has already declined.
- `src/jls/sim/` is 1,256 lines across seven files. `Simulator.runEventLoop` is a 25-line while
  loop. `BatchSimulator.pause(boolean)` is still literally `stopping = true` with a javadoc
  saying pausing "doesn't make sense in batch mode." `SimEvent.java:87` is still
  `private static long sequence = 0`.
- The planning corpus this whole family cites — `docs/parity-contract.md:469-477`,
  `docs/virtual-hardware-parity.md` P15, `docs/plan/features/`, `docs/plan/evidence/BRIEF.md` —
  **is not in the tree.** `find . -name '*parity*'` returns nothing; `docs/plan/` does not exist.
  Every load-bearing citation in #839's ancestry resolves to a document a contributor cannot
  read. That is not a nitpick at the visionary altitude: it means the argument for the tower
  #839 sits on top of is currently unauditable from the repository.

The project's real spine is `ARCHITECTURE.md`'s "Recorded decisions" section — decisions with a
named revisit trigger and a named cost. #333 has no such gate. #312 half-admits this in its own
re-planning protocol: "'no' is a legitimate recorded answer for a capacity axis nobody is
funding." Nothing in the repository names a funder.

## Reframe 1 — the checkpoint is a property of the design, not of the run

This is the one I would actually act on, and it makes #839 disappear.

#839's AC-3 already demands that the checkpoint "carries no partition count or identifier a
resumed run could observe." Take that seriously and follow it one step further: if the
checkpoint contains nothing about the partitioning, then **the checkpoint of a 4-partition run
at a barrier and the checkpoint of a whole-design run at the same simulated instant are the
same artifact.** There is no "partitioned case" of checkpoint/resume. There is one checkpoint
format, defined over the design's global state at a global time, which a partitioned run
happens to assemble from four contributors.

The mechanism to make that true is already latent in the codebase and in #426:

- `Circuit.getElementsInStableOrder()` (`src/jls/Circuit.java:479`) already exists and is
  already what `Simulator.initSimulation` iterates, precisely so element order is "a pure
  function of circuit content."
- #426 keys every element-state record and every event callback by **stable id** (its O4/H4:
  restore must resolve to the existing instance by stable id), and #318/FEAT-014 exists to give
  watched elements partition-independent names.

If the checkpoint section is *defined* as "records sorted by stable id, events sorted by
(time, seq), engine record with `now`, `maxTime`, sequence base," then partition-independence
is a consequence of the format, not a property to be tested per partition count. Assembly
across partitions becomes a merge-sort on a key that already has to exist. The "coherence
argument" #839 calls its contribution shrinks to a single sentence added to #426's format
document plus one assertion in #456's gate: *the writer's input is the union of every
contributor's state at one committed time; the barrier is the only instant at which that union
is well-formed.*

Concretely, I would move #839's substance into the two issues that already own the mechanism:

- **#426 (TASK-0074)** gains one contract line — the checkpoint's record order is a total order
  on stable id and is independent of who produced each record — and one refusal: a writer
  invoked with contributors at unequal committed times refuses by name. That refusal is the
  barrier-coherence argument, expressed where the format lives, and it is checkable long before
  any partitioning exists (feed the writer two `Simulator`s at different `now`).
- **#456 (TASK-0075)** gains one fixture in its existing null-methodology: assemble the same
  state from N synthetic contributors and assert the bytes are identical for N = 1, 2, 4. That
  costs a parameterized argument, not 3-5 mw, and it runs today against a single-machine
  harness with no transport, no barrier, and no cluster.

The payoff is that the invariant is armed *before* the distributed engine exists, so if #333 is
ever built the partitioned checkpoint is free and if #333 is never built the project still got
the stronger format.

## Reframe 2 — repartition-on-resume is the capability, not the hazard

AC-3 treats resuming at a different partition count as a risk to be contained: "either
byte-identical or refused by name." That is exactly backwards for the stated client. The
deployment context #839 invokes is a shared cluster — SLURM, k8s — where the whole reason to
checkpoint is that the allocation you get back is *not* the allocation you had. A run that can
only resume onto the same node count has bought suspension without buying rescheduling, which
is most of the value the issue's title claims.

Under Reframe 1 this is not extra work: a checkpoint with no partition information in it can be
handed to any partitioning of the same design, because repartitioning is a function of the
design plus a boundary description (#332's artifact), not of the state. **Elastic resume falls
out of the format rather than being engineered.** If the project ever wants the capacity axis,
this is the property worth advertising, and it is the one #839 explicitly declines to try for.

## Reframe 3 — the client is SIGTERM, so build deferral and delete refusal

AC-1 offers refusal and deferral as co-equal answers to a mid-interval checkpoint request. But
on a real scheduler nobody types "checkpoint now": the process receives SIGTERM with a grace
period, or the scheduler's preemption hook fires. For that client, refusal is never a correct
answer — it converts a recoverable preemption into a lost run. The right contract is
one-directional: **a suspend request is always accepted and always honored at the next barrier**,
with the grace period compared against the barrier interval and the run exiting 0 with a resume
token. The barrier interval then becomes a scheduling parameter with a stated bound, which is
the number a cluster operator actually needs.

Dropping the refusal path removes a user-visible error contract, removes a message that must be
worded and tested, and turns AC-1 from a two-branch specification into a one-line one. It also
generalizes: the same "flag now, act at the next quiescent instant" shape is exactly what
TASK-0014 must build for batch pause on `Simulator.beforeEvent()` (`src/jls/sim/Simulator.java`,
the hook at the top of `runEventLoop`). One mechanism, two consumers.

## Alternative worth pricing: the replay checkpoint

An out-of-the-box option neither #839 nor #363 considers. JLS's determinism discipline is
strong enough that a *logical* checkpoint is well-defined: `(circuit bytes, test-vector file,
target time)`. Resume = re-run from t=0 and continue. It captures no element state, needs no
per-element mapping across ~30 `initSim` implementations, needs no refusal list, needs no
format version, and is trivially partition-count invariant because it contains no state at all.

It is O(L) on resume instead of O(1), so it is not the end state — but as the *first* landing
it would give #839's AC-1, AC-2 and AC-3 with near-zero new surface, and thereafter it is the
differential oracle for the real codec: replay-resume and state-resume must produce the same
VCD tail. Given that #363's residual (per-element state mapping across the registered
vocabulary plus the refusal list) is admitted to be unpriced and unnamed in the task registry,
having a cheap correct fallback that proves the property first is worth more than the ordering
#839 assumes.

## Disregarding the acceptance criteria, and why

I am setting aside AC-1 through AC-4 as written, not because they are wrong in detail but
because they scope a task that should not exist as a separate task. AC-4 already says the
serialization is #363's mechanism "consumed as-is" and that any new field belongs in that
mechanism's schema and gate. Follow that instruction to its conclusion and there is no residue:
the barrier-coherence rule is a precondition on the writer (#426), the invariance evidence is a
parameterization of the existing gate (#456), and the suspend semantics belong to the pause
mechanism (TASK-0014). A 3-5 mw task whose entire content is "one argument plus a fixture, on a
mechanism owned elsewhere" is the plan's own decomposition rules producing a placeholder.

AC-5 ("`mvn verify` green; whole-design checkpoint behaviour unchanged") survives unchanged and
is right.

## Ordering note that outlives the verdict

#830 (TASK-C333-1, the cross-platform determinism experiment, 1-2 mw) is the only member of
this family that is cheap, useful regardless of whether #333 ever proceeds, and load-bearing for
every byte-identity claim in #839's AC-2. It should be run now and its result committed even if
the rest of the tower is shelved — and if it refutes, #839's AC-2 was never satisfiable as
stated.

## Verdict: redirect

The end #839 serves — a run that survives interruption and can be rescheduled — is real and
worth having. The route is wrong twice over: it puts the partition-independence invariant in the
last task instead of the first, and it hangs a small argument off a distributed-simulation
program whose demand gate the project has never opened, in a codebase whose own recorded
decision (#221) calls a far smaller step premature. Fold the substance into #426 and #456 as a
partition-free checkpoint format plus an N-contributor assembly assertion, give the suspend
contract to TASK-0014's quiescent instant as deferral-only, and close #839 with a `REPLAN:`
recording that its contribution landed upstream. If #333 is ever funded, the partitioned
checkpoint will already be done.
