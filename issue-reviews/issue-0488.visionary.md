# Issue #488: JLS writes two element tags — FieldExtend and RegisterFile — that its own frozen tag table and its own normative file-format spec both say do not exist
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the two-line diff, #488 is a claim about what JLS should be: **a simulator
whose normative file-format specification is true, and whose per-element facts cannot
silently disagree with each other.** Both halves of that are squarely on the project's
arc. `docs/file-format.md` is advertised in README as normative, the container image
exists so autograders and third-party tooling consume JLS output, and §7's own preamble
(`docs/file-format.md:286-290`) instructs a conformant reader to *reject* an undocumented
tag. A spec that is wrong by two rows is a real defect against a real audience. Endorsed
as a goal, without reservation.

The reframing is about the second half — the durable machinery. The issue's remedy is
"add the missing rows to the second copy, add the missing rows to the third copy, and add
a fourth assertion pinning copy two to copy one." That is the correct move if
`SaveTags.WRITABLE` is a thing JLS keeps. The project has already decided it is not.

## The alignment test

`docs/grand-architecture.md` names `ElementRegistry`/`ElementType` as one of three
"enabling triad" seams and states the destination plainly (`:293-302`):

> **The registry is the mechanism, generalized.** `ElementType` already records a
> deliberate *two-layer descriptor* split — a headless core half
> (tag/class/factory/aliases) and a separate GUI-side palette entry … #78's registry is
> not merely *like* the plugin system; it *is* its first instance, and #212 is the same
> instance opened to external providers.

Under that destination `SaveTags.WRITABLE` cannot survive. It is a `Map.ofEntries`
literal in `jls.elem`, compiled shut (`src/jls/elem/SaveTags.java:41-74`). An element
type contributed by an external provider can never acquire a row in it. #488 is
therefore repairing a table the architecture has already scheduled for demolition, and
— worse — installing a permanent test that welds the condemned table to the survivor.
#375 then makes it a permanent subclass of `RegistryTotalityTestBase`, at which point
the redundancy has a maintenance contract around it.

The measurement in O6 is the tell, and the issue reports it without drawing the
conclusion: `SaveTags` has **zero production callers**. I re-derived it — the only
non-javadoc references outside its own file are `FileFormatSpecTest`, `SaveTagsTest`, and
a path string in `CollabSecurityRatchetTest:121`. `Circuit.load` routes through
`ElementRegistry.forTag` (`src/jls/Circuit.java:918`). A "mirror" that no production code
reads and no test diffed against the registry is not a mirror; it is a second source of
truth that happened to drift. #79's design intent was sound in 2026-07 and has been
overtaken by #78 shipping.

## How many copies of "the set of element tags" exist

Counting what I found in the tree, the fact "which tags does JLS write" is stored:

1. `ElementRegistry.ALL` — live, read by the loader.
2. `SaveTags.WRITABLE` + `LOADABLE_ONLY` — dead, tests only.
3. `docs/file-format.md` §7 table rows.
4. §7's sentence "exactly these **32** tags" — a hand-maintained integer.
5. 26 literal `output.println("ELEMENT X")` sites, plus `Gate.Kind.saveName` for the
   eight `Gate` subclasses (`src/jls/elem/Gate.java:75-76`, `:397`), whose comment reads
   "a frozen tag: must match SaveTags / spec §7" — enforced by nothing.
6. `FileFormatSpecTest`'s hand-written full-coverage fixture.
7. `ElementRenderers.BY_TYPE`, `ElementDialogs.BY_TYPE`, `Palette`, `ElementVocabulary`,
   `HdlExporter`'s policy (#372, #375, #492).
8. `ARCHITECTURE.md`, which is stale in exactly this way *right now*: `:93` says
   "`ELEMENT Foo` resolves via `SaveTags.resolve(tag)`" (false since #78), and `:115-117`
   says "There is no element registry yet — issue #78 will introduce one" while
   `ElementRegistry` is shipped and in the load path.

#488 repairs copies 2, 3 and 4, adds one assertion on the (1,2) edge, and leaves 5, 6, 7,
8 as they are. It also *adds* drift sources: §7.6 puts each element's attribute list in
the new notes cells, and §11 admits "if either element's attribute set changes, the doc
rows go stale with nothing checking them." A task whose thesis is "hand-maintained copies
drift" should not ship two new hand-maintained copies.

## Three concrete alternatives, cheapest first

**A. Delete the count sentence instead of bumping it.** Change `docs/file-format.md:291`
to "Version-1 and version-2 writers emit exactly the tags in this table:". No information
is lost — the table below it carries the count — and one drift source disappears
permanently. The issue instead hardcodes `34`, guaranteeing that the thirty-sixth element
type re-breaks it, and its own P4 concedes no test reads the sentence (it is a human
`grep` in the DoD, and §14 says so explicitly: "only the rows are checked by a test"). This
is a one-line change that strictly dominates the proposed one-word change.

**B. Absorb `SaveTags` into `ElementType`; the assertion becomes vacuous.** Give
`ElementType` a `writable` flag (default true, false for `TestGen`) and, if the alias
table ever gains an entry, keep aliases where they already live (`ElementType.aliases()`
— the mechanism exists and is empty in both tables). `SaveTags.resolve` /
`writableTags()` / `loadableOnlyTags()` become three-line delegates over the registry, or
vanish, since nothing in `src/` calls them. Then:

- `FileFormatSpecTest.specTagTableAndCodeTagTableAgree` (`:337-341`) keeps working
  unchanged in spirit — it compares §7 to the registry instead of to a shadow of it, and
  becomes the *only* place the spec is pinned, which is where the pin belongs.
- The issue's new `everyWritableRegisteredTagIsInTheFrozenTagTable` is not written,
  because it would compare the registry to itself. Making a test unnecessary is a
  stronger fix than making it pass.
- The `TestGen` exemption stops being a `private static final Set` in a test file that
  the author of the thirty-sixth element type will never open (§7.5), and becomes a
  constructor argument sitting on the line next to `TestGen` in the registry — visible at
  exactly the moment it matters.
- #375's base class loses a subclass instead of gaining one.

This is a pure test-and-doc refactor: `git grep 'SaveTags\.' -- src/` returns nothing, so
no production behaviour is at risk, and H3's byte-identical-save claim holds trivially.

**C. Make the writer derive its tag from the descriptor — the seam that dissolves the
family.** The authority on "what tag JLS writes" is `save()`, and today nothing connects
it to any table. Add `ElementRegistry.tagOf(Element)` and a `protected final
writeTagLine(PrintWriter)` on `Element`; replace the 26 literals and `Gate.Kind.saveName`
with it. After that, emitting a tag no table knows about is not a defect the build
catches — it is unrepresentable. Note that #488's proposed test does *not* close this
edge: a typo in a new `Gate.Kind.saveName` leaves the registry, `SaveTags` and §7 mutually
consistent and the writer wrong, caught only if someone remembered to extend the
hand-written fixture. This is the same shape as #492 (HDL export policy) and #372 (GUI
tables): a per-type fact stored away from the type. Cutting at this seam is what makes
#315's whole residual smaller rather than better-tested.

Incidental confirmation of H2, mechanically rather than from prose: 26 literal `ELEMENT`
sites + 8 `Gate` subclasses = 34 = 35 registered − `TestGen`. H2 holds; the issue's Open
Question 1 can be closed with option (b)'s derivation for free.

## What I would disregard from the acceptance criteria, and why

I am explicitly setting aside the §14 items that pin the durable half to `SaveTags`:
"`SaveTags.WRITABLE` contains 34 entries", the `NON_WRITABLE_TAGS`-in-a-test-file design
of §7.5, and P6's mutation check on `SaveTags` rows. Every one of them is effort spent
making a condemned table trustworthy. Also set aside: bumping the count sentence to 34
(alternative A replaces it). I would keep, unchanged: the two §7 rows, the `FORMAT`
no-bump decision (Open Question 2, correctly answered "no"), the fixture extension in
`FileFormatSpecTest`, and the §11 warning that the fixture is not a general guard.

Also worth landing alongside, since it is the same defect in the same document family and
costs two lines: `ARCHITECTURE.md:93` and `:115-117` are currently false about the loader
and about whether a registry exists. Leaving them while fixing §7 is fixing the copy that
was measured and not the copy that was not.

## Verdict

**endorse-with-reframing.** The goal is right and the two missing §7 rows should land
this week — the harm to third-party readers is real and the fix is two lines of data. But
the durable half is aimed at the wrong artifact. Land the rows plus alternative A
(delete the count sentence) as the defect fix; then do alternative B as its own small
task — absorb `SaveTags` into `ElementType`, delete the shadow table, and let
`FileFormatSpecTest` pin §7 against the registry directly. Open Question 3 ("should
`SaveTags` gain a production caller, or be deleted?") is not a rider to be noted and
deferred; against `docs/grand-architecture.md` it is the actual question this issue
found, and the answer is delete. Alternative C is the larger prize and belongs under
#315 next to #492, where it retires that residual instead of instrumenting it.
