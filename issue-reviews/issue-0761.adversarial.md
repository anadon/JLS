# Issue #761: TASK-C577-1: the CSE 260M corpus lands in tree as committed compatibility fixtures with recorded provenance
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of what was checked

Fetched #761 (open, zero comments). Fetched its stated context: parent #577
(FEAT-C33-3, both comments — including the 2026-08-08 "ORDERING CORRECTION"
posted the same day as this review), sibling tasks #763 (TASK-C577-2, the CI
lane) and #765 (TASK-C577-3, licensing/kit), and #509 (the WashU/Siever
adoption issue this task's boundary references). Read `README.md`,
`ARCHITECTURE.md`, `CONTRIBUTING.md`, `.gitattributes`, and
`test/fixtures/legacy-4.1/README.md` (the closest existing precedent for a
provenance-tracked external corpus, from the closed #56). Grepped the tree
for a "large-fixture policy" (CONTRIBUTING.md, docs/, .gitattributes) — none
exists. Checked prior sibling review `issue-reviews/issue-0763.adversarial.md`
for cross-referenced defects. Fetched `github.com/bsiever/JLS` directly to
verify the corpus's claimed source.

## Findings, most severe first

### 1. The task's own `ordering_after` reintroduces the exact dependency cycle #577 corrected today

> `ordering_after: [509]`

#577's 2026-08-08 "ORDERING CORRECTION" comment (posted the same day as this
review, on the parent this task belongs to) diagnoses and fixes precisely
this: a two-node cycle where #577 declared `ordering_after: [509]` while
"#509's AC-3 (corpus passes load + simulate + grade) is satisfied by AC-1
here" — neither side could move first. The fix, verbatim from that comment:

> `ordering_after_by_criterion: AC-1: [] # ready now, AC-2: [] # ready now,
> AC-3: [509], AC-4: [509, 578, 576]`

TASK-C577-1 (this issue) is exactly the task that executes #577's AC-1/AC-2
— the fixtures-only, no-licensing-required half the correction says is
"ready now" with no #509 dependency. But #761's own machine block still
carries `ordering_after: [509]`, unchanged since filing on 2026-08-04 (four
days before the correction) and with zero comments since. If this edge is
honored, the cycle is back: #509 AC-3 waits on #577 AC-1, #577 AC-1 is
implemented by #761, and #761 waits on #509. The correction fixed the
feature-level edge but nothing propagated it down to the task that actually
carries the work.

**Recommendation:** clear #761's `ordering_after` to `[]` (or explicitly
cite #577's 2026-08-08 correction and mirror its AC-1/AC-2 edge), matching
what the parent issue now says should be true.

### 2. No cited source establishes that the actual lab-circuit corpus is publicly obtainable at all — the only verified public artifact is the tool, not the content

The outcome statement asserts the corpus "stops being an external artifact
... and becomes committed repository content" as if the artifact is sitting
somewhere ready to pull in, "independently of the kit half" and "requir[ing]
no content-licensing agreement." Tracing the demand evidence back: #509
cites (a) the WashU course site directing students to *download the JLS
tool* from bsiever's releases, and (b) an ACM workshop paper *about* the
pedagogy — neither is a pointer to a corpus of lab `.jls` files. Fetching
`github.com/bsiever/JLS` directly confirms it: GPL-3.0-licensed tool source
and release binaries only, no lab circuits, no course content of any kind.
Nothing in #509, #577, or #761 names a public location for the actual
CSE 260M lab circuits. If the only route to those files runs through asking
Dr. Siever directly for his lab set, that is itself a permission
conversation — the very thing #577's boundary reserves for AC-3 ("content
licensing is settled in writing with Dr. Siever before any adapted material
ships") and that #509's AC-1 ("written criteria list... confirmed by
Dr. Siever") hasn't happened yet. The claim that fixtures "require no
content-licensing agreement" quietly assumes the files are already legally
available for redistribution by virtue of being unmodified; it does not
establish that they are available to this project *at all*.

**Recommendation:** name the actual source of the corpus files (a public
repo, a public course archive, or "obtained directly from Dr. Siever under
an as-yet-undocumented informal permission") before treating this as
independent, license-free, startable-now work. If the source turns out to
require instructor contact, say so plainly rather than implying the corpus
is sitting in the open waiting to be `git add`ed.

### 3. "The repo's large-fixture policy" does not exist, so the AC is either vacuous or an unstated hard blocker

> "Corpus size on disk is stated and the repo's large-fixture policy is
> applied before it lands."

Grepped `CONTRIBUTING.md`, `docs/**`, and `.gitattributes` for any large-file
or size-cap policy: none exists. `.gitattributes` only sets `-text` on
`*.jls`/`test/resources/**` for CRLF safety (issue #111) — nothing about
size thresholds, Git LFS, or exclusion rules. This is the same defect class
already confirmed on sibling #763 (its finding 1: an AC that leans on a
CI-lane-budget policy that is itself unbuilt and undeclared as a
dependency). As written, an implementer can satisfy this bullet by writing
"no large-fixture policy exists, so none was applied" — technically true,
substantively empty — or could stall indefinitely waiting on a policy
nobody has committed to writing.

**Recommendation:** either state an interim size threshold in this issue
directly (e.g., "flag for maintainer review if the corpus exceeds N MB"),
or add a dependency on whatever issue is meant to define that policy (none
was found in the tracker during this review — worth confirming one exists
before citing it as a gate).

### 4. Per-file license triage and omission are implied by the parent's correction but never appear in this task's acceptance criteria

#577's correction comment, section 4, states the actual authorization this
task carries: "If any file's redistribution licence is not already clear
from its published source, that file waits for AC-3 like the rest of the
kit; the fixture set is allowed to be smaller than the corpus and must say
which files it omitted and why." #761's acceptance criteria never mention
per-file triage or a required omission-with-reason record — only that
"the permission under which it is present" is recorded for files that do
land. Nothing forces an implementer to document *what was excluded and
why* when a file's rights are unclear, which is exactly the "quiet
deletion" failure mode #763's AC-2 was written to prevent for regressions
— here it applies to files that never enter the corpus at all, and no
analogous safeguard exists in this issue.

**Recommendation:** add an explicit AC requiring that any file excluded on
licensing-clarity grounds is named with a stated reason, mirroring the
mechanism #577's correction already describes and #763's AC-2 already
models for a different case.

### 5. Effort estimate does not obviously account for findings 1-2

`band_mw: 0.5` — half a maintainer-week — is priced for "commit files,
write provenance, apply a size policy." It does not obviously price in:
locating a corpus whose public existence this review could not confirm
(finding 2), or resolving an ordering cycle that currently blocks the task
outright (finding 1). If the corpus turns out to require direct
instructor contact, this estimate is for a different, smaller task than
the one actually in front of an implementer.

**Recommendation:** re-estimate once finding 2 is resolved (source named),
or split "locate and confirm corpus source" out as a small, separate
pre-step so the 0.5 mw figure isn't silently absorbing an open-ended
unknown.

### 6. Minor: "the corpus's relationship to #509 is stated in tree" has no named location

Could be satisfied by one sentence anywhere under `test/` — a fixture
README, a code comment, a commit message — none of which is "in tree" in
a way a later reader would reliably find. `test/fixtures/legacy-4.1/README.md`
is a good precedent for where this kind of provenance note belongs; #761
doesn't point to it or an equivalent.

**Recommendation:** name the file (e.g., a `test/fixtures/cse260m/README.md`
provenance note, mirroring the legacy-4.1 precedent) directly in the AC.

## What's solid

- The scope boundary against #763 (CI lane) and #765 (licensing/kit) is
  clean and matches the parent #577's own stated split — no re-litigation
  of provenance or licensing bleeding into the wrong task.
- "No adapted or rewritten course material is included in this task" is a
  clear, testable negative constraint that would catch obvious scope
  creep into the kit half.
- Landing the license-clean half first, ahead of the #509 conversation, is
  the right instinct in principle and matches #577's own corrected
  sequencing intent (finding 1 is that #761 itself hasn't caught up to that
  correction, not that the intent is wrong).

## Verdict rationale

Two of the six findings are not nitpicks: the task's own ordering edge
currently recreates a dependency cycle its parent explicitly diagnosed and
fixed the same day (finding 1), and the premise that a redistributable
corpus of lab circuits is sitting somewhere accessible, independent of any
conversation with Dr. Siever, is unverified — the one public artifact this
review could actually check (bsiever/JLS on GitHub) contains the tool, not
course content (finding 2). Both are fixable by editing the issue (clear the
stale ordering edge; name the corpus source or admit it isn't known yet)
rather than by discarding the task, so this is needs-rework rather than
should-not-proceed. The remaining findings (nonexistent large-fixture
policy, missing omission-and-reason mechanism, unpriced unknowns) should be
folded into the same edit pass rather than discovered mid-execution.
