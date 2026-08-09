# Issue #528: FEAT-C21-4: a PrairieLearn question grades a JLS lab through the in-tree external-grader image, driving batch mode only
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Strip the CAP-21 scaffolding and the want is small and good: **an instructor at a
PrairieLearn school can assign a JLS lab and have it graded.** Everything else in the
issue — the in-tree image, the byte-identity clause, the dedicated lane — is machinery
chosen to deliver that, not the want itself. Judged against the project's arc it is
plainly aligned: `docs/batch-interface.md` is already declared a stability contract,
`docs/vcd-interop.md` §4 already blesses "plain subprocess bridge" as *the* supported
grading pattern and forbids co-simulation (#63), and `examples/autograde/autograde.py`
is the existing one-platform-less proof. #528 is the next honest step on that line.

But the *shape* CAP-21 hands this issue is cut along the wrong seam three times over,
and PrairieLearn — uniquely among the four platforms — is where those three miscuts are
both most visible and most fixable.

## Miscut 1: four adapters instead of one emitter seam

The dedup note on #525 defends the four adapters as four outcomes because each emits a
different native format. True — and irrelevant to how the code should be cut. JLS
already has a first-class idiom for "same computation, several output formats": `-i
out.png|out.jpg|out.svg`, `-export out.v`, `-vcd out.vcd`. One engine, format selected
at the boundary, goldens per format. CAP-21 instead proposes four out-of-tree scripting
kits, four Dockerfiles, four CI lanes, four doc-tests, and then a fifth issue (#531) to
prove the four agree — a parity obligation that exists *only because* four
implementations were created.

The alternative: one **canonical score vector** — a pure function from the frozen xUnit
artifact of #524 to `(test id, points, max points, message, attachment)` — with four
thin *projections* over it. Parity then stops being a 300-submission container
experiment and becomes an ordinary JUnit golden test on a pure function; the containers
are left proving only what containers can prove (that the image runs). AC-2 here
("byte-identical to the other adapters' vectors") is the tell: it is a claim about JSON
float formatting across four languages, which is exactly the property a shared emitter
makes true by construction and four hand-written adapters make true by luck and CI.

## Miscut 2: an in-tree image duplicates the project's most expensive machinery

`area:distribution` on this issue is the warning label. JLS already publishes
`ghcr.io/anadon/jls` — headless by construction, multi-arch amd64/arm64/riscv64,
digest-pinned base, apt-snapshot-pinned, `SOURCE_DATE_EPOCH`-clamped, cosign-signed,
provenance-attested (`resources/packaging/Dockerfile`, `scripts/build-container.sh`,
README "Container image"). "The image pins a specific headless JLS build" describes a
thing that already exists and is called a version tag.

A second published image inherits every one of those obligations — signing identity,
attestation, three architectures, reproducibility lane, a release-workflow job, staleness
relative to `jls` releases — for the sake of a `/grade/run` script. PrairieLearn's
`externalGradingOptions` takes an arbitrary `image` **and** an `entrypoint`, and the
question's `tests/` tree is mounted into `/grade`. So the honest deliverable is *data*,
not an artifact: `info.json` naming `ghcr.io/anadon/jls:<version>`, a run script, hidden
vectors, and — if the results emitter needs an interpreter the base image lacks — a
four-line course-owned `Dockerfile` printed in the README. Zero new registries, zero new
signing surface, and the kit automatically tracks JLS releases instead of pinning one.

If the emitter cannot be written against what the base image already carries (sh,
coreutils, no python3, no jq), that is an argument for making JLS itself able to write
the platform result file — not for JLS operating a second image.

## Miscut 3: hermetic pinning cannot detect the drift AC-5 promises

AC-5 wants "externalGrader contract drift surfaces as a red lane." #531 makes the fixture
hermetic, account-free, network-free, with "each platform's documented contract pinned in
the fixture." A pinned local copy of someone else's prose changes only when *you* change
it. That lane is green forever, including the semester PrairieLearn changes the contract.
The criterion as written is unsatisfiable under the apparatus assigned to satisfy it.

And here is the reframe that only PrairieLearn (and nbgrader) admits: **it is the one
adapter whose platform you can actually run.** PrairieLearn is open source and ships its
own container images; Gradescope (Turnitin) and GitHub Classroom cannot be run in JLS's
CI at any price. So this adapter should not imitate the hermetic-prose-pin pattern the
closed platforms force — it should run a **version-pinned real PrairieLearn grader**
against the fixture question, and a separate scheduled `contract-watch` job (network at
pull time only, cached digest) should bump that pin and go red when the real behavior
moves. One such job serves all four platforms as the *only* real drift oracle the kit
will ever have. That also argues this should be CAP-21's **first** adapter and its demo
slice, not the third: it is the only one that can falsify the delivery shape against
reality before three siblings are built on the assumption.

## Disregarded acceptance criteria, and why

- **AC-2 (byte-identical to the other adapters' vectors)** — I am disregarding this as a
  goal for *this* issue. Byte-identity across platforms is the least interesting property
  the kit can have and the most expensive to maintain, and worse, it is a *ceiling*: it
  pushes every adapter toward the least-capable common output. PrairieLearn's results
  format accepts per-test HTML output and file attachments. JLS can already export SVG
  and PNG circuit images (`-i out.svg`) and VCD waveforms. The distinctive thing — the
  thing no teaching-simulator ecosystem currently offers, and a far better banner than
  matching floats four ways — is **a failing test that shows the student the waveform and
  the circuit at the failing edge**, which is exactly the counterexample-rendering
  direction `docs/capability-roadmap/lf-04-formal-and-grading.md` is already headed
  toward. Assert score *equivalence* at the canonical-vector level (a unit test);
  let each adapter present as richly as its platform allows.
- **AC-3 (the image pins a specific headless JLS build)** — disregarded per miscut 2.
  Replace with: "the question kit references a published `ghcr.io/anadon/jls` tag and
  adds no new published artifact."

## What to keep, unchanged

- Batch-artifacts-only, never an interactive session (AC-1 / KC-21-2 / #63). This is a
  standing normative constraint of the project, not a per-adapter choice, and #528 states
  it correctly.
- Documented-contract-only, degrade with a named error. Same discipline as #525.
- Scripted, CI-executed README (AC-4). The doc-test is the part most likely to still be
  earning its keep in five years.
- Static questions now; randomized generators deferred to a real `jls.api`. Correct call —
  a randomized generator needs a programmatic surface, and faking one with CLI string
  assembly would be the worst possible first customer for that API.

## Bottom line

The destination is right and on the project's arc. The route is four parallel kits, a
second published image, and a parity obligation manufactured by the duplication itself.
Cut instead at the emitter seam JLS already uses for every other output format, ship this
adapter as data over the existing image, make it adapter #1, and spend the saved effort on
the feedback quality that only JLS can give a PrairieLearn student.
