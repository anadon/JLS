# Issue #453: TASK-0061: the first four N-ary element types exist as placeable, savable, simulable elements — with the binary boundary crossed only through a declared bridge
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of its apparatus, the ask is one sentence: **a student should be able to draw,
clock and simulate a balanced-ternary datapath in JLS** (P11), and the binary world should
meet it at a declared, refusing boundary rather than by silent folding. That goal is good,
it is downstream-load-bearing (#345's drawn ternary machine, #428's JLS-T3 ISA), and BRIEF
D10 is right that "nobody asked" is not a rebuttal. I am not arguing against N-ary JLS.

I am arguing that this issue picks the wrong seam to cut along, and that the two arguments
it offers as *structural rather than stylistic* both dissolve when you cut elsewhere.

## The seam the project already committed to

JLS's value currency is `@Nullable BitSet`, everywhere: `Input.setValue/getValue`
(`src/jls/elem/Input.java:59,72`), `Output.propagate` (`src/jls/elem/Output.java:136`),
`Put.currentValue` (`src/jls/elem/Put.java:385`), `SimEvent.NewValue(BitSet)`
(`src/jls/sim/SimEvent.java:39`). `docs/capability-roadmap/sweep-01-values-and-logic.md`
opens by calling that "**the narrowest waist in the whole program**" and names V1 — a
sealed `jls.core.LogicVector` with `Binary`/`FourState` cases — as "the spine", unlocking
16 standards directly and 24 counting dependents. `keystone-b-migration.md:535` costs the
migration stage by stage, including *stage 2, "widen the plumbing"* — precisely
`Put.currentValue`, `Input`/`Output`, `WireNet`, `SimEvent`, `TraceSample` — and organises
the whole thing behind a **per-circuit value-model attribute defaulting to today's model**
so every golden stays byte-identical until stage 9.

That is a fully designed, costed plan to make "what a wire carries" a first-class,
extensible, sealed type in the headless core. **A radix alphabet is a case of that type.**

Now the sharpest concrete finding: **this issue does not say what a `BitSet` holding the
digit `2` looks like, and neither does its dependency list.** §7.10 reasons entirely in
digits over $D_N$; §7.3 says the per-put radix "arrives from #419"; §6 says every `react`
is a call into #422's `RadixOps`. But none of that changes the type that `Output.propagate`
takes. So one of two things is true, and the issue is silent about which:

- **(a)** `MvlGate` packs ternary digits into a `BitSet` under some private encoding. Then
  the encoding is a new, undocumented value model that `Display`, `TraceSample`, VCD export
  (`BatchSimulator:522-551`), the `-t` grammar, `Watchable`/probe text and the save grammar
  all have to learn independently — the exact fragmentation the `LogicVector` sealed type
  exists to prevent.
- **(b)** #419 quietly widens the currency. Then **#419 *is* the keystone-b value migration
  under another name**, executed for radix only, without the dual-mode discipline, without
  the differential golden harness of stage 0, and without the `FourState` case that 24
  standards are waiting on — and the real V1 migration afterwards must reconcile two
  independent widenings of the same eight call sites.

Either way, the largest architectural decision in this programme is being made in a task
that lists it in neither its deliverables nor its Open Questions.

## The two "structural, not taste" arguments both dissolve

**O2 (`HdlExporter` fails open on an attribute).** The observation is accurate; the
inference is not. Look at the site: `buildModel` at `src/jls/hdl/HdlExporter.java:180-196`
is *already total* — anything not in `TOPOLOGY`/`SKIPPED`/`EXPORTED` becomes an `offender`
and throws `HdlExportException` naming it. Unknown classes already fail closed. The defect
is narrower and more general than the issue says: **classification asks `getClass()` when it
should ask the element.** Every widening sweep-01 has queued hits this identically — a
four-state `Adder` is `EXPORTED.contains(Adder.class) == true` and emits `assign sum = a+b;`
computing the wrong thing; so is an open-drain `Output` (V3), a bidirectional pin (V6), a
don't-care `TruthTable` (V7). The fix is a ~30-line change the project is already shaped
for: move the bucket onto the descriptor (`ElementType`, `src/jls/elem/ElementType.java`) or
a capability interface from #78, make the exporter ask "can you be rendered under this
value model?", and pin totality with a test. Do that once, and *no* future attribute can
fail open. Do it #453's way, and you have bought one attribute's safety with a permanent
duplicate element taxonomy — and V1's attributes will still fail open.

**O3 (a new attribute costs a format bump; a new type does not).** `docs/file-format.md`
§9 does not merely permit the bump — it *instructs* it: "Writers **SHOULD** therefore prefer
a version bump over an 'ignorable' attribute whenever dropping the attribute would change
simulation behavior" (`docs/file-format.md:466-471`). There is precedent one paragraph up:
FORMAT 2 exists for exactly this, vertical `Binder`/`Splitter` orientation, "a mis-load,
hence the bump". A `radix` attribute is the textbook case, the remedy is `FORMAT 3`, and a
version refusal is *strictly better* than an unknown-tag refusal because it is a statement
about the file rather than about whichever element happened to be parsed first. O3 is not a
second independent reason; it is the spec's own worked example pointing the other way.

## The alternative design

**Cut along the alphabet, not the element class.**

1. `jls.core.Alphabet` (or a third case of keystone-b's `LogicVector`): `Binary`,
   `FourState`, `Digits(radix)`. One sealed type, exhaustive dispatch, AWT-free — the model
   `SimEvent.Payload` already sets and `grand-architecture.md` §4.3 already mandates.
2. **Gates need no new classes at all.** `AndGate` is 78 lines and overrides exactly one
   thing: `computeOutput()` (`src/jls/elem/AndGate.java:64-72`). `Gate` is already the
   sealed superclass carrying inputs, bits, orientation, delay, drawing, save. Make
   `computeOutput` alphabet-polymorphic and `MvlGate(MIN_MAX, MIN)` **is** `AndGate` over a
   radix-3 alphabet. H2 — "MIN_MAX collapses exactly to binary at N=2" — stops being a
   hypothesis to test at every timestamp and becomes true by construction, because there is
   one code path. `MvlNot` and `MvlConstant` vanish the same way.
3. **`RadixBridge` survives, generalised.** It is the one genuinely new element here, and
   its justification is better than the issue's: down-converting digit `2` to binary has no
   image — and *so does down-converting `X`*. `NetlistImporter.connectConstant` folding
   `BIT_X` to 0 through a field named `coercedX` (`src/jls/hdl/imp/ImportSummary.java:27-28`)
   is the same failure this issue exists to forbid. One `AlphabetBridge`, one partial
   function with a declared diagnostic, and `digitTwoIsNeverFoldedToBinary()` and "no more
   coercedX" become **the same assertion**. That is the elegant version of this task.
4. **Port refusals become one rule, not eight messages.** A port declares the alphabet it
   accepts in its descriptor; the connection check reads it. `Register.C`, `Memory.CS/OE/WE`,
   `TriState.control`, `Clock` all declare `Binary` and the message is generated. P6's eight
   hand-written strings and their parameterized test are the same information typed twice,
   and every future element repeats the exercise.

## Where the issue's route leads if you extrapolate it

Batch 1 is four types. Batch 2 is four more. #345's drawn ternary machine needs registers,
memories, muxes, displays, adders, truth tables, pins, splitters/binders, subcircuit
boundaries and probes that speak ternary — i.e. a shadow copy of the 35-entry registry, at
the issue's own measured ~66 lines of registration and `ARCHITECTURE.md`'s sixteen
touchpoints each, plus an icon, a help page, a round-trip fixture and a `Palette` row per
type. Every cross-cutting surface doubles too: help tree, `ElementVocabulary`, VCD, `-t`,
image export, HDL. Meanwhile `ARCHITECTURE.md:117-145` and #78 are actively trying to make
adding *one* element cheaper. A parallel `Mvl*` family is the largest single expansion of
the thing the project is trying to shrink — and it is entered through a task that frames the
registration tax as a feature ("the tax is the point").

O6 is the tell the issue already senses this: `TruthTable`'s don't-care code `2` collides
with radix-3 data, and the fix "is also sweep-01 V7's don't-care/don't-know separation. **One
fix, two programmes.**" That sentence is true of the whole task, not just of `TruthTable`.

## What I am disregarding, and what I would keep

I am explicitly setting aside these acceptance criteria: the four `Mvl*` classes as classes;
"`EXPORTED` is not widened, verified by diff" as a *design* commitment; "no `FORMAT` bump";
and the framing of the eight-site registration tax as the mechanism. I keep, unchanged: the
anti-fold discipline (H5/P5) and its test, which is the single most valuable line in the
issue; the bridge as a declared element that names both alphabets rather than inferring them
(Open Question 5's recommended default is right, and for the right reason); P12's pedagogy
conditioning, which is real and unowned by any test; the opt-in palette group (P9); and P11
as the definition of done.

**Sequencing I would propose instead.** (i) Land the descriptor/capability fix to
`HdlExporter`'s three buckets — small, immediately valuable, and it retires O2 for every
future value-model widening. (ii) Land keystone-b stages 0–2 (fences, `LogicVector`, widen
the plumbing) with the per-circuit value-model attribute; this is already costed and it is
the work #419 is otherwise about to do by accident. (iii) Add `Digits(radix)` as the third
alphabet case and make `Gate.computeOutput` polymorphic — at which point drawing and
simulating a ternary min/max datapath needs exactly one new element, the bridge. (iv) Then
re-file this task as what remains: the bridge, the alphabet-aware port refusal rule, the
palette group, and the help-text corrections.

If the maintainer keeps the parallel-family route anyway, two changes are still mandatory:
**name the value representation** (which `BitSet`, or which new type, holds a `2`) as a
blocking Open Question in either #453 or #419, and **do not treat "no FORMAT bump" as a
virtue** — `docs/file-format.md:466-471` says otherwise for anything that changes what a
circuit computes.
