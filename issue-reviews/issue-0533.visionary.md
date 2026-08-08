# Issue #533: FEAT-C23-4: single-event stepping animates each propagation wavefront along the wires — per-gate delay stops being a number in a dialog and becomes time a student watches pass
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the mechanism away and the claim is: *JLS computes a causal, timed
trajectory and shows the student only its endpoints.* CAP-23 (#504) says the
same thing at capstone scale, and `docs/capability-roadmap/lf-03-causal-debug.md`
says it in the tree's own words — "nothing in the tree records why a value is
what it is." PF-4's specific job is the smallest, most visceral instance of it:
make propagation delay a thing you *watch*, not a number you typed into a dialog.

That end is right, unusually well-aligned with what JLS is for, and worth
funding. The route proposed to reach it is wrong twice, and I am explicitly
disregarding acceptance criteria 1 and 2 as written.

## Objection 1: the animation, as specified, teaches the wrong physics

`docs/simulation-semantics.md` §6.1 is normative and unambiguous:

> **Wires are ideal**: propagation across a net takes zero simulation time and
> the whole net carries one value. There is no per-wire or per-segment delay.
> … Consequence: all delay in JLS lives in elements, none in wiring.

The issue's outcome text asks to "watch the propagation animate **along the
wires**, with each gate's delay visibly consuming simulated time." Those two
clauses are in tension. A wavefront crawling down a wire renders transit time
in the one place the model has none, and renders zero time in gates — the one
place the model puts all of it. In a *pedagogy* tool this is not a cosmetic
quibble: the whole point of the feature is to install a mental model of where
delay comes from, and the specified animation installs the opposite one. A
student who watches JLS for an hour will come away believing long wires are
slow, which is true of real silicon for reasons JLS deliberately does not
model and cannot explain.

## The reframe: draw the event queue on the canvas, not motion along wires

The honest visual of "time is passing" in an event-driven model is **the set of
events currently in flight**. Concretely: for every `SimEvent` pending in
`Simulator.eventQueue`, draw a marker on its target element showing the
remaining delay (a fill bar inside the gate body, a countdown ring, whatever
reads at classroom projector size). Wires flip instantaneously when the event
fires, because that is what happens. Each step redraws the overlay.

This is better than wire-crawl animation on every axis I can find:

- **It is true to §6.1 and §6.2.** Delay is drawn inside elements. Ideal wires
  are drawn as ideal.
- **It needs no animation timer, no frame loop, no speed slider.** It is one
  extra pass in `paintComponent` over a queue snapshot. Wire-crawl needs
  wall-clock interpolation, which means a second timing domain (frames) layered
  on the sim thread — precisely the coupling that produced issue #49's H7/H8
  findings.
- **It makes the hazard lesson sharper, and earlier.** A static-1 hazard *is*
  two in-flight events on reconvergent paths with different remaining times.
  Under this overlay a student sees both markers on screen simultaneously, with
  unequal countdowns, before either fires. That is CAP-23 §1 step 2's insight —
  the two unequal-delay paths — delivered visually at step 3 for free, without
  PF-3's event-graph retention. Wire-crawl animation shows one wavefront at a
  time and never puts the two paths in the same frame.
- **It costs less.** CAP-23 bands PF-4 at 2–3 mw largely on animation. A queue
  projection is a paint pass and a step predicate.

Prerequisite the issue does not name: **JLS has no on-canvas signal-value
vocabulary at all today.** `Wire` has no draw method that consults its value;
`WireNet.value` is read only by `showInfo` text, probes, and traces. Values reach
the student through `ElementValueDisplays.show` modal dialogs and the trace
window, never the schematic. "Animate the affected wire(s)" therefore silently
presupposes a static wire-value rendering that has to be invented first. I would
lift that out as its own item and ship it ahead of everything in CAP-23 — a
schematic that colors nets by value during simulation is, I suspect, a larger
pedagogical win per mw than any PF in the capstone, and every PF here depends on
it.

## Objection 2: this should be a recorded trajectory, not an engine mode

AC-1 asks for "a stepping mode" in the interactive engine. AC-3 and AC-4 then
spend their strength defending against that mode: prove stepping reproduces the
free-run event sequence, prove batch stays byte-identical. Those criteria exist
only because the design reaches into the loop.

Cut the seam one layer out instead. `Simulator.afterEvent(SimEvent)` is already
a per-event hook, already receives the fired event on the sim thread, already
costs nothing when unoverridden, and `SimEvent` already carries `(time, seq)` —
a total order, so **an event ordinal is an address that exists in the model
today**. Attach a bounded trajectory recorder there, gated on the diagnosis UI
being open (the same tap PF-1 needs and KC-23-1 measures), and "stepping"
becomes a *view over a recording*, not a mode in the engine:

- AC-3 becomes true by construction. There is one run; a view cannot perturb an
  event sequence it is merely indexing.
- AC-4 becomes true by construction. Batch never opens the view, so the tap is
  the no-op base hook.
- Stepping gains a backward direction for free — which is #535's entire feature.

That last point matters for project shape. The dedup comment on this issue works
hard to keep #533 and #535 apart ("different directions, different mechanisms").
Read from the outside, the effort of that defense is the tell: both are controls
over "show me the canvas at ordinal N," and #535's chosen mechanism —
deterministic re-simulation truncated at T — is *literally this engine loop with
a different stopping predicate*. One control (an event-ordinal scrubber: drag
right to step, drag left to replay-from-0 with honest progress) yields both
outcomes from one mechanism. The project's own recorded discipline points here:
#221 keeps one simulation strategy; CAP-23 §3 risk 1 forbids "two cause-chain
data models." Two temporal-navigation models is the same mistake one abstraction
lower.

Corollary: `ordering_after: []` is the wrong metadata. This should sit behind
PF-1's tap, or share its seam by construction, or the capstone acquires two
independent doorways into the hot plane — exactly what §6's hot-plane rule and
KC-23-1 exist to prevent.

## Third, smaller point: absorb the existing Step control, do not sit beside it

`InteractiveSimulator` already ships Step (advance `stepAmount` *time units*, via
`stepEnd` in `beforeEvent`) and Animate (repeat that step every second). Adding
event-granular and wavefront-granular stepping as a new mode gives the toolbar
three temporal controls with overlapping meanings and one shared, delicate
`volatile`/`pauseSem` control path. The right shape is one Step button and a
granularity selector — `1 event | 1 wavefront | N time units` — where the
existing time-quantum behavior is the third setting. That is a net simplification
of a surface the project has already had concurrency bugs in, and it retires the
odd "Animate = press Step once a second" idiom in favor of real playback over the
trajectory.

## Sequencing note

AC-3 pins behavior on "the shipped hazard-demo circuit." No such circuit exists
in the tree (`examples/` contains only `autograde/`); it is a CAP-23 §1 premise
with no owner. Whoever files first should file the fixture, because the hazard
demo is load-bearing for AC-1, AC-3, AC-6 and this issue's AC-3 alike.

## Bottom line

Endorse the outcome — propagation observable at event granularity is one of the
highest-value things CAP-23 contains, and the single instance most likely to
change how a class understands timing. Rewrite the route:

1. Replace "animate along the wires" with an **in-flight event overlay**: pending
   events drawn on their target elements with remaining delay; wires flip
   instantly. Truthful to §6.1, cheaper, and it shows both hazard paths at once.
2. Replace "stepping mode" with a **recorded trajectory plus an ordinal
   scrubber**, tapped at the existing `afterEvent` hook, shared with PF-1 and
   subsuming #535's rewind.
3. Fold the new granularity into the **existing Step control** rather than
   adding a fourth temporal mode.
4. File the missing prerequisite — **on-canvas signal values** — separately and
   ahead of the capstone; nothing here is legible without it.
