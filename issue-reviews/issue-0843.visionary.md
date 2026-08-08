# Issue #843: TASK-C370-2: every element gets a dense integer index and a primitive column store exists beside the object graph, with behaviour unchanged by construction
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

FEAT-054 (#370) wants the runtime state of a circuit to stop being a graph of Java
objects, because at 6.8 objects per logic element the headers alone eat the capacity
target before a single field is counted. That goal is correct and I endorse it without
reservation. #843 is the seam PR: mint a dense `int` per element, stand up primitive
columns beside `Circuit.elements`, read nothing from them yet.

My finding is that this project has already decided how to get flat state, in writing,
twice — and #843 cuts along the opposite seam from the one it decided on. I am
disregarding AC-1 and AC-3 as written, and the reason is architectural, not editorial.

## 1. The project already specified this, and it is not element-indexed columns

`docs/grand-architecture.md` §6 ("Two planes") makes the hot/cold split the organizing
decision of the whole architecture: editing, persistence, ops, collab are the *cold
plane* (objects, indirection is free); the simulation loop is the *hot plane* inside
`core`, "invisible to every other module." It then names the Verilator lesson as a
recorded-decision candidate — **elaborate-to-flat, as a second simulator strategy
inside `core`**.

`docs/capability-roadmap/lf-02-compiled-evaluation.md` §2.1 spells out the same thing
at implementation grain, and it is worth quoting against #843's acceptance criteria:
the flat state is `jls.core.sim.Netlist`, built by a `NetlistBuilder` that flattens
hierarchy, unions nets across jumps, and "assigns dense integer ids **to nets and to
evaluation slots**, and keeps a bidirectional map `nodeId ↔ drawn Element / WireNet`.
**Build this map on day one.**" The state layout it prints — `long[] a, b, u` plane-major
*by net id*, `int[] width` per net, `int[] opBase, ops, result`, `byte[] opcode`,
`int[] order`, `int[] levelStart` — is indexed by signal and by evaluation slot. Not by
element. §5 adds: "**Independent and early: the elaborator.** Flattening + net union +
dense ids depends on nothing and is wanted by three consumers."

So the repository already contains a specified, dependency-free, three-consumer version
of exactly the seam #843 is trying to cut, keyed differently and derived rather than
co-resident. #843 does not cite it, and #370's four-row decomposition does not contain
it. That is the misalignment.

## 2. Columns *beside* the object graph cannot reach the budget, and this PR makes the coexistence permanent

K17-1 sets ≤150 B/element. Count what survives #843 and #846 by construction:

- `Element` (`src/jls/elem/Element.java:22-50`) carries `id`, `stableId`,
  `stableIdFromFile`, `x`, `y`, `width`, `height`, `uneditable`, `tracePosition`,
  `highlight`, `savex`, `savey`, `circuit` — ~12 fields plus a header, per object.
- `stableId` is itself a heap object (`ElementId`: `String replica` + `long counter`,
  `ElementId.java:184-186`) — roughly 32 B of *identity* per element, before #843's
  third identity is added.
- `LogicElement` adds `lx/ly/savex/savey` plus two `Vector`s (`LogicElement.java:33,35`);
  each `Vector` is an object plus a backing array — two more objects each.
- `WireEnd extends LogicElement` (`WireEnd.java:17`) and pays all of the above, times the
  wire-end population, which on the measured wire-heavy fixture is most of the circuit.

Header cost alone at 6.8 objects × 12-16 B is 82-109 B/element — 55-73% of the budget —
and #846 removes *state fields*, not objects. No task in the C370 roster deletes the
object graph. A feature whose acceptance is bytes-per-element therefore cannot pass on
this decomposition even if all six children land perfectly; the residue is already over
budget. #843 is the PR that turns "both representations exist" from an implementation
detail into a committed interface, and #851 ("the editor reads the flat state through a
view") then binds the editor to the flat side too, so neither representation can later
be deleted without re-opening both.

Under an elaborated netlist this problem evaporates rather than being solved: the flat
image is *derived and disposable*, the object graph is the cold-plane editing model, and
the capacity question becomes "how large a netlist fits", which is a question about
`long[]`s. It also makes #370's invariant 4 ("no second representation maintained by
discipline") true by derivation instead of by a test, and invariant 3 (editor per-edit
cost — the one that can veto the feature) true because the editor is not touched at all.

## 3. The index key is wrong for the objects that dominate

AC-1 says "every element." But the fat objects are largely not `Element`s:
`Put`/`Input`/`Output` are a separate sealed hierarchy (`Put.java:16`), and `WireNet` is
a plain class (`WireNet.java:16`). Neither gets an index under AC-1, and both are counted
inside the 6.8. Meanwhile `Wire` and `WireEnd` *are* elements and would each burn a slot
in every column even though a wire has no runtime state worth a column. An element-keyed
index is simultaneously too wide (drawn geometry) and too narrow (misses puts and nets).
Net-keyed plus slot-keyed, as lf-02 specifies, matches what actually holds state.

## 4. AC-1's allocator already exists — twice — and a third one is the thing #848 exists to police

`Circuit.getElementsInStableOrder()` (`Circuit.java:479-485`) returns every element in a
deterministic total order by stable id; `Simulator.initInputs` and
`Simulator.initSimulation` (`Simulator.java:151,196`) and `CircuitRenderer:262` already
consume it, and #181/#182 already depend on it being canonical. `Circuit.save` (~`:1490-1503`)
already assigns exactly a dense contiguous `0..N-1` over a deterministic order.

So the honest reframing of AC-1 is: **index = position in the canonical order**, not a new
allocator. Do that and #848 ("a test fails the moment either grows a private mapping")
becomes unnecessary — there is nothing to diverge, because the queue, the columns and the
save path all read one ordering function. A property you can delete is better than a
property you can test. Note the one real constraint this exposes and the issue does not
mention: "stable for the lifetime of the loaded circuit" is false the moment the editor
adds or deletes an element, so #843 is quietly asserting a lifetime the editor does not
respect. Elaboration makes that a non-problem: the netlist is rebuilt on elaborate, and
indices are only required stable within one simulation run.

## 5. There is a large, cheap win sitting in front of this one

`WireEnd.java:36-48`: every wire end permanently holds `myCopy` (cut/paste scratch),
`loadAttach`, `loadPut`, `loadTriState`, an eagerly allocated `LinkedHashSet<Integer>`
`loadWires` and an eagerly allocated `HashMap<Integer,String>` `probeMap` — all of it
load-time scratch, none of it ever cleared, allocated even for wire ends the editor
creates and never loads. That is two collection objects (~140 B of headers before a
single entry) plus boxed `Integer` keys per connection, on the object type that dominates
the 60,004-element wire-heavy fixture measured at ~2,150 B/element. Moving it to a side
table alive only during `finishLoad` is a contained change with no new indexing scheme, no
editor exposure, no interface commitment — and it is measurable by #842's own harness the
week #842 lands. If the wire-heavy baseline drops substantially from that alone, FEAT-054's
denominator changes and so does the shape of the argument for everything downstream.

## The alternative TASK-C370-2 I would file instead

**"An elaborator produces a flat netlist from a loaded circuit, and the HDL exporter is
its first consumer."**

- `jls.core.sim.NetlistBuilder` walks a `Circuit` once and emits dense net ids and slot
  ids, plus the bidirectional `nodeId ↔ Element/WireNet` map lf-02 demands on day one.
- Lift `HdlExporter`'s `UnionFind` net walk (`src/jls/hdl/HdlExporter.java:1103`, cited in
  lf-02 as `:1038-1109`) into it and make `HdlExporter` a consumer. This is lf-02's own
  recommendation and it means the seam PR has a *real client* rather than columns nobody
  reads — so "no behaviour change" is proved by the existing HDL-export goldens rather
  than asserted, and the diff removes a duplicate net-identity implementation instead of
  adding a second element representation.
- Element indices are `getElementsInStableOrder()` positions; nets and slots get their own
  dense ids from the builder. One ordering function, three id spaces that are honestly
  different things.
- Columns come next (#846's work), allocated *inside* the netlist, where nothing forces
  them to coexist with the objects.

This keeps every one of #843's stated protections — AC-2 (no `react` touched: the ESCAPE
shim is lf-02's answer and it is stronger), AC-4 (the builder lives in `jls.core`, which
is where the headless ratchet wants it), AC-5 (no format change) — and drops only the two
criteria that encode the seam I think is wrong: AC-1's element-keyed allocator and AC-3's
"beside the object graph."

## What would change my mind

If CAP-17's capacity target genuinely requires that the *drawn, editable* model fit at
scale — not just the simulation image — then the object graph must die and element-keyed
columns are on the right axis. But then #843's premise is inverted: FEAT-055's streaming
elaboration (#332), which #370 lists as a downstream *consumer*, is the same purchase,
because a design that must never materialize objects has to go from file to columns
without passing through `HashSet<Element>`. Either way, "columns beside the object graph"
is the one shape that reaches neither destination.
