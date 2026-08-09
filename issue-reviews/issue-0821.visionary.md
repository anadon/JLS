# Issue #821: TASK-C567-2: CONTRIBUTING's first screen is clone, build, test, PR — and the commands in it are executed by CI so the invitation cannot go stale
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

It is not for CONTRIBUTING's first ten lines. It is the smallest tile of CAP-30
(#514): *an outside developer's first PR merges within a week*. #567 splits that
into a filing door (#820) and a building door (this issue). The building door is
supposed to answer one question a stranger asks in the first ninety seconds:
"can I get this green on my machine before I invest anything?"

Judged against that question, the issue as written is aimed slightly off-target
and picks a mechanism the project already has a better version of.

## The premise is largely already false

`CONTRIBUTING.md` lines 6–15 *are* a quickstart today:

```
## Getting started
  mvn verify          # compile (warnings are errors), tests, SpotBugs
  java -jar target/jls-*.jar
JDK 25 or newer and Maven are the only requirements ...
Sources live in `src/`, tests in `test/` (JUnit 5, headless — no display needed).
```

Ten lines, no forward reference to tier/band/ID conventions. AC-1 and #567's
AC-3 are ~70% satisfied before anyone starts. What is genuinely absent is the
`git clone` line, the "open a PR" line, the Maven floor (3.6.3, per
`pom.xml`'s enforcer — CONTRIBUTING says only "and Maven"), and any staleness
guard. So this is a small delta, not a rewrite — and the "internal monologue"
indictment from #510 §5 / CAP-30 was aimed at the *tracker's spec prose*, which
is #820's territory, not at CONTRIBUTING's opening.

That matters because the issue's framing invites a large edit where a five-line
edit plus one test is the whole honest job.

## Where the newcomer actually dies, and the reframe that fixes it

Not on line 10. On line 17, where "Making changes" begins: NullAway/JSpecify
ratchets, the three-population `instanceof` rule, "never add a `default:` arm",
SpotBugs exclusion policy, and fifty lines of coverage-ratchet climb convention
with epsilon-headroom lore. A friendly ten-line porch in front of a hundred-line
minefield does not change the outcome the capstone measures.

Worse, the quickstart's promise is **documented in the same file to be false**.
AC-5 wants "a green local test run" from `mvn verify`; AC-4 wants the stated JDK
to match CI. But CONTRIBUTING lines 84–89 record the #233 incident: a coverage
floor calibrated on JDK 25 measured lower on JDK 26 and failed an unrelated PR.
README says "JDK 25 or newer". A newcomer on 26 who follows the quickstart to the
letter can get red for a reason that is not theirs and not explicable in ten
lines. AC-4 and AC-5 as written are in tension with the project's own record.

**Reframe: name two gates, not one.** The pom already separates them —
`coverage-ratchet` and SpotBugs bind to `verify`; the test suite runs at `test`.

- `mvn test` is the **newcomer gate**: does the code compile and do the tests
  pass on your machine? Deterministic across JDKs, fast, and the honest answer
  to "am I set up correctly?"
- `mvn verify` is the **merge gate**: ratchets, SpotBugs, NullAway, coverage
  floors. Named in the quickstart as "what CI will run", with one sentence
  saying a ratchet failure is a conversation, not a rejection.

That single distinction does more for CAP-30 AC-4 (first PR merged in a week)
than any amount of prose polish, and it costs two lines.

## AC-3's mechanism duplicates a seam the project already owns

The issue asks for the quickstart's commands to run "as CI doc-test steps". The
project's established, and better, answer to "this document must not drift" is a
**JUnit test that reads the markdown and cross-checks it against the artifact**:
`ExtensionPointCatalogTest` (docs/extension-points.md ↔ the constants, *both
directions*), `CliFlagTableTest`, `FileFormatSpecTest`, `HotkeysHelpAccuracyTest`,
`HelpTopicsTest`, `NullMarkedRatchetTest`, `SealedHierarchyTest`. README even
records the single-source principle explicitly for the installer:
`scripts/build-installer.sh` is "the single recipe used both locally and by CI."

A new shell lane in a 59 KB `ci.yml` that already carries seventeen jobs is the
weaker instrument, and it is weaker in the exact dimension AC-4 cares about:

- A CI step running on JDK 25 **cannot notice** that CONTRIBUTING says 21 — it
  just passes. Toolchain-text drift is invisible to execution.
- It cannot notice that CONTRIBUTING omits the Maven floor the enforcer requires
  (`[3.6.3,)`), or that a future raise to `[3.9,)` left the prose behind.
- It cannot notice that "sources live in `src/`" stopped matching
  `<sourceDirectory>` (line 132), or that `target/jls-*.jar` stopped matching
  `finalName`.
- It runs nowhere the contributor can see it. The on-ramp's whole point is
  feedback *before* the push.

**Concrete alternative: `test/jls/QuickstartContractTest.java`.** Parse the
fenced blocks and prose in CONTRIBUTING's first section and assert, in both
directions, against machine-readable truth:

| Doc claim | Pinned against |
|---|---|
| JDK floor in prose | `requireJavaVersion` in `pom.xml` **and** the `java:` baseline in `ci.yml`'s `build` matrix |
| Maven floor in prose | `requireMavenVersion` (`[3.6.3,)`) |
| `src/`, `test/` | `<sourceDirectory>` / `<testSourceDirectory>` |
| `target/jls-*.jar` glob | the shade/`finalName` output pattern |
| clone URL | `<url>` / SCM in `pom.xml` (line 17) |
| every command named | drawn from an allowlist whose Maven goals exist in the effective build |

This runs inside `mvn verify` — on CI *and* on the contributor's laptop — and it
is the same shape as the tests already guarding six other documents. It is
strictly more sensitive than execution for the drift class that actually bites.

**And the execution proof already exists.** The `build` job *is* checkout →
set-up-JDK → `mvn verify` from a clean tree, on two JDKs, on every push. Adding
a lane to prove the quickstart's commands run from a clean checkout re-proves
what the required lane proves already. The honest design is: the test pins the
text to CI's configuration; the existing `build` job is the execution. No new
lane, no eighteenth job, one file added.

## A second alternative worth naming: lead with the zero-install path

The staleness AC-4 fears is a symptom of asking the newcomer to assemble a
toolchain. JLS already ships three ways to skip that: `.devcontainer/` (Maven +
Temurin 26 + every optional tool), Codespaces wired to it, and a flake. A
quickstart that opens with "Open in Codespaces, then `mvn test`" gives a green
run in one click with a toolchain that *cannot* drift from the doc, because the
doc doesn't name one — and keeps the local `git clone` path as the second entry
for people who want it. That ordering serves CAP-30 AC-3 ("fork CI gives a
first-time contributor green/red feedback with zero maintainer action") far more
directly than a shell lane does, and it makes AC-5 genuinely reachable rather
than aspirational.

## Disregarding AC-2

I am explicitly setting aside AC-2 ("the existing contract prose is preserved
verbatim below"). It optimizes for the wrong property. Preserving a hundred
lines of ratchet law verbatim under a friendly porch produces a document that is
welcoming for ten lines and forbidding for a hundred — which is the state
CAP-30 is trying to exit. The document has two audiences and should become two
documents:

- **CONTRIBUTING** = the invitation: quickstart, how to open a PR, what review
  feels like, and (per #571, which the #567 boundary note says will land here)
  the response-time promise.
- **`docs/maintainer-gates.md`** = the law: coverage ratchet climb convention,
  mutation floors, NullAway ratchet, sealed dispatch, SpotBugs policy. Linked
  once from CONTRIBUTING as "what CI enforces and why". ARCHITECTURE.md already
  hosts exactly this genre under "Recorded decisions", so the seam is familiar.

Verbatim preservation is then satisfied by *moving* text, not by pinning it in
place — and the #567 boundary note's constraint (nobody rewrites anyone else's
section) is honored, since a move is not a rewrite.

## Does it strengthen the arc?

Yes, with the reframe. It pulls slightly against the arc as written: a new CI
lane where a doc-test belongs duplicates a mature seam, and "verbatim below"
freezes the structural problem that repels the very contributor the capstone
wants. Nothing here is wrong-headed; it is a good goal reached by the second-best
route.

## Recommended shape

1. Add `git clone https://github.com/anadon/JLS.git` and a two-line "open a PR"
   step; state JDK **25** and Maven **3.6.3** explicitly, sourced from the
   enforcer. (~5 lines.)
2. Split the gate: `mvn test` for "am I set up", `mvn verify` for "what CI runs".
3. Lead with devcontainer/Codespaces; local toolchain second.
4. Replace AC-3's CI lane with `test/jls/QuickstartContractTest` pinning the doc
   to `pom.xml` and `ci.yml` in both directions; let the existing `build` job be
   the execution proof.
5. Move the ratchet law to `docs/maintainer-gates.md` instead of freezing it
   verbatim beneath the quickstart.
