# Issue #386: TASK-0051: the external-tool suites stop passing by not running, and the tools the board and parity work need are actually installed
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its four deliverables, the claim is one sentence: **a green build must be a
statement about what was verified, not about what was attempted.** That is the same claim
`HeadlessCoreRatchetTest`, `NotificationRatchetTest`, `SocketConfinementRatchetTest`,
`ExtensionPointCatalogTest` and `HelpTopicsTest`'s completeness leg already make elsewhere in
this tree — each converts "we intend X" into "the build fails without X". #359 §4.3 states the
invariant in the general form: *a skip is never silently a pass*. The issue is right, the
invariant is right, and making the required Linux install fatal is not even novel — `ci.yml:312`
(agda) and `:405` (sway rig) already install fail-closed on the same runner. HDL is the outlier,
not the pioneer. Confirming D8: a tool used as a **CI witness** never sits between a student and
the offline jar, so this costs nothing the project has promised away.

Where the issue pulls against the arc is in *shape*: it solves a project-wide problem for four
named suites, builds a second registry that will rot, annexes a sibling's evidence, and staples a
GUI feature onto a CI change. Four reframings follow; the first two I would treat as replacing
P4/P5 outright.

## Reframing 1 — arming is a project-wide seam, not an HDL one

The four HDL suites are not the skip surface. They are 4 of ~33:

```
test/jls/ui/*                    25 classes, assumeFalse(GraphicsEnvironment.isHeadless())
test/jls/AutogradeBridgeExampleTest.java   gated on python
test/jls/WaylandStartupCliTest.java        gated on OS + JBR
test/jls/{FileHandleRelease,BootListenerHygiene,UntrustedFileHardening}Test  gated on /proc
```

The display case is the same defect verbatim: `ci.yml` runs `xvfb-run ... -Djls.test.headless=false`
only `if command -v xvfb-run`, else plain `mvn -B verify`. If the apt line loses `xvfb`, the
`display-tests` surefire execution (`pom.xml:274-289`) runs headless and **25 UI classes
assumption-skip green**. TASK-0051's fatal install fixes the install; its arming assertion (P4)
lists six HDL binaries and covers none of this.

So P4 as specified is a hardcoded tool list in one test class — a second registry of external
dependencies, maintained by hand, guaranteed to drift the moment TASK-0100 adds `ngspice` or #63
adds a co-simulation tool. This repo has a better idiom and uses it everywhere: declare at the
use site, cross-check the catalog in both directions.

Concrete alternative:

- A `@RequiresCapability("yosys")` / `@RequiresCapability("display")` JUnit extension in `test/`
  that resolves through `ToolLocator` (or a headless probe) and does exactly one of two things:
  **skip** when arming is not demanded, **fail** when it is. Every existing `assumeTrue`/
  `assumeFalse` gate becomes one annotation; `ToolLocator` stays the single locator (#359 §4.2).
- A `CapabilityCatalogTest` cross-checking the annotation values against a catalog table in both
  directions — the exact contract `ExtensionPointCatalogTest` already enforces for extension
  points, and `HelpTopicsTest` for help topics.

The payoff is that the mechanism is built once and the fifth, sixth and seventh tool cost an
annotation, not a task. #359's §2 already worries that TASK-0100 will "build the comparator
discipline twice"; the same hazard applies one layer down to the arming discipline, and this is
where it is cheap to prevent.

## Reframing 2 — assert the run's report, not the workflow's YAML

P5 has a JUnit test parse `.github/workflows/ci.yml` and assert three jobs set an environment
variable. Nothing in `test/` reads a workflow file today, and the dependency runs the wrong way:
Java test code becomes a linter for CI configuration, pinned to job names and YAML shape, and it
still cannot tell you whether the suites *ran* — only that a string is present.

Invert it. Have the build **emit** what it verified:

- Surefire already writes per-class `tests`/`skipped` — the issue's own O2 greps them. Add one
  small step that renders a capability manifest (`capability: armed|absent, executed=N,
  skipped=M`) into `$GITHUB_STEP_SUMMARY` and as a job artifact.
- Check in one expectation file per lane (`ci/capabilities-linux-required.txt`) and diff the
  manifest against it. A lane that quietly stops arming a capability fails on a diff, with the
  diff being the error message.

This satisfies the absorbed comment's item 1 ("executed-versus-skipped legible without opening
surefire XML") as a by-product rather than as a bolt-on, covers display/python/agda alongside HDL,
gives #359's integration criterion 3 ("the skip count is reported") its artifact, and gives P7's
version lines a natural home in the same manifest. It is also the honest object: greenness is a
property of the run, not of the YAML text.

## Reframing 3 — the board chain already has a home, and it is not a new Java class

P6 proposes `test/jls/hdl/board/IcestickSynthesisSmokeTest.java` spawning three subprocesses,
with §7.9 spending a paragraph fencing `ProcessBuilder` out of `src/`. But the chain already
exists in shell — `scripts/icestick-handoff.sh`, with `icestick-handoff-selftest.sh` proving its
control flow against stubs, run on the required lane. The missing piece is not a Java harness; it
is **the same script run once with real tools instead of stubs**. One CI step, no third
`ProcessBuilder` site, no new confinement argument, and the stub self-test and the real run then
guard the same artifact.

Two corrections that fall out of looking at the tree:

1. "Run the chain over the shipped `.pcf` golden" has no synthesis input. `test/resources/hdl/board/`
   contains **only** `blinky_icestick.pcf`; there is no `blinky.v`. The design comes from
   `BoardFixtures.blinkyText()` through the real emitters. So the honest chain starts at the
   circuit and runs `-export design.v -board icestick -pins pins.txt`, which also exercises the
   CLI wiring the handoff doc documents. That is a strictly better test than synthesizing a
   committed netlist.
2. This is #264's evidence. #359 Open Question 3 assigns the iCEstick real-toolchain walk to
   TASK-0052, filed *under #264*, "not owned here" — and #386 quietly takes it. The overlap should
   be named and resolved before either lands, or the same bitstream gets produced twice under two
   owners.

## Reframing 4 — `verilator` should arrive with its consumer, and its consumer is the point

Installing a tool "as pre-positioning" with no test is the kind of thing P4 exists to prevent
becoming permanent, and Open Question 2 knows it. But the fix is not to defend the empty install —
it is that `verilator --lint-only -Wall` over the shipped `.v` goldens is a ~15-line test that is
**the only thing in this entire issue that moves toward #359's title**. `iverilog -g2005` accepts
width mismatches, inferred latches, undriven and unused signals; Verilator's lint rejects them.
Those are precisely the "valid-but-wrong output" class the owning feature is named for. If one
thing ships here beyond the arming, make it that; it is cheaper than the board chain and worth
more.

## Disregarding an acceptance criterion: the File → Export HDL menu item

I am explicitly setting aside the P3 / §14 GUI export rows. They do not belong in a CI-arming
task, for reasons that are structural rather than stylistic:

- Different audience, different reviewer. One PR would carry workflow policy, subprocess-spawning
  tests, and an EDT-threaded Swing action. If H3 refutes (the chain fails on the shipped golden —
  which §10 correctly refuses to paper over), the student-facing feature is held hostage to a
  toolchain finding.
- The stated contract is already wrong for the product. "Byte-identical to the CLI for the same
  circuit and suffix" omits `-board`/`-pins` (`JLSStart:783-786`, `:913`). A GUI export that cannot
  reach the constraint emitter ships a strictly weaker export than the CLI, and the board on-ramp
  — the very thing this issue is arming tools for — remains unreachable from the editor. The
  interesting design question is a small export dialog (language + optional board/bindings), and
  that question deserves its own issue.
- A new editor action is not one menu row in this codebase: ARCHITECTURE's editor discipline plus
  `MenuBarSpecTest`, `MenuAcceleratorPolicyTest`, `HotkeysHelpAccuracyTest` and `HelpTopicsTest`
  all have opinions, and none of them is a CI concern.

Refile it against #59/#162 as "HDL export reachable from the editor, board-aware", and let this
task be about arming.

## The uncomfortable observation

This issue hardens a **syntax** oracle. `IverilogCompileTest` compiles; `GhdlCompileTest`
analyzes; neither compares behavior — #359 says so in its own abstract, and says the behavioral
comparator is "the single criterion that distinguishes this feature from what already ships" and
carries **no task id at all**. Meanwhile the pieces of a real behavioral oracle are already in
tree: the `-t` vector grammar, `BatchSimulator`'s VCD export, `docs/vcd-interop.md`'s profile, and
`VcdExportGoldenTest`. Emitting a testbench that drives the exported module with the same vectors
and diffing settling-point values against JLS's own trace is the work that makes every armed leg
mean something.

So: arm the lanes (small, correct, do it), but do not let arming plus a board walk plus a GUI
action consume the budget that the comparator needs. The strongest version of this issue is
narrow enough to land in days.

## What I would keep unchanged

- Fatal install on the required LTS leg, soft on the advisory leg, with the reason in the workflow
  comment (§7.12 item 2). Correct, precedented, cheap.
- The contributor case (P8) as non-negotiable. Arming is a CI property, not a build requirement.
- Versions recorded rather than pinned (P7), with the trade-off written down.
- H3's refusal to weaken the test if the shipped golden will not place-and-route. That is the
  finding, and it is the most valuable possible outcome of this work.
- The absorbed comment's lockstep rule with #406: no lane becomes required while its install can
  still degrade silently.

## Reshaped scope, one line

Arm capabilities generally (annotation + catalog test), assert the emitted capability manifest
instead of the workflow YAML, run the real chain through the existing handoff script from the
blinky fixture under #264's ownership, land `verilator --lint-only` with its consumer, and refile
the GUI export separately.
