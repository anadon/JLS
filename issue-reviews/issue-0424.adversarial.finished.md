# Issue #424: TASK-0067: a sealed host byte seam that exists only when a person grants it at invocation, drained at a loop boundary a pause cannot skip, and unreachable from any .jls file
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Method

Fetched issue #424 and its parent #324 (FEAT-032) via the GitHub API. Cross-checked every code citation (O1-O8) against the current checkout at HEAD (`bd54461`): `src/jls/sim/Simulator.java`, `src/jls/JLSStart.java`, `src/jls/sim/BatchSimulator.java`, `src/jls/edit/InteractiveSimulator.java`, `test/jls/SocketConfinementRatchetTest.java`, `test/jls/HeadlessCoreRatchetTest.java`, `test/jls/ExtensionPointCatalogTest.java`, `test/jls/CliFlagTableTest.java`, `docs/extension-points.md`, `pom.xml`, `ARCHITECTURE.md`. No build was run.

## Findings

### 1. (High) P10's acceptance criterion cannot be satisfied as written — it either goes unverified or forces the design it's meant to forbid

The issue's Definition of Done and P10 both require: *"`docs/extension-points.md` carries an `elem.host-port` row recorded as **a sealed in-tree collaborator, not a contributable seam**, pinned by `ExtensionPointCatalogTest`."*

But `test/jls/ExtensionPointCatalogTest.java:106-109` (`typedNowDocIds()`):

```java
String status = cells[7].strip();
if (!status.startsWith("typed now")) {
    continue;
}
```

Only rows whose Status cell starts with the literal text `typed now` are inspected by `docTableAndConstantsAgreeBothWays()` at all; every other status (including `pending`, and by construction "sealed in-tree collaborator, not a contributable seam") is silently skipped. So:

- If the new row's status is written as the issue specifies ("a sealed in-tree collaborator, not a contributable seam"), the test never looks at it — it is not "pinned," contrary to the issue's own claim. Someone could delete the row, mistype the id, or let it drift, and the test suite would stay green.
- The only way to make the test actually enforce the row is to give it a status starting with `typed now`. But `typed now` rows are cross-checked against a real `jls.module.ExtensionPoint` constant in one of the four `HOLDERS` classes (`ElementExtensionPoints`, `GuiExtensionPoints`, `HdlExtensionPoints`, `OpExtensionPoints`), and `idsAreUniqueKebabCaseAndPrefixed`/`contractsAreClosedTypes` then require that constant to carry a real, closed `contract()` type that things can register against via `ExtensionRegistry`. That is exactly the "contributable seam" shape D7 and H1 forbid for `HostBytePort`.

The issue asserts a false property of its own cited test. **Recommendation:** either drop "pinned by `ExtensionPointCatalogTest`" from P10/DoD (it isn't and can't be, without contradicting the sealing decision), or extend `ExtensionPointCatalogTest` with a new, explicitly-scoped assertion (e.g. a fixed set of "sealed, non-contributable" ids that must appear in the doc) — which is new test-authoring work this issue doesn't budget for.

### 2. (High) The binding authority for the whole design — Decision D7 — is sourced to a document that does not exist in this repository

§1 states: *"Decision D7 is binding and it decides the shape (`docs/plan/evidence/BRIEF.md` §12, landed in `3a81a4a7d6a0f108ec201e632732d308cc02b3fc`...)"* and quotes it verbatim as the reason `HostBytePort` must be sealed rather than a plugin seam.

There is no `docs/plan/` directory and no file matching `*brief*` (case-insensitive) anywhere in the checked-out tree at HEAD. `docs/` contains `docs/plan-features` style names but no `plan/evidence/BRIEF.md`. The sibling parent issue #324 cites the exact same document with the same caveat ("not present at `2d0ca9d`"), so this is a corpus-wide provenance gap rather than something unique to #424 — but #424 leans on it as the entire justification for the security property (sealed-not-pluggable) that the rest of the task exists to implement and ratchet. A reviewer or implementer picking this up cannot independently verify D7's wording, scope, or that it was ever actually ratified — they're asked to trust a citation to a file nobody can open. **Recommendation:** either land `docs/plan/evidence/BRIEF.md` (or an accessible equivalent) before this task is picked up, or restate D7's substance directly in this issue/ARCHITECTURE.md so the decision doesn't depend on an unreachable artifact.

### 3. (Medium) Evidence commit `2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` is not reachable from HEAD in this checkout

Every O1-O8 code citation and line number is pinned to that hash; `git cat-file -t 2d0ca9d...` fails and `git log --all` finds no match. The checkout is shallow, so this isn't proof the commit never existed upstream — but it means the specific claim "src/, test/ and pom.xml are byte-identical between that commit and the current default-branch tip" is unverifiable here. That said, the issue's own rule 6 asks implementers to re-verify O1/O2 at pickup time regardless, and I did so independently against current HEAD (`bd54461`): `grep -rn "System\.in" src/` → 0 hits, `src/jls/io/` absent, `-serial` not in `JLSStart.FLAGS` or accepted by the parser. **The substance of O1/O2 still holds today**, so this is a provenance/traceability weakness rather than a factual error.

### 4. (Medium) Task-size vs. sibling estimate is optimistic given the acceptance-criteria surface

Parent #324 prices TASK-0067 + TASK-0068 combined at "4 weeks exclusively owned," implying roughly 2 weeks for this task alone. The actual §8 checklist is: a new package (sealed interface + 5 permit classes + `HostByteRing`), `Simulator` wiring at a specific line, a new CLI flag with its `apply` case, a `BatchSimulator.displayOutcome` change, **two** new from-scratch ratchet tests (`SessionBoundaryRatchetTest`, plus the `HeadlessCoreRatchetTest` extension), a `pom.xml` JaCoCo PACKAGE rule pinned to the `jls.sim` bar (0.930/0.920/0.845 — a stringent floor for all-new code), a `HostBytePortContractTest` with at least 4 parameterized assertions, an overflow/non-blocking/no-allocation test (see finding 6), a `docs/extension-points.md` edit whose test-enforcement is broken (finding 1), and a CHANGELOG entry. This is a large amount of net-new infrastructure and test-authoring for a "task" tier estimate that a sibling issue already treats as roughly half of a 4-week budget.

### 5. (Medium) `PanelPort` is required to ship "wired to nothing," but its behavior contract is never specified

Open Question 2 resolves: "the permit ships now, minimally, wired to nothing... Blocks execution of the seal's shape." But §7.4's four-method contract (`poll`/`emit`/`close`/`grantName`) gives no guidance on what an unconnected `PanelPort` should *do*: does `poll()` return `-1` forever like `NullPort` (in which case what does it add over `NullPort` before TASK-0069 exists)? Does `emit()` buffer indefinitely, drop, or throw? The contract test `everyPermittedPortRoundTripsAByte()` (P2) is stated to run over *every* permitted subclass including `PanelPort`, so whoever implements this task has to invent a round-trippable behavior for a component that by design has no consumer yet — and that invented behavior becomes load-bearing API once TASK-0069 lands, without TASK-0069 having any say in it. **Recommendation:** specify `PanelPort`'s pre-pane semantics explicitly (most likely: behave exactly like `PipePort`, an in-memory loopback, until the pane exists) rather than leaving "wired to nothing" to be improvised.

### 6. (Low-Medium) P8's "never blocked" and "no allocation" criteria are asserted as testable but the verification mechanism is unspecified

§8's checklist item for P8 says "assert the drop counter and that the host thread never blocked" — but JUnit has no built-in mechanism for asserting non-blocking behavior or zero allocation. A test that merely asserts `offer()` returns within a generous wall-clock timeout, or that never actually measures allocation, would satisfy the checklist's letter (a green test named appropriately) while leaving the real requirement (no allocation on the hot drain path, guaranteed non-blocking `offer`) unverified. This is exactly the kind of acceptance-criterion gap the adversarial lens is meant to catch: the DoD item can be checked off by a test that doesn't actually prove the property. **Recommendation:** name the mechanism up front — e.g. a fixed-iteration timing ceiling for non-blocking, and either a JFR/async-profiler allocation assertion or a manually-instrumented allocation-counting harness for the no-allocation claim, rather than leaving both to whoever writes the test to interpret.

### 7. (Low) Minor: the four-line closed-set falsification table (§10) and the Definition-of-Done checklist are unusually rigorous and internally consistent

Noted as a positive rather than a defect: H1-H5 each name a concrete refuting observation, and most of them (H2/O4 in particular — the drain-before-`beforeEvent()` placement, verified against the current `runEventLoop` at `Simulator.java:215-221`) are correctly derived from the real code, not just asserted. The threading-safety framing (O5, "no foreign thread posts") correctly identifies the single highest-risk property of this feature and matches `ARCHITECTURE.md`'s documented threading model and the precedent of issue #49.

## Things that check out

- O1 (no `-serial` flag today), O2 (no `System.in`, no `jls.io`) — reproduced independently against current HEAD.
- O5 (`Simulator.post`/`eventQueue`/`dupCheck` single-threaded contract) — matches `Simulator.java:165-169` exactly.
- O6 (unreachable `default:` case in `JLSStart.apply`) — matches `JLSStart.java:1128-1132` exactly; the "add both in one edit" caution is well-founded, since a `FlagSpec` without a case fails only at runtime.
- O7 (ratchet idioms, empty baselines) — `SocketConfinementRatchetTest`/`HeadlessCoreRatchetTest` both confirmed to use the "empty baseline, substring-scan" idiom described; copying it for `SessionBoundaryRatchetTest` is a reasonable, low-risk pattern.
- H5/P11 (`CliFlagTableTest` needs no edit) — confirmed: the test is fully table-driven off `JLSStart.commandLineFlags()`/`usageText()`, so this claim is correct as stated.
- The relationship to #324/#212/#38/#49 is coherent and each is genuinely load-bearing rather than decorative; #212's `ServiceLoader` concern and this task's seal are correctly kept distinct.

## Verdict rationale

The core engineering — where the drain goes, why the ring is SPSC, why sealing beats a plugin seam, the CLI/output compatibility story — is well-grounded and independently verifiable against the real codebase, which is why this isn't `needs-rework`. But two acceptance-criteria defects are concrete and would ship broken if not caught: P10's "pinned by `ExtensionPointCatalogTest`" claim is false given the actual test logic (finding 1), and the entire premise rests on an unlocatable evidence document (finding 2). Those need fixing before the Definition of Done can be trusted at face value, hence `sound-with-concerns` rather than `sound`.
