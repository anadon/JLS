# Issue #224: Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## The claim, restated

The title is two claims joined by "wired by": (a) JLS becomes a layered
headless kernel with GUI/batch/HDL/collab as peer consumers, and (b) the
wiring mechanism is a module/plugin system with full dependency-and-ordering
support. Claim (a) is right, is most of the way home, and should be the
project's spine. Claim (b) is where I part company: the evidence in the tree
says the module runtime is not the thing that produced (a), is not the thing
that will finish it, and the capstone's remaining payload is largely the cost
of making (b) load-bearing for its own sake.

## What actually produced the layering — and it wasn't modules

`HeadlessCoreRatchetTest`'s `BASELINE` is now `Set.of()`
(`/home/user/JLS/test/jls/HeadlessCoreRatchetTest.java:90`). Sixty
core-candidate files were cleaned to zero AWT/Swing/`jls.edit` imports across
`jls.sim`, `jls.elem`, `jls.hdl`, `jls.module`, `jls.core`, plus `Circuit` and
its load/save collaborators. That is a real, enforced architectural boundary,
and it was bought with a ~90-line regex test and a shrinking file list. It is
the single best thing in this program and it is barely mentioned in §2 —
because it isn't a feature, it's a predicate.

Now look at what the module runtime bought. `CoreModule.register()`
(`/home/user/JLS/src/jls/boot/CoreModule.java:39`) is a `for` loop copying
`ElementRegistry.all()` into a map. `GuiModule` copies `Palette.entries()`.
`HdlModule` copies two emitters. `CollabModule` contributes nothing at all —
its `register()` body is a comment. All four `start()` methods are comments.
All four manifests declare `provides: Set.of()`, `optional: Set.of()`,
`after: Set.of()`, `before: Set.of()`, `Activation.Eager()`. So of the seven
rules the determination doc earned from prior art — separate dependency and
ordering axes, the required/optional strength ladder, capability tokens with
concrete-name fallback, lazy activation triggers, two-phase init as the cycle
escape hatch — **zero are exercised by any shipped module.** ~1,200 lines of
`jls.module` + `jls.boot` topologically sort four nodes and three edges, then
discard the result: `JLS.java:60` calls `JlsModules.boot()` and drops the
return value.

The determination doc's own §10 defence is "it falls out of the evidence…
every open issue lands in exactly one module." But the issues landed in one
*package* each — `jls.elem`, `jls.hdl`, `jls.collab.*`, `jls.edit` — and Java
already had that partition, for free, with compiler enforcement. The manifest
graph restates the package graph in data, at runtime, where it can no longer
be checked by the compiler.

## The hole the module system doesn't see, and the ratchet wasn't pointed at

Here is the finding that decided my verdict. The op layer — the capstone's
"mutation seam," collab's wire vocabulary, the thing §4 criterion 5 requires
carry "no Swing types on the wire" — has this in its sealed interface:

```java
// src/jls/collab/op/CircuitOp.java:51
void apply(Circuit circuit, Graphics g) throws OpRejected;
```

Twelve of twenty files in `jls.collab.op` import `java.awt.Graphics`, and
`AddElements`, `SetElementConfig`, `FlipElement`, `RotateElement` import
`jls.edit.SwingTextMetrics` to convert that `Graphics` inside the op. An op
arriving from a remote peer, or replayed in a headless grading run, cannot be
applied. `jls.collab` is absent from the ratchet's `CORE_PACKAGE_PREFIXES`,
so nothing catches it.

The seam to fix it already exists and is already headless:
`jls.core.TextMetrics` (`/home/user/JLS/src/jls/core/TextMetrics.java`),
written for exactly this purpose, with `jls.edit.SwingTextMetrics` as the GUI
implementation. Changing `apply(Circuit, Graphics)` to
`apply(Circuit, TextMetrics)`, pushing the `forGraphics` call up to the ~6
editor call sites, and adding `"src/jls/collab/"` to `CORE_PACKAGE_PREFIXES`
is a day of work that delivers more of "layered headless kernel" than the
entire module runtime has. No criterion in §4 asks for it. That asymmetry —
elaborate machinery for a graph of four static nodes, while the kernel's own
mutation vocabulary imports the GUI package — is the strongest argument that
this capstone is organized around the wrong artifact.

## Two more places the plan pulls against the project's own determination

**Criterion 3 is a switchover whose success condition is that nothing
happens.** "When palette/menu/HDL dispatch reads the extension registry, the
element golden suite and the HDL goldens stay byte-stable." There are exactly
four dispatch sites (`Circuit.java:918`, `Palette.java:218`,
`ElementBlocks.java:118`, the `JLSStart` emitter selection). Replacing four
static lookups with four registry lookups, proven to change nothing, is pure
indirection — and per the #403 note in the 2026-08-08 comment it also *removes
the golden suite's free pinning of module wiring*, creating an ordering hazard
(#403 before #277) that exists only because the switchover exists.

**Outcome (6) contradicts §9 of the determination doc.** `docs/grand-architecture.md`
§9 lists as deliberately excluded: "No plugin execution surface ahead of
demand (#212, gated); the registry is closed today and opens via
`ServiceLoader` only when a real user asks." §1(6) then makes "drop an
external element-provider jar and see its elements appear" a capstone
acceptance step, and §2 makes #212 required with the minimality argument
"drop #212 and (6) fails." The issue's own Open Questions section concedes
the gate has no evidence behind it. A capstone cannot both honour the
architecture's demand gate and require the gated capability as an exit
criterion. This isn't a bookkeeping slip; it is the module thesis needing a
consumer badly enough to conscript the one the doc reserved.

**And the god class grew.** §2's motivation is that boundaries keep changes
"local to a module instead of threading through a 4,119-line editor."
`SimpleEditor.java` is now **5,852 lines** — up ~42% across the exact period
in which #77, #220, #222, #223 and the boot wiring landed. Whatever the module
program is doing, it is not making the editor smaller.

## The reframing I would take instead

I am explicitly disregarding §4 criterion 3 and §1(6). Keep the goal — kernel
plus peer consumers, three futures as additive work — and change the route.

**1. Promote the ratchet from a boundary check to the architecture.** Generalize
`HeadlessCoreRatchetTest` from one forbidden-import rule into a declared
allowed-import matrix: `jls.core` ← nothing; `jls.elem`/`jls.sim` ← core;
`jls.collab.*` ← core, elem, sim; `jls.hdl` ← core, elem; `jls.edit` ← anything.
That is ~40 lines on top of machinery already written, it catches every layer
violation (including the `jls.collab.op` → `jls.edit` one above) at build time
rather than boot time, and it needs no runtime, no manifests, no topological
sort. This *is* "boundaries are compiler- and test-enforced" — the phrase §2
already uses to justify the module system, delivered by the mechanism that
actually has the property.

**2. Keep manifests as documentation, delete the runtime.** `docs/extension-points.md`
is genuinely valuable: seven named seams with contract, cardinality, lifecycle
and owning issue, cross-checked by `ExtensionPointCatalogTest`. Keep the
`ExtensionPoint` constants and that test. Keep the static tables
(`ElementRegistry`, `Palette`, the emitter list) as the contribution
mechanism — they are total-tested, type-checked, and deterministic by
construction rather than by Kahn's algorithm. Retire `ModuleRuntime`,
`ModuleResolver`, `ModuleManifest`, and `jls.boot`, or park them behind an
explicit "reserved for the day a third-party module exists" note.

**3. When demand appears, open exactly one seam.** `ElementRegistry.all()`
becomes `builtins ++ ServiceLoader.load(ElementProvider)`. One line, one
interface, the #222 trusted-opt-in stance intact, discovery still ordered by
the registry rather than by `ServiceLoader` iteration. No general extension
registry, no dispatch switchover, no #403/#277 ordering window. This is what
§4.3 actually prescribed before it was generalized into a framework.

**4. Cut the next seam at "element," not at "subsystem."** The measured ROI in
this repo is #78: PR #271 added two elements as one registry row plus one
palette row each, against a documented 16-step ritual. That is the pattern
with evidence. The next dose is not a module runtime — it is finishing #78's
residual (the four runtime-throw stubs as compile-time obligations) and
extending the descriptor to generate what still lives in six other places:
help topic + `Map.jhm` row, round-trip fixture, HDL emitter mapping,
`ElementVocabulary` entry. One row, whole element. That directly compounds all
three futures in §2 — RISC-V datapath elements land faster, emitter coverage
tracks the roster automatically, and the collab op vocabulary stays total over
element types by construction.

## What I would keep verbatim

The six-step Outcome Statement is the best thing in this issue and I would not
touch steps (1)–(4). "Do these six things, observe these six results" is a
falsifiable product-level acceptance test, and very few tracking issues in any
project have one. Under my reframing the residual capstone reads: build clean,
run headless with the boundary matrix green, launch the GUI, and walk a drawn
circuit through Verilog to an iCEStick bitstream. That is a genuine, shippable
identity — a headless-first, batch-and-CI-native logic simulator with an FPGA
on-ramp, which is precisely where §7 says JLS is already *ahead* of Digital and
Logisim. It is also ~90% true today. Step (5) is #163's, as the 2026-08-08
review argued; step (6) is conditional on demand that has not appeared.

## Governance note, because it bears on the vision

Three comments dated 2026-08-08 — 17:48, 18:22, 18:33 — each claim to
supersede the machine block, and the first two strike #168–#171 while the
third rules that they stay. A reader today cannot determine this capstone's
required set from this capstone. When the mechanism a project is building is
"deterministic dependency-and-ordering resolution," and the project's own
dependency metadata is in an unresolved three-way contradiction, that is worth
reading as a signal about where the real coordination cost lives. It is not in
the wiring of four compiled-in subsystems.

## Feature-set judgment under the reframing

Keep **#76** (retiring 163 `JLSInfo.` references is real decoupling with a
user-visible payoff — theming), **#167** (the op layer earns its keep as the
undo/scripting/replay substrate *once its `Graphics` dependency is removed*),
and **#264** (the board on-ramp is the differentiating capability). Narrow
**#223** to the catalog document and its test, dropping the runtime closeout.
Defer **#212** with `WAIVED:` naming the demand gate, per §9. That is a
capstone that can close on evidence rather than on indirection.
