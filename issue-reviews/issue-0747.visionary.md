# Issue #747: TASK-C546-1: a circuit reads out as a part-to-whole prose narrative a blind student can follow linearly
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the deliverable noun away and the claim is: **a JLS circuit must have a
non-visual representation that is complete enough to study from.** The prose
file is one rendering of that; it is not the thing itself. CAP-26 (#507) says
so implicitly — step 2 (spoken navigation) and step 3 (prose + tactile) are the
same information reaching a student through three different channels, and PF-5's
VPAT then has to claim, criterion by criterion, that the information exists at
all (WCAG 1.1.1, 1.3.1, 4.1.2, 508 502.3.6 relationships).

The project has already done this analysis and reached a stronger conclusion
than #747 does. `docs/standards-adoption/03-accessibility-conformance.md` §6
("The canvas disclosure") costs the full Java-Accessibility-API canvas tree at
8–15 maintainer-days, recommends **against** it, and recommends instead "a
keyboard-reachable circuit outline view — a `JTree` or `JList` of elements,
their names, their values, and their connections", explicitly on the Revised 508
**E101.2 Equivalent Facilitation** theory. That recommendation and #747 are the
same model with two front ends. #747 does not cite it.

## Reframing 1 (the load-bearing one): build the model, not the file

Cut the seam at a **`CircuitOutline`** — a headless, canonically-ordered,
language-free semantic model of a circuit (ports, state boundaries, blocks,
nets, element roles, connectivity) — and make the prose narrative one *emitter*
over it, roughly 200 lines. Renderers that fall out for free or near-free:

| Consumer | Renderer | Owning issue |
|---|---|---|
| offline prose narrative | plain text / Markdown | #747 (this) |
| the outline view the ACR doc recommends | Swing `TreeModel` — accessible for free on all three AT stacks | #355 / CAP-26 PF-3 |
| spoken element + connection announcements | phrase per node, no second vocabulary | CAP-26 PF-3 |
| `<title>`/`<desc>` on the *ordinary* SVG export | node labels | the `SvgExportTest.titleAndDescAreEmitted` leg already planned in the ACR doc |
| tactile SVG label layer | same names, same order | #749 |
| ACR evidence appendix | golden dump | CAP-26 PF-5 |

This is not speculation about a pattern JLS might like; it is the pattern JLS
already proved. `jls.hdl.HdlModel` is a 1005-line "language-neutral structural
model … it knows nothing about Verilog or VHDL syntax", with `VerilogEmitter`
and `VhdlEmitter` rendering from it and `HdlNames` supplying deterministic
naming. `ElementRenderers`/`BuiltinElementRenderers` is the same shape on the
GUI side. A `NarrativeExporter` that walks `Circuit` directly would be the
project's *fourth* independent traversal of the element graph (save, HDL, SVG,
narrative), each with its own idea of ordering, naming, and hierarchy — and the
fifth and sixth (outline tree, announcements) will then be authored separately
because nothing shared exists to hang them on.

Do **not** literally reuse `HdlModel`: it is deliberately partial (`HdlExporter`
refuses non-synthesizable elements) where the narrative must be total over the
registry, and `HdlNames` legalizes identifiers for Verilog, which is wrong for
prose. Reuse the *shape*, and factor out the genuinely shared part — the
`WireNet` → net-graph construction inside `HdlExporter.buildModel` — rather than
writing it a third time.

## Reframing 2: the acceptance oracle is the weak point — replace it

AC-2 ("narrative ordering passes a guideline checklist test asserting
part-to-whole structure") will, in practice, assert that section headings appear
in a given order. That test passes for a narrative that is useless — one that
names every gate and lets the reader reconstruct nothing. It tests *shape*, not
*sufficiency*, and sufficiency is the entire point.

The house discipline already has the right instrument. #85's H1 made the
semantics doc falsifiable by requiring golden values be **re-derivable from the
document alone**; `CircuitRoundTripTest` and the format spec pin meaning by
round-trip. Do the same here: make the narrative's structural layer
machine-parseable and add a **reconstruction test** — parse the narrative back,
rebuild a connectivity graph, assert it is isomorphic to the original circuit's
(modulo geometry and cosmetic elements). That proves the blind reader is not
missing information a sighted reader has. Keep the checklist test as a
secondary, ordering-only assertion; do not let it be the only one.

Side benefit: a grammar-carried structural layer satisfies AC-4's "no string
concatenation that assumes English word order" *by construction* rather than by
review vigilance.

## Reframing 3: the hard part is decomposition, and the issue never names it

"Inputs, then the blocks they feed, then how the blocks compose, then outputs"
presumes blocks exist. A student's flat adder is a bag of gates and wires with
no blocks in it, and any sequential circuit has feedback loops that make
"inputs first, outputs last" ill-defined. This — not phrase generation — is the
1–1.5 mw of real work, and it needs a recorded decision:

1. **`SubCircuit` hierarchy** where the student used it (free, and pedagogically
   the right answer: it is the structure they authored).
2. **State elements as cut points** otherwise: `Register`, `Memory`,
   `RegisterFile`, `ShiftRegister`, `StateMachine` break every cycle; narrate
   the combinational cones between state boundaries. This matches the
   edge-triggering account in `docs/simulation-semantics.md` §8 and is exactly
   how the subject is taught.
3. **Output cones** within a cone-free region, topologically levelized from
   inputs, ties broken deterministically.

Naming is the other half and is quietly hostile: elements are optionally named,
and `HdlExporter` synthesizes identity from coordinates ("AndGate at (240,120)").
Coordinates are precisely the channel this reader does not have. Unnamed
elements need **role-derived** names ("the AND gate driving `sum`"), stable
across runs — a naming rule worth writing down next to `docs/component-naming.md`
rather than inventing inside an exporter.

## Reframing 4: the dependency on #542's task is the wrong dependency

`ordering_after: [TASK-C542-2]` chains this behind registry-keyed
**state-to-encoding** data — thickness, dash, glyph. Those are *visual*
encodings. Prose must say "high", "low", "high impedance"; it must not say
"a thick dashed line". The thing both consumers genuinely share is the canonical
**state vocabulary** (the wire-state domain of `docs/simulation-semantics.md` §2
and `BitSetUtils`), from which one renderer picks a glyph and the other picks a
noun. Factor that enum + its names out as a small shared piece and the two tasks
run in parallel instead of in series — a real schedule win on a 3–4 mw feature.

Note also that a static narrative of a drawn circuit has **no state to describe
at all**. The dependency as written is largely vacuous until the narrative
covers a simulation, which brings me to:

## The out-of-the-box addition: narrate behavior, not only topology

A blind student completing a lab needs to know what the circuit *did*. VCD is
unreadable to a screen reader; the trace window is a picture. `BatchSimulator`
already accumulates `TraceSample`s and `jls.BatchTracePrinter` already renders
them textually. A **narrated trace** — "at 30 ns, `carry_in` goes high; the
adder's sum output follows at 40 ns" — over the same `CircuitOutline` names is
close to free and is plausibly worth more to lab completion than the static
narrative, and it is where the C542-2 state vocabulary actually earns its
dependency. I would fold this into the feature's scope rather than leaving the
accessible bundle describing a circuit that never runs.

Second, cheap, large-audience win: point the same model at `<title>`/`<desc>` in
the **ordinary** `-i out.svg` export. Embossers are rare; a lab report or course
page carrying a screen-readable SVG is common, and this costs one emitter.

## Duplication to head off

AC-3 adds a fourth per-element English text obligation. JLS already requires,
per element type: a help page (`HelpTopicsTest` completeness), status/probe text
(`showInfo`/`showCurrentValue`, ARCHITECTURE.md's sixteen-step list), an
accessible name in the palette (#75/#210), and now a describable phrase. These
will drift — that is what four parallel tables always do. One `describe`
contract on the element-type descriptor, feeding all four (the help page's
opening sentence included), is the version of AC-3 worth building.

Placement matters: `ElementType` is deliberately core-only ("GUI concerns …
never appear here"), and a description is neither loader concern nor GUI
concern. The recorded seam is `docs/extension-points.md` — publish an
`elem.element-describer` point alongside `ELEMENT_PROVIDER`, catalogued and
pinned by `ExtensionPointCatalogTest`. That also fixes AC-3's sharp edge: "an
unmapped type **fails the build**" is correct for built-ins and wrong for
external providers (#212), where a missing phrase must be a load-time rejection
of that provider, not a broken build for everyone.

## The i18n line — be explicit rather than clever

AC-4 walks right up to a recorded decision (ARCHITECTURE.md, "Internationalization:
non-goal", with "PRs adding partial i18n scaffolding will be declined") whose
revisit trigger (b) is *"the element-registry work (#78) centralizing element
metadata to the point that string externalization becomes cheap as a side
effect."* A registry-keyed phrase table plus a language-free structural layer is
close to a demonstration that trigger (b) has fired. That is a good outcome, but
it should be decided, not smuggled in under an accessibility task. File it as a
`REPLAN:` on #507 or a follow-up on #85: either the phrase table is the
recorded first externalized string set, or the decision is reaffirmed and the
table stays inline English on purpose.

## What I would keep, and what I would change

Keep: AC-1 (one command, deterministic — the project's determinism convention
already has teeth via `DeterministicSaveTest`/`SvgExportTest`); AC-4's intent.
Change: AC-2 gains the reconstruction oracle and stops being the only test;
AC-3 becomes one `describe` contract on the extension-point seam, with the
build-failure rule scoped to built-in types; the deliverable becomes
`CircuitOutline` + a prose emitter, with the outline `TreeModel` named as the
second consumer so #355/PF-3 cannot re-author it; the `ordering_after` chain is
cut down to the shared state vocabulary. I am not disregarding the stated
outcome — the outcome is right and, as CAP-26 says, category-defining. I am
saying the issue as filed buys one file where the same money buys the
representation the ACR document already asked for.
