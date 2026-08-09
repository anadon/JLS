# Issue #804: TASK-C593-1: additive and subtractive selection and rubber-band over a mixed element/wire set, landing in the decomposed collaborators and nowhere near SimpleEditor
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

One sentence: *shift-click should extend the selection and ctrl-click should
toggle it, the way it does in every editor a student has ever used.* That want
is real, it is cited (bsiever-fork #18), and it is one of the cheapest parity
wins in CAP-37's whole roster.

The issue then spends four of its five acceptance criteria on something else
entirely: which file the code lives in (AC-4), which document scored it
(AC-1/AC-5), and which not-yet-existing machine asserts it (AC-3). Read the
chain the correction comment records — #440 → #84 → #804 → #593 → #521 — and a
1–1.5 mw behaviour task sits behind a coverage floor, a 5,852-line refactor, and
a catalog. That is not a scheduling complaint. It is evidence that the issue has
identified the wrong obstacle.

## The actual obstacle is that JLS has no selection model

Not "selection logic in the wrong file" — *no model*. At HEAD the editor's
selection is two fields in `EditWindow`:

- `Set<Element> selected` — the members;
- `private @Nullable Rectangle selRect` (`src/jls/edit/SimpleEditor.java:1245`)
  — a rectangle that is simultaneously the painted highlight
  (`:2481-2483`), **the hit region for "did the user grab the selection"**
  (`:2715-2720`, `if (sr.contains(x,y))` → begin group drag), and the implicit
  definition of whether a selection exists at all (`:3479-3482`, a selection
  whose members yield no rectangle drops the machine back to `idle`).

Additive and subtractive selection are not merely unimplemented against that
representation — they are *semantically incoherent* with it. A discontiguous
selection's bounding box necessarily encloses elements the user did not select;
the moment ctrl-click lands, `sr.contains(x,y)` starts a group drag from a
press on an unselected gate. Mixed element/wire sets make it worse: `Wire.getRect`
returns `getIndexBounds()`, the endpoint bbox **grown by a full `Geometry.SPACING`
on every side** (`src/jls/elem/Wire.java:207-218`), so any wire in the selection
inflates the grab region by a grid cell in each direction.

This is why AC-4's "nothing lands inside `SimpleEditor`" cannot be the gate: the
defect is a data-representation choice, and relocating it into a `MouseMachine`
relocates it intact.

## The project already knows how to do this, and it is not GoF State

JLS's own recurring answer to "make editor behaviour assertable without a
display" is **extract the decision as a pure function**, not extract the control
flow as objects:

- `src/jls/edit/DeleteKeyPolicy.java`, `KeyboardConstructionPolicy.java`,
  `OptionMenuPolicy.java`, `src/jls/MenuAcceleratorPolicy.java`,
  `ToolkitPolicy.java` — each a Swing-free decision with the platform/modifier
  input injected, and the whole matrix unit-tested on one runner.
- `SimpleEditor.deleteSelectionPlan` (`:872`) and `moveSelectionPlan` (`:1053`)
  — `static`, Swing-free, `@jls.testedby jls.edit.DeleteGestureTest` /
  `MoveGestureTest`, pinned headlessly today.

Note what those last two prove: they are **inside `SimpleEditor.java`** and they
are fully decoupled and fully headless. "In that file" and "coupled to Swing"
are orthogonal, and the project has already demonstrated it twice. AC-4 gates on
the wrong property.

## The concrete alternative: land `Selection` first, and #84 gets easier

**I am disregarding AC-4 as written and replacing it.** Proposed shape for this
task, none of which requires #84, #440, or #592:

1. **`jls.edit.Selection`** — a value type over stable element ids with
   `add / toggle / subtract / replace`; `hitsMember(x,y)` *replacing*
   `selRect.contains(x,y)` as the group-drag test; and `bounds()` demoted to a
   derived painting view that no decision ever reads.
2. **`SelectionGesturePolicy.resolve(Selection, HitResult, int modifiers)` →
   `Selection`** — the same shape as `DeleteKeyPolicy`, with the modifier mask
   injected so the mac cmd-vs-ctrl matrix is testable on one runner exactly as
   `MenuAcceleratorPolicyTest` does it.
3. **`MarqueePolicy.select(Circuit, Bounds, Mode)`** — the containment rule
   stated once, as an enumerated choice.
4. `SimpleEditor`'s handlers shrink to *hit-test → call policy → assign field →
   repaint*. That is wiring, not logic.
5. **AC-4, purchasable this week**: a rule in `test/jls/ArchitectureRulesTest`
   (sibling of `collabLayersAreHeadless`, `:150`) asserting the `Selection`
   types reference no `java.awt`/`javax.swing` type. #804 gets its
   not-waivable structural gate without waiting for #84 to produce one.

The ordering consequence is the point of this review: **the selection model is a
prerequisite of the state-machine extraction, not a consequence of it.** #84's
own falsification criterion (§10: "if the extracted states cannot be
instantiated without a display substrate, re-cut the context interface rather
than widening it to Swing types") is a bet on exactly these fields —
`selected`, `selRect`, the caret — being cuttable. Today they are an
`awt.Rectangle` doing triple duty. Land the value type first and #84's hardest
step becomes mechanical. The current edge points the wrong way.

## The prerequisite the issue is missing (and it is not #84)

`moveSelectionPlan` returns `null` — inline fallback — the instant a `Wire` or
`WireEnd` is in the selection (`:1063-1067`), and `docs/operation-layer.md`
confirms this is deliberate: only "a pure relocation of plain elements" is
expressible today.

#804 exists to make mixed element/wire selections *routine*. Ship it as written
and every new gesture it enables commits through the one path that is not an op,
not a single undo batch, and not headlessly assertable — the code the whole
decomposition is trying to retire. The genuine upstream is the op layer's
move-with-wires vocabulary (the `RemoveWire` + `AddWire.survivors` composition
already exists for *delete*; the move analogue does not — #167/#337, not #84).
Without it, AC-3's "headless test" can pin what gets *selected* but not what
happens when the user drags it, which is the half users actually notice.

## AC-2 documents an accident

The uniform containment rule AC-2 asks for already exists: `Element.isInside` =
bbox contained (`src/jls/elem/Element.java:558`), `Wire.isInside` = both
endpoints contained (`Wire.java:425`), `WireEnd.isInside` = point contained.
What is missing is not a rule but a *decision*. Evidence that it accreted rather
than being chosen: the guard at `:3464-3467` skipping "degenerate rectangles
(from wires)" is dead code — #35 gave wires a real rect — and its comment now
states something false.

So write the row as a choice, not a transcription, and make it the parity
question it actually is: full-containment versus intersect, and whether JLS
adopts the direction-sensitive marquee (drag left→right encloses, right→left
touches) that switchers arrive with. AC-2's "the same containment rule"
phrasing forecloses the one question a parity catalog exists to ask.

## Where this pulls against the arc, mildly

#592's catalog is right for contested rows — identifier rules, wire coloring,
every REFUSE, anything whose value is arguable. For shift-extend and
ctrl-toggle the gate costs more than the work, and the project already has a
lighter instrument that does the same job: ARCHITECTURE.md's "Recorded
decisions" blocks, each with a rationale and a revisit trigger. Let the
uncontested gestures land behind the ArchUnit rule and headless policy tests,
then write their catalog rows *from shipped behaviour with test names attached*
— rows that cite an executable pin are worth more than rows that cite an
intention.

## Verdict

**endorse-with-reframing.** The outcome is right and underfunded relative to its
value. Reframe the deliverable from "selection behaviour lands in the decomposed
collaborators" to "the editor acquires a selection model": a Swing-free
`Selection` value type plus two policy functions, gated by a new
`ArchitectureRulesTest` rule rather than by #84's completion, and ordered
*before* #84 rather than after it. Add the real blocker the issue omits — the
op-layer move vocabulary for selections containing wires — and drop AC-4's
file-location test in favour of a property (no AWT/Swing type in the signature)
that the project can already enforce and has already enforced three times.
