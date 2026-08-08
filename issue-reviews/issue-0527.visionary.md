# Issue #527: FEAT-C23-1: a docked chronogram opens on the live event stream — grouped signals, bus radix, cursor-delta measurement — and costs the kernel nothing while it stays closed
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Read against CAP-23 (#504), #527 is not "JLS needs a waveform viewer." It is the
*identity and time substrate* the rest of the capstone hangs off: PF-2 needs a
waveform edge that knows which element emitted it, PF-3 needs a transition to
point back into the scheduler, PF-6 needs a cursor time that can drive a replay.
The chronogram is the visible half; the load-bearing half is a stable per-signal
identity plus a stable sample stream. The issue's own AC-5 ("consumes a stable
seam, not the concrete queue structures") is the only criterion that names this,
and it is the one that should be driving the design rather than riding last.

That reframing matters because JLS is not going to beat GTKWave or Surfer at
rendering waveforms, and should not try. The project already ships the
professional handoff (#216, `docs/vcd-interop.md`) and has recorded that live
co-simulation stays out (#63). JLS's differentiator is the *causal* loop, not
lane rendering. Scope the panel to exactly what keeps a student's eye between
the edge and the schematic — and no further.

## Where the issue's premise is wrong about HEAD

The Outcome paragraph says the shipped surface "shows recorded samples and that
is all," and #504's Background repeats it. Reading `src/jls/edit/Trace.java` and
`src/jls/edit/InteractiveSimulator.java` at HEAD, four of the six things #527
promises already exist in rough form:

- **Live feed, not recordings.** `InteractiveSimulator.afterEvent` (:879) pushes
  values into `Trace.addValue` on the sim thread, and `Traces.draw` commits them
  on the EDT (`Trace.commit`, `Trace.java:214`). This is already an event-stream
  tap; #527 is not adding one, it is *replacing* one.
- **Bus radix.** `displayBase` (:102) with bin/dec/hex buttons (:271) →
  `Traces.setBase` (:1310) → `Trace.setBase` (`Trace.java:620`), rendered through
  `BitSetUtils.ToString(value, base)`. The gap is that radix is *global*, not
  per-lane; AC-1's "user-chosen radix" is a per-signal model property, ~a day.
- **Reordering.** `Trace.mousePressed` (`Trace.java:514`) offers move to
  top/up/down/bottom, and the order persists into the circuit file via
  `Element.setTracePosition` (`Element.java:192`), replayed by `Traces.setup`.
- **One cursor with a value readout** (`Trace.java:398-429`), including the
  scrolled-out-of-view name label.

Genuinely new: *grouping*, the *second* cursor and delta, docking/default-hidden,
and the closed cost ratchet. That is a materially smaller feature than 4–6 mw of
"the panel is real GUI work," and the estimate should be re-derived after
someone actually reads `Trace.java`. Filing a fresh chronogram beside the
existing one, rather than growing it, would leave JLS with two waveform surfaces
and two radix models — the exact failure the #534/#535 boundary comment works so
hard to avoid.

## The reframing: one sample stream, and the cost problem disappears

AC-4 asks for a bespoke benchmark proving a tap is free when closed. There is a
route where "free when closed" is structural rather than measured, and it is
half-built already.

`Simulator.probeSample(name, bits, time, value)` (`Simulator.java:285`) is
exactly the seam AC-5 describes: a value-typed push, in the headless core, that
touches no queue structure. `WireNet.propagate` (`WireNet.java:518-527`) already
fires it for every probed net; `BatchSimulator.probeSample` (:295) folds it into
the VCD. The interactive engine ignores it entirely and instead does, per event:

```java
Trace tr = traceMap.get(el);                       // InteractiveSimulator:885
for (Wire wire : wireMap.keySet()) {               // :891
    wireMap.get(wire).addValue(wire.getValue(), now);
}
```

That is a HashMap lookup plus a full iteration of every probed wire, on every
event, whether or not the trace area is dragged open — plus a `BitSet.clone()`
per accepted change inside `addValue`. **HEAD is already the "build with the tap
unconditionally enabled" that AC-4 wants to show red against.** So:

1. Define one `SampleSink` in `jls.sim` — `(signalId, bits, time, value)` — and
   let `Simulator` hold zero or more. `probeSample` becomes its first producer;
   watched-element samples become the second, moved out of `afterEvent` and out
   of contact with `SimEvent` altogether. With no sink attached, the per-event
   cost is one `isEmpty()` branch, and *nothing* about the tap can be
   proportional to probe count.
2. `BatchSimulator`'s VCD fold, TASK-0010's streaming writer (#405), the
   chronogram, #534's pre-trigger ring buffer and #535's replay display all
   become sinks over the same stream. #405 is described in #527 as "adjacent but
   distinct: this panel displays, it does not dump" — true at the surface, but
   both consume the identical change list, and #405 §7.10 has already written
   that list's ordering contract down formally. One producer, several sinks, is
   strictly less machinery than a display tap plus a dump path plus a capture
   path.
3. AC-4 becomes a *counting* test, not a tolerance band: assert zero sink
   invocations and zero per-event allocations when the panel is closed, plus a
   coarse throughput check. #405 §9 already learned this lesson in this repo —
   "asserted as a ratio bound rather than a wall-clock threshold, so the test
   does not flake on a slow runner." A `ChronogramClosedCostTest` with a
   measured tolerance band on kernel throughput, in CI on shared runners, will
   be quarantined within two months.
4. The falsification transcript AC-4 demands should be recorded against **HEAD**,
   not against a strawman build constructed for the purpose. It will be red, and
   it will be red for a real reason.

This also disarms the issue's own cost rhetoric. The 47.7% figure
(`docs/capability-roadmap/README.md:764`) is about *event-queue* bookkeeping —
it is #476's and keystone C's number, not the tap's. Citing it as justification
for tap frugality is a non-sequitur; what it actually argues is that a per-event
HashMap iteration lives in the same expensive neighbourhood and should be
deleted regardless of whether anyone ever builds a chronogram. That deletion is
a good standalone task and would make this feature cheaper, not more expensive.

## The seam the issue does not cut, and must

`Traces` is an inner class of a 1437-line `InteractiveSimulator` that also owns
run control, the toolbar, the clock label and the status bar; `Trace` is a
`JPanel` where the waveform model (change list, radix, cursor time → value
lookup, `firstChangeAtOrBefore`) and the Swing rendering are the same object.
Adding grouping, per-lane radix and a cursor pair to *that* is how you get a
third thousand-line class.

The alternative: a `WaveModel` — lanes, groups, per-lane radix, cursor pair,
delta, time↔pixel mapping — as plain headless types in `jls.sim` or a new
`jls.wave`, with the Swing panel a thin renderer over it. This is not a
cleanliness preference; it is the only way #527's acceptance criteria are
*testable in this repo*. CI is headless with no display (ARCHITECTURE.md, "Test
layout"); `test/jls/ui` layer 1 is headless model assertions and layers 2–3 are
still reserved under #91; the `gui-wayland` lane is a screenshot smoke test, not
an assertion harness. "Signals can be grouped and reordered" and "two cursors
report the time delta" are verifiable at layer 1 against a model, and verifiable
nowhere at all against a `JPanel`. CAP-23's AC-1 ("passes as a scripted GUI
test") quietly assumes a harness that does not exist; #527 is the right place to
notice that and to sidestep it by putting the semantics in testable objects.
`TraceGeometry` (already extracted for exactly this reason, per the #121
comments in `Trace.java:263`) is the precedent to follow, at a larger scale.

## Two smaller course corrections

- **Do not persist grouping into the `.jls` file.** `tracePosition` already leaks
  view state into the circuit format, which `docs/file-format.md` governs as a
  normative contract with round-trip tests. Groups, per-lane radix and cursor
  positions are per-user view state; put them in a sidecar or `UserPrefs` keyed
  by circuit path, and record the intent to migrate `tracePosition` there later.
  #534 genuinely needs its instruments in the file (they are circuit content);
  #527's view state genuinely does not, and conflating the two is how the format
  accretes.
- **AC-3 is nearly free and should say so.** `InteractiveSimulator`'s constructor
  already returns early on `JLSInfo.batch`, and `HeadlessCoreRatchetTest` already
  forbids AWT/Swing/`jls.edit` imports in `jls.sim`. Under the sink design, "no
  chronogram code runs headless" is enforced by the existing ratchet plus "batch
  attaches no chronogram sink." Writing it as a new criterion invites a new
  bespoke test for a property two existing mechanisms already hold.

## Alternative considered and rejected

*Make the in-tool panel a thin client of an external viewer* — i.e. promote
#535's Surfer cursor-sync to be PF-1 and skip the docked panel. It is genuinely
tempting: zero rendering code, professional-grade waveform tooling for free.
Rejected, and the reasons are worth recording: the offline single-jar deployment
model (README, "Installing JLS") means the default student has no viewer
installed; #535 AC-4 already commits to the in-tool loop standing alone; and
cross-probing (PF-2) requires the edge→element identity to live inside JLS
anyway. But the rejection argues *for* a minimal in-tool panel, not a rich one:
anything past grouping, radix and a cursor pair is duplicating a commodity JLS
has already documented a handoff to.

## Verdict

Endorse the outcome, with the design re-cut: build the sample-stream seam and a
headless `WaveModel` first, grow the shipped `Trace`/`Traces` surface into the
chronogram rather than filing a second one, re-derive the 4–6 mw band against
what HEAD already does, and replace AC-4's tolerance-band benchmark with a
structural counting test whose falsification transcript is recorded against
HEAD. AC-5 is the criterion that should be listed first, because satisfying it
properly makes AC-3 and AC-4 fall out for free.
