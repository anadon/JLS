# Issue #694: TASK-C525-1: the in-tree Gradescope template exists — pinned headless-JRE image, setup and run_autograder scripts, and an xUnit-to-results.json adapter with a visible/hidden split
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

One sentence, from CAP-21 (#502): an instructor who wants JLS grading on Gradescope
should not have to write a bespoke wrapper and guess at exit codes. That end is
squarely inside JLS's arc — `docs/batch-interface.md` §6 already freezes the `-t`
grammar, the stdout report and the VCD profile *as a stability contract for graders*,
and the README already advertises `ghcr.io/anadon/jls` as the headless surface "for
autograders and CI". The gap #694 names is real and `examples/autograde/autograde.py`
is its honest measure: three literal report lines as a grading criterion.

I endorse the outcome and the boundary (no LTI, no JLS-operated service, documented
spec only). I do not endorse the three mechanisms the acceptance criteria pick, and I
say so explicitly below: **AC-1's "a template directory in tree that instructors copy"
and AC-2's adapter-owned visible/hidden decoding are, in my view, the wrong artifacts,
and I am disregarding them.** AC-3 and AC-4 survive the reframing unchanged — indeed
they get easier.

## Reframing 1: the Dockerfile is already written, and it is signed

AC-4 asks that "the JLS build is pinned by digest". JLS *already ships* a digest-
addressable, multi-arch, reproducibility-disciplined headless image:
`resources/packaging/Dockerfile` (digest-pinned Ubuntu base, apt snapshot pin,
`SOURCE_DATE_EPOCH` from the commit, jdeps/jlink-trimmed runtime), built by the single
recipe `scripts/build-container.sh`, published, keyless-cosign-signed and
provenance-attested by `.github/workflows/release.yml` (which already holds
`steps.digest.outputs.digest` in hand).

A second Dockerfile "pinning a headless-JRE JLS build" inside a Gradescope template is
a *third* copy of a recipe the project deliberately unified into one. The template's
container step should be two lines against the artifact that already exists:

```dockerfile
FROM gradescope/autograder-base:<pinned>
COPY --from=ghcr.io/anadon/jls@sha256:<digest> /opt/jls /opt/jls
```

Then "pinned by digest" is not a new discipline to maintain, it is the digest the
release workflow already computed, signed and attested — and an instructor can verify
their grader's JLS with the exact `cosign verify` / `gh attestation verify` commands
the README already documents. No other autograder kit in this space can say that.

## Reframing 2: the adapter should not be a script — `results.json` should be a JLS output format

This is the load-bearing one. The issue's adapter is a program that reads xUnit XML,
reads the lab's visible/hidden declaration, applies per-test scores, bounds output, and
writes `results.json`. Three consequences follow, and all three are avoidable:

1. **It re-implements the lab-as-data format outside JLS.** AC-2 requires the split be
   "declared by the lab, not hard-coded in the adapter" — so the adapter must either
   parse the CAP-06 lab file (a second parser, in another language, with no shared
   conformance test) or decode points and visibility out of xUnit `<properties>`. xUnit
   has no slot for points or visibility; smuggling them through a foreign envelope means
   every one of the four adapters must know a private JLS convention anyway. The envelope
   buys nothing it was chosen for.
2. **It makes CAP-21 AC-1 (byte-identical scores across four platforms) an empirical
   claim** requiring a 300-submission corpus run through four containers. If instead one
   scoring computation inside JLS emits N serializations, parity is true *by
   construction* and testable as a golden file in `mvn verify` — the same discipline
   `VcdExportGoldenTest` already applies to the VCD profile.
3. **It multiplies by four.** #526 / #528 / #530 each repeat the whole exercise.

The alternative: make the platform result document a JLS report format, selected by a
flag — `jls -b -t lab.tests -report gradescope:results.json circuit.jls`. Then the
"template" is a `run_autograder` of about ten lines and a Dockerfile of two, and the
Gradescope, PrairieLearn, Classroom-annotation and TAP emitters are four small classes
next to `VerilogEmitter`/`VhdlEmitter` rather than four kits in three languages.

Anticipated objection — CAP-21 §3 risk 1 says adapters must live in dedicated CI lanes,
not the core matrix. That constraint is about *external toolchains* (docker, iverilog,
platform accounts), and it is untouched: the containerized end-to-end lane still lives
outside the core matrix. A JSON serializer with a byte-exact golden adds no toolchain,
and JLS already emits two foreign formats in core (IEEE 1364 VCD, Verilog-2005) under
§6's stability promise. Vendor drift becomes a version bump of one emitter under
machinery the project already runs, instead of a bespoke drift-guard lane (which is
what TASK-C525-3 currently exists to build).

Upstream consequence worth recording on #524: if this route is taken, the frozen
contract should be a **native, versioned verdict document**, with xUnit as one
projection among several — not xUnit as the truth with everything else decoded back out
of it.

## Reframing 3: ship the autograder as a release asset, not a directory to copy

"An instructor copies a directory out of the JLS tree" creates one unversioned fork of
the kit per course, and every one of them rots — which is precisely the failure
TASK-C525-3's drift guards and AC-5's doc-tests are being built to chase. The project's
entire distribution story says otherwise: installers, checksums, SBOM, attestations,
`gh attestation verify`. Build the Gradescope zip in the release workflow, in the job
that already knows the image digest, and publish it as
`jls-gradescope-autograder-<version>.zip` with the same provenance attestation as every
other asset. The instructor downloads and uploads it; they never build, never copy,
never pin a digest by hand, and they have an upgrade path. AC-1's "building it produces
a valid autograder zip" then holds on every release rather than being asserted once.
(A jar-side `jls --init-autograder gradescope ./dir` writing the same kit from bundled
resources is the offline variant, consistent with the self-contained-jar ethos.)

## What the issue omits: the submissions are hostile input

The issue bounds *output* ("a stated limit, with truncation marked") and says nothing
about bounding the *run*. A grader executes files written by 300 people who are
being graded by it. JLS has unusually good ground to stand on here — `#38`'s caps
(`FileAbstractor.MAX_CIRCUIT_TEXT_BYTES`, 64 MiB measured against *decompressed* text,
XZ bomb–safe), `UntrustedFileHardeningTest`, the `LoadError` taxonomy instead of stack
traces, `-d` as a simulation time limit, and a `SECURITY.md` that already names student
circuit files as untrusted input. The template is where that becomes an operational
default: `--network=none`, read-only mount, an explicit `-d`, a JVM heap cap, a
wall-clock timeout, and a documented statement that a malformed submission yields a
named `LoadError`, never a hung grader. Against the community Logisim harnesses CAP-21
cites as prior art, that is the differentiator — and it is free, because the work is
already done.

## Sequencing, and the demand question

Nothing #694 consumes exists at HEAD: no verdict machinery, no xUnit surface, no exit
status 3, no lab-as-data format, no CAP-21 fixture lab, no 300-submission corpus
(`grep -ril xunit` over the tree hits only roadmap prose). It is transitively behind
#524, #369 and #466, and it names a fixture owned by #531. It cannot start.

More importantly: no instructor is named anywhere in #502, #525 or #694. The project's
recorded style is to gate speculative surface on demand — i18n declined for want of a
requesting user, #212 held shut until "a real user asks", the XML plugin loader removed
rather than maintained, LTI excluded outright. Four platform adapters at 12–17 mw,
built ahead of any named course, is the same shape as the surfaces this project has
repeatedly declined. CAP-21's own §Cost names the right move: the 2–3 mw demo slice —
a Gradescope kit over **today's** frozen three-status stdout contract. Under Reframing
2 that slice is smaller still, could ship before the freeze lands, and would put a real
instructor in the loop whose feedback should decide whether the other three adapters
are ever filed as work rather than as three more emitter classes.

## Verdict

**endorse-with-reframing.** Right goal, right exclusions, wrong three artifacts. Keep
AC-3 and AC-4; replace AC-1 with "the release workflow publishes an attested
autograder zip whose Dockerfile derives from the signed `ghcr.io/anadon/jls` digest",
and replace AC-2's adapter with a `-report gradescope` output format of JLS itself,
golden-tested in `mvn verify`. Add a resource-bounding criterion the issue currently
lacks. Ship one platform, over today's contract, and let demand decide the rest.
