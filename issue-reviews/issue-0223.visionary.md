# Issue #223: Extension-point catalog: enumerate and type the seams modules contribute to (element provider, palette contributor, exporter, op observer, …)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the graph bookkeeping away and the claim is: *JLS should be a host, not a
program.* A course should ship a jar with its own element, its own exporter, its
own theme, and JLS should load it without a fork (`#300`); a migration importer
should be an out-of-tree contribution (`#311`); collaboration should watch every
mutation through a declared notch (`#299`). That end is the right one and it is
consistent with everything else in the tree: README sells a classroom tool with
a batch/autograder surface and an HDL bridge; `docs/grand-architecture.md` §2
names the latent product; §4.3 fixes the inversion — *the host publishes points
and never names a module*. I endorse the destination without reservation.

What I want to argue with is the road. Three things in the codebase, read
together, say the chosen mechanism is heavier than the destination requires and
that the remaining work as specified pulls against the project's own direction.

## Observation 1 — the catalog apparatus exists to repair an erasure the project chose

`ExtensionPoint` (`src/jls/module/ExtensionPoint.java:26`) is a
`record(String id, Class<T> contract)` — the type-safe heterogeneous container
key. `ExtensionRegistry` holds `Map<String, List<Object>>` and casts on the way
in and out (`ExtensionRegistry.java` `contribute`/`contributions`). Everything
this feature still owes follows from that one decision:

- `docs/extension-points.md` must be *normative* (ARCHITECTURE.md §"Extension
  points") because the ids are strings and nothing in the compiler knows them.
- `ExtensionPointCatalogTest` (204 lines) must reflectively cross-check doc
  against constants **in both directions**, and must assert
  `contractsAreClosedTypes` — a test re-deriving what `sealed`/`final` already
  say to `javac`.
- "typed now" vs "pending" has to be a *tracked state* with an owner column,
  which is what created the `blocked_by: [61, 62, 84, 76]` close-out gate, the
  three-way argument with #403/#212, and Open Question 1.

Now look at what the erasure buys. Points can only be declared by the host:
`ExtensionRegistry`'s constructor takes a fixed collection, `contribute` rejects
anything undeclared, and `JlsModules.registry()` (`src/jls/boot/JlsModules.java:49`)
is the single declaration site in the tree. §4.3 says it will always be this way —
modules contribute, they never publish seams. So the dynamic string-keyed
indirection pays rent for a capability the architecture explicitly forbids.

**Reframing A — the module interface *is* the catalog.** Replace
`register(ExtensionRegistry)` with typed, defaulted contribution methods on
`JlsModule`:

```java
public interface JlsModule {
  default void elements(ElementCatalog c) {}
  default void palette(PaletteBuilder b) {}
  default void exporters(ExporterCatalog c) {}
  default void observers(OpSinks s) {}
}
```

Adding a seam is adding a method — a source change in the kernel, exactly as
expensive as adding a row + a constant + a doc paragraph today, but checked by
`javac` instead of by reflection over a Markdown table. The consequences are
what make this worth saying out loud: `ExtensionPointCatalogTest`'s
bidirectional check has nothing left to check (drift is impossible); the
"impostor point" and wrong-type failure modes cease to exist; `docs/extension-points.md`
becomes informative prose plus generated javadoc rather than a normative artifact
with a build-enforced parity contract; and **"pending row" stops being a state**,
because an unbuilt seam is simply a method nobody wrote. `#569`'s
frozen/evolving/internal taxonomy also gets a better anchor — `@ApiStatus`-style
annotations on real types beat a status column in a table.

## Observation 2 — #277 as written reinstalls the pattern #76 is deleting

`Circuit.java:918` resolves element tags through the static `ElementRegistry`
inside the load path. `Palette.java:218` resolves palette rows the same way at
class-init. `#277` §8 proposes routing both through "the boot snapshot", and
`#403` then has to invent a *global accessor that fails fast when read before
boot*. That is a process-wide mutable-until-boot singleton on the deserialization
path — structurally the same thing as `JLSInfo`'s statics, which ARCHITECTURE.md
records as ~126 call sites of technical debt that #76 exists to remove. The
feature would be adding one while a sibling removes the others.

There is a second, concrete hazard in the same place: contributions are currently
*derived from* the tables they would then feed. `CoreModule` contributes
`ElementRegistry.all()`; `GuiModule` contributes `Palette.entries()`, whose static
initializer calls `ElementRegistry.forTag`. Making those tables read the registry
closes a class-initialization cycle. The honest fix is not ordering cleverness.

**Reframing B — inject at the composition root; never publish a global.** Boot
builds the immutable `ElementCatalog`, `Palette`, `ExporterSet`, `OpSinks` and
*hands* them to `SimpleEditor`, to the batch driver, and to `Circuit.load` as a
parameter. Then: no accessor, no fail-fast contract (you cannot read what you
were never given), no init cycle, and #330's strongest carried criterion —
*remove a module's contribution and the behavior observably disappears* — becomes
a three-line unit test instead of a boot-level integration harness. `Circuit.load`
staying constructible in a test without booting the module system is worth more
to this project than registry-driven dispatch is, and injection keeps both.

## Observation 3 — the seven rows are four different mechanisms wearing one coat

- `elem.element-provider` → `ElementType`: a *descriptor table* keyed by save-file tag.
- `gui.palette-contributor` → `PaletteEntry`: not a seam at all — `Palette` is a
  *view over* the element catalog (every entry is built from an `ElementType`).
- `hdl.exporter` → `HdlEmitter`: a genuine strategy set, and the only row with two
  independent implementors.
- `collab.op-observer` → `OpSink`: a listener list. Java has had this since 1.1.
- `gui.theme`: cardinality "one active" — that is *configuration selection*, and
  `ExtensionRegistry` cannot express it. It appends to a list like everything else.
  The cardinality column is prose the mechanism does not enforce.
- `hdl.importer`, `app.command`: contracts that do not exist.

Only the **Contract** column is actually enforced by the mechanism; Cardinality
and Lifecycle phase are documentation asserted as normative. A catalog whose own
columns are half-unbacked, and 43% of whose rows are IOUs, is not the API freeze
that `#212` and `#569` are waiting behind.

**Reframing C — the catalog earns rows; it does not reserve them.** Normative
status only for a seam with ≥2 real implementors, at least one of which is not the
built-in table. Today that is `hdl.exporter`, and `elem.element-provider` the moment
#212 lands one foreign type. Everything else moves to an *informative* "seams we
anticipate, and who owns them" note — which fully discharges the "pending seams
are named here first, so nobody invents a parallel mechanism" rule, since that rule
needs a *paragraph*, not a build-checked row. **I am explicitly disregarding two
stated completion criteria:** §5 criterion 4 ("all seven rows typed") and the
`blocked_by: [61, 62, 84, 76]` close-out gate. Neither protects a user or a
contributor; they make this feature's closure hostage to four unrelated issues in
order to keep a table symmetrical. Open Question 1 then answers itself — not
because option (b) wins on bookkeeping grounds, but because the question stops
being about this feature at all.

## Observation 4 — sequence behind the first real out-of-tree jar, not behind completeness

ARCHITECTURE.md records that the inherited XML plugin loader was *removed* in
5.0.0 (#80) for being unreachable, with a `ServiceLoader` registry as the recorded
direction **if demand appears**; #212 is explicitly demand-gated. The tree now
contains ~1,650 lines of module machinery, four boot modules, and ~490 lines of
tests, contributing a mirror of tables it does not read. That is not a moral
failing — it was landed behavior-neutral on purpose, and cleanly. But it is the
same shape as the thing that was deleted, and it will stay that shape until one
real external contribution proves the seam.

**Reframing D — prove one seam end-to-end rather than wiring four.** Take
`elem.element-provider` only: a genuinely out-of-tree jar, discovered by
`ServiceLoader`, supplying one `ElementType`, injected per Reframing B, with a
`.jls` file naming that tag loading in the presence of the jar and failing with an
*intelligible* message without it (the `ForeignElement` boundary #212 names — note
no such type exists in the tree yet). One seam, one demo, one falsification. The
palette follows for free (it is a view). The exporter seam has two implementors and
can wait for a third. The op-observer read path costs nothing and can ride along as
#277 already recommends. This is smaller than the current remaining scope, it
retires the "populated but unread" embarrassment in `JlsModules.java:31-34` and
`JLS.java:60` that this feature's own DoD now tracks, and it converts the catalog
from a promise into a demonstration.

## Where the arc is genuinely strengthened

Three things this feature got right and should not be relitigated: the *host
publishes, never names* inversion; the hot/cold plane rule (§6 — the registry
decides *that a type exists*, never *what happens per event*, and #330's structural
`ArchitectureRulesTest` assertion of it is the single most valuable criterion
carried into this issue); and the determinism obligation — that the first dispatch
consumption removes the free golden pin and must land with its own determinism
tests. Keep all three verbatim under any reframing above; they are properties of
the destination, not of the mechanism.

## Verdict

**endorse-with-reframing.** The goal is right and central to JLS's next decade.
The route should shed the string-keyed erasure that forced the normative-doc
apparatus into existence (A), refuse the global boot accessor in favor of
composition-root injection (B), stop treating unbuilt seams as tracked table rows
and drop the four-issue close-out gate (C), and prove the seam with one real
out-of-tree element rather than wiring four consumers into a registry nothing yet
contributes to (D). Adopting A and C alone deletes most of the coordination load
this issue has accumulated — ten comments of merges, re-homes and half-edge
repairs around a remaining code delta plausibly under 400 lines.
