# Issue #594: FEAT-C37-3: a user who knows what a part is called finds it by typing — palette search, recently-used and keyboard palette navigation close Logisim-Evolution's oldest findability complaint
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is three palette affordances. The actual end is one sentence
in the title: *a user who knows what a part is called finds it by typing*. Everything
downstream — the index, the MRU, the focus order — is machinery in service of that,
and the machinery is chosen by analogy to the incumbent's complaint (Logisim-Evolution
#1234 says "no component search", so JLS builds component search). That is a fine
first-order reading, but it inherits the incumbent's framing of the problem along with
its complaint. JLS's own trajectory suggests a larger and cheaper target.

## Where the trajectory already stands

Two facts change the shape of this work and neither is reflected in the issue body:

1. **The palette is already a table, not a hand-coded toolbar.** `jls.edit.Palette`
   (issue #78, PR #246) holds all ~30 entries; `PaletteEntry`
   (`/home/user/JLS/src/jls/edit/PaletteEntry.java`) carries display name, group,
   icon, and help topic; `ElementType` carries tag, aliases and factory;
   `SimpleEditor.makeElements` (line 2306) is a generated view over that table, and
   `GuiExtensionPoints.PALETTE_CONTRIBUTOR` (#223) already types the seam. A search
   index over this is a pure function of data that exists today.
2. **JLS already has a closed operation vocabulary.** `jls.edit.EditOp` plus the one
   shared `Action` per op (#75) is a command layer in everything but name, and
   `docs/extension-points.md` lists `app.command` as a *pending* seam owned by #84
   with #220's activation runtime. The project has already decided it wants a command
   vocabulary; it just has not built the surface that makes one visible.

## Reframing 1 (primary): build the omnibox, not a palette search box

The elegant route is not "search over the palette" but **one invoke-by-typing surface
over a vocabulary of *items*, of which palette elements are the first source**. One
keystroke opens a transient overlay; typing filters; Enter invokes. Item sources:

- element types (from `Palette` / `ElementRegistry`) → invoke = begin placement;
- editor operations (`EditOp`) → invoke = the same shared `Action` #75 built;
- open subcircuits (today's hand-coded Import button, the one toolbar control with no
  palette row) → invoke = import;
- help topics (`Map.jhm`, already keyed per element via `PaletteEntry.helpTopic`) →
  invoke = open the #11 viewer at that topic;
- later, module-contributed commands — i.e. this *is* the `app.command` seam, built
  once for a user-visible reason rather than speculatively.

Why this is strictly better rather than merely bigger:

- **It costs about the same.** The expensive parts — a deterministic matcher, a
  ranked result list, keyboard navigation, an empty-state message, a display test —
  are identical whether the item space is 30 elements or 30 elements plus 20 ops plus
  N subcircuits. The extra work is the item-source interface, which is ~30 lines.
- **It resolves the issue's own internal tension.** AC-1 wants incremental search over
  the palette; AC-5 forbids new default-visible chrome. A search field docked in the
  toolbar *is* new default-visible chrome. A transient overlay is invisible until
  invoked, so K9 holds by construction instead of by argument.
- **It gives recently-used something worth ranking.** With 30 icons all simultaneously
  visible on one toolbar, an MRU strip of elements is close to valueless and, if
  rendered as a persistent strip, is exactly the chrome AC-5 forbids. MRU over an
  *unbounded* item space (subcircuits, module elements from #212, commands) earns its
  keep immediately.
- **It stops the second-focus-model risk at the source.** AC-3's "reuse #75's shared
  `Action` layer rather than a parallel one" is the right instinct; an omnibox that
  invokes `Action`s makes reuse the only implementable design instead of a discipline
  clause someone must remember.
- **It unblocks help search for free.** `ARCHITECTURE.md` records that in-jar help is
  searchless today and hosted docs are the planned future. Indexing `Map.jhm` topics
  costs one item source and is the only searchability students get offline.

## Reframing 2: the real asset is a nomenclature map, not a matcher

A matcher over JLS's own display names still fails the switcher this capstone is
about. A Logisim user types "D flip-flop", "tunnel", "constant"; a Digital user types
"driver"; a CircuitVerse user types "LED". JLS's names are `Register`, `JumpStart`,
`Constant`, `TriState`, `Display`. **The vocabulary translation is the whole value;
the substring match is trivia.** That table — incumbent term → JLS type, cited to the
source tool — belongs in `docs/` (naturally in #592's catalog, which is already the
gate), is testable (every row must resolve to a registered `ElementType` tag), and
pays three times: it feeds the search index, it is publishable migration
documentation for switchers, and it is the beginning of the cell map the pending
HDL/Logisim importer seam (#61/#62) will need. Cut along that seam and search becomes
the *cheap* consumer of an asset with independent value.

Corollary, and a trap the issue walks into: **AC-1's "at least one alias form" must
not be satisfied by `ElementType.aliases()`.** Those are historical *save tags*, a
frozen file-format compatibility contract from #79. They are not user synonyms — none
of them is "inverter", "mux", or "flip-flop" — and overloading them makes a
format-stability surface answer to UX pressure. Synonyms belong on the GUI half
(`PaletteEntry`, or a sibling `searchTerms` list), pinned by `PaletteContractTest`.

## Where the issue pulls against the arc

- **The hard gate is aimed at the wrong dependency.** "If #316 stalls, this feature
  waits" reads as prudent, but #84's *residual* is only the nine-state mouse machine;
  palette construction was decomposed two PRs ago. A search index, an MRU model and a
  matcher touch none of the nine states. Waiting on #84 buys no architectural safety
  here and costs schedule. The honest gate is the invariant, not the issue number:
  *no new state, no new branch, in the interaction machine.*
- **There is exactly one genuine `SimpleEditor` coupling, and the issue does not name
  it.** `setup(Element, boolean fromToolBar)` at line 5358 chooses the drop point as
  the last mouse position, or the viewport centre when invoked from the toolbar, then
  branches on `getMousePosition()` for `chosen` vs `placing`. It never consults #75's
  keyboard caret. A keyboard user who arrows the caret into place, opens the search
  surface and picks "Adder" will get the adder at the viewport centre, not at the
  caret — a silent failure of AC-3's spirit that no accessible-name assertion catches.
  Fixing it means `setup` taking an explicit drop point, caret-first when the caret
  exists. That is a small, real change inside the god class; declare it rather than
  discover it. Likewise `setup`'s #207 mid-placement refusal must be the search
  surface's behaviour too, not a second story.

## Concrete shape I would build

- `jls.edit.CommandIndex` (or `Findable`/`FindItem`) — pure, Swing-free, headless, in
  the established pattern of `KeyboardConstructionPolicy` / `OptionMenuPolicy` /
  `DeleteKeyPolicy`: item records (id, display name, synonyms, category, invoke key)
  plus a **deterministic** ranking (exact > prefix > word-start > subsequence; ties
  broken by palette order, never by hash order). Determinism is what keeps the #91
  test from being flaky by design; the issue's AC-1 does not require it and should.
- Item sources contributed, not hard-coded, so #212's external elements and #220's
  lazy commands appear without touching the surface.
- **Build the index on first invoke, not at startup.** This satisfies AC-5's startup
  ratchet clause literally and for free, and is the only correct choice once
  contributions can arrive lazily.
- Tests: headless matcher/ranking/MRU tests plus one `display`-tagged test that opens
  the overlay through the real focus owner, types, and asserts the element lands *at
  the caret* — the assertion that actually pins AC-3.

## Verdict

**endorse-with-reframing.** The end is right and well-chosen: findability is the
switcher-facing gap, and JLS's data model is already shaped to close it. I am not
disregarding the acceptance criteria wholesale, but I would rewrite three of them:
AC-1's "over the palette" becomes "over an item vocabulary whose first source is the
palette" and its alias clause moves off `ElementType.aliases()` onto authored
synonyms sourced from #592's incumbent-nomenclature rows; AC-2's recently-used becomes
MRU over that vocabulary with no persistent visible strip; and the #316 gate is
restated as the invariant it is meant to protect so this feature is not parked behind
an unrelated refactor. Done this way, the issue stops being one more ergonomic patch
and becomes the surface that makes JLS's already-decided command architecture real.
