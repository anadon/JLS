# Issue #813: TASK-C596-3: viewport and zoom polish lands last, and the bucket closes with an estimate-versus-actual record per item instead of running forever
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Two unrelated things are fused into one task. (a) The last scored item of the #596
long-tail bucket — viewport/zoom ergonomics. (b) The *closing discipline* for the whole
bucket: an estimate-versus-actual ledger that makes KC-37-2's 1.5x stop-loss mechanical
rather than aspirational, so #596 terminates instead of becoming the forever-bucket
#521 explicitly fears.

(b) is the more valuable half and is the reason the issue exists at all. (a) is, at
HEAD, mostly already shipped. That asymmetry is what the reframing below is about.

## Ground truth at HEAD

- #74 is **closed as completed** and the view transform landed: `src/jls/edit/Viewport.java`
  (449 lines) is a pure, AWT-component-free, headless-testable object with scale, pan,
  ladder stops, `fit(bounds, w, h)`, `toModel`/`toScreen` in both point and rectangle
  forms. `test/jls/edit/ViewportTest.java` is 494 lines of property tests over it.
- The editor already has: Zoom In / Zoom Out / Actual Size / **Fit to Circuit** with
  accelerators (`SimpleEditor.java:1797-1861`, key bindings `:1494-1554`), Ctrl/Cmd+wheel
  zoom-at-cursor, plain wheel = vertical scroll, Shift+wheel = horizontal
  (`mouseWheelMoved`, `:3908-3928`), space-drag / middle-drag pan (`panning`/`spaceDown`,
  `:1143-1152`), and canvas auto-grow replacing the retired 10% button (`autoGrow`, `:2066`).
- So of the three things AC-1 names — "zoom-to-selection, zoom-to-fit, scroll and pan
  behaviour" — **two are already in the product**. A catalog row that scores zoom-to-fit
  as a GAP is scoring a July-2026 commit, and the ledger this issue demands would book
  its estimate and actual against work that already shipped.

## The collision the issue never names

`EditWindow` is a **private inner class of `SimpleEditor`** (`:1121`). Every piece of live
viewport behaviour lives inside it:

- `applyZoom` (`:2137-2172`) re-derives the zoom-at-cursor fixed-point math *itself*, against
  `pane.getViewport()`'s scroll position, and calls `viewport.zoomTo(clamped, 0, 0)` —
  deliberately discarding the anchor the `Viewport` class was written to handle.
- `zoomFit` (`:2209-2243`) re-implements `Viewport.fit` inline, with its own margin, its own
  clamp, and its own `setViewPosition` centering.
- `Viewport.fit(...)` and `Viewport.pan(...)` have **no production callers at all** — grep
  finds them only in `ViewportTest`. The class's translation fields are permanently zero
  by design comment (`:1124-1132`): "Panning is handled by the enclosing `JScrollPane`."

So there are two coordinate authorities: `Viewport` owns scale, Swing's `JViewport` owns
translation, and the reconciliation arithmetic lives in a 5,852-line class that #316/#84
exist to decompose. AC-4 of this issue says nothing lands inside `SimpleEditor` — but
**zoom-to-selection cannot be written anywhere else today**, because the selection set,
`selectionBounds()` (`:3663`), the scroll pane, and the scale all live in that inner class.
As written, AC-4 is unsatisfiable, and the likely outcome is either a KC-37-1 violation
recorded as a "process violation" per AC-3, or the item is "refused with a reason" — which
would refuse the one genuinely missing feature in order to satisfy a boundary rule.

## The reframing: finish #74's hypothesis instead of polishing around it

#74's stated hypothesis was "one `AffineTransform` (scale s, translate tx,ty)... inverted
once at each mouse-event entry point." The implementation delivered half of it and handed
translation to `JScrollPane`. Restore the other half and the polish items stop being items:

1. **Let `Viewport` own translation.** The canvas component becomes a fixed-size window onto
   an unbounded model plane; `Viewport.translate` is the pan. Scrollbars become a *view of*
   the viewport (set from model bounds ∪ visible rect on each change), not the source of truth.
2. Then, with no new math anywhere:
   - zoom-to-fit is `viewport.fit(circuit.getBounds(), w, h)` — already written, already tested;
   - **zoom-to-selection is `viewport.fit(selectionBounds(), w, h)`** — the same call with a
     different rectangle, and `selectionBounds()` already exists for dirty-region work;
   - pan is `viewport.pan(dx, dy)`; zoom-at-cursor is `viewport.zoomBy(f, ev.getX(), ev.getY())`,
     the method whose anchor parameter is currently thrown away.
3. **`modelSize` and `autoGrow` disappear.** The 1000-px "canvas that grows" is a 1998 relic
   that only exists because the scroll pane needs a preferred size. With translation in the
   viewport there is no canvas size to grow — there is a model bounding box and a margin.
   That is ~60 lines of state and edge-case logic deleted from `SimpleEditor`, not added.
4. `applyZoom` and `zoomFit` collapse to a couple of lines each, and the interesting behaviour
   moves into a class that is already headless — which is what makes AC-4's "pinned by a
   headless test" cheap rather than a display-lane negotiation.

This is a net *reduction* in `SimpleEditor` while delivering the feature, which is the only
shape of work that both honors KC-37-1 and does not have to wait for #316's nine-state
extraction. It also pays forward: #74 §12's minimap, the HiDPI device-scale factor that
`Viewport`'s javadoc already reserves a seat for, and #62's fit-after-ELK-import all want
exactly one object that answers "what part of the model is on screen."

## If that is judged too large: the minimum viable seam

Extract the five view methods (`applyZoom`, `zoomFit`, `visibleCenter`, `applyPreferredSize`,
`autoGrow`) into a package-private `CanvasView` collaborating with `Viewport` through a
three-method `ScrollPort` interface (`getExtent()`, `getPosition()`, `setPosition(Point)`),
implemented by Swing's `JViewport` in production and by a plain fake in tests. That is a
half-day, keeps the current two-authority design, and still makes every zoom/pan behaviour
assertable headlessly and satisfies KC-37-1 literally. It is strictly worse than the
reframing above — it institutionalizes the dual authority — but it unblocks the item today.

## On the ledger half — I am disregarding AC-2 and AC-3 as written

A separate "committed record" for #596 duplicates infrastructure #592 is already required to
build: #592 AC-3 says the stop-loss "is expressed as a column the catalog itself can be read
against." The estimate/actual/disposition record should therefore be **three more columns in
the #592 catalog document**, updated as each row is worked, not a second file that has to be
reconciled with the first. One artifact, one source of truth; a row's grade, score, estimate,
actual and disposition read left-to-right. Otherwise the project acquires a scorecard and a
ledger that will disagree, and the first thing a reader must do is work out which one is true.

Second: the bucket's closing discipline is **coupled to the last implementation item's
completion** by being carried on this task. If viewport polish stops out at 1.5x, the very
record that is supposed to document the stop-out is inside the stopped task. Decouple them —
the closing record is a zero-code close-out comment/commit on **#596 itself**, with no
dependency on whether item three landed. That is also what makes "the feature ends when the
scored set is exhausted" enforceable: exhaustion is a property of the catalog, not of #813.

## Concrete residue worth scoring (what is actually missing)

If the catalog is re-derived against HEAD rather than against #74's pre-implementation text,
the honest GAP list for this row is short and should be stated as such:

- **zoom-to-selection** — genuinely absent; one `Viewport.fit` call away under the reframing.
- **no zoom readout** — `getZoomScale()` exists (`:669`) but nothing shows the user "150%";
  every incumbent shows it, and it is the cheapest switcher-legibility win here.
- **trackpad scrolling drops sub-unit deltas** — `mouseWheelMoved` uses `getUnitsToScroll()`
  for plain scroll while using `getPreciseWheelRotation()` for zoom (`:3910-3927`); on
  high-precision devices the integer path stutters. Real, small, and testable.
- **per-window zoom persistence** — #74 §7's last checkbox; unlanded, and reasonable to
  REFUSE by name rather than build.

Everything else in AC-1's phrasing is shipped. Saying so in the catalog is worth more to
#521 than landing it a second time.

## Verdict

**endorse-with-reframing.** The closing-ledger intent is right and should survive; fold it
into #592's catalog columns and hang it on #596 rather than on the last code task. The
viewport half should be rescoped from "polish items" to "give `Viewport` the translation it
was designed for," which deletes `SimpleEditor` state, makes zoom-to-selection a one-liner,
turns AC-4's headless-test requirement from an obstacle into a consequence, and leaves the
project with the single view authority that the minimap, HiDPI, and second-canvas work will
all need anyway.
