# Issue #721: TASK-C531-3: the whole four-way fixture runs containerized in CI with no platform account and no call to any platform service
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Context

#721 (`TASK-C531-3`) is one of four sibling tasks decomposing `FEAT-C21-6`
(#531), which itself serves `CAP-21` (#502) — a capstone proposing an
in-tree kit that autogrades JLS labs on Gradescope, GitHub Classroom,
PrairieLearn and nbgrader with byte-identical scores. Today, per #502's
own Background section and confirmed by a repo scan here, **zero code or
docs in the tree mention any of the four platforms** except the roadmap
documents and this issue family itself — no CLI contract freeze, no
adapter, no fixture. This task's scope is the execution environment only
(containerize + pin), explicitly deferring the assertion to #719
(`TASK-C531-2`) and the ordering/determinism gate to #724
(`TASK-C531-4`).

## Findings

**1. [High] The declared `ordering_after` omits the dependencies the acceptance criteria actually require.**
`ordering_after: ["TASK-C531-1"]` is the only edge declared. But AC1 says
*"Every adapter lane runs in a container … a network-isolated run
passes"* — there is no "adapter lane" to containerize until the four
adapter-build tasks (`FEAT-C21-2..5`, filed as #525 Gradescope, and its
siblings #526/#528/#530) and the frozen CLI contract they consume
(`FEAT-C21-1`, #524) have landed, and none of those are closed. Compare
the sibling tasks in the same family, which get this right: #719
(`TASK-C531-2`) declares `ordering_after: ["TASK-C531-1", 525, 526, 528,
530]`, and #724 (`TASK-C531-4`) declares `ordering_after: ["TASK-C531-3",
"TASK-C524-2"]`. #721 sits between them in the dependency chain
(apparatus → **containerize adapters** → assert parity → gate ordering)
but is the one task in the chain that fails to name its adapter/contract
prerequisites. As filed, this task could be picked up and "completed" by
building a container that runs nothing — the AC does not distinguish an
empty lane from a real one, because there is nothing yet to make it not
empty. Recommend adding 524, 525, 526, 528, 530 (or their resolved issue
numbers) to `ordering_after`, matching #719's pattern, and possibly
splitting this into "1-2 stub lanes ready now" vs. "closes once all four
adapters exist" so partial progress is visible without letting a red
herring pass count as done.

**2. [Medium] "Updating a pinned contract is a reviewed change … not a silent refresh" has no enforcement mechanism.**
Every other structural claim in this issue family gets a concrete
verification hook: #524 requires a `CliContractConformanceTest` and a
queryable contract version; #724 requires the lane ordering to be
"enforced structurally — an adapter lane cannot be scheduled ahead of the
conformance gate by reordering a workflow file." This task's fourth
criterion has no analogous hook — "a reviewed change" is satisfied by any
PR with a description, whether or not that description actually names
the affected adapter behavior. As written, a maintainer could bump a
pinned spec file with a one-line "update Gradescope spec" commit message
and technically satisfy the checkbox. Recommend a concrete mechanism:
e.g., a CI check requiring any diff to a pinned-contract file to also
touch a companion "affected adapter behavior" note, failing the build
otherwise — the same "make it a promise, not a habit" discipline #524
applies to the CLI contract itself.

**3. [Medium] "a network-isolated run passes" names no isolation mechanism, so it can pass by accident rather than by construction.**
The AC doesn't say how network isolation is enforced (`docker
--network=none`? a `Assumptions.assumeTrue` connectivity probe? an
outbound-connection interceptor that fails the build on any attempt?).
Without a stated mechanism, "passes" can mean nothing more than "no lane
happened to make a network call in this particular run" — which is
exactly the illusory-hermeticity failure mode CAP-21's own `KC-21-2`
warns about for interactive sessions ("if any adapter needs … a live
protocol to grade, stop that adapter"). A lane that silently tolerates
network access but doesn't use it today would pass this criterion and
then break the very first time a dependency update reintroduces a stray
call. Recommend naming the isolation mechanism explicitly and asserting
on the mechanism itself (build fails on any attempted outbound
connection), not merely on the run's exit code.

**4. [Medium] "pinned … with its version and source recorded" assumes all four platforms publish something version-shaped, and they don't uniformly.**
PrairieLearn's `externalGrader` interface and nbgrader's autograde model
are closer to versioned software releases; Gradescope's autograder spec
and GitHub Classroom's Action surface are living documentation pages with
no release version number. The AC doesn't define what "version" means
for a non-versioned doc — a fetch date? a content hash? a Wayback Machine
snapshot URL? Left open, each of the four adapter authors can invent
their own pinning convention, which undermines exactly the comparability
this task exists to buy (four platforms measured the same way). Recommend
this task define one pinning record format (URL + retrieval date +
sha256 of the fetched artifact, minimum) applied identically across all
four, rather than leaving it to be improvised per adapter.

**5. [Low, residual risk rather than a defect] Pinning a "documented" contract can pass while the live platform has undocumented behavior — already acknowledged upstream, worth restating locally.**
The issue's own Outcome text is honest that this converts platform drift
into "a red lane against a pinned contract rather than an outage," and
#502's `KC-21-3` already accepts "ship with three platforms rather than
one scraped one" as the fallback. But nothing in this task's AC (or its
parent #531's) requires even an occasional live-account smoke check to
catch the case where the *documented* spec never changes but the actual
service silently diverges from its own docs (Gradescope, being
proprietary, is the platform most likely to do this). Not a blocker;
worth a one-line note in the eventual PR that this is a known, accepted
gap rather than an oversight.

## What's solid

- The four-way boundary split (apparatus #717 / assertion #719 /
  containerization #721 / ordering-and-determinism #724) is clean and
  non-overlapping as written — each task's Boundary section names exactly
  what it is not, and cross-checking the sibling issues confirms no
  content is duplicated between them.
- "Dedicated lanes, not entries in the core toolchain matrix" is not a
  speculative ask — `.github/workflows/ci.yml` already implements exactly
  this pattern for the Windows/macOS/Wayland lanes (advisory,
  `continue-on-error`, separate from the JDK build matrix), so the
  convention this criterion invokes is real and precedented in this repo
  today.
- The task correctly treats "no platform account, no network dependency"
  as an execution-environment concern distinct from the parity assertion
  itself (#719) and the conformance-ordering gate (#724) — that
  separation of concerns is the right decomposition, independent of the
  dependency-graph gap noted in finding 1.

## Bottom line

The scope and boundary are well-drawn, and the design choices (dedicated
lanes, documented-contract pinning, network isolation) are individually
reasonable and grounded in real repo precedent. The blocking problem is
structural: the task's `ordering_after` doesn't name the adapter-build
and CLI-contract-freeze tasks its own acceptance criteria depend on, so
it is currently workable-on-paper against prerequisites that don't exist,
and two of its four acceptance criteria (contract review, network
isolation) lack a verification mechanism, unlike their siblings in the
same issue family. Fix the ordering edges and add the missing enforcement
hooks before this is picked up.
