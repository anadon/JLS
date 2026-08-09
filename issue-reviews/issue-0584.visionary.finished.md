# Issue #584: FEAT-C35-1: one plain-text source tree builds both the in-jar help and a static site — today's content migrates mechanically and the offline jar stays complete
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this is actually for

CAP-35 (#519) states the end plainly: *documentation cannot silently drift from the
program it describes*, and it must reach both the offline lab machine and the web.
#584 is the plumbing under that. The plumbing is worth building — but the issue
picks its seams by analogy to a generic docs-site migration, and JLS is not a
generic project. Three facts about this repository's trajectory should drive the
design and currently do not appear anywhere in the issue:

1. **JLS already has machine-readable sources of truth for most of its reference
   content.** `jls.elem.ElementRegistry`/`ElementType` and `jls.edit.Palette`/
   `PaletteEntry` (#78) carry tag, class, group, tooltip and `helpTopic`;
   `JLSStart.FLAGS` is the authoritative flag list (#71, `CliFlagTableTest`);
   `EditOp` is the authoritative accelerator table (#75,
   `HotkeysHelpAccuracyTest`). ARCHITECTURE.md's "adding an element" checklist
   already lists five hand-synced sites (element page, `Map.jhm`, `JLSHelpTOC.xml`,
   palette row, test).
2. **The jar is byte-reproducible and that is a headline property** (README,
   `docs/reproducibility.md`, CI rebuild check). Generated pages ship *inside* it.
3. **The build floor is "nothing beyond Maven and a JDK"** (README, "Optional
   development tools" is explicit that everything else is optional).

Measured against those, the corpus itself is small: 83 HTML pages, 16,356 words,
10 images, one `<table>` in the entire tree, zero non-ASCII bytes. This is not a
manual that needs an industrial static-site generator. It is a manual that needs
to stop being hand-maintained in three places at once.

## Reframing 1 — emit one tree, not two

The issue's spine is "one source → two targets," and then AC-2, AC-4 and AC-5,
plus #585 AC-3 and #587 AC-4, are all spent policing the gap between the two
targets. Cut the seam differently: **emit one site tree and ship it verbatim in
the jar.** The jar's `/help/**` *is* the published directory; #585 publishes the
same bytes under `/5.0.x/`.

What that collapses:
- #585 AC-3 ("hosted URL derivable from the in-jar topic id") becomes string
  concatenation over an identical path space, not a derivation rule that can rot.
- #587 AC-4 ("a claim cannot be true in one target and false in the other")
  becomes unrepresentable rather than asserted.
- AC-4's "viewer-safe subset" stops being a divergent profile and becomes the
  site's *baseline*: the pages are HTML the `JEditorPane` renders, and the web
  presentation (nav chrome, #585's search) is a progressive-enhancement layer the
  Swing viewer ignores. Verify the `HTMLEditorKit` treatment of a relative
  `<link rel=stylesheet>` early — it resolves inside the jar, so offline
  completeness is safe either way, but if it renders badly the enhancement layer
  moves to a path the viewer never loads.

Cost: the jar grows by the site chrome (help is 488 K today; chrome is
kilobytes). That is a good trade for deleting an entire class of cross-target
drift from three downstream issues.

## Reframing 2 — generate the reference half from the program; don't author it and then test it

This is the reframing that changes what #584 should deliver. #587 exists only
because docs and code are two authorings that must be reconciled. A pipeline is
strictly stronger than a ratchet: **you cannot drift from a source you are
generated from.**

Concretely, the source tree should not be prose-only. Each element page carries
front matter binding it to a registry descriptor (`element: AND`), and the
generator emits the ports/parameters/group table from `ElementType`/`PaletteEntry`
while the author writes only the narrative fragment. Same for the hotkey table
(from `EditOp` + `MenuAcceleratorPolicy`) and the flag reference (from `FLAGS`).
And then the two files that are pure hand-maintained sync today —
`resources/help/Map.jhm` and `resources/help/JLSHelpTOC.xml` — become *build
products* of front matter plus tree order, not authored files. That removes two
of the five hand-synced sites in ARCHITECTURE.md's element checklist.

`HelpTopicsTest` keeps working unchanged (it reads the classpath, so it does not
care that the tree is generated) — AC-2 is exactly right and should be kept
verbatim. But #587's AC-2 ("each page's documented ports and parameters checked
against the registry descriptor") should be re-scoped once this lands: there is
nothing left to check when the table is emitted from the descriptor. Say so in
#584's boundary section rather than letting #587 build an assertion layer over
generated content.

## Reframing 3 — do not create a fourth documentation home

ARCHITECTURE.md's "Help delivery" decision already partitions: `docs/*.md` and
README are the normative, web-readable home for contracts; in-jar help is the
student manual. #584 adds a *fourth* tree, and its hosted output will sit beside
`docs/` on the web with overlapping content — `resources/help/simulator/batch/
overview.html` and `docs/batch-interface.md` §1–3 describe the same watched-element
output today, in two voices, with only one of them normative and neither aware of
the other. That is the drift problem CAP-35 exists to kill, reintroduced by the
fix.

The elegant version: **the single source tree is `docs/` extended**, and each page
declares `in-jar: true|false` in front matter. The jar takes the student-manual
subset; the site publishes everything, specs included. #585 gets the normative
specs hosted and versioned for free — a strictly better hosted manual than the
help tree alone — and there is exactly one plain-text corpus in the repository.
If that is rejected, #584 must still amend ARCHITECTURE.md to say which tree owns
which claim, because the current text ("help content stays plain HTML 3.2 with
relative links", "hosted docs are the planned future") becomes false the day this
lands. Landing the pipeline without that amendment leaves the project's own map
lying about the project — the precise failure CAP-35 names.

## Constraints the issue never names, and should

- **Determinism is a hard requirement, not a nicety.** Generated pages enter a
  byte-reproducible jar. Sorted iteration everywhere (no `HashMap` order in the
  TOC or `Map.jhm` emitters), no build timestamps, no host paths, fixed locale.
  This deserves an AC and arguably a kill criterion; the existing CI rebuild check
  is the enforcement, free.
- **Toolchain floor.** MkDocs, Antora, Hugo and Docusaurus all break "Maven and a
  JDK." AC-1 says `mvn`-reachable, which is consistent, but the obvious
  implementations are not. Practically this means a small in-repo generator module
  over `commonmark-java`/flexmark at build scope — never a runtime dependency, so
  jar self-containment and `bom.json` stay unchanged.
- **Encoding.** Today's pages are pure ASCII with no charset declaration, and both
  `Help.readResource` and `HelpTopicsTest` decode ISO-8859-1. A UTF-8 generator
  that emits one curly quote mojibakes silently in the viewer. The AC-4 subset
  must pin the encoding, and the enforcement should simply fail on any byte above
  0x7F until the viewer path is proven.
- **Format choice.** For 83 pages, one table, no non-ASCII: Markdown plus front
  matter plus a minimal include mechanism is enough; AsciiDoc's admonitions and
  cross-reference validation are the only real pull. Record the decision as a
  section in ARCHITECTURE.md, which is where this project records decisions, not a
  new `docs/` file.

## Disregarding AC-5 as written

AC-5 ("byte-auditable — a diff report versus what ships today, every intentional
difference named") is the criterion pulling hardest against the outcome.
Generated HTML differs from hand-written HTML on nearly every line: attribute
quoting, `<p>` closing, the 36 pages of `<font color=yellow>`, whitespace. Naming
every difference across 83 pages becomes either a week of clerical work or a
rubber stamp, and neither protects a student.

Replace it with a stronger and actually checkable invariant: **normalized-content
equivalence** — per page, tag-stripped and whitespace-collapsed text must match
byte-for-byte; the link graph must be identical; the topic-id → page mapping must
be identical (the boundary comment on this issue is right that topic-id stability
is #585's real dependency, and this states it directly). Differences live in a
committed waiver list with a reason each. That catches the failure that matters —
content silently lost in migration — and ignores the one that does not.

## The alternative I searched and rejected, but which should be priced

KC-35-1 says that if the in-jar renderer's subset squeezes the source, the priced
answer is upgrading the viewer (a bundled lightweight renderer). Before buying
Flying Saucer-class embedding, price the cheaper move: **`Desktop.browse` at the
bundled `file:` URL.** The jar already carries the whole site; extracting it to a
temp directory and opening the system browser gets modern rendering, search and
real typography for free, at roughly zero dependency cost. I do not propose it as
the *default* — the version-locked offline manual on a kiosk-locked lab machine is
exactly the deployment CAP-35 promises not to regress — but it is the natural
implementation of the "open in browser" cross-link #585 wants anyway, and it is
the correct first answer to KC-35-1, not an embedded renderer.

## Net

Endorse the feature; keep AC-1, AC-2 and AC-3 as written. Reframe the target
model to one emitted tree, admit the registries as first-class doc inputs so
#587 shrinks rather than grows, decide explicitly whether `docs/` is inside or
outside the source tree, add determinism and encoding to the subset contract, and
replace AC-5's byte audit with a normalized-content invariant. Done that way this
issue does not merely modernize the help build — it removes two hand-synced files
from the element-authoring checklist and makes a whole downstream issue smaller.
