# Issue #184: Release-artifact reproducibility gaps: container apt pinning, installer SOURCE_DATE_EPOCH, and a BOM reproducibility guard in CI
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the three landed findings away and one sentence remains: *a person who downloads
JLS should be able to tell, per artifact, exactly what kind of trust they are being
offered — and that statement should not be able to go stale.* That aim is right, it is
squarely on the project's arc (an educational tool students download and instructors
approve), and the plumbing to serve it has almost entirely landed.

But the issue's single outstanding criterion — P2, "run the pinned container build twice
at one commit, diff the JLS-controlled layer digests, paste the table into a closing
comment" — does not serve that aim. **I am explicitly disregarding P2 as the acceptance
criterion.** Three reasons, in ascending order of importance.

## 1. P2's recommended method cannot produce P2's evidence

`build-container.sh` attaches `rewrite-timestamp=true` *only* on the push path
(`scripts/build-container.sh:79-82`, guarded by `PUSH=1`), and writes
`target/container/digest` only there too. `release.yml`'s container job sets
`PUSH: ${{ github.event_name == 'push' && '1' || '0' }}`
(`.github/workflows/release.yml:207-215`), so the issue's own recommended route — "a
`workflow_dispatch` dry run of `release.yml`, run twice" — builds multi-arch *without*
`--load` or `--push`, exercises neither `rewrite-timestamp` nor digest capture, and
leaves nothing addressable to compare. Option (b), the next real release plus one local
rebuild, is not like-for-like either: the local rebuild takes the non-push path and so
omits the mtime rewrite the published image had. That asymmetry is itself the finding I
would surface — **the documented rebuild path is deterministically different from the
publish path, so an independent verifier cannot match the published layers even in
principle.** The one-line fix (move `rewrite-timestamp=true` onto every image-type
exporter, including `type=oci,dest=…` and `type=docker`) is worth more than the
experiment, and it is not on the checklist.

Three weeks of "outstanding, awaiting a maintainer to hand-trigger a run" is the symptom;
an unexecutable method is the cause.

## 2. P2 has almost no falsifying power

Two builds minutes apart on one runner would pass **even with no snapshot pin at all** —
the Ubuntu mirror does not move between them. The hypothesis the pin exists to defend is
*temporal*: that the image built from tag `v5.0.x` today matches the one built from it in
six months, across mirror rotation, base-image retagging and archive churn. A same-runner
double build is structurally incapable of falsifying that. The project already knows this
distinction and applies it elsewhere — `ci.yml`'s reproducibility job deliberately
escalates from a same-runner pre-filter to a *perturbed* independent rebuild
(`.github/workflows/ci.yml:829-847`, shifted path/TZ/locale/umask). P2 stops at the
pre-filter tier for the one artifact whose dominant variance axis is time.

**Better experiment, same cost:** a monthly cron that rebuilds the most recent release tag
and diffs its JLS-controlled layer digests against the published manifest. That is a real
null test for the snapshot pin, it runs unattended, and it fails loudly when
`snapshot.ubuntu.com` semantics change or the base digest is force-moved.

## 3. The capability this feature claims is already satisfied — by a different file

§1 promises "the *whole* release-asset set has a classified, guarded integrity story."
`docs/reproducibility.md:14-25` is that classification, and it already reads: jar **Yes**,
BOM **Yes**, Linux deb/rpm/AppImage **Yes (CI-gated)**, msi/dmg **No**, container **No**.
The container being "No" is a *correct, honest* entry — not a defect. Nothing in the
capability statement requires flipping it to "Yes"; P2 is a leftover from the pre-#185
framing, when the classification table did not exist yet.

Meanwhile the thing that genuinely violates the capability statement is not on the
checklist at all: **`README.md:53-60` — the very permalink §1 cites as evidence that
Finding A landed — states "the installers are *not* byte-reproducible (the native
packaging tools embed wall-clock state)".** That was true when it was written and is false
now: `ci.yml`'s `installer-reproducibility` job hard-gates deb/rpm/AppImage byte-identity
on both arches, and `docs/reproducibility.md:161-171` says so. The README now understates
the guarantee, telling a Linux verifier not to bother rebuilding something that *does*
rebuild identically. `docs/reproducibility.md:196-199` has the mirror-image staleness
("the container image ... installs distribution packages at build time" — it no longer
does; they are snapshot-pinned). A checklist item scored `[x]` on a permalink to prose
that has since drifted is exactly the failure mode this whole cluster keeps trying to
legislate away with boundary comments.

## The reframing: make the classification a build input, not prose

One artifact replaces the pile: **an in-repo `release-artifacts.yaml`** — one entry per
published asset, each with `integrity: reproducible-gated | attestation-only`, the CI job
name that proves it, and the verifier command. Then:

- CI fails if `release.yml` publishes an asset absent from the manifest, or if a
  `reproducible-gated` entry names a job that does not exist or is not required.
- The §1 table in `docs/reproducibility.md` and the README paragraph are **generated from
  it** (or diffed against it), so `README.md:53-60` cannot contradict `ci.yml` again.
- #185 gets its "declared artifact set" for free; #338 gets its acceptance surface;
  #184's own §4 invariant ("no artifact is *claimed* reproducible without a CI gate or
  recorded two-build evidence") becomes machine-checked — and the "or recorded two-build
  evidence" escape hatch, which is what P2 is trying to satisfy by hand, can be deleted
  outright. Evidence in an issue comment is not a guard; it rots at the next
  `APT_SNAPSHOT` bump and no one notices.

Much of the seven-issue bookkeeping around this cluster (#184/#185/#188/#338 plus slices
#471/#818/#819, three dedup passes, two boundary records) exists to keep prose claims
aligned with build reality across issue boundaries. A generated manifest does that
continuously and at zero marginal cost per pass.

## A different seam for the container itself

If the container is ever to move to "Yes", the elegant cut is not a better experiment —
it is **shipping no apt-installed layer at all**. Today the final stage apt-installs
`fontconfig fonts-dejavu-core` into a shipped layer (`resources/packaging/Dockerfile:59-63`),
which forces the snapshot pin into the runtime image and then forces the
`rm .../50snapshot` trick to undo it. Replace that with `COPY --from=runtime` of the font
files and fontconfig libs, and the shipped JLS-controlled layers reduce to three copies of
already-reproducible content: the jlink runtime, the jar, the fonts. Determinism then
rests on one question (is `jlink` output byte-stable?) instead of on an apt transaction
plus an external snapshot service that, note, is currently a *release-blocking availability
dependency* — if `snapshot.ubuntu.com` is down, the container job fails and no security
release ships. The builder stage still needs the pin; builder layers are not published, so
that is fine.

## Concretely, what I would put on the checklist instead

1. Fix `README.md:53-60` and `docs/reproducibility.md:196-199` to match CI. (Closes the
   real violation of this feature's own capability statement.)
2. Move `rewrite-timestamp=true` onto all image exporters in `build-container.sh`, so the
   local/dry-run path is the published path.
3. Add a `container-reproducibility` CI job: amd64 only, `--output type=oci,dest=…`, built
   twice, layer digests diffed. No daemon-with-registry, no QEMU, no maintainer in the
   loop — the same shape as the two double-build gates already in `ci.yml`.
4. Add the monthly rebuild-the-last-tag drift check (§2 above) — or, if that is judged not
   worth the runner minutes for an autograder-facing image whose consumers already pin by
   digest, **close #184 by narrowing**: container stays `attestation-only` in the manifest,
   which is what `docs/reproducibility.md` already declares. Comment 5176158422 anticipates
   this outcome; I think it is the *default* answer, not the fallback.
5. Land `release-artifacts.yaml` and generate the two tables from it.

Items 1–3 are days of work and end the issue honestly. P2 as written is neither necessary
(item 4) nor sufficient (item 2) nor executable (§1).
