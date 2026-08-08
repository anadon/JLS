# Issue #529: FEAT-C23-2: clicking a waveform edge flashes the gate that emitted it on the canvas, and clicking a canvas wire lands on its chronogram lane — identity by click, not by matching names by eye
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the CAP-23 framing away and the claim is not about chronograms at all. It is:
**JLS's views each hold the identity of the thing they display, and then refuse to say it out
loud, so the human re-derives it by string matching.** That is a general defect with a general
fix, and #529 is filed as one instance of it (waveform ↔ canvas) inside one capstone.

The instance is right. The framing — a point-to-point link between two panels, gated behind
FEAT-C23-1's panel, resolving "through the same seam the chronogram tap uses" — is narrower
than the project's own trajectory already justifies, and in one respect actively wrong.

## The plumbing this issue proposes to build already exists

This is the load-bearing finding, and it reframes the cost and the ordering.

- `src/jls/edit/Trace.java:113` — every trace lane is constructed **with the `Element` it
  displays**, and `getElement()` (`:149`) hands it back. Lane → element identity: present, today.
- `src/jls/edit/InteractiveSimulator.java:94,96` — `Map<Element,Trace> traceMap` and
  `Map<Wire,Trace> wireMap`, populated in `findTraces` (`:981`, `:1005`). Element → lane
  identity: present, today, **both** for watched elements and for probed wires.
- `src/jls/edit/Trace.java:606` — `public void mouseClicked(MouseEvent event) {}`. An empty
  method on a panel that already registers itself as a `MouseListener` (`:124`).
- `src/jls/elem/Element.java:24` — every element, wires included (`Wire extends Element`),
  carries a persisted `ElementId` (#165). `Element.isHighlighted()` and
  `ElementRenderSupport.drawHighlight` already exist and are already drawn by every renderer.

So the two maps required for bidirectional cross-probing, the identity type, the highlight
flag, and the click hook are all in the tree. What is genuinely missing is three things, none
of them identity plumbing: a focus/flash state with a defined lifetime, scroll-into-view on the
canvas viewport, and on-demand lane creation for a wire that has no probe yet.

## Reframing 1 (primary): build the focus bus, not the link

CAP-23 needs "click here, land there" **three times**, and as filed it will build it three times:

- PF-2 (this issue): chronogram lane ↔ canvas element.
- PF-3 / #532: cause-chain row → canvas element. The dedup comment says #532 *consumes* #529's
  selection — which only works if the selection is a thing, published somewhere, not a private
  call from `Trace.mouseClicked` into `SimpleEditor`.
- PF-6 / CAP-23 AC-4 `ViewerSyncTest`: "add-signal from a canvas click and JLS time-follow from
  a viewer cursor move." That is cross-probing again, with an out-of-process peer on one end.

And the project has three more consumers standing by: `jls.collab` (whose ops already address
elements by `ElementId` — `collab/op/AttachProbe.java:19`) wants remote-peer focus presence;
`LoadError` (#58) wants "show me the element that failed"; the JumpStart/JumpEnd renderer
already implements a hand-rolled cross-highlight **by matching names**
(`src/jls/edit/JumpStartRenderer.java:45-56`) — literally the anti-pattern this issue's title
condemns, shipping in the canvas today.

Concrete alternative: make the deliverable a **`FocusBus`** — a tiny AWT-free service in
`jls.edit` (or `jls.core`) over a closed identity type, `ElementId | NetKey | (ElementId,
PortName)`, with `focus(target, Origin)` and a subscriber list. Publish it as an
`ExtensionPoint` alongside `GuiExtensionPoints.PALETTE_CONTRIBUTOR` and catalogue it in
`docs/extension-points.md` — the seam discipline recorded in ARCHITECTURE.md ("Extension
points: the typed seam catalog", #223) is exactly built for this, and no seam yet exists for
"a view says: this thing, now." Then:

- chronogram lane click → `focus(lane.element.stableId)`; canvas subscriber highlights and scrolls.
- canvas wire click → `focus(netKey)`; chronogram subscriber selects or creates the lane.
- #532's cause-chain rows become a third publisher and cost nothing extra.
- PF-6's viewer sync becomes an **adapter on the bus**, not a second cross-probe mechanism —
  which is the difference between AC-4 being a week and being a month.
- the JumpStart name-match becomes a bus subscriber and the duplicate dies.

This is the same shape the project already chose twice: a closed data-only op vocabulary in
collab, a typed extension-point registry for modules. A focus bus is that pattern applied to
*attention* instead of *mutation*, and it is what makes "identity by click" a property of JLS
rather than a property of one panel pair.

## Reframing 2: cut the `ordering_after: [FEAT-C23-1]` edge — deliver on the shipped trace window first

I am explicitly disregarding this issue's ordering constraint and its AC-3.

`ordering_after: [FEAT-C23-1]` puts a 2–3 mw feature behind a 4–6 mw panel, and AC-3 binds the
proof to `HazardDiagnosisWalkthroughTest`, which additionally needs PF-5's analyzer element that
nobody has filed. That is roughly 10 mw of prerequisite standing between the project and an
outcome whose two required maps already exist in `InteractiveSimulator`.

Against the *shipped* `Traces` panel, `Trace.mouseClicked` → `element.setHighlighted(true)` +
viewport scroll, and canvas wire click → `wireMap.get(wire)`/`traceMap.get(el)` + scroll the
trace pane, is a small change that delivers the stated outcome — "the student stops matching
signal names by eye" — to every user of JLS today, on a panel that is not default-hidden and not
waiting on a capstone. It also does something the current plan cannot: it **falsifies or
confirms the capstone's central UX premise for near-zero cost**, before 18–26 mw is committed.
If clicking an edge and watching a gate light up does not visibly change how students debug, that
is worth knowing at 1 mw, not at 12.

The chronogram then inherits a working, already-exercised focus bus instead of specifying one.

## Reframing 3: cross-probing must not touch the event loop at all

AC-4 says cross-probing "resolves identity through the same seam the chronogram tap uses; no
additional per-event cost accrues when the chronogram is closed."

The second clause is right and the first clause is what endangers it. Cross-probing is a **pull
at click time**, not a push per event. It needs `traceMap`/`wireMap`/`Trace.getElement()` — data
structures that already exist and are consulted once per human gesture. Routing it through
FEAT-C23-1's per-event tap couples a zero-frequency operation to the hottest loop in the program
(the kernel already spends 47.7% of warm-loop time on event bookkeeping) and converts a property
that could be **true by construction** — PF-2 imports nothing from `jls.sim`'s hot path — into
one that has to be *measured* under AC-5's tolerance. Rewrite AC-4 as: *no class introduced by
this feature is referenced from `Simulator.runEventLoop`, `post`, or any `react`; the
`HeadlessCoreRatchetTest` style of structural assertion, not a benchmark.*

There is one honest exception, and it is where I would draw the #529/#532 line more sharply than
the dedup comment does. For a **watched-element** lane, the emitting element is the lane's own
element — free. For a **probed-wire** lane on a single-driver net, `WireNet` knows the driver —
still free. Only for a multi-driver / tri-state net does "which driver caused *this* edge" require
the producer relation from the event stream (`Gate.java:708` posts `new SimEvent(now+propDelay,
this, ...)` — the callback *is* the producer, so `Simulator.afterEvent` already sees it). Ship
#529 without the tap, covering the single-driver case that is essentially all of a teaching
circuit, and let the multi-driver case be the first customer of #532's retained event graph. That
is a cleaner seam than "the same seam the chronogram tap uses," and it removes #529 from AC-5's
blast radius entirely.

## Two specifics the issue underspecifies

**Net identity.** AC-2 says "focuses that signal's chronogram lane, creating the lane if the
signal is not yet displayed," and the boundary note waves at #373 as "consumed if present, not
required; cross-probing keys on element/event identity, not display names." A `WireNet` is a
computed partition, not an element, so there is no stable id to key on — unless you adopt the
convention `jls.collab` already invented: `NetBlocks` canonicalizes a net by **stable-id order of
its members** (`src/jls/collab/op/NetBlocks.java:31-38`), and `AttachProbe` addresses a probe by a
wire's `ElementId`. Define `NetKey = min(ElementId over the net's ends)` and reuse the tested
convention rather than minting a third one. Also note honestly: *creating* a lane requires
**naming** it in the UI, so #373's stability does bear on this after all — not on the key, on the
label. Say so instead of disclaiming it.

**"Flash" is a testability smell.** A transient animation is the hardest possible thing to assert
in AC-3's scripted test, and `test/jls/ui`'s Layer 2 (real display via `xvfb-run`) is the
expensive layer. Model it as state: `FocusBus` holds `(target, requestedAtNanos, token)`, the
renderer decides how that state looks, and the test asserts the *model* at Layer 1 headlessly —
consistent with the "assert the model, never brittle pixel goldens" discipline in
`test/jls/ui/package-info.java`. Then AC-1 and AC-2 are provable without a display and without
the walkthrough test existing yet.

## Where this lands

The issue's outcome is correct and worth funding; its architecture, ordering, and proof strategy
are each one level too specific to CAP-23. Endorse the outcome; reframe as: a typed focus/identity
seam in the extension-point catalog, keyed on `ElementId` and a `NetKey` borrowed from `NetBlocks`,
delivered first against the shipped trace window ahead of FEAT-C23-1, structurally forbidden from
touching the event loop, and asserted at Layer 1 as model state. Every later consumer — #532's
cause chain, PF-6's viewer sync, collab presence, the JumpStart duplicate — then gets it for free
instead of building a fourth copy.
