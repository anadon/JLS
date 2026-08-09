# Issue #620: TASK-C490-2: the trace window gains a real-valued row and a headless CSV form, because the value domain today admits only a BitSet or null
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its framing, #620 buys one thing: **a way for a quantity that is not a
logic value to become visible and testable**. Three capstones want it — #490's
reflection lab, #303's audio front end, #305's analog work — and none of them can
show its result today. That end is right, it is genuinely shared, and starting it
on day one is the correct call. Everything below is about the route, not the
destination.

The framing, however, is wrong in a way that matters to the project's arc. The
title says "the value domain today admits only a BitSet or null." That is true of
`docs/simulation-semantics.md` §2 — and it is not the obstruction. The obstruction
is that JLS has **no observation channel separate from the value domain**: the only
thing a trace can draw is the output value of a watched element. A transmission
line's far-end voltage is not the output value of anything; it is a derived
quantity the element computes. Widening the *value domain* to admit reals is a
much larger and more dangerous claim than widening the *set of things that can be
observed*, and it is not what #490 needs.

## Why the value-domain framing pulls against the project

`ARCHITECTURE.md:341-368` records the discrete-event interpreter as JLS's **sole**
simulation strategy, and binds any future strategy to being "observably identical
to the event model as specified in `docs/simulation-semantics.md` — the
two-states-plus-HiZ value domain and multi-driver/tri-state resolution (§2, §9)"
with #202's RV32I run as a differential oracle. A REPLAN that widens §2 to include
real numbers quietly weakens that equivalence criterion for every future reader,
and it reopens `VcdExportGoldenTest.vcdIsStructurallyWellFormedAndTwoStatePlusHiZ`,
which exists precisely to pin the domain shut. The existing HiZ encoding is
already a compromise — `src/jls/sim/TraceSample.java:19` carries HiZ as a marker
bit above the element's width, and `src/jls/edit/Trace.java` keeps an `off`
sentinel BitSet for the same purpose. Adding a third inhabitant to that union is
how a two-state simulator acquires a mixed-signal type system by accident.

**Reframing:** #620 adds an *observation* channel, not a value. §2 stays true,
word for word. Say so in the issue, because the current title is the thing a
future maintainer will cite when they widen §2 for real.

## The seam is already cut — and it is not `Change`

The strongest argument for the reframing is mechanical. `src/jls/edit/Trace.java`
is not final, its geometry fields are `protected`, and
`src/jls/edit/InteractiveSimulator.java:1399` already contains
`private class Header extends Trace` — a row that lives in the same window, on the
same time axis, sharing the same cursor, and painting something that is not a
signal at all. **The row model is already polymorphic by subclassing.** A
real-valued row is `RealTrace extends Trace` overriding `paintComponent`, fed by
its own sample list, touching neither `Change`, nor `BitSetUtils`, nor
`TraceSample`, nor any existing golden. Acceptance criterion 3 ("every existing
BitSet-valued row renders exactly as before") stops being a thing to test and
becomes a thing that is true by construction.

That also fixes the shared-scope economics, which as written I predict will fail.
`Trace.MAX_RETAINED_CHANGES` is 100,000 *changes* (`Trace.java:31`), and the whole
list is a value-**change** store — deduplicated, drawn as segments between
transitions. #490's closed-form waveform has no transitions; it is a function
sampled at some rate nobody has declared. #303 is worse: 44.1 kHz audio against a
100,000-entry cap is **2.3 seconds** of signal. If #620 lands `double` inside
`Change` and calls it done, #303 does not "re-scope, not re-estimate" per criterion
4 — it arrives and finds a rewrite. The series the row consumes must be
sample-carrying with a declared interval from the first commit, or the shared-cost
argument that justifies this task is false.

## Disregarding acceptance criterion 2: do not mint a CSV format

I am explicitly setting aside AC-2 as written. JLS already has a headless waveform
surface: `BatchSimulator.toVcd()` (`src/jls/sim/BatchSimulator.java:383-475`),
profiled normatively in `docs/batch-interface.md` §4, golden-tested byte-for-byte,
shipped in the container image, and documented for GTKWave/Surfer in
`docs/vcd-interop.md`. IEEE 1364-2001 §18 — the standard `toVcd` already names in
its own Javadoc — defines `$var real 64 <id> <name> $end` and `r<value> <id>` value
changes, and both GTKWave and Surfer render real vars as analog traces with zoom
and cursors that `Trace.java`'s 40-pixel three-level renderer will never match.

So the elegant route is: **emit the far-end waveform as a VCD real var.** It is a
strictly additive change to a format JLS already owns and already promises
stability for; it costs one `$var` line and one value encoder branch; every
existing golden stays byte-identical because no existing signal becomes real; and
it lands the headless half of #490's criterion 3 and #303/#305's shared dependency
in one move. A bespoke CSV, by contrast, mints a **third** headless waveform
surface (stdout watched-element format, VCD, CSV), each with its own stability
promise under `docs/batch-interface.md` §6, and CSV is the one of the three that no
waveform viewer opens.

AC-2's "byte-identical to the rendered data" is also not a testable predicate —
rendered data is pixels. The contract worth writing is a **data** equality: one
function produces the series, the exporter and the painter both consume it, and the
test asserts they received the same `double[]`. That is stronger than byte
equality, cheaper, and it survives every future change to the renderer. Note also
that a byte-stable float format is a real decision (round-trip `Double.toString`
vs. fixed `%.*e`, and locale independence) that AC-2 hides; #490's own 1e-12
cross-check needs full round-trip precision, so pick round-trip and say so.

## Where the pedagogy actually lives, and AC-1 is silent about it

AC-1 promises "the same time axis, cursor and zoom behaviour every other row has."
Every one of those is the **horizontal** axis. `Trace.paintComponent` has no
vertical concept whatsoever: `HEIGHT` is 40, values are drawn at `top`, `middle`,
`bottom`, and the cursor readout is `BitSetUtils.ToString(value, base)`. #490's
entire lesson is a vertical statement — 5.500 V against a 3.300 V rail, 166.7% —
and it is unreadable without a y-scale, a unit label, and a rail reference line.
The row also needs to be taller than 40 px to be worth looking at. **AC-1 as
written can be satisfied completely while the feature it exists to serve remains
invisible.** Add the vertical half: declared units, autoranged or declared y-range,
and a horizontal rail marker (which is also where #490's overshoot diagnostic
becomes a picture rather than a sentence).

## Two smaller notes

- The row cannot be built as "a change to the trace window's row model" alone
  (AC-5). The samples must reach the row from somewhere, and today only a watched
  `Element` or a probed net can feed one. A minimal third source kind — an element
  publishing a named analytic series — is unavoidable, and it is the same seam
  `toVcd` already uses to unify `eventTrace` with `probeTrace` (#200). Better to
  name it now than to discover it as scope creep.
- `ARCHITECTURE.md:57-60` places `InteractiveSimulator` in `jls.sim`; the file is
  `src/jls/edit/InteractiveSimulator.java`. Whoever executes this touches that
  boundary and should fix the sentence in passing.

## Recommended shape

1. `RealSample(long time, double value)` and a sampled series in `jls.sim` —
   headless, no AWT, `HeadlessCoreRatchetTest` stays green.
2. VCD real-var emission in `toVcd`, `docs/batch-interface.md` §4 extended, one
   new golden; existing goldens untouched.
3. `RealTrace extends Trace` with a vertical axis, units and a rail marker.
4. Record on #303 and #305 the **series contract** (sample-carrying, declared
   interval), not merely that the row was paid for.

Steps 1-2 alone discharge #490's K18-4 fallback ("stop at the headless CSV form"),
at roughly a third of the stated scope and with zero permanent surface added beyond
one additive line in a format JLS already maintains. That is the version worth
funding first.
