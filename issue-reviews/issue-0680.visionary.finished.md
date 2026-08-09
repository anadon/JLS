# Issue #680: TASK-C527-2: a docked chronogram panel draws live waveforms off the event tap — signals groupable and reorderable, buses in a chosen radix
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

#680 is PF-1's visible half: the waveform surface that CAP-23 (#504) hangs cross-probing,
the cause-chain inspector, and rewind off. That end is right and squarely on the project's
arc — lf-03 argues persuasively that a first-year with a wrong adder bit has only forward
tools for a backward question, and every later PF needs a time axis to point at. I endorse
building it.

What I do not endorse is the shape, because the shape rests on a premise that HEAD
contradicts. The issue (and #527, and #504's Background) says the trace window "shows
recorded samples and that is all", and that a multi-bit signal renders "as N unlabelled
rows". Neither is true at HEAD:

- It is already live. `InteractiveSimulator.afterEvent` (`:879-896`) calls
  `Trace.addValue` on the sim thread and the run loop commits (`Trace.java:180`, `:214`);
  the panel paints as the simulation advances. That is AC-1, shipped.
- Reordering already exists — `Trace.mousePressed` (`:513-565`) pops "Move Trace To
  Top/Up/Down/To Bottom" into `Traces.move` (`InteractiveSimulator.java:1336`). That is
  half of AC-2, shipped.
- Radix already exists — `Trace.base` with `setBase(2|10|16)` (`:620-624`), rendered
  through `BitSetUtils.ToString` (`:341`), driven today by a *global* menu
  (`InteractiveSimulator.java:270`). The field is already per-`Trace` instance.
- A bus is already one lane with its value drawn as text between transitions
  (`Trace.java:328-347`), not N rows. And a cursor already reads out the value at a time
  (`:403-428`).

So the deliverable in #680 that does not exist is: named groups, and per-lane rather than
global radix. Two to three maintainer-weeks and a new panel package is a very large
vehicle for those two. Worse, the thing lf-03 identifies as the actual defect is not in
the acceptance criteria at all: **the waveform is wiped on every run**
(`InteractiveSimulator.java:612-619` clears `traces`, `traceMap`, `wireMap`, `memTraces`
before each `runSim`), and nothing about which signals a student cares about survives
anything. AC-2 asks that grouping survive "a pause/resume within the session" — a bar the
current panel clears trivially, since pause does not touch the trace list. That criterion
is not measuring the property that matters.

## Reframing 1 — cut at the signal model, not at a new panel

The durable object here is not a panel. It is an **observed-signal model**: an ordered
list of observations, each with a stable name, a width, a radix, and a group, owned by the
circuit rather than by a Swing component. Cut there and the whole cluster simplifies:

- **Grouping and per-lane radix become properties of the model**, rendered by the existing
  `Traces` column (a BoxLayout list with a `move` API — a group is a header row plus a
  contiguity rule over that list) and the existing per-instance `Trace.base`. This is
  plumbing an existing field to the row popup that already exists, not new painting code.
- **Persistence goes through the pipeline JLS already has.** Watched/probe state already
  serializes (`probe` is a `setValue` line kind per ARCHITECTURE's load protocol); lane
  order, group names, and radix belong in the same records via `Attribute`. Then the
  arrangement survives a save and a reopen — and, far more valuable pedagogically, an
  *instructor* can ship `lab3.jls` with `operands`, `carry chain`, and `sum` already
  grouped and the bus lanes already in hex. Chronogram configuration becomes course
  material. That is worth an order of magnitude more than "survives pause/resume", and it
  is not more work; it is the same work aimed one layer lower.
- **One model, four consumers.** The same list is what `BatchSimulator.afterEvent`
  (`:140-180`) and `toVcd` should name and order signals from, what `BatchTracePrinter`
  prints, what #373's probe names attach to, and what PF-2 cross-probing and PF-6's viewer
  sync resolve identity against. Today "which signals are observable and what are they
  called" is re-derived independently by `findTraces` (`:967-1019`) and `findWatched`/
  `findProbes` in the batch path. A new panel with its own third answer makes that worse;
  #504 risk 1 warns against exactly this kind of two-model drift.

I am explicitly disregarding AC-2's "survives a pause/resume within the session" and
replacing it with "survives save/reopen and is byte-visible in the plain-text save", and
AC-3's fixture assertion should assert the *model's* formatting, not a panel's pixels.

## Reframing 2 — the tap already exists; the interesting cost is elsewhere

AC-5 asks the panel to read only #678's consumer interface and no "event queue types", so
#476 can land underneath. But `Simulator.afterEvent(SimEvent)` (`Simulator.java:241`,
default empty at `:269`) is already that hook: it is called once per retired event, is
already the sole feed for both the interactive traces and the batch VCD, already takes an
event rather than the queue, and is already free when unused. #678 formalizes a seam that
is 90% present. Fine — but then #680's AC-5 is a test about #678, not about this panel,
and it should not be spent here.

The real cost in the interactive observation path is not a branch on an empty consumer
list. It is this, in `afterEvent`:

```java
for (Wire wire : wireMap.keySet()) {
    tr = wireMap.get(wire);
    tr.addValue(wire.getValue(), now);
}
```

Every probed wire is re-polled on *every* retired event, whether or not it changed — O(probes)
per event, with a synchronized `addValue` and a `BitSet.equals` each. The elegant move is to
invert it: let net changes push. `WireNet.propagate` is where a value actually changes, and
lf-03 already names it as the journal write point for the causal work. An observation seam cut
at *net change* — `(signal identity, value, time)` — costs nothing when unobserved, deletes
this poll loop, hands the chronogram exactly the tuple stream it wants, and gives PF-3's
journal and PF-1's lanes **one** stream instead of two. Cutting at event-retire instead keeps
the poll loop and mints a second observation model beside the one PF-3 will need anyway.

## Reframing 3 — win AC-4 structurally, by inverting the simulator/view relationship

AC-4 wants a test that fails if the batch path touches the panel's package. As written it is
a package-name check that will be green on day one and will never catch the thing that
actually goes wrong. Note what HEAD does: `private Traces traces = new Traces();`
(`InteractiveSimulator.java:92`), plus the `JButton`/`JLabel`/`JTextField` fields at `:55-84`,
are *field initializers* — they run before the constructor's `if (JLSInfo.batch) return;`.
Batch already constructs the trace container. "Constructs no panel in headless mode" is
therefore a stronger claim than the current code supports, and a package-name assertion will
not notice.

The structural fix is the one this feature is a good occasion for: `InteractiveSimulator` is
1437 lines of Swing living in a class ARCHITECTURE.md still describes as `jls.sim` — the
simulator *is* the view. Invert it. The simulator publishes observations; the editor owns and
lazily creates the waveform view. Then "no chronogram in headless" is true by construction
(the sim module cannot name a view type), the `jls.sim` headless ratchet extends to cover it
for free, and CAP-23's later PFs attach to a view the editor owns rather than to the engine.

## The alternative I considered and reject

Do not build a panel at all: stream live VCD to a FIFO and drive Surfer's remote-control
protocol (#504 OQ-2 already contemplates it for PF-6). It would be cheap, and it is the
professional workflow. I reject it as the *primary* path — a first-year on a lab machine with
one self-contained jar must not be told to install a waveform viewer, and README's whole
deployment story is "one file, offline". But harvest the one good thing from it: if the
chronogram's signal set, names and radix are the same model the VCD writer serializes, the
in-app view and the exported dump name the same signals, and PF-6's cursor sync becomes a
mapping between two views of one model instead of a name-matching heuristic.

## What I would keep from #680 verbatim

AC-1 (live lanes) as a regression guard on behavior that already exists; AC-3's insistence
that rendered bus text be *asserted* rather than eyeballed — that discipline is right and the
existing `Trace` has no such test; and the default-hidden posture. Keep the band at 2-3 mw
only if it buys the model and the inversion; a second waveform panel standing beside
`Trace`/`Traces` — with two retention policies and `TraceGeometryTest`/`TraceRetentionTest`/
`TraceWindowingTest` still pinning the old one — is the outcome to forbid outright. Grow the
shipped panel, or move it wholesale into the new package and delete the original. Not both
alive; ARCHITECTURE's #78 lesson about sixteen places to touch is the same lesson.
