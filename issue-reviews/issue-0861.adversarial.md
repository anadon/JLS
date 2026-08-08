# Issue #861: TASK-C582-1: a committed script collects per-asset download counts and appends them idempotently to a tracked file — re-running a recorded date changes nothing
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of the ask

A committed, locally-runnable script hits the public GitHub releases API,
records per-release/per-asset `download_count` into a tracked, diffable
file, is idempotent per already-recorded date, documents its own schema,
and carries unit tests (idempotency + a recorded-fixture parse) that don't
touch the network. It is one of three sibling tasks under #582 (#862 is
the scheduler, #863 is the policy doc).

## Findings, most severe first

**1. "Idempotent" is specified loosely enough that a trivial, wrong
implementation passes AC-3/AC-5.** AC-3 says a re-run for an
already-recorded date must leave the row "alone **or** replaced in place,
never duplicated." Download counts are cumulative and keep climbing
through the day, so the realistic failure mode is: script runs twice on
the same date, sees two *different* counts, and either (a) freezes on the
first (stale, under-counts that day) or (b) silently overwrites with the
second (fine, but nothing forces this choice). AC-5 only requires tests to
"exercise the idempotency," which an implementer can satisfy by re-running
the *same* fixture twice and asserting the file is unchanged — a test that
never exercises the case that actually matters (two runs, same date,
diverging counts). A single-maintainer implementation could ship the
"freeze on first write" behavior, pass every stated AC, and quietly
under-report every day the scheduled workflow (#862) happens to retry.
**Recommendation:** AC-3 should pick one behavior (last-write-wins is the
only one consistent with "counts are cumulative" per #582's own framing),
and AC-5 should explicitly require a test with a second, larger recorded
count for the same date.

**2. The auth story in the Outcome text contradicts itself across
execution contexts.** Quoted: *"It authenticates with nothing more than
the workflow token, so a contributor can run it locally against the
public API."* The workflow token (`GITHUB_TOKEN`/`github.token`) is an
Actions-runner-only ambient credential (confirmed in
`.github/workflows/release.yml:151` — it's injected as an env var inside
a job, not something that exists on a contributor's laptop). A contributor
running "locally" therefore either (a) runs fully unauthenticated, subject
to GitHub's 60 requests/hour per-IP limit for the public API, or (b)
supplies their own personal token — which is a credential the Outcome
text explicitly says isn't needed ("no repository secret"). AC-1 restates
"no repository secret" but never says the script must work
unauthenticated, nor that it should accept an optional `GITHUB_TOKEN`/
`GH_TOKEN` override for the higher authenticated limit. As written, the
same script has to serve two different auth models the issue treats as
one. **Recommendation:** split AC-1 into "unauthenticated by default,
optional token env var for higher rate limit" and state the 60/hr
unauthenticated ceiling explicitly so an implementer isn't guessing.

**3. Failure-handling is entirely out of scope here but silently required
by the sibling task.** #862 AC-2 requires "an API failure, rate limit, or
malformed response fails the run visibly and writes nothing." #861 has no
acceptance criterion that the collector itself must raise/exit non-zero on
API failure rather than writing an empty or zero row — none of AC-1
through AC-5 test failure behavior. A #861 implementation can pass 100% of
its own ACs while swallowing errors and returning an empty result set
(satisfying "runnable locally," "idempotent," "documented schema," and
"unit tested" without ever specifying what happens on a 403/;rate-limit
response). #862 would then discover the gap only when trying to build its
own AC-2 test on top of a collector that doesn't expose a distinguishable
failure signal, forcing a rework of #861's script under a different issue
number. **Recommendation:** #861 should carry at least one AC establishing
the collector's error contract (e.g., "a non-2xx or malformed API response
raises/exits non-zero and produces no output"), since #862 depends on it
without ever re-declaring it.

**4. "Per-release, per-asset download counts" silently means "GitHub
Releases assets only," and nothing in #861 or its sibling #863 says so.**
README.md documents distribution through the Nix flake
(`nix run github:anadon/JLS`), GitHub Packages/Maven
(`maven.pkg.github.com/anadon/JLS`), and the container image
(`ghcr.io/anadon/jls`, README.md:103-124) — none of which increment a
GitHub Release asset's `download_count`. #582's boundary note says the
policy doc (#863) will state whether "Flathub/winget/Homebrew" counters
are folded in, but never mentions the channels this repo *already ships
today* (Nix, Maven Packages, GHCR pulls) that the collector's chosen API
structurally cannot see. #861 AC-4 asks the file's schema note to mention
"known limitations (... mirrors are not visible, and so on)" but doesn't
require naming the specific existing channels this repo advertises in its
own README that fall outside the metric. Without that, a reader of the
resulting file could reasonably (and wrongly) read a flat or declining
release-asset count as "adoption is flat," when in fact users may have
shifted to `nix run` or `docker pull ghcr.io/anadon/jls`, both invisible
to this collector. **Recommendation:** AC-4 should require the schema note
to explicitly enumerate the channels *this repository already publishes*
that are out of scope (Nix flake, Maven Packages/GHP, GHCR container
pulls), not just gesture at "mirrors."

**5. No pagination requirement, and no test-infrastructure precedent for
whatever language is chosen.** The GitHub releases list endpoint paginates
(30/page by default); as JLS accumulates releases over years the script
must page through history or it will silently stop counting older
releases once release count exceeds a page. No AC mentions pagination.
Separately, ARCHITECTURE.md's "Test layout" section describes JUnit 5 as
the sole test framework in this repo, and the existing `scripts/*.sh` and
`scripts/*.py` files (`normalize-dmg.py`, `normalize-msi.py`, the rig
scripts) have **no unit tests anywhere in `test/`** — there is no
established pattern in this codebase for unit-testing a standalone
script in any language. AC-5's "unit coverage" therefore requires either
(a) writing the collector in Java to reuse JUnit, which is an unusual
choice for a scheduled script and pulls it into `mvn verify`'s SpotBugs/
warnings-as-errors gate, or (b) introducing a new test toolchain (pytest,
bats, node) with no precedent, which is a scope decision the issue doesn't
make and CI (`.github/workflows/ci.yml`) doesn't currently run.
**Recommendation:** either AC-1 should name the implementation language,
or the issue should acknowledge the new-toolchain decision explicitly
instead of leaving "unit coverage" to imply an existing pattern that
doesn't exist.

## What's solid

- The premise is technically correct: `GET /repos/{owner}/{repo}/releases`
  is reachable unauthenticated for a public repo and does return
  per-asset `download_count`, so AC-1's core claim holds.
- AC-2 ("stable, diffable format... git diff shows exactly what changed")
  is a good, concrete, testable requirement and matches the project's
  general preference for plain-text diffability (e.g. the plain-text
  `.jls` save option, README.md:301-306).
- AC-5's fixture-based network-free parse test is exactly the right shape
  for testability and is consistent with how this repo already tests
  other file-format parsers (`FileAbstractorTest`, `FormatHeaderTest`
  per ARCHITECTURE.md).
- The task correctly declares `ordering_after: []` and is consistent with
  #862/#863 both declaring `ordering_after: ["TASK-C582-1"]` — the
  dependency graph across the three sibling tasks is coherent.
- The boundary note on #582 (comment) correctly scopes this away from
  #338/#443 (build/gating, not counting) and from #590 (positioning
  statement, not instrumentation) — no overlap risk there.

## Bottom line

The task is well-bounded relative to its siblings and technically
grounded in a real, reachable API, but it under-specifies the exact
behavior that makes "idempotent" mean something (finding 1), asserts an
auth story that doesn't survive contact with "runnable locally" (finding
2), leaves a load-bearing error contract implicit and owned by a
different issue (finding 3), and lets its "download counts" framing imply
more coverage of this project's actual distribution surface than the
chosen API can deliver (finding 4). None of these require abandoning the
task, but each should be tightened before implementation starts, or the
acceptance criteria can be satisfied by a script that technically passes
and is still wrong in the way that matters for a KPI meant to replace
stars.
