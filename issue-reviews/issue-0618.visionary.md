# Issue #618: TASK-C490-1: a drawn line between two nets carries an impedance and a delay, and its far end reflects — the closed-form superposition, with its truncation term count reported
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

One sentence: **a student should be unable to keep believing a wire is a wire.** The
payload is a number they cannot argue with — 5.500 V arriving on a 3.3 V rail — and the
reframe is that the regime is entered by *edge rate*, not clock rate (166.7% → 132.3%
with the clock untouched). Everything else in #618 and its parent #490 is delivery
machinery for that one observation.

The goal is right, and the *method* is unusually well chosen. #490's central insight —
that a lossless line with resistive terminations driven by a piecewise-linear source has
an exact closed form, so this content can enter JLS with **no MNA, no Newton, no timestep
control** — is the only way this material could ever be aligned with the project. The
recorded determinations are unambiguous: `docs/capability-roadmap/README.md` §6(a) puts
continuous-time and analog out on ground (a), *"Supporting these means being a SPICE-class
solver — a different tool"*; `docs/capability-roadmap/sweep-06-physical-boundary.md` says
of Touchstone/IBIS-analog *"No continuous-time solver, and none should be added"*; and
`ARCHITECTURE.md`'s #221 decision makes the event-queue interpreter JLS's **only**
simulation execution strategy. A geometric series threads all three needles. Keep it.

What I am rejecting is not the physics or the kernel. It is the **artifact**: that this
lands as a registered, saved, palette-bearing *element type*.

## 1. The element cannot carry the lesson, by construction

Follow the value through. The element sits between two ordinary nets; a net's simulated
value is `src/jls/elem/WireNet.java:405`, `private @Nullable BitSet value = new BitSet(1);`
— two-state plus HiZ (`docs/simulation-semantics.md` §2). Invariant 3 of #490 and AC4 here
forbid touching `WireNet.propagate`. Therefore **the element's downstream output is a
BitSet**. It cannot drive 5.5 V into anything. Its entire digital behaviour is "delay a
transition by `Td`" — which is `src/jls/elem/DelayGate.java:15`, already in the tree,
already registered: *"Logically neutral, simply delays a signal change by a given amount."*

So the 5.500 V exists only in a **side channel** — the real-valued trace row, which is a
*different, unfiled* scope of #490, shared with #303 and #305. Strip that away and #618
delivers a parameterised `DelayGate` whose extra parameters change nothing observable. The
element is not an element. It is an **instrument wearing a schematic body**.

That single fact explains every awkwardness in the parent: why a "context-derived
visibility rule" has to be invented so the thing stays out of the palette; why K18-4 exists
as a stop condition; why the honest fallback in K18-4 is *"stop at the headless CSV form"*
— i.e. the fallback is the analysis without the element, and it still teaches the lesson.
When the degraded mode is the whole product, the seam is in the wrong place.

## 2. The cut inverts the parent's own permanence discipline

#490's proudest structural argument is permanence ordering: reversible work first, the
irreversible surface last, *"K18-1 gates the permanent surface … the golden corpus and the
analytic cross-check must be green before the dialog, the renderer and the palette entry."*

#618 is the *first* child, and its AC1 demands the element "registers, places, saves, loads
and simulates". Registration is the frozen save tag. Saving is the format commitment.
Placing is the palette obligation under `test/jls/edit/PaletteContractTest.java`'s
`paletteIsTotalOverTheElementRegistry` (verified today: 35 registry types against 32 palette
entries, `NON_PALETTE_TAGS = {SubCircuit, WireEnd, TestGen}`). The four-termination golden
and the 1e-12 cross-check are a *sibling* scope, not a gate inside this task. So the task
that lands 100% of the irreversible surface delivers 0% of the observable lesson, and does
so before its own stop condition can fire. AC3's "the term count is **reported**, not
hidden" has no surface named anywhere in #618 to report *to*.

## 3. The reframing: it is not an element, it is a declared property of the net you
already drew

**I am explicitly setting aside AC1 and AC5.** AC1 requires the element to register, place
and save; AC5 requires counting the registration tax against 66-lines-across-12-files.
Under the alternative below, the registration tax is **zero** and there is nothing to count
— which also means the element-versus-net-kind decision that number was made on no longer
needs defending.

#486 (FEAT-058) is already building the exact seam this needs: a net declares a **length**,
a driver declares an **edge rate**, and `jls -check` reports a verdict computed from them
with "not assessable" as the honest default. Extend that vocabulary by two more declared
electrical facts — `Z0` and the terminating resistances — and make the reflection lab an
**analysis over declared attributes**, emitting the closed-form far-end waveform (and `N`,
the truncation term count) through the report/CSV channel for any net that opted in.

What this buys, all of it structural rather than stylistic:

- **No new element type, no frozen tag, no palette entry, no help topic, no K9 debt, no
  registration tax.** #490's Invariant 4 ("the palette count does not rise") holds by
  construction instead of by a bespoke visibility mechanism that must be designed, built
  and tested.
- **K18-4 disappears.** There is no palette entry a first-year could meet while drawing an
  adder, so the stop condition has nothing to trip on.
- **The element-versus-net-kind argument becomes moot.** #490 spends its longest paragraph
  refuting "make `WireNet` a distributed net kind" on the grounds that a net holds one
  value. Correct — and irrelevant here. Declaring `Z0` on a net is an *annotation consumed
  offline*, exactly like #486's declared length, and it changes `WireNet.propagate` by
  precisely as much as that one does: not at all.
- **CAP-18 becomes one story instead of three.** #486 declares the electrical facts, #487
  exports them as constraints, and the reflection lab *computes over the same facts*. Today
  #490 consumes #486's two attributes and then invents a parallel parameter home on a new
  element; #486's own criterion 3 ("neither downstream rung adds a third attribute") gets
  much easier to discharge honestly.

**And it is better pedagogy, which is the argument that should decide it.** CAP-18's thesis
is that *the wire you already drew* stopped being a wire. Shipping a placeable
transmission-line component teaches the opposite: that a transmission line is a special
part you insert, and therefore that ordinary nets — the ones the student did not decorate —
are safe. That is the precise misconception #486 exists to break, re-installed by #490's
delivery vehicle. An annotation on an ordinary net says the true thing: nothing was
inserted; this is what your wire was doing all along.

## 4. Two cheaper fallbacks, in descending ambition

- **B — ship it as a fixture and a document.** The four terminations plus the edge-rate
  collapse (50 ps / 1 ns / 5 ns → 166.7% / 132.3% / <105%) as a committed corpus under
  `examples/` with a `docs/` page and a small headless computation. Zero persisted surface
  of any kind. If the kernel is genuinely eight lines, the delivery vehicle should be
  measured in the same units, and #490's own permanence logic says the zero-surface variant
  is the most valuable thing in the capstone.
- **C — if it must be drawable, make it a probe, not a series component.** A
  `DisplayElement`-family SI probe *attached to* an existing net, reporting that net's
  far-end waveform, rather than a two-port inserted *into* the signal path. The
  digital-passthrough embarrassment of §1 evaporates (there is no passthrough), and the
  "your existing wire is the line" framing survives.

## 5. The decision actually hiding in here, and what I would fund first

The real architectural question in this family is not "does JLS get a transmission-line
element". It is **"does JLS's observable output become real-valued?"** — `TraceSample` is
`record TraceSample(long time, BitSet value)` (`src/jls/sim/TraceSample.java`); there are no
real numbers anywhere in the trace, VCD or CSV surfaces. That is a genuine, permanent,
identity-level widening of the project, it is shared three ways (#490, #303, #305), it is
reversible in the sense that it commits no save tag, and it is the *only* piece without
which none of this is visible.

So the child worth funding first is the one #490 correctly notes is independent from day
one: **the real-valued trace row**. #618 as filed inverts that — it orders itself after
#367 and #487 and says nothing about the surface its own AC3 needs. If exactly one thing
in CAP-18's third rung gets built, it should be the row, not the element.

## Verdict

**rethink.** The goal is one of the best in the CAP-18 family and the closed-form kernel is
the right physics delivered the right way — no solver, no second numerical strategy, no
collision with #221 or roadmap §6(a). But the artifact this task commits is a registered,
saved, palette-bearing element whose simulated behaviour is indistinguishable from the
`DelayGate` already in the tree, whose pedagogical payload lives entirely in an unfiled
sibling scope, and whose framing quietly re-teaches the misconception the capstone was
built to destroy. Re-decide the artifact before filing implementation: declare the
electrical facts on the net (#486's seam), compute the lattice as an analysis, report `N`
and the waveform through the report channel, and let the permanent surface be nothing at
all.
