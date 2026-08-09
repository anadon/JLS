# Issue #749: TASK-C546-2: a tactile SVG sized for swell paper, with a lint that enforces the BANA line-width and spacing rules by cited edition
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#749 is the tactile-SVG half of TASK-C546-1 (#747, prose narrative), which is
itself a task under FEAT-C26-4 (#546), a planned feature of CAP-26 (#507).
The issue is cleanly scoped against its siblings (#540/#536 own camera-ready
print, not duplicated here) and its falsification requirement (AC-3) is a
solid instance of the project's red-run discipline. But it inherits two
unresolved risks from its parent without restating or resolving them
(cross-platform determinism, BANA edition/licensing), and it leaves the
single biggest question a tactile-graphics deliverable has to answer —
whether and how the diagram carries any text a blind user can actually
read — completely unaddressed, which is exactly the kind of gap where the
stated lint could go green while the artifact stays useless to its audience.

## Findings, most severe first

### 1. The lint's stated scope never touches labeling, so a lint-clean SVG can still be unusable to a blind reader

AC-2 defines the lint as enforcing "line widths, minimum spacing and
permitted symbol substitutions." BANA's actual tactile-graphics guidelines
also govern how text is represented on a tactile diagram — print text is not
tactilely readable at all, so labels normally have to be braille or omitted
in favor of a legend keyed to the accompanying narrative. The issue never
says which: does the tactile SVG carry pin/gate labels as embossed braille
cells, as a lettered/numbered key cross-referenced to #747's prose narrative,
or not at all? `CircuitRenderer.exportImage` (`src/jls/edit/CircuitRenderer.java:314-358`)
currently draws element names with ordinary `Graphics2D.drawString` — if that
text path is reused unmodified for the tactile theme, the exported SVG would
visually show gate/pin names as printed glyphs that mean nothing under a
fingertip, while still passing a lint that never checks labeling at all. This
is a case where the stated acceptance criteria can pass while the feature's
actual purpose (something a blind student can use) fails, and it is the one
question this issue most needed to answer explicitly.

**Recommendation:** State in the issue whether labels are braille, a
narrative-keyed legend, or deliberately absent, and add a lint rule (or an
explicit stated exclusion, citing why it's out of scope) covering whichever
choice is made.

### 2. The cross-platform byte-determinism requirement CAP-26 imposes on this exact deliverable is not in #749's acceptance criteria at all

CAP-26 AC-6 (`AccessibleExportDeterminismTest`) requires "Accessible exports
byte-identical across CI platforms," and #546 (the parent feature) repeats
this as its own AC-4. #749's five acceptance criteria say nothing about
determinism. That omission matters here specifically because the rendering
path this task will almost certainly reuse or extend —
`CircuitRenderer.java:314-358` — draws text via the platform's default font
(`gg.getFontMetrics()`/`drawString`, no bundled font anywhere in the tree;
confirmed by a repo-wide search), which is the exact mechanism the sibling
review of #546 identified as the reason byte-identical SVG across CI is not
free (see #536's own cost note: "text metrics is most of it"). If #749 draws
any text (symbol legend, page-format metadata, title), it inherits that risk
silently; if it doesn't, the issue should say so rather than leaving the
determinism requirement unstated at the level where the actual rendering
choice gets made.

**Recommendation:** Either add an explicit AC restating CAP-26 AC-6 for this
task's own SVG (and confirm it draws no OS-fallback text), or state plainly
that the tactile SVG is glyph/path-only with no drawString calls, so the
determinism inheritance is verifiable rather than assumed.

### 3. BANA edition is still unpinned, and the copyright question on encoding its numeric rules is still unaddressed — same gap as the parent, unresolved at the task level

AC-2 requires "every rule cites the guideline edition it comes from," but the
issue body itself never names an edition (BANA's *Guidelines and Standards
for Tactile Graphics* has multiple published editions). This is the same gap
flagged against the parent issue #546 (which also never pins an edition
despite CAP-26 OQ-3 explicitly naming "the recommended default"). A task two
levels below the capstone is the right place to actually pin the concrete
number, and it doesn't. Separately, BANA's guideline documents are a
copyrighted commercial publication, not a public standard like WCAG; encoding
specific numeric line-width/spacing thresholds into GPL-3.0-or-later lint
source plausibly needs either a permissions check or an independently-derived
measurement basis, and nothing in this issue (or #546) mentions either.

**Recommendation:** Name the exact BANA edition/printing in this issue's text
before implementation starts, and add a line requiring a licensing check (or
independent re-derivation of the numeric thresholds) before any BANA-sourced
constant lands in lint source.

### 4. "Sized to a stated swell-paper page format" names no format, so AC-1 can be trivially satisfied without matching what a disability-services office's embosser actually takes

Swell-paper (microcapsule/PIAF-style) sheets ship in specific fixed sizes
(commonly 11×11.5 in in the US); AC-1 only requires "a stated swell-paper
page format" — any self-declared format, including one invented for the
test fixture, satisfies the letter of the criterion. There is no reference
to an actual embosser/paper spec in the issue, and no existing `PageFormat`
infrastructure in the tree targets non-standard paper sizes (`grep` over
`src/jls/JLSStart.java` shows only the ordinary `PrinterJob`/`PageFormat`
path at `:2408-2409`, oriented at normal print, not swell-paper stock).

**Recommendation:** Name the target sheet size(s) (or a small enumerated set)
explicitly in the issue, sourced from an actual swell-paper product spec, not
left to be invented at implementation time.

### 5. "One command" is new CLI surface with no design and no line item in the cost band

`JLSStart.java`'s `FLAGS` table (`:759-787`) has no tactile-export flag today,
and CLI flag changes elsewhere in this project routinely require updates to
`CliFlagTableTest` (`usageDocumentsExactlyTheParserFlags`) and, depending on
scope, `docs/batch-interface.md`. AC-1's "one command" implies a new flag (or
a documented extension of `-i`) but the issue specifies neither the flag name
nor whether it reuses `-i` with a theme selector, and the 1.5-2 mw band has no
visible allowance for CLI plumbing, usage-table updates, or flag-table test
changes.

**Recommendation:** Name the concrete CLI surface (new flag vs. `-i` theme
option) in the issue text, and confirm the band accounts for
`CliFlagTableTest` updates.

### 6. The registry-keyed state-encoding dependency this task actually needs is not declared here — only inherited transitively through a sibling task

#546 (the parent) states plainly that the tactile output "consumes FEAT-C26-1's
registry-keyed state-to-encoding data" (i.e., TASK-C542-2, filed as #731) for
depicting state via touch-appropriate symbol substitutions — that's exactly
what AC-2's "permitted symbol substitutions" needs. But #749's own
`ordering_after` names only `[TASK-C546-1]` (#747, the prose-narrative task),
not #731 directly. The dependency is satisfied today only because #747
happens to declare `ordering_after: [TASK-C542-2]` itself — an implicit,
two-hop chain that a future re-ordering of #747 (a narrative task that has no
intrinsic reason to gate on the SVG task's own dependency) could silently
break.

**Recommendation:** Add `TASK-C542-2` (#731) directly to #749's
`ordering_after`, rather than relying on it arriving for free through a
sibling task's unrelated ordering.

## What's solid

- AC-3's falsification requirement ("a deliberately too-thin line or
  too-tight spacing fails the lint, with that red run recorded") is a clean
  instance of the project's standing red-run-first discipline and needs no
  rework.
- The non-duplication boundary (AC-5: this is the accessible bundle, #540/#536
  own camera-ready print) accurately reflects both referenced issues' actual
  stated scope — confirmed by reading #536 and #540 directly, no
  contradiction found.
- The division of labor against #747 (narrative vs. tactile SVG as separate
  tasks under one feature) is a reasonable decomposition and matches #546's
  own AC-1 ("one command emits both").

## Process note

Per AC-4, CAP-26 Open Question 3 ("which tactile-graphics guideline set does
the lint encode?") is marked as still blocking PF-4's filing, and this task
was filed anyway against the recommended BANA default with an explicit
REPLAN escape valve on #507. That is consistent with how #546 itself was
filed, but it means the same unresolved capstone-level question is now
load-bearing on two issues (#546 and this task) rather than one — any
REPLAN that changes the guideline choice invalidates work done against this
issue's current acceptance criteria, not just its parent's.
