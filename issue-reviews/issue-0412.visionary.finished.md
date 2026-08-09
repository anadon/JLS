# Issue #412: TASK-0038: a program builds a circuit by naming verbs, not by emitting save text, and a mistyped attribute is a diagnostic instead of a silent drop
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Two things, fused into one task: (a) *a program can construct a circuit* — the
prerequisite for the RV32 machine (#326), the ternary CPU (#345), grading/import
(#300, #304), and the deletion of `riscv/jlsbuild.py`; and (b) *a mistyped
attribute name is loud*. Goal (a) is genuinely central to the project's arc —
`docs/grand-architecture.md` §2 names all three trajectories, and each of them
needs construction-without-a-canvas. Goal (b) is a real defect. But they are
defects at different layers, and welding them into one façade produces a design
that keeps the thing it says it removes. Below, the four places where I think a
better route exists, then what I would keep.

## 1. The headline property is not achieved by this design

"No verb writes save text and no verb reparses one" is the issue's own
Definition-of-Done bullet, verified "by inspection". But §7.5 *requires* a
private block renderer that emits `ELEMENT` text and reuses `Ops.escape`, and
the only way to get that text into a circuit is `AddElements(List<String>
blocks)` → `ElementBlocks.load` (`src/jls/collab/op/ElementBlocks.java:85`),
which runs a `Scanner` over the block and drives `Element.setValue`. So the
verbs write save text and reparse it on every `place`. The change is that the
emitter becomes `private`. That is a worthwhile encapsulation win — the format
stops being a public construction surface — but it should be stated as that, not
as its opposite, because the difference decides whether the next layer down ever
gets fixed.

The seam is one level lower. `CircuitOp`'s javadoc calls the vocabulary "a
sealed interface over data-only records"; in fact `AddElements` and `AddWire`
carry the *file format* as their payload, complete with file-local `int` id
renumbering inside `NetBlocks`. Every property #412 wants — typed construction,
attribute diagnostics, no reparse, a stable programmatic surface, a smaller
network attack surface for #170 — falls out of changing that payload from
`String` blocks to a typed descriptor plus an attribute map. The two halves
needed already ship: `ElementType` (#78, `src/jls/elem/ElementType.java`) gives
tag → class → `Function<Circuit, Element>` factory, and `Attribute`
(`src/jls/elem/Attribute.java`) gives name + `setInt`/`setLong`/`setBigInt`/
`setString`, each already returning "did I consume this?". Construct, then apply
attributes. No text, no `Scanner`, no escaping, no `MAX_BLOCK`.

## 2. The typo defect is a one-line loss in `Element.setValue`, not a missing API

`src/jls/elem/Element.java:344-395`: four `setValue` overloads loop over
`savedAttributes()`, and each `Attribute.setX` already *returns a boolean*
saying whether the name matched. The methods discard it and fall out of the
loop. O4 is that discarded boolean. Fixing it there — a strict mode that throws
`UnknownAttribute` naming the type and the token, with the lenient path retained
for the loader, where tolerance is deliberate forward compatibility for pre-fork
files — fixes it for the loader, for `AddElements`, for collab peers, for
`CircuitSnapshot` undo, for `NetlistImporter`, and for the verbs, all at once.
Fixing it in a builder fixes it only for callers who happen to use the builder,
and leaves the op layer — the *network* surface #170 hardens — still silently
dropping peer-supplied garbage. §7.11's "the defect the task exists to close"
is closed for one caller.

This also dissolves Open Question 2 entirely: nobody needs to reach `protected
savedAttributes()` from another package if the strictness lives on the object
that owns the list.

## 3. O2 misses the biggest in-tree generative path, and the miss changes the design

`src/jls/hdl/imp/NetlistImporter.java` — 1067 lines, in `src/`, shipped,
headless — builds a whole circuit from a Yosys netlist: ports, cells, gates,
muxes, wire nets, geometry from `HeuristicLayeredLayouter`, and then emits
`CIRCUIT` / `ELEMENT` / `WireEnd` save text (`:805`, `:815`, `:959`) as
`ImportResult(text, summary)`. Three consequences:

- "There is no supported way to build a circuit from a program" is not true.
  There is one; it is called Verilog import, it ships, and it is the emit-text
  idiom at production scale. O2's census (two *test-tree* builders plus an
  out-of-tree Python script) understates the problem and picks the wrong
  migration target.
- `NetlistImporter.Builder` already contains the model the verb set is
  reinventing — `Elem(id, saveType, attrs, width, height, ports)`, endpoint
  map, deferred readers, and a graph handed to a layouter. Its `attrs` map is
  precisely the `Map<String, Object>` §7.4 proposes, and it is exactly where
  the O4 typo defect bites *in shipped code*.
- §11 worries that migrating two test builders "is not the same as proving they
  work for a 580-element machine." Migrating `NetlistImporter` is that proof,
  and it is available now: the Yosys import goldens are a far more sensitive
  regression gate than the HDL export goldens, because they pin nets and
  geometry, not just module structure.

## 4. Migrating `CircuitTextBuilder` spends coverage the project cannot spare

Its javadoc states the point: "text exactly as the editor saves it, loaded
through the real loader so wire nets are the real thing." Reimplementing it over
the verbs converts 24 suites — including the batch and sequential simulation
goldens, the oracles `docs/simulation-semantics.md` cites — from *loader*
regression tests into *op-layer* regression tests, silently. §7.12 claim 3
presents this as cost-free because the signatures do not change; the signatures
are not what those tests are for. Keep `CircuitTextBuilder` on text as the
loader fixture it is, add the verbs as an independent second path, and let the
parity test be the bridge. This also removes §11's own trap — the parity test
degrading into comparing the verbs with themselves — because the text path is
never deleted.

## 5. Two smaller structural misjudgments

**Package home (OQ1).** `docs/grand-architecture.md` §3 already places "the
operation vocabulary" inside `jls.core`, the enforced-headless kernel. A
construction API whose named consumers are autograders, HDL import, and two CPU
builds is not a collaboration feature; calling it `jls.collab.op` to avoid
adding a package buys a week and pays for it permanently in the layer map, and
the eventual move breaks exactly the external consumers this issue is written
for. `jls.core` is already in `CORE_PACKAGE_PREFIXES`, so P9 holds by
construction and OQ1 dissolves.

**Vocabulary vs. registry (§7.4, P5).** `ElementVocabulary.ALLOWED` is 34
hardcoded tokens; `ElementRegistry` is total over loadable types. Routing
*trusted in-process* construction through the *untrusted-input* allowlist means
a first-party program cannot place a type a first-party file may contain, every
new element must be added to a security list to be constructible, and a #212
provider-contributed type can never appear at all — which the issue's own
related-work row flags and then resolves the wrong way. Existence checks belong
to `ElementRegistry`; `ElementVocabulary` belongs where bytes arrive from a
peer, in `CircuitOpReader`.

## 6. Criteria I would disregard, named explicitly

- **P3 (byte-identical saves) as a success criterion.** It forces the verbs to
  reproduce a serializer's attribute emission order and default geometry — the
  exact coupling the task exists to break — and H2's stated refutation path
  ("diff the two save texts and fix the verbs") is that coupling written down.
  The property that matters is semantic: same elements, same nets, same
  attribute values, same trace (P4). Canonical save bytes are #166's problem.
  Demote P3 to a diagnostic.
- **P8 as the regression gate.** HDL export is insensitive to geometry and to
  most attributes; a builder that places every element at the wrong coordinates
  passes all 70 goldens. A stronger gate is free: iterate `ElementRegistry.all()`,
  build one of every type through the verbs, and round-trip — totality the
  34-token list cannot give.
- **§7.12 claim 3** (migrate both test builders) — see §4 above.

## 7. What I would keep, and the alternative shape

Keep: construction-through-`OpSink` rather than a bypass; rejection over silent
no-op; the headless ratchet from birth; and the refusal to grow a second
grammar. Those are right and they are the project's discipline.

The alternative I would build instead, same destination:

1. Make `Element.setValue` strict-on-construct, lenient-on-load (#78/TASK-0003
   territory). O4 closes everywhere at once, including on the network path.
2. Add typed op payloads: `AddElements(List<ElementSpec>)` where `ElementSpec`
   is `(tag, Map<String, Value>)` resolved through `ElementRegistry`. `Ops.escape`,
   `MAX_BLOCK`, and `ElementBlocks.load` retreat to `CircuitOpReader`, where
   untrusted text actually arrives. The op layer becomes data-only in fact.
3. Add a headless `OpSink` — today the only implementation in `src/` is an
   anonymous class at `SimpleEditor.java:5547`. That class plus (2) is most of
   what "a program can build a circuit" means.
4. Ship the construction verbs in `jls.core` as thin sugar over (2)+(3):
   `newCircuit`, `place`, `connect`, `instantiate` (subcircuits — hierarchy is
   what a 580-element machine is *made of*, and its absence is the roadmap
   consumers' first blocker, not a documentable gap), `build`. Leave `remove`,
   `configure`, `probe` to the editing surface (#382); a builder that hands you
   a finished `Circuit` does not need to delete from it.
5. Let geometry be derivable: `jls.hdl.layout.SchematicLayouter` already turns a
   graph into coordinates. A construction API that demands x/y from every caller
   is a drawing transcriber, not a circuit builder — and hand-placing 580
   elements is the reason `jlsbuild.py` is unpleasant enough to delete.
6. Migrate `NetlistImporter` as the adequacy proof; leave the test-tree text
   builders alone.

Steps 1–3 are smaller than the task as filed, are useful to #170, #382, #61 and
#78 independently of the verbs, and make step 4 nearly free. That ordering is
the reframing.
