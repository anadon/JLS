# Issue #482: TASK-0105: the palette gains a view dimension, so a registered element type stops meaning a first-year toolbar button — enforced by a currently-passing test that must be rewritten
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Stripped of the palette vocabulary, #482 is asking for one thing the project does not
have: **a way for the element registry to hold a type that a given editing context may
not create.** Today registration and offerability are the same fact — `Palette.entry`
refuses an unregistered tag (`src/jls/edit/Palette.java:214-225`) and
`PaletteContractTest.paletteIsTotalOverTheElementRegistry` refuses a registered tag
with no row (`test/jls/edit/PaletteContractTest.java:47-66`). The issue is right that
this is a gate held shut by a green test, right that it must open before the first
analog type is registered, and right that a preference is the wrong key. That much I
endorse without reservation.

Where I part company is the shape of the key. The issue makes the new fact a
**presentation attribute of a button** — `String view` on `PaletteEntry`, a second copy
on `Palette.Group`, a hardcoded `vis(view, context)` switch — on the stated ground that
"a view is a presentation fact and the registry is not a presentation table" (§1). But
read the issue's own justification for the work: *"the analog region already refuses
`Clock`, `Stop`, `Pause`, `Display` and `SigGen` inside a bound region; a per-view
palette stops offering buttons the model would reject."* That is not a presentation
fact. That is a **legality** fact, and it already exists in the model — twice over,
once as the region rule and once, in #331's residual, as a **fourth sealed permit on
`Element`** beside `DisplayElement`, `LogicElement` and `Wire`. The issue is proposing
to hand-author a third representation of something the type system is about to encode.

## The reframing: admissibility on the core descriptor, not a view on the GUI half

Give `ElementType` a small closed classification — call it a domain (`DIGITAL`,
`ANALOG`, `PHYSICAL`) and a creation mode (`PALETTE`, `IMPLICIT`, `IMPORTED`,
`BATCH_ONLY`) — with no default, so a new type cannot be registered without answering
both. Then the palette is a *derivation*, not a table with a new column:

    entriesFor(context) = ENTRIES.filter(e -> admits(context, domainOf(e.type())))

Six things the issue spends two weeks on stop existing:

1. **H1 and P3 (cross-view uniqueness) become unstatable rather than asserted.** "A
   typo puts a gate in two views" is a failure mode of a hand-copied string. A type has
   one domain the way it has one tag; there is no second place to write it wrong. The
   issue's most-defended invariant is an artifact of the encoding it chose.
2. **The #383 dependency dissolves.** #383 is a *file-format and op-grammar* task
   sitting behind #319, #337 and two unfiled prerequisites (TASK-0033's section frame,
   TASK-0035's `ItemKey`). #482 needs no persisted view token and no op discriminator —
   only the domain of a type and the domain of the current editing context. Gating the
   palette ratchet on that chain is a scheduling error, and it directly threatens the
   issue's own best instruction ("ship this before the first analog element"). Under the
   reframing #482 can land next week; as written it cannot land until five other things do.
3. **T2 ("a second view vocabulary") stops being a threat requiring discipline.** The
   palette holds no view token at all, so there is nothing to mint twice.
4. **Open Question 3 evaporates.** No views are "declared empty", so P5
   (`everyDeclaredViewHasAtLeastOneGroupOrIsExplicitlyEmpty`) — a test that exists only
   to police a declaration — is never written.
5. **`vis` and the region's placement rule become one predicate.** As written they are
   two rules that must agree: the toolbar hides analog devices, and the region
   separately rejects `Clock`. Two rules that must agree are a defect generator; the
   issue notices the coincidence and does not collapse it.
6. **The deny list (Open Question 2, H4, P9) is answered by construction.** `TestGen`
   is not "denied"; it is `BATCH_ONLY`. `SubCircuit` is `IMPORTED`, `WireEnd` is
   `IMPLICIT`. Those are three different reasons currently collapsed into one opaque
   `NON_PALETTE_TAGS` set, and #482 proposes collapsing them again into a second opaque
   `DENY` set. Derived from creation mode, the network vocabulary is
   `registry.filter(mode != BATCH_ONLY)` — the exclusion is *reasoned*, the "34 before
   and after" arithmetic is a consequence rather than an assertion, and a future type
   cannot be silently admitted to the network surface because the field has no default.
   That is a materially better answer to O5's javadoc than either delegation option the
   issue considers.

## Two defects that fall out of the presentation framing

**The toolbar is not the disclosure surface.** `SimpleEditor.makeElement`
(`src/jls/edit/SimpleEditor.java:2400-2427`) builds a `JButton` **and** a mirror
`JMenuItem` into the `elements` menu from the same entry, and `HelpTopicsTest`
(`test/jls/HelpTopicsTest.java:167-183`) derives its required help topics from
`Palette.entries()`. §7.4 deliberately keeps `entries()` returning everything, and P7
inspects only the toolbar's component tree. As specified, 22 analog types land as 22
items in a first-year's `elements` menu, 22 mandatory pages in the student-facing help
TOC, and 22 contributions on `gui.palette-contributor` via
`src/jls/boot/GuiModule.java:42`. K9 says the first-year must never *see* the ECE/EE
machinery; a toolbar-only gate does not deliver that. A derived filter naturally
applies at every consumer of the palette, because every consumer asks the same question.

**The count ratchet is weaker than the contract it replaces.**
`defaultViewShowsExactlyThirtyTwoButtons()` fails on any change (T6 already schedules
its edit to 33 for #474) and passes for the wrong reason: swap a gate for a resistor and
the count is still 32. K9's own text chose the count only to avoid "re-litigating the
set" (`docs/virtual-hardware-parity.md` at `3a81a4a`), which was the right call when no
domain existed. With a domain, the set assertion is free and strictly stronger: the
default toolbar's tag set **equals** `{t : domain(t)=DIGITAL ∧ mode(t)=PALETTE}`. It
self-maintains across #474, and it still fails loudly if an analog type reaches the
default view. Given how carefully §11/T1 guards against weakening a contract during a
rewrite, shipping a count where a set is available is the one place the issue does the
thing it warns about.

## Alignment with the larger arc

The direction is right and is the project's own: #78's two-layer descriptor,
`docs/extension-points.md`'s "one catalog, cross-checked in both directions", the
registry that `ARCHITECTURE.md` promises will "collapse most of" the sixteen-step
element checklist. But count the hand-maintained set-difference relations over the same
35 tags today: `Palette.ENTRIES`, `NON_PALETTE_TAGS`, `KNOWN_MISSING_ICONS`,
`ElementVocabulary.ALLOWED`, `HelpTopicsTest`'s derived topic map. #482 adds a sixth
(view assignment) and a seventh (`DENY`) and spends most of its two weeks reconciling
them. That is motion *along* the project's arc in intent and *against* it in mechanism:
the arc is toward one authority with derived projections, and every new hand-authored
column makes #84 (routing palette construction through the contributor seam) and #316
(editor decomposition) harder, because both must now carry the extra column forward.

One honest caveat about the greater alignment: the whole analog programme sits in
tension with a recorded decision in this tree. `docs/capability-roadmap/README.md` §6(a)
places continuous-time and analog *out* of scope on "different tool class" grounds, and
`sweep-02-timing.md:726` says consuming that data "requires an analogue solver, which is
a different tool". The documents that reverse this (`docs/plan/evidence/BRIEF.md` D9,
`analog-determination.md` D-A10, `docs/virtual-hardware-parity.md` K9) exist only at
`3a81a4a`, on an unmerged branch — the issue is admirably explicit about that. That
tension is #331's to resolve, not #482's, and it is a further argument for the
reframing: a domain enum plus a derived filter is worth doing for the breadboard (#329),
the N-ary family (#361) and the batch-only/import distinctions *regardless* of whether
the analog programme ever ships. The `String view` column is worth doing only if it does.

## What I am disregarding, and what I am keeping

I am setting aside acceptance criteria P3, P5, P6, P9, P11 and Open Questions 1–3 as
written: they are all consequences of the view-token encoding, and under the domain
encoding they are either vacuous, unstatable, or answered by construction. I keep the
issue's three genuinely load-bearing commitments unchanged: **the rewrite must be
strictly stronger than the test it replaces** (achieved better by the set assertion),
**visibility is derived from the model and takes no `UserPrefs` argument** (achieved by
construction, since the predicate's inputs are a type and a context), and **this ships
before the first analog element type is registered** (achieved sooner, because the #383
chain is no longer in the way). Sections 9's audit discipline — old and new assertion
text side by side, allowlist size recorded before and after — should survive verbatim.
