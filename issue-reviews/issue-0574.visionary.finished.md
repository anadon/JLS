# Issue #574: FEAT-C32-3: every shop-window surface — README and example gallery — carries a "Try it in your browser" link that lands on a running demo circuit
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not links. The outcome it names — "a prospective evaluator's path from first
hearing of JLS to a running in-browser example is one click, and from there one
more click to the installers" — is a claim about **the project's front door**.
#574 is the only issue in the CAP-32 family that owns the funnel end-to-end;
#572 owns a mechanism, #573 owns a page, #574 owns whether a stranger ever
arrives. Judged as "add two hyperlinks," it is trivially correct and banded
right. Judged as "own the front door," it is filed against the wrong surfaces
and ordered behind the wrong dependency.

## Observation 1: four issues are each appending to the README, and the README is not a shop window

The dedup comment on this issue records the split cheerfully — #545 owns the
pitch, #553 owns migration links, #574 owns the demo link — and #551 AC-3 adds a
fourth ("the README links to the gallery"). Nobody owns the README's information
architecture. The result is predictable: a file that today is 369 lines and opens
with an OpenSSF badge, then installer verification, cosign invocations, SHA256
custody rationale and a Wayland toolkit matrix, will acquire a fifth, sixth and
seventh above-the-fold claimant. TASK-C574-1 (#845) AC-1 and AC-5 already feel
this — "above the fold," "try, then install, rather than competing for the same
attention" — which is an information-architecture requirement wearing a link's
clothes.

The deeper fact: **JLS has no website.** `docs/standards-adoption/10-desktop-and-housekeeping.md`
§ screenshots states it plainly ("The project has no published site today"), and
`ARCHITECTURE.md` § "Help delivery" records hosted docs as a planned future with,
per CAP-35's own evidence line, no owning issue. Meanwhile #551 wants to publish
a gallery page, #573 wants to publish demo pages, CAP-35 PF-2 (#519) wants to
publish a versioned manual, #886 wants shareable demo URLs, and #574 wants to
link to all of it. Five issues each independently stand up a fragment of a site,
and none owns the site.

## Reframe A (primary): make the site the feature, and this issue its home page

Cut the seam one level up. A single **static site substrate** — one GitHub Pages
publish from one build, `/`, `/gallery/`, `/demo/<example>/`, `/docs/<version>/` —
absorbs #551's page, #573's pages and CAP-35 PF-2's manual as *routes*, and turns
#574 from "add links" into **"the home page is the funnel: try → browse → install."**
That is exactly this issue's stated outcome, and it stops being an integration
afterthought.

What this buys, concretely:

- **The README shrinks instead of growing.** It carries one link to the site and
  keeps its normative content (verification custody, batch contract pointers).
  The shop-window competition among #545/#553/#574/#551 dissolves because the
  shop window is no longer a source-tree text file rendered by GitHub's CSS.
- **AC-3 ("static pages only, nothing operated or account-gated") gets stronger,
  not weaker.** Today's phrasing is a constraint on link targets; under one
  substrate it is a property of the deployment (a file copy, per #573 AC-3), and
  the anti-simulator.io permanence pitch from #510 becomes structurally true
  rather than per-link audited.
- **CAP-35's cost drops.** PF-2's "hosted versioned manual" is the same publish
  step. The project would pay the Pages/CI plumbing once instead of three times.

This does not violate KC-32-2: a site with a gallery, read-only demos and a
manual introduces no in-browser editing surface.

## Reframe B: the links are a build product of an example manifest, not content

TASK-C574-2 (#847) already reaches for the right idea — "generated from the same
example manifest the gallery and the demo both consume" — but **no issue owns
that manifest.** #548 owns a curated set surfaced as a Swing menu over
`resources/samples/`; #551 owns SVG renders; #573 owns demo pages; #574 owns
links. Four consumers, zero declared source of truth.

Declare one: a committed manifest (id, circuit path, caption, suggested
exercise, category, `demo: true|false`) that is the single input to (a) the Swing
Examples menu, (b) the `-i out.svg` gallery generator, (c) the demo page
generator, (d) the site's nav and the README's one link. This is the same move
the codebase already made for element save tags — `SaveTags.resolve` as a frozen
table with `SaveTagsTest`/`FileFormatSpecTest` as ratchets, replacing a
per-element switch — and the same instinct as `docs/extension-points.md`'s typed
seam catalog. It fits the house style precisely.

With that seam, #574's acceptance criteria become nearly free: AC-2 is "the
generator emits a link when `demo: true`," and #847 AC-3 ("a check fails the
build when the gallery claims a demo link whose target is not present") is a
**build invariant, not a check** — the generator cannot emit a link to a page it
did not also emit.

Corollary worth acting on: **#845 AC-3 should not be a network link check.** A
CI lane that probes an external URL is a flaky lane and an odd fit for a project
that treats reproducibility as a contract (`.buildinfo`, rebuilt-and-compared jar
and BOM on every push). If the site and the demo are published from this
repository by this workflow, the correct guard is that the built site's internal
links resolve within the built artifact — deterministic, offline, no third-party
availability in the critical path. Keep the external check only for links that
genuinely leave the project.

## Reframe C (the one that may make the problem disappear): stop waiting on CheerpJ

This is the sharpest structural point, and it argues for **reordering, not
descoping**.

#574 sits at the end of a chain — #548 → #551 → #574 and #572 → #573 → #574 —
whose head is a feasibility spike with a genuinely uncertain outcome. KC-32-1
sets a 15-second click-to-interactive threshold for a CheerpJ-wrapped Swing app;
the dominant cost there is the wasm JVM runtime, not JLS's own footprint (82k
LOC plus FlatLaf, JFreeSVG, xz). Swing interaction fidelity under CheerpJ is the
second uncertainty. A no-go is a live possibility, and #574's dedup comment
already plans for it: "a stalled #573 leaves #551 shippable with these links
simply unfilled." That is a correct failure mode — and also an admission that
the project's entire zero-install evaluation story is staked on one substrate
bet.

But **#572's fallback (a) is built from parts that already ship.** `-i out.svg`
(JFreeSVG, `JLSStart` flag table, issue #154) emits resolution-independent
circuit renders today. `-vcd` (`BatchSimulator`, `TraceSample`, documented as a
stability contract in `docs/batch-interface.md` and `docs/vcd-interop.md`) emits
the signal history today. Fallback (a) is "SVG + a JS player driven by
pre-computed VCD" — i.e. a few hundred lines of JavaScript over two shipped,
tested, byte-reproducible batch outputs. It needs no go/no-go, no wasm runtime,
loads in well under a second, degrades to a static image with JS off, and is
immune to any upstream CheerpJ regression.

So: **make fallback (a) the default path and CheerpJ the optional upgrade.** Then
the gallery entries *are* the demos — an SVG that animates when you scrub or
toggle — and #574's headline requirement collapses to a tautology: the shop
window is the demo, so every shop-window surface trivially "carries" it. There is
no link to keep alive, no second page to funnel toward, no dependency on an
unproven spike, and #551 and #573 largely merge.

I am **explicitly setting aside AC-1 as worded** ("lands on a running
FEAT-C32-2 demo page") under this reframe: the better goal is that the gallery
entry itself runs, which makes the landing-page hop and its dead-link risk
disappear. CheerpJ, if #572 goes, then adds full-fidelity interaction as a
per-example upgrade on the same manifest — strictly additive, never blocking.

The honest limit: fallback (a) plays back *pre-computed* traces, so "toggle any
input and see the circuit respond" is only true for input combinations rendered
ahead of time. For a handful of curated examples with few inputs, precomputing
the full toggle space is cheap and the illusion is complete; for the RV32I
showcase (#548 AC-2) it is not, and that example wants either CheerpJ or a
scripted walkthrough. That is a per-example property the manifest should carry —
which is another argument for the manifest.

## Alignment

The work pulls with the project's arc, not against it: #510's finding that
zero-install is the one structural advantage of the web competitors is credible,
and CAP-32's narrow re-scope after CAP-19's `not_planned` closure is disciplined.
Nothing here reaches into the Swing application (the concern raised on #886's
Open Question 2 does not apply to #574). The one place it pulls against the arc
is subtle: a project whose distribution story is otherwise *fiercely*
self-contained — reproducible jar, attestations, cosign, no service to operate —
should be uneasy about the front door depending on a third-party wasm JVM whose
regressions it cannot fix. Reframe C removes that dependency from the critical
path.

## If only one thing changes

File the site substrate as the owning surface (or re-aim #574 to be it), declare
the example manifest, and re-order so the SVG+VCD path ships first. Then #574's
two tasks are generator output rather than hand-maintained links, and the funnel
exists whether or not #572 ever says go.
