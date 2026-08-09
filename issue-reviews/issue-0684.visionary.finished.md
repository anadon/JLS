# Issue #684: TASK-C529-1: clicking a waveform edge flashes the element that emitted it, scrolling the canvas to it if it is off-screen
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

CAP-23 (#504) §1 step 2 is one gesture in a student's head: *click the bad edge, find
out why it happened*. The capstone splits that gesture across three features — PF-2's
flash (#529 → this task), PF-3's cause chain (#532 → #688/#689). But "which element
emitted this edge" and "which event caused this event" are the same query at depth 0 and
depth N. #684 is not a cross-probing feature that happens to sit near a cause-chain
feature; it is **hop zero of the cause chain**, and the project's arc is stronger if it
is built as such.

That framing matters because of ordering. #684 is `ordering_after: [TASK-C527-2]` and
#689 is `ordering_after: [TASK-C532-1, TASK-C529-1]`. As written, #684 invents an
edge→emitter resolution now, and #688's retention ring buffer arrives afterwards with a
producer/consumer/element/delay record that answers the same question more completely.
#685 AC-4 already names the hazard ("a single resolution path, not two that can
disagree") but scopes it only to the two cross-probe *directions*. The real duplication
risk is across features: two answers to "who drove this edge", one from a lane-local
lookup and one from the retained relation, differing exactly on tri-state buses where the
answer is interesting. CAP-23 §3 risk 1 says "never two cause-chain models"; this is how
you get one anyway, by the side door.

## Reframing 1 (primary): resolve structurally, and only capture what structure cannot say

I am explicitly disregarding **AC-2 as written** — "identity resolution keys on element
and event identity through TASK-C527-1's seam" — as a universal requirement.

A chronogram lane in JLS is bound to one of two things, and both already carry the
emitter statically:

- a **watched element**: `Trace` already holds `private Element element` — "the element
  whose output this trace records" (`src/jls/edit/Trace.java:56-57`, constructed at
  `src/jls/edit/InteractiveSimulator.java:1000`). The emitter of every edge in that lane
  is that element. There is nothing to resolve.
- a **probed wire net**: the emitter is the net's driving `Output`
  (`src/jls/edit/InteractiveSimulator.java:979-981`). For a single-driver net — every
  net in a first-year adder — that is a structural fact of the wiring graph, answerable
  at click time with a walk over `WireNet.getAllEnds()`.

The only configuration where per-edge capture adds information is a **tri-state net with
more than one driving Output**, where `WireNet.propagate` picks "the first active driver
in net order" and warns once on conflict (`src/jls/elem/WireNet.java:443-482`). That set
of nets is known when the net is built and is typically empty.

So: **depth-0 resolution structural by default; per-edge driver capture only on
multi-driver nets.** The consequences are not marginal, they are architectural:

1. **AC-4 becomes a tautology instead of a measurement.** The dominant path adds *zero*
   kernel code, so `ChronogramClosedCostTest`'s tolerance cannot move. When the hardest
   acceptance criterion in an issue becomes free under a reframing, that is the signal
   the reframing is right.
2. It respects the recorded ADR that the hot plane stays inside core with no
   indirection (ARCHITECTURE.md, "Simulation execution strategy", #221). Uniform
   per-event emitter capture pushes debug metadata into the retire path for the 99% case
   that never needed it.
3. It gets the *better* answer on tri-state buses, because it captures the driver that
   was actually active rather than re-deriving net order after the fact — and bus
   contention is precisely the pedagogically rich case CAP-23's audience hits.
4. It does not block on #678. #684 can land beside the tap rather than behind it.

## Reframing 2: shape the API as a provenance node, not an element reference

Whatever the depth-0 implementation, the *signature* #684 exposes should be the one
#689 will need:

```
record Provenance(ElementId emitter, long firedAt, long delayConsumed,
                  Optional<Provenance> producer)
Provenance resolve(Lane lane, long tick)   // producer empty until #688 lands
```

#684 renders hop 0 and leaves `producer` empty; #688 fills in the parent link; #689
renders hops 1..N. One resolution path, one data model, no consumer edit when retention
arrives. Adding this constraint costs #684 nothing today and is unrecoverable later —
once a `flashEmitterOf(edge)` method exists and the walkthrough test asserts on it,
#689 will grow a second path rather than deepen the first.

## Reframing 3: identity is `ElementId`, and reveal is an editor primitive

The issue says "element/event identity, not display names" and stops there. The repo
already answers what identity means, and the answer is not an object reference:

- `Element.stableId` (`src/jls/elem/Element.java:24`, #165) is persisted, minted
  deterministically, and — decisively — **survives undo**:
  `StableElementIdTest#undoRestorePreservesIds`, because `CircuitSnapshot` restores
  through the load path (ARCHITECTURE.md: "undo semantics are exactly save/load
  semantics"). Every `Element` *object* is replaced by that restore. A chronogram holding
  element references therefore breaks on the first Ctrl-Z of a debugging session —
  silently, and not by deletion, so **AC-3 as written does not cover it**. AC-3 should be
  "no longer resolvable in the current circuit", with undo/redo as a named test case
  alongside deletion.
- There is no `ElementId → Element` index on `Circuit` today; only sorted enumeration
  (`Circuit.getElementsInStableOrder`, `src/jls/Circuit.java:479-485`). Building that
  index here is a ten-line durable asset that #688/#689, the collab op layer
  (`src/jls/collab`), and the #78 element registry all want, versus a chronogram-local
  map that helps nobody else.
- **Open design question this issue must settle:** whether `ElementId` uniqueness spans
  nested subcircuit circuits. `SubCircuit` holds its own `Circuit`
  (`src/jls/elem/SubCircuit.java:26`) and `copy()` rebuilds it
  (`:332-344`, with `StableElementIdTest#copyMintsAFreshId`). If ids are unique only
  *within a* `Circuit`, the resolution key is `(circuit path, ElementId)` and every
  consumer downstream inherits that shape. Getting this wrong means an edge inside one
  instance of a subcircuit flashes a gate in a sibling instance.

Likewise, "flash and scroll into view" is not a chronogram behavior; it is
`SimpleEditor.reveal(target, reason)`. Two facts make this non-obvious and worth pinning
in this issue:

- The flash **must not** reuse `Element.setHighlight` (`src/jls/elem/Element.java:41,571`).
  That is the hover/selection highlight, and `SimpleEditor` clears it on the next mouse
  motion (`src/jls/edit/SimpleEditor.java:3779`, "unhighlight whatever the cursor left").
  A flash built on it dies the instant the student moves the mouse toward the gate they
  were just shown. It needs to be a separate transient decoration with its own bounded
  timer on the EDT.
- "Scrolls the viewport to it" is ambiguous between two live mechanisms: the
  `JScrollPane` (`SimpleEditor.java:130,433`) and the `Viewport` pan/zoom transform
  (`src/jls/edit/Viewport.java`, `pan`/`zoomTo`/`fit`). Reveal belongs at the `Viewport`
  layer — "bring this model rectangle into view, panning only, zooming out only if it
  cannot fit" — which is testable headlessly exactly like `ViewportTest`, whereas a
  scrollpane-position answer is not.

That primitive is then immediately reused by #685's inverse direction, #689's per-hop
selection, `LoadError`'s "which element" reporting, and collaborative presence.

## One risk the issue does not name

The CAP-23 demo slice ships this flash *without* the cause chain, and on a reconvergent
glitch the emitting gate is the **last** gate — the least informative element in the
chain. A satisfying flash that points at the wrong end of the story is a real pedagogical
hazard for a tool whose thesis is "the tool answers why". Cheap mitigation, which also
pre-shapes the UI for #689: pair the flash with one line of status text naming the
element and the delay it consumed, and a "why did this change?" affordance that is
visibly present and disabled until #688/#689 land. The demo slice then reads as one hop
of a chain, not as the whole answer.

## Tests worth more than AC-2's rename test

Under structural resolution the rename test asserts against a risk that no longer
exists — names were never on the path. Higher-value pins: (a) undo/redo, then click,
still resolves; (b) a two-driver tri-state bus resolves to the driver that was actually
active at that tick, not to net order's first; (c) an edge on a net inside a subcircuit
instance flashes inside *that* instance.

## Verdict

**endorse-with-reframing.** The outcome is right and load-bearing for CAP-23. Keep AC-1
and AC-4. Rewrite AC-2 to require the provenance-node signature and structural depth-0
resolution with per-edge capture only on multi-driver nets; widen AC-3 from "deleted" to
"unresolvable, including after undo", keyed on `ElementId`; and add the `reveal`
primitive and the `ElementId → Element` index as named deliverables rather than
incidental code. The band (1–1.5 mw) survives this — most of it is a smaller
implementation than the one written down.
