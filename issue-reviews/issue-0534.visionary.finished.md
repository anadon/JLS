# Issue #534: FEAT-C23-5: a triggering logic analyzer and a programmable word generator are drawable elements — they serialize with the circuit, fire on edge/pattern/duration with pre-trigger capture, and export through the existing VCD path
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the instrument metaphor and #534 wants two things: **diagnosis should start from a
caught event rather than from scrolling**, and **the stimulus that produced a run should be
a first-class, reproducible, savable artifact**. Both are right, and both are on the
project's arc — CAP-23 (#504) is correct that JLS computes causal structure and throws it
away, and correct that `src/jls/edit/Trace.java` displays and nothing more.

The framing I want to challenge is that these two ends are best served by *two new drawable
elements*. Half of that is already built, the other half is being put on the wrong seam, and
the acceptance criterion that looks smallest (AC-3) is quietly the most valuable work in the
issue.

## 1. The word generator largely ships today — it is called SigGen

`src/jls/elem/SigGen.java` is already a drawable canvas element that: serializes as
`ELEMENT SigGen` with declarative `Attribute` persistence (`SigGen.java:110-134`), round-trips
through the ordinary load path, carries a user-authored stimulus program, and drives the
circuit headlessly by posting timed events at `initSim` (`SigSim.java:129`, `:196`). Its
sibling `TestGen` is the same parser fed from a file, and the shared grammar is a documented
stability contract (`docs/batch-interface.md` §2: `name initial { for d v | until t v } end`).

Measured against AC-1 and AC-3, the *word generator's* delta is not "an instrument" — it is
(a) output ports so it drives wires instead of resolving `InputPin`s by name, and (b) a table
editor over the spec instead of a free-text pane. Both are genuine improvements: name-binding
fails soft today (`specError("no input pin for signal ...")`) and breaks when a pin is renamed.

Minting a *second* stimulus element instead gives JLS three stimulus languages (`-t` files,
SigGen text, word-generator table) and walks straight into a semantic collision the issue
never mentions: `BatchSimulator.addTestGen` (`src/jls/sim/BatchSimulator.java:190-208`)
**deletes every `SigGen` in the top-level circuit** when `-t` is supplied. A new stimulus
element that is not a `SigSim` survives that deletion and double-drives the circuit; one that
is a `SigSim` inherits an already-specified, already-documented answer for free.

**Concrete alternative:** do not file a word generator. Grow `SigGen` — add output ports, add
a tabular editor that is a *view over the existing grammar* (round-tripping table ⇄ text), and
let AC-3's "same program" be literally the same bytes in both paths. That is the difference
between a golden test that must be constructed and a golden test that is nearly a tautology.

## 2. The trigger is a skeuomorph, and it is on the wrong seam

A bench logic analyzer triggers because probes and capture RAM are scarce: you cannot record
everything, so you tell the hardware what to keep. **A simulator has no such scarcity.** JLS
already sees every event; `Simulator.runEventLoop` calls `afterEvent(event)`
(`src/jls/sim/Simulator.java:241`, seam at `:269`) and `probeSample` (`:285`) exists as the
kernel's observation seam — the roadmap notes both are already what the VCD exporter consumes
(`docs/capability-roadmap/README.md:55`). A trigger, in a simulator, is not a peripheral. It is
a **predicate over the event stream**, and it belongs beside `afterEvent` in `jls.sim` where
batch and interactive share it, not in a `react` method on a canvas element.

Putting it on an element has three costs the issue absorbs without pricing:

- **AC-5 becomes a benchmark ratchet for a problem that need not exist.** A wired element with
  a `react` method is on the hot path whenever it is wired, armed or not. A predicate registry
  that is empty is *structurally* free — nothing to measure, nothing to regress. AC-5 turns
  from "prove the tax is small" into "there is no tax", which is strictly the stronger K9 story.
- **AC-1's byte-identical round-trip buys nothing here.** Serializing a diagnostic query with
  the circuit is desirable, but the circuit format already carries non-topological state, and a
  saved trigger is a *session/diagnosis* artifact — it does not change what the circuit *is*.
  Making it an element means every `.jls` a student mails to an instructor carries debugging
  scaffolding as part of the design.
- **It duplicates `Pause`/`Stop`.** `src/jls/elem/Pause.java` and `Stop.java` are already
  "condition on inputs → act on the simulator" elements. A third one with richer conditions is
  the moment to generalize the trigger *concept*, not to add a fourth box.

**Concrete alternative:** a `jls.sim` trigger service — edge / pattern / min-max duration
predicates registered against watched nets, evaluated at the `afterEvent` seam, expressible on
the CLI for batch, and saved as circuit-adjacent metadata. If a canvas affordance is still
wanted (and the "instrument mental model" argument in Open Question 3 has real pedagogical
force), let the element be a *marker* that references a named trigger — a bookmark on the
schematic — rather than the engine.

## 3. The real prize is hiding inside AC-3

AC-3 says the interactively recorded VCD must equal the headless batch golden byte for byte.
**JLS has no interactive VCD writer at all.** VCD lives entirely on `BatchSimulator`
(`setVcdFile`/`writeVcd`/`toVcd`, `BatchSimulator.java:335-476`) over `jls.sim.TraceSample`.
The interactive side has an entirely separate, GUI-resident recorder: `Trace.Change` records
kept newest-first with `MAX_RETAINED_CHANGES = 100_000` (`src/jls/edit/Trace.java:31`), and
`grep` finds no VCD anywhere in `src/jls/edit/InteractiveSimulator.java`.

So AC-3 is not an instrument criterion at all. It is a demand that **JLS grow one recording
path shared by both front ends** — the `TraceSample` recorder moved into `jls.sim`, driven off
`afterEvent`, consumed by the VCD writer, by `BatchTracePrinter`, by the chronogram, and by
#538's WaveJSON exporter. That unification is worth more to the project than both instruments
combined, and every neighbour needs it: PF-1's chronogram needs a recorder it does not own,
PF-3's cause chain needs bounded retention, PF-5 needs pre-trigger history, and #538's
producer/consumer boundary question (the cross-cluster comment on this issue) dissolves the
moment there is one producer.

Note the ring-buffer arithmetic nobody has done: as filed, JLS would end up with **four**
bounded histories — `Trace`'s 100k-change scrollback, PF-5's pre-trigger capture, PF-3's
cause-chain retention (KC-23-2), and the batch `TraceSample` list. One bounded recorder with a
retention window serving all four is the design; four rings is the accident.

## 4. Alignment with the project's trajectory

ARCHITECTURE.md's recorded decisions are consistently *one mechanism per job*: one simulation
strategy (#221), one classloader and type namespace (#222), one typed extension registry
(#223), one element registry (#78 — `src/jls/elem/ElementRegistry.java` now exists, which does
lower the per-element cost that ARCHITECTURE's "sixteen places" list describes, a fair point in
the issue's favour). "One recorder" is the same decision, unwritten. #534 as filed pulls
against that grain in two places (a second stimulus mechanism, a second trigger mechanism) and
silently depends on resolving it in a third (AC-3).

One more out-of-the-box route worth recording: the roadmap already plans **the glitch detector**
(`docs/capability-roadmap/README.md:898`, lane D — "Hazards become visible, named and
countable"). For CAP-23's own headline scenario, "the tool lists the three glitches it found,
click one" is a *better* student experience than "place an analyzer, wire it, guess a trigger
condition, run, hope." CAP-23 risk 1 flags the P4/P9 overlap abstractly; this is its concrete
instance. If the glitch detector lands first, the analyzer's marquee demo is already served,
and PF-5 shrinks to the stimulus half — which, per §1, is mostly `SigGen` growing up.

## What I would keep, and what I am disregarding

Keeping: the outcome (diagnosis from a caught event; reproducible savable stimulus), AC-4's
VCD-only stance (correct, and it is what makes one recorder tractable), and AC-3 — but read as
a recorder-unification criterion, which is how it should be titled.

**Explicitly disregarding as written:** AC-1's byte-identical *element* round-trip and AC-5's
armed/unarmed throughput ratchet. Both are consequences of the canvas-element decision rather
than of the outcome; on the seam proposed in §2 the first becomes a metadata round-trip and
the second becomes vacuous. If Open Question 3 is re-opened on that basis, its "batch mode can
drive them" argument should note that batch already drives stimulus two ways today.

**Cost.** The 3–4 mw band is defensible for the element work and not for AC-3. Either price the
recorder unification here (call it 6–8 mw combined) or file it as its own feature that PF-1,
PF-5, and #538 all order after. As filed, the cheapest-looking criterion carries the largest
unowned piece of architecture in the capstone.
