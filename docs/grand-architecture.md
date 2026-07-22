# The grand architecture JLS should have

A forward-looking companion to [`ARCHITECTURE.md`](../ARCHITECTURE.md).
Where `ARCHITECTURE.md` maps HEAD — where code lives *today* — this
document answers a different question: **given how JLS is actually used,
what it could become, and every currently-open issue, what is the single
most correct target architecture to converge on?** It is a determination,
not a work order: it names the shape, the keystone, the connective
mechanism, the layer boundaries, the sequence, and — just as importantly —
what is deliberately excluded.

The central finding, stated once and defended throughout: **the open
issues are not a backlog, they are a latent architecture — a set of
modules wired by dependencies and ordering that no one has yet drawn as a
whole.** All ~46 open issues map cleanly onto six layers over one enabling
triad, and the layers are already emerging as packages (`jls.elem` with a
shipped `ElementRegistry`, `jls.collab.{op,crdt,net,session}`,
`jls.hdl.{scan,yosys,layout,imp}`). What is missing is (1) the enforced
headless core boundary and (2) an explicit **module/plugin interface with
full dependency and ordering support** to replace today's implicit package
coupling and closed static registry. This revision centers the design on
that mechanism, grounded in a survey of how comparable tools and other
ecosystems solved the same problems.

## 1. What JLS is (the load-bearing facts)

Any target must hold these fixed, because they are the actual product:

- **A single self-contained jar** is the deployment model students and
  labs rely on; help works offline and version-locked to the binary
  (recorded decision, `ARCHITECTURE.md`). The architecture may not assume
  a network, a server, or an install step. This single constraint, more
  than any other, decides the plugin model (see §4).
- **Two co-equal front ends, not one.** The GUI editor (Swing) *and* the
  headless batch/grading surface (`-b -t`, VCD, image/Verilog export, the
  `ghcr.io/anadon/jls` container) are both first-class; the batch
  interface is already a stability contract (`docs/batch-interface.md`).
- **~69k lines of Swing-dominated Java**, single-maintainer, audience of
  first-year students and instructors. Assessed explicitly against a
  Kotlin/Scala rewrite → **no** (#96): Java-25 baseline plus Kotlin's
  transferable practices as *compiler-enforced* properties, incrementally.
- **A schematic-first model** — typed pins, bit-widths, wire nets,
  subcircuits, a discrete-event simulator. That element graph is the asset
  everything else walks.

## 2. What JLS could be (the latent product)

Three trajectories are already funded by real issues and working code:

- **A serious datapath / CPU teaching tool.** `riscv/` already builds a
  working RV32I CPU through the model, with a reference simulator and
  differential fuzzing; #201 (register file, sign/zero-extend), #199
  (synchronous memory), #202 (RV32I integration golden) push from gate
  toys to real processors.
- **An FPGA-deployment bridge.** #59/#61/#62/#63 stage HDL export →
  Yosys-netlist import → subprocess co-simulation; #213/#215 carry the
  drawn circuit to a bitstream on a named board. Settled stance:
  orchestrate external tools, never reimplement HDL semantics.
- **A collaborative editor.** #163 and its stack (#167→#168→#169→#170→
  #171) — simultaneous, pure-P2P, no server.

Each has a research doc, a tracking issue, and (for the first two)
in-tree code. The architecture's job is to let all three land *without a
rewrite between them* — which they can, because they share a spine, and
because that spine can be made an explicit module graph.

## 3. The keystone and the connective tissue

The most correct architecture has one non-negotiable structural move and
one organizing mechanism.

**The keystone (#77): one enforced-headless kernel.** Extract a
`jls.core` — circuit model, element registry, discrete-event simulation,
persistence, and the operation vocabulary — that imports no `java.awt`, no
`javax.swing`, no `jls.edit`. Make the GUI *one consumer* of that core,
not its center. Today there is no layering: the abstract `Simulator`
imports Swing and `jls.edit`; `Circuit` holds an `Editor` back-reference;
batch mode is headless only because a runtime flag is set; and `JLSInfo`
is a ~640-reference public-static hub wiring everything to everything.
This is the highest-leverage single change in the tracker, and — per the
tool survey in §7 — it is the one discipline on which JLS is already
*ahead* of Digital and Logisim (it has `HeadlessCoreRatchetTest`; they
grew GUI-entangled cores and pay for it; KiCad is now pushing the same
boundary *out of process*).

**The connective tissue: a module/plugin interface with dependencies and
ordering.** The six layers below are only real if something *enforces*
their boundaries and *wires* them deterministically. Today that wiring is
implicit (package imports + the `JLSInfo` bus) and the one explicit
registry — `ElementRegistry` (#78, shipped) — is closed by construction
(a static `List.of(...)`, no discovery). The determination of this
revision: **generalize that registry into a first-class module system.**
Every layer and feature-cluster becomes a *module* that declares what it
`provides`, what it `requires`, and how it orders relative to others; the
element-provider API (#212) stops being a special case and becomes the
canonical instance of the mechanism. §4 specifies that interface; it is
deliberately the *minimum* that captures the value, because JLS's
constraints (§1) forbid the heavy machinery (§4, "rejected").

**The enabling triad** — each is one seam of the module system, and each
is already partly built:

| Triad member | Issue | State | Role in the module system |
|---|---|---|---|
| **Headless core** | #77 | not yet extracted | the root module every other module requires; the boundary the ratchet enforces |
| **Element registry** | #78 | `ElementRegistry`/`ElementType` shipped, closed | the *seed* of the plugin mechanism; already a descriptor+factory table with a build-enforced totality test |
| **Operation layer** | #167 | `OpSink`/`CircuitOp` shipped under `jls.collab.op` | the single mutation entry point every dynamic consumer (collab, undo) observes |

## 4. The JLS module model (the plugin interface)

This section is the heart of the revision. It is synthesized from a survey
of JVM plugin systems (JPMS, `ServiceLoader`, OSGi, Eclipse RCP, Jenkins,
Gradle, IntelliJ), cross-ecosystem dependency/ordering theory (systemd,
VS Code, Emacs/Neovim, Cargo/npm/Maven, Nix, Debian), and how EDA tools
(Digital, Logisim, DigitalJS, KiCad) actually register and extend
components. Sources in §7. The model is the **smallest set of mechanisms
that captures ~90% of the value at ~10% of the framework weight**, chosen
against JLS's single-jar / single-maintainer / static-first / compile-time-
safe constraints.

### 4.1 The manifest — declarative, evaluated without running module code

Each module carries a static descriptor (a `record`, or an annotation
processed at build time), readable *without* loading the module's logic —
the universal precondition for ordering and lazy activation:

```
ModuleManifest(
  id:         "hdl.export",                 // stable unique name
  apiVersion: 1,                            // single integer; major = break
  provides:   ["hdl-export", "verilog"],    // capability tokens
  requires:   ["core", "element-registry"], // hard deps (token OR id)
  optional:   ["board-constraints"],        // soft: use-if-present, never a gate
  after:      ["core"],                     // ORDERING ONLY — separate axis
  before:     [],
  activation: OnCommand("export-verilog")   // Eager | OnCommand | OnEvent | OnDemand
)
```

### 4.2 The seven rules (each earned by a specific prior-art pitfall)

1. **Two separate axes: dependency ≠ ordering.** `requires`/`optional`
   answer *does it exist / is it a gate*; `before`/`after` answer *when do
   I run*. A `requires` edge implies an `after` edge, but `after` alone
   pulls nothing in. This is systemd's `Requires=` vs `After=` and Emacs
   `:requires` vs `:after` — the single most important decoupling, and the
   #1 bug source when fused. It lets JLS say "order after the board module
   *if present*" without forcing it to load.
2. **A strength ladder, not a boolean.** `requires` (hard, missing → fail
   loudly) vs `optional` (soft, missing → graceful no-op, and *never*
   eager-loads its target). Debian `Depends`/`Recommends`; IntelliJ's
   optional `config-file` slice; systemd `Requires`/`Wants`.
3. **Capability tokens with concrete-name fallback.** `requires:
   ["hdl-export"]` is satisfied by any module whose `provides` contains
   that token; 0 providers → hard error, >1 → error unless a `|`
   alternative is declared. This is Debian `Provides` / virtual packages —
   substitutability with a trivial string-set match, *not* OSGi's
   namespaced capability filters (rejected as over-weight).
4. **Do not own a version solver.** One jar ⇒ one version of everything by
   construction (Maven "nearest wins" / JPMS "refuse to choose"). Built-in
   modules share JLS's exact types. External modules check a single
   `apiVersion` integer (major = break) — no ranges, no SAT, no
   backtracking. Diamond-dependency and duplicate-type bugs are structurally
   impossible.
5. **Resolve once, at startup, by topological sort; fail loud on cycles.**
   Kahn's algorithm over `requires` + `after` edges; a remaining cycle is
   reported *with its path*, never papered over (Jenkins/IntelliJ do
   exactly this — not OSGi start-levels, which are overkill at JLS's scale).
6. **Two-phase init as the only cycle escape hatch.** Phase 1 `register()`:
   construct, publish `provides` tokens, touch no other module. Phase 2
   `start()`: resolve `requires`/`optional` references (now all present)
   and go live. A legitimate cycle is split across the phases (Spring bean
   construction + `afterPropertiesSet`; OSGi `INSTALLED→RESOLVED→ACTIVE`).
7. **Lazy activation, eager opt-in for the kernel core.** Default modules
   to a trigger (`OnCommand`/`OnEvent`/`OnDemand`); only the true core
   (`core`, config, look-and-feel) is `Eager` and topo-ordered at boot.
   This is VS Code `activationEvents` / lazy.nvim triggers / Emacs
   autoloads — and it *is* the literal meaning of #212's "gated behind
   demonstrated demand": a provider registers a cheap shim (a palette
   entry, a menu item) and its `start()` runs on first real use.

### 4.3 Discovery, isolation, and inversion of control

- **Discovery = static registration first, `ServiceLoader` for external
  providers.** The compiled-in modules are a registry list you edit —
  exactly today's `ElementRegistry.ALL` — so the common case stays fully
  type-checked and needs no framework. `ServiceLoader` (JDK-native, zero
  deps, works on classpath *and* module path, and `stream()` exposes
  provider `type()` *without instantiating*) is added only for external
  jars, only when #212's demand gate opens. Ordering never trusts
  `ServiceLoader` iteration order (explicitly unspecified) — the topo-sort
  in rule 5 owns it.
- **Type-safe SPI.** The module and extension contracts are **sealed
  interfaces** (`permits` the built-ins; `non-sealed` for external
  providers) with **record** manifests, under `@NullMarked` (#93/#94/#95).
  This is strictly better than every XML-descriptor system surveyed: the
  compiler enforces exhaustiveness and nullness instead of a runtime XML
  parse. It also unifies the modern-Java program with the module program —
  they are the same work.
- **Isolation: one shared in-process namespace for first-party; out-of-
  process for the untrusted and the external tools.** Compiled-in and
  first-party modules share JLS's types directly, so `instanceof`/casts
  stay sound and there is one version of every type — deliberately
  choosing Maven/JPMS single-version over npm/OSGi coexistence, whose
  per-plugin classloaders are the #1 pain source in Jenkins, IntelliJ, and
  OSGi. The *out-of-process* boundary (KiCad's protobuf-over-socket IPC
  model) is reserved for two cases where it earns its cost: **untrusted
  third-party providers** (crash isolation + a real trust boundary — the
  open question #212/#170 must answer before any such provider ships) and
  the **external tool integrations already run as subprocesses** (Yosys,
  GHDL, Icarus, ELK — where out-of-process *also* sidesteps the GPLv3
  linking hazard, e.g. ELK's EPL-2.0, that JLS already flagged).
- **The host publishes extension points; it never names a module.** The
  core defines typed extension-point interfaces (element provider, palette
  contributor, exporter, op observer); modules contribute implementations
  discovered through the registry. The dependency graph stays a DAG rooted
  at contracts, so adding a module never edits the core (Eclipse extension
  registry; the inversion-of-control seam).

### 4.4 What is deliberately rejected (the weight JLS does not pay)

- **OSGi / Equinox / Felix** — per-bundle classloaders, the dynamic
  "service can vanish mid-call" model, manifest ceremony, `uses`-constraint
  debugging. A full platform to maintain; pure cost for one jar with no
  hot-redeploy need.
- **Full JPMS modularization** — strong encapsulation but no versioning, a
  rigid static graph, split-package/automatic-module friction. Adopt the
  `uses`/`provides`/`ServiceLoader` *pattern* on the classpath; a single
  `module-info.java` is optional and only to police the public API surface.
- **Version-range solving / side-by-side versions** — every ecosystem that
  tried it (only OSGi truly did) paid heavily; the rest forbid it. One jar
  ⇒ "the version in the jar."
- **Per-plugin classloaders and, by default, process isolation** — in-
  process shared types give compile-time safety; the subprocess/sandbox is
  an opt-in layer for untrusted code only.

## 5. The six layers, as a module graph

The layers of the first determination survive intact — they are the coarse
dependency *tiers*. What changes is that the boundaries are now **declared
and enforced by the module manifests of §4**, not implicit. Every open
issue lands in exactly one module with no leftovers; that clean partition
is the strongest evidence the shape is right.

```
┌───────────────────────────────────────────────────────────────────────┐
│  DISTRIBUTION & SUPPLY CHAIN  (cross-cutting band; not a runtime module)│
│  reproducible jar+BOM · attested/signed installers · container image   │
│  #82 #134 #184 #185 #188 #190 #191         gate: #159 coverage ratchet  │
└───────────────────────────────────────────────────────────────────────┘
        ▲ packages and ships every module below; depends on none at runtime

  provides: editor, palette          provides: batch, vcd, image, hdl-*
┌─────────────────────────────┐   ┌─────────────────────────────────────┐
│  gui   (jls.edit / jls.ui)  │   │  batch/services & hdl               │
│  the ONLY AWT module        │   │  requires: core, element-registry   │
│  requires: core             │   │  batch/test-vector (#200 #214)      │
│  #84 #75 #76 #86 #73 #207   │   │  hdl.export/import/cosim            │
│  #208 #210 · #214 test panel│   │   (#59 #61 #62 #63) out-of-process  │
│  #91/#162 harness · #101 WL │   │  #213 boards · #215 bitstream       │
│  ext-points: palette entry, │   │  #202 RV32I golden                  │
│  test panel, collab overlay │   │  #212 external element providers    │
└──────────────┬──────────────┘   └──────────────────┬──────────────────┘
   after: core │ (consumers)                          │ after: core
               │                                        │
        ┌──────┴────────────────────────────────────────┴──────┐
        │  collab   (jls.collab.*, no Swing)                     │
        │  requires: core, ops · provides: collab               │
        │  #163 · #168 net → #169 session → #171 crdt (ordered)  │
        │  #170 security = the closed op vocabulary hardened     │
        └───────────────────────────┬───────────────────────────┘
                     requires: core, ops · replicates CircuitOps
┌────────────────────────────────────┴──────────────────────────────────┐
│  core   (jls.core)  —  THE KERNEL / ROOT MODULE   (#77, Eager)         │
│  provides: core, element-registry, sim, persistence, ops              │
│  requires: nothing   ·   imports no java.awt / javax.swing / jls.edit │
│                                                                        │
│   model         Circuit · WireNet · typed pins/bit-widths · subckt     │
│   registry      ElementRegistry + ElementType (#78) — the seed plugin  │
│                 mechanism; element providers are modules (#212)        │
│                 + datapath elements #201 · sync memory #199 · #198 fix │
│   simulation    discrete-event Simulator + BatchSimulator (headless)   │
│   persistence   FORMAT-versioned save/load; canonical serialization    │
│   operations    CircuitOp vocabulary + OpSink, stable ids (#167)       │
│                                                                        │
│   built with null-safety (#93) · records/value semantics (#94) ·       │
│   sealed exhaustive dispatch (#95)  — program #96, = the SPI's safety  │
└────────────────────────────────────────────────────────────────────────┘
```

Notes that the module framing clarifies:

- **The registry is the mechanism, generalized.** `ElementType` already
  records a deliberate *two-layer descriptor* split — a headless core half
  (tag/class/factory/aliases) and a separate GUI-side palette entry
  (icon/category/help/dialog). That is exactly a module contributing to
  *two* extension points: the `core` element-provider point and the `gui`
  palette point. #78's registry is not merely *like* the plugin system; it
  *is* its first instance, and #212 is the same instance opened to external
  providers. This is precisely how Digital (`ElementTypeDescription` +
  `ElementLibrary`, with saved subcircuits registered through the *same*
  path as built-ins) and Logisim (`Library → Tool → ComponentFactory`) do
  it.
- **Collab sits on `ops`, not on the GUI.** Its lower modules (net →
  session → crdt, in that `after` order) touch no Swing; only presence
  overlays reach up into `gui`. #170's "closed op vocabulary" is the
  network-facing hardening of the same `CircuitOp` grammar the editor
  submits — the reason the operation layer is worth extracting even before
  collaboration ships.
- **HDL and boards are `requires: core, element-registry`** and nothing
  else: the exporter walks the model, the importer uses the registry as its
  Yosys-cell→element table (#61), and everything external is a subprocess.

## 6. Two planes: modules compose the cold plane; simulation is the hot plane

A module/plugin system is a *composition and wiring* mechanism operating at
startup and human-interaction timescale. It must never sit on the
simulation inner loop, which touches signal values millions of times per
run — the hottest path in the program. The architecture keeps these on
opposite sides of a line:

- **Cold plane (modules).** Editing, persistence, ops, collaboration,
  export, plugin discovery, extension-point dispatch. Wired once by the
  §4 machinery; indirection here is free.
- **Hot plane (inside `core`).** The discrete-event loop runs entirely
  within the `core` module over the elaborated wiring graph, with zero
  plugin indirection, no capability lookup, no cross-module call per event.
  Results reach watchers (trace window, probes, collab presence) through a
  **batched, rate-limited** channel, never per-signal.

This is also where the Verilator lesson lands as a **recorded-decision
candidate**: JLS is today a pure discrete-event *interpreter* (like Icarus
Verilog) — fine for classroom gate circuits, but the `riscv/` CPU-scale
trajectory (§2) is exactly where Verilator's *elaborate-to-flat* approach
(levelize the graph once, run a statically-ordered evaluation pass, ~100×)
pays off. The determination: keep the event loop for interactive/animated
use, and — *if* CPU-scale designs become common — add a levelized compiled
evaluation pass **inside the `core` module, behind the same boundary**, as
a second simulator strategy. It is a core-internal optimization, invisible
to every other module, and it is the honest home for the performance
concern that a "put all data in one general store" design would instead
put fatally on the hot path.

## 7. Prior-art grounding

The determination is not invented; it is the consensus of the tool class
and of mature plugin ecosystems, adapted to JLS's constraints.

**EDA/logic-sim tools converge on four things** (sources: Digital
[repo](https://github.com/hneemann/Digital), issue
[#52](https://github.com/hneemann/Digital/issues/52); Logisim-evolution
[repo](https://github.com/logisim-evolution/logisim-evolution),
[architecture](https://deepwiki.com/logisim-evolution/logisim-evolution);
[DigitalJS](https://github.com/tilk/digitaljs) /
[yosys2digitaljs](https://github.com/tilk/yosys2digitaljs); KiCad
[IPC API](https://dev-docs.kicad.org/en/apis-and-binding/ipc-api/);
[Verilator](https://www.veripool.org/verilator/)):

1. **Core/GUI layering** — a model+sim+persistence core importing no GUI
   toolkit, editors and batch as consumers. JLS is *ahead* here (enforced
   ratchet); KiCad is pushing the boundary out of process for isolation.
2. **Component registration** — a self-describing *descriptor + factory*
   in a lookup structure, never a per-type switch; saved circuits
   registered through the *same* mechanism as primitives. JLS's #78 is a
   direct instance and would collapse its honest ~16-step element-authoring
   ritual to one.
3. **HDL interop** — export-first (framed as an FPGA deployment vehicle,
   not HDL teaching), CI-validated against real external tools that skip
   when absent; import only via Yosys `write_json` cell→element mapping,
   never a hand-written parser; co-sim via subprocess, never a commercial
   dependency (Logisim's Questa coupling is the anti-pattern). JLS's staged
   plan (#59–#63) already adopts this verbatim.
4. **In-editor test panels** — mainstream (Digital's `TestCaseElement`,
   Logisim's test-vector format + headless grading CLI). JLS's #214 is the
   small, HDL-independent catch-up item over its existing `-t` engine.

**Plugin/ordering ecosystems converge on** (sources:
[JEP 261](https://openjdk.org/jeps/261);
[`ServiceLoader`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/ServiceLoader.html);
[OSGi Core 7](https://docs.osgi.org/specification/osgi.core/7.0.0/);
[Jenkins deps](https://wiki.jenkins.io/display//JENKINS/Dependencies-among-plugins.html);
[IntelliJ deps](https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html);
[systemd ordering](https://fedoramagazine.org/systemd-unit-dependencies-and-order/);
[VS Code activation](https://code.visualstudio.com/api/references/activation-events);
[Debian Policy §7](https://www.debian.org/doc/debian-policy/ch-relationships.html);
[Cargo resolver](https://doc.rust-lang.org/cargo/reference/resolver.html)):
discovery and ordering are separate concerns (discover declaratively,
order by topo-sort); dependency and ordering are *separate axes*; a
required/optional strength ladder; lazy activation with an eager core;
and — for a small single-artifact app — *don't own a version solver* and
*don't reach for per-plugin classloaders*. Every one of these is a rule in
§4.

## 8. The dependency spine (what to build, in order)

```
#77 headless core  ──┬── is the root module; enables all of L4/L3 and the
                     │    enforced boundary the module manifests declare
#78 registry (done) ─┼── generalize the closed ElementRegistry into the
                     │    module/extension-point mechanism of §4
#167 op layer (done)─┴── the mutation seam collab (#168→#169→#171) requires
        │
        └─ on #165 stable ids + #166 canonical save (the merge/undo oracle)

#212 external providers ── the same mechanism, ServiceLoader-discovered,
                           opened only when the demand gate opens
#96 (#93/#94/#95) ── runs THROUGH core extraction and IS the SPI's type-safety
#84 (decompose SimpleEditor) ── the gui-module counterpart to #77; proceeds
                                as the op layer lands (#167 is its first slice)
```

The critical path is the enabling triad plus the module-mechanism
generalization: extract `core` (#77), turn `ElementRegistry` (#78) into the
§4 module/extension-point system, keep the op layer (#167) as the mutation
seam. Ship those and the three futures (§2) attach as modules rather than
demanding rewrites; L5 (gui) parallelizes because it is already a de-facto
consumer through leaky seams.

## 9. What the architecture deliberately excludes

Firm boundaries are as load-bearing as layers; re-proposing these
re-litigates a settled decision:

- **No general "central store for all data."** A path-addressed,
  lock-and-watch-per-access, homoiconic store is elegant for the cold
  plane but fatal on the hot plane (§6); the module system gives the same
  composition/merge/plugin benefits without taxing simulation, so the
  general store is not adopted.
- **No OSGi, no version solver, no per-plugin classloaders, no in-process
  untrusted plugins** (§4.4).
- **No central server for collaboration** (#163); pure-P2P is a constraint.
- **No in-house HDL simulation or parsing beyond a header scanner** (#59,
  #63); external synthesizer + JSON netlist import, subprocess co-sim.
  SystemC import out of scope.
- **No language rewrite** (#96); no internationalization until a course
  requests it; in-jar HTML help stays the offline student manual (all
  recorded).
- **No plugin execution surface ahead of demand** (#212, gated); the
  registry is closed today and opens via `ServiceLoader` only when a real
  user asks, with the trust/sandbox stance resolved first.
- **Installers are attested, not assumed reproducible** (#188); the jar+BOM
  are the reproducible artifacts.

## 10. Why this is the *most correct* shape

- **It falls out of the evidence.** Every open issue lands in exactly one
  module over one triad, with no forcing. A backlog that partitions this
  cleanly is describing a structure that already exists in intent.
- **It has a single keystone with maximal leverage.** The headless-core
  extraction (#77) is on the critical path of embedding, HDL, *and*
  collaboration at once; no competing single change has that reach.
- **The connective mechanism is minimal and prior-art-validated.** The §4
  module model is the smallest design the surveyed ecosystems converge on
  for a small single-artifact app — and it is a generalization of code JLS
  *already shipped* (`ElementRegistry`/`ElementType`), not a new framework.
- **It preserves both front ends and admits all three futures additively.**
  gui, batch, HDL, and collab are peer consumers of `core`; each attaches
  as a module without disturbing the others.
- **Its boundaries are enforced, not aspirational.** `HeadlessCoreRatchetTest`,
  `ElementRegistryTest`'s totality check, `-Werror`+NullAway, the coverage
  ratchet (#159), and a startup topo-sort with loud cycle detection make
  each boundary a compiler or CI obligation — the only kind of architecture
  a single maintainer can hold.

## 11. One-paragraph statement of the determination

JLS should converge on a six-layer architecture whose foundation is an
enforced-headless kernel (`jls.core`: model, self-describing element
registry, discrete-event simulation, versioned persistence, and an
invertible/serializable operation vocabulary), built with null-safety,
value semantics, and sealed exhaustive dispatch as compiler-enforced
properties. The layers are wired and their boundaries enforced by a
minimal **module/plugin interface with full dependency and ordering
support** — declarative record manifests; separate dependency and ordering
axes; a required/optional strength ladder; capability tokens with
concrete-name fallback; startup topological ordering with loud cycle
detection and two-phase init; lazy activation with an eager core;
`ServiceLoader` discovery for external providers behind a demand gate; one
shared in-process type namespace for first-party code and out-of-process
isolation reserved for untrusted providers and external tools — which is
the generalization of the `ElementRegistry` JLS already shipped, and the
smallest design the plugin-ecosystem prior art converges on for a
single-jar, single-maintainer, compile-time-safe app. Above `core` sit
three peer consumer modules — a pure-P2P collaboration module on the
operation vocabulary, a headless services-and-export module orchestrating
external HDL/FPGA toolchains out of process, and the Swing GUI as the sole
AWT-importing module — all packaged by a cross-cutting reproducible-and-
attested distribution band, with the simulation hot loop kept inside
`core` and off the module path entirely (and a levelized compiled
evaluation pass reserved there for CPU-scale designs). The critical path is
the enabling triad — #77 (core), #78 (registry, generalized into the
module mechanism), and #167 (operations) — and shipping it turns JLS's
three trajectories (CPU-scale teaching, FPGA deployment, collaborative
editing) into additive modules rather than rewrites.
