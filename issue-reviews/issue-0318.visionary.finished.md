# Issue #318: FEAT-014 (RESIDUAL): nets, groups and nested instances get names that survive sharing, and geometry becomes one record per view
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the machine block away and #318 asks for two things: (1) *every artefact a
person can point at must have a name that is a pure function of circuit content*,
and (2) *the same artefact must be placeable in more than one drawing*. Both are
worth having. Goal (1) is the through-line of #165/#166/#183 and is the precondition
for refdes determinism (#298), partition-independent watch names (#333) and any
op-stream replay (#299/#163). Goal (2) is what a breadboard canvas (#329) needs.

I endorse both goals and reject the mechanism the issue picks for them. The issue's
central move — a compound string `view:instancePath:sid` that becomes the persisted
key of a global sidecar geometry table — is one seam too coarse, and it manufactures
three of its own four Open Questions. There is a smaller shape that gets the same
capability, dissolves the open questions, and makes acceptance criterion 2 stop being
vacuous rather than being *marked* vacuous.

## Grounding (verified at the checkout)

- `src/jls/elem/Element.java:18` — `Element` is `sealed … permits DisplayElement,
  LogicElement, Wire`; `:24` mints the `sid`. `WireNet` (`src/jls/elem/WireNet.java:16`)
  is a plain class with no `save` and no id, exactly as the issue says.
- `src/jls/Circuit.java:1355-1382` — **wire nets are not loaded, they are recomputed**:
  `finishLoad` partitions `WireEnd`s into connected components and constructs
  `new WireNet()` per component. A net is a derived view over the wire graph.
- `src/jls/collab/op/AttachProbe.java:19` — a probe is attached to a **`Wire`** by
  `ElementId`, not to a net. Nothing in the shipped op vocabulary names a net.
- `src/jls/core/` already contains `Bounds`, `GridPoint`, `Orientation`, `Geometry`,
  `TextMetrics` — the geometry vocabulary is extracted; only its *storage* is not.
- `docs/plan/features/FEAT-014-*`, cited by §2 as the source of the ordering, **does
  not exist in the tree**. The cost band and the ordering rationale are unverifiable
  from the repository.

## Reframe 1 — the instance path is a runtime coordinate, never a persisted key

The issue treats "shared definitions arrive" and "geometry per view" as one problem
because both seem to need the path. They do not.

When FEAT-017 (#357) makes a definition shared rather than inlined
(`SubCircuit.java:287` is the inlining today), the contents of that definition are
*one drawing*. Two instances of a shared 4-bit adder should not have two different
internal layouts — that would be a bug, not a feature. So the geometry of an artefact
inside a definition is keyed by its **block-local sid**, full stop, and that key is
already unique and already enforced (`docs/file-format.md:394-396`). What genuinely
varies per instance is *annotation*: refdes, watch flags, per-instance parameter
overrides, probe names. Those are a small, enumerable set — not geometry.

This is precisely how the nearest prior art splits it. KiCad stores a symbol's
position once, in the sheet file; its `instances` block carries per-path
`reference`/`unit` only; and the PCB — the archetypal "second view" — keys its
footprints by **their own UUIDs**, carrying the schematic path as a *link field*, not
as the key. The tool that has lived with hierarchical multi-instantiation longest
deliberately does not key geometry by instance path.

Concretely, instead of TASK-0035's parseable compound key:

- Per-view geometry rows live **inside the `CIRCUIT` block they describe**, keyed by
  bare `sid`. Canonical order is then the already-shipped #166 sid order — no new
  ordering to specify, no `parse`/`toString` inverse pair to prove, no new grammar in
  `docs/file-format.md` §8 beyond a section header.
- The instance path becomes a *derived runtime* coordinate (a `List<ElementId>` walked
  from the root) used by diagnostics, refdes and watch naming. It is a value type in
  `jls.core` with an obvious `toString` for humans; nothing round-trips through it, so
  no injectivity proof is owed by the file format.
- Cross-view binding, where an artefact genuinely has an independent existence in
  another view (a breadboard chip), is an explicit **reference field** on the other
  view's own record — `boundTo <instance-path>` — not a compound key.

Under this shape §5 criterion 2 ("uniqueness survives a *shared* definition") is not
vacuous-pending-#357; it is *true by construction*, because uniqueness never leaves
the block scope where the reader already enforces it. The issue's own §7 REPLAN entry
for "FEAT-017 deferred indefinitely" then costs nothing.

## Reframe 2 — do not mint net identity; derive it

The issue calls net identity "the expensive half" and its Open Question 1 is a choice
between widening a `sealed permits` list and building a parallel identity holder.
Both options are expensive because both assume a net is a *thing that is saved*. It
isn't: `Circuit.java:1355` rebuilds every net from connectivity on every load, and
`UtilFunctionsTest#partitionRebuildsWireNets` pins that. Minting and persisting an id
for a derived object is the design error underneath both options — and it is the one
thing that puts invariant 1 (every existing file saves byte-identical) at risk, since
100% of existing files would gain new saved data on first save.

Third option the issue never considers: **a net's identity is a pure function of its
content** — e.g. the least `ElementId` among its member wires under the #166 canonical
order, or a hash of that sorted member list where a total order is wanted. Then:

- Criterion 3 (permute element order, ids identical) holds by construction.
- Invariant 1 holds trivially — nothing new is written to any file.
- Open Question 1 dissolves: `Element` stays sealed, `WireNet` stays a plain class.
- Open Question 2 (legacy minting) dissolves: there is nothing to mint.
- The address is a function of content, which is the property §3 says the whole
  feature is for, and is *strictly stronger* than a minted id, which is a function of
  creation history and only accidentally of content.

The cost is real and should be stated: a derived net id changes when the net's wires
change. But that is semantically correct — editing a net's wires changes which net it
is — and where a user wants continuity across such an edit, the right anchor is an
artefact they placed (a probed `Wire`, a future net-label element) that carries its
own `sid`. No shipped consumer needs net *permanence*: probes name wires, ops name
elements, and #333's requirement is partition-independence, which derivation gives.

## Reframe 3 — a view is a document, not a column

The `VIEW` section as specified is a sidecar table of `address → (x,y,w,h,orientation)`
with `schematic` carved out as special. That carve-out is load-bearing for invariant 1
and it will rot: the moment a second view needs anything geometry-adjacent that
`Element` already has, the special case grows a second exception. Worse, the table
presumes that a second view's atoms are *the same atoms* at different coordinates.
For #329 (breadboard) they are not: a solderless breadboard holds physical chips,
jumper wires and rails, and a 74-series chip is one physical part standing for a
subcircuit *instance*. That is a different graph with its own artefacts, not a second
x/y for a gate.

The more elegant cut: a non-default view is **its own block with its own element
records**, whose artefacts carry their own `sid`s and a `boundTo` reference into the
schematic's instance path. All views become symmetric — none is special — and
invariant 1 holds not by carve-out but because the schematic block is untouched by
construction. Skip-and-preserve (#319) is still the enabling mechanism; it just wraps
a block rather than a table. This also makes #329 tractable on its own terms rather
than forcing it through a coordinate table it will immediately outgrow.

## Alignment with the project's arc

`docs/grand-architecture.md` §2 names three funded trajectories: CPU/datapath
teaching, the FPGA bridge, and collaboration. The addressing half of #318 serves the
third and — via refdes determinism — the second. The **per-view geometry half serves
none of them**; its consumers are #329 (a sweep item) and capstones. Meanwhile #318 is
`blocked_by` #319 and #337, neither landed, and its own Cost section admits the
registry band exceeds its filed rows by 2.75-4.25x with the discrepancy unresolved.
For a single-maintainer educational tool, the honest sequencing is: derive net and
group identity (cheap, unblocks #298/#333 now, blocked by nothing), define the derived
instance-path value type (cheap, unblocks #299 diagnostics), and let per-view geometry
wait for #329 to actually need it — at which point it should be designed as Reframe 3
rather than pre-committed as a table today.

## Acceptance criteria I am explicitly disregarding

- **§5 criterion 1/2 and TASK-0035's `parse`/`toString` exact inverses.** Under
  Reframe 1 there is no persisted compound address to round-trip; uniqueness is the
  block-scoped rule that already ships. Criterion 2 is not "marked vacuous", it is
  retired.
- **§3's "one `VIEW` section per non-default view, rows of address → geometry".**
  Replaced by per-block sections (Reframe 1) or per-view blocks (Reframe 3).
- **Open Questions 1 and 2.** Both presuppose minted, saved net identity; Reframe 2
  removes the presupposition.

## What I would keep unchanged

The `schematic`-keeps-today's-bytes discipline and invariant 1; the DoD line requiring
the golden corpus to be byte-identical *demonstrated* rather than asserted; the
insistence that op inverses stay exact rather than approximate; the single view
vocabulary; and the refusal to add a second `(x,y)` pair to `Element`. Those are all
right, and they survive every reframing above.

## Verdict

**rethink.** The goals are correct and the issue is unusually honest about its own
vacuity and cost gaps. But its central artefact — a compound persisted address with a
global geometry table — is the wrong seam: it buys nothing that block-local sids plus
a derived path do not, it forces net identity into a saved-state design that fights
invariant 1, and it pre-commits a second-view storage shape before the only consumer
that needs one has been designed. Re-cut along content-derived identity + derived
instance path + per-view blocks, and roughly half of this feature disappears while the
capability it exists for arrives sooner.
