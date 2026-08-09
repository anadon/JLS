# Issue #741: TASK-C544-3: signal-state changes are announced as they happen during simulation — or the reduced set is recorded as a named exception, never papered over
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Stripped of mechanism, the claim is: **a blind student should know what the
circuit is doing while it runs, not afterwards.** That is a good claim and it
belongs in JLS's arc — the project already treats "a signal's value over time"
as a first-class, normatively specified object (`docs/simulation-semantics.md`,
`docs/batch-interface.md`, the VCD profile, `jls.sim.TraceSample`).

But the issue converts that claim into a very specific bet: push
`AccessibleContext` property-change events from a background simulation thread,
through java-atk-wrapper, into Orca, at a rate throttled by a coalescing rule,
over an attention set defined as "the focused element and its connections."
Every clause of that bet is contestable, and the project has already written
down why.

## The project's own playbook argues against this route

`docs/standards-adoption/03-accessibility-conformance.md` is the repository's
considered accessibility strategy, and it lands almost exactly opposite to
FEAT-C26-3:

- §6 costs the full JAAPI canvas scene model at **8–15 maintainer-days plus
  permanent per-element maintenance**, and closes with *"Recommendation: do not
  build it as part of this project"* — naming *"a genuine risk that
  java-atk-wrapper surfaces none of it to Orca."* #741 is the deepest, most
  bridge-dependent slice of exactly that model.
- §6 instead recommends **an accessible, keyboard-reachable representation of
  the same information in stock Swing** (~3–4 days), justified under Revised 508
  **E101.2 Equivalent Facilitation**. Stock components are accessible on all
  three AT stacks *without* bridge-specific work.
- The testing section explicitly recommends **not** adding an AT-SPI/accerciser
  lane to CI, citing the `gui-wayland` lane's own history (twenty runs to earn
  promotion, an `UNVERIFIED-PLACEHOLDER` checksum, `PIXEL_DIFF_MIN` parked at
  `0`) as the honest cost estimate for a fragile GUI lane. #743 puts
  `OrcaLabSessionTest` in that lane anyway, and #741's evidence depends on it.
- The closing paragraph: *"If you only have budget for one, build the outline
  view and fix the contrast — that helps an actual blind student."*

None of the C544 tasks cite this document. A band that contradicts the
repository's own recorded strategy should either supersede it explicitly or be
re-derived from it. Right now it does neither.

## The sequencing inversion (this is the strongest point)

§1 of the same playbook: `scripts/build-installer.sh` derives its jlink module
set from `jdeps --print-module-deps`, which can never report
`jdk.accessibility`. **Every shipped `.msi` therefore bundles a runtime with no
Java Access Bridge — NVDA and JAWS get nothing at all from an installed JLS.**

That is a one-line fix, sized at one maintainer-day. #741 is 1.5–2 mw of work
whose beneficiaries are, today, only users running the jar on a system JDK with
`libatk-wrapper-java-jni` present. TASK-C544-5 (#745) proposes to ship an NVDA
checklist for a distribution where NVDA is structurally deaf. Doing live speech
before the bridge fix is spending the band's most expensive slice on the
narrowest audience while the widest one gets nothing.

**Re-order: the Access Bridge fix precedes the whole band, and TASK-C544-1's
spike should run on a build that actually has a bridge.**

## The reframing: announce the trace, on the student's clock

I am disregarding AC-1's attention model, AC-2's channel constraint, and AC-3's
fallback framing, because a different seam reaches the same outcome with far
less risk. Two moves:

**1. The attention set already exists, and it is better than "focus plus
connections."** JLS has `Watchable.isWatched()` (`src/jls/elem/Watchable.java`)
and `Wire.hasProbe()`; `InteractiveSimulator.findTraces`
(`src/jls/edit/InteractiveSimulator.java:973`) collects them, and
`afterEvent` (`:879`) feeds every change into a `Trace` row. `EditOp.WATCH` and
`EditOp.PROBE` (`src/jls/edit/EditOp.java:34-37`) make that set
**keyboard-curatable today**, already pinned by
`KeyboardEditingFaithfulTest.watchKeyTogglesWatchedThroughFocusOwner`. The set
is durable, nameable, saved in the `.jls` file, and identical to what a sighted
student sees in the trace window — where #741's implicit focus-derived set is
novel, invisible, unsaveable, and diverges from the sighted workflow.

**2. The trace window is the right accessible surface, not the canvas.**
`src/jls/edit/Trace.java:20` is `class Trace extends JPanel implements
MouseListener, MouseMotionListener` — 626 lines, no key bindings, no
`setFocusable`, no accessible wiring. The playbook calls it *"the second-largest
gap after the canvas and, unlike the canvas, cheaply fixable... the trace is a
list of rows with sampled values."* A live simulation is, structurally, a
**table**: signals down, time across. Make it a real accessible table with
row/time keyboard navigation and per-row accessible value, and live announcement
stops being a bespoke channel bolted to a custom-painted canvas and becomes the
ordinary behavior of a stock widget on every AT stack. That is one deliverable
that simultaneously closes a Level-A **2.1.1 Keyboard** failure the ACR cannot
otherwise rate "Supports."

**3. Announce on the student's clock, not the circuit's clock.** AC-1 concedes
the core problem — event-rate speech is not accessibility — and answers it with
a coalescing rule. Coalescing an unbounded asynchronous stream into speech is a
tuning problem with no deterministic oracle, which is exactly what this
project's evidentiary culture (goldens, ratchets, red-on-break) cannot pin.
Invert it: JLS already has **Step** and **Animate** (`stepEnd`, `:37`,
`:324-420`). One keypress → one utterance → *the diff of the attention set since
the last step*. This is how screen readers work everywhere else: speech follows
user action. Consequences:

- The coalescing rule becomes a *diff*, stated in one sentence and asserted
  deterministically headlessly, with no AT attached — satisfying the spirit of
  TASK-C544-2's AC-3 for the dynamic half too.
- AC-4 ("costs nothing measurable in the simulation loop") is true *by
  construction*: nothing is added to `runEventLoop`; the diff is read at step
  boundaries from state `afterEvent` already maintains.
- Free-running mode gets a bounded periodic summary using the pattern already in
  the tree — `beforeReact`'s `lastClockUpdate` 50 ms rate limit (`:844-871`,
  issue #49 H8) — rather than a new invention.

## Consequence for the test seam

AC-2's "no second announcement channel" conflates *what should be said* with
*whether it arrives*. Split them. Announcement **content** derives from the
trace/sample model — `Simulator.probeSample` (`src/jls/sim/Simulator.java:285`)
already exists as a headless, per-change hook feeding the VCD exporter, so the
content is golden-testable in `mvn verify` with no display and no screen reader.
The Orca session (#743) then only has to prove **delivery** — one thin assertion
in the fragile lane instead of the whole capability resting there. That is a
strictly better risk split than the current design, where a flaky compositor
lane is the sole evidence for a category claim.

## If the reframing is rejected

Two things must still change in the issue as written:

- AC-2 requires emitting "through the same accessible model TASK-C544-2
  traverses," but no such model exists — grep finds `getAccessibleContext` only
  in dialogs, `KeyPad`, and `SimpleEditor:2412` (palette buttons). The canvas
  child model is #380, unbuilt and, per the playbook, not recommended. #741
  should declare that dependency as a hard blocker, not an assumption.
- AC-4's phrasing ("costs nothing measurable") is unfalsifiable as stated.
  Make it structural: *no new work inside `Simulator.runEventLoop` or its
  hooks*, pinned the way `HeadlessCoreRatchetTest` pins the AWT boundary.

## What I would fund instead, in order

1. `jdk.accessibility` in the installer module set + `accessibility.properties`
   (1 day) — unblocks every Windows AT user; without it the band's Windows
   claims are void.
2. Accessible, keyboard-navigable trace table with step-clocked change
   announcements (2–3 days) — delivers #741's stated *outcome*, closes a
   Level-A keyboard failure, and works on all three AT stacks.
3. The accessible circuit outline view (3–4 days) — the playbook's recommended
   equivalent-facilitation answer, which also supplies TASK-C544-2's traversal
   without a canvas scene model.
4. Only then re-open the question of live canvas announcements, with a spike
   (#737) run against a bridge that actually exists.

This ordering ships a blind student a working simulation experience before it
ships a VPAT exception explaining why they don't have one.
