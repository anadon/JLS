# Issue #775: TASK-C551-2: the gallery is a published page linked from the README and linking back to install, readable in both browser themes
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-27 (#511) exists because seven competitor teardowns independently found that a
switcher bounces in the first ten minutes. #775 is the *pre-install* half of that
fix: the only surface CAP-27 owns that a stranger can reach without downloading
anything. Judged against that arc, "a page of pictures" is a weak instrument —
CircuitVerse and Falstad answer the same visitor with a *running* circuit. The
dedup adjudication on #551 correctly refused to merge this with #573 (static
depiction vs. executable demo), but it settled a boundary question, not the
strategic one: what can this page do *today*, with shipped machinery, that beats a
picture?

It can hand the visitor the circuit and a command that runs it with no JDK. That
capability already ships (README lines 103-114): `docker run --rm -v "$PWD:/work"
ghcr.io/anadon/jls -b -t tests circuit.jls`, multi-arch, headless by construction.
That is a pre-install evaluation path JLS has and Falstad does not, and it is not
blocked on #572's go/no-go. The issue never mentions it.

## Reframing 1 — the gallery is an evaluation surface, not a brochure

Each entry should carry: the SVG, the caption, #548's *suggested exercise*, a link
to the `.jls` file itself, and the one-line container command that simulates it.
The generator from #773 already holds all five (it walks the Examples set to render
each circuit). Cost is a template field and a link; the payoff is that the page
answers "is this tool any good?" with an artifact the visitor can run, instead of a
JPEG of one. The exercise text in particular is free — #548 AC-4 requires it to
exist and the gallery would otherwise be a second, poorer index of the same set.

I would add as an acceptance criterion: *every entry links its `.jls` source, and
the printed run command is exercised in CI* (the batch golden tests already run
these circuits headless; asserting the documented command line matches what CI runs
is the same drift discipline as `HotkeysHelpAccuracyTest` and `CliFlagTableTest`).

## Reframing 2 — one entry template, four consumers

Four issues are converging on the same list of circuits: this gallery, #574's "try
it in your browser" links, CAP-27 PF-5's lessons, and CAP-29's migration pages. AC-4
here ("no entry implies a runner exists") fences correctly against premature claims
but expresses it as an absence. Express it as a *slot* instead: the entry template
has an optional `demo` field, empty today, that #574 fills without restructuring the
page. Same for a `lesson` field. Then PF-5 and #574 are data changes, not new pages,
and JLS ends with one publication surface for the example corpus rather than three
that must be kept in sync. Today's shape — static gallery now, demo page later —
otherwise guarantees two pages listing the same circuits with divergent captions.

## Reframing 3 — "via the repository **or** GitHub Pages" is the one decision that must not be deferred

There is no Pages workflow in `.github/workflows/` and no site of any kind. Whoever
picks this up in a 0.5-1 mw band decides, by accident, the shape of the first thing
JLS ever publishes to the web — and CAP-35 PF-2 (#519) then inherits it. The
"must not grow into hosted-manual scope" fence protects the *content* boundary and
does nothing about the *infrastructure* boundary, which is the one that actually
bites.

ARCHITECTURE.md already records the project's posture (lines 264-267): "Repo
documents (README, `docs/*.md`, this file) are already web-readable on GitHub and
are the normative home for contracts." Follow it. Ship `docs/gallery.md`, generated
by #773's command, rendered by GitHub. That choice collapses most of this task:

- AC-2 becomes a file-existence assertion in the existing test style (the
  `ReadmeOnboardingTest` shape #545 AC-4 already plans) rather than an HTTP link
  checker against a live site.
- AC-3 becomes largely free — GitHub renders the surrounding page in the reader's
  theme, and JLS's SVGs carry their own opaque white ground
  (`CircuitRenderer.java:323-324` sets `Color.white` and fills the bounds), so no
  invisible-black-on-black failure exists.
- No Pages deploy, no `gh-pages` branch, no permissions change, no second CI lane.

When CAP-35 stands up the real site, the same generator emits the same entries into
that site's template. Say that in the issue: *the gallery's hosting is deliberately
in-repo Markdown; CAP-35 owns the eventual site and inherits this generator.*

## Reframing 4 — solve "both themes" in the exporter, once, for every consumer

AC-3 is written at page altitude, so it will be solved at page altitude, and then
re-solved by CAP-35's manual, #545's README, and CAP-29's migration pages. The
durable seam is one level down: `-i out.svg` currently bakes a white background
rect and light-palette strokes. JLS already has the abstraction to do better —
`Theme` is a record of semantic roles (`background`, `wireZero`, `foreground`
pending) with a CVD-validated palette and a `ThemeTest` delta-E harness. Two honest
routes:

- Emit a light and a dark render per circuit and reference them with GitHub's
  documented `<picture>` + `media="(prefers-color-scheme: dark)"` pattern. Works in
  GitHub Markdown today, works on any future static site, deterministic, and
  #773's byte-identical golden check extends unchanged.
- Or a single SVG carrying both palettes behind an internal
  `@media (prefers-color-scheme: dark)` block — more elegant, but verify GitHub's
  SVG sanitizer preserves `<style>` before relying on it.

Note the interaction with #289 §7.1, which pins print/export output to be
*independent of the runtime-selected theme*. A file that carries both palettes still
satisfies that: the bytes do not depend on which theme the user picked. Worth
recording explicitly, because a careless reading of #289 forbids exactly the right
design here. This belongs on #773 (the generator), not #775 — but #775's AC-3 is
what should ask for it.

## Where the work pulls with the trajectory

It does, on the whole. It rides shipped machinery, adds no runtime, and is the
cheapest surface serving the cheapest capstone. One duplication watch: #773's "CI
fails when a committed render differs from a fresh one" is the third instance of
this pattern in the repo (reproducible jar/`bom.json`, installer repro lane). Make
it a generic "generated artifacts are current" gate so CAP-35 PF-3's screenshot
pipeline plugs into the same check rather than growing a fourth.

## What I would change in the issue as written

1. Replace "published via the repository or GitHub Pages" with a decision:
   `docs/gallery.md`, in-repo, with CAP-35 named as the inheritor of the generator.
2. AC-1: entries carry caption **and** #548's exercise, the `.jls` link, and the
   container run command; the command is CI-exercised.
3. AC-3: restate as an exporter-level criterion — renders ship a light/dark pair (or
   a deliberate single ground, stated on the page) — and hand it to #773.
4. AC-4: keep "static, no backend"; change "no entry implies a runner exists" to
   "the entry template reserves an empty demo slot that #574 fills."

None of this makes the issue bigger; 1 and 3 make it smaller. The band stays 0.5-1 mw.
