# FEAT-048 - A2D/D2A bridge elements and A-STEP synchronization

**Status:** proposed | **Cost:** 4-6 mw | **Owner program:** UNOWNED |
**Spine rank:** -

## Capability delivered

A drawn design can cross between the continuous and the discrete world in both
directions, at places the student chose and drew, with the crossing rule visible
as element parameters rather than hidden in an engine. A one-bit analog-to-digital
bridge publishes a digital transition at the tick a solved trajectory crosses a
threshold; a one-bit digital-to-analog bridge ramps a solved node when a digital
value changes. Underneath them, the two engines agree on time: the discrete event
loop owns the clock and the continuous region is a self-scheduling participant in
it, so the crossing time is exact rather than bounded, and no rollback machinery
is needed. And the model gains the mechanism that keeps the two worlds apart when
they are not deliberately bridged - a port and net alphabet that says what kind of
signal a terminal carries, checked when a wire is drawn, when a file is loaded and
when a design is elaborated.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-12 | required | the comparator output must become a digital edge a drawn beat counter can count |
| CAP-14 | required | mixed-signal parity is a claim about the crossing rule; a bridge with an unstated policy cannot be compared to anything |
| CAP-10 | required | the drawn ladder is fed by digital values crossing into the solver, and the synchronization guard is what makes it run at usable speed |
| CAP-11 | required | samples must cross from the analog front end into digital logic |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| FEAT-046 | A bridge publishes a crossing found in a solved trajectory. There is no trajectory to bisect and no node to ramp until the solver exists |
| FEAT-049 | A bridge has one analog terminal, and an analog terminal is a port on an element the type hierarchy does not admit today. The permit and the port widening land with the drawn analog elements |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0102 | Bridge elements and the synchronization protocol | The two bridges with their declared thresholds and ramps, the lock-step contract with the event loop, and the step-cap regimes |

## Acceptance criteria

1. Both bridges are **one-bit level converters**. An n-bit converter is drawn -
   a ladder, or a comparator with a successive-approximation register - and
   neither bridge takes a sample rate as a parameter. Sampling is drawn, by
   clocking a register.
2. The analog-to-digital bridge takes a low threshold, a high threshold and a
   publication delay. Low below high gives a dead band with hysteresis; low
   above high is refused as a typo rather than reinterpreted.
3. The published transition lands on the **earliest tick at which the solved
   trajectory crosses**, located by bisecting the accepted step, bounded by a
   logarithmic number of extra solves and terminated by the integer tick
   lattice. A test asserts the published stream is unchanged when the timestep
   controller's parameters change.
4. The publication delay is at least one tick and this is enforced, not
   defaulted - a crossing published at the current instant could be consumed by
   logic that changes a boundary input at the same instant, and the event loop
   has no bound on same-instant passes.
5. The digital-to-analog bridge **ramps** rather than steps, over a declared
   rise or fall time, and registers the ramp's end as a breakpoint. Stepping into
   a continuous solver is a discontinuity the integrator must reject and retake.
6. Analog and digital nets cannot be connected by accident. The domain check
   sits **above** the width check, is unconditional, fires at every place a
   connection is made, names the sanctioned bridge in its message, is re-checked
   on load as a validation that never widens, and asserts at elaboration.
7. A region containing no digital-to-analog bridge is never interrupted by a
   digital event, and this is asserted as a property of the region's ports rather
   than measured.
8. The behavior change is documented before it is discovered: a design
   containing a continuous region always has a pending event, so a run ends by
   reaching its time limit rather than by running out of activity, and the run's
   time limit becomes the analysis stop time.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the bridges, the synchronization contract and the port alphabet | **no issue** |

## Design notes

**Invert the time ownership and an entire mechanism disappears.** The
industry-standard mixed-signal coordination loop needs a rollback routine
because it drains the digital queue speculatively into a window the continuous
solve has not yet earned. That is a consequence of an ownership choice, not a law.
If the event loop owns time and the continuous region is a self-scheduling
element holding exactly one pending self-event - the idiom `Clock` already uses
(`src/jls/elem/Clock.java:392,421`) - then every Newton retry, every continuation
step and every rejected timestep happens inside one call at one instant, and only
the accepted result becomes a posted event. Simulation time never runs backwards;
only the solver's private time does. This is the same post-a-computed-future-value
pattern the shipped elements already use.

The engine footprint is one observational method that reports the time of the
earliest pending event. The event loop's own documentation already sanctions a
peek for hooks that need it.

**The step cap must be built in the right order or the feature is unusable.** A
region with no digital-to-analog bridge cannot be invalidated by any digital
event and needs no cap at all - which covers the heart rate monitor and audio
capture outright. A region whose bridges are driven only by clocked elements can
cap the step at the next queued event exactly, because the clock self-schedules
its next transition. The unconditional conservative cap must not ship alone: at
the measured event rate per clock cycle, a 44.1 kHz bridge against a fast clock
is on the order of a million wasted solver visits per genuine boundary event.
Speculative execution across asynchronous bridges is deferred, and nothing in the
capstone set needs it.

**The port alphabet is one mechanism with several populations.** Domain (analog
or digital) is one axis; radix is another (FEAT-028); drive strength is a third
(FEAT-027). All three are answers to "what may this terminal carry", all three
are checked at the same four connection sites, and all three have the same
validate-don't-widen rule on load. Whichever increment ships first pays for the
descriptor and the check sites - roughly two and a half weeks - and the others
pay a small fraction. Built separately, JLS acquires three refusal vocabularies
that disagree at the edges.

**The trap that makes the check order load-bearing.** A port declaring zero bits
means "arbitrary width" (`src/jls/elem/Put.java:34`), and the width check is
guarded on both widths being positive
(`src/jls/edit/SimpleEditor.java:4015`, and identically at `:4142, :4247,
:4358`). An analog terminal declaring zero bits therefore passes the width check
unconditionally, and the net's width recomputation would then silently take the
digital width (`src/jls/elem/WireNet.java:280`). Giving analog terminals a width
of one is worse: it fires the wrong message and lets a one-bit digital wire
connect silently, which is the exact case the bridge exists to mediate. The
domain check must be above the width check and unconditional.

Two small honesty fixes ship with the mechanism: the wire and wire-end
information text appends a no-input note whenever a net has no driver, and calls
a zero-width net not connected. Both are correct for digital nets and alarming
nonsense on every analog net.

## Risks

- **A published crossing that depends on the integrator is a golden that moves.**
  Bisecting to a tick rather than reporting the accepted timepoint is what makes
  the digital stream independent of timestep policy. If the bisection is dropped
  as an optimization, every mixed-signal golden becomes coupled to the solver's
  step control.
- **The domain check landing in a release without the bridges** would make legal
  designs undrawable and teach users that the tool is broken. They ship together.
- **Same-instant loops.** Without the publication-delay floor, a bridge feeding
  logic that feeds a bridge can produce an unbounded same-instant cycle the
  event loop cannot break. The floor is the analog counterpart of the causal
  delay floor the fidelity boundary already requires.
- **A second clock.** Any pressure to add a sample-rate parameter to a bridge
  should be refused by name: it creates a second owner of time, which is how
  mixed-signal simulators acquire their hardest bugs.

## Evidence

- The bridge determination, both parameter lists, the crossing policy, the
  publication-delay floor and the ramping requirement:
  `11-analog-determination.md:379-436` (§2.5).
- The synchronization inversion, the deleted rollback requirement, the
  observational peek, the three step-cap regimes and the arithmetic that kills
  the naive cap: `11-analog-determination.md:437-533` (§2.6).
- The port-alphabet decision and its shared cost:
  `11-analog-determination.md:326-333` (D-A5); the domain-separation design, the
  four enforcement layers and the zero-bits trap at `:262-325` (§2.3).
- Verified at HEAD `addc6c5`: `src/jls/elem/Put.java:34` (zero bits implies
  arbitrary); `src/jls/edit/SimpleEditor.java:4015,4142,4247,4358` (the four
  guarded width checks); `src/jls/elem/WireNet.java:272-283` (recheck widens by
  taking the maximum at `:280` and sets the has-driver flag at `:283`);
  `src/jls/elem/Clock.java:392,421` (the self-scheduling idiom);
  `src/jls/sim/Simulator.java:217` (the event-loop guard that never empties once
  a region is present); `src/jls/sim/BatchSimulator.java:568-570` (the two stop
  reasons this changes).
