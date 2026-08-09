# Issue #698: TASK-C534-2: a programmable word generator plays a stimulus table from the canvas, and the same program recorded interactively and headlessly is byte-identical
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

Two claims are bundled here under one 1–1.5 mw band:

1. **Stimulus should be a first-class, savable, canvas-resident artifact** — a student's test
   program travels with the circuit instead of living in a side file.
2. **An interactive run and a graded batch run of the same stimulus must not disagree.**

Claim 1 is right and *already largely shipped*. Claim 2 is right, is the more valuable of the
two by a wide margin, and is not a word-generator property at all — it is a statement about
JLS having one recorder and one stimulus path. The task as filed spends its budget on the
part that exists and hides the part that does not inside a single acceptance criterion.

## 1. The word generator is `SigGen`, and it already meets AC-1

`src/jls/elem/SigGen.java` is a drawable, placeable canvas element that serializes as
`ELEMENT SigGen` through declarative `Attribute` persistence, round-trips through the ordinary
`Circuit.load`/`finishLoad` path, and carries a user-authored stimulus program. `SigSim.initSim`
(`src/jls/elem/SigSim.java:40-204`) parses exactly the table this issue describes — value,
hold, width — as `name initial { for <d> <v> | until <t> <v> } end`, with hex literals and `#`
comments, and posts the whole schedule as timed `SimEvent`s at time 0. It is in
`SaveTags`, in `Palette` (`Group.TEST`), in `docs/file-format.md`, in `Map.jhm`, in
`ElementVocabulary` for collab, and its grammar is a *documented stability promise*
(`docs/batch-interface.md` §2).

AC-1 ("placeable, wirable and programmable through the ordinary element path, round-trips
byte-identically") is therefore already green for a shipped element, and AC-5 ("an idle
generator with no program costs nothing measurable") is vacuous for it: an empty program posts
zero events and `SigSim.react` throws — the element is not on the hot path at all. Two of five
criteria are satisfied before any work starts, by the wrong element only in the sense that it
has a different name.

The **real** delta between `SigGen` and the issue's word generator is one word in the outcome
statement: "the element drives *its outputs*". `SigGen` has no `Put`s. It resolves stimulus by
scanning `getCircuit().getElements()` for an `InputPin` whose *name* matches
(`SigSim.java:85-94`) and fails soft when none does — spooky action at a distance that silently
breaks when a pin is renamed. Giving `SigGen` output ports so it drives wires is a genuine,
small, well-motivated improvement, and it is the only element-shaped work this task should own.

**Concrete alternative to the whole of AC-1/AC-2:** do not mint a third stimulus mechanism.
Grow `SigGen` — add output ports (behind a format-version bump so old files still load), and
add a tabular editor that is a *view over the existing grammar*, round-tripping table ⇄ text so
the saved bytes stay the documented `-t` language. A third stimulus language (`-t` files,
`SigGen` text, word-generator table) is three parsers, three help pages, three grammars in
`batch-interface.md`, and a semantic collision the issue never mentions: `addTestGen` deletes
every `SigGen` in the top-level circuit when `-t` is given, so a new stimulus element that is
not a `SigSim` survives that deletion and double-drives the circuit.

## 2. AC-3 is not an instrument criterion — it is unowned architecture, and it is red today

**JLS has no interactive VCD writer.** `setVcdFile`/`writeVcd`/`toVcd` and the `TraceSample`
accumulation live entirely on `jls.sim.BatchSimulator` (`:24-34`, `:335-476`), gated on
`JLSInfo.printTrace || vcdFileName != null`. `jls.edit.InteractiveSimulator` has a wholly
separate, GUI-resident recorder (`traceMap`/`wireMap` of `jls.edit.Trace` rows) and no VCD
anywhere. So "the VCD recorded from an interactive run" is not a thing that can be recorded
today; AC-3 silently commissions a second recorder, then tests that the two recorders agree.
Building two implementations and policing their equality with a golden is the shape that
*guarantees* eventual drift. The design that makes the test near-tautological is one recorder.

And the drift is not hypothetical — the two paths already disagree structurally on stimulus:

- **Batch** (`BatchSimulator.addTestGen`, `:190-208`): creates the `TestGen`, **adds it to the
  circuit**, removes every `SigGen`. It is then initialized by `initSimulation()` along with
  everything else, in the circuit's canonical stable-id order.
- **Interactive** (`InteractiveSimulator.runSim`, `:588-608`): creates the `TestGen`, **does not
  add it to the circuit**, does not remove `SigGen`s, calls `initSimulation()` first and
  `gen.initSim(this)` *afterwards*.

`Simulator.initSimulation` (`:186-200`) is explicit that same-time events fire in posting order
(`SimEvent.seq`) and that stable-id seeding is what makes "every simulated value a pure function
of circuit content" (#181). Posting the entire stimulus schedule *after* every other element's
time-0 events is a different seed order than posting it in id position. For any circuit where
time-0 ordering matters — cross-coupled latches, multi-driver nets, the exact cases #181 was
written for — the interactive and batch runs of the *same program* are entitled to differ, and
a `SigGen` left alive interactively double-drives pins the batch run drove once. AC-3 is
falsifiable against HEAD before the word generator exists.

That is the finding this task should be carrying, and it is worth more than the element.

## 3. The reframing I would fund instead

Split #698 into two tasks and re-band:

- **T-a (~1 mw): `SigGen` grows up.** Output ports; tabular editor over the existing grammar;
  name-binding retained as a compatibility path. Delete AC-1 and AC-5 as *new-element* criteria
  — they are already met — and replace them with "an existing circuit containing a `SigGen`
  loads unchanged and simulates identically."
- **T-b (~3–5 mw, ordering before this task and before PF-1): one recorder.** Lift
  `TraceSample` accumulation and the VCD writer out of `BatchSimulator` into a `jls.sim`
  recorder driven off the existing `afterEvent`/`probeSample` seams, with a bounded retention
  window; make both front ends consume it; unify stimulus setup so `-t` is applied identically
  in both (add the generator to the circuit, in id order, in both paths). *Then* AC-3 reads
  "the same recorder produced both files," which is a one-line test.

T-b is the piece the whole capstone silently depends on: PF-1's chronogram needs a recorder it
does not own, PF-3 needs bounded retention (KC-23-2), PF-5's pre-trigger capture is a retention
window, and #538's WaveJSON consumer wants exactly one producer — which dissolves the
cross-cluster boundary question recorded on #534. Left inside a 1–1.5 mw task labelled "word
generator", it will be discovered late and paid for badly.

## 4. Alignment

ARCHITECTURE.md's recorded decisions are uniformly *one mechanism per job*: one simulation
strategy (#221), one classloader and type namespace (#222), one typed extension registry
(#223), one element registry (#78). "One recorder, one stimulus language" is the same decision,
simply unwritten. As filed, #698 pulls against that grain twice (a second stimulus element, a
second VCD recorder) while its most valuable criterion depends on resolving it.

**Explicitly disregarding as written:** AC-1 and AC-5. Both are satisfied by `SigGen` today, and
restating them as criteria for a new element buys a duplicate rather than a capability. AC-2
survives only as "the improved `SigGen` drives its output ports at the declared ticks." AC-4
(VCD only, no FST) I endorse without change — it is precisely what makes one recorder tractable.

**Cost.** The 1–1.5 mw band prices T-a and nothing else. If AC-3 stays in this task, the honest
band is 4–6 mw; if it moves to T-b as a prerequisite, 1–1.5 mw is right and this task becomes
genuinely small — which is the outcome to want.
