# Issue #525: FEAT-C21-2: an instructor copies the in-tree Gradescope template and a JLS lab grades in the platform's native UI — per-test scores, visible/hidden split, bounded output
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The end is not `results.json`. It is: **an instructor who already has a lab gets a
working course in an afternoon, and never becomes the maintainer of grading glue.**
Gradescope is just where the largest number of those instructors already are.

That end is squarely on JLS's arc — more so than the capstone's four-platform framing
admits. The README already advertises `ghcr.io/anadon/jls` as the image "for
autograders and CI". `docs/vcd-interop.md` §4 already names "autograde over the batch
CLI" as *the supported grading pattern* and ships a runnable bridge pinned in CI. #466
is already building `-report`, a byte-deterministic `GradeReport`, exit status 3, a
rubric mapping checks to points, and `grade.py` walking a directory of submissions.
#525 is the last two inches of a road the project has been paving for a year.

Which is exactly why its *shape* is wrong. As written it proposes a second Dockerfile,
a second grading program, a copied-and-forked template, and a throughput criterion
borrowed from a sibling issue. Every one of those has an existing seam in this tree
that it declines to cut along.

## Reframing A: Gradescope is a report format, not an adapter

#466 §7.4 gives `GradeReport` two renderings (xUnit XML, plain lines) behind one flag,
with **extension dispatch** already an open question resolved toward `.xml` → xUnit,
everything else → plain (#466 OQ 3). Gradescope's `results.json` is a third rendering
of the same verdict list: per-test name, score, max score, visibility, output.

So the natural form of this feature is `jls -b -t v -check e -report results.json` —
a `GradescopeReport` renderer beside `XunitReport`, in the jar, in Java, golden-pinned
by the same `GradeReportGoldenTest` machinery, inheriting #466's P6/P7 determinism
contract for free. Not a Python shim that runs in someone else's container under
whatever Python that container happens to have.

Three consequences that are not merely tidiness:

1. **Zero new runtime dependencies in the grading container.** `resources/packaging/Dockerfile`
   builds a jlink'd runtime with modules derived by `jdeps` from the shaded jar and
   nothing else — no Python, no `jq`. An out-of-jar adapter forces the template to
   `apt install` into a digest-pinned, snapshot-pinned image, quietly forfeiting
   the reproducibility discipline that Dockerfile's header comment spends 20 lines
   establishing.
2. **Byte-determinism becomes structural.** #524 AC-4 demands "no timestamps, ordering,
   or locale nondeterminism across container boundaries." A Java renderer over a pure
   `GradeReport` is a pure function (#466 §7.10). A Python shim re-introduces exactly
   the locale/dict-ordering/float-formatting hazards the freeze is meant to eliminate,
   in the one place JLS cannot test.
3. **Four adapters collapse to four renderers.** PrairieLearn's and nbgrader's payloads
   are the same projection with different field names. If they are renderers, the
   four-way parity claim of #502 AC-1 is a theorem about one function's outputs rather
   than an empirical comparison of four programs.

The counter-argument — vendor formats move on vendor schedules, and a jar rendering
needs a JLS release to follow — is real but weak here: the template pins a digest
anyway, so a live course is frozen regardless. Keep the escape hatch honest by having
the plain/JSON canonical rendering remain emittable, so a 20-line shim can chase a spec
change before the next release.

## Reframing B: the rubric belongs to the lab, not to the adapter

"Per-test scores, visible/hidden split, bounded output" are three pieces of *rubric*,
and #525 implicitly puts them in Gradescope-specific configuration. #466 already puts
a rubric in the lab (`examples/autograde/lab-01/`: "a rubric mapping checks to points").
Extend that, in platform-neutral vocabulary — `points:`, `visibility: student | staff |
after-due` — and every renderer becomes configuration-free.

If instead each adapter owns its own rubric mapping, the same failure mode the pass-1
deduplication comment on this very issue warns about for corpora arrives for rubrics:
four rubrics, four sets of points, and the byte-identity claim silently becomes four
separate claims. This is the single highest-leverage edit available to #525, and it is
a change to the *lab format*, not to this issue's code.

**"Bounded output" likewise belongs upstream.** A counterexample-bearing verdict list
over 300 vectors can be megabytes. Truncation policy — how many mismatches, what marker,
where — must live in `GradeReport` so all renderings truncate identically; otherwise
each adapter truncates its own way and parity dies on the first verbose failure.

## Reframing C: generate the kit, don't ship a template to be copied

"An instructor copies the in-tree template" is the weakest sentence in the issue.
Copied templates fork on contact: every course diverges, and no fix ever reaches the
population. This project has already decided against that pattern everywhere else —
`scripts/build-installer.sh` is "the single recipe used both locally and by CI"
(README), and `scripts/wayland-rig.sh`, `x11-rig.sh`, `macos-rig.sh`,
`icestick-handoff.sh` are all one-script-serves-both.

Ship `scripts/make-autograder.sh lab/ -o autograder.zip` instead. Then:

- AC-4 ("the template README executes as scripted doc-test steps in CI") stops being a
  literate-programming exercise and becomes "CI runs the same command the instructor
  runs" — which is the project's actual idiom, and strictly stronger evidence.
- The failure-classification promise gets a real mechanism. #525 claims the kit
  "degrades with a named error, never a silent misgrade, if the spec moves," but names
  nothing that proves it. Every rig in this tree proves exactly that kind of claim with
  a companion selftest driving the *unmodified* script against a stub toolchain and
  asserting documented exit codes (`wayland-rig-selftest.sh`, `x11-rig-selftest.sh`,
  `macos-rig-selftest.sh`, `icestick-handoff-selftest.sh`). `make-autograder-selftest.sh`
  — malformed rubric, missing lab file, unreachable image digest, schema mismatch, each
  with its own exit code — is what turns AC-5 from an aspiration into a test.
- Instructors get fixes by bumping a version, not by re-copying files they have edited.

## Reframing D: the Dockerfile already exists — derive, don't rebuild

`resources/packaging/Dockerfile` publishes `ghcr.io/anadon/jls`: multi-arch
(amd64/arm64/riscv64), base image digest-pinned, apt pinned to a `snapshot.ubuntu.com`
state, `SOURCE_DATE_EPOCH`-clamped, jlink-trimmed, cosign-signed with a provenance
attestation, and headless by construction. "A Dockerfile pinning a headless-JRE JLS
build" describes a *second* recipe that will drift from that one on base image, JDK,
snapshot date, and fonts.

The template's Dockerfile should be three lines: `FROM ghcr.io/anadon/jls@sha256:…`,
`COPY run_autograder /autograder/run_autograder`, `ENTRYPOINT []` (the published image's
entrypoint is `java -jar`, which Gradescope's harness does not want). Then a course is
pinned to a *signed, attested* JLS by digest — a verification story no bespoke template
Dockerfile can offer, and a genuinely differentiating claim: "your autograder image is
cryptographically traceable to a JLS release commit."

## Disregarding AC-1 and AC-2 as written

**AC-1's 300-submission corpus is not this issue's outcome.** The adversarial pass
found the missing dependency edge to #531; the better fix is to delete the claim rather
than add the edge. #525's outcome is one instructor, one lab, zero to graded. Corpus
throughput and cross-platform parity are #531's whole reason to exist.

**AC-2 measures the wrong quantity.** Gradescope runs *one submission per container
invocation*. Nothing an instructor experiences is ever "300 submissions in one
containerized run"; what they hit is the per-submission ceiling — cold container start,
plus JVM start, plus simulation — and a 300-submission batch time tells you nothing
about it. Replace with: **p95 wall time from container start to `results.json` for a
single submission**, measured in the shape the platform actually runs, and stated
against Gradescope's documented per-submission limit rather than a self-selected budget.

If a fast full-corpus number *is* wanted (for #531's lane), the right way to get it is
not a shell loop over 300 JVM starts: push the directory walk into the CLI so one
process grades a corpus. One JVM, one locale, one ordering — cheaper *and* more
deterministic, and it retires #466's `grade.py` parallelism question (OQ 6) as a
side effect.

## On the four-platform commitment itself

Every other speculative surface in this tree carries a demand gate and a recorded
revisit trigger: i18n declined for want of "a requesting user"; external element
providers (#212) staged behind a demand gate; out-of-process plugin isolation
"not built speculatively"; co-simulation rejected outright (#63). Four proprietary
vendor integrations arrive with no named instructor, no course, and no demand gate —
the largest non-JLS surface proposed in the repository, in a tree already carrying a
1,145-line `ci.yml`, four platform rigs, Agda proofs and multi-arch release plumbing.

#525 is the right one to build, and possibly the only one: largest install base, and
the image it needs already ships. I would gate #526/#528/#530 on a real instructor on
a real course, exactly as #212 is gated, and let the canonical report rendering plus a
documented reference renderer serve everyone else. KC-21-3 already contemplates
shipping three platforms rather than four; the same reasoning reaches one plus a
contract.

## What I endorse without change

- Documented spec only, no scraping, no undocumented endpoints. Correct posture for a
  proprietary target, and consistent with the project's subprocess-boundary stance.
- Dedicated CI lane, not the core matrix.
- Files-only, no LTI, no JLS-operated service. This is what keeps the kit survivable by
  one maintainer, and it is the constraint that makes everything above cheap.

## If this issue is rewritten

Roughly: (1) a `GradescopeReport` rendering of #466's `GradeReport`, golden-pinned;
(2) rubric fields — points, visibility, output bound — added to the lab-as-data format,
not to adapter config; (3) `scripts/make-autograder.sh` plus its selftest, replacing
the copied template and the README doc-test; (4) a three-line derived Dockerfile
`FROM` the signed published image by digest; (5) per-submission wall-time evidence
against Gradescope's documented ceiling. Corpus and parity move to #531. That is a
1–2 mw issue with a stronger claim than the 2–3 mw one filed, and it leaves three
sibling adapters as near-zero-cost renderings rather than three more integrations to
own forever.
