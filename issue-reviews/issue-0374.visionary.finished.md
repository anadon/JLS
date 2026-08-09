# Issue #374: TASK-0015: a wedged CI job costs minutes instead of the silent six-hour ceiling, and a new job cannot arrive unbounded
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of apparatus, the claim is: *CI has an implicit property nobody chose (360 minutes),
and JLS's house style is that implicit properties get made explicit and then enforced.* That
claim is correct and squarely on the project's arc. `docs/grand-architecture.md` §10 states the
creed outright — boundaries are "enforced, not aspirational … a compiler or CI obligation, the
only kind of architecture a single maintainer can hold" — and the repo lives it: eight
`*RatchetTest` classes, `HelpTopicsTest`'s completeness check, `ElementConstructorContractTest`,
five `scripts/*-selftest.sh` guards. An unbounded job is exactly the kind of unchosen default
this project has repeatedly converted into a written-down decision.

So the *goal* is endorsed without reservation. Everything below is about the seam.

## The reframing: eliminate the mistake instead of detecting it

The issue takes "23 hand-written job blocks" as a fact of nature and asks only "how do we police
23 hand-written numbers?" But 23 is not a fact of nature — it is duplication that the repo has
*already factored out one layer down and never propagated upward*:

- Five jobs shell out to the same recipe: `ci.yml:892, :896, :947, :951, :1021, :1087` plus
  `release.yml`'s `installers` — all `scripts/build-installer.sh`, the README's declared
  "single recipe used both locally and by CI".
- Four GUI lanes are thin wrappers over `scripts/{wayland,x11,macos}-rig.sh` and
  `windows-rig.ps1` (`ci.yml:453, :542, :669, :752`).

The workflow layer is glue around ~6 archetypes, written out 23 times. Every per-job property —
timeout, permissions, JDK setup, cache config — must therefore be copied 23 times and then
policed. This issue polices one such property and leaves the generator of the problem intact.

GitHub Actions gives exactly one DRY mechanism here: **reusable workflows (`workflow_call`)**.
YAML anchors are unsupported (confirmed: zero anchors in ci.yml, and Actions rejects them), and
composite actions cannot declare a job timeout — so `workflow_call` is not one option among
several, it is *the* option. A callee owns its own `timeout-minutes`; every caller inherits it.
Under that cut:

- The number of hand-derived timeout values drops from 23 to ~6, one per archetype.
- A new GUI lane or installer lane **cannot** arrive unbounded, because it does not declare a
  job at all — it declares a `uses:` line. The ratchet's entire reason for existing evaporates
  over the majority of the surface.
- `ci.yml` stops being 1,145 lines, which is itself a standing tax on #101, #91, #162, #265 and
  #111, all of which have to edit it.

Structural elimination beats detection-after-the-fact, and this repo already prefers that when
it can get it (`SaveTags.resolve` exists so tag text *cannot* reach `Class.forName`; `TellUser`
exists so a raw `JOptionPane` *cannot* be the easy path — the ratchet is the backstop, not the
mechanism).

**Sequencing consequence, and this is the load-bearing part.** #317 §3 pins "two byte-stable
check names" and TASK-0017/#265/#111 are about to register lanes as *required checks*. Moving a
job into a reusable workflow changes its check name (`caller / callee-job`). So the
deduplication is cheap **now** and expensive **forever after** branch protection is registered.
This issue sits at precisely the moment where that refactor is free — and doing only the timeout
sweep freezes 23 duplicated job blocks in place under branch protection days later. That is the
strongest argument in this review: the issue is correctly sequenced, and is spending its
sequencing position on the smaller of the two available wins.

## Second reframing: the enforcement does not belong in `mvn verify`

Every existing ratchet asserts a property of **the program** — `src/` text or `target/classes`
bytecode (`HeadlessCoreRatchetTest`, `NotificationRatchetTest`, `SocketConfinementRatchetTest`,
`DialogCoverageRatchetTest`). Those belong in the suite because they must hold for anyone
building JLS from any tree. `WorkflowTimeoutRatchetTest` would be the first ratchet whose
subject is not JLS but *this repository's GitHub configuration*. That is a category change the
issue does not notice, and it has a concrete cost: §7.11 mandates the scanner **fail, not skip**
when it finds nothing — so `mvn verify` goes red for anyone building from a tree without
`.github/`. JLS ships deb, rpm, AppImage, MSI, DMG and a Nix flake; source-tarball rebuilds are
an explicitly courted workflow (`docs/reproducibility.md`). Coupling the product's gate to CI
metadata is a real regression in that posture, and the issue's own anti-vacuity rule is what
forces it.

The repo already has the right home, and it is not JUnit: `scripts/icestick-handoff-selftest.sh`
is described in `ci.yml:47-55` as a "pure-shell guard … runs once on the required LTS leg (same
treatment the GUI lanes give their own `*-rig-selftest.sh` scripts)". A
`scripts/workflow-policy-check.sh` is the same idiom, and it dissolves the issue's central
self-declared risk:

- O6 ("no YAML parser on the classpath") and H2 ("a line matcher is sufficient") are artifacts of
  choosing Java as the host. `scripts/normalize-msi.py` and `normalize-dmg.py` already make
  Python a build-tooling dependency; `python3 -c "import yaml"` is a **real parser**, present on
  every hosted runner and in the devcontainer, and touches `pom.xml` not at all. §11's "central
  risk" and the whole `push:`/`pull_request:`/`schedule:` indentation hazard simply do not exist.
- The check generalizes for free to the two other workflow properties this repo maintains by
  hand discipline and does not enforce: **all 70 `uses:` are SHA-pinned** (70/70 today — nothing
  asserts it; Scorecard only reports) and every workflow declares top-level `permissions:`.
  One script retires three review habits instead of one.

## Third reframing: the title's promise and the formula contradict each other

The title promises "minutes". §7.10 specifies `τ(j) = ⌈2·max R(j)⌉`. For the installer and
emulated-aarch64 jobs that is an hour or two — not minutes. A job-level bound is a *cost ceiling*,
not a hang detector, and the issue's own H3 concedes it does not attribute anything.

What actually delivers attribution in minutes already exists in this repo, and the issue walks
past it. Every GUI rig carries named, documented, overridable budgets at the layer that knows
what it is waiting for — `scripts/wayland-rig.sh:57-59` (`SWAY_TIMEOUT=30`, `CONTROL_TIMEOUT=60`,
`WINDOW_TIMEOUT=90`), mirrored in `x11-rig.sh:90-94` and `macos-rig.sh:113-116` — and they `die`
with a message naming the thing that timed out (`"no wayland socket after ${SWAY_TIMEOUT}s"`),
under the exit-1/exit-2 JLS-side-vs-upstream classification, itself guarded by
`wayland-rig-selftest.sh`. **JLS already solved bounded-hang-with-attribution, in shell, at the
right layer.** The generalizable principle is: *bound the wait where the wait is understood*;
the GitHub job timeout is the outermost backstop, not the instrument.

That inverts the issue's emphasis. Step- and script-level budgets on the genuinely hang-prone
operations (the four fetches of O5, plus any new wait loop) are the primary deliverable; the
job-level number is the cheap backstop and does not need ten green runs to justify.

## What I would build instead (concrete)

1. **One commit, no ceremony:** `timeout-minutes` on all 23 jobs from four archetype ceilings —
   fast/lint ≈ 10, build/GUI ≈ 30, installer/emulated ≈ 90, release ≈ 120. Unify this with
   #317 Open Question 2: the required gate measures 141 s (#317 OQ2), so the fast-lane ceiling
   *is* budget `B`. The issue and its parent are currently deriving that same number twice, in
   two places, by two methods.
2. **Delete the 10-green-runs ritual.** §11 already predicts the figures rot and cannot be
   re-derived from the tree; §10 says never lower the key when it trips. A dated comment
   recording an unverifiable external observation is documentation debt with a decay date. An
   archetype ceiling is re-derivable by anyone, forever, and is *equally* honest given that
   hosted-runner variance is dominated by queueing and network. This explicitly disregards
   §5 P7 and the §14 criterion requiring the run-history table.
3. **Drop the expected-file-count tripwire (P4's `|W| = 6`).** §7.11 admits it fires on every
   legitimate workflow addition — a guaranteed future red build for a reason unrelated to
   timeouts, which Open Question 4 then tries to paper over with method naming. The repo's own
   answer to vacuity is better and already written down: `test/jls/ui/package-info.java` mandates
   "every helper assertion … itself pinned by at least one deliberately-failing test
   (assert-the-assertion)". Run the scanner over an embedded known-bad fixture and assert it
   reports the offender. That is anti-vacuity with no tripwire and no magic constant.
4. **Host the check in `scripts/workflow-policy-check.sh`** (python3 + PyYAML), invoked as a CI
   step alongside the existing `*-selftest.sh` guards, extended to SHA-pinning and top-level
   `permissions:`. Not in `mvn verify`.
5. **Then, before TASK-0017 registers required checks:** collapse the installer and GUI
   archetypes into `workflow_call` callees. File it as a sibling task now, while the check names
   are still free to move.

## Trajectory check

Aligned in intent, and it duplicates nothing. One tension worth naming: this project's recorded
decisions are notable for *declining* machinery until a trigger fires (i18n non-goal, IPC
plugin boundary reserved, second simulation strategy not built). The five lines of YAML that
unblock #265 and #111 are uncontroversial and could land today; a Java YAML line-matcher with an
anti-vacuity clause, a file-count tripwire, and five open questions is machinery, and it carries
all of this task's design risk while carrying none of its value. Splitting them lets the value
ship immediately and lets the enforcement mechanism be decided on its merits.

(Minor drift note for whoever picks this up: O4 claims four `continue-on-error` sites; HEAD has
eight job-level ones across `ci.yml`, `release.yml` and `repro-installers.yml`. Evidence pinned
to `2d0ca9d` has already begun to rot — which is itself the argument against §5 P7's dated,
externally-sourced comments.)
