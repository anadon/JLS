# Issue #699: TASK-C525-3: Gradescope spec drift turns a dedicated lane red, never a live course — plus the template README executed as CI doc-test steps
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its four checkboxes, #699 makes one claim about what JLS should become: **the
grading kit is a promise JLS keeps, not a sample JLS published.** Everything else in CAP-21
(#502) is delivery — templates, adapters, images. #699 is the part that says the delivery
does not rot. That claim is squarely on the project's arc. This repository already treats
"the documented procedure must be executable and self-tested" as house law:
`scripts/wayland-rig-selftest.sh` drives the unmodified Wayland rig against a stub toolchain
and asserts each failure is classified with the documented exit code;
`scripts/icestick-handoff-selftest.sh` does the same for the FPGA handoff with a hermetic
stub PATH, no hardware, no network. #699 is that pattern applied to a grading platform.
Endorsed in intent.

But the headline is not achievable as written, and two of the four criteria re-implement
things the tree already does better. Below: one falsification, three reframings, one addition.

## 1. The headline claim is false as specified (the falsification)

"Gradescope spec drift turns a dedicated lane red, never a live course."

AC-1 validates emitted `results.json` **against the pinned documented spec**. The pinned spec
lives in JLS's tree. The adapter lives in JLS's tree. When Turnitin changes `results.json`,
neither file changes — the lane stays green and the course still breaks. What the lane
actually detects is *adapter regression against JLS's belief about the spec*, which is worth
having and is not what the title promises.

This exact failure mode is already named and mitigated elsewhere in the tree.
`docs/standards-adoption/03-accessibility-conformance.md:795` calls it out for the ACR — "the
published document silently becomes false" — and prescribes the fix: *put the report date and
commit SHA in the report so staleness is visible*, plus a tripwire that fires independently.

**Reframing.** Split the guard in two, and name each honestly:

- **Inward (blocking, hermetic):** `docs/gradescope-results-schema.md` or a JSON Schema
  checked in with provenance front-matter — source URL, fetch date, SHA-256 of the fetched
  page. The lane validates emitted `results.json` against *that snapshot*. Failure means
  **adapter-side**.
- **Outward (scheduled, advisory):** a spec-watch lane on the nightly cron — this repo already
  runs a cron lane for exactly one job (`gui-wayland`, `.github/workflows/ci.yml:12`) — that
  re-fetches the published spec and fails when the recorded hash moves. Failure means
  **platform-side**. It is allowed to be non-blocking; it must not be absent, because it is
  the only mechanism in the design that can ever go red on real drift.

Without the second lane, AC-1 should be retitled to what it does. With it, the issue's title
becomes true.

## 2. AC-3 is already shipped, and can be made structural instead of asserted

"An instrumented build asserts that no interactive session is opened at any point in the lane."

`test/jls/HeadlessCoreCanaryTest.java` already does this, and does it harder: it forks a JVM
with `-Xlog:class+load` and `java.awt.headless` deliberately **unset**, runs a load +
`BatchSimulator` round trip, and asserts no `java.awt.`, `javax.swing.` or `jls.edit.` class
is ever loaded. Re-asserting the same property inside a CI lane is weaker (it covers only the
paths CI walks, never the instructor's edited copy of the template) and, per CAP-21 AC-4, is
destined to be written four times, once per adapter.

**I am disregarding AC-3 as written.** The better goal is to make an interactive session
*unrepresentable* in the graded runtime rather than asserted absent in one lane:

- `resources/packaging/Dockerfile:48` derives modules with `jdeps --print-module-deps` from
  the shaded jar, so `java.desktop` comes along and `-Djava.awt.headless=true` at line 75 is
  the compensating control.
- Issue #77's headless-core extraction is already ratcheting (`HeadlessCoreRatchetTest`,
  shrinking baseline). When `jls.core` exists, a `jls-grade` image can be jlinked **without
  `java.desktop`**, and "no adapter opens an interactive session" becomes a property of the
  module graph — one check, true for all four platforms and for every fork of the template.

Restated AC-3: *the pinned grading image's module graph contains no `java.desktop`; the lane
asserts the module list of the image it actually runs.* If #77 is too far out, keep the
canary and have the lane assert it ran — do not build a fifth bespoke instrument.

## 3. Build the harness adapter-generic, not Gradescope-shaped

#699 is scoped as task 3 of 3 for one platform. But CAP-21 needs the identical machinery for
GitHub Classroom, PrairieLearn and nbgrader: a dedicated lane, a dated platform-contract
snapshot, a README doc-test, a blame classifier. Shipping it Gradescope-shaped guarantees
either three copies or a refactor at adapter #2 — and this is the *first* adapter, so the
shape chosen here is the shape the other three inherit.

**Reframing.** Make the deliverable a small adapter-conformance harness with a registry —
one directory per platform holding `spec-snapshot` (+ provenance), `doctest.steps`, and the
expected blame taxonomy — whose first and only registered client is Gradescope. The band
(0.5–1 mw) barely moves; the payoff is that KC-21-3 ("drop the adapter, ship three platforms
rather than one scraped one") becomes *delete a registry entry* instead of *unpick a lane*.
A kill criterion you cannot execute cheaply is a kill criterion that never fires.

## 4. The best idea in the issue is under-generalized, and untested as filed

AC-4 — failure output names JLS-side / adapter-side / platform-side — is the strongest thing
here and is pure house style: it is `wayland-rig.sh`'s exit 0/1/2 verdict ("a failure is
classified as JLS-side — exit 1 — or upstream JBR/sway — exit 2", README:255) and it is
`LoadError`'s fixed category taxonomy that tests assert on (`ARCHITECTURE.md:185`).

But as filed it is prose. Both precedents carry a **selftest that pins the classification
itself** against stubbed inputs. Add that AC: a hermetic selftest drives the unmodified
adapter against a malformed `results.json`, a contract-violating xUnit input, and a mutated
spec snapshot, and asserts each is classified into the right bucket. Otherwise the classifier
is unexercised until its first real failure, which is the worst moment to discover it
misattributes.

**Dependency reality worth flagging:** `docs/batch-interface.md` §1 documents exactly three
exit statuses (0/1/2) — and a known deviation where test-file errors print to *stdout* and
exit 1. CAP-21's PF-1 plans a status 3, which does not exist. Until it does, "JLS-side vs
adapter-side" can only be distinguished by parsing stdout strings — precisely the
"grading as literal bytes of a report format" antipattern CAP-21's own Background indicts
`examples/autograde/autograde.py` for. Either order #699 after the status-3 widening, or
record that the classifier ships string-matching and will be rewritten.

## 5. The addition the issue never considered: verify the pin, not just the zip

TASK-C525-1 (#694) plans "a Dockerfile pinning a headless-JRE JLS build." JLS **already
publishes** that artifact: `ghcr.io/anadon/jls`, multi-arch (amd64/arm64/riscv64),
keyless-cosign signed, provenance-attested, `SOURCE_DATE_EPOCH`-stamped, digest-addressable,
headless by construction (README:103-124). A second, template-local JRE derivation is a
duplicate of a signed asset, and #699's doc-test is where that choice would get frozen.

So the lane should assert something stronger than "the zip is well-formed": that the digest
the template pins **resolves to a published image**, and that `cosign verify` and
`gh attestation verify` pass inside the lane. That turns the guarantee from *our adapter
emits valid JSON* into *the artifact an instructor runs is the artifact we signed* — which
is continuous with the project's entire distribution arc (SHA256SUMS, `.buildinfo`,
`docs/reproducibility.md`) and is a claim no peer teaching simulator makes. It costs a few
lines and it is the highest-leverage thing in this task.

## 6. Doc-test honesty (a bounded, achievable version of AC-2)

"From a clean checkout to a graded fixture assignment, with no undocumented manual step in
between" cannot be literally met: creating the course, uploading the zip and configuring the
autograder are UI operations on a proprietary platform that risk 5 forbids automating. The
doc-test will therefore cover a prefix of the README and the rest stays unverified prose —
and the AC will read as satisfied by testing the easy half.

The tree already solved this shape: `docs/wayland-desktop-checklist.md` is a scripted
CI lane for what is automatable *plus* a dated once-per-release manual spot-check for what is
not. Do the same: mark every README step `machine-executed` or `manual-verified <date>`, and
make the doc-test **fail on an unmarked step**. That is a real executable-completeness
property, and it is checkable.

## Verdict

**endorse-with-reframing.** The work belongs and the instinct is exactly right, but ship it
as: (1) inward hermetic conformance against a *dated, hash-recorded* spec snapshot plus an
outward scheduled spec-watch that is the only thing entitled to the phrase "spec drift";
(2) no-interactive-session as a module-graph property of the pinned image, not a fifth
bespoke assertion; (3) a platform-generic conformance harness whose first client is
Gradescope; (4) a selftested blame taxonomy, ordered behind the exit-status widening;
(5) signature/attestation verification of the pinned image inside the lane; (6) a doc-test
that fails on unmarked steps rather than pretending the platform half is covered.
