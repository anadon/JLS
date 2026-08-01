# TASK-0067 - The host byte port seam

**Status:** proposed | **Cost:** 2 wk | **Blocked by:** none

## Deliverable

A sealed byte-exchange seam that exists only when a person grants it at
invocation, and that no `.jls` file can reach.

1. **New package `src/jls/io/`**, AWT-free, with a `package-info.java` carrying
   `@NullMarked` (`PackageInfoRatchetTest#everyPackageHasPackageInfo` and
   `NullMarkedRatchetTest#markedPackagesStayMarked` both fail on a new package
   without one).

2. **`sealed interface HostBytePort permits NullPort, StdioPort, FilePort,
   PipePort, PanelPort`** with four methods: `int poll()` (next received byte,
   or `-1`), `void emit(int b)`, `void close()`, and `String grantName()` -
   the string printed on the outcome line. `NullPort.INSTANCE` is the default
   and returns `-1` forever. `PipePort` is the in-memory test double; it is a
   permit, not a test-tree class, so it stays inside the seal.

3. **`src/jls/io/HostByteRing`** - a fixed-capacity single-producer /
   single-consumer ring. The host thread calls `offer(byte)`; the simulation
   thread calls `drainTo(HostBytePort)`. Overflow drops with a counter that the
   outcome line reports; it never blocks the host thread and never allocates in
   the drain.

4. **The drain point, and it is not inside `beforeEvent`.** `Simulator` gains
   `private HostBytePort hostPort = NullPort.INSTANCE`, a `setHostPort`, and a
   `protected final HostBytePort hostPort()`. The drain call goes in
   `runEventLoop` (`src/jls/sim/Simulator.java:215-243`) **immediately before**
   the `if (!beforeEvent()) continue;` at `:220-221`, not inside `beforeEvent`
   itself. `InteractiveSimulator.beforeEvent` returns `false` on a pause and on
   a step end (`src/jls/edit/InteractiveSimulator.java:736-810`), so a drain
   placed inside the hook is skipped for the whole time a user is paused - the
   exact case a console needs it most.

5. **No foreign thread posts.** `Simulator.post` is unsynchronized over a plain
   `PriorityQueue` with a single-thread contract
   (`src/jls/sim/Simulator.java:25,27,165-169`). Nothing in `jls.io` imports
   `jls.sim`; the ring is the only crossing.

6. **Grant, never ambient.** `JLSStart.FLAGS`
   (`src/jls/JLSStart.java:759-788`) gains
   `new FlagSpec("serial", Arity.REQUIRED, "port", "a host port", ...)` taking
   `none` (default), `stdio`, or a file path; `JLSStart.apply`
   (`:1024-1134`) gains the matching `case "serial"`. The granted door is named
   on the outcome line (`BatchSimulator.displayOutcome`,
   `src/jls/sim/BatchSimulator.java:562-572`). A loaded circuit never acquires a
   port: no attribute, no tag, no element field selects one.

7. **`test/jls/SessionBoundaryRatchetTest`**, in the
   `SocketConfinementRatchetTest` idiom
   (`test/jls/SocketConfinementRatchetTest.java:34-49` - a source-substring scan
   with an empty baseline): no `java.io`, no `java.nio.file`, no
   `ServiceLoader`, no host handle construction anywhere under `src/jls/elem/`.

8. **Governance in the same commit.**
   `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES`
   (`test/jls/HeadlessCoreRatchetTest.java:74-79`) gains `"src/jls/io/"`;
   `pom.xml` gains a `jls.io` PACKAGE rule at the `jls.sim` bar
   (`pom.xml:449-471`, 0.930/0.920/0.845); `docs/extension-points.md` gains an
   `elem.host-port` row recorded as a **sealed in-tree collaborator, not a
   contributable seam**, pinned by `ExtensionPointCatalogTest`.

## Enables features

| FEAT-NNN | what this unblocks |
|---|---|
| FEAT-032 | The door itself. Every other part of the feature - the element, the transcript, the pane - is a consumer of this seam. |

## Prerequisite tasks

None. TASK-0014 (long-lived batch with pause and clean interrupt) makes a
console session *useful*, but the drain point it needs exists at HEAD, and this
task neither reads nor writes anything TASK-0014 produces.

## Acceptance test

`test/jls/io/HostBytePortContractTest`:

- `everyPermittedPortRoundTripsAByte()` - a `@ParameterizedTest` over
  `HostBytePort.class.getPermittedSubclasses()`, so a sixth permit added later
  cannot skip the contract. Asserts `emit` then `poll` for the loopback-capable
  permits and asserts `poll() == -1` for `NullPort`.
- `aDeclinedBeforeEventStillDrains()` - **the regression that pins deliverable
  4**. A `Simulator` subclass whose `beforeEvent` returns `false` for the first
  N iterations still sees bytes offered during those iterations. Fails against
  any implementation that drains inside the hook.
- `bytesBecomeVisibleOnlyAtALoopBoundary()` - a byte offered from another thread
  during a `react` is not visible to `hostPort().poll()` until the next
  iteration of `runEventLoop`. This is the determinism property: host arrival
  timing cannot change an element's output *within* an event.
- `noGrantMeansNullPort()` - a `BatchSimulator` constructed with no `-serial`
  has `hostPort() == NullPort.INSTANCE`.

`test/jls/SessionBoundaryRatchetTest#nothingUnderElemTouchesTheHost()` - asserts
the empty baseline, so the first violation is the failing one.

`test/jls/CliFlagTableTest` needs no edit: `usage()` is generated from `FLAGS`
(`src/jls/JLSStart.java:801-803`) and the parser accepts exactly the table
(`:852`), so the suite sweeps `-serial` automatically.

## Related GitHub issues

| # | title | relationship |
|---:|---|---|
| - | the host byte port seam | **no issue** - the registry records the host-I/O and device layer as untracked |
| 223 | Extension-point catalog: enumerate and type the seams modules contribute to | overlaps - the catalog gains a row that says this is *not* a seam; recording the negative is the point |
| 224 | Grand architecture: a layered headless kernel wired by a dependency-and-ordering module/plugin system (tracking issue) | informs - `JlsModules.boot()` runs from `JLS.main` in every mode (`src/jls/JLS.java:60`) but nothing reads the registry for dispatch, so the static path is the correct one today |
| 212 | Element-provider plugin API: discover external `ElementType` descriptors via `ServiceLoader` atop the #78 registry | overlaps - a host-touching provider discovered through `ServiceLoader` is exactly what the seal forbids; the two must not be conflated |

## Notes

- **`JLSStart.apply`'s `default:` arm (`:1129-1132`) is commented "unreachable"**
  and calls `usageError`. Adding a `FlagSpec` without adding a `case` is
  therefore a *runtime* usage error, not a compile error. Add both in one edit.
- **`Arity.REQUIRED` operands are read through the checked helper**
  (`src/jls/JLSStart.java:1161-1169`), which rejects a missing operand and one
  beginning with `-`. A file path starting with `-` is not expressible; say so
  in the flag's help text rather than special-casing it.
- **`-serial` is an addition, not a format change.** `docs/batch-interface.md`
  §6 classes a new flag as "additions that cannot break a conforming consumer" -
  minor-version material, still requiring a CHANGELOG entry.
- **The outcome line is parsed by tests.** `displayOutcome`
  (`src/jls/sim/BatchSimulator.java:562-572`) prints one of four fixed strings;
  `docs/batch-interface.md` §3.1 freezes it. Append the grant as a *second*
  line rather than editing the frozen one.
- **`HeadlessCoreRatchetTest` takes path prefixes and its `BASELINE` is
  `Set.of()`** (`test/jls/HeadlessCoreRatchetTest.java:90`). Never add a line to
  it; `jls.io` must be born clean.
- Do not model this as a `Reacts` implementor. Nothing in `jls.io` is an
  element; the element reaches the port through `sim.hostPort()`.

## Evidence

- `src/jls/sim/Simulator.java:25,27` - `eventQueue`, `dupCheck`; `:36,38,44` -
  `now`, `maxTime`, `stopping`; `:165-169` - `post`; `:215-243` - the event
  loop and the `beforeEvent` call site at `:220`; `:252-255` - the hook.
- `src/jls/edit/InteractiveSimulator.java:626` - the `"Runner"` thread;
  `:736-810` - `beforeEvent` returning `false` on pause (`:770`) and on step end
  (`:805`).
- `src/jls/JLSStart.java:704-722` - `Arity`; `:759-788` - `FLAGS`; `:801-803` -
  usage generation; `:1024-1134` - `apply`.
- `src/jls/sim/BatchSimulator.java:562-572` - `displayOutcome`.
- `test/jls/SocketConfinementRatchetTest.java:34-49` - the ratchet idiom.
- `test/jls/HeadlessCoreRatchetTest.java:74-79,90` - core prefixes, empty
  baseline.
- `pom.xml:449-471` - the `jls.sim` package rule this one is modeled on.
- `docs/virtual-hardware-parity.md` L3 - the permits list, the `beforeEvent`
  adjudication and the grant-never-ambient rule, with the contradiction it
  resolves.
