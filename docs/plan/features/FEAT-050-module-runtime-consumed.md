# FEAT-050 - Module runtime consumed: extension points and providers

**Status:** proposed | **Cost:** 5-10 mw | **Owner program:** P12 |
**Spine rank:** S8

## Capability delivered

The module runtime that already boots in every run mode stops being scaffolding
and starts being the thing that decides what the program does. Subsystems find
each other through declared, typed extension points instead of through compiled-
in call sites, and an element type can be contributed from outside the tree
through a discovery path rather than by editing a sealed permits list. A course,
a lab or a research fork can add an element or an exporter without forking JLS.

## Consumed by capstones

| CAP-NN | required/beneficial | what it needs from this feature |
|---|---|---|
| CAP-01 | required | per-view palettes and per-view element contributions must dispatch through the registry, and the op-observer notch must actually fan out |
| CAP-06 | beneficial | a course ships its own element or exporter as a module rather than a fork |
| CAP-16 | beneficial | the migration importer is exactly the shape of an out-of-tree contribution |
| CAP-02 | beneficial | the device subsystem is hosted in the module runtime that boots and is unread |
| CAP-03 | beneficial | the N-ary palette contributes through the registry that boots and is unread |

## Prerequisite features

| FEAT-NNN | why |
|---|---|
| - | **None required.** The registry, the four typed points, the resolver and the boot path all exist at HEAD; this feature is consumption, not construction. FEAT-001 is adjacent (both are registry-keyed totality problems) but neither blocks the other. |

## Prerequisite tasks

| TASK-NNNN | title | why |
|---|---|---|
| TASK-0106 | Consume the module registry for dispatch, with a typed catalog | Turns population into dispatch and finishes the catalog's pending rows |
| TASK-0107 | The element-provider discovery path | Discovers external `ElementType` descriptors through the service loader atop the registry |

## Acceptance criteria

1. At least one shipped seam dispatches through `ExtensionRegistry` rather than
   through a compiled-in call site, and removing a module's contribution
   observably removes the behavior - asserted by a test, not by inspection.
2. `OP_OBSERVER` fans out: a registered observer sees every `OpSink.submit`.
3. Every seam in `docs/extension-points.md` marked `pending` is either typed or
   its row states why it is still pending and who owns it. The existing
   bidirectional doc/code cross-check keeps passing.
4. An `ElementType` descriptor packaged outside the JLS tree is discovered,
   registered, drawable and simulable, with the trust boundary honored: the
   element-type allowlist that governs network-sourced content still applies.
5. Dispatch order is deterministic and pinned. Two runs with the same module set
   produce byte-identical goldens; module *declaration* order does not leak into
   observable behavior beyond the resolver's documented topological order.
6. Nothing on the simulation inner loop acquires plugin indirection - asserted
   as a structural test, not as a review convention.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 224 | Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue) | depends on / tracking - this feature is #224's consumption stage; #224 does not close on it alone |
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | closes - but see the correction below; most of #223 has already landed |
| 212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` atop the #78 registry | closes |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - its registry half already shipped; #212 builds on it |

## Design notes

**The premise, verified.** `JlsModules.boot()` runs from `JLS.main` before
`JLSStart.start()` in every run mode, and its own javadoc says the quiet part:
"the registry is populated but nothing reads it for dispatch yet"
(`src/jls/boot/JlsModules.java:15-35`). Grepping `ExtensionRegistry` across
`src/` returns hits only in `jls.module` (the definition) and `jls.boot` (the
population). A device module contributed today would boot correctly and be
invisible.

**A correction to the registry's scoping of TASK-0106.** The catalog half is
not outstanding. `docs/extension-points.md:28-36` already carries seven rows
with id, contract type, home package, cardinality, lifecycle phase and status,
and `test/jls/ExtensionPointCatalogTest.java` already cross-checks the document
against the constants **in both directions** (`:32-39`, `:90-118`). What
remains of #223 is the four rows still marked `pending`. Scope TASK-0106 to
dispatch consumption plus those rows; do not re-author what shipped.

**The hard constraint.** `docs/grand-architecture.md` §6 puts the simulation
inner loop entirely inside the hot plane with zero plugin indirection, and §6
is explicit that indirection is fine on the cold plane and fatal on the hot
one. This feature lives entirely on the cold plane. A device element
contributed through this path is an ordinary `LogicElement` once resolved; the
registry decides *that it exists*, never *what happens per event*.

**The governance gate.** `docs/extension-points.md` requires a row - id,
contract type, cardinality, lifecycle phase - before a new seam can exist.
Every new seam this feature opens files its row in the same commit, or the
catalog test fails, which is the desired behavior.

## Risks

- **Consumption changes behavior, population did not.** Booting is currently
  side-effect-free with respect to observable behavior, which is why the golden
  suite pins it for free. The first dispatch consumption removes that safety
  net; the acceptance criteria demand determinism tests precisely there.
- **The trust boundary is real.** An external element provider is untrusted
  code. The recorded #222 plugin trust boundary and the element-type allowlist
  that FEAT-052 hardens for network input both apply; a discovery path that
  ignores them hands CAP-01 a security regression it then has to buy back.
- **Cost band basis.** S8 is banded 1-2 wk for the consumption plus the
  `OP_OBSERVER` fan-out (`10-capstone-plan.md` §2.1); the registry's 5-10 mw
  covers both tasks including the external provider path and its tests. Report
  which half is funded.

## Evidence

- Boots but is unread: `src/jls/boot/JlsModules.java:15-35` (javadoc),
  `:49-56` (the four declared points); `BRIEF.md` §13 records the same finding
  against `src/jls/JLS.java:60`.
- No dispatch consumer: `ExtensionRegistry` appears only under `src/jls/module/`
  and `src/jls/boot/` at `addc6c5`.
- Catalog already shipped and cross-checked: `docs/extension-points.md:28-36`,
  `:38-50`; `test/jls/ExtensionPointCatalogTest.java:32-39`, `:90-118`,
  `:137-144`.
- Hot/cold plane rule: `docs/grand-architecture.md` §6 (`:314-342`).
- Cost: `10-capstone-plan.md` §2.1 row S8 (1-2 wk, score 1.33).
