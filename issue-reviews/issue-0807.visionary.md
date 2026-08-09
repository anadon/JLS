# Issue #807: TASK-C594-1: typing a part's name finds it — incremental palette search over names and aliases, with a no-match that explains itself
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

A student knows the word for the thing and cannot find the button. That is a real,
cited, unglamorous defect (Logisim-Evolution #1234), and closing it is squarely on
the project's arc: JLS's whole differentiator is pedagogy-first ergonomics measured
against incumbents (#592's catalog, #521 CAP-37). The Outcome paragraph is right and
the "no-match explains itself" clause is the best sentence in the issue.

Two of the five acceptance criteria, however, name the wrong source of truth, and one
of them will actively obstruct work the project has already committed to. I am
disregarding AC-1's "alias" and AC-3's "built from the element registry" as written,
and I say why below.

## Finding 1 — the "alias" AC-1 asks for does not exist, and the one that does is a different animal

`ElementType.aliases()` (`/home/user/JLS/src/jls/elem/ElementType.java:36,103`) is a
**save-format compatibility** mechanism from #79: historical `ELEMENT <tag>` tokens
kept loadable across a class rename. Its javadoc is explicit that the GUI half is
elsewhere — "GUI concerns — palette icon, category, help topic, creation dialog —
belong to a separate GUI-side palette entry and never appear here" (lines 18–22).

And today **every one of the 35 registered types declares zero aliases**
(`/home/user/JLS/src/jls/elem/ElementRegistry.java:38-77` — no varargs argument on any
row). So AC-1 is either unsatisfiable from the registry, or satisfiable only by
inventing user-facing synonyms on `ElementType` — which puts UI vocabulary into a
descriptor that also feeds the loader and, per #482 H4/O5, the collaboration
allowlist (`ElementVocabulary` becomes registry-minus-deny). Search synonyms should
not be one refactor away from being network-acceptable tokens.

The vocabulary that actually answers a student's query already exists, in three
places, none of which is the registry:

- `PaletteEntry.tooltip` — the human name ("multiplexor", "unbundle wires",
  "sign- or zero-extend a field to a wider bus")
  (`/home/user/JLS/src/jls/edit/Palette.java:123-188`);
- `PaletteEntry.fallbackText` — the short label ("1-to-N", "ST. MAC.");
- `PaletteEntry.helpTopic` → a real prose page under `resources/help/elements/**`,
  already link-checked and completeness-tested by `HelpTopicsTest`.

Note what this predicts: a student typing **"multiplexer"** misses the tag (`Mux`),
misses the tooltip ("multiplex**or**"), and hits only the help prose. The help corpus
is the strongest index JLS already owns, and indexing it costs no new hand-maintained
table — which is exactly what AC-1's "rather than a hand-maintained list" is reaching
for and pointing in the wrong direction.

## Finding 2 — AC-3 pulls directly against #482, and will land as a test that blocks it

AC-3: "the search index is built from the element registry, so a newly registered
type is findable without editing the search code."

#482 (TASK-0105) exists to **break** exactly that equation on the palette axis. Its
whole premise is that a currently-green assertion — one palette entry per registered
type — would force 22 analog device types into the first-year toolbar (69% growth),
and that K9 must gate *visibility*, not existence. A registry-sourced search index
reinstates "registered ⇒ reachable by a first-year" through a side door: a CS-101
student types "op" and finds an op-amp that #482 spent a whole task hiding. The same
applies to #401's breadboard parts, #453's N-ary types, and #361's ternary family.

Worse, AC-3 will be encoded as a test ("a newly registered type is findable without
editing the search code"), and that test then becomes an obstacle to #482 in precisely
the shape #482 documents as its own hardest problem (its O2: the blocker is a *passing*
assertion, not missing code). Landing #807 before #482 manufactures a second one.

The correct source is `Palette.entries(view)` filtered by #482's `vis(view, context)`
predicate — the view-scoped palette table, which is already the total, authored,
GUI-side projection of the registry. Neither #482 nor #277 (registry-driven dispatch
— the seam this index should read through) appears in this task's `ordering_after`,
and both belong there ahead of #803.

## Finding 3 — the cheapest index is no index

AC-4 worries that "index construction does not move the startup cost ratchet." That
worry is a tell that the design imagined something bigger than the data. The corpus is
**32 palette rows of short strings**, immutable, built at class-init. Substring or
prefix matching over 32 tooltips per keystroke is free; there is nothing to construct,
so there is nothing to ratchet. Adding the help-page prose changes this only if it is
parsed eagerly — index it lazily on first query (a student who never searches pays
nothing) and AC-4 is satisfied by construction rather than by measurement.

Also unresolved in the issue: AC-1 wants a field that updates as you type, AC-4 forbids
new default-visible chrome. A permanently-visible search box violates AC-4; the issue
never says where the field lives. That contradiction is design pressure toward the
reframing below, not a detail to settle in review.

## Alternative framing A (recommended) — a findability surface, not a palette widget

Build one hidden, keystroke-summoned "what do you want?" overlay over three corpora at
once: **placeable parts** (view-scoped palette entries), **commands** (#75's shipped
shared `EditOp`/`Action` layer), and **help topics** (`Map.jhm`, 88 entries). Type a
word; matching parts, commands and topics rank together; Enter places the part,
invokes the action, or opens the page.

Why this is the better goal, not just a bigger one:

- It subsumes #808 (recently-used, keyboard palette navigation) almost entirely: a
  recency-ranked, keyboard-only surface *is* a recently-used set reachable by keyboard,
  without a second focus model or a second persisted list.
- It closes a gap ARCHITECTURE.md records but no issue owns: in-jar help has **no
  search**, and "searchability" is named there as a reason to move help to the web
  someday. This makes the offline manual searchable today, for free.
- It is the discoverability answer to #482's hidden views: parts a context legitimately
  permits become reachable by name even when their group is not on the toolbar, using
  #482's own `vis` predicate rather than a parallel visibility rule.
- It honours K9 *better* than the issue does: a surface behind a keystroke adds zero
  default-visible chrome, where a docked search field adds some.
- It respects the hard gate (KC-37-1) more naturally. Reading the boot
  `ExtensionRegistry` snapshot (#277) and #75's `Action` layer, it needs one key binding
  and one popup from `SimpleEditor` — which is 5,852 lines at HEAD, up from the ~4k
  ARCHITECTURE.md still records. A search field welded into `makeElements`' toolbar loop
  is growth in exactly the wrong place.

The component-name scheme (`docs/component-naming.md`: `palette.<slug>`,
`menu.elements.<slug>`) already gives the #91 harness a stable handle to assert results
headlessly, so AC-5's "test that fails at the pre-change commit" is no harder here.

## Alternative framing B (if the fleet wants the small version)

Ship no new widget at all: with nothing selected, typing filters the toolbar — matching
buttons stay lit, others dim, the status line carries the query and the no-match text.
Zero chrome, zero index, zero new persisted state, and it lands as a pure function over
`Palette.entries(view)` plus one editor hook. This satisfies the Outcome sentence
completely and defers every architectural commitment until #482 and #316 have landed.

## The no-match message deserves more ambition than "say the query"

JLS *has* a flip-flop — it is `Register` with triggering options. Telling a first-year
"no part matches 'flip flop'" is a correct dead end and a pedagogical failure. The
valuable version is a redirect: *"JLS has no part called 'flip flop' — a clocked storage
element is **Register**, and edge vs. level triggering is a Register option."* Likewise
demux → Decoder, "7 segment" → Display, "wire label" → JumpStart/JumpEnd, "sign extend"
→ FieldExtend, buffer → TriState.

That curated table *is* the alias vocabulary AC-1 wants — but it is teaching material,
so its home is beside the help topics or on the GUI-half `PaletteEntry`, never on
`ElementType`. Ten hand-written redirects are worth more to a student than any amount of
fuzzy matching, and they are the part of this task that no incumbent has.

## Alignment summary

Strengthens the arc: yes — findability is core to CAP-37 and to JLS's pedagogy claim.
Duplicates: partially, with #808, which the framing-A surface would largely absorb.
Pulls against: AC-3 vs. #482 is a genuine conflict, not a nuance, and it is the one
thing in this issue that must change before code is written.

Concretely: keep the Outcome and AC-2; rewrite AC-1 to index tooltips, labels, help
prose and a curated redirect table; rewrite AC-3 to source from the view-scoped palette
via #277's snapshot; add #482 and #277 to `ordering_after`; keep AC-4 and AC-5.
