# Issue #185: Reproducible Builds conformance: independent-rebuild verification, published .buildinfo, and a declared reproducible-artifact scope
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the four work-streams away and one claim remains: *a stranger, holding
nothing but the tag and the published assets, can prove the bytes we shipped
are the bytes the source produces.* Everything else — the perturbed rebuild,
the `.buildinfo`, the scope table — is instrumentation for that claim. The
issue's own comments say as much (comment 5181650898: "#338 asks whether CI
can rebuild its own installer bit-for-bit; this issue asks whether a stranger,
from source, can rebuild the published artifact"). That framing is correct and
it is the right thing for JLS to want.

It also aligns with the project's arc. JLS is an educational tool that runs on
university lab machines and inside autograders (`ghcr.io/anadon/jls`,
`docs/batch-interface.md` as a stability contract). Its distinguishing quality
is not features — it is that a department can adopt it without a security
review, because the README says exactly what each guarantee covers and
refuses to overclaim (the `msi`/`dmg` honesty paragraph, `#128`/`#135` on
unsigned macOS builds, `#136` on the absent GPG key). #185 is the load-bearing
piece of that posture for the artifact people actually download. Endorse the
goal without reservation.

## The problem: the last criterion is the weakest instrument in the whole feature

Everything that landed here is a *gate*. The perturbed rebuild
(`ci.yml:829-846`), the same-runner pre-filter, the installer double-builds,
the wayland-rig selftest, `HeadlessCoreRatchetTest` — the project's settled
idiom is that a property nobody can regress silently is a property CI enforces.
The single remaining criterion, P2, breaks that idiom: it asks a human to run
`mvn artifact:compare` once, by hand, and paste the output into a comment.

Three things follow, and I think they are decisive:

1. **It is self-attestation dressed as third-party verification.** The
   maintainer running compare against their own release is exactly the trust
   relationship the abstract says verification should stop depending on.
2. **It decays instantly.** A transcript for release N+1 says nothing about
   N+2. The gap it is designed to detect — release path diverging from CI path
   — reopens the moment either workflow is edited.
3. **It has not been runnable for 24 days and nobody owns the unblock.** v5.0.4
   is 2026-07-16; the pom sits at `5.0.5-SNAPSHOT`; the precondition is a
   release event with no scheduled owner. Comment 5176161103 flags this exactly
   ("an unowned precondition — worth a maintainer decision"), and the answer
   has been four dedup passes and eight status comments defending a boundary
   around a task that cannot run. That is real cost: the issue now generates
   more coordination than work.

## Reframing 1 — the blocker is an illusion; measure the thing today

P2's actual hypothesis is *release path ≡ CI path*. That is measurable now.

The two paths genuinely differ. The CI gate builds with
`mvn -B -DskipTests package` (`ci.yml`, "Build reference jar and BOM").
`release.yml:82-90` builds with
`mvn -B verify org.apache.maven.plugins:maven-artifact-plugin:3.6.0:buildinfo`
— full test run, jacoco agent, SpotBugs, enforcer, plus a trailing goal. Both
the jar and `bom.json` are produced at `package` (cyclonedx binds there,
`pom.xml:593`), so I expect they match; but *expecting* is precisely the state
P2 exists to end. Worse, `docs/reproducibility.md` §3.3 hands third parties the
**CI-path** command and tells them its output must equal the **release-path**
artifact. That equivalence is shipped, documented, relied upon, and unverified.

The fix costs one CI step and no release: add a third leg to the
`Reproducible build check (Linux)` job that rebuilds with the release
command line verbatim and byte-compares against the reference. Alternatively —
and this is already built — `release.yml` has a `workflow_dispatch` dry-run
that "exercises every build step … nothing is published." It produces a real
`.buildinfo` on any ref, today. Comparing a dry-run's jar+`.buildinfo` against
a CI-path rebuild yields essentially all of P2's evidentiary content while
v5.0.4 is still the newest tag.

Strictly better than either: delete the divergence instead of testing it. Hoist
the build invocation into one place — `scripts/build-release.sh`, in the idiom
already established by `scripts/build-installer.sh` ("the single recipe used
both locally and by CI") — and have ci.yml, release.yml, and §3.3 all cite it.
Then "release path ≡ CI path" is true by construction and the criterion
evaporates.

## Reframing 2 — make conformance standing, not attested

Replace "record a transcript" with a `release-verify` job that runs after a tag
publishes: fresh runner, `gh release download`, install the JDK the published
`.buildinfo` names, `git clone --branch v<tag>`, rebuild, compare, fail loudly
on mismatch. That is the same work the maintainer would do by hand, executed
every release forever, and its log *is* the transcript. It also converts
`docs/reproducibility.md` §3.3 from a promise into an executed recipe — the
recipe becomes the test of itself, which is the strongest form the project's
own documentation-honesty norm can take.

## Reframing 3 — the one that actually gets a stranger involved (the redirect worth having)

`docs/reproducibility.md` §6 promises a submission to
[reproducible-central](https://github.com/jvm-repo-rebuild/reproducible-central)
"once a conforming release has shipped with its `.buildinfo`." That precondition
is not the real one. reproducible-central rebuilds artifacts published to
**Maven Central**; JLS publishes only to GitHub Packages (`pom.xml:31-37`),
which — as §3.4's own caveat concedes — requires an access token even for
public reads. So the documented path to genuine third-party verification is
currently unreachable, and the `artifact:compare` recipe that P2 turns on is
gated behind a `settings.xml` and a `read:packages` token that no casual
auditor will produce. The friction is not incidental; it is why P2 has to be
performed by the maintainer.

Publishing `io.github.anadon:jls` to Maven Central (Sonatype Central Portal;
the `io.github.anadon` namespace verifies via the GitHub account already in
hand) collapses the whole problem:

- reproducible-central's infrastructure rebuilds the release and publishes a
  public pass/fail badge — an actual independent party, doing the actual
  independent rebuild, on a cadence JLS does not run;
- §3.4 loses its token caveat and becomes the frictionless recipe;
- as a side effect, autograder and lab-tooling authors can depend on JLS as a
  Maven coordinate, which serves the batch-interface arc directly.

I have found no record anywhere in `docs/` of Maven Central publication being
considered and rejected — the searches there are all about *consuming*
libraries. This looks like an unexplored option, not a settled one, and it is
the highest-leverage move available to this issue.

## Smaller things visible from here

- `project.build.outputTimestamp` (`pom.xml:47`) is a hand-maintained constant
  with a "bump it alongside the version" comment. Forgetting it does not break
  reproducibility, so no gate can catch it; the artifact just misreports its
  vintage. A release-time guard beside the existing tag↔pom check is the cheap
  fix. (Deriving it from the git commit date is the usual advice and would be
  *wrong* here: the perturbed rebuild builds from `git archive` with no `.git`,
  and so does any third party working from a source tarball.)
- The `.buildinfo` records "latest Temurin 25 GA at build time" after the fact.
  That is defensible and honestly documented, but it means every past release
  becomes unreproducible the day Adoptium drops that archive. Worth one line in
  §2 acknowledging the horizon.

## What I would disregard, and why

I am setting aside the DoD's last checkbox as written — "`mvn artifact:compare`
transcript … in a closing comment." A prose transcript in an issue thread is
not how this project stores truth about itself; CI jobs and normative docs are.
Keeping #185 open until a human pastes shell output into GitHub buys a claim
that is weaker than the gates already merged and that expires on publication.

Concretely: land Reframing 1 now (one CI step, or the shared build script),
schedule Reframing 2 as #819's actual content, file Reframing 3 as a new issue
against `docs/reproducibility.md` §6, and close #185 on the capability that has
been true since `2eb3e0c`. The feature shipped. What remains is not this
issue's completion — it is the next issue's premise.
