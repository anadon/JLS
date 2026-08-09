# Issue #705: TASK-C526-3: the Classroom starter repo template ships, its README runs as CI doc-test steps, and runner-image drift turns the adapter lane red
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Three of #705's four criteria serve one end that is unmistakably in JLS's grain:
*an instructor's path from nothing to a graded assignment must be an executable
artifact, not prose, and it must be pinned to a build whose bytes are named.*
That is the same conviction behind `scripts/build-installer.sh` ("the single
recipe used both locally and by CI"), the `.buildinfo` rebuild recipe, the
cosign/attestation verification commands printed in README.md, and
`AutogradeBridgeExampleTest`, which already runs `examples/autograde/autograde.py`
over the real batch CLI "exactly as docs/vcd-interop.md tells an instructor to
run it." #705 is asking for one more instance of a pattern the project has
already run five times. Endorse the end.

The fourth criterion — a dedicated lane whose job is to surface *GitHub Actions
runner-image drift* — is where this issue pulls against the trajectory, and it is
the one I would disregard as stated. Details below.

## Why AC-3's runner-drift clause buys nothing here

Classroom is the one adapter in CAP-21 whose "vendor" is GitHub, and JLS is
already one of GitHub's heaviest customers in this repository. `.github/workflows/ci.yml`
carries 16 jobs; `ubuntu-latest`, `windows-latest`, `macos-latest` and
`ubuntu-24.04-arm` all float, on every push **and** on a nightly cron
(`schedule: cron: "17 4 * * *"`, ci.yml:12-13). Runner-image drift already turns
this repository red, on fifteen surfaces, every night. A seventeenth lane whose
declared purpose is to notice the same event adds no information the maintainer
does not already receive — and it collides with its own sibling clause: the only
way to *see* image drift is to sit on the floating image, which is exactly what
the core matrix (the thing AC-3 forbids the lane from joining) already does.
Gradescope (#699) and PrairieLearn (#710) have a real blind spot — their upstream
is invisible from a hermetic lane. Classroom does not have that problem; it has
the opposite one, a signal already arriving in duplicate.

**I am explicitly disregarding AC-3's "distinguishes runner-image drift from an
adapter or contract fault" requirement as written**, because the better move is
not to classify that failure but to delete the failure class.

## Reframe 1: design the hazard out — make `jls-grade` a container action

README.md:103-124 already ships `ghcr.io/anadon/jls`, headless by construction,
multi-arch (`amd64`/`arm64`/`riscv64`), keyless-cosign signed and attested. If
`jls-grade` is a **Docker container action pinned by digest**
(`image: docker://ghcr.io/anadon/jls@sha256:…`) rather than a composite action
that `setup-java`s a JDK and unpacks a jar on the runner, then the runner image
contributes nothing to the grade but the Actions runtime and the Docker daemon.
Toolchain drift under the Action becomes structurally impossible instead of
monitored. This also makes AC-4 — "records which JLS build digest it pins" — fall
out of the action definition itself rather than being a README paragraph that can
disagree with the workflow.

There is a second, sharper reason to prefer the container. #701's caching
criterion assumes Actions cache helps; **`actions/cache` is scoped per
repository**, and in the Classroom topology every student repo is a fresh
repository that pushes a handful of times. Two hundred students means two hundred
permanently cold caches. A pinned image whose layers the hosted runner may
already hold, or simply a small image, is the real answer; a cache key is theatre
at this cardinality. #705 is the issue that writes the workflow file students
actually run, so this belongs in its reframing even though the Action is #701's.

## Reframe 2: the starter is a *rendering* of the lab kit, not a fourth hand-written kit

CAP-21 will otherwise land four hand-maintained starter kits (#699, #705, #710,
#713) that differ in packaging and agree in content. Cut the seam the other way:

- `labs/<lab>/` — the CAP-06 lab-as-data unit: starter circuit, hidden vectors,
  point values. Platform-neutral, one copy, shared with #531's corpus.
- `adapters/classroom/` — the Action, plus a ~15-line workflow *template* and a
  `walkthrough.sh`. Nothing else Classroom-specific exists.
- `scripts/new-starter.sh classroom <lab> <outdir>` — materializes a
  ready-to-push starter tree from those two inputs.

Then "a repo generated from it grades itself on push with no manual wiring"
becomes a property a script produces and CI can check, instead of a claim about a
directory. Adding a fifth platform is a directory, not a task.

## Reframe 3: AC-1 as written is not testable in-tree, and the generator fixes that

GitHub Classroom generates student repos from a **template repository** — a real
repo with the template flag, living in some org. An in-tree directory is a
*source* for one, not one. As written, AC-1 either quietly requires an
out-of-tree artifact (which drags in Open Question 3's org ownership, the thing
CAP-21 explicitly deferred) or it is unfalsifiable. With Reframe 2's generator,
the honest and stronger criterion is: *`new-starter.sh` emits a tree, CI pushes
nothing, and a job runs that tree's own workflow file against a fixture
submission via a local action reference (`uses: ./adapters/classroom/action`) on
a hosted runner.* That is a genuine end-to-end run of the real Action on the real
runner image with zero org, zero Classroom account, zero network dependency on a
vendor — and it *is* the doc-test. The lane stops being a 0.5–1 mw drift
apparatus and becomes one job of perhaps twenty lines.

## Reframe 4: generate the README from the script; do not execute the README

"The README's steps execute as doc-tests" keeps two artifacts in agreement by
parsing one of them, and requires fenced-block extraction machinery that #699,
#710 and #713 would each write again. Invert it: `walkthrough.sh` is the source of
truth, CI runs it, and `README.md` is *rendered* from it — commands plus captured
output — with byte-identity asserted the way the jar and `bom.json` already are.
"The README drifted" and "an undocumented manual step crept in" stop existing
rather than being detected. `CliFlagTableTest` and `HelpTopicsTest` are the
in-tree precedent for docs that cannot outlive the behavior they describe; this
is the same move one level up.

## What to keep and strengthen: AC-4 is the best line in the issue

"The template records which JLS build digest it pins and how an instructor
updates it" is the criterion most aligned with the project's whole arc, and it is
the one stated most weakly. Make it an artifact, not a note: a `jls.lock` in the
starter carrying version, image digest, and the exact
`cosign verify … --certificate-identity-regexp='^https://github.com/anadon/JLS/'`
line from README.md:120-123; an `update` path that is a script; and a CI check
that the pinned digest still resolves and still verifies. *That* is a drift lane
worth having — it catches our own release moving, being retagged or being
deleted under a live course, which is a failure this repository can actually
cause and currently cannot see. Note also that ci.yml pins every third-party
action by commit SHA with a version comment; the starter workflow must inherit
that discipline, or the kit teaches instructors a habit the project rejects.

## Where this pulls with the arc, and where against

With: executable-not-prose documentation, files-only kit (#498 §8 exclusion 7
untouched by every reframe above), named failures over silence, digest-pinned
reproducibility. Against: as written it commits a single-maintainer project to a
seventeenth CI lane guarding a vendor surface fifteen existing lanes already
guard, and to a fourth bespoke starter kit whose content is identical to three
siblings. The reframes preserve every outcome #705 states — template ships,
walkthrough executes in CI, digest pin recorded and updatable — while removing
the lane that duplicates existing signal and the machinery that would be written
four times.

## Recommendation

Endorse the outcome; rewrite the route. Concretely: keep AC-1 (via a generator
script so it is testable in-tree), keep AC-2 but invert its direction
(README rendered from `walkthrough.sh`), **drop AC-3's runner-drift clause** and
replace it with the pinned-digest resolvability check, and promote AC-4 from a
recorded note to a `jls.lock` plus update script. If the shared
`adapters/`-registry harness proposed on #699/#710 lands first, what remains here
is a directory and a fixture.
