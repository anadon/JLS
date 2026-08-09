# Issue #732: TASK-C555-1: `docs/performance.md` publishes JLS's throughput with the full method — and names at least one place a competitor is faster
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not speed. CAP-28 (#512) says it outright: the survey scored JLS 2/5 on scale/perf
"**for lack of receipts, not lack of speed** — the deficit is epistemic." So #732 is
not a performance task at all; it is a *credibility* task, and it belongs to the same
family as `docs/reproducibility.md`, `docs/batch-interface.md`'s stability contract,
and the README's volunteered defects (unsigned macOS builds by choice, installers that
are deliberately not byte-reproducible, the JLS 4.1 loader that "silently drops initial
memory contents"). That family is the strongest thing this project has. A performance
page joins it naturally. **Endorse the direction without reservation.**

The reframings below are about the shape of the artifact, and about one thing the
issue's acceptance criteria get backwards.

## The thing it gets backwards

AC-4 asks for "at least one place where a competitor is faster." On the evidence
already on master, that framing is inverted. `docs/capability-roadmap/keystone-c-performance.md`
§2 measures the flagship workload at **8,090 simulated cycles/s** warm-loop, ~4,600
including `initSimulation`, and **1,100–1,450 cycles/s end-to-end from the CLI**.
Digital's published claim, per CAP-28's own evidence line, is 120 kHz. Whatever the
workloads' incomparability, a reader will do that division. The loss is not "at least
one place"; on the headline number it is the entire first impression.

An AC that treats the loss as a garnish will produce exactly the document the Outcome
says it wants to avoid — because the honest version is not "we are slower here, and
here is one example," it is "we are slower, and here is precisely why, and here is what
the cost buys you." I am therefore **disregarding AC-4 as written** in favour of:

> The doc contains a trade table, not a concession. Each row: a workload class, who is
> faster, the mechanism (JLS's event-driven `PriorityQueue` + `HashSet` dedup is 47.7%
> of the measured loop; Digital's is a different evaluation strategy), and what JLS's
> choice buys — per-element propagation delays, multi-driver/HiZ resolution, causal
> event ordering, VCD-faithful traces. That is the honest claim, and it is one a
> competitor's marketing line cannot answer.

Second-order point: satisfying an *honesty* criterion by quoting a competitor's number
measured on their workload and their hardware is self-undermining. Either source the
loss from mechanism (above), or pull the cheapest slice of #560 forward — one afternoon
running Digital and Logisim-Evolution once on the same box — so the sentence is
measured rather than quoted. Ordering #732 before #560 currently forces the worse option.

## Reframing 1 — generate the numbers; don't write them

AC-2 ("numbers taken from the suite's machine-readable output rather than transcribed")
has no observable difference from its own violation: a transcribed number and a
generated one are byte-identical, so no reviewer and no test can tell them apart. The
criterion is a promise, not a check.

#730 already specifies the machine-readable results file and already says
`docs/performance.md` should be "generated or checked from it." Take the strong form:

- `docs/performance.md` = hand-written prose + a delimited generated block
  (`<!-- BEGIN GENERATED: perf-results v1 -->`) rendered from #730's results file by a
  committed command.
- A test regenerates the block and fails on any diff outside declared-volatile fields.

Three consequences worth the extra hour. AC-2 becomes structurally true instead of
aspirational. #733's "check fails the build when an uncited performance claim appears"
gets a machine-readable source of truth to check *against*, rather than a regex hunting
for digits near the word "faster". And a large part of #557's staleness defence
collapses into "regeneration produced a diff" — the lane still needs bands for
*measurement* drift, but "the doc no longer matches the results file" stops being a
separate mechanism.

## Reframing 2 — split the receipts by reproducibility class

AC-3 ("an independent party can reproduce each published number within a stated
tolerance") is unachievable for throughput and trivially achievable for something the
issue never mentions. keystone-c states the rule itself: *"Ratios are robust; absolute
nanoseconds are machine-specific."* A reader on 2015 hardware will miss any honest
throughput tolerance by 3×, and the doc's central promise will be the first thing that
breaks.

But the event census is **bit-exact on every machine and every JDK**: 2,331,793 events
fired, 2,596,496 posted, 264,702 dup-suppressed, max queue depth 12,093,
`PinChanged` 82.3%. Those are properties of JLS, not of the box. Publish in three tiers,
each with its own tolerance statement:

| tier | example | tolerance |
|---|---|---|
| deterministic (JLS facts) | event counts by payload/callback, dup-suppressed, queue depth, node/net census | exact — any mismatch is a bug |
| machine-normalized | ns/event, events per clock cycle | narrow band, hardware-class-tagged |
| wall-clock | cycles/s, events/s | stated hardware only; **explicitly no cross-hardware claim** |

This is a better doc *and* it is the tier the rest of the programme actually consumes:
#442's event-count equality is asserted against exactly those integers, and #557's
ceilings are honest only against the normalized tier. A doc that publishes only tier 3
gives the internal consumers nothing.

## Reframing 3 — publish phase-split numbers, and fix `SigSim` first

keystone-c §2 shows `initSimulation` at 0.568 s against `runEventLoop`'s 0.742 s on
k2000 — a 43% swing depending on which you meant — and identifies the cause exactly:
`src/jls/elem/SigSim.java:64,67,71,74` builds the de-commented vector text with repeated
`String +=`, quadratic in vector size, 259 of 1215 JFR samples. It calls the fix "a
`StringBuilder` and an afternoon."

The first published number becomes the anchor everyone cites, and #557 will band it.
Publishing a headline cycles/s that is ~43% inflated by a known one-afternoon bug, then
re-publishing when it is fixed, spends the credibility this capstone exists to buy.
**Land the `SigSim` fix before the number is published**, and publish all three phases
(CLI end-to-end, init+loop, warm loop) rather than choosing the flattering one — the
end-to-end figure is what a student actually experiences and is the number an
educational tool should lead with.

## The duplication the issue has not noticed

`docs/capability-roadmap/keystone-c-performance.md` is **already in `docs/`**, is
already public to anyone browsing the repo, and already publishes throughput with
methodology in more depth than #732 asks for — produced by ten harnesses
(`KernelProbe`, `KernelProbe2`, `Census`, `NoDedup`, six microbenchmark drivers) that
live in a `/tmp` scratchpad path recorded in §12 and exist nowhere in the tree. Five
files under `docs/` carry performance numbers of this kind. That is the single largest
unbacked-public-claim liability in the repository, and #733's sweep walks straight into
it.

#732 should decide the relationship rather than leave it to the sweep: either
`docs/performance.md` supersedes for public numbers and each capability-roadmap document
gains a dated banner marking it an internal research record, or its numbers are
re-derived from the committed suite. Both are cheap; discovering the collision during
#733 is not.

## An alignment win the issue misses

`ARCHITECTURE.md:355` records the decision not to build a levelized/compiled pass, with
revisit trigger "a concrete CPU-scale design on the `riscv/` trajectory that is unusably
slow interactively." That is not a testable condition and nobody will ever agree it has
been met. keystone-c already recommends restating it quantitatively (e.g. "below 10
kcycles/s on the #202 golden's CPU").

`docs/performance.md` is the natural home for that threshold. Put the published number
and the architectural revisit trigger on the same page, both fed by the same suite, and
the doc stops being a marketing surface and becomes a live design instrument: the day
the measured number crosses the line, a recorded scope decision reopens itself. Nothing
else in the CAP-28 chain does that, and it costs one section.

## Shape

Model it on `docs/reproducibility.md`, which solved this exact problem for builds:
specified set → environment record → third-party recipe → CI gate → explicit
out-of-scope. Five sections, the same order, and — like the README's careful "note the
scope of each guarantee" paragraph — an explicit statement of what each number does
*not* claim.

**Verdict: endorse-with-reframing.** The goal is right and squarely on the project's
arc. Generate the numbers rather than writing them, publish deterministic receipts
alongside wall-clock ones, fix `SigSim` before anchoring a figure, resolve the
keystone-c collision here rather than in #733, and replace AC-4's single conceded loss
with a trade table that makes the loss the point.
