# Issue #527: FEAT-C23-1: a docked chronogram opens on the live event stream — grouped signals, bus radix, cursor-delta measurement — and costs the kernel nothing while it stays closed
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

A docked chronogram panel (PF-1 of capstone CAP-23, #504) replacing the
existing `Trace`/`Traces` window with grouped/reorderable signals, bus
radix, and a two-cursor time-delta readout. The panel must be
default-hidden and its event-stream "tap" must be zero-cost when closed
(AC-4, `ChronogramClosedCostTest`), and must sit on a stable seam so
TASK-0063 (#476)'s queue replacement doesn't touch it (AC-5). The issue's
own comment already does duplicate-boundary work against sibling issues
#534/#535, which is good practice and not repeated here.

## Findings, most severe first

**1. [High] AC-4's cost justification borrows the 47.7% figure from a code path the chronogram's tap does not sit on, and the actual, already-built zero-cost precedent for this exact feature is never cited.**

The issue states: *"the kernel already spends 47.7% of warm loop time on
event bookkeeping, so the tap must be zero-cost."* That figure is
`PriorityQueue`+`dupCheck` `HashSet` cost inside `Simulator.post`/
`runEventLoop` (`docs/capability-roadmap/keystone-c-performance.md` line
52, restated in #476's abstract as "151.8 ns of the 318 ns"). A
chronogram tap does not sit there — it sits at the retire-path
notification point, `event.getCallBack().react(...)` in
`Simulator.java:239`, which is exactly where `InteractiveSimulator`
already hooks via `afterEvent(SimEvent event)`
(`src/jls/edit/InteractiveSimulator.java:879-896`). That method is
already gated by `if (!isQuiet())` before it touches `traceMap`/
`wireMap` — i.e., **the zero-cost-when-closed pattern this issue asks
for already exists in the codebase for the current trace window**, and
is unrelated to the queue/dedup structures the 47.7% figure describes.
This is the same misattribution flagged independently in this repo's own
review of #678 ("the 47.7% figure is borrowed from a different code path
than the one this issue's tap sits on"), so it is a repeated pattern
across sibling CAP-23 issues, not a one-off. As written, `AC-4`
motivates urgency with a number that doesn't apply to the mechanism in
question, and the issue misses the chance to just require "reuse the
existing `isQuiet()`-gated `afterEvent` seam," which is both cheaper to
verify and the seam that actually exists.
**Recommendation:** rewrite the Outcome paragraph and AC-4 to cite the
real cost model (a per-event virtual call plus a conditional, at the
`afterEvent`/retire-path seam), drop the 47.7% citation, and require the
implementation to extend the existing `isQuiet()`-style gate rather than
inventing new tap infrastructure.

**2. [High] AC-5's "stable seam, not the concrete queue structures" is a straw-man risk: the existing seam is already queue-implementation-agnostic, so the criterion doesn't actually constrain the design.**

`afterEvent` fires after `Simulator.runEventLoop` has already polled the
event off whatever queue implementation is in use
(`Simulator.java:220-241`); it receives only the already-dequeued
`SimEvent`, never touches `eventQueue`/`dupCheck` directly. TASK-0063
(#476)'s own interface contract (§7.4) confirms `Simulator.post` stays
the only enqueue path and the loop's poll/react/afterEvent shape is
unchanged by the queue swap. So the risk AC-5 is guarding against — a
chronogram tap coupled to `PriorityQueue`/`HashSet` internals — cannot
happen if the panel is built the obvious way (extending `afterEvent` or
a new but equally-shaped hook). As stated, AC-5 sounds like a real
design constraint but is unfalsifiable in a useful sense: any
implementation that hooks in at the natural point trivially satisfies
it, and the criterion never says what a *violation* would look like
(what would "consuming the concrete queue structures" even mean for a
display-only consumer that has no reason to see `eventQueue` at all?).
**Recommendation:** replace AC-5 with a concrete, checkable statement —
e.g. "the chronogram consumes `Simulator.afterEvent`/`SimEvent`
only, and does not reference `Simulator.eventQueue` or `Simulator.dupCheck`
by type" — so a reviewer or a test (a package-private field-usage check,
or simply "the panel class does not import `jls.sim.CalendarQueue`" once
#476 lands) can actually fail it.

**3. [Medium] AC-4's baseline fixture, "the first-year adder flow," does not exist anywhere in this repository and is not defined by the issue.**

Grepping the tree (`src/`, `test/`, `docs/`) for "first-year adder" or
any adder-specific golden/benchmark named that way returns nothing; the
only "first-year" hits are prose in `docs/capability-roadmap/*.md`
describing course context, not a fixture. `ChronogramClosedCostTest`
(AC-4) is asked to assert this named flow "match[es] baseline within
measured tolerance" without the issue naming the fixture file, the
tolerance, or the measurement methodology (wall clock? JFR sample
share? events/sec, per #476's own methodology, which at least states
units and in-loop-vs-`initSimulation` scope explicitly). Left this
vague, whoever picks up the task can satisfy AC-4 with any benchmark
they build and any tolerance they pick — which is exactly the "shown
red once… before any pass counts" falsification ask undermined by not
pinning what "red" and "tolerance" mean quantitatively.
**Recommendation:** name the actual fixture (a specific `.jls` circuit
under `examples/` or `test/resources/`), state the tolerance as a number
(e.g. "≤1% throughput delta"), and state the measurement method (in-loop
events/sec vs wall-clock), mirroring #476's own P10 discipline.

**4. [Medium] "K9" is cited as if it were a defined term, but resolves nowhere in this repository.**

The Outcome paragraph says the panel "is default-hidden (K9)" and AC-4
parenthetically tags itself "(CAP-23 AC-5, K9)." Searching `ARCHITECTURE.md`,
`README.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, and every file under
`docs/` for a definition of "K9" as a named rule or gate returns nothing
— its only "definition" is CAP-23's own kill criterion KC-23-1 ("a
diagnosis feature that taxes every user who never opens it fails K9 by
construction"), which restates the property being tested rather than
defining the term "K9" itself or what other criteria it might carry
(is K9 a formal severity/kill-criterion class shared across issues, or
an ad hoc label invented per-issue?). This same gap has already been
flagged independently against multiple sibling issues in this tracker
(#534, #542, #550, #766, #771, #780), so it is a systemic labeling
problem in the roadmap corpus, not specific to #527, but #527 inherits
it verbatim.
**Recommendation:** either define K9 once in `ARCHITECTURE.md`'s
"Recorded decisions" section (it already hosts comparable cross-cutting
rules like the i18n non-goal) and cite it, or drop the shorthand and
state the property in full at each use site.

**5. [Medium] AC-1's "grouped and reordered" signals and "bus radix" display are entirely new UI surface with no existing code to build from, and the issue does not size or scope this honestly against the 4-6 mw band.**

`src/jls/edit/Trace.java` (626 lines, read in full for signal/value
handling) has no concept of a bus, a radix, or a group at all — it is a
flat list of per-`Change` boolean/`BitSet` waveforms drawn one row at a
time (`Trace.HEIGHT`, `MAX_RETAINED_CHANGES`, the `Change` record). "a
user-chosen radix (bin/hex/dec at minimum)" implies at least a
value-formatting layer keyed to `Wire`/bus width that does not exist
today, and "signals can be grouped and reordered" implies persisted
per-panel layout state (also absent — `InteractiveSimulator.Traces`
today is an append-only `LinkedList<Trace>` in insertion order, no
reorder API). This is not necessarily wrong to want, but the issue
presents it as roughly the same size as the "docked panel + cost
ratchet" framing suggests (4-6 mw per its header and per #504's PF-1
line item), when in fact grouping/reorder + radix formatting each look
like their own design surface (persistence format, undo interaction,
at minimum). Plausible, but under-costed relative to what AC-1 actually
asks for.
**Recommendation:** split AC-1 into an MVP (single radix, no grouping)
and explicitly mark grouping/reorder as a stretch criterion with its own
cost line, or raise the band and say so.

**6. Solid, move on.** The issue's own boundary comment (#527 vs #534 vs
#535) is a real, well-reasoned deduplication check with concrete
acceptance-criteria citations on both sides — this is exactly the kind
of adversarial self-check that should exist before implementation
starts, and it holds up under a second read.

**7. Solid, move on.** AC-3 ("no chronogram code runs in headless/batch
mode") is consistent with the existing architecture: `jls.sim` stays
AWT/Swing-free under `HeadlessCoreRatchetTest`
(`ARCHITECTURE.md` "Threading model"), and a GUI-only panel living in
`jls.edit` naturally satisfies this without new enforcement machinery.

## Verdict rationale

Two of the six acceptance criteria (AC-4, AC-5) rest on either a
misattributed performance figure or an unfalsifiable-as-written
constraint, and a third (AC-1) is under-scoped relative to what it
literally asks for. None of this is fatal — the feature itself is
buildable on the existing `afterEvent`/`isQuiet()` seam, which the issue
doesn't even need to invent — but the acceptance criteria as written
would let a `ChronogramClosedCostTest` pass without ever having tested
what the issue's own Outcome paragraph claims it tests. Needs rework
before it's ready to implement against.
