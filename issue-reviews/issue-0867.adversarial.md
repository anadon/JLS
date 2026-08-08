# Issue #867: TASK-C590-2: a release-announcement checklist exists in-tree — what the writeup must show, which venues, who posts — so a flare moment is not spent on a bare git tag
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: sound-with-concerns

## Summary of what was checked

Fetched #867 (open, no comments) plus its lineage: parent feature #590
(FEAT-C36-3, open, 1 comment) and the issue it cites, #588 (FEAT-C36-1,
open). Confirmed sibling task #866 (TASK-C590-1, README/site paragraph).
Confirmed `docs/release/`, `docs/comparisons/`, and
`docs/reviews/evidence/2026-08-niche-survey/` do not exist yet in this
checkout. Read README.md, ARCHITECTURE.md, CONTRIBUTING.md, SECURITY.md,
and `docs/wayland-desktop-checklist.md` as the repo's existing precedent
for a per-release checklist.

## Findings, most severe first

**1. No verification mechanism is named, and every AC is satisfiable by empty-calorie prose (gameable acceptance criteria).**
AC-1 through AC-5 each require the checklist to "name," "state," or
"define" something, but nothing in the issue ties any of those to a test,
a link-checker, or a review rubric. Contrast this with the immediate
sibling and cousin issues in the same cluster: #866 AC-2 requires "a
committed drift check," #588 AC-1 gives a hard word cap ("~2500 words")
and AC-2 requires a runnable, reproducible appendix. #867 has no
analogous teeth. A contributor can satisfy AC-1's "target venues this
niche actually reads" by listing generic venues (Hacker News, r/FPGA)
with zero research backing, and AC-3's "flare moment" definition by
writing one sentence that is technically present but useless in
practice — and the PR would pass review by the letter of the issue.
*Recommendation:* add a concrete check, e.g. a word/line cap enforced by
a script (mirroring #588's numeric precedent) and require the venue list
to cite why each venue was chosen (a decision record, not just names).

**2. AC-4's "short enough to be followed in one sitting" is unquantified where sibling issues set numbers.**
Quote: "AC-4: It is short enough to be followed in one sitting; anything
longer is a document the moment will outrun." No word count, line count,
or page limit is given. #588 AC-1 (same cluster, same day) sets "~2500
words" as a concrete cap for a much more elaborate deliverable
(comparison notes with runnable appendices). A one-sitting checklist
should be trivially cheaper to bound numerically, yet #867 uses only
evocative language. *Recommendation:* borrow the pattern already in the
cluster — state an explicit cap (e.g. "under 400 words" or "fits on one
screen without scrolling").

**3. AC-1's "who posts" is close to vacuous in a declared single-maintainer project.**
CONTRIBUTING.md and SECURITY.md both describe JLS as a "single-maintainer
project" (SECURITY.md line 87: "revoke a signing key on a
single-maintainer project"). With one maintainer (anadon), "who posts" has
exactly one possible non-trivial answer, so requiring the checklist to
"concretely name" it adds a line of boilerplate rather than forcing a real
decision. This isn't false, just low-value as stated — unless the intent
is to future-proof for a multi-maintainer state the issue never mentions.
*Recommendation:* either drop this clause or reframe it forward-looking
("who posts, and who is the fallback if the maintainer is unavailable at
the moment the demo lands").

**4. #867 restates content owned by #588 (FEAT-C36-1) with no drift-prevention mechanism, unlike its sibling task.**
Quote: "AC-2: It states the claim standard inherited from FEAT-C36-1 — any
competitor comparison in it cites that competitor's own tracker and is
re-checked against their current release at posting time." This is a
second, independent restatement of #588's AC-3/AC-4 language (#588 is
itself open and unimplemented — `docs/comparisons/` does not exist in
this checkout). The sibling task #866 was given an explicit mechanism for
exactly this kind of cross-issue restatement risk (AC-2: "A committed
drift check fails when the two copies diverge," AC-5 ties back to
"FEAT-C36-1's citation standard" the same way #867's AC-2 does). #867 has
no equivalent — if #588's actual final wording of the claim standard
changes during its own implementation, nothing forces `announcement-checklist.md`
to be revisited. *Recommendation:* either point AC-2 at a single source
of truth (e.g., have the checklist link to #588's doc rather than restate
the standard in prose) or add a drift/staleness check the way #866 did.

**5. `ordering_after: []` is inconsistent with the task's own content dependency, and no revisit trigger is recorded.**
The front matter declares no ordering dependency, so #867 can be started
and closed immediately. But AC-2 requires the checklist to state a claim
standard "inherited from FEAT-C36-1" (#588), and #588 is itself
`ordering_after` three other unimplemented issues (#300, #512, #560) —
i.e., far from done. This isn't a hard contradiction (#867 only needs to
*describe* the standard, not depend on #588's artifacts existing), but it
means #867 can close and the checklist can go stale relative to #588
before #588 even ships, with nothing in #867 flagging that risk the way
ARCHITECTURE.md's "Recorded decisions" convention requires an explicit
"Revisit trigger" for exactly this kind of forward reference.
*Recommendation:* add a one-line "revisit trigger: if FEAT-C36-1's final
claim-standard wording changes" note to the checklist itself once written.

**6. The larger feature's real acceptance gate (#590 AC-3, "exercised once") has no owning task, so #867 risks becoming a checklist nobody runs.**
`search_issues` for children of #590 returns only #866 and #867 — no
TASK-C590-3 or equivalent exists to track "at least one release has gone
out under it, with the writeup and posting links recorded in the release
notes" (#590 AC-3). #590's own comment (2026-08-04, on #590) says this
explicitly: "A checklist nobody has run does not satisfy this issue... AC-2
without AC-3 is a file; AC-2 with AC-3 is a practice." #867, taken alone,
only ever produces the file (#590's AC-2), and by design (not a flaw in
#867's own text, but a gap in the cluster) nothing currently tracks
actually running it. A reviewer merging #867's PR and closing #867 could
reasonably believe the release-announcement problem is solved, when the
practice half is untracked. *Recommendation:* note in #867 or in a new
task that this issue does not by itself close #590, and file the missing
"exercise it once" task.

**7. AC-3's "flare moment" definition is inherently self-graded with no example or rubric.**
Quote: "It defines what counts as a flare moment, so the checklist is not
triggered by every patch release nor skipped for a real one." No example
is given (the parent #590 issue does give one — "the Linux-boot demo,
#508 item 4" — but #867 does not import or reference it). Any definition,
good or bad, formally satisfies the AC. *Recommendation:* pull #590's
concrete example (Linux-boot demo) into #867's own body so the
implementer has a calibration point, rather than inventing the concept
from scratch.

## What's solid

- The core deliverable (a single short in-tree file,
  `docs/release/announcement-checklist.md`) is well-scoped, matches #590
  AC-2 verbatim, and does not conflict with anything currently in the repo.
- No code, security, or licensing surface is touched — pure documentation,
  so build/CI/coverage-ratchet risk is essentially zero.
- The task is properly split from #866 (README/site paragraph) with a
  clean boundary — no functional overlap found.
- AC-5 (self-promotion norms) is concrete and testable on its own: either
  the checklist names the fallback behavior or it doesn't.
- The repo already has a working precedent for a per-release checklist
  (`docs/wayland-desktop-checklist.md`, recorded as a comment on #100) that
  the implementer can pattern-match against for format and enforcement,
  even though #867 doesn't point to it.
