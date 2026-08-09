# Issue #162: UI display lane hardening: fail-closed xvfb on the required Linux Build leg and the 20-run stability record for the display-tagged suite
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the CI vocabulary and the claim underneath is: **a green required check must be
evidence that the thing it names actually ran.** That is squarely the project's arc.
README stakes JLS on verifiable claims — reproducible jar with a `.buildinfo` recipe,
CycloneDX BOM, cosign/attestation verification commands, an explicit refusal to ship a
GPG signature whose custody story is weak. ARCHITECTURE's "Recorded decisions" section
exists so that guarantees are written down rather than assumed. A required leg that
passes while `DialogConstructionSmokeTest` silently ran zero of its 25 families is
exactly the unverifiable claim this project elsewhere declines to make. Arming the lane
is right, and it is the highest-value class of CI work: converting a lie into either a
truth or a red X.

So the goal is endorsed without reservation. The *mechanism* and the *evidence artifact*
are both misaligned with how this codebase already solves this problem class.

## Reframe 1: this is a ratchet, not a YAML edit

JLS has a mature, distinctive idiom for "a guarantee that must not silently erode": an
in-JVM ratchet test. `test/jls/HeadlessCoreRatchetTest` (no AWT/Swing/`jls.edit` imports
in the core), `test/jls/ui/DialogCoverageRatchetTest` (bytecode scan over every
`ElementFormDialog` subclass), plus `NullMarkedRatchetTest`, `PackageInfoRatchetTest`,
`SocketConfinementRatchetTest`, `PointerApiRatchetTest`, `CollabSecurityRatchetTest`.
The pattern is the project's signature move, and this issue proposes to solve a ratchet-
shaped problem with a shell conditional in `.github/workflows/ci.yml` instead.

The shell fix is also *weaker than its own prediction*. §5 P3 says "a green required
Linux leg implies the display suite actually ran," but §8's remedy only proves that apt
exited 0 and `xvfb-run` is on PATH. It does not prove execution. Every one of the 24
`@Tag("display")` classes carries its own private skip:

```
Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), ...)
```

(24 files under `test/jls/ui/`, e.g. `DialogConstructionSmokeTest.java:66`,
`InteractiveSimulatorSmokeTest.java:62`). That assumption fires on a broken `$DISPLAY`,
on a regression to `pom.xml`'s `jls.test.headless` default (`pom.xml:56`), on the
`display-tests` surefire execution being reconfigured (`pom.xml:278-284`), and on a
`@Tag` typo in a new class — none of which a fail-closed apt line touches. And P3's
verification in §14 is "surefire evidence … pasted in the PR": a human eyeballing one
run, not an invariant. The silent-downgrade defect would be fixed for exactly one of its
causes, verified once.

**Concrete alternative.** A `jls.test.require` capability property plus one helper and
one ratchet:

1. `test/jls/TestCapability.java` — `TestCapability.DISPLAY.requireOrSkip()`. If the
   capability is named in `-Djls.test.require`, a missing substrate throws
   `AssertionError` naming the capability; otherwise it degrades to today's
   `Assumptions.assumeFalse` skip.
2. Replace all 24 raw `assumeFalse(isHeadless())` sites with that call. Mechanical, no
   behavior change locally.
3. `CapabilityArmingRatchetTest` — a source/bytecode scan (same technique as
   `DialogCoverageRatchetTest.java:65`) forbidding raw `Assumptions.assume*` in `test/`
   outside the sanctioned helper, so new silent-skip sites cannot creep back in.
4. CI: the required Linux leg adds `-Djls.test.require=display`. Nothing else changes;
   `xvfb-run` absence now fails *inside the build*, with a message naming the missing
   capability, on every run forever.

This satisfies P1 and P3 by construction, and it fails closed on the causes the YAML fix
cannot see. It also costs less YAML than the issue's own §8, not more.

## Reframe 2: the same seam collapses #386, #406, and part of #91

The chain-integrity comment (2026-08-08) already notices the shape: #406 owns "zero
display-tagged tests executed must FAIL" for Windows/macOS, #386 owns whether external
tools are installed rather than self-skipped, and it advises "use one formulation across
all three lanes rather than three near-identical ones." The capability property *is* that
one formulation. `test/jls/hdl/ToolLocator.java` and `IverilogCompileTest.java:34` /
`GhdlCompileTest.java:35` use the identical `assumeTrue(tool != null)` idiom; they take
`TestCapability.IVERILOG` etc. for free. Then:

- Linux Build leg: `-Djls.test.require=display,iverilog,ghdl,yosys` — closes #162 and #386.
- Windows lane: `-Djls.test.require=display` — closes #406's assertion half.
- Local `mvn verify`: property unset, everything skips exactly as today, satisfying §7.12
  compatibility with no special-casing.

Four issues currently orbit one invariant with four bespoke mechanisms. For a
single-maintainer project, collapsing that graph is itself a deliverable. I would file
the capability seam as the unit of work and let #162 be the first consumer.

## Reframe 3: split the tag, and the stability question dissolves

The 20-run record exists only because arming is all-or-nothing: if arming means all 24
classes block every PR, you need evidence the flakiest of them is safe. But the suite is
not homogeneous. 16 of the 24 route through `EditorGestureSupport`, which drives
`java.awt.Robot` — that is the family `pom.xml:285-291` describes as having
"nondeterministic … window-manager-less realization timing" and the reason
`rerunFailingTestsCount=2` exists. The remaining set — `DialogConstructionSmokeTest`,
`DialogCoverageRatchetTest`, `InteractiveSimulatorSmokeTest`, `EdtViolationDetectorTest`,
`RenderBoundsTest`, the menu/keypad spec tests — is `invokeAndWait`-driven with no
pointer-exclusivity or popup-realization dependence.

Split `display` into `display` (deterministic, required, armed now) and `display-robot`
(Robot-driven, advisory until #91 retires the retries). The deterministic half needs *no*
stability record — it has no timing nondeterminism to record — so it can be armed today
in one PR with zero CI-history archaeology. And it is precisely the half the §Intended
Audience paragraph cares about: construction-time NPEs, layout crashes, EDT violations
before a student sees them. The Robot half's stability is #91's actual subject.

## Disregarding an acceptance criterion: P2 / DoD item 2

I am explicitly setting aside P2 and the "20-run audit recorded in
`test/jls/ui/package-info.java` or `docs/`" completion criterion. Two reasons.

**It is self-defeating as scoped here.** The chain-integrity comment states the principle
correctly — "a record accrued with retries still in place measures the retries" — and
then this issue's DoD demands the record while `rerunFailingTestsCount=2` (`pom.xml:292`)
is still in the tree and retirement belongs to #91, which is `blocked_by: [162]`. The
issue orders itself before the only change that would make its own measurement mean
anything.

**A prose table is the wrong artifact for this project.** Everything else JLS guarantees
is pinned by a test. A dated list of run IDs in a Javadoc comment is stale the day after
it is written, cannot be re-derived (Actions log retention expires), and no test fails
when it becomes false — it is exactly the kind of unverifiable claim ARCHITECTURE's
recorded-decisions discipline is meant to replace. Surefire already emits
`<flakyFailure>` elements whenever a rerun rescues a test. Counting them from
`target/surefire-reports/*.xml` and printing the count to `$GITHUB_STEP_SUMMARY` — beside
the existing JaCoCo summary step in `ci.yml` — makes flakiness a live number on every
run, and once it sits at zero the honest promotion is to drop `rerunFailingTestsCount` to
0 and let the build assert it. That is a permanent instrument instead of a one-time note,
and it hands #91 real data rather than a manual audit.

## Reframe 4: stop depending on apt at all

§10 anticipates that fail-closed apt may redden unrelated PRs during mirror outages, and
offers "fall back to a dedicated display leg" — which relocates the flake rather than
removing it. The project already has a better answer in-house. `ci.yml` pins
oss-cad-suite by SHA-256 for Windows precisely because unpinned package managers are
untrustworthy, and the repo publishes `ghcr.io/anadon/jls` and a `flake.nix`. Running the
Linux Build job in a pinned container (or provisioning xvfb + the HDL tools via the
existing flake) removes the apt dependency entirely, makes the substrate reproducible
rather than best-effort, and fixes #386's tool-availability problem in the same stroke.
This is the option most consonant with the README's reproducibility posture and the issue
never considers it.

## Alignment verdict

Strengthens the arc, wrong instrument. Arm the lane — and arm it inside the JVM where
this project arms everything else, with one capability mechanism serving all lanes, a tag
split that lets the deterministic half go required immediately, and a continuous flake
counter in place of a hand-typed 20-run table. Hand the stability record to #91 where the
retry retirement lives, and unblock #91 the moment the arming lands.
