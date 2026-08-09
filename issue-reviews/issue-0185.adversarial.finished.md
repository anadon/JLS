# Issue #185: Reproducible Builds conformance: independent-rebuild verification, published .buildinfo, and a declared reproducible-artifact scope
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#185 claims all substantive work is landed (verified against `ci.yml`,
`release.yml`, and `docs/reproducibility.md` at the current tree — the
claims check out) and holds itself open solely for "P2": a
`mvn artifact:compare` transcript against the first post-v5.0.4 release.
The landed 80% is solid. The remaining 20% — the acceptance criterion for
P2, and the issue's own tracking hygiene around it — has real problems:
the named recipe cannot verify what the issue says it verifies, and the
issue's machine-readable dependency block is already contradicted by its
own most recent comment.

## Findings, most severe first

**1. [High] The P2 acceptance criterion names a recipe that cannot check what it claims to check, and is gameable as a result.**
The Completion Criteria say: *"P2 recorded: `mvn artifact:compare`
transcript against the first post-v5.0.4 release's published `.buildinfo`
succeeds."* But `docs/reproducibility.md:110-125` (§3.4) shows the actual
command:

```sh
mvn -B -DskipTests package \
  org.apache.maven.plugins:maven-artifact-plugin:3.6.0:compare \
  -Dreference.repo=https://maven.pkg.github.com/anadon/JLS
```

This diffs a local rebuild against the **GitHub Packages Maven registry**
— nothing in it references `-Dbuildinfo` or the `.buildinfo` file's
SHA-512 entries at all; that's the separate, differently-numbered §3.3
recipe. Worse, the registry copy is not the release-asset jar: I read
`.github/workflows/release.yml` and the `release` job (line 90, `mvn -B
verify ...:buildinfo`) is what produces the jar/BOM/`.buildinfo` actually
attached to the GitHub Release and listed in `SHA256SUMS`, while a
*separate* `maven-registry` job (`needs: release`, its own fresh
`actions/checkout`, its own `mvn -B deploy`, line 149) independently
rebuilds the commit to populate the registry. So `artifact:compare`
"succeeding" only proves a stranger's rebuild matches a second CI-side
build (the registry job's), not the bytes users actually download. A
literal reading of the DoD line lets P2 be checked off by a transcript
that never touches the `.buildinfo` and never touches the release-asset
jar — exactly the "verification could pass while the real goal fails"
failure mode this lens hunts for. *Recommendation:* rewrite the DoD/P2
line to either (a) require the §3.3 recipe (`sha256sum` against the
published `SHA256SUMS`/`.buildinfo` SHA-512s — the one that actually
checks the release asset) as the primary proof, with §3.4 demoted to
secondary, or (b) if §3.4 stays primary, state plainly that it verifies
registry parity, not `.buildinfo` parity, and drop the "against ...
published `.buildinfo`" wording.

**2. [High] The issue's own dependency machine block is already false, not just at risk of becoming false.**
The body's `requires_tasks: []`, "no children filed" (comment
5154304841), and GitHub's own `has_children:false` on #185 all assert no
child task exists. But the issue's *own last comment* (same day as the
body's last edit, both timestamped 2026-08-04T16:09:57Z) states: *"#819
... is this issue's filed slice"* for exactly the P2 work, and I
confirmed #819 exists, is open, and is titled `TASK-C185-1` with
`part_of_feature: 185` in its own body. I independently checked both
directions with `issue_read`: `get_sub_issues(185)` returns `[]`, and
#819's `has_parent` is `false`. Rule A in #185's own §7 says "Machine
block, roster table, and mermaid graph agree with reality at close" — but
this mismatch is live *now*, four days before any close, and nothing in
#185's body names #819 or points a reader there. Anyone auditing #185
from its stated `requires_tasks`/`related` fields (which lists #33, #44,
#180-184, #188-191 but not #819) will not discover the child that is
supposed to execute the issue's sole remaining criterion.
*Recommendation:* either link #819 as a structured GitHub sub-issue of
#185, or add a `REPLAN:` comment on #185 naming it and updating
`requires_tasks`/`planned_tasks` accordingly.

**3. [Medium] The Impact section's central claim overstates what the committed recipe delivers.**
"Verification no longer depends on trusting the project's own runner" is
the issue's stated payoff. But per finding 1, the specific recipe (§3.4)
the DoD commits to (a) needs a GitHub `read:packages` token — the doc's
own §3.4 caveat calls this "friction" and README.md:97-101 tells
"plain-download users" to avoid GitHub Packages entirely for this reason
— and (b) even with that token, compares against a second CI-produced
artifact rather than the downloaded release asset. The more genuinely
independent, no-auth recipe already exists in the same document (§3.3)
but isn't the one the DoD names. *Recommendation:* make §3.3 the DoD's
primary recipe; note that this doubles as fixing finding 1.

**4. [Medium] Closure is gated on an event with no reminder mechanism, and the issue's own text half-acknowledges this without mitigating it.**
`blocked_by: []` is annotated "the remaining step waits on the next
tagged release, an event, not an issue." I confirmed `CHANGELOG.md`
still reads `## [Unreleased] — 5.0.5-SNAPSHOT`, and found nothing in
`.github/workflows/` that pings or reopens #185 (or #819) on a `v*` tag
push. This is a real feasibility risk the issue names but does not
mitigate — it can stay open indefinitely on nothing but a human
remembering, and #819 (the task meant to act on the event) has the same
gap. *Recommendation:* a one-line `release.yml` step (or a scheduled
check) that comments on #185/#819 when a new tag ships would close the
gap cheaply.

**5. [Low] Evidence for "P1 supported" is scattered across four commit SHAs and one CI run URL rather than one durable anchor.**
`docs/reproducibility.md:141-146` cites commit `49de3d6`; the issue body
and comments separately cite `34d92bf` (integration tip), `2eb3e0c`
(master merge), and `29afb26` (evidence_commit), plus [CI run
29665293624](https://github.com/anadon/JLS/actions/runs/29665293624)
(subject to Actions log/artifact retention limits). Each reference is
individually plausible as the history of one claim moving through
branches, but reconstructing "P1 is supported" requires walking a
six-comment thread rather than following a single pinned anchor — a
minor but real verifiability tax on future auditors.

## What's solid

- The landed CI mechanics check out against the actual files: the
  perturbed-rebuild gate (`ci.yml:799-855`, job `Reproducible build check
  (Linux)`) really does rebuild from a different workspace path under
  `TZ=Pacific/Kiritimati`, `LC_ALL=C`, `umask 077`, and diffs jar +
  `bom.json`, with a diffoscope-upload step gated on failure.
- `.buildinfo` publication is real and matches the stated invariant: it
  runs in the same Maven session as the release build
  (`release.yml:90/95/114`), so it records the checksums of the artifacts
  actually attached — I confirmed this is a single job, not split across
  the `release`/`maven-registry` jobs.
- `docs/reproducibility.md`'s declared-scope table, the #184
  installer/container boundary language, and the explicit "not yet
  reproducible" honesty about msi/dmg are present and consistent with
  what #184's own issue text claims — no scope-honesty violation found.
- The out-of-scope boundary against #184/#188/#189/#180-183 is drawn
  consistently across #185's and #184's issue bodies (I read both); no
  ownership contradiction between the two.

## Verdict rationale

`needs-rework`: the landed 80% is verifiably real, but the single
remaining acceptance criterion (P2) targets a recipe that cannot prove
what the issue claims it proves (finding 1), and the issue's own
tracking discipline — which it insists on elsewhere (§7's "every response
ends in a REPLAN:" rule, rule A) — is already violated by an
undocumented, unlinked child (#819) filed the same day the body was last
touched (finding 2). Both are fixable without re-doing any landed work,
but leaving them as-is risks a closing comment that reads as proof of
release-path reproducibility while actually proving only registry-path
parity.
