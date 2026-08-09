# Issue #383: TASK-0036: a second view's geometry lives in its own preserved section, and a drag in one view is not a drag in the other
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Stripped of its apparatus, the claim is: *JLS should stop treating "where a thing
is" as a property of the thing.* That is correct and overdue. `Element` at
`src/jls/elem/Element.java:27-34` carries `x`, `y`, `width`, `height` as saved
element state, and every consumer — 31 renderer classes in `src/jls/edit/`, the
110 `.getX()`-family reads in that package alone, `SpatialIndex`, the op layer —
reaches through the element to get them. A breadboard canvas (#329), an analog
canvas, and the layout view sketched in `docs/capability-roadmap/sweep-06-physical-boundary.md`
§E all break on that. The direction is endorsed without reservation.

What I am rejecting is both mechanisms this task picks to get there, and the
order it does them in. I am explicitly disregarding acceptance criteria P1, P3,
Open Question 2 and Open Question 4, because each exists only to service a design
decision that should not be made.

## 1. The view already has a home in the address; the op field is a second vocabulary

§12 says, correctly, "There must not be two view vocabularies." This task then
creates the second one. TASK-0035's `ItemKey` is `view ":" instancePath ":" sid`
— the view is *in the address*. This task nonetheless leaves ops addressing
elements by bare `ElementId` (`MoveElements.java:27`, `Ops.resolve` at
`src/jls/collab/op/Ops.java:33-43`) and bolts a parallel ` String view` line onto
four of eleven records.

The consequences are all self-inflicted:

- P3 exists only because a field was added where it does not belong. Every one of
  the eleven arms of `CircuitOpReader`'s switch (`:119-178`) must be edited — four
  to accept, seven to forbid — and §11 correctly identifies that the compiler
  helps with none of it. That is a smell, not a hazard to be tested around.
- H4/P8 ("the inverse must carry the same view") is a property that has to be
  asserted only because the view is a payload component that `invert` could get
  wrong. If the view rides in the ids, `MoveElements(ids, -dx, -dy)` is
  view-preserving by construction, not by test.
- Open Question 4 (does `AddElements` take a view, or a `(view, row)` pair?)
  dissolves: an add addresses placements, so the placement key *is* the answer.

**Alternative: geometric ops address placements, not elements.** Give the four
geometric kinds `List<ItemKey>` where they today take `List<ElementId>`; leave the
other seven addressing elements. The wire form does not gain a line at all — the
existing ` String id "r0:1"` becomes ` String id "breadboard:top:r0:1"` when it
means a non-default placement, and a two-field value keeps parsing as today's
schematic-relative id (`ElementId`'s replica charset is `[0-9a-z]{1,64}`, so
field-count discrimination is unambiguous). H2/P2 hold *more* strongly than the
issue's design gives them: no new byte is emitted for any op writable today.
P1 becomes "a placement-addressed move parses"; P3 becomes vacuous; the
"forbid on seven kinds" work vanishes; and the type system, not a reader switch,
is what says a watch toggle has no view. Validation reduces to one uniform rule
("all keys in one op share a view"), stated once.

This also fixes a gap the issue does not mention: ops cannot address anything
inside a subcircuit today (`Ops.resolve` scans one flat `circuit.getElements()`).
`ItemKey` carries `instancePath` for exactly that reason. Ops need the full key
regardless of views; adding a narrower, redundant `view` component first is
building the wrong half of the same change.

## 2. `(x, y, w, h, orientation)` is the schematic's tuple, not a view-neutral one

§7.10's `G : V × A ⇀ (x, y, w, h, o)` looks general and is not. Two of its five
components are wrong on their face: `width`/`height` are *derived* — the
`Attribute` rows at `Element.java:231-252` document them as omitted from the save
and recomputed on load. Persisting per-view copies of a derived quantity opens a
divergence channel in exactly the place H3 exists to close.

The bigger problem is the consumer. #329 §3 defines the breadboard's data as
`hole : P ⇀ H` over strips and jumper edges `J` — a discrete combinatorial
placement over a board topology, not a pixel rectangle. Sweep-06 §E says of the
layout view, in its own words, that it needs "a *separate* coordinate space for a
*separate* view, not a change to the schematic model — which is what makes it
tractable." So the universal tuple this task standardizes is a tuple that its own
named blocker (#329) will not use and that the next view after it explicitly
must not use. It is a generalization from one instance, and the instance is the
one it is generalizing away from.

**Alternative: the section frame carries view-owned, view-typed placement rows.**
The shared, cross-view contract is exactly three things: the key (`ItemKey`), the
canonical ordering, and skip-and-preserve. The *row schema* belongs to the view
that owns it — schematic: nothing (element blocks are authoritative);
breadboard: `(strip, hole, package, orientation)`; layout: nm coordinates. The
core reads a section it does not know as opaque ordered bytes, which is already
FEAT-013's semantics, so P7 is served by the frame rather than by this task. What
this task then owns is small and permanent: `ItemKey`-keyed sections, their
ordering, and the rule that a view's rows are a pure function of that view's
content.

## 3. The default view should be special at the *writer*, not in the API

"`get` on schematic delegates to the element; `put` on schematic throws" (§7.4)
makes the asymmetry structural and permanent. It leaks immediately: Open Question
5 has to invent a matching orientation asymmetry, and §8 has to ask a future
author to please not fix it. A service whose mutator throws for the only view
that exists today is a service every caller must branch on.

**Alternative: one uniform placement table for every view, including schematic,
with a write-time elision rule.** Load materializes the schematic's rows from the
element blocks; save writes them back into the element blocks and emits no `VIEW`
section for the view whose geometry the element blocks carry. Identical bytes,
identical H1/P9, no throwing mutator, no asymmetric orientation rule — and the
schematic stops being a permanent exception in the model. #329's Open Question 2
("which view drives the simulation") becomes expressible instead of awkward.

H3 is right that a second `(x, y)` on `Element` must not exist, but the issue
under-costs what makes it true. `ElementRenderer.draw(Graphics, Element)`
(`src/jls/edit/ElementRenderer.java:24`) has renderers pull coordinates out of
the element; 31 renderers and the hit-test path do the same. The seam that makes
H3 hold is *placement-parameterized rendering and hit-testing* — `draw(Graphics,
Element, Placement)` or a render context — not "a side table with a cache"
(§10). That is a real, nameable piece of work this task's §8 does not list, and
it is the piece that actually delivers "geometry is not a property of the thing."
(Note also that `jls.util.Placement` is already taken, per
`docs/pointer-geometry-census.md`.)

## 4. A view is a module, not a string

ARCHITECTURE.md's recorded decision of 2026-07-27 (#223) and
`docs/extension-points.md` settle how JLS admits new capabilities: typed
`ExtensionPoint` constants, contributions through `ExtensionRegistry`, catalogue
cross-checked in both directions. Open Question 1 — "what is in the closed view
vocabulary at v1, with registration open" — is a request for a registry that the
architecture has already decided how to build, and a `String view` component is
the untyped version of it.

A view is precisely a module contributing to several points at once: a canvas, a
renderer set, a palette slice (which TASK-0105 is separately inventing a "view
dimension" for), a placement row schema, and possibly op kinds. Adding a
`gui.view-provider` row to the catalogue makes TASK-0036 and TASK-0105 one
mechanism instead of two parallel view dimensions, and makes "which views exist"
a resolved module graph rather than a string the reader must be taught to trust.
`docs/extension-points.md` has no view seam today; that absence is the actual
finding.

## 5. Sequencing: the mechanism precedes its only consumer by four features

#329 is blocked by #316, #318, #341 and #365; this task is blocked by #319 and
#337 and depends on two unfiled siblings. So a view abstraction would be frozen
into the file format and the network grammar — both stability surfaces — with
zero live second views to falsify it. §11's own "Criterion P6 can pass with one
view" is the tell.

The project's grand architecture puts persistence and ops inside the `core`
kernel (#77) and names the critical path as #77/#78/#167. `CircuitOp.apply` still
taking a `Graphics` (`CircuitOp.java:51`, this task's #337 blocker, and the reason
§7.9 concedes the inverse tests are display-bound) is a symptom of that
extraction being unfinished. Widening the op grammar *before* the core boundary
lands means the widened grammar moves twice.

**Alternative sequencing.** (a) Land `ItemKey` and the section frame. (b) Let
#329's canvas own a private, breadboard-typed section keyed by `ItemKey` — the
frame already preserves it for readers that do not know it, so nothing is lost by
not generalizing. (c) Move geometric ops to placement addressing as part of the
op layer's own hierarchy work (#167), where it is a correctness fix, not a
multi-view feature. (d) Promote "view" to a catalogued extension point when the
*second* non-schematic view is real — analog or layout — with two instances to
generalize from instead of zero.

## What I would keep verbatim

H1 and P9 (the whole golden corpus stays byte-identical) are the right invariant
and should survive any redesign. So should the §7.11 row that says a `VIEW` row
whose key names a missing element is preserved on write, not dropped —
silently discarding another editor's data is the failure mode that justifies the
section frame in the first place. And the refusal in §10 to regenerate a moved
golden is exactly the right instinct.

## Bottom line

The end is right; both means are cut across the grain. Carry the view in the
address that already contains it, let each view own its own row schema, make the
schematic special only at the writer, register views the way this project has
already decided to register capabilities, and build the generalization behind the
consumer rather than four features ahead of it. Done that way, most of §5 and
§8 disappears rather than being satisfied — which is the sign the seam moved to
the right place.
