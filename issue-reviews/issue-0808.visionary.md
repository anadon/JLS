# Issue #808: TASK-C594-2: a recently-used set and full keyboard palette navigation, over #75's shared Action layer rather than a second focus model
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the framing and #808 is the second half of #594's answer to one question: *how does a
student get the part they want onto the canvas without hunting a 32-button icon strip?* Above it,
#521 PF-3 calls that "findability" and #521 AC-4 says the honest measure is a timed task — build a
4-bit counter from scratch, and don't be slower afterwards. That is the goal, and it is a good one.

#808 answers it with two named affordances (an MRU set, keyboard traversal of the palette) plus a
conformance clause (dispatch through #75's `Action` layer). I endorse the goal and want the plan
re-cut. Three things in the tree make the current cut the wrong one.

## 1. The "no parallel accelerator scheme" AC is already false, and that is the opportunity

AC-3 asks for a test asserting no parallel accelerator scheme is introduced. But the palette
already *is* a parallel action family. `EditOp` (`src/jls/edit/EditOp.java`) is a closed enum of 18
**unparameterized verbs** — PROBE, CUT, ROTATE_CW, WIRE_START. The palette's actions are 32
anonymous `AbstractAction`s minted inside `SimpleEditor.makeElement` (`SimpleEditor.java:2380-2430`),
one per `PaletteEntry`, none of them an `EditOp`, none of them in `EditActionMatrixTest`'s matrix.
"Place an AndGate" is not expressible in the shared layer because the layer has no parameter.

So the AC as written can only be satisfied by testing that nothing *new* was added — a ratchet on a
seam that is already split. The valuable move is the opposite: **widen the Action layer to carry a
parameter** so that "place element of type T" becomes a first-class, addressable operation
(`EditOp.PLACE` + `ElementType`, or a `PaletteAction` registry keyed on the registry tag — the
palette already resolves every row through `ElementRegistry.forTag`, `Palette.java:218`). Once
placement is addressable, search (#807), recently-used, keyboard traversal, a future command
palette, macro/replay (#316 TASK-0069's transcript) and rebindable keys (#570) all dispatch *the
same object* instead of each growing its own path into `makeElement`. That is a seam worth cutting;
"assert we didn't add a second scheme" is not.

## 2. The keyboard half is 90% built and anchored to the wrong point

Every palette entry is **already** mirrored into a `JMenu` named `elements`, with an accessible
name and a stable component name (`menu.elements.<tag>`, #210) set per item
(`SimpleEditor.makeElement`, `:2400-2430`). A `JMenu` is arrow-navigable and Enter-selectable on
Swing's own menu focus model — exactly the "not a second focus model" #808 asks for. The reason a
keyboard user cannot reach it is one line of plumbing: that menu is added to `newMenu`
(`:1358`), a `JPopupMenu` (`:1182`) shown *only* from the two mouse handlers
(`newMenu.show(this,sx,sy)` at `:2695` and `:3370`). `grep` for `setComponentPopupMenu`,
`getPopupLocation`, `VK_CONTEXT_MENU` across `src/` and `test/` returns nothing. Register the popup
as the canvas's component popup and override `getPopupLocation` to anchor at the caret, and
keyboard palette navigation exists — on the platform's focus model, with the accessible names
already asserted.

The real defect hiding under AC-2 is the **drop point**. `setup(Element, boolean fromToolBar)`
(`SimpleEditor.java:5358`) picks the drop coordinate as the last tracked *mouse* position `x/y`, or
the viewport centre when `fromToolBar` (`:5402-5412`). The keyboard caret (`caret`, `:1281`) — the
whole point of #75's keyboard construction, "the keyboard counterpart of the mouse pointer" — is
never consulted. A keyboard-only user who has parked the caret gets the part at a stale pointer
position or the middle of the view and must arrow it back. AC-2 ("completable from the keyboard
alone") is satisfiable while that is true, which is exactly the kind of criterion that passes and
ships a bad experience.

**Concrete re-aim:** the invariant to pin is *the caret is the drop anchor whenever the gesture
originated from the keyboard*, headlessly assertable against the caret model. That is one seam, one
test, and it is worth more than the rest of #808 combined. It also does not belong here: it is a
gap in #75's shipped keyboard-construction contract, not a PF-2..5 ergonomic fix, so it can be
re-homed to #75's residual and land *now* rather than waiting behind #316 and the KC-37-1 gate.

## 3. A type-level MRU is the weakest item in PF-3, and the right one is next to it

JLS's placement flow is `setup()` → **creation dialog** (`ElementDialogs.setup`, `:5416`) → place.
What a student repeats when building a 4-bit counter is not "another Register" — it is "another
register configured *exactly like the last one*: 4 bits, positive edge." A type-only MRU saves a
mouse trip to a button that is already permanently visible on the toolbar, and saves nothing on the
dialog, which is where the time goes. It sits in the narrow band between "the button is right
there" and "copy the configured one you already placed" (`EditOp.COPY`/`PASTE`).

**Reframe: replace the MRU with repeat-last-placement.** One new op — place another element
identical to the last placed one, dialog answers reused (or pre-filled) — is one `EditOp`, one
shared `Action`, one accelerator, zero new chrome, zero new model, zero persistence, and it is the
single item most likely to move #592 AC-5's timed 4-bit-counter baseline. If a history longer than
one is wanted later, it generalizes to a *configured*-element ring, not a type ring.

## 4. Merge #808 into #807: one command surface, not three

#807 is building incremental search over element names. #808 adds an MRU list plus keyboard
traversal. These are one widget: a summonable overlay where typing filters, arrows select, Enter
places — and whose **empty-query state is the recently-used list**. That is the quick-open pattern,
and it collapses #807 AC-1/AC-2 and #808 AC-1/AC-2 into a single model with a single focus story.

It also resolves a constraint conflict neither issue notices. K9 (#521 AC-5) forbids new
default-visible chrome; #594 AC-5 restates it. #808 AC-1's "reachable by both mouse and keyboard"
pressures toward a visible MRU tray — new default-visible chrome, on a toolbar the #316 TASK-0105
per-view work is trying to *stop* growing. An overlay is invisible until summoned and costs zero
chrome. If a mouse path is required, the existing right-click popup already provides it once the
recently-used entries are added to the same `newMenu`.

## 5. Two hazards the plan should record before it is executed

- **Do not source search synonyms from `ElementType.aliases()`.** That table exists for *save-tag*
  compatibility across renames (#79, `ElementRegistry` header comment). Search synonyms
  ("flip-flop" → Register, "inverter" → NOT, "demux" → Decoder) are UI vocabulary with a completely
  different change rate; coupling them makes every synonym addition a file-format-adjacent edit.
  Put them on `PaletteEntry`, or better, mine the help tree — every palette row already carries a
  `helpTopic` (`Palette.java:124-188`) into `Map.jhm`, and `HelpTopicsTest` already enforces
  palette-to-topic completeness. The help index gives real prose, real synonyms, and a no-match
  answer that can offer the help page instead of an apology.
- **Scope the model by view from day one.** #316 TASK-0105 adds a `view` dimension to
  `PaletteEntry` for the breadboard canvas (#329). A recently-used model and a search index built
  without it get re-cut when that lands. Key both by view, defaulting to `schematic`.

On persistence: AC-1's "persists across a session" is *weaker* than the infrastructure already in
the tree. `jls.UserPrefs` (#76) wraps `java.util.prefs` with an in-memory fallback for sandboxed
runs; across-restart persistence is nearly free. Pick across-restarts, or state plainly why
per-session is better (shared lab accounts leaking one student's history to the next is a real
reason) — but don't leave the weaker promise as an accident.

## Disregarded acceptance criteria, stated plainly

I am rejecting **AC-1** (build repeat-last-placement instead, and let the search overlay's
zero-query state be the history if one is still wanted), and **AC-3** (widen the `Action` layer to
carry a parameter rather than ratchet against a parallelism that already exists). I am re-aiming
**AC-2** at the caret-as-drop-anchor invariant and re-homing that piece to #75. **AC-4** (model-side
collaborators, not `SimpleEditor` fields) and **AC-5** (test red at the pre-change commit, scored
row in #592) I endorse unchanged — they are the strongest lines in the issue.

## Trajectory check

Nothing here is urgent. #808 sits behind #592's catalog (no rows scored yet) and behind KC-37-1,
whose gate — #316 FEAT-008 — reports at `2d0ca9d` that the nine-state machine is *not* extracted,
`jls.edit` has *no* coverage floor, and TASK-0019/TASK-0020 are *not filed*. The queue depth is the
argument for re-cutting now: by the time this is fundable, executing it as written would have
produced a second action family, a chrome strip fighting K9, and a history list of types nobody
needed — while the caret still drops parts in the middle of the viewport.
