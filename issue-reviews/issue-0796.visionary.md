# Issue #796: TASK-C586-1: a committed capture manifest names every screenshot, and one command regenerates the whole image set on the existing headless-sway rig
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machinery away and #586's claim is a *staleness* property, not a capture
property: no image in the docs may outlive the interface it depicts. #796 is the
first move — give every shot a name and a producer. That goal is right and it fits
the project's dominant arc: JLS has spent this cycle turning ambient facts into
build products (`bom.json`, `.buildinfo`, reproducible jar, `docs/batch-interface.md`
as a stability contract, `HeadlessCoreRatchetTest` pinning the headless boundary).
Doc images are the last hand-pasted artifact. Endorse the end.

The mechanism is where I part company. AC-3 makes "runs on #101's rig" a
*requirement*, and AC-4 exiles anything richer than boot-and-screenshot to #91.
Together those two criteria pick the compositor framebuffer as the seam. That is
the wrong seam, and the evidence is already in the tree.

## Grounding: what the repo already has

- `src/jls/edit/CircuitRenderer.java:86` — `draw(Graphics, Set<Element>, @Nullable
  SimpleEditor)`. The *same* paint path serves the live editor canvas, printing,
  and export. It is `Graphics2D`-generic.
- `CircuitRenderer.java:301,314` — `exportImage` writes PNG/JPEG and, on `.svg`,
  swaps in JFreeSVG's `SVGGraphics2D`. Fully headless, no display of any kind.
- `test/jls/SvgExportTest.java:72` — `exportingTwiceIsByteIdentical`, and four
  fresh loads besides, because `#166`'s HashSet iteration order must not reach the
  bytes. Circuit renderings are already **byte-deterministic**, not
  tolerance-deterministic.
- `test/jls/ui/EditorGestureSupport.java:116` — the display suite already paints
  the live editor canvas into a `Graphics` in-process.
- `docs/component-naming.md` + `test/jls/ui/ComponentIdentityTest.java` — every
  palette button is `palette.<slug>`, every mirror menu item `menu.elements.<slug>`,
  every dialog field `dialog.<slug>.<field>`, enforced on the live app (#210).
  **The UI already has a stable address space for panes.**
- `scripts/wayland-rig.sh:308` — the rig launches `java -Dawt.toolkit.name=WLToolkit
  -jar "$JAR"` with **no file argument**, and `:60` defaults `PIXEL_DIFF_MIN=0`.
- The repo contains **zero screenshots**. `resources/help/**` holds ten images,
  all element glyphs; README has none.

## The objection: the compositor cannot address what the manifest promises

AC-1 says an entry names "circuit, window/pane and theme". `grim` and `import`
capture an *output*, not a pane. So of the three axes the schema promises:

- **circuit** — the rig does not accept a file (`:308`); adding one is a rig change,
  which AC-3 forbids ("adds no second display or runtime provisioning" is satisfied,
  but #101 owns the rig and #586's boundary note says this feature does not re-own it).
- **pane** — unaddressable through a screenshot without cropping by pixel geometry,
  which is exactly the brittleness `RenderAssert`'s header disavows ("never brittle
  pixel goldens").
- **theme** — a Swing LookAndFeel setting, invisible to the compositor except as
  colors in a frame you must then crop.

So under AC-4, essentially every entry worth writing — "the palette mid-drag", "the
trace window during a run", "an adder dialog" — is stamped *blocked on #91*, and the
manifest that ships expresses precisely the one shot the rig already takes: a whole
empty desktop with an empty editor on it. That is a schema with one row and no
consumer, designed before its first consumer (#545/#551) has said what it needs.

Worse, #586's own boundary comment names two inherited weaknesses — `PIXEL_DIFF_MIN`
is `"0"` so a blank frame passes, and `gui-wayland` fail-opens when the JBR CDN
fails. #796 does not merely risk inheriting them; AC-3 *mandates* building the
capture path on top of them. A capture command whose engine can legitimately produce
no images and exit green is a poor foundation for a ratchet (#797) that has to
distinguish "regenerated" from "silently didn't".

## The reframing: cut at the component tree, not the framebuffer

Address a capture as `(circuit, component name, theme, size)` and render it
**in-process** — `component.printAll(g)` into a `BufferedImage`, or
`CircuitRenderer.exportImage` for circuit-only shots — inside the existing
`@Tag("display")` surface on the already-fail-closed `gui-x11` substrate. Three
classes fall out, with sharply different costs:

- **Class A — circuit renderings** (`jls -i out.svg circuit.jls`). No display, no
  JBR, no compositor, byte-identical per `SvgExportTest`, resolution-independent,
  and diffable in git. For a logic simulator this is most of what the docs want to
  show, and README already recommends SVG "for slides, lab reports and hosted docs".
  For this whole class, #586's AC-4 (a measured pixel tolerance with recorded
  derivation) becomes **moot** — the tolerance is zero bytes.
- **Class B — UI chrome** (palette, menu bar, dialog, trace window). Paint the named
  component. The theme is `UIManager.setLookAndFeel` before the paint. The state is
  a fixture built with the synthetic-`MouseEvent` dispatch #91's Layer 2 already
  proved deterministic (~1.3 s, versus the abandoned Robot path's 750 s and 4/7
  flake). No `wtype`, no compositor input, no dependency on #91's *remaining* work —
  only on what it already landed.
- **Class C — true desktop shots** (window decorations, the app in a real session).
  Genuinely wants #101's rig. Few, and the least likely to rot.

What this buys, measured against the feature's own criteria:

- **#586 AC-3** ("a manifest entry naming a window, pane or circuit that no longer
  exists fails loudly") becomes a *lookup miss* — `getName()` returned nothing for
  `palette.adder` — carrying the name in the exception, exact and free. Under the
  screenshot framing it has to be inferred from pixels.
- **#586 AC-4** collapses to Class B only, and even there a blank capture is caught
  by `RenderAssert`'s existing "something was painted at all" check rather than by a
  calibrated `PIXEL_DIFF_MIN` this task does not own and cannot make progress on.
- **#796 AC-3's spirit** — no second display apparatus — is honored *more* strongly:
  Class A needs none at all, Class B reuses the test suite's substrate, and neither
  imports the JBR fail-open.
- **#797** gets a check it can actually write: match doc image references against
  manifest keys, and resolve every manifest key against a live component tree.

## What I am disregarding, and why

**AC-3 as written** ("the capture path runs on #101's rig") and **AC-4** (defer
anything past boot-and-screenshot to #91). They were written to prevent scope theft
from #101 — a good instinct, and the boundary note defending it is careful. But they
encode a *mechanism* to enforce a *boundary*, and the mechanism cannot express the
manifest schema AC-1 promises. The boundary survives intact under the reframing:
this task still provisions no display, still does not touch `wayland-rig.sh`, still
does not add `wtype` scripting. It simply stops routing pixels through a compositor
that has no concept of the things the manifest names.

## What survives unchanged

AC-1 and AC-2 stand as the real deliverable: a committed manifest, one command, and
the release procedure invoking it. I would add three fields the current schema omits
and the first real consumer will demand: **crop/region of interest**, **scale/DPI**
(store listings and a hosted manual want 2x), and **locale** (there is a
second-language arc in this roadmap; a screenshot is the one doc asset that hard-bakes
UI language). A manifest that cannot express a crop gets rewritten by #545.

## Smallest honest next step

Ship Class A alone first: manifest schema, `mvn`-invocable regeneration, and the
release-procedure hook, with circuit SVGs as the only entry kind. It is buildable
today with zero display infrastructure, it exercises the schema against a real
producer, and it makes #797's ratchet demonstrable. Then add Class B behind the same
schema when #545/#551 name the first chrome shot. Class C stays a `planned` note on
#586 pointing at #101, where it belongs — and where, if it is never picked up, the
feature still delivers its actual promise.
