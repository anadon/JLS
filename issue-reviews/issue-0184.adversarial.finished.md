# Issue #184: Release-artifact reproducibility gaps: container apt pinning, installer SOURCE_DATE_EPOCH, and a BOM reproducibility guard in CI
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

Findings A (installer `SOURCE_DATE_EPOCH`), B (container apt pinning), and C
(CI hashes `bom.json`) are genuinely landed and every file:line citation in
the issue body checks out against the working tree (`ci.yml` L798-855,
`resources/packaging/Dockerfile`, `scripts/build-container.sh`,
`scripts/build-installer.sh`, `pom.xml` L47, `README.md` L53-60 all match).
The issue is not fabricated evidence. What remains — P2, the two-build
container layer-digest comparison — has a specific methodology flaw and the
issue's own bookkeeping has already drifted from what its comment thread
records as reality.

## Findings (most severe first)

**1. [High] The issue's own recommended P2 methodology cannot exercise the mechanism it's meant to validate.**
§6 and the Open Questions section recommend "two `workflow_dispatch` dry runs
of `release.yml`" as the default way to gather P2 evidence, "because it
exercises the exact production path." It does not. `release.yml` sets
`PUSH: ${{ github.event_name == 'push' && '1' || '0' }}`
(`.github/workflows/release.yml` L213) — a `workflow_dispatch` run always has
`PUSH=0`. In `scripts/build-container.sh`, the `rewrite-timestamp=true`
output flag is only emitted `if [ "${PUSH:-0}" = "1" ]` (L79-82); the native
single-arch smoke-test build (L187-190 of `release.yml`) uses plain
`docker build`, which never takes that flag either. The Dockerfile's own
comment states the mechanism is load-bearing for exactly the thing P2
measures: *"SOURCE_DATE_EPOCH ... BuildKit clamps image-config and history
timestamps to it (layer-tarball mtimes additionally need the push path's
rewrite-timestamp=true output option)"* (`resources/packaging/Dockerfile`
L27-29). So a dry run pins apt packages and clamps image config/history, but
the JLS-controlled layer *tarball* mtimes — and hence their digests — are
left on build wall-clock time. Two dry runs of the identical commit, run at
different times, will very plausibly diverge on layer digest for a reason
that has nothing to do with residual non-determinism (apt cache, ldconfig,
jlink ordering). If that happens, §7's Re-planning Protocol sends the
executor hunting for a root cause that doesn't exist, while the actual gap —
the dry run never touches the push-only code path — goes unnoticed.
**Recommendation:** before anyone burns a dry run on this, either (a) make
`build-container.sh` request layer-mtime rewriting on a non-push local
build too (buildx supports `type=docker`/`type=oci` destinations with
`rewrite-timestamp=true`), or (b) change the recommended P2 procedure to
something that actually exercises the push path (e.g., push to a scratch
tag/registry twice) — and update #818's AC-1, which inherits the same
"two `workflow_dispatch` dry runs" recommendation, accordingly.

**2. [Medium] The issue body is stale relative to its own comment thread: #818 already exists but isn't wired in.**
Comment `5181646511` (2026-08-04, the most recent) states plainly: *"#818 is
this issue's filed slice"* — `TASK-C184-1`, `tier:task`, open, with
`part_of_feature: 184` in its YAML block, and acceptance criteria that are
verbatim P2. Yet the live issue body still lists P2 as `(planned — unfiled)`
in the §2 roster table, still carries it as an unfiled `planned_tasks` entry
in the machine block, and the Completion Criteria checklist still has an
unchecked box for *"`planned_tasks` empty — the P2 scope either executed
directly ... or filed as a `tier:task` child and closed"* with no mention of
#818 discharging it. Checking live state confirms the disconnect is not just
textual: `issue_read get_parent` on #818 returns `parent: null`, and #184's
own `get` response shows `has_children: false` — the two issues are linked
only by a comment's prose, not by GitHub's native sub-issue relationship or
by an edit to #184's `requires_tasks`/roster. A worker who reads #184's body
and machine block but skips all six comments will not discover #818 and may
re-file the same task a second time. **Recommendation:** edit the body's
roster, `requires_tasks`, and mermaid graph to reference #818, and establish
the actual GitHub sub-issue link.

**3. [Medium] `related` list omits the two issues the thread spends the most words on.**
The machine block's `related: [44, 65, 185, 188, 189, 194, 180, 181, 182,
183]` does not include #338 or #818, despite two of the six comments
(`5175916561`, `5176158422`, `5181646511`) being entirely about adjudicating
#184 against #338 and filing #818. The issue states its own consistency rule
— *"Machine block, roster table, and mermaid graph agree with reality at
close (rule A)"* — but that rule is already violated pre-close, for anyone
who trusts the machine block over the prose.

**4. [Low] The `APT_SNAPSHOT`/`outputTimestamp` coupling the issue relies on is asserted, not enforced.**
§1 claims `APT_SNAPSHOT=20260716T000000Z` "matches the pom's
`outputTimestamp` date," and the Global Invariants section requires it be
"bumped only deliberately together with the base-image digest ... never
silently." Both values are independent hardcoded literals — Dockerfile
`ARG APT_SNAPSHOT=20260716T000000Z` vs. pom.xml's
`<project.build.outputTimestamp>2026-07-16T00:00:00Z</project.build.outputTimestamp>`
(`pom.xml` L47) — with no test or CI step tying them together (grep of
`test/` and `.github/` for `outputTimestamp` finds only a comment, no
assertion). This is exactly the class of silent-drift risk Finding C's BOM
guard exists to catch elsewhere in the same issue, yet the apt-pin/epoch
pairing itself is undefended.

**5. [Low] Cost/process observation, not a content defect.**
The issue frames the remaining work as "a single bounded experiment" that
"needs a runner with a docker daemon" and "rides along until a maintainer
triggers a run." Three weeks after the pin landed (`7edd009`, 2026-07-18)
and five days after #818 was filed (2026-08-04), against a current date of
2026-08-09, no dry run has been triggered, while the issue has accumulated
six comments of multi-agent dedup/audit bookkeeping across four related
issues (#185, #188, #338, #818). The bookkeeping overhead has now likely
exceeded the cost of simply running the two dry runs — worth flagging to
whoever next picks this up, especially since Finding 1 means running them
as currently recommended won't even produce a trustworthy answer.

## What's solid

- Every file:line citation in §1 and §3 matches the current working tree —
  the evidence isn't fabricated or stale in the way many "capability
  statement" issues are.
- The out-of-scope boundary (installers → #188, conformance → #185, program
  behavior determinism → #180-183) is coherent and doesn't try to absorb
  adjacent work.
- A/B/C's Definition-of-Done items are genuinely checked, not aspirational.
- §7's Re-planning Protocol correctly anticipates a P2 failure mode and
  requires a `REPLAN:` comment rather than a silent doc edit — good
  discipline, undermined only by Finding 1 making a false failure likely.

## Verdict rationale

Not `should-not-proceed`: the landed work is real and the scope is coherent.
Not `sound`/`sound-with-concerns`: the one substantive piece of work left in
this issue (P2) is specified via a methodology that, per the codebase's own
comments, will not exercise the mechanism under test, and the issue's
tracking metadata (children, `related`) has already fallen out of sync with
its own comment thread while still open. Both are fixable without
re-scoping the feature, which is why this lands at `needs-rework` rather
than a harsher verdict.
