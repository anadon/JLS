# Issue #555: FEAT-C28-2: `docs/performance.md` states JLS's throughput with the full method — hardware, JDK, flags, node counts — honestly framed, and the README cites it in one line
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not speed. CAP-28 (#512) says it plainly — "the deficit is epistemic", JLS scored
2/5 on scale/perf "for lack of receipts, not lack of speed". #555 is the moment
JLS stops being a project whose performance is a matter of opinion. That is the
right ambition and it fits the repo's deepest habit: this tree already publishes
`docs/reproducibility.md` (a declared artifact set, a `.buildinfo` toolchain pin,
a third-party rebuild recipe, and an explicit list of what does *not* reproduce),
`docs/simulation-semantics.md`, `docs/batch-interface.md`, `docs/file-format.md`.
JLS's real product line is *normative documents backed by oracles*. A performance
doc belongs in that line, and `docs/reproducibility.md` is the template for AC-2,
not a generic "methodology section".

Endorsed in direction. Four reframings follow, one of which I think is the
issue's most valuable possible output and is nowhere in its acceptance criteria.

## 1. There is already a rival public number surface in-tree, and the issue does not know about it

`docs/capability-roadmap/keystone-c-performance.md` is 870 lines of published,
in-repo, GitHub-readable performance claims: **318 ns/event, 124 µs per simulated
clock cycle, ≈8,090 cycles/s warm, ≈4,600 with `initSimulation`, 1,100–1,450
end-to-end from the CLI**, a full JFR attribution table (37.6% value / 47.7%
queue / 4.9% logic), a representation bake-off in ns/op, and a projection of
25–40 kcycles/s after the engine stack. It states hardware, JDK-ish environment,
flags (`-XX:FlightRecorderOptions=stackdepth=512`), node counts (1551 elements,
297 nets, 522 eval slots) and the clocking regime.

So AC-4 — "no public performance claim exists anywhere in README/docs that the
doc does not back" — is violated at filing time by dozens of numbers, and the
violation is worse than it looks: §12's reproduction section points at
`/tmp/claude-0/.../scratchpad/bench/`, a path that does not exist, and at
`riscv/bench_kernel.py`, which its own docstring calls "a scratch harness, not
part of the verification suite, and can be deleted" — and which D5/#413 will
delete. The tree's most detailed performance claims are *already* unreproducible.
That is the exact failure CAP-28 exists to end, sitting inside the repo, and
#555's boundary notes name #335 and #560 but not this file.

**Concrete ask:** #555 owns the disposition of `keystone-c-performance.md`, not
just the creation of `performance.md`. Either (a) demote it in place with a dated
banner — "analysis of 2026-07 against 5.0.5-SNAPSHOT; current published numbers
live in `docs/performance.md`" — or (b) promote its harnesses into #554's suite
and cite. Doing neither leaves JLS with two performance documents that will
disagree by the next engine change, which is the precise thing this capstone
promises will never happen again.

## 2. Publish a profile with its units, not a score on someone else's axis

The framing in #512 is "Digital publishes 120 kHz and wins every performance
conversation by default". Written as specified, `docs/performance.md`'s headline
is 8,090 cycles/s against a competitor's 120,000 — a 15× loss — and AC-3's
"name at least one place a competitor is faster" becomes a confession bullet
under a headline that already concedes everything.

But the two numbers are not the same unit. JLS is a transport-delay,
per-element-timed, event-driven simulator: gates 10/5, Mux 25, Register 50,
Memory 100, Adder 30×bits (`docs/simulation-semantics.md` §6.2, §7), narrow
pulses survive, and every VCD byte and trace timestamp depends on it —
`ARCHITECTURE.md`'s recorded decision (§"Simulation execution strategy") binds
any future engine to bit-for-bit agreement *including* those delays. A
cycle-based simulator's "kHz" is a different physical quantity; comparing them
directly is a category error that JLS would be volunteering to lose.

**Reframe the doc's spine:** §1 defines the unit and the timing model *before*
any number; §2 gives the cost profile (ns/event, events/cycle, cycles/s, and the
bookkeeping-vs-logic split, which is JLS's most credible and most interesting
number); §3 states what the timing model buys — glitch visibility, exact VCD,
transport-delay pedagogy — and what it costs, naming Digital as faster and saying
*why* structurally rather than apologetically. That satisfies AC-3 more honestly
than a sentence does, and it makes JLS the party that defined the axis. It also
pre-loads #560's harness with the only comparison that can be fair: same
workload, and a stated caveat about which semantics each engine implements.

## 3. The published number should not be the one no user experiences

Every number in scope is headless batch. The surface an instructor and thirty
students actually meet is the interactive engine with a trace window open — and
`keystone-c-performance.md` §6.4 flags it as *structurally worse and entirely
unmeasured*: `InteractiveSimulator.afterEvent` does an O(probes) walk with a
`BitSet` clone per probe on **every event**, so JLS gets slower the more a
student probes, which is backwards for a teaching tool.

A public performance doc that publishes only the batch figure is technically
honest and practically misleading. **Add one interactive figure** — events/s or
ms/animation-frame at a stated circuit size with N probes open, N ∈ {0, 4, 16} —
even if the first published value is embarrassing. It is single-tool
self-measurement, so it stays inside #555's boundary against #560, and it is the
number most likely to change a real adoption decision.

## 4. The mechanism is wrong: this project pins documents with tests, not with prose

AC-4 asserts a global invariant over the whole doc tree in an English sentence.
This repo does not do that anywhere else it matters: `HelpTopicsTest` link-checks
and completeness-checks the help tree, `CliFlagTableTest` pins the flag table,
`SaveTagsTest`/`FileFormatSpecTest` pin the format spec, `ExtensionPointCatalogTest`
cross-checks the catalog in both directions, `HeadlessCoreRatchetTest` and
`NotificationRatchetTest` keep architectural rules from silently reappearing.
Prose invariants rot; ratchets do not.

**Do it the JLS way.** #554 AC-4 already requires machine-readable suite output.
Then:

- `docs/performance.md` carries a **generated** numbers block (or includes a
  committed `docs/performance-data.*` produced by the suite) — never
  hand-transcribed;
- a `PerformanceClaimTest` scans `README.md` and `docs/**.md` for a small claim
  vocabulary (`events/s`, `cycles/s`, `kHz`, `ns/event`, `faster`, `throughput`)
  and fails on any hit outside `docs/performance.md` that lacks a citation
  marker — AC-4 becomes executable, and today it would immediately flag §1 above;
- the doc records the commit/version each number was measured at, in a table
  column, from day one. A single undated headline number will be a lie within one
  release: #475/#476 and the value-domain migration are projected to move it
  3–5×, and D5 is about to delete its fixture's home.

This also stops FEAT-C28-3 (#553-lane scheduling) from having to invent a
staleness mechanism later; it inherits one.

## 5. The output I would most want, and would trade an AC for

`ARCHITECTURE.md` records that the discrete-event interpreter is JLS's **sole**
strategy, with the revisit trigger "a concrete CPU-scale design that is unusably
slow interactively". `keystone-c-performance.md` §2 already says the right thing
about it: "unusably slow" is not a testable condition and nobody will ever agree
it has been met — restate it quantitatively, e.g. *below 10 kcycles/s on the #202
golden*.

That is the durable artifact hiding inside #555. A number decays; a **published
performance policy** does not. `docs/performance.md` should state, in JLS's own
voice: what this simulator optimizes for (timing fidelity and classroom
responsiveness), what it has deliberately declined to build (levelized/compiled
evaluation, per #221), and **the measured threshold at which that decision
reopens** — with the same-document numbers showing where JLS sits against it
today. Then the doc answers "why are you slower than Digital?" with a decision
and a trigger instead of a shrug, and it converts an untestable architectural
escape clause into a tripwire the perf lane (FEAT-C28-3) can watch.

I would accept shipping that section *instead of* AC-3's comparative sentence if
only one could land — the policy statement contains the honest comparison as a
consequence, while the sentence does not contain the policy.

## Verdict

**endorse-with-reframing.** The goal is right and the ordering (#554 → #555 →
#560 → #588) is sound. Keep AC-1 and AC-2 as written; AC-2 should explicitly
model itself on `docs/reproducibility.md`. Restructure AC-3 into a
semantics-and-policy section rather than a confession sentence. Replace AC-4's
prose invariant with a `PerformanceClaimTest` ratchet, and add two acceptance
items the issue lacks: (i) `docs/capability-roadmap/keystone-c-performance.md`
is dated/demoted or its numbers are re-homed, so the tree has exactly one live
performance claim surface; (ii) at least one *interactive* figure is published
alongside the batch throughput.
