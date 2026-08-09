# Issue #561: FEAT-C29-4: a Falstad text-format circuit's logic subset opens in JLS as a working circuit, and every analog element is a named loss by design — the smallest importer proves the shared-report generalization
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two purposes are bundled here and they pull in different directions. The stated
one is instrumental: be the cheapest adopter of #556's report contract so the
contract is proved general (AC-3, and the mirror comment's "falsification test
of #556"). The unstated one is the actual user value #510 §3 identifies: capture
"the digital-coursework overflow" — Falstad courses that outgrew an analog solver
and need deterministic timing, hierarchy and grading.

The instrumental purpose is well served by the issue as written. The user purpose
is not, and three of the issue's own framings work against it. I endorse the work
and reject the shape.

## Ground truth from the tree

JLS already has an importer, and it is the fact the whole importer-family program
should be organized around. `src/jls/hdl/imp/NetlistImporter.java` (1,067 lines)
turns a Yosys netlist into a JLS circuit; `ImportSummary.java` already carries a
source-category → element-count mapping table plus a coercion tally — a proto
version of #556's construct/disposition contract, written independently, months
earlier. It has the no-partial-circuit discipline #323 §4 wants to inherit, and
the layout seam beside it (`src/jls/hdl/layout/`).

And it is unreachable. `grep -rn NetlistImporter src/` returns nothing outside
`src/jls/hdl/imp/`; the `FLAGS` table at `src/jls/JLSStart.java:759-788` has
`-export`, `-board`, `-pins`, `-savetext` and no import flag; #510 footnote 2
records the same finding ("wired to no CLI flag or menu, hence unreachable by
users… releasing and surfacing them is cheap score").

So CAP-29 plans a fourth importer on infrastructure whose only shipped instance
no user can invoke. That is the alignment problem, and everything below follows
from it.

## Reframing 1 — the falsification test already exists, and it is stronger

AC-3 asks this issue to prove the shared contract is not a `.circ` dialect by
having a second importer emit it unchanged. But #556, #558, #559 and #561 are all
being specified by the same author in the same week against the same mental model;
an adopter designed alongside the contract is a weak oracle. `NetlistImporter` is
the strong one: an importer written *before* the contract existed, for a source
(Yosys cells) unlike any of the three planned formats, with its own already-shipped
report shape. If `ImportSummary` can be expressed in #556's contract without
information loss — and if `coercedX` (a *semantic* coercion, not a dropped
construct) fits — the contract is general. If it cannot, that is discovered for
roughly zero marginal cost, before three importers are built on it.

Concretely: move the generalization proof to #556 as "retrofit
`jls.hdl.imp.ImportSummary` onto the shared contract," and let #561 stop carrying
AC-3's proof burden. The Falstad importer then justifies itself on user value
alone, which is a healthier basis for it anyway.

## Reframing 2 — a Falstad circuit is a link, not a file

This is the reframing that makes most of the problem disappear. Falstad's
ecosystem currency is the share URL — the "uncountable web embeds" of #510 §1.
Lecture notes, forum answers and lab handouts carry links, not files. An importer
whose entry point is a saved text file asks a switcher to first discover Falstad's
export dialog, which is exactly the on-ramp tax #510 §2 names as JLS's worst axis.

The circuit travels *inside* the link (plain in the `cct` form, compressed in the
`ctz` form) — so "paste a Falstad link" needs no network access at all: decode
locally, never dereference the URL. That property should be stated and tested as
an invariant, because it turns what looks like a security regression into a
security *feature* and keeps AC-4's #38 posture intact ("no import path reaches
the filesystem or the network," per #323 §4.3). Verify the two encodings before
committing; the compressed form needs a small decompressor and that cost belongs
in the band.

"Paste the link from your lecture notes and it opens" is a materially different
product than "import a .txt," at very nearly the same implementation cost, and it
is the only migration lever in CAP-29 that can be demoed in one sentence.

## Reframing 3 — I am disregarding "every analog element is a named loss by design"

The issue's title clause and its boundary note make blanket analog exclusion a
permanent non-goal, and AC-1 is satisfied by naming every analog element. Both can
be fully met while producing a circuit that does not work.

In real Falstad logic circuits the analog elements are not decoration; they are the
*interface*. Voltage sources, switches, grounds, pull-up resistors and LEDs are how
inputs get driven and outputs get observed. Drop them all as named losses and you
import a correct gate netlist with nothing driving it and nothing displaying it —
"opens" without "works," which contradicts the issue's own title and CAP-29's
outcome sentence.

The better rule is a distinction the issue never draws:

- **Analog at the boundary maps by intent.** Ground/Vcc → `Constant`; a switch or
  voltage source feeding a logic input → `InputPin`; an LED or output probe →
  `OutputPin`/`Display`; a clock source → `Clock`; a pull-up into a gate input →
  `Constant`. This is the same "map by semantics, name is a hint" rule #323 §3
  already ratifies, applied across the analog/digital line.
- **Analog as computation is the named loss** — op-amps, RC networks, transistor-level
  logic, anything whose behavior is the solver's. Permanently, exactly as written.

Replace AC-1's blanket clause with: *every analog element is either intent-mapped
at the boundary with the mapping named in the report, or a named loss; a circuit
whose logic imports but whose drivers or observers were dropped is a failure, not a
success.* An import with zero unexplained losses and zero drivable inputs should
not pass this issue.

## The gap this slice is uniquely placed to expose — and AC-5 as written cannot hold

#556's contract is construct → disposition → location → explanation: a
*per-construct* ledger. Falstad breaks it, and that discovery is worth more than the
importer.

Falstad's digital behavior is emergent from an analog solver (#510 §1: "Falstad's
digital behavior is emergent analog"; their own ring-counter unpredictability,
their #364). JLS is a discrete-event simulator with per-element propagation delays
(`docs/simulation-semantics.md` §6-7). Every gate that imports *successfully* lands
in a different timing model. AC-5 — "never silently rewrites semantics" — is
therefore unachievable by a per-construct ledger: nothing was dropped, so nothing
is reported, and the circuit may still behave differently. The same hole exists in
`ImportSummary.coercedX` today: a realized-but-changed disposition with nowhere to
live.

So the contract needs a second axis: a **model-delta statement** — a whole-import
declaration of which semantic model changed and what class of circuit is affected
(here: settling-time-dependent behavior, analog-threshold logic levels, and any
circuit whose correctness depended on the solver's convergence). Make that a
deliverable of #561 and feed it back to #556. It is also directly reusable by #559,
whose CircuitVerse `delay` attribute is the identical species of problem
(realized construct, changed meaning).

## Cut the seam at the lowering, not the schema

#556 generalizes the report's *shape*. It does not generalize any code. If four
importers share a schema and nothing else, the marginal-cost argument in CAP-29's
"why this is a capstone" is unfunded — each format still writes its own model
building, placement and emission.

The elegant seam is: `format-specific parse → common construct model (with
provenance and a loss ledger) → placement → circuit`. `NetlistImporter` already
implements the back half; #323 Open Question 4 already flags that its `Builder`
(`NetlistImporter.java:410`, `private static final class`) must be promoted before
a second importer forks it. Do that promotion once, and Falstad text becomes a
parser plus a mapping table — plausibly under the 2-3 mw band rather than at it.

One live contradiction to settle first: `ImportResult.saveText()` builds circuits by
emitting save text and reparsing it — the approach #323 §2 alternative 2 explicitly
*rejects* ("makes the importer's correctness depend on the save grammar"), with
#337's headless op layer as the recorded fix, graded only "beneficial." A
four-importer family will bake whichever answer you pick in four places. Decide
before the family, not during it.

## Sequencing, and a naming collision worth catching now

Ordering: import should become a reachable verb — File > Import (foreign) plus an
`-import` CLI flag, over the existing `NetlistImporter` — before any new format is
parsed. That is days of work, it converts already-paid-for capability into user
value, and it gives #561 a live surface to land on instead of a dead end.

Note the vocabulary collision that surfacing will hit: `SimpleEditor.addToImportMenu`
(`src/jls/edit/SimpleEditor.java:477`) already owns "Import" for pulling a subcircuit
from another open `.jls`. Two unrelated meanings of the same verb in the same menu
will confuse exactly the switcher this capstone is trying to keep. Pick the names
now (e.g. "Import Circuit From…" vs "Insert Subcircuit"), cheaply, rather than after
four importers ship.

Also honest: #510 §4 rates the on-ramp gates (shop window, first-run, chronogram,
published benchmark) as prerequisites to *every* migration pull. A Falstad user who
pastes a link, gets a working circuit, and lands in an empty `JTabbedPane` with no
waveform view has been migrated into a bounce.

## Recommended shape

Keep the feature. Change it to: **paste-a-Falstad-link import of the logic subset,
with the analog/digital boundary intent-mapped and analog computation named as loss,
emitting #556's contract plus a model-delta statement, landing on a promoted shared
lowering behind a reachable Import verb.** Move AC-3's generalization proof to #556
via the `NetlistImporter`/`ImportSummary` retrofit. AC-2, AC-4 and AC-5's undo
clause stand unchanged; AC-1 and the boundary note's blanket analog exclusion are
the parts I am explicitly disregarding, because satisfying them as written permits
an import that opens and does not work.
