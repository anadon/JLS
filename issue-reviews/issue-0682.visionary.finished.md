# Issue #682: TASK-C527-3: two cursors measure a time delta in ticks, and in physical units the moment a circuit declares what a tick means
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its packaging, #682 asks JLS to acquire its **first quantitative
measurement instrument**. Everything JLS shows a student today is qualitative:
`Element.showCurrentValue` gives one value with no time attached, and the trace
window draws shapes. The one number in the whole viewer is the absolute
simulation time under the cursor — `InteractiveSimulator.Header.paintComponent`
(`src/jls/edit/InteractiveSimulator.java:1422-1426`) prints `time` and nothing
else. A delta is the first thing in JLS that turns a drawn waveform into an
*answer*: propagation delay, pulse width, clock-to-Q, skew.

That goal is squarely on the project's arc. `docs/capability-roadmap/README.md`'s
P4 programme is built on exactly this claim — "the critical path, highlighted on
the schematic … *that is the comparison the whole adder unit of a first-year
course exists to teach*, and today JLS can only assert it verbally" (:441-444).
A cursor delta is the smallest thing in the tree that starts paying that debt.
I endorse the goal without reservation. The reframing below is about **which
seam it is cut along, and when it arrives** — and on both counts the issue as
written works against its own purpose.

## Reframing 1 — the delta is a headless query over the change history, not a paint-time pixel subtraction

The issue never says where the measurement lives, and the implied answer is
"inside the chronogram panel, next to the cursor drag handlers." That is the
wrong seam, and the tree already shows the right one twice.

`jls.sim.TraceGeometry` exists precisely because tic/label geometry was extracted
out of `Trace.paintComponent` so its properties could be unit-tested, and its own
javadoc records the discipline: "Deliberately free of AWT so it stays inside the
headless core (`HeadlessCoreRatchetTest`)." `Trace.firstChangeAtOrBefore`
(`src/jls/edit/Trace.java:445-458`) is already a pure binary search over a value
history with a package-private test seam bolted on beside it. The measurement
belongs in the same place: an AWT-free type over a `(time, value)` history that
answers *"what is the tick delta between these two anchors, and which transitions
bound it"*. The chronogram then renders that answer; it does not compute it.

Three things fall out of that seam at no extra cost:

- **AC-2 becomes honestly testable.** As written, "asserted against a fixture
  whose transition times are known" has to be asserted through the GUI, and per
  ARCHITECTURE.md's test layout, `test/jls/ui` layers 2 and 3 (Swing harness,
  render-to-image) are **reserved, not built**. Layer 1 can only assert a model —
  so the model has to exist. Cutting here is not stylistic preference; it is the
  only route to the criterion the issue already demands.
- **The same measurement reaches batch.** JLS's real differentiator is that
  `docs/batch-interface.md` is a *stability contract* consumed by autograders. A
  delay measurement that exists only under a mouse cannot be asserted in a `-t`
  vector, cannot appear in a lab autograder, and cannot be regression-tested.
  The same headless query makes "assert clk↑→Q↑ ≤ N ticks" a batch capability —
  two consumers for the price of one, and it is the capability P4's timing-check
  tier (`README.md:415-421`, "a violation can be *reported* without being
  *modelled*, and reporting is 90% of the teaching value") wants first.
- **It stops being throwaway work.** #680 explicitly *replaces* `Trace.java` and
  the `Traces` panel. Cursor code written inside that panel dies with it; a
  headless measurement type survives the panel rewrite untouched.

## Reframing 2 — anchor cursors to transitions, not to pixels (this is a correctness argument, not taste)

Today's cursor is an `int` pixel: `Trace.sliderPos`, converted by
`Trace.mouseMoved` via `long time = now-(long)(width-x)*scaleFactor`
(`Trace.java:496`). At `scaleFactor > 1` — and the scale factor is a
user-editable field (`InteractiveSimulator.java:222`) — **one pixel is many
ticks**, so a pixel-anchored cursor cannot express "exactly at the transition."
A delta computed from two such cursors is quantized to `scaleFactor` ticks. AC-1
asks for snapping and AC-2 asks for an exact tick figure asserted against known
transition times; with a pixel anchor those two criteria are in direct conflict
at any zoom level other than 1, and a test written at the default scale will pass
while the feature is silently wrong where students actually use it (zoomed out,
hunting a glitch).

Anchor each cursor as `(trace, tick)` — snapped to a transition, or free — and
the pixel becomes a rendering detail. Scale-invariant, resize-invariant,
zoom-invariant, and the delta is an exact `long` subtraction with no geometry in
it at all. The snapping primitive needed is `nextChangeAfter(time)`, the exact
mirror of the `firstChangeAtOrBefore` that `Trace.java` already has.

## Reframing 3 — measure *named edges*, not two arbitrary x positions

"Two cursors and a delta" is the GTKWave idiom, imported wholesale. The
pedagogical prize is narrower and better: **an edge-to-edge measurement that
names its endpoints.** If a cursor snaps to a transition it already knows which
signal and which direction; the readout can be `clk↑ → Q↑ : 12 ticks` instead of
a bare number. That is the difference between a ruler and an instrument — it
survives being screenshotted into a lab report, it is what an autograder would
assert, and it is precisely the reading P4's setup/hold, clock-skew and
critical-path labs need. The cost over a bare delta is one label.

## Reframing 4 — delete AC-3 and AC-4; let #882 own every time display

The comment on this issue correctly records that nothing in JLS can declare what
a tick means and that #882 owns that. The stronger conclusion the comment stops
short of: **the physical-unit clause does not belong in this issue at all.**
#882's AC-6 already says "the declared unit reaches the consumers that exist
today — the VCD `$timescale`, the waveform axis and delay-dialog suffixes." Once
this task produces a tick count from a headless query, a delta *is* a waveform
time display, and rendering it in the declared unit is the same one call to
`TimeBase.seconds(long)` that #882 must make in three other places anyway.

So: **I am disregarding acceptance criteria 3 and 4 as scoped here.** Not
waiving them to a later date — relocating them. Keeping them on #682 creates the
cross-programme ordering edge the completeness review had to invent, invites the
second conversion #882 §7.5 exists to prevent, and leaves "does this display
render declared units" as a per-site decision made independently at four sites.
Delete both criteria, record `TIMEBASE` display as a #882 obligation covering the
delta readout, and the only cross-programme edge in CAP-23's roster disappears
rather than being documented. AC-4's "no fabricated unit, no placeholder" is then
a global property of one display policy, not a promise re-made per widget.

## Reframing 5 — the ordering is backwards, and that is the costliest line in the issue

`ordering_after: [TASK-C527-2]` chains a 1 mw item that delivers the entire
student-visible payoff behind a 2-3 mw panel rewrite, which is itself behind a
1-2 mw kernel-seam task (#678) gated on a *falsification-first performance
ratchet*. The highest-value, lowest-risk, least-reversible-decision work in the
family is scheduled last, behind the two pieces most likely to slip.

Nothing in the measurement needs either. Today's `Traces` panel already
broadcasts one cursor to every lane (`InteractiveSimulator.java:1375-1383`),
already renders it per-lane with the value at that time (`Trace.java:397-429`),
and already prints the cursor's absolute time in the header. **A second anchor
and a subtraction is the whole delta.** Land the headless measurement plus a
second cursor on the panel that exists, now, in parallel with #678; #680's
chronogram inherits a tested measurement instead of writing one. The ~40 lines of
paint and mouse code that #680 discards are lines #680 was rewriting regardless.
The roadmap's own warning about building two things against two layouts six
months apart cuts *for* this ordering, not against it: the durable half is
written once, and only the disposable half is written twice.

## What I would keep verbatim

- **AC-5.** Cursor state is view state, not `.jls` state — right, and the
  byte-identical-save discipline is this project's strongest habit. One
  amendment in the same spirit: `InteractiveSimulator.runSim` wipes all trace
  history on every run (`:612-619`), so a pixel cursor is destroyed by a re-run.
  A `(trace, tick)` anchor can be re-snapped after a deterministic re-run, which
  is what a student comparing two design variants actually needs.
- **The "never invent a unit" instinct.** It is the same instinct that keeps
  `docs/simulation-semantics.md` §1 calling `1 ns` a compatibility mapping.
  Relocating the clause to #882 preserves it; it does not soften it.

## Net

The goal is right and under-scoped in the direction that matters. Build the
measurement as an AWT-free query over the change history, anchor cursors to
transitions rather than pixels, name the edges in the readout, hand the
physical-unit rendering to #882 entirely, and unchain it from #680 so the payoff
arrives before the rewrite rather than after it. Under that framing this stops
being a 1 mw ornament on a panel that does not exist yet and becomes the first
brick of P4's measurement tier — one that batch mode, autograders and the future
chronogram all stand on.
