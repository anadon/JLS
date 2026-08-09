# Issue #781: TASK-C589-1: the grading-contract white paper exists as one instructor-facing document — stability, determinism and provenance, readable as a PDF handout with no repository access
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

#781 is a small (band 0.5-1 mw) task carving three sections (batch stability,
determinism, provenance) off the parent feature #589 (FEAT-C36-2), with
enforcement citations, kit-linking and the limits section correctly deferred
to the sibling task #783 (TASK-C589-2, confirmed to exist and to match this
split). That decomposition is sound. The problems are in the machine-block
edge and in what the issue asks a contributor to assert as fact about
interfaces that do not exist yet.

## Findings, most severe first

### 1. (High) `ordering_after: [300, 524]` repeats an edge-shape defect the repo's own review process fixed on sibling issues today

The issue's YAML orders this task behind **#300**, the CAP-06 capstone. But
today's (2026-08-08) review pass on #300 itself explicitly withdrew this
exact pattern from two sibling issues and stated the standing rule:

> "A capstone closes only when everything beneath it closes. So
> `ordering_after: [300]` does not mean 'after the CAP-06 verdict slice' —
> the thing its author plainly intended — it means 'after the last of
> CAP-06', including the 25–36 mw handout-library half that #508 explicitly
> dropped from near-term scope... **Standing guidance for future
> consumers: order after #466 for the verdict engine, report and exit
> status 3; order after #369 only for properties, equivalence or coverage...
> Do not order after #300.**" (#300, comment 5227458020)

That comment demoted `ordering_after: [300, 369]` to `[466]`-style edges on
#576 and #755 for precisely this reason. #781 carries the same `300` edge
those fixes removed, filed at the same time (2026-08-04) as the pattern that
was later ruled illegal, and nobody has mirrored the fix onto #781 or its
parent #589 (whose `ordering_after` also cites "#300 CAP-06"). As written,
this task is nominally blocked on CAP-06's entire ~50-80 mw required set
(FEAT-053, FEAT-005/006/007, FEAT-016/017, FEAT-003/015 residuals) rather
than on the one artifact (#466) it actually needs content from.

**Recommendation:** change `ordering_after` to name #466 (and #524, which is
already correctly named) instead of #300; file the same correction on #589.

### 2. (High) The dependency chain it *does* correctly cite is itself mid-revision and largely unbuilt — AC "no guarantee is invented" is at risk

Section (a) is required to consume "#524's policy" for the batch-interface
versioning rule. But #524 posted a self-correcting revision comment on
2026-08-08 (today) that:
- dropped #369 from its own `ordering_after`, narrowing its real
  prerequisite to **#466 alone**;
- confirms #466's headless verdict channel — `jls.sim.Expectations`,
  `TestVectorRunner`, `GradeReport`, the `-check`/`-report` flags, **exit
  status 3**, and the `docs/batch-interface.md` §1/§2.5/§6 changes — "does
  not exist yet";
- confirms `CliContractConformanceTest` does not exist and that at the
  cited commit `grep -n '"check"\|"report"' src/jls/JLSStart.java` returns
  nothing.

Grounding this in the current tree: `docs/batch-interface.md:36-40` documents
exactly three exit statuses (0/1/2); there is no fourth. `src/jls/JLSStart.java`
has no `-check`/`-report` flag and no contract-version query. So "the frozen
batch-interface contract" section (a) is meant to describe **does not exist
yet**, and per #524's own comment thread its shape is still being
renegotiated between reviewers as of today. Given #781's tiny cost band, a
contributor could plausibly pick it up believing it's a quick documentation
task, find nothing frozen to cite, and either stall or paraphrase #524's
*proposed* policy as though it were shipped — directly violating #781's own
"No guarantee is invented here" clause. The issue should say explicitly that
picking this up before #524 (and transitively #466) lands is out of scope,
not just imply it via `ordering_after`.

### 3. (Medium) "published at a stable URL" is untestable and in tension with "no repository access"

AC-1 requires the paper to be "published at a stable URL" and simultaneously
"self-contained enough to read as a PDF handout with no repository access."
Checked in-tree: there is no docs-hosting mechanism. `ARCHITECTURE.md`'s own
recorded decision ("Help delivery: in-jar now, hosted docs are the planned
future") states hosted, versioned web documentation is *not yet built*.
`.github/workflows/` has six workflows (`ci`, `repro-installers`,
`scorecard`, `release`, `mutation`, `codeql`) and none of them deploys a docs
site (no Pages/mkdocs/readthedocs config anywhere in the tree). So the only
URL achievable today is a GitHub blob/raw URL into this repository — which
*is* repository access, and which is not "stable" in the sense a course
committee would need (branch-relative blob URLs move on rewrite; the
commit-pinned form is stable but not the kind of link one hands a
department). Unlike its sibling #589 (whose AC-4 pins the kit-link
requirement to "a link check keeps that reference alive"), #781 names no
verification mechanism for AC-1 at all — a contributor can satisfy the
checkbox by committing the file and picking any URL, without the artifact
ever surviving being forwarded outside the repo.

**Recommendation:** either name the concrete hosting target (e.g. GitHub
Pages off `docs/`, README-linked raw file, or explicitly "no external
hosting yet — the in-tree path is the interim stable URL") and add a
verification step, or drop "published at a stable URL" from this task's AC
until a hosting mechanism exists.

### 4. (Medium) Sequencing gap with TASK-C589-2 could let an unenforced promise reach instructors

#781 explicitly excludes "enforcement citations" (which test/ratchet backs
each claim) — that's #783's job. But #781's own AC-1 has this task publish
the document at a stable URL on its own, with no gate tying "published" to
#783 having landed first. The parent feature #589's design intent is
explicit that "no claim in the paper stands alone" and a "skeptical reader
can verify rather than trust" (#589 AC-3, restated in its own boundary
comment). If #781 merges and publishes before #783, JLS is presenting a
grading-contract document with unenforced claims to exactly the audience
(a course committee deciding whether to adopt JLS for grading) this feature
exists to protect — the failure mode CAP-36 is designed to prevent.

**Recommendation:** either merge #781 and #783 as one PR/landing event, or
add an explicit AC to #781 that publication is gated on #783, or mark the
first-published revision of the document as a draft until enforcement
citations land.

### 5. (Low) "PDF handout" is a metaphor with no artifact requirement — mildly gameable

Nothing in the AC requires an actual PDF to be generated or exist anywhere;
"readable as a PDF handout" only constrains self-containedness of the prose.
A literal in-tree Markdown file that never gets rendered to PDF technically
satisfies every checkbox. Likely intended, but worth a one-line clarification
that no PDF-rendering deliverable is required, since the title otherwise
reads as promising one.

## What's solid (no action needed)

- The boundary split with TASK-C589-2 is real and consistent: #783 exists,
  is titled "TASK-C589-2: every guarantee ... names the test ... limits ...
  CAP-21's kits link it," and its scope is exactly the complement #781
  claims to exclude.
- "Competitor claims belong in #588's notes, not here" is consistent with
  #588's own scope (FEAT-C36-1, comparison notes) — no overlap.
- The three-section structure (a/b/c) matches #589's AC-2 verbatim; no drift
  between task and parent on *what* the document must say, only on *when*
  it can honestly be written (Finding 2).
- `part_of_feature: 589` correctly identifies the parent, even though the
  GitHub-native sub-issue relationship is not set (`has_parent: false` via
  `issue_read`/`get_parent`) — a tooling gap worth a maintainer note, but not
  a defect in this issue's content.

## Verdict rationale

`needs-rework`: the content plan is sound and well-bounded, but the ordering
edge is a known-bad pattern already corrected on sibling issues today, the
transitive dependency chain is actively unstable and largely unbuilt (risking
an "invented guarantee" if worked prematurely), and the publication
criterion has no defined target or verification mechanism. These are fixable
without re-scoping the task's actual content.
