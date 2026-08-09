# Issue #265: CI test parity across supported platforms: add a macOS headless test lane and promote the cross-platform suites to required checks
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the nine stages away and the want underneath is one sentence: *a macOS
user should not be the first to discover a macOS-specific defect.* That want is
legitimate and Stage 1 already paid for itself — the first live run found the
`VK_DELETE` glyph divergence in `HotkeysHelpAccuracyTest`.

The issue then answers that want with a specific and expensive instrument:
replicate `mvn verify` on every OS and make each replica a required check.
Nine stages here, nine mirrored in #111. That instrument is not the one this
project uses anywhere else, and three of its nine stages are disqualified by an
argument the issue itself already makes.

## The project's own answer to "does it work on macOS?" is better than this one

JLS has already invented, and uses consistently, a far cheaper way to verify
platform behaviour: **make the platform decision a pure function of an injected
`os.name` and test the whole OS matrix on one runner.**

- `src/jls/MenuAcceleratorPolicy.java` — `isMac(String osName)`, every
  accelerator derived from the injected value; `test/jls/MenuAcceleratorPolicyTest.java`
- `src/jls/edit/DeleteKeyPolicy.java` — javadoc says it outright: *"injected
  `os.name` value, so the whole matrix is unit-testable"*
- `src/jls/ToolkitPolicy.java`, `src/jls/LookAndFeelPolicy` (via
  `LookAndFeelPolicyTest`), `src/jls/edit/OptionMenuPolicy`,
  `KeyboardConstructionPolicy`
- `src/jls/edit/EditOp.java:105` — *"from `os.name` rather than from `Toolkit`"*
- `Circuit.save` (`src/jls/Circuit.java:1469`) wraps the writer in
  `canonicalNewlines` precisely so that "a circuit saved on Windows must
  byte-match the same circuit saved on Linux" — the line-ending taxonomy class
  the issue plans to discover by burn-in is already dead by construction.

Alongside that sits a second habit: static ratchets that make a whole defect
class unwritable — `HeadlessCoreRatchetTest`, `NotificationRatchetTest`,
`PointerApiRatchetTest`, `SocketConfinementRatchetTest`,
`NullMarkedRatchetTest`, `ExtensionPointCatalogTest`. Eight of them.

The `VK_DELETE` fix is exactly this idiom applied once more: the test now
derives key names from VK codes and never from `KeyEvent.getKeyText`, because
that method is platform-varying. Note what that fix did to the lane that found
it — **it retired its own detector.** After the fix, no macOS run will ever
catch that class in that file again. Under invariant 4 ("failures are fixed at
the source"), every genuine finding this program produces converts a
platform-dependent test into a platform-independent one. The lane's yield is a
strictly decreasing function of its own success, yet the plan proposes to pay
for it as a permanent required gate on every pull request, forever, on the
scarcest runner class GitHub offers.

## The issue disqualifies its own Stages 4, 6 and 7 and does not notice

§1's out-of-scope list rules out the Agda lane with a principle stated cleanly:

> *"verifies a platform-independent artifact; a per-OS re-run proves nothing new."*

That principle is correct, and it applies unchanged to three planned stages:

- **Stage 4 (HDL goldens on macOS).** `test/jls/hdl/IverilogCompileTest.java`
  reads `test/resources/hdl/*.v` — **committed golden files** — and compiles
  them with whatever `iverilog` is on PATH. It does not generate Verilog during
  the test. Running it on macOS therefore compiles byte-identical committed
  bytes with a Homebrew build of iverilog. That tests Homebrew's iverilog, not
  JLS. The DoD line "run log shows iverilog/ghdl/yosys invoked" is satisfiable
  and still proves nothing about JLS on macOS.
- **Stage 6 (macOS JaCoCo floor).** Coverage of platform-independent bytecode,
  measured a second time. It measures the same branches the Linux ratchet
  already gates, and — because the `*Policy` classes take `os.name` as a
  parameter — even the platform-branching code is fully covered on Linux today.
  Its only real effect is to import the zero-margin flake risk that `pom.xml`'s
  own #233 note warns about, onto a second OS.
- **Stage 7 (advisory JDK-26 macOS leg).** A JDK-26 regression in a
  platform-independent codebase surfaces on the Linux advisory leg, which
  already exists.

Three of nine stages are per-OS re-runs of platform-independent artifacts, by
the issue's own stated test.

## Where the arc actually points

README's centre of gravity is the deterministic, headless surface: batch mode,
the `-t` grammar as a *documented stability contract*, VCD for autograders, a
container image, a bit-for-bit reproducible jar with a `.buildinfo` recipe,
normative specs in `docs/`, and machine-checked Agda proofs. ARCHITECTURE.md's
quality vocabulary is spec + golden + ratchet + proof. This project buys
confidence by making properties unfalsifiable by construction, then pinning
them with a fast, cheap oracle.

#265 is the one program in the repo that buys confidence by brute repetition.
It also pulls directly against #317, which exists because the required gate must
stay short (measured at 141 s; no `timeout-minutes` on any of the 19 `ci.yml`
jobs) — and against #374, which #265's own REPLAN concedes *blocks every
promotion here*. A required macOS lane whose critical path includes a network
Homebrew install of three HDL simulators, a full JaCoCo run, and a WindowServer
rig whose capture path depends on TCC grants that may be permanently
unavailable on hosted runners, is the exact shape #317 is trying to prevent.
The "bills at zero" cost argument in §1 answers the money question and skips the
one that matters: macOS runners are the most concurrency-limited class on public
repos, so promotion moves the merge gate's tail latency onto them.

## Concrete alternative: portability by construction, macOS as a probe

1. **Name the seam and ratchet it.** Promote the scattered `*Policy` classes
   into an explicit portability seam (a `PlatformProfile` derived once from
   `os.name`, plus the existing policies), and add `PortabilityRatchetTest` in
   the mould of `HeadlessCoreRatchetTest`: no class outside the registered seam
   may reference the enumerated platform-varying APIs — `KeyEvent.getKeyText`,
   `getKeyModifiersText`, `Toolkit.getMenuShortcutKeyMaskEx`, `File.separator`
   and `System.lineSeparator()` in canonical output, `toLowerCase()` without a
   `Locale`, `/proc` paths outside the two probe tests. This catches the
   `VK_DELETE` class *before it is written*, on Linux, in milliseconds, forever
   — instead of after a 20-run burn-in on a required macOS lane. It is the same
   move `ExtensionPointCatalogTest` already makes for extension points.
2. **Invert the lanes' trigger.** `macos` and `macos-gui` carry
   `if: github.event_name != 'schedule'` — backwards for a discovery
   instrument. Move them to the nightly cron, where their wall clock is free and
   their flake blocks nobody. A discovery instrument belongs where discovery is
   cheap.
3. **Ship a divergence registry, not a required check.** The nightly lane's
   product is a diff against a checked-in list of known platform divergences;
   a new one opens an issue. This is the same rigor level the project already
   accepts for real-hardware Wayland (`docs/wayland-desktop-checklist.md`, a
   once-per-release scripted spot-check) — and it, not a required check, is what
   Stage 2's taxonomy note actually wants to be.
4. **If gating is non-negotiable, gate on a smoke lane.** A 2–3 minute macOS
   job: jar boots, one batch golden runs, one `.jls` round-trips, and the
   `*Policy` suite runs under a real macOS JVM to pin that the injected-`os.name`
   answers match the live ones. That is the entire genuinely macOS-dependent
   claim. Stages 4, 6 and 7 drop; Stage 8 becomes a one-line documented skip on
   two files, not a stage; Stage 9 stays a nightly rig.

I am explicitly disregarding the DoD's Stage 4, 6 and 7 boxes: they can each be
checked without moving the goal, and the issue's own Agda-lane argument is the
reason.

## Verdict

**rethink.** The goal stands and Stage 1 was worth doing. The instrument is
wrong: nine required lanes (eighteen with #111) to defend a platform surface
that amounts to accelerators, toolkit selection, key text, file paths and line
endings — four of which this codebase has *already* made platform-independent by
construction. Keep the lanes as nightly probes, spend the saved effort on the
portability seam and its ratchet, and the parity problem largely stops existing
rather than being permanently policed. As a tracker, #265's unique remaining
content is #671/#672/#673 after #406 and #386 absorbed Stages 2–6 — three lanes
of marginal value carrying a nine-comment coordination history. That ratio is
itself the strongest argument for the smaller design.
