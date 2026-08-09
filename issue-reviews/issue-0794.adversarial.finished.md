# Issue #794: TASK-C585-1: cutting a release publishes the manual under a versioned path and repoints latest, and an older version's URL keeps serving its own content
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#794 is the task-level slice of FEAT-C35-2 (#585) that actually has to stand up
the publish pipeline: release-time emission under a versioned path, a `latest`
alias, and old-version URL survival. It inherits several defects straight from
its parent feature issue without repair, and adds a couple of its own: an
unimplementable AC-1, an AC-2 that names content no upstream task in the
cluster demonstrably produces, an AC-3 with a bootstrap/verification problem,
and a hosting mechanism that is named nowhere in this issue or in the repo's
CI. `band_mw: 0.5-1` does not look priced for what remains once those are
accounted for.

## Findings, most severe first

### 1. AC-1's text is literally broken — inherited unfixed from #585

> "Publishing runs as part of the release procedure: cutting a release emits
> the site under `//` and repoints the `latest` alias, with no manual step."

`//` is an empty double-slash, not a version placeholder (`/<version>/`, per
the Outcome paragraph's own `/5.0.x/` example one section up in #585). This is
the exact defect flagged against parent issue #585's identically-worded AC-1
in the companion review of that issue (`issue-reviews/issue-0585.adversarial.md`,
finding #1) — it was copied into this child task and still not fixed. As
written, the criterion names no artifact path, so no one can check against
it. **Recommendation:** fix the text to name the scheme explicitly (e.g.
"emits `/<version>/` and repoints `/latest/`") before scoping implementation.

### 2. No hosting mechanism is named, and none exists in this repo's CI today

Nothing in #794 says where the versioned site is served from. I checked
`.github/workflows/*.yml` directly: no `actions/deploy-pages` or
`actions/upload-pages-artifact` step, no `pages:` permission block, no
`gh-pages` branch reference, no `CNAME`. Every other publishing surface this
project has (Maven registry, `ghcr.io` container image, SignPath Authenticode
signing, GPG release signing) gets an explicit one-time-enrollment note
inline in `release.yml` (e.g. "Enrollment (one-time, maintainer): apply at
signpath.org…", line ~571). Standing up a new host (GitHub Pages is the
obvious fit given the rest of the project's GitHub-native tooling, but is
never named) needs the equivalent — and AC-1's "no manual step" claim is
about the *steady-state* release procedure, but says nothing about the
one-time setup this task must still budget and document.
**Recommendation:** name the hosting target explicitly and add its
enrollment step to this issue's scope, with the same treatment `release.yml`
already gives SignPath/GPG.

### 3. AC-2 names content that no upstream task in this cluster demonstrably produces

> "The hosted manual is live carrying at least the element reference and the
> batch-interface guide."

I checked `resources/help/**` (what #793/TASK-C584-3 mechanically migrates
per its AC-1: "existing help content is migrated... by a mechanical
conversion"). There is no consolidated "element reference" page —
`resources/help/elements/overview.html` is a narrative tour that links out to
~25 separate per-category pages (`gates/and.html`, `components/mux.html`,
`memory/register.html`, etc.), not a single reference document. And there is
zero mention of "batch interface" anywhere under `resources/help/` — the
batch/grading contract lives at the repo-root `docs/batch-interface.md`, a
document neither #584 nor #793's issue body claims to fold into the help
source tree. So AC-2 either (a) is trivially true by calling the existing
scattered per-element pages "the element reference" collectively — in which
case the AC is unfalsifiable window-dressing — or (b) silently assumes new
synthesis work (a real consolidated reference page, and porting
`docs/batch-interface.md` content into the help pipeline) that no issue in
the FEAT-C35 cluster (#584, #793, #792, #585) actually owns.
**Recommendation:** either point AC-2 at content that concretely exists post-
migration, or add an explicit AC to #793/#584 committing to produce it, and
cross-reference it from here the way AC-4 already does for TASK-C584-2.

### 4. AC-3 has an unaddressed bootstrap problem and no named verification mechanism

> "An older version's URL keeps serving that older version's content after a
> newer release, verified against a previously published version."

On the very first release cut under this new pipeline there is no
"previously published version" under the new versioned-path scheme to check
regression against — the check needs either two real throwaway releases (the
existing `workflow_dispatch` dry-run path in `release.yml` explicitly
publishes nothing, per its own comment: "nothing is published") or a
synthetic two-release simulation, and #794 names neither. Contrast this with
its own sibling tasks: #792's AC-2 cites a concrete, already-existing
automated check (`HelpTopicsTest`), and #793's AC-2 requires a diff report
that "fails the migration" on an unexplained difference. #794's AC-1/AC-2/
AC-3 name no test or script at all — only AC-4 does, by pointing at "the
in-jar offline test from TASK-C584-2." Without an equivalent for AC-3, "keeps
serving... verified" can be satisfied once by a human demo at implementation
time, and a later regression (a deploy script that overwrites the previous
version's directory, a `latest` symlink race) has nothing catching it in CI
afterward. **Recommendation:** name a concrete, repeatable check — e.g. a CI
job that cuts two synthetic version directories against a scratch Pages
target (or a local static-file assertion) and asserts the first is still
byte-identical after the second is emitted.

### 5. Retention/pruning policy is unstated — an unbounded, indefinite commitment

"An older version's URL keeps serving that older version's content after a
newer release" is, read literally, a permanent-retention promise: every
historical release's manual stays live forever, with no cap, archival
policy, or acknowledgment that this is a deliberate choice. If GitHub Pages
ends up the host (the natural default per finding #2), that publishes to the
`gh-pages` branch or a Pages artifact with GitHub's documented recommended
size guidance in play over a multi-year release cadence. This project is
otherwise careful about naming unbounded-growth risks as recorded decisions
(the checkpoint-writer coalescing, the GPG key-custody rationale in
`SECURITY.md`) — this one is silent. **Recommendation:** either state
explicitly that all tagged releases are retained forever because static HTML
is cheap, or bound it (e.g., last N minor lines plus `latest`), and record
which.

### 6. `band_mw: 0.5-1` looks underpriced once findings #2–#5 are counted

Every other publishing surface in this repo (Maven registry, `ghcr.io`,
SignPath, GPG) was bolted onto CI infrastructure that already existed for a
different artifact type. #794 is the first task in this project that has to
build a *new* publish target from nothing: hosting enrollment (#2), a
versioned-path emission + `latest` repoint mechanism with retention decisions
(#5), and a genuinely-verifiable regression check for old-version survival
that doesn't exist yet (#4). That is a wider scope than TASK-C584-2/-3
(#792/#793), each priced at 1-1.5 mw for "only" generator/migration work
against infrastructure (`mvn`, the existing help tree) that already exists.
0.5-1 mw for #794 reads optimistic once the missing infrastructure is
counted, echoing the same pattern the sibling review of #585 flagged for the
parent feature's own estimate.

## What's solid

- AC-4 (offline parity) is concrete and correctly delegates to a named,
  already-scoped test from TASK-C584-2 (#792 AC-3) rather than re-litigating
  it — the right way to cite a cross-issue contract.
- The underlying goal — a versioned URL that survives further releases, so
  an instructor's syllabus link stays valid — is well-motivated and matches
  ARCHITECTURE.md's own recorded "hosted docs are the planned future" note.
- The task's ordering chain (`TASK-C584-1` → `TASK-C584-2` → `TASK-C584-3` →
  this issue) is internally consistent, and this issue correctly precedes
  `TASK-C585-2` (#795), which depends on a live hosted site to add search and
  computed cross-links onto.

## Recommendation

Needs rework before implementation starts: fix AC-1's broken placeholder text
(finding #1) rather than carrying the same defect a second time past its
parent issue, name the hosting mechanism and its one-time enrollment cost
(finding #2), resolve whether AC-2's named content actually exists anywhere
in the pipeline this issue depends on (finding #3), give AC-3 a concrete,
repeatable verification mechanism instead of a one-time "verified against a
previously published version" demo (finding #4), and record a retention
policy (finding #5). As written, an implementation could satisfy every AC's
literal text — emit *something* at *some* path once, demonstrate old-URL
survival by hand once — while leaving the actual goal (a durable, automated,
syllabus-safe versioned manual) unmet.
