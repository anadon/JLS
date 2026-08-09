# Issue #749: TASK-C546-2: a tactile SVG sized for swell paper, with a lint that enforces the BANA line-width and spacing rules by cited edition
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

The end is unambiguous and I endorse it without reservation: **a blind student's
disability-services office receives a file, prints it on swell paper, and the
student's fingers can read the circuit they are being graded on.** CAP-26 (#507)
is right that no simulator in the category offers this, and #749 is the half of
that path that survives leaving the building.

But the issue is written as *stylesheet + lint*, and that is the wrong shape for
the end. Below, I disregard acceptance criteria 1, 2, 3 and 5 as stated — not
the outcome they serve — and propose a different seam.

## 1. The lint has nothing to enforce, and the thing it can't enforce is the whole problem

Split the BANA rule families the issue names:

- **Line width** is a *constant of the emitter*. If the tactile stylesheet writes
  `stroke-width` from one named constant, no output the pipeline produces can
  ever fail the width rule. AC-3's "deliberately too-thin line fails the lint"
  therefore exercises a hand-built fixture, not the product. That is a test of
  the test.
- **Symbol substitution** is likewise a property of the symbol table: total by
  construction or missing entirely (see §3), never "wrong width".
- **Minimum spacing** is the only rule with real content — and it is not a
  property of the style at all. It is a property of the **layout**, which this
  issue inherits unexamined from the editor canvas.

And there the arithmetic is unkind. `jls.core.Geometry.SPACING` is 12 px; a lab
adder occupies many hundreds of pixels of drawn extent, and swell paper gives
roughly 10 usable inches. BANA's tactile separation floor (order 1/8 in between
adjacent raised elements, more between a line and a braille cell) means a page
holds on the order of 60–70 discriminable positions per axis. A circuit drawn on
a 12 px grid, uniformly scaled to that page, violates spacing the moment it has
more than a handful of elements and a couple of wire crossings — and JLS wire
routing is orthogonal with crossings by design.

So the lint's honest verdict on most real student circuits is **FAIL**, and the
feature as scoped ships a gate the user has no way to pass. A lint that says no
and offers no remedy is not an accessible export; it is a refusal with a
citation.

**Reframing:** the deliverable is a **tactile layout engine**, and compliance is
*by construction* — the emitter places marks on a tactile grid whose pitch is
the BANA minimum, so spacing cannot be violated. The lint survives, demoted to
what a ratchet is actually good for: a regression check that the constructive
invariant still holds, run over the whole example corpus. That inverts the
issue's title, which is why this is `rethink` and not `endorse-with-reframing`.

## 2. Nobody has decided what happens to the text — and swell paper decides it for you

The issue says "symbol substitutions appropriate to touch rather than to sight"
and never mentions labels. Every JLS circuit is covered in them: element names,
pin names, bit widths, state names. Swell paper raises *whatever is black*. Ship
visual glyphs and the student gets raised print letters, which the large majority
of braille readers cannot read. This is not a detail; it is most of the ink on
the page.

Two coherent answers, and the issue must pick one before it is fundable:

1. **Keyed graphic (recommended for v1).** The diagram carries no text at all —
   only short alphanumeric keys drawn as tactile-legible shapes, or nothing —
   and the key is resolved by the prose narrative from TASK-C546-1 (#747). This
   is the elegant move: it turns CAP-26 AC-3's "one command emits both" from a
   packaging convenience into a *semantic dependency*. The two halves stop being
   two renderers that happen to run together and become one artifact whose
   graphic half is unreadable without its prose half — which is exactly how BANA
   graphics work in practice (graphic + facing key page).
2. **Real braille**, via `liblouis` as a subprocess. Defensible, and #222's
   recorded trust/isolation boundary already blesses out-of-process external
   tools (the Yosys/GHDL/ELK pattern), which also keeps LGPL linkage away from
   the GPLv3 jar. But it is a new external dependency on the DSO's side of the
   pipeline and it is not a 1.5–2 mw task.

Pick (1), record (2) with its revisit trigger, and say so in the issue.

## 3. The boundary note is inverted: this needs #536 and #540, it does not avoid them

AC-5 disclaims FEAT-C24-1 (#536) and FEAT-C24-5 (#540) as "not duplicated here."
The disclaimer is aimed at the right risk and lands on the wrong side of it.

- **#540 exists precisely to prevent a second symbol vocabulary** ("no element's
  print symbol is hand-maintained outside the registry mapping"; CAP-24 risk 2:
  "no third vocabulary"). "Symbol substitutions appropriate to touch" *is* a
  symbol vocabulary. Minting it in a tactile-only emitter is the decay mode
  #540's ratchet was written to stop, one element at a time, in a corner of the
  tree nobody sighted will ever look at. Tactile should be a **third theme
  through the one symbol registry** — `ElementRenderers`
  (`src/jls/edit/ElementRenderers.java`) is already a class-keyed registry, and
  it is the seam — with #540's totality test extended to cover the tactile row,
  not a parallel table.
- **#536 owns deterministic text metrics via a bundled font with no OS
  fallback**, which is the only mechanism by which CAP-26 AC-6
  (`AccessibleExportDeterminismTest`, byte-identical across CI platforms) can
  hold for a vector export. `jls.core.TextMetrics` is the seam and
  `jls.edit.SwingTextMetrics` is today's OS-dependent implementation. #749
  inherits AC-6 while disclaiming the only feature that can satisfy it.

`ordering_after` should read `[TASK-C546-1, <#540's task>, <#536's deterministic
renderer slice>]`, and the cost band should be re-derived on the assumption that
those land first — at which point 1.5–2 mw is plausible, and without them it is
not.

## 4. SVG is the wrong physical carrier

Swell paper is fed through an ordinary laser printer and then a heater. The one
property that must survive is **exact physical scale**: the 1/8 in the lint just
certified has to arrive as 1/8 in on paper. SVG reaches the printer through
whatever the DSO happens to open it in, and every one of those paths defaults to
"fit to page" — which silently rescales the artifact and voids the guarantee.
The compliance claim would be true of the file and false of the print.

**A page-sized PDF at exact dimensions is the correct deliverable**, and #536 is
already building a deterministic PDF renderer directly (its AC-3, no SVG→PDF
conversion). Keep SVG as the inspectable, diffable, lintable intermediate —
useful, and the right thing to check into a course repo — but the embossable
artifact should be PDF. The issue's title commits to the wrong noun.

## 5. The tactile floor collides with PF-1's redundant encoding

FEAT-C26-4 says this consumes FEAT-C26-1's registry-keyed state-to-encoding data
"for state depiction in the tactile output." CAP-26 PF-1 defines that encoding as
*line thickness, dash patterns, glyph markers* — chosen for a monochrome
**screen/print** channel. On swell paper: dash patterns near the 1/8 in
separation floor read as a continuous line or as noise, and two line thicknesses
are only distinguishable by touch if they differ by well more than a visual
just-noticeable difference. So the tactile lint, honestly written, would **flag
PF-1's own encoding vocabulary**.

That is a genuine cross-feature finding, not a detail: the tactile channel needs
its own state vocabulary (texture/interruption at tactile scale, or — better —
no state depiction on the graphic at all, deferring live state entirely to the
narrative and to PF-3's spoken output, which is where a *changing* value belongs
anyway). Either way this belongs in a REPLAN on #507 before #749 is worked.

## 6. The conformance-honesty precondition

`grep -rli "tactile\|BANA\|braille\|swell" docs/` returns nothing today. CAP-26
§3 risk 1 and the project's recorded rule in
`docs/standards-adoption/11-costed-rejections.md` are explicit: never claim
conformance to something unread. "Every rule cites the guideline edition" is a
good instinct that, without a companion document, reads to a procurement officer
as conformance to the whole guideline — and a 200-page graphics standard reduced
to three numeric constants is not that.

**Precondition on AC-2:** a `docs/standards-adoption/` entry (the natural
neighbour of `03-accessibility-conformance.md`) recording which edition was read,
which rules are encoded, which are deliberately *not* encoded and why, and what
the claim is in words — "machine-checked against N named BANA rules," never
"BANA-compliant." Then the citation in the lint is a pointer into a document that
exists, and an edition change is the visible diff the issue wants.

## What I would fund instead — the tactile atlas

One command, one bundle, from one traversal:

1. Compute the part-to-whole decomposition **once** — the same tree #747 walks
   for the narrative.
2. Emit **one page per block** on a BANA-pitch tactile grid, each page holding at
   most N marks so spacing headroom is structural rather than checked. Signals
   leaving a page become labelled off-page connectors, the standard tactile
   answer to a diagram too dense for one sheet. Decompose rather than shrink —
   which is what the guidelines themselves recommend, and what CAP-26's own §1
   "part-to-whole" framing already commits to for the prose half.
3. The narrative and the pages **share one key namespace**, so a student reading
   linearly and feeling the page are in the same place.

Restated acceptance criteria: (1) one command emits the narrative and a
page-set PDF (plus SVG intermediates) for any circuit, deterministic and
byte-identical across platforms; (2) tactile geometry is generated on the BANA
grid pitch, so a spacing violation is unreachable — the lint is a corpus-wide
regression ratchet over `examples/` and the palette sweep, with its red run
recorded by perturbing the *pitch constant*, not by hand-editing an SVG; (3) the
tactile symbol row lives in #540's registry and its totality test, with a new
element lacking one failing the build; (4) no visual text on the graphic — keys
only, resolved by the narrative; (5) a `docs/standards-adoption/` entry names the
edition, the encoded rules, and the unencoded ones.

That is a bigger task than 1.5–2 mw. It is also the one that produces something a
student can actually read, and it makes the hardest stated criterion — spacing —
disappear instead of turning it into a wall.
