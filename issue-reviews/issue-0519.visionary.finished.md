# Issue #519: CAP-35: one documentation source builds the in-app help, a hosted versioned manual, and the screenshots — and none of it can silently drift from the program it describes
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this capstone is actually for

Two goals are bundled under one title, and they have different economics:

1. **Docs cannot lie.** JLS already leads here — `HelpTopicsTest`, `HotkeysHelpAccuracyTest`,
   `CliFlagTableTest`, and the normative `docs/*.md` with code anchors. This is a *ratchet*
   goal: cheap, incremental, immediately valuable, and it needs no pipeline.
2. **Docs are reachable, searchable, linkable, and not visibly from 1998.** This is a
   *delivery* goal: it needs a build, a publishing surface, and imagery.

The issue fuses them ("one source, three targets, drift-proofed") and then sequences the
ratchet goal *behind* the delivery goal (#587 `ordering_after: [FEAT-C35-1]`, AC-4 "run
against the generated targets"). That is the single largest structural mistake in the
filing, because the highest-value item in the whole capstone — the comment's own finding
that `CliFlagTableTest` "reads no documentation file at all", so `docs/batch-interface.md`
can name a flag `JLSStart.FLAGS` rejects and nothing turns red — is a one-day change
against today's tree that has been made to wait on a 2–3 mw source-format migration. The
`FLAGS` table (`src/jls/JLSStart.java:759`) is fifteen rows of structured data and
`docs/batch-interface.md` already declares itself normative; binding them is a `Grep`, a
set comparison, and an assertion. Land it now, unblocked.

## Reframing 1: generate the element reference; do not assert against it

I am explicitly disregarding #519's PF-4 line "every element page asserts against the
registry" and #587's AC-2 ("each page's documented ports, parameters and stated behavior
are checked against the registry descriptor"). As written, this asks a test to extract a
port list and parameter semantics from English prose and compare it to a descriptor. Look
at what such a test would have to parse — `resources/help/elements/gates/and.html` states
its inputs in a sentence ("Type the desired number of inputs in the 'Inputs' field"), its
orientation rule in a paragraph, and its bit-width behavior in 120 words. Any assertion
strong enough to catch a real stale port list is a prose parser; any assertion weak enough
to be maintainable catches nothing. This is the most brittle possible route to the
property.

The benchmark the issue cites points the other way. Digital's manual is not hand-authored
and then tested — its element reference is *emitted* from the element library descriptors
(pins, attributes, per-attribute prose keys), and its docu-tests mostly assert that every
element and attribute *has* a description, i.e. they guard the generator's input, not a
generated page's agreement with the model. Generation makes drift impossible by
construction; assertion makes it detectable after the fact. The issue imported the effect
and dropped the mechanism.

The concrete alternative, and it cuts along a seam JLS already has: **each element help
page becomes prose plus a generated fact block** — a ports table, a parameters table, and
the save tag/aliases — emitted from `jls.elem.ElementType`, `jls.edit.PaletteEntry`
(`type()`, `iconName()`, `tooltip()`, `helpTopic()` — the icon gif is right there, so the
palette icon lands in the page for free), and `Element.savedAttributes()`'s `Attribute`
declarations (`src/jls/elem/Attribute.java`, the issue-#23 single-declaration registry).
The human writes the *why*; the build writes the *what*. AC-2 then evaporates: there is
nothing to assert because there is nothing that can disagree.

This has a cost the issue should own rather than dodge: `ElementType` is deliberately thin
today (tag, aliases, class, factory — 130 lines, and its javadoc explicitly excludes GUI
concerns), and `Attribute` carries a save name and accessors but no display label, unit, or
range. The fact block needs a modest descriptor extension. That extension is not a
documentation cost — it is the payoff-heaviest core change on the board:

- it retires items 14/15 of ARCHITECTURE.md's "sixteen places" list for a new element;
- it feeds the HDL cell map (#61) and dialog generation (`ElementFormDialog`, #26) from the
  same table;
- it is *literally* revisit trigger (b) of the recorded i18n non-goal ("the element-registry
  work (#78) centralizing element metadata to the point that string externalization becomes
  cheap as a side effect") — and a hosted manual is exactly when a second language stops
  being hypothetical.

A documentation capstone that lands element metadata strengthens five other arcs. A
documentation capstone that lands a prose-parsing test strengthens none.

## Reframing 2: most "screenshots" should be `-i` exports, not captures

PF-3 buys a scripted capture pipeline on the headless-sway rig and, as the verification
comment concedes, inherits #101's two live weaknesses (`PIXEL_DIFF_MIN="0"`, the
fail-open JBR download) plus an unrecorded prerequisite on #91 for anything needing
interaction scripting. That is a large, flaky dependency chain.

Now look at the actual imagery need. `resources/help/**` contains 83 HTML pages and exactly
ten images — nine toolbar gifs and `keypad.jpg`. README has zero screenshots. The
repository has two raster images outside the icon set. The set PF-3 exists to regenerate
does not exist yet, so AC-3 ("screenshots are build products, not hand-pasted files") is
asserting a property over the empty set; the real work is *creating* the figures, which is
CAP-27/#73 territory.

And most figures a logic-simulator manual wants are **circuits, not chrome** — "here is a
4-bit adder", "here is this state machine". JLS can already emit those headlessly and
deterministically: `-i` exports PNG/JPEG/**SVG** from a `.jls` file with no display at all
(`FLAGS` row for `-i`; `CliImageExportTest`). So:

- **Figures** = `.jls` fixtures under a `docs/figures/` tree, rendered by the existing CLI
  in a Maven goal. Deterministic, no rig, no flake, SVG-crisp on the hosted site, PNG for
  the in-jar viewer — and every figure doubles as a file the student can open and simulate.
  This is the 80%.
- **Chrome shots** (toolbar, a dialog, the trace window, store listings) = the sway rig, a
  handful of images, genuinely dependent on #101/#91. This is the 20%.

Split PF-3 that way and the majority of the imagery lands with no dependency on an open,
fail-open CI lane. The issue never considers that JLS is its own best figure generator.

## Reframing 3: "one source" that still leaves two documentation systems

The title claims one documentation source. After all four PFs land, JLS still has two:
`docs/*.md` (25 normative contract documents with code anchors, web-readable on GitHub)
and the help tree (student-facing manual). Worse, AC-2 names "the batch-interface guide" as
required content of the hosted manual — so the plan publishes a *second* rendering of
`docs/batch-interface.md`, a document that is explicitly a stability contract, into a second
location under a second URL. That is a fork of a normative document created by the capstone
whose stated purpose is abolishing forks.

The reframing: the hosted manual has two sections from the start — **Manual** (from the
migrated help tree) and **Contracts** (`docs/*.md`, published verbatim, same anchors,
canonical-linked back to the repo path). One site, two content roots, zero copies. This
also answers what "versioned per release" should mean: the contract documents are the part
where `/5.0.x/` URLs actually matter, because those are the ones scripts and course
materials cite.

## Reframing 4: price the viewer swap up front, not as a kill criterion

KC-35-1 correctly says the viewer should upgrade rather than the source degrade — but
frames it as an escape hatch. #584's AC-4 meanwhile commits to writing down and
build-enforcing a "viewer-safe subset", which is a permanent tax on every future page paid
to keep `JEditorPane`'s HTML 3.2 renderer (`src/jls/Help.java:209`). Evaluate the inversion
in PF-1, not after: **bundle the generated static site in the jar and have Help open it in
the desktop browser** (`java.awt.Desktop` — currently unused anywhere in `src/`, so this is
new capability, not a refactor), with the existing Swing pane as the offline/no-browser
fallback. If that holds, PF-1 emits *one* target instead of two, AC-4's subset discipline
disappears, and the offline lab machine gets a better manual than it has today rather than
an unchanged one. If it does not hold (kiosked lab images, no browser), you have priced it
and kept the subset with reasons. Either way this is a decision, not a contingency.

## Where the arc is already ahead of the issue

Two stale premises worth correcting in the body: ARCHITECTURE.md's "There is no element
registry yet — issue #78 will introduce one" is stale against HEAD (`jls.elem.ElementRegistry`,
`jls.elem.ElementType`, `jls.edit.Palette`/`PaletteEntry` all exist, and `HelpTopicsTest`
already derives palette coverage from `Palette.entries()` rather than a hand list). The
generative route in Reframing 1 is therefore closer than the issue assumes. And the
`related` row's framing of #70 as "the seed of the drift-proofing discipline" undersells it:
the discipline has already shipped three ratchets and derives one of them from the registry.
JLS is not catching up to Digital on drift-proofing — it is one architectural move behind on
*generation*, and that move is the capstone.

## Summary of the endorsement

Keep the capstone; it owns a genuinely orphaned decision and the outcome statement is right.
Re-cut it as: (a) doc-side flag ratchet now, unsequenced; (b) element reference generated
from an extended registry descriptor, deleting #587's AC-2 rather than implementing it;
(c) figures from `-i`, chrome shots from the rig, as two separate slices; (d) one site with
Manual and Contracts roots; (e) the browser-first viewer priced in PF-1 rather than parked
in KC-35-1.
