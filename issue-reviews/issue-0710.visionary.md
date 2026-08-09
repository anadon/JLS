# Issue #710: TASK-C528-3: externalGrader contract drift turns the PrairieLearn lane red, and the question-kit README runs as CI doc-test steps
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the PrairieLearn specifics, #710 wants two guarantees that the whole
CAP-21 arc (#502) rests on: *a grading integration cannot rot silently*, and *when
it does break, the red lane says whose fault it is*. Both are right, both are in
the project's grain — JLS already refuses to let a doc outlive the behavior it
describes (`CliFlagTableTest`, `HelpTopicsTest`, the byte-identical goldens, the
`.buildinfo` reproducibility recipe). The end is not in question.

The route is. As written, #710 is the third of four near-verbatim clones — #699
(Gradescope), #705 (Classroom), #710 (PrairieLearn), #715 (nbgrader) — each
filing "a dedicated lane, a pinned platform contract, README-as-doc-test, and a
three-way failure classifier" as its own 0.5–1 mw of bespoke machinery. Four
copies of one mechanism, in a single-maintainer project whose README already
carries four installer families, three GUI rigs and six workflows. That is the
first thing to fix, and it is fixable without giving up a single stated outcome.

## The contradiction at the center: hermetic lanes cannot see drift

AC-1 and the title promise that *externalGrader contract drift* turns the lane
red. AC-3 requires the lane be hermetic — no PrairieLearn account, no network
call. Those cannot both hold. A hermetic lane compares the adapter's output
against a snapshot **we** vendored; it goes red when *JLS* changes, never when
*PrairieLearn* changes. The failure mode CAP-21 risk 1 actually names — "not as a
broken course mid-semester" — is precisely upstream moving while our snapshot
sits still, and this lane is blind to it by construction. AC-4's three categories
collapse to two observables for the same reason: with no view of upstream, "contract
drift" and "JLS-side contract change" are the same event seen from different ends.

This is not a nitpick about wording; it is a missing lane. The project already
knows the shape of the answer: `gui-wayland` runs per-push *and* on a nightly
cron; #735/#738 put a scheduled, non-required perf lane in front of published
numbers so "a red lane names which published number is now a lie." The same split
applies here:

- **Conformance (hermetic, per-PR, blocking).** Emitted results validate against
  `contract.lock` — a vendored copy of PrairieLearn's documented schema plus its
  source URL, fetch date and sha256. This is #710's AC-1 and AC-3, honestly labeled.
- **Drift watch (scheduled, network-allowed, advisory, never a required check).**
  Re-fetch each `contract.lock`'s upstream URL, diff the hash, and on change open
  or update an issue naming the platform. This is the only mechanism that can
  deliver the title's promise, and it belongs once at the CAP-21 level, not four
  times per adapter.

**I am explicitly disregarding AC-1's "drift" framing and AC-4's three-way split
as stated**, because a hermetic lane can only assert conformance to a snapshot.
Keep the words honest and the lane becomes buildable.

## Reframe 1: one adapter harness, four registrations

Cut the seam across platforms rather than down each one. An `adapters/<platform>/`
registry where each directory contributes exactly four files — `contract.lock`,
the mapping, `walkthrough.sh`, and a generated `README.md` — plus one
`scripts/adapter-lane.sh <platform>` that validates, executes and classifies.
Adding PrairieLearn then *is* adding a directory. #710 drops from 0.5 mw of new
machinery to a registration plus its fixtures; #705 and #715 do the same; the
harness itself gets built once, in #699 (Gradescope first: largest install base,
already ordered earliest). Four platforms, one classifier to keep correct, one
place where a fifth platform costs an afternoon instead of a task.

Note also that #531 AC-2 and AC-5 already own "each platform's documented contract
is pinned in the fixture" and "the adapter lanes are dedicated CI lanes, not
entries in the core matrix" — the same ground #710's AC-1 and AC-3 claim, with no
ordering edge recorded between them. Either #710 consumes #531's fixture or #531
consumes #710's lane; today both file it and neither cites the other.

## Reframe 2: generate the README, do not execute it

"The README's steps execute as doc-tests" keeps two artifacts in sync by parsing
one of them. Invert it: `walkthrough.sh` is the single source of truth, CI runs
it, and the README is *rendered* from it — commands and captured output — with
byte-identity asserted the same way the jar and `bom.json` are. The whole class of
"the README drifted" and "an undocumented manual step crept in" stops existing,
rather than being detected. This is JLS's native idiom (goldens, `.buildinfo`,
`CliFlagTableTest`), and it removes the fenced-block extraction machinery that
would otherwise be written four times.

## Reframe 3: make the classifier a rig with a selftest

AC-4 asks the failure output to name the culprit but gives no way to verify it
does. The project has already solved this exact problem three times:
`scripts/wayland-rig-selftest.sh`, `macos-rig-selftest.sh`,
`icestick-handoff-selftest.sh` — each drives the unmodified rig against a stub
toolchain and asserts every scenario lands on its documented exit code. The
classifier here needs the same: seed a JLS-side contract violation, an adapter
mapping fault, and a `contract.lock` mismatch, and assert three distinct named
exits. Without that, AC-4 is a wish; with it, AC-4 is a test — and it is the
cheapest acceptance criterion on the list once the harness is shared.

## Reframe 4 (larger, worth a REPLAN comment on #502)

The boldest simplification is upstream of all four adapter tasks: make each
platform's report an **output format of the frozen CLI** (`--report=prairielearn`)
rather than a per-platform mapping script. The image collapses to the existing
`ghcr.io/anadon/jls` plus a ~20-line entrypoint that satisfies PrairieLearn's file
placement; the mapping becomes Java under `test/jls/` with ordinary goldens; an
"adapter mapping fault" becomes a unit-test failure with a stack trace instead of
a container diff. Most importantly, CAP-21's four-way byte-identical parity (AC-1,
KC-21-1) stops being a 300-submission × 4-container proof and becomes true by
construction — one emitter, four renderings of the same verdict object. It stays
strictly files-only, so #498 §8 exclusion 7 is untouched. If this lands, #710's
lane shrinks to schema validation plus the generated walkthrough, and #531's
corpus run becomes a regression net rather than the load-bearing evidence.

## Where this pulls with the project, and where against

With: doc-cannot-rot discipline, hermetic CI, files-not-services, named errors
over silence. Against: the per-adapter framing quietly commits a one-maintainer
project to four vendor contracts guarded four ways, which is exactly the tax
KC-21-3 was written to bound. The reframes above keep every outcome #710 states
while making the fifth platform nearly free and the real drift — the kind that
breaks a course in week nine — actually visible.

## Recommendation

Endorse the outcome; rewrite the route. Build the shared harness in #699; reduce
#710 to registering `adapters/prairielearn/` (contract.lock, entrypoint mapping,
walkthrough, fixtures); split the honest hermetic conformance gate from a single
scheduled upstream drift-watch lane owned at #502; generate the README instead of
parsing it; add a rig selftest for the classifier; record the ordering edge to
#531 and #690 in both directions.
