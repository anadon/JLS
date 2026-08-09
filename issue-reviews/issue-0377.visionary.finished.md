# Issue #377: TASK-0022: the per-cycle active fraction stops being "never measured" — a two-cycle machine, an internal clock, and per-callback event attribution yield α, CPI and k with their method
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Strip the apparatus and the ask is: *price engine work honestly before funding it.*
Two consumers are named — #232/#362 (is a proposed speedup worth the weeks?) and
CAP-02's boot table (is a 4.0e7-instruction Linux boot 1.15 h or 6.0 h?). That
goal is right and the project needs it. The instrument the issue reaches for —
three factored constants measured on a purpose-built two-cycle machine — is the
wrong one, and the issue contains its own refutation.

## The load-bearing objection: α and k are gauge, not physics

§7.10 states the identity plainly: `k · α · L · CPI = N_react / N_ret = E_instr`.
The three constants are a *factorisation of one measured ratio*. Nothing measures
α independently of k; nothing measures k independently of α. Given `E_instr` and
any census `L`, you can hand back infinitely many (α, k) pairs that reproduce it.
`docs/machine-calibration.md:437-439` already says so: *"The second form
(T = N_instr × ev_instr / R) is the more robust one, because it avoids splitting
α from CPI, and it is the form the table in 4.4 uses."* This task proposes to
measure the factors of the form the source document deprecates, and its headline
deliverable — α — is the one factor that never appears in the arithmetic that is
actually used.

The §4.2 "single most important open question" (468 ev/instr at k = 1.8 implies
α = 0.155, below α's own floor) is not a physical contradiction at all. It is
three estimates of a decomposition failing to agree because the decomposition is
underdetermined. Measure `E_instr` and the contradiction evaporates without
anyone having to pick a k. That is the reframing that makes the problem
disappear: **the unknown is one number, not three, and the issue's own algebra
says which one.**

## For the primary named consumer, the constants cancel identically

#362 is "every retired event costs measurably less." A speedup is
`T_before / T_after` on a fixed circuit and a fixed workload. `k`, `α`, `L`,
`CPI` and `N_instr` are all identical on both sides and cancel exactly. The
issue asserts that without them "a speedup figure is arithmetic over a guess" —
that is only true if you insist on expressing the speedup as a projected boot
wall clock, which §8 then forbids this task from quoting anyway. What #232 and
#362 need is a **repeatable throughput harness on a fixed fixture ladder**, and
there is none in the tree today: no JMH in `pom.xml`, no perf lane, no
`@Tag("longrun")` anywhere under `test/`. That absence is the real gap, and it
is not what this task fills.

## α is not merely unmeasured — it is under-defined in this engine

`α = (1/N_cyc) Σ |A(c)| / L` presupposes a cycle boundary. `jls.sim` has no such
concept: `Simulator.runEventLoop` knows only `now` and a `PriorityQueue`, and
`Clock` (`src/jls/elem/Clock.java:384-421`) is an ordinary element that reposts
`NewValue` at `now + when`. A harness must therefore *infer* cycle edges by
watching one element's posts — and elements post at `now + propDelay`, so a
cycle's propagation tail routinely lands after the next edge. `Memory` makes this
acute: its self-posts at `now + accessTime` (O6) are asynchronous by design, so
membership of `A(c)` depends on where you cut. Two defensible cuts give
different α on the same run. Pinning such a number with an exact-equality test
(P3) and a band (P5) ratchets a convention, not a property. Nothing in §11
addresses this, and it is a deeper threat than the merged-`Memory` artifact P7
guards against.

## Alternative A — one circuit, two drives, zero new fixtures

The 2.02x TestGen-versus-`Clock` discrepancy (O2) is a claim about **drive**, not
about machine depth. The controlled experiment is one circuit under two drives,
which is exactly P1 — and the circuit already exists. `test/fixtures/riscv-sum1to10.jls`
has exactly one `InputPin` (the census in O5 confirms it), stepped by the
generated `-t` vector in `RiscvCpuGoldenTest.clockVector()`. Replacing that one
element with a `Clock` at the same period, via `-savetext` and a one-line edit,
yields the element-for-element identical pair the historical 245.5/121.5 claim
was supposedly taken on. Total cost: an afternoon, one added fixture variant, no
hand-drawn CPU, no GUI authoring (Open Question 1 dissolves), no third CPU
fixture competing with #202's worked example and TASK-0025's anchor — a conflict
§12 itself worries about and then creates.

This also settles CPI for free on the existing fixture: `RiscvCpuGoldenTest`
already declares `STEPS = 34` cycles for 34 dynamic instructions, so CPI = 1 is
measured, not estimated, at one end of the range.

## Alternative B — the census belongs in `src`, as a user-visible surface

§7.4 says "None in `src/`" and §7.12 makes "zero changes under `src/jls/sim/`" a
completion criterion. That is the wrong seam for this project's arc. JLS ships a
*documented, versioned batch interface* (`docs/batch-interface.md`) with VCD
export for autograders. An event census — total reacted events, and the breakdown
by callback class — is the same kind of artifact: a `jls -b -stats circuit.jls`
line, or a section of the existing VCD/trace path, riding on the `afterEvent`
hook that O7 already found. That is:

- **pedagogy**, for a teaching simulator: "your circuit retired 41,000 events in
  34 cycles; 78% were `WireEnd`" is a lesson a student can act on, and today the
  tool cannot say it;
- **the measurement**, obtained as a side effect rather than as a private
  `Map<Class,Integer>` inside one test class;
- **permanent**, surviving the plan corpus, the `riscv/` deletion, and TASK-0024's
  rewrite of the document.

It respects the constraints that matter: `afterEvent` is already overridden in
`BatchSimulator:140`, the hot-plane rule and `HeadlessCoreRatchetTest` are
untouched, and the `@jls.testedby` tag question (Open Question 4) resolves by
the counter becoming a first-class thing with its own test rather than a test
being cited as if it were a contract.

## Alternative C — ratchet `E_instr`, not α

Replace the α band (P5) with a small tracked table: for each fixture that runs a
program, pin `N_react`, `N_cyc`, `N_ret`, the census `L`, and the drive — as
exact equalities, which is P3's good idea. `E_instr = N_react / N_ret` falls out
directly and is the only quantity the boot model consumes. When FEAT-038's real
~580-element drawn machine lands, its row is added and the extrapolation stops
being an extrapolation. Every publication rule the document imposes (§2.5, §7.3
step 6) is satisfied by the table's columns rather than by prose discipline. And
an engine change that moves event counts still goes red — the enforcement value
the issue wants is preserved intact.

## Trajectory

Master's `ARCHITECTURE.md` records the #221 decision that the discrete-event
interpreter is the **sole** strategy, with an explicitly *empirical* revisit
trigger: "a concrete CPU-scale design on the `riscv/` trajectory that is unusably
slow interactively." The right instrument for an empirical trigger is a
benchmark, not a factored analytic model. Meanwhile the entire corpus this task
discharges — `docs/machine-calibration.md`, `docs/plan/**` — exists only on
`claude/jls-virtual-hardware-linux-njsoma` at `2d0ca9d`; neither is on the
default branch. Landing a frozen hand-drawn CPU fixture and exact-equality tests
into `test/` to service an off-branch document, for a capstone costed at
155-250 maintainer-weeks that may never be funded, is investment in the plan
rather than in the tool. Alternatives A-C produce work that pays off either way.

## Acceptance criteria I am disregarding, and why

- **The two-cycle unified-memory fixture, and Open Questions 1-2.** A third
  hand-drawn CPU, frozen forever, to measure a decomposition that is
  underdetermined and whose §11 already concedes will not transfer to a deeper
  pipeline. Alternative A gets the discriminating observation without it.
- **"α is within the recorded band" (P5) and the α/k write-back into §6.1.**
  Publish `E_instr` with its census, drive, and pass count instead. If a reader
  wants α, the document can state the (α, k) pair it *chose*, labelled a
  convention.
- **"Zero changes under `src/jls/sim/`" (§7.12).** Inverted deliberately: a small,
  documented census surface in `src` is the durable form of this work.

## What I would keep unchanged

§8 item 1 — per-callback attribution, run under both drives, table published
whichever way H1 falls. It is cheap, decisive, unblocked, and it is the only part
of this task whose answer cannot be obtained any other way. Ship it alone, and
make everything downstream contingent on what it says. The prohibition on
adjusting any number to make a consistency check pass (§10) should be lifted out
of this issue and into the document as a standing rule.
