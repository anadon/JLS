# Issue #363: FEAT-035: a running simulation can be written to disk and resumed as the byte-identical continuation — same time, same pending events, same memory and register contents
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the machine block and four distinct wants are bundled under one mechanism:

1. **Durability** — a long run survives a laptop closing or a grading machine going away.
2. **Re-entry** — snapshot the interesting instant once, re-enter it many times cheaply.
3. **Migration** — move a live partition between hosts (CAP-17, #312).
4. **Suspension** — park a run so a cluster scheduler can reclaim the node (also #312).

Only (3) and arguably (4) genuinely require *state* on disk. (1) and (2) require only that
the simulator can be put back into a given state by *some* means — and JLS already has a
means, which the issue never mentions.

## The reframing: resume by replay; make the checkpoint a cache, not a contract

`docs/simulation-semantics.md:91` states the property this whole feature is trying to buy,
already achieved: ordering is "fully deterministic — a pure function of circuit content."
`Simulator.initSimulation` (`src/jls/sim/Simulator.java:189-200`) seeds from
`getElementsInStableOrder` for exactly this reason. There is no `Math.random`, `Random`,
`currentTimeMillis`, or `nanoTime` anywhere under `src/jls/sim/`, `src/jls/elem/`, or
`src/jls/Circuit.java` — I grepped. Batch stimulus is a file (`-t`). VCD output is already
declared free of `$date`/`$version`. The goldens (`BatchSimulationGoldenTest`,
`VcdExportGoldenTest`) assert byte identity on every push.

So a resume token can be `(circuit content hash, test-vector hash, flag set, target time)`,
and "resume" means *re-run to that instant*. The continuation is byte-identical not because
a serializer was careful but **because it is literally the same computation**. Everything the
issue spends four tasks and a residual establishing is obtained by construction:

- §5 criterion 1 (the commuting square) — vacuous.
- §5 criterion 2 (waveform tail identity) — vacuous.
- §5 criterion 3 (refusal list exercised) — no refusal list exists.
- §5 criterion 4 (element-kind totality over 35 types) — no per-element mapping exists; a
  36th element type is safe by default instead of dangerous by default.
- §5 criterion 5 (version skew refuses) — a replay token carries no state to misinterpret.
- Invariant 2 ("no partial checkpoint is ever written") — nothing partial can be written.
- `blocked_by: [319]` — dissolved. No format carrier is needed at all.
- TASK-0014 (`pause` ≠ `stop`) — not needed for the core capability. You checkpoint at a
  *declared simulation time* (`-checkpoint-at 40000000`), which is deterministic and
  reproducible, not at a wall-clock interruption. A SIGINT under replay needs to record one
  number: how far you got.

The cost is CPU: re-entry is O(t). That is the real tradeoff and the issue should have been
written as an argument about it. At the measured **8,090 cycles/s warm** (keystone C,
`docs/capability-roadmap/keystone-c-performance.md:655`), a "multi-hour" run is ~10^8 cycles.
Replay-based durability ships in about a week, unblocks nothing, changes no file format, and
serves wants (1) and (2) immediately. Then state capture becomes what it should always have
been: **an accelerator with a fallback**. A checkpoint that cannot be written, or cannot be
read, or was written by last year's JLS, degrades to *slow* rather than to *refused* — and
the validity check is not a code review over a decaying field list, it is "replay from t=0
and compare", which is the same oracle the goldens already use.

That inversion is the point. The issue's own §7 says "a checkpoint that resumes *nearly*
identically is worse than none." Correct — and the architecture that makes that impossible is
not a stricter serializer, it is a design where the reference answer remains computable.

**I am explicitly disregarding the stated Completion Criteria**, because they encode the
premise I am rejecting: that byte-identity of a serialized state blob is the definition of
done. Under the reframing the definition of done is *observable* continuation identity
against a replay oracle, and the state blob is an optional fast path measured in seconds
saved, not in fields captured.

## If state capture is still built (it should be, for CAP-17), cut it elsewhere

CAP-17's partition migration is the one want replay cannot serve, so state serialization is
eventually real. But the seam the issue chooses is wrong twice.

**Wrong seam 1: a new per-element "state mapping" vocabulary.** ARCHITECTURE.md's "Adding an
element today (the honest list)" already enumerates sixteen places a new element touches and
says "If you find yourself doing this, read #78 first; the registry is the recorded
direction." This issue adds a seventeenth — an un-numbered residual spanning 35 types — and
schedules it *before* the registry that exists to collapse such lists. That pulls against the
project's arc. The elegant alternative: runtime state is a new **`Attribute` category**
(`Attribute.RuntimeAttribute`), riding the existing declarative save/`setValue` protocol.
Consequences, all free: `AllElementsRoundTripTest`/`SaveTagsTest`-shaped sweeps give registry
totality (§5 criterion 4) with no new machinery; `CircuitSnapshot` — which is already
"deflated save-format text restored through the ordinary load path" — gains
simulation-state-aware undo, a real GUI feature nobody asked for and everybody wants; and the
residual stops being a fifth roster row and becomes 35 small PRs against a tested seam.

**Wrong seam 2: a section inside the circuit file.** Open Question 1 offers (a) raw section,
(b) sidecar, (c) directory. The answer is none of these as framed: **a checkpoint is its own
file that references the circuit by content hash.** Invariant 1 ("a circuit without a
checkpoint section loads exactly as today") is a tell — the cheapest way to guarantee it is
not to put simulation state in circuit files at all. That also kills the 64 MiB
`MAX_CIRCUIT_TEXT_BYTES` pressure, kills the version-negotiation entanglement, kills the
FEAT-013 dependency, and prevents a genuinely bad footgun: a student's `.jls` silently
carrying stale run state into the editor.

## Corrections that stand regardless of framing

- **`dupCheck` is derived, not state.** `post` adds to both structures, `runEventLoop` removes
  from both (`Simulator.java:167-169`, `:224-225`), `initSimulation` clears both. `dupCheck`
  is *exactly* the pending set of `eventQueue` at every quiescent point. The abstract and §1
  elevate a cache into a first-class serialized artifact ("**including the duplicate-check
  state**"). It should be rebuilt on load and pinned by an invariant test. Note also that
  `SimEvent.hashCode` mixes `System.identityHashCode(callBack)` — serializing that set would
  be serializing JVM identity hashes, which is precisely the hash-order dependence invariant 4
  forbids.
- **Normalize the sequence numbers; don't restore a base.** §3 makes "`SimEvent`'s counter
  becomes per-`Simulator`" load-bearing. Better: at checkpoint time, renumber the pending
  events `0..n-1` in queue order and set the counter to `n`. `compareTo` only reads *relative*
  order, and monotone renumbering preserves every comparison, including against events posted
  after resume. This deletes a serialized field, deletes the per-`Simulator` refactor from the
  critical path, and upgrades byte-identity into **confluence**: two different runs arriving at
  the same state serialize to the same bytes. That is a strictly stronger property and the one
  that actually serves the parity discipline in #347.
- **The "format has no carrier" claim is overstated.** `docs/file-format.md` §5 is explicit:
  "**Unknown attribute names are silently ignored** … the format's main forward-compatibility
  valve." Only unknown *item kinds* and *tags* are hard errors. Engine state is small
  structured data the existing `int`/`long`/`Int`/`String`/`ref` grammar carries fine — and
  `ref` is required anyway, since `SimEvent.equals` compares `callBack` by reference identity,
  so event callbacks must resolve to the same element instances the loaded graph holds. Bulk
  memory already has a text encoding in the format (`Memory.save`'s `initrle`, `:456-466`),
  usable verbatim for running contents. §6's evidence-4 argument for the FEAT-013 dependency
  does not survive that reading.

## Alignment with the project's arc

`ARCHITECTURE.md`'s recorded decision on simulation strategy (#221) states the interpreter is
the sole strategy because "classroom-scale gate circuits are the present workload" and a
second strategy is "premature optimization until CPU-scale designs are actually common," with
a revisit trigger of an unusably slow `riscv/` design. **FEAT-035's entire justification is
the falsity of that premise** — multi-hour runs are asserted as the workload throughout. Both
cannot be true. If they are (and keystone C plus `lf-02-compiled-evaluation.md` argue they
are: 4.9% of time is digital logic, 95% bookkeeping, ~25-40 kcycles/s reachable), then the
honest response to "runs take hours" is not to make hours durable — it is to pull the #221
revisit trigger that has already been specified in detail. Making a 6-hour run resumable and
making it a 20-minute run are answers to the same complaint, and only one of them also serves
every other user of the tool. Ordering a 10-17 month-week checkpoint feature ahead of
#362/FEAT-030 and the compiled pass optimizes the durability of a slowness the roadmap has
already scheduled for removal.

## What I would keep

The discipline is right even where the design is not. Writing the equivalence property before
the thing it verifies (TASK-0075 → TASK-0074) is correct and rare, and it survives the
reframing intact — it just gets a cheaper oracle. The refusal-over-silence instinct is right.
The observation that `beforeEvent()` (`Simulator.java:252`, called at `:220`) is already the
pause seam is a good, cheap finding worth landing on its own.

## Recommended re-plan

1. **FEAT-035a — resume by deterministic replay.** Resume token, `-checkpoint-at`, SIGINT
   records the reached time, a CI test that a token-resumed run's dump equals the
   uninterrupted tail. No format change, no `blocked_by`, ~1-2 wk. Serves CAP-02 and the
   grading-batch case outright.
2. **FEAT-035b — state capture as an accelerator**, gated behind (1) as its oracle, expressed
   as `RuntimeAttribute`s on the existing save protocol, landing element kind by element kind,
   each independently valuable, with unmapped kinds simply not cached. No refusal list as a
   user-facing contract — a "not cacheable" list is an internal performance note.
3. **FEAT-035c — migration/suspension for CAP-17**, which is where the strict all-or-nothing
   contract genuinely belongs, scoped to the partitioned model rather than to the whole
   element vocabulary.

Under that split, the residual disappears, `blocked_by: [319]` disappears, TASK-0014 becomes
optional polish shared with FEAT-006, and the 10-17 mw band's unreconciled gap (Open Question
5) stops being a scheduling mystery because the expensive part is no longer on the path to the
capability.
