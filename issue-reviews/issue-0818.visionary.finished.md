# Issue #818: TASK-C184-1: the pinned container build runs twice at one commit and its JLS-controlled layer digests are compared — the claim that was never exercised
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is a digest table in a comment on #184. The actual purpose,
read against #184's governing invariant — *"No artifact is claimed reproducible
without a CI gate or recorded two-build evidence"* — is to close the last hole in
JLS's distribution-integrity story: the container is the one release asset whose
determinism plumbing shipped without ever executing. That end is right and worth
doing. Everything below is about the method, the metric, and where the evidence
lives, because as written all three pull against the arc the project has followed
for every other artifact.

Note that ARCHITECTURE.md is silent here by design — it describes the simulator
core, not distribution. The trajectory this issue must be judged against lives in
`README.md` (lines 52-62), `docs/reproducibility.md`, `.github/workflows/ci.yml`
(five reproducibility jobs, lines 798, 866, 924, 1000, 1064), and
`.github/workflows/repro-installers.yml`. That arc is unambiguous: **every
reproducibility claim in this project is a standing, re-run check, not a recorded
one-time observation.**

## AC-1's recommended path cannot produce AC-2's data

This is not a nitpick; it invalidates the plan. In `scripts/build-container.sh`
the reproducibility-relevant export exists *only* on the push branch:

- line 82: `--output "type=image,push=true,rewrite-timestamp=true"` — the mtime
  rewrite is attached to the push output, nowhere else.
- line 84: on the non-push path the script itself prints *"PLATFORMS without
  PUSH=1: build-only, results stay in the buildx cache"*.
- lines 91-95: `target/container/digest` is written only when `PUSH=1`.

And in `release.yml` (lines 212-221) `PUSH` is `1` only for `github.event_name ==
'push'`; the digest read step is gated on the same condition. So two
`workflow_dispatch` dry runs export nothing, load nothing, and write no digest
file. There is no artifact to hash, let alone diff. The Dockerfile comment
(lines 31-32) already concedes the underlying fact: *"layer-tarball mtimes
additionally need the push path's `rewrite-timestamp=true` output option."*

The corollary is sharper than a broken recipe: **container reproducibility as
implemented is a property of the export, not of the build.** The layers' internal
mtimes are wall-clock until BuildKit rewrites them on the way out. Any local
comparison (`docker image inspect` diff_ids from the native build at
`release.yml:187-190`) will differ between two builds and will look like a
reproducibility failure that is actually a plumbing artifact — the exact
false-alarm that AC-4 would then send someone chasing "apt cache state, ldconfig
output, jlink ordering" for nothing.

## Reframing 1 — make the exportable path the normal path, then gate it

Instead of arranging a bespoke manual run, delete the special case. Give
`build-container.sh` an always-present output spec:

- push: `type=image,push=true,rewrite-timestamp=true` (unchanged)
- otherwise: `type=oci,dest=target/container/image.oci.tar,rewrite-timestamp=true`

That is a handful of lines and it changes the shape of the problem entirely. Two
builds now yield two OCI tarballs, offline, with no registry, no credentials, and
no maintainer needed to trigger a publish; the per-layer digests are read straight
out of the tarball's `index.json`/manifest, and a mismatch feeds `diffoscope` on
the tarballs directly. What #818 frames as a one-off experiment requiring a docker
daemon and a dispatch becomes a three-minute CI step — and it also fixes a real
latent hazard, that the dry-run leg today does not exercise the export path a
release will use.

Then add the leg where its precedent already sits.
`.github/workflows/repro-installers.yml` is the template this issue should have
copied: dispatch + monthly cron, matrix of legs, double build, per-artifact sha256
table into `$GITHUB_STEP_SUMMARY`, diffoscope on divergence, `continue-on-error`
report-only until a lane proves stable, then promoted to a hard gate (as #189's
aarch64 lane was, per `docs/reproducibility.md:168-170`). A `container` leg in
that probe answers P2 on its first monthly run and keeps answering it forever.
That is strictly more than #818 asks for, at less human cost.

## Reframing 2 — the metric. Layer digests are the wrong primary assertion

`APT_SNAPSHOT` was not introduced to make digests equal. It was introduced so the
package set cannot float with the upstream mirror (`Dockerfile:21-28`). Those are
different claims, and the cheap one is the one that matters:

- **Tier 1 (durable, robust, what the pin is actually for):** the installed
  package set and versions are identical across builds. `dpkg-query -W
  -f='${Package} ${Version}\n' | sort` hashed and compared. This survives buildx
  upgrades, compression changes, and jlink churn; it fails loudly on the realistic
  regression — the snapshot service being unavailable or apt silently falling back
  to the moving archive — which a digest comparison would report only as an
  unexplained mismatch.
- **Tier 2 (aspirational):** OCI-export layer digest equality, per Reframing 1,
  report-only until green, then gated.

Out-of-the-box corollary: **bake the tier-1 evidence into the artifact.** Write
the sorted package manifest to `/opt/jls/packages.txt` in the final stage. Then
the pin's effect is verifiable by any consumer with `docker run ... cat`, is
covered by the existing cosign signature and provenance attestation on the
manifest digest, and the smoke test at `release.yml:191-195` can hash it. Evidence
that lives inside the signed artifact beats evidence in an issue comment, which
brings me to the last point.

## Reframing 3 — the evidence destination

AC-2 puts the outcome in a comment on #184. That is the one place in this project
where reproducibility evidence does *not* live. The msi got
`docs/windows-msi-determinism.md`; the dmg got `docs/dmg-reproducibility.md` plus
`scripts/measure-dmg-repro.sh`; the whole set is declared in
`docs/reproducibility.md` §1, whose table currently reads "Container image — No"
(line 20) and whose §6 promises the table expands "as #184 makes the container
image deterministic." The natural close-out is `docs/container-reproducibility.md`
(command, snapshot date, base digest, buildx version, digest table) plus the §1
row flipping — not a comment that is stale the instant `APT_SNAPSHOT` or the base
digest is bumped, with nothing in the repo detecting that staleness.

## What I am disregarding, and why

I am explicitly disregarding **AC-1** and **AC-2** as written: the recommended
two-dry-run path exports no image and produces no digests, so AC-2's table cannot
be built from AC-1's runs. Replace both with: (a) add the OCI export with
`rewrite-timestamp=true` to the non-push path of `build-container.sh`; (b) add a
container leg to `repro-installers.yml` that double-builds and diffs the
tier-1 package manifest and the tier-2 layer digests, report-only.

**AC-3, AC-4, AC-5 I endorse unchanged and would strengthen.** AC-5's discipline —
no claim beyond what this run covered — is the best sentence in the issue and is
precisely why AC-1/AC-2 must be fixed rather than executed: running the
recommended path and reporting "no divergence observed" from a build that exported
nothing would itself be the over-claim AC-5 forbids. AC-4's named residuals are
also where I would place the honest risk: `jdeps --print-module-deps` ordering
feeding `--add-modules`, and jlink's `--compress zip-6` output (`Dockerfile:47-50`)
are the plausible non-determinism, and tier-1 will not catch them — tier-2 will.

## One structural observation, larger than this issue

The image builds its own trimmed runtime with an apt-provided JDK inside the
container (`Dockerfile:47-50`), while `scripts/build-installer.sh` runs the same
jdeps→jlink recipe on the runner with a Temurin JDK. Two independent
runtime-construction paths, each needing its own determinism argument. If the
container consumed the runtime the installer path already produces and gates
(`ci.yml:866`), the container's hardest reproducibility question would collapse
into a mechanism that is already proven. That is out of scope for #818 and
probably for #184, but it is the seam worth cutting along the next time this area
is opened, and it belongs in #185's conformance arc rather than as another
per-artifact patch.

## Verdict

**endorse-with-reframing.** The goal — retire an unexercised claim before it
hardens into documentation — is correct and overdue. Executed as written it would
either fail to produce data or produce misleading data, and would deposit whatever
it did produce in a form that decays. Reframed as "make the export path testable,
then add the container leg to the probe workflow that already exists, and assert
the package pin as the primary claim," the issue delivers more, costs less, needs
no maintainer to trigger anything, and stops being a task that has to be redone
the next time `APT_SNAPSHOT` moves.
