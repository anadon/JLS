# Issue #821: TASK-C567-2: CONTRIBUTING's first screen is clone, build, test, PR — and the commands in it are executed by CI so the invitation cannot go stale
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#821 is a task-level slice of #567's AC-3 ("CONTRIBUTING's first ten lines
are a working quickstart"). The parent goal is reasonable, and a chunk of
it already exists at HEAD. But AC-3's "doc-tested quickstart" mechanism is
underspecified to the point of being unsafe or unbuildable as literally
written, and AC-1/AC-5 pull against the project's own stated contribution
norm one section below where the quickstart would sit.

## Findings (most severe first)

**1. AC-3's literal reading can hang CI indefinitely — no execution model or timeout is specified.**
The Outcome names four quickstart steps: clone, build, test, "open a PR."
AC-3 says "the quickstart's commands run as CI doc-test steps from a clean
checkout." The existing quickstart's second line is `java -jar
target/jls-*.jar` (`CONTRIBUTING.md:10`) — a Swing GUI launch. JLS is a
desktop app with an event loop that does not exit on its own, and
`ARCHITECTURE.md:206` states CI runs headless ("no display"). Literally
executing that line as a CI step either throws `HeadlessException`
immediately (a step that always "fails" red, defeating the doc-test's
purpose since it can never pass without editing the quickstart to not
demonstrate running the app) or, under xvfb, blocks forever waiting for
window-manager interaction. The issue specifies none of: which quickstart
lines are actually executed, how the "launch the app" line is verified
without hanging, or a step timeout. This is a real, near-term operational
hazard, not a hypothetical — the current draft-quickstart already contains
the offending line. *Recommendation: scope AC-3 explicitly to build+test
commands only ("mvn verify" family), and either drop the launch line from
what's doc-tested or replace it with a headless-safe smoke invocation
(`java -jar … -help` / batch-mode flag) that actually exits.*

**2. AC-3 cannot cover "PR submission," which AC-1 requires the quickstart to include.**
AC-1: the quickstart must cover "clone, build, test **and PR submission**."
AC-3: "the commands it names are executed as doc-test steps in CI... a
divergence between the documented command and the real one fails the
build." You cannot safely auto-execute "open a pull request" as a CI step
on every push/PR: `ci.yml:16-17` sets `permissions: contents: read`
(least-privilege, issue #68) specifically to deny write/PR-creation scope,
and even with elevated permissions, a build step that opens a real PR on
every CI run would spam the repo and can't test itself without infinite
regress. So either AC-1's fourth quickstart item is never actually
doc-tested (silently narrowing AC-3's "the commands it names" promise) or
AC-3 as written asks for something CI structurally cannot do without
weakening the least-privilege token posture #68 established. The issue
doesn't acknowledge the conflict. *Recommendation: explicitly exempt the
PR-submission step from AC-3's execution requirement (documentation only),
and say so in the AC text rather than leaving it implicit.*

**3. AC-3's "divergence...fails the build" guarantee is narrower than advertised, and a divergence already exists today.**
The current quickstart says `mvn verify` (`CONTRIBUTING.md:9`). The actual
CI build step is `xvfb-run -a mvn -B verify -Djls.test.headless=false`
(display substrate) or `mvn -B verify` (`ci.yml:82-85`) — both carry `-B`
and the headless-display variant carries an extra system property the
quickstart never mentions. Under a literal "commands must match CI" bar,
today's own quickstart already diverges from what CI runs, yet nobody
would call that a bug — `-B` is CI-batch-mode noise irrelevant to a human
running the command interactively. This shows the acceptance criterion is
gameable in both directions: (a) a doc-test step that just re-runs the
documented string in isolation only catches cases where the *literal
string* now errors, not cases where CI's real invocation quietly diverged
from it (e.g. a new required `-D` flag added only to `ci.yml`, leaving the
doc-tested command still "passing" while being incomplete/misleading for
a human); (b) making the doc-test genuinely diff against `ci.yml`'s
command text would force either exposing CI-internal flags in the
newcomer-facing quickstart (bad UX, contradicts the "ten-line invitation"
goal) or hand-waving which parts must match. *Recommendation: define
precisely what "divergence" means — e.g. "the exact string in the fenced
code block, executed verbatim, must exit 0" — and accept that this does
not, by itself, guarantee semantic parity with the real CI build command;
don't oversell the safety property in the Outcome text.*

**4. AC-4's "Maven version matching what CI actually uses" has no ground truth to copy from.**
For JDK this works: CI pins the required leg to JDK 25 (`ci.yml:33-40`,
`java-version: ${{ matrix.java }}`) and the enforcer plugin requires
`[25,)` (`pom.xml:617-619`), so "JDK 25" is unambiguous and already stated
(`CONTRIBUTING.md:13`). For Maven, CI never selects or pins a version —
`setup-java@…` (`ci.yml:58-62`) configures only `distribution`/
`java-version`/`cache`, and Maven itself comes from whatever
`ubuntu-latest`/`windows-latest` ships. The only written floor is the
enforcer's `requireMavenVersion` `[3.6.3,)` (`pom.xml:614-616`) — a 2019
release, almost certainly far below what the runner image actually
provides. So "matching what CI actually uses" is not answerable from any
file in the repo; satisfying AC-4 either means writing the enforcer floor
(3.6.3), which is not "what CI actually uses" and reads as stale/absurd
next to JDK 25, or means introspecting the live runner image and hand
copying a number that GitHub can bump without this repo's knowledge — the
exact staleness failure mode the issue is trying to eliminate everywhere
else. *Recommendation: either drop "Maven version" from AC-4 and state
only the enforcer floor with the caveat that it's a floor not a pin, or
have CI pin an explicit Maven version (e.g. via `actions/setup-java`'s
Maven wrapper or a maven-version action) so there is something real to
document and doc-test against.*

**5. AC-1 + AC-5 push the reader past the project's own gating norm one section down.**
AC-5: "A reader who follows only the quickstart reaches a green local test
run without opening any other document," ending at "open a PR" (AC-1).
But the contract prose the same file preserves "intact ... below it"
(AC-2) opens with: "**Open or comment on an issue first** for anything
beyond a trivial fix" (`CONTRIBUTING.md:19`), and the tracking-issue
pointer (#33) right after it. A reader who does exactly what AC-5 asks —
never opens another document — will skip that gate and go straight from
green tests to a PR, which is the opposite of the maintainer's stated
process for non-trivial changes. The issue doesn't require the quickstart
to at least flag "for non-trivial changes, see the note below before
opening your PR," so satisfying AC-1/AC-5 to the letter actively
encourages the low-context drive-by PRs the "issue first" rule exists to
prevent. *Recommendation: add an AC requiring the quickstart's PR step to
carry a one-line pointer to the issue-first norm, or narrow AC-5's "no
other document" claim to cover only the build/test path, not PR
submission.*

**6. Feasibility/cost of AC-3 as a new CI lane is unbudgeted.**
No doc-test infrastructure exists anywhere in the repo today — `grep` over
`.github/workflows/*.yml` and `scripts/` turns up nothing that parses
markdown fences or replays them (confirmed by search). AC-3 therefore
requires new tooling (a script to extract fenced code blocks from
CONTRIBUTING.md and execute them) plus a new CI job/step, and — per
finding 1 — a second near-full `mvn verify` invocation if the build/test
lines are taken literally, on top of the existing JDK 25 + JDK 26 matrix
(`ci.yml:33-40`) that already runs `mvn -B verify` per leg. The issue
prices none of this: no mention of where the extraction script lives, how
it's tested itself, or the added wall-clock cost per PR. *Recommendation:
name the extraction mechanism (or explicitly scope it to "re-run the
existing `mvn verify` step and diff only that block's text," reusing the
main job's output instead of a second full build) before this is
estimated as a task-sized unit of work.*

## What's solid

- The core ask — put an actionable quickstart above the contract prose,
  keep the contract prose verbatim — is a legitimate, low-risk doc change
  and much of it (a "Getting started" section with `mvn verify`) already
  exists in `CONTRIBUTING.md:6-15`, so this is a real refinement, not
  invented work.
- AC-2 (preserve contract prose verbatim) is unambiguous and trivially
  checkable by diff.
- Tying this task to #567 via `part_of_feature: 567` is consistent — #567
  AC-3 already asks for exactly this quickstart, so #821 is a coherent
  decomposition rather than a duplicate or a conflicting mandate.

## Net

The plain "reorder and pad out the quickstart" half of this task is sound
and cheap. The "doc-tested, cannot-go-stale" half (AC-3, AC-4, and the
AC-1/AC-5 interaction) is where the issue overpromises: at least one
concrete hazard (finding 1, GUI-launch line hanging headless CI) is
already latent in today's file, one requirement is structurally
unbuildable as stated (finding 2), and one AC has no ground truth to
target (finding 4). This needs the mechanism spelled out — scope of what
gets executed, timeout/headless handling, and what "divergence" formally
means — before implementation starts, not discovered mid-PR.
