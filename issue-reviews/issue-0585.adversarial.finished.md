# Issue #585: FEAT-C35-2: the manual is on the web at a per-release URL a student can paste into an assignment, and in-app help offers the same page in a browser
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The core idea — a versioned, linkable hosted manual with computed
"open in browser" links from the existing in-jar viewer — is a reasonable
next step given ARCHITECTURE.md's own recorded "hosted docs are the planned
future" note. But the issue is written as if the hosting mechanism, the
retention model, and the search bar are all solved problems needing only
implementation, when none of them exist in this repo today (no Pages
workflow, no site generator, no search infra), and it leans hard on a
sibling issue (#584) for a guarantee (#584: 519) that #584 does not yet
promise. One acceptance criterion is textually broken as written.

## Findings, most severe first

### 1. AC-1's own text is nonsensical as an acceptance criterion

`AC-1: publishing is part of the release procedure, not a manual step
someone remembers — cutting a release emits `//` and repoints `latest`.`

Read literally, the criterion says a release "emits `//`" — an empty,
un-filled placeholder where a versioned path (`/5.0.x/`, per the Outcome
paragraph two lines above) was clearly intended. As currently worded this
AC cannot be checked against, because it does not name what gets emitted.
This is not a nitpick about prose quality: every other AC in this issue is
phrased as a checkable assertion, and this is the one that gates whether
publishing is automated at all. **Recommendation:** fix the AC text to name
the actual artifact path scheme (e.g. "emits `/<version>/` and repoints
`/latest/`") before anyone scopes work against it.

### 2. AC-3 depends on a guarantee #584 has not made

`AC-3: every hosted page's URL is derivable from its in-jar topic id, so
the in-app "open in browser" link is computed rather than hand-maintained.`

I read #584 (FEAT-C35-1, the source-tree/build-pipeline issue this one is
`ordering_after`). #584's own five ACs say nothing about topic-id
stability across the migration it performs — they cover single-source
build, `HelpTopicsTest` passing unchanged, offline completeness, a
viewer-safe subset, and a byte-auditable diff. The only place topic-id
stability appears is in **this issue's own posted comment**, which
explicitly *asks* #584 to add it: *"AC-3 depends on a contract #584 must
preserve... #584 AC-5's byte-auditable diff should therefore record
topic-id stability explicitly... a migration that silently renames ids
passes #584 and breaks every computed 'open in browser' link here."* That
request has not been reflected back into #584's issue body (I re-fetched
#584 directly; its ACs are unchanged). So #585 AC-3 currently rests on a
contract that is, at best, a comment-only request against an issue that
has not accepted it. **Recommendation:** do not start #585's AC-3 work
until #584's issue body itself commits to topic-id stability (not just a
cross-issue comment); until then, `ordering_after: [FEAT-C35-1]` is
necessary but not sufficient.

### 3. No hosting target is named, and "the release procedure" implies out-of-band setup this issue doesn't budget

Nothing in the issue says *where* `/5.0.x/` and `/latest/` are served
from. I checked: there is no `gh-pages` branch, no
`actions/deploy-pages`/`actions/upload-pages-artifact` step anywhere in
`.github/workflows/*.yml`, and no custom-domain config (`CNAME`) in the
repo. Every other publishing surface this project has (Maven registry,
container registry, SignPath, GPG signing) required a documented one-time
maintainer enrollment step, visibly called out in `release.yml`'s comments
(e.g. "Enrollment (one-time, maintainer): apply at signpath.org...").
FEAT-C35-2 needs the equivalent — enabling GitHub Pages (or standing up an
alternative host), possibly a custom domain/DNS/TLS for a stable
per-release URL "a student can paste into an assignment" — and none of
that is named as a prerequisite, gated, or priced. **Recommendation:** name
the hosting mechanism explicitly (GitHub Pages is the natural default given
the rest of the project's GitHub-native tooling) and add the one-time setup
step to the issue the way `release.yml` documents SignPath/GPG enrollment.

### 4. AC-2's "keeps serving that older version's content" has no retention or pruning policy

`AC-2: ... an older version's URL keeps serving that older version's
content after a newer release.` Taken at face value this is a permanent,
unbounded retention commitment: every historical release's manual stays
live forever. For a project already several major/minor versions in (5.0.x
is current per the README) with an active release cadence, this is
indefinite storage growth with no stated cap, archival policy, or even an
acknowledgment that it's a deliberate choice. Compare to how carefully this
project treats other unbounded-growth risks (e.g. the checkpoint-writer
coalescing, the GPG key custody rationale) — here there's silence.
**Recommendation:** either state the retention policy explicitly (e.g.
"all tagged releases, pruned never, because HTML is cheap") so it's a
recorded decision rather than an accident, or bound it (e.g. last N minor
lines plus `latest`).

### 5. AC-5 is two criteria bolted together, and the harder half has no verification mechanism at all

`AC-5: search returns the expected page for a spot-check set of terms
committed as fixtures, and the site degrades to a usable static browse
with scripting disabled.`

The first half is gameable in the classic way: a small, implementer-chosen
fixture list can be made to pass trivially (pick terms that are page
titles) while general search quality — typo tolerance, multi-word
queries, terms that appear in several pages — is never checked, and a
regression that breaks search for everything *except* the committed
fixture set still passes CI forever. The second half — "usable static
browse with scripting disabled" — names no fixture, no CI check, and no
definition of "usable" (can you reach the element reference from the TOC?
does pagination work? are there no-JS fallback links?) anywhere in this
issue or in #584/#587's ACs. Unlike every other AC in this issue (and the
sibling issues), AC-5's second clause is entirely unfalsifiable as written.
**Recommendation:** split into two ACs; give the no-JS-degradation clause
an explicit, checkable test (e.g. "curl the hosted TOC page with JS
disabled and assert the element-reference and batch-interface links are
present as plain `<a href>` tags"), and widen the search fixture set's
purpose statement so it's understood as a regression floor, not full
search-quality coverage.

### 6. AC-4's CI cadence is unspecified, so "same strictness" can be satisfied by a much weaker check

`AC-4: link integrity across the hosted site is checked in CI with the
same strictness the in-jar tree already gets — a dead cross-link fails the
build.` The in-jar comparator, `HelpTopicsTest`, runs on every `mvn
verify`, i.e. every push and PR (ARCHITECTURE.md: "`mvn verify` runs the
suite... keep it green"). AC-4 never says the hosted-site checker runs on
that same cadence. Since AC-1 only requires publishing at release time, an
implementation that runs the hosted link checker *only* during the
release-cutting job (not on every PR) would satisfy AC-4's literal text —
"checked in CI," "fails the build" — while giving contributors far weaker,
much-later feedback than the in-jar tree gets today, contradicting the
"same strictness" claim in spirit. **Recommendation:** state explicitly
that the hosted-site link checker runs against the generated (not yet
published) site on every PR/push, the same cadence as `HelpTopicsTest`,
with publishing as a separate, later step gated on the same check passing.

### 7. Client-side search dependency's license is unaddressed, unlike every other third-party choice this project has made

This codebase is visibly careful about license compatibility for anything
that ships to users: FlatLaf's Apache-2.0-with-zero-transitive-deps status
got its own evaluation doc before adoption
(`docs/flatlaf-evaluation-2026-07.md`), and the plugin-trust-boundary
decision explicitly calls out avoiding "GPLv3 in-process-linking hazards
(e.g. ELK's EPL-2.0)." A client-side search library is a new third-party
dependency shipped to every visitor of the hosted site, and this issue
names no candidate, no license check, and no size/CDN-dependency
constraint (a "no scripting" degradation requirement in AC-5 implies the
search JS must also not silently depend on a third-party CDN — GPLv3
distribution and self-hosting norms this project already follows
elsewhere for its own assets). **Recommendation:** name the search
mechanism's license and hosting model (self-hosted static JS, not a
CDN-fetched or SaaS search service) explicitly, with the same rigor
FlatLaf got.

### 8. Feasibility: `band_mw: "1-2"` looks underpriced once the missing infrastructure is counted

Once findings #2–#7 are taken seriously, the real scope includes: a
from-scratch static-site publishing pipeline integrated into a release
workflow that currently has zero Pages/hosting steps; one-time,
human-in-the-loop hosting enrollment (finding #3); a retention policy
decision (#4); a search feature with both a fixtures test and an
unaddressed no-JS-fallback requirement (#5); a CI job running on a new
cadence against a target that doesn't exist yet (#6); and a vetted,
licensed client-side dependency (#7) — stacked on top of a hard
prerequisite, #584, priced at 2-3 mw and itself not started. Two other
FEAT-C35 siblings inspected for this review (#586, #587) show the same
pattern of comment-thread diligence catching cross-issue seams that the
issue bodies themselves don't reflect back — worth noting as a project-wide
pattern, not unique to #585, but it means the machine-block `band_mw`
estimates in this cluster should be treated as provisional rather than
committed.

## What's solid

- The comment thread's dedup pass against #584 and #587 is genuinely
  careful — it correctly separates "link integrity" (this issue's AC-4:
  does the link resolve) from #587's "claim accuracy" (does the resolved
  page say something true), with a concrete example showing both failure
  modes can occur independently. That's the right cut.
- The boundary section's non-negotiable offline-parity constraint ("Nothing
  in the in-jar experience may come to depend on network access") is
  correctly restated from #584 and consistent with the "single self-contained
  jar" story `README.md` and ARCHITECTURE.md both tell — a good guardrail
  against scope creep into the in-jar viewer.
- Explicitly deferring screenshots to #586 and store-listing copy to CAP-34
  keeps this issue's surface from ballooning into adjacent work.
- The core direction (versioned URL, computed rather than hand-maintained
  cross-links) is sound engineering, contingent on #584 actually delivering
  the topic-id stability it needs (finding #2).

## Recommendation

Needs rework before implementation starts: fix AC-1's broken text (finding
#1), get topic-id stability into #584's own acceptance criteria rather than
resting on a comment (finding #2), name the hosting mechanism and its
one-time setup cost (finding #3), and decide the retention policy (finding
#4). AC-5 and AC-4 as written let an implementation satisfy the letter of
"link integrity" and "search works" while leaving real gaps — a no-JS
visitor with a broken TOC, or search that only ever gets tested against its
own fixture list — that the issue's stated verification would never catch.
