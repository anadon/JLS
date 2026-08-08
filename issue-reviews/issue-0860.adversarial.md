# Issue #860: TASK-C581-3: a tagged release bumps the cask's version and sha256 without a human, so the cask cannot quietly point at a stale dmg
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

The third of three TASK-C581 tasks under FEAT-C34-3 (#581, "brew install --cask
jls"). #858 (TASK-C581-1) creates the cask and decides where it lives
(`homebrew/cask` vs. a project-owned tap). #859 (TASK-C581-2) makes the
cask's `caveats` text track the README's Gatekeeper paragraph. #860, this
issue, adds the release-workflow step that keeps the cask's `version`/`sha256`
current on every tagged release, "via a PR to `homebrew/cask` or a direct
commit to the project tap, whichever TASK-C581-1 decided."

## Findings, most severe first

**1. AC-1 ("no manual step beyond approval") and AC-3 ("red workflow, not a
stale-but-green listing") both silently assume the tap path, and neither
branch survives the `homebrew/cask` path they explicitly name.** Quoted:
*"via a PR to `homebrew/cask` or a direct commit to the project tap,
whichever TASK-C581-1 decided."* If #858 (still open, decision unmade —
its own AC-3 only requires the choice be "recorded in-tree with its
reasons") picks `homebrew/cask`: the release workflow can open a PR, but it
cannot merge it — `homebrew/cask` is a third-party repository with its own
CI, style linting, and human maintainers on their own schedule. "No manual
step beyond approval" then either quietly redefines "approval" to mean "a
stranger's review on a repo this project doesn't control" (which can take
days and can be rejected outright, contradicting "the loop is verified once
… so the failure mode … cannot happen silently" in the Outcome text), or the
task is only fully achievable under the tap path — which the issue never
says. AC-3 has the same problem in reverse: a workflow that successfully
*opens* the PR is green, yet the cask on `brew` stays stale until an
external human merges it — exactly the "stale-but-green listing" AC-3
claims not to allow, except the staleness now lives outside this repo's
workflow status entirely. **Recommendation:** either #860 should be
descoped to the tap path only (deferring the `homebrew/cask` PR-and-wait
workflow to a follow-up with its own acceptance criteria for the
review-latency window), or AC-1/AC-3 need separate, explicit criteria per
path.

**2. AC-4 depends on a "shared per-channel maintenance ledger" and a
"KC-34-1" 0.5 mw threshold that do not exist anywhere in this repository,
and the dependency is not declared.** `ordering_after` lists only
`["TASK-C581-1", "TASK-C581-2"]` (#858, #859). A repo-wide search for
`ledger`, `KC-34`, and `mw threshold` finds no committed ledger file or
KC-34-1 definition — only a structurally similar but unrelated "badge
ledger" mention in `docs/standards-adoption/OPEN-QUESTIONS.md:62,65`, and a
sibling task (#857, TASK-C580-3 for winget) that references "the same
ledger every channel reports to" without it existing either. Whichever task
actually creates that shared ledger and defines KC-34-1's threshold is a
real, unstated dependency of AC-4 that #860 does not order itself after.
**Recommendation:** name the issue that creates the shared channel ledger
and KC-34-1 threshold, and add it to `ordering_after`, or drop AC-4 from
this task until that infrastructure exists.

**3. Nothing in #858, #859, or #860 provisions the credential that actually
performs the update.** AC-5 asks that the update path be documented "well
enough that a second maintainer could re-establish it, including any tap
credentials or bot configuration it depends on" — but documenting a
credential is not the same as the credential existing. `release.yml`'s
current secret inventory (`RELEASE_GPG_KEY`, `RELEASE_GPG_PASSPHRASE`,
`SIGNPATH_API_TOKEN`, `SIGNPATH_ORGANIZATION_ID`) has nothing for a
Homebrew tap or a fork-and-PR bot against `homebrew/cask`, and #858's own
AC-1 only requires that "a cask definition exists," not that write access
to wherever it lives has been arranged. #860 can therefore reach its own
AC-1 ("demonstrated end to end once on a real release") only after
someone, in an undeclared side-channel, sets up a GitHub App/PAT with push
rights to a tap repo or fork rights against `homebrew/cask` — a
maintainer-side manual step this single-maintainer project (per
ARCHITECTURE.md's i18n non-goal rationale, "single-maintainer pedagogy
tool") has to do once, unaccounted for in any of the three tasks' cost
bands. **Recommendation:** make credential provisioning an explicit AC of
#858 or #860, not an implicit prerequisite discovered at implementation
time.

**4. AC-1's "demonstrated end to end once on a real release" has no
dry-run equivalent, unlike every comparable step already in
`release.yml`.** The existing release workflow deliberately gives every
publishing action a `workflow_dispatch` dry-run path that "exercises every
build step ... on any ref without touching a release" (`release.yml:12-16`,
`23-35`) — attestation, GitHub Release publish, Maven deploy, and
container push are all gated on `github.event_name == 'push'` so they can
be rehearsed safely. The cask-update step, by contrast, is only specified
as tested by doing it for real once. A first real exercise of a step that
pushes to an external, publicly-visible surface (a homebrew/cask PR, or a
commit to a public tap) with no prior rehearsal is exactly the kind of
live-fire test the rest of this workflow was designed to avoid.
**Recommendation:** add an AC requiring the update step to run in dry-run
mode (compute version/sha256, format the PR/commit, but not push) on
`workflow_dispatch`, mirroring the rest of `release.yml`.

**5. AC-2's "mismatch" is underspecified enough that a no-op check would
satisfy it.** "The sha256 is read from the release's published checksums
asset; a missing asset or mismatch fails the update" — mismatch *against
what*? If the implementation's only comparison is "does the checksums file
exist and contain a line for the dmg," that trivially never mismatches
(the checksum was generated by the same job that names the file,
`release.yml:635-649`), and the AC is satisfied by a script that never
independently re-hashes the downloaded dmg. **Recommendation:** AC-2
should require the update step to independently download the release dmg
and re-hash it, then compare against the value in
`SHA256SUMS-installers-macos-aarch64` — otherwise "mismatch" has no
scenario that can ever occur, and the check verifies nothing.

## What's solid

- The dependency ordering on #858 (cask must exist) and #859 (caveats must
  already be README-linked) before automating updates is the right
  sequence — updating a cask that doesn't exist, or automating drift into
  an as-yet-unverified caveats block, would be worse than doing this task
  first.
- AC-2's grounding fact is real: `release.yml:631-649` does generate a
  per-OS `SHA256SUMS-installers-<os>-<arch>` asset (macOS legs producing
  `SHA256SUMS-installers-macos-aarch64`), so "read the sha256 from the
  release's published checksums asset" points at an artifact that actually
  exists today, not a hoped-for one.
- The general principle in AC-3 — fail loud rather than silently stale —
  matches this project's established error-reporting philosophy
  (ARCHITECTURE.md's "Error-reporting contracts": structured `LoadError`s,
  no swallowed failures) even though, per finding 1, it doesn't survive
  the third-party-review case.
- The macOS-unsigned-by-choice framing this task inherits (README.md:37-43,
  #128/#135) is left alone here, correctly — #860 only touches
  version/sha256 automation, not the signing stance.

## Bottom line

The core mechanism — hash the published dmg, update a cask stanza, fail
loudly on mismatch — is sound in principle and the checksums asset it
depends on already exists. But the issue was written as if the
`homebrew/cask`-vs-tap fork in the road (still undecided in #858) doesn't
change what "no manual step" and "red workflow" mean, it leans on a
shared-ledger dependency (AC-4) that isn't declared or built anywhere in
this repo, and it skips both the credential-provisioning question and the
dry-run rehearsal every sibling release-workflow step gets. None of this
kills the task, but AC-1/AC-3/AC-4 need to be rewritten before an
implementer can build against them without guessing.
