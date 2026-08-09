# Issue #701: TASK-C526-1: the jls-grade Action runs a pinned, cached JLS build against a lab's hidden vectors and reports Classroom points
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the machinery and the end is: *an instructor puts a JLS lab in a git repo and
student pushes grade themselves, with no bespoke harness and no JLS-operated service.*
That end is squarely on the project's arc. JLS already ships every hard prerequisite:
a documented batch contract (`docs/batch-interface.md`, #72/#42), a headless core the
`HeadlessCoreRatchetTest` keeps AWT-free (#77), a digest-addressable multi-arch
container (`resources/packaging/Dockerfile`, cosign-signed, `release.yml` emits
`steps.digest.outputs.digest`), and a worked subprocess grading example
(`examples/autograde/autograde.py`). Nothing here asks JLS to become a service, and
the files-only shape honors #498 §8 exclusion 7. I endorse the outcome.

What I would change is where the seams are cut. Three of them are in the wrong place,
and one of the three makes CAP-21's headline claim structurally unprovable.

## 1. The scoring function is unowned — and it is the whole parity claim

AC-4 here says the score summary must be "in the shared form #531's parity test
consumes, with no post-hoc normalization." AC-1 on #502 says per-student score vectors
must be byte-identical across four adapters. But trace the chain: #369 freezes the
verdict layer with invariant 3, **"No single score… nothing in this feature emits a
bare percentage."** #524 freezes invocation, exit codes, artifact paths and an *xUnit
schema*. xUnit carries pass/fail, not points. So the mapping *verdicts → points* — test
weights, partial credit, rounding, how UNRUN scores — is owned by no issue in the
stack, and by construction four adapters will each implement it. Byte-identity then
becomes a property maintained by vigilance across four codebases, and #531's
300-submission fixture is not a proof but a tripwire that fires after divergence.

**Reframing: adapters must be format transducers with zero arithmetic.** Put point
weights in the CAP-06 lab-as-data file (they are a property of the *lab*, not of
Gradescope), and have JLS itself emit the canonical score vector — one normative,
versioned artifact alongside the xUnit XML, keyed by stable test id. Then #701's Action
does no computation: it reads the score vector and re-renders it in Classroom's shape.
AC-1 becomes true *by construction* rather than by fixture, KC-21-1 ("two platforms
cannot be made byte-identical") collapses to "one platform's UI rounds our number,
which we detect at the render boundary", and #531 drops from load-bearing apparatus to
a cheap regression lane. Enforce it the way this project already enforces things: a
ratchet test asserting no adapter source file contains arithmetic on scores — the same
move as `NotificationRatchetTest` and `HeadlessCoreRatchetTest`.

This is the one change I would make before #701 is implementable, and it belongs on
#524, not here. As written, #701's AC-4 ("no post-hoc normalization") is a promise the
Action cannot keep alone, because nothing upstream hands it a number.

## 2. The annotation anchor forces a decision this task's sibling cannot make

Annotations are TASK-C526-2's boundary, but they determine #701's shape and nobody has
priced them. GitHub check annotations anchor to *path + line*. A `.jls` file is XZ data
by default (README "Circuit files"), so line numbers are meaningless — annotations
degrade to file-level, i.e. "something in ALU.jls is wrong", which is what a student
already knew. Two consequences the issue set never states:

- **The starter template must mandate plain-text saves** (`-savetext`, or the Save As
  file-type choice, #129). That is a template *requirement*, decided here, in #701's
  neighborhood — and it is a happy alignment: plain-text `.jls` is the format that
  diffs in git, which is the entire premise of a Classroom workflow. The starter repo
  should probably enforce it with a CI check, not a README sentence.
- **The locator belongs in the frozen contract, not in the Action.** JLS knows which
  element disagreed; the Action would have to reverse-engineer a line from an element
  name by re-parsing the circuit. Every adapter needs the same locator (Gradescope's
  per-test output, PrairieLearn's feedback, the annotation). So #524's schema should
  carry a source locator per failing verdict — element id always, line/column when the
  circuit was loaded from plain text. Push that upstream now; retrofitting a locator
  after a *frozen* schema ships is exactly the compatibility event #502 risk 3 warns
  about.

## 3. "An Action exists in tree" may be the wrong artifact entirely

GitHub Classroom's autograding is itself implemented as reusable actions
(`classroom-resources/autograding-command-grader` + the grading reporter): per-test
steps run a command, exit status decides the test, the reporter aggregates points.
Against today's shipped three-status contract, one `docker run
ghcr.io/anadon/jls@sha256:… -b -t vector_k.txt circuit.jls` per hidden vector already
produces per-test Classroom points with **zero JLS-authored Action code**. That route
deletes: marketplace publication (Open Question 3, which "blocks shipping"), an Action
release/versioning surface a single-maintainer Java repo would carry forever, and most
of CAP-21 risk 1 (runner-image drift lands on GitHub's maintained actions, not ours).

The one thing it cannot do is per-file annotations — which means **annotations are the
sole justification for owning code here**, and that should be stated as such. My
recommendation: build the composite/YAML route first as the shipped default, and let
the annotation requirement, if it survives contact with §2 above, promote it to a thin
*composite* action — never a JS or Docker action. Prove the bespoke Action is necessary
before writing it; the issue currently assumes it.

## 4. The caching criterion optimizes a non-problem and adds a misgrade path

AC-1 requires caching "so a warm run does not re-download the build." The image is
ubuntu:26.04 + a jlink'd ~50 MB runtime + fonts + the jar; a ghcr pull from a
GitHub-hosted runner is seconds, and `actions/cache` restore of a docker save tarball
is frequently *slower* than the pull it replaces. Worse, an incorrectly keyed cache
restores the wrong build — a silent misgrade, precisely the failure mode CAP-21 forbids
everywhere else. If caching stays: key the cache on the digest itself so a wrong-build
restore is impossible, and make the criterion conditional on a *measured* cold-run
budget rather than asserted a priori. Consider also that the jar path (runner's
preinstalled JDK + a ~10 MB release jar verified by `SHA256SUMS`) may beat both.

## 5. Digest pinning has an expiry problem worth naming

A digest frozen into a template that instructors copy means a course runs a fossil
JLS forever — including past security fixes to a loader that, in this deployment,
parses **untrusted student input on institutional infrastructure**. This Action is the
first place JLS deliberately executes hostile-capable circuit files at automation scale
(cf. #38's caps, `UntrustedFileHardeningTest`, SECURITY.md). Reproducibility of a
course *offering* and compatibility of the *contract* are different needs: pin the
digest for the semester, but also assert the CLI contract version (#524 AC-5 makes it
queryable) and fail with a named error on mismatch, and give the template a documented,
Dependabot-visible refresh path. Otherwise "pinned and reproducible" quietly means
"unpatchable."

## 6. Ordering: this should be the demo slice, not the tail of the stack

#701 sits behind #524, which sits behind #369 + #466, and #369 is `blocked_by` #316
(the 4k-line `SimpleEditor` decomposition), #321 (Yosys JSON writer) and #347. A
workflow file that shells out to a container does not need an editor decomposition or
a netlist writer. #502 already offers a 2–3 mw demo slice and assigns it to Gradescope;
**assign it here instead.** The Classroom adapter is the one whose infrastructure
already exists in tree (published digest-addressable image, attestation, release
plumbing), the one that needs no new container artifact, and the one that runs today on
the shipped three-status contract with per-vector granularity approximated by one
invocation per vector. Shipping it first gets an instructor a working lab this
semester and — more valuable — generates the real requirements (score vector, source
locator, template constraints) that #524 should freeze. Freezing a public contract
before any consumer has taught you what it needs is the inversion I would most want
avoided here.

## Disposition

Endorse the outcome; reframe the work. Concretely, before implementation: (a) move the
verdict→points mapping into the lab data and a JLS-emitted score vector, with an
adapters-do-no-arithmetic ratchet; (b) push the source locator and the plain-text
starter-template requirement into #524/#526 rather than discovering them in
TASK-C526-2; (c) require evidence that a bespoke Action beats
`autograding-command-grader` + a digest-pinned `docker run` before writing one;
(d) make caching conditional on measurement and digest-keyed; (e) re-target CAP-21's
demo slice to this adapter and let it inform the freeze.
