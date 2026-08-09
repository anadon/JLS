# Issue #696: TASK-C534-1: a triggering logic analyzer is a drawable element — edge, pattern and duration arming with pre-trigger history, serialized with the circuit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The instrument is not the point. CAP-23 (#504) states the point in one
sentence — "diagnosis starts from a caught event instead of from scrolling" —
and #696 is the first task that has to make *caught* mean something. Strip the
Multisim vocabulary and the claim is: **a JLS run should carry a machine-found
point of interest, and everything downstream (chronogram, VCD, batch report)
should be able to address that point.** That claim is right, it is unserved
today, and it belongs on the canvas rather than in a panel — the element form
also keeps the trigger predicate inside the ordinary `react()` event model
instead of taxing the kernel loop, which is exactly what #221's recorded
"interpreter is the sole strategy, hot plane stays plugin-free" decision and
CAP-23's AC-5 ratchet want.

What I disagree with is the shape. As written, #696 builds a self-contained
appliance — channels, capture buffer, pre-trigger depth, three trigger kinds,
its own headless read-out — and every one of those parts already exists
elsewhere in JLS in a partly-built form. The issue would produce a second
recorder, a second sample retention policy, and a second way to get samples out
of a run.

## Three of the four mechanisms already exist

- **Channels and headless read-out.** `Watchable`
  (`src/jls/elem/Watchable.java`) plus wire *probes* (#200) already name signals
  at any hierarchy depth; `BatchSimulator.probeSample` /
  `Simulator.probeSample` (`src/jls/sim/Simulator.java:285`) fold a named net
  into the trace without splicing a tap, and `docs/batch-interface.md` §4.1
  makes probes a documented VCD signal class. A logic analyzer with N physical
  input pins is a *regression* against #200: it forces the student to redraw the
  schematic around the instrument to observe what a probe names for free.
- **Pre-trigger history.** The interactive engine already retains 100 000
  changes per signal — `Trace.MAX_RETAINED_CHANGES`
  (`src/jls/edit/Trace.java:32`), added by #121. Batch retains everything in
  `BatchSimulator.eventTrace`/`probeTrace`. Pre-trigger samples are not missing;
  the *marker saying which sample was interesting* is missing.
- **A drawable element that fires on a condition.** `Stop` and `Pause`
  (`src/jls/elem/Stop.java:147`, `src/jls/elem/Pause.java`) are already
  four-input canvas elements that act when an input goes non-zero, and
  `docs/simulation-semantics.md` §11 already has a home for their semantics. A
  trigger is these elements with a different verb.

The genuinely new primitive in #696 is exactly one thing: **a
duration/pulse-width qualifier** (the min/max leg), because nothing in JLS can
say "this was high for less than N units". Edge is a two-sample comparison;
pattern is an AND gate.

## The seam that is actually missing, and that #696 will hit at AC-4

There is no shared recording. Batch has `getTraceSamples()` in
`jls.sim.BatchSimulator`, headless by construction (#77). Interactive has
`private record Change(BitSet value, long when)` living inside a `JPanel`
(`src/jls/edit/Trace.java:51`), reachable only with a display. #696 AC-4 says
the analyzer "works with no display present" and AC-3 says a *capture* honours
a pre-trigger depth. Implemented literally, the element grows its own ring
buffer so it can be display-independent — a third sample store, with a third
retention policy, whose contents then have to be reconciled with the two that
already exist for #700's VCD leg and #534 AC-3's byte-for-byte
interactive-equals-batch claim (the same reconciliation #716 hits from the
WaveJSON end).

**The higher-leverage cut is to lift the recorder, not to grow a buffer.** One
`jls.sim` recording type — the value-change history of a run, bounded, with
markers — fed identically by `BatchSimulator.afterEvent` and by the interactive
engine, with `Trace` demoted to a view over it. That single move serves #696,
#698's byte-identity criterion, #700, PF-1's chronogram, #716, and CAP-24's
"one recorded run" rule. It is more work than #696 as written, and it is the
work all of those issues are individually going to pay for anyway.

## Reframing 1 (primary): the trigger is a bookmark, not a recorder

Make the element's whole output a **timestamped marker on the existing
recording**, plus (optionally) the `Pause` behaviour so the run stops where the
student can look at it. No channels, no capture window, no pre-trigger depth
parameter, no capture serialization question. "Pre-trigger history" becomes what
it already is — scrollback — and "the capture window" becomes what #700 already
owns: the chronogram centres on the marker.

This buys three things almost free:

1. **AC-4 headless with no new surface.** Make the trigger element `Watchable`
   with a 1-bit fired pulse. Then a headless run reads the capture through the
   *existing* `-vcd`/`-r` path: the trigger becomes an ordinary signal in the
   dump, at the exact tick it fired, with no extension to the frozen VCD profile
   (`docs/batch-interface.md` §6). The alternative — an instrument read-out —
   either widens that contract or invents a fourth output stream.
2. **AC-5 structurally, not by measurement.** Trace accumulation is already
   gated (`if (!JLSInfo.printTrace && vcdFileName == null) return;`). An unarmed
   trigger that allocates nothing and writes nothing cannot cost more than a
   `Stop` element does today; inertness stops being a benchmark you must defend
   and becomes a property of the design.
3. **Arming needs no new batch flag.** Armed-ness is a saved element property,
   so `Circuit.load` arms it and `docs/batch-interface.md` stays frozen. Say
   this explicitly in the task — it is the difference between a minor addition
   and reopening a stability contract.

## Reframing 2: primitives over an appliance — the pattern trigger is an AND gate

JLS's entire thesis is *draw the logic*. Re-implementing combinational matching
inside a modal dialog ("pattern across the inputs", presumably with don't-cares)
teaches nothing and duplicates the gates the course is about. Ship instead:

- `Trigger` — the sink: fires on non-zero input, marks the recording, optionally
  pauses. Directly generalizes `Stop`/`Pause`; same four-input geometry.
- `EdgeDetect` — rising/falling/either, one bit in, one-tick pulse out.
- `PulseWidth` — the one truly new primitive: asserts when its input has held a
  value for less than / more than N time units. This is the runt-pulse detector,
  and it is reusable far beyond this instrument.

Pattern arming is then an AND tree the student draws — visible, editable,
inspectable by the PF-3 cause chain, and *itself* a lesson. A convenience
"Analyzer" that expands to these primitives (or a dialog that is sugar over the
same predicate) can follow if the Multisim-refugee audience actually asks; it
should never be the only form.

Note the pedagogical asymmetry this exposes: with the appliance, the hazard
lesson ends at "the tool caught it". With primitives, the student *builds a runt
detector out of gates and a width qualifier*, which is closer to what CAP-23's
instructor audience is actually trying to teach.

## Reframing 3: channels are probes, not pins

If a channel set is wanted at all, it should be a selection over already-named
signals (probes and watched elements), not physical wires into a box. #200
exists precisely because splicing taps to observe internal nets is the wrong
move; an eight-pin analyzer reintroduces it eight times per instrument, and
drags schematic layout into a diagnosis decision.

## Criteria I would explicitly disregard

- **AC-1's "wirable"** — keep *placeable, configurable, round-trips*; drop the
  requirement that observation channels be wired. Trigger inputs stay physical
  (1–4, like `Stop`); observation stays probe-based.
- **AC-3's "capture whose window includes the configured number of pre-trigger
  samples"** — replace with: the marker lands at the known firing tick, and the
  pre-trigger view is the recording's existing scrollback. Depth becomes a view
  parameter under #700/PF-1, not element state. This also removes the unasked
  question of whether captures serialize into `.jls` (they must not — that is
  run state in a document format, and it would break AC-1's byte-identity the
  first time someone saves after a run).
- **AC-3's fixture premise** — "the shipped hazard demo's runt pulse" is
  load-bearing in #504 §1, #534 AC-2 and here, and **it does not exist**: the
  repository ships no hazard circuit (`examples/` holds only `autograde.py`;
  the only `.jls` files are RISC-V and test fixtures), and no filed issue owns
  authoring it. Three tasks currently assert against a demo nobody is building.
  File it — one small static-1-hazard circuit plus its expected firing tick is
  the cheapest, highest-value artifact in this whole capstone, and it is also
  the teaching object the capstone is ultimately for.

## Alignment notes for whoever implements this

- **Home the semantics in `docs/simulation-semantics.md` §11**, next to
  `Stop`/`Pause`. In particular, define *pulse width* and *runt* once, there.
  CAP-23 risk 1 warns against two cause-chain models; the same hazard applies to
  timing vocabulary — P4's static glitch detector must inherit this definition,
  not mint a second one.
- **Registration is cheaper than ARCHITECTURE.md claims.** The "sixteen places"
  list is stale: `ElementRegistry`/`ElementType` (#78) landed, and
  renderers/dialogs are split out (`src/jls/edit/*Renderer.java`,
  `*Dialog.java`, `Palette`). Budget the help topic, `AllElementsRoundTripTest`
  fixture and palette entry; the reflective-loading and per-element-switch pain
  is gone.
- **Determinism.** The firing tick must be a pure function of circuit content;
  same-time ordering is already pinned by `SimEvent` seq and stable-id seeding
  (#181). A trigger that reads sibling element state during `react` rather than
  its own inputs would break that — keep it an ordinary event consumer.

## Verdict

**endorse-with-reframing.** The outcome is right and the canvas-element form is
right. Re-cut the work as: (1) lift the run recording into `jls.sim` so one
history serves both engines; (2) `Trigger` + `EdgeDetect` + `PulseWidth`
primitives that mark that recording, with pattern arming drawn as logic; (3)
the fired trigger as an ordinary `Watchable` signal so headless read-out costs
the frozen batch contract nothing; and (4) file the hazard demo that three
issues already assume. The appliance-shaped analyzer, if still wanted, is then
sugar over parts that already earn their keep.
