# Issue #743: TASK-C544-4: a scripted Orca session builds and simulates a two-gate circuit by keyboard alone, in the Wayland CI rig, with the spoken output asserted
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

One sentence in the Outcome carries the whole task: *"a regression that silences an
announcement turns a lane red."* The end is a standing, falsifiable guarantee that the
words JLS produces for the two-gate lab do not quietly disappear. Orca, sway, JBR,
AT-SPI2 and speech-dispatcher are **transport**, not the thing being guaranteed.

That distinction is the whole review. #743 puts the guarantee at the far end of the
transport chain, and the transport is the part of the chain JLS does not own.

## The seam is cut in the wrong place

The chain the issue proposes to gate on has six links:

`Element/AccessibleContext` → **java-atk-wrapper (JAW)** → session D-Bus / AT-SPI2 →
Orca → speech-dispatcher → a capturable log.

JLS owns link 1. A JLS pull request can only regress link 1. Links 2–6 regress on
runner-image updates, distro package churn and upstream releases — and the project's
own in-tree research already measured how load-bearing that is:
`docs/standards-adoption/03-accessibility-conformance.md:164-173` records that JAW is
**not part of the JDK**, is "thinly maintained", and that a jlink'd bundled runtime
"has its own `conf/` and cannot pick up the distro's wrapper";
`docs/standards-adoption/OPEN-QUESTIONS.md:77` says outright *"Do not promise Orca
support before testing it."*

So the failure most likely to turn this lane red is not the failure the test exists to
catch. And the standing repair for a lane that reddens for substrate reasons is to
weaken its assertion — which is precisely the decay #411 exists to reverse
(`PIXEL_DIFF_MIN: "0"`, `JBR_SHA256: UNVERIFIED-PLACEHOLDER-…`). #743 would add a third
placeholder-in-waiting to the same lane.

## The project already decided this, and #743 does not cite the decision

`03-accessibility-conformance.md:549-556` considers exactly this test — an AT-SPI
assertion against a booted JLS — and concludes:

> **Recommendation: do not add this to CI.** … The `gui-wayland` lane's own history —
> twenty runs to earn promotion, a `UNVERIFIED-PLACEHOLDER` checksum still in
> `ci.yml`, and a `PIXEL_DIFF_MIN` still parked at `0` — is the honest cost estimate
> for a fragile GUI lane. A red-for-substrate-reasons a11y lane would erode the value
> of a green build.

The same document's "What only a human can do" section (`:588-607`) proposes
`docs/accessibility-at-checklist.md`, *"written in the style of the existing
`docs/wayland-desktop-checklist.md`"* — a scripted **once-per-release** Orca/NVDA/
VoiceOver spot-check whose log becomes the ACR's "Evaluation Methods Used" field.

#743 is the exact inverse of both recommendations and does not mention either. That is
the alignment failure: JLS's whole verification culture for things it does not own is
two-tier — automate the layer you own, spot-check the layer you don't. README:178-183
states it for Wayland desktops verbatim ("a headless software-rendered rig can diverge
from real GPU-backed compositors"). #743 collapses that into one gating lane.

## The rig-reuse claim understates the delta by an order of magnitude

AC-4 says the test "reuses the #101 rig's provisioning and exit-code contract".
Measured at HEAD: `wtype` appears **nowhere** in `scripts/` or
`.github/workflows/ci.yml`. `scripts/wayland-rig.sh` (366 lines) has never synthesized
a keystroke — its tool preflight is `for tool in sway swaymsg grim jq` (line 66); it
launches `HelloSwingControl`, then JLS, asserts a window in `swaymsg -t get_tree`,
screenshots with `grim`, and exits 0/1/2.

To satisfy AC-1 the rig must additionally acquire: synthetic keyboard input; a session
D-Bus; more GSettings schemas (that fight was already had once, #411 O-notes on
`ci.yml:401-403`); `libatk-wrapper-java-jni` plus
`assistive_technologies=org.GNOME.Accessibility.AtkWrapper` written into the **JBR's**
`conf/accessibility.properties` — the bundled-runtime problem `:167` names; Orca; and
speech-dispatcher configured to a capturable output module. Each needs a matching stub
in `scripts/wayland-rig-selftest.sh` (177 lines) or the exit-code contract stops being
self-guarded. That is a second rig, not a reuse, and it is the reason the 1.5–2 mw band
is not credible.

## The falsification guarantee is conditional in a way AC-4 does not fix

Per #411 O5, `ci.yml:421-427` sets `skip=true` and exits 0 when the JBR download fails:
an outage is a skip, deliberately, so the required gate is not wedged on JetBrains'
uptime. Correct for a boot lane — fatal for this one. On any day the CDN is down,
"a regression that silences an announcement turns a lane red" is false, and the build
is green. AC-4 asks the test to *name* #411; naming does not repair a guarantee whose
negation is indistinguishable from success. Only moving the load-bearing assertion
somewhere non-execution is impossible repairs it.

## The design I would build instead — three rungs, load-bearing one in surefire

I am explicitly disregarding AC-1 and AC-4's premise (the assertion lives in the
Wayland rig) and keeping AC-2 and AC-3 (content, not mere speech; falsification
recorded first), which are the good parts and are cheaper to honour elsewhere.

**Rung 1 — `LabNarrationGoldenTest`, headless, in the default surefire execution,
gating.** Define announcements as a pure function in a new `jls.a11y` package with no
Swing import (the boundary `HeadlessCoreRatchetTest` already knows how to enforce):
`announce(circuit, focus, event) → String`. Drive the two-gate lab through the existing
Layer-1 harness (`test/jls/ui/CircuitAssert`, per `test/jls/ui/package-info.java`) plus
`InteractiveSimulator`, and assert the **entire transcript** against a golden file.
This is the project's dominant idiom — `BatchSimulationGoldenTest`,
`VcdExportGoldenTest`, `SequentialGoldenTest`, `ElementSimulationGoldenTest` are the
oracles the normative docs already cite. A removed announcement turns this red in
milliseconds, on every platform JLS ships to, including riscv64 and the container.
It satisfies AC-2 and AC-3 *more strongly* than the Orca run, and it enforces #741's
AC-2 ("no second announcement channel") by construction, because the Swing accessible
layer becomes a renderer of this function rather than a parallel author of strings.

**Rung 2 — an AT-SPI bus probe, skip-when-absent, nightly, non-gating until it earns
promotion.** Assert JLS appears on the bus with the expected accessible names and
`CONTROLLER_FOR`/`CONTROLLED_BY` relations (`:415-427`), and that a state change emits
the corresponding AT-SPI event. Use `Assumptions.assumeTrue` — the shipped
`iverilog`/`ghdl`/`yosys` pattern (README:223-231), which is this project's existing
answer to "an external toolchain we do not control". Promote it to gating only on a
written green record, the way `gui-wayland` earned its 20/20.

**Rung 3 — `docs/accessibility-at-checklist.md`, per release, human.** The scripted
Orca session, alongside the NVDA rows #745 is already filing and VoiceOver, with AT and
OS versions recorded. This is where the actual screen reader belongs, and it answers
`:607`'s warning that an ACR written from unit tests with no screen-reader session is
the failure mode the whole item exists to avoid.

#547's VPAT then cites *which rung* covers each row. That is strictly more honest than
AC-4's "no test, no claim" collapsed onto one brittle lane, and it survives a JAW
regression without either lying or going red.

## The better goal, named plainly: narrate on the batch surface

#546 (FEAT-C26-4) already carries `area:batch` and already emits a part-to-whole prose
narrative headlessly. Rung 1's `announce()` is the *same function* evaluated over
simulation events instead of over structure — and `BatchSimulator` already accumulates
exactly the event stream it needs (`src/jls/sim/BatchSimulator.java:24-34`, watched
elements and `TraceSample`s, the machinery behind `-t` and `-vcd`).

Give it a CLI verb — `-narrate`, beside `-vcd`, `-i` and `-export` under
`docs/batch-interface.md`'s stability contract — and a blind student completes the lab
on **any** of the six shipped platforms with no GUI, no JBR, no Wayland, no JAW, no
Orca, including the container image autograders already use. That is a strictly
stronger claim than CAP-26 §1 step 2, it lands on the project's best-tested and
most-owned seam instead of its least, and no schematic simulator in the category has
it either — the category-defining claim survives the reframing intact.

If that lands first, #743's residual job is small (does the GUI path render the same
strings?) and its risk band collapses.

## Ordering consequence

#743 is `ordering_after: [TASK-C544-3]`, itself downstream of the #737 spike. Under
this reframing rung 1 comes **first**, ahead of #739 and #741: the golden transcript is
the specification those two implement against. It also makes #737's verdict cheap to
act on — if live announcement is unreachable through Swing, the golden loses its
state-change lines and the VPAT exception is a diff, not a redesign.

## Smaller note, if the Orca session survives anyway

`OrcaLabSessionTest` is a JUnit name for something that cannot run under surefire. The
repo's convention for rig-shaped work is a shell rig plus a `*-rig-selftest.sh` plus a
JUnit **ratchet** over the configuration (`WaylandRigPinRatchetTest`, per #411). Name it
that way, or the first reader will look for it in `mvn verify` and conclude the suite
covers something it does not.
