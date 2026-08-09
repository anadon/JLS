# Issue #596: FEAT-C37-5: the small stuff the incumbents' users waited eight years for lands in catalog-score order — wire coloring, monospace text, viewport polish — and the bucket closes instead of running forever
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of ceremony, #596 is two different things wearing one label. The first is
three concrete render-layer wins (wire colour, monospace text, viewport polish).
The second is a *governance device*: a per-item stop-loss, a score order, and a
closing condition, invented because CAP-37 (#521, KC-37-2) correctly fears that
"ergonomic polish has no natural end." The governance half is the reason the issue
exists. The three items are almost incidental — the body names them and then spends
most of its length fencing them.

That split matters, because when you check the three items against the tree, the
feature's substance nearly evaporates while its process overhead stays fully priced.

## The load-bearing finding: the #316 gate is aimed at the wrong seam

The body and the pass-2 comment both make "nothing lands inside `SimpleEditor`" a
hard gate, and make #316 (which is itself blocked by #337 and #317) a predecessor.
But #316's residual is specifically the **nine-state mouse machine** —
`src/jls/edit/SimpleEditor.java:770-789`. Rendering left that class already:

- `src/jls/edit/ElementRenderers.java` is a class-keyed render registry (#77), with
  ~30 `*Renderer` collaborators including `WireRenderer.java`, `TextRenderer.java`,
  `WireEndRenderer.java`, `CircuitRenderer.java`.
- `src/jls/edit/Viewport.java` (449 lines, #74) already owns zoom/pan as a *pure*
  class — its own javadoc says "deliberately pure … so the whole coordinate contract
  is unit-testable under `java.awt.headless=true`", with a recorded adjudication for
  wheel zoom, the 25/50/…/400% ladder, and fit.

So all three named items live in collaborators that #316 *already produced elsewhere*.
Wire colour is `WireRenderer.draw` plus `Theme`. Monospace is a font role. Viewport
polish is `Viewport` plus a few call sites. KC-37-1 was written against the god class
of 2026-07; applied literally in 2026-08 it parks the project's cheapest user-visible
wins behind a five-month refactor of the mouse handlers, which those wins never touch.
**I am disregarding the "ordering_after #316" gate for renderer-local items**, and would
keep it only for anything that must add or alter a gesture.

## Item by item, the reframing

**Monospace text (Digital #1129-class) — already shipped where the issue points it.**
`src/jls/elem/Text.java` persists `fontName`/`fontSize`/`isBold`/`isItalic`, and
`TextDialog.java:170` populates the family combo from
`getAvailableFontFamilyNames()`. A user who wants monospace annotation picks
"Monospaced" today. The row as written should grade HAVE.

The real gap is one level up and more valuable: **JLS has no font *role* at all.**
`grep -rn "new Font(\|deriveFont" src/` finds fonts constructed in exactly three
places (`TextDialog`, `TextRenderer`, `SwingTextMetrics`) plus proportional
`deriveFont` scaling in `AdderRenderer`/`StopRenderer`. Every *value-bearing* surface
— `MemoryContentsDialog`, `TruthTableEditor`, `Trace` (`:237` reads the ambient
`FontMetrics`), probe labels, batch-echo panes — renders hex digits and bit columns in
a proportional L&F font, so columns of values do not line up. That is the complaint
behind every "monospace" wishlist item in this tool category. The elegant route is a
`Theme`-adjacent **font role** (`Theme` is already the "semantic role, not a raw value"
seam; §Look-and-feel in ARCHITECTURE.md records the FlatLaf substrate), applied to
value surfaces only, pinned by a headless role-assignment test. That is smaller than
the catalog machinery required to authorize it, and it makes the stated row obsolete.

**Wire colouring (Digital #1308) — the comment is right that it collides; the way out
is to stop treating it as colour.** Both readings the comment names are bad: state
colouring belongs to #542's registry, and user-assignable per-wire paint is durable
presentation state in `.jls`, which #76 §4 invariant 2 forbids for presentation work.
But neither reading is what users actually want. The underlying complaint is *"I cannot
tell which net is which in a dense schematic."* JLS already models net identity:
`WireNet`, and `JumpStart` carries a shared signal `name` (`src/jls/elem/JumpStart.java:30`).

Concrete alternative: **derived net-identity encoding, not stored wire colour.** Colour
(or better, a thin casing/halo drawn *under* the state stroke) is assigned
deterministically per `WireNet` from a fixed, small categorical ramp — keyed on the net's
name where one exists, on a stable net index otherwise. Properties:

- **No format change.** Nothing durable is added; `#76` invariant 2 survives intact and
  the row stops needing an owner for a format decision nobody is scoped to make.
- **No collision with #542.** State keeps the channels it already owns —
  `WireRenderer.strokeFor` gives HiZ a dash and non-zero a 3px stroke. Identity takes a
  separate geometric channel (the casing), so #542's totality test and the ≥25 delta-E
  floor are unperturbed; the identity ramp is verified once, as a set, by the same
  `ThemeTest` apparatus.
- **Headlessly assertable** and, being derived, idempotent across save/load — the
  strongest possible answer to AC-3.
- It is strictly *better* than Digital's own feature: users get the grouping benefit
  without hand-painting hundreds of segments.

If the maintainer wants user control, the smallest honest form is a per-net *override
in preferences* (`UserPrefs` already exists, #76), not in the circuit file.

**Viewport polish — undefined, and half of it is already someone else's.** `Viewport`
shipped with its behaviour adjudicated. What remains is call-site wiring and
persistence — and "per-window-zoom preference key" is explicitly one of #76's three
*planned tasks*. As written, this row's most likely landing is a duplicate of #76's
prefs task rather than anything #592 scores. Either name the residual concretely
(pan-drag gesture? fit affordance in the menu? zoom-to-selection?) or delete the row
and let #76 finish its own roster.

## The structural claim

The pass-2 comment concedes the real problem: this feature is defined **by subtraction**
from #593/#594/#595/#570, and AC-1 is enforceable "only if #592's rows are both
exhaustive and disjoint." A funding unit whose scope is a set difference over four
sibling issues, gated on a document that does not exist, gated in turn on a refactor
blocked by two other features, is an enormous apparatus for three render edits — two of
which dissolve on inspection and one of which belongs to #542/#76.

The project's actual arc, as ARCHITECTURE.md records it, is *seams and registries*:
element renderers (#77), the save-tag table (#79), the extension-point catalog (#223),
`Theme` roles (#76), `Viewport` (#74), and a habit of writing decisions down with
revisit triggers. Work that rides an existing seam strengthens that arc. #596 does the
opposite: it invents a parallel governance track that makes the *cheapest* changes the
*most procedurally expensive* ones in the repository.

## What I would do instead

1. **Dissolve the bucket as a funding unit.** Re-home its content:
   - font role → a task under **#76** (it is presentation-role work, same seam as #286);
   - net-identity encoding → a row under **#542**, whose registry and delta-E apparatus
     it must ride anyway;
   - viewport residual → **#76**'s prefs task, or a named `Viewport` task, or deleted.
2. **Keep the stop-loss, move it out of an issue.** KC-37-2's 1.5× rule and
   "refuse by name, in writing" are the genuinely valuable inventions here. They are
   *policy*, and policy does not close. #592 AC-3 already makes the stop-loss a catalog
   *column*; put the enforcement note in CONTRIBUTING.md next to the recorded-decisions
   habit, and let the catalog be the forever-artifact instead of an issue that must
   pretend to terminate.
3. **Keep, verbatim:** AC-2's refuse-by-name discipline and AC-3's
   "test that fails at the pre-change commit." Those are the two criteria that would
   survive any restructuring.
4. **Fix the owning-feature column on #592** as the comment recommends — that is the
   one upstream change that makes any of this schedulable.

## Criteria I am explicitly disregarding

- **AC-1 (catalog-only, score order)** for the two items that are already implementable
  in existing seams. Requiring a not-yet-written scorecard before a one-file font-role
  change is process cost with no risk being bought down.
- **The #316 hard gate**, per the seam argument above — it should bind gestures, not
  renderers.
- **AC-5's cost ratchets.** `grep -rl "budget\|Ratchet" test/` finds the architecture,
  headless-core, notification and dialog-coverage ratchets — there is no per-edit or
  startup cost ratchet in the tree. An AC cannot assert that a nonexistent ratchet is
  "unmoved"; either build it (a real, reusable contribution) or drop the clause.

## Bottom line

Three-quarters of #596's named scope is either already shipped, owned by #542/#76, or
undefined. Its durable contribution is a funding discipline that should be a written
policy on the catalog, not an issue. Rethink the unit: keep the discipline, redirect the
items to the seams that already exist, and ship the net-identity casing as the item that
actually answers the eight-year complaint.
