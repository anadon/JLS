# Issue #724: TASK-C531-4: a seeded CLI-contract violation fails the build before any adapter test runs, and two full corpus runs are byte-identical end to end
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Two obligations, only loosely related, parked in the same task because both
land "in the fixture":

1. **Blame localization.** When the frozen CLI contract (#524) breaks, the
   build should say *which contract clause broke*, not emit four adapter
   failures whose common cause is one level up.
2. **A measurability precondition.** CAP-21's byte-identical parity claim
   (#502 AC-1) is meaningless if the same corpus run twice differs, so
   something must pin corpus-scale determinism.

Both are worth having. Neither is well served by the mechanism the issue
names — CI lane ordering plus a fixture-local double-run — and the project
already owns better machinery for each.

## The trajectory this lands in

- **JLS already has a determinism doctrine, and it is more rigorous than
  this one.** `docs/reproducibility.md` declares a reproducible artifact
  set (§1 table), names the perturbation axes, and the `reproducibility`
  job in `/home/user/JLS/.github/workflows/ci.yml` (lines 798–846)
  implements exactly the shape #724 AC-3/AC-4 describes: same-runner
  double build as a pre-filter (#44), then an *independent perturbed*
  rebuild across path / TZ / locale / umask (#185), with the diverged
  artifacts uploaded for `diffoscope` so the failure names the offending
  input. `installer-reproducibility{,-aarch64}` extends the same pattern
  to a second architecture.
- **JLS already has a recorded-artifact grading doctrine.**
  `examples/autograde/autograde.py` (#216) and its CI doc-test
  `test/jls/AutogradeBridgeExampleTest.java` grade by running JLS to
  completion and inspecting *finished* outputs — stdout report plus VCD —
  with live co-simulation explicitly rejected (#63). CAP-21's own AC-4
  (`RecordedArtifactOnlyTest`) restates this as a normative constraint on
  all four adapters.
- **The project's habit is to refuse speculative machinery until a demand
  gate opens** (i18n non-goal; #221 single sim strategy; #222 staged
  plugin trust; #212's demand gate). A permanent four-lane containerized
  CI apparatus with no named requesting instructor pulls against that
  habit — a capstone-level concern I raise but do not relitigate here.

## Reframing 1 (headline): grade the corpus once, replay it four times

CAP-21 AC-4 already forbids any adapter from driving a live JLS session:
every adapter is, by construction, a **pure projection of recorded batch
artifacts into a platform-native format**. If that is true, then running
the 300-submission corpus separately inside each adapter lane is not
required by anything — and #724's AC-3 doubles it, giving 4 × 300 × 2 =
2400 simulator runs per CI pass to prove a property of *one* producer.

The elegant cut:

- One **producer** stage runs the corpus once under the frozen contract and
  emits a canonical, normalized per-student score artifact plus the raw
  xUnit bytes. This is the only thing that ever executes JLS.
- Each adapter is a **projection** replayed against those recorded bytes,
  in-process or in a thin container, needing no simulator, no JDK matrix,
  and no corpus rerun.
- Parity (#719 / TASK-C531-2) then holds *by construction over a shared
  source*, and the test asserts each projection is total and lossless
  rather than empirically comparing four independent pipelines and hoping.

Under this cut, "two consecutive full corpus runs produce identical bytes
across container boundaries" collapses to "the single producer artifact is
deterministic" — which is #524's envelope determinism (TASK-C524-4)
evaluated at corpus scale, i.e. one new datapoint rather than a new
apparatus. Cross-container determinism risk (CAP-21 risk 4) shrinks to one
boundary instead of four. Wall-time (#502 AC-3) stops being a problem to
budget for.

## Reframing 2: make ordering a data dependency, not a scheduling policy

AC-2 as written — "an adapter lane cannot be scheduled ahead of the
conformance gate by reordering a workflow file" — is not achievable in the
medium it targets. In GitHub Actions, lane order *is* a `needs:` edge in a
workflow file; any assertion about it is a lint over YAML, one edit away
from being wrong, and silently absent when someone runs an adapter locally
or on a fork. Enforcing it "structurally" inside Actions is theater.

The seam that actually holds: **the conformance suite is what produces the
adapters' input.** Have the conformance run stamp the build it validated —
a contract-version + conformance digest, the natural extension of #524's
AC-5 ("the contract version is queryable from the CLI itself, so an adapter
can refuse an incompatible build with a named error") — and have every
adapter harness refuse to start against a build carrying no valid stamp,
reporting *the named clause*. This is the same trust shape the repo already
uses for releases (checksums + `gh attestation verify`,
`docs/reproducibility.md` §5).

Consequences:
- Adapters cannot run ahead of conformance because there is nothing for
  them to consume — ordering is a property of the artifact graph, not the
  scheduler, and it survives forks, local runs, and workflow refactors.
- The revision comment's real requirement ("the failure the build reports
  is the named conformance clause, not four adapter failures") is satisfied
  without any ordering machinery.
- It becomes cheaply testable: hand the adapter harness an unstamped or
  mismatched build and assert the named refusal. That is a JUnit test at
  fixture scale, not an assertion about CI logs — which resolves the
  awkwardness the revision comment created by taking the seeded violation
  away (#524 owns it) while still demanding this task prove ordering.

## Reframing 3: one determinism claim, not three

Right now three places would own determinism: the landed `reproducibility`
job, TASK-C524-4 (envelope), and this task (corpus). Three owners means
three vocabularies and three failure messages for one property.

Instead: add grading artifacts as rows in the `docs/reproducibility.md` §1
specified-artifact table, and reuse the §4 gate mechanics — double run as
pre-filter, *perturbed* run as the real test, diffoscope-style comparison
naming the first differing member. Note that AC-4 asks the failure to name
"the axis that moved (rerun, container, locale, host)" while AC-3 only ever
perturbs *rerun* and *container*; as written the diagnostic must guess at
axes the experiment never varied. Borrowing §4's perturbed-rebuild design
(TZ=Pacific/Kiritimati, LC_ALL=C, umask 077, different workspace path, plus
the existing aarch64 lane for host) makes axis attribution real rather than
rhetorical — and makes it one sentence of new prose in a document JLS
already maintains, instead of a new subsystem.

## Restated shape I would accept

- The producer stage emits a canonical score artifact; adapters replay it.
- Adapters refuse an unstamped build by named clause; asserted by test, not
  by log inspection or YAML lint.
- Corpus artifacts join the `docs/reproducibility.md` §1 table; the gate is
  the existing double-plus-perturbed pattern, one axis per run.
- Nightly-only for the containerized full-corpus leg, following ci.yml's
  existing schedule discipline (only `gui-wayland` runs on cron).

## Disregarded acceptance criteria, and why

I am explicitly setting aside AC-2's "structurally enforced lane ordering"
and AC-3's "two consecutive full corpus runs" as stated. The first names an
enforcement the medium cannot provide; the second buys the weakest form of
determinism at the highest CI cost, and duplicates a landed gate that
already does more. The *outcomes* behind them — a failure that names the
contract clause, and corpus bytes that are stable across environments — I
endorse without reservation. The verdict is endorse-with-reframing rather
than rethink because those outcomes are correct and load-bearing for
CAP-21; only the seams should move.
