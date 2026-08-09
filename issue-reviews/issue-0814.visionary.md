# Issue #814: TASK-C168-1: the 64 SAS glyphs ship as bundled, licence-cleared images, and a missing or unlicensed glyph fails the build rather than the Verify dialog
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and the task is one sentence: *make the
second channel of the SAS comparison exist, and make its absence loud.* The
first channel — seven English words — already exists and is already correct
(`src/jls/collab/net/Sas.java:38-54`, frozen, index-addressed, phonetically
chosen). The words are never on the wire; both peers compute indices from the
transcript secret and render them locally. So everything this task adds is
**presentation**, and the security property survives its total absence — the
research doc says so in as many words: "comparison then works even rendered as
words alone" (`docs/collaborative-editing-research.md:369`).

That reframing matters because it tells you what the task may trade away. It
may not trade away distinguishability-at-a-glance between two screens. It may
absolutely trade away artistic fidelity, third-party sourcing, and the entire
licensing apparatus AC-3 builds — if a cheaper route reaches the same end.

## The route the issue chose, and why I would not take it

#168's Open Question offers two doors — adapt an existing openly licensed set,
or draw originals — and recommends the first. AC-3 then builds the machinery
that door requires: a provenance table, a per-image source column, a
GPLv3-compatibility assertion, and an escalation branch for "no compatible set
can be obtained." That is a lot of standing structure erected around an asset
decision, and it imports a permanent liability: 64 third-party binaries in a
repository whose supply-chain posture is one of its selling points (byte-
reproducible jar, CycloneDX BOM, attestations, Scorecard badge — `README.md`),
and whose own audit already records that **zero files in the tree carry an SPDX
identifier and the existing 76 GIFs have an open provenance question**
(`docs/standards-adoption/09-cra-and-supply-chain.md:405-406, 866`). Vendoring
64 more images with a bespoke sidecar manifest does not advance that arc; it
adds a second, incompatible provenance mechanism next to the REUSE work section
09 is already designing.

Worse, the practical path from "existing openly licensed set" to "bundled
image" is not clean. The good candidates (OpenMoji CC-BY-SA, Twemoji CC-BY,
Material Symbols Apache-2.0) are SVG. Swing cannot render SVG without Batik or
svgSalamander, which #168's invariant 6 forbids. So the set gets rasterised
once, by hand, at some size, with some baked-in colours — and now the project
owns a derivative work whose CC-BY attribution obligation must be *conveyed to
recipients*, not merely filed in-tree. `src/jls/About.java` has no third-party
notice surface and the jar has no NOTICE file. AC-3 as written would be
satisfied by a file that does not discharge the licence.

And the baked-in colours collide with the project's own trajectory:
`src/jls/Theme.java:1-45` records a colour-vision-safe Okabe-Ito default, a
CLASSIC alternate, and a dark variant explicitly deferred behind the ~126
hardcoded-foreground sweep of #76. Sixty-four fixed-palette rasters are 64 new
call sites that will fight that sweep the day it lands.

## The alternative the issue never considers: generate them

JLS has already solved "we need artwork we do not have," in this repository,
with a recorded pattern. `scripts/GenerateIcons.java` (issue #82) draws the
application icon programmatically in pure Java2D, writes PNG/ICO/ICNS
containers by hand so no external tool is needed, and checks the output in. Its
opening comment states the exact situation this task is in: "The project has no
standalone logo artwork... so the icon is a simple letterform rendered
programmatically."

Apply that pattern: `scripts/GenerateSasGlyphs.java` emits 64 PNGs from
primitives; provenance for every one is "original work, this repository";
AC-3's licence table collapses to one line and its escalation branch becomes
unreachable; the CC-BY/attribution hazard disappears; the theme problem
disappears because a regeneration can take a palette; and the byte-reproducible
build gains a *stronger* test than AC-2 asks for — regenerate in CI and assert
the checked-in files are byte-identical, which is exactly the house pattern
section 09 praises at line 591 and the discipline `test/resources/hdl` goldens
already use.

The objection is real: an acorn drawn from Bezier primitives by an engineer is
worse art than an acorn drawn by an illustrator. But that objection prices the
wrong thing. Recognisability as *an acorn* only matters for the word↔picture
link; the security-relevant act is "does the shape on my screen match the shape
you are describing," and the word is doing that work already.

## The lever the issue leaves on the table

If drawability is the constraint, **choose a vocabulary that is drawable.**
The issue treats `WORDS` as immovable, and `Sas.java:31-37` does say index n
must mean the same glyph forever. But look at what is actually released: the
transport shipped, the words did not — no `jls.collab.ui` exists, no Verify
dialog exists (#816 is unfiled work), the words are never transmitted, and no
user of any JLS build has ever seen a SAS. The freeze is an author's
self-discipline anticipating a UI that does not exist yet. **This task is the
last moment at which the vocabulary is free.** Swapping "glacier," "harbor,"
"penguin," and "mushroom" for nouns that render unmistakably at 48px from
primitives — key, house, tree, star, arrow, moon, bell, anchor, flag, heart,
ladder, drum — makes the artwork problem largely dissolve rather than get
solved. Half the current list already qualifies.

I would rather see that decided here, deliberately and in writing, than see the
vocabulary frozen by the accident of which images someone managed to find.

## Where the issue is right, and should not be softened

- **Index-addressed, one accessor** (AC-1) is the correct seam. Glyph identity
  is one concept; I would put the accessor *on `Sas`* rather than in a new
  class, returning a resource name (a `String`), not an `Image`. That makes
  AC-4 structural instead of aspirational — a function returning a string
  cannot acquire a Swing dependency — and the totality test becomes five
  lines: for every index, `Sas.class.getResource(name) != null`. No asset
  counting, no size/count bookkeeping to drift.
- **Fail the build, not the dialog** (AC-2) is exactly right, and is the
  project's characteristic move (`HelpTopicsTest`, `SaveTagsTest`,
  `ExtensionPointCatalogTest`). Keep it even though the protocol already
  forbids the renames it guards against; it is nearly free.
- **No network fetch, ever** is non-negotiable and correctly stated.

## What I would add that the issue does not ask for

**Make this the first row of a repo-wide asset manifest, not a collab-local
one.** A `resources/ASSET-PROVENANCE` (or the REUSE.toml section 09 already
sketches at :390-400) covering the 64 glyphs *and* the 33 palette GIFs *and*
the 34-file unreferenced `src/jls/images/` duplicate that three separate
documents have now flagged as dead weight (`docs/standards-adoption/
OPEN-QUESTIONS.md:38`). Same test, same cost, closes an existing hole instead
of digging a parallel one. A per-feature licence sidecar is the kind of thing
that gets written once and never generalised.

**State the a11y ordering explicitly.** 508 §502 / EN 301 549 clause 11 work is
live (`docs/standards-adoption/03-accessibility-conformance.md`). A blind
student verifying a session compares words; the image contributes nothing to
the accessible tree beyond an accessible name that *is* the word. Writing
"words are the primary channel, images are redundant reinforcement" into this
task constrains #816 correctly — Confirm gates on the words being present and
readable, and a glyph panel that fails to render degrades to words rather than
blocking a security decision. AC-2's framing ("rendering a blank panel to a
user who is being asked to make a security decision") quietly assumes the
opposite, that the images are load-bearing. They are not.

**Record the split against #168 §2.** That section explicitly *rejected*
"glyph images as their own task" on the grounds that the Verify dialog is
untestable without them. This issue is that rejected cut, and AC-4 refutes the
rejection's premise by asserting the glyph layer is headlessly testable alone.
The split is right; it just needs a REPLAN comment on #168 so the feature's
recorded rationale stops contradicting its own children.

## Disposition

Endorse the outcome, reframe the route. Keep AC-1 (with the accessor on `Sas`,
returning a resource name), AC-2, AC-4, AC-5. **Disregard AC-3 as written**:
its provenance table, per-image source column, and no-compatible-set escalation
branch are all consequences of a sourcing decision I would reverse. Answer
#168's Open Question with the third door it never listed — generate the set
in-house from primitives, per the `GenerateIcons` precedent — and spend the
saved effort on the vocabulary-drawability question while the vocabulary is
still free to change.
