# TASK-0107 - The element-provider discovery path

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** TASK-0106

## Deliverable

External element descriptors discovered through `ServiceLoader` on top of the
element registry, with the sealed-hierarchy wall stated honestly and a path
through it that costs no permits change.

Precisely what changes:

1. **`src/jls/elem/ElementProvider.java`**, new - the service type:
   `Collection<ElementType> types()` plus `String providerId()`. The service
   interface is **JLS's own**, not `ElementType` itself, so a provider can
   contribute several descriptors, can be identified in diagnostics, and can be
   rejected as a unit.
2. **`src/jls/module/ServiceLoaderDiscovery.java`**, new - a
   `ServiceLoader.load(ElementProvider.class, loader)` pass that runs inside
   `ModuleRuntime`'s register phase and contributes every discovered
   `ElementType` to `elem.element-provider`. Discovery is **opt-in by
   invocation**, never by a file: a `-providers <dir>` flag
   (`src/jls/JLSStart.java:759-789`, 14 specs at HEAD, plus its
   `CliFlagTableTest` row) names a directory whose jars are added to a child
   class loader. A default launch discovers nothing and opens no path.
3. **The sealed wall, and the way through it.**
   `src/jls/elem/Element.java:17-18` permits exactly `DisplayElement`,
   `LogicElement`, `Wire`; `src/jls/elem/LogicElement.java:17-21` permits 24
   subclasses. **An out-of-tree class cannot be an `Element`.** Two paths exist
   and one is recommended:
   - **(a) unseal `LogicElement`** behind a documented SPI base class. Rejected:
     it deletes the compiler-enforced totality that makes every exhaustive
     switch over the hierarchy a build-time contract - the property that makes
     adding an in-tree element a ~65-line, mechanically-checked registration tax
     rather than a hunt.
   - **(b) `src/jls/elem/ForeignElement.java`** - a new *in-tree* permitted
     `LogicElement` subclass that delegates behavior (`react`, pin geometry,
     save/load of a bounded attribute map) to an out-of-tree
     `ForeignElementBehavior` interface. **Recommended.** Costs one permits
     entry once, keeps every switch total, and makes the trust boundary a single
     class instead of the whole hierarchy.
4. **The confinement rules a provider runs under**, enforced not documented:
   a provider's descriptors are namespaced (`providerId:tag`) so a provider
   cannot shadow a built-in tag; a provider that throws during discovery is
   reported and skipped, never fatal; a discovered type is **not** added to the
   network allowlist (see Notes).
5. **`docs/extension-points.md`** - the `elem.element-provider` row's status
   moves from "typed now (#78 shipped; #212 external)" to consumed-and-external,
   with the discovery mechanism and the `ForeignElement` boundary named.
   `docs/grand-architecture.md` §4.3's out-of-process reservation stays as
   written and is cited, not restated.

Done means: a jar built outside this repository, dropped into a directory named
on the command line, contributes a placeable, savable, loadable element - and
does so through exactly one in-tree class.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-050 | The external half of the feature: the registry that TASK-0106 made load-bearing becomes reachable from outside the build. |

## Prerequisite tasks

| TASK-NNNN | why |
|---|---|
| TASK-0106 | A discovered provider contributes into the extension registry. At HEAD, and until TASK-0106 lands, **nothing reads that registry for dispatch** - so a discovered provider would boot correctly and be invisible. That is not a preference; it is the measured HEAD state, recorded in `JlsModules`' own javadoc. |

## Acceptance test

`test/jls/module/ServiceLoaderDiscoveryTest.java`, new. The fixture provider is
built into `target/test-classes` under its own `META-INF/services` entry and
loaded through a child class loader, so the test exercises the real mechanism
rather than a hand-constructed list:

- `aDiscoveredProviderContributesItsTypesToTheRegistry()`.
- `aDiscoveredTypeIsPlaceableSavableAndLoadable()` - place a `ForeignElement`
  through the op layer, save the circuit, reload it in a JVM state where the
  provider is present, and assert byte-identical canonical saves.
- `aFileNamingADiscoveredTypeWithoutTheProviderFailsLoudly()` - the same file
  loaded with no provider must produce
  `LoadError.Category.UNKNOWN_ELEMENT` with the provider id in the message, not
  a silent drop. This is the failure mode a plugin system must get right.
- `aProviderCannotShadowABuiltInTag()` - a provider declaring `AndGate` is
  rejected, naming both.
- `aProviderThatThrowsDuringDiscoveryIsReportedAndSkipped()`.
- `noProvidersAreDiscoveredWithoutTheFlag()` - the default-closed assertion.
- `discoveryOrderIsDeterministic()` - `ServiceLoader`'s iteration order follows
  class-path order; sort by `providerId()` before contributing, and assert two
  runs over a shuffled directory listing produce the same order.

`test/jls/CollabSecurityRatchetTest`, extended:

- `classForNameStaysAtItsPinnedSites` (`:110-126`) - the pinned-site set gains
  `ServiceLoaderDiscovery` **explicitly**, with a justification comment.
  `ServiceLoader` is reflection; leaving the ratchet to notice it later is how
  a security ratchet stops meaning anything.
- `networkInputCannotNameADiscoveredType()` - new: a discovered type's tag must
  not be accepted by `ElementVocabulary`.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| 212 | Element-provider plugin API: discover external ElementType descriptors via ServiceLoader atop the #78 registry (the recorded replacement for the removed XML loader, #80 H2) | closes |
| 78 | Element descriptor and registry: self-describing elements, a compiler-enforced authoring contract, capability interfaces, one Orientation enum | informs - #212 is explicitly "atop the #78 registry"; the descriptor type is #78's |
| 224 | Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue) | tracking |
| 170 | Collaboration security hardening: closed op vocabulary, element-type allowlist for network input, caps, ratchet tests (collab cross-cutting) | overlaps - a discovery path that widened the network allowlist would undo #170; the separation is asserted by a test here |

Recorded decision, closed, cite as such and not as open: **#80** (the XML plugin
loader removal, whose recorded replacement sketch is this task).

## Notes

- **#212 carries a demand gate in its own text. It does not apply here.** A
  demand gate is a legitimate filter for third-party feature requests; applied
  to the project owner's own roadmap it inverts its own purpose. The question
  this task answers is the path and the cost, and both are written above.
- **A discovered type must never reach the network allowlist.**
  `src/jls/collab/op/ElementVocabulary.java:39-45` is the closed set a session
  peer may name. If discovery widened it, a peer could name a type the local
  user never installed - or worse, one they did, chosen by the peer. The
  allowlist stays registry-minus-deny-list over **built-in** types only, and the
  test above is the guard.
- **The sealed hierarchy is a feature, and the honest cost of keeping it is one
  class.** The measured in-tree registration tax for a new element is roughly 65
  lines across 12 files, demonstrated twice in a single commit; a new element
  type costs **zero format versions**. `ForeignElement` pays that tax once and
  then never again.
- **The class loader is the security surface.** A child loader with the
  application loader as parent, no `setAccessible` grants, and no privileged
  context. Host-touching providers stay out-of-process, per the reservation
  already recorded in `docs/grand-architecture.md` §4.3 - in-process host access
  is the one variant that cannot be walked back.
- **`ArchitectureRulesTest.collabDependsOnNoReflection`**
  (`test/jls/ArchitectureRulesTest.java:277`) is scoped to `jls.collab`, so it
  will not fire here - but `CollabSecurityRatchetTest`'s pinned-site set is
  repository-wide. Read both before adding the `ServiceLoader` call.
- **Discovery runs in the register phase, not lazily.** A provider whose element
  appears in a file that is loaded before discovery finished would fail
  spuriously. Eager, ordered, before any load.

## Evidence

- `src/jls/elem/Element.java:17-18` - `sealed ... permits DisplayElement,
  LogicElement, Wire`; `src/jls/elem/LogicElement.java:17-21` - 24 permits.
- `src/jls/elem/ElementRegistry.java:38` - the static `ALL` list, 35 rows;
  `src/jls/elem/ElementType.java` - the descriptor (tag, aliases, class,
  factory) a provider would contribute.
- `src/jls/boot/CoreModule.java:39-45` - the built-in contribution loop, the
  shape a discovered provider mirrors.
- `src/jls/elem/ElementExtensionPoints.java:16-27` - the seam's javadoc, which
  names #212 as its external consumer.
- `src/jls/Circuit.java:918-927` - `ElementRegistry.forTag` and the
  `UNKNOWN_ELEMENT` failure with its upgrade hint; the message a missing
  provider must extend.
- `test/jls/CollabSecurityRatchetTest.java:87-97` (`collabDoesNoReflection`),
  `:110-126` (`classForNameStaysAtItsPinnedSites`).
- `src/jls/JLS.java:11-17` - the class javadoc recording that the removed XML
  loader's replacement "should be a ServiceLoader-based registry".
- Do not restate: `docs/grand-architecture.md` §4.3 owns the out-of-process
  reservation; `docs/extension-points.md` owns the seam catalog.
