# Issue #593: FEAT-C37-2: a switcher selects, drags, aligns and duplicates a group of elements the way every other editor taught them to — compound selection stops being the first thing that bounces them
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is right and it is the right end: a person who already knows
Logisim-Evolution or Digital should not lose their first ten minutes to
"I can't select two things." bsiever-fork #18 is real course-era debt,
and CAP-37's insight — that the incumbents' trackers are a free UX
research corpus — is one of the better strategic reads in this repo.
Nothing below argues against the outcome.

What I want to re-cut is the *route*. As filed, #593 is a single
feature parcelled by user-visible outcome ("manipulating what is
already on the canvas"), gated behind a documentation catalog (#592)
and a god-class refactor chain (#440 → #84 → #804/#805/#806). The
work it actually contains lives at three different architectural
depths, only one of which needs that chain. Cutting along the outcome
instead of along the layer is what forced the 700-word fence comment
against #594/#595/#596 — a prose boundary is what you write when the
type boundary is missing.

## Grounding: what the code actually does today

- Hit resolution is **undefined by construction**, not "nearest
  instead of under the cursor." `SimpleEditor.mousePressed` iterates
  `for (Element el : circuit.elementsAt(x,y))` and takes the first
  `contains(x,y)` hit (`src/jls/edit/SimpleEditor.java:2617`, again at
  `:2672` and `:3347`). `Circuit.elementsAt` (`src/jls/Circuit.java:588`)
  delegates to `SpatialIndex.query`, which returns a `HashSet`
  (`src/jls/SpatialIndex.java:189`) — and populates it by two different
  traversals depending on query size (`:200` vs `:212`). When a wire
  segment, a wire end and an element's bounding box all cover the
  cursor, which one you get is hash order. AC-4 reads as a UX polish
  row; it is really a missing total order in the core hit-test layer.
- Additive/subtractive selection is not merely weak, it is absent:
  the only `isShiftDown` in the 5,852-line file is a scrollbar choice
  (`:3924`). There is no modifier plumbing to extend.
- Selection is a `Set<Element> selected` plus a bounding `selRect`
  (`:1245`, `:2661`, `:3592`), and the two operations that already
  moved off the state machine — `deleteSelectionPlan` (`:872`) and
  `moveSelectionPlan` (`:1053`) — are **static, Swing-free planners**
  returning `List<CircuitOp>`, pinned headlessly by
  `test/jls/edit/DeleteGestureTest.java` and `MoveGestureTest.java`
  against canonical save bytes (#166).

That last point is the whole reframing. The layer this feature needs
already exists, already runs headless, and already has an acceptance
oracle — and it is *not* #84's state machine.

## Reframing 1: cut along the layer, and most of this stops being blocked

Three layers, three schedules:

1. **Hit resolution** (`jls.SpatialIndex` / a new `HitResolver`): a
   documented total order over overlapping candidates — wire segment
   beats element body beats bounding box, ties broken by z/insertion
   order, never by hash. Pure core, no AWT, headless-testable today,
   fixes clicking on *everything*, not just wires. Zero dependence on
   #84 or #316. This is AC-4, and it is a correctness fix that should
   not be waiting behind a UX catalog at all.
2. **Selection semantics** (planners beside `moveSelectionPlan`): a
   `Selection` value type plus `duplicateSelectionPlan`,
   `alignSelectionPlan`, and additive/subtractive set algebra —
   `List<CircuitOp>` in, canonical bytes out, tested exactly the way
   move and delete already are. This is the substance of AC-3. It
   touches `SimpleEditor` only as a call site; if you extract the two
   existing planners into a `SelectionOps` file on the way, KC-37-1 is
   honoured in letter *and* spirit rather than being a reason to wait.
3. **Gesture bindings** (modifier keys, rubber-band mode, drag
   thresholds): genuinely per-state input plumbing. This — and only
   this — belongs behind #84.

Layers 1 and 2 are, by inspection, the majority of the user-visible
value and can be funded now. As filed, all three sit behind
`#440 → #84 → #804/#805/#806`, a chain whose head is a task in a file
that grew from 4,119 to 5,852 lines *while* five extractions succeeded
(#84 §1). Betting the switcher-retention outcome on that chain
completing is the largest risk in this issue, and it is avoidable.

## Reframing 2: the refusal reason wants to be a type, not a prose boundary

AC-3 says group operations "preserve connectivity or refuse with a
named reason," and the dedup comment then spends a paragraph splitting
the refusal *behaviour* (here) from the refusal *message quality*
(#595). That split is already solved elsewhere in this codebase:
`LoadError` is a fixed category taxonomy plus location, detail and an
actionable hint, published through one channel, with every front end
rendering the same structure (ARCHITECTURE.md, "Error-reporting
contracts"). Do the same thing: planners return an `EditRefusal`
(category, the two disagreeing parties with their locations,
the reconciling edit), #593 owns the category set, #595 owns the
rendering and the message-quality corpus. The prose fence becomes a
compile-time boundary, AC-2's "fails at the pre-change commit" test
becomes a headless assertion on a category enum, and #595 inherits a
structured input instead of a pile of strings. This is a pattern the
project has already ratified once and would be strictly better for
having twice.

## Reframing 3: I am disregarding align/distribute as specified

AC-3 names "group drag, align, distribute and duplicate." Group drag
and duplicate are sound: a translation of a closed selection by a grid
multiple is **connectivity-invariant by construction** — intra-group
geometry is unchanged, and only the selection boundary can gain or
lose connections, which is precisely what `moveSelectionPlan` already
computes (`:1079-1095`).

Align and distribute are a different animal, and I do not think they
belong here as written. They change *intra-group* relative geometry.
In JLS connectivity is positional: wires are polylines between ends,
and moving an element without its attached wire ends either stretches
the net into nonsense or detaches it. So an align that "preserves
connectivity or refuses" will spend most of its life refusing on any
circuit that is actually wired — the exact circuits users want tidy.
The honest options are (a) align-and-reroute, which is a wire-routing
feature roughly the size of this whole issue, or (b) don't ship it.

Note also that align/distribute appears in none of the cited
complaints — bsiever #18 is compound selection, Digital #882 is
wire interaction, Logisim #1234 is component search. It entered
through the drawing-tool idiom (Illustrator, Visio), not through the
evidence. That collides with AC-1's own rule that nothing lands that
#592 did not score. The replacement that serves the real want —
"my schematic looks tidy" — is grid-quantized **group nudge** (the
`keyboardNudge` path already exists) and a grid-quantized duplicate
offset, both connectivity-invariant, both cheap, both catalog-defensible.

## Reframing 4: the acceptance vehicle CAP-37 should build once

AC-2 requires each behaviour pinned by a test failing at the
pre-change commit; AC-5 re-times a 4-bit counter by hand. A hand-timed
task measured once is not a ratchet — it detects nothing on the next
commit. This project's actual idiom is golden files
(`BatchSimulationGoldenTest`, `VcdExportGoldenTest`, canonical save
#166). Propose an **interaction-script golden**: a text fixture of
input events replayed against #84's machine (or, for layers 1-2,
straight against the planners), asserting canonical save bytes. Then
every catalog row in #592 becomes a data file rather than a Java
class, and #594, #595 and #596 all inherit the same vehicle. Filed
once at CAP-37 level, it is worth more than four features' worth of
bespoke assertions — and it is the thing that makes "the after is not
slower, and still correct" a standing property rather than an anecdote.

## Alignment with the larger arc

Positive: this pulls in the same direction as the op-layer migration
(#167 / `docs/operation-layer.md`) and the collaboration program
(#163) — every selection operation expressed as validated, invertible
`CircuitOp`s is exactly what replication and per-peer undo need later.
A `Selection` + planner layer is a gift to #163, not a detour from it.

Negative, if left as filed: the issue's centre of gravity is the
interaction machine, which is the one part of the work with no
downstream leverage. Cut it as filed and CAP-37's value arrives last
and all at once, behind the riskiest dependency in the chain. Cut it
by layer and the correctness fix (hit order) lands first, the reusable
semantics land second, and only the keyboard-modifier plumbing waits
on #84 — which is also the part users forgive being late.

## Recommendation

Endorse the outcome. Re-cut the feature into: (a) a hit-resolution
total order in core, unblocked from #84 and arguably a bug fix; (b) a
`Selection` + planner layer with an `EditRefusal` taxonomy, sibling to
the existing move/delete planners; (c) gesture bindings behind #84.
Drop align/distribute in favour of connectivity-invariant group nudge
and grid-quantized duplicate unless #592 scores align on cited
evidence and someone funds routing. Lift the interaction-script golden
to CAP-37 so all four features share one acceptance vehicle.
