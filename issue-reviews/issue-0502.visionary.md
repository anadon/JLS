# Issue #502: CAP-21: one in-tree kit makes the same JLS lab autograde unchanged on Gradescope, GitHub Classroom, PrairieLearn and nbgrader — with byte-identical scores on all four
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the six planned features away and the intent is one sentence: *a JLS lab
should be gradeable on the platform a course already runs, without the
instructor writing a harness.* That end is squarely on the project's arc.
`README.md:103-114` already advertises `ghcr.io/anadon/jls` as the headless
surface "for autograders and CI"; `docs/batch-interface.md` is a normative
stability contract written for exactly this consumer; `examples/autograde/`
plus `test/jls/AutogradeBridgeExampleTest.java` already demonstrate the
subprocess-and-inspect grading pattern in CI. I endorse the end without
reservation.

I do not endorse the shape. The issue converts "reach four platforms" into
"own four vendor-facing integrations, a second contract-freeze institution, and
a four-container CI corpus" — 12–17 mw of permanently-maintained surface at bus
factor 1, defended by a headline claim (byte-identical scores) that the issue's
own KC-21-1 expects to die. Every one of those six deliverables has a cheaper
in-tree analogue that the issue never considers, and one of them rests on a
factual error about the repository.

## The factual error at the base of PF-1

Open Question 2 asks whether the CLI contract would be "the first formally
frozen public interface of JLS," recommends "yes," and declares that question
blocking on PF-1's filing. The answer is no, and it is no in writing.
`docs/batch-interface.md` §6 ("Stability promise") already freezes the `-t`
grammar (§2), the stdout report (§3) and the VCD profile (§4), and already
carries the versioning policy PF-1 proposes to invent: *"a CHANGELOG entry, and
a major version bump, or a compatibility flag that keeps the format specified
here available unchanged."* `docs/file-format.md` is a second normative frozen
interface. The conformance suite also exists in substance —
`CliFlagTableTest`, `CliSmokeTest`, `BatchSimulationGoldenTest.
watchedElementsPrintInNameOrder`, `VcdExportGoldenTest` — and §5 of the same
document names each test as the oracle for each frozen section.

So PF-1's genuine content is much smaller than 2–3 mw of new institution: §1
(invocation, streams, exit statuses, artifact paths) is the one section §6 does
*not* freeze, and the verdict envelope CAP-06 adds is the one artifact not yet
covered. Shipping PF-1 as a separate "frozen CLI contract" with its own semver
policy and its own ratchet creates a *second* freeze regime beside §6 — two
documents, two policies, two ratchets, one CLI. That is a governance fork, and
risk 3 and KC-21-4 are both symptoms of it.

**Alternative 1 — dissolve PF-1 into the document that already freezes.**
Extend `batch-interface.md` §6's frozen set to include §1 and a new §7 (the
verdict/xUnit artifact), add the exit-status-3 row to §1's table when CAP-06
lands it, and let the existing golden tests be the conformance suite. That is a
documentation change plus a handful of assertions, and it removes Open
Question 1, Open Question 2, risk 3 and KC-21-4 simultaneously — you cannot get
a freeze/roadmap deadlock between two capstones if the freeze happens inside
the issue that ships the format. The right seam is: *a format is frozen by the
issue that ships it, in the document that already holds the freeze policy.*
CAP-21 then consumes a contract instead of manufacturing one.

## The adapters are the wrong unit of delivery

All four platforms want the same thing in four wrappers: a list of
(test name, pass/fail, points, message). Gradescope wants `results.json`;
PrairieLearn wants its own JSON; Classroom wants an exit code plus points;
nbgrader wants a nonzero exit per hidden cell. Each is a pure function of the
xUnit XML CAP-06 emits, and each is on the order of fifty lines.

The issue packages those four functions as four *kits* — a Dockerfile, a
setup script, a starter repo, a marketplace Action, an external-grader image, a
notebook unit — and then needs PF-6, a 300-submission corpus run in four
containers in CI, to prove the four agree. But they agree by construction if
they are one code path.

**Alternative 2 — put the writers in the jar, not in four templates.**
`jls -b --report gradescope out.json` (and `prairielearn`, `xunit`, `text`)
makes every adapter a `report`-package writer beside the existing VCD emitter,
pinned by byte-exact goldens in `test/jls/` exactly like `VcdExportGoldenTest`
pins §4. Consequences:

- PF-2..PF-5 stop being deliverables and become README recipes. The Gradescope
  "template" reduces to `FROM ghcr.io/anadon/jls` plus a five-line
  `run_autograder` — because the signed, multi-arch, attested headless image
  the template would otherwise build *already ships* (`README.md:103-124`).
- PF-6 stops being a hermetic four-container corpus fixture and becomes a
  golden test. AC-1's byte-parity becomes a tautology checked at build time
  rather than an experiment run against four vendor runtimes.
- Platform drift (risk 1) hits fifty lines of Java with a golden, not a
  container image with a doc-test lane. KC-21-3's "drop the adapter" becomes
  "drop a `--report` value," which is a CHANGELOG line rather than a
  documentation excision.
- The "no adapter opens an interactive session" property of AC-4 is enforced
  structurally — a writer over `BatchSimulator` output cannot open a session —
  rather than by an instrumented build.

The residual objection is that platform glue must live *somewhere* the platform
can read. Yes: in `docs/`, as recipes, in the same register as
`docs/vcd-interop.md`, which already does this job for GTKWave/Surfer and for
the autograde bridge. Documented recipes rot loudly (a doc-test can still run
one of them); an owned Docker template rots silently and holds a semester
hostage.

**Alternative 2b (Open Question 3 dissolved).** Do not publish a Marketplace
Action under any org. `uses: anadon/JLS/.github/actions/grade@v5.1.0` resolves
from this repository against the release tags JLS already publishes. No
marketplace listing, no org custody question, no separate version namespace,
and the pin is the same artifact the checksums and attestations already cover.

## The headline claim is the wrong one to chase

"Byte-identical scores on all four platforms" is chosen because it is
falsifiable and quotable. It is also fragile in exactly the way KC-21-1 admits,
and it buys an instructor nothing they asked for — nobody grading on Gradescope
compares their gradebook byte-wise against a PrairieLearn instance they do not
run. The four-way diff is an artifact of the deliverable shape (four
independent adapters) and disappears under Alternative 2.

**Alternative 3 — grade *provenance*, not grade portability.** JLS already runs
an unusually strong reproducibility discipline: byte-reproducible jar and BOM,
per-release `.buildinfo`, cosign-signed images, `gh attestation verify`,
byte-identical simulation goldens. The distinctive, durable thing this project
can offer autograding — which no teaching-simulator ecosystem offers — is a
**re-verifiable grade receipt**: the verdict artifact carries the JLS version,
a hash of the lab spec, a hash of the submission, and a digest over the verdict
set, such that a student, a TA, or a regrade appeal can re-run
`jls -b --report ...` anywhere and get the same digest. That claim does not
depend on any vendor's spec holding still, survives all of KC-21-1's failure
modes, extends a discipline the project already pays for, and answers a real
course problem (grade disputes, regrades, lost autograder state) that four-way
parity does not touch. Platform agreement then follows as a corollary of one
writer set plus one digest, and AC-1 can be retired rather than weakened.

## Alignment, duplication, and one thing genuinely missing

- **Duplication.** PF-1 partly re-owns CAP-06's report channel (FEAT-053 #369,
  TASK-0111 #466); the issue concedes the ordering exists while recording
  `blocked_by: []`. Alternative 1 removes the overlap rather than sequencing it.
- **Pull against recorded instinct.** `ARCHITECTURE.md` declines i18n as "a
  large, ongoing tax with no requesting user," with the revisit trigger *"a
  concrete request from an instructor or course."* #212's external providers are
  gated on "a real user asks"; #80 removed a plugin loader that shipped for
  years because nobody could reach it. CAP-21 proposes four vendor integrations
  with a permanent drift tax and names no requesting instructor for any of the
  four. The project's own governance instrument applies: ship platform one on
  demand-free grounds (it is nearly free under Alternative 2), and demand-gate
  platforms two through four on an instructor asking. That is not a weaker
  outcome; it is the same outcome ordered by evidence.
- **Citation to a non-normative source.** KC-21-2 calls #498 §7.2's
  recording-not-session clause "a permanent normative constraint." #498 states
  in its own header that the document is *"explicitly non-normative… Nothing in
  it may be cited as settled policy,"* and §7.2 is a *proposal to amend* the
  live text, arguing the current wording is unfaithful to #63. The normative
  sentence actually in the tree is `docs/vcd-interop.md:18-23`. The constraint
  is right; the authority is wrong, and §7.2 explicitly warns against a sweep
  hardening the wording into permanence — which is what KC-21-2 does. Cite
  `vcd-interop.md`, and leave the amendment to the issue that owns it.
- **Missing, and more valuable than a fourth adapter.** A grading kit is the
  first place JLS runs 300 untrusted `.jls` files unattended.
  `UntrustedFileHardeningTest` and #38 cover parse-side hostility, and `-d`
  bounds simulated time, but nothing bounds wall clock, heap, or output volume
  for a pathological submission — and `SECURITY.md:55-56` puts resource
  exhaustion from a hostile circuit file in scope. A documented
  per-submission resource envelope (timeout, memory cap, output cap, named
  failure rather than a hung container) is the load-bearing safety asset of any
  autograding story and appears nowhere in the six PFs.

## Summary

I am disregarding the stated acceptance criteria, and specifically AC-1's
four-way byte-parity headline and PF-1's separate freeze regime. The outcome —
JLS grading on the platforms courses actually run — is worth pursuing and is
cheaper than the issue believes. Re-cut it as: freeze §1 and the verdict
envelope inside `docs/batch-interface.md` §6 as part of CAP-06; add report
writers to the jar with byte-exact goldens; ship recipes over the existing
signed container image instead of four owned kits; make the verdict a
re-verifiable receipt; demand-gate platforms beyond the first; and add the
resource envelope nobody has written. That is a small fraction of 12–17 mw, it
adds no institution the project does not already have, and it leaves nothing
behind that a vendor's release note can break mid-semester.
