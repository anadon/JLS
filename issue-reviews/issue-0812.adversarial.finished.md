# Issue #812: TASK-C596-2: text elements render monospace where monospace is what the content needs — Digital #1129-class, scored and pinned
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue asks for per-element monospace font selection on `Text` elements,
persisted with the circuit and consistent between the editor and figure
export. The problem: the codebase already has almost everything the
acceptance criteria ask for, the one genuinely new criterion (AC-3) leans on
an unbuilt sibling feature it doesn't declare as a dependency, and the
process criteria (AC-4) reference a scoring catalog that does not exist in
the repository. As written, an implementer cannot tell what code change this
issue is actually funding.

## Findings, most severe first

### 1. (Critical) The core ask already exists in the codebase; the issue never checks this

`src/jls/elem/Text.java` already has `fontName`/`fontSize`/`isBold`/`isItalic`
fields, all persisted via the declarative `Attribute` mechanism
(`OWN_ATTRIBUTES`, lines 274–384) — chosen per element, saved and loaded with
the circuit. `src/jls/edit/TextDialog.java` (lines 168–183) populates the
font-name combo box from
`GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()`,
which is guaranteed by the JVM spec to include the logical family
`"Monospaced"` on every platform. `TextRenderer.java` (lines 39–58) builds an
AWT `Font` straight from `t.getFontName()`/`getFontSize()`/bold/italic and
draws with it — no special-casing, no proportional-only assumption. In other
words: a user can already open the text dialog, type or pick `Monospaced`,
and get a monospace-rendered, persisted, per-element font today, in both the
editor canvas and the existing SVG/PNG export path (both draw through the
same `ElementRenderers.draw` → `TextRenderer`).

The issue's outcome section ("so a truth-table fragment ... lines up instead
of drifting") asserts a present-tense defect but cites no repro circuit, no
failing test, and no code path that actually forces a proportional font.
Two possibilities, both are issue defects:
- Nothing is actually broken — the acceptance criteria (AC-1, most of AC-2)
  are already true, and the issue is asking to fund and pin behavior that
  exists, risking an implementer inventing redundant UI (e.g. a "monospace"
  checkbox that duplicates the general font picker) to have *something* to
  land.
- Something subtler is broken (e.g., a specific bug in kerning/substitution
  the author observed) — but the issue doesn't say what, so the acceptance
  criteria can't be checked against the actual defect.

**Recommendation:** before funding, verify against HEAD whether `Text` +
`Monospaced` already produces aligned columns in both the editor and `-i`
export. If yes, close as already-satisfied or rescope to "pin existing
behavior with a regression test" (a much smaller, honestly-scoped item). If
no, the issue needs to name the actual failure mode.

### 2. (High) AC-3 depends on #536, which is unbuilt and undeclared as a dependency

AC-3: "The change composes with the bundled-font determinism of #536: figure
exports of a monospace text element remain byte-identical across the three
CI platforms." But #536 (FEAT-C24-1, "a schematic exports as print-styled
SVG and PDF") is itself an **open, unimplemented** feature — its own
frontmatter marks `feat_id` "provisional; renumbered at adjudication," and
its AC-2 ("`FigureDeterminismTest`... via a bundled deterministic font path —
no OS font fallback") describes work not yet done. Confirmed against the
repo: no `FigureDeterminismTest`, no "print theme," no bundled-font asset,
and no PDF export path exist anywhere in `src/` or `test/`. The current SVG
export (`src/jls/edit/CircuitRenderer.java`, line 201: `FontMetrics fm =
gg.getFontMetrics();`) measures text through live, OS-supplied AWT
`FontMetrics` — i.e., today's figure export is explicitly *not*
cross-platform byte-identical for text. So the issue's own framing —
"it must not disturb the deterministic text metrics the figure-export path
depends on" — describes a guarantee that does not currently exist.

`ordering_after` for #812 lists only `[TASK-C596-1]` (#811); it does not
list #536. An implementer who picks up #812 in score order, ignorant of
#536's status, cannot satisfy AC-3: there is no bundled deterministic font
path to compose with yet.

**Recommendation:** add #536 to `ordering_after`, or drop AC-3 to "byte-
identical on a single platform" until #536 lands, or explicitly state AC-3
is void until #536 ships.

### 3. (High) AC-4's "drawn from #592's catalog" references a catalog that doesn't exist

AC-4 requires the item be "drawn from #592's catalog in score order with its
estimate and actual recorded." #592 (FEAT-C37-1) is the feature that
*creates* that catalog, and its own AC-1 says the catalog is "published
under `docs/`" — but `docs/` contains no such file today (checked: no
`*catalog*`, `*parity*`, or `*ergonomic*` file exists). #592 is open and, per
its `gates` list, gates PF-2..5 of #521, all of which — including this
long-tail bucket #596 — are supposed to wait on it. So AC-4 asks #812 to
cite a scored row in a document that has not been written, making the
criterion either impossible to satisfy honestly right now, or trivially
gameable (an implementer can just assert "yes, drawn from the catalog" with
nothing in the repo to check it against, since the checker — the catalog
itself — doesn't exist).

**Recommendation:** #812 should not be workable until #592's catalog is
actually published with a scored row for this item; make that an explicit
blocking dependency rather than an implicit one inherited through #596.

### 4. (Medium) AC-4's estimate/actual/1.5x-stop-loss bookkeeping is unverifiable by any test

"its estimate and actual recorded (KC-37-2)... pinned by a test that fails
at the pre-change commit" bundles a genuine, testable engineering
requirement (a regression test) with a paperwork requirement (recording
mw estimate vs. actual, stopping at 1.5x overrun) that has no defined
location, format, or automated check anywhere in this repo. Nothing stops
an implementer from writing any numbers post hoc — the stop-loss rule is
unenforceable as stated. This is fine as project *process* but should not
be phrased as an "acceptance criterion" a reviewer can objectively verify;
as written it's gameable by construction.

**Recommendation:** either point to a concrete artifact (a CHANGELOG line,
an issue comment template, a tracked field) where estimate/actual live, or
strip this from the code-level acceptance criteria and keep it as
process guidance.

### 5. (Medium) AC-2's "equal advance widths ... fixture" is underspecified and can pass while missing the real hazard

Java's logical "Monospaced" family (and many physical monospace fonts) only
guarantee equal advance widths for glyphs the backing physical font actually
has; characters requiring font substitution/fallback (non-ASCII symbols,
box-drawing characters someone might paste into a "bit pattern" annotation)
can silently break equal-width layout without an exception. AC-2 doesn't say
the fixture must include such characters, so a fixture of plain ASCII digits
and letters would pass trivially while the "content needs" motivating case
(arbitrary pasted schematic annotations) remains unguarded.

**Recommendation:** specify the fixture includes at least one non-ASCII or
substitution-prone glyph, or explicitly scope the guarantee to ASCII.

### 6. (Low) External citation ("Digital #1129-class") is unverifiable from this repo and not linked

The title and body cite "Digital #1129-class" (presumably hneemann/Digital's
issue tracker) as the motivating parity gap, consistent with #592's AC-1
citation table, but no URL is given and this review has no access to that
tracker to confirm the cited issue actually describes a monospace-text gap
matching what's proposed here. Low risk since #592 is the canonical citation
owner, but #812 repeats the claim without a link, so a future reader can't
independently check it without cross-referencing #592.

## What's solid

- The `ordering_after: [TASK-C596-1]` sequencing (#811, wire coloring)
  correctly follows #592's score-order requirement in spirit, one line at a
  time.
- "lands outside `SimpleEditor` (KC-37-1)" is already naturally satisfied —
  `Text`, `TextDialog`, and `TextRenderer` all already live outside
  `SimpleEditor` in `jls.elem`/`jls.edit`, so this constraint costs nothing
  extra here.
- AC-1's "default face saves byte-identically" is consistent with the
  current `Attribute` implementation: `Text`'s `fn`/`fs` attributes don't
  override `omitted()`, so they're already written unconditionally today,
  and nothing about a monospace addition need change that as long as no new
  attribute is added carelessly.
