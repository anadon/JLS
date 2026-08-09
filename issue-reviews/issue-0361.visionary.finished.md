# Issue #361: FEAT-029: a balanced-ternary datapath is something you draw, clock, probe, dump and test with the same palette, viewer and grammar as any binary circuit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is actually for

Stripped of the ternary framing, the end is: **a JLS wire can carry a symbol from a
declared alphabet, and everything downstream of a wire — drawing, simulation, probing,
waveform, `-t` vectors, export — keeps working.** Balanced ternary is the demo, not the
goal. Today's amendment says so out loud: the kernel is restated over *intervals*, the
ternary CPU census is dropped, and the completeness census becomes CAP-39's
mixed-alphabet walkthrough (#888). The title is now a legacy artifact of the closed
CAP-03 (#295).

That end is worth pursuing. The decomposition proposed to reach it is not the one I
would fund, and the reason is a single line in §4.

## The load-bearing objection: invariant 1 is a code-shape rule wearing an invariant's clothes

Invariant 1 — "no radix attribute is added to any existing element type; a test asserts
every pre-existing type still reports radix 2" — is defended as the thing that keeps this
from becoming a change to JLS rather than an addition to it. It conflates two claims:

- **Behavioral:** a binary user's experience is that nothing happened. Same goldens, same
  saved bytes, same palette, same VCD, same warm-loop timing. This is right, it is the
  whole point, and #888's AC-4/AC-6/AC-7 already state it *behaviorally* and testably.
- **Structural:** no existing class may mention a domain. This is not implied by the
  first, and it is the sole reason this feature costs a parallel element family.

Every visible cost in §2, §5 and §6 is downstream of the structural half. Fourteen new
registered types, batched over two release cycles because a package coverage average
resists them; a palette-view dimension needed to hide them; fourteen `HdlExporter` rows;
fourteen icons, help topics, `SaveTags` rows, dialogs, renderers and round-trip fixtures;
a "family completeness census" invented because nobody can tell by inspection whether
fourteen is the right number. Replace the structural half with "every element type
*defaults* to `[0,1]`, swept over `ElementRegistry`" — still a build failure if violated,
still sweepable, still forbids a type acquiring a domain by accident — and most of that
cost evaporates.

## The seam I would cut instead: transport vs. operator, not binary vs. N-ary

The element hierarchy does not divide into binary and non-binary elements. It divides
into elements that **interpret** values and elements that **move, hold, select, or show**
them:

- `Mux.react` selects an input and copies it. `Register`/`ShiftRegister`/`Memory`/
  `RegisterFile` store and reproduce. `Splitter`/`Binder` regroup positions.
  `InputPin`/`OutputPin`/`SubCircuit`/`JumpStart`/`JumpEnd` carry across a boundary.
  `Display`, `Constant`, probes present or emit. **None of these has binary semantics** —
  they have a `bits` count on their puts and nothing else, and a domain on the port is a
  strictly analogous parameter.
- The genuinely binary things are `Gate` and its sealed permits list, `Adder`'s carry,
  `Extend`'s sign replication, `Decoder`, and `TruthTable`'s row enumeration.

Cut there and the new registered types number roughly **three** — an interval-ordering
gate (min/max), a unary interval operator (reflect/cycle), and the bridge — rather than
fourteen. And the payoff is qualitative, not just cheaper: the proposed fork **is not
closed under composition.** There is no N-ary `Register`, `Memory`, `SubCircuit`,
`InputPin` or `Splitter` anywhere in TASK-0061's roster. A ternary *datapath* — the word
in the title — needs storage and hierarchy. #888's walkthrough is a combinational ALU
slice plus a bus plus binary control precisely because storage is not reachable; the
census was drawn around what the fork can express. Under the fork, every future
"a ternary X" is another class, forever, and the sole-maintainer surface compounds. FEAT-039
(#345), which still sits downstream, is the proof: a drawn ternary CPU under this plan
means forking the storage family too.

**The strongest argument for my seam is that it coincides with the performance seam
#344 already drew.** The REPLAN establishes a binary fast tier and a generic tier with a
differential oracle. Transport elements copy a value object and never inspect it — they
are tier-agnostic by construction. Operator elements are exactly the ones that dispatch
on tier. So the boundary that keeps KC-39-1 (no measurable binary-loop regression) is the
same boundary that decides which elements generalize and which fork. That is not a
coincidence to work around; it is the architecture telling you where to cut.

## Second alternative: the family is drawable, so draw it

The issue rules out the plugin path (#330) and never considers the extension mechanism
JLS already ships: **`TruthTable` + `SubCircuit`**. Under an interval-aware value model, a
user-authored truth table over `[-1,+1]` ports *is* min, max, an Allen–Givone literal, a
reflect, a cycle, and the T-gate — all of them, with the row-budget refusal the amendment
already specifies. Wrapped in a subcircuit it is savable, nameable, reusable and
shippable under `examples/`.

For the stated audience — "a student or researcher exploring non-binary logic" — this is
*pedagogically better*, not merely cheaper. Writing out Łukasiewicz implication as a
3×3 table is the lesson. Selecting `MinGate` from a hidden toolbar is not. A shipped
ternary circuit library costs zero registered types, zero icons, zero help pages, zero
`HdlExporter` rows, and zero pressure on the `jls.elem` coverage floor — which is to say
it deletes §6's entire two-release-cycle calendar argument. Promote a drawn primitive to
a compiled-in element only when the census shows it is hot. That inverts the plan's
ordering from *ship fourteen, then compose* to *compose, then ship the few that earned it*
— and it directly answers §2's own worry that "shipping eight types nobody has composed
into a datapath is how an element family acquires a missing primitive discovered a year
later."

## Third: the interval model partially duplicates the width model

A port already declares how much it can carry: `Put.bits` (`src/jls/elem/Put.java:34`).
A width-`w` binary port carries `2^w` values. The interval model adds a *second*
per-port value-space notion, then needs amendment item 3 (bundle numerals) to relate the
two and item 4 (the bridge) to convert between them. Two overlapping answers to "how much
information is on this wire" is a smell. The unification is one sentence: **a port's
domain is `(alphabet, width)`**, today's model being `([0,1], w)`; the bridge is then not
a new element but `Splitter`/`Binder` generalized once. The amendment half-sees this — it
literally calls the bridge a "generalized Splitter/Binder" — and then files it as a new
type anyway. The only thing preventing the generalization is invariant 1.

## What I would keep, and promote

- **TASK-0105, per-view palettes.** The most valuable thing in this roster and the one
  least dependent on it. It is shared with #316, #329 and #331, and it is the mechanism
  that lets JLS grow at all without eating the first-year toolbar. It should land on its
  own merit, ahead of and independent of any N-ary work, not as this feature's gate.
- **The kernel-once discipline** and the exact-collapse identity at `[0,1]`. Correct, and
  it is what makes generalizing transport elements safe rather than reckless.
- **The honesty rules**: the encoding named in the export header, the radix manifest in
  the dump, "a lowering is not a ternary netlist." Keep all three verbatim.
- **The interval reframing itself** (amendment item 1-2). Balanced ternary as native
  `[-1,+1]` rather than a rendering over `{0,1,2}` is the single best idea in the thread.

## Trajectory check

The 304-standard sweep and all eight leapfrog studies never derived N-ary logic as a
capability — `docs/capability-roadmap/sweep-01-values-and-logic.md` identifies the value
domain as "the narrowest waist in the whole program" and spends V1-V8 on four-state, drive
strength, driver/net kind, 1164, reset discipline, inout, don't-cares and timing checks.
Not one of them is radix. The demand signal for this feature is exogenous: its only
beneficiary was closed as priority 17 of 18, and a replacement capstone was written the
same week to keep it alive. That is not disqualifying — a maintainer may fund a thing
because it is interesting — but it should set the budget. Fourteen permanent element
types and two release cycles is a large bet on an outcome the project's own analysis
never asked for; three types plus a drawn library is a proportionate one, and it leaves
the four-state spine (#322) as the program's real center of gravity, where the roadmap
put it.

## Two same-day drifts worth folding in

- The amendment (18:55) still speaks of nets/bundles holding intervals; the maintainer's
  correction on #344 (19:05) moved the domain **onto ports, one direction only, wires as
  pure value carriers**. Items 3 and 4 need restating in port terms — which, notably,
  makes my `(alphabet, width)` unification the natural spelling.
- §3 leans on `HdlExporter`'s "four class sets" and the `REJECTED` bucket. On `master`
  there are **three** (`src/jls/hdl/HdlExporter.java:422,431,436`); `REJECTED` is
  branch-only work (#492, per the evidence-pin comment). Invariant 6 currently cites a
  mechanism that does not exist.

## Acceptance criteria I am explicitly disregarding

Integration criterion 3 ("the default palette is unchanged *with the family present*")
and criterion 2 ("the family is complete, measured against a census") are both artifacts
of the fork. Under the reframing there is no eight-entry family to hide and no census to
draft — the palette question reduces to TASK-0105's own contract, and completeness is
demonstrated by CAP-39's walkthrough running, which criterion 1 already asserts. Keep
criteria 1, 4 (restated as *defaults to* `[0,1]`) and 5. Drop 2 and 3 with the roster
they measure.

**Verdict: rethink.** The goal stands and is now better stated than when it was filed.
The route — a parallel element family forced into existence by a structural prohibition,
batched around a coverage average, and not closed under composition with storage or
hierarchy — is the expensive way to reach it. Re-cut along transport-vs-operator, relax
invariant 1 to its behavioral form, ship the primitives as drawn circuits first, and the
feature shrinks from two release cycles to something a single maintainer can hold.
