# Issue #709: TASK-C536-2: a schematic exports as print-styled SVG with committed visual goldens, distinct from the shipped screen-styled export
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this task is really for

Under the ACs, #709 wants one thing: *the figure in the handout is the circuit, drawn the
way a circuit is drawn on paper, and a change to that drawing is visible to a human before
it reaches a lab.* That is a good end and it serves the project's arc — the instructor is
the person who chooses the simulator, and JLS's headless surface (`-i`, `-b`, the
`ghcr.io/anadon/jls` container, the reproducible jar) already exists precisely so
instructor-side automation is possible without a GUI.

I endorse that end. I do not endorse the shape #709 gives it, which is *a second exporter
standing beside the first, with the first frozen byte-for-byte*. That shape is the one
choice in this task that is hard to undo, and it is the wrong one.

## Four checkable facts about the tree

1. **The shipped SVG export is a `Graphics2D` recording, not a figure.**
   `CircuitRenderer.exportImage` (`/home/user/JLS/src/jls/edit/CircuitRenderer.java:301-361`)
   substitutes `org.jfree.svg.SVGGraphics2D` for the raster `Graphics`, sorts elements into
   wire/part layers for stable order, and replays `ElementRenderers.draw`. Everything is
   absolute coordinates and a flat element sequence.
2. **There is no pre-change golden for it, by deliberate decision.**
   `/home/user/JLS/test/jls/SvgExportTest.java` states it: "Deliberately no full-document
   golden - text layout coordinates depend on the JDK's font metrics, which differ across
   machines." What exists is `exportingTwiceIsByteIdentical` — self-consistency within one
   JVM, not a committed artifact.
3. **"No screen chrome" is not expressible as a theme.** `src/jls/Theme.java` is a ten-field
   colour record whose `apply()` mutates `JLSInfo.Palette.*` statics. Chrome is drawn by
   *conditionals* in renderers — `PinRenderer.java:74-76` fills `watchColor` when
   `pin.isWatched()`; `ElementRenderSupport.drawHighlight` fills **hardcoded `Color.pink`**
   (`ElementRenderSupport.java:33-37`), not even `Theme.highlight`. No colour value turns
   those off, and a hardcoded pink is immune to the theme entirely.
4. **The registry the print-symbol map needs already exists**: `jls.elem.ElementRegistry`
   with ~30 `ElementType` rows. (ARCHITECTURE.md §"Adding an element today" still says "there
   is no element registry yet" — stale; worth fixing, but out of scope here.) The renderer
   seam is `ElementRenderers` + 29 `*Renderer` classes keyed by element class.

## The load-bearing objection: AC-2 freezes the wrong thing

AC-2 — "a test asserts [the screen export's] output for the same circuit is byte-identical
to the pre-change golden" — fails three ways at once, and the third is the important one.

- **There is no pre-change golden** (fact 2), so AC-2 begins by minting the artifact the
  project decided it could not have. Minting it is only possible *after* #707's bundled-font
  metrics land — and if #707's determinism applies to the whole figure path, the screen
  export's bytes change, so the golden is not "pre-change" at all. AC-2 is either vacuous or
  self-contradicting depending on how #707 scopes its font ownership.
- **Byte-identity of the screen export is not a property any user has.** No one consumes
  screen-SVG bytes; they consume the picture. The AC protects a hash, not a behaviour.
- **It converts a path that should be migrated into a path that may never be touched.**
  The pass-1 boundary comment on #536 leaves one question open and calls it the important
  one: "are these three renderings [#536, #546, #551] of one symbol vocabulary through one
  seam, or three renderers?" AC-2 answers it by regression-lock: any unification that routes
  screen and print through one description changes screen bytes and turns AC-2's test red.
  A test whose job is to fail when you do the right thing is a bad test, and this one
  outlives the task that added it.

## Reframing A — one writer, two profiles; not two writers

The distinction #709 draws (print writer vs. screen writer) is real but it is a *distinction
in output policy*, not in machinery. Express it as one export entry point taking a profile:

- a **role tag** on every emitted primitive — body, wire, pin, label, value, watch-marker,
  selection-highlight, grid — assigned where the renderer already knows it;
- a **profile** = (role filter, palette, symbol map). `screen` = all roles, `Theme.active()`
  colours, screen symbols. `print` = chrome roles dropped, monochrome, #723's print symbols.

Then #709's deliverable is ~a role filter, a profile constant, and a golden, instead of a
parallel writer. The claim "the screen export is unchanged" becomes structural (same code,
`screen` profile) rather than a byte assertion over a frozen copy — and it stays true when
the writer is later improved, which AC-2's version does not. #546 (tactile) and #551
(gallery) become two more profiles rather than the third and fourth renderer, and #537's
CircuiTikZ becomes a backend over role-tagged geometry instead of a `Graphics2D`-call
scraper. This is also the only reading under which #536 AC-4 ("no parallel symbol vocabulary
is minted") survives contact with the code: #709 as written must either hand-write 29 print
renderers or thread a style parameter through a seam it claims not to change.

## Reframing B — the radical simplification: the style *is* a stylesheet

SVG already has the mechanism this feature is reinventing. Emit **one** SVG in which every
primitive carries `class="wire"`, `class="label"`, `class="watch"`, and so on, and put the
palette in an embedded `<style>` block. Then:

- print vs. screen is *the same document with a different stylesheet* — 20 lines of CSS, not
  a second render path or a second golden;
- an instructor can restyle a figure for their handout (thicker wires, a house font colour,
  hide pin values) **without a JLS release**, which is a capability no AC in CAP-24 offers
  and which is worth more to the actual user than byte-identity across three CI runners;
- the diff a reviewer reads for a style change is the `<style>` block, one hunk;
- roles that must not *appear* in print (selection, grid, watch) are still filtered at
  emission — CSS `display:none` is the wrong tool for content that shouldn't leave the
  building — so the role tagging of reframing A is still required. CSS handles *appearance*;
  the filter handles *presence*.

Text-as-glyph-outlines (the #536 review's reframing 2, which I endorse) is compatible: a
filled path carries a class just as a `<text>` node does, and outlines are what make the
figure viewer-independent rather than merely byte-identical.

## Reframing C — a golden that isn't diff-local doesn't do the job the issue states

#709's own justification for goldens is "a rendering change is a diff in review rather than
a discovery in a printed lab." A byte golden over an absolute-coordinate `Graphics2D`
recording does not deliver that: move one gate and every subsequent coordinate shifts, so
the reviewer sees a whole-file diff and rubber-stamps it. The property that delivers the
stated outcome is **diff locality**, and it is a design constraint on the writer, not a test
you can add afterwards:

- one `<g id="…">` per element, id derived from the JLS element id;
- element-local coordinates under a per-element `transform`, so moving a gate changes one
  attribute;
- emission order = element id order (the existing bounds-then-classname sort is stable but
  reshuffles when geometry changes).

Test it directly: *move one gate → exactly one hunk; rename one pin → exactly one hunk.*
That is testable on a single platform, is what a reviewer actually experiences, and the
current exporter cannot pass it by accident.

## What I am disregarding, and why

**AC-2 in full.** It is unsatisfiable as stated, protects a property no user holds, and
entrenches the duplication the program's own boundary analysis flags as unresolved. Replace
with: *the screen profile of the unified writer produces the same picture as before —
asserted structurally (same code path, same role set) plus the existing
`exportingTwiceIsByteIdentical`.*

**AC-1's "committed golden" as the whole of the criterion.** Keep the golden; add the
diff-locality criterion above, without which the golden does not serve the reason #709 gives
for wanting it.

AC-3 (headless reachability) is right and nearly free — `-i` is already headless and
`CliImageExportTest` already exercises it; the print profile should be a flag on that same
surface (`-i --profile=print out.svg`), not a new CLI verb. AC-4 (totality via #723's
ratchet) is right and is the strongest AC in the set; under reframing A it rides
`ElementRegistry` rather than policing 29 hand-written print renderers.

## Scope, honestly

Two fixtures this task's ACs depend on — the hazard-demo circuit and the palette-sweep
circuit — exist nowhere in the tree; #723 owns the second, nothing owns the first. And #508
placed CAP-24 in "cheap slice now, rest gated" with a planning ratchet ("no new tier:feature
until two capstones close") that this chain sits awkwardly against, with no instructor yet
on record asking for the figure. The reframed #709 is genuinely the cheap slice: role tags
on the existing renderers, a print profile, a CSS-classed emission with element-local
transforms, one golden, one diff-locality test. The second writer is not.

## Restated task

*The existing SVG export gains role tags, element-local groups keyed by JLS element id, and
a profile parameter; a `print` profile drops chrome roles and selects #723's print symbols;
one committed golden plus a diff-locality test guard it; the `screen` profile is the same
code and keeps producing the same picture.* Same outcome, one exporter, and the seam left
open by #536's boundary comment gets answered by construction instead of by policy.
