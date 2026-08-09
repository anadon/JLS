# Issue #397: TASK-0100: the analog solver is checked against real ngspice nightly inside a derived envelope, with detectors that catch a regression staying inside it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Strip the apparatus and one sentence remains: *JLS's analog answer should be believable by
someone who does not trust JLS.* Everything else — three tiers, 26 fixtures, four detectors,
a nightly lane, a promotion ritual — is machinery in service of that. Judged against that
end, the machinery is aimed at the weakest available instrument, built out of a component
that is scheduled to be thrown away, and it centres on a number its own capstone has already
refuted.

## 1. The envelope at the centre of the task is refuted by the issue's own Observations

H1 asserts ngspice's build-to-build noise is `~1e-13` and places 1e-4 "two decades above" it.
O5, four paragraphs earlier in the same issue, reports the measurement: two ngspice builds on a
two-element linear RC differed in 66% of internal time points with **worst relative difference
5.38e-04**. Those are two different quantities — same-build repeat noise versus across-version
drift — and H1 substitutes the first where the second is needed. The consequence is not
cosmetic. 5.38e-4 is *5.4x larger* than the acceptance envelope, so an honest, genuinely
independent comparison is expected to breach 1e-4 on any runner set that spans two ngspice
releases — which is exactly the population H4 insists on by leaving the oracle unpinned. H4 and
H1 cannot both hold.

#309 (CAP-14), the capstone this task ultimately serves, has already worked this through and
superseded the number: binding floor = max(measured across-version drift 5.38e-4, `RELTOL` 1e-3)
= 1e-3; ceiling = model-error onset 1e-2; declared envelope = √(1e-3 × 1e-2) = **3.16e-3**. It
states plainly that "the filed 1e-4 could not stand" and that AC-2 and the anti-cheat lower
bound overlapped at 1e-4 for every passing value. TASK-0100 hardcodes 1e-4 into
`EnvelopeComparator`, into H1, into §7.10's derivation, into §7.12's "do not tighten below ~1e-8"
guidance, and into the Definition of Done, and instructs the implementer to **write the refuted
arithmetic into a source comment** as the tolerance's justification.

## 2. The deeper consequence: pointwise oracle comparison has ~one decade of usable window

Floor 1e-3, ceiling 1e-2. That is the whole instrument. A measuring device with one decade of
range, whose zero point moves with an upstream project's release schedule, is a smoke test, not
a correctness oracle. The issue implicitly concedes this — it needs four detectors precisely
because the envelope catches so little — but keeps the envelope as the subject and the detectors
as supplements. The proportions are inverted. #309's own OQ-8 asks whether the pointwise leg is
worth keeping at all against derived scalars (final value, peak, RMS, period); that question
should be answered *before* 21 fixtures are built around the pointwise leg, not after.

## 3. The wrong seam: the deck emitter is the load-bearing artifact and it is being built as scrap

§6 puts `NgspiceDeck` in `test/`, private, unversioned, with no golden of its own. But the deck
is what decides whether any of this means anything. #309's FEAT-001 rationale states the hazard
exactly: a deck that silently omits a device "agrees beautifully with a JLS solution taken after
the same omission," and KC-14-5 makes unfrozen positional pin order a stop condition because a
mis-ordered device line *parses, simulates, and answers a different circuit*. TASK-0100 has no
defence against either. Its anti-cheat bounds are about numerical independence; structural
fidelity of the deck is unguarded. A green nightly over the wrong circuit is the failure mode
this task is least equipped to see.

Meanwhile #309 requires the emitter as a **shipping** capability: `jls -export design.cir
design.jls`, byte-identical golden decks, no date and no command line, positional pin order
frozen, node names a function of stable id via the shared partition IR (#336), registry-keyed
totality (#315) so no device vanishes. `src/jls/hdl/` already demonstrates the shape:
`HdlModel` walked once, rendered by `VerilogEmitter` and `VhdlEmitter` in unrelated syntaxes. A
SPICE deck is the third renderer over that same walk. Building a fourth, private, unpinned net
walk in `test/` acquires a second partition that will disagree with the first, and then discards
it when the real emitter lands.

## 4. The alternative the issue never considers: export first, solve later

Invert the dependency. A SPICE deck exporter needs the net partition (#336), seconds on the time
axis (#367) and device cards (#331). **It does not need FEAT-046's solver at all.** Ship it, and
JLS is an analog schematic-capture front end for ngspice: the student in FEAT-046's own audience
statement — "a student drawing a mixed-signal front end" — draws the circuit in JLS, exports,
runs `ngspice -b`, sees the waveform. That is a complete, honest, useful capability at roughly a
tenth of the analog programme's cost, with zero numerics, zero determinism regime, zero
26-fixture corpus, and zero exposure to the 1e-3..1e-2 window. It is also precisely the
delegation stance the repository already holds and already documents:
`docs/capability-roadmap/README.md` §6 — "JLS emits a netlist and constraints; nextpnr,
openFPGALoader and vendor tools consume them" — and README's framing of Verilog export as "a
deployment bridge, not an HDL tutorial."

Export-first also *reorders the risk correctly*. The silent-wrong-deck hazard (§3) becomes the
first thing pinned instead of the last. And when the pure-Java solver later arrives as the
offline convenience the single self-contained jar deserves, its differential corpus already
exists: the exporter's goldens, now additionally run through the solver. TASK-0100's entire
content collapses into a comparator plus a lane over fixtures somebody else already paid for.

The honest counter: the offline jar is load-bearing (README, ARCHITECTURE.md), and a lab machine
without ngspice gets nothing from an exporter. Granted — this is a *sequencing* argument, not a
substitution one. Export-first does not cancel the solver; it delivers something shippable
first and de-risks what follows.

## 5. The oracle-free tier is the real asset, and it is item three of three

Tier C — 5 invariants, required lane, no external binary, no version drift, no promotion
ritual — is the strongest thing in this issue and gets five bullet points. The issue itself says
node-renumbering invariance is "the strongest single test here." It is right, and it should have
concluded from that. Two families it never reaches:

- **Order of accuracy.** For a time integrator the sharpest correctness statement available is
  the *observed* convergence order: halve `h`, watch global error fall by `2^p` for the claimed
  `p`. It is oracle-free, deterministic, cheap, and it measures directly what R1/R3/R4/R5 are all
  proxies for. A trapezoidal integrator that has quietly become first-order passes every 3.16e-3
  envelope check ever written and fails an order test on the first run.
- **Manufactured solutions and metamorphic relations.** Choose `x(t)`, derive the source that
  produces it: unlimited exact fixtures without hand-deriving ten closed-form circuits.
  Metamorphic relations generalize Tier C well past its five: impedance scaling (`R→kR`,
  `C→C/k` gives an identical waveform), time scaling, source superposition on linear nets,
  Thévenin/Norton substitution, series/parallel reduction, subcircuit flattening. These are
  *relations*, not fixtures — property-based and generative, composing with the habit the repo
  already has (`GenerativeRoundTripFuzzTest`, `ContainerMutationFuzzTest`). A corpus that grows
  without a maintainer-week per circuit.

Against this, "26 committed fixtures" is the expensive way to buy less evidence.

## 6. Trajectory: the tree still says analog is a different tool class

`docs/capability-roadmap/README.md` §6(a) and `sweep-03-elements-and-hdl.md` both exclude
continuous-time analog on the stated ground that "supporting these means being a SPICE-class
solver — a different tool, not a deeper digital model," and `docs/grand-architecture.md` §9
excludes in-house simulation in favour of orchestrating external tools. README.md's first
sentence still describes "an educational digital logic circuit editor and simulator";
ARCHITECTURE.md contains no analog anything. #309 AC-7 reopens the refusal deliberately and by
the right procedure — that is to its credit. But TASK-0100's founding warrant, maintainer
directive D8, is cited from `docs/plan/evidence/BRIEF.md` §13, and `docs/plan/` **does not exist
in this tree**. A 7-11 maintainer-week task whose justification is unlanded, contradicting three
landed strategy documents, should not proceed until the amendment is in the tree. That is a
one-paragraph edit to README, one recorded decision in ARCHITECTURE.md, and one superseding note
in the roadmap §6 — cheap, and it is what makes the programme legible to anyone who arrives after.

## 7. What to do instead

1. **Do not build the 26-fixture corpus.** Build #309's own stated minimum: two fixtures (linear
   RC, RLC) through the emitter, parser, comparator and lane — the "2-3 mw honest version." Grow
   per device family behind the promotion gate that already exists.
2. **Promote the deck emitter to `src/`** as the third renderer over the shared net partition,
   with a byte-identical deck golden, frozen positional pin order and registry-keyed totality.
   That is the artifact with standalone student value and the one that decides whether the oracle
   is comparing the drawn circuit at all. `ProcessBuilder` still never enters `src/` — emitting a
   file is not running a binary, and the invariant survives untouched.
3. **Make the oracle-free tier the subject of a task of its own**: order-of-accuracy verification,
   manufactured solutions, and the metamorphic relation set, all on the required lane. This is the
   evidence that does not rot, and it is the only tier immune to §2's one-decade window.
4. **Take the envelope from #309, not from this issue.** 3.16e-3, derived at close by the printed
   rule, with the anti-cheat floor at 5.38e-4 so the two conditions stay disjoint.

## Disregarded acceptance criteria, and why

I am explicitly setting aside the DoD lines that pin 1e-4, the "~1e-8" tightening threshold, the
"two decades either side" derivation comment, and the 21-fixture Tier A/B build-out. They encode
an arithmetic error the owning capstone has already corrected, and they buy a decade of
resolution at a cost of several maintainer-months. The criteria worth keeping unchanged are P5
(the parser rejects a surviving `Date:` line — land it first, exactly as written), P6 (the
oracle-free invariants, which deserve promotion to the task's subject), P7 (self-skip), and P8
(`ProcessBuilder` confined to `test/` — and, under §7.2 above, to `test/` *plus* a `src/` emitter
that writes text and spawns nothing).
