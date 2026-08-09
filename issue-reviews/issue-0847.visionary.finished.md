# Issue #847: TASK-C574-2: each gallery entry links to its own demo page, generated from the same manifest — an example with no demo cannot advertise one
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its wiring, #847 asks for one property: **the shop window cannot lie about
what is behind it.** That is the same property `HelpTopicsTest` (#70) already enforces
for the in-jar manual — every `Map.jhm` url names a bundled page, every inline `href`
resolves, every bundled page is reachable from some topic — and the same property the
README's reproducibility apparatus enforces for release artifacts. The instinct is
correct and deeply in-arc. My objection is not to the goal but to the seam it cuts.

## The load-bearing thing this issue assumes and nobody owns

AC-2 says the links come from "the shared example manifest." **No issue defines that
manifest.** Tracing the phrase across the tree:

- #766 (TASK-C548-2) AC-3: "Each circuit is categorized in data the menu reads."
- #768 (TASK-C548-3) AC-1: caption + exercise "in a form the Examples menu and the
  gallery can read"; AC-4 additionally wants the caption *inside the circuit* as a
  `Text` element — a second home for the same string.
- #773 (TASK-C551-1) AC-3: "Captions come from #548's metadata."
- #841 (TASK-C573-2) AC-1: "generated from the same example manifest."
- #847 (here) AC-2: "generated from the shared example manifest."

Five consumers, zero owners. No task names its location, its schema, its format, or the
fact that it must be read by **two very different readers**: `jls.edit` Swing menu code
off the classpath (`#130`: never `user.dir`), and a static-site build step outside the
JVM. #847 is the fifth issue to spend the manifest and the first whose AC set is
*unsatisfiable without it* — and it is banded at 0.25–0.5 mw, which is the cost of
consuming a manifest, not of inventing one. Either this task quietly invents a fifth
definition or it hand-writes links and fails its own AC-2.

**Concrete alternative framing #1 (the one I would actually do first):** file the
manifest as a task under #548 and make it the samples' `Map.jhm`. The repo already has
the shape and the test lineage — `resources/help/Map.jhm` + `JLSHelpTOC.xml` +
`HelpTopicsTest`. Do the same for `resources/samples/`: one committed index carrying, per
example, `id`, file name, category, caption, exercise, and a `demo:` field the demo build
fills. It ships on the classpath (so the Examples menu reads it), it is plain text (so the
site generator reads it without a JVM), and #768 AC-4's in-circuit caption `Text` element
becomes *generated and asserted against* the manifest rather than authored twice. AC-3 of
this issue then costs one `assertTrue(broken.isEmpty(), ...)` in the
`HelpTopicsTest` idiom, not a bespoke build check on a web bundle.

## The reframing that makes the problem disappear

The link-integrity problem exists only because the plan builds **two page sets that must
reference each other**: static SVG gallery entries (#773/#775) and running demo pages
(#840/#841/#844). AC-3 is a guard on a seam this design chose to create.

**Concrete alternative framing #2 — one page per example, progressively enhanced.** Make
the per-example page the atom. Each page carries: the `-i out.svg` render, the caption and
exercise, the install CTA — and, when the example is demo-enabled, the running mechanism
mounted on that same page. The gallery becomes an *index over those pages*, so a gallery
entry does not hold a cross-reference to a demo bundle at all; it links to the page that
already is the entry. The failure AC-3 checks for — "gallery claims a demo link whose
target is not present" — becomes **unrepresentable** rather than detected. Every AC here
survives except AC-3, which stops being needed.

This also fixes AC-1's weakest instinct. AC-1 settles for silence: an example without a
demo "renders without a broken or placeholder link." But #841 AC-3 *records the reason* —
measured click-to-interactive, why the example was excluded. Dropping that reason on the
floor is the project throwing away information it deliberately paid to produce, on the
one surface where a visitor is asking exactly that question. On a per-example page you
can say the true thing: "this circuit is too large to run in the browser (measured 42 s);
download it and open it in JLS." That is #844's honesty discipline applied one surface
earlier, and it converts a dead end into an install prompt.

Note also that under #572's fallback (a) — "headless-rendered interactive SVG with a JS
signal player driven by pre-computed VCD" — the gallery render **is** the demo asset, and
the two-page-set split collapses on its own. #847 as written is shaped for the CheerpJ-go
branch and silently assumes it; the per-page framing is the one design that is correct
under all three of #572's outcomes, including no-go (b), where the page carries a video.

## The duplication nobody has counted

Five tasks now each carry their own drift/link check over the same corpus:

| Task | its check |
|---|---|
| #773 AC-2 | CI regenerates renders, fails on diff |
| #775 AC-2 | README↔gallery and gallery↔install links, fails when either is dead |
| #841 AC-4 | demo example list vs shipped curated set disagree |
| #845 AC-3 | CI link check on the demo URL |
| #847 AC-3 | gallery claims a demo link absent from the bundle |

Five checks, five places to be wrong, five things to maintain — for one invariant: *the
shop window's claims match the shipped example set.* **Concrete alternative framing #3:**
one committed `scripts/build-shopwindow.sh` that consumes `resources/samples/` + the
manifest and emits the whole static tree (index, per-example pages, demo assets, README
fragment), plus one regenerate-and-diff gate. That is exactly the reproducibility idiom
the project already runs for the jar and `bom.json` on every push, and it collapses the
table above into one lane. #847 is the right moment to notice this, because it is the
issue that would otherwise add row five.

## Where this pulls against the larger arc

The project is accumulating **three separately-owned static web presences**: the CAP-27
gallery, the CAP-32 demo, and CAP-35's (#519) hosted versioned manual — which
ARCHITECTURE.md's "Help delivery" decision already names as the planned future, with the
help tree deliberately kept as portable HTML 3.2 "so the same tree can be published to the
web without rewriting." #551 explicitly forbids growing into CAP-35's scope, and that
boundary is defensible as *ownership*, but it is producing a project with three publishing
pipelines and one domain. The arc points at one static site with three sections and one
publish step. I am not asking #847 to build that; I am asking that its generator be
written as a *section emitter* of one site build rather than as gallery-specific
post-processing, so the eventual convergence is a merge and not a rewrite.

## What I would keep, drop, and add

- **Keep** AC-2 (generated, committed command) and AC-4 (static only, no operated or
  account-gated intermediary) unchanged — AC-4 is the capstone's permanence property and
  is non-negotiable.
- **Keep** AC-5, but note it is a regression guard on #775 AC-2, not new content.
- **Rewrite AC-1**: an example without a demo shows the *recorded reason* from #841 AC-3
  and an install prompt, not silence.
- **I am disregarding AC-3 as stated.** Under framing #2 it guards an impossible
  condition. If framing #2 is rejected and the two page sets stay split, then AC-3 should
  not be a new bespoke build check: implement it as a `HelpTopicsTest`-lineage JUnit test
  over the manifest — every `demo:` entry resolves to a bundle file, every bundle file is
  reachable from some manifest entry (the orphan direction, which #847 currently omits and
  which is how a demo page goes unadvertised).
- **Add a hard dependency**: this task is `blocked_by` a manifest-defining task under
  #548. Without it the 0.25–0.5 mw band is fiction.

## Verdict

**endorse-with-reframing.** The property is right and belongs in the project. The design
is one seam too late: it repairs a cross-reference between two page sets that a
per-example-page cut would never have created, it spends a shared manifest no issue has
built, and it adds the fifth copy of one invariant. Land the manifest first, cut the
gallery/demo seam at the page rather than the link, and most of this issue becomes
structure instead of a check.
