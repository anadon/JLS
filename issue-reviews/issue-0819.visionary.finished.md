# Issue #819: TASK-C185-1: a stranger's `mvn artifact:compare` against a published release matches, and the transcript is the proof — or the release path differs from the CI path and we find out that way
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the acceptance criteria away and one sentence remains, and it is a good
one: *the CI path and the release path are two different code paths, and
nothing today proves they emit the same bytes.* The perturbed-rebuild gate
(`ci.yml:829-846`) proves CI against CI. The `.buildinfo` records what the
release job claims it used. Nobody has ever closed the loop from outside. The
falsification branch in the title — "or the release path differs from the CI
path and we find out that way" — is the whole value, and it is real.

I endorse that goal without reservation. I am disregarding AC-1, AC-3 and AC-5
as written, because the mechanism they mandate cannot deliver it and is in
internal contradiction. The reframing below reaches the same end more cheaply,
and reaches it every release instead of once.

## Why the mandated mechanism is the wrong seam

**1. `artifact:compare` does not compare the artifact the claim is about.**
`docs/reproducibility.md` §3.4 points the compare goal at
`-Dreference.repo=https://maven.pkg.github.com/anadon/JLS`. That jar is not the
release-asset jar. `release.yml`'s `maven-registry` job is a *separate job* with
its own `actions/checkout` and its own `mvn -B deploy -DskipTests
-Djacoco.skip=true -Dspotbugs.skip=true` (release.yml:148). The release job
built its jar with `mvn -B verify …:buildinfo` (release.yml:88). So a release
run produces at minimum three independently built jars — release job, registry
job, and the `container-image` job's `mvn -B -q package -DskipTests` — and
`SHA256SUMS`/`.buildinfo` record only the first. AC-3 says "the declared
artifact set of `docs/reproducibility.md` is the set compared"; §1 of that
document declares `jls-<version>.jar` the release asset. AC-1 says the compare
runs "against that release's published `.buildinfo`". Recipe 3.4 does neither:
it compares a stranger's rebuild against the *registry* jar and against a
`.buildinfo` the plugin regenerates locally. A green transcript would be
evidence about the least-used distribution channel; a red one would be
ambiguous about which of three build paths diverged.

**2. AC-5 contradicts AC-1.** AC-5 demands "no repository write access, no
maintainer secret, and no unpublished input … at any step." AC-1 demands the
recipe be run "exactly as `docs/reproducibility.md` states it." The document
itself states (§3.4 caveat, and README.md:99-101) that GitHub's Maven registry
requires an access token even for public downloads. The two criteria cannot
both be satisfied by recipe 3.4. This is not a doc defect to be "noted" — it is
the recipe being structurally unfit for the outsider claim.

**3. A pasted transcript is a decaying proof, and that pulls against the
project's arc.** Everything else in this repository's verification culture is a
standing mechanized invariant with a named oracle: the perturbed rebuild is a
required ruleset context; `wayland-rig-selftest.sh` guards the rig's own
verdict logic on every event; the installer legs double-build and diffoscope on
divergence; ARCHITECTURE.md's simulation-strategy decision binds any future
implementation to a *bit-for-bit differential oracle* rather than to a review.
#819 is the one place the project reverts to "a human ran a command once and
pasted the output into a comment." It proves one release, in one person's
environment, at one moment, and is never re-run. Under this project's own
standards that is the weakest available form of evidence for its strongest
claim.

**4. The reproducible-central escape hatch is probably closed.** §6 and #185's
Open Questions both hold out a reproducible-central submission as the real
external check. Worth verifying before anyone plans around it: that project
rebuilds artifacts **published to Maven Central**, and JLS publishes only to
GitHub Packages. If so, the entire "genuinely third-party rebuild" branch is
unavailable unless JLS starts publishing to Central — a decision the README
already leans against ("plain-download users should prefer the Releases page";
JLS is an application, not a library). Deciding that explicitly retires a
future-work item that has been sitting unowned across four status comments.

## The reframing: three moves, none of which waits on a release

**A. Cross-check the three release-run jars inside `release.yml`.** The
`maven-registry` and `container-image` jobs already `needs: release`, so the
published `SHA256SUMS` exists by the time they run. Have each of them, after
building its own jar, byte-compare it against the release job's checksum and
fail on mismatch. This is *exactly* the hypothesis #819 wants to falsify —
"does the release path produce the same bytes" — tested on the release path
itself, at every release, with no human, no token, and no waiting. If the
registry jar and the release jar ever diverge, this catches it; recipe 3.4
would have blamed the stranger.

**B. Make the CI gate build the release command line.** The gate builds
`mvn -B -DskipTests package`; the release builds `mvn -B verify
…artifact:buildinfo`. Different goal lists, different active plugins (jacoco,
spotbugs, surefire). Today they happen to agree; nothing asserts it. Adding one
reference build using the release's exact goal list — or reusing #44's
same-runner pre-filter with that command — converts an untested assumption into
a gate, again with no release needed. This is the single highest-value
pre-release detector available and it costs a few CI minutes.

**C. Mechanize the outsider simulation as a secretless `verify-release`
workflow.** A `release: published` + `workflow_dispatch` + monthly-cron job
with `permissions: {}` that: fetches the public release download URLs by plain
HTTP (no `gh auth`), parses `java.version`/`java.vendor`/`mvn.version` out of
the `.buildinfo`, provisions exactly that Temurin build, `git clone --branch
v<tag>` from the public URL into a fresh directory (never the workspace
checkout), rebuilds, and diffs SHA-512 against the `.buildinfo` entries. The
"stranger" property AC-5 is reaching for is *mechanically assertable* here —
empty permissions, public URLs only — in a way a human's word in a comment
never is, and it re-runs forever. Divergence files the bug automatically with
diffoscope output, satisfying AC-4's intent structurally.

Under this framing the transcript in a comment becomes a byproduct — the first
green run's log *is* the transcript, permalinked — and #185's P2 closes on
evidence that keeps holding rather than on evidence that expired the moment it
was pasted.

## Doc consequences (state them, don't soften anything)

- Demote §3.4 from "verification recipe" to an informative note for tooling
  that speaks `.buildinfo` natively, and say plainly it is **not** the
  conformance path because its reference is the registry jar, not the release
  asset. This is a scope-honesty correction, not a relaxation — it narrows the
  claim.
- Recipe 3.1/3.3 lead with `gh release download`, which needs `gh auth login`.
  For a credential-free outsider path give the plain `curl` of the public
  `releases/download/` URL first. Small, but it is the difference between AC-5
  being true and being nearly true.
- Resolve the reproducible-central item in §6 and #185's Open Questions one way
  or the other, with the Maven Central precondition named. An unowned
  future-work item that has survived four status comments is a decision that
  has been deferred, not made.

## Where this leaves #185 and #338

#185's comment thread has already reasoned itself to the right place — pass 2
records that "the substantive half already shipped" and that only the external
outcome remains, and the cluster-E note correctly rejects #338's subsumption
claim on the ground that #338 asks "can CI rebuild its own installer" while
#185 asks "can a stranger rebuild the published artifact." That distinction is
sound and this issue is its rightful home. My objection is not to the boundary;
it is that the chosen instrument points at the registry channel through a
credentialed door, when the claim is about the public release asset through no
door at all. Move A alone would let #185 close on stronger evidence than the
transcript at the very next tag, and moves B and C can land before it.
