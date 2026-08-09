# Issue #406: TASK-0017: a test that fails only on macOS or Windows blocks the merge, with the display and HDL suites armed and the coverage ratchet evaluated
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the four clauses away and the goal is one sentence: **a defect that only
manifests on a student's Windows lab machine or a TA's MacBook must not reach a
release.** That goal is one of the few CI investments in this repository that
buys direct end-user value. JLS ships `.msi`, `.dmg`, `.deb`, AppImage and a
container image (README "Installing JLS"); its product is a save format whose
bytes matter, a batch grading contract, and an editor students drive by mouse.
`ci.yml:120-126` names three Windows-only defects that *already shipped* —
CRLF round-trip, held file handles blocking `@TempDir`, goldens rewritten by
`autocrlf`. Those are real, and none of them is caught by any Linux lane.

So the goal is endorsed without reservation. What I want to redirect is the
*instrument*. Three of the four clauses are proxies for things that can be
measured directly, and the fourth is being landed in the same change as the
promotion it is supposed to be measured against.

## Reframing 1 — coverage is not a platform property; delete clause 4

`-Djacoco.skip=true` on the platform lanes is treated as an omission to be
fixed (P2, O4, H2, Open Question 2, §8 step 3, and roughly a third of the
issue's word count). It is not an omission. It is a correct architectural
boundary that the repository has *already articulated in the neighbouring
case*: the carry-forward comment records "#111 invariant 2 is binding: the
Linux `build` job remains the sole gating authority for SpotBugs." Coverage
deserves the same rule for the same reason. A percentage floor measures a
property of the *test suite*, not of the platform; running it on three
substrates measures the same suite three times and buys three independent
chances of a false red for reasons unrelated to the change — which is the #233
incident (`CONTRIBUTING.md:83-89`) replayed on two more axes, on lanes where
several suites structurally self-skip (Agda proofs are Linux-only by design,
`ci.yml:253`; the HDL legs depend on a network install). `CONTRIBUTING.md:90-99`
already forbids *setting* floors from non-canonical runs; the issue's P8 exists
solely to restate that prohibition, and §7.10 has to prove the inequality has
"slack in the favourable direction" — three paragraphs of reasoning to defend
a gate whose only defensible outcome is "it passes."

**Drop P2, Open Question 2, §8 step 3 and the epsilon-widening study entirely.
Keep `-Djacoco.skip=true` on both platform lanes and write the invariant down
instead: Linux JDK-25 headless is the sole gating authority for coverage, as it
already is for SpotBugs and the proofs.** This is the reframing that makes a
problem disappear rather than solving it: it deletes an open question that
"blocks execution", a three-platform measurement study, a package × platform
table, and a whole class of future flakes, at the cost of nothing the issue can
name.

## Reframing 2 — what clause 4 was actually reaching for is vacuity, and that
is an integer, not a percentage

The honest fear behind "enable the ratchet" is *a green lane that ran nothing*.
The carry-forward comment reaches the same conclusion from the other side
(item 5: "a run that executed zero display-tagged tests must fail rather than
pass") but still leaves the JaCoCo clause standing beside it. Coverage is a
terrible vacuity detector — noisy enough to need an epsilon study — and an
executed-test count is a perfect one. Every skip in this codebase is a JUnit
`Assumptions` call at a known site: `Assumptions.assumeFalse(GraphicsEnvironment
.isHeadless(), …)` in the 25 display classes, `assumeTrue(ToolLocator
.findOnPath("iverilog") != null, …)` in `test/jls/hdl/IverilogCompileTest.java:33`.

Concretely: a `-Djls.test.require=display,hdl` property that turns those
assumptions into failures, plus a surefire-report assertion that the
display-tagged execution ran ≥ 25 classes with 0 skips. Exact integers, no
calibration, no epsilon, and the failure message names the tool or the tag
rather than a bundle percentage. That single mechanism subsumes clause 4,
clause 2 (below), and the "manual verification recorded in the PR" of §9 —
which today asks a human to eyeball a skip count on every promotion.

## Reframing 3 — fail-closed on the *install step* puts YosysHQ and Homebrew
on JLS's merge path

P9 asks that a failed `brew install` or a failed oss-cad-suite download turn
the whole required lane red. Consider what that means for a single-maintainer
project: a Homebrew formula rename (which `ci.yml:281-283` already flags as
plausible), a YosysHQ release-asset move, or a GitHub CDN blip means *nobody
can merge anything to JLS* — including the fix. The asset is not cached:
`actions/cache` appears nowhere in `ci.yml`, so every Windows run re-downloads
a large release archive over the public internet, on the critical path of a
gate that is about to block merges. §7.11 treats this as obviously correct and
§11 notices only that a drifted formula name "will surface on the first run."

The requirement is "the HDL goldens must not silently skip." Two moves satisfy
it without handing a third party a veto over the merge queue:

1. **Cache the toolchain.** `OSS_CAD_SHA256` (`ci.yml:173`) is a perfect,
   already-present cache key. A pinned, checksum-keyed `actions/cache` entry
   takes the network off the critical path for every run after the first, and
   the pin bump becomes the only network event.
2. **Move fail-closed from the step to the test** (Reframing 2's
   `-Djls.test.require=hdl`). Then a missing `iverilog` fails
   `IverilogCompileTest` by name — attributable, retryable, and legible in the
   check output — instead of failing an opaque shell step whose log a
   contributor has to open to understand why their unrelated PR is red.

That is strictly more fail-closed than P9 (it also catches a toolchain that
installs but is not on `PATH`, which P9 misses) and strictly less fragile.

## Reframing 4 — promote the headless suite now; leave the display suites
advisory. I am explicitly disregarding P3-as-a-blocking-criterion.

Every historical platform defect the issue can name — CRLF round-trip, held
file handles, `autocrlf` goldens, macOS case-insensitivity, path separators —
is a **headless, model-level** bug. Not one needs a window server. Meanwhile
the display corpus is 25 classes including `Robot`-driven flows
(`test/jls/ui/PaletteDropTest.java`, `EditorKeyboardConstructionTest.java`,
`EditorGestureSupport.java`) carrying, in `ci.yml:129-134`'s own words,
"Xvfb/WM-less timing and pointer-exclusivity workarounds." And the repository
already has hard evidence about the macOS substrate: `ci.yml:612-635` records
that on run 30322375242 the macOS GUI rig's capture came back with zero unique
colors because TCC withholds Screen Recording from the non-interactive CI
session, and that hosted runners "may NEVER grant it." §7.2 asserts "macOS
runners have a real window server" as a settled platform assumption; the
adjacent job in the same file documents that the session's *permissions* are
the actual constraint, and that `Robot`-class posting is exactly what TCC
gates.

So the risk/benefit split is lopsided in a way the issue never separates:
**the headless half is high-value and near-zero-flake; the display half is
low-marginal-value (Linux/xvfb already gates it, #162) and the highest-flake
thing in the repository.** Bundling them means the entire promotion — including
the CRLF gate that would have caught three shipped bugs — waits on 25 Robot
tests surviving twenty consecutive runs on two substrates neither has ever run
on required.

Reframed acceptance: promote `mvn -B verify` (headless, `-Djacoco.skip=true`)
on both platforms to blocking as soon as #374 lands. Keep the display suites
running with `-Djls.test.headless=false` on both lanes but in a **separate,
still-advisory step or job**, with the vacuity counter of Reframing 2 so a
silent self-skip is loud. Promote each display substrate on its own record,
after #91 has retired `rerunFailingTestsCount = 2` (`pom.xml:293`) — because
H3 is correct that the retries make any flake rate measured now a lower bound,
and promoting against a knowingly-optimistic number is the one thing O5's
procedure exists to prevent.

## The larger arc — this is the seventh hand-rolled promotion ritual

The deepest observation is structural and is not about #406's content at all.
Grep `ci.yml` for the promotion protocol and it appears seven times, in prose,
one paragraph per job: `gui-wayland` (executed, `:328-352`),
`installer-reproducibility-aarch64` (executed, `:917+`), `gui-x11` (`:574-576`),
`macos-gui` (`:703-706`), `windows-gui` (`:995-997`), `windows-installer-msi`,
`macos-installer-reproducibility`, plus the two lanes here. Each restates
"required once 20 consecutive runs show at most one failure", each promises a
hand-transcribed list of run ids in a comment, each ends with "the maintainer
then registers the byte-stable name as a required branch-protection check."
`#406` proposes to do this a seventh and eighth time by hand, and to add
`PlatformLaneRatchetTest` — a regex scraper over `ci.yml` (whose own §11 admits
it "should parse YAML rather than scan lines") that hard-codes two job names
and four clauses and will be obsolete the day it goes green.

The project has a mechanism-shaped hole and keeps filling it with prose. The
elegant route, and the one that makes #406 cheap instead of expensive:

- **One declarative in-tree policy file** — `.github/lane-policy.yml` — listing
  every job, its maturity (`advisory` | `required`), and its promotion record.
  The workflow's `continue-on-error` is then a fact *derived from* the policy
  file, not duplicated in prose beside it.
- **One generic `LanePolicyTest`** replacing `PlatformLaneRatchetTest`: parse
  `ci.yml` as YAML, assert every job's `continue-on-error` matches its declared
  maturity, and assert every `required` job carries a record with ≥ 20
  enumerated run ids and a cause for each non-green. That is one test for all
  eight lanes, and it never needs editing again — where the proposed test needs
  a new method per clause per platform.
- **One `scripts/promotion-record.sh`** that queries the Actions API for the
  last N runs of a check name and emits the record block. §9 says "there is no
  local substitute; a promotion asserted rather than observed is exactly what
  O5's procedure exists to prevent" — correct, and the answer is to *generate*
  the observation rather than to trust a human transcription of twenty
  ten-digit run ids.
- **One drift check** comparing `gh api .../branch_protection` against the
  policy file's `required` set. This is the piece §7.1 and §7.7 concede lives
  outside the repository and "this task does not pretend to" cover — which
  means the actual gate, today, is an unverified maintainer action recorded in
  a closing comment. That is the weakest link in the whole design and no amount
  of `ci.yml` assertion touches it.

Under that mechanism, #406 shrinks to: land the cache and the
`-Djls.test.require` property, flip two entries in the policy file, run the
record generator. The eight C-series duplicates (#661-#673) collapse too,
because the thing they each re-derive is the ritual, not the platform.

## Where this sits against the whole trajectory

Aligned: cross-platform determinism is load-bearing for a project whose README
promises byte-reproducible jars, `-text`-pinned circuit goldens
(`.gitattributes`), and a documented batch contract used by autograders. The
`*RatchetTest` family and ARCHITECTURE.md's "Recorded decisions" section show a
project that genuinely prefers mechanized invariants to prose — which is
precisely why filling the promotion hole with an eighth prose paragraph pulls
against its own instincts.

Pulling against: doubling required runner minutes on every PR (§11) for a
single-maintainer educational tool, in service of a coverage gate that should
not exist and a display gate that duplicates #162's substrate. The reframed
version buys the entire user-visible benefit — Windows and macOS regressions
block the merge — at a fraction of the runner cost, with no epsilon study, no
network dependency on the merge path, and no new hand-maintained ledger.

## If only one thing changes

Split the issue at the seam it already contains but does not honour: **arming
is free while the lanes are advisory; promotion is the risky flip.** Land the
cache, the require-property, the macOS display arming and the vacuity counter
*today*, with `continue-on-error` untouched — zero merge risk, immediate signal,
and the burn-in then measures the lane that will actually gate. Then flip the
flag once #374 has landed. The issue's own carry-forward item 10 reaches half
of this ("arm first, then measure"); the other half is that the arming needs no
20-run record at all, and holding it hostage to one is the single largest
avoidable delay in the plan.
