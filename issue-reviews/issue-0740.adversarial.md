# Issue #740: TASK-C560-1: a committed harness runs JLS, Digital and Logisim-Evolution on the same workloads on the same machine, each at its own recommended settings
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

A harness, committed and documented in-tree, runs JLS, Digital and Logisim-Evolution
over identical workloads on one machine, each competitor at its own recommended,
version-pinned settings with the recommendation's source cited, and records
not-applicable workloads honestly rather than degrading them. `ordering_after: [554]`.
The Boundary explicitly excludes the published table (that's #742, TASK-C560-2) and
prose (CAP-36) — this issue is the harness and its fairness, nothing else. #740 is
narrower than its sibling/predecessor #560 (FEAT-C28-4), which already carries its own
adversarial review in this tree (`issue-reviews/issue-0560.adversarial.md`); several
findings below are #740-specific instances of the same underlying gaps, verified fresh
against this checkout and against #742.

## Findings, most severe first

**1. Feasibility is unestablished: nothing shows Digital or Logisim-Evolution expose a
scriptable, GUI-free throughput mode comparable to JLS's `-b -t`, and this project's own
tooling is deliberately X11/GUI-averse.**
README.md:216-217 states flatly: "X11 is deliberately not part of this project's
tooling: no X server, no XWayland, no X11 utilities," and the dev container
(README.md:273-275) ships "no X11 components." JLS's own headless batch mode
(`docs/batch-interface.md`, `-b -t`) is what makes "same workload, same machine"
tractable for JLS's side at all — it is a CLI, no display needed. The issue's Outcome
promises the harness "drives JLS, Digital and Logisim-Evolution over the same
workloads on the same host," but nothing in the issue establishes that either
competitor has a documented, scriptable batch-throughput mode; if the fastest honest
path for either is "drive the GUI and time it," the harness either cannot run inside
this project's own X11-averse CI/dev environment, or the comparison silently becomes
JLS's optimized headless loop timed against a competitor's GUI-interactive loop — which
is exactly the strawman the issue's own closing line ("a comparison whose competitor
configuration cannot be defended is worth less than no comparison") forbids.
*Recommendation:* add an AC requiring the implementer to record, before building
anything else, how each competitor is invoked headlessly (or document that GUI timing
was unavoidable and defend why it's still fair); note any X11 dependency the harness
introduces as a deviation from the recorded no-X11 tooling posture.

**2. The acceptance criteria never require the workload set to include the CPU-scale
fixture that the sibling issue's whole outcome depends on — a real cross-issue
contradiction, not a hypothetical one.**
#742 (TASK-C560-2, already filed and open) states its outcome as "Digital's 120 kHz
[simulated processor clock] claim gets a measurement rather than a counter-claim" — that
number is meaningless without a CPU-scale workload in the shared workload set #740
produces. But #740's own ACs only say "the same workloads on the same machine" (AC-2)
and "workloads that a competitor genuinely cannot express are recorded as
not-applicable" — nothing requires a CPU-scale fixture to be *among* the workloads
chosen. An implementer could satisfy every checkbox in #740 using three small,
JLS-favorable circuits, hand a technically-compliant harness to #742, and leave #742
unable to deliver the one comparison the whole capstone (#512 CAP-28, evidence line:
"Digital teardown... published 120 kHz processor claim") exists to answer.
Compounding this: the only CPU-scale fixture in the tree today, `riscv/build/k2000.jls`,
is untracked and generated only by `riscv/bench_kernel.py` (verified live in this
checkout — see finding 3), so even if an implementer wanted to include it, it does not
yet exist as a committed fixture #740 could point at.
*Recommendation:* add an AC requiring the workload set to include a CPU-scale fixture
comparable in kind to the one behind Digital's published clock-rate claim, and make
that requirement visible in #740 rather than left implicit in #742.

**3. The dependency chain is deeper and further from landing than `ordering_after: [554]`
suggests, and this is independently verifiable in the current tree.**
`ordering_after: [554]` names only the immediate predecessor. #554 (FEAT-C28-1) itself
carries `ordering_after: [#413 TASK-0025]`, and #413 is a large, still-open task whose
entire point is committing a tracked CPU-scale calibration fixture and deleting
`riscv/` — verified still pending in this checkout:
```
$ ls riscv/          # still present: bench_kernel.py, build_cpu.py, jlsbuild.py, ...
$ git ls-files --error-unmatch riscv/build/k2000.jls
error: pathspec 'riscv/build/k2000.jls' did not match any file(s) known to git
```
So #740 is transitively gated behind at least two substantial, currently-open pieces of
work (#413 → #554) that its own `ordering_after` field does not surface. Anyone picking
up #740 by reading only #740 will not discover that the CPU-scale fixture it may need
(finding 2) does not yet exist in committed form.
*Recommendation:* note the transitive dependency on #413 explicitly in #740's boundary
notes, mirroring the same gap already flagged against #560 (issue-0560.adversarial.md
finding 6).

**4. "documented... so a third party can re-run the whole head-to-head unaided,"
"recommended settings," and "no strawman setups" are unfalsifiable by inspection.**
Nothing in #740 defines who adjudicates whether a chosen competitor configuration is
genuinely that competitor's own recommendation if a Digital or Logisim-Evolution
maintainer later disputes it, nor does the issue require citing the competitor's own
docs/README for the claimed "recommended settings" (contrast the citation discipline
#520 CAP-36 imposes on competitor claims elsewhere in this same programme, per the
#560 review's finding 5). Likewise "unaided" third-party re-run has no independent
verification step (e.g., someone other than the author actually running it in a clean
environment) and no tolerance is specified for what counts as a successful
reproduction — a harness that runs cleanly on a different machine but produces
different per-workload winners would technically satisfy the letter of the ACs.
*Recommendation:* require a link to each competitor's own documentation establishing
the settings used as "recommended," and require the harness output to include both
absolute numbers and cross-tool ratios (more likely to transfer across hardware than
absolute wall-clock).

**5. Platform/toolchain compatibility on "the same machine" is unaddressed.**
JLS pins `maven.compiler.release` to 25 (`pom.xml:43`) and the README documents a
narrow, carefully-maintained JDK/toolkit compatibility matrix (X11 vs Wayland vs
headless, JDK 25+ throughout, README.md:172-177). Digital and Logisim-Evolution are
independently-versioned third-party Java desktop applications with their own JDK
requirements and windowing assumptions, not audited here. "Same machine" for a
same-host, same-JDK-process style comparison may require running three different
runtimes' worth of assumptions on one box, and the issue is silent on whether that is
even mechanically compatible (e.g., if a competitor pins an older JDK that conflicts
with the harness's own tooling) — a real feasibility question this issue should record
an answer to, not leave for the implementer to discover mid-task.
*Recommendation:* add a note (even a stub) on how JDK/runtime coexistence across three
independently-versioned Java GUI apps on one machine is handled.

**6. `band_mw: 0.5-1` looks tight against the actual burden this issue carries.**
Sibling #554 (JLS-only benchmark suite, zero third-party tooling) is separately
estimated at "1-2" mw. #740 additionally requires standing up, correctly configuring,
and *fairly* operating two entire third-party GPL applications with unaudited
headless/batch capabilities (finding 1), resolving what "recommended settings" means
for each with a defensible source (finding 4), and documenting version pinning and
hardware — for an estimate at or below the JLS-only suite's. This estimate is
inherited from #512 CAP-28's PF-4 line rather than independently re-derived here, so
it's worth flagging as imported optimism rather than a #740-specific error, but #740 is
the issue that will actually absorb the overrun if it materializes.

## What's solid

- The Boundary section's producer/consumer split against #742 (harness vs. published
  table) and CAP-36 (prose write-ups) is clean, correctly cross-references real, open,
  correctly-scoped issues (#742 verified open and titled consistently), and avoids the
  scope creep a less disciplined task-issue would invite.
- Requiring not-applicable workloads to be recorded "with the reason, rather than run
  in a degraded form that flatters JLS" is a genuinely strong anti-gaming clause — it
  closes the most obvious way a self-interested implementer could rig the comparison.
- Routing external tool comparison through a subprocess/same-machine harness rather
  than any in-process integration is consistent with this project's own recorded
  architecture decision (`ARCHITECTURE.md`, "Plugin trust boundary," #222) that
  external tool integrations (Yosys, GHDL/Icarus, ELK) stay on a subprocess boundary —
  no novel trust or licensing exposure is introduced by this issue's approach.
- Pinning each competitor to "current release, version pinned" is the right baseline
  discipline for reproducibility, even though (per finding 4) it lacks a verification
  mechanism.
