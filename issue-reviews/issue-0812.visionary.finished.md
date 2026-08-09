# Issue #812: TASK-C596-2: text elements render monospace where monospace is what the content needs — Digital #1129-class, scored and pinned
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What the issue is really for

Stripped of the catalog ceremony, #812 asks for one thing: *a bit pattern typed
into a schematic annotation should line up.* Everything else in the body —
score order, estimate/actual, KC-37-1, KC-37-2 — is the bucket's containment
machinery (#596), not the user's need. So the question for this lens is: what
is the smallest, most durable change that makes annotation text line up, and
does the change the issue describes sit at the right seam?

## The decisive fact: the feature is already shipped

`Text` has carried a per-element font family, size, bold, italic and color
since before the fork, and all five are already persisted through the
declarative `Attribute` machinery:

- `src/jls/elem/Text.java:19-29` (fields), `:294-383` (the `fn` / `fs` /
  `bold` / `ital` / `color` save rows)
- `src/jls/edit/TextDialog.java:169-179` — the family combo is populated from
  `GraphicsEnvironment.getAvailableFontFamilyNames()`, which includes the JDK
  logical families (`Monospaced`, `Serif`, `SansSerif`, `Dialog`,
  `DialogInput`) alongside every installed physical family
- `src/jls/edit/TextRenderer.java:39` — `new Font(getFontName(), style, size)`
  at draw time, which is also the path `-i out.svg` renders through
  (`src/jls/edit/CircuitRenderer.java:312-358`, JFreeSVG)
- `test/jls/edit/TextMetricsParityTest.java` already asserts `Provider`
  metrics for the family literally named `"Monospaced"`

A student who wants a monospace annotation today selects `Monospaced` in the
text dialog. It persists. It renders with equal advance widths in the editor
and in PNG/SVG export. AC-1 and AC-2 of this issue describe HEAD. Funding a
task to build them is funding a re-implementation, and its "test that fails at
the pre-change commit" (AC-4) can only be made to fail by inventing a *second*
way to say the same thing — a `mono` boolean beside `fn` — which is strictly
worse: two representations of one property, a new save row, a new dialog
control, and a new "what if `fn=Serif` and `mono=1`" question. That is the
opposite of K9 and of #596's own "no new default-visible complexity".

## The real gap, which #812 does not name

The gap is not *monospace*, it is *font identity portability*, and it is a live
defect:

`SwingTextMetrics.defaultFontName()` (`src/jls/edit/SwingTextMetrics.java:82-84`)
returns `g.getFont().getFamily()` — the **resolved** family of the canvas font.
Under the FlatLaf default (ARCHITECTURE.md, "Look-and-feel", #153) that is the
platform UI face: Segoe UI on Windows, a GTK face on Linux, the system face on
macOS. `Text.init` (`Text.java:88-90`) writes that string into the saved `fn`
field the first time an annotation is created. Consequences, none filed:

1. A circuit annotated on Windows saves `fn Segoe UI`. Opened on Linux,
   `new Font("Segoe UI", …)` silently falls back to `Dialog`; metrics change,
   the element's width/height change, columns drift, and the *file bytes*
   change on the next save. Two students, one circuit, different bytes.
2. The same mechanism makes #536 AC-2 unreachable for any circuit containing a
   `Text` element: byte-identical SVG "across the three CI platforms" cannot
   survive a per-element handle to whatever fonts the machine happens to have.
   #812 AC-3 asks monospace text to "compose with the bundled-font determinism
   of #536" — but the composition problem is not created by monospace, it is
   created by the free-form `fn` field that already exists, and #536 has not
   landed. AC-3 is therefore untestable at funding time, and #812's
   `ordering_after` (only TASK-C596-1) does not admit that.
3. Choosing a face from `getAvailableFontFamilyNames()` means the editor's most
   user-visible dropdown has machine-dependent contents — unbounded, untestable
   by the #91 harness, and empty-ish in the minimal containers the README warns
   about ("text rendering needs at least one installed font").

## Reframing A (primary): own the text stack; three symbolic faces, bundled

I am explicitly disregarding acceptance criteria 1, 2 and 4 as written. The
change worth making is:

**`Text` stops naming OS fonts and names roles.** `fn` accepts a small closed
vocabulary — `@sans`, `@serif`, `@mono` — each bound to a font file bundled in
the jar, measured through the existing `jls.core.TextMetrics` seam against the
bundled face rather than against `Graphics`. Legacy physical names keep loading
(they are just another `fn` string); new elements are created with `@sans` and
the ambient-font default at `Text.java:88-93` goes away.

What that single move buys, in one stroke:

- Monospace, as one row of a three-row table — #592's `Digital #1129` row
  closes as HAVE at ~zero marginal cost, which is the honest score.
- The drift bug above disappears: a circuit renders identically on every
  machine, which for a tool whose README sells reproducible jars, byte-identical
  BOMs and autograder containers is not polish, it is the house style.
- #536 AC-2 becomes reachable rather than aspirational: with no OS font in the
  render, `FigureDeterminismTest` can assert `Text` too, not just symbols.
- No format change (#76 §4 invariant 2, cited in #596's own comment on the
  wire-coloring collision): `fn` already exists and already round-trips
  strings. `Attribute.omitted()` (`src/jls/elem/Attribute.java:145`) preserves
  byte-identity for untouched circuits without a new row.
- The dialog's font combo shrinks from "every family on this machine" to three
  radio buttons — testable by the #91 harness, stable in headless containers,
  and less default-visible complexity than today.
- It lands entirely outside `SimpleEditor` (KC-37-1 satisfied by construction:
  `Text`, `TextDialog`, `TextRenderer`, `SwingTextMetrics`).

That work belongs to **#536**, not to #596's long tail. #536 already declares
"owning text metrics via a bundled deterministic font path — no OS font
fallback in the render" as its AC-2 and prices text metrics as most of its
band. `Text`'s free-form family is the single hardest case for that claim.
Doing it there once means #812 never needs to be worked; doing it here means
doing it twice, the second time as a compatibility fix.

## Reframing B (secondary): make alignment structural, not typographic

The three examples in the outcome — truth-table fragment, bit pattern, register
layout — are *tables* that users are hand-aligning with spaces. Monospace makes
hand-alignment survivable; it does not make it good (no rules, no headers, no
column widths, breaks on edit). A `Table`/legend annotation whose columns are
laid out by the renderer would serve the stated need better and would be
font-agnostic. I rank it second — `Text` is deliberately dumb and a new element
is sixteen touch points (ARCHITECTURE.md, "Adding an element today") until #78
lands — but it deserves a REFUSE-with-reason row in #592 rather than silence,
because it is the reason "add monospace" feels small and still does not close
the underlying complaint.

## Alignment with the project's arc

The trajectory is unmistakable: `jls.core` free of AWT (#77), a headless
simulator ratchet, byte-reproducible jar and BOM, provenance attestations,
deterministic figure export (#536), a container image for autograders. JLS is
becoming a tool whose output is *the same everywhere*. A per-element pointer
into the host's font catalog is the last unpinned nondeterminism in the
geometry model — it even leaks into saved bytes. #812 as written walks past
that leak and adds a switch beside it. Reframed as "faces are ours, and there
are three of them", the same user-visible outcome arrives while the arc gets
straighter.

## What I would keep

The containment discipline (score order, estimate/actual, 1.5x stop-loss,
outside `SimpleEditor`) is the right medicine for #596 and should survive
verbatim. What should change is the row it is applied to: score
`Digital #1129` as **HAVE-today, portability-blocked**, cross-reference it to
#536, and spend the 0.5–1 mw on the symbolic-face vocabulary under #536 where
its determinism test already has a home.
