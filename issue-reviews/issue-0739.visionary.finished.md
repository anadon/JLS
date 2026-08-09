# Issue #739: TASK-C544-2: the circuit is navigable as an element graph — every element and connection reachable by keyboard with a spoken name, role and connection context
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

One sentence survives the yaml: *a student who cannot see the canvas must be able to
answer "what is this, and what is it wired to?" and then move along that wire.* That
end is unambiguously right and is where the project's arc already points — CAP-26
(#507) funds it, #355 names the gap as the difference between passing and failing a
course accessibility review, and #75 already shipped the operability half.

I endorse the end. I do not endorse the seam. As written, #739 puts a graph-walking
cursor and a description vocabulary inside the GUI, on top of an `AccessibleContext`
that does not exist yet (#380), over a 5,852-line custom-painted panel
(`/home/user/JLS/src/jls/edit/SimpleEditor.java`), and bets the whole deliverable on
AT-SPI reading a hand-rolled accessible tree for a custom component — the one thing
#380 §11 concedes it never tests. Every one of those choices has a cheaper, more
durable alternative that this repo has already built somewhere else.

## 1. The graph already exists — in `jls.hdl`, and it is about to be built a third time

`HdlExporter.buildModel` (`/home/user/JLS/src/jls/hdl/HdlExporter.java:170`) already
computes exactly the relation #739 wants to walk: it unions `WireNet`s under a
union-find (`:1102`), aliases `JumpStart`/`JumpEnd` pairs into single nets (`:216-303`),
resolves ports and bit widths, and attaches a human comment per element ("AndGate at
(240,120)"). `jls.hdl.layout.LayoutGraph` builds a second, coarser graph for imported
netlists. #546 (FEAT-C26-4, same capstone) will build a third when it emits its
part-to-whole prose narrative — which needs precisely #739's AC-2 sentence
("output of AND gate 3, driving input B of the adder"), in the same words, for the same
student, under a different feature id.

So the tracker currently has three sibling efforts each walking the circuit as a graph
and each inventing its own way to say a connection in English, and #739 proposes a
fourth walker inside Swing. `docs/grand-architecture.md` §3 calls the headless kernel
"the highest-leverage single change in the tracker"; §5 puts `hdl` and `gui` on the same
tier, both `requires: core`. A description-of-the-circuit model belongs *below* both.

**This matters for correctness, not just tidiness.** AC-1 says "every element and every
connection is reachable by keyboard-driven graph traversal ... in a connected circuit."
Connectivity in JLS is not the wire graph:

- `JumpStart`/`JumpEnd` connect by *name*, with no drawn wire. A traversal over
  geometric wires tells a blind student the signal ends at a labelled stub; a sighted
  student sees the label and knows better. `HdlExporter` solved this already.
- `Splitter`/`Binder` map bit ranges, so "the element at the other end" is really "bits
  3..0 of the other end."
- `SubCircuit` makes the graph hierarchical. A traversal with no "descend / ascend"
  move cannot reach most of a real lab circuit, and #380's flat child-per-element model
  has no place to put that move.
- Unattached inputs read as 0 (`docs/simulation-semantics.md`); the narration must say
  "unconnected", not fall silent.

#380's relation set — one `AccessibleRelation` per wire between its two endpoint
elements — is geometric and therefore produces a *wrong* graph for any circuit that
uses jumps or subcircuits, i.e. every circuit past week three of a course. Building
#739 on it inherits the error and hard-codes it into the announcement layer.

## 2. Reframing A — put the narration in the kernel, ship it as batch output first

Build `jls.core.CircuitOutline` (headless, no AWT): nodes keyed by `ElementId`
(`/home/user/JLS/src/jls/elem/Element.java:619`), edges = normalized nets with
port-level endpoints, hierarchy edges for subcircuits, plus a `role(...)` mapping total
over `ElementRegistry` (`/home/user/JLS/src/jls/elem/ElementRegistry.java:38-77`, the
totality test already exists as `ElementRegistryTest`) and a phrase renderer that turns
an (element, port, net) triple into the AC-2 sentence. Derive it from the same
normalization `HdlExporter` performs — lift that code down into core rather than copy it.

Then every consumer is thin:

- **#546's prose narrative** = one deterministic fold over the outline in part-to-whole
  order. Byte-identical across platforms for free, because nothing renders.
- **#380's accessible tree** = an adapter: `getAccessibleChild(i)` reads outline node
  *i*; relations read outline edges. It stops being a design problem and becomes ~150
  lines of glue.
- **#739's traversal** = a cursor over outline nodes plus four moves (next/prev port,
  follow net, descend/ascend), living in the shared `Action` layer, satisfying AC-4 by
  construction.
- **#741's live announcements** = outline cursor + `Simulator` value at that net.
- **A new user-visible artifact today:** `jls -describe circuit.jls` on stdout, under the
  existing stability contract in `docs/batch-interface.md`. That single flag is
  gradeable, diffable in version control next to `-savetext`, usable by an instructor
  writing a handout (#875, #714), and testable by a golden file in the existing
  `test/jls/` suite with no display and no screen reader.

The consequence for scheduling is the interesting part. The adversarial comment on this
issue correctly computes the chain #316 → #380 → #737 → #739 and concludes the task is
not schedulable today. **Under this reframing most of #739's substance is schedulable
this week**, because the outline and its phrase vocabulary need no harness, no coverage
floor on `jls.edit`, no `AccessibleContext`, and no Orca. Only the last mile — wiring the
cursor to Swing and speaking it — sits behind #737's kill gate. That is the right place
for a kill gate to sit: in front of the 20% that can fail for platform reasons, not in
front of the 80% that cannot.

## 3. Reframing B — traversal is graph-aware *selection*, and the view is a real widget

Two mechanisms in this issue are riskier than they need to be.

**The cursor.** #739 implies a second position concept inside the canvas: focus is on
the panel, and a hidden "current element" moves within it. That is a shadow focus model —
exactly what AC-4 says it does not want — and exposing it to AT requires the
`ACTIVE_DESCENDANT` property-change dance, the least well-supported corner of the Swing
accessibility bridge. But the editor **already has** a position concept: the selection.
It renders, it is what every op addresses, it is undo-clean, and it is what a sighted
user manipulates. Make the traversal moves ordinary selection commands — *select driver
of this input*, *select loads of this output*, *select next port*, *enter subcircuit* —
registered in #75's shared `Action` layer and available from the menu. Now the blind
student and the sighted student are moving the *same* state, the announcement layer only
ever has to describe "the current selection", and sighted keyboard users get a feature
schematic editors are genuinely asked for ("where does this wire go?"). An
accessibility-only mechanism with one user constituency decays; ARCHITECTURE.md's
recorded i18n decision is this project's own written statement of that failure mode. A
mechanism both constituencies use does not decay.

**The view.** Rather than betting the band on AT-SPI reading a bespoke accessible tree
over a custom panel, render the outline in a dockable **Circuit Explorer** `JTree`. Swing
ships mature, bridge-tested accessibility for `JTree`/`JList`; names, roles, expansion,
selection and keyboard traversal all come from the toolkit, and this repo already ships
the pattern — `jls.Help` is a Swing TOC tree over generated content
(ARCHITECTURE.md, "Module layout"). Traversal then costs a `TreeModel` over
`CircuitOutline` and a selection sync, and #380's canvas `AccessibleContext` degrades
from prerequisite to nice-to-have. The explorer pane is also useful to everyone: finding
a gate in a 200-element circuit, jumping across a subcircuit boundary, seeing hierarchy.

## 4. What I am explicitly disregarding, and why

- **AC-2's "derived from the accessible model"** — inverted. The accessible model should
  be derived from a core narration model, not the other way round. As written it makes
  a Swing artifact the source of truth for a sentence that also has to appear in a batch
  export (#546) and, later, in a VPAT claim (#754).
- **AC-3's assertion target** — assert against `CircuitOutline`, not "the accessible
  model." Asserting against the accessible model is what #380 §11 already admits proves
  nothing about what a user hears; asserting against a core model at least also pins a
  shipped artifact (`-describe`) that users consume directly.
- **AC-1's completeness phrasing** — "every connection in a connected circuit" is
  undefined until jumps, splitters, subcircuit boundaries and unconnected pins are named.
  Replace it with: reachability is asserted over the *normalized* net graph, and a
  circuit fixture containing a jump, a splitter and a subcircuit is part of the fixture
  set. Otherwise the criterion passes on two AND gates and fails on any real lab.

## 5. Does it strengthen the arc?

The goal does; the construction pulls against it in two measurable ways. It adds new
bulk to the class #84 exists to decompose (a cost #380 already records against itself),
and it puts a description vocabulary in `jls.edit` that `docs/grand-architecture.md`
§5 would place in `core` — where `hdl`, `batch` and the accessible bundle can all reach
it. Cut along the core seam instead and this task stops being the fourth circuit walker
and becomes the one that retires the other three.

**Recommended disposition:** keep #739's Outcome verbatim; split it into (a) a core task
for `CircuitOutline` + phrase vocabulary + `-describe`, schedulable now, independent of
#316/#380/#737, and jointly owned with #546; and (b) this task, reduced to cursor
Actions + the Circuit Explorer view + the Orca assertion, ordered after #737 only.
