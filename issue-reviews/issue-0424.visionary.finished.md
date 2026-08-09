# Issue #424: TASK-0067: a sealed host byte seam that exists only when a person grants it at invocation, drained at a loop boundary a pause cannot skip, and unreachable from any .jls file
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is really for

The end is not a byte port. It is: **a drawn machine whose observable is a transcript** — #202's
RV32I, #295/#301's shell prompt, an instructor diffing what a submission printed. Read that way,
the issue is asking JLS to grow a second "the world talks to the circuit" contract. JLS already
has one, and the issue does not know it.

## The finding that reframes the whole task: the door already exists, and D7 named it

`src/jls/elem/TestGen.java` is an element under `src/jls/elem/` that does
`new FileInputStream(file)` in `initSim` (L68-L72) on a path supplied by `-t` on the command line,
installed at invocation by `BatchSimulator.addTestGen` (L190-L211), and **never nameable from a
`.jls` file**. It is a permit of `sealed class SigSim permits SigGen, TestGen`. Its grammar,
timing semantics and error contract are frozen in `docs/batch-interface.md` §2.

That is: a sealed intermediate, a host handle, an invocation-time grant, an element consumer,
and a stability contract — shipping today. D7's own words are "sealed intermediate following the
existing Gate/Group/Pin/**SigSim** pattern"; the issue copies SigSim's `sealed` keyword and
misses that SigSim's *mechanism* is the design being re-invented.

Consequences, in descending order of importance:

- **O2 is an overclaim.** "The first read-side host door in the project's history" is true only if
  "host door" means literally `System.in`. A read-side host *file* door granted at invocation
  exists, is documented, and is graded against.
- **P6 cannot hold as written.** "Scan `src/jls/elem/` for `java.io`, `java.nio.file` … observe
  **none**, against an empty baseline": 35 files under `src/jls/elem/` import `java.io` today
  (save/load needs `PrintWriter`/`Scanner`), and `TestGen` constructs a stream. The scan fails on
  day one, and the response will be a baseline — the one thing §8 forbids. The property #324
  criterion 2 actually wants is *reachability from `Reacts.react()`*, which is a call-graph
  question, not a substring question. `test/jls/ArchitectureRulesTest.java` already runs ArchUnit
  (`socketEndpointsAreConfinedToCollabNet`, L249) and is the right instrument.

## Reframing 1 (the big one): make the timed byte stream the primitive, live stdio the adapter

TASK-0067 builds a live pipe and defers replay to TASK-0069. Invert it. The primitive is a
**stamped byte schedule** — exactly `#324`'s `T = ((t,d,b), …)` — and it is the same shape as
`-t`: values bound to *simulated* times, posted as `SimEvent`s. Live `stdio` becomes an adapter
that stamps arrivals as it admits them **and records the schedule it produced**, so every live
session is replayable by construction.

Under that framing, the hardest parts of this issue stop existing:

- Determinism is structural, not asserted. §7.10 Stage 3 as written is vacuous — it says results
  agree *given* the visibility indices agree, which is the thing in question.
- **H3's "refuted → record and stop" case disappears.** A byte that arrives while the queue is
  empty is invisible today: `runEventLoop`'s guard is `!eventQueue.isEmpty()`, so an idle
  console-bearing circuit ends with "No More Activity" rather than waiting. Arrival cannot create
  activity. Modelled as a posted input event (the `SigSim` shape) it can, with no change to the
  threading contract the issue routes to "#49's successor".
- #324 criterion 6 — "CI refuses a golden produced while a live door was granted" — becomes
  unnecessary rather than a new ratchet to invent: there is one artifact, and it is always the
  schedule.
- The ring, the drop counter, "no allocation in the drain", and §11's "thread correctness is the
  whole feature" are all costs of the live-pipe-first choice, not of the capability.

## Reframing 2: delete the `Simulator` drain — the receive side already self-schedules

#324 criterion 3 says the receive side "**self-schedules** its next poll exactly as `Clock` does",
and O8 cites `Clock.java:392/421` for it. If the console element pulls at its own scheduled
`react`, the per-iteration drain in `runEventLoop` is redundant — and with it go O4, H2, P3,
Stage 2, the hot-path non-allocation constraint, and a three-way edit collision on
`Simulator.java` that §11 already flags.

The drain does not buy what H2 claims. `InteractiveSimulator.beforeEvent` **blocks inside the
hook** on `pauseSem.acquire()` (`src/jls/edit/InteractiveSimulator.java` L767-L770). A drain placed
immediately *before* the call runs once and then the thread parks for the entire pause. Bytes
offered while a user is paused are not delivered under either placement. P3's fake — a subclass
returning `false` for N iterations without blocking — spins, so **the test passes while the real
pause path starves**. That is a green regression test for a property the product does not have.
The residual benefit of the drain slot is delivery during a step-end decline, immediately before
a pause. It is not worth touching the shared loop.

## Reframing 3: grant to a named element, not to an ambient `sim.hostPort()`

§7.4 puts the port on `Simulator` and has elements reach it via `sim.hostPort()` — ambient
authority for every element on the reaction path, fenced by a source-scanning lint. In a project
whose recorded direction is *typed* seams (`docs/extension-points.md`, #223), the stronger cut is
to deliver the capability to one named element instance at invocation. The precedent is again in
the tree: `-s paramfile` already lets the invoker configure elements by name
(`ELEMENT <name> WATCHED true`, `JLSStart.processParamFile`) without the `.jls` file participating.
`Simulator` then grows no host field at all, and P6's negative becomes trivially provable because
there is nothing ambient to reach.

## Right-size the seal

Sealing is a fine API decision and a poor security boundary, and the issue rests the security
argument on it (H1). The property that actually holds is §7.10 Stage 4's: **the domain of
`f_grant` is the command line**. That is what stops a `.jls` file, and it is testable directly.
Sealing stops an *external jar*, but ARCHITECTURE.md's #222 decision already records that external
providers ship in-process with "full JVM authority" — such a jar calls `new FileInputStream`
without asking `HostBytePort`. So the seal defends against a threat the recorded architecture has
already conceded, while the real defence gets one prediction. Also note the permit set is
ceremony: `PipePort` and `PanelPort` are both memory-backed rings (`PanelPort` cannot touch Swing
anyway — P9 puts `src/jls/io/` inside `HeadlessCoreRatchetTest`), and `StdioPort`/`FilePort` are
both stream pairs. Five permits, two implementations, one shipped "wired to nothing" (OQ2).

## Acceptance criteria I would disregard, and why

P6 as written (impossible; replace with an ArchUnit reachability rule from `Reacts.react()`),
P3 and the §14 drain-placement checkbox (proves nothing about the pause it names), P8's
"no allocation in the drain" (optimising a path that should not exist), and OQ2's
"ship `PanelPort` wired to nothing" (speculative API forced by the seal). I would keep, unchanged:
the `-s`/`-t`-style invocation-only grant, P5's null default, P7's frozen outcome strings, P11's
untouched `CliFlagTableTest`, and the `package-info.java`/`@NullMarked` hygiene.

One tension to name: §9 requires "one manual `-serial stdio` session" as evidence, but a human
cannot type into a run bounded by `-d` and terminated by an empty queue. The issue is right that
#354 is not a *blocker* for the seam; it is a blocker for the seam being demonstrable, and the
evidence plan quietly assumes otherwise.

## Verdict

**endorse-with-reframing.** The capability is squarely on JLS's arc and the grant model is the
right instinct. But the task builds a second, wall-clock-coupled external-world mechanism beside
the simulated-time one JLS already ships and sells as a contract, and pays for it with a hot-loop
edit, a cross-thread ring, and a ratchet that cannot pass. Reframed as *a stamped byte schedule
driven by an invocation-granted, self-scheduling element in the `SigSim` family, with live stdio
as a recording adapter*, the same seven properties hold, TASK-0069's replay falls out for free,
and `Simulator.java` is not touched at all.
