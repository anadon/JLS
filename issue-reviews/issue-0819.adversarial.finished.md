# Issue #819: TASK-C185-1: a stranger's `mvn artifact:compare` against a published release matches, and the transcript is the proof — or the release path differs from the CI path and we find out that way
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#819 is the sole remaining planned child of #185 (Reproducible Builds
conformance): run the `mvn artifact:compare` recipe from
`docs/reproducibility.md` §3.4 against the first release published after
v5.0.4 and record the transcript. The framing is disciplined (explicit
falsification path in AC-4, an honesty constraint against softening
claims), but the acceptance criteria describe a check the named recipe
cannot actually perform, and the issue is not wired into #185's own
tracking scheme.

## Findings, most severe first

**1. [High] AC-1 demands verification "against that release's published
`.buildinfo`," but the recipe the issue commits to never touches the
`.buildinfo`.** AC-1: "The recipe is run exactly as `docs/reproducibility.md`
states it, ... against that release's published `.buildinfo`, in the
toolchain the `.buildinfo` records." The recipe the issue's own title
names is §3.4, `mvn artifact:compare`, whose documented command is:

```sh
mvn -B -DskipTests package \
  org.apache.maven.plugins:maven-artifact-plugin:3.6.0:compare \
  -Dreference.repo=https://maven.pkg.github.com/anadon/JLS
```

(`docs/reproducibility.md:117-119`). This compares the local rebuild
against whatever is published to the GitHub Packages Maven registry —
there is no `-Dbuildinfo=...` or any other reference to the `.buildinfo`
file anywhere in that command or its surrounding prose. The recipe that
actually diffs against the `.buildinfo`'s SHA-512 entries is the
*different*, separately-numbered §3.3 ("Independent rebuild",
`docs/reproducibility.md:79-108`). As literally written, AC-1 cannot be
satisfied by the recipe the issue's title and body single out.
*Recommendation:* either retarget the issue at §3.3 (which does check
against the `.buildinfo`) as the primary proof and demote §3.4 to a
secondary/optional check, or rewrite AC-1 to describe what §3.4 actually
verifies (registry parity, not `.buildinfo` parity).

**2. [High] The artifact §3.4 compares against is not the release-asset
build the `.buildinfo` describes — it's a third, independent build.**
`.github/workflows/release.yml`'s `release` job runs
`mvn -B verify org.apache.maven.plugins:maven-artifact-plugin:3.6.0:buildinfo`
(line 90) and is what produces the jar/BOM/`.buildinfo` actually attached
to the GitHub Release. A separate `maven-registry` job (`needs: release`,
its own fresh `actions/checkout`, its own runner) independently runs
`mvn -B deploy -DskipTests ...` (line 149) to populate the Maven
registry — with no artifact hand-off between the two jobs. So there are
three distinct Maven builds in play: the CI perturbed-rebuild gate (P1,
already proves CI-against-itself), the `release` job's build (produces
the actual downloadable Release bytes + `.buildinfo`), and the
`maven-registry` job's build (what `-Dreference.repo` actually diffs
against). A match in §3.4 shows a stranger's rebuild agrees with the
*registry* build, which is itself a second CI-side build of the same
commit — it does not, by itself, prove the *Release-asset* bytes (the
ones users actually download and whose checksums appear in
`SHA256SUMS`/the `.buildinfo`) match. This directly undercuts the issue's
stated purpose: "This is the only step that proves the *release* path
produces the same bytes as the *CI* path." *Recommendation:* have the
transcript also `sha256sum` the local rebuild against the release
asset's `SHA256SUMS` (§3.3's mechanism) so the comment actually closes
the release-vs-CI gap the issue claims to close, not just a
registry-vs-independent-rebuild gap.

**3. [Medium] AC-5 is satisfiable by a reading that elides the friction
the doc itself admits exists for this recipe.** AC-5: "no repository
write access, no maintainer secret, and no unpublished input is needed
at any step." `docs/reproducibility.md:121-124` states plainly: "GitHub's
Maven registry requires an access token even for public downloads (a
`settings.xml` server entry with a `read:packages` token), so recipe 3.3
is the friction-free path; 3.4 is provided for tooling that speaks
`.buildinfo`/compare natively." A stranger's own PAT is not a
"maintainer secret," so AC-5 can be ticked off narrowly true while the
recipe still requires the outsider to hold a GitHub account and mint a
token — exactly the friction README.md:97-101 independently tells
"plain-download users" to avoid by preferring the Releases page instead.
*Recommendation:* AC-5 should say explicitly whether "a personal GitHub
PAT with `read:packages`" counts as an acceptable prerequisite for "a
stranger" or disqualifies the recipe; right now it's gameable either way.

**4. [Medium] #819 is not linked into #185's own tracking scheme despite
claiming to be part of it.** #819's body opens with `part_of_feature: 185`
and `ordering_after: ["the first tagged release after v5.0.4"]`, but
`issue_read(get_sub_issues, 185)` returns an empty list and
`issue_read(get_parent, 819)` returns `null` — no structured GitHub
sub-issue link exists in either direction. #185's own machine block
(last touched 2026-08-04T16:09:57Z, the same day #819 was opened) still
reads `requires_tasks: []` with "no children filed," and its
`planned_tasks` entry describes the P2 close-out in prose but names no
issue number. Anyone auditing #185's DAG per its own documented
convention (§7's `REPLAN:` discipline, the roster/mermaid-graph rule)
will not discover #819 exists. *Recommendation:* file #819 as a
structured sub-issue of #185 (or add a `REPLAN:` comment on #185 naming
#819), matching the tracking discipline #185 itself sets out.

**5. [Low] AC-3's msi/dmg/container boundary language is inherited
boilerplate that can't actually be at risk here.** AC-3: "...the
msi/dmg/container boundary owned by #184/#188 stays intact." But
`mvn artifact:compare` against the Maven Packages registry can
structurally only ever touch the jar (and the pom, which the doc itself
calls "source" and not really compared) — the registry never carries
msi/dmg/container assets, so this clause guards against a scope creep
that §3.4 was never capable of. Harmless, but it signals AC-3 was
copy-adjusted from #185's global invariants rather than written for this
specific recipe, and a less careful implementer might spend effort
"confirming" a non-risk instead of noticing finding 1.

**6. [Low] No mechanism surfaced to actually trigger this task when its
blocking event occurs.** The task is gated on "the first tagged release
after v5.0.4 (an event, not an issue)." `CHANGELOG.md`'s
`## [Unreleased] — 5.0.5-SNAPSHOT` header confirms no such release exists
yet. Nothing in `.github/workflows/` posts a reminder or reopens/pings
this issue on the next `v*` tag push; it relies entirely on a human
remembering. Low severity only because the cost of forgetting is a
delayed DoD checkbox on #185, not a correctness risk.

## What's solid

- AC-4 (file a `tier:task` bug under #185 on mismatch, with diffoscope
  output, no softened wording) matches #185 §7's pre-existing re-planning
  protocol exactly — good consistency with the parent feature's own rules.
- AC-2's requirement to record tag + JDK vendor/version + Maven version
  lines up with exactly what `docs/reproducibility.md` §2 says the
  `.buildinfo` contains, so the recorded transcript will be independently
  checkable against that file.
- The scope is appropriately small (one verification task, not a rewrite
  of #185's landed work), and gating on a real release event rather than
  faking one against v5.0.4 (which predates `.buildinfo` publication) is
  correct — running this today would necessarily fail or be vacuous.

## Verdict rationale

`needs-rework`: the issue is well-intentioned and mostly consistent with
its parent's conventions, but AC-1 — the central acceptance criterion —
describes a verification (against the `.buildinfo`) that the specific
recipe the issue names (`mvn artifact:compare`, §3.4) does not perform,
and finding 2 shows the gap is not cosmetic: §3.4 as documented cannot
prove what the issue's own outcome statement claims it proves. That
should be fixed — either by retargeting the primary check to §3.3 or by
rewriting AC-1 to match what §3.4 actually verifies — before this task is
picked up, or the eventual closing comment on #185 will read as proof of
something it didn't establish.
