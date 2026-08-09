# Issue #721: TASK-C531-3: the whole four-way fixture runs containerized in CI with no platform account and no call to any platform service
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the mechanics and #721 is asserting two things about what JLS should become:
(1) the grading kit must be *verifiable without asking anyone's permission* — no vendor
account, no vendor uptime, no credential in CI; and (2) the four platforms' contracts
must become **artifacts JLS owns** rather than behaviour JLS observes.

Both are dead-centre for this project. The repo already lives this doctrine everywhere
else: `resources/packaging/Dockerfile` digest-pins `ubuntu:26.04@sha256:678c655…`, routes
apt through `snapshot.ubuntu.com` at `APT_SNAPSHOT=20260716T000000Z`, and clamps
timestamps to `SOURCE_DATE_EPOCH` from the HEAD commit; `.github/workflows/ci.yml` pins
every action by SHA; `repro-installers.yml` re-derives installers monthly. And
`docs/standards-adoption/07-waveform-formats.md` states the governing question outright:
*"the question is never 'can we get certified' but 'what artifact makes the claim
checkable by a skeptical reader.'"* #721 is that question applied to Gradescope,
Classroom, PrairieLearn and nbgrader. Endorsed in aim.

The framing, though, contains one claim that cannot be true as designed and one
architectural choice that costs far more than the outcome needs.

## Problem 1: a hermetic lane is structurally incapable of detecting platform drift

The Outcome's headline benefit — *"Platform drift then surfaces as a red lane against a
pinned contract rather than as an outage or a mid-semester surprise"* — is the one thing
this design provably cannot deliver. A lane that consults only a local pin, with
`--network none`, is green forever no matter what Turnitin ships next semester. It
detects **JLS drifting away from the pin**, which is worth having and is not what the
sentence says. The pin and the drift-detector are opposites: hermeticity is exactly the
property of being blind to the outside world.

The repo already has the right shape for the missing half, and it is not a lane.
`repro-installers.yml` is a *separate, scheduled, non-PR-gating probe* on
`cron: "17 6 3 * *"`. The reframing:

- **Hermetic conformance lane** (what #721 should actually build): blocking, offline,
  fast, asserts adapter output conforms to the pinned contract. Never touches a vendor.
- **Spec-drift canary** (a sibling task #721 should spawn, not absorb): its own workflow,
  monthly cron, fetches each platform's *documented* spec at its recorded URL, hashes it,
  and on change opens an issue naming the platform and diffing the retrieved text against
  the pin. Non-blocking by construction — a vendor 503 can never redden a PR.

That split also *supplies* AC-4 instead of merely wishing for it. "Updating a pinned
contract is a reviewed change that shows which adapter behaviour it affects" is exactly a
canary-opened issue plus the PR that answers it; today AC-4 is a discipline with no
mechanism, and disciplines with no mechanism decay first.

## Problem 2: three of the four adapters need no container at all

The ACs assume four symmetric containerized lanes. The platforms are not symmetric:

- **Gradescope** — the contract is *"a `results.json` with these fields"*. Testing your
  adapter against it needs a JSON validator, not a container.
- **PrairieLearn externalGrader** — same: a results JSON shape plus a mount convention.
- **Classroom Action** — the contract is the Actions output/annotation surface: text on
  stdout in a documented form.
- **nbgrader** — the *only* leg where the vendor's own software must be in the loop
  (autograde executes cells, the gradebook is a real SQLite schema). This is the one leg
  with genuine hermeticity cost, and its pin is a hashed wheel set, not a document.

So the natural seam is not "four lanes" but **pure adapters plus one packaging proof**:

1. Make each adapter a pure function `xUnit XML → platform artifact bytes`, with no I/O
   beyond its arguments. Then the 300-submission corpus runs *in `mvn verify`* as ordinary
   JUnit — 1200 in-process calls, seconds, zero containers, zero credentials, zero new
   flake surface. Hermeticity becomes a property of the code, not of the sandbox.
2. Keep **one** containerized lane whose job is the claim purity cannot make: that the
   built Gradescope zip / PrairieLearn image / nbgrader unit really executes end to end.
   Smoke-scale — a handful of submissions covering correct/partial/malformed/adversarial
   — not 300. Corpus scale belongs to the pure tests; the container proves packaging.
3. Reuse the **already-published image**, pinned by digest: `ghcr.io/anadon/jls` is built
   by `scripts/build-container.sh` from a recipe explicitly written so that "local builds
   and CI cannot drift." A fixture that stands up its own four Dockerfiles re-owns that
   asset and creates a second recipe to keep honest. Pin the digest the way the base image
   is pinned.

This dissolves CAP-21 risk 1 rather than isolating it. "A flaky adapter never wedges the
core toolchain matrix" is achieved best by there being nothing flaky to wedge with — a
pure function over committed bytes has no network, no clock and no container to flake.
And it removes a real recurring cost: `ci.yml` is already 1145 lines and ~20 jobs
including QEMU-emulated aarch64 installer reproducibility. Four more container lanes each
grinding 300 submissions is minutes on every PR, forever, for a claim that does not need
them.

## Problem 3: pin executable schemas, not prose

AC-2 as written ("pinned in the fixture with its version and source recorded") is
satisfiable by dropping four saved web pages in a directory. Inert. The load-bearing pin
is the *machine-checkable* form of each contract: a JSON Schema for Gradescope
`results.json` (including the visibility enum and output limits), a JSON Schema for
PrairieLearn results, the nbgrader gradebook schema plus the `nbgrader export` column set,
and a grammar for the Classroom annotation surface. Each carries a sidecar recording
source URL, retrieval date, vendor version string and content hash — the same metadata
`docs/standards-adoption/` already keeps for external standards, and the input the canary
diffs against. Then "conforms to the pinned contract" is a test result rather than a
claim, and a seeded perturbation fails with a schema path.

## Smaller notes

- `ordering_after: ["TASK-C531-1"]` is under-specified. This lane has nothing to execute
  until at least one adapter exists; TASK-C531-2 correctly lists `[..., 525, 526, 528, 530]`.
  Either mirror that or state that #721 lands incrementally, one platform at a time.
- "No platform account" is a consequence of the kit being files-only (#498 §8 exclusion 7),
  not an achievement to test for. The testable constraint is **run-time network
  isolation** — so make `--network none` the *only* invocation path rather than an extra
  "a network-isolated run passes" check that a future edit can quietly drop.
- Be explicit that the nbgrader image's build-time PyPI fetch is a pinned-by-hash
  dependency, not a platform-service call. Otherwise AC-1 reads as violated by the one leg
  that legitimately needs vendor software.

## The larger opportunity

If JLS is going to pin four grading platforms' documented contracts as versioned,
hashed, schema-shaped artifacts and keep them fresh with a canary, that collection is
worth more outside this repo than inside it. Publish `kit/contracts/` as a small public
registry with its own CHANGELOG. No teaching-tool ecosystem currently offers one, every
instructor writing a bespoke harness needs exactly it, and it costs almost nothing beyond
what #721 already builds. That turns a private CI fixture into a piece of the ecosystem —
the same move `docs/batch-interface.md` made by publishing the batch contract as a
stability promise instead of keeping it as internal behaviour.

## Disregarded acceptance criteria, and why

I am explicitly setting aside AC-1's *"every adapter lane runs in a container"* and AC-3's
dedicated-four-lane framing. They buy isolation for a problem that purity removes, at
permanent CI cost, and they push the 300-submission corpus into the slowest possible
execution mode. Replace with: pure adapters tested at corpus scale in `mvn verify`; one
network-isolated container lane proving packaging at smoke scale against the digest-pinned
`ghcr.io/anadon/jls`; nbgrader's leg carrying its own hash-pinned image; and a separate
scheduled spec-drift canary that owns the outcome sentence this issue currently claims for
a lane that cannot deliver it.
